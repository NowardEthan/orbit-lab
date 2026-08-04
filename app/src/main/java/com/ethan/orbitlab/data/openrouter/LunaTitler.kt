package com.ethan.orbitlab.data.openrouter

import com.ethan.orbitlab.data.Mensagem

/**
 * Batiza a conversa pelo assunto atual via OpenRouter (flash).
 *
 * Só roda em **debug** com chave local (`OpenRouterConfig.isConfigured`). No
 * release sideload a chave está vazia — devolve null e o título fica como está.
 * Produto de chat = luna-core; isto é higiene Fase 5, não caminho de produção.
 */
object LunaTitler {

    suspend fun gerarTitulo(mensagens: List<Mensagem>): String? {
        if (!OpenRouterConfig.isConfigured()) return null

        val corpo = mensagens
            .filterNot { it.erro }
            .takeLast(10)
            .joinToString("\n") { m ->
                val quem = if (m.isLuna) "Luna" else "Usuário"
                val t = m.texto.ifBlank {
                    m.attachments.joinToString(", ") { it.name }.ifBlank { "(sem texto)" }
                }
                "$quem: ${t.take(280)}"
            }
            .trim()
        if (corpo.isBlank()) return null

        val messages = listOf(
            ChatMessage(
                role = "system",
                content = "Você nomeia conversas. Responda APENAS com um título curto em " +
                    "português do Brasil, de 2 a 4 palavras, sem aspas e sem ponto final, " +
                    "capitalizado como uma frase. Capture o assunto atual da conversa.",
            ),
            ChatMessage(
                role = "user",
                content = "Conversa:\n$corpo\n\nTítulo:",
            ),
        )

        val bruto = runCatching { OpenRouterClient.chat(messages) }.getOrNull() ?: return null
        return limparTitulo(bruto)
    }

    /** Tira aspas/ruído do modelo e limita a um rótulo curto. */
    private fun limparTitulo(bruto: String): String? {
        val linha = bruto.trim()
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.removePrefix("Título:")
            ?.removePrefix("Titulo:")
            ?.trim()
            ?.trim('"', '\'', '“', '”', '.', '—', '-', ' ')
            ?: return null
        if (linha.isBlank() || linha.equals("…", ignoreCase = true)) return null
        // No máximo 6 palavras / 40 caracteres — cabe no header e na lista.
        val curto = linha.split(Regex("\\s+")).take(6).joinToString(" ")
        return curto.take(40).ifBlank { null }
    }
}
