package com.ethan.orbitlab.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Motion OrbitLab — o idioma do botão «+».
 *
 * - Press: spring dura, scale ~0.90 (dá pra sentir)
 * - Abrir: fade + scale curto
 * - Fechar: mais rápido que abrir (assimétrico)
 * - Sem ripple Material; sem dual-tree nas abas
 */
object OrbitMotion {
    const val msInstant = 70
    const val msFast = 90
    const val msMed = 110
    const val msEnter = 90

    val springPress = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 14_000f,
    )

    val springSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    val springSoft = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 7_000f,
    )

    val tweenInstant = tween<Float>(durationMillis = msInstant)
    val tweenFast = tween<Float>(durationMillis = msFast)
    val tweenMed = tween<Float>(durationMillis = msMed)
    val tweenEnter = tween<Float>(durationMillis = msEnter)

    val enterSlideDp = 3.dp
    const val enterStaggerMaxMs = 0
    const val enterAlphaFrom = 0.92f

    /** Press padrão do app (FAB / tabs / cards). */
    const val pressScale = 0.90f

    /** Popover do «+» — nasce de baixo. */
    val popupOrigin = TransformOrigin(0.5f, 1f)

    fun popupEnter(): EnterTransition =
        scaleIn(
            animationSpec = tween(msFast),
            initialScale = 0.94f,
            transformOrigin = popupOrigin,
        ) + fadeIn(tweenFast)

    fun popupExit(): ExitTransition =
        scaleOut(
            animationSpec = tween(msInstant),
            targetScale = 0.94f,
            transformOrigin = popupOrigin,
        ) + fadeOut(tweenInstant)

    /** Overlay de tela cheia (chat / novidades) — só fade (scale full-screen é caro). */
    fun overlayEnter(): EnterTransition = fadeIn(tweenFast)

    fun overlayExit(): ExitTransition = fadeOut(tweenInstant)

    /** Painéis colapsáveis (reasoning / research). */
    fun expandEnter(): EnterTransition =
        expandVertically(tween(msFast)) + fadeIn(tweenFast)

    fun expandExit(): ExitTransition =
        shrinkVertically(tween(msInstant)) + fadeOut(tweenInstant)

    fun scrimEnter(): EnterTransition = fadeIn(tweenFast)
    fun scrimExit(): ExitTransition = fadeOut(tweenInstant)
}

/**
 * Enter leve só com fade curto (sem slide/spring — evita “elástico”).
 * Listas internas / overlays; **não** usar nas abas do shell.
 */
fun Modifier.orbitEnter(delayMillis: Int = 0): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.coerceAtMost(40).toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else OrbitMotion.enterAlphaFrom,
        animationSpec = OrbitMotion.tweenFast,
        label = "orbitEnterAlpha",
    )
    graphicsLayer { this.alpha = alpha }
}

/**
 * Troca de aba: só fade com **tween** (sem translationY — menos invalidate).
 *
 * Usa Animatable + snap: o `animateFloatAsState` antigo partia de alpha≈1
 * (estado da aba anterior), então o fade era invisível.
 */
fun Modifier.orbitTabReveal(key: Any): Modifier = composed {
    val alpha = remember(key) { Animatable(0f) }

    LaunchedEffect(key) {
        alpha.snapTo(0f)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(OrbitMotion.msMed + 30),
        )
    }

    graphicsLayer { this.alpha = alpha.value }
}

@Composable
fun rememberOrbitPressScale(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressedScale: Float = OrbitMotion.pressScale,
): Pair<MutableInteractionSource, Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = OrbitMotion.springPress,
        label = "orbitPressScale",
    )
    return interactionSource to scale
}

/** Clickable com press spring — sem ripple. */
fun Modifier.orbitPressable(
    enabled: Boolean = true,
    pressedScale: Float = OrbitMotion.pressScale,
    onClick: () -> Unit,
): Modifier = composed {
    val (interaction, scale) = rememberOrbitPressScale(pressedScale = pressedScale)
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}
