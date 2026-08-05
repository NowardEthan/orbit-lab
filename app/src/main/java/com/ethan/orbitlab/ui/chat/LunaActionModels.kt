package com.ethan.orbitlab.ui.chat

/**
 * Modelo unificado de ações da Luna — espelho de `orbit-mobile/src/lib/lunaActionModel.ts`.
 *
 * Timeline agentica first: web (`web_search` / `ler_url` / `verificar_fontes`) entra no
 * mesmo fio das outras tools. O profile DEEP_RESEARCH ainda marca o run no wire, mas a UI
 * não abre painel/dossiê separado.
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
    /** A6.4 — `neuronio` = subagente; default/null = mão da Luna. */
    val papelUi: String? = null,
    val neuronioEspecialidade: String? = null,
)

/** Um passo do plano do turno (SSE `tipo: "plano"` / coleira `planejar`). */
data class PassoPlano(
    val texto: String,
    val feito: Boolean = false,
)

/**
 * Segmento do fio agentico (estilo Cursor): narração e tools na ordem em que chegaram.
 * Vazio em [LunaActionRun.fluxo] = legado (timeline em cima, texto embaixo).
 */
sealed class LunaTurnoSegmento {
    data class Narracao(val texto: String) : LunaTurnoSegmento()
    data class Acao(val stepId: String) : LunaTurnoSegmento()
}

data class LunaActionRun(
    val id: String,
    val title: String,
    val status: LunaActionRunStatus,
    val steps: List<LunaActionStep>,
    val profile: LunaActionProfile,
    /** Checklist viva do turno — vazia se a Luna não chamou `planejar`. */
    val plano: List<PassoPlano> = emptyList(),
    /** Ordem cronológica narração↔tools (SSE). */
    val fluxo: List<LunaTurnoSegmento> = emptyList(),
)

/** Meta-tools da coleira: a checklist *é* a UI delas — não repetir na timeline. */
fun ehFerramentaDePlano(ferramenta: String): Boolean =
    ferramenta == "planejar" ||
        ferramenta == "concluir_passo" ||
        ferramenta == "adicionar_passo" ||
        ferramenta == "plano"

fun LunaActionRun.isDeepResearch(): Boolean =
    profile == LunaActionProfile.DEEP_RESEARCH

/**
 * Há uma imagem sendo desenhada/editada AGORA neste run? (passo de `gerar_imagem`/`editar_imagem`
 * ainda RUNNING). Devolve o rótulo do gesto — "Desenhando" ou "Ajustando" — pro cartão-fantasma,
 * ou `null` se não há imagem em geração. É o gatilho do placeholder estilo ChatGPT.
 */
fun LunaActionRun.imagemEmGeracao(): String? {
    val passo = steps.lastOrNull {
        it.status == LunaActionStepStatus.RUNNING &&
            (it.ferramenta == "gerar_imagem" || it.ferramenta == "editar_imagem")
    } ?: return null
    return if (passo.ferramenta == "editar_imagem") "Ajustando" else "Desenhando"
}

fun LunaActionRun.toolSteps(): List<LunaActionStep> =
    steps.filter { step ->
        val f = step.ferramenta
        // Meta-tools do plano ficam só na checklist. Web entra na timeline agentica.
        f == null || !ehFerramentaDePlano(f)
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
    "gerir_meta" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { "Ajustando meta" },
        done = { "Meta atualizada" },
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
            if (h.isBlank()) "Criando o documento…" else "Criando \"$h\"…"
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
            if (h.isBlank()) "Atualizando o documento…" else "Atualizando \"$h\"…"
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
            if (h.isBlank()) "Ajustando o documento…" else "Ajustando \"$h\"…"
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Documento ajustado" else "Ajustou \"$h\""
        },
    )
    "inserir_blocos" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Acrescentando no documento…" else "Acrescentando em \"$h\"…"
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Acrescentou no documento" else "Acrescentou em \"$h\""
        },
    )
    "editar_bloco_artefato" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Editando o bloco…" else "Editando bloco em \"$h\"…"
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Editou o bloco" else "Editou bloco em \"$h\""
        },
    )
    "ler_bloco" -> ToolMeta(
        kind = LunaActionStepKind.SUMMARIZE,
        live = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Lendo o bloco…" else "Lendo bloco de \"$h\"…"
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Leu o bloco" else "Leu bloco de \"$h\""
        },
    )
    "ler_artefato" -> ToolMeta(
        kind = LunaActionStepKind.SUMMARIZE,
        live = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Lendo o documento…" else "Lendo \"$h\"…"
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Leu o documento" else "Leu \"$h\""
        },
    )
    "ler_estrutura" -> ToolMeta(
        kind = LunaActionStepKind.SUMMARIZE,
        live = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Consultando a estrutura…" else "Consultando estrutura de \"$h\"…"
        },
        done = { arg ->
            val h = resolverTituloHumano(arg)
            if (h.isBlank()) "Consultou a estrutura" else "Consultou estrutura de \"$h\""
        },
    )
    "ler_secao" -> ToolMeta(
        kind = LunaActionStepKind.SUMMARIZE,
        live = { arg -> if (arg.isBlank()) "Lendo a seção…" else "Lendo \"$arg\"…" },
        done = { arg -> if (arg.isBlank()) "Leu a seção" else "Leu \"$arg\"" },
    )
    "buscar_no_artefato" -> ToolMeta(
        kind = LunaActionStepKind.SUMMARIZE,
        live = { arg -> if (arg.isBlank()) "Buscando no documento…" else "Buscando por \"$arg\"…" },
        done = { arg -> if (arg.isBlank()) "Buscou no documento" else "Buscou por \"$arg\"" },
    )
    "gerar_imagem" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { "Desenhando a imagem" },
        done = { "Desenhou a imagem" },
    )
    "editar_imagem" -> ToolMeta(
        kind = LunaActionStepKind.WRITE,
        live = { "Ajustando a imagem" },
        done = { "Ajustou a imagem" },
    )
    "perguntar" -> ToolMeta(
        kind = LunaActionStepKind.PLAN,
        live = { "Pensando numa pergunta" },
        done = { "Te perguntou algo" },
    )
    "anotar_canone" -> ToolMeta(
        kind = LunaActionStepKind.MEMORY,
        live = { arg ->
            when {
                arg.contains("apagar", ignoreCase = true) -> "Apagando do cânone…"
                arg.contains("editar", ignoreCase = true) -> "Editando o cânone…"
                arg.contains("ler", ignoreCase = true) -> "Lendo o cânone…"
                arg.contains("limpar", ignoreCase = true) -> "Limpando o cânone…"
                else -> "Anotando no cânone…"
            }
        },
        done = { arg ->
            when {
                arg.contains("apagar", ignoreCase = true) -> "Apagou do cânone"
                arg.contains("editar", ignoreCase = true) -> "Editou o cânone"
                arg.contains("ler", ignoreCase = true) -> "Leu o cânone"
                arg.contains("limpar", ignoreCase = true) -> "Limpou o cânone"
                else -> "Anotou no cânone"
            }
        },
    )
    "consultar_neuronio" -> ToolMeta(
        kind = LunaActionStepKind.REASON,
        live = { arg ->
            when {
                arg.contains("auditoria", ignoreCase = true) -> "Consultando Auditoria…"
                arg.contains("canone", ignoreCase = true) ||
                    arg.contains("cânone", ignoreCase = true) -> "Consultando Cânone…"
                arg.contains("pesquisa", ignoreCase = true) -> "Consultando Pesquisa…"
                else -> "Consultando Orientação…"
            }
        },
        done = { arg ->
            when {
                arg.contains("auditoria", ignoreCase = true) -> "Consultou Auditoria"
                arg.contains("canone", ignoreCase = true) ||
                    arg.contains("cânone", ignoreCase = true) -> "Consultou Cânone"
                arg.contains("pesquisa", ignoreCase = true) -> "Consultou Pesquisa"
                else -> "Consultou Orientação"
            }
        },
    )
    "listar_artefatos" -> ToolMeta(
        kind = LunaActionStepKind.MEMORY,
        live = { "Consultando a estante…" },
        done = { "Consultou a estante" },
    )
    else -> ToolMeta(
        kind = LunaActionStepKind.RUN,
        live = { "Executando ação…" },
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
