package com.ethan.orbitlab.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * UpdateBanner — banner de atualização do Início no redesign 1.0 (grafite, azul pastel, minimalista).
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
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(
                1.dp,
                if (mandatory) OrbitTokens.danger.copy(alpha = 0.5f) else OrbitTokens.graphiteHair,
                shape,
            )
            .then(if (!downloading) Modifier.orbitPressable(onClick = onUpdate) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (mandatory) OrbitTokens.danger.copy(alpha = 0.15f) else OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (downloading) {
                CircularProgressIndicator(
                    color = OrbitTokens.bluePastel,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    if (mandatory) Icons.Rounded.SystemUpdate else Icons.Rounded.Download,
                    contentDescription = null,
                    tint = if (mandatory) OrbitTokens.danger else OrbitTokens.bluePastel,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    downloading -> "Baixando atualização…"
                    mandatory -> "Atualização obrigatória"
                    version != null -> "OrbitLab v$version disponível"
                    else -> "Nova versão disponível"
                },
                color = if (mandatory) OrbitTokens.danger else OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    downloading -> "Instalando em instantes ($pct%)"
                    mandatory -> "Atualize para continuar usando o lab"
                    else -> "Toque para baixar e instalar"
                },
                color = OrbitTokens.textMidN,
                fontSize = 12.sp,
                maxLines = 1,
            )
            if (downloading) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = OrbitTokens.bluePastel,
                    trackColor = OrbitTokens.graphiteHair,
                )
            }
        }
    }
}
