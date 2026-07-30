package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Os três modos da Luna, exclusivos entre si (escolhe-um). O seletor troca entre eles; nunca
 * há dois ligados ao mesmo tempo. É o enum que a `PrefsRepository` traduz pros dois booleanos
 * (técnico / agêntico) — `Conversa` é o estado com os dois desligados.
 */
enum class ModoLunaOpcao { Conversa, Tecnico, MaosAObra }

/**
 * Seletor de modo da Luna — substitui o antigo toggle «Técnico» por um menu que se explica.
 *
 * O toggle dizia SÓ que existia um modo técnico, nunca o que ele fazia (nem que o «desligado»
 * também é um modo, o de conversa). Aqui cada modo ganha nome e uma linha de explicação: a
 * pessoa escolhe sabendo, em vez de adivinhar o que o botão aceso muda.
 */
private data class ModoLuna(
    val opcao: ModoLunaOpcao,
    val nome: String,
    val descricao: String,
    val icone: ImageVector,
)

private val MODOS = listOf(
    ModoLuna(
        opcao = ModoLunaOpcao.Conversa,
        nome = "Conversa",
        descricao = "O jeito natural dela: leve, caloroso e direto ao ponto. Ótimo pro dia a dia e pra pensar junto.",
        icone = Icons.Rounded.ChatBubbleOutline,
    ),
    ModoLuna(
        opcao = ModoLunaOpcao.Tecnico,
        nome = "Técnico",
        descricao = "Respostas mais fundas e organizadas — com rigor, estrutura e os termos certos. Ótimo pra estudar, revisar ou redigir algo sério.",
        icone = Icons.Rounded.Tune,
    ),
    ModoLuna(
        opcao = ModoLunaOpcao.MaosAObra,
        nome = "Mãos à obra",
        descricao = "Ela planeja e usa as ferramentas — cria e edita documentos, marca passos. Ótimo pra tocar uma tarefa junto; é mais lento e pesa um pouco mais.",
        icone = Icons.Rounded.Handyman,
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunaModeSheet(
    visible: Boolean,
    modoAtivo: ModoLunaOpcao,
    onSelect: (ModoLunaOpcao) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.surfaceRaised,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OrbitTokens.borderSoft),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 12.dp),
        ) {
            Text(
                "Como a Luna responde",
                color = OrbitTokens.textHigh,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Escolhe o jeito que combina com o que você precisa agora.",
                color = OrbitTokens.textMid,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(16.dp))

            MODOS.forEachIndexed { i, modo ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                ModeCard(
                    modo = modo,
                    selecionado = modo.opcao == modoAtivo,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(modo.opcao)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    modo: ModoLuna,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    val forma = RoundedCornerShape(OrbitMetrics.radiusCard)
    val bg = if (selecionado) OrbitTokens.accentSoft else OrbitTokens.surface
    val borda = if (selecionado) OrbitTokens.accent.copy(alpha = 0.55f) else OrbitTokens.borderSoft
    val tintIcone = if (selecionado) OrbitTokens.accentText else OrbitTokens.textMid

    Row(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(bg)
            .border(1.dp, borda, forma)
            .orbitPressable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (selecionado) OrbitTokens.accent.copy(alpha = 0.18f) else OrbitTokens.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(modo.icone, contentDescription = null, tint = tintIcone, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                modo.nome,
                color = OrbitTokens.textHigh,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                modo.descricao,
                color = OrbitTokens.textMid,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selecionado) OrbitTokens.accent else Color.Transparent)
                .then(
                    if (selecionado) Modifier
                    else Modifier.border(1.5.dp, OrbitTokens.borderSoft, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selecionado) {
                Icon(Icons.Rounded.Check, contentDescription = "Selecionado", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}
