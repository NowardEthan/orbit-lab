# B0 — Fundação estável (spec)

> Fase do [`BOLHA-ROADMAP.md`](../BOLHA-ROADMAP.md) · **2026-08-04**
> Não entrega feature nova. Congela contratos, documenta smoke e deixa o repo pronto pra B1.

---

## Contratos (não negociar sem atualizar o roadmap)

| Contrato | Implementação | Regra |
|----------|---------------|--------|
| FAB só no background | `BolhaLunaService` + `ProcessLifecycleOwner` | Com qualquer Activity do OrbitLab em `STARTED`, FAB `GONE` |
| Painel = Activity | `BolhaPainelActivity` (translucent, `singleInstance`, taskAffinity própria) | Composer / pickers / mic **não** no overlay |
| Composer canônico | `ChatInputArea` dentro de `BolhaLunaPainel` | Sem fork de composer na bolha |
| Mesma conversa | `ChatRepository.conversaPrincipal()` | O que manda no painel aparece no chat do app |
| Morph a partir do FAB | extras `fab_cx` / `fab_cy` no Intent | Abrir/fechar com `transformOrigin` no centro do FAB |
| Preferência | `PrefsRepository.bolhaAtiva` + overlay | Desligar = `stopService` + prefs false |
| Canal lab | `updates-lab.json` | Publish só sob pedido; `versionCode` só sobe |

### Arquivos canônicos

```
app/src/main/java/com/ethan/orbitlab/ui/bolha/
  BolhaLunaService.kt      # FGS + WindowManager (só FAB)
  BolhaOverlay.kt          # UI/gesto do FAB
  BolhaPainelActivity.kt   # Activity do painel
  BolhaLunaPainel.kt       # Sheet + ChatInputArea
  BolhaNav.kt              # Ponte “abrir no app”
  OverlayPermissao.kt      # SYSTEM_ALERT_WINDOW
```

Toggle: `AjustesScreen` → “Bolha da Luna”.

---

## DoD desta fase

| Item | Quem | Status |
|------|------|--------|
| Copy Ajustes: só em segundo plano | repo | ✅ |
| Roadmap + link no `AGENTS.md` | repo | ✅ |
| Esta spec versionada | repo | ✅ |
| Compilação `compileLabDebugKotlin` | CI/local | ✅ (rodar na sessão) |
| Smoke aparelho (checklist abaixo) | Ethan | ⬜ |

**B0 fecha no repo** quando os ✅ acima estão no `main`.  
**B0 fecha de verdade** quando o smoke no aparelho estiver ticado (pode ser na mesma sessão do primeiro publish pós-B1).

---

## Smoke B0 (aparelho de referência)

Marcar ao validar (APK do `main` atual ou lab ≥ código com FAB 56 + background-only + morph):

- [ ] Ligar bolha em Ajustes → concede overlay (e notificação se Android 13+)
- [ ] Com OrbitLab aberto → FAB **invisível**
- [ ] Home / outro app → FAB **aparece**
- [ ] Arrastar e soltar → gruda na borda (snap ainda seco — ok na B0)
- [ ] Abrir painel → morph a partir do FAB
- [ ] Composer: texto + enviar ok
- [ ] Composer: foto **ou** áudio → mensagem na conversa principal do app
- [ ] Fechar painel (X / scrim) → volta ao app de baixo + FAB reaparece
- [ ] Desligar em Ajustes (ou long-press atual) → bolha some e não volta sozinha

### Se falhar

| Sintoma | Onde olhar |
|---------|------------|
| FAB visível com app aberto | `ProcessLifecycleOwner` em `BolhaLunaService` |
| Composer sem + / mic | Painel não está na Activity? |
| Mensagem não aparece no app | `conversaPrincipal()` / `ChatRepository` |
| Fechar não volta pro WhatsApp | `taskAffinity` / `singleInstance` da `BolhaPainelActivity` |
| Overlay negado | `OverlayPermissao` + fluxo `aguardandoOverlay` nos Ajustes |

---

## Fora da B0 (próximas fases)

Snap spring, dismiss por arraste, posição salva → **B1**  
Handoff círculo→sheet → **B2**  
Badge / peek → **B3**  
Paridade reasoning/imagens → **B4**

---

## Saída

Ao mergear esta fase:

1. Atualizar tabela de progresso em `BOLHA-ROADMAP.md` (B0 🟡 ou ✅)
2. Próximo pedido natural: **“vamos especificar a B1”**
