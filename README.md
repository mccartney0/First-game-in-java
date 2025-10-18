# First Game in Java

First Game in Java é um jogo de tiro top-down simples escrito em Java puro. Ele foi criado originalmente como um projeto de aprendizagem e demonstra um loop de jogo básico, renderização de sprites, detecção de colisões e uma IA simples de pathfinding utilizando a implementação de A* incluída.

## Estrutura do projeto

```
src/
└── com/traduvertgames
    ├── entities/         // Jogador, inimigos, projéteis, itens
    ├── graficos/         // Carregador de sprite sheet e HUD da interface
    ├── main/             // Ponto de entrada do jogo, menu e utilitários de áudio
    └── world/            // Tiles, geração de mundos e pathfinding
```

Os recursos, como sprites e áudio, estão localizados no diretório `res/`, enquanto o wrapper do Gradle está incluído para facilitar a compilação e a execução do projeto.

## Visão geral do gameplay

- **Estados do jogo** – O jogo alterna entre os estados `MENU`, `NORMAL` e `GAMEOVER`. O menu principal permite iniciar ou carregar partidas, o estado normal controla o loop de jogo e, ao zerar a vida do jogador, o estado de game over exibe a tela de reinício.【F:src/com/traduvertgames/main/Game.java†L53-L154】【F:src/com/traduvertgames/main/Menu.java†L21-L103】
- **Loop principal** – O método `run` mantém o jogo atualizado a 60 ticks por segundo, chamando `update()` para lógica e `render()` para desenhar o cenário, entidades, UI e mensagens contextuais.【F:src/com/traduvertgames/main/Game.java†L207-L312】
- **Progressão de fases** – Ao eliminar todos os inimigos do nível atual, um novo mapa (`level1.png` a `level4.png`) é carregado. Após o último nível, o jogo aumenta os atributos máximos do jogador para partidas estendidas.【F:src/com/traduvertgames/main/Game.java†L160-L187】

## Controles e interações

- **Movimentação:** `W`, `A`, `S`, `D` ou setas direcionais.
- **Pulo:** `Espaço` ativa o salto 2D simulado do personagem.【F:src/com/traduvertgames/main/Game.java†L323-L363】【F:src/com/traduvertgames/entities/Player.java†L43-L118】
- **Ataque:** `X` dispara projéteis enquanto houver munição e mana; o mouse também pode ser usado para mirar e atirar.【F:src/com/traduvertgames/main/Game.java†L343-L389】【F:src/com/traduvertgames/entities/Player.java†L119-L224】
- **Menu:** `Enter` confirma opções, `Esc` retorna ao menu/pausa, `T` salva o progresso quando em jogo.【F:src/com/traduvertgames/main/Game.java†L339-L372】【F:src/com/traduvertgames/main/Menu.java†L42-L103】

## Itens e recursos

- **Vida, mana e munição** – O jogador possui barras máximas configuradas dinamicamente; pacotes de vida (`LifePack`) e armas (`Weapon`) são coletados via colisão para restaurar recursos.【F:src/com/traduvertgames/entities/Player.java†L19-L118】【F:src/com/traduvertgames/entities/Entity.java†L109-L194】
- **Projéteis** – Há dois tipos de disparo (`Bullet` e `BulletShoot`) com atualizações e renderização independentes; os disparos energizados (`BulletShoot`) agora também são utilizados pelos inimigos para ataques à distância.【F:src/com/traduvertgames/entities/Bullet.java†L5-L12】【F:src/com/traduvertgames/entities/BulletShoot.java†L12-L52】
- **Inimigos** – A IA intercala patrulha, perseguição com recálculo dinâmico de caminhos, flanqueia o jogador e dispara projéteis sempre que há linha de visão, contabilizando eliminações para avançar de fase.【F:src/com/traduvertgames/entities/Enemy.java†L28-L370】

## Sistema de salvamento

O jogo grava vida, mana, quantidade de munição (arma), inimigos derrotados, progresso de `levelPlus` e fase atual em `save.txt`, aplicando uma codificação simples. A opção "carregar jogo" do menu lê esse arquivo, restaura atributos do jogador e recarrega o mapa correspondente. Ao morrer, pressionar `Enter` recarrega automaticamente o último save disponível; caso o arquivo não exista, um novo jogo é iniciado do nível 1 com todos os recursos resetados.【F:src/com/traduvertgames/main/Game.java†L63-L156】【F:src/com/traduvertgames/main/Game.java†L409-L637】【F:src/com/traduvertgames/main/Menu.java†L24-L162】

## HUD, áudio e recursos

- **HUD** – A classe `UI` desenha barras e textos com vida, mana, inimigos restantes, armas e mensagens de `Game Over` diretamente sobre o canvas escalonado.【F:src/com/traduvertgames/main/Game.java†L229-L288】
- **Áudio** – `Sound.java` encapsula efeitos de som e música ambiente utilizados ao interagir com o menu ou sofrer dano.【F:src/com/traduvertgames/main/Sound.java†L1-L120】
- **Mundo** – A classe `World` carrega os tiles dos arquivos `level*.png`, gerando colisões, entidades e caminhos de A* a partir dos dados de pixels.【F:src/com/traduvertgames/world/World.java†L20-L145】

## Requisitos

- Java 11 ou superior
- Gradle 7+ (ou utilize o Gradle wrapper incluso)

## Compilação e execução

Você pode compilar e executar o projeto utilizando o Gradle wrapper:

```
./gradlew run
```

Para gerar um JAR distribuível:

```
./gradlew build
```

Os artefatos gerados ficarão disponíveis em `build/libs/`.

## Dicas de desenvolvimento

- Ponto de entrada do jogo: `src/com/traduvertgames/main/Game.java`
- Menu e tratamento de entrada: `src/com/traduvertgames/main/Menu.java`
- O diretório `res/` contém os sprite sheets, arquivos de fase e clipes de áudio referenciados pelo código.

Sinta-se à vontade para experimentar com o código e aprender mais sobre conceitos básicos de programação de jogos em Java!
