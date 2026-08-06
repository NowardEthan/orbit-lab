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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.financas.Carteira
import com.ethan.orbitlab.data.financas.CategoriasFinanca
import com.ethan.orbitlab.data.financas.FinancasRepository
import com.ethan.orbitlab.data.financas.FirestoreFinancas
import com.ethan.orbitlab.data.financas.Recorrente
import com.ethan.orbitlab.data.financas.RecorrenteRascunho
import com.ethan.orbitlab.data.financas.TipoLancamento
import com.ethan.orbitlab.data.financas.formatarReais
import com.ethan.orbitlab.data.financas.parsearReaisParaCentavos
import com.ethan.orbitlab.data.financas.resumoRecorrentes
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
/** Salmão pastel — “comprometido” no concept. */
private val SalmonPastel = Color(0xFFE8A090)

/**
 * Recorrentes — polish alinhado ao concept: insight, KPIs, entram/saem.
 */
@Composable
fun RecorrentesScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val carteiras by FinancasRepository.carteiras.collectAsState()
    val recorrentes by FinancasRepository.recorrentes.collectAsState()
    var formulario by remember { mutableStateOf<FormularioRecorrente?>(null) }
    val scope = rememberCoroutineScope()

    val ativos = remember(recorrentes) { recorrentes.filter { it.ativo } }
    val resumo = remember(ativos) { resumoRecorrentes(ativos) }
    val entram = remember(ativos) {
        ativos.filter { it.tipo == TipoLancamento.ENTRADA }
            .sortedBy { it.diaDoMes }
    }
    val saem = remember(ativos) {
        ativos.filter { it.tipo == TipoLancamento.SAIDA }
            .sortedBy { it.diaDoMes }
    }
    // P3.5: muda a chave quando virar o dia (chaveDia usa dia/mes/ano local).
    val mesRotulo = remember(System.currentTimeMillis() / 86_400_000L) {
        SimpleDateFormat("MMMM", Locale.forLanguageTag("pt-BR"))
            .format(Date())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("pt-BR")) else it.toString() }
    }
    val pctFixos = remember(resumo) {
        if (resumo.entramCentavos <= 0L) null
        else ((resumo.saemCentavos * 100) / resumo.entramCentavos).toInt().coerceIn(0, 999)
    }
    val fracComprometido = remember(resumo) {
        if (resumo.entramCentavos <= 0L) 0f
        else (resumo.saemCentavos.toFloat() / resumo.entramCentavos.toFloat()).coerceIn(0f, 1f)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = OrbitMetrics.pagePadding,
                end = OrbitMetrics.pagePadding,
                top = 2.dp,
                bottom = 120.dp,
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
                        "Recorrentes",
                        color = OrbitTokens.textHiN,
                        fontSize = 28.sp,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                        modifier = Modifier.orbitEnter(0),
                    )
                    PillMes(mesRotulo)
                }
            }

            if (ativos.isEmpty()) {
                item {
                    VazioRecorrentes(
                        onAdicionar = {
                            if (carteiras.isNotEmpty()) {
                                formulario = FormularioRecorrente.Novo
                            }
                        },
                    )
                }
            } else {
                if (pctFixos != null) {
                    item {
                        CardInsightRecorrentes(
                            pct = pctFixos,
                            sobra = resumo.sobraLivreCentavos,
                        )
                    }
                }

                item {
                    CardResumoMes(
                        entram = resumo.entramCentavos,
                        saem = resumo.saemCentavos,
                        sobra = resumo.sobraLivreCentavos,
                        fracComprometido = fracComprometido,
                    )
                }

                if (entram.isNotEmpty()) {
                    item {
                        CabecalhoSecao(
                            titulo = "Entram todo mês",
                            total = resumo.entramCentavos,
                            entrada = true,
                        )
                    }
                    items(entram, key = { it.id }) { r ->
                        LinhaRecorrenteConcept(
                            recorrente = r,
                            onClick = { formulario = FormularioRecorrente.Editar(r) },
                        )
                    }
                }

                if (saem.isNotEmpty()) {
                    item {
                        CabecalhoSecao(
                            titulo = "Saem todo mês",
                            total = resumo.saemCentavos,
                            entrada = false,
                        )
                    }
                    items(saem, key = { it.id }) { r ->
                        LinhaRecorrenteConcept(
                            recorrente = r,
                            onClick = { formulario = FormularioRecorrente.Editar(r) },
                        )
                    }
                }
            }

            item {
                Text(
                    "Recorrentes — o que entra e sai todo mês",
                    color = OrbitTokens.textLowN,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    textAlign = TextAlign.Center,
                )
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
                .orbitPressable {
                    if (carteiras.isEmpty()) return@orbitPressable
                    formulario = FormularioRecorrente.Novo
                }
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
                "Novo recorrente",
                color = OrbitTokens.onBluePastel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    formulario?.let { form ->
        RecorrenteFormSheet(
            carteiras = carteiras,
            inicial = when (form) {
                FormularioRecorrente.Novo -> null
                is FormularioRecorrente.Editar -> form.recorrente
            },
            onDismiss = { formulario = null },
            onSalvar = { rascunho ->
                val u = uid ?: return@RecorrenteFormSheet
                scope.launch {
                    when (form) {
                        FormularioRecorrente.Novo ->
                            FirestoreFinancas.criarRecorrente(u, rascunho)
                        is FormularioRecorrente.Editar ->
                            FirestoreFinancas.atualizarRecorrente(u, form.recorrente.id, rascunho)
                    }
                    formulario = null
                }
            },
            onDesativar = when (val f = form) {
                FormularioRecorrente.Novo -> null
                is FormularioRecorrente.Editar -> ({
                    val u = uid
                    if (u != null) {
                        scope.launch {
                            FirestoreFinancas.desativarRecorrente(u, f.recorrente.id)
                            formulario = null
                        }
                    }
                })
            },
        )
    }
}

private sealed class FormularioRecorrente {
    data object Novo : FormularioRecorrente()
    data class Editar(val recorrente: Recorrente) : FormularioRecorrente()
}

@Composable
private fun PillMes(rotulo: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(OrbitTokens.graphiteSurf)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            rotulo,
            color = OrbitTokens.textHiN,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CardInsightRecorrentes(pct: Int, sobra: Long) {
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
                withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 14.sp)) {
                    append("Seus fixos comem ")
                }
                withStyle(
                    SpanStyle(
                        color = OrbitTokens.textHiN,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                ) {
                    append("$pct%")
                }
                withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 14.sp)) {
                    append(" do que entra. Sobram ")
                }
                withStyle(
                    SpanStyle(
                        color = OrbitTokens.textHiN,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    ),
                ) {
                    append(formatarReais(sobra))
                }
                withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 14.sp)) {
                    append(" pro mês — pra viver ou reforçar a reserva.")
                }
            },
            lineHeight = 20.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CardResumoMes(
    entram: Long,
    saem: Long,
    sobra: Long,
    fracComprometido: Float,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(16.dp)
            .orbitEnter(2),
    ) {
        Row(Modifier.fillMaxWidth()) {
            ColunaKpi(
                valor = formatarReais(entram),
                label = "entram fixos",
                cor = OrbitTokens.online,
                modifier = Modifier.weight(1f),
            )
            ColunaKpi(
                valor = formatarReais(saem),
                label = "saem fixos",
                cor = OrbitTokens.textHiN,
                modifier = Modifier.weight(1f),
            )
            ColunaKpi(
                valor = formatarReais(sobra),
                label = "sobra livre",
                cor = OrbitTokens.bluePastel,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        // Barra bi-cor: salmão (comprometido) + verde (livre)
        Row(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp)),
        ) {
            val c = fracComprometido.coerceIn(0.02f, 0.98f)
            Box(
                Modifier
                    .weight(c)
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(SalmonPastel),
            )
            Box(
                Modifier
                    .weight((1f - c).coerceAtLeast(0.02f))
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(OrbitTokens.online),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendaPonto("Comprometido", SalmonPastel)
            LegendaPonto("Livre", OrbitTokens.online)
        }
    }
}

@Composable
private fun ColunaKpi(
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
private fun LegendaPonto(label: String, cor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(cor),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = OrbitTokens.textMidN, fontSize = 12.sp)
    }
}

@Composable
private fun CabecalhoSecao(titulo: String, total: Long, entrada: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            titulo,
            color = OrbitTokens.textHiN,
            fontSize = 17.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            buildString {
                append(if (entrada) "+ " else "− ")
                append(formatarReais(total))
            },
            color = if (entrada) OrbitTokens.online else OrbitTokens.textHiN,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LinhaRecorrenteConcept(
    recorrente: Recorrente,
    onClick: () -> Unit,
) {
    val entrada = recorrente.tipo == TipoLancamento.ENTRADA
    val categoria = CategoriasFinanca.porId(recorrente.categoria)
    val corPonto = if (entrada) OrbitTokens.online else corDoTom(categoria.tom)
    val valorLabel = buildString {
        if (entrada) append("+ ")
        if (recorrente.variavel) append("~ ")
        append(formatarReais(recorrente.valorCentavos))
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrbitTokens.graphiteSurf)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .padding(end = 12.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(corPonto),
        )
        Column(Modifier.weight(1f)) {
            Text(
                recorrente.apelido,
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            if (entrada) {
                Text(
                    "${categoria.rotulo} · todo dia ${recorrente.diaDoMes}",
                    color = OrbitTokens.textLowN,
                    fontSize = 12.sp,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildString {
                            append(categoria.rotulo)
                            if (recorrente.variavel) append(" · variável")
                        },
                        color = OrbitTokens.textLowN,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "dia ${recorrente.diaDoMes}",
                        color = OrbitTokens.textMidN,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(OrbitTokens.graphiteRaised)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Text(
            valorLabel,
            color = if (entrada) OrbitTokens.online else OrbitTokens.textHiN,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun VazioRecorrentes(onAdicionar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(20.dp)
            .orbitEnter(2),
    ) {
        Text(
            "Nenhum fixo ainda",
            color = OrbitTokens.textHiN,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Salário, aluguel, internet… cadastra aqui. No dia, o Orbit gera o lançamento sozinho.",
            color = OrbitTokens.textMidN,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "+ Novo recorrente",
            color = OrbitTokens.bluePastel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.orbitPressable(onClick = onAdicionar),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecorrenteFormSheet(
    carteiras: List<Carteira>,
    inicial: Recorrente?,
    onDismiss: () -> Unit,
    onSalvar: (RecorrenteRascunho) -> Unit,
    onDesativar: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tipo by remember { mutableStateOf(inicial?.tipo ?: TipoLancamento.SAIDA) }
    var apelido by remember { mutableStateOf(inicial?.apelido.orEmpty()) }
    var valorTexto by remember {
        mutableStateOf(
            inicial?.valorCentavos
                ?.let { formatarReais(it).removePrefix("R$\u00A0").removePrefix("R$ ") }
                .orEmpty(),
        )
    }
    var diaTexto by remember { mutableStateOf(inicial?.diaDoMes?.toString() ?: "1") }
    var categoria by remember {
        mutableStateOf(inicial?.categoria ?: CategoriasFinanca.padraoPara(tipo).id)
    }
    var carteiraId by remember {
        mutableStateOf(inicial?.carteiraId ?: carteiras.firstOrNull()?.id.orEmpty())
    }
    var variavel by remember { mutableStateOf(inicial?.variavel ?: false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val categoriasVisiveis = remember(tipo) {
        if (tipo == TipoLancamento.ENTRADA) {
            listOf(CategoriasFinanca.renda, CategoriasFinanca.outros)
        } else {
            CategoriasFinanca.todas.filter { it.id != CategoriasFinanca.renda.id }
        }
    }

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
                .padding(top = 8.dp, bottom = 20.dp),
        ) {
            Text(
                if (inicial == null) "Novo recorrente" else "Editar recorrente",
                color = OrbitTokens.textHiN,
                fontSize = 20.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipSel("Saída", tipo == TipoLancamento.SAIDA) {
                    tipo = TipoLancamento.SAIDA
                    if (categoria == CategoriasFinanca.renda.id) {
                        categoria = CategoriasFinanca.contas.id
                    }
                }
                ChipSel("Entrada", tipo == TipoLancamento.ENTRADA) {
                    tipo = TipoLancamento.ENTRADA
                    categoria = CategoriasFinanca.renda.id
                }
            }

            Spacer(Modifier.height(14.dp))
            Campo("Apelido", apelido, { apelido = it }, "Ex.: Aluguel")
            Spacer(Modifier.height(12.dp))
            Campo("Valor", valorTexto, { valorTexto = it }, "0,00", KeyboardType.Decimal)
            Spacer(Modifier.height(12.dp))
            Campo(
                "Dia do mês",
                diaTexto,
                { if (it.length <= 2) diaTexto = it.filter { ch -> ch.isDigit() } },
                "10",
                KeyboardType.Number,
            )

            Spacer(Modifier.height(14.dp))
            Text("Categoria", color = OrbitTokens.textLowN, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categoriasVisiveis.forEach { cat ->
                    ChipSel("${cat.emoji} ${cat.rotulo}", categoria == cat.id) {
                        categoria = cat.id
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Carteira", color = OrbitTokens.textLowN, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                carteiras.forEach { c ->
                    ChipSel(c.apelido, carteiraId == c.id) { carteiraId = c.id }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Valor variável", color = OrbitTokens.textHiN, fontSize = 14.sp)
                    Text(
                        "Estimativa (ex.: energia ~)",
                        color = OrbitTokens.textLowN,
                        fontSize = 12.sp,
                    )
                }
                Switch(
                    checked = variavel,
                    onCheckedChange = { variavel = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OrbitTokens.onBluePastel,
                        checkedTrackColor = OrbitTokens.bluePastel,
                        uncheckedThumbColor = OrbitTokens.textMidN,
                        uncheckedTrackColor = OrbitTokens.graphiteRaised,
                    ),
                )
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
                        val valor = parsearReaisParaCentavos(valorTexto)
                        val dia = diaTexto.toIntOrNull()
                        if (valor == null || valor <= 0L) {
                            erro = "Informa um valor maior que zero"
                            return@orbitPressable
                        }
                        if (dia == null || dia !in 1..31) {
                            erro = "Dia do mês entre 1 e 31"
                            return@orbitPressable
                        }
                        if (carteiraId.isBlank()) {
                            erro = "Escolhe uma carteira"
                            return@orbitPressable
                        }
                        val rascunho = RecorrenteRascunho(
                            tipo = tipo,
                            valorCentavos = valor,
                            diaDoMes = dia,
                            categoria = categoria,
                            carteiraId = carteiraId,
                            apelido = apelido,
                            variavel = variavel,
                            ativo = true,
                        )
                        if (!rascunho.valido()) {
                            erro = "Preenche apelido e os campos"
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

            if (onDesativar != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Desativar fixo",
                    color = OrbitTokens.danger,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .orbitPressable(onClick = onDesativar)
                        .padding(8.dp),
                )
            }

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
private fun ChipSel(label: String, ativo: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.textMidN,
        fontSize = 13.sp,
        fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (ativo) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun Campo(
    rotulo: String,
    valor: String,
    onValor: (String) -> Unit,
    placeholder: String,
    teclado: KeyboardType = KeyboardType.Text,
) {
    Text(rotulo, color = OrbitTokens.textLowN, fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    BasicTextField(
        value = valor,
        onValueChange = onValor,
        singleLine = true,
        textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = 15.sp),
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
