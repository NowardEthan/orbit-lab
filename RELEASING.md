# Release do Orbit (Compose / OrbitLab)

> O app chama-se **Orbit**. Este repo é o lab Compose que **substitui** o Expo
> (`orbit-mobile`) no mesmo canal `orbit-releases`. Não existe `updates-lab.json`.

## Quando publicar

Só quando o capítulo Compose estiver bom o bastante pro β (e depois estável).
Até lá: flavor **`lab`** no telemóvel de desenvolvimento.

## Build

```bash
# Dev local (package com.ethan.orbitlab)
./scripts/build-apk.sh --lab

# Substituir o Orbit β instalado
./scripts/build-apk.sh --beta

# Substituir o Orbit estável
./scripts/build-apk.sh --stable
```

Saída: `dist/orbit.apk` (+ `dist/orbit-<versão>[-beta|-lab].apk`).

`versionCode` / `versionName` vivem em `app/build.gradle.kts` — **só sobem**, na
sequência do mobile (hoje 79 / 2.25.0).

## Publicar beta (mesmo fluxo do mobile)

1. `./scripts/build-apk.sh --beta`
2. Em `orbit-releases`: atualizar `updates-beta.json` (`latestVersion`,
   `latestVersionCode`, uma entrada `news`)
3. `gh release upload beta ./dist/orbit.apk --clobber` (tag fixa `beta`)

Quem tem o Orbit β atualiza e recebe o Compose.

## Publicar estável

1. Validar no β
2. `./scripts/build-apk.sh --stable`
3. `gh release create vX.Y.Z ./dist/orbit.apk`
4. Atualizar `updates.json` (+ espelhar no beta)

Detalhes do mural e cadência: `orbit-releases/AGENTS.md` e
`orbit-mobile/AGENTS.md` §4.
