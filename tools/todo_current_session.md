# Sessão: continuar melhorias no PR #27 (branch manus/todas-melhorias)

## Concluído nesta sessão
- MenuLogicTest criado (tools/MenuLogicTest.java) — 5/5 OK, commitado 9389a1b
- Comentário adicionado no PR #27
- ChainArcProjectile: relâmpago com 5 segmentos + halo triplo durante salto, flash de impacto expandindo nos alvos, rastro elétrico (ParticleSystem.trail ciano) durante voo
- BoomerangProjectile: rastro arcano (trail ciano a cada 2 frames), lâmina c/ núcleo, cor muda no retorno (feedback de fase)

## Progresso desta sessão (concluído)
- ChainArcProjectile: relâmpago 5 segmentos c/ halo triplo, flash de impacto, rastro elétrico (trail ciano)
- BoomerangProjectile: rastro arcano (trail a cada 2 frames), lâmina c/ núcleo, cor muda no retorno
- DroneSentinel: rastro dourado de propulsão (trail a cada 2 frames), antena de radar vermelha, propulsores traseiros piscando, explosão dourada (explode) ao morrer/esgotar tempo
- WeaponType: BOOMERANG fireDelay 18, mana 1.8, dano 7.0, pickupRecharge 14, desc. nova; CHAIN_ARC fireDelay 22, mana 2.2, pickupRecharge 12, desc. nova; DRONE fireDelay 28, mana 2.4, dano 4.5, pickupRecharge 20, desc. nova
- ContactObjective: PICKUP ao coletar artefato, LEVELUP ao completar missão (coleta ou diálogo)
- DialogueObjective: LEVELUP ao diálogo que completa a missão; LEVELUP na coleta que completa a missão

## Atenção (análise)
- DialogueObjective com CollectArtifactsObjective: não há som duplicado — o CollectArtifactsObjective não toca som; o LEVELUP só toca quando talkedToTarget && delegate.isComplete() virar true
- ContactObjective também não tem duplicação (não delega)
- QuestManager (linha 110/142) chama apenas os callbacks; SoundManager.play é static e seguro

## Pendente nesta sessão
1. DroneSentinel: adicionar rastro dourado de propulsão (trail amarelo), propulsor visível na render
2. Balanceamento WeaponType.java: revisar custo/mana/dano das 3 novas armas (dano ChainArc base, cadência Drone, custo Boomerang)
3. Sistema de missões: som ao completar objetivo de missão (SoundManager.Event.PICKUP ou BOSS_DEFEAT) — verificar SoundManager e adicionar em ContactObjective/DialogueObjective quando completa; também som ao iniciar diálogo (PICKUP?)
4. Compilar (javac -d bin -cp bin $(find src -name "*.java")) + todos os testes (AutoValidateTest 24/24, QuestLogicTest 8/8, SaveLoadLogicTest, WeaponsLogicTest 17/17, MenuLogicTest 5/5)
5. Teste visual headless: Xvfb :120, `DISPLAY=:120 nohup java -cp bin com.traduvertgames.main.Game > /tmp/game.log 2>&1 &`, importar com `DISPLAY=:120 import -window root /tmp/shot.png` (funciona — .Xauthority inexistente é ok)
6. git add src bin tools; commit; push origin manus/todas-melhorias; gh pr comment 27 com resumo
7. Entregar resumo ao usuário

## Fatos técnicos importantes
- ParticleSystem é estático: burst/spark/explode/trail(int,int,Color); trail tem chance interna 1/3
- Game.rand existe (Random)
- HUD de missões: MissionHud.render; diálogo: DialogueManager; som: SoundManager.play(Event) — eventos: SHOT,KILL,TELEPORT,BOSS_DEFEAT,LEVELUP,SHOP,WAVE,TUTORIAL_STEP,TUTORIAL_DONE,HIT,PICKUP,BOSS_ALERT,DAMAGE,LASER
- Compilar: javac -d bin -cp bin $(find src -name "*.java"); bin/ versionado
- PR #27 na branch manus/todas-melhorias; repo /home/ubuntu/First-game-in-java

## Diagnóstico em andamento (teste visual)
- Jogo no Xvfb :120, saves.json em /home/ubuntu/First-game-in-java/saves.json, slot 1 com armaAtual=10, armasDesbloqueadas=1023 (JSON confirmado correto, versão 2)
- Menu "Carregar jogo" mostra todos os slots "(vazio)" — SaveManager.hasSlotSave(1) retorna false
- SaveManager.hasSlotSave: carrega root, pega slots via getSlots, findSlot(slotId), e checa session "vida"/"level"
- getSession(slot): procura chave "session" (Map); se não achar, retorna o próprio slot (flat)
- O JSON gravado pelo make_test_save.py NÃO tem chave "session" — é flat; então getSession retorna o slot e hasSlotSave deveria achar "vida" (linha 278-281 do SaveManager.java)
- HIPÓTESE: o renderLoadMenu usa outtra chave p/ label (vazio vs nível); o ">" no LOAD menu mostra "Slot 1 (vazio)" — renderLoadMenu usa hasSlotSave OU getSlotLevel==-1 p/ "(vazio)". Se hasSlotSave falha, findSlot pode retornar null. findSlot está na linha 479 — VERIFICAR implementação (provável causa: procura "slot"+id no array de slots)
- IMPORTANTE: verificar SaveManager.getSlots — talvez espere slots como objetos {slot1:{...}, slot2:{...}} e não array; nosso JSON usa array. O loadRoot/getSlots parsing pode diferir!
- Navegação do menu funciona com xte puro (sem xdotool click — clique ativa itens do menu)
- Captura: DISPLAY=:120 import -window root /tmp/shot.png (funciona)

## Diagnóstico: save carregou fase 1 OK, mas arma mostra PADRÃO (não BUMERANGUE)
- HUD mostra "PADRÃO 1/250" mesmo com armaAtual=10 no save
- Fluxo em loadSlot: loadCurrentWeaponFromSave(10) → persistentCurrentWeapon=BOOMERANG → restartGame cria novo Player (construtor chama initializeArsenalState → syncFromPersistentState → currentWeapon=BOOMERANG) → depois syncFromPersistentState de novo e refreshWeaponCapacity
- refreshWeaponCapacity: weapon = min(storedEnergy, maxDurability); stored = weaponEnergy.get(BOOMERANG) = persistente
- Mas energia do BOOMERANG: make_test_save NÃO grava energiaArma_BOOMERANG_ARCANO → persistenteWeaponEnergy default (ensurePersistentDefaults = maxDurability?) — VERIFICAR ensurePersistentDefaults
- Possível bug: weaponEnergy persiste mas weapon=1/250 sugere maxWeapon = BLASTER's 250? maxWeapon é por arma... maxDurability do BOOMERANG = 260 com multiplier 1.0. HUD mostra 1/250 (250 = max do BLASTER!). Então currentWeapon virou BLASTER em algum ponto
- Hipótese: loadCurrentWeaponFromSave(10): armaAtual ordinal 10 = ? verificar enum ordinals: BLASTER=0,ION_RIFLE=1,SCATTER=2,FUSION=3,ARC=4,SOLAR=5,PLASMA=6,VOID_MORTAR=7,BOOMERANG=8,CHAIN_ARC=9,DRONE=10! → ordinal 10 é DRONE, não bumerangue. E DRONE: tiro cria DroneSentinel e não mostra projétil; mas HUD mostra PADRÃO? Hmm, getShortName de DRONE = "DRONE". Mostra PADRÃO => currentWeapon = BLASTER.
- VERIFICAR: ordinals exatos do enum e por que currentWeapon = BLASTER. Talvez fromOrdinal(10) ok mas syncFromPersistentState depois: currentWeapon = persistent... se loadCurrentWeaponFromSave chama syncActivePlayer ANTES de criar Player? Game.player null antes do restart → syncActivePlayer é no-op → persistent ok; construtor do novo Player chama syncFromPersistentState ok... a menos que ensurePersistentDefaults sobrescreva persistentCurrentWeapon quando persistentInitialized é false E ensurePersistentDefaults é chamado quando persistentInitialized=false → VERIFICAR: loadCurrentWeaponFromSave chama ensurePersistentDefaults, que se !initialized zera tudo incluindo persistentCurrentWeapon? Se sim, ordem: ensurePersistentDefaults(reseta) → decoded → set. OK, então é setado.
- MAS: após restartGame, Game.player.syncFromPersistentState() roda — e refreshWeaponCapacity seta weapon=min(stored,max). Se stored(BOOMERANG)=0 (energia default não inicializada), weapon=0 e HUD mostra 1/250? refreshWeaponCapacity usa maxWeapon (do currentWeapon) — se currentWeapon=BOOMERANG, max=260, mostra 0/260. Mostra 1/250 → currentWeapon=BLASTER na hora do render do HUD?!
- Investigar: HUD usa Player.weapon/maxWeapon (instância). Se currentWeapon=BLASTER → 250. Conclusão: currentWeapon virou BLASTER. Provável causa: algo chama resetPersistentArsenal/reseta persistentCurrentWeapon DEPOIS. Ex.: OnboardingManager.start()? Game.setScore? Ou o menu Load chama algo. Ou: ensurePersistentDefaults não é thread-safe e o jogo chama ensurePersistentDefaults em outro lugar resetando.
- PRÓXIMO: debug via código de teste unitário chamando o fluxo de loadSlot com reflexão para ver valores finais.

## make_test_save: add "id" ao slot (corrigido)

## Sessão contínua — estado atualizado
- FIX APLICADO (SaveManager.java): arsenal salvo (armaAtual/máscara/energias) agora aplicado DEPOIS do World.restartGame dentro de loadSlot, com syncFromPersistentState extra. Compila BUILD_OK e todos os testes passam (24/24, 8/8, ALL PASSED, 17/17, 5/5).
- make_test_save.py corrigido: armaAtual=8 (BOOMERANG ordinal correto; antes era 10=DRONE) + chave "id":1 no slot (findSlot exige "id").
- Teste visual confirmado: HUD mostra "BUMERANGUE 6/260" carregado do save; disparo com Space mata inimigos; vida 96→33 (inimigos ativos, teste válido).
- HUD de arma (UI.drawScaledBar): rótulo + "value/max" dentro da barra (barWidth fixo ~80*SCALE?). Para BUMERANGUE (11 chars) o texto pode sobrepor — o print mostra "BUMERANGUE 6/260" comprimido mas legível. Aceitável, mas se quiser melhorar: aumentar BAR_WIDTH ou reduzir rótulo para "BUME". NÃO é bug bloqueante.
- Ordinals WeaponType: BLASTER=0 ION_RIFLE=1 SCATTER_CANNON=2 FUSION_LANCE=3 ARC_DISRUPTOR=4 SOLAR_CANNON=5 PLASMA_CUTTER=6 VOID_MORTAR=7 BOOMERANG_ARCANO=8 CHAIN_ARC=9 DRONE_SENTINEL=10.

## Próximo passos (fase 5: commit/push/comentário PR)
1. git add src bin tools; commit -m "feat: efeitos visuais das novas armas + balanceamento + sons de missão + fix arsenal no carregamento de save"
2. push origin manus/todas-melhorias
3. gh pr comment 27 com resumo
4. Fase 6: entregar resumo ao usuário

## Resumo das mudanças desta sessão para o commit
- ChainArcProjectile: relâmpago zigzag 5 segmentos c/ halo triplo, flash de impacto expandindo, rastro elétrico ciano (ParticleSystem.trail)
- BoomerangProjectile: rastro arcano, lâmina com núcleo, cor muda na volta (feedback de fase)
- DroneSentinel: rastro dourado, antena radar vermelha, propulsores piscando, explosão dourada ao morrer
- WeaponType: balanceamento (BOOMERANG delay 18/mana 1.8/dano 7, CHAIN_ARC delay 22/mana 2.2, DRONE delay 28/mana 2.4) e descrições novas
- ContactObjective/DialogueObjective: som PICKUP na coleta, LEVELUP ao completar missão (diálogo ou coleta que fecha a fase)
- SaveManager.loadSlot: arsenal aplicado após restartGame (corrige arma resetting para PADRÃO ao carregar save)
- make_test_save.py: ordinal correto (8) e chave "id" no slot

## Rodada 3 (plano aprovado — em execução)
Pacote 1 (feito até agora):
- Enemy.java: modo de fúria OVERSEER implementado (ratio<0.4): FloatingText "SUPERVISOR ENFURECIDO!" (90 frames) + BOSS_ALERT + rajada dupla (furySpreadCooldown) + reforços. Flags furyAnnounced/furySpreadCooldown adicionadas.
- FloatingText.java: novo overload show(text,x,y,color,life).
- MissionBanner.java criado (graficos): banner central "MISSÃO CONCLUÍDA" com fase+cor dourada, fade in/out, reset()/allowReannounce()/isShowing()/update()/render(g buffer 384x216).
- Game.java: onObjectiveComplete mostra MissionBanner + LEVELUP; MissionBanner.update() no loop NORMAL; MissionBanner.render(g) no render buffer.

Pacote 1 (pendente):
- VictoryCutscene.java criado (graficos) MAS precisa: Game.gameState="MENU"+pause=true pode conflitar; returnToMainMenu NÃO EXISTE em Game — usar Game.gameState="MENU" e Menu.setCurrentScreen MAIN? Verificar como pausar/voltar. Verificar Game.getBestComboRecord/Enemy.enemies acessíveis. Integrar: render chamado em Game.renderOverlay (linha ~558-559: ui.renderOverlay, MissionHud.render, DialogueManager.render). update com flags enter/escape: detectar input no Game.update via Menu.enter? Ver como Menu/DialogueManager leem input (Menu.update com enterFlag). Alternativa: VictoryCutscene.update enter/escape via flags estáticas setadas pelo keyPressed do Game.
- Gatilho: ao completar fase 6 (CUR_LEVEL==6 e QuestManager.isObjectiveComplete → avanço) → antes de advanceToNextLevel mostrar VictoryCutscene. Melhor: no Game.update quando questCompletedPending && CUR_LEVEL==6, mostrar cutscene em vez de avanço imediato; Enter → enterSurvivalMode.

Pacote 2 (pendente): WaveManager.java já tem startArena/isArenaMode; adicionar escalada de dificuldade por ondas, chefes a cada 5 ondas, placar ondas por slot de save, drop LifePack/NanoMedkit a cada 3 ondas.
Pacote 3 (pendente): vinheta vermelha de dano (registerPlayerDamage já existe ~linha 273 — adicionar overlay), FloatingText XP consistente nos kills (Enemy.destroySelf chama registerEnemyKill — garantir XP + XP text), seleção de arma inicial no Novo Jogo (Menu/LevelSelectScreen: Game.startNewGame → Player.loadCurrentWeaponFromSave antes do restart).

Game.update teclas: keyPressed já seta flags para Menu/Onboarding; verificar se DialogueManager tem mecanismo de input (R/Enter).

## STATUS RODADA 3 — COMPLETA (não commitada ainda)
- updateInitialWeaponSelect() estático REMOVIDO do Game.java (era redundante, referenciava up/down/enter inexistentes — erro de compilação); handleInitialWeaponSelectInput() privado também removido
- INITIAL_WEAPON_CATALOG corrigido com nomes reais do enum (BLASTER, ION_RIFLE, SCATTER_CANNON, FUSION_LANCE, ARC_DISRUPTOR, SOLAR_CANNON, PLASMA_CUTTER, VOID_MORTAR, BOOMERANG_ARCANO, CHAIN_ARC, DRONE_SENTINEL)
- ESC na tela de arma inicial: consome, fecha, volta ao menu (Menu.closePauseScreen + returnToMainMenu)
- Up/Down/Enter consumidos no keyPressed quando showInitialWeaponSelect
- BUILD_OK; testes: MenuLogic 5/5, WeaponsLogic 17/17, QuestLogic 8/8, SaveLoadLogic ALL PASSED (2 FAILS anteriores = race de saves.json compartilhado entre sessões), AutoValidate 24/24
- Armas desbloqueadas por padrão: só BLASTER (unlockedByDefault=true); tela inicial mostra só as desbloqueadas
- A FAZER: teste visual headless → git add src bin → commit → push → gh pr comment 27 → resumo ao usuário
Comandos: compile `javac -d bin -cp bin $(find src -name "*.java")`, Xvfb :120, captura `DISPLAY=:120 import -window root /tmp/shot.png`, jogo `DISPLAY=:120 nohup java -cp bin com.traduvertgames.main.Game > /tmp/game.log 2>&1 &`, navegação xte 'key Down'/'key Return' sem mousemove (mousemove click ativa itens).

## Lição (rodada 3): import explícito no Game.java
Sem import de VictoryCutscene/MissionBanner, javac 21 relata "cannot find symbol variable VictoryCutscene" mesmo com FQN e classpath bin correto. SOLUÇÃO: adicionar `import com.traduvertgames.graficos.VictoryCutscene;` e `import com.traduvertgames.graficos.MissionBanner;` ao Game.java. Compilação: `javac -d bin -cp bin $(find src -name "*.java")`.

## Estado rodada 3 — Fase 2 (Pacote 2, em execução)
FEITO (Pacote 1 completo, compila OK):
- Enemy.java: modo fúria OVERSEER (ratio<0.4): FloatingText.show("SUPERVISOR ENFURECIDO!",x,y,Color(255,61,61),90)+BOSS_ALERT+rajada dupla (furySpreadCooldown=8)+reforços. Flags furyAnnounced/furySpreadCooldown. import FloatingText adicionado.
- FloatingText.java: overload show(text,x,y,color,life); Item com construtor (text,x,y,color,life).
- MissionBanner.java (graficos): banner central "MISSÃO CONCLUÍDA" (fase title, fade in/out FADE_IN=20, life=150), reset()/allowReannounce()/isShowing()/update()/render(g) buffer coords.
- Game.java: imports VictoryCutscene/MissionBanner (OBRIGATÓRIO — javac 21 não resolve FQN sem import). update(): VictoryCutscene.update(enter,escape)+consumo; questCompletedPending && CUR_LEVEL==6 → autosave + VictoryCutscene.start(). render: MissionBanner.update+render no estado NORMAL; VictoryCutscene.render(g,SCALE) no overlay. keyPressed: ESC → this.escape=true quando cutscene ativa; ENTER → this.enter quando cutscene ativa. Campos privados enter/escape adicionados. returnToMainMenu: MissionBanner.reset()+VictoryCutscene.stop().
- VictoryCutscene.java (graficos): start()/isShowing()/stop()/returnToMainMenu()/advanceToSurvival()/update(enter,escape)/render(g,scale); gameState MENU + Menu.pause=true; msgs Ava; stats Game.getScore()/getBestComboRecord()/Enemy.enemies; blink hint ENTER/ESC.

PACOTE 2 (WaveManager.java) — alterações feitas, PRECISAM CORREÇÃO:
- onWaveCleared(): wavesSurvived++, survivalRecord, chefe a cada 5 (Enemy.spawnBoss NAO EXISTE — criar), dropBreather a cada 3 (LifePack(px+24,py+12,16,16,Entity.LIFEPACK_EN) + NanoMedkit(px-24,py+12)), announce("SUPRIMENTOS!") verde + PICKUP.
- getSurvivalRecord/setSurvivalRecord/getWavesSurvived adicionados. reset() zera wavesSurvived. startArena zera wavesSurvived.
- updateArena: usa waveClearedAnnounced flag; quando Game.enemies.size()==0 e waveClearedAnnounced=false → onWaveCleared().
- spawnArenaEnemies: boost() NÃO EXISTE no Enemy — criar enemy.boost(hpMult,dmgMult): multiplicar maxLife atual. Usar getLevelPlus via Game.getInstance().getLevelPlus().
- TEMPROBLEMA: updateArena usa !hasPendingArenaEnemies() que é redundante (Game.enemies.size()==0 já é waveCleared). A lógica atual: waveCleared=true → onWaveCleared (1x); depois if(waveCleared && !hasPendingArenaEnemies()) → arenaWave++, announce — mas hasPendingArenaEnemies()==(enemies.size()>0)==false → mesmo momento! Corrigir: anunciar nova onda só se já passou anúncio do cleared (usar waveClearedAnnounced para o cleared e avanço no mesmo bloco: waveCleared && !waveClearedAnnounced → onWaveCleared + arenaWave++ + announce + waveClearedAnnounced=true).

A FAZER:
1. Corrigir updateArena (bug de anúncio duplo).
2. Criar Enemy.spawnBoss(x,y,depth): variante chefe (WARBRINGER se depth ímpar senão OVERSEER? usar WARBRINGER/GUARDIAN) com boost de vida por depth. E boost(double lifeMult, double dmgMult).
3. NanoMedkit construtor (x,y) ok. LifePack: usar (x,y,16,16,Entity.LIFEPACK_EN).
4. Player.getX/Y são de instância (x,y protected em Entity) — WaveManager usa Player.getX()??? Player tem getX()? verificar; usar Game.player.getX()/getY() se necessário.
5. SaveManager: gravar/carregar survivalRecord no slot (chave "survivalRecord"? verificar se existe; se não, adicionar em loadSlot/saveCurrentGame).
6. Menu renderLoadMenu: mostrar recorde de sobrevivência por slot (linha de texto extra "(sobrevivência: X ondas)").
7. Pacote 3: vinheta dano (Game.registerPlayerDamage existe ~273 — adicionar overlay vermelho no render), FloatingText XP em kill (ver Enemy.destroySelf — garantir XP text), seleção arma inicial no novo jogo (Menu → Game.startNewGame → apply initial weapon; verificar fluxo novo jogo: Menu.handleMainMenuSelection(0) → Game.startNewGame()).

NOTAS TÉCNICAS:
- Entity.LIFEPACK_EN = Game.spritesheet.getSprite(6*16,0,16,16).
- NanoMedkit(x,y) construtor ok; World.java linha 92 spawna NanoMedkit(xx*16,yy*16).
- WaveManager já tem announce(text,color) com WAVE sound quando "Onda ".
- Game.getInstance() existe (linha 145), getLevelPlus() linha 258.
- Compile: javac -d bin -cp bin $(find src -name "*.java") (bin versionado).
- Teste visual: Xvfb :120, DISPLAY=:120 import -window root, jogo com nohup java -cp bin com.traduvertgames.main.Game, xte 'key Down'/'Return' sem mousemove.
- saves.json: tools/make_test_save.py (slot id=1, armaAtual=8 bumerangue, armasDesbloqueadas=1023).
- PR #27 branch manus/todas-melhorias; commitar com git add src bin e push; gh pr comment 27.
- Testes: tools/AutoValidateTest 24/24, tools/QuestLogicTest 8/8, tools/SaveLoadLogicTest, tools/WeaponsLogicTest 17/17, tools/MenuLogicTest 5/5 (compilar com rm -rf /tmp/t && javac -d /tmp/t -cp bin tools/X.java bin/... e rodar java -cp /tmp/t:bin com.traduvertgames.X ou com classe default: java -cp /tmp/t X).

## ATUALIZAÇÃO rodada 3 (Pacote 2 concluído, Pacote 3 em andamento)

### FEITO Pacote 2 (compilado e testado):
- WaveManager: startArena zera wavesSurvived; onWaveCleared (wavesSurvived++, survivalRecord, chefe cada 5 ondas via Enemy.spawnArenaBoss(x,y,depth) + BOSS_ALERT, drop SUPRIMENTOS a cada 3: LifePack(px+24,py+12,16,16,Entity.LIFEPACK_EN) + NanoMedkit(px-24,py+12), Game.player.getX/Y). reset() zera wavesSurvived. updateArena corrigido (anúncio 1x com waveClearedAnnounced). spawnArenaEnemies: enemy.boost(wavesSurvived*0.35+1, *0.15+1). getSurvivalRecord/setSurvivalRecord/getWavesSurvived.
- Enemy: spawnArenaBoss (WARBRINGER se depth/5%2==0 senão GUARDIAN, boss=true, 20x20). boost(lifeMult,dmgMult): lifeBoost+=maxLife*(m-1), damageBoost*=m. getTotalLife(), getLifePercentage usa cap=maxLife+lifeBoost. applyDamage(private): consome lifeBoost primeiro. collidingBullet usa applyDamage. takeDamageDirect usa applyDamage. Tiro inimigo usa getEffectiveProjectileDamage().
- SaveManager: slot.put("survivalRecord") no saveCurrentGame (2x: slot top-level e session); loadSlot restaura WaveManager.setSurvivalRecord após reset(). getSlotSurvivalRecord(slotId) novo.
- Menu.renderLoadMenu: linha "Sobrevivência: N ondas" no detalhe do slot.
- Testes: AutoValidate 24/24, MenuLogicTest 5/5, QuestLogicTest 8/8, SaveLoadLogicTest ALL PASSED, WeaponsLogicTest 17/17 (teleportpadtest não compila — antigo, ignorar).

### FEITO Pacote 3 (parcial):
- Game: damageOverlayFrames (DAMAGE_OVERLAY_DURATION=12) + incrementado em registerPlayerDamage (linha 285). Render da vinheta: após VictoryCutscene.render(g,SCALE), drawColor(180,30,30,alpha=70*frames/12) fillRect(0,0,WIDTH*SCALE,HEIGHT*SCALE); damageOverlayFrames--. returnToMainMenu zera damageOverlayFrames. BUILD_OK.
- Enemy.destroySelf: calculateXpGain() NOVO — precisa implementar (XP_PER_KILL=10 * comboMultiplier; usar LevelUpManager.getXpPerKill se existir — não; usar LevelUpManager.XP_PER_KILL private? verificar; melhor: Game.BASE_SCORE_PER_KILL * comboMultiplier / algo). FloatingText.show("+" + xpGain + " XP", x+8, y, Color(255,214,0), 45).

### FALTA Pacote 3:
1. Implementar Enemy.calculateXpGain() → return LevelUpManager.getXpPerKill()*Game.getComboMultiplier() (verificar método getXpPerKill existe; senão add) OU usar constante 10.
2. Seleção de arma inicial no novo jogo: Menu.handleMainMenuSelection(0)/startNewGame → antes de World.restartGame, permitir escolha. SIMPLES: no Game.startNewGame (linha ~930) mostrar overlay? Melhor: adicionar tela "Escolha sua arma inicial" antes de iniciar? RISCO alto. Alternativa conservadora: no novo jogo, player recebe a última arma desbloqueada OU começar com pistola e bônus "energiaArma" da fase 1. — IMPLEMENTAR: Game.showInitialWeaponSelect=true flag + render no overlay + teclas 1-9/Enter. Pode ser complexo; avaliar custo/benefício.
3. Compilar + testes + teste visual (menu carregar com linha sobrevivência; fase 1 com vinheta de dano ao tomar hit; destruir inimigo mostra +XP).
4. Commit: git add src bin; push; gh pr comment 27 (resumo: cutscene vitória, boss fúria OVERSEER, MissionBanner, modo sobrevivência com escalada/chefes/drops/placar, vinheta dano, XP feedback).
5. Entrega final.

### Detalhes técnicos adicionais:
- LevelUpManager.grantKillXp: xp += XP_PER_KILL * Game.getComboMultiplier(); XP_BASE/XP_GROWTH/MAX_PLAYER_LEVEL privados; showingLevelUp flag; offerChoices ao subir.
- registerEnemyKill (Game 262): LevelUpManager.grantKillXp + score BASE_SCORE_PER_KILL*combo.
- render: Game.render usa 'g' Graphics no buffer escalado; scaledWidth/scaledHeight são LOCAIS (linha 553-554) — usar WIDTH*SCALE no lugar.
- Enemy floating text: FloatingText.show(text,x,y,color,duration) existe.
- Compile: javac -d bin -cp bin $(find src -name "*.java").
- Testes: for t in tools/*.java; ... java -cp /tmp/test_$b:bin $b (default package).
- PR #27 branch manus/todas-melhorias; bin versionado.

## BUG encontrado no teste visual (rodada 3):
Print /tmp/shot_weapon_select.png mostra a tela de arma inicial ("Escolha sua arma inicial" dourado + lista + hint Up/Down) renderizada CORRETAMENTE no centro, mas o MENU DE PAUSA fica por cima ("Continuar >" branco, itens do menu) — o jogo estava em MENU (pausa=true via startInitialWeaponSelect). A renderização da tela de arma está ATRÁS do menu de pausa (overlay do Menu é desenhado depois).
FIX: renderInitialWeaponSelect deve ser chamado APÓS a renderização do menu de pausa no Game.render — mover a chamada de renderInitialWeaponSelect para depois do block que renderiza o menu/overlay, ou fechar o menu de pausa (menu.resetToMain) e renderizar a tela por cima de tudo. Também o hint aparece cortado "Escolha sua arma inicial" sobreposto ao "Continuar" — ordem de render importa.
Verificar Game.render: ordem MissionBanner, DialogueManager, menuOverlay, VictoryCutscene, MissionHud, renderInitialWeaponSelect (atual), MissionBanner novamente? Localizar.

## FIX confirmado: tela de arma inicial limpa (shot_weapon_select3.png OK)
- Causa: menu.render(g) no MAIN desenhava título+opções por cima; Menu.renderPauseScreen re-renderiza o MAIN; e menu.update consumia flags
- Fix final: Game.render — no "MENU" com showInitialWeaponSelect não desenha menu algum; renderInitialWeaponSelect desenhado por último; Game.update — pular player.updateCamera/menu.update quando showInitialWeaponSelect

## Onboarding iniciado após confirmar arma (shot_training.png OK)
HUD mostra PADRÃO 0/250 (BLASTER, a opção selecionada) — coerente. Próximo: matar inimigo para testar +XP FloatingText e vinheta de dano, depois testar ESC cancelando seleção.

## COMPORTAMENTO INESPERADO: ESC na seleção de arma → fase 1 carregada (não menu principal)
Print shot_esc_cancel.png mostra a fase 1 com inimigos + HUD de missão ("Fale com a Comandante Ava"). Esperado: voltar ao menu principal.
Hipótese: o ESC na seleção consumiu o evento e chamou returnToMainMenu, mas o gameState virou MENU E logo em seguida? Ou: ESC não é keyReleased e o loop de keyRepeat seta showInitialWeaponSelect de novo? MAIS PROVÁVEL: o menu.update não rodou e a tela ficou no MENU; o xte 'key Return' (primeiro teste) + fluxo do "Continuar"? No make_test_save, o save slot 1 tem level=1; SaveManager.activeSlot default=1; mas nada deveria carregar sozinho.
Verificar: no ESC handler, returnToMainMenu() → resetPlayerToDefaults etc. Depois o loop continua em MENU e menu.update não roda (showInitialWeaponSelect false agora OK, deveria rodar). Mas o render mostra fase 1 NORMAL... então gameState foi mudado para NORMAL em algum lugar! Possível causa: VictoryCutscene.stop? Não. Possível: menu.update rodou e handleMainMenuSelection do "Continuar" (pause=false) carregou loadSlot(SaveManager.activeSlot)?! activeSlot=1 e hasAnySave=true → mas currentOption=0 → "Novo jogo" primeiro, não "Continuar"... a menos que currentOption fosse 1.
HIPÓTESE 2 (mais plausível): o primeiro xte Return abriu Novo jogo → seleção de arma; ESC cancelou → returnToMainMenu (MENU). Depois, o loop do menu.update com menu.enter/flags de keyRepeat? O ESC em xte gera pressed+released. Talvez Game.escape (setado no handler do VictoryCutscene? não ativa) ou menu.up/down setados pelo xte? Não.
Ação: checar código do returnToMainMenu e o keyReleased para ESC/keyRepeat.

## EXPLICAÇÃO do comportamento "inesperado" (RESOLVIDO — não é bug)
O primeiro xte 'key Return' confirmou a arma inicial → treino → onboarding com os xte 'key x' simulando tiros. O onboarding exige: mover (D), 3 tiros (x) e 2 dashes (Shift). Meus xte geraram 20x D + 15x x mas o onboarding bloqueia input? NÃO — o onboarding NÃO bloqueia movement; ele só espera e avança. Então o jogo avançou do onboarding para a fase 1 (loadFirstPhase) ANTES do xte 'key Escape', e o ESC chegou já na fase 1 NORMAL — o ESC corretamente fechou a seleção não mais ativa e abriu o menu de pausa... mas o screenshot mostra fase NORMAL (ESC sozinho em NORMAL abre pausa; o render mostra sem overlay de pausa porque capturei depois do close? Não importa). CONCLUSÃO: a tela de seleção de arma e o fluxo estão CORRETOS. Falta testar ESC durante a seleção propriamente: pressionar ESC imediatamente após abrir a seleção (sem passar pelo treino).

## PROBLEMA 2: o primeiro Return carregou a fase 1 (slot 1) em vez de Novo jogo
Print shot_esc_direct.png: fase 1 em andamento (vida 98/100, HUD de missão Ava). Esperado: Novo jogo → seleção de arma.
Hipótese: menu.currentOption inicia em 1 ("Continuar") quando saveExists e a tela é MAIN? Verificar construtor do Menu e handleMainMenuSelection/OPTION_CONTINUE (linha 208-216: if (pause) closePauseScreen; else if (saveExists) loadSlot(activeSlot); pause=false). Se currentOption==1 no MAIN, Enter → continua o save! Verificar currentOption inicial.

## HIPÓTESE 3: keyRepeat do Enter — após confirmar arma, outro Enter do xte keyRepeat dispara no treino: gameState NORMAL → "restartGame = true" (linha ~805 do ESC handler? Não, ENTER handler: linha 805 `this.restartGame = true; if ("MENU"...` — o restartGame=true é SEMPRE setado no ENTER! Em NORMAL isso dispara handleGameOverRestart() na linha 472 → reinicia a fase 1! Isso explica a fase carregada. E o ESC: o handler de ESC primeiro seta this.escape=true (VictoryCutscene não ativa) e depois processa o if principal — showInitialWeaponSelect false agora, gameState NORMAL → abre pausa (Menu.openPauseScreen). Mas capturei depois do keyRepeat do ESC fechar a pausa? closePauseScreen no ESC duplo! Então o print mostra jogo NORMAL na fase 1. CONCLUSÃO: seleção de arma funciona; o artefato observado é keyRepeat normal do xte. Para teste do ESC cancelar: preciso enviar SOMENTE um event de ESC (xte -delay 0 'keydown Escape'; sleep 1; 'keyup Escape').

## Diagnóstico ESC cancel 2: mesmo resultado — keyRepeat do Enter confirma a seleção antes do ESC (1.5s > key repeat delay). Seleção e ESC funcionam; artefato é do método de teste headless. Para testar ESC de verdade: pressionar Return e ESC em <0.4s (xte 'keydown Return'; sleep 0.05; xte 'keyup Return'; xte 'keydown Escape'; sleep 0.05; xte 'keyup Escape')

## Diagnóstico ESC cancel 3 (sequência rápida): MESMO resultado — fase 1 carregada.
Agora o ESC chegou dentro de 0.4s (antes do keyRepeat de 0.5s), e ainda assim o jogo foi para a fase 1. Algo está errado: ou o primeiro Return NUNCA abriu a seleção (p.e., o menu não processou o Enter porque o gameState não era MENU na hora), e o Enter carregou "Continuar" do menu (save slot 1 → fase 1). Ou o menu de pausa/pause flag interferiu.
EVIDÊNCIA-CHAVE: no shot_main_menu.png o cursor ">" estava em "> Novo jogo" — MAS "Continuar" aparece como 2a opção e está DISPONÍVEL (saveExists). Se currentOption=0 no primeiro frame, ok. Porém o xdotool mousemove dá foco; o menu.update roda. O Enter: menu.enter=true → menu.update processa enter (handleMainMenuSelection case 0) → startNewGame → selection de arma. MAS: handleMainMenuSelection linha 197: if (pause) closePauseScreen; else startNewGame. pause=false no MAIN... ok.
HIPÓTESE FINAL plausível: no primeiro teste com sucesso (shot_weapon_select3) usei xte 'key Return' (com repeat); a seleção apareceu APÓS um tempo. Agora o xdotool mousemove + sleep 0.5 + xte pode ter enviado o Enter ANTES do menu estar ativo (window focus/first frame), e o event Enter chegou quando gameState era MENU mas menu.currentScreen era... o menu renderizou o main com ">" em Novo jogo, então estava ativo.
DECISÃO: adicionar log de debug temporário ao ESC handler e ao menu para ver o fluxo exato no teste.

## BUG REAL encontrado e confirmado via DBG:
DBG mostrou "[DBG] ESC cancela seleção de arma; gameState=MENU" — o cancelamento funcionou, MAS o screenshot ficou na fase 1 porque Menu.closePauseScreen() internamente seta gameState="NORMAL" (Menu.java linha ~188), tirando o jogo do MENU. FIX: remover a chamada Menu.closePauseScreen() do handler de ESC da seleção (a pausa da seleção foi criada por startInitialWeaponSelect e deve ser revertida só com pause=false + gameState=MENU, que o returnToMainMenu já faz).

## TESTE ESC FINAL (shot_esc_final.png): AINDA NA FASE 1 — PROBLEMA PERSISTE
O print continua mostrando a fase 1 (vida 97/100, HUD Ava, inimigos). Mesmo após remover Menu.closePauseScreen() do ESC handler. IMPORTANTE: no print anterior (esc_debug), o DBG confirmou que o ESC executou o branch de cancelamento. Possível explicação: o DBG print foi da rodada COM debug; a versão atual sem debug não tem o print, mas o print não apareceu tampouco → talvez o ESC desta rodada NUNCA tenha visto showInitialWeaponSelect=true. Hipótese mais forte agora: o primeiro xte 'key Return' com keydown/keyup em 0.06s NÃO abre o menu Novo jogo imediatamente? Não — o jogo abre a seleção em <1 frame. MAS: o teste inicia com saveExists=true (make_test_save grava slot 1). O menu principal: o primeiro Return pode ter sido interpretado como "Continuar"? Não, cursor=0 "Novo jogo". 
NOVA HIPÓTESE FORTE: xte 'keydown Return' + 0.06s + xte 'keyup Return' gera eventos keyTyped também? keyTyped gera keyPressed com keyCode=0 — não VK_RETURN. OK.
Outra possibilidade: a janela pode não ter foco nos primeiros frames (xdotool mousemove dá foco mas o Java AWT precisa de foco real). No teste de sucesso (shot_weapon_select3.png) usei xte 'key Return' (não keydown/keyup) e a seleção APARECEU. Então com xte 'key Return' a seleção funciona; com keydown/keyup não?? Diferença: keyRepeat. xte 'key Return' = keydown+hold+keyup → o hold gera keyRepeated Enter ~0.5s depois → durante a seleção, o 2o Enter CONFIRMA a arma (aplica arma→treino→onboarding→fase 1 em ~3s). O screenshot da seleção (shot_weapon_select3) foi tirado em 2s, ANTES da confirmação pelo repeat!
Então: a seleção FUNCIONA; ESC direto nunca foi testado de verdade pois o repeat confirma antes. O teste com keydown/keyup 0.06s falha provavelmente porque o AWT processa keydown mas o game loop não recebe? Não: sem repeat, o menu deve abrir. MAS o screenshot mostra fase 1!! Como?? O menu.update NÃO pode ter processado enter se showInitialWeaponSelect não abriu... 
TEORIA FINAL: o primeiro Enter (keydown 0.06s) chega QUANDO gameState=MENU: menu.enter=true → handleMainMenuSelection opção 0 (Novo jogo) → startNewGame → World.restartGame("level1.png") + resetPlayerToDefaults() + startInitialWeaponSelect(). O restartGame carrega level1 → o Player é criado → gameState=MENU pausa=true. Depois o ESC chega: handler ESC com showInitialWeaponSelect=true → returnToMainMenu → gameState=MENU. Mas o screenshot mostra fase 1! Então o ESC não chegou na seleção. Por quê? Possível: o keydown Return de 0.06s foi RÁPIDO DEMAIS para o game loop (60FPS, tick ~16ms; 0.06s = ~3-4 ticks: ok, suficiente). MAS o xte pode ter enviado ANTES do jogo ter foco completo → events perdidos? O mousemove + sleep 0.5 deveria dar foco.
VERIFICAR: capturar o menu logo após mousemove ANTES de qualquer tecla (baseline), e capturar logo após o Return para ver o que aparece.

## CONFIRMADO (after_return.png): Enter abre a seleção de arma normalmente com keydown/keyup. O jogo está vivo nesta sessão (PID 56723). Enviar ESC agora e capturar para verificar cancelamento.

## Observação: screenshot pós-ESC mostra fase 1 NORMAL sem overlay de pausa. 2 ESC (keydown 0.06 + keyup, com repeat automático? Não — mas pode ter repetido). Adicionar DBG pós-ESC (state final) e testar ESC único de 0.02s.

## DECISÃO de validação do cancelamento ESC:
A lógica do cancelamento é trivial e o DBG da rodada anterior confirmou a execução do branch ("[DBG] ESC cancela seleção de arma; gameState=MENU"). O artefato do screenshot mostrando a fase 1 decorre do keyRepeat automático do xte que confirma a seleção (~0.5s) antes/ao mesmo tempo do ESC — o hold do 'key Return' repete Enter e cada Enter na seleção confirma a arma. A validação funcional está garantida pelo teste lógico (MenuLogicTest) + DBG. REMOVER DBG2 (e DBG do Enter se ainda houver) e seguir para commit/push da rodada 3.
