# Rodada 15 — planejamento (branch manus/rodada15, a partir de main 35cc0a8)

## Pedidos do usuário
1. VERIFICAR menu: sair/salvar/carregar (fluxos quebrados no passado)
2. VERIFICAR sons (pediu "sons coerentes")
3. FAZER balanceamento de dificuldade (fase 1 difícil, curva suave)
4. FAZER sons coerentes do jogo
5. FAZER navegação A/D + setas no menu de upgrades (level up)

## Diagnóstico — Menu (Menu.java main atual)
- SAIR: main menu "sair" → EXIT_CONFIRM (Não/Sim, ESC NÃO fecha — ver tecla de escape!); Sim → System.exit(0). PAUSE "sair do jogo" → mesma tela. ESC no EXIT_CONFIRM? update() só responde a up/down/enter. **BUG: ESC não funciona no EXIT_CONFIRM nem em telas de menu** (o usuário disse "quando aperto ESC o jogo deveria sair do menu"?) — adicionar escape que fecha telas (voltar ao anterior; no MAIN com pause fecha a pausa).
- SALVAR: PAUSE "salvar jogo" → SaveManager.saveCurrentGame() + closePauseScreen. OK.
- CARREGAR: main "continuar" (fora de pausa) → loadSlot(activeSlot). main "carregar jogo" → tela LOAD com 3 slots + Voltar. OK, gameState=MENU→NORMAL feito no loadSlot.
- FALTA: navegação por ESC nas telas do menu (voltar); A/D nas opções.

## Sons (SoundManager)
- Ver: pools de clips, eventos existentes. Planejar: tiro, hit, morte, level-up, conclusão de fase, loja aberta, diálogo, click menu — gerar WAV sintéticos proceduralmente (AudioSystem, sem assets externos) para evitar problemas de classpath.

## Balanceamento
- PhaseConfig / Enemy spawn / dano inicial: reduzir dano de spawn inicial da fase 1, +HP inicial do player (verificar applyDifficultyToPlayerStats, WaveManager, DETECTION de mobs perto do spawn).

## LevelUp menu (LevelUpManager.java)
- Verificar como escolhe upgrade: só setas? Adicionar A (esquerda) e D (direita) + números 1-3 para seleção direta.

## Estado da implementação (atualização contínua)

### 2. Fluxos de menu — FEITO (Menu.java editado)
- Adicionados campos `left`, `right`, `escape` no Menu
- Menu.update(): left/right movem seleção (A/D + setas esquerda/direita); escape chama escapeFromCurrentScreen()
- escapeFromCurrentScreen(): PAUSE→volta ao jogo; OPTIONS/LOAD/HOW_TO_PLAY/EXIT_CONFIRM→voltam ao nível anterior; MAIN com pause→fecha pausa
- FALTA: wire as teclas no Game.java keyPressed: no bloco "MENU".equals(gameState) precisa enviar menu.left/right/escape (agora: VK_UP/DOWN enviam menu.up/down nos 4 estados MENU/SHOP/LEVELUP/LEVELSELECT)
- ESC no Game.java linha ~980: para gameState MENU sem pause não faz nada (só GAMEOVER/LVLSEL/LEVELUP/SHOP); adicionar: else if ("MENU".equals(gameState)) { menu.escape = true; }
- A/D no MENU: adicionar menu.left=true/menu.right=true nas teclas VK_LEFT/VK_A e VK_RIGHT/VK_D quando gameState==MENU
- CUIDADO: player.left/right não pode interferir (MENU não usa player.update? verificar) — ok, gameState MENU pula lógica de jogo.

### LevelUpManager update(): navegação vertical só; cards são HORIZONTAIS (3 cards lado a lado)
- navigateUp/navigateDown mudam choiceIndex (W/S ou ↑/↓)
- NOVO: navigateLeft/navigateRight (A/D, ←/→) mapear para choiceIndex-1/+1 no Game.java quando LEVELUP
- Seleção direta por teclas 1/2/3 no level up: Game tecla VK_1-3 quando LEVELUP → LevelUpManager.confirmChoice(index) — usar setChoiceIndex antes.

### 3/4. Sons
- SoundManager.java: tem pools de clips (pools, poolIdx), Event enum com LEVELUP, play(Event), unload, toggle
- Ver eventos existentes antes de criar: SHOOT, HIT, DEATH, LEVEL_COMPLETE, SHOP_OPEN, DIALOGUE, CLICK, MENU_NAV, VICTORY
- Gerar WAV sintéticos via AudioSystem (sem arquivos externos):
  - SHOOT: oscilador curto 400→150Hz envelope rápido
  - HIT: ruído branco curto 60ms
  - DEATH: tom descendente 300→80Hz 0.6s
  - LEVELUP/LEVEL_COMPLETE: arpeggio ascendente
  - SHOP_OPEN: tom suave
  - VICTORY: acorde ascendente
- Implementação: classe sonora procedural com Clip criado via SourceDataLine ou ByteArrayInputStream com WAV header手工.

### Balanceamento (fase 4)
- Verificar Player.applyDifficultyToPlayerStats e onde vida inicial é definida (restartGame/resetPlayerToDefaults?)
- Fase 1: menos spawns iniciais, mobs mais lentos perto do spawn (spawnRadius mínimo maior), +vida inicial

### Validação pós-implementação
- Build: javac -d bin -cp bin $(find src -name "*.java")
- Testes existentes: AutoValidate, StoryNpcPlacementTest, BannerHintTest, LevelSelectLogicTest, PhaseTransitionTest (rodar com DISPLAY=:120, cp bin:res)
- Criar MenuNavigationTest (nova): ESC/A/D nas telas do menu + level up com A/D
- Commit/push PR: branch manus/rodada15 → PR novo (gh pr create), comentar via arquivo

## Progresso (atualizado continuamente)

### 2. Menu — FEITO
- Menu.java: campos left/right/escape; moveSelection com som MENU_SELECT; escapeFromCurrentScreen() (PAUSE→volta jogo; OPTIONS/LOAD/HOW_TO_PLAY/EXIT_CONFIRM→nível anterior; MAIN+pause→fecha pausa);
- Game.java keyPressed: MENU → menu.left/right (A/D e setas) e menu.escape=true+return (topo do bloco ESC, depois do cooldown da loja); LEVELUP → LevelUpManager.selectAndConfirm(0/1/2) nas teclas 1/2/3.
- LevelUpManager.java: selectAndConfirm(index), navigateLeft/Right; hint atualizado.
- BUILD_OK.

### 3. Sons — FEITO
- make_sounds.py estendido: level_complete.wav (0.45s fanfarra 5 notas), victory.wav (1.2s fanfarra+acorde G), dialogue_start.wav (0.14s duplo blip), purchase.wav (0.17s coin 3 notas), menu_select.wav (0.05s blip) — gerados em res/sounds e bin/sounds.
- SoundManager.Event: LEVEL_COMPLETE, VICTORY, DIALOGUE_START, PURCHASE, MENU_SELECT adicionados ao FILES map.
- Hooks: Game.onObjectiveComplete → LEVEL_COMPLETE; VictoryCutscene.start → VICTORY; DialogueManager.startNearestDialogue → DIALOGUE_START; ShopManager.purchase() (após check score) → PURCHASE; Menu.moveSelection → MENU_SELECT.
- BUILD_OK.

### 4. Balanceamento — FEITO (rodada 15)
- Game.applyDifficultyScalingForCurrentLevel: baseMaxLife = 120 + (level-1)*8 na campanha (120 fase1 → 176 fase8); chefes e modo infinito intactos.
- Enemy.spawnRandomVariant aplica scaleForPhase(enemy, getCurrentLevel): fase1 0.55, fase2 0.72, fase3 0.85, fase4+ sem redução; chefes isBoss() mantêm escala própria.
- scaleForPhase: life capada em maxLife*factor; lifeBoost negativo via Math.min(0,...); damageBoost = min(1, factor). applyDamage já consome lifeBoost>0 primeiro (negativo não interfere); getLifePercentage ok (cap=maxLife).
- BUILD_OK. isBoss() já existia — removi duplicado.

### 5. LevelUp A/D + 1/2/3 — FEITO (feito junto com o menu)
- Captura visual /tmp/r15_menu.png: menu principal renderiza OK sobre o jogo; itens "Novo jogo, Continuar, Carregar jogo (indisponível), Como jogar, Opções, Sair" com marcador >. Carregar indisponível porque não há save (correto). MenuNavigationTest 12/12 (com Spritesheet real mockada via Game.spritesheet field). Suíte lógica 10/10.
- Game.java: MENU → menu.left/right (A/D/setas); ESC no MENU → menu.escape=true+return; 1/2/3 → LevelUpManager.selectAndConfirm(0/1/2) (fora do bloco ENTER).
- Menu.java: left/right/escape; moveSelection com MENU_SELECT; escapeFromCurrentScreen.
- LevelUpManager: navigateLeft/Right (setChoiceIndex ±1), selectAndConfirm(index), hint atualizado.
- BUILD_OK.
- MenuNavigationTest criado em tools/ (sem AWT, pura reflexão). Suíte lógica 9/9 passando + MenuNavigationTest.
- FALHAS do teste a corrigir: (a) navigateLeft 0→0 travou? NÃO — na verdade falhou porque depois do setChoiceIndex(choiceIndex-1) com choiceIndex=0, setChoiceIndex faz (0-1+CHOICES)%CHOICES=2 (wraparound), então o clamp não existe! FIX: navigateLeft/Right não devem wrapar — usar max/min; (b) selectAndConfirm(1): choiceIndex virou 2 pelo wrap e depois 2→1? Não — selectAndConfirm chama applyChoice(1) que fecha o level-up (showingLevelUp=false) e choiceIndex pode ser resetado a 0 no reset() chamado por applyChoice. FIX teste: verificar via outro campo (ex.: levelapplied) ou não chamar selectAndConfirm com showing=true+choiceIndex=2 antes. Realmente aplicar: choiceIndex ficou 2 (do teste anterior) → selectAndConfirm(1) aplica 1 → reset → choiceIndex=0. Então o esperado é 0 (reset). Corrigir teste.
- FIX código (a): navigateLeft: setChoiceIndex(choiceIndex - 1) → choiceIndex = Math.max(0, choiceIndex-1); navigateRight: choiceIndex = Math.min(CHOICES-1, choiceIndex+1).
- Player.life/maxLife = 100 estático; applyDifficultyScalingForCurrentLevel define baseMaxLife=100 baseMana=500 baseShield=150 (linha ~1523); CUR_LEVEL>=7 → bônus final stretch.
- applyDifficultyScaling(baseMaxLife..) → scaledMaxLife via OptionsConfig.getLifeMultiplier (dificuldade!). 
- FALTA: ver applyDifficultyScaling completo; aplicar bônus inicial da fase 1 (ex.: baseMaxLife 120 na fase 1, bônus crescente; OU via getLifeMultiplier); ver WaveManager.spawn — spawnDensity por fase; verificar PHANTOM não aparece na fase 1 (já feito antes? verificar).
- Plano balanceamento: (a) fase 1: +20 vida base (baseMaxLife 120), spawn inicial reduzido (WaveManager wave 1 menos inimigos); (b) dano inicial de mobs menor em nível fácil.

### 5. LevelUp A/D — FEITO (navegateLeft/Right + 1/2/3)

### 6. Validação
- Suíte: AutoValidate, StoryNpcPlacementTest, BannerHintTest, LevelSelectLogicTest, PhaseTransitionTest — rodar com DISPLAY=:120, cp bin:res.
- Criar tools/MenuNavigationTest.java: testar ESC/left/right/1/2/3 via Menu.update() e LevelUpManager (sem Game? Menu.update depende de saveExists; MenuNpcTest anterior? usar MenuLogicTest existente em tools/MenuLogicTest.java — verificar o que ele testa antes)
- Commit/push + gh pr create --base main --title "Rodada 15: ..."; comentar.
- Comandos: javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep -i error | head -3; echo BUILD_OK


---

# RODADA 16 — QA DA LOJA (branch manus/qa-loja)

## Estado do merge (rodada 16, fase 1)
- PR #29 merged na main (commit 516952e, --no-ff); branch remota manus/rodada15 deletada; gh pr view confirmou merge.
- Branch manus/qa-loja criada a partir da main; BUILD_OK.

## Diagnóstico do fluxo da loja (fase 2)
Problemas históricos relatados pelo usuário na loja:
1. Não subia/descia itens — navegar com W/S (ok agora) mas A/D não funcionavam; comprar não fechava a loja corretamente; ESC piscava (key-repeat reabria menu de pausa).
2. Menu de vida aparecia atrás da loja com aparência diferente (overlay da HUD em gameState SHOP + Menu.renderPauseScreen + ShopManager.render juntos = dupla renderização).
3. Compra bem-sucedida fecha a loja (close() → gameState=NORMAL + escCooldown=15) — mas se a HUD renderiza em NORMAL, aparece "flash" da HUD antes do jogo retomar.
4. Feedback "Comprado!" — após purchaseSucceeded a loja fecha (linha 228), então o feedback some com ela.

Código atual (conferido):
- ShopManager.java (335 linhas): open/close/navigateUp/Down/purchaseSelected/update/render; escCooldown 15; SHOP evento no open e close; PURCHASE no purchase().
- Game.java 599/645: ShopManager.open() em dois pontos (onObjectiveComplete e outro); 621: update pula se questCompletedPending||ShopManager.isOpen(); 721 e 788: ShopManager.render(overlayG) em dois pontos (NORMAL e SHOP! — render duplicado no gameState NORMAL linha 788!); 914/931: navigate; 976-977: purchaseSelected no ENTER; 1004-1005: ESC cooldown; 1028-1030: ESC fecha loja.
- gameState SHOP update (559): ShopManager.update(); ParticleSystem.update() — jogo congelado (menu de pausa é renderizado em 788? ver: linha ~786: else if ("SHOP") → Menu.renderPauseScreen + ShopManager.render).
- Menu.renderPauseScreen no SHOP desenha o menu de pausa atrás (bug: overlay duplicado, item 2).
- Render duplicado: 788 (linha do else if gameState) e 721.

## Plano de implementação (fase 3)
1. Remover render duplicado: ShopManager.render apenas no gameState SHOP (não no NORMAL 721); no SHOP não renderizar Menu.renderPauseScreen atrás (sobreposição), manter só ShopManager.render + HUD compacta (desenhada separada? conferir como HUD compacta é renderizada: MissionHud.render no overlay — durante SHOP pode ocultar HUD).
2. A/D na loja: adicionar ShopManager.navigateLeft/Right semântica? A loja é vertical (lista). A/D pode pular 5 itens (PageUp/Down) OU simplesmente subir/descer. Melhor: A/D sobe/desce também (paridade com setas).
3. ESC com cooldown correto: já existe (15 frames). Verificar que menu.escape não é disparado no SHOP (ESC handler: linha 1028 fecha loja e return? — precisa garantir que não cai no bloco MENU).
4. Após compra bem-sucedida: manter loja aberta com feedback "Vida recuperada!" por ~1.5s e depois fechar? Usuário reclamou que "permanece aberto" — comportamento atual fecha. Manter fechamento, mas sem mostrar HUD atrás: transição limpa.
5. Comprar vários itens seguidos: feedbackTimer 90 frames; compra fecha a loja — o usuário não consegue comprar 2 itens numa visita. MELHOR: não fechar após compra; manter aberta para compras múltiplas, fechar só com ESC/Enter? Decisão: manter aberta após compra (feedback 90 frames), fechar apenas via ESC — usuário pode continuar comprando.
6. Tecla Enter sem compra: navegar e confirmar — ok.
7. Teste QA: criar ShopQaTest.java (estático, sem AWT): navegação wrap, compra insuficiente, compra bem-sucedida (mock Player/Companion via reflexão) e render não duplicado.
8. Verificação visual headless: DISPLAY=:120 com script que entra na loja e navega/compra.

## Implementações feitas (fase 3)
- Game.java render: ShopManager.render removido do bloco NORMAL (render duplicado) e removido Menu.renderPauseScreen do bloco SHOP. Loja renderiza 1x.
- ShopManager.java: navigateA()/navigateD(); compra bem-sucedida NÃO fecha mais a loja (closeOnNextEnter=true); confirmOrPurchase(): Enter fecha após compra ou compra novo item.
- Game.java ENTER da loja: ShopManager.confirmOrPurchase(); A/D no SHOP: navigateA/D.
- Hint da loja atualizado.
- Verificado fluxo: onObjectiveComplete→open (prioridade level-up via shopPendingOpened); update NORMAL consome questCompletedPending ao fechar (advanceToNextLevel) — ok com loja persistente.

## Validação pendente (fase 4)
- Suíte: AutoValidate, StoryNpcPlacementTest, BannerHintTest, LevelSelectLogicTest, PhaseTransitionTest, CompanionSaveRestoreTest, SaveLoadLogicTest, QuestLogicTest, ShopSkinLogicTest, MenuNavigationTest + ShopQaTest.
- Commit/push + gh pr create --base main --head manus/qa-loja.


## Debug ShopQaTest (em andamento)
- PROBLEMA: probe com Class.forName(..., false) + setAccessible falha em get() com IllegalAccessException (Reflection.newIllegalAccessException) — getDeclaredField de classe NÃO inicializada NÃO permite setAccessible em alguns módulos? Na verdade o teste ShopQaTest rodou parcialmente (10 passaram). O probe simples falhou no primeiro get (linha 20: "aberta=..." — sm.getDeclaredField("open").get(null) falhou).
- NO ShopQaTest: as verificações de compra falham: score não muda (9200 esperado, continua 10000), life não muda, closeOnNextEnter=false.
- DIAGNÓSTICO PROVÁVEL: purchase() roda mas Game.addScore(-800) com score=10000 → 9200 deveria funcionar (addScore usa score+=delta com Math.max(0,...)). MAS se score field do teste aponta para field de classe não-inicializada com setAccessible, o .set() pode funcionar mas o método addScore lê o field REAL da classe inicializada (a JVM tem apenas 1 field para a classe — o Class<?> do forName(false) é o MESMO objeto Class que o inicializado). Então set funciona.
- VERDADEIRO BUG PROVÁVEL: purchase() chama SoundManager.play(PURCHASE) → SoundManager estático inicializa clipes; se falhar (ex.: exceção engolida no play), execução continua. OU purchase chama Companion.setSkin/etc — não no CURAR.
- SUSPEITA FORTE: feedback "Pontuação insuficiente!" — Game.getScore() vs score field: getScore() retorna score; mas purchase() usa Game.getScore() < price — se getScore() lê o field score da classe inicializada e eu setei via field da mesma classe, ok.
- CHECAR: o bloco "compra sem pontuação não fecha" PASSA — porque purchase() com score=10 seta feedback "Pontuação insuficiente!" e NÃO fecha (return). Depois setei score=10000 e compra deveria passar. Mas life não mudou → purchase NÃO entrou no switch? Ou purchase não foi chamada (confirmOrPurchase: closeOnNextEnter=false → purchaseSelected). 
- PRÓXIMO PASSO: adicionar try/catch no teste para ver a exceção real da invoke (pode ser ExceptionInInitializerError de SoundManager engolida?). Rodar probe com try/catch e print do stacktrace completo.
- NOTA: na 1ª rodagem do teste (13 passaram) a compra FUNCIONOU (life 160 passou, score 9200 passou) — depois da minha edição (maxLife=200) passou a falhar. A edição não deve afetar. Diferença: na 1ª rodagem, "segunda compra debita 600" falhou. Na 2ª e 3ª, TODAS as compras falham.
- POSSIBILIDADE REAL: ShopManager.purchase: linha `if (Game.getScore() < item.price)` — MAS existe Game.setScore? Se algum código anterior setou score a 0? Não.
- OUTRA: selection mudou (navigateDown → 1 = ESCUDO) ok. A seleção pode ter wrapado para 13 (compra não existe → default: purchaseSucceeded=false → fecha?? não, feedback). Hmm "segunda compra mantém a loja aberta" PASSOU → purchase não fechou → ou purchaseSucceeded=false (item inválido?) ou feedback insuficiente.
- DECISÃO: capturar Throwable da invoke e printar.
- Commit pendente da rodada 16: Game.java (render da loja único), ShopManager.java (navigateA/D, closeOnNextEnter, confirmOrPurchase), Game.java (ENTER→confirmOrPurchase, A/D SHOP), hint loja; tools/ShopQaTest.java novo. Branch manus/qa-loja, ainda não pushada.
