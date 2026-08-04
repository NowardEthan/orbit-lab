package com.ethan.orbitlab.ui.artefato

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.artefato.BlocoArtefato
import com.ethan.orbitlab.data.artefato.PropsBlocoArtefato
import com.ethan.orbitlab.data.artefato.TipoBlocoArtefato
import com.ethan.orbitlab.data.artefato.novoIdBloco
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType

/**
 * Editor Notion-like: uma linha por bloco, Enter cria parágrafo, Backspace no vazio funde/apaga,
 * `/` abre o menu de tipos.
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (lista.size == 1 && lista[0].text.isEmpty() && lista[0].type == TipoBlocoArtefato.paragraph) {
            EmptyStateArtefato(hint = emptyHint)
            Spacer(Modifier.height(8.dp))
        }

        lista.forEachIndexed { index, bloco ->
            BlocoLinha(
                bloco = bloco,
                index = index,
                total = lista.size,
                pedirFoco = focusBlocoId == bloco.id,
                onFocoPedidoConsumido = onFocusConsumed,
                onChange = { novo ->
                    onBlocosChange(lista.toMutableList().also { it[index] = novo })
                },
                onEnter = {
                    val novo = BlocoArtefato(id = novoIdBloco(), type = TipoBlocoArtefato.paragraph, text = "")
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
                    val limpo = bloco.text.removePrefix("/").trimStart()
                    onBlocosChange(
                        lista.toMutableList().also {
                            it[index] = bloco.copy(
                                type = tipo,
                                text = if (tipo == TipoBlocoArtefato.divider) "" else limpo,
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
            )
        }
    }
}

@Composable
private fun EmptyStateArtefato(hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OrbitTokens.graphiteSurf.copy(alpha = 0.55f))
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Text(
            text = "Página vazia",
            color = OrbitTokens.textHiN,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = OrbitType.display,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = hint,
            color = OrbitTokens.textMidN,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun BlocoLinha(
    bloco: BlocoArtefato,
    index: Int,
    total: Int,
    pedirFoco: Boolean,
    onFocoPedidoConsumido: () -> Unit,
    onChange: (BlocoArtefato) -> Unit,
    onEnter: () -> Unit,
    onBackspaceVazio: () -> Unit,
    onSlashTipo: (TipoBlocoArtefato, PropsBlocoArtefato?) -> Unit,
    onMover: (Int) -> Unit,
) {
    val focusRequester = remember(bloco.id) { FocusRequester() }
    var focused by remember(bloco.id) { mutableStateOf(false) }
    var slashAberto by remember(bloco.id) { mutableStateOf(false) }
    var valor by remember(bloco.id, bloco.text) {
        mutableStateOf(TextFieldValue(bloco.text, TextRange(bloco.text.length)))
    }

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

    if (bloco.type == TipoBlocoArtefato.divider) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .clickable { /* selecionável no futuro */ },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(OrbitTokens.graphiteHair),
            )
            Text(
                "  divisória  ",
                color = OrbitTokens.textLowN,
                fontSize = 11.sp,
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(OrbitTokens.graphiteHair),
            )
            MenuBlocoHandle(
                podeSubir = index > 0,
                podeDescer = index < total - 1,
                onMover = onMover,
                onTipo = { t, p -> onSlashTipo(t, p) },
            )
        }
        return
    }

    val style = estiloDoBloco(bloco)
    val prefix = prefixoVisual(bloco)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when (bloco.type) {
                    TipoBlocoArtefato.callout -> OrbitTokens.bluePastel.copy(alpha = 0.08f)
                    TipoBlocoArtefato.quote -> OrbitTokens.graphiteSurf.copy(alpha = 0.4f)
                    TipoBlocoArtefato.code -> OrbitTokens.graphiteRaised.copy(alpha = 0.55f)
                    else -> androidx.compose.ui.graphics.Color.Transparent
                },
            )
            .padding(vertical = 2.dp),
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
                    slashAberto = t.startsWith("/") && !t.contains('\n')
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
                    .onFocusChanged { focused = it.isFocused }
                    .onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when {
                            e.key == Key.Enter && !e.nativeKeyEvent.isShiftPressed -> {
                                onEnter()
                                true
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
                            text = when (bloco.type) {
                                TipoBlocoArtefato.heading -> "Título"
                                TipoBlocoArtefato.bullet, TipoBlocoArtefato.numbered -> "Item"
                                TipoBlocoArtefato.todo -> "Tarefa"
                                TipoBlocoArtefato.quote -> "Citação"
                                TipoBlocoArtefato.callout -> "Destaque"
                                TipoBlocoArtefato.code -> "código…"
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

            DropdownMenu(
                expanded = slashAberto,
                onDismissRequest = { slashAberto = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = OrbitTokens.graphiteRaised,
                border = BorderStroke(1.dp, OrbitTokens.graphiteHair),
            ) {
                SlashOpcoes.forEach { op ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(op.titulo, color = OrbitTokens.textHiN, fontSize = 14.sp)
                                Text(op.subtitulo, color = OrbitTokens.textLowN, fontSize = 11.sp)
                            }
                        },
                        onClick = {
                            slashAberto = false
                            onSlashTipo(op.tipo, op.props)
                        },
                    )
                }
            }
        }

        if (focused) {
            MenuBlocoHandle(
                podeSubir = index > 0,
                podeDescer = index < total - 1,
                onMover = onMover,
                onTipo = { t, p -> onSlashTipo(t, p) },
            )
        }
    }
}

@Composable
private fun MenuBlocoHandle(
    podeSubir: Boolean,
    podeDescer: Boolean,
    onMover: (Int) -> Unit,
    onTipo: (TipoBlocoArtefato, PropsBlocoArtefato?) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        Icon(
            Icons.Rounded.DragHandle,
            contentDescription = "Opções do bloco",
            tint = OrbitTokens.textLowN,
            modifier = Modifier
                .size(20.dp)
                .clickable { aberto = true },
        )
        DropdownMenu(
            expanded = aberto,
            onDismissRequest = { aberto = false },
            containerColor = OrbitTokens.graphiteRaised,
            shape = RoundedCornerShape(14.dp),
        ) {
            if (podeSubir) {
                DropdownMenuItem(
                    text = { Text("Mover pra cima", color = OrbitTokens.textHiN) },
                    onClick = { aberto = false; onMover(-1) },
                )
            }
            if (podeDescer) {
                DropdownMenuItem(
                    text = { Text("Mover pra baixo", color = OrbitTokens.textHiN) },
                    onClick = { aberto = false; onMover(1) },
                )
            }
            SlashOpcoes.forEach { op ->
                DropdownMenuItem(
                    text = { Text(op.titulo, color = OrbitTokens.textHiN, fontSize = 13.sp) },
                    onClick = {
                        aberto = false
                        onTipo(op.tipo, op.props)
                    },
                )
            }
        }
    }
}

private data class SlashOpcao(
    val titulo: String,
    val subtitulo: String,
    val tipo: TipoBlocoArtefato,
    val props: PropsBlocoArtefato? = null,
)

private val SlashOpcoes = listOf(
    SlashOpcao("Texto", "Parágrafo comum", TipoBlocoArtefato.paragraph),
    SlashOpcao("Título 1", "Heading grande", TipoBlocoArtefato.heading, PropsBlocoArtefato(level = 1)),
    SlashOpcao("Título 2", "Heading médio", TipoBlocoArtefato.heading, PropsBlocoArtefato(level = 2)),
    SlashOpcao("Título 3", "Heading pequeno", TipoBlocoArtefato.heading, PropsBlocoArtefato(level = 3)),
    SlashOpcao("Lista", "Marcadores", TipoBlocoArtefato.bullet),
    SlashOpcao("Numerada", "1. 2. 3.", TipoBlocoArtefato.numbered),
    SlashOpcao("Tarefa", "Checkbox", TipoBlocoArtefato.todo, PropsBlocoArtefato(checked = false)),
    SlashOpcao("Citação", "Quote", TipoBlocoArtefato.quote),
    SlashOpcao("Destaque", "Callout", TipoBlocoArtefato.callout),
    SlashOpcao("Código", "Bloco mono", TipoBlocoArtefato.code),
    SlashOpcao("Divisória", "Linha horizontal", TipoBlocoArtefato.divider),
)

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
    TipoBlocoArtefato.callout -> "✦"
    else -> null
}
