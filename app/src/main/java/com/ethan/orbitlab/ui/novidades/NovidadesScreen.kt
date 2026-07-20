package com.ethan.orbitlab.ui.novidades

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.updates.OrbitNewsItem
import com.ethan.orbitlab.updates.OrbitNewsSection
import com.ethan.orbitlab.updates.OrbitNewsTag
import com.ethan.orbitlab.updates.OrbitUpdatesUiState
import com.ethan.orbitlab.updates.groupNewsByVersion

private data class TagUi(val rotulo: String, val cor: Color)

private fun OrbitNewsTag.toUi(): TagUi = when (this) {
    OrbitNewsTag.NOVIDADE -> TagUi("Novo", OrbitTokens.online)
    OrbitNewsTag.CORRECAO -> TagUi("Correção", OrbitTokens.gold)
    OrbitNewsTag.AVISO -> TagUi("Aviso", OrbitTokens.accent)
}

@Composable
fun NovidadesScreen(
    onBack: () -> Unit,
    updates: OrbitUpdatesUiState = OrbitUpdatesUiState(loading = false),
    onRefresh: () -> Unit = {},
) {
    val sections = groupNewsByVersion(updates.news)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.ink1)
            .background(
                Brush.radialGradient(
                    colors = listOf(OrbitTokens.accent.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(-200f, -200f),
                    radius = 1800f,
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        NovidadesHeader(onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                updates.loading && sections.isEmpty() -> {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = OrbitTokens.accent, strokeWidth = 2.dp)
                    }
                }
                updates.error && sections.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(OrbitTokens.surface.copy(alpha = 0.5f))
                            .clickable { onRefresh() }
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Não deu pra carregar as novidades",
                            color = OrbitTokens.textHigh,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Toque para tentar de novo",
                            color = OrbitTokens.accentText,
                            fontSize = 13.sp,
                        )
                    }
                }
                sections.isEmpty() -> {
                    Text(
                        "Quando sair uma versão nova, o mural aparece aqui.",
                        color = OrbitTokens.textMid,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
                else -> {
                    sections.forEach { section ->
                        VersaoCard(
                            section = section,
                            instalada = section.version == updates.currentVersion
                                || (section.version == null && !updates.updateAvailable),
                        )
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
private fun NovidadesHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ArrowBackIosNew,
                contentDescription = "Voltar",
                tint = OrbitTokens.textHigh,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        val infiniteTransition = rememberInfiniteTransition(label = "estrela")
        val pulseStar by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "estrelaPulse",
        )
        Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = OrbitTokens.gold,
            modifier = Modifier.size(20.dp).scale(pulseStar),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Novidades", color = OrbitTokens.textHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("O que mudou no Orbit", color = OrbitTokens.textLow, fontSize = 12.sp)
        }
    }
}

@Composable
private fun VersaoCard(section: OrbitNewsSection, instalada: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(OrbitTokens.surface.copy(alpha = 0.4f))
            .border(1.dp, OrbitTokens.borderSoft.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OrbitTokens.accentSoft)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = section.version?.let { "v$it" } ?: "Outras",
                    color = OrbitTokens.accentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (instalada) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OrbitTokens.online.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("instalada", color = OrbitTokens.online, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.weight(1f))
            if (section.date.isNotBlank()) {
                Text(section.date, color = OrbitTokens.textLow, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(14.dp))
        val titulo = section.items.firstOrNull()?.title ?: "Atualização"
        Text(titulo, color = OrbitTokens.textHigh, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            section.items.forEach { item -> MudancaLinha(item) }
        }
    }
}

@Composable
private fun MudancaLinha(item: OrbitNewsItem) {
    val tag = item.tag.toUi()
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .width(76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tag.cor.copy(alpha = 0.15f))
                .border(1.dp, tag.cor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(tag.rotulo, color = tag.cor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (item.body.isNotBlank() && item.body != item.title) {
                Text(item.title, color = OrbitTokens.textHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(item.body, color = OrbitTokens.textMid, fontSize = 14.sp, lineHeight = 20.sp)
            } else {
                Text(item.title, color = OrbitTokens.textMid, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 900)
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
