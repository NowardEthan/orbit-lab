package com.ethan.orbitlab.ui.canvas

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType

/**
 * Tipo de device para device frames.
 */
enum class DeviceType(
    val displayName: String,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val frameColor: Color,
    val hasNotch: Boolean = false,
    val hasDynamicIsland: Boolean = false,
    val hasHomeIndicator: Boolean = false,
) {
    IPHONE_15_PRO(
        displayName = "iPhone 15 Pro",
        viewportWidth = 393,
        viewportHeight = 852,
        frameColor = Color(0xFF2C2C2E),
        hasDynamicIsland = true,
    ),
    IPHONE_SE(
        displayName = "iPhone SE",
        viewportWidth = 375,
        viewportHeight = 667,
        frameColor = Color(0xFF1C1C1E),
        hasHomeIndicator = true,
    ),
    PIXEL_8(
        displayName = "Pixel 8",
        viewportWidth = 412,
        viewportHeight = 915,
        frameColor = Color(0xFF1A1A1A),
        hasNotch = true,
    ),
    GALAXY_S24(
        displayName = "Galaxy S24",
        viewportWidth = 360,
        viewportHeight = 780,
        frameColor = Color(0xFF1A1A1A),
        hasNotch = true,
    ),
    DESKTOP_14(
        displayName = "Desktop 14\"",
        viewportWidth = 1440,
        viewportHeight = 900,
        frameColor = Color(0xFF2C2C2E),
    ),
    ;

    val aspectRatio: Float get() = viewportWidth.toFloat() / viewportHeight.toFloat()
}

/**
 * Dados de um mockup.
 */
data class MockupData(
    val device: DeviceType = DeviceType.IPHONE_15_PRO,
    val bg: Color = OrbitTokens.graphiteBg,
    val components: List<MockupComponent> = emptyList(),
)

/**
 * Componente de mockup.
 */
sealed class MockupComponent {
    abstract val id: String
    abstract val style: MockupStyle

    data class Text(
        override val id: String,
        val content: String,
        override val style: MockupStyle = MockupStyle(),
    ) : MockupComponent()

    data class Card(
        override val id: String,
        val children: List<MockupComponent> = emptyList(),
        override val style: MockupStyle = MockupStyle(),
    ) : MockupComponent()

    data class Button(
        override val id: String,
        val label: String,
        override val style: MockupStyle = MockupStyle(),
        val variant: ButtonVariant = ButtonVariant.PRIMARY,
    ) : MockupComponent()

    data class Row(
        override val id: String,
        val children: List<MockupComponent> = emptyList(),
        override val style: MockupStyle = MockupStyle(),
    ) : MockupComponent()

    data class Column(
        override val id: String,
        val children: List<MockupComponent> = emptyList(),
        override val style: MockupStyle = MockupStyle(),
    ) : MockupComponent()

    data class Spacer(
        override val id: String,
        val size: Dp = 8.dp,
        override val style: MockupStyle = MockupStyle(),
    ) : MockupComponent()

    data class Divider(
        override val id: String,
        override val style: MockupStyle = MockupStyle(),
    ) : MockupComponent()

    data class Avatar(
        override val id: String,
        val src: String? = null,
        val size: Dp = 40.dp,
        override val style: MockupStyle = MockupStyle(),
    ) : MockupComponent()

    data class Badge(
        override val id: String,
        val label: String,
        override val style: MockupStyle = MockupStyle(),
        val variant: BadgeVariant = BadgeVariant.INFO,
    ) : MockupComponent()

    data class Image(
        override val id: String,
        val src: String,
        override val style: MockupStyle = MockupStyle(),
    ) : MockupComponent()
}

/**
 * Estilo base para componentes.
 */
data class MockupStyle(
    val margin: Dp = 0.dp,
    val marginTop: Dp = 0.dp,
    val marginBottom: Dp = 0.dp,
    val marginStart: Dp = 0.dp,
    val marginEnd: Dp = 0.dp,
    val padding: Dp = 0.dp,
    val paddingTop: Dp = 0.dp,
    val paddingBottom: Dp = 0.dp,
    val paddingStart: Dp = 0.dp,
    val paddingEnd: Dp = 0.dp,
    val bg: Color? = null,
    val borderRadius: Dp = 0.dp,
    val opacity: Float = 1f,
    val width: Dp? = null,
    val height: Dp? = null,
    val flex: Float? = null,
    val gap: Dp? = null,
    // Text
    val fontSize: Int? = null,
    val fontWeight: Int? = null,
    val color: Color? = null,
    val align: TextAlign = TextAlign.START,
    // Button
    val buttonBg: Color? = null,
    val buttonTextColor: Color? = null,
)

enum class TextAlign { START, CENTER, END }
enum class ButtonVariant { PRIMARY, SECONDARY, GHOST, DESTRUCTIVE }
enum class BadgeVariant { INFO, SUCCESS, WARNING, ERROR }

/**
 * Parser de JSON para MockupData.
 */
object MockupParser {

    fun parse(json: String): Result<MockupData> {
        return runCatching {
            val map = parseJson(json)
            parseMockupData(map)
        }
    }

    private fun parseJson(json: String): Map<String, Any?> {
        // Parser JSON simples (sem dependência externa)
        val result = mutableMapOf<String, Any?>()
        val content = json.trim()

        if (content.startsWith("{")) {
            parseObject(content.substring(1, content.length - 1), result)
        }

        return result
    }

    private fun parseObject(content: String, result: MutableMap<String, Any?>) {
        var i = 0
        while (i < content.length) {
            // Pula espaços
            while (i < content.length && content[i] == ' ') i++
            if (i >= content.length) break

            // Extrai chave
            if (content[i] != '"') { i++; continue }
            i++
            val keyEnd = content.indexOf('"', i)
            if (keyEnd == -1) break
            val key = content.substring(i, keyEnd)
            i = keyEnd + 1

            // Pula espaços e dois-pontos
            while (i < content.length && content[i] in " :") i++

            // Extrai valor
            val value = parseValue(content, i)
            result[key] = value.first
            i = value.second

            // Pula vírgula
            while (i < content.length && content[i] in ", ") i++
        }
    }

    private fun parseValue(content: String, start: Int): Pair<Any?, Int> {
        var i = start
        while (i < content.length && content[i] == ' ') i++

        return when (content[i]) {
            '"' -> {
                i++ // opening quote
                val end = content.indexOf('"', i)
                if (end == -1) Pair(content.substring(i), content.length)
                else Pair(content.substring(i, end), end + 1)
            }
            '{' -> {
                val end = findMatchingBrace(content, i)
                val inner = content.substring(i + 1, end)
                val map = mutableMapOf<String, Any?>()
                parseObject(inner, map)
                Pair(map, end + 1)
            }
            '[' -> {
                val end = findMatchingBracket(content, i)
                val inner = content.substring(i + 1, end)
                val list = parseArray(inner)
                Pair(list, end + 1)
            }
            't', 'f' -> {
                if (content.startsWith("true", i)) Pair(true, i + 4)
                else if (content.startsWith("false", i)) Pair(false, i + 5)
                else Pair(null, i + 1)
            }
            'n' -> {
                if (content.startsWith("null", i)) Pair(null, i + 4)
                else Pair(null, i + 1)
            }
            else -> {
                // Número
                var end = i
                while (end < content.length && content[end] in "0123456789.-") end++
                val str = content.substring(i, end)
                Pair(str.toDoubleOrNull() ?: str, end)
            }
        }
    }

    private fun parseArray(content: String): List<Any?> {
        val result = mutableListOf<Any?>()
        var i = 0
        while (i < content.length) {
            while (i < content.length && content[i] in ", ") i++
            if (i >= content.length) break
            val value = parseValue(content, i)
            result.add(value.first)
            i = value.second
            while (i < content.length && content[i] in ", ") i++
        }
        return result
    }

    private fun findMatchingBrace(content: String, start: Int): Int {
        var depth = 1
        var i = start + 1
        var inString = false
        while (i < content.length && depth > 0) {
            when {
                content[i] == '"' && (i == 0 || content[i - 1] != '\\') -> inString = !inString
                !inString && content[i] == '{' -> depth++
                !inString && content[i] == '}' -> depth--
            }
            i++
        }
        return i - 1
    }

    private fun findMatchingBracket(content: String, start: Int): Int {
        var depth = 1
        var i = start + 1
        var inString = false
        while (i < content.length && depth > 0) {
            when {
                content[i] == '"' && (i == 0 || content[i - 1] != '\\') -> inString = !inString
                !inString && content[i] == '[' -> depth++
                !inString && content[i] == ']' -> depth--
            }
            i++
        }
        return i - 1
    }

    private fun parseMockupData(map: Map<String, Any?>): MockupData {
        val deviceStr = map["device"] as? String ?: "iphone-15-pro"
        val device = when {
            deviceStr.contains("iphone", true) && deviceStr.contains("15", true) -> DeviceType.IPHONE_15_PRO
            deviceStr.contains("iphone", true) && deviceStr.contains("se", true) -> DeviceType.IPHONE_SE
            deviceStr.contains("pixel", true) -> DeviceType.PIXEL_8
            deviceStr.contains("galaxy", true) -> DeviceType.GALAXY_S24
            deviceStr.contains("desktop", true) || deviceStr.contains("14", true) -> DeviceType.DESKTOP_14
            else -> DeviceType.IPHONE_15_PRO
        }

        val bgStr = map["bg"] as? String
        val bg = bgStr?.let { parseColor(it) } ?: OrbitTokens.graphiteBg

        val componentsList = map["components"] as? List<*>
        val components = componentsList?.mapNotNull { parseComponent(it as? Map<*, *>) } ?: emptyList()

        return MockupData(device, bg, components)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseComponent(map: Map<*, *>?): MockupComponent? {
        if (map == null) return null

        val id = "comp_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
        val type = (map["type"] as? String) ?: "card"
        val style = parseStyle(map["style"] as? Map<*, *>)

        return when (type.lowercase()) {
            "text" -> MockupComponent.Text(
                id = id,
                content = (map["content"] as? String) ?: "",
                style = style,
            )
            "card" -> MockupComponent.Card(
                id = id,
                children = (map["children"] as? List<*>)?.mapNotNull { parseComponent(it as? Map<*, *>) } ?: emptyList(),
                style = style,
            )
            "button" -> MockupComponent.Button(
                id = id,
                label = (map["label"] as? String) ?: "Button",
                style = style,
                variant = parseButtonVariant(map["variant"] as? String),
            )
            "row" -> MockupComponent.Row(
                id = id,
                children = (map["children"] as? List<*>)?.mapNotNull { parseComponent(it as? Map<*, *>) } ?: emptyList(),
                style = style,
            )
            "column" -> MockupComponent.Column(
                id = id,
                children = (map["children"] as? List<*>)?.mapNotNull { parseComponent(it as? Map<*, *>) } ?: emptyList(),
                style = style,
            )
            "spacer" -> MockupComponent.Spacer(
                id = id,
                size = parseDp(map["size"]),
                style = style,
            )
            "divider" -> MockupComponent.Divider(
                id = id,
                style = style,
            )
            "avatar" -> MockupComponent.Avatar(
                id = id,
                src = map["src"] as? String,
                size = parseDp(map["size"]),
                style = style,
            )
            "badge" -> MockupComponent.Badge(
                id = id,
                label = (map["label"] as? String) ?: "Badge",
                style = style,
                variant = parseBadgeVariant(map["variant"] as? String),
            )
            "image" -> MockupComponent.Image(
                id = id,
                src = (map["src"] as? String) ?: "",
                style = style,
            )
            else -> MockupComponent.Card(id = id, style = style)
        }
    }

    private fun parseStyle(map: Map<*, *>?): MockupStyle {
        if (map == null) return MockupStyle()

        return MockupStyle(
            margin = parseDp(map["margin"]),
            marginTop = parseDp(map["marginTop"]),
            marginBottom = parseDp(map["marginBottom"]),
            marginStart = parseDp(map["marginStart"]),
            marginEnd = parseDp(map["marginEnd"]),
            padding = parseDp(map["padding"]),
            paddingTop = parseDp(map["paddingTop"]),
            paddingBottom = parseDp(map["paddingBottom"]),
            paddingStart = parseDp(map["paddingStart"]),
            paddingEnd = parseDp(map["paddingEnd"]),
            bg = (map["bg"] as? String)?.let { parseColor(it) },
            borderRadius = parseDp(map["radius"] ?: map["borderRadius"]),
            opacity = (map["opacity"] as? Double)?.toFloat() ?: 1f,
            width = parseDp(map["width"]),
            height = parseDp(map["height"]),
            gap = parseDp(map["gap"]),
            fontSize = (map["fontSize"] as? Double)?.toInt(),
            fontWeight = (map["fontWeight"] as? Double)?.toInt(),
            color = (map["color"] as? String)?.let { parseColor(it) },
            align = parseTextAlign(map["align"] as? String),
            buttonBg = (map["buttonBg"] as? String)?.let { parseColor(it) },
            buttonTextColor = (map["buttonTextColor"] as? String)?.let { parseColor(it) },
        )
    }

    private fun parseDp(value: Any?): Dp {
        return when (value) {
            is Number -> value.toDouble().dp
            is String -> value.replace("dp", "").replace("px", "").toDoubleOrNull()?.dp ?: 8.dp
            else -> 0.dp
        }
    }

    private fun parseColor(str: String?): Color? {
        if (str == null) return null
        val clean = str.trim()
        return try {
            if (clean.startsWith("#")) {
                val hex = clean.removePrefix("#")
                val colorLong = when (hex.length) {
                    3 -> "FF${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}".toLong(16)
                    6 -> "FF$hex".toLong(16)
                    8 -> hex.toLong(16)
                    else -> return null
                }
                Color(colorLong.toInt())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseTextAlign(str: String?): TextAlign {
        return when (str?.lowercase()) {
            "center" -> TextAlign.CENTER
            "end", "right" -> TextAlign.END
            else -> TextAlign.START
        }
    }

    private fun parseButtonVariant(str: String?): ButtonVariant {
        return when (str?.lowercase()) {
            "secondary" -> ButtonVariant.SECONDARY
            "ghost" -> ButtonVariant.GHOST
            "destructive" -> ButtonVariant.DESTRUCTIVE
            else -> ButtonVariant.PRIMARY
        }
    }

    private fun parseBadgeVariant(str: String?): BadgeVariant {
        return when (str?.lowercase()) {
            "success", "feito", "ok" -> BadgeVariant.SUCCESS
            "warning", "atencao" -> BadgeVariant.WARNING
            "error", "danger" -> BadgeVariant.ERROR
            else -> BadgeVariant.INFO
        }
    }
}

/**
 * Device frame do iPhone 15 Pro.
 */
@Composable
fun IPhoneFrame(
    device: DeviceType,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = true

    Box(modifier = modifier) {
        // Frame externo
        Box(
            modifier = Modifier
                .width((device.viewportWidth * 0.5f).dp)
                .height((device.viewportHeight * 0.5f).dp)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA)
                ),
        ) {
            // Dynamic Island ou Notch
            if (device.hasDynamicIsland) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .width(120.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black),
                )
            } else if (device.hasNotch) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .width(150.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(Color.Black),
                )
            }

            // Viewport (tela)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (device.hasDynamicIsland) 52.dp else if (device.hasNotch) 44.dp else 8.dp)
                    .padding(horizontal = 4.dp)
                    .width((device.viewportWidth * 0.5f - 8).dp)
                    .height((device.viewportHeight * 0.5f - 60).dp)
                    .clip(RoundedCornerShape(35.dp))
                    .background(OrbitTokens.graphiteBg),
            ) {
                content()
            }

            // Barra de status simulada
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (device.hasDynamicIsland) 18.dp else if (device.hasNotch) 14.dp else 14.dp)
                    .width((device.viewportWidth * 0.5f - 60).dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "9:41",
                    style = OrbitType.Body.XXS.copy(fontWeight = FontWeight.Medium),
                    color = OrbitTokens.textHiN,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = OrbitTokens.textHiN,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = OrbitTokens.textHiN,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.BatteryFull,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = OrbitTokens.textHiN,
                    )
                }
            }

            // Home indicator
            if (device.hasHomeIndicator) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .width(134.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.3f)),
                )
            }
        }
    }
}

/**
 * Widget de controle do mockup.
 */
@Composable
fun MockupControls(
    onScreenshot: () -> Unit,
    onCopyCode: () -> Unit,
    onDeviceChange: (DeviceType) -> Unit,
    currentDevice: DeviceType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(OrbitTokens.graphiteSurf.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Device selector
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(OrbitMetrics.radiusXs))
                .background(OrbitTokens.graphiteRaised)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = currentDevice.displayName,
                style = OrbitType.Body.XXS,
                color = OrbitTokens.textMid,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Screenshot
        Icon(
            imageVector = Icons.Default.Screenshot,
            contentDescription = "Screenshot",
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(OrbitTokens.graphiteRaised)
                .padding(4.dp),
            tint = OrbitTokens.textMid,
        )
    }
}

/**
 * Preview renderer de mockup.
 */
@Composable
fun MockupPreview(
    data: MockupData,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(data.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(
                data.components.filterIsInstance<MockupComponent.Spacer>().firstOrNull()?.size ?: 8.dp
            ),
        ) {
            data.components.forEach { component ->
                when (component) {
                    is MockupComponent.Text -> RenderText(component)
                    is MockupComponent.Card -> RenderCard(component)
                    is MockupComponent.Button -> RenderButton(component)
                    is MockupComponent.Row -> RenderRow(component)
                    is MockupComponent.Column -> RenderColumn(component)
                    is MockupComponent.Spacer -> Spacer(modifier = Modifier.height(component.size))
                    is MockupComponent.Divider -> RenderDivider(component)
                    is MockupComponent.Avatar -> RenderAvatar(component)
                    is MockupComponent.Badge -> RenderBadge(component)
                    is MockupComponent.Image -> RenderImage(component)
                }
            }
        }
    }
}

@Composable
private fun RenderText(comp: MockupComponent.Text) {
    Text(
        text = comp.content,
        style = OrbitType.Body.MD.copy(
            fontSize = (comp.style.fontSize ?: 14).sp,
            fontWeight = FontWeight(comp.style.fontWeight ?: 400),
        ),
        color = comp.style.color ?: OrbitTokens.textHiN,
        modifier = Modifier
            .padding(
                start = comp.style.marginStart,
                end = comp.style.marginEnd,
                top = comp.style.marginTop,
                bottom = comp.style.marginBottom,
            ),
    )
}

@Composable
private fun RenderCard(comp: MockupComponent.Card) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = comp.style.marginStart,
                end = comp.style.marginEnd,
                top = comp.style.marginTop,
                bottom = comp.style.marginBottom,
            )
            .clip(RoundedCornerShape(comp.style.borderRadius))
            .background(comp.style.bg ?: OrbitTokens.graphiteSurf)
            .padding(comp.style.padding),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            comp.children.forEach { child ->
                when (child) {
                    is MockupComponent.Text -> RenderText(child)
                    is MockupComponent.Button -> RenderButton(child)
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun RenderButton(comp: MockupComponent.Button) {
    val bgColor = comp.style.buttonBg ?: when (comp.variant) {
        ButtonVariant.PRIMARY -> OrbitTokens.accent
        ButtonVariant.SECONDARY -> OrbitTokens.graphiteRaised
        ButtonVariant.GHOST -> Color.Transparent
        ButtonVariant.DESTRUCTIVE -> OrbitTokens.danger
    }
    val textColor = comp.style.buttonTextColor ?: when (comp.variant) {
        ButtonVariant.PRIMARY -> OrbitTokens.onBluePastel
        ButtonVariant.DESTRUCTIVE -> Color.White
        else -> OrbitTokens.textHiN
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OrbitMetrics.radiusSm))
            .background(bgColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = comp.label,
            style = OrbitType.Body.MD.copy(fontWeight = FontWeight.Medium),
            color = textColor,
        )
    }
}

@Composable
private fun RenderRow(comp: MockupComponent.Row) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = comp.style.marginStart,
                end = comp.style.marginEnd,
                top = comp.style.marginTop,
                bottom = comp.style.marginBottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(comp.style.gap ?: 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        comp.children.forEach { child ->
            when (child) {
                is MockupComponent.Avatar -> RenderAvatar(child)
                is MockupComponent.Text -> RenderText(child)
                is MockupComponent.Badge -> RenderBadge(child)
                else -> {}
            }
        }
    }
}

@Composable
private fun RenderColumn(comp: MockupComponent.Column) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = comp.style.marginStart,
                end = comp.style.marginEnd,
                top = comp.style.marginTop,
                bottom = comp.style.marginBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(comp.style.gap ?: 8.dp),
    ) {
        comp.children.forEach { child ->
            when (child) {
                is MockupComponent.Text -> RenderText(child)
                is MockupComponent.Card -> RenderCard(child)
                else -> {}
            }
        }
    }
}

@Composable
private fun RenderDivider(comp: MockupComponent.Divider) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = comp.style.marginStart,
                end = comp.style.marginEnd,
                top = comp.style.marginTop,
                bottom = comp.style.marginBottom,
            )
            .height(1.dp)
            .background(OrbitTokens.border),
    )
}

@Composable
private fun RenderAvatar(comp: MockupComponent.Avatar) {
    Box(
        modifier = Modifier
            .size(comp.size)
            .clip(CircleShape)
            .background(OrbitTokens.graphiteRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (comp.src != null) {
            // TODO: AsyncImage
            Icon(
                imageVector = Icons.Default.Brightness1,
                contentDescription = null,
                modifier = Modifier.size(comp.size * 0.5f),
                tint = OrbitTokens.textMid,
            )
        } else {
            Text(
                text = "?",
                style = OrbitType.Body.MD,
                color = OrbitTokens.textMid,
            )
        }
    }
}

@Composable
private fun RenderBadge(comp: MockupComponent.Badge) {
    val bgColor = when (comp.variant) {
        BadgeVariant.INFO -> OrbitTokens.accent.copy(alpha = 0.2f)
        BadgeVariant.SUCCESS -> OrbitTokens.online.copy(alpha = 0.2f)
        BadgeVariant.WARNING -> OrbitTokens.warning.copy(alpha = 0.2f)
        BadgeVariant.ERROR -> OrbitTokens.danger.copy(alpha = 0.2f)
    }
    val textColor = when (comp.variant) {
        BadgeVariant.INFO -> OrbitTokens.accentText
        BadgeVariant.SUCCESS -> OrbitTokens.online
        BadgeVariant.WARNING -> OrbitTokens.warning
        BadgeVariant.ERROR -> OrbitTokens.danger
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = comp.label,
            style = OrbitType.Body.XXS,
            color = textColor,
        )
    }
}

@Composable
private fun RenderImage(comp: MockupComponent.Image) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(comp.style.height ?: 120.dp)
            .clip(RoundedCornerShape(comp.style.borderRadius))
            .background(OrbitTokens.graphiteRaised),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = OrbitTokens.textLow,
        )
    }
}
