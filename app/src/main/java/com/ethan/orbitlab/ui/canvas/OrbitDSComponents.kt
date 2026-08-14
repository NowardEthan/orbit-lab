package com.ethan.orbitlab.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.Hanken
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType

/**
 * Status de sessão para SessionRow.
 */
enum class SessionStatus {
    ATIVA,
    PAUSADA,
    FINALIZADA,
    AGENDADA,
}

/**
 * Props para SessionRow (Orbit DS).
 */
data class SessionRowProps(
    val session: SessionData,
    val status: SessionStatus = SessionStatus.ATIVA,
    val active: Boolean = false,
    val compact: Boolean = false,
)

/**
 * Dados de sessão.
 */
data class SessionData(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val tags: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val duration: String? = null,
    val thumbnail: String? = null,
)

/**
 * SessionRow — componente Orbit DS renderizado no preview.
 */
@Composable
fun OrbitSessionRow(
    props: SessionRowProps,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val session = props.session

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(
                if (props.active) OrbitTokens.accent.copy(alpha = 0.1f)
                else OrbitTokens.graphiteSurf
            )
            .then(
                if (props.active) Modifier.border(
                    1.dp,
                    OrbitTokens.accent.copy(alpha = 0.3f),
                    RoundedCornerShape(OrbitMetrics.radiusSm)
                )
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(if (props.compact) 8.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(if (props.compact) 8.dp else 10.dp)
                .clip(CircleShape)
                .background(
                    when (props.status) {
                        SessionStatus.ATIVA -> OrbitTokens.online
                        SessionStatus.PAUSADA -> OrbitTokens.warning
                        SessionStatus.FINALIZADA -> OrbitTokens.textLow
                        SessionStatus.AGENDADA -> OrbitTokens.accent
                    }
                ),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Avatar/Thumbnail placeholder
        Box(
            modifier = Modifier
                .size(if (props.compact) 32.dp else 40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (props.status == SessionStatus.ATIVA) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(if (props.compact) 16.dp else 20.dp),
                    tint = OrbitTokens.online,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                style = OrbitType.Body.MD.copy(
                    fontWeight = if (props.active) FontWeight.Medium else FontWeight.Normal,
                ),
                color = if (props.active) OrbitTokens.accentText else OrbitTokens.textHiN,
            )
            if (!props.compact) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    session.subtitle?.let {
                        Text(
                            text = it,
                            style = OrbitType.Body.XXS,
                            color = OrbitTokens.textMid,
                        )
                    }
                    session.duration?.let {
                        Text(
                            text = "· $it",
                            style = OrbitType.Body.XXS,
                            color = OrbitTokens.textLow,
                        )
                    }
                }
            }
        }

        // Tags
        if (session.tags.isNotEmpty() && !props.compact) {
            OrbitBadgeChip(
                label = session.tags.first(),
                variant = BadgeVariant.INFO,
            )
        }
    }
}

/**
 * BadgeChip — variante de badge Orbit DS.
 */
@Composable
fun OrbitBadgeChip(
    label: String,
    variant: BadgeVariant = BadgeVariant.INFO,
    modifier: Modifier = Modifier,
) {
    val (bgColor, textColor) = when (variant) {
        BadgeVariant.INFO -> OrbitTokens.accent.copy(alpha = 0.15f) to OrbitTokens.accentText
        BadgeVariant.SUCCESS -> OrbitTokens.online.copy(alpha = 0.15f) to OrbitTokens.online
        BadgeVariant.WARNING -> OrbitTokens.warning.copy(alpha = 0.15f) to OrbitTokens.warning
        BadgeVariant.ERROR -> OrbitTokens.danger.copy(alpha = 0.15f) to OrbitTokens.danger
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = OrbitType.Body.XXS,
            color = textColor,
        )
    }
}

/**
 * CardLuaz — card de destaque do Início.
 */
@Composable
fun OrbitCardLuaz(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit = {},
    accentColor: Color = OrbitTokens.gold,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusMd))
            .background(OrbitTokens.graphiteSurf)
            .border(
                1.dp,
                accentColor.copy(alpha = 0.2f),
                RoundedCornerShape(OrbitMetrics.radiusMd),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Glow indicator
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.3f),
                            Color.Transparent,
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = OrbitType.Body.LG,
                fontWeight = FontWeight.Medium,
                color = OrbitTokens.textHiN,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = OrbitType.Body.SM,
                    color = OrbitTokens.textMid,
                )
            }
        }
    }
}

/**
 * ButtonPrimary — botão primário Orbit.
 */
@Composable
fun OrbitButtonPrimary(
    label: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(
                if (enabled) OrbitTokens.accent
                else OrbitTokens.graphiteRaised
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = OrbitType.Body.MD,
            fontWeight = FontWeight.Medium,
            color = if (enabled) OrbitTokens.onBluePastel else OrbitTokens.textLow,
        )
    }
}

/**
 * ButtonSecondary — botão secundário Orbit.
 */
@Composable
fun OrbitButtonSecondary(
    label: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(OrbitTokens.graphiteRaised)
            .border(
                1.dp,
                if (enabled) OrbitTokens.border else OrbitTokens.border.copy(alpha = 0.5f),
                RoundedCornerShape(OrbitMetrics.radiusSm),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = com.ethan.orbitlab.ui.theme.Hanken,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (enabled) OrbitTokens.textHiN else OrbitTokens.textLow,
            )
    }
}

/**
 * InputField — campo de input Orbit.
 */
@Composable
fun OrbitInputField(
    value: String,
    onValueChange: (String) -> Unit = {},
    placeholder: String = "",
    label: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = OrbitType.Body.XXS,
                color = OrbitTokens.textMid,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
                .background(OrbitTokens.graphiteRaised)
                .border(
                    1.dp,
                    OrbitTokens.border,
                    RoundedCornerShape(OrbitMetrics.radiusSm),
                )
                .padding(12.dp),
        ) {
            Text(
                text = value.ifBlank { placeholder },
                style = TextStyle(
                    fontFamily = Hanken,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = if (value.isNotBlank()) OrbitTokens.textHiN else OrbitTokens.textLow,
            )
        }
    }
}

/**
 * DividerOrbit — divisor Orbit.
 */
@Composable
fun OrbitDivider(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(OrbitTokens.border.copy(alpha = 0.3f)),
    )
}

/**
 * AvatarOrbit — avatar com status.
 */
@Composable
fun OrbitAvatar(
    name: String,
    src: String? = null,
    size: Dp = 40.dp,
    status: SessionStatus? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(OrbitTokens.graphiteRaised),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(2).uppercase(),
                style = OrbitType.Body.MD.copy(
                    fontSize = (size.value * 0.4f).sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = OrbitTokens.textMid,
            )
        }

        // Status indicator
        status?.let {
            Box(
                modifier = Modifier
                    .size((size.value * 0.3f).dp)
                    .clip(CircleShape)
                    .background(
                        when (it) {
                            SessionStatus.ATIVA -> OrbitTokens.online
                            SessionStatus.PAUSADA -> OrbitTokens.warning
                            SessionStatus.FINALIZADA -> OrbitTokens.textLow
                            SessionStatus.AGENDADA -> OrbitTokens.accent
                        }
                    )
                    .border(
                        2.dp,
                        OrbitTokens.graphiteSurf,
                        CircleShape,
                    )
                    .align(Alignment.BottomEnd),
            )
        }
    }
}

/**
 * PreviewRenderer — renderiza componentes Orbit DS no mockup.
 */
object OrbitDSRenderer {

    /**
     * Renderiza um componente Orbit DS a partir de um tipo string.
     */
    @Composable
    fun render(
        tipo: String,
        props: Map<String, Any?>,
        onClick: () -> Unit = {},
    ): @Composable () -> Unit {
        return when (tipo.lowercase()) {
            "sessionrow" -> {
                val session = SessionData(
                    id = props["id"] as? String ?: "",
                    title = props["title"] as? String ?: "Sessão",
                    subtitle = props["subtitle"] as? String,
                    tags = (props["tags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    duration = props["duration"] as? String,
                )
                val status = when ((props["status"] as? String)?.lowercase()) {
                    "ativa", "active" -> SessionStatus.ATIVA
                    "pausada", "paused" -> SessionStatus.PAUSADA
                    "finalizada", "done" -> SessionStatus.FINALIZADA
                    "agendada", "scheduled" -> SessionStatus.AGENDADA
                    else -> SessionStatus.ATIVA
                }
                val compact = props["compact"] as? Boolean ?: false
                {
        OrbitSessionRow(
            props = SessionRowProps(
                session = session,
                status = status,
                compact = compact,
            ),
            onClick = onClick,
            modifier = Modifier,
        )
                }
            }
            "badgechip", "badge" -> {
                val label = props["label"] as? String ?: "Badge"
                val variant = when ((props["variant"] as? String)?.lowercase()) {
                    "success", "feito" -> BadgeVariant.SUCCESS
                    "warning", "atencao" -> BadgeVariant.WARNING
                    "error", "danger" -> BadgeVariant.ERROR
                    else -> BadgeVariant.INFO
                }
                {
                    OrbitBadgeChip(label = label, variant = variant)
                }
            }
            "buttonprimary", "button" -> {
                val label = props["label"] as? String ?: "Button"
                {
                    OrbitButtonPrimary(label = label, onClick = onClick)
                }
            }
            "buttonsecondary" -> {
                val label = props["label"] as? String ?: "Button"
                {
                    OrbitButtonSecondary(label = label, onClick = onClick)
                }
            }
            "cardluaz" -> {
                val title = props["title"] as? String ?: "Card"
                val subtitle = props["subtitle"] as? String
                {
                    OrbitCardLuaz(
                        title = title,
                        subtitle = subtitle,
                        onClick = onClick,
                    )
                }
            }
            "divider" -> {
                {
                    OrbitDivider()
                }
            }
            "avatar" -> {
                val name = props["name"] as? String ?: "?"
                {
                    OrbitAvatar(name = name)
                }
            }
            else -> {
                {
                    Text(
                        text = "Unknown: $tipo",
                        style = OrbitType.Body.SM,
                        color = OrbitTokens.danger,
                    )
                }
            }
        }
    }
}
