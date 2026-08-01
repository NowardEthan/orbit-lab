package com.ethan.orbitlab.ui.chat

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.export.ConversaExporter
import com.ethan.orbitlab.data.export.ConversaExporter.ExportFormato
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens

/**
 * Folha de exportação de um ARTEFATO — irmã da [ExportSheet], mas parte de um corpo em Markdown
 * (não de uma conversa de bolhas). Reaproveita os visuais (pílulas de formato, botões de ação) e
 * a mesma infra de SAF/compartilhar do [ConversaExporter].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtefatoExportSheet(
    titulo: String,
    conteudo: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var formato by remember { mutableStateOf(ExportFormato.MARKDOWN) }
    // O conteúdo é montado no clique e guardado até o Uri voltar do seletor «Salvar como».
    var pendente by remember { mutableStateOf<String?>(null) }

    val salvar = rememberLauncherForActivityResult(CriarDocumento()) { uri ->
        val texto = pendente
        pendente = null
        if (uri != null && texto != null) {
            val ok = ConversaExporter.salvarNoUri(context, uri, texto)
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
                "Exportar artefato",
                color = OrbitTokens.textHigh,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Escolha o formato e como levar o artefato.",
                color = OrbitTokens.textMid,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))

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

            AcaoBotao(
                icone = Icons.Rounded.FileDownload,
                titulo = "Baixar no celular",
                subtitulo = "Salva o arquivo onde você escolher",
                destaque = true,
                onClick = {
                    pendente = ConversaExporter.montarArtefato(formato, titulo, conteudo)
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
                    ConversaExporter.compartilharConteudo(
                        context,
                        titulo,
                        ConversaExporter.montarArtefato(formato, titulo, conteudo),
                        formato,
                    )
                    onDismiss()
                },
            )
        }
    }
}
