# Testar o auto-update do Lab

Canal isolado: flavor **`lab`** → `updates-lab.json` → release tag **`lab`**.
Não mexe no Orbit Expo.

| | Versão | Code | Onde |
|---|--------|------|------|
| Instalar primeiro (antiga) | 0.1.0 | 1 | [orbit-0.1.0.apk](https://github.com/NowardEthan/orbit-releases/releases/download/lab/orbit-0.1.0.apk) |
| Update (nova) | 0.2.0 | 2 | [orbit.apk](https://github.com/NowardEthan/orbit-releases/releases/download/lab/orbit.apk) (via banner) |

Manifesto já no `main`:  
https://raw.githubusercontent.com/NowardEthan/orbit-releases/main/updates-lab.json

## Só com o telemóvel (sem notebook)

1. No telemóvel, abre o link da **0.1.0** e instala o APK  
   (permite «fontes desconhecidas» / instalar apps do Chrome se pedir)
2. Abre o app **Orbit** (`com.ethan.orbitlab`) — em Ajustes deve dizer `v0.1.0 (1)` e canal lab
3. Na **Início** → banner «Nova versão disponível»
4. Toca → baixa → confirma instalação
5. Ajustes → `v0.2.0 (2)` ✅

## Com notebook (Android Studio)

```bash
cd OrbitLab
# labDebug já está em 0.1.0 — Run no telemóvel
# Ou: adb install -r  (depois de baixar orbit-0.1.0.apk)
```

Depois segue o mesmo: abrir app → banner → atualizar.

## Se o banner não aparecer

- Internet a bloquear GitHub
- Já estás em code ≥ 2
- Ajustes não diz «Canal lab (dev)»
