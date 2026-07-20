package com.ethan.orbitlab.ui.chat

/** Estado de uma fonte web na timeline de pesquisa. */
enum class LunaFonteStatus {
    ENCONTRADA,
    LENDO,
    LIDA,
    CONFIRMADA,
    DESCARTADA,
    CITADA,
}

enum class LunaResearchStepKind {
    PLAN,
    SEARCH,
    READ,
    VERIFY,
    SUMMARIZE,
    WRITE,
}

enum class LunaResearchStepStatus {
    PENDING,
    RUNNING,
    DONE,
    ERROR,
}

enum class LunaResearchRunStatus {
    RUNNING,
    DONE,
}

data class LunaWebFonte(
    val id: String,
    val title: String,
    val url: String,
    val domain: String,
    val snippet: String? = null,
    val status: LunaFonteStatus = LunaFonteStatus.ENCONTRADA,
    val publishedAt: String? = null,
)

data class LunaCitacao(
    val id: String,
    val index: Int,
    val sourceId: String,
    val title: String,
    val url: String,
    val excerpt: String? = null,
)

data class LunaResearchStep(
    val id: String,
    val label: String,
    val detail: String? = null,
    val kind: LunaResearchStepKind = LunaResearchStepKind.SEARCH,
    val status: LunaResearchStepStatus = LunaResearchStepStatus.DONE,
    val queries: List<String> = emptyList(),
    val sources: List<LunaWebFonte> = emptyList(),
    val citations: List<LunaCitacao> = emptyList(),
)

data class LunaResearchRun(
    val id: String,
    val title: String,
    val status: LunaResearchRunStatus,
    val steps: List<LunaResearchStep>,
)

fun LunaResearchRun.totalFontes(): Int =
    steps.flatMap { it.sources }.distinctBy { it.url }.size

fun LunaResearchRun.totalConsultas(): Int =
    steps.sumOf { it.queries.size }

fun LunaResearchRun.totalCitacoes(): Int =
    steps.flatMap { it.citations }.distinctBy { it.index }.size

fun LunaResearchRun.fasesConcluidas(): Int =
    steps.count { it.status == LunaResearchStepStatus.DONE }

fun LunaFonteStatus.rotulo(): String = when (this) {
    LunaFonteStatus.ENCONTRADA -> "Encontrada"
    LunaFonteStatus.LENDO -> "Lendo"
    LunaFonteStatus.LIDA -> "Lida"
    LunaFonteStatus.CONFIRMADA -> "Confirmada"
    LunaFonteStatus.DESCARTADA -> "Descartada"
    LunaFonteStatus.CITADA -> "Citada"
}

fun LunaResearchStepKind.icone(): String = when (this) {
    LunaResearchStepKind.PLAN -> "◈"
    LunaResearchStepKind.SEARCH -> "⌕"
    LunaResearchStepKind.READ -> "⊟"
    LunaResearchStepKind.VERIFY -> "⊜"
    LunaResearchStepKind.SUMMARIZE -> "◐"
    LunaResearchStepKind.WRITE -> "¶"
}
