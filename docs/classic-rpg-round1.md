# RPG Clássico — Rodada 1: Fundação

## Objetivo da entrega

A Rodada 1 estabelece um vertical slice jogável do **RPG Clássico** dentro da aplicação existente, sem substituir a engine atual nem misturar a progressão de fantasia com os sistemas do shooter. O modo entra pelo menu principal, cria uma sessão própria, apresenta uma escolha inicial de arquétipo, permite explorar um mapa mínimo top-down e oferece uma HUD dedicada com vida, mana, stamina, XP, objetivo e controles.

> A regra de isolamento é simples: o RPG Clássico reutiliza a janela, o canvas, o loop e o sistema de entrada da aplicação, mas não reutiliza o `Player` shooter, o arsenal futurista, os projéteis, o combo, os créditos do metagame ou o level-up roguelite.

## Arquitetura implementada

| Camada | Responsabilidade | Componentes |
| --- | --- | --- |
| Identidade de modo | Distinguir a experiência clássica sem espalhar condicionais de domínio | `GameModeType`, `Game.isClassicRpgMode()` |
| Cena e ciclo | Controlar criação, update, render, input e teardown do modo | `ClassicRpgMode` |
| Personagem | Atributos tradicionais, recursos, XP, nível e pontos de atributo | `RpgCharacterStats`, `RpgArchetype` |
| Exploração | Movimento top-down, colisão por tiles e câmera | `RpgPlayerController`, `RpgMap` |
| Integração | Menu, ciclo do `Game`, mouse, teclado e retorno ao menu | `Menu`, `Game` |
| Persistência | Sessão `CLASSIC_RPG` e árvore própria de save/load | `SaveManager` |
| Verificação | Testes headless do fluxo principal e regressões de fundação | `ClassicRpgModeTest` |

## Fluxo jogável

A opção **RPG Clássico** aparece no menu principal entre **Aventura RPG** e **Campanha narrativa**. Ao confirmar, a aplicação fecha overlays do shooter, limpa o estado regional e cria um `ClassicRpgMode`. A primeira tela é a criação de personagem, com três arquétipos iniciais:

| Arquétipo | Ênfase | Identidade inicial |
| --- | --- | --- |
| Guardião | Resistência e bloqueio | Mais vida, vitalidade e defesa física |
| Arcanista | Mana e afinidade arcana | Mais mana, inteligência e defesa mágica |
| Errante | Mobilidade e golpes críticos | Mais destreza, velocidade e chance crítica |

Depois da confirmação, o player é colocado no mapa **Vale de Brumafolha**, que contém uma vila, a Estrada Antiga, o Bosque dos Sussurros, um lago e as Ruínas do Sino. A movimentação é bloqueada por bordas, água e estruturas; a câmera acompanha o personagem com um pequeno look-ahead baseado na direção atual.

## Controles da Rodada 1

| Entrada | Ação |
| --- | --- |
| `WASD` ou setas | Movimento top-down |
| `Enter` | Confirmar arquétipo |
| `C` | Abrir ou fechar a ficha de atributos |
| `R` | Exibir a orientação contextual da exploração |
| `I` | Exibir aviso de que o inventário será expandido em rodada posterior |
| `T` | Salvar no slot ativo |
| `Esc` | Voltar ou abrir o menu de pausa conforme o estado global |

Durante o RPG Clássico, o mouse, o disparo, o arsenal, o inventário de pickups e os atalhos específicos do shooter são consumidos ou ignorados para evitar vazamento de domínio.

## Persistência e compatibilidade

O `SaveManager` grava `gameMode: "CLASSIC_RPG"` e adiciona uma seção `classicRpg` dentro da sessão do slot. Essa seção contém o schema do modo, o identificador do mapa, o objetivo, o tempo jogado, o snapshot do personagem, a posição/direção do player e o ponto de repouso lógico.

| Dado clássico | Campo persistido | Comportamento de carga |
| --- | --- | --- |
| Modo | `gameMode` | Seleciona o caminho exclusivo do RPG Clássico |
| Personagem | `classicRpg.character` | Restaura arquétipo, nível, XP, recursos e atributos |
| Player | `classicRpg.player` | Restaura posição e direção sem recriar o `Player` shooter |
| Mundo | `classicRpg.mapId` | Restaura `vale_brumafolha` |
| Objetivo | `classicRpg.objective` | Mantém a orientação atual da exploração |
| Compatibilidade | Campos legados do slot | Permanecem disponíveis para saves antigos e outros modos |

A carga de um slot clássico não chama `World.restartGame`, não reabre a seleção de armas e não restaura arsenal futurista. Em sentido contrário, os caminhos de campanha, Aventura RPG regional e Mundo Aberto continuam usando seus fluxos atuais.

## Critérios verificados

A suíte `ClassicRpgModeTest` cobre a existência da opção no menu, a entrada no modo, a seleção de arquétipo, a separação dos atributos do shooter, o bloqueio de movimento no mapa, a renderização da cena/HUD/ficha e o round-trip de save/load com posição e arquétipo.

A validação da Rodada 1 deve ser executada com:

```bash
./gradlew test --tests com.traduvertgames.test.ClassicRpgModeTest --no-daemon
./gradlew test --no-daemon
```

## Próximas rodadas previstas

A fundação deixa pontos de extensão explícitos para as próximas entregas, mas não finge implementar conteúdo fora do escopo desta rodada. O inventário RPG, equipamentos e peso entram na rodada de itens; NPCs, diálogo e quests entram na rodada social; combate, inimigos e dano entram na rodada de combate; dungeons, crafting, loja, descanso, música e polimento seguem como sistemas independentes.
