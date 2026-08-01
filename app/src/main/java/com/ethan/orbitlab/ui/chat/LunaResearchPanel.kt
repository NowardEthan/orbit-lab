package com.ethan.orbitlab.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.delay

/**
 * Painel de pesquisa profunda — colapsável, com fontes e referências clicáveis.
 */
@Composable
fun LunaResearchPanel(
    run: LunaResearchRun,
    liveLabel: String? = null,
    inicialmenteAberto: Boolean = false,
) {
    var aberto by remember { mutableStateOf(inicialmenteAberto) }
    val done = run.status == LunaResearchRunStatus.DONE
    val fasesDone = run.fasesConcluidas()
    val fasesTotal = run.steps.size.coerceAtLeast(1)
    val progresso = fasesDone.toFloat() / fasesTotal

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusCard))
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(OrbitMetrics.radiusCard))
            .padding(12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .orbitPressable { aberto = !aberto },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = if (done) OrbitTokens.textMidN else OrbitTokens.bluePastel,
                modifier = Modifier.size(16.dp),
            )
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "PESQUISA PROFUNDA",
                        color = OrbitTokens.textLowN,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                    )
                    StatusChip(
                        texto = if (done) "Relatório pronto" else "A pesquisar",
                        destaque = !done,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = run.title,
                    color = OrbitTokens.textHiN,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = if (aberto) {
                    Icons.Rounded.KeyboardArrowDown
                } else {
                    Icons.Rounded.KeyboardArrowRight
                },
                contentDescription = if (aberto) "Recolher pesquisa" else "Ver pesquisa",
                tint = OrbitTokens.textLowN,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progresso },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = OrbitTokens.bluePastel,
            trackColor = OrbitTokens.graphiteRaised,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                append("$fasesDone/$fasesTotal fases")
                append(" · ${run.totalFontes()} fontes")
                append(" · ${run.totalConsultas()} consultas")
                if (run.totalCitacoes() > 0) append(" · ${run.totalCitacoes()} referências")
            },
            color = OrbitTokens.textLowN,
            fontSize = 11.sp,
        )

        if (!liveLabel.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = liveLabel,
                color = OrbitTokens.bluePastel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val stripFontes = remember(run) {
            run.steps.flatMap { it.sources }.distinctBy { it.url }.take(8)
        }
        if (stripFontes.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                stripFontes.forEach { fonte ->
                    DomainChip(fonte = fonte)
                }
            }
        }

        AnimatedVisibility(
            visible = aberto,
            enter = OrbitMotion.expandEnter(),
            exit = OrbitMotion.expandExit(),
        ) {
            Column(
                Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                run.steps
                    .filter {
                        it.status != LunaResearchStepStatus.PENDING ||
                            it.queries.isNotEmpty() ||
                            it.sources.isNotEmpty()
                    }
                    .forEach { step ->
                        ResearchStepCard(step)
                    }
            }
        }
    }
}

/**
 * Dossiê — a cara da PESQUISA PROFUNDA.
 *
 * A resposta da Luna, quando ela pesquisou na web, não sai numa bolha de chat comum:
 * vira um card de relatório — o que o distingue é SER um cartão (borda, lavagem no topo,
 * barra de progresso, trilha das fases), não uma cor própria. Cabeçalho "PESQUISA PROFUNDA"
 * + nº de fontes, o corpo da resposta, o rodapé com as fontes clicáveis, e as fases atrás
 * de um toque. Acento único: o azul pastel, como no resto do app.
 *
 * Serve os dois momentos: ao vivo (`streaming = true`, resposta a escrever com o caret
 * a piscar) e a mensagem já assente no histórico.
 */
@Composable
fun LunaDossieCard(
    run: LunaResearchRun,
    resposta: String,
    modifier: Modifier = Modifier,
    streaming: Boolean = false,
    liveLabel: String? = null,
    inicialmenteAberto: Boolean = false,
) {
    var detalhesAbertos by remember { mutableStateOf(inicialmenteAberto) }
    val done = run.status == LunaResearchRunStatus.DONE && !streaming
    val fasesDone = run.fasesConcluidas()
    val fasesTotal = run.steps.size.coerceAtLeast(1)
    val progresso = fasesDone.toFloat() / fasesTotal
    val temResposta = resposta.isNotBlank()

    val stripFontes = remember(run) {
        run.steps.flatMap { it.sources }.distinctBy { it.url }.take(8)
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusCard))
            .background(OrbitTokens.graphiteSurf)
            .border(
                1.dp,
                OrbitTokens.bluePastel.copy(alpha = 0.28f),
                RoundedCornerShape(OrbitMetrics.radiusCard),
            ),
    ) {
        // ── Cabeçalho com lavagem pastel ───────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .background(OrbitTokens.bluePastel.copy(alpha = 0.09f))
                .orbitPressable { detalhesAbertos = !detalhesAbertos }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.TravelExplore,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(18.dp),
            )
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "PESQUISA PROFUNDA",
                        color = OrbitTokens.bluePastel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                    if (!done) {
                        StatusChip(texto = "A pesquisar", destaque = true)
                    }
                }
                if (run.title.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = run.title,
                        color = OrbitTokens.textHiN,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val stats = dossieStats(run)
                if (stats.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stats,
                        color = OrbitTokens.bluePastel.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Icon(
                imageVector = if (detalhesAbertos) {
                    Icons.Rounded.KeyboardArrowDown
                } else {
                    Icons.Rounded.KeyboardArrowRight
                },
                contentDescription = if (detalhesAbertos) "Recolher fases" else "Ver fases",
                tint = OrbitTokens.textLowN,
                modifier = Modifier.size(18.dp),
            )
        }

        if (!done) {
            LinearProgressIndicator(
                progress = { progresso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = OrbitTokens.bluePastel,
                trackColor = OrbitTokens.bluePastel.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Butt,
            )
        }

        // ── Corpo: a timeline viva (enquanto pesquisa) → a resposta (o relatório) ───
        val passosVivos = remember(run) {
            run.steps.filter {
                it.status != LunaResearchStepStatus.PENDING || it.queries.isNotEmpty()
            }
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            when {
                temResposta -> {
                    LunaMarkdown(content = resposta)
                    if (streaming) {
                        Spacer(Modifier.height(4.dp))
                        DossieCaret()
                    }
                }
                // Estilo IDE: a Luna trabalhando à vista — cada passo surge na trilha,
                // o corrente pulsa, os concluídos ganham o ponto cheio.
                streaming && passosVivos.isNotEmpty() -> {
                    ResearchLiveTimeline(passosVivos)
                }
                streaming -> {
                    Text(
                        text = liveLabel?.takeIf { it.isNotBlank() } ?: "Começando a pesquisa…",
                        color = OrbitTokens.bluePastel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // ── Rodapé: as fontes, sempre à mão ────────────────────────────────────────
        if (stripFontes.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OrbitTokens.graphiteHair),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                stripFontes.forEach { fonte -> DomainChip(fonte = fonte) }
            }
        }

        // ── Fases detalhadas (queries, fontes por passo, referências) ──────────────
        AnimatedVisibility(
            visible = detalhesAbertos,
            enter = OrbitMotion.expandEnter(),
            exit = OrbitMotion.expandExit(),
        ) {
            Column(
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = buildString {
                        append("$fasesDone/$fasesTotal fases")
                        append(" · ${run.totalConsultas()} consultas")
                        if (run.totalCitacoes() > 0) append(" · ${run.totalCitacoes()} referências")
                    },
                    color = OrbitTokens.textLowN,
                    fontSize = 11.sp,
                )
                run.steps
                    .filter {
                        it.status != LunaResearchStepStatus.PENDING ||
                            it.queries.isNotEmpty() ||
                            it.sources.isNotEmpty()
                    }
                    .forEach { step -> ResearchStepCard(step) }
            }
        }
    }
}

@Composable
private fun DossieCaret() {
    val infinite = rememberInfiniteTransition(label = "dossieCaret")
    val a by infinite.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dossieCaretA",
    )
    Box(
        Modifier
            .width(2.dp)
            .height(13.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(OrbitTokens.bluePastel.copy(alpha = a)),
    )
}

/** "6 fontes · 4 consultas · 3 referências" — só o que já tem número. */
private fun dossieStats(run: LunaResearchRun): String {
    val partes = mutableListOf<String>()
    val f = run.totalFontes()
    val c = run.totalConsultas()
    val r = run.totalCitacoes()
    if (f > 0) partes += "$f ${if (f == 1) "fonte" else "fontes"}"
    if (c > 0) partes += "$c ${if (c == 1) "consulta" else "consultas"}"
    if (r > 0) partes += "$r ${if (r == 1) "referência" else "referências"}"
    return partes.joinToString(" · ")
}

/**
 * Timeline viva da pesquisa — a Luna trabalhando à vista, estilo agente de IDE.
 *
 * Um trilho vertical liga os passos; cada um entra deslizando, o corrente pulsa com um
 * halo, os concluídos ganham o ponto cheio. O rótulo já vem do servidor ao vivo
 * ("Pesquisando 'X'" → "Pesquisar 'X'", "Cruzando as fontes" → "Cruzou as fontes").
 */
@Composable
private fun ResearchLiveTimeline(passos: List<LunaResearchStep>) {
    val pulso = rememberInfiniteTransition(label = "timelinePulso")
    val halo by pulso.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(760),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timelineHalo",
    )

    // A pesquisa é rápida e PARALELA — a Luna dispara vários sites de uma vez, então os
    // passos chegam quase todos no mesmo jato. Se desenhássemos tudo junto, viraria um
    // bloco que "aparece" (o "woow instantâneo"). Aqui um portão revela um passo de cada
    // vez pra dar pra acompanhar, com cadência adaptativa: acelera quando acumulou fila,
    // pra a apresentação nunca ficar muito atrás do que ela já fez de verdade.
    var revelados by remember { mutableStateOf(minOf(1, passos.size)) }
    LaunchedEffect(passos.size) {
        while (revelados < passos.size) {
            val restam = passos.size - revelados
            val cadencia = when {
                restam > 6 -> 170L
                restam > 3 -> 320L
                else -> 480L
            }
            delay(cadencia)
            revelados++
        }
    }
    val mostrados = passos.take(revelados.coerceIn(0, passos.size))

    Column {
        mostrados.forEachIndexed { i, passo ->
            TimelineRow(
                passo = passo,
                primeiro = i == 0,
                ultimo = i == mostrados.lastIndex,
                // O passo da frente pulsa como "estou nele" enquanto ainda há passos por
                // revelar — mesmo que o servidor já os tenha marcado como concluídos.
                ativo = i == mostrados.lastIndex && revelados < passos.size,
                halo = halo,
            )
        }
    }
}

@Composable
private fun TimelineRow(
    passo: LunaResearchStep,
    primeiro: Boolean,
    ultimo: Boolean,
    ativo: Boolean,
    halo: Float,
) {
    val erro = passo.status == LunaResearchStepStatus.ERROR
    val pendente = passo.status == LunaResearchStepStatus.PENDING
    // "Trabalhando à vista": ou o servidor marcou RUNNING, ou é o passo da frente na
    // revelação paced. Nos dois casos o ponto pulsa e o rótulo ganha peso.
    val rodando = (passo.status == LunaResearchStepStatus.RUNNING || ativo) && !erro
    val corPonto = when {
        erro -> OrbitTokens.danger
        rodando -> OrbitTokens.bluePastel
        pendente -> OrbitTokens.textLowN
        else -> OrbitTokens.bluePastel.copy(alpha = 0.82f)
    }
    val corTrilho = OrbitTokens.bluePastel.copy(alpha = 0.22f)

    // Entrada: o passo surge deslizando de baixo, uma vez.
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(passo.id) { entrada.animateTo(1f, tween(300)) }

    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .graphicsLayer {
                alpha = entrada.value
                translationY = (1f - entrada.value) * 7.dp.toPx()
            },
    ) {
        Box(Modifier.width(22.dp).fillMaxHeight()) {
            Canvas(Modifier.fillMaxHeight().width(22.dp)) {
                val cx = size.width / 2f
                val dotY = 9.dp.toPx()
                val sw = 1.5.dp.toPx()
                if (!primeiro) {
                    drawLine(corTrilho, Offset(cx, 0f), Offset(cx, dotY), sw)
                }
                if (!ultimo) {
                    drawLine(corTrilho, Offset(cx, dotY), Offset(cx, size.height), sw)
                }
                if (rodando) {
                    drawCircle(
                        OrbitTokens.bluePastel.copy(alpha = halo),
                        radius = 7.dp.toPx(),
                        center = Offset(cx, dotY),
                    )
                }
                if (pendente) {
                    drawCircle(
                        corPonto,
                        radius = 3.dp.toPx(),
                        center = Offset(cx, dotY),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                } else {
                    drawCircle(corPonto, radius = 4.dp.toPx(), center = Offset(cx, dotY))
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 4.dp, bottom = 14.dp),
        ) {
            Text(
                text = passo.label,
                color = if (rodando) OrbitTokens.textHiN else OrbitTokens.textMidN,
                fontSize = 13.sp,
                fontWeight = if (rodando) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = when {
                passo.queries.isNotEmpty() -> passo.queries.joinToString(" · ")
                passo.sources.isNotEmpty() ->
                    "${passo.sources.size} ${if (passo.sources.size == 1) "fonte" else "fontes"}"
                else -> null
            }
            if (!sub.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = sub,
                    color = OrbitTokens.textLowN,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(texto: String, destaque: Boolean) {
    Text(
        text = texto,
        color = if (destaque) OrbitTokens.onBluePastel else OrbitTokens.textMidN,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (destaque) Modifier.background(OrbitTokens.bluePastel)
                else Modifier.background(OrbitTokens.graphiteRaised),
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun DomainChip(fonte: LunaWebFonte) {
    val context = LocalContext.current
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(999.dp))
            .orbitPressable { abrirUrl(context, fonte.url) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        FaviconLetter(domain = fonte.domain, size = 14.dp)
        Text(
            text = fonte.domain.removePrefix("www."),
            color = OrbitTokens.textMidN,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun ResearchStepCard(step: LunaResearchStep) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(OrbitTokens.graphiteSurf.copy(alpha = 0.55f))
            .border(1.dp, OrbitTokens.graphiteHair.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = step.kind.icone(),
                color = OrbitTokens.bluePastel,
                fontSize = 13.sp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = step.label,
                    color = OrbitTokens.textHiN,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!step.detail.isNullOrBlank()) {
                    Text(
                        text = step.detail,
                        color = OrbitTokens.textLowN,
                        fontSize = 11.sp,
                    )
                }
            }
            if (step.status == LunaResearchStepStatus.RUNNING) {
                StatusChip(texto = "Agora", destaque = true)
            } else if (step.status == LunaResearchStepStatus.DONE) {
                Text("✓", color = OrbitTokens.textLowN, fontSize = 12.sp)
            }
        }

        if (step.queries.isNotEmpty()) {
            BlockLabel("Consultas")
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                step.queries.forEach { q ->
                    Text(
                        text = "⌕  $q",
                        color = OrbitTokens.textMidN,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrbitTokens.graphiteBg.copy(alpha = 0.45f))
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                    )
                }
            }
        }

        if (step.sources.isNotEmpty()) {
            BlockLabel("Fontes · ${step.sources.size}")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                step.sources.forEach { fonte ->
                    SourceRow(fonte)
                }
            }
        }

        if (step.citations.isNotEmpty()) {
            BlockLabel("Referências · ${step.citations.size}")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                step.citations.forEach { cite ->
                    CitationRow(cite)
                }
            }
        }
    }
}

@Composable
private fun BlockLabel(texto: String) {
    Text(
        text = texto.uppercase(),
        color = OrbitTokens.textLowN,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun SourceRow(fonte: LunaWebFonte) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .orbitPressable { abrirUrl(context, fonte.url) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        FaviconLetter(domain = fonte.domain, size = 18.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = fonte.title,
                color = OrbitTokens.bluePastel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(fonte.domain.removePrefix("www."))
                    if (!fonte.publishedAt.isNullOrBlank()) append(" · ${fonte.publishedAt}")
                },
                color = OrbitTokens.textLowN,
                fontSize = 10.sp,
            )
            if (!fonte.snippet.isNullOrBlank()) {
                Text(
                    text = fonte.snippet,
                    color = OrbitTokens.textMidN,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = fonte.status.rotulo(),
            color = when (fonte.status) {
                LunaFonteStatus.DESCARTADA -> OrbitTokens.textLowN
                LunaFonteStatus.CONFIRMADA, LunaFonteStatus.CITADA -> OrbitTokens.bluePastel
                LunaFonteStatus.LENDO -> OrbitTokens.bluePastel
                else -> OrbitTokens.textMidN
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(OrbitTokens.graphiteRaised)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun CitationRow(cite: LunaCitacao) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .orbitPressable { abrirUrl(context, cite.url) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "[${cite.index}]",
            color = OrbitTokens.bluePastel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = cite.title,
                color = OrbitTokens.bluePastel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!cite.excerpt.isNullOrBlank()) {
                Text(
                    text = cite.excerpt,
                    color = OrbitTokens.textMidN,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FaviconLetter(domain: String, size: Dp) {
    val letter = domain.removePrefix("www.").firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(OrbitTokens.graphiteRaised)
            .border(1.dp, OrbitTokens.graphiteHair, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = OrbitTokens.textMidN,
            fontSize = (size.value * 0.55f).sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun abrirUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
