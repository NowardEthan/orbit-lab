package com.ethan.orbitlab.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ethan.orbitlab.data.Mensagem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta conversa(s) da Luna — compartilhar OU baixar no aparelho, em Markdown, texto ou HTML.
 *
 * Formato "Exportado do Orbit": título, data, e o papo em **Você:** / **Luna:**.
 */
object ConversaExporter {

    /** (título da conversa, mensagens dela). */
    data class Item(val titulo: String, val mensagens: List<Mensagem>)

    /** Formatos oferecidos ao exportar. */
    enum class ExportFormato(
        val ext: String,
        val mime: String,
        val rotulo: String,
        val descricao: String,
    ) {
        MARKDOWN("md", "text/markdown", "Markdown", "Formatado (.md) — Obsidian, Notion, GitHub…"),
        TEXTO("txt", "text/plain", "Texto", "Simples (.txt) — abre em qualquer lugar"),
        HTML("html", "text/html", "Página", "HTML (.html) — abre no navegador, dá pra imprimir/PDF"),
    }

    private val dataFmt = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale("pt", "BR"))

    /** Monta o conteúdo no formato pedido. */
    fun montar(formato: ExportFormato, titulo: String, mensagens: List<Mensagem>): String =
        when (formato) {
            ExportFormato.MARKDOWN -> montarMarkdown(titulo, mensagens)
            ExportFormato.TEXTO -> montarTexto(titulo, mensagens)
            ExportFormato.HTML -> montarHtml(titulo, mensagens)
        }

    private fun corpoMensagem(msg: Mensagem): String = msg.texto.trim().ifBlank {
        if (msg.attachments.isNotEmpty()) {
            "[anexos: " + msg.attachments.joinToString(", ") { it.name } + "]"
        } else {
            "…"
        }
    }

    fun montarMarkdown(titulo: String, mensagens: List<Mensagem>): String = buildString {
        append("# ").append(titulo.trim().ifBlank { "Conversa" }).append("\n\n")
        append("*Exportado do Orbit · ").append(dataFmt.format(Date())).append("*\n\n")
        append("---\n\n")
        mensagens.sortedBy { it.timestamp }.forEach { msg ->
            append("**").append(if (msg.isLuna) "Luna" else "Você").append(":**\n\n")
            append(corpoMensagem(msg)).append("\n\n")
        }
    }

    private fun montarTexto(titulo: String, mensagens: List<Mensagem>): String = buildString {
        append(titulo.trim().ifBlank { "Conversa" }).append("\n")
        append("Exportado do Orbit · ").append(dataFmt.format(Date())).append("\n")
        append("=".repeat(32)).append("\n\n")
        mensagens.sortedBy { it.timestamp }.forEach { msg ->
            append(if (msg.isLuna) "Luna:" else "Você:").append("\n")
            append(corpoMensagem(msg)).append("\n\n")
        }
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br>")

    private fun montarHtml(titulo: String, mensagens: List<Mensagem>): String {
        val tit = titulo.trim().ifBlank { "Conversa" }
        val turnos = mensagens.sortedBy { it.timestamp }.joinToString("\n") { msg ->
            val quem = if (msg.isLuna) "Luna" else "Você"
            val cls = if (msg.isLuna) "luna" else "voce"
            """      <article class="turn $cls">
        <div class="who">${esc(quem)}</div>
        <div class="body">${esc(corpoMensagem(msg))}</div>
      </article>"""
        }
        return """<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(tit)}</title>
<style>
  :root { --bg:#0e1014; --card:#1a1c22; --line:rgba(255,255,255,.08);
          --high:#f2f4f8; --mid:#9ca3b0; --accent:#7a9af5; --you:#4b75f2; }
  @media (prefers-color-scheme: light) {
    :root { --bg:#f4f6fb; --card:#ffffff; --line:rgba(0,0,0,.08);
            --high:#12141a; --mid:#5a616e; --accent:#3a5fd4; --you:#3a5fd4; }
  }
  * { box-sizing: border-box; }
  body { margin:0; background:var(--bg); color:var(--high);
         font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
         line-height:1.6; padding:32px 16px; }
  main { max-width:680px; margin:0 auto; }
  h1 { font-size:1.6rem; margin:0 0 4px; letter-spacing:-.02em; }
  .meta { color:var(--mid); font-size:.85rem; margin:0 0 24px; }
  .thread { display:flex; flex-direction:column; gap:14px; }
  .turn { background:var(--card); border:1px solid var(--line);
          border-radius:16px; padding:14px 16px; }
  .who { font-size:.72rem; text-transform:uppercase; letter-spacing:.08em;
         font-weight:700; margin-bottom:6px; color:var(--accent); }
  .turn.voce .who { color:var(--you); }
  .body { white-space:pre-wrap; word-wrap:break-word; }
  footer { color:var(--mid); font-size:.75rem; text-align:center; margin-top:28px; }
</style>
</head>
<body>
  <main>
    <h1>${esc(tit)}</h1>
    <p class="meta">Exportado do Orbit · ${esc(dataFmt.format(Date()))}</p>
    <div class="thread">
$turnos
    </div>
    <footer>🌙 Orbit · Luna</footer>
  </main>
</body>
</html>
"""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Artefatos — exporta um corpo em Markdown direto (não uma conversa de bolhas).
    // O artefato JÁ é Markdown, então o MD sai quase intacto; o HTML ganha uma
    // conversão leve pra virar uma página imprimível (o caminho "salvar em PDF").
    // ─────────────────────────────────────────────────────────────────────────

    /** Monta o artefato no formato pedido, a partir do título + corpo em Markdown. */
    fun montarArtefato(formato: ExportFormato, titulo: String, corpoMarkdown: String): String =
        when (formato) {
            ExportFormato.MARKDOWN -> montarArtefatoMarkdown(titulo, corpoMarkdown)
            ExportFormato.TEXTO -> montarArtefatoTexto(titulo, corpoMarkdown)
            ExportFormato.HTML -> montarArtefatoHtml(titulo, corpoMarkdown)
        }

    private fun montarArtefatoMarkdown(titulo: String, corpo: String): String = buildString {
        append("# ").append(titulo.trim().ifBlank { "Artefato" }).append("\n\n")
        append("*Exportado do Orbit · ").append(dataFmt.format(Date())).append("*\n\n")
        append("---\n\n")
        append(corpo.trim()).append("\n")
    }

    private fun montarArtefatoTexto(titulo: String, corpo: String): String = buildString {
        append(titulo.trim().ifBlank { "Artefato" }).append("\n")
        append("Exportado do Orbit · ").append(dataFmt.format(Date())).append("\n")
        append("=".repeat(32)).append("\n\n")
        append(corpo.trim()).append("\n")
    }

    private fun escBasico(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    /** Formatação inline (negrito, itálico, código, links) sobre texto já escapado. */
    private fun mdInline(raw: String): String {
        var t = escBasico(raw)
        t = Regex("`([^`]+)`").replace(t) { "<code>${it.groupValues[1]}</code>" }
        t = Regex("""\*\*([^*]+)\*\*""").replace(t) { "<strong>${it.groupValues[1]}</strong>" }
        t = Regex("""(?<!\*)\*([^*]+)\*(?!\*)""").replace(t) { "<em>${it.groupValues[1]}</em>" }
        t = Regex("""\[([^\]]+)\]\(([^)]+)\)""").replace(t) {
            "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>"
        }
        return t
    }

    /** Conversão Markdown→HTML por blocos — cobre o que a Luna produz (títulos, listas, citações, hr). */
    private fun markdownParaHtml(md: String): String {
        val out = StringBuilder()
        val linhas = md.replace("\r\n", "\n").split("\n")
        var emUl = false
        var emOl = false
        fun fecharListas() {
            if (emUl) { out.append("</ul>\n"); emUl = false }
            if (emOl) { out.append("</ol>\n"); emOl = false }
        }
        for (linha in linhas) {
            val t = linha.trim()
            when {
                t.isEmpty() -> fecharListas()
                t == "---" || t == "***" || t == "___" -> { fecharListas(); out.append("<hr>\n") }
                t.startsWith("### ") -> { fecharListas(); out.append("<h3>${mdInline(t.removePrefix("### "))}</h3>\n") }
                t.startsWith("## ") -> { fecharListas(); out.append("<h2>${mdInline(t.removePrefix("## "))}</h2>\n") }
                t.startsWith("# ") -> { fecharListas(); out.append("<h1>${mdInline(t.removePrefix("# "))}</h1>\n") }
                t.startsWith("> ") -> { fecharListas(); out.append("<blockquote>${mdInline(t.removePrefix("> "))}</blockquote>\n") }
                Regex("^[-*] ").containsMatchIn(t) -> {
                    if (emOl) { out.append("</ol>\n"); emOl = false }
                    if (!emUl) { out.append("<ul>\n"); emUl = true }
                    out.append("<li>${mdInline(t.replaceFirst(Regex("^[-*] "), ""))}</li>\n")
                }
                Regex("""^\d+\. """).containsMatchIn(t) -> {
                    if (emUl) { out.append("</ul>\n"); emUl = false }
                    if (!emOl) { out.append("<ol>\n"); emOl = true }
                    out.append("<li>${mdInline(t.replaceFirst(Regex("""^\d+\. """), ""))}</li>\n")
                }
                else -> { fecharListas(); out.append("<p>${mdInline(t)}</p>\n") }
            }
        }
        fecharListas()
        return out.toString()
    }

    private fun montarArtefatoHtml(titulo: String, corpo: String): String {
        val tit = titulo.trim().ifBlank { "Artefato" }
        val corpoHtml = markdownParaHtml(corpo)
        return """<!doctype html>
<html lang="pt-BR">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${escBasico(tit)}</title>
<style>
  :root { --bg:#0e1014; --card:#1a1c22; --line:rgba(255,255,255,.08);
          --high:#f2f4f8; --mid:#9ca3b0; --accent:#7a9af5; }
  @media (prefers-color-scheme: light) {
    :root { --bg:#f4f6fb; --card:#ffffff; --line:rgba(0,0,0,.08);
            --high:#12141a; --mid:#5a616e; --accent:#3a5fd4; }
  }
  * { box-sizing: border-box; }
  body { margin:0; background:var(--bg); color:var(--high);
         font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
         line-height:1.65; padding:40px 16px; }
  main { max-width:680px; margin:0 auto; }
  h1 { font-size:1.9rem; margin:0 0 4px; letter-spacing:-.02em; }
  .meta { color:var(--mid); font-size:.85rem; margin:0 0 28px;
          border-bottom:1px solid var(--line); padding-bottom:18px; }
  .doc h2 { font-size:1.3rem; margin:1.7em 0 .5em; letter-spacing:-.01em; }
  .doc h3 { font-size:1.08rem; margin:1.4em 0 .4em; }
  .doc p { margin:.7em 0; }
  .doc ul, .doc ol { margin:.6em 0; padding-left:1.4em; }
  .doc li { margin:.25em 0; }
  .doc blockquote { margin:1em 0; padding:.5em 1em; border-left:3px solid var(--accent);
                    color:var(--mid); background:var(--card); border-radius:0 8px 8px 0; }
  .doc code { background:var(--card); border:1px solid var(--line); border-radius:5px;
              padding:.1em .35em; font-size:.9em; }
  .doc hr { border:none; border-top:1px solid var(--line); margin:1.8em 0; }
  .doc a { color:var(--accent); }
  footer { color:var(--mid); font-size:.75rem; text-align:center; margin-top:36px; }
</style>
</head>
<body>
  <main>
    <h1>${escBasico(tit)}</h1>
    <p class="meta">Exportado do Orbit · ${escBasico(dataFmt.format(Date()))}</p>
    <article class="doc">
$corpoHtml    </article>
    <footer>🌙 Orbit · Luna</footer>
  </main>
</body>
</html>
"""
    }

    /** Compartilha um artefato (um corpo já pronto) — escreve o arquivo no cache e abre o chooser. */
    fun compartilharConteudo(
        context: Context,
        titulo: String,
        conteudo: String,
        formato: ExportFormato = ExportFormato.MARKDOWN,
    ) {
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val authority = "${context.packageName}.fileprovider"
        val file = File(dir, nomeArquivo(titulo, formato))
        file.writeText(conteudo)
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = formato.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titulo)
            putExtra(Intent.EXTRA_TEXT, conteudo)
        }.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(intent, "Exportar artefato").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun nomeArquivo(titulo: String, formato: ExportFormato = ExportFormato.MARKDOWN): String {
        val slug = titulo.trim().lowercase(Locale("pt", "BR"))
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)
            .ifBlank { "conversa" }
        return "orbit-$slug.${formato.ext}"
    }

    /** Grava o conteúdo num Uri escolhido pelo usuário (SAF). true = salvou. */
    fun salvarNoUri(context: Context, uri: Uri, conteudo: String): Boolean =
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(conteudo.toByteArray(Charsets.UTF_8))
                true
            } ?: false
        }.getOrDefault(false)

    /** Escreve o(s) arquivo(s) no cache e abre o compartilhador do Android. */
    fun compartilhar(
        context: Context,
        itens: List<Item>,
        formato: ExportFormato = ExportFormato.MARKDOWN,
    ) {
        val validos = itens.filter { it.mensagens.isNotEmpty() }
        if (validos.isEmpty()) return

        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>()
        // Evita colidir dois arquivos com o mesmo slug (títulos parecidos).
        val usados = HashSet<String>()
        validos.forEach { item ->
            var nome = nomeArquivo(item.titulo, formato)
            var n = 1
            while (!usados.add(nome)) {
                nome = nomeArquivo(item.titulo, formato).removeSuffix(".${formato.ext}") +
                    "-${++n}.${formato.ext}"
            }
            val file = File(dir, nome)
            file.writeText(montar(formato, item.titulo, item.mensagens))
            uris.add(FileProvider.getUriForFile(context, authority, file))
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = formato.mime
                putExtra(Intent.EXTRA_STREAM, uris[0])
                putExtra(Intent.EXTRA_SUBJECT, validos[0].titulo)
                // Fallback texto para apps que ignoram o anexo (colar no chat).
                putExtra(Intent.EXTRA_TEXT, montar(formato, validos[0].titulo, validos[0].mensagens))
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = formato.mime
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "${uris.size} conversas do Orbit")
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val chooser = Intent.createChooser(intent, "Exportar conversa")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
