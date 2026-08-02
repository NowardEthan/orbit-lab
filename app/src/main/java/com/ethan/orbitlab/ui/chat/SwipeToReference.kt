package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlinx.coroutines.launch

/**
 * Puxar-pra-referenciar, estilo WhatsApp.
 *
 * Arrasta a bolha pra a direita: um ícone de resposta acende atrás, do lado de início, e ao passar
 * o gatilho (com um tec de vibração) a mensagem vira a referência do composer — pra você perguntar
 * sobre AQUELE trecho ou AQUELA imagem sem copiar nada. Solta e a bolha volta sozinha ao lugar.
 *
 * Só reage a arrasto HORIZONTAL (o `detectHorizontalDragGestures` só captura depois do slop lateral),
 * então a rolagem vertical da conversa continua intacta. Fica desligado enquanto a folha de ações
 * está aberta (`enabled`), pra os dois gestos não brigarem.
 */
@Composable
fun SwipeToReference(
    enabled: Boolean,
    onReferenciar: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier) { content() }
        return
    }

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val gatilhoPx = with(density) { 64.dp.toPx() }
    val maxPx = with(density) { 92.dp.toPx() }
    val offset = remember { Animatable(0f) }
    var passouGatilho by remember { mutableStateOf(false) }

    Box(modifier) {
        // Ícone de resposta atrás da bolha, do lado de início; cresce e acende conforme puxa.
        val progresso = (offset.value / gatilhoPx).coerceIn(0f, 1f)
        Icon(
            Icons.AutoMirrored.Rounded.Reply,
            contentDescription = null,
            tint = OrbitTokens.accentText,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
                .size(22.dp)
                .graphicsLayer {
                    alpha = progresso
                    val escala = 0.6f + 0.4f * progresso
                    scaleX = escala
                    scaleY = escala
                },
        )

        Box(
            modifier = Modifier
                .graphicsLayer { translationX = offset.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { passouGatilho = false },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val novo = (offset.value + dragAmount).coerceIn(0f, maxPx)
                            scope.launch { offset.snapTo(novo) }
                            if (!passouGatilho && novo >= gatilhoPx) {
                                passouGatilho = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            if (offset.value >= gatilhoPx) onReferenciar()
                            scope.launch { offset.animateTo(0f) }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f) }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
