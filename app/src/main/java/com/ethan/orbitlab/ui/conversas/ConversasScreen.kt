package com.ethan.orbitlab.ui.conversas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
import com.ethan.orbitlab.ui.theme.OrbitFills
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import com.ethan.orbitlab.ui.theme.rememberOrbitPressScale

private val SearchPill = RoundedCornerShape(999.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversasScreen(onOpenChat: (String) -> Unit = {}) {
    var query by remember { mutableStateOf("") }
    val conversas by ChatRepository.conversas.collectAsState()

    val selecionados = remember { mutableStateListOf<String>() }
    val isSelectionMode = selecionados.isNotEmpty()

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

    // Destaque visual da conversa mais recente (mockup: barra + canto azul).
    val idDestaque = filtradas.firstOrNull()?.id

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
                .padding(top = 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = OrbitMetrics.pagePadding),
            ) {
                HeaderConversas(
                    isSelectionMode = isSelectionMode,
                    quantidadeSelecionada = selecionados.size,
                )
                Spacer(modifier = Modifier.height(16.dp))
                SearchBar(query = query, onQueryChange = { query = it })
                Spacer(modifier = Modifier.height(16.dp))
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
                        idDestaque = idDestaque,
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
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_orbit_symbol),
            contentDescription = "Orbit",
            tint = Color.Unspecified,
            modifier = Modifier.size(32.dp),
        )
        Column {
            Text(
                "Conversas",
                color = OrbitTokens.textHigh,
                fontSize = OrbitMetrics.titleSize,
                fontWeight = OrbitMetrics.titleWeight,
                letterSpacing = (-0.3).sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (isSelectionMode) {
                Text(
                    text = "$quantidadeSelecionada selecionada${if (quantidadeSelecionada == 1) "" else "s"}",
                    color = OrbitTokens.textHigh,
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
                        color = OrbitTokens.textMid,
                        fontSize = OrbitMetrics.captionSize,
                    )
                    Icon(
                        Icons.Rounded.Album,
                        contentDescription = null,
                        tint = OrbitTokens.textLow,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BarraSelecao(
    quantidade: Int,
    onCancelar: () -> Unit,
    onApagar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusCard))
            .background(OrbitTokens.surfaceRaised)
            .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(OrbitMetrics.radiusCard))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OrbitTokens.surfaceHover)
                .orbitPressable(onClick = onCancelar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Cancelar",
                tint = OrbitTokens.textHigh,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = "$quantidade selecionada${if (quantidade == 1) "" else "s"}",
            color = OrbitTokens.textMid,
            fontSize = OrbitMetrics.bodySize,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(OrbitMetrics.radiusPill))
                .background(OrbitFills.danger.brush)
                .orbitPressable(onClick = onApagar)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = OrbitFills.danger.onFill,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Apagar",
                color = OrbitFills.danger.onFill,
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
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.borderSoft, SearchPill)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = OrbitTokens.textLow,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = OrbitTokens.textHigh, fontSize = OrbitMetrics.bodySize),
            modifier = Modifier.weight(1f),
            singleLine = true,
            cursorBrush = SolidColor(OrbitTokens.accent),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "Buscar no histórico...",
                        color = OrbitTokens.textLow,
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
                    .background(OrbitTokens.surfaceRaised)
                    .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icone, contentDescription = null, tint = OrbitTokens.textMid, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                titulo,
                color = OrbitTokens.textHigh,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                corpo,
                color = OrbitTokens.textMid,
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
    idDestaque: String?,
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
                    color = OrbitTokens.textLow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OrbitTokens.ink1)
                        .padding(top = 4.dp, bottom = 10.dp),
                )
            }
            items(itens, key = { it.id }) { conv ->
                val ativa = !isSelectionMode && conv.id == idDestaque
                ConversaItem(
                    conv = conv,
                    isSelectionMode = isSelectionMode,
                    isSelected = selecionados.contains(conv.id),
                    isDestaque = ativa,
                    onOpenChat = onOpenChat,
                    onLongPress = onLongPress,
                    modifier = Modifier.padding(bottom = OrbitMetrics.itemGap),
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
    isDestaque: Boolean,
    onOpenChat: (String) -> Unit,
    onLongPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val marcada = isSelected || isDestaque
    val borderColor = when {
        isSelected -> OrbitTokens.border
        isDestaque -> OrbitTokens.borderSoft
        else -> OrbitTokens.borderSoft
    }
    val backgroundColor = when {
        isSelected -> OrbitTokens.surfaceRaised
        else -> OrbitTokens.surface
    }
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)
    val (interaction, scale) = rememberOrbitPressScale()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onOpenChat(conv.id) },
                onLongClick = { onLongPress(conv.id) },
            ),
    ) {
        // Arcos só no item em destaque/selecionado — evita Canvas em cada linha no scroll.
        if (marcada) {
            Canvas(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.55f },
            ) {
                val stroke = Stroke(width = 1.2.dp.toPx())
                val cx = size.width * 0.92f
                val cy = size.height * 0.55f
                val base = size.minDimension * 0.55f
                for (i in 0..3) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.045f),
                        radius = base + i * (size.minDimension * 0.22f),
                        center = Offset(cx, cy),
                        style = stroke,
                    )
                }
            }
        }

        if (marcada) {
            Canvas(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp),
            ) {
                val path = Path().apply {
                    moveTo(size.width, size.height)
                    lineTo(size.width, size.height * 0.18f)
                    lineTo(size.width * 0.18f, size.height)
                    close()
                }
                drawPath(path, color = OrbitTokens.accent)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (marcada) {
                Box(
                    Modifier
                        .padding(vertical = 14.dp)
                        .width(3.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                        .background(OrbitTokens.accent),
                )
                Spacer(Modifier.width(13.dp))
            } else {
                Spacer(Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conv.titulo,
                        color = OrbitTokens.textHigh,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!isSelectionMode) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = conv.horaFormatada,
                            color = OrbitTokens.textLow,
                            fontSize = OrbitMetrics.captionSize,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = conv.preview,
                    color = OrbitTokens.textMid,
                    fontSize = OrbitMetrics.bodySize,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isSelectionMode) {
                Row(
                    Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(12.dp))
                    SelectionCheck(selected = isSelected)
                }
            }
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
                    Modifier.background(OrbitTokens.accent)
                } else {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.5.dp, OrbitTokens.border, CircleShape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1014, widthDp = 380, heightDp = 800)
@Composable
fun ConversasScreenPreview() {
    ConversasScreen()
}
