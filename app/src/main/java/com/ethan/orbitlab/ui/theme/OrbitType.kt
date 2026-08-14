package com.ethan.orbitlab.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.R

/**
 * Tipografia da Orbit 1.0.
 *
 * Duas famílias empacotadas (OFL) em res/font, ambas fontes VARIÁVEIS —
 * o eixo `wght` é dirigido por [FontVariation] pra cada peso.
 *
 *  - [Bricolage] — títulos / identidade. Grotesco com charme, distinto do Claude.
 *  - [Hanken]    — corpo / leitura. Sans limpa e legível; é o padrão do app.
 *
 * Em API < 26 o [FontVariation] é ignorado e cai na instância padrão do arquivo —
 * degrada de forma segura (a maioria dos aparelhos é 26+).
 */
@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: FontWeight) = Font(
    R.font.bricolage_grotesque,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun hanken(weight: FontWeight) = Font(
    R.font.hanken_grotesk,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Família de exibição — títulos e identidade. */
val Bricolage = FontFamily(
    bricolage(FontWeight.Normal),
    bricolage(FontWeight.Medium),
    bricolage(FontWeight.SemiBold),
    bricolage(FontWeight.Bold),
)

/** Família de corpo — leitura. Padrão do app. */
val Hanken = FontFamily(
    hanken(FontWeight.Normal),
    hanken(FontWeight.Medium),
    hanken(FontWeight.SemiBold),
    hanken(FontWeight.Bold),
)

/** Atalhos semânticos pra usar nas telas. */
object OrbitType {
    /** Bricolage — títulos, saudações, identidade. */
    val display = Bricolage

    /** Hanken — corpo, o padrão. */
    val body = Hanken

    /** Estilos de texto prég_definidos. */
    object Body {
        val XXS = androidx.compose.ui.text.TextStyle(
            fontFamily = Hanken,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
        val XS = androidx.compose.ui.text.TextStyle(
            fontFamily = Hanken,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
        val SM = androidx.compose.ui.text.TextStyle(
            fontFamily = Hanken,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        val MD = androidx.compose.ui.text.TextStyle(
            fontFamily = Hanken,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        val LG = androidx.compose.ui.text.TextStyle(
            fontFamily = Hanken,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
    }

    /** Estilos de títulos. */
    object Headline {
        val SM = androidx.compose.ui.text.TextStyle(
            fontFamily = Bricolage,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
        val MD = androidx.compose.ui.text.TextStyle(
            fontFamily = Bricolage,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
        )
        val LG = androidx.compose.ui.text.TextStyle(
            fontFamily = Bricolage,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
        )
    }
}
