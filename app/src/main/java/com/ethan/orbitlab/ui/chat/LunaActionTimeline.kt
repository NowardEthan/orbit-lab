package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Orquestra a UI de ações da Luna:
 * - plano (coleira) → [LunaPlanChecklist] sempre à vista
 * - tools (imagem/vídeo/memória…) → [LunaToolTimeline]
 * - web → [LunaResearchPanel] só se [LunaActionProfile.DEEP_RESEARCH]
 */
@Composable
fun LunaActionTimeline(
    run: LunaActionRun,
    liveLabel: String? = null,
    inicialmenteAberto: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tools = run.toolSteps()
    val research = run.toLegacyResearchRun()
    val aoVivo = run.status == LunaActionRunStatus.RUNNING

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (run.plano.isNotEmpty()) {
            LunaPlanChecklist(plano = run.plano, aoVivo = aoVivo)
        }
        if (tools.isNotEmpty()) {
            LunaToolTimeline(steps = tools, inicialmenteAberto = inicialmenteAberto)
        }
        if (run.isDeepResearch() && research != null) {
            LunaResearchPanel(
                run = research,
                liveLabel = liveLabel,
                inicialmenteAberto = inicialmenteAberto,
            )
        }
    }
}
