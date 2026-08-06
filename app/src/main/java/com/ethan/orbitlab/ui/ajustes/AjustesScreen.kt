package com.ethan.orbitlab.ui.ajustes

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.ethan.orbitlab.BuildConfig
import com.ethan.orbitlab.data.AuthProvider
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.data.crash.CrashReporting
import com.ethan.orbitlab.data.lunaapi.LunaApiClient
import com.ethan.orbitlab.data.local.LocationRepository
import com.ethan.orbitlab.data.updates.ApkInstaller
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.data.updates.isNewer
import com.ethan.orbitlab.ui.bolha.BolhaLunaService
import com.ethan.orbitlab.ui.bolha.OverlayPermissao
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ajustes — design minimalista, compacto e elegante (redesign 1.0).
 * Organizado por preferências essenciais, sem poluidores ou botões de dev.
 */
@Composable
fun AjustesScreen(
    onAbrirPerfil: () -> Unit = {},
) {
    var tela by remember { mutableStateOf(AjustesTela.Lista) }
    var confirmarSair by remember { mutableStateOf(false) }
    var confirmarCrashTeste by remember { mutableStateOf(false) }
    var toquesVersao by remember { mutableStateOf(0) }
    val session by AuthRepository.session.collectAsState()
    val profile by UserProfileRepository.profile.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    when (tela) {
        AjustesTela.Privacidade -> {
            PrivacidadeScreen(onBack = { tela = AjustesTela.Lista })
            return
        }
        AjustesTela.ClaudePessoal -> {
            ClaudePessoalScreen(onBack = { tela = AjustesTela.Lista })
            return
        }
        AjustesTela.Lista -> Unit
    }

    val vibracao by PrefsRepository.vibracao.collectAsState()
    val pesquisaProfunda by PrefsRepository.pesquisaProfunda.collectAsState()
    val localizacaoAtiva by PrefsRepository.localizacaoAtiva.collectAsState()
    val permLocalizacao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { concessoes ->
        if (concessoes.values.any { it }) {
            PrefsRepository.setLocalizacaoAtiva(true)
            LocationRepository.atualizarEmBackground(context, forcar = true)
        }
    }

    val bolhaAtiva by PrefsRepository.bolhaAtiva.collectAsState()
    val bolhaRodando by BolhaLunaService.rodando.collectAsState()
    val bolhaPainel by BolhaLunaService.painelAberto.collectAsState()
    var onboardingBolha by remember { mutableStateOf(false) }
    // Em Android 13+ a notificação fixa do serviço precisa de permissão; a bolha liga de qualquer jeito.
    val permNotificacao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    // Voltou da tela "desenhar sobre outros apps"? Se concedeu, sobe a bolha sozinha.
    var aguardandoOverlay by remember { mutableStateOf(false) }

    fun ligarBolhaAgora() {
        if (OverlayPermissao.concedida(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            BolhaLunaService.ligar(context)
        } else {
            PrefsRepository.setBolhaAtiva(true)
            aguardandoOverlay = true
            OverlayPermissao.abrirAjustes(context)
        }
    }

    val subtituloBolha = when {
        !bolhaAtiva -> "Só em segundo plano · toque = resposta rápida · arraste pra baixo pra guardar"
        !OverlayPermissao.concedida(context) -> "Ativa nas prefs · falta permissão de overlay"
        bolhaPainel -> "Ativa · painel aberto"
        bolhaRodando -> "Ativa · oculta enquanto o app está aberto"
        else -> "Ativa · religando…"
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            when {
                aguardandoOverlay && OverlayPermissao.concedida(context) -> {
                    aguardandoOverlay = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    BolhaLunaService.ligar(context)
                }
                aguardandoOverlay && !OverlayPermissao.concedida(context) -> {
                    // Voltou dos ajustes sem conceder — um breadcrumb por tentativa.
                    aguardandoOverlay = false
                    CrashReporting.breadcrumb("bolha_permission_denied")
                }
                PrefsRepository.bolhaAtiva.value -> {
                    BolhaLunaService.tentarReligarSePreferida(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    val manifest by UpdatesRepository.manifest.collectAsState()
    val updateAvailable = remember(manifest) {
        val m = manifest ?: return@remember false
        val latestCode = m.latestVersionCode
        if (latestCode != null) latestCode > BuildConfig.VERSION_CODE
        else isNewer(m.latestVersion, BuildConfig.VERSION_NAME)
    }

    if (confirmarSair) {
        AlertDialog(
            onDismissRequest = { confirmarSair = false },
            title = { Text("Sair da conta?") },
            text = {
                Text("Sua sessão será encerrada. Suas conversas permanecem salvas na nuvem.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarSair = false
                        scope.launch { AuthRepository.sair() }
                    },
                ) {
                    Text("Sair", color = OrbitTokens.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSair = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = OrbitTokens.graphiteRaised,
        )
    }

    if (confirmarCrashTeste) {
        AlertDialog(
            onDismissRequest = { confirmarCrashTeste = false },
            title = { Text("Crash de teste?") },
            text = {
                Text(
                    "O app vai fechar de propósito pra validar o Crashlytics no Firebase. " +
                        "Use só pra diagnóstico — não manda dado da conversa.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarCrashTeste = false
                        CrashReporting.forcarCrashDeTeste()
                    },
                ) {
                    Text("Forçar crash", color = OrbitTokens.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarCrashTeste = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = OrbitTokens.graphiteRaised,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(
                top = 6.dp,
                start = OrbitMetrics.pagePadding,
                end = OrbitMetrics.pagePadding,
                bottom = 120.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        HeaderAjustes()

        ContaLinha(
            nome = profile.displayName.ifBlank { session?.displayName ?: "Conta" },
            detalhe = buildString {
                val handle = profile.username.takeIf { it.isNotBlank() }?.let { "@$it" }
                val provedor = when (session?.provider) {
                    AuthProvider.AURA -> "Conta Aura"
                    AuthProvider.GOOGLE -> "Google"
                    null -> null
                }
                append(listOfNotNull(handle, provedor).joinToString(" · ").ifBlank { "Ver perfil" })
            },
            avatarUrl = profile.avatarUrl,
            onAbrirPerfil = onAbrirPerfil,
        )

        if (updateAvailable) {
            val pct = (progress * 100).toInt()
            Grupo(titulo = "Atualização") {
                Linha(
                    icone = Icons.Rounded.Info,
                    titulo = if (downloading) "Baixando atualização…" else "Nova versão disponível",
                    subtitulo = when {
                        downloading -> "Instalando em instantes ($pct%)"
                        manifest?.latestVersion != null -> "OrbitLab v${manifest?.latestVersion} pronta pra instalar"
                        else -> "Toque para baixar e instalar"
                    },
                    trailing = if (downloading) "$pct%" else "Instalar",
                    iconeTint = OrbitTokens.bluePastel,
                    clicavel = !downloading,
                    onClick = {
                        val url = manifest?.apkUrl ?: return@Linha
                        if (downloading) return@Linha
                        downloading = true
                        progress = 0f
                        scope.launch {
                            try {
                                ApkInstaller.downloadAndInstall(context, url) { p ->
                                    progress = p
                                }
                            } catch (_: Exception) {
                                ApkInstaller.openInBrowser(context, url)
                            } finally {
                                downloading = false
                                progress = 0f
                            }
                        }
                    },
                )
            }
        }

        Grupo(titulo = "Preferências") {
            LinhaSwitch(
                icone = Icons.Rounded.Vibration,
                titulo = "Vibração",
                subtitulo = "Feedback háptico nos gestos",
                checado = vibracao,
                onCheck = { PrefsRepository.setVibracao(it) },
            )
            Divisoria()
            LinhaSwitch(
                icone = Icons.Rounded.TravelExplore,
                titulo = "Pesquisa profunda",
                subtitulo = "Luna cruza fontes na web para respostas mais precisas",
                checado = pesquisaProfunda,
                onCheck = { PrefsRepository.setPesquisaProfunda(it) },
            )
            Divisoria()
            LinhaSwitch(
                icone = Icons.Rounded.LocationOn,
                titulo = "Localização e clima",
                subtitulo = "Compartilha contexto de local e mostra o clima no Início",
                checado = localizacaoAtiva,
                onCheck = { ligar ->
                    if (ligar) {
                        if (LocationRepository.temPermissao(context)) {
                            PrefsRepository.setLocalizacaoAtiva(true)
                            LocationRepository.atualizarEmBackground(context, forcar = true)
                        } else {
                            permLocalizacao.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        }
                    } else {
                        PrefsRepository.setLocalizacaoAtiva(false)
                        LocationRepository.limpar()
                    }
                },
            )
            Divisoria()
            LinhaSwitch(
                icone = Icons.Rounded.Nightlight,
                titulo = "Bolha da Luna",
                subtitulo = subtituloBolha,
                checado = bolhaAtiva,
                onCheck = { ligar ->
                    if (ligar) {
                        if (!PrefsRepository.bolhaOnboardingVisto) {
                            onboardingBolha = true
                        } else {
                            ligarBolhaAgora()
                        }
                    } else {
                        aguardandoOverlay = false
                        BolhaLunaService.desligar(context)
                    }
                },
            )
            if (bolhaAtiva) {
                Text(
                    "Se a bolha sumir em alguns Androids: Ajustes do sistema → apps → OrbitLab → " +
                        "bateria sem restrição (e autostart, se existir).",
                    color = OrbitTokens.textLowN,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (onboardingBolha) {
            AlertDialog(
                onDismissRequest = { onboardingBolha = false },
                title = { Text("Bolha da Luna", fontWeight = FontWeight.SemiBold) },
                text = {
                    Text(
                        "A Luna flutua sobre outros apps só quando o OrbitLab está em segundo plano.\n\n" +
                            "• Toque → resposta rápida\n" +
                            "• Toque longo → painel completo\n" +
                            "• Arraste pra baixo → guardar\n\n" +
                            "O pontinho azul avisa resposta nova — sem mostrar o texto.",
                        color = OrbitTokens.textMidN,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            PrefsRepository.setBolhaOnboardingVisto(true)
                            onboardingBolha = false
                            ligarBolhaAgora()
                        },
                    ) { Text("Entendi") }
                },
                dismissButton = {
                    TextButton(onClick = { onboardingBolha = false }) {
                        Text("Agora não")
                    }
                },
            )
        }

        val llmPessoalAtivo by PrefsRepository.llmPessoalAtivo.collectAsState()
        val llmPessoalModel by PrefsRepository.llmPessoalModel.collectAsState()

        Grupo(titulo = "Modelo pessoal") {
            Linha(
                icone = Icons.Rounded.SmartToy,
                titulo = "Claude pessoal",
                subtitulo = if (PrefsRepository.llmPessoalConfigurado()) {
                    llmPessoalModel
                } else {
                    "Base URL, chave e modelo"
                },
                trailing = if (llmPessoalAtivo && PrefsRepository.llmPessoalConfigurado()) "Ligado" else "Standby",
                iconeTint = OrbitTokens.bluePastel,
                onClick = { tela = AjustesTela.ClaudePessoal },
            )
        }

        Grupo(titulo = "Privacidade e dados") {
            Linha(
                icone = Icons.Rounded.Shield,
                titulo = "Como a Luna trata seus dados",
                subtitulo = "Privacidade, modelo e retenção de conversas",
                iconeTint = OrbitTokens.bluePastel,
                onClick = { tela = AjustesTela.Privacidade },
            )
            Divisoria()
            Linha(
                icone = Icons.Rounded.DeleteForever,
                titulo = "Apagar meus dados",
                subtitulo = "Exclui o histórico de conversas neste dispositivo",
                danger = true,
                onClick = { tela = AjustesTela.Privacidade },
            )
        }

        Grupo(titulo = "Sobre") {
            Linha(
                icone = Icons.Rounded.Info,
                titulo = "Versão do OrbitLab",
                trailing = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                // 7 toques → confirma crash de teste do Crashlytics (Fase 3).
                clicavel = true,
                onClick = {
                    toquesVersao += 1
                    if (toquesVersao >= 7) {
                        toquesVersao = 0
                        confirmarCrashTeste = true
                    } else {
                        scope.launch {
                            delay(2_000)
                            if (toquesVersao < 7) toquesVersao = 0
                        }
                    }
                },
            )
        }

        LinhaSair(onClick = { confirmarSair = true })

        Text(
            "Orbit · Aura OS",
            color = OrbitTokens.textLowN,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

private enum class AjustesTela { Lista, Privacidade, ClaudePessoal }

@Composable
private fun ClaudePessoalScreen(
    onBack: () -> Unit,
) {
    val ativo by PrefsRepository.llmPessoalAtivo.collectAsState()
    val baseSalva by PrefsRepository.llmPessoalBaseUrl.collectAsState()
    val keySalva by PrefsRepository.llmPessoalApiKey.collectAsState()
    val modeloSalvo by PrefsRepository.llmPessoalModel.collectAsState()

    var baseUrl by remember(baseSalva) { mutableStateOf(baseSalva) }
    var apiKey by remember(keySalva) { mutableStateOf(keySalva) }
    var modelo by remember(modeloSalvo) { mutableStateOf(modeloSalvo.ifBlank { "claude-fable-5" }) }
    var mostrarChave by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    var testando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun configurado(): Boolean =
        baseUrl.trim().startsWith("http") &&
            apiKey.trim().isNotBlank() &&
            modelo.trim().isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(
                top = 6.dp,
                start = OrbitMetrics.pagePadding,
                end = OrbitMetrics.pagePadding,
                bottom = 120.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Voltar",
                    tint = OrbitTokens.textHiN,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Claude pessoal",
                    color = OrbitTokens.textHiN,
                    fontSize = OrbitMetrics.titleSize,
                    fontWeight = OrbitMetrics.titleWeight,
                    letterSpacing = (-0.3).sp,
                )
                Text(
                    "Chave local para a sua conta.",
                    color = OrbitTokens.textMidN,
                    fontSize = OrbitMetrics.captionSize,
                    lineHeight = 16.sp,
                )
            }
        }

        Grupo(
            titulo = "Ativação",
            rodape = "Texto e ferramentas usam essa API. Voz, visão por vídeo/frame e imagens geradas ficam nos provedores do Orbit.",
        ) {
            LinhaSwitch(
                icone = Icons.Rounded.SmartToy,
                titulo = "Usar Claude",
                subtitulo = if (ativo) "Conversas passam pelo modelo pessoal" else "Orbit usa o provedor padrão",
                checado = ativo,
                onCheck = { ligado ->
                    if (ligado && !configurado()) {
                        aviso = "Preencha Base URL, API key e modelo antes de ligar."
                        PrefsRepository.setLlmPessoalAtivo(false)
                    } else {
                        if (ligado) PrefsRepository.salvarLlmPessoal(baseUrl, apiKey, modelo)
                        PrefsRepository.setLlmPessoalAtivo(ligado)
                        aviso = if (ligado) "Ativado." else "Desativado."
                    }
                },
            )
        }

        Grupo(titulo = "Conexão") {
            CampoClaude(
                icone = Icons.Rounded.Link,
                label = "Base URL",
                value = baseUrl,
                onValueChange = { baseUrl = it },
                placeholder = "https://api.exemplo.com/v1",
                keyboardType = KeyboardType.Uri,
            )
            Spacer(Modifier.height(12.dp))
            CampoClaude(
                icone = Icons.Rounded.Key,
                label = "API key",
                value = apiKey,
                onValueChange = { apiKey = it },
                placeholder = "sk-...",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (mostrarChave) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { mostrarChave = !mostrarChave }) {
                        Icon(
                            if (mostrarChave) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (mostrarChave) "Ocultar chave" else "Mostrar chave",
                            tint = OrbitTokens.textLowN,
                        )
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            CampoClaude(
                icone = Icons.Rounded.SmartToy,
                label = "Modelo",
                value = modelo,
                onValueChange = { modelo = it },
                placeholder = "claude-fable-5",
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    if (!configurado()) {
                        PrefsRepository.setLlmPessoalAtivo(false)
                        aviso = "Preencha Base URL, API key e modelo."
                    } else {
                        PrefsRepository.salvarLlmPessoal(baseUrl, apiKey, modelo)
                        aviso = "Salvo."
                    }
                },
                enabled = baseUrl.isNotBlank() || apiKey.isNotBlank() || modelo.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrbitTokens.bluePastel,
                    contentColor = OrbitTokens.onBluePastel,
                    disabledContainerColor = OrbitTokens.graphiteRaised,
                    disabledContentColor = OrbitTokens.textLowN,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Salvar")
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    if (!configurado()) {
                        PrefsRepository.setLlmPessoalAtivo(false)
                        aviso = "Preencha Base URL, API key e modelo antes de testar."
                        return@Button
                    }
                    PrefsRepository.salvarLlmPessoal(baseUrl, apiKey, modelo)
                    testando = true
                    aviso = "Testando conexao..."
                    scope.launch {
                        val token = AuthRepository.getIdToken()
                        val resultado = LunaApiClient.testarProviderPessoal(
                            token,
                            baseUrl,
                            apiKey,
                            modelo,
                        )
                        aviso = if (resultado.ok) {
                            val ms = resultado.latencyMs?.let { " em ${it}ms" }.orEmpty()
                            val model = resultado.model.ifBlank { modelo.trim() }
                            "Conexao OK com $model$ms."
                        } else {
                            resultado.message
                        }
                        testando = false
                    }
                },
                enabled = !testando && configurado(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrbitTokens.graphiteRaised,
                    contentColor = OrbitTokens.textHiN,
                    disabledContainerColor = OrbitTokens.graphiteRaised,
                    disabledContentColor = OrbitTokens.textLowN,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (testando) "Testando..." else "Testar conexao")
            }
            if (aviso.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    aviso,
                    color = OrbitTokens.textMidN,
                    fontSize = OrbitMetrics.captionSize,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun CampoClaude(
    icone: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                icone,
                contentDescription = null,
                tint = OrbitTokens.textLowN,
            )
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrbitTokens.bluePastel,
            unfocusedBorderColor = OrbitTokens.graphiteHair,
            focusedTextColor = OrbitTokens.textHiN,
            unfocusedTextColor = OrbitTokens.textHiN,
            focusedLabelColor = OrbitTokens.bluePastel,
            unfocusedLabelColor = OrbitTokens.textLowN,
            cursorColor = OrbitTokens.bluePastel,
            focusedContainerColor = OrbitTokens.graphiteSurf,
            unfocusedContainerColor = OrbitTokens.graphiteSurf,
        ),
    )
}

@Composable
private fun HeaderAjustes() {
    Column {
        Text(
            "Ajustes",
            color = OrbitTokens.textHiN,
            fontSize = OrbitMetrics.titleSize,
            fontWeight = OrbitMetrics.titleWeight,
            letterSpacing = (-0.3).sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Preferências do app e segurança da sua conta.",
            color = OrbitTokens.textMidN,
            fontSize = OrbitMetrics.captionSize,
            lineHeight = 16.sp,
        )
    }
}

/** Conta no topo — linha nua, sem cartão. O avatar carrega a presença. */
@Composable
private fun ContaLinha(
    nome: String,
    detalhe: String,
    avatarUrl: String?,
    onAbrirPerfil: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .orbitPressable(onClick = onAbrirPerfil)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
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
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = OrbitTokens.textMidN,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                nome,
                color = OrbitTokens.textHiN,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                detalhe,
                color = OrbitTokens.textMidN,
                fontSize = OrbitMetrics.captionSize,
                maxLines = 1,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Grupo nu: rótulo quieto + linhas direto no grafite, sem moldura. */
@Composable
private fun Grupo(
    titulo: String,
    rodape: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            titulo.uppercase(),
            color = OrbitTokens.textLowN,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
        if (!rodape.isNullOrBlank()) {
            Text(
                rodape,
                color = OrbitTokens.textLowN,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun Linha(
    icone: ImageVector,
    titulo: String,
    subtitulo: String? = null,
    trailing: String? = null,
    iconeTint: Color = OrbitTokens.textMidN,
    danger: Boolean = false,
    clicavel: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clicavel) Modifier.orbitPressable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = if (danger) OrbitTokens.danger else iconeTint,
            modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                color = if (danger) OrbitTokens.danger else OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitulo != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitulo,
                    color = OrbitTokens.textLowN,
                    fontSize = OrbitMetrics.captionSize,
                    lineHeight = 16.sp,
                )
            }
        }
        if (trailing != null) {
            Text(
                trailing,
                color = OrbitTokens.textMidN,
                fontSize = OrbitMetrics.captionSize,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (clicavel) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (danger) OrbitTokens.danger.copy(alpha = 0.7f) else OrbitTokens.textLowN,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun LinhaSwitch(
    icone: ImageVector,
    titulo: String,
    subtitulo: String? = null,
    checado: Boolean,
    onCheck: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = OrbitTokens.textMidN,
            modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                color = OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitulo != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitulo,
                    color = OrbitTokens.textLowN,
                    fontSize = OrbitMetrics.captionSize,
                    lineHeight = 16.sp,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checado,
            onCheckedChange = onCheck,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OrbitTokens.onBluePastel,
                checkedTrackColor = OrbitTokens.bluePastel,
                checkedBorderColor = OrbitTokens.bluePastel,
                uncheckedThumbColor = OrbitTokens.textLowN,
                uncheckedTrackColor = OrbitTokens.graphiteRaised,
                uncheckedBorderColor = OrbitTokens.graphiteHair,
            ),
        )
    }
}

/** Fio fino entre linhas do mesmo grupo, alinhado sob o texto (larga do ícone). */
@Composable
private fun Divisoria() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 33.dp)
            .height(1.dp)
            .background(OrbitTokens.graphiteHair),
    )
}

/** Sair — linha de texto vermelha, sem caixa (semântica destrutiva quieta). */
@Composable
private fun LinhaSair(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .orbitPressable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Logout,
            contentDescription = null,
            tint = OrbitTokens.danger,
            modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            "Sair da conta",
            color = OrbitTokens.danger,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF17181B, widthDp = 380, heightDp = 900)
@Composable
fun AjustesScreenPreview() {
    AjustesScreen()
}
