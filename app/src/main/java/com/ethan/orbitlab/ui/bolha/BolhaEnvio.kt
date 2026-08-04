package com.ethan.orbitlab.ui.bolha

import android.app.Application
import android.util.Base64
import android.widget.Toast
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.lunaMessageIdForUser
import com.ethan.orbitlab.data.lunaapi.LunaApiChat
import com.ethan.orbitlab.data.lunaapi.LunaApiClient
import com.ethan.orbitlab.data.newUserMessageId
import com.ethan.orbitlab.data.voice.VoiceClip
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import com.ethan.orbitlab.ui.chat.detalheFalha
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/** Envio rápido da bolha (B5) — texto/áudio na conversa principal, sem painel. */
object BolhaEnvio {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Grava → Whisper → [enviarTexto]. */
    fun enviarAudio(app: Application, clip: VoiceClip) {
        scope.launch {
            try {
                val b64 = withContext(Dispatchers.IO) {
                    val bytes = clip.file.readBytes()
                    clip.file.delete()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
                val token = AuthRepository.getIdToken()
                when (val r = LunaApiClient.transcribe(token, b64, clip.mimeType)) {
                    is LunaApiClient.TranscribeResult.Ok -> enviarTexto(app, r.text)
                    is LunaApiClient.TranscribeResult.Erro -> {
                        Toast.makeText(
                            app,
                            "Não ouvi o áudio: ${r.mensagem}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            } catch (e: Exception) {
                clip.file.delete()
                Toast.makeText(
                    app,
                    "Falha ao transcrever: ${e.message ?: "erro desconhecido"}",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    fun enviarTexto(app: Application, texto: String) {
        val limpo = texto.trim()
        if (limpo.isEmpty()) return
        val conversaId = ChatRepository.conversaPrincipal()
        if (ChatRepository.turnoEmAndamento(conversaId)) return
        val historicoAntes = ChatRepository.getConversa(conversaId)?.mensagens.orEmpty()
        val userMsgId = newUserMessageId()
        val lunaMsgId = lunaMessageIdForUser(userMsgId)
        ChatRepository.enviarMensagem(
            conversaId = conversaId,
            texto = limpo,
            isLuna = false,
            messageId = userMsgId,
        )
        ChatRepository.publicarStream(conversaId, LunaStreamEstado.Raciocinando(""))
        BolhaSinal.setPensando(true)
        ChatRepository.launchTurno(conversaId, lunaMsgId) {
            try {
                val r = LunaApiChat.responder(
                    context = app,
                    conversaId = conversaId,
                    historico = historicoAntes,
                    textoUsuario = limpo,
                    anexos = emptyList(),
                    reference = null,
                    userMessageId = userMsgId,
                    lunaMessageId = lunaMsgId,
                    onEstado = { ChatRepository.publicarStream(conversaId, it) },
                )
                if (r.cotaEsgotada) return@launchTurno
                ChatRepository.enviarMensagem(
                    conversaId = conversaId,
                    texto = r.resposta,
                    isLuna = true,
                    reasoning = r.reasoning.takeIf { it.isNotBlank() && !r.erro },
                    reasoningDuracao = r.reasoningDuracao.takeIf { it.isNotBlank() && !r.erro },
                    actionRun = if (r.erro) null else r.actionRun,
                    imagensGeradas = if (r.erro) emptyList() else r.imagensGeradas,
                    messageId = lunaMsgId,
                    persistirNuvem = !r.erro,
                    erro = r.erro,
                )
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
                ChatRepository.publicarStream(conversaId, LunaStreamEstado.Idle)
                BolhaSinal.setPensando(false)
            }
        }
    }
}
