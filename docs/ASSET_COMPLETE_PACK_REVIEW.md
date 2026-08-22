# Parecer de Assets — Pacotes completo e complementar

**Data de revisão:** 21 de agosto de 2026  
**Fontes preservadas:** `first_game_rpg_complete_pack.zip` e `first_game_rpg_assets_complementares.zip`  
**Método:** inventário em quarentena, leitura de metadados PNG, verificação de alfa e grade, inspeção de folha de contato e leitura nativa do Titã.

## Resultado executivo

Os dois pacotes foram aceitos como uma base de produção consistente para o RPG. Foram encontrados **193 PNGs**, dos quais **191 são assets de arte** e dois são folhas de prévia que não entram no runtime. Há **107 spritesheets**, **166 arquivos com alfa** e ciclos de personagem, NPC e inimigo com a grade esperada de três frames. Nenhum arquivo foi executado e nenhum ZIP de origem será modificado.

| Grupo | Situação | Verificação |
|---|---|---|
| Herói | Aprovado | Oito sheets de `96×32`, três frames de `32×32` por ação e direção. |
| NPCs | Aprovado | Walk cycles de `96×32` e retratos de `96×96`; a direção artística é consistente com o herói. |
| Inimigos | Aprovado | Cinquenta e dois sheets de `96×32`, com transparência e silhuetas distinguíveis. |
| Titã da Bruma | Aprovado | Oito sheets de `288×96`, três frames de `96×96`; a silhueta robótica passou a ter escala e leitura de chefe. |
| Terreno, props e biomas | Aprovado para catálogo | Tilesets, ponte, ruínas, árvores, carroça e arenas usam a linguagem de pixel art do mapa. |
| VFX, itens e UI | Aprovado para catálogo | Células transparentes em `32×32` ou sheets compatíveis; devem ser ligados somente aos sistemas que já existem. |
| Loops ambientais | Aprovado com regra especial | Tiras de `288×32` representam nove frames de `32×32`, não uma sheet de três frames. |
| `preview_contact_sheet.png` | Excluído | É material de referência opaco, não arte de runtime. |

## Decisões de integração

Os fontes aprovados serão copiados para `res/assets/incoming/complete-pack/` e os derivados para `res/assets/generated/`, com catálogo de hashes. O runtime recebe primeiro o conjunto que tem representação direta: herói, NPCs, inimigos, Titã, terrenos, props, retratos e efeitos de combate. Itens, telas, biomas alternativos e loops ambientais permanecem empacotados e catalogados, mas só são ativados quando houver uma cena ou um sistema de jogo que os consuma.

> A integração não substitui um fonte recebido. Apenas PNGs derivados e manifestos de catálogo podem ser regenerados.

## Condições de aceite contínuo

Uma alteração futura deve conservar PNG RGBA, nomes determinísticos, células de `32×32` para atores comuns ou `96×96` para o Titã, e três frames para animações de personagem, NPC, inimigo, ataque e chefe. Loops ambientais são a exceção declarada: possuem nove células horizontais de `32×32`.

Após a integração, a suíte de testes, `validateContent` e o build Android são obrigatórios. A validação final em dispositivo verifica enquadramento, movimento, mira, colisão, legibilidade de silhueta e ausência de suavização de pixel.

## Registro de integração

Os fontes aprovados foram catalogados em `res/assets/incoming/complete-pack/`; seus derivados e a lista de hashes estão em `res/assets/generated/rpg_world/complete-pack/complete_pack_catalog.json`. O importador `scripts/import_complete_packs.py` separou **105 spritesheets** em **351 frames de runtime**, copiou recursos de mundo explicitamente usados pelo APK e ignorou as duas folhas de prévia.

O runtime Android prioriza o pacote completo para o terreno, a ponte, pinheiro, arco de ruína, pilar, carroça, fogueira, baú, inimigos e Titã da Bruma. Quando um PNG não está presente, o carregador recua para o Lote 1 ou para os atlas originais; nenhum arquivo-fonte é sobrescrito. Efeitos, itens, telas, biomas alternativos e loops ambientais permanecem catalogados até que uma cena ou sistema os consuma.

| Verificação | Resultado |
|---|---|
| Compilação Android | `assembleDebug` concluído com sucesso. |
| Testes de regressão | `test` concluído com sucesso. |
| Validação de conteúdo | `validateContent` retornou zero erros e zero avisos. |
| Pixel art | O filtro de bitmap permanece desativado no runtime. |
