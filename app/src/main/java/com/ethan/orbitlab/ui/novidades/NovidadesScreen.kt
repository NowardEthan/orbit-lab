package com.ethan.orbitlab.ui.novidades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.BuildConfig
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.updates.OrbitNewsSection
import com.ethan.orbitlab.data.updates.OrbitNewsTag
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.ui.theme.OrbitFill
import com.ethan.orbitlab.ui.theme.OrbitFills
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable

// ---- Modelo do changelog ----

enum class TipoMudanca(
    val rotulo: String,
    val fill: OrbitFill,
    val icone: ImageVector,
) {
    NOVO("Novo", OrbitFills.online, Icons.Rounded.AutoAwesome),
    MELHORIA("Melhoria", OrbitFills.accent, Icons.Rounded.Build),
    CORRECAO("Correção", OrbitFills.luz, Icons.Rounded.BugReport),
}

/** Uma novidade dentro de uma versão: seu tipo, título e o corpo já quebrado em parágrafos. */
data class NotaItem(
    val tipo: TipoMudanca,
    val titulo: String,
    val paragrafos: List<String>,
)

data class VersaoNota(
    val versao: String,
    val data: String,
    val accent: OrbitFill,
    val atual: Boolean = false,
    val itens: List<NotaItem>,
)

/** Quebra o corpo (ensaio com \n\n entre parágrafos) numa lista limpa, sem vazios. */
private fun paragrafosDe(corpo: String): List<String> =
    corpo.split("\n\n", "\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

private val notasDemo = listOf(
    VersaoNota(
        versao = "0.1.0",
        data = "17 jul",
        accent = OrbitFills.accent,
        atual = true,
        itens = listOf(
            NotaItem(
                TipoMudanca.NOVO,
                "Primeiro respiro",
                listOf("O laboratório abre as portas — Início, Perfil e a barra flutuante."),
            ),
        ),
    ),
)

private fun sectionsToNotas(sections: List<OrbitNewsSection>): List<VersaoNota> {
    val currentVersion = BuildConfig.VERSION_NAME
    val accents = listOf(OrbitFills.accent, OrbitFills.lua, OrbitFills.online, OrbitFills.luz)
    return sections.mapIndexed { index, section ->
        VersaoNota(
            versao = section.version ?: "?",
            data = formatNewsDate(section.date),
            accent = accents[index % accents.size],
            atual = section.version == currentVersion,
            itens = section.items.map { item ->
                NotaItem(
                    tipo = tagToTipo(item.tag),
                    titulo = item.title,
                    paragrafos = paragrafosDe(item.body.ifBlank { item.title }),
                )
            },
        )
    }
}

private fun tagToTipo(tag: OrbitNewsTag): TipoMudanca = when (tag) {
    OrbitNewsTag.CORRECAO -> TipoMudanca.CORRECAO
    OrbitNewsTag.AVISO -> TipoMudanca.MELHORIA
    OrbitNewsTag.NOVIDADE -> TipoMudanca.NOVO
}

private fun formatNewsDate(raw: String): String {
    if (raw.isBlank()) return raw
    val parts = raw.split("-")
    if (parts.size != 3) return raw
    val month = when (parts[1]) {
        "01" -> "jan"
        "02" -> "fev"
        "03" -> "mar"
        "04" -> "abr"
        "05" -> "mai"
        "06" -> "jun"
        "07" -> "jul"
        "08" -> "ago"
        "09" -> "set"
        "10" -> "out"
        "11" -> "nov"
        "12" -> "dez"
        else -> parts[1]
    }
    return "${parts[2].toIntOrNull() ?: parts[2]} $month"
}

// ---- Abas internas ----

private enum class AbaNovidades(val rotulo: String) {
    ATUALIZACOES("Atualizações"),
    NOTIFICACOES("Notificações"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovidadesScreen(onBack: () -> Unit) {
    val manifest by UpdatesRepository.manifest.collectAsState()
    val carregando by UpdatesRepository.loading.collectAsState()
    val notas = remember(manifest) {
        val remote = UpdatesRepository.newsSections()
        if (remote.isNotEmpty()) sectionsToNotas(remote) else notasDemo
    }

    var aba by remember { mutableStateOf(AbaNovidades.ATUALIZACOES) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.ink1),
    ) {
        AtmosferaNovidades()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            NovidadesHeader(onBack)

            SeletorAbas(
                ativa = aba,
                onSelect = { aba = it },
                modifier = Modifier
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 14.dp, bottom = 6.dp),
            )

            when (aba) {
                AbaNovidades.ATUALIZACOES -> ListaAtualizacoes(
                    notas = notas,
                    carregando = carregando,
                    onRefresh = { UpdatesRepository.refresh(force = true) },
                )
                AbaNovidades.NOTIFICACOES -> NotificacoesVazio()
            }
        }
    }
}

@Composable
private fun SeletorAbas(
    ativa: AbaNovidades,
    onSelect: (AbaNovidades) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusChip + 4.dp))
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(OrbitMetrics.radiusChip + 4.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AbaNovidades.entries.forEach { item ->
            val selecionada = item == ativa
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(OrbitMetrics.radiusChip))
                    .background(if (selecionada) OrbitTokens.accent else Color.Transparent)
                    .orbitPressable { onSelect(item) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    item.rotulo,
                    color = if (selecionada) Color.White else OrbitTokens.textMid,
                    fontSize = 13.sp,
                    fontWeight = if (selecionada) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListaAtualizacoes(
    notas: List<VersaoNota>,
    carregando: Boolean,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = carregando,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = OrbitMetrics.pagePadding,
                    end = OrbitMetrics.pagePadding,
                    top = 8.dp,
                    bottom = 40.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(OrbitMetrics.itemGap),
        ) {
            notas.forEachIndexed { index, nota ->
                Box(Modifier.orbitEnter((index * 12).coerceAtMost(48))) {
                    VersaoCard(nota)
                }
            }
        }
    }
}

@Composable
private fun AtmosferaNovidades() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-24).dp)
                .size(180.dp)
                .background(
                    Brush.radialGradient(
                        listOf(OrbitTokens.accent.copy(alpha = 0.08f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-48).dp, y = 100.dp)
                .size(140.dp)
                .background(
                    Brush.radialGradient(
                        listOf(OrbitTokens.violet.copy(alpha = 0.05f), Color.Transparent),
                    ),
                ),
        )
    }
}

@Composable
private fun NovidadesHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(OrbitMetrics.iconBtn)
                .clip(CircleShape)
                .background(OrbitTokens.surface)
                .border(1.dp, OrbitTokens.borderSoft, CircleShape)
                .orbitPressable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ArrowBackIosNew,
                contentDescription = "Voltar",
                tint = OrbitTokens.textHigh,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = painterResource(R.drawable.ic_orbit_symbol),
            contentDescription = "Orbit",
            tint = Color.Unspecified,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Novidades",
                color = OrbitTokens.textHigh,
                fontSize = OrbitMetrics.titleSize,
                fontWeight = OrbitMetrics.titleWeight,
                letterSpacing = (-0.3).sp,
            )
            Text(
                "O que mudou e o que a Luna te avisa",
                color = OrbitTokens.textMid,
                fontSize = OrbitMetrics.captionSize,
            )
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OrbitTokens.borderSoft.copy(alpha = 0.65f)),
    )
}

/**
 * O card de uma versão. Recolhível: a atual já nasce aberta; as antigas ficam fechadas,
 * mostrando só o cabeçalho e os títulos, pra rolar sem virar um paredão. Um toque abre.
 */
@Composable
private fun VersaoCard(nota: VersaoNota) {
    var aberto by remember(nota.versao) { mutableStateOf(nota.atual) }
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.surface)
            .border(
                1.dp,
                if (nota.atual) OrbitTokens.accent.copy(alpha = 0.55f) else OrbitTokens.borderSoft,
                shape,
            )
            .orbitPressable { aberto = !aberto }
            .padding(16.dp),
    ) {
        // Cabeçalho: versão, data, "Agora" e a setinha de recolher.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(nota.accent.start),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                "v${nota.versao}",
                color = OrbitTokens.textHigh,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
            )
            Spacer(Modifier.width(8.dp))
            Text("·", color = OrbitTokens.textLow, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                nota.data,
                color = OrbitTokens.textLow,
                fontSize = OrbitMetrics.captionSize,
            )
            Spacer(Modifier.weight(1f))
            if (nota.atual) {
                ChipAgora()
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (aberto) "Recolher" else "Abrir",
                tint = OrbitTokens.textLow,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (aberto) 180f else 0f),
            )
        }

        if (!aberto) {
            // Recolhido: só os títulos, um por linha, pra dar pra bater o olho.
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                nota.itens.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(item.tipo.fill.start),
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            item.titulo,
                            color = OrbitTokens.textMid,
                            fontSize = OrbitMetrics.bodySize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = aberto) {
            Column {
                Spacer(Modifier.height(6.dp))
                nota.itens.forEachIndexed { index, item ->
                    if (index > 0) {
                        Spacer(Modifier.height(16.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(OrbitTokens.borderSoft.copy(alpha = 0.5f)),
                        )
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Spacer(Modifier.height(14.dp))
                    }
                    NotaItemView(item)
                }
            }
        }
    }
}

@Composable
private fun NotaItemView(item: NotaItem) {
    Column {
        TagPill(item.tipo)
        Spacer(Modifier.height(8.dp))
        Text(
            item.titulo,
            color = OrbitTokens.textHigh,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.15).sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item.paragrafos.forEach { paragrafo ->
                Text(
                    paragrafo,
                    color = OrbitTokens.textMid,
                    fontSize = OrbitMetrics.bodySize,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

@Composable
private fun TagPill(tipo: TipoMudanca) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusChip))
            .background(tipo.fill.brush)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            tipo.icone,
            contentDescription = null,
            tint = tipo.fill.onFill,
            modifier = Modifier.size(13.dp),
        )
        Text(
            tipo.rotulo,
            color = tipo.fill.onFill,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun ChipAgora() {
    Text(
        "Agora",
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusChip))
            .background(OrbitTokens.accent)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** A aba de Notificações ainda não tem conteúdo — mas o berço fica pronto e explicado. */
@Composable
private fun NotificacoesVazio() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(OrbitTokens.surface)
                .border(1.dp, OrbitTokens.borderSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.NotificationsNone,
                contentDescription = null,
                tint = OrbitTokens.textMid,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Sem notificações por enquanto",
            color = OrbitTokens.textHigh,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Quando a Luna tiver algo pra te avisar — um lembrete que você pediu, uma novidade sua — vai aparecer aqui.",
            color = OrbitTokens.textMid,
            fontSize = OrbitMetrics.bodySize,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 980)
@Composable
fun NovidadesScreenPreview() {
    NovidadesScreen(onBack = {})
}
