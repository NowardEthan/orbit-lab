# Lançar o Lab pelo GitHub (sem notebook)

O repo `orbit-lab` é **privado** — o celular não baixa APK/manifesto dele.
O canal de produção sideload publica no **`orbit-releases`** (público); o build
roda no Actions do `orbit-lab`.

Detalhes de cadência: [`RELEASING.md`](RELEASING.md). Endurecimento: [`HARDENING.md`](HARDENING.md).

## 1. Secret (uma vez)

1. Crie um PAT no GitHub (classic ou fine-grained) com **contents: write** no repo
   `NowardEthan/orbit-releases`
2. Em https://github.com/NowardEthan/orbit-lab/settings/secrets/actions  
   → **New repository secret**  
   → nome: `ORBIT_RELEASES_TOKEN`  
   → valor: o PAT

Secrets da keystore de release (obrigatórios no CI): ver [`SIGNING.md`](SIGNING.md)
(`LAB_KEYSTORE_BASE64`, `LAB_KEYSTORE_PASSWORD`, `LAB_KEY_ALIAS`, `LAB_KEY_PASSWORD`).

## 2. Rodar o build

1. https://github.com/NowardEthan/orbit-lab/actions  
2. **Build & Release Lab** → **Run workflow**  
3. Ex.: `version_name=0.30.4`, `version_code=91`, `publish=✅`  
4. Espere ficar verde

O workflow:

- Builda o APK (`assembleLabRelease`)
- Sobe `orbit.apk` na tag `lab` do **orbit-releases**
- Atualiza `updates-lab.json` no **main** do orbit-releases
- Guarda também o APK nos **Artefatos** do run (download manual se precisar)

## 3. No celular

Com uma versão anterior do Lab instalada (mesma assinatura):

1. Abre o app → banner na Início  
2. Atualiza → nova versão

Manifesto:  
https://raw.githubusercontent.com/NowardEthan/orbit-releases/main/updates-lab.json

Release/tag:  
https://github.com/NowardEthan/orbit-releases/releases/tag/lab

## Sem o secret

O workflow ainda **builda** e deixa o APK nos Artefatos. Aí você baixa no PC e
publica manual no `orbit-releases` (tag `lab` + commit do `updates-lab.json`).

## Assinatura

- **CI:** sempre assina com a release key (secrets `LAB_*`).
- **Local sem keystore:** ainda pode cair na debug key (só pra dev).
- **Migração:** aparelhos com APK antigo (debug) **desinstalam e reinstalam uma vez**
  na primeira build release; depois o update encadeia de novo.
