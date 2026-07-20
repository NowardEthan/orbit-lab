package com.ethan.orbitlab.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
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
            .background(OrbitTokens.ink1)
            .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(OrbitMetrics.radiusCard))
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
                tint = if (done) OrbitTokens.textMid else OrbitTokens.accentText,
                modifier = Modifier.size(16.dp),
            )
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "PESQUISA PROFUNDA",
                        color = OrbitTokens.textLow,
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
                    color = OrbitTokens.textHigh,
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
                tint = OrbitTokens.textLow,
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
            color = OrbitTokens.accent,
            trackColor = OrbitTokens.surfaceHover,
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
            color = OrbitTokens.textLow,
            fontSize = 11.sp,
        )

        if (!liveLabel.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = liveLabel,
                color = OrbitTokens.accentText,
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

@Composable
private fun StatusChip(texto: String, destaque: Boolean) {
    Text(
        text = texto,
        color = if (destaque) OrbitTokens.accentText else OrbitTokens.textMid,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (destaque) OrbitTokens.accentSoft
                else OrbitTokens.surfaceHover,
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
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(999.dp))
            .orbitPressable { abrirUrl(context, fonte.url) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        FaviconLetter(domain = fonte.domain, size = 14.dp)
        Text(
            text = fonte.domain.removePrefix("www."),
            color = OrbitTokens.textMid,
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
            .background(OrbitTokens.surface.copy(alpha = 0.55f))
            .border(1.dp, OrbitTokens.borderSoft.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = step.kind.icone(),
                color = OrbitTokens.accentText,
                fontSize = 13.sp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = step.label,
                    color = OrbitTokens.textHigh,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!step.detail.isNullOrBlank()) {
                    Text(
                        text = step.detail,
                        color = OrbitTokens.textLow,
                        fontSize = 11.sp,
                    )
                }
            }
            if (step.status == LunaResearchStepStatus.RUNNING) {
                StatusChip(texto = "Agora", destaque = true)
            } else if (step.status == LunaResearchStepStatus.DONE) {
                Text("✓", color = OrbitTokens.textLow, fontSize = 12.sp)
            }
        }

        if (step.queries.isNotEmpty()) {
            BlockLabel("Consultas")
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                step.queries.forEach { q ->
                    Text(
                        text = "⌕  $q",
                        color = OrbitTokens.textMid,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrbitTokens.ink0.copy(alpha = 0.45f))
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
        color = OrbitTokens.textLow,
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
                color = OrbitTokens.accentText,
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
                color = OrbitTokens.textLow,
                fontSize = 10.sp,
            )
            if (!fonte.snippet.isNullOrBlank()) {
                Text(
                    text = fonte.snippet,
                    color = OrbitTokens.textMid,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = fonte.status.rotulo(),
            color = when (fonte.status) {
                LunaFonteStatus.DESCARTADA -> OrbitTokens.textLow
                LunaFonteStatus.CONFIRMADA, LunaFonteStatus.CITADA -> OrbitTokens.accentText
                LunaFonteStatus.LENDO -> OrbitTokens.accentText
                else -> OrbitTokens.textMid
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(OrbitTokens.surfaceHover)
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
            color = OrbitTokens.accentText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = cite.title,
                color = OrbitTokens.accentText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!cite.excerpt.isNullOrBlank()) {
                Text(
                    text = cite.excerpt,
                    color = OrbitTokens.textMid,
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
            .background(OrbitTokens.surfaceHover)
            .border(1.dp, OrbitTokens.borderSoft, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = OrbitTokens.textMid,
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
