package com.ethan.orbitlab.ui.bolha

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlin.math.roundToInt

private val BolhaTamanho = 44.dp
private val BolhaAura = 30.dp
private val BolhaIcone = 18.dp
private val BolhaFolgaSombra = 10.dp

/**
 * O que se vê no overlay: a bolha da Luna.
 *
 * - arrastar → move a janela (o serviço reposiciona o [android.view.WindowManager]);
 * - soltar → gruda na borda mais perto;
 * - tocar → abre a Luna;
 * - toque longo → guarda a bolha (fecha o serviço).
 *
 * Compacta (44dp) com press spring e enter curto — idioma [OrbitMotion].
 */
@Composable
fun BolhaOverlay(
    onArrastar: (dx: Int, dy: Int) -> Unit,
    onSoltar: () -> Unit,
    onTocar: () -> Unit,
    onFechar: () -> Unit,
) {
    val bordaGradiente = Brush.horizontalGradient(
        listOf(
            OrbitTokens.bluePastel,
            OrbitTokens.bluePastel.copy(alpha = 0.45f),
        ),
    )

    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enter.animateTo(1f, OrbitMotion.tweenMed)
    }

    var pressionado by remember { mutableStateOf(false) }
    val pressScale = remember { Animatable(1f) }
    LaunchedEffect(pressionado) {
        pressScale.animateTo(
            targetValue = if (pressionado) OrbitMotion.pressScale else 1f,
            animationSpec = OrbitMotion.springPress,
        )
    }

    val enterScale = 0.88f + 0.12f * enter.value
    val escala = enterScale * pressScale.value

    Box(modifier = Modifier.padding(BolhaFolgaSombra)) {
        Box(
            modifier = Modifier
                .size(BolhaTamanho)
                .graphicsLayer {
                    scaleX = escala
                    scaleY = escala
                    alpha = enter.value
                }
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = OrbitTokens.bluePastel.copy(alpha = 0.38f),
                    spotColor = Color.Black.copy(alpha = 0.48f),
                )
                .clip(CircleShape)
                .background(OrbitTokens.graphiteRaised)
                .border(1.dp, bordaGradiente, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressionado = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                pressionado = false
                            }
                        },
                        onTap = { onTocar() },
                        onLongPress = { onFechar() },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { pressionado = true },
                        onDragEnd = {
                            pressionado = false
                            onSoltar()
                        },
                        onDragCancel = { pressionado = false },
                        onDrag = { change, drag ->
                            change.consume()
                            onArrastar(drag.x.roundToInt(), drag.y.roundToInt())
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(BolhaAura)
                    .clip(CircleShape)
                    .background(OrbitTokens.bluePastel.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Nightlight,
                    contentDescription = "Falar com a Luna",
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(BolhaIcone),
                )
            }
        }
    }
}
