package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

@Composable
fun ComposerAttachmentStrip(
    attachments: List<ComposerAttachment>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        items(attachments, key = { it.id }) { item ->
            AttachmentChip(item = item, onRemove = { onRemove(item.id) })
        }
    }
}

@Composable
fun AttachmentChip(
    item: ComposerAttachment,
    onRemove: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thumbSize = if (compact) 28.dp else 36.dp
    val thumbPx = with(density) { 72.dp.roundToPx() }.coerceIn(48, 96)
    val showThumb = item.kind == AttachmentKind.IMAGE || item.kind == AttachmentKind.VIDEO

    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(thumbSize)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (showThumb) OrbitTokens.borderSoft
                    else item.swatch.copy(alpha = 0.28f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                showThumb && item.uri != null -> {
                    AsyncImage(
                        model = attachmentThumbRequest(context, item, thumbPx),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    if (item.kind == AttachmentKind.VIDEO) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                else -> {
                    Icon(
                        Icons.Rounded.Description,
                        contentDescription = null,
                        tint = item.swatch,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.width(112.dp)) {
            Text(
                item.name,
                color = OrbitTokens.textHigh,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!compact) {
                Text(item.sizeLabel, color = OrbitTokens.textLow, fontSize = 10.sp)
            }
        }
        if (onRemove != null) {
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .orbitPressable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remover anexo",
                    tint = OrbitTokens.textLow,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
