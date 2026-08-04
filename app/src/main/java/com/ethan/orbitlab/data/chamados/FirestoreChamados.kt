package com.ethan.orbitlab.data.chamados

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Acesso Firestore dos chamados ("Fala com o Ethan").
 *
 * Coleção de topo `chamados` (lida por dono + admin), conversa em `chamados/{id}/mensagens`.
 * Molde igual a [com.ethan.orbitlab.data.financas.FirestoreFinancas]: `object` + `subscribe*`
 * com [ListenerRegistration], `suspend` pras escritas, sempre grava `id` no doc, extração
 * manual (nada de `.toObject()` — seguro sob R8). Ordenação no cliente pra evitar índice composto.
 */
object FirestoreChamados {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private fun chamadosCol() = db.collection("chamados")

    private fun mensagensCol(chamadoId: String) =
        chamadosCol().document(chamadoId).collection("mensagens")

    /**
     * Gera o id do chamado ANTES de criar o doc. A UI precisa dele pra montar o caminho do
     * Storage (`chamados/{ownerUid}/{chamadoId}/...`) e subir os anexos antes de `abrir`.
     */
    fun novoChamadoId(): String = chamadosCol().document().id

    /** Preview de capa quando a mensagem é só anexo (sem texto): "📷 Imagem", "🎬 Vídeo"… */
    private fun previewCapa(texto: String, anexos: List<AnexoChamado>): String {
        if (texto.isNotBlank()) return texto
        val a = anexos.firstOrNull() ?: return texto
        val extra = if (anexos.size > 1) " +${anexos.size - 1}" else ""
        return when (a.kind) {
            "image" -> "📷 Imagem$extra"
            "video" -> "🎬 Vídeo$extra"
            else -> "📎 Anexo$extra"
        }
    }

    // ── Lista do usuário / caixa do admin ────────────────────────────────────

    /** Escuta os chamados do próprio usuário. Ordena por atividade (updatedAt desc) no cliente. */
    fun subscribeMeus(
        uid: String,
        onChange: (List<Chamado>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return chamadosCol().whereEqualTo("uid", uid).addSnapshotListener { snap, err ->
            if (err != null) {
                onError(err)
                return@addSnapshotListener
            }
            onChange(
                snap?.documents.orEmpty()
                    .mapNotNull { toChamado(it) }
                    .sortedByDescending { it.updatedAtMs },
            )
        }
    }

    /** Escuta TODOS os chamados — só pro admin (Ethan). Não abrir pra usuário comum. */
    fun subscribeTodos(
        onChange: (List<Chamado>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return chamadosCol().addSnapshotListener { snap, err ->
            if (err != null) {
                onError(err)
                return@addSnapshotListener
            }
            onChange(
                snap?.documents.orEmpty()
                    .mapNotNull { toChamado(it) }
                    .sortedByDescending { it.updatedAtMs },
            )
        }
    }

    // ── Abertura + conversa ──────────────────────────────────────────────────

    /**
     * Abre um chamado: cria a capa + a primeira mensagem (a do usuário) numa tacada.
     * Devolve o id novo. `naoLidoAdmin=true` já acende a caixa do Ethan.
     */
    suspend fun abrir(
        uid: String,
        nomeUsuario: String,
        rascunho: ChamadoRascunho,
        contextoApp: String?,
        anexos: List<AnexoChamado> = emptyList(),
        chamadoId: String? = null,
    ): String {
        require(rascunho.valido()) { "Chamado inválido" }
        val ref = if (chamadoId != null) chamadosCol().document(chamadoId) else chamadosCol().document()
        val agora = System.currentTimeMillis()
        val texto = rascunho.mensagem.trim()
        ref.set(
            mapOf(
                "id" to ref.id,
                "uid" to uid,
                "nomeUsuario" to nomeUsuario.trim().ifBlank { "Alguém" },
                "tipo" to rascunho.tipo,
                "titulo" to rascunho.titulo.trim(),
                "status" to StatusChamado.ABERTO,
                "contextoApp" to contextoApp,
                "ultimaMensagem" to previewCapa(texto, anexos),
                "ultimoAutor" to AutorChamado.USUARIO,
                "naoLidoUsuario" to false,
                "naoLidoAdmin" to true,
                "createdAt" to agora,
                "updatedAt" to agora,
            ),
        ).await()
        val msgRef = mensagensCol(ref.id).document()
        msgRef.set(
            mapOf(
                "id" to msgRef.id,
                "autor" to AutorChamado.USUARIO,
                "texto" to texto,
                "anexos" to anexos.map { anexoToMap(it) },
                "createdAt" to agora,
            ),
        ).await()
        return ref.id
    }

    /** Escuta a conversa de um chamado, em ordem cronológica (mais antiga primeiro). */
    fun subscribeMensagens(
        chamadoId: String,
        onChange: (List<MensagemChamado>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return mensagensCol(chamadoId).addSnapshotListener { snap, err ->
            if (err != null) {
                onError(err)
                return@addSnapshotListener
            }
            onChange(
                snap?.documents.orEmpty()
                    .mapNotNull { toMensagem(it) }
                    .sortedBy { it.createdAtMs },
            )
        }
    }

    /**
     * Manda uma mensagem e atualiza a capa (preview, autor, não-lido, updatedAt).
     * Resposta do Ethan move o status pra "Respondido" (a menos que já estivesse resolvido).
     */
    suspend fun enviar(
        chamadoId: String,
        autor: String,
        texto: String,
        anexos: List<AnexoChamado> = emptyList(),
    ) {
        val limpo = texto.trim()
        if (limpo.isEmpty() && anexos.isEmpty()) return
        val agora = System.currentTimeMillis()
        val ehEthan = autor == AutorChamado.ETHAN

        val msgRef = mensagensCol(chamadoId).document()
        msgRef.set(
            mapOf(
                "id" to msgRef.id,
                "autor" to autor,
                "texto" to limpo,
                "anexos" to anexos.map { anexoToMap(it) },
                "createdAt" to agora,
            ),
        ).await()

        val capa = mutableMapOf<String, Any?>(
            "ultimaMensagem" to previewCapa(limpo, anexos),
            "ultimoAutor" to autor,
            "updatedAt" to agora,
            "naoLidoUsuario" to ehEthan,
            "naoLidoAdmin" to !ehEthan,
        )
        if (ehEthan) {
            val atual = chamadosCol().document(chamadoId).get().await().get("status") as? String
            if (atual != StatusChamado.RESOLVIDO) capa["status"] = StatusChamado.RESPONDIDO
        }
        chamadosCol().document(chamadoId).set(capa, SetOptions.merge()).await()
    }

    /** Só o admin move o estado do chamado. */
    suspend fun atualizarStatus(chamadoId: String, status: String) {
        require(status in StatusChamado.todos) { "Status inválido" }
        chamadosCol().document(chamadoId).set(
            mapOf(
                "status" to status,
                "updatedAt" to System.currentTimeMillis(),
            ),
            SetOptions.merge(),
        ).await()
    }

    /** Zera o "não lido" do lado que acabou de abrir o chamado (usuário ou admin). */
    suspend fun marcarLido(chamadoId: String, ehAdmin: Boolean) {
        val campo = if (ehAdmin) "naoLidoAdmin" else "naoLidoUsuario"
        chamadosCol().document(chamadoId)
            .set(mapOf(campo to false), SetOptions.merge())
            .await()
    }

    // ── Mapeadores ────────────────────────────────────────────────────────────

    private fun toChamado(doc: DocumentSnapshot): Chamado? {
        val data = doc.data ?: return null
        val uid = (data["uid"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val titulo = (data["titulo"] as? String)?.trim().orEmpty()
        if (titulo.isBlank()) return null
        return Chamado(
            id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id,
            uid = uid,
            nomeUsuario = (data["nomeUsuario"] as? String)?.takeIf { it.isNotBlank() } ?: "Alguém",
            tipo = (data["tipo"] as? String)?.takeIf { it in TipoChamado.todos } ?: TipoChamado.DUVIDA,
            titulo = titulo,
            status = (data["status"] as? String)?.takeIf { it in StatusChamado.todos }
                ?: StatusChamado.ABERTO,
            contextoApp = (data["contextoApp"] as? String)?.takeIf { it.isNotBlank() },
            ultimaMensagem = (data["ultimaMensagem"] as? String).orEmpty(),
            ultimoAutor = (data["ultimoAutor"] as? String)?.takeIf { it.isNotBlank() }
                ?: AutorChamado.USUARIO,
            naoLidoUsuario = data["naoLidoUsuario"] as? Boolean ?: false,
            naoLidoAdmin = data["naoLidoAdmin"] as? Boolean ?: false,
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            updatedAtMs = timestampMs(data["updatedAt"]) ?: 0L,
        )
    }

    private fun toMensagem(doc: DocumentSnapshot): MensagemChamado? {
        val data = doc.data ?: return null
        val texto = (data["texto"] as? String)?.trim().orEmpty()
        val anexos = (data["anexos"] as? List<*>).orEmpty().mapNotNull { mapToAnexo(it) }
        // Mensagem só-anexo tem texto vazio de propósito — não pode ser descartada.
        if (texto.isBlank() && anexos.isEmpty()) return null
        return MensagemChamado(
            id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id,
            autor = (data["autor"] as? String)?.takeIf {
                it == AutorChamado.USUARIO || it == AutorChamado.ETHAN
            } ?: AutorChamado.USUARIO,
            texto = texto,
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            anexos = anexos,
        )
    }

    private fun anexoToMap(a: AnexoChamado): Map<String, Any?> = mapOf(
        "id" to a.id,
        "kind" to a.kind,
        "url" to a.url,
        "mime" to a.mime,
        "name" to a.name,
        "sizeLabel" to a.sizeLabel,
    )

    private fun mapToAnexo(raw: Any?): AnexoChamado? {
        val m = raw as? Map<*, *> ?: return null
        val url = (m["url"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        return AnexoChamado(
            id = (m["id"] as? String)?.takeIf { it.isNotBlank() } ?: url.hashCode().toString(),
            kind = (m["kind"] as? String)?.takeIf { it.isNotBlank() } ?: "file",
            url = url,
            mime = (m["mime"] as? String).orEmpty(),
            name = (m["name"] as? String)?.takeIf { it.isNotBlank() } ?: "anexo",
            sizeLabel = (m["sizeLabel"] as? String).orEmpty(),
        )
    }

    private fun timestampMs(value: Any?): Long? = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> null
    }
}
