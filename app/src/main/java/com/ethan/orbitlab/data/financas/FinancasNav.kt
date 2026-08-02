package com.ethan.orbitlab.data.financas

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Destinos de aba Finanças (nomes = [OrbitTab].name no shell).
 */
object FinancasDestino {
    const val EXTRATO = "EXTRATO"
    const val CAPTURA = "CAPTURA"
    const val CARTOES = "CARTOES"
    const val RECORRENTES = "RECORRENTES"
    const val TRANSFERENCIA = "TRANSFERENCIA"
    const val METAS = "METAS"
    const val CONSTANCIA = "CONSTANCIA"
    const val FINANCAS = "FINANCAS"
}

/**
 * Ponte leve entre telas Finanças → troca de aba no [OrbitShell].
 */
object FinancasNav {
    private val _destino = MutableStateFlow<String?>(null)
    private val _navegarTick = MutableStateFlow(0L)
    val navegarTick: StateFlow<Long> = _navegarTick.asStateFlow()

    fun abrir(destino: String) {
        _destino.value = destino
        _navegarTick.value = System.currentTimeMillis()
    }

    fun consumir(): String? {
        val d = _destino.value
        _destino.value = null
        return d
    }
}
