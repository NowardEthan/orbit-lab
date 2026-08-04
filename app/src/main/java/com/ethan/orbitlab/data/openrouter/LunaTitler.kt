package com.ethan.orbitlab.data.openrouter

import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.data.lunaapi.LunaApiClient

/**
 * Batiza a conversa pelo assunto atual via **luna-core** (`POST /v1/conversa/titulo`).
 *
 * Antes ia direto no OpenRouter do APK; Fase 5 tirou a chave do release. Agora o
 * caminho de produto é o servidor (Groq menor). Falha → null (título fica como está).
 */
object LunaTitler {

    suspend fun gerarTitulo(mensagens: List<Mensagem>): String? {
        val payload = mensagens
            .filterNot { it.erro }
            .takeLast(10)
            .map { m ->
                val t = m.texto.ifBlank {
                    m.attachments.joinToString(", ") { it.name }.ifBlank { "(sem texto)" }
                }
                LunaApiClient.MensagemBusca(
                    id = m.id,
                    papel = if (m.isLuna) "luna" else "user",
                    texto = t.take(280),
                )
            }
        if (payload.isEmpty()) return null
        val token = AuthRepository.getIdToken()
        return LunaApiClient.gerarTitulo(token, payload)
    }
}
