package com.ethan.orbitlab.domain.rotina

/** 0 = domingo … 6 = sábado (igual a Calendar.DAY_OF_WEEK - 1 / Date.getDay()). */
typealias DiaSemana = Int

/** Minutos desde a meia-noite. 7h30 = 450. */
typealias Minuto = Int

val DIAS_CURTOS = listOf("dom", "seg", "ter", "qua", "qui", "sex", "sáb")
val DIAS_LONGOS = listOf(
    "domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado",
)

data class PassoBloco(
    val id: String,
    val texto: String,
    val feito: Boolean = false,
)

data class SubTarefa(
    val id: String,
    val texto: String,
    val feito: Boolean = false,
    val hora: Minuto? = null,
    val notificar: Boolean = false,
)

enum class OrigemBloco { Ethan, Luna }

data class BlocoRotina(
    val id: String,
    val titulo: String,
    val dias: List<DiaSemana>,
    val inicio: Minuto,
    val fim: Minuto,
    /** Hex `#RRGGBB`. */
    val cor: String,
    val icone: String? = null,
    val notificar: Boolean = true,
    val alarme: Boolean = false,
    val nota: String? = null,
    val roteiro: String? = null,
    val guia: String? = null,
    val passos: List<PassoBloco>? = null,
    /** Fixas · todo dia (molde do bloco). */
    val subtarefas: List<SubTarefa>? = null,
    val criadoEm: Long? = null,
    val origem: OrigemBloco = OrigemBloco.Ethan,
    /** null = rotina Normal. */
    val setId: String? = null,
)

data class RotinaSet(
    val id: String,
    val nome: String,
    val cor: String? = null,
    /** YYYY-MM-DD. */
    val de: String? = null,
    val ate: String? = null,
    val criadoEm: Long? = null,
)

const val ROTINA_NORMAL = "normal"
const val PREFIXO_SESSAO_BLOCO = "rotina-"
const val SESSAO_ROTINA_GERAL = "rotina-geral"

fun sessaoDeBloco(blocoId: String): String = "$PREFIXO_SESSAO_BLOCO$blocoId"

data class ItensDoDia(
    val passosFeitos: List<String> = emptyList(),
    val subsFeitas: List<String> = emptyList(),
    /** Tarefas só deste dia (não no molde). */
    val tarefasDoDia: List<SubTarefa> = emptyList(),
)

enum class EstadoDoBloco { Feito, HojeNao, Ignorado }

data class EstadoAgora(
    val atual: BlocoRotina? = null,
    val faltamMinutos: Int? = null,
    val proximo: BlocoRotina? = null,
    val emMinutos: Int? = null,
    val proximoEhAmanha: Boolean = false,
    val livreMinutos: Int? = null,
)

data class BuracoLivre(
    val inicio: Minuto,
    val fim: Minuto,
    val minutos: Int,
)
