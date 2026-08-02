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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cartão-fantasma enquanto a Luna desenha/edita — estilo ChatGPT (campo abstrato vivo),
 * com a cara do Orbit: grafite + azul pastel + um fio de violeta/dourado, sem mascote no
 * centro. O quadrado inteiro “pensa” a imagem; o rótulo embaixo diz o gesto.
 */
@Composable
fun LunaImagePlaceholderCard(
    label: String = "Desenhando",
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(18.dp)
    val infinite = rememberInfiniteTransition(label = "img-gen")

    // Tempo contínuo — fases das manchas se desencontram pra não parecer um loop rígido.
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing)),
        label = "tempo",
    )
    val tLento by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing)),
        label = "tempo-lento",
    )
    val pulso by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulso",
    )
    // Grão estável por sessão do card (não dança a cada frame).
    val grao = remember {
        List(96) {
            Triple(Random.nextFloat(), Random.nextFloat(), 0.03f + Random.nextFloat() * 0.07f)
        }
    }

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
                .background(OrbitTokens.ink1),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val min = size.minDimension

                // Base: grafite com um sopro frio no canto.
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF12141A),
                            Color(0xFF1A1D26),
                            Color(0xFF151820),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                    ),
                )

                // Manchas de névoa (radial soft) — “procedural” sem shader.
                fun mancha(
                    cx: Float,
                    cy: Float,
                    raio: Float,
                    cor: Color,
                    alpha: Float,
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(cor.copy(alpha = alpha), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = raio,
                        ),
                        radius = raio,
                        center = Offset(cx, cy),
                    )
                }

                // Azul pastel — mancha principal (deriva lenta).
                mancha(
                    cx = w * (0.32f + 0.10f * cos(t)),
                    cy = h * (0.38f + 0.08f * sin(t * 0.9f)),
                    raio = min * (0.55f + 0.06f * pulso),
                    cor = OrbitTokens.bluePastel,
                    alpha = 0.22f + 0.08f * pulso,
                )
                // Violeta discreto — segundo plano, fora de fase.
                mancha(
                    cx = w * (0.72f + 0.08f * sin(tLento)),
                    cy = h * (0.30f + 0.10f * cos(tLento * 1.1f)),
                    raio = min * 0.48f,
                    cor = OrbitTokens.violet,
                    alpha = 0.14f + 0.05f * (1f - pulso),
                )
                // Acento frio / accent — passa pelo meio como um “pensamento”.
                mancha(
                    cx = w * (0.50f + 0.18f * cos(t * 0.7f + 1.2f)),
                    cy = h * (0.62f + 0.12f * sin(t * 0.8f)),
                    raio = min * 0.42f,
                    cor = OrbitTokens.accentText,
                    alpha = 0.16f,
                )
                // Sopro dourado bem baixo — assinatura Luna, sem virar o foco.
                mancha(
                    cx = w * (0.28f + 0.06f * sin(tLento + 0.4f)),
                    cy = h * (0.78f + 0.05f * cos(t)),
                    raio = min * 0.28f,
                    cor = OrbitTokens.gold,
                    alpha = 0.07f + 0.03f * pulso,
                )
                // Halo central suave (como se a imagem estivesse a nascer).
                mancha(
                    cx = w * 0.5f,
                    cy = h * 0.48f,
                    raio = min * (0.34f + 0.04f * pulso),
                    cor = Color.White,
                    alpha = 0.045f + 0.025f * pulso,
                )

                // Grão fino — textura de “ainda não é pixel final”.
                for ((nx, ny, a) in grao) {
                    val twinkle = 0.55f + 0.45f * (0.5f + 0.5f * sin(t * 3f + nx * 12f + ny * 9f))
                    drawCircle(
                        color = Color.White.copy(alpha = a * twinkle),
                        radius = 1.1f,
                        center = Offset(nx * w, ny * h),
                    )
                }

                // Vinheta — empurra o olho pro centro sem parecer moldura.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f),
                        ),
                        center = Offset(w * 0.5f, h * 0.48f),
                        radius = min * 0.78f,
                    ),
                )

                // Faixa de luz bem suave atravessando (shimmer orgânico, não skeleton duro).
                val sweep = (0.5f + 0.5f * sin(t * 1.4f))
                val x0 = w * (sweep - 0.35f)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                        start = Offset(x0, 0f),
                        end = Offset(x0 + w * 0.45f, h),
                    ),
                )
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
