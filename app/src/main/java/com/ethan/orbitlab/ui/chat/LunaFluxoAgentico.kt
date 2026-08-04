package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Fio agentico estilo Cursor: checklist (se houver) + narração e tools **intercalados**
 * na ordem do SSE — não um bloco de badges em cima e o texto embaixo.
 *
 * Se [LunaActionRun.fluxo] estiver vazio, cai no legado ([LunaActionTimeline] + markdown).
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
                LunaMarkdown(content = textoFallback)
            }
            if (mostrarCaret) {
                Spacer(Modifier.height(4.dp))
                StreamCaret()
            }
        }
        return
    }

    val stepsById = run.steps.associateBy { it.id }

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (run.plano.isNotEmpty()) {
            LunaPlanChecklist(plano = run.plano, aoVivo = aoVivo)
        }

        fluxo.forEach { seg ->
            when (seg) {
                is LunaTurnoSegmento.Narracao -> {
                    val t = seg.texto.trim()
                    if (t.isNotEmpty()) {
                        LunaMarkdown(content = t)
                    }
                }
                is LunaTurnoSegmento.Acao -> {
                    val step = stepsById[seg.stepId] ?: return@forEach
                    // Meta-tools de plano não entram no fio (a checklist já é a UI).
                    if (step.ferramenta != null && ehFerramentaDePlano(step.ferramenta)) return@forEach
                    LunaToolPassoInline(
                        step = step,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }

        if (mostrarCaret) {
            StreamCaret()
        }
    }
}
