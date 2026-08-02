package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Cartão de pergunta da Luna (ferramenta `perguntar`): quando uma escolha de gosto mudaria o
 * resultado, ela pergunta ANTES de agir, com opções tocáveis — igual às perguntas com alternativas
 * que aparecem pra ele. Tocar numa opção envia aquela resposta na hora; ele também pode ignorar e
 * escrever a própria no composer. Fica no lado dela do chat, sob a última bolha. 🌙
 */
@Composable
fun LunaPerguntaCard(
    pergunta: PerguntaLuna,
    onResponder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .clip(forma)
            .background(OrbitTokens.bubbleLuna)
            .border(1.dp, OrbitTokens.borderSoft, forma)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Cabeçalho: deixa claro que é a Luna te perguntando algo.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Rounded.HelpOutline,
                contentDescription = null,
                tint = OrbitTokens.accentText,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "Luna perguntou",
                color = OrbitTokens.accentText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            pergunta.texto,
            color = OrbitTokens.textHigh,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pergunta.opcoes.forEach { opcao ->
                OpcaoPergunta(texto = opcao, onClick = { onResponder(opcao) })
            }
        }

        Text(
            "ou escreva a sua resposta abaixo",
            color = OrbitTokens.textLow,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun OpcaoPergunta(
    texto: String,
    onClick: () -> Unit,
) {
    val forma = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(OrbitTokens.surfaceRaised)
            .border(1.dp, OrbitTokens.borderSoft, forma)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            texto,
            color = OrbitTokens.textHigh,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = OrbitTokens.accentText,
            modifier = Modifier.size(16.dp),
        )
    }
}
