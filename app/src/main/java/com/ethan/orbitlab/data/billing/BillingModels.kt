package com.ethan.orbitlab.data.billing

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Modelos + cópia do billing da Luna, espelhando o cliente RN
 * (`orbit-mobile/src/features/billing/`) em Kotlin. Fonte única de preços,
 * limites e formatação — pra não reinventar número em cada tela.
 *
 * A carteira é imposta no SERVIDOR (mobile-api/billing). Aqui o app só LÊ o
 * retrato de uso (`/v1/billing/usage`) e mostra — o servidor já entrega
 * `remainingTokens` com a conta janela×semana feita, então o Kotlin não repete
 * a matemática de merge do RN.
 */

enum class PlanId(val id: String) {
    FREE("free"),
    PLUS("plus"),
    PRO("pro");

    companion object {
        fun fromId(raw: String?): PlanId = when (raw?.trim()?.lowercase()) {
            "plus" -> PLUS
            "pro" -> PRO
            else -> FREE
        }
    }
}

/** Custo mínimo de uma conversa (espelha CUSTO_MINIMO_CHAT do servidor, reprecificado 2026-08). */
const val CUSTO_MINIMO_CHAT = 3_000L

/** As duas "lagostas" que pesam na carteira — espelham as constantes do servidor. */
const val CUSTO_IMAGEM_GERADA = 30_000L
const val CUSTO_PESQUISA_PROFUNDA = 15_000L

/** Janela rolante do Grátis — 5 h (sessões longas sem reset prematuro). */
const val FREE_QUOTA_WINDOW_HOURS = 5

/** Teto de tokens por janela de 5 h (free/plus/pro). Fora da janela rolante = null. */
val WINDOW_TOKEN_LIMITS: Map<PlanId, Long> = mapOf(
    PlanId.FREE to 35_000L,
    PlanId.PLUS to 180_000L,
    PlanId.PRO to 450_000L,
)

/** Teto de tokens por 7 dias. */
val WEEKLY_TOKEN_LIMITS: Map<PlanId, Long> = mapOf(
    PlanId.FREE to 150_000L,
    PlanId.PLUS to 750_000L,
    PlanId.PRO to 2_250_000L,
)

/** Formata contagem de tokens pra exibição (pt-BR): 35 mil, 2,3 M, 900. */
fun formatarTokens(n: Long): String {
    val v = max(0L, n)
    if (v >= 1_000_000L) {
        val m = v / 1_000_000.0
        return if (m % 1.0 == 0.0) "${m.toInt()} M"
        else "${String.format("%.1f", m).replace('.', ',')} M"
    }
    if (v >= 10_000L) return "${(v / 1_000.0).roundToLong()} mil"
    if (v >= 1_000L) {
        val k = v / 1_000.0
        return if (k % 1.0 == 0.0) "${k.toInt()} mil"
        else "${String.format("%.1f", k).replace('.', ',')} mil"
    }
    return v.toString()
}

private fun Double.roundToLong(): Long = Math.round(this)

/** Horas até renovar (arredonda pra cima). */
fun horasAteReset(resetsAtMs: Long, agoraMs: Long = System.currentTimeMillis()): Int =
    max(0, ceil((resetsAtMs - agoraMs) / 3_600_000.0).toInt())

/** "em 2h 15min", "em 40 minutos", "em 3 horas" — o quanto falta pra carteira renovar. */
fun formatarReset(msAteReset: Long): String {
    if (msAteReset <= 0) return "em breve"
    val totalMin = ceil(msAteReset / 60_000.0).toInt()
    val horas = totalMin / 60
    val min = totalMin % 60
    return when {
        horas == 0 -> if (min == 1) "em 1 minuto" else "em $min minutos"
        min == 0 -> if (horas == 1) "em 1 hora" else "em $horas horas"
        else -> "em ${horas}h ${min}min"
    }
}

/** Uso semanal (o binding que aperta primeiro num usuário intenso). */
data class WeeklyTokens(
    val used: Long,
    val limit: Long,
    val remaining: Long,
    val resetsAtMs: Long?,
)

/** Fallback reduzido (OpenRouter free) quando a carteira do plano esgota. */
data class ReducedMode(
    val available: Boolean,
    val dailyUsed: Long,
    val dailyLimit: Long,
    val dailyRemaining: Long,
    val resetsAtMs: Long?,
)

/**
 * Retrato de uso — espelha o `QuotaUsageSnapshot` que o servidor devolve em
 * `/v1/billing/usage`. O servidor já calcula [remainingTokens] (min entre o que
 * sobra na janela e na semana), então o medidor lê isso direto.
 */
data class UsageSnapshot(
    val planId: PlanId,
    val cycle: String,            // "window" | "monthly" | "unlimited"
    val windowHours: Int?,
    val usedTokens: Long,
    val windowTokenLimit: Long?,  // null = ilimitado (criador verificado)
    val remainingTokens: Long?,   // null = ilimitado
    val bonusTurns: Int,
    val resetsAtMs: Long?,
    val weeklyTokens: WeeklyTokens?,
    val reducedMode: ReducedMode?,
    val loading: Boolean = false,
) {
    /** Sem teto (criador) — nunca barra, nunca mostra medidor de fim. */
    val ilimitado: Boolean get() = windowTokenLimit == null

    /** % consumida da carteira efetiva (0–100). */
    val pct: Int
        get() {
            val limite = windowTokenLimit ?: return 0
            if (limite <= 0) return 0
            return min(100, floor((usedTokens.toDouble() / limite) * 100).toInt())
        }

    /** Ainda dá pra mandar uma conversa no plano? (fora o fallback reduzido) */
    val temSaldoParaChat: Boolean
        get() {
            if (ilimitado) return true
            val r = remainingTokens ?: return true
            return r >= CUSTO_MINIMO_CHAT
        }

    companion object {
        val CARREGANDO = UsageSnapshot(
            planId = PlanId.FREE,
            cycle = "window",
            windowHours = FREE_QUOTA_WINDOW_HOURS,
            usedTokens = 0,
            windowTokenLimit = WINDOW_TOKEN_LIMITS[PlanId.FREE],
            remainingTokens = WINDOW_TOKEN_LIMITS[PlanId.FREE],
            bonusTurns = 0,
            resetsAtMs = null,
            weeklyTokens = null,
            reducedMode = null,
            loading = true,
        )
    }
}

// ── Planos (cópia + preços, espelho de plans.ts) ───────────────────────────

enum class Disponibilidade { SIM, NAO, LIMITADO }

data class PlanFeature(
    val label: String,
    val disponivel: Disponibilidade,
    val detalhe: String? = null,
)

data class PlanConfig(
    val id: PlanId,
    val nome: String,
    val tagline: String,
    val precoMensal: Int,
    val precoAnual: Int?,
    val precoAnualPorMes: Double?,
    val destaque: Boolean = false,
    val badge: String? = null,
    val features: List<PlanFeature>,
)

/** "~11 conversas por janela" — com a conversa a ~3k (reprecificação 2026-08). */
private fun aproxConversasPorJanela(planId: PlanId): String {
    val teto = WINDOW_TOKEN_LIMITS[planId] ?: return ""
    val n = floor(teto.toDouble() / CUSTO_MINIMO_CHAT).toInt()
    return "~$n conversas por janela"
}

private fun detalheLimiteTokens(planId: PlanId): String {
    val janela = formatarTokens(WINDOW_TOKEN_LIMITS[planId] ?: 0)
    val semana = formatarTokens(WEEKLY_TOKEN_LIMITS[planId] ?: 0)
    return "$janela a cada $FREE_QUOTA_WINDOW_HOURS h · $semana/semana"
}

val PLANS: List<PlanConfig> = listOf(
    PlanConfig(
        id = PlanId.FREE,
        nome = "Grátis",
        tagline = "Para conhecer a Luna",
        precoMensal = 0,
        precoAnual = null,
        precoAnualPorMes = null,
        features = listOf(
            PlanFeature("Tokens na nuvem", Disponibilidade.LIMITADO, detalheLimiteTokens(PlanId.FREE)),
            PlanFeature("Uso típico", Disponibilidade.LIMITADO, aproxConversasPorJanela(PlanId.FREE)),
            PlanFeature("Histórico na nuvem", Disponibilidade.SIM),
            PlanFeature("Memória entre conversas", Disponibilidade.LIMITADO),
            PlanFeature("Gerar imagens e pesquisa profunda", Disponibilidade.LIMITADO, "pesa mais na carteira"),
        ),
    ),
    PlanConfig(
        id = PlanId.PLUS,
        nome = "Luna Plus",
        tagline = "Uso diário com folga",
        precoMensal = 39,
        precoAnual = 390,
        precoAnualPorMes = 32.5,
        features = listOf(
            PlanFeature("Tokens na nuvem", Disponibilidade.LIMITADO, detalheLimiteTokens(PlanId.PLUS)),
            PlanFeature("Uso típico", Disponibilidade.LIMITADO, aproxConversasPorJanela(PlanId.PLUS)),
            PlanFeature("Memória global", Disponibilidade.SIM),
            PlanFeature("Gerar imagens e pesquisa profunda", Disponibilidade.SIM),
            PlanFeature("Sincronização entre dispositivos", Disponibilidade.SIM),
        ),
    ),
    PlanConfig(
        id = PlanId.PRO,
        nome = "Luna Pro",
        tagline = "Potência premium para uso intenso",
        precoMensal = 79,
        precoAnual = 790,
        precoAnualPorMes = 65.83,
        destaque = true,
        badge = "Recomendado",
        features = listOf(
            PlanFeature("Tokens na nuvem", Disponibilidade.LIMITADO, detalheLimiteTokens(PlanId.PRO)),
            PlanFeature("Uso típico", Disponibilidade.LIMITADO, aproxConversasPorJanela(PlanId.PRO)),
            PlanFeature("Memória global completa", Disponibilidade.SIM),
            PlanFeature("Gerar imagens e pesquisa profunda", Disponibilidade.SIM),
            PlanFeature("Prioridade nas respostas", Disponibilidade.SIM),
        ),
    ),
)

fun planoPorId(id: PlanId): PlanConfig = PLANS.firstOrNull { it.id == id } ?: PLANS.first()

/** Planos com checkout no mobile (o Grátis não tem botão de assinar). */
val PLANOS_CHECKOUT: List<PlanConfig> = PLANS.filter { it.id == PlanId.PLUS || it.id == PlanId.PRO }

// ── CPF/CNPJ (porta de cpfCnpj.ts) ─────────────────────────────────────────

/** Só os dígitos. */
fun normalizarCpfCnpj(raw: String): String = raw.filter { it.isDigit() }

/** Válido = 11 dígitos (CPF) ou 14 (CNPJ). Não valida os dígitos verificadores (o Asaas confere). */
fun cpfCnpjValido(digitos: String): Boolean = digitos.length == 11 || digitos.length == 14

/** Máscara pra exibição: 000.000.000-00 (CPF) ou 00.000.000/0000-00 (CNPJ). */
fun formatarCpfCnpj(digitosRaw: String): String {
    val d = normalizarCpfCnpj(digitosRaw).take(14)
    return if (d.length <= 11) {
        buildString {
            for (i in d.indices) {
                if (i == 3 || i == 6) append('.')
                if (i == 9) append('-')
                append(d[i])
            }
        }
    } else {
        buildString {
            for (i in d.indices) {
                if (i == 2 || i == 5) append('.')
                if (i == 8) append('/')
                if (i == 12) append('-')
                append(d[i])
            }
        }
    }
}
