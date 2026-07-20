package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens

/**
 * Strip leve de ferramentas (não-web) — chips, sem painel de pesquisa.
 */
@Composable
fun LunaToolStrip(
    steps: List<LunaActionStep>,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return
    Row(
        modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEach { step ->
            ToolChip(step)
        }
    }
}

@Composable
private fun ToolChip(step: LunaActionStep) {
    val running = step.status == LunaActionStepStatus.RUNNING
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .border(1.dp, OrbitTokens.borderSoft, shape)
            .background(
                if (running) OrbitTokens.accentSoft else OrbitTokens.surfaceRaised,
                shape,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (running) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = OrbitTokens.accentText,
            )
        } else {
            Text(
                step.kind.icone(),
                color = OrbitTokens.textMid,
                fontSize = 11.sp,
            )
        }
        Text(
            step.label,
            color = OrbitTokens.textHigh,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
