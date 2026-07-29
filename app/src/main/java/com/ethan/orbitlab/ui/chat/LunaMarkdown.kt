package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens

/**
 * Markdown da Luna — um sistema só.
 *
 * Fluxo contínuo: tipografia próxima, pouca caixa.
 * Quote = só trilho; código = inset leve. Sem “cards” competindo dentro do balão.
 */
private object Md {
    val gap = 3.dp
    val gapTitulo = 10.dp
    val rail = 2.dp
    val radius = 6.dp
    val body = 15.sp
    val bodyLine = 21.sp
    val mono = 13.sp
    val monoLine = 18.sp
}

/**
 * Parte o Markdown em blocos completos — usado no streaming.
 */
fun fatiarMarkdown(fonte: String): List<String> =
    parseBlocosMarkdown(fonte).map { serializarBloco(it) }

private fun serializarBloco(bloco: BlocoMd): String = when (bloco) {
    is BlocoMd.Titulo -> "${"#".repeat(bloco.nivel)} ${bloco.texto}"
    is BlocoMd.Paragrafo -> bloco.texto
    is BlocoMd.Quote -> "> ${bloco.texto}"
    is BlocoMd.Lista -> bloco.itens.mapIndexed { i, item ->
        if (bloco.ordenada) "${i + 1}. $item" else "- $item"
    }.joinToString("\n")
    is BlocoMd.Codigo -> {
        val lang = bloco.linguagem.orEmpty()
        "```$lang\n${bloco.texto}\n```"
    }
    is BlocoMd.Divisor -> "---"
}

/**
 * Cache incremental: em append (stream), reutiliza blocos fechados e só troca a cauda.
 */
private class MdIncrementalCache {
    private var cached = ""
    private var blocos: List<BlocoMd> = emptyList()

    fun update(content: String): List<BlocoMd> {
        if (content == cached) return blocos
        if (content.startsWith(cached) && cached.isNotEmpty() && blocos.isNotEmpty()) {
            val fechados = (blocos.size - 1).coerceAtLeast(0)
            if (fechados > 0) {
                val novo = parseBlocosMarkdown(content)
                val prefixo = blocos.take(fechados)
                if (novo.size >= fechados && novo.take(fechados) == prefixo) {
                    // Mantém instâncias dos fechados (Compose skip) + cauda nova.
                    blocos = List(fechados) { i -> blocos[i] } + novo.drop(fechados)
                    cached = content
                    return blocos
                }
            }
        }
        cached = content
        blocos = parseBlocosMarkdown(content)
        return blocos
    }
}

@Composable
fun LunaMarkdown(
    content: String,
    modifier: Modifier = Modifier,
) {
    val cache = remember { MdIncrementalCache() }
    val blocos = remember(content) { cache.update(content) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Md.gap),
    ) {
        blocos.forEachIndexed { index, bloco ->
            val anterior = blocos.getOrNull(index - 1)
            val isTail = index == blocos.lastIndex
            key(if (isTail) "tail-$index" else "blk-$index-${bloco.hashCode()}") {
                if (bloco is BlocoMd.Titulo && index > 0 && anterior !is BlocoMd.Titulo) {
                    Spacer(Modifier.height(Md.gapTitulo - Md.gap))
                }
                when (bloco) {
                    is BlocoMd.Titulo -> MdTitulo(bloco)
                    is BlocoMd.Paragrafo -> MdTexto(bloco.texto)
                    is BlocoMd.Quote -> MdQuote(bloco.texto)
                    is BlocoMd.Lista -> MdLista(bloco)
                    is BlocoMd.Codigo -> MdCodigo(bloco)
                    is BlocoMd.Divisor -> MdDivisor()
                }
            }
        }
    }
}

@Composable
private fun MdTitulo(bloco: BlocoMd.Titulo) {
    val styled = remember(bloco.texto) { estiloInline(bloco.texto) }
    Text(
        text = styled,
        color = OrbitTokens.textHigh,
        fontSize = when (bloco.nivel) {
            1 -> 17.sp
            2 -> 16.sp
            else -> 14.sp
        },
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.15).sp,
        lineHeight = when (bloco.nivel) {
            1 -> 23.sp
            else -> 21.sp
        },
    )
}

@Composable
private fun MdTexto(texto: String, italico: Boolean = false) {
    val styled = remember(texto) { estiloInline(texto) }
    Text(
        text = styled,
        color = OrbitTokens.textHigh,
        fontSize = Md.body,
        lineHeight = Md.bodyLine,
        fontStyle = if (italico) FontStyle.Italic else FontStyle.Normal,
    )
}

/** Quote contínuo — só o trilho, sem card/borda. */
@Composable
private fun MdQuote(texto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(Md.rail)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(OrbitTokens.accent.copy(alpha = 0.55f)),
        )
        val styled = remember(texto) { estiloInline(texto) }
        Text(
            text = styled,
            color = OrbitTokens.textMid,
            fontSize = Md.body,
            lineHeight = Md.bodyLine,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun MdLista(bloco: BlocoMd.Lista) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        bloco.itens.forEachIndexed { index, item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier.width(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bloco.ordenada) {
                        Text(
                            text = "${index + 1}.",
                            color = OrbitTokens.textLow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    } else {
                        Box(
                            Modifier
                                .padding(top = 8.dp)
                                .size(3.5.dp)
                                .clip(CircleShape)
                                .background(OrbitTokens.textLow),
                        )
                    }
                }
                val styled = remember(item) { estiloInline(item) }
                Text(
                    text = styled,
                    color = OrbitTokens.textHigh,
                    fontSize = Md.body,
                    lineHeight = Md.bodyLine,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MdCodigo(bloco: BlocoMd.Codigo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Md.radius))
            .background(OrbitTokens.ink0.copy(alpha = 0.45f)),
    ) {
        if (bloco.linguagem != null) {
            Text(
                text = bloco.linguagem,
                color = OrbitTokens.textLow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
                modifier = Modifier.padding(start = 9.dp, top = 6.dp, end = 9.dp),
            )
        }
        Text(
            text = bloco.texto,
            color = OrbitTokens.textHigh.copy(alpha = 0.88f),
            fontSize = Md.mono,
            lineHeight = Md.monoLine,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 9.dp,
                    end = 9.dp,
                    top = if (bloco.linguagem != null) 2.dp else 7.dp,
                    bottom = 7.dp,
                ),
        )
    }
}

@Composable
private fun MdDivisor() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(1.dp)
            .background(OrbitTokens.borderSoft.copy(alpha = 0.35f)),
    )
}

private sealed class BlocoMd {
    data class Titulo(val nivel: Int, val texto: String) : BlocoMd()
    data class Paragrafo(val texto: String) : BlocoMd()
    data class Quote(val texto: String) : BlocoMd()
    data class Lista(val ordenada: Boolean, val itens: List<String>) : BlocoMd()
    data class Codigo(val texto: String, val linguagem: String?) : BlocoMd()
    data object Divisor : BlocoMd()
}

/**
 * Linha inteira em negrito — a Luna usa isso como título de seção (ex.: `**1. finalidade**`),
 * ainda mais no Modo técnico. Sem isto o título grudava no parágrafo seguinte e não respirava.
 * O `(?:(?!\*\*).)+` garante um único span de negrito (não pega `**a** texto **b**`), e o `:?`
 * absorve dois-pontos no fim (`**Resumo:**`).
 */
private val RE_TITULO_NEGRITO = Regex("""^\*\*((?:(?!\*\*).)+?)\*\*:?$""")

private fun parseBlocosMarkdown(fonte: String): List<BlocoMd> {
    val linhas = fonte.replace("\r\n", "\n").lines()
    val out = mutableListOf<BlocoMd>()
    var i = 0

    while (i < linhas.size) {
        val trim = linhas[i].trim()

        when {
            trim.isEmpty() -> i++

            trim == "---" || trim == "***" || trim == "___" -> {
                out += BlocoMd.Divisor
                i++
            }

            trim.startsWith("```") -> {
                val lang = trim.removePrefix("```").trim().ifBlank { null }
                val buf = StringBuilder()
                i++
                while (i < linhas.size && !linhas[i].trim().startsWith("```")) {
                    if (buf.isNotEmpty()) buf.append('\n')
                    buf.append(linhas[i])
                    i++
                }
                if (i < linhas.size) i++
                out += BlocoMd.Codigo(buf.toString(), lang)
            }

            trim.startsWith("### ") -> {
                out += BlocoMd.Titulo(3, trim.removePrefix("### ").trim())
                i++
            }
            trim.startsWith("## ") -> {
                out += BlocoMd.Titulo(2, trim.removePrefix("## ").trim())
                i++
            }
            trim.startsWith("# ") -> {
                out += BlocoMd.Titulo(1, trim.removePrefix("# ").trim())
                i++
            }

            RE_TITULO_NEGRITO.matches(trim) -> {
                val texto = RE_TITULO_NEGRITO.find(trim)!!.groupValues[1].trim()
                out += BlocoMd.Titulo(2, texto)
                i++
            }

            trim.startsWith(">") -> {
                val buf = StringBuilder()
                while (i < linhas.size) {
                    val t = linhas[i].trim()
                    if (!t.startsWith(">")) break
                    val corpo = t.removePrefix(">").trimStart()
                    if (buf.isNotEmpty()) buf.append(' ')
                    buf.append(corpo)
                    i++
                }
                out += BlocoMd.Quote(buf.toString())
            }

            trim.matches(Regex("""^[-*]\s+.+""")) || trim.matches(Regex("""^\d+\.\s+.+""")) -> {
                val ordenada = trim.matches(Regex("""^\d+\.\s+.+"""))
                val itens = mutableListOf<String>()
                while (i < linhas.size) {
                    val t = linhas[i].trim()
                    val item = when {
                        t.matches(Regex("""^[-*]\s+.+""")) ->
                            t.replace(Regex("""^[-*]\s+"""), "")
                        t.matches(Regex("""^\d+\.\s+.+""")) ->
                            t.replace(Regex("""^\d+\.\s+"""), "")
                        else -> break
                    }
                    itens += item
                    i++
                }
                out += BlocoMd.Lista(ordenada, itens)
            }

            else -> {
                val buf = StringBuilder(trim)
                i++
                while (i < linhas.size) {
                    val t = linhas[i].trim()
                    if (t.isEmpty()) break
                    if (t == "---" || t == "***" || t == "___") break
                    if (t.startsWith("#") || t.startsWith(">") || t.startsWith("```") ||
                        RE_TITULO_NEGRITO.matches(t) ||
                        t.matches(Regex("""^[-*]\s+.+""")) || t.matches(Regex("""^\d+\.\s+.+"""))
                    ) {
                        break
                    }
                    buf.append(' ').append(t)
                    i++
                }
                out += BlocoMd.Paragrafo(buf.toString())
            }
        }
    }
    return out
}

private fun estiloInline(fonte: String): AnnotatedString {
    val bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = OrbitTokens.textHigh)
    val italic = SpanStyle(fontStyle = FontStyle.Italic)
    val code = SpanStyle(
        fontFamily = FontFamily.Monospace,
        color = OrbitTokens.accentText,
        background = OrbitTokens.ink0.copy(alpha = 0.55f),
        fontSize = 13.sp,
    )
    val strike = SpanStyle(
        textDecoration = TextDecoration.LineThrough,
        color = OrbitTokens.textLow,
    )
    val linkStyle = SpanStyle(
        color = OrbitTokens.accentText,
        textDecoration = TextDecoration.Underline,
        fontWeight = FontWeight.Medium,
    )

    return buildAnnotatedString {
        var rest = fonte
        // Links primeiro; depois bold/italic/code/strike.
        val pattern = Regex(
            """\[(.+?)]\((https?://[^\s)]+)\)|\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`|~~(.+?)~~""",
        )
        while (rest.isNotEmpty()) {
            val match = pattern.find(rest)
            if (match == null) {
                append(rest)
                break
            }
            if (match.range.first > 0) {
                append(rest.substring(0, match.range.first))
            }
            when {
                match.groupValues[1].isNotEmpty() -> {
                    val label = match.groupValues[1]
                    val url = match.groupValues[2]
                    pushLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(style = linkStyle),
                        ),
                    )
                    append(label)
                    pop()
                }
                match.groupValues[3].isNotEmpty() -> withStyle(bold) { append(match.groupValues[3]) }
                match.groupValues[4].isNotEmpty() -> withStyle(italic) { append(match.groupValues[4]) }
                match.groupValues[5].isNotEmpty() -> withStyle(code) { append(" ${match.groupValues[5]} ") }
                match.groupValues[6].isNotEmpty() -> withStyle(strike) { append(match.groupValues[6]) }
            }
            rest = rest.substring(match.range.last + 1)
        }
    }
}
