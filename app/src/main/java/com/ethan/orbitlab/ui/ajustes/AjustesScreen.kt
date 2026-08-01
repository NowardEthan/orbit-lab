package com.ethan.orbitlab.ui.ajustes

import android.Manifest
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ethan.orbitlab.BuildConfig
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.AuthDiag
import com.ethan.orbitlab.data.AuthProvider
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.data.local.LocationRepository
import com.ethan.orbitlab.data.updates.ApkInstaller
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.data.updates.isNewer
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch

/**
 * Ajustes — releitura minimalista (redesign 1.0).
 * Sem cartões emoldurados nem ladrilhos: grupos nus sobre o grafite,
 * separados por fios finos e ar. O azul pastel só pinga onde importa.
 */
@Composable
fun AjustesScreen(
    onAbrirPerfil: () -> Unit = {},
) {
    var tela by remember { mutableStateOf(AjustesTela.Lista) }
    var confirmarSair by remember { mutableStateOf(false) }
    val session by AuthRepository.session.collectAsState()
    val profile by UserProfileRepository.profile.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    when (tela) {
        AjustesTela.Privacidade -> {
            PrivacidadeScreen(onBack = { tela = AjustesTela.Lista })
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
    val diagAgora by AuthDiag.agora.collectAsState()
    val clipboard = LocalClipboardManager.current
    var diarioCopiado by remember { mutableStateOf(false) }
    val diarioSessao = remember(diagAgora) {
        buildString {
            if (diagAgora.isNotBlank()) append("ABERTURA DE AGORA\n").append(diagAgora)
            val antes = AuthDiag.anterior
            if (antes.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("ABERTURA ANTERIOR\n").append(antes)
            }
        }
    }
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
                Text(
                    "Você volta pra tela de login. As conversas na nuvem continuam salvas nesta Conta Aura.",
                )
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
        verticalArrangement = Arrangement.spacedBy(30.dp),
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

        Grupo(
            titulo = "Modo de resposta",
            rodape = "A Luna escolhe o modelo a cada mensagem — leve no papo, profundo quando o assunto pede. O raciocínio dela fica sempre à mostra.",
        ) {
            Linha(
                icone = Icons.Rounded.FlashOn,
                titulo = "Automático",
                subtitulo = "A Luna decide o modelo por mensagem",
                clicavel = false,
            )
        }

        Grupo(
            titulo = "Preferências",
            rodape = "Só neste aparelho — o lab ainda não sincroniza prefs.",
        ) {
            LinhaSwitch(
                icone = Icons.Rounded.Vibration,
                titulo = "Vibração",
                subtitulo = "Feedback háptico em gestos (long press, etc.)",
                checado = vibracao,
                onCheck = { PrefsRepository.setVibracao(it) },
            )
            Divisoria()
            LinhaSwitch(
                icone = Icons.Rounded.TravelExplore,
                titulo = "Pesquisa profunda",
                subtitulo = "A Luna cruza as fontes antes de responder — mais fiel, um pouco mais lento",
                checado = pesquisaProfunda,
                onCheck = { PrefsRepository.setPesquisaProfunda(it) },
            )
            Divisoria()
            LinhaSwitch(
                icone = Icons.Rounded.LocationOn,
                titulo = "Localização e clima",
                subtitulo = "Dá à Luna o «onde» e o tempo agora — e mostra o clima no Início",
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
        }

        Grupo(
            titulo = "Privacidade e dados",
            rodape = "A Luna é uma companhia, não uma terapeuta — em crise, o CVV atende no 188.",
        ) {
            Linha(
                icone = Icons.Rounded.Shield,
                titulo = "Como a Luna trata seus dados",
                subtitulo = "O que fica salvo, a IA de terceiros e seus direitos",
                iconeTint = OrbitTokens.bluePastel,
                onClick = { tela = AjustesTela.Privacidade },
            )
            Divisoria()
            Linha(
                icone = Icons.Rounded.DeleteForever,
                titulo = "Apagar meus dados",
                subtitulo = "Apaga conversas deste lab — sem volta",
                danger = true,
                onClick = { tela = AjustesTela.Privacidade },
            )
        }

        Grupo(titulo = "Sobre") {
            if (updateAvailable) {
                Linha(
                    icone = Icons.Rounded.Info,
                    titulo = "Atualização disponível",
                    subtitulo = manifest?.latestVersion?.let { "OrbitLab v$it pronta pra instalar" },
                    trailing = "Instalar",
                    iconeTint = OrbitTokens.bluePastel,
                    onClick = {
                        val url = manifest?.apkUrl ?: return@Linha
                        scope.launch {
                            try {
                                ApkInstaller.downloadAndInstall(context, url) {}
                            } catch (_: Exception) {
                                ApkInstaller.openInBrowser(context, url)
                            }
                        }
                    },
                )
                Divisoria()
            }
            Linha(
                icone = Icons.Rounded.Info,
                titulo = "Versão",
                trailing = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                clicavel = false,
            )
            // Diário de bordo da abertura (ver AuthDiag). Fica aqui porque quando a
            // reentrada dá certo ele nunca chega à tela de login, onde o diário também aparece.
            if (diarioSessao.isNotBlank()) {
                Divisoria()
                Linha(
                    icone = Icons.Rounded.Info,
                    titulo = "Diário da sessão",
                    subtitulo = if (diarioCopiado) "Copiado — cola aqui pro Claude" else "Por que a abertura demorou / pediu login",
                    trailing = if (diarioCopiado) "Ok" else "Copiar",
                    onClick = {
                        clipboard.setText(AnnotatedString(diarioSessao))
                        diarioCopiado = true
                    },
                )
            }
        }

        LinhaSair(onClick = { confirmarSair = true })

        Text(
            "Orbit · Aura",
            color = OrbitTokens.textLowN,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}

private enum class AjustesTela { Lista, Privacidade }

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
        Spacer(Modifier.height(6.dp))
        Text(
            "Conta, transparência e como a Luna trata seus dados.",
            color = OrbitTokens.textMidN,
            fontSize = 13.sp,
            lineHeight = 18.sp,
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
                .size(46.dp)
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
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                nome,
                color = OrbitTokens.textHiN,
                fontSize = 16.sp,
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
            modifier = Modifier.size(22.dp),
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
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = if (danger) OrbitTokens.danger else iconeTint,
            modifier = Modifier.size(20.dp),
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = OrbitTokens.textMidN,
            modifier = Modifier.size(20.dp),
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
            .padding(start = 34.dp)
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Logout,
            contentDescription = null,
            tint = OrbitTokens.danger,
            modifier = Modifier.size(20.dp),
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
