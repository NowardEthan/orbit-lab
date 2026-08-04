package com.ethan.orbitlab.data.financas

import androidx.annotation.Keep
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Fatura de crédito = saídas no cartão no ciclo (fechamento→fechamento),
 * abatida por transferências com motivo [MotivoTransferencia.PAGAR_FATURA] pra esse cartão.
 */
@Keep
data class FaturaCredito(
    val gastoCicloCentavos: Long,
    val pagoCicloCentavos: Long,
    val faturaCentavos: Long,
    val limiteCentavos: Long?,
    val limiteLivreCentavos: Long?,
    val ciclo: FaixaPeriodo,
    val fechamentoDia: Int?,
    val vencimentoDia: Int?,
    val vencimentoMs: Long?,
) {
    val pctUsado: Float
        get() {
            val lim = limiteCentavos ?: return 0f
            if (lim <= 0L) return 0f
            return (faturaCentavos.toFloat() / lim.toFloat()).coerceIn(0f, 1.5f)
        }
}

fun cicloFaturaAtual(
    fechamentoDia: Int?,
    agoraMs: Long = System.currentTimeMillis(),
): FaixaPeriodo {
    if (fechamentoDia == null || fechamentoDia !in 1..31) {
        return faixaDoPeriodo(PeriodoExtrato.MES, agoraMs)
    }
    val c = calendarioFatura()
    c.timeInMillis = agoraMs
    val hoje = c.get(Calendar.DAY_OF_MONTH)
    val ano = c.get(Calendar.YEAR)
    val mes = c.get(Calendar.MONTH) // 0-based

    val (iniAno, iniMes) = if (hoje >= fechamentoDia) {
        ano to mes
    } else {
        // Mês anterior
        c.set(ano, mes, 1, 0, 0, 0)
        c.add(Calendar.MONTH, -1)
        c.get(Calendar.YEAR) to c.get(Calendar.MONTH)
    }
    val inicioMs = dataDiaNoMes(fechamentoDia, iniAno, iniMes)
    c.timeInMillis = inicioMs
    c.add(Calendar.MONTH, 1)
    val fimMs = dataDiaNoMes(fechamentoDia, c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    return FaixaPeriodo(inicioMs, fimMs)
}

fun faturaCredito(
    carteira: Carteira,
    lancamentos: List<Lancamento>,
    transferencias: List<Transferencia>,
    agoraMs: Long = System.currentTimeMillis(),
): FaturaCredito? {
    if (carteira.tipo != TipoCarteira.CARTAO_CREDITO) return null
    val ciclo = cicloFaturaAtual(carteira.fechamentoDia, agoraMs)
    val gasto = lancamentos
        .filter {
            it.carteiraId == carteira.id &&
                it.tipo == TipoLancamento.SAIDA &&
                it.dataMs >= ciclo.inicioMs &&
                it.dataMs < ciclo.fimMsExclusivo
        }
        .sumOf { it.valorCentavos }
    val pago = transferencias
        .filter {
            it.paraCarteiraId == carteira.id &&
                it.motivo == MotivoTransferencia.PAGAR_FATURA &&
                it.dataMs >= ciclo.inicioMs &&
                it.dataMs < ciclo.fimMsExclusivo
        }
        .sumOf { it.valorCentavos }
    val fatura = (gasto - pago).coerceAtLeast(0L)
    val limite = carteira.limiteCentavos
    val livre = limite?.let { (it - fatura).coerceAtLeast(0L) }
    return FaturaCredito(
        gastoCicloCentavos = gasto,
        pagoCicloCentavos = pago,
        faturaCentavos = fatura,
        limiteCentavos = limite,
        limiteLivreCentavos = livre,
        ciclo = ciclo,
        fechamentoDia = carteira.fechamentoDia,
        vencimentoDia = carteira.vencimentoDia,
        vencimentoMs = proximoVencimentoMs(carteira.vencimentoDia, agoraMs),
    )
}

fun proximoVencimentoMs(vencimentoDia: Int?, agoraMs: Long): Long? {
    if (vencimentoDia == null || vencimentoDia !in 1..31) return null
    val c = calendarioFatura()
    c.timeInMillis = agoraMs
    val hoje = c.get(Calendar.DAY_OF_MONTH)
    val ano = c.get(Calendar.YEAR)
    val mes = c.get(Calendar.MONTH)
    return if (hoje <= vencimentoDia) {
        dataDiaNoMes(vencimentoDia, ano, mes)
    } else {
        c.set(ano, mes, 1, 0, 0, 0)
        c.add(Calendar.MONTH, 1)
        dataDiaNoMes(vencimentoDia, c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    }
}

/** Dias até o vencimento (0 = hoje; negativo = passou). */
fun diasAte(ms: Long?, agoraMs: Long = System.currentTimeMillis()): Int? {
    if (ms == null) return null
    val diff = inicioDoDia(ms) - inicioDoDia(agoraMs)
    return (diff / (24L * 60 * 60 * 1000)).toInt()
}

private fun calendarioFatura(): Calendar =
    Calendar.getInstance(TimeZone.getDefault(), Locale.forLanguageTag("pt-BR"))

private fun dataDiaNoMes(dia: Int, ano: Int, mes0: Int): Long {
    val c = calendarioFatura()
    c.clear()
    c.set(Calendar.YEAR, ano)
    c.set(Calendar.MONTH, mes0)
    val max = c.getActualMaximum(Calendar.DAY_OF_MONTH)
    c.set(Calendar.DAY_OF_MONTH, dia.coerceIn(1, max))
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}
