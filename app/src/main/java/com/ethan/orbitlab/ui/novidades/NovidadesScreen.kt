package com.ethan.orbitlab.ui.novidades

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.ethan.orbitlab.BuildConfig
import com.ethan.orbitlab.data.updates.OrbitNewsSection
import com.ethan.orbitlab.data.updates.OrbitNewsTag
import com.ethan.orbitlab.data.updates.UpdatesRepository
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.R
import com.ethan.orbitlab.ui.theme.OrbitFill
import com.ethan.orbitlab.ui.theme.OrbitFills
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable

private data class TagUi(val rotulo: String, val cor: Color)

enum class TipoMudanca(
    val rotulo: String,
    val fill: OrbitFill,
    val icone: ImageVector,
) {
    NOVO("Novo", OrbitFills.online, Icons.Rounded.AutoAwesome),
    MELHORIA("Melhoria", OrbitFills.accent, Icons.Rounded.Build),
    CORRECAO("Correção", OrbitFills.luz, Icons.Rounded.BugReport),
}

data class Mudanca(val tipo: TipoMudanca, val texto: String)

data class VersaoNota(
    val versao: String,
    val data: String,
    val titulo: String,
    val resumo: String,
    val mudancas: List<Mudanca>,
    val accent: OrbitFill,
    val atual: Boolean = false,
)

private val notasDemo = listOf(
    VersaoNota(
        versao = "0.5.0",
        data = "19 jul",
        titulo = "Visual Orbit unificado",
        resumo = "Início, Conversas, Perfil, Ajustes e Chat no mesmo idioma visual — logo, cards e assinatura Aura Blue.",
        accent = OrbitFills.accent,
        atual = true,
        mudancas = listOf(
            Mudanca(TipoMudanca.MELHORIA, "Todas as abas alinhadas ao padrão dark com acentos pontuais."),
            Mudanca(TipoMudanca.MELHORIA, "Chat mais fluido: stream, markdown e scroll ao enviar."),
            Mudanca(TipoMudanca.NOVO, "Central de Novidades no mesmo visual do restante do lab."),
        ),
    ),
    VersaoNota(
        versao = "0.4.0",
        data = "19 jul",
        titulo = "Ajustes chegou",
        resumo = "Controle da conta e um lugar pra ver o que muda no OrbitLab.",
        accent = OrbitFills.lua,
        mudancas = listOf(
            Mudanca(TipoMudanca.NOVO, "Tela de Ajustes: conta, Luna, preferências e privacidade."),
            Mudanca(TipoMudanca.NOVO, "Central de Novidades — histórico de versões do OrbitLab."),
        ),
    ),
    VersaoNota(
        versao = "0.3.0",
        data = "18 jul",
        titulo = "A Luna ganhou voz",
        resumo = "Falar com o dedo — gravar, travar e cancelar sem sair do chat.",
        accent = OrbitFills.lua,
        mudancas = listOf(
            Mudanca(TipoMudanca.NOVO, "Gravador de áudio: segure o microfone, arraste pra cima pra travar."),
            Mudanca(TipoMudanca.MELHORIA, "O botão segue o dedo e vibra nos momentos-chave."),
            Mudanca(TipoMudanca.CORRECAO, "Arraste pra esquerda cancela sem enviar por engano."),
        ),
    ),
    VersaoNota(
        versao = "0.2.0",
        data = "18 jul",
        titulo = "Conversas & Chat",
        resumo = "A espinha das conversas: lista, busca e o primeiro chat com a Luna.",
        accent = OrbitFills.online,
        mudancas = listOf(
            Mudanca(TipoMudanca.NOVO, "Lista de conversas com busca e seleção múltipla."),
            Mudanca(TipoMudanca.NOVO, "Chat com a Luna, com balões e histórico."),
        ),
    ),
    VersaoNota(
        versao = "0.1.0",
        data = "17 jul",
        titulo = "Primeiro respiro",
        resumo = "O laboratório abre as portas — Início, Perfil e a barra flutuante.",
        accent = OrbitFills.luz,
        mudancas = listOf(
            Mudanca(TipoMudanca.NOVO, "Tela Início com a atmosfera da Luna."),
            Mudanca(TipoMudanca.NOVO, "Perfil com a Luz e a Lua (gamificação)."),
            Mudanca(TipoMudanca.NOVO, "A espinha do app: barra de baixo flutuante."),
        ),
    ),
)

private fun sectionsToNotas(sections: List<OrbitNewsSection>): List<VersaoNota> {
    val currentVersion = BuildConfig.VERSION_NAME
    val accents = listOf(OrbitFills.accent, OrbitFills.lua, OrbitFills.online, OrbitFills.luz)
    return sections.mapIndexed { index, section ->
        val first = section.items.firstOrNull()
        VersaoNota(
            versao = section.version ?: "?",
            data = formatNewsDate(section.date),
            titulo = first?.title ?: "Novidades",
            resumo = first?.body.orEmpty(),
            accent = accents[index % accents.size],
            atual = section.version == currentVersion,
            mudancas = section.items.map { item ->
                Mudanca(
                    tipo = tagToTipo(item.tag),
                    texto = item.body.ifBlank { item.title },
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

@Composable
fun NovidadesScreen(onBack: () -> Unit) {
    val manifest by UpdatesRepository.manifest.collectAsState()
    val sections = remember(manifest) {
        val remote = UpdatesRepository.newsSections()
        if (remote.isNotEmpty()) sectionsToNotas(remote) else notasDemo
    }
    val notas = sections.ifEmpty { notasDemo }
    val atual = notas.firstOrNull { it.atual } ?: notas.first()
    val arquivo = notas.filterNot { it.atual }

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
                verticalArrangement = Arrangement.spacedBy(OrbitMetrics.sectionGap),
            ) {
                Box(Modifier.orbitEnter(0)) {
                    CardVersaoAtual(atual)
                }

                if (arquivo.isNotEmpty()) {
                    Box(Modifier.orbitEnter(32)) {
                        SecaoAnteriores(arquivo)
                    }
                }
            }

            Text(
                "Orbit v${updates.currentVersion}",
                color = OrbitTokens.textLow,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
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
                "O que mudou no OrbitLab",
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

@Composable
private fun CardVersaoAtual(nota: VersaoNota) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, shape),
    ) {
        AssinaturaAzul(
            Modifier
                .align(Alignment.BottomEnd)
                .size(16.dp),
        )

        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(OrbitTokens.accent),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "v${nota.versao}",
                            color = OrbitTokens.textHigh,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                        )
                        ChipAgora()
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        nota.data,
                        color = OrbitTokens.textLow,
                        fontSize = OrbitMetrics.captionSize,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                nota.titulo,
                color = OrbitTokens.textHigh,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                nota.resumo,
                color = OrbitTokens.textMid,
                fontSize = OrbitMetrics.bodySize,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                nota.mudancas.forEach { MudancaLinha(it) }
            }
        }
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

@Composable
private fun MudancaLinha(mudanca: Mudanca) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(OrbitMetrics.radiusIcon))
                .background(mudanca.tipo.fill.brush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                mudanca.tipo.icone,
                contentDescription = null,
                tint = mudanca.tipo.fill.onFill,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                mudanca.tipo.rotulo,
                color = OrbitTokens.textHigh,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                mudanca.texto,
                color = OrbitTokens.textMid,
                fontSize = OrbitMetrics.bodySize,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun SecaoAnteriores(notas: List<VersaoNota>) {
    Column {
        Text(
            "ANTERIORES",
            color = OrbitTokens.textLow,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.7.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(OrbitMetrics.itemGap)) {
            notas.forEachIndexed { index, nota ->
                Box(Modifier.orbitEnter((index * 12).coerceAtMost(48))) {
                    CardVersaoArquivo(nota)
                }
            }
        }
    }
}

@Composable
private fun CardVersaoArquivo(nota: VersaoNota) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, shape)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(nota.accent.start),
            )
            Text(
                "v${nota.versao}",
                color = OrbitTokens.textHigh,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("·", color = OrbitTokens.textLow, fontSize = 13.sp)
            Text(
                nota.data,
                color = OrbitTokens.textLow,
                fontSize = OrbitMetrics.captionSize,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            nota.titulo,
            color = OrbitTokens.textHigh,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.15).sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            nota.resumo,
            color = OrbitTokens.textMid,
            fontSize = OrbitMetrics.bodySize,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            nota.mudancas.forEach { mudanca ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        mudanca.tipo.rotulo,
                        color = mudanca.tipo.fill.start,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(72.dp),
                    )
                    Text(
                        mudanca.texto,
                        color = OrbitTokens.textMid,
                        fontSize = OrbitMetrics.bodySize,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssinaturaAzul(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width, size.height)
            lineTo(size.width, size.height * 0.15f)
            lineTo(size.width * 0.15f, size.height)
            close()
        }
        drawPath(path, color = OrbitTokens.accent)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 980)
@Composable
fun NovidadesScreenPreview() {
    NovidadesScreen(
        onBack = {},
        updates = OrbitUpdatesUiState(
            loading = false,
            currentVersion = "2.25.0",
            news = listOf(
                OrbitNewsItem(
                    id = "demo-1",
                    date = "2026-07-20",
                    tag = OrbitNewsTag.NOVIDADE,
                    title = "Auto-update nativo",
                    body = "O Orbit Compose já lê o mesmo manifesto do canal de produção.",
                    version = "2.25.0",
                ),
                OrbitNewsItem(
                    id = "demo-2",
                    date = "2026-07-16",
                    tag = OrbitNewsTag.CORRECAO,
                    title = "Ajustes de estabilidade",
                    body = "Correções do capítulo anterior.",
                    version = "2.24.4",
                ),
            ),
        ),
    )
}
