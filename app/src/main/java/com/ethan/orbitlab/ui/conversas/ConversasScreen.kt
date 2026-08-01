package com.ethan.orbitlab.ui.conversas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.ChatRepository
import com.ethan.orbitlab.data.Conversa
import com.ethan.orbitlab.data.export.ConversaExporter
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.ethan.orbitlab.ui.theme.rememberOrbitPressScale
import kotlinx.coroutines.launch

private val SearchPill = RoundedCornerShape(999.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversasScreen(
    onOpenChat: (String) -> Unit = {},
    onAbrirEstante: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val conversas by ChatRepository.conversas.collectAsState()

    val selecionados = remember { mutableStateListOf<String>() }
    val isSelectionMode = selecionados.isNotEmpty()
    val exportScope = rememberCoroutineScope()
    val context = LocalContext.current

    val filtradas = remember(conversas, query) {
        conversas
            .filter {
                query.isBlank() ||
                    it.titulo.contains(query, ignoreCase = true) ||
                    it.preview.contains(query, ignoreCase = true)
            }
            .sortedByDescending { it.ultimaAtualizacao }
    }

    val grupos = remember(filtradas) {
        filtradas.groupBy { it.grupoDia }.toList()
    }

    BackHandler(enabled = isSelectionMode) {
        selecionados.clear()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = OrbitMetrics.pagePadding),
            ) {
                HeaderConversas(
                    isSelectionMode = isSelectionMode,
                    quantidadeSelecionada = selecionados.size,
                    onAbrirEstante = onAbrirEstante,
                )
                Spacer(modifier = Modifier.height(16.dp))
                SearchBar(query = query, onQueryChange = { query = it })
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                conversas.isEmpty() -> {
                    EstadoVazio(
                        icone = Icons.Rounded.ChatBubbleOutline,
                        titulo = "Nenhuma conversa ainda",
                        corpo = "Toque no + pra começar a falar com a Luna.",
                    )
                }
                filtradas.isEmpty() -> {
                    EstadoVazio(
                        icone = Icons.Rounded.SearchOff,
                        titulo = "Nada encontrado",
                        corpo = "Tenta outra busca no histórico.",
                    )
                }
                else -> {
                    ListaConversas(
                        grupos = grupos,
                        isSelectionMode = isSelectionMode,
                        selecionados = selecionados,
                        bottomPad = if (isSelectionMode) 84.dp else 24.dp,
                        onOpenChat = { id ->
                            if (isSelectionMode) {
                                if (selecionados.contains(id)) selecionados.remove(id)
                                else selecionados.add(id)
                            } else {
                                onOpenChat(id)
                            }
                        },
                        onLongPress = { id ->
                            if (!selecionados.contains(id)) selecionados.add(id)
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSelectionMode,
            enter = OrbitMotion.popupEnter(),
            exit = OrbitMotion.popupExit(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(bottom = 12.dp),
        ) {
            BarraSelecao(
                quantidade = selecionados.size,
                onCancelar = { selecionados.clear() },
                onExportar = {
                    val ids = selecionados.toList()
                    selecionados.clear()
                    exportScope.launch {
                        val itens = ids.mapNotNull { id ->
                            val conv = ChatRepository.getConversa(id) ?: return@mapNotNull null
                            val msgs = ChatRepository.carregarMensagens(id)
                            if (msgs.isEmpty()) null else ConversaExporter.Item(conv.titulo, msgs)
                        }
                        if (itens.isNotEmpty()) ConversaExporter.compartilhar(context, itens)
                    }
                },
                onApagar = {
                    ChatRepository.deletarConversas(selecionados.toList())
                    selecionados.clear()
                },
            )
        }
    }
}

@Composable
private fun HeaderConversas(
    isSelectionMode: Boolean,
    quantidadeSelecionada: Int,
    onAbrirEstante: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Conversas",
                color = OrbitTokens.textHiN,
                fontSize = OrbitMetrics.titleSize,
                fontWeight = OrbitMetrics.titleWeight,
                letterSpacing = (-0.3).sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (isSelectionMode) {
                Text(
                    text = "$quantidadeSelecionada selecionada${if (quantidadeSelecionada == 1) "" else "s"}",
                    color = OrbitTokens.textHiN,
                    fontSize = OrbitMetrics.captionSize,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Histórico com a Luna",
                        color = OrbitTokens.textMidN,
                        fontSize = OrbitMetrics.captionSize,
                    )
                    Icon(
                        Icons.Rounded.Album,
                        contentDescription = null,
                        tint = OrbitTokens.textLowN,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        // Atalho pra Estante — os artefatos são primos da conversa, então moram aqui, ao lado
        // do histórico (e não no menu do "+", que é só pra CRIAR coisa). Some no modo seleção
        // pra não competir com a barra de ações.
        if (!isSelectionMode) {
            Row(
                modifier = Modifier
                    .clip(SearchPill)
                    .background(OrbitTokens.graphiteSurf)
                    .border(1.dp, OrbitTokens.graphiteHair, SearchPill)
                    .orbitPressable(onClick = onAbrirEstante)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Rounded.Description,
                    contentDescription = null,
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Artefatos",
                    color = OrbitTokens.textHiN,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun BarraSelecao(
    quantidade: Int,
    onCancelar: () -> Unit,
    onExportar: () -> Unit,
    onApagar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusCard))
            .background(OrbitTokens.graphiteRaised)
            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(OrbitMetrics.radiusCard))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OrbitTokens.graphiteSurf)
                .orbitPressable(onClick = onCancelar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Cancelar",
                tint = OrbitTokens.textHiN,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = "$quantidade selecionada${if (quantidade == 1) "" else "s"}",
            color = OrbitTokens.textMidN,
            fontSize = OrbitMetrics.bodySize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OrbitTokens.graphiteSurf)
                .orbitPressable(onClick = onExportar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.IosShare,
                contentDescription = "Exportar",
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(18.dp),
            )
        }

        // Apagar mantém o vermelho — é semântica de ação destrutiva, não acento de marca.
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                .background(OrbitTokens.danger)
                .orbitPressable(onClick = onApagar)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Apagar",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SearchPill)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, SearchPill)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = OrbitMetrics.bodySize),
            modifier = Modifier.weight(1f),
            singleLine = true,
            cursorBrush = SolidColor(OrbitTokens.bluePastel),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "Buscar no histórico...",
                        color = OrbitTokens.textLowN,
                        fontSize = OrbitMetrics.bodySize,
                    )
                }
                innerTextField()
            },
        )
    }
}

@Composable
private fun EstadoVazio(
    icone: ImageVector,
    titulo: String,
    corpo: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OrbitMetrics.pagePadding)
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(0.85f),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrbitTokens.graphiteRaised)
                    .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icone, contentDescription = null, tint = OrbitTokens.textMidN, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                titulo,
                color = OrbitTokens.textHiN,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                corpo,
                color = OrbitTokens.textMidN,
                fontSize = OrbitMetrics.bodySize,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListaConversas(
    grupos: List<Pair<String, List<Conversa>>>,
    isSelectionMode: Boolean,
    selecionados: List<String>,
    bottomPad: Dp,
    onOpenChat: (String) -> Unit,
    onLongPress: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = OrbitMetrics.pagePadding,
            end = OrbitMetrics.pagePadding,
            bottom = bottomPad,
        ),
    ) {
        grupos.forEach { (rotulo, itens) ->
            stickyHeader(key = "h-$rotulo") {
                Text(
                    text = rotulo.uppercase(),
                    color = OrbitTokens.textLowN,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OrbitTokens.graphiteBg)
                        .padding(top = 14.dp, bottom = 8.dp),
                )
            }
            items(itens, key = { it.id }) { conv ->
                ConversaItem(
                    conv = conv,
                    isSelectionMode = isSelectionMode,
                    isSelected = selecionados.contains(conv.id),
                    onOpenChat = onOpenChat,
                    onLongPress = onLongPress,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversaItem(
    conv: Conversa,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onOpenChat: (String) -> Unit,
    onLongPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Lista NUA (direção 1.0, estilo Claude): sem cartão, sem borda, sem ladrilho, sem prévia.
    // Só título + hora, muito ar. O único preenchimento que aparece é a faixa da seleção —
    // e mesmo essa é grafite quieto, não acento. Contraste vem do texto, não de moldura.
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    val (interaction, scale) = rememberOrbitPressScale()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .then(if (isSelected) Modifier.background(OrbitTokens.graphiteSurf) else Modifier)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onOpenChat(conv.id) },
                onLongClick = { onLongPress(conv.id) },
            )
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = conv.titulo,
            color = OrbitTokens.textHiN,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        if (isSelectionMode) {
            SelectionCheck(selected = isSelected)
        } else {
            Text(
                text = conv.horaFormatada,
                color = OrbitTokens.textLowN,
                fontSize = OrbitMetrics.captionSize,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun SelectionCheck(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(OrbitTokens.bluePastel)
                } else {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.5.dp, OrbitTokens.textLowN, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = OrbitTokens.onBluePastel,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF17181B, widthDp = 380, heightDp = 800)
@Composable
fun ConversasScreenPreview() {
    ConversasScreen()
}
