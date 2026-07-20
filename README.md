# OrbitLab

Laboratório Android nativo (Kotlin + Jetpack Compose) do ecossistema Luna/Orbit.

- **package:** `com.ethan.orbitlab`
- **Guarda-chuva:** [`luna-workspace`](https://github.com/NowardEthan/luna-workspace) (submódulo `OrbitLab/`)
- **App de produção (Expo):** [`orbit-mobile`](https://github.com/NowardEthan/orbit-mobile) — este repo é o *lab*, não o substituto.

## Abrir no Android Studio

1. File → Open → pasta `OrbitLab`
2. Deixa o Gradle syncar (usa o JDK do Android Studio)
3. Corre o módulo `:app` num emulador ou dispositivo

`local.properties` é gerado pela IDE (SDK path) e **não** vai para o Git.

## Estrutura rápida

```
app/src/main/java/com/ethan/orbitlab/
  MainActivity.kt
  shell/          # shell + navegação
  ui/             # telas (início, chat, conversas, …)
  data/           # repositórios
  demo/           # dados de demonstração
  ui/theme/       # tokens / tema Compose
```
