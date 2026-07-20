# OrbitLab — diretrizes (repo do lab)

> Laboratório Android **nativo** (Kotlin + Jetpack Compose) do produto **Orbit**.
> O nome do app (launcher, UI) é **Orbit** — «OrbitLab» é só a pasta/repo do lab.
> Destino: **substituir** o [`orbit-mobile/`](../orbit-mobile/AGENTS.md) (Expo) no mesmo canal de
> auto-update. Escreva tudo em **pt-BR**.

## O que é

App de experimentação de UI/shell **Orbit**, para validar ecrãs e navegação em Compose **sem**
tocar no `android/` versionado do Expo — até herdar o `applicationId` de produção e ser o APK
publicado em `orbit-releases`.

| | |
|---|---|
| Repo | https://github.com/NowardEthan/orbit-lab (privado) |
| Guarda-chuva | submódulo `OrbitLab/` em `luna-workspace` |
| Abrir | Android Studio → pasta `OrbitLab` |

## Armadilhas

1. **Não misturar com `orbit-mobile` no dia a dia.** Use o flavor **`lab`** (`com.ethan.orbitlab`)
   para desenvolver — não sobrescreve o Orbit instalado. Flavors `stable` / `beta` usam os
   pacotes de produção e **só** existem para a substituição.
2. **Não commitar** `local.properties`, APKs, nem segredos. O `app/debug.keystore` é o mesmo do
   mobile (sideload) — necessário para o update in-place; não troque sem combinar.
3. Copy de UI em **pt-BR**.
4. **`versionCode` só sobe** (mesma regra do mobile). Continua a sequência do Expo.

## Flavors (canal)

| Flavor | applicationId | Manifesto | Uso |
|--------|---------------|-----------|-----|
| `lab` (default no dia a dia) | `com.ethan.orbitlab` | `orbit-lab/updates-lab.json` | Dev + CI/Actions |
| `beta` | `com.luna.orbitmobile.beta` | `updates-beta.json` | Substituir o Orbit β |
| `stable` | `com.luna.orbitmobile` | `updates.json` | Substituir o Orbit estável |

Correr: `./gradlew :app:installLabDebug` (ou `BetaRelease` / `StableRelease` quando for publicar).

## Auto-update (dentro do Lab)

Mesmo contrato do orbit-mobile — **não** há `updates-lab.json`. O Lab lê o canal que vai herdar:

- Código: `app/src/main/java/com/ethan/orbitlab/updates/`
- UI: banner na Início (`UpdateBanner`) + secção em Ajustes
- Comparação por `versionCode` (string só como reserva)
- Download + `FileProvider` + intent de instalação (`REQUEST_INSTALL_PACKAGES`)
- **Testar Lab→Lab:** [`TESTE-UPDATE.md`](TESTE-UPDATE.md) (`updates-lab.json`, flavor lab code 1→2)

Quando o Lab estiver pronto para **substituir** o Expo: build `beta`/`stable` → `orbit.apk`
nos canais oficiais (não uses o `updates-lab.json` para isso).

## Estrutura

```
app/src/main/java/com/ethan/orbitlab/
  shell/     # OrbitShell + navegação
  ui/        # ecrãs (inclui ui/rotina/)
  data/      # repositórios (chat + rotina in-memory)
  domain/    # lógica pura (rotina: agora, vigência)
  demo/      # fixtures de demo
  updates/   # manifesto + download/install
  ui/theme/  # tokens Compose
```

## Rotina (MVP local)

Tela completa em `ui/rotina/` — chips Normal/alternativas, cartão Agora, dias,
lista com buracos, detalhe Hoje/Fixas. Dados em `RotinaRepository` (memória).

Ainda stub: alarme nativo, painel Agora, chat Luna do bloco, Firestore.