# Contrato de Arte — First Game RPG

> **Objetivo:** substituir os assets provisórios por um pacote de pixel art coeso, legível em celular e compatível com a importação do Asset Coach.

## 1. Diagnóstico da captura atual

A captura confirma que a correção deve combinar **arte, composição e interface**. Os tiles de grama, terra e água usam cores muito saturadas e repetição curta; os inimigos circulares não têm silhueta, anatomia nem linguagem de material compatível com o cenário; o chefe está grande sem ter detalhes que justifiquem a escala. Como consequência, mapa, criaturas e HUD parecem pertencer a protótipos diferentes.

| Problema visto | Consequência no jogo | Correção necessária |
|---|---|---|
| Gramado verde muito saturado e repetido | O chão compete com personagens e tiros. | Paleta mais contida, três a quatro variações de tile e transições desenhadas. |
| Inimigos como círculos lisos | Não há leitura de ameaça, direção ou tipo de criatura. | Silhuetas de topo 3/4, contorno escuro e animações com peso. |
| Chefe muito maior que os demais sem anatomia | A escala parece acidental, não épica. | Sprite próprio de chefe em canvas maior e com zonas visuais claras. |
| HUD e diálogo cobrem grande parte do campo | O jogador perde o combate enquanto lê. | Reorganização de interface depois da troca de arte: diálogo compacto, retrato e hierarquia de combate. |
| Faixas pretas no dispositivo | A composição do mapa perde área útil. | Ajuste separado de viewport e safe area no APK; **não é um problema de asset**. |

O novo pacote deve adotar **pixel art de fantasia sombria, vista superior em 3/4**, com leitura limpa em telas pequenas. Não misture cartoon vetorial, gradiente suave, pintura digital, 3D e pixel art no mesmo lote.

## 2. Direção visual obrigatória

| Decisão | Contrato |
|---|---|
| Câmera | Top-down em 3/4, com personagens vistos levemente de cima; nunca frontal, lateral pura ou isométrica. |
| Linguagem | Pixel art nativa, bordas duras, clusters de pixels deliberados e contorno seletivo em azul-preto. |
| Resolução base | Grid lógico de **32 × 32 px**. Personagem, NPC, inimigo comum, item e tile devem caber em um canvas de 32 px. |
| Chefe | Canvas de **96 × 96 px** (3 × 3 tiles), com pivô no centro inferior. Não reduza o chefe a 32 px. |
| Paleta | 12–18 cores por personagem; 16–24 cores por tile set; sombras em azul-violeta escuro, não preto puro. |
| Contraste | Silhueta do personagem deve ter valor mais claro ou mais escuro que o chão imediatamente atrás dela. |
| Transparência | PNG RGBA verdadeiro, sem fundo branco, halo verde, sombra difusa ou borda semitransparente colorida. |
| Escala | Sem suavização. Qualquer redimensionamento deve ser *nearest-neighbor* em múltiplos inteiros. |

### Paleta de referência

Use esta paleta como direção, não como obrigação de usar todos os tons em todo sprite.

| Função | Cor sugerida | Uso |
|---|---|---|
| Contorno | `#17212B` | Separar silhuetas e detalhes de maior contraste. |
| Sombra fria | `#27354B` | Roupa escura, pedras, sombra de personagem. |
| Verde de mata | `#356344` e `#5E8951` | Gramado com contraste contido. |
| Terra | `#6F4931` e `#A7754B` | Caminhos e solo exposto. |
| Ouro de foco | `#D5B46C` | Metal, missão, relíquia e pequenos destaques. |
| Ameaça | `#B84A57` | Olhos, magia hostil e ataques de inimigo. |
| Magia bruma | `#6E78B8` | Efeitos arcanos; usar pontualmente. |

### Contrato Bruma & Fortaleza — alinhamento com a capa do portal

A capa do portal é a referência de atmosfera do RPG. Ela não pede que cada tile copie uma pintura detalhada; pede que todos os elementos compartilhem a mesma **hierarquia de valor e material**: fundo frio e profundo, pedra azul-acinzentada, vegetação escurecida, névoa desaturada e pequenos focos de ouro-lanterna. O ouro deve conduzir o olhar para porta, relíquia, missão, retrato ou interação; nunca cobrir o mapa inteiro.

| Camada visual | Material e faixa cromática | Regra de uso no APK |
|---|---|---|
| Céu, distância e névoa | `#0A1222`, `#17212B`, `#2B6B73` em baixa opacidade | Névoa fria vem depois do terreno e antes dos props; ela reduz saturação sem apagar a silhueta. |
| Pedra, muralha e ruína | `#27354B`, `#40516B`, `#71819A` | Prefira blocos de pedra, frisos e rachaduras a superfícies lisas ou azul-ciano. |
| Mata e solo | `#2D493E`, `#3B5D49`, `#5E493A` | Verde e terra ficam abaixo do brilho de personagens, perigos e objetivos. Evite grama neon e marrom laranja. |
| Ouro-lanterna | `#D7B45F`, `#F5D98B` | Use em contornos finos, fogo, portal, botão ativo, relíquia e textos prioritários; não como preenchimento constante. |
| Ameaça | `#A94A61`, `#E45F75` | Reserve para barra de vida hostil, olhar de criatura, impacto e feitiço inimigo. |
| Interface | obsidiana `#0A1222` com borda ouro-lanterna e texto marfim `#F1E9D0` | Painéis precisam revelar o jogo, não competir com sprites e mapa. |

> Todo novo asset deve passar na pergunta: **“parece existir no caminho de pedra e névoa que leva à Fortaleza?”** Se a resposta depender de cor neon, gradiente suave, borda preta pura ou volume cartunesco, o asset deve voltar à revisão.

#### Luz e composição

O mundo usa iluminação fria e difusa, com uma fonte secundária quente local — lanterna, portal, relíquia ou fogo. Não desenhe sombras pretas chapadas nem brilhos com desfoque. A luz quente ocupa poucos pixels e deve servir como ponto de navegação ou de narrativa. Personagens e inimigos precisam ter contorno azul-marinho seletivo e um valor diferente do tile que ocupam, mesmo sob a camada de névoa.

#### Prompt aditivo para os próximos assets

Acrescente o trecho abaixo a cada prompt de sprite, tile ou prop. Ele complementa o Prompt Mestre da seção 5 sem mudar grade, tamanho, pivô ou convenção de nome.

```text
Art direction: match a moody dark-fantasy fortress archive. Use cold blue-gray stone,
desaturated forest greens, deep navy shadows, thin lantern-gold focal accents and
low, misty contrast. The world feels ancient, damp and guarded — never cheerful,
neon, glossy, cartoon-vector or brightly saturated. Keep hard pixel clusters and a
single subtle lantern-style light direction; no smooth gradients, bloom or blur.
```

## 3. Formato de entrega que o Asset Coach aceita

O formato recomendado é uma spritesheet com **três colunas e uma linha**, sem espaço entre células. O Asset Coach dividirá a folha da esquerda para a direita e nomeará os frames de `0` a `2`.

| Tipo | Arquivo de entrada | Tamanho do arquivo | Células | Resultado após dividir |
|---|---|---:|---|---|
| Caminhada / ataque de personagem | `<id>_<ação>_<direção>_sheet.png` | `96 × 32 px` | 3 × `32 × 32` | `<id>_<ação>_<direção>_0.png` até `_2.png` |
| Tile set de mapa | `<bioma>_tileset.png` | Múltiplos exatos de 32 px | Grade `32 × 32` | Um PNG por célula, em ordem de leitura. |
| Prop comum | `<id>.png` | `32 × 32 px` | 1 | Um PNG com alfa. |
| Prop grande | `<id>.png` | `64 × 64 px` | 1 | Um PNG com alfa, sujeito a posicionamento manual. |
| Chefe | `<id>_<ação>_<direção>_sheet.png` | `288 × 96 px` | 3 × `96 × 96` | Três frames grandes; requeremos o pivô inferior consistente. |

> Não coloque margens, guias, números, texto, molduras ou espaços vazios entre os três frames. A folha deve começar na coordenada `(0, 0)` e terminar exatamente no limite da terceira célula.

### Convenção de nomes obrigatória

```text
hero_walk_down_sheet.png
hero_walk_left_sheet.png
hero_walk_right_sheet.png
hero_walk_up_sheet.png
hero_attack_down_sheet.png
hero_attack_left_sheet.png
hero_attack_right_sheet.png
hero_attack_up_sheet.png
```

Os IDs já aceitos pelo fluxo atual são `hero`, `npc_commandant`, `npc_healer` e `npc_cartographer`. Para inimigos e chefe, use um ID curto em *snake_case*, por exemplo `enemy_moss_wolf` ou `boss_mist_titan`. Não use acentos, espaços, parênteses, versão no nome ou nomes genéricos como `sprite_final.png`.

## 4. Lote 1 que deve ser enviado primeiro

Não envie um pacote enorme de uma vez. O primeiro lote deve provar que a nova direção de arte funciona dentro do mapa antes de criarmos todas as variações.

| Prioridade | Asset | Quantidade | Entrega |
|---:|---|---:|---|
| 1 | Herói jogável | 8 spritesheets | `walk` e `attack` em `down`, `left`, `right`, `up`. |
| 2 | Inimigo comum principal | 8 spritesheets | Mesmo contrato do herói, com silhueta distinta. |
| 3 | Titã da Bruma | 8 spritesheets de chefe | `288 × 96 px`; caminhada curta e ataque de impacto. |
| 4 | Tileset da Clareira | 1 sheet de `256 × 256 px` | Grade 8 × 8: grama, terra, bordas, água e transições. |
| 5 | Ponte de madeira | 3 PNGs | Início, meio e fim; cada peça `32 × 32 px`. |
| 6 | Retrato da Comandante Ava | 1 PNG | `96 × 96 px`, transparência; usado no diálogo compacto. |

Depois de aprovar esse lote no APK, seguimos para os três NPCs, inimigos secundários, props, magia, itens e telas de inventário.

## 5. Prompts de produção

Estes prompts definem a estética. Para um resultado diretamente importável, gere ou desenhe os frames no Aseprite, Krita ou Piskel e exporte-os em PNG RGBA. Geradores de imagem podem servir como referência de conceito, mas não substituem a revisão de grade, pivô e contagem de frames.

### Prompt mestre — aplicar a todos os sprites

```text
Create a production-ready top-down 3/4 pixel-art game sprite for a dark fantasy RPG.
Camera: consistent 3/4 top-down view, readable from a mobile screen.
Style: true hand-crafted pixel art with intentional pixel clusters, hard edges, selective dark navy outline, no vector shapes, no soft airbrush, no 3D render.
Palette: restrained forest green, earth brown, cold navy shadow, lantern gold accents, muted crimson only for danger.
Silhouette: immediately readable at 32 pixels; feet anchored at the bottom-center pivot; keep a clean transparent background.
Constraints: no text, no UI, no frame borders, no drop shadow, no glow halo, no white background, no watermark, no anti-aliasing, no perspective other than 3/4 top-down.
```

### Herói — folha de caminhada

```text
Create a 96 by 32 pixel PNG sprite sheet: exactly 3 columns and 1 row, each frame exactly 32 by 32 pixels, no gutters.
Subject: a brave young field explorer hero for a dark fantasy RPG, practical moss-green hooded coat, leather boots, small lantern-gold brooch, short sword sheathed, warm but determined face.
Animation: three-frame walking cycle facing [DOWN | LEFT | RIGHT | UP]; frame 0 contact pose, frame 1 passing pose, frame 2 opposite contact pose. Keep the body mass, feet pivot, proportions, light source and costume identical across all frames.
[PROMPT MESTRE]
```

### Herói — folha de ataque

```text
Create a 96 by 32 pixel PNG sprite sheet: exactly 3 columns and 1 row, each frame exactly 32 by 32 pixels, no gutters.
Subject: the same young field explorer hero with moss-green hooded coat and short sword.
Animation: three-frame sword attack facing [DOWN | LEFT | RIGHT | UP]; frame 0 anticipation, frame 1 readable strike, frame 2 recovery. The weapon arc must stay inside the 32 by 32 cell and must not include a soft glow trail.
[PROMPT MESTRE]
```

### Inimigo comum — Lobo de Musgo

```text
Create a 96 by 32 pixel PNG sprite sheet: exactly 3 columns and 1 row, each frame exactly 32 by 32 pixels, no gutters.
Subject: moss wolf corrupted by mist, low quadruped silhouette, bark-like fur, moss patches, two dim crimson eyes, dark navy outline. It must look dangerous but distinct from the hero and from the grass beneath it.
Animation: [WALK | ATTACK] facing [DOWN | LEFT | RIGHT | UP], three clear poses with the same bottom-center foot pivot.
[PROMPT MESTRE]
```

### Chefe — Titã da Bruma

```text
Create a 288 by 96 pixel PNG sprite sheet: exactly 3 columns and 1 row, each frame exactly 96 by 96 pixels, no gutters.
Subject: the Mist Titan, an ancient stone-and-root guardian, broad asymmetrical shoulders, cracked teal runes, one lantern-gold core in the chest, large hands, weighty stance. It is a boss seen from 3/4 top-down, not a round blob.
Animation: [WALK | ATTACK] facing [DOWN | LEFT | RIGHT | UP], three poses with a consistent bottom-center pivot. Frame 1 of attack is a ground slam; debris and magic remain contained inside the frame.
Style: dense but readable pixel art at 96 pixels; no photorealism, no smooth gradients, no background.
```

### Tileset — Clareira da Bruma

```text
Create a 256 by 256 pixel PNG tile set for a dark fantasy RPG, exactly 8 columns by 8 rows, each tile exactly 32 by 32 pixels, no gutters and no labels.
Camera: top-down 3/4-compatible ground tiles. Palette: restrained moss green, fern green, earth brown, cold stone gray and shallow blue water; avoid neon green and overly saturated cyan.
Required tile groups in reading order: grass variations, dirt path variations, grass-to-dirt edges and corners, shallow-water variations, water-to-grass edges and corners, dark stone, mossy stone, decorative roots and small flowers. Every edge tile must connect cleanly to its neighbor.
Constraints: no characters, no creatures, no text, no large repeating texture, no lighting baked across multiple cells, no soft blur.
```

### Retrato de diálogo — Ava, Comandante

```text
Create a 96 by 96 pixel PNG portrait with transparent background for a dark fantasy RPG dialogue panel.
Subject: Commander Ava, adult scout leader, short dark hair, practical navy and forest-green field armor, lantern-gold clasp, composed but urgent expression, three-quarter bust view facing inward.
Style: polished pixel art matching the 32-pixel world palette; clean silhouette; no panel, no name text, no speech bubble, no background, no watermark.
```

## 6. Critérios de aceitação antes da importação

O Asset Coach e a revisão humana devem recusar ou devolver um arquivo se algum requisito rígido falhar.

| Regra | Aprovar quando | Recusar quando |
|---|---|---|
| Formato | PNG RGBA, íntegro e legível. | JPG, WebP, PNG sem alfa ou arquivo corrompido. |
| Grade | As dimensões são divisíveis pelo tamanho de célula declarado. | Sobra pixel, gutter acidental ou borda cortada. |
| Nomes | ID, ação, direção e sufixo `_sheet` seguem o padrão. | Espaços, acentos, `final`, `novo`, `v2` ou índice manual fora do padrão. |
| Transparência | Fundo realmente transparente e borda limpa. | Fundo branco/colorido, halo, sombra difusa ou fringe. |
| Perspectiva | Top-down 3/4 coerente com a direção selecionada. | Frontal, lateral pura, isométrico ou mudança de câmera entre frames. |
| Pivô | Pés ou base da criatura ficam no mesmo ponto inferior em todos os frames. | Personagem salta, desliza ou muda de tamanho sem intenção de gameplay. |
| Leitura | A silhueta é reconhecível em escala nativa. | Monstro circular genérico, contorno fraco ou cores confundidas com o chão. |
| Estilo | Pixel art com paleta e luz coerentes. | Vetor, 3D, pintura suave, gradiente ou mistura de estilos. |

## 7. Como enviar

Envie os PNGs individualmente ou em um único `.zip`, sem alterar os nomes. A estrutura abaixo é preferida:

```text
first-game-rpg-assets-lote-1/
  characters/
    hero/
      hero_walk_down_sheet.png
      hero_walk_left_sheet.png
      ...
    enemy_moss_wolf/
      enemy_moss_wolf_walk_down_sheet.png
      ...
    boss_mist_titan/
      boss_mist_titan_attack_down_sheet.png
      ...
  tiles/
    clearing_tileset.png
  props/
    bridge_start.png
    bridge_middle.png
    bridge_end.png
  portraits/
    npc_commandant_ava_portrait.png
```

Ao receber o lote, o processo será: **inspecionar → validar grade/alfa/nome → dividir spritesheets → revisar animação → normalizar sem alterar a fonte → integrar no mapa → gerar APK para sua aprovação**.
