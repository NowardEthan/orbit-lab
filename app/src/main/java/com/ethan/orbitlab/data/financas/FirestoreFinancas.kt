package com.ethan.orbitlab.data.financas

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Finanças — carteiras + lançamentos.
 *
 * Molde igual a [com.ethan.orbitlab.data.firebase.FirestoreDocumentos]: `object` +
 * `subscribe*` com [ListenerRegistration] + `suspend` pras escritas. Sempre grava `id` no doc.
 * Valores em centavos ([Long]).
 */
object FirestoreFinancas {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private fun carteirasCol(uid: String) =
        db.collection("users").document(uid).collection("carteiras")

    private fun lancamentosCol(uid: String) =
        db.collection("users").document(uid).collection("lancamentos")

    private fun recorrentesCol(uid: String) =
        db.collection("users").document(uid).collection("recorrentes")

    private fun transferenciasCol(uid: String) =
        db.collection("users").document(uid).collection("transferencias")

    private fun metasCol(uid: String) =
        db.collection("users").document(uid).collection("metasFinanceiras")

    /**
     * Escuta todas as carteiras do usuário. Filtra arquivadas no cliente (evita índice).
     * Ordena: crédito primeiro, depois apelido.
     */
    fun subscribeCarteiras(
        uid: String,
        incluirArquivadas: Boolean = false,
        onChange: (List<Carteira>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return carteirasCol(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                onError(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents.orEmpty()
                .mapNotNull { toCarteira(it) }
                .filter { incluirArquivadas || !it.arquivada }
                .sortedWith(
                    compareBy<Carteira> {
                        when (it.tipo) {
                            TipoCarteira.CARTAO_CREDITO -> 0
                            TipoCarteira.CONTA_DEBITO -> 1
                            else -> 2
                        }
                    }.thenBy { it.apelido.lowercase() },
                )
            onChange(lista)
        }
    }

    /**
     * Cria carteira. Gera id no cliente, grava o campo `id` no doc (padrão do lab).
     * Devolve o id novo.
     */
    suspend fun criarCarteira(uid: String, rascunho: CarteiraRascunho): String {
        val ref = carteirasCol(uid).document()
        val agora = System.currentTimeMillis()
        val data = rascunhoParaMap(rascunho, id = ref.id, agora = agora, createdAtMs = agora)
        ref.set(data).await()
        return ref.id
    }

    /** Atualiza campos editáveis; preserva `createdAt` e `id`. */
    suspend fun atualizarCarteira(uid: String, id: String, rascunho: CarteiraRascunho) {
        val agora = System.currentTimeMillis()
        val ref = carteirasCol(uid).document(id)
        val existente = ref.get().await()
        val createdAt = timestampMs(existente.get("createdAt")) ?: agora
        val data = rascunhoParaMap(rascunho, id = id, agora = agora, createdAtMs = createdAt)
        ref.set(data, SetOptions.merge()).await()
    }

    /** Soft-delete: marca `arquivada = true` (some da lista, não apaga o histórico futuro). */
    suspend fun arquivarCarteira(uid: String, id: String) {
        carteirasCol(uid).document(id).update(
            mapOf(
                "arquivada" to true,
                "updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    /**
     * Saldo da carteira com lançamentos ± transferências.
     * Preferir a função top-level [com.ethan.orbitlab.data.financas.saldoDerivado] na UI.
     */
    fun saldoDerivado(
        carteira: Carteira,
        lancamentos: List<Lancamento> = emptyList(),
        transferencias: List<Transferencia> = emptyList(),
    ): Long = com.ethan.orbitlab.data.financas.saldoDerivado(carteira, lancamentos, transferencias)

    // ── Lançamentos ──────────────────────────────────────────────────────────

    /**
     * Escuta todos os lançamentos. Filtra período no cliente (evita índice composto).
     * Ordena por data desc.
     */
    fun subscribeLancamentos(
        uid: String,
        onChange: (List<Lancamento>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return lancamentosCol(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                onError(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents.orEmpty()
                .mapNotNull { toLancamento(it) }
                .sortedByDescending { it.dataMs }
            onChange(lista)
        }
    }

    suspend fun criarLancamento(uid: String, rascunho: LancamentoRascunho): String {
        val ref = lancamentosCol(uid).document()
        val agora = System.currentTimeMillis()
        ref.set(rascunhoParaMapLancamento(rascunho, id = ref.id, agora = agora, createdAtMs = agora))
            .await()
        return ref.id
    }

    suspend fun atualizarLancamento(uid: String, id: String, rascunho: LancamentoRascunho) {
        val agora = System.currentTimeMillis()
        val ref = lancamentosCol(uid).document(id)
        val existente = ref.get().await()
        val createdAt = timestampMs(existente.get("createdAt")) ?: agora
        ref.set(
            rascunhoParaMapLancamento(rascunho, id = id, agora = agora, createdAtMs = createdAt),
            SetOptions.merge(),
        ).await()
    }

    suspend fun apagarLancamento(uid: String, id: String) {
        lancamentosCol(uid).document(id).delete().await()
    }

    // ── Recorrentes ──────────────────────────────────────────────────────────

    fun subscribeRecorrentes(
        uid: String,
        onChange: (List<Recorrente>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return recorrentesCol(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                onError(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents.orEmpty()
                .mapNotNull { toRecorrente(it) }
                .sortedWith(
                    compareBy<Recorrente> { it.diaDoMes }
                        .thenBy { it.apelido.lowercase() },
                )
            onChange(lista)
        }
    }

    suspend fun criarRecorrente(uid: String, rascunho: RecorrenteRascunho): String {
        val ref = recorrentesCol(uid).document()
        val agora = System.currentTimeMillis()
        ref.set(rascunhoParaMapRecorrente(rascunho, id = ref.id, agora = agora, createdAtMs = agora))
            .await()
        return ref.id
    }

    suspend fun atualizarRecorrente(uid: String, id: String, rascunho: RecorrenteRascunho) {
        val agora = System.currentTimeMillis()
        val ref = recorrentesCol(uid).document(id)
        val existente = ref.get().await()
        val createdAt = timestampMs(existente.get("createdAt")) ?: agora
        ref.set(
            rascunhoParaMapRecorrente(rascunho, id = id, agora = agora, createdAtMs = createdAt),
            SetOptions.merge(),
        ).await()
    }

    /** Soft: desativa. Continua na lista (filtrável), para de gerar. */
    suspend fun desativarRecorrente(uid: String, id: String) {
        recorrentesCol(uid).document(id).update(
            mapOf(
                "ativo" to false,
                "updatedAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    suspend fun apagarRecorrente(uid: String, id: String) {
        recorrentesCol(uid).document(id).delete().await()
    }

    // ── Transferências ───────────────────────────────────────────────────────

    fun subscribeTransferencias(
        uid: String,
        onChange: (List<Transferencia>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return transferenciasCol(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                onError(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents.orEmpty()
                .mapNotNull { toTransferencia(it) }
                .sortedByDescending { it.dataMs }
            onChange(lista)
        }
    }

    suspend fun criarTransferencia(uid: String, rascunho: TransferenciaRascunho): String {
        require(rascunho.valido()) { "Transferência inválida" }
        val ref = transferenciasCol(uid).document()
        val agora = System.currentTimeMillis()
        ref.set(
            rascunhoParaMapTransferencia(rascunho, id = ref.id, agora = agora, createdAtMs = agora),
        ).await()
        return ref.id
    }

    suspend fun apagarTransferencia(uid: String, id: String) {
        transferenciasCol(uid).document(id).delete().await()
    }

    // ── Metas financeiras ────────────────────────────────────────────────────

    fun subscribeMetas(
        uid: String,
        onChange: (List<MetaFinanceira>) -> Unit,
        onError: (Exception) -> Unit = {},
    ): ListenerRegistration {
        return metasCol(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                onError(err)
                return@addSnapshotListener
            }
            val lista = snap?.documents.orEmpty()
                .mapNotNull { toMeta(it) }
                .sortedBy { it.apelido.lowercase() }
            onChange(lista)
        }
    }

    suspend fun criarMeta(uid: String, rascunho: MetaFinanceiraRascunho): String {
        require(rascunho.valido()) { "Meta inválida" }
        val ref = metasCol(uid).document()
        val agora = System.currentTimeMillis()
        ref.set(rascunhoParaMapMeta(rascunho, id = ref.id, agora = agora, createdAtMs = agora))
            .await()
        return ref.id
    }

    suspend fun atualizarMeta(uid: String, id: String, rascunho: MetaFinanceiraRascunho) {
        require(rascunho.valido()) { "Meta inválida" }
        val agora = System.currentTimeMillis()
        val ref = metasCol(uid).document(id)
        val existente = ref.get().await()
        val createdAt = timestampMs(existente.get("createdAt")) ?: agora
        ref.set(
            rascunhoParaMapMeta(rascunho, id = id, agora = agora, createdAtMs = createdAt),
            SetOptions.merge(),
        ).await()
    }

    suspend fun apagarMeta(uid: String, id: String) {
        metasCol(uid).document(id).delete().await()
    }

    private fun rascunhoParaMapMeta(
        r: MetaFinanceiraRascunho,
        id: String,
        agora: Long,
        createdAtMs: Long,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "apelido" to r.apelido.trim(),
        "tipo" to r.tipo,
        "alvoCentavos" to r.alvoCentavos,
        "atualCentavos" to r.atualCentavos,
        "categoria" to r.categoria?.takeIf { it.isNotBlank() },
        "ativa" to r.ativa,
        "createdAt" to createdAtMs,
        "updatedAt" to agora,
    )

    private fun toMeta(doc: DocumentSnapshot): MetaFinanceira? {
        val data = doc.data ?: return null
        val apelido = (data["apelido"] as? String)?.trim().orEmpty()
        if (apelido.isBlank()) return null
        val tipo = (data["tipo"] as? String)?.takeIf {
            it == TipoMeta.RESERVA || it == TipoMeta.CORTE || it == TipoMeta.GASTO_MES
        } ?: return null
        val alvo = (data["alvoCentavos"] as? Number)?.toLong() ?: return null
        if (alvo < 0) return null
        return MetaFinanceira(
            id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id,
            apelido = apelido,
            tipo = tipo,
            alvoCentavos = alvo,
            atualCentavos = (data["atualCentavos"] as? Number)?.toLong() ?: 0L,
            categoria = (data["categoria"] as? String)?.takeIf { it.isNotBlank() },
            ativa = data["ativa"] as? Boolean ?: true,
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            updatedAtMs = timestampMs(data["updatedAt"]) ?: 0L,
        )
    }

    private fun rascunhoParaMapTransferencia(
        r: TransferenciaRascunho,
        id: String,
        agora: Long,
        createdAtMs: Long,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "deCarteiraId" to r.deCarteiraId,
        "paraCarteiraId" to r.paraCarteiraId,
        "valorCentavos" to r.valorCentavos,
        "data" to r.dataMs,
        "motivo" to r.motivo,
        "nota" to r.nota?.trim()?.takeIf { it.isNotEmpty() },
        "createdAt" to createdAtMs,
        "updatedAt" to agora,
    )

    private fun toTransferencia(doc: DocumentSnapshot): Transferencia? {
        val data = doc.data ?: return null
        val de = (data["deCarteiraId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val para = (data["paraCarteiraId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        if (de == para) return null
        val valor = (data["valorCentavos"] as? Number)?.toLong() ?: return null
        if (valor <= 0) return null
        val dataMs = timestampMs(data["data"]) ?: return null
        return Transferencia(
            id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id,
            deCarteiraId = de,
            paraCarteiraId = para,
            valorCentavos = valor,
            dataMs = dataMs,
            motivo = (data["motivo"] as? String)?.takeIf { it.isNotBlank() },
            nota = (data["nota"] as? String)?.takeIf { it.isNotBlank() },
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            updatedAtMs = timestampMs(data["updatedAt"]) ?: 0L,
        )
    }

    private fun rascunhoParaMapRecorrente(
        r: RecorrenteRascunho,
        id: String,
        agora: Long,
        createdAtMs: Long,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tipo" to r.tipo,
        "valorCentavos" to r.valorCentavos,
        "diaDoMes" to r.diaDoMes,
        "categoria" to r.categoria,
        "carteiraId" to r.carteiraId,
        "apelido" to r.apelido.trim(),
        "variavel" to r.variavel,
        "ativo" to r.ativo,
        "createdAt" to createdAtMs,
        "updatedAt" to agora,
    )

    private fun toRecorrente(doc: DocumentSnapshot): Recorrente? {
        val data = doc.data ?: return null
        val apelido = (data["apelido"] as? String)?.trim().orEmpty()
        if (apelido.isBlank()) return null
        val tipo = (data["tipo"] as? String)?.takeIf {
            it == TipoLancamento.ENTRADA || it == TipoLancamento.SAIDA
        } ?: return null
        val valor = (data["valorCentavos"] as? Number)?.toLong() ?: return null
        if (valor < 0) return null
        val dia = (data["diaDoMes"] as? Number)?.toInt() ?: return null
        if (dia !in 1..31) return null
        val carteiraId = (data["carteiraId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        return Recorrente(
            id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id,
            tipo = tipo,
            valorCentavos = valor,
            diaDoMes = dia,
            categoria = (data["categoria"] as? String)?.takeIf { it.isNotBlank() }
                ?: CategoriasFinanca.outros.id,
            carteiraId = carteiraId,
            apelido = apelido,
            variavel = data["variavel"] as? Boolean ?: false,
            ativo = data["ativo"] as? Boolean ?: true,
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            updatedAtMs = timestampMs(data["updatedAt"]) ?: 0L,
        )
    }

    private fun rascunhoParaMapLancamento(
        r: LancamentoRascunho,
        id: String,
        agora: Long,
        createdAtMs: Long,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "tipo" to r.tipo,
        "valorCentavos" to r.valorCentavos,
        "data" to r.dataMs,
        "descricao" to r.descricao.trim(),
        "categoria" to r.categoria,
        "carteiraId" to r.carteiraId,
        "recorrenteId" to r.recorrenteId,
        "origem" to r.origem,
        "capturaRaw" to r.capturaRaw,
        "pago" to r.pago,
        "createdAt" to createdAtMs,
        "updatedAt" to agora,
    )

    private fun toLancamento(doc: DocumentSnapshot): Lancamento? {
        val data = doc.data ?: return null
        val tipo = (data["tipo"] as? String)?.takeIf {
            it == TipoLancamento.ENTRADA || it == TipoLancamento.SAIDA
        } ?: return null
        val valor = (data["valorCentavos"] as? Number)?.toLong() ?: return null
        if (valor < 0) return null
        val carteiraId = (data["carteiraId"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val dataMs = timestampMs(data["data"]) ?: return null
        return Lancamento(
            id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id,
            tipo = tipo,
            valorCentavos = valor,
            dataMs = dataMs,
            descricao = (data["descricao"] as? String).orEmpty(),
            categoria = (data["categoria"] as? String)?.takeIf { it.isNotBlank() }
                ?: CategoriasFinanca.outros.id,
            carteiraId = carteiraId,
            recorrenteId = (data["recorrenteId"] as? String)?.takeIf { it.isNotBlank() },
            origem = (data["origem"] as? String)?.takeIf { it.isNotBlank() }
                ?: OrigemLancamento.MANUAL,
            capturaRaw = (data["capturaRaw"] as? String)?.takeIf { it.isNotBlank() },
            pago = data["pago"] as? Boolean ?: true,
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            updatedAtMs = timestampMs(data["updatedAt"]) ?: 0L,
        )
    }

    private fun rascunhoParaMap(
        r: CarteiraRascunho,
        id: String,
        agora: Long,
        createdAtMs: Long,
    ): Map<String, Any?> {
        val base = mutableMapOf<String, Any?>(
            "id" to id,
            "tipo" to r.tipo,
            "banco" to r.banco?.trim()?.takeIf { it.isNotEmpty() },
            "apelido" to r.apelido.trim(),
            "cor" to r.cor,
            "ultimos4" to r.ultimos4?.trim()?.takeIf { it.isNotEmpty() },
            "saldoInicialCentavos" to r.saldoInicialCentavos,
            "arquivada" to false,
            "createdAt" to createdAtMs,
            "updatedAt" to agora,
        )
        if (r.tipo == TipoCarteira.CARTAO_CREDITO) {
            base["limiteCentavos"] = r.limiteCentavos
            base["fechamentoDia"] = r.fechamentoDia
            base["vencimentoDia"] = r.vencimentoDia
        } else {
            // Limpa campos de crédito se o tipo mudou.
            base["limiteCentavos"] = null
            base["fechamentoDia"] = null
            base["vencimentoDia"] = null
        }
        return base
    }

    private fun toCarteira(doc: DocumentSnapshot): Carteira? {
        val data = doc.data ?: return null
        val apelido = (data["apelido"] as? String)?.trim().orEmpty()
        if (apelido.isBlank()) return null
        val tipo = (data["tipo"] as? String)?.takeIf { it.isNotBlank() }
            ?: TipoCarteira.CONTA_DEBITO
        return Carteira(
            id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: doc.id,
            tipo = tipo,
            banco = (data["banco"] as? String)?.takeIf { it.isNotBlank() },
            apelido = apelido,
            cor = (data["cor"] as? String)?.takeIf { it in CorCarteira.todas }
                ?: CorCarteira.GRAFITE,
            ultimos4 = (data["ultimos4"] as? String)?.takeIf { it.isNotBlank() },
            limiteCentavos = (data["limiteCentavos"] as? Number)?.toLong(),
            fechamentoDia = (data["fechamentoDia"] as? Number)?.toInt(),
            vencimentoDia = (data["vencimentoDia"] as? Number)?.toInt(),
            saldoInicialCentavos = (data["saldoInicialCentavos"] as? Number)?.toLong() ?: 0L,
            arquivada = data["arquivada"] as? Boolean ?: false,
            createdAtMs = timestampMs(data["createdAt"]) ?: 0L,
            updatedAtMs = timestampMs(data["updatedAt"]) ?: 0L,
        )
    }

    private fun timestampMs(value: Any?): Long? = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> null
    }
}

/** Entrada do formulário — ainda sem id (criação) ou com id implícito na atualização. */
data class CarteiraRascunho(
    val tipo: String,
    val banco: String? = null,
    val apelido: String,
    val cor: String = CorCarteira.GRAFITE,
    val ultimos4: String? = null,
    val limiteCentavos: Long? = null,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val saldoInicialCentavos: Long = 0,
) {
    fun valido(): Boolean {
        if (apelido.trim().isBlank()) return false
        if (tipo !in listOf(
                TipoCarteira.CONTA_DEBITO,
                TipoCarteira.CARTAO_CREDITO,
                TipoCarteira.DINHEIRO,
            )
        ) return false
        if (tipo == TipoCarteira.CARTAO_CREDITO) {
            val f = fechamentoDia
            val v = vencimentoDia
            if (f != null && f !in 1..31) return false
            if (v != null && v !in 1..31) return false
        }
        return saldoInicialCentavos >= 0
    }
}

/** Entrada do formulário de lançamento. */
data class LancamentoRascunho(
    val tipo: String,
    val valorCentavos: Long,
    val dataMs: Long,
    val descricao: String,
    val categoria: String,
    val carteiraId: String,
    val recorrenteId: String? = null,
    val origem: String = OrigemLancamento.MANUAL,
    val capturaRaw: String? = null,
    val pago: Boolean = true,
) {
    fun valido(): Boolean {
        if (tipo != TipoLancamento.ENTRADA && tipo != TipoLancamento.SAIDA) return false
        if (valorCentavos <= 0) return false
        if (carteiraId.isBlank()) return false
        if (categoria.isBlank()) return false
        return true
    }
}

/** Entrada do formulário de meta. */
data class MetaFinanceiraRascunho(
    val apelido: String,
    val tipo: String,
    val alvoCentavos: Long,
    val atualCentavos: Long = 0,
    val categoria: String? = null,
    val ativa: Boolean = true,
) {
    fun valido(): Boolean {
        if (apelido.trim().isBlank()) return false
        if (tipo !in listOf(TipoMeta.RESERVA, TipoMeta.CORTE, TipoMeta.GASTO_MES)) return false
        if (alvoCentavos <= 0) return false
        if (atualCentavos < 0) return false
        if (tipo == TipoMeta.CORTE && categoria.isNullOrBlank()) return false
        return true
    }
}

/** Entrada do formulário de transferência. */
data class TransferenciaRascunho(
    val deCarteiraId: String,
    val paraCarteiraId: String,
    val valorCentavos: Long,
    val dataMs: Long,
    val motivo: String? = null,
    val nota: String? = null,
) {
    fun valido(): Boolean {
        if (deCarteiraId.isBlank() || paraCarteiraId.isBlank()) return false
        if (deCarteiraId == paraCarteiraId) return false
        if (valorCentavos <= 0) return false
        return true
    }
}

/** Entrada do formulário de recorrente. */
data class RecorrenteRascunho(
    val tipo: String,
    val valorCentavos: Long,
    val diaDoMes: Int,
    val categoria: String,
    val carteiraId: String,
    val apelido: String,
    val variavel: Boolean = false,
    val ativo: Boolean = true,
) {
    fun valido(): Boolean {
        if (tipo != TipoLancamento.ENTRADA && tipo != TipoLancamento.SAIDA) return false
        if (valorCentavos <= 0) return false
        if (diaDoMes !in 1..31) return false
        if (carteiraId.isBlank()) return false
        if (apelido.trim().isBlank()) return false
        if (categoria.isBlank()) return false
        return true
    }
}
