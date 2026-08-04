package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitFills
import com.ethan.orbitlab.ui.theme.OrbitTokens

/**
 * Checklist do plano da Luna — à vista, sempre aberta.
 *
 * Espelha a coleira do core (`planejar` / `concluir_passo`): a sensação de agente
 * que declara o caminho e risca passo a passo, sem esconder em “N passos”.
 */
@Composable
fun LunaPlanChecklist(
    plano: List<PassoPlano>,
    aoVivo: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (plano.isEmpty()) return

    val feitos = plano.count { it.feito }
    val corrente = plano.indexOfFirst { !it.feito }
    val resumo = when {
        feitos >= plano.size -> "Plano · pronto"
        feitos == 0 -> "Plano · ${plano.size} passos"
        else -> "Plano · $feitos/${plano.size}"
    }

    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape),
    ) {
        Box(
            Modifier
                .width(2.5.dp)
                .fillMaxHeight()
                .background(OrbitTokens.bluePastel.copy(alpha = 0.45f)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = resumo,
                color = OrbitTokens.textMidN,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                plano.forEachIndexed { i, passo ->
                    val ativo = aoVivo && i == corrente && !passo.feito
                    PlanoLinha(passo = passo, ativo = ativo)
                }
            }
        }
    }
}

@Composable
private fun PlanoLinha(
    passo: PassoPlano,
    ativo: Boolean,
) {
    val pulso by rememberInfiniteTransition(label = "planoCorrente").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "planoPulso",
    )
    val corTexto by animateColorAsState(
        targetValue = when {
            passo.feito -> OrbitTokens.textLowN
            ativo -> OrbitTokens.textHiN
            else -> OrbitTokens.textMidN
        },
        label = "planoCor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (ativo) Modifier.alpha(0.7f + 0.3f * pulso) else Modifier),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CaixaPlano(marcado = passo.feito, ativo = ativo)
        Text(
            text = passo.texto,
            color = corTexto,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = if (ativo) FontWeight.Medium else FontWeight.Normal,
            textDecoration = if (passo.feito) TextDecoration.LineThrough else null,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CaixaPlano(marcado: Boolean, ativo: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    val lado = 18.dp
    when {
        marcado -> Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(lado)
                .clip(shape)
                .background(OrbitFills.accent.brush),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = OrbitFills.accent.onFill,
                modifier = Modifier.size(13.dp),
            )
        }
        ativo -> Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(lado)
                .clip(shape)
                .border(1.5.dp, OrbitTokens.bluePastel, shape)
                .background(OrbitTokens.bluePastel.copy(alpha = 0.12f)),
        )
        else -> Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(lado)
                .clip(shape)
                .border(1.5.dp, OrbitTokens.border, shape),
        )
    }
}
