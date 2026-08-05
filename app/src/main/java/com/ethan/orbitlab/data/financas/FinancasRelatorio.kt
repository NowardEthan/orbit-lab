package com.ethan.orbitlab.data.financas

/**
 * Relatório e progresso de metas (F7).
 * Open Finance / Pluggy ficam de fora — DLC futura.
 */

data class FatiaCategoria(
    val categoriaId: String,
    val valorCentavos: Long,
    val pct: Float,
)

data class ProgressoMeta(
    val meta: MetaFinanceira,
    val atualCentavos: Long,
    val alvoCentavos: Long,
    val pct: Float,
    val derivado: Boolean,
)

/** Meta de gasto do mês: meta `gasto_mes` ativa, senão heurística dos fixos. */
fun metaGastoMes(
    recorrentes: List<Recorrente>,
    metas: List<MetaFinanceira> = emptyList(),
): Long {
    val própria = metas.firstOrNull {
        it.ativa && it.tipo == TipoMeta.GASTO_MES && it.alvoCentavos > 0
    }
    if (própria != null) return própria.alvoCentavos
    val r = resumoRecorrentes(recorrentes.filter { it.ativo })
    return when {
        r.entramCentavos > 0L -> (r.entramCentavos * 0.70).toLong()
        r.saemCentavos > 0L -> (r.saemCentavos * 1.20).toLong()
        else -> 0L
    }
}

fun gastoPorCategoria(
    lancamentos: List<Lancamento>,
    soSaidas: Boolean = true,
): List<FatiaCategoria> {
    val filtrados = lancamentos.filter {
        if (soSaidas) it.tipo == TipoLancamento.SAIDA else true
    }
    val total = filtrados.sumOf { it.valorCentavos }.coerceAtLeast(1L)
    return filtrados
        .groupBy { it.categoria.ifBlank { CategoriasFinanca.outros.id } }
        .map { (cat, lista) ->
            val v = lista.sumOf { it.valorCentavos }
            FatiaCategoria(
                categoriaId = cat,
                valorCentavos = v,
                pct = v.toFloat() / total.toFloat(),
            )
        }
        .sortedByDescending { it.valorCentavos }
}

fun progressoMeta(
    meta: MetaFinanceira,
    lancamentosMes: List<Lancamento>,
): ProgressoMeta {
    val alvo = meta.alvoCentavos.coerceAtLeast(0L)
    val (atual, derivado) = when (meta.tipo) {
        TipoMeta.RESERVA -> meta.atualCentavos.coerceAtLeast(0L) to false
        TipoMeta.CORTE -> {
            val cat = meta.categoria
            val gasto = lancamentosMes
                .filter {
                    it.tipo == TipoLancamento.SAIDA &&
                        (cat.isNullOrBlank() || it.categoria == cat)
                }
                .sumOf { it.valorCentavos }
            gasto to true
        }
        TipoMeta.GASTO_MES -> {
            val gasto = lancamentosMes
                .filter { it.tipo == TipoLancamento.SAIDA }
                .sumOf { it.valorCentavos }
            gasto to true
        }
        else -> meta.atualCentavos to false
    }
    val pct = if (alvo <= 0L) 0f else (atual.toFloat() / alvo.toFloat()).coerceIn(0f, 1.5f)
    return ProgressoMeta(
        meta = meta,
        atualCentavos = atual,
        alvoCentavos = alvo,
        pct = pct,
        derivado = derivado,
    )
}

fun progressosMetas(
    metas: List<MetaFinanceira>,
    lancamentosMes: List<Lancamento>,
): List<ProgressoMeta> =
    metas.filter { it.ativa }
        .map { progressoMeta(it, lancamentosMes) }
        .sortedByDescending { it.pct }
