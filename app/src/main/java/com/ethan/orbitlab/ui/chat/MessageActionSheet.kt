package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.Mensagem
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionSheet(
    mensagem: Mensagem,
    onDismiss: () -> Unit,
    onReferenciarMensagem: () -> Unit,
    onReferenciarImagem: (ComposerAttachment) -> Unit,
    onCopiar: () -> Unit,
    onReenviar: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imagens = mensagem.attachments.filter {
        it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.VIDEO
    }
    val podeReferenciarTexto = mensagem.texto.isNotBlank() || mensagem.attachments.isNotEmpty()

    // Ações "leves" (referenciar / copiar) vão juntas num cartão; reenviar fica à parte
    // porque apaga o que vem depois — merece respiro e cor de aviso.
    val leves = buildList {
        if (podeReferenciarTexto) {
            add(
                AcaoItem(
                    icone = Icons.Rounded.FormatQuote,
                    label = "Referenciar mensagem",
                    subtitle = "Citar no composer pra Luna responder em contexto",
                    onClick = { onReferenciarMensagem(); onDismiss() },
                ),
            )
        }
        imagens.forEach { img ->
            add(
                AcaoItem(
                    icone = Icons.Rounded.Image,
                    label = if (img.kind == AttachmentKind.VIDEO) "Referenciar vídeo" else "Referenciar imagem",
                    subtitle = img.name,
                    onClick = { onReferenciarImagem(img); onDismiss() },
                ),
            )
        }
        if (mensagem.texto.isNotBlank()) {
            add(
                AcaoItem(
                    icone = Icons.Rounded.ContentCopy,
                    label = "Copiar texto",
                    subtitle = null,
                    onClick = { onCopiar(); onDismiss() },
                ),
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.surfaceRaised,
        // Véu bem leve: quem apaga o resto da conversa é o recuo das mensagens vizinhas
        // (ver ChatTimeline), pra que a mensagem selecionada continue acesa aqui atrás.
        scrimColor = Color.Black.copy(alpha = 0.16f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 20.dp),
        ) {
            Text(
                if (mensagem.isLuna) "Mensagem da Luna" else "Sua mensagem",
                color = OrbitTokens.textHigh,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "O que você quer fazer com ela?",
                color = OrbitTokens.textMid,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))

            if (leves.isNotEmpty()) {
                GrupoAcoes(leves)
                Spacer(Modifier.height(12.dp))
            }

            // Reenviar — destaque de aviso (some o que vem depois).
            GrupoAcoes(
                listOf(
                    AcaoItem(
                        icone = Icons.Rounded.Refresh,
                        label = "Reenviar a partir daqui",
                        subtitle = "A Luna responde de novo; o que vem depois é apagado",
                        aviso = true,
                        onClick = { onReenviar(); onDismiss() },
                    ),
                ),
            )
        }
    }
}

private data class AcaoItem(
    val icone: ImageVector,
    val label: String,
    val subtitle: String?,
    val aviso: Boolean = false,
    val onClick: () -> Unit,
)

/** Cartão que agrupa linhas de ação com divisórias finas entre elas. */
@Composable
private fun GrupoAcoes(itens: List<AcaoItem>) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, shape),
    ) {
        itens.forEachIndexed { i, item ->
            if (i > 0) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 66.dp)
                        .height(1.dp)
                        .background(OrbitTokens.borderSoft),
                )
            }
            ActionRow(item)
        }
    }
}

@Composable
private fun ActionRow(item: AcaoItem) {
    val tintIcone = if (item.aviso) OrbitTokens.warning else OrbitTokens.accentText
    // Ladrilho SEMPRE neutro — a cor mora no ícone (contraste), não numa lavagem da própria cor.
    val bgTile = OrbitTokens.surfaceRaised
    Row(
        Modifier
            .fillMaxWidth()
            .orbitPressable(onClick = item.onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(bgTile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icone, contentDescription = null, tint = tintIcone, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                item.label,
                color = if (item.aviso) OrbitTokens.warning else OrbitTokens.textHigh,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    item.subtitle,
                    color = OrbitTokens.textMid,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OrbitTokens.textLow,
            modifier = Modifier.size(18.dp),
        )
    }
}
