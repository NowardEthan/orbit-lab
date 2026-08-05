package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.captura.CapturaRepository
import com.ethan.orbitlab.data.financas.CategoriasFinanca
import com.ethan.orbitlab.data.financas.FinancasDestino
import com.ethan.orbitlab.data.financas.FinancasLuzEngine
import com.ethan.orbitlab.data.financas.FinancasNav
import com.ethan.orbitlab.data.financas.FinancasRepository
import com.ethan.orbitlab.data.financas.FirestoreFinancas
import com.ethan.orbitlab.data.financas.Lancamento
import com.ethan.orbitlab.data.financas.LancamentoRascunho
import com.ethan.orbitlab.data.financas.PeriodoExtrato
import com.ethan.orbitlab.data.financas.TipoLancamento
import com.ethan.orbitlab.data.financas.TipoMeta
import com.ethan.orbitlab.data.financas.faixaDoMesOffset
import com.ethan.orbitlab.data.financas.faixaDoPeriodo
import com.ethan.orbitlab.data.financas.filtrarPorPeriodoAteHoje
import com.ethan.orbitlab.data.financas.filtrarPorPeriodo
import com.ethan.orbitlab.data.financas.formatarReais
import com.ethan.orbitlab.data.financas.gastoPorDiaDoMes
import com.ethan.orbitlab.data.financas.gerarInsightFinancas
import com.ethan.orbitlab.data.financas.inicioDoMes
import com.ethan.orbitlab.data.financas.lancamentoJaConta
import com.ethan.orbitlab.data.financas.lancamentosFuturos
import com.ethan.orbitlab.data.financas.lancamentosQueJaContam
import com.ethan.orbitlab.data.financas.metaGastoMes
import com.ethan.orbitlab.data.financas.offsetPeriodoComMovimento
import com.ethan.orbitlab.data.financas.progressosMetas
import com.ethan.orbitlab.data.financas.resumoDoPeriodo
import com.ethan.orbitlab.data.financas.resumoRecorrentes
import com.ethan.orbitlab.data.financas.rotuloFaixaExtrato
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val CardRadius = 24.dp
private val KpiRadius = 20.dp

/**
 * Painel Finanças — polish visual alinhado ao concept (grafite + azul pastel).
 */
@Composable
fun FinancasDashboardScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val carteiras by FinancasRepository.carteiras.collectAsState()
    val lancamentos by FinancasRepository.lancamentos.collectAsState()
    val recorrentes by FinancasRepository.recorrentes.collectAsState()
    val metas by FinancasRepository.metas.collectAsState()
    val luz by FinancasLuzEngine.estado.collectAsState()
    val capturaPendentes by CapturaRepository.pendentes.collectAsState()
    val scope = rememberCoroutineScope()
    /** null = segue o mês com movimento; Int = escolha manual no picker. */
    var offsetMesManual by remember { mutableStateOf<Int?>(null) }
    var formulario by remember { mutableStateOf<Lancamento?>(null) }
    var pickerMes by remember { mutableStateOf(false) }

    val agora = System.currentTimeMillis()
    val lancamentosEfetivos = remember(lancamentos, agora) { lancamentosQueJaContam(lancamentos, agora) }
    val lancamentosPlanejados = remember(lancamentos, agora) { lancamentosFuturos(lancamentos, agora) }
    // Síncrono (não LaunchedEffect): no 1º frame com dados o resumo já usa julho se agosto
    // estiver vazio — senão o painel pintava R$ 0 e parecia que «não registrou».
    // Chave por size+max data — evita remember “preso” se a lista mudar de identidade sem equals.
    val chaveLanc = remember(lancamentosEfetivos) {
        "${lancamentosEfetivos.size}:${lancamentosEfetivos.maxOfOrNull { it.dataMs } ?: 0L}"
    }
    val offsetMesAuto = remember(chaveLanc) {
        offsetPeriodoComMovimento(PeriodoExtrato.MES, lancamentosEfetivos, System.currentTimeMillis())
    }
    val offsetMes = offsetMesManual ?: offsetMesAuto
    val faixaMes = remember(offsetMes, chaveLanc) {
        faixaDoMesOffset(offsetMes, System.currentTimeMillis())
    }
    val faixaMesAnt = remember(offsetMes, chaveLanc) {
        faixaDoMesOffset(offsetMes - 1, System.currentTimeMillis())
    }
    val refMesMs = faixaMes.inicioMs
    val doMes = remember(chaveLanc, faixaMes, agora) { filtrarPorPeriodoAteHoje(lancamentos, faixaMes, agora) }
    val doMesAnt = remember(chaveLanc, faixaMesAnt, agora) { filtrarPorPeriodoAteHoje(lancamentos, faixaMesAnt, agora) }
    val planejadosDoMes = remember(lancamentosPlanejados, faixaMes) {
        filtrarPorPeriodo(lancamentosPlanejados, faixaMes)
    }
    val resumo = remember(doMes) { resumoDoPeriodo(doMes) }
    val meta = remember(recorrentes, metas) { metaGastoMes(recorrentes, metas) }
    val pctMeta = if (meta > 0) (resumo.saiuCentavos.toFloat() / meta.toFloat()).coerceIn(0f, 1.2f) else 0f
    val dentroMeta = meta > 0L && resumo.saiuCentavos <= meta
    val ofensiva = luz.ofensiva
    val insight = remember(doMes, doMesAnt, recorrentes, meta) {
        gerarInsightFinancas(doMes, doMesAnt, recorrentes, meta)
    }
    val hojeIni = remember { faixaDoPeriodo(PeriodoExtrato.DIA, agora) }
    val deHoje = remember(lancamentosEfetivos) {
        filtrarPorPeriodo(lancamentosEfetivos, hojeIni).sortedByDescending { it.dataMs }
    }
    // Lista contínua: se hoje está vazio, mostra os mais recentes (não some o histórico).
    val listaPainel = remember(deHoje, lancamentosEfetivos) {
        if (deHoje.isNotEmpty()) deHoje
        else lancamentosEfetivos.sortedByDescending { it.dataMs }
    }
    val tituloListaPainel = if (deHoje.isNotEmpty()) "Hoje" else "Recentes"
    val pendentes = remember(doMes, planejadosDoMes) {
        (doMes + planejadosDoMes).filter { !it.pago && it.tipo == TipoLancamento.SAIDA }
    }
    val contasPagarCentavos = remember(pendentes) { pendentes.sumOf { it.valorCentavos } }
    val fixos = remember(recorrentes) { resumoRecorrentes(recorrentes.filter { it.ativo }) }
    val progressos = remember(metas, doMes) { progressosMetas(metas, doMes) }
    val reservaMeta = progressos.firstOrNull { it.meta.tipo == TipoMeta.RESERVA }
    val reservaPct = remember(reservaMeta, fixos) {
        when {
            reservaMeta != null && reservaMeta.alvoCentavos > 0 ->
                ((reservaMeta.atualCentavos * 100) / reservaMeta.alvoCentavos).toInt().coerceIn(0, 100)
            fixos.entramCentavos <= 0L -> 0
            else -> ((fixos.sobraLivreCentavos * 100) / fixos.entramCentavos).toInt().coerceIn(0, 100)
        }
    }
    val serie = remember(lancamentosEfetivos, refMesMs) { gastoPorDiaDoMes(lancamentosEfetivos, refMesMs) }
    val carteiraPorId = remember(carteiras) { carteiras.associateBy { it.id } }
    val mesLabel = remember(refMesMs) {
        SimpleDateFormat("MMMM", Locale.forLanguageTag("pt-BR"))
            .format(Date(refMesMs))
            .replaceFirstChar { it.titlecase(Locale.forLanguageTag("pt-BR")) }
    }
    val contasLabel = if (pendentes.isEmpty()) {
        "—"
    } else {
        val curto = formatarReais(contasPagarCentavos)
            .replace("R$\u00A0", "")
            .replace("R$ ", "")
        "${pendentes.size} · R$ $curto"
    }

    fun marcarPago(lanc: Lancamento) {
        val u = uid ?: return
        scope.launch {
            FirestoreFinancas.atualizarLancamento(
                u,
                lanc.id,
                LancamentoRascunho(
                    tipo = lanc.tipo,
                    valorCentavos = lanc.valorCentavos,
                    dataMs = lanc.dataMs,
                    descricao = lanc.descricao,
                    categoria = lanc.categoria,
                    carteiraId = lanc.carteiraId,
                    recorrenteId = lanc.recorrenteId,
                    origem = lanc.origem,
                    capturaRaw = lanc.capturaRaw,
                    tags = lanc.tags,
                    pago = true,
                ),
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OrbitMetrics.pagePadding,
            end = OrbitMetrics.pagePadding,
            top = 2.dp,
            bottom = 36.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Finanças",
                    color = OrbitTokens.textHiN,
                    fontSize = 28.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.orbitEnter(0),
                )
                PillMes(
                    mesLabel,
                    Modifier
                        .orbitEnter(1)
                        .orbitPressable { pickerMes = true },
                )
            }
        }

        item {
            CardInsightConcept(
                destaque = insight.titulo,
                apoio = insight.corpo,
            )
        }

        if (capturaPendentes.isNotEmpty()) {
            item {
                Text(
                    "Luna viu ${capturaPendentes.size} compra(s) — toque pra confirmar.",
                    color = OrbitTokens.onBluePastel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CardRadius))
                        .background(OrbitTokens.bluePastel)
                        .orbitPressable { FinancasNav.abrir(FinancasDestino.CAPTURA) }
                        .padding(14.dp),
                )
            }
        }

        item {
            CardAnelMetaConcept(
                gasto = resumo.saiuCentavos,
                meta = meta,
                pct = pctMeta,
                dentroMeta = dentroMeta,
                ofensiva = ofensiva,
            )
        }

        item {
            Text(
                if (offsetMes == 0) "Este mês" else mesLabel,
                color = OrbitTokens.textHiN,
                fontSize = 17.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KpiTile(
                    icone = Icons.Rounded.KeyboardArrowUp,
                    iconeBg = OrbitTokens.online,
                    valor = formatarReais(resumo.entrouCentavos),
                    label = "entrou",
                    labelCor = OrbitTokens.textLowN,
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    icone = Icons.Rounded.KeyboardArrowDown,
                    iconeBg = OrbitTokens.danger,
                    valor = formatarReais(resumo.saiuCentavos),
                    label = "saiu",
                    labelCor = OrbitTokens.textLowN,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KpiTile(
                    icone = Icons.Rounded.CreditCard,
                    iconeBg = OrbitTokens.graphiteRaised,
                    valor = contasLabel,
                    label = "contas a pagar",
                    labelCor = if (pendentes.isEmpty()) OrbitTokens.textLowN else OrbitTokens.danger,
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    icone = Icons.Rounded.Star,
                    iconeBg = OrbitTokens.graphiteRaised,
                    valor = "$reservaPct%",
                    label = "reserva",
                    labelCor = OrbitTokens.textLowN,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tituloListaPainel,
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Tudo",
                    color = OrbitTokens.bluePastel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.orbitPressable {
                        FinancasNav.abrir(FinancasDestino.EXTRATO)
                    },
                )
            }
        }

        if (listaPainel.isEmpty() && pendentes.none { it.dataMs >= hojeIni.inicioMs }) {
            item {
                Text(
                    "Nada por aqui ainda — o Extrato e o Registrar cobrem o resto.",
                    color = OrbitTokens.textMidN,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CardRadius))
                        .background(OrbitTokens.graphiteSurf)
                        .padding(16.dp),
                )
            }
        } else {
            items(listaPainel.take(8), key = { it.id }) { lanc ->
                LinhaHojeConcept(
                    lancamento = lanc,
                    carteiraNome = carteiraPorId[lanc.carteiraId]?.apelido,
                    onClick = { formulario = lanc },
                    onMarcarPago = if (!lanc.pago) {{ marcarPago(lanc) }} else null,
                )
            }
            val outrasPendentes = pendentes.filter { it.id !in deHoje.map { h -> h.id }.toSet() }.take(4)
            items(outrasPendentes, key = { "p-${it.id}" }) { lanc ->
                LinhaHojeConcept(
                    lancamento = lanc,
                    carteiraNome = carteiraPorId[lanc.carteiraId]?.apelido,
                    destaqueVencendo = true,
                    onClick = { formulario = lanc },
                    onMarcarPago = { marcarPago(lanc) },
                )
            }
        }

        if (progressos.isNotEmpty()) {
            item {
                Text(
                    "Metas",
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(progressos.take(3), key = { "m-${it.meta.id}" }) { p ->
                MiniBarraMeta(
                    titulo = p.meta.apelido,
                    atual = p.atualCentavos,
                    alvo = p.alvoCentavos,
                    pct = p.pct,
                    reserva = p.meta.tipo == TipoMeta.RESERVA,
                )
            }
        }

        item {
            Text(
                if (offsetMes == 0) "Gasto do mês" else "Gasto · $mesLabel",
                color = OrbitTokens.textHiN,
                fontSize = 17.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            GraficoBarrasMes(serie = serie)
        }
    }

    if (pickerMes) {
        MesOffsetSheet(
            offsetAtual = offsetMes,
            onEscolher = {
                offsetMesManual = it
                pickerMes = false
            },
            onDismiss = { pickerMes = false },
        )
    }

    formulario?.let { lanc ->
        RegistrarSheet(
            carteiras = carteiras.filter { !it.arquivada },
            inicial = lanc,
            onDismiss = { formulario = null },
            onSalvar = { resultado ->
                val u = uid ?: return@RegistrarSheet
                scope.launch {
                    FirestoreFinancas.atualizarLancamento(u, lanc.id, resultado.rascunho)
                    formulario = null
                }
            },
        )
    }
}

@Composable
private fun PillMes(mesLabel: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(OrbitTokens.graphiteSurf)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            mesLabel,
            color = OrbitTokens.textHiN,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = OrbitTokens.textMidN,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CardInsightConcept(destaque: String, apoio: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(16.dp)
            .orbitEnter(2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Orbe azul da Luna
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            OrbitTokens.bluePastel,
                            OrbitTokens.bluePastelDim.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.85f), OrbitTokens.bluePastel),
                        ),
                    ),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = OrbitTokens.textHiN,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                    ),
                ) {
                    append(destaque)
                }
                if (apoio.isNotBlank()) {
                    append(" ")
                    withStyle(
                        SpanStyle(
                            color = OrbitTokens.textMidN,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                        ),
                    ) {
                        append(apoio)
                    }
                }
            },
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CardAnelMetaConcept(
    gasto: Long,
    meta: Long,
    pct: Float,
    dentroMeta: Boolean,
    ofensiva: Int,
) {
    val pctMostrar = (pct * 100).toInt().coerceAtMost(999)
    val temMeta = meta > 0L
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(108.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(108.dp)) {
                val stroke = 11.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(
                    color = OrbitTokens.graphiteRaised,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    size = arcSize,
                    topLeft = topLeft,
                )
                drawArc(
                    color = when {
                        !temMeta -> OrbitTokens.graphiteHair
                        dentroMeta -> OrbitTokens.bluePastel
                        else -> OrbitTokens.danger
                    },
                    startAngle = -90f,
                    sweepAngle = if (temMeta) 360f * pct.coerceIn(0f, 1f) else 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    size = arcSize,
                    topLeft = topLeft,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (temMeta) "$pctMostrar%" else "R$",
                    color = OrbitTokens.textHiN,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Bricolage,
                )
                Text(
                    if (temMeta) "da meta" else "do mês",
                    color = OrbitTokens.textLowN,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = OrbitTokens.textHiN,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    ) {
                        append(formatarReais(gasto))
                    }
                    withStyle(
                        SpanStyle(
                            color = OrbitTokens.textLowN,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    ) {
                        if (temMeta) {
                            append(" / ")
                            append(formatarReais(meta).replace("R$\u00A0", "").replace("R$ ", ""))
                        }
                    }
                },
            )
            Text(
                if (temMeta) "gasto do mês" else "gasto realizado",
                color = OrbitTokens.textLowN,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        when {
                            !temMeta -> OrbitTokens.graphiteRaised
                            dentroMeta -> OrbitTokens.online
                            else -> OrbitTokens.danger
                        },
                    )
                    .orbitPressable { if (!temMeta) FinancasNav.abrir(FinancasDestino.METAS) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (temMeta && dentroMeta) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    when {
                        !temMeta -> "Definir orçamento"
                        dentroMeta -> "Dentro da meta"
                        else -> "Acima da meta"
                    },
                    color = if (temMeta) Color.White else OrbitTokens.textHiN,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (temMeta && ofensiva > 0) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            "$ofensiva dias",
                            color = OrbitTokens.textHiN,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "no orçamento",
                            color = OrbitTokens.textLowN,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiTile(
    icone: ImageVector,
    iconeBg: Color,
    valor: String,
    label: String,
    labelCor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .aspectRatio(1.05f)
            .clip(RoundedCornerShape(KpiRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icone,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Column {
            Text(
                valor,
                color = OrbitTokens.textHiN,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                color = labelCor,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun LinhaHojeConcept(
    lancamento: Lancamento,
    carteiraNome: String?,
    destaqueVencendo: Boolean = false,
    onClick: () -> Unit = {},
    onMarcarPago: (() -> Unit)? = null,
) {
    val cat = CategoriasFinanca.porId(lancamento.categoria)
    val entrada = lancamento.tipo == TipoLancamento.ENTRADA
    val futuro = !lancamentoJaConta(lancamento)
    val hora = remember(lancamento) {
        val ms = lancamento.createdAtMs.takeIf { it > 0 } ?: lancamento.dataMs
        SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(ms))
    }
    val dataCurta = remember(lancamento.dataMs) {
        SimpleDateFormat("d MMM", Locale.forLanguageTag("pt-BR"))
            .format(Date(lancamento.dataMs))
            .lowercase(Locale.forLanguageTag("pt-BR"))
    }
    val titulo = buildString {
        append(lancamento.descricao.ifBlank { cat.rotulo })
        if (carteiraNome != null && lancamento.descricao.isNotBlank()) {
            // concept: "Almoço · iFood" — descrição já pode trazer o estabelecimento
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .orbitPressable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when {
                        destaqueVencendo || !lancamento.pago -> OrbitTokens.danger
                        entrada -> OrbitTokens.online
                        else -> OrbitTokens.bluePastel
                    },
                ),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(cat.rotulo)
                    append(" · ")
                    append(if (futuro) dataCurta else hora)
                    if (futuro) append(" · planejado")
                    if (!lancamento.pago) append(" · a pagar")
                },
                color = if (!lancamento.pago) OrbitTokens.danger else OrbitTokens.textLowN,
                fontSize = 12.sp,
            )
        }
        Text(
            (if (entrada) "+ " else "— ") +
                formatarReais(lancamento.valorCentavos)
                    .replace("R$\u00A0", "R$ ")
                    .let { if (it.startsWith("R$")) it else "R$ $it" },
            color = OrbitTokens.textMidN,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
        if (onMarcarPago != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Marcar pago",
                color = OrbitTokens.bluePastel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(start = 20.dp)
                    .orbitPressable(onClick = onMarcarPago),
            )
        }
        if (lancamento.tags.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .padding(start = 20.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                lancamento.tags.take(4).forEach { tag ->
                    Text(
                        "#$tag",
                        color = OrbitTokens.bluePastel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(OrbitTokens.bluePastel.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MesOffsetSheet(
    offsetAtual: Int,
    onEscolher: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    // Linha do tempo contínua — não só «mês atual / passado».
    val agora = System.currentTimeMillis()
    val opcoes = (0 downTo -11).map { off ->
        val faixa = faixaDoMesOffset(off, agora)
        val label = when (off) {
            0 -> "Mês atual · ${rotuloFaixaExtrato(PeriodoExtrato.MES, faixa)}"
            -1 -> "Mês passado · ${rotuloFaixaExtrato(PeriodoExtrato.MES, faixa)}"
            else -> rotuloFaixaExtrato(PeriodoExtrato.MES, faixa)
        }
        off to label
    }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = OrbitTokens.graphiteSurf,
        contentColor = OrbitTokens.textHiN,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            Text(
                "Ver mês",
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            opcoes.forEach { (off, label) ->
                val on = offsetAtual == off
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
                        .orbitPressable { onEscolher(off) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniBarraMeta(
    titulo: String,
    atual: Long,
    alvo: Long,
    pct: Float,
    reserva: Boolean,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(titulo, color = OrbitTokens.textHiN, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "${(pct * 100).toInt().coerceAtMost(999)}%",
                color = if (!reserva && pct > 1f) OrbitTokens.danger else OrbitTokens.textMidN,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(OrbitTokens.graphiteHair),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct.coerceIn(0.02f, 1f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        when {
                            reserva -> OrbitTokens.gold
                            pct > 1f -> OrbitTokens.danger
                            else -> OrbitTokens.bluePastel
                        },
                    ),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${formatarReais(atual)} / ${formatarReais(alvo)}",
            color = OrbitTokens.textLowN,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun GraficoBarrasMes(serie: List<Pair<Int, Long>>) {
    val max = (serie.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(16.dp),
    ) {
        if (serie.isEmpty()) {
            Text("Sem dados ainda", color = OrbitTokens.textLowN, fontSize = 13.sp)
            return
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            serie.forEach { (_, valor) ->
                val h = ((valor.toFloat() / max.toFloat()) * 100f).coerceAtLeast(if (valor > 0) 4f else 2f)
                Box(
                    Modifier
                        .weight(1f)
                        .height(h.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (valor > 0) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised,
                        ),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("dia 1", color = OrbitTokens.textLowN, fontSize = 10.sp)
            Text("hoje", color = OrbitTokens.textLowN, fontSize = 10.sp)
        }
    }
}
