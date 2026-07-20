package com.ethan.orbitlab.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferências locais do lab — espelho leve do AsyncStorage do orbit-mobile.
 */
object PrefsRepository {
    private const val PREFS = "orbitlab_prefs"
    private const val KEY_REASONING = "orbit.luna.reasoning.enabled"
    private const val KEY_VIBRACAO = "orbit.lab.vibracao"
    private const val KEY_LUNA_DIRECT = "orbit.luna.chat.direct"

    private lateinit var prefs: SharedPreferences

    private val _reasoningEnabled = MutableStateFlow(true)
    val reasoningEnabled: StateFlow<Boolean> = _reasoningEnabled.asStateFlow()

    private val _vibracao = MutableStateFlow(true)
    val vibracao: StateFlow<Boolean> = _vibracao.asStateFlow()

    /** true = OpenRouter direto no lab; false = mobile-api no Railway (agentico). */
    private val _lunaDirectEnabled = MutableStateFlow(true)
    val lunaDirectEnabled: StateFlow<Boolean> = _lunaDirectEnabled.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _reasoningEnabled.value = prefs.getBoolean(KEY_REASONING, true)
        _vibracao.value = prefs.getBoolean(KEY_VIBRACAO, true)
        _lunaDirectEnabled.value = prefs.getBoolean(KEY_LUNA_DIRECT, true)
    }

    fun setReasoningEnabled(enabled: Boolean) {
        _reasoningEnabled.value = enabled
        prefs.edit().putBoolean(KEY_REASONING, enabled).apply()
    }

    fun setVibracao(enabled: Boolean) {
        _vibracao.value = enabled
        prefs.edit().putBoolean(KEY_VIBRACAO, enabled).apply()
    }

    fun setLunaDirectEnabled(enabled: Boolean) {
        _lunaDirectEnabled.value = enabled
        prefs.edit().putBoolean(KEY_LUNA_DIRECT, enabled).apply()
    }

    /** Limpa prefs locais (após «apagar dados»). */
    fun reset() {
        prefs.edit().clear().apply()
        _reasoningEnabled.value = true
        _vibracao.value = true
        _lunaDirectEnabled.value = true
    }
}
