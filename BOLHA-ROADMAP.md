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
| **B5** | Fluxo rápido | Quick reply (texto curto + mic) sem sheet 90%; atalhos claros | B3 |
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

**Meta:** a bolha se comporta como chat-head de produto, não protótipo.

### Escopo

| Item | Detalhe |
|------|---------|
| Snap animado | Spring até a borda (não teleporte) |
| Persistência | Salvar X (lado) + Y em `PrefsRepository`; restaurar no `onCreate` do serviço |
| Dismiss por arraste | Zona inferior “Guardar” / X; soltar ali desliga (prefs off) |
| Retirar long-press destrutivo | Ou rebaixar a “atalho avançado” com confirmação — preferência: só zona de dismiss |
| Haptics | Leve no snap, no abrir, no dismiss |
| Limites de tela | Não invadir notch / gesture bar; clamp de Y |

### Fora

- Edge peek (B3), morph redondo perfeito (B2)

### DoD

- [ ] Soltar anima até a borda em ≤ ~300ms percebidos  
- [ ] Matar processo / religar → bolha no mesmo canto/Y  
- [ ] Arrastar pra zona de dismiss desliga sem long-press acidental  
- [ ] Haptic em snap + dismiss (device com vibrador)  
- [ ] Smoke B0 ainda verde  

### Riscos

- `WindowManager` + animação de `params.x/y` no frame — preferir `Animatable`/`ValueAnimator` no serviço ou animar só o Compose e commit na borda no fim  
- Zona de dismiss não pode roubar toques do app de baixo quando invisível  

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

- [ ] Em slow-mo mental: não há “FAB some → tela vazia → sheet sobe”  
- [ ] Fechar termina no ponto onde a bolha está  
- [ ] Voltar do app → FAB entra com fade/scale curto  
- [ ] Sem regressão de composer (Activity continua hospedando pickers)  

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

- [ ] Peek não impede toque no app de baixo na área livre  
- [ ] Resposta em background → badge visível; abrir painel limpa  
- [ ] Stream ativo → sinal no ícone sem tremor de texto  
- [ ] Cota esgotada → sinal distinto + parede no painel  

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

- [ ] Turno com reasoning + imagem gerada renderiza no painel como no app (smoke)  
- [ ] Retry recupera turno órfão sem duplicar user msg  
- [ ] Composer e cota intactos  

### Nota

Reusar composables do `ui/chat/`; evitar fork de bolha. Se algo exigir Activity extra (viewer), bridge mínima ou “abrir no app”.

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

- [ ] Mandar áudio rápido sem abrir sheet 90%  
- [ ] Expandir pro painel preserva texto rascunho se houver  
- [ ] Quick composer também só com app em background  

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

- [ ] Primeira ativação mostra onboarding uma vez  
- [ ] Ajustes reflete estado real (permissão / foreground / rodando)  
- [ ] Notificação FGS com copy útil e não alarmista  
- [ ] Doc curto no `AGENTS.md` ou este arquivo: armadilhas OEM  

---

## Explicitamente fora do roadmap (por enquanto)

- iOS (sem overlay equivalente)  
- Bolha multi-conversa / switcher de threads  
- Responder notificação de outros apps  
- Tradução flutuante / OCR de tela  
- Idle “vivo” com scale da bolha inteira  
- Substituir o chat nativo  

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
| B0 | 🟡 repo | 2026-08-04 | Spec + contratos no repo; baseline `main` (FAB 56, background-only, morph). Smoke aparelho pendente. Canal lab ainda 0.33.2 (publish 0.33.3 cancelado). |
| B1 | ⬜ | | |
| B2 | ⬜ | | |
| B3 | ⬜ | | |
| B4 | ⬜ | | |
| B5 | ⬜ | | |
| B6 | ⬜ | | |

**Legenda:** ⬜ não começou · 🟡 em curso / parcial · ✅ feita e validada no aparelho

---

## Como puxar a próxima fatia

1. Escolher a fase (`B1` recomendada agora).  
2. Pedir: **“vamos especificar a B1”** → gera checklist de arquivos + DoD da sessão.  
3. Implementar → smoke → (se pedir) PR + publish lab com `versionCode` > canal.  
4. Atualizar a tabela de progresso neste arquivo.

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
