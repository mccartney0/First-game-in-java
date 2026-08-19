# Loop principal — Aventura RPG regional

## Decisão de produto

O modo padrão de **Novo jogo** passa a ser a **Aventura RPG regional**. A campanha fixa das fases 1–9 permanece disponível como **Campanha narrativa**, preservando o conteúdo existente, os objetivos, a seleção de fases e a Nova campanha+.

> **Ciclo principal:** tutorial → hub/refúgio → exploração livre → evento ou missão regional → dungeon opcional ou retorno → recompensa permanente → nova decisão.

## Estados de modo

| Modo | Entrada | Mundo | Objetivo principal |
|---|---|---|---|
| Aventura RPG | Novo jogo | Mapa procedural regional | Explorar, concluir eventos, ajudar NPCs e vencer dungeons. |
| Campanha narrativa | Campanha narrativa | Fases fixas 1–9 | Seguir a história e os objetivos tradicionais. |
| Sobrevivência | Vitória ou seletor de fases | Profundidades procedurais/ondas | Resistir e alcançar novos recordes. |

O modo é salvo na sessão do slot. Assim, **Continuar** recria o tipo correto de mundo: uma sessão RPG volta a uma superfície procedural, enquanto uma sessão narrativa volta à fase fixa correspondente.

## Contratos de atividade

Uma atividade regional precisa ter cinco estados explícitos: `AVAILABLE`, `ACTIVE`, `COMPLETED`, `FAILED` ou `EXPIRED`. Nenhum evento pode permanecer ativo quando o jogador entra em dungeon, troca de mapa, volta ao menu ou carrega outro slot.

A conclusão entrega a recompensa uma única vez, registra o histórico persistente e devolve o jogador à exploração. A falha encerra apenas a atividade corrente, conserva créditos e upgrades já obtidos e permite iniciar outra atividade pelo hub. Nenhum evento regional altera `questCompletedPending` da campanha fixa.

## Hub e exploração

A tecla `H` abre o hub na superfície procedural. A exploração não é uma fase linear: o jogador pode atravessar as seis regiões, aproximar-se de POIs, conversar com NPCs, iniciar eventos ou entrar em uma dungeon. O botão **Exploração livre** fecha o painel sem alterar a atividade atual.

O retorno ao hub ocorre pela tecla `H` em qualquer área segura da superfície. Conclusões e falhas exibem um banner, limpam os alvos temporários e liberam imediatamente a próxima decisão; não há abertura automática de loja ou transição de campanha.

## Eventos

| Tipo | Objetivo | Falha | Recompensa base |
|---|---|---|---:|
| `AMBUSH` | Eliminar a composição que cercou a rota. | Tempo esgotado. | 180 créditos e 250 pontos. |
| `ELITE_HUNT` | Eliminar o alvo de elite antes da retirada. | Tempo esgotado ou alvo não localizado. | 300 créditos e 450 pontos. |
| `RESCUE` | Proteger um sobrevivente até o refúgio regional. | Sobrevivente derrotado ou tempo esgotado. | 240 créditos e reputação regional. |
| `SUPPLY_CONVOY` | Escoltar o comboio por três checkpoints. | Carga destruída ou tempo esgotado. | 220 créditos, recursos e reputação regional. |

Os dois novos eventos usam a malha de regiões e POIs existente. `RESCUE` reutiliza a física de escolta corrigida do informante; `SUPPLY_CONVOY` usa uma entidade própria com checkpoints, vida e feedback visual. Ambos recebem inimigos vinculados ao evento e não podem duplicar recompensa em saves ou mortes repetidas.

## Proteção contra travamentos

O jogo deve consumir todas as teclas de overlays antes do movimento e do tiro. Diálogos pausam o mundo, `ESC` fecha o diálogo antes da pausa, e a ausência do NPC alvo deve permitir cancelar ou reiniciar a atividade. Ao carregar um save, um evento ativo é restaurado somente se o mapa correspondente puder recriar seus alvos; caso contrário, ele é marcado como expirado e o jogador retorna à exploração sem bloqueio.

## Critérios de aceite

A implementação será aceita quando `Novo jogo` abrir a Aventura RPG por padrão; `Campanha narrativa` continuar iniciando as fases fixas; `Continuar` restaurar o modo salvo; H abrir o hub em qualquer região da superfície; RESCUE e SUPPLY_CONVOY puderem ser iniciados manualmente e automaticamente; cada evento concluir, falhar, expirar e salvar sem travar; entrar em dungeon ou voltar ao menu limpar alvos temporários; e a campanha fixa continuar aprovando sua suíte de regressões.
