# Loop RPG regional

## Visão geral

A expansão transforma o mundo procedural pós-campanha em um ciclo de exploração com decisões opcionais. A campanha fixa continua usando as fases 1–9 e não exige o novo hub. No modo procedural de superfície, o jogador pode abrir o hub regional, aceitar a tarefa do NPC da região, iniciar um evento, entrar em uma dungeon ou simplesmente voltar à exploração.

> **Princípio de design:** atividades opcionais devem ampliar a exploração, não bloquear o objetivo principal nem obrigar o jogador a repetir telas entre fases.

## Hub regional

O hub é um estado modal chamado `REGIONAL_HUB`. Ele abre com a tecla `H` somente quando o jogador está no mundo RPG procedural, em uma região identificada, fora de dungeons, diálogos, inventário e transições. O mundo fica congelado enquanto o painel está aberto.

| Opção | Comportamento |
|---|---|
| Missão principal | Fecha o hub e reapresenta o objetivo principal atual. |
| Missão do NPC regional | Ativa a mesma definição persistente oferecida pelo NPC da região e atualiza o HUD de missão. |
| Evento regional | Escolhe deterministicamente uma emboscada ou caça a elite ainda disponível no ciclo. |
| Masmorra opcional | Solicita a entrada na dungeon da região, exceto quando ela já foi concluída. |
| Exploração livre | Fecha o hub sem alterar o objetivo. |
| Fechar hub | Cancela a decisão e retorna à superfície. |

As setas ou `W/S` navegam, `ENTER` confirma e `ESC` cancela. O NPC continua existindo no mapa e usa a mesma definição de quest utilizada pela opção do hub; portanto, falar com Mara, Davi, Iara, Kellan, Nó-7 ou Sentinela-9 permanece uma alternativa válida.

## Eventos dinâmicos

`DynamicEventManager` controla eventos de superfície com estado temporário e histórico persistente. Após aproximadamente quinze segundos de exploração sem atividade corrente, o mundo pode iniciar automaticamente um evento quando a quantidade de inimigos estiver baixa. O hub também permite início manual.

| Evento | Encontro | Conclusão | Recompensa |
|---|---|---|---|
| Emboscada regional | Bomber, dois Swarm, Shielder e Sniper | Derrotar todas as ameaças rastreadas | 180 créditos permanentes e 250 pontos |
| Caça à elite | Sniper ou Shielder de elite, acompanhado por pressão de grupo | Derrotar o alvo de elite | 300 créditos permanentes e 450 pontos |

O evento ativo aparece em um card próprio da HUD com objetivo, ameaças restantes e tempo em segundos. Ele expira após noventa segundos. Ao trocar de camada, entrar em dungeon ou regenerar o mapa, apenas o evento corrente é encerrado; as conclusões anteriores continuam registradas. Os inimigos criados pelo evento são vinculados ao evento para que uma morte normal conclua a atividade sem duplicar recompensa.

## Novos inimigos

As quatro variantes foram adicionadas ao enum `Enemy.Variant` e possuem comportamento, parâmetro de combate e marcador visual próprios.

| Variante | Papel | Comportamento principal |
|---|---|---|
| `BOMBER` | Explosivo frágil | Emite um aviso de proximidade e explode ao morrer, causando dano moderado em área ao piloto e a inimigos próximos. |
| `SHIELDER` | Suporte | Emite uma aura de suporte; aliados próximos recebem redução de dano de projéteis. Deve ser priorizado para desmontar a formação. |
| `SNIPER` | Longo alcance | Mantém baixa mobilidade e dispara um tiro de precisão de alcance e dano altos quando há linha de visão. |
| `SWARM` | Pressão de grupo | Tem pouca vida, velocidade alta, avanço em rajadas e probabilidade de contato maior que a de um mob comum. |

As variantes novas só entram no sorteio geral a partir da fase 4. Eventos e conteúdos regionais podem instanciá-las explicitamente, permitindo uma composição previsível sem sobrecarregar as fases iniciais.

## Dungeons e progressão permanente

As dungeons existentes continuam sendo procedurais, opcionais e específicas por região. A entrada acontece pelo hub ou pelos portais já presentes no mundo. A saída permanece bloqueada até que o chefe regional correto seja derrotado. `DungeonManager` grava a conclusão por região e impede que a recompensa seja entregue novamente.

Cada chefe regional concede 350 créditos, 500 pontos e uma arma permanente própria. O mapeamento atual é o seguinte:

| Região | Arma permanente |
|---|---|
| Refúgio | Bumerangue arcano |
| Ruínas | Disruptor de arco |
| Pântano | Canhão solar |
| Tundra | Arco em cadeia |
| Santuário | Cortador de plasma |
| Núcleo | Lança de fusão |

Se a arma já estiver desbloqueada, a conclusão continua registrada e os créditos continuam sendo concedidos uma única vez por dungeon; o arsenal não é duplicado.

## Persistência e transições

A sessão JSON mantém as conclusões de quests, dungeons e eventos. O novo campo `dynamicEvents` contém o histórico de conclusões e, quando aplicável, o tipo, região, profundidade, tempo e necessidade de geração do evento ativo. O carregamento restaura esse estado antes do retorno ao mapa e não reabre diálogos ou a tela de seleção de arma.

Um novo jogo limpa eventos temporários e seu histórico de ciclo, mas não apaga as melhorias permanentes do piloto, créditos acumulados ou armas desbloqueadas. A campanha fixa continua abrindo seus estados originais (`NORMAL`, `MENU`, `SHOP`, `LEVELUP`, `LEVELSELECT` e `GAMEOVER`) sem depender de `REGIONAL_HUB`.

## Critérios de aceite verificados

| Sistema | Critério |
|---|---|
| Hub | Abre apenas na superfície procedural, congela o jogo e aceita navegação, confirmação e cancelamento. |
| Missão regional | A escolha pelo hub ativa a mesma quest persistente do NPC e conserva título e progresso. |
| Evento | Emboscada e caça a elite possuem geração automática/manual, card de HUD, tempo limite e recompensa. |
| Inimigos | As quatro variantes têm comportamento distinto e marcadores visuais de identificação. |
| Dungeon | A entrada é opcional, o chefe regional é validado, a saída só libera após a vitória e a conclusão é persistente. |
| Progressão | Dungeon e eventos concedem créditos permanentes; dungeons também podem desbloquear armas. |
| Save/load | Estado de eventos concluídos/ativos, quests e dungeons é serializável e recuperável. |
| Campanha | O build e a suíte headless preservam os objetivos e transições das fases 1–9. |

## Arquivos principais

A interface está em `src/com/traduvertgames/graficos/HubScreen.java`; eventos em `src/com/traduvertgames/world/DynamicEventManager.java`; novas variantes e habilidades em `src/com/traduvertgames/entities/Enemy.java`; recompensas em `src/com/traduvertgames/world/DungeonManager.java`; integração de ciclo e HUD em `src/com/traduvertgames/main/Game.java`; e persistência em `src/com/traduvertgames/main/SaveManager.java`.
