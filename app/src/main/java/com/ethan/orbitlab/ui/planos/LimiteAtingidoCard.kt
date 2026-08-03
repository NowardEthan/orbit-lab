package com.ethan.orbitlab.ui.planos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.billing.PlanId
import com.ethan.orbitlab.data.billing.UsageSnapshot
import com.ethan.orbitlab.data.billing.formatarReset
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitFills
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * A parede graciosa — acende no chat quando o servidor recusa o turno por cota
 * (HTTP 429 `quota_exceeded`). Não é um erro seco: a Luna "foi dormir e recarrega
 * a lua", com o quando e o CTA pros planos.
 *
 * Fica ACIMA do composer (edição contida) — não injetada na lista de mensagens.
 * Contraste, não opacidade: CTA aceso no degradê pastel (OrbitFills), corpo em
 * superfície grafite.
 */
@Composable
fun LimiteAtingidoCard(
    usage: UsageSnapshot,
    onVerPlanos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reset = usage.resetsAtMs
    val quando = if (reset != null) {
        formatarReset(reset - System.currentTimeMillis())
    } else null
    val ehGratis = usage.planId == PlanId.FREE

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(OrbitTokens.graphiteRaised),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.NightsStay,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Text(
                "A lua recolheu por ora",
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = buildString {
                append("Você usou a carteira de tokens desta janela. ")
                append(
                    if (quando != null) "Ela renova $quando."
                    else "Ela renova em breve.",
                )
                if (ehGratis) append(" Um plano te dá bastante mais fôlego.")
            },
            color = OrbitTokens.textMidN,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )

        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(OrbitFills.accent.brush)
                .orbitPressable(onClick = onVerPlanos)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (ehGratis) "Ver planos" else "Ver meu plano",
                color = OrbitFills.accent.onFill,
                fontSize = 14.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
