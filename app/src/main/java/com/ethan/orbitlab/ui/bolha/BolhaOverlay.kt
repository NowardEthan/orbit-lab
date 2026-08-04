package com.ethan.orbitlab.ui.bolha

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ethan.orbitlab.data.voice.VoiceClip
import com.ethan.orbitlab.data.voice.VoiceRecorder
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private val BolhaAura = 40.dp
private val BolhaIcone = 22.dp
private val BolhaTamanhoDp = 56.dp
private val BolhaFolgaSombraDp = 12.dp

/**
 * Overlay da bolha (B1–B5).
 *
 * Idle WRAP · arraste tela cheia + Guardar · toque = quick reply ·
 * expandir = painel completo · badge/pensando/cota no FAB.
 */
@Composable
fun BolhaOverlay(
    telaCheia: Boolean,
    offsetX: Int,
    offsetY: Int,
    sobreDismiss: Boolean,
    quickAberto: Boolean,
    badge: Boolean,
    pensando: Boolean,
    alertaCota: Boolean,
    enterNonce: Int,
    hapticsLigados: Boolean,
    onDragStart: () -> Unit,
    onArrastar: (dx: Int, dy: Int) -> Unit,
    onSoltar: () -> Unit,
    onTocar: () -> Unit,
    onAbrirPainel: (rascunho: String) -> Unit,
    onFecharQuick: () -> Unit,
    onEnviarQuick: (String) -> Unit,
    onEnviarAudioQuick: (VoiceClip) -> Unit,
) {
    if (telaCheia) {
        Box(Modifier.fillMaxSize()) {
            ZonaDismiss(ativa = sobreDismiss)
            ConteudoBolha(
                modifier = Modifier.offset { IntOffset(offsetX, offsetY) },
                quickAberto = quickAberto,
                badge = badge,
                pensando = pensando,
                alertaCota = alertaCota,
                enterNonce = enterNonce,
                hapticsLigados = hapticsLigados,
                onDragStart = onDragStart,
                onArrastar = onArrastar,
                onSoltar = onSoltar,
                onTocar = onTocar,
                onAbrirPainel = onAbrirPainel,
                onFecharQuick = onFecharQuick,
                onEnviarQuick = onEnviarQuick,
                onEnviarAudioQuick = onEnviarAudioQuick,
            )
        }
    } else {
        ConteudoBolha(
            modifier = Modifier,
            quickAberto = quickAberto,
            badge = badge,
            pensando = pensando,
            alertaCota = alertaCota,
            enterNonce = enterNonce,
            hapticsLigados = hapticsLigados,
            onDragStart = onDragStart,
            onArrastar = onArrastar,
            onSoltar = onSoltar,
            onTocar = onTocar,
            onAbrirPainel = onAbrirPainel,
            onFecharQuick = onFecharQuick,
            onEnviarQuick = onEnviarQuick,
            onEnviarAudioQuick = onEnviarAudioQuick,
        )
    }
}

@Composable
private fun ConteudoBolha(
    modifier: Modifier,
    quickAberto: Boolean,
    badge: Boolean,
    pensando: Boolean,
    alertaCota: Boolean,
    enterNonce: Int,
    hapticsLigados: Boolean,
    onDragStart: () -> Unit,
    onArrastar: (dx: Int, dy: Int) -> Unit,
    onSoltar: () -> Unit,
    onTocar: () -> Unit,
    onAbrirPainel: (rascunho: String) -> Unit,
    onFecharQuick: () -> Unit,
    onEnviarQuick: (String) -> Unit,
    onEnviarAudioQuick: (VoiceClip) -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        if (quickAberto) {
            QuickComposer(
                onEnviar = onEnviarQuick,
                onEnviarAudio = onEnviarAudioQuick,
                onExpandir = onAbrirPainel,
                onFechar = onFecharQuick,
            )
            Spacer(Modifier.height(8.dp))
        }
        BolhaFab(
            badge = badge,
            pensando = pensando,
            alertaCota = alertaCota,
            enterNonce = enterNonce,
            hapticsLigados = hapticsLigados,
            onDragStart = onDragStart,
            onArrastar = onArrastar,
            onSoltar = onSoltar,
            onTocar = onTocar,
            onAbrirPainel = { onAbrirPainel("") },
        )
    }
}

@Composable
private fun QuickComposer(
    onEnviar: (String) -> Unit,
    onEnviarAudio: (VoiceClip) -> Unit,
    onExpandir: (rascunho: String) -> Unit,
    onFechar: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val recorder = remember(context) { VoiceRecorder(context.applicationContext) }
    DisposableEffect(recorder) {
        onDispose { recorder.cancel() }
    }
    var texto by remember { mutableStateOf("") }
    var gravando by remember { mutableStateOf(false) }

    fun temMic(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    Row(
        Modifier
            .padding(end = BolhaFolgaSombraDp)
            .widthIn(max = 280.dp)
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(22.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = texto,
            onValueChange = { texto = it },
            enabled = !gravando,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = 14.sp),
            cursorBrush = SolidColor(OrbitTokens.bluePastel),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (texto.isNotBlank()) {
                        onEnviar(texto)
                        texto = ""
                    }
                },
            ),
            decorationBox = { inner ->
                Box {
                    when {
                        gravando -> Text(
                            "Gravando…",
                            color = OrbitTokens.bluePastel,
                            fontSize = 14.sp,
                        )
                        texto.isEmpty() -> Text(
                            "Fala rápido…",
                            color = OrbitTokens.textLowN,
                            fontSize = 14.sp,
                        )
                    }
                    if (!gravando) inner()
                }
            },
        )
        Icon(
            Icons.Rounded.OpenInFull,
            contentDescription = "Abrir painel",
            tint = OrbitTokens.textMidN,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .orbitPressable(onClick = { onExpandir(texto) })
                .padding(6.dp),
        )
        if (texto.isNotBlank()) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.bluePastel)
                    .orbitPressable(onClick = {
                        onEnviar(texto)
                        texto = ""
                    }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Enviar",
                    tint = OrbitTokens.onBluePastel,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (gravando) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised,
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (!temMic()) {
                                    Toast.makeText(
                                        context,
                                        "Abre o painel pra liberar o microfone.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    onExpandir("")
                                    return@detectTapGestures
                                }
                                if (!recorder.start()) {
                                    Toast.makeText(
                                        context,
                                        "Não deu pra gravar. Tenta de novo.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@detectTapGestures
                                }
                                gravando = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val soltou = tryAwaitRelease()
                                gravando = false
                                if (!soltou) {
                                    recorder.cancel()
                                    return@detectTapGestures
                                }
                                val clip = recorder.finish()
                                if (clip == null) {
                                    Toast.makeText(
                                        context,
                                        "Áudio curto demais — segura um pouco mais.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    onEnviarAudio(clip)
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = "Segurar para gravar",
                    tint = if (gravando) OrbitTokens.onBluePastel else OrbitTokens.textMidN,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Fechar",
            tint = OrbitTokens.textLowN,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .orbitPressable(onClick = onFechar)
                .padding(5.dp),
        )
    }
}

@Composable
private fun ZonaDismiss(ativa: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (ativa) 1f else 0.55f,
        animationSpec = OrbitMotion.tweenFast,
        label = "dismissZone",
    )
    val escala by animateFloatAsState(
        targetValue = if (ativa) 1.06f else 1f,
        animationSpec = OrbitMotion.springSoft,
        label = "dismissScale",
    )
    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = 28.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                this.alpha = alpha
                scaleX = escala
                scaleY = escala
            },
        ) {
            Box(
                Modifier
                    .size(if (ativa) 56.dp else 48.dp)
                    .shadow(
                        elevation = if (ativa) 12.dp else 6.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = OrbitTokens.danger.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.4f),
                    )
                    .clip(CircleShape)
                    .background(
                        if (ativa) OrbitTokens.danger.copy(alpha = 0.92f)
                        else OrbitTokens.graphiteRaised.copy(alpha = 0.92f),
                    )
                    .border(
                        1.dp,
                        if (ativa) OrbitTokens.danger else OrbitTokens.graphiteHair,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    tint = if (ativa) Color.White else OrbitTokens.textMidN,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                if (ativa) "Solta pra guardar" else "Guardar",
                color = OrbitTokens.textHiN,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Box(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BolhaFab(
    badge: Boolean,
    pensando: Boolean,
    alertaCota: Boolean,
    enterNonce: Int,
    hapticsLigados: Boolean,
    onDragStart: () -> Unit,
    onArrastar: (dx: Int, dy: Int) -> Unit,
    onSoltar: () -> Unit,
    onTocar: () -> Unit,
    onAbrirPainel: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val bordaGradiente = Brush.horizontalGradient(
        listOf(
            OrbitTokens.bluePastel,
            OrbitTokens.bluePastel.copy(alpha = 0.45f),
        ),
    )

    key(enterNonce) {
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

        val pulso = if (pensando) {
            val inf = rememberInfiniteTransition(label = "pensando")
            inf.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.85f,
                animationSpec = infiniteRepeatable(
                    tween(900, easing = LinearEasing),
                    RepeatMode.Reverse,
                ),
                label = "anel",
            ).value
        } else {
            0f
        }

        val enterScale = 0.88f + 0.12f * enter.value
        val escala = enterScale * pressScale.value

        Box(modifier = Modifier.padding(BolhaFolgaSombraDp)) {
            Box(
                modifier = Modifier
                    .size(BolhaTamanhoDp)
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                        alpha = enter.value
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = OrbitTokens.bluePastel.copy(alpha = 0.45f),
                        spotColor = Color.Black.copy(alpha = 0.55f),
                    )
                    .clip(CircleShape)
                    .background(OrbitTokens.graphiteRaised)
                    .border(
                        width = if (pensando) 2.dp else 1.dp,
                        brush = if (pensando) {
                            Brush.sweepGradient(
                                listOf(
                                    OrbitTokens.bluePastel.copy(alpha = pulso),
                                    OrbitTokens.bluePastel.copy(alpha = 0.15f),
                                    OrbitTokens.bluePastel.copy(alpha = pulso),
                                ),
                            )
                        } else {
                            bordaGradiente
                        },
                        shape = CircleShape,
                    )
                    // Um único detector: toque vs long-press vs arraste.
                    // Dois pointerInput (tap + drag) competiam e o longPress engolia o arraste.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            pressionado = true
                            var tratado = false
                            val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                            try {
                                withTimeout(longPressTimeout) {
                                    val passouSlop = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                        change.consume()
                                    }
                                    if (passouSlop != null) {
                                        tratado = true
                                        onDragStart()
                                        try {
                                            drag(down.id) { change ->
                                                val delta = change.positionChange()
                                                onArrastar(
                                                    delta.x.roundToInt(),
                                                    delta.y.roundToInt(),
                                                )
                                                change.consume()
                                            }
                                        } finally {
                                            onSoltar()
                                        }
                                    }
                                    // null = soltou antes do slop → toque (após o withTimeout)
                                }
                            } catch (_: TimeoutCancellationException) {
                                // Parado até o timeout → long-press
                                tratado = true
                                if (hapticsLigados) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onAbrirPainel()
                                waitForUpOrCancellation()
                            } finally {
                                pressionado = false
                            }

                            if (!tratado) {
                                if (hapticsLigados) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onTocar()
                            }
                        }
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
            if (badge || alertaCota) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (alertaCota) OrbitTokens.danger else OrbitTokens.bluePastel)
                        .border(1.5.dp, OrbitTokens.graphiteRaised, CircleShape),
                )
            }
        }
    }
}
