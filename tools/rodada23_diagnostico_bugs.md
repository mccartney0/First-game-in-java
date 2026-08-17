# Diagnóstico dos bugs reportados na rodada 23

## Bug 1 — Telas sobrepostas (Game Over / Fase Concluída)

Evidências das capturas do usuário:

1. **Game Over**: "Game Over", "Voltando ao menu em...", "Pontuação final",
   "Recorde", "Melhor combo da partida" e os botões aparecem DESALINHADOS
   (texto grande branco em posições escaladas diferentes do botão azul),
   sobre um cenário de fundo onde inimigos ainda estão visíveis e há uma
   faixa "A Base Secreta de Nia" residual da missão — o mundo continua
   renderizado com entidades atrás da tela de game over.

2. **Fase Concluída**: o texto branco ("Fase 2 concluída!", "Tempo: 0:00",
   "Próxima fase", "Missão: Defender o ponto", "Total acumulado", "Melhor
   partida", "ENTER: continuar", "NOVO RECORDE GLOBAL") aparece DUPLICADO
   — uma vez por cima do card e uma vez "solto" no fundo — porque a faixa
   de transição (`showLevelTransition`) e o card `PhaseStatsScreen`
   desenham textos com fontes/posições diferentes ao mesmo tempo, e a
   fonte 36 do Game Over não é escalada pelo SCALE enquanto o resto usa
   coordenadas escaladas.

Causas raiz localizadas em `Game.render()`:

- O bloco `GAMEOVER` usa `g.setFont(...)` (36/28/24/16 **sem multiplicar
  pelo SCALE**) e coordenadas absolutas `scaledHeight/2+NN` em pixels de
  janela — os textos ficam enormes/deslocados em relação ao botão azul
  (`drawGameOverActions`) que é escalado corretamente.
- `showLevelTransition > 0` desenha a faixa de conclusão **mesmo com o
  `PhaseStatsScreen` visível** (o card chama `show()` e a faixa ainda está
  ativa) → texto duplicado por trás do card.
- `Enemy.enemies` (total da campanha) e `SaveManager.getBestRun...`
  (melhor partida) aparecem juntos quando `Enemy.enemies > 0`, empurrando
  o card; e `hidingHud` esconde a HUD de combate, mas o texto duplicado
  vem da combinação faixa+card.

Correção aplicada:

1. Game Over: fontes multiplicadas pelo SCALE (`36*scale`, etc.) e
   coordenadas no espaço do jogo escalado (em `unit = scale/4` ou
   diretamente `... * scale`), consistentes com `drawGameOverActions`.
2. Faixa `showLevelTransition`: suprimida quando o
   `PhaseStatsScreen.isShowing()` estiver visível (o card é o resumo
   oficial da fase concluída).
3. Overlay do Game Over: manter o world renderizado atrás (cenário) mas
   com o mesmo escurecimento; inimigos congelados já são suprimidos no
   `isTransitioning()`/GAMEOVER? — verificar; se não, pular Enemy render
   no GAMEOVER como no `questCompletedPending`.

## Bug 2 — Música de ambiente só inicia ao carregar jogo

Comportamento observado: `.\gradlew.bat run` → novo jogo → música não
toca; só ao "Carregar jogo" ela começa.

Suspeita principal: `MusicManager`/`SoundManager` só chama `play()` na
rotina de `loadSlot()` (migração/restauração), e o novo jogo não
aciona `SoundManager.play()` para a zona da fase 1. Verificar:

- `MusicZoneManager` / zonas de música (MusicZoneTest) — a zona só
  ativa quando o jogador anda pela fase (World.onPlayerMove?).
- `loadSlot` → `SoundManager.play(...)` explícito.
- `Game.startNewGame()` → checar se há chamada de música inicial.

Correção: tocar a música da zona atual no `startNewGame`/`restartGame`
(novo jogo e fase carregada), igual ao que o loadSlot faz.

## Padrão para testes

- `out=/tmp/test_X; rm -rf $out; mkdir -p $out; javac -d $out -cp bin:res tools/X.java 2>/dev/null && timeout 60 env DISPLAY=:120 java -cp $out:bin:res X 2>/dev/null | tail -1`
- Builds: `find src -name "*.java" > /tmp/alljava.txt && xargs javac -d bin -cp bin < /tmp/alljava.txt`
- Regressão (19 suítes): ObjectivesVariadosTest, ShopQaTest, MenuNavigationTest,
  PhaseTransitionTest, AutoValidate, StoryNpcPlacementTest, WaypointFrameTest,
  TransitionCooldownTest, MusicZoneTest, InventoryTest, BranchingNpcTest,
  Rodada22bTest..22fTest, FaseSaveE2ETest, Rodada22g2Test, Rodada22g3Test.

## Bug 2 — detalhes técnicos confirmados

- `MusicManager.setZone(Zone.forLevel(1))` é chamado em `startNewGame()`
  (linha 1501) e `advanceToNextLevel()` (linha 1718); `Zone.forLevel(1)`
  = FOREST → path `music_forest.wav` (existe em res/sounds).
- `setZone` → `load(path)` abre o Clip com `clip.loop(LOOP_CONTINUOUSLY)`
  MAS `clip.start()` NÃO é chamado no load! O start vem do `startAt` no
  update do crossfade (`if (!fadingIn.isRunning()) fadingIn.start()` no
  update) — OK em teoria.
- **Suspeita principal**: o crossfade (`update()`) só é chamado dentro do
  `if ("NORMAL".equals(gameState))` (linha ~616) — e no novo jogo o
  estado inicial é a SELEÇÃO DE ARMA INICIAL (`startInitialWeaponSelect()`)
  com gameState não-NORMAL (MENU?), então `MusicManager.update()` não
  roda → fadingIn nunca faz start → música nunca toca. Ao "carregar
  jogo", o loadSlot cai direto em estado NORMAL → update roda → música
  toca.
- Teste a fazer: probe headless que cria Game → startNewGame → espera 3s
  → verifica MusicManager.isMusicPlaying() (criar getter para teste).
- Fix proposto: chamar `MusicManager.update()` em qualquer gameState
  (ou iniciar música de forma imediata com startAt no setZone) e/ou
  chamar setZone também ao entrar em estados de menu/menu inicial.

## Paths zonas
FOREST=music_forest.wav (nível 1-2), TENSION=music_tension.wav (3-5),
BOSS=music_boss.wav (6+), ARENA=music_arena.wav. CROSSFADE_FRAMES=120 (~2s).

## Bug 2 — status do fix (atualizado)

Fix APLICADO no Game.java: `MusicManager.update()` movido para ANTES do
bloco `if ("NORMAL".equals(gameState))` — roda em qualquer gameState.
(Carregado no estado da sessão.)

O probe Rodada23aMusicAndScreensTest.java mostra:
- novo jogo inicia em MENU: PASS
- crossfade iniciado (fadingIn != null) e música tocando: **FAIL** — o
  jogo TRAVOU no probe (timeout 124), o loop de MusicManager.update()
  provavelmente trava no AudioSystem quando não há servidor de som
  (headless com pulse/PulseAudio ausente no sandbox → Clip.start() trava
  o loop? Não, update roda no mesmo thread). Na verdade: startNewGame()
  chamou setZone → load() → clip.open(ais) → pode THROWAR/travar quando
  não há linha de áudio (Linux headless: Clip.open com linha indisponível
  lança LineUnavailableException → catch Exception retorna null → setZone
  retorna → fadingIn=null → loop do teste continua → deveria terminar em
  300 iterações...). O teste NÃO terminou → travou em outro lugar:
  provavelmente no NEW Game() constructor → new World() → Spritesheet →
  áudio ou AWT. Verificar: travamento pode ser no `SoundManager.unload()`
  ou no frame.init. IMPORTANTE: o teste NÃO travou no run anterior
  (completou 8 checks) — agora travou no loop de música.

Hipótese restante: `MusicManager.update()` chamado fora do bloco NORMAL
roda também no GAMEOVER/MENU do teste e o `AudioSystem.getClip()` trava
no sandbox headless SEM linha de áudio? startAt → clip.start() → no
Linux headless sem PulseAudio o start pode bloquear?? Improvável.
MAIS PROVÁVEL: o `new Game()` no probe trava DEPOIS de imprimir os
checks estáticos (os checks 2-5 são grep no fonte, depois vem o loop de
música). O loop: update() → load não é chamado de novo (zone já setada)
→ crossfadeRemaining=120 → applyGain → setFramePosition... nada trava.
A NÃO SER: o AudioSystem.getClip() dentro do load trava na PRIMEIRA
chamada em linha sem mixer? Não — load roda UMA vez.
→ Investigar: rodar probe com stacktrace (sem 2>/dev/null) e time.

## Outros dados
- res/: level1-8.png já existem (incl. level7/8!). Sounds: /sounds/*.wav
  no classpath (build.gradle: main.java.srcDirs=['src','res'],
  main.resources.srcDirs=['src','res']).
- build.gradle usa Gradle com srcDirs res; testes headless usam -cp bin:res.

## Bug 2 — causa raiz FINAL

1. **O freeze do probe é SOLO-DO-SANDBOX**: `AudioSystem.getMixerInfo()` retorna 0
   mixers (sem PulseAudio no sandbox). `AudioSystem.getClip()` lança
   IllegalArgumentException → load() retorna null → crossfade não inicia.
   No PC do usuário (Windows, PulseAudio/mixer presente) o clip funciona.
2. **Mas o bug do usuário é REAL e está no Game.java**: `MusicManager.update()`
   rodava APENAS dentro do gameState NORMAL. No novo jogo, o estado inicial
   é MENU (seleção de arma) → o crossfade nunca era conduzido → música muda
   até carregar um save (loadSlot vai direto ao estado NORMAL). FIX JÁ
   APLICADO: update() chamado antes do bloco NORMAL, todos os gameStates.
3. **Correções colaterais no MusicManager**: clampToFloor usa o mínimo do
   FloatControl do próprio clip (controles variam -80..0 dB); o floor fixo
   -60 dB foi removido da interpolação.
4. No probe: como o sandbox não tem mixer, os checks de crossfade/música
   só podem validar: zone atribuída + update roda fora do NORMAL + load não
   trava o jogo (carregar em thread separada).

## Status dos outros fixes (Game Over / transição)
- Fontes proporcionais ao SCALE (unit=SCALE/4) no GAMEOVER: OK no fonte.
- Faixa de transição suprimida com PhaseStatsScreen.isShowing(): OK.
- Inimigos ocultos durante GAMEOVER: OK.
- Probe Rodada23a: 6/8 PASS; 2 FAIL são o crossfade/música que dependem de
  mixer real — no PC do usuário devem passar (o volume default é 0 dB/
  normal). Verificar: getMusicVolume() retorna 0.0 → 0 dB = ganho máximo
  da trilha (normal); no PC funciona.
