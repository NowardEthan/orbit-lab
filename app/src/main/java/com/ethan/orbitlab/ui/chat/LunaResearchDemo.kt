package com.ethan.orbitlab.ui.chat

/** Fixtures alinhadas ao demo Orbit «Pesquisa profunda · RAG». */

private val SRC_ARXIV = LunaWebFonte(
    id = "src-arxiv",
    title = "Retrieval-Augmented Generation for Technical Documentation",
    url = "https://arxiv.org/abs/2402.05132",
    domain = "arxiv.org",
    snippet = "Survey sobre chunking, re-ranking e eval em corpus técnicos.",
    status = LunaFonteStatus.LIDA,
    publishedAt = "Fev 2025",
)

private val SRC_LLAMAINDEX = LunaWebFonte(
    id = "src-llamaindex",
    title = "Evaluating RAG Pipelines — LlamaIndex Docs",
    url = "https://docs.llamaindex.ai/en/stable/optimizing/evaluation/evaluation/",
    domain = "docs.llamaindex.ai",
    snippet = "Golden datasets, faithfulness e métricas de retrieval.",
    status = LunaFonteStatus.LIDA,
    publishedAt = "Docs",
)

private val SRC_PINECONE = LunaWebFonte(
    id = "src-pinecone",
    title = "Hybrid Search for Production RAG",
    url = "https://www.pinecone.io/learn/hybrid-search/",
    domain = "pinecone.io",
    snippet = "Dense + sparse retrieval para documentação longa.",
    status = LunaFonteStatus.LIDA,
    publishedAt = "Jan 2026",
)

private val SRC_OPENAI = LunaWebFonte(
    id = "src-openai",
    title = "Long context vs RAG — when to use each",
    url = "https://platform.openai.com/docs/guides/retrieval",
    domain = "platform.openai.com",
    snippet = "Guia oficial sobre retrieval e limites de contexto.",
    status = LunaFonteStatus.ENCONTRADA,
    publishedAt = "Docs",
)

private val SRC_ANTHROPIC = LunaWebFonte(
    id = "src-anthropic",
    title = "Building effective agents — tool use patterns",
    url = "https://docs.anthropic.com/en/docs/build-with-claude/tool-use",
    domain = "docs.anthropic.com",
    snippet = "Padrões de pesquisa e citação em fluxos agenticos.",
    status = LunaFonteStatus.ENCONTRADA,
    publishedAt = "Docs",
)

private val SRC_LANGCHAIN = LunaWebFonte(
    id = "src-langchain",
    title = "Graph RAG tutorial",
    url = "https://python.langchain.com/docs/tutorials/graph/",
    domain = "python.langchain.com",
    snippet = "Quando graph RAG compensa vs vector store simples.",
    status = LunaFonteStatus.CONFIRMADA,
    publishedAt = "2025",
)

private val SRC_COHERE = LunaWebFonte(
    id = "src-cohere",
    title = "Reranking for enterprise search",
    url = "https://docs.cohere.com/docs/rerank",
    domain = "docs.cohere.com",
    snippet = "Re-ranking como passo obrigatório em docs API extensas.",
    status = LunaFonteStatus.LIDA,
    publishedAt = "Docs",
)

private val SRC_MILVUS = LunaWebFonte(
    id = "src-milvus",
    title = "Code-aware chunking for OpenAPI specs",
    url = "https://milvus.io/blog/code-aware-chunking-rag.md",
    domain = "milvus.io",
    snippet = "Estratégias para specs OpenAPI e SDK references.",
    status = LunaFonteStatus.LIDA,
    publishedAt = "Dez 2025",
)

private val SRC_DATABRICKS = LunaWebFonte(
    id = "src-databricks",
    title = "Mosaic AI Vector Search in production",
    url = "https://www.databricks.com/blog/mosaic-ai-vector-search",
    domain = "databricks.com",
    snippet = "Casos enterprise com observability e golden sets.",
    status = LunaFonteStatus.ENCONTRADA,
    publishedAt = "Mar 2026",
)

private val SRC_DESCARTADA = LunaWebFonte(
    id = "src-marketing",
    title = "RAG will fix your docs (sponsored)",
    url = "https://example.com/blog/rag-fix-docs",
    domain = "example.com",
    snippet = "Artigo sem métricas — candidato a descarte.",
    status = LunaFonteStatus.DESCARTADA,
    publishedAt = "2024",
)

private val CITACOES = listOf(
    LunaCitacao(
        id = "cite-1",
        index = 1,
        sourceId = SRC_ARXIV.id,
        title = SRC_ARXIV.title,
        url = SRC_ARXIV.url,
        excerpt = "Hybrid retrieval + re-rank como baseline em docs técnicas longas.",
    ),
    LunaCitacao(
        id = "cite-2",
        index = 2,
        sourceId = SRC_PINECONE.id,
        title = SRC_PINECONE.title,
        url = SRC_PINECONE.url,
        excerpt = "Sparse+dense melhora recall em terminologia de API.",
    ),
    LunaCitacao(
        id = "cite-3",
        index = 3,
        sourceId = SRC_LLAMAINDEX.id,
        title = SRC_LLAMAINDEX.title,
        url = SRC_LLAMAINDEX.url,
        excerpt = "Faithfulness e answer relevance como métricas mínimas.",
    ),
    LunaCitacao(
        id = "cite-4",
        index = 4,
        sourceId = SRC_LANGCHAIN.id,
        title = SRC_LANGCHAIN.title,
        url = SRC_LANGCHAIN.url,
        excerpt = "Graph RAG justifica-se com relações explícitas entre entidades.",
    ),
)

val DEMO_PESQUISA_USER =
    "Faz uma pesquisa profunda sobre RAG para documentação técnica — estado da arte, trade-offs e quem lidera em 2025–2026."

fun demoReasoningPesquisaProfunda(): String = """
    Pedido de síntese com várias perspectivas.
    Antes de abrir o browser, decomponho em sub-perguntas verificáveis.
    Critérios: recência, fonte primária, reputação do domínio.
""".trimIndent()

fun demoMarkdownPesquisaProfunda(): String = """
Concluí a pesquisa profunda sobre **RAG para documentação técnica**. Sintetizei arquitecturas actuais, trade-offs de latência/qualidade e práticas de avaliação em produção.

## Highlights

- Hybrid search + re-ranking continua o baseline sólido para docs API longas [[arxiv](https://arxiv.org/abs/2402.05132)] [[pinecone](https://www.pinecone.io/learn/hybrid-search/)].
- Graph RAG só compensa quando há relações explícitas entre entidades do domínio [[langchain](https://python.langchain.com/docs/tutorials/graph/)].
- Eval contínuo (golden sets + tracing) separa stacks maduras de demos [[llamaindex](https://docs.llamaindex.ai/en/stable/optimizing/evaluation/evaluation/)].

## Arquitectura recomendada

1. Chunking consciente de estrutura (OpenAPI / headings).
2. Retrieval híbrido + re-rank.
3. Observability com golden sets em produção.

> Expande a timeline acima para rever **fontes**, **consultas** e **referências** por fase.
""".trimIndent()

/** Run completo — estado final «Relatório pronto». */
fun demoResearchRunPronto(): LunaResearchRun = LunaResearchRun(
    id = "run-deep-research-rag",
    title = "Pesquisa profunda · RAG em docs técnicos",
    status = LunaResearchRunStatus.DONE,
    steps = listOf(
        LunaResearchStep(
            id = "dr-plan",
            label = "Definir estratégia e sub-perguntas",
            detail = "4 sub-perguntas · critérios: recência, primária, reputação",
            kind = LunaResearchStepKind.PLAN,
            status = LunaResearchStepStatus.DONE,
        ),
        LunaResearchStep(
            id = "dr-search-1",
            label = "Pesquisar na web",
            detail = "12 resultados · 5 fontes selecionadas",
            kind = LunaResearchStepKind.SEARCH,
            status = LunaResearchStepStatus.DONE,
            queries = listOf(
                "RAG technical documentation 2025",
                "retrieval augmented generation evaluation benchmark",
                "graph RAG developer docs",
            ),
            sources = listOf(
                SRC_ARXIV.copy(status = LunaFonteStatus.ENCONTRADA),
                SRC_LLAMAINDEX.copy(status = LunaFonteStatus.ENCONTRADA),
                SRC_PINECONE.copy(status = LunaFonteStatus.ENCONTRADA),
                SRC_LANGCHAIN.copy(status = LunaFonteStatus.ENCONTRADA),
                SRC_OPENAI.copy(status = LunaFonteStatus.ENCONTRADA),
            ),
        ),
        LunaResearchStep(
            id = "dr-read-1",
            label = "Ler fontes prioritárias",
            detail = "5 páginas · ~18 min de leitura efectiva",
            kind = LunaResearchStepKind.READ,
            status = LunaResearchStepStatus.DONE,
            sources = listOf(
                SRC_ARXIV.copy(status = LunaFonteStatus.LIDA),
                SRC_LLAMAINDEX.copy(status = LunaFonteStatus.LIDA),
                SRC_PINECONE.copy(status = LunaFonteStatus.LIDA),
            ),
        ),
        LunaResearchStep(
            id = "dr-search-2",
            label = "Aprofundar lacunas",
            detail = "8 resultados · 3 fontes novas",
            kind = LunaResearchStepKind.SEARCH,
            status = LunaResearchStepStatus.DONE,
            queries = listOf(
                "long context vs RAG API documentation",
                "code-aware chunking openapi",
            ),
            sources = listOf(
                SRC_COHERE.copy(status = LunaFonteStatus.ENCONTRADA),
                SRC_MILVUS.copy(status = LunaFonteStatus.ENCONTRADA),
                SRC_ANTHROPIC.copy(status = LunaFonteStatus.ENCONTRADA),
            ),
        ),
        LunaResearchStep(
            id = "dr-verify",
            label = "Cruzar fontes e fact-check",
            detail = "3 claims validadas · 1 fonte rebaixada",
            kind = LunaResearchStepKind.VERIFY,
            status = LunaResearchStepStatus.DONE,
            sources = listOf(
                SRC_LANGCHAIN.copy(status = LunaFonteStatus.CONFIRMADA),
                SRC_LLAMAINDEX.copy(status = LunaFonteStatus.CONFIRMADA),
                SRC_PINECONE.copy(status = LunaFonteStatus.CONFIRMADA),
                SRC_DESCARTADA,
            ),
        ),
        LunaResearchStep(
            id = "dr-search-3",
            label = "Última varredura",
            detail = "6 resultados · 1 fonte enterprise",
            kind = LunaResearchStepKind.SEARCH,
            status = LunaResearchStepStatus.DONE,
            queries = listOf(
                "enterprise RAG documentation stack 2026",
                "RAG observability tracing production",
            ),
            sources = listOf(SRC_DATABRICKS.copy(status = LunaFonteStatus.ENCONTRADA)),
        ),
        LunaResearchStep(
            id = "dr-synthesize",
            label = "Sintetizar achados",
            detail = "Esboço · 6 secções",
            kind = LunaResearchStepKind.SUMMARIZE,
            status = LunaResearchStepStatus.DONE,
        ),
        LunaResearchStep(
            id = "dr-write",
            label = "Redigir relatório com citações",
            detail = "Markdown · 4 referências principais",
            kind = LunaResearchStepKind.WRITE,
            status = LunaResearchStepStatus.DONE,
            citations = CITACOES,
            sources = CITACOES.map {
                LunaWebFonte(
                    id = it.sourceId,
                    title = it.title,
                    url = it.url,
                    domain = it.url.removePrefix("https://").substringBefore('/'),
                    status = LunaFonteStatus.CITADA,
                )
            },
        ),
    ),
)

/** Snapshots progressivos pra animar o stream (índice = passos concluídos). */
fun demoResearchSnapshots(): List<Pair<String, LunaResearchRun>> {
    val pronto = demoResearchRunPronto()
    val steps = pronto.steps
    return steps.indices.map { i ->
        val live = when (steps[i].kind) {
            LunaResearchStepKind.SEARCH ->
                "Pesquisando \"${steps[i].queries.firstOrNull() ?: steps[i].label}\"…"
            LunaResearchStepKind.READ ->
                "Lendo ${steps[i].sources.firstOrNull()?.domain ?: "fontes"}…"
            LunaResearchStepKind.VERIFY -> "Cruzando fontes e fact-check…"
            LunaResearchStepKind.PLAN -> "Definindo estratégia…"
            LunaResearchStepKind.SUMMARIZE -> "Sintetizando achados…"
            LunaResearchStepKind.WRITE -> "Redigindo relatório…"
        }
        val parcial = pronto.copy(
            status = LunaResearchRunStatus.RUNNING,
            steps = steps.mapIndexed { idx, step ->
                when {
                    idx < i -> step.copy(status = LunaResearchStepStatus.DONE)
                    idx == i -> step.copy(status = LunaResearchStepStatus.RUNNING)
                    else -> step.copy(
                        status = LunaResearchStepStatus.PENDING,
                        sources = emptyList(),
                        citations = emptyList(),
                        queries = if (step.kind == LunaResearchStepKind.SEARCH) step.queries else emptyList(),
                    )
                }
            },
        )
        live to parcial
    } + ("Relatório pronto" to pronto)
}

fun pedidoParecePesquisaProfunda(texto: String): Boolean {
    val t = texto.lowercase()
    return t.contains("pesquisa profunda") ||
        t.contains("pesquisa na internet") ||
        t.contains("pesquisa na web") ||
        (t.contains("pesquisa") && (t.contains("rag") || t.contains("fontes") || t.contains("web")))
}
