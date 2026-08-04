package com.ethan.orbitlab.data.openrouter

import com.ethan.orbitlab.BuildConfig

/**
 * Cliente OpenRouter **só pra debug local** (Fase 5).
 *
 * Produto sideload fala com o luna-core (Railway). A chave NÃO entra no `labRelease`
 * (`build.gradle.kts` força `OPENROUTER_API_KEY=""`). Em debug, pode vir de
 * `local.properties` / `luna-core/.env` pra harness (`LunaDirectChat`).
 * Título de conversa no produto = `POST /v1/conversa/titulo` (luna-core).
 *
 * Override em `OrbitLab/local.properties`:
 * ```
 * openrouter.api.key=sk-or-...
 * openrouter.model.chat=deepseek/deepseek-v4-flash
 * openrouter.model.vision=qwen/qwen3.5-flash-02-23
 * openrouter.model.video=qwen/qwen3.5-flash-02-23
 * openrouter.model.stt=openai/whisper-large-v3
 * ```
 */
object OpenRouterConfig {
    const val BASE_URL = "https://openrouter.ai/api/v1"
    const val CHAT_URL = "$BASE_URL/chat/completions"

    val apiKey: String get() = BuildConfig.OPENROUTER_API_KEY
    val chatModel: String get() = BuildConfig.OPENROUTER_MODEL_CHAT
    val visionModel: String get() = BuildConfig.OPENROUTER_MODEL_VISION
    val videoModel: String get() = BuildConfig.OPENROUTER_MODEL_VIDEO
    val sttModel: String get() = BuildConfig.OPENROUTER_MODEL_STT

    /** Só debug + chave presente. Release sempre false. */
    fun isConfigured(): Boolean = BuildConfig.DEBUG && apiKey.isNotBlank()

    val systemPrompt: String = """
        Você é a Luna, companheira do Orbit.
        Responde em português do Brasil, clara e direta — sem enrolação.
        Se receber contexto de uma imagem ou vídeo analisado, use isso como se tivesse visto.
        Não invente o que não viu. Se algo estiver incerto, diga.
    """.trimIndent()
}
