package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * FAB circular minimalista da Luna para Finanças (54dp).
 * Alterna suavemente entre o ícone da lua e o ícone de fechar.
 */
@Composable
fun FinancasLunaFab(
    ativo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bordaGradiente = Brush.horizontalGradient(
        if (ativo) {
            listOf(
                OrbitTokens.bluePastel,
                OrbitTokens.bluePastel.copy(alpha = 0.5f),
            )
        } else {
            listOf(
                OrbitTokens.bluePastel.copy(alpha = 0.40f),
                OrbitTokens.graphiteHair,
                OrbitTokens.bluePastel.copy(alpha = 0.20f),
            )
        },
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = OrbitTokens.bluePastel.copy(alpha = if (ativo) 0.45f else 0.25f),
                spotColor = Color.Black.copy(alpha = 0.50f),
            )
            .size(54.dp)
            .clip(CircleShape)
            .background(if (ativo) OrbitTokens.graphiteSurf else OrbitTokens.graphiteRaised)
            .border(1.dp, bordaGradiente, CircleShape)
            .orbitPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Aura interna sutil
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (ativo) OrbitTokens.bluePastel.copy(alpha = 0.25f)
                    else OrbitTokens.bluePastel.copy(alpha = 0.14f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (ativo) Icons.Rounded.Close else Icons.Rounded.Nightlight,
                contentDescription = "Luna Finanças",
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
