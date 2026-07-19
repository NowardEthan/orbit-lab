package com.ethan.orbitlab.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens do Orbit em Compose — as MESMAS cores do app (src/theme/tokens.ts), agora nativas.
 *
 * `object` = um singleton pronto pra usar: `OrbitTokens.accent`, `OrbitTokens.gold`, etc.
 * Cor no Compose é 0xAARRGGBB (alpha na frente): 0xFF = opaco; 0x14 ≈ 8% de opacidade.
 */
object OrbitTokens {
    val ink0 = Color(0xFF0A0B0F)
    val ink1 = Color(0xFF0E1014)
    val surface = Color(0xFF1C1F28)
    val surfaceRaised = Color(0xFF242830)
    val surfaceHover = Color(0xFF2C303C)

    val border = Color(0xFF3A404C)
    val borderSoft = Color(0x14FFFFFF) // rgba(255,255,255,0.08)

    val textHigh = Color(0xFFF2F4F8)
    val textMid = Color(0xFF9CA3B0)
    val textLow = Color(0xFF7A808C)

    val accent = Color(0xFF4B75F2)
    val accentText = Color(0xFF7A9AF5)
    val accentSoft = Color(0x244B75F2) // rgba(75,117,242,0.14)

    // Os dois eixos da gamificação
    val gold = Color(0xFFF5D047)   // a luz
    val violet = Color(0xFFB98CFF) // a lua
    val online = Color(0xFF6BC4A0)
    val warning = Color(0xFFFFB74D)
    val danger = Color(0xFFEF5350)
}
