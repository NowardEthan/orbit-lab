package com.ethan.orbitlab.shell

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Conversa
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.ui.ajustes.AjustesScreen
import com.ethan.orbitlab.ui.auth.LoginScreen
import com.ethan.orbitlab.ui.chat.ChatScreen
import com.ethan.orbitlab.ui.conversas.ConversasScreen
import com.ethan.orbitlab.ui.estante.EstanteScreen
import com.ethan.orbitlab.ui.inicio.InicioScreen
import com.ethan.orbitlab.ui.novidades.NovidadesScreen
import com.ethan.orbitlab.ui.perfil.PerfilScreen
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitIconButton
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.ethan.orbitlab.ui.theme.orbitTabReveal

/**
 * OrbitShell — a moldura do app na direção 1.0: gaveta lateral (à esquerda) no
 * lugar da barra de baixo, + uma barra de cima fininha com o ☰. As telas seguem
 * uma-por-vez com micro-reveal; chat/novidades/estante são overlays full-screen.
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
    val activity = LocalContext.current as? Activity

    // Sessão sumiu mas a conta Google segue autorizada aqui: reentra sozinho antes de
    // pedir login. Uma tentativa por abertura — se não der, cai na tela de login.
    val expirou by AuthRepository.restauroExpirou.collectAsState()
    var reentrando by remember { mutableStateOf(false) }
    LaunchedEffect(expirou, session) {
        if (!expirou || session != null) return@LaunchedEffect
        if (activity == null) {
            AuthRepository.desistiuDoRestauro()
            return@LaunchedEffect
        }
        reentrando = true
        val deuCerto = AuthRepository.restaurarGoogleSilencioso(activity)
        reentrando = false
        if (!deuCerto) AuthRepository.desistiuDoRestauro()
    }

    if (!authReady) {
        Box(
            Modifier.fillMaxSize().background(OrbitTokens.graphiteBg),
            contentAlignment = Alignment.Center,
        ) {
            var demorou by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(900)
                demorou = true
            }
            if (demorou) {
                Text(
                    if (reentrando) "Reentrando na sua conta…" else "Recuperando sua sessão…",
                    color = OrbitTokens.textMidN,
                    fontSize = 13.sp,
                )
            }
        }
        return
    }

    if (session == null) {
        LoginScreen(onAutenticado = {})
        return
    }

    // Volta exatamente onde ele parou (o Android mata o app por fome de memória).
    var abaAtual by remember {
        mutableStateOf(
            PrefsRepository.ultimaAba
                ?.let { salva -> OrbitTab.entries.firstOrNull { it.name == salva } }
                ?: OrbitTab.INICIO,
        )
    }
    var conversaAtivaId by remember { mutableStateOf(PrefsRepository.ultimaConversa) }
    var chatAberto by remember { mutableStateOf(conversaAtivaId != null) }
    // Texto digitado no composer do Início: abre uma conversa nova JÁ mandando esta 1ª mensagem.
    var mensagemInicial by remember { mutableStateOf<String?>(null) }
    var novidadesAberto by remember { mutableStateOf(false) }
    var estanteAberta by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val fecharGaveta = remember { { scope.launch { drawerState.close() }; Unit } }

    LaunchedEffect(abaAtual) { PrefsRepository.ultimaAba = abaAtual.name }
    LaunchedEffect(chatAberto, conversaAtivaId) {
        PrefsRepository.ultimaConversa = conversaAtivaId?.takeIf { chatAberto }
    }
    val manifest by UpdatesRepository.manifest.collectAsState()
    val seenSignature by UpdatesRepository.seenSignature.collectAsState()
    val seenLoaded by UpdatesRepository.seenLoaded.collectAsState()
    val temNovidade = remember(manifest, seenSignature, seenLoaded) {
        if (!seenLoaded) return@remember false
        val topNews = manifest?.news?.firstOrNull()?.id.orEmpty()
        val sig = manifest?.let { "${it.latestVersion}::$topNews" }
        sig != null && sig != seenSignature
    }
    val conversas by ChatRepository.conversas.collectAsState()
    val profile by UserProfileRepository.profile.collectAsState()
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
    val onAbrirPerfil = remember { { abaAtual = OrbitTab.PERFIL } }
    val onConversarComLuna = remember {
        {
            conversaAtivaId = ChatRepository.conversas.value.firstOrNull()?.id
                ?: ChatRepository.criarConversa()
            chatAberto = true
        }
    }
    val onAba = remember { { tab: OrbitTab -> abaAtual = tab } }
    val onFecharChat = remember { { chatAberto = false } }
    val onFecharNovidades = remember { { novidadesAberto = false } }
    val onFecharEstante = remember { { estanteAberta = false } }
    val onAbrirEstante = remember { { estanteAberta = true } }
    val onNovaConversa = remember {
        {
            val newId = ChatRepository.criarConversa()
            conversaAtivaId = newId
            chatAberto = true
        }
    }
    val onNovaConversaComTexto = remember {
        { texto: String ->
            val newId = ChatRepository.criarConversa()
            conversaAtivaId = newId
            mensagemInicial = texto
            chatAberto = true
        }
    }

    val temOverlay = chatAberto || novidadesAberto || estanteAberta

    // Botão 'Voltar' nativo: gaveta primeiro, depois os overlays.
    BackHandler(enabled = drawerState.isOpen || temOverlay) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            chatAberto -> chatAberto = false
            novidadesAberto -> novidadesAberto = false
            estanteAberta -> estanteAberta = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen || !temOverlay,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = OrbitTokens.graphiteBg,
                drawerContentColor = OrbitTokens.textHiN,
            ) {
                OrbitDrawer(
                    atual = abaAtual,
                    recentes = conversas.take(6),
                    nome = (profile.displayName.ifBlank { session?.displayName.orEmpty() })
                        .substringBefore(' ').ifBlank { "você" },
                    avatarUrl = profile.avatarUrl,
                    onAba = { tab -> onAba(tab); fecharGaveta() },
                    onEstante = { onAbrirEstante(); fecharGaveta() },
                    onPerfil = { onAbrirPerfil(); fecharGaveta() },
                    onAbrirConversa = { id -> onOpenChat(id); fecharGaveta() },
                    onNovaConversa = { onNovaConversa(); fecharGaveta() },
                )
            }
        },
    ) {
        ShellConteudo(
            abaAtual = abaAtual,
            temOverlay = temOverlay,
            abaStateHolder = abaStateHolder,
            temNovidade = temNovidade,
            chatAberto = chatAberto,
            conversaAtivaId = conversaAtivaId,
            novidadesAberto = novidadesAberto,
            estanteAberta = estanteAberta,
            onAbrirMenu = { scope.launch { drawerState.open() } },
            onAbrirNovidades = onAbrirNovidades,
            onOpenChat = onOpenChat,
            onNovaConversa = onNovaConversa,
            onNovaConversaComTexto = onNovaConversaComTexto,
            mensagemInicial = mensagemInicial,
            onMensagemInicialConsumida = { mensagemInicial = null },
            onAbrirEstante = onAbrirEstante,
            onConversarComLuna = onConversarComLuna,
            onAbrirPerfil = onAbrirPerfil,
            onFecharChat = onFecharChat,
            onFecharNovidades = onFecharNovidades,
            onFecharEstante = onFecharEstante,
        )
    }
}

@Composable
private fun ShellConteudo(
    abaAtual: OrbitTab,
    temOverlay: Boolean,
    abaStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    temNovidade: Boolean,
    chatAberto: Boolean,
    conversaAtivaId: String?,
    novidadesAberto: Boolean,
    estanteAberta: Boolean,
    onAbrirMenu: () -> Unit,
    onAbrirNovidades: () -> Unit,
    onOpenChat: (String) -> Unit,
    onNovaConversa: () -> Unit,
    onNovaConversaComTexto: (String) -> Unit,
    mensagemInicial: String?,
    onMensagemInicialConsumida: () -> Unit,
    onAbrirEstante: () -> Unit,
    onConversarComLuna: () -> Unit,
    onAbrirPerfil: () -> Unit,
    onFecharChat: () -> Unit,
    onFecharNovidades: () -> Unit,
    onFecharEstante: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(OrbitTokens.graphiteBg)) {
        Column(Modifier.fillMaxSize()) {
            OrbitTopBar(
                onAbrirMenu = onAbrirMenu,
                temNovidade = temNovidade,
                onAbrirNovidades = onAbrirNovidades,
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .orbitTabReveal(abaAtual.name),
            ) {
                if (!temOverlay) {
                    abaStateHolder.SaveableStateProvider(abaAtual.name) {
                        when (abaAtual) {
                            OrbitTab.INICIO -> InicioScreen(
                                onAbrirConversa = onOpenChat,
                                onNovaConversa = onNovaConversa,
                                onNovaConversaComTexto = onNovaConversaComTexto,
                                idleAtivo = true,
                            )
                            OrbitTab.CONVERSAS -> ConversasScreen(
                                onOpenChat = onOpenChat,
                                onAbrirEstante = onAbrirEstante,
                            )
                            OrbitTab.PERFIL -> PerfilScreen(
                                onConversarComLuna = onConversarComLuna,
                                onAbrirConversa = onOpenChat,
                            )
                            OrbitTab.AJUSTES -> AjustesScreen(onAbrirPerfil = onAbrirPerfil)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = chatAberto && conversaAtivaId != null,
            enter = OrbitMotion.overlayEnter(),
            exit = OrbitMotion.overlayExit(),
            modifier = Modifier.fillMaxSize(),
        ) {
            conversaAtivaId?.let { id ->
                ChatScreen(
                    conversaId = id,
                    onBack = onFecharChat,
                    mensagemInicial = mensagemInicial,
                    onMensagemInicialConsumida = onMensagemInicialConsumida,
                )
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

        AnimatedVisibility(
            visible = estanteAberta,
            enter = OrbitMotion.overlayEnter(),
            exit = OrbitMotion.overlayExit(),
            modifier = Modifier.fillMaxSize(),
        ) {
            EstanteScreen(onBack = onFecharEstante)
        }
    }
}

/**
 * Barra de cima fininha: o ☰ à esquerda e o sino de novidades à direita — este último
 * mora aqui pra aparecer em TODOS os destinos (Início/Conversas/Perfil/Ajustes) e ficar
 * naturalmente fora do chat (overlay com header próprio). As telas trazem o próprio título.
 */
@Composable
private fun OrbitTopBar(
    onAbrirMenu: () -> Unit,
    temNovidade: Boolean,
    onAbrirNovidades: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(46.dp)
            .padding(horizontal = OrbitMetrics.pagePadding - 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrbitIconButton(
            icon = Icons.Rounded.Menu,
            contentDescription = "Menu",
            onClick = onAbrirMenu,
            tint = OrbitTokens.textHiN,
            iconSize = 22.dp,
        )
        Spacer(Modifier.weight(1f))
        Box(contentAlignment = Alignment.Center) {
            OrbitIconButton(
                icon = Icons.Rounded.Notifications,
                contentDescription = "Novidades",
                onClick = onAbrirNovidades,
                iconSize = 21.dp,
            )
            if (temNovidade) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(OrbitTokens.bluePastel),
                )
            }
        }
    }
}

@Composable
private fun OrbitDrawer(
    atual: OrbitTab,
    recentes: List<Conversa>,
    nome: String,
    avatarUrl: String?,
    onAba: (OrbitTab) -> Unit,
    onEstante: () -> Unit,
    onPerfil: () -> Unit,
    onAbrirConversa: (String) -> Unit,
    onNovaConversa: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp),
    ) {
        // Assinatura
        Text(
            "Orbit",
            color = OrbitTokens.textHiN,
            fontSize = 26.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.padding(start = 8.dp, top = 18.dp, bottom = 20.dp),
        )

        // Destinos
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            DrawerNavItem(Icons.Rounded.Home, "Início", atual == OrbitTab.INICIO) { onAba(OrbitTab.INICIO) }
            DrawerNavItem(Icons.Rounded.Email, "Conversas", atual == OrbitTab.CONVERSAS) { onAba(OrbitTab.CONVERSAS) }
            DrawerNavItem(Icons.AutoMirrored.Rounded.MenuBook, "Estante", false, onEstante)
            DrawerNavItem(Icons.Rounded.Person, "Perfil", atual == OrbitTab.PERFIL) { onAba(OrbitTab.PERFIL) }
            DrawerNavItem(Icons.Rounded.Settings, "Ajustes", atual == OrbitTab.AJUSTES) { onAba(OrbitTab.AJUSTES) }

            if (recentes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = OrbitTokens.graphiteHair)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Recentes",
                    color = OrbitTokens.textLowN,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                )
                recentes.forEach { conv ->
                    Text(
                        conv.titulo,
                        color = OrbitTokens.textMidN,
                        fontSize = 14.5.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .orbitPressable { onAbrirConversa(conv.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }

        // Rodapé: perfil + pílula Nova conversa
        HorizontalDivider(color = OrbitTokens.graphiteHair)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .orbitPressable(onClick = onPerfil)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.graphiteRaised),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        nome.take(1).uppercase(),
                        color = OrbitTokens.textHiN,
                        fontSize = 15.sp,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                nome,
                color = OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(OrbitTokens.bluePastel)
                .orbitPressable(onClick = onNovaConversa)
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Chat,
                contentDescription = null,
                tint = OrbitTokens.onBluePastel,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Nova conversa",
                color = OrbitTokens.onBluePastel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DrawerNavItem(
    icone: ImageVector,
    label: String,
    ativo: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (ativo) OrbitTokens.graphiteRaised else androidx.compose.ui.graphics.Color.Transparent)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = if (ativo) OrbitTokens.bluePastel else OrbitTokens.textMidN,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = if (ativo) OrbitTokens.textHiN else OrbitTokens.textMidN,
            fontSize = 15.5.sp,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF17181B, widthDp = 380, heightDp = 800)
@Composable
fun OrbitShellPreview() {
    OrbitShell()
}
