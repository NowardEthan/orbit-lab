package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.captura.CapturaPermissoes
import com.ethan.orbitlab.data.captura.CapturaRepository
import com.ethan.orbitlab.data.captura.CapturaStatusGeral
import com.ethan.orbitlab.data.captura.SaudeBanco
import com.ethan.orbitlab.data.captura.StatusBancoCaptura
import com.ethan.orbitlab.data.captura.SugestaoCaptura
import com.ethan.orbitlab.data.financas.FinancasRepository
import com.ethan.orbitlab.data.financas.Lancamento
import com.ethan.orbitlab.data.financas.LancamentoRascunho
import com.ethan.orbitlab.data.financas.OrigemLancamento
import com.ethan.orbitlab.data.financas.PeriodoExtrato
import com.ethan.orbitlab.data.financas.FirestoreFinancas
import com.ethan.orbitlab.data.financas.faixaDoPeriodo
import com.ethan.orbitlab.data.financas.filtrarPorPeriodo
import com.ethan.orbitlab.data.financas.inicioDoDia
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val CardRadius = 22.dp

/**
 * Captura automática — polish alinhado ao concept.
 */
@Composable
fun CapturaScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val consentimento by CapturaRepository.consentimento.collectAsState()
    val desejada by CapturaRepository.desejada.collectAsState()
    val pendentes by CapturaRepository.pendentes.collectAsState()
    val carteiras by FinancasRepository.carteiras.collectAsState()
    val lancamentos by FinancasRepository.lancamentos.collectAsState()
    val ultima by CapturaRepository.ultimaCapturaMs.collectAsState()

    var tick by remember { mutableStateOf(0) }
    // P0.3: refaz o status quando a tela volta do background (ex: usuário concedeu
    // permissão de notificação ou bateria em Settings). Sem isso o card fica
    // stale até a próxima interação interna.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val status = remember(consentimento, desejada, ultima, tick, pendentes.size) {
        CapturaRepository.statusGeral(context)
    }
    val capturasMes = remember(lancamentos) {
        filtrarPorPeriodo(lancamentos, faixaDoPeriodo(PeriodoExtrato.MES))
            .count { it.origem == OrigemLancamento.CAPTURA }
    }

    var editando by remember { mutableStateOf<SugestaoCaptura?>(null) }
    var detalhe by remember { mutableStateOf<SugestaoCaptura?>(null) }
    val avisoFilaDisco = remember { CapturaRepository.pendentes.value.isNotEmpty() }
    var mostrarAvisoFila by remember { mutableStateOf(avisoFilaDisco) }

    fun confirmar(s: SugestaoCaptura, rascunho: LancamentoRascunho? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val carteiraId = rascunho?.carteiraId
            ?: s.carteiraId
            ?: carteiras.firstOrNull { !it.arquivada }?.id
            ?: return
        val draft = rascunho ?: LancamentoRascunho(
            tipo = s.aviso.tipo,
            valorCentavos = s.aviso.valorCentavos,
            dataMs = inicioDoDia(s.aviso.quandoMs),
            descricao = s.aviso.descricao,
            categoria = s.categoriaId,
            carteiraId = carteiraId,
            origem = OrigemLancamento.CAPTURA,
            capturaRaw = s.aviso.raw,
            pago = true,
        ).copy(origem = OrigemLancamento.CAPTURA, capturaRaw = s.aviso.raw)
        scope.launch {
            runCatching {
                FirestoreFinancas.criarLancamento(
                    uid,
                    draft.copy(
                        origem = OrigemLancamento.CAPTURA,
                        capturaRaw = s.aviso.raw,
                    ),
                )
                CapturaRepository.consumir(s.aviso.id)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OrbitMetrics.pagePadding,
            end = OrbitMetrics.pagePadding,
            top = 2.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Captura automática",
                color = OrbitTokens.textHiN,
                fontSize = 26.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.orbitEnter(0),
            )
        }

        item { CardIntroCaptura() }

        if (mostrarAvisoFila && pendentes.isNotEmpty()) {
            item {
                Text(
                    "${pendentes.size} ${if (pendentes.size == 1) "sugestão esperando" else "sugestões esperando"}",
                    color = OrbitTokens.bluePastel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OrbitTokens.graphiteSurf)
                        .orbitPressable { mostrarAvisoFila = false }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }

        if (!consentimento) {
            item {
                CardConsentimento(
                    onAceitar = {
                        CapturaRepository.aceitarConsentimento()
                        CapturaRepository.setDesejada(true)
                        tick++
                    },
                )
            }
        } else {
            item {
                CardStatusEscutando(
                    status = status,
                    desejada = desejada,
                    ultimaMs = ultima,
                    capturasMes = capturasMes,
                    onToggle = {
                        CapturaRepository.setDesejada(it)
                        tick++
                    },
                )
            }

            item {
                Text(
                    "Pra funcionar",
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                CardRequisitos(
                    listenerOk = status.listenerAtivo,
                    bateriaOk = status.bateriaOk,
                    onAbrirListener = {
                        CapturaPermissoes.abrirConfigListener(context)
                        tick++
                    },
                    onAbrirBateria = {
                        CapturaPermissoes.pedirIgnorarBateria(context)
                        tick++
                    },
                )
            }

            item {
                Text(
                    "Bancos que o Orbit escuta",
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                CardBancos(status.bancos)
            }

            if (pendentes.isNotEmpty()) {
                item {
                    Text(
                        "A Luna viu",
                        color = OrbitTokens.textHiN,
                        fontSize = 17.sp,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(pendentes.reversed(), key = { it.aviso.id }) { s ->
                    SugestaoCapturaCard(
                        sugestao = s,
                        carteiraNome = carteiras.find { it.id == s.carteiraId }?.apelido,
                        onRegistrar = { confirmar(s) },
                        onEditar = { editando = s },
                        onIgnorar = { CapturaRepository.ignorar(s.aviso.id) },
                        onAbrir = { detalhe = s },
                    )
                }
            }

            item {
                Text(
                    "Simular aviso (lab)",
                    color = OrbitTokens.bluePastel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .orbitPressable { CapturaRepository.simularDemo() }
                        .padding(vertical = 4.dp),
                )
            }
        }
    }

    detalhe?.let { s ->
        SugestaoCapturaSheet(
            sugestao = s,
            carteiraNome = carteiras.find { it.id == s.carteiraId }?.apelido,
            onDismiss = { detalhe = null },
            onRegistrar = {
                confirmar(s)
                detalhe = null
            },
            onEditar = {
                detalhe = null
                editando = s
            },
            onIgnorar = {
                CapturaRepository.ignorar(s.aviso.id)
                detalhe = null
            },
        )
    }

    editando?.let { s ->
        val inicial = Lancamento(
            id = s.aviso.id,
            tipo = s.aviso.tipo,
            valorCentavos = s.aviso.valorCentavos,
            dataMs = inicioDoDia(s.aviso.quandoMs),
            descricao = s.aviso.descricao,
            categoria = s.categoriaId,
            carteiraId = s.carteiraId.orEmpty(),
            origem = OrigemLancamento.CAPTURA,
            capturaRaw = s.aviso.raw,
            pago = true,
        )
        RegistrarSheet(
            carteiras = carteiras.filter { !it.arquivada },
            inicial = inicial,
            onDismiss = { editando = null },
            onSalvar = { resultado ->
                confirmar(s, resultado.rascunho)
                editando = null
            },
        )
    }
}

@Composable
private fun CardIntroCaptura() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(16.dp)
            .orbitEnter(1),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                OrbitTokens.bluePastel.copy(alpha = 0.55f),
                                OrbitTokens.graphiteRaised,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 14.sp)) {
                        append("A Luna lê os ")
                    }
                    withStyle(
                        SpanStyle(
                            color = OrbitTokens.textHiN,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        ),
                    ) {
                        append("avisos de compra")
                    }
                    withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 14.sp)) {
                        append(" que seu banco manda e registra pra você. Você só confirma.")
                    }
                },
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(OrbitTokens.online.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = OrbitTokens.online,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Tudo no seu aparelho · nada sai daqui",
                color = OrbitTokens.online,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CardConsentimento(onAceitar: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(16.dp),
    ) {
        Text(
            "Ativar captura",
            color = OrbitTokens.textHiN,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "O Orbit vai ler só avisos dos apps dos bancos. Nada sobe pra nuvem até você tocar em Registrar.",
            color = OrbitTokens.textMidN,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(OrbitTokens.bluePastel)
                .orbitPressable(onClick = onAceitar)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Entendi — ativar",
                color = OrbitTokens.onBluePastel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CardStatusEscutando(
    status: CapturaStatusGeral,
    desejada: Boolean,
    ultimaMs: Long?,
    capturasMes: Int,
    onToggle: (Boolean) -> Unit,
) {
    val escutando = desejada && status.listenerAtivo
    val subtitulo = buildString {
        if (ultimaMs == null) append("ainda nenhuma captura")
        else append("última captura há ${relativo(ultimaMs)}")
        append(" · ")
        append("$capturasMes este mês")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (escutando) OrbitTokens.online.copy(alpha = 0.2f)
                    else OrbitTokens.graphiteRaised,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (escutando) OrbitTokens.online else OrbitTokens.textLowN),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (escutando) "Escutando" else if (desejada) "Quase lá" else "Pausada",
                color = OrbitTokens.textHiN,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitulo,
                color = OrbitTokens.textMidN,
                fontSize = 12.sp,
            )
        }
        Switch(
            checked = desejada,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OrbitTokens.onBluePastel,
                checkedTrackColor = OrbitTokens.bluePastel,
                uncheckedThumbColor = OrbitTokens.textMidN,
                uncheckedTrackColor = OrbitTokens.graphiteRaised,
            ),
        )
    }
}

@Composable
private fun CardRequisitos(
    listenerOk: Boolean,
    bateriaOk: Boolean,
    onAbrirListener: () -> Unit,
    onAbrirBateria: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf),
    ) {
        LinhaRequisito(
            ok = listenerOk,
            titulo = "Acesso a notificações",
            subtitulo = if (listenerOk) "o Orbit pode ler os avisos" else "precisa liberar nas configs",
            onClick = if (!listenerOk) onAbrirListener else null,
        )
        HorizontalDivider(
            color = OrbitTokens.graphiteHair,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        LinhaRequisito(
            ok = bateriaOk,
            titulo = "Bateria sem restrição",
            subtitulo = if (bateriaOk) {
                "pra não perder avisos em 2º plano"
            } else {
                "toque pra não otimizar o Orbit"
            },
            onClick = if (!bateriaOk) onAbrirBateria else null,
        )
    }
}

@Composable
private fun LinhaRequisito(
    ok: Boolean,
    titulo: String,
    subtitulo: String,
    onClick: (() -> Unit)?,
) {
    val base = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 14.dp)
    Row(
        if (onClick != null) base.orbitPressable(onClick = onClick) else base,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (ok) OrbitTokens.online.copy(alpha = 0.2f)
                    else OrbitTokens.warning.copy(alpha = 0.2f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (ok) Icons.Rounded.Check else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (ok) OrbitTokens.online else OrbitTokens.warning,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                color = OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(subtitulo, color = OrbitTokens.textLowN, fontSize = 12.sp)
        }
        Text(
            if (ok) "Ativo" else "Ligar",
            color = if (ok) OrbitTokens.online else OrbitTokens.warning,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CardBancos(bancos: List<StatusBancoCaptura>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf),
    ) {
        bancos.forEachIndexed { i, b ->
            if (i > 0) {
                HorizontalDivider(
                    color = OrbitTokens.graphiteHair,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
            }
            LinhaBancoConcept(b)
        }
    }
}

@Composable
private fun LinhaBancoConcept(status: StatusBancoCaptura) {
    val (sub, label, corLabel) = when (status.saude) {
        SaudeBanco.OK -> Triple("recebendo avisos", "Ok", OrbitTokens.online)
        SaudeBanco.QUIETO -> Triple(status.detalhe, "Quieto", OrbitTokens.warning)
        SaudeBanco.NUNCA -> Triple("ainda sem aviso", "—", OrbitTokens.textLowN)
    }
    val letra = status.banco.rotulo.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val corBanco = corMarcaBanco(status.banco.id)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(corBanco),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                letra,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                status.banco.rotulo,
                color = OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(sub, color = OrbitTokens.textLowN, fontSize = 12.sp)
            if (status.saude == SaudeBanco.QUIETO) {
                Text(
                    "Como ligar →",
                    color = OrbitTokens.warning,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.orbitPressable { /* P2.5: instrucoes */ },
                )
            }
        }
        Text(
            label,
            color = corLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun corMarcaBanco(id: String): Color = when (id) {
    "nubank" -> Color(0xFF820AD1)
    "inter" -> Color(0xFFFF7A00)
    "itau" -> Color(0xFFEC7000)
    "c6" -> Color(0xFF2A2C32)  // grafite um pouco mais claro — 0xFF1A1A1A invisível no fundo grafite
    "picpay" -> Color(0xFF21C25E)
    "bradesco" -> Color(0xFFCC092F)
    "santander" -> Color(0xFFEC0000)
    "bb" -> Color(0xFFF9DD16)
    else -> OrbitTokens.graphiteRaised
}

private fun relativo(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    val min = diff / 60_000
    return when {
        min < 1 -> "agora"
        min < 60 -> "${min} min"
        min < 60 * 24 -> "${min / 60} h"
        else -> "${min / (60 * 24)} d"
    }
}
