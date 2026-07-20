package com.ethan.orbitlab.ui.chat

/**
 * Modelo unificado de ações da Luna — espelho de `orbit-mobile/src/lib/lunaActionModel.ts`.
 *
 * - **deep-research**: usou web (`web_search` / `ler_url`) → painel de pesquisa profunda
 * - **task**: só tools (imagem, vídeo, memória…) → strip de chips, sem painel
 */

enum class LunaActionProfile {
    TASK,
    DEEP_RESEARCH,
}

enum class LunaActionStepKind {
    REASON,
    PLAN,
    READ,
    SEARCH,
    VISION,
    VIDEO,
    MEMORY,
    WRITE,
    RUN,
    VERIFY,
    ITERATE,
    SUMMARIZE,
}

enum class LunaActionStepStatus {
    PENDING,
    RUNNING,
    DONE,
    ERROR,
    SKIPPED,
}

enum class LunaActionRunStatus {
    RUNNING,
    DONE,
    ERROR,
}

data class LunaActionStep(
    val id: String,
    val label: String,
    val detail: String? = null,
    val status: LunaActionStepStatus = LunaActionStepStatus.DONE,
    val kind: LunaActionStepKind = LunaActionStepKind.RUN,
    val queries: List<String> = emptyList(),
    val sources: List<LunaWebFonte> = emptyList(),
    val citations: List<LunaCitacao> = emptyList(),
    val ferramenta: String? = null,
)

data class LunaActionRun(
    val id: String,
    val title: String,
    val status: LunaActionRunStatus,
    val steps: List<LunaActionStep>,
    val profile: LunaActionProfile,
)

fun LunaActionRun.isDeepResearch(): Boolean =
    profile == LunaActionProfile.DEEP_RESEARCH

fun LunaActionRun.toolSteps(): List<LunaActionStep> =
    steps.filter { it.kind !in WEB_KINDS }

fun LunaActionRun.webSteps(): List<LunaActionStep> =
    steps.filter { it.kind in WEB_KINDS }

private val WEB_KINDS = setOf(
    LunaActionStepKind.SEARCH,
    LunaActionStepKind.READ,
    LunaActionStepKind.VERIFY,
)

data class ToolMeta(
    val kind: LunaActionStepKind,
    val live: (String) -> String,
    val done: (String) -> String,
)

fun toolMeta(ferramenta: String): ToolMeta = when (ferramenta) {
    "web_search" -> ToolMeta(
        kind = LunaActionStepKind.SEARCH,
        live = { arg -> if (arg.isBlank()) "Pesquisando na web" else "Pesquisando \"$arg\"" },
        done = { arg -> if (arg.isBlank()) "Pesquisa na web" else "Pesquisar \"$arg\"" },
    )
    "ler_url" -> ToolMeta(
        kind = LunaActionStepKind.READ,
        live = { arg -> if (arg.isBlank()) "Lendo o link" else "Lendo $arg" },
        done = { arg -> if (arg.isBlank()) "Leitura de link" else "Ler $arg" },
    )
    "ver_imagem" -> ToolMeta(
        kind = LunaActionStepKind.VISION,
        live = { "Olhando a imagem" },
        done = { "Analisou a imagem" },
    )
    "ver_video" -> ToolMeta(
        kind = LunaActionStepKind.VIDEO,
        live = { "Assistindo o vídeo" },
        done = { "Assistiu o vídeo" },
    )
    "consultar_atlas" -> ToolMeta(
        kind = LunaActionStepKind.MEMORY,
        live = { arg -> if (arg.isBlank()) "Consultando a memória" else "Lembrando de \"$arg\"" },
        done = { arg -> if (arg.isBlank()) "Consultou a memória" else "Lembrou de \"$arg\"" },
    )
    else -> ToolMeta(
        kind = LunaActionStepKind.RUN,
        live = { "Usando uma ferramenta" },
        done = { ferramenta.replace('_', ' ') },
    )
}

fun ehFerramentaDeWeb(ferramenta: String): Boolean {
    val kind = toolMeta(ferramenta).kind
    return kind == LunaActionStepKind.SEARCH || kind == LunaActionStepKind.READ
}

fun LunaActionStepKind.icone(): String = when (this) {
    LunaActionStepKind.REASON -> "◎"
    LunaActionStepKind.PLAN -> "◈"
    LunaActionStepKind.READ -> "⊟"
    LunaActionStepKind.SEARCH -> "⌕"
    LunaActionStepKind.VISION -> "▣"
    LunaActionStepKind.VIDEO -> "▶"
    LunaActionStepKind.MEMORY -> "❖"
    LunaActionStepKind.WRITE -> "✎"
    LunaActionStepKind.RUN -> "▶"
    LunaActionStepKind.VERIFY -> "✓"
    LunaActionStepKind.ITERATE -> "↻"
    LunaActionStepKind.SUMMARIZE -> "◐"
}

/**
 * Constrói um [LunaActionRun] a partir dos steps wire (Firestore / demo).
 * Só vira pesquisa profunda se houver ferramenta de web.
 */
fun buildActionRunFromWire(
    id: String,
    title: String,
    wireSteps: List<WireToolStep>,
    status: LunaActionRunStatus = LunaActionRunStatus.DONE,
): LunaActionRun {
    val steps = wireSteps.mapIndexed { index, wire ->
        val meta = toolMeta(wire.ferramenta)
        val arg = wire.argumento
        LunaActionStep(
            id = "step-$index",
            label = if (wire.sucesso != false) meta.done(arg) else meta.live(arg),
            detail = arg.takeIf { it.isNotBlank() },
            status = when {
                wire.sucesso == false -> LunaActionStepStatus.ERROR
                else -> LunaActionStepStatus.DONE
            },
            kind = meta.kind,
            queries = listOfNotNull(arg.takeIf { it.isNotBlank() && meta.kind == LunaActionStepKind.SEARCH }),
            sources = wire.fontes,
            citations = wire.fontes.mapIndexed { ci, fonte ->
                LunaCitacao(
                    id = "c-$index-$ci",
                    index = ci + 1,
                    sourceId = fonte.id,
                    title = fonte.title,
                    url = fonte.url,
                )
            },
            ferramenta = wire.ferramenta,
        )
    }
    val usouWeb = wireSteps.any { ehFerramentaDeWeb(it.ferramenta) }
    return LunaActionRun(
        id = id,
        title = title,
        status = status,
        steps = steps,
        profile = if (usouWeb) LunaActionProfile.DEEP_RESEARCH else LunaActionProfile.TASK,
    )
}

data class WireToolStep(
    val ferramenta: String,
    val argumento: String = "",
    val sucesso: Boolean? = true,
    val fontes: List<LunaWebFonte> = emptyList(),
)

/** Compat: converte o modelo antigo de pesquisa para ActionRun (sempre deep-research). */
fun LunaResearchRun.toDeepResearchActionRun(): LunaActionRun = LunaActionRun(
    id = id,
    title = title,
    status = when (status) {
        LunaResearchRunStatus.RUNNING -> LunaActionRunStatus.RUNNING
        LunaResearchRunStatus.DONE -> LunaActionRunStatus.DONE
    },
    steps = steps.map { step ->
        LunaActionStep(
            id = step.id,
            label = step.label,
            detail = step.detail,
            status = when (step.status) {
                LunaResearchStepStatus.PENDING -> LunaActionStepStatus.PENDING
                LunaResearchStepStatus.RUNNING -> LunaActionStepStatus.RUNNING
                LunaResearchStepStatus.DONE -> LunaActionStepStatus.DONE
                LunaResearchStepStatus.ERROR -> LunaActionStepStatus.ERROR
            },
            kind = when (step.kind) {
                LunaResearchStepKind.PLAN -> LunaActionStepKind.PLAN
                LunaResearchStepKind.SEARCH -> LunaActionStepKind.SEARCH
                LunaResearchStepKind.READ -> LunaActionStepKind.READ
                LunaResearchStepKind.VERIFY -> LunaActionStepKind.VERIFY
                LunaResearchStepKind.SUMMARIZE -> LunaActionStepKind.SUMMARIZE
                LunaResearchStepKind.WRITE -> LunaActionStepKind.WRITE
            },
            queries = step.queries,
            sources = step.sources,
            citations = step.citations,
        )
    },
    profile = LunaActionProfile.DEEP_RESEARCH,
)

fun LunaActionRun.toLegacyResearchRun(): LunaResearchRun? {
    if (!isDeepResearch()) return null
    return LunaResearchRun(
        id = id,
        title = title,
        status = when (status) {
            LunaActionRunStatus.RUNNING -> LunaResearchRunStatus.RUNNING
            LunaActionRunStatus.DONE, LunaActionRunStatus.ERROR -> LunaResearchRunStatus.DONE
        },
        steps = webSteps().map { step ->
            LunaResearchStep(
                id = step.id,
                label = step.label,
                detail = step.detail,
                kind = when (step.kind) {
                    LunaActionStepKind.PLAN -> LunaResearchStepKind.PLAN
                    LunaActionStepKind.SEARCH -> LunaResearchStepKind.SEARCH
                    LunaActionStepKind.READ -> LunaResearchStepKind.READ
                    LunaActionStepKind.VERIFY -> LunaResearchStepKind.VERIFY
                    LunaActionStepKind.SUMMARIZE -> LunaResearchStepKind.SUMMARIZE
                    LunaActionStepKind.WRITE -> LunaResearchStepKind.WRITE
                    else -> LunaResearchStepKind.SEARCH
                },
                status = when (step.status) {
                    LunaActionStepStatus.PENDING -> LunaResearchStepStatus.PENDING
                    LunaActionStepStatus.RUNNING -> LunaResearchStepStatus.RUNNING
                    LunaActionStepStatus.DONE, LunaActionStepStatus.SKIPPED -> LunaResearchStepStatus.DONE
                    LunaActionStepStatus.ERROR -> LunaResearchStepStatus.ERROR
                },
                queries = step.queries,
                sources = step.sources,
                citations = step.citations,
            )
        },
    )
}
