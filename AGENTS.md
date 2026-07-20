# OrbitLab — diretrizes

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

## Estrutura

```
app/src/main/java/com/ethan/orbitlab/
  shell/     # OrbitShell + navegação
  ui/        # telas
  data/      # repositórios
  demo/      # fixtures de demo
  ui/theme/  # OrbitTokens + OrbitFills + OrbitMetrics + OrbitMotion
```
