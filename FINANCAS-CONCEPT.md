# Finanças — o exocórtex da grana (OrbitLab)

> Spec pro antigravity executar **no OrbitLab** (Android nativo, **Kotlin + Jetpack Compose**,
> pacote `com.ethan.orbitlab`). Um **módulo de finanças pessoais dentro do Orbit**, pra uma mente
> neurodivergente, **assistido pela Luna**, na linguagem visual do redesign 1.0 (grafite + azul
> pastel). A Luna faz o trabalho chato (registra, categoriza, avisa) e o app devolve previsibilidade
> + gamificação. Planejado com o Ethan em **2026-08-01**.
> **Leia o [AGENTS.md](AGENTS.md) deste repo antes de mexer.**

**Conceito visual (10 telas, mockup navegável):** https://claude.ai/code/artifact/f6dc9b18-a7c0-4eb7-aa27-aa917d59a17a
> ⚠️ O mockup foi feito em HTML só pra você *ver* as telas — o alvo real é **Compose**, não web.
> Use-o como referência de layout/hierarquia, não de código.

> **Nota de stack (importante):** este módulo é pro **OrbitLab (Compose)**, o app-laboratório onde já
> vivem o redesign 1.0, os Artefatos e as Novidades — **NÃO** pro `orbit-mobile` (React Native/Expo).
> Se você viu uma versão anterior desta spec em `orbit-mobile/` com tipos `.ts`, ela estava no repo
> errado; esta aqui é a boa.

---

## 1. O porquê

O Ethan é autista + TDAH. Dinheiro, pra muita mente neurodivergente, é **ansiedade por falta de
previsibilidade**: "quanto posso gastar sem furar?", "o que é fixo e o que é meu?", "de onde saiu
isso?". Apps de finanças tradicionais empurram **planilha e disciplina** — exatamente o que trava
quem é neurodivergente.

A aposta do Orbit é outra: **a Luna faz o chato** (registra, categoriza, lembra, avisa) e o app
devolve **previsibilidade** ("isso é fixo, o resto é seu") + **gamificação** (ficar no orçamento vira
luz e ofensiva, igual à rotina). Finanças não é app novo — é **mais um domínio alimentando o mesmo
motor**.

Referência de organização visual: um dashboard estilo LMS (cards de resumo, anéis, timeline, gráfico,
conquistas) — **estrutura** dessa; **pele** do Orbit (grafite + azul pastel, "largou o violeta").

---

## 2. Decisões cravadas (não re-derivar)

- **Alvo = OrbitLab (Compose/Kotlin).** Não é o orbit-mobile RN. Entra na **gaveta lateral** já
  existente ([`shell/OrbitShell.kt`](app/src/main/java/com/ethan/orbitlab/shell/OrbitShell.kt) →
  `OrbitDrawer`), como mais uma aba.
- **Pele grafite + azul pastel** via os tokens do app — **base cinza, cor como acento pontual**
  (ver §4). Nada de violeta. Semânticos separados do accent: verde = entrada/ok, vermelho =
  saída/alerta, âmbar = variável/atenção.
- **Captura automática = por NOTIFICAÇÃO do banco** (não Pluggy/Open Finance agora). O app lê os
  avisos de compra com um `NotificationListenerService` **no aparelho**, a Luna sugere o lançamento,
  você confirma. Como o OrbitLab é **nativo puro**, isso é um serviço Kotlin direto — **sem ponte
  React Native**. Pluggy/Open Finance fica como **DLC futura** (§10). Ver §6.
- **Registro manual + "fala com a Luna" são a BASE.** A captura é acelerador em cima, não a fundação.
  Tudo funciona sem captura.
- **Transferência entre carteiras NÃO é gasto nem entrada.** É dinheiro seu mudando de lugar; só
  ajusta o saldo dos dois lados. Nunca entra no cálculo de gasto/receita.
- **Dinheiro é SEMPRE centavos inteiros** (`Long`), nunca `Double`/`Float`. Formata na exibição.
- **Gamificação reusa a linguagem luz/lua/conquistas** do app (§8) — mas atenção: hoje ela é mais
  **visual/demo** no OrbitLab do que motor de verdade. Ler §8 antes de prometer "reuso".
- **Dado bancário é o mais sensível que existe** → local-first, consentimento explícito, entra no
  roadmap de segurança/privacidade do ecossistema (o do `orbit-mobile`,
  `ROADMAP-SEGURANCA-PRIVACIDADE-LEGAL.md`). Ver §6 e §10.

---

## 3. As 10 telas do conceito

Grafite, com a gaveta lateral (☰) no topo. A Luna aparece como **card de insight** no topo das telas
+ ações dela na timeline do chat (não é um chat de tela cheia dominando a tela de finanças).

1. **Início (dashboard do mês)** — Luna lendo os números; anel "gasto do mês" (X% da meta) + selo
   "dentro da meta" + ofensiva (🔥 N dias no orçamento); 4 KPIs (entrou/saiu/contas a pagar/reserva);
   timeline "Hoje" (lançamentos do dia, conta vencendo em vermelho); gráfico do mês; metas com barra.
2. **Constância (a grana virando luz)** — fase da lua + streak "N dias no orçamento" + ✨ luz total;
   luz da semana (barrinhas); grade de **conquistas** (acesas × bloqueadas). Mesma linguagem da rotina.
3. **Gaveta lateral** — perfil no topo (avatar + ✨ luz + 🔥 dias) e a nav: Início, Finanças, Entradas
   e saídas, Cartões, Transferência, Recorrentes, Metas & luz, Conversas, Caixa de Ideias, Novidades,
   Ajustes.
4. **Recorrentes** — resumo "todo mês" (entram fixos / saem fixos / **sobra livre**) + barra empilhada
   comprometido×livre; lista "Entram todo mês" e "Saem todo mês" com **etiqueta do dia**.
5. **Entradas e saídas (extrato)** — filtro Dia/Semana/Mês; faixa de saldo (entrou/saiu/saldo);
   lançamentos **agrupados por dia** com subtotal; FAB "＋ Registrar".
6. **Registrar** — folha de baixo (bottom sheet, fundo **sólido** — nada de Glass); alternador
   **Saída/Entrada**; valor grande; chips de categoria; campos Data / **Carteira** / **Repetir todo
   mês** (cria recorrente) / Nota; "Salvar".
7. **Captura automática (ativar + vigiar)** — explicação + selo "tudo no aparelho"; cartão de status
   "Escutando · última captura há X"; requisitos (acesso a notificações ✓, bateria sem restrição ✓);
   **bancos que o Orbit escuta** com saúde por banco (Nubank ✓, Inter ✓, **Itaú ⚠️ "nenhum aviso há 8
   dias" → Como ligar**). É a tela que **percebe quando a captura cai** e chama pra reativar.
8. **A Luna captura (o momento)** — chega o aviso ("Nubank · Compra aprovada · R$ 32 em IFOOD"); a
   Luna sugere no chat: "Vi uma compra de R$ 32 no iFood. Registro como saída de hoje?" com **palpite
   de categoria** e botões **Registrar / Editar / Ignorar**. Renderiza como um card na timeline (ver §7).
9. **Cartões** — Luna com insight ("78% no crédito do Nubank, fatura fecha em 2 dias"); resumo (faturas
   abertas / limite livre); **cartão principal** desenhado como cartão real + **fatura atual / limite**
   com barra + **fecha/vence**; lista "Todos" separando **por banco e por tipo** (crédito/débito) +
   dinheiro; "+ Adicionar cartão ou conta".
10. **Transferência** — valor grande; **De → (swap) → Para**; chips de motivo (Pagar fatura / Reserva /
    Ajuste); nota clara "não conta como gasto"; "Transferir".

---

## 4. Pele visual (usar os tokens do OrbitLab)

**Não crave cor na mão.** Use o sistema que já existe (regra do [AGENTS.md](AGENTS.md) §Visual):

- Cores base: [`OrbitTokens`](app/src/main/java/com/ethan/orbitlab/ui/theme/OrbitTokens.kt) —
  `graphiteBg`, `textHiN`, superfícies, etc. **A impressão dominante é dark/cinza contido.**
- Acentos: [`OrbitFills`](app/src/main/java/com/ethan/orbitlab/ui/theme/OrbitFills.kt) — **só pontuais**
  (cards Luz/Lua, chips de estado, anel do avatar, conquistas). **Nunca** encher lista/stat/chrome de
  cor. Princípio **contraste, não opacidade** — aceso ACENDE no degradê (`brush` + `onFill`), não é
  `cor.copy(alpha=.15)` com texto na mesma cor.
- Espaçamento/raios: [`OrbitMetrics`](app/src/main/java/com/ethan/orbitlab/ui/theme/OrbitMetrics.kt).
  Tipografia: [`OrbitType`](app/src/main/java/com/ethan/orbitlab/ui/theme/OrbitType.kt).
  Motion: [`OrbitMotion`](app/src/main/java/com/ethan/orbitlab/ui/theme/OrbitMotion.kt) (`orbitEnter`,
  `orbitPressable`) — curto e intencional, sem barulho.

Semânticos (verde/vermelho/âmbar) são **separados do accent** — adicione como tokens próprios se ainda
não existirem, não reaproveite o azul pra "positivo". Números com fonte tabular.

---

## 5. Modelo de dados (Firestore via repositório Kotlin)

Espelhar o padrão de [`data/firebase/FirestoreDocumentos.kt`](app/src/main/java/com/ethan/orbitlab/data/firebase/FirestoreDocumentos.kt):
um `object FirestoreFinancas` (ou repos separados) com `db.collection("users").document(uid).collection(...)`,
`subscribe*` (listener em tempo real → `callbackFlow`), e `suspend fun` pras escritas (`atualizar`,
`apagar`, `renomear`, `duplicar` já são o molde ali). **Sempre gravar o campo `id` no doc.**
**Valores em centavos (`Long`).**

```kotlin
// users/{uid}/carteiras/{carteiraId}
data class Carteira(
    val id: String,
    val tipo: String,            // "conta_debito" | "cartao_credito" | "dinheiro"
    val banco: String? = null,   // "Nubank", "Inter"...
    val apelido: String,         // "Nubank Crédito"
    val cor: String,             // chave de gradiente/base pro cartão visual
    val ultimos4: String? = null,
    val limiteCentavos: Long? = null,      // só cartao_credito
    val fechamentoDia: Int? = null,        // 3
    val vencimentoDia: Int? = null,        // 10
    val saldoInicialCentavos: Long = 0,
    val arquivada: Boolean = false,
)

// users/{uid}/lancamentos/{lancamentoId}
data class Lancamento(
    val id: String,
    val tipo: String,            // "entrada" | "saida"
    val valorCentavos: Long,     // sempre positivo; o tipo diz o sinal
    val data: Long,              // epoch millis (ou Timestamp do Firestore)
    val descricao: String,
    val categoria: String,       // id de categoria (enum + custom)
    val carteiraId: String,
    val recorrenteId: String? = null,
    val origem: String,          // "manual" | "luna" | "captura"
    val capturaRaw: String? = null,  // texto do aviso do banco (auditoria)
    val pago: Boolean = true,        // false = conta pendente
)

// users/{uid}/recorrentes/{recorrenteId}
data class Recorrente(
    val id: String,
    val tipo: String,            // "entrada" | "saida"
    val valorCentavos: Long,
    val diaDoMes: Int,
    val categoria: String,
    val carteiraId: String,
    val apelido: String,         // "Aluguel"
    val variavel: Boolean = false,   // energia ~ (valor é estimativa)
    val ativo: Boolean = true,
)

// users/{uid}/transferencias/{transferenciaId}  — NÃO é gasto nem entrada
data class Transferencia(
    val id: String,
    val deCarteiraId: String,
    val paraCarteiraId: String,
    val valorCentavos: Long,
    val data: Long,
    val motivo: String? = null,  // "pagar_fatura" | "reserva" | "ajuste"
)

// users/{uid}/metasFinanceiras/{metaId}
data class MetaFinanceira(
    val id: String,
    val apelido: String,         // "Reserva de emergência"
    val tipo: String,            // "reserva" | "corte"
    val alvoCentavos: Long,
    val atualCentavos: Long,
)
```

**Saldo de uma carteira** = `saldoInicial` + entradas − saídas ± transferências. **Gasto do mês** =
soma das `saida` no período (transferências NÃO entram). **Fatura de um crédito** = soma das `saida`
naquele cartão no ciclo (fechamento a fechamento), abatida por transferências com `motivo =
"pagar_fatura"`.

Categorias: um enum base (Alimentação, Transporte, Moradia, Saúde, Lazer, Contas, Renda...) cada uma
com cor + emoji, + custom do usuário.

**Regras Firestore:** replicar o padrão dono-só das outras coleções `users/{uid}/…` (read/write/delete
só do dono). O servidor `luna-core` escreve com `firebase-admin` (ignora regras); o app lê/escreve com
as regras.

---

## 6. Captura automática por notificação (a parte pesada)

No OrbitLab é **mais simples** que no orbit-mobile: app nativo puro → um serviço Kotlin direto, **sem
bridge RN**. (No orbit-mobile existe o módulo de alarme
`android/app/src/main/java/com/luna/orbitmobile/alarme/` — serve de **prior-art do ecossistema** pra
como lidar com foreground/permissão/bateria; mas aqui o serviço é do próprio OrbitLab, que ainda não
tem nenhum serviço nativo — este seria o primeiro.)

**Arquitetura:**
1. **`CapturaNotificacaoService`** (`data/captura/`) — estende `NotificationListenerService`. No
   `onNotificationPosted`, filtra pelos pacotes dos bancos, extrai `title`/`text` (`extras`), roda o
   **parser por banco** e joga `{ banco, valorCentavos, descricao, quando, raw }` num
   repositório/`Flow` que a UI observa.
2. **Permissão:** `BIND_NOTIFICATION_LISTENER_SERVICE` no Manifest + o usuário precisa **conceder na
   tela do sistema** (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`). A tela **Captura automática**
   (§3, tela 7) guia isso e checa se o listener está ativo.
3. **Parser por banco = DADOS, não código hard.** Uma tabela de regras (regex por pacote/banco) que dá
   pra atualizar sem recompilar (ideal: baixar do servidor / do manifesto de updates). Cada banco
   escreve o aviso do seu jeito e **muda de vez em quando** — por isso regra versionada + fallback
   manual.
4. **Fluxo:** banco avisa → serviço parseia no aparelho → emite pro app → a Luna monta a **sugestão**
   (tela 8) com palpite de categoria (por descrição/histórico) e carteira (aviso do Nubank → carteira
   do Nubank). Renderiza como **card na timeline do chat** — mesmo padrão do
   [`ui/chat/DocumentoCard.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/DocumentoCard.kt) +
   [`LunaActionModels.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/LunaActionModels.kt). Usuário
   confirma → cria `Lancamento` (origem `"captura"`).
5. **Guarda de saúde (o "garantir isso" que o Ethan pediu):** guardar `ultimoAvisoPorBanco` (em
   `PrefsRepository` ou Firestore). Se um banco fica quieto N dias enquanto outros recebem → **alerta
   na tela 7**. Detectar também **permissão revogada** e **notificação do banco desligada**.
6. **Bateria:** pedir ignorar otimização de bateria (senão o Android mata o listener em 2º plano).

**Limites honestos:** só pega o que **gera notificação** (compra no cartão sim; boleto/alguns Pix não).
O manual + a Luna no chat ("gastei 32 no almoço") cobrem o resto.

**Privacidade/LGPD (inegociável):** processamento **no aparelho**; nada sai daqui a não ser o lançamento
que o usuário confirma (vira dado normal no Firestore). Consentimento explícito na ativação. **Entra no
roadmap de segurança** (o do orbit-mobile, `ROADMAP-SEGURANCA-PRIVACIDADE-LEGAL.md`) — é o dado mais
sensível do app. "Roda no aparelho" é a escolha técnica mais simples E o maior argumento de confiança.

---

## 7. A Luna nas finanças

**Ferramentas: server-side, no `luna-core`** (independente de o app ser Compose ou RN). Criar as tools
lá (mãos da Luna) + registrar no chat:

- `registrar_lancamento(tipo, valor, categoria?, carteira?, data?, recorrente?)` — "gastei 32 no almoço".
- `listar_lancamentos(periodo?, carteira?, categoria?)`.
- `resumo_financeiro(periodo?)` — entrou/saiu/saldo/por categoria/comparativo.
- `gerir_recorrente(...)`, `gerir_carteira(...)`, `transferir(de, para, valor, motivo?)`.

**No app (OrbitLab), as ações da Luna renderizam** via
[`LunaActionModels.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/LunaActionModels.kt) +
[`LunaActionTimeline.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/LunaActionTimeline.kt) (os badges
de ferramenta) — igual aos Artefatos. Adicionar as chaves de `toolMeta` das novas ferramentas (ex.:
`registrar_lancamento` → "Lançamento registrado"), senão o badge não aparece.

**Insights (o card que fala):** a Luna lê os números e comenta no topo das telas — "gastou R$ 860 a
menos que setembro", "fixos comem 34% do que entra", "fatura fecha em 2 dias". Gerados a partir do
`resumo_financeiro`, não escritos à mão.

> A Luna no OrbitLab é a **tela de chat** ([`ui/chat/ChatScreen.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/ChatScreen.kt)),
> não um FAB flutuante (isso é do orbit-mobile). Nas telas de finanças ela entra como **card de
> insight** + as sugestões/ações caem na timeline do chat.

---

## 8. Gamificação (⚠️ ler antes de prometer "reuso")

A linguagem existe **visualmente** no OrbitLab (o AGENTS.md fala em "cards Luz / Lua (Início)" e
"conquistas desbloqueadas"), mas **o motor de verdade (pontos de luz, ofensiva, fase da lua ligados a
eventos) NÃO está implementado aqui** — a rotina é um MVP local (stub), e a espinha de luz/constância
madura vive no **orbit-mobile (RN)**. Então, no OrbitLab:

- **Não dá pra "só plugar"** numa engine de luz que não existe. Duas opções: (a) construir uma engine
  de luz enxuta no OrbitLab (fonte `"financas"`), reusando a **linguagem visual** (OrbitFills, cards
  Luz/Lua, conquistas); ou (b) tratar a gamificação como capítulo próprio (F4), depois do básico.
- **Eventos que dão luz:** ficar no orçamento do dia, pagar recorrente em dia, registrar em dia.
- **Ofensiva / fase da lua:** "N dias no orçamento" move a constância/lua, mesma metáfora da rotina.
- **Conquistas:** grade de badges (mês no azul, 10 contas em dia, reserva começada, ofensiva 7/30 dias).

⚠️ Inspiração de loop é Solo Leveling, **mas vestido de Luna** (lua/luz/fases) — **não deixar o SL
explícito**. O desenho maior (rank/perda/recolhimento) é horizonte guardado, freado de propósito.

---

## 9. Roadmap em capítulos

Manual primeiro, captura depois (mais difícil e arriscada). Cada capítulo é um **release lab coerente**
(critério de batelar — não 1 release por micro-mudança). Ver §11 pra publicar.

- **F0 — Fundação + Carteiras.** Repos Firestore (§5) + regras + CRUD de carteiras (tela **Cartões**,
  sem fatura ainda) + entrada na gaveta (`OrbitDrawer`). Saldo derivado. Sem Luna, sem captura.
- **F1 — Registrar + Extrato.** Tela **Registrar** (entrada/saída, categoria, carteira) + tela
  **Entradas e saídas** (agrupado por dia/mês, saldo do período). Base de tudo.
- **F2 — Recorrentes.** Repo + tela + **geração automática** dos fixos no dia + aviso de vencimento.
  O toggle "Repetir todo mês" do Registrar cria um recorrente.
- **F3 — Início (dashboard) + Luna insights.** Anel gasto/meta, KPIs, timeline hoje, gráfico. Tools no
  `luna-core` (`registrar_lancamento`, `resumo_financeiro`) + card de insight.
- **F4 — Gamificação.** Engine de luz enxuta (§8) + tela **Constância** + conquistas, na linguagem
  visual existente.
- **F5 — Captura automática (nativo).** `NotificationListenerService` + onboarding (tela 7) + parser
  por banco + guarda de saúde + Luna sugere (tela 8). **Depende de F1.** Fase mais pesada e sensível
  (LGPD).
- **F6 — Transferência + fatura de crédito a fundo.** Ciclo fechamento/vencimento, "pagar fatura" que
  abate, tela **Transferência**.
- **F7 (horizonte).** Relatório por categoria; **Pluggy/Open Finance** como DLC; metas a fundo.

F0→F3 já é um app de finanças útil sem nada de nativo. F4 e F5 podem trocar de ordem (F4 dá recompensa
cedo; F5 dá o "mágico", mas é caro e sensível).

---

## 10. Riscos honestos (não varrer pra baixo do tapete)

- **Parser por banco frágil** — texto do aviso muda → regras versionadas (dado, não código) + tela de
  saúde (tela 7) + fallback manual sempre presente.
- **Bateria mata o serviço** — pedir whitelist de otimização.
- **Float em dinheiro = bug** — sempre centavos `Long`.
- **Gamificação não existe pronta aqui** (§8) — não prometer "reuso" de uma engine que é do RN.
- **LGPD / dado bancário** — local-first, consentimento, item no roadmap de segurança. Gente real já
  usa o Orbit; não pode escorregar aqui.
- **Captura não pega tudo** (boleto/alguns Pix sem notificação) — manual + Luna cobrem.
- **Escopo** — 10 telas é o conceito completo; **não construir tudo de uma vez**.

---

## 11. Publicar a release (fluxo LAB — sem repetir o loop do auto-update)

> ⚠️ O [`RELEASING.md`](RELEASING.md) deste repo está **DESATUALIZADO** (manda `--beta`/`--stable`,
> tag `beta`, `updates-beta.json` — nada disso existe mais). O fluxo real é o **lab**, abaixo.

**A analogia:** o app se auto-atualiza comparando um número, o `versionCode` — é o **número da página
do livro**. O app instalado olha a página que tem e a que o servidor anuncia; se a do servidor for
**maior**, ele oferece atualizar. **O erro de antes:** montar o APK **sem as flags de versão** faz o
flavor `lab` cair no **fallback do `build.gradle` (0.5.0 / código 4)**. Aí o app se acha 0.5.x, o
manifesto pede um código alto, ele baixa o `orbit.apk`… que É o 0.5.x → **atualiza pra sempre em
círculo**. E quem já estava num código alto leva **erro ao instalar** (`INSTALL_FAILED_VERSION_DOWNGRADE`
— o Android recusa página menor por cima de maior).

### As regras que evitam o susto

1. **Montar `assembleLabRelease` COM as duas flags de versão** (a armadilha é esquecê-las OU montar
   `assembleLabDebug` por engano — o asset sai gordo e/ou com versão de fallback):
   ```bash
   ./gradlew assembleLabRelease "-PlabVersionCode=N" "-PlabVersionName=X.Y.Z" --no-daemon
   ```
   **As aspas importam no PowerShell** — `-PlabVersionName=0.9.8` sem aspas quebra (o `.9.8` vira task).
   O `versionCode` **só sobe**, nunca repete nem desce.
2. **Conferir a versão do APK montado** por `app/build/outputs/apk/lab/release/output-metadata.json`
   (tem `versionCode`/`versionName` em texto). Se aparecer **0.5.x** = montou errado (fallback) — refaz.
3. Copiar `app/build/outputs/apk/lab/release/app-lab-release.apk` → `dist/orbit.apk` (asset **sempre**
   `orbit.apk`).

### Publicar

4. `gh release create lab-X.Y.Z ./dist/orbit.apk -R NowardEthan/orbit-releases --title "Orbit Lab X.Y.Z — <gancho>" --notes "..."` (tag por-versão `lab-X.Y.Z`).
5. Editar `orbit-releases/updates-lab.json` (`"app":"orbit-lab"`): `latestVersion`, `latestVersionCode`,
   `publishedAt`, `apkUrl` (=`.../releases/download/lab-X.Y.Z/orbit.apk`) e **prepend** uma news
   `{id,date,version,tag,title,body}`. Commit + push na `main` do submódulo `orbit-releases`.

O app lê `updates-lab.json` e oferece update quando `latestVersionCode > versionCode instalado`
(**o versionCode manda, não o nome**). Todo lab é assinado com a **mesma debug key** → instala por cima
sem desinstalar. Verifique o manifesto pelo **raw** (`raw.githubusercontent.com/...`), não pela API do
GitHub (rate-limit 60/h; celular e PC dividem o IP no Wi-Fi).

### Cadência (regra do Ethan)

Release tem **critério**: batele num **capítulo coerente** (ex.: "F0+F1 — registrar e ver a grana"),
**não** 1 release por micro-mudança. Commits podem ser frequentes; RELEASE (APK + bump + mural) é
batelado.

> O app OrbitLab costuma ficar **no working tree, não commitado** (workflow do Ethan) — não commite o
> app sem ele pedir. Docs/conceitos como este podem ficar no repo normalmente.

---

## 12. O que JÁ existe pra reusar (OrbitLab)

- **Padrão de repositório Firestore:** [`data/firebase/FirestoreDocumentos.kt`](app/src/main/java/com/ethan/orbitlab/data/firebase/FirestoreDocumentos.kt)
  (`object` + `collection("users/{uid}/…")` + `subscribe*` em `callbackFlow` + `suspend` pras escritas;
  `apagar`/`renomear`/`duplicar` prontos como molde) e
  [`FirestoreChat.kt`](app/src/main/java/com/ethan/orbitlab/data/firebase/FirestoreChat.kt).
- **Card na timeline + badges de ferramenta:** [`ui/chat/DocumentoCard.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/DocumentoCard.kt),
  [`LunaActionModels.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/LunaActionModels.kt),
  [`LunaActionTimeline.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/LunaActionTimeline.kt) — molde
  pro card de sugestão da captura e pros badges das ferramentas de finanças.
- **Exportar (se quiser exportar extrato/relatório):** [`data/export/ConversaExporter.kt`](app/src/main/java/com/ethan/orbitlab/data/export/ConversaExporter.kt)
  + [`ui/chat/ExportSheet.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/ExportSheet.kt) /
  [`ArtefatoExportSheet.kt`](app/src/main/java/com/ethan/orbitlab/ui/chat/ArtefatoExportSheet.kt).
- **Navegação / gaveta:** [`shell/OrbitShell.kt`](app/src/main/java/com/ethan/orbitlab/shell/OrbitShell.kt)
  (`ModalNavigationDrawer` + `OrbitDrawer`) — adicionar a aba Finanças aqui.
- **Tema:** `ui/theme/OrbitTokens.kt`, `OrbitFills.kt`, `OrbitMetrics.kt`, `OrbitType.kt`, `OrbitMotion.kt`.
- **Prefs locais:** [`data/PrefsRepository.kt`](app/src/main/java/com/ethan/orbitlab/data/PrefsRepository.kt)
  (bom pra `ultimoAvisoPorBanco` da guarda de saúde).
- **Ferramentas da Luna:** server-side no `luna-core` (mãos da Luna + registro no chat) — o app só as consome.
- **Prior-art nativo (foreground/bateria):** o módulo de alarme do orbit-mobile
  (`android/app/src/main/java/com/luna/orbitmobile/alarme/`) — referência de como lidaram com serviço em
  primeiro plano e whitelist de bateria.
- **Conceito visual:** https://claude.ai/code/artifact/f6dc9b18-a7c0-4eb7-aa27-aa917d59a17a
