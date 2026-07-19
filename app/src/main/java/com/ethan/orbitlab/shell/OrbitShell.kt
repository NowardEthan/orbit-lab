package com.ethan.orbitlab.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector

import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.ui.ajustes.AjustesScreen
import com.ethan.orbitlab.ui.chat.ChatScreen
import com.ethan.orbitlab.ui.conversas.ConversasScreen
import com.ethan.orbitlab.ui.inicio.InicioScreen
import com.ethan.orbitlab.ui.novidades.NovidadesScreen
import com.ethan.orbitlab.ui.perfil.PerfilScreen
import com.ethan.orbitlab.ui.theme.OrbitTokens

/**
 * OrbitShell — a espinha do app: barra de baixo fixa + telas trocando por cima dela.
 * O estado ATIVO da aba se marca pela COR acesa (branco vs cinza) + o ícone crescendo um pouco.
 */
private enum class OrbitTab(
    val label: String,
    val icone: ImageVector,
) {
    INICIO("Início", Icons.Rounded.Home),
    CONVERSAS("Conversas", Icons.Rounded.Email),
    PERFIL("Perfil", Icons.Rounded.Person),
    AJUSTES("Ajustes", Icons.Rounded.Settings),
}

@Composable
fun OrbitShell() {
    var abaAtual by remember { mutableStateOf(OrbitTab.INICIO) }
    var menuAberto by remember { mutableStateOf(false) }
    var chatAberto by remember { mutableStateOf(false) }
    var conversaAtivaId by remember { mutableStateOf<String?>(null) }
    var novidadesAberto by remember { mutableStateOf(false) }
    var temNovidade by remember { mutableStateOf(true) }

    // Intercepta o botão 'Voltar' nativo do Android
    BackHandler(enabled = menuAberto || chatAberto || novidadesAberto) {
        when {
            menuAberto -> menuAberto = false
            chatAberto -> chatAberto = false
            novidadesAberto -> novidadesAberto = false
        }
    }

    Box(Modifier.fillMaxSize().background(OrbitTokens.ink1)) {
        Column(Modifier.fillMaxSize()) {
            // A ÁREA DE CONTEÚDO
            Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
                Crossfade(targetState = abaAtual, animationSpec = tween(220), label = "aba") { aba ->
                    when (aba) {
                        OrbitTab.INICIO -> InicioScreen(
                            onAbrirNovidades = { novidadesAberto = true; temNovidade = false },
                            temNovidade = temNovidade
                        )
                        OrbitTab.CONVERSAS -> ConversasScreen(
                            onOpenChat = { id ->
                                conversaAtivaId = id
                                chatAberto = true
                            }
                        )
                        OrbitTab.PERFIL -> PerfilScreen()
                        OrbitTab.AJUSTES -> AjustesScreen()
                    }
                }
            }
            OrbitBottomBar(
                atual = abaAtual,
                onAba = { abaAtual = it; menuAberto = false },
                menuAberto = menuAberto,
                onMais = { menuAberto = !menuAberto }
            )
        }
        
        // Fundo escurecido que fecha o menu ao tocar fora
        AnimatedVisibility(
            visible = menuAberto,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { menuAberto = false }
            )
        }
        
        // Mini-menu Flutuante
        AnimatedVisibility(
            visible = menuAberto,
            enter = scaleIn(animationSpec = tween(150), initialScale = 0.8f, transformOrigin = TransformOrigin(0.5f, 1f)) + fadeIn(tween(150)),
            exit = scaleOut(animationSpec = tween(100), targetScale = 0.8f, transformOrigin = TransformOrigin(0.5f, 1f)) + fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp)
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(OrbitTokens.surfaceRaised.copy(alpha = 0.95f))
                    .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(20.dp))
                    .padding(8.dp)
            ) {
                // Única opção atual: Nova Conversa
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { 
                            menuAberto = false
                            val newId = ChatRepository.criarConversa()
                            conversaAtivaId = newId
                            chatAberto = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.ChatBubble, contentDescription = null, tint = OrbitTokens.accentText, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Nova Conversa", color = OrbitTokens.textHigh, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Chat Overlay
        AnimatedVisibility(
            visible = chatAberto && conversaAtivaId != null,
            enter = scaleIn(animationSpec = tween(150), initialScale = 0.9f) + fadeIn(tween(150)),
            exit = scaleOut(animationSpec = tween(150), targetScale = 0.9f) + fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            conversaAtivaId?.let { id ->
                ChatScreen(conversaId = id, onBack = { chatAberto = false })
            }
        }

        // Novidades Overlay (abre pelo sino da Início)
        AnimatedVisibility(
            visible = novidadesAberto,
            enter = scaleIn(animationSpec = tween(150), initialScale = 0.9f) + fadeIn(tween(150)),
            exit = scaleOut(animationSpec = tween(150), targetScale = 0.9f) + fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            NovidadesScreen(onBack = { novidadesAberto = false })
        }
    }
}

@Composable
private fun OrbitBottomBar(atual: OrbitTab, onAba: (OrbitTab) -> Unit, menuAberto: Boolean, onMais: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = OrbitTokens.surfaceRaised.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, OrbitTokens.borderSoft),
        shadowElevation = 24.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(OrbitTab.INICIO, atual, onAba, Modifier.weight(1f))
            TabItem(OrbitTab.CONVERSAS, atual, onAba, Modifier.weight(1f))

            // Espaço fixo no centro para o botão mágico
            Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) {
                val interacao = remember { MutableInteractionSource() }
                val pressionado by interacao.collectIsPressedAsState()
                val escala by animateFloatAsState(if (pressionado) 0.85f else 1f, animationSpec = tween(120), label = "fab")
                Box(
                    Modifier
                        .scale(escala)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(colors = listOf(OrbitTokens.violet, OrbitTokens.gold)))
                        .clickable(interactionSource = interacao, indication = null) { onMais() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add, 
                        contentDescription = "Abrir", 
                        tint = Color.White, 
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer { rotationZ = if (menuAberto) 45f else 0f } // Gira para formar um X
                    )
                }
            }

            TabItem(OrbitTab.PERFIL, atual, onAba, Modifier.weight(1f))
            TabItem(OrbitTab.AJUSTES, atual, onAba, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(tab: OrbitTab, atual: OrbitTab, onAba: (OrbitTab) -> Unit, modifier: Modifier = Modifier) {
    val ativa = tab == atual
    // O estado ativo acende a cor
    val cor by animateColorAsState(
        if (ativa) OrbitTokens.textHigh else OrbitTokens.textLow,
        animationSpec = tween(200),
        label = "cor",
    )
    // Sensação de clique macio
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val escalaClique by animateFloatAsState(if (pressionado) 0.86f else 1f, animationSpec = tween(120), label = "press")

    // O ícone cresce um pouquinho quando a aba está ativa
    val escalaIcone by animateFloatAsState(if (ativa) 1.15f else 1f, animationSpec = tween(200), label = "iconScale")

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(64.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .clickable(interactionSource = interacao, indication = null) { onAba(tab) }
                .scale(escalaClique),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icone,
                contentDescription = tab.label,
                tint = cor,
                modifier = Modifier
                    .size(26.dp)
                    .scale(escalaIcone),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 800)
@Composable
fun OrbitShellPreview() {
    OrbitShell()
}
