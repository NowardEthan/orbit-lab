package com.ethan.orbitlab.ui.planos

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AutoAwesome
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
import com.ethan.orbitlab.data.billing.formatarTokens
import com.ethan.orbitlab.data.billing.planoPorId
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Medidor da carteira — pílula/cartão "X restantes · renova em Y" com barra de
 * saldo. Mora no rodapé da gaveta; tocar abre os Planos.
 *
 * Segue a regra visual do app: contraste, não opacidade — barra de saldo em
 * azul pastel sobre trilho grafite; nunca accent-sobre-accent.
 */
@Composable
fun UsageMeter(
    usage: UsageSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plano = planoPorId(usage.planId)
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OrbitTokens.graphiteSurf)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = if (usage.planId == PlanId.FREE) OrbitTokens.textMidN else OrbitTokens.bluePastel,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                plano.nome,
                color = OrbitTokens.textHiN,
                fontSize = 13.5.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = when {
                    usage.loading -> "…"
                    usage.ilimitado -> "Ilimitado"
                    else -> "${formatarTokens(usage.remainingTokens ?: 0)} restantes"
                },
                color = if (usage.ilimitado) OrbitTokens.bluePastel else OrbitTokens.textMidN,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (!usage.ilimitado && !usage.loading) {
            Spacer(Modifier.height(9.dp))
            val limite = usage.windowTokenLimit ?: 1L
            val saldoFrac = ((usage.remainingTokens ?: 0L).toFloat() / limite.toFloat())
                .coerceIn(0f, 1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(OrbitTokens.graphiteRaised),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(saldoFrac)
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (saldoFrac <= 0.12f) OrbitTokens.warning else OrbitTokens.bluePastel),
                )
            }
            val reset = usage.resetsAtMs
            if (reset != null && saldoFrac < 1f) {
                Spacer(Modifier.height(7.dp))
                Text(
                    "renova ${formatarReset(reset - System.currentTimeMillis())}",
                    color = OrbitTokens.textLowN,
                    fontSize = 11.5.sp,
                )
            }
        }

        if (usage.planId == PlanId.FREE) {
            Spacer(Modifier.height(9.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Ver planos",
                    color = OrbitTokens.bluePastel,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("→", color = OrbitTokens.bluePastel, fontSize = 13.sp)
            }
        }
    }
}
