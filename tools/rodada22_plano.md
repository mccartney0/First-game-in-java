# Rodada 22 — plano (executar após merge do PR #35)

## Estado atual
PR #35 mergeado na main (commit f298a55). Branch atual: `manus/rodada22-trilha-npcs-inventario` (pushed, sem PR ainda).

## Pedido do usuário (rodada 22)
1. **Trilha sonora adaptativa** para diferentes áreas do mapa (músicas por bioma).
2. **Mais tipos de NPCs com missões secundárias e diálogos ramificados**.
3. **Sistema de inventário visual** para gerenciar itens e equipamentos.

## Arquitetura de áudio (SoundManager.java — já lido)
- `src/com/traduvertgames/main/SoundManager.java`: efeitos com enum `Event`, pool de Clips, FILES=/sounds/*.wav, `play(Event)`, `unload()`, volume via `OptionsConfig.getSoundVolume()`. NÃO tem suporte a música de fundo.
- Opções: `OptionsConfig.isSoundEnabled()`, `getSoundVolume()`.
- Sons procedurais existentes: `tools/` tem geradores (SoundGen.java?) usados nas rodadas anteriores — verificar geradores procedurais em tools/ (ex. SoundGen*.java, GenerateSounds.java) que criam WAVs no diretório sounds.
- Xvfb ativo em DISPLAY=:120 para testes que instanciam Game.

## Mapa/biomas das fases (res/level1..8.png) — mapear por cores dominantes:
- level1: grama/floresta | level2: deserto? | ... definir ZonaBioma por fase.

## Plano de implementação

### 1. Trilha sonora adaptativa (MusicManager.java)
- Gerar 3-4 trilhas procedurais WAV em loop (Java Sound synthesis via Tool javax.sound.sampled ou usar arquivos reais gerados por AI music skill):
  - `music_forest.wav` — floresta calma (fases 1-2)
  - `music_tension.wav` — tensão (fases 3-5, base do inimigo)
  - `music_boss.wav` — intensa (fases 6-8, chefe)
  - `music_arena.wav` — adrenalina (modo infinito/arena)
- MusicManager: crossfade suave (~2s), volume de música separado (OptionsConfig.add musicVolume), `setZone(Biome)`, chamada no `World.restartGame`/WaveManager.
- **Decisão: usar geração procedural em Java (sem IA) para loop perfeito** — padrão do projeto. Geradores em `tools/` que gravam WAVs com senoides/ruído. Loop: usar Clip.LOOP_CONTINUOUSLY.

### 2. NPCs secundários + diálogos ramificados
- `SideQuest.java`: missões simples (coletar N itens, matar N inimigos, entregar item) com recompensa (loot/moedas/upgrade).
- `InteractiveNpc` já tem DialogueManager; estender para branches: classe `DialogueBranch` com escolhas (1/2), cada escolha levando a outro nó; flag de conclusão por NPC; persistir em save v3 (dialogFlags já existe).
- 3 novos NPCs: Mercador (oferece desconto na loja?), Veterano (side quest de kills), Pesquisadora (coleta itens).
- Registrar NPCs via QuestManager.onLevelLoaded por fase (StoryManager.setStoryNpcs por fase).

### 3. Inventário visual (InventoryManager.java)
- Tecla I abre; grid 4x3; itens coletáveis (LifePack, NanoMedkit, moedas?) e armas (Player weapons 0-3); equipar arma muda arma atual; usar item consome (vida/mana).
- HUD: ícones por item; contagem.
- Overlay scaled-space (overlayG) consistente com rodada 20.

## Comandos
- Build: `cd /home/ubuntu/First-game-in-java && javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep error | head -3; echo BUILD_OK`
- Teste: `out=/tmp/test_X && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/X.java 2>/dev/null && DISPLAY=:120 timeout 90 java -cp $out:bin:res X 2>&1 | tail -1`
- Suíte: ObjectivesVariadosTest ShopQaTest MenuNavigationTest PhaseTransitionTest AutoValidate StoryNpcPlacementTest WaypointFrameTest TransitionCooldownTest
- PR: `gh pr create --title "Rodada 22 — ..." --body ... --base main`

## Backlog futuro (rodadas 23+)
- Balanceamento fino chefes fases 7/8
- Mais skins/companions com preview na loja
- Missões secundárias extras / diálogos ramificados profundos
- Pausa por ESC durante combate

## STATUS
- [ ] MusicManager + trilhas procedurais (WAVs gerados em tools/rodada22_music_gen.java → res/sounds/)
- [ ] SideQuest + NPCs secundários + diálogos ramificados
- [ ] InventoryManager visual (tecla I)
- [ ] Build + suíte + testes novos + commit + push + PR


## PROGRESSO (atualizado)

### ✅ Item 1 — Trilha sonora adaptativa: CONCLUÍDO E TESTADO
- `tools/MusicGen.java` → gera 4 WAVs em `res/sounds/` (music_forest, tension, boss, arena; 72s, 44.1kHz/16-bit/mono, fade-out 5s p/ loop suave). Já executado, arquivos gerados.
- `src/.../main/MusicManager.java`: enum Zone {FOREST,TENSION,BOSS,ARENA} com forLevel(int); setZone com crossfade 2s (120 frames), update() por frame, pause/resume/unload/applyMusicPreference, ganho via FloatControl.MASTER_GAIN.
- `OptionsConfig.java`: musicVolumeDb (passos de 2dB, clamp -10..+5), getMusicVolume(), adjustMusicVolume(deltaDb), applyMusicPreference chama MusicManager.
- `Menu.java`: opções agora 4 itens (musica, volume da trilha, dificuldade, voltar); Enter aumenta volume da trilha, Shift+Enter diminui; renderOptionsMenu mostra "Trilha sonora: X dB"; handleLoadSelection pausa MusicManager.
- `Menu.java` campo novo: `public boolean shift;`
- `Game.java`: keyPressed VK_SHIFT → menu.shift=true; update() NORMAL chama MusicManager.update(); startNewGame → setZone(FOREST/forLevel(1)); returnToMainMenu → MusicManager.pause(); advanceToNextLevel → setZone(forLevel(CUR_LEVEL)); enterSurvivalMode e enterInfiniteMode → setZone(ARENA).
- Teste: `tools/MusicZoneTest.java` — ALL PASSED (zonas, crossfade, volume, pause/resume, unload, crossfade interrompido).
- Padrão: testes em tools/ rodam sem loop: `SoundManager.unload()` + `new Game()`, DISPLAY=:120.

### ⬜ Item 2 — NPCs secundários + diálogos ramificados: PENDENTE
Arquitetura: InteractiveNpc (lines[], InteractionListener onInteractionStart/End, finished, wasInteracted, isDialogueSaved via SaveManager.hasNpcDialogue(name, level), indicador ✓). DialogueManager: startNearestDialogue, advance (linha atual), render. StoryManager.placeStoryNpcs() com relocateByTag. QuestManager: notifyEnemyKilled, collectQuestItem, notifyDialogueStarted/Finished, onLevelLoaded. SaveManager: NPC_DIALOGUES_KEY em progress, restoreNpcDialogues (map phase→flags).

Plano escolhido:
- `BranchingNpc extends InteractiveNpc`: DialogueBranch (id, text, Choice[label,targetId], onSelected action). Actions: startSideQuest, grantReward (heal/shield), setDiscount.
- `DialogueManager`: detectar BranchingNpc → render choices (Enter/1..9), advance segue o nó escolhido.
- `SideQuest` + `SideQuestManager`: tipos KILL_N, COLLECT_N; hooks QuestManager; progresso persistido em session "sideQuests".
- 3 NPCs: Veterano Rex (f2/4/6, kills), Pesquisadora Lila (f3/5/7, coleta itens), Mercador Finn (f5/7, moedas→desconto loja).
- Registrar via StoryManager.placeStoryNpcs relocateByTag com tags novas.
- Salvar flags: reutilizar SaveManager.npcDialogues (hasNpcDialogue/completeNpcDialogue?) — verificar métodos exatos do SaveManager.

### ⬜ Item 3 — Inventário visual: PENDENTE
- `InventoryManager`: grid 3x4, tecla I (Game keyPressed, estado NORMAL); navegação setas + Enter para usar; ESC fecha.
- Consumíveis: LIFEPACK, NANOMEDKIT, SHIELDORB, ENERGYCELL, OVERCLOCK (enum interno ItemType).
- Mudar Player.checkCollisionLifePack/NanoMedkit/ShieldOrb/EnergyCell/OverclockModule: coletar para inventário (Som PICKUP) em vez de efeito imediato.
- Render em overlayG (padrão MissionBanner, scaled-space); painel canto inferior-esquerdo quando fechado mostra contadores.
- SaveManager: session "inventory" persist; restaurar no load.

### ⬜ Itens finais
- Testes: InventoryTest, BranchingNpcTest (+ SideQuest)
- Suíte completa: ObjectivesVariadosTest, ShopQaTest, MenuNavigationTest, PhaseTransitionTest, AutoValidate, StoryNpcPlacementTest, WaypointFrameTest, TransitionCooldownTest
- Commit/push na branch `manus/rodada22-trilha-npcs-inventario`, PR #36 (--base main)
- Branch criada sobre f298a55 (PR #35 mergeado)

### Comandos
- Build: `cd /home/ubuntu/First-game-in-java && javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep error | head -3; echo BUILD_OK`
- Teste: `out=/tmp/test_X && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/X.java 2>/dev/null && DISPLAY=:120 timeout 90 java -cp $out:bin:res X 2>&1 | tail -1`
- git add -A; git commit; git push origin manus/rodada22-trilha-npcs-inventario; gh pr create --title "Rodada 22 — trilha adaptativa, NPCs secundários e inventário" --body "..." --base main


## ATUALIZAÇÃO 2 (inventário em progresso)

### ✅ Item 3 — Inventário visual: CONCLUÍDO E INTEGRADO
- `src/.../main/InventoryManager.java` criado: enum ItemType (MEDKIT, NANOMEDKIT, ENERGY_CELL, SHIELD_ORB, OVERCLOCK, AMMO_PACK, ACCESS_KEY, DATA_CORE) com 8 slots, contagem por tipo, add/addPickup (com FloatingText), toggle (tecla I), navigateUp/Down/Left/Right, useSelected (heal/addMana/addShield/refillCurrentWeapon/applyComboSurge), consume(), serialize/deserialize Map<String,Integer>, reset(), update() (cooldown 15 frames), render(Graphics2D overlayG): barra de contadores na base + painel grade 2x4 (espaço escalado, identidade HUD).
- Player.java: checkCollisionLifePack/NanoMedkit/ShieldOrb/EnergyCell/OverclockModule agora adicionam ao inventário (addPickup) em vez de efeito imediato; PICKUP sound mantido; Overclock dá ComboSurge x1 na coleta.
- Game.java: VK_I abre/fecha inventário (toggle; ignora durante diálogo); ENTER/SPACE usa item quando aberto; setas/WASD navegam a grade quando aberto (sem mover jogador); R ignora com inventário aberto; hidingHud inclui InventoryManager.isOpen(); InventoryManager.render(overlayG) após DialogueManager.render; InventoryManager.update() no bloco NORMAL; InventoryManager.reset() no startNewGame.
- SaveManager.java: session.put("inventario", ...) em saveCurrentGame; restore com deserialize ao carregar (reset se null).

### Testes criados
- `tools/InventoryTest.java` — coleta, toggle, uso, navegação, serialização (ainda não rodado).
- `tools/MusicZoneTest.java` — ALL PASSED.

### ⬜ Falta
1. Rodar InventoryTest (padrão tools: SoundManager.unload() + new Game(), DISPLAY=:120).
2. Item 2 — NPCs secundários (BranchingNpc, DialogueBranch, SideQuestManager): VER PLANO NO INÍCIO DO ARQUIVO (fase 3 do plano).
3. Tests BranchingNpcTest/SideQuestTest, suíte completa.
4. Commit/push na branch `manus/rodada22-trilha-npcs-inventario`, PR #36 (--base main).

### Decisões de design registradas
- Inventário NÃO pausa o jogo: bloqueia apenas movimento/navegação; inimigos continuam (hidingHud esconde HUD).
- Consumíveis de uso único (MediKit/NanoMed) removem entidade na coleta; efeito no uso.
- Tecla I ignorada durante diálogo.


## ATUALIZAÇÃO 3 — Rodada 22 quase completa

### ✅ Item 2 — NPCs secundários + diálogos ramificados: CONCLUÍDO E TESTADO
- `src/.../dialogue/BranchingNpc.java`: árvore de nós (DialogueNode), getNodeText() protected, selectChoice(choiceIndex) executa ação + troca nó, isTerminal/hasChoices.
- `src/.../entities/SecondaryNpcs.java`: Veterano Rex (KILL_N 10 kills, fases 2-8, tiles {10,8}/{34,18}/{14,20}), Pesquisadora Lila (COLLECT_N 3 ENERGY_CELL, fases 3-8, tiles {26,16}/{12,14}/{36,8}), Mercador Finn (DELIVER DATA_CORE por +50 mana +75 pts, fases 5-8, tiles {32,20}/{20,6}/{8,16}).
- `src/.../quest/SideQuestManager.java`: Type KILL_N/COLLECT_N/DELIVER; Reward (life/shield/mana/score); register/activate/isActive/isCompleted/addProgress/onEnemyKilled/refreshCollectibles/deliver/serialize/deserialize/reset.
- `StoryManager.placeStoryNpcs`: spawnSecondaryNpcs(level) adiciona NPCs (uma vez por fase) + relocateSecondary por tile.
- `QuestManager.notifyEnemyKilled` → SideQuestManager.onEnemyKilled; `QuestManager.update` → refreshCollectibles.
- `DialogueManager`: detecção BranchingNpc em startNearestDialogue/advance (Enter escolhe opção 0)/selectBranchChoice/getBranchChoices/isLastLine; render mostra "1. 2. 3." escolhas + rodapé "Digite 1-3 para escolher, Enter para a primeira".
- `Game.java`: teclas 1/2/3 e NUMPAD 1/2/3 durante diálogo → selectBranchChoice.
- Teste: `tools/BranchingNpcTest.java` — ALL PASSED (19 checks).

### ✅ Item 3 — Inventário: CONCLUÍDO E TESTADO (tools/InventoryTest.java ALL PASSED, System.exit adicionado)
### ✅ Item 1 — Música: CONCLUÍDO (tools/MusicZoneTest.java ALL PASSED)

### ⬜ FALTA
1. Suíte completa de regressão (ObjectivesVariadosTest, ShopQaTest, MenuNavigationTest, PhaseTransitionTest, AutoValidate, StoryNpcPlacementTest, WaypointFrameTest, TransitionCooldownTest, MusicZoneTest, InventoryTest, BranchingNpcTest).
2. Possível persistência de sideQuests no SaveManager (session.put sideQuests) — AINDA NÃO FEITO! Adicionar no saveCurrentGame (session.put("sideQuests", serialize()) e restore).
3. Commit/push branch `manus/rodada22-trilha-npcs-inventario`; PR #36 --base main.
4. Nota: MissionHud/QuestManager podem exibir missões secundárias? Não implementado — o SideQuestManager não tem HUD própria (o getObjectiveTitle do QuestManager continua o principal). Decisão: sem HUD extra para não poluir (o dialogue show o label).
