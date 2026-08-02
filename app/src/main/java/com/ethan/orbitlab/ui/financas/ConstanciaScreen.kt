package com.ethan.orbitlab.ui.financas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.financas.CONQUISTAS_FINANCAS
import com.ethan.orbitlab.data.financas.ConquistaDef
import com.ethan.orbitlab.data.financas.FaseLua
import com.ethan.orbitlab.data.financas.FinancasLuzEngine
import com.ethan.orbitlab.data.financas.FinancasLuzEstado
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitEnter
import com.ethan.orbitlab.ui.theme.orbitPressable
import java.text.NumberFormat
import java.util.Locale

private val CardRadius = 28.dp

/**
 * Constância — polish + toggle Semana/Mês na luz.
 */
@Composable
fun ConstanciaScreen() {
    val estado by FinancasLuzEngine.estado.collectAsState()
    val diasSemana = remember { listOf("S", "T", "Q", "Q", "S", "S", "D") }
    val desbloqueadas = estado.conquistas.size
    val totalConquistas = CONQUISTAS_FINANCAS.size
    var vistaMes by remember { mutableStateOf(false) }
    val recordeAlvo = when {
        estado.ofensiva < 7 -> 7
        estado.ofensiva < 30 -> 30
        else -> null
    }
    val faltamRecorde = recordeAlvo?.let { (it - estado.ofensiva).coerceAtLeast(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = OrbitMetrics.pagePadding,
            end = OrbitMetrics.pagePadding,
            top = 2.dp,
            bottom = 36.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Constância",
                color = OrbitTokens.textHiN,
                fontSize = 28.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.orbitEnter(0),
            )
        }

        item {
            HeroConstancia(estado)
        }

        item {
            BannerRecorde(faltam = faltamRecorde, ofensiva = estado.ofensiva)
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (vistaMes) "Luz do mês" else "Luz da semana",
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (vistaMes) "Semana" else "Mês",
                    color = OrbitTokens.bluePastel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.orbitPressable { vistaMes = !vistaMes },
                )
            }
            Spacer(Modifier.height(10.dp))
            if (vistaMes) {
                CardLuzMesConcept(estado)
            } else {
                CardLuzSemanaConcept(estado.luzSemana, diasSemana)
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Conquistas",
                    color = OrbitTokens.textHiN,
                    fontSize = 17.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "$desbloqueadas / $totalConquistas",
                    color = OrbitTokens.bluePastel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CONQUISTAS_FINANCAS.forEach { def ->
                    TileConquista(
                        def = def,
                        desbloqueada = def.id in estado.conquistas,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroConstancia(estado: FinancasLuzEstado) {
    val fasesNum = when (estado.fase) {
        FaseLua.NOVA -> 0
        FaseLua.CRESCENTE -> 1
        FaseLua.QUASE_CHEIA -> 2
        FaseLua.CHEIA -> 3
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LuaFaseVisual(estado.fase, Modifier.size(120.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            "${estado.ofensiva} dias",
            color = OrbitTokens.textHiN,
            fontSize = 32.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 14.sp)) {
                    append("dentro do orçamento · lua ")
                }
                withStyle(
                    SpanStyle(
                        color = OrbitTokens.bluePastel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(estado.fase.rotulo.removePrefix("lua ").trim())
                }
            },
        )
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatHero(
                valor = formatarLuz(estado.luzTotal),
                valorCor = OrbitTokens.gold,
                detalhe = "✨  luz total",
            )
            StatHero(
                valor = "+${estado.luzEstaSemana}",
                valorCor = OrbitTokens.textHiN,
                detalhe = "esta semana",
            )
            StatHero(
                valor = "$fasesNum",
                valorCor = OrbitTokens.textHiN,
                detalhe = "🌙  fases",
            )
        }
    }
}

@Composable
private fun StatHero(valor: String, valorCor: Color, detalhe: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            valor,
            color = valorCor,
            fontSize = 20.sp,
            fontFamily = Bricolage,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            detalhe,
            color = OrbitTokens.textLowN,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun LuaFaseVisual(fase: FaseLua, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        // Glow externo
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            OrbitTokens.bluePastel.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Canvas(Modifier.fillMaxSize(0.78f)) {
            val dim = this.size
            val cx = dim.width / 2f
            val cy = dim.height / 2f
            val r = minOf(dim.width, dim.height) / 2f
            // Disco base (lua)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFF2F4F8), Color(0xFFB8BEC8), Color(0xFF8A909A)),
                    center = Offset(cx - r * 0.25f, cy - r * 0.3f),
                    radius = r * 1.2f,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
            // Sombra da fase (crescente / quase / nova)
            val sombraOffset = when (fase) {
                FaseLua.NOVA -> 0f
                FaseLua.CRESCENTE -> r * 0.55f
                FaseLua.QUASE_CHEIA -> r * 0.28f
                FaseLua.CHEIA -> r * 2f // fora — lua cheia
            }
            if (fase != FaseLua.CHEIA) {
                drawCircle(
                    color = OrbitTokens.graphiteSurf,
                    radius = if (fase == FaseLua.NOVA) r * 0.98f else r * 0.92f,
                    center = Offset(cx + sombraOffset, cy),
                )
            }
        }
    }
}

@Composable
private fun BannerRecorde(faltam: Int?, ofensiva: Int) {
    val texto = when {
        faltam == null -> "Recorde batido neste ciclo. Mantém o ritmo — a luz continua acumulando."
        faltam == 0 -> "Hoje você empata o próximo marco. Um dia no orçamento e a fase sobe."
        ofensiva == 0 -> "Um dia no orçamento já acende a ofensiva. Registrar e pagar o fixo ajuda."
        else -> "Faltam $faltam dias pra bater seu próximo marco de constância. " +
            "Pagar um fixo ou ficar no orçamento hoje já garante o de amanhã."
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(OrbitTokens.graphiteSurf)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(OrbitTokens.bluePastel, OrbitTokens.bluePastelDim, Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            buildAnnotatedString {
                if (faltam != null && faltam > 0) {
                    withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 13.5.sp)) {
                        append("Faltam ")
                    }
                    withStyle(
                        SpanStyle(
                            color = OrbitTokens.textHiN,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                        ),
                    ) {
                        append("$faltam dias")
                    }
                    withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 13.5.sp)) {
                        append(
                            " pra bater seu próximo marco de constância. " +
                                "Pagar um fixo hoje já garante o de amanhã.",
                        )
                    }
                } else {
                    withStyle(SpanStyle(color = OrbitTokens.textMidN, fontSize = 13.5.sp)) {
                        append(texto)
                    }
                }
            },
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CardLuzMesConcept(estado: FinancasLuzEstado) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(vertical = 18.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatarLuz(estado.luzTotal),
                color = OrbitTokens.bluePastel,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Bricolage,
            )
            Spacer(Modifier.height(4.dp))
            Text("luz total", color = OrbitTokens.textLowN, fontSize = 12.sp)
        }
        Box(
            Modifier
                .width(1.dp)
                .height(40.dp)
                .background(OrbitTokens.graphiteHair),
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "+${estado.luzEstaSemana}",
                color = OrbitTokens.online,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Bricolage,
            )
            Spacer(Modifier.height(4.dp))
            Text("esta semana", color = OrbitTokens.textLowN, fontSize = 12.sp)
        }
        Box(
            Modifier
                .width(1.dp)
                .height(40.dp)
                .background(OrbitTokens.graphiteHair),
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${estado.ofensiva}d",
                color = OrbitTokens.textHiN,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = Bricolage,
            )
            Spacer(Modifier.height(4.dp))
            Text("ofensiva", color = OrbitTokens.textLowN, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CardLuzSemanaConcept(barras: List<Int>, labels: List<String>) {
    val max = (barras.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(OrbitTokens.graphiteSurf)
            .padding(16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            barras.forEachIndexed { i, v ->
                val frac = if (v <= 0) 0.1f else (v.toFloat() / max).coerceIn(0.12f, 1f)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.65f)
                                .fillMaxHeight(frac)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (v > 0) OrbitTokens.bluePastel
                                    else OrbitTokens.bluePastel.copy(alpha = 0.22f),
                                ),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        labels.getOrElse(i) { "·" },
                        color = OrbitTokens.textLowN,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun TileConquista(def: ConquistaDef, desbloqueada: Boolean) {
    Column(
        Modifier
            .width(96.dp)
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (desbloqueada) OrbitTokens.graphiteSurf
                else OrbitTokens.graphiteSurf.copy(alpha = 0.55f),
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (desbloqueada) {
                        Brush.radialGradient(
                            listOf(
                                OrbitTokens.bluePastel.copy(alpha = 0.45f),
                                OrbitTokens.bluePastel.copy(alpha = 0.08f),
                                Color.Transparent,
                            ),
                        )
                    } else {
                        Brush.radialGradient(
                            listOf(OrbitTokens.graphiteRaised, Color.Transparent),
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                def.emoji,
                fontSize = 22.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            def.titulo,
            color = if (desbloqueada) OrbitTokens.textHiN else OrbitTokens.textLowN,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp,
        )
    }
}

private fun formatarLuz(n: Int): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(n)
