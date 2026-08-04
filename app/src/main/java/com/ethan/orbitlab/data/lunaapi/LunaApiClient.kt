package com.ethan.orbitlab.data.lunaapi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
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
        /** Código legível do erro (ex.: "quota_exceeded") — deixa o cliente distinguir cota de erro seco. */
        val errorCode: String? = null,
        /** Quando a carteira renova (ms epoch), no caso de `quota_exceeded`. */
        val quotaResetsAtMs: Long? = null,
        /** ms até o 1º `content` delta (só no stream). */
        val ttfbMs: Long? = null,
        /** fase → ms desde o início (só no stream). */
        val phasesMs: Map<String, Long> = emptyMap(),
    ) {
        /** Turno recusado por cota (o servidor manda `code: "quota_exceeded"` num 429). */
        val cotaEsgotada: Boolean get() = errorCode == "quota_exceeded"
        /** Anti-rajada no servidor (`rate_limited`) — NÃO é parede de cota. */
        val rateLimited: Boolean get() = errorCode == "rate_limited"
    }

    private fun mensagem429(json: JSONObject?, fallbackGenerico: String): String {
        val code = json?.optString("code")?.takeIf { it.isNotBlank() }
        val doServidor = json?.optString("error")?.takeIf { it.isNotBlank() }
        if (doServidor != null) return doServidor
        return when (code) {
            "rate_limited" ->
                "Calma — tô recebendo rápido demais agora. Espera uns segundos e manda de novo."
            "quota_exceeded" ->
                "Você chegou no limite do plano por agora."
            else -> fallbackGenerico
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) optLong(key) else null

    /** Uma mensagem enxuta pra mandar ao buscador semântico. */
    data class MensagemBusca(val id: String, val papel: String, val texto: String)

    /** Um acerto da busca — id da mensagem pra rolar até lá + o trecho citado. */
    data class BuscaItem(val messageId: String, val papel: String, val trecho: String)

    data class BuscaResposta(val resultados: List<BuscaItem>, val error: String?)

    /** Mesmos limites do servidor — cortar aqui evita subir um payload gigante por nada. */
    private const val BUSCA_MAX_MSGS = 120
    private const val BUSCA_MAX_CHARS = 500

    /** Rede tropeçou (DNS/timeout/socket morto/5xx)? Vale re-tentar, igual o chat faz. */
    private fun buscaTransitoria(msg: String?): Boolean {
        if (msg == null) return true
        val m = msg.lowercase()
        return listOf(
            "unable to resolve host", "failed to connect", "timeout", "timed out",
            "econnreset", "connection reset", "connection closed", "connection abort",
            "unexpected end", "network", "falha de rede", "http 5",
            " 500", " 502", " 503", " 504",
        ).any { m.contains(it) }
    }

    /**
     * POST /v1/conversa/buscar — busca SEMÂNTICA na conversa (o «pergunta à Luna» da busca).
     * O servidor lê as mensagens e devolve os pontos que casam pelo SIGNIFICADO, com o id
     * validado contra as mensagens reais (não deixa inventar id).
     *
     * Teimosa como o chat: um socket keep-alive morto derruba a 1ª tentativa e, sem re-tentar,
     * o erro cru do OkHttp (que traz o endereço do servidor no texto) chegava na tela.
     */
    suspend fun buscarConversa(
        idToken: String?,
        query: String,
        mensagens: List<MensagemBusca>,
    ): BuscaResposta {
        val enxutas = mensagens.takeLast(BUSCA_MAX_MSGS).map { m ->
            if (m.texto.length <= BUSCA_MAX_CHARS) m else m.copy(texto = m.texto.take(BUSCA_MAX_CHARS))
        }
        var ultimo = BuscaResposta(emptyList(), "Não consegui buscar agora.")
        var tentativa = 0
        while (true) {
            tentativa++
            val r = buscarUmaVez(idToken, query, enxutas)
            if (r.error == null) return r
            ultimo = r
            if (tentativa >= 3 || !buscaTransitoria(r.error)) break
            evictConnections()
            delay(tentativa * 900L)
        }
        // Erro de rede vira recado humano — o endereço do servidor não é problema dele.
        return if (buscaTransitoria(ultimo.error)) {
            BuscaResposta(emptyList(), "A conexão tropeçou. Tenta de novo?")
        } else {
            ultimo
        }
    }

    private suspend fun buscarUmaVez(
        idToken: String?,
        query: String,
        mensagens: List<MensagemBusca>,
    ): BuscaResposta = withContext(Dispatchers.IO) {
        if (!LunaApiConfig.isConfigured()) {
            return@withContext BuscaResposta(emptyList(), "Busca indisponível neste build.")
        }

        val arr = JSONArray()
        mensagens.forEach { m ->
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("papel", m.papel)
                    .put("texto", m.texto),
            )
        }
        val body = JSONObject().put("query", query).put("mensagens", arr)

        val request = Request.Builder()
            .url(LunaApiConfig.buscarUrl)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .apply {
                if (!idToken.isNullOrBlank()) header("Authorization", "Bearer $idToken")
            }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                    ?: return@use BuscaResposta(emptyList(), "HTTP ${response.code} na busca.")
                if (json.optBoolean("ok", false)) {
                    val out = mutableListOf<BuscaItem>()
                    val rs = json.optJSONArray("resultados")
                    if (rs != null) {
                        for (i in 0 until rs.length()) {
                            val o = rs.optJSONObject(i) ?: continue
                            val id = o.optString("message_id")
                            if (id.isBlank()) continue
                            out.add(
                                BuscaItem(
                                    messageId = id,
                                    papel = o.optString("papel"),
                                    trecho = o.optString("trecho"),
                                ),
                            )
                        }
                    }
                    BuscaResposta(out, null)
                } else {
                    BuscaResposta(
                        emptyList(),
                        json.optString("error").ifBlank { "Não consegui buscar agora." },
                    )
                }
            }
        } catch (e: IOException) {
            BuscaResposta(
                emptyList(),
                e.message?.takeIf { it.isNotBlank() } ?: "Falha de rede na busca.",
            )
        }
    }

    /**
     * POST /v1/conversa/titulo — batiza a conversa no servidor (Groq menor).
     * Devolve null se falhar; o cliente mantém o título atual.
     */
    suspend fun gerarTitulo(
        idToken: String?,
        mensagens: List<MensagemBusca>,
    ): String? = withContext(Dispatchers.IO) {
        if (!LunaApiConfig.isConfigured()) return@withContext null
        val enxutas = mensagens.takeLast(10).map { m ->
            if (m.texto.length <= 280) m else m.copy(texto = m.texto.take(280))
        }
        if (enxutas.isEmpty()) return@withContext null

        val arr = JSONArray()
        enxutas.forEach { m ->
            val papel = when (m.papel.lowercase()) {
                "luna", "assistant" -> "luna"
                else -> "user"
            }
            arr.put(JSONObject().put("papel", papel).put("texto", m.texto))
        }
        val body = JSONObject().put("mensagens", arr)
        val request = Request.Builder()
            .url(LunaApiConfig.tituloUrl)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .apply {
                if (!idToken.isNullOrBlank()) header("Authorization", "Bearer $idToken")
            }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull() ?: return@withContext null
                if (!json.optBoolean("ok", false)) return@withContext null
                json.optString("title").trim().takeIf { it.isNotBlank() }
            }
        } catch (_: IOException) {
            null
        }
    }

    sealed class StreamEvent {
        data class Status(val phase: String, val label: String? = null) : StreamEvent()
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
                        error = mensagem429(json, "Limite de uso atingido no servidor."),
                        errorCode = json?.optString("code")?.takeIf { it.isNotBlank() },
                        quotaResetsAtMs = json?.optLongOrNull("resetsAtMs"),
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
                                val label = json.optString("label").trim()
                                if (phase.isNotEmpty()) {
                                    phaseMarks.putIfAbsent(phase, elapsed)
                                    onEvent(StreamEvent.Status(phase, label.ifEmpty { null }))
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
                        // O pré-cheque de cota recusa ANTES do SSE abrir → cai aqui como 429 com corpo JSON.
                        val bodyJson = runCatching { JSONObject(bodyPreview) }.getOrNull()
                        val msg = when (httpCode) {
                            401 -> "Sessão expirada. Sai e entra de novo."
                            429 -> mensagem429(bodyJson, "Limite de uso atingido no servidor.")
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
                                errorCode = bodyJson?.optString("code")?.takeIf { it.isNotBlank() },
                                quotaResetsAtMs = bodyJson?.optLongOrNull("resetsAtMs"),
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

    sealed class TranscribeResult {
        data class Ok(val text: String) : TranscribeResult()
        data class Erro(val mensagem: String) : TranscribeResult()
    }

    /**
     * POST /v1/transcribe — Whisper no servidor (chave nunca no APK).
     * Mesmo contrato do orbit-mobile (`lunaTranscribe`).
     */
    suspend fun transcribe(
        idToken: String?,
        audioBase64: String,
        mimeType: String = "audio/mp4",
        language: String = "pt",
    ): TranscribeResult = withContext(Dispatchers.IO) {
        if (!LunaApiConfig.isConfigured()) {
            return@withContext TranscribeResult.Erro("Transcrição indisponível neste build.")
        }
        val body = JSONObject()
            .put("audioBase64", audioBase64)
            .put("mimeType", mimeType)
            .put("language", language)
        val request = Request.Builder()
            .url(LunaApiConfig.transcribeUrl)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .apply {
                if (!idToken.isNullOrBlank()) header("Authorization", "Bearer $idToken")
            }
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        try {
            http.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                when {
                    response.code == 401 -> TranscribeResult.Erro(
                        "Sessão expirada. Sai e entra de novo.",
                    )
                    response.code == 429 -> TranscribeResult.Erro(
                        mensagem429(json, "Limite de uso atingido. Tenta de novo depois."),
                    )
                    json == null -> TranscribeResult.Erro(
                        if (raw.isNotBlank()) raw.take(200) else "HTTP ${response.code}",
                    )
                    json.optBoolean("ok", false) -> {
                        val text = json.optString("text").trim()
                        if (text.isBlank()) {
                            TranscribeResult.Erro("Não detectamos fala neste áudio.")
                        } else {
                            TranscribeResult.Ok(text)
                        }
                    }
                    else -> TranscribeResult.Erro(
                        json.optString("error").ifBlank {
                            "Não consegui transcrever o áudio."
                        },
                    )
                }
            }
        } catch (e: IOException) {
            TranscribeResult.Erro(
                e.message?.takeIf { it.isNotBlank() }
                    ?: "Falha de rede na transcrição.",
            )
        }
    }
}
