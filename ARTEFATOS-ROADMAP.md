# Artefatos — Notion da Luna (roadmap)

> Editor de artefatos por **blocos tipados** (estilo Notion), compartilhado entre o humano (OrbitLab) e as mãos da Luna (luna-core). Markdown continua como **projeção** (export + bridge).

## Princípios

1. **Uma verdade: blocos.** `conteudo` (MD) é gerado a partir de `blocos`.
2. **Luna e humano no mesmo documento.** Ela edita por `blocoId`; você edita no app.
3. **Migrar sem perder a estante.** Docs v1 abrem e convertem MD → blocos; o save grava `schemaVersion: 2`.
4. **Ship por fatias.** Publish lab só quando pedir.

## Fases

| Fase | Status | DoD |
|------|--------|-----|
| **N0** Schema + bridge | Feito | `schemaVersion`, `Bloco`, `mdToBlocos`/`blocosToMd`, migrate on save (core + Lab) |
| **N1** Editor visual | Feito | Lista de blocos, Enter/Backspace, slash menu (`ui/artefato/`) |
| **N2** Tools Luna | Feito | `ler_bloco`, `inserir_blocos`, `editar_bloco_artefato` + diretriz de continuação |
| **N3** Página produto | Feito | Outline (índice), criar pela Galeria (+), callout/empty state |

## Modelo Firestore

`users/{uid}/documentos/{id}`:

| Campo | Papel |
|-------|--------|
| `schemaVersion` | `1` = MD legado; `2` = blocos |
| `blocos` | array ordenado de blocos tipados |
| `conteudo` | projeção MD |
| `titulo`, `canone`, `conversaId`, `versoes` | iguais |

## Tools (agêntico)

| Tool | Uso |
|------|-----|
| `ler_estrutura` | índice + `blocoId` dos headings |
| `ler_bloco` | um bloco por id |
| `inserir_blocos` | **continuação** (após `after_id`) — preferida vs reescrever tudo |
| `editar_bloco_artefato` | muda texto/props de um id (nome evita colisão com `editar_bloco` da rotina) |
| `editar_trecho_artefato` | bridge MD (ainda útil) |

## Fora do MVP

- Databases / kanban / nested pages
- Drag-and-drop rico
- Editor na bolha
- Collab realtime além do listener Firestore

## Código

| Onde | O quê |
|------|--------|
| `core/.../artefatoBlocos.ts` | tipos + bridge MD |
| `core/.../firestoreDocumentos.ts` | CRUD v2 |
| `core/.../maosDosDocumentos.ts` | tools de bloco |
| `OrbitLab/.../data/artefato/` | espelho Kotlin |
| `OrbitLab/.../ui/artefato/` | editor + outline |

---

# Orbit Canvas — workspace visual para a Luna

> Canvas infinito de **elementos manipuláveis** (não pixels). A Luna age no canvas como
> agente — move, adiciona, calcula, arruma — enquanto o usuário observa e edita.
> Persistido e versionado no Firestore.

## Visão

```
┌─────────────────────────────────────────────────────────┐
│  Canvas Workspace                                         │
│  ┌──────────┐   ┌─────────────────────┐                │
│  │ Card     │   │ [Image]              │                │
│  │ Luz      │   │                     │                │
│  └────┬─────┘   └─────────────────────┘                │
│       │ ↕ resize/move                                   │
│  ┌────▼─────────────────────┐                          │
│  │ Shape (gradient)         │                          │
│  └───────────────────────────┘                          │
│                                                           │
│  Luna: "Posicionei o card acima da imagem"              │
└─────────────────────────────────────────────────────────┘
```

**DNA do produto:**
- Elementos com posição, tamanho, estilo (não raster)
- Luna autônoma + narração (vê o que ela faz)
- Ligado ao Orbit DS (componentes reais no canvas)
- Persistência com versionamento (git-like)
- Cálculo de layout (grid, align, distribute)

## Princípios

1. **Elementos > Pixels.** Cada objeto é uma entidade com ID, bounds, estilo.
2. **Canvas como artefato.** Versionado no Firestore como `workspace`.
3. **Luna verificada.** Ações visuais aparecem no stream + timeline (A4.2).
4. **Orbit DS integrado.** Componentes reais podem viver no canvas.
5. **Ship por fatias.** Não fazer tudo de uma vez.

## Modelo de dados

### Elemento base

```typescript
interface CanvasElement {
  id: string;              // formato: ce_${timestamp}_${random}
  tipo: ElementoTipo;
  bounds: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
  rotation?: number;       // graus, default 0
  style: ElementoStyle;
  props: TipoEspecificoProps;  // depende do tipo
  children?: string[];    // IDs de filhos (para group)
  locked?: boolean;
  visible?: boolean;
}

type ElementoTipo =
  | "card"       // container com Orbit DS
  | "image"      // imagem URL
  | "shape"      // primitivas geométricas
  | "text"       // texto editável
  | "group"      // agrupamento
  | "connector"; // linha entre elementos

interface ElementoStyle {
  fill?: string;           // cor hex ou gradiente
  stroke?: { color: string; width: number };
  shadow?: { x: number; y: number; blur: number; color: string };
  opacity?: number;        // 0-1
  cornerRadius?: number;
}
```

### Workspace

```typescript
interface CanvasWorkspace {
  id: string;
  uid: string;
  titulo: string;
  elementos: Record<string, CanvasElement>;  // por ID
  versao: number;
  criadoEm: Timestamp;
  atualizadoEm: Timestamp;
  versoes?: CanvasVersao[];  // histórico
}

interface CanvasVersao {
  numero: number;
  elementos: Record<string, CanvasElement>;
  autor: "luna" | uid;
  delta?: string;  // descrição da mudança
  timestamp: Timestamp;
}
```

### Props por tipo

| Tipo | Props específicos |
|------|-------------------|
| `card` | `componente?: string` (nome do Orbit DS), `componentProps?: Record<string, unknown>` |
| `image` | `src: string`, `crop?: { x, y, w, h }`, `filters?: ImageFilters` |
| `shape` | `shapeType: "rect" \| "circle" \| "line" \| "triangle"`, `points?: Point[]` |
| `text` | `content: string`, `fontSize: number`, `fontWeight: number`, `align: "left" \| "center" \| "right"` |
| `connector` | `fromId: string`, `toId: string`, `fromAnchor: "top" \| "bottom" \| "left" \| "right"`, `toAnchor: string`, `lineStyle: "solid" \| "dashed" \| "dotted"` |

## Ferramentas da Luna (luna-core)

### CRUD de elementos

| Tool | Params | Descrição |
|------|--------|-----------|
| `criar_canvas` | `titulo` | Cria workspace novo |
| `ler_canvas` | `id` | Retorna workspace com elementos |
| `adicionar_elemento` | `canvasId`, `tipo`, `bounds`, `props`, `style` | Adiciona elemento |
| `mover_elemento` | `canvasId`, `elementoId`, `para: { x, y }`, `duracao_ms?` | Move com animação opcional |
| `redimensionar_elemento` | `canvasId`, `elementoId`, `bounds` | Resize |
| `estilizar_elemento` | `canvasId`, `elementoId`, `style` | Atualiza estilo |
| `remover_elemento` | `canvasId`, `elementoId` | Remove |
| `agrupar_elementos` | `canvasId`, `elementoIds[]` | Cria group |
| `desagrupar_elementos` | `canvasId`, `groupId` | Explode group |

### Layout e cálculo

| Tool | Params | Descrição |
|------|--------|-----------|
| `calcular_layout` | `canvasId`, `elementoIds[]`, `estrategia`, `gap?` | Auto-arrange |
| `alinhar_elementos` | `canvasId`, `elementoIds[]`, `eixo: "x" \| "y"`, `ref: "left" \| "center" \| "right" \| "top" \| "middle" \| "bottom"` | Alinha |
| `distribuir_elementos` | `canvasId`, `elementoIds[]`, `eixo: "x" \| "y"`, `gap` | Espaço igual |
| `encaixar_elemento` | `canvasId`, `elementoId`, `referenciaId`, `posicao: "left" \| "right" \| "above" \| "below" \| "inside"` | Snap to |

### Versionamento

| Tool | Params | Descrição |
|------|--------|-----------|
| `historico_canvas` | `canvasId` | Lista versões |
| `restaurar_versao` | `canvasId`, `versaoNumero` | Restaura snapshot |

## Estrutura Firestore

```
users/{uid}/workspaces/{workspaceId}
  ├── titulo, uid, versao, criadoEm, atualizadoEm
  └── elementos/{elementoId}
      ├── bounds, tipo, style, props, rotation, locked, visible
      └── children[] (só para groups)
```

**Subcoleção de versões:**

```
users/{uid}/workspaces/{workspaceId}/versoes/{versaoNumero}
  ├── numero, autor, elementos (snapshot), delta, timestamp
```

## Fases de implementação

| Fase | Status | DoD | Prioridade |
|------|--------|-----|------------|
| **C0** Schema + CRUD | ✅ | Tipos, modelo Firestore, criar/ler workspace | Alta |
| **C1** Canvas UI base | ✅ | `InfiniteCanvas`, pan/zoom, renderizar elementos | Alta |
| **C2** Interação usuário | ✅ | Selecionar, mover, resize via touch | Alta |
| **C3** Tools Luna (CRUD) | ✅ | `adicionar_elemento`, `mover_elemento`, etc no core | Alta |
| **C4** Stream visual | ✅ | Timeline Luna, ações animadas, narração | Média |
| **C5** Layout tools | ✅ | Alinhar, distribuir, auto-layout, snap | Média |
| **C6** Orbit DS components | ✅ | SessionRow, Badge, Button, Card no preview | Baixa |
| **C7** Versionamento | ✅ | Undo/redo, histórico, diff, snapshots | Baixa |
| **C8** Code Mockup Engine | ✅ | Parser JSON, device frames, editor, screenshot | Alta |

**Progresso:** Todas as fases implementadas. Canvas completo com mockup engine mobile-first.

## Integração com A4.2 (Narração)

Quando a Luna age no canvas, o stream mostra:

```
Vou posicionar o card acima da imagem.
  · Movendo Card (100ms)
  · Movendo Card (400ms)
Pronto — alinhei os dois elementos.
```

Eventos SSE:
```typescript
{ tipo: "canvas_acao", acao: "mover", elementoId: "ce_xxx", de: {x,y}, para: {x,y}, duracao_ms: 400 }
```

---

# C8 — Code Mockup Engine (mobile-first)

> Motor de mockups visuais alimentado por código. A Luna gera código (JSON, CSS-like, DSL)
> e o canvas renderiza preview interativo em tempo real. Mobile-first — simula device frames
> (iPhone, Android) com zoom/screenshot.

## Visão

```
┌─────────────────────────────────────────────────────────────────┐
│  Canvas Workspace                                               │
│                                                                 │
│  ┌───────────────────────┐  ┌──────────────────────────────┐  │
│  │  Code Editor          │  │  Device Frame (iPhone 15)    │  │
│  │  ───────────────────  │  │  ┌────────────────────────┐ │  │
│  │  {                   │  │  │                        │ │  │
│  │    type: "card",     │  │  │   Card Luz             │ │  │
│  │    bg: "#1a1a2e",    │  │  │   ──────────           │ │  │
│  │    padding: 16,      │  │  │   SessionRow           │ │  │
│  │    children: [...]   │  │  │                        │ │  │
│  │  }                   │  │  └────────────────────────┘ │  │
│  └───────────────────────┘  └──────────────────────────────┘  │
│                                                                 │
│  [Copy Code] [Screenshot] [Export]                               │
└─────────────────────────────────────────────────────────────────┘
```

**DNA:**
- A Luna **gera código declarativo** → canvas **renderiza visual**
- Mobile-first: device frames reais (iPhone, Pixel, Galaxy)
- Preview ao vivo: edição de código → atualização instantânea
- Screenshot: exporta imagem do device frame
- Ligado ao Orbit DS: componentes reais no preview

## DSL de Mockup

### Formato JSON (recomendado)

```json
{
  "device": "iphone-15-pro",
  "bg": "#141820",
  "viewport": {
    "width": 393,
    "height": 852
  },
  "components": [
    {
      "type": "SessionRow",
      "props": { "session": "...", "status": "ativa" },
      "style": { "marginBottom": 8 }
    },
    {
      "type": "card",
      "style": { "bg": "#1a1a2e", "radius": 16, "padding": 16 },
      "children": [
        { "type": "text", "content": "Título", "style": { "fontSize": 18, "bold": true } },
        { "type": "button", "label": "Começar", "style": { "bg": "#4B75F2" } }
      ]
    }
  ]
}
```

### Propriedades suportadas

| Categoria | Props | Descrição |
|-----------|-------|-----------|
| **Layout** | `margin`, `padding`, `gap`, `width`, `height` | Espaçamento e dimensões |
| **Visual** | `bg`, `border`, `radius`, `shadow`, `opacity` | Aparência |
| **Typography** | `fontSize`, `fontWeight`, `color`, `align` | Texto |
| **Position** | `position`, `top`, `left`, `right`, `bottom` | Posicionamento |
| **Flex** | `flex`, `direction`, `justify`, `align` | Flexbox |
| **Grid** | `gridCols`, `gridGap` | Grid layout |

### Componentes disponíveis

| Componente | Props | Descrição |
|-----------|-------|-----------|
| `SessionRow` | `session`, `status` | Linha de sessão (Orbit DS) |
| `Card` | `children`, `style` | Container card |
| `Text` | `content`, `style` | Texto |
| `Button` | `label`, `style`, `onPress` | Botão |
| `Image` | `src`, `style` | Imagem |
| `Avatar` | `src`, `size`, `status` | Avatar |
| `Badge` | `label`, `variant` | Badge/chip |
| `Divider` | `style` | Separador |
| `List` | `items`, `renderItem` | Lista |
| `Row` | `children`, `style` | Linha flex |
| `Column` | `children`, `style` | Coluna flex |
| `Spacer` | `size` | Espaço vazio |

## Device Frames

| Device | Viewport | Frame |
|--------|----------|-------|
| iPhone 15 Pro | 393×852 | Notched, titanium |
| iPhone SE | 375×667 | Home button |
| Pixel 8 | 412×915 | Punch hole |
| Galaxy S24 | 360×780 | Center punch |
| Desktop 14" | 1440×900 | Browser chrome |
| Custom | W×H | N/A |

## Fases

| Fase | Status | DoD | Prioridade |
|------|--------|-----|------------|
| **C8.1** DSL parser | ✅ | JSON → AST, validação, propriedades | Alta |
| **C8.2** Device frames | ✅ | iPhone/Pixel frames, zoom, pan | Alta |
| **C8.3** Live preview | ✅ | Renderização em tempo real | Alta |
| **C8.4** Code editor | ✅ | Editor syntax-highlight + preview | Média |
| **C8.5** Screenshot | ✅ | Export PNG do device frame | Média |
| **C8.6** Orbit DS integration | ✅ | SessionRow, Badge, Button, Card no preview | Média |

**C8 implementado:** Parser JSON, device frames, live preview, editor com syntax highlighting, screenshot export, Orbit DS components.

## Ferramentas Luna

| Tool | Params | Descrição |
|------|--------|-----------|
| `criar_mockup` | `device?`, `json` | Cria mockup a partir de JSON |
| `atualizar_mockup` | `canvasId`, `json` | Atualiza preview em tempo real |
| `renderizar_componente` | `tipo`, `props`, `style` | Renderiza componente único |
| `tirar_screenshot` | `canvasId` | Gera imagem do preview |

## Código

| Onde | O quê |
|------|--------|
| `core/.../ferramentas/canvasFerramentas.ts` | Tipos TS + ferramentas Luna |
| `OrbitLab/.../data/canvas/CanvasModels.kt` | Tipos Kotlin |
| `OrbitLab/.../data/canvas/FirestoreWorkspaces.kt` | CRUD Firestore |
| `OrbitLab/.../data/canvas/WorkspaceRepository.kt` | Repositório StateFlow |
| `OrbitLab/.../ui/canvas/InfiniteCanvas.kt` | Canvas infinito pan/zoom |
| `OrbitLab/.../ui/canvas/CanvasElementView.kt` | Elementos interativos |
| `OrbitLab/.../ui/canvas/CanvasToolbar.kt` | Toolbar + minimap |
| `OrbitLab/.../ui/canvas/CanvasWorkspaceScreen.kt` | Tela workspace |
| `OrbitLab/.../ui/canvas/CanvasGalleryScreen.kt` | Galeria workspaces |
| `OrbitLab/.../ui/canvas/MockupEngine.kt` | Parser + device frames + renderer |
| `OrbitLab/.../ui/canvas/CodeEditor.kt` | Editor + syntax highlight |
| `OrbitLab/.../ui/canvas/ScreenshotCapture.kt` | Export PNG |
| `OrbitLab/.../ui/canvas/CanvasStream.kt` | Timeline Luna + ações |
| `OrbitLab/.../ui/canvas/LayoutTools.kt` | Alinhar/distribuir/snap |
| `OrbitLab/.../ui/canvas/OrbitDSComponents.kt` | Componentes Orbit DS |
| `OrbitLab/.../ui/canvas/CanvasVersioning.kt` | Undo/redo + histórico |

## Ligação com Artefatos

Canvas e Artefatos são **unidades diferentes do mesmo produto** (workspace visual vs documento linear). Compartilham:
- Mesma fonte de dados (`users/{uid}/`)
- Mesma Luna (ferramentas unificadas)
- Mesma UX de chat (timeline)

---

# D1 — Motor 3D Low Poly (futuro)

> Motor de geometria 3D declarativo para a Luna criar modelos low poly.
> Diferente do Meshy (geração de malha por IA), aqui a Luna trabalha com
> **cálculos explícitos** de vértices, faces, normais e transforms.

## Visão

```
┌─────────────────────────────────────────────────────────────────┐
│  Luna: "Cria um cubo de 8x8x8 com rotação suave no eixo Y"    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    JSON 3D Declarativo
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  3D Engine (Compose Canvas 3D)                                   │
│                                                                 │
│  Scene Graph (hierarchy de transforms)                          │
│       ↓                                                         │
│  Render Pipeline: Vertices → Faces → Normals → Lighting          │
│                                                                 │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                │
│  │  Cube    │ →  │  Plane   │ →  │ Pyramid  │ → Scene         │
│  │  8x8x8  │    │  4x0.5   │    │  5 faces │                 │
│  └──────────┘    └──────────┘    └──────────┘                │
└─────────────────────────────────────────────────────────────────┘
```

**DNA:**
- Geometria explícita (não malha gerada por IA)
- Low poly por design (faces triangulares mínimas)
- transforms hierárquicos (pai/filho)
- Iluminação Phong básica
- Export OBJ simplificado

## DSL 3D

### Formato JSON

```json
{
  "scene": {
    "camera": { "position": [0, 5, 10], "target": [0, 0, 0] },
    "ambient": 0.3,
    "lights": [
      { "type": "directional", "direction": [1, -1, 1], "intensity": 0.8 }
    ]
  },
  "objects": [
    {
      "id": "cube1",
      "type": "cube",
      "size": [8, 8, 8],
      "position": [0, 4, 0],
      "rotation": [0, 45, 0],
      "material": { "color": "#4B75F2", "roughness": 0.5 }
    }
  ]
}
```

### Primitivos Suportados

| Primitivo | Params | Geometria |
|-----------|--------|-----------|
| `cube` | `size: [w, h, d]` | 12 triângulos, 8 vértices |
| `sphere` | `radius`, `segments` | ~segmentos² triângulos |
| `cylinder` | `radius`, `height`, `segments` | ~segments×2+2 triângulos |
| `cone` | `radius`, `height`, `segments` | ~segments+1 triângulos |
| `pyramid` | `base`, `height` | 4 faces laterais + base |
| `torus` | `radius`, `tube`, `radialSeg`, `tubularSeg` | ~radialSeg×tubularSeg |
| `plane` | `size: [w, h]` | 2 triângulos |

### Geometria Manual

```json
{
  "type": "custom",
  "vertices": [
    [0, 0, 0], [1, 0, 0], [0.5, 1, 0],
    [0.5, 0, 1], [1, 0, 1], [0.5, 1, 1]
  ],
  "faces": [
    [0, 1, 2],
    [3, 5, 4]
  ],
  "normals": [
    [0, 0, 1], [0, 0, 1]
  ],
  "uvs": [
    [0, 0], [1, 0], [0.5, 1],
    [0, 0], [1, 0], [0.5, 1]
  ]
}
```

## Pipeline de Renderização

```
JSON 3D
   ↓
Parser (valida geometria)
   ↓
Scene Graph (transform hierarchy)
   ↓
Geometry Processor (vertices + transforms)
   ↓
Normal Calculator (por face)
   ↓
Lighting (Phong: ambient + diffuse)
   ↓
Rasterizer (Canvas 2D simulando 3D ou OpenGL)
   ↓
Frame Buffer
```

## Fases

| Fase | Status | DoD | Prioridade |
|------|--------|-----|------------|
| **D1.1** Core 3D | ⬜ | Vertices, faces, scene graph, transforms | Alta |
| **D1.2** Primitivos | ⬜ | Cube, sphere, cylinder, cone, pyramid | Alta |
| **D1.3** Iluminação | ⬜ | Phong lighting, normals, shading | Alta |
| **D1.4** Camera | ⬜ | LookAt, perspective projection | Média |
| **D1.5** Animação | ⬜ | Rotation, scale, position tweening | Média |
| **D1.6** Interação | ⬜ | Rotate scene via touch/gyro | Média |
| **D1.7** Export | ⬜ | Export OBJ, JSON geometria | Baixa |

## Ferramentas Luna

| Tool | Params | Descrição |
|------|--------|-----------|
| `criar_cena_3d` | `json` | Cria cena 3D a partir de JSON |
| `adicionar_objeto` | `cenaId`, `tipo`, `transform`, `material` | Adiciona primitivo |
| `mover_objeto` | `cenaId`, `objetoId`, `transform` | Atualiza transform |
| `rotacionar_objeto` | `cenaId`, `objetoId`, `eixo`, `graus` | Rotaciona |
| `aplicar_material` | `cenaId`, `objetoId`, `material` | Textura/cor |
| `exportar_obj` | `cenaId` | Exporta como OBJ |

## Código

| Onde | O quê |
|------|--------|
| `OrbitLab/.../ui/canvas3d/` | Módulo 3D completo |
| `OrbitLab/.../ui/canvas3d/Scene3D.kt` | Scene graph |
| `OrbitLab/.../ui/canvas3d/Geometry.kt` | Vertices, faces, primitivos |
| `OrbitLab/.../ui/canvas3d/Transform.kt` | Matrix transforms |
| `OrbitLab/.../ui/canvas3d/Lighting.kt` | Phong shading |
| `OrbitLab/.../ui/canvas3d/Camera.kt` | LookAt, projection |
| `OrbitLab/.../ui/canvas3d/Renderer3D.kt` | Render pipeline |
| `OrbitLab/.../ui/canvas3d/Scene3DScreen.kt` | Tela de cena 3D |
| `core/.../ferramentas/canvas3dFerramentas.ts` | Tools Luna 3D |
