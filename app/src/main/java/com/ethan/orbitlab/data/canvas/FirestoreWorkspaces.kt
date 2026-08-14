package com.ethan.orbitlab.data.canvas

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.tasks.await

/**
 * Workspaces do canvas — espelho de
 * `luna-core/mobile-api/src/firestoreWorkspaces.ts`.
 *
 * Schema: `elementos` é um mapa de ID → CanvasElement (não array, para updates granulares).
 * Versionamento opcional (snapshots no histórico).
 */
object FirestoreWorkspaces {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // ═══════════════════════════════════════════════════════════════════
    //  CAMINHOS DE COLEÇÃO
    // ═══════════════════════════════════════════════════════════════════

    private fun workspacesCol(uid: String) =
        db.collection("users").document(uid).collection("workspaces")

    private fun elementosCol(uid: String, wsId: String) =
        workspacesCol(uid).document(wsId).collection("elementos")

    private fun versoesCol(uid: String, wsId: String) =
        workspacesCol(uid).document(wsId).collection("versoes")

    private fun wsDoc(uid: String, wsId: String) =
        workspacesCol(uid).document(wsId)

    private fun elDoc(uid: String, wsId: String, elId: String) =
        elementosCol(uid, wsId).document(elId)

    // ═══════════════════════════════════════════════════════════════════
    //  SUBSCRIBE / LISTENERS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Escuta todos os workspaces do usuário (para galeria).
     */
    fun subscribeTodos(
        uid: String,
        onChange: (List<WorkspaceUi>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return workspacesCol(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err)
                    return@addSnapshotListener
                }
                val workspaces = snap?.documents.orEmpty()
                    .mapNotNull { toWorkspaceUi(it) }
                    .sortedByDescending { it.atualizadoEm }
                onChange(workspaces)
            }
    }

    /**
     * Escuta um workspace específico (com elementos).
     */
    fun subscribeWorkspace(
        uid: String,
        workspaceId: String,
        onChange: (CanvasWorkspace?) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return wsDoc(uid, workspaceId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err)
                    return@addSnapshotListener
                }
                if (snap == null || !snap.exists()) {
                    onChange(null)
                    return@addSnapshotListener
                }
                onChange(toWorkspace(snap))
            }
    }

    /**
     * Escuta elementos de um workspace (mais granular).
     */
    fun subscribeElementos(
        uid: String,
        workspaceId: String,
        onChange: (Map<String, CanvasElement>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return elementosCol(uid, workspaceId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err)
                    return@addSnapshotListener
                }
                val elementos = snap?.documents.orEmpty()
                    .mapNotNull { d ->
                        val raw = d.data as? Map<*, *> ?: return@mapNotNull null
                        d.id to parseCanvasElement(d.id, raw)
                    }
                    .toMap()
                onChange(elementos)
            }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CRUD
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Cria um workspace novo.
     */
    suspend fun criar(uid: String, titulo: String): String {
        val agora = System.currentTimeMillis()
        val ws = CanvasWorkspace.criar(titulo, uid)
        val ref = workspacesCol(uid).document(ws.id)
        ref.set(ws.toFirestoreMap()).await()
        return ws.id
    }

    /**
     * Lê um workspace (sem elementos granulares — usar subscribe para real-time).
     */
    suspend fun ler(uid: String, workspaceId: String): CanvasWorkspace? {
        val snap = wsDoc(uid, workspaceId).get().await()
        return if (snap.exists()) toWorkspace(snap) else null
    }

    /**
     * Lê workspace com elementos carregados (batch).
     */
    suspend fun lerComElementos(uid: String, workspaceId: String): CanvasWorkspace? {
        val ws = ler(uid, workspaceId) ?: return null
        val elementosSnap = elementosCol(uid, workspaceId).get().await()
        val elementos = elementosSnap.documents
            .mapNotNull { d ->
                val raw = d.data as? Map<*, *> ?: return@mapNotNull null
                d.id to parseCanvasElement(d.id, raw)
            }
            .toMap()
        return ws.copy(elementos = elementos)
    }

    /**
     * Renomeia workspace.
     */
    suspend fun renomear(uid: String, workspaceId: String, titulo: String) {
        wsDoc(uid, workspaceId).update(
            mapOf(
                "titulo" to titulo.trim(),
                "atualizadoEm" to System.currentTimeMillis(),
            ),
        ).await()
    }

    /**
     * Apaga workspace e todos os seus elementos.
     */
    suspend fun apagar(uid: String, workspaceId: String) {
        val batch: WriteBatch = db.batch()
        // Apagar workspace
        batch.delete(wsDoc(uid, workspaceId))
        // Apagar elementos
        val elementosSnap = elementosCol(uid, workspaceId).get().await()
        elementosSnap.documents.forEach { d ->
            batch.delete(d.reference)
        }
        // Apagar versões
        val versoesSnap = versoesCol(uid, workspaceId).get().await()
        versoesSnap.documents.forEach { d ->
            batch.delete(d.reference)
        }
        batch.commit().await()
    }

    /**
     * Duplica workspace.
     */
    suspend fun duplicar(uid: String, workspaceId: String): String {
        val orig = lerComElementos(uid, workspaceId) ?: throw IllegalStateException("workspace $workspaceId não existe")
        val agora = System.currentTimeMillis()
        val novo = orig.copy(
            id = CanvasWorkspace.novoId(),
            titulo = "Cópia de ${orig.titulo}",
            versao = 1,
            criadoEm = agora,
            atualizadoEm = agora,
        )
        val batch: WriteBatch = db.batch()
        batch.set(wsDoc(uid, novo.id), novo.toFirestoreMap())
        // Duplicar elementos
        orig.elementos.forEach { (elId, el) ->
            batch.set(elDoc(uid, novo.id, elId), el.toFirestoreMap())
        }
        batch.commit().await()
        return novo.id
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ELEMENTOS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Adiciona elemento ao workspace.
     */
    suspend fun adicionarElemento(uid: String, workspaceId: String, elemento: CanvasElement) {
        val batch: WriteBatch = db.batch()
        batch.set(elDoc(uid, workspaceId, elemento.id), elemento.toFirestoreMap())
        batch.update(
            wsDoc(uid, workspaceId),
            mapOf(
                "atualizadoEm" to System.currentTimeMillis(),
                "versao" to FieldValue.increment(1),
                "elementoCount" to FieldValue.increment(1),
            ),
        )
        batch.commit().await()
    }

    /**
     * Atualiza bounds/posição de um elemento (com animação opcional).
     */
    suspend fun atualizarBounds(
        uid: String,
        workspaceId: String,
        elementoId: String,
        bounds: ElementoBounds,
        rotation: Float? = null,
    ) {
        val updates = linkedMapOf<String, Any>(
            "bounds" to bounds.toFirestoreMap(),
            "atualizadoEm" to System.currentTimeMillis(),
        )
        if (rotation != null) updates["rotation"] = rotation
        val batch: WriteBatch = db.batch()
        batch.update(elDoc(uid, workspaceId, elementoId), updates)
        batch.update(
            wsDoc(uid, workspaceId),
            mapOf(
                "atualizadoEm" to System.currentTimeMillis(),
                "versao" to FieldValue.increment(1),
            ),
        )
        batch.commit().await()
    }

    /**
     * Atualiza estilo de um elemento.
     */
    suspend fun atualizarStyle(
        uid: String,
        workspaceId: String,
        elementoId: String,
        style: ElementoStyle,
    ) {
        val batch: WriteBatch = db.batch()
        batch.update(
            elDoc(uid, workspaceId, elementoId),
            mapOf(
                "style" to style.toFirestoreMap(),
                "atualizadoEm" to System.currentTimeMillis(),
            ),
        )
        batch.update(
            wsDoc(uid, workspaceId),
            mapOf(
                "atualizadoEm" to System.currentTimeMillis(),
                "versao" to FieldValue.increment(1),
            ),
        )
        batch.commit().await()
    }

    /**
     * Atualiza props específicos de um elemento.
     */
    suspend fun atualizarProps(
        uid: String,
        workspaceId: String,
        elementoId: String,
        propsKey: String,
        propsMap: Map<String, Any?>,
    ) {
        val batch: WriteBatch = db.batch()
        batch.update(
            elDoc(uid, workspaceId, elementoId),
            mapOf(
                propsKey to propsMap,
                "atualizadoEm" to System.currentTimeMillis(),
            ),
        )
        batch.update(
            wsDoc(uid, workspaceId),
            mapOf(
                "atualizadoEm" to System.currentTimeMillis(),
                "versao" to FieldValue.increment(1),
            ),
        )
        batch.commit().await()
    }

    /**
     * Remove elemento do workspace.
     */
    suspend fun removerElemento(uid: String, workspaceId: String, elementoId: String) {
        val batch: WriteBatch = db.batch()
        batch.delete(elDoc(uid, workspaceId, elementoId))
        batch.update(
            wsDoc(uid, workspaceId),
            mapOf(
                "atualizadoEm" to System.currentTimeMillis(),
                "versao" to FieldValue.increment(1),
                "elementoCount" to FieldValue.increment(-1),
            ),
        )
        batch.commit().await()
    }

    /**
     * Atualiza múltiplos elementos de uma vez (batch).
     */
    suspend fun atualizarElementos(
        uid: String,
        workspaceId: String,
        elementos: Map<String, CanvasElement>,
    ) {
        val batch: WriteBatch = db.batch()
        elementos.forEach { (elId, el) ->
            batch.set(elDoc(uid, workspaceId, elId), el.toFirestoreMap())
        }
        batch.update(
            wsDoc(uid, workspaceId),
            mapOf(
                "atualizadoEm" to System.currentTimeMillis(),
                "versao" to FieldValue.increment(elementos.size.toLong()),
                "elementoCount" to elementos.size,
            ),
        )
        batch.commit().await()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  VERSIONAMENTO
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Cria snapshot de versão (chamado antes de operações destrutivas).
     */
    suspend fun criarVersao(
        uid: String,
        workspaceId: String,
        autor: String = "luna",
        delta: String? = null,
    ) {
        val elementos = mutableMapOf<String, CanvasElement>()
        val elementosSnap = elementosCol(uid, workspaceId).get().await()
        elementosSnap.documents.forEach { d ->
            val raw = d.data as? Map<*, *> ?: return@forEach
            elementos[d.id] = parseCanvasElement(d.id, raw)
        }
        val wsSnap = wsDoc(uid, workspaceId).get().await()
        val versaoAtual = (wsSnap.getLong("versao") ?: 1).toInt()
        versoesCol(uid, workspaceId).add(
            mapOf(
                "numero" to versaoAtual,
                "elementos" to elementos.mapValues { (_, el) -> el.toFirestoreMap() },
                "autor" to autor,
                "delta" to delta,
                "timestamp" to System.currentTimeMillis(),
            ),
        ).await()
    }

    /**
     * Lista versões do workspace.
     */
    suspend fun listarVersoes(uid: String, workspaceId: String): List<CanvasVersaoUi> {
        val snap = versoesCol(uid, workspaceId).get().await()
        return snap.documents.mapNotNull { d ->
            val raw = d.data ?: return@mapNotNull null
            CanvasVersaoUi(
                numero = (raw["numero"] as? Number)?.toInt() ?: 0,
                autor = raw["autor"] as? String ?: "luna",
                delta = raw["delta"] as? String,
                timestamp = (raw["timestamp"] as? Number)?.toLong()
                    ?: (raw["timestamp"] as? Timestamp)?.toDate()?.time
                    ?: 0L,
            )
        }.sortedByDescending { it.numero }
    }

    /**
     * Restaura workspace para uma versão anterior.
     */
    suspend fun restaurarVersao(uid: String, workspaceId: String, versaoNumero: Int) {
        // Criar versão atual antes de restaurar
        criarVersao(uid, workspaceId, autor = "user", delta = "Antes de restaurar v$versaoNumero")
        // Buscar versão
        val versoesSnap = versoesCol(uid, workspaceId)
            .whereEqualTo("numero", versaoNumero)
            .get().await()
        val versaoDoc = versoesSnap.documents.firstOrNull()
            ?: throw IllegalStateException("versão $versaoNumero não encontrada")
        @Suppress("UNCHECKED_CAST")
        val elementosRaw = versaoDoc.get("elementos") as? Map<*, *>
        @Suppress("UNCHECKED_CAST")
        val elementos: Map<String, CanvasElement> = elementosRaw?.mapNotNull { (elId, elRaw) ->
            val key = elId as? String ?: return@mapNotNull null
            val value = elRaw as? Map<*, *> ?: return@mapNotNull null
            key to parseCanvasElement(key, value)
        }?.toMap() ?: emptyMap()
        // Aplicar elementos
        val batch: WriteBatch = db.batch()
        // Limpar elementos atuais
        val elementosSnap = elementosCol(uid, workspaceId).get().await()
        elementosSnap.documents.forEach { d ->
            batch.delete(d.reference)
        }
        // Restaurar elementos da versão
        elementos.forEach { (elId, el) ->
            batch.set(elDoc(uid, workspaceId, elId), el.toFirestoreMap())
        }
        batch.update(
            wsDoc(uid, workspaceId),
            mapOf(
                "versao" to (versaoNumero + 1),
                "atualizadoEm" to System.currentTimeMillis(),
                "elementoCount" to elementos.size,
            ),
        )
        batch.commit().await()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private fun toWorkspaceUi(doc: DocumentSnapshot): WorkspaceUi? {
        val raw = doc.data ?: return null
        return parseWorkspaceUi(doc.id, raw)
    }

    private fun toWorkspace(doc: DocumentSnapshot): CanvasWorkspace {
        @Suppress("UNCHECKED_CAST")
        val data = doc.data?.let { it as Map<*, *> } ?: emptyMap<Nothing?, Nothing>()
        return parseCanvasWorkspace(doc.id, data)
    }
}

/** UI model para listagem de versões. */
data class CanvasVersaoUi(
    val numero: Int,
    val autor: String,
    val delta: String?,
    val timestamp: Long,
)
