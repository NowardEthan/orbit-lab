# Testar o auto-update do Lab (no notebook)

O flavor **`lab`** (`com.ethan.orbitlab`) lê **`updates-lab.json`** — não mexe no
Orbit Expo (estável/β). Versão instalada de teste: **0.1.0 (code 1)**. Manifesto
aponta para **0.2.0 (code 2)**.

## Passo a passo

### 1. Instalar a versão «antiga» (0.1.0)

No Android Studio: flavor **`labDebug`** → Run no telemóvel.

Ou:

```bash
cd OrbitLab
./scripts/build-apk.sh --lab --debug
adb install -r dist/orbit.apk
```

Confirma em Ajustes: canal lab, versão `v0.1.0 (1)`.

### 2. Gerar a versão «nova» (0.2.0)

Em `app/build.gradle.kts`, no flavor `lab`:

```kotlin
versionCode = 2
versionName = "0.2.0"
```

Depois:

```bash
./scripts/build-apk.sh --lab
# → dist/orbit.apk  (package com.ethan.orbitlab, code 2)
```

### 3. Publicar o APK no GitHub (tag `lab`)

No repo **orbit-releases** (com `gh` autenticado):

```bash
# Criar a release pré uma vez (se ainda não existir):
gh release create lab ./dist/orbit.apk --repo NowardEthan/orbit-releases \
  --prerelease --title "lab 0.2.0" --notes "Primeiro APK Compose para testar update"

# Nas próximas: só substituir o asset
gh release upload lab ./dist/orbit.apk --repo NowardEthan/orbit-releases --clobber
```

O `updates-lab.json` já aponta para:
`…/releases/download/lab/orbit.apk`

Se mudares versão/code no manifesto, faz commit+push do JSON **depois** do upload.

### 4. Abrir o Lab e atualizar

1. Abre o app (ainda 0.1.0)
2. Na **Início** deve aparecer o banner «Nova versão disponível»
3. Toca → baixa → o Android pede para instalar
4. Na 1ª vez: permite «instalar apps desconhecidas» para o Orbit
5. Confirma em Ajustes: `v0.2.0 (2)`

## Se o banner não aparecer

- Sem internet / VPN a bloquear GitHub
- Manifesto ainda não no `main` do orbit-releases (o app lê `main`)
- `versionCode` instalado ≥ `latestVersionCode` do JSON
- Ajustes → Atualizações: confirma «Canal lab (dev)»

## Importante

- **`updates-lab.json`** = só o package do lab. Não substitui o Expo.
- Quando fores **substituir** o Orbit de verdade, usas flavors `beta`/`stable` +
  `updates-beta.json` / `updates.json` (sem este ficheiro).
