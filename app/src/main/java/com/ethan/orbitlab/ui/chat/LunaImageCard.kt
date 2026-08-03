package com.ethan.orbitlab.ui.chat

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Cartão de uma imagem que a LUNA desenhou (ferramenta `gerar_imagem`). Fica no lado dela do
 * chat: a imagem em si, uma etiqueta "desenhada pela Luna", e as ações salvar/compartilhar.
 * Tocar na imagem abre o visualizador imersivo (mesmo do resto do chat), com pinch-zoom.
 */
@Composable
fun LunaImagemGeradaLista(
    imagens: List<ImagemGerada>,
    modifier: Modifier = Modifier,
    onRegerarAspecto: ((aspectoLabel: String, aspectoRatio: String) -> Unit)? = null,
) {
    if (imagens.isEmpty()) return
    Column(
        modifier = modifier.widthIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        imagens.forEach { imagem ->
            LunaImageCard(
                imagem = imagem,
                onRegerarAspecto = onRegerarAspecto,
            )
        }
    }
}

@Composable
private fun LunaImageCard(
    imagem: ImagemGerada,
    modifier: Modifier = Modifier,
    onRegerarAspecto: ((aspectoLabel: String, aspectoRatio: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var visualizando by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }

    val forma = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier.widthIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(forma)
                .border(1.dp, OrbitTokens.borderSoft, forma)
                .orbitPressable(onClick = { visualizando = true }),
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imagem.url)
                    .crossfade(true)
                    .build(),
                contentDescription = imagem.prompt.ifBlank { "Imagem gerada pela Luna" },
                contentScale = ContentScale.FillWidth,
                loading = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .background(OrbitTokens.surfaceRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = OrbitTokens.accentText,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                },
                error = {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .background(OrbitTokens.surfaceRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "não consegui carregar a imagem",
                            color = OrbitTokens.textMid,
                            fontSize = 13.sp,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
            )

            // Rodapé discreto: um véu escuro degradê só na base pra a etiqueta ler em qualquer imagem.
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.45f),
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "Desenhada pela Luna",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (onRegerarAspecto != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AspectoChip(label = "16:9", emoji = "📺", onClick = { onRegerarAspecto("widescreen 16:9", "16:9") })
                AspectoChip(label = "9:16", emoji = "📱", onClick = { onRegerarAspecto("vertical 9:16", "9:16") })
                AspectoChip(label = "1:1", emoji = "🔲", onClick = { onRegerarAspecto("quadrado 1:1", "1:1") })
                AspectoChip(label = "21:9", emoji = "🖼️", onClick = { onRegerarAspecto("ultrawide 21:9", "21:9") })
            }
        }
    }

    if (visualizando) {
        val item = remember(imagem.url) {
            ComposerAttachment(
                id = "luna-img-${imagem.url.hashCode()}",
                kind = AttachmentKind.IMAGE,
                name = imagem.prompt.ifBlank { "Imagem da Luna" },
                sizeLabel = "—",
                mime = "image/webp",
                uri = Uri.parse(imagem.url),
            )
        }
        MediaViewerDialog(
            item = item,
            onDismiss = { visualizando = false },
            onDownload = {
                if (!salvando) {
                    salvando = true
                    scope.launch {
                        val ok = ImagemGeradaIO.salvarNaGaleria(context, imagem.url)
                        salvando = false
                        Toast.makeText(
                            context,
                            if (ok) "Imagem salva na galeria" else "Não consegui salvar a imagem",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            onShareOverride = {
                scope.launch {
                    val ok = ImagemGeradaIO.compartilhar(context, imagem.url)
                    if (!ok) {
                        Toast.makeText(
                            context,
                            "Não consegui preparar a imagem",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
        )
    }
}

/**
 * Baixar / salvar / compartilhar a imagem gerada — ela vive numa URL do Storage, então o
 * salvar e o compartilhar precisam trazer os bytes de volta primeiro.
 */
object ImagemGeradaIO {

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun baixar(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                resp.body?.bytes()
            }
        }.getOrNull()
    }

    /** Salva na galeria (Pictures/Orbit). true = salvou. */
    suspend fun salvarNaGaleria(context: Context, url: String): Boolean {
        val bytes = baixar(url) ?: return false
        return withContext(Dispatchers.IO) {
            runCatching {
                val nome = "luna-${System.currentTimeMillis()}.webp"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, nome)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/webp")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/Orbit",
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values,
                    ) ?: return@runCatching false
                    resolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: return@runCatching false
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } else {
                    @Suppress("DEPRECATION")
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES,
                        ),
                        "Orbit",
                    ).apply { mkdirs() }
                    val arquivo = File(dir, nome)
                    arquivo.writeBytes(bytes)
                    // Faz o arquivo aparecer na galeria.
                    MediaStore.Images.Media.insertImage(
                        context.contentResolver,
                        arquivo.absolutePath,
                        nome,
                        "Imagem gerada pela Luna",
                    )
                    true
                }
            }.getOrDefault(false)
        }
    }

    /** Escreve num arquivo de cache e abre o compartilhador do Android. true = abriu. */
    suspend fun compartilhar(context: Context, url: String): Boolean {
        val bytes = baixar(url) ?: return false
        return runCatching {
            val dir = File(context.cacheDir, "imagens").apply { mkdirs() }
            val arquivo = File(dir, "luna-${System.currentTimeMillis()}.webp")
            withContext(Dispatchers.IO) { arquivo.writeBytes(bytes) }
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, arquivo)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/webp"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Compartilhar imagem")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }
}

@Composable
private fun AspectoChip(
    label: String,
    emoji: String,
    onClick: () -> Unit,
) {
    val forma = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .clip(forma)
            .background(OrbitTokens.graphiteRaised)
            .border(1.dp, OrbitTokens.borderSoft, forma)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = emoji,
            fontSize = 10.sp,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            color = OrbitTokens.textMid,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
