package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Streaming da Luna — por fases.
 *
 * 1. Raciocínio abre e cresce linha a linha
 * 2. Raciocínio recolhe
 * 3. Resposta revela **palavra a palavra** (blocos de código entram inteiros)
 */
sealed class LunaStreamEstado {
    data object Idle : LunaStreamEstado()

    data class Pesquisando(
        val run: LunaActionRun,
        val liveLabel: String,
    ) : LunaStreamEstado()

    data class Raciocinando(
        /** Raciocínio REAL (tokens de reasoning) — vira a caixa recolhível. */
        val parcial: String,
        val actionRun: LunaActionRun? = null,
        /** Rótulo de progresso (Analisando…/Escrevendo…) — só o texto do "Pensando…", não é raciocínio. */
        val fase: String = "",
    ) : LunaStreamEstado()

    data class Respondendo(
        val reasoning: String,
        val reasoningDuracao: String,
        val respostaParcial: String,
        val actionRun: LunaActionRun? = null,
    ) : LunaStreamEstado()
}

data class LunaStreamResultado(
    val reasoning: String,
    val reasoningDuracao: String,
    val resposta: String,
    val actionRun: LunaActionRun? = null,
    /** Imagens que a Luna desenhou neste turno (ferramenta `gerar_imagem`). */
    val imagensGeradas: List<ImagemGerada> = emptyList(),
    /** Pergunta que a Luna fez antes de agir (ferramenta `perguntar`) — efêmera, vira cartão de opções. */
    val pergunta: PerguntaLuna? = null,
    /** true = a "resposta" é um aviso de falha, não fala real da Luna (não persistir/nem virar memória). */
    val erro: Boolean = false,
    /** true = o turno foi recusado por cota (429 quota_exceeded) — a parede graciosa cuida do aviso; não pintar balão. */
    val cotaEsgotada: Boolean = false,
)

suspend fun executarStreamLuna(
    reasoningCompleto: String,
    respostaCompleta: String,
    onEstado: (LunaStreamEstado) -> Unit,
    actionRun: LunaActionRun? = null,
): LunaStreamResultado {
    val linhasReasoning = reasoningCompleto
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val tokens = fatiarTokensStream(respostaCompleta)

    onEstado(LunaStreamEstado.Raciocinando("", actionRun = actionRun))
    delay(280)

    val reasoningBuf = StringBuilder()
    linhasReasoning.forEachIndexed { i, linha ->
        if (reasoningBuf.isNotEmpty()) reasoningBuf.append('\n')
        reasoningBuf.append(linha)
        onEstado(LunaStreamEstado.Raciocinando(reasoningBuf.toString(), actionRun = actionRun))
        delay(if (i == 0) 380L else 320L)
    }

    val reasoningFinal = reasoningBuf.toString().ifBlank { reasoningCompleto }
    val duracaoSeg = ((linhasReasoning.size * 0.32) + 0.6).coerceAtLeast(0.8)
    val duracaoLabel = "${duracaoSeg.roundToInt()}s"

    delay(220)

    val respostaBuf = StringBuilder()
    var lastEmitMs = 0L
    tokens.forEachIndexed { index, token ->
        respostaBuf.append(token)
        val agora = android.os.SystemClock.uptimeMillis()
        val ultimo = index == tokens.lastIndex
        // Throttle de UI (~40ms) na fase Respondendo — menos recomposes no markdown
        if (ultimo || agora - lastEmitMs >= 40L) {
            onEstado(
                LunaStreamEstado.Respondendo(
                    reasoning = reasoningFinal,
                    reasoningDuracao = duracaoLabel,
                    respostaParcial = respostaBuf.toString(),
                    actionRun = actionRun,
                ),
            )
            lastEmitMs = agora
        }
        delay(atrasoToken(token))
    }

    delay(80)

    return LunaStreamResultado(
        reasoning = reasoningFinal,
        reasoningDuracao = duracaoLabel,
        resposta = respostaCompleta,
        actionRun = actionRun,
    )
}

/** Demo: anima fases da pesquisa profunda e depois o relatório. */
suspend fun executarStreamPesquisaProfunda(
    onEstado: (LunaStreamEstado) -> Unit,
): LunaStreamResultado {
    val snapshots = demoResearchSnapshots()
    snapshots.dropLast(1).forEach { (live, run) ->
        onEstado(
            LunaStreamEstado.Pesquisando(
                run = run.toDeepResearchActionRun(),
                liveLabel = live,
            ),
        )
        delay(520L)
    }
    val runFinal = snapshots.last().second.toDeepResearchActionRun()
    onEstado(LunaStreamEstado.Pesquisando(run = runFinal, liveLabel = "Relatório pronto"))
    delay(280L)

    return executarStreamLuna(
        reasoningCompleto = demoReasoningPesquisaProfunda(),
        respostaCompleta = demoMarkdownPesquisaProfunda(),
        onEstado = onEstado,
        actionRun = runFinal,
    )
}

/** Demo: tools (imagem/vídeo/arquivo) — strip, sem painel de pesquisa. */
suspend fun executarStreamFerramentas(
    anexos: List<ComposerAttachment>,
    onEstado: (LunaStreamEstado) -> Unit,
): LunaStreamResultado {
    val wire = anexos.map { att ->
        val ferramenta = when (att.kind) {
            AttachmentKind.IMAGE -> "ver_imagem"
            AttachmentKind.VIDEO -> "ver_video"
            AttachmentKind.FILE -> "consultar_atlas"
        }
        WireToolStep(ferramenta = ferramenta, argumento = att.name)
    }
    val running = buildActionRunFromWire(
        id = "tools-live",
        title = "Ferramentas",
        wireSteps = wire,
        status = LunaActionRunStatus.RUNNING,
    ).copy(
        steps = wire.mapIndexed { i, w ->
            val meta = toolMeta(w.ferramenta)
            LunaActionStep(
                id = "t-$i",
                label = meta.live(w.argumento),
                status = LunaActionStepStatus.RUNNING,
                kind = meta.kind,
                ferramenta = w.ferramenta,
            )
        },
    )
    onEstado(LunaStreamEstado.Pesquisando(run = running, liveLabel = "Usando ferramentas"))
    delay(700L)

    val done = buildActionRunFromWire(
        id = "tools-done",
        title = "Ferramentas",
        wireSteps = wire,
        status = LunaActionRunStatus.DONE,
    )
    val nomes = anexos.joinToString(", ") { it.name }
    return executarStreamLuna(
        reasoningCompleto = "Recebi ${anexos.size} anexo(s): $nomes.\nVou resumir o que dá pra inferir.",
        respostaCompleta = """
Recebi **${anexos.size}** anexo${if (anexos.size == 1) "" else "s"}: $nomes.

A Luna usou as ferramentas dela (olhar imagem, assistir vídeo, ler arquivo) — o strip acima mostra isso, separado da pesquisa profunda na web.
        """.trimIndent(),
        onEstado = onEstado,
        actionRun = done,
    )
}

/**
 * Tokens pra stream: palavras + espaços/quebras.
 * Fences ```…``` entram de uma vez (Markdown quebrado no meio fica feio).
 */
internal fun fatiarTokensStream(texto: String): List<String> {
    val out = mutableListOf<String>()
    var i = 0
    while (i < texto.length) {
        if (texto.startsWith("```", i)) {
            val fecha = texto.indexOf("```", i + 3)
            val fim = if (fecha < 0) texto.length else fecha + 3
            var chunk = texto.substring(i, fim)
            i = fim
            if (i < texto.length && texto[i] == '\n') {
                chunk += '\n'
                i++
            }
            out += chunk
            continue
        }

        when {
            texto[i] == '\n' -> {
                var j = i
                while (j < texto.length && texto[j] == '\n') j++
                out += texto.substring(i, j)
                i = j
            }
            texto[i].isWhitespace() -> {
                var j = i
                while (j < texto.length && texto[j].isWhitespace() && texto[j] != '\n') j++
                out += texto.substring(i, j)
                i = j
            }
            else -> {
                var j = i
                while (j < texto.length && !texto[j].isWhitespace()) j++
                out += texto.substring(i, j)
                i = j
            }
        }
    }
    return out
}

private fun atrasoToken(token: String): Long = when {
    token.startsWith("```") -> 280L
    token.all { it == '\n' } -> if (token.length >= 2) 90L else 45L
    token.isBlank() -> 12L
    token.last() in ".:;!?" -> 52L
    token.length <= 2 -> 22L
    else -> 30L
}

@Composable
fun LunaStreamDraft(
    estado: LunaStreamEstado,
    modifier: Modifier = Modifier,
) {
    when (estado) {
        is LunaStreamEstado.Idle -> Unit
        is LunaStreamEstado.Pesquisando -> {
            Column(modifier.fillMaxWidth(0.92f)) {
                LunaActionTimeline(
                    run = estado.run,
                    liveLabel = estado.liveLabel,
                    inicialmenteAberto = true,
                )
            }
        }
        is LunaStreamEstado.Raciocinando -> {
            val mostrarRaciocinio by PrefsRepository.reasoningEnabled.collectAsState()
            // Pesquisa profunda: a Luna trabalha DENTRO do dossiê — a timeline viva no corpo
            // vai acendendo os passos. É o mesmo card que depois vira o relatório, então não
            // há troca brusca de visual quando a resposta chega.
            val dossie = estado.actionRun?.takeIf { it.isDeepResearch() }?.toLegacyResearchRun()
            if (dossie != null) {
                Column(modifier.fillMaxWidth(0.96f)) {
                    estado.actionRun?.plano?.takeIf { it.isNotEmpty() }?.let { plano ->
                        LunaPlanChecklist(
                            plano = plano,
                            aoVivo = estado.actionRun.status == LunaActionRunStatus.RUNNING,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    LunaDossieCard(
                        run = dossie,
                        resposta = "",
                        streaming = true,
                        liveLabel = estado.fase.ifBlank { null },
                        inicialmenteAberto = false,
                    )
                }
            } else {
                // Desenhando/editando uma imagem AGORA: o quadrado-fantasma toma o lugar do
                // "Pensando…" — é ele o indicador de progresso enquanto a imagem não chega.
                val labelImagem = estado.actionRun?.imagemEmGeracao()
                Column(modifier.fillMaxWidth(0.92f)) {
                    estado.actionRun?.let { run ->
                        LunaActionTimeline(run = run, inicialmenteAberto = false)
                        Spacer(Modifier.height(8.dp))
                    }
                    when {
                        labelImagem != null -> {
                            LunaImagePlaceholderCard(
                                label = labelImagem,
                                modifier = Modifier.align(Alignment.Start),
                            )
                        }
                        mostrarRaciocinio && estado.parcial.isNotBlank() -> {
                            Column(modifier.fillMaxWidth(0.85f)) {
                                LunaReasoning(
                                    texto = estado.parcial,
                                    inicialmenteAberto = true,
                                    expandidoControlado = true,
                                    clicavel = false,
                                    animarTamanho = false,
                                )
                            }
                        }
                        // Sempre mostra o indicador enquanto espera o primeiro token — com o
                        // rótulo da fase (Analisando…/Escrevendo…) quando o servidor manda, senão
                        // “Pensando…”. A caixa de raciocínio só aparece com raciocínio de verdade.
                        else -> LunaReasoningPensando(label = estado.fase.ifBlank { "Pensando…" })
                    }
                }
            }
        }
        is LunaStreamEstado.Respondendo -> {
            StreamRespostaDraft(
                reasoning = estado.reasoning,
                reasoningDuracao = estado.reasoningDuracao,
                respostaParcial = estado.respostaParcial,
                actionRun = estado.actionRun,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun StreamRespostaDraft(
    reasoning: String,
    reasoningDuracao: String,
    respostaParcial: String,
    actionRun: LunaActionRun? = null,
    modifier: Modifier = Modifier,
) {
    val mostrarRaciocinio by PrefsRepository.reasoningEnabled.collectAsState()

    // Pesquisa profunda: a resposta se forma DENTRO do dossiê (mesma cara de quando
    // assenta no histórico), não numa bolha que depois vira card.
    val dossie = actionRun?.takeIf { it.isDeepResearch() }?.toLegacyResearchRun()
    if (dossie != null && respostaParcial.isNotBlank()) {
        BoxWithConstraints(modifier.fillMaxWidth()) {
            Column(Modifier.widthIn(max = maxWidth * 0.96f)) {
                actionRun.plano.takeIf { it.isNotEmpty() }?.let { plano ->
                    LunaPlanChecklist(
                        plano = plano,
                        aoVivo = actionRun.status == LunaActionRunStatus.RUNNING,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                LunaDossieCard(
                    run = dossie,
                    resposta = respostaParcial,
                    streaming = true,
                    inicialmenteAberto = false,
                )
            }
        }
        return
    }

    Box(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            actionRun?.let { run ->
                LunaActionTimeline(run = run, inicialmenteAberto = false)
                Spacer(Modifier.height(8.dp))
            }
            if (mostrarRaciocinio && reasoning.isNotBlank()) {
                LunaReasoning(
                    texto = reasoning,
                    duracaoLabel = reasoningDuracao,
                    inicialmenteAberto = false,
                    expandidoControlado = false,
                    clicavel = true,
                    animarTamanho = false,
                )
                Spacer(Modifier.height(8.dp))
            }
            // Enquanto digita, a Luna já aparece em largura total, texto solto (sem bolha) —
            // pra não haver pulo visual quando a resposta assenta no histórico.
            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    LunaMarkdown(content = respostaParcial)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StreamCaret()
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamCaret() {
    val infinite = rememberInfiniteTransition(label = "caret")
    val a by infinite.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "caretA",
    )
    Box(
        Modifier
            .width(2.dp)
            .height(13.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(OrbitTokens.accent.copy(alpha = a)),
    )
}
