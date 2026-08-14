package com.ethan.orbitlab.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType

/**
 * Tipo de linguagem para syntax highlighting.
 */
enum class CodeLanguage(val displayName: String, val extension: String) {
    JSON("JSON", "json"),
    KOTLIN("Kotlin", "kt"),
    SWIFT("Swift", "swift"),
    TYPESCRIPT("TypeScript", "ts"),
    CSS("CSS", "css"),
    ;

    companion object {
        fun fromExtension(ext: String): CodeLanguage {
            return entries.find { it.extension == ext.lowercase() } ?: JSON
        }
    }
}

/**
 * Código de exemplo para mockup.
 */
val DEFAULT_MOCKUP_CODE = """{ "device": "iphone-15-pro", "bg": "#141820", "components": [] }"""

/**
 * Editor de código com syntax highlighting.
 */
@Composable
fun CodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    language: CodeLanguage = CodeLanguage.JSON,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    var cursorPosition by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusMd))
            .background(OrbitTokens.ink0)
            .padding(16.dp),
    ) {
        // Header com linguagem
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(
                text = language.displayName,
                style = OrbitType.Body.XXS.copy(fontWeight = FontWeight.Medium),
                color = OrbitTokens.textLow,
            )
            Text(
                text = "${code.lines().size} linhas",
                style = OrbitType.Body.XXS,
                color = OrbitTokens.textLow,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // Números de linha
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                val lines = code.lines()
                lines.forEachIndexed { index, _ ->
                    Text(
                        text = "${index + 1}",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = OrbitTokens.textLow.copy(alpha = 0.5f),
                            lineHeight = 20.sp,
                        ),
                        modifier = Modifier.height(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Editor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (readOnly) {
                    SyntaxHighlightedText(
                        code = code,
                        language = language,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    BasicTextField(
                        value = code,
                        onValueChange = { newCode ->
                            onCodeChange(newCode)
                            cursorPosition = newCode.length
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = OrbitTokens.textHiN,
                            lineHeight = 20.sp,
                        ),
                        cursorBrush = SolidColor(OrbitTokens.accent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (code.isEmpty()) {
                                    Text(
                                        text = "// Digite seu código aqui...",
                                        style = TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            color = OrbitTokens.textLow.copy(alpha = 0.5f),
                                        ),
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Texto com syntax highlighting.
 */
@Composable
fun SyntaxHighlightedText(
    code: String,
    language: CodeLanguage,
    modifier: Modifier = Modifier,
) {
    val annotated = remember(code, language) {
        highlightSyntax(code, language)
    }

    Text(
        text = annotated,
        modifier = modifier,
    )
}

private fun highlightSyntax(code: String, language: CodeLanguage): AnnotatedString {
    return buildAnnotatedString {
        val lines = code.lines()

        lines.forEachIndexed { lineIndex, line ->
            var i = 0

            while (i < line.length) {
                when (language) {
                    CodeLanguage.JSON -> highlightJsonLine(line) { style, start, end ->
                        withStyle(style) {
                            append(line.substring(start, end))
                        }
                    }
                    else -> {
                        // Fallback: texto simples
                        append(line.substring(i))
                    }
                }
                i = line.length
            }

            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun highlightJsonLine(
    line: String,
    emit: (SpanStyle, Int, Int) -> Unit,
) {
    var i = 0

    while (i < line.length) {
        when {
            // String (chave ou valor)
            line[i] == '"' -> {
                val start = i
                i++
                while (i < line.length && line[i] != '"') {
                    if (line[i] == '\\' && i + 1 < line.length) i++
                    i++
                }
                if (i < line.length) i++

                // Determina se é chave ou valor
                val afterQuote = line.indexOf(':', start)
                if (afterQuote != -1 && afterQuote < i) {
                    // É chave (vem antes do :)
                    emit(
                        SpanStyle(
                            color = Color(0xFF9CDCFE), // azul claro
                            fontWeight = FontWeight.Medium,
                        ),
                        start,
                        i,
                    )
                } else {
                    // É valor string
                    emit(
                        SpanStyle(color = Color(0xFFCE9178)), // laranja
                        start,
                        i,
                    )
                }
            }

            // Número
            line[i].isDigit() || (line[i] == '-' && i + 1 < line.length && line[i + 1].isDigit()) -> {
                val start = i
                if (line[i] == '-') i++
                while (i < line.length && (line[i].isDigit() || line[i] == '.')) i++
                emit(
                    SpanStyle(color = Color(0xFFB5CEA8)), // verde claro
                    start,
                    i,
                )
            }

            // Booleano/null
            line.substring(i).startsWith("true") -> {
                emit(SpanStyle(color = Color(0xFF569CD6)), i, i + 4) // azul
                i += 4
            }
            line.substring(i).startsWith("false") -> {
                emit(SpanStyle(color = Color(0xFF569CD6)), i, i + 5)
                i += 5
            }
            line.substring(i).startsWith("null") -> {
                emit(SpanStyle(color = Color(0xFF569CD6)), i, i + 4)
                i += 4
            }

            // Pontuação
            line[i] in "{}[],:" -> {
                emit(
                    SpanStyle(color = Color(0xFFD4D4D4)),
                    i,
                    i + 1,
                )
                i++
            }

            else -> {
                i++
            }
        }
    }
}

/**
 * Tela de mockup com editor + preview lado a lado.
 */
@Composable
fun MockupEditorScreen(
    initialCode: String = DEFAULT_MOCKUP_CODE,
    onCodeChange: (String) -> Unit = {},
    onScreenshot: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf(initialCode) }
    var mockupData by remember { mutableStateOf<MockupData?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var selectedDevice by remember { mutableStateOf(DeviceType.IPHONE_15_PRO) }
    var viewMode by remember { mutableIntStateOf(0) } // 0 = split, 1 = code, 2 = preview

    // Parsear JSON quando código mudar
    androidx.compose.runtime.LaunchedEffect(code) {
        onCodeChange(code)
        MockupParser.parse(code).fold(
            onSuccess = { data ->
                mockupData = data
                parseError = null
            },
            onFailure = { e ->
                parseError = e.message
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OrbitTokens.graphiteBg),
    ) {
        // Tabs de visualização
        ScrollableTabRow(
            selectedTabIndex = viewMode,
            containerColor = OrbitTokens.graphiteBg,
            contentColor = OrbitTokens.textMid,
            edgePadding = 16.dp,
        ) {
            Tab(
                selected = viewMode == 0,
                onClick = { viewMode = 0 },
                text = { Text("Split") },
            )
            Tab(
                selected = viewMode == 1,
                onClick = { viewMode = 1 },
                text = { Text("Código") },
            )
            Tab(
                selected = viewMode == 2,
                onClick = { viewMode = 2 },
                text = { Text("Preview") },
            )
        }

        // Conteúdo
        when (viewMode) {
            0 -> {
                // Split view
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    // Editor
                    CodeEditor(
                        code = code,
                        onCodeChange = { code = it },
                        language = CodeLanguage.JSON,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Preview
                    MockupPreviewPanel(
                        mockupData = mockupData,
                        error = parseError,
                        device = selectedDevice,
                        onDeviceChange = { selectedDevice = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
            1 -> {
                // Só código
                CodeEditor(
                    code = code,
                    onCodeChange = { code = it },
                    language = CodeLanguage.JSON,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                )
            }
            2 -> {
                // Só preview
                MockupPreviewPanel(
                    mockupData = mockupData,
                    error = parseError,
                    device = selectedDevice,
                    onDeviceChange = { selectedDevice = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                )
            }
        }
    }
}

/**
 * Painel de preview com device frame.
 */
@Composable
fun MockupPreviewPanel(
    mockupData: MockupData?,
    error: String?,
    device: DeviceType,
    onDeviceChange: (DeviceType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        // Seletor de device
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            DeviceType.entries.take(3).forEach { d ->
                DeviceChip(
                    device = d,
                    selected = d == device,
                    onClick = { onDeviceChange(d) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(OrbitMetrics.radiusMd))
                .background(OrbitTokens.ink0)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            when {
                error != null -> {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Erro de parse",
                            style = OrbitType.Body.MD,
                            color = OrbitTokens.danger,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = OrbitType.Body.XXS,
                            color = OrbitTokens.textMid,
                        )
                    }
                }
                mockupData != null -> {
                    // Placeholder para preview de mockup
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(OrbitTokens.graphiteRaised),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Text(
                            text = "Mockup Preview",
                            style = OrbitType.Body.MD,
                            color = OrbitTokens.textMid,
                        )
                    }
                }
                else -> {
                    Text(
                        text = "Preview",
                        style = OrbitType.Body.MD,
                        color = OrbitTokens.textMid,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceChip(
    device: DeviceType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(OrbitMetrics.radiusXs))
            .background(
                if (selected) OrbitTokens.accent.copy(alpha = 0.2f)
                else OrbitTokens.graphiteRaised
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = device.displayName,
            style = OrbitType.Body.XXS,
            color = if (selected) OrbitTokens.accentText else OrbitTokens.textMid,
        )
    }
}

/**
 * DSL para criar mockups programaticamente (API fluente).
 */
object MockupDSL {

    fun mockup(
        device: DeviceType = DeviceType.IPHONE_15_PRO,
        bg: Color = OrbitTokens.graphiteBg,
        block: MockupBuilder.() -> Unit,
    ): MockupData {
        val builder = MockupBuilder()
        builder.block()
        return MockupData(
            device = device,
            bg = bg,
            components = builder.components,
        )
    }

    fun MockupBuilder.card(
        style: MockupStyle = MockupStyle(),
        children: List<MockupComponent> = emptyList(),
    ): MockupComponent.Card {
        return MockupComponent.Card(
            id = "card_${System.currentTimeMillis()}",
            style = style,
            children = children,
        )
    }

    fun MockupBuilder.text(
        content: String,
        style: MockupStyle = MockupStyle(),
    ): MockupComponent.Text {
        return MockupComponent.Text(
            id = "text_${System.currentTimeMillis()}",
            content = content,
            style = style,
        )
    }

    fun MockupBuilder.button(
        label: String,
        style: MockupStyle = MockupStyle(),
    ): MockupComponent.Button {
        return MockupComponent.Button(
            id = "button_${System.currentTimeMillis()}",
            label = label,
            style = style,
        )
    }

    fun MockupBuilder.spacer(size: Dp = 8.dp): MockupComponent.Spacer {
        return MockupComponent.Spacer(
            id = "spacer_${System.currentTimeMillis()}",
            size = size,
        )
    }

    class MockupBuilder {
        val components = mutableListOf<MockupComponent>()

        fun add(component: MockupComponent) {
            components.add(component)
        }
    }
}
