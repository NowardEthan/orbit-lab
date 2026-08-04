package com.ethan.orbitlab.ui.bolha

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.billing.PlanosNav
import com.ethan.orbitlab.data.billing.UsageRepository
import com.ethan.orbitlab.ui.chat.ChatInputArea
import com.ethan.orbitlab.ui.chat.ChatTimeline
import com.ethan.orbitlab.ui.chat.MessageActionSheet
import com.ethan.orbitlab.ui.chat.ThreadReference
import com.ethan.orbitlab.ui.chat.buildImageReference
import com.ethan.orbitlab.ui.chat.buildMessageReference
import com.ethan.orbitlab.ui.chat.referenciaAoPuxar
import com.ethan.orbitlab.ui.chat.rememberChatTurno
import com.ethan.orbitlab.ui.planos.LimiteAtingidoCard
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch

/**
 * Painel de conversa da bolha — hospedado em [BolhaPainelActivity] (Activity translúcida)
 * pra reutilizar o [ChatInputArea] completo e a mesma timeline do chat nativo.
 *
 * Continua a MESMA conversa principal ([ChatRepository.conversaPrincipal]).
 *
 * @param fabOrigemX centro X do FAB em coords de janela/tela (−1 = fallback).
 * @param fabOrigemY centro Y do FAB em coords de janela/tela (−1 = fallback).
 */
@Composable
fun BolhaLunaPainel(
    onFechar: () -> Unit,
    onAbrirNoApp: () -> Unit,
    fabOrigemX: Float = -1f,
    fabOrigemY: Float = -1f,
    rascunhoInicial: String = "",
) {
    val appContext = LocalContext.current.applicationContext as Application
    val conversaId = remember { ChatRepository.conversaPrincipal() }
    val conversa by ChatRepository.observarConversa(conversaId).collectAsState(initial = null)
    val mensagens = conversa?.mensagens.orEmpty()

    val turnoState = rememberChatTurno(conversaId = conversaId, appContext = appContext)
    val turno = turnoState.turno
    val streamState = turno.streamState
    val streamLunaMsgId = turnoState.streamLunaMsgId
    val perguntaAtiva = turno.perguntaAtiva
    val ocioso = turnoState.ocioso

    val cotaBloqueada by UsageRepository.bloqueado.collectAsState()
    val usageCota by UsageRepository.usage.collectAsState()
    val semSaldo = !usageCota.loading && !usageCota.ilimitado && !usageCota.temSaldoParaChat
    val paredeCota = cotaBloqueada || semSaldo

    var messageReference by remember { mutableStateOf<ThreadReference?>(null) }
    var actionSheetMsg by remember { mutableStateOf<Mensagem?>(null) }

    val scope = rememberCoroutineScope()
    val densidade = LocalDensity.current
    val teclado = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val progresso = remember { Animatable(0f) }
    var sheetSize by remember { mutableStateOf(Offset.Zero) }
    var sheetPos by remember { mutableStateOf(Offset.Zero) }
    var fechando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        progresso.animateTo(1f, tween(OrbitMotion.msMed + 90))
        BolhaSinal.limparBadge()
        mensagens.lastOrNull { it.isLuna }?.id?.let { PrefsRepository.setBolhaLastLunaMsgId(it) }
    }

    fun fecharAnimado(depois: () -> Unit = onFechar) {
        if (fechando) return
        fechando = true
        teclado?.hide()
        scope.launch {
            progresso.animateTo(0f, tween(OrbitMotion.msFast + 30))
            depois()
        }
    }

    val p = progresso.value
    val temOrigem = fabOrigemX >= 0f && fabOrigemY >= 0f
    val pivotX = if (temOrigem && sheetSize.x > 0f) {
        ((fabOrigemX - sheetPos.x) / sheetSize.x).coerceIn(0f, 1f)
    } else {
        0.5f
    }
    val pivotY = if (temOrigem && sheetSize.y > 0f) {
        ((fabOrigemY - sheetPos.y) / sheetSize.y).coerceIn(0f, 1f)
    } else {
        1f
    }
    val fabPx = with(densidade) { 56.dp.toPx() }
    val startScale = if (sheetSize.y > 0f) {
        (fabPx / sheetSize.y).coerceIn(0.06f, 0.22f)
    } else {
        0.12f
    }
    val scale = startScale + (1f - startScale) * p
    val cantoTopo = (40f - 18f * p).dp

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = p }
                .background(Color.Black.copy(alpha = 0.48f))
                .orbitPressable(enabled = !fechando, onClick = { fecharAnimado() }),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .statusBarsPadding()
                .imePadding()
                .navigationBarsPadding()
                .onGloballyPositioned { coords ->
                    sheetSize = Offset(
                        coords.size.width.toFloat(),
                        coords.size.height.toFloat(),
                    )
                    sheetPos = coords.positionInWindow()
                }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(pivotX, pivotY)
                    scaleX = scale
                    scaleY = scale
                    alpha = (0.35f + 0.65f * p).coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(topStart = cantoTopo, topEnd = cantoTopo))
                .background(OrbitTokens.graphiteSurf),
        ) {
            AlcaPainel()
            CabecalhoPainel(
                onFechar = { fecharAnimado() },
                onAbrirNoApp = { fecharAnimado(onAbrirNoApp) },
            )

            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (mensagens.isEmpty() && ocioso) {
                    VazioPainel()
                } else {
                    ChatTimeline(
                        conversaId = conversaId,
                        historico = mensagens,
                        streamState = streamState,
                        ocultarMessageId = streamLunaMsgId,
                        onRetry = turno.onRetry,
                        pergunta = perguntaAtiva.value,
                        onResponderPergunta = { opcao ->
                            perguntaAtiva.value = null
                            turno.onSend(opcao, emptyList(), null)
                        },
                        onMessageLongPress = { msg ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            actionSheetMsg = msg
                        },
                        onReferenciarMedia = { msg, att ->
                            val ref = buildImageReference(msg, mensagens, att)
                            if (ref != null) {
                                messageReference = ref
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onReferenciarPorSwipe = { msg ->
                            val ref = referenciaAoPuxar(msg, mensagens)
                            if (ref != null) {
                                messageReference = ref
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onRegerarAspecto = { msg, aspectoLabel, aspectoRatio ->
                            turno.regerarAspecto(msg, mensagens, aspectoLabel, aspectoRatio)
                        },
                    )
                }
            }

            if (paredeCota) {
                LimiteAtingidoCard(
                    usage = usageCota,
                    onVerPlanos = {
                        PlanosNav.abrir()
                        fecharAnimado(onAbrirNoApp)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OrbitMetrics.pagePadding)
                        .padding(bottom = 10.dp),
                ) {
                    ChatInputArea(
                        onSend = { t, a, r ->
                            turno.onSend(t, a, r)
                            messageReference = null
                        },
                        streamState = streamState,
                        messageReference = messageReference,
                        onClearReference = { messageReference = null },
                        containerColor = OrbitTokens.graphiteRaised,
                        textoInicial = rascunhoInicial,
                    )
                }
            }
        }

        val sheetMsg = actionSheetMsg
        if (sheetMsg != null) {
            MessageActionSheet(
                mensagem = sheetMsg,
                onDismiss = { actionSheetMsg = null },
                onReferenciarMensagem = {
                    val ref = buildMessageReference(sheetMsg, mensagens)
                    if (ref != null) {
                        messageReference = ref
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onReferenciarImagem = { att ->
                    val ref = buildImageReference(sheetMsg, mensagens, att)
                    if (ref != null) {
                        messageReference = ref
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onCopiar = {
                    if (sheetMsg.texto.isNotBlank()) {
                        clipboard.setText(AnnotatedString(sheetMsg.texto))
                    }
                },
                onReenviar = { turno.reenviarDesde(sheetMsg) },
            )
        }
    }
}

@Composable
private fun AlcaPainel() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(OrbitTokens.textLowN.copy(alpha = 0.45f)),
        )
    }
}

@Composable
private fun CabecalhoPainel(onFechar: () -> Unit, onAbrirNoApp: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(OrbitTokens.bluePastel.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Nightlight,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Luna",
                color = OrbitTokens.textHiN,
                fontSize = 16.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Flutuando · conversa principal",
                color = OrbitTokens.textLowN,
                fontSize = 11.sp,
            )
        }
        Icon(
            Icons.Rounded.OpenInFull,
            contentDescription = "Abrir no app",
            tint = OrbitTokens.textMidN,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .orbitPressable(onClick = onAbrirNoApp)
                .padding(8.dp),
        )
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Fechar",
            tint = OrbitTokens.textMidN,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .orbitPressable(onClick = onFechar)
                .padding(8.dp),
        )
    }
}

@Composable
private fun VazioPainel() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Tô aqui 🌙",
            color = OrbitTokens.textHiN,
            fontSize = 18.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Texto, foto, áudio ou arquivo — mesma conversa do app, sem sair do que você tá fazendo.",
            color = OrbitTokens.textMidN,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}
