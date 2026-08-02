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
    CHECK,
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
    steps.filter { step ->
        val f = step.ferramenta
        f == null || !ehFerramentaDeWeb(f)
    }

fun LunaActionRun.webSteps(): List<LunaActionStep> =
    steps.filter { step ->
        val f = step.ferramenta
        f != null && ehFerramentaDeWeb(f)
    }

data class ToolMeta(
    val kind: LunaActionStepKind,
    val live: (String) -> String,
    val done: (String) -> String,
)

fun pareceIdHash(texto: String): Boolean {
    val t = texto.trim()
    if (t.length !in 12..45) return false
    if (t.contains(" ") || t.contains("\n") || t.contains("/") || t.contains(".")) return false
    return t.matches(Regex("^[a-zA-Z0-9_-]+$")) && t.any { it.isDigit() } && t.any { it.isUpperCase() }
}

fun resolverTituloHumano(idOuTitulo: String): String {
    val t = idOuTitulo.trim()
    if (t.isBlank()) return ""
    // Se contém quebras de linha, tags markdown (#, ##, **), ou é um texto longo, não é um título!
    if (t.contains("\n") || t.startsWith("#") || t.startsWith("```") || t.length > 50) {
        val primeiraLinha = t.lineSequence().firstOrNull().orEmpty().trim()
            .removePrefix("#").removePrefix("#").removePrefix("#").trim()
        if (primeiraLinha.isNotBlank() && primeiraLinha.length in 3..40 && !primeiraLinha.contains("\n") && !pareceIdHash(primeiraLinha)) {
            return primeiraLinha
        }
        return ""
    }
    val cached = com.ethan.orbitlab.data.firebase.FirestoreDocumentos.obterTitulo(t)
    if (!cached.isNullOrBlank()) return cached
    if (pareceIdHash(t)) return ""
    return t
}

fun toolMeta(ferramenta: String): ToolMeta = when (ferramenta) {
    "web_search" -> ToolMeta(
        kind = LunaActionStepKind.SEARCH,
        live = { arg -> if (arg.isBlank()) "Pesquisando na web" else "Pesquisando \"$arg\"" },
        done = { arg -> if (arg.isBlank()) "Pesquisa na web" else "Pesquisou \"$arg\"" },
    )
    "ler_url" -> ToolMeta(
        kind = LunaActionStepKind.READ,
        live = { arg -> if (arg.isBlank()) "Lendo o link" else "Lendo $arg" },
        done = { arg -> if (arg.isBlank()) "Leitura de link" else "Leu $arg" },
    )
    "ver_imagem" -> ToolMeta(
        kind = LunaActionStepKind.VISION,
        live = { "Analisando a imagem" },
        done = { "Analisou a imagem" },
    )
    "ver_video" -> ToolMeta(
        kind = LunaActionStepKind.VIDEO,
        live = { "Analisando o vídeo" },
        done = { "Analisou o vídeo" },
    )
    "consultar_atlas" -> ToolMeta(
        kind = LunaActionStepKind.MEMORY,
        live = { arg -> if (arg.isBlank()) "Consultando a memória" else "Lembrando de \"$arg\"" },
        done = { arg -> if (arg.isBlank()) "Consultou a memória" else "Lembrou de \"$arg\"" },
    )
    "verificar_fontes" -> ToolMeta(
        kind = LunaActionStepKind.VERIFY,
        live = { "Cruzando as fontes" },
        done = { "Cruzou as fontes" },
    )
    "planejar" -> ToolMeta(
        kind = LunaActionStepKind.PLAN,
        live = { "Traçando o plano" },
        done = { "Traçou o plano" },
    )
    "concluir_passo" -> ToolMeta(
        kind = LunaActionStepKind.CHECK,
        live = { "Concluindo o passo" },
        done = { arg -> if (arg.isBlank()) "Passo concluído" else "Passo \"$arg\" concluído" },
    )
    "adicionar_passo" -> ToolMeta(
        kind = LunaActionStepKind.PLAN,
        live = { "Anotando o passo" },
        done = { "Passo adicionado" },
    )
    "registrar_lancamento" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { "Registrando lançamento" },
        done = { "Lançamento registrado" },
    )
    "listar_lancamentos" -> ToolMeta(
        kind = LunaActionStepKind.READ,
        live = { "Lendo o extrato" },
        done = { "Extrato lido" },
    )
    "resumo_financeiro" -> ToolMeta(
        kind = LunaActionStepKind.READ,
        live = { "Lendo as finanças" },
        done = { "Resumo financeiro" },
    )
    "gerir_recorrente" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { "Ajustando recorrente" },
        done = { "Recorrente atualizado" },
    )
    "gerir_carteira" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { "Ajustando carteira" },
        done = { "Carteira atualizada" },
    )
    "transferir" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { "Transferindo" },
        done = { "Transferência feita" },
    )
    "criar_artefato" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Criando o documento" else "Criando \"$h\""
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Documento criado" else "Criou \"$h\""
        },
    )
    "editar_artefato" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Atualizando o documento" else "Atualizando \"$h\""
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Documento atualizado" else "Atualizou \"$h\""
        },
    )
    "editar_trecho_artefato" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Ajustando o documento" else "Ajustando \"$h\""
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Documento ajustado" else "Ajustou \"$h\""
        },
    )
    "ler_artefato" -> ToolMeta(
        kind = LunaActionStepKind.SUMMARIZE,
        live = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Lendo o documento" else "Lendo \"$h\""
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Leu o documento" else "Leu \"$h\""
        },
    )
    "ler_estrutura" -> ToolMeta(
        kind = LunaActionStepKind.SUMMARIZE,
        live = { "Consultando a estrutura" },
        done = { "Consultou a estrutura" },
    )
    "ler_secao" -> ToolMeta(
        kind = LunaActionStepKind.SUMMARIZE,
        live = { arg -> if (arg.isBlank()) "Lendo a seção" else "Lendo \"$arg\"" },
        done = { arg -> if (arg.isBlank()) "Leu a seção" else "Leu \"$arg\"" },
    )
    "buscar_no_artefato" -> ToolMeta(
        kind = LunaActionStepKind.SUMMARIZE,
        live = { arg -> if (arg.isBlank()) "Buscando no documento" else "Buscando por \"$arg\"" },
        done = { arg -> if (arg.isBlank()) "Buscou no documento" else "Buscou por \"$arg\"" },
    )
    "anotar_canone" -> ToolMeta(
        kind = LunaActionStepKind.MEMORY,
        live = { "Guardando anotação" },
        done = { "Anotou no acervo" },
    )
    "listar_artefatos" -> ToolMeta(
        kind = LunaActionStepKind.MEMORY,
        live = { "Consultando a estante" },
        done = { "Consultou a estante" },
    )
    else -> ToolMeta(
        kind = LunaActionStepKind.RUN,
        live = { "Executando ação" },
        done = { "Ação concluída" },
    )
}

/**
 * Só ferramentas de internet abrem o painel «PESQUISA PROFUNDA».
 *
 * Não dá pra olhar o [LunaActionStepKind]: `resumo_financeiro` / `listar_lancamentos`
 * também usam `READ` («ler o extrato»), e isso fazia o Lab pintar grana como pesquisa
 * web — 0 fontes, 0 consultas, modal errado.
 */
fun ehFerramentaDeWeb(ferramenta: String): Boolean =
    ferramenta == "web_search" ||
        ferramenta == "ler_url" ||
        ferramenta == "verificar_fontes"

fun LunaActionStepKind.icone(): String = when (this) {
    LunaActionStepKind.REASON -> "◎"
    LunaActionStepKind.PLAN -> "◈"
    LunaActionStepKind.CHECK -> "✓"
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
