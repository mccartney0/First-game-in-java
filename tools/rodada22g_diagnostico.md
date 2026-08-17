# Rodada 22g — Testes de troca de fase e persistência de save (pedido do usuário)

## Pedido
O usuário pediu testes de troca de fase e de save correto, pois está encontrando bugs em produção. Entregável: suíte E2E validada + correções se houver falha.

## Arquivo criado
`tools/FaseSaveE2ETest.java` — 8 cenários:
1. advanceToNextLevel (1→2) + saveCurrentGame retorna true
2. maxLevelReached == fase atual (2), hasSlotSave(1), getSlotLevel(1)==2
3. loadSlot(1) restaura fase 2, vida/mana/escudo
4. getHighestUnlockedLevel == 2 e < 3 (seletor trava fase 3)
5. avançar para fase 3 + save → unlocked == 3
6. inventário (2 MEDKIT + 1 NANOMEDKIT) sobrevive à recarga
7. SideQuestManager.complete("resgate_veterano") sobrevive à recarga
8. avançar até MAX_LEVEL sem travar + unlocked == MAX_LEVEL

APIs reais confirmadas: Game.advanceToNextLevel (static), Game.CUR_LEVEL/MAX_LEVEL, Game.restartGame NÃO existe — usar com.traduvertgames.world.World.restartGame("level1.png"). InventoryManager.add(ItemType, qty), count(ItemType); enum: MEDKIT, NANOMEDKIT, ENERGY_CELL, SHIELD_ORB, OVERCLOCK, AMMO_PACK, ACCESS_KEY. SideQuestManager: complete(id), isCompleted(id). SaveManager: saveCurrentGame(), loadSlot(n), getHighestUnlockedLevel(), getSlotLevel(n), hasSlotSave(n), activeSlot=1. updateCampaign grava maxLevelReached e completedLevels (completed = reached-1).

Padrão de init do teste: SoundManager.unload(); new Game(); Game.setCurrentLevel(1); Game.SCALE=4; World.restartGame; Game.player = new Player(200,100,16,16,new BufferedImage).

SetField usa reflection em campos estáticos Player.life/mana/shield (double).

loadSlot chama World.restartGame(levelN.png) internamente — assets existem em res/.

## Estado
- Teste compilado OK após correções de API. FALTA RODAR.
- Comando: cd /home/ubuntu/First-game-in-java && find src -name "*.java" | xargs javac -d bin -cp bin && out=/tmp/test_FaseSaveE2E && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/FaseSaveE2ETest.java && DISPLAY=:120 timeout 120 java -cp $out:bin:res FaseSaveE2ETest
- Se falhar: corrigir no jogo, rebuild, rerodar; depois commit "fix(22g)", push para manus/rodada22-trilha-npcs-inventario (PR #36), e regressão completa das 16 suites + este novo teste.
- Regressão completa (16 suites): ObjectivesVariadosTest ShopQaTest MenuNavigationTest PhaseTransitionTest AutoValidate StoryNpcPlacementTest WaypointFrameTest TransitionCooldownTest MusicZoneTest InventoryTest BranchingNpcTest Rodada22bTest Rodada22cTest Rodada22dTest Rodada22eTest Rodada22fTest — loop em /home/ubuntu/First-game-in-java com DISPLAY=:120 timeout 90.

## Falhas encontradas (execução 1)
1. "Fases 1 e 2 concluídas" (hasSlotSave(1)) — FAIL. CAUSA CONFIRMADA: saveCurrentGame grava chaves (vida/mana/level...) no slot, copia para `session`, depois REMOVE do slot as chaves da session → slot fica só com id/progress/timestamp. hasSlotSave (linha ~605-615) verifica session no SLOT (linha 609: `getSession(slot).containsKey("vida")`) — deveria funcionar... MAS o teste limpa o arquivo no cenário 5? Não limpa — o cenário 5 chama saveCurrentGame na fase 3; slot1 fica com level=3. hasSlotSave(slot1): getSession(slot) → contém vida? Sim, deveria. Falha real: talvez o hasSlotSave lê outro slot? Ver getSlotLevel(1)==2 no cenário 2 passou (novo jogo) — depois cenário 4 salva de novo level=3, cenário 5 hasSlotSave FAIL... Ver código de hasSlotSave linha exata.
2. "Missão secundária persistida" — FAIL: sideQuestsDone gravado no save mas loadSlot não restaura SideQuestManager.getCompleted? Ver restore do loadSlot (linhas ~370-480).
3. Nota: cenário 4 (unlocked<3) passou — depois do cenário 5 o save é level 3.

## Diagnóstico falha 1 (hasSlotSave) — em aberto
Suspeita principal: writeRoot/getSlots — verificar se a reescrita do JSON por loadSlot/save mantém a lista de slots com id=1. Ver writeRoot e findSlot.

## Falha 2 (sideQuests) — DIAGNOSTICADA: não é bug do jogo?
SideQuestManager.deserialize POPULA completed com o id "resgate_veterano" e isCompleted verifica completed.get(id)==Boolean.TRUE — deveria funcionar. A falha pode ser outra: o loadSlot no cenário 7 usa SaveManager.activeSlot=1 mas o cleanSaveFile do cenário 7 reseta; complete("resgate_veterano"); saveCurrentGame → session.sideQuestsDone={resgate_veterano:true}; loadSlot → restore. MAS loadSlot chama reset()? Verificar início do loadSlot: se chama SideQuestManager.reset()/InventoryManager.reset() ANTES do restore — mas o restore re-popula. Porém no cenário 7 o g6 = newGame() — o construtor do Game registra quests? verificar: SideQuestManager.register é chamado em algum init (World/QuestManager.init)? Se o manager é limpo em algum lugar durante o restartGame... O restore vem DEPOIS do reset no loadSlot — ainda assim o completed fica populado. A falha real pode ser: InventoryManager.reset() é chamado dentro do restore antes? Ou o loadSlot chama QuestManager.reset? Investigar o início do loadSlot (340-395) e o que o construtor new Game() faz com InventoryManager/SideQuestManager.

## Progresso no diagnóstico falha 2
- Nenhum SideQuestManager.reset em todo o src. Restore correto. prepareForLevel não reseta side quests.
- Próximo passo: ver restante do construtor Game() (200-240) — se chama startNewGame/onboarding que reseta algo; e confirmar com teste isolado do cenário 7 (extrair em teste pequeno com prints).

## Diagnóstico falha 2 — próximo suspeito: restoreObjectiveState
Este método pode resetar o SideQuestManager ao restaurar o estado da fase (para não carregar progresso de missão da fase anterior). Ver restoreObjectiveState e onde é chamado no loadSlot (linha ~467).

## Estado consolidado (22g) — salvar antes de compactação

### Suíte tools/FaseSaveE2ETest.java — 19 checks, execução 1: 17 PASS / 2 FAIL
- FAIL 1: "Fases 1 e 2 concluídas" (hasSlotSave(1)) no cenário 5. Cenário 5 NÃO limpa arquivo; salva fase 3 (setCurrentLevel(3)+saveCurrentGame). Expectativa: hasSlotSave(1)==true. saveCurrentGame grava slot com session contendo vida/level; depois REMOVE chaves do slot (175-177). getSession(slot) usa slot.get("session") → tem vida/level → deveria true. Investigar o que muda entre cenário 2 (PASS hasSlotSave) e cenário 5 (FAIL). Diferença: cenário 5 segue cenário 4 (setCurrentLevel(3)+save). O save grava level=3. hasSlotSave verifica sessionId=1. Talvez findSlot retorna null por que slots regravados? writeRoot escreve root com "slots"=lista. getSlots retorna. findSlot checa slot.get("id"). saveCurrentGame NÃO seta slot.put("id")! findOrCreateSlot seta id. saveCurrentGame usa findOrCreateSlot (linha 106) que define id. OK deveria ter id.
- FAIL 2: "Missão secundária persistida" no cenário 7. Cenário 7: cleanSaveFile; g5=newGame(); initPlayerAndWorld; SideQuestManager.complete("resgate_veterano"); saveCurrentGame(); g6=newGame(); loadSlot(1); isCompleted("resgate_veterano")==false.
  - restore dos sideQuests ocorre em loadSlot linhas 399-414 (populate completed) ANTES do restartGame (428) e ANTES de restoreObjectiveState (471/489).
  - restoreObjectiveState (581+) usa progress do slot para restaurar estado da missão — ver linhas 588-600: pode chamar prepareForLevel/restart que reseta sideQuests? VERIFICAR LINHAS 588-620.
  - Nenhum SideQuestManager.reset em todo o src (grep confirmado).

### Códigos/chaves
- loadSlot: 347-493. restoreObjectiveState: 581+. getSession: 570-578. hasSlotSave: 617-624. getHighestUnlockedLevel: 627-641. getSlotLevel: 643+. updateCampaign: 311-330. saveCurrentGame: 102-188. cleanSaveFile apaga saves.json e seta activeSlot=1.
- Inventário: cenários 6 PASSAM (add/count MEDKIT/NANOMEDKIT OK, deserializa OK).
- Fase máximo: cenário 8 PASS (avanço 8x, unlocked=8).

### Fluxo de jogo relevante
- Game advanceToNextLevel (1658): CUR_LEVEL++ até MAX_LEVEL=8, depois entra survival (levelPlus++, enterSurvivalMode). Save: saveCurrentGame usa getCurrentLevel()/getLevelPlus()/instance.levelPlus.
- loadSlot restaura savedLevelPlus/savedLevel, chama World.restartGame(levelN.png) (linha 428), depois onLevelLoaded etc.

### Próximos passos
1. Ver restoreObjectiveState linhas 583-600 — se chama prepareForLevel/restart que limpa sideQuests (hipótese forte para FAIL 2).
2. Para FAIL 1: verificar se o save da fase 3 grava activeSlot/root correto; testar isoladamente (hasSlotSave após cenário 5).
3. Corrigir bugs no jogo, rebuild (find src -name "*.java" | xargs javac -d bin -cp bin), rerodar teste, depois regressão 16 suites, commit fix(22g), push manus/rodada22-trilha-npcs-inventario, reportar.

## restoreObjectiveState (581-596) NÃO toca sideQuests. Próximo: replicar cenário 7 isolado com debug.
Hipóteses restantes para FAIL 2:
A) SideQuestManager.getCompleted() retorna cópia, mas complete() usa outro mapa? Não: completed.put.
B) O saveCurrentGame grava sideQuestsDone antes da completação? Ordem no teste: complete() DEPOIS do save? — verificar ordem real no teste (linhas ~115-140 do FaseSaveE2ETest).
C) saveCurrentGame pode ser chamado duas vezes (update NORMAL chama saveAutoSave?) — entre complete e load, o g.update() não é chamado no teste (não há update). Mas o loadSlot usa root do DISCO — se saveCurrentGame retornou false (escrita falhou), o disco fica com save antigo (vazio pós-clean) → restore vazio. Testar: o cenário 5 usa saveCurrentGame e depois loadSlot (cenário 3 usa loadSlot) — cenário 6 (inventário) PASSOU usando saveCurrentGame e loadSlot. Então gravação OK.
D) Cenário 7: cleanSaveFile() → saveCurrentGame() na fase 1 → loadSlot(1). Inventário (save+load) passa no cenário 6 com mesmo padrão. Único diferente: sideQuestsDone. Verificar o TESTE linhas exatas.

## FAIL 2 RESOLVIDO (erro do TESTE, não bug do jogo)
"resgate_veterano" é id fictício que não existe no jogo. Ids reais: "rex_kills_{nivel}" e "lila_collect_{nivel}" (SecondaryNpcs.java 33, 108). Corrigir teste: usar "rex_kills_1". Não é bug do jogo — sideQuestsDone grava qualquer id, mas isso é comportamento intencional (flexível). Usar id real no teste.

## FAIL 1 em aberto (hasSlotSave(1) false após cenário 5)
Cenário 5 não limpa arquivo; cenário 4 salva fase 3 (level=3) no slot 1; cenário 5 check hasSlotSave(1) falha. Investigar: talvez o saveCurrentGame do cenário 4 usa game.instance = g2 do cenário 2/3 (novo jogo) e getCurrentLevel()=3, mas updateCampaign pode setar completedLevels e remover slot? Não. Verificar no debug: ler saves.json após cenário 4 e verificar estrutura. Escrever teste de debug isolado.

## DebugSlotTest criado em tools/DebugSlotTest.java
Teste isolado: saveCurrentGame na fase 3, depois hasSlotSave(1), getSlotLevel(1), highestUnlocked, dump JSON. Correção feita: usar com.traduvertgames.entities.Player (não Game.Player). Falta rodar:
cd /home/ubuntu/First-game-in-java && find src -name "*.java" | xargs javac -d bin -cp bin && out=/tmp/test_debugslot && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/DebugSlotTest.java && DISPLAY=:120 timeout 60 java -cp $out:bin:res DebugSlotTest

Nota: o heredoc anterior vazou "echo" no shell por causa das aspas; ignorar output truncado anterior.

## Ações restantes 22g
1. Rodar DebugSlotTest para diagnosticar FAIL 1 (hasSlotSave).
2. Corrigir FaseSaveE2ETest (fail 2 já corrigido: rex_kills_1).
3. Se houver bug real no jogo: corrigir, rebuild, rerodar suíte até 19/19.
4. Regressão 16 suites + commit "test(22g): suíte E2E de troca de fase e save (+ fix se houver)" + push manus/rodada22-trilha-npcs-inventario (PR #36).
5. DebugSlotTest é temporário — remover antes do commit final.

## DebugSlotTest (nível 1): saveCurrentGame=true, hasSlotSave(1)=true, slot level=3 OK
=> O save está correto ISOLADO. A falha do cenário 5 vem de INTERFERÊNCIA entre cenários.
Suspeita: o cenário 4 faz SaveManager.saveCurrentGame() com fase 3 → updateCampaign → completedLevels. Cenário 5 check hasSlotSave(1). MAS entre eles não há nada que apague... A MENOS que cleanSaveFile do cenário 6? Não, 6 vem depois. Ver ordem real do teste linhas 85-116 para conferir o que acontece entre cenário 4 e o check do cenário 5 (o check "Fases 1 e 2 concluídas" está em qual cenário?).

## FAIL 1 — cenário 5: hasSlotSave(1) false DEPOIS do cenário 4+5
Sequência: cenário 2 (PASS hasSlotSave, fase 2 salva), cenário 3 (loadSlot(1) — OK), cenário 4 (getHighestUnlockedLevel==2 PASS, <3 PASS), cenário 5 (setCurrentLevel(3); saveCurrentGame; unlocked==3 PASS; hasSlotSave(1) FAIL).
O debug isolado com save na fase 3 devolveu hasSlotSave=true e level=3.
No teste real, o save do cenário 2 é fase 2; cenário 3 loadSlot(1) (restaura vida/mana/escudo ~80/250/30); cenário 5 salva fase 3. O slot 1 deve ter level=3 e session com vida/level → true.
SUSPEITA REAL: updateCampaign — ver linhas 311-330: pode apagar o slot inteiro! `root.put("slots", slots)` é feito ANTES ou DEPOIS de updateCampaign? No saveCurrentGame: updateCampaign(root, game) (linha 171) ANTES de root.put("slots", slots) (173). E updateCampaign(root, game): ver código — pode fazer root.put("slots", ...) com slots SEM o slot atual? Não. MAS: updateCampaign escreve "campaign"={maxLevelReached, completedLevels}. completedLevels pode sobrescrever? Não afeta hasSlotSave.
ALTERNATIVA: getSlots — ao ler root com loadRoot, slots vêm do disco. writeRoot grava. loadRoot lê. OK.
O mais provável BUG REAL: updateCampaign (311-330) faz algo como root.remove("slots") ou getSlots do root com "completedLevels" como lista de slots?? "completedLevels": ["level1"...] — se getSlots procurar root.get("slots") e houver... não.
AÇÃO: rodar o teste com dumps após cenário 4 e após cenário 5 (adicionar temporariamente prints no teste) OU verificar updateCampaign linhas 311-330 diretamente.

## Execução 2: hasSlotSave=false mesmo com arquivo presente. Falha 2 voltou (rex_kills_1 não foi usada? sed com resgate_veterano→rex_kills_1 falhou?). Verificar se o sed funcionou: grep rex_kills_1 no teste. E dumpar o JSON real após cenário 5 (ver estrutura do slot).
Possível causa raiz comum: saveCurrentGame do cenário 5 com game.instance = g1? g1 foi criado no cenário 1; cenário 3 criou g2; g2 tem game.gameState... loadSlot do cenário 3 setou Game.gameState=NORMAL. saveCurrentGame usa Game.getInstance() = g2 (último newGame)? getInstance é static — o último construtor define instance = this → g2. g2.getCurrentLevel()=2 (cenário 3 restoreu 2), cenário 5 setCurrentLevel(3). getLevelPlus()=0. OK.
Mas wait — no debug isolado funcionou. A diferença crítica: no teste real, o loadSlot do cenário 3 carrega vida 100/120 (a fase 2 tem vida 120?) — o setField do cenário 1 setou 80 antes do save. O load do cenário 3 restoreu 80. Cenário 5 setCurrentLevel(3) + saveCurrentGame. O saveCurrentGame usa Player.life = 80. OK não afeta hasSlotSave.
NOVO ÂNGULO: updateCampaign usa game.getCurrentLevel() — no cenário 5 reached=3, maxLevelReached=3. completedLevels=[1,2]. OK.
VERIFICAR DIRETO: dumpar saves.json após cenário 5 e ver o conteúdo do slot 1.

## ACHADO CRÍTICO
JSON após cenário 5: slot 1 tem apenas {id, timestamp, progress} — a "session" INTEIRA sumiu (sem vida/level/inventario)! Os cenários 1-4 gravaram session corretamente; o cenário 5 gravou só progress+timestamp.
Diferença do cenário 5: SaveManager.saveCurrentGame() chamado logo após SaveManager.saveCurrentGame() do cenário 4?? Não — entre eles só getHighestUnlockedLevel (leitura). E o cenário 3 chamou loadSlot(1) que... seta activeSlot=1, ok.
Pistas: slot.timestamp foi ATUALIZADO (103128) → writeRoot ocorreu. Mas session ausente → saveCurrentGame do cenário 5 não colocou session? Impossível (código fixo). A MENOS QUE: o slot do cenário 5 seja O MESMO objeto do disco com session, e algo o REMOVA antes da gravação — ou o saveCurrentGame do cenário 4 tenha falhado na parte da session.
Hipótese: saveCurrentGame do cenário 4 grava session com inventario {} e sideQuests {}; cenário 5: saveCurrentGame carrega root (com session do cenário 4), slot com session {...}, faz slot.put("session", NOVA session)... sobrescreve. OK deveria existir.
NOVA PISTA: o JSON5 mostra progress.traitorTalked=false — esse é npcDialogues+progress. O timestamp mudou. MAS não tem session NEM bestRun. writeBestRun(root) — ver se ele sobrescreve root inteiro (root = emptyRoot()??)
VERIFICAR writeBestRun e o início de loadRoot/emptyRoot no saveCurrentGame: root = loadRoot() — se saves.json estiver LOCKED/aberto? Não, é leitura.
AÇÃO: ver writeBestRun (linhas ~700-780) e procurar qualquer root.put("slots", ...) que use lista sem sessão, e verificar se saveCurrentGame do cenário 5 passa por game != null path com exceção após slot.put("session") mas ANTES do writeRoot... Se exceção após session.put mas antes de writeRoot, writeRoot não gravaria. MAS timestamp mudou → writeRoot ocorreu. Hmm, timestamp vem de currentTimestamp() posto ANTES do session.put (linha ~170). Se exceção em slot.put("companionType",...) (catch Throwable ignored — não lança). Então exceção não.
Ver writeBestRun!!!

## writeBestRun OK (não corrompe). writeRoot escreve root inteiro — ok.
## Próximo passo: dump do JSON APÓS cenário 4 (linha 108, após getHighestUnlockedLevel < 3) para ver se o save do cenário 4 já perdeu a session. Se sim → bug no saveCurrentGame quando session contém inventario {} + sideQuests {} + companionType ""... O cenário 2 (fase 2 save) gravou session completa (visto no debug isolado? Não, debug isolado era cenário de fase 3 isolado).
Hipótese forte: o slot.put("session", session) é sobrescrito?? Não. MAS: saveCurrentGame usa Map<String,Object> slot = findOrCreateSlot(slots, activeSlot) — o slot RETORNADO é o objeto da lista. slot.put("session",...) modifica o mesmo objeto. Depois root.put("slots", slots) grava. OK.
ALTERNATIVA: no cenário 5, o loadRoot lê o arquivo; getSlots retorna nova lista com os maps. OK.
O mais provável: o JSON5 é do save do CENÁRIO 5, e a session do cenário 5 foi gravada mas o JSON mostra só progress/timestamp. Isso significa que saveCurrentGame do cenário 5 NÃO gravou session. Mas o código é fixo... A MENOS QUE a exceção ocorra ANTES de slot.put("session", session) mas DEPOIS do timestamp... timestamp é linha ~170 ANTES de session.put (~163? Não: session.put é 162-164, timestamp ~170 vem DEPOIS!). Se timestamp foi atualizado, session.put já ocorreu. Então a gravação ocorreu com session. O JSON5 não tem session! CONTRADIÇÃO → o arquivo foi regravado por OUTRA escrita sem session depois. Quem mais grava? writeRoot só chamado em saveCurrentGame e loadRoot? buscar writeRoot chamadas. writeBestRun não grava. updateCampaign não grava. loadRoot não grava. MAS: writeBestRun + updateCampaign são chamados DENTRO do saveCurrentGame ANTES de root.put("slots", slots) e writeRoot. writeRoot grava uma única vez. 
VERIFICAR: outras chamadas de writeRoot fora saveCurrentGame! (grep writeRoot)
Se houver outra escrita (ex.: restore do loadSlot salva o slot "como está" após migração?), aí sim o slot sem session viria de lá. loadSlot não grava... verificar.

## writeRoot só em saveCurrentGame (187) e clearSlot (753). clearSlot NÃO chamado no teste.
## O mistério: JSON5 (save do cenário 5) sem session. A gravação do cenário 5 NÃO incluiu session.
## Próxima ação decidida: adicionar dump do JSON após cenário 4 (linha ~108) para ver estado do disco entre 4 e 5. Já existe dump após cenário 5. Adicionar após linha 108.
## NOTA: o dump JSON5 mostrou progress com objectiveState da fase 3 (TALKED=false;DELEGATE=IDX=0|S0=...S1=UNKNOWN) — o objectiveState é do wrapper DialogueObjective (SequenceObjective dentro). S1=UNKNOWN é normal para BossHunt não iniciado. OK.

## DECISIVO: JSON4 (pós cenário 4) tem session COMPLETA. JSON5 (pós cenário 5) NÃO tem session.
O saveCurrentGame do cenário 5 (ferramenta de teste) gravou objectiveState fase 3 + progress + timestamp mas NÃO a session. Ou o save do cenário 5 gravou session e um SAVE POSTERIOR (thread viva de g1/g2 com saveAutoSave periódico) regravou o arquivo SEM session (player default de uma instância antiga, objectiveState... não).
AÇÃO: ver a ordem exata de put no saveCurrentGame linhas 155-190 e o saveAutoSave do thread (Game.update ~655). Verificar se a thread de g2 (cenário 3) continua viva e grava periodicamente com uma session que não inclui os campos novos (inventario/sideQuests etc — talvez a session do thread seja montada de forma antiga).
Ver: saveCurrentGame 155-190; Game.update saveAutoSave; e se o cenário 5 do teste real chama saveCurrentGame (sim).

## Próximo: ver saveCurrentGame linhas 120-155 (criação da session) — o loop copia chaves antigas do slot para session; se o slot NÃO tem session anterior (clean/primeira gravação), session = novo HashMap; ok. Se o slot tem session anterior, getSession retorna a session antiga → reutiliza. OK.
## O JSON5 sem session: o único caminho é o saveCurrentGame do cenário 5 NÃO ter executado session.put ("slot.put("session", session)") — mas timestamp veio. Ordem: session.put (antes) → progress → survivalRecord → timestamp → remove keys → updateCampaign → writeRoot. Se exceção após timestamp mas antes de writeRoot... writeRoot não grava. timestamp veio → writeRoot ocorreu.
## ÚNICA EXPLICAÇÃO RESTANTE: o saveCurrentGame do cenário 5 não foi o último writer. O thread de g1 ou g2 (jogos vivos do teste) chama saveAutoSave periodicamente. O saveAutoSave chama saveCurrentGame → grava session. A MENOS que saveAutoSave seja outro método que grava SEM session! Ver saveAutoSave linhas 193-242.

## Decisão: instrumentar saveCurrentGame com try/catch que imprime stacktrace (o código atual não tem try no caminho da session; mas getActive/getSkin pode lançar fora do try interno? getSkin() não está no try da sessão — está DENTRO do try do companion (130-145) — ok.
## Instrumentação: envolver bloco session.put("inventario"...) → writeRoot com try { ... } catch (Throwable t) { t.printStackTrace(); } e ver stderr. Adicionar em SaveManager linha 155-187: try{bloco}catch(Throwable t){System.err.println("SAVE-ERR: "+t);t.printStackTrace();} — o teste roda com stderr capturado.

## Estado: instrumentação falhou na âncora (try sem catch aplicado, anchor3 mudou para "} catch ..." mas a linha "return writeRoot(root);" do método saveCurrentGame não bateu com o padrão — o script aplicou no clearSlot!). Ver linhas 165-200 atuais do SaveManager.java e corrigir manualmente: remover try órfão da linha ~171 e ~195, e remover "} catch (Throwable __t)..." da linha ~755 (clearSlot). Depois aplicar try/catch corretamente apenas em saveCurrentGame (linhas ~163-192).
## IMPORTANTE: mesmo com bin antigo (sem instrumentação) o teste 17/19 com as mesmas 2 falhas.

## 2026-08-17 ~10:50 — Instrumentação OK (try/catch compilou). Rodada com instrumentação: SEM SAVE-ERR no stderr. As 2 falhas persistem (17/19).
## CONCLUSÃO: NENHUMA exceção. A session SOME de outra forma:
## Teoria mais forte agora: o teste é multi-instância (g, g2, g3, g4, g5, g6). Os threads do Game (instâncias vivas) chamam saveAutoSave periodicamente. A thread de ALGUMA instância antiga (com gameState NORMAL, mas Player.stats DEFAULT da fase default, session SEM inventário/sideQuests) grava POR CIMA depois do save do cenário 5.
## Como provar: ver o timestamp do saves.json vs o momento da gravação do cenário 5, e se a ordem dos eventos bate. O teste já fez dump do JSON5 (10:31:28 no JSON). Adicionar dumps com timestamp de nano do processo ao redor da falha para comparar com o timestamp do JSON.
## OU mais simples: verificação direta — no teste, entre o save e o check do cenário 5, dormir 0 e dumpar; comparar com o que o teste lê em seguida. Se o dump pós-save tem session e o arquivo no check não tem, é outra thread gravando depois.
## AÇÃO: dumpar após o saveCurrentGame do cenário 5 (linha do teste: saveCurrentGame() chamado) e logo antes do check; comparar.

## Timestamp JSON5 = 10:34:18 (mesmo momento do save do cenário 5). Mas SEM session. E SEM exceção.
## NOVA PISTA: o loop copia entry.getValue() para session, mas session.put sobrescreve com tipos diferentes? Map<Object,Object> — HashMap<String,Object> aceita qualquer Object. session.put(key, entry.getValue()) — entry.getValue() é Object do slot (Double etc). ok.
## TESTE MINIMAL DECISIVO: rodar só o cenário 5 isolado.

## 2026-08-17 ~10:45 — MinimalSaveTest: PASS (session presente, hasSlotSave true, fase 3).
## O bug só aparece no teste multi-instância (g/g2/g3...). Causa: thread viva de alguma instância grava depois.
## Próximo: instrumentar com SAVE-START/SAVE-END + linha de chamada + timestamp + temSession — definitivamente.

## ESTADO COMPLETO (rodada 22g — testes de troca de fase e save)
- Arquivos-chave: tools/FaseSaveE2ETest.java (suíte, 19 checks, 2 falham: "Fases 1 e 2 concluídas" e "Missão secundária concluída persistida após recarga" [esta pode ser o mesmo root do id errado rex_kills_1 vs resgate_veterano — VERIFICAR: sed trocou o id? grep rex]); tools/MinimalSaveTest.java (PASS isolado); src SaveManager.java INSTRUMENTADO (SAVE-ERR try/catch ok compilado; sem exceções no run).
- Dump JSON5 (após save fase 3 no cenário 5): slot id=1 com timestamp/progress/traitorTalked/campaign mas SEM "session". maxLevelReached=3, completedLevels=[2]. Timestamp 10:34:18 = do save do cenário 5.
- MinimalSaveTest (mesmo save isolado): session PRESENTE, PASS.
- Conclusão parcial: a session some APENAS com threads de Game vivas em paralelo (teste usa g, g2). Alguma thread chama saveCurrentGame depois e grava SEM session — ou a chamada do cenário 5 em si perde a session (mas minimal passa!).
- Próximos passos: instrumentar SAVE-START/SAVE-END com Thread name + timestamp + temSession antes de writeRoot; rodar FaseSaveE2ETest; identificar o writer final.
- Corrigir SaveManager: remover instrumentação antes do commit final.
- Cenário 7 (missões secundárias): usar id real "resgate_veterano"? NÃO EXISTE — ids reais verificados antes: procurar ids em SecondaryNpcs.java (grep 'ne.*Objective'). O teste já usa id real (sed foi aplicado). Se ainda FAIL: checar isCompleted com o id real.
- Suíte completa de regressão: ObjectivesVariadosTest, ShopQaTest, MenuNavigationTest, PhaseTransitionTest, AutoValidate, StoryNpcPlacementTest, WaypointFrameTest, TransitionCooldownTest, MusicZoneTest, InventoryTest, BranchingNpcTest, Rodada22bTest, Rodada22cTest, Rodada22dTest, Rodada22eTest, Rodada22fTest.
- Comando: out=/tmp/test_X; rm -rf $out; mkdir -p $out; javac -d $out -cp bin:res tools/X.java 2>/dev/null && DISPLAY=:120 timeout 60 java -cp $out:bin:res X 2>&1 | tail -2
- Commit final: git add -A && git commit -m "test(e2e): fase e save, corrige bugs encontrados" && git push origin manus/rodada22-trilha-npcs-inventario
- PR #36: https://github.com/mccartney0/First-game-in-java/pull/36

## 2026-08-17 ~10:55 — DECISIVO: no run real, 5 SAVE-START aparecem (chamadas da main do teste) e ZERO SAVE-END e ZERO SAVE-ERR. Impossível com o código atual, salvo se: (a) bin antigo em uso, ou (b) o método lança antes do END mas fora do try (entre START e try). Linhas entre START e try: Game.getInstance(), loadRoot(), getSlots(), findOrCreateSlot(), puts no slot, captureBestRun(), loop WeaponType, try companion. captureBestRun() chama Game.getKillsThisLevel() / getLevelTimeMs() — se lançam, caem FORA do try (o try começa no bloco session/progress). E NÃO são Throwable... RuntimeException cai no catch(Throwable __t)?? NÃO — o try cobre só "slot.put session...writeRoot"?? VERIFICAR: no código atual o try inicia ANTES do "slot.put("session", session)" — a exceção entre START e try não é capturada! Mas então o teste morreria com stacktrace em stderr... O stderr não mostra stacktrace porque System.err do teste principal é redirecionado (2>/tmp/e2e_err.txt) — grep SAVE-ERR vazio, mas stacktrace poderia estar lá sem o prefixo! Ver o e2e_err.txt completo.
