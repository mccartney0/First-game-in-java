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
- **Progressão de fases** – Cada mapa traz um objetivo próprio controlado pelo `QuestManager`; ao completar a missão vigente (coletar relíquias, caçar o chefe, interromper o ritual, resgatar civis ou recuperar protocolos científicos) o jogo carrega o próximo arquivo `level1.png`–`level5.png` e reaplica os bônus de progressão quando o ciclo é concluído.【F:src/com/traduvertgames/main/Game.java†L230-L321】【F:src/com/traduvertgames/quest/QuestManager.java†L15-L91】
- **Sistema de missões** – Os objetivos ficam visíveis no painel “Missão atual” da HUD, exibindo título, descrição e progresso em tempo real para guiar a exploração estilo RPG.【F:src/com/traduvertgames/graficos/UI.java†L94-L166】

## Controles e interações

- **Movimentação:** `W`, `A`, `S`, `D` ou setas direcionais.
- **Pulo:** `Espaço` ativa o salto 2D simulado do personagem.【F:src/com/traduvertgames/main/Game.java†L323-L363】【F:src/com/traduvertgames/entities/Player.java†L43-L118】
- **Ataque:** `X` dispara projéteis enquanto houver munição e mana; o mouse também pode ser usado para mirar e atirar.【F:src/com/traduvertgames/main/Game.java†L452-L516】【F:src/com/traduvertgames/entities/Player.java†L101-L242】
- **Troca de arma:** `Q`/`E` alternam entre as armas desbloqueadas e as teclas `1` a `4` selecionam diretamente um arquétipo específico.【F:src/com/traduvertgames/main/Game.java†L452-L497】【F:src/com/traduvertgames/entities/Player.java†L484-L512】
- **Menu:** `Enter` confirma opções, `Esc` retorna ao menu/pausa, `T` salva o progresso quando em jogo.【F:src/com/traduvertgames/main/Game.java†L339-L372】【F:src/com/traduvertgames/main/Menu.java†L42-L103】
- **Painel tático:** `Tab` alterna entre ocultar e exibir os cartões informativos avançados da HUD, liberando a visão do campo de batalha quando minimizados.【F:src/com/traduvertgames/main/Game.java†L133-L140】【F:src/com/traduvertgames/main/Game.java†L465-L519】【F:src/com/traduvertgames/graficos/UI.java†L52-L77】
- **Opções:** O submenu "Opções" do menu principal permite ligar ou desligar a música ambiente e alternar a dificuldade entre Fácil, Normal e Difícil; a configuração ajusta automaticamente a vida, mana, capacidade de munição e o dano sofrido pelo jogador.【F:src/com/traduvertgames/main/Menu.java†L25-L230】【F:src/com/traduvertgames/main/OptionsConfig.java†L1-L92】【F:src/com/traduvertgames/main/Game.java†L180-L357】【F:src/com/traduvertgames/entities/BulletShoot.java†L1-L60】【F:src/com/traduvertgames/entities/Enemy.java†L320-L420】

## Itens e recursos

- **Vida, mana, escudo e munição** – O jogador agora possui um escudo reativo adicional que absorve dano antes de atingir a vida, com limites reajustados pela dificuldade. Pacotes de vida (`LifePack`) e armas (`Weapon`) continuam restaurando recursos ao contato, enquanto a durabilidade das armas persiste entre sessões.【F:src/com/traduvertgames/entities/Player.java†L19-L512】【F:src/com/traduvertgames/main/Game.java†L624-L704】
- **Orbes de escudo** – Orbes renderizados por vetores (`ShieldOrb`) fornecem cargas de proteção extras. Eles podem ser posicionados diretamente no mapa com a cor `#8E24AA` ou dropados aleatoriamente pelos inimigos ao serem derrotados.【F:src/com/traduvertgames/entities/ShieldOrb.java†L1-L79】【F:src/com/traduvertgames/entities/Enemy.java†L585-L613】【F:src/com/traduvertgames/world/World.java†L39-L88】
- **Células de energia** – As novas `EnergyCell` restauram mana e recarregam parcialmente a arma ativa, sendo representadas por formas vetoriais animadas. Podem ser adicionadas ao mapa com a cor `#1DE9B6` e também surgem como saque após uma eliminação.【F:src/com/traduvertgames/entities/EnergyCell.java†L1-L74】【F:src/com/traduvertgames/entities/Enemy.java†L585-L613】【F:src/com/traduvertgames/entities/Player.java†L321-L416】【F:src/com/traduvertgames/world/World.java†L39-L88】
- **Kits de nanites e módulos de sobrecarga** – `NanoMedkit` gera um pulso de cura e reforça o escudo enquanto `OverclockModule` restitui mana, arma atual e prolonga o combo ativo, aparecendo tanto em fases quanto como saque inimigo (`#FF5252` e `#00E5FF`).【F:src/com/traduvertgames/entities/NanoMedkit.java†L1-L55】【F:src/com/traduvertgames/entities/OverclockModule.java†L1-L49】【F:src/com/traduvertgames/entities/Enemy.java†L603-L619】【F:src/com/traduvertgames/world/World.java†L61-L103】【F:src/com/traduvertgames/entities/Player.java†L401-L438】
- **Artefatos de missão** – Relíquias (`QuestItem`), faróis (`QuestBeacon`) e pesquisadores (`QuestNPC`) utilizam novas cores (`#FFC107`, `#4CAF50`, `#795548`) para habilitar objetivos estilo RPG – coletar itens, canalizar energia ou resgatar NPCs, respectivamente.【F:src/com/traduvertgames/entities/QuestItem.java†L1-L62】【F:src/com/traduvertgames/entities/QuestBeacon.java†L1-L63】【F:src/com/traduvertgames/entities/QuestNPC.java†L1-L44】【F:src/com/traduvertgames/world/World.java†L44-L120】
- **Protocolos de dados e especialistas** – O nível avançado apresenta `DataCore` (`#00ACC1`) como itens colecionáveis e dois novos NPCs (`EngineerNPC` – `#FFB74D`, `ResearcherNPC` – `#7E57C2`) que concedem bônus ao serem resgatados, integrando a missão "Protocolos perdidos".【F:src/com/traduvertgames/entities/DataCore.java†L1-L45】【F:src/com/traduvertgames/entities/EngineerNPC.java†L1-L27】【F:src/com/traduvertgames/entities/ResearcherNPC.java†L1-L18】【F:src/com/traduvertgames/world/World.java†L61-L105】【F:src/com/traduvertgames/quest/DataRecoveryObjective.java†L1-L55】
- **Arsenal modular** – Seis armas com características próprias (Blaster, Rifle de Íons, Canhão Dispersor, Lança de Fusão, Disruptor de Arco e Canhão Solar) podem ser desbloqueadas, cada uma com custos de mana, dano e cadência diferenciados. A durabilidade de cada arma é persistida no save e pode ser reabastecida ao coletar novos itens.【F:src/com/traduvertgames/entities/WeaponType.java†L16-L167】【F:src/com/traduvertgames/entities/Player.java†L323-L512】【F:src/com/traduvertgames/main/Menu.java†L120-L185】
- **Projéteis** – Há dois tipos de disparo (`Bullet` e `BulletShoot`) com atualizações e renderização independentes; os disparos energizados (`BulletShoot`) agora também são utilizados pelos inimigos para ataques à distância.【F:src/com/traduvertgames/entities/Bullet.java†L5-L12】【F:src/com/traduvertgames/entities/BulletShoot.java†L12-L52】
- **Inimigos** – A IA intercala patrulha, perseguição com recálculo dinâmico de caminhos, flanqueia o jogador e dispara projéteis sempre que há linha de visão. Além das variantes Scout, Teleporter e Artillery, os mapas podem convocar Guardiões (`Variant.WARDEN`, tanque pesado) e o chefe Warbringer (`Variant.WARBRINGER`) por meio dos códigos `#3F51B5` e `#E91E63`.【F:src/com/traduvertgames/entities/Enemy.java†L25-L214】【F:src/com/traduvertgames/world/World.java†L44-L120】

## Pontuação e combos

- **Pontuação dinâmica** – Cada inimigo derrotado concede pontos que escalam com o multiplicador de combo atual; a pontuação final e o recorde são exibidos tanto na HUD quanto na tela de game over.【F:src/com/traduvertgames/main/Game.java†L68-L152】【F:src/com/traduvertgames/main/Game.java†L227-L309】
- **Combos temporizados** – O multiplicador de combo aumenta a cada abate consecutivo e expira após alguns segundos sem derrotas ou ao sofrer dano, reiniciando a contagem.【F:src/com/traduvertgames/main/Game.java†L125-L207】【F:src/com/traduvertgames/main/Game.java†L310-L360】【F:src/com/traduvertgames/entities/Enemy.java†L169-L220】【F:src/com/traduvertgames/entities/BulletShoot.java†L12-L52】
- **Registro persistente** – O save game inclui a pontuação atual, recorde e os melhores combos para que o progresso da partida seja retomado exatamente de onde parou.【F:src/com/traduvertgames/main/Game.java†L146-L205】【F:src/com/traduvertgames/main/Menu.java†L63-L170】

## Sistema de salvamento

O jogo grava vida, mana, escudo, quantidade de munição (arma), inimigos derrotados, progresso de `levelPlus`, fase atual, pontuação, recorde e os melhores combos em `save.txt`, aplicando uma codificação simples. A opção "carregar jogo" do menu lê esse arquivo, restaura atributos do jogador, atualiza o placar e recarrega o mapa correspondente. Ao morrer, pressionar `Enter` recarrega automaticamente o último save disponível; caso o arquivo não exista, um novo jogo é iniciado do nível 1 com todos os recursos resetados.【F:src/com/traduvertgames/main/Game.java†L228-L309】【F:src/com/traduvertgames/main/Game.java†L624-L704】【F:src/com/traduvertgames/main/Menu.java†L150-L247】

## HUD, áudio e recursos

- **HUD** – A classe `UI` exibe barras compactas dentro do canvas (agora incluindo a barra de escudo) e, após o escalonamento, projeta painéis translúcidos com status do piloto, a missão ativa com texto resumido, placar e arsenal desbloqueado. O canvas opera em 384×216 pixels (1 152×648 após o `SCALE`), com espaçamento ampliado, valores numéricos nas barras e cartões reposicionados para aproveitar a área extra.【F:src/com/traduvertgames/main/Game.java†L37-L320】【F:src/com/traduvertgames/graficos/UI.java†L18-L210】
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

## Gerar os mapas RPG

Os arquivos `res/level1.png` a `res/level5.png` não são versionados e devem ser criados localmente sempre que você clonar o repositório ou ajustar o layout das fases. Utilize o script Python localizado em `tools/generate_maps.py`:

```
python3 tools/generate_maps.py
```

O script depende apenas da biblioteca padrão do Python 3, grava as imagens diretamente em `res/` e imprime um resumo de cada mapa gerado. Sempre que quiser experimentar novos layouts, edite as funções `level_one()`–`level_four()` e execute novamente o comando acima.

Tokens úteis no gerador:

- `.` – piso navegável (preto, utilizado como preenchimento padrão)
- `#` – parede sólida padrão (branca)
- `X` – parede destrutível (cinza, `0xFF808080` no PNG gerado)
- `P` – posição inicial do jogador (`0xFF0026FF`)
- `E` – ponto de surgimento de inimigo padrão (`0xFFFF0000`)
- `T` – inimigo variante Teleporter (`0xFF9C27B0`)
- `A` – inimigo variante Artillery (`0xFF00BCD4`)
- `G` – inimigo variante Warden (`0xFF3F51B5`)
- `B` – chefe Warbringer (`0xFFE91E63`)
- `W` – arma coletável (`0xFFFF6A00`)
- `H` – kit de vida (`0xFF4CFF00`)
- `L` – munição/balas extras (`0xFFFFD800`)
- `Q` – item de missão (`0xFFFFC107`)
- `O` – baliza de missão (`0xFF4CAF50`)
- `N` – NPC de missão a ser resgatado (`0xFF795548`)
- `C` – célula de energia (`0xFF1DE9B6`)
- `M` – kit de nanites (`0xFFFF5252`)
- `R` – módulo de sobrecarga (`0xFF00E5FF`)
- `D` – núcleo de dados (`0xFF00ACC1`)
- `S` – pesquisador NPC (`0xFF7E57C2`)
- `I` – engenheiro NPC (`0xFFFFB74D`)

## Dicas de desenvolvimento

- Ponto de entrada do jogo: `src/com/traduvertgames/main/Game.java`
- Menu e tratamento de entrada: `src/com/traduvertgames/main/Menu.java`
- O diretório `res/` contém os sprite sheets, arquivos de fase e clipes de áudio referenciados pelo código; o script `tools/generate_maps.py` recria automaticamente os mapas RPG (`level1.png` a `level5.png`).【F:tools/generate_maps.py†L1-L226】

Sinta-se à vontade para experimentar com o código e aprender mais sobre conceitos básicos de programação de jogos em Java!
