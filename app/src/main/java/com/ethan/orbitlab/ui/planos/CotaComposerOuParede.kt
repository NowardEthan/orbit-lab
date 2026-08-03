package com.ethan.orbitlab.ui.planos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.data.billing.PlanosNav
import com.ethan.orbitlab.data.billing.UsageRepository

/**
 * Parede graciosa de cota — mesma regra do chat normal:
 * medidor sem saldo pro turno OU 429 do servidor → [LimiteAtingidoCard];
 * senão o composer / conteúdo filho.
 *
 * Usar em chat, finanças e início pra não deixar a entrada "solta"
 * quando a carteira free acabou.
 */
@Composable
fun CotaComposerOuParede(
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    val cotaBloqueada by UsageRepository.bloqueado.collectAsState()
    val usageCota by UsageRepository.usage.collectAsState()
    val semSaldo =
        !usageCota.loading && !usageCota.ilimitado && !usageCota.temSaldoParaChat
    if (cotaBloqueada || semSaldo) {
        LimiteAtingidoCard(
            usage = usageCota,
            onVerPlanos = { PlanosNav.abrir() },
            modifier = modifier.then(cardModifier),
        )
    } else {
        Box(modifier) {
            content()
        }
    }
}
