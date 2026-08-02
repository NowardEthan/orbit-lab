package com.ethan.orbitlab.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cartão-fantasma enquanto a Luna desenha/edita.
 *
 * Visual na linha dos loaders abstratos geométricos (anéis de pontos, espiral, alvo) —
 * tipo o pack Craftwork Abstract Loading Animations — em grafite + azul pastel Orbit,
 * sem mascote e sem névoa.
 */
@Composable
fun LunaImagePlaceholderCard(
    label: String = "Desenhando",
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(18.dp)
    val infinite = rememberInfiniteTransition(label = "img-gen")

    val giro by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4_200, easing = LinearEasing)),
        label = "giro",
    )
    val giroContrario by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(6_800, easing = LinearEasing)),
        label = "giro-contrario",
    )
    val pulso by infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulso",
    )
    val faseEspiral by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(5_500, easing = LinearEasing)),
        label = "espiral",
    )

    Column(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .clip(forma)
            .background(OrbitTokens.bubbleLuna)
            .border(1.dp, OrbitTokens.borderSoft, forma),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(OrbitTokens.graphiteBg),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val centro = Offset(cx, cy)
                val min = size.minDimension
                val escala = min / 280f

                val azul = OrbitTokens.bluePastel
                val azulDim = OrbitTokens.bluePastelDim
                val fio = OrbitTokens.textLowN.copy(alpha = 0.35f)
                val ouro = OrbitTokens.gold.copy(alpha = 0.55f)

                // Anel alvo externo (tracejado contínuo que gira).
                rotate(giroContrario, centro) {
                    drawCircle(
                        color = fio,
                        radius = 88f * escala,
                        center = centro,
                        style = Stroke(width = 1.2f * escala),
                    )
                    // Arcos curtos no anel — “scanner”.
                    drawArc(
                        color = azul.copy(alpha = 0.55f),
                        startAngle = -18f,
                        sweepAngle = 52f,
                        useCenter = false,
                        topLeft = Offset(cx - 88f * escala, cy - 88f * escala),
                        size = Size(176f * escala, 176f * escala),
                        style = Stroke(width = 2.4f * escala, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = azulDim.copy(alpha = 0.35f),
                        startAngle = 140f,
                        sweepAngle = 40f,
                        useCenter = false,
                        topLeft = Offset(cx - 88f * escala, cy - 88f * escala),
                        size = Size(176f * escala, 176f * escala),
                        style = Stroke(width = 1.8f * escala, cap = StrokeCap.Round),
                    )
                }

                // Anel de pontos (dot ring) — gira no sentido horário.
                val rPontos = 64f * escala
                val nPontos = 16
                for (i in 0 until nPontos) {
                    val ang = Math.toRadians((giro + i * (360f / nPontos)).toDouble())
                    val p = Offset(
                        cx + cos(ang).toFloat() * rPontos,
                        cy + sin(ang).toFloat() * rPontos,
                    )
                    val destaque = i % 4 == 0
                    drawCircle(
                        color = if (destaque) azul.copy(alpha = 0.85f) else azul.copy(alpha = 0.28f),
                        radius = if (destaque) 3.2f * escala * pulso else 2.1f * escala,
                        center = p,
                    )
                }

                // Segundo anel de pontos, mais interno, sentido contrário.
                val rPontosIn = 42f * escala
                val nIn = 10
                for (i in 0 until nIn) {
                    val ang = Math.toRadians((giroContrario + i * (360f / nIn)).toDouble())
                    val p = Offset(
                        cx + cos(ang).toFloat() * rPontosIn,
                        cy + sin(ang).toFloat() * rPontosIn,
                    )
                    drawCircle(
                        color = azulDim.copy(alpha = 0.5f),
                        radius = 2.4f * escala,
                        center = p,
                    )
                }

                // Espiral de pontos (dot spiral) — “desenrola” com o tempo.
                val voltas = 2.15f
                val passos = 28
                for (i in 0 until passos) {
                    val u = i / (passos - 1f)
                    val ang = faseEspiral + u * voltas * (PI * 2).toFloat()
                    val r = (12f + u * 52f) * escala
                    val p = Offset(
                        cx + cos(ang.toDouble()).toFloat() * r,
                        cy + sin(ang.toDouble()).toFloat() * r,
                    )
                    drawCircle(
                        color = azul.copy(alpha = 0.15f + 0.55f * (1f - u)),
                        radius = (1.4f + 1.6f * (1f - u)) * escala,
                        center = p,
                    )
                }

                // Núcleo — alvo mínimo que respira (sem virar mascote).
                drawCircle(
                    color = azul.copy(alpha = 0.12f * pulso),
                    radius = 18f * escala * pulso,
                    center = centro,
                )
                drawCircle(
                    color = azul.copy(alpha = 0.75f),
                    radius = 4.5f * escala,
                    center = centro,
                )
                // Faísca dourada bem discreta no núcleo — assinatura Luna.
                drawCircle(
                    color = ouro,
                    radius = 1.6f * escala,
                    center = Offset(cx + 7f * escala, cy - 6f * escala),
                )

                // Triângulos geométricos leves orbitando (motif do pack).
                rotate(giro * 0.35f, centro) {
                    val rTri = 74f * escala
                    for (k in 0 until 3) {
                        val baseAng = k * 120f
                        val a0 = Math.toRadians(baseAng.toDouble())
                        val tip = Offset(
                            cx + cos(a0).toFloat() * rTri,
                            cy + sin(a0).toFloat() * rTri,
                        )
                        val s = 5.5f * escala
                        val perp = baseAng + 90f
                        val a1 = Math.toRadians((perp).toDouble())
                        val left = Offset(
                            tip.x + cos(a1).toFloat() * s - cos(a0).toFloat() * s * 0.6f,
                            tip.y + sin(a1).toFloat() * s - sin(a0).toFloat() * s * 0.6f,
                        )
                        val right = Offset(
                            tip.x - cos(a1).toFloat() * s - cos(a0).toFloat() * s * 0.6f,
                            tip.y - sin(a1).toFloat() * s - sin(a0).toFloat() * s * 0.6f,
                        )
                        drawLine(
                            color = azul.copy(alpha = 0.35f),
                            start = tip,
                            end = left,
                            strokeWidth = 1.3f * escala,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = azul.copy(alpha = 0.35f),
                            start = tip,
                            end = right,
                            strokeWidth = 1.3f * escala,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = azul.copy(alpha = 0.22f),
                            start = left,
                            end = right,
                            strokeWidth = 1.1f * escala,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = OrbitTokens.bluePastel.copy(alpha = 0.55f + 0.35f * pulso),
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "$label a imagem…",
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
