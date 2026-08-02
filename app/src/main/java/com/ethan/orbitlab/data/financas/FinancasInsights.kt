package com.ethan.orbitlab.data.financas

/**
 * Insights locais pro card da Luna no dashboard (F3).
 * Gerados a partir dos números — não são copy fixa.
 * Quando `resumo_financeiro` existir no core, o card pode passar a consumir a mesma lógica.
 */
data class InsightFinancas(
    val titulo: String,
    val corpo: String,
)

fun gerarInsightFinancas(
    lancamentosMes: List<Lancamento>,
    lancamentosMesAnterior: List<Lancamento>,
    recorrentes: List<Recorrente>,
    metaGastoCentavos: Long,
): InsightFinancas {
    val resumo = resumoDoPeriodo(lancamentosMes)
    val resumoAnt = resumoDoPeriodo(lancamentosMesAnterior)
    val fixos = resumoRecorrentes(recorrentes.filter { it.ativo })

    // 1) Comparativo com mês anterior
    if (resumoAnt.saiuCentavos > 0) {
        val diff = resumoAnt.saiuCentavos - resumo.saiuCentavos
        if (diff > 5_00) { // > R$ 5
            return InsightFinancas(
                titulo = "Gastou ${formatarReais(diff)} a menos que o mês passado",
                corpo = if (resumo.saiuCentavos <= metaGastoCentavos) {
                    "Nesse ritmo a meta segue no azul. Mantém?"
                } else {
                    "Ainda está acima da meta — mas a curva melhorou."
                },
            )
        }
        if (diff < -5_00) {
            return InsightFinancas(
                titulo = "Gastou ${formatarReais(-diff)} a mais que o mês passado",
                corpo = "Vale olhar o Extrato e ver o que inchou.",
            )
        }
    }

    // 2) Fixos vs o que entra
    if (fixos.entramCentavos > 0) {
        val pct = ((fixos.saemCentavos * 100.0) / fixos.entramCentavos).toInt()
        if (pct >= 30) {
            return InsightFinancas(
                titulo = "Fixos comem $pct% do que entra",
                corpo = "Sobra livre: ${formatarReais(fixos.sobraLivreCentavos)} pra o resto do mês.",
            )
        }
    }

    // 3) Meta
    if (metaGastoCentavos > 0) {
        val restante = metaGastoCentavos - resumo.saiuCentavos
        return if (restante >= 0) {
            InsightFinancas(
                titulo = "Dentro da meta do mês",
                corpo = "Ainda dá pra gastar ${formatarReais(restante)} sem furar.",
            )
        } else {
            InsightFinancas(
                titulo = "Meta furada em ${formatarReais(-restante)}",
                corpo = "Sem drama — registra o que falta e a gente recalcula.",
            )
        }
    }

    return InsightFinancas(
        titulo = "Olhando a grana com você",
        corpo = if (resumo.saiuCentavos == 0L && resumo.entrouCentavos == 0L) {
            "Ainda sem movimento neste mês. Registra o primeiro lançamento?"
        } else {
            "Entrou ${formatarReais(resumo.entrouCentavos)}, saiu ${formatarReais(resumo.saiuCentavos)}."
        },
    )
}

/**
 * Ofensiva leve (sem engine F4): dias consecutivos, de ontem pra trás,
 * em que o gasto do dia ficou ≤ meta diária.
 */
fun diasNoOrcamento(
    lancamentos: List<Lancamento>,
    metaMesCentavos: Long,
    agoraMs: Long = System.currentTimeMillis(),
): Int {
    if (metaMesCentavos <= 0L) return 0
    val c = java.util.Calendar.getInstance()
    c.timeInMillis = agoraMs
    val diasNoMes = c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val metaDia = metaMesCentavos / diasNoMes.coerceAtLeast(1)
    val inicioMes = inicioDoMes(agoraMs)

    var streak = 0
    var diaRef = inicioDoDia(agoraMs)
    while (diaRef >= inicioMes) {
        val fim = fimDoDiaExclusivo(diaRef)
        val gasto = lancamentos
            .filter { it.dataMs >= diaRef && it.dataMs < fim && it.tipo == TipoLancamento.SAIDA }
            .sumOf { it.valorCentavos }
        if (gasto > metaDia) return streak
        streak++
        diaRef -= 24L * 60 * 60 * 1000
    }
    return streak
}

fun gastoPorDiaDoMes(
    lancamentos: List<Lancamento>,
    agoraMs: Long = System.currentTimeMillis(),
): List<Pair<Int, Long>> {
    val ini = inicioDoMes(agoraMs)
    val c = java.util.Calendar.getInstance()
    c.timeInMillis = agoraMs
    val hoje = c.get(java.util.Calendar.DAY_OF_MONTH)
    val saidas = lancamentos.filter {
        it.tipo == TipoLancamento.SAIDA && it.dataMs >= ini && chaveMes(it.dataMs) == chaveMes(agoraMs)
    }
    return (1..hoje).map { dia ->
        val diaMs = dataDoRecorrenteNoMes(dia, agoraMs)
        val fim = fimDoDiaExclusivo(diaMs)
        val gasto = saidas
            .filter { it.dataMs >= diaMs && it.dataMs < fim }
            .sumOf { it.valorCentavos }
        dia to gasto
    }
}
