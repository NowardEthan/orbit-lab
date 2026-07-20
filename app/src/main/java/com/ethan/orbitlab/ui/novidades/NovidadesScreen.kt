package com.ethan.orbitlab.ui.novidades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import androidx.compose.runtime.getValue

// ---- Modelo do changelog ----

enum class TipoMudanca(val rotulo: String, val cor: Color) {
    NOVO("Novo", OrbitTokens.online),
    MELHORIA("Melhoria", OrbitTokens.accent),
    CORRECAO("Correção", OrbitTokens.gold)
}

data class Mudanca(val tipo: TipoMudanca, val texto: String)

data class VersaoNota(
    val versao: String,
    val data: String,
    val titulo: String,
    val mudancas: List<Mudanca>,
    val atual: Boolean = false
)

// Demo: as próprias atualizações que a gente foi fazendo no Orbit.
private val notasDemo = listOf(
    VersaoNota(
        versao = "0.4.0", data = "19 jul", titulo = "Ajustes chegou", atual = true,
        mudancas = listOf(
            Mudanca(TipoMudanca.NOVO, "Tela de Ajustes: conta, Luna, preferências e privacidade."),
            Mudanca(TipoMudanca.NOVO, "Central de Novidades — é isso que você está vendo agora. 🌙"),
            Mudanca(TipoMudanca.MELHORIA, "Transições entre as abas mais macias.")
        )
    ),
    VersaoNota(
        versao = "0.3.0", data = "18 jul", titulo = "A Luna ganhou voz",
        mudancas = listOf(
            Mudanca(TipoMudanca.NOVO, "Gravador de áudio: segure o microfone, arraste pra cima pra travar."),
            Mudanca(TipoMudanca.MELHORIA, "O botão segue o dedo e vibra nos momentos-chave."),
            Mudanca(TipoMudanca.CORRECAO, "Arraste pra esquerda cancela sem enviar por engano.")
        )
    ),
    VersaoNota(
        versao = "0.2.0", data = "18 jul", titulo = "Conversas & Chat",
        mudancas = listOf(
            Mudanca(TipoMudanca.NOVO, "Lista de conversas com busca e seleção múltipla."),
            Mudanca(TipoMudanca.NOVO, "Chat com a Luna, com balões e histórico.")
        )
    ),
    VersaoNota(
        versao = "0.1.0", data = "17 jul", titulo = "Primeiro respiro",
        mudancas = listOf(
            Mudanca(TipoMudanca.NOVO, "Tela Início com a atmosfera da Luna."),
            Mudanca(TipoMudanca.NOVO, "Perfil com a Luz e a Lua (gamificação)."),
            Mudanca(TipoMudanca.NOVO, "A espinha do app: barra de baixo flutuante.")
        )
    )
)

@Composable
fun NovidadesScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.ink1)
            .background(
                Brush.radialGradient(
                    colors = listOf(OrbitTokens.accent.copy(alpha = 0.3f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(-200f, -200f),
                    radius = 1800f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        NovidadesHeader(onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            notasDemo.forEach { nota ->
                VersaoCard(nota)
            }
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Voltar", tint = OrbitTokens.textHigh, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        val infiniteTransition = rememberInfiniteTransition()
        val pulseStar by infiniteTransition.animateFloat(
            initialValue = 0.5f, targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
        )
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = OrbitTokens.gold, modifier = Modifier.size(20.dp).scale(pulseStar))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Novidades", color = OrbitTokens.textHigh, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("O que mudou no Orbit", color = OrbitTokens.textLow, fontSize = 12.sp)
        }
    }
}

@Composable
private fun VersaoCard(nota: VersaoNota) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(OrbitTokens.surface.copy(alpha = 0.4f))
            .border(1.dp, OrbitTokens.borderSoft.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        // Cabeçalho do card: pílula da versão + (badge "atual") + data
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(OrbitTokens.accentSoft)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("v${nota.versao}", color = OrbitTokens.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (nota.atual) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OrbitTokens.online.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("agora", color = OrbitTokens.online, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(nota.data, color = OrbitTokens.textLow, fontSize = 13.sp)
        }

        Spacer(Modifier.height(14.dp))
        Text(nota.titulo, color = OrbitTokens.textHigh, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            nota.mudancas.forEach { MudancaLinha(it) }
        }
    }
}

@Composable
private fun MudancaLinha(mudanca: Mudanca) {
    Row(verticalAlignment = Alignment.Top) {
        // Etiqueta do tipo (Novo / Melhoria / Correção)
        Box(
            modifier = Modifier
                .width(76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(mudanca.tipo.cor.copy(alpha = 0.15f))
                .border(1.dp, mudanca.tipo.cor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(mudanca.tipo.rotulo, color = mudanca.tipo.cor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            mudanca.texto,
            color = OrbitTokens.textMid,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 900)
@Composable
fun NovidadesScreenPreview() {
    NovidadesScreen(onBack = {})
}
