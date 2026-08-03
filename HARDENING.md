# Endurecimento OrbitLab + luna-core (MVP produção sideload)

Plano para sair de “app que funciona” → “app que se opera com identidade estável,
visibilidade e freio de abuso”. **Não é Play Store** nesta rodada.

Cliente = este repo. API = `core/src/luna-core` (Railway).

Princípio: **cota e billing são verdade no servidor.** Parede no app é UX.

---

## Status

| Fase | Tema | Status |
|------|------|--------|
| **0** | Docs: Lab = produção sideload | **em curso** |
| **1** | Signing de release + CI + SHA Firebase | pendente |
| **2** | Minify / R8 no `labRelease` | pendente |
| **3** | Crash reporting (Crashlytics preferido) | pendente |
| **4** | Rate limit no luna-core | pendente (pode paralelizar com 3) |
| **5** | Higiene: sem chave OpenRouter no release; DirectChat fora do produto | pendente |

Ordem: `0 → 1 → 2 → 3`, com `4` em paralelo a partir de `1`/`3`.  
Signing **antes** de R8 (uma variável por release).

---

## Fase 0 — Docs alinhados

**DoD**

- [x] `AGENTS.md` deixa de dizer “não é produção” / “só lab de UI”
- [x] `RELEASING.md` descreve flavor `lab` + `updates-lab.json` + Actions
- [x] Este arquivo (`HARDENING.md`) com DoD das fases
- [ ] Mapa do monorepo (`luna-workspace/AGENTS.md`) aponta Lab como cliente sideload atual

**Humano:** nenhum passo de console nesta fase.

---

## Fase 1 — Signing de release

**Problema:** `labRelease` assina com debug keystore.

**DoD**

- [ ] Keystore de release gerada e **fora do Git** (backup seguro)
- [ ] Secrets no GitHub Actions (`LAB_KEYSTORE_BASE64`, senhas, alias)
- [ ] `signingConfigs.release` no Gradle; só `labRelease` usa essa chave
- [ ] CI assina e `apksigner verify` passa
- [ ] SHA-1/256 da release key no Firebase (`com.ethan.orbitlab`) + Google login OK no APK release
- [ ] Nota de migração: aparelhos com APK debug **não** atualizam in-place → desinstalar/reinstalar uma vez
- [ ] `RELEASING.md` / `TESTE-UPDATE.md` atualizados

**Humano (Ethan):** gerar keystore, guardar senhas, criar secrets, colar SHA no Firebase.  
O agente ensina o passo a passo na hora.

---

## Fase 2 — Minify / R8

**DoD**

- [ ] `isMinifyEnabled = true` no `release` (e shrink se estável)
- [ ] `proguard-rules.pro` cobre Compose, Firebase, OkHttp, serialização usada
- [ ] Smoke no APK minificado: login, chat, planos/cota, finanças, auto-update N→N+1
- [ ] Sem regressão óbvia de tamanho/crash na abertura

**Humano:** smoke no celular; reportar se algo quebrar.

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

---

## Fase 4 — Rate limit (luna-core)

**DoD**

- [ ] Middleware por **uid** (rotas autenticadas caras) + teto frouxo por **IP**
- [ ] `429` com código `rate_limited` (≠ `quota_exceeded`) + `Retry-After` quando fizer sentido
- [ ] Feature flag em `/health` (`rateLimit: true`)
- [ ] Testes do contador / estouro
- [ ] App trata mensagem humana sem confundir com parede de cota
- [ ] Limites ajustáveis por env

**Humano:** validar em staging/prod com smoke; ajustar números se doer UX.

---

## Fase 5 — Higiene de superfície

**DoD**

- [ ] CI/release **não** embute `OPENROUTER_API_KEY` no `BuildConfig`
- [ ] `LunaDirectChat` fora do caminho de produto (só debug local, se ainda existir)
- [ ] Docs listam SHA debug **e** release

---

## Critério de plano concluído

1. APK sideload com release key, atualizável em cadeia após a migração única.  
2. R8 ligado e smoke OK.  
3. Crash de teste visível no painel.  
4. Core devolve rate limit sob spam; health marca a feature.  
5. Docs e release batem com a realidade.
