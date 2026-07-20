# Orbit (lab nativo)

Laboratório Android nativo (Kotlin + Jetpack Compose) do **Orbit**.
O app chama-se **Orbit** (igual ao de produção); este repo/pasta (`OrbitLab`) é só o lab Compose.

- **Destino:** substituir o app Expo (`orbit-mobile`) no mesmo auto-update (`orbit-releases`).
- **Dev local:** flavor `lab` → package `com.ethan.orbitlab` (não colide com o Orbit Expo instalado).
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

Ainda **não** publique o Lab no `orbit-releases` até a paridade mínima — ver
[`RELEASING.md`](RELEASING.md) quando for a hora.

## Build APK

```bash
./scripts/build-apk.sh --lab      # package lab (dev)
./scripts/build-apk.sh --beta     # substitui Orbit β
./scripts/build-apk.sh --stable   # substitui Orbit estável
```

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
