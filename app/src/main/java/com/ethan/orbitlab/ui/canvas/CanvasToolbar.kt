package com.ethan.orbitlab.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.canvas.ElementoBounds
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType

/**
 * Toolbar do canvas — controles para adicionar elementos e zoom.
 */
@Composable
fun CanvasToolbar(
    onAdicionarCard: () -> Unit,
    onAdicionarShape: () -> Unit,
    onAdicionarTexto: () -> Unit,
    onAdicionarImagem: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFitToScreen: () -> Unit,
    zoomLevel: Float = 1f,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(OrbitTokens.graphiteSurf.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(OrbitMetrics.radiusSm),
        color = Color.Transparent,
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Grupo: Adicionar
            ToolbarSection(label = "Adicionar") {
                ToolbarButton(
                    label = "Card",
                    icon = Icons.Default.Add,
                    onClick = onAdicionarCard,
                )
                ToolbarButton(
                    label = "Shape",
                    icon = Icons.Default.Add,
                    onClick = onAdicionarShape,
                )
                ToolbarButton(
                    label = "Texto",
                    icon = Icons.Default.TextFields,
                    onClick = onAdicionarTexto,
                )
                ToolbarButton(
                    label = "Imagem",
                    icon = Icons.Default.Image,
                    onClick = onAdicionarImagem,
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(OrbitTokens.border.copy(alpha = 0.3f)),
            )

            // Grupo: Zoom
            ToolbarSection(label = "Zoom") {
                ToolbarButton(
                    label = "+",
                    icon = Icons.Default.ZoomIn,
                    onClick = onZoomIn,
                )
                Text(
                    text = "${(zoomLevel * 100).toInt()}%",
                    style = OrbitType.Body.SM,
                    color = OrbitTokens.textMid,
                    modifier = Modifier.width(48.dp),
                )
                ToolbarButton(
                    label = "-",
                    icon = Icons.Default.ZoomOut,
                    onClick = onZoomOut,
                )
                ToolbarButton(
                    label = "Fit",
                    icon = Icons.Default.ZoomOut,
                    onClick = onFitToScreen,
                )
            }
        }
    }
}

@Composable
private fun ToolbarSection(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = OrbitType.Body.XXS,
            color = OrbitTokens.textLow,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(OrbitMetrics.radiusXs))
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = OrbitTokens.textMid,
                )
            } else {
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrbitTokens.textMid,
                )
            }
        }
    }
}

/**
 * Indicador de seleção de elemento.
 */
@Composable
fun SelectionBar(
    elementoNome: String,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(OrbitTokens.graphiteSurf.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(OrbitMetrics.radiusSm),
        color = Color.Transparent,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = elementoNome,
                style = OrbitType.Body.SM,
                color = OrbitTokens.textHiN,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onBringToFront,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FlipToFront,
                    contentDescription = "Trazer para frente",
                    modifier = Modifier.size(16.dp),
                    tint = OrbitTokens.textMid,
                )
            }
            IconButton(
                onClick = onSendToBack,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FlipToBack,
                    contentDescription = "Enviar para trás",
                    modifier = Modifier.size(16.dp),
                    tint = OrbitTokens.textMid,
                )
            }
            IconButton(
                onClick = onDuplicate,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Duplicar",
                    modifier = Modifier.size(16.dp),
                    tint = OrbitTokens.textMid,
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir",
                    modifier = Modifier.size(16.dp),
                    tint = OrbitTokens.danger,
                )
            }
        }
    }
}

/**
 * Mini-mapa do canvas (overview).
 */
@Composable
fun CanvasMinimap(
    elementos: Map<String, com.ethan.orbitlab.data.canvas.CanvasElement>,
    viewportBounds: ElementoBounds,
    onNavigate: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val minimapWidth = 120.dp
    val minimapHeight = 80.dp
    val canvasWidth = 2000f
    val canvasHeight = 2000f

    Surface(
        modifier = modifier
            .size(minimapWidth, minimapHeight)
            .clip(RoundedCornerShape(OrbitMetrics.radiusXs)),
        color = OrbitTokens.ink0.copy(alpha = 0.9f),
        shape = RoundedCornerShape(OrbitMetrics.radiusXs),
    ) {
        Box {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val x = (down.position.x / minimapWidth.toPx()) * canvasWidth
                            val y = (down.position.y / minimapHeight.toPx()) * canvasHeight
                            onNavigate(x, y)
                        }
                    }
            ) {
                // Desenha elementos como retângulos pequenos
                elementos.values.forEach { el ->
                    val x = (el.bounds.x / canvasWidth) * size.width
                    val y = (el.bounds.y / canvasHeight) * size.height
                    val w = (el.bounds.width / canvasWidth) * size.width
                    val h = (el.bounds.height / canvasHeight) * size.height
                    drawRect(
                        color = OrbitTokens.accent.copy(alpha = 0.6f),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(w.coerceAtLeast(2f), h.coerceAtLeast(2f)),
                    )
                }

                // Viewport atual
                val vpX = (viewportBounds.x / canvasWidth) * size.width
                val vpY = (viewportBounds.y / canvasHeight) * size.height
                val vpW = (viewportBounds.width / canvasWidth) * size.width
                val vpH = (viewportBounds.height / canvasHeight) * size.height
                drawRect(
                    color = OrbitTokens.accentText,
                    topLeft = Offset(vpX, vpY),
                    size = androidx.compose.ui.geometry.Size(vpW, vpH),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
                )
            }
        }
    }
}
