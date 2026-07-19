package com.ethan.orbitlab.ui.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens

@Composable
fun PerfilScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        HeaderCapaAvatar()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        BioESocialStats()
        
        Spacer(modifier = Modifier.height(36.dp))
        
        GridConquistas()
    }
}

@Composable
private fun HeaderCapaAvatar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        // Capa Estilo Mesh/Atmosfera da Luna
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(OrbitTokens.violet.copy(alpha = 0.5f), OrbitTokens.gold.copy(alpha = 0.15f))
                    )
                )
        ) {
            // Efeitos de luz (Glowing Orb) integrados à capa
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-30).dp)
                    .size(200.dp)
                    .background(Brush.radialGradient(listOf(OrbitTokens.violet.copy(alpha=0.6f), Color.Transparent)))
            )
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-40).dp, y = 40.dp)
                    .size(150.dp)
                    .background(Brush.radialGradient(listOf(OrbitTokens.gold.copy(alpha=0.4f), Color.Transparent)))
            )
            
            // Botão de Configurações no canto superior
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 24.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.ink1.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Configurações", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        
        // Avatar circular centralizado, sobrepondo a capa
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(116.dp)
                .clip(CircleShape)
                .background(OrbitTokens.ink1) // Borda negativa para "cortar" a capa
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(OrbitTokens.surfaceRaised)
                    // Anel brilhante usando as cores da gamificação
                    .border(3.dp, Brush.linearGradient(listOf(OrbitTokens.gold, OrbitTokens.violet)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, contentDescription = "Avatar", tint = OrbitTokens.textMid, modifier = Modifier.size(54.dp))
            }
        }
    }
}

@Composable
private fun BioESocialStats() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ethan", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Título "Lunatizado" (Badge do usuário)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(OrbitTokens.violet.copy(alpha = 0.15f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Rounded.Star, contentDescription = null, tint = OrbitTokens.violet, modifier = Modifier.size(16.dp))
            Text("Explorador Sênior da Lumen", color = OrbitTokens.violet, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Stats em linha (Substituindo 'Seguidores/Seguindo')
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("32", "Ofensiva", OrbitTokens.online)
            StatItem("1.240", "Luz", OrbitTokens.gold)
            StatItem("850", "Lua", OrbitTokens.violet)
        }
    }
}

@Composable
private fun StatItem(valor: String, label: String, cor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, color = cor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = OrbitTokens.textMid, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// Dados para a Grade de Conquistas
private data class BadgeMock(
    val id: Int, 
    val titulo: String, 
    val icone: ImageVector, 
    val colorStart: Color, 
    val colorEnd: Color,
    val contentColor: Color,
    val opaco: Boolean = false
)

@Composable
private fun GridConquistas() {
    val badges = listOf(
        BadgeMock(1, "Iniciador", Icons.Rounded.LocalFireDepartment, OrbitTokens.online, Color(0xFF23B55C), Color.White),
        BadgeMock(2, "Foco Profundo", Icons.Rounded.WorkspacePremium, OrbitTokens.gold, Color(0xFFD89B1A), Color(0xFF332504)),
        BadgeMock(3, "Mente Aberta", Icons.Rounded.Star, OrbitTokens.violet, Color(0xFF6732DD), Color.White),
        BadgeMock(4, "Bloqueado", Icons.Rounded.Lock, OrbitTokens.surfaceRaised, OrbitTokens.surface, OrbitTokens.textLow, opaco = true),
        BadgeMock(5, "Bloqueado", Icons.Rounded.Lock, OrbitTokens.surfaceRaised, OrbitTokens.surface, OrbitTokens.textLow, opaco = true),
        BadgeMock(6, "Misterioso", Icons.Rounded.Lock, OrbitTokens.surfaceRaised, OrbitTokens.surface, OrbitTokens.textLow, opaco = true)
    )
    
    Column(Modifier.padding(horizontal = 24.dp)) {
        Text("Suas Conquistas", color = OrbitTokens.textHigh, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        // Simulação de um grid 3x2 com colunas fixas
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                badges.take(3).forEach { badge ->
                    BadgeCard(badge, Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                badges.drop(3).forEach { badge ->
                    BadgeCard(badge, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: BadgeMock, modifier: Modifier) {
    val alphaBackground = if (badge.opaco) 0.5f else 1f
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(badge.colorStart.copy(alpha = alphaBackground), badge.colorEnd.copy(alpha = alphaBackground))))
            .padding(vertical = 24.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Círculo interno para destacar o ícone, usando o contraste do texto
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(badge.contentColor.copy(alpha = if (badge.opaco) 0.05f else 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(badge.icone, contentDescription = null, tint = badge.contentColor.copy(alpha = if (badge.opaco) 0.4f else 0.9f), modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = badge.titulo, 
            color = badge.contentColor.copy(alpha = if (badge.opaco) 0.4f else 1f), 
            fontSize = 13.sp, 
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 800)
@Composable
fun PerfilScreenPreview() {
    PerfilScreen()
}
