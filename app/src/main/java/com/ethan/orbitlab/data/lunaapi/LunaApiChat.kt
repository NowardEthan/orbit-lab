package com.ethan.orbitlab.data.lunaapi

import android.content.Context
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.data.firebase.ChatMediaUpload
import com.ethan.orbitlab.data.latencia.LatenciaProbe
import com.ethan.orbitlab.ui.chat.AttachmentKind
import com.ethan.orbitlab.ui.chat.ComposerAttachment
import com.ethan.orbitlab.ui.chat.LunaStreamEstado
import com.ethan.orbitlab.ui.chat.LunaStreamResultado
import com.ethan.orbitlab.ui.chat.ThreadReference
import com.ethan.orbitlab.ui.chat.formatMessageWithReference
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone
import kotlin.math.roundToInt

/** Falha transitória (rede/DNS/timeout/conexão abortada/5xx) que vale re-tentar. */
private fun erroTransitorio(msg: String?): Boolean {
    if (msg == null) return true
    val m = msg.lowercase()
    return listOf(
        "unable to resolve host", "failed to connect", "timeout", "timed out",
        "econnreset", "connection reset", "connection closed", "connection abort",
        "unexpected end", "network", "http 5", " 500", " 502", " 503", " 504",
        "stream fechou", "falha de rede",
    ).any { m.contains(it) }
}

/**
 * Chat via mobile-api no Railway — SSE em `/v1/chat/stream`.
 * Mostra fases (analisando / memória / escrevendo) e pinta o texto conforme chega.
 */
object LunaApiChat {

    private fun rotuloFase(phase: String): String = when (phase) {
        "analysing" -> "Analisando…"
        "memory" -> "Memória…"
        "writing" -> "Escrevendo…"
        else -> "Pensando…"
    }

    suspend fun responder(
        context: Context,
        conversaId: String,
        historico: List<com.ethan.orbitlab.data.Mensagem>,
        textoUsuario: String,
        anexos: List<ComposerAttachment>,
        reference: ThreadReference?,
        userMessageId: String,
        lunaMessageId: String,
        onEstado: (LunaStreamEstado) -> Unit,
    ): LunaStreamResultado {
        if (!LunaApiConfig.isConfigured()) {
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "",
                resposta = "URL da API Luna não configurada. Coloca `luna.api.url` no local.properties " +
                    "ou `EXPO_PUBLIC_LUNA_API_URL` no orbit-mobile/.env e recompila.",
                erro = true,
            )
        }

        val idToken = AuthRepository.getIdToken()
        if (idToken.isNullOrBlank()) {
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "",
                resposta = "Precisa estar logado pra falar com o servidor Luna.",
                erro = true,
            )
        }

        val uid = AuthRepository.session.value?.uid
        if (uid.isNullOrBlank()) {
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "",
                resposta = "Sessão inválida — tenta entrar de novo.",
                erro = true,
            )
        }

        val displayMessage = textoUsuario.trim()
        val message = when {
            reference != null -> formatMessageWithReference(displayMessage, reference)
            else -> displayMessage
        }.ifBlank {
            if (anexos.isNotEmpty()) "Veja os anexos." else "Oi"
        }

        val uploaded = if (anexos.isNotEmpty()) {
            ChatMediaUpload.uploadAttachments(
                context = context,
                uid = uid,
                conversationId = conversaId,
                messageId = userMessageId,
                attachments = anexos,
            )
        } else {
            anexos
        }

        val attachmentsJson = JSONArray()
        val documentsJson = JSONArray()
        uploaded.forEach { att ->
            val url = att.uri?.toString()?.takeIf { ChatMediaUpload.isRemoteUri(att.uri) }
            if (url.isNullOrBlank()) return@forEach
            when (att.kind) {
                AttachmentKind.FILE -> {
                    documentsJson.put(
                        JSONObject().apply {
                            put("id", att.id)
                            put("name", att.name)
                            put("mimeType", att.mime.ifBlank { "application/octet-stream" })
                            put("url", url)
                        },
                    )
                }
                AttachmentKind.IMAGE, AttachmentKind.VIDEO -> {
                    attachmentsJson.put(
                        JSONObject().apply {
                            put("id", att.id)
                            put("name", att.name)
                            put(
                                "mimeType",
                                att.mime.ifBlank {
                                    if (att.kind == AttachmentKind.VIDEO) "video/mp4" else "image/jpeg"
                                },
                            )
                            put("url", url)
                        },
                    )
                }
            }
        }

        val session = AuthRepository.session.value
        val profile = UserProfileRepository.profile.value
        val nome = profile.displayName.ifBlank { session?.displayName.orEmpty() }

        val body = JSONObject().apply {
            put("message", message)
            put("displayMessage", displayMessage.ifBlank { message })
            put("sessionId", conversaId)
            put("userMessageId", userMessageId)
            put("lunaMessageId", lunaMessageId)
            put("providerId", "auto")
            put("modelKey", "auto")
            put("timeZone", TimeZone.getDefault().id)
            put("reasoningEnabled", PrefsRepository.reasoningEnabled.value)
            put("reasoningEffort", "medium")
            if (nome.isNotBlank()) put("userDisplayName", nome.take(64))
            if (attachmentsJson.length() > 0) put("attachments", attachmentsJson)
            if (documentsJson.length() > 0) put("documents", documentsJson)
        }

        val t0 = System.currentTimeMillis()
        val mainHandler = Handler(Looper.getMainLooper())
        onEstado(LunaStreamEstado.Raciocinando(""))

        val respostaBuf = StringBuilder()
        val reasoningBuf = StringBuilder()
        var faseAtual = ""

        fun emitirUi() {
            val texto = respostaBuf.toString()
            val reasoning = reasoningBuf.toString()
            val durMs = System.currentTimeMillis() - t0
            val durLabel = "${(durMs / 1000.0).roundToInt().coerceAtLeast(1)}s"
            val estado = if (texto.isNotEmpty()) {
                LunaStreamEstado.Respondendo(
                    reasoning = reasoning,
                    reasoningDuracao = durLabel,
                    respostaParcial = texto,
                )
            } else {
                // Raciocínio real (se houver) vira a caixa; a fase é só o rótulo do "Pensando…".
                // Antes a fase entrava no parcial e virava uma caixa de raciocínio fantasma.
                LunaStreamEstado.Raciocinando(
                    parcial = reasoning,
                    fase = if (faseAtual.isNotBlank()) rotuloFase(faseAtual) else "",
                )
            }
            mainHandler.post { onEstado(estado) }
        }

        // Retry em falha de rede/conexão: se abortar SEM ter transmitido nada, evicta o
        // socket morto do pool e tenta de novo (não duplica texto já mostrado).
        var result = LunaApiClient.ChatResult(text = "", sessionId = "")
        var tentativa = 0
        val maxTentativas = 3
        while (true) {
            tentativa++
            respostaBuf.setLength(0)
            reasoningBuf.setLength(0)
            faseAtual = ""
            result = LunaApiClient.chatStream(idToken, body) { event ->
                when (event) {
                    is LunaApiClient.StreamEvent.Status -> {
                        faseAtual = event.phase
                        if (respostaBuf.isEmpty()) emitirUi()
                    }
                    is LunaApiClient.StreamEvent.Reasoning -> {
                        reasoningBuf.append(event.delta)
                        emitirUi()
                    }
                    is LunaApiClient.StreamEvent.Content -> {
                        respostaBuf.append(event.delta)
                        emitirUi()
                    }
                    is LunaApiClient.StreamEvent.Acao -> {
                        // L1: só marca atividade; UI de tools fica pra depois
                        if (respostaBuf.isEmpty() && reasoningBuf.isEmpty()) {
                            faseAtual = "writing"
                            emitirUi()
                        }
                    }
                    is LunaApiClient.StreamEvent.Error -> Unit
                }
            }
            val podeRepetir = result.error != null && respostaBuf.isEmpty() &&
                erroTransitorio(result.error)
            if (podeRepetir && tentativa < maxTentativas) {
                LunaApiClient.evictConnections()
                mainHandler.post { onEstado(LunaStreamEstado.Raciocinando("")) }
                kotlinx.coroutines.delay(tentativa * 1200L)
                continue
            }
            break
        }

        val totalMs = System.currentTimeMillis() - t0
        val dur = (totalMs / 1000.0).roundToInt().coerceAtLeast(1)
        val phasesDetail = result.phasesMs.entries
            .joinToString(" ") { "${it.key}=${it.value}ms" }
            .ifBlank { null }

        if (result.error != null && result.text.isBlank()) {
            LatenciaProbe.record(
                caminho = "paia_stream",
                totalMs = totalMs,
                ttfbMs = result.ttfbMs,
                chars = 0,
                ok = false,
                detalhe = listOfNotNull(result.error.take(80), phasesDetail).joinToString(" | "),
            )
            return LunaStreamResultado(
                reasoning = result.reasoning,
                reasoningDuracao = "${dur}s",
                resposta = "Não consegui responder: ${result.error}",
                erro = true,
            )
        }

        val texto = result.text.ifBlank { respostaBuf.toString() }.ifBlank { "…" }
        val reasoning = result.reasoning.ifBlank { reasoningBuf.toString() }
        LatenciaProbe.record(
            caminho = "paia_stream",
            totalMs = totalMs,
            ttfbMs = result.ttfbMs,
            chars = texto.length,
            ok = true,
            detalhe = phasesDetail,
        )

        return LunaStreamResultado(
            reasoning = reasoning,
            reasoningDuracao = "${dur}s",
            resposta = texto,
        )
    }
}
