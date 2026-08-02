package com.ethan.orbitlab.data.financas

/**
 * Gera lançamentos do mês a partir dos recorrentes ativos — idempotente.
 *
 * Regra: se o dia do recorrente **já chegou** neste mês e ainda não existe
 * lançamento com aquele `recorrenteId` no mês, cria um.
 * Saídas nascem `pago = false` (conta a pagar / aviso de vencimento).
 * Entradas nascem `pago = true` (já contabilizadas no fluxo).
 */
object GeracaoRecorrentes {
    private var gerando = false

    suspend fun garantirMesAtual(
        uid: String,
        recorrentes: List<Recorrente>,
        lancamentos: List<Lancamento>,
        agoraMs: Long = System.currentTimeMillis(),
    ) {
        if (gerando) return
        val ativos = recorrentes.filter { it.ativo }
        if (ativos.isEmpty()) return

        gerando = true
        try {
            val mes = chaveMes(agoraMs)
            val hoje = inicioDoDia(agoraMs)
            val jaGerados = lancamentos
                .filter { it.recorrenteId != null && chaveMes(it.dataMs) == mes }
                .mapNotNull { it.recorrenteId }
                .toSet()

            for (r in ativos) {
                if (r.id in jaGerados) continue
                val dataPrevista = dataDoRecorrenteNoMes(r.diaDoMes, agoraMs)
                if (dataPrevista > hoje) continue // ainda não chegou o dia

                FirestoreFinancas.criarLancamento(
                    uid = uid,
                    rascunho = LancamentoRascunho(
                        tipo = r.tipo,
                        valorCentavos = r.valorCentavos,
                        dataMs = dataPrevista,
                        descricao = r.apelido,
                        categoria = r.categoria,
                        carteiraId = r.carteiraId,
                        recorrenteId = r.id,
                        origem = OrigemLancamento.MANUAL,
                        pago = r.tipo == TipoLancamento.ENTRADA,
                    ),
                )
            }
        } finally {
            gerando = false
        }
    }
}
