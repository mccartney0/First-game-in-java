# Notas da sessão — Rodada 29 (Metagame)

## Estado
- PRs #43 (JUnit) e #44 (GameState) MERGEADOS na main. Repositório sincronizado (main @860869c).
- 10/10 testes JUnit passando na main.
- PR #45 a ser aberto ao final.

## Branch
- Criar `manus/metagame-rodada-29` a partir de main.

## Plano técnico
### Nova classe: `src/com/traduvertgames/state/PilotUpgrades.java`
Melhorias permanentes (com custos/escalas):
| Melhoria | Efeito | Custo inicial | Escala | Máx níveis |
|---|---|---|---|---|
| CELLS (vida base) | +25 maxLife | 100 | +50 | 8 |
| REGEN (vida/s) | +1 vida/2s tick | 200 | +100 | 5 |
| SHIELD (escudo inicial) | +20% maxShield | 150 | +100 | 5 |
| AMMO (mana/munição inicial) | +15% maxMana | 120 | +100 | 5 |

Campos estáticos: `credits` (int), `Map<Upgrade, Integer> levels`, `Map<Upgrade, Integer> baseCosts`.
Métodos: `getLevel(Upgrade)`, `canAfford(Upgrade)`, `buy(Upgrade)` (aplica upgrade; soma custo ao créditos gasto), `apply(Player)` (aplica stats ao player: maxLife, regen tick, shield inicial, mana inicial), `resetSession()` (reset não-persistente; créditos persistem), `serialize()/deserialize()`, `applyToPlayer()`.

### Regeneração
Player.tick ou Game.update: aplicar `Player.life += regen` periodicamente (a cada ~1s). Verificar onde tick do player existe.

### Créditos
- Ganho: ao completar fase (questCompletedPending/advanceToNextLevel em Game.java ~600) +50; chefe kill +100; kills infinito (EnemyKillTracker/WaveManager) 1 crédito/kill, recorde profundidade +50.
- Armazenar no saves.json: raiz nível superior `credits` + `pilotUpgrades` {nívels}; sessão no slot. Persistir em saveCurrentGame e autosave (saveAutoSave linha 215).

### Pontos de integração (localizados)
- SaveManager.saveCurrentGame (linha 108): gravar `credits`, `pilotUpgrades` na raiz e session.
- SaveManager.loadSlot (linha 415+): restaurar e chamar `PilotUpgrades.applyToPlayer()` após `applyDifficultyToPlayerStats()` (linha ~Player.life=savedLife), pois difficulty define os bases.
- Game.resetPlayerToDefaults (1770), applyDifficultyToPlayerStats (1982), applyDifficultyScalingForCurrentLevel (1969): aplicar upgrades DEPOIS de difficulty.
- Game.advanceToNextLevel/questCompletedPending (~595-611): credit reward.
- GameState: NÃO adicionar upgrades (é estado persistente de conta, não de fase).

### UI
- Menu Pausa: opção nova "Upgrades do Piloto"? — MAIS SIMPLES: usar ShopManager (linha ~318 PAUSE_SAVE_GAME): adicionar seção/upgrades dentro do ShopManager? ShopManager é a loja de itens de fase. Alternativa limpa: nova tela `PilotUpgradesScreen` aberta via opção no menu principal ("Melhorias") e via menu pausa. Se complexo demais: adicionar apenas no menu principal.

### Validação
- `./gradlew test --rerun-tasks`, `./gradlew check`, playthrough: ganhar créditos, comprar upgrade, verificar stats, salvar/continuar, regressão de resets.

## Playthrough (ferramentas)
- Jogo: `cd /home/ubuntu/First-game-in-java && nohup bash -c 'DISPLAY=:0 java -cp bin:res com.traduvertgames.main.Game' > /tmp/game.log 2>&1 &`
- Driver: `/tmp/GameDriver.java` (compilado /tmp/GameDriver.class, cp /tmp) — teclas mapeadas: ENTER,ESC,UP,DOWN,LEFT,RIGHT,W,A,S,D,X,Q,E,T,P,I,F,L,SPACE,F5,F6,1,2,3.
- Window: `W=$(xdotool search --name "Game 2 RPG" | head -1)` (id 18874375).
- Menu principal: ENTER=seleção atual; setas navegam.
- Menu pausa: P abre; ESC fecha.
- Saves: saves.json no diretório do projeto.

## Integração confirmada (leitura de código)
- Game.registerEnemyKill() linha ~398: adicionar credits por kill (1 crédito).
- Game.update NORMAL linha ~699 (QuestManager.isObjectiveComplete → onObjectiveComplete → advanceToNextLevel): créditos por fase concluída (50) + save.
- grantCampaignReward() linha 1635: chamada no fim das fases 7 e 8 — adicionar +100 créditos chefe lá.
- Player.update() linha 115: adicionar regenTick no início (antes de handleJump) — só em estado NORMAL (verificar gameState via Game.gameState).
- applyToPlayer(): chamar APÓS applyDifficultyScalingForCurrentLevel (linha 2019) em Game.applyDifficultyToPlayerStats (linha 1982) — aplicar ao final do método.
- SaveManager.saveCurrentGame (linha ~200): root.put("metagame", PilotUpgrades.serialize()).
- SaveManager.loadSlot: após restoreObjectiveState/restoreNarrativeFlags, restaurar metagame e chamar applyToPlayer().
- Menu principal MAIN_OPTIONS: adicionar "melhorias do piloto"? Não — adicionar na tela de pausa (PAUSE_OPTIONS_LIST) item "upgrades"? Simples: opção no menu principal "melhorias" que abre tela própria PilotUpgradesScreen (nova classe) — com tecla 'M' também no estado MENU.
- HUD: menu principal mostra créditos no topo do menu (desenho em Menu.draw ~ver drawMenu).
- kill credits: WaveManager infinito? Para simplificar: créditos por kill em registerEnemyKill (só fase), e no modo infinito WaveManager — verificar waveManager kill callbacks depois; mínimo: kills de fase contam.

## Progresso da implementação Rodada 29 (atualizar)
### FEITO
1. `src/com/traduvertgames/state/PilotUpgrades.java` — CRIADO. Enums Upgrade (CELLS +25 maxLife/custo 100+50*nív máx8; REGEN +vida/s custo 200+100*nív máx5; SHIELD +20% inicial custo 150+100*nív máx5; AMMO +15% mana inicial custo 120+100*nív máx5). Métodos: getCredits/addCredits/spendCredits/resetCredits/getLevel/getNextCost/canAfford/buy/applyToPlayer/regenTick/serialize/deserialize/summary/labels/resetCredits.
2. `src/com/traduvertgames/graficos/PilotUpgradesScreen.java` — CRIADO. Tela do metagame com saldo, up/down/confirm/open/close/toggle/draw. Compra dá banner + som LEVELUP; sem créditos → banner ERRO.
3. Game.java: registerEnemyKill +1 crédito (linha ~408); onObjectiveComplete +50 créditos + save + banner (linha ~783); grantCampaignReward +100 créditos (linha ~1655); regenTick() no update NORMAL (linha ~700); applyToPlayer() no fim de applyDifficultyScaling (linha ~2072).
4. SaveManager.java: saveCurrentGame grava root.put("metagame", PilotUpgrades.serialize()) (linha ~210); restoreMetagame(root) método novo (linha ~406); loadSlot chama restoreMetagame após restoreDeepRecord (linha ~444); refreshMetagame() público (linha ~322).
5. Menu.java: renderMainMenu mostra "CREDITOS: N" em amarelo abaixo do título (linha ~552), com refreshMetagame().

### FALTA
- Menu.java: item "melhorias do piloto" no MAIN_OPTIONS (linha 33: array + OPTION_UPGRADES=6); getMainMenuLabel para o item; handleMainMenuSelection case; update(): tecla M no estado MAIN abre PilotUpgradesScreen; keyPressed do Game.java: tecla M (VK_M) no estado MENU abre a tela.
- PilotUpgradesScreen: fechar com ESC/back — o ESC do Menu deve delegar (se open, close). Verificar como Menu.update() trata tela — mais simples: adicionar Screen.UPGRADES no enum Screen + render case renderUpgradesScreen(g) + ESC fecha para MAIN.
- Game.update() ou Menu.update(): quando PilotUpgradesScreen.isOpen(), renderizar em cima (Game.render() desenha menu? o draw do canvas chama menu.render). Adicionar chamada PilotUpgradesScreen.draw(g) no Game.render após menu.render quando gameState MENU e tela aberta — OU dentro do Menu.renderMainMenu? Melhor: Game.render — ver onde render() desenha.
- Compilar tudo, ./gradlew check, playthrough, PR.

### Playthrough checklist Rodada 29
- Matar spiders na fase 1 → verificar créditos acumulando no menu
- Comprar Células Vitais → verificar maxLife aumenta (HUD vida máxima)
- Comprar Regeneração → vida sobe com o tempo
- Save T + Continuar → créditos/upgrades persistem
- Ver banner FASE CONCLUIDA +50 créditos (se concluir fase)

## Estado (pré-compactação)
### Implementação 99% feita:
- PilotUpgrades.java OK, PilotUpgradesScreen.java OK, Menu.java (item "melhorias do piloto" OPTION_UPGRADES=5, interceptação up/down/enter/escape quando open, draw após renderMainMenu, saldo CREDITOS no renderMainMenu), Game.java (kill +1, fase +50, chefe +100, regenTick, applyToPlayer em applyDifficultyScaling), SaveManager.java (metagame persistido/restaurado/refreshMetagame), compila ok.

### Testes: 11 novos em MetagamePersistenceTest.java (10 passam, 1 falha)
- upgradeCostsEscalatePerLevel() FAILED na linha 54. DEBUG XML mostra "DEBUG credits=0 cost=100" uma vez.
- SUSPEITA FORTE: Gradle test usa classes de main de build/classes/java/main recompiladas pelo compileJava, mas O PROBLEMA PODE SER que PilotUpgrades.buy com credits=0 cost=100 retorna false corretamente, e o teste espera true — ou seja, o @BeforeEach NÃO está zerando antes? Não. REAL: os créditos iniciais são 0 e o teste dá addCredits(100) ANTES do primeiro buy? NÃO! O teste: assertEquals(100, getNextCost) [line 50], print+buy [51] — FALTA addCredits(100) antes do primeiro buy! BUG NO TESTE (não no código). addCredits(100) foi esquecido na primeira compra.
- Fix: adicionar PilotUpgrades.addCredits(100) antes do primeiro buy no teste upgradeCostsEscalatePerLevel. Depois remover DEBUG prints.
- /tmp/CheckUpgrades.java criado (verificar com javac -cp build/classes/java/main /tmp/CheckUpgrades.java && java -cp /tmp:build/classes/java/main CheckUpgrades).

### Próximos passos
1. Corrigir teste (addCredits(100) antes do buy1; remover DEBUG), rerun: esperar 21/21.
2. Playthrough: iniciar jogo, matar spiders (ver créditos no menu), abrir "melhorias do piloto" (DOWN 5x → "melhorias do piloto", ENTER), comprar CELLS (cost 100), ver banner UPGRADE, salvar T, Continuar, validar persistência.
3. ./gradlew check, commit, push, gh pr create (base main, título "Rodada 29: Metagame — créditos persistentes e melhorias do piloto").
4. Playthrough driver: DISPLAY=:0 java -cp /tmp GameDriver (classpath /tmp, ações: click 640 400, tap TECLA, hold/TECLA wait MS). W=$(xdotool search --name "Game 2 RPG" | head -1).

## Playthrough Rodada 29 (atualização)
Menu inicial m0.png OK: "CREDITOS: 0" em amarelo + item "Melhorias do piloto" no menu principal.
Jogo iniciado: arena → Fase 1 (m2.png). Vida 100/120 no início, agora 88/120 (m4.png) — spiders atacam; 4 spiders visíveis + 2 no canto inferior direito. Mana 479/500.
Próximos passos playthrough: matar mais spiders com X (tiros na direção do movimento), voltar ao menu (ESC/P) e verificar CREDITOS: N > 0 (1 crédito por kill). Depois: abrir "Melhorias do piloto" (DOWN 5x → ENTER), comprar CELULAS VITAIS, validar banner. Depois salvar T, Continuar, validar persistência.
GameDriver: DISPLAY=:0 java -cp /tmp GameDriver [comandos]. W=$(xdotool search --name "Game 2 RPG" | head -1) = 18874375. Screenshot: DISPLAY=:0 import -window $W /tmp/mN.png.
PID jogo: pgrep -f com.traduvertgames.main.Game. Log: /tmp/game.log (stdout pode não ir pro log — jogo GUI).

### Observação playthrough
m5.png: menu principal aberto de novo (CREDITOS: 0). O jogo provavelmente morreu entre m4 (88/120) e m5 — os spiders atacaram enquanto eu atirava. CREDITOS: 0 porque não salvei kill? Não — o kill crédito fica no GameState (memória), não no disco até salvar/refreshMetagame. O menu renderMainMenu chama refreshMetagame() que lê do disco! O refreshMetagame lê do saves.json — sem save, mostra 0 mesmo com créditos na memória. BUG DE UI: refreshMetagame no renderMainMenu deve refletir estado em memória? Verificar: o menu principal mostra o saldo só após salvar. Preciso salvar com T durante o jogo para ver os créditos no menu. Ajustar estratégia: no próximo jogo, matar spiders → T → ESC → ver créditos. Nota: também pode ser feature (persistência). DECISÃO: fazer playthrough: matar spiders, T, morrer/sair, ver créditos no menu.

### ACHADO CRÍTICO
saves.json metagame: credits=0 após matar spiders (HUD vida caiu 100→52, spiders atingidos). VERIFICAR: onde adicionei addCredits(1) no registerEnemyKill — pode ter ido para Game.registerEnemyKill (static) ou no registro errado. Grep 'addCredits' no src. Se kill não deu crédito, revisar. Também: save T salvou corretamente (vida 57 no slot). Mas killsThisLevel=0 no slot?? inimigosMortos=0 — talvez eu não tenha matado NENHUM spider ainda (só dano). Confirmar: matar spider visivelmente (animação de morte) e checar créditos de novo.

### Registro 2
registerEnemyKill está correto (linha 408, chamado em Enemy.java:1119 na morte). inimigosMortos=0 no slot = eu nunca matei um spider (só causei dano). Continua playthrough: matar spiders de fato (ficar perto + atirar até sumirem), ver vida, depois T + ESC + menu (CREDITOS deve > 0).

### Registro 3
m9.png: menu principal reaberto (CREDITOS: 0, "Carregar jogo" disponível = save existe). O menu abre sozinho? Provavelmente o jogo morreu e o auto-restart levou ao menu (ou ESC involuntário do driver?). Créditos 0 MESMO COM SAVE. Hipóteses: (a) nenhum kill de fato; (b) addCredits incrementa na memória mas o refreshMetagame/saveCurrentGame não inclui o valor atual? — saveCurrentGame grava metagame no writeRoot OK (chave "metagame" presente). Logo (a) é provável: não matei spiders, ou o registerEnemyKill não é chamado. VERIFICAR Enemy.java:1119 contexto. Também: menu reabriu sozinho — verificar se morte chama returnToMainMenu.

### Registro 4 (investigação)
Enemy.destroySelf chama registerEnemyKill corretamente. Mana caiu 500→455 (tiros disparam), mas vida dos spiders alta — provavelmente preciso de MUITOS tiros (spiders ~40-60 vida? dano do BLASTER baixo). E o menu reabriu sozinho duas vezes (m5, m9) — ver linha 734 do Game.java (returnToMainMenu) — é no update NORMAL? Se o player morreu durante os comandos, o menu de game over apareceu e... fechou sozinho (auto-restart?) Não: auto-restart não abre menu. O menu aberto nas capturas: o auto-restart da fase após morte chama restartCurrentPhaseWithoutSave — na fase 1 sem save, o fluxo GAMEOVER mostra menu por 10s e depois "Voltar ao menu"? Ver o menu de game over e o fluxo. Alternativa: o comando do driver 'wait' longo fez o driver não enviar mais nada e o jogo morreu → menu GAMEOVER → depois ESC automático? Não existe ESC automático. SIMPLES: na morte o menu GAMEOVER aparece; após ~10s o AutoStart/continua? Não importa — DECISÃO: para validar créditos, NÃO depender de menu: escrever teste Java que chama addCredits e lê getCredits (teste unitário ok já passa). Para validar playthrough: matar spiders DE FATO (confirmar +XP amarelo) e então ver menu com créditos > 0.

### Registro 5 (conclusão do mistério do menu)
menuReturnTimer > 0 retorna ao menu após game over — comportamento esperado (auto-retorno ao menu após morrer). Os créditos ficam em memória no PilotUpgrades e são persistidos apenas no T/save. Sequência correta: matar spiders → T → menu mostra CREDITOS > 0 (refreshMetagame lê saves.json no renderMainMenu). Playthrough continua.

### Registro 6 (estado atual)
Save carregado OK: m10.png mostra Fase 1 com vida 55/120 (save restaurado), mana 455/500, munição 232/250. Spiders visíveis: (~500,490), (625,675), (720,675), (1000,800 canto). Player em (~590,570) centro. HUD normal "Missão: Contacto com o Comando".
Próximo: atirar nos spiders até matarem (confirmar +XP amarelo), T, morrer/sair, menu mostra CREDITOS > 0, abrir "Melhorias do piloto" (DOWN 5x ENTER no menu), comprar CELULAS VITAIS, banner UPGRADE, validar stats (vida max 145?), salvar T, Continuar, validar persistência.
Comandos: DISPLAY=:0 java -cp /tmp GameDriver [cmds]; screenshot: DISPLAY=:0 import -window $W /tmp/mN.png; W=18874375.

### Registro 7
m11: munição 227/250 (tiros dispararam) mas spiders ainda vivos (3 visíveis, 55/120 vida, sem +XP). Spiders parecem ter MUITA vida (~80-120?) ou o dano é 1. Continuar rajadas longas em cada spider OU aceitar: para o playthrough, posso validar créditos via modo infinito? Não. Estratégia: rajadas longas de 20 tiros em cada spider, verificar mana/munição caindo, buscar o +XP.
