package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.lunaMessageIdForUser
import com.ethan.orbitlab.data.newUserMessageId
import com.ethan.orbitlab.data.lunaapi.LunaApiChat
import com.ethan.orbitlab.data.openrouter.LunaDirectChat
import com.ethan.orbitlab.ui.theme.OrbitFills
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.ethan.orbitlab.ui.theme.rememberOrbitPressScale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class RecordState { Idle, Recording, Locked }

private val localeBr = Locale("pt", "BR")
/** Raio = metade da altura mínima (48) → pílula em 1 linha; multi-linha vira retângulo arredondado. */
private val ComposerFieldShape = RoundedCornerShape(24.dp)
private val SendIdleBg = Color(0xFFF2F4F8)
private const val ComposerMaxLines = 6

private fun formatHoraMensagem(timestamp: Long): String =
    SimpleDateFormat("HH:mm", localeBr).format(Date(timestamp))

/** mm:ss a partir de um total de segundos. */
private fun formatTimer(totalSegundos: Int): String {
    val m = totalSegundos / 60
    val s = totalSegundos % 60
    return "%d:%02d".format(m, s)
}

@Composable
fun ChatScreen(conversaId: String, onBack: () -> Unit) {
    val conversa by ChatRepository.observarConversa(conversaId).collectAsState(initial = null)
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    // MutableState passado por referência — header não lê tokens do stream
    val streamState = remember { mutableStateOf<LunaStreamEstado>(LunaStreamEstado.Idle) }
    val lunaDirect by PrefsRepository.lunaDirectEnabled.collectAsState()
    /** Id da bolha Luna em voo (Railway) — some do histórico enquanto o draft streama. */
    var streamLunaMsgId by remember { mutableStateOf<String?>(null) }
    var messageReference by remember(conversaId) { mutableStateOf<ThreadReference?>(null) }
    var actionSheetMsg by remember { mutableStateOf<Mensagem?>(null) }
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

    val onSend = remember(conversaId, streamState, appContext, lunaDirect) {
        { texto: String, anexos: List<ComposerAttachment>, reference: ThreadReference? ->
            val textoEnvio = texto.trim()
            val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
            // Ids estáveis no Railway: o servidor persiste o mesmo par e não duplica.
            val userMsgId = if (lunaDirect) null else newUserMessageId()
            val lunaMsgId = userMsgId?.let { lunaMessageIdForUser(it) }
            ChatRepository.enviarMensagem(
                conversaId = conversaId,
                texto = textoEnvio,
                isLuna = false,
                attachments = anexos,
                reference = reference,
                messageId = userMsgId,
            )
            // Slot da Luna — “Pensando…” até a resposta completa
            streamLunaMsgId = lunaMsgId
            streamState.value = LunaStreamEstado.Raciocinando("")
            // Escopo de app: não cancela se a composição sair no meio da chamada
            ChatRepository.launch {
                try {
                    val resultado = if (lunaDirect) {
                        LunaDirectChat.responder(
                            context = appContext,
                            historico = historicoAntes,
                            textoUsuario = textoEnvio,
                            anexos = anexos,
                            reference = reference,
                            onEstado = { streamState.value = it },
                        )
                    } else {
                        LunaApiChat.responder(
                            context = appContext,
                            conversaId = conversaId,
                            historico = historicoAntes,
                            textoUsuario = textoEnvio,
                            anexos = anexos,
                            reference = reference,
                            userMessageId = userMsgId!!,
                            lunaMessageId = lunaMsgId!!,
                            onEstado = { streamState.value = it },
                        )
                    }
                    ChatRepository.enviarMensagem(
                        conversaId = conversaId,
                        texto = resultado.resposta,
                        isLuna = true,
                        reasoning = resultado.reasoning.takeIf { it.isNotBlank() },
                        reasoningDuracao = resultado.reasoningDuracao.takeIf { it.isNotBlank() },
                        actionRun = resultado.actionRun,
                        messageId = lunaMsgId,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val origem = if (lunaDirect) "OpenRouter" else "servidor Luna"
                    ChatRepository.enviarMensagem(
                        conversaId = conversaId,
                        texto = "Erro ao falar com o $origem: ${e.message ?: "desconhecido"}",
                        isLuna = true,
                        messageId = lunaMsgId,
                    )
                } finally {
                    streamState.value = LunaStreamEstado.Idle
                    streamLunaMsgId = null
                }
            }
            Unit
        }
    }

    // Só volta se a conversa sumir de verdade (evita pop no meio do sync Firestore)
    LaunchedEffect(conversaId) {
        if (ChatRepository.getConversa(conversaId) != null) return@LaunchedEffect
        delay(900)
        if (ChatRepository.getConversa(conversaId) == null) onBack()
    }

    val conversaAtual = conversa
    if (conversaAtual == null) {
        Box(Modifier.fillMaxSize().background(OrbitTokens.ink1))
    } else {
        val historico = conversaAtual.mensagens

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OrbitTokens.ink1)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            ChatHeader(onBack = onBack, titulo = conversaAtual.titulo)

            Box(modifier = Modifier.weight(1f)) {
                ChatTimeline(
                    historico = historico,
                    streamState = streamState,
                    ocultarMessageId = streamLunaMsgId,
                    onMessageLongPress = { msg ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        actionSheetMsg = msg
                    },
                    onReferenciarMedia = { msg, att ->
                        val ref = buildImageReference(msg, historico, att)
                        if (ref != null) {
                            messageReference = ref
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                )
            }

            ChatInputArea(
                streamState = streamState,
                messageReference = messageReference,
                onClearReference = { messageReference = null },
                onSend = { texto, anexos, ref ->
                    onSend(texto, anexos, ref)
                    messageReference = null
                },
            )
        }

        val sheetMsg = actionSheetMsg
        if (sheetMsg != null) {
            MessageActionSheet(
                mensagem = sheetMsg,
                onDismiss = { actionSheetMsg = null },
                onReferenciarMensagem = {
                    val ref = buildMessageReference(sheetMsg, historico)
                    if (ref != null) {
                        messageReference = ref
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                onReferenciarImagem = { att ->
                    val ref = buildImageReference(sheetMsg, historico, att)
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
            )
        }
    }
}

@Composable
private fun ChatHeader(onBack: () -> Unit, titulo: String) {
    val tituloExibido =
        if (titulo.equals("Nova conversa", ignoreCase = true)) "Luna" else titulo

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(OrbitMetrics.iconBtn)
                    .clip(CircleShape)
                    .background(OrbitTokens.surface)
                    .border(1.dp, OrbitTokens.borderSoft, CircleShape)
                    .orbitPressable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Voltar",
                    tint = OrbitTokens.textHigh,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.size(42.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.luna_avatar),
                    contentDescription = "Avatar Luna",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFF2F4F8).copy(alpha = 0.35f), CircleShape),
                )
                AssinaturaAzul(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 1.dp, y = 1.dp)
                        .size(12.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tituloExibido,
                    color = OrbitTokens.textHigh,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.2).sp,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(OrbitTokens.online),
                    )
                    Text(
                        "Online · memória privada",
                        color = OrbitTokens.textMid,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OrbitTokens.borderSoft.copy(alpha = 0.65f)),
        )
    }
}

/** Canto Aura Blue — mesma assinatura do FAB / cards ativos. */
@Composable
private fun AssinaturaAzul(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width, size.height)
            lineTo(size.width, size.height * 0.15f)
            lineTo(size.width * 0.15f, size.height)
            close()
        }
        drawPath(path, color = OrbitTokens.accent)
    }
}

@Composable
private fun ChatTimeline(
    historico: List<Mensagem>,
    streamState: MutableState<LunaStreamEstado>,
    ocultarMessageId: String? = null,
    onMessageLongPress: (Mensagem) -> Unit = {},
    onReferenciarMedia: (Mensagem, ComposerAttachment) -> Unit = { _, _ -> },
) {
    val streamEstado = streamState.value
    val listState = rememberLazyListState()
    val streamAtivo = streamEstado !is LunaStreamEstado.Idle
    val density = LocalDensity.current
    val limiarFundoPx = with(density) { 120.dp.toPx() }
    val mensagensVisiveis = remember(historico, ocultarMessageId, streamAtivo) {
        if (streamAtivo && ocultarMessageId != null) {
            historico.filter { it.id != ocultarMessageId }
        } else {
            historico
        }
    }

    // Nova mensagem (envio ou resposta final) → sempre desce, mesmo no meio do histórico.
    LaunchedEffect(mensagensVisiveis.size, streamAtivo) {
        if (mensagensVisiveis.isEmpty() && !streamAtivo) return@LaunchedEffect
        listState.irAoFundo(animado = true)
    }

    // Stream: ao começar, sobe ao fundo; depois só acompanha se o user continuar no fim.
    LaunchedEffect(listState, streamAtivo) {
        if (!streamAtivo) return@LaunchedEffect
        listState.irAoFundo(animado = false)
        var ultimaAltura = -1
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            Triple(last?.index ?: -1, last?.size ?: 0, info.totalItemsCount)
        }
            .distinctUntilChanged()
            .collect { (index, altura, total) ->
                if (total <= 0 || index != total - 1) return@collect
                if (!listState.pertoDoFundo(limiarFundoPx)) {
                    ultimaAltura = altura
                    return@collect
                }
                if (ultimaAltura >= 0 && altura > ultimaAltura) {
                    listState.scrollBy((altura - ultimaAltura).toFloat())
                } else if (ultimaAltura < 0) {
                    listState.irAoFundo(animado = false)
                }
                ultimaAltura = altura
                delay(40)
            }
    }

    if (mensagensVisiveis.isEmpty() && !streamAtivo) {
        ChatEmptyState()
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OrbitMetrics.pagePadding,
            end = OrbitMetrics.pagePadding,
            top = 16.dp,
            bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = mensagensVisiveis,
            key = { it.id },
            contentType = { if (it.isLuna) "luna" else "user" },
        ) { msg ->
            MessageBubble(
                msg = msg,
                onLongPress = { onMessageLongPress(msg) },
                onReferenciarMedia = { att -> onReferenciarMedia(msg, att) },
            )
        }
        if (streamAtivo) {
            item(key = "luna-stream", contentType = "stream") {
                LunaStreamDraft(estado = streamEstado)
            }
        }
    }
}

/** Último item visível e perto do rodapé da viewport. */
private fun LazyListState.pertoDoFundo(limiarPx: Float): Boolean {
    val info = layoutInfo
    val total = info.totalItemsCount
    if (total <= 0) return true
    val last = info.visibleItemsInfo.lastOrNull() ?: return true
    if (last.index < total - 1) return false
    val fimDoItem = last.offset + last.size + info.afterContentPadding
    return fimDoItem - info.viewportEndOffset <= limiarPx
}

/** Vai ao último item e corrige overflow residual. */
private suspend fun LazyListState.irAoFundo(animado: Boolean) {
    val total = layoutInfo.totalItemsCount
    if (total <= 0) return
    val last = total - 1
    if (animado) animateScrollToItem(last) else scrollToItem(last)
    delay(16)
    val info = layoutInfo
    val item = info.visibleItemsInfo.lastOrNull() ?: return
    val overflow = item.offset + item.size + info.afterContentPadding - info.viewportEndOffset
    if (overflow > 0) {
        if (animado) animateScrollBy(overflow.toFloat()) else scrollBy(overflow.toFloat())
    }
}

@Composable
private fun ChatEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OrbitMetrics.pagePadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.82f),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrbitTokens.surface)
                    .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint = OrbitTokens.accentText,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Começa a conversa",
                color = OrbitTokens.textHigh,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "A Luna responde com clareza — texto, voz ou as duas.",
                color = OrbitTokens.textMid,
                fontSize = OrbitMetrics.bodySize,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    msg: Mensagem,
    onLongPress: () -> Unit = {},
    onReferenciarMedia: (ComposerAttachment) -> Unit = {},
) {
    val shape = if (msg.isLuna) {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = OrbitMetrics.radiusCard,
            bottomEnd = OrbitMetrics.radiusCard,
            bottomStart = OrbitMetrics.radiusCard,
        )
    } else {
        RoundedCornerShape(
            topStart = OrbitMetrics.radiusCard,
            topEnd = 4.dp,
            bottomEnd = OrbitMetrics.radiusCard,
            bottomStart = OrbitMetrics.radiusCard,
        )
    }
    val bubbleInteraction = remember { MutableInteractionSource() }
    val mostrarRaciocinio by PrefsRepository.reasoningEnabled.collectAsState()

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = if (msg.isLuna) Alignment.Start else Alignment.End,
            modifier = Modifier
                .align(if (msg.isLuna) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxWidth(if (msg.isLuna && msg.actionRun != null) 0.96f else 0.82f),
        ) {
            if (msg.isLuna && msg.actionRun != null) {
                LunaActionTimeline(
                    run = msg.actionRun,
                    inicialmenteAberto = false,
                )
                Spacer(Modifier.height(8.dp))
            }

            if (mostrarRaciocinio && msg.isLuna && !msg.reasoning.isNullOrBlank()) {
                LunaReasoning(
                    texto = msg.reasoning,
                    duracaoLabel = msg.reasoningDuracao,
                    inicialmenteAberto = false,
                )
                Spacer(Modifier.height(6.dp))
            }

            val temAnexos = !msg.isLuna && msg.attachments.isNotEmpty()
            val temTexto = msg.texto.isNotBlank()
            val soAnexos = temAnexos && !temTexto
            val temReferencia = !msg.isLuna && msg.reference != null

            if (temAnexos) {
                MessageAttachments(
                    attachments = msg.attachments,
                    solo = soAnexos,
                    modifier = Modifier.align(Alignment.End),
                    onReferenciarMedia = onReferenciarMedia,
                )
                if (temTexto || temReferencia) Spacer(Modifier.height(6.dp))
            }

            if (temTexto || temReferencia) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (msg.actionRun != null) 0.88f else 1f)
                        .align(if (msg.isLuna) Alignment.Start else Alignment.End)
                        .clip(shape)
                        .then(
                            if (msg.isLuna) {
                                Modifier
                                    .background(OrbitTokens.bubbleLuna)
                                    .border(1.dp, OrbitTokens.borderSoft, shape)
                            } else {
                                Modifier.background(OrbitTokens.accent)
                            },
                        )
                        .combinedClickable(
                            interactionSource = bubbleInteraction,
                            indication = null,
                            onClick = {},
                            onLongClick = onLongPress,
                        ),
                ) {
                    if (msg.isLuna) {
                        AssinaturaAzul(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(12.dp),
                        )
                    }
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (temReferencia && msg.reference != null) {
                            MessageReferenceQuote(reference = msg.reference)
                            if (temTexto) Spacer(Modifier.height(8.dp))
                        }
                        if (temTexto) {
                            if (msg.isLuna) {
                                LunaMarkdown(content = msg.texto)
                            } else {
                                Text(
                                    text = msg.texto,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatHoraMensagem(msg.timestamp),
                color = OrbitTokens.textLow.copy(alpha = 0.85f),
                fontSize = 11.sp,
                modifier = Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = onLongPress,
                ),
            )
        }
    }
}

@Composable
private fun ChatInputArea(
    onSend: (String, List<ComposerAttachment>, ThreadReference?) -> Unit,
    streamState: MutableState<LunaStreamEstado>,
    messageReference: ThreadReference? = null,
    onClearReference: () -> Unit = {},
) {
    val enabled = streamState.value is LunaStreamEstado.Idle
    var texto by remember { mutableStateOf("") }
    var anexos by remember { mutableStateOf<List<ComposerAttachment>>(emptyList()) }
    var attachAberto by remember { mutableStateOf(false) }
    var recordState by remember { mutableStateOf(RecordState.Idle) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var segundos by remember { mutableIntStateOf(0) }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val cancelThresholdPx = with(density) { 120.dp.toPx() }
    val lockThresholdPx = with(density) { 90.dp.toPx() }

    val imageBudget = (MAX_ATTACH_IMAGES - anexos.count { it.kind == AttachmentKind.IMAGE })
        .coerceAtLeast(0)
    val fileBudget = (
        MAX_ATTACH_FILES -
            anexos.count { it.kind == AttachmentKind.FILE || it.kind == AttachmentKind.VIDEO }
        ).coerceAtLeast(0)

    LaunchedEffect(recordState) {
        if (recordState == RecordState.Recording || recordState == RecordState.Locked) {
            while (true) {
                delay(1000)
                segundos += 1
            }
        }
    }

    val cancelProgresso = if (recordState == RecordState.Recording) {
        (-dragOffset.x / cancelThresholdPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val lockProgresso = if (recordState == RecordState.Recording) {
        (-dragOffset.y / lockThresholdPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    fun resetar() {
        recordState = RecordState.Idle
        dragOffset = Offset.Zero
        segundos = 0
    }

    fun enviarAudio() {
        onSend("Áudio (${formatTimer(segundos)})", emptyList(), messageReference)
        resetar()
    }

    fun enviarTexto() {
        if (texto.isBlank() && anexos.isEmpty() && messageReference == null) return
        onSend(texto.trim(), anexos, messageReference)
        texto = ""
        anexos = emptyList()
    }

    ComposerAttachSheet(
        visible = attachAberto,
        imageBudget = imageBudget,
        fileBudget = fileBudget,
        onDismiss = { attachAberto = false },
        onConfirm = { novos ->
            val imagens = anexos.filter { it.kind == AttachmentKind.IMAGE }.toMutableList()
            val arquivos = anexos
                .filter { it.kind == AttachmentKind.FILE || it.kind == AttachmentKind.VIDEO }
                .toMutableList()
            novos.forEach { item ->
                when (item.kind) {
                    AttachmentKind.IMAGE ->
                        if (imagens.size < MAX_ATTACH_IMAGES) imagens += item
                    AttachmentKind.FILE, AttachmentKind.VIDEO ->
                        if (arquivos.size < MAX_ATTACH_FILES) arquivos += item
                }
            }
            anexos = imagens + arquivos
            attachAberto = false
        },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitTokens.ink1)
            .padding(horizontal = OrbitMetrics.pagePadding, vertical = 12.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f },
    ) {
        if (messageReference != null) {
            ComposerReferenceChip(
                reference = messageReference,
                onClear = onClearReference,
            )
        }

        ComposerAttachmentStrip(
            attachments = anexos,
            onRemove = { id -> anexos = anexos.filterNot { it.id == id } },
        )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val gravando =
            recordState == RecordState.Recording || recordState == RecordState.Locked
        val multiLinha = texto.contains('\n') || texto.length > 48
        val isTyping =
            texto.isNotBlank() || anexos.isNotEmpty() || messageReference != null

        // Slot fixo do Plus — se remover na gravação, o Compose remonta o mic e cancela o hold
        if (!gravando) {
            val plusEnabled = enabled && imageBudget + fileBudget > 0
            val (plusInteraction, plusPressScale) = rememberOrbitPressScale()
            Box(
                Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = plusPressScale
                        scaleY = plusPressScale
                    }
                    .clip(CircleShape)
                    .background(OrbitTokens.surface)
                    .border(1.dp, OrbitTokens.borderSoft, CircleShape)
                    .clickable(
                        interactionSource = plusInteraction,
                        indication = null,
                        enabled = plusEnabled,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            attachAberto = true
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Anexos",
                    tint = if (anexos.isNotEmpty()) OrbitTokens.accentText else OrbitTokens.textHigh,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            Spacer(Modifier.size(44.dp))
        }

        // Campo — animações de gravação só aqui (não remonta o mic)
        RecordingAnimHost(ativo = gravando) { dotAlpha ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 140.dp)
                    .clip(ComposerFieldShape)
                    .background(OrbitTokens.surface)
                    .border(1.dp, OrbitTokens.borderSoft, ComposerFieldShape)
                    .padding(
                        horizontal = 14.dp,
                        vertical = if (multiLinha && !gravando) 10.dp else 11.dp,
                    ),
                contentAlignment = when {
                    gravando -> Alignment.CenterStart
                    multiLinha -> Alignment.TopStart
                    else -> Alignment.CenterStart
                },
            ) {
                when (recordState) {
                    RecordState.Idle -> {
                        BasicTextField(
                            value = texto,
                            onValueChange = { texto = it },
                            textStyle = TextStyle(
                                color = OrbitTokens.textHigh,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(OrbitTokens.accent),
                            maxLines = ComposerMaxLines,
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxWidth()) {
                                    if (texto.isEmpty()) {
                                        Text(
                                            when {
                                                messageReference != null -> "Pergunta sobre a referência…"
                                                anexos.isNotEmpty() -> "Adiciona uma legenda…"
                                                else -> "Mensagem pra Luna..."
                                            },
                                            color = OrbitTokens.textLow,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                    RecordState.Recording -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(OrbitTokens.danger.copy(alpha = dotAlpha)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(formatTimer(segundos), color = OrbitTokens.textHigh, fontSize = 15.sp)
                            Spacer(Modifier.weight(1f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .offset {
                                        IntOffset(dragOffset.x.coerceIn(-48f, 0f).roundToInt(), 0)
                                    }
                                    .alpha(1f - cancelProgresso),
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowBackIosNew,
                                    contentDescription = null,
                                    tint = OrbitTokens.textLow,
                                    modifier = Modifier.size(11.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "arraste pra cancelar",
                                    color = OrbitTokens.textLow,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    RecordState.Locked -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.orbitPressable { resetar() },
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = "Descartar",
                                    tint = OrbitTokens.textMid,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    "Descartar",
                                    color = OrbitTokens.textMid,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(OrbitTokens.danger.copy(alpha = dotAlpha)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(formatTimer(segundos), color = OrbitTokens.textHigh, fontSize = 15.sp)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = OrbitTokens.accentText,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        // Mic/Send fora do AnimHost — o hold precisa sobreviver ao Idle→Recording
        val escalaBase by animateFloatAsState(
            if (recordState == RecordState.Recording) 1.18f else 1f,
            animationSpec = OrbitMotion.springPress,
            label = "escala",
        )
        val (sendInteraction, sendPressScale) = rememberOrbitPressScale()

        val mostrarSend = isTyping || recordState == RecordState.Locked
        val corBotao = when {
            recordState != RecordState.Idle -> OrbitTokens.danger
            mostrarSend -> SendIdleBg
            else -> OrbitTokens.accent
        }
        val tintIcone = when {
            recordState != RecordState.Idle -> Color.White
            mostrarSend -> OrbitTokens.ink0
            else -> Color.White
        }

        // pointerInput estável: NÃO trocar o modifier ao entrar em Recording
        val micHoldAtivo = enabled && !isTyping
        val holdModifier = when {
            !enabled -> Modifier
            mostrarSend || recordState == RecordState.Locked -> {
                Modifier.clickable(interactionSource = sendInteraction, indication = null) {
                    if (recordState == RecordState.Locked) {
                        enviarAudio()
                    } else {
                        enviarTexto()
                    }
                }
            }
            else -> {
                Modifier.pointerInput(micHoldAtivo) {
                    if (!micHoldAtivo) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        recordState = RecordState.Recording
                        dragOffset = Offset.Zero
                        segundos = 0
                        var finalizado = false
                        while (!finalizado) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                dragOffset += change.positionChange()
                                when {
                                    -dragOffset.x >= cancelThresholdPx -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        resetar()
                                        finalizado = true
                                    }
                                    -dragOffset.y >= lockThresholdPx -> {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        recordState = RecordState.Locked
                                        dragOffset = Offset.Zero
                                        finalizado = true
                                    }
                                }
                            } else {
                                if (segundos >= 1) enviarAudio() else resetar()
                                finalizado = true
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (recordState == RecordState.Recording) {
                Box(
                    modifier = Modifier
                        .offset(y = (-74 - lockProgresso * 10).dp)
                        .size(width = 40.dp, height = 58.dp)
                        .clip(RoundedCornerShape(OrbitMetrics.radiusCard))
                        .background(OrbitTokens.surfaceRaised.copy(alpha = 0.9f))
                        .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(OrbitMetrics.radiusCard)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = lerp(OrbitTokens.textLow, OrbitTokens.accentText, lockProgresso),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                            tint = OrbitTokens.textLow.copy(alpha = 1f - lockProgresso),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            val offsetBotao = if (recordState == RecordState.Recording) {
                IntOffset(
                    dragOffset.x.coerceIn(-cancelThresholdPx, 0f).roundToInt(),
                    dragOffset.y.coerceIn(-lockThresholdPx, 0f).roundToInt(),
                )
            } else {
                IntOffset.Zero
            }

            val escalaBotao = escalaBase * if (mostrarSend || recordState == RecordState.Locked) {
                sendPressScale
            } else {
                1f
            }
            Box(
                modifier = Modifier
                    .offset { offsetBotao }
                    .graphicsLayer {
                        scaleX = escalaBotao
                        scaleY = escalaBotao
                    }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(corBotao)
                    .then(holdModifier),
                contentAlignment = Alignment.Center,
            ) {
                if (mostrarSend) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Enviar",
                        tint = tintIcone,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = 1.dp),
                    )
                } else {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = "Segurar para gravar",
                        tint = tintIcone,
                        modifier = Modifier.size(24.dp),
                    )
                }
                if (recordState == RecordState.Idle && !mostrarSend) {
                    AssinaturaAzul(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(11.dp),
                    )
                }
            }
        }
    } // Row
    } // Column
}


/**
 * Anima o ponto de gravação só com [ativo] — o botão mic fica fora deste host
 * para o hold (pointerInput) não ser cancelado ao entrar em Recording.
 */
@Composable
private fun RecordingAnimHost(
    ativo: Boolean,
    content: @Composable (dotAlpha: Float) -> Unit,
) {
    if (!ativo) {
        content(1f)
        return
    }
    val infinite = rememberInfiniteTransition(label = "rec")
    val dotAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dot",
    )
    content(dotAlpha)
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 800)
@Composable
fun ChatScreenPreview() {
    ChatScreen(conversaId = "mock", onBack = {})
}
