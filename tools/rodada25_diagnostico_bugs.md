# Diagnóstico — bugs reportados 17/08/2026 (pós-merge 24a/24b)

## Bug 1: Diálogo sobreposto (imagem do usuário, fase 2, Lila)
- Painel: `panelHeight = 108*scale/4+20` = 128px (scale=4). Texto inicia em `panelY+48`, linha ~19px+2.
- Escolhas longas ("Por que não vai você?") quebradas em 2 linhas (linha 335-340) SEM limitar `choicesLineY` ao painel → escrevem por cima do rodapé e fora do painel.
- Rodapé em `panelY+panelHeight-12` é desenhado SEMPRE → se texto+escolhas > painel, o rodapé fica em cima das escolhas (sobreposição visível na imagem).
- Também: footerHint "Digite 1-3 para escolher, Enter para a primeira" desenhado em cima do texto de progresso quando escolhas vazias? Não — rodapé ok, o problema é conteúdo excedendo.
- FIX planejado: calcular `remainingHeight`; se texto+escolhas ultrapassarem, encerrar o conteúdo dentro do painel e desenhar o rodapé SEMPRE por último (após conteúdo clamped). Melhor: redimensionar painel pela necessidade (mín 128, máx ~260) OU truncar texto de escolha na largura com reticências e limitar linhas.

## Bug 2: Beacon travado na fase 2
- Objetivo "defender o porto / localizar beacon": verificar fluxo do nível 2 em StoryManager/QuestManager — o avanço exige "matar tudo e falar com todos" (Ava? Rex?). Ver objectiveState, checkCompletion por fase 2.
- Suspeita: spawn dos secundários em fase 2 (Rex) em tile @(0,0)?? Não — fix do canto do spawn. Checar se Rex aparece na fase 2 e se o objetivo exige falar com ele.

## Bug 3: Mobs alterados após restart pós-morte
- loadGameFromSave (fix 24a) tenta saves.json; carrega nível salvo. Mas ao reiniciar, o mapa pode gerar inimigos diferentes (World.restartGameFromFile recria entidades) — verificar se entities da campanha têm spawn determinístico (fixo do mapa PNG) vs procedural.
- Possível problema: autosave do restart carrega save antigo enquanto WaveManager/enemiesMortos ficam, duplicando? Ver loadGameFromSave linhas ~1477-1502 do Game.java.

## Suítes novas planeadas (Rodada24c virou fix — usar PR "rodada25" ou rod 24c como fix)
- Teste diálogo: render com escolhas longas não ultrapassa painel; rodapé não sobreposto.
- Teste restart pós-morte: loadGameFromSave retoma o nível exato e inimigos determinísticos.

## ACHADOS DETALHADOS (verificados no código)

### Bug 1 — Diálogo sobreposto (CONFIRMADO causa raiz)
- `DialogueManager.render()` (linha 266-354): painel fixo `panelHeight=128` (scale=4). Texto inicia em `panelY+48`, linha ~19px+2 (name+26, body lineY += getHeight()+2).
- Escolhas longas (ex.: Lila "Não posso agora"/"Por que não vai você?") quebram em 2 linhas SEM limitar `choicesLineY` ao painel → escreve por baixo do painel e por cima do rodapé (desenhado fixamente em `panelY+panelHeight-12`).
- Texto do corpo também pode ter 4+ linhas e colidir com rodapé.
- FIX: redimensionar o painel pela necessidade (mín 128; altura = 16(nome) + 48(topo) + N linhas*21 + rodapé 24 + margem), clamp no máximo ~90% da tela; rodapé SEMPRE desenhado após o conteúdo e nunca por cima.

### Bug 2 — Beacon travado (causa provável IDENTIFICADA)
- Fase 2 = `DialogueObjective(Sequence(HoldObjective, BossHuntObjective), "Engenheira Nia")`.
- `HoldObjective.onLevelLoaded()` cria beacon programático em (17,11) SE `QuestManager.getCurrentLevel()==2` E tile válido (linha 87-107).
- `HoldObjective.onLevelStart()` LIMPA `trackedBeacons`, `channel=0`, `spawned=false` (linha 57-63) — é chamado por `QuestManager.prepareForLevel` a cada troca de nível.
- BUG CANDIDATO forte: no restart pós-morte via `loadSlot`: `World.restartGame("level2.png")` → `placeStoryNpcs` → cria os 3 secundários (fases 2-8, rod 24a) e os reposiciona; MAS `placeStoryNpcs` é chamado DEPOIS do World... O beacon da fase 2 é criado em `onLevelLoaded` — verificar SE `onLevelLoaded` é chamado após restart de save. A ordem: `World.restartGame → placeStoryNpcs → prepareForLevel?`. No loadSlot: `World.restartGame` (linha 494) e depois NADA chama prepareForLevel novamente — o `prepareForLevel(savedLevel)` NÃO é chamado no loadSlot! `QuestManager.setCurrentLevel` deve existir? Ver `setCurrentLevel` no QuestManager. Se `onLevelLoaded` não é disparado → beacon NUNCA é criado → "Localize o beacon do setor" PARA SEMPRE. O deserializeState recria beacons só se o estado salvo tiver BEACONS não vazios (mas no inicio, channel=0 e beacon recém criado e ainda não ativado → serializeState salva BEACONS=x,y → restore recria — só se o autosave aconteceu DEPOIS do beacon spawn e ANTES da morte).
- CENÁRIO usuário: morreu cedo na fase 2 (antes de ativar o beacon, channel salvo=0, BEACONS=vazio no serialize — beacon está em trackedBeacons mas éActivated=false → entra no serialize!) → BEACONS salvo; reload recria. OK. Mas se a Lila/Rex aparecem na fase 2 (24a!) e o usuário FALOU com a Lila → side quest "lila_collect_2" ativada; o objetivo "falar com todos"? Não — fase 2 não exige secundários.
- OUTRO SUSPEITO: `spawnSecondaryNpcs` cria Lila na fase 2? inCampaign=2..8 → SIM cria Rex, Lila, Finn na fase 2 (24a mudou: antes Lila era fase 3+!). A imagem do usuário mostra Lila na fase 2 com bug de diálogo → confirmado spawn 3 NPCs na fase 2. O objetivo diz "Engenheira Nia" mas talvez a meta mostrada diga "falar com todos"? Ver getObjectiveText.
- TRAVADO: "fiz tudo e ficou bugado... parou na missao do beacon" → depois de defender o beacon e avançar, a fase NÃO avança para BossHunt? Ver `SequenceObjective` e `DialogueObjective.isComplete/onBeaconActivated`.

### Bug 3 — Mobs alterados após restart (causa provável)
- `loadSlot` linha 482: `Enemy.enemies = savedEnemies;` (contagem global de mortos).
- Mapa nível 2: inimigos do PNG fixo + WaveManager spawna ondas? Ver WaveManager na campanha: `startArena` é só infinito. Campanha usa inimigos do mapa (applyMapPixels cria Enemy).
- `World.restartGame` limpa `Game.entities` (linha 325-327). `placeStoryNpcs` recria NPCs.
- `applyDifficultyScalingForCurrentLevel` escala vida/dano por fase — determinístico.
- CANDIDATO: `WaveManager.reset()` + restart: se o usuário matou N inimigos, `Enemy.enemies=savedEnemies` restaura contagem mas os inimigos do mapa são recriados todos de novo → ao morrer/restartar, TODOS os mobs voltam ao mapa? O save grava `inimigosMortos` mas o restart não mata os já-mortos! O jogador vê mobs que já tinha matado re-aparecerem = "mobs da sala modificados".
- FIX possivel: salvar/Restaurar posições dos inimigos mortos (kill set) ou manter inimigos mortos como "corpos/gravestones". Simples: serializar lista de inimigos mortos (identificados por posição+tipo) e ao recriar o mapa, pular os já mortos. Complexidade média. Alternativa mínima: documentar? Não — usuário reclamou.

### Plano de fixes (rodada 25 — usar branch rodada25/PR 40):
1. DialogueManager.render: painel dinâmico + clamp; suíte `Rodada25DialogTest`.
2. Fase 2 beacon: investigar fluxo completo com suíte `Rodada25Phase2Test` (simular DialogueObjective→falar com Nia→HoldObjective→ativar→defender→completo→BossHunt; restart pós-morte no meio retoma estado). Corrigir onLevelLoaded desparado no restart de save.
3. Inimigos mortos não ressuscitam: serializar kill set por posição e recriar; suíte `Rodada25KillPersistenceTest`.
4. Regressão 28/28 + novas suítes.

### Nota fluxo autosave
- autosave roda no GAMEOVER (linha 662). saveCurrentGame → serializeObjectiveState → save. No loadSlot → world restart → restoreObjectiveState (line 535) chama QuestManager.deserializeObjectiveState → onLevelLoaded já passou no restartGame ANTES do restore! → HoldObjective.onLevelLoaded criou beacon, depois deserializeState restaura canal mas re-recria beacon se BEACONS vazio?? `onBeaconSpawned` foi chamado na criação do beacon → trackedBeacons tem 1 → `reconnectRestoredBeacons` retorna true → não cria segundo. OK. MAS o `deserializeState` do HOLD restaura `channel` do save. Se o autosave salvou channel=0, ok.
- PROBLEMA real provável: `onLevelLoaded` do HoldObjective é chamado pelo World.restartGame (que chama placeStoryNpcs... verificar quando onLevelLoaded dispara — procurar quem chama onLevelLoaded).

## CAUSA RAIZ CONFIRMADA — beacon travado + mobs alterados
`World.restartGameCommon` chama `QuestManager.prepareForLevel(levelNumber)` (linha 341) → `currentObjective = createObjectiveForLevel(level)` → **cria um NOVO objetivo do zero e chama `onLevelStart()` que zera canal/beacons!** Isso acontece no `loadSlot` (restart após morte), porque o `World.restartGame` roda na linha 494 do SaveManager, ANTES de `restoreObjectiveState` (linha 535).

Sequência no restart pós-morte:
1. `World.restartGame("level2.png")` → `prepareForLevel(2)` → **novo HoldObjective com canal=0, beacon programático criado em (17,11) e registrado**.
2. `placeStoryNpcs()` → cria Rex+Lila+Finn (fases 2-8) e reposiciona.
3. `restoreObjectiveState` → `deserializeObjectiveState` → restaura canal salvo (ex.: 0) — OK, mas:
4. **O beacon programático criado no passo 1 já está em `trackedBeacons`** — `reconnectRestoredBeacons` retorna true, não recria. Até aqui ok.

PROBLEMA REAL: o novo `currentObjective` substitui qualquer estado anterior; `restoreObjectiveState` restaura o canal, MAS o `onLevelLoaded()` do novo objetivo JÁ criou o beacon — e `deserializeState` depois faz `recreateRestoredBeaconsNow` só se BEACONS pendentes; e o beacon novo criado no passo 1 pode estar em tile (17,11) válido — ok.

PORÉM o usuário diz que trava "no beacon": se ele morreu SEM falar com a Nia, o objectiveState salvo tem canal=0 e BEACONS=(17*16,11*16) salvo. Restart: beacon recriado duas vezes? O beacon do passo 1 e o restored — `trackedBeacons` já contém o do passo 1 → `reconnectRestoredBeacons` acha spawned=true e NÃO recria. Único beacon. Canal restaurado 0. OK??

SEGUNDO PROBLEMA: `restoreObjectiveState` só restaura o ESTADO, não re-fala. Mas `DialogueObjective.talkedToTarget` é recriado falso (novo objeto)! Se o usuário JÁ tinha falado com a Nia e estava defendendo o beacon, o novo DialogueObjective exige falar com a Nia DE NOVO → bloqueia avanço! `SequenceObjective` só avança estágios quando o interno completa; o `isComplete` do DialogueObjective = talkedToTarget && delegate completo. Se talked=false → progresso mostra "Fale com a Engenheira Nia" → o jogador "trava" achando que é o beacon.
→ FIX: salvar também talkedToTarget (talked) no objectiveState e restaurar no DialogueObjective (serializable/deserializable). QuestManager.deserializeObjectiveState precisa mapear por tipo de objetivo. Ver serializeState do DialogueObjective — existe?

## ANÁLISE FASE 2 — fluxo de restart
DialogueObjective.serializeState salva "TALKED=...;DELEGATE=IDX=...|S0=...|S1=..." → restore funciona (TALKED restaurado, índice restaurado). Estado de Hold restaurado. → No restart, o progresso de missão é PRESERVADO corretamente pela arquitetura.

MAS há 3 gaps reais:
1. **Inimigos mortos do mapa voltam**: `restartGameCommon` limpa entities e o mapa PNG recria TODOS os inimigos do mapa. `Enemy.enemies=savedEnemies` é só um contador global. O jogador vê mobs já mortos reaparecerem = "mobs da sala modificados". FIX: serializar kill set por posição (x,y,variant) em saveCurrentGame; ao recriar inimigos em applyMapPixels, pular os cujas posições estão no kill set; limpar kill set na troca de fase e no onLevelStart.
2. **Beacon/objetivos no restart de save**: ok na arquitetura, mas validar com teste E2E (usuário morreu antes de falar com Nia → restart → deve retomar).
3. **Dialogo sobreposto** (fix render).

Decisão: 3 fixes + 3 suítes. Fase 2 travamento provável: usuário morreu, ao restart beacon recriado mas o autosave salvou com canal 0 e a HUD mostra "Localize o beacon do setor" se beacon não rastreado — verificar se `trackedBeacons` pós-restart contém o beacon (spawned via onLevelLoaded antes de deserialize? ORDEM: restartGame → prepareForLevel → onLevelLoaded (cria beacon, onBeaconSpawned) → restoreObjectiveState. OK → não trava. O travamento real pode ser: Lila/Rex/Finn spawnam na fase 2 e o jogador gasta tempo falando com eles; a zona de defesa (raio 90) fica cheia de inimigos do mapa perto do beacon (17,11)→ canal nunca avança? Ver mapa level2: densidade de inimigos perto do centro? Se o jogador não afasta os mobs da zona, "Defenda! N invasores" fica eterno — mas não é bug de código.

→ Priorizar: kill persistence (2) + dialog render (1) + teste E2E fase 2 restart (3).

## PROGRESSO RODADA 25 (fixes implementados, pendente: testes + regressão + commit/PR)
Branch: ainda na `main` local (rodadas 24a/24b mergeadas). Plano: branch `rodada25` a partir da main + PR #40.

### Implementado (todas as edições já compiladas com sucesso):
1. `src/com/traduvertgames/dialogue/DialogueManager.java` — render com dois passes: countWrapLines antes, painel redimensionado (mín 128, clamp screenHeight-32), drawWrappedLines/wrapText helpers, rodapé desenhado por último em footerY = panelY+panelHeight-12. COMPILED OK.
2. `src/com/traduvertgames/main/EnemyKillTracker.java` (novo) — kill set por tile (chave "x,y,B|N"), markDead no destroySelf, isAlreadyDead consultado no applyMapPixels (todos os pixels de inimigo: FF0000, FF9C27B0, FF00BCD4, FF3F51B5, FF009688, FFF4511E, FFE91E63, FF7986CB, FF81C784, FFFF5722, FFBF360C, FF74DE80, FFFFC800). COMPILED OK.
3. `src/com/traduvertgames/main/Game.java` — resetLevelStats: EnemyKillTracker.reset() + setCurrentLevel(CUR_LEVEL). COMPILED OK.
4. `src/com/traduvertgames/main/SaveManager.java` — saveCurrentGame grava "inimigosMortosSet"; loadSlot restaura (setCurrentLevel(savedLevel)+deserialize) após definição de savedLevel. COMPILED OK.

### Arquivos de teste:
- `tools/Rodada25DialogTest.java` criado (render buffer, painel cresce, rodapé dentro, sem vazamento).
- Pendentes: Rodada25KillTest (matar inimigo → restart → inimigo não volta) e regressão 30/30.

### Comandos úteis:
- Compilar: find src -name "*.java" > /tmp/alljava.txt && xargs javac -d bin -cp bin < /tmp/alljava.txt
- Teste: out=/tmp/test_X && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/X.java 2>/dev/null && DISPLAY=:120 timeout 120 java -cp $out:bin:res X 2>&1 | tail -2
- Xvfb: DISPLAY=:120 ativo. Saves: rm -f saves.json save.txt antes dos testes.
- Build usuário Windows: .\gradlew.bat run

### Suítes regressão base (28): ObjectivesVariadosTest ShopQaTest MenuNavigationTest PhaseTransitionTest AutoValidate StoryNpcPlacementTest WaypointFrameTest TransitionCooldownTest MusicZoneTest InventoryTest BranchingNpcTest Rodada22bTest Rodada22cTest Rodada22dTest Rodada22eTest Rodada22fTest FaseSaveE2ETest Rodada22g2Test Rodada22g3Test Rodada23aMusicAndScreensTest Rodada23bTest Rodada23cBalanceTest Rodada23dPostCampaignTest Rodada24aReportTest Rodada24aDeepLoreTest Rodada24aSideQuestTest Rodada24bEliteTest Rodada24bDeepRecordTest
+ Rodada25DialogTest, Rodada25KillTest (novas)

## DEBUG KILLTEST (atualizado):
- Save funciona (probe3 provou): "inimigosMortosSet":"..." gravado, loadRoot parse OK.
- Teste atual: 12/14 pass. tracker PERSISTE após loadSlot (loadSlot re-aplica tracker após World.restartGame — CORRIGIDO).
- RESTAM 2 FAILS: "Inimigo abatido não ressuscitou no reload" e "Tile do abatido vazio no reload" — ou seja, o inimigo do tile alvo AINDA aparece no reload mesmo com tracker populado.
- Hipótese: Enemy.getX()/getY() pode retornar posição NÃO alinhada à grade (float). markDead usa tile = getX()/16 (int div). Mas applyMapPixels consulta com xx,yy do PIXEL do mapa (inteiro) — se o Enemy foi criado em xx*16, yy*16 → getX()/16 == xx desde que getX() retorne xx*16. POSSIVEL: getX() pode mudar entre criação e marcação (inimigo se moveu!). destroySelf usa getX()/getY() ATUAIS (movidos) ≠ tile original do mapa → tracker registra tile errado, reload não pula.
- FIX PROPOSTO: Enemy deve guardar tileX/tileY de spawn (final) e usá-los no markDead. E no teste usar tile estático. Verificar Enemy tem campos spawnXY ou usar getX()/getY() no construtor.
## ATUALIZAÇÃO FINAL (bugs resolvidos e verificados)

### Bug 3 — Causa raiz CONFIRMADA (tile do beacon é parede no level2.png)
- `level2.png` (34×22): preto = chão, branco = PAREDE. Tile designado (17,11) do beacon é BRANCO → `isWallTile(17,11)=true` → `onLevelLoaded` do `HoldObjective` NUNCA cria o beacon → missão trava em "Localize o beacon do setor" (percebido pelo jogador como travamento após morrer/reiniciar, mas existe desde a primeira carga).
- Inimigos do level2 (tiles 8,9 / 26,9 / 8,12 / 26,12) estão a 120+ px do beacon (raio 90) → zona limpa.
- FIX: `findNearestFloorTile` em raio crescente procura chão válido mais próximo.

### Resultados dos testes (rodada 25)
- Rodada25DialogTest: 7/7 PASS (painel dinâmico, rodapé dentro).
- Rodada25KillTest: 14/14 PASS (kill persistence).
- Rodada25BeaconTest: 15/15 PASS (beacon em chão válido, canal avança, reload restaura posição+canal, tracker integrado).
- Probe `tools/_ProbeBeacon.java` e `_ProbeBeacon2.java`: removidas ao final.

### Estado antigo (não sobrescrever)
- Teste rodado: DISPLAY=:120 timeout 60 java -cp bin:res:$out Rodada25KillTest (cp com bin PRIMEIRO importa: classes $out com precedência para a classe do teste; bin dá classes do jogo)
- DialogTest: Rodada25DialogTest.java criado — usar BranchingNpc(anon, getNodes protegidos?) VERIFICAR API BranchingNpc antes de compilar (startNearestDialogue, getBranchChoices, advance, close, isActive, render(Graphics2D) — checar se render existe).

## RODADA 25 — STATUS ATUAL (atualizado)

### Bugs do usuário (relatados agora):
1. Diálogo da Lila com TEXTO SOBREPOSTO (escolhas longas escrevem sobre o rodapé 1/1) — FIXADO em DialogueManager.render (painel dinâmico, rodapé sempre no fim). Suíte: tools/Rodada25DialogTest.java (COMPILAR E VALIDAR — API BranchingNpc verificar: startNearestDialogue, advance, close, isActive, render(Graphics2D g)).
2. Beacon trava após reinício — causa raiz encontrada (prepareForLevel recria objetivo e perde canal; flow analisado; ver se precisa de fix: o restoreObjectiveState no loadSlot já restaura — mas no RESTART do jogo (Game.update consume restartGame flag) não usa loadSlot! Ver Game.update linha ~1400 "handleGameOverRestart" → chama World.restartGameFromFile? Se não passar por loadSlot, a missão recomeça do início → beacon trava. VERIFICAR E FIXAR: fazer o restart pós-morte usar loadSlot/restoreObjectiveState.
3. Mobs "mudam"/ressuscitam após morrer e reiniciar — implementado EnemyKillTracker (rodada 25):
   - tools/_ProbeKill.java (probe, apagar antes do commit)
   - EnemyKillTracker.java criado: markDead/isAlreadyDead/serialize/deserialize/reset/deadCount/setCurrentLevel; chave "x,y,N|B"; grave em SaveManager chave "inimigosMortosSet" (slot.put antes da session ser construída, pois purge remove keys duplicadas).
   - Enemy.destroySelf usa spawnTile.x/spawnTile.y (tile de SPWN — getX() muda com movimento!). spawnTile já existia (Vector2i spawnTile).
   - World.applyMapPixels: skips em 0xFF0000/9C27B0/00BCD4/3F51B5/009688/F4511E/E91E63/7986CB/81C784 (13 isAlreadyDead no src).
   - loadSlot: setCurrentLevel+deserialize APÓS World.restartGame (linha ~506) pois resetLevelStats zera o tracker.

### MISTÉRIO DO COMPILADOR (RESOLVER):
- bin/com/traduvertgames/world/World.class NÃO contém EnemyKillTracker mesmo após javac isolado (verbose mostra "parsing started" do src correto, exit=0).
- Suspeita: Java 21 javac com -cp bin não recompila pois usa bytecode antigo? NÃO — tamanho idêntico 11985 após rm.
- PRÓXIMO TESTE: `javac -d /tmp/wtest -sourcepath src -cp bin src/com/traduvertgames/world/World.java` com -d outro diretório; verificar bytecode lá. Se OK → bug no sistema de arquivos/bin (cache overlay?). Se igual → algo no src mesmo (ex.: código dentro de bloco não alcançável não é o caso; talvez o javac resolve isAlreadyDead como erro silencioso? Não, exit=0).
- Teste Rodada25KillTest: 12/14 pass (fail: enemy ainda aparece no reload, pois World.class antigo).
- Rodada25DialogTest: ainda não compilada.
- Após fixar compilador: rodar as 2 suítes novas + regressão 28/28.

### Build que funcionou antes (regressões verdes na main):
`cd /home/ubuntu/First-game-in-java && find src -name "*.java" > /tmp/alljava.txt && javac -d bin -sourcepath src @/tmp/alljava.txt` (sem -cp bin).

### Fluxo do usuário (Windows): .\gradlew.bat run — gradle usa build/classes/java/main (ATENÇÃO: há build/classes/java/main/com/traduvertgames/world/World.class DUPLICADO — quando o usuário buildar, o gradle recompila do src; no sandbox o teste usa bin:res, não build/).

### Suítes novas da rodada 25 (a commitar):
- tools/Rodada25DialogTest.java (texto diálogo não sobreposto; painel dinâmico; rodapé na base)
- tools/Rodada25KillTest.java (E2E: matar, save, loadSlot, inimigo não volta)
- src/com/traduvertgames/main/EnemyKillTracker.java (novo)
- Apagar antes do commit: tools/_ProbeKill.java, tools/_ProbeKill2.java, tools/_ProbeKill3.java, /tmp/probes

## DESCOBERTA COMPILADOR (RESOLVIDO):
O bytecode bin/World.class AGORA contém as 10 chamadas EnemyKillTracker.isAlreadyDead (verificado via javap -c -p > /tmp/world.bytecode.txt; o grep -c anterior falhava por padrão case-sensitive no pipe). A causa do "bytecode antigo": o javac não recompilava World.class quando o EnemyKillTracker.class estava presente no -cp bin (cache de resolução de classes — ao remover EnemyKillTracker.class e recompilar, o javac recompilou corretamente).

## NOVA DESCOBERTA (PROBE AINDA FAILING):
Com classes na MESMA classloader (probes compilados em bin, java -cp bin:res): carga2 ainda = 11 inimigos e o tile 8,9 reaparece como SCOUT. O killSet no runtime contém [8,9N] antes do restart (verificado).
Hipóteses restantes:
1. O pixel do mapa em 8,9 PODE SER o WARBRINGER (boss=true) — killSet tem 8,9N e o tile tem boss pixel → não pula. Verificar o PNG level2.png pixel 8,9 (ler BufferedImage e imprimir pixels ao redor).
2. ensurePhaseBoss recria WARBRINGER no tile do jogador aleatório? Não, distante do spawn.
3. O WARBRINGER do mapa (17,10 boss) + WARBRINGER extra? Não.
PRÓXIMO: dump dos pixels do level2.png na posição 8,9 e 17,10; e listar os pixels que criam inimigos no PNG.

## FIXES DE TESTE:
Compilar probes dentro de bin (não $out) para evitar dupla classloader de EnemyKillTracker: `javac -d bin -cp bin tools/_ProbeKill3.java`.

## KILL TEST — DESCOBERTA FINAL (resolvido como TESTE, código ok):
- O World.class AGORA contém as 13 chamadas isAlreadyDead (javap -p bin/... mostra 13; bytecode correto: ldc 65536 -65536; if_icmpne goto skip).
- O "killSet vira [] após restart2" no probe manual: porque o probe chamava World.restartGame DIRETO sem loadSlot — resetLevelStats zera o tracker (Game.java:311). NO JOGO REAL o restart usa loadSlot, que reaplica o tracker APÓS o restart (SaveManager linhas 506-507). Código correto.
- O Rodada25KillTest (12/14) ainda fail nos 2 checks do reload: o teste verifica "alvo = Game.enemies.get(0)" da carga 1 (ARTILLERY 8,9) e depois "mortoApareceu" varre todos enemies do reload procurando tile do alvo. PASS "Tracker persiste" mas fail "inimigo não ressuscitou".
- CAUSA PROVÁVEL DOS 2 FAILS: o teste roda java -cp $out:bin:res (classes $out primeiro) — o teste em si está em $out, mas EnemyKillTracker/World vêm de bin. PORÉM: ao rodar `javac -d $out -cp bin:res tools/Rodada25KillTest.java`, o javac pode ter COPIADO EnemyKillTracker.class antigo para $out?? Não (javac -d $out só grava a classe do teste).
- MAIS PROVÁVEL: o alvo.get(0) = ARTILLERY tile 8,9; killSet tem 8,9N; no reload o applyMapPixels pula 8,9... mas o WARBRINGER do mapa (17,10) + ensurePhaseBoss não adiciona (mapHasBoss) → 10 inimigos. O teste diz "mortoApareceu=true" — algum inimigo com tile (8,9) existe. VERIFICAR: no reload, get(0) pode ser o WARBRINGER 17,10; o tile do alvo (8,9) tem SCOUT se o mapa tem SCOUT em 8,9?? O dump mostrou: pixel 8,9=FF0000 (spawnRandomVariant → SCOUT na carga1; ARTILLERY também vem de outro tile? NÃO: ARTILLERY tile 8,9 na carga1 = pixel 0xFF00BCD4 em 8,9?? mas dump mostrou 8,9=FF0000! CONFLITO: o dump de pixels especial listou 8,9=0xFFFF0000 e 17,10=0xFFE91E63 — mas a carga1 mostra ARTILLERY 8,9 e WARDEN 11,7 etc. O PNG TEM MAIS pixels especiais não cobertos no dump (switch sem 0xFF3F51B5/009688/F4511E/7986CB/81C784/9C27B0/00BCD4/E91E63). Ou seja o dump estava incompleto — a cor real de 8,9 pode ser 0xFF00BCD4 (ARTILLERY).
- CONCLUSÃO REAL: se tracker persiste (isAlreadyDead=true no tile) e o enemy aparece no tile mesmo assim → ou o tile do alvo é boss (key 8,9N vs 8,9B mismatch!) — o alvo.isBoss() false → key N. Se o pixel é boss=false e está na killSet N, o skip deveria valer. ENTÃO: o jogo REAL (loadSlot) deve funcionar; o teste pode estar com classe antiga no $out (javac em $out pega EnemyKillTracker de bin via -cp, ok). RODAR NOVAMENTE com cp bin primeiro e com killSet impresso: `DISPLAY=:120 timeout 60 java -cp bin:res:$out Rodada25KillTest`.
- Se continuar falhando: adicionar print do killSet e dos tiles no teste (linha ~85-96) para debug.

## Próximos passos da rodada 25:
1. Resolver os 2 fails do KillTest (debug com prints: killSet no reload + tiles dos enemies).
2. Compilar e rodar Rodada25DialogTest (diálogo sobreposto — fix feito no DialogueManager; ajustar API se necessário).
3. Verificar bug do beacon no restart pós-morte (restart via Game.update usa World.restartGameFromFile?? NÃO passa por loadSlot — objetivo recriado → canal perdido → fase 2 trava no beacon!). FIX: no handleGameOverRestart (Game.update), usar loadSlot/restoreObjectiveState OU QuestManager.prepareForLevel deve ser evitado. Ver Game.update linhas ~1400 (handleGameOverRestart).
4. Regressão completa 28/28 + commit PR (branch rodada25, commits: DialogueManager + EnemyKillTracker + SaveManager + Game + World + Enemy + 2 suítes).
5. Apagar tools/_ProbeKill.java, _ProbeKill2.java, _ProbeKill3.java, _DumpMap.java, _DumpTile89.java antes do commit.

## STACK TRACE DO CONSTRUCTOR (RESOLVIDO PARCIALMENTE):
A stack do ctor em 8,9 no reload: Enemy.spawnRandomVariant(Enemy.java:327) → World.applyMapPixels(World.java:117) → World.restartGameCommon(356) → World.restartGame(333) → SaveManager.loadSlot(503). O killSet tinha 8,9N (serialize confirmou) ANTES do restart. Mas o ctor foi chamado = isAlreadyDead retornou FALSE durante o applyMapPixels.
ATENÇÃO: saveCurrentGame grava session.put("inimigosMortosSet", serialize()) na linha 121. loadSlot linha 447-448 aplica setCurrentLevel+deserialize; depois linha 503 restartGame (com tracker ativo — deveria pular); linha 506-507 reaplica.
Stack 1 (carga1): ctor 8,9 em World.restartGame linha 48 do teste (fase carregada) — ok, sem tracker.
Stack 2 (reload): ctor 8,9 via loadSlot(503) com tracker ativo mas NÃO pulou!
HIPÓTESE NOVA: o ctor printou "SCOUT boss=false" MAS O ALVO NA CARGA1 FOI... "DEBUG enemy reload: SENTINEL tile=8,9"! O reload mostra SENTINEL (não SCOUT). O ctor do SENTINEL em 8,9 NÃO printou. MAS a stack mostra ctor via spawnRandomVariant linha 327 → spawnRandomVariant escolhe variante ALEATÓRIA (Variant.values()[rand]) e depois SETA variant? Não há setVariant... Então o print seria SCOUT (ctor default). A stack só capturou UM ctor (o SCOUT da linha 117 = pixel FF0000). O SENTINEL 8,9: veio de spawnRandomVariant de OUTRO pixel (ex. outro FF0000 em 8,12=SCOUT na carga1 virou WARDEN no reload!) — SIM! spawnRandomVariant randomiza: WARDEN 8,12 (carga1 TELEPORTER 8,12?), SENTINEL 8,9, etc. TODOS os inimigos da carga1 trocaram de variante → CONFIRMA que todos vêm do spawnRandomVariant do mapa (os 10 pixels FF0000/random + fixos).
CONCLUSÃO FINAL: o applyMapPixels NO RELOAD DE FATO CRIA os inimigos. O isAlreadyDead(8,9,false) retorna false no momento da linha 117. POR QUÊ: entre a linha 448 (deserialize com 8,9N) e a linha 503 (restart), algo LIMPA o killSet... resetLevelStats? World.restartGame → restartGameCommon → resetLevelStats ZERA O TRACKER (linha 311)! A sequência é: linha 448 aplica tracker → linha 503 restart → linha 506 reaplica. O applyMapPixels roda DENTRO do restart (linha 503) COM tracker vazio (zerado por resetLevelStats na linha 311 do Game, que é chamada pelo restartGameCommon)!!! O reaplicado em 506 é TARDE DEMAIS!
FIX CORRETO: aplicar o tracker ANTES do restartGame no loadSlot? Mas o restart chama resetLevelStats que zera de novo. Alternativa: adicionar flag skipReset no resetLevelStats, ou reordenar: aplicar tracker DEPOIS do restart E re-rodar o applyMapPixels? Melhor: mover o setCurrentLevel+deserialize para DEPOIS do World.restartGame E modificar resetLevelStats para NÃO zerar se EnemyKillTracker.currentLevel==savedLevel (fase recarregada), ou simplesmente reaplicar + chamar EnemyKillTracker.disableReset... 
SOLUÇÃO MAIS SIMPLES: em resetLevelStats, não chamar EnemyKillTracker.reset() se estiver num recarregamento (flag Game.loadingSave ou parâmetro). OU: SaveManager.loadSlot aplica o tracker e depois força EnemyKillTracker.setCurrentLevel+deserialize DEPOIS do restart (já faz 506-507) — mas o mundo já foi criado sem o skip! Precisa re-aplicar o skip: impossível pós-criação. ENTÃO: desativar o reset do tracker dentro do loadSlot (Game.loadingSave=true) ou adicionar método resetLevelStats(boolean resetKills).

## RESOLVIDO (KillTest 14/14!):
CAUSA RAIZ FINAL: resetLevelStats (chamado pelo restartGame do loadSlot) zerava o tracker DEPOIS que o loadSlot o restaurou — os mobs voltavam.
FIX: Game.restorePhase (boolean estático, linha 84) — resetLevelStats NÃO zera o tracker quando restorePhase=true; SaveManager.loadSlot envolve World.restartGame com restorePhase=true/finally(false), com tracker deserializado antes e reaplicado depois (linhas 447-448 e 506-513).
KillTest: 14/14 PASS. Debug prints ainda no teste (linhas 86-92) — REMOVER antes do commit junto com gameState="PAUSED" (linha ~83).

## BUG 3 RESOLVIDO (BeaconTest 15/15!):
CAUSA RAIZ REAL: tile designado (17,11) do beacon programático da fase 2 é BRANCO (parede) no level2.png → onLevelLoaded do HoldObjective nunca cria o beacon → "Localize o beacon do setor" trava desde a primeira carga (percebido após morrer/reiniciar).
FIX: findNearestFloorTile (raio crescente) em HoldObjective.onLevelLoaded.
handleGameOverRestart: JÁ USA loadGameFromSave → loadSlot → estado restaurado (TALKED+SPAWNED+CHANNEL persistem no objectiveState salvo). Não precisou mudar — apenas garantir que o autosave da morte grava antes (já grava no update do GAMEOVER).
Rodada25BeaconTest: 15/15 PASS. Rodada25DialogTest: 7/7 PASS. Rodada25KillTest: 14/14 PASS.

## Restante da rodada 25:
1. Rodada25KillTest: remover prints debug + linha gameState=PAUSED, commitar em tools/.
2. Rodada25DialogTest: compilar e validar (painel dinâmico no DialogueManager — já editado).
3. Bug beacon no restart pós-morte: o restart do jogo real (handleGameOverRestart no Game.update) usa World.restartGameFromFile direto? Verificar Game.update linhas ~1400 — o restart pós-morte NÃO passa por loadSlot → objetivo recriado do zero → beacon trava. FIX: fazer o handleGameOverRestart restaurar o objetivo (chamar restoreObjectiveState) ou recarregar via SaveManager.loadSlot quando existe save.
4. Remover stack trace do Enemy.java (linhas 198-201: "if (x/16==8...") — é código temporário de debug!
5. Regressão 28 suítes; commit PR rodada25; apagar tools/_*.java probes.
