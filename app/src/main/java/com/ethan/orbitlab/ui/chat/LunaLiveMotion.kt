package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens

@Composable
internal fun LunaTextoVivo(
    text: String,
    modifier: Modifier = Modifier,
    ativo: Boolean = true,
    color: Color = OrbitTokens.textMidN,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    lineHeight: androidx.compose.ui.unit.TextUnit = 17.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    maxLines: Int = Int.MAX_VALUE,
) {
    if (!ativo) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }

    val transicao = rememberInfiniteTransition(label = "texto-vivo")
    val progresso by transicao.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
        ),
        label = "brilho-texto",
    )
    var tamanhoTexto by remember { mutableStateOf(IntSize.Zero) }
    val largura = tamanhoTexto.width.toFloat().coerceAtLeast(1f)
    val faixa = largura * 0.42f
    val centro = -faixa + (largura + faixa * 2f) * progresso
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to OrbitTokens.textLowN.copy(alpha = 0.74f),
            0.42f to color.copy(alpha = 0.92f),
            0.5f to OrbitTokens.textHiN,
            0.58f to OrbitTokens.bluePastel,
            1f to OrbitTokens.textLowN.copy(alpha = 0.74f),
        ),
        start = Offset(centro - faixa, 0f),
        end = Offset(centro + faixa, 0f),
    )

    Text(
        text = text,
        style = TextStyle(
            brush = brush,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { tamanhoTexto = it.size },
        modifier = modifier,
    )
}

@Composable
internal fun LunaLiveRail(
    aoVivo: Boolean,
    erro: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val progresso = if (aoVivo) {
        val transicao = rememberInfiniteTransition(label = "trilho-vivo")
        val animado by transicao.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = LinearEasing),
            ),
            label = "pulso-trilho",
        )
        animado
    } else {
        0f
    }
    val base = when {
        erro -> OrbitTokens.danger.copy(alpha = 0.42f)
        aoVivo -> OrbitTokens.bluePastel.copy(alpha = 0.32f)
        else -> OrbitTokens.bluePastel.copy(alpha = 0.18f)
    }
    val brilho = if (erro) OrbitTokens.danger else OrbitTokens.bluePastel

    Box(
        modifier
            .width(2.5.dp)
            .fillMaxHeight()
            .drawBehind {
                val radius = CornerRadius(size.width, size.width)
                drawRoundRect(color = base, cornerRadius = radius)
                if (aoVivo) {
                    val faixa = (size.height * 0.38f).coerceAtLeast(18f)
                    val y = -faixa + (size.height + faixa * 2f) * progresso
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to brilho.copy(alpha = 0.15f),
                            0.5f to brilho.copy(alpha = 0.95f),
                            0.55f to brilho.copy(alpha = 0.18f),
                            1f to Color.Transparent,
                            startY = y,
                            endY = y + faixa,
                        ),
                        topLeft = Offset(0f, y),
                        size = Size(size.width, faixa),
                        cornerRadius = radius,
                    )
                }
            },
    )
}

@Composable
internal fun LunaStatusDot(
    status: LunaActionStepStatus,
    modifier: Modifier = Modifier,
    size: Dp = 9.dp,
) {
    val rodando = status == LunaActionStepStatus.RUNNING
    val erro = status == LunaActionStepStatus.ERROR
    val pulso = if (rodando) {
        val transicao = rememberInfiniteTransition(label = "status-dot")
        val animado by transicao.animateFloat(
            initialValue = 0.72f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 820),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot-pulso",
        )
        animado
    } else {
        1f
    }
    val cor = when {
        erro -> OrbitTokens.danger
        rodando -> OrbitTokens.bluePastel
        status == LunaActionStepStatus.DONE -> OrbitTokens.online
        else -> OrbitTokens.textLowN
    }
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                val escala = if (rodando) 0.9f + 0.16f * pulso else 1f
                scaleX = escala
                scaleY = escala
                alpha = if (rodando) 0.65f + 0.35f * pulso else 0.88f
            }
            .clip(CircleShape)
            .background(cor.copy(alpha = if (rodando) 0.24f else 0.14f))
            .border(1.dp, cor.copy(alpha = if (rodando) 0.88f else 0.42f), CircleShape),
    )
}

internal fun Modifier.liveSurface(ativo: Boolean, erro: Boolean = false): Modifier {
    val shape = RoundedCornerShape(11.dp)
    val borderColor = when {
        erro -> OrbitTokens.danger.copy(alpha = 0.26f)
        ativo -> OrbitTokens.bluePastel.copy(alpha = 0.24f)
        else -> Color.Transparent
    }
    val bg = if (ativo) {
        OrbitTokens.graphiteSurf.copy(alpha = 0.62f)
    } else {
        Color.Transparent
    }
    return this
        .clip(shape)
        .background(bg)
        .border(1.dp, borderColor, shape)
}
