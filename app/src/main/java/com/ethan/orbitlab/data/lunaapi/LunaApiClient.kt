package com.ethan.orbitlab.data.lunaapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente da mobile-api — POST /v1/chat (resposta completa, sem SSE).
 */
object LunaApiClient {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class ChatResult(
        val text: String,
        val sessionId: String,
        val idempotent: Boolean = false,
        val error: String? = null,
    )

    suspend fun chat(
        idToken: String?,
        body: JSONObject,
    ): ChatResult = withContext(Dispatchers.IO) {
        if (!LunaApiConfig.isConfigured()) {
            return@withContext ChatResult(
                text = "",
                sessionId = "",
                error = "LUNA_API_URL ausente no build.",
            )
        }

        val request = Request.Builder()
            .url(LunaApiConfig.chatUrl)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .apply {
                if (!idToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $idToken")
                }
            }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                when {
                    response.code == 401 -> ChatResult(
                        text = "",
                        sessionId = "",
                        error = "Sessão expirada. Sai e entra de novo.",
                    )
                    response.code == 429 -> ChatResult(
                        text = "",
                        sessionId = "",
                        error = json?.optString("error")?.takeIf { it.isNotBlank() }
                            ?: "Limite de uso atingido no servidor.",
                    )
                    json == null -> ChatResult(
                        text = "",
                        sessionId = "",
                        error = if (raw.isNotBlank()) raw.take(280) else "HTTP ${response.code}",
                    )
                    json.optBoolean("ok", false) -> ChatResult(
                        text = json.optString("text").trim(),
                        sessionId = json.optString("sessionId"),
                        idempotent = json.optBoolean("idempotent", false),
                    )
                    else -> ChatResult(
                        text = "",
                        sessionId = "",
                        error = json.optString("error").ifBlank {
                            "Erro HTTP ${response.code} no servidor Luna."
                        },
                    )
                }
            }
        } catch (e: IOException) {
            ChatResult(
                text = "",
                sessionId = "",
                error = e.message?.takeIf { it.isNotBlank() }
                    ?: "Falha de rede com o servidor Luna.",
            )
        }
    }
}
