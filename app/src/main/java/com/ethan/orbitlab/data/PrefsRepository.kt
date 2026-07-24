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
    private const val KEY_VIBRACAO = "orbit.lab.vibracao"
    private const val KEY_SESSAO_UID = "orbit.lab.sessao.uid"

    private lateinit var prefs: SharedPreferences

    /** Raciocínio SEMPRE visível — não é mais opção do usuário. */
    val reasoningEnabled: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    /**
     * Chat SEMPRE pelo servidor Luna (Railway/core) — a Luna de verdade, com memória e
     * ferramentas. O OpenRouter direto foi aposentado (era uma Luna oca e dava pra esquecer ligada).
     */
    val lunaDirectEnabled: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    private val _vibracao = MutableStateFlow(true)
    val vibracao: StateFlow<Boolean> = _vibracao.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _vibracao.value = prefs.getBoolean(KEY_VIBRACAO, true)
    }

    /**
     * Uid da última conta que entrou — marcador de «já tem sessão neste aparelho».
     * Serve pra segurar a tela de login enquanto o Firebase lê a conta do disco: num
     * celular apertado de memória o app é recriado toda hora, e sem isto ele piscava
     * o login (e pedia a conta Google de novo) só porque o Firebase ainda não respondera.
     */
    var sessaoUid: String?
        get() = if (::prefs.isInitialized) prefs.getString(KEY_SESSAO_UID, null) else null
        set(value) {
            if (!::prefs.isInitialized) return
            val ed = prefs.edit()
            if (value.isNullOrBlank()) ed.remove(KEY_SESSAO_UID) else ed.putString(KEY_SESSAO_UID, value)
            ed.apply()
        }

    fun setVibracao(enabled: Boolean) {
        _vibracao.value = enabled
        prefs.edit().putBoolean(KEY_VIBRACAO, enabled).apply()
    }

    /** Limpa prefs locais (após «apagar dados»). */
    fun reset() {
        prefs.edit().clear().apply()
        _vibracao.value = true
    }
}
