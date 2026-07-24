package com.ethan.orbitlab.data

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Diário de bordo da sessão — por que a tela de login apareceu.
 *
 * A sessão some no aparelho dele e não dá pra ler o logcat daqui. Então o app anota o que
 * aconteceu na abertura (achou a conta? demorou? a reentrada falhou por quê?) e mostra o
 * resumo na própria tela de login. O de ANTES fica guardado: o interessante costuma ter
 * acontecido na abertura passada, não nesta.
 */
object AuthDiag {
    private const val LIMITE = 16

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

    /**
     * Radiografia do cofre do Firebase Auth em disco.
     *
     * A conta guardada deveria viver num arquivo de preferências chamado
     * `com.google.firebase.auth.api.Store.<chave>`, onde a chave sai do nome do app + o id
     * dele. O diário já provou que o SDK nunca devolve essa conta; falta saber por quê, e só
     * há três respostas possíveis: o arquivo não existe (ninguém escreveu), existe vazio
     * (alguém limpou), ou existe cheio e o SDK procura por outra chave (duas chaves = dois
     * cofres, e ele abre o errado). Uma linha por abertura resolve qual das três é.
     */
    fun radiografarCofre(context: Context, quando: String) {
        val achados = runCatching {
            java.io.File(context.applicationInfo.dataDir, "shared_prefs")
                .listFiles()
                ?.filter { it.name.startsWith("com.google.firebase.auth") }
                ?.sortedBy { it.name }
                ?.map { arquivo ->
                    val nome = arquivo.name.removeSuffix(".xml")
                    val chave = nome.removePrefix("com.google.firebase.auth.api.Store.")
                    val quantas = runCatching {
                        context.getSharedPreferences(nome, Context.MODE_PRIVATE).all.size
                    }.getOrDefault(-1)
                    val etiqueta =
                        if (chave.length > 20) "${chave.take(6)}…${chave.takeLast(10)}" else chave
                    "$etiqueta ${arquivo.length()}b/${quantas}ch"
                }
                .orEmpty()
        }.getOrElse { listOf("não deu pra ler (${it.javaClass.simpleName})") }

        anota(
            "cofre $quando: " + if (achados.isEmpty()) {
                "NENHUM arquivo do Auth em disco"
            } else {
                achados.joinToString(" · ")
            },
        )
    }

    private const val TIQUE_MS = 200L
    private const val VIGIA_MS = 15_000L

    /**
     * Vigia da thread principal durante a abertura.
     *
     * A conta guardada demora segundos pra chegar, e só há duas explicações: o Firebase está
     * conversando com a rede, ou a thread principal está entupida e o aviso dele espera na
     * fila (o callback do listener é entregue nela). Um tique a cada 200ms separa as duas —
     * atraso grande no tique é culpa nossa; tique liso é o Firebase mesmo.
     */
    fun vigiarAbertura() {
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            var pior = 0L
            var quandoPior = 0.0
            var anterior = SystemClock.elapsedRealtime()
            val fim = anterior + VIGIA_MS
            while (SystemClock.elapsedRealtime() < fim) {
                delay(TIQUE_MS)
                val agora = SystemClock.elapsedRealtime()
                val atraso = agora - anterior - TIQUE_MS
                if (atraso > pior) {
                    pior = atraso
                    quandoPior = (agora - t0) / 1000.0
                }
                anterior = agora
            }
            anota(
                String.format(
                    java.util.Locale.US,
                    "thread principal: maior travada %dms (aos +%.1fs)",
                    pior,
                    quandoPior,
                ),
            )
        }
    }
}
