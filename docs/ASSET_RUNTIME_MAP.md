# Mapa de uso dos assets gerados

Os assets em `res/assets/generated/` não formam um pacote único obrigatório. Alguns são carregados diretamente pelo runtime; outros são referências de direção de arte ou saídas prontas para uma integração futura.

| Diretório ou asset | Uso atual | Consumidor principal |
|---|---|---|
| `enemies/scout_ref.png` e `enemies/enemy_*.png` | Sprites normalizados dos inimigos do shooter e da Charneca. | `AssetCatalog.enemySprite`, `Enemy` e `RpgCombatEnemy`. |
| `enemies/enemy_mire_hound.png`, `enemy_bog_oracle.png`, `enemy_mire_brute.png` | Encontros liberados após a patrulha de Sena. | `ClassicRpgMode` e `RpgContentEnemyProfile`. |
| `enemies/enemy_mist_sovereign.png` | Sprite do chefe opcional Soberano da Bruma. | `ClassicRpgMode` e `RpgCombatEnemy`. |
| `abilities/mist_sovereign_nucleo_da_bruma.png` | Ícone exibido no halo da pulsação especial do Soberano. | `AssetCatalog.contentAbilityIcon` e `RpgCombatEnemy`. |
| `tiles/brumafolha_*.png` | Variações determinísticas de grama, estrada e ruínas do Vale. | `RpgMap`. |
| `items/elixir_de_bruma.png` e `rpg_weapons/lamina_de_bruma.png` | Bolsa de Viagem do RPG. | `ClassicRpgMode` e métodos de ícone do `AssetCatalog`. |
| `weapons/*.png` e `companions/*.png` | Armas e companheiros do loop shooter/regional. | `AssetCatalog`, HUD e renderização de entidades. |
| `world/*.png` | Portal de dungeon e textura regional. | `AssetCatalog` e cenas de exploração. |
| `terrain_sources/*` e `visual_target.png` | Referências de direção de arte; não são carregadas durante a partida. | Content Studio e documentação visual. |

## Habilidade do Soberano

O botão **Exportar habilidade: Núcleo da Bruma** na aba **Inimigos** do Content Studio grava o PNG e seu manifesto `boss_ability`. Quando a patrulha de Sena está concluída, o Soberano aparece no leste da Charneca e recarrega esse manifesto para definir dano, alcance e cooldown da pulsação.
