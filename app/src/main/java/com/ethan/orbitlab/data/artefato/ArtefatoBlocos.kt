package com.ethan.orbitlab.data.artefato

/**
 * Contrato de blocos do artefato — espelho de
 * `luna-core/src/ferramentas/artefatoBlocos.ts` (Notion da Luna — N0).
 *
 * Markdown continua como projeção (`conteudo`) pra export e tools legadas.
 */

const val SCHEMA_ARTEFATO_MD = 1
const val SCHEMA_ARTEFATO_BLOCOS = 2

enum class TipoBlocoArtefato {
    paragraph,
    heading,
    bullet,
    numbered,
    todo,
    quote,
    code,
    divider,
    callout,
    ;

    companion object {
        fun from(raw: String?): TipoBlocoArtefato =
            entries.firstOrNull { it.name == raw } ?: paragraph
    }
}

data class PropsBlocoArtefato(
    val level: Int? = null,
    val checked: Boolean? = null,
    val language: String? = null,
)

data class BlocoArtefato(
    val id: String,
    val type: TipoBlocoArtefato,
    val text: String = "",
    val props: PropsBlocoArtefato? = null,
)

private var seqBloco = 0

fun novoIdBloco(): String {
    seqBloco = (seqBloco + 1) % 1_000_000
    val t = System.currentTimeMillis().toString(36)
    val r = (100000..999999).random().toString(36)
    return "b_${t}_${r}_$seqBloco"
}

fun blocosToMd(blocos: List<BlocoArtefato>): String {
    if (blocos.isEmpty()) return ""
    val out = ArrayList<String>()
    var numbered = 0
    for (b in blocos) {
        when (b.type) {
            TipoBlocoArtefato.heading -> {
                val nivel = (b.props?.level ?: 1).coerceIn(1, 3)
                out += "${"#".repeat(nivel)} ${b.text}".trimEnd()
                numbered = 0
            }
            TipoBlocoArtefato.bullet -> {
                out += "- ${b.text}"
                numbered = 0
            }
            TipoBlocoArtefato.numbered -> {
                numbered += 1
                out += "$numbered. ${b.text}"
            }
            TipoBlocoArtefato.todo -> {
                val mark = if (b.props?.checked == true) "x" else " "
                out += "- [$mark] ${b.text}"
                numbered = 0
            }
            TipoBlocoArtefato.quote, TipoBlocoArtefato.callout -> {
                out += b.text.lines().joinToString("\n") { "> $it" }
                numbered = 0
            }
            TipoBlocoArtefato.code -> {
                val lang = b.props?.language?.trim().orEmpty()
                out += "```$lang\n${b.text}\n```"
                numbered = 0
            }
            TipoBlocoArtefato.divider -> {
                out += "---"
                numbered = 0
            }
            TipoBlocoArtefato.paragraph -> {
                out += b.text
                numbered = 0
            }
        }
    }
    val joined = out.joinToString("\n\n").replace(Regex("\n{3,}"), "\n\n").trim()
    return if (joined.isEmpty()) "" else "$joined\n"
}

fun mdToBlocos(md: String): List<BlocoArtefato> {
    val texto = md.replace("\r\n", "\n")
    if (texto.isBlank()) {
        return listOf(BlocoArtefato(id = novoIdBloco(), type = TipoBlocoArtefato.paragraph, text = ""))
    }

    val linhas = texto.split("\n")
    val blocos = ArrayList<BlocoArtefato>()
    var i = 0
    val paraBuf = ArrayList<String>()

    fun flushPara() {
        if (paraBuf.isEmpty()) return
        val t = paraBuf.joinToString("\n").trimEnd()
        paraBuf.clear()
        if (t.isEmpty()) return
        blocos += BlocoArtefato(id = novoIdBloco(), type = TipoBlocoArtefato.paragraph, text = t)
    }

    while (i < linhas.size) {
        val linha = linhas[i]
        val trim = linha.trim()

        if (trim.startsWith("```") || trim.startsWith("~~~")) {
            flushPara()
            val fence = trim.take(3)
            val language = trim.drop(3).trim()
            i += 1
            val codeLines = ArrayList<String>()
            while (i < linhas.size && !linhas[i].trim().startsWith(fence)) {
                codeLines += linhas[i]
                i += 1
            }
            if (i < linhas.size) i += 1
            blocos += BlocoArtefato(
                id = novoIdBloco(),
                type = TipoBlocoArtefato.code,
                text = codeLines.joinToString("\n"),
                props = if (language.isNotEmpty()) PropsBlocoArtefato(language = language) else null,
            )
            continue
        }

        if (Regex("^---+$").matches(trim) || Regex("^\\*\\*\\*+$").matches(trim)) {
            flushPara()
            blocos += BlocoArtefato(id = novoIdBloco(), type = TipoBlocoArtefato.divider, text = "")
            i += 1
            continue
        }

        val h = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$").find(linha)
        if (h != null) {
            flushPara()
            val nivel = h.groupValues[1].length.coerceAtMost(3)
            blocos += BlocoArtefato(
                id = novoIdBloco(),
                type = TipoBlocoArtefato.heading,
                text = h.groupValues[2].trim(),
                props = PropsBlocoArtefato(level = nivel),
            )
            i += 1
            continue
        }

        val todo = Regex("^(\\s*)[-*+]\\s+\\[([ xX])]\\s+(.*)$").find(linha)
        if (todo != null) {
            flushPara()
            blocos += BlocoArtefato(
                id = novoIdBloco(),
                type = TipoBlocoArtefato.todo,
                text = todo.groupValues[3],
                props = PropsBlocoArtefato(checked = todo.groupValues[2].equals("x", ignoreCase = true)),
            )
            i += 1
            continue
        }

        val bullet = Regex("^(\\s*)[-*+]\\s+(.*)$").find(linha)
        if (bullet != null) {
            flushPara()
            blocos += BlocoArtefato(
                id = novoIdBloco(),
                type = TipoBlocoArtefato.bullet,
                text = bullet.groupValues[2],
            )
            i += 1
            continue
        }

        val num = Regex("^(\\s*)\\d+\\.\\s+(.*)$").find(linha)
        if (num != null) {
            flushPara()
            blocos += BlocoArtefato(
                id = novoIdBloco(),
                type = TipoBlocoArtefato.numbered,
                text = num.groupValues[2],
            )
            i += 1
            continue
        }

        if (trim.startsWith(">")) {
            flushPara()
            val quoteLines = ArrayList<String>()
            while (i < linhas.size && linhas[i].trim().startsWith(">")) {
                quoteLines += linhas[i].replace(Regex("^\\s*>\\s?"), "")
                i += 1
            }
            val body = quoteLines.joinToString("\n")
            val isCallout = body.trim().startsWith("[!") ||
                body.trim().startsWith("💡") ||
                body.trim().startsWith("⚠️") ||
                body.trim().startsWith("ℹ️")
            blocos += BlocoArtefato(
                id = novoIdBloco(),
                type = if (isCallout) TipoBlocoArtefato.callout else TipoBlocoArtefato.quote,
                text = body,
            )
            continue
        }

        if (trim.isEmpty()) {
            flushPara()
            i += 1
            continue
        }

        paraBuf += linha
        i += 1
    }
    flushPara()

    if (blocos.isEmpty()) {
        return listOf(BlocoArtefato(id = novoIdBloco(), type = TipoBlocoArtefato.paragraph, text = ""))
    }
    return blocos
}

/** Garante blocos + projeção MD coerentes a partir do que veio do Firestore. */
fun normalizarDocumentoBlocos(
    conteudo: String?,
    blocos: List<BlocoArtefato>?,
    schemaVersion: Int?,
): Triple<Int, List<BlocoArtefato>, String> {
    val temBlocos = !blocos.isNullOrEmpty()
    if (temBlocos && (schemaVersion ?: 0) >= SCHEMA_ARTEFATO_BLOCOS) {
        val norm = blocos!!.map { b ->
            if (b.id.isBlank()) b.copy(id = novoIdBloco()) else b
        }
        return Triple(SCHEMA_ARTEFATO_BLOCOS, norm, blocosToMd(norm))
    }
    val md = conteudo.orEmpty()
    val parsed = mdToBlocos(md)
    return Triple(SCHEMA_ARTEFATO_BLOCOS, parsed, blocosToMd(parsed))
}

fun headingsOutline(blocos: List<BlocoArtefato>): List<BlocoArtefato> =
    blocos.filter { it.type == TipoBlocoArtefato.heading }
