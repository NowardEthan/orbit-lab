package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Orquestra a UI de ações da Luna (timeline agentica first):
 * - plano (coleira) → [LunaPlanChecklist]
 * - tools (web, imagem, memória, artefato…) → [LunaToolTimeline]
 *
 * Pesquisa web entra no mesmo fio das outras tools — sem painel/dossiê à parte.
 */
@Composable
fun LunaActionTimeline(
    run: LunaActionRun,
    @Suppress("UNUSED_PARAMETER") liveLabel: String? = null,
    inicialmenteAberto: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tools = run.toolSteps()
    val aoVivo = run.status == LunaActionRunStatus.RUNNING

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (run.plano.isNotEmpty()) {
            LunaPlanChecklist(plano = run.plano, aoVivo = aoVivo)
        }
        if (tools.isNotEmpty()) {
            LunaToolTimeline(
                steps = tools,
                inicialmenteAberto = inicialmenteAberto || aoVivo,
            )
        }
    }
}
