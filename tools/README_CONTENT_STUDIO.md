# Content Studio

O **Content Studio** é o aplicativo desktop de produção do jogo. Ele exporta mapas RPG, mundo aberto, tiles 32×32, sprites de inimigos com transparência e manifestos de inspeção.

No Windows, dê duplo clique em `tools\open_content_studio.bat`. Como alternativa, no diretório do projeto execute:

```bat
gradlew.bat runContentStudio
```

No Linux ou macOS, execute `./gradlew runContentStudio`.

As exportações são enviadas aos diretórios utilizados pelo jogo. Mapas regionais ficam em `bin/large_rpg_maps/`, mundos abertos em `bin/open_world_maps/`, tiles em `res/assets/generated/tiles/` e sprites em `res/assets/generated/enemies/`. Consulte `docs/CONTENT_STUDIO_CONTRACT.md` para o contrato de compatibilidade.

As referências visuais de direção de arte do Vale de Brumafolha ficam em `res/assets/generated/terrain_sources/`. Elas orientam as variantes 32×32 de grama, estrada e ruínas; os arquivos de runtime permanecem em `res/assets/generated/tiles/` com os nomes definidos em `docs/VALE_BRUMAFOLHA_CONTENT_STUDIO.md`.
