# Assinatura de release (Fase 1) — guia pra Ethan

O wiring no Gradle/CI já está no repo. **Agora é a sua vez** nos passos abaixo.
Faça na ordem. Se travar num passo, me diga o número.

> Guarde senhas num gerenciador (1Password, Bitwarden, etc.).  
> **Perder a keystore = app “novo” pro Android** (usuários precisam desinstalar).

---

## Passo 1 — Gerar a keystore (uma vez)

No PowerShell, na pasta do repo `OrbitLab` (não precisa de Android Studio):

```powershell
keytool -genkeypair -v `
  -keystore orbit-lab-release.keystore `
  -alias orbitlab `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -storetype PKCS12
```

O `keytool` pergunta:

1. **Senha do keystore** — anote (vai virar `LAB_KEYSTORE_PASSWORD`)
2. **Senha da chave** — pode ser a mesma (vira `LAB_KEY_PASSWORD`)
3. Nome, organização, etc. — pode preencher com Luna / BR; não precisa ser perfeito

Arquivo gerado: `orbit-lab-release.keystore` na raiz do OrbitLab  
(está no `.gitignore` — **não** dê commit).

Se `keytool` não for encontrado: instale um JDK 17+ e use o caminho completo, ex.:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair ...
```

---

## Passo 2 — Arquivo local (opcional, pra build no PC)

```powershell
copy keystore.properties.example keystore.properties
# Edite keystore.properties com as senhas reais
```

Teste:

```powershell
.\gradlew :app:assembleLabRelease -PlabVersionCode=91 -PlabVersionName=0.30.4
```

Sem warning de “assinando com debug” no log do Gradle.

---

## Passo 3 — Extrair SHA-1 e SHA-256 (Firebase)

```powershell
keytool -list -v -keystore orbit-lab-release.keystore -alias orbitlab
```

Copie as linhas **SHA1** e **SHA256** (formato com `:`).

---

## Passo 4 — Cadastrar fingerprints no Firebase

1. Abra https://console.firebase.google.com/ → projeto **`luna-8787d`**
2. ⚙ Project settings → app Android **`com.ethan.orbitlab`**
3. **Add fingerprint** → cole o SHA-1 da release
4. Repita pro SHA-256
5. (Opcional) Confira em Google Cloud → Credentials se surgiu/atualizou o cliente OAuth Android

Espere 5–10 min antes de testar Google login no APK novo.

**Não remova** o SHA do debug — você ainda usa ele em builds de desenvolvimento.

---

## Passo 5 — Secrets no GitHub (orbit-lab)

1. Gere o Base64 do keystore (PowerShell):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\orbit-lab-release.keystore")) | Set-Clipboard
```

Isso copia o Base64 pro clipboard (pode ser um texto longo).

2. Abra https://github.com/NowardEthan/orbit-lab/settings/secrets/actions  
3. Crie **quatro** secrets:

| Nome | Valor |
|------|--------|
| `LAB_KEYSTORE_BASE64` | o Base64 do passo acima (cole tudo, uma linha) |
| `LAB_KEYSTORE_PASSWORD` | senha do keystore |
| `LAB_KEY_ALIAS` | `orbitlab` (ou o alias que você usou) |
| `LAB_KEY_PASSWORD` | senha da chave |

`ORBIT_RELEASES_TOKEN` continua existindo pra publicar.

---

## Passo 6 — Backup

- Copie `orbit-lab-release.keystore` pra um lugar seguro **fora** do PC de dia a dia (drive criptografado / cofre).
- Guarde as duas senhas + alias no mesmo cofre.
- Sem isso, um dia o CI some e você não gera mais updates da mesma identidade.

---

## Passo 7 — Me avisar

Quando 1–6 estiverem feitos, diga **“secrets prontos”**.  
Aí eu (ou você) rodo o Actions **Build & Release Lab** e confirmamos:

- job “Verificar assinatura” verde  
- APK **não** é Android Debug  
- Google login no celular com o APK novo

---

## Migração dos aparelhos (importante)

Hoje os celulares têm APK assinado com **debug**.  
A primeira build com **release key** **não** atualiza por cima — o Android bloqueia.

Em cada aparelho do grupo:

1. Desinstalar o Lab antigo  
2. Instalar o APK novo (release)  
3. A partir daí, o banner de update volta a funcionar em cadeia

Avise o grupo antes da primeira publish com a chave nova.

---

## Referência rápida

| Item | Onde |
|------|------|
| Wiring Gradle | `app/build.gradle.kts` |
| CI | `.github/workflows/build-lab.yml` |
| Exemplo props | `keystore.properties.example` |
| Plano geral | `HARDENING.md` |
