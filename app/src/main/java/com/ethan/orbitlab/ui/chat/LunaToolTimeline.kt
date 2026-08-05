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
 * Passo de ferramenta no fio.
 * - [comoChip] true (default): chrome grafite distinto da prosa da Luna (fluxo Cursor).
 * - false: linha quieta dentro da timeline recolhível (sem caixa dupla).
 */
@Composable
fun LunaToolPassoInline(
    step: LunaActionStep,
    modifier: Modifier = Modifier,
    comoChip: Boolean = true,
) {
    val rodando = step.status == LunaActionStepStatus.RUNNING
    val erro = step.status == LunaActionStepStatus.ERROR
    val labelLimpo = remember(step.label) {
        step.label.lineSequence().firstOrNull().orEmpty()
            .replace(Regex("[#*_`~]"), "")
            .trim()
            .let { if (it.length > 48) it.take(45) + "…" else it }
    }
    val detalhe = step.detail?.trim()?.takeIf { d ->
        d.isNotBlank() &&
            d != step.label &&
            !eCodigoOuPayloadTecnico(d) &&
            !pareceIdHash(d) &&
            !step.label.contains(d, ignoreCase = true)
    }
    val shape = RoundedCornerShape(8.dp)
    val rowMod = if (comoChip) {
        modifier
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(
                1.dp,
                when {
                    erro -> OrbitTokens.danger.copy(alpha = 0.35f)
                    rodando -> OrbitTokens.bluePastel.copy(alpha = 0.28f)
                    else -> OrbitTokens.graphiteHair
                },
                shape,
            )
            .padding(horizontal = 10.dp, vertical = 7.dp)
    } else {
        modifier.fillMaxWidth()
    }
    Row(
        modifier = rowMod,
        verticalAlignment = if (comoChip) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(if (comoChip) 8.dp else 9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (comoChip) 14.dp else 15.dp)
                .then(if (!comoChip) Modifier.padding(top = 2.dp) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (rodando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.4.dp,
                    color = OrbitTokens.bluePastel,
                )
            } else {
                Text(
                    step.kind.icone(),
                    color = if (erro) OrbitTokens.danger else OrbitTokens.textLowN,
                    fontSize = if (comoChip) 11.sp else 12.sp,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                labelLimpo,
                color = when {
                    rodando -> OrbitTokens.bluePastel
                    erro -> OrbitTokens.danger
                    comoChip -> OrbitTokens.textMidN
                    else -> OrbitTokens.textHiN
                },
                fontSize = if (comoChip) 12.sp else 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = if (comoChip) 15.sp else 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detalhe != null) {
                Text(
                    detalhe,
                    color = OrbitTokens.textLowN,
                    fontSize = if (comoChip) 11.sp else 12.sp,
                    lineHeight = if (comoChip) 14.sp else 16.sp,
                    maxLines = if (comoChip) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
