package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Reasoning da Luna — quieto, recolhível.
 * Open/close no idioma do menu «+» (expand rápido, close mais rápido).
 */
@Composable
fun LunaReasoning(
    texto: String,
    duracaoLabel: String? = null,
    inicialmenteAberto: Boolean = false,
    expandidoControlado: Boolean? = null,
    clicavel: Boolean = true,
    @Suppress("UNUSED_PARAMETER") animarTamanho: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var abertoInterno by remember { mutableStateOf(inicialmenteAberto) }
    val aberto = expandidoControlado ?: abertoInterno

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (clicavel && expandidoControlado == null) {
                        Modifier.orbitPressable { abertoInterno = !abertoInterno }
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (aberto) {
                    Icons.Rounded.KeyboardArrowDown
                } else {
                    Icons.Rounded.KeyboardArrowRight
                },
                contentDescription = if (aberto) "Recolher raciocínio" else "Ver raciocínio",
                tint = OrbitTokens.textLow,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = if (expandidoControlado == true && duracaoLabel == null) {
                    "Raciocinando"
                } else {
                    "Raciocínio"
                },
                color = OrbitTokens.textMid,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            if (duracaoLabel != null) {
                Text(
                    text = "· $duracaoLabel",
                    color = OrbitTokens.textLow,
                    fontSize = 12.sp,
                )
            }
        }

        AnimatedVisibility(
            visible = aberto,
            enter = OrbitMotion.expandEnter(),
            exit = OrbitMotion.expandExit(),
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrbitTokens.ink1)
                    .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(10.dp)),
            ) {
                Box(
                    Modifier
                        .width(2.5.dp)
                        .fillMaxHeight()
                        .background(OrbitTokens.accent.copy(alpha = 0.35f)),
                )
                Text(
                    text = texto,
                    color = OrbitTokens.textMid,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
fun LunaReasoningPensando(
    modifier: Modifier = Modifier,
    label: String = "Pensando…",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Três bolinhas pulsando em onda (typing indicator) — não mais estático.
        val transicao = rememberInfiniteTransition(label = "pensando")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                val alpha by transicao.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 620, delayMillis = i * 160),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dot$i",
                )
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(OrbitTokens.accent.copy(alpha = alpha)),
                )
            }
        }
        Text(
            text = label,
            color = OrbitTokens.textMid,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
