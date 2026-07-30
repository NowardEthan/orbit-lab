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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.firebase.DocumentoUi
import com.ethan.orbitlab.data.firebase.FirestoreDocumentos
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * O cartão do documento no fio da conversa.
 *
 * Nasce da conversa: a Luna escreve um texto/plano/auditoria e ele fica AQUI, com um lugar
 * próprio, em vez de se diluir no chat. Um toque abre o leitor. Discreto de propósito — a
 * conversa continua sendo a protagonista; o documento é o que ficou dela.
 */
@Composable
fun DocumentoCard(
    doc: DocumentoUi,
    onAbrir: (DocumentoUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OrbitTokens.surfaceRaised)
            .border(1.dp, OrbitTokens.accentSoft, RoundedCornerShape(14.dp))
            .orbitPressable { onAbrir(doc) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(OrbitTokens.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = OrbitTokens.accentText,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.titulo,
                color = OrbitTokens.textHigh,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Documento · toque para abrir",
                color = OrbitTokens.textMid,
                fontSize = 12.sp,
            )
        }
    }
}

/**
 * O leitor — uma folha alta com o documento renderizado em Markdown, rolável.
 * Fundo sólido (não translúcido): documento é pra ler, precisa de contraste firme.
 *
 * Um toque em «Editar» vira a folha num editor: o corpo em Markdown cru num campo, e o Ethan
 * mexe à mão. «Salvar» grava de volta na estante (o listener redesenha tudo). A mão dele; a mão
 * da Luna é a de reescrever pela conversa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentoReaderSheet(
    doc: DocumentoUi,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // A edição vive em estado local, semeada do documento. `remember(doc.id)` re-semeia quando
    // troca de documento; enquanto edita, a fonte da verdade na tela é o que ele digitou.
    var editando by remember(doc.id) { mutableStateOf(false) }
    var tituloLocal by remember(doc.id) { mutableStateOf(doc.titulo) }
    var conteudoLocal by remember(doc.id) { mutableStateOf(doc.conteudo) }
    var salvando by remember(doc.id) { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DOCUMENTO",
                    color = OrbitTokens.accentText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.weight(1f),
                )
                if (!editando) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .orbitPressable { editando = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = OrbitTokens.accentText,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Editar",
                            color = OrbitTokens.accentText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            if (editando) {
                val campoCores = TextFieldDefaults.colors(
                    focusedContainerColor = OrbitTokens.surfaceRaised,
                    unfocusedContainerColor = OrbitTokens.surfaceRaised,
                    focusedTextColor = OrbitTokens.textHigh,
                    unfocusedTextColor = OrbitTokens.textHigh,
                    cursorColor = OrbitTokens.accent,
                    focusedIndicatorColor = OrbitTokens.accent,
                    unfocusedIndicatorColor = OrbitTokens.accentSoft,
                )
                OutlinedTextField(
                    value = tituloLocal,
                    onValueChange = { tituloLocal = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Título") },
                    colors = campoCores,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = conteudoLocal,
                    onValueChange = { conteudoLocal = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    label = { Text("Corpo (Markdown)") },
                    colors = campoCores,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Cancelar: descarta o que digitou, volta ao documento como está na estante.
                    Text(
                        text = "Cancelar",
                        color = OrbitTokens.textMid,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .orbitPressable(enabled = !salvando) {
                                tituloLocal = doc.titulo
                                conteudoLocal = doc.conteudo
                                editando = false
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    val podeSalvar = !salvando && conteudoLocal.isNotBlank() && tituloLocal.isNotBlank()
                    Text(
                        text = if (salvando) "Salvando…" else "Salvar",
                        color = if (podeSalvar) OrbitTokens.accentText else OrbitTokens.textMid,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (podeSalvar) OrbitTokens.accentSoft else OrbitTokens.surfaceRaised)
                            .orbitPressable(enabled = podeSalvar) {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@orbitPressable
                                salvando = true
                                scope.launch {
                                    runCatching {
                                        FirestoreDocumentos.atualizar(uid, doc.id, tituloLocal, conteudoLocal)
                                    }
                                    salvando = false
                                    editando = false
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            } else {
                Text(
                    text = tituloLocal,
                    color = OrbitTokens.textHigh,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .width(40.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(OrbitTokens.accent),
                )
                Spacer(Modifier.height(18.dp))
                LunaMarkdown(
                    content = conteudoLocal,
                    variante = LunaMarkdownVariante.Documento,
                )
            }
        }
    }
}
