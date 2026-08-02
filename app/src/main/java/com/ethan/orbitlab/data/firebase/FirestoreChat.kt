package com.ethan.orbitlab.data.firebase

import android.net.Uri
import com.ethan.orbitlab.data.Conversa
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.formatarHoraCurta
import com.ethan.orbitlab.data.limparPreviewMensagem
import com.ethan.orbitlab.data.ordenarMensagensChat
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.ethan.orbitlab.ui.chat.ImagemGerada
import com.ethan.orbitlab.ui.chat.LunaActionRun
import com.ethan.orbitlab.ui.chat.LunaActionRunStatus
import com.ethan.orbitlab.ui.chat.LunaActionStepKind
import com.ethan.orbitlab.ui.chat.LunaActionStepStatus
import com.ethan.orbitlab.ui.chat.LunaFonteStatus
import com.ethan.orbitlab.ui.chat.LunaWebFonte
import com.ethan.orbitlab.ui.chat.ThreadReference
import com.ethan.orbitlab.ui.chat.WireToolStep
import com.ethan.orbitlab.ui.chat.buildActionRunFromWire
import com.ethan.orbitlab.ui.chat.ehFerramentaDeWeb
import com.ethan.orbitlab.ui.chat.formatBytes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Paths e mapeamento Firestore — espelho de
 * `orbit-mobile/src/lib/firebase/firestoreChat.ts`.
 */
object FirestoreChat {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    fun conversationsCol(uid: String) =
        db.collection("users").document(uid).collection("conversations")

    fun conversationDoc(uid: String, conversationId: String) =
        conversationsCol(uid).document(conversationId)

    fun messagesCol(uid: String, conversationId: String) =
        conversationDoc(uid, conversationId).collection("messages")

    /** Conversa fixa do módulo Finanças — id estável no Firestore (sobrevive a rebuild). */
    const val ID_CONVERSA_FINANCAS = "financas"
    const val TITULO_CONVERSA_FINANCAS = "Finanças"

    fun isSessaoDeBloco(id: String): Boolean = id.startsWith("rotina-")
    fun isSessaoDeCaixa(id: String): Boolean = id == "ideias-geral"
    fun isSessaoFinancas(id: String): Boolean = id == ID_CONVERSA_FINANCAS

    fun subscribeConversations(
        uid: String,
        onChange: (List<ConversaMeta>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return conversationsCol(uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err)
                    return@addSnapshotListener
                }
                if (snap == null) {
                    onChange(emptyList())
                    return@addSnapshotListener
                }
                val list = snap.documents.mapNotNull { doc ->
                    if (doc.get("deletedAt") != null) return@mapNotNull null
                    if (isSessaoDeBloco(doc.id) || isSessaoDeCaixa(doc.id)) return@mapNotNull null
                    ConversaMeta(
                        id = doc.id,
                        titulo = doc.getString("title")?.trim()?.ifBlank { null } ?: "Conversa",
                        preview = doc.getString("preview")?.trim().orEmpty(),
                        updatedAtMs = timestampMs(doc.get("updatedAt"))
                            ?: timestampMs(doc.get("createdAt"))
                            ?: System.currentTimeMillis(),
                        messageCount = (doc.getLong("messageCount") ?: 0L).toInt(),
                        deletedMessageIds = (doc.get("deletedMessageIds") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?.toSet()
                            .orEmpty(),
                        legacyMessages = parseLegacyMessages(doc),
                    )
                }
                onChange(list)
            }
    }

    fun subscribeMessages(
        uid: String,
        conversationId: String,
        deletedIds: Set<String>,
        legacy: List<Mensagem>,
        onChange: (List<Mensagem>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return messagesCol(uid, conversationId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err)
                    return@addSnapshotListener
                }
                val docs = snap?.documents.orEmpty()
                val sub = docs.mapIndexedNotNull { index, doc ->
                    toMensagem(doc, ordemNaQuery = index)
                }.filter { it.id !in deletedIds }
                val leg = legacy.filter { it.id !in deletedIds }
                val merged = when {
                    sub.isEmpty() -> leg
                    leg.isEmpty() -> sub
                    else -> {
                        val seen = leg.map { it.id }.toSet()
                        leg + sub.filter { it.id !in seen }
                    }
                }
                // Sempre cronológico (antigo → novo). Empate de createdAt no
                // batch do Railway não pode inverter o par user→luna.
                onChange(ordenarMensagensChat(merged))
            }
    }

    /**
     * Garante que o doc da conversa existe. Se já existe, NÃO zera `messageCount`
     * nem sobrescreve título (salvo [titleLocked] + título pedido — caso Finanças).
     */
    suspend fun ensureConversation(
        uid: String,
        conversationId: String,
        title: String = "Nova conversa",
        titleLocked: Boolean = false,
    ) {
        val ref = conversationDoc(uid, conversationId)
        val snap = ref.get().await()
        if (snap.exists()) {
            val patch = mutableMapOf<String, Any>(
                "lunaSessaoId" to conversationId,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (snap.get("deletedAt") != null) {
                patch["deletedAt"] = FieldValue.delete()
            }
            if (titleLocked) {
                patch["title"] = title.trim().ifBlank { "Conversa" }.take(48)
                patch["titleLocked"] = true
            }
            ref.set(patch, SetOptions.merge()).await()
            return
        }
        ref.set(
            mapOf(
                "title" to title.trim().ifBlank { "Nova conversa" }.take(48),
                "preview" to "Nenhuma mensagem ainda.",
                "lunaSessaoId" to conversationId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "messageCount" to 0,
                "titleLocked" to titleLocked,
            ),
        ).await()
    }

    /**
     * Renomeia a conversa (título que a Luna gera pelo assunto atual). Trava o título
     * (`titleLocked`) pra o palpite ingênuo da 1ª mensagem não brigar com ele — a própria
     * Luna pode reescrever depois, que este método sempre grava por cima.
     */
    suspend fun renomearConversa(uid: String, conversationId: String, title: String) {
        val limpo = title.trim().take(48)
        if (limpo.isBlank()) return
        conversationDoc(uid, conversationId).set(
            mapOf(
                "title" to limpo,
                "titleLocked" to true,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun softDeleteConversation(uid: String, conversationId: String) {
        conversationDoc(uid, conversationId).set(
            mapOf(
                "deletedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    /** Soft-delete de mensagens específicas (pra truncar a conversa ao reenviar de um ponto). */
    suspend fun marcarMensagensApagadas(
        uid: String,
        conversationId: String,
        messageIds: List<String>,
    ) {
        if (messageIds.isEmpty()) return
        conversationDoc(uid, conversationId).set(
            mapOf(
                "deletedMessageIds" to FieldValue.arrayUnion(*messageIds.toTypedArray()),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun writeUserMessage(
        uid: String,
        conversationId: String,
        messageId: String,
        text: String,
        attachments: List<ComposerAttachment>,
        reference: ThreadReference?,
        titleHint: String?,
    ) {
        val previewSource = text.trim()
            .ifBlank { reference?.excerpt.orEmpty() }
            .ifBlank { attachments.firstOrNull()?.name.orEmpty() }
        val convRef = conversationDoc(uid, conversationId)
        val snap = convRef.get().await()
        val titleLocked = snap.getBoolean("titleLocked") == true
        val currentTitle = snap.getString("title").orEmpty()
        val patch = mutableMapOf<String, Any>(
            "preview" to previewSource.take(120),
            "updatedAt" to FieldValue.serverTimestamp(),
            "lunaSessaoId" to conversationId,
        )
        if (!snap.exists()) {
            patch["createdAt"] = FieldValue.serverTimestamp()
            patch["messageCount"] = 1
        }
        if (!titleLocked &&
            titleHint != null &&
            (currentTitle.isBlank() || currentTitle.equals("Nova conversa", true))
        ) {
            patch["title"] = titleHint.take(48)
        }
        convRef.set(patch, SetOptions.merge()).await()

        val msg = mutableMapOf<String, Any>(
            "role" to "user",
            "text" to text.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        if (attachments.isNotEmpty()) {
            msg["attachments"] = attachments.map { att ->
                buildMap<String, Any> {
                    put("id", att.id)
                    put("kind", att.toFirestoreKind())
                    put("name", att.name)
                    put("size", att.sizeBytes)
                    put("mime", att.mime)
                    att.uri?.toString()?.takeIf { it.isNotBlank() }?.let { put("uri", it) }
                }
            }
        }
        reference?.let { msg["reference"] = referenceToMap(it) }
        messagesCol(uid, conversationId).document(messageId).set(msg).await()
        if (snap.exists()) {
            bumpMessageCount(uid, conversationId, 1)
        }
    }

    suspend fun writeLunaMessage(
        uid: String,
        conversationId: String,
        messageId: String,
        text: String,
        reasoning: String?,
        actionRun: LunaActionRun?,
        imagensGeradas: List<ImagemGerada> = emptyList(),
    ) {
        conversationDoc(uid, conversationId).set(
            mapOf(
                "preview" to text.trim().take(120),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()

        val msg = mutableMapOf<String, Any>(
            "role" to "luna",
            "text" to text.trim(),
        )
        reasoning?.trim()?.takeIf { it.isNotEmpty() }?.let { msg["reasoning"] = it }
        actionRun?.let { run ->
            val mapped = actionRunToFirestore(run)
            if (mapped.isNotEmpty()) msg["research"] = mapped
        }
        if (imagensGeradas.isNotEmpty()) {
            msg["imagens"] = imagensGeradas.map { img ->
                buildMap<String, Any> {
                    put("url", img.url)
                    img.prompt.takeIf { it.isNotBlank() }?.let { put("prompt", it) }
                }
            }
        }
        val msgRef = messagesCol(uid, conversationId).document(messageId)
        val existing = msgRef.get().await()
        // Não sobrescreve createdAt se o Railway já gravou — evita reordenar o fio.
        if (!existing.exists() || existing.get("createdAt") == null) {
            msg["createdAt"] = FieldValue.serverTimestamp()
        }
        msgRef.set(msg, SetOptions.merge()).await()
        if (!existing.exists()) bumpMessageCount(uid, conversationId, 1)
    }

    private suspend fun bumpMessageCount(uid: String, conversationId: String, delta: Long) {
        conversationDoc(uid, conversationId).set(
            mapOf("messageCount" to FieldValue.increment(delta)),
            SetOptions.merge(),
        ).await()
    }

    private fun referenceToMap(ref: ThreadReference): Map<String, Any> = when (ref) {
        is ThreadReference.Message -> mapOf(
            "kind" to "message",
            "messageId" to ref.messageId,
            "role" to if (ref.isLuna) "luna" else "user",
            "messageIndex" to ref.messageIndex,
            "excerpt" to ref.excerpt,
            "fullText" to ref.fullText,
        )
        is ThreadReference.Image -> buildMap {
            put("kind", "document")
            put("messageId", ref.messageId)
            put("role", if (ref.isLuna) "luna" else "user")
            put("messageIndex", ref.messageIndex)
            put("excerpt", ref.excerpt)
            put("attachmentId", ref.attachmentId)
            put("attachmentName", ref.attachmentName)
            ref.uri?.toString()?.let { put("attachmentUri", it) }
        }
        is ThreadReference.ArtefatoTrecho -> mapOf(
            "kind" to "artefato",
            "messageId" to ref.messageId,
            "role" to "user",
            "documentoId" to ref.documentoId,
            "titulo" to ref.titulo,
            "trecho" to ref.trecho,
            "excerpt" to ref.excerpt,
        )
    }

    private fun actionRunToFirestore(run: LunaActionRun): List<Map<String, Any>> {
        return run.steps.mapNotNull { step ->
            val ferramenta = step.ferramenta ?: when (step.kind) {
                LunaActionStepKind.SEARCH -> "web_search"
                LunaActionStepKind.READ -> "ler_url"
                LunaActionStepKind.VISION -> "ver_imagem"
                LunaActionStepKind.VIDEO -> "ver_video"
                LunaActionStepKind.MEMORY -> "consultar_atlas"
                else -> return@mapNotNull null
            }
            buildMap {
                put("ferramenta", ferramenta)
                put("argumento", step.queries.firstOrNull() ?: step.detail ?: step.label)
                put("sucesso", step.status == LunaActionStepStatus.DONE)
                if (step.sources.isNotEmpty()) {
                    put(
                        "fontes",
                        step.sources.map { fonte ->
                            buildMap<String, Any> {
                                put("url", fonte.url)
                                fonte.title.takeIf { it.isNotBlank() }?.let { put("title", it) }
                            }
                        },
                    )
                }
            }
        }
    }

    private fun researchToFirestore(run: LunaActionRun): List<Map<String, Any>> =
        actionRunToFirestore(run)

    @Suppress("UNCHECKED_CAST")
    private fun toMensagem(doc: DocumentSnapshot, ordemNaQuery: Int = 0): Mensagem? {
        val data = doc.data ?: return null
        val role = when (data["role"] as? String) {
            "user" -> false
            "luna", "assistant" -> true
            else -> return null
        }
        val attachments = parseAttachments(data["attachments"])
        val imagensGeradas = parseImagensGeradas(data["imagens"])
        val reference = parseReference(data["reference"])
        val actionRun = parseActionRun(data["research"])
        // serverTimestamp pendente = null → ASC coloca no início (fio invertido).
        // Usa ordem da query + âncora recente pra ficar no fim até confirmar.
        val created = timestampMs(data["createdAt"])
            ?: if (doc.metadata.hasPendingWrites()) {
                System.currentTimeMillis() + ordemNaQuery
            } else {
                // legado sem createdAt: preserva ordem relativa da query
                ordemNaQuery.toLong()
            }
        return Mensagem(
            id = doc.id,
            texto = (data["text"] as? String).orEmpty(),
            isLuna = role,
            timestamp = created,
            reasoning = data["reasoning"] as? String,
            actionRun = actionRun,
            attachments = attachments,
            imagensGeradas = imagensGeradas,
            reference = reference,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseImagensGeradas(raw: Any?): List<ImagemGerada> {
        val lista = raw as? List<*> ?: return emptyList()
        return lista.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val url = (m["url"] as? String)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ImagemGerada(url = url, prompt = (m["prompt"] as? String).orEmpty())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAttachments(raw: Any?): List<ComposerAttachment> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val id = m["id"] as? String ?: return@mapNotNull null
            val name = m["name"] as? String ?: "arquivo"
            val mime = m["mime"] as? String ?: "application/octet-stream"
            val kindRaw = m["kind"] as? String
            val size = when (val s = m["size"]) {
                is Number -> s.toLong()
                else -> 0L
            }
            val uriStr = m["uri"] as? String
            ComposerAttachment(
                id = id,
                kind = firestoreKindToAttachment(kindRaw, mime),
                name = name,
                sizeLabel = formatBytes(size),
                sizeBytes = size,
                mime = mime,
                uri = uriStr?.takeIf { it.isNotBlank() }?.let(Uri::parse),
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseReference(raw: Any?): ThreadReference? {
        val m = raw as? Map<*, *> ?: return null
        val messageId = m["messageId"] as? String ?: return null
        val excerpt = (m["excerpt"] as? String)?.trim().orEmpty()
        if (excerpt.isEmpty()) return null
        val isLuna = (m["role"] as? String) != "user"
        val messageIndex = (m["messageIndex"] as? Number)?.toInt() ?: 0
        val attachmentId = m["attachmentId"] as? String
        val kind = m["kind"] as? String
        if (kind == "artefato") {
            val documentoId = m["documentoId"] as? String ?: messageId
            val trecho = (m["trecho"] as? String).orEmpty()
            if (trecho.isBlank()) return null
            return ThreadReference.ArtefatoTrecho(
                documentoId = documentoId,
                titulo = (m["titulo"] as? String) ?: "Artefato",
                trecho = trecho,
                excerpt = excerpt,
            )
        }
        if (kind == "document" || attachmentId != null) {
            return ThreadReference.Image(
                messageId = messageId,
                messageIndex = messageIndex,
                isLuna = isLuna,
                attachmentId = attachmentId ?: "att-$messageIndex",
                attachmentName = (m["attachmentName"] as? String) ?: "Anexo",
                uri = (m["attachmentUri"] as? String)?.let(Uri::parse),
                excerpt = excerpt,
            )
        }
        return ThreadReference.Message(
            messageId = messageId,
            messageIndex = messageIndex,
            isLuna = isLuna,
            excerpt = excerpt,
            fullText = (m["fullText"] as? String) ?: excerpt,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseActionRun(raw: Any?): LunaActionRun? {
        val list = raw as? List<*> ?: return null
        if (list.isEmpty()) return null
        val wire = list.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val ferramenta = m["ferramenta"] as? String ?: return@mapNotNull null
            val argumento = m["argumento"] as? String ?: ""
            val fontesRaw = m["fontes"] as? List<*>
            val sources = fontesRaw?.mapIndexedNotNull { fi, f ->
                val fm = f as? Map<*, *> ?: return@mapIndexedNotNull null
                val url = fm["url"] as? String ?: return@mapIndexedNotNull null
                LunaWebFonte(
                    id = "f-$ferramenta-$fi",
                    title = (fm["title"] as? String) ?: url,
                    url = url,
                    domain = runCatching { Uri.parse(url).host }.getOrNull().orEmpty(),
                    status = LunaFonteStatus.CITADA,
                )
            }.orEmpty()
            WireToolStep(
                ferramenta = ferramenta,
                argumento = argumento,
                sucesso = m["sucesso"] as? Boolean ?: true,
                fontes = sources,
            )
        }
        if (wire.isEmpty()) return null
        val title = if (wire.any { ehFerramentaDeWeb(it.ferramenta) }) {
            "Pesquisa"
        } else {
            "Ferramentas"
        }
        return buildActionRunFromWire(
            id = "fs-actions",
            title = title,
            wireSteps = wire,
            status = LunaActionRunStatus.DONE,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseLegacyMessages(doc: DocumentSnapshot): List<Mensagem> {
        val raw = doc.get("messages") as? List<*> ?: return emptyList()
        return raw.mapIndexedNotNull { index, item ->
            val m = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val role = when (m["role"] as? String) {
                "user" -> false
                "luna", "assistant" -> true
                else -> return@mapIndexedNotNull null
            }
            val id = (m["id"] as? String)?.takeIf { it.isNotBlank() } ?: "legacy-$index"
            Mensagem(
                id = id,
                texto = (m["text"] as? String).orEmpty(),
                isLuna = role,
                timestamp = timestampMs(m["createdAt"]) ?: (index.toLong()),
            )
        }
    }

    private fun timestampMs(value: Any?): Long? = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> null
    }
}

data class ConversaMeta(
    val id: String,
    val titulo: String,
    val preview: String,
    val updatedAtMs: Long,
    val messageCount: Int = 0,
    val deletedMessageIds: Set<String> = emptySet(),
    val legacyMessages: List<Mensagem> = emptyList(),
)

fun ConversaMeta.toConversa(mensagens: List<Mensagem> = emptyList()): Conversa {
    val previewFonte = preview.ifBlank {
        mensagens.lastOrNull()?.texto
    }
    return Conversa(
        id = id,
        titulo = titulo,
        mensagens = mensagens,
        ultimaAtualizacao = updatedAtMs,
        preview = limparPreviewMensagem(previewFonte),
        horaFormatada = formatarHoraCurta(updatedAtMs),
        messageCount = mensagens.size.coerceAtLeast(messageCount),
    )
}
