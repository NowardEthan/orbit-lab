package com.ethan.orbitlab.data

import java.util.UUID

/**
 * Par estável user↔luna — alinhado ao orbit-mobile, com sufixo que ordena bem
 * no Firestore quando o `createdAt` empatar (`-0` antes de `-1`).
 */
fun newUserMessageId(): String = "m${UUID.randomUUID()}-0"

fun lunaMessageIdForUser(userMessageId: String): String = when {
    userMessageId.endsWith("-0") -> userMessageId.removeSuffix("-0") + "-1"
    userMessageId.startsWith("u") && !userMessageId.startsWith("u-") ->
        "l${userMessageId.removePrefix("u")}"
    else -> "l-$userMessageId"
}

/** Inverso de [lunaMessageIdForUser] — null se o id não for o par estável. */
fun userMessageIdForLuna(lunaMessageId: String): String? = when {
    lunaMessageId.endsWith("-1") -> lunaMessageId.removeSuffix("-1") + "-0"
    lunaMessageId.startsWith("l") && !lunaMessageId.startsWith("l-") ->
        "u${lunaMessageId.removePrefix("l")}"
    lunaMessageId.startsWith("l-") -> {
        val rest = lunaMessageId.removePrefix("l-")
        if (rest.startsWith("u")) rest else null
    }
    else -> null
}

/**
 * Garante que a bolha da Luna fique depois da do usuário do mesmo par.
 * Cobre o caso em que o Railway grava os dois com o mesmo `createdAt` e o
 * desempate por `__name__` coloca `l…` antes de `u…`.
 */
fun stabilizeLunaAfterUser(messages: List<Mensagem>): List<Mensagem> {
    if (messages.size < 2) return messages
    val result = messages.toMutableList()
    var i = 0
    while (i < result.size) {
        val msg = result[i]
        if (!msg.isLuna) {
            i++
            continue
        }
        val userId = userMessageIdForLuna(msg.id)
        if (userId == null) {
            i++
            continue
        }
        val userIdx = result.indexOfFirst { it.id == userId }
        if (userIdx < 0 || userIdx < i) {
            i++
            continue
        }
        result.removeAt(i)
        val insertAt = result.indexOfFirst { it.id == userId } + 1
        result.add(insertAt.coerceAtMost(result.size), msg)
    }
    return result
}

/** Ordenação cronológica + user antes de luna no empate + par estável. */
fun ordenarMensagensChat(messages: List<Mensagem>): List<Mensagem> {
    val sorted = messages.sortedWith(
        compareBy<Mensagem> { it.timestamp }
            .thenBy { if (it.isLuna) 1 else 0 }
            .thenBy { it.id },
    )
    return stabilizeLunaAfterUser(sorted)
}
