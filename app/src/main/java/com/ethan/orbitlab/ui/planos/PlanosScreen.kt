package com.ethan.orbitlab.ui.planos

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.data.billing.Disponibilidade
import com.ethan.orbitlab.data.billing.LunaBillingApi
import com.ethan.orbitlab.data.billing.PLANS
import com.ethan.orbitlab.data.billing.PlanConfig
import com.ethan.orbitlab.data.billing.PlanFeature
import com.ethan.orbitlab.data.billing.PlanId
import com.ethan.orbitlab.data.billing.UsageRepository
import androidx.compose.runtime.collectAsState
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitFills
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch

private enum class Periodo(val label: String, val api: String) {
    MENSAL("Mensal", "monthly"),
    ANUAL("Anual", "annual"),
}

/**
 * Tela de Planos — free/plus/pro com a cópia portada de `plans.ts`. "Assinar" abre
 * a folha de CPF, chama o checkout Asaas e abre a URL no navegador; "Já paguei"
 * sincroniza o plano com o servidor.
 *
 * O OrbitLab não tem conta anônima (AuthRepository: "Sem anônimo"), então toda
 * sessão já tem email — o checkout nunca precisa orientar login.
 */
@Composable
fun PlanosScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val usage by UsageRepository.usage.collectAsState()

    var periodo by remember { mutableStateOf(Periodo.ANUAL) }
    var planoCheckout by remember { mutableStateOf<PlanId?>(null) }
    var ocupado by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        Text(
            "Planos",
            color = OrbitTokens.textHiN,
            fontSize = 26.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Escolha o fôlego que combina com seu uso. A conversa é barata e generosa; " +
                "gerar imagem e pesquisa profunda pesam mais na carteira.",
            color = OrbitTokens.textMidN,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )

        Spacer(Modifier.height(18.dp))
        AlternadorPeriodo(periodo) { periodo = it }

        Spacer(Modifier.height(18.dp))
        PLANS.forEach { plano ->
            PlanoCard(
                plano = plano,
                periodo = periodo,
                atual = plano.id == usage.planId,
                ocupado = ocupado,
                onAssinar = { planoCheckout = plano.id },
            )
            Spacer(Modifier.height(14.dp))
        }

        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .orbitPressable(enabled = !ocupado) {
                    ocupado = true
                    scope.launch {
                        val token = AuthRepository.getIdToken()
                        if (token == null) {
                            ocupado = false
                            toast(context, "Entre na sua conta para continuar.")
                            return@launch
                        }
                        val r = LunaBillingApi.sync(token)
                        ocupado = false
                        when {
                            r.ok && !r.plano.isNullOrBlank() && r.plano != "free" -> {
                                UsageRepository.refresh()
                                toast(context, "Plano atualizado. Bom proveito! 🌙")
                            }
                            r.ok -> toast(context, "Nenhuma assinatura paga encontrada ainda.")
                            else -> toast(context, r.erro ?: "Não consegui sincronizar agora.")
                        }
                    }
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Já paguei — sincronizar meu plano",
                color = OrbitTokens.bluePastel,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    planoCheckout?.let { alvo ->
        CpfCnpjSheet(
            onDismiss = { planoCheckout = null },
            onConfirmar = { digitos ->
                planoCheckout = null
                ocupado = true
                scope.launch {
                    val token = AuthRepository.getIdToken()
                    if (token == null) {
                        ocupado = false
                        toast(context, "Entre na sua conta para continuar.")
                        return@launch
                    }
                    val r = LunaBillingApi.checkout(token, alvo, periodo.api, digitos)
                    ocupado = false
                    when (r) {
                        is LunaBillingApi.CheckoutResult.Ok -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(r.url))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(intent) }
                                .onFailure { toast(context, "Não consegui abrir o pagamento.") }
                        }
                        is LunaBillingApi.CheckoutResult.Erro -> toast(context, r.mensagem)
                    }
                }
            },
        )
    }
}

private fun toast(context: android.content.Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}

@Composable
private fun AlternadorPeriodo(periodo: Periodo, onEscolher: (Periodo) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(OrbitTokens.graphiteSurf)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Periodo.entries.forEach { p ->
            val ativo = p == periodo
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (ativo) OrbitTokens.bluePastel else androidx.compose.ui.graphics.Color.Transparent)
                    .orbitPressable { onEscolher(p) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        p.label,
                        color = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.textMidN,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (p == Periodo.ANUAL) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "2 meses grátis",
                            color = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.bluePastel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanoCard(
    plano: PlanConfig,
    periodo: Periodo,
    atual: Boolean,
    ocupado: Boolean,
    onAssinar: () -> Unit,
) {
    val temCheckout = plano.id == PlanId.PLUS || plano.id == PlanId.PRO
    val corBorda = when {
        atual -> OrbitTokens.bluePastel
        plano.destaque -> OrbitTokens.bluePastelDim
        else -> OrbitTokens.graphiteHair
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(OrbitTokens.graphiteSurf)
            .border(if (atual || plano.destaque) 1.5.dp else 1.dp, corBorda, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                plano.nome,
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            when {
                atual -> Selo("Seu plano", OrbitTokens.bluePastel, OrbitTokens.onBluePastel)
                plano.badge != null -> Selo(plano.badge!!, OrbitTokens.graphiteRaised, OrbitTokens.bluePastel)
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(plano.tagline, color = OrbitTokens.textMidN, fontSize = 13.sp)

        Spacer(Modifier.height(12.dp))
        PrecoBloco(plano, periodo)

        Spacer(Modifier.height(14.dp))
        plano.features.forEach { f ->
            FeatureLinha(f)
            Spacer(Modifier.height(9.dp))
        }

        if (temCheckout) {
            Spacer(Modifier.height(6.dp))
            val habilitado = !atual && !ocupado
            val usaFill = plano.destaque && habilitado
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .then(
                        if (usaFill) Modifier.background(OrbitFills.accent.brush)
                        else Modifier.background(OrbitTokens.graphiteRaised),
                    )
                    .orbitPressable(enabled = habilitado, onClick = onAssinar)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (atual) "Plano atual" else "Assinar ${plano.nome}",
                    color = when {
                        atual -> OrbitTokens.textMidN
                        usaFill -> OrbitFills.accent.onFill
                        else -> OrbitTokens.bluePastel
                    },
                    fontSize = 14.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun PrecoBloco(plano: PlanConfig, periodo: Periodo) {
    if (plano.id == PlanId.FREE) {
        Text(
            "Grátis",
            color = OrbitTokens.textHiN,
            fontSize = 26.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
        )
        return
    }
    val anual = periodo == Periodo.ANUAL && plano.precoAnualPorMes != null
    val valorMes = if (anual) plano.precoAnualPorMes!! else plano.precoMensal.toDouble()
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "R$ ${formatarReais(valorMes)}",
            color = OrbitTokens.textHiN,
            fontSize = 26.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(4.dp))
        Text("/mês", color = OrbitTokens.textMidN, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
    }
    if (anual && plano.precoAnual != null) {
        Spacer(Modifier.height(2.dp))
        Text(
            "cobrado R$ ${plano.precoAnual} por ano",
            color = OrbitTokens.textLowN,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun FeatureLinha(f: PlanFeature) {
    val (icone, tint) = when (f.disponivel) {
        Disponibilidade.SIM -> Icons.Rounded.Check to OrbitTokens.bluePastel
        Disponibilidade.LIMITADO -> Icons.Rounded.Remove to OrbitTokens.warning
        Disponibilidade.NAO -> Icons.Rounded.Close to OrbitTokens.textLowN
    }
    Row(verticalAlignment = Alignment.Top) {
        Icon(icone, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp).padding(top = 1.dp))
        Spacer(Modifier.width(9.dp))
        Column {
            Text(
                f.label,
                color = if (f.disponivel == Disponibilidade.NAO) OrbitTokens.textLowN else OrbitTokens.textHiN,
                fontSize = 13.5.sp,
            )
            if (!f.detalhe.isNullOrBlank()) {
                Text(f.detalhe!!, color = OrbitTokens.textMidN, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun Selo(texto: String, fundo: androidx.compose.ui.graphics.Color, cor: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(fundo)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(texto, color = cor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

/** "39", "32,50", "79" — sem centavos quando redondo. */
private fun formatarReais(v: Double): String {
    if (v % 1.0 == 0.0) return v.toInt().toString()
    return String.format("%.2f", v).replace('.', ',')
}
