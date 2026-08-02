package com.ethan.orbitlab.data.financas

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado compartilhado de Finanças — um listener por coleção, várias telas consomem.
 * Liga/desliga com a sessão (chamado pelo [com.ethan.orbitlab.shell.OrbitShell]).
 */
object FinancasRepository {
    private const val TAG = "OrbitFinancas"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _carteiras = MutableStateFlow<List<Carteira>>(emptyList())
    val carteiras: StateFlow<List<Carteira>> = _carteiras.asStateFlow()

    private val _lancamentos = MutableStateFlow<List<Lancamento>>(emptyList())
    val lancamentos: StateFlow<List<Lancamento>> = _lancamentos.asStateFlow()

    private val _recorrentes = MutableStateFlow<List<Recorrente>>(emptyList())
    val recorrentes: StateFlow<List<Recorrente>> = _recorrentes.asStateFlow()

    private val _transferencias = MutableStateFlow<List<Transferencia>>(emptyList())
    val transferencias: StateFlow<List<Transferencia>> = _transferencias.asStateFlow()

    private val _metas = MutableStateFlow<List<MetaFinanceira>>(emptyList())
    val metas: StateFlow<List<MetaFinanceira>> = _metas.asStateFlow()

    private var regCarteiras: ListenerRegistration? = null
    private var regLancamentos: ListenerRegistration? = null
    private var regRecorrentes: ListenerRegistration? = null
    private var regTransferencias: ListenerRegistration? = null
    private var regMetas: ListenerRegistration? = null
    private var uidAtivo: String? = null

    /** Garante listeners pro uid atual. Idempotente. */
    fun garantirSessao(uid: String?) {
        if (uid == null) {
            parar()
            return
        }
        if (uid == uidAtivo &&
            regCarteiras != null &&
            regLancamentos != null &&
            regRecorrentes != null &&
            regTransferencias != null &&
            regMetas != null
        ) return
        parar()
        uidAtivo = uid
        FinancasLuzEngine.ligarUid(uid)
        regCarteiras = FirestoreFinancas.subscribeCarteiras(
            uid = uid,
            onChange = { _carteiras.value = it },
            onError = { Log.e(TAG, "listener carteiras: ${it.message}", it) },
        )
        regLancamentos = FirestoreFinancas.subscribeLancamentos(
            uid = uid,
            onChange = {
                _lancamentos.value = it
                tentarGerar()
                reconciliarLuz()
            },
            onError = { Log.e(TAG, "listener lancamentos: ${it.message}", it) },
        )
        regRecorrentes = FirestoreFinancas.subscribeRecorrentes(
            uid = uid,
            onChange = {
                _recorrentes.value = it
                tentarGerar()
                reconciliarLuz()
            },
            onError = { Log.e(TAG, "listener recorrentes: ${it.message}", it) },
        )
        regTransferencias = FirestoreFinancas.subscribeTransferencias(
            uid = uid,
            onChange = { _transferencias.value = it },
            onError = { Log.e(TAG, "listener transferencias: ${it.message}", it) },
        )
        regMetas = FirestoreFinancas.subscribeMetas(
            uid = uid,
            onChange = { _metas.value = it },
            onError = { Log.e(TAG, "listener metas: ${it.message}", it) },
        )
    }

    private fun tentarGerar() {
        val uid = uidAtivo ?: return
        val recorrentes = _recorrentes.value
        val lancamentos = _lancamentos.value
        if (recorrentes.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                GeracaoRecorrentes.garantirMesAtual(uid, recorrentes, lancamentos)
            }
        }
    }

    private fun reconciliarLuz() {
        val lancamentos = _lancamentos.value
        val recorrentes = _recorrentes.value
        scope.launch(Dispatchers.Default) {
            runCatching {
                FinancasLuzEngine.reconciliar(lancamentos, recorrentes)
            }
        }
    }

    fun parar() {
        regCarteiras?.remove()
        regLancamentos?.remove()
        regRecorrentes?.remove()
        regTransferencias?.remove()
        regMetas?.remove()
        regCarteiras = null
        regLancamentos = null
        regRecorrentes = null
        regTransferencias = null
        regMetas = null
        uidAtivo = null
        FinancasLuzEngine.ligarUid(null)
        _carteiras.value = emptyList()
        _lancamentos.value = emptyList()
        _recorrentes.value = emptyList()
        _transferencias.value = emptyList()
        _metas.value = emptyList()
    }

    fun garantirSessaoAtual() {
        garantirSessao(FirebaseAuth.getInstance().currentUser?.uid)
    }
}
