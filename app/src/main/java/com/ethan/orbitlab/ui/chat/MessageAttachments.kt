package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

fun formatFileExtension(name: String): String {
    val ext = name.substringAfterLast('.', missingDelimiterValue = "")
        .trim()
        .uppercase()
    return when {
        ext.isBlank() -> "ARQ"
        ext.length > 4 -> ext.take(4)
        else -> ext
    }
}

/**
 * Anexos na bolha — mídia limpa (sem nome) + cards de arquivo.
 */
@Composable
fun MessageAttachments(
    attachments: List<ComposerAttachment>,
    solo: Boolean = false,
    modifier: Modifier = Modifier,
    onReferenciarMedia: ((ComposerAttachment) -> Unit)? = null,
) {
    if (attachments.isEmpty()) return

    val media = remember(attachments) {
        attachments.filter {
            it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.VIDEO
        }
    }
    val files = remember(attachments) {
        attachments.filter { it.kind == AttachmentKind.FILE }
    }
    val multiMedia = media.size > 1
    var previewIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    fun openMedia(item: ComposerAttachment) {
        val idx = media.indexOfFirst { it.id == item.id }
        previewIndex = if (idx >= 0) idx else 0
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End,
    ) {
        if (media.isNotEmpty()) {
            if (multiMedia) {
                MediaGrid(
                    items = media,
                    onOpen = { openMedia(it) },
                    onLongPress = onReferenciarMedia,
                )
            } else {
                val unico = media.first()
                MediaTile(
                    item = unico,
                    solo = solo,
                    multi = false,
                    onOpen = { openMedia(unico) },
                    onLongPress = onReferenciarMedia?.let { { it(unico) } },
                    modifier = Modifier
                        .fillMaxWidth(if (solo) 1f else 0.92f)
                        .widthIn(max = 280.dp),
                )
            }
        }
        files.forEach { file ->
            MessageFileCard(
                item = file,
                onOpen = { openAttachmentExternally(context, file) },
                modifier = Modifier
                    .fillMaxWidth(if (solo) 1f else 0.92f)
                    .widthIn(max = 280.dp),
            )
        }
    }

    previewIndex?.let { idx ->
        MediaViewerDialog(
            items = media,
            initialIndex = idx,
            onDismiss = { previewIndex = null },
            onReferenciar = onReferenciarMedia,
        )
    }
}

@Composable
fun MessageAttachmentsRow(attachments: List<ComposerAttachment>) {
    MessageAttachments(attachments = attachments, solo = true)
}

@Composable
private fun MediaGrid(
    items: List<ComposerAttachment>,
    onOpen: (ComposerAttachment) -> Unit,
    onLongPress: ((ComposerAttachment) -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 3.dp
        val colW = (maxWidth - gap) / 2
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            items.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { item ->
                        MediaTile(
                            item = item,
                            solo = false,
                            multi = true,
                            onOpen = { onOpen(item) },
                            onLongPress = onLongPress?.let { { it(item) } },
                            modifier = Modifier.width(colW),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.width(colW))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaTile(
    item: ComposerAttachment,
    solo: Boolean,
    multi: Boolean,
    onOpen: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val thumbPx = with(density) { 320.dp.roundToPx() }.coerceIn(180, 480)
    val ratio = when {
        multi -> 1f
        solo -> 5f / 4f
        else -> 4f / 3f
    }
    val shape = RoundedCornerShape(if (solo && !multi) 18.dp else 14.dp)
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.22f))
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                enabled = item.uri != null,
                onClick = onOpen,
                onLongClick = onLongPress?.let {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            )
            .aspectRatio(ratio),
    ) {
        if (item.uri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(Size(thumbPx, thumbPx))
                    .memoryCacheKey("msg-${item.id}")
                    .diskCacheKey("msg-${item.id}")
                    .crossfade(false)
                    .allowHardware(true)
                    .build(),
                contentDescription = if (item.kind == AttachmentKind.VIDEO) "Vídeo" else "Imagem",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(OrbitTokens.surfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Image,
                    contentDescription = null,
                    tint = OrbitTokens.textLow,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        if (item.kind == AttachmentKind.VIDEO) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.48f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = "Reproduzir",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageFileCard(
    item: ComposerAttachment,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = formatFileExtension(item.name)
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .clip(shape)
            .background(OrbitTokens.accent.copy(alpha = 0.94f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .orbitPressable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .padding(horizontal = 9.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                ext,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.sizeLabel,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = "Abrir",
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp),
        )
    }
}
