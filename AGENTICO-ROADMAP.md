# Luna agentica por base — Roadmap

> **Norte:** a Luna é, em essência, como um agente (planeja, usa ferramentas, age) —
> **e** continua profunda (rigor, contexto, não resposta rasa).
> Não é um toggle “Ação” que o usuário liga. É a personalidade do produto.
>
> **Criado:** 2026-08-04 · Depois do ship da bolha (B*) · Spec por fase: “vamos especificar a A__”
> Relacionado: modos sem seletor em [`BOLHA-ROADMAP.md`](BOLHA-ROADMAP.md) § M1 (absorvido aqui).

---

## O que queremos sentir

| | |
|---|---|
| **Como o Cursor / tu** | Vê o pedido, decide se precisa de tool, age, explica o que fez |
| **Profunda** | Não é chatbot raso: quando o assunto pede, aprofunda sem o usuário ter que ligar “Técnico” |
| **Não invasiva** | “Oi” / small talk → resposta leve, **sem** pedágio de 4 rodadas de tools |
| **Uma verdade** | Chat, bolha e finanças obedecem a mesma política no **luna-core** |

**Frase-guia:** agentica por *capacidade e postura*; conversacional por *economia* quando o turno é simples.

---

## Estado de partida (hoje)

| Camada | Hoje |
|--------|------|
| Lab UI | ~~Seletor Conversa / Técnico / Ação~~ → **removido (A1)** |
| Lab → core | `modoAgentico` só módulo Finanças; `modoTecnico` false; soft router no resto |
| Core gate | `deveUsarModoAgentico()`: **force** se flag **ou** soft router (regex/anexo/finanças/web/doc/imagem) |
| Path agentico | `responderComoLunaAgentico` → `executorAgentico` (multi-rodada, SSE `acao`, sem stream de texto fino) |
| Path conversa | stream clássico (`responderComoLunaStream`) |
| Soft router | Já é o default do core quando a flag **não** vem — o toggle “Ação” só **força** sempre |

Arquivos-chave:

- Lab: `PrefsRepository`, `LunaApiChat`, `LunaModeSheet`, `ChatInputArea`
- Core: `pipeline/detectoresIntencao.ts`, `executarPipelineCompleto.ts`, `responderComoLunaAgentico.ts`, `agente/executorAgentico.ts`, `ferramentas/registroFerramentasChat.ts`

---

## O que *não* fazer

1. **`forcarAgentico` em todo turno** — transforma “bom dia” em loop caro; quebra a sensação premium.  
2. **Deixar o seletor e só mudar o default pra Ação** — continua mentalidade de “modo”, não de essência.  
3. **Agentico raso** — tools sem profundidade de raciocínio / sem qualidade de resposta.  
4. **Dois produtos** — bolha leve vs chat agentico com regras diferentes.

---

## Arquitetura alvo

```
mensagem do usuário
    → classificador de turno (core): leve | profunda | ação
    → leve:     stream conversacional (voz Luna, sem tools)
    → profunda: stream/agent leve com raciocínio alto, tools só se precisar
    → ação:     loop agentico (tools + timeline), resposta final profunda
```

- **Profunda** não exige seletor: o classificador sobe reasoning / detalhe quando o pedido pede análise, código, pedagogia, etc.  
- **Ação** = tools disponíveis + executor quando há trabalho (doc, finanças, web, imagem, multi-passo).  
- Cliente **não** escolhe modo; só mostra o que aconteceu (timeline / reasoning).

---

## Mapa das fases

| Fase | Nome | Valida |
|------|------|--------|
| **A0** | Contrato de essência | Doc + política “soft agentico + profunda”; sem force global |
| **A1** | Sem seletor no Lab | Some Conversa/Técnico/Ação; prefs ignoradas; bolha = chat |
| **A2** | Soft router afiado | Detectors melhores; menos FP/FN; métricas |
| **A3** | Profundidade por turno | “Técnico” vira postura automática, não flag sticky |
| **A4** | Agentico premium | Planejamento seletivo, latência, streaming parcial se der |
| **A5** | Paridade produto | Lab + bolha (+ mobile se ainda existir caminho) na mesma política |

**Ordem:** `A0 → A1` rápido (produto); `A2 → A3` core; `A4` polish; `A5` fecha.

```
A0 ──► A1 (Lab UI)
 │
 └──► A2 ──► A3 ──► A4
              └──► A5
```

---

## A0 — Contrato de essência

**Meta:** todo mundo alinhado no que “agentica por base” significa.

### Escopo

- Este roadmap + menção no `AGENTS.md`  
- Decisão escrita: **base = soft router + profundidade por turno**, nunca force-all  
- Lista de exemplos canônicos (DoD de produto):

| Pedido | Esperado |
|--------|----------|
| “Oi” / “obrigado” | Conversa leve, 0 tools |
| “Explica como funciona X em detalhe” | Profunda, tools só se precisar |
| “Registra 50 no almoço” / “cria um doc” | Agentico + tool |
| “Pesquisa Y e resume” | Agentico + web (se ligado) |
| Ambíguo (“me ajuda com isso”) | Pode perguntar / plano curto, sem gastar 4 tools à toa |

### DoD

- [x] Contrato acima aceito (Ethan engatou o sprint agentico, 2026-08-04)  
- [x] Link no `AGENTS.md`  

---

## A1 — Sem seletor (Lab + bolha)

**Meta:** a UI para de mentir que existem “modos”.

### Escopo

- Remover seletor do `ChatInputArea` / `LunaModeSheet` (ou deixar morto)  
- Parar de enviar `modoAgentico` / `modoTecnico` sticky (ou sempre `false` / omitir)  
- Core continua no soft router (já default)  
- Finanças pode continuar forçando agentico **no módulo**, não via toggle global  

### DoD

- [x] Composer sem Conversa/Técnico/Ação  
- [x] Body: `modoAgentico` só se módulo Finanças; `modoTecnico` sempre false; prefs sticky apagadas  
- [ ] Pedidos de ação ainda disparam tools (smoke aparelho)  
- [ ] “Oi” continua leve (smoke aparelho)  
- [x] Bolha usa o mesmo `ChatInputArea` (sem seletor)  

---

## A2 — Soft router afiado

**Meta:** a essência agentica **funciona** sem o usuário pedir modo.

### Escopo (core)

- Revisar `detectoresIntencao` / `deveUsarModoAgentico` — FP (R$, “pesquisa” genérico) e FN (pedido claro de ação que cai em conversa)  
- Telemetria opaca: `agentico_sim|nao`, motivo do gate, latência (sem texto de chat)  
- Ajustar tetos de rodadas pra turnos leves que escaparem pro agentico  

### DoD

- [x] Detectores afiados (FP R$ metafórico / «minha pesquisa»; lançamento rápido ok)  
- [x] `decidirGateAgentico` + `avaliarGateAgentico` com motivo opaco  
- [x] Log `LUNA_AGENTIC_GATE_LOG=1` → `agentico_gate usar=… motivo=…`  
- [x] Harness `tests/detectoresIntencaoAgentico.test.ts` (tabela A0 parcial)  
- [ ] Smoke latência “oi” no aparelho / bench  

---

## A3 — Profundidade por turno

**Meta:** profunda como default *quando o assunto pede* — sem chip Técnico.

### Escopo

- Substituir `modoTecnico` sticky por sinal de profundidade no classificador (ou heurística + prompt)  
- Reasoning alto / resposta estruturada quando o pedido for analítico  
- Small talk continua curto e caloroso (voz Luna)  

### DoD

- [x] `mensagemPedeProfundidade` no core + injeta diretriz + `reasoningEffort=high`  
- [x] Diretriz sem “carregou no botão” (pedido do turno)  
- [ ] Smoke: “Explica a fundo…” → densa; “Tudo bem?” → curto  

---

## A4 — Agentico premium (polish)

**Meta:** quando age, parece agente de verdade — confiável e rápido o bastante.

### Escopo

- Planejamento só quando multi-passo (não em tool única óbvia)  
- **Task list exposta** (abaixo) — a coleira do core vira UI, não só meta-tool  
- Melhor narrar → agir → fechar (timeline clara)  
- **Narração progressiva** (abaixo A4.2) — pontes «Vou ler…» + SSE `content` mid-loop  
- Pesquisa profunda / vision: opt-in ou gatilho claro (custo)  
- Guardrail: max tools / max rodadas por tipo de turno  

### A4.2 — Narração estilo Cursor (pontes entre ações)

**Diagnóstico:** o path agentico descartava `content` quando havia `tool_calls`
(provider + executor tratavam texto como fim do loop). A UI só mostrava timeline
recolhida («N passos») — sensação de “ela fez uma ação” sem prosa.

**O que mudou (ship)**

| Camada | Mudança |
|--------|---------|
| Provider | `content` + `tool_calls` na mesma resposta |
| Executor | ponte → `onNarracaoRodada` → continua o loop; texto final junta pontes |
| Pipeline / SSE | `onNarracao` → `onStreamContentDelta` (`content` mid-loop) |
| Prompt | `DIRETRIZ_NARRACAO_AGENTICA` + anti-meta com exceção de ponte de 1 frase |
| Lab | timeline **aberta** enquanto RUNNING; labels live com `…`; toolMeta blocos |

**Sensação alvo**

```
Vou ler o artefato pra me informar.
  ⊟ Lendo o documento…          ← timeline aberta
Entendi. Agora ajusto só esse trecho.
  ⊟ Ajustando o documento…
Pronto — troquei a frase X.
```

**DoD A4.2**

- [x] Provider devolve content+tools  
- [x] Executor narra e continua; fallback humano (sem «Executei N ações…»)  
- [x] Diretriz de pontes + afrouxar anti-meta só pra ação  
- [x] Lab: timeline ao vivo aberta + toolMeta `inserir_blocos` / `ler_bloco` / …  
- [ ] Smoke aparelho: «lê o artefato e corrige Y» → ponte → lendo → ponte → editando  

### A4.1 — Task list / plano à vista (sensação Cursor)

O core já tem a coleira (`planejar` → `concluir_passo` → `adicionar_passo`) e emite SSE
`tipo: "plano"` com snapshot `{ texto, feito }[]`. A descrição da tool promete
“lista à vista dele”.

**Diagnóstico real (Lab):** o evento `tipo: "plano"` era **ignorado** em `aplicarAcao`.
Na prática o usuário **não via nem** o rótulo “Traçou o plano” de forma útil —
a coleira existia no servidor e a UI do Lab ficava muda. Não era “só esconder
checklist atrás da timeline”; era **zero affordance de plano**.

| Camada | Papel |
|--------|--------|
| Core | Continua dono do estado do turno; emite snapshot a cada mudança |
| Lab live | Card/checklist **sempre aberto** enquanto houver plano (não recolhido em “N passos”) |
| Lab histórico | Persistir `plano` na mensagem Luna e reidratar no fio |
| Timeline de tools | Esconder `planejar` / `concluir_passo` / `adicionar_passo` — a checklist *é* esses passos |

**Sensação alvo**

```
Plano · 1/3
☐ Ler o documento atual          ← corrente (primeiro ☐), leve destaque
☑ Reescrever em prosa            ← feito, riscado/suave
☐ Conferir o resultado
```

- Sem card pesado: mesma linguagem quieta do reasoning / tool timeline (fio, tipografia mid).  
- Atualiza ao vivo a cada evento `plano` (marca ☑ sem “piscar” a lista inteira).  
- Quando o turno fecha: checklist completa fica no histórico acima da resposta.  
- Pedido de 1 tool óbvia: **sem** plano (já é regra do prompt) — UI não inventa lista fantasma.

**DoD A4.1**

- [x] `aplicarAcao` trata `tipo == "plano"` e alimenta `LunaActionRun.plano`  
- [x] Checklist visível no stream e no histórico (`LunaPlanChecklist` + persistência `plano`)  
- [x] Meta-tools de plano fora da timeline genérica  
- [ ] Pedido multi-passo (ex. ler + reescrever doc): lista aparece e vai riscando (smoke no aparelho)  

### DoD (A4 geral)

- [ ] Fluxo doc ou finanças: timeline legível + resultado certo  
- [ ] Task list A4.1 verde  
- [x] Narração A4.2 no core + Lab (smoke aparelho pendente)  
- [ ] Menos sensação de “travou pensando” em ação simples  
- [ ] Nenhum loop absurdo em small talk (regressão)  

---

## A5 — Paridade produto

**Meta:** uma Luna só.

### Escopo

- Mesma política Lab chat ↔ bolha ↔ (finanças: agentico de módulo ok)  
- Documentar flags env (`LUNA_AGENTIC_*`) e defaults de produção  
- Se `orbit-mobile` ainda falar com o core: alinhar body (sem depender de toggle)  

### DoD

- [ ] Smoke: mesmo pedido no chat e na bolha → mesmo tipo de path  
- [ ] AGENTS / RELEASING mencionam “sem seletor de modo”  

---

## Relação com M1 (modos adaptativos)

A seção M1 em `BOLHA-ROADMAP.md` era o pedido “tirar o seletor”.  
**Este doc é o norte maior:** não só tirar UI — fazer a Luna **ser** agentica e profunda.

| M1 (antigo) | Aqui |
|-------------|------|
| Tirar seletor | **A1** |
| Router automático | **A2** + **A3** |
| Guardrails | **A2** / **A4** |

Ao especificar, preferir **A0/A1…** deste arquivo.

---

## Registro de progresso

| Fase | Status | Data | Notas |
|------|--------|------|-------|
| A0 | ✅ | 2026-08-04 | Contrato aceito; Ethan engatou sprint |
| A1 | 🟡 | 2026-08-04 | Seletor fora; body sem force sticky; smoke aparelho pendente |
| A2 | 🟡 | 2026-08-04 | Detectores + harness verdes; smoke latência pendente |
| A3 | 🟡 | 2026-08-04 | Profundidade por turno no pipeline; smoke pendente |
| A4 | 🟡 | 2026-08-04 | A4.1 checklist no Lab (código); smoke pendente |
| A5 | ⬜ | | |

---

## Como puxar

**Sprint atual (agentico):** `A0 ✅ → A1 🟡 → A4.1 smoke → A2/A3`.

1. Smoke A1 no aparelho (“oi” leve + ação com tools).  
2. Smoke A4.1 (lista de passos à vista em pedido multi-passo).  
3. Core **A2/A3** com harness de casos.  
4. Resto do **A4** (latência / stream parcial) quando a sensação de agente ainda travar.  
5. Bolha B* publish em paralelo / quando Ethan pedir (não bloqueia A1).
