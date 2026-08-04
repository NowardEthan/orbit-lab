# B1 — Gesto premium (spec)

> Fase do [`BOLHA-ROADMAP.md`](../BOLHA-ROADMAP.md) · **2026-08-04**
> Snap spring, dismiss por arraste, posição salva, haptics. Sem edge peek (B3) nem handoff visual (B2).

---

## Escopo desta sessão

| Item | Como |
|------|------|
| Snap animado | `ValueAnimator` em `params.x` até a borda (~220–280ms) |
| Persistência | `PrefsRepository`: lado (esq/dir) + Y |
| Dismiss | No arraste, overlay vira tela cheia; zona inferior “Guardar”; soltar ali desliga |
| Sem long-press destrutivo | Removido — só a zona de dismiss |
| Haptics | Snap / entrar na zona / dismiss / abrir painel — respeita `vibracao` |
| Clamp Y | Margem top (~status) e bottom (~gesture bar) |

## Arquivos

- `data/PrefsRepository.kt` — keys posição
- `ui/bolha/BolhaLunaService.kt` — modos WRAP/MATCH, snap, persistir
- `ui/bolha/BolhaOverlay.kt` — gesto + zona dismiss
- `ui/ajustes/AjustesScreen.kt` — copy do gesto
- Roadmap progresso

## DoD

- [x] Soltar anima até a borda (não teleporta)
- [x] Religar serviço → mesmo lado + Y
- [x] Arrastar pra zona inferior desliga (prefs off)
- [x] Long-press **não** desliga mais
- [x] Haptic no snap e no dismiss (se vibração ligada)
- [x] `compileLabDebugKotlin` ok
- [ ] Smoke no aparelho — Ethan

## Fora

Edge peek, morph círculo→sheet, badge, quick reply.
