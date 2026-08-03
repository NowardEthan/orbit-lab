package com.ethan.orbitlab.data.billing

import com.ethan.orbitlab.data.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * A carteira do lado do app: lê `/v1/billing/usage` e mantém um retrato vivo pra
 * o medidor e a parede. Escuta o Firestore (`users/{uid}/usage/` + o doc do
 * usuário pro plano) só pra saber QUANDO recarregar — a conta em si é do servidor.
 *
 * `object` no estilo de [AuthRepository]/PrefsRepository: uma instância, viva
 * enquanto o app existir.
 */
object UsageRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _usage = MutableStateFlow(UsageSnapshot.CARREGANDO)
    val usage: StateFlow<UsageSnapshot> = _usage.asStateFlow()

    /** Parede real: acende quando um turno volta 429 `quota_exceeded`; cai no refresh com saldo. */
    private val _bloqueado = MutableStateFlow(false)
    val bloqueado: StateFlow<Boolean> = _bloqueado.asStateFlow()
    private var bloqueioResetMs: Long? = null

    private var uidAtual: String? = null
    private val listeners = mutableListOf<ListenerRegistration>()
    private var refreshJob: Job? = null

    /** Liga a carteira à sessão. Chamar quando o uid muda (login/logout). */
    fun observar(uid: String?) {
        if (uid == uidAtual) return
        uidAtual = uid
        limparListeners()
        if (uid.isNullOrBlank()) {
            _usage.value = UsageSnapshot.CARREGANDO
            _bloqueado.value = false
            return
        }
        _usage.value = UsageSnapshot.CARREGANDO
        refresh()

        val db = FirebaseFirestore.getInstance()
        val usageCol = db.collection("users").document(uid).collection("usage")
        val recarregar = { -> refresh() }
        listeners += usageCol.document("_free_window").addSnapshotListener { _, _ -> recarregar() }
        listeners += usageCol.document("_weekly").addSnapshotListener { _, _ -> recarregar() }
        // O plano vive no doc do usuário — mudou (assinou/caiu), recarrega.
        listeners += db.collection("users").document(uid).addSnapshotListener { _, _ -> recarregar() }
    }

    /** Recarrega a carteira do servidor (fonte da verdade). */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val token = AuthRepository.getIdToken() ?: return@launch
            val snap = LunaBillingApi.buscarUso(token) ?: return@launch
            _usage.value = snap
            if (snap.temSaldoParaChat) {
                _bloqueado.value = false
                bloqueioResetMs = null
            }
        }
    }

    /**
     * Desconto otimista — o medidor reage no instante do envio, sem esperar o
     * servidor. O refresh seguinte reconcilia com a verdade.
     */
    fun descontar(custo: Long) {
        if (custo < 1) return
        val u = _usage.value
        if (u.ilimitado || u.loading) return
        val rem = u.remainingTokens ?: return
        _usage.value = u.copy(
            usedTokens = u.usedTokens + custo,
            remainingTokens = max(0L, rem - custo),
        )
    }

    /** Turno recusado por cota (HTTP 429). Acende a parede até o próximo refresh com saldo. */
    fun marcarBloqueado(resetsAtMs: Long?) {
        if (resetsAtMs != null) bloqueioResetMs = resetsAtMs
        _bloqueado.value = true
        val u = _usage.value
        if (!u.ilimitado) {
            _usage.value = u.copy(remainingTokens = 0L, resetsAtMs = resetsAtMs ?: u.resetsAtMs)
        }
    }

    private fun limparListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }
}
