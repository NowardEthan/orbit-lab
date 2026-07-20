package com.ethan.orbitlab.ui.ajustes

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.updates.OrbitUpdatesUiState

@Composable
fun AjustesScreen(
    updates: OrbitUpdatesUiState = OrbitUpdatesUiState(loading = false),
) {
    // Interruptores com estado de verdade (por enquanto só locais, sem persistir).
    var notificacoes by remember { mutableStateOf(true) }
    var vibracao by remember { mutableStateOf(true) }
    var modoEscuro by remember { mutableStateOf(true) }

    val canalLabel = when (updates.canal) {
        "beta" -> "Canal de testes (Orbit β)"
        "stable" -> "Canal estável"
        else -> "Canal lab (dev)"
    }
    val versaoLabel = "v${updates.currentVersion} (${updates.currentVersionCode})"

    // Fundo com brilho no topo-direita
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.ink1)
            .background(
                Brush.radialGradient(
                    colors = listOf(OrbitTokens.violet.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(800f, -100f),
                    radius = 1600f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
        // Título
        Text(
            "Ajustes",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        // Card de conta (topo)
        ContaCard()

        // Grupo: Luna (violeta = a lua)
        SecaoAjustes("Luna") {
            LinhaAjuste(Icons.Rounded.AutoAwesome, OrbitTokens.violet, "Personalidade", "Como a Luna fala com você")
            Divisoria()
            LinhaAjuste(Icons.Rounded.RecordVoiceOver, OrbitTokens.violet, "Voz", "A voz dela nos áudios")
            Divisoria()
            LinhaAjuste(Icons.Rounded.Psychology, OrbitTokens.violet, "Memória", "O que ela lembra de você")
        }

        // Grupo: Preferências (azul = accent)
        SecaoAjustes("Preferências") {
            LinhaSwitch(Icons.Rounded.Notifications, OrbitTokens.accent, "Notificações", notificacoes) { notificacoes = it }
            Divisoria()
            LinhaSwitch(Icons.Rounded.Vibration, OrbitTokens.accent, "Vibração", vibracao) { vibracao = it }
            Divisoria()
            LinhaSwitch(Icons.Rounded.DarkMode, OrbitTokens.accent, "Modo escuro", modoEscuro) { modoEscuro = it }
            Divisoria()
            LinhaAjuste(Icons.Rounded.Language, OrbitTokens.accent, "Idioma", trailing = "Português")
        }

        // Grupo: Atualizações (mesmo canal do orbit-releases)
        SecaoAjustes("Atualizações") {
            LinhaAjuste(
                Icons.Rounded.Info,
                if (updates.canalBeta) OrbitTokens.gold else OrbitTokens.accent,
                canalLabel,
                subtitulo = if (updates.updateAvailable && updates.latestVersion != null) {
                    "Nova: v${updates.latestVersion}"
                } else {
                    "Versão instalada $versaoLabel"
                },
                clicavel = false,
            )
        }

        // Grupo: Privacidade
        SecaoAjustes("Privacidade") {
            LinhaAjuste(Icons.Rounded.Description, OrbitTokens.textMid, "Política de Privacidade")
            Divisoria()
            LinhaAjuste(Icons.Rounded.Gavel, OrbitTokens.textMid, "Termos de Uso")
            Divisoria()
            LinhaAjuste(Icons.Rounded.DeleteForever, OrbitTokens.danger, "Apagar conta", danger = true)
        }

        // Grupo: Sobre (dourado = a luz)
        SecaoAjustes("Sobre") {
            LinhaAjuste(Icons.Rounded.Info, OrbitTokens.gold, "Versão", trailing = versaoLabel, clicavel = false)
            Divisoria()
            LinhaAjuste(Icons.Rounded.Star, OrbitTokens.gold, "Avaliar o app")
        }

        // Sair
        BotaoSair()

        // Rodapé
        Text(
            "Feito com 🌙 · OrbitLab",
            color = OrbitTokens.textLow,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
    }
}

/** Card da conta no topo — avatar com anel dourado/violeta + nome + chevron. */
@Composable
private fun ContaCard() {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(OrbitTokens.surfaceRaised.copy(alpha = 0.8f), OrbitTokens.surface.copy(alpha = 0.5f)))
            )
            .border(1.dp, OrbitTokens.violet.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable { }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "anel")
        val pulseRing by infiniteTransition.animateFloat(
            initialValue = 0.1f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
            label = "anelPulse",
        )
        // Avatar com anel de gamificação animado
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(OrbitTokens.surfaceRaised)
                .border(3.dp, Brush.linearGradient(listOf(OrbitTokens.gold.copy(alpha = pulseRing), OrbitTokens.violet.copy(alpha = pulseRing))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = OrbitTokens.textMid, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Ethan", color = OrbitTokens.textHigh, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("Ver e editar perfil", color = OrbitTokens.textMid, fontSize = 13.sp)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OrbitTokens.textLow, modifier = Modifier.size(22.dp))
    }
}

/** Um grupo: rótulo pequeno + um card arredondado envolvendo as linhas. */
@Composable
private fun SecaoAjustes(titulo: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Text(
            titulo.uppercase(),
            color = OrbitTokens.textLow,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(OrbitTokens.surface.copy(alpha = 0.4f))
                .border(1.dp, OrbitTokens.borderSoft.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
        ) {
            content()
        }
    }
}

/** Linha padrão: ícone colorido + título (+subtítulo) + trailing (texto/chevron). */
@Composable
private fun LinhaAjuste(
    icone: ImageVector,
    cor: Color,
    titulo: String,
    subtitulo: String? = null,
    trailing: String? = null,
    danger: Boolean = false,
    clicavel: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clicavel) Modifier.clickable { } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconeQuadrado(icone, cor)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                color = if (danger) OrbitTokens.danger else OrbitTokens.textHigh,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitulo != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitulo, color = OrbitTokens.textLow, fontSize = 12.sp)
            }
        }
        if (trailing != null) {
            Text(trailing, color = OrbitTokens.textMid, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
        }
        if (clicavel && !danger) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = OrbitTokens.textLow, modifier = Modifier.size(20.dp))
        }
    }
}

/** Linha com interruptor à direita. */
@Composable
private fun LinhaSwitch(
    icone: ImageVector,
    cor: Color,
    titulo: String,
    checado: Boolean,
    onCheck: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconeQuadrado(icone, cor)
        Spacer(Modifier.width(14.dp))
        Text(titulo, color = OrbitTokens.textHigh, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Switch(
            checked = checado,
            onCheckedChange = onCheck,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OrbitTokens.accent,
                checkedBorderColor = OrbitTokens.accent,
                uncheckedThumbColor = OrbitTokens.textLow,
                uncheckedTrackColor = OrbitTokens.surfaceHover,
                uncheckedBorderColor = OrbitTokens.border
            )
        )
    }
}

/** O quadradinho colorido de fundo do ícone. */
@Composable
private fun IconeQuadrado(icone: ImageVector, cor: Color) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
    }
}

/** Linha fina de separação entre as linhas de um grupo. */
@Composable
private fun Divisoria() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 68.dp)
            .height(1.dp)
            .background(OrbitTokens.borderSoft.copy(alpha = 0.5f))
    )
}

/** Botão de sair, em vermelho suave. */
@Composable
private fun BotaoSair() {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OrbitTokens.danger.copy(alpha = 0.12f))
            .clickable { }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = OrbitTokens.danger, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Sair da conta", color = OrbitTokens.danger, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 900)
@Composable
fun AjustesScreenPreview() {
    AjustesScreen()
}
