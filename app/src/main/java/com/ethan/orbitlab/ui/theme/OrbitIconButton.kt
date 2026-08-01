package com.ethan.orbitlab.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Botão de ícone minimalista — só o glifo sobre o grafite, sem moldura circular nem fundo.
 * A área de toque (34dp) fica discreta; o ripple do [orbitPressable] dá o retorno.
 * É o padrão único de todos os headers/topbars do 1.0 (nada de círculo com borda).
 */
@Composable
fun OrbitIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = OrbitTokens.textMidN,
    iconSize: Dp = 20.dp,
    tap: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(tap)
            .clip(CircleShape)
            .orbitPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
