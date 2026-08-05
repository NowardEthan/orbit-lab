package com.ethan.orbitlab.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                .liveSurface(ativo = rodando, erro = false)
                .orbitPressable {
                    aberto = !aberto
                    usuarioMexeu = true
                }
                .padding(horizontal = if (rodando) 8.dp else 0.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (rodando) {
                LunaStatusDot(status = LunaActionStepStatus.RUNNING, size = 10.dp)
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
            if (rodando) {
                LunaTextoVivo(
                    text = resumo,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false),
                )
            } else {
                Text(
                    text = resumo,
                    color = OrbitTokens.textMidN,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
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
    val partes = remember(step) { partesLinhaDetalhe(step) }
    val stats = remember(step) { statsDiffDoLabel(step) }
    val snippet = remember(step) { snippetDoDetail(step) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        LunaStatusDot(status = step.status, size = 7.dp, modifier = Modifier.padding(top = 4.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
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
                    append(partes.acao)
                }
                if (partes.referencia.isNotBlank()) {
                    append(" · ")
                    withStyle(
                        SpanStyle(
                            color = if (erro) OrbitTokens.danger else OrbitTokens.textMidN,
                            fontWeight = FontWeight.Medium,
                        ),
                    ) {
                        append(partes.referencia)
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
            if (snippet.isNotBlank()) {
                Text(
                    text = snippet,
                    color = OrbitTokens.textLowN,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (step.queries.isNotEmpty()) {
                Text(
                    text = step.queries.take(2).joinToString(" · ") { q ->
                        val t = q.trim().trim('"')
                        if (t.length > 42) "“${t.take(39)}…”" else "“$t”"
                    },
                    color = OrbitTokens.textLowN,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (step.sources.isNotEmpty()) {
                FontesCompactas(fontes = step.sources)
            }
        }
    }
}

/** Chips de domínio clicáveis — pesquisa no fio, sem painel à parte. */
@Composable
private fun FontesCompactas(fontes: List<LunaWebFonte>) {
    val context = LocalContext.current
    val unicas = remember(fontes) { fontes.distinctBy { it.url }.take(6) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        unicas.forEach { fonte ->
            val dominio = fonte.domain.removePrefix("www.").ifBlank { "fonte" }
            Text(
                text = dominio,
                color = OrbitTokens.bluePastel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(OrbitTokens.graphiteRaised.copy(alpha = 0.66f))
                    .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(999.dp))
                    .orbitPressable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(fonte.url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
        if (fontes.distinctBy { it.url }.size > unicas.size) {
            Text(
                text = "+${fontes.distinctBy { it.url }.size - unicas.size}",
                color = OrbitTokens.textLowN,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

private data class PartesLinhaDetalhe(val acao: String, val referencia: String)

/** Resumo colapsado: «Consultou estrutura · Livro» / contagens. */
internal fun resumoClusterTools(steps: List<LunaActionStep>): String {
    if (steps.isEmpty()) return ""
    if (steps.size == 1) {
        val p = partesLinhaDetalhe(steps.first())
        return if (p.referencia.isNotBlank()) "${p.acao} · ${p.referencia}" else p.acao.ifBlank {
            limparLabelTool(steps.first().label)
        }
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

/**
 * Uma linha limpa: ação + referência do artefato.
 * Se não há ref, cai no label do toolMeta (nunca verbo sozinho «Leu»).
 */
private fun partesLinhaDetalhe(step: LunaActionStep): PartesLinhaDetalhe {
    val label = limparLabelTool(step.label)
    val refCompleta = referenciaArtefato(step)
    // detail pode ser «Título · preview» — a 1ª parte é a referência principal.
    val ref = refCompleta.substringBefore(" · ").trim().ifBlank { refCompleta }
    val acao = when (step.ferramenta) {
        "ler_estrutura" -> if (step.status == LunaActionStepStatus.RUNNING) "Consultando estrutura" else "Consultou estrutura"
        "inserir_blocos" -> if (step.status == LunaActionStepStatus.RUNNING) "Acrescentando" else "Acrescentou"
        "editar_artefato", "editar_trecho_artefato", "editar_bloco_artefato" ->
            if (step.status == LunaActionStepStatus.RUNNING) "Editando" else "Editou"
        "ler_artefato", "ler_secao", "ler_bloco" ->
            if (step.status == LunaActionStepStatus.RUNNING) "Lendo" else "Leu"
        "criar_artefato" -> if (step.status == LunaActionStepStatus.RUNNING) "Criando" else "Criou"
        "web_search" -> if (step.status == LunaActionStepStatus.RUNNING) "Pesquisando" else "Pesquisou"
        "ler_url" -> if (step.status == LunaActionStepStatus.RUNNING) "Lendo link" else "Leu link"
        "verificar_fontes" -> if (step.status == LunaActionStepStatus.RUNNING) "Cruzando fontes" else "Cruzou fontes"
        "listar_artefatos" -> if (step.status == LunaActionStepStatus.RUNNING) "Listando estante" else "Listou a estante"
        "consultar_neuronio" -> {
            val esp = step.neuronioEspecialidade?.lowercase().orEmpty()
            val nome = when (esp) {
                "auditoria" -> "Auditoria"
                "canone" -> "Cânone"
                "pesquisa" -> "Pesquisa"
                else -> "Orientação"
            }
            if (step.status == LunaActionStepStatus.RUNNING) "Consultando $nome" else "Consultou $nome"
        }
        else -> {
            return PartesLinhaDetalhe(acao = label, referencia = "")
        }
    }
    if (ref.isBlank()) {
        // Sem título/alvo: mostra o label rico do toolMeta em vez do verbo sozinho.
        return PartesLinhaDetalhe(acao = label.ifBlank { acao }, referencia = "")
    }
    if (label.contains(ref, ignoreCase = true)) {
        return PartesLinhaDetalhe(acao = label, referencia = "")
    }
    return PartesLinhaDetalhe(acao = acao, referencia = ref)
}

/** Segunda linha quieta: trecho depois de « · » no detail (preview / seção). */
private fun snippetDoDetail(step: LunaActionStep): String {
    val d = step.detail?.trim().orEmpty()
    if (!d.contains(" · ")) return ""
    val trecho = d.substringAfter(" · ").trim()
    if (trecho.isBlank() || pareceIdHash(trecho)) return ""
    return if (trecho.length > 56) trecho.take(53) + "…" else trecho
}

/** Título/alvo humano do passo — nunca o markdown nem eco do próprio label. */
private fun referenciaArtefato(step: LunaActionStep): String {
    val d = step.detail?.trim().orEmpty()
    if (d.isBlank() || pareceIdHash(d) || d.startsWith("{") || d.startsWith("[")) return ""
    if (d.length > 96) return ""
    val label = limparLabelTool(step.label)
    // Detail que é só eco do label («a estrutura», «no documento») → ignora.
    val ruido = listOf(
        "a estrutura", "estrutura", "o documento", "no documento", "documento",
        "o artefato", "artefato", "a seção", "seção", "o bloco", "bloco",
    )
    val principal = d.substringBefore(" · ").trim().ifBlank { d }
    if (ruido.any { principal.equals(it, ignoreCase = true) }) return ""
    if (label.contains(principal, ignoreCase = true) && !d.contains(" · ")) return ""
    // Tirar aspas extras do título (mantém «Título · preview» no detail bruto).
    return d.trim('"').trim()
}

/** Extrai +N -M se o label/detail trouxer (ex. futuro do core). */
private fun statsDiffDoLabel(step: LunaActionStep): Pair<Int, Int>? {
    val fonte = listOfNotNull(step.label, step.detail).joinToString(" ")
    val m = Regex("""\+(\d+)\s+-(\d+)""").find(fonte) ?: return null
    return m.groupValues[1].toInt() to m.groupValues[2].toInt()
}
