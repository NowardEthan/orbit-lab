# Orbit — roadmap guarda-chuva

> Norte: transformar o Orbit em um sistema operacional pessoal da Luna: um lugar onde conversa,
> memoria, financas, documentos, bolha e a tela Hoje se conectam como um produto so.
>
> Criado: 2026-08-05. Canal de ship: OrbitLab `lab`.
> Roadmaps relacionados: [`AGENTICO-ROADMAP.md`](AGENTICO-ROADMAP.md),
> [`AGENTICO-POLISH-ROADMAP.md`](AGENTICO-POLISH-ROADMAP.md),
> [`ARTEFATOS-ROADMAP.md`](ARTEFATOS-ROADMAP.md),
> [`BOLHA-ROADMAP.md`](BOLHA-ROADMAP.md).

---

## Sensacao alvo

| Pilar | O que o app deve fazer sentir |
|-------|-------------------------------|
| Hoje | "O Orbit sabe o que importa agora." |
| Luna | "Ela entende onde estou e consegue agir sem eu configurar modo." |
| Memoria | "As coisas que fiz com ela continuam encontraveis." |
| Acoes | "Quando ela trabalha, vejo o plano, o progresso e o resultado." |
| Modulos | "Financas, documentos, chat e bolha sao partes da mesma Luna." |
| Confianca | "O app nao inventa numero, nao esconde erro e nao me sobrecarrega." |

---

## Principios de produto

1. **Primeira tela util, nao vitrine.** Ao abrir, o usuario deve poder agir ou entender o dia.
2. **Uma Luna, varios contextos.** A Luna muda de foco pelo lugar e pelo pedido, nao por seletores.
3. **Estado honesto.** Numeros zerados, metas ausentes, falhas e planejamentos futuros precisam dizer a verdade.
4. **Acoes tangiveis.** Ferramentas, documentos e eventos devem deixar rastros navegaveis.
5. **Polish quieto.** Brilho, motion e glass existem para orientar, nao para competir com a tarefa.
6. **Ship por camadas.** Cada fase precisa caber em uma release testavel no aparelho.

---

## Mapa das fases

| Fase | Nome | Resultado |
|------|------|-----------|
| **O0** | Fundacao e inventario | Saber o que existe, onde esta e o que esta quebrado |
| **O1** | Hoje cockpit | Inicio vira painel vivo do dia |
| **O2** | Luna transversal | Contexto por modulo + acoes sugeridas |
| **O3** | Busca e comandos | Abrir, achar e agir de qualquer lugar |
| **O4** | Timeline unificada | Historico de eventos e feitos da Luna |
| **O5** | Artefatos como workspace | Documentos ganham estante, status e acoes rapidas |
| **O6** | Bolha integrada | Bolha com continuidade, sinal e contexto externo |
| **O7** | Financeiro inteligente | Tags, planejado vs realizado e orcamento guiado |
| **O8** | Operacao e confianca | Telemetria opaca, QA, performance e release hygiene |

Ordem recomendada:

```text
O0 -> O1 -> O2 -> O3
          \-> O4 -> O5
          \-> O7
O6 em paralelo depois de O1
O8 acompanha todas
```

---

## O0 — Fundacao e inventario

**Meta:** ter uma fotografia honesta do Orbit antes de empilhar mais produto.

### Escopo

- Mapear telas atuais: Inicio, Chat, Galeria/artefatos, Financas, Planos, Ajustes, Bolha.
- Registrar quais repositorios/camadas sao fonte de verdade para cada dominio.
- Levantar empty states fracos, estados quebrados e dados que parecem fake.
- Criar checklist smoke padrao por release lab.
- Definir vocabulario visual comum: chips, timeline, cards, badges, barra viva, CTA.

### DoD

- [ ] Documento de inventario com telas e fluxos.
- [ ] Checklist smoke OrbitLab atualizado.
- [ ] Lista priorizada de bugs de confianca.
- [ ] Nenhum novo modulo sem owner/fonte de verdade documentado.

---

## O1 — Hoje cockpit

**Meta:** a tela Inicio deixa de ser apenas vitrine e vira o resumo vivo do Orbit.

### Entregas

- **Faixa Hoje:** saudacao + estado atual da Luna + 1 acao recomendada.
- **Cartoes compactos:** Financas, Conversas, Artefatos, Proximos compromissos, Update.
- **Continuar de onde parou:** ultima conversa, ultimo documento, ultima acao da Luna.
- **Empty states uteis:** quando nao ha dados, sugerir o primeiro passo sem tutorialzao.
- **Motion de presenca:** brilho discreto apenas em estados vivos/ativos.

### Fora da fase

- Busca global completa.
- Timeline historica profunda.
- Automacoes reais.

### DoD

- [ ] Inicio mostra pelo menos 3 sinais reais do app quando houver dados.
- [ ] Estado zerado nao mostra cards fake.
- [ ] Tocar em cada card leva ao modulo certo.
- [ ] Visual cabe em telas pequenas sem cortar texto.

---

## O2 — Luna transversal

**Meta:** a Luna entende o modulo atual e fala/age com contexto, sem virar varios bots separados.

### Entregas

- Contexto de tela no request: `inicio`, `chat`, `financas`, `artefato`, `galeria`, `ajustes`.
- Sugestoes de prompt por modulo, sem poluir o composer.
- Respostas que sabem onde agir: "abrir metas", "registrar", "continuar documento".
- Estado agentico unico: timeline e streaming com a mesma linguagem visual em todos os contextos.
- Politica de fallback: se nao puder agir, explicar e oferecer o caminho manual.

### DoD

- [ ] Luna em Financas nao responde como chat generico.
- [ ] Luna em artefato consegue citar/abrir o documento ativo.
- [ ] Inicio consegue pedir "o que tenho para ver hoje?" com contexto real.
- [ ] Small talk continua leve, sem tools desnecessarias.

---

## O3 — Busca e comandos

**Meta:** achar qualquer coisa e disparar acoes sem navegar pela gaveta.

### Entregas

- Command palette simples, acionada por botao/atalho visual.
- Busca local por conversas, documentos, financas e telas.
- Acoes diretas: registrar gasto, criar documento, abrir metas, abrir ultima conversa.
- Ranking simples: recentes primeiro, depois correspondencia por titulo/tag/categoria.
- Estados: carregando, vazio, erro e resultados agrupados.

### DoD

- [ ] "meta", "cartao", "documento", "almoço" retornam resultados relevantes.
- [ ] Acoes diretas funcionam sem precisar abrir o modulo antes.
- [ ] Palette fecha com back e nao briga com teclado.
- [ ] Nenhum dado sensivel aparece em logs/crashes.

---

## O4 — Timeline unificada

**Meta:** o Orbit tem uma memoria visual dos acontecimentos, nao so listas isoladas.

### Eventos candidatos

- Mensagem importante ou conversa retomada.
- Documento criado/editado pela Luna.
- Movimento financeiro registrado, planejado ou pago.
- Meta criada/atingida.
- Update publicado/instalado.
- Acao agentica concluida ou falha.

### Entregas

- Modelo local de `OrbitEvent`.
- Timeline no Inicio com ultimos eventos.
- Filtros por tipo: Luna, Financas, Artefatos, Sistema.
- Deep links: tocar no evento abre o objeto original.
- Agrupamento por dia: Hoje, Ontem, Esta semana.

### DoD

- [ ] Eventos nao duplicam a cada snapshot.
- [ ] Eventos sensiveis usam texto seguro e curto.
- [ ] Tocar em evento funciona para pelo menos Financas, Chat e Artefatos.
- [ ] Timeline vazia ensina sem parecer marketing.

---

## O5 — Artefatos como workspace

**Meta:** documentos deixam de ser apenas resposta de chat e viram objetos do Orbit.

### Entregas

- Estante de artefatos com status: rascunho, editado, gerado pela Luna, favorito.
- Preview compacto no Inicio e na Timeline.
- Acoes rapidas: abrir, renomear, continuar com Luna, exportar/copiar.
- Melhor ponte chat -> documento: quando a Luna cria/edita, o artefato aparece claramente.
- Revisao visual do editor para estados de bloco, outline e empty.

### DoD

- [ ] Documento criado pela Luna aparece fora do chat.
- [ ] Abrir artefato preserva posicao/outline.
- [ ] Continuar com Luna referencia o artefato correto.
- [ ] Export/preview nao perde conteudo.

---

## O6 — Bolha integrada

**Meta:** a bolha parece uma extensao do Orbit, nao um atalho separado.

### Entregas

- Continuar fases B2/B3/B4 do [`BOLHA-ROADMAP.md`](BOLHA-ROADMAP.md).
- Sinal de atividade: pensando, erro, resposta nova, cota.
- Contexto externo leve: "estou fora do app", sem prometer leitura impossivel.
- Abrir app a partir da bolha preservando conversa e estado.
- Ajustes/OEM com copy clara para bateria, overlay e autostart.

### DoD

- [ ] Bolha some no foreground e volta no background sem pulo feio.
- [ ] Painel e chat principal mantem continuidade.
- [ ] Erro/cota nao parecem bolha morta.
- [ ] Smoke em aparelho de referencia.

---

## O7 — Financeiro inteligente

**Meta:** consolidar a confianca financeira e adicionar inteligencia sem inventar dados.

### Entregas

- Planejado vs realizado com bloco proprio de proximos compromissos.
- Tags automaticas/sugeridas pela Luna, com cor e filtro.
- Orcamento guiado: sugerir meta com base em recorrentes/historico, nunca criar fake.
- Regras de recorrentes: vencendo, atrasado, pago, previsto.
- Insights com explicacao curta: "por que a Luna esta dizendo isso?"

### DoD

- [x] Sem meta fantasma de R$ 3.000,00.
- [x] Lancamentos futuros nao contam antes do dia.
- [x] Tags base em movimentacoes.
- [ ] Bloco "Proximos compromissos" separado.
- [ ] Luna sugere tags ao registrar.
- [ ] Orcamento guiado sem automatismo perigoso.

---

## O8 — Operacao e confianca

**Meta:** melhorar a velocidade de ship sem perder seguranca, privacidade e qualidade.

### Entregas

- Checklist smoke por modulo antes de release.
- Telemetria opaca de telas, falhas, latencia e agentic gate, sem conteudo sensivel.
- Performance: recomposicao do chat/Inicio/timeline em listas longas.
- Estados de erro padronizados.
- Processo de release com versao, notas e manifest sempre conferidos.

### DoD

- [ ] Toda release tem checklist minimo preenchido.
- [ ] CrashReporting nunca recebe texto financeiro/chat/documento.
- [ ] `Build & Release Lab` verde antes de anunciar update.
- [ ] App continua usavel offline/parcial onde fizer sentido.

---

## Primeira leva recomendada

### Sprint O1.1 — Inicio vivo

1. Redesenhar Inicio como cockpit compacto.
2. Adicionar cards reais: continuar conversa, financeiro realizado/planejado, artefatos recentes.
3. Empty states honestos.
4. Smoke em aparelho + release lab.

### Sprint O2.1 — Contexto da Luna por tela

1. Enviar contexto de tela para `luna-core`.
2. Ajustar prompts locais/sugestoes.
3. Garantir que Financas, Artefatos e Inicio geram respostas contextualizadas.
4. Usar a mesma timeline agentica polida.

### Sprint O3.1 — Command palette MVP

1. UI de palette.
2. Index local simples: telas, conversas, artefatos, tags financeiras.
3. Acoes diretas basicas.
4. Deep links via `FinancasNav`/shell.

---

## Backlog solto, mas valioso

- Favoritos/pins globais.
- Perfis de foco: trabalho, estudo, casa.
- Inbox interno de notificacoes calmas.
- Lembretes/automacoes quando houver ferramenta segura.
- Busca semantica em documentos e conversas.
- Melhor onboarding para permissao da bolha.
- Galeria de documentos com colecoes.
- Tema visual de estados da Luna: pensando, agindo, verificando, pronto.

---

## Como usar este roadmap

- Quando comecar uma fase, criar uma spec menor: `roadmaps/O1-inicio-vivo.md` ou equivalente.
- Cada fase deve terminar em uma release testavel.
- Nao shipar "sistema inteiro" de uma vez; shipar sensacao coerente por fatia.
- Se uma melhoria tocar `luna-core`, `OrbitLab` e UI ao mesmo tempo, documentar o contrato primeiro.
