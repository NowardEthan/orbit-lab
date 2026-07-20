package com.ethan.orbitlab.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Preenchimento saturado → flat com texto de contraste alto.
 *
 * É o gesto dos cards “A Luz” / “A Lua” no Início: fundo vivo, conteúdo legível.
 * Usar em chips, badges, CTAs e cards de destaque.
 *
 * **Anti-padrão:** `cor.copy(alpha = 0.15f)` + texto na mesma cor (verde em verde, etc.).
 * Isso fica sem contraste — não usar para rótulos/chips.
 */
data class OrbitFill(
    val start: Color,
    val end: Color,
    /** Cor do texto/ícone sobre o gradiente. */
    val onFill: Color,
) {
    val colors: List<Color> get() = listOf(start, end)
    val brush: Brush get() = Brush.linearGradient(colors)
}

/**
 * Catálogo dos fills do OrbitLab — fonte única para não reinventar hex em cada tela.
 */
object OrbitFills {
    /** Gamificação: A Luz (dourado → âmbar, texto escuro). */
    val luz = OrbitFill(
        start = OrbitTokens.gold,
        end = OrbitTokens.goldFlat,
        onFill = OrbitTokens.onGold,
    )

    /** Gamificação: A Lua (violeta → roxo profundo, texto branco). */
    val lua = OrbitFill(
        start = OrbitTokens.violet,
        end = OrbitTokens.violetFlat,
        onFill = Color.White,
    )

    /** Estado positivo / “Novo” / online. */
    val online = OrbitFill(
        start = OrbitTokens.online,
        end = OrbitTokens.onlineFlat,
        onFill = Color.White,
    )

    /** Accent sólido (pílulas de versão, “Melhoria”). */
    val accent = OrbitFill(
        start = OrbitTokens.accent,
        end = OrbitTokens.accentFlat,
        onFill = Color.White,
    )

    /** CTA hero (accent → violeta), ex.: “Retomar Sessão”. */
    val cta = OrbitFill(
        start = OrbitTokens.accent,
        end = OrbitTokens.violet,
        onFill = Color.White,
    )

    /** Anel / FAB de gamificação (violeta → ouro). */
    val orbita = OrbitFill(
        start = OrbitTokens.violet,
        end = OrbitTokens.gold,
        onFill = Color.White,
    )

    /** Acção destrutiva (sair / apagar). */
    val danger = OrbitFill(
        start = OrbitTokens.danger,
        end = OrbitTokens.dangerFlat,
        onFill = Color.White,
    )

    /** Neutro / bloqueado / linhas sem ênfase. */
    val muted = OrbitFill(
        start = OrbitTokens.surfaceRaised,
        end = OrbitTokens.surface,
        onFill = OrbitTokens.textMid,
    )
}
