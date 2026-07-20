package com.ethan.orbitlab.data.openrouter

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Cliente OpenRouter mínimo — chat stream + visão/vídeo multimodal.
 * Sem pipeline agentico do luna-core.
 */
object OpenRouterClient {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    sealed class StreamEvent {
        data class Delta(val text: String) : StreamEvent()
        data class Reasoning(val text: String) : StreamEvent()
        data class Done(val fullText: String) : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    fun streamChat(
        messages: List<ChatMessage>,
        model: String = OpenRouterConfig.chatModel,
    ): Flow<StreamEvent> = callbackFlow {
        if (!OpenRouterConfig.isConfigured()) {
            trySend(StreamEvent.Error("OPENROUTER_API_KEY ausente. Coloca a chave no luna-core/.env ou local.properties."))
            close()
            return@callbackFlow
        }

        val body = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("temperature", 0.7)
            put("messages", messagesToJson(messages))
        }

        val request = Request.Builder()
            .url(OpenRouterConfig.CHAT_URL)
            .header("Authorization", "Bearer ${OpenRouterConfig.apiKey}")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://orbitlab.local")
            .header("X-Title", "OrbitLab")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val full = StringBuilder()
        val factory = EventSources.createFactory(http)
        val source = factory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                if (data == "[DONE]") {
                    trySend(StreamEvent.Done(full.toString()))
                    close()
                    return
                }
                runCatching {
                    val root = JSONObject(data)
                    val delta = root.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("delta")
                        ?: return@runCatching
                    val content = delta.optString("content", "")
                    if (content.isNotEmpty()) {
                        full.append(content)
                        trySend(StreamEvent.Delta(content))
                    }
                    val reasoning = delta.optString("reasoning", "")
                        .ifBlank { delta.optString("reasoning_content", "") }
                    if (reasoning.isNotEmpty()) {
                        trySend(StreamEvent.Reasoning(reasoning))
                    }
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: okhttp3.Response?,
            ) {
                val msg = t?.message
                    ?: response?.let { "HTTP ${it.code}" }
                    ?: "Falha no stream OpenRouter."
                trySend(StreamEvent.Error(msg))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                if (full.isNotEmpty()) {
                    trySend(StreamEvent.Done(full.toString()))
                }
                close()
            }
        })

        awaitClose { source.cancel() }
    }

    /**
     * Chat completo (sem SSE) — uma resposta JSON.
     */
    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = OpenRouterConfig.chatModel,
    ): String = withContext(Dispatchers.IO) {
        if (!OpenRouterConfig.isConfigured()) {
            error("OPENROUTER_API_KEY ausente. Coloca a chave no luna-core/.env ou local.properties.")
        }

        val body = JSONObject().apply {
            put("model", model)
            put("stream", false)
            put("temperature", 0.7)
            put("messages", messagesToJson(messages))
        }

        val request = Request.Builder()
            .url(OpenRouterConfig.CHAT_URL)
            .header("Authorization", "Bearer ${OpenRouterConfig.apiKey}")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://orbitlab.local")
            .header("X-Title", "OrbitLab")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("OpenRouter HTTP ${response.code}: ${raw.take(200)}")
            }
            val message = JSONObject(raw)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
            val content = message?.optString("content").orEmpty().trim()
            content.ifBlank { "…" }
        }
    }

    /**
     * Uma chamada (não-stream) multimodal — imagem ou vídeo via URL HTTPS ou data URI.
     */
    suspend fun analisarMidia(
        midiaUrl: String,
        mime: String,
        pergunta: String?,
        ehVideo: Boolean,
    ): String = withContext(Dispatchers.IO) {
        if (!OpenRouterConfig.isConfigured()) {
            error("OPENROUTER_API_KEY ausente.")
        }
        val model = if (ehVideo) OpenRouterConfig.videoModel else OpenRouterConfig.visionModel
        val instrucao = buildString {
            append(
                if (ehVideo) {
                    "Você é os olhos de quem não pode ver este vídeo. Descreva o que acontece, " +
                        "em português do Brasil, factual. Transcreva texto legível. Não invente."
                } else {
                    "Você é os olhos de quem não pode ver esta imagem. Descreva o que está nela, " +
                        "em português do Brasil, factual. Transcreva texto legível (OCR). Não invente."
                },
            )
            if (!pergunta.isNullOrBlank()) {
                append("\n\nPergunta específica: ").append(pergunta.trim())
            }
        }
        val midiaPart = if (ehVideo) {
            JSONObject().put("type", "video_url")
                .put("video_url", JSONObject().put("url", midiaUrl))
        } else {
            JSONObject().put("type", "image_url")
                .put("image_url", JSONObject().put("url", midiaUrl))
        }
        val userContent = JSONArray()
            .put(JSONObject().put("type", "text").put("text", instrucao))
            .put(midiaPart)

        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.2)
            put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", userContent),
                ),
            )
        }

        val request = Request.Builder()
            .url(OpenRouterConfig.CHAT_URL)
            .header("Authorization", "Bearer ${OpenRouterConfig.apiKey}")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://orbitlab.local")
            .header("X-Title", "OrbitLab")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Visão OpenRouter HTTP ${response.code}: ${raw.take(200)}")
            }
            val content = JSONObject(raw)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            content.trim().ifBlank { "Não consegui analisar a mídia." }
        }
    }

    /** Lê URI local (content:// / file://) → data URI base64 (imagens pequenas). */
    suspend fun uriToDataUri(context: Context, uri: Uri, mime: String): String =
        withContext(Dispatchers.IO) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Não deu pra ler o arquivo.")
            if (bytes.size > 12 * 1024 * 1024) {
                error("Arquivo grande demais pra mandar em base64 (>12 MB). Usa URL do Storage.")
            }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:${mime.ifBlank { "image/jpeg" }};base64,$b64"
        }

    private fun messagesToJson(messages: List<ChatMessage>): JSONArray {
        val arr = JSONArray()
        messages.forEach { msg ->
            arr.put(
                JSONObject()
                    .put("role", msg.role)
                    .put("content", msg.content),
            )
        }
        return arr
    }
}

data class ChatMessage(
    val role: String, // system | user | assistant
    val content: String,
)
