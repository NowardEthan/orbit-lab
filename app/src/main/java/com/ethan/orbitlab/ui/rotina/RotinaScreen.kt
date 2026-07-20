package com.ethan.orbitlab.ui.rotina

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ethan.orbitlab.domain.rotina.BlocoRotina
import com.ethan.orbitlab.domain.rotina.DIAS_CURTOS
import com.ethan.orbitlab.domain.rotina.DiaSemana
import com.ethan.orbitlab.domain.rotina.EstadoAgora
import com.ethan.orbitlab.domain.rotina.EstadoDoBloco
import com.ethan.orbitlab.domain.rotina.ItemListaRotina
import com.ethan.orbitlab.domain.rotina.ROTINA_NORMAL
import com.ethan.orbitlab.domain.rotina.RotinaSet
import com.ethan.orbitlab.domain.rotina.duracaoDoBloco
import com.ethan.orbitlab.domain.rotina.duracaoLegivel
import com.ethan.orbitlab.domain.rotina.minutoParaHora
import com.ethan.orbitlab.domain.rotina.periodoLegivel
import com.ethan.orbitlab.ui.theme.OrbitTokens

private fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    OrbitTokens.accent
}

@Composable
fun RotinaScreen(
    onFechar: () -> Unit,
    vm: RotinaViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var blocoDetalhe by remember { mutableStateOf<BlocoRotina?>(null) }
    val blocoVivo = blocoDetalhe?.let { aberto ->
        state.blocos.find { it.id == aberto.id } ?: aberto
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(OrbitTokens.ink1)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            CabecalhoRotina(onFechar = onFechar)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ChipsRotina(
                    sets = state.sets,
                    setAtivo = state.setAtivo,
                    vigenteHoje = state.vigenteHoje,
                    onSelecionar = { vm.selecionarRotina(it) },
                )

                if (!state.vendoAVigente) {
                    Text(
                        "A ver uma rotina que não é a de hoje — edições valem, mas ela só cobra no período.",
                        color = OrbitTokens.warning,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }

                if (state.diaVisto == state.diaHoje) {
                    CartaoAgora(state.estado)
                }

                FaixaDias(
                    diaHoje = state.diaHoje,
                    diaVisto = state.diaVisto,
                    contagem = state.contagemPorDia,
                    onDia = { vm.selecionarDia(it) },
                )

                if (state.conflitos.isNotEmpty()) {
                    Text(
                        "Há blocos a sobrepor-se hoje — confira os horários.",
                        color = OrbitTokens.danger,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }

                ListaBlocos(
                    itens = state.itensLista,
                    estadosHoje = state.estadosHoje,
                    itensHoje = state.itensHoje,
                    estado = state.estado,
                    ehHoje = state.diaVisto == state.diaHoje,
                    onToggleFeito = { vm.alternarFeitoHoje(it) },
                    onAbrir = { blocoDetalhe = it },
                )
            }
        }

        blocoVivo?.let { bloco ->
            BlocoDetalheSheet(
                bloco = bloco,
                itens = state.itensHoje[bloco.id],
                feitoHoje = state.estadosHoje[bloco.id] == EstadoDoBloco.Feito,
                ehHoje = state.diaVisto == state.diaHoje,
                onFechar = { blocoDetalhe = null },
                onToggleFeito = { vm.alternarFeitoHoje(bloco.id) },
                onMarcarTarefa = { tid -> vm.marcarTarefa(bloco.id, tid) },
                onAdicionarHoje = { texto -> vm.adicionarTarefaDoDia(bloco.id, texto) },
            )
        }
    }
}

@Composable
private fun CabecalhoRotina(onFechar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onFechar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Fechar", tint = OrbitTokens.textHigh)
        }
        Spacer(Modifier.width(8.dp))
        Text("Rotina", color = OrbitTokens.textHigh, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ChipsRotina(
    sets: List<RotinaSet>,
    setAtivo: String,
    vigenteHoje: String,
    onSelecionar: (String?) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChipRotina(
            nome = "Normal",
            cor = OrbitTokens.accent,
            ativo = setAtivo == ROTINA_NORMAL,
            vigente = vigenteHoje == ROTINA_NORMAL,
            onClick = { onSelecionar(null) },
        )
        sets.forEach { set ->
            ChipRotina(
                nome = set.nome,
                cor = parseHex(set.cor ?: "#4B75F2"),
                ativo = setAtivo == set.id,
                vigente = vigenteHoje == set.id,
                subtitulo = periodoLegivel(set),
                onClick = { onSelecionar(set.id) },
            )
        }
    }
}

@Composable
private fun ChipRotina(
    nome: String,
    cor: Color,
    ativo: Boolean,
    vigente: Boolean,
    subtitulo: String? = null,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (ativo) cor.copy(alpha = 0.22f) else OrbitTokens.surface)
            .border(
                1.dp,
                if (ativo) cor.copy(alpha = 0.6f) else OrbitTokens.borderSoft,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (vigente) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(OrbitTokens.online))
                Spacer(Modifier.width(6.dp))
            }
            Text(nome, color = OrbitTokens.textHigh, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        if (subtitulo != null) {
            Text(subtitulo, color = OrbitTokens.textLow, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CartaoAgora(estado: EstadoAgora) {
    val titulo: String
    val sub: String
    when {
        estado.atual != null -> {
            titulo = "Agora · ${estado.atual.titulo}"
            sub = "Falta ${duracaoLegivel(estado.faltamMinutos ?: 0)}" +
                (estado.proximo?.let { " · a seguir ${it.titulo}" }.orEmpty())
        }
        estado.proximo != null -> {
            titulo = "Próximo · ${estado.proximo.titulo}"
            val quando = if (estado.proximoEhAmanha) "amanhã" else "em ${duracaoLegivel(estado.emMinutos ?: 0)}"
            sub = "$quando · ${minutoParaHora(estado.proximo.inicio)}"
        }
        else -> {
            titulo = "Rotina"
            sub = "Nenhum bloco neste momento"
        }
    }

    Column(
        Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OrbitTokens.surfaceRaised)
            .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(OrbitTokens.online))
            Spacer(Modifier.width(8.dp))
            Text(titulo, color = OrbitTokens.textHigh, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(sub, color = OrbitTokens.textMid, fontSize = 13.sp)
    }
}

@Composable
private fun FaixaDias(
    diaHoje: DiaSemana,
    diaVisto: DiaSemana,
    contagem: Map<DiaSemana, Int>,
    onDia: (DiaSemana) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DIAS_CURTOS.forEachIndexed { i, label ->
            val ativo = i == diaVisto
            val hoje = i == diaHoje
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (ativo) OrbitTokens.accentSoft else Color.Transparent)
                    .clickable { onDia(i) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    label,
                    color = if (ativo) OrbitTokens.accentText else OrbitTokens.textLow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${contagem[i] ?: 0}",
                    color = if (ativo) OrbitTokens.textHigh else OrbitTokens.textMid,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (hoje) {
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(OrbitTokens.gold),
                    )
                }
            }
        }
    }
}

@Composable
private fun ListaBlocos(
    itens: List<ItemListaRotina>,
    estadosHoje: Map<String, EstadoDoBloco>,
    itensHoje: Map<String, com.ethan.orbitlab.domain.rotina.ItensDoDia>,
    estado: EstadoAgora,
    ehHoje: Boolean,
    onToggleFeito: (String) -> Unit,
    onAbrir: (BlocoRotina) -> Unit,
) {
    Column(
        Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (itens.isEmpty()) {
            Text(
                "Nenhum bloco neste dia. Toque noutro dia ou cria um bloco.",
                color = OrbitTokens.textMid,
                fontSize = 14.sp,
            )
            return
        }
        itens.forEach { item ->
            when (item) {
                is ItemListaRotina.Vazio -> {
                    Text(
                        "· ${duracaoLegivel(item.minutos)} livre ·",
                        color = OrbitTokens.textLow,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                is ItemListaRotina.Bloco -> {
                    val b = item.bloco
                    val itensBloco = itensHoje[b.id]
                    val fixas = b.subtarefas.orEmpty()
                    val feitas = itensBloco?.subsFeitas.orEmpty().toSet()
                    val extras = itensBloco?.tarefasDoDia.orEmpty()
                    val total = fixas.size + extras.size
                    val feitasN = fixas.count { it.id in feitas } + extras.count { it.id in feitas }
                    BlocoCard(
                        bloco = b,
                        agora = ehHoje && estado.atual?.id == b.id,
                        ehProximo = ehHoje && estado.proximo?.id == b.id && estado.atual == null,
                        feitoHoje = estadosHoje[b.id] == EstadoDoBloco.Feito,
                        tarefasFeitas = feitasN,
                        tarefasTotal = total,
                        mostrarToggle = ehHoje,
                        onToggleFeito = { onToggleFeito(b.id) },
                        onPress = { onAbrir(b) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BlocoCard(
    bloco: BlocoRotina,
    agora: Boolean,
    ehProximo: Boolean,
    feitoHoje: Boolean,
    tarefasFeitas: Int,
    tarefasTotal: Int,
    mostrarToggle: Boolean,
    onToggleFeito: () -> Unit,
    onPress: () -> Unit,
) {
    val cor = parseHex(bloco.cor)
    val dur = duracaoDoBloco(bloco)
    val barra = (3f + ((dur - 30).coerceIn(0, 8 * 60).toFloat() / (8 * 60)) * 3f).dp

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (agora) OrbitTokens.surfaceHover else OrbitTokens.surface)
            .border(
                width = if (agora) 1.5.dp else 1.dp,
                color = if (agora) cor.copy(alpha = 0.5f) else OrbitTokens.borderSoft,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onPress)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(barra)
                .height(52.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(cor),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.width(52.dp)) {
            Text(minutoParaHora(bloco.inicio), color = OrbitTokens.textHigh, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(minutoParaHora(bloco.fim), color = OrbitTokens.textLow, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(bloco.titulo, color = OrbitTokens.textHigh, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (agora) Text("● agora", color = OrbitTokens.online, fontSize = 11.sp)
                else if (ehProximo) Text("a seguir", color = OrbitTokens.accentText, fontSize = 11.sp)
                if (tarefasTotal > 0) {
                    Text("$tarefasFeitas/$tarefasTotal ✓", color = OrbitTokens.textMid, fontSize = 11.sp)
                }
            }
        }
        if (mostrarToggle) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (feitoHoje) OrbitTokens.gold else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (feitoHoje) OrbitTokens.gold else OrbitTokens.border,
                        CircleShape,
                    )
                    .clickable(onClick = onToggleFeito),
                contentAlignment = Alignment.Center,
            ) {
                if (feitoHoje) {
                    Icon(Icons.Rounded.Check, contentDescription = "Feito", tint = Color(0xFF1A1408), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
