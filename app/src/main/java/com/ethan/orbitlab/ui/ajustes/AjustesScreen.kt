package com.ethan.orbitlab.ui.ajustes

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ethan.orbitlab.BuildConfig
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.AuthProvider
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.UserProfileRepository
import com.ethan.orbitlab.data.lunaapi.LunaApiConfig
import com.ethan.orbitlab.data.updates.ApkInstaller
import com.ethan.orbitlab.data.updates.UpdatesRepository
import com.ethan.orbitlab.data.updates.isNewer
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch

private val ContaCardBg = Color(0xFFF2F4F8)

/**
 * Ajustes — alinhado ao SettingsScreen do orbit-mobile
 * (transparência, privacidade real, sessão).
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

    val reasoning by PrefsRepository.reasoningEnabled.collectAsState()
    val vibracao by PrefsRepository.vibracao.collectAsState()
    val lunaDirect by PrefsRepository.lunaDirectEnabled.collectAsState()
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
            containerColor = OrbitTokens.surfaceRaised,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(
                top = 24.dp,
                start = OrbitMetrics.pagePadding,
                end = OrbitMetrics.pagePadding,
                bottom = 120.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(OrbitMetrics.sectionGap),
    ) {
        HeaderAjustes()

        ContaCard(
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

        SecaoAjustes(
            titulo = "Conexão do chat",
            rodape = if (lunaDirect) {
                "OpenRouter direto — latência mínima, sem agente no servidor."
            } else {
                "Railway (${LunaApiConfig.baseUrl.removePrefix("https://")}) — pipeline agentico como no orbit-mobile."
            },
            assinaturaAzul = true,
        ) {
            LinhaSwitch(
                icone = Icons.Rounded.FlashOn,
                titulo = "OpenRouter direto",
                subtitulo = if (lunaDirect) {
                    "Ligação direta ao modelo (lab)"
                } else {
                    "Desligado — usa o servidor Luna no Railway"
                },
                checado = lunaDirect,
                onCheck = { PrefsRepository.setLunaDirectEnabled(it) },
            )
            LinhaAjuste(
                icone = Icons.Rounded.Cloud,
                titulo = "Servidor Luna (Railway)",
                subtitulo = if (lunaDirect) {
                    "Desligue o switch acima pra testar o agente"
                } else {
                    "Ativo — memória, tools e roteamento do core"
                },
                iconeBg = OrbitTokens.surfaceRaised,
                iconeTint = if (lunaDirect) OrbitTokens.textLow else OrbitTokens.accentText,
                clicavel = false,
            )
        }

        SecaoAjustes(
            titulo = "Modo de resposta",
            rodape = "A Luna escolhe o modelo a cada mensagem — leve no papo, profundo quando o assunto pede.",
        ) {
            LinhaAjuste(
                icone = Icons.Rounded.FlashOn,
                titulo = "Automático",
                subtitulo = "A Luna decide o modelo por mensagem",
                iconeBg = OrbitTokens.surfaceRaised,
                iconeTint = OrbitTokens.textMid,
                clicavel = false,
            )
        }

        SecaoAjustes(
            titulo = "Transparência",
            rodape = "Use quando quiser acompanhar como a resposta está sendo construída.",
            assinaturaAzul = true,
        ) {
            LinhaSwitch(
                icone = Icons.Rounded.AutoAwesome,
                titulo = "Mostrar raciocínio",
                subtitulo = "Exibe o processo quando a Luna enviar etapas",
                checado = reasoning,
                onCheck = { PrefsRepository.setReasoningEnabled(it) },
            )
        }

        SecaoAjustes(
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
        }

        SecaoAjustes(
            titulo = "Privacidade e dados",
            rodape = "A Luna é uma companhia, não uma terapeuta — em crise, o CVV atende no 188.",
        ) {
            LinhaAjuste(
                icone = Icons.Rounded.Shield,
                titulo = "Como a Luna trata seus dados",
                subtitulo = "O que fica salvo, a IA de terceiros e seus direitos",
                iconeBg = OrbitTokens.accent.copy(alpha = 0.18f),
                iconeTint = OrbitTokens.accentText,
                onClick = { tela = AjustesTela.Privacidade },
            )
            Divisoria()
            LinhaAjuste(
                icone = Icons.Rounded.DeleteForever,
                titulo = "Apagar meus dados",
                subtitulo = "Apaga conversas deste lab — sem volta",
                danger = true,
                iconeBg = OrbitTokens.danger.copy(alpha = 0.18f),
                iconeTint = OrbitTokens.danger,
                onClick = { tela = AjustesTela.Privacidade },
            )
        }

        SecaoAjustes(titulo = "Sobre") {
            if (updateAvailable) {
                LinhaAjuste(
                    icone = Icons.Rounded.Info,
                    titulo = "Atualização disponível",
                    subtitulo = manifest?.latestVersion?.let { "OrbitLab v$it pronta pra instalar" },
                    trailing = "Instalar",
                    iconeBg = OrbitTokens.accent.copy(alpha = 0.18f),
                    iconeTint = OrbitTokens.accentText,
                    onClick = {
                        val url = manifest?.apkUrl ?: return@LinhaAjuste
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
            LinhaAjuste(
                icone = Icons.Rounded.Info,
                titulo = "Versão",
                trailing = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                clicavel = false,
                iconeBg = OrbitTokens.surfaceRaised,
                iconeTint = OrbitTokens.textMid,
            )
        }

        BotaoSair(onClick = { confirmarSair = true })

        Text(
            "Orbit · Aura",
            color = OrbitTokens.textLow,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_orbit_symbol),
                contentDescription = "Orbit",
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp),
            )
            Text(
                "Ajustes",
                color = OrbitTokens.textHigh,
                fontSize = OrbitMetrics.titleSize,
                fontWeight = OrbitMetrics.titleWeight,
                letterSpacing = (-0.3).sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Conta, transparência e como a Luna trata seus dados.",
            color = OrbitTokens.textMid,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ContaCard(
    nome: String,
    detalhe: String,
    avatarUrl: String?,
    onAbrirPerfil: () -> Unit,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ContaCardBg)
            .orbitPressable(onClick = onAbrirPerfil)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OrbitTokens.ink1),
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
                    tint = ContaCardBg.copy(alpha = 0.55f),
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                nome,
                color = OrbitTokens.ink0,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                detalhe,
                color = OrbitTokens.textLow,
                fontSize = OrbitMetrics.captionSize,
                maxLines = 1,
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OrbitTokens.ink0.copy(alpha = 0.45f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SecaoAjustes(
    titulo: String,
    rodape: String? = null,
    assinaturaAzul: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            titulo,
            color = OrbitTokens.textMid,
            fontSize = OrbitMetrics.captionSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
        )
        val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(OrbitTokens.surface)
                .border(1.dp, OrbitTokens.borderSoft, shape),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
            if (assinaturaAzul) {
                Canvas(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp),
                ) {
                    val path = Path().apply {
                        moveTo(size.width, size.height)
                        lineTo(size.width, size.height * 0.18f)
                        lineTo(size.width * 0.18f, size.height)
                        close()
                    }
                    drawPath(path, color = OrbitTokens.accent)
                }
            }
        }
        if (!rodape.isNullOrBlank()) {
            Text(
                rodape,
                color = OrbitTokens.textLow,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun LinhaAjuste(
    icone: ImageVector,
    titulo: String,
    subtitulo: String? = null,
    trailing: String? = null,
    iconeBg: Color,
    iconeTint: Color,
    danger: Boolean = false,
    clicavel: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clicavel) Modifier.orbitPressable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconeTile(icone, iconeBg, iconeTint)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                color = if (danger) OrbitTokens.danger else OrbitTokens.textHigh,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitulo != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitulo,
                    color = OrbitTokens.textLow,
                    fontSize = OrbitMetrics.captionSize,
                    lineHeight = 16.sp,
                )
            }
        }
        if (trailing != null) {
            Text(
                trailing,
                color = OrbitTokens.textMid,
                fontSize = OrbitMetrics.captionSize,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (clicavel) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (danger) OrbitTokens.danger.copy(alpha = 0.7f) else OrbitTokens.textLow,
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
            .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconeTile(
            icone = icone,
            bg = OrbitTokens.surfaceRaised,
            tint = OrbitTokens.textMid,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                color = OrbitTokens.textHigh,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitulo != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitulo,
                    color = OrbitTokens.textLow,
                    fontSize = OrbitMetrics.captionSize,
                    lineHeight = 16.sp,
                )
            }
        }
        Switch(
            checked = checado,
            onCheckedChange = onCheck,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OrbitTokens.accent,
                checkedBorderColor = OrbitTokens.accent,
                uncheckedThumbColor = OrbitTokens.textLow,
                uncheckedTrackColor = OrbitTokens.surfaceHover,
                uncheckedBorderColor = OrbitTokens.border,
            ),
        )
    }
}

@Composable
private fun IconeTile(
    icone: ImageVector,
    bg: Color,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(OrbitMetrics.iconRow)
            .clip(RoundedCornerShape(OrbitMetrics.radiusIcon))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icone, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun Divisoria() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 62.dp, end = 14.dp)
            .height(1.dp)
            .background(OrbitTokens.borderSoft),
    )
}

@Composable
private fun BotaoSair(onClick: () -> Unit) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.danger.copy(alpha = 0.35f), shape)
            .orbitPressable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Logout,
            contentDescription = null,
            tint = OrbitTokens.danger,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Sair da conta",
            color = OrbitTokens.danger,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 900)
@Composable
fun AjustesScreenPreview() {
    AjustesScreen()
}
