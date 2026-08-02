package com.ethan.orbitlab.data.financas

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ponte leve Cartões → tela Transferência (pré-preenche "pagar fatura").
 * O [com.ethan.orbitlab.shell.OrbitShell] observa [navegarTick] pra trocar de aba.
 */
object TransferenciaLauncher {
    data class Prefill(
        val deCarteiraId: String? = null,
        val paraCarteiraId: String? = null,
        val motivo: String? = null,
        val valorCentavos: Long? = null,
    )

    private val _prefill = MutableStateFlow<Prefill?>(null)
    private val _navegarTick = MutableStateFlow(0L)
    val navegarTick: StateFlow<Long> = _navegarTick.asStateFlow()

    fun abrir(prefill: Prefill = Prefill()) {
        _prefill.value = prefill
        _navegarTick.value = System.currentTimeMillis()
    }

    fun consumirPrefill(): Prefill? {
        val p = _prefill.value
        _prefill.value = null
        return p
    }
}
