# Testar / lançar o Lab sem notebook

## Fluxo recomendado: GitHub Actions

1. Abre https://github.com/NowardEthan/orbit-lab/actions  
2. **Build & Release Lab** → **Run workflow**
3. Preenche:
   - `version_name`: ex. `0.3.0`
   - `version_code`: ex. `3` (tem de ser **maior** que o instalado)
   - `publish`: ✅
4. Espera o job verde
5. No telemóvel (com a build antiga instalada) → Início → banner → atualizar

O workflow:
- Builda o APK (`assembleLabRelease`)
- Publica em https://github.com/NowardEthan/orbit-lab/releases/tag/lab (`orbit.apk`)
- Actualiza `updates-lab.json` no **main** (o app lê daqui)

## Só com o telemóvel (já há builds)

| | Versão | Link |
|---|--------|------|
| Antiga | 0.1.0 | [orbit-0.1.0.apk](https://github.com/NowardEthan/orbit-releases/releases/download/lab/orbit-0.1.0.apk) (legado) ou artefactos Actions |
| Nova | via banner | [orbit.apk](https://github.com/NowardEthan/orbit-lab/releases/download/lab/orbit.apk) |

Manifesto:  
https://raw.githubusercontent.com/NowardEthan/orbit-lab/main/updates-lab.json

1. Instala a 0.1.0  
2. Abre o app → banner  
3. Atualiza → nova versão

## Local (quando a rede aguentar)

```bash
./gradlew :app:assembleLabRelease -PlabVersionCode=3 -PlabVersionName=0.3.0
# ou: ./scripts/build-apk.sh --lab
```
