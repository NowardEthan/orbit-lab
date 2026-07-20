package com.ethan.orbitlab.domain.rotina

fun minutoParaHora(m: Minuto): String {
    val h = (m / 60) % 24
    val min = m % 60
    return "%02d:%02d".format(h, min)
}

fun horaParaMinuto(hhmm: String): Minuto {
    val parts = hhmm.split(':')
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return (h % 24) * 60 + (min % 60)
}

/** «1h30», «45min». */
fun duracaoLegivel(minutos: Int): String {
    val m = minutos.coerceAtLeast(0)
    if (m < 60) return "${m}min"
    val h = m / 60
    val resto = m % 60
    return if (resto == 0) "${h}h" else "${h}h%02d".format(resto)
}

fun duracaoDoBloco(b: BlocoRotina): Int = b.fim - b.inicio

fun periodoLegivel(set: RotinaSet): String? {
    fun bonito(iso: String): String {
        val parts = iso.split('-')
        if (parts.size < 3) return iso
        val meses = listOf(
            "jan", "fev", "mar", "abr", "mai", "jun",
            "jul", "ago", "set", "out", "nov", "dez",
        )
        val d = parts[2].toIntOrNull() ?: return iso
        val m = (parts[1].toIntOrNull() ?: 1) - 1
        return "$d/${meses.getOrElse(m) { "?" }}"
    }
    val de = set.de
    val ate = set.ate
    return when {
        de != null && ate != null -> "${bonito(de)} – ${bonito(ate)}"
        de != null -> "a partir de ${bonito(de)}"
        ate != null -> "até ${bonito(ate)}"
        else -> null
    }
}
