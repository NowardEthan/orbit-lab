package com.ethan.orbitlab.domain.rotina

/** Uma alternativa está a vigorar nesta data? `ate` é o último dia INCLUÍDO. */
fun vigoraEm(set: RotinaSet, hojeISO: String): Boolean {
    val de = set.de ?: return false
    val ate = set.ate ?: return false
    return hojeISO >= de && hojeISO <= ate
}

/**
 * Qual rotina vigora numa data.
 * Entre alternativas que cobrem o dia, a que COMEÇA mais tarde ganha.
 * Nenhuma → Normal.
 */
fun rotinaVigenteEm(sets: List<RotinaSet>, hojeISO: String): String {
    val aVigorar = sets
        .filter { vigoraEm(it, hojeISO) }
        .sortedBy { it.de.orEmpty() }
    return aVigorar.lastOrNull()?.id ?: ROTINA_NORMAL
}
