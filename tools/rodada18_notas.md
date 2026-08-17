# Rodada 18 — Correção da seta, objetivos fases 3/4/5/7, HUD compacta

## CONFIRMADO (teste via World.restartGame — fluxo real)

Com o fallback do StoryManager, a Ava da fase 1 agora fica em **(240,176)** mundo (tile chão mais próximo de (16,11): (15,11) — na verdade o teste mostra 240=tile 15, y 176=tile 11). O waypoint agora aponta corretamente: do spawn (32,32) até a Ava são 232 px = 14 m na direção sudeste — seta correta na borda inferior direita. PROBLEMA DA SETA DO USUÁRIO RESOLVIDO na raiz (NPCs presos no canto). FALTA: (1) evitar seta sobrepor o card; (2) marcador do alvo no MiniMap; (3) verificar o tile escolhido: (15,11) é o rosa E649B1?? Não, isWallTile de tile default = wall... 240/16=15, 176/16=11. (15,11) tem o pixel rosa E649B1 (cor desconhecida = tile default wall). Mas o teste mostra Ava em (240,176)! Verificação: `isWallTile` retorna `false` para tile **null** (`instanceof WallTile` = false). O fallback aceita tile null como chão — por isso a Ava foi parar em (15,11), que tem o pixel rosa E649B1 (cor desconhecida → tile null). O tile ideal é chão explícito (0x000000). CORRIGIR no fallback: aceitar apenas tile != null E não WallTile. Depois: a Ava da fase 1 vai para (17,11)=chão real, dist 1 do preferido. ALTERNATIVA MELHOR para narrativa: mudar o pixel rosa (15,11) do level1 para 000000 (chão) — era claramente um resíduo de edição; assim o tile (15,11) fica chão explícito.

## Estado atual (importante)

O reposicionamento dos NPCs pelo `StoryManager.moveToNearestFreeTile` já foi corrigido: quando o tile preferido (ex.: Ava em (16,11) no level1) e o anel de raio 8 falham, o método agora varre o mapa inteiro por distância manhattan e escolhe o primeiro chão válido (em vez de manter a posição original). A edição foi aplicada em `src/com/traduvertgames/quest/StoryManager.java` (o fallback com `bestTx/bestTy` após o loop de raio 8).

**PORÉM o teste WaypointFrameTest ainda mostra a Ava em (48,32)** — preciso investigar: (a) se o fallback realmente roda (o anel de raio 8 pode estar encontrando um chão antes — verificar tile (17,11) etc.), (b) ou se `placeStoryNpcs` nem roda no fluxo do teste (o construtor do Game roda `new World("/level1.png")` no init, e o caminho com `restartGame` é outro — no construtor NÃO há chamada a `placeStoryNpcs`! Ver Game.java linhas 176-185: o init inicial não chama StoryManager; só o `restartGameCommon` em World.java:313 o chama). O teste imprime (48,32) porque é o fluxo do construtor — CORRETO no teste mas o jogo real usa restartGame que chama placeStoryNpcs. PRECISO rodar o teste via World.restartGame("level1.png") para reproduzir o fluxo real.

## Cores do terreno (aplicarMapPixels, World.java:81+)

`0xFF000000` = chão (FloorTile), `0xFFFFFFFF` = PAREDE (WallTile), `0xFF808080` = parede destrutível. Cores não reconhecidas (ex.: rosa E649B1 no level1 tile 15,11) ficam com o tile inicial do array — verificar o inicial (provavelmente WallTile, daí o "mapa cercado de paredes"). Tiles de chão no level1: linha y=10 e y=11 (16,11 é FFFFFF parede; (17,11) é chão 000000), linha y=15 inteira.

## Dados reais dos mapas (scan_detailed.py — o scan antigo com agrupamento por tile estava BUGADO)

| Nível | spawn | Ava | Outros |
|---|---|---|---|
| 1 | (2,2) | (3,2) | QuestItem×4, Warden×2, Beacon×3, Teleporte×3, rosa?? (15,11) |
| 3 | (3,3) | (3,2) | Ivo (5,2), Warden×3, Beacon×3, Teleporte×4 |
| 4 | (3,3) | (3,2) | Mercurio (5,2), Beacon×3, Warden×2, Teleporte×4, QuestNPC×2, QuestItem |
| 5 | (3,3) | (3,2) | Ivo (5,2), Teleporte×2, QuestItem×3, Warden×2, EngineerNPC, WARBRINGER (32,19) |
| 7 | (3,3) | (7,3) | Warden×3, Sentinel×3, Beacon×2, Ravager×3, Ivo/Nia/Mercurio (linha 20) |

## Seta/waypoint

O waypoint da fase 1 é matematicamente correto (teste: alvo CommanderNpc (48,32), jogador (32,32), 1 m a leste). A seta "6 m no canto superior esquerdo" da screenshot do usuário é consistente com ele andando ~64 px a sudeste da Ava; o NPC teal que ele vê abaixo à direita provavelmente é outro (beacon/Engenheiro), não a Ava. A reclamação real embutida é que a Ava fica grudada no canto inicial — corrigido pelo fallback do StoryManager. Melhorias a fazer no `MissionHud.drawWaypoint`: (1) não desenhar seta colidindo com o card da missão (deslocar para baixo do card quando clampada na área do card); (2) adicionar marcador do alvo no minimapa (MiniMap.java, canto sup. direito, ponto amarelo pulsante na posição do alvo do waypoint).

## MiniMap.java

`render(Graphics g)` em `src/com/traduvertgames/graficos/MiniMap.java` (79 linhas): painel 56×32 no canto sup. direito (panelX=WIDTH-56-8, panelY=8), TILE_DRAW=2, cores COLOR_FLOOR/COLOR_WALL/COLOR_PLAYER/COLOR_ENEMY. Adicionar no final (após o player) o ponto do alvo: `QuestManager.getTargetHint()` → mesma lógica de findTargetEntity do MissionHud (extrair para método package-shared ou usar reflexão) e desenhar ponto amarelo (255,235,59) pulsante em `panelX + (target.getX()/16)*TILE_DRAW`.

## Objetivos por fase — EDITADOS (QuestManager.createObjectiveForLevel)

- Fase 3: DialogueObjective(SequenceObjective(SurviveObjective("O laboratório sob cerco",...,30) → RitualObjective), "Pesquisador Ivo") ✓ aplicado
- Fase 4: mantida DialogueObjective(RescueObjective, "Armeiro Mercúrio") ✓ (aceitável)
- Fase 5: DialogueObjective(SequenceObjective(DataRecoveryObjective → BossHuntObjective("Derrubar o Warbringer",...,"o Warbringer")), "Pesquisador Ivo") ✓ aplicado
- Fase 7: DialogueObjective(SequenceObjective(SabotageObjective → HoldObjective("Isolar o núcleo do Guardião",...)), "Comandante Ava") ✓ aplicado
- Fases 2/6/8 já usavam Sequence (rodada 17). Fase 1 = ContactObjective direto.

APIs: HoldObjective(title,desc) alvo="Beacon do setor", auto-spawn beacon só level 2 (fases precisam de beacons do mapa: level 7 tem 2 beacons ✓). SurviveObjective(title,desc,sec) timer só em NORMAL. BossHuntObjective(title,desc,name) SEM getTargetHint — adicionar override (bossName enquanto !bossDefeated), senão waypoint sem seta de chefe. EscortObjective auto-spawn só level 8; fase 5 usa BossHunt (WARBRINGER do mapa, isBoss=true ✓). SabotageObjective: targetHint Hélio antes de traitorTalked, Guardião depois.

## PENDÊNCIAS: 1) getTargetHint no BossHuntObjective; 2) MissionHud seta não sobrepor card + marcador alvo no MiniMap; 3) ajustar ObjectivesVariadosTest se necessário + rebuild + regressão; 4) commit/push branch manus/objetivos-fase (PR #32). StoryManager fallback FEITO; pixel rosa level1 FEITO (tools/fix_level1_pixel.py).

## Objetivo antigo (obsoleto — não confiar mais nesta seção):

Fases 2/6/8 já usam Sequence com Hold/Survive/Escort (rodada 17). Legados a redesenhar: fase 3 = `DialogueObjective(SequenceObjective(SurviveObjective(30s) → RitualObjective), "Pesquisador Ivo")`; fase 4 = `DialogueObjective(SequenceObjective(DataRecoveryObjective → HoldObjective(beacon)), "Armeiro Mercúrio")`; fase 5 = `DialogueObjective(SequenceObjective(EscortObjective → BossHuntObjective("Derrubar o Warbringer", "...", "o Warbringer")), "Pesquisador Ivo")` (aproveita WARBRINGER fixo do mapa); fase 7 = `DialogueObjective(SequenceObjective(SabotageObjective → BossHuntObjective("o Guardião do Subsolo")), "Comandante Ava")`.

## Regressão (após fix do StoryManager + pixel level1)

StoryNpcPlacementTest 39/39, ObjectivesVariadosTest 20/20, ShopQaTest 19/19, MenuNavigationTest 12/12, PhaseTransitionTest OK, AutoValidate 24/24. GameOverUxTest não existe no repo (nome errado nos resumos anteriores).

## Comandos úteis

- Build: `cd /home/ubuntu/First-game-in-java && javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep error | head -3; echo BUILD_OK`
- Teste: `out=/tmp/test_X && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/X.java 2>/dev/null && DISPLAY=:120 timeout 60 java -cp $out:bin:res X 2>&1 | tail -2`
- Testes de regressão: ObjectivesVariadosTest, ShopQaTest, GameOverUxTest, MenuNavigationTest, PhaseTransitionTest, AutoValidate, StoryNpcPlacementTest (ferramentas em tools/)
- PR: branch `manus/objetivos-fase`, PR #32 aberto.
- Game expõe `Game.getBufferImage()` (adicionei) para testes de frame.
