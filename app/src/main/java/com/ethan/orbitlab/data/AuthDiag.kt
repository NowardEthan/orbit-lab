package com.ethan.orbitlab.data

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Diário de bordo da sessão — por que a tela de login apareceu.
 *
 * A sessão some no aparelho dele e não dá pra ler o logcat daqui. Então o app anota o que
 * aconteceu na abertura (achou a conta? demorou? a reentrada falhou por quê?) e mostra o
 * resumo na própria tela de login. O de ANTES fica guardado: o interessante costuma ter
 * acontecido na abertura passada, não nesta.
 */
object AuthDiag {
    private const val LIMITE = 12

    private val t0 = SystemClock.elapsedRealtime()
    private val linhas = mutableListOf<String>()

    private val _agora = MutableStateFlow("")
    val agora: StateFlow<String> = _agora.asStateFlow()

    /**
     * O que ficou registrado na abertura anterior.
     *
     * Lido UMA vez, quando o objeto nasce (antes da primeira anotação sobrescrever o disco).
     */
    private val anteriorGuardado: String = PrefsRepository.authDiag.orEmpty()
    val anterior: String get() = anteriorGuardado

    fun anota(msg: String) {
        val s = (SystemClock.elapsedRealtime() - t0) / 1000.0
        synchronized(linhas) {
            linhas += String.format(java.util.Locale.US, "+%.1fs %s", s, msg)
            if (linhas.size > LIMITE) linhas.removeAt(0)
            _agora.value = linhas.joinToString("\n")
        }
        PrefsRepository.authDiag = _agora.value
    }
}
