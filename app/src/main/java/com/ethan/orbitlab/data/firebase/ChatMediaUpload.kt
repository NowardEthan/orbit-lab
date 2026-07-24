package com.ethan.orbitlab.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.ethan.orbitlab.ui.chat.AttachmentKind
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Upload de anexos de chat para o mesmo bucket/paths do orbit-mobile.
 *
 * Path:
 * `users/{uid}/conversations/{conversationId}/messages/{messageId}/attachments/{id}/{name}`
 *
 * O mesmo anexo é pedido por dois caminhos ao mesmo tempo (o balão da mensagem e a chamada
 * à Luna). Antes cada um subia o arquivo por conta — dois uploads simultâneos pro MESMO
 * endereço, gastando o dobro da rede e brigando entre si. Aqui o primeiro sobe e o segundo
 * espera o resultado dele.
 */
object ChatMediaUpload {
    private val storage: FirebaseStorage get() = FirebaseStorage.getInstance()

    private const val TENTATIVAS = 3
    private const val HOST = "https://firebasestorage.googleapis.com/v0/b"

    /**
     * Foto de celular sai com 12 MP e 4-6 MB. Subir isso em rede de celular leva quase um
     * minuto — e o modelo que vai olhar a imagem reduz pra ~1000 px antes de ver. Então
     * mandamos já enxuto: mesmo conteúdo, uma fração do tempo.
     */
    private const val LADO_MAX = 1600
    private const val QUALIDADE = 82

    /** Abaixo disso não vale reprocessar: o ganho é menor que o custo. */
    private const val PISO_BYTES = 320L * 1024

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        // Vídeo em rede de celular sobe devagar; cortar no meio é o pior dos mundos.
        .writeTimeout(300, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tranca = Mutex()
    private val emAndamento = mutableMapOf<String, Deferred<ComposerAttachment>>()
    private val prontos = mutableMapOf<String, ComposerAttachment>()

    fun isRemoteUri(uri: Uri?): Boolean {
        val s = uri?.toString().orEmpty()
        return s.startsWith("https://", ignoreCase = true) ||
            s.startsWith("http://", ignoreCase = true)
    }

    suspend fun uploadAttachments(
        context: Context,
        uid: String,
        conversationId: String,
        messageId: String,
        attachments: List<ComposerAttachment>,
    ): List<ComposerAttachment> {
        if (attachments.isEmpty()) return attachments
        return attachments.map { att ->
            uploadOne(context, uid, conversationId, messageId, att)
        }
    }

    private suspend fun uploadOne(
        context: Context,
        uid: String,
        conversationId: String,
        messageId: String,
        attachment: ComposerAttachment,
    ): ComposerAttachment {
        val local = attachment.uri
        if (local == null || isRemoteUri(local)) return attachment

        val safeName = sanitizeFileName(attachment.name)
        val path =
            "users/$uid/conversations/$conversationId/messages/$messageId/" +
                "attachments/${attachment.id}/$safeName"

        val tarefa = tranca.withLock {
            prontos[path]?.let { return it }
            emAndamento[path] ?: escopo.async {
                subir(context, path, attachment, local)
            }.also { emAndamento[path] = it }
        }
        try {
            val subido = tarefa.await()
            tranca.withLock {
                emAndamento.remove(path)
                if (prontos.size > 40) prontos.clear()
                prontos[path] = subido
            }
            return subido
        } catch (e: Throwable) {
            tranca.withLock { if (emAndamento[path] === tarefa) emAndamento.remove(path) }
            throw e
        }
    }

    /**
     * Sobe pelo endpoint REST do Storage, com o token de ID no cabeçalho.
     *
     * O SDK monta a autenticação sozinho e, quando ele não anexa o token, o servidor devolve
     * "permission denied" mesmo com as regras liberando o dono — foi o erro de acesso que
     * apareceu no chat. Aqui o token vai explícito, do mesmo jeito que já funciona no resto
     * do app. E sobe teimando: rede de celular tropeça, desistir na primeira perde a foto.
     */
    private suspend fun subir(
        context: Context,
        path: String,
        attachment: ComposerAttachment,
        local: Uri,
    ): ComposerAttachment {
        val subida = try {
            subirArquivo(context, path, local, attachment.mime)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IOException(recado(attachment, e), e)
        }
        return attachment.copy(
            uri = Uri.parse(subida.url),
            mime = subida.mime,
            sizeLabel = tamanhoLegivel(subida.bytes),
        )
    }

    /** O que de fato foi parar no Storage (pode ser uma versão enxuta do original). */
    data class Subida(val url: String, val mime: String, val bytes: Long)

    /** Sobe um arquivo qualquer pro Storage e devolve a URL de download. */
    suspend fun subirArquivo(
        context: Context,
        path: String,
        local: Uri,
        mimeDeclarado: String,
    ): Subida {
        val bucket = storage.reference.bucket
        val original = mimeDeclarado.ifBlank { "application/octet-stream" }
        val destino = "$HOST/$bucket/o?uploadType=media&name=${enc(path)}"

        // Imagem pesada vira uma cópia enxuta antes de encostar na rede.
        val enxuta = withContext(Dispatchers.IO) { encolherImagem(context, local, original) }
        // Sem tamanho conhecido não dá pra fazer streaming — materializa uma cópia UMA vez
        // (dentro do laço, cada tentativa apagaria o arquivo da anterior).
        val copia = if (enxuta == null && tamanhoDe(context, local) <= 0L) {
            withContext(Dispatchers.IO) { runCatching { copiarParaCache(context, local) }.getOrNull() }
        } else {
            null
        }
        val fonte = enxuta?.arquivo ?: copia
        val mime = enxuta?.mime ?: original
        val bytes = fonte?.length() ?: tamanhoDe(context, local).coerceAtLeast(0L)

        try {
            var ultima: Exception? = null
            var tentativa = 0
            while (tentativa < TENTATIVAS) {
                tentativa++
                // 1ª tentativa usa o token em cache; se falhar, força um token novo.
                val token = idToken(forcar = tentativa > 1)
                    ?: throw IOException("sem sessão pra subir o arquivo")
                try {
                    val json = withContext(Dispatchers.IO) {
                        val corpo = if (fonte != null) {
                            corpoDeArquivo(fonte, mime)
                        } else {
                            corpoDeUri(context, local, mime, bytes)
                        }
                        val req = Request.Builder()
                            .url(destino)
                            .addHeader("Authorization", "Firebase $token")
                            .post(corpo)
                            .build()
                        http.newCall(req).execute().use { resp ->
                            val txt = resp.body?.string().orEmpty()
                            if (!resp.isSuccessful) {
                                throw IOException("HTTP ${resp.code} ${resumo(txt)}")
                            }
                            JSONObject(txt)
                        }
                    }
                    val chave = json.optString("downloadTokens")
                    val url = "$HOST/$bucket/o/${enc(path)}?alt=media" +
                        if (chave.isNotBlank()) "&token=$chave" else ""
                    return Subida(url = url, mime = mime, bytes = bytes)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ultima = e
                    if (tentativa < TENTATIVAS) delay(tentativa * 1200L)
                }
            }
            throw ultima ?: IOException("o upload falhou sem dizer por quê")
        } finally {
            enxuta?.arquivo?.delete()
            copia?.delete()
        }
    }

    private suspend fun idToken(forcar: Boolean): String? = runCatching {
        FirebaseAuth.getInstance().currentUser?.getIdToken(forcar)?.await()?.token
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /** Streaming: o arquivo nunca é carregado inteiro na memória (celular apertado). */
    private fun corpoDeArquivo(arquivo: File, mime: String): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = mime.toMediaTypeOrNull()
        override fun contentLength(): Long = arquivo.length()
        override fun writeTo(sink: BufferedSink) {
            FileInputStream(arquivo).use { sink.writeAll(it.source()) }
        }
    }

    private fun corpoDeUri(
        context: Context,
        uri: Uri,
        mime: String,
        tamanho: Long,
    ): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = mime.toMediaTypeOrNull()
        override fun contentLength(): Long = if (tamanho > 0L) tamanho else -1L
        override fun writeTo(sink: BufferedSink) {
            abrirEntrada(context, uri).use { entrada -> sink.writeAll(entrada.source()) }
        }
    }

    private class Enxuta(val arquivo: File, val mime: String)

    /**
     * Reduz a imagem antes de subir: lado maior em [LADO_MAX], JPEG em [QUALIDADE].
     *
     * O que sai daqui costuma ser 10-20x menor que a foto original, e é exatamente o que
     * o modelo enxerga de qualquer jeito. A orientação do EXIF é aplicada na hora (quem
     * recomprime sem olhar o EXIF entrega a foto deitada).
     *
     * Devolve `null` quando não vale a pena mexer — aí sobe o arquivo original, intacto.
     */
    private fun encolherImagem(context: Context, uri: Uri, mime: String): Enxuta? {
        val m = mime.lowercase(Locale.US)
        if (!m.startsWith("image/") || m.contains("gif")) return null
        val original = tamanhoDe(context, uri)
        if (original in 1 until PISO_BYTES) return null

        return runCatching {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            abrirEntrada(context, uri).use { BitmapFactory.decodeStream(it, null, limites) }
            val maior = maxOf(limites.outWidth, limites.outHeight)
            if (maior <= 0) return null

            val giro = giroExif(context, uri)

            // Decodifica já reduzido (potência de 2) — nunca cabe a foto inteira na memória.
            val opcoes = BitmapFactory.Options().apply {
                inSampleSize = amostragem(maior, LADO_MAX)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bruta = abrirEntrada(context, uri).use {
                BitmapFactory.decodeStream(it, null, opcoes)
            } ?: return null

            val escala = (LADO_MAX.toFloat() / maxOf(bruta.width, bruta.height)).coerceAtMost(1f)
            val matriz = Matrix().apply {
                if (escala < 1f) postScale(escala, escala)
                if (giro != 0) postRotate(giro.toFloat())
            }
            val pronta = if (matriz.isIdentity) {
                bruta
            } else {
                Bitmap.createBitmap(bruta, 0, 0, bruta.width, bruta.height, matriz, true)
                    .also { if (it !== bruta) bruta.recycle() }
            }

            val destino = File(File(context.cacheDir, "uploads").apply { mkdirs() }, "opt_${System.nanoTime()}.jpg")
            destino.outputStream().use { saida ->
                pronta.compress(Bitmap.CompressFormat.JPEG, QUALIDADE, saida)
            }
            pronta.recycle()

            // Se não ficou menor (imagem já otimizada), o original serve melhor.
            if (destino.length() <= 0L || (original > 0 && destino.length() >= original)) {
                destino.delete()
                return null
            }
            Enxuta(destino, "image/jpeg")
        }.getOrNull()
    }

    private fun amostragem(maior: Int, alvo: Int): Int {
        var passo = 1
        while (maior / (passo * 2) >= alvo) passo *= 2
        return passo
    }

    private fun giroExif(context: Context, uri: Uri): Int = runCatching {
        val exif = abrirEntrada(context, uri).use { ExifInterface(it) }
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)

    private fun tamanhoLegivel(bytes: Long): String = when {
        bytes <= 0L -> "—"
        bytes < 1024L * 1024 -> "${(bytes / 1024f).toInt()} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    }

    private fun tamanhoDe(context: Context, uri: Uri): Long {
        arquivoLocal(context, uri)?.let { return it.length() }
        return runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
        }.getOrNull()?.takeIf { it > 0L } ?: -1L
    }

    private fun copiarParaCache(context: Context, uri: Uri): File {
        val pasta = File(context.cacheDir, "uploads").apply { mkdirs() }
        val destino = File(pasta, "up_${System.nanoTime()}")
        abrirEntrada(context, uri).use { entrada ->
            destino.outputStream().use { saida -> entrada.copyTo(saida) }
        }
        return destino
    }

    private fun enc(valor: String): String =
        URLEncoder.encode(valor, "UTF-8").replace("+", "%20")

    /** Mensagem de erro do Storage vem em JSON — pega só a frase útil. */
    private fun resumo(corpo: String): String {
        val doJson = runCatching {
            JSONObject(corpo).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrNull()
        return (doJson?.takeIf { it.isNotBlank() } ?: corpo).trim().take(160)
    }

    /**
     * Fluxo de leitura do anexo.
     *
     * Quando o arquivo é nosso (câmera do app, em cache), lê direto do disco: pular o
     * content provider evita o descritor que morre calado em aparelho apertado de memória.
     */
    private fun abrirEntrada(context: Context, uri: Uri): InputStream {
        arquivoLocal(context, uri)?.let { return FileInputStream(it) }
        return context.contentResolver.openInputStream(uri)
            ?: throw IOException("o aparelho não abriu o arquivo")
    }

    private fun arquivoLocal(context: Context, uri: Uri): File? {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            return uri.path?.let(::File)?.takeIf { it.canRead() }
        }
        if (uri.scheme.equals("content", ignoreCase = true) &&
            uri.authority == "${context.packageName}.fileprovider"
        ) {
            val segs = uri.pathSegments
            if (segs.size >= 2 && segs[0] == "camera") {
                val relativo = segs.drop(1).joinToString("/")
                return File(File(context.cacheDir, "camera"), relativo).takeIf { it.canRead() }
            }
        }
        return null
    }

    /** Recado que serve pra ele e pra mim: o que falhou + a pista técnica, sem "Success". */
    private fun recado(attachment: ComposerAttachment, causa: Throwable): String {
        val alvo = when (attachment.kind) {
            AttachmentKind.IMAGE -> "a imagem"
            AttachmentKind.VIDEO -> "o vídeo"
            AttachmentKind.FILE -> "o arquivo"
        }
        return "não consegui subir $alvo (${cadeia(causa)})"
    }

    private fun cadeia(causa: Throwable): String {
        val partes = mutableListOf<String>()
        var atual: Throwable? = causa
        var nivel = 0
        while (atual != null && nivel < 4) {
            partes += pista(atual)
            val proxima = atual.cause
            atual = if (proxima === atual) null else proxima
            nivel++
        }
        return partes.distinct().joinToString(" ← ")
    }

    private fun pista(t: Throwable): String {
        val nome = t.javaClass.simpleName
        val msg = t.message?.trim()?.takeIf {
            it.isNotEmpty() && !it.equals("Success", ignoreCase = true)
        }
        return if (msg == null) nome else "$nome: ${msg.take(140)}"
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("""[^\w.\-()+@]"""), "_").trim()
        return cleaned.takeIf { it.isNotEmpty() }?.take(120) ?: "arquivo"
    }
}

/** kind Firestore: image | file (vídeo = file, como no mobile). */
fun ComposerAttachment.toFirestoreKind(): String = when (kind) {
    AttachmentKind.IMAGE -> "image"
    AttachmentKind.VIDEO, AttachmentKind.FILE -> "file"
}

fun firestoreKindToAttachment(
    kind: String?,
    mime: String,
): AttachmentKind {
    if (kind == "image") return AttachmentKind.IMAGE
    val m = mime.lowercase(Locale.US)
    if (m.startsWith("video/")) return AttachmentKind.VIDEO
    return AttachmentKind.FILE
}
