package com.ethan.orbitlab.ui.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.canvas.CanvasElement
import com.ethan.orbitlab.data.canvas.ElementoBounds
import com.ethan.orbitlab.data.canvas.ElementoStyle
import com.ethan.orbitlab.data.canvas.ElementoTipo
import com.ethan.orbitlab.data.canvas.TextProps
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Elemento interativo do canvas — suporta seleção, arrasto e redimensionamento.
 *
 * @param elemento O elemento a renderizar
 * @param isSelected Se o elemento está selecionado
 * @param onSelect Callback quando elemento é selecionado
 * @param onBoundsChange Callback quando bounds mudam (move/resize)
 * @param onDoubleTap Callback para double-tap (editar)
 * @param zoomLevel Nível de zoom atual (para ajustar tamanho dos handles)
 */
@Composable
fun CanvasElementView(
    elemento: CanvasElement,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onBoundsChange: (ElementoBounds) -> Unit,
    onDoubleTap: () -> Unit = {},
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Posição animada
    val animX = remember(elemento.bounds.x) { Animatable(elemento.bounds.x) }
    val animY = remember(elemento.bounds.y) { Animatable(elemento.bounds.y) }

    // Sincroniza quando bounds mudam externamente
    LaunchedEffect(elemento.bounds.x, elemento.bounds.y) {
        animX.snapTo(elemento.bounds.x)
        animY.snapTo(elemento.bounds.y)
    }

    // Estado de arrasto
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Estado de resize
    var isResizing by remember { mutableStateOf<ResizeHandle?>(null) }
    var resizeStartBounds by remember { mutableStateOf(elemento.bounds) }
    var resizeStartOffset by remember { mutableStateOf(Offset.Zero) }

    // Handle size ajustado pelo zoom
    val handleSizePx = (12 / zoomLevel).coerceIn(6f, 24f).dp
    val handleSizePxFloat = with(density) { handleSizePx.toPx() }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    animX.value.roundToInt(),
                    animY.value.roundToInt(),
                )
            }
            .size(
                width = elemento.bounds.width.dp,
                height = elemento.bounds.height.dp,
            )
            .then(
                if (elemento.locked) Modifier
                else Modifier
                    .pointerInput(elemento.id) {
                        detectTapGestures(
                            onTap = { onSelect() },
                            onDoubleTap = { onDoubleTap() },
                        )
                    }
                    .pointerInput(elemento.id) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                                // Atualiza posição em tempo real
                                scope.launch {
                                    animX.snapTo(elemento.bounds.x + dragOffset.x)
                                    animY.snapTo(elemento.bounds.y + dragOffset.y)
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                // Persiste nova posição
                                val newBounds = elemento.bounds.copy(
                                    x = elemento.bounds.x + dragOffset.x,
                                    y = elemento.bounds.y + dragOffset.y,
                                )
                                onBoundsChange(newBounds)
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                isDragging = false
                                // volta à posição original
                                scope.launch {
                                    animX.snapTo(elemento.bounds.x)
                                    animY.snapTo(elemento.bounds.y)
                                }
                                dragOffset = Offset.Zero
                            },
                        )
                    }
            )
    ) {
        // Conteúdo do elemento
        Box(modifier = Modifier.matchParentSize()) {
            when (elemento.tipo) {
                ElementoTipo.text -> {
                    TextElementContent(elemento)
                }
                ElementoTipo.image -> {
                    ImageElementContent(elemento)
                }
                else -> {
                    // Shape/card genérico
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                color = elemento.style.fill?.let {
                                    runCatching { Color(android.graphics.Color.parseColor(it)) }
                                        .getOrNull()
                                } ?: OrbitTokens.graphiteSurf,
                            )
                            .clip(RoundedCornerShape(elemento.style.cornerRadius?.dp ?: 8.dp)),
                    )
                }
            }
        }

        // Selection overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 2.dp,
                        color = OrbitTokens.accent,
                        shape = RoundedCornerShape(elemento.style.cornerRadius?.dp ?: 8.dp),
                    )
            )

            // Handles de redimensionamento
            if (!elemento.locked) {
                ResizeHandles(
                    bounds = elemento.bounds,
                    handleSize = handleSizePxFloat,
                    cornerRadius = elemento.style.cornerRadius ?: 8f,
                    onResizeStart = { handle ->
                        isResizing = handle
                        resizeStartBounds = elemento.bounds
                        resizeStartOffset = Offset.Zero
                    },
                    onResize = { handle, delta ->
                        val newBounds = calcularNovosBounds(resizeStartBounds, handle, delta)
                        // Atualização visual em tempo real
                        onBoundsChange(newBounds)
                    },
                    onResizeEnd = { _, _ ->
                        isResizing = null
                    },
                )
            }
        }
    }
}

/**
 * Handles de redimensionamento nas 4 pontas.
 */
@Composable
private fun ResizeHandles(
    bounds: ElementoBounds,
    handleSize: Float,
    cornerRadius: Float,
    onResizeStart: (ResizeHandle) -> Unit,
    onResize: (ResizeHandle, Offset) -> Unit,
    onResizeEnd: (ResizeHandle, Offset) -> Unit,
) {
    val density = LocalDensity.current

    // Posições dos handles
    val handles = listOf(
        ResizeHandle.TopLeft to Offset(bounds.x, bounds.y),
        ResizeHandle.TopRight to Offset(bounds.x + bounds.width, bounds.y),
        ResizeHandle.BottomLeft to Offset(bounds.x, bounds.y + bounds.height),
        ResizeHandle.BottomRight to Offset(bounds.x + bounds.width, bounds.y + bounds.height),
    )

    handles.forEach { (handle, position) ->
        val offsetX = (position.x - handleSize / 2).roundToInt()
        val offsetY = (position.y - handleSize / 2).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX, offsetY) }
                .size(handleSize.roundToInt().dp)
                .clip(RoundedCornerShape(cornerRadius.coerceAtMost(handleSize / 2).dp))
                .background(OrbitTokens.accent)
                .border(
                    width = 1.5.dp,
                    color = OrbitTokens.onBluePastel,
                    shape = RoundedCornerShape(cornerRadius.coerceAtMost(handleSize / 2).dp),
                )
                .pointerInput(handle) {
                    detectDragGestures(
                        onDragStart = { onResizeStart(handle) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onResize(handle, dragAmount)
                        },
                        onDragEnd = { onResizeEnd(handle, Offset.Zero) },
                        onDragCancel = { onResizeEnd(handle, Offset.Zero) },
                    )
                }
        )
    }
}

/**
 * Handles de redimensionamento.
 */
enum class ResizeHandle {
    TopLeft, TopRight, BottomLeft, BottomRight,
}

/**
 * Calcula novos bounds baseado no handle e delta.
 */
private fun calcularNovosBounds(
    bounds: ElementoBounds,
    handle: ResizeHandle,
    delta: Offset,
): ElementoBounds {
    return when (handle) {
        ResizeHandle.TopLeft -> bounds.copy(
            x = bounds.x + delta.x,
            y = bounds.y + delta.y,
            width = (bounds.width - delta.x).coerceAtLeast(50f),
            height = (bounds.height - delta.y).coerceAtLeast(50f),
        )
        ResizeHandle.TopRight -> bounds.copy(
            y = bounds.y + delta.y,
            width = (bounds.width + delta.x).coerceAtLeast(50f),
            height = (bounds.height - delta.y).coerceAtLeast(50f),
        )
        ResizeHandle.BottomLeft -> bounds.copy(
            x = bounds.x + delta.x,
            width = (bounds.width - delta.x).coerceAtLeast(50f),
            height = (bounds.height + delta.y).coerceAtLeast(50f),
        )
        ResizeHandle.BottomRight -> bounds.copy(
            width = (bounds.width + delta.x).coerceAtLeast(50f),
            height = (bounds.height + delta.y).coerceAtLeast(50f),
        )
    }
}

/**
 * Conteúdo de elemento de texto.
 */
@Composable
private fun TextElementContent(elemento: CanvasElement) {
    val textProps = elemento.textProps ?: TextProps(content = "")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = elemento.style.fill?.let {
                    runCatching { Color(android.graphics.Color.parseColor(it)) }
                        .getOrNull()
                } ?: Color.Transparent,
            )
            .padding(8.dp),
        contentAlignment = when (textProps.align) {
            "center" -> Alignment.Center
            "right" -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        },
    ) {
        Text(
            text = textProps.content,
            style = TextStyle(
                fontFamily = com.ethan.orbitlab.ui.theme.Hanken,
                fontSize = textProps.fontSize.sp,
                fontWeight = FontWeight(textProps.fontWeight),
            ),
            color = textProps.color?.let {
                runCatching { Color(android.graphics.Color.parseColor(it)) }
                    .getOrNull()
            } ?: OrbitTokens.textHiN,
        )
    }
}

/**
 * Conteúdo de elemento de imagem (placeholder).
 */
@Composable
private fun ImageElementContent(elemento: CanvasElement) {
    val imageProps = elemento.imageProps
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(OrbitTokens.graphiteRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (imageProps?.src?.isNotBlank() == true) {
            // Placeholder - usar Coil para carregar imagem em produção
            Text(
                text = "🖼",
                style = TextStyle(fontSize = 32.sp),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                if (imageProps != null) {
                    Text(
                        text = "Adicionar imagem",
                        style = OrbitType.Body.XS,
                        color = OrbitTokens.textLow,
                    )
                }
            }
        }
    }
}

/**
 * Wrapper para canvas com elementos interativos.
 */
@Composable
fun InteractiveCanvas(
    elementos: Map<String, CanvasElement>,
    selecionadoId: String?,
    zoomLevel: Float = 1f,
    onSelect: (String?) -> Unit,
    onBoundsChange: (String, ElementoBounds) -> Unit,
    onElementDoubleTap: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(modifier = modifier) {
        elementos.values
            .filter { it.visible }
            .sortedBy { it.bounds.y }
            .forEach { el ->
                CanvasElementView(
                    elemento = el,
                    isSelected = el.id == selecionadoId,
                    onSelect = { onSelect(el.id) },
                    onBoundsChange = { newBounds -> onBoundsChange(el.id, newBounds) },
                    onDoubleTap = { onElementDoubleTap(el.id) },
                    zoomLevel = zoomLevel,
                )
            }

        content()
    }
}
