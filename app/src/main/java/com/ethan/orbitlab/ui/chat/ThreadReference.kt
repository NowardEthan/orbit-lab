package com.ethan.orbitlab.ui.chat

import android.net.Uri
import com.ethan.orbitlab.data.Mensagem

/**
 * Referência contextual — espelho do orbit-mobile (+ imagem, pedido do lab).
 */
sealed class ThreadReference {
    abstract val messageId: String
    abstract val messageIndex: Int
    abstract val isLuna: Boolean
    abstract val excerpt: String

    data class Message(
        override val messageId: String,
        override val messageIndex: Int,
        override val isLuna: Boolean,
        override val excerpt: String,
        val fullText: String,
    ) : ThreadReference()

    data class Image(
        override val messageId: String,
        override val messageIndex: Int,
        override val isLuna: Boolean,
        val attachmentId: String,
        val attachmentName: String,
        val uri: Uri?,
        override val excerpt: String = attachmentName,
        /** Prompt original da arte da Luna (quando soubermos) — ancora o sujeito na edição. */
        val sourcePrompt: String? = null,
    ) : ThreadReference()

    /**
     * Um trecho de um artefato, apontado no leitor pra a Luna mudar.
     *
     * O [trecho] é uma cópia EXATA da fonte Markdown (veio da seleção sobre o texto cru, não do
     * que aparece renderizado) — é isso que faz dele um `trecho_antigo` válido pra a mão cirúrgica
     * `editar_trecho_artefato`. O [documentoId] é o id do artefato (o `id` da ferramenta).
     * Os campos de mensagem não se aplicam aqui; ficam neutros.
     */
    data class ArtefatoTrecho(
        val documentoId: String,
        val titulo: String,
        val trecho: String,
        override val excerpt: String = trecho.trim().replace(Regex("\\s+"), " ").take(280),
        override val messageId: String = documentoId,
        override val messageIndex: Int = 0,
        override val isLuna: Boolean = false,
    ) : ThreadReference()
}

/** Monta a referência a um trecho de artefato. `null` se o trecho vier vazio. */
fun buildArtefatoTrechoReference(
    documentoId: String,
    titulo: String,
    trecho: String,
): ThreadReference.ArtefatoTrecho? {
    if (documentoId.isBlank() || trecho.isBlank()) return null
    return ThreadReference.ArtefatoTrecho(
        documentoId = documentoId,
        titulo = titulo.ifBlank { "Artefato" },
        trecho = trecho,
    )
}

fun messageIndexInThread(mensagens: List<Mensagem>, messageId: String): Int {
    val i = mensagens.indexOfFirst { it.id == messageId }
    return if (i < 0) 0 else i + 1
}

fun buildMessageReference(
    msg: Mensagem,
    mensagens: List<Mensagem>,
    excerptText: String = msg.texto,
): ThreadReference.Message? {
    val full = msg.texto.trim().ifBlank {
        if (msg.attachments.isNotEmpty()) {
            msg.attachments.joinToString(", ") { it.name }
        } else {
            ""
        }
    }
    val excerpt = excerptText.trim().ifBlank { full }
    if (excerpt.isBlank()) return null
    return ThreadReference.Message(
        messageId = msg.id,
        messageIndex = messageIndexInThread(mensagens, msg.id),
        isLuna = msg.isLuna,
        excerpt = excerpt.take(280),
        fullText = full,
    )
}

fun buildImageReference(
    msg: Mensagem,
    mensagens: List<Mensagem>,
    attachment: ComposerAttachment,
): ThreadReference.Image? {
    if (attachment.kind != AttachmentKind.IMAGE && attachment.kind != AttachmentKind.VIDEO) {
        return null
    }
    return ThreadReference.Image(
        messageId = msg.id,
        messageIndex = messageIndexInThread(mensagens, msg.id),
        isLuna = msg.isLuna,
        attachmentId = attachment.id,
        attachmentName = attachment.name,
        uri = attachment.uri,
        excerpt = attachment.name,
    )
}

/**
 * A referência que uma mensagem vira ao ser PUXADA (swipe, estilo WhatsApp).
 *
 * Referencia o que a mensagem É: se ela carrega uma imagem — sua (anexo) ou dela (desenhada) —
 * aponta a imagem, pra a Luna se basear nela; senão, aponta o texto. `null` quando não há nada
 * citável (mensagem vazia sem mídia).
 */
fun referenciaAoPuxar(msg: Mensagem, mensagens: List<Mensagem>): ThreadReference? {
    // Imagem/vídeo que VOCÊ anexou.
    msg.attachments
        .firstOrNull { it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.VIDEO }
        ?.let { return buildImageReference(msg, mensagens, it) }

    // Imagem que a LUNA desenhou (vive numa URL do Storage — sintetizamos um anexo pra citar).
    msg.imagensGeradas.firstOrNull()?.let { img ->
        val nome =
            img.prompt.trim().takeIf { it.isNotBlank() }?.take(80) ?: "Imagem da Luna"
        val att = ComposerAttachment(
            id = "img-gerada-${msg.id}",
            kind = AttachmentKind.IMAGE,
            name = nome,
            sizeLabel = "—",
            mime = "image/png",
            uri = runCatching { Uri.parse(img.url) }.getOrNull(),
        )
        return buildImageReference(msg, mensagens, att)?.copy(
            sourcePrompt = img.prompt.trim().takeIf { it.isNotBlank() },
        )
    }

    // Só texto.
    return buildMessageReference(msg, mensagens)
}

fun referenceChipLabel(ref: ThreadReference): String = when (ref) {
    is ThreadReference.Message -> {
        val autor = if (ref.isLuna) "Luna" else "Você"
        "Msg #${ref.messageIndex} · $autor — ${ref.excerpt}"
    }
    is ThreadReference.Image -> {
        val tipo = "Imagem"
        "Msg #${ref.messageIndex} · $tipo — ${ref.attachmentName}"
    }
    is ThreadReference.ArtefatoTrecho -> "«${ref.titulo}» — ${ref.excerpt}"
}

/** Prompt enviado à Luna (demo) — mesmo formato do mobile. */
fun formatMessageWithReference(userText: String, ref: ThreadReference): String {
    val question = userText.trim()
    val cite = ref.excerpt.replace("\"", "\\\"")
    return when (ref) {
        is ThreadReference.Message -> {
            val author = if (ref.isLuna) "Luna" else "você"
            buildString {
                appendLine("[Referência contextual]")
                appendLine("- Mensagem #${ref.messageIndex} ($author)")
                appendLine("- Trecho citado: \"$cite\"")
                appendLine()
                if (question.isNotBlank()) {
                    append("Pergunta sobre este trecho:\n$question")
                } else {
                    append("Comentário sobre este trecho.")
                }
            }
        }
        is ThreadReference.Image -> {
            val name = ref.attachmentName.replace("\"", "\\\"")
            val url = ref.uri?.toString()?.takeIf {
                it.startsWith("http://") || it.startsWith("https://")
            }
            val brief = ref.sourcePrompt?.trim()?.takeIf { it.isNotBlank() }
            buildString {
                appendLine("[Referência contextual]")
                appendLine("- Imagem: \"$name\" (mensagem #${ref.messageIndex})")
                if (ref.isLuna && !url.isNullOrBlank()) {
                    // Base explícita pra editar_imagem — sem isto ela caía na ÚLTIMA arte.
                    appendLine("- Esta é uma arte DA LUNA (não um anexo dele).")
                    appendLine("- URL da BASE para editar_imagem (base_url): $url")
                    if (brief != null) {
                        appendLine("- Assunto/prompt original desta arte: «${brief.take(280)}»")
                    }
                    appendLine(
                        "- CONTINUIDADE / ESTILO («faz realista», retoque, mesma cena): " +
                            "chama editar_imagem com base_url = esta URL. " +
                            "NÃO uses gerar_imagem (do zero o motor realista inventa outro assunto).",
                    )
                    appendLine(
                        "- Mantém o MESMO sujeito desta arte (objeto/cena/pessoa — o que estiver nela).",
                    )
                } else if (!url.isNullOrBlank()) {
                    appendLine("- URL da imagem: $url")
                }
                appendLine()
                if (question.isNotBlank()) {
                    append("Pergunta sobre esta imagem:\n$question")
                } else {
                    append("Comentário sobre esta imagem.")
                }
            }
        }
        is ThreadReference.ArtefatoTrecho -> buildString {
            // O trecho vem cru da fonte — é o `trecho_antigo` pronto pra `editar_trecho_artefato`.
            // Entregue entre marcas pra a Luna copiá-lo tal e qual, sem re-emitir o artefato inteiro.
            appendLine("[Referência a um artefato]")
            appendLine("- Artefato: «${ref.titulo}» (id: ${ref.documentoId})")
            appendLine("- Trecho apontado (cópia EXATA do texto atual do artefato):")
            appendLine("<<<TRECHO")
            appendLine(ref.trecho)
            appendLine("TRECHO>>>")
            appendLine()
            appendLine(
                "Se ele pedir pra alterar ESTE trecho, use editar_trecho_artefato com o id acima e " +
                    "este texto como trecho_antigo (copie-o tal e qual, sem as marcas <<<TRECHO). " +
                    "Não reescreva o artefato inteiro.",
            )
            appendLine()
            if (question.isNotBlank()) {
                append("Pedido sobre este trecho:\n$question")
            } else {
                append("Comentário sobre este trecho.")
            }
        }
    }
}
