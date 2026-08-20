# Content Studio no Vale de Brumafolha

Este guia explica como criar assets no **Content Studio** e conectá-los à cena do **Vale de Brumafolha**, o mapa do modo **RPG Clássico**. O fluxo foi desenhado para separar arte de regras de jogo: trocar um PNG não modifica colisões, posições da missão ou save.

> O Vale usa uma grade lógica de **32×32 pixels por tile**. A arte pode evoluir sem alterar essa medida, que é compartilhada por movimento, câmera e colisão.

## Abrindo a ferramenta

No Windows, abra `tools\open_content_studio.bat`. Como alternativa, a partir da raiz do projeto, execute:

```bat
gradlew.bat runContentStudio
```

No Linux ou macOS, use `./gradlew runContentStudio`. Para restaurar o pacote visual padrão do projeto, execute `gradlew.bat generateDefaultEnemySprites` no Windows ou `./gradlew generateDefaultEnemySprites` nos demais sistemas.

| Aba do Content Studio | O que ela produz | Diretório de saída |
| --- | --- | --- |
| **Mapas** | PNG procedural e manifesto JSON. | `bin/large_rpg_maps/` ou `bin/open_world_maps/` |
| **Tiles** | Tile PNG de 32×32 e manifesto JSON. | `res/assets/generated/tiles/` |
| **Inimigos** | Sprite PNG transparente de 32×32 e manifesto JSON. | `res/assets/generated/enemies/` |
| **Manifesto** | Inspeção do JSON da última exportação. | Ao lado do arquivo exportado |

## Criando um tile para Brumafolha

O terreno de grama comum do Vale é ligado ao asset `brumafolha_grass.png`. Para criar ou substituir esse tile, abra a aba **Tiles**, informe exatamente o nome `brumafolha_grass`, escolha **GRAMA** e clique em **Exportar tile 32×32**.

O aplicativo salva dois arquivos:

| Arquivo | Função |
| --- | --- |
| `res/assets/generated/tiles/brumafolha_grass.png` | Arte desenhada em toda área de grama padrão do Vale. |
| `res/assets/generated/tiles/brumafolha_grass.json` | Manifesto com tamanho, tipo e exigência de alfa. |

Depois de exportar, feche e abra o jogo novamente. O catálogo de assets mantém imagens em cache enquanto a aplicação está em execução. O renderizador do Vale aplica automaticamente o PNG a tiles de grama; o mapa e a colisão continuam idênticos.

Para criar tiles experimentais sem substituir a grama já integrada, use nomes próprios como `brumafolha_pedra_01` ou `brumafolha_ruinas_01`. Eles serão exportados de forma válida, mas ainda precisam de um vínculo de renderização antes de aparecer no Vale. O próximo passo recomendado é ampliar `RpgMap` com uma tabela de materiais, mantendo a mesma grade de colisão.

## Criando inimigos e ligando-os à missão

Na aba **Inimigos**, escolha um papel de combate e uma paleta. A ferramenta cria sprites de 32×32 com fundo transparente e bordas seguras para reduzir artefatos quando o jogo os desenha em escala lógica menor.

| Papel no Content Studio | Nome exportado | Uso atual no jogo |
| --- | --- | --- |
| **SCOUT** | `scout_ref.png` | Variantes leves e fallback legível. |
| **BOMBER** | `enemy_bomber.png` | Variantes explosivas e Sapper. |
| **SHIELDER** | `enemy_shielder.png` | Shielder, Warden e Sentinel. |
| **ARTILLERY** | `enemy_artillery.png` | Artillery, Sniper e Teleporter. |
| **SWARM** | `enemy_swarm.png` | Swarm e Phantom. |
| **GUARDIAN** | `enemy_guardian.png` | Guardian, chefes pesados e o **Guardião do Bosque**. |

O inimigo da missão **Guardião do Bosque** usa diretamente `enemy_guardian.png`. Para trocar sua arte, gere novamente o papel **GUARDIAN** e reinicie o jogo. A luta, os três pontos de integridade, o custo de 8 de stamina por ataque e a recompensa permanecem os mesmos.

> Um sprite destinado ao jogo deve preservar silhueta distinta, alfa real no fundo e contraste contra terreno verde, cinza e escuro. Evite moldura, texto, círculo de HUD e fundo xadrez dentro do PNG.

## Criando mapas personalizados

Na aba **Mapas**, selecione **Aventura RPG regional** ou **Mundo Aberto**, defina largura, altura, profundidade e uma seed. A seed é reprodutível: anote-a para gerar de novo a mesma topologia.

| Tipo | Tamanho mínimo | Destino | Uso atual |
| --- | --- | --- | --- |
| Regional | 192×128 tiles | `bin/large_rpg_maps/` | Aventura RPG regional. |
| Mundo Aberto | 512×320 tiles | `bin/open_world_maps/` | Modo Mundo Aberto. |
| Vale de Brumafolha | 36×24 tiles | Código de `RpgMap` | RPG Clássico. |

Os mapas procedurais exportados não substituem automaticamente o Vale, porque Brumafolha contém posicionamento narrativo específico de Iara e do Guardião. Use o Content Studio para criar assets visuais do Vale agora; para converter um mapa procedural em uma nova área do RPG Clássico, será necessário adicionar uma cena e seu conjunto de NPCs/objetivos ao código.

## Decisão recomendada para qualidade visual dos tiles

Não é recomendável aumentar agora `RpgMap.TILE_SIZE` de 32 para 48 ou 64. Essa constante também controla a malha de colisão, limites de câmera, posição de NPCs, spawn do jogador e localização do Guardião. Alterá-la exigiria redimensionar o mapa inteiro e revalidar movimento, missão e saves.

| Alternativa | Impacto visual | Risco técnico | Recomendação |
| --- | --- | --- | --- |
| Aumentar `TILE_SIZE` lógico | Mais área para arte por tile. | Alto: altera física, câmera e todas as coordenadas. | Não agora. |
| Exportar PNG maior e desenhá-lo em 32×32 | Nenhum ganho final; a imagem será reduzida. | Baixo, mas desperdiça detalhe. | Evitar. |
| Melhorar arte em 32×32 com variantes | Textura e leitura muito melhores sem romper a grade. | Baixo. | **Implementar primeiro.** |
| Criar atlas 32×32 com 4–8 variantes por terreno | Reduz repetição e melhora aparência do mapa inteiro. | Médio, apenas no renderizador. | Próximo incremento recomendado. |
| Aumentar a escala da janela | Exibe os mesmos pixels maiores. | Baixo; não acrescenta detalhe. | Opcional para telas maiores. |

A evolução indicada é manter a grade física em 32×32 e criar **variações visuais de 32×32** para grama, vila, floresta, estrada e ruínas. O renderizador pode escolher uma variante de modo determinístico pela posição do tile. Assim, o mapa fica mais rico, preserva pixel art nítida e não altera colisão, missão ou compatibilidade de saves.

## Checklist antes de testar

1. Exportar o PNG e confirmar o manifesto JSON correspondente.
2. Usar o nome exato do asset integrado, como `brumafolha_grass` ou o papel `GUARDIAN`.
3. Fechar e reabrir o jogo para limpar o cache de assets.
4. Executar `gradlew.bat build` no Windows antes de publicar uma alteração.
5. Entrar no RPG Clássico, falar com Iara e confirmar que o Guardião continua visível e derrotável no Bosque dos Sussurros.
