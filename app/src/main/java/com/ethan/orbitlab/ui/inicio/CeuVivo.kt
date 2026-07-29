package com.ethan.orbitlab.ui.inicio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.sin

/**
 * "Céu Vivo" — o fundo do clima que muda com o CÉU (código WMO) e com a HORA do dia, e respira.
 *
 * A ideia: em vez de um gradiente fixo por tempo, a mesma cena ganha uma paleta viva —
 * o amanhecer é rosa-âmbar, a tarde limpa é azul quente, a noite é índigo com luz de lua;
 * a chuva esfria pra azul-petróleo, a tempestade puxa o violeta Penumbra. Por cima, um brilho
 * (sol/lua) no canto que pulsa devagar, e uma atmosfera leve (fios de chuva, flocos, estrelas)
 * só quando faz sentido. Tudo em UM Canvas com duas animações baratas — nada de partícula cara.
 *
 * As paletas ficam escuras embaixo de propósito: é onde mora o texto branco do card.
 */

enum class TipoCeu { LIMPO, NUBLADO, CHUVA, NEVE, NEBLINA, TEMPESTADE }

/** Paleta pronta pra um momento: 3 paradas do gradiente (topo→base), o brilho e o tipo. */
data class PaletaCeu(
    val stops: List<Color>,
    val brilho: Color,
    val tipo: TipoCeu,
    val noite: Boolean,
)

private fun classificar(codigo: Int?): TipoCeu = when (codigo) {
    null -> TipoCeu.NUBLADO
    0, 1 -> TipoCeu.LIMPO
    2, 3 -> TipoCeu.NUBLADO
    45, 48 -> TipoCeu.NEBLINA
    in 71..77, 85, 86 -> TipoCeu.NEVE
    in 95..99 -> TipoCeu.TEMPESTADE
    in 51..67, in 80..82 -> TipoCeu.CHUVA
    else -> TipoCeu.NUBLADO
}

/** Hora local do aparelho (0–23) — o "quando" que tinge o céu. */
fun horaLocal(): Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

/**
 * Escolhe a paleta a partir do código do tempo e da hora. Só o céu LIMPO ganha as 4 fases
 * (amanhecer/dia/entardecer/noite); os outros variam entre dia e noite, que é o que muda mesmo.
 */
fun paletaCeu(codigo: Int?, hora: Int): PaletaCeu {
    val tipo = classificar(codigo)
    val noite = hora < 5 || hora >= 20
    val amanhecer = hora in 5..7
    val entardecer = hora in 17..19

    fun p(vararg c: Long) = c.map { Color(it) }

    return when (tipo) {
        TipoCeu.LIMPO -> when {
            amanhecer -> PaletaCeu(p(0xFF402F3C, 0xFF2A2236, 0xFF17151D), Color(0xFFF4A659), tipo, false)
            entardecer -> PaletaCeu(p(0xFF3D2A3A, 0xFF2A2039, 0xFF17141F), Color(0xFFF0863C), tipo, false)
            noite -> PaletaCeu(p(0xFF1A2444, 0xFF12192E, 0xFF0C0F18), Color(0xFFA9BEE0), tipo, true)
            else -> PaletaCeu(p(0xFF20344A, 0xFF182838, 0xFF121A24), Color(0xFFF3C55A), tipo, false)
        }
        TipoCeu.NUBLADO ->
            if (noite) PaletaCeu(p(0xFF20242D, 0xFF1A1E25, 0xFF121419), Color(0xFF7C8598), tipo, true)
            else PaletaCeu(p(0xFF2B3039, 0xFF23272F, 0xFF171A20), Color(0xFFC4CDDA), tipo, false)
        TipoCeu.CHUVA ->
            if (noite) PaletaCeu(p(0xFF16212F, 0xFF111A25, 0xFF0B1017), Color(0xFF5C86A0), tipo, true)
            else PaletaCeu(p(0xFF1E2C3B, 0xFF17242F, 0xFF10161D), Color(0xFF74AEC9), tipo, false)
        TipoCeu.NEVE ->
            if (noite) PaletaCeu(p(0xFF1E2731, 0xFF171E27, 0xFF10151B), Color(0xFFAEBECE), tipo, true)
            else PaletaCeu(p(0xFF28333F, 0xFF202A33, 0xFF161C23), Color(0xFFDCE7F0), tipo, false)
        TipoCeu.NEBLINA ->
            if (noite) PaletaCeu(p(0xFF202429, 0xFF191C22, 0xFF111318), Color(0xFF838A94), tipo, true)
            else PaletaCeu(p(0xFF2C2F35, 0xFF24272D, 0xFF181B20), Color(0xFFB9C0C9), tipo, false)
        TipoCeu.TEMPESTADE ->
            if (noite) PaletaCeu(p(0xFF1E1830, 0xFF151024, 0xFF0A0812), Color(0xFF7E6BC0), tipo, true)
            else PaletaCeu(p(0xFF271F38, 0xFF1B172A, 0xFF0E0C16), Color(0xFF9A85D6), tipo, false)
    }
}

/**
 * Desenha o fundo vivo. `compacto` = card do Início (brilho menor); false = herói do modal.
 * Uma transição respira o brilho; outra faz a chuva/neve cair. Estrelas só no céu limpo à noite.
 */
@Composable
fun CeuVivoFundo(
    paleta: PaletaCeu,
    modifier: Modifier = Modifier,
    compacto: Boolean = false,
) {
    val transicao = rememberInfiniteTransition(label = "ceu")
    val pulso by transicao.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulso",
    )
    val duracaoQueda = if (paleta.tipo == TipoCeu.NEVE) 6500 else 1900
    val queda by transicao.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(duracaoQueda, easing = LinearEasing), RepeatMode.Restart),
        label = "queda",
    )

    Canvas(modifier) {
        // Base: gradiente vertical de 3 paradas.
        drawRect(brush = Brush.verticalGradient(paleta.stops))

        // Brilho (sol/lua) no canto superior direito, respirando.
        val centro = Offset(size.width * 0.82f, size.height * if (compacto) 0.16f else 0.22f)
        val raio = size.minDimension * (if (compacto) 0.62f else 0.78f) * (0.92f + 0.08f * pulso)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(paleta.brilho.copy(alpha = 0.32f + 0.14f * pulso), Color.Transparent),
                center = centro,
                radius = raio,
            ),
            center = centro,
            radius = raio,
        )

        // Atmosfera leve, só quando o tempo pede.
        when (paleta.tipo) {
            TipoCeu.CHUVA -> desenharChuva(paleta.brilho, queda, forte = false)
            TipoCeu.TEMPESTADE -> desenharChuva(paleta.brilho, queda, forte = true)
            TipoCeu.NEVE -> desenharNeve(paleta.brilho, queda)
            TipoCeu.LIMPO -> if (paleta.noite) desenharEstrelas(paleta.brilho, pulso)
            else -> Unit
        }
    }
}

private fun DrawScope.desenharChuva(cor: Color, progresso: Float, forte: Boolean) {
    val n = if (forte) 16 else 11
    val comprimento = size.height * 0.11f
    val alpha = if (forte) 0.20f else 0.16f
    for (i in 0 until n) {
        val x = size.width * ((i * 0.137f + 0.05f) % 1f)
        val fase = i * 0.0913f
        val y = (((progresso + fase) % 1f) * (size.height + comprimento)) - comprimento
        drawLine(
            color = cor.copy(alpha = alpha),
            start = Offset(x, y),
            end = Offset(x - comprimento * 0.16f, y + comprimento),
            strokeWidth = 1.3.dp.toPx(),
        )
    }
}

private fun DrawScope.desenharNeve(cor: Color, progresso: Float) {
    val n = 12
    for (i in 0 until n) {
        val baseX = size.width * ((i * 0.163f + 0.04f) % 1f)
        val fase = i * 0.0817f
        val avanco = (progresso + fase) % 1f
        val y = avanco * (size.height + 20f) - 10f
        val x = baseX + sin((avanco * 2f * PI).toFloat()) * 8f
        val r = 1.5.dp.toPx() + (i % 3) * 0.5.dp.toPx()
        drawCircle(color = cor.copy(alpha = 0.24f), radius = r, center = Offset(x, y))
    }
}

private val pontosEstrela = listOf(
    0.15f to 0.22f, 0.34f to 0.14f, 0.55f to 0.28f, 0.70f to 0.11f,
    0.24f to 0.42f, 0.62f to 0.46f, 0.86f to 0.36f, 0.44f to 0.20f,
)

private fun DrawScope.desenharEstrelas(cor: Color, pulso: Float) {
    pontosEstrela.forEachIndexed { i, (fx, fy) ->
        val brilho = 0.10f + 0.14f * (0.5f + 0.5f * sin((pulso * 2f * PI + i).toFloat()))
        drawCircle(
            color = cor.copy(alpha = brilho),
            radius = 1.2.dp.toPx(),
            center = Offset(size.width * fx, size.height * fy),
        )
    }
}
