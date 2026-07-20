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
    val sizeBytes: Long = 0L,
    val swatch: Color = Color(0xFF3A4558),
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
