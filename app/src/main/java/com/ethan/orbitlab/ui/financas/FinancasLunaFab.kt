package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * FAB da Luna — aparece em todas as abas de Finanças.
 * Abre a conversa dedicada «Finanças» (tools/insights entram depois no chat).
 */
@Composable
fun FinancasLunaFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .shadow(10.dp, CircleShape, ambientColor = OrbitTokens.bluePastel.copy(alpha = 0.35f))
            .size(56.dp)
            .clip(CircleShape)
            .background(OrbitTokens.bluePastel)
            .orbitPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Nightlight,
            contentDescription = "Falar com a Luna sobre finanças",
            tint = OrbitTokens.onBluePastel,
            modifier = Modifier.size(26.dp),
        )
    }
}
