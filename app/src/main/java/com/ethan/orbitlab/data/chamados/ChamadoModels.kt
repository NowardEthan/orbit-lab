package com.ethan.orbitlab.data.chamados

import com.ethan.orbitlab.BuildConfig

/**
 * "Fala com o Ethan" — canal de suporte humano (chamados/tickets).
 *
 * Diferente de Finanças (que mora em `users/{uid}/...`, privado do dono), um chamado é lido
 * por DUAS pessoas — o usuário e o Ethan — então vive numa coleção de topo `chamados`, cada
 * doc carimbado com o `uid` do dono. A conversa fica em `chamados/{id}/mensagens`.
 *
 * Molde de leitura igual ao [com.ethan.orbitlab.data.financas.FirestoreFinancas]: extração
 * manual de `data["campo"]` (nada de `.toObject()` por reflexão → seguro sob R8/minify).
 */

/** Tipo do chamado — o "assunto" que o usuário escolhe ao abrir. */
object TipoChamado {
    const val BUG = "bug"
    const val IDEIA = "ideia"
    const val DUVIDA = "duvida"

    val todos = listOf(BUG, IDEIA, DUVIDA)

    fun rotulo(t: String?): String = when (t) {
        BUG -> "Problema"
        IDEIA -> "Ideia"
        DUVIDA -> "Dúvida"
        else -> "Chamado"
    }
}

/** Estado do chamado — só o Ethan (admin) move; o usuário só acompanha. */
object StatusChamado {
    const val ABERTO = "aberto"
    const val EM_ANALISE = "em_analise"
    const val RESPONDIDO = "respondido"
    const val RESOLVIDO = "resolvido"

    val todos = listOf(ABERTO, EM_ANALISE, RESPONDIDO, RESOLVIDO)

    fun rotulo(s: String?): String = when (s) {
        ABERTO -> "Aberto"
        EM_ANALISE -> "Em análise"
        RESPONDIDO -> "Respondido"
        RESOLVIDO -> "Resolvido"
        else -> "Aberto"
    }
}

/** Quem escreveu a mensagem dentro da conversa do chamado. */
object AutorChamado {
    const val USUARIO = "usuario"
    const val ETHAN = "ethan"
}

/** Um chamado — a "capa" que aparece na lista (do usuário) e na caixa de entrada (do Ethan). */
data class Chamado(
    val id: String,
    val uid: String,              // dono (quem abriu)
    val nomeUsuario: String,      // pra o Ethan reconhecer sem abrir
    val tipo: String,
    val titulo: String,
    val status: String,
    val contextoApp: String?,     // "lab 0.30.3/90 · plano free" — anexado automático na abertura
    val ultimaMensagem: String,   // preview na lista
    val ultimoAutor: String,      // usuario | ethan (pra decidir badge "respondido")
    val naoLidoUsuario: Boolean,  // tem resposta do Ethan que o usuário ainda não viu
    val naoLidoAdmin: Boolean,    // tem mensagem do usuário que o Ethan ainda não viu
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

/** Uma fala dentro da conversa do chamado. */
data class MensagemChamado(
    val id: String,
    val autor: String,            // usuario | ethan
    val texto: String,
    val createdAtMs: Long,
)

/** Entrada do formulário de abertura — ainda sem id. */
data class ChamadoRascunho(
    val tipo: String,
    val titulo: String,
    val mensagem: String,
) {
    fun valido(): Boolean =
        tipo in TipoChamado.todos &&
            titulo.trim().length >= 3 &&
            mensagem.trim().length >= 3
}

/**
 * Contexto técnico anexado automaticamente na abertura, pra o Ethan não precisar perguntar
 * "qual versão?". Ex.: "lab 0.30.3/90 · plano free".
 */
fun contextoAppAtual(plano: String? = null): String {
    val base = "lab ${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE}"
    return if (!plano.isNullOrBlank()) "$base · plano $plano" else base
}
