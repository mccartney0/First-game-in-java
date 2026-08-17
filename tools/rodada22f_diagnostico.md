# Rodada 22f — Card "FASE CONCLUÍDA" minúsculo e Enter não responde

## Sintomas (print do usuário)
1. Card central azul escuro "FASE CONCLUÍDA" aparece minúsculo e ilegível (parece ~75x38px) sobre a fase nova (mobs e mapa já visíveis).
2. Apertar Enter/botões não fecha o card; só fecha ao abrir o menu e voltar.

## Causas identificadas

### Causa 1 — Card minúsculo (CONFIRMADA)
PhaseStatsScreen.render (linha ~110):
```
int unit = scale / 4;          // scale=4 → unit=1
int panelW = 300 * unit / 4;   // 300*1/4 = 75px
int panelH = 150 * unit / 4;   // 37px
```
O `unit/4` divide de novo: era para converter unidades de escala 4 para base, mas já divide 2 vezes. Correto seria `panelW = 300 * unit` (sem /4), ou seja 300px em qualquer escala. Fontes também: `16 * unit / 4` = 4px! O texto tem 4px de altura. Mesma classe de bug do HUD.

### Causa 2 — Enter não fecha o card (hipótese forte)
Game.update linha ~508: `if (PhaseStatsScreen.isShowing()) { PhaseStatsScreen.update(enter, escape); enter=false; escape=false; }` — isso deve consumir Enter. MAS: no print o jogo continua rodando (mobs ativos, mapa visível, HUD escondida) → gameState NORMAL. O enter é setado em keyPressed? Verificar:
- Linha 1154: `menu.enter = true` (Menu)
- Linha 1331, 1381: `this.enter = true` (Game.keyPressed?)
Confirmar se Game.keyPressed seta `enter=true` para o ENTER key; e se o Enter é consumido por OUTRO handler antes (ex.: MissionBanner/loja). Também verificar: o usuário "volta pro tutorial de início" — OnboardingManager pode REATIVAR ao restartGame? start() do construtor chama OnboardingManager.start()? Não: OnboardingManager.start é chamado em algum ponto. Se o card não fecha e o jogo prossegue... na verdade o card mostra ANTES de mostrar a fase (showLevelTransition=255). No print a fase já está visível e o card pequeno no centro → o usuário esperou o fade esmaecer com o card aberto. Então o card está aberto e Enter não o fecha.
Hipótese: keyPressed seta `enter=true` apenas quando... verificar linhas 1320-1390 do Game.java (keyPressed/KeyReleased).

## Correções planejadas
1. PhaseStatsScreen.render: remover o `/4` duplo — `panelW = 300 * unit`, `panelH = 160 * unit`, fontes `18*unit`, `14*unit` etc. (base: unidade 1 = 4px de mundo; manter proporção ~300px mínimo legível).
2. Investigar enter/escape flags: confirmar que keyPressed seta os flags em NORMAL e que o update consome; corrigir se algum caminho (ESC handler, inventário, shop) engole o Enter.
3. Teste novo: Rodada22fTest — render do card em probe (tamanho >= 300px), Enter fecha o card com gameState NORMAL, card não reabre sozinho.

## Contexto técnico
- Branch: manus/rodada22-trilha-npcs-inventario, PR #36.
- Build: `find src -name "*.java" | xargs javac -d bin -cp bin`; testes: `javac -d $out -cp bin:res tools/X.java` + `DISPLAY=:120 timeout 60 java -cp $out:bin:res X`.
- Fase concluída: QuestManager.onObjectiveComplete? não — avanço via `advanceToNextLevel` (linha ~1668): `prepareForLevel → restartGame → PhaseStatsScreen.show() → transitionAlpha=255 → showLevelTransition=240`.
- PhaseStatsScreen.show() em Game.java linhas 1678/1714/1754.
- Card update: `Game.update` linha ~508; `dismiss()` quando enter && framesElapsed>FADE_TOTAL.
- PhaseStatsScreen.java render em ~linha 103-135; FADE_TOTAL, capturedTimeMs, QuestTitle().
- Print mostra card minúsculo com borda ciano, fundo escuro azulado, título amarelo "FASE CONCLUÍDA", linhas "Tempo: ...", "Kills: ...", "Combo máximo: ...", "Setas para voltar à tela — ESC — continuar".

## Diagnóstico CONFIRMADO

### Causa do Enter ignorado (CONFIRMADA)
Game.keyPressed VK_ENTER (linha ~1323):
```java
if (e.getKeyCode() == KeyEvent.VK_ENTER) {
    if (DialogueManager.isActive()) { DialogueManager.advance(); }
    else if (InventoryManager.isOpen()) { InventoryManager.useSelected(); }
    else if (VictoryCutscene.isShowing()) { this.enter = true; }
}
```
Quando o PhaseStatsScreen está aberto (gameState NORMAL, não dialogo, não inventário, não cutscene), **nenhum branch levanta `this.enter`** — o flag nunca é setado e o `PhaseStatsScreen.update(enter, escape)` recebe enter=false para sempre. O card nunca fecha por Enter/ESC (ESC também não levanta escape em NORMAL nesse bloco — mas o usuário disse que "volta pro tutorial de início" — provavelmente por outro caminho ou pela pausa?).

Fix: adicionar `else { this.enter = true; }` para que o Enter chegue ao PhaseStatsScreen (que já consome o flag no update). O ESC em NORMAL abre pausa — aceitável? Melhor: deixar o ESC fechar o card também? A pausa abre a tela de pausa (que fica sobre o card, e de lá o usuário pode voltar). O usuário já relatou que abrir o menu resolve — então manter ESC→pausa, mas garantir que Enter fecha o card.

### Causa do card minúsculo (CONFIRMADA)
PhaseStatsScreen.render: `unit = scale/4` (scale=4→unit=1); `panelW = 300*unit/4 = 75px`; fontes `16*unit/4 = 4px`. O `/4` duplo esmaga tudo. Fix: remover `/4` final (mantendo `unit = scale/4` que converte px-base-4 para px reais... espera: 300px desejados → `panelW = 300 * unit * 4 / 4 = 300*unit`? Se unit=1, 300px. Sim: `panelW = 300 * unit`, fontes `16*unit`/`12*unit`.

Também verificar: a largura mínima com borda (drawRect x,y,w-1,h-1) com w=75 → quase nada. O card deve ter ~300px+ de largura.

## Risco do fix do Enter genérico (avaliado — SEGURO)
Em `Game.update`: o `this.enter` é consumido (`enter=false`) apenas nos blocos `PhaseStatsScreen.isShowing()` e `VictoryCutscene.isShowing()`. Quando nenhum overlay está ativo, o flag levantado é simplesmente descartado — não há nenhum consumidor em gameState NORMAL livre (a loja, level-up, inventário têm seus próprios caminhos). O VK_ENTER genérico não ativa nada indevido.

## Fix do render (aplicado)
Removido o `/4` duplo: panel 300x152 * unit (300px/152px em escala 4), fontes 16/12/10 * unit.

## Falta
1. Rodada22fTest (Enter fecha o card com gameState NORMAL; dimensões do card >= 300x150 em escala 4; card não reabre).
2. Rebuild + regressão completa + commit/push.

## Estado atual (pós-rebuild)
- Fix render APLICADO e funcionando: card 298x150px (drawRect w-1 → 298), centralizado, fonts 16/12/10*unit.
- Fix Enter genérico APLICADO no Game.keyPressed (linha ~1335): `this.enter = true` no else final.
- IMPORTANTE: sempre rebuild `find src -name "*.java" | xargs javac -d bin -cp bin` ANTES de rodar testes (bin/ fica desatualizado).
- Rodada22fTest (tools/Rodada22fTest.java): probe BufferedImage 1536x864, fadeIn forçado a 16 via reflection antes do probe.

## Falhas restantes no teste (a depurar)
1. "Card fecha após Enter durante gameState NORMAL" — FAIL. Fluxo: stateField.set(null,"NORMAL"); enterField.setBoolean(g,true); update.invoke(true,false). MAS PhaseStatsScreen.show() faz Menu.pause=true e gameState="MENU". O update: enter=true && framesElapsed>FADE_TOTAL(16) → dismiss() → gameState=Normal, Menu.pause=false, startTransitionCooldown(). MAS: framesElapsed começa 0 no show e o update incrementa ANTES da checagem — primeiro update: framesElapsed=1, não >16. No teste chamei update só 1x → enter não consumido! FIX do teste: forçar framesElapsed=17 via reflection OU chamar update 18x com enter=true a cada frame. ATENÇÃO: update consome enter no bloco PhaseStatsScreen.isShowing do Game.update — no teste chamo PhaseStatsScreen.update diretamente, que também consome? Não, update() do card não zera flags; ele só lê. Então basta forçar framesElapsed>=17 e chamar update(true,false) uma vez.
2. "Card não reabre sozinho" — decorre da falha 1 (card ainda aberto; g.update em NORMAL com showStats... verificar se g.update reabre o card: show é chamado em advanceToNextLevel (1678) — só em avanço de fase. Com card mostrando, o update intercepta em isShowing. Então após corrigir 1, este deve passar.
- Após correção: rebuild, regressão completa (14 suites), commit "fix(22f)", push, reportar.

## Contexto de fluxo (para referência)
- Fase concluída: QuestManager.onObjectiveComplete → questCompletedPending=true, loja abre (se não level8) OU advanceToNextLevel. Avance: prepareForLevel → restartGame → PhaseStatsScreen.show() (gameState=MENU, Menu.pause=true) → transitionAlpha=255, showLevelTransition=240.
- Game.update: if (PhaseStatsScreen.isShowing()) update(enter,escape) e consome enter/escape.
- dismiss(): showing=false, Menu.pause=false, gameState=NORMAL, startTransitionCooldown() (exceto arena).
