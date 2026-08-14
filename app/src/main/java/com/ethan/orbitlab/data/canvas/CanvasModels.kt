package com.ethan.orbitlab.data.canvas

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Contrato de elementos do canvas — espelho de
 * `luna-core/src/ferramentas/canvasTypes.ts`.
 *
 * Canvas infinito de elementos manipuláveis (não pixels). A Luna age no canvas
 * adicionando, movendo, estilizando elementos enquanto o usuário observa.
 */

// ═══════════════════════════════════════════════════════════════════
//  TIPO E ENUMS
// ═══════════════════════════════════════════════════════════════════

/** Tipos de elemento suportados no canvas. */
enum class ElementoTipo {
    card,      // container com componentes Orbit DS
    image,     // imagem URL
    shape,     // primitivas geométricas
    text,      // texto editável
    group,     // agrupamento
    connector, // linha entre elementos
    ;

    companion object {
        fun from(raw: String?): ElementoTipo =
            entries.firstOrNull { it.name == raw } ?: card
    }
}

/** Tipos de shape primitiva. */
enum class ShapeType {
    rect,
    circle,
    line,
    triangle,
    ;

    companion object {
        fun from(raw: String?): ShapeType =
            entries.firstOrNull { it.name == raw } ?: rect
    }
}

/** Estilos de linha para connectors. */
enum class LineStyle {
    solid,
    dashed,
    dotted,
    ;

    companion object {
        fun from(raw: String?): LineStyle =
            entries.firstOrNull { it.name == raw } ?: solid
    }
}

/** Âncoras de conexão para connectors. */
enum class Anchor {
    top,
    bottom,
    left,
    right,
    ;

    companion object {
        fun from(raw: String?): Anchor =
            entries.firstOrNull { it.name == raw } ?: top
    }
}

/** Estratégias de auto-layout. */
enum class LayoutEstrategia {
    grid,
    vertical,
    horizontal,
    radial,
    ;

    companion object {
        fun from(raw: String?): LayoutEstrategia =
            entries.firstOrNull { it.name == raw } ?: grid
    }
}

/** Eixo de alinhamento. */
enum class AlinhamentoEixo {
    x,
    y,
    ;

    companion object {
        fun from(raw: String?): AlinhamentoEixo =
            entries.firstOrNull { it.name == raw } ?: x
    }
}

/** Referência de alinhamento. */
enum class AlinhamentoRef {
    left,
    center,
    right,
    top,
    middle,
    bottom,
    ;

    companion object {
        fun from(raw: String?): AlinhamentoRef =
            entries.firstOrNull { it.name == raw } ?: center
    }
}

/** Posição de encaixe. */
enum class EncaixePosicao {
    left,
    right,
    above,
    below,
    inside,
    ;

    companion object {
        fun from(raw: String?): EncaixePosicao =
            entries.firstOrNull { it.name == raw } ?: inside
    }
}

// ═══════════════════════════════════════════════════════════════════
//  STYLES
// ═══════════════════════════════════════════════════════════════════

/** Estilo visual de um elemento. */
data class ElementoStyle(
    val fill: String? = null,           // cor hex ou gradiente
    val strokeColor: String? = null,
    val strokeWidth: Float? = null,
    val shadowX: Float? = null,
    val shadowY: Float? = null,
    val shadowBlur: Float? = null,
    val shadowColor: String? = null,
    val opacity: Float? = null,
    val cornerRadius: Float? = null,
) {
    companion object {
        val EMPTY = ElementoStyle()
    }
}

/** Converte style para mapa Firestore. */
fun ElementoStyle.toFirestoreMap(): Map<String, Any?> {
    val map = linkedMapOf<String, Any?>()
    fill?.let { map["fill"] = it }
    strokeColor?.let { map["strokeColor"] = it }
    strokeWidth?.let { map["strokeWidth"] = it }
    if (shadowX != null || shadowY != null || shadowBlur != null || shadowColor != null) {
        map["shadow"] = linkedMapOf<String, Any?>(
            "x" to shadowX,
            "y" to shadowY,
            "blur" to shadowBlur,
            "color" to shadowColor,
        )
    }
    opacity?.let { map["opacity"] = it }
    cornerRadius?.let { map["cornerRadius"] = it }
    return map
}

/** Converte mapa Firestore para style. */
fun parseElementoStyle(raw: Map<*, *>?): ElementoStyle {
    if (raw == null) return ElementoStyle.EMPTY
    val shadowMap = raw["shadow"] as? Map<*, *>
    return ElementoStyle(
        fill = raw["fill"] as? String,
        strokeColor = raw["strokeColor"] as? String,
        strokeWidth = (raw["strokeWidth"] as? Number)?.toFloat(),
        shadowX = (shadowMap?.get("x") as? Number)?.toFloat(),
        shadowY = (shadowMap?.get("y") as? Number)?.toFloat(),
        shadowBlur = (shadowMap?.get("blur") as? Number)?.toFloat(),
        shadowColor = shadowMap?.get("color") as? String,
        opacity = (raw["opacity"] as? Number)?.toFloat(),
        cornerRadius = (raw["cornerRadius"] as? Number)?.toFloat(),
    )
}

// ═══════════════════════════════════════════════════════════════════
//  BOUNDS
// ═══════════════════════════════════════════════════════════════════

/** Posição e tamanho de um elemento no canvas. */
data class ElementoBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    fun toFirestoreMap(): Map<String, Float> = mapOf(
        "x" to x,
        "y" to y,
        "width" to width,
        "height" to height,
    )

    companion object {
        fun from(raw: Map<*, *>?): ElementoBounds? {
            val m = raw ?: return null
            val x = (m["x"] as? Number)?.toFloat() ?: return null
            val y = (m["y"] as? Number)?.toFloat() ?: return null
            val w = (m["width"] as? Number)?.toFloat() ?: return null
            val h = (m["height"] as? Number)?.toFloat() ?: return null
            return ElementoBounds(x, y, w, h)
        }

        val ZERO = ElementoBounds(0f, 0f, 100f, 100f)
    }
}

// ═══════════════════════════════════════════════════════════════════
//  PROPS ESPECÍFICOS POR TIPO
// ═══════════════════════════════════════════════════════════════════

/** Props para elemento tipo card. */
data class CardProps(
    val componente: String? = null,  // nome do Orbit DS (ex: "SessionRow")
    val componentProps: Map<String, Any?>? = null,
)

/** Props para elemento tipo image. */
data class ImageProps(
    val src: String,
    val cropX: Float? = null,
    val cropY: Float? = null,
    val cropWidth: Float? = null,
    val cropHeight: Float? = null,
)

/** Props para elemento tipo shape. */
data class ShapeProps(
    val shapeType: ShapeType = ShapeType.rect,
    val points: List<Pair<Float, Float>>? = null,  // para lines/polygons
)

/** Props para elemento tipo text. */
data class TextProps(
    val content: String,
    val fontSize: Float = 14f,
    val fontWeight: Int = 400,
    val align: String = "left",
    val color: String? = null,
)

/** Props para elemento tipo connector. */
data class ConnectorProps(
    val fromId: String,
    val toId: String,
    val fromAnchor: Anchor = Anchor.right,
    val toAnchor: Anchor = Anchor.left,
    val lineStyle: LineStyle = LineStyle.solid,
    val lineWidth: Float = 2f,
    val lineColor: String? = null,
)

// ═══════════════════════════════════════════════════════════════════
//  ELEMENTO
// ═══════════════════════════════════════════════════════════════════

/**
 * Elemento do canvas — entidade com ID, bounds, estilo e props específicos.
 */
data class CanvasElement(
    val id: String,
    val tipo: ElementoTipo,
    val bounds: ElementoBounds,
    val rotation: Float = 0f,
    val style: ElementoStyle = ElementoStyle.EMPTY,
    val locked: Boolean = false,
    val visible: Boolean = true,
    // Props específicos por tipo
    val cardProps: CardProps? = null,
    val imageProps: ImageProps? = null,
    val shapeProps: ShapeProps? = null,
    val textProps: TextProps? = null,
    val connectorProps: ConnectorProps? = null,
    // Para groups
    val children: List<String> = emptyList(),
) {
    /** Tipo do elemento como string (para Firestore). */
    val tipoStr: String get() = tipo.name

    fun toFirestoreMap(): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>(
            "tipo" to tipoStr,
            "bounds" to bounds.toFirestoreMap(),
            "rotation" to rotation,
            "style" to style.toFirestoreMap(),
            "locked" to locked,
            "visible" to visible,
        )
        // Props específicos
        when (tipo) {
            ElementoTipo.card -> cardProps?.let {
                val p = linkedMapOf<String, Any?>()
                it.componente?.let { c -> p["componente"] = c }
                it.componentProps?.let { cp -> p["componentProps"] = cp }
                if (p.isNotEmpty()) map["cardProps"] = p
            }
            ElementoTipo.image -> imageProps?.let {
                val p = linkedMapOf<String, Any?>("src" to it.src)
                if (it.cropX != null) p["cropX"] = it.cropX
                if (it.cropY != null) p["cropY"] = it.cropY
                if (it.cropWidth != null) p["cropWidth"] = it.cropWidth
                if (it.cropHeight != null) p["cropHeight"] = it.cropHeight
                map["imageProps"] = p
            }
            ElementoTipo.shape -> shapeProps?.let {
                val p = linkedMapOf<String, Any?>("shapeType" to it.shapeType.name)
                it.points?.let { pts -> p["points"] = pts.map { (x, y) -> listOf(x, y) } }
                map["shapeProps"] = p
            }
            ElementoTipo.text -> textProps?.let {
                val p = linkedMapOf<String, Any?>(
                    "content" to it.content,
                    "fontSize" to it.fontSize,
                    "fontWeight" to it.fontWeight,
                    "align" to it.align,
                )
                it.color?.let { c -> p["color"] = c }
                map["textProps"] = p
            }
            ElementoTipo.connector -> connectorProps?.let {
                map["connectorProps"] = linkedMapOf<String, Any?>(
                    "fromId" to it.fromId,
                    "toId" to it.toId,
                    "fromAnchor" to it.fromAnchor.name,
                    "toAnchor" to it.toAnchor.name,
                    "lineStyle" to it.lineStyle.name,
                    "lineWidth" to it.lineWidth,
                    "lineColor" to it.lineColor,
                )
            }
            ElementoTipo.group -> {
                if (children.isNotEmpty()) map["children"] = children
            }
        }
        return map
    }

    companion object {
        private var seqElemento = 0

        fun novoId(): String {
            seqElemento = (seqElemento + 1) % 1_000_000
            val t = System.currentTimeMillis().toString(36)
            val r = (100000..999999).random().toString(36)
            return "ce_${t}_${r}_$seqElemento"
        }

        /** Cria elemento padrão para testes/debug. */
        fun novo(tipo: ElementoTipo, x: Float = 0f, y: Float = 0f, width: Float = 100f, height: Float = 100f): CanvasElement {
            return CanvasElement(
                id = novoId(),
                tipo = tipo,
                bounds = ElementoBounds(x, y, width, height),
            )
        }
    }
}

/** Parseia um elemento do Firestore. */
@Suppress("UNCHECKED_CAST")
fun parseCanvasElement(id: String, raw: Map<*, *>): CanvasElement {
    val tipo = ElementoTipo.from(raw["tipo"] as? String)
    val boundsMap = raw["bounds"] as? Map<*, *>
    val bounds = ElementoBounds.from(boundsMap) ?: ElementoBounds.ZERO
    val rotation = (raw["rotation"] as? Number)?.toFloat() ?: 0f
    val style = parseElementoStyle(raw["style"] as? Map<*, *>)
    val locked = raw["locked"] as? Boolean ?: false
    val visible = raw["visible"] as? Boolean ?: true
    val childrenRaw = raw["children"] as? List<*>
    val children = childrenRaw?.mapNotNull { it as? String } ?: emptyList()

    // Parse props específicos
    val cardProps = (raw["cardProps"] as? Map<*, *>)?.let {
        CardProps(
            componente = it["componente"] as? String,
            componentProps = (it["componentProps"] as? Map<*, *>)?.mapKeys { e -> e.key as? String ?: "" }?.mapValues { e -> e.value },
        )
    }
    val imageProps = (raw["imageProps"] as? Map<*, *>)?.let {
        ImageProps(
            src = it["src"] as? String ?: "",
            cropX = (it["cropX"] as? Number)?.toFloat(),
            cropY = (it["cropY"] as? Number)?.toFloat(),
            cropWidth = (it["cropWidth"] as? Number)?.toFloat(),
            cropHeight = (it["cropHeight"] as? Number)?.toFloat(),
        )
    }
    val shapeProps = (raw["shapeProps"] as? Map<*, *>)?.let {
        val pointsRaw = it["points"] as? List<*>
        val points: List<Pair<Float, Float>>? = pointsRaw?.mapNotNull { pt ->
            val arr = pt as? List<*>
            if (arr?.size == 2) {
                val x = (arr[0] as? Number)?.toFloat()
                val y = (arr[1] as? Number)?.toFloat()
                if (x != null && y != null) x to y else null
            } else null
        }
        ShapeProps(
            shapeType = ShapeType.from(it["shapeType"] as? String),
            points = points,
        )
    }
    val textProps = (raw["textProps"] as? Map<*, *>)?.let {
        TextProps(
            content = it["content"] as? String ?: "",
            fontSize = (it["fontSize"] as? Number)?.toFloat() ?: 14f,
            fontWeight = (it["fontWeight"] as? Number)?.toInt() ?: 400,
            align = it["align"] as? String ?: "left",
            color = it["color"] as? String,
        )
    }
    val connectorProps = (raw["connectorProps"] as? Map<*, *>)?.let {
        ConnectorProps(
            fromId = it["fromId"] as? String ?: "",
            toId = it["toId"] as? String ?: "",
            fromAnchor = Anchor.from(it["fromAnchor"] as? String),
            toAnchor = Anchor.from(it["toAnchor"] as? String),
            lineStyle = LineStyle.from(it["lineStyle"] as? String),
            lineWidth = (it["lineWidth"] as? Number)?.toFloat() ?: 2f,
            lineColor = it["lineColor"] as? String,
        )
    }

    return CanvasElement(
        id = id,
        tipo = tipo,
        bounds = bounds,
        rotation = rotation,
        style = style,
        locked = locked,
        visible = visible,
        cardProps = cardProps,
        imageProps = imageProps,
        shapeProps = shapeProps,
        textProps = textProps,
        connectorProps = connectorProps,
        children = children,
    )
}

/** Snapshot de versão do workspace. */
data class CanvasVersao(
    val numero: Int,
    val elementos: Map<String, CanvasElement>,
    val autor: String,  // "luna" | uid
    val delta: String? = null,
    val timestamp: Long,
)

// ═══════════════════════════════════════════════════════════════════
//  WORKSPACE
// ═══════════════════════════════════════════════════════════════════

/**
 * Workspace do canvas — artefato visual versionado.
 */
data class CanvasWorkspace(
    val id: String,
    val uid: String,
    val titulo: String,
    val elementos: Map<String, CanvasElement> = emptyMap(),
    val versao: Int = 1,
    val criadoEm: Long = System.currentTimeMillis(),
    val atualizadoEm: Long = System.currentTimeMillis(),
    val versoes: List<CanvasVersao> = emptyList(),
) {
    companion object {
        private var seqWorkspace = 0

        fun novoId(): String {
            seqWorkspace = (seqWorkspace + 1) % 1_000_000
            val t = System.currentTimeMillis().toString(36)
            val r = (100000..999999).random().toString(36)
            return "ws_${t}_${r}_$seqWorkspace"
        }

        fun criar(titulo: String, uid: String): CanvasWorkspace {
            val agora = System.currentTimeMillis()
            return CanvasWorkspace(
                id = novoId(),
                uid = uid,
                titulo = titulo.ifBlank { "Canvas sem título" },
                elementos = emptyMap(),
                versao = 1,
                criadoEm = agora,
                atualizadoEm = agora,
            )
        }
    }

    fun toFirestoreMap(): Map<String, Any?> {
        return linkedMapOf(
            "uid" to uid,
            "titulo" to titulo,
            "versao" to versao,
            "elementoCount" to elementos.size,
            "criadoEm" to criadoEm,
            "atualizadoEm" to atualizadoEm,
        )
    }

    fun elementosToFirestoreMap(): Map<String, Map<String, Any?>> {
        return elementos.mapValues { (_, el) -> el.toFirestoreMap() }
    }
}

/** UI model para listagem de workspaces. */
data class WorkspaceUi(
    val id: String,
    val titulo: String,
    val versao: Int,
    val criadoEm: Long,
    val atualizadoEm: Long,
    val elementoCount: Int = 0,
)

/** Parseia workspace do Firestore. */
@Suppress("UNCHECKED_CAST")
fun parseCanvasWorkspace(id: String, raw: Map<*, *>): CanvasWorkspace {
    val elementosRaw = raw["elementos"] as? Map<*, *>
    @Suppress("UNCHECKED_CAST")
    val elementos: Map<String, CanvasElement> = elementosRaw?.mapNotNull { (elId, elRaw) ->
        val key = elId as? String ?: return@mapNotNull null
        val value = elRaw as? Map<*, *> ?: return@mapNotNull null
        key to parseCanvasElement(key, value)
    }?.toMap() ?: emptyMap()

    val versoesRaw = raw["versoes"] as? List<*>
    @Suppress("UNCHECKED_CAST")
    val versoes = versoesRaw?.mapNotNull { vRaw ->
        val v = vRaw as? Map<*, *> ?: return@mapNotNull null
        val versaoElementosRaw = v["elementos"] as? Map<*, *>
        @Suppress("UNCHECKED_CAST")
        val versaoElementos: Map<String, CanvasElement> = versaoElementosRaw?.mapNotNull { (elId, elRaw2) ->
            val key = elId as? String ?: return@mapNotNull null
            val value = elRaw2 as? Map<*, *> ?: return@mapNotNull null
            key to parseCanvasElement(key, value)
        }?.toMap() ?: emptyMap()
        CanvasVersao(
            numero = (v["numero"] as? Number)?.toInt() ?: 0,
            elementos = versaoElementos,
            autor = v["autor"] as? String ?: "luna",
            delta = v["delta"] as? String,
            timestamp = (v["timestamp"] as? Number)?.toLong()
                ?: (v["timestamp"] as? Timestamp)?.toDate()?.time
                ?: System.currentTimeMillis(),
        )
    } ?: emptyList()

    return CanvasWorkspace(
        id = id,
        uid = raw["uid"] as? String ?: "",
        titulo = (raw["titulo"] as? String)?.ifBlank { "Canvas sem título" } ?: "Canvas sem título",
        elementos = elementos,
        versao = (raw["versao"] as? Number)?.toInt() ?: 1,
        criadoEm = (raw["criadoEm"] as? Number)?.toLong()
            ?: (raw["criadoEm"] as? Timestamp)?.toDate()?.time
            ?: System.currentTimeMillis(),
        atualizadoEm = (raw["atualizadoEm"] as? Number)?.toLong()
            ?: (raw["atualizadoEm"] as? Timestamp)?.toDate()?.time
            ?: System.currentTimeMillis(),
        versoes = versoes,
    )
}

/** Parseia workspace resumido (para listagem). */
fun parseWorkspaceUi(id: String, raw: Map<*, *>): WorkspaceUi {
    val elementosRaw = raw["elementos"] as? Map<*, *>
    val countAgregado = (raw["elementoCount"] as? Number)?.toInt()
    return WorkspaceUi(
        id = id,
        titulo = (raw["titulo"] as? String)?.ifBlank { "Canvas sem título" } ?: "Canvas sem título",
        versao = (raw["versao"] as? Number)?.toInt() ?: 1,
        criadoEm = (raw["criadoEm"] as? Number)?.toLong()
            ?: (raw["criadoEm"] as? Timestamp)?.toDate()?.time
            ?: 0L,
        atualizadoEm = (raw["atualizadoEm"] as? Number)?.toLong()
            ?: (raw["atualizadoEm"] as? Timestamp)?.toDate()?.time
            ?: 0L,
        elementoCount = countAgregado ?: elementosRaw?.size ?: 0,
    )
}

/**
 * Contexto do canvas para passar ao chat da Luna.
 * Usado para dar contexto ao modelo sobre o workspace atual.
 */
data class CanvasChatContext(
    val workspaceId: String,
    val workspaceNome: String,
    val elementoCount: Int,
    val workspaceJson: String? = null, // JSON serializado do workspace para contexto
) {
    /**
     * Gera um prompt de contexto para a Luna.
     */
    fun toContextPrompt(): String {
        return buildString {
            appendLine("## Contexto: Canvas '${workspaceNome}'")
            appendLine("- Workspace ID: $workspaceId")
            appendLine("- Total de elementos: $elementoCount")
            if (!workspaceJson.isNullOrBlank()) {
                appendLine()
                appendLine("Resumo dos elementos visiveis:")
                appendLine(workspaceJson)
            }
            appendLine()
            appendLine("O usuário está editando este canvas. Você pode ajudar com:")
            appendLine("- Adicionar, mover ou estilizar elementos")
            appendLine("- Sugerir layouts ou compositions")
            appendLine("- Explicar conceitos de design UI/UX")
            appendLine("- Gerar código de componentes")
        }
    }
}
