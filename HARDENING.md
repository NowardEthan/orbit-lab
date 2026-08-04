

# Endurecimento OrbitLab + luna-core (MVP produção sideload)

Plano para sair de “app que funciona” → “app que se opera com identidade estável,
visibilidade e freio de abuso”. **Não é Play Store** nesta rodada.

Cliente = este repo. API = `core/src/luna-core` (Railway).

Princípio: **cota e billing são verdade no servidor.** Parede no app é UX.

---

## Status


| Fase  | Tema                                    | Status                                                      |
| ----- | --------------------------------------- | ----------------------------------------------------------- |
| **0** | Docs: Lab = produção sideload           | **feita**                                                   |
| **1** | Signing de release + CI + SHA Firebase  | **feita** (0.30.4; login OK no aparelho)                    |
| **2** | Minify / R8 no `labRelease`             | **feita** (0.30.5)                                          |
| **3** | Crash reporting (Crashlytics preferido) | **em curso** → 0.30.6                                       |
| **4** | Rate limit no luna-core                 | pendente (pode paralelizar com 3)                           |
| **5** | Higiene: OpenRouter no APK + DirectChat | **parcial** — chave já vazia no CI; falta isolar DirectChat |


Ordem: `0 → 1 → 2 → 3`, com `4` em paralelo a partir de `1`/`3`.

---

## Fase 0 — Docs alinhados

**DoD**

- [x] `AGENTS.md` / `RELEASING.md` / este arquivo / mapa do monorepo

---

## Fase 1 — Signing de release

**Guia:** `[SIGNING.md](SIGNING.md)`

**DoD**

- [x] Keystore + secrets `LAB_*` + CI assina + publish
- [x] `ORBIT_RELEASES_TOKEN` renovado (publish Actions verde)
- [x] Google login no APK release no celular — Ethan

---

## Fase 2 — Minify / R8

### Achado importante (auditoria)

O Lab **não** usa `.toObject(dataClass)` do Firestore. Escritas/leituras são
`Map` + parsers manuais (`toCarteira`, `rascunhoParaMap`, etc.). A “mina clássica”
de R8 + POJO **não se aplica hoje** — mas:

1. `@Keep` nos modelos de finanças/perfil/conversa (cinto se alguém mudar pra POJO)
2. `-keep class com.ethan.orbitlab.data.**` no ProGuard
3. Smoke de **números** de finanças no APK minificado (ainda obrigatório)

### DoD

- [x] `@Keep` em Carteira, Lancamento, Recorrente, Meta, Transferencia, FaturaCredito, UserProfile, ConversaMeta
- [x] `proguard-rules.pro` (data.** + Firebase + OkHttp + Kotlin)
- [x] `isMinifyEnabled = true` + `isShrinkResources = true` no `release`
- [x] `assembleLabRelease` local verde com R8 (0.30.5 / 92)
- [x] Publish 0.30.5 pelo Actions
- [x] Smoke no celular (performance + uso) — Ethan

---

## Fase 3 — Crash reporting

**Firebase Crashlytics** (`luna-8787d`), via `google-services.json` + plugin
(mapping R8 sobe no build de release).

**DoD**

- [x] SDK + plugins Gradle + `CrashReporting`
- [x] uid opaco; sem chat/financeiro (política em `AGENTS.md`)
- [x] Breadcrumb: tela, versionName/versionCode
- [x] Gesto de teste: Ajustes → 7 toques na versão
- [ ] Publish **0.30.6** + crash de teste aparece no console Firebase — Ethan

**Humano:** atualizar pra 0.30.6 → Ajustes → 7× na versão → confirmar → reabrir app →
olhar Firebase Console → Crashlytics (pode levar alguns minutos).

---

## Fase 4 — Rate limit (luna-core)

**DoD**

- [x] Middleware uid + IP (`mobile-api/src/rateLimit.ts`)
- [x] `429` `rate_limited` ≠ `quota_exceeded` (+ `Retry-After`)
- [x] `/health` → `rateLimit: true`
- [x] Testes + mensagem humana no app (sem parede de cota)
- [x] Limites por env (`LUNA_RL_*`, `LUNA_RL_DISABLED`)

**Rotas:** chat, chat/stream, STT, vision, extract, buscar, rosary.
**Fora:** `/health`, billing/webhook, usage.

**Humano:** após deploy no Railway, `GET /health` deve mostrar `features.rateLimit: true`
e um `commit` novo. No app, rajada → aviso “rápido demais” (não “lua dormiu”).

---

## Fase 5 — Higiene de superfície

- [x] `OPENROUTER_API_KEY` vazia no CI (path `.env` fora do repo)
- [ ] Isolar `LunaDirectChat` pra debug local só
- [x] Docs SHA debug + release (`SIGNING.md` / `AGENTS.md`)

---

## Critério de plano concluído

1. Release key + updates em cadeia.
2. R8 + smoke de números de finanças.
3. Crashlytics.
4. Rate limit no core.
5. DirectChat fora do produto.

