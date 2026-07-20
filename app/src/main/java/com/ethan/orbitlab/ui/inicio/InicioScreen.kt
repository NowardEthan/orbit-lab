package com.ethan.orbitlab.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.updates.UpdateBanner
import com.ethan.orbitlab.updates.OrbitUpdatesUiState

@Composable
fun InicioScreen(
    onAbrirNovidades: () -> Unit = {},
    temNovidade: Boolean = false,
    updates: OrbitUpdatesUiState = OrbitUpdatesUiState(loading = false),
    onAtualizar: () -> Unit = {},
    onAbrirRotina: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        
        // 1. Efeito de Iluminação (Glowing Orb) no fundo superior esquerdo
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-40).dp, y = (-20).dp)
                .size(240.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(OrbitTokens.violet.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )
        // Glowing Orb de Luz (Gold) no lado direito
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 140.dp)
                .size(180.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(OrbitTokens.gold.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            HeaderSeccao(onAbrirNovidades, temNovidade)

            val showUpdate = updates.updateAvailable && !updates.apkUrl.isNullOrBlank()
            if (showUpdate) {
                UpdateBanner(
                    version = updates.latestVersion,
                    mandatory = updates.mandatory,
                    downloading = updates.downloading,
                    progress = updates.downloadProgress,
                    onUpdate = onAtualizar,
                )
            }

            GamificacaoSeccao()
            DestaqueAcaoSeccao()
            RotinaPreviewSeccao(onAbrirRotina = onAbrirRotina)
        }
    }
}

@Composable
private fun HeaderSeccao(onAbrirNovidades: () -> Unit, temNovidade: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Bom dia,", color = OrbitTokens.textMid, fontSize = 16.sp)
            // Um toque premium: Nome em branco puro, chamando muito a atenção.
            Text("Ethan.", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        }

        // Sino — abre as Novidades. A bolinha dourada só aparece se houver algo não lido.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(OrbitTokens.surfaceRaised, OrbitTokens.surface)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(OrbitTokens.borderSoft, Color.Transparent)),
                    shape = CircleShape
                )
                .clickable { onAbrirNovidades() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = "Novidades", tint = OrbitTokens.textHigh, modifier = Modifier.size(22.dp))
            // Indicador de novidade não lida (bolinha dourada)
            if (temNovidade) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-12).dp, y = 12.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(OrbitTokens.gold)
                        .border(1.5.dp, OrbitTokens.ink1, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun GamificacaoSeccao() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GamificationCard(
            modifier = Modifier.weight(1f),
            title = "A Luz",
            value = "1.240",
            subtitle = "Consistência",
            gradientColors = listOf(OrbitTokens.gold, Color(0xFFD89B1A)),
            contentColor = Color(0xFF332504),
            icon = Icons.Rounded.Star
        )
        GamificationCard(
            modifier = Modifier.weight(1f),
            title = "A Lua",
            value = "850",
            subtitle = "Descoberta",
            gradientColors = listOf(OrbitTokens.violet, Color(0xFF6732DD)),
            contentColor = Color.White,
            icon = Icons.Rounded.Favorite
        )
    }
}

@Composable
private fun GamificationCard(
    modifier: Modifier, 
    title: String, 
    value: String, 
    subtitle: String, 
    gradientColors: List<Color>,
    contentColor: Color, 
    icon: ImageVector
) {
    // Design vibrante e 100% preenchido com gradiente (estilo do botão)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .padding(24.dp)
    ) {
        Icon(icon, contentDescription = null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(value, color = contentColor, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(title, color = contentColor.copy(alpha = 0.95f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = contentColor.copy(alpha = 0.75f), fontSize = 13.sp)
    }
}

@Composable
private fun DestaqueAcaoSeccao() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(colors = listOf(OrbitTokens.surfaceRaised, OrbitTokens.surfaceHover))
            )
            .border(
                1.dp, 
                Brush.linearGradient(listOf(OrbitTokens.borderSoft, Color.Transparent)), 
                RoundedCornerShape(32.dp)
            )
            .padding(28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(OrbitTokens.online))
            Text("Próximo passo sugerido", color = OrbitTokens.textMid, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Trilha Lumen: O que é IA?", color = OrbitTokens.textHigh, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Continue expandindo sua Lua explorando os conceitos base de IA Generativa.", color = OrbitTokens.textLow, fontSize = 14.sp, lineHeight = 20.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Botão de Call to Action com Gradiente Super Saturado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(OrbitTokens.accent, OrbitTokens.violet)))
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retomar Sessão", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RotinaPreviewSeccao(onAbrirRotina: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sua Rotina Hoje", color = OrbitTokens.textHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Abrir rotina",
                color = OrbitTokens.accentText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAbrirRotina),
            )
        }
        Spacer(Modifier.height(16.dp))
        
        // Preview — toque abre a rotina completa
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(OrbitTokens.surface)
                .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(20.dp))
                .clickable(onClick = onAbrirRotina)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF6AA0FF)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Trabalho", color = OrbitTokens.textHigh, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("09:00 - 17:00 · toque para abrir", color = OrbitTokens.textMid, fontSize = 13.sp)
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(OrbitTokens.surfaceRaised),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = OrbitTokens.accentText, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 800)
@Composable
fun InicioScreenPreview() {
    InicioScreen()
}
