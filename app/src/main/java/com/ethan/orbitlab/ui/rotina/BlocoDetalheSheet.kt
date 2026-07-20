package com.ethan.orbitlab.ui.rotina

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.domain.rotina.BlocoRotina
import com.ethan.orbitlab.domain.rotina.ItensDoDia
import com.ethan.orbitlab.domain.rotina.SubTarefa
import com.ethan.orbitlab.domain.rotina.minutoParaHora
import com.ethan.orbitlab.domain.rotina.sessaoDeBloco
import com.ethan.orbitlab.ui.theme.OrbitTokens

@Composable
fun BlocoDetalheSheet(
    bloco: BlocoRotina,
    itens: ItensDoDia?,
    feitoHoje: Boolean,
    ehHoje: Boolean,
    onFechar: () -> Unit,
    onToggleFeito: () -> Unit,
    onMarcarTarefa: (String) -> Unit,
    onAdicionarHoje: (String) -> Unit,
) {
    val cor = try {
        Color(android.graphics.Color.parseColor(bloco.cor))
    } catch (_: Exception) {
        OrbitTokens.accent
    }
    val feitas = itens?.subsFeitas.orEmpty().toSet()
    val fixas = bloco.subtarefas.orEmpty()
    val hoje = itens?.tarefasDoDia.orEmpty()
    var novaTarefa by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onFechar),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(OrbitTokens.ink1)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(cor))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(bloco.titulo, color = OrbitTokens.textHigh, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "${minutoParaHora(bloco.inicio)} – ${minutoParaHora(bloco.fim)}",
                        color = OrbitTokens.textMid,
                        fontSize = 13.sp,
                    )
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onFechar),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = OrbitTokens.textMid)
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (ehHoje) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (feitoHoje) OrbitTokens.gold.copy(alpha = 0.15f) else OrbitTokens.surface)
                            .clickable(onClick = onToggleFeito)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (feitoHoje) OrbitTokens.gold else Color.Transparent)
                                .border(1.5.dp, if (feitoHoje) OrbitTokens.gold else OrbitTokens.border, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (feitoHoje) {
                                Icon(Icons.Rounded.Check, null, tint = Color(0xFF1A1408), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (feitoHoje) "Bloco concluído hoje" else "Marcar bloco como feito",
                            color = OrbitTokens.textHigh,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (ehHoje) {
                    SecaoTarefas(
                        titulo = "Tarefas de hoje",
                        tarefas = hoje,
                        feitas = feitas,
                        onToggle = onMarcarTarefa,
                        editavel = true,
                        nova = novaTarefa,
                        onNovaChange = { novaTarefa = it },
                        onAdicionar = {
                            onAdicionarHoje(novaTarefa)
                            novaTarefa = ""
                        },
                    )
                }

                SecaoTarefas(
                    titulo = "Fixas · todo dia",
                    tarefas = fixas,
                    feitas = if (ehHoje) feitas else emptySet(),
                    onToggle = if (ehHoje) onMarcarTarefa else { _ -> },
                    editavel = false,
                    permitirToggle = ehHoje,
                )

                if (!bloco.roteiro.isNullOrBlank()) {
                    SecaoTexto("A Luna diz", bloco.roteiro!!)
                }
                if (!bloco.guia.isNullOrBlank()) {
                    SecaoTexto("Guia completo", bloco.guia!!)
                }

                // Stub do balão Luna (chat do bloco) — sessão documentada, UI só visual.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(OrbitTokens.violet.copy(alpha = 0.12f))
                        .border(1.dp, OrbitTokens.violet.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ChatBubble, null, tint = OrbitTokens.violet, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Falar com a Luna", color = OrbitTokens.textHigh, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Em breve · sessão ${sessaoDeBloco(bloco.id)}",
                            color = OrbitTokens.textLow,
                            fontSize = 11.sp,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SecaoTarefas(
    titulo: String,
    tarefas: List<SubTarefa>,
    feitas: Set<String>,
    onToggle: (String) -> Unit,
    editavel: Boolean,
    permitirToggle: Boolean = true,
    nova: String = "",
    onNovaChange: (String) -> Unit = {},
    onAdicionar: () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titulo.uppercase(), color = OrbitTokens.textLow, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        if (tarefas.isEmpty() && !editavel) {
            Text("Nenhuma ainda.", color = OrbitTokens.textMid, fontSize = 13.sp)
        }
        tarefas.forEach { t ->
            val feita = t.id in feitas
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrbitTokens.surface.copy(alpha = 0.5f))
                    .then(if (permitirToggle) Modifier.clickable { onToggle(t.id) } else Modifier)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (feita) OrbitTokens.gold else Color.Transparent)
                        .border(1.5.dp, if (feita) OrbitTokens.gold else OrbitTokens.border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (feita) Icon(Icons.Rounded.Check, null, tint = Color(0xFF1A1408), modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        t.texto,
                        color = if (feita) OrbitTokens.textLow else OrbitTokens.textHigh,
                        fontSize = 14.sp,
                    )
                    t.hora?.let {
                        Text(minutoParaHora(it), color = OrbitTokens.textLow, fontSize = 11.sp)
                    }
                }
            }
        }
        if (editavel) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrbitTokens.surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = nova,
                    onValueChange = onNovaChange,
                    singleLine = true,
                    cursorBrush = SolidColor(OrbitTokens.accent),
                    textStyle = TextStyle(color = OrbitTokens.textHigh, fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (nova.isEmpty()) {
                            Text("Nova tarefa de hoje…", color = OrbitTokens.textLow, fontSize = 14.sp)
                        }
                        inner()
                    },
                )
                Text(
                    "Add",
                    color = OrbitTokens.accentText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAdicionar)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun SecaoTexto(titulo: String, corpo: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo.uppercase(), color = OrbitTokens.textLow, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(corpo, color = OrbitTokens.textMid, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
