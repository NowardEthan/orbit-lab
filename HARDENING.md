# Endurecimento OrbitLab + luna-core (MVP produção sideload)

Plano para sair de “app que funciona” → “app que se opera com identidade estável,
visibilidade e freio de abuso”. **Não é Play Store** nesta rodada.

Cliente = este repo. API = `core/src/luna-core` (Railway).

Princípio: **cota e billing são verdade no servidor.** Parede no app é UX.

---

## Status

| Fase | Tema | Status |
|------|------|--------|
| **0** | Docs: Lab = produção sideload | **feita** |
| **1** | Signing de release + CI + SHA Firebase | **feita** (0.30.4 / code 91; token releases OK) |
| **2** | Minify / R8 no `labRelease` | pendente — **mina Firestore** (ver abaixo) |
| **3** | Crash reporting (Crashlytics preferido) | pendente |
| **4** | Rate limit no luna-core | pendente (pode paralelizar com 3) |
| **5** | Higiene: OpenRouter no APK + DirectChat | **parcial** — chave já vazia no CI; falta isolar DirectChat |

Ordem: `0 → 1 → 2 → 3`, com `4` em paralelo a partir de `1`/`3`.  
Signing **antes** de R8 (uma variável por release).

---

## Fase 0 — Docs alinhados

**DoD**

- [x] `AGENTS.md` deixa de dizer “não é produção” / “só lab de UI”
- [x] `RELEASING.md` descreve flavor `lab` + `updates-lab.json` + Actions
- [x] Este arquivo (`HARDENING.md`) com DoD das fases
- [x] Mapa do monorepo (`luna-workspace/AGENTS.md`) aponta Lab como cliente sideload atual

**Humano:** nenhum passo de console nesta fase.

---

## Fase 1 — Signing de release

**Problema:** `labRelease` assina com debug keystore.

**Guia humano (passo a passo completo):** [`SIGNING.md`](SIGNING.md)

**DoD**

- [x] `signingConfigs.release` no Gradle (quando keystore/props existem)
- [x] CI decodifica keystore, exige secrets `LAB_*`, verifica que não é debug
- [x] `.gitignore` + `keystore.properties.example` + nota de migração
- [x] Keystore de release + secrets `LAB_*` no GitHub — Ethan
- [x] CI: decode + build + `apksigner verify` (não-debug)
- [x] Publish lab **0.30.4** / code **91** (APK + `updates-lab.json`)
- [x] Renovar `ORBIT_RELEASES_TOKEN` — Actions publish verde (run 30864624854)
- [ ] Confirmar Google login no APK release no celular — Ethan

**Próximo humano:** desinstalar Lab antigo → instalar 0.30.4 → testar Google login.

---

## Fase 2 — Minify / R8 ⚠️ mina Firestore

**Problema:** R8 renomeia campos de `data class`. O Firestore usa reflexão
(`.toObject()` / `.set(dataClass)`) — **não é Gson/Moshi**. Depois do minify,
finanças pode voltar com valores **zerados/nulos sem crash**. Crashlytics (Fase 3)
não avisa. Num app de dinheiro, isso mata confiança.

Hoje há dezenas de usos em `FinancasModels`, `FinancasFatura`, `FirestoreChat`,
`UserProfileRepository`, etc.

### Antes de ligar o R8 (obrigatório)

1. Anotar `@Keep` (`androidx.annotation.Keep`) em **toda** `data class` / modelo
   que passa por `.toObject()` / `.set()` — **finanças em primeiro lugar**.
   Explícito, mora ao lado do código; modelo novo sem `@Keep` fica óbvio no review.
2. Cinto-e-suspensório no `proguard-rules.pro`:

   ```
   -keep class com.ethan.orbitlab.data.** { *; }
   ```

   (ou o glob de nested types que o projeto usar — não confiar só no glob.)
3. Regras Compose / Firebase Auth / OkHttp como de costume.

### DoD

- [ ] `@Keep` nos modelos Firestore (finanças + chat + profile + demais `.toObject`)
- [ ] `proguard-rules.pro` com keep de `data.**` + Compose/Firebase/OkHttp
- [ ] `isMinifyEnabled = true` no `release` (shrink só se estável depois)
- [ ] Smoke no APK **minificado**:
  - login Google
  - chat stream
  - planos / cota
  - **finanças: conferir números reais** (saldo, lançamento, fatura) — não só “abre”
  - auto-update N→N+1
- [ ] Sem regressão silenciosa de dados (valores zerados = falha)

**Humano:** smoke no celular com atenção especial a **valores de finanças**.

**Nota pro agente:** na Fase 2, anotar `@Keep` **antes** de ligar o R8. Esse é o pulo do gato.

---

## Fase 3 — Crash reporting

Preferência: **Firebase Crashlytics** (projeto `luna-8787d`). Alternativa: Sentry.

**DoD**

- [ ] SDK no flavor lab
- [ ] Identidade de crash: uid opaco (sem e-mail / sem texto de chat / sem dado financeiro)
- [ ] Breadcrumb leve: tela, `versionName`/`versionCode`
- [ ] Crash de teste aparece no console
- [ ] 5 linhas de política de PII no `AGENTS.md`

**Humano:** confirmar no console Firebase que o crash de teste chegou.

**Lembrete:** Crashlytics **não** substitui o smoke de finanças da Fase 2 — corrupção
por R8+Firestore não crasha.

---

## Fase 4 — Rate limit (luna-core)

**DoD**

- [ ] Middleware por **uid** (rotas autenticadas caras) + teto frouxo por **IP**
- [ ] `429` com código `rate_limited` (≠ `quota_exceeded`) + `Retry-After` quando fizer sentido
- [ ] Feature flag em `/health` (`rateLimit: true`)
- [ ] Testes do contador / estouro
- [ ] App trata mensagem humana sem confundir com parede de cota (“lua dormiu”)
- [ ] Limites ajustáveis por env

**Humano:** validar em staging/prod com smoke; ajustar números se doer UX.

---

## Fase 5 — Higiene de superfície

**Chave OpenRouter no APK (já resolvido por construção):**  
`BuildConfig.OPENROUTER_API_KEY` vem de `local.properties` / `../core/src/luna-core/.env`.
No CI do `orbit-lab` esse path **não existe** → a chave sai **vazia** no APK oficial.
Não precisa de passo extra pra “não embutir no release”.

**DoD**

- [x] CI/release **não** embute `OPENROUTER_API_KEY` no `BuildConfig` (vazio no Actions)
- [ ] `LunaDirectChat` / caminho OpenRouter no cliente isolados pra **debug local só**
      (prefs já forçam off; remover ou trancar superfície de risco)
- [ ] Docs listam SHA debug **e** release (parcialmente em `SIGNING.md` / `AGENTS.md`)

---

## Critério de plano concluído

1. APK sideload com release key, atualizável em cadeia após a migração única.  
2. R8 ligado **com** `@Keep` nos modelos Firestore + smoke de **números** de finanças OK.  
3. Crash de teste visível no painel.  
4. Core devolve rate limit sob spam; health marca a feature.  
5. DirectChat fora do produto; docs e release batem com a realidade.
