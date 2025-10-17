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
