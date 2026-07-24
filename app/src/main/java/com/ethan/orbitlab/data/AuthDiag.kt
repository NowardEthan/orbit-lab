package com.ethan.orbitlab.data

import android.content.Context
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
     * Já sabemos que o cofre `[DEFAULT]+<appId>` está CHEIO e o SDK não o relê ao abrir.
     * Falta ver O QUE tem dentro: os NOMES das chaves (sem valores — é conta do dono) dizem
     * se o usuário guardado (FIREBASE_USER) está no arquivo padrão ou perdido em outro. Se
     * o FIREBASE_USER estiver no arquivo certo e mesmo assim não voltar, o defeito é a
     * leitura/parse do SDK, não a gravação — e o conserto é outro.
     */
    fun radiografarCofre(context: Context, quando: String) {
        val achados = runCatching {
            java.io.File(context.applicationInfo.dataDir, "shared_prefs")
                .listFiles()
                ?.filter { it.name.startsWith("com.google.firebase.auth") }
                ?.sortedBy { it.name }
                ?.map { arquivo ->
                    val nome = arquivo.name.removeSuffix(".xml")
                    val chaveArq = nome.removePrefix("com.google.firebase.auth.api.Store.")
                    val prefs = runCatching {
                        context.getSharedPreferences(nome, Context.MODE_PRIVATE).all.keys
                    }.getOrDefault(emptySet())
                    // Só o rótulo final de cada chave (FIREBASE_USER, GET_TOKEN_RESPONSE…),
                    // nunca o valor — o valor é a conta dele.
                    val rotulos = prefs
                        .map { it.substringAfterLast('.').take(22) }
                        .sorted()
                        .joinToString(",")
                        .ifBlank { "vazio" }
                    val etiqueta =
                        if (chaveArq.length > 16) "${chaveArq.take(6)}…${chaveArq.takeLast(8)}" else chaveArq
                    "$etiqueta ${arquivo.length()}b [$rotulos]"
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

}
