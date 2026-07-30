package com.ethan.orbitlab.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * A estante de documentos — espelho de
 * `luna-core/mobile-api/src/firestoreDocumentos.ts`.
 *
 * O servidor grava em `users/{uid}/documentos` quando a Luna usa `criar_documento`. Aqui o app
 * escuta os documentos DESTA conversa (`conversaId`) e os desenha como cartões no fio, ancorados
 * pelo `createdAt` logo depois da mensagem que os criou.
 */
object FirestoreDocumentos {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private fun documentosCol(uid: String) =
        db.collection("users").document(uid).collection("documentos")

    /**
     * Escuta os documentos nascidos nesta conversa. Filtra só por `conversaId` (igualdade, sem
     * índice composto) e ordena no cliente por `createdAt` — evita ter de provisionar índice.
     */
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

    /**
     * Salva a edição do próprio Ethan — o outro lado da mão que a Luna tem no core. Mexe só no
     * título/conteúdo, carimba `updatedAt` e marca `updatedBy = "user"`. O listener já ativo
     * redesenha o cartão e o leitor sozinho.
     */
    suspend fun atualizar(uid: String, id: String, titulo: String, conteudo: String) {
        documentosCol(uid).document(id).update(
            mapOf(
                "titulo" to titulo.trim(),
                "conteudo" to conteudo,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "user",
            ),
        ).await()
    }

    private fun toDocumento(doc: DocumentSnapshot): DocumentoUi? {
        val data = doc.data ?: return null
        val titulo = (data["titulo"] as? String)?.trim().orEmpty()
        val conteudo = (data["conteudo"] as? String).orEmpty()
        if (titulo.isBlank() && conteudo.isBlank()) return null
        return DocumentoUi(
            id = doc.id,
            titulo = titulo.ifBlank { "Documento" },
            conteudo = conteudo,
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            updatedAtMs = timestampMs(data["updatedAt"]) ?: timestampMs(data["createdAt"]) ?: 0L,
        )
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
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
