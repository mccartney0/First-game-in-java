# Rodada 17 — Objetivos variados por fase: mapeamento da arquitetura

## Branch e estado
- Branch: `manus/objetivos-fase` (a partir da main, já com PR #31 mergeado na main local 3658165; push ok).
- Build: `cd /home/ubuntu/First-game-in-java && javac -d bin -cp bin $(find src -name "*.java")`
- Testes: `javac -d $out -cp bin:res tools/X.java && java -cp $out:bin:res X` (DISPLAY=:120 p/ testes com Game).
- Suíte regressiva existente: ShopQaTest (19/19), MenuNavigationTest (12/12), AutoValidate (24/24), StoryNpcPlacementTest (39/39), PhaseTransitionTest, GameOverUxTest (11/11).
- PR #31 (companions-ux) está OPEN no GitHub mas mergeado LOCALMENTE na main para não travar. Ao final: se ainda open, fechar via gh ou mencionar ao usuário.

## Como missões funcionam
- Interface `RPGObjective` (quest/): onLevelStart, onLevelLoaded, update, onQuestItemSpawned/Collected, onBeaconSpawned/Activated, onNpcSpawned/Rescued, onEnemyKilled, onDialogueStarted/Finished, getTitle/Description/ProgressText, isComplete, serialize/deserializeState, getTargetHint.
- `BaseObjective(title, description)` implementa RPGObjective.
- `QuestManager`: currentObjective estático, createObjectiveForLevel(level): 1=ContactObjective, 2=Dialogue(BossHunt,Nia), 3=Dialogue(Ritual,Ivo), 4=Dialogue(Rescue,Mercúrio), 5=Dialogue(DataRecovery,Ivo), 6=Dialogue(BossHunt(Supervisor),Ava), 7=Dialogue(Sabotage,Ava), 8=InfiltratorObjective, 9=NullObjective (sobrevivência).
- Notify: QuestManager.registerQuestItem/collectQuestItem/registerBeacon/activateBeacon/registerNpc/rescueNpc/notifyEnemyKilled/notifyDialogueStarted/Finished.
- Game.update: `if (QuestManager.isObjectiveComplete()) onObjectiveComplete();` → questCompletedPending=true → ShopManager.open() abre a loja → ao fechar avança fase.

## Entidades de missão e pixels dos mapas (World.applyMapPixels)
- 0xFFFFC107 QuestItem(amarelo) | 0xFF4CAF50 QuestBeacon(verde) | 0xFF795548 QuestNPC(marrom) | 0xFF00897C CommanderNpc(Ava).
- NPCs suporte: 0xFF66BB6A engenheira Nia, 0xFF5E35B1 pesquisador Ivo, 0xFFFF9800 armeiro Mercúrio, 0xFFFFB74D EngineerNPC.
- Contagem por mapa (beacon/item/npc/ava): level1: 0/4/0/1, level2: 0/0/0/1, level3: 3/0/0/1, level4: 0/1/2/1, level5: 0/3/0/1, level6: 0/0/0/1, level7: 0/3/0/1, level8: 0/0/0/1. Mapas 32x22 até 46x30.

## Plano de implementação (objetivos variados por fase)
Mantendo compatibilidade total (mapas, saves, fluxos):
1. **Fase 2 — "Defender o ponto" (HoldObjective)**: o jogador deve ativar E defender um beacon por X segundos enquanto ondas de inimigos atacam o ponto. Se inimigos chegarem perto demais do beacon durante a defesa, o canal regredia. Usar QuestBeacon existente (registrar + defender). Novo método onEnemyNearTarget não existe na interface — implementar com distância via Game.entities (iterar inimigos vivos no update do objetivo) OU usar raio próprio do objetivo com posição do beacon.
2. **Fase 6 — "Sobreviver" (SurviveObjective)**: sobreviver N segundos contra ondas intensas (WaveManager existente). Usar frames de jogo.
3. **Fase 8 — "Escoltar NPC" (EscortObjective)**: proteger o escorted NPC que se move em direção ao ponto de fuga; se morrer, falha; se chegar, vitória. Subclasse de QuestNPC com movimento e vida.
4. Integrar: QuestManager.createObjectiveForLevel retorna esses objetivos nas fases 2, 6, 8 (mantendo diálogo prévio onde faz sentido via DialogueObjective? — simplificar: fase 2 usa HoldObjective puro com banner; fases 6 e 8 mantêm chefe → combinar: HoldObjective + boss? Melhor: fase 2 Hold, fase 6 Survive (sem chefe), fase 8 Escort → chegada conclui).
   - Decisão: NÃO alterar fases existentes com chefes (2,6,8 têm chefes?) — level2/6/8 não têm boss pixel específico; BossHunt conclui ao matar boss (isBoss). Fase 2 atual: Dialogue(BossHunt,Nia).
   - ESCOLHA FINAL: adicionar MODIFICADORES, não substituir: 
     - Fase 2: Dialogue(HoldObjective(...), Nia) — conversar com Nia, depois defender o beacon.
     - Fase 6: Dialogue(SurviveObjective(...), Ava) — sobreviver 240s.
     - Fase 8: InfiltratorObjective (mantém chefe) + EscortObjective? Não — InfiltratorObjective precisa matar boss. Simplificar: Fase 8 mantém Infiltrator; Escort entra na fase... não há fase livre.
   - REVER: fases: 1 contato, 2 boss hunt, 3 ritual, 4 resgate, 5 data recovery, 6 boss supervisor, 7 sabotagem+boss, 8 infiltrator boss.
   - MELHOR PLANO: objetivos variados COMPOSTOS que enriquecem as fases existentes:
     - Fase 2: BossHunt vira "Defesa do Warbringer": depois do diálogo, o jogador deve defender 1 beacon enquanto o Warbringer emerge; defender beacon → liberar boss hunt (chain).
     - Fase 6: BossHunt(Supervisor) + Survive prefix: sobreviver N segundos primeiro, depois localizar o Supervisor.
     - Fase 8: Infiltrator + Escort: escoltar o infiltrado até o núcleo antes de enfrentar o PRIME.
   - Implementação de composição: novo `CompositeObjective`? Simples: cada objetivo específico implementa a cadeia internamente (estado machine).
5. HUD: MissionHud já mostra getObjectiveProgress — adicionar indicador visual de tempo (Survive) e integridade do ponto (Hold) na HUD? MissionHud já renderiza título+progresso no topo. Adicionar barra/indicador simples na HUD para hold (barra do canal) e timer para survive — MissionHud tem acesso a Game.player... fazer na HUD com dados do QuestManager (getProgressText com emojis já cobre). DECISÃO: enriquecer MissionHud com barra de defesa (verde →vermelho) quando objetivo ativo tem isDefending(), e timer de sobrevivência grande.
6. Sons: usar EVENTs existentes (LEVEL_COMPLETE no sucesso). Talvez novo som DEFENSE_ALARARM? Não criar WAV novo desta vez (manter escopo); reusar.

## Testes novos
- tools/ObjectivesVariadosTest.java: valida HoldObjective (progresso + conclusão), SurviveObjective (timer), EscortObjective (NPC vivo + chegada), Composite/chain behavior, progresso persistido serialize/deserialize (save v3), missão conclui sem erro.

## Arquivos a modificar
- src/com/traduvertgames/quest/: HoldObjective.java (novo), SurviveObjective.java (novo), EscortObjective.java (novo, + EscortNpc.java em entities), QuestManager.java (createObjectiveForLevel 2,6,8)
- src/com/traduvertgames/entities/EscortNpc.java (novo, com vida + movimento para ponto de fuga)
- src/com/traduvertgames/graficos/MissionHud.java (barra de defesa + timer)
- World.java: pixel novo para EscortNpc? — usar spawner programático via QuestManager.onLevelLoaded com posição fixa (não precisa alterar mapa).
- tools/ObjectivesVariadosTest.java, tools/rodada17_notas.md

## Cuidados
- Saves v3: serializeState para as novas classes (formato próprio, robusto a versões).
- Fase 8 tem InfiltratorObjective com serializeState BRIEFING=/BOSS= — compor Escort dentro: Infiltrator com fase prévia de escolta (state "ESCORT=alive|arrived").
- BossHuntObjective é final — não subclassável! Hold/Survive precisam ser próprios (não extender).
- BossHunt.registerBossPresence é chamado por QuestManager.notifyBossSpotted (quem chama? procurar "notifyBossSpotted").

## Progresso de implementação (round 17)
- [x] HoldObjective.java criado (defender beacon, DEFENSE_RADIUS=90, CHANNEL_MAX=600, canal avança 1/frame sem invasores, regride 2/frame por invasor). Usa Game.entities (inimigos mortos já removidos da lista — Enemy não tem isAlive). Som LEVELUP ao completar. serialize: SPAWNED=/CHANNEL=.
- [x] SurviveObjective.java criado (default 60s, FPS=60). Timer avança só em gameState NORMAL. getRemainingSeconds/getTimeProgress para HUD. serialize: FRAMES=/DURATION=.
- [x] EscortNpc.java criado (SPEED 0.6, MAX_HP=3, FEAR_RADIUS 120, HIT_INVULN 45). Métodos: takeHit(), hasArrived(), getHp(), escapeTargetX/Y(), distanceTo(x,y), distanceFromSpawn() — PRECISA ADD MÉTODOS no EscortNpc: escapeTargetX/Y (getters de escapeX/Y) e distanceFromSpawn() (distância da origem ao destino). QuestManager.registerEscort(this) e QuestManager.escortFailed(this)/escortArrived(this) — PRECISA ADD no QuestManager.
- [x] EscortObjective.java criado (THREAT_RADIUS=70, ATTACK_INTERVAL=60). update conta inimigos na zona e chama escort.takeHit(). serialize: FAILED/ARRIVED/ALIVE.
- CONFIRMADO: player spawna level2=(3,3), level8=(3,3). Beacon fase 2 (17,11) centro; escolta fase 8 spawn (8,10), fuga (42,26), mapa 46x30 OK. QuestBeacon construtor (int,int,Color).
FALTA: métodos auxiliares no EscortNpc; hooks no QuestManager (registerEscort/escortFailed/escortArrived); hook de dano no Enemy vs EscortNpc (inimigos atacam? por enquanto só timer de ameaça do objetivo; OK); integrar no QuestManager.createObjectiveForLevel: fase 2 = DialogueObjective(new HoldObjective(), "Engenheira Nia") — CONFERIR se fase 2 tem beacon no mapa (level2: beacon=0!) — phase2 não tem beacon pixel! PRECISA adicionar beacon na fase 2 OU mudar plano: usar pixel 4CAF50 não existe em level2. OPÇÃO: adicionar spawn programático do beacon na fase 2 (onLevelStart posiciona? Não dá: beacon é criado só via mapa). MELHOR: editar o PNG level2.png adicionando pixel beacon (76,175,80) em posição segura — via PIL (script). Similarly fase 8: EscortNpc precisa spawno programático — criar um método QuestManager.spawnEscort(level) chamado em onLevelLoaded? onLevelLoaded é chamado após o mapa carregar; Game.entities.add(new EscortNpc(x,y,escapeX,escapeY)) funciona (posição fixa por fase). Também fase 6 SurviveObjective — não precisa de nada no mapa (timer), MAS a fase 6 atual é Dialogue(BossHunt(Supervisor),Ava) com chefe; SURVIVE substitui? A fase 6 sem itens no mapa conclui via boss. SE sobreviver 60s é o objetivo, não precisa mais de boss? MAS boss do supervisor é spawado como pixel? level6: beacon=0 item=0 npc=0 ava=1 — o boss é spawado pelo WaveManager/Enemy.spawn? Procurar onde o boss da fase 6 é gerado (BossHuntObjective.registerBossPresence + Enemy.spawnBoss?). DECISÃO DE DESIGN (a confirmar): fase 2=Hold(2 beacons programáticos? ou 1 no PNG), fase 6=Survive 90s (sem boss? manter BossHunt não faz sentido sem boss). Verificar como chefe da fase 6 spawna: grep "WARBRINGER\|OVERSEER\|boss" em WaveManager/Enemy/Game.

## Pontos críticos restantes
1. Verificar spawn do chefe da fase 6 (se é o WaveManager ou pixel) — se for WaveManager com isBoss, manter chefe E sobreviver? Simplificar: fase 6 = SurviveObjective puro (60s), chefe não necessário? MAS WaveManager pode spawar boss em fases pares? VERIFICAR.
2. Level2 PNG sem beacon: adicionar 1 beacon via PIL (escolher tile chão, não paredão).
3. Fase 8 Escort: spawno programático do EscortNpc (posição x,y de um tile vazio do level8 + destino) — usar NPC de apoio existente? Não, EscortNpc novo. Escolher tiles: level8.png 46x30; usar pixel 5E35B1? não — usar spawn programático em coordenadas fixas (ex.: spawn perto do jogador? Player nasce onde? procurar spawn player).
4. QuestManager hooks: registerEscort(EscortNpc), escortFailed, escortArrived — e failed → banner (MissionBanner.show) + reiniciar fase? RESTART: chamar Game.restartCurrentLevel? Existe método restart/restartLevel no Game/World? procurar.
5. HUD: MissionHud — adicionar barra de defesa (para HoldObjective) e timer grande (SurviveObjective). Acessar objetivo atual: QuestManager.getCurrentObjective() não existe — ADD ou usar instanceof no HUD? HUD não conhece classes; adicionar métodos getObjective(): RPGObjective ao QuestManager.
6. Inimigos NÃO atacam EscortNpc naturalmente (só Player)? Verificar Enemy.update colisão com entidades (colide só com player/bullets?). Se não, o timer de ameaça do objetivo basta.
7. Teste: tools/ObjectivesVariadosTest.java com Xvfb (DISPLAY=:120).
8. Sons: usar LEVELUP existente (já feito nos 3 objetivos).
9. InfiltratorObjective da fase 8: se substituirmos por EscortObjective, perder boss fight. ALTERNATIVA: manter Infiltrator na fase 8 e mover Escort para... não há fase livre. DECISÃO: fase 8 = EscortObjective (chegada conclui) — o boss OVERSEER PRIME era o final narrativo da campanha (linha 492 Game: questCompletedPending && CUR_LEVEL==8 → VictoryCutscene). SE fase 8 concluir por escolta, a cutscene final ainda roda? Ver linha 488-500 do Game.

## DECISÃO FINAL DE DESIGN (round 17)
Chefes por mapa: level2 WARBRINGER(1), level5 WARBRINGER(1), level6 OVERSEER(1), level7 GUARDIAN(1)+4 PHANTOM, level8 OVERSEER_PRIME(1)+4 PHANTOM. ensurePhaseBoss: fase>=2 sem boss no mapa → spawna WARBRINGER (fase 6 OVERSEER). BossHunt funciona via isBoss.

Plano de integração (compatível com bosses existentes):
- Fase 2: manter Dialogue(BossHunt, Nia) + NOVO HoldObjective COMO SEQUÊNCIA: novo wrapper SequenceObjective(delegates...) que executa na ordem — primeiro Hold, depois BossHunt. Implementar SequenceObjective genérico.
  - level2 NÃO tem beacon → adicionar beacon programático: QuestManager.onLevelLoaded pode criar Entity e add em Game.entities. HoldObjective registra via onBeaconSpawned — criar QuestBeacon no onLevelStart ou onLevelLoaded do objetivo (chamar QuestManager.registerBeacon).
- Fase 6: manter Dialogue(BossHunt(Supervisor), Ava) + Survive pré-fase? Melhor: Sequence(Survive 45s, BossHunt). Sobreviver libera a "porta" do supervisor? BossHunt registra presença ao ver boss. Sequence: Survive completo → objetivo vira BossHunt.
- Fase 8: manter InfiltratorObjective (briefing + boss PRIME) + Escort pós-briefing? Infiltrator já tem briefing; adiciona Escort entre briefing e boss: Sequence(Escort, BossHunt do PRIME)... Infiltrator é final: BRIEFING= + BOSS=. Compor: Sequence(EscortObjective, InfiltratorObjective).
- SequenceObjective: novo arquivo quest/SequenceObjective.java: lista de RPGObjective + índice ativo; eventos delegados ao ativo; serializa "IDX=" + estado do ativo.
- Beacon fase 2: criar em QuestManager.onLevelLoaded OU dentro do HoldObjective.onLevelStart/onLevelLoaded via onLevelLoaded (World chama após mapa). HoldObjective.onLevelLoaded: se !spawned, criar QuestBeacon(centro-ish do mapa). Coordenadas por fase: QuestManager.getCurrentLevel(). level2 34x22 tiles → centro (17*16,11*16). Mas precisa ser tile livre (chão). Usar posição com fallback via World.isFreeTile(isValidTile).
- Escort fase 8: EscortNpc spawn programático em onLevelLoaded (QuestManager.spawnEscort(level)). level8 46x30: spawn perto (10*16,10*16), destino (35*16,20*16). Validar tiles livres com isValidTile via World (public static isValidTile).
- HUD: adicionar no MissionHud barra/timer usando QuestManager.getCurrentObjective() (novo getter que retorna o objetivo ATIVO da sequência — SequenceObjective precisa de getActive()). HUD mostra: se HoldActive → barra de canal; se SurviveActive → timer grande "60s".
- Game.java update: nada novo (update já chama QuestManager.update() que propaga).
- Falha da escolta (escortFailed): MissionBanner.show vermelho "Escolta comprometida!" + reiniciar fase. RESTART FASE: existe World.restartGame("levelN.png") estático → chama Game.world restart + QuestManager.prepareForLevel. MAS o jogador perderia vida atual? usar restartGame — mantém save? Não, restart zera stats. Aceitável: banner pede para o jogador reintentar; vida/player reset — ok (padrão da engine). Adicionar hook: Game.restartCurrentLevel()? Game.advanceToNextLevel é estático em Game; restartGame é World.restartGame (static). Game.java tem campos restartGame boolean + saveGame. Usar World.restartGame("level"+CUR_LEVEL+".png") + reset de estado game over. Simples: no QuestManager.escortFailed → Game.restartCurrentLevel() novo método estático: CUR_LEVEL reset? não, mesma fase. World.restartGame(restart). Cuidado: Game.update verifica gameState NORMAL.

## PROGRESSO v2 (após edits)
- [x] HoldObjective.java — ok
- [x] SurviveObjective.java — ok
- [x] EscortNpc.java — OK (tem escapeTargetX/Y(), distanceTo(), distanceFromSpawn(), initialX/Y capturados no construtor, takeHit(), hasArrived(), getHp())
- [x] EscortObjective.java — OK com onEscortSpawned/onEscortFailed(onEscFailed → banner vermelho + QuestManager.restartCurrentLevel())/onEscortArrived(LEVELUP)
- [x] SequenceObjective.java — OK com getActive(), onEscortEvent(Consumer<EscortStage>), interface EscortStage (onEscortSpawned/Failed/Arrived)
- [x] QuestManager.java — edits aplicados: hooks registerEscort/escortFailed/escortArrived (delegam a EscortObjective OU SequenceObjective.onEscortEvent quando active é EscortObjective), getCurrentObjective(), restartCurrentLevel() (World.restartGame), fases: 2=Dialogue(Sequence(Hold, BossHunt), Nia), 6=Dialogue(Sequence(Survive 45s, BossHunt Supervisor), Ava), 8=Sequence(Infiltrator, Escort). Import World adicionado.
- FALTA:
  1. HoldObjective: level2 não tem beacon no mapa → criar beacon programático no onLevelLoaded/onLevelStart quando getCurrentLevel()==2 e !spawned: new QuestBeacon(x,y,color) + Game.entities.add. Coordenadas: nível 34x22, centro (17*16, 11*16) — validar com World.isValidTile(17,11)? (World.isFreeTile existe? linha 236: public static boolean isFree(int xNext,int yNext, int zplayer)). Usar World.isFreeTile ou isFree. Conferir signature World.java:236: `public static boolean isFree(int xNext,int yNext, int zplayer)` — zplayer=0/1? player usa zplayer=0 para chão? Conferir caller. Alternativa mais simples: procurar tile livre perto do centro (loop try tile).
  2. EscortObjective fase 8: criar EscortNpc programático no onLevelLoaded (só fase 8): posição inicial (10,10)*16 e destino (35,20)*16 — validar tiles livres; add Game.entities.
  3. HUD: MissionHud — adicionar barra de canal (HoldObjective.getChannelProgress()) e timer (SurviveObjective.getRemainingSeconds()) no card da missão; usar QuestManager.getCurrentObjective() e desembrulhar DialogueObjective.getDelegate? NÃO EXISTE! DialogueObjective não expõe delegate — adicionar getDelegate() público no DialogueObjective (privado atualmente) OU no HUD unwrap: enquanto (obj instanceof DialogueObjective) obj = ((DialogueObjective)obj).getDelegate(); enquanto instanceof SequenceObjective → getActive(). Cuidado: getActive retorna NullObjectiveHolder quando vazio — ok.
  4. MissionBanner: verificar assinatura show(String,String,Color,Color,int) existe (usada em DialogueObjective linha ~53 — confirmar).
  5. SoundManager.Event.LEVELUP usado em Sequence/objetivos — existe (ContactObjective usa).
  6. Teste ObjectivesVariadosTest.java (DISPLAY=:120 p/ Game): hold progressão, survive timer, escort (chegada/falha), sequence (2 etapas), serialize/deserialize.
  7. Build check após tudo.
  8. PR: branch manus/objetivos-fase já pushada (empty, ok).
- Build cmd: `cd /home/ubuntu/First-game-in-java && javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep error | head -3; echo BUILD_OK`
- World.restartGame(String) é static, reinicia map+prepareForLevel+ensurePhaseBoss — cuidado loop recursivo se escort falhar durante restart? onEscortFailed→restartCurrentLevel→World.restartGame→QuestManager.prepareForLevel(nível 8)→novo Sequence(Infiltrator(novo), Escort(novo)) — o EscortNpc novo chama registerEscort no construtor OK, sem loop (restart não passa por escortFailed). OK.
- InfiltratorObjective: serializa "BRIEFING=;BOSS=" — verificar se Sequence serialize/deserialize funciona com Infiltrator (usa ";S0=" prefix + Infiltrator.serializeState) — Infiltrator usa split(";") — risco: formato Infiltrator usa ";" como separador interno?? Infiltrator state: "BRIEFING=true;BOSS=false" — quando Sequence faz state.split(";") → partes "IDX=0","S0=BRIEFING=true","BOSS=false"! DESERIALIZE QUEBRA. CORRIGIR: Sequence deve separar estágios por algo único (ex "||") ou Infiltrator deve escaping. MELHOR: mudar separador do Sequence para "||" no serialize/deserialize.

## PROGRESSO v3
Feitos: DialogueObjective.getDelegate() adicionado; SequenceObjective usa separador "|" (split("\\|")) para evitar conflito com ";" interno do Infiltrator; Dialogue serialize usa ";DELEGATE=" (ok — Dialogue não passa estado bruto do delegate ao split(";")? NA VERDADE Dialogue serializeState() = "TALKED=..;DELEGATE=..." — o Dialogue deserialize usa indexOf(';') e passa todo o resto ao delegate.deserializeState — funciona pois delegate (Sequence) faz split("\\|"). OK.
FALTAM (checklist):
1. HoldObjective fase 2: criar beacon programático no onLevelLoaded: `if (QuestManager.getCurrentLevel()==2 && !spawned) { QuestBeacon b = new QuestBeacon(x,y,0xFF4CAF50); Game.entities.add(b); QuestManager.registerBeacon(b); }` — verificar construtor QuestBeacon (parâmetros) e coordenadas válidas em level2 (34x22): centro (17,11) usar World.isFreeTile(17,11) (verificar signature: public static boolean isFree(int xNext,int yNext, int zplayer) — zplayer=0?). Se isFree não é tile-based, usar pixel-check via World.tiles.
2. EscortNpc fase 8: criar no EscortObjective.onLevelLoaded: só fase 8 e !spawned: findFreeTile perto (10,10); destino (35,20); new EscortNpc(px,py,dx,dy); Game.entities.add.
3. HUD (MissionHud.java): unwrap do objetivo ativo para mostrar barra Hold/timer Survive. Pattern: RPGObjective o = QuestManager.getCurrentObjective(); while (o instanceof DialogueObjective) o = ((DialogueObjective)o).getDelegate(); while (o instanceof SequenceObjective) o = ((SequenceObjective)o).getActive(); depois instanceof HoldObjective → barra; instanceof SurviveObjective → timer.
4. MissionBanner.show(title,subtitle,titleColor,subColor,lifeFrames) — assinatura confirma (linha 44). OK.
5. Teste tools/ObjectivesVariadosTest.java + build + PR.
6. Verificar que World.isFree existe: grep "isFree" em World.java — linha 236 `public static boolean isFree(int xNext,int yNext, int zplayer)`.

## PROGRESSO v4 (antes da compactação)
IMPLEMENTAÇÃO CONCLUÍDA (código-fonte):
- HoldObjective.java OK: onLevelLoaded cria beacon programático fase 2 (tile 17,11 via World.isValidTile/isWallTile, cor 0x4CAF50), banner "DEFESA NECESSÁRIA"; métodos isActive/isUnderAttack/getPercentText/getChannelProgress.
- SurviveObjective.java OK (45s default na fase 6 via QuestManager, timer gameState NORMAL, LEVELUP ao completar).
- EscortNpc.java OK: imports Game, SoundManager, FloatingText (entities), QuestManager, Camera; campos initialX/Y; setX/setY com cast (int).
- EscortObjective.java OK: onLevelLoaded cria EscortNpc fase 8 (spawn 8,10; fuga 42,26 em mapa 46x30), banner "INFORMANTE LOCALIZADO"; onEscortSpawned/Failed (banner vermelho + QuestManager.restartCurrentLevel)/Arrived (LEVELUP).
- SequenceObjective.java OK: getActive(), onEscortEvent com Consumer<EscortStage>, interface EscortStage, serializa com separador "|" (split("\\|")), fases 2 e 8 usam Sequence; getDelegate() em DialogueObjective.
- QuestManager.java OK: hooks registerEscort/escortFailed/escortArrived (delegam a EscortObjective ou SequenceObjective.onEscortEvent), getCurrentObjective(), restartCurrentLevel (World.restartGame), fases 2/6/8 atualizadas, import World.
- MissionHud.java OK: unwrapObjective (Dialogue→delegate, Sequence→getActive), drawObjectiveWidgets: barra central Hold (verde/vermelho "DEFESA SOB ATAQUE!") e timer grande central Survive (verde→vermelho ≤10s).
Player spawn: level2=(3,3), level8=(3,3). Beacon fase2 (17,11) centro ok; escolta fase8 (8,10)→(42,26) ok.
Build: cmd `cd /home/ubuntu/First-game-in-java && javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep error | head -5; echo BUILD_OK`
FALTA:
1. Rodar build (após fix dos imports) — já feito v3.
2. Verificar Entity.setX/Y aceitam int (sim, linhas 79/83); setMask existe.
3. Criar tools/ObjectivesVariadosTest.java (reflection style do ShopQaTest, Xvfb DISPLAY=:120): testes p/ Survive (timer), Hold (canal), Escort (chegada/falha), Sequence (avanco de etapa), serialize/deserialize, spawn programático beacon fase 2 e escolta fase 8 (inicializar Game minimamente?).
4. Rodar suíte regressiva: MenuNavigationTest 12/12, AutoValidate 24/24, StoryNpcPlacementTest 39/39, PhaseTransitionTest, ShopQaTest 19/19, GameOverUxTest 11/11 — todos em tools/.
5. Commitar + push + PR #32 (branch manus/objetivos-fase, remota já existe vazia).
6. Mensagem final ao usuário com instruções de merge/teste.

## PROGRESSO v5
CONFIRMADO: chefes fixos por pixel nos mapas — WARBRINGER (E91E63) no level2, OVERSEER (7986CB) no level6/8, OVERSEER_PRIME (D01937) level8. Sequências fase 2 (Hold→BossHunt) e 6 (Survive→BossHunt) e 8 (Infiltrator→Escort→) coerentes com os chefes existentes. BUILD_OK obtido.
NEXT: criar ObjectivesVariadosTest (reflection, igual padrão ShopQaTest) + suíte regressiva + commit + PR.

## PROGRESSO v6
- Corrigidos hooks de escolta em QuestManager (delegam também ao estágio de SequenceObjective via onEscortEvent).
- RPGObjective.onBossSpotted() default adicionado; propagado em DialogueObjective e SequenceObjective; QuestManager.notifyBossSpotted agora chama interface (corrigia bug latente: fases 2/6 jamais concluiriam, pois BossHunt estava dentro de wrappers que não propagavam).
- BossHuntObjective.onBossSpotted chama registerBossPresence.
- BUILD_OK confirmado.
- tools/ObjectivesVariadosTest.java criado (testes: canal Hold avança/regressão/conclusão, Survive 3s timer NORMAL, Sequence avanço de etapa + boss spotted + conclusão + serialize/deserialize, DialogueObjective.getDelegate, escala de fase). Fix de compilação feito (Constructor<?> + add/remove por reflexão). NÃO RODOU AINDA.
- Fase 8 coerente: PRIME pixel (23,14), Ava NPC no mapa, ordem briefing→chefe→escolta.
- PRIME pixel confirmações: level2 WARBRINGER E91E63 existe, level6/8 OVERSEER 7986CB, level8 PRIME D01937 em (23,14).
NEXT: rodar ObjectivesVariadosTest (DISPLAY=:120); suíte regressiva; commit; push origin manus/objetivos-fase (remota existe, vazia); PR #32; mensagem final.
Comandos: cd /home/ubuntu/First-game-in-java && out=/tmp/test_obj && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/ObjectivesVariadosTest.java 2>&1 | grep -v Note | head -5 && DISPLAY=:120 timeout 60 java -cp $out:bin:res ObjectivesVariadosTest
