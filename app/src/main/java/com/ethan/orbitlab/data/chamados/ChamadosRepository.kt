package com.ethan.orbitlab.data.chamados

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado dos chamados enquanto a sessão existir. Molde igual ao
 * [com.ethan.orbitlab.data.financas.FinancasRepository]: um `object` com [StateFlow]s +
 * listeners que se limpam ao trocar de sessão.
 *
 * Admin: quando o `uid` logado é o do Ethan, o repositório também escuta a coleção inteira
 * (a caixa de entrada). Pra qualquer outro usuário, [ehAdmin] fica `false` e [todos] vazio.
 */
object ChamadosRepository {
    /**
     * uid do Ethan (dono do app) — destrava o modo admin ("seu lado").
     * É o mesmo `UID_CRIADOR_CANONICO` que dá cota ilimitada no servidor
     * (luna-core: interlocutor/esquemaInterlocutor.ts → ehCriadorVerificado).
     * Vazio = ninguém é admin (o app funciona normal, só sem a caixa de entrada).
     */
    private const val ETHAN_UID = "aKp1czWVMqWQdJ9nAIcIKgxKNu92"

    private val _meus = MutableStateFlow<List<Chamado>>(emptyList())
    val meus: StateFlow<List<Chamado>> = _meus.asStateFlow()

    private val _todos = MutableStateFlow<List<Chamado>>(emptyList())
    val todos: StateFlow<List<Chamado>> = _todos.asStateFlow()

    private val _ehAdmin = MutableStateFlow(false)
    val ehAdmin: StateFlow<Boolean> = _ehAdmin.asStateFlow()

    private var regMeus: ListenerRegistration? = null
    private var regTodos: ListenerRegistration? = null
    private var uidAtual: String? = null

    /** Liga (ou troca) a escuta pra sessão atual. Idempotente pro mesmo uid. */
    fun garantirSessao(uid: String?) {
        if (uid == uidAtual) return
        limpar()
        uidAtual = uid
        if (uid == null) return

        regMeus = FirestoreChamados.subscribeMeus(uid, onChange = { _meus.value = it })

        val admin = ETHAN_UID.isNotBlank() && uid == ETHAN_UID
        _ehAdmin.value = admin
        if (admin) {
            regTodos = FirestoreChamados.subscribeTodos(onChange = { _todos.value = it })
        }
    }

    private fun limpar() {
        regMeus?.remove(); regMeus = null
        regTodos?.remove(); regTodos = null
        _meus.value = emptyList()
        _todos.value = emptyList()
        _ehAdmin.value = false
        uidAtual = null
    }
}
