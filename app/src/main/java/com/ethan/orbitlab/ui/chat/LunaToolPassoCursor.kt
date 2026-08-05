package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Badge de ferramenta estilo Cursor: texto quieto + chevron, **sem** glifo/ícone estranho.
 * Colapsado = resumo; expandido = linhas de detalhe (arg, fontes, stats se houver).
 */
@Composable
fun LunaToolClusterCursor(
    steps: List<LunaActionStep>,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return
    val rodando = steps.any { it.status == LunaActionStepStatus.RUNNING }
    var aberto by remember(steps.map { it.id }.joinToString()) {
        mutableStateOf(rodando)
    }
    var usuarioMexeu by remember { mutableStateOf(false) }

    LaunchedEffect(rodando) {
        if (rodando) {
            aberto = true
            usuarioMexeu = false
        } else if (!usuarioMexeu) {
            // Ao terminar o lote, recolhe — o fio fica limpo como no Cursor.
            aberto = false
        }
    }

    val resumo = remember(steps) { resumoClusterTools(steps) }

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .orbitPressable {
                    aberto = !aberto
                    usuarioMexeu = true
                }
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (rodando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.4.dp,
                    color = OrbitTokens.bluePastel,
                )
            } else {
                Icon(
                    imageVector = if (aberto) {
                        Icons.Rounded.KeyboardArrowDown
                    } else {
                        Icons.Rounded.KeyboardArrowRight
                    },
                    contentDescription = if (aberto) "Recolher ações" else "Ver detalhes",
                    tint = OrbitTokens.textLowN,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = resumo,
                color = if (rodando) OrbitTokens.bluePastel else OrbitTokens.textMidN,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }

        AnimatedVisibility(
            visible = aberto,
            enter = OrbitMotion.expandEnter(),
            exit = OrbitMotion.expandExit(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 2.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                steps.forEach { step ->
                    LinhaDetalheTool(step)
                }
            }
        }
    }
}

@Composable
private fun LinhaDetalheTool(step: LunaActionStep) {
    val rodando = step.status == LunaActionStepStatus.RUNNING
    val erro = step.status == LunaActionStepStatus.ERROR
    val verbo = remember(step) { verboDetalheTool(step) }
    val alvo = remember(step) { alvoDetalheTool(step) }
    val stats = remember(step) { statsDiffDoLabel(step) }

    val linha = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = when {
                    erro -> OrbitTokens.danger
                    rodando -> OrbitTokens.bluePastel
                    else -> OrbitTokens.textLowN
                },
                fontWeight = FontWeight.Normal,
            ),
        ) {
            append(verbo)
        }
        if (alvo.isNotBlank()) {
            append(" ")
            withStyle(
                SpanStyle(
                    color = if (erro) OrbitTokens.danger else OrbitTokens.textMidN,
                    fontWeight = FontWeight.Medium,
                ),
            ) {
                append(alvo)
            }
        }
        if (stats != null) {
            append(" ")
            withStyle(SpanStyle(color = OrbitTokens.online, fontWeight = FontWeight.Medium)) {
                append("+${stats.first}")
            }
            append(" ")
            withStyle(SpanStyle(color = OrbitTokens.danger, fontWeight = FontWeight.Medium)) {
                append("-${stats.second}")
            }
        }
    }

    Text(
        text = linha,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Resumo colapsado: «Consultou estrutura, ajustou documento» / contagens. */
internal fun resumoClusterTools(steps: List<LunaActionStep>): String {
    if (steps.isEmpty()) return ""
    if (steps.size == 1) {
        return limparLabelTool(steps.first().label)
    }
    val reads = steps.count {
        it.kind == LunaActionStepKind.READ ||
            it.kind == LunaActionStepKind.SEARCH ||
            it.kind == LunaActionStepKind.VISION ||
            it.kind == LunaActionStepKind.VIDEO ||
            it.kind == LunaActionStepKind.MEMORY
    }
    val writes = steps.count { it.kind == LunaActionStepKind.WRITE }
    val runs = steps.count {
        it.kind == LunaActionStepKind.RUN || it.kind == LunaActionStepKind.VERIFY
    }
    val parts = buildList {
        if (reads > 0) add(if (reads == 1) "consultou 1 vez" else "consultou $reads vezes")
        if (writes > 0) add(if (writes == 1) "editou 1 vez" else "editou $writes vezes")
        if (runs > 0) add(if (runs == 1) "rodou 1 ação" else "rodou $runs ações")
        val outros = steps.size - reads - writes - runs
        if (outros > 0 && isEmpty()) {
            add(if (outros == 1) "1 ação" else "$outros ações")
        } else if (outros > 0) {
            add("+$outros")
        }
    }
    if (parts.isEmpty()) {
        return limparLabelTool(steps.last().label)
    }
    // Capitaliza só a primeira letra — resto quieto como no Cursor.
    val junto = parts.joinToString(", ")
    return junto.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun limparLabelTool(label: String): String =
    label.lineSequence().firstOrNull().orEmpty()
        .replace(Regex("[#*_`~]"), "")
        .trim()
        .removeSuffix("…")
        .trim()
        .let { if (it.length > 56) it.take(53) + "…" else it }

private fun verboDetalheTool(step: LunaActionStep): String {
    if (step.status == LunaActionStepStatus.RUNNING) {
        return when (step.kind) {
            LunaActionStepKind.READ, LunaActionStepKind.MEMORY -> "Lendo"
            LunaActionStepKind.SEARCH -> "Pesquisando"
            LunaActionStepKind.WRITE -> "Editando"
            LunaActionStepKind.VISION, LunaActionStepKind.VIDEO -> "Olhando"
            else -> "Trabalhando"
        }
    }
    return when (step.kind) {
        LunaActionStepKind.READ, LunaActionStepKind.MEMORY -> "Leu"
        LunaActionStepKind.SEARCH -> "Pesquisou"
        LunaActionStepKind.WRITE -> "Editou"
        LunaActionStepKind.VISION, LunaActionStepKind.VIDEO -> "Olhou"
        LunaActionStepKind.VERIFY -> "Conferiu"
        LunaActionStepKind.RUN -> "Rodou"
        else -> limparLabelTool(step.label).take(24).ifBlank { "Fez" }
    }
}

private fun alvoDetalheTool(step: LunaActionStep): String {
    val d = step.detail?.trim()?.takeIf {
        it.isNotBlank() &&
            !pareceIdHash(it) &&
            it.length < 80 &&
            !it.startsWith("{") &&
            !it.startsWith("[")
    }
    if (d != null) return d
    // Tira o verbo do label («Consultou a estrutura» → «a estrutura»).
    val label = limparLabelTool(step.label)
    val semVerbo = label
        .replace(Regex("^(Consultou|Lendo|Leu|Editou|Editando|Ajustou|Ajustando|Pesquisou|Pesquisando|Criou|Criando|Olhou|Olhando)\\s+", RegexOption.IGNORE_CASE), "")
        .trim()
    return semVerbo.take(56)
}

/** Extrai +N -M se o label/detail trouxer (ex. futuro do core). */
private fun statsDiffDoLabel(step: LunaActionStep): Pair<Int, Int>? {
    val fonte = listOfNotNull(step.label, step.detail).joinToString(" ")
    val m = Regex("""\+(\d+)\s+-(\d+)""").find(fonte) ?: return null
    return m.groupValues[1].toInt() to m.groupValues[2].toInt()
}
