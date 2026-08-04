# Bolha da Luna — Roadmap

> Chat-head flutuante do OrbitLab: atalho pra conversar com a Luna **sobre outros apps**,
> sem competir com o chat nativo quando o app está aberto.
>
> **Criado:** 2026-08-04 · Canal: `lab` (`updates-lab.json`) · Package `com.ethan.orbitlab`
> Spec executável de cada fase é escrita **quando a fase começa** (pedir: “vamos especificar a B__”).
> Este doc é o mapa, não o passo a passo de implementação.

---

## Norte do produto

| | |
|---|---|
| **Job da bolha** | Estar à mão quando o OrbitLab está em segundo plano |
| **Job do painel** | Mesma conversa principal, composer completo, sensação de “nasceu do FAB” |
| **Job do app** | Workspace completo — a bolha **some** com o app em foreground |
| **Não é** | Segundo app de chat; menu radial; assistente invasivo com idle piscando |

**Frase-guia:** se a bolha atrapalhar mais do que ajudar no WhatsApp/Uber, a fase falhou — mesmo com features a mais.

---

## Estado de partida (baseline — 2026-08-04)

Código no `main` (ainda **não** publicado como 0.33.3; canal estável em **0.33.2 / 101**).

| Peça | Estado |
|------|--------|
| FAB overlay (`BolhaLunaService` + `BolhaOverlay`) | 56dp, arrastar, snap seco na borda, long-press desliga |
| Só no background | `ProcessLifecycleOwner` — some com Activity do OrbitLab aberta |
| Painel | `BolhaPainelActivity` translúcida + `ChatInputArea` (anexos/mic/câmera/modo) |
| Morph FAB→modal | scale + `transformOrigin` a partir do centro do FAB |
| Conversa | `ChatRepository.conversaPrincipal()` — mesma do app |
| Prefs | `bolhaAtiva` + permissão overlay; posição **não** persiste |
| Notificação FGS | canal mínimo, texto genérico |
| Paridade chat | composer ok; bolhas ainda “light” (reasoning / imagem gerada / retry incompletos) |

Arquivos-chave: `ui/bolha/*`, toggle em `AjustesScreen`, manifesto `SYSTEM_ALERT_WINDOW` + FGS `specialUse`.

---

## Princípios de execução

1. **Gesto antes de feature** — snap, dismiss e posição vêm antes de badge/quick-reply.
2. **Motion Orbit** — curto, intencional (`OrbitMotion`); sem idle agressivo no FAB inteiro (`AGENTS.md`).
3. **Activity só onde Android exige** — painel/composer na Activity; FAB no overlay.
4. **Foreground = bolha off** — não negociar; o chat nativo manda.
5. **Uma fase por fatia shipável** — cada Bx fecha com DoD testável no aparelho + (opcional) publish lab.
6. **Cota/erro honestos** — parede e falha de rede não podem parecer “bolha morta”.
7. **OEM é realidade** — Xiaomi/Samsung matam FGS; Ajustes precisa ensinar, não só culpar o usuário.

---

## Mapa das fases

| Fase | Nome | O quê valida | Depende |
|------|------|--------------|---------|
| **B0** | Fundação estável | Baseline usável, docs, sem regressão do composer | — |
| **B1** | Gesto premium | Snap spring, dismiss por arraste, posição salva, haptics | B0 |
| **B2** | Continuidade visual | Handoff FAB↔modal redondo→sheet; enter ao voltar pro background | B1 |
| **B3** | Bolha com sinal | Edge peek, badge/unread, “pensando…”, pulso de erro/cota | B1 |
| **B4** | Paridade do painel | Reasoning, imagens geradas, retry, referência — perto do chat | B0 |
| **B5** | Fluxo rápido | ~~Quick reply~~ **retirado** (incomodava) — toque abre o painel | B3 |
| **B6** | Operação & confiança | Onboarding, copy FGS, guia OEM/bateria, telemetria mínima sem PII | B1 |

**Ordem recomendada:** `B0 → B1 → B2` em série; `B3` e `B4` em paralelo depois de B1; `B5` depois de B3; `B6` pode começar cedo (copy/onboarding) e fechar depois de B1.

```
B0 ──► B1 ──► B2
         │
         ├──► B3 ──► B5
         ├──► B4
         └──► B6 (parcial desde B1)
```

---

## B0 — Fundação estável

**Spec:** [`bolha/B0-fundacao.md`](bolha/B0-fundacao.md)

**Meta:** o que já existe no `main` fica explícito, testável e pronto pra empilhar polish — sem inventar feature nova.

### Escopo

- Congelar contratos: FAB só background; painel = Activity; composer = `ChatInputArea`
- Smoke checklist no aparelho (spec B0)
- Roadmap + menção em `AGENTS.md` / Ajustes alinhados ao comportamento real
- Publish lab **só quando** Ethan pedir (canal não sobe sozinho)

### Fora

- Snap spring, badge, quick reply, edge peek

### DoD

- [x] Copy de Ajustes descreve “só em segundo plano”
- [x] Este roadmap + spec B0 versionados no repo
- [x] Contratos congelados na spec (FAB / Activity / `ChatInputArea` / conversa principal)
- [x] `compileLabDebugKotlin` ok (sem mudança de comportamento nesta fase)
- [ ] Checklist smoke no aparelho de referência — **Ethan** (lista na spec B0)

### Smoke B0 (aparelho)

Ver checklist ticável em [`bolha/B0-fundacao.md`](bolha/B0-fundacao.md).

---

## B1 — Gesto premium

**Spec:** [`bolha/B1-gesto-premium.md`](bolha/B1-gesto-premium.md)

**Meta:** a bolha se comporta como chat-head de produto, não protótipo.

### Escopo

| Item | Detalhe |
|------|---------|
| Snap animado | `ValueAnimator` ~240ms até a borda |
| Persistência | `PrefsRepository` lado + Y |
| Dismiss por arraste | Overlay tela cheia no drag + zona “Guardar” |
| Sem long-press destrutivo | Removido |
| Haptics | Snap / zona / dismiss / toque — respeita vibração |
| Limites de tela | Clamp Y (status + gesture) |

### Fora

- Edge peek (B3), morph redondo perfeito (B2)

### DoD

- [x] Soltar anima até a borda (~240ms)  
- [x] Religar → mesmo lado + Y (prefs)  
- [x] Arrastar pra zona de dismiss desliga  
- [x] Long-press não desliga mais  
- [x] Haptic em snap + dismiss (se vibração ligada)  
- [ ] Smoke B0+B1 no aparelho — **Ethan**  

### Riscos

- Durante o arraste a tela cheia captura toques (ok); idle continua WRAP + `NOT_TOUCH_MODAL`  
- Coordenadas cutout: validar em 2 densidades no smoke  

---

## B2 — Continuidade visual (handoff)

**Meta:** abrir/fechar o painel parece **um** objeto (a bolha), não dois sistemas.

### Escopo

| Item | Detalhe |
|------|---------|
| Handoff de abertura | FAB some no mesmo frame em que o morph começa; opcional “ghost” circular no pivot |
| Corner morph | Interpolar aparência círculo → `RoundedCornerShape` do sheet (clip/`Path` ou crossfade de máscara) |
| Handoff de fechamento | Recolhe ao centro do FAB **salvo**; FAB reaparece no fim do progresso (não antes) |
| Enter no background | Ao `ON_STOP` do app, FAB faz enter curto (`OrbitMotion`), não só `VISIBLE` |
| Transições Activity | Manter `overridePendingTransition(0,0)`; zero flash do sistema |

### Fora

- Shared Element real entre janelas diferentes (overlay ≠ Activity) — aproximar, não perseguir API impossível  

### DoD

- [x] FAB some no instante do `expandirPainel` (antes do startActivity)  
- [x] Cantos interpolam no morph; enter no background via `enterNonce`  
- [ ] Smoke visual aparelho: sem “tela vazia”; fechar no ponto do FAB  
- [x] Sem regressão de composer (Activity continua hospedando pickers)  

### Riscos

- Coordenadas overlay vs `positionInWindow` em OEMs com cutout — validar em 2 densidades  
- Teclado aberto no fechar: recolher com IME pode distorcer o morph — fechar IME antes do collapse  

---

## B3 — Bolha com sinal

**Meta:** a bolha comunica estado sem abrir o painel.

### Escopo

| Item | Detalhe |
|------|---------|
| Edge peek | Após N segundos idle na borda, traduz ~40–50% pra fora; toque/arraste restaura |
| Badge / unread | Ponto sutil quando chega resposta da Luna com painel fechado (e app em background) |
| Estado “pensando” | Anel ou glow **no ícone** (não scale do card inteiro) enquanto stream ativo na conversa principal |
| Erro / cota | Pulso/vermelho contido ou badge distinto; abrir painel mostra parede já existente |
| Limpar badge | Ao abrir painel ou ao abrir o chat no app |

### Fora

- Notificação push aparte da FGS (pode ser B6)  
- Preview da mensagem na bolha (privacidade)  

### DoD

- [x] Peek (~45%, delay idle) — smoke touch-through no aparelho  
- [x] Badge em background; limpa no painel / quick / chat nativo  
- [x] Stream → anel no ícone (não scale do card)  
- [x] Cota → ponto vermelho + parede no painel  
- [ ] Smoke aparelho B3 completo  

### Privacidade

- Badge **não** mostra trecho da mensagem no overlay  
- Crashlytics / analytics: só eventos opacos (`bolha_badge_show`), nunca texto  

---

## B4 — Paridade do painel

**Meta:** o painel não parece “chat light” depois do composer completo.

### Escopo

| Item | Detalhe |
|------|---------|
| Reasoning | `LunaReasoning` quando a msg tiver reasoning |
| Imagens geradas | `LunaImagemGeradaLista` / cards do chat |
| Action timeline | Já parcial — alinhar ao chat (aberto/fechado) |
| Retry | Mesmo padrão Finanças/Chat (órfã / erro) |
| Referência de mídia | Long-press / referenciar se o chat tiver e couber no overlay |
| Markdown / erros | Paridade visual de bolhas usuário/Luna |

### Fora

- Toda a shell do chat (lista de conversas, busca, media viewer fullscreen complexo) — “Abrir no app” cobre  

### DoD

- [x] Reasoning + imagem + timeline no painel (código)  
- [x] Retry órfã/erro sem duplicar user (`ChatTurno` + mesmo `ChatTimeline`/`MessageBubble`)  
- [x] Composer e cota intactos  
- [x] Pergunta, referência (swipe/long-press), regerar aspecto, auto-retry, action sheet  
- [ ] Smoke reasoning/imagem/retry no aparelho  

### Nota

Reusar composables do `ui/chat/` (`ChatTimeline`, `MessageBubble`, `ChatTurno`); evitar fork de bolha. Shell (busca/export/estante) fica no app — “Abrir no app”.

---

## B5 — Fluxo rápido

**Meta:** interações de 2 segundos sem sheet de 90%.

### Escopo

| Item | Detalhe |
|------|---------|
| Quick composer | Toque curto no FAB → pill/composer colado à bolha (1 linha + enviar + mic) |
| Toque longo / segundo gesto | Abre painel completo (ou botão “expandir”) |
| Envio rápido | Texto/áudio entram na conversa principal; resposta pode só acender badge (B3) |
| Escape | Toque fora / back fecha o quick composer sem matar a bolha |

### Fora

- Anexos/câmera no quick (mandam pro painel completo)  
- Multi-conversa na bolha  

### DoD

- [x] Mic hold no quick (se já tem permissão; senão abre painel)  
- [x] Expandir leva `textoInicial` pro `ChatInputArea`  
- [x] Quick só com app em background (mesmo gate do FAB)  
- [ ] Smoke texto + áudio + rascunho no aparelho  

### Riscos

- Mic no Service/FGS: validar tipo FGS / permissão em Android 14+  
- Quick UI no `WindowManager` (sem Activity) — texto ok; pickers não  

---

## B6 — Operação & confiança

**Meta:** ligar a bolha não gera suporte; OEM não parece “bug da Luna”.

### Escopo

| Item | Detalhe |
|------|---------|
| Onboarding 1 tela | Ao ligar pela 1ª vez: overlay, “some no app”, dismiss, privacidade do badge |
| FGS | Título/corpo/ação da notificação: “Bolha ativa · toque pra abrir o Orbit” (ou abrir painel) |
| Guia OEM | Link/texto em Ajustes: bateria sem restrição / autostart (genérico pt-BR) |
| Estado em Ajustes | “Ativa · oculta (app aberto)” / “Ativa · flutuando” / “Sem permissão” |
| Telemetria mínima | Contadores: open_panel, dismiss, snap, permission_denied — sem PII, alinhado Crashlytics rules |
| Recuperação | `tentarReligarSePreferida` já existe — cobrir boot? (opcional `BOOT_COMPLETED` só se prefs + overlay) |

### Fora

- Play Store listing / AccessibilityService hacks  

### DoD

- [x] Onboarding 1× ao ligar  
- [x] Ajustes: subtítulo por permissão / painel / foreground  
- [x] FGS copy útil (“Some com o app aberto…”)  
- [x] Tip OEM + armadilha em `AGENTS.md`  
- [x] Breadcrumbs: open_panel, dismiss, snap, quick_*, permission_denied, badge_show  
- [ ] Smoke primeira ativação + OEM no aparelho  

---

## Explicitamente fora do roadmap (por enquanto)

- iOS (sem overlay equivalente)  
- Bolha multi-conversa / switcher de threads  
- Responder notificação de outros apps  
- Tradução flutuante / OCR de tela  
- Idle “vivo” com scale da bolha inteira  
- Substituir o chat nativo  

---

## Sprint seguinte — depois do ship B*

Ordem combinada (2026-08-04):

1. **Fechar B2–B6** (este sprint FAB) + publish lab.  
2. **A4.1** — task list / plano à vista ([`AGENTICO-ROADMAP.md`](AGENTICO-ROADMAP.md) § A4.1).  
   O Lab **não mostrava nem** “Traçou o plano” (evento `tipo: "plano"` engolido).  
3. **A0 → A1** — Luna agentica por base (tirar seletor).  

> Luna **em essência** agentica **e** profunda — sem seletor Conversa/Técnico/Ação.
> Roadmap completo: [`AGENTICO-ROADMAP.md`](AGENTICO-ROADMAP.md) (A0–A5).
> **Nunca** `forcarAgentico` em todo “oi”.

---

## Critérios de qualidade transversais

Em **toda** fase que mexe em UI/motion:

- [ ] pt-BR (Brasil) em copy — nunca pt-PT  
- [ ] `OrbitMotion` / sem ripple Material no FAB  
- [ ] FAB invisível com app em foreground  
- [ ] Composer completo no painel não quebra  
- [ ] versionCode do lab só sobe no publish  
- [ ] Smoke B0 (ou subset) no aparelho antes de publish  

---

## Registro de progresso

| Fase | Status | Data | Notas |
|------|--------|------|-------|
| B0 | 🟡 repo | 2026-08-04 | Spec + contratos no repo; baseline `main`. Smoke aparelho pendente. Canal lab 0.33.2. |
| B1 | 🟡 repo | 2026-08-04 | Snap animado, posição salva, dismiss por arraste, haptics. Smoke aparelho pendente. |
| B2 | 🟡 | 2026-08-04 | WIP: morph/cantos/enter; falta ghost + hide no mesmo frame |
| B3 | 🟡 | 2026-08-04 | WIP: peek/badge/pensando/cota; limpar badge no chat nativo |
| B4 | 🟡 | 2026-08-04 | WIP: reasoning/imagem/retry no painel; ref. long-press opcional |
| B5 | ⛔ retirado | 2026-08-04 | Quick reply removido por UX; toque no FAB → painel |
| B6 | 🟡 | 2026-08-04 | WIP: onboarding/FGS/OEM/estado; fechar telemetria + doc |

**Legenda:** ⬜ não começou · 🟡 em curso / parcial · ✅ feita e validada no aparelho

---

## Como puxar a próxima fatia

1. **Agora:** fechar sprint **B2–B6** nesta branch (`cursor/bolha-b2-b6-completo-g9h0`).  
2. Smoke aparelho → PR → publish lab (se Ethan pedir).  
3. Em seguida: **A4.1** (task list) no [`AGENTICO-ROADMAP.md`](AGENTICO-ROADMAP.md).

---

## Referências

| Doc / código | Papel |
|--------------|--------|
| [`AGENTS.md`](AGENTS.md) | Visual, motion, armadilhas OrbitLab |
| [`RELEASING.md`](RELEASING.md) / [`TESTE-UPDATE.md`](TESTE-UPDATE.md) | Publish canal lab |
| [`HARDENING.md`](HARDENING.md) | Produção sideload (contexto) |
| `ui/bolha/` | Implementação |
| `ui/chat/ChatInputArea` | Composer canônico |
| `ui/financas/FinancasLunaChatSheet` | Referência de sheet 88% + composer |
