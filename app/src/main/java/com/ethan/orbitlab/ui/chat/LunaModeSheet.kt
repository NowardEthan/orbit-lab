package com.ethan.orbitlab.ui.chat

// LEGADO A1 — seletor Conversa/Técnico/Ação removido do produto. Não ligar na UI.

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        descricao = "Leve e direta — o jeito natural dela.",
        icone = Icons.Rounded.ChatBubbleOutline,
    ),
    ModoLuna(
        opcao = ModoLunaOpcao.Tecnico,
        nome = "Técnico",
        descricao = "Fundo e rigoroso, com os termos certos.",
        icone = Icons.Rounded.Tune,
    ),
    ModoLuna(
        opcao = ModoLunaOpcao.MaosAObra,
        nome = "Ação",
        descricao = "Usa as ferramentas e toca a tarefa com você.",
        icone = Icons.Rounded.Handyman,
    ),
)

@Deprecated("A1: sem seletor de modo — soft router no core")
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
        containerColor = OrbitTokens.graphiteRaised,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OrbitTokens.graphiteHair),
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
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Escolhe o jeito que combina com o que você precisa agora.",
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(12.dp))

            MODOS.forEachIndexed { i, modo ->
                if (i > 0) Spacer(Modifier.height(6.dp))
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
    // Um só acento (azul pastel): o selecionado ACENDE, o resto fica quieto no grafite —
    // sem os três tons antigos (violeta/azul/verde). A seleção é a borda + o ladrilho aceso
    // + o selo, todos no pastel; contraste, não opacidade.
    val borda = if (selecionado) OrbitTokens.bluePastel else OrbitTokens.graphiteHair

    Row(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(OrbitTokens.graphiteSurf)
            .border(if (selecionado) 1.5.dp else 1.dp, borda, forma)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (selecionado) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modo.icone,
                contentDescription = null,
                tint = if (selecionado) OrbitTokens.onBluePastel else OrbitTokens.textMidN,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                modo.nome,
                color = OrbitTokens.textHiN,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                modo.descricao,
                color = OrbitTokens.textMidN,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .then(
                    if (selecionado) Modifier.background(OrbitTokens.bluePastel)
                    else Modifier.border(1.5.dp, OrbitTokens.graphiteHair, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selecionado) {
                Icon(Icons.Rounded.Check, contentDescription = "Selecionado", tint = OrbitTokens.onBluePastel, modifier = Modifier.size(12.dp))
            }
        }
    }
}
