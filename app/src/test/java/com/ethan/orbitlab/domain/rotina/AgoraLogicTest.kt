package com.ethan.orbitlab.domain.rotina

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgoraLogicTest {

    private fun bloco(
        id: String,
        inicio: Int,
        fim: Int,
        dias: List<Int> = listOf(1),
    ) = BlocoRotina(
        id = id,
        titulo = id,
        dias = dias,
        inicio = inicio,
        fim = fim,
        cor = "#4B75F2",
    )

    @Test
    fun detectaBlocoAtual() {
        val blocos = listOf(bloco("a", 540, 600), bloco("b", 720, 780))
        val e = estadoAgora(blocos, dia = 1, minutoAtual = 550)
        assertEquals("a", e.atual?.id)
        assertEquals(50, e.faltamMinutos)
    }

    @Test
    fun detectaProximoELivre() {
        val blocos = listOf(bloco("a", 540, 600), bloco("b", 720, 780))
        val e = estadoAgora(blocos, dia = 1, minutoAtual = 610)
        assertNull(e.atual)
        assertEquals("b", e.proximo?.id)
        assertEquals(110, e.livreMinutos)
    }

    @Test
    fun conflitosQuandoSobrepostos() {
        val blocos = listOf(bloco("a", 540, 700), bloco("b", 660, 780))
        val c = conflitos(blocos, 1)
        assertEquals(1, c.size)
        assertTrue(c[0].first.id == "a" && c[0].second.id == "b")
    }

    @Test
    fun buracosNaLista() {
        val doDia = listOf(bloco("a", 540, 600), bloco("b", 720, 780))
        val itens = itensComBuracos(doDia)
        assertEquals(3, itens.size)
        assertTrue(itens[1] is ItemListaRotina.Vazio)
        assertEquals(120, (itens[1] as ItemListaRotina.Vazio).minutos)
    }
}
