package com.ethan.orbitlab.shell

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import com.ethan.orbitlab.BuildConfig
import com.ethan.orbitlab.data.updates.isNewer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import androidx.compose.ui.draw.rotate
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
import com.ethan.orbitlab.data.financas.FinancasNav
import com.ethan.orbitlab.data.financas.FinancasRepository
import com.ethan.orbitlab.data.financas.TransferenciaLauncher
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.ui.ajustes.AjustesScreen
import com.ethan.orbitlab.ui.auth.LoginScreen
import com.ethan.orbitlab.ui.chat.ChatScreen
import com.ethan.orbitlab.ui.conversas.ConversasScreen
import com.ethan.orbitlab.ui.estante.EstanteScreen
import com.ethan.orbitlab.ui.financas.CapturaScreen
import com.ethan.orbitlab.ui.financas.CartoesScreen
import com.ethan.orbitlab.ui.financas.ConstanciaScreen
import com.ethan.orbitlab.ui.financas.ExtratoScreen
import com.ethan.orbitlab.ui.financas.FinancasDashboardScreen
import com.ethan.orbitlab.ui.financas.FinancasLunaChatSheet
import com.ethan.orbitlab.ui.financas.FinancasLunaFab
import com.ethan.orbitlab.ui.financas.FinancasLunaWidget
import com.ethan.orbitlab.ui.financas.MetasScreen
import com.ethan.orbitlab.ui.financas.RecorrentesScreen
import com.ethan.orbitlab.ui.financas.TransferenciaScreen
import com.ethan.orbitlab.ui.inicio.InicioScreen
import com.ethan.orbitlab.ui.novidades.NovidadesScreen
import com.ethan.orbitlab.ui.perfil.PerfilScreen
import com.ethan.orbitlab.ui.planos.PlanosScreen
import com.ethan.orbitlab.ui.planos.UsageMeter
import com.ethan.orbitlab.data.billing.PlanosNav
import com.ethan.orbitlab.data.billing.UsageRepository
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
 * uma-por-vez com micro-reveal; chat/novidades são overlays full-screen.
 *
 * Finanças vive na gaveta como destinos próprios (Entradas e saídas, Cartões) —
 * não como chips internos. Dashboard "Finanças" (F3) entra depois nesta seção.
 */
private enum class OrbitTab(
    val label: String,
    val icone: ImageVector,
) {
    INICIO("Início", Icons.Rounded.Home),
    FINANCAS("Finanças", Icons.Rounded.AccountBalanceWallet),
    CONSTANCIA("Constância", Icons.Rounded.AutoAwesome),
    EXTRATO("Movimentações", Icons.Rounded.SwapVert),
    CARTOES("Cartões", Icons.Rounded.CreditCard),
    RECORRENTES("Recorrentes", Icons.Rounded.Repeat),
    TRANSFERENCIA("Transferência", Icons.Rounded.SwapHoriz),
    METAS("Metas", Icons.Rounded.Flag),
    CAPTURA("Captura", Icons.Rounded.Hearing),
    CONVERSAS("Conversas", Icons.Rounded.Email),
    ESTANTE("Estante", Icons.AutoMirrored.Rounded.MenuBook),
    PLANO("Planos", Icons.Rounded.WorkspacePremium),
    PERFIL("Perfil", Icons.Rounded.Person),
    AJUSTES("Ajustes", Icons.Rounded.Settings),
}

private fun OrbitTab.ehFinancas(): Boolean = when (this) {
    OrbitTab.FINANCAS,
    OrbitTab.CONSTANCIA,
    OrbitTab.EXTRATO,
    OrbitTab.CARTOES,
    OrbitTab.RECORRENTES,
    OrbitTab.TRANSFERENCIA,
    OrbitTab.METAS,
    OrbitTab.CAPTURA,
    -> true
    else -> false
}

private fun orbitTabFromPrefs(salva: String?): OrbitTab? {
    if (salva.isNullOrBlank()) return null
    return OrbitTab.entries.firstOrNull { it.name == salva }
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
        mutableStateOf(orbitTabFromPrefs(PrefsRepository.ultimaAba) ?: OrbitTab.INICIO)
    }

    // Finanças: um listener compartilhado enquanto a sessão existir.
    LaunchedEffect(session?.uid) {
        FinancasRepository.garantirSessao(session?.uid)
        // A carteira (medidor + parede) segue a mesma sessão: lê /v1/billing/usage e
        // escuta o Firestore pra atualizar sozinha.
        UsageRepository.observar(session?.uid)
    }
    // Cartões → "Pagar fatura" abre a aba Transferência já pré-preenchida.
    val transferenciaNav by TransferenciaLauncher.navegarTick.collectAsState()
    LaunchedEffect(transferenciaNav) {
        if (transferenciaNav > 0L) abaAtual = OrbitTab.TRANSFERENCIA
    }
    // Painel / outras telas → Extrato, Captura, etc.
    val financasNavTick by FinancasNav.navegarTick.collectAsState()
    LaunchedEffect(financasNavTick) {
        if (financasNavTick <= 0L) return@LaunchedEffect
        val nome = FinancasNav.consumir() ?: return@LaunchedEffect
        orbitTabFromPrefs(nome)?.let { abaAtual = it }
    }
    var conversaAtivaId by remember { mutableStateOf(PrefsRepository.ultimaConversa) }
    var chatAberto by remember { mutableStateOf(conversaAtivaId != null) }
    // FAB Finanças → widget flutuante e sheet modal.
    var financasWidgetAberto by remember { mutableStateOf(false) }
    var financasChatAberto by remember { mutableStateOf(false) }
    var financasConversaId by remember { mutableStateOf<String?>(null) }
    // Texto digitado no composer do Início: abre uma conversa nova JÁ mandando esta 1ª mensagem.
    var mensagemInicial by remember { mutableStateOf<String?>(null) }
    var novidadesAberto by remember { mutableStateOf(false) }

    // Parede do chat / medidor pedem "ver planos" → abre a aba Planos fechando os overlays.
    val planosNavTick by PlanosNav.tick.collectAsState()
    LaunchedEffect(planosNavTick) {
        if (planosNavTick <= 0L) return@LaunchedEffect
        abaAtual = OrbitTab.PLANO
        chatAberto = false
        novidadesAberto = false
    }

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
    val updateAvailable = remember(manifest) {
        val m = manifest ?: return@remember false
        val latestCode = m.latestVersionCode
        if (latestCode != null) latestCode > BuildConfig.VERSION_CODE
        else isNewer(m.latestVersion, BuildConfig.VERSION_NAME)
    }
    val notificacoesCount = remember(manifest, seenSignature, seenLoaded, updateAvailable) {
        if (!seenLoaded) return@remember 0
        val news = manifest?.news.orEmpty()
        val seenTopId = seenSignature?.substringAfter("::", "")?.takeIf { it.isNotEmpty() }
        val unseen = when {
            news.isEmpty() -> 0
            seenTopId == null -> news.size // nunca abriu Novidades → tudo é novo
            else -> news.indexOfFirst { it.id == seenTopId }.let { if (it >= 0) it else news.size }
        }
        if (unseen == 0 && updateAvailable) 1 else unseen
    }
    val temNovidade = notificacoesCount > 0
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
    val onAbrirEstante = remember { { abaAtual = OrbitTab.ESTANTE } }
    val onAbrirFinancas = remember { { abaAtual = OrbitTab.FINANCAS } }
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
    val onAbrirConversaFinancas = remember {
        {
            financasConversaId = ChatRepository.garantirConversaFinancas()
            financasChatAberto = true
        }
    }

    val temOverlay = chatAberto || novidadesAberto
    val temModalFinancas = financasChatAberto

    // Botão 'Voltar' nativo: gaveta → widget Finanças → sheet Finanças → overlays.
    BackHandler(enabled = drawerState.isOpen || temOverlay || financasWidgetAberto || temModalFinancas) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            financasWidgetAberto -> financasWidgetAberto = false
            financasChatAberto -> financasChatAberto = false
            chatAberto -> chatAberto = false
            novidadesAberto -> novidadesAberto = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen || (!temOverlay && !temModalFinancas),
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
            notificacoesCount = notificacoesCount,
            chatAberto = chatAberto,
            conversaAtivaId = conversaAtivaId,
            novidadesAberto = novidadesAberto,
            onAbrirMenu = { scope.launch { drawerState.open() } },
            onAbrirNovidades = onAbrirNovidades,
            onOpenChat = onOpenChat,
            onNovaConversa = onNovaConversa,
            onNovaConversaComTexto = onNovaConversaComTexto,
            mensagemInicial = mensagemInicial,
            onMensagemInicialConsumida = { mensagemInicial = null },
            onAbrirEstante = onAbrirEstante,
            onAbrirFinancas = onAbrirFinancas,
            onConversarComLuna = onConversarComLuna,
            onAbrirPerfil = onAbrirPerfil,
            onFecharChat = onFecharChat,
            onFecharNovidades = onFecharNovidades,
            onAbrirConversaFinancas = onAbrirConversaFinancas,
            financasWidgetAberto = financasWidgetAberto,
            onToggleFinancasWidget = {
                if (!financasWidgetAberto) {
                    financasConversaId = ChatRepository.garantirConversaFinancas()
                }
                financasWidgetAberto = !financasWidgetAberto
            },
            onFecharFinancasWidget = { financasWidgetAberto = false },
            financasChatAberto = financasChatAberto,
            financasConversaId = financasConversaId,
            onFecharFinancasChat = { financasChatAberto = false },
            onExpandirFinancasChat = {
                val id = financasConversaId ?: ChatRepository.garantirConversaFinancas()
                financasWidgetAberto = false
                financasChatAberto = false
                conversaAtivaId = id
                chatAberto = true
            },
        )
    }
}

@Composable
private fun ShellConteudo(
    abaAtual: OrbitTab,
    temOverlay: Boolean,
    abaStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    notificacoesCount: Int,
    chatAberto: Boolean,
    conversaAtivaId: String?,
    novidadesAberto: Boolean,
    onAbrirMenu: () -> Unit,
    onAbrirNovidades: () -> Unit,
    onOpenChat: (String) -> Unit,
    onNovaConversa: () -> Unit,
    onNovaConversaComTexto: (String) -> Unit,
    mensagemInicial: String?,
    onMensagemInicialConsumida: () -> Unit,
    onAbrirEstante: () -> Unit,
    onAbrirFinancas: () -> Unit,
    onConversarComLuna: () -> Unit,
    onAbrirPerfil: () -> Unit,
    onFecharChat: () -> Unit,
    onFecharNovidades: () -> Unit,
    onAbrirConversaFinancas: () -> Unit,
    financasWidgetAberto: Boolean,
    onToggleFinancasWidget: () -> Unit,
    onFecharFinancasWidget: () -> Unit,
    financasChatAberto: Boolean,
    financasConversaId: String?,
    onFecharFinancasChat: () -> Unit,
    onExpandirFinancasChat: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(OrbitTokens.graphiteBg)) {
        Column(Modifier.fillMaxSize()) {
            OrbitTopBar(
                onAbrirMenu = onAbrirMenu,
                notificacoesCount = notificacoesCount,
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
                                onAbrirFinancas = onAbrirFinancas,
                                onAbrirEstante = onAbrirEstante,
                                idleAtivo = true,
                            )
                            OrbitTab.FINANCAS -> FinancasDashboardScreen()
                            OrbitTab.CONSTANCIA -> ConstanciaScreen()
                            OrbitTab.EXTRATO -> ExtratoScreen()
                            OrbitTab.CARTOES -> CartoesScreen()
                            OrbitTab.RECORRENTES -> RecorrentesScreen()
                            OrbitTab.TRANSFERENCIA -> TransferenciaScreen()
                            OrbitTab.METAS -> MetasScreen()
                            OrbitTab.CAPTURA -> CapturaScreen()
                            OrbitTab.CONVERSAS -> ConversasScreen(
                                onOpenChat = onOpenChat,
                                onAbrirEstante = onAbrirEstante,
                            )
                            OrbitTab.ESTANTE -> EstanteScreen()
                            OrbitTab.PLANO -> PlanosScreen()
                            OrbitTab.PERFIL -> PerfilScreen(
                                onConversarComLuna = onConversarComLuna,
                                onAbrirConversa = onOpenChat,
                            )
                            OrbitTab.AJUSTES -> AjustesScreen(onAbrirPerfil = onAbrirPerfil)
                        }
                    }
                }

                // FAB da Luna + Widget Flutuante em Finanças
                if (!temOverlay && abaAtual.ehFinancas()) {
                    val id = financasConversaId ?: ChatRepository.garantirConversaFinancas()

                    // Widget Flutuante da Luna (emerge sobre o FAB)
                    FinancasLunaWidget(
                        conversaId = id,
                        visivel = financasWidgetAberto,
                        onFechar = onFecharFinancasWidget,
                        onAbrirFullscreen = onExpandirFinancasChat,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = OrbitMetrics.pagePadding, bottom = 80.dp),
                    )

                    // FAB circular de 54dp no canto inferior esquerdo
                    FinancasLunaFab(
                        ativo = financasChatAberto,
                        onClick = onAbrirConversaFinancas,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = OrbitMetrics.pagePadding, bottom = 20.dp),
                    )
                }
            }
        }

        if (financasChatAberto) {
            val id = financasConversaId ?: ChatRepository.garantirConversaFinancas()
            FinancasLunaChatSheet(
                conversaId = id,
                onDismiss = onFecharFinancasChat,
                onAbrirFullscreen = onExpandirFinancasChat,
            )
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
    notificacoesCount: Int,
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
                tint = if (notificacoesCount > 0) OrbitTokens.textHiN else OrbitTokens.textMidN,
                iconSize = 21.dp,
            )
            if (notificacoesCount > 0) {
                val label = if (notificacoesCount > 99) "99+" else "$notificacoesCount"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-1).dp)
                        .height(16.dp)
                        .defaultMinSize(minWidth = 16.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(OrbitTokens.bluePastel)
                        .padding(horizontal = if (label.length > 1) 5.dp else 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = OrbitTokens.onBluePastel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp,
                    )
                }
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
            DrawerNavItem(Icons.Rounded.Home, "Início", atual == OrbitTab.INICIO) {
                onAba(OrbitTab.INICIO)
            }
            DrawerNavItem(Icons.Rounded.Email, "Conversas", atual == OrbitTab.CONVERSAS) {
                onAba(OrbitTab.CONVERSAS)
            }
            DrawerNavItem(Icons.AutoMirrored.Rounded.MenuBook, "Estante", atual == OrbitTab.ESTANTE) {
                onAba(OrbitTab.ESTANTE)
            }

            val emFinancas = atual.ehFinancas()
            var financasAberta by remember {
                mutableStateOf(PrefsRepository.gavetaFinancasAberta || emFinancas)
            }
            // Se entrou numa tela de Finanças com a seção fechada, abre sozinho.
            LaunchedEffect(emFinancas) {
                if (emFinancas && !financasAberta) {
                    financasAberta = true
                    PrefsRepository.gavetaFinancasAberta = true
                }
            }

            DrawerSecaoColapsavel(
                titulo = "Finanças",
                aberta = financasAberta,
                destacada = emFinancas,
                onToggle = {
                    financasAberta = !financasAberta
                    PrefsRepository.gavetaFinancasAberta = financasAberta
                },
            ) {
                // Subitens: indentados + mais leves — não competem com Início/Conversas/…
                Column(
                    Modifier
                        .padding(start = 10.dp, end = 2.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrbitTokens.graphiteSurf.copy(alpha = 0.55f))
                        .padding(vertical = 4.dp),
                ) {
                    DrawerSubNavItem(
                        Icons.Rounded.AccountBalanceWallet,
                        "Painel",
                        atual == OrbitTab.FINANCAS,
                    ) { onAba(OrbitTab.FINANCAS) }
                    DrawerSubNavItem(
                        Icons.Rounded.AutoAwesome,
                        "Constância",
                        atual == OrbitTab.CONSTANCIA,
                    ) { onAba(OrbitTab.CONSTANCIA) }
                    DrawerSubNavItem(
                        Icons.Rounded.SwapVert,
                        "Movimentações",
                        atual == OrbitTab.EXTRATO,
                    ) { onAba(OrbitTab.EXTRATO) }
                    DrawerSubNavItem(
                        Icons.Rounded.CreditCard,
                        "Cartões",
                        atual == OrbitTab.CARTOES,
                    ) { onAba(OrbitTab.CARTOES) }
                    DrawerSubNavItem(
                        Icons.Rounded.Repeat,
                        "Recorrentes",
                        atual == OrbitTab.RECORRENTES,
                    ) { onAba(OrbitTab.RECORRENTES) }
                    DrawerSubNavItem(
                        Icons.Rounded.SwapHoriz,
                        "Transferência",
                        atual == OrbitTab.TRANSFERENCIA,
                    ) { onAba(OrbitTab.TRANSFERENCIA) }
                    DrawerSubNavItem(
                        Icons.Rounded.Flag,
                        "Metas",
                        atual == OrbitTab.METAS,
                    ) { onAba(OrbitTab.METAS) }
                    DrawerSubNavItem(
                        Icons.Rounded.Hearing,
                        "Captura",
                        atual == OrbitTab.CAPTURA,
                    ) { onAba(OrbitTab.CAPTURA) }
                }
            }

            DrawerNavItem(Icons.Rounded.WorkspacePremium, "Planos", atual == OrbitTab.PLANO) {
                onAba(OrbitTab.PLANO)
            }
            DrawerNavItem(Icons.Rounded.Person, "Perfil", atual == OrbitTab.PERFIL) {
                onAba(OrbitTab.PERFIL)
            }
            DrawerNavItem(Icons.Rounded.Settings, "Ajustes", atual == OrbitTab.AJUSTES) {
                onAba(OrbitTab.AJUSTES)
            }

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

        // Rodapé: medidor da carteira + perfil + pílula Nova conversa
        val usageCarteira by UsageRepository.usage.collectAsState()
        UsageMeter(
            usage = usageCarteira,
            onClick = { onAba(OrbitTab.PLANO) },
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
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

/**
 * Cabeçalho de seção que abre/fecha os filhos — pra Finanças (e futuras seções densas)
 * não engolir a gaveta inteira.
 */
@Composable
private fun DrawerSecaoColapsavel(
    titulo: String,
    aberta: Boolean,
    destacada: Boolean,
    onToggle: () -> Unit,
    conteudo: @Composable () -> Unit,
) {
    val rotacao by animateFloatAsState(
        targetValue = if (aberta) 180f else 0f,
        label = "chevron-financas",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .orbitPressable(onClick = onToggle)
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            titulo.uppercase(),
            color = if (destacada) OrbitTokens.bluePastel else OrbitTokens.textLowN,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = if (aberta) "Recolher $titulo" else "Expandir $titulo",
            tint = if (destacada) OrbitTokens.bluePastel else OrbitTokens.textLowN,
            modifier = Modifier
                .size(18.dp)
                .rotate(rotacao),
        )
    }
    AnimatedVisibility(
        visible = aberta,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column { conteudo() }
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

/** Destino dentro de uma seção (Finanças) — indentado, menor, sem “peso” de aba raiz. */
@Composable
private fun DrawerSubNavItem(
    icone: ImageVector,
    label: String,
    ativo: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (ativo) OrbitTokens.graphiteRaised
                else androidx.compose.ui.graphics.Color.Transparent,
            )
            .orbitPressable(onClick = onClick)
            .padding(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tracinho + ícone menor = hierarquia visual de submenu
        Box(
            Modifier
                .padding(end = 10.dp)
                .width(2.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(
                    if (ativo) OrbitTokens.bluePastel
                    else OrbitTokens.graphiteHair,
                ),
        )
        Icon(
            icone,
            contentDescription = null,
            tint = if (ativo) OrbitTokens.bluePastel else OrbitTokens.textLowN,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = if (ativo) OrbitTokens.textHiN else OrbitTokens.textMidN,
            fontSize = 13.5.sp,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF17181B, widthDp = 380, heightDp = 800)
@Composable
fun OrbitShellPreview() {
    OrbitShell()
}
