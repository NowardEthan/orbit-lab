package com.ethan.orbitlab.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * A estante de artefatos — espelho de
 * `luna-core/mobile-api/src/firestoreDocumentos.ts`.
 *
 * O servidor grava em `users/{uid}/documentos` quando a Luna usa `criar_artefato`. Aqui o app
 * escuta os artefatos DESTA conversa (`conversaId`) e os desenha como cartões no fio, ancorados
 * pelo `createdAt` logo depois da mensagem que os criou.
 *
 * (A coleção Firestore continua `documentos` de propósito — o nome «artefato» é só o rótulo
 * visível, pra não confundir com os ARQUIVOS/PDFs anexados. Sem migração de dados.)
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
     * A estante inteira: TODOS os artefatos do usuário, de qualquer conversa. Sem filtro (logo
     * sem índice composto) e ordenado no cliente por `updatedAt` desc — os mexidos há menos
     * tempo no topo. É o que a tela «Meus artefatos» escuta.
     */
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
     * Salva a edição do próprio Ethan — o outro lado da mão que a Luna tem no core. Mexe só no
     * título/conteúdo, carimba `updatedAt` e marca `updatedBy = "user"`. O listener já ativo
     * redesenha o cartão e o leitor sozinho.
     */
    suspend fun atualizar(
        uid: String,
        id: String,
        titulo: String,
        conteudo: String,
        // Guardar uma versão antes de sobrescrever? Sim numa edição de verdade; NÃO num toque de
        // checkbox (senão cada ☑ vira uma "versão" e polui o histórico).
        versionar: Boolean = true,
    ) {
        val ref = documentosCol(uid).document(id)
        if (versionar) {
            // Best-effort: se a foto falhar (regra ainda não propagou, rede caiu), o histórico
            // perde UM retrato — mas o salvamento do texto NÃO pode ser refém disso. Salvar é sagrado.
            runCatching {
                val snap = ref.get().await()
                val atual = snap.getString("conteudo").orEmpty()
                // Só tira o retrato quando o CORPO muda mesmo e havia texto (nada de versão vazia).
                if (snap.exists() && atual.isNotBlank() && atual != conteudo) {
                    snapshotVersao(uid, id, snap)
                }
            }
        }
        ref.update(
            mapOf(
                "titulo" to titulo.trim(),
                "conteudo" to conteudo,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "user",
            ),
        ).await()
    }

    /**
     * O histórico de versões anteriores de um artefato — os retratos guardados a cada reescrita,
     * do mais recente ao mais antigo. Leitura sob demanda (só ao abrir o «Histórico»), não um
     * listener: ninguém fica olhando o histórico o tempo todo.
     */
    suspend fun lerVersoes(uid: String, docId: String): List<VersaoUi> {
        val snap = versoesCol(uid, docId).get().await()
        return snap.documents.mapNotNull { d ->
            val conteudo = d.getString("conteudo") ?: return@mapNotNull null
            VersaoUi(
                id = d.id,
                titulo = d.getString("titulo").orEmpty().ifBlank { "Sem título" },
                conteudo = conteudo,
                savedAtMs = d.getLong("savedAt") ?: 0L,
                autor = d.getString("autor")?.takeIf { it.isNotBlank() } ?: "luna",
            )
        }.sortedByDescending { it.savedAtMs }
    }

    /**
     * Volta o artefato a uma versão anterior. Guarda o estado ATUAL como versão primeiro (restaurar
     * também é desfazível — nada se perde), depois reescreve o corpo com o da foto escolhida. Marca
     * `updatedBy = "user"` (foi o Ethan quem restaurou).
     */
    suspend fun restaurar(uid: String, docId: String, versao: VersaoUi) {
        val ref = documentosCol(uid).document(docId)
        // Best-effort de novo: guardar o "agora" antes de voltar é o ideal, mas não pode travar a
        // restauração em si (é o que o Ethan pediu ao tocar «Restaurar»).
        runCatching {
            val snap = ref.get().await()
            val atual = snap.getString("conteudo").orEmpty()
            if (snap.exists() && atual.isNotBlank() && atual != versao.conteudo) {
                snapshotVersao(uid, docId, snap)
            }
        }
        ref.update(
            mapOf(
                "titulo" to versao.titulo,
                "conteudo" to versao.conteudo,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "user",
            ),
        ).await()
    }

    /** Grava um retrato do estado atual (lido de `snap`) na subcoleção `versoes`. */
    private suspend fun snapshotVersao(uid: String, docId: String, snap: DocumentSnapshot) {
        val data = snap.data ?: return
        versoesCol(uid, docId).add(
            mapOf(
                "titulo" to (data["titulo"] as? String).orEmpty(),
                "conteudo" to (data["conteudo"] as? String).orEmpty(),
                "savedAt" to (timestampMs(data["updatedAt"]) ?: System.currentTimeMillis()),
                "autor" to ((data["updatedBy"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (data["origem"] as? String)?.takeIf { it.isNotBlank() }
                    ?: "luna"),
            ),
        ).await()
    }

    /**
     * Salva a «bíblia» do artefato — os fatos fixos (nomes, idades, relações). É METADADO à parte
     * do corpo: mexe só no campo `canone`, NÃO versiona (não é reescrita de texto) e carimba quem
     * tocou. O mesmo campo que a Luna escreve no core via `anotar_canone` — aqui é o Ethan editando
     * à mão. O leitor que estiver ouvindo o doc redesenha sozinho.
     */
    suspend fun atualizarCanone(uid: String, id: String, canone: String) {
        documentosCol(uid).document(id).update(
            mapOf(
                "canone" to canone,
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "user",
            ),
        ).await()
    }

    /** Renomeia só o título (atalho do `atualizar` sem tocar no corpo). */
    suspend fun renomear(uid: String, id: String, titulo: String) {
        documentosCol(uid).document(id).update(
            mapOf(
                "titulo" to titulo.trim(),
                "updatedAt" to System.currentTimeMillis(),
                "updatedBy" to "user",
            ),
        ).await()
    }

    /** Apaga o artefato da estante. Some do fio (o listener redesenha sem ele). */
    suspend fun apagar(uid: String, id: String) {
        documentosCol(uid).document(id).delete().await()
    }

    /**
     * Duplica um artefato — cria uma cópia na MESMA conversa (lê o original pra herdar o
     * `conversaId`, sem o qual o cartão não apareceria no fio). Nasce marcada `origem = "user"`
     * (foi o Ethan quem duplicou) com título «Cópia de …». Devolve o id novo.
     */
    suspend fun duplicar(uid: String, id: String): String {
        val orig = documentosCol(uid).document(id).get().await()
        val dados = orig.data ?: throw IllegalStateException("artefato $id não existe")
        val agora = System.currentTimeMillis()
        val tituloOrig = (dados["titulo"] as? String)?.trim().orEmpty().ifBlank { "Artefato" }
        val novo = hashMapOf(
            "titulo" to "Cópia de $tituloOrig",
            "conteudo" to (dados["conteudo"] as? String).orEmpty(),
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
        if (titulo.isBlank() && conteudo.isBlank()) return null
        val origem = (data["origem"] as? String)?.takeIf { it.isNotBlank() } ?: "luna"
        val tituloFinal = titulo.ifBlank { "Artefato" }
        registrarTitulo(doc.id, tituloFinal)
        return DocumentoUi(
            id = doc.id,
            titulo = tituloFinal,
            conteudo = conteudo,
            canone = (data["canone"] as? String).orEmpty(),
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            updatedAtMs = timestampMs(data["updatedAt"]) ?: timestampMs(data["createdAt"]) ?: 0L,
            origem = origem,
            updatedBy = (data["updatedBy"] as? String)?.takeIf { it.isNotBlank() } ?: origem,
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
    /** Quando este retrato foi guardado (era o `updatedAt` do estado preservado). */
    val savedAtMs: Long,
    /** Quem tinha mexido nesse estado preservado: "luna" | "user". */
    val autor: String,
)
