package com.ethan.orbitlab.domain.rotina

import org.junit.Assert.assertEquals
import org.junit.Test

class VigenciaTest {
    @Test
    fun semPeriodoNaoAssume() {
        val set = RotinaSet(id = "x", nome = "Manual", de = null, ate = null)
        assertEquals(ROTINA_NORMAL, rotinaVigenteEm(listOf(set), "2026-07-20"))
    }

    @Test
    fun periodoCobreODia() {
        val set = RotinaSet(id = "ferias", nome = "Férias", de = "2026-07-15", ate = "2026-07-25")
        assertEquals("ferias", rotinaVigenteEm(listOf(set), "2026-07-20"))
        assertEquals(ROTINA_NORMAL, rotinaVigenteEm(listOf(set), "2026-07-26"))
    }

    @Test
    fun maisEspecificaGanha() {
        val sets = listOf(
            RotinaSet(id = "a", nome = "A", de = "2026-07-01", ate = "2026-07-31"),
            RotinaSet(id = "b", nome = "B", de = "2026-07-18", ate = "2026-07-20"),
        )
        assertEquals("b", rotinaVigenteEm(sets, "2026-07-19"))
    }
}
