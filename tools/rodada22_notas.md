# Rodada 22 — notas de arquitetura (lido até agora)

## Música hoje (importante — NÃO há MusicManager, e sim Sound.java legado)
- `res/music.wav` (7,9 MB) — música única do jogo inteiro, carregada por `Sound.java` em `Sound.music = load("/music.wav", 1)` com pool de 1 clip e `loop(300)` (300 repetições).
- `OptionsConfig.isMusicEnabled()/toggleMusic()/applyMusicPreference()` chama `Sound.music.loop()/stop()`.
- Menu.java linha ~335: toggle música; linha ~366: resume após pausa; linha ~785: mostra "Música: Ligada/Desligada" no menu de opções.
- **NÃO há música por fase/bioma hoje** — é uma música só (loopada).

## Strategy para trilha adaptativa (decidido)
Criar `MusicManager.java` novo que substitui o uso de Sound.music:
- Mapeamento de biomas por fase (Zona): cada fase tem um tema. Ex.:
  - FASES 1-2: `music_calm.wav` (gramado/floresta, calma)
  - FASES 3-5: `music_tension.wav` (base inimiga, tensão)
  - FASES 6-8: `music_boss.wav` (núcleo/arena do chefe, intensa)
  - MODO INFINITO: `music_arena.wav` (adrenalina)
  - MENU: manter music.wav original (não mexer) ou usar tema ambiente
- Crossfade suave ~2s ao trocar de zona (dois Clips: tocar novo com volume subindo, parar o antigo).
- `MusicManager.setZone(Zone zone)` — chamar em World.restartGame / Game.advanceToNextLevel / WaveManager.startArena.
- OptionsConfig: adicionar `getMusicVolume()` e aplicar ganho no clip da música.
- Gerar os 4 WAVs por síntese procedural (Tool de síntese em java, estilo geradores das rodadas anteriores — mas não achei script python/py de geração de som; o python_port é outro repo). Vou criar gerador java próprio `tools/MusicGen.java` que grava WAVs em res/.
- Formato WAV: 44.1kHz, 16-bit, mono, ~90s (loop suave com fade-out curto no fim).

## Sintese procedural — ideias musicais (90s cada)
- calm: pad senoidal + pluck suave pentatônico, 72 BPM, tom maior
- tension: pulso grave 80 BPM, arpejo menor tenso, filtro
- boss: 130 BPM, bateria sintética, baixo pulsante, stabs menores
- arena: 140 BPM, adrenalina, bateria rápida + lead agudo
- Implementar com `AudioSystem` — escrever byte[] PCM e gravar WAV com RIFF header simples (sem dependência).

## Inventário (decidido)
- `InventoryManager.java`: grid 3x4, abre com I (verificar I livre — checar Game.keyListeners); desenha em overlayG (scaled-space, como MissionBanner).
- Células: itens consumíveis (LifePack, ManaBoost, ShieldCell se existirem — checar Entity items em Game.java), armas Player (4 armas já existem: Player tem maxWeapon?).
- Ações: selecionar célula com setas/teclas 1-9? usar mouse? — Decidir: navegação por teclado (setas + Enter = usar/equipar, ESC fecha).
- Salvar em SaveManager v3: adicionar inventory no save/load.
- HUD: badge de item em mãos no canto (indicar item equipado/ativo).

## NPCs secundários + diálogos ramificados (decidido)
- `DialogueNode`: id, texto, choices[] (label → targetNodeId ou ação), onSelected action (giveItem, grantReward, setFlag).
- `InteractiveNpc` já suporta texto/dialogue; estender para nós com branches.
- `SideQuestManager`: lista de side quests por fase (coletar N itens, matar N inimigos, entregar) com recompensa; progresso via QuestManager hooks (onKill/onPickup já existem? checar).
- 3 NPCs novos: "Veterano Rex" (missões de kills, fases 2/4), "Pesquisadora Lila" (coleta itens, fases 3/5), "Mercador Finn" (desconto na loja + side quest de moedas, fases 5/7).
- Flags de diálogo por NPC já persistem em save v3 (dialogFlags no JsonParser).

## Testes existentes que precisam continuar passando
ObjectivesVariadosTest 20/20, ShopQaTest 19/19, MenuNavigationTest 12/12, PhaseTransitionTest 24/24, AutoValidate 24/24, StoryNpcPlacementTest 39/39, WaypointFrameTest OK, TransitionCooldownTest 15/15.

## Comandos
- Build: `cd /home/ubuntu/First-game-in-java && javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep error | head -3; echo BUILD_OK`
- Teste: `out=/tmp/test_X && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/X.java 2>/dev/null && DISPLAY=:120 timeout 90 java -cp $out:bin:res X 2>&1 | tail -1`
- PR: `gh pr create --title "Rodada 22 — ..." --body ... --base main`
- Branch: manus/rodada22-trilha-npcs-inventario (já criada e pushed)

## PR #34/#35 referências
- PR #34: roda 20 (mergeado). PR #35: roda 21 (mergeado, f298a55). PR atual a criar será #36.
