# Assets, áudio e localização

## Sprites de companions e inimigos

Os atlas transparentes ficam em `res/assets/generated/companions/companion_set_clean.png` e `res/assets/generated/enemies/enemy_set_clean.png`. O `AssetCatalog` carrega os atlas uma vez, recorta a célula da variante, remove a margem transparente e mantém o resultado em cache.

`Companion.render(...)` chama `AssetCatalog.companionSprite(type)`. `Enemy.render(...)` chama `AssetCatalog.enemySprite(variant)`. Se o arquivo faltar ou não puder ser lido, o jogo usa o desenho procedural/sprite legado. A arte não altera hitbox, dano, vida, elite ou marcadores táticos.

Para substituir um sprite, mantenha a mesma célula/ordem do atlas ou altere apenas o mapeamento no `AssetCatalog`. Não edite as entidades para trocar arquivos.

## Voz por personagem

As vozes seguem o caminho:

```text
res/audio/voices/{locale}/{speaker_slug}/line_{index}.wav
```

O `DialogueManager` localiza as falas e chama `VoiceManager.playDialogueLine(...)` ao abrir e avançar uma linha. `VoiceManager.registerLine(...)` permite substituir uma fala em runtime sem alterar a missão. Ao fechar ou interromper o diálogo, a voz atual é parada.

A amostra inicial usa `pt-BR` e `en-US`, com voz consistente por personagem. Para completar uma campanha, gere cada linha com o mesmo personagem/voz e o mesmo índice usado pelo diálogo. As instruções de direção do TTS devem ficar em inglês antes de `:`; o texto falado fica no idioma final.

## Efeitos sonoros

Os efeitos curtos continuam centralizados no `SoundManager.Event`. O pacote novo inclui magia, coleta de XP, transição de sobrevivência, abertura de dungeon e quatro famílias de armas. Para substituir um arquivo:

```java
SoundManager.registerFile(
    SoundManager.Event.MAGIC_CAST,
    "/sounds/custom/magic_cast.wav"
);
```

Os eventos respeitam `OptionsConfig.isSoundEnabled()` e o volume configurado. O script `tools/generate_sfx_assets.py` gera efeitos determinísticos de referência; assets profissionais podem substituir os mesmos nomes sem mudança de código.

## Localização

Os catálogos ficam em `res/i18n/messages_pt_BR.properties` e `res/i18n/messages_en_US.properties`. Use `Localization.tr(...)` para chaves obrigatórias e `Localization.trOr(...)` quando o texto original precisa servir de fallback. O menu Opções possui a seleção de idioma, e o código escolhido é salvo na sessão para sobreviver ao carregamento.

A localização de diálogo usa `dialogue.{speaker_slug}.{lineIndex}`. Uma chave ausente não esvazia a fala: mantém o texto original e pode ser completada posteriormente.
