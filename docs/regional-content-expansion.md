# NPCs regionais, missões e masmorras

## Conteúdo regional

Cada uma das seis regiões do mundo procedural terá um NPC repetível, um título de missão secundária, uma condição de conclusão e uma recompensa distinta. As missões usarão o `SideQuestManager` existente, com IDs estáveis para sobreviver a transições e carregamento de save.

| Região | NPC | Missão | Tipo |
|---|---|---|---|
| Refúgio da Colônia | Mara, a Intendente | Suprimentos para o Refúgio | coletar MediKits |
| Ruínas Industriais | Davi, o Sucateiro | Limpeza das Ruínas | eliminar patrulhas |
| Pântano de Lodo | Iara, a Boticária | Remédios no Lodo | coletar NanoMedkits |
| Tundra de Contenção | Kellan, o Vigia | Sinal na Tundra | eliminar elites |
| Santuário da IA | Nó-7, o Arquivista | Memória do Santuário | coletar Data Cores |
| Núcleo do Supervisor | Sentinela-9 | Câmara do Núcleo | eliminar guardas |

Os NPCs permanecem no mapa após a primeira conversa. O mesmo diálogo mostra oferta, progresso ou estado concluído, sem apagar o personagem da exploração.

## Masmorras

Cada bolsão de mobs terá um portal de entrada. Ao entrar, o jogador é levado a uma instância determinística de **64×40 tiles**, gerada pela profundidade e região. A instância terá salas, corredores, suprimentos, inimigos, uma saída e um chefe regional. Ao concluir o chefe, a dungeon entrega recompensa e fica marcada como concluída no save.

| Região | Chefe regional | Identidade de combate |
|---|---|---|
| Refúgio | Infiltrador do Refúgio | mobilidade e ataques de retaguarda |
| Ruínas | Artilheiro de Sucata | pressão de longo alcance |
| Pântano | Fantasma do Lodo | drenagem e perseguição |
| Tundra | Guardião Criogênico | tanque e regeneração |
| Santuário | Supervisor do Santuário | fúria e reforços |
| Núcleo | Supervisor-Prime | chefe de alto risco |

A dungeon não substituirá o objetivo principal da campanha. Ela será um conteúdo opcional do mundo procedural, com retorno ao mapa de superfície, preservação de recursos e persistência do estado de conclusão.
