# OrbitLab — diretrizes (app de produção sideload)

> App Android **nativo** (Kotlin + Jetpack Compose) da Luna / Orbit.
> **Este é o cliente de produção atual** (distribuição sideload via `orbit-releases`).
> O Expo em [`orbit-mobile/`](../orbit-mobile/) ficou em desenvolvimento / legado — não misturar.
> Idioma: **pt-BR** (Brasil) em UI, comentários e docs — **nunca pt-PT**
> (sem “ecrã”, “ficheiro”, “controlo”, “sítio”, “actual”).

## O que é

Cliente mobile da Luna (`com.ethan.orbitlab`): chat, planos/billing, finanças, galeria,
perfil e auto-update. Backend = **luna-core** (Railway). Canal de update =
`updates-lab.json` no repo público `orbit-releases`.

| | |
|---|---|
| Repo | https://github.com/NowardEthan/orbit-lab (privado) |
| Guarda-chuva | submódulo `OrbitLab/` em `luna-workspace` |
| Abrir | Android Studio → pasta `OrbitLab` |
| Release | [`RELEASING.md`](RELEASING.md) · assinatura [`SIGNING.md`](SIGNING.md) · endurecimento [`HARDENING.md`](HARDENING.md) |
| Bolha (chat-head) | [`BOLHA-ROADMAP.md`](BOLHA-ROADMAP.md) · fase atual [`bolha/B0-fundacao.md`](bolha/B0-fundacao.md) · código `ui/bolha/` |
| Build/CI | [`.github/workflows/build-lab.yml`](.github/workflows/build-lab.yml) · [`TESTE-UPDATE.md`](TESTE-UPDATE.md) |

## Armadilhas

1. **Não misturar com `orbit-mobile`.** Produto estável vive aqui; o Expo não é o caminho de ship.
2. **Não commitar** `local.properties`, APKs, keystores, nem segredos.
3. Copy de UI em **pt-BR** (Brasil). Se soar a Portugal, reescreva.
4. **Cota / billing** são verdade no **luna-core**. Parede no app é UX; APK antiga não fura carteira.
5. Release endurecido (signing, R8, Crashlytics, rate limit, DirectChat fora) — ver [`HARDENING.md`](HARDENING.md).
6. **Sem chave OpenRouter no APK de produção.** Chat = luna-core. `LunaDirectChat` é harness de debug.

## Crashlytics / privacidade nos crashes

SDK em `data/crash/CrashReporting.kt` (Firebase Crashlytics, projeto `luna-8787d`).

**O que pode ir no crash:**

- uid opaco do Auth
- tela atual (nome da aba: `INICIO`, `CHAT`…)
- `versionName` / `versionCode` / canal `lab`

**O que NUNCA vai:**

- e-mail, nome, @username
- texto de chat, prompts, URLs de mídia
- valores financeiros, descrição de lançamento

Teste: Ajustes → toque 7× na linha da versão → confirmar crash. Painel:
Firebase Console → Crashlytics.

## Visual — base cinza, cor como detalhe

A impressão dominante do app é **dark / cinza contido** (`ink`, `surface`, tipografia quieta).
Espaçamento e raios: [`OrbitMetrics`](app/src/main/java/com/ethan/orbitlab/ui/theme/OrbitMetrics.kt).

[`OrbitFills`](app/src/main/java/com/ethan/orbitlab/ui/theme/OrbitFills.kt) são **acentos pontuais**, não a tela inteira:

| Usar fill | Não usar fill |
|-----------|----------------|
| Cards Luz / Lua (Início) | Ícones de cada linha em listas (Ajustes) |
| Chips de estado (Novo, agora, versão) | Stats / badges de sistema enchendo a tela |
| FAB da barra, anel do avatar | Botões destrutivos / “Sair” como bloco saturado |
| Conquistas desbloqueadas (troféus) | Listas inteiras coloridas |

**Anti-padrões:**

1. `cor.copy(alpha = 0.15f)` + texto na mesma cor (sem contraste).
2. Preencher listas / stats / chrome com `OrbitFills` — a base é cinza.

Cores base: [`OrbitTokens`](app/src/main/java/com/ethan/orbitlab/ui/theme/OrbitTokens.kt).

## Motion

Animações via [`OrbitMotion`](app/src/main/java/com/ethan/orbitlab/ui/theme/OrbitMotion.kt):
`orbitEnter` (stagger), `orbitPressable` / `rememberOrbitPressScale`, springs/tweens compartilhados.

- Curto e intencional (premium), não barulho.
- Idle (sem toque): brilho em faixa (`clipToBounds` + offset só na faixa), glow no ícone, orbs. **Nunca** `scale`/`translation` no card inteiro — o texto treme.
- Evitar `infiniteTransition` agressivo fora do Chat (gravador) e destes idles sutis.

## Google Sign-In (Firebase)

Projeto Firebase **`luna-8787d`**, com `WEB_CLIENT_ID` em
`data/firebase/FirebaseBootstrap.kt`. O botão Google usa Credential Manager
(`GetSignInWithGoogleOption`).

**Package:** `com.ethan.orbitlab` (≠ `com.luna.orbitmobile` do Expo).

Se o login diz «cancelado» sem o usuário cancelar, falta o **cliente OAuth Android**
para este package + fingerprint do keystore em uso.

### SHA-1 do debug (dev / builds atuais com debug key)

```
SHA-1: 5E:8F:16:06:2E:A3:CD:2C:4A:0D:54:78:76:BA:A6:F3:8C:AB:F6:25
```

Passos (Ethan, console):

1. [Firebase](https://console.firebase.google.com/) → `luna-8787d` → ⚙ Project settings
2. App Android `com.ethan.orbitlab` → **Add fingerprint**
3. Cola o SHA-1 (e o SHA-256 se pedir)
4. Em Google Cloud → APIs & Services → Credentials: confirma OAuth client tipo **Android**
   com esse package + SHA-1
5. Espera 5–10 min e tenta de novo no celular

Quando existir **keystore de release** (Fase 1 do hardening), cadastrar também o SHA-1/256
dessa chave — senão o Google login quebra só no APK de produção.

O `WEB_CLIENT_ID` (tipo Web) **não muda**.

## Latência (bench)

Harness CLI no guarda-chuva: [`scripts/bench-luna-latencia/`](../scripts/bench-luna-latencia/README.md).

No dispositivo, cada turno registra no Logcat (`OrbitLatencia`) via
`data/latencia/LatenciaProbe`.

```bash
adb logcat -s OrbitLatencia
```

## Estrutura

```
app/src/main/java/com/ethan/orbitlab/
  shell/     # OrbitShell + navegação
  ui/        # telas (início, chat, finanças, planos, …)
  data/      # repositórios (auth, luna-api, billing, finanças, updates)
  demo/      # fixtures de demo
  ui/theme/  # OrbitTokens + OrbitFills + OrbitMetrics + OrbitMotion
```
