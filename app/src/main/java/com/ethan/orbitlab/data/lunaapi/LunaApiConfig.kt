package com.ethan.orbitlab.data.lunaapi

import com.ethan.orbitlab.BuildConfig

/** URL base da Luna Mobile API (Railway) — mesmo contrato do orbit-mobile. */
object LunaApiConfig {
    val baseUrl: String get() = BuildConfig.LUNA_API_URL.trimEnd('/')
    val chatUrl: String get() = "$baseUrl/v1/chat"
    val streamUrl: String get() = "$baseUrl/v1/chat/stream"
    val buscarUrl: String get() = "$baseUrl/v1/conversa/buscar"
    val healthUrl: String get() = "$baseUrl/health"

    fun isConfigured(): Boolean = baseUrl.isNotBlank()
}
