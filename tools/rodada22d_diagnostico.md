# Rodada 22d — Diagnóstico (escurecimento + inventário desalinhado)

## Sintomas reportados (prints do usuário)
1. **Print 1 (loja entre fases):** tela ao redor da loja escura/enxugada (ok em parte, pois é o overlay da loja), mas o usuário diz que ao SAIR da loja os mobs ficam invisíveis — persiste.
2. **Print 2 (in-game):** barra do inventário ("MediKit x1 | NanoMed x1 | Célula x5...") sobreposta com a HUD de recursos (VIDA/ESCUDO/MANA/BOOMERANGUE 168/260) no canto inferior esquerdo, e texto "I = inventario" colidindo com "BOOMERANGUE". Parece "travado" após pegar item.

## Causas identificadas no código
- **Sobreposição inventário × HUD compacta (UI.drawResourceHudScaled):**
  - UI desenha painel de recursos em `panelY = screenHeight - panelHeight - 6`, com `panelHeight = (4*LINE_SPACING+6)*h` (h=3, LINE_SPACING=16 → ~210px na base).
  - InventoryManager barra: `y = windowHeight - 46` (~818 de 864) e hint "I = inventario" em `y = windowHeight - 16`. A barra e a HUD competem pelo mesmo canto inferior esquerdo.
- **Escurecimento com a loja aberta:** questCompletedPending=true → `hidingHud=true` (HUD escondida, ok), MAS o overlay escuro vem do próprio painel da loja (fundos escuros são esperados). Persistência: `showLevelTransition > 0` desenha faixa escura central (190 alpha) durante toda a loja — pode ser o que o usuário chama de "permanece". A transição é concluída quando a loja fecha; ok. Porém o print mostra a faixa "Fase X concluída!" ativa durante a loja — é o comportamento intencional, mas o usuário acha escuro demais.
- **Mobs invisíveis após sair da loja:** a condição no update/render usa `questCompletedPending || isTransitionCooldown()` — quando a loja fecha, `advanceToNextLevel` é chamado, mas entre o close e o restart há frames onde pendência=false e cooldown foi setado; render do enemy já tem `isTransitioning()` que checa questCompletedPending||showLevelTransition>0||PhaseStatsScreen||cooldown. Possível: após close da loja, `Game.gameState` passa a NORMAL, questCompletedPending=false, MAS showLevelTransition=150+RESPIRO_FRAMES (300) e transitionAlpha=255 → inimigos ocultos por ~300 frames enquanto a HUD mostra jogo "vazio". O usuário clica/anda e depois "avança pra outra fase". Comportamento esperado da rodada 21, mas o fade total 255 + 5s de aviso torna muito longo/escuro.
- **Resumo dos fixes (proposta):**
  A. Reposicionar a barra do inventário e a HUD compacta para não colidirem (mover a barra do inventário para y ~ windowHeight-120 OU encurtar o painel HUD; mais simples: barra do inventário em y = windowHeight - 120 e hint ao lado).
  B. Encurtar o aviso de conclusão na loja (showLevelTransition ~120 durante loja) ou não mostrar a faixa enquanto a loja está aberta.
  C. Acelerar a revelação pós-loja: fade decaindo já rápido; manter aviso mais curto.
- **Status anterior (22c já aplicado):** fade decai sempre, inimigos congelados com isTransitioning.
- Teste já existente: Rodada22cTest (fade + freeze) — atualizar conforme novos fix.

## Arquivos-chave
- src/com/traduvertgames/graficos/UI.java (drawResourceHudScaled ~160, drawOverlayHint)
- src/com/traduvertgames/main/InventoryManager.java (render barra ~268-297)
- src/com/traduvertgames/main/Game.java (render: linha ~904 showLevelTransition faixa; update onObjectiveComplete)

## Progresso 22d (fixes aplicados e BUILD_DONE)
1. **InventoryManager.java render**: barra de itens movida para y = windowHeight - hudPanelHeight - 6 - 22 (logo acima do painel HUD compacta), hint "I = inventario" em y+2. Resolve sobreposição com VIDA/ESCUDO/MANA/ARMA.
2. **Game.java render**: faixa de conclusão "Fase X concluída!" agora só desenha com !ShopManager.isOpen() e alpha 120 (antes 190). Resolve "permanece escuro" com loja aberta.
3. **Game.java**: todos os showLevelTransition = 180+RESPIRO_FRAMES → 90+RESPIRO_FRAMES (linhas 1655/1686/1708/etc.). Transição pós-loja mais curta. Fade 255 decai 8/frame (~32 frames).
4. **TransitionCooldownTest**: atualizar esperados — showLevelTransition inicial pós advanceToNextLevel = 90+150=240 (teste espera >= 150 → ainda ok). O teste "aviso prolongado (>= 150)" passa.
5. **Rodada22cTest**: pode precisar ajuste (não dependia desses valores; ok).

## Pendências
- Rodar regressão completa: ObjectivesVariadosTest, ShopQaTest, MenuNavigationTest, PhaseTransitionTest, AutoValidate, StoryNpcPlacementTest, WaypointFrameTest, TransitionCooldownTest, MusicZoneTest, InventoryTest, BranchingNpcTest, Rodada22bTest, Rodada22cTest
- Eventualmente criar Rodada22dTest (verificar posições: painel HUD em panelY=732, barra inventário em y=836; sem overlap; faixa suprimida durante loja).
- Commitar+pushar (branch manus/rodada22-trilha-npcs-inventario), PR #36: https://github.com/mccartney0/First-game-in-java/pull/36
- Reportar ao usuário.

## Contexto técnico dos testes
- Padrão: javac -d $out -cp bin:res tools/X.java; DISPLAY=:120 timeout 60 java -cp $out:bin:res X
- UI.drawResourceHudScaled: painel y = screenHeight-(4*9+6)*3-6 = 732 (SCALE 4), altura 126.
- QuestCompletedPending pós-loja → advanceToNextLevel → fade 255 + aviso 240 frames + cooldown 150: mobs visíveis após fade zerar; HUD escondida durante aviso (esperado, respiro).
