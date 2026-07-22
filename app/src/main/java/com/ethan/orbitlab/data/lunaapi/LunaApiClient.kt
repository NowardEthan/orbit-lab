package com.ethan.orbitlab.data.lunaapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Cliente da mobile-api — POST /v1/chat (JSON) e POST /v1/chat/stream (SSE).
 */
object LunaApiClient {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Descarta conexões ociosas do pool. Chamar antes de re-tentar após "software caused
     * connection abort": um socket keep-alive morto reusado aborta de novo — evictar força
     * uma conexão nova.
     */
    fun evictConnections() {
        runCatching { http.connectionPool.evictAll() }
    }

    data class ChatResult(
        val text: String,
        val sessionId: String,
        val reasoning: String = "",
        val idempotent: Boolean = false,
        val error: String? = null,
        /** ms até o 1º `content` delta (só no stream). */
        val ttfbMs: Long? = null,
        /** fase → ms desde o início (só no stream). */
        val phasesMs: Map<String, Long> = emptyMap(),
    )

    sealed class StreamEvent {
        data class Status(val phase: String) : StreamEvent()
        data class Reasoning(val delta: String) : StreamEvent()
        data class Content(val delta: String) : StreamEvent()
        data class Acao(val json: JSONObject) : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

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

    /**
     * POST /v1/chat/stream — eventos nomeados: status, reasoning, content, acao, error, done.
     * [onEvent] pode ser chamado na thread do OkHttp; o chamador deve marshalar pra UI.
     */
    suspend fun chatStream(
        idToken: String?,
        body: JSONObject,
        onEvent: (StreamEvent) -> Unit = {},
    ): ChatResult = withContext(Dispatchers.IO) {
        if (!LunaApiConfig.isConfigured()) {
            return@withContext ChatResult(
                text = "",
                sessionId = "",
                error = "LUNA_API_URL ausente no build.",
            )
        }

        val request = Request.Builder()
            .url(LunaApiConfig.streamUrl)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .apply {
                if (!idToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $idToken")
                }
            }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        suspendCancellableCoroutine { cont ->
            val finished = AtomicBoolean(false)
            val t0 = System.currentTimeMillis()
            val phaseMarks = linkedMapOf<String, Long>()
            var ttfbMs: Long? = null
            val contentBuf = StringBuilder()
            val reasoningBuf = StringBuilder()
            var sessionId = body.optString("sessionId", "")

            fun complete(result: ChatResult) {
                if (!finished.compareAndSet(false, true)) return
                if (cont.isActive) cont.resume(result)
            }

            val factory = EventSources.createFactory(http)
            val source = factory.newEventSource(
                request,
                object : EventSourceListener() {
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String,
                    ) {
                        val eventName = type?.takeIf { it.isNotBlank() } ?: "message"
                        val json = runCatching { JSONObject(data) }.getOrNull() ?: return
                        val elapsed = System.currentTimeMillis() - t0

                        when (eventName) {
                            "status" -> {
                                val phase = json.optString("phase").trim()
                                if (phase.isNotEmpty()) {
                                    phaseMarks.putIfAbsent(phase, elapsed)
                                    onEvent(StreamEvent.Status(phase))
                                }
                            }
                            "reasoning" -> {
                                val delta = json.optString("delta")
                                if (delta.isNotEmpty()) {
                                    reasoningBuf.append(delta)
                                    onEvent(StreamEvent.Reasoning(delta))
                                }
                            }
                            "content" -> {
                                val delta = json.optString("delta")
                                if (delta.isNotEmpty()) {
                                    if (ttfbMs == null) ttfbMs = elapsed
                                    contentBuf.append(delta)
                                    onEvent(StreamEvent.Content(delta))
                                }
                            }
                            "acao" -> onEvent(StreamEvent.Acao(json))
                            "error" -> {
                                val msg = json.optString("error").ifBlank { "Erro de streaming." }
                                onEvent(StreamEvent.Error(msg))
                                eventSource.cancel()
                                complete(
                                    ChatResult(
                                        text = contentBuf.toString(),
                                        sessionId = sessionId,
                                        reasoning = reasoningBuf.toString(),
                                        error = msg,
                                        ttfbMs = ttfbMs,
                                        phasesMs = phaseMarks.toMap(),
                                    ),
                                )
                            }
                            "done" -> {
                                val text = json.optString("text").ifBlank { contentBuf.toString() }
                                val reasoning = json.optString("reasoning").ifBlank {
                                    reasoningBuf.toString()
                                }
                                sessionId = json.optString("sessionId").ifBlank { sessionId }
                                eventSource.cancel()
                                complete(
                                    ChatResult(
                                        text = text.trim(),
                                        sessionId = sessionId,
                                        reasoning = reasoning,
                                        idempotent = json.optBoolean("idempotent", false),
                                        ttfbMs = ttfbMs,
                                        phasesMs = phaseMarks.toMap(),
                                    ),
                                )
                            }
                        }
                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: okhttp3.Response?,
                    ) {
                        val httpCode = response?.code
                        val bodyPreview = response?.body?.string()?.take(280).orEmpty()
                        val msg = when (httpCode) {
                            401 -> "Sessão expirada. Sai e entra de novo."
                            429 -> "Limite de uso atingido no servidor."
                            else -> t?.message?.takeIf { it.isNotBlank() }
                                ?: bodyPreview.takeIf { it.isNotBlank() }
                                ?: httpCode?.let { "HTTP $it no stream Luna." }
                                ?: "Falha de rede com o servidor Luna."
                        }
                        onEvent(StreamEvent.Error(msg))
                        complete(
                            ChatResult(
                                text = contentBuf.toString(),
                                sessionId = sessionId,
                                reasoning = reasoningBuf.toString(),
                                error = msg,
                                ttfbMs = ttfbMs,
                                phasesMs = phaseMarks.toMap(),
                            ),
                        )
                    }

                    override fun onClosed(eventSource: EventSource) {
                        if (finished.get()) return
                        // Stream fechou sem `done` — devolve o que acumulou.
                        complete(
                            ChatResult(
                                text = contentBuf.toString().trim(),
                                sessionId = sessionId,
                                reasoning = reasoningBuf.toString(),
                                error = if (contentBuf.isEmpty()) {
                                    "Stream fechou sem resposta."
                                } else {
                                    null
                                },
                                ttfbMs = ttfbMs,
                                phasesMs = phaseMarks.toMap(),
                            ),
                        )
                    }
                },
            )

            cont.invokeOnCancellation { source.cancel() }
        }
    }
}
