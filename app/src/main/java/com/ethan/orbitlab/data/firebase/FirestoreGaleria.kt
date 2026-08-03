package com.ethan.orbitlab.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * A galeria de imagens — o outro lado da [FirestoreDocumentos] (que junta os artefatos). Aqui
 * juntamos TODA imagem do usuário num lugar só: as que a **Luna** desenhou (`gerar_imagem`, guardadas
 * em `mensagem.imagens[]`) e as que **você** anexou (guardadas em `mensagem.attachments[]` com
 * `kind = "image"`, já subidas pro Storage).
 *
 * Diferente dos artefatos, imagem NÃO tem coleção própria — cada uma mora embutida na mensagem onde
 * nasceu, espalhada pelas conversas. Então aqui a gente varre as conversas e recolhe as imagens de
 * cada uma (uma leitura por conversa, em paralelo). É retroativo de graça — pega tudo que já existe,
 * sem migração. Se um dia ficar pesado, o caminho é denormalizar numa coleção `imagens` na hora que
 * a imagem nasce (mesmo padrão dos `documentos`); por ora, varrer é simples e certo.
 */
object FirestoreGaleria {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /** Quantas conversas varrer de uma vez — as mais mexidas primeiro. */
    private const val MAX_CONVERSAS = 100

    /**
     * Recolhe todas as imagens do usuário, da mais nova pra mais velha. Uma leitura por conversa,
     * disparadas em paralelo; falha de uma conversa não derruba as outras (só some daquela).
     */
    suspend fun carregarImagens(uid: String): List<ImagemGaleria> {
        val convSnap = db.collection("users").document(uid).collection("conversations")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(MAX_CONVERSAS.toLong())
            .get()
            .await()

        val porConversa = coroutineScope {
            convSnap.documents.map { conv ->
                async(Dispatchers.IO) { imagensDaConversa(uid, conv) }
            }.awaitAll()
        }

        return porConversa
            .flatten()
            .distinctBy { it.url }
            .sortedByDescending { it.createdAtMs }
    }

    private suspend fun imagensDaConversa(uid: String, conv: DocumentSnapshot): List<ImagemGaleria> {
        val conversaId = conv.id
        val titulo = (conv.getString("title") ?: conv.getString("preview")).orEmpty().trim()

        val msgs = runCatching {
            conv.reference.collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
        }.getOrNull() ?: return emptyList()

        val out = ArrayList<ImagemGaleria>()
        for (doc in msgs.documents) {
            val createdAtMs = timestampMs(doc.get("createdAt")) ?: 0L

            // As que a Luna desenhou.
            (doc.get("imagens") as? List<*>)?.forEach { item ->
                val m = item as? Map<*, *> ?: return@forEach
                val url = (m["url"] as? String)?.takeIf { ehRemota(it) } ?: return@forEach
                out += ImagemGaleria(
                    url = url,
                    prompt = (m["prompt"] as? String).orEmpty(),
                    origem = "luna",
                    conversaId = conversaId,
                    conversaTitulo = titulo,
                    createdAtMs = createdAtMs,
                )
            }

            // As que você anexou (só imagem, e só as que já viraram URL remota do Storage —
            // `content://`/`file://` de outra sessão não carregam aqui).
            (doc.get("attachments") as? List<*>)?.forEach { item ->
                val m = item as? Map<*, *> ?: return@forEach
                if ((m["kind"] as? String) != "image") return@forEach
                val url = (m["uri"] as? String)?.takeIf { ehRemota(it) } ?: return@forEach
                out += ImagemGaleria(
                    url = url,
                    prompt = (m["name"] as? String).orEmpty(),
                    origem = "user",
                    conversaId = conversaId,
                    conversaTitulo = titulo,
                    createdAtMs = createdAtMs,
                )
            }
        }
        return out
    }

    private fun ehRemota(url: String): Boolean = url.startsWith("https://", ignoreCase = true)

    private fun timestampMs(value: Any?): Long? = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> null
    }
}

/**
 * Uma imagem na galeria — venha da Luna ou de você. `id` é derivado da URL (chave estável pro grid e
 * pro pager). `prompt` é a legenda: o pedido que gerou a imagem (Luna) ou o nome do arquivo (anexo).
 */
data class ImagemGaleria(
    val url: String,
    val prompt: String,
    /** Quem trouxe a imagem: "luna" | "user". */
    val origem: String,
    val conversaId: String,
    val conversaTitulo: String,
    val createdAtMs: Long,
) {
    val id: String get() = url
    val ehLuna: Boolean get() = origem == "luna"
}
