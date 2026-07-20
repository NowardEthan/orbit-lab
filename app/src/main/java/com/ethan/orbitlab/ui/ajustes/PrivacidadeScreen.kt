package com.ethan.orbitlab.ui.ajustes

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.legal.LegalDocuments
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch

/**
 * Privacidade e dados — espelho do PrivacyScreen do mobile.
 * «Apagar» soft-delete conversas na nuvem, limpa prefs e encerra a sessão.
 */
@Composable
fun PrivacidadeScreen(
    onBack: () -> Unit,
    onDadosApagados: () -> Unit = {},
) {
    var docPolitica by remember { mutableStateOf(false) }
    var docTermos by remember { mutableStateOf(false) }
    var confirmPasso by remember { mutableStateOf(0) } // 0 off, 1 primeiro, 2 segundo
    var apagando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    when {
        docPolitica -> {
            LegalDocScreen(
                titulo = "Política de Privacidade",
                markdown = LegalDocuments.politicaDePrivacidade,
                onBack = { docPolitica = false },
            )
            return
        }
        docTermos -> {
            LegalDocScreen(
                titulo = "Termos de Uso",
                markdown = LegalDocuments.termosDeUso,
                onBack = { docTermos = false },
            )
            return
        }
    }

    BackHandler(onBack = onBack)

    if (confirmPasso == 1) {
        AlertDialog(
            onDismissRequest = { confirmPasso = 0 },
            title = { Text("Apagar seus dados?") },
            text = {
                Text(
                    "As conversas vão pra lixeira na nuvem (soft delete), as preferências deste aparelho " +
                        "são limpas e você sai da Conta Aura. Não dá pra desfazer daqui.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmPasso = 2 }) {
                    Text("Continuar", color = OrbitTokens.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmPasso = 0 }) {
                    Text("Cancelar")
                }
            },
            containerColor = OrbitTokens.surfaceRaised,
        )
    }
    if (confirmPasso == 2) {
        AlertDialog(
            onDismissRequest = { if (!apagando) confirmPasso = 0 },
            title = { Text("Tem certeza absoluta?") },
            text = { Text("Última confirmação. Ao tocar em «Apagar tudo», não há volta.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (apagando) return@TextButton
                        apagando = true
                        scope.launch {
                            ChatRepository.apagarTodosOsDados()
                            PrefsRepository.reset()
                            AuthRepository.apagarTudo()
                            apagando = false
                            confirmPasso = 0
                            onDadosApagados()
                            onBack()
                        }
                    },
                ) {
                    Text("Apagar tudo", color = OrbitTokens.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!apagando) confirmPasso = 0 }) {
                    Text("Cancelar")
                }
            },
            containerColor = OrbitTokens.surfaceRaised,
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(OrbitTokens.ink1)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = OrbitMetrics.pagePadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.surface)
                    .border(1.dp, OrbitTokens.borderSoft, CircleShape)
                    .orbitPressable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Voltar",
                    tint = OrbitTokens.textHigh,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Privacidade e dados",
                color = OrbitTokens.textHigh,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "O trato é simples: seus dados são seus. Aqui está, sem enrolação, o que acontece com " +
                    "eles enquanto você conversa com a Luna.",
                color = OrbitTokens.textMid,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            CartaoPrivacidade(
                icon = Icons.Rounded.Lock,
                cor = OrbitTokens.accent,
                titulo = "O que fica salvo",
            ) {
                Text(
                        "Neste lab, as conversas e a Conta Aura ficam só neste aparelho. " +
                        "No Orbit de verdade, conversas, rotina e o que a Luna aprende sobre você " +
                        "ficam na sua conta (Firebase). Nada é vendido nem vira anúncio.",
                    color = OrbitTokens.textMid,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }

            CartaoPrivacidade(
                icon = Icons.Rounded.Memory,
                cor = OrbitTokens.gold,
                titulo = "Passa por uma IA",
            ) {
                Text(
                    "Pra Luna pensar e responder, o texto das suas mensagens é enviado a provedores " +
                        "de inteligência artificial. Eles processam a mensagem naquele instante pra " +
                        "gerar a resposta. Sem isso, ela não fala.",
                    color = OrbitTokens.textMid,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }

            CartaoPrivacidade(
                icon = Icons.Outlined.FavoriteBorder,
                cor = OrbitTokens.danger,
                titulo = "A Luna não é terapeuta",
            ) {
                Text(
                    "A Luna é uma companhia — ela acolhe, escuta e organiza, mas não é profissional " +
                        "de saúde. Ela não substitui psicólogo, psiquiatra ou médico.",
                    color = OrbitTokens.textMid,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Se você estiver passando por um momento difícil ou de crise, procure ajuda de " +
                        "gente de verdade. No Brasil, o CVV atende de graça, 24h, no 188 (ou em cvv.org.br).",
                    color = OrbitTokens.textMid,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }

            val docsShape = RoundedCornerShape(OrbitMetrics.radiusCard)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(docsShape)
                    .background(OrbitTokens.surface)
                    .border(1.dp, OrbitTokens.borderSoft, docsShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                DocLinkRow(
                    icon = DocPoliticaIcon,
                    label = "Política de Privacidade",
                    onClick = { docPolitica = true },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp)
                        .height(1.dp)
                        .background(OrbitTokens.borderSoft),
                )
                DocLinkRow(
                    icon = DocTermosIcon,
                    label = "Termos de Uso",
                    onClick = { docTermos = true },
                )
            }

            val zonaShape = RoundedCornerShape(OrbitMetrics.radiusCard)
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(zonaShape)
                    .background(OrbitTokens.danger.copy(alpha = 0.08f))
                    .border(1.dp, OrbitTokens.danger.copy(alpha = 0.28f), zonaShape)
                    .padding(16.dp),
            ) {
                Text(
                    "Apagar meus dados",
                    color = OrbitTokens.danger,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Seu direito de sumir. No lab isso apaga conversas e preferências deste aparelho. " +
                        "No app real, apaga conta, memória e login na nuvem.",
                    color = OrbitTokens.textMid,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrbitTokens.surface)
                        .border(1.dp, OrbitTokens.danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .orbitPressable(enabled = !apagando) { confirmPasso = 1 }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (apagando) {
                        CircularProgressIndicator(
                            color = OrbitTokens.danger,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = OrbitTokens.danger,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (apagando) "Apagando…" else "Apagar meus dados",
                        color = OrbitTokens.danger,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoPrivacidade(
    icon: ImageVector,
    cor: Color,
    titulo: String,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, shape)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                titulo,
                color = OrbitTokens.textHigh,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}
