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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.financas.Carteira
import com.ethan.orbitlab.data.financas.CategoriasFinanca
import com.ethan.orbitlab.data.financas.FinancasRepository
import com.ethan.orbitlab.data.financas.FirestoreFinancas
import com.ethan.orbitlab.data.financas.GrupoDiaExtrato
import com.ethan.orbitlab.data.financas.Lancamento
import com.ethan.orbitlab.data.financas.LancamentoRascunho
import com.ethan.orbitlab.data.financas.PeriodoExtrato
import com.ethan.orbitlab.data.financas.RecorrenteRascunho
import com.ethan.orbitlab.data.financas.TipoLancamento
import com.ethan.orbitlab.data.financas.agruparPorDia
import com.ethan.orbitlab.data.financas.diaDoMesDe
import com.ethan.orbitlab.data.financas.faixaDoPeriodo
import com.ethan.orbitlab.data.financas.filtrarPorPeriodo
import com.ethan.orbitlab.data.financas.formatarReais
import com.ethan.orbitlab.data.financas.resumoDoPeriodo
import com.ethan.orbitlab.data.financas.rotuloDiaExtrato
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

private val CardRadius = 20.dp

/**
 * Movimentações (extrato) — polish alinhado ao concept.
 */
@Composable
fun ExtratoScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val carteiras by FinancasRepository.carteiras.collectAsState()
    val lancamentos by FinancasRepository.lancamentos.collectAsState()
    var periodo by remember { mutableStateOf(PeriodoExtrato.MES) }
    var formulario by remember { mutableStateOf<FormularioLancamento?>(null) }
    var filtrosAberto by remember { mutableStateOf(false) }
    var filtroCarteiraId by remember { mutableStateOf<String?>(null) }
    var filtroCategoriaId by remember { mutableStateOf<String?>(null) }
    var soPendentes by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val faixa = remember(periodo) { faixaDoPeriodo(periodo) }
    val doPeriodo = remember(lancamentos, faixa) { filtrarPorPeriodo(lancamentos, faixa) }
    val filtrados = remember(doPeriodo, filtroCarteiraId, filtroCategoriaId, soPendentes) {
        doPeriodo.filter { l ->
            (filtroCarteiraId == null || l.carteiraId == filtroCarteiraId) &&
                (filtroCategoriaId == null || l.categoria == filtroCategoriaId) &&
                (!soPendentes || !l.pago)
        }
    }
    val resumo = remember(filtrados) { resumoDoPeriodo(filtrados) }
    val grupos = remember(filtrados) { agruparPorDia(filtrados) }
    val carteiraPorId = remember(carteiras) { carteiras.associateBy { it.id } }
    val filtrosAtivos = filtroCarteiraId != null || filtroCategoriaId != null || soPendentes

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
                    pago = true,
                ),
            )
        }
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
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Movimentações",
                        color = OrbitTokens.textHiN,
                        fontSize = 28.sp,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                        modifier = Modifier.orbitEnter(0),
                    )
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = "Filtros",
                        tint = if (filtrosAtivos) OrbitTokens.bluePastel else OrbitTokens.textMidN,
                        modifier = Modifier
                            .size(22.dp)
                            .orbitPressable { filtrosAberto = true },
                    )
                }
                Spacer(Modifier.height(14.dp))
                FiltroPeriodo(atual = periodo, onEscolher = { periodo = it })
                Spacer(Modifier.height(14.dp))
                FaixaSaldoConcept(
                    entrou = resumo.entrouCentavos,
                    saiu = resumo.saiuCentavos,
                    saldo = resumo.saldoCentavos,
                )
                Spacer(Modifier.height(8.dp))
            }

            if (carteiras.isEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    AvisoSemCarteira()
                }
            } else if (grupos.isEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    ExtratoVazio(onRegistrar = { formulario = FormularioLancamento.Novo })
                }
            } else {
                grupos.forEach { grupo ->
                    item(key = "h-${grupo.diaMs}") {
                        CabecalhoDiaConcept(grupo)
                    }
                    itemsIndexed(
                        grupo.lancamentos,
                        key = { _, it -> it.id },
                    ) { index, lanc ->
                        Column {
                            LinhaLancamentoConcept(
                                lancamento = lanc,
                                carteira = carteiraPorId[lanc.carteiraId],
                                onClick = { formulario = FormularioLancamento.Editar(lanc) },
                                onMarcarPago = if (!lanc.pago) {
                                    { marcarPago(lanc) }
                                } else {
                                    null
                                },
                            )
                            if (index < grupo.lancamentos.lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 22.dp)
                                        .height(1.dp)
                                        .background(OrbitTokens.graphiteHair),
                                )
                            }
                        }
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
                .orbitPressable {
                    if (carteiras.isEmpty()) return@orbitPressable
                    formulario = FormularioLancamento.Novo
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
                "Registrar",
                color = OrbitTokens.onBluePastel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    if (filtrosAberto) {
        ExtratoFiltrosSheet(
            carteiras = carteiras.filter { !it.arquivada },
            carteiraId = filtroCarteiraId,
            categoriaId = filtroCategoriaId,
            soPendentes = soPendentes,
            onCarteira = { filtroCarteiraId = it },
            onCategoria = { filtroCategoriaId = it },
            onPendentes = { soPendentes = it },
            onLimpar = {
                filtroCarteiraId = null
                filtroCategoriaId = null
                soPendentes = false
            },
            onDismiss = { filtrosAberto = false },
        )
    }

    formulario?.let { form ->
        RegistrarSheet(
            carteiras = carteiras,
            inicial = when (form) {
                FormularioLancamento.Novo -> null
                is FormularioLancamento.Editar -> form.lancamento
            },
            onDismiss = { formulario = null },
            onSalvar = { resultado ->
                val u = uid ?: return@RegistrarSheet
                scope.launch {
                    var rascunho = resultado.rascunho
                    if (resultado.repetirTodoMes && form is FormularioLancamento.Novo) {
                        val recorrenteId = FirestoreFinancas.criarRecorrente(
                            u,
                            RecorrenteRascunho(
                                tipo = rascunho.tipo,
                                valorCentavos = rascunho.valorCentavos,
                                diaDoMes = diaDoMesDe(rascunho.dataMs),
                                categoria = rascunho.categoria,
                                carteiraId = rascunho.carteiraId,
                                apelido = rascunho.descricao,
                            ),
                        )
                        rascunho = rascunho.copy(recorrenteId = recorrenteId)
                    }
                    when (form) {
                        FormularioLancamento.Novo ->
                            FirestoreFinancas.criarLancamento(u, rascunho)
                        is FormularioLancamento.Editar ->
                            FirestoreFinancas.atualizarLancamento(u, form.lancamento.id, rascunho)
                    }
                    formulario = null
                }
            },
            onApagar = when (val f = form) {
                FormularioLancamento.Novo -> null
                is FormularioLancamento.Editar -> ({
                    val u = uid
                    if (u != null) {
                        scope.launch {
                            FirestoreFinancas.apagarLancamento(u, f.lancamento.id)
                            formulario = null
                        }
                    }
                })
            },
        )
    }
}


private sealed class FormularioLancamento {
    data object Novo : FormularioLancamento()
    data class Editar(val lancamento: Lancamento) : FormularioLancamento()
}

@Composable
private fun FiltroPeriodo(atual: PeriodoExtrato, onEscolher: (PeriodoExtrato) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrbitTokens.graphiteSurf)
            .padding(4.dp),
    ) {
        listOf(
            PeriodoExtrato.DIA to "Dia",
            PeriodoExtrato.SEMANA to "Semana",
            PeriodoExtrato.MES to "Mês",
        ).forEach { (valor, label) ->
            val ativo = atual == valor
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (ativo) Color(0xFF2A3140) else Color.Transparent,
                    )
                    .orbitPressable { onEscolher(valor) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (ativo) OrbitTokens.textHiN else OrbitTokens.textMidN,
                    fontSize = 14.sp,
                    fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun FaixaSaldoConcept(entrou: Long, saiu: Long, saldo: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColunaResumo(
            valor = formatarNumeroCurto(entrou),
            label = "entrou",
            cor = OrbitTokens.online,
            modifier = Modifier.weight(1f),
        )
        DivisorVertical()
        ColunaResumo(
            valor = formatarNumeroCurto(saiu),
            label = "saiu",
            cor = OrbitTokens.textHiN,
            modifier = Modifier.weight(1f),
        )
        DivisorVertical()
        ColunaResumo(
            valor = (if (saldo >= 0) "+" else "") + formatarNumeroCurto(saldo),
            label = "saldo",
            cor = OrbitTokens.bluePastel,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DivisorVertical() {
    Box(
        Modifier
            .width(1.dp)
            .height(36.dp)
            .background(OrbitTokens.graphiteHair),
    )
}

@Composable
private fun ColunaResumo(
    valor: String,
    label: String,
    cor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            valor,
            color = cor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = OrbitTokens.textLowN,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun CabecalhoDiaConcept(grupo: GrupoDiaExtrato) {
    val sinal = if (grupo.subtotalCentavos >= 0) "+ " else "− "
    val abs = kotlin.math.abs(grupo.subtotalCentavos)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            rotuloDiaExtrato(grupo.diaMs),
            color = OrbitTokens.textLowN,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.9.sp,
        )
        Text(
            sinal + formatarReais(abs),
            color = OrbitTokens.textLowN,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun LinhaLancamentoConcept(
    lancamento: Lancamento,
    carteira: Carteira?,
    onClick: () -> Unit,
    onMarcarPago: (() -> Unit)? = null,
) {
    val cat = CategoriasFinanca.porId(lancamento.categoria)
    val entrada = lancamento.tipo == TipoLancamento.ENTRADA
    val hora = remember(lancamento) {
        val ms = lancamento.createdAtMs.takeIf { it > 0 } ?: lancamento.dataMs
        SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(ms))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .orbitPressable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !lancamento.pago -> OrbitTokens.danger
                            entrada -> OrbitTokens.online
                            else -> corDoTom(cat.tom)
                        },
                    ),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    lancamento.descricao.ifBlank { cat.rotulo },
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
                        append(hora)
                        if (!lancamento.pago) append(" · pendente")
                    },
                    color = if (!lancamento.pago) OrbitTokens.warning else OrbitTokens.textLowN,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                (if (entrada) "+ " else "− ") + formatarReais(lancamento.valorCentavos),
                color = if (entrada) OrbitTokens.online else OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (onMarcarPago != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Marcar pago",
                color = OrbitTokens.bluePastel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(start = 22.dp)
                    .orbitPressable(onClick = onMarcarPago),
            )
        }
    }
}

@Composable
private fun ExtratoVazio(onRegistrar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(20.dp)
            .orbitEnter(2),
    ) {
        Text(
            "Nada neste período",
            color = OrbitTokens.textHiN,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Registra uma saída ou entrada — a Luna ajuda depois; agora a base é manual.",
            color = OrbitTokens.textMidN,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "+ Registrar",
            color = OrbitTokens.bluePastel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.orbitPressable(onClick = onRegistrar),
        )
    }
}

@Composable
private fun AvisoSemCarteira() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(20.dp),
    ) {
        Text(
            "Primeiro, uma carteira",
            color = OrbitTokens.textHiN,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Abre Cartões na gaveta e adiciona uma conta ou cartão. Depois volta pra registrar.",
            color = OrbitTokens.textMidN,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )
    }
}

/** Número curto tipo concept: 480000 → "4.800" */
private fun formatarNumeroCurto(centavos: Long): String {
    val abs = kotlin.math.abs(centavos)
    val reais = abs / 100
    val fmt = java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))
    val corpo = fmt.format(reais)
    return if (centavos < 0) "−$corpo" else corpo
}

internal fun corDoTom(tom: String): Color = when (tom) {
    "verde" -> OrbitTokens.online
    "vermelho" -> OrbitTokens.danger
    "azul" -> OrbitTokens.bluePastel
    "ambar" -> OrbitTokens.warning
    "roxo" -> OrbitTokens.violet
    else -> OrbitTokens.textLowN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtratoFiltrosSheet(
    carteiras: List<Carteira>,
    carteiraId: String?,
    categoriaId: String?,
    soPendentes: Boolean,
    onCarteira: (String?) -> Unit,
    onCategoria: (String?) -> Unit,
    onPendentes: (Boolean) -> Unit,
    onLimpar: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(top = 8.dp, bottom = 20.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Filtros",
                    color = OrbitTokens.textHiN,
                    fontSize = 18.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Limpar",
                    color = OrbitTokens.bluePastel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.orbitPressable(onClick = onLimpar),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("Carteira", color = OrbitTokens.textLowN, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChipFiltro("Todas", carteiraId == null) { onCarteira(null) }
                carteiras.forEach { c ->
                    ChipFiltro(c.apelido, carteiraId == c.id) { onCarteira(c.id) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Categoria", color = OrbitTokens.textLowN, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChipFiltro("Todas", categoriaId == null) { onCategoria(null) }
                CategoriasFinanca.todas.forEach { cat ->
                    ChipFiltro("${cat.emoji} ${cat.rotulo}", categoriaId == cat.id) {
                        onCategoria(cat.id)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (soPendentes) OrbitTokens.bluePastel.copy(alpha = 0.18f) else OrbitTokens.graphiteRaised)
                    .orbitPressable { onPendentes(!soPendentes) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Só pendentes (a pagar)", color = OrbitTokens.textHiN, fontSize = 14.sp)
                Text(
                    if (soPendentes) "Ligado" else "Off",
                    color = if (soPendentes) OrbitTokens.bluePastel else OrbitTokens.textLowN,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrbitTokens.bluePastel)
                    .orbitPressable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Aplicar",
                    color = OrbitTokens.onBluePastel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ChipFiltro(label: String, ativo: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.textHiN,
        fontSize = 13.sp,
        fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (ativo) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
