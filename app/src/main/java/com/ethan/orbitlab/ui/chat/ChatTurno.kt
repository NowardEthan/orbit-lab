package com.ethan.orbitlab.ui.chat

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.lunaMessageIdForUser
import com.ethan.orbitlab.data.lunaapi.LunaApiChat
import com.ethan.orbitlab.data.newUserMessageId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Orquestração de um turno Luna — compartilhada entre [ChatScreen], painel da bolha e quick reply.
 * Garante retry sem duplicar a fala do usuário, pergunta ativa, rename e cota.
 */
class ChatTurno(
    val conversaId: String,
    val streamState: MutableState<LunaStreamEstado>,
    val perguntaAtiva: MutableState<PerguntaLuna?>,
    private val appContext: Context,
    private val setStreamLunaMsgId: (String?) -> Unit,
    private val onAposRespostaOk: (() -> Unit)? = null,
) {
    val dispararResposta: (
        textoEnvio: String,
        historicoAntes: List<Mensagem>,
        anexos: List<ComposerAttachment>,
        reference: ThreadReference?,
        userMsgId: String?,
        lunaMsgId: String?,
        reenvio: Boolean,
        textoParaModelo: String?,
    ) -> Unit = disparar@{ textoEnvio, historicoAntes, anexos, reference, userMsgId, lunaMsgId, reenvio, textoParaModelo ->
        setStreamLunaMsgId(lunaMsgId)
        streamState.value = LunaStreamEstado.Raciocinando("")
        perguntaAtiva.value = null
        val onEstado: (LunaStreamEstado) -> Unit = { e ->
            ChatRepository.publicarStream(conversaId, e)
            streamState.value = e
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
                    userMessageId = userMsgId ?: newUserMessageId(),
                    lunaMessageId = lunaMsgId ?: newUserMessageId(),
                    reenvio = reenvio,
                    textoParaModelo = textoParaModelo,
                    onEstado = onEstado,
                )
                if (resultado.cotaEsgotada) {
                    perguntaAtiva.value = null
                } else {
                    ChatRepository.enviarMensagem(
                        conversaId = conversaId,
                        texto = resultado.resposta,
                        isLuna = true,
                        reasoning = resultado.reasoning.takeIf { it.isNotBlank() && !resultado.erro },
                        reasoningDuracao = resultado.reasoningDuracao.takeIf {
                            it.isNotBlank() && !resultado.erro
                        },
                        actionRun = if (resultado.erro) null else resultado.actionRun,
                        imagensGeradas = if (resultado.erro) emptyList() else resultado.imagensGeradas,
                        messageId = lunaMsgId,
                        persistirNuvem = !resultado.erro,
                        erro = resultado.erro,
                    )
                    perguntaAtiva.value = if (resultado.erro) null else resultado.pergunta
                    if (!resultado.erro) {
                        ChatRepository.talvezRenomearPelaLuna(conversaId)
                        onAposRespostaOk?.invoke()
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
                setStreamLunaMsgId(null)
            }
        }
        if (!iniciou) {
            setStreamLunaMsgId(ChatRepository.lunaMsgIdEmVoo(conversaId) ?: lunaMsgId)
            streamState.value = ChatRepository.streamDaConversa(conversaId).value
        }
    }

    val onSend: (String, List<ComposerAttachment>, ThreadReference?) -> Unit =
        enviar@{ texto, anexos, reference ->
            val textoEnvio = texto.trim()
            if (textoEnvio.isEmpty() && anexos.isEmpty() && reference == null) return@enviar
            val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
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
            dispararResposta(
                textoEnvio,
                historicoAntes,
                anexos,
                reference,
                userMsgId,
                lunaMsgId,
                false,
                null,
            )
        }

    /** Retry órfã / erro Luna — sem reenviar a fala do usuário. */
    val onRetry: () -> Unit = {
        if (!ChatRepository.turnoEmAndamento(conversaId)) {
            val msgs = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
            val ultima = msgs.lastOrNull()
            if (ultima != null && streamState.value is LunaStreamEstado.Idle) {
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
                    val userMsgId = userMsg.id
                    val lunaMsgId = replyId ?: lunaMessageIdForUser(userMsg.id)
                    dispararResposta(
                        userMsg.texto,
                        historicoAntes,
                        emptyList(),
                        userMsg.reference,
                        userMsgId,
                        lunaMsgId,
                        true,
                        null,
                    )
                }
            }
        }
    }

    fun regerarAspecto(
        msg: Mensagem,
        historico: List<Mensagem>,
        aspectoLabel: String,
        aspectoRatio: String,
    ) {
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
    }

    fun reenviarDesde(sheetMsg: Mensagem) {
        if (streamState.value !is LunaStreamEstado.Idle) return
        val msgs = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
        val ancora: Mensagem? = if (!sheetMsg.isLuna) {
            sheetMsg
        } else {
            val idx = msgs.indexOfFirst { it.id == sheetMsg.id }
            if (idx >= 0) msgs.take(idx).lastOrNull { !it.isLuna } else null
        }
        if (ancora == null) return
        val historicoAntes = msgs.take(msgs.indexOfFirst { it.id == ancora.id })
        ChatRepository.truncarApos(conversaId, ancora.id)
        val userMsgId = ancora.id
        val lunaMsgId = newUserMessageId()
        dispararResposta(
            ancora.texto,
            historicoAntes,
            emptyList(),
            ancora.reference,
            userMsgId,
            lunaMsgId,
            true,
            null,
        )
    }
}

data class ChatTurnoState(
    val turno: ChatTurno,
    val streamLunaMsgId: String?,
    val ocioso: Boolean,
)

@Composable
fun rememberChatTurno(
    conversaId: String,
    appContext: Context,
    onAposRespostaOk: (() -> Unit)? = null,
): ChatTurnoState {
    val streamState = remember { mutableStateOf<LunaStreamEstado>(LunaStreamEstado.Idle) }
    val streamRepo by ChatRepository.streamDaConversa(conversaId).collectAsState()
    LaunchedEffect(streamRepo) { streamState.value = streamRepo }

    val streamLunaMsgIdState = remember {
        mutableStateOf(ChatRepository.lunaMsgIdEmVoo(conversaId))
    }
    LaunchedEffect(conversaId) {
        streamLunaMsgIdState.value = ChatRepository.lunaMsgIdEmVoo(conversaId)
    }

    val perguntaAtiva = remember(conversaId) { mutableStateOf<PerguntaLuna?>(null) }
    val onAposOkAtualizado = rememberUpdatedOnApos(onAposRespostaOk)

    val turno = remember(conversaId, appContext, streamState, perguntaAtiva, streamLunaMsgIdState) {
        ChatTurno(
            conversaId = conversaId,
            streamState = streamState,
            perguntaAtiva = perguntaAtiva,
            appContext = appContext,
            setStreamLunaMsgId = { streamLunaMsgIdState.value = it },
            onAposRespostaOk = { onAposOkAtualizado.value?.invoke() },
        )
    }

    val ocioso = streamState.value is LunaStreamEstado.Idle &&
        !ChatRepository.turnoEmAndamento(conversaId)

    val conversa by ChatRepository.observarConversa(conversaId).collectAsState(initial = null)
    val ultimaMsg = conversa?.mensagens?.lastOrNull()
    var autoRetomadoId by remember(conversaId) { mutableStateOf<String?>(null) }
    LaunchedEffect(ultimaMsg?.id, ocioso, conversaId) {
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
        turno.onRetry()
    }

    return ChatTurnoState(
        turno = turno,
        streamLunaMsgId = streamLunaMsgIdState.value,
        ocioso = ocioso,
    )
}

@Composable
private fun rememberUpdatedOnApos(
    onAposRespostaOk: (() -> Unit)?,
): MutableState<(() -> Unit)?> {
    val state = remember { mutableStateOf(onAposRespostaOk) }
    state.value = onAposRespostaOk
    return state
}

/** A última resposta da Luna é um erro (pra oferecer "tentar de novo")? */
fun respostaEhErro(texto: String): Boolean {
    val t = texto.trimStart()
    return t.startsWith("Não consegui responder") ||
        t.startsWith("Nao consegui responder") ||
        t.startsWith("Erro ao falar") ||
        t.startsWith("Erro ao")
}

fun detalheFalha(e: Throwable): String {
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
