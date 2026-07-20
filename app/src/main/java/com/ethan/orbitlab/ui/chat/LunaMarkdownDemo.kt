package com.ethan.orbitlab.ui.chat

/**
 * Demos de Markdown da Luna — conteúdo editorial pra validar o renderer e o stream.
 * Copy em pt-BR.
 */

/** Resposta rica de revisão — usada no mock e como base do stream. */
fun demoMarkdownIaGenerativa(tema: String = "IA generativa"): String = """
Boa — vamos montar um mapa mental de **$tema**, sem enrolação.

## O que é, de verdade
Modelos generativos não “entendem” o mundo como a gente. Eles aprendem *estatística de linguagem* (ou de pixels, código, etc.) e, na hora de responder, **amostram** o próximo pedaço mais plausível dado o contexto.

Três ideias pra grudar:

1. **Padrão** — o treino comprime regularidades dos dados
2. **Amostragem** — gerar = escolher o próximo token com alguma aleatoriedade
3. **Controle** — temperatura, system prompt e exemplos guiam o tom

---

## Analogia útil
Pensa numa orquestra improvisando: cada músico (token) olha o que já tocou e decide a próxima nota. Não há partitura mágica — há *hábito musical* treinado em milhares de shows.

> Rede neural ≠ mágica. É voto em cadeia com boa memória.

## Como estudar sem se perder
- Comece pelo *efeito* (o que sai), depois abra a *mecânica*
- Separe **capacidade** (o modelo consegue?) de **alinhamento** (responde do jeito certo?)
- Teste com prompts curtos e compare *temperatura* baixa vs alta

### Mini-exercício
Reescreva este pedido em duas versões — uma vaga e uma precisa:

```prompt
Explique neurônios.
```

Versão precisa (exemplo):

```prompt
Explique um neurônio artificial como se eu tivesse 12 anos.
Use a analogia de um voto. No máximo 5 frases.
```

## Fórmula mental (quando quiser o formal)

```kotlin
// ideia, não código de produção
val saida = ativacao(soma(entrada * peso) + vies)
```

Cada `peso` é “quanto confio neste sinal?”. A *ativação* decide se o voto passa adiante.

---

## Próximo passo
Quer que a gente faça um **quiz rápido** (3 perguntas) ou um exemplo com *prompt engineering* no seu tema?
""".trimIndent()

fun demoReasoningIaGenerativa(tema: String = "IA generativa"): String = """
Pedido: revisão de “$tema”.
Estrutura: definição curta → 3 ideias → analogia → estudo → exercício → formal leve → CTA.
Manter tom de professora Luna: claro, sem jargão no começo.
""".trimIndent()

/** Demo alternativa — redes neurais (mock da trilha). */
fun demoMarkdownNeuronio(): String = """
Beleza — vamos do zero, sem jargão primeiro.

## Neurônio em uma frase
É uma unidade que **recebe sinais**, mistura com *pesos* e decide se passa algo adiante.

## Em passos
1. Chegam entradas (números / sinais)
2. Cada entrada tem um **peso** (“quanto importa?”)
3. Soma tudo (+ um viés, às vezes)
4. Uma função decide: passa ou não / com que intensidade

- Entrada fraca + peso alto ainda pode influenciar
- Entrada forte + peso perto de zero quase some

> Cada conexão é um “quanto confio neste sinal?”.

---

## Do informal ao formal

```kotlin
val saida = f(soma(entrada * peso) + vies)
```

Aqui `f` é a *ativação* — o “juiz” do voto.

### Por que isso importa na Lumen
Quando a gente fala em *aprender*, na prática estamos **ajustando pesos** pra o erro cair. A magia está na escala: milhões desses votos em camadas.

Quer um exemplo **numérico** (com 2 entradas) ou prefere analogia visual da trilha?
""".trimIndent()
