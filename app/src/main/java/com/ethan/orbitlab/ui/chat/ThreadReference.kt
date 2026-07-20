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
    ) : ThreadReference()
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

fun referenceChipLabel(ref: ThreadReference): String = when (ref) {
    is ThreadReference.Message -> {
        val autor = if (ref.isLuna) "Luna" else "Você"
        "Msg #${ref.messageIndex} · $autor — ${ref.excerpt}"
    }
    is ThreadReference.Image -> {
        val tipo = "Imagem"
        "Msg #${ref.messageIndex} · $tipo — ${ref.attachmentName}"
    }
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
            buildString {
                appendLine("[Referência contextual]")
                appendLine("- Imagem: \"$name\" (mensagem #${ref.messageIndex})")
                appendLine()
                if (question.isNotBlank()) {
                    append("Pergunta sobre esta imagem:\n$question")
                } else {
                    append("Comentário sobre esta imagem.")
                }
            }
        }
    }
}
