package com.ethan.orbitlab.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

@Composable
fun UpdateBanner(
    version: String?,
    mandatory: Boolean,
    downloading: Boolean,
    progress: Float,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pct = (progress.coerceIn(0f, 1f) * 100).toInt()
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    val bg = if (mandatory) OrbitTokens.danger.copy(alpha = 0.18f) else OrbitTokens.accent.copy(alpha = 0.16f)
    val border = if (mandatory) OrbitTokens.danger.copy(alpha = 0.45f) else OrbitTokens.accent.copy(alpha = 0.35f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .then(if (!downloading) Modifier.orbitPressable(onClick = onUpdate) else Modifier)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(OrbitMetrics.radiusIcon))
                .background(OrbitTokens.accent),
            contentAlignment = Alignment.Center,
        ) {
            if (downloading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    Icons.Rounded.Download,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    downloading -> "Baixando atualização…"
                    mandatory -> "Atualização necessária"
                    else -> "Nova versão disponível"
                },
                color = OrbitTokens.textHigh,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    downloading -> "$pct%"
                    version != null -> "OrbitLab v$version · toque para baixar e instalar"
                    else -> "Toque para baixar e instalar"
                },
                color = OrbitTokens.textMid,
                fontSize = 12.sp,
                maxLines = 1,
            )
            if (downloading) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = OrbitTokens.accent,
                    trackColor = OrbitTokens.surfaceRaised,
                )
            }
        }
    }
}
