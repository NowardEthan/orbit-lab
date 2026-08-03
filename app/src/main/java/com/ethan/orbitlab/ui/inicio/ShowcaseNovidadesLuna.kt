package com.ethan.orbitlab.ui.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Card premium de descoberta na Início — Finanças, Luna desenha e artefatos.
 * Base grafite; azul pastel só no chip e nos ícones (sem fills saturados).
 */
@Composable
fun ShowcaseNovidadesLuna(
    onAbrirFinancas: () -> Unit,
    onLunaDesenha: () -> Unit,
    onAbrirEstante: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(OrbitMetrics.radiusCard)

    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrbitTokens.graphiteSurf)
            .border(1.dp, OrbitTokens.graphiteHair, shape),
    ) {
        // Glow pontual no canto — presença, sem mover o card.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-20).dp, y = (-28).dp)
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            OrbitTokens.bluePastel.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Novo",
                        color = OrbitTokens.onBluePastel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(OrbitMetrics.radiusChip))
                            .background(OrbitTokens.bluePastel)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "O que a Luna ganhou",
                        color = OrbitTokens.textHiN,
                        fontSize = 17.sp,
                        fontFamily = Bricolage,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.2).sp,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Três coisas novas pra explorar com ela.",
                        color = OrbitTokens.textMidN,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OrbitTokens.graphiteRaised)
                        .orbitPressable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Dispensar",
                        tint = OrbitTokens.textLowN,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            ShowcaseLinha(
                icone = Icons.Rounded.AccountBalanceWallet,
                titulo = "Finanças com a Luna",
                sub = "Painel, extrato e captura do dia a dia",
                onClick = onAbrirFinancas,
            )
            ShowcaseDivisor()
            ShowcaseLinha(
                icone = Icons.Rounded.AutoAwesome,
                titulo = "Luna desenha",
                sub = "Peça uma arte e ela cria no chat",
                onClick = onLunaDesenha,
            )
            ShowcaseDivisor()
            ShowcaseLinha(
                icone = Icons.Rounded.MenuBook,
                titulo = "Artefatos",
                sub = "Documentos e criações guardados na Galeria",
                onClick = onAbrirEstante,
            )
        }
    }
}

@Composable
private fun ShowcaseDivisor() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 48.dp)
            .height(1.dp)
            .background(OrbitTokens.graphiteHair),
    )
}

@Composable
private fun ShowcaseLinha(
    icone: ImageVector,
    titulo: String,
    sub: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .orbitPressable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icone,
                contentDescription = null,
                tint = OrbitTokens.bluePastel,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                color = OrbitTokens.textHiN,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                sub,
                color = OrbitTokens.textMidN,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = OrbitTokens.textLowN,
            modifier = Modifier.size(16.dp),
        )
    }
}
