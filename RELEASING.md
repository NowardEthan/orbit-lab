# Release do OrbitLab (produção sideload)

> Cliente Android de produção da Luna. Package `com.ethan.orbitlab`.
> Canal público de update: repo **`orbit-releases`** → `updates-lab.json` + tag `lab`.
> Endurecimento (signing, R8, crash, rate limit, higiene OpenRouter): [`HARDENING.md`](HARDENING.md).

## Canal atual

| | |
|---|---|
| Flavor Gradle | **só `lab`** (não há `--beta` / `--stable` neste repo) |
| Manifesto produção | https://raw.githubusercontent.com/NowardEthan/orbit-lab-releases/main/updates-lab.json |
| Releases produção | https://github.com/NowardEthan/orbit-lab-releases/releases |
| Manifesto debug | https://raw.githubusercontent.com/NowardEthan/orbit-lab-debug-releases/main/updates-lab.json |
| Releases debug | https://github.com/NowardEthan/orbit-lab-debug-releases/releases |
| Ponte legado | https://raw.githubusercontent.com/NowardEthan/orbit-releases/main/updates-lab.json |
| Backend | luna-core (Railway), auth Firebase, billing/cota no servidor |
| Play Store | **fora de escopo** nesta fase — só sideload + auto-update |

`versionCode` / `versionName` do flavor `lab` vivem em `app/build.gradle.kts`
(override no CI com `-PlabVersionCode` / `-PlabVersionName`). **Só sobem.**

Produção e debug vivem em repos separados para o GitHub mostrar cada release no
histórico certo. O repo antigo `orbit-releases` fica como ponte: apps já instalados
que ainda leem esse manifesto recebem a próxima produção e, depois dessa build,
passam a ler `orbit-lab-releases`.

## Quando publicar

Quando o capítulo estiver estável o bastante pro grupo de usuários do sideload
(ou hotfix crítico: cota, crash de login, update quebrado).

## Como publicar (caminho oficial)

Ver o passo a passo completo em [`TESTE-UPDATE.md`](TESTE-UPDATE.md):

1. Actions → **Build & Release Lab** → Run workflow  
2. Preencher `version_name`, `version_code` (maior que o instalado), `release_channel=production`, novidades, `publish=✅`  
3. Secret `ORBIT_RELEASES_TOKEN` precisa existir (PAT com write nos repos de distribuição)  
4. No celular: banner na Início → atualizar

Sem o secret: o workflow ainda gera o APK nos **Artefatos** do run; aí você sobe
manual no `orbit-releases` (tag `lab` + commit do `updates-lab.json`).

## Build local (dev)

```bash
./gradlew :app:assembleLabRelease -PlabVersionCode=NN -PlabVersionName=X.Y.Z
# APK: app/build/outputs/apk/lab/release/
```

Assinatura: com `keystore.properties` / secrets `LAB_*`, o `labRelease` usa a
**release key**. Sem isso, build local cai na debug key (aviso no Gradle).
No CI a release key é **obrigatória** — ver [`SIGNING.md`](SIGNING.md).

**Migração:** a primeira APK com release key não atualiza por cima de builds
assinados com debug — desinstalar e reinstalar uma vez em cada aparelho.

## O que NÃO fazer

- Não publicar Flavor “beta/stable” deste repo — o script antigo e docs do Expo
  não se aplicam aqui.
- Não baixar APK do repo privado `orbit-lab` no celular — use `orbit-releases`.
- Não rebaixar `versionCode`.
- Não embutir segredos de LLM no APK de produção (caminho de produto = luna-core).

## Checklist rápido pré-publish

- [ ] Smoke: login, chat stream, planos/cota, finanças básica, banner de update
- [ ] `versionCode` > `latestVersionCode` do `updates-lab.json` atual
- [ ] Texto de novidades em pt-BR
- [ ] (Quando Fase 1+ existir) APK assinado com release key + SHA no Firebase
