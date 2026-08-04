package com.ethan.orbitlab.ui.bolha

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.billing.PlanosNav
import com.ethan.orbitlab.data.billing.UsageRepository
import com.ethan.orbitlab.data.lunaMessageIdForUser
import com.ethan.orbitlab.data.lunaapi.LunaApiChat
import com.ethan.orbitlab.data.newUserMessageId
import com.ethan.orbitlab.ui.chat.ChatInputArea
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.ethan.orbitlab.ui.chat.LunaActionTimeline
import com.ethan.orbitlab.ui.chat.LunaMarkdown
import com.ethan.orbitlab.ui.chat.LunaMarkdownVariante
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import com.ethan.orbitlab.ui.chat.MessageAttachments
import com.ethan.orbitlab.ui.chat.ThreadReference
import com.ethan.orbitlab.ui.planos.LimiteAtingidoCard
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/**
 * Painel de conversa da bolha — hospedado em [BolhaPainelActivity] (Activity translúcida)
 * pra reutilizar o [ChatInputArea] completo (anexos, galeria, câmera, áudio, modo).
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
) {
    val appContext = LocalContext.current.applicationContext as Application
    val conversaId = remember { ChatRepository.conversaPrincipal() }
    val conversa by ChatRepository.observarConversa(conversaId).collectAsState(initial = null)
    val mensagens = conversa?.mensagens.orEmpty()

    val streamRepo by ChatRepository.streamDaConversa(conversaId).collectAsState()
    val streamState = remember { mutableStateOf<LunaStreamEstado>(LunaStreamEstado.Idle) }
    LaunchedEffect(streamRepo) { streamState.value = streamRepo }
    var streamLunaMsgId by remember { mutableStateOf(ChatRepository.lunaMsgIdEmVoo(conversaId)) }
    val listState = rememberLazyListState()
    val ocioso = streamState.value is LunaStreamEstado.Idle &&
        !ChatRepository.turnoEmAndamento(conversaId)
    val cotaBloqueada by UsageRepository.bloqueado.collectAsState()
    val usageCota by UsageRepository.usage.collectAsState()
    val semSaldo = !usageCota.loading && !usageCota.ilimitado && !usageCota.temSaldoParaChat
    val paredeCota = cotaBloqueada || semSaldo

    LaunchedEffect(mensagens.size, streamState.value) {
        val ultimo = mensagens.lastIndex
        if (ultimo >= 0) listState.animateScrollToItem(ultimo)
    }

    fun enviar(
        texto: String,
        anexos: List<ComposerAttachment> = emptyList(),
        reference: ThreadReference? = null,
    ) {
        val limpo = texto.trim()
        if (limpo.isEmpty() && anexos.isEmpty() && reference == null) return
        if (!ocioso || paredeCota) return
        val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
        val userMsgId = newUserMessageId()
        val lunaMsgId = lunaMessageIdForUser(userMsgId)
        ChatRepository.enviarMensagem(
            conversaId = conversaId,
            texto = limpo,
            isLuna = false,
            messageId = userMsgId,
            attachments = anexos,
            reference = reference,
        )
        streamLunaMsgId = lunaMsgId
        streamState.value = LunaStreamEstado.Raciocinando("")
        val onEstado: (LunaStreamEstado) -> Unit = { e ->
            ChatRepository.publicarStream(conversaId, e)
            streamState.value = e
        }
        ChatRepository.launchTurno(conversaId, lunaMsgId) {
            try {
                val r = LunaApiChat.responder(
                    context = appContext,
                    conversaId = conversaId,
                    historico = historicoAntes,
                    textoUsuario = limpo,
                    anexos = anexos,
                    reference = reference,
                    userMessageId = userMsgId,
                    lunaMessageId = lunaMsgId,
                    onEstado = onEstado,
                )
                if (r.cotaEsgotada) return@launchTurno
                ChatRepository.enviarMensagem(
                    conversaId = conversaId,
                    texto = r.resposta,
                    isLuna = true,
                    reasoning = r.reasoning.takeIf { it.isNotBlank() && !r.erro },
                    reasoningDuracao = r.reasoningDuracao.takeIf { it.isNotBlank() && !r.erro },
                    actionRun = if (r.erro) null else r.actionRun,
                    messageId = lunaMsgId,
                    persistirNuvem = !r.erro,
                    erro = r.erro,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ChatRepository.enviarMensagem(
                    conversaId = conversaId,
                    texto = "Erro ao falar com o servidor Luna: ${e.message ?: e.javaClass.simpleName}",
                    isLuna = true,
                    messageId = lunaMsgId,
                    persistirNuvem = false,
                    erro = true,
                )
            } finally {
                streamState.value = LunaStreamEstado.Idle
                streamLunaMsgId = null
            }
        }
    }

    val scope = rememberCoroutineScope()
    val densidade = LocalDensity.current
    val progresso = remember { Animatable(0f) }
    var sheetSize by remember { mutableStateOf(Offset.Zero) }
    var sheetPos by remember { mutableStateOf(Offset.Zero) }
    var fechando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Um pouco mais longo que o enter padrão — o morph do FAB precisa respirar.
        progresso.animateTo(1f, tween(OrbitMotion.msMed + 90))
    }

    fun fecharAnimado(depois: () -> Unit = onFechar) {
        if (fechando) return
        fechando = true
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
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
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
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = OrbitMetrics.pagePadding,
                            vertical = 6.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            mensagens.filter { it.id != streamLunaMsgId },
                            key = { it.id },
                        ) { msg -> BalaoPainel(msg) }
                        if (!ocioso) {
                            item(key = "stream") { StatusStreamPainel(streamState.value) }
                        }
                    }
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
                        onSend = { t, a, r -> enviar(t, a, r) },
                        streamState = streamState,
                        containerColor = OrbitTokens.graphiteRaised,
                        exibirSeletorModo = true,
                    )
                }
            }
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

@Composable
private fun BalaoPainel(msg: Mensagem) {
    val luna = msg.isLuna
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (luna) Alignment.Start else Alignment.End,
    ) {
        msg.actionRun?.let { run ->
            LunaActionTimeline(
                run = run,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .widthIn(max = 320.dp),
            )
        }
        val temAnexos = !luna && msg.attachments.isNotEmpty()
        val temTexto = msg.texto.isNotBlank()
        if (temAnexos) {
            MessageAttachments(
                attachments = msg.attachments,
                solo = temAnexos && !temTexto,
                modifier = Modifier.align(Alignment.End),
            )
            if (temTexto) Spacer(Modifier.height(6.dp))
        }
        if (temTexto || msg.erro) {
            Box(
                Modifier
                    .widthIn(max = 320.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (luna) 4.dp else 16.dp,
                            bottomEnd = if (luna) 16.dp else 4.dp,
                        ),
                    )
                    .background(if (luna) OrbitTokens.bubbleLuna else OrbitTokens.bubbleUser)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (luna && !msg.erro) {
                    LunaMarkdown(content = msg.texto, variante = LunaMarkdownVariante.Chat)
                } else {
                    Text(
                        msg.texto,
                        color = if (msg.erro) OrbitTokens.danger else OrbitTokens.textHiN,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusStreamPainel(estado: LunaStreamEstado) {
    val label = when (estado) {
        is LunaStreamEstado.Idle -> return
        is LunaStreamEstado.Pesquisando -> estado.liveLabel
        is LunaStreamEstado.Raciocinando -> estado.fase.ifBlank { "Pensando…" }
        is LunaStreamEstado.Respondendo -> "Escrevendo…"
    }
    val parcial = (estado as? LunaStreamEstado.Respondendo)?.respostaParcial.orEmpty()
    val run = when (estado) {
        is LunaStreamEstado.Pesquisando -> estado.run
        is LunaStreamEstado.Raciocinando -> estado.actionRun
        is LunaStreamEstado.Respondendo -> estado.actionRun
        else -> null
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        run?.let {
            LunaActionTimeline(
                run = it,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .widthIn(max = 320.dp),
            )
        }
        Text(
            label,
            color = OrbitTokens.bluePastel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        if (parcial.isNotBlank()) {
            Box(
                Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    .background(OrbitTokens.bubbleLuna)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                LunaMarkdown(content = parcial, variante = LunaMarkdownVariante.Chat)
            }
        }
    }
}
