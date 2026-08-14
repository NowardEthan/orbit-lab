package com.ethan.orbitlab.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlignHorizontalCenter
import androidx.compose.material.icons.filled.AlignHorizontalLeft
import androidx.compose.material.icons.filled.AlignHorizontalRight
import androidx.compose.material.icons.filled.AlignVerticalBottom
import androidx.compose.material.icons.filled.AlignVerticalCenter
import androidx.compose.material.icons.filled.AlignVerticalTop
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.data.canvas.AlinhamentoEixo
import com.ethan.orbitlab.data.canvas.AlinhamentoRef
import com.ethan.orbitlab.data.canvas.CanvasElement
import com.ethan.orbitlab.data.canvas.ElementoBounds
import com.ethan.orbitlab.data.canvas.LayoutEstrategia
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType
import kotlin.math.max
import kotlin.math.min

/**
 * Ferramentas de layout para o canvas.
 */
object LayoutTools {

    /**
     * Calcula auto-layout para elementos.
     */
    fun calcularLayout(
        elementos: List<CanvasElement>,
        estrategia: LayoutEstrategia,
        gap: Dp = 24.dp,
        startX: Float = 0f,
        startY: Float = 0f,
    ): Map<String, ElementoBounds> {
        if (elementos.isEmpty()) return emptyMap()

        val resultado = mutableMapOf<String, ElementoBounds>()
        val gapPx = gap.value.toFloat()

        when (estrategia) {
            LayoutEstrategia.grid -> {
                val cols = kotlin.math.ceil(kotlin.math.sqrt(elementos.size.toDouble())).toInt()
                elementos.forEachIndexed { i, el ->
                    val col = i % cols
                    val row = i / cols
                    resultado[el.id] = el.bounds.copy(
                        x = startX + col * (el.bounds.width + gapPx),
                        y = startY + row * (el.bounds.height + gapPx),
                    )
                }
            }
            LayoutEstrategia.vertical -> {
                var y = startY
                elementos.forEach { el ->
                    resultado[el.id] = el.bounds.copy(
                        x = el.bounds.x,
                        y = y,
                    )
                    y += el.bounds.height + gapPx
                }
            }
            LayoutEstrategia.horizontal -> {
                var x = startX
                elementos.forEach { el ->
                    resultado[el.id] = el.bounds.copy(
                        x = x,
                        y = el.bounds.y,
                    )
                    x += el.bounds.width + gapPx
                }
            }
            LayoutEstrategia.radial -> {
                val centerX = startX + 400
                val centerY = startY + 300
                val radius = maxOf(150f, elementos.size * 30f)
                elementos.forEachIndexed { i, el ->
                    val angle = (2 * kotlin.math.PI * i) / elementos.size - kotlin.math.PI / 2
                    resultado[el.id] = el.bounds.copy(
                        x = (centerX + radius * kotlin.math.cos(angle) - el.bounds.width / 2).toFloat(),
                        y = (centerY + radius * kotlin.math.sin(angle) - el.bounds.height / 2).toFloat(),
                    )
                }
            }
        }

        return resultado
    }

    /**
     * Alinha elementos em um eixo.
     */
    fun alinhar(
        elementos: List<CanvasElement>,
        eixo: AlinhamentoEixo,
        ref: AlinhamentoRef,
    ): Map<String, ElementoBounds> {
        if (elementos.isEmpty()) return emptyMap()

        val resultado = mutableMapOf<String, ElementoBounds>()

        if (eixo == AlinhamentoEixo.x) {
            val refs = elementos.map { el ->
                when (ref) {
                    AlinhamentoRef.left -> el.bounds.x
                    AlinhamentoRef.center -> el.bounds.x + el.bounds.width / 2
                    AlinhamentoRef.right -> el.bounds.x + el.bounds.width
                    else -> el.bounds.x
                }
            }

            val refValue = when (ref) {
                AlinhamentoRef.center -> (refs.minOrNull()!! + refs.maxOrNull()!!) / 2
                AlinhamentoRef.right -> refs.maxOrNull()!!
                else -> refs.minOrNull()!!
            }

            elementos.forEach { el ->
                val x = when (ref) {
                    AlinhamentoRef.left -> refValue
                    AlinhamentoRef.center -> refValue - el.bounds.width / 2
                    AlinhamentoRef.right -> refValue - el.bounds.width
                    else -> el.bounds.x
                }
                resultado[el.id] = el.bounds.copy(x = x)
            }
        } else {
            val refs = elementos.map { el ->
                when (ref) {
                    AlinhamentoRef.top -> el.bounds.y
                    AlinhamentoRef.middle -> el.bounds.y + el.bounds.height / 2
                    AlinhamentoRef.bottom -> el.bounds.y + el.bounds.height
                    else -> el.bounds.y
                }
            }

            val refValue = when (ref) {
                AlinhamentoRef.middle -> (refs.minOrNull()!! + refs.maxOrNull()!!) / 2
                AlinhamentoRef.bottom -> refs.maxOrNull()!!
                else -> refs.minOrNull()!!
            }

            elementos.forEach { el ->
                val y = when (ref) {
                    AlinhamentoRef.top -> refValue
                    AlinhamentoRef.middle -> refValue - el.bounds.height / 2
                    AlinhamentoRef.bottom -> refValue - el.bounds.height
                    else -> el.bounds.y
                }
                resultado[el.id] = el.bounds.copy(y = y)
            }
        }

        return resultado
    }

    /**
     * Distribui elementos igualmente.
     */
    fun distribuir(
        elementos: List<CanvasElement>,
        eixo: AlinhamentoEixo,
        gap: Dp = 24.dp,
    ): Map<String, ElementoBounds> {
        if (elementos.size < 2) {
            return elementos.associate { it.id to it.bounds }
        }

        val resultado = mutableMapOf<String, ElementoBounds>()
        val gapPx = gap.value.toFloat()

        // Ordenar por posição
        val ordenados = elementos.sortedBy { el ->
            if (eixo == AlinhamentoEixo.x) el.bounds.x else el.bounds.y
        }

        // Calcular tamanhos
        val tamanhos = ordenados.map { el ->
            if (eixo == AlinhamentoEixo.x) el.bounds.width else el.bounds.height
        }
        val totalTamanho = tamanhos.sum()
        val totalGap = gapPx * (ordenados.size - 1)

        // Espaço disponível
        val primeiro = ordenados.first()
        val ultimo = ordenados.last()
        val disponivel = if (eixo == AlinhamentoEixo.x) {
            ultimo.bounds.x + ultimo.bounds.width - primeiro.bounds.x
        } else {
            ultimo.bounds.y + ultimo.bounds.height - primeiro.bounds.y
        }

        val escala = if (totalTamanho + totalGap > 0) {
            (disponivel - totalGap) / (totalTamanho + totalGap)
        } else 1f

        var pos = if (eixo == AlinhamentoEixo.x) primeiro.bounds.x else primeiro.bounds.y

        ordenados.forEachIndexed { i, el ->
            val tamanho = tamanhos[i]
            if (eixo == AlinhamentoEixo.x) {
                resultado[el.id] = el.bounds.copy(
                    x = pos,
                    y = el.bounds.y,
                )
            } else {
                resultado[el.id] = el.bounds.copy(
                    x = el.bounds.x,
                    y = pos,
                )
            }
            pos += tamanho * escala + gapPx
        }

        return resultado
    }

    /**
     * Encaixa elemento em relação a outro.
     */
    fun encaixar(
        elemento: CanvasElement,
        referencia: CanvasElement,
        posicao: com.ethan.orbitlab.data.canvas.EncaixePosicao,
        gap: Dp = 8.dp,
    ): ElementoBounds {
        val ref = referencia.bounds
        val el = elemento.bounds
        val gapPx = gap.value.toFloat()

        return when (posicao) {
            com.ethan.orbitlab.data.canvas.EncaixePosicao.left -> el.copy(
                x = ref.x - el.width - gapPx,
                y = ref.y,
            )
            com.ethan.orbitlab.data.canvas.EncaixePosicao.right -> el.copy(
                x = ref.x + ref.width + gapPx,
                y = ref.y,
            )
            com.ethan.orbitlab.data.canvas.EncaixePosicao.above -> el.copy(
                x = ref.x,
                y = ref.y - el.height - gapPx,
            )
            com.ethan.orbitlab.data.canvas.EncaixePosicao.below -> el.copy(
                x = ref.x,
                y = ref.y + ref.height + gapPx,
            )
            com.ethan.orbitlab.data.canvas.EncaixePosicao.inside -> el.copy(
                x = ref.x + gapPx,
                y = ref.y + gapPx,
                width = max(ref.width - gapPx * 2, el.width),
                height = max(ref.height - gapPx * 2, el.height),
            )
        }
    }

    /**
     * Centraliza elementos na tela.
     */
    fun centralizar(
        elementos: List<CanvasElement>,
        canvasWidth: Float,
        canvasHeight: Float,
    ): Map<String, ElementoBounds> {
        if (elementos.isEmpty()) return emptyMap()

        // Bounding box de todos
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        elementos.forEach { el ->
            minX = min(minX, el.bounds.x)
            minY = min(minY, el.bounds.y)
            maxX = max(maxX, el.bounds.x + el.bounds.width)
            maxY = max(maxY, el.bounds.y + el.bounds.height)
        }

        val contentWidth = maxX - minX
        val contentHeight = maxY - minY

        val offsetX = (canvasWidth - contentWidth) / 2 - minX
        val offsetY = (canvasHeight - contentHeight) / 2 - minY

        return elementos.associate { el ->
            el.id to el.bounds.copy(
                x = el.bounds.x + offsetX,
                y = el.bounds.y + offsetY,
            )
        }
    }
}

/**
 * Barra de ferramentas de alinhamento.
 */
@Composable
fun AlignmentToolbar(
    onAlign: (AlinhamentoEixo, AlinhamentoRef) -> Unit,
    onDistribute: (String) -> Unit,
    onAutoLayout: (LayoutEstrategia) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(OrbitTokens.graphiteSurf.copy(alpha = 0.95f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Alinhamento horizontal
        Text(
            text = "Alinhar X",
            style = OrbitType.Body.XXS,
            color = OrbitTokens.textLow,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AlignmentButton(
                icon = Icons.Default.AlignHorizontalLeft,
                label = "Esquerda",
                onClick = { onAlign(AlinhamentoEixo.x, AlinhamentoRef.left) },
            )
            AlignmentButton(
                icon = Icons.Default.AlignHorizontalCenter,
                label = "Centro",
                onClick = { onAlign(AlinhamentoEixo.x, AlinhamentoRef.center) },
            )
            AlignmentButton(
                icon = Icons.Default.AlignHorizontalRight,
                label = "Direita",
                onClick = { onAlign(AlinhamentoEixo.x, AlinhamentoRef.right) },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Alinhamento vertical
        Text(
            text = "Alinhar Y",
            style = OrbitType.Body.XXS,
            color = OrbitTokens.textLow,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AlignmentButton(
                icon = Icons.Default.AlignVerticalTop,
                label = "Topo",
                onClick = { onAlign(AlinhamentoEixo.y, AlinhamentoRef.top) },
            )
            AlignmentButton(
                icon = Icons.Default.AlignVerticalCenter,
                label = "Centro",
                onClick = { onAlign(AlinhamentoEixo.y, AlinhamentoRef.middle) },
            )
            AlignmentButton(
                icon = Icons.Default.AlignVerticalBottom,
                label = "Baixo",
                onClick = { onAlign(AlinhamentoEixo.y, AlinhamentoRef.bottom) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Distribuir
        Text(
            text = "Distribuir",
            style = OrbitType.Body.XXS,
            color = OrbitTokens.textLow,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AlignmentButton(
                icon = Icons.Default.LinearScale,
                label = "Horizontal",
                onClick = { onDistribute("x") },
            )
            AlignmentButton(
                icon = Icons.Default.RadioButtonChecked,
                label = "Vertical",
                onClick = { onDistribute("y") },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Auto-layout
        Text(
            text = "Auto Layout",
            style = OrbitType.Body.XXS,
            color = OrbitTokens.textLow,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AlignmentButton(
                icon = Icons.Default.GridView,
                label = "Grid",
                onClick = { onAutoLayout(LayoutEstrategia.grid) },
            )
            AlignmentButton(
                icon = Icons.Default.SpaceBar,
                label = "Vertical",
                onClick = { onAutoLayout(LayoutEstrategia.vertical) },
            )
        }
    }
}

@Composable
private fun AlignmentButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = OrbitTokens.textMid,
            )
        }
    }
}

/**
 * Guias de alinhamento visuais.
 */
@Composable
fun AlignmentGuides(
    elementos: List<CanvasElement>,
    selectedIds: Set<String>,
    modifier: Modifier = Modifier,
) {
    if (selectedIds.size < 2) return

    val selecionados = elementos.filter { it.id in selectedIds }
    if (selecionados.size < 2) return

    // Calcular centro e extremos
    val centerX = selecionados.map { it.bounds.x + it.bounds.width / 2 }.average().toFloat()
    val centerY = selecionados.map { it.bounds.y + it.bounds.height / 2 }.average().toFloat()
    val leftX = selecionados.minOf { it.bounds.x }
    val rightX = selecionados.maxOf { it.bounds.x + it.bounds.width }
    val topY = selecionados.minOf { it.bounds.y }
    val bottomY = selecionados.maxOf { it.bounds.y + it.bounds.height }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Linha central X
                drawLine(
                    color = OrbitTokens.accent.copy(alpha = 0.5f),
                    start = Offset(centerX, topY - 20),
                    end = Offset(centerX, bottomY + 20),
                    strokeWidth = 1f,
                )
                // Linha central Y
                drawLine(
                    color = OrbitTokens.accent.copy(alpha = 0.5f),
                    start = Offset(leftX - 20, centerY),
                    end = Offset(rightX + 20, centerY),
                    strokeWidth = 1f,
                )
            },
    )
}

/**
 * Snap to guides visual.
 */
@Composable
fun SnapGuides(
    snapX: Float?,
    snapY: Float?,
    modifier: Modifier = Modifier,
) {
    if (snapX == null && snapY == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                snapX?.let { x ->
                    drawLine(
                        color = OrbitTokens.online.copy(alpha = 0.7f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 2f,
                    )
                }
                snapY?.let { y ->
                    drawLine(
                        color = OrbitTokens.online.copy(alpha = 0.7f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f,
                    )
                }
            },
    )
}

/**
 * Distância entre elementos para snap.
 */
object SnapEngine {
    private const val SNAP_THRESHOLD = 8f

    /**
     * Calcula snap para uma posição.
     */
    fun calcularSnap(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        outros: List<CanvasElement>,
        enabled: Boolean = true,
    ): Pair<Float?, Float?> {
        if (!enabled) return Pair(null, null)

        var snapX: Float? = null
        var snapY: Float? = null

        val left = x
        val right = x + width
        val top = y
        val bottom = y + height
        val centerX = x + width / 2
        val centerY = y + height / 2

        for (outro in outros) {
            val oLeft = outro.bounds.x
            val oRight = outro.bounds.x + outro.bounds.width
            val oCenterX = outro.bounds.x + outro.bounds.width / 2
            val oTop = outro.bounds.y
            val oBottom = outro.bounds.y + outro.bounds.height
            val oCenterY = outro.bounds.y + outro.bounds.height / 2

            // Snap horizontal
            if (kotlin.math.abs(left - oLeft) < SNAP_THRESHOLD) snapX = oLeft
            if (kotlin.math.abs(right - oRight) < SNAP_THRESHOLD) snapX = oRight
            if (kotlin.math.abs(centerX - oCenterX) < SNAP_THRESHOLD) snapX = oCenterX - width / 2
            if (kotlin.math.abs(left - oRight) < SNAP_THRESHOLD) snapX = oRight
            if (kotlin.math.abs(right - oLeft) < SNAP_THRESHOLD) snapX = oLeft - width

            // Snap vertical
            if (kotlin.math.abs(top - oTop) < SNAP_THRESHOLD) snapY = oTop
            if (kotlin.math.abs(bottom - oBottom) < SNAP_THRESHOLD) snapY = oBottom
            if (kotlin.math.abs(centerY - oCenterY) < SNAP_THRESHOLD) snapY = oCenterY - height / 2
            if (kotlin.math.abs(top - oBottom) < SNAP_THRESHOLD) snapY = oBottom
            if (kotlin.math.abs(bottom - oTop) < SNAP_THRESHOLD) snapY = oTop - height
        }

        return Pair(snapX, snapY)
    }
}
