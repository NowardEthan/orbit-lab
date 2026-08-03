# Assinatura de release do OrbitLab — guia passo a passo

Este guia é pra **você (Ethan)** fazer a Parte Humana da Fase 1 do endurecimento.
O código/CI já está pronto no repo. Sem estes passos, o GitHub Actions **não consegue**
publicar um APK “oficial” assinado.

Leia com calma. Faça **na ordem**. Se travar, anote o número do passo e me chama.

| | |
|---|---|
| Tempo estimado | 20–40 min (primeira vez) |
| Faz uma vez | Gerar keystore + secrets + SHA no Firebase + backup |
| Depois | Só publicar builds (Actions) — a chave fica nos secrets |

---

## O que é isso (em 30 segundos)

Todo APK Android precisa de uma **assinatura** — uma identidade digital do app.

- Hoje o Lab usa a chave de **debug** (a do desenvolvimento).
- Em produção sideload queremos uma chave de **release** só nossa.
- Essa chave mora num arquivo chamado **keystore** (tipo um cofre com cadeado).
- O celular só aceita **update** se a assinatura for a **mesma** da instalação anterior.

Por isso a **primeira** APK com chave nova exige **desinstalar e reinstalar** nos aparelhos
que ainda têm o Lab antigo (assinado com debug). Depois disso, updates voltam a encadear.

```
[você gera a keystore]
        ↓
[cola SHA no Firebase]  ← Google login no APK release
        ↓
[cola secrets no GitHub]
        ↓
[Actions builda e assina]
        ↓
[primeira install = desinstalar o antigo]
        ↓
[próximas = banner de update normal]
```

---

## Antes de começar — checklist

- [ ] PC com Windows e PowerShell
- [ ] Acesso ao GitHub do repo `NowardEthan/orbit-lab` (Settings → Secrets)
- [ ] Acesso ao Firebase do projeto `luna-8787d`
- [ ] Um gerenciador de senhas (1Password, Bitwarden, Notion privado… qualquer cofre)
- [ ] Pasta do repo OrbitLab no disco (Android Studio ou clone)

**Não** commite o arquivo `.keystore` nem as senhas no Git.

---

## Passo 1 — Abrir o PowerShell na pasta certa

1. Abra o Explorer na pasta do OrbitLab, algo como:

   `C:\Users\ethan\Documents\Projects\Luna\OrbitLab`

2. Barra de endereço → digite `powershell` → Enter  
   (ou botão direito → “Abrir no Terminal”)

3. Confirme:

```powershell
pwd
# Deve mostrar ...\OrbitLab
```

---

## Passo 2 — Achar o `keytool`

O `keytool` vem com o Java / Android Studio.

Teste:

```powershell
keytool -help
```

Se disser que não encontrou o comando, use o do Android Studio (ajuste o caminho se o seu for outro):

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -help
```

Se esse funcionar, **troque** `keytool` por esse caminho completo nos comandos abaixo  
(ou crie um alias na sessão):

```powershell
Set-Alias keytool "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
```

---

## Passo 3 — Gerar a keystore (uma vez na vida)

Cole isto no PowerShell **dentro da pasta OrbitLab**:

```powershell
keytool -genkeypair -v `
  -keystore orbit-lab-release.keystore `
  -alias orbitlab `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -storetype PKCS12
```

### O que o programa vai perguntar

| Pergunta | O que fazer |
|----------|-------------|
| Senha do keystore | Invente uma senha **forte**. Anote no cofre como `LAB_KEYSTORE_PASSWORD`. |
| Confirmar senha | Digite de novo. |
| Senha da chave (key) | Pode ser **a mesma**. Anote como `LAB_KEY_PASSWORD`. |
| Nome e sobrenome | Ex.: `Ethan` ou `Luna Orbit` |
| Unidade organizacional | Ex.: `Luna` |
| Organização | Ex.: `Luna Core` |
| Cidade | Ex.: `Brasilia` (sem acento se reclamar) |
| Estado | Ex.: `DF` |
| Código do país | `BR` |
| Confirmar tudo? | `sim` / `yes` |

### Resultado esperado

Na pasta OrbitLab aparece o arquivo:

```
orbit-lab-release.keystore
```

Ele está no `.gitignore` — **não** deve ir pro Git. Se o `git status` listar esse arquivo
como “novo a commitar”, **não** dê `git add` nele.

---

## Passo 4 — (Opcional) Build local assinado

Útil pra testar no PC antes do CI.

```powershell
copy keystore.properties.example keystore.properties
notepad keystore.properties
```

Preencha assim (troque as senhas):

```properties
storeFile=orbit-lab-release.keystore
storePassword=SUA_SENHA_DO_KEYSTORE
keyAlias=orbitlab
keyPassword=SUA_SENHA_DA_CHAVE
```

Salve e rode:

```powershell
.\gradlew :app:assembleLabRelease -PlabVersionCode=91 -PlabVersionName=0.30.4
```

No log do Gradle **não** deve aparecer o aviso de “assinando com debug”.  
APK em: `app\build\outputs\apk\lab\release\`

---

## Passo 5 — Extrair SHA-1 e SHA-256

O Firebase precisa desses “digitais” da chave pra o **Google Sign-In** funcionar no APK release.

```powershell
keytool -list -v -keystore orbit-lab-release.keystore -alias orbitlab
```

Digite a senha do keystore quando pedir.

Na saída, procure blocos assim (valores inventados):

```
SHA1: AA:BB:CC:...
SHA256: 11:22:33:...
```

**Copie os dois** (com os dois-pontos) pra um bloco de notas temporário.

---

## Passo 6 — Cadastrar no Firebase

1. Abra https://console.firebase.google.com/
2. Projeto **`luna-8787d`**
3. ⚙ **Project settings** (engrenagem)
4. Role até o app Android **`com.ethan.orbitlab`**
5. Em **SHA certificate fingerprints** → **Add fingerprint**
6. Cole o **SHA-1** da release → Save
7. **Add fingerprint** de novo → cole o **SHA-256** → Save

### Importante

- **Não apague** o SHA do **debug** (o que já está lá). Dev e release convivem.
- Espere **5–10 minutos** pra propagar antes de testar login Google no APK novo.
- Se o login Google disser “cancelado” sem você cancelar: quase sempre é SHA faltando ou ainda propagando.

---

## Passo 7 — Secrets no GitHub (orbit-lab)

O Actions precisa da keystore em forma de texto (Base64) + senhas.

### 7.1 — Gerar o Base64 e copiar pro clipboard

Ainda no PowerShell, pasta OrbitLab:

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes("$PWD\orbit-lab-release.keystore")
) | Set-Clipboard

Write-Host "Base64 copiado pro clipboard. Nao cole em chat publico."
```

É um texto **longo**. Não cole no Discord/chat aberto. Só no secret do GitHub.

### 7.2 — Criar os quatro secrets

1. Abra: https://github.com/NowardEthan/orbit-lab/settings/secrets/actions  
   (precisa ser dono/admin do repo)
2. **New repository secret** — crie um por um:

| Nome do secret | Valor |
|----------------|--------|
| `LAB_KEYSTORE_BASE64` | Cole o Base64 do clipboard (tudo, uma linha) |
| `LAB_KEYSTORE_PASSWORD` | Senha do keystore (Passo 3) |
| `LAB_KEY_ALIAS` | `orbitlab` (igual ao `-alias` do comando) |
| `LAB_KEY_PASSWORD` | Senha da chave (Passo 3) |

Confira se `ORBIT_RELEASES_TOKEN` **já existe** (serve pra publicar no `orbit-releases`).  
Se não existir, veja `TESTE-UPDATE.md`.

---

## Passo 8 — Backup (não pule)

Se perder a keystore, o Android trata o próximo APK como **outro app**. Updates morrem.

Guarde **fora** do PC do dia a dia:

- [ ] Cópia de `orbit-lab-release.keystore` (drive criptografado / pendrive seguro / cofre)
- [ ] Senha do keystore
- [ ] Senha da chave
- [ ] Alias (`orbitlab`)
- [ ] SHA-1 e SHA-256 (opcional, dá pra regenerar com o keytool)

---

## Passo 9 — Me avisar

Quando 1–8 estiverem feitos, responda no chat:

> **secrets prontos**

Aí eu (ou você) rodamos o Actions **Build & Release Lab** e conferimos:

1. Job “Decodificar keystore” verde  
2. Job “Verificar assinatura” verde (não pode ser Android Debug)  
3. Você instala o APK no celular e testa **Google login**

---

## Passo 10 — Migração nos celulares (primeira vez)

Aparelhos que já têm o Lab **antigo** (assinado com debug):

1. Avisar o grupo: “vou publicar uma build com assinatura nova”  
2. Em cada celular: **desinstalar** o Orbit Lab  
3. Instalar o APK novo (do release `lab` ou do artefato do Actions)  
4. Login de novo  
5. A partir daí, o banner de update na Início volta a funcionar normalmente

Sem desinstalar, o Android recusa a atualização (“conflito de assinatura”).

---

## Se algo der errado

| Sintoma | O que checar |
|---------|----------------|
| `keytool` não encontrado | Passo 2 — caminho do Android Studio JBR |
| Actions falha “Falta o secret LAB_*” | Passo 7 — nomes **exatos** dos secrets |
| Actions falha “APK ainda assinado com debug” | Keystore/senha errados; ou Gradle não leu `keystore.properties` no CI |
| Google login “cancelado” | Passo 6 — SHA release no Firebase; esperar 5–10 min |
| Update não instala | Passo 10 — desinstalar o APK debug antigo uma vez |
| Perdi a keystore | Sem backup = precisa de app “novo” (outro package ou wipe geral). Evite. |

---

## Referência rápida (depois que já está feito)

| Item | Onde |
|------|------|
| Guia (este arquivo) | `SIGNING.md` |
| Plano geral | `HARDENING.md` |
| Publicar APK | `TESTE-UPDATE.md` / `RELEASING.md` |
| Wiring Gradle | `app/build.gradle.kts` |
| CI | `.github/workflows/build-lab.yml` |
| Exemplo de props locais | `keystore.properties.example` |
| Package do app | `com.ethan.orbitlab` |
| Alias padrão | `orbitlab` |
| Arquivo da chave | `orbit-lab-release.keystore` (local, gitignored) |

---

## Depois que terminar

Volte ao chat e diga **secrets prontos**. Seguimos com o teste do Actions e a primeira publish com a chave de release.
