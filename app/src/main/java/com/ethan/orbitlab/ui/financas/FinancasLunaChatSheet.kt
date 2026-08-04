package com.ethan.orbitlab.ui.financas

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.lunaMessageIdForUser
import com.ethan.orbitlab.data.lunaapi.LunaApiChat
import com.ethan.orbitlab.data.newUserMessageId
import com.ethan.orbitlab.ui.chat.ChatInputArea
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.ethan.orbitlab.ui.chat.LunaActionTimeline
import com.ethan.orbitlab.ui.chat.LunaMarkdown
import com.ethan.orbitlab.ui.chat.LunaMarkdownVariante
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import com.ethan.orbitlab.ui.chat.ThreadReference
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay

/**
 * Chat da Luna nas Finanças — painel inferior (~88%) na mesma janela da Activity
 * (não [ModalBottomSheet]: o IME não empurrava o composer lá).
 * Reusa a conversa «Finanças», o [ChatInputArea] e o [LunaMarkdown] do chat.
 */
@Composable
fun FinancasLunaChatSheet(
    conversaId: String,
    onDismiss: () -> Unit,
    onAbrirFullscreen: () -> Unit = {},
) {
    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize()) {
        // Scrim — toque fora fecha.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .orbitPressable(onClick = onDismiss),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .imePadding()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(OrbitTokens.graphiteSurf),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(OrbitTokens.textLowN.copy(alpha = 0.45f)),
            )
            FinancasLunaChatConteudo(
                conversaId = conversaId,
                onDismiss = onDismiss,
                onAbrirFullscreen = onAbrirFullscreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun FinancasLunaChatConteudo(
    conversaId: String,
    onDismiss: () -> Unit,
    onAbrirFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext as Application
    val conversa by ChatRepository.observarConversa(conversaId).collectAsState(initial = null)
    val mensagens = conversa?.mensagens.orEmpty()
    // Fonte da verdade do turno: ChatRepository (sobrevive fechar sheet / abrir widget).
    val streamState = remember { mutableStateOf<LunaStreamEstado>(LunaStreamEstado.Idle) }
    val streamRepo by ChatRepository.streamDaConversa(conversaId).collectAsState()
    LaunchedEffect(streamRepo) {
        streamState.value = streamRepo
    }
    var streamLunaMsgId by remember { mutableStateOf(ChatRepository.lunaMsgIdEmVoo(conversaId)) }
    LaunchedEffect(conversaId) {
        streamLunaMsgId = ChatRepository.lunaMsgIdEmVoo(conversaId)
    }
    val listState = rememberLazyListState()
    val ocioso = streamState.value is LunaStreamEstado.Idle &&
        !ChatRepository.turnoEmAndamento(conversaId)

    LaunchedEffect(mensagens.size, streamState.value) {
        val ultimo = mensagens.lastIndex
        if (ultimo >= 0) {
            listState.animateScrollToItem(ultimo)
        }
    }

    // Gera a resposta sem reenviar a fala do usuário (onRetry / auto-retry usam isto).
    val dispararResposta = remember(conversaId, streamState, appContext) {
        { textoEnvio: String,
          anexos: List<ComposerAttachment>,
          reference: ThreadReference?,
          historicoAntes: List<Mensagem>,
          userMsgId: String,
          lunaMsgId: String,
          reenvio: Boolean ->
            streamLunaMsgId = lunaMsgId
            streamState.value = LunaStreamEstado.Raciocinando("")
            val onEstado: (LunaStreamEstado) -> Unit = { e ->
                if (ChatRepository.publicarStream(conversaId, e)) {
                    streamState.value = e
                }
            }
            val iniciou = ChatRepository.launchTurno(conversaId, lunaMsgId) {
                try {
                    val resultado = LunaApiChat.responder(
                        context = appContext,
                        conversaId = conversaId,
                        historico = historicoAntes,
                        textoUsuario = textoEnvio,
                        anexos = anexos,
                        reference = reference,
                        userMessageId = userMsgId,
                        lunaMessageId = lunaMsgId,
                        reenvio = reenvio,
                        onEstado = onEstado,
                    )
                    ChatRepository.enviarMensagem(
                        conversaId = conversaId,
                        texto = resultado.resposta,
                        isLuna = true,
                        reasoning = resultado.reasoning.takeIf { it.isNotBlank() && !resultado.erro },
                        reasoningDuracao = resultado.reasoningDuracao
                            .takeIf { it.isNotBlank() && !resultado.erro },
                        actionRun = if (resultado.erro) null else resultado.actionRun,
                        messageId = lunaMsgId,
                        persistirNuvem = !resultado.erro,
                        erro = resultado.erro,
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
            // Já havia turno ativo (ex.: saiu do sheet e voltou) — só religa o stream, sem 2º HTTP.
            if (!iniciou) {
                streamLunaMsgId = ChatRepository.lunaMsgIdEmVoo(conversaId) ?: lunaMsgId
                streamState.value = ChatRepository.streamDaConversa(conversaId).value
            }
            Unit
        }
    }

    fun enviar(
        texto: String,
        anexos: List<ComposerAttachment> = emptyList(),
        reference: ThreadReference? = null,
    ) {
        val textoEnvio = texto.trim()
        if (textoEnvio.isEmpty() && anexos.isEmpty() && reference == null) return
        if (!ocioso) return
        val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
        val userMsgId = newUserMessageId()
        val lunaMsgId = lunaMessageIdForUser(userMsgId)
        ChatRepository.enviarMensagem(
            conversaId = conversaId,
            texto = textoEnvio,
            isLuna = false,
            messageId = userMsgId,
            attachments = anexos,
            reference = reference,
        )
        dispararResposta(textoEnvio, anexos, reference, historicoAntes, userMsgId, lunaMsgId, false)
    }

    // Retoma sem duplicar: última msg sua órfã, ou erro da Luna.
    val onRetry = remember(dispararResposta, conversaId) {
        {
            if (!ChatRepository.turnoEmAndamento(conversaId)) {
                val msgs = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
                val ultima = msgs.lastOrNull()
                if (ultima != null && streamState.value is LunaStreamEstado.Idle) {
                    val alvo: Triple<Mensagem, List<Mensagem>, String?>? = when {
                        !ultima.isLuna -> Triple(ultima, msgs.dropLast(1), null)
                        ultima.erro || financasRespostaEhErro(ultima.texto) -> {
                            val userAntes = msgs.dropLast(1).lastOrNull { !it.isLuna }
                            userAntes?.let { u ->
                                val idxUser = msgs.indexOfFirst { it.id == u.id }
                                Triple(u, msgs.subList(0, idxUser).toList(), ultima.id)
                            }
                        }
                        else -> null
                    }
                    alvo?.let { (userMsg, historicoAntes, replyId) ->
                        val lunaMsgId = replyId ?: lunaMessageIdForUser(userMsg.id)
                        dispararResposta(
                            userMsg.texto,
                            emptyList(),
                            userMsg.reference,
                            historicoAntes,
                            userMsg.id,
                            lunaMsgId,
                            true,
                        )
                    }
                }
            }
            Unit
        }
    }

    // Turno interrompido deixa a mensagem dele sozinha — retoma após respirar.
    var autoRetomadoId by remember(conversaId) { mutableStateOf<String?>(null) }
    val ultimaMsg = conversa?.mensagens?.lastOrNull()
    LaunchedEffect(ultimaMsg?.id, ocioso) {
        val orfa = ultimaMsg ?: return@LaunchedEffect
        if (orfa.isLuna || !ocioso || autoRetomadoId == orfa.id) return@LaunchedEffect
        if (ChatRepository.turnoEmAndamento(conversaId)) return@LaunchedEffect
        if (System.currentTimeMillis() - orfa.timestamp > 30 * 60_000L) return@LaunchedEffect
        delay(1500)
        if (ChatRepository.turnoEmAndamento(conversaId)) return@LaunchedEffect
        val agora = ChatRepository.getConversa(conversaId)?.mensagens?.lastOrNull()
        if (agora == null || agora.id != orfa.id || agora.isLuna) return@LaunchedEffect
        if (streamState.value !is LunaStreamEstado.Idle) return@LaunchedEffect
        val lunaId = lunaMessageIdForUser(orfa.id)
        if (ChatRepository.respostaJaChegou(conversaId, lunaId) != null) return@LaunchedEffect
        autoRetomadoId = orfa.id
        onRetry()
    }

    val sugPrompts = listOf(
        "💰 Quanto gastei este mês?",
        "💳 Cria um cartão Nubank",
        "🎯 Meta de reserva R$ 1000",
        "✍️ Gastei 50 no almoço",
        "🔄 Lista as recorrentes",
    )

    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.bluePastel.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Nightlight,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Luna · Finanças",
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Registra, resume, transfere…",
                    color = OrbitTokens.textLowN,
                    fontSize = 12.sp,
                )
            }
            Icon(
                Icons.Rounded.OpenInFull,
                contentDescription = "Abrir em tela cheia",
                tint = OrbitTokens.textMidN,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .orbitPressable(onClick = onAbrirFullscreen)
                    .padding(8.dp),
            )
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Fechar",
                tint = OrbitTokens.textMidN,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .orbitPressable(onClick = onDismiss)
                    .padding(8.dp),
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (mensagens.isEmpty() && ocioso) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = OrbitMetrics.pagePadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Fala o que rolou",
                        color = OrbitTokens.textHiN,
                        fontSize = 18.sp,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "«gastei 32 no almoço», «quanto saiu este mês?», «transfere 200 pro crédito»",
                        color = OrbitTokens.textMidN,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Spacer(Modifier.height(18.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        items(sugPrompts) { prompt ->
                            val shapeChip = RoundedCornerShape(99.dp)
                            Box(
                                modifier = Modifier
                                    .clip(shapeChip)
                                    .background(OrbitTokens.graphiteRaised)
                                    .border(1.dp, OrbitTokens.graphiteHair, shapeChip)
                                    .orbitPressable { enviar(prompt) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    text = prompt,
                                    color = OrbitTokens.textHiN,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = OrbitMetrics.pagePadding,
                        end = OrbitMetrics.pagePadding,
                        top = 4.dp,
                        bottom = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        mensagens.filter { it.id != streamLunaMsgId },
                        key = { it.id },
                    ) { msg ->
                        BolhaFinancasChat(msg)
                    }
                    if (!ocioso) {
                        item(key = "stream") {
                            StatusStreamFinancas(streamState.value)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 12.dp),
        ) {
            ChatInputArea(
                onSend = { t, a, r -> enviar(t, a, r) },
                streamState = streamState,
                containerColor = OrbitTokens.graphiteRaised,
            )
        }
    }
}

/** Última resposta da Luna é um erro (pra retomar sem duplicar a fala do usuário)? */
private fun financasRespostaEhErro(texto: String): Boolean {
    val t = texto.trimStart()
    return t.startsWith("Não consegui responder") ||
        t.startsWith("Nao consegui responder") ||
        t.startsWith("Erro ao falar") ||
        t.startsWith("Erro ao") ||
        t.startsWith("Erro no servidor")
}

@Composable
private fun BolhaFinancasChat(msg: Mensagem) {
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
                LunaMarkdown(
                    content = msg.texto,
                    variante = LunaMarkdownVariante.Chat,
                )
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

@Composable
private fun StatusStreamFinancas(estado: LunaStreamEstado) {
    val label = when (estado) {
        is LunaStreamEstado.Idle -> return
        is LunaStreamEstado.Pesquisando -> estado.liveLabel
        is LunaStreamEstado.Raciocinando ->
            estado.fase.ifBlank { "Pensando…" }
        is LunaStreamEstado.Respondendo -> "Escrevendo…"
    }
    val parcial = when (estado) {
        is LunaStreamEstado.Respondendo -> estado.respostaParcial
        else -> ""
    }
    estado.let { e ->
        val run = when (e) {
            is LunaStreamEstado.Pesquisando -> e.run
            is LunaStreamEstado.Raciocinando -> e.actionRun
            is LunaStreamEstado.Respondendo -> e.actionRun
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
                    LunaMarkdown(
                        content = parcial,
                        variante = LunaMarkdownVariante.Chat,
                    )
                }
            }
        }
    }
}
