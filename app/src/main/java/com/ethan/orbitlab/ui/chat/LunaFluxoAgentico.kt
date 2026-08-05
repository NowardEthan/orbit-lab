package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens

/**
 * Fio agentico estilo Cursor: checklist (se houver) + narração e tools **intercalados**.
 * Tools viram clusters colapsáveis (texto quieto + chevron), sem ícones estranhos.
 */
@Composable
fun LunaFluxoAgentico(
    run: LunaActionRun,
    textoFallback: String,
    aoVivo: Boolean = false,
    mostrarCaret: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val fluxo = run.fluxo
    if (fluxo.isEmpty()) {
        Column(modifier.fillMaxWidth()) {
            LunaActionTimeline(
                run = run,
                inicialmenteAberto = aoVivo || run.status == LunaActionRunStatus.RUNNING,
            )
            if (textoFallback.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                if (aoVivo) {
                    TextoAgenticoAoVivo(texto = textoFallback)
                } else {
                    LunaMarkdown(content = textoFallback)
                }
            }
            if (mostrarCaret) {
                Spacer(Modifier.height(4.dp))
                StreamCaret()
            }
        }
        return
    }

    val stepsById = run.steps.associateBy { it.id }
    val blocos = remember(fluxo, run.steps) {
        agruparFluxoUi(fluxo, stepsById)
    }

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        if (run.plano.isNotEmpty()) {
            LunaPlanChecklist(plano = run.plano, aoVivo = aoVivo)
        }

        blocos.forEachIndexed { index, bloco ->
            val ultimo = index == blocos.lastIndex
            val chave = when (bloco) {
                is FluxoUiBloco.Narracao ->
                    if (aoVivo && ultimo) "narracao-viva" else "narracao-${bloco.texto.hashCode()}"
                is FluxoUiBloco.Tools ->
                    "tools-${bloco.steps.joinToString("|") { "${it.id}:${it.status}" }}"
            }
            key(chave) {
                when (bloco) {
                    is FluxoUiBloco.Narracao -> {
                        val t = bloco.texto.trim()
                        if (t.isNotEmpty()) {
                            if (aoVivo && ultimo) {
                                TextoAgenticoAoVivo(texto = t)
                            } else {
                                LunaMarkdown(content = t)
                            }
                        }
                    }
                    is FluxoUiBloco.Tools -> {
                        LunaToolClusterCursor(steps = bloco.steps)
                    }
                }
            }
        }

        if (mostrarCaret) {
            StreamCaret()
        }
    }
}

@Composable
private fun TextoAgenticoAoVivo(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = texto,
        color = OrbitTokens.textHiN,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
        modifier = modifier.fillMaxWidth(),
    )
}

private sealed class FluxoUiBloco {
    data class Narracao(val texto: String) : FluxoUiBloco()
    data class Tools(val steps: List<LunaActionStep>) : FluxoUiBloco()
}

/** Junta Acaos consecutivas num cluster (como o lote «Edited N files…» do Cursor). */
private fun agruparFluxoUi(
    fluxo: List<LunaTurnoSegmento>,
    stepsById: Map<String, LunaActionStep>,
): List<FluxoUiBloco> {
    val out = mutableListOf<FluxoUiBloco>()
    var batch = mutableListOf<LunaActionStep>()

    fun flushBatch() {
        if (batch.isEmpty()) return
        out += FluxoUiBloco.Tools(batch.toList())
        batch = mutableListOf()
    }

    for (seg in fluxo) {
        when (seg) {
            is LunaTurnoSegmento.Narracao -> {
                flushBatch()
                if (seg.texto.isNotBlank()) out += FluxoUiBloco.Narracao(seg.texto)
            }
            is LunaTurnoSegmento.Acao -> {
                val step = stepsById[seg.stepId] ?: continue
                if (step.ferramenta != null && ehFerramentaDePlano(step.ferramenta)) continue
                batch += step
            }
        }
    }
    flushBatch()
    return out
}
