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
