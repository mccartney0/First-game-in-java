- [x] Auditar atalhos de entrada, HUD, mini-mapa e schema de save do Mundo Aberto.
- [x] Criar marcadores personalizados no setor atual, com remoção e persistência por slot.
- [x] Renderizar marcadores personalizados e dicas de uso no mini-mapa.
- [x] Exibir aviso suave com código e nome do setor ao cruzar uma nova área.
- [x] Cobrir os novos fluxos com regressões, build limpo e QA headless.
- [x] Auditar monstros gigantes, spawns por setor, HUD e renderização ambiental.
- [x] Definir clima, fases de dia/noite e modificadores legíveis para cada setor.
- [x] Implementar telemetria ambiental, ciclo visual e persistência de estado.
- [x] Aplicar regras de clima e período aos monstros gigantes.
- [x] Validar regressões, balanceamento e QA headless antes do commit.
- [x] Auditar sons, música e roteiro do playthrough climático do Mundo Aberto.
- [x] Executar playthrough controlado pelos setores e registrar achados de clima e gigantes.
- [x] Definir camadas de ambiente e trilhas adaptativas por período e condição climática.
- [x] Integrar assets substituíveis, crossfades e telemetria de áudio no jogo.
- [x] Validar regressões, QA e playthrough final antes de publicar.
- [x] Sincronizar a main e reproduzir a falha de entrada da Aventura RPG.
- [x] Corrigir o recurso de mapa ausente no carregamento por `gradlew.bat run`.
- [x] Impedir que tonalização de Mundo Aberto escureça Campanha e Aventura RPG.
- [x] Tornar os modos iniciais e o caminho para dungeons explícitos na interface.
- [x] Adicionar regressões de fluxo, executar build, QA e playthrough de aceitação.
- [x] Auditar atlas dos inimigos, renderização de sprites, gargalos do mapa e estado do RPG Clássico.
- [x] Definir manifestos e formato de exportação do aplicativo de produção de mapas, tiles e assets.
- [x] Substituir os monstros ilegíveis por sprites transparentes com silhuetas distintas.
- [x] Introduzir setores ativos para limitar atualizações e desenho do mundo RPG grande.
- [x] Criar loop inicial de NPC, objetivo e encontro no RPG Clássico.
- [x] Implementar aplicativo desktop de geração e exportação de conteúdo.
- [x] Cobrir assets, desempenho, exportação e gameplay com regressões e QA completo.
- [x] Documentar o fluxo de criação, exportação e integração de inimigos e tiles do Content Studio ao Vale de Brumafolha.
- [x] Registrar a recomendação técnica para evolução visual dos tiles sem alterar a lógica de colisão do RPG Clássico.
- [x] Conectar o tile de grama exportado e o sprite Guardian do Content Studio ao renderizador do Vale de Brumafolha.
- [x] Isolar a composição do RPG Clássico para impedir que HUDs do shooter e mini-mapa cubram sua cena.
- [x] Substituir avisos de texto por diálogos com retrato, nome, falas e escolhas claras para Iara e o Guardião.
- [x] Criar e integrar variações determinísticas de terreno 32×32 para grama, estrada e ruínas do Vale.
- [x] Estender o Content Studio e seus manifestos com propriedades de inimigos e tiles personalizados.
- [x] Atualizar a documentação e validar visualmente o novo fluxo do RPG Clássico.
- [x] Gerar e integrar um pacote visual mais rico de variantes 32×32 para grama, estrada e ruínas de Brumafolha.
- [x] Corrigir o travamento do menu e dos controles de escape do modo RPG.
- [x] Renomear a apresentação do modo de RPG Clássico para RPG, preservando o identificador de save compatível.
- [x] Implementar inventário RPG funcional, interações de cenário e feedback de uso de itens.
- [x] Evoluir a interface e as telas do RPG sem reutilizar HUDs, menus ou terminologia do shooter.
- [x] Validar o fluxo completo de criação, diálogo, inventário, pausa, retorno ao menu e save do RPG.
- [x] Gerar e instalar na pasta de runtime as variações finais 32×32 de grama, estrada e ruínas.
- [x] Garantir a escolha determinística de variantes de tile por posição no renderizador do Vale.
- [x] Adicionar uma galeria visual interna de referências de terreno ao Content Studio.
- [x] Validar os assets, a galeria e o build completo antes da publicação.
- [x] Expandir o inventário do RPG com equipamentos, consumíveis, coleta e persistência.
- [x] Adicionar diálogos ramificados com escolhas, estados de NPC e recompensas ao modo RPG.
- [x] Integrar inimigos configuráveis do Content Studio a comportamentos de IA distintos no jogo.
- [x] Modernizar a composição, navegação e feedback dos menus principais.
- [x] Cobrir os novos fluxos de RPG, IA e menu com regressões e validação integrada.
- [x] Adicionar modelos e exportadores de consumíveis e armas configuráveis ao Content Studio.
- [x] Integrar exemplos de consumível e arma gerados ao inventário e combate do RPG.
- [x] Cobrir manifestos, assets e uso no runtime com regressões antes da publicação.
- [x] Criar uma área externa conectada ao Vale de Brumafolha, com entrada e retorno claros.
- [x] Implementar inimigos RPG ativos com vida, comportamento, ataques e feedback de dano.
- [x] Adicionar drops, experiência, subida de nível e persistência dos encontros do novo mapa.
- [x] Criar um chefe opcional com recompensa permanente e estado persistente.
- [x] Executar regressões e QA do vertical slice antes da publicação.

## Próxima expansão — Charneca da Bruma

- [x] Criar uma NPC com missão própria, diálogo ramificado, objetivo e recompensa persistente.
- [x] Adicionar um baú especial desbloqueado pelo progresso da missão e salvar seu estado.
- [x] Criar uma transição visual e mensagem de descoberta ao cruzar o Portão da Charneca.
- [x] Ampliar o Content Studio com novas variações de inimigos configuráveis e integrá-las ao encontro externo.
- [x] Cobrir a expansão com regressões de missão, recompensas, save e geração de conteúdo.

## Demonstração do Content Studio — chefe da Charneca

- [x] Criar um chefe configurável no canvas de inimigos com atributos e perfil de IA próprios.
- [x] Exportar e inspecionar o sprite PNG transparente e o manifesto JSON do chefe.
- [x] Executar a validação automatizada de exportação e compatibilidade de runtime.

## Habilidade do Soberano da Bruma e integração de assets

- [x] Mapear quais assets gerados já estão ligados ao runtime e quais permanecem apenas como referências visuais.
- [x] Criar uma habilidade especial exportável para o Soberano da Bruma no Content Studio.
- [x] Integrar a habilidade ao combate do chefe e cobrir o cooldown, o efeito e o manifesto em regressões.

## Expansão visual — personagens, armas e projéteis

- [x] Consolidar e publicar a atualização de quests, NPCs dialogáveis, savegame e portal web já implementada.
- [x] Auditar o atlas de referência recebido e os formatos/manifestos aceitos pelo Content Studio.
- [x] Criar sprites consistentes para protagonista, NPCs, armas e projéteis com fundo transparente e escala de gameplay.
- [x] Integrar os novos sprites e metadados de ponto de disparo no runtime Android RPG.
- [x] Estender o importador do Content Studio para recortar, catalogar e validar automaticamente personagens, armas e tiros.
- [x] Executar `test`, `validateContent` e o build Android; atualizar o portal e publicar os artefatos.

## Animações direcionais — caminhada e ataque

- [x] Auditar os sprites RPG, o loop de atualização e os pontos de direção existentes no runtime Android.
- [x] Estender o Content Studio com contratos e exportadores de frames para caminhar e atacar nas quatro direções.
- [x] Gerar quadros transparentes e sincronizá-los automaticamente com os assets do APK.
- [x] Aplicar seleção temporal de frames, direção e feedback de ataque ao protagonista e aos NPCs.
- [x] Executar regressões, `validateContent` e o build Android antes da publicação.

## Wiki, playthrough e onboarding interativo

- [x] Mapear os sistemas, missões e regiões que devem receber páginas navegáveis na wiki.
- [x] Criar um percurso de playthrough com etapas, objetivos, decisões e marcos de progresso.
- [x] Implementar onboarding contextual no APK para movimento, combate, interação, inventário e savegame.
- [x] Adicionar wiki e playthrough interativos ao portal com navegação, busca e acompanhamento de etapas.
- [x] Validar o APK, a página web e os links de download antes da publicação.

## Imersão sonora — expedição RPG

- [x] Mapear os gatilhos de passos, ataque, disparo, impacto, diálogo e conquista no loop Android.
- [x] Produzir um pacote leve de efeitos sonoros com variação de passos, magia, aço e conversa.
- [x] Integrar áudio de baixa latência com volume contextual, controle de repetição e liberação segura no ciclo da Activity.
- [x] Sincronizar os sons com caminhada, combate e caixas de diálogo sem bloquear a renderização.
- [x] Executar regressões, build Android e verificação do pacote final antes da publicação.

## Conquistas — cinco marcos da expedição

- [x] Modelar os cinco marcos de missão e o estado de conquista persistente no portal.
- [x] Criar um mural interativo de conquistas com progresso, emblemas e transições acessíveis.
- [x] Vincular o playthrough aos marcos desbloqueáveis e manter o progresso no navegador.
- [x] Validar as rotas, o estado interativo e a experiência móvel antes da publicação.

## Música adaptativa — regiões e chefes

- [x] Mapear regiões, estados de exploração e transições de chefes no loop Android.
- [x] Compor e exportar faixas em loop para Clareira, Águas do Norte, Fortaleza e confrontos de chefe.
- [x] Implementar um diretor musical com troca segura de loops, intensificação em chefes e restauração ao retomar o jogo.
- [x] Expor controle de música e volume no HUD sem competir com os controles de ação.
- [x] Validar transições, ciclo de vida, pacote Android e regressões antes da publicação.

## Wiki viva e Códice da Bruma

- [x] Criar um Códice navegável para regiões, criaturas, personagens, armas, relíquias e chefes.
- [x] Atualizar a wiki com animações, onboarding, áudio, conquistas e futuras notas de versão.
- [x] Adicionar um índice de atualizações que conecte cada implementação nova aos artigos técnicos relevantes.
- [x] Validar navegação, busca, responsividade e consistência visual do portal antes da publicação.
