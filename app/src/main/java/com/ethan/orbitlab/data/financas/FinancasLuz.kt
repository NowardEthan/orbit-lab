package com.ethan.orbitlab.data.financas

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Locale

/** Fonte da engine — separada da rotina do mobile. */
const val FONTE_LUZ_FINANCAS = "financas"

enum class FaseLua(val rotulo: String) {
    NOVA("lua nova"),
    CRESCENTE("crescente"),
    QUASE_CHEIA("quase cheia"),
    CHEIA("cheia"),
}

object ConquistaId {
    const val PRIMEIRA_LUZ = "primeira_luz"
    const val OFENSIVA_7 = "ofensiva_7"
    const val OFENSIVA_30 = "ofensiva_30"
    const val MES_NO_AZUL = "mes_no_azul"
    const val CONTAS_10 = "contas_10"
    const val RESERVA = "reserva_comecada"
}

data class ConquistaDef(
    val id: String,
    val titulo: String,
    val descricao: String,
    val emoji: String,
)

val CONQUISTAS_FINANCAS = listOf(
    ConquistaDef(ConquistaId.PRIMEIRA_LUZ, "Primeira luz", "Ganhou luz pela primeira vez", "✨"),
    ConquistaDef(ConquistaId.OFENSIVA_7, "Semana firme", "7 dias no orçamento", "🔥"),
    ConquistaDef(ConquistaId.OFENSIVA_30, "Mês constante", "30 dias no orçamento", "🌙"),
    ConquistaDef(ConquistaId.MES_NO_AZUL, "Mês no azul", "Gastou dentro da meta do mês", "💙"),
    ConquistaDef(ConquistaId.CONTAS_10, "Contas em dia", "10 fixos pagos no prazo", "✅"),
    ConquistaDef(ConquistaId.RESERVA, "Reserva começada", "Sobra livre positiva nos fixos", "⭐"),
)

data class FinancasLuzEstado(
    val luzTotal: Int = 0,
    val ofensiva: Int = 0,
    val fase: FaseLua = FaseLua.NOVA,
    /** Luz ganha em cada dia da semana atual (seg→dom). */
    val luzSemana: List<Int> = List(7) { 0 },
    val conquistas: Set<String> = emptySet(),
    val luzEstaSemana: Int = 0,
) {
    /** Quantas das 4 barreiras de fase estão acesas (0 = nova … 4 = cheia). */
    val fasesCompletas: Int get() = when (fase) {
        FaseLua.NOVA -> 0
        FaseLua.CRESCENTE -> 1
        FaseLua.QUASE_CHEIA -> 2
        FaseLua.CHEIA -> 4
    }
}

private object LuzPontos {
    const val ORCAMENTO_DIA = 40
    const val REGISTROU = 15
    const val RECORRENTE_PAGO = 25
}

/**
 * Engine enxuta de luz/ofensiva/fase — só Finanças, persistência local por uid.
 * Idempotente: reprocessa a partir dos lançamentos sem duplicar prêmios do dia.
 */
object FinancasLuzEngine {
    private const val PREFS = "orbitlab_financas_luz"
    private lateinit var prefs: SharedPreferences
    private var uid: String? = null

    private val _estado = MutableStateFlow(FinancasLuzEstado())
    val estado: StateFlow<FinancasLuzEstado> = _estado.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun ligarUid(novoUid: String?) {
        uid = novoUid
        _estado.value = if (novoUid == null) FinancasLuzEstado() else carregar(novoUid)
    }

    /**
     * Reconcilia ofensiva + prêmios do dia a partir dos dados atuais.
     * Seguro chamar a cada snapshot.
     */
    fun reconciliar(
        lancamentos: List<Lancamento>,
        recorrentes: List<Recorrente>,
        agoraMs: Long = System.currentTimeMillis(),
    ) {
        val u = uid ?: return
        if (!::prefs.isInitialized) return

        var est = carregar(u)
        val meta = metaGastoMes(recorrentes)
        val ofensiva = diasNoOrcamento(lancamentos, meta, agoraMs)
        val fase = faseDaOfensiva(ofensiva)

        val awards = awardsSet(u).toMutableSet()
        val conquistas = est.conquistas.toMutableSet()
        var luzTotal = est.luzTotal
        val semanaMap = semanaMap(u).toMutableMap()

        val hojeKey = chaveDia(agoraMs)
        val hojeIni = inicioDoDia(agoraMs)
        val hojeFim = fimDoDiaExclusivo(agoraMs)
        val c = Calendar.getInstance()
        c.timeInMillis = agoraMs
        val diasNoMes = c.getActualMaximum(Calendar.DAY_OF_MONTH)
        val metaDia = if (meta > 0) meta / diasNoMes.coerceAtLeast(1) else Long.MAX_VALUE

        val gastoHoje = lancamentos
            .filter { it.dataMs >= hojeIni && it.dataMs < hojeFim && it.tipo == TipoLancamento.SAIDA }
            .sumOf { it.valorCentavos }

        // ── Orçamento do dia ──
        val awardOrc = "orcamento:$hojeKey"
        if (meta > 0 && gastoHoje <= metaDia && awardOrc !in awards) {
            awards += awardOrc
            luzTotal += LuzPontos.ORCAMENTO_DIA
            somarSemana(semanaMap, hojeKey, LuzPontos.ORCAMENTO_DIA)
        }

        // ── Registrou hoje (manual) ──
        val registrou = lancamentos.any {
            it.dataMs >= hojeIni && it.dataMs < hojeFim &&
                it.origem == OrigemLancamento.MANUAL && it.recorrenteId == null
        }
        val awardReg = "registro:$hojeKey"
        if (registrou && awardReg !in awards) {
            awards += awardReg
            luzTotal += LuzPontos.REGISTROU
            somarSemana(semanaMap, hojeKey, LuzPontos.REGISTROU)
        }

        // ── Recorrentes pagos em dia (este mês) ──
        var pagosEmDia = 0
        for (l in lancamentos) {
            val rid = l.recorrenteId ?: continue
            if (l.tipo != TipoLancamento.SAIDA || !l.pago) continue
            if (chaveMes(l.dataMs) != chaveMes(agoraMs)) continue
            val rec = recorrentes.find { it.id == rid } ?: continue
            val previsto = dataDoRecorrenteNoMes(rec.diaDoMes, l.dataMs)
            if (inicioDoDia(l.updatedAtMs.takeIf { it > 0 } ?: l.dataMs) <= previsto + 24L * 60 * 60 * 1000) {
                pagosEmDia++
                val awardRec = "rec_pago:${chaveMes(agoraMs)}:$rid"
                if (awardRec !in awards) {
                    awards += awardRec
                    luzTotal += LuzPontos.RECORRENTE_PAGO
                    val diaPago = chaveDia(l.updatedAtMs.takeIf { it > 0 } ?: l.dataMs)
                    somarSemana(semanaMap, diaPago, LuzPontos.RECORRENTE_PAGO)
                }
            }
        }

        // ── Conquistas ──
        if (luzTotal > 0) conquistas += ConquistaId.PRIMEIRA_LUZ
        if (ofensiva >= 7) conquistas += ConquistaId.OFENSIVA_7
        if (ofensiva >= 30) conquistas += ConquistaId.OFENSIVA_30
        if (pagosEmDia >= 10) conquistas += ConquistaId.CONTAS_10
        val resumoMes = resumoDoPeriodo(
            filtrarPorPeriodoAteHoje(lancamentos, faixaDoPeriodo(PeriodoExtrato.MES, agoraMs), agoraMs),
        )
        if (meta > 0 && resumoMes.saiuCentavos <= meta && resumoMes.saiuCentavos > 0) {
            conquistas += ConquistaId.MES_NO_AZUL
        }
        val fixos = resumoRecorrentes(recorrentes.filter { it.ativo })
        if (fixos.sobraLivreCentavos > 0 && fixos.entramCentavos > 0) {
            conquistas += ConquistaId.RESERVA
        }

        limparSemanaVelha(semanaMap, agoraMs)
        val luzSemana = barrasSemana(semanaMap, agoraMs)
        val luzEstaSemana = luzSemana.sum()

        est = FinancasLuzEstado(
            luzTotal = luzTotal,
            ofensiva = ofensiva,
            fase = fase,
            luzSemana = luzSemana,
            conquistas = conquistas,
            luzEstaSemana = luzEstaSemana,
        )
        salvar(u, est, awards, semanaMap)
        _estado.value = est
    }

    fun faseDaOfensiva(ofensiva: Int): FaseLua = when {
        ofensiva >= 14 -> FaseLua.CHEIA
        ofensiva >= 7 -> FaseLua.QUASE_CHEIA
        ofensiva >= 3 -> FaseLua.CRESCENTE
        else -> FaseLua.NOVA
    }

    // ── Persistência ──

    private fun prefix(uid: String) = "u_$uid."

    private fun carregar(uid: String): FinancasLuzEstado {
        val p = prefix(uid)
        val conquistas = prefs.getStringSet("${p}conquistas", emptySet())?.toSet().orEmpty()
        val ofensiva = prefs.getInt("${p}ofensiva", 0)
        return FinancasLuzEstado(
            luzTotal = prefs.getInt("${p}luz", 0),
            ofensiva = ofensiva,
            fase = faseDaOfensiva(ofensiva),
            luzSemana = List(7) { 0 },
            conquistas = conquistas,
            luzEstaSemana = 0,
        )
    }

    private fun awardsSet(uid: String): Set<String> =
        prefs.getStringSet("${prefix(uid)}awards", emptySet())?.toSet().orEmpty()

    private fun semanaMap(uid: String): MutableMap<String, Int> {
        val raw = prefs.getString("${prefix(uid)}semana", "").orEmpty()
        if (raw.isBlank()) return mutableMapOf()
        return raw.split(';')
            .mapNotNull {
                val (k, v) = it.split('=', limit = 2).takeIf { p -> p.size == 2 } ?: return@mapNotNull null
                k to (v.toIntOrNull() ?: return@mapNotNull null)
            }
            .toMap()
            .toMutableMap()
    }

    private fun salvar(
        uid: String,
        est: FinancasLuzEstado,
        awards: Set<String>,
        semana: Map<String, Int>,
    ) {
        val p = prefix(uid)
        // Awards crescem — poda as mais antigas se passar de 400
        val awardsTrim = if (awards.size > 400) awards.toList().takeLast(300).toSet() else awards
        prefs.edit()
            .putInt("${p}luz", est.luzTotal)
            .putInt("${p}ofensiva", est.ofensiva)
            .putStringSet("${p}conquistas", est.conquistas)
            .putStringSet("${p}awards", awardsTrim)
            .putString("${p}semana", semana.entries.joinToString(";") { "${it.key}=${it.value}" })
            .apply()
    }

    private fun somarSemana(map: MutableMap<String, Int>, diaKey: String, pontos: Int) {
        map[diaKey] = (map[diaKey] ?: 0) + pontos
    }

    private fun limparSemanaVelha(map: MutableMap<String, Int>, agoraMs: Long) {
        val limite = inicioDaSemana(agoraMs) - 7L * 24 * 60 * 60 * 1000
        val keys = map.keys.toList()
        for (k in keys) {
            val ms = parseChaveDia(k) ?: continue
            if (ms < limite) map.remove(k)
        }
    }

    private fun barrasSemana(map: Map<String, Int>, agoraMs: Long): List<Int> {
        val ini = inicioDaSemana(agoraMs)
        return (0 until 7).map { i ->
            val dia = ini + i * 24L * 60 * 60 * 1000
            map[chaveDia(dia)] ?: 0
        }
    }

    private fun chaveDia(ms: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun parseChaveDia(key: String): Long? {
        val p = key.split('-')
        if (p.size != 3) return null
        val c = Calendar.getInstance()
        c.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt(), 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
