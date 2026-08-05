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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Timeline de ferramentas (não-web) — recolhível, no idioma do «Raciocínio».
 *
 * Enquanto há passo RUNNING, fica aberta (estilo Cursor). Ao terminar, recolhe
 * com resumo do último passo (+ contagem se houver vários).
 */
@Composable
fun LunaToolTimeline(
    steps: List<LunaActionStep>,
    inicialmenteAberto: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return

    val rodando = steps.firstOrNull { it.status == LunaActionStepStatus.RUNNING }
    var aberto by remember { mutableStateOf(inicialmenteAberto || rodando != null) }
    var usuarioFechou by remember { mutableStateOf(false) }

    // Abre sozinha quando começa a trabalhar; recolhe ao terminar (salvo se ele abriu à mão).
    LaunchedEffect(rodando != null) {
        if (rodando != null) {
            aberto = true
            usuarioFechou = false
        } else if (!usuarioFechou) {
            aberto = false
        }
    }

    val ultimoFeito = steps.lastOrNull {
        it.status == LunaActionStepStatus.DONE || it.status == LunaActionStepStatus.ERROR
    }
    val resumoBruto = when {
        rodando != null -> rodando.label
        steps.size == 1 -> steps.first().label
        ultimoFeito != null -> {
            val n = steps.size
            if (n > 1) "${ultimoFeito.label} · $n passos" else ultimoFeito.label
        }
        else -> "${steps.size} passos"
    }
    val resumo = remember(resumoBruto) {
        resumoBruto.lineSequence().firstOrNull().orEmpty()
            .replace(Regex("[#*_`~]"), "")
            .trim()
            .let { if (it.length > 52) it.take(49) + "…" else it }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .orbitPressable {
                    aberto = !aberto
                    usuarioFechou = !aberto
                }
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                    steps.forEach { step ->
                        LunaToolPassoInline(step = step, comoChip = false)
                    }
                }
            }
        }
    }
}

/**
 * Linha quieta dentro da timeline legado — sem glifo ⊟/⌕ (no aparelho virava «Icon»).
 * No fio agentico o cluster colapsável é [LunaToolClusterCursor].
 */
@Composable
fun LunaToolPassoInline(
    step: LunaActionStep,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") comoChip: Boolean = true,
) {
    val rodando = step.status == LunaActionStepStatus.RUNNING
    val erro = step.status == LunaActionStepStatus.ERROR
    val labelLimpo = remember(step.label, step.papelUi) {
        val base = step.label.lineSequence().firstOrNull().orEmpty()
            .replace(Regex("[#*_`~]"), "")
            .trim()
            .let { if (it.length > 52) it.take(49) + "…" else it }
        if (step.papelUi == "neuronio" && !base.contains("Neurônio", ignoreCase = true)) {
            "Neurônio · $base"
        } else {
            base
        }
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (rodando) {
            CircularProgressIndicator(
                modifier = Modifier.size(11.dp),
                strokeWidth = 1.3.dp,
                color = OrbitTokens.bluePastel,
            )
        }
        Text(
            labelLimpo,
            color = when {
                rodando -> OrbitTokens.bluePastel
                erro -> OrbitTokens.danger
                else -> OrbitTokens.textMidN
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun eCodigoOuPayloadTecnico(texto: String): Boolean {
    val t = texto.lowercase().trim()
    return t.startsWith("{") || t.startsWith("[") || t.startsWith("```") ||
        t.contains("import ") || t.contains("export ") || t.contains("function") ||
        t.contains("class ") || t.contains("fun ") || t.contains("diff") ||
        t.contains("<html>") || t.contains("const ") || t.contains("val ") ||
        t.contains("void ") || t.contains("return ") || t.contains("\n")
}
