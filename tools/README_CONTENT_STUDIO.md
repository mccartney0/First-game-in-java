# Content Studio

O **Content Studio** é o aplicativo desktop de produção do jogo. Ele exporta mapas RPG, mundo aberto, tiles 32×32, sprites de inimigos com transparência e manifestos de inspeção.

No Windows, dê duplo clique em `tools\open_content_studio.bat`. Como alternativa, no diretório do projeto execute:

```bat
gradlew.bat runContentStudio
```

No Linux ou macOS, execute `./gradlew runContentStudio`.

As exportações são enviadas aos diretórios utilizados pelo jogo. Mapas regionais ficam em `bin/large_rpg_maps/`, mundos abertos em `bin/open_world_maps/`, tiles em `res/assets/generated/tiles/` e sprites em `res/assets/generated/enemies/`. Consumíveis do RPG ficam em `res/assets/generated/items/`, enquanto armas melee do RPG ficam em `res/assets/generated/rpg_weapons/`. Consulte `docs/CONTENT_STUDIO_CONTRACT.md` para o contrato de compatibilidade.

As referências visuais de direção de arte do Vale de Brumafolha ficam em `res/assets/generated/terrain_sources/`. Elas orientam as variantes 32×32 de grama, estrada e ruínas; os arquivos de runtime permanecem em `res/assets/generated/tiles/` com os nomes definidos em `docs/VALE_BRUMAFOLHA_CONTENT_STUDIO.md`.

Na aba **Referências**, a galeria mostra grama, estrada e ruínas lado a lado. O botão **Gerar pacote runtime 32×32** escreve automaticamente as quatro variantes de grama, três de estrada e três de ruínas nos nomes que o Vale consome.

Para regenerar o mesmo pacote sem abrir a interface, execute:

```bat
gradlew.bat generateBrumafolhaTerrain
```

## Inimigos da Charneca da Bruma

Na aba **Inimigos**, os papéis **Cão de Turfa**, **Oráculo do Brejo** e **Bruto da Charneca** oferecem comportamentos de investida, maldição à distância e confronto fortificado. É possível ajustar vida, dano, velocidade, perfil de IA e paleta antes de exportar o sprite transparente e seu manifesto JSON.

O botão **Gerar pacote da Charneca** cria as três variações de runtime. Elas são liberadas no RPG quando a patrulha da batedora Sena é concluída; ao iniciar uma jornada, o jogo lê novamente os manifestos para aplicar os valores configurados.

Para regenerar o pacote sem abrir a interface, execute:

```bat
gradlew.bat generateOutlandEnemyPack
```

## Demonstração de chefe no canvas

Na aba **Inimigos**, o botão **Gerar demo: Soberano da Bruma** cria um chefe completo no canvas e exibe o PNG recém-exportado no painel de prévia. O perfil demonstrativo usa a silhueta de soberano coroado, 48 de vida base, 12 de dano, velocidade `0.55` e o comportamento **Guardião — resiste e se regenera**. O manifesto registra também `"boss": true`, permitindo inspeção e validação automatizada do papel de chefe.

Para repetir a demonstração sem abrir a interface gráfica, execute:

```bat
gradlew.bat generateMistSovereignBoss
```

O botão **Exportar habilidade: Núcleo da Bruma** cria o ícone e o manifesto da pulsação especial do Soberano. Os campos padrão são 14 de dano, 180 ticks de cooldown e alcance de 168 pixels; o runtime RPG lê esse manifesto quando o chefe é criado.

```bat
gradlew.bat generateMistSovereignAbility
```

## Itens e armas do RPG

Na aba **Itens RPG**, escolha entre **Consumível** e **Arma**. Todo export gera um PNG 32×32 transparente e um manifesto JSON ao lado. Para consumíveis, configure os valores de vida, mana e fôlego restaurados. Para armas, configure o estilo, o bônus de dano, o custo de fôlego e a raridade.

O pacote inicial gera `elixir_de_bruma` e `lamina_de_bruma`, já reconhecidos automaticamente pela Bolsa de Viagem do RPG. No jogo, abra a bolsa com **I**, navegue com **W/S** e use ou equipe com **Enter**.

Para gerar os dois exemplos sem abrir a interface, execute:

```bat
gradlew.bat generateDefaultRpgContent
```
