# OrbitLab — diretrizes

> Laboratório Android **nativo** (Kotlin + Jetpack Compose). Não é o app de produção.
> Produção = [`orbit-mobile/`](../orbit-mobile/AGENTS.md) (Expo). Escreva tudo em **pt-BR**.

## O que é

App de experimentação de UI/shell Orbit (`com.ethan.orbitlab`), para validar ecrãs e navegação
em Compose **sem** tocar no `android/` versionado do Expo.

| | |
|---|---|
| Repo | https://github.com/NowardEthan/orbit-lab (privado) |
| Guarda-chuva | submódulo `OrbitLab/` em `luna-workspace` |
| Abrir | Android Studio → pasta `OrbitLab` |

## Armadilhas

1. **Não misturar com `orbit-mobile`.** Mudanças de produto estáveis vão para o Expo; aqui é lab.
2. **Não commitar** `local.properties`, APKs, nem segredos.
3. Copy de UI em **pt-BR**.

## Estrutura

```
app/src/main/java/com/ethan/orbitlab/
  shell/     # OrbitShell + navegação
  ui/        # ecrãs
  data/      # repositórios
  demo/      # fixtures de demo
  ui/theme/  # tokens Compose
```
