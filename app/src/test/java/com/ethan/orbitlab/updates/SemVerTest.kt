package com.ethan.orbitlab.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {
    @Test
    fun comparaVersoesSimples() {
        assertEquals(-1, compareVersions("2.24.4", "2.25.0"))
        assertEquals(0, compareVersions("2.25.0", "2.25.0"))
        assertEquals(1, compareVersions("2.25.0", "2.24.4"))
    }

    @Test
    fun isNewerDetectaAtualizacao() {
        assertTrue(isNewer("2.25.0", "2.24.4"))
        assertFalse(isNewer("2.24.4", "2.25.0"))
        assertFalse(isNewer("2.25.0", "2.25.0"))
    }

    @Test
    fun ignoraSufixoNaoNumericoNoSegmento() {
        // "2.25.0-beta" → segmentos 2, 25, 0
        assertEquals(0, compareVersions("2.25.0-beta", "2.25.0"))
    }
}
