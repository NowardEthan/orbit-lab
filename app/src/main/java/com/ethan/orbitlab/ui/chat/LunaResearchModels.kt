package com.ethan.orbitlab.ui.chat

/**
 * Fontes e citações da web — hoje vivem dentro do fio agêntico (um passo de
 * `web_search`/`ler_url`/`verificar_fontes` em [LunaActionStep]), não num painel
 * de pesquisa à parte. O modelo antigo de dossiê (`LunaResearchRun`/`Step`) foi
 * aposentado com a unificação da timeline.
 */

/** Estado de uma fonte web na timeline. */
enum class LunaFonteStatus {
    ENCONTRADA,
    LENDO,
    LIDA,
    CONFIRMADA,
    DESCARTADA,
    CITADA,
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
