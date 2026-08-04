package com.ethan.orbitlab.ui.bolha

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sinais da bolha (B3) — badge / pensando / alerta de cota.
 * Sem texto de mensagem (privacidade).
 */
object BolhaSinal {
    private val _badge = MutableStateFlow(false)
    val badge: StateFlow<Boolean> = _badge.asStateFlow()

    private val _pensando = MutableStateFlow(false)
    val pensando: StateFlow<Boolean> = _pensando.asStateFlow()

    private val _alertaCota = MutableStateFlow(false)
    val alertaCota: StateFlow<Boolean> = _alertaCota.asStateFlow()

    fun setBadge(v: Boolean) {
        if (v && !_badge.value) {
            runCatching {
                com.ethan.orbitlab.data.crash.CrashReporting.breadcrumb("bolha_badge_show")
            }
        }
        _badge.value = v
    }

    fun limparBadge() {
        _badge.value = false
    }

    fun setPensando(v: Boolean) {
        _pensando.value = v
    }

    fun setAlertaCota(v: Boolean) {
        _alertaCota.value = v
    }
}
