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
| **1** | Signing de release + CI + SHA Firebase | **feita** (0.30.4; login OK no aparelho) |
| **2** | Minify / R8 no `labRelease` | **em curso** → 0.30.5 |
| **3** | Crash reporting (Crashlytics preferido) | pendente |
| **4** | Rate limit no luna-core | pendente (pode paralelizar com 3) |
| **5** | Higiene: OpenRouter no APK + DirectChat | **parcial** — chave já vazia no CI; falta isolar DirectChat |

Ordem: `0 → 1 → 2 → 3`, com `4` em paralelo a partir de `1`/`3`.

---

## Fase 0 — Docs alinhados

**DoD**

- [x] `AGENTS.md` / `RELEASING.md` / este arquivo / mapa do monorepo

---

## Fase 1 — Signing de release

**Guia:** [`SIGNING.md`](SIGNING.md)

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
- [ ] Smoke no celular: login, chat, **números de finanças**, update 0.30.4→0.30.5
- [ ] Publish 0.30.5 pelo Actions

**Humano:** depois do install, abrir Finanças e conferir saldos/lançamentos reais.

---

## Fase 3 — Crash reporting

Preferência: **Firebase Crashlytics** (`luna-8787d`).

**DoD**

- [ ] SDK no flavor lab
- [ ] uid opaco; sem chat/financeiro no crash
- [ ] Breadcrumb: tela, versionName/versionCode
- [ ] Crash de teste no console
- [ ] Política de PII no `AGENTS.md`

**Lembrete:** Crashlytics não pega corrupção silenciosa de dados — smoke de finanças manda.

---

## Fase 4 — Rate limit (luna-core)

**DoD**

- [ ] Middleware uid + IP
- [ ] `429` `rate_limited` ≠ `quota_exceeded`
- [ ] `/health` → `rateLimit: true`
- [ ] Testes + mensagem humana no app
- [ ] Limites por env

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
