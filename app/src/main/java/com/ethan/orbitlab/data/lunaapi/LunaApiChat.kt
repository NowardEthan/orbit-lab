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
import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Chat via mobile-api no Railway — resposta completa (sem SSE).
 * Mostra só “Pensando…” até o JSON chegar.
 */
object LunaApiChat {

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
        onEstado(LunaStreamEstado.Raciocinando(""))

        val result = LunaApiClient.chat(idToken, body)
        val totalMs = System.currentTimeMillis() - t0
        val dur = (totalMs / 1000.0).roundToInt().coerceAtLeast(1)

        if (result.error != null) {
            LatenciaProbe.record(
                caminho = "paia_json",
                totalMs = totalMs,
                ttfbMs = totalMs,
                chars = 0,
                ok = false,
                detalhe = result.error.take(80),
            )
            return LunaStreamResultado(
                reasoning = "",
                reasoningDuracao = "${dur}s",
                resposta = "Não consegui responder: ${result.error}",
            )
        }

        val texto = result.text.ifBlank { "…" }
        LatenciaProbe.record(
            caminho = "paia_json",
            totalMs = totalMs,
            ttfbMs = totalMs,
            chars = texto.length,
            ok = true,
        )

        return LunaStreamResultado(
            reasoning = "",
            reasoningDuracao = "${dur}s",
            resposta = texto,
        )
    }
}
