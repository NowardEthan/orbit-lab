package com.ethan.orbitlab.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.firebase.DocumentoUi
import com.ethan.orbitlab.data.firebase.FirestoreDocumentos
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.lunaMessageIdForUser
import com.ethan.orbitlab.data.newUserMessageId
import com.ethan.orbitlab.data.lunaapi.LunaApiChat
import com.ethan.orbitlab.data.lunaapi.LunaApiClient
import com.ethan.orbitlab.data.voice.VoiceRecorder
import com.google.firebase.auth.FirebaseAuth
import com.ethan.orbitlab.ui.theme.OrbitIconButton
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
fun ChatScreen(
    conversaId: String,
    onBack: () -> Unit,
    mensagemInicial: String? = null,
    onMensagemInicialConsumida: () -> Unit = {},
) {
    val conversa by ChatRepository.observarConversa(conversaId).collectAsState(initial = null)
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    // MutableState passado por referência — header não lê tokens do stream.
    // Fonte da verdade do turno em voo: ChatRepository (sobrevive leave/return).
    val streamState = remember { mutableStateOf<LunaStreamEstado>(LunaStreamEstado.Idle) }
    val streamRepo by ChatRepository.streamDaConversa(conversaId).collectAsState()
    LaunchedEffect(streamRepo) {
        streamState.value = streamRepo
    }
    /** Id da bolha Luna em voo (Railway) — some do histórico enquanto o draft streama. */
    var streamLunaMsgId by remember { mutableStateOf(ChatRepository.lunaMsgIdEmVoo(conversaId)) }
    LaunchedEffect(conversaId) {
        // Ao reabrir no meio do turno, religa o id da bolha em voo.
        streamLunaMsgId = ChatRepository.lunaMsgIdEmVoo(conversaId)
    }
    // Pergunta viva da Luna (ferramenta `perguntar`) — cartão de opções sob a última bolha.
    // Efêmera: nasce quando ela pergunta, morre quando você responde (ou manda outra coisa).
    val perguntaAtiva = remember(conversaId) { mutableStateOf<PerguntaLuna?>(null) }
    var messageReference by remember(conversaId) { mutableStateOf<ThreadReference?>(null) }
    var actionSheetMsg by remember { mutableStateOf<Mensagem?>(null) }
    var exportAberto by remember { mutableStateOf(false) }
    var buscaAberta by remember { mutableStateOf(false) }
    // Documentos desta conversa (a estante) — escutados do Firestore, desenhados como cartões no
    // fio. `docReader` guarda o documento aberto no leitor.
    var documentos by remember(conversaId) { mutableStateOf<List<DocumentoUi>>(emptyList()) }
    var docReader by remember { mutableStateOf<DocumentoUi?>(null) }
    // A estante da conversa (lista dos artefatos) — aberta pela pílula flutuante do canto.
    var estanteAberta by remember { mutableStateOf(false) }
    DisposableEffect(conversaId) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            documentos = emptyList()
            onDispose { }
        } else {
            val reg = FirestoreDocumentos.subscribeDaConversa(
                uid = uid,
                conversaId = conversaId,
                onChange = { documentos = it },
            )
            onDispose { reg.remove() }
        }
    }
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
    // textoParaModelo: instruções técnicas só pro servidor; o balão mostra textoEnvio.
    val dispararResposta = remember(conversaId, streamState, appContext) {
        { textoEnvio: String,
          historicoAntes: List<Mensagem>,
          anexos: List<ComposerAttachment>,
          reference: ThreadReference?,
          userMsgId: String?,
          lunaMsgId: String?,
          reenvio: Boolean,
          textoParaModelo: String? ->
            streamLunaMsgId = lunaMsgId
            streamState.value = LunaStreamEstado.Raciocinando("")
            // Turno novo começou: some com um cartão de pergunta anterior que ficou pendurado.
            perguntaAtiva.value = null
            // Escopo de app: não cancela se a composição sair. Se já há turno ativo, ignora
            // o 2º disparo (leave/return não pode gerar outra resposta por cima).
            val onEstado: (LunaStreamEstado) -> Unit = { e ->
                ChatRepository.publicarStream(conversaId, e)
                streamState.value = e
            }
            val iniciou = ChatRepository.launchTurno(conversaId, lunaMsgId) {
                try {
                    // Fase 5: produto = só luna-core. LunaDirectChat fica fora do caminho.
                    val resultado = LunaApiChat.responder(
                        context = appContext,
                        conversaId = conversaId,
                        historico = historicoAntes,
                        textoUsuario = textoEnvio,
                        anexos = anexos,
                        reference = reference,
                        userMessageId = userMsgId ?: newUserMessageId(),
                        lunaMessageId = lunaMsgId ?: newUserMessageId(),
                        reenvio = reenvio,
                        textoParaModelo = textoParaModelo,
                        onEstado = onEstado,
                    )
                    if (resultado.cotaEsgotada) {
                        // Cota esgotada: a parede graciosa (acima do composer) já dá o recado.
                        // Não cria balão de erro nem persiste — a fala do usuário fica pra retomar
                        // quando a carteira renovar.
                        perguntaAtiva.value = null
                    } else {
                        ChatRepository.enviarMensagem(
                            conversaId = conversaId,
                            texto = resultado.resposta,
                            isLuna = true,
                            reasoning = resultado.reasoning.takeIf { it.isNotBlank() && !resultado.erro },
                            reasoningDuracao = resultado.reasoningDuracao.takeIf { it.isNotBlank() && !resultado.erro },
                            actionRun = if (resultado.erro) null else resultado.actionRun,
                            imagensGeradas = if (resultado.erro) emptyList() else resultado.imagensGeradas,
                            // Reusa o id (substitui um balão de erro no lugar) ou gera novo (anexa).
                            messageId = lunaMsgId,
                            // Erro = aviso local: não vai pra nuvem nem vira memória; some no retry.
                            persistirNuvem = !resultado.erro,
                            erro = resultado.erro,
                        )
                        // Ela perguntou algo? Acende o cartão de opções sob a bolha dela.
                        perguntaAtiva.value = if (resultado.erro) null else resultado.pergunta
                        // Deu certo → a Luna rebatiza a conversa pelo assunto atual (no 1º par
                        // e a cada 6 turnos). Barato e à parte; não trava a resposta.
                        if (!resultado.erro) {
                            ChatRepository.talvezRenomearPelaLuna(conversaId)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ChatRepository.enviarMensagem(
                        conversaId = conversaId,
                        texto = "Erro ao falar com o servidor Luna: ${detalheFalha(e)}",
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
            if (!iniciou) {
                streamLunaMsgId = ChatRepository.lunaMsgIdEmVoo(conversaId) ?: lunaMsgId
                streamState.value = ChatRepository.streamDaConversa(conversaId).value
            }
            Unit
        }
    }

    val onSend = remember(dispararResposta, conversaId) {
        { texto: String, anexos: List<ComposerAttachment>, reference: ThreadReference? ->
            val textoEnvio = texto.trim()
            val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
            // Par de ids ordenado (…-0 tua, …-1 Luna): upsert no Railway não duplica;
            // enquanto o carimbo do servidor não assenta, o Firestore ordena pelo id
            // e a Luna NUNCA cai acima da tua. 🌙
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
            dispararResposta(textoEnvio, historicoAntes, anexos, reference, userMsgId, lunaMsgId, false, null)
            Unit
        }
    }

    // Veio do composer do Início: esta conversa nasceu com um texto pra mandar. Dispara UMA vez,
    // só se a conversa ainda está vazia (não reenviar ao reabrir), e some da mão da tela em seguida.
    LaunchedEffect(conversaId, mensagemInicial) {
        val inicial = mensagemInicial?.trim().orEmpty()
        if (inicial.isEmpty()) return@LaunchedEffect
        val jaTemMensagens = !ChatRepository.getConversa(conversaId)?.mensagens.isNullOrEmpty()
        onMensagemInicialConsumida()
        if (!jaTemMensagens) onSend(inicial, emptyList(), null)
    }

    // Retomar sem duplicar: se a última é uma mensagem SUA sem resposta (saiu do app e a
    // resposta se perdeu), ou um erro da Luna, refaz a resposta pra AQUELA mensagem — sem
    // reenviar a fala do usuário. A Luna vê a mensagem uma vez só, como você pediu. 🌙
    val onRetry = remember(dispararResposta, conversaId) {
        {
            // Já tem geração em curso (saiu e voltou) — não dispara outro turno.
            if (!ChatRepository.turnoEmAndamento(conversaId)) {
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
                        val userMsgId = userMsg.id
                        val lunaMsgId = replyId ?: lunaMessageIdForUser(userMsg.id)
                        // reenvio=true: reescreve o buffer da sessão com o histórico ANTES desta fala,
                        // pra ela entrar uma vez só (o buffer quente do servidor podia tê-la de antes).
                        dispararResposta(userMsg.texto, historicoAntes, emptyList(), userMsg.reference, userMsgId, lunaMsgId, true, null)
                    }
                }
            }
            Unit
        }
    }

    // Turno interrompido (processo morto / rede) deixa a mensagem DELE sozinha.
    // NÃO confundir com «saiu do chat e o job ainda gera» — aí o turno continua no repo.
    var autoRetomadoId by remember(conversaId) { mutableStateOf<String?>(null) }
    val ultimaMsg = conversa?.mensagens?.lastOrNull()
    val ocioso = streamState.value is LunaStreamEstado.Idle &&
        !ChatRepository.turnoEmAndamento(conversaId)
    LaunchedEffect(ultimaMsg?.id, ocioso) {
        val orfa = ultimaMsg ?: return@LaunchedEffect
        if (orfa.isLuna || !ocioso || autoRetomadoId == orfa.id) return@LaunchedEffect
        if (ChatRepository.turnoEmAndamento(conversaId)) return@LaunchedEffect
        if (System.currentTimeMillis() - orfa.timestamp > 30 * 60_000L) return@LaunchedEffect
        // Respira: a resposta pode estar descendo do Firestore neste instante.
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

    // Só volta se a conversa sumir de verdade (evita pop no meio do sync Firestore)
    LaunchedEffect(conversaId) {
        if (ChatRepository.getConversa(conversaId) != null) return@LaunchedEffect
        delay(900)
        if (ChatRepository.getConversa(conversaId) == null) onBack()
    }

    val conversaAtual = conversa
    if (conversaAtual == null) {
        Box(Modifier.fillMaxSize().background(OrbitTokens.graphiteBg))
    } else {
        val historico = conversaAtual.mensagens

        Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OrbitTokens.graphiteBg)
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
                    pergunta = perguntaAtiva.value,
                    onResponderPergunta = { opcao ->
                        perguntaAtiva.value = null
                        onSend(opcao, emptyList(), null)
                    },
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
                    onReferenciarPorSwipe = { msg ->
                        val ref = referenciaAoPuxar(msg, historico)
                        if (ref != null) {
                            messageReference = ref
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onRegerarAspecto = { msg, aspectoLabel, aspectoRatio ->
                        val ref = referenciaAoPuxar(msg, historico)
                        val display = "Refaz em $aspectoRatio"
                        val modelo =
                            "Refaz esta imagem em aspect_ratio=$aspectoRatio ($aspectoLabel). " +
                                "Usa editar_imagem na MESMA arte (não desenhar do zero). " +
                                "Só estender o canvas — sujeito idêntico. " +
                                "NÃO girar 90°. Mesma cena e estilo."
                        val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
                        val userMsgId = newUserMessageId()
                        val lunaMsgId = lunaMessageIdForUser(userMsgId)
                        ChatRepository.enviarMensagem(
                            conversaId = conversaId,
                            texto = display,
                            isLuna = false,
                            reference = ref,
                            messageId = userMsgId,
                        )
                        dispararResposta(
                            display,
                            historicoAntes,
                            emptyList(),
                            ref,
                            userMsgId,
                            lunaMsgId,
                            false,
                            modelo,
                        )
                    },
                )

                // Acesso flutuante à estante — só aparece quando há artefato nesta conversa.
                if (documentos.isNotEmpty()) {
                    EstanteFabConversa(
                        quantidade = documentos.size,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            estanteAberta = true
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 12.dp),
                    )
                }
            }

            // Parede graciosa: cota no teto (medidor zerado) OU 429 do servidor.
            // Antes só o 429 acendia — e um bypass no servidor deixava a mensagem passar
            // mesmo com remaining=0. Agora o composer some assim que não há saldo pro chat.
            val cotaBloqueada by com.ethan.orbitlab.data.billing.UsageRepository.bloqueado.collectAsState()
            val usageCota by com.ethan.orbitlab.data.billing.UsageRepository.usage.collectAsState()
            val semSaldo =
                !usageCota.loading && !usageCota.ilimitado && !usageCota.temSaldoParaChat
            if (cotaBloqueada || semSaldo) {
                com.ethan.orbitlab.ui.planos.LimiteAtingidoCard(
                    usage = usageCota,
                    onVerPlanos = { com.ethan.orbitlab.data.billing.PlanosNav.abrir() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            } else {
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

        if (estanteAberta) {
            EstanteConversaSheet(
                documentos = documentos,
                onAbrir = { doc ->
                    estanteAberta = false
                    docReader = doc
                },
                onDismiss = { estanteAberta = false },
            )
        }

        docReader?.let { doc ->
            DocumentoReaderSheet(
                doc = doc,
                onDismiss = { docReader = null },
                onReferenciarTrecho = { ref ->
                    messageReference = ref
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                            val userMsgId = ancora.id
                            val lunaMsgId = newUserMessageId()
                            // reenvio=true: manda o histórico truncado pro servidor reescrever o
                            // buffer — senão a fala antiga sobrevive e a Luna diz «você já mandou isso».
                            dispararResposta(ancora.texto, historicoAntes, emptyList(), ancora.reference, userMsgId, lunaMsgId, true, null)
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
                .padding(horizontal = OrbitMetrics.pagePadding - 6.dp)
                .padding(top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitIconButton(
                icon = Icons.Rounded.ArrowBackIosNew,
                contentDescription = "Voltar",
                onClick = onBack,
                tint = OrbitTokens.textHiN,
                iconSize = 17.dp,
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Só o título — sem avatar nem linha de status. O topo fica leve e o nome da
            // conversa (que agora assenta e não fica trocando) é o que ancora a tela.
            Text(
                tituloExibido,
                color = OrbitTokens.textHiN,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.weight(1f),
            )

            // Buscar nesta conversa (busca semântica — «pergunta à Luna»).
            OrbitIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = "Buscar na conversa",
                onClick = onBuscar,
                iconSize = 19.dp,
            )

            // Mais ações (exportar…) num menuzinho — tira o peso do topo.
            Box {
                OrbitIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "Mais",
                    onClick = { menuAberto = true },
                    iconSize = 20.dp,
                )
                if (menuAberto) {
                    Popup(
                        alignment = Alignment.TopEnd,
                        offset = IntOffset(
                            0,
                            with(density) { 40.dp.roundToPx() },
                        ),
                        onDismissRequest = { menuAberto = false },
                        properties = PopupProperties(focusable = true),
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(min = 208.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(OrbitTokens.graphiteRaised)
                                .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(14.dp))
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
                .background(OrbitTokens.graphiteHair.copy(alpha = 0.65f)),
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
            tint = OrbitTokens.textHiN,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            rotulo,
            color = OrbitTokens.textHiN,
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
        drawPath(path, color = OrbitTokens.bluePastel)
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
    onReferenciarPorSwipe: (Mensagem) -> Unit = {},
    onRegerarAspecto: (Mensagem, String, String) -> Unit = { _, _, _ -> },
    pergunta: PerguntaLuna? = null,
    onResponderPergunta: (String) -> Unit = {},
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
                    selecionada -> OrbitTokens.bluePastel.copy(alpha = 0.22f)
                    destacado -> OrbitTokens.bluePastel.copy(alpha = 0.16f)
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
            Column(
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
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
                    // Puxar-pra-referenciar (WhatsApp): arrasta a bolha e ela vira a referência.
                    // Desligado quando a folha de ações está aberta, pra os gestos não brigarem.
                    SwipeToReference(
                        enabled = selecionadoId == null,
                        onReferenciar = { onReferenciarPorSwipe(msg) },
                    ) {
                        MessageBubble(
                            msg = msg,
                            // Só a última bolha entra animada — abrir a conversa não faz o histórico
                            // inteiro dançar; quem envia/recebe agora é que ganha a chegada com vida.
                            animarEntrada = index == mensagensVisiveis.lastIndex,
                            onLongPress = { onMessageLongPress(msg) },
                            onReferenciarMedia = { att -> onReferenciarMedia(msg, att) },
                            onRegerarAspecto = { aspectoLabel, aspectoRatio ->
                                onRegerarAspecto(msg, aspectoLabel, aspectoRatio)
                            },
                        )
                    }
                }
            }
        }
        if (streamAtivo) {
            item(key = "luna-stream", contentType = "stream") {
                LunaStreamDraft(estado = streamEstado)
            }
        } else {
            val ultima = mensagensVisiveis.lastOrNull()
            // A Luna fez uma pergunta e a última bolha é dela: cartão de opções logo abaixo.
            if (pergunta != null && ultima?.isLuna == true && !ultima.erro) {
                item(key = "pergunta", contentType = "pergunta") {
                    LunaPerguntaCard(
                        pergunta = pergunta,
                        onResponder = onResponderPergunta,
                    )
                }
            } else {
                // Retomar: última é SUA (resposta se perdeu ao sair do app) ou é um erro da Luna.
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
                .background(OrbitTokens.graphiteRaised)
                .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(20.dp))
                .orbitPressable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Tentar de novo",
                color = OrbitTokens.bluePastel,
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
                    .background(OrbitTokens.graphiteSurf)
                    .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ChatBubbleOutline,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Começa a conversa",
                color = OrbitTokens.textHiN,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "A Luna responde com clareza — texto, voz ou as duas.",
                color = OrbitTokens.textMidN,
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
    onRegerarAspecto: (aspectoLabel: String, aspectoRatio: String) -> Unit = { _, _ -> },
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
                // A Luna ocupa a largura toda (texto solto, sem bolha); você continua na
                // bolha padrão, mais estreita, ancorada à direita.
                .fillMaxWidth(if (msg.isLuna) 1f else 0.82f),
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

            // Imagem que a Luna DESENHOU: cartão no lado dela (esquerda), acima da fala.
            if (msg.isLuna && msg.imagensGeradas.isNotEmpty()) {
                LunaImagemGeradaLista(
                    imagens = msg.imagensGeradas,
                    modifier = Modifier.align(Alignment.Start),
                    onRegerarAspecto = onRegerarAspecto,
                )
                if (temTexto) Spacer(Modifier.height(8.dp))
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
                        .fillMaxWidth()
                        .align(if (msg.isLuna) Alignment.Start else Alignment.End)
                        .then(
                            if (msg.isLuna) {
                                // Fala da Luna: texto solto, largura total, sem bolha nem
                                // rabinho — respira como no Claude/ChatGPT.
                                Modifier
                            } else {
                                // Sua bolha: cinza escura discreta (não mais o accent forte) —
                                // ancora à direita sem gritar; a Luna segue solta à esquerda.
                                Modifier.clip(shape).background(OrbitTokens.bubbleUser)
                            },
                        )
                        .combinedClickable(
                            interactionSource = bubbleInteraction,
                            indication = null,
                            onClick = {},
                            onLongClick = onLongPress,
                        ),
                ) {
                    Column(
                        Modifier.padding(
                            horizontal = if (msg.isLuna) 0.dp else 14.dp,
                            vertical = if (msg.isLuna) 0.dp else 10.dp,
                        ),
                    ) {
                        if (temReferencia && msg.reference != null) {
                            MessageReferenceQuote(reference = msg.reference)
                            if (temTexto) Spacer(Modifier.height(8.dp))
                        }
                        if (temTexto) {
                            if (msg.isLuna) {
                                // Modo técnico: conserta o registro (abertura casual + caixa-baixa)
                                // na exibição — o texto gravado no Firestore fica intacto.
                                val conteudo = if (PrefsRepository.mensagemEhTecnica(msg.id)) {
                                    formalizarTecnico(msg.texto)
                                } else {
                                    msg.texto
                                }
                                LunaMarkdown(content = conteudo)
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
                color = OrbitTokens.textLowN.copy(alpha = 0.85f),
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
fun ChatInputArea(
    onSend: (String, List<ComposerAttachment>, ThreadReference?) -> Unit,
    streamState: MutableState<LunaStreamEstado>,
    messageReference: ThreadReference? = null,
    onClearReference: () -> Unit = {},
    modifier: Modifier = Modifier,
    containerColor: Color = OrbitTokens.graphiteSurf,
    exibirSeletorModo: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember(context) { VoiceRecorder(context.applicationContext) }
    DisposableEffect(recorder) {
        onDispose { recorder.cancel() }
    }

    var transcrevendo by remember { mutableStateOf(false) }
    val enabled = streamState.value is LunaStreamEstado.Idle && !transcrevendo
    val modoTecnico by PrefsRepository.modoTecnico.collectAsState()
    val modoAgentico by PrefsRepository.modoAgentico.collectAsState()
    val modoAtivo = when {
        modoAgentico -> ModoLunaOpcao.MaosAObra
        modoTecnico -> ModoLunaOpcao.Tecnico
        else -> ModoLunaOpcao.Conversa
    }
    var texto by remember { mutableStateOf("") }
    var anexos by remember { mutableStateOf<List<ComposerAttachment>>(emptyList()) }
    var attachAberto by remember { mutableStateOf(false) }
    var modoSheetAberto by remember { mutableStateOf(false) }
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

    fun temMic(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    val pedirMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "Permita o microfone para mandar áudio pra Luna.",
                Toast.LENGTH_SHORT,
            ).show()
        }
        // O hold já soltou enquanto o diálogo estava aberto — próxima tentativa grava.
    }

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

    fun resetar(descartarAudio: Boolean = true) {
        if (descartarAudio) recorder.cancel()
        recordState = RecordState.Idle
        dragOffset = Offset.Zero
        segundos = 0
    }

    /** Grava de verdade → STT no servidor → chat com o texto (como o orbit-mobile). */
    fun enviarAudio() {
        val clip = recorder.finish()
        resetar(descartarAudio = false)
        if (clip == null) {
            Toast.makeText(context, "Áudio curto demais — segura um pouco mais.", Toast.LENGTH_SHORT)
                .show()
            return
        }
        keyboard?.hide()
        transcrevendo = true
        scope.launch {
            try {
                val b64 = withContext(Dispatchers.IO) {
                    val bytes = clip.file.readBytes()
                    clip.file.delete()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
                val token = AuthRepository.getIdToken()
                when (val r = LunaApiClient.transcribe(token, b64, clip.mimeType)) {
                    is LunaApiClient.TranscribeResult.Ok -> {
                        onSend(r.text, emptyList(), messageReference)
                    }
                    is LunaApiClient.TranscribeResult.Erro -> {
                        Toast.makeText(
                            context,
                            "Não ouvi o áudio: ${r.mensagem}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            } catch (e: Exception) {
                clip.file.delete()
                Toast.makeText(
                    context,
                    "Falha ao transcrever: ${e.message ?: "erro desconhecido"}",
                    Toast.LENGTH_LONG,
                ).show()
            } finally {
                transcrevendo = false
            }
        }
    }

    fun enviarTexto() {
        if (texto.isBlank() && anexos.isEmpty() && messageReference == null) return
        // Fecha o teclado na hora do envio — a tela já desce sozinha pra acompanhar a resposta.
        keyboard?.hide()
        onSend(texto.trim(), anexos, messageReference)
        texto = ""
        anexos = emptyList()
    }

    fun tentarIniciarGravacao(): Boolean {
        if (!temMic()) {
            pedirMic.launch(Manifest.permission.RECORD_AUDIO)
            return false
        }
        if (!recorder.start()) {
            Toast.makeText(context, "Não deu pra gravar. Tenta de novo.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
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

    LunaModeSheet(
        visible = modoSheetAberto,
        modoAtivo = modoAtivo,
        onSelect = { opcao ->
            when (opcao) {
                ModoLunaOpcao.Conversa -> {
                    PrefsRepository.setModoTecnico(false)
                    PrefsRepository.setModoAgentico(false)
                }
                ModoLunaOpcao.Tecnico -> PrefsRepository.setModoTecnico(true)
                ModoLunaOpcao.MaosAObra -> PrefsRepository.setModoAgentico(true)
            }
        },
        onDismiss = { modoSheetAberto = false },
    )

    Column(modifier = modifier.fillMaxWidth()) {
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
            .background(containerColor, ComposerFieldShape)
            .border(1.dp, OrbitTokens.graphiteHair, ComposerFieldShape),
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
                    gravando || transcrevendo -> Alignment.CenterStart
                    multiLinha -> Alignment.TopStart
                    else -> Alignment.CenterStart
                },
            ) {
                when {
                    transcrevendo -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = OrbitTokens.bluePastel,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Ouvindo o áudio…",
                                color = OrbitTokens.textMidN,
                                fontSize = 15.sp,
                            )
                        }
                    }
                    recordState == RecordState.Idle -> {
                        BasicTextField(
                            value = texto,
                            onValueChange = { texto = it },
                            textStyle = TextStyle(
                                color = OrbitTokens.textHiN,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(OrbitTokens.bluePastel),
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
                                            color = OrbitTokens.textLowN,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                    recordState == RecordState.Recording -> {
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
                            Text(formatTimer(segundos), color = OrbitTokens.textHiN, fontSize = 15.sp)
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
                                    tint = OrbitTokens.textLowN,
                                    modifier = Modifier.size(11.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    "arraste pra cancelar",
                                    color = OrbitTokens.textLowN,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    else -> {
                        // Locked
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
                                    tint = OrbitTokens.textMidN,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    "Descartar",
                                    color = OrbitTokens.textMidN,
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
                            Text(formatTimer(segundos), color = OrbitTokens.textHiN, fontSize = 15.sp)
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = OrbitTokens.bluePastel,
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
                tint = if (anexos.isNotEmpty()) OrbitTokens.bluePastel else OrbitTokens.textHiN,
                modifier = Modifier.size(24.dp),
            )
        }

        // Botão de modo: some na gravação (a linha vira controles de áudio). Mostra o modo
        // ATIVO (ícone + nome) e abre o seletor. Discreto tipo Claude: pílula-fantasma neutra
        // sempre — SEM preenchimento colorido; o modo aceso (Técnico/Ação) só tinge o texto e o
        // ícone de azul pastel, Conversa fica cinza. O chevron avisa que abre um menu, não alterna.
        if (!gravando && exibirSeletorModo) {
            Spacer(Modifier.width(8.dp))
            val modoAceso = modoAtivo != ModoLunaOpcao.Conversa
            val tecShape = RoundedCornerShape(50)
            val tecTint = if (modoAceso) OrbitTokens.bluePastel else OrbitTokens.textMidN
            val modoIcone = when (modoAtivo) {
                ModoLunaOpcao.Tecnico -> Icons.Rounded.Tune
                ModoLunaOpcao.MaosAObra -> Icons.Rounded.Handyman
                ModoLunaOpcao.Conversa -> Icons.Rounded.ChatBubbleOutline
            }
            val modoRotulo = when (modoAtivo) {
                ModoLunaOpcao.Tecnico -> "Técnico"
                ModoLunaOpcao.MaosAObra -> "Ação"
                ModoLunaOpcao.Conversa -> "Conversa"
            }
            Row(
                Modifier
                    .clip(tecShape)
                    .clickable(enabled = enabled) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        modoSheetAberto = true
                    }
                    .padding(start = 8.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modoIcone,
                    contentDescription = null,
                    tint = tecTint,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    modoRotulo,
                    color = tecTint,
                    fontSize = 13.sp,
                    fontWeight = if (modoAceso) FontWeight.SemiBold else FontWeight.Medium,
                )
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = "Trocar modo",
                    tint = tecTint.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp),
                )
            }
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
        // Enviar/mic acendem no pastel com o ícone escuro (contraste), igual ao composer do
        // Início. Só a gravação foge pro vermelho.
        val corBotao = when {
            recordState != RecordState.Idle -> OrbitTokens.danger
            else -> OrbitTokens.bluePastel
        }
        val tintIcone = when {
            recordState != RecordState.Idle -> Color.White
            else -> OrbitTokens.onBluePastel
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
                        if (!tentarIniciarGravacao()) return@awaitEachGesture
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
                        .background(OrbitTokens.graphiteRaised.copy(alpha = 0.9f))
                        .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(OrbitMetrics.radiusCard)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = lerp(OrbitTokens.textLowN, OrbitTokens.bluePastel, lockProgresso),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.height(2.dp))
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                            tint = OrbitTokens.textLowN.copy(alpha = 1f - lockProgresso),
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
