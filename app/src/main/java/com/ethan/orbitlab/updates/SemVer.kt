package com.ethan.orbitlab.updates

/** Comparação simples de versões "x.y.z" (sem pre-release). */

fun parseVersion(v: String): List<Int> =
    v.trim().split('.').map { part ->
        part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
    }

/** -1 se a < b, 0 se iguais, 1 se a > b. */
fun compareVersions(a: String, b: String): Int {
    val pa = parseVersion(a)
    val pb = parseVersion(b)
    val len = maxOf(pa.size, pb.size)
    for (i in 0 until len) {
        val da = pa.getOrElse(i) { 0 }
        val db = pb.getOrElse(i) { 0 }
        if (da > db) return 1
        if (da < db) return -1
    }
    return 0
}

/** `candidate` é estritamente mais novo que `current`? */
fun isNewer(candidate: String, current: String): Boolean =
    compareVersions(candidate, current) > 0
