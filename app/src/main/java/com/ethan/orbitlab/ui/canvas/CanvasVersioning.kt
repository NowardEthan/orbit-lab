package com.ethan.orbitlab.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.data.canvas.CanvasElement
import com.ethan.orbitlab.data.canvas.CanvasWorkspace
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType
import kotlinx.coroutines.delay

/**
 * Estado de uma versão snapshot.
 */
data class CanvasVersao(
    val numero: Int,
    val elementos: Map<String, CanvasElement>,
    val timestamp: Long = System.currentTimeMillis(),
    val autor: String = "luna",
    val delta: String? = null,
    val label: String? = null,
)

/**
 * Estado de undo/redo.
 */
class CanvasUndoState {
    private val _historico = mutableStateOf<List<CanvasVersao>>(emptyList())
    val historico get() = _historico.value

    private var _indiceAtual = mutableIntStateOf(-1)
    val indiceAtual get() = _indiceAtual.intValue

    private val _podeDesfazer = mutableStateOf(false)
    val podeDesfazer get() = _podeDesfazer.value

    private val _podeRefazer = mutableStateOf(false)
    val podeRefazer get() = _podeRefazer.value

    /**
     * Salva um snapshot do estado atual.
     */
    fun salvarSnapshot(
        elementos: Map<String, CanvasElement>,
        autor: String = "user",
        delta: String? = null,
        label: String? = null,
    ) {
        val numero = _historico.value.lastOrNull()?.numero?.plus(1) ?: 1
        val versao = CanvasVersao(
            numero = numero,
            elementos = elementos.toMap(),
            autor = autor,
            delta = delta,
            label = label,
        )

        // Remove versões futuras se estamos no meio do histórico
        val novoHistorico = if (_indiceAtual.intValue < _historico.value.size - 1) {
            _historico.value.take(_indiceAtual.intValue + 1) + versao
        } else {
            _historico.value + versao
        }

        // Limita histórico a 50 versões
        _historico.value = novoHistorico.takeLast(50)
        _indiceAtual.intValue = _historico.value.size - 1

        atualizarFlags()
    }

    /**
     * Desfaz a última ação.
     */
    fun desfazer(): Map<String, CanvasElement>? {
        if (_indiceAtual.intValue > 0) {
            _indiceAtual.intValue--
            atualizarFlags()
            return _historico.value[_indiceAtual.intValue].elementos
        }
        return null
    }

    /**
     * Refaz a última ação desfeita.
     */
    fun refazer(): Map<String, CanvasElement>? {
        if (_indiceAtual.intValue < _historico.value.size - 1) {
            _indiceAtual.intValue++
            atualizarFlags()
            return _historico.value[_indiceAtual.intValue].elementos
        }
        return null
    }

    /**
     * Limpa o histórico.
     */
    fun limpar() {
        _historico.value = emptyList()
        _indiceAtual.intValue = -1
        atualizarFlags()
    }

    private fun atualizarFlags() {
        _podeDesfazer.value = _indiceAtual.intValue > 0
        _podeRefazer.value = _indiceAtual.intValue < _historico.value.size - 1
    }
}

/**
 * Toolbar de undo/redo com histórico.
 */
@Composable
fun CanvasUndoToolbar(
    undoState: CanvasUndoState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val podeDesfazer = undoState.podeDesfazer
    val podeRefazer = undoState.podeRefazer
    val historico = undoState.historico
    val indiceAtual = undoState.indiceAtual

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(OrbitTokens.graphiteSurf.copy(alpha = 0.95f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Undo
        IconButton(
            onClick = onUndo,
            enabled = podeDesfazer,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Undo,
                contentDescription = "Desfazer",
                modifier = Modifier.size(20.dp),
                tint = OrbitTokens.textMid.copy(alpha = if (podeDesfazer) 1f else 0.3f),
            )
        }

        // Redo
        IconButton(
            onClick = onRedo,
            enabled = podeRefazer,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Redo,
                contentDescription = "Refazer",
                modifier = Modifier.size(20.dp),
                tint = OrbitTokens.textMid.copy(alpha = if (podeRefazer) 1f else 0.3f),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Indicador de posição no histórico
        if (historico.isNotEmpty()) {
            Text(
                text = "${indiceAtual + 1}/${historico.size}",
                style = OrbitType.Body.XXS,
                color = OrbitTokens.textLow,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Histórico
        IconButton(
            onClick = onShowHistory,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Histórico",
                modifier = Modifier.size(20.dp),
                tint = OrbitTokens.textMid,
            )
        }
    }
}

/**
 * Painel de histórico de versões.
 */
@Composable
fun CanvasHistoryPanel(
    versoes: List<CanvasVersao>,
    versaoAtual: Int,
    onRestaurar: (CanvasVersao) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusMd))
            .background(OrbitTokens.graphiteSurf)
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Histórico",
                style = OrbitType.Headline.SM,
                color = OrbitTokens.textHiN,
            )
            Text(
                text = "${versoes.size} versões",
                style = OrbitType.Body.XXS,
                color = OrbitTokens.textMid,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (versoes.isEmpty()) {
            Text(
                text = "Nenhuma versão salva",
                style = OrbitType.Body.MD,
                color = OrbitTokens.textLow,
                modifier = Modifier.padding(vertical = 32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(
                    items = versoes.reversed(),
                    key = { _, v -> v.numero },
                ) { index, versao ->
                    val isAtual = versao.numero == versaoAtual
                    VersionItem(
                        versao = versao,
                        isAtual = isAtual,
                        onClick = { onRestaurar(versao) },
                    )
                }
            }
        }
    }
}

/**
 * Item de versão no histórico.
 */
@Composable
private fun VersionItem(
    versao: CanvasVersao,
    isAtual: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isAtual) OrbitTokens.accent.copy(alpha = 0.1f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Indicador de versão atual
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isAtual) OrbitTokens.accent
                    else OrbitTokens.graphiteRaised
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isAtual) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = OrbitTokens.onBluePastel,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = versao.label ?: "Versão ${versao.numero}",
                style = OrbitType.Body.MD,
                color = if (isAtual) OrbitTokens.accentText else OrbitTokens.textHiN,
                fontWeight = if (isAtual) FontWeight.Medium else FontWeight.Normal,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = versao.autor,
                    style = OrbitType.Body.XXS,
                    color = OrbitTokens.textMid,
                )
                Text(
                    text = "·",
                    style = OrbitType.Body.XXS,
                    color = OrbitTokens.textLow,
                )
                Text(
                    text = formatarTempo(versao.timestamp),
                    style = OrbitType.Body.XXS,
                    color = OrbitTokens.textLow,
                )
            }
            versao.delta?.let { delta ->
                Text(
                    text = delta,
                    style = OrbitType.Body.XXS,
                    color = OrbitTokens.accent,
                )
            }
        }

        // Contagem de elementos
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(OrbitTokens.graphiteRaised)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = "${versao.elementos.size}",
                style = OrbitType.Body.XXS,
                color = OrbitTokens.textMid,
            )
        }
    }
}

/**
 * Formata timestamp para texto relativo.
 */
private fun formatarTempo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "agora"
        diff < 3_600_000 -> "${diff / 60_000}min"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> "${diff / 604_800_000}sem"
    }
}

/**
 * Criador automático de snapshots com debounce.
 */
class AutoSnapshot(
    private val onSnapshot: (Map<String, CanvasElement>, String?) -> Unit,
    private val delayMs: Long = 2000,
) {
    private var pendingSnapshot: Map<String, CanvasElement>? = null
    private var lastSnapshot: Map<String, CanvasElement>? = null

    /**
     * Agenda um snapshot se houver mudanças.
     */
    fun agendar(elementos: Map<String, CanvasElement>, label: String? = null) {
        pendingSnapshot = elementos
    }

    /**
     * Força um snapshot imediato (ex: antes de ação grande).
     */
    fun forcar(elementos: Map<String, CanvasElement>, label: String? = null) {
        lastSnapshot = elementos
        onSnapshot(elementos, label)
    }
}

/**
 * Diferencial entre dois estados de canvas.
 */
object CanvasDiff {

    /**
     * Calcula a diferença entre dois estados.
     */
    fun calcular(
        anterior: Map<String, CanvasElement>,
        atual: Map<String, CanvasElement>,
    ): DiffResultado {
        val adicionados = atual.keys - anterior.keys
        val removidos = anterior.keys - atual.keys
        val modificados = anterior.keys.intersect(atual.keys).filter { id ->
            anterior[id] != atual[id]
        }

        val acoes = mutableListOf<DiffAcao>()

        adicionados.forEach { id ->
            atual[id]?.let {
                acoes.add(DiffAcao.Adicionado(it))
            }
        }

        removidos.forEach { id ->
            anterior[id]?.let {
                acoes.add(DiffAcao.Removido(it))
            }
        }

        modificados.forEach { id ->
            val antes = anterior[id]!!
            val depois = atual[id]!!
            val delta = descreverMudanca(antes, depois)
            acoes.add(DiffAcao.Modificado(antes, depois, delta))
        }

        return DiffResultado(
            acoes = acoes,
            adicionados = adicionados.size,
            removidos = removidos.size,
            modificados = modificados.size,
        )
    }

    private fun descreverMudanca(antes: CanvasElement, depois: CanvasElement): String {
        val partes = mutableListOf<String>()

        if (antes.bounds != depois.bounds) {
            partes.add("posição")
        }
        if (antes.style != depois.style) {
            partes.add("estilo")
        }
        if (antes.tipo != depois.tipo) {
            partes.add("tipo")
        }
        if (antes.textProps != depois.textProps) {
            partes.add("texto")
        }

        return if (partes.isEmpty()) "modificado"
        else partes.joinToString(", ")
    }
}

/**
 * Resultado de diff.
 */
data class DiffResultado(
    val acoes: List<DiffAcao>,
    val adicionados: Int,
    val removidos: Int,
    val modificados: Int,
) {
    val isVazio: Boolean get() = adicionados == 0 && removidos == 0 && modificados == 0

    fun paraLabel(): String {
        return buildString {
            if (adicionados > 0) append("+$adicionados ")
            if (modificados > 0) append("~$modificados ")
            if (removidos > 0) append("-$removidos")
        }.trim()
    }
}

/**
 * Ação de diff.
 */
sealed class DiffAcao {
    data class Adicionado(val elemento: CanvasElement) : DiffAcao()
    data class Removido(val elemento: CanvasElement) : DiffAcao()
    data class Modificado(
        val antes: CanvasElement,
        val depois: CanvasElement,
        val delta: String,
    ) : DiffAcao()
}
