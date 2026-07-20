package com.ethan.orbitlab.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.ui.ajustes.AjustesScreen
import com.ethan.orbitlab.ui.auth.LoginScreen
import com.ethan.orbitlab.ui.chat.ChatScreen
import com.ethan.orbitlab.ui.conversas.ConversasScreen
import com.ethan.orbitlab.ui.inicio.InicioScreen
import com.ethan.orbitlab.ui.novidades.NovidadesScreen
import com.ethan.orbitlab.ui.perfil.PerfilScreen
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.ethan.orbitlab.ui.theme.orbitTabReveal

/**
 * OrbitShell — barra + telas.
 * Abas: uma árvore + micro-reveal; overlays: fade/slide curto.
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
    val authReady by AuthRepository.authReady.collectAsState()
    val session by AuthRepository.session.collectAsState()

    if (!authReady) {
        Box(
            Modifier.fillMaxSize().background(OrbitTokens.ink1),
            contentAlignment = Alignment.Center,
        ) {
            // Splash curto enquanto o Firebase Auth restaura a sessão
        }
        return
    }

    if (session == null) {
        LoginScreen(onAutenticado = {})
        return
    }

    var abaAtual by remember { mutableStateOf(OrbitTab.INICIO) }
    var menuAberto by remember { mutableStateOf(false) }
    var chatAberto by remember { mutableStateOf(false) }
    var conversaAtivaId by remember { mutableStateOf<String?>(null) }
    var novidadesAberto by remember { mutableStateOf(false) }
    val manifest by UpdatesRepository.manifest.collectAsState()
    val seenSignature by UpdatesRepository.seenSignature.collectAsState()
    val seenLoaded by UpdatesRepository.seenLoaded.collectAsState()
    val temNovidade = remember(manifest, seenSignature, seenLoaded) {
        if (!seenLoaded) return@remember false
        val topNews = manifest?.news?.firstOrNull()?.id.orEmpty()
        val sig = manifest?.let { "${it.latestVersion}::$topNews" }
        sig != null && sig != seenSignature
    }
    val abaStateHolder = rememberSaveableStateHolder()

    val onAbrirNovidades = remember {
        {
            novidadesAberto = true
            UpdatesRepository.markSeen()
        }
    }
    val onOpenChat = remember {
        { id: String ->
            conversaAtivaId = id
            chatAberto = true
        }
    }
    val onAbrirAjustes = remember { { abaAtual = OrbitTab.AJUSTES } }
    val onAbrirPerfil = remember { { abaAtual = OrbitTab.PERFIL } }
    val onConversarComLuna = remember {
        {
            conversaAtivaId = ChatRepository.conversas.value.firstOrNull()?.id
                ?: ChatRepository.criarConversa()
            chatAberto = true
        }
    }
    val onAba = remember {
        { tab: OrbitTab ->
            abaAtual = tab
            menuAberto = false
        }
    }
    val onMais = remember { { menuAberto = !menuAberto } }
    val onFecharMenu = remember { { menuAberto = false } }
    val onFecharChat = remember { { chatAberto = false } }
    val onFecharNovidades = remember { { novidadesAberto = false } }
    val onNovaConversa = remember {
        {
            menuAberto = false
            val newId = ChatRepository.criarConversa()
            conversaAtivaId = newId
            chatAberto = true
        }
    }

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
            // Uma aba por vez; sob chat/novidades não compomos a aba (estado no SaveableStateHolder)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .orbitTabReveal(abaAtual.name),
            ) {
                if (!chatAberto && !novidadesAberto) {
                    abaStateHolder.SaveableStateProvider(abaAtual.name) {
                        when (abaAtual) {
                            OrbitTab.INICIO -> InicioScreen(
                                onAbrirNovidades = onAbrirNovidades,
                                temNovidade = temNovidade,
                                onAbrirConversa = onOpenChat,
                                onNovaConversa = onNovaConversa,
                                idleAtivo = true,
                                onMarkNovidadesVistas = { UpdatesRepository.markSeen() },
                            )
                            OrbitTab.CONVERSAS -> ConversasScreen(onOpenChat = onOpenChat)
                            OrbitTab.PERFIL -> PerfilScreen(
                                onConversarComLuna = onConversarComLuna,
                                onAbrirConversa = onOpenChat,
                            )
                            OrbitTab.AJUSTES -> AjustesScreen(onAbrirPerfil = onAbrirPerfil)
                        }
                    }
                }
            }
            OrbitBottomBar(
                atual = abaAtual,
                onAba = onAba,
                menuAberto = menuAberto,
                onMais = onMais,
            )
        }

        AnimatedVisibility(
            visible = menuAberto,
            enter = OrbitMotion.scrimEnter(),
            exit = OrbitMotion.scrimExit(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onFecharMenu,
                    ),
            )
        }

        AnimatedVisibility(
            visible = menuAberto,
            enter = OrbitMotion.popupEnter(),
            exit = OrbitMotion.popupExit(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(OrbitMetrics.radiusCard))
                    .background(OrbitTokens.surfaceRaised.copy(alpha = 0.95f))
                    .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(OrbitMetrics.radiusCard))
                    .padding(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                        .orbitPressable(onClick = onNovaConversa)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.ChatBubble,
                        contentDescription = null,
                        tint = OrbitTokens.accentText,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Nova conversa",
                        color = OrbitTokens.textHigh,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Chat / Novidades — só fade (sem scale full-screen)
        AnimatedVisibility(
            visible = chatAberto && conversaAtivaId != null,
            enter = OrbitMotion.overlayEnter(),
            exit = OrbitMotion.overlayExit(),
            modifier = Modifier.fillMaxSize(),
        ) {
            conversaAtivaId?.let { id ->
                ChatScreen(conversaId = id, onBack = onFecharChat)
            }
        }

        AnimatedVisibility(
            visible = novidadesAberto,
            enter = OrbitMotion.overlayEnter(),
            exit = OrbitMotion.overlayExit(),
            modifier = Modifier.fillMaxSize(),
        ) {
            NovidadesScreen(onBack = onFecharNovidades)
        }
    }
}

@Composable
private fun OrbitBottomBar(atual: OrbitTab, onAba: (OrbitTab) -> Unit, menuAberto: Boolean, onMais: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = OrbitMetrics.radiusCard, topEnd = OrbitMetrics.radiusCard),
        color = OrbitTokens.surfaceRaised,
        border = BorderStroke(1.dp, OrbitTokens.borderSoft),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp)
                .height(OrbitMetrics.navHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(OrbitTab.INICIO, atual, onAba, Modifier.weight(1f))
            TabItem(OrbitTab.CONVERSAS, atual, onAba, Modifier.weight(1f))

            Box(Modifier.width(72.dp), contentAlignment = Alignment.Center) {
                FabMais(menuAberto = menuAberto, onMais = onMais)
            }

            TabItem(OrbitTab.PERFIL, atual, onAba, Modifier.weight(1f))
            TabItem(OrbitTab.AJUSTES, atual, onAba, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FabMais(menuAberto: Boolean, onMais: () -> Unit) {
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) OrbitMotion.pressScale else 1f,
        animationSpec = OrbitMotion.springPress,
        label = "fabPress",
    )
    val rotacao by animateFloatAsState(
        targetValue = if (menuAberto) 45f else 0f,
        animationSpec = OrbitMotion.springPress,
        label = "fabRot",
    )

    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clickable(
                interactionSource = interacao,
                indication = null,
                role = Role.Button,
                onClick = onMais,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F4F8)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = if (menuAberto) "Fechar" else "Abrir",
                tint = OrbitTokens.ink0,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = rotacao },
            )
        }
        // Assinatura Aura Blue no canto (mockup)
        Canvas(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(12.dp),
        ) {
            val path = Path().apply {
                moveTo(size.width, size.height)
                lineTo(size.width, size.height * 0.15f)
                lineTo(size.width * 0.15f, size.height)
                close()
            }
            drawPath(path, color = OrbitTokens.accent)
        }
    }
}

@Composable
private fun TabItem(tab: OrbitTab, atual: OrbitTab, onAba: (OrbitTab) -> Unit, modifier: Modifier = Modifier) {
    val ativa = tab == atual
    val cor = if (ativa) OrbitTokens.textHigh else OrbitTokens.textLow
    val indicadorLargura by animateDpAsState(
        targetValue = if (ativa) 12.dp else 0.dp,
        animationSpec = tween(OrbitMotion.msFast),
        label = "indicador",
    )
    val interacao = remember { MutableInteractionSource() }
    val pressionado by interacao.collectIsPressedAsState()
    val escalaIcone by animateFloatAsState(
        targetValue = if (pressionado) OrbitMotion.pressScale else 1f,
        animationSpec = OrbitMotion.springPress,
        label = "tabPress",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interacao,
                indication = null,
                role = Role.Tab,
                onClick = { onAba(tab) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = tab.icone,
                contentDescription = tab.label,
                tint = cor,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = escalaIcone
                        scaleY = escalaIcone
                    },
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(indicadorLargura)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (ativa) OrbitTokens.accent else Color.Transparent),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 800)
@Composable
fun OrbitShellPreview() {
    OrbitShell()
}
