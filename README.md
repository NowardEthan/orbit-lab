# OrbitLab

Laboratório Android nativo (Kotlin + Jetpack Compose) do ecossistema Luna/Orbit.

- **Destino:** substituir o app Expo (`orbit-mobile`) no mesmo auto-update (`orbit-releases`).
- **Dev local:** flavor `lab` → package `com.ethan.orbitlab` (não colide com o Orbit instalado).
- **Produção (quando for a hora):** flavors `beta` / `stable` → mesmos packages do Orbit.

## Abrir no Android Studio

1. File → Open → pasta `OrbitLab`
2. Deixa o Gradle syncar (usa o JDK do Android Studio)
3. Corre o flavor **`labDebug`** num emulador ou dispositivo

`local.properties` é gerado pela IDE (SDK path) e **não** vai para o Git.

## Auto-update

O Lab já traz o cliente de atualizações (mesmo manifesto do mobile):

- Lê `updates.json` (stable) ou `updates-beta.json` (beta/lab)
- Banner na Início quando há versão nova
- Ajustes mostram canal + versão instalada

Ainda **não** publique o Lab no `orbit-releases` até a paridade mínima — o fluxo de release continua
documentado em `orbit-mobile/AGENTS.md` e `orbit-releases/AGENTS.md`.

## Estrutura rápida

```
app/src/main/java/com/ethan/orbitlab/
  MainActivity.kt
  shell/          # shell + navegação
  ui/             # ecrãs (início, chat, conversas, …)
  updates/        # auto-update (manifesto + install)
  data/           # repositórios
  demo/           # dados de demonstração
  ui/theme/       # tokens / tema Compose
```
