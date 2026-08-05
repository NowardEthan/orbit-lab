package com.ethan.orbitlab.ui.chat

import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class AttachmentKind {
    IMAGE,
    VIDEO,
    FILE,
}

data class ComposerAttachment(
    val id: String = UUID.randomUUID().toString(),
    val kind: AttachmentKind,
    val name: String,
    val sizeLabel: String,
    val mime: String,
    val uri: Uri? = null,
    val thumbnailUri: Uri? = null,
    val sizeBytes: Long = 0L,
    val swatch: Color = Color(0xFF3A4558),
)

/**
 * Uma imagem que a LUNA desenhou (ferramenta `gerar_imagem` no core). Diferente de um anexo do
 * usuário: nasce de um `prompt` e vive numa URL do Storage. Vira um cartão no lado dela do chat.
 */
data class ImagemGerada(
    val url: String,
    val prompt: String,
)

/**
 * Uma pergunta que a LUNA fez antes de agir (ferramenta `perguntar` no core), quando uma escolha
 * de gosto mudaria o resultado. Vira um cartão com opções tocáveis sob a bolha dela; tocar numa
 * opção envia aquela resposta na hora. É efêmera — some quando você responde. 🌙
 */
data class PerguntaLuna(
    val texto: String,
    val opcoes: List<String>,
)

data class GalleryAlbum(
    val id: String,
    val title: String,
    val count: Int,
)

data class GalleryMedia(
    val id: String,
    val albumId: String,
    val name: String,
    val uri: Uri,
    val mime: String,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val dateAddedSec: Long = 0L,
) {
    val sizeLabel: String get() = formatBytes(sizeBytes)
}

const val MAX_ATTACH_IMAGES = 5
const val MAX_ATTACH_FILES = 5
/** Vídeos contam no orçamento de arquivos (como no mobile). */
const val MAX_VIDEO_BYTES = 80L * 1024L * 1024L

fun GalleryMedia.toAttachment(): ComposerAttachment = ComposerAttachment(
    id = id,
    kind = if (isVideo) AttachmentKind.VIDEO else AttachmentKind.IMAGE,
    name = name,
    sizeLabel = sizeLabel,
    mime = mime,
    uri = uri,
    sizeBytes = sizeBytes,
)

fun formatBytes(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> {
        val mb = bytes / (1024.0 * 1024.0)
        String.format("%.1f MB", mb).replace('.', ',')
    }
}
