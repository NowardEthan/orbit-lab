package com.ethan.orbitlab.ui.artefato

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.artefato.BlocoArtefato
import com.ethan.orbitlab.data.artefato.PropsBlocoArtefato
import com.ethan.orbitlab.data.artefato.SaborCallout
import com.ethan.orbitlab.data.artefato.TipoBlocoArtefato
import com.ethan.orbitlab.data.artefato.novoIdBloco
import com.ethan.orbitlab.ui.chat.corDoSabor
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType

/**
 * Editor Notion-like: uma linha por bloco, Enter continua lista/todo ou cria parágrafo,
 * Backspace no vazio funde/apaga, `/` abre o painel de tipos (ícones + preview).
 */
@Composable
fun ArtefatoEditor(
    blocos: List<BlocoArtefato>,
    onBlocosChange: (List<BlocoArtefato>) -> Unit,
    focusBlocoId: String? = null,
    onFocusConsumed: () -> Unit = {},
    onPedirFoco: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    emptyHint: String = "Escreva, ou digite / pra escolher um bloco…",
) {
    val lista = if (blocos.isEmpty()) {
        listOf(BlocoArtefato(id = novoIdBloco(), type = TipoBlocoArtefato.paragraph, text = ""))
    } else {
        blocos
    }

    LaunchedEffect(blocos.isEmpty()) {
        if (blocos.isEmpty()) onBlocosChange(lista)
    }

    val paginaVazia =
        lista.size == 1 &&
            lista[0].text.isEmpty() &&
            lista[0].type == TipoBlocoArtefato.paragraph

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        lista.forEachIndexed { index, bloco ->
            BlocoLinha(
                bloco = bloco,
                index = index,
                total = lista.size,
                pedirFoco = focusBlocoId == bloco.id,
                onFocoPedidoConsumido = onFocusConsumed,
                emptyHintPrimeiro = if (paginaVazia) emptyHint else null,
                onChange = { novo ->
                    onBlocosChange(lista.toMutableList().also { it[index] = novo })
                },
                onEnter = {
                    val tipoContinua = when (bloco.type) {
                        TipoBlocoArtefato.bullet,
                        TipoBlocoArtefato.numbered,
                        TipoBlocoArtefato.todo,
                        -> bloco.type
                        else -> TipoBlocoArtefato.paragraph
                    }
                    val propsContinua = when (tipoContinua) {
                        TipoBlocoArtefato.todo -> PropsBlocoArtefato(checked = false)
                        else -> null
                    }
                    val novo = BlocoArtefato(
                        id = novoIdBloco(),
                        type = tipoContinua,
                        text = "",
                        props = propsContinua,
                    )
                    val next = lista.toMutableList()
                    next.add(index + 1, novo)
                    onBlocosChange(next)
                    onPedirFoco(novo.id)
                },
                onBackspaceVazio = {
                    if (lista.size <= 1) return@BlocoLinha
                    val next = lista.toMutableList()
                    next.removeAt(index)
                    val focoId = next[(index - 1).coerceAtLeast(0)].id
                    onBlocosChange(next)
                    onPedirFoco(focoId)
                },
                onSlashTipo = { tipo, props ->
                    onBlocosChange(
                        lista.toMutableList().also {
                            it[index] = bloco.copy(
                                type = tipo,
                                text = "",
                                props = props,
                            )
                        },
                    )
                },
                onMover = { dir ->
                    val j = index + dir
                    if (j !in lista.indices) return@BlocoLinha
                    val next = lista.toMutableList()
                    val tmp = next[index]
                    next[index] = next[j]
                    next[j] = tmp
                    onBlocosChange(next)
                },
                onApagar = {
                    if (lista.size <= 1) {
                        onBlocosChange(
                            listOf(
                                BlocoArtefato(
                                    id = lista[0].id,
                                    type = TipoBlocoArtefato.paragraph,
                                    text = "",
                                ),
                            ),
                        )
                    } else {
                        val next = lista.toMutableList()
                        next.removeAt(index)
                        val focoId = next[(index - 1).coerceAtLeast(0)].id
                        onBlocosChange(next)
                        onPedirFoco(focoId)
                    }
                },
            )
        }
    }
}

@Composable
private fun BlocoLinha(
    bloco: BlocoArtefato,
    index: Int,
    total: Int,
    pedirFoco: Boolean,
    onFocoPedidoConsumido: () -> Unit,
    emptyHintPrimeiro: String?,
    onChange: (BlocoArtefato) -> Unit,
    onEnter: () -> Unit,
    onBackspaceVazio: () -> Unit,
    onSlashTipo: (TipoBlocoArtefato, PropsBlocoArtefato?) -> Unit,
    onMover: (Int) -> Unit,
    onApagar: () -> Unit,
) {
    val focusRequester = remember(bloco.id) { FocusRequester() }
    var focused by remember(bloco.id) { mutableStateOf(false) }
    var slashAberto by remember(bloco.id) { mutableStateOf(false) }
    var menuPainel by remember(bloco.id) { mutableStateOf(false) }
    var valor by remember(bloco.id, bloco.text) {
        mutableStateOf(TextFieldValue(bloco.text, TextRange(bloco.text.length)))
    }
    // Rótulo do divisor (só usado no ramo divider) — estado local pra não pular o cursor.
    var rotuloDivisor by remember(bloco.id) { mutableStateOf(bloco.props?.label ?: "") }

    LaunchedEffect(bloco.text) {
        if (valor.text != bloco.text && !focused) {
            valor = TextFieldValue(bloco.text, TextRange(bloco.text.length))
        }
    }

    LaunchedEffect(pedirFoco) {
        if (pedirFoco) {
            runCatching { focusRequester.requestFocus() }
            onFocoPedidoConsumido()
        }
    }

    val shape = RoundedCornerShape(8.dp)
    val fundoTipo = when (bloco.type) {
        TipoBlocoArtefato.callout ->
            corDoSabor(SaborCallout.from(bloco.props?.callout)).copy(alpha = 0.09f)
        TipoBlocoArtefato.quote -> OrbitTokens.graphiteSurf.copy(alpha = 0.4f)
        TipoBlocoArtefato.code -> OrbitTokens.graphiteRaised.copy(alpha = 0.55f)
        else -> Color.Transparent
    }
    val fundoFoco = if (focused) OrbitTokens.graphiteSurf.copy(alpha = 0.45f) else fundoTipo
    val bordaFoco = if (focused) {
        Modifier.border(1.dp, OrbitTokens.graphiteHair, shape)
    } else {
        Modifier
    }

    val querySlash = if (valor.text.startsWith("/")) valor.text.removePrefix("/") else ""
    val opcoesSlash = lembrarOpcoesFiltradas(querySlash)
    val painelVisivel = (slashAberto && focused) || menuPainel

    Column(modifier = Modifier.fillMaxWidth()) {
        if (bloco.type == TipoBlocoArtefato.divider) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .then(bordaFoco)
                    .background(fundoFoco)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .clickable {
                        focused = true
                        menuPainel = true
                        slashAberto = false
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(OrbitTokens.graphiteHair),
                ) {}
                BasicTextField(
                    value = rotuloDivisor,
                    onValueChange = { novo ->
                        val limpo = novo.replace("\n", "")
                        rotuloDivisor = limpo
                        onChange(
                            bloco.copy(
                                props = (bloco.props ?: PropsBlocoArtefato())
                                    .copy(label = limpo.ifBlank { null }),
                            ),
                        )
                    },
                    textStyle = TextStyle(
                        color = OrbitTokens.textLowN,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(OrbitTokens.bluePastel),
                    modifier = Modifier
                        .widthIn(min = 56.dp)
                        .padding(horizontal = 10.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (rotuloDivisor.isEmpty()) {
                                Text("rótulo", color = OrbitTokens.textLowN.copy(alpha = 0.6f), fontSize = 11.sp)
                            }
                            inner()
                        }
                    },
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(OrbitTokens.graphiteHair),
                ) {}
                BotaoHandleBloco(
                    ativo = menuPainel,
                    onClick = {
                        menuPainel = !menuPainel
                        slashAberto = false
                    },
                )
            }
        } else {
            val style = estiloDoBloco(bloco)
            val prefix = prefixoVisual(bloco)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .then(bordaFoco)
                    .background(fundoFoco)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (bloco.type == TipoBlocoArtefato.todo) {
                    Icon(
                        imageVector = if (bloco.props?.checked == true) {
                            Icons.Rounded.CheckBox
                        } else {
                            Icons.Rounded.CheckBoxOutlineBlank
                        },
                        contentDescription = if (bloco.props?.checked == true) "Desmarcar" else "Marcar",
                        tint = OrbitTokens.bluePastel,
                        modifier = Modifier
                            .padding(top = 4.dp, end = 8.dp)
                            .size(22.dp)
                            .clickable {
                                onChange(
                                    bloco.copy(
                                        props = (bloco.props ?: PropsBlocoArtefato()).copy(
                                            checked = !(bloco.props?.checked ?: false),
                                        ),
                                    ),
                                )
                            },
                    )
                } else if (prefix != null) {
                    Text(
                        text = prefix,
                        color = OrbitTokens.textLowN,
                        fontSize = style.fontSize,
                        fontWeight = style.fontWeight,
                        fontFamily = style.fontFamily,
                        modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = valor,
                        onValueChange = { next ->
                            valor = next
                            val t = next.text
                            val abreSlash = t.startsWith("/") && !t.contains('\n')
                            slashAberto = abreSlash
                            if (abreSlash) menuPainel = false
                            onChange(bloco.copy(text = t))
                        },
                        textStyle = style,
                        cursorBrush = SolidColor(OrbitTokens.bluePastel),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        keyboardActions = KeyboardActions(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 28.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { st ->
                                focused = st.isFocused
                                if (!st.isFocused) {
                                    // Delay implícito: o clique no painel roda antes do dismiss
                                    // se o painel estiver na mesma árvore — fechamos só o slash
                                    // quando o texto deixa de ser comando.
                                    if (!valor.text.startsWith("/")) slashAberto = false
                                }
                            }
                            .onPreviewKeyEvent { e ->
                                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when {
                                    e.key == Key.Enter && !e.nativeKeyEvent.isShiftPressed -> {
                                        if (slashAberto && opcoesSlash.isNotEmpty()) {
                                            val op = opcoesSlash.first()
                                            slashAberto = false
                                            onSlashTipo(op.tipo, op.props)
                                            true
                                        } else {
                                            onEnter()
                                            true
                                        }
                                    }
                                    e.key == Key.Backspace && valor.text.isEmpty() -> {
                                        onBackspaceVazio()
                                        true
                                    }
                                    else -> false
                                }
                            },
                        decorationBox = { inner ->
                            if (valor.text.isEmpty() && focused) {
                                Text(
                                    text = when {
                                        emptyHintPrimeiro != null -> emptyHintPrimeiro
                                        bloco.type == TipoBlocoArtefato.heading -> "Título"
                                        bloco.type == TipoBlocoArtefato.bullet ||
                                            bloco.type == TipoBlocoArtefato.numbered -> "Item"
                                        bloco.type == TipoBlocoArtefato.todo -> "Tarefa"
                                        bloco.type == TipoBlocoArtefato.quote -> "Citação"
                                        bloco.type == TipoBlocoArtefato.callout -> "Destaque"
                                        bloco.type == TipoBlocoArtefato.code -> "código…"
                                        else -> "Escreva, ou /"
                                    },
                                    color = OrbitTokens.textLowN,
                                    fontSize = style.fontSize,
                                    fontFamily = style.fontFamily,
                                    fontWeight = FontWeight.Normal,
                                )
                            }
                            inner()
                        },
                    )
                }

                if (focused || menuPainel) {
                    BotaoHandleBloco(
                        ativo = menuPainel,
                        onClick = {
                            menuPainel = !menuPainel
                            slashAberto = false
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = painelVisivel,
            enter = fadeIn() + slideInVertically { -8 },
            exit = fadeOut() + slideOutVertically { -8 },
        ) {
            PainelBlocosNotion(
                opcoes = if (slashAberto) opcoesSlash else slashOpcoes(),
                query = if (slashAberto) querySlash else "",
                mostrarAcoes = menuPainel && !slashAberto,
                podeSubir = index > 0,
                podeDescer = index < total - 1,
                onMover = { dir ->
                    menuPainel = false
                    onMover(dir)
                },
                onApagar = {
                    menuPainel = false
                    onApagar()
                },
                onEscolher = { op ->
                    slashAberto = false
                    menuPainel = false
                    onSlashTipo(op.tipo, op.props)
                },
                modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun BotaoHandleBloco(
    ativo: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Icon(
        imageVector = Icons.Rounded.DragHandle,
        contentDescription = "Opções do bloco",
        tint = if (ativo) OrbitTokens.bluePastel else OrbitTokens.textLowN,
        modifier = Modifier
            .padding(start = 4.dp)
            .size(36.dp)
            .clip(shape)
            .background(
                if (ativo) OrbitTokens.bluePastel.copy(alpha = 0.12f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
    )
}

/**
 * Painel estilo Notion: ações rápidas em chips + grade de blocos com ícone e preview.
 */
@Composable
private fun PainelBlocosNotion(
    opcoes: List<SlashOpcao>,
    query: String,
    mostrarAcoes: Boolean,
    podeSubir: Boolean,
    podeDescer: Boolean,
    onMover: (Int) -> Unit,
    onApagar: () -> Unit,
    onEscolher: (SlashOpcao) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.35f))
            .clip(shape)
            .background(OrbitTokens.graphiteRaised)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .padding(vertical = 10.dp),
    ) {
        if (mostrarAcoes) {
            Text(
                "Ações rápidas",
                color = OrbitTokens.textLowN,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (podeSubir) {
                    ChipAcao(
                        icone = Icons.Rounded.KeyboardArrowUp,
                        label = "Subir",
                        onClick = { onMover(-1) },
                    )
                }
                if (podeDescer) {
                    ChipAcao(
                        icone = Icons.Rounded.KeyboardArrowDown,
                        label = "Descer",
                        onClick = { onMover(1) },
                    )
                }
                ChipAcao(
                    icone = Icons.Rounded.DeleteOutline,
                    label = "Apagar",
                    perigo = true,
                    onClick = onApagar,
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 4.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(1.dp)
                    .background(OrbitTokens.graphiteHair),
            ) {}
        }

        Text(
            if (query.isNotBlank()) "Resultados" else "Blocos",
            color = OrbitTokens.textLowN,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )

        if (opcoes.isEmpty()) {
            Text(
                "Nada com «${query.trim()}»",
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                opcoes.forEach { op ->
                    LinhaOpcaoBloco(
                        opcao = op,
                        onClick = { onEscolher(op) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipAcao(
    icone: ImageVector,
    label: String,
    onClick: () -> Unit,
    perigo: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    val tint = if (perigo) OrbitTokens.danger else OrbitTokens.textHiN
    Row(
        modifier = Modifier
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LinhaOpcaoBloco(
    opcao: SlashOpcao,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(opcao.corFundo)
                .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = opcao.icone,
                contentDescription = null,
                tint = opcao.corIcone,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                opcao.titulo,
                color = OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                opcao.subtitulo,
                color = OrbitTokens.textLowN,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        MiniPreviewBloco(opcao)
    }
}

/** Amostrinha visual do bloco — o “cheiro” Notion à direita. */
@Composable
private fun MiniPreviewBloco(opcao: SlashOpcao) {
    val boxShape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .width(72.dp)
            .height(36.dp)
            .clip(boxShape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, boxShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        when (opcao.tipo) {
            TipoBlocoArtefato.heading -> {
                val nivel = opcao.props?.level ?: 1
                Text(
                    "Aa",
                    color = OrbitTokens.textHiN,
                    fontSize = when (nivel) {
                        1 -> 18.sp
                        2 -> 15.sp
                        else -> 13.sp
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = OrbitType.display,
                )
            }
            TipoBlocoArtefato.bullet -> Text(
                "•  •  •",
                color = OrbitTokens.textMidN,
                fontSize = 12.sp,
            )
            TipoBlocoArtefato.numbered -> Text(
                "1. 2.",
                color = OrbitTokens.textMidN,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            TipoBlocoArtefato.todo -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(14.dp),
                )
                Box(
                    Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .background(OrbitTokens.textLowN.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
                )
            }
            TipoBlocoArtefato.quote -> Text(
                "❝ …",
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            TipoBlocoArtefato.callout -> Text(
                "✦ hey",
                color = OrbitTokens.bluePastel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            TipoBlocoArtefato.code -> Text(
                "{ }",
                color = OrbitTokens.textHiN,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
            )
            TipoBlocoArtefato.divider -> Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OrbitTokens.textLowN.copy(alpha = 0.55f)),
            )
            TipoBlocoArtefato.paragraph -> Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(OrbitTokens.textLowN.copy(alpha = 0.45f), RoundedCornerShape(2.dp)),
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.65f)
                        .height(3.dp)
                        .background(OrbitTokens.textLowN.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun lembrarOpcoesFiltradas(query: String): List<SlashOpcao> {
    val q = query.trim().lowercase()
    return remember(q) {
        val todas = slashOpcoes()
        if (q.isEmpty()) {
            todas
        } else {
            todas.filter {
                it.titulo.lowercase().contains(q) ||
                    it.subtitulo.lowercase().contains(q) ||
                    it.aliases.any { a -> a.contains(q) }
            }
        }
    }
}

private data class SlashOpcao(
    val titulo: String,
    val subtitulo: String,
    val tipo: TipoBlocoArtefato,
    val props: PropsBlocoArtefato? = null,
    val icone: ImageVector,
    val corIcone: Color,
    val corFundo: Color,
    val aliases: List<String> = emptyList(),
)

private fun slashOpcoes(): List<SlashOpcao> {
        val azul = OrbitTokens.bluePastel
        val surf = OrbitTokens.graphiteSurf
        return listOf(
            SlashOpcao(
                titulo = "Texto",
                subtitulo = "Parágrafo comum",
                tipo = TipoBlocoArtefato.paragraph,
                icone = Icons.Rounded.Notes,
                corIcone = OrbitTokens.textHiN,
                corFundo = surf,
                aliases = listOf("paragrafo", "texto", "p"),
            ),
            SlashOpcao(
                titulo = "Título 1",
                subtitulo = "Heading grande",
                tipo = TipoBlocoArtefato.heading,
                props = PropsBlocoArtefato(level = 1),
                icone = Icons.Rounded.Title,
                corIcone = azul,
                corFundo = azul.copy(alpha = 0.12f),
                aliases = listOf("h1", "titulo", "heading"),
            ),
            SlashOpcao(
                titulo = "Título 2",
                subtitulo = "Heading médio",
                tipo = TipoBlocoArtefato.heading,
                props = PropsBlocoArtefato(level = 2),
                icone = Icons.Rounded.Title,
                corIcone = azul,
                corFundo = azul.copy(alpha = 0.10f),
                aliases = listOf("h2", "titulo"),
            ),
            SlashOpcao(
                titulo = "Título 3",
                subtitulo = "Heading pequeno",
                tipo = TipoBlocoArtefato.heading,
                props = PropsBlocoArtefato(level = 3),
                icone = Icons.Rounded.Title,
                corIcone = azul,
                corFundo = azul.copy(alpha = 0.08f),
                aliases = listOf("h3", "titulo"),
            ),
            SlashOpcao(
                titulo = "Lista",
                subtitulo = "Marcadores",
                tipo = TipoBlocoArtefato.bullet,
                icone = Icons.Rounded.FormatListBulleted,
                corIcone = OrbitTokens.textHiN,
                corFundo = surf,
                aliases = listOf("bullet", "lista", "ul"),
            ),
            SlashOpcao(
                titulo = "Numerada",
                subtitulo = "1. 2. 3.",
                tipo = TipoBlocoArtefato.numbered,
                icone = Icons.Rounded.FormatListNumbered,
                corIcone = OrbitTokens.textHiN,
                corFundo = surf,
                aliases = listOf("numbered", "ol", "numero"),
            ),
            SlashOpcao(
                titulo = "Tarefa",
                subtitulo = "Checkbox pra riscar",
                tipo = TipoBlocoArtefato.todo,
                props = PropsBlocoArtefato(checked = false),
                icone = Icons.Rounded.CheckBoxOutlineBlank,
                corIcone = azul,
                corFundo = azul.copy(alpha = 0.10f),
                aliases = listOf("todo", "tarefa", "check"),
            ),
            SlashOpcao(
                titulo = "Citação",
                subtitulo = "Destaque em quote",
                tipo = TipoBlocoArtefato.quote,
                icone = Icons.Rounded.FormatQuote,
                corIcone = OrbitTokens.textMidN,
                corFundo = surf,
                aliases = listOf("quote", "citacao"),
            ),
            SlashOpcao(
                titulo = "Dica",
                subtitulo = "Callout 💡 com sabor de dica",
                tipo = TipoBlocoArtefato.callout,
                props = PropsBlocoArtefato(callout = SaborCallout.dica.chave),
                icone = Icons.Rounded.Lightbulb,
                corIcone = corDoSabor(SaborCallout.dica),
                corFundo = corDoSabor(SaborCallout.dica).copy(alpha = 0.12f),
                aliases = listOf("dica", "tip", "sacada", "callout"),
            ),
            SlashOpcao(
                titulo = "Atenção",
                subtitulo = "Callout ⚠️ de aviso",
                tipo = TipoBlocoArtefato.callout,
                props = PropsBlocoArtefato(callout = SaborCallout.atencao.chave),
                icone = Icons.Rounded.WarningAmber,
                corIcone = corDoSabor(SaborCallout.atencao),
                corFundo = corDoSabor(SaborCallout.atencao).copy(alpha = 0.12f),
                aliases = listOf("atencao", "aviso", "cuidado", "warning", "callout"),
            ),
            SlashOpcao(
                titulo = "Feito",
                subtitulo = "Callout ✅ de sucesso",
                tipo = TipoBlocoArtefato.callout,
                props = PropsBlocoArtefato(callout = SaborCallout.feito.chave),
                icone = Icons.Rounded.CheckCircle,
                corIcone = corDoSabor(SaborCallout.feito),
                corFundo = corDoSabor(SaborCallout.feito).copy(alpha = 0.12f),
                aliases = listOf("feito", "ok", "sucesso", "done", "callout"),
            ),
            SlashOpcao(
                titulo = "Dúvida",
                subtitulo = "Callout ❓ de pergunta",
                tipo = TipoBlocoArtefato.callout,
                props = PropsBlocoArtefato(callout = SaborCallout.duvida.chave),
                icone = Icons.Rounded.HelpOutline,
                corIcone = corDoSabor(SaborCallout.duvida),
                corFundo = corDoSabor(SaborCallout.duvida).copy(alpha = 0.12f),
                aliases = listOf("duvida", "pergunta", "question", "callout"),
            ),
            SlashOpcao(
                titulo = "Fixado",
                subtitulo = "Callout 📌 de destaque",
                tipo = TipoBlocoArtefato.callout,
                props = PropsBlocoArtefato(callout = SaborCallout.fixado.chave),
                icone = Icons.Rounded.PushPin,
                corIcone = corDoSabor(SaborCallout.fixado),
                corFundo = corDoSabor(SaborCallout.fixado).copy(alpha = 0.12f),
                aliases = listOf("fixado", "pin", "importante", "callout"),
            ),
            SlashOpcao(
                titulo = "Nota",
                subtitulo = "Callout ℹ️ neutro",
                tipo = TipoBlocoArtefato.callout,
                props = PropsBlocoArtefato(callout = SaborCallout.info.chave),
                icone = Icons.Rounded.PriorityHigh,
                corIcone = corDoSabor(SaborCallout.info),
                corFundo = corDoSabor(SaborCallout.info).copy(alpha = 0.12f),
                aliases = listOf("nota", "note", "info", "destaque", "callout"),
            ),
            SlashOpcao(
                titulo = "Código",
                subtitulo = "Bloco monoespaçado",
                tipo = TipoBlocoArtefato.code,
                icone = Icons.Rounded.Code,
                corIcone = OrbitTokens.textHiN,
                corFundo = OrbitTokens.graphiteBg,
                aliases = listOf("code", "codigo"),
            ),
            SlashOpcao(
                titulo = "Divisória",
                subtitulo = "Linha horizontal",
                tipo = TipoBlocoArtefato.divider,
                icone = Icons.Rounded.HorizontalRule,
                corIcone = OrbitTokens.textLowN,
                corFundo = surf,
                aliases = listOf("divider", "linha", "hr"),
            ),
            SlashOpcao(
                titulo = "Divisor com rótulo",
                subtitulo = "Linha com texto no meio",
                tipo = TipoBlocoArtefato.divider,
                props = PropsBlocoArtefato(label = ""),
                icone = Icons.Rounded.HorizontalRule,
                corIcone = OrbitTokens.textMidN,
                corFundo = surf,
                aliases = listOf("divider", "rotulo", "secao", "parte", "linha"),
            ),
        )
}

@Composable
private fun estiloDoBloco(bloco: BlocoArtefato): TextStyle {
    return when (bloco.type) {
        TipoBlocoArtefato.heading -> {
            val nivel = bloco.props?.level ?: 1
            TextStyle(
                color = OrbitTokens.textHiN,
                fontSize = when (nivel) {
                    1 -> 26.sp
                    2 -> 22.sp
                    else -> 18.sp
                },
                fontWeight = FontWeight.Bold,
                fontFamily = OrbitType.display,
                letterSpacing = (-0.4).sp,
                lineHeight = when (nivel) {
                    1 -> 32.sp
                    2 -> 28.sp
                    else -> 24.sp
                },
            )
        }
        TipoBlocoArtefato.code -> TextStyle(
            color = OrbitTokens.textHiN,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 20.sp,
        )
        TipoBlocoArtefato.quote, TipoBlocoArtefato.callout -> TextStyle(
            color = OrbitTokens.textMidN,
            fontSize = 15.sp,
            fontFamily = OrbitType.body,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )
        else -> TextStyle(
            color = OrbitTokens.textHiN,
            fontSize = 16.sp,
            fontFamily = OrbitType.body,
            lineHeight = 26.sp,
        )
    }
}

private fun prefixoVisual(bloco: BlocoArtefato): String? = when (bloco.type) {
    TipoBlocoArtefato.bullet -> "•"
    TipoBlocoArtefato.numbered -> "›"
    TipoBlocoArtefato.quote -> "❝"
    TipoBlocoArtefato.callout -> SaborCallout.from(bloco.props?.callout).emoji
    else -> null
}
