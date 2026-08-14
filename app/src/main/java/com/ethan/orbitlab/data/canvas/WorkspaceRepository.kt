package com.ethan.orbitlab.data.canvas

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repositório de workspaces — wrapper de alto nível sobre FirestoreWorkspaces
 * para uso na UI com StateFlow.
 */
class WorkspaceRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ═══════════════════════════════════════════════════════════════════
    //  STATE
    // ═══════════════════════════════════════════════════════════════════

    private val _workspaces = MutableStateFlow<List<WorkspaceUi>>(emptyList())
    val workspaces: StateFlow<List<WorkspaceUi>> = _workspaces.asStateFlow()

    private val _workspaceAtual = MutableStateFlow<CanvasWorkspace?>(null)
    val workspaceAtual: StateFlow<CanvasWorkspace?> = _workspaceAtual.asStateFlow()

    private val _elementos = MutableStateFlow<Map<String, CanvasElement>>(emptyMap())
    val elementos: StateFlow<Map<String, CanvasElement>> = _elementos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════
    //  LISTENERS
    // ═══════════════════════════════════════════════════════════════════

    private var workspacesListener: ListenerRegistration? = null
    private var workspaceListener: ListenerRegistration? = null
    private var elementosListener: ListenerRegistration? = null

    /**
     * Inicia escuta da lista de workspaces.
     */
    fun iniciarEscutaWorkspaces(uid: String) {
        pararEscutaWorkspaces()
        workspacesListener = FirestoreWorkspaces.subscribeTodos(
            uid = uid,
            onChange = { lista ->
                _workspaces.value = lista
            },
            onError = { e ->
                _error.value = "Erro ao carregar workspaces: ${e.message}"
            },
        )
    }

    /**
     * Inicia escuta de um workspace específico com seus elementos.
     */
    fun iniciarEscutaWorkspace(uid: String, workspaceId: String) {
        pararEscutaWorkspace()
        _loading.value = true
        workspaceListener = FirestoreWorkspaces.subscribeWorkspace(
            uid = uid,
            workspaceId = workspaceId,
            onChange = { ws ->
                _workspaceAtual.value = ws
                _loading.value = false
            },
            onError = { e ->
                _error.value = "Erro ao carregar workspace: ${e.message}"
                _loading.value = false
            },
        )
        elementosListener = FirestoreWorkspaces.subscribeElementos(
            uid = uid,
            workspaceId = workspaceId,
            onChange = { els ->
                _elementos.value = els
            },
            onError = { e ->
                _error.value = "Erro ao carregar elementos: ${e.message}"
            },
        )
    }

    /**
     * Para escuta da lista de workspaces.
     */
    fun pararEscutaWorkspaces() {
        workspacesListener?.remove()
        workspacesListener = null
    }

    /**
     * Para escuta do workspace atual.
     */
    fun pararEscutaWorkspace() {
        workspaceListener?.remove()
        workspaceListener = null
        elementosListener?.remove()
        elementosListener = null
    }

    /**
     * Limpa todo o state.
     */
    fun limpar() {
        pararEscutaWorkspaces()
        pararEscutaWorkspace()
        _workspaces.value = emptyList()
        _workspaceAtual.value = null
        _elementos.value = emptyMap()
        _error.value = null
    }

    fun dispose() {
        limpar()
        scope.cancel()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CRUD
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Cria workspace novo.
     */
    fun criar(uid: String, titulo: String, onSucesso: (String) -> Unit = {}) {
        scope.launch {
            try {
                _loading.value = true
                _error.value = null
                val id = FirestoreWorkspaces.criar(uid, titulo)
                _loading.value = false
                onSucesso(id)
            } catch (e: Exception) {
                _error.value = "Erro ao criar workspace: ${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * Carrega workspace com elementos (não reativo).
     */
    suspend fun carregar(uid: String, workspaceId: String): CanvasWorkspace? {
        return try {
            _loading.value = true
            _error.value = null
            val ws = FirestoreWorkspaces.lerComElementos(uid, workspaceId)
            _workspaceAtual.value = ws
            _elementos.value = ws?.elementos ?: emptyMap()
            _loading.value = false
            ws
        } catch (e: Exception) {
            _error.value = "Erro ao carregar workspace: ${e.message}"
            _loading.value = false
            null
        }
    }

    /**
     * Renomeia workspace.
     */
    fun renomear(uid: String, workspaceId: String, titulo: String) {
        scope.launch {
            try {
                _error.value = null
                FirestoreWorkspaces.renomear(uid, workspaceId, titulo)
            } catch (e: Exception) {
                _error.value = "Erro ao renomear: ${e.message}"
            }
        }
    }

    /**
     * Apaga workspace.
     */
    fun apagar(uid: String, workspaceId: String, onSucesso: () -> Unit = {}) {
        scope.launch {
            try {
                _loading.value = true
                _error.value = null
                FirestoreWorkspaces.apagar(uid, workspaceId)
                _loading.value = false
                onSucesso()
            } catch (e: Exception) {
                _error.value = "Erro ao apagar: ${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * Duplica workspace.
     */
    fun duplicar(uid: String, workspaceId: String, onSucesso: (String) -> Unit = {}) {
        scope.launch {
            try {
                _loading.value = true
                _error.value = null
                val novoId = FirestoreWorkspaces.duplicar(uid, workspaceId)
                _loading.value = false
                onSucesso(novoId)
            } catch (e: Exception) {
                _error.value = "Erro ao duplicar: ${e.message}"
                _loading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ELEMENTOS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Adiciona elemento ao workspace atual.
     */
    fun adicionarElemento(uid: String, workspaceId: String, elemento: CanvasElement) {
        scope.launch {
            try {
                _error.value = null
                FirestoreWorkspaces.adicionarElemento(uid, workspaceId, elemento)
                // Atualiza state local (Firestore listener vai confirmar depois)
                _elementos.value = _elementos.value + (elemento.id to elemento)
            } catch (e: Exception) {
                _error.value = "Erro ao adicionar elemento: ${e.message}"
            }
        }
    }

    /**
     * Move elemento (atualiza bounds).
     */
    fun moverElemento(uid: String, workspaceId: String, elementoId: String, x: Float, y: Float) {
        scope.launch {
            try {
                _error.value = null
                val bounds = ElementoBounds(
                    x = x,
                    y = y,
                    width = _elementos.value[elementoId]?.bounds?.width ?: 100f,
                    height = _elementos.value[elementoId]?.bounds?.height ?: 100f,
                )
                FirestoreWorkspaces.atualizarBounds(uid, workspaceId, elementoId, bounds)
                // Atualiza state local
                _elementos.value[elementoId]?.let { el ->
                    _elementos.value = _elementos.value + (elementoId to el.copy(bounds = bounds))
                }
            } catch (e: Exception) {
                _error.value = "Erro ao mover elemento: ${e.message}"
            }
        }
    }

    /**
     * Move elemento com animação (finaliza bounds após animação).
     */
    suspend fun moverElementoAnimado(
        uid: String,
        workspaceId: String,
        elementoId: String,
        x: Float,
        y: Float,
    ) {
        try {
            _error.value = null
            val bounds = ElementoBounds(
                x = x,
                y = y,
                width = _elementos.value[elementoId]?.bounds?.width ?: 100f,
                height = _elementos.value[elementoId]?.bounds?.height ?: 100f,
            )
            // Atualiza state local primeiro (animação)
            _elementos.value[elementoId]?.let { el ->
                _elementos.value = _elementos.value + (elementoId to el.copy(bounds = bounds))
            }
            // Persiste no Firestore
            FirestoreWorkspaces.atualizarBounds(uid, workspaceId, elementoId, bounds)
        } catch (e: Exception) {
            _error.value = "Erro ao mover elemento: ${e.message}"
        }
    }

    /**
     * Redimensiona elemento.
     */
    fun redimensionarElemento(
        uid: String,
        workspaceId: String,
        elementoId: String,
        width: Float,
        height: Float,
    ) {
        scope.launch {
            try {
                _error.value = null
                val bounds = ElementoBounds(
                    x = _elementos.value[elementoId]?.bounds?.x ?: 0f,
                    y = _elementos.value[elementoId]?.bounds?.y ?: 0f,
                    width = width,
                    height = height,
                )
                FirestoreWorkspaces.atualizarBounds(uid, workspaceId, elementoId, bounds)
                // Atualiza state local
                _elementos.value[elementoId]?.let { el ->
                    _elementos.value = _elementos.value + (elementoId to el.copy(bounds = bounds))
                }
            } catch (e: Exception) {
                _error.value = "Erro ao redimensionar: ${e.message}"
            }
        }
    }

    /**
     * Estiliza elemento.
     */
    fun estilizarElemento(
        uid: String,
        workspaceId: String,
        elementoId: String,
        style: ElementoStyle,
    ) {
        scope.launch {
            try {
                _error.value = null
                FirestoreWorkspaces.atualizarStyle(uid, workspaceId, elementoId, style)
                // Atualiza state local
                _elementos.value[elementoId]?.let { el ->
                    _elementos.value = _elementos.value + (elementoId to el.copy(style = style))
                }
            } catch (e: Exception) {
                _error.value = "Erro ao estilizar: ${e.message}"
            }
        }
    }

    /**
     * Remove elemento.
     */
    fun removerElemento(uid: String, workspaceId: String, elementoId: String) {
        scope.launch {
            try {
                _error.value = null
                FirestoreWorkspaces.removerElemento(uid, workspaceId, elementoId)
                // Atualiza state local
                _elementos.value = _elementos.value - elementoId
            } catch (e: Exception) {
                _error.value = "Erro ao remover elemento: ${e.message}"
            }
        }
    }

    /**
     * Atualiza múltiplos elementos (batch).
     */
    fun atualizarElementos(
        uid: String,
        workspaceId: String,
        elementos: Map<String, CanvasElement>,
    ) {
        scope.launch {
            try {
                _error.value = null
                FirestoreWorkspaces.atualizarElementos(uid, workspaceId, elementos)
                // Atualiza state local
                _elementos.value = _elementos.value + elementos
            } catch (e: Exception) {
                _error.value = "Erro ao atualizar elementos: ${e.message}"
            }
        }
    }

    /**
     * Atualiza elemento localmente (sem persistir) — útil para preview antes de commitar.
     */
    fun atualizarElementoLocal(elemento: CanvasElement) {
        _elementos.value = _elementos.value + (elemento.id to elemento)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  VERSIONAMENTO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Cria snapshot de versão.
     */
    fun criarVersao(uid: String, workspaceId: String, autor: String = "luna", delta: String? = null) {
        scope.launch {
            try {
                _error.value = null
                FirestoreWorkspaces.criarVersao(uid, workspaceId, autor, delta)
            } catch (e: Exception) {
                _error.value = "Erro ao criar versão: ${e.message}"
            }
        }
    }

    /**
     * Lista versões do workspace.
     */
    suspend fun listarVersoes(uid: String, workspaceId: String): List<CanvasVersaoUi> {
        return try {
            _error.value = null
            FirestoreWorkspaces.listarVersoes(uid, workspaceId)
        } catch (e: Exception) {
            _error.value = "Erro ao listar versões: ${e.message}"
            emptyList()
        }
    }

    /**
     * Restaura workspace para versão anterior.
     */
    fun restaurarVersao(uid: String, workspaceId: String, versaoNumero: Int) {
        scope.launch {
            try {
                _loading.value = true
                _error.value = null
                FirestoreWorkspaces.restaurarVersao(uid, workspaceId, versaoNumero)
                // Recarrega workspace após restaurar
                carregar(uid, workspaceId)
            } catch (e: Exception) {
                _error.value = "Erro ao restaurar versão: ${e.message}"
                _loading.value = false
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════════

    /** Obtém elemento por ID. */
    fun elemento(id: String): CanvasElement? = _elementos.value[id]

    /** Lista de elementos ordenada por z-index (para render). */
    fun elementosOrdenados(): List<CanvasElement> =
        _elementos.value.values.sortedBy { it.bounds.y }

    /** Limpa erro. */
    fun limparErro() {
        _error.value = null
    }
}
