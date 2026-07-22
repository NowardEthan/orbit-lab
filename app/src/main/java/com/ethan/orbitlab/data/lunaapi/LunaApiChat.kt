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
            )
        }

        val idToken = AuthRepository.getIdToken()
        if (idToken.isNullOrBlank()) {
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "",
                resposta = "Precisa estar logado pra falar com o servidor Luna.",
            )
        }

        val uid = AuthRepository.session.value?.uid
        if (uid.isNullOrBlank()) {
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "",
                resposta = "Sessão inválida — tenta entrar de novo.",
            )
        }

        val displayMessage = textoUsuario.trim()
        val messageBase = when {
            reference != null -> formatMessageWithReference(displayMessage, reference)
            else -> displayMessage
        }.ifBlank {
            if (anexos.isNotEmpty()) "Veja os anexos." else "Oi"
        }

        // Referência a imagem antiga: sobe de novo pro Storage (PAIA precisa da URL).
        val anexosParaEnviar = anexos.toMutableList()
        if (reference is ThreadReference.Image && reference.uri != null) {
            val jaTem = anexosParaEnviar.any {
                it.uri == reference.uri || it.id == reference.attachmentId
            }
            if (!jaTem) {
                anexosParaEnviar += ComposerAttachment(
                    id = reference.attachmentId,
                    kind = AttachmentKind.IMAGE,
                    name = reference.attachmentName,
                    sizeLabel = "",
                    mime = "image/jpeg",
                    uri = reference.uri,
                )
            }
        }

        val uploaded = if (anexosParaEnviar.isNotEmpty()) {
            ChatMediaUpload.uploadAttachments(
                context = context,
                uid = uid,
                conversationId = conversaId,
                messageId = userMessageId,
                attachments = anexosParaEnviar,
            )
        } else {
            anexosParaEnviar
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

        // Mitigação no cliente até o prompt do `ver_imagem` no luna-core endurecer OCR.
        val message = if (attachmentsJson.length() > 0) {
            buildString {
                appendLine("[Instrução de visão — obrigatória]")
                appendLine(
                    "Se precisar ler a imagem/vídeo, use a ferramenta ver_imagem (ou equivalente). " +
                        "Transcreva texto LITERALMENTE entre aspas. Não complete nomes de ruas, " +
                        "linhas de ônibus/BRT, paradas, placas ou números. " +
                        "Se estiver borrado ou incerto, diga exatamente «ilegível» — não invente leitura plausível.",
                )
                appendLine()
                append(messageBase)
            }
        } else {
            messageBase
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
                val parcial = when {
                    reasoning.isNotBlank() -> reasoning
                    faseAtual.isNotBlank() -> rotuloFase(faseAtual)
                    else -> ""
                }
                LunaStreamEstado.Raciocinando(parcial)
            }
            mainHandler.post { onEstado(estado) }
        }

        val result = LunaApiClient.chatStream(idToken, body) { event ->
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
