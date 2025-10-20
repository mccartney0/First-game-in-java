# Guia de rotas e funcionalidades

Este documento detalha o fluxo das principais rotas de execução do jogo, descrevendo como os módulos interagem e quais funcionalidades cada parte do código entrega.

## 1. Inicialização e ciclo principal

- `com.traduvertgames.main.Game` é instanciado a partir do método `main`, que configura a janela (`JFrame`), o canvas de renderização e inicializa listas compartilhadas de entidades, inimigos e projéteis.【F:src/com/traduvertgames/main/Game.java†L31-L93】
- Após configurar sprites e carregar `World`, o jogo cria a thread principal com `start()` e entra em `run()`, que mantém o loop fixo de atualização/renderização a 60 FPS.【F:src/com/traduvertgames/main/Game.java†L94-L206】【F:src/com/traduvertgames/main/Game.java†L207-L312】

## 2. Estados do jogo e fluxo de telas

- O atributo `Game.gameState` define as rotas possíveis: `MENU`, `NORMAL` e `GAMEOVER`. Cada estado ativa lógicas diferentes em `update()` e `render()`, como interação com o menu, execução do jogo ou exibição da tela de derrota.【F:src/com/traduvertgames/main/Game.java†L53-L206】
- No estado `MENU`, a classe `Menu` controla navegação (`up`, `down`, `enter`), habilita música ambiente, abre o submenu de opções e decide entre iniciar, continuar ou carregar o jogo salvo, redirecionando o fluxo para `NORMAL` ou finalizando a aplicação.【F:src/com/traduvertgames/main/Menu.java†L23-L230】
- O submenu "Opções" permite ativar/desativar a música (`OptionsConfig.toggleMusic`) e ciclar a dificuldade; `Game.applyDifficultyToPlayerStats()` recalcula vida, mana e munição máximas enquanto o dano sofrido é escalado via `Game.getDamageTakenMultiplier()`.【F:src/com/traduvertgames/main/Menu.java†L93-L230】【F:src/com/traduvertgames/main/OptionsConfig.java†L1-L92】【F:src/com/traduvertgames/main/Game.java†L200-L360】【F:src/com/traduvertgames/entities/BulletShoot.java†L1-L60】【F:src/com/traduvertgames/entities/Enemy.java†L320-L420】
- Em `GAMEOVER`, o loop aguarda `Enter` para reiniciar o estado `NORMAL`, recarregando o mundo conforme o progresso salvo (se existir `save.txt`).【F:src/com/traduvertgames/main/Game.java†L124-L206】

## 3. Progressão de níveis

- A cada atualização, quando `Game.enemies` fica vazio, `World.restartGame` é chamado para carregar o próximo mapa (`levelN.png`). Após concluir os quatro níveis padrão, `levelPlus` aumenta e atributos máximos do jogador são elevados para rodadas sucessivas.【F:src/com/traduvertgames/main/Game.java†L160-L187】
- O arquivo do mundo define a distribuição de tiles, inimigos e itens; `World.java` traduz pixels do PNG em instâncias de `Tile`, `Enemy`, `LifePack`, `Weapon` e projéteis iniciais.【F:src/com/traduvertgames/world/World.java†L20-L145】

## 4. Sistema de entidades e combate

- `Player` cuida da movimentação com checagem de colisão (`World.isFree`), salto simulado, recolhimento de itens e gerenciamento de vida/mana/munição. O disparo pode ser acionado por teclado ou mouse, gerando instâncias de `BulletShoot` ou `Bullet` adicionadas às listas do jogo.【F:src/com/traduvertgames/entities/Player.java†L19-L224】
- `Enemy` alterna entre patrulhas locais, perseguição com recálculo dinâmico de caminhos e ataques à distância quando há linha de visão, além de ainda poder causar dano corpo a corpo e sinalizar feedback visual (`damage`).【F:src/com/traduvertgames/entities/Enemy.java†L28-L369】
- Ambas as subclasses de projéteis atualizam posicionamento, detectam colisões com paredes e inimigos e removem-se do jogo quando saem da área válida.【F:src/com/traduvertgames/entities/Bullet.java†L5-L12】【F:src/com/traduvertgames/entities/BulletShoot.java†L10-L41】

## 5. Salvamento e carregamento

- O menu oferece a opção "carregar jogo" quando `save.txt` existe. O método `Menu.loadGame` decodifica os valores com um deslocamento definido (encode) e `applySave` restaura vida, mana, contagem de inimigos e fase atual antes de reabrir o mundo correto.【F:src/com/traduvertgames/main/Menu.java†L58-L158】
- Durante o estado `NORMAL`, pressionar `T` define `Game.saveGame = true`. A rotina no `update()` registra atributos atuais com `Menu.saveGame`, codificando números e gravando no arquivo de texto.【F:src/com/traduvertgames/main/Game.java†L87-L154】【F:src/com/traduvertgames/main/Menu.java†L126-L158】

## 6. Interface e áudio

- A classe `UI` desenha barras de vida, mana e munição, além de textos informativos como quantidade de inimigos vivos e mortos, e mensagens de Game Over. Essa renderização ocorre após desenhar o mundo e entidades para manter os indicadores sobrepostos.【F:src/com/traduvertgames/main/Game.java†L229-L288】
- Os efeitos e a música são carregados por `Sound.java`, que expõe instâncias estáticas para cada áudio (`music`, `hurtEffect`, etc.), permitindo iniciar loops ou executar efeitos sob demanda no menu e em combate.【F:src/com/traduvertgames/main/Sound.java†L1-L120】

## 7. Recursos e assets

- Sprites e sons residem em `res/`, com `Spritesheet` responsável por cortar imagens para as animações do jogador, inimigos e itens.【F:src/com/traduvertgames/main/Game.java†L63-L114】【F:src/com/traduvertgames/graficos/Spritesheet.java†L7-L82】
- Os mapas (`level1.png` a `level4.png`) definem a "rota" espacial de cada fase, orientando tanto a geração de tiles (`FloorTile`, `WallTile`) quanto os nós de navegação usados pela IA de A*.【F:src/com/traduvertgames/world/World.java†L20-L145】【F:src/com/traduvertgames/world/AStar.java†L9-L140】

