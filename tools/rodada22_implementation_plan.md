# Rodada 22 — plano de implementação final

Branch: `manus/rodada22-trilha-npcs-inventario` (criada sobre f298a55).

## 1. Trilha sonora adaptativa — MusicManager

Arquitetura existente: `Sound.java` carrega `/music.wav` único em `Sound.music` com pool de 1 clip e `loop(300)`; `OptionsConfig.applyMusicPreference()` chama `Sound.music.loop()/stop()`; Menu toggleMusic.

Decisão: manter Sound.music intacto (menu usa). Criar `MusicManager.java`:
- Gerar 4 temas procedurais via `tools/MusicGen.java` (java puro, grava WAV PCM 44.1kHz/16-bit/mono, ~75s com fade-out 4s para loop suave) em `res/sounds/music_forest.wav`, `music_tension.wav`, `music_boss.wav`, `music_arena.wav`.
- Mapeamento Zona por fase: 1-2 = FOREST (calma, pad + pluck pentatônico 72 BPM); 3-5 = TENSION (pulso grave 80 BPM, arpejo menor); 6-8 = BOSS (130 BPM, stabs menores); infinito/arena = ARENA (140 BPM adrenalina).
- `MusicManager.setZone(Zone)` com crossfade 2s (Clip novo entra com ganho subindo, antigo para). Volume separado: `OptionsConfig` ganha `musicVolumeDb` e `adjustMusicVolume()`; ganho aplicado no clip.
- Hook: chamar em `Game.advanceToNextLevel()` (zona = CUR_LEVEL), `WaveManager.startArena()` (zona ARENA), e Game.startNewGame (zona da fase atual). Evitar tocar na tela de MENU.
- Menu opções: mostrar volume música (M/m). Opções do menu (linha ~785 do Menu.java) — adicionar item.

## 2. NPCs secundários com missões e diálogos ramificados

Arquitetura existente: `InteractiveNpc` (falas[], InteractionListener onInteractionStart/End, finished, wasInteracted, flags no save v3 via SaveManager.npcDialogues, indicador ✓). `StoryManager.placeStoryNpcs()` registra relocation por tag. `QuestManager.notifyDialogueStarted/Finished`, `notifyEnemyKilled`, `collectQuestItem`.

Decisão:
- `SideQuest.java`: classe de missão simples — tipos KILL_N, COLLECT_N, DELIVER; target, amount, reward (heal/shield/coin). Progresso via hooks QuestManager.notifyEnemyKilled e collectQuestItem + novo método checkSideQuestProgress no Game update (chamado no fim do update NORMAL).
- `SideQuestManager`: registro global; persistir progresso em save v3 (session "sideQuests" map phase→state JSON simples; reutilizar ObjectiveState serialization).
- `BranchingNpc.java` estende InteractiveNpc: `DialogueBranch` com texto + choices (label → nextBranch), onChoice actions (startSideQuest, completeSideQuest, grantReward, shopDiscount flag). DialogueManager: detectar se target é BranchingNpc → mostra choices (teclas 1/2) no render; advance() segue para o nó escolhido.
- 3 NPCs: Veterano Rex (fases 2/4/6, side quest KILL_N, recompensa heal/mana), Pesquisadora Lila (fases 3/5/7, COLLECT_N itens de quest, recompensa shield/loja desconto), Mercador Finn (fases 5/7, side quest moedas/DELIVER, recompensa: desconto na loja via ShopManager.setDiscount).
- Registrar via StoryManager.placeStoryNpcs() com relocateByTag (tags novas).

## 3. Inventário visual — InventoryManager

Arquitetura existente: Player.checkCollisionLifePack/NanoMedkit etc. (coleta imediata ao tocar). Teclas usadas: A/D/W/S setas, R diálogo, I LIVRE, T, P, Q, E, F, L, X, TAB, F11, ESC, ENTER, SPACE, 1-6.
Itens: LifePack (heal 40), NanoMedkit (heal+shield), ShieldOrb, EnergyCell (mana+energia), OverclockModule (mana+boost+combo), QuestItem/DataCore (missões).

Decisão:
- `InventoryManager.java`: grid 3x4 (12 slots); abre com I (keyPressed, estado NORMAL apenas). Slots = itens consumíveis que o jogador COLIDE: mudar Player.checkCollision*: em vez de aplicar efeito imediato, adicionar ao inventário (Sound PICKUP, FloatingText). Consumíveis: LIFEPACK, NANOMEDKIT, SHIELDORB, ENERGYCELL, OVERCLOCK.
- Navegação: setas/A-D move cursor, teclas 1-9 (ou cursor+ENTER) usa/equipa, ESC fecha. Usar item: heala/shield conforme tipo; remover do slot (consumível).
- Armazenamento: `Game.inventory` List<ItemType>; persistir em SaveManager (session "inventory").
- Render: overlayG scaled-space (MissionBanner padrão), painel central superior-direito? Melhor: painel canto inferior-esquerdo (não cobre jogo) quando fechado mostra contador de itens por tipo; aberto mostra grid com cursor e dicas.
- HUD: ao selecionar item no inventário, badge "próximo item a usar" no HUD? Manter simples: grid aberto.

## 4. Testes novos
- `MusicZoneTest`: MusicManager.setZone por fase corretamente mapeada; crossfade não trava; unload.
- `InventoryTest`: add/use/item persiste; tecla I abre/fecha (simular via GameManager.inventory aberto?).
- `BranchingNpcTest`: escolha de branch, onChoice action, persistência.
- Suíte completa existente deve continuar verde.

## 5. Build e PR
- Build: `javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep error | head -3; echo BUILD_OK`
- Teste: `out=/tmp/test_X && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/X.java 2>/dev/null && DISPLAY=:120 timeout 90 java -cp $out:bin:res X 2>&1 | tail -1`
- PR #36: `gh pr create --title "Rodada 22 — ..." --base main`

## STATUS / TODO
- [ ] MusicGen.java → gerar 4 WAVs em res/sounds/
- [ ] MusicManager.java + hook em Game/WaveManager + OptionsConfig musicVolume + Menu opção
- [ ] BranchingNpc + DialogueManager choices + SideQuest/SideQuestManager
- [ ] 3 NPCs novos + StoryManager.placeStoryNpcs register
- [ ] InventoryManager + I key + Player.collectToInventory (mudar checkCollision*)
- [ ] SaveManager: inventory + sideQuests persist
- [ ] 3 testes novos + suíte completa verde
- [ ] Commit + push + PR
