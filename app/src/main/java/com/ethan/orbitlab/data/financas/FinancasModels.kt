package com.ethan.orbitlab.data.financas

import androidx.annotation.Keep
import java.text.NumberFormat
import java.util.Locale

/** Tipo de carteira — string estável no Firestore (não enum serializado). */
object TipoCarteira {
    const val CONTA_DEBITO = "conta_debito"
    const val CARTAO_CREDITO = "cartao_credito"
    const val DINHEIRO = "dinheiro"

    fun rotulo(tipo: String): String = when (tipo) {
        CONTA_DEBITO -> "Débito"
        CARTAO_CREDITO -> "Crédito"
        DINHEIRO -> "Dinheiro"
        else -> tipo
    }
}

/**
 * Chaves de cor visual do cartão — gravadas no Firestore; resolvidas pra Brush na UI.
 * Não é hex livre: paleta curta pra não virar bagunça.
 */
object CorCarteira {
    const val GRAFITE = "grafite"
    const val AZUL = "azul"
    const val ROXO = "roxo"
    const val VERDE = "verde"
    const val AMBAR = "ambar"

    val todas = listOf(GRAFITE, AZUL, ROXO, VERDE, AMBAR)
}

/**
 * Carteira / cartão — `users/{uid}/carteiras/{id}`.
 *
 * Saldo derivado = [saldoInicialCentavos] + entradas − saídas ± transferências.
 */
/** Persistido via Map no Firestore — @Keep = cinto se o mapeamento mudar pra POJO. */
@Keep
data class Carteira(
    val id: String,
    val tipo: String,
    val banco: String? = null,
    val apelido: String,
    val cor: String = CorCarteira.GRAFITE,
    val ultimos4: String? = null,
    val limiteCentavos: Long? = null,
    val fechamentoDia: Int? = null,
    val vencimentoDia: Int? = null,
    val saldoInicialCentavos: Long = 0,
    val arquivada: Boolean = false,
    val createdAtMs: Long = 0,
    val updatedAtMs: Long = 0,
)

/** Tipo do lançamento — valor sempre positivo; o tipo diz o sinal. */
object TipoLancamento {
    const val ENTRADA = "entrada"
    const val SAIDA = "saida"
}

/** Origem do lançamento (auditoria). */
object OrigemLancamento {
    const val MANUAL = "manual"
    const val LUNA = "luna"
    const val CAPTURA = "captura"
}

/**
 * Categorias base — id estável no Firestore. Cor visual resolve na UI.
 * Custom do usuário fica pra capítulo futuro.
 */
data class CategoriaFinanca(
    val id: String,
    val rotulo: String,
    val emoji: String,
    /** Chave semântica: verde/vermelho/azul/ambar/roxo/cinza */
    val tom: String,
)

object CategoriasFinanca {
    val alimentacao = CategoriaFinanca("alimentacao", "Alimentação", "🍽️", "azul")
    val transporte = CategoriaFinanca("transporte", "Transporte", "🚌", "ambar")
    val moradia = CategoriaFinanca("moradia", "Moradia", "🏠", "roxo")
    val saude = CategoriaFinanca("saude", "Saúde", "💊", "verde")
    val lazer = CategoriaFinanca("lazer", "Lazer", "🎮", "roxo")
    val contas = CategoriaFinanca("contas", "Contas", "🧾", "ambar")
    val renda = CategoriaFinanca("renda", "Renda", "💰", "verde")
    val outros = CategoriaFinanca("outros", "Outros", "✨", "cinza")

    val todas = listOf(
        alimentacao, transporte, moradia, saude, lazer, contas, renda, outros,
    )

    fun porId(id: String): CategoriaFinanca =
        todas.firstOrNull { it.id == id } ?: outros

    fun padraoPara(tipo: String): CategoriaFinanca =
        if (tipo == TipoLancamento.ENTRADA) renda else alimentacao
}

/**
 * Recorrente mensal — `users/{uid}/recorrentes/{id}`.
 * No dia [diaDoMes] o app gera um [Lancamento] (idempotente por mês).
 */
@Keep
data class Recorrente(
    val id: String,
    val tipo: String,
    val valorCentavos: Long,
    val diaDoMes: Int,
    val categoria: String,
    val carteiraId: String,
    val apelido: String,
    /** Valor é estimativa (ex.: energia ~ R$ 180). */
    val variavel: Boolean = false,
    val ativo: Boolean = true,
    val createdAtMs: Long = 0,
    val updatedAtMs: Long = 0,
)

data class ResumoRecorrentes(
    val entramCentavos: Long,
    val saemCentavos: Long,
) {
    val sobraLivreCentavos: Long get() = entramCentavos - saemCentavos
    val comprometidoCentavos: Long get() = saemCentavos
}

/** Resumo do mês a partir dos recorrentes ativos. */
fun resumoRecorrentes(recorrentes: List<Recorrente>): ResumoRecorrentes {
    var entram = 0L
    var saem = 0L
    for (r in recorrentes) {
        if (!r.ativo) continue
        when (r.tipo) {
            TipoLancamento.ENTRADA -> entram += r.valorCentavos
            TipoLancamento.SAIDA -> saem += r.valorCentavos
        }
    }
    return ResumoRecorrentes(entram, saem)
}

/**
 * Data (início do dia) do recorrente neste mês de [refMs].
 * Se o dia não existe (ex.: 31 em fevereiro), usa o último dia do mês.
 */
fun dataDoRecorrenteNoMes(diaDoMes: Int, refMs: Long = System.currentTimeMillis()): Long {
    val c = calendarioLocal()
    c.timeInMillis = inicioDoMes(refMs)
    val max = c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val dia = diaDoMes.coerceIn(1, max)
    c.set(java.util.Calendar.DAY_OF_MONTH, dia)
    return c.timeInMillis
}

fun diaDoMesDe(ms: Long): Int {
    val c = calendarioLocal()
    c.timeInMillis = ms
    return c.get(java.util.Calendar.DAY_OF_MONTH)
}

/** Chave YYYY-MM pra idempotência da geração. */
fun chaveMes(ms: Long): String {
    val c = calendarioLocal()
    c.timeInMillis = ms
    val y = c.get(java.util.Calendar.YEAR)
    val m = c.get(java.util.Calendar.MONTH) + 1
    return "%04d-%02d".format(y, m)
}

/**
 * Lançamento — `users/{uid}/lancamentos/{id}`.
 * Transferências NÃO passam por aqui (coleção própria em F6).
 */
@Keep
data class Lancamento(
    val id: String,
    val tipo: String,
    val valorCentavos: Long,
    /** Epoch millis do dia do lançamento (preferência: início do dia local). */
    val dataMs: Long,
    val descricao: String,
    val categoria: String,
    val carteiraId: String,
    val recorrenteId: String? = null,
    val origem: String = OrigemLancamento.MANUAL,
    val capturaRaw: String? = null,
    val tags: List<String> = emptyList(),
    val pago: Boolean = true,
    val createdAtMs: Long = 0,
    val updatedAtMs: Long = 0,
)

/** Recorte do extrato: Dia / Semana / Mês. */
enum class PeriodoExtrato { DIA, SEMANA, MES }

data class FaixaPeriodo(val inicioMs: Long, val fimMsExclusivo: Long)

data class ResumoPeriodo(
    val entrouCentavos: Long,
    val saiuCentavos: Long,
) {
    val saldoCentavos: Long get() = entrouCentavos - saiuCentavos
}

data class GrupoDiaExtrato(
    /** Início do dia local. */
    val diaMs: Long,
    val lancamentos: List<Lancamento>,
    val subtotalCentavos: Long,
)

/** Formata centavos pra R$ brasileiro (ex.: 3200 → "R$ 32,00"). */
/**
 * [formatarReais] sem o prefixo "R$" (com ou sem NBSP). Pra usar dentro de linhas de
 * composicao que ja tem "R$" embutido no layout (ou em concat com outros valores).
 */
fun formatarReaisSemPrefixo(centavos: Long): String {
    return formatarReais(centavos)
        .replace("R$", "")
        .replace(" ", "")
        .trim()
}

fun formatarReais(centavos: Long): String {
    val nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    return nf.format(centavos / 100.0)
}

/**
 * Parseia texto livre do usuário ("32", "32,50", "R$ 1.234,56") pra centavos.
 * Devolve null se não der pra ler um valor válido (≥ 0).
 */
fun parsearReaisParaCentavos(texto: String): Long? {
    val limpo = texto
        .trim()
        .replace("R$", "", ignoreCase = true)
        .replace(" ", "")
        .replace(".", "")
        .replace(',', '.')
    if (limpo.isBlank()) return 0L
    val valor = limpo.toDoubleOrNull() ?: return null
    if (valor < 0) return null
    return Math.round(valor * 100.0)
}

// ── Período / tags de data (calendário local) ─────────────────────────────────

private fun calendarioLocal(): java.util.Calendar =
    java.util.Calendar.getInstance(java.util.TimeZone.getDefault(), Locale.forLanguageTag("pt-BR"))

/** Zera hora → 00:00:00.000 do dia de [ms]. */
fun inicioDoDia(ms: Long): Long {
    val c = calendarioLocal()
    c.timeInMillis = ms
    c.set(java.util.Calendar.HOUR_OF_DAY, 0)
    c.set(java.util.Calendar.MINUTE, 0)
    c.set(java.util.Calendar.SECOND, 0)
    c.set(java.util.Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun fimDoDiaExclusivo(ms: Long): Long {
    val c = calendarioLocal()
    c.timeInMillis = inicioDoDia(ms)
    c.add(java.util.Calendar.DAY_OF_MONTH, 1)
    return c.timeInMillis
}

/** Segunda-feira 00:00 da semana que contém [ms]. */
fun inicioDaSemana(ms: Long): Long {
    val c = calendarioLocal()
    c.timeInMillis = inicioDoDia(ms)
    // Calendar: DOMINGO=1 … SÁBADO=7. Queremos segunda como início.
    val dia = c.get(java.util.Calendar.DAY_OF_WEEK)
    val deslocamento = when (dia) {
        java.util.Calendar.MONDAY -> 0
        java.util.Calendar.SUNDAY -> -6
        else -> java.util.Calendar.MONDAY - dia
    }
    c.add(java.util.Calendar.DAY_OF_MONTH, deslocamento)
    return c.timeInMillis
}

fun inicioDoMes(ms: Long): Long {
    val c = calendarioLocal()
    c.timeInMillis = inicioDoDia(ms)
    c.set(java.util.Calendar.DAY_OF_MONTH, 1)
    return c.timeInMillis
}

fun faixaDoPeriodo(periodo: PeriodoExtrato, agoraMs: Long = System.currentTimeMillis()): FaixaPeriodo {
    return when (periodo) {
        PeriodoExtrato.DIA -> {
            val ini = inicioDoDia(agoraMs)
            FaixaPeriodo(ini, fimDoDiaExclusivo(agoraMs))
        }
        PeriodoExtrato.SEMANA -> {
            val ini = inicioDaSemana(agoraMs)
            val c = calendarioLocal()
            c.timeInMillis = ini
            c.add(java.util.Calendar.DAY_OF_MONTH, 7)
            FaixaPeriodo(ini, c.timeInMillis)
        }
        PeriodoExtrato.MES -> {
            val ini = inicioDoMes(agoraMs)
            val c = calendarioLocal()
            c.timeInMillis = ini
            c.add(java.util.Calendar.MONTH, 1)
            FaixaPeriodo(ini, c.timeInMillis)
        }
    }
}

/** Mês relativo a [agoraMs]: 0 = atual, -1 = anterior, etc. */
fun faixaDoMesOffset(offsetMeses: Int, agoraMs: Long = System.currentTimeMillis()): FaixaPeriodo {
    val c = calendarioLocal()
    c.timeInMillis = inicioDoMes(agoraMs)
    c.add(java.util.Calendar.MONTH, offsetMeses)
    val ini = c.timeInMillis
    c.add(java.util.Calendar.MONTH, 1)
    return FaixaPeriodo(ini, c.timeInMillis)
}

/**
 * Dia / semana / mês com offset (0 = período corrente, -1 = anterior…).
 * O extrato é contínuo: o filtro só recorta a linha do tempo, não «zera» a conta.
 */
fun faixaDoPeriodoOffset(
    periodo: PeriodoExtrato,
    offset: Int,
    agoraMs: Long = System.currentTimeMillis(),
): FaixaPeriodo {
    if (offset == 0) return faixaDoPeriodo(periodo, agoraMs)
    val c = calendarioLocal()
    c.timeInMillis = agoraMs
    when (periodo) {
        PeriodoExtrato.DIA -> c.add(java.util.Calendar.DAY_OF_MONTH, offset)
        PeriodoExtrato.SEMANA -> c.add(java.util.Calendar.DAY_OF_MONTH, offset * 7)
        PeriodoExtrato.MES -> c.add(java.util.Calendar.MONTH, offset)
    }
    return faixaDoPeriodo(periodo, c.timeInMillis)
}

/** Rótulo curto da faixa pra UI («31 de jul», «28 jul – 3 ago», «Julho 2026»). */
fun rotuloFaixaExtrato(periodo: PeriodoExtrato, faixa: FaixaPeriodo): String {
    val loc = Locale.forLanguageTag("pt-BR")
    return when (periodo) {
        PeriodoExtrato.DIA ->
            java.text.SimpleDateFormat("d 'de' MMM", loc).format(java.util.Date(faixa.inicioMs))
        PeriodoExtrato.SEMANA -> {
            val a = java.text.SimpleDateFormat("d MMM", loc).format(java.util.Date(faixa.inicioMs))
            val b = java.text.SimpleDateFormat("d MMM", loc)
                .format(java.util.Date(faixa.fimMsExclusivo - 1L))
            "$a – $b"
        }
        PeriodoExtrato.MES ->
            java.text.SimpleDateFormat("MMMM yyyy", loc)
                .format(java.util.Date(faixa.inicioMs))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(loc) else it.toString() }
    }
}

/** Diferença em meses de calendário: data no passado → negativo. */
fun offsetMesDe(dataMs: Long, agoraMs: Long = System.currentTimeMillis()): Int {
    val atual = calendarioLocal().apply { timeInMillis = inicioDoMes(agoraMs) }
    val alvo = calendarioLocal().apply { timeInMillis = inicioDoMes(dataMs) }
    val anos = alvo.get(java.util.Calendar.YEAR) - atual.get(java.util.Calendar.YEAR)
    val meses = alvo.get(java.util.Calendar.MONTH) - atual.get(java.util.Calendar.MONTH)
    return anos * 12 + meses
}

/**
 * Se o período corrente está vazio mas há lançamentos, aponta pro período do mais recente.
 * Assim o Extrato não «some» só porque virou o mês.
 */
fun offsetPeriodoComMovimento(
    periodo: PeriodoExtrato,
    lancamentos: List<Lancamento>,
    agoraMs: Long = System.currentTimeMillis(),
): Int {
    if (lancamentos.isEmpty()) return 0
    if (filtrarPorPeriodo(lancamentos, faixaDoPeriodo(periodo, agoraMs)).isNotEmpty()) return 0
    val alvo = lancamentos.maxByOrNull { it.dataMs } ?: return 0
    return when (periodo) {
        PeriodoExtrato.MES -> offsetMesDe(alvo.dataMs, agoraMs)
        PeriodoExtrato.DIA -> {
            val delta = inicioDoDia(alvo.dataMs) - inicioDoDia(agoraMs)
            (delta / (24L * 60 * 60 * 1000)).toInt()
        }
        PeriodoExtrato.SEMANA -> {
            val delta = inicioDaSemana(alvo.dataMs) - inicioDaSemana(agoraMs)
            (delta / (7L * 24 * 60 * 60 * 1000)).toInt()
        }
    }
}

/** Ontem 00:00 local. */
fun ontemMs(agoraMs: Long = System.currentTimeMillis()): Long {
    val c = calendarioLocal()
    c.timeInMillis = inicioDoDia(agoraMs)
    c.add(java.util.Calendar.DAY_OF_MONTH, -1)
    return c.timeInMillis
}

/** Dia [dia] do mês de [refMs] (clamped ao último dia do mês). */
fun dataDiaNoMesLocal(dia: Int, refMs: Long = System.currentTimeMillis()): Long {
    val c = calendarioLocal()
    c.timeInMillis = inicioDoMes(refMs)
    val max = c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    c.set(java.util.Calendar.DAY_OF_MONTH, dia.coerceIn(1, max))
    return c.timeInMillis
}

fun filtrarPorPeriodo(lancamentos: List<Lancamento>, faixa: FaixaPeriodo): List<Lancamento> =
    lancamentos.filter { it.dataMs >= faixa.inicioMs && it.dataMs < faixa.fimMsExclusivo }

fun lancamentoJaConta(lancamento: Lancamento, agoraMs: Long = System.currentTimeMillis()): Boolean =
    lancamento.dataMs < fimDoDiaExclusivo(agoraMs)

fun lancamentosQueJaContam(
    lancamentos: List<Lancamento>,
    agoraMs: Long = System.currentTimeMillis(),
): List<Lancamento> = lancamentos.filter { lancamentoJaConta(it, agoraMs) }

fun filtrarPorPeriodoAteHoje(
    lancamentos: List<Lancamento>,
    faixa: FaixaPeriodo,
    agoraMs: Long = System.currentTimeMillis(),
): List<Lancamento> = filtrarPorPeriodo(lancamentosQueJaContam(lancamentos, agoraMs), faixa)

fun lancamentosFuturos(
    lancamentos: List<Lancamento>,
    agoraMs: Long = System.currentTimeMillis(),
): List<Lancamento> = lancamentos.filter { !lancamentoJaConta(it, agoraMs) }

fun normalizarTagsFinancas(tags: List<String>): List<String> =
    tags
        .flatMap { it.split(",", ";", "\n", "\t") }
        .map { tag ->
            tag.trim()
                .removePrefix("#")
                .lowercase(Locale.forLanguageTag("pt-BR"))
                .filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == ' ' }
                .replace(Regex("\\s+"), "-")
        }
        .filter { it.length in 2..24 }
        .distinct()
        .take(8)

fun resumoDoPeriodo(lancamentos: List<Lancamento>): ResumoPeriodo {
    var entrou = 0L
    var saiu = 0L
    for (l in lancamentos) {
        when (l.tipo) {
            TipoLancamento.ENTRADA -> entrou += l.valorCentavos
            TipoLancamento.SAIDA -> saiu += l.valorCentavos
        }
    }
    return ResumoPeriodo(entrou, saiu)
}

/** Agrupa por dia (desc), com subtotal = entradas − saídas do dia. */
fun agruparPorDia(lancamentos: List<Lancamento>): List<GrupoDiaExtrato> {
    return lancamentos
        .groupBy { inicioDoDia(it.dataMs) }
        .entries
        .sortedByDescending { it.key }
        .map { (dia, lista) ->
            val ordenada = lista.sortedByDescending { it.dataMs }
            val sub = resumoDoPeriodo(ordenada).saldoCentavos
            GrupoDiaExtrato(diaMs = dia, lancamentos = ordenada, subtotalCentavos = sub)
        }
}

/** Rótulo curto do dia: "HOJE · 1 OUT", "ONTEM · 30 SET", "28 SET". */
fun rotuloDiaExtrato(diaMs: Long, agoraMs: Long = System.currentTimeMillis()): String {
    val hoje = inicioDoDia(agoraMs)
    val ontem = hoje - 24L * 60 * 60 * 1000
    val fmt = java.text.SimpleDateFormat("d MMM", Locale.forLanguageTag("pt-BR"))
    val data = fmt.format(java.util.Date(diaMs)).uppercase(Locale.forLanguageTag("pt-BR"))
    return when (inicioDoDia(diaMs)) {
        hoje -> "HOJE · $data"
        ontem -> "ONTEM · $data"
        else -> data
    }
}

/**
 * Meta financeira — `users/{uid}/metasFinanceiras/{id}`.
 * - [TipoMeta.RESERVA]: progresso manual ([atualCentavos] / [alvoCentavos])
 * - [TipoMeta.CORTE]: teto por categoria no mês (atual derivado dos lançamentos)
 * - [TipoMeta.GASTO_MES]: teto geral do mês (substitui o heurístico do anel)
 */
object TipoMeta {
    const val RESERVA = "reserva"
    const val CORTE = "corte"
    const val GASTO_MES = "gasto_mes"

    fun rotulo(tipo: String): String = when (tipo) {
        RESERVA -> "Reserva"
        CORTE -> "Corte"
        GASTO_MES -> "Gasto do mês"
        else -> tipo
    }
}

@Keep
data class MetaFinanceira(
    val id: String,
    val apelido: String,
    val tipo: String,
    val alvoCentavos: Long,
    /** Só faz sentido em reserva (ou override); corte/gasto_mes preferem valor derivado. */
    val atualCentavos: Long = 0,
    val categoria: String? = null,
    val ativa: Boolean = true,
    val createdAtMs: Long = 0,
    val updatedAtMs: Long = 0,
)

/**
 * Transferência entre carteiras — `users/{uid}/transferencias/{id}`.
 * NÃO é gasto nem entrada; só move saldo.
 */
object MotivoTransferencia {
    const val PAGAR_FATURA = "pagar_fatura"
    const val RESERVA = "reserva"
    const val AJUSTE = "ajuste"

    fun rotulo(motivo: String?): String = when (motivo) {
        PAGAR_FATURA -> "Pagar fatura"
        RESERVA -> "Reserva"
        AJUSTE -> "Ajuste"
        else -> "Transferência"
    }
}

@Keep
data class Transferencia(
    val id: String,
    val deCarteiraId: String,
    val paraCarteiraId: String,
    val valorCentavos: Long,
    val dataMs: Long,
    val motivo: String? = null,
    val nota: String? = null,
    val createdAtMs: Long = 0,
    val updatedAtMs: Long = 0,
)

/**
 * Saldo da carteira = inicial + entradas − saídas ± transferências.
 * Transferências NÃO entram no gasto do mês.
 */
fun saldoDerivado(
    carteira: Carteira,
    lancamentos: List<Lancamento>,
    transferencias: List<Transferencia> = emptyList(),
    agoraMs: Long = System.currentTimeMillis(),
): Long {
    var saldo = carteira.saldoInicialCentavos
    val fimHoje = fimDoDiaExclusivo(agoraMs)
    for (l in lancamentos) {
        if (l.dataMs >= fimHoje) continue
        if (l.carteiraId != carteira.id) continue
        when (l.tipo) {
            TipoLancamento.ENTRADA -> saldo += l.valorCentavos
            TipoLancamento.SAIDA -> saldo -= l.valorCentavos
        }
    }
    for (t in transferencias) {
        if (t.dataMs >= fimHoje) continue
        when (carteira.id) {
            t.deCarteiraId -> saldo -= t.valorCentavos
            t.paraCarteiraId -> saldo += t.valorCentavos
        }
    }
    return saldo
}
