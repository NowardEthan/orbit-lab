package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
    var exportAberto by remember { mutableStateOf(false) }
    var buscaAberta by remember { mutableStateOf(false) }
    // Id da mensagem pra rolar até / destacar após tocar num resultado da busca.
    var scrollAlvo by remember { mutableStateOf<String?>(null) }
    var destaqueId by remember { mutableStateOf<String?>(null) }
    // Onde ele estava lendo quando saiu desta conversa (null = estava no fim, o padrão).
    val ancoraInicial = remember(conversaId) { PrefsRepository.ancoraDe(conversaId) }

    // O destaque do resultado é um pulso: acende ao chegar e apaga sozinho.
    LaunchedEffect(destaqueId) {
        if (destaqueId != null) {
            delay(1600)
            destaqueId = null
        }
    }
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

    // Gera (e persiste) a resposta da Luna para um dado texto + histórico. NÃO envia a
    // mensagem do usuário — quem envia é o onSend. O onRetry chama isto direto, então
    // retomar uma mensagem órfã não duplica a fala do usuário no histórico.
    val dispararResposta = remember(conversaId, streamState, appContext, lunaDirect) {
        { textoEnvio: String,
          historicoAntes: List<Mensagem>,
          anexos: List<ComposerAttachment>,
          reference: ThreadReference?,
          userMsgId: String?,
          lunaMsgId: String? ->
            streamLunaMsgId = lunaMsgId
            streamState.value = LunaStreamEstado.Raciocinando("")
            // Escopo de app: não cancela se a composição sair no meio da chamada.
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
                            userMessageId = userMsgId ?: newUserMessageId(),
                            lunaMessageId = lunaMsgId ?: newUserMessageId(),
                            onEstado = { streamState.value = it },
                        )
                    }
                    ChatRepository.enviarMensagem(
                        conversaId = conversaId,
                        texto = resultado.resposta,
                        isLuna = true,
                        reasoning = resultado.reasoning.takeIf { it.isNotBlank() && !resultado.erro },
                        reasoningDuracao = resultado.reasoningDuracao.takeIf { it.isNotBlank() && !resultado.erro },
                        actionRun = if (resultado.erro) null else resultado.actionRun,
                        // Reusa o id (substitui um balão de erro no lugar) ou gera novo (anexa).
                        messageId = lunaMsgId,
                        // Erro = aviso local: não vai pra nuvem nem vira memória; some no retry.
                        persistirNuvem = !resultado.erro,
                        erro = resultado.erro,
                    )
                    // Deu certo → a Luna rebatiza a conversa pelo assunto atual (no 1º par
                    // e a cada 6 turnos). Barato e à parte; não trava a resposta.
                    if (!resultado.erro) {
                        ChatRepository.talvezRenomearPelaLuna(conversaId)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val origem = if (lunaDirect) "OpenRouter" else "servidor Luna"
                    ChatRepository.enviarMensagem(
                        conversaId = conversaId,
                        texto = "Erro ao falar com o $origem: ${detalheFalha(e)}",
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
            Unit
        }
    }

    val onSend = remember(dispararResposta, conversaId, lunaDirect) {
        { texto: String, anexos: List<ComposerAttachment>, reference: ThreadReference? ->
            val textoEnvio = texto.trim()
            val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
            // Par de ids ordenado (…-0 tua, …-1 Luna) em AMBOS os caminhos. No Railway
            // evita duplicar; no direto garante que, enquanto o carimbo do servidor não
            // assenta, o Firestore ordene pelo id e a Luna NUNCA caia acima da tua. 🌙
            val userMsgId = newUserMessageId()
            val lunaMsgId = lunaMessageIdForUser(userMsgId)
            ChatRepository.enviarMensagem(
                conversaId = conversaId,
                texto = textoEnvio,
                isLuna = false,
                attachments = anexos,
                reference = reference,
                messageId = userMsgId,
            )
            dispararResposta(textoEnvio, historicoAntes, anexos, reference, userMsgId, lunaMsgId)
            Unit
        }
    }

    // Retomar sem duplicar: se a última é uma mensagem SUA sem resposta (saiu do app e a
    // resposta se perdeu), ou um erro da Luna, refaz a resposta pra AQUELA mensagem — sem
    // reenviar a fala do usuário. A Luna vê a mensagem uma vez só, como você pediu. 🌙
    val onRetry = remember(dispararResposta, conversaId, lunaDirect) {
        {
            val msgs = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
            val ultima = msgs.lastOrNull()
            if (ultima != null && streamState.value is LunaStreamEstado.Idle) {
                // (userMsg a refazer, histórico antes dela, id da resposta a substituir/anexar)
                val alvo: Triple<Mensagem, List<Mensagem>, String?>? = when {
                    !ultima.isLuna -> Triple(ultima, msgs.dropLast(1), null)
                    ultima.erro || respostaEhErro(ultima.texto) -> {
                        val userAntes = msgs.dropLast(1).lastOrNull { !it.isLuna }
                        userAntes?.let { u ->
                            val idxUser = msgs.indexOfFirst { it.id == u.id }
                            Triple(u, msgs.subList(0, idxUser).toList(), ultima.id)
                        }
                    }
                    else -> null
                }
                alvo?.let { (userMsg, historicoAntes, replyId) ->
                    // Reusa o id da SUA mensagem — o servidor faz upsert e NÃO duplica a tua fala.
                    // (Gerar id novo aqui era o bug: o Railway gravava um segundo turno seu.)
                    val userMsgId = if (lunaDirect) null else userMsg.id
                    val lunaMsgId = replyId ?: if (lunaDirect) null else lunaMessageIdForUser(userMsg.id)
                    dispararResposta(userMsg.texto, historicoAntes, emptyList(), userMsg.reference, userMsgId, lunaMsgId)
                }
            }
            Unit
        }
    }

    // Turno interrompido (o Android matou o app enquanto ele estava fora, a rede caiu no
    // meio) deixa a mensagem DELE sozinha, sem resposta. Em vez de exigir que ele fique
    // tocando em «tentar de novo», a tela retoma sozinha — uma vez por mensagem órfã, e só
    // se for recente (abrir uma conversa velha não acorda a Luna do nada).
    var autoRetomadoId by remember(conversaId) { mutableStateOf<String?>(null) }
    val ultimaMsg = conversa?.mensagens?.lastOrNull()
    val ocioso = streamState.value is LunaStreamEstado.Idle
    LaunchedEffect(ultimaMsg?.id, ocioso) {
        val orfa = ultimaMsg ?: return@LaunchedEffect
        if (orfa.isLuna || !ocioso || autoRetomadoId == orfa.id) return@LaunchedEffect
        if (System.currentTimeMillis() - orfa.timestamp > 30 * 60_000L) return@LaunchedEffect
        // Respira: a resposta pode estar descendo do Firestore neste instante.
        delay(1500)
        val agora = ChatRepository.getConversa(conversaId)?.mensagens?.lastOrNull()
        if (agora == null || agora.id != orfa.id || agora.isLuna) return@LaunchedEffect
        if (streamState.value !is LunaStreamEstado.Idle) return@LaunchedEffect
        autoRetomadoId = orfa.id
        onRetry()
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

        Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OrbitTokens.ink1)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            ChatHeader(
                onBack = onBack,
                titulo = conversaAtual.titulo,
                onBuscar = { buscaAberta = true },
                onExport = { exportAberto = true },
            )

            Box(modifier = Modifier.weight(1f)) {
                ChatTimeline(
                    conversaId = conversaId,
                    historico = historico,
                    streamState = streamState,
                    ocultarMessageId = streamLunaMsgId,
                    scrollParaId = scrollAlvo,
                    onScrollFeito = { scrollAlvo = null },
                    destacarId = destaqueId,
                    selecionadoId = actionSheetMsg?.id,
                    ancoraInicial = ancoraInicial,
                    onAncora = { id -> PrefsRepository.setAncora(conversaId, id) },
                    onRetry = onRetry,
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

        BuscaConversaOverlay(
            visivel = buscaAberta,
            mensagens = historico,
            onIrPara = { id ->
                buscaAberta = false
                scrollAlvo = id
                destaqueId = id
            },
            onFechar = { buscaAberta = false },
        )
        }

        if (exportAberto) {
            ExportSheet(
                titulo = conversaAtual.titulo,
                mensagens = historico,
                onDismiss = { exportAberto = false },
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
                onReenviar = {
                    if (streamState.value is LunaStreamEstado.Idle) {
                        val msgs = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
                        // A âncora é a mensagem do USUÁRIO a refazer: a própria (se for sua)
                        // ou a última tua antes dela (se você tocou numa da Luna).
                        val ancora: Mensagem? = if (!sheetMsg.isLuna) {
                            sheetMsg
                        } else {
                            val idx = msgs.indexOfFirst { it.id == sheetMsg.id }
                            if (idx >= 0) msgs.take(idx).lastOrNull { !it.isLuna } else null
                        }
                        if (ancora != null) {
                            val historicoAntes = msgs.take(msgs.indexOfFirst { it.id == ancora.id })
                            ChatRepository.truncarApos(conversaId, ancora.id)
                            // Servidor: REUSA o id da âncora (upsert → não duplica a tua fala) e usa
                            // um lunaMsgId NOVO pra forçar regeneração (não pegar o cache do turno).
                            val userMsgId = if (lunaDirect) null else ancora.id
                            val lunaMsgId = if (lunaDirect) null else newUserMessageId()
                            dispararResposta(ancora.texto, historicoAntes, emptyList(), ancora.reference, userMsgId, lunaMsgId)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ChatHeader(
    onBack: () -> Unit,
    titulo: String,
    onBuscar: () -> Unit = {},
    onExport: () -> Unit = {},
) {
    val tituloExibido =
        if (titulo.equals("Nova conversa", ignoreCase = true)) "Luna" else titulo
    var menuAberto by remember { mutableStateOf(false) }
    val density = LocalDensity.current

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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Buscar nesta conversa (busca semântica — «pergunta à Luna»).
            Box(
                modifier = Modifier
                    .size(OrbitMetrics.iconBtn)
                    .clip(CircleShape)
                    .background(OrbitTokens.surface)
                    .border(1.dp, OrbitTokens.borderSoft, CircleShape)
                    .orbitPressable(onClick = onBuscar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "Buscar na conversa",
                    tint = OrbitTokens.textHigh,
                    modifier = Modifier.size(19.dp),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Mais ações (exportar…) num menuzinho — tira o peso do topo.
            Box {
                Box(
                    modifier = Modifier
                        .size(OrbitMetrics.iconBtn)
                        .clip(CircleShape)
                        .background(OrbitTokens.surface)
                        .border(1.dp, OrbitTokens.borderSoft, CircleShape)
                        .orbitPressable(onClick = { menuAberto = true }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Mais",
                        tint = OrbitTokens.textHigh,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (menuAberto) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        offset = IntOffset(
                            0,
                            with(density) { (OrbitMetrics.iconBtn + 6.dp).roundToPx() },
                        ),
                        onDismissRequest = { menuAberto = false },
                        properties = PopupProperties(focusable = true),
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 208.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(OrbitTokens.surfaceRaised)
                                .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(14.dp))
                                .padding(6.dp),
                        ) {
                            ChatMenuItem(
                                icone = Icons.Rounded.IosShare,
                                rotulo = "Exportar conversa",
                                onClick = {
                                    menuAberto = false
                                    onExport()
                                },
                            )
                        }
                    }
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

/** Linha de ação do menuzinho de três-pontinhos do topo. */
@Composable
private fun ChatMenuItem(
    icone: ImageVector,
    rotulo: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = OrbitTokens.textHigh,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            rotulo,
            color = OrbitTokens.textHigh,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
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
    conversaId: String,
    historico: List<Mensagem>,
    streamState: MutableState<LunaStreamEstado>,
    ocultarMessageId: String? = null,
    scrollParaId: String? = null,
    onScrollFeito: () -> Unit = {},
    destacarId: String? = null,
    selecionadoId: String? = null,
    ancoraInicial: String? = null,
    onAncora: (String?) -> Unit = {},
    onRetry: () -> Unit = {},
    onMessageLongPress: (Mensagem) -> Unit = {},
    onReferenciarMedia: (Mensagem, ComposerAttachment) -> Unit = { _, _ -> },
) {
    val streamEstado = streamState.value
    // Uma posição de rolagem por conversa: trocar de conversa não herda o lugar da anterior.
    val listState = rememberSaveable(conversaId, saver = LazyListState.Saver) { LazyListState() }
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

    // Posicionamento da lista — abertura e mensagem nova no MESMO efeito, de propósito:
    // em dois efeitos separados eles corriam juntos e o «desce até o fim» atropelava a
    // volta pra onde ele estava lendo.
    var posicionado by remember(conversaId) { mutableStateOf(false) }
    LaunchedEffect(mensagensVisiveis.size, streamAtivo) {
        // Lista vazia = histórico ainda chegando do Firestore; espera a próxima leva.
        if (mensagensVisiveis.isEmpty() && !streamAtivo) return@LaunchedEffect
        if (!posicionado) {
            val alvo = ancoraInicial
            val idx = if (alvo == null) -1 else mensagensVisiveis.indexOfFirst { it.id == alvo }
            // Sem âncora (ou a mensagem já não existe) o lugar certo é o fim, como sempre.
            if (idx >= 0) runCatching { listState.scrollToItem(idx) } else listState.irAoFundo(animado = false)
            posicionado = true
            return@LaunchedEffect
        }
        // Daqui pra frente: mensagem nova (envio ou resposta) sempre desce.
        listState.irAoFundo(animado = true)
    }

    // Guarda a âncora conforme ele lê: a mensagem no topo da tela. Se estiver perto do fim,
    // guarda nada — o fim é onde o app já abre, e é lá que a mensagem nova aparece.
    val listaAgora by rememberUpdatedState(mensagensVisiveis)
    val selecionadoAgora by rememberUpdatedState(selecionadoId)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx ->
                // Antes de assentar a lista o topo é o item 0 por um instante; guardar isso
                // ancorava a conversa no começo do histórico pra sempre.
                if (!posicionado) return@collect
                // Com mensagem selecionada a lista está deslocada pelo vão da folha de ações:
                // o que ele vê agora não é onde ele estava lendo.
                if (selecionadoAgora != null) return@collect
                val perto = listState.pertoDoFundo(limiarFundoPx)
                onAncora(if (perto) null else listaAgora.getOrNull(idx)?.id)
            }
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

    // Selecionou uma mensagem: a folha de ações cobre a metade de baixo da tela, então se a
    // bolha escolhida ficaria escondida atrás dela, sobe ela pro alto — de nada serve acender
    // uma mensagem que ele não vê.
    LaunchedEffect(selecionadoId) {
        val alvo = selecionadoId ?: return@LaunchedEffect
        val idx = listaAgora.indexOfFirst { it.id == alvo }
        if (idx < 0) return@LaunchedEffect
        // Um quadro pro vão de baixo entrar em vigor antes de medirmos.
        delay(24)
        val info = listState.layoutInfo
        val altura = info.viewportEndOffset
        if (altura <= 0) return@LaunchedEffect
        val item = info.visibleItemsInfo.firstOrNull { it.index == idx }
        val cabe = item != null && item.offset >= 0 && item.offset + item.size <= altura * 0.45f
        if (cabe) return@LaunchedEffect
        runCatching {
            listState.animateScrollToItem(idx, scrollOffset = -(altura * 0.10f).toInt())
        }
    }

    // Tocou num resultado da busca → rola até a mensagem (centralizada dá pra ver o contexto).
    LaunchedEffect(scrollParaId) {
        val alvo = scrollParaId ?: return@LaunchedEffect
        val idx = mensagensVisiveis.indexOfFirst { it.id == alvo }
        if (idx >= 0) {
            val visivel = listState.layoutInfo.viewportEndOffset
            runCatching { listState.animateScrollToItem(idx, scrollOffset = -visivel / 3) }
        }
        onScrollFeito()
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
            // Com uma mensagem selecionada abrimos um vão embaixo: sem esse espaço a lista
            // não teria pra onde rolar e a última bolha ficaria presa atrás da folha de ações.
            bottom = 28.dp + if (selecionadoId != null) 300.dp else 0.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            items = mensagensVisiveis,
            key = { _, msg -> msg.id },
            contentType = { _, msg -> if (msg.isLuna) "luna" else "user" },
        ) { index, msg ->
            // Pulso de destaque: quando a busca aponta esta mensagem, um brilho suave acende
            // atrás dela e apaga sozinho — pra você ver onde aterrissou.
            val destacado = msg.id == destacarId
            // Selecionada (segurou pra abrir as ações): ela fica acesa e um pouco maior,
            // e as vizinhas recuam pro fundo — pra não ter dúvida de em qual você tocou.
            val selecionada = selecionadoId != null && msg.id == selecionadoId
            val recuada = selecionadoId != null && !selecionada
            val corPulso by animateColorAsState(
                targetValue = when {
                    selecionada -> OrbitTokens.accent.copy(alpha = 0.22f)
                    destacado -> OrbitTokens.accent.copy(alpha = 0.16f)
                    else -> Color.Transparent
                },
                animationSpec = tween(durationMillis = if (selecionada || destacado) 180 else 700),
                label = "pulsoMensagem",
            )
            val brilho by animateFloatAsState(
                targetValue = if (recuada) 0.34f else 1f,
                animationSpec = tween(durationMillis = 180),
                label = "brilhoMensagem",
            )
            val crescer by animateFloatAsState(
                targetValue = if (selecionada) 1.025f else 1f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
                label = "cresceMensagem",
            )
            Box(
                modifier = Modifier
                    .animateItem(fadeInSpec = null, fadeOutSpec = null)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = brilho
                        scaleX = crescer
                        scaleY = crescer
                        transformOrigin = TransformOrigin(
                            pivotFractionX = if (msg.isLuna) 0f else 1f,
                            pivotFractionY = 0.5f,
                        )
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(corPulso)
                    .padding(vertical = 2.dp),
            ) {
                MessageBubble(
                    msg = msg,
                    // Só a última bolha entra animada — abrir a conversa não faz o histórico
                    // inteiro dançar; quem envia/recebe agora é que ganha a chegada com vida.
                    animarEntrada = index == mensagensVisiveis.lastIndex,
                    onLongPress = { onMessageLongPress(msg) },
                    onReferenciarMedia = { att -> onReferenciarMedia(msg, att) },
                )
            }
        }
        if (streamAtivo) {
            item(key = "luna-stream", contentType = "stream") {
                LunaStreamDraft(estado = streamEstado)
            }
        } else {
            // Retomar: última é SUA (resposta se perdeu ao sair do app) ou é um erro da Luna.
            val ultima = mensagensVisiveis.lastOrNull()
            val podeRetry = ultima != null &&
                (!ultima.isLuna || ultima.erro || respostaEhErro(ultima.texto))
            if (podeRetry) {
                item(key = "retry", contentType = "retry") {
                    RetryChip(onClick = onRetry)
                }
            }
        }
    }
}

/**
 * Detalhe legível de uma falha.
 *
 * `e.message` cru às vezes é inútil ("Success", vindo de erro nativo com errno zerado) ou
 * vazio — nesses casos o nome da exceção diz mais do que a mensagem.
 */
private fun detalheFalha(e: Throwable): String {
    val msg = e.message?.trim()?.takeIf {
        it.isNotEmpty() && !it.equals("Success", ignoreCase = true)
    }
    val causa = e.cause?.takeIf { it !== e }
    return when {
        msg != null -> msg
        causa != null -> "${e.javaClass.simpleName} ← ${detalheFalha(causa)}"
        else -> e.javaClass.simpleName
    }
}

/** A última resposta da Luna é um erro (pra oferecer "tentar de novo")? */
private fun respostaEhErro(texto: String): Boolean {
    val t = texto.trimStart()
    return t.startsWith("Não consegui responder") ||
        t.startsWith("Nao consegui responder") ||
        t.startsWith("Erro ao falar") ||
        t.startsWith("Erro ao")
}

/** Botãozinho "tentar de novo" — retoma a mensagem sem duplicá-la pra Luna. */
@Composable
private fun RetryChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(OrbitTokens.surfaceRaised)
                .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(20.dp))
                .orbitPressable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = OrbitTokens.accentText,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Tentar de novo",
                color = OrbitTokens.accentText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
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
    modifier: Modifier = Modifier,
    animarEntrada: Boolean = false,
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

    // Entrada com vida: a bolha sobe do lado de quem fala (você da direita, a Luna da
    // esquerda), cresce a partir do rabinho e assenta com um leve overshoot da mola —
    // viva, sem pular feito app genérico. Só dispara uma vez, quando a bolha nasce.
    val density = LocalDensity.current
    val deslocaBase = with(density) { 12.dp.toPx() }
    var nasceu by remember { mutableStateOf(!animarEntrada) }
    LaunchedEffect(Unit) { nasceu = true }
    val entrada by animateFloatAsState(
        targetValue = if (nasceu) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "entradaBolha",
    )
    val faltando = 1f - entrada

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entrada.coerceIn(0f, 1f)
                translationY = faltando * deslocaBase
                translationX = faltando * (if (msg.isLuna) -deslocaBase else deslocaBase)
                val escala = 0.94f + 0.06f * entrada
                scaleX = escala
                scaleY = escala
                transformOrigin = TransformOrigin(
                    pivotFractionX = if (msg.isLuna) 0f else 1f,
                    pivotFractionY = 1f,
                )
            },
    ) {
        Column(
            horizontalAlignment = if (msg.isLuna) Alignment.Start else Alignment.End,
            modifier = Modifier
                .align(if (msg.isLuna) Alignment.CenterStart else Alignment.CenterEnd)
                .fillMaxWidth(if (msg.isLuna && msg.actionRun != null) 0.96f else 0.82f),
        ) {
            // Pesquisa profunda: a resposta vira um DOSSIÊ (card de relatório com
            // assinatura violeta), montado mais abaixo no lugar da bolha comum. Aqui em
            // cima só entra o strip de ferramentas das tarefas que NÃO são web.
            val dossieRun = if (msg.isLuna) {
                msg.actionRun?.takeIf { it.isDeepResearch() }?.toLegacyResearchRun()
            } else {
                null
            }

            if (msg.isLuna && msg.actionRun != null && dossieRun == null) {
                LunaActionTimeline(
                    run = msg.actionRun,
                    inicialmenteAberto = false,
                )
                Spacer(Modifier.height(8.dp))
            }

            if (mostrarRaciocinio && msg.isLuna && !msg.reasoning.isNullOrBlank() && dossieRun == null) {
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

            if (dossieRun != null && temTexto) {
                LunaDossieCard(
                    run = dossieRun,
                    resposta = msg.texto,
                    inicialmenteAberto = false,
                )
            } else if (temTexto || temReferencia) {
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
    val keyboard = LocalSoftwareKeyboardController.current
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
        // Fecha o teclado na hora do envio — a tela já desce sozinha pra acompanhar a resposta.
        keyboard?.hide()
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

    val gravando =
        recordState == RecordState.Recording || recordState == RecordState.Locked
    val multiLinha = texto.contains('\n') || texto.length > 48
    val isTyping =
        texto.isNotBlank() || anexos.isNotEmpty() || messageReference != null

    // Composer num cartão único (estilo Claude): o campo em cima, os botões dentro
    // embaixo. O formato vem do background/border — SEM .clip(): o clip mascararia o
    // balão de "arraste pra travar", que sobe pra fora do cartão durante a gravação.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitTokens.surface, ComposerFieldShape)
            .border(1.dp, OrbitTokens.borderSoft, ComposerFieldShape),
    ) {
    Column(modifier = Modifier.fillMaxWidth().padding(6.dp)) {

        // Campo — animações de gravação só aqui (não remonta o mic)
        RecordingAnimHost(ativo = gravando) { dotAlpha ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 150.dp)
                    .padding(
                        horizontal = 10.dp,
                        vertical = if (multiLinha && !gravando) 8.dp else 10.dp,
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

        Spacer(Modifier.height(2.dp))

        // Botões dentro do cartão, embaixo: anexos à esquerda, mic/enviar à direita.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        // «+» SEMPRE na árvore (identidade estável). Na gravação ele só fica invisível e
        // sem toque — não é removido: se saísse da árvore, o mic ao lado remontaria e
        // cancelaria o hold. Sem placeholder fantasma, sem a fragilidade de antes.
        val plusEnabled = enabled && !gravando && imageBudget + fileBudget > 0
        val (plusInteraction, plusPressScale) = rememberOrbitPressScale()
        Box(
            Modifier
                .size(44.dp)
                .graphicsLayer {
                    val s = if (gravando) 0f else plusPressScale
                    scaleX = s
                    scaleY = s
                    alpha = if (gravando) 0f else 1f
                }
                .clip(CircleShape)
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

        Spacer(Modifier.weight(1f))

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
        } // Row de controles
    } // Column do cartão
    } // Box do cartão
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
