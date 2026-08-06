package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.financas.Carteira
import com.ethan.orbitlab.data.financas.CorCarteira
import com.ethan.orbitlab.data.financas.FinancasRepository
import com.ethan.orbitlab.data.financas.FirestoreFinancas
import com.ethan.orbitlab.data.financas.Lancamento
import com.ethan.orbitlab.data.financas.MotivoTransferencia
import com.ethan.orbitlab.data.financas.TipoCarteira
import com.ethan.orbitlab.data.financas.Transferencia
import com.ethan.orbitlab.data.financas.TransferenciaLauncher
import com.ethan.orbitlab.data.financas.TransferenciaRascunho
import com.ethan.orbitlab.data.financas.faturaCredito
import com.ethan.orbitlab.data.financas.formatarReais
import com.ethan.orbitlab.data.financas.inicioDoDia
import com.ethan.orbitlab.data.financas.ontemMs
import com.ethan.orbitlab.data.financas.parsearReaisParaCentavos
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

private val CardRadius = 20.dp

/**
 * Transferência — valor, DE/PARA, data, motivo, CTA + Recentes.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransferenciaScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val carteiras by FinancasRepository.carteiras.collectAsState()
    val lancamentos by FinancasRepository.lancamentos.collectAsState()
    val transferencias by FinancasRepository.transferencias.collectAsState()
    val ativas = remember(carteiras) { carteiras.filter { !it.arquivada } }
    val scope = rememberCoroutineScope()
    val fmtData = remember {
        SimpleDateFormat("d MMM", Locale.forLanguageTag("pt-BR"))
    }

    var deId by remember { mutableStateOf<String?>(null) }
    var paraId by remember { mutableStateOf<String?>(null) }
    var valorTxt by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf<String?>(MotivoTransferencia.AJUSTE) }
    var dataMs by remember { mutableStateOf(inicioDoDia(System.currentTimeMillis())) }
    var erro by remember { mutableStateOf<String?>(null) }
    var okMsg by remember { mutableStateOf<String?>(null) }
    var picker by remember { mutableStateOf<PickerLado?>(null) }
    var pickerData by remember { mutableStateOf(false) }
    var apagarId by remember { mutableStateOf<String?>(null) }

    val hojeMs = remember { inicioDoDia(System.currentTimeMillis()) }
    val ontem = remember { ontemMs() }
    val labelData = when (inicioDoDia(dataMs)) {
        hojeMs -> "Hoje"
        ontem -> "Ontem"
        else -> fmtData.format(Date(dataMs))
    }

    val navTick by TransferenciaLauncher.navegarTick.collectAsState()
    // P1.1: Página 1 do navTick já foi aplicada. Só roda de novo se vier prefill novo,
    // E defaults só se a tela acabou de abrir sem prefill (deId==null). NUNCA
    // sobrescreve escolha manual do usuário.
    var navTickAplicado by remember { mutableStateOf(0L) }
    LaunchedEffect(navTick, ativas.size) {
        if (ativas.isEmpty()) return@LaunchedEffect
        if (navTick > 0L && navTick == navTickAplicado) return@LaunchedEffect
        val prefill = TransferenciaLauncher.consumirPrefill()
        if (prefill != null) {
            deId = prefill.deCarteiraId
                ?: ativas.firstOrNull { it.tipo != TipoCarteira.CARTAO_CREDITO }?.id
            paraId = prefill.paraCarteiraId
                ?: ativas.firstOrNull { it.tipo == TipoCarteira.CARTAO_CREDITO }?.id
            motivo = prefill.motivo ?: MotivoTransferencia.PAGAR_FATURA
            val v = prefill.valorCentavos
            if (v != null && v > 0) {
                val reais = v / 100
                val cents = (v % 100).toInt()
                valorTxt = if (cents == 0) reais.toString() else "%d,%02d".format(reais, cents)
            }
            navTickAplicado = navTick
        } else if (deId == null) {
            deId = ativas.firstOrNull { it.tipo != TipoCarteira.CARTAO_CREDITO }?.id
                ?: ativas.firstOrNull()?.id
            paraId = ativas.firstOrNull { it.id != deId }?.id
        }
    }

    val de = ativas.find { it.id == deId }
    val para = ativas.find { it.id == paraId }
    val centavos = parsearReaisParaCentavos(valorTxt) ?: 0L
    val podeSalvar = de != null && para != null && de.id != para.id && centavos > 0 && uid != null

    fun trocarLados() {
        val a = deId
        deId = paraId
        paraId = a
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OrbitMetrics.pagePadding,
            end = OrbitMetrics.pagePadding,
            top = 2.dp,
            bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Transferência",
                color = OrbitTokens.textHiN,
                fontSize = 28.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.orbitEnter(0),
            )
        }

        if (ativas.size < 2) {
            item {
                Text(
                    "Precisa de pelo menos duas carteiras pra transferir. Cria outra em Cartões.",
                    color = OrbitTokens.textMidN,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CardRadius))
                        .background(OrbitTokens.graphiteSurf)
                        .padding(16.dp),
                )
            }
        } else {
            item {
                CampoValorGrande(
                    valorTxt = valorTxt,
                    onValor = {
                        valorTxt = it.filter { c -> c.isDigit() || c == ',' || c == '.' }
                        erro = null
                        okMsg = null
                    },
                )
            }

            item {
                Column(Modifier.fillMaxWidth()) {
                    CardLadoTransferencia(
                        rotulo = "DE",
                        carteira = de,
                        subtitulo = subtituloLado(de, lancamentos, transferencias),
                        onClick = { picker = PickerLado.De },
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(OrbitTokens.graphiteRaised)
                                .orbitPressable(onClick = { trocarLados() }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.SwapVert,
                                contentDescription = "Inverter",
                                tint = OrbitTokens.bluePastel,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    CardLadoTransferencia(
                        rotulo = "PARA",
                        carteira = para,
                        subtitulo = subtituloLado(para, lancamentos, transferencias),
                        onClick = { picker = PickerLado.Para },
                    )
                }
            }

            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(OrbitTokens.graphiteRaised)
                        .orbitPressable { pickerData = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = OrbitTokens.bluePastel,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Data", color = OrbitTokens.textMidN, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(labelData, color = OrbitTokens.textHiN, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = OrbitTokens.textLowN,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            item {
                Text(
                    "Motivo",
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChipMotivo(
                        label = MotivoTransferencia.rotulo(MotivoTransferencia.PAGAR_FATURA),
                        icone = Icons.Rounded.CreditCard,
                        ativo = motivo == MotivoTransferencia.PAGAR_FATURA,
                        onClick = { motivo = MotivoTransferencia.PAGAR_FATURA },
                    )
                    ChipMotivo(
                        label = MotivoTransferencia.rotulo(MotivoTransferencia.RESERVA),
                        icone = Icons.Rounded.Flag,
                        ativo = motivo == MotivoTransferencia.RESERVA,
                        onClick = { motivo = MotivoTransferencia.RESERVA },
                    )
                    ChipMotivo(
                        label = MotivoTransferencia.rotulo(MotivoTransferencia.AJUSTE),
                        icone = Icons.Rounded.Edit,
                        ativo = motivo == MotivoTransferencia.AJUSTE,
                        onClick = { motivo = MotivoTransferencia.AJUSTE },
                    )
                }
            }

            item {
                BannerInfoTransferencia()
            }

            erro?.let {
                item { Text(it, color = OrbitTokens.danger, fontSize = 13.sp) }
            }
            okMsg?.let {
                item { Text(it, color = OrbitTokens.online, fontSize = 13.sp) }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (podeSalvar) OrbitTokens.bluePastel
                            else OrbitTokens.graphiteRaised,
                        )
                        .orbitPressable(enabled = podeSalvar) {
                            val u = uid ?: return@orbitPressable
                            val d = deId ?: return@orbitPressable
                            val p = paraId ?: return@orbitPressable
                            scope.launch {
                                runCatching {
                                    FirestoreFinancas.criarTransferencia(
                                        u,
                                        TransferenciaRascunho(
                                            deCarteiraId = d,
                                            paraCarteiraId = p,
                                            valorCentavos = centavos,
                                            dataMs = dataMs,
                                            motivo = motivo,
                                        ),
                                    )
                                }.onSuccess {
                                    okMsg = "Transferiu ${formatarReais(centavos)}."
                                    valorTxt = ""
                                    erro = null
                                }.onFailure { e ->
                                    erro = e.message ?: "Não deu pra transferir."
                                    okMsg = null
                                }
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (centavos > 0) "Transferir ${formatarReais(centavos)}"
                        else "Transferir",
                        color = if (podeSalvar) OrbitTokens.onBluePastel else OrbitTokens.textLowN,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            item {
                Text(
                    "Transferência — mover sem virar gasto",
                    color = OrbitTokens.textLowN,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }

        if (transferencias.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recentes",
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(transferencias.take(30), key = { it.id }) { t ->
                LinhaTransferenciaRecente(
                    transferencia = t,
                    deNome = carteiras.find { it.id == t.deCarteiraId }?.apelido ?: "—",
                    paraNome = carteiras.find { it.id == t.paraCarteiraId }?.apelido ?: "—",
                    dataLabel = when (inicioDoDia(t.dataMs)) {
                        hojeMs -> "Hoje"
                        ontem -> "Ontem"
                        else -> fmtData.format(Date(t.dataMs))
                    },
                    onLongPress = { apagarId = t.id },
                )
            }
            item {
                Text(
                    "Segure pra apagar",
                    color = OrbitTokens.textLowN,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }

    if (pickerData) {
        DataEscolhaSheet(
            selecionadaMs = dataMs,
            onEscolher = {
                dataMs = inicioDoDia(it)
                pickerData = false
            },
            onDismiss = { pickerData = false },
        )
    }

    apagarId?.let { id ->
        AlertDialog(
            onDismissRequest = { apagarId = null },
            containerColor = OrbitTokens.graphiteSurf,
            titleContentColor = OrbitTokens.textHiN,
            textContentColor = OrbitTokens.textMidN,
            title = { Text("Apagar transferência?") },
            text = { Text("O saldo das carteiras volta a refletir sem essa movimentação.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val u = uid
                        apagarId = null
                        if (u != null) {
                            scope.launch {
                                runCatching { FirestoreFinancas.apagarTransferencia(u, id) }
                                    .onFailure { e ->
                                        erro = e.message ?: "Não deu pra apagar."
                                    }
                            }
                        }
                    },
                ) {
                    Text("Apagar", color = OrbitTokens.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { apagarId = null }) {
                    Text("Cancelar", color = OrbitTokens.textMidN)
                }
            },
        )
    }

    picker?.let { lado ->
        PickerCarteiraSheet(
            titulo = if (lado == PickerLado.De) "De onde sai" else "Pra onde vai",
            opcoes = ativas.filter {
                when (lado) {
                    PickerLado.De -> it.id != paraId
                    PickerLado.Para -> it.id != deId
                }
            },
            selecionadaId = when (lado) {
                PickerLado.De -> deId
                PickerLado.Para -> paraId
            },
            lancamentos = lancamentos,
            transferencias = transferencias,
            onPick = { c ->
                when (lado) {
                    PickerLado.De -> deId = c.id
                    PickerLado.Para -> paraId = c.id
                }
                picker = null
            },
            onDismiss = { picker = null },
        )
    }
}

private enum class PickerLado { De, Para }

@Composable
private fun CampoValorGrande(
    valorTxt: String,
    onValor: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .orbitEnter(1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("R$", color = OrbitTokens.textLowN, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        BasicTextField(
            value = valorTxt,
            onValueChange = onValor,
            textStyle = TextStyle(
                color = OrbitTokens.textHiN,
                fontSize = 48.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                letterSpacing = (-1).sp,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            cursorBrush = SolidColor(OrbitTokens.bluePastel),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (valorTxt.isBlank()) {
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
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text("quanto mover", color = OrbitTokens.textLowN, fontSize = 13.sp)
    }
}

@Composable
private fun CardLadoTransferencia(
    rotulo: String,
    carteira: Carteira?,
    subtitulo: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .orbitPressable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            rotulo,
            color = OrbitTokens.textLowN,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        carteira?.let { brushDaCorCarteira(it.cor) }
                            ?: Brush.linearGradient(
                                listOf(OrbitTokens.graphiteRaised, OrbitTokens.graphiteRaised),
                            ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    carteira?.let { iconeTipo(it.tipo) } ?: Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    carteira?.let { nomeCarteira(it) } ?: "Escolher carteira",
                    color = OrbitTokens.textHiN,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (carteira != null) {
                    Text(
                        subtitulo,
                        color = OrbitTokens.textLowN,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = OrbitTokens.textLowN,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ChipMotivo(
    label: String,
    icone: ImageVector,
    ativo: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (ativo) OrbitTokens.bluePastel else OrbitTokens.graphiteSurf)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.textMidN,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.textHiN,
            fontSize = 13.sp,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun BannerInfoTransferencia() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = OrbitTokens.bluePastel,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 13.sp)) {
                    append("Transferência ")
                }
                withStyle(
                    SpanStyle(
                        color = OrbitTokens.textHiN,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    ),
                ) {
                    append("não conta como gasto")
                }
                withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 13.sp)) {
                    append(" nem entrada. É dinheiro seu mudando de lugar — só ajusta o saldo dos dois cartões.")
                }
            },
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerCarteiraSheet(
    titulo: String,
    opcoes: List<Carteira>,
    selecionadaId: String?,
    lancamentos: List<Lancamento>,
    transferencias: List<Transferencia>,
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
                titulo,
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            opcoes.forEach { c ->
                val on = c.id == selecionadaId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (on) OrbitTokens.bluePastel.copy(alpha = 0.18f)
                            else OrbitTokens.graphiteRaised,
                        )
                        .orbitPressable(onClick = { onPick(c) })
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brushDaCorCarteira(c.cor)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            iconeTipo(c.tipo),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            nomeCarteira(c),
                            color = OrbitTokens.textHiN,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            subtituloLado(c, lancamentos, transferencias),
                            color = OrbitTokens.textLowN,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun nomeCarteira(c: Carteira): String {
    val base = c.banco?.takeIf { it.isNotBlank() } ?: c.apelido
    return "$base — ${TipoCarteira.rotulo(c.tipo)}"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinhaTransferenciaRecente(
    transferencia: Transferencia,
    deNome: String,
    paraNome: String,
    dataLabel: String,
    onLongPress: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$deNome → $paraNome",
                color = OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                formatarReais(transferencia.valorCentavos),
                color = OrbitTokens.bluePastel,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${MotivoTransferencia.rotulo(transferencia.motivo)} · $dataLabel",
            color = OrbitTokens.textLowN,
            fontSize = 12.sp,
        )
    }
}

private fun subtituloLado(
    carteira: Carteira?,
    lancamentos: List<Lancamento>,
    transferencias: List<Transferencia>,
): String {
    if (carteira == null) return ""
    return if (carteira.tipo == TipoCarteira.CARTAO_CREDITO) {
        val fatura = faturaCredito(carteira, lancamentos, transferencias)
        "fatura ${formatarReais(fatura?.faturaCentavos ?: 0L)}"
    } else {
        "saldo ${formatarReais(saldoDerivado(carteira, lancamentos, transferencias))}"
    }
}

private fun iconeTipo(tipo: String): ImageVector = when (tipo) {
    TipoCarteira.CARTAO_CREDITO -> Icons.Rounded.CreditCard
    TipoCarteira.DINHEIRO -> Icons.Rounded.Payments
    else -> Icons.Rounded.AccountBalanceWallet
}

private fun brushDaCorCarteira(cor: String): Brush {
    val (a, b) = when (cor) {
        CorCarteira.AZUL -> Color(0xFF4A7EC8) to Color(0xFF1E3A6E)
        CorCarteira.ROXO -> Color(0xFF9B4DCA) to Color(0xFF5A0A9A)
        CorCarteira.VERDE -> Color(0xFF2F7A5B) to Color(0xFF143D2E)
        CorCarteira.AMBAR -> Color(0xFFB8862E) to Color(0xFF5C3A10)
        else -> Color(0xFF3A3D44) to Color(0xFF1E2024)
    }
    return Brush.linearGradient(listOf(a, b))
}
