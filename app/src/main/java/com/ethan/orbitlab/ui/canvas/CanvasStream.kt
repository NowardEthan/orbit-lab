package com.ethan.orbitlab.ui.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType
import kotlinx.coroutines.delay
import org.json.JSONObject

/**
 * Tipo de ação da Luna no canvas.
 */
enum class CanvasAcaoTipo(
    val icone: ImageVector,
    val label: String,
) {
    ADICIONAR(Icons.Default.Add, "Adicionando"),
    MOVER(Icons.Default.OpenWith, "Movendo"),
    REDIMENSIONAR(Icons.Default.Transform, "Redimensionando"),
    ESTILIZAR(Icons.Default.Palette, "Estilizando"),
    DELETAR(Icons.Default.Delete, "Removendo"),
    CRIAR(Icons.Default.Create, "Criando"),
    ALINHAR(Icons.Default.MoveUp, "Alinhando"),
    GROUP(Icons.Default.Add, "Agrupando"),
    ;

    companion object {
        fun from(tipo: String): CanvasAcaoTipo {
            return entries.find { it.name.equals(tipo, ignoreCase = true) } ?: MOVER
        }
    }
}

/**
 * Status de uma ação.
 */
enum class AcaoStatus {
    PENDENTE,
    EM_ANDAMENTO,
    CONCLUIDA,
    ERRO,
}

/**
 * Ação da Luna no canvas.
 */
data class CanvasAcao(
    val id: String,
    val tipo: CanvasAcaoTipo,
    val elementoId: String,
    val elementoNome: String,
    val detalhes: String = "",
    val status: AcaoStatus = AcaoStatus.PENDENTE,
    val duracaoMs: Long = 300,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val isCompleta: Boolean get() = status == AcaoStatus.CONCLUIDA || status == AcaoStatus.ERRO
}

/**
 * Estado do stream de ações.
 */
class CanvasStreamState {
    private val _acoes = mutableStateOf<List<CanvasAcao>>(emptyList())
    val acoes get() = _acoes.value

    fun adicionar(acao: CanvasAcao) {
        _acoes.value = _acoes.value + acao
    }

    fun atualizarStatus(id: String, status: AcaoStatus) {
        _acoes.value = _acoes.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
    }

    fun limpar() {
        _acoes.value = emptyList()
    }

    fun removerAntigas(limiteMs: Long = 60_000) {
        val agora = System.currentTimeMillis()
        _acoes.value = _acoes.value.filter {
            !it.isCompleta || (agora - it.timestamp) < limiteMs
        }
    }
}

/**
 * Timeline de ações da Luna.
 */
@Composable
fun LunaCanvasTimeline(
    acoes: List<CanvasAcao>,
    expanded: Boolean = true,
    onCollapse: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = expanded,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(OrbitMetrics.radiusMd))
                .background(OrbitTokens.graphiteSurf.copy(alpha = 0.95f))
                .padding(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(OrbitTokens.online),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Luna",
                        style = OrbitType.Body.SM,
                        fontWeight = FontWeight.Medium,
                        color = OrbitTokens.textHiN,
                    )
                }
                Text(
                    text = "${acoes.size} ações",
                    style = OrbitType.Body.XXS,
                    color = OrbitTokens.textMid,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (acoes.isEmpty()) {
                Text(
                    text = "Aguardando ações...",
                    style = OrbitType.Body.SM,
                    color = OrbitTokens.textLow,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height((acoes.size.coerceAtMost(5) * 48).dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(
                        items = acoes.takeLast(10),
                        key = { _, acao -> acao.id },
                    ) { index, acao ->
                        TimelineItem(acao = acao)
                    }
                }
            }
        }
    }
}

/**
 * Item da timeline.
 */
@Composable
private fun TimelineItem(acao: CanvasAcao) {
    var showAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(acao.id) {
        if (acao.status == AcaoStatus.EM_ANDAMENTO) {
            showAnimation = true
            delay(acao.duracaoMs)
            showAnimation = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (showAnimation) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusXs))
            .background(
                when (acao.status) {
                    AcaoStatus.PENDENTE -> OrbitTokens.graphiteRaised.copy(alpha = 0.5f)
                    AcaoStatus.EM_ANDAMENTO -> OrbitTokens.accent.copy(alpha = 0.15f)
                    AcaoStatus.CONCLUIDA -> OrbitTokens.online.copy(alpha = 0.1f)
                    AcaoStatus.ERRO -> OrbitTokens.danger.copy(alpha = 0.1f)
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .scale(scale),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ícone de status
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (acao.status) {
                AcaoStatus.PENDENTE -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = OrbitTokens.textLow,
                    )
                }
                AcaoStatus.EM_ANDAMENTO -> {
                    Icon(
                        imageVector = acao.tipo.icone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = OrbitTokens.accent,
                    )
                }
                AcaoStatus.CONCLUIDA -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = OrbitTokens.online,
                    )
                }
                AcaoStatus.ERRO -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = OrbitTokens.danger,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Texto
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${acao.tipo.label} ${acao.elementoNome}",
                style = OrbitType.Body.XXS,
                color = when (acao.status) {
                    AcaoStatus.EM_ANDAMENTO -> OrbitTokens.textHiN
                    AcaoStatus.CONCLUIDA -> OrbitTokens.textMid
                    else -> OrbitTokens.textLow
                },
                fontWeight = if (acao.status == AcaoStatus.EM_ANDAMENTO) FontWeight.Medium else FontWeight.Normal,
            )
            if (acao.detalhes.isNotBlank()) {
                Text(
                    text = acao.detalhes,
                    style = OrbitType.Body.XXS,
                    color = OrbitTokens.textLow,
                )
            }
        }

        // Indicador de progresso animado
        if (acao.status == AcaoStatus.EM_ANDAMENTO) {
            val progress = remember { Animatable(0f) }
            LaunchedEffect(acao.id) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = acao.duracaoMs.toInt(),
                        easing = LinearEasing,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.accent.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    OrbitTokens.accent,
                                    OrbitTokens.accent.copy(alpha = 0f),
                                ),
                            ),
                        )
                        .scale(progress.value * 1.5f),
                )
            }
        }
    }
}

/**
 * Barra de narração da Luna no canvas.
 */
@Composable
fun LunaNarrationBar(
    mensagem: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && mensagem.isNotBlank(),
        enter = slideInHorizontally { -it } + fadeIn(),
        exit = slideOutHorizontally { -it } + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
                .background(OrbitTokens.bubbleLuna.copy(alpha = 0.95f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.online),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mensagem,
                style = OrbitType.Body.SM,
                color = OrbitTokens.textHiN,
            )
        }
    }
}

/**
 * Provider de stream de ações para o canvas.
 */
class CanvasStreamProvider(
    private val onAcao: (CanvasAcao) -> Unit,
) {
    private var sequencia = 0

    fun criarAcao(
        tipo: CanvasAcaoTipo,
        elementoId: String,
        elementoNome: String,
        detalhes: String = "",
        duracaoMs: Long = 300,
    ): CanvasAcao {
        sequencia++
        return CanvasAcao(
            id = "acao_${System.currentTimeMillis()}_$sequencia",
            tipo = tipo,
            elementoId = elementoId,
            elementoNome = elementoNome,
            detalhes = detalhes,
            status = AcaoStatus.PENDENTE,
            duracaoMs = duracaoMs,
        )
    }

    fun iniciar(acao: CanvasAcao) {
        onAcao(acao.copy(status = AcaoStatus.EM_ANDAMENTO))
    }

    fun concluir(acao: CanvasAcao) {
        onAcao(acao.copy(status = AcaoStatus.CONCLUIDA))
    }

    fun erro(acao: CanvasAcao, mensagem: String) {
        onAcao(acao.copy(status = AcaoStatus.ERRO, detalhes = mensagem))
    }
}

/**
 * Helper para criar ações comuns.
 */
object CanvasAcoes {

    fun adicionar(elementoNome: String, tipo: String = "elemento"): CanvasStreamProvider.() -> CanvasAcao = {
        criarAcao(
            tipo = CanvasAcaoTipo.ADICIONAR,
            elementoId = "",
            elementoNome = elementoNome,
            detalhes = "Criando $tipo",
            duracaoMs = 400,
        )
    }

    fun mover(elementoNome: String, deX: Float, deY: Float, paraX: Float, paraY: Float): CanvasStreamProvider.() -> CanvasAcao = {
        criarAcao(
            tipo = CanvasAcaoTipo.MOVER,
            elementoId = "",
            elementoNome = elementoNome,
            detalhes = "(${deX.toInt()}, ${deY.toInt()}) → (${paraX.toInt()}, ${paraY.toInt()})",
            duracaoMs = 300,
        )
    }

    fun estilizar(elementoNome: String, propriedade: String): CanvasStreamProvider.() -> CanvasAcao = {
        criarAcao(
            tipo = CanvasAcaoTipo.ESTILIZAR,
            elementoId = "",
            elementoNome = elementoNome,
            detalhes = propriedade,
            duracaoMs = 200,
        )
    }

    fun alinhar(elementoNome: String, alinhamento: String): CanvasStreamProvider.() -> CanvasAcao = {
        criarAcao(
            tipo = CanvasAcaoTipo.ALINHAR,
            elementoId = "",
            elementoNome = elementoNome,
            detalhes = "Alinhando: $alinhamento",
            duracaoMs = 250,
        )
    }

    fun deletar(elementoNome: String): CanvasStreamProvider.() -> CanvasAcao = {
        criarAcao(
            tipo = CanvasAcaoTipo.DELETAR,
            elementoId = "",
            elementoNome = elementoNome,
            detalhes = "Removendo do canvas",
            duracaoMs = 200,
        )
    }
}

/**
 * Evento SSE de ação no canvas (para integração com o backend).
 */
data class CanvasSSEEvent(
    val tipo: String,
    val acao: String,
    val elementoId: String,
    val de: Posicao? = null,
    val para: Posicao? = null,
    val duracaoMs: Long = 300,
    val props: Map<String, Any?> = emptyMap(),
) {
    data class Posicao(val x: Float, val y: Float)
}

/**
 * Parser de eventos SSE do canvas.
 */
object CanvasSSEParser {

    fun parse(json: String): Result<CanvasSSEEvent> {
        return runCatching {
            val root = JSONObject(json)

            CanvasSSEEvent(
                tipo = root.optString("tipo", "canvas_acao"),
                acao = root.optString("acao", ""),
                elementoId = root.optString("elementoId", ""),
                de = root.optPosicao("de") ?: root.optPosicaoFlat("de"),
                para = root.optPosicao("para") ?: root.optPosicaoFlat("para"),
                duracaoMs = root.optLong("duracao_ms", 300L),
            )
        }
    }

    private fun JSONObject.optPosicao(key: String): CanvasSSEEvent.Posicao? {
        val obj = optJSONObject(key) ?: return null
        if (!obj.has("x") || !obj.has("y")) return null
        return CanvasSSEEvent.Posicao(
            x = obj.optDouble("x").toFloat(),
            y = obj.optDouble("y").toFloat(),
        )
    }

    private fun JSONObject.optPosicaoFlat(prefix: String): CanvasSSEEvent.Posicao? {
        val xKey = "${prefix}_x"
        val yKey = "${prefix}_y"
        if (!has(xKey) || !has(yKey)) return null
        return CanvasSSEEvent.Posicao(
            x = optDouble(xKey).toFloat(),
            y = optDouble(yKey).toFloat(),
        )
    }

    /**
     * Serializa evento para SSE.
     */
    fun serialize(event: CanvasSSEEvent): String {
        val deJson = event.de?.let { """{"x":${it.x},"y":${it.y}}""" } ?: "null"
        val paraJson = event.para?.let { """{"x":${it.x},"y":${it.y}}""" } ?: "null"

        return """{"tipo":"${event.tipo}","acao":"${event.acao}","elementoId":"${event.elementoId}","de":$deJson,"para":$paraJson,"duracao_ms":${event.duracaoMs}}"""
    }
}
