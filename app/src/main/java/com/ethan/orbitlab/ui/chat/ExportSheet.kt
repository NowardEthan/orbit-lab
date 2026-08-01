package com.ethan.orbitlab.ui.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.export.ConversaExporter
import com.ethan.orbitlab.data.export.ConversaExporter.ExportFormato
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/** Cria documento via SAF respeitando mime + nome (funciona do Android 5 pra cima). */
internal class CriarDocumento : ActivityResultContract<Pair<String, String>, Uri?>() {
    override fun createIntent(context: Context, input: Pair<String, String>): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.first)
            .putExtra(Intent.EXTRA_TITLE, input.second)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.takeIf { resultCode == Activity.RESULT_OK }?.data
}

/**
 * Folha de exportação da conversa — escolhe o formato e decide: baixar no aparelho
 * (seletor «Salvar como» do sistema) ou mandar pro compartilhador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    titulo: String,
    mensagens: List<com.ethan.orbitlab.data.Mensagem>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var formato by remember { mutableStateOf(ExportFormato.MARKDOWN) }
    // O conteúdo é montado no clique e guardado até o Uri voltar do seletor.
    var pendente by remember { mutableStateOf<String?>(null) }

    val salvar = rememberLauncherForActivityResult(CriarDocumento()) { uri ->
        val conteudo = pendente
        pendente = null
        if (uri != null && conteudo != null) {
            val ok = ConversaExporter.salvarNoUri(context, uri, conteudo)
            Toast.makeText(
                context,
                if (ok) "Salvo no aparelho" else "Não consegui salvar o arquivo",
                Toast.LENGTH_SHORT,
            ).show()
        }
        onDismiss()
    }

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
                "Exportar conversa",
                color = OrbitTokens.textHigh,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Escolha o formato e como levar a conversa.",
                color = OrbitTokens.textMid,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))

            // Seletor de formato (pílulas).
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormato.entries.forEach { f ->
                    FormatoPill(
                        rotulo = f.rotulo,
                        selecionado = f == formato,
                        modifier = Modifier.weight(1f),
                        onClick = { formato = f },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                formato.descricao,
                color = OrbitTokens.textLow,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 2.dp),
            )

            Spacer(Modifier.height(18.dp))

            // Ações.
            AcaoBotao(
                icone = Icons.Rounded.FileDownload,
                titulo = "Baixar no celular",
                subtitulo = "Salva o arquivo onde você escolher",
                destaque = true,
                onClick = {
                    pendente = ConversaExporter.montar(formato, titulo, mensagens)
                    salvar.launch(formato.mime to ConversaExporter.nomeArquivo(titulo, formato))
                },
            )
            Spacer(Modifier.height(10.dp))
            AcaoBotao(
                icone = Icons.Rounded.IosShare,
                titulo = "Compartilhar",
                subtitulo = "Manda pra outro app (WhatsApp, Drive…)",
                destaque = false,
                onClick = {
                    ConversaExporter.compartilhar(
                        context,
                        listOf(ConversaExporter.Item(titulo, mensagens)),
                        formato,
                    )
                    onDismiss()
                },
            )
        }
    }
}

@Composable
internal fun FormatoPill(
    rotulo: String,
    selecionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusPill)
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selecionado) OrbitTokens.accent else OrbitTokens.surface)
            .border(
                1.dp,
                if (selecionado) OrbitTokens.accent else OrbitTokens.borderSoft,
                shape,
            )
            .orbitPressable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecionado) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            rotulo,
            color = if (selecionado) Color.White else OrbitTokens.textMid,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun AcaoBotao(
    icone: ImageVector,
    titulo: String,
    subtitulo: String,
    destaque: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val bg = if (destaque) OrbitTokens.accent else OrbitTokens.surface
    val corTitulo = if (destaque) Color.White else OrbitTokens.textHigh
    val corSub = if (destaque) Color.White.copy(alpha = 0.82f) else OrbitTokens.textMid
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .then(if (destaque) Modifier else Modifier.border(1.dp, OrbitTokens.borderSoft, shape))
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (destaque) Color.White.copy(alpha = 0.16f) else OrbitTokens.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icone,
                contentDescription = null,
                tint = if (destaque) Color.White else OrbitTokens.accentText,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, color = corTitulo, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(1.dp))
            Text(subtitulo, color = corSub, fontSize = 12.sp)
        }
    }
}
