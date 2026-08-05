package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.financas.Carteira
import com.ethan.orbitlab.data.financas.CarteiraRascunho
import com.ethan.orbitlab.data.financas.CorCarteira
import com.ethan.orbitlab.data.financas.FaturaCredito
import com.ethan.orbitlab.data.financas.FinancasRepository
import com.ethan.orbitlab.data.financas.FirestoreFinancas
import com.ethan.orbitlab.data.financas.Lancamento
import com.ethan.orbitlab.data.financas.MotivoTransferencia
import com.ethan.orbitlab.data.financas.PeriodoExtrato
import com.ethan.orbitlab.data.financas.TipoCarteira
import com.ethan.orbitlab.data.financas.TipoLancamento
import com.ethan.orbitlab.data.financas.TransferenciaLauncher
import com.ethan.orbitlab.data.financas.diasAte
import com.ethan.orbitlab.data.financas.faixaDoPeriodo
import com.ethan.orbitlab.data.financas.faturaCredito
import com.ethan.orbitlab.data.financas.filtrarPorPeriodo
import com.ethan.orbitlab.data.financas.filtrarPorPeriodoAteHoje
import com.ethan.orbitlab.data.financas.formatarReais
import com.ethan.orbitlab.data.financas.lancamentosQueJaContam
import com.ethan.orbitlab.data.financas.parsearReaisParaCentavos
import com.ethan.orbitlab.data.financas.proximoVencimentoMs
import com.ethan.orbitlab.data.financas.saldoDerivado
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CardRadius = 22.dp

/**
 * Cartões — polish alinhado ao concept: insight, resumo, cartão principal, lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartoesScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val carteiras by FinancasRepository.carteiras.collectAsState()
    val lancamentos by FinancasRepository.lancamentos.collectAsState()
    val transferencias by FinancasRepository.transferencias.collectAsState()
    var formulario by remember { mutableStateOf<FormularioCarteira?>(null) }
    var arquivarAlvo by remember { mutableStateOf<Carteira?>(null) }
    var filtroTipo by remember { mutableStateOf<String?>(null) }
    var filtroTipoAberto by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val agora = System.currentTimeMillis()
    val lancamentosEfetivos = remember(lancamentos, agora) { lancamentosQueJaContam(lancamentos, agora) }

    val credito = remember(carteiras) {
        carteiras.filter { it.tipo == TipoCarteira.CARTAO_CREDITO }
    }
    val principal = credito.firstOrNull() ?: carteiras.firstOrNull()
    val faturas = remember(carteiras, lancamentos, transferencias, agora) {
        credito.mapNotNull { faturaCredito(it, lancamentos, transferencias, agora) }
    }
    val faturasAbertas = faturas.sumOf { it.faturaCentavos }
    val limiteLivreTotal = faturas.sumOf { it.limiteLivreCentavos ?: 0L }
    val insight = remember(carteiras, lancamentosEfetivos, credito, agora) {
        insightCartoes(carteiras, lancamentosEfetivos, credito, agora)
    }
    val faixaMes = remember(agora) { faixaDoPeriodo(PeriodoExtrato.MES, agora) }
    val gastoMesPorCarteira = remember(lancamentos, faixaMes, agora) {
        filtrarPorPeriodoAteHoje(lancamentos, faixaMes, agora)
            .filter { it.tipo == TipoLancamento.SAIDA }
            .groupBy { it.carteiraId }
            .mapValues { (_, xs) -> xs.sumOf { it.valorCentavos } }
    }
    val faturaPrincipal = remember(principal, lancamentos, transferencias, agora) {
        principal?.let { faturaCredito(it, lancamentos, transferencias, agora) }
    }
    val demais = remember(carteiras, principal, filtroTipo) {
        carteiras
            .filter { it.id != principal?.id }
            .filter { filtroTipo == null || it.tipo == filtroTipo }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = OrbitMetrics.pagePadding,
                end = OrbitMetrics.pagePadding,
                top = 2.dp,
                bottom = 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "Cartões",
                    color = OrbitTokens.textHiN,
                    fontSize = 28.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.orbitEnter(0),
                )
            }

            if (carteiras.isEmpty()) {
                item {
                    VazioCartoes(onAdicionar = { formulario = FormularioCarteira.Nova })
                }
            } else {
                if (insight != null) {
                    item { CardInsightCartoes(insight) }
                }

                if (credito.isNotEmpty()) {
                    item {
                        ResumoFaturasAbertas(
                            faturasAbertas = faturasAbertas,
                            limiteLivre = limiteLivreTotal,
                        )
                    }
                }

                if (principal != null) {
                    item {
                        Text(
                            "Cartão principal",
                            color = OrbitTokens.textHiN,
                            fontSize = 17.sp,
                            fontFamily = Bricolage,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    item {
                        CartaoPrincipalConcept(
                            carteira = principal,
                            saldo = saldoDerivado(principal, lancamentos, transferencias),
                            fatura = faturaPrincipal,
                            onClick = { formulario = FormularioCarteira.Editar(principal) },
                            onArquivar = { arquivarAlvo = principal },
                            onPagarFatura = faturaPrincipal?.takeIf { it.faturaCentavos > 0 }?.let {
                                {
                                    val de = carteiras.firstOrNull { c ->
                                        !c.arquivada && c.tipo != TipoCarteira.CARTAO_CREDITO
                                    }
                                    TransferenciaLauncher.abrir(
                                        TransferenciaLauncher.Prefill(
                                            deCarteiraId = de?.id,
                                            paraCarteiraId = principal.id,
                                            motivo = MotivoTransferencia.PAGAR_FATURA,
                                            valorCentavos = faturaPrincipal.faturaCentavos,
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.orbitEnter(2),
                        )
                    }
                }

                val demaisFiltrados = demais
                if (demaisFiltrados.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Todos",
                                color = OrbitTokens.textHiN,
                                fontSize = 17.sp,
                                fontFamily = Bricolage,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Filtrar",
                                color = OrbitTokens.bluePastel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.orbitPressable { filtroTipoAberto = true },
                            )
                        }
                    }
                    items(demaisFiltrados, key = { it.id }) { c ->
                        val faturaLinha = faturaCredito(c, lancamentos, transferencias, agora)
                        LinhaCarteiraConcept(
                            carteira = c,
                            valorDireita = when (c.tipo) {
                                TipoCarteira.CARTAO_CREDITO -> faturaLinha?.faturaCentavos ?: 0L
                                else -> gastoMesPorCarteira[c.id] ?: 0L
                            },
                            subtituloDireita = when (c.tipo) {
                                TipoCarteira.CARTAO_CREDITO -> "fatura"
                                TipoCarteira.DINHEIRO -> "dinheiro"
                                else -> "conta"
                            },
                            onClick = { formulario = FormularioCarteira.Editar(c) },
                            onArquivar = { arquivarAlvo = c },
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(OrbitMetrics.pagePadding)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(999.dp),
                    ambientColor = OrbitTokens.bluePastel.copy(alpha = 0.5f),
                    spotColor = OrbitTokens.bluePastel.copy(alpha = 0.65f),
                )
                .clip(RoundedCornerShape(999.dp))
                .background(OrbitTokens.bluePastel)
                .orbitPressable { formulario = FormularioCarteira.Nova }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = OrbitTokens.onBluePastel,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Adicionar",
                color = OrbitTokens.onBluePastel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    formulario?.let { form ->
        CarteiraFormSheet(
            inicial = when (form) {
                FormularioCarteira.Nova -> null
                is FormularioCarteira.Editar -> form.carteira
            },
            onDismiss = { formulario = null },
            onSalvar = { rascunho ->
                val u = uid ?: return@CarteiraFormSheet
                scope.launch {
                    when (form) {
                        FormularioCarteira.Nova ->
                            FirestoreFinancas.criarCarteira(u, rascunho)
                        is FormularioCarteira.Editar ->
                            FirestoreFinancas.atualizarCarteira(u, form.carteira.id, rascunho)
                    }
                    formulario = null
                }
            },
        )
    }

    arquivarAlvo?.let { alvo ->
        AlertDialog(
            onDismissRequest = { arquivarAlvo = null },
            containerColor = OrbitTokens.graphiteSurf,
            titleContentColor = OrbitTokens.textHiN,
            textContentColor = OrbitTokens.textMidN,
            title = { Text("Arquivar ${alvo.apelido}?") },
            text = {
                Text("Some da lista. O histórico de lançamentos continua intacto.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val u = uid
                        val id = alvo.id
                        arquivarAlvo = null
                        if (u != null) {
                            scope.launch { FirestoreFinancas.arquivarCarteira(u, id) }
                        }
                    },
                ) {
                    Text("Arquivar", color = OrbitTokens.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { arquivarAlvo = null }) {
                    Text("Cancelar", color = OrbitTokens.textMidN)
                }
            },
        )
    }

    if (filtroTipoAberto) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { filtroTipoAberto = false },
            containerColor = OrbitTokens.graphiteSurf,
            contentColor = OrbitTokens.textHiN,
            dragHandle = null,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 8.dp, bottom = 20.dp),
            ) {
                Text(
                    "Filtrar por tipo",
                    color = OrbitTokens.textHiN,
                    fontSize = 18.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(14.dp))
                listOf(
                    null to "Todos",
                    TipoCarteira.CARTAO_CREDITO to "Crédito",
                    TipoCarteira.CONTA_DEBITO to "Débito",
                    TipoCarteira.DINHEIRO to "Dinheiro",
                ).forEach { (tipo, label) ->
                    val on = filtroTipo == tipo
                    Text(
                        label,
                        color = if (on) OrbitTokens.onBluePastel else OrbitTokens.textHiN,
                        fontSize = 15.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
                            .orbitPressable {
                                filtroTipo = tipo
                                filtroTipoAberto = false
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

private sealed class FormularioCarteira {
    data object Nova : FormularioCarteira()
    data class Editar(val carteira: Carteira) : FormularioCarteira()
}

private data class InsightCartoes(
    val destaquePct: String,
    val banco: String,
    val diasFecha: Int?,
)

private fun insightCartoes(
    carteiras: List<Carteira>,
    lancamentos: List<Lancamento>,
    credito: List<Carteira>,
    agoraMs: Long,
): InsightCartoes? {
    if (credito.isEmpty()) return null
    val mes = filtrarPorPeriodoAteHoje(lancamentos, faixaDoPeriodo(PeriodoExtrato.MES, agoraMs), agoraMs)
        .filter { it.tipo == TipoLancamento.SAIDA }
    val total = mes.sumOf { it.valorCentavos }
    if (total <= 0L) return null
    val idsCredito = credito.map { it.id }.toSet()
    val noCredito = mes.filter { it.carteiraId in idsCredito }
    if (noCredito.isEmpty()) return null
    val porCartao = noCredito.groupBy { it.carteiraId }
        .maxByOrNull { it.value.sumOf { l -> l.valorCentavos } } ?: return null
    val cartao = credito.find { it.id == porCartao.key } ?: return null
    val pct = ((porCartao.value.sumOf { it.valorCentavos } * 100) / total).toInt()
    val diasFecha = cartao.fechamentoDia?.let { dia ->
        diasAte(proximoVencimentoMs(dia, agoraMs), agoraMs)
    }
    return InsightCartoes(
        destaquePct = "$pct%",
        banco = cartao.banco?.takeIf { it.isNotBlank() } ?: cartao.apelido,
        diasFecha = diasFecha,
    )
}

@Composable
private fun CardInsightCartoes(insight: InsightCartoes) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(14.dp)
            .orbitEnter(1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            OrbitTokens.bluePastel.copy(alpha = 0.95f),
                            OrbitTokens.bluePastelDim.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = OrbitTokens.textHiN,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                ) {
                    append(insight.destaquePct)
                }
                withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 14.sp)) {
                    append(" dos seus gastos vão no crédito do ")
                }
                withStyle(
                    SpanStyle(
                        color = OrbitTokens.textHiN,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                ) {
                    append(insight.banco)
                }
                withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 14.sp)) {
                    append(". De olho na fatura")
                    when (val d = insight.diasFecha) {
                        null -> append(".")
                        0 -> append(" — fecha hoje.")
                        1 -> append(" — fecha amanhã.")
                        else -> if (d < 0) append(".") else append(" — fecha em ${d} dias.")
                    }
                }
            },
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ResumoFaturasAbertas(faturasAbertas: Long, limiteLivre: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatarReais(faturasAbertas),
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text("faturas abertas", color = OrbitTokens.textLowN, fontSize = 12.sp)
        }
        Box(
            Modifier
                .width(1.dp)
                .height(36.dp)
                .background(OrbitTokens.graphiteHair),
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatarReais(limiteLivre),
                color = OrbitTokens.bluePastel,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text("limite livre", color = OrbitTokens.textLowN, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CartaoPrincipalConcept(
    carteira: Carteira,
    saldo: Long,
    fatura: FaturaCredito?,
    onClick: () -> Unit,
    onArquivar: () -> Unit,
    onPagarFatura: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(188.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(brushDaCor(carteira.cor))
                .orbitPressable(onClick = onClick)
                .padding(18.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        carteira.banco ?: carteira.apelido,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        TipoCarteira.rotulo(carteira.tipo).uppercase(Locale.forLanguageTag("pt-BR")),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .width(42.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF5D047), Color(0xFFD4A017)),
                            ),
                        ),
                )
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        carteira.ultimos4?.let { "••••  ••••  ••••  $it" }
                            ?: carteira.apelido,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    )
                    Box(Modifier.size(width = 40.dp, height = 24.dp)) {
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEB001B).copy(alpha = 0.9f)),
                        )
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF79E1B).copy(alpha = 0.9f)),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardRadius))
                .background(OrbitTokens.graphiteSurf)
                .padding(16.dp),
        ) {
            if (fatura != null && carteira.tipo == TipoCarteira.CARTAO_CREDITO) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = OrbitTokens.textHiN,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                ),
                            ) {
                                append(formatarReais(fatura.faturaCentavos))
                            }
                            fatura.limiteCentavos?.let { lim ->
                                withStyle(
                                    SpanStyle(
                                        color = OrbitTokens.textLowN,
                                        fontSize = 15.sp,
                                    ),
                                ) {
                                    append(" / ")
                                    append(
                                        formatarReais(lim)
                                            .replace("R$\u00A0", "")
                                            .replace("R$ ", ""),
                                    )
                                }
                            }
                        },
                    )
                    Text("fatura atual", color = OrbitTokens.textLowN, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(OrbitTokens.graphiteHair),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fatura.pctUsado.coerceIn(0.02f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                if (fatura.pctUsado > 0.9f) OrbitTokens.danger
                                else OrbitTokens.bluePastel,
                            ),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Fecha ${rotuloDiaCurto(fatura.fechamentoDia)}",
                        color = OrbitTokens.textMidN,
                        fontSize = 13.sp,
                    )
                    Text(
                        "Vence ${rotuloDiaCurto(fatura.vencimentoDia)}",
                        color = OrbitTokens.textMidN,
                        fontSize = 13.sp,
                    )
                }
                if (onPagarFatura != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Pagar fatura",
                        color = OrbitTokens.bluePastel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.orbitPressable(onClick = onPagarFatura),
                    )
                }
            } else {
                Text("Saldo", color = OrbitTokens.textLowN, fontSize = 12.sp)
                Text(
                    formatarReais(saldo),
                    color = OrbitTokens.textHiN,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Arquivar",
                color = OrbitTokens.textLowN,
                fontSize = 12.sp,
                modifier = Modifier.orbitPressable(onClick = onArquivar),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaCarteiraConcept(
    carteira: Carteira,
    valorDireita: Long,
    subtituloDireita: String,
    onClick: () -> Unit,
    onArquivar: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrbitTokens.graphiteSurf)
            .combinedClickable(onClick = onClick, onLongClick = onArquivar)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brushDaCor(carteira.cor)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                iconeDoTipo(carteira.tipo),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                carteira.banco?.takeIf { it.isNotBlank() } ?: carteira.apelido,
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(TipoCarteira.rotulo(carteira.tipo))
                    if (carteira.tipo != TipoCarteira.CARTAO_CREDITO) {
                        append(" · gasto no mês")
                    }
                },
                color = OrbitTokens.textLowN,
                fontSize = 12.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatarReais(valorDireita),
                color = OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtituloDireita,
                color = OrbitTokens.textLowN,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun VazioCartoes(onAdicionar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(20.dp)
            .orbitEnter(2),
    ) {
        Text(
            "Nenhuma carteira ainda",
            color = OrbitTokens.textHiN,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Adiciona uma conta, cartão ou dinheiro vivo — é a base pra registrar o que entra e sai.",
            color = OrbitTokens.textMidN,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "+ Adicionar cartão ou conta",
            color = OrbitTokens.bluePastel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.orbitPressable(onClick = onAdicionar),
        )
    }
}

private fun rotuloDiaCurto(dia: Int?): String {
    if (dia == null) return "—"
    val ms = proximoVencimentoMs(dia, System.currentTimeMillis()) ?: return "dia $dia"
    return SimpleDateFormat("d MMM", Locale.forLanguageTag("pt-BR"))
        .format(Date(ms))
        .lowercase(Locale.forLanguageTag("pt-BR"))
}

private fun iconeDoTipo(tipo: String): ImageVector = when (tipo) {
    TipoCarteira.CARTAO_CREDITO -> Icons.Rounded.CreditCard
    TipoCarteira.DINHEIRO -> Icons.Rounded.Payments
    else -> Icons.Rounded.AccountBalanceWallet
}

private fun brushDaCor(cor: String): Brush {
    val (a, b) = when (cor) {
        CorCarteira.AZUL -> Color(0xFF4A7EC8) to Color(0xFF1E3A6E)
        CorCarteira.ROXO -> Color(0xFF9B4DCA) to Color(0xFF5A0A9A)
        CorCarteira.VERDE -> Color(0xFF2F7A5B) to Color(0xFF143D2E)
        CorCarteira.AMBAR -> Color(0xFFB8862E) to Color(0xFF5C3A10)
        else -> Color(0xFF3A3D44) to Color(0xFF1E2024)
    }
    return Brush.linearGradient(listOf(a, b))
}

// ── Formulário ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarteiraFormSheet(
    inicial: Carteira?,
    onDismiss: () -> Unit,
    onSalvar: (CarteiraRascunho) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tipo by remember {
        mutableStateOf(inicial?.tipo ?: TipoCarteira.CONTA_DEBITO)
    }
    var apelido by remember { mutableStateOf(inicial?.apelido.orEmpty()) }
    var banco by remember { mutableStateOf(inicial?.banco.orEmpty()) }
    var cor by remember { mutableStateOf(inicial?.cor ?: CorCarteira.GRAFITE) }
    var ultimos4 by remember { mutableStateOf(inicial?.ultimos4.orEmpty()) }
    var saldoTexto by remember {
        mutableStateOf(
            inicial?.saldoInicialCentavos
                ?.takeIf { it != 0L }
                ?.let { formatarReais(it).removePrefix("R$\u00A0").removePrefix("R$ ") }
                .orEmpty(),
        )
    }
    var limiteTexto by remember {
        mutableStateOf(
            inicial?.limiteCentavos
                ?.let { formatarReais(it).removePrefix("R$\u00A0").removePrefix("R$ ") }
                .orEmpty(),
        )
    }
    var fechamento by remember {
        mutableStateOf(inicial?.fechamentoDia?.toString().orEmpty())
    }
    var vencimento by remember {
        mutableStateOf(inicial?.vencimentoDia?.toString().orEmpty())
    }
    var erro by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.graphiteSurf,
        contentColor = OrbitTokens.textHiN,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            Text(
                if (inicial == null) "Nova carteira" else "Editar carteira",
                color = OrbitTokens.textHiN,
                fontSize = 20.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))

            Text("Tipo", color = OrbitTokens.textLowN, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChipTipo("Débito", TipoCarteira.CONTA_DEBITO, tipo) { tipo = it }
                ChipTipo("Crédito", TipoCarteira.CARTAO_CREDITO, tipo) { tipo = it }
                ChipTipo("Dinheiro", TipoCarteira.DINHEIRO, tipo) { tipo = it }
            }

            Spacer(Modifier.height(16.dp))
            CampoTexto(
                rotulo = "Apelido",
                valor = apelido,
                onValor = { apelido = it },
                placeholder = "Ex.: Nubank Crédito",
            )
            Spacer(Modifier.height(12.dp))
            CampoTexto(
                rotulo = "Banco (opcional)",
                valor = banco,
                onValor = { banco = it },
                placeholder = "Ex.: Nubank",
            )

            Spacer(Modifier.height(16.dp))
            Text("Cor do cartão", color = OrbitTokens.textLowN, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CorCarteira.todas.forEach { chave ->
                    val selecionada = cor == chave
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(brushDaCor(chave))
                            .then(
                                if (selecionada) {
                                    Modifier.border(2.dp, OrbitTokens.bluePastel, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .orbitPressable { cor = chave },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            CampoTexto(
                rotulo = "Saldo inicial",
                valor = saldoTexto,
                onValor = { saldoTexto = it },
                placeholder = "0,00",
                teclado = KeyboardType.Decimal,
            )

            if (tipo == TipoCarteira.CARTAO_CREDITO) {
                Spacer(Modifier.height(12.dp))
                CampoTexto(
                    rotulo = "Últimos 4 dígitos",
                    valor = ultimos4,
                    onValor = { if (it.length <= 4) ultimos4 = it.filter { ch -> ch.isDigit() } },
                    placeholder = "1234",
                    teclado = KeyboardType.Number,
                )
                Spacer(Modifier.height(12.dp))
                CampoTexto(
                    rotulo = "Limite",
                    valor = limiteTexto,
                    onValor = { limiteTexto = it },
                    placeholder = "5.000,00",
                    teclado = KeyboardType.Decimal,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoTexto(
                        rotulo = "Fecha (dia)",
                        valor = fechamento,
                        onValor = { if (it.length <= 2) fechamento = it.filter { ch -> ch.isDigit() } },
                        placeholder = "3",
                        teclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    CampoTexto(
                        rotulo = "Vence (dia)",
                        valor = vencimento,
                        onValor = { if (it.length <= 2) vencimento = it.filter { ch -> ch.isDigit() } },
                        placeholder = "10",
                        teclado = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            erro?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = OrbitTokens.danger, fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrbitTokens.bluePastel)
                    .orbitPressable {
                        val saldo = parsearReaisParaCentavos(saldoTexto)
                        if (saldo == null) {
                            erro = "Saldo inválido"
                            return@orbitPressable
                        }
                        val limite = if (tipo == TipoCarteira.CARTAO_CREDITO && limiteTexto.isNotBlank()) {
                            parsearReaisParaCentavos(limiteTexto).also {
                                if (it == null) {
                                    erro = "Limite inválido"
                                    return@orbitPressable
                                }
                            }
                        } else null
                        val rascunho = CarteiraRascunho(
                            tipo = tipo,
                            banco = banco,
                            apelido = apelido,
                            cor = cor,
                            ultimos4 = ultimos4.takeIf { it.isNotBlank() },
                            limiteCentavos = limite,
                            fechamentoDia = fechamento.toIntOrNull(),
                            vencimentoDia = vencimento.toIntOrNull(),
                            saldoInicialCentavos = saldo,
                        )
                        if (!rascunho.valido()) {
                            erro = "Preenche o apelido (e dias 1–31 no crédito)"
                            return@orbitPressable
                        }
                        erro = null
                        onSalvar(rascunho)
                    }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Salvar",
                    color = OrbitTokens.onBluePastel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Cancelar",
                color = OrbitTokens.textMidN,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .orbitPressable(onClick = onDismiss)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun ChipTipo(
    label: String,
    valor: String,
    atual: String,
    onEscolher: (String) -> Unit,
) {
    val ativo = atual == valor
    Text(
        label,
        color = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.textMidN,
        fontSize = 13.sp,
        fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (ativo) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
            .orbitPressable { onEscolher(valor) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun CampoTexto(
    rotulo: String,
    valor: String,
    onValor: (String) -> Unit,
    placeholder: String,
    teclado: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(rotulo, color = OrbitTokens.textLowN, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = valor,
            onValueChange = onValor,
            singleLine = true,
            textStyle = TextStyle(
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
            ),
            cursorBrush = SolidColor(OrbitTokens.bluePastel),
            keyboardOptions = KeyboardOptions(
                keyboardType = teclado,
                capitalization = if (teclado == KeyboardType.Text) {
                    KeyboardCapitalization.Sentences
                } else {
                    KeyboardCapitalization.None
                },
            ),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrbitTokens.graphiteRaised)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (valor.isEmpty()) {
                        Text(placeholder, color = OrbitTokens.textLowN, fontSize = 15.sp)
                    }
                    inner()
                }
            },
        )
    }
}
