# OrbitLab — diretrizes (repo do lab)

> Laboratório Android **nativo** (Kotlin + Jetpack Compose). Não é o app de produção.
> Produção = [`orbit-mobile/`](../orbit-mobile/AGENTS.md) (Expo).
> Idioma: **pt-BR** (Brasil) em UI, comentários e docs — **nunca pt-PT** (sem “ecrã”, “ficheiro”, “controlo”, “sítio”, “actual”).

## O que é

App de experimentação de UI/shell Orbit (`com.ethan.orbitlab`), para validar telas e navegação
em Compose **sem** tocar no `android/` versionado do Expo.

| | |
|---|---|
| Repo | https://github.com/NowardEthan/orbit-lab (privado) |
| Guarda-chuva | submódulo `OrbitLab/` em `luna-workspace` |
| Abrir | Android Studio → pasta `OrbitLab` |

## Armadilhas

1. **Não misturar com `orbit-mobile`.** Mudanças de produto estáveis vão para o Expo; aqui é lab.
2. **Não commitar** `local.properties`, APKs, nem segredos.
3. Copy de UI em **pt-BR** (Brasil). Se soar a Portugal, reescreva.

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

O Lab usa o projeto Firebase **`luna-8787d`** (mesmo do mobile), com
`WEB_CLIENT_ID` em `data/firebase/FirebaseBootstrap.kt`. O botão Google usa
Credential Manager (`GetSignInWithGoogleOption`).

**Package do Lab:** `com.ethan.orbitlab` (≠ `com.luna.orbitmobile` do Expo).

Se o login diz «cancelado» sem o utilizador cancelar, falta o **cliente OAuth Android**
para este package + SHA-1 do `app/debug.keystore`:

```
SHA-1: 5E:8F:16:06:2E:A3:CD:2C:4A:0D:54:78:76:BA:A6:F3:8C:AB:F6:25
```

Passos (Ethan, consola):

1. [Firebase](https://console.firebase.google.com/) → `luna-8787d` → ⚙ Project settings  
2. **Add app** → Android → package `com.ethan.orbitlab`  
   (ou, na app Android já existente, **Add fingerprint**)  
3. Cola o SHA-1 acima (e o SHA-256 se pedir)  
4. Em Google Cloud → APIs & Services → Credentials: confirma que existe
   OAuth client tipo **Android** com esse package + SHA-1  
5. Espera 5–10 min a propagar e tenta de novo no telemóvel

O `WEB_CLIENT_ID` (tipo Web) **não muda** — continua o que está no código.

## Latência (bench PAIA vs OpenRouter)

Harness CLI no guarda-chuva: [`scripts/bench-luna-latencia/`](../scripts/bench-luna-latencia/README.md).

No dispositivo, cada turno regista no Logcat (`OrbitLatencia`) via
`data/latencia/LatenciaProbe` — caminhos `openrouter_direct`, `paia_json` e `paia_stream`.

```bash
adb logcat -s OrbitLatencia
```

## Estrutura

```
app/src/main/java/com/ethan/orbitlab/
  shell/     # OrbitShell + navegação
  ui/        # telas
  data/      # repositórios
  demo/      # fixtures de demo
  ui/theme/  # OrbitTokens + OrbitFills + OrbitMetrics + OrbitMotion
```


## Rotina (MVP local)

Tela completa em `ui/rotina/` — chips Normal/alternativas, cartão Agora, dias,
lista com buracos, detalhe Hoje/Fixas. Dados em `RotinaRepository` (memória).

Ainda stub: alarme nativo, painel Agora, chat Luna do bloco, Firestore.