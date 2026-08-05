package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.financas.CategoriasFinanca
import com.ethan.orbitlab.data.financas.FatiaCategoria
import com.ethan.orbitlab.data.financas.FinancasRepository
import com.ethan.orbitlab.data.financas.FirestoreFinancas
import com.ethan.orbitlab.data.financas.MetaFinanceira
import com.ethan.orbitlab.data.financas.MetaFinanceiraRascunho
import com.ethan.orbitlab.data.financas.PeriodoExtrato
import com.ethan.orbitlab.data.financas.ProgressoMeta
import com.ethan.orbitlab.data.financas.TipoMeta
import com.ethan.orbitlab.data.financas.faixaDoPeriodo
import com.ethan.orbitlab.data.financas.filtrarPorPeriodo
import com.ethan.orbitlab.data.financas.filtrarPorPeriodoAteHoje
import com.ethan.orbitlab.data.financas.formatarReais
import com.ethan.orbitlab.data.financas.gastoPorCategoria
import com.ethan.orbitlab.data.financas.parsearReaisParaCentavos
import com.ethan.orbitlab.data.financas.progressosMetas
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val CardRadius = 22.dp

/**
 * Metas — polish no idioma das outras abas (sem concept próprio: insight + cards + relatório).
 */
@Composable
fun MetasScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val metas by FinancasRepository.metas.collectAsState()
    val lancamentos by FinancasRepository.lancamentos.collectAsState()
    val scope = rememberCoroutineScope()
    val agora = System.currentTimeMillis()
    val doMes = remember(lancamentos) {
        filtrarPorPeriodoAteHoje(lancamentos, faixaDoPeriodo(PeriodoExtrato.MES, agora), agora)
    }
    val progressos = remember(metas, doMes) { progressosMetas(metas, doMes) }
    val fatias = remember(doMes) { gastoPorCategoria(doMes) }
    val insight = remember(progressos) { insightMetas(progressos) }
    val tetoMes = progressos.firstOrNull { it.meta.tipo == TipoMeta.GASTO_MES }
    val reservas = progressos.filter { it.meta.tipo == TipoMeta.RESERVA }
    var formulario by remember { mutableStateOf<FormularioMeta?>(null) }
    var somarAlvo by remember { mutableStateOf<MetaFinanceira?>(null) }

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
                    "Metas",
                    color = OrbitTokens.textHiN,
                    fontSize = 28.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.orbitEnter(0),
                )
            }

            if (insight != null) {
                item { CardInsightMetas(insight) }
            }

            if (progressos.isNotEmpty()) {
                item {
                    ResumoMetasStrip(
                        qtd = progressos.size,
                        tetoLivre = tetoMes?.let {
                            (it.alvoCentavos - it.atualCentavos).coerceAtLeast(0L)
                        },
                        reservaPct = reservas.firstOrNull()?.let {
                            (it.pct * 100).toInt().coerceAtMost(999)
                        },
                    )
                }
            }

            if (progressos.isEmpty()) {
                item {
                    VazioMetas(onNova = { formulario = FormularioMeta.Nova })
                }
            } else {
                item {
                    Text(
                        "Suas metas",
                        color = OrbitTokens.textHiN,
                        fontSize = 17.sp,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(progressos, key = { it.meta.id }) { p ->
                    CardMetaConcept(
                        progresso = p,
                        onClick = { formulario = FormularioMeta.Editar(p.meta) },
                        onSomar = if (p.meta.tipo == TipoMeta.RESERVA) {
                            { somarAlvo = p.meta }
                        } else {
                            null
                        },
                    )
                }
            }

            item {
                Text(
                    "Por categoria (mês)",
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                if (fatias.isEmpty()) {
                    Text(
                        "Sem saídas neste mês ainda — o relatório aparece quando tiver gasto.",
                        color = OrbitTokens.textMidN,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CardRadius))
                            .background(OrbitTokens.graphiteSurf)
                            .padding(16.dp),
                    )
                } else {
                    CardRelatorioCategorias(fatias)
                }
            }

            item { CardOpenFinanceStub() }
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
                .orbitPressable { formulario = FormularioMeta.Nova }
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
                "Nova meta",
                color = OrbitTokens.onBluePastel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    formulario?.let { form ->
        MetaFormSheet(
            inicial = when (form) {
                FormularioMeta.Nova -> null
                is FormularioMeta.Editar -> form.meta
            },
            onDismiss = { formulario = null },
            onSalvar = { rascunho ->
                val u = uid ?: return@MetaFormSheet
                scope.launch {
                    when (form) {
                        FormularioMeta.Nova -> FirestoreFinancas.criarMeta(u, rascunho)
                        is FormularioMeta.Editar ->
                            FirestoreFinancas.atualizarMeta(u, form.meta.id, rascunho)
                    }
                    formulario = null
                }
            },
            onApagar = (form as? FormularioMeta.Editar)?.let { ed ->
                {
                    val u = uid ?: return@let
                    scope.launch {
                        FirestoreFinancas.apagarMeta(u, ed.meta.id)
                        formulario = null
                    }
                }
            },
        )
    }

    somarAlvo?.let { meta ->
        SomarReservaSheet(
            meta = meta,
            onDismiss = { somarAlvo = null },
            onConfirmar = { delta ->
                val u = uid ?: return@SomarReservaSheet
                scope.launch {
                    FirestoreFinancas.atualizarMeta(
                        u,
                        meta.id,
                        MetaFinanceiraRascunho(
                            apelido = meta.apelido,
                            tipo = meta.tipo,
                            alvoCentavos = meta.alvoCentavos,
                            atualCentavos = (meta.atualCentavos + delta).coerceAtLeast(0L),
                            categoria = meta.categoria,
                            ativa = meta.ativa,
                        ),
                    )
                    somarAlvo = null
                }
            },
        )
    }
}

private sealed class FormularioMeta {
    data object Nova : FormularioMeta()
    data class Editar(val meta: MetaFinanceira) : FormularioMeta()
}

private data class InsightMetas(val destaque: String, val resto: String)

private fun insightMetas(progressos: List<ProgressoMeta>): InsightMetas? {
    if (progressos.isEmpty()) return null
    val teto = progressos.firstOrNull { it.meta.tipo == TipoMeta.GASTO_MES }
    if (teto != null) {
        val pct = (teto.pct * 100).toInt().coerceAtMost(999)
        val livre = (teto.alvoCentavos - teto.atualCentavos).coerceAtLeast(0L)
        return if (teto.pct > 1f) {
            InsightMetas(
                destaque = "$pct%",
                resto = " do teto do mês já foi — você passou do limite. Hora de frear ou ajustar a meta.",
            )
        } else {
            InsightMetas(
                destaque = "$pct%",
                resto = " do teto do mês já foi. Sobram ${formatarReais(livre)} pra gastar sem estourar.",
            )
        }
    }
    val reserva = progressos
        .filter { it.meta.tipo == TipoMeta.RESERVA }
        .maxByOrNull { it.pct }
    if (reserva != null) {
        val falta = (reserva.alvoCentavos - reserva.atualCentavos).coerceAtLeast(0L)
        return if (reserva.pct >= 1f) {
            InsightMetas(
                destaque = reserva.meta.apelido,
                resto = " já bateu o alvo. Parabéns — quer subir a barra?",
            )
        } else {
            InsightMetas(
                destaque = formatarReais(falta),
                resto = " pra fechar “${reserva.meta.apelido}”. Um pouco por vez fecha o ciclo.",
            )
        }
    }
    val corte = progressos.firstOrNull {
        it.meta.tipo == TipoMeta.CORTE && it.pct > 0.85f
    }
    if (corte != null) {
        val pct = (corte.pct * 100).toInt()
        return InsightMetas(
            destaque = "$pct%",
            resto = " do corte “${corte.meta.apelido}” já foi. De olho nessa categoria.",
        )
    }
    return InsightMetas(
        destaque = "${progressos.size}",
        resto = if (progressos.size == 1) {
            " meta ativa — o Painel usa o teto do mês quando você criar um."
        } else {
            " metas ativas. Reserva, teto e cortes — cada uma puxa o mês pra um lado."
        },
    )
}

@Composable
private fun CardInsightMetas(insight: InsightMetas) {
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
                            OrbitTokens.gold.copy(alpha = 0.85f),
                            OrbitTokens.goldFlat.copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Flag,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
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
                    append(insight.destaque)
                }
                withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 14.sp)) {
                    append(insight.resto)
                }
            },
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ResumoMetasStrip(
    qtd: Int,
    tetoLivre: Long?,
    reservaPct: Int?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColunaResumo(
            valor = "$qtd",
            label = if (qtd == 1) "meta ativa" else "metas ativas",
            cor = OrbitTokens.textHiN,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .width(1.dp)
                .height(36.dp)
                .background(OrbitTokens.graphiteHair),
        )
        ColunaResumo(
            valor = tetoLivre?.let { formatarReais(it) } ?: "—",
            label = "livre no teto",
            cor = OrbitTokens.bluePastel,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .width(1.dp)
                .height(36.dp)
                .background(OrbitTokens.graphiteHair),
        )
        ColunaResumo(
            valor = reservaPct?.let { "$it%" } ?: "—",
            label = "na reserva",
            cor = OrbitTokens.gold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ColunaResumo(
    valor: String,
    label: String,
    cor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            valor,
            color = cor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(label, color = OrbitTokens.textLowN, fontSize = 11.sp)
    }
}

@Composable
private fun VazioMetas(onNova: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(20.dp)
            .orbitEnter(2),
    ) {
        Text(
            "Nenhuma meta ainda",
            color = OrbitTokens.textHiN,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Cria uma reserva ou um teto de gasto — o anel do Painel passa a respeitar o teto do mês.",
            color = OrbitTokens.textMidN,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "+ Nova meta",
            color = OrbitTokens.bluePastel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.orbitPressable(onClick = onNova),
        )
    }
}

@Composable
private fun CardMetaConcept(
    progresso: ProgressoMeta,
    onClick: () -> Unit,
    onSomar: (() -> Unit)?,
) {
    val m = progresso.meta
    val estourada = progresso.pct > 1f && m.tipo != TipoMeta.RESERVA
    val corBarra = when {
        m.tipo == TipoMeta.RESERVA -> OrbitTokens.gold
        estourada -> OrbitTokens.danger
        else -> OrbitTokens.bluePastel
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .orbitPressable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    m.apelido,
                    color = OrbitTokens.textHiN,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(TipoMeta.rotulo(m.tipo))
                        if (m.tipo == TipoMeta.CORTE && m.categoria != null) {
                            append(" · ")
                            append(CategoriasFinanca.porId(m.categoria).rotulo)
                        }
                        if (progresso.derivado) append(" · do mês")
                    },
                    color = OrbitTokens.textLowN,
                    fontSize = 12.sp,
                )
            }
            Text(
                "${(progresso.pct * 100).toInt().coerceAtMost(999)}%",
                color = when {
                    m.tipo == TipoMeta.RESERVA && progresso.pct >= 1f -> OrbitTokens.online
                    estourada -> OrbitTokens.danger
                    else -> OrbitTokens.textHiN
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Bricolage,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(OrbitTokens.graphiteHair),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progresso.pct.coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(corBarra),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatarReais(progresso.atualCentavos),
                color = OrbitTokens.textHiN,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "de ${formatarReais(progresso.alvoCentavos)}",
                color = OrbitTokens.textLowN,
                fontSize = 13.sp,
            )
        }
        if (onSomar != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "+ Somar à reserva",
                color = OrbitTokens.bluePastel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.orbitPressable(onClick = onSomar),
            )
        }
    }
}

@Composable
private fun CardRelatorioCategorias(fatias: List<FatiaCategoria>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf),
    ) {
        fatias.forEachIndexed { i, fatia ->
            if (i > 0) {
                HorizontalDivider(
                    color = OrbitTokens.graphiteHair,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
            val cat = CategoriasFinanca.porId(fatia.categoriaId)
            LinhaCategoriaConcept(
                emoji = cat.emoji,
                nome = cat.rotulo,
                valor = fatia.valorCentavos,
                pct = fatia.pct,
            )
        }
    }
}

@Composable
private fun LinhaCategoriaConcept(
    emoji: String,
    nome: String,
    valor: Long,
    pct: Float,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$emoji  $nome",
                color = OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                formatarReais(valor),
                color = OrbitTokens.textHiN,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(OrbitTokens.graphiteHair),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(OrbitTokens.bluePastel),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${(pct * 100).toInt()}% do gasto",
            color = OrbitTokens.textLowN,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun CardOpenFinanceStub() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Open Finance",
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Em breve",
                color = OrbitTokens.textLowN,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(OrbitTokens.graphiteRaised)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Conectar o banco direto fica pra depois. Por agora a captura por notificação + o manual cobrem o dia a dia.",
            color = OrbitTokens.textMidN,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetaFormSheet(
    inicial: MetaFinanceira?,
    onDismiss: () -> Unit,
    onSalvar: (MetaFinanceiraRascunho) -> Unit,
    onApagar: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var apelido by remember { mutableStateOf(inicial?.apelido.orEmpty()) }
    var tipo by remember { mutableStateOf(inicial?.tipo ?: TipoMeta.RESERVA) }
    var alvoTxt by remember {
        mutableStateOf(
            inicial?.alvoCentavos?.let { if (it % 100 == 0L) (it / 100).toString() else "%d,%02d".format(it / 100, (it % 100).toInt()) }
                .orEmpty(),
        )
    }
    var atualTxt by remember {
        mutableStateOf(
            inicial?.atualCentavos?.takeIf { it > 0 }?.let {
                if (it % 100 == 0L) (it / 100).toString() else "%d,%02d".format(it / 100, (it % 100).toInt())
            }.orEmpty(),
        )
    }
    var categoria by remember {
        mutableStateOf(inicial?.categoria ?: CategoriasFinanca.alimentacao.id)
    }
    val alvo = parsearReaisParaCentavos(alvoTxt) ?: 0L
    val atual = parsearReaisParaCentavos(atualTxt) ?: 0L
    val rascunho = MetaFinanceiraRascunho(
        apelido = apelido,
        tipo = tipo,
        alvoCentavos = alvo,
        atualCentavos = if (tipo == TipoMeta.RESERVA) atual else 0L,
        categoria = if (tipo == TipoMeta.CORTE) categoria else null,
        ativa = inicial?.ativa ?: true,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.graphiteSurf,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                if (inicial == null) "Nova meta" else "Editar meta",
                color = OrbitTokens.textHiN,
                fontSize = 20.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Text("Tipo", color = OrbitTokens.textLowN, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(TipoMeta.RESERVA, TipoMeta.GASTO_MES, TipoMeta.CORTE).forEach { t ->
                    val on = tipo == t
                    Text(
                        TipoMeta.rotulo(t),
                        color = if (on) OrbitTokens.onBluePastel else OrbitTokens.textHiN,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (on) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
                            .orbitPressable(onClick = { tipo = t })
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            CampoTexto("Apelido", apelido, { apelido = it }, KeyboardCapitalization.Sentences)
            Spacer(Modifier.height(12.dp))
            CampoTexto("Alvo (R$)", alvoTxt, { alvoTxt = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, keyboard = KeyboardType.Decimal)
            if (tipo == TipoMeta.RESERVA) {
                Spacer(Modifier.height(12.dp))
                CampoTexto("Já guardado (R$)", atualTxt, { atualTxt = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, keyboard = KeyboardType.Decimal)
            }
            if (tipo == TipoMeta.CORTE) {
                Spacer(Modifier.height(12.dp))
                Text("Categoria", color = OrbitTokens.textLowN, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CategoriasFinanca.todas.filter { it.id != CategoriasFinanca.renda.id }.forEach { c ->
                        val on = categoria == c.id
                        Text(
                            "${c.emoji} ${c.rotulo}",
                            color = if (on) OrbitTokens.onBluePastel else OrbitTokens.textHiN,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (on) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
                                .orbitPressable(onClick = { categoria = c.id })
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (rascunho.valido()) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
                    .orbitPressable(enabled = rascunho.valido()) { onSalvar(rascunho) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Salvar",
                    color = if (rascunho.valido()) OrbitTokens.onBluePastel else OrbitTokens.textLowN,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (onApagar != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Apagar meta",
                    color = OrbitTokens.danger,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .orbitPressable(onClick = onApagar)
                        .padding(8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SomarReservaSheet(
    meta: MetaFinanceira,
    onDismiss: () -> Unit,
    onConfirmar: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var txt by remember { mutableStateOf("") }
    val delta = parsearReaisParaCentavos(txt) ?: 0L
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.graphiteSurf,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            Text("Somar à ${meta.apelido}", color = OrbitTokens.textHiN, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            CampoTexto("Valor (R$)", txt, { txt = it.filter { c -> c.isDigit() || c == ',' || c == '.' } }, keyboard = KeyboardType.Decimal)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (delta > 0) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
                    .orbitPressable(enabled = delta > 0) { onConfirmar(delta) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Somar", color = if (delta > 0) OrbitTokens.onBluePastel else OrbitTokens.textLowN, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CampoTexto(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    Text(label, color = OrbitTokens.textLowN, fontSize = 11.sp)
    Spacer(Modifier.height(6.dp))
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = 16.sp),
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboard,
        ),
        singleLine = true,
        cursorBrush = SolidColor(OrbitTokens.bluePastel),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(OrbitTokens.graphiteRaised)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}
