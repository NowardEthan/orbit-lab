package com.ethan.orbitlab.data.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ponte leve pra abrir a tela de Planos de qualquer canto (parede do chat,
 * medidor, perfil) → o [OrbitShell] observa e troca a aba, fechando overlays.
 * Espelha o padrão de [com.ethan.orbitlab.data.financas.FinancasNav].
 */
object PlanosNav {
    private val _tick = MutableStateFlow(0L)
    val tick: StateFlow<Long> = _tick.asStateFlow()

    fun abrir() {
        _tick.value = System.currentTimeMillis()
    }
}
