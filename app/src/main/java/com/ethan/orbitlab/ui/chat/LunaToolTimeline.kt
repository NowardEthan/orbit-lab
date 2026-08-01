package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Timeline de ferramentas (não-web) — recolhível, no idioma do «Raciocínio».
 *
 * Fechada, é só uma linha limpa (a ação em curso, ou "N passos" quando concluiu).
 * Aberta, revela a lista vertical de passos numa caixa com fio à esquerda —
 * a mesma linguagem do reasoning, nada de badges soltos sobre a bolha.
 */
@Composable
fun LunaToolTimeline(
    steps: List<LunaActionStep>,
    inicialmenteAberto: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return

    var aberto by remember { mutableStateOf(inicialmenteAberto) }
    val rodando = steps.firstOrNull { it.status == LunaActionStepStatus.RUNNING }
    val resumo = when {
        rodando != null -> rodando.label
        steps.size == 1 -> steps.first().label
        else -> "${steps.size} passos"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .orbitPressable { aberto = !aberto }
                .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (rodando != null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 1.5.dp,
                    color = OrbitTokens.bluePastel,
                )
            } else {
                Icon(
                    imageVector = if (aberto) {
                        Icons.Rounded.KeyboardArrowDown
                    } else {
                        Icons.Rounded.KeyboardArrowRight
                    },
                    contentDescription = if (aberto) "Recolher ações" else "Ver ações",
                    tint = OrbitTokens.textLowN,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = resumo,
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        AnimatedVisibility(
            visible = aberto,
            enter = OrbitMotion.expandEnter(),
            exit = OrbitMotion.expandExit(),
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrbitTokens.graphiteSurf)
                    .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(10.dp)),
            ) {
                Box(
                    Modifier
                        .width(2.5.dp)
                        .fillMaxHeight()
                        .background(OrbitTokens.bluePastel.copy(alpha = 0.45f)),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    steps.forEach { step -> LinhaPasso(step) }
                }
            }
        }
    }
}

@Composable
private fun LinhaPasso(step: LunaActionStep) {
    val rodando = step.status == LunaActionStepStatus.RUNNING
    val erro = step.status == LunaActionStepStatus.ERROR
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        // Marcador do passo — spinner enquanto roda, senão o glifo do tipo (quieto).
        Box(
            modifier = Modifier.size(15.dp).padding(top = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (rodando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = OrbitTokens.bluePastel,
                )
            } else {
                Text(
                    step.kind.icone(),
                    color = if (erro) OrbitTokens.danger else OrbitTokens.textLowN,
                    fontSize = 12.sp,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                step.label,
                color = when {
                    rodando -> OrbitTokens.bluePastel
                    erro -> OrbitTokens.danger
                    else -> OrbitTokens.textHiN
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
            )
            val detalhe = step.detail
            if (!detalhe.isNullOrBlank() && detalhe != step.label) {
                Text(
                    detalhe,
                    color = OrbitTokens.textLowN,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
