package com.ethan.orbitlab.ui.canvas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.ethan.orbitlab.data.canvas.CanvasElement
import com.ethan.orbitlab.data.canvas.ElementoBounds
import com.ethan.orbitlab.data.canvas.ElementoTipo
import com.ethan.orbitlab.data.canvas.ShapeType
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlin.math.abs

/**
 * Canvas infinito com pan e zoom.
 *
 * @param elementos Mapa de ID → CanvasElement a renderizar
 * @param selecionadoId ID do elemento selecionado (para highlight)
 * @param onElementoClicado Callback quando elemento é clicado
 * @param onCanvasClicado Callback quando canvas (não elemento) é clicado
 * @param onBoundsChange Callback quando elemento é movido/redimensionado
 * @param panHabilitado Se pan está habilitado
 * @param zoomHabilitado Se zoom está habilitado
 */
@Composable
fun InfiniteCanvas(
    elementos: Map<String, CanvasElement>,
    selecionadoId: String? = null,
    onElementoClicado: (String) -> Unit = {},
    onCanvasClicado: (Offset) -> Unit = {},
    onBoundsChange: (String, ElementoBounds) -> Unit = { _, _ -> },
    panHabilitado: Boolean = true,
    zoomHabilitado: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Transformação do canvas
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    // Para animação suave
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    val animScale = remember { Animatable(1f) }

    // Sync com valores animados
    LaunchedEffect(offsetX, offsetY, scale) {
        animOffsetX.snapTo(offsetX)
        animOffsetY.snapTo(offsetY)
        animScale.snapTo(scale)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OrbitTokens.graphiteBg)
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectCanvasGestures(
                    panHabilitado = panHabilitado,
                    zoomHabilitado = zoomHabilitado,
                    onPan = { dx, dy ->
                        offsetX += dx
                        offsetY += dy
                    },
                    onZoom = { zoomFactor, centroid ->
                        val newScale = (scale * zoomFactor).coerceIn(0.25f, 4f)
                        // Zoom em torno do centróide do gesto
                        if (zoomHabilitado) {
                            val scaleChange = newScale / scale
                            offsetX = centroid.x - (centroid.x - offsetX) * scaleChange
                            offsetY = centroid.y - (centroid.y - offsetY) * scaleChange
                            scale = newScale
                        }
                    },
                    onTap = { offset ->
                        // Converte para coordenadas do canvas
                        val canvasX = (offset.x - animOffsetX.value) / animScale.value
                        val canvasY = (offset.y - animOffsetY.value) / animScale.value
                        // Procura elemento clicado
                        val clicado = elementos.values
                            .filter { it.visible && !it.locked }
                            .sortedByDescending { it.bounds.y }
                            .firstOrNull { el ->
                                val b = el.bounds
                                canvasX >= b.x && canvasX <= b.x + b.width &&
                                        canvasY >= b.y && canvasY <= b.y + b.height
                            }
                        if (clicado != null) {
                            onElementoClicado(clicado.id)
                        } else {
                            onCanvasClicado(Offset(canvasX, canvasY))
                        }
                    },
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Aplica transformação
            withTransform({
                translate(left = animOffsetX.value, top = animOffsetY.value)
                scale(scaleX = animScale.value, scaleY = animScale.value, pivot = Offset.Zero)
            }) {
                // Grid de fundo
                desenharGrid()

                // Elementos
                elementos.values
                    .filter { it.visible }
                    .sortedBy { it.bounds.y }
                    .forEach { el ->
                        desenharElemento(
                            elemento = el,
                            isSelected = el.id == selecionadoId,
                        )
                    }
            }
        }

        // Overlay para conteúdo composable (ex: componentes Orbit DS)
        content()
    }
}

/**
 * Desenha grid de fundo.
 */
private fun DrawScope.desenharGrid() {
    val gridSize = 50f
    val gridColor = Color.White.copy(alpha = 0.03f)

    // Linhas verticals
    var x = 0f
    while (x < size.width * 3) {
        drawLine(
            color = gridColor,
            start = Offset(x - size.width, 0f),
            end = Offset(x - size.width, size.height * 3),
            strokeWidth = 1f,
        )
        x += gridSize
    }

    // Linhas horizontais
    var y = 0f
    while (y < size.height * 3) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y - size.height),
            end = Offset(size.width * 3, y - size.height),
            strokeWidth = 1f,
        )
        y += gridSize
    }
}

/**
 * Desenha um elemento no canvas.
 */
private fun DrawScope.desenharElemento(
    elemento: CanvasElement,
    isSelected: Boolean,
) {
    val bounds = elemento.bounds
    val style = elemento.style

    // Cor de preenchimento
    val fillColor = style.fill?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    } ?: Color.Transparent

    // Cor do stroke
    val strokeColor = style.strokeColor?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    } ?: OrbitTokens.border

    val strokeWidth = style.strokeWidth ?: 1f
    val cornerRadius = style.cornerRadius ?: 0f
    val opacity = style.opacity ?: 1f

    when (elemento.tipo) {
        ElementoTipo.card, ElementoTipo.group -> {
            // Retângulo com possíveis cantos arredondados
            drawRoundRect(
                color = fillColor.copy(alpha = opacity),
                topLeft = Offset(bounds.x, bounds.y),
                size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(bounds.x, bounds.y),
                size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                style = Stroke(width = strokeWidth),
            )
        }

        ElementoTipo.shape -> {
            when (elemento.shapeProps?.shapeType ?: ShapeType.rect) {
                ShapeType.rect -> {
                    drawRoundRect(
                        color = fillColor.copy(alpha = opacity),
                        topLeft = Offset(bounds.x, bounds.y),
                        size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                    )
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(bounds.x, bounds.y),
                        size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                        style = Stroke(width = strokeWidth),
                    )
                }
                ShapeType.circle -> {
                    drawCircle(
                        color = fillColor.copy(alpha = opacity),
                        radius = minOf(bounds.width, bounds.height) / 2,
                        center = Offset(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2),
                    )
                    drawCircle(
                        color = strokeColor,
                        radius = minOf(bounds.width, bounds.height) / 2,
                        center = Offset(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2),
                        style = Stroke(width = strokeWidth),
                    )
                }
                ShapeType.line -> {
                    drawLine(
                        color = strokeColor,
                        start = Offset(bounds.x, bounds.y),
                        end = Offset(bounds.x + bounds.width, bounds.y + bounds.height),
                        strokeWidth = strokeWidth,
                    )
                }
                ShapeType.triangle -> {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(bounds.x + bounds.width / 2, bounds.y)
                        lineTo(bounds.x + bounds.width, bounds.y + bounds.height)
                        lineTo(bounds.x, bounds.y + bounds.height)
                        close()
                    }
                    drawPath(path, fillColor.copy(alpha = opacity))
                    drawPath(path, strokeColor, style = Stroke(width = strokeWidth))
                }
            }
        }

        ElementoTipo.image -> {
            // Placeholder para imagem (a imagem real virá via Coil/AsyncImage)
            drawRoundRect(
                color = OrbitTokens.surface,
                topLeft = Offset(bounds.x, bounds.y),
                size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
            )
            drawRoundRect(
                color = OrbitTokens.border,
                topLeft = Offset(bounds.x, bounds.y),
                size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                style = Stroke(width = 1f),
            )
            // Ícone de imagem placeholder
            drawCircle(
                color = OrbitTokens.textLow,
                radius = 8f,
                center = Offset(bounds.x + bounds.width / 2 - 8, bounds.y + bounds.height / 2 - 8),
            )
        }

        ElementoTipo.connector -> {
            // TODO: Implementar conector com base em anchor points
            val cp = elemento.connectorProps
            if (cp != null) {
                // Linha simples por enquanto
                drawLine(
                    color = cp.lineColor?.let {
                        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
                    } ?: strokeColor,
                    start = Offset(bounds.x, bounds.y),
                    end = Offset(bounds.x + bounds.width, bounds.y + bounds.height),
                    strokeWidth = cp.lineWidth,
                )
            }
        }

        ElementoTipo.text -> {
            // Placeholder para texto (usar Text do Compose fora do Canvas)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(bounds.x, bounds.y),
                size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
            )
        }
    }

    // Selection highlight
    if (isSelected) {
        val selectionPadding = 4f
        drawRoundRect(
            color = OrbitTokens.accent.copy(alpha = 0.3f),
            topLeft = Offset(bounds.x - selectionPadding, bounds.y - selectionPadding),
            size = androidx.compose.ui.geometry.Size(
                bounds.width + selectionPadding * 2,
                bounds.height + selectionPadding * 2,
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius + 2),
            style = Stroke(width = 2f),
        )
        // Handles de resize
        val handleSize = 8f
        val corners = listOf(
            Offset(bounds.x - handleSize / 2, bounds.y - handleSize / 2),
            Offset(bounds.x + bounds.width - handleSize / 2, bounds.y - handleSize / 2),
            Offset(bounds.x - handleSize / 2, bounds.y + bounds.height - handleSize / 2),
            Offset(bounds.x + bounds.width - handleSize / 2, bounds.y + bounds.height - handleSize / 2),
        )
        corners.forEach { corner ->
            drawCircle(
                color = OrbitTokens.accent,
                radius = handleSize / 2,
                center = corner,
            )
        }
    }
}

/**
 * Detecta gestos de pan, zoom e tap no canvas.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectCanvasGestures(
    panHabilitado: Boolean,
    zoomHabilitado: Boolean,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float, Offset) -> Unit,
    onTap: (Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var zoom = 1f
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPan = false

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (canceled) break

            if (event.type == PointerEventType.Release) {
                if (!pastTouchSlop && !lockedToPan) {
                    // Era um tap
                    onTap(down.position)
                }
                break
            }

            if (event.type == PointerEventType.Move) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    val centroidSize = event.calculateCentroidSize()
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = abs(panChange.x) + abs(panChange.y)

                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                        lockedToPan = panMotion > zoomMotion && panHabilitado
                    }
                }

                if (pastTouchSlop) {
                    if (zoomHabilitado && !lockedToPan) {
                        onZoom(zoomChange, event.calculateCentroid())
                    }
                    if (panHabilitado) {
                        onPan(panChange.x, panChange.y)
                    }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * Animação de zoom suave.
 */
@Composable
fun animateCanvasZoom(
    targetScale: Float,
    onScaleChange: (Float) -> Unit,
) {
    val animScale = remember { Animatable(1f) }

    LaunchedEffect(targetScale) {
        animScale.animateTo(
            targetValue = targetScale,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
        onScaleChange(animScale.value)
    }
}
