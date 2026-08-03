package com.ethan.orbitlab.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.ethan.orbitlab.data.firebase.DocumentoUi
import com.ethan.orbitlab.data.firebase.FirestoreDocumentos
import com.ethan.orbitlab.data.firebase.VersaoUi
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/** Assinatura do artefato — antes um degradê violeta; agora o acento único (azul pastel sólido). */
private val gradienteArtefato: Brush get() = SolidColor(OrbitTokens.bluePastel)

/**
 * O cartão do artefato no fio da conversa.
 *
 * Nasce da conversa: a Luna escreve um texto/plano/auditoria e ele fica AQUI, com um lugar
 * próprio, em vez de se diluir no chat. Um toque abre o leitor. Discreto de propósito — a
 * conversa continua sendo a protagonista; o artefato é o que ficou dela.
 *
 * (Chama-se «artefato» na tela pra não se confundir com os ARQUIVOS/PDFs que o usuário anexa.
 * Por dentro a coleção Firestore ainda é `documentos` e os símbolos mantêm «Documento».)
 */
@Composable
fun DocumentoCard(
    doc: DocumentoUi,
    onAbrir: (DocumentoUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable { onAbrir(doc) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Ladrilho neutro + ícone azul pastel: identidade discreta no fio, sem virar mais um badge.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.titulo,
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Artefato · toque para abrir",
                color = OrbitTokens.textMidN,
                fontSize = 12.sp,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * O acesso flutuante à estante — uma pílula no canto superior direito do fio que abre a lista
 * dos artefatos DESTA conversa.
 *
 * Substitui os cartões soltos entre as bolhas (que poluíam e roubavam a vez da conversa): o
 * artefato deixa de disputar espaço no fio e passa a morar num lugar só, à mão. A pílula se
 * nomeia («N artefatos») pra não virar um ícone-charada, e é sólida (superfície + sombra) porque
 * flutua sobre o texto rolando — precisa de fundo, não de um banho translúcido.
 */
@Composable
fun EstanteFabConversa(
    quantidade: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .shadow(10.dp, shape, clip = false)
            .clip(shape)
            .background(OrbitTokens.graphiteRaised)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable(onClick = onClick)
            .padding(start = 6.dp, end = 13.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Ladrilho neutro + ícone azul pastel: contraste sem virar mais um badge colorido.
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(OrbitTokens.graphiteSurf),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            text = if (quantidade == 1) "1 artefato" else "$quantidade artefatos",
            color = OrbitTokens.textHiN,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * A estante da conversa — folha que lista os artefatos criados aqui, do mais recente pro mais
 * antigo. Cada linha abre o leitor (o mesmo de sempre). É o destino da pílula flutuante.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstanteConversaSheet(
    documentos: List<DocumentoUi>,
    onAbrir: (DocumentoUi) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ordenados = remember(documentos) {
        documentos.sortedByDescending { maxOf(it.updatedAtMs, it.createdAtMs) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.graphiteRaised,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OrbitTokens.graphiteHair),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 12.dp),
        ) {
            Text(
                "Artefatos",
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (ordenados.size == 1) {
                    "O que a Luna criou nesta conversa."
                } else {
                    "${ordenados.size} artefatos criados nesta conversa."
                },
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(16.dp))
            ordenados.forEachIndexed { i, doc ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                LinhaArtefatoEstante(doc = doc, onClick = { onAbrir(doc) })
            }
        }
    }
}

/** Uma linha da estante — ladrilho de identidade, título, e o último gesto (criado/editado). */
@Composable
private fun LinhaArtefatoEstante(doc: DocumentoUi, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val editado = doc.updatedAtMs > doc.createdAtMs + 2000
    val autor = if (editado) doc.updatedBy else doc.origem
    val quem = if (autor == "user") "por você" else "pela Luna"
    val verbo = if (editado) "editado" else "criado"
    val quando = tempoRelativo(if (editado) doc.updatedAtMs else doc.createdAtMs)
    val sub = listOf(verbo, quem, quando).filter { it.isNotBlank() }.joinToString(" ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(19.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = doc.titulo,
                color = OrbitTokens.textHiN,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                color = OrbitTokens.textMidN,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * O leitor — uma TELA CHEIA fixa (não mais bottom-sheet), com o artefato renderizado em
 * Markdown, rolável. Fundo sólido (ink1) + atmosfera discreta, como a tela de Novidades:
 * artefato é pra ler, precisa de contraste firme e de não "empurrar" o resto pra baixo.
 *
 * Um toque em «Editar» vira a tela num editor: o corpo em Markdown cru num campo, e o Ethan
 * mexe à mão. «Salvar» grava de volta na estante (o listener redesenha tudo). A mão dele; a mão
 * da Luna é a de reescrever pela conversa. O menu ⋮ traz as ações de folha: exportar, copiar,
 * renomear, duplicar e apagar — o artefato como ferramenta, não só vitrine.
 */
@Composable
fun DocumentoReaderSheet(
    doc: DocumentoUi,
    onDismiss: () -> Unit,
    onReferenciarTrecho: ((ThreadReference.ArtefatoTrecho) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current

    // A edição vive em estado local, semeada do artefato. `remember(doc.id)` re-semeia quando
    // troca de artefato; enquanto edita, a fonte da verdade na tela é o que ele digitou.
    var editando by remember(doc.id) { mutableStateOf(false) }
    var tituloLocal by remember(doc.id) { mutableStateOf(doc.titulo) }
    var conteudoLocal by remember(doc.id) { mutableStateOf(doc.conteudo) }
    // O corpo em edição carrega o cursor (o verniz precisa saber a linha ativa pra revelar o
    // Markdown só ali). Re-semeado ao ENTRAR na edição, com o cursor no fim.
    var editorValor by remember(doc.id) { mutableStateOf(TextFieldValue(doc.conteudo)) }
    LaunchedEffect(editando) {
        if (editando) {
            editorValor = TextFieldValue(conteudoLocal, selection = TextRange(conteudoLocal.length))
        }
    }

    // Modo «marcar trecho»: o corpo vira um campo só-leitura sobre a fonte CRUA (com o verniz que
    // a faz parecer renderizada). A seleção dá offsets EXATOS na fonte — `substring` devolve o
    // trecho tal e qual, pronto pra virar `trecho_antigo` da mão cirúrgica. Nada de adivinhar.
    var selecionando by remember(doc.id) { mutableStateOf(false) }
    var refValor by remember(doc.id) { mutableStateOf(TextFieldValue(doc.conteudo)) }
    LaunchedEffect(selecionando) {
        if (selecionando) refValor = TextFieldValue(conteudoLocal)
    }
    val selInicio = minOf(refValor.selection.start, refValor.selection.end)
    val selFim = maxOf(refValor.selection.start, refValor.selection.end)
    val trechoMarcado = if (selFim > selInicio) refValor.text.substring(selInicio, selFim) else ""

    // Estados das ações do menu ⋮.
    var menuAberto by remember(doc.id) { mutableStateOf(false) }
    var exportarAberto by remember(doc.id) { mutableStateOf(false) }
    var renomeando by remember(doc.id) { mutableStateOf(false) }
    var confirmandoApagar by remember(doc.id) { mutableStateOf(false) }
    var historicoAberto by remember(doc.id) { mutableStateOf(false) }
    var canoneAberto by remember(doc.id) { mutableStateOf(false) }
    var acaoEmCurso by remember(doc.id) { mutableStateOf(false) }
    // A bíblia acompanha mudanças externas (a Luna anotou via `anotar_canone`) quando o painel
    // está fechado — mesmo cuidado do corpo/título acima.
    var canoneLocal by remember(doc.id) { mutableStateOf(doc.canone) }
    LaunchedEffect(doc.canone) {
        if (!canoneAberto) canoneLocal = doc.canone
    }

    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // Quando o artefato muda POR FORA (a Luna reescreveu, ou uma restauração acabou de gravar) e a
    // gente NÃO está editando, a tela acompanha. Sem isto o leitor ficava preso ao texto semeado na
    // abertura — invisível até então, mas gritante com o histórico (restaurar não "pegava" na tela).
    LaunchedEffect(doc.conteudo, doc.titulo) {
        if (!editando) {
            conteudoLocal = doc.conteudo
            tituloLocal = doc.titulo
        }
    }

    // Edição ao vivo, na própria tela: sair da edição PERSISTE o que ele digitou (título+corpo)
    // quando há mudança real e nada ficou vazio. Se esvaziou tudo, volta ao que estava — não
    // deixa a estante com um artefato em branco. Sem tela de formulário, sem botão de descartar.
    fun salvarESair() {
        val t = tituloLocal.trim()
        val c = conteudoLocal
        if (uid != null && t.isNotBlank() && c.isNotBlank()) {
            if (t != doc.titulo || c != doc.conteudo) {
                scope.launch {
                    runCatching { FirestoreDocumentos.atualizar(uid, doc.id, t, c) }
                }
            }
            tituloLocal = t
        } else {
            tituloLocal = doc.titulo
            conteudoLocal = doc.conteudo
        }
        editando = false
    }

    val conteudoLimpo = remember(conteudoLocal, tituloLocal) {
        val lines = conteudoLocal.lines()
        if (lines.isNotEmpty()) {
            val firstLineClean = lines.first().trim().removePrefix("#").trim()
            val tituloClean = tituloLocal.trim().removePrefix("#").trim()
            if (firstLineClean.equals(tituloClean, ignoreCase = true) ||
                (tituloClean.length > 5 && firstLineClean.contains(tituloClean, ignoreCase = true))) {
                lines.drop(1).dropWhile { it.isBlank() }.joinToString("\n")
            } else {
                conteudoLocal
            }
        } else {
            conteudoLocal
        }
    }

    Dialog(
        onDismissRequest = {
            when {
                editando -> salvarESair()
                selecionando -> selecionando = false
                else -> onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OrbitTokens.graphiteBg)
                .orbitEnter(),
        ) {
            AtmosferaArtefato()

            // imePadding: o Dialog é edge-to-edge (decorFitsSystemWindows=false); sem isto o
            // teclado sobe na frente do campo ao editar. Espelha ChatScreen / FinancasLunaChatSheet.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                // ── Cabeçalho fixo: voltar · identidade · ações ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OrbitMetrics.pagePadding)
                        .padding(top = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BotaoIconeChrome(
                        icone = Icons.Rounded.ArrowBackIosNew,
                        descricao = "Fechar",
                    ) {
                        when {
                            editando -> salvarESair()
                            selecionando -> selecionando = false
                            else -> onDismiss()
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    if (editando) {
                        Text(
                            text = "Editando",
                            color = OrbitTokens.textMidN,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.2).sp,
                            modifier = Modifier.weight(1f),
                        )
                        BotaoConcluir { salvarESair() }
                    } else if (selecionando) {
                        Text(
                            text = "Marque o trecho",
                            color = OrbitTokens.textMidN,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.2).sp,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        TagArtefato()
                        Spacer(Modifier.weight(1f))
                        BotaoEditar { editando = true }
                        Spacer(Modifier.width(8.dp))
                        // O menu ⋮ — as ações de folha (estilo Notion).
                        Box {
                            BotaoIconeChrome(
                                icone = Icons.Rounded.MoreVert,
                                descricao = "Mais ações",
                            ) { menuAberto = true }
                            DropdownMenu(
                                expanded = menuAberto,
                                onDismissRequest = { menuAberto = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = OrbitTokens.graphiteRaised,
                                border = BorderStroke(1.dp, OrbitTokens.graphiteHair),
                                tonalElevation = 0.dp,
                                shadowElevation = 14.dp,
                                offset = DpOffset(0.dp, 6.dp),
                                modifier = Modifier.width(224.dp),
                            ) {
                                ItemMenu("Exportar", Icons.Rounded.FileDownload) {
                                    menuAberto = false
                                    exportarAberto = true
                                }
                                ItemMenu("Copiar texto", Icons.Rounded.ContentCopy) {
                                    menuAberto = false
                                    clipboard.setText(AnnotatedString(conteudoLocal))
                                    Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                                }
                                ItemMenu("Renomear", Icons.Rounded.DriveFileRenameOutline) {
                                    menuAberto = false
                                    renomeando = true
                                }
                                ItemMenu("Duplicar", Icons.Rounded.FileCopy) {
                                    menuAberto = false
                                    if (uid != null && !acaoEmCurso) {
                                        acaoEmCurso = true
                                        scope.launch {
                                            val ok = runCatching { FirestoreDocumentos.duplicar(uid, doc.id) }.isSuccess
                                            acaoEmCurso = false
                                            Toast.makeText(
                                                context,
                                                if (ok) "Artefato duplicado" else "Não consegui duplicar",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            if (ok) onDismiss()
                                        }
                                    }
                                }
                                ItemMenu("Cânone", Icons.Rounded.MenuBook) {
                                    menuAberto = false
                                    canoneAberto = true
                                }
                                ItemMenu("Histórico de versões", Icons.Rounded.History) {
                                    menuAberto = false
                                    historicoAberto = true
                                }
                                if (onReferenciarTrecho != null) {
                                    ItemMenu("Marcar trecho pra Luna", Icons.Rounded.FormatQuote) {
                                        menuAberto = false
                                        selecionando = true
                                    }
                                }
                                DivisorMenu()
                                ItemMenu("Apagar", Icons.Rounded.DeleteOutline, perigo = true) {
                                    menuAberto = false
                                    confirmandoApagar = true
                                }
                            }
                        }
                    }
                }
                // Fio-de-cabelo separando o chrome do conteúdo.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OrbitTokens.graphiteHair.copy(alpha = 0.65f)),
                )

                // ── Corpo rolável ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = OrbitMetrics.pagePadding)
                        .padding(top = 8.dp, bottom = 36.dp),
                ) {
                    if (editando) {
                        // Edição no lugar: título e corpo viram campos sem moldura, com a MESMA
                        // tipografia do leitor. É o Markdown cru, editável ao vivo bem aqui — vira
                        // heading/negrito/checkbox quando ele conclui. Fonte-do-texto estilo Obsidian,
                        // não um formulário à parte.
                        BasicTextField(
                            value = tituloLocal,
                            onValueChange = { tituloLocal = it },
                            textStyle = TextStyle(
                                color = OrbitTokens.textHiN,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                lineHeight = 32.sp,
                            ),
                            cursorBrush = SolidColor(OrbitTokens.bluePastel),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (tituloLocal.isEmpty()) {
                                    Text(
                                        text = "Título do artefato",
                                        color = OrbitTokens.textLowN,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp,
                                        lineHeight = 32.sp,
                                    )
                                }
                                inner()
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier
                                .width(44.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(gradienteArtefato),
                        )
                        Spacer(Modifier.height(18.dp))
                        BasicTextField(
                            value = editorValor,
                            onValueChange = {
                                editorValor = it
                                conteudoLocal = it.text
                            },
                            textStyle = TextStyle(
                                color = OrbitTokens.textHiN,
                                fontSize = 16.sp,
                                lineHeight = 26.sp,
                            ),
                            cursorBrush = SolidColor(OrbitTokens.bluePastel),
                            // O verniz: fora da linha do cursor, some com o `##`/`>` e mostra a linha
                            // já como título/citação. Na linha ativa, os marcadores voltam esmaecidos
                            // pra editar. É a mágica "live preview" do Obsidian.
                            visualTransformation = vernizMarkdown(
                                editorValor.selection.start,
                                editorValor.selection.end,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 300.dp),
                            decorationBox = { inner ->
                                if (editorValor.text.isEmpty()) {
                                    Text(
                                        text = "Escreve aqui em Markdown…",
                                        color = OrbitTokens.textLowN,
                                        fontSize = 16.sp,
                                        lineHeight = 26.sp,
                                    )
                                }
                                inner()
                            },
                        )
                    } else if (selecionando) {
                        // Marcar trecho: o corpo é a fonte CRUA num campo só-leitura, com o mesmo verniz
                        // do editor pra parecer renderizado. O que ele arrasta = offsets exatos na fonte.
                        Text(
                            text = tituloLocal,
                            color = OrbitTokens.textHiN,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp,
                            lineHeight = 28.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(OrbitTokens.graphiteSurf)
                                .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.FormatQuote,
                                contentDescription = null,
                                tint = OrbitTokens.bluePastel,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Arraste sobre o texto pra marcar o trecho que a Luna deve mudar.",
                                color = OrbitTokens.textMidN,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        BasicTextField(
                            value = refValor,
                            onValueChange = { refValor = it },
                            readOnly = true,
                            textStyle = TextStyle(
                                color = OrbitTokens.textHiN,
                                fontSize = 16.sp,
                                lineHeight = 26.sp,
                            ),
                            cursorBrush = SolidColor(OrbitTokens.bluePastel),
                            visualTransformation = vernizMarkdown(
                                refValor.selection.start,
                                refValor.selection.end,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 140.dp),
                        )
                    } else {
                        Text(
                            text = tituloLocal,
                            color = OrbitTokens.textHiN,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            lineHeight = 32.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .width(36.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(gradienteArtefato),
                        )
                        Spacer(Modifier.height(6.dp))
                        MetaArtefato(doc = doc, conteudo = conteudoLocal)
                        Spacer(Modifier.height(10.dp))
                        LunaMarkdown(
                            content = conteudoLimpo,
                            variante = LunaMarkdownVariante.Documento,
                            onToggleCheck = if (uid != null) {
                                { idx ->
                                    val novo = alternarChecklistNoTexto(conteudoLocal, idx)
                                    conteudoLocal = novo
                                    scope.launch {
                                        runCatching {
                                            // Marcar item não é reescrita — não versiona (senão cada
                                            // ☑ vira uma "versão" e enche o histórico de ruído).
                                            FirestoreDocumentos.atualizar(
                                                uid, doc.id, tituloLocal, novo, versionar = false,
                                            )
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            // ── Barra flutuante de marcar trecho (só no modo seleção) ──
            if (selecionando && onReferenciarTrecho != null) {
                BarraReferenciarTrecho(
                    trecho = trechoMarcado,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onReferenciar = {
                        val ref = buildArtefatoTrechoReference(doc.id, tituloLocal, trechoMarcado)
                        if (ref != null) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selecionando = false
                            onReferenciarTrecho(ref)
                            onDismiss()
                        }
                    },
                )
            }
        }
    }

    // ── Folha de exportar (formatos + baixar/compartilhar) ──
    if (exportarAberto) {
        ArtefatoExportSheet(
            titulo = tituloLocal,
            conteudo = conteudoLocal,
            onDismiss = { exportarAberto = false },
        )
    }

    // ── Renomear (só o título, rápido) ──
    if (renomeando) {
        var nomeNovo by remember(doc.id) { mutableStateOf(tituloLocal) }
        AlertDialog(
            onDismissRequest = { renomeando = false },
            containerColor = OrbitTokens.graphiteRaised,
            title = { Text("Renomear artefato", color = OrbitTokens.textHiN) },
            text = {
                OutlinedTextField(
                    value = nomeNovo,
                    onValueChange = { nomeNovo = it },
                    singleLine = true,
                    label = { Text("Título") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = OrbitTokens.graphiteSurf,
                        unfocusedContainerColor = OrbitTokens.graphiteSurf,
                        focusedTextColor = OrbitTokens.textHiN,
                        unfocusedTextColor = OrbitTokens.textHiN,
                        cursorColor = OrbitTokens.bluePastel,
                        focusedIndicatorColor = OrbitTokens.bluePastel,
                        unfocusedIndicatorColor = OrbitTokens.graphiteHair,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nomeNovo.isNotBlank() && !acaoEmCurso,
                    onClick = {
                        if (uid != null) {
                            val alvo = nomeNovo.trim()
                            acaoEmCurso = true
                            scope.launch {
                                runCatching { FirestoreDocumentos.renomear(uid, doc.id, alvo) }
                                acaoEmCurso = false
                                tituloLocal = alvo
                                renomeando = false
                            }
                        }
                    },
                ) { Text("Salvar", color = OrbitTokens.bluePastel) }
            },
            dismissButton = {
                TextButton(onClick = { renomeando = false }) {
                    Text("Cancelar", color = OrbitTokens.textMidN)
                }
            },
        )
    }

    // ── Apagar (com confirmação — some do fio) ──
    if (confirmandoApagar) {
        AlertDialog(
            onDismissRequest = { confirmandoApagar = false },
            containerColor = OrbitTokens.graphiteRaised,
            title = { Text("Apagar artefato?", color = OrbitTokens.textHiN) },
            text = {
                Text(
                    "«$tituloLocal» sai da estante e desaparece desta conversa. Não dá pra desfazer.",
                    color = OrbitTokens.textMidN,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !acaoEmCurso,
                    onClick = {
                        if (uid != null) {
                            acaoEmCurso = true
                            scope.launch {
                                val ok = runCatching { FirestoreDocumentos.apagar(uid, doc.id) }.isSuccess
                                acaoEmCurso = false
                                confirmandoApagar = false
                                if (ok) {
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Não consegui apagar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                ) { Text("Apagar", color = OrbitTokens.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoApagar = false }) {
                    Text("Cancelar", color = OrbitTokens.textMidN)
                }
            },
        )
    }

    // ── Cânone (a bíblia dos fatos fixos, por cima do leitor) ──
    if (canoneAberto) {
        CanoneArtefatoSheet(
            docId = doc.id,
            canoneInicial = canoneLocal,
            uid = uid,
            onDismiss = { canoneAberto = false },
            onSalvo = { canoneLocal = it },
        )
    }

    // ── Histórico de versões (o álbum de fotos, por cima do leitor) ──
    if (historicoAberto) {
        HistoricoArtefatoSheet(
            docId = doc.id,
            uid = uid,
            conteudoAtual = doc.conteudo,
            onDismiss = { historicoAberto = false },
            onRestaurar = { versao ->
                if (uid != null && !acaoEmCurso) {
                    acaoEmCurso = true
                    scope.launch {
                        val ok = runCatching {
                            FirestoreDocumentos.restaurar(uid, doc.id, versao)
                        }.isSuccess
                        acaoEmCurso = false
                        if (ok) {
                            // Reflete já na tela — o host (chat/estante) pode segurar um snapshot
                            // congelado do doc, então não espera o listener pra "pegar".
                            tituloLocal = versao.titulo
                            conteudoLocal = versao.conteudo
                            historicoAberto = false
                            Toast.makeText(context, "Versão restaurada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Não consegui restaurar", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
        )
    }
}

/** Atmosfera discreta do leitor — dois brilhos radiais fracos, ecoando a tela de Novidades. */
@Composable
private fun AtmosferaArtefato() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 48.dp, y = (-30).dp)
                .size(200.dp)
                .background(
                    Brush.radialGradient(
                        listOf(OrbitTokens.bluePastel.copy(alpha = 0.08f), Color.Transparent),
                    ),
                ),
        )
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-56).dp, y = 120.dp)
                .size(160.dp)
                .background(
                    Brush.radialGradient(
                        listOf(OrbitTokens.bluePastel.copy(alpha = 0.05f), Color.Transparent),
                    ),
                ),
        )
    }
}

/** A "cara" do artefato no cabeçalho — chip grafite discreto com ícone azul pastel. */
@Composable
private fun TagArtefato() {
    val shape = RoundedCornerShape(OrbitMetrics.radiusPill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = OrbitTokens.bluePastel,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Artefato",
            color = OrbitTokens.textHiN,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.1.sp,
        )
    }
}

/** Botão de chrome circular (voltar / ⋮) — superfície neutra + fio-de-cabelo, ícone claro. */
@Composable
private fun BotaoIconeChrome(
    icone: ImageVector,
    descricao: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, CircleShape)
            .orbitPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icone,
            contentDescription = descricao,
            tint = OrbitTokens.textHiN,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Botão «Editar» — pílula grafite neutra com ícone em azul pastel. */
@Composable
private fun BotaoEditar(onClick: () -> Unit) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusPill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = null,
            tint = OrbitTokens.bluePastel,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Editar",
            color = OrbitTokens.textHiN,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * A linha de metadados sob o título — o "quem mexeu e quando" do Notion. Mostra o último gesto
 * (criado/editado, pela Luna ou por você, há quanto tempo) e o tamanho da leitura. Discreta:
 * informa sem competir com o texto.
 */
@Composable
private fun MetaArtefato(doc: DocumentoUi, conteudo: String) {
    // Folga de 2s: o primeiro save carimba createdAt e updatedAt quase juntos — só conta como
    // "editado" quando houve um segundo gesto depois de criado.
    val editado = doc.updatedAtMs > doc.createdAtMs + 2000
    val autor = if (editado) doc.updatedBy else doc.origem
    val verbo = if (editado) "editado" else "criado"
    val quem = if (autor == "user") "por você" else "pela Luna"
    val quando = tempoRelativo(if (editado) doc.updatedAtMs else doc.createdAtMs)
    val palavras = remember(conteudo) { contarPalavras(conteudo) }
    val leitura = ((palavras + 199) / 200).coerceAtLeast(1)

    val partes = buildList {
        add(listOf(verbo, quem, quando).filter { it.isNotBlank() }.joinToString(" "))
        if (palavras > 0) {
            add("$palavras ${if (palavras == 1) "palavra" else "palavras"}")
            add("$leitura min de leitura")
        }
    }
    Text(
        text = partes.joinToString("   ·   "),
        color = OrbitTokens.textLowN,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    )
}

private val localeBrDoc = java.util.Locale("pt", "BR")

private fun contarPalavras(texto: String): Int {
    val t = texto.trim()
    return if (t.isEmpty()) 0 else t.split(Regex("\\s+")).count { it.isNotBlank() }
}

/** "agora há pouco" · "há 12 min" · "há 3 h" · "ontem" · "há 4 dias" · "em 05/07/2026". */
private fun tempoRelativo(ms: Long): String {
    if (ms <= 0L) return ""
    val diff = System.currentTimeMillis() - ms
    if (diff < 0) return "agora há pouco"
    val min = diff / 60_000
    val hora = diff / 3_600_000
    val dia = diff / 86_400_000
    return when {
        min < 1 -> "agora há pouco"
        min < 60 -> "há $min min"
        hora < 24 -> "há $hora h"
        dia == 1L -> "ontem"
        dia < 7 -> "há $dia dias"
        else -> "em " + java.text.SimpleDateFormat("dd/MM/yyyy", localeBrDoc)
            .format(java.util.Date(ms))
    }
}

/**
 * Um item do menu ⋮ do leitor — linha própria (não o `DropdownMenuItem` padrão): ícone e rótulo
 * juntos, respiro medido, e a variante de perigo (apagar) em vermelho. Casa com o cartão do menu.
 */
@Composable
private fun ItemMenu(
    rotulo: String,
    icone: ImageVector,
    perigo: Boolean = false,
    onClick: () -> Unit,
) {
    val cor = if (perigo) OrbitTokens.danger else OrbitTokens.textHiN
    val corIcone = if (perigo) OrbitTokens.danger else OrbitTokens.textMidN
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icone, contentDescription = null, tint = corIcone, modifier = Modifier.size(18.dp))
        Text(rotulo, color = cor, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
    }
}

/** Fio-de-cabelo entre grupos do menu — separa o destrutivo do resto. */
@Composable
private fun DivisorMenu() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(1.dp)
            .background(OrbitTokens.graphiteHair),
    )
}

/** Botão «Concluir» — pílula em degradê (acende ao terminar a edição), conteúdo branco. */
@Composable
private fun BotaoConcluir(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
            .background(gradienteArtefato)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = OrbitTokens.onBluePastel,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Concluir",
            color = OrbitTokens.onBluePastel,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cânone — a "bíblia" do artefato: os fatos fixos que não podem se contradizer.
//
// O lado VISÍVEL do `anotar_canone` do core. A Luna já mantém esses fatos sozinha
// (e os relê sempre antes de mexer, mesmo num livro grande onde só vê o índice);
// aqui o Ethan os vê e corrige à mão. É metadado à parte do corpo — não vaza na
// exportação nem na contagem de palavras. Editor simples, um fato por linha.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CanoneArtefatoSheet(
    docId: String,
    canoneInicial: String,
    uid: String?,
    onDismiss: () -> Unit,
    onSalvo: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Re-semeado a cada abertura do painel (composição nova quando `canoneAberto` volta a ser true).
    var texto by remember(docId) { mutableStateOf(canoneInicial) }

    // Sair PERSISTE o que mudou (e só se mudou). Nada de botão descartar — igual ao editor do corpo.
    fun salvarESair() {
        val t = texto.trim()
        if (uid != null && t != canoneInicial.trim()) {
            scope.launch { runCatching { FirestoreDocumentos.atualizarCanone(uid, docId, t) } }
            onSalvo(t)
        }
        onDismiss()
    }

    // Dialog próprio pra subir por cima do leitor (mesmo motivo do histórico).
    Dialog(
        onDismissRequest = { salvarESair() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
    BackHandler { salvarESair() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.graphiteBg)
            .orbitEnter(),
    ) {
        AtmosferaArtefato()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Cabeçalho: voltar · identidade · concluir
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BotaoIconeChrome(icone = Icons.Rounded.ArrowBackIosNew, descricao = "Voltar") {
                    salvarESair()
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Cânone",
                        color = OrbitTokens.textHiN,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    )
                    Text(
                        text = "A bíblia deste artefato",
                        color = OrbitTokens.textMidN,
                        fontSize = 12.5.sp,
                    )
                }
                BotaoConcluir { salvarESair() }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OrbitTokens.graphiteHair.copy(alpha = 0.65f)),
            )

            // Corpo: nota do que é + editor
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 18.dp, bottom = 36.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(OrbitTokens.graphiteSurf)
                        .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = OrbitTokens.bluePastel,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(11.dp))
                    Text(
                        text = "Os fatos que não podem mudar de um trecho pro outro — nomes, idades, " +
                            "quem é quem. A Luna lê isto sempre antes de mexer no texto, mesmo num " +
                            "artefato grande. Um fato por linha.",
                        color = OrbitTokens.textMidN,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
                Spacer(Modifier.height(20.dp))
                BasicTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    textStyle = TextStyle(
                        color = OrbitTokens.textHiN,
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                    ),
                    cursorBrush = SolidColor(OrbitTokens.bluePastel),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    decorationBox = { inner ->
                        if (texto.isEmpty()) {
                            Text(
                                text = "Ex.:\nMarina — arquiteta, 30 anos, mora em Lisboa\n" +
                                    "Pedro é irmão da Marina",
                                color = OrbitTokens.textLowN,
                                fontSize = 16.sp,
                                lineHeight = 26.sp,
                            )
                        }
                        inner()
                    },
                )
            }
        }
    }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Histórico de versões — o "álbum de fotos" do artefato.
//
// Cada reescrita (a Luna numa revisão, ou o Ethan no editor) guarda um retrato do
// estado anterior em `documentos/{id}/versoes`. Esta tela lê esse álbum, deixa abrir
// uma foto pra ler, e restaurar (que também vira desfazível — o estado de agora é
// fotografado antes). Fetch sob demanda, não listener: ninguém fica de olho no álbum.
// ─────────────────────────────────────────────────────────────────────────────

/** A tela do histórico — lista de versões por cima do leitor. */
@Composable
private fun HistoricoArtefatoSheet(
    docId: String,
    uid: String?,
    // O texto ATUAL do artefato — o «depois» da versão mais recente, o outro lado do diff.
    conteudoAtual: String,
    onDismiss: () -> Unit,
    onRestaurar: (VersaoUi) -> Unit,
) {
    var versoes by remember(docId) { mutableStateOf<List<VersaoUi>?>(null) }
    var selecionada by remember(docId) { mutableStateOf<VersaoUi?>(null) }

    LaunchedEffect(docId, uid) {
        versoes = if (uid == null) {
            emptyList()
        } else {
            runCatching { FirestoreDocumentos.lerVersoes(uid, docId) }.getOrDefault(emptyList())
        }
    }

    // Envolto num Dialog próprio pra subir POR CIMA do leitor (que também é um Dialog).
    // Como Box solto renderizava ATRÁS da janela do leitor → o histórico/diff ficava invisível.
    Dialog(
        onDismissRequest = { if (selecionada != null) selecionada = null else onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
    // Voltar: fecha a pré-visualização antes; depois a tela.
    BackHandler {
        if (selecionada != null) selecionada = null else onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.graphiteBg)
            .orbitEnter(),
    ) {
        AtmosferaArtefato()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BotaoIconeChrome(icone = Icons.Rounded.ArrowBackIosNew, descricao = "Voltar") {
                    onDismiss()
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Histórico",
                        color = OrbitTokens.textHiN,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    )
                    val sub = when (val v = versoes) {
                        null -> "carregando…"
                        else -> if (v.isEmpty()) {
                            "nenhuma versão ainda"
                        } else {
                            "${v.size} ${if (v.size == 1) "versão guardada" else "versões guardadas"}"
                        }
                    }
                    Text(sub, color = OrbitTokens.textMidN, fontSize = 12.5.sp)
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OrbitTokens.graphiteHair.copy(alpha = 0.65f)),
            )

            when (val lista = versoes) {
                null -> Unit // silêncio breve durante o fetch
                else -> if (lista.isEmpty()) {
                    HistoricoVazio()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = OrbitMetrics.pagePadding,
                            end = OrbitMetrics.pagePadding,
                            top = 16.dp,
                            bottom = 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(lista, key = { _, v -> v.id }) { index, v ->
                            // O «depois» desta versão: a versão mais nova (index-1), ou o texto
                            // atual pra a mais recente (index 0).
                            val sucessor = if (index == 0) conteudoAtual else lista[index - 1].conteudo
                            CartaoVersao(
                                versao = v,
                                sucessor = sucessor,
                                onClick = { selecionada = v },
                            )
                        }
                    }
                }
            }
        }
    }

    selecionada?.let { v ->
        val lista = versoes.orEmpty()
        val idx = lista.indexOfFirst { it.id == v.id }
        val sucessor = when {
            idx <= 0 -> conteudoAtual
            else -> lista[idx - 1].conteudo
        }
        PreviewVersao(
            versao = v,
            sucessor = sucessor,
            onVoltar = { selecionada = null },
            onRestaurar = { onRestaurar(v) },
        )
    }
    }
}

/** Estado vazio do histórico — explica de onde as versões vêm, em vez de só um vão em branco. */
@Composable
private fun HistoricoVazio() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OrbitMetrics.pagePadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrbitTokens.graphiteRaised)
                    .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.History,
                    contentDescription = null,
                    tint = OrbitTokens.textMidN,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Ainda não há versões anteriores",
                color = OrbitTokens.textHiN,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Toda vez que este artefato for reescrito — pela Luna ou por você — uma foto do " +
                    "texto anterior fica guardada aqui pra você poder voltar.",
                color = OrbitTokens.textMidN,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

/**
 * Um cartão de versão na lista — quem mexeu, quando, e O QUE MUDOU (resumo «+N −M» + um gist do
 * trecho), em vez do texto inteiro. O «sucessor» é o estado que veio depois desta versão; o diff
 * entre os dois é a edição que esta foto registrou.
 */
@Composable
private fun CartaoVersao(versao: VersaoUi, sucessor: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val quem = if (versao.autor == "user") "Você" else "Luna"
    val quando = tempoRelativo(versao.savedAtMs)
    val diff = remember(versao.conteudo, sucessor) { diffArtefato(versao.conteudo, sucessor) }
    val gist = remember(diff) { trechoMudanca(diff) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .orbitPressable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = quem,
                color = OrbitTokens.bluePastel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (quando.isNotBlank()) {
                Text(
                    text = "  ·  $quando",
                    color = OrbitTokens.textLowN,
                    fontSize = 12.5.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            ResumoMudanca(diff, tamanho = 12.5.sp)
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = OrbitTokens.textLowN,
                modifier = Modifier.size(18.dp),
            )
        }
        if (gist.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = gist,
                color = OrbitTokens.textMidN,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Lê uma versão antiga — tela cheia com DUAS vistas: «Alterações» (o diff pro estado seguinte, o
 * padrão — mostra o que esta edição mudou) e «Texto completo» (a foto inteira). Barra pra restaurar
 * (com confirmação) fixa embaixo.
 */
@Composable
private fun PreviewVersao(
    versao: VersaoUi,
    sucessor: String,
    onVoltar: () -> Unit,
    onRestaurar: () -> Unit,
) {
    var confirmando by remember { mutableStateOf(false) }
    var modoDiff by remember(versao.id) { mutableStateOf(true) }
    val diff = remember(versao.conteudo, sucessor) { diffArtefato(versao.conteudo, sucessor) }
    BackHandler { onVoltar() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitTokens.graphiteBg)
            .orbitEnter(),
    ) {
        AtmosferaArtefato()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BotaoIconeChrome(icone = Icons.Rounded.ArrowBackIosNew, descricao = "Voltar") {
                    onVoltar()
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = versao.titulo,
                        color = OrbitTokens.textHiN,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val quem = if (versao.autor == "user") "por você" else "pela Luna"
                    val quando = tempoRelativo(versao.savedAtMs)
                    Text(
                        text = listOf("versão", quem, quando).filter { it.isNotBlank() }.joinToString(" "),
                        color = OrbitTokens.textMidN,
                        fontSize = 12.5.sp,
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OrbitTokens.graphiteHair.copy(alpha = 0.65f)),
            )
            // Seletor de vista + o resumo «+N −M» à direita quando em «Alterações».
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SeletorVista(
                    modoDiff = modoDiff,
                    onSelecionar = { modoDiff = it },
                )
                Spacer(Modifier.weight(1f))
                if (modoDiff) ResumoMudanca(diff, tamanho = 13.sp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 16.dp, bottom = 28.dp),
            ) {
                if (modoDiff) {
                    DiffTexto(diff)
                } else {
                    LunaMarkdown(
                        content = versao.conteudo,
                        variante = LunaMarkdownVariante.Documento,
                    )
                }
            }
            // Barra inferior fixa — a única ação da tela.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 8.dp, bottom = 12.dp),
            ) {
                BotaoRestaurar { confirmando = true }
            }
        }
    }

    if (confirmando) {
        AlertDialog(
            onDismissRequest = { confirmando = false },
            containerColor = OrbitTokens.graphiteRaised,
            title = { Text("Restaurar esta versão?", color = OrbitTokens.textHiN) },
            text = {
                Text(
                    "O texto de agora vira uma versão no histórico (nada se perde) e o artefato " +
                        "volta a como estava nesta foto.",
                    color = OrbitTokens.textMidN,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmando = false
                    onRestaurar()
                }) { Text("Restaurar", color = OrbitTokens.bluePastel) }
            },
            dismissButton = {
                TextButton(onClick = { confirmando = false }) {
                    Text("Cancelar", color = OrbitTokens.textMidN)
                }
            },
        )
    }
}

/** Seletor de vista da versão: «Alterações» (diff) ou «Texto completo» (a foto inteira). */
@Composable
private fun SeletorVista(modoDiff: Boolean, onSelecionar: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusPill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        SegmentoVista("Alterações", ativo = modoDiff) { onSelecionar(true) }
        SegmentoVista("Texto completo", ativo = !modoDiff) { onSelecionar(false) }
    }
}

@Composable
private fun SegmentoVista(rotulo: String, ativo: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusPill)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(if (ativo) Modifier.background(gradienteArtefato) else Modifier)
            .orbitPressable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            rotulo,
            color = if (ativo) OrbitTokens.onBluePastel else OrbitTokens.textMidN,
            fontSize = 12.5.sp,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Botão «Restaurar esta versão» — pílula larga em degradê, conteúdo branco (a ação da tela). */
@Composable
private fun BotaoRestaurar(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
            .background(gradienteArtefato)
            .orbitPressable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Restore,
            contentDescription = null,
            tint = OrbitTokens.onBluePastel,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Restaurar esta versão",
            color = OrbitTokens.onBluePastel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Diff de versões — «o que mudou», não o artefato inteiro.
//
// Cada versão é uma foto do estado ANTES de uma reescrita. O que mudou é o diff
// entre ela e o que veio DEPOIS (a versão mais nova, ou o texto atual pra a mais
// recente). Mostramos tracked-changes PALAVRA a palavra: se a Luna trocou uma
// frase num parágrafo, aparece só aquela frase riscada+nova — não o parágrafo
// duplicado. Aparamos prefixo/sufixo iguais (viram «··· N linhas iguais»), então
// num livro não se rola o capítulo todo pra ver uma vírgula.
// ─────────────────────────────────────────────────────────────────────────────

private sealed interface DiffToken {
    val texto: String
    data class Igual(override val texto: String) : DiffToken
    data class Add(override val texto: String) : DiffToken
    data class Rem(override val texto: String) : DiffToken
}

private data class DiffArtefato(
    val linhasIguaisAcima: Int,
    val linhasIguaisAbaixo: Int,
    val tokens: List<DiffToken>,
    /** Palavras (não espaços) que entraram / saíram — o resumo do cartão. */
    val adicionadas: Int,
    val removidas: Int,
)

/** Quebra em tokens preservando os espaços/quebras (cada espaço vira um token próprio). */
private fun tokenizarDiff(texto: String): List<String> =
    Regex("\\s+|\\S+").findAll(texto).map { it.value }.toList()

private fun ehPalavra(token: String): Boolean = token.isNotBlank()

/**
 * Diff entre dois estados do artefato. Apara as linhas iguais no topo/rodapé (barato, e é o que
 * blinda o caso de texto grande com mudança cirúrgica), e roda um LCS de tokens só no miolo que
 * sobrou. Guarda de custo: se o miolo for grande demais, cai num bloco «tudo isto saiu / tudo isto
 * entrou» em vez do LCS palavra a palavra.
 */
private fun diffArtefato(antigo: String, novo: String): DiffArtefato {
    val a = antigo.split("\n")
    val b = novo.split("\n")

    var pre = 0
    while (pre < a.size && pre < b.size && a[pre] == b[pre]) pre++
    var fa = a.size
    var fb = b.size
    while (fa > pre && fb > pre && a[fa - 1] == b[fb - 1]) { fa--; fb-- }

    val iguaisAcima = pre
    val iguaisAbaixo = a.size - fa
    val mioloA = a.subList(pre, fa).joinToString("\n")
    val mioloB = b.subList(pre, fb).joinToString("\n")

    if (mioloA.isEmpty() && mioloB.isEmpty()) {
        return DiffArtefato(iguaisAcima, iguaisAbaixo, emptyList(), 0, 0)
    }

    val ta = tokenizarDiff(mioloA)
    val tb = tokenizarDiff(mioloB)

    // Guarda de custo: LCS é O(n·m). Pra reescrita grande, mostra o miolo em bloco.
    if (ta.size.toLong() * tb.size.toLong() > 400_000L) {
        val tokens = buildList {
            if (mioloA.isNotEmpty()) add(DiffToken.Rem(mioloA))
            if (mioloA.isNotEmpty() && mioloB.isNotEmpty()) add(DiffToken.Igual("\n"))
            if (mioloB.isNotEmpty()) add(DiffToken.Add(mioloB))
        }
        return DiffArtefato(iguaisAcima, iguaisAbaixo, tokens, tb.count { ehPalavra(it) }, ta.count { ehPalavra(it) })
    }

    val n = ta.size
    val m = tb.size
    val lcs = Array(n + 1) { IntArray(m + 1) }
    for (i in n - 1 downTo 0) {
        for (j in m - 1 downTo 0) {
            lcs[i][j] = if (ta[i] == tb[j]) lcs[i + 1][j + 1] + 1
            else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
    }

    val tokens = mutableListOf<DiffToken>()
    var add = 0
    var rem = 0
    var i = 0
    var j = 0
    while (i < n && j < m) {
        when {
            ta[i] == tb[j] -> { tokens.add(DiffToken.Igual(ta[i])); i++; j++ }
            lcs[i + 1][j] >= lcs[i][j + 1] -> { tokens.add(DiffToken.Rem(ta[i])); if (ehPalavra(ta[i])) rem++; i++ }
            else -> { tokens.add(DiffToken.Add(tb[j])); if (ehPalavra(tb[j])) add++; j++ }
        }
    }
    while (i < n) { tokens.add(DiffToken.Rem(ta[i])); if (ehPalavra(ta[i])) rem++; i++ }
    while (j < m) { tokens.add(DiffToken.Add(tb[j])); if (ehPalavra(tb[j])) add++; j++ }

    return DiffArtefato(iguaisAcima, iguaisAbaixo, tokens, add, rem)
}

/** Um gist de uma linha do que mudou — o texto que ENTROU (senão o que saiu), pro cartão. */
private fun trechoMudanca(diff: DiffArtefato): String {
    val add = diff.tokens.filterIsInstance<DiffToken.Add>().joinToString("") { it.texto }.trim()
    val base = add.ifBlank {
        diff.tokens.filterIsInstance<DiffToken.Rem>().joinToString("") { it.texto }.trim()
    }
    return base.replace(Regex("\\s+"), " ").trim()
}

/** «+N −M» — o resumo compacto do que mudou (palavras que entraram / saíram). */
@Composable
private fun ResumoMudanca(diff: DiffArtefato, tamanho: TextUnit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (diff.adicionadas == 0 && diff.removidas == 0) {
            Text("sem mudança de texto", color = OrbitTokens.textLowN, fontSize = tamanho)
        } else {
            if (diff.adicionadas > 0) {
                Text(
                    "+${diff.adicionadas}",
                    color = OrbitTokens.online,
                    fontSize = tamanho,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (diff.removidas > 0) {
                Text(
                    "−${diff.removidas}",
                    color = OrbitTokens.danger,
                    fontSize = tamanho,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Desenha o diff como tracked-changes: verde entra, vermelho riscado sai, o resto neutro. */
@Composable
private fun DiffTexto(diff: DiffArtefato, modifier: Modifier = Modifier) {
    val corAdd = OrbitTokens.online
    val corRem = OrbitTokens.danger
    val texto = remember(diff) {
        buildAnnotatedString {
            for (tk in diff.tokens) {
                when (tk) {
                    is DiffToken.Igual ->
                        withStyle(SpanStyle(color = OrbitTokens.textHiN)) { append(tk.texto) }
                    is DiffToken.Add ->
                        withStyle(
                            SpanStyle(color = corAdd, background = corAdd.copy(alpha = 0.16f)),
                        ) { append(tk.texto) }
                    is DiffToken.Rem ->
                        withStyle(
                            SpanStyle(
                                color = corRem,
                                background = corRem.copy(alpha = 0.12f),
                                textDecoration = TextDecoration.LineThrough,
                            ),
                        ) { append(tk.texto) }
                }
            }
        }
    }
    Column(modifier) {
        if (diff.linhasIguaisAcima > 0) {
            GapIguais(diff.linhasIguaisAcima)
            Spacer(Modifier.height(12.dp))
        }
        if (diff.tokens.isEmpty()) {
            Text(
                "Sem alterações de texto nesta versão.",
                color = OrbitTokens.textMidN,
                fontSize = 14.sp,
            )
        } else {
            Text(text = texto, fontSize = 15.sp, lineHeight = 24.sp)
        }
        if (diff.linhasIguaisAbaixo > 0) {
            Spacer(Modifier.height(12.dp))
            GapIguais(diff.linhasIguaisAbaixo)
        }
    }
}

/** O marcador de trecho igual aparado — «··· N linhas iguais». */
@Composable
private fun GapIguais(linhas: Int) {
    Text(
        text = "···  $linhas ${if (linhas == 1) "linha igual" else "linhas iguais"}",
        color = OrbitTokens.textLowN,
        fontSize = 12.sp,
    )
}

/**
 * Barra flutuante do modo «marcar trecho»: mostra o que foi selecionado e o envia como referência
 * pra o composer. Fica esmaecida até haver uma seleção — o botão só dispara com trecho de verdade.
 */
@Composable
private fun BarraReferenciarTrecho(
    trecho: String,
    onReferenciar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val temTrecho = trecho.isNotBlank()
    val resumo = remember(trecho) { trecho.trim().replace(Regex("\\s+"), " ").take(120) }
    Column(
        modifier
            .fillMaxWidth()
            .background(OrbitTokens.graphiteRaised),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OrbitTokens.graphiteHair.copy(alpha = 0.65f)),
        )
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(top = 14.dp, bottom = 16.dp),
        ) {
            Text(
                text = if (temTrecho) "“$resumo”" else "Arraste sobre o texto pra marcar um trecho.",
                color = if (temTrecho) OrbitTokens.textHiN else OrbitTokens.textLowN,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (temTrecho) Modifier.background(gradienteArtefato)
                        else Modifier.background(OrbitTokens.graphiteSurf),
                    )
                    .orbitPressable { if (temTrecho) onReferenciar() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Referenciar na conversa",
                    color = if (temTrecho) OrbitTokens.onBluePastel else OrbitTokens.textLowN,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Verniz "live preview" (estilo Obsidian) — v1: títulos e citações.
//
// O texto continua sendo Markdown puro; o verniz é só APARÊNCIA. Fora da linha do
// cursor ele ESCONDE o marcador de bloco (`## `, `> `) e desenha a linha já como
// título/citação. Na linha ativa, os marcadores voltam esmaecidos pra editar.
//
// Escopo de propósito: só marcadores no INÍCIO da linha (remoção-só, um por linha
// → o mapa de offset fica simples e robusto). Ênfase inline (`**`, `*`), listas e
// checkboxes ficam pro próximo degrau, se o toque agradar.
// ─────────────────────────────────────────────────────────────────────────────

private fun estiloHeading(nivel: Int): SpanStyle = when (nivel) {
    1 -> SpanStyle(
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold,
        color = OrbitTokens.textHiN,
        letterSpacing = (-0.4).sp,
    )
    2 -> SpanStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = OrbitTokens.textHiN,
        letterSpacing = (-0.3).sp,
    )
    else -> SpanStyle(
        fontSize = 17.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = OrbitTokens.textHiN,
    )
}

private val marcadorHeading = Regex("^(#{1,6})[ \\t]+")
private val marcadorCitacao = Regex("^>[ \\t]?")

/** Constrói o verniz sabendo a seleção atual (pra revelar a linha do cursor). */
private fun vernizMarkdown(selInicio: Int, selFim: Int): VisualTransformation =
    VisualTransformation { texto ->
        val bruto = texto.text
        val n = bruto.length
        val construtor = AnnotatedString.Builder()
        val removidos = ArrayList<Pair<Int, Int>>() // [inicio, fim) em coords originais
        val corMarcador = SpanStyle(color = OrbitTokens.textLowN)
        val estiloCitacao = SpanStyle(color = OrbitTokens.textMidN, fontStyle = FontStyle.Italic)

        var i = 0
        while (i <= n) {
            val fimLinha = bruto.indexOf('\n', i).let { if (it == -1) n else it }
            val linha = bruto.substring(i, fimLinha)
            // A seleção "toca" a linha? (linha ocupa [i, fimLinha]; o cursor pode pousar no fim)
            val ativa = selFim >= i && selInicio <= fimLinha

            val mh = marcadorHeading.find(linha)
            val mq = if (mh == null) marcadorCitacao.find(linha) else null
            when {
                mh != null -> {
                    val nivel = mh.groupValues[1].length
                    val marcador = mh.value
                    if (ativa) {
                        construtor.pushStyle(corMarcador)
                        construtor.append(marcador)
                        construtor.pop()
                    } else {
                        removidos.add(i to (i + marcador.length))
                    }
                    construtor.pushStyle(estiloHeading(nivel))
                    construtor.append(linha.substring(marcador.length))
                    construtor.pop()
                }
                mq != null -> {
                    val marcador = mq.value
                    if (ativa) {
                        construtor.pushStyle(corMarcador)
                        construtor.append(marcador)
                        construtor.pop()
                    } else {
                        removidos.add(i to (i + marcador.length))
                    }
                    construtor.pushStyle(estiloCitacao)
                    construtor.append(linha.substring(marcador.length))
                    construtor.pop()
                }
                else -> construtor.append(linha)
            }

            if (fimLinha < n) construtor.append('\n')
            i = fimLinha + 1
        }

        TransformedText(construtor.toAnnotatedString(), MapaRemocao(n, removidos))
    }

/**
 * Mapa de offset pra quando o verniz REMOVE trechos (só marcadores de início de linha). Os
 * `removidos` são intervalos `[inicio, fim)` disjuntos e ordenados; o texto exibido é o original
 * menos esses trechos. Traduz posição de cursor entre os dois mundos — o pulo-do-gato pra não
 * estourar índice quando o `##` some da tela.
 */
private class MapaRemocao(
    private val tamOriginal: Int,
    private val removidos: List<Pair<Int, Int>>,
) : OffsetMapping {
    private val totalRemovido = removidos.sumOf { it.second - it.first }

    override fun originalToTransformed(offset: Int): Int {
        var acc = 0
        for ((s, e) in removidos) {
            when {
                e <= offset -> acc += (e - s)
                s < offset -> { acc += (offset - s); break }
                else -> break
            }
        }
        return (offset - acc).coerceIn(0, tamOriginal - totalRemovido)
    }

    override fun transformedToOriginal(offset: Int): Int {
        var acc = 0
        for ((s, e) in removidos) {
            val inicioTrans = s - acc
            if (offset <= inicioTrans) return (offset + acc).coerceIn(0, tamOriginal)
            acc += (e - s)
        }
        return (offset + acc).coerceIn(0, tamOriginal)
    }
}
