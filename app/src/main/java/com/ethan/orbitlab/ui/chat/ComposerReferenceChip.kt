package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
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
import coil.request.ImageRequest
import coil.size.Size
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/** Chip acima do composer — referência ativa. */
@Composable
fun ComposerReferenceChip(
    reference: ThreadReference,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(shape)
            .background(OrbitTokens.graphiteRaised)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Trilho de acento: contraste sólido em vez de um banho de accentSoft.
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(OrbitTokens.bluePastel),
        )
        Row(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        when (reference) {
            is ThreadReference.Image -> {
                if (reference.uri != null) {
                    val density = LocalDensity.current
                    val px = with(density) { 40.dp.roundToPx() }
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(reference.uri)
                            .size(Size(px, px))
                            .memoryCacheKey("ref-${reference.attachmentId}")
                            .crossfade(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    RefIcon(Icons.Rounded.Image)
                }
            }
            is ThreadReference.Message -> RefIcon(Icons.Rounded.ChatBubbleOutline)
            is ThreadReference.ArtefatoTrecho -> RefIcon(Icons.Rounded.FormatQuote)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when (reference) {
                    is ThreadReference.Image -> "Referência · imagem"
                    is ThreadReference.ArtefatoTrecho -> "Referência · artefato"
                    is ThreadReference.Message -> "Referência · mensagem"
                },
                color = OrbitTokens.bluePastel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                referenceChipLabel(reference),
                color = OrbitTokens.textHiN,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
        }
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .orbitPressable(onClick = onClear),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remover referência",
                tint = OrbitTokens.textMidN,
                modifier = Modifier.size(16.dp),
            )
        }
        }
    }
}

@Composable
private fun RefIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(OrbitTokens.graphiteSurf),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = OrbitTokens.bluePastel, modifier = Modifier.size(18.dp))
    }
}

/** Quote inset na bolha enviada. */
@Composable
fun MessageReferenceQuote(
    reference: ThreadReference,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.75f)),
        )
        if (reference is ThreadReference.Image && reference.uri != null) {
            val density = LocalDensity.current
            val px = with(density) { 36.dp.roundToPx() }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(reference.uri)
                    .size(Size(px, px))
                    .memoryCacheKey("quote-${reference.attachmentId}")
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        }
        Text(
            referenceChipLabel(reference),
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            lineHeight = 16.sp,
        )
    }
}
