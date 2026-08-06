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
    @Deprecated(
        "Anti-padrão de opacidade (accent-sobre-accent). Use contraste: OrbitFills.accent.brush " +
            "+ onFill pra estados acesos, ou surfaceRaised + ícone saturado pra ladrilhos.",
    )
    val accentSoft = Color(0x244B75F2)

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

    // P1.3: semanticos separados do accent (AGENTS.md sec 4: verde=entrada, vermelho=saida, ambar=atencao).
    // Versoes "Flat" (contidas) pra alternadores Saida/Entrada no Registrar e similares.
    val semaforoEntrada = Color(0xFF2F5A48)
    val semaforoSaida = Color(0xFF6B3E3A)
    val semaforoAtencao = Color(0xFFFFB74D) // idem ao warning

    // ── Orbit 1.0 — base grafite neutra + azul pastel ─────────────────────────
    // Direção do redesign (ver memória orbit-redesign-1-0): larga o violeta/Penumbra,
    // vai pra um grafite neutro com azul pastel só nos detalhes. Tokens NOVOS,
    // aplicados só nas superfícies já repaginadas (gaveta, barra de cima, Início)
    // pra não regredir as telas que ainda não chegaram a vez — as antigas seguem
    // nos tokens acima até serem migradas capítulo a capítulo.
    val graphiteBg = Color(0xFF17181B)      // fundo raiz (canvas atrás das telas)
    val graphiteSurf = Color(0xFF202226)    // cartão / superfície
    val graphiteRaised = Color(0xFF292C31)  // ladrilho neutro elevado
    val graphiteHair = Color(0x12FFFFFF)    // filete ~7%

    val bluePastel = Color(0xFF9CC0F0)      // acento pastel (destino ativo, CTA, enviar)
    val bluePastelDim = Color(0xFF7FA6DC)   // variante mais fechada (fim do degradê)
    val onBluePastel = Color(0xFF12233A)    // texto/ícone escuro sobre o pastel (contraste)

    val bubbleUser = Color(0xFF2A2D33)      // bolha do usuário no chat (cap. 3), cinza discreto

    // Texto neutro (grafite) — menos azulado que textHigh/Mid/Low
    val textHiN = Color(0xFFECECEE)
    val textMidN = Color(0xFF9A9CA2)
    val textLowN = Color(0xFF6A6C72)
}
