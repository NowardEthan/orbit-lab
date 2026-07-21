package com.ethan.orbitlab.data.latencia

import android.util.Log
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Sonda leve de latência no dispositivo — compara caminhos no Logcat.
 *
 * Filtrar: `adb logcat -s OrbitLatencia`
 *
 * Não substitui o harness CLI (`scripts/bench-luna-latencia`); serve para
 * turnos reais no telemóvel enquanto usas o app.
 */
object LatenciaProbe {
    private const val TAG = "OrbitLatencia"
    private const val MAX = 40

    data class Amostra(
        val caminho: String,
        val totalMs: Long,
        val ttfbMs: Long?,
        val chars: Int,
        val ok: Boolean,
        val atMs: Long = System.currentTimeMillis(),
    )

    private val recent = ConcurrentLinkedDeque<Amostra>()

    fun record(
        caminho: String,
        totalMs: Long,
        ttfbMs: Long? = null,
        chars: Int = 0,
        ok: Boolean = true,
        detalhe: String? = null,
    ) {
        val a = Amostra(caminho, totalMs, ttfbMs, chars, ok)
        recent.addFirst(a)
        while (recent.size > MAX) recent.removeLast()
        val ttfb = ttfbMs?.let { "${it}ms" } ?: "—"
        val extra = detalhe?.takeIf { it.isNotBlank() }?.let { " | $it" } ?: ""
        Log.i(
            TAG,
            "caminho=$caminho total=${totalMs}ms ttfb=$ttfb chars=$chars ok=$ok$extra",
        )
    }

    fun recentes(): List<Amostra> = recent.toList()

    fun resumo(caminho: String? = null): String {
        val list = recentes().filter { caminho == null || it.caminho == caminho }
        if (list.isEmpty()) return "sem amostras"
        val totals = list.map { it.totalMs }
        val mean = totals.average()
        return "n=${list.size} total_mean=${mean.toLong()}ms " +
            "min=${totals.minOrNull()}ms max=${totals.maxOrNull()}ms"
    }
}
