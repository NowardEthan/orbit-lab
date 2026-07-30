package com.ethan.orbitlab.ui.chat

/**
 * Pós-correção do registro do Modo técnico.
 *
 * O modelo (deepseek) NÃO obedece o registro formal por system prompt de forma confiável —
 * medido no harness p20: ele abre casual («boa!», «olha só,», «claro!») e às vezes vem tudo
 * em caixa-baixa, mesmo com a diretriz mandando o contrário. Baixar a temperatura piora (colapsa
 * na voz dominante, que na Luna é a casual). Como o defeito é de SUPERFÍCIE (a estrutura e a
 * profundidade quase sempre vêm), dá pra consertar em pós — na hora de EXIBIR, sem mexer no que
 * está gravado no Firestore.
 *
 * Faz duas coisas e só isso: (1) tira a saudação/interjeição casual da abertura; (2) capitaliza
 * os inícios de frase. NÃO inventa rigor. É puro e idempotente — rodar de novo num texto já
 * formal não muda nada — e protege blocos de código (``` e `inline`) pra não estragar termos.
 */

/** Interjeições puras — seguras de tirar mesmo sem pontuação («aaah», «eita», «kkk»). */
private val INTERJEICAO = Regex(
    "^\\s*(?:a{1,}h+|aa+h|eita|opa|nossa|caramba|uau|humm+|hmm+|haha+|hehe+|kk+|rs{2,})" +
        "\\b[\\s,!.:;…—–-]*",
    RegexOption.IGNORE_CASE,
)

/**
 * Marcadores de discurso casuais — só tiro quando vêm fechados por pontuação («olha só,»,
 * «claro!»), pra nunca comer conteúdo de verdade («boa parte das empresas…» fica intacto).
 * Sem «bem» de propósito: pegaria «bem-vindo».
 */
private val MARCADOR = Regex(
    "^\\s*(?:" +
        "boa(?: pergunta| noite| tarde| dia)?|" +
        "olha(?: só| aqui)?|olá|" +
        "ótim[oa]|excelente|perfeito|legal|massa|show|maravilha|" +
        "claro|com certeza|certo|beleza|blz|tranquilo|" +
        "então|pois é|pois então|" +
        "cara|mano|rapaz|saca|" +
        "vamos lá|bora|deixa eu te (?:ajudar|explicar)[^,.!:;]*|deixa comigo" +
        ")\\s*[,!.:;…—–-]+\\s*",
    RegexOption.IGNORE_CASE,
)

/** Chars que abrem uma linha/frase sem serem a primeira letra (markdown, aspas, parênteses). */
private fun ehMarcadorInicio(c: Char): Boolean =
    c == '#' || c == '>' || c == '-' || c == '*' || c == '+' || c == '_' || c == '~' ||
        c == '—' || c == '–' || c == '•' || c == '·' || c == '▪' ||
        c == '[' || c == '(' || c == '"' || c == '\'' || c == '“' || c == '”' ||
        c == '‘' || c == '’' || c == '«'

/** Tira a abertura casual, peelando em cadeia («aaah ótimo! então …» → «então …»). */
private fun tirarAberturaCasual(bruto: String): String {
    var s = bruto.trimStart()
    var guarda = 0
    while (guarda++ < 6) {
        val m = INTERJEICAO.find(s) ?: MARCADOR.find(s) ?: break
        val resto = s.substring(m.range.last + 1)
        // Nunca zera a resposta: se sobrar quase nada, o «casual» era o conteúdo — para.
        if (resto.trim().length < 12) break
        s = resto
    }
    return s.trimStart()
}

/** Capitaliza inícios de frase (e de linha), pulando código e marcadores markdown. */
private fun capitalizarInicios(texto: String): String {
    val sb = StringBuilder(texto.length)
    var inFence = false      // dentro de ``` … ```
    var inInline = false     // dentro de `…`
    var inicioFrase = true   // esperando a 1ª letra de uma frase/linha
    var i = 0
    val n = texto.length
    while (i < n) {
        val c = texto[i]

        if (c == '`') {
            var j = i
            while (j < n && texto[j] == '`') j++
            val run = j - i
            if (run >= 3) inFence = !inFence else if (!inFence) inInline = !inInline
            sb.append(texto, i, j)
            inicioFrase = false // código conta como conteúdo visto
            i = j
            continue
        }

        if (inFence || inInline) {
            sb.append(c)
            i++
            continue
        }

        if (c == '\n') {
            sb.append(c)
            inicioFrase = true
            inInline = false // crase inline não cruza linha
            i++
            continue
        }

        if (inicioFrase) {
            when {
                c.isWhitespace() || ehMarcadorInicio(c) -> {
                    sb.append(c); i++
                }
                c.isDigit() -> {
                    // Lista ordenada «1.» / «2)» — pula o marcador e segue esperando a letra.
                    var j = i
                    while (j < n && texto[j].isDigit()) j++
                    if (j < n && (texto[j] == '.' || texto[j] == ')') &&
                        j + 1 < n && texto[j + 1].isWhitespace()
                    ) {
                        sb.append(texto, i, j + 1)
                        i = j + 1
                    } else {
                        // Número é conteúdo de verdade (ex.: «2026 foi…») — não capitaliza depois.
                        sb.append(texto, i, j)
                        inicioFrase = false
                        i = j
                    }
                }
                c.isLetter() -> {
                    sb.append(c.uppercaseChar())
                    inicioFrase = false
                    i++
                }
                else -> { // emoji/símbolo: mantém e segue esperando a letra
                    sb.append(c); i++
                }
            }
            continue
        }

        sb.append(c)
        if (c == '.' || c == '!' || c == '?' || c == '…') {
            val prox = if (i + 1 < n) texto[i + 1] else ' '
            if (prox == ' ' || prox == '\t' || prox == '\n') inicioFrase = true
        }
        i++
    }
    return sb.toString()
}

/**
 * Deixa o texto no registro formal esperado no Modo técnico: sem abertura casual e com os
 * inícios de frase capitalizados. Idempotente e não-destrutivo — feito pra rodar na exibição.
 */
fun formalizarTecnico(bruto: String): String {
    if (bruto.isBlank()) return bruto
    return capitalizarInicios(tirarAberturaCasual(bruto))
}
