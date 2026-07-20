package com.ethan.orbitlab.ui.chat

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imagens = mensagem.attachments.filter {
        it.kind == AttachmentKind.IMAGE || it.kind == AttachmentKind.VIDEO
    }
    val podeReferenciarTexto = mensagem.texto.isNotBlank() || mensagem.attachments.isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.surfaceRaised,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 20.dp),
        ) {
            Text(
                "Ações",
                color = OrbitTokens.textHigh,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))

            if (podeReferenciarTexto) {
                ActionRow(
                    icon = Icons.Rounded.FormatQuote,
                    label = "Referenciar mensagem",
                    subtitle = "Citar no composer pra Luna responder em contexto",
                    onClick = {
                        onReferenciarMensagem()
                        onDismiss()
                    },
                )
            }

            imagens.forEach { img ->
                val rotulo = if (img.kind == AttachmentKind.VIDEO) {
                    "Referenciar vídeo"
                } else {
                    "Referenciar imagem"
                }
                ActionRow(
                    icon = Icons.Rounded.Image,
                    label = rotulo,
                    subtitle = img.name,
                    onClick = {
                        onReferenciarImagem(img)
                        onDismiss()
                    },
                )
            }

            if (mensagem.texto.isNotBlank()) {
                ActionRow(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Copiar texto",
                    subtitle = null,
                    onClick = {
                        onCopiar()
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .orbitPressable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(OrbitTokens.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = OrbitTokens.accentText, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = OrbitTokens.textHigh, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = OrbitTokens.textMid, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}
