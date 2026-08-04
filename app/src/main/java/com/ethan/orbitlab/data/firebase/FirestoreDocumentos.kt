package com.ethan.orbitlab.data.firebase

import com.ethan.orbitlab.data.artefato.BlocoArtefato
import com.ethan.orbitlab.data.artefato.PropsBlocoArtefato
import com.ethan.orbitlab.data.artefato.SCHEMA_ARTEFATO_BLOCOS
import com.ethan.orbitlab.data.artefato.SCHEMA_ARTEFATO_MD
import com.ethan.orbitlab.data.artefato.TipoBlocoArtefato
import com.ethan.orbitlab.data.artefato.blocosToMd
import com.ethan.orbitlab.data.artefato.mdToBlocos
import com.ethan.orbitlab.data.artefato.normalizarDocumentoBlocos
import com.ethan.orbitlab.data.artefato.novoIdBloco
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * A estante de artefatos — espelho de
 * `luna-core/mobile-api/src/firestoreDocumentos.ts`.
 *
 * Schema v2: `blocos` é a verdade; `conteudo` é projeção Markdown. Docs antigos convertem
 * on-the-fly na leitura; o próximo save grava v2.
 */
object FirestoreDocumentos {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val tituloCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun registrarTitulo(id: String, titulo: String) {
        if (id.isNotBlank() && titulo.isNotBlank()) {
            tituloCache[id] = titulo
        }
    }

    fun obterTitulo(idOrTitle: String): String? {
        if (idOrTitle.isBlank()) return null
        return tituloCache[idOrTitle]
    }

    private fun documentosCol(uid: String) =
        db.collection("users").document(uid).collection("documentos")

    private fun versoesCol(uid: String, docId: String) =
        documentosCol(uid).document(docId).collection("versoes")

    fun subscribeDaConversa(
        uid: String,
        conversaId: String,
        onChange: (List<DocumentoUi>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return documentosCol(uid)
            .whereEqualTo("conversaId", conversaId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err)
                    return@addSnapshotListener
                }
                val docs = snap?.documents.orEmpty()
                    .mapNotNull { toDocumento(it) }
                    .sortedBy { it.createdAtMs }
                onChange(docs)
            }
    }

    fun subscribeTodos(
        uid: String,
        onChange: (List<DocumentoUi>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return documentosCol(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err)
                    return@addSnapshotListener
                }
                val docs = snap?.documents.orEmpty()
                    .mapNotNull { toDocumento(it) }
                    .sortedByDescending { it.updatedAtMs }
                onChange(docs)
            }
    }

    /**
     * Cria um artefato vazio (ou com corpo) pela UI da Galeria — schema v2 desde o nascimento.
     */
    suspend fun criar(
        uid: String,
        titulo: String,
        conteudo: String = "",
        conversaId: String? = null,
        blocos: List<BlocoArtefato>? = null,
    ): String {
        val agora = System.currentTimeMillis()
        val seed = blocos ?: mdToBlocos(conteudo.ifBlank { "" })
        val (_, blocosFinais, md) = normalizarDocumentoBlocos(
            conteudo = conteudo,
            blocos = seed.ifEmpty {
                listOf(BlocoArtefato(id = novoIdBloco(), type = TipoBlocoArtefato.paragraph, text = ""))
            },
            schemaVersion = SCHEMA_ARTEFATO_BLOCOS,
        )
        val novo = hashMapOf<String, Any?>(
            "titulo" to titulo.trim().ifBlank { "Sem título" },
            "conteudo" to md,
            "blocos" to blocosFinais.map { it.toFirestoreMap() },
            "schemaVersion" to SCHEMA_ARTEFATO_BLOCOS,
            "conversaId" to (conversaId ?: ""),
            "origem" to "user",
            "createdAt" to agora,
            "updatedAt" to agora,
            "updatedBy" to "user",
        )
        return documentosCol(uid).add(novo).await().id
    }

    /**
     * Salva a edição do próprio Ethan. Preferir [blocos] quando o editor Notion está ativo —
     * grava v2 + projeção MD. Aceita só [conteudo] (legado) e migra.
     */
    suspend fun atualizar(
        uid: String,
        id: String,
        titulo: String,
        conteudo: String,
        // Guardar uma versão antes de sobrescrever? Sim numa edição de verdade; NÃO num toque de
        // checkbox (senão cada ☑ vira uma "versão" e polui o histórico).
        versionar: Boolean = true,
        blocos: List<BlocoArtefato>? = null,
    ) {
        val ref = documentosCol(uid).document(id)
        val (_, blocosFinais, md) = if (blocos != null) {
            Triple(SCHEMA_ARTEFATO_BLOCOS, blocos, blocosToMd(blocos))
        } else {
            normalizarDocumentoBlocos(conteudo, null, SCHEMA_ARTEFATO_MD)
        }

        if (versionar) {
            runCatching {
                val snap = ref.get().await()
                val atual = snap.getString("conteudo").orEmpty()
                if (snap.exists() && atual.isNotBlank() && atual != md) {
                    snapshotVersao(uid, id, snap)
                }
            }
        }
        ref.update(
            mapOf(
                "titulo" to titulo.trim(),
                "conteudo" to md,
                "blocos" to blocosFinais.map { it.toFirestoreMap() },
                "schemaVersion" to SCHEMA_ARTEFATO_BLOCOS,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "user",
            ),
        ).await()
    }

    suspend fun lerVersoes(uid: String, docId: String): List<VersaoUi> {
        val snap = versoesCol(uid, docId).get().await()
        return snap.documents.mapNotNull { d ->
            val conteudo = d.getString("conteudo") ?: return@mapNotNull null
            val blocos = parseBlocos(d.get("blocos"))
            VersaoUi(
                id = d.id,
                titulo = d.getString("titulo").orEmpty().ifBlank { "Sem título" },
                conteudo = conteudo,
                blocos = blocos,
                savedAtMs = d.getLong("savedAt") ?: 0L,
                autor = d.getString("autor")?.takeIf { it.isNotBlank() } ?: "luna",
            )
        }.sortedByDescending { it.savedAtMs }
    }

    suspend fun restaurar(uid: String, docId: String, versao: VersaoUi) {
        val ref = documentosCol(uid).document(docId)
        runCatching {
            val snap = ref.get().await()
            val atual = snap.getString("conteudo").orEmpty()
            if (snap.exists() && atual.isNotBlank() && atual != versao.conteudo) {
                snapshotVersao(uid, docId, snap)
            }
        }
        val (_, blocos, md) = normalizarDocumentoBlocos(
            versao.conteudo,
            versao.blocos,
            if (versao.blocos != null) SCHEMA_ARTEFATO_BLOCOS else SCHEMA_ARTEFATO_MD,
        )
        ref.update(
            mapOf(
                "titulo" to versao.titulo,
                "conteudo" to md,
                "blocos" to blocos.map { it.toFirestoreMap() },
                "schemaVersion" to SCHEMA_ARTEFATO_BLOCOS,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "user",
            ),
        ).await()
    }

    private suspend fun snapshotVersao(uid: String, docId: String, snap: DocumentSnapshot) {
        val data = snap.data ?: return
        val conteudo = (data["conteudo"] as? String).orEmpty()
        val blocos = parseBlocos(data["blocos"])
        val (_, blocosNorm, md) = normalizarDocumentoBlocos(
            conteudo,
            blocos,
            (data["schemaVersion"] as? Number)?.toInt(),
        )
        versoesCol(uid, docId).add(
            mapOf(
                "titulo" to (data["titulo"] as? String).orEmpty(),
                "conteudo" to md,
                "blocos" to blocosNorm.map { it.toFirestoreMap() },
                "schemaVersion" to SCHEMA_ARTEFATO_BLOCOS,
                "savedAt" to (timestampMs(data["updatedAt"]) ?: System.currentTimeMillis()),
                "autor" to ((data["updatedBy"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (data["origem"] as? String)?.takeIf { it.isNotBlank() }
                    ?: "luna"),
            ),
        ).await()
    }

    suspend fun atualizarCanone(uid: String, id: String, canone: String) {
        documentosCol(uid).document(id).update(
            mapOf(
                "canone" to canone,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "user",
            ),
        ).await()
    }

    suspend fun renomear(uid: String, id: String, titulo: String) {
        documentosCol(uid).document(id).update(
            mapOf(
                "titulo" to titulo.trim(),
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "user",
            ),
        ).await()
    }

    suspend fun apagar(uid: String, id: String) {
        documentosCol(uid).document(id).delete().await()
    }

    suspend fun duplicar(uid: String, id: String): String {
        val orig = documentosCol(uid).document(id).get().await()
        val dados = orig.data ?: throw IllegalStateException("artefato $id não existe")
        val agora = System.currentTimeMillis()
        val tituloOrig = (dados["titulo"] as? String)?.trim().orEmpty().ifBlank { "Artefato" }
        val conteudo = (dados["conteudo"] as? String).orEmpty()
        val blocos = parseBlocos(dados["blocos"])
        val (_, blocosNorm, md) = normalizarDocumentoBlocos(
            conteudo,
            blocos,
            (dados["schemaVersion"] as? Number)?.toInt(),
        )
        val novo = hashMapOf(
            "titulo" to "Cópia de $tituloOrig",
            "conteudo" to md,
            "blocos" to blocosNorm.map { it.toFirestoreMap() },
            "schemaVersion" to SCHEMA_ARTEFATO_BLOCOS,
            "conversaId" to (dados["conversaId"] as? String).orEmpty(),
            "origem" to "user",
            "createdAt" to agora,
            "updatedAt" to agora,
            "updatedBy" to "user",
        )
        return documentosCol(uid).add(novo).await().id
    }

    private fun toDocumento(doc: DocumentSnapshot): DocumentoUi? {
        val data = doc.data ?: return null
        val titulo = (data["titulo"] as? String)?.trim().orEmpty()
        val conteudo = (data["conteudo"] as? String).orEmpty()
        if (titulo.isBlank() && conteudo.isBlank() && data["blocos"] == null) return null
        val origem = (data["origem"] as? String)?.takeIf { it.isNotBlank() } ?: "luna"
        val tituloFinal = titulo.ifBlank { "Artefato" }
        registrarTitulo(doc.id, tituloFinal)
        val rawBlocos = parseBlocos(data["blocos"])
        val schema = (data["schemaVersion"] as? Number)?.toInt()
        val (schemaNorm, blocos, md) = normalizarDocumentoBlocos(conteudo, rawBlocos, schema)
        return DocumentoUi(
            id = doc.id,
            titulo = tituloFinal,
            conteudo = md.ifBlank { conteudo },
            blocos = blocos,
            schemaVersion = schemaNorm,
            canone = (data["canone"] as? String).orEmpty(),
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            updatedAtMs = timestampMs(data["updatedAt"]) ?: timestampMs(data["createdAt"]) ?: 0L,
            origem = origem,
            updatedBy = (data["updatedBy"] as? String)?.takeIf { it.isNotBlank() } ?: origem,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseBlocos(raw: Any?): List<BlocoArtefato>? {
        val lista = raw as? List<*> ?: return null
        if (lista.isEmpty()) return null
        return lista.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            val id = (m["id"] as? String)?.takeIf { it.isNotBlank() } ?: novoIdBloco()
            val type = TipoBlocoArtefato.from(m["type"] as? String)
            val text = (m["text"] as? String).orEmpty()
            val propsMap = m["props"] as? Map<*, *>
            val props = if (propsMap != null) {
                PropsBlocoArtefato(
                    level = (propsMap["level"] as? Number)?.toInt(),
                    checked = propsMap["checked"] as? Boolean,
                    language = propsMap["language"] as? String,
                )
            } else {
                null
            }
            BlocoArtefato(id = id, type = type, text = text, props = props)
        }
    }

    private fun BlocoArtefato.toFirestoreMap(): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>(
            "id" to id,
            "type" to type.name,
            "text" to text,
        )
        val p = props
        if (p != null) {
            val propsMap = linkedMapOf<String, Any?>()
            p.level?.let { propsMap["level"] = it }
            p.checked?.let { propsMap["checked"] = it }
            p.language?.let { propsMap["language"] = it }
            if (propsMap.isNotEmpty()) map["props"] = propsMap
        }
        return map
    }

    private fun timestampMs(value: Any?): Long? = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> null
    }
}

data class DocumentoUi(
    val id: String,
    val titulo: String,
    val conteudo: String,
    val blocos: List<BlocoArtefato> = emptyList(),
    val schemaVersion: Int = SCHEMA_ARTEFATO_BLOCOS,
    /** A «bíblia» — fatos fixos (nomes, idades, relações). `""` quando ainda não há cânone. */
    val canone: String = "",
    val createdAtMs: Long,
    val updatedAtMs: Long,
    /** Quem CRIOU: "luna" | "user". */
    val origem: String = "luna",
    /** Quem mexeu por ÚLTIMO: "luna" | "user". */
    val updatedBy: String = "luna",
)

/** Uma foto do artefato guardada no histórico, tirada antes de uma reescrita sobrescrever o texto. */
data class VersaoUi(
    val id: String,
    val titulo: String,
    val conteudo: String,
    val blocos: List<BlocoArtefato>? = null,
    /** Quando este retrato foi guardado (era o `updatedAt` do estado preservado). */
    val savedAtMs: Long,
    /** Quem tinha mexido nesse estado preservado: "luna" | "user". */
    val autor: String,
)
