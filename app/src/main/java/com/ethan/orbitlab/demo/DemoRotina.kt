package com.ethan.orbitlab.demo

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens

/**
 * A primeira tela demo — a Rotina do Orbit recriada em Compose, com dados FAKE.
 *
 * É o teu jeito de trabalhar: idealizar com dados demo, mexer no visual, e só depois plugar o real.
 * Aqui não tem backend nenhum — é 100% "Storybook". Abre este arquivo no Android Studio e olha o
 * painel de @Preview à direita (Split/Design): ele renderiza isto AO VIVO, sem rodar o app.
 */

// ── Dados demo (o "fixture") ──────────────────────────────────────────────────
private data class Bloco(
    val ini: String,
    val fim: String,
    val titulo: String,
    val cor: Color,
    val feitas: Int = 0,
    val total: Int = 0,
    val agora: Boolean = false,
    val feito: Boolean = false,
)

private val blocosDemo = listOf(
    Bloco("07:00", "07:30", "Acordar", Color(0xFFE6B84A), feito = true),
    Bloco("09:00", "17:00", "Trabalho", Color(0xFF6AA0FF), feitas = 2, total = 6, agora = true),
    Bloco("12:00", "13:00", "Almoço", Color(0xFFE08A5B), total = 3),
    Bloco("18:30", "19:30", "Treino", Color(0xFF57C77E)),
)

// ── Um card de bloco ──────────────────────────────────────────────────────────
// Repara como o Compose é declarativo (igual React): você DESCREVE a UI, não manda comandos.
// `Modifier` é a corrente que empilha estilo/layout (padding, cor, borda, tamanho…).
@Composable
private fun BlocoCard(b: Bloco) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (b.agora) OrbitTokens.surfaceHover else OrbitTokens.surface)
            .border(
                width = if (b.agora) 1.5.dp else 1.dp,
                color = OrbitTokens.borderSoft,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(end = 12.dp),
    ) {
        // A barra lateral colorida — o "peso" do bloco
        Box(
            Modifier
                .width(4.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(b.cor),
        )
        Spacer(Modifier.width(10.dp))

        // Horário
        Column(Modifier.width(52.dp).padding(start = 8.dp)) {
            Text(b.ini, color = OrbitTokens.textHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(b.fim, color = OrbitTokens.textLow, fontSize = 11.sp)
        }

        // Título + meta (weight(1f) = "toma o espaço que sobra", como flex: 1)
        Column(Modifier.weight(1f)) {
            Text(b.titulo, color = OrbitTokens.textHigh, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (b.agora) Text("● agora", color = OrbitTokens.online, fontSize = 11.sp)
                if (b.total > 0) Text("${b.feitas}/${b.total} ✓", color = OrbitTokens.textMid, fontSize = 11.sp)
            }
        }

        // Círculo de dar baixa
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (b.feito) OrbitTokens.gold else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (b.feito) OrbitTokens.gold else OrbitTokens.border,
                    shape = RoundedCornerShape(11.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (b.feito) Text("✓", color = Color(0xFF1A1408), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014)
@Composable
fun BlocoCardPreview() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BlocoCard(blocosDemo[0]) // Feito
        BlocoCard(blocosDemo[1]) // Agora
        BlocoCard(blocosDemo[2]) // Pendente
    }
}

// ── A tela ────────────────────────────────────────────────────────────────────
@Composable
fun DemoRotina() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.ink1)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Rotina", color = OrbitTokens.textHigh, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        blocosDemo.forEach { BlocoCard(it) }
    }
}

// ── @Preview = o teu Storybook nativo ─────────────────────────────────────────
// Renderiza no Android Studio, ao vivo, sem device e sem rodar o app. Mexeu no código → mudou aqui.
@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 360, heightDp = 640)
@Composable
fun DemoRotinaPreview() {
    DemoRotina()
}
