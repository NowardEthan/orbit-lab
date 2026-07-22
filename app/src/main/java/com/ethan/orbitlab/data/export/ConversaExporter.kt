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
 * Exporta conversa(s) da Luna em Markdown e abre o compartilhador do Android.
 *
 * Formato igual ao "Exportado do Orbit": título, data, e o papo em **Você:** / **Luna:**.
 * 1 conversa → um arquivo (ACTION_SEND); várias → vários arquivos (ACTION_SEND_MULTIPLE).
 */
object ConversaExporter {

    /** (título da conversa, mensagens dela). */
    data class Item(val titulo: String, val mensagens: List<Mensagem>)

    private val dataFmt = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale("pt", "BR"))

    fun montarMarkdown(titulo: String, mensagens: List<Mensagem>): String = buildString {
        append("# ").append(titulo.trim().ifBlank { "Conversa" }).append("\n\n")
        append("*Exportado do Orbit · ").append(dataFmt.format(Date())).append("*\n\n")
        append("---\n\n")
        mensagens.sortedBy { it.timestamp }.forEach { msg ->
            append("**").append(if (msg.isLuna) "Luna" else "Você").append(":**\n\n")
            val corpo = msg.texto.trim().ifBlank {
                if (msg.attachments.isNotEmpty()) {
                    "[anexos: " + msg.attachments.joinToString(", ") { it.name } + "]"
                } else {
                    "…"
                }
            }
            append(corpo).append("\n\n")
        }
    }

    private fun nomeArquivo(titulo: String): String {
        val slug = titulo.trim().lowercase(Locale("pt", "BR"))
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(40)
            .ifBlank { "conversa" }
        return "orbit-$slug.md"
    }

    /** Escreve o(s) .md no cache e abre o compartilhador. */
    fun compartilhar(context: Context, itens: List<Item>) {
        val validos = itens.filter { it.mensagens.isNotEmpty() }
        if (validos.isEmpty()) return

        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>()
        // Evita colidir dois arquivos com o mesmo slug (títulos parecidos).
        val usados = HashSet<String>()
        validos.forEach { item ->
            var nome = nomeArquivo(item.titulo)
            var n = 1
            while (!usados.add(nome)) {
                nome = nomeArquivo(item.titulo).removeSuffix(".md") + "-${++n}.md"
            }
            val file = File(dir, nome)
            file.writeText(montarMarkdown(item.titulo, item.mensagens))
            uris.add(FileProvider.getUriForFile(context, authority, file))
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                putExtra(Intent.EXTRA_SUBJECT, validos[0].titulo)
                // Fallback texto para apps que ignoram o anexo (colar no chat).
                putExtra(Intent.EXTRA_TEXT, montarMarkdown(validos[0].titulo, validos[0].mensagens))
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "text/plain"
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
