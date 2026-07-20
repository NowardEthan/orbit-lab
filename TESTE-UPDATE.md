# Lançar o Lab pelo GitHub (sem notebook)

O repo `orbit-lab` é **privado** — o telemóvel não consegue baixar APK/manifesto
dele. Por isso o canal lab publica no **`orbit-releases`** (público), e o build
corre no Actions do `orbit-lab`.

## 1. Secret (uma vez)

1. Cria um PAT em GitHub (classic ou fine-grained) com **contents: write** no repo
   `NowardEthan/orbit-releases`
2. Em https://github.com/NowardEthan/orbit-lab/settings/secrets/actions  
   → **New repository secret**  
   → nome: `ORBIT_RELEASES_TOKEN`  
   → valor: o PAT

## 2. Correr o build

1. https://github.com/NowardEthan/orbit-lab/actions  
2. **Build & Release Lab** → **Run workflow**
3. Ex.: `version_name=0.3.0`, `version_code=3`, `publish=✅`
4. Espera ficar verde

O workflow:
- Builda o APK
- Sobe `orbit.apk` na tag `lab` do **orbit-releases**
- Actualiza `updates-lab.json` no **main** do orbit-releases
- Guarda também o APK nos **Artefactos** do run (download manual se precisares)

## 3. No telemóvel

Com a **0.1.0** instalada:

1. Abre o app → banner na Início  
2. Atualiza → nova versão

APK base (0.1.0):  
https://github.com/NowardEthan/orbit-releases/releases/download/lab/orbit-0.1.0.apk

Manifesto:  
https://raw.githubusercontent.com/NowardEthan/orbit-releases/main/updates-lab.json

## Sem o secret

O workflow ainda **builda** e deixa o APK nos Artefactos. Aí baixas no PC e:
`gh release upload lab ./orbit.apk --repo NowardEthan/orbit-releases --clobber`
(+ commit do `updates-lab.json`).
