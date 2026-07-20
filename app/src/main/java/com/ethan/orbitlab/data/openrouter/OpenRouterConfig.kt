package com.ethan.orbitlab.data.openrouter

import com.ethan.orbitlab.BuildConfig

/**
 * Modelos OpenRouter usados no lab — direto no provedor, sem luna-core / agentico.
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

    fun isConfigured(): Boolean = apiKey.isNotBlank()

    val systemPrompt: String = """
        Você é a Luna, companheira do Orbit.
        Responde em português do Brasil, clara e directa — sem enrolação.
        Se receber contexto de uma imagem ou vídeo analisado, use isso como se tivesse visto.
        Não invente o que não viu. Se algo estiver incerto, diga.
    """.trimIndent()
}
