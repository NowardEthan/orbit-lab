package com.ethan.orbitlab.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens do Orbit em Compose — base dark/cinza; acentos saturados via [OrbitFills].
 *
 * Cor no Compose é 0xAARRGGBB (alpha na frente): 0xFF = opaco; 0x14 ≈ 8% de opacidade.
 */
object OrbitTokens {
    val ink0 = Color(0xFF0A0B0F)
    val ink1 = Color(0xFF0E1014)
    // Superfícies um degrau mais neutras (menos “azuladas”)
    val surface = Color(0xFF1A1C22)
    /** Balão da Luna — um pouco mais escuro que [surface], pra diferir do chrome. */
    val bubbleLuna = Color(0xFF15171C)
    val surfaceRaised = Color(0xFF22252C)
    val surfaceHover = Color(0xFF2A2D36)

    val border = Color(0xFF3A3E48)
    val borderSoft = Color(0x14FFFFFF) // rgba(255,255,255,0.08)

    val textHigh = Color(0xFFF2F4F8)
    val textMid = Color(0xFF9CA3B0)
    val textLow = Color(0xFF7A808C)

    val accent = Color(0xFF4B75F2)
    val accentFlat = Color(0xFF3A5FD4)
    val accentText = Color(0xFF7A9AF5)
    val accentSoft = Color(0x244B75F2) // só superfícies suaves, NÃO chips

    val gold = Color(0xFFF5D047)
    val goldFlat = Color(0xFFD89B1A)
    val onGold = Color(0xFF332504)

    val violet = Color(0xFFB98CFF)
    val violetFlat = Color(0xFF6732DD)

    val online = Color(0xFF6BC4A0)
    val onlineFlat = Color(0xFF23B55C)

    val warning = Color(0xFFFFB74D)
    val danger = Color(0xFFEF5350)
    val dangerFlat = Color(0xFFC62828)
}
