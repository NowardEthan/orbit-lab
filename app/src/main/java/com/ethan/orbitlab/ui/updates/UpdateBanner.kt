package com.ethan.orbitlab.ui.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens

/**
 * Banner de atualização — mesmo papel do UpdateBanner do orbit-mobile.
 * Toque → baixa e instala o APK do manifesto.
 */
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
    val bg = if (mandatory) OrbitTokens.danger else OrbitTokens.accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = !downloading, onClick = onUpdate)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            if (downloading) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0.05f, 1f) },
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                    trackColor = Color.White.copy(alpha = 0.25f),
                )
            } else {
                Icon(
                    Icons.Rounded.SystemUpdateAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = when {
                    downloading -> "Baixando atualização…"
                    mandatory -> "Atualização necessária"
                    else -> "Nova versão disponível"
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    downloading -> "$pct%"
                    version != null -> "Orbit v$version · toque para baixar e instalar"
                    else -> "toque para baixar e instalar"
                },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (downloading) {
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.25f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                            .height(4.dp)
                            .background(Color.White),
                    )
                }
            }
        }
        if (!downloading) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.Download,
                contentDescription = "Baixar",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
