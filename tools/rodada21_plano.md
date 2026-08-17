# Rodada 21 — plano (executar após merge do PR #34)

## Estado atual
PR #34 mergeado na main (commit 9fda78b). Branch atual: `manus/rodada21-polimento-transicao` (PR a criar — body básico já enviado, mas falhou por ausência de commits; recriar após primeiro commit com `gh pr create --title "Rodada 21 — Transição de fase calmante + polimento" --body ...`).

## Bug reportado pelo usuário (screenshot rodada 21)
Depois de concluir a fase (diálogo com NPC), o jogo exibe "Fase 2 concluída! / Próxima fase: ... / Missão: ..." mas tudo acontece rápido demais: o aviso some logo e os mobs já atacam. Também aparecem DUAS mensagens sobrepostas ("Fase 2 concluída!" e o banner de lore da nova fase ao mesmo tempo) — na screenshot o menu de pausa aparece desenhado por trás da mensagem de transição.

## Diagnóstico
1. `advanceToNextLevel()` chama `PhaseStatsScreen.show()` que seta `gameState = "MENU"`, MAS `PhaseStatsScreen` tem `AUTO_DISMISS_FRAMES = 280` e `dismiss()` restaura `gameState = "NORMAL"` → o jogo volta ao combate sozinho.
2. Na screenshot, o "menu principal" aparece desenhado atrás da mensagem de transição: quando `gameState = "MENU"` e `showLevelTransition > 0`, o render desenha o menu (linha 836-839) AO MESMO TEMPO que o aviso — sobreposição feia.
3. A nova fase carrega com inimigos já nascendo; não há "respiro" inicial (o WaveManager começa a spawar imediatamente).

## Correções rodadas 21
1. **Respiro seguro pós-transição (`RESPIRO_FRAMES`)**: após fechar a tela de stats, manter inimigos congelados (pausados, como no onboarding) por ~2,5s (~150 frames) até o jogador ter tempo de se orientar. Nova flag `Game.transitionCooldown > 0` que pausa Enemy.update e WaveManager spawns. O aviso "Fase X concluída!" permanece visível durante o cooldown (não decresce durante cooldown).
2. **Sem auto-dismiss em `PhaseStatsScreen` durante a campanha**: só fecha por Enter (manter auto-dismiss para modo sobrevivência infinito, onde o jogador não pode travar). Na campanha, fechar por Enter → inicia o cooldown de respiro.
3. **Não desenhar o menu de pausa sobre o aviso de transição**: no render, quando `showLevelTransition > 0` e `gameState == "MENU"` (stats screen), não renderizar o menu (o card de stats já está visível por cima).
4. **Fade preto cobre tudo na transição**: garantir `transitionAlpha = 150` persista durante o cooldown (não decair enquanto cooldown > 0).
5. **Evitar sobreposição de banners**: MissionBanner.showComplete (150 frames) não deve ser cancelado pelo banner de lore que `advanceToNextLevel` dispara (360 frames) — adiar o banner de lore em ~1,5s após o cooldown (ou mostrar lore depois que o usuário apertar Enter).
6. **Build + suíte completa + teste novo `TransitionCooldownTest` (render simulado: inimigos congelados por N frames após transição, cooldown decrementa)** + commit + push + PR.

## Comandos
- Build: `cd /home/ubuntu/First-game-in-java && javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep error | head -3; echo BUILD_OK`
- Teste: `out=/tmp/test_X && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/X.java 2>/dev/null && DISPLAY=:120 timeout 90 java -cp $out:bin:res X 2>&1 | tail -1`
- Suíte: ObjectivesVariadosTest ShopQaTest MenuNavigationTest PhaseTransitionTest AutoValidate StoryNpcPlacementTest (todos em tools/)

## Backlog futuro (rodadas 22+)
- Balanceamento fino chefes fases 7/8
- Mais skins/companions com preview na loja
- Missões secundárias e diálogos extras
- Pausa por ESC durante combate (pausa real, hoje ESC só fecha telas)
