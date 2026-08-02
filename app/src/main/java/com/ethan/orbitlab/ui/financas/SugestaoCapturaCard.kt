package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.captura.SugestaoCaptura
import com.ethan.orbitlab.data.financas.CategoriasFinanca
import com.ethan.orbitlab.data.financas.formatarReais
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Card compacto na fila da Captura — estilo DocumentoCard + ações da Luna.
 */
@Composable
fun SugestaoCapturaCard(
    sugestao: SugestaoCaptura,
    carteiraNome: String?,
    onRegistrar: () -> Unit,
    onEditar: () -> Unit,
    onIgnorar: () -> Unit,
    onAbrir: () -> Unit,
) {
    val cat = CategoriasFinanca.porId(sugestao.categoriaId)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(16.dp))
            .orbitPressable(onClick = onAbrir)
            .padding(14.dp),
    ) {
        Text(
            "Luna · ${sugestao.aviso.bancoRotulo}",
            color = OrbitTokens.bluePastel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Vi uma compra de ${formatarReais(sugestao.aviso.valorCentavos)} em ${sugestao.aviso.descricao}. Registro como saída de hoje?",
            color = OrbitTokens.textHiN,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${cat.emoji} ${cat.rotulo}" +
                (carteiraNome?.let { " · $it" } ?: ""),
            color = OrbitTokens.textMidN,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoAcao("Registrar", destaque = true, onClick = onRegistrar, modifier = Modifier.weight(1f))
            BotaoAcao("Editar", destaque = false, onClick = onEditar, modifier = Modifier.weight(1f))
            BotaoAcao("Ignorar", destaque = false, onClick = onIgnorar, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SugestaoCapturaSheet(
    sugestao: SugestaoCaptura,
    carteiraNome: String?,
    onDismiss: () -> Unit,
    onRegistrar: () -> Unit,
    onEditar: () -> Unit,
    onIgnorar: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cat = CategoriasFinanca.porId(sugestao.categoriaId)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.graphiteSurf,
        contentColor = OrbitTokens.textHiN,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                "A Luna captura",
                color = OrbitTokens.textHiN,
                fontSize = 20.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Vi uma compra de ${formatarReais(sugestao.aviso.valorCentavos)} no ${sugestao.aviso.descricao}. " +
                    "Registro como saída de hoje?",
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${sugestao.aviso.bancoRotulo} · ${cat.emoji} ${cat.rotulo}" +
                    (carteiraNome?.let { " · $it" } ?: " · escolha a carteira em Editar"),
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                sugestao.aviso.raw,
                color = OrbitTokens.textLowN,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotaoAcao("Registrar", destaque = true, onClick = onRegistrar, modifier = Modifier.weight(1f))
                BotaoAcao("Editar", destaque = false, onClick = onEditar, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            BotaoAcao("Ignorar", destaque = false, onClick = onIgnorar, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BotaoAcao(
    label: String,
    destaque: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (destaque) OrbitTokens.bluePastel else OrbitTokens.graphiteRaised)
            .orbitPressable(onClick = onClick)
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = if (destaque) OrbitTokens.onBluePastel else OrbitTokens.textHiN,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
