package com.ethan.orbitlab.legal

/**
 * Documentos legais — alinhados ao orbit-mobile (`src/legal/documents.ts`).
 * No lab não há aceite versionado nem Firebase; o texto é o mesmo trato.
 */
object LegalDocuments {
    const val LEGAL_VERSION = 1
    const val LEGAL_ATUALIZADO_EM = "16 de julho de 2026"
    const val IDADE_MINIMA = 16
    const val CONTROLADOR = "Ethan André"
    const val CONTATO_PRIVACIDADE = "Lunacorecreative@gmail.com"

    val politicaDePrivacidade: String = """
## Política de Privacidade — Orbit

_Última atualização: ${LEGAL_ATUALIZADO_EM}_

O trato é simples: seus dados são seus. Aqui está, sem juridiquês, o que o Orbit coleta, por que, para onde vai e o que você pode fazer a respeito.

Responsável pelos seus dados (o "controlador"): **$CONTROLADOR**. Contato: **$CONTATO_PRIVACIDADE**.

## O que coletamos

- **Sua conta.** Conta Aura (email e senha) ou Google: nome, e-mail e foto que o provedor informa.
- **Suas conversas com a Luna.** O texto das mensagens que você troca com ela.
- **Sua rotina.** Blocos, tarefas, horários, lembretes e o seu progresso (a "luz").
- **O que a Luna aprende sobre você.** Fatos e preferências que ela guarda para lembrar de você entre conversas.
- **Áudio de voz.** Quando você grava um recado, o áudio é transcrito em texto; usamos a transcrição para a Luna responder.
- **Dados técnicos.** Versão do app e informações necessárias para enviar notificações.

Guardamos só o que serve a você. **Não vendemos seus dados** e não os usamos para anúncios.

## Por que usamos (finalidade e base legal)

Usamos seus dados para o Orbit funcionar: a Luna responder, lembrar de você, organizar sua rotina e te avisar dos lembretes. As bases legais (LGPD, art. 7º) são o **seu consentimento** e a **execução do serviço** que você pediu ao usar o app.

## Com quem compartilhamos

Para o app existir, alguns parceiros processam dados por nós:

- **Provedores de Inteligência Artificial.** Para a Luna pensar e responder, o texto das suas mensagens é enviado a empresas de IA que rodam os modelos. Elas processam a mensagem para gerar a resposta.
- **Google / Firebase.** Guarda sua conta e seus dados na nuvem e cuida do login.

Esses parceiros podem processar dados **fora do Brasil** (por exemplo, nos Estados Unidos). Ao usar o Orbit, você concorda com essa transferência, feita para prestar o serviço.

## Por quanto tempo guardamos

Enquanto você tiver conta. Quando você apaga sua conta (dentro do app, em Ajustes → Privacidade e dados), removemos suas conversas, rotina, memória e login. Cópias de segurança e registros técnicos podem levar um tempo curto para expirar.

## Seus direitos

A LGPD (art. 18) garante que você possa: **saber** o que temos, **corrigir**, **exportar**, **apagar** e **revogar o consentimento**. No app você já **apaga tudo num toque**. Para qualquer outro pedido, escreva para **$CONTATO_PRIVACIDADE** e respondemos.

## Segurança

Protegemos seus dados com login, regras de acesso por dono (só você acessa o que é seu) e conexões criptografadas (HTTPS). Nenhum sistema é 100% infalível, mas levamos isso a sério.

## Idade mínima

O Orbit é para pessoas de **${IDADE_MINIMA} anos ou mais**. Se você tem entre ${IDADE_MINIMA} e 18, use com o conhecimento de um responsável.

## A Luna não é terapeuta

O Orbit é uma companhia — não substitui profissional de saúde nem dá conselho médico. Em um momento de crise, procure ajuda de verdade. No Brasil, o **CVV atende de graça, 24h, no 188** (ou em cvv.org.br).

## Mudanças nesta política

Se algo importante mudar, atualizamos este texto e pedimos seu aceite de novo quando fizer sentido.

## Falar com a gente

Dúvidas ou pedidos sobre seus dados: **$CONTATO_PRIVACIDADE**.
""".trimIndent()

    val termosDeUso: String = """
## Termos de Uso — Orbit

_Última atualização: ${LEGAL_ATUALIZADO_EM}_

Bem-vindo ao Orbit. Ao usar o app, você concorda com estes termos. Escrevemos curto e direto.

## O que é o Orbit

O Orbit é um app com a **Luna**, uma companhia de inteligência artificial que conversa, acolhe e ajuda a organizar sua rotina. É software, não uma pessoa — e não é profissional de saúde.

## Idade mínima

Você precisa ter **${IDADE_MINIMA} anos ou mais** para usar o Orbit. Entre ${IDADE_MINIMA} e 18, use com o conhecimento de um responsável.

## A Luna pode errar

As respostas são geradas por IA e **podem conter erros**. Não são conselho médico, jurídico ou financeiro. Confira o que for importante antes de agir. A Luna **não substitui** psicólogo, psiquiatra ou médico.

## Em caso de crise

O Orbit não é serviço de emergência. Se você ou alguém estiver em risco, procure ajuda imediata. No Brasil, o **CVV** atende de graça, 24h, no **188** (cvv.org.br).

## Sua conta e seu uso

Você é responsável pela sua conta e pelo que faz no app. Não use o Orbit para nada ilegal, para prejudicar outras pessoas nem para burlar o serviço.

## O que é seu e o que é nosso

Suas conversas e seus dados são **seus**. O app, a marca Orbit e a Luna são nossos. Usar o app não transfere essa propriedade.

## Disponibilidade

Oferecemos o Orbit "como está". Ele pode mudar, ficar indisponível por manutenção ou ser descontinuado. Faremos o possível para avisar mudanças relevantes.

## Limite de responsabilidade

Na medida permitida pela lei, não nos responsabilizamos por decisões tomadas com base no conteúdo gerado pela Luna, nem por indisponibilidades. Use com bom senso.

## Privacidade

O tratamento dos seus dados está explicado na **Política de Privacidade**, que faz parte destes termos.

## Mudanças nos termos

Podemos atualizar estes termos. Quando a mudança for importante, pediremos seu aceite novamente.

## Contato

Fale com a gente em **$CONTATO_PRIVACIDADE**.
""".trimIndent()
}
