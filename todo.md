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

## Direção musical narrativa e melhoria de assets

- [x] Mapear ameaças próximas, dano recebido, chefes derrotados e conversas de Ava, Orin e Ilyra.
- [x] Compor camadas de combate leve, médio e crítico, além de uma vinheta de vitória para chefes.
- [x] Criar leitmotifs curtos e exclusivos para os NPCs principais, com transições sem cortar efeitos de diálogo.
- [x] Integrar o estado de intensidade ao diretor musical, com retorno estável à região após o confronto.
- [x] Publicar no Códice um guia de melhoria de assets com silhueta, animação, atlas, paleta, metadados e validação.
- [x] Validar mixagem, ciclo de vida Android, APK, documentação e responsividade antes da publicação.

## Asset Coach e documentação operacional

- [x] Mapear importadores, exportadores, manifestos e validações que o Asset Coach deve reutilizar.
- [x] Criar uma aba Asset Coach no Content Studio com inspeção de PNG, prévia, diagnóstico e plano de correção.
- [x] Implementar normalização segura para cópias de trabalho: escala, canvas 32×32, alfa, recorte e nomes de frames.
- [x] Gerar ou atualizar manifestos RPG com os metadados mínimos, sem sobrescrever assets-fonte.
- [x] Documentar no Git o fluxo de criar, importar, ajustar, validar, exportar e sincronizar assets e trilhas.
- [x] Atualizar Wiki Viva e Códice para apontar o novo fluxo de produção.
- [x] Executar testes, `validateContent`, builds e validação visual antes de publicar.

## Asset Coach — lote, comparação e cobertura

- [x] Mapear a seleção de arquivos, a fila de normalização e os dados de frames já exportados.
- [x] Implementar importação em lote com progresso, relatório individual e isolamento de falhas por arquivo.
- [x] Criar comparação visual lado a lado antes/depois, com restauração segura da prévia original.
- [x] Adicionar botão Desfazer que descarte a cópia de trabalho sem tocar no arquivo-fonte.
- [x] Construir painel de cobertura de animações com status por entidade, direção, caminhada e ataque.
- [x] Documentar os comandos e atualizar Wiki Viva e Códice com o novo fluxo.
- [x] Executar testes, `validateContent`, builds e inspeção visual antes da publicação.

## Asset Coach — exportação, regras e criação de assets

- [x] Mapear o modelo do relatório de cobertura e os pontos seguros de exportação CSV/PDF.
- [x] Implementar exportação do relatório de animações em CSV e PDF com resumo, grade e itens ausentes.
- [x] Criar regras aprovadas de correção em lote, com seleção explícita e relatório de cada transformação.
- [x] Adicionar arrastar e soltar de PNGs à fila, mantendo diagnósticos e falhas isoladas por arquivo.
- [x] Ampliar o guia com criação no Content Studio e em editores externos, exportação, importação e teste no APK.
- [x] Atualizar a Wiki Viva e o Códice com o fluxo de produção e publicação de assets.
- [x] Executar testes, `validateContent`, builds e inspeção visual antes da publicação.

## Asset Coach — presets e prévia animada

- [x] Mapear a configuração comum e as diferenças de exportação entre Aseprite, Krita e Piskel.
- [x] Criar presets aplicáveis que ajustem canvas, alfa, grade, escala e nomeação de frames.
- [x] Implementar prévia animada com play, pausa, velocidade e seleção de direção/ação antes da exportação.
- [x] Cobrir os presets e a composição de frames com testes automatizados.
- [x] Documentar o fluxo por editor no Git e na Wiki Viva.
- [x] Executar testes, `validateContent`, builds e inspeção visual antes da publicação.

## Asset Coach — importação automática de spritesheets

- [x] Mapear a grade, a ordem de leitura e o contrato de nomeação para converter uma spritesheet em frames RPG.
- [x] Implementar divisão não destrutiva por largura, altura, colunas e linhas, com prévia dos frames resultantes.
- [x] Integrar a importação de spritesheet à fila, à prévia animada e ao pacote de saída `rpg_sprites`.
- [x] Cobrir folhas válidas, dimensões inválidas e nomeação determinística com testes automatizados.
- [x] Documentar o fluxo para Aseprite, Krita e Piskel no Git e na Wiki Viva.
- [x] Executar testes, `validateContent`, builds e inspeção visual antes da publicação.

## Direção de arte — revisão e troca de assets

- [x] Auditar escala, coerência de estilo, leitura de silhueta, HUD e contraste a partir da captura do APK.
- [x] Definir a ficha de entrega e os prompts de arte para cenário, herói, NPCs, inimigos, chefe, itens e efeitos.
- [ ] Acrescentar critérios de validação visual ao Asset Coach para rejeitar escala, grade, alfa e nomenclatura incompatíveis.
- [x] Receber, catalogar e importar os assets aprovados sem sobrescrever os originais enviados.
- [x] Corrigir a composição de jogo, testar no APK e atualizar a documentação de produção visual.

### Lote 1 recebido

- [x] Inspecionar o ZIP sem executar arquivos e inventariar formatos, dimensões e nomes.
- [x] Validar spritesheets, tileset, pivôs e transparência contra `docs/ASSET_ART_DIRECTION.md`.
- [x] Aprovar, devolver ou ajustar cada asset de forma não destrutiva antes de integrá-lo ao APK.

### Pacotes completos recebidos

- [x] Inventariar `complete_pack` e `assets_complementares` em quarentena sem executar conteúdo recebido.
- [x] Validar por categoria os tiles, personagens, NPCs, inimigos, chefe, efeitos e UI contra a ficha de arte.
- [x] Gerar somente derivados de runtime aprovados e atualizar os manifestos de conteúdo necessários.
- [x] Integrar os aprovados com fallbacks seguros, mantendo bloqueados os assets que não atendam ao contrato.
- [x] Validar o APK, documentar o parecer por arquivo e publicar o resultado.

## Combate móvel — movimento e mira

- [x] Mapear a zona morta, a velocidade, a orientação e o disparo atuais do personagem.
- [x] Separar direção de movimento e direção de mira para permitir recuo e ataque lateral.
- [x] Calibrar zona morta, aceleração, velocidade diagonal e cadência de toque no controle móvel.
- [x] Exibir feedback discreto de direção e ponto de disparo sem encobrir o combate.
- [x] Executar testes, montar o APK e documentar os controles ajustados.
