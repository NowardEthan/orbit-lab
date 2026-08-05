package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.financas.Carteira
import com.ethan.orbitlab.data.financas.CategoriasFinanca
import com.ethan.orbitlab.data.financas.Lancamento
import com.ethan.orbitlab.data.financas.LancamentoRascunho
import com.ethan.orbitlab.data.financas.OrigemLancamento
import com.ethan.orbitlab.data.financas.TipoLancamento
import com.ethan.orbitlab.data.financas.formatarReais
import com.ethan.orbitlab.data.financas.inicioDoDia
import com.ethan.orbitlab.data.financas.normalizarTagsFinancas
import com.ethan.orbitlab.data.financas.ontemMs
import com.ethan.orbitlab.data.financas.parsearReaisParaCentavos
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Saída ativa — vermelho-marrom contido do concept. */
private val SaidaAtiva = Color(0xFF6B3E3A)
/** Entrada ativa — verde contido. */
private val EntradaAtiva = Color(0xFF2F5A48)

/**
 * Resultado do Registrar — lançamento + opcional "virar recorrente".
 */
data class ResultadoRegistrar(
    val rascunho: LancamentoRascunho,
    val repetirTodoMes: Boolean,
)

/**
 * Folha de baixo — polish alinhado ao concept Registrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarSheet(
    carteiras: List<Carteira>,
    inicial: Lancamento? = null,
    onDismiss: () -> Unit,
    onSalvar: (ResultadoRegistrar) -> Unit,
    onApagar: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tipo by remember {
        mutableStateOf(inicial?.tipo ?: TipoLancamento.SAIDA)
    }
    var valorTexto by remember {
        mutableStateOf(
            inicial?.valorCentavos
                ?.let { formatarReais(it).removePrefix("R$\u00A0").removePrefix("R$ ") }
                .orEmpty(),
        )
    }
    var editandoValor by remember { mutableStateOf(inicial == null) }
    var categoria by remember {
        mutableStateOf(
            inicial?.categoria ?: CategoriasFinanca.padraoPara(tipo).id,
        )
    }
    var carteiraId by remember {
        mutableStateOf(inicial?.carteiraId ?: carteiras.firstOrNull()?.id.orEmpty())
    }
    var descricao by remember { mutableStateOf(inicial?.descricao.orEmpty()) }
    var tagsTexto by remember { mutableStateOf(inicial?.tags?.joinToString(", ").orEmpty()) }
    var dataMs by remember {
        mutableStateOf(inicial?.dataMs ?: inicioDoDia(System.currentTimeMillis()))
    }
    var repetirTodoMes by remember {
        mutableStateOf(inicial?.recorrenteId != null)
    }
    var pago by remember { mutableStateOf(inicial?.pago ?: true) }
    val jaEhRecorrente = inicial?.recorrenteId != null
    var erro by remember { mutableStateOf<String?>(null) }
    var pickerCarteira by remember { mutableStateOf(false) }
    var pickerData by remember { mutableStateOf(false) }

    val categoriasVisiveis = remember(tipo) {
        if (tipo == TipoLancamento.ENTRADA) {
            listOf(CategoriasFinanca.renda, CategoriasFinanca.outros)
        } else {
            CategoriasFinanca.todas.filter { it.id != CategoriasFinanca.renda.id }
        }
    }
    val carteiraSel = carteiras.find { it.id == carteiraId }
    val hojeMs = remember { inicioDoDia(System.currentTimeMillis()) }
    val ontem = remember { ontemMs() }
    val labelData = when (inicioDoDia(dataMs)) {
        hojeMs -> "Hoje"
        ontem -> "Ontem"
        else -> SimpleDateFormat("d MMM", Locale.forLanguageTag("pt-BR"))
            .format(Date(dataMs))
            .lowercase(Locale.forLanguageTag("pt-BR"))
    }
    val labelSalvar = if (tipo == TipoLancamento.SAIDA) "Salvar saída" else "Salvar entrada"
    val tagsPreview = remember(tagsTexto) { normalizarTagsFinancas(listOf(tagsTexto)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.graphiteSurf,
        contentColor = OrbitTokens.textHiN,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(OrbitTokens.textLowN.copy(alpha = 0.45f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(top = 4.dp, bottom = 20.dp),
        ) {
            Text(
                if (inicial == null) "Registrar" else "Editar",
                color = OrbitTokens.textHiN,
                fontSize = 22.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AlternadorTipo(
                    label = "Saída",
                    ativo = tipo == TipoLancamento.SAIDA,
                    corAtiva = SaidaAtiva,
                    modifier = Modifier.weight(1f),
                ) {
                    tipo = TipoLancamento.SAIDA
                    if (categoria == CategoriasFinanca.renda.id) {
                        categoria = CategoriasFinanca.alimentacao.id
                    }
                }
                AlternadorTipo(
                    label = "Entrada",
                    ativo = tipo == TipoLancamento.ENTRADA,
                    corAtiva = EntradaAtiva,
                    modifier = Modifier.weight(1f),
                ) {
                    tipo = TipoLancamento.ENTRADA
                    categoria = CategoriasFinanca.renda.id
                }
            }

            Spacer(Modifier.height(28.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .orbitPressable { editandoValor = true },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("R$", color = OrbitTokens.textMidN, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                if (editandoValor) {
                    BasicTextField(
                        value = valorTexto,
                        onValueChange = { valorTexto = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = OrbitTokens.textHiN,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Bricolage,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-1).sp,
                        ),
                        cursorBrush = SolidColor(OrbitTokens.bluePastel),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (valorTexto.isEmpty()) {
                                    Text(
                                        "0,00",
                                        color = OrbitTokens.textLowN.copy(alpha = 0.55f),
                                        fontSize = 48.sp,
                                        fontFamily = Bricolage,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                } else {
                    val centavos = parsearReaisParaCentavos(valorTexto) ?: 0L
                    Text(
                        formatarReais(centavos)
                            .removePrefix("R$\u00A0")
                            .removePrefix("R$ "),
                        color = OrbitTokens.textHiN,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Bricolage,
                        letterSpacing = (-1).sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "toque pra editar o valor",
                    color = OrbitTokens.textLowN,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(22.dp))

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categoriasVisiveis.forEach { cat ->
                    val ativo = categoria == cat.id
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (ativo) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised,
                            )
                            .orbitPressable { categoria = cat.id }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(cat.emoji, fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            cat.rotulo,
                            color = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.textHiN,
                            fontSize = 13.sp,
                            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrbitTokens.graphiteRaised),
            ) {
                LinhaFormulario(
                    icone = Icons.Rounded.CalendarMonth,
                    rotulo = "Data",
                    onClick = { pickerData = true },
                ) {
                    Text(labelData, color = OrbitTokens.textHiN, fontSize = 14.sp)
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = OrbitTokens.textLowN,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DivisorForm()
                LinhaFormulario(
                    icone = Icons.Rounded.CreditCard,
                    rotulo = "Carteira",
                    onClick = {
                        if (carteiras.isNotEmpty()) pickerCarteira = true
                    },
                ) {
                    Text(
                        carteiraSel?.apelido ?: "Escolher",
                        color = if (carteiraSel != null) OrbitTokens.textHiN else OrbitTokens.textLowN,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(120.dp),
                        textAlign = TextAlign.End,
                    )
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = OrbitTokens.textLowN,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DivisorForm()
                LinhaFormulario(
                    icone = Icons.Rounded.Repeat,
                    rotulo = "Repetir todo mês",
                    onClick = null,
                ) {
                    Switch(
                        checked = repetirTodoMes,
                        onCheckedChange = { if (!jaEhRecorrente) repetirTodoMes = it },
                        enabled = !jaEhRecorrente,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OrbitTokens.onBluePastel,
                            checkedTrackColor = OrbitTokens.bluePastel,
                            uncheckedThumbColor = OrbitTokens.textMidN,
                            uncheckedTrackColor = OrbitTokens.graphiteSurf,
                            disabledCheckedThumbColor = OrbitTokens.onBluePastel.copy(alpha = 0.7f),
                            disabledCheckedTrackColor = OrbitTokens.bluePastel.copy(alpha = 0.5f),
                        ),
                    )
                }
                DivisorForm()
                LinhaFormulario(
                    icone = Icons.Rounded.CheckCircle,
                    rotulo = "Já pago",
                    onClick = null,
                ) {
                    Switch(
                        checked = pago,
                        onCheckedChange = { pago = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OrbitTokens.onBluePastel,
                            checkedTrackColor = OrbitTokens.bluePastel,
                            uncheckedThumbColor = OrbitTokens.textMidN,
                            uncheckedTrackColor = OrbitTokens.graphiteSurf,
                        ),
                    )
                }
                DivisorForm()
                LinhaFormulario(
                    icone = Icons.Rounded.Notes,
                    rotulo = "Nota",
                    onClick = null,
                ) {
                    BasicTextField(
                        value = descricao,
                        onValueChange = { descricao = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = OrbitTokens.textHiN,
                            fontSize = 14.sp,
                            textAlign = TextAlign.End,
                        ),
                        cursorBrush = SolidColor(OrbitTokens.bluePastel),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        modifier = Modifier.width(160.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (descricao.isEmpty()) {
                                    Text(
                                        "Opcional",
                                        color = OrbitTokens.textLowN,
                                        fontSize = 14.sp,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
                DivisorForm()
                LinhaFormulario(
                    icone = Icons.Rounded.Label,
                    rotulo = "Tags",
                    onClick = null,
                ) {
                    BasicTextField(
                        value = tagsTexto,
                        onValueChange = { tagsTexto = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = OrbitTokens.textHiN,
                            fontSize = 14.sp,
                            textAlign = TextAlign.End,
                        ),
                        cursorBrush = SolidColor(OrbitTokens.bluePastel),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                        ),
                        modifier = Modifier.width(160.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (tagsTexto.isEmpty()) {
                                    Text(
                                        "ex: ifood, casa",
                                        color = OrbitTokens.textLowN,
                                        fontSize = 14.sp,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }

            if (tagsPreview.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    tagsPreview.forEach { tag ->
                        Text(
                            "#$tag",
                            color = OrbitTokens.bluePastel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(OrbitTokens.bluePastel.copy(alpha = 0.12f))
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            if (carteiras.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Cria uma carteira em Cartões antes de registrar.",
                    color = OrbitTokens.warning,
                    fontSize = 13.sp,
                )
            }

            erro?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = OrbitTokens.danger, fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrbitTokens.bluePastel)
                    .orbitPressable {
                        val valor = parsearReaisParaCentavos(valorTexto)
                        if (valor == null || valor <= 0L) {
                            erro = "Informa um valor maior que zero"
                            return@orbitPressable
                        }
                        if (carteiraId.isBlank()) {
                            erro = "Escolhe uma carteira"
                            return@orbitPressable
                        }
                        val rascunho = LancamentoRascunho(
                            tipo = tipo,
                            valorCentavos = valor,
                            dataMs = inicioDoDia(dataMs),
                            descricao = descricao.ifBlank {
                                CategoriasFinanca.porId(categoria).rotulo
                            },
                            categoria = categoria,
                            carteiraId = carteiraId,
                            origem = inicial?.origem ?: OrigemLancamento.MANUAL,
                            tags = tagsPreview,
                            pago = pago,
                            recorrenteId = inicial?.recorrenteId,
                            capturaRaw = inicial?.capturaRaw,
                        )
                        if (!rascunho.valido()) {
                            erro = "Não deu pra salvar — confere os campos"
                            return@orbitPressable
                        }
                        erro = null
                        onSalvar(
                            ResultadoRegistrar(
                                rascunho = rascunho,
                                repetirTodoMes = repetirTodoMes && !jaEhRecorrente,
                            ),
                        )
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    labelSalvar,
                    color = OrbitTokens.onBluePastel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (inicial != null && onApagar != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Apagar lançamento",
                    color = OrbitTokens.danger,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .orbitPressable(onClick = onApagar)
                        .padding(8.dp),
                )
            }
        }
    }

    if (pickerCarteira) {
        PickerCarteiraRegistrar(
            carteiras = carteiras,
            selecionadaId = carteiraId,
            onPick = {
                carteiraId = it.id
                pickerCarteira = false
            },
            onDismiss = { pickerCarteira = false },
        )
    }

    if (pickerData) {
        DataEscolhaSheet(
            selecionadaMs = dataMs,
            onEscolher = {
                dataMs = it
                pickerData = false
            },
            onDismiss = { pickerData = false },
        )
    }
}

@Composable
private fun AlternadorTipo(
    label: String,
    ativo: Boolean,
    corAtiva: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (ativo) corAtiva else OrbitTokens.graphiteRaised)
            .orbitPressable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (ativo) Color.White else OrbitTokens.textMidN,
            fontSize = 14.sp,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun LinhaFormulario(
    icone: ImageVector,
    rotulo: String,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    val base = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp)
    Row(
        if (onClick != null) base.orbitPressable(onClick = onClick) else base,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = OrbitTokens.textMidN,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            rotulo,
            color = OrbitTokens.textHiN,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun DivisorForm() {
    HorizontalDivider(
        color = OrbitTokens.graphiteHair,
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 14.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerCarteiraRegistrar(
    carteiras: List<Carteira>,
    selecionadaId: String,
    onPick: (Carteira) -> Unit,
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
            Text(
                "Carteira",
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            carteiras.forEach { c ->
                val on = c.id == selecionadaId
                Text(
                    c.apelido,
                    color = if (on) OrbitTokens.onBluePastel else OrbitTokens.textHiN,
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (on) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised,
                        )
                        .orbitPressable(onClick = { onPick(c) })
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                )
            }
        }
    }
}

/**
 * DatePicker Material3 — calendário de verdade (UTC do picker → início do dia local).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataEscolhaSheet(
    selecionadaMs: Long,
    onEscolher: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDiaParaUtcPicker(selecionadaMs),
    )
    val cores = DatePickerDefaults.colors(
        containerColor = OrbitTokens.graphiteSurf,
        titleContentColor = OrbitTokens.textHiN,
        headlineContentColor = OrbitTokens.textHiN,
        weekdayContentColor = OrbitTokens.textMidN,
        subheadContentColor = OrbitTokens.textMidN,
        navigationContentColor = OrbitTokens.textHiN,
        yearContentColor = OrbitTokens.textHiN,
        currentYearContentColor = OrbitTokens.bluePastel,
        selectedYearContentColor = OrbitTokens.onBluePastel,
        selectedYearContainerColor = OrbitTokens.bluePastel,
        dayContentColor = OrbitTokens.textHiN,
        selectedDayContentColor = OrbitTokens.onBluePastel,
        selectedDayContainerColor = OrbitTokens.bluePastel,
        todayContentColor = OrbitTokens.bluePastel,
        todayDateBorderColor = OrbitTokens.bluePastel,
        dayInSelectionRangeContentColor = OrbitTokens.textHiN,
        dayInSelectionRangeContainerColor = OrbitTokens.graphiteRaised,
        dividerColor = OrbitTokens.graphiteRaised,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val utc = datePickerState.selectedDateMillis
                    if (utc != null) {
                        onEscolher(utcPickerParaLocalDia(utc))
                    } else {
                        onDismiss()
                    }
                },
            ) {
                Text("OK", color = OrbitTokens.bluePastel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = OrbitTokens.textMidN)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = OrbitTokens.graphiteSurf),
    ) {
        DatePicker(
            state = datePickerState,
            colors = cores,
            showModeToggle = true,
        )
    }
}

/** Material DatePicker usa meia-noite UTC; finanças usam início do dia local. */
private fun localDiaParaUtcPicker(localMs: Long): Long {
    val local = Calendar.getInstance(TimeZone.getDefault()).apply {
        timeInMillis = inicioDoDia(localMs)
    }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
        )
    }.timeInMillis
}

private fun utcPickerParaLocalDia(utcMs: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMs
    }
    val local = Calendar.getInstance(TimeZone.getDefault()).apply {
        clear()
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
        )
    }
    return inicioDoDia(local.timeInMillis)
}
