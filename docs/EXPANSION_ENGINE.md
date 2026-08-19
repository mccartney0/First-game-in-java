# Engine de expansão do RPG

A expansão do RPG agora está dividida em três camadas: **catálogo de conteúdo**, **gerador de mundo** e **integração de runtime**. O jogo continua carregando PNGs como mapas compatíveis, mas os dados de regiões, POIs e perfis de spawn ficam centralizados em `RpgExpansionEngine`.

## Gerar mapas grandes

O comando abaixo gera um mapa de 192×128 tiles, com profundidade 2, semente determinística e manifesto JSON:

```bash
tools/generate_large_maps.sh \
  --width=192 \
  --height=128 \
  --depth=2 \
  --count=3 \
  --seed=424242 \
  --output=bin/large_rpg_maps
```

Cada saída contém um PNG compatível com `World.restartGameFromFile(...)` e um JSON com dimensões, semente, regiões, POIs e orçamento de inimigos. Alterar a semente preserva a mesma estrutura de regras e produz outro layout. A Aventura RPG usa o gerador grande por padrão; o gerador anterior permanece reservado ao modo de sobrevivência e aos testes legados.

## Adicionar uma região

Para criar uma nova região, adicione uma `RegionSpec` ao catálogo `RpgExpansionEngine`, incluindo identificador, nome de apresentação, cor de piso compatível com o carregador atual, cor de destaque, densidade e variantes preferenciais. Em seguida, ajuste a divisão espacial em `regionForTile(...)` e inclua o POI inicial em `defaultPois()`.

A região deve usar uma das cores de piso já interpretadas por `World`: preto para piso normal, marrom para lama ou cinza-azulado para gelo. A direção visual mais rica fica nos assets, nos acentos do manifesto e nos elementos desenhados pela HUD; isso evita quebrar a compatibilidade do mapa PNG.

## Adicionar um POI

Um POI é definido por identificador estável, tipo lógico, região e posição relativa em milésimos do mapa. O gerador resolve a posição para qualquer largura e altura e registra o mesmo ponto no `RpgWorldManager`, mantendo hub, NPCs, eventos e dungeons alinhados ao mapa físico.

## Adicionar um asset

Assets gerados ficam em `res/assets/generated/`. Os ícones de armas limpos são carregados por `AssetCatalog`, que possui fallback silencioso quando um asset ainda não foi gerado. Para adicionar um ícone, inclua o arquivo PNG, associe seu caminho à `WeaponType` e use `AssetCatalog.drawWeaponIcon(...)` em HUDs ou telas modais.

A direção visual atual combina pixel art sci-fi de alta legibilidade, metal escuro, ciano elétrico, dourado, violeta e verde tóxico. A referência completa está em `res/assets/generated/visual_target.png`; o atlas de companions, o atlas de inimigos, o portal e o atlas de terrenos servem como guias para futuras integrações.
