# Pipeline automático de assets do jogo

Os arquivos enviados pelo usuário são tratados como **assets próprios do projeto**. O fluxo preserva os originais em `res/assets/incoming/user_uploads/` e gera cópias prontas para o runtime em `res/assets/generated/`.

## Onde cada asset é usado

| Grupo | Entrada | Saída | Uso no jogo |
|---|---|---|---|
| Ícones de armas | `*_clean.png` | `generated/weapons/*_clean.png` | `AssetCatalog.weaponIcon`, HUD, seleção e telas de build. |
| Efeitos de tiro | `blaster.png`, `ion_rifle.png`, `scatter_cannon.png`, `fusion_lance.png`, `void_mortar.png` | `generated/effects/*_shot.png` | Biblioteca de efeitos com rastro/brilho. O runtime atual ainda desenha o projétil por código; por isso estes arquivos não são desenhados automaticamente durante a partida. |
| Companheiros | `companion_set_clean.webp` | `generated/companions/companion_set_clean.png` | Atlas de 3 colunas lido por `AssetCatalog.companionSprite`: `SCOUT`, `SHIELD_BOT` e `FAIRY`. |
| Inimigos | `enemy_set_clean.webp` | `generated/enemies/enemy_set_clean.png` | Atlas de 3×2 usado como fallback por `AssetCatalog.enemySprite` e por entidades de combate. |
| Scout | `scout_ref.png` | `generated/enemies/scout_ref.png` | Sprite individual do `SCOUT` e fallback de variantes que ainda não têm arte própria. |
| Portal | `dungeon_portal.png` | `generated/world/dungeon_portal.png` | `AssetCatalog.dungeonPortal()` já carrega o arquivo; a cena ainda precisa chamar esse método em um renderizador de portal ativo. |
| Referências | `visual_target.webp`, pranchas com efeitos | `generated/references/*` | Direção de arte e consulta; não são carregadas durante a partida. |

> O importador não substitui a lógica de combate. Ele prepara arquivos e mantém nomes compatíveis com o catálogo existente; colisão, dano, cadência e IA continuam sendo definidos no código Java.

## Como o recorte funciona

O script `tools/import_user_assets.py` converte tudo para RGBA, remove apenas regiões de fundo conectadas às bordas e preserva brilhos internos protegidos pelo contorno do pixel art. Em seguida, recorta o envelope alfa dos sprites individuais, adicionando uma pequena margem. Para atlases, ele **não recorta a prancha inteira**: mantém a dimensão e a grade originais para que os índices usados pelo `AssetCatalog` continuem estáveis.

Além do atlas de runtime, o script gera cópias individuais em `generated/atlas_cells/companions/` e `generated/atlas_cells/enemies/`. Essas células servem para inspeção, testes e futura configuração individual no Content Studio.

## Execução automática

Na raiz do projeto, execute:

```bash
./gradlew importUserAssets
```

O comando processa todos os arquivos encontrados em `res/assets/incoming/user_uploads/` e atualiza `res/assets/generated/user_asset_manifest.json`. O manifesto registra a origem, a categoria, os arquivos gerados e o consumidor esperado.

Também é possível executar diretamente:

```bash
python3 tools/import_user_assets.py
```

O botão **Importar assets do projeto** no Content Studio executa o mesmo pipeline. Assim, o fluxo recomendado para uma nova arte é: colocar o arquivo na pasta de entrada, abrir o Content Studio, importar o pacote e executar o jogo/testes.

## Convenção para novos arquivos

Use nomes descritivos e estáveis. Para armas, mantenha `blaster`, `ion_rifle`, `scatter_cannon`, `fusion_lance` e `void_mortar`. Para novos assets, prefira letras minúsculas, números, hífen ou sublinhado. O arquivo original deve permanecer em `res/assets/incoming/user_uploads/`; os arquivos em `generated/` são saídas reproduzíveis e podem ser recriados a qualquer momento.
