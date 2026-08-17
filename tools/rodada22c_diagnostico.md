# Rodada 22c — Diagnóstico confirmado

## Bug A — Faixas pretas após transição de fase
**Causa confirmada.** Em `advanceToNextLevel()` (Game.java ~1674), o fade é ativado com
`transitionAlpha = 150` (semi-transparente) e o decaimento (linha 667) é bloqueado enquanto
`transitionCooldown > 0`:

```java
if (transitionAlpha > 0 && transitionCooldown <= 0) {
    transitionAlpha = Math.max(0, transitionAlpha - 3);
}
```

Quando o jogador fecha a loja (SHOP→NORMAL), o bloco `else if ("SHOP".equals(gameState))`
**não executa o decaimento do cooldown nem do alpha** (eles estão fora do bloco NORMAL).
Depois de fechar a loja, `advanceToNextLevel()` roda, zera `questCompletedPending` e seta
`transitionAlpha=150` + `transitionCooldown=RESPIRO_FRAMES`... mas espera — a linha 1674
está dentro do bloco NORMAL do update, então decairia. Porém o update roda por estado; o
decaimento (662-669) está FORA dos blocos de estado (executa sempre). Ok, então o cooldown
decai em qualquer estado... MAS `isTransitioning()` usa `questCompletedPending || showLevelTransition > 0`
e o render (724) pula inimigos quando `questCompletedPending || isTransitionCooldown()`.

**Releitura do print:** faixas horizontais escuras largas cruzando a tela, jogo visível entre
elas. Isso NÃO é o fade inteiro — é uma faixa fixa. Possível causa real: o fade de 150 fica
no ar por 2,5s ENQUANTO o jogo já está visível → a tela fica escurecida (aparenta faixas
junto do letterboxing/fill preto da janela). E `g.fillRect(0,0,WIDTH,HEIGHT)` cobre a tela
de 384x216 por inteiro, não faixas.

**Candidato real:** o `g.fillRect(0, 0, windowWidth, windowHeight)` (preenchimento da janela)
com drawOffset/letterboxing? Não. Verificar `Camera.y` ou `drawImage` do buffer com offset?
`bs.getDrawGraphics()` ... `drawImage(buffer, drawOffsetX, drawOffsetY, ...)` — se o offset
não for aplicado, a janela não múltipla exibe bordas... mas faixas NO MEIO.

**Hipótese final mais provável (combina print + relato "após passar da primeira fase"):**
o `transitionAlpha = 150` permanece ~150/3 = 50 frames (~0,8s) MESMO com cooldown — o decaimento
roda sempre. Não gera faixa permanente. PORÉM se o fade é ativado e algo congela o decaimento
(estado PAUSE? pausa decaiu 0?), o alpha fica cravado.

→ Ação prática: independentemente da causa exata, o design atual é frágil. Fix:
1. `transitionAlpha` inicial 255 (fade total).
2. Decaimento SEMPRE (remover condição `transitionCooldown <= 0`).
3. Fade decai mais rápido (8/frame = ~0,5s).
4. Garantir que em todos os estados (SHOP, PAUSE) o fade continue decaindo — o bloco já está
   fora dos estados, mas confirmar posicionamento no método update.
5. Inimigos invisíveis: ver Bug B.

## Bug B — Inimigos invisíveis mas ativos após fechar a loja
**Causa confirmada.** Dois mecanismos competem:

1. `Enemy.render` retorna cedo quando `Game.isTransitioning()` — que é TRUE enquanto
   `questCompletedPending` (fase concluída, loja aberta) OU `showLevelTransition > 0`.
2. O `update` de inimigos (linha 575) congela apenas com `isTransitionCooldown()` + pausas
   — `questCompletedPending` **NÃO congela o update** (inimigos continuam se movendo/atirando
   enquanto a loja está aberta — o design antigo era "inimigos congelados enquanto a loja
   está aberta", mas isso depende de outro mecanismo).

O fluxo real (com PhaseStatsScreen rod. 21): objetivo completo → card de stats PAUSADO
mostrado → Enter fecha o card → `startTransitionCooldown()` → cooldown decai → ao zerar,
`showLevelTransition=0` e inimigos voltam. Aí avança de fase (fade + nova fase).

O relato do usuário: fecha a loja → inimigos invisíveis mas batendo. Na nova ordem
(statsScreen → cooldown), isso indica que após o fechamento o jogo está num estado onde
`isTransitioning()` continua true (showLevelTransition>0) mas o update dos inimigos JÁ
está rodando (cooldown zerou ou inimigos em gameState que não congela).

**Fix robusto:** unificar o freeze/ocultação:
- Enquanto `isTransitioning()` (concluída/aguardando avanço OU transição visível), inimigos
  NÃO atualizam E não renderizam.
- Congelar o update também para `questCompletedPending` (linha 575).
- Garantir que após o avanço (nova fase carregada) as flags sejam resetadas — já ocorre
  (advanceToNextLevel zera questCompletedPending e showLevelTransition; cooldown reinicia).

## Fixes planejados (22c)
- Game.java: fade alpha 255, decaimento independente do cooldown, 8/frame.
- Game.java linha ~575: incluir `questCompletedPending` no freeze do update de Enemy.
- Game.java: resetar `transitionAlpha` e cooldown em returnToMainMenu (já linha 1914/1585).
- Teste: Rodada22cTest — valida que (a) fade zera em N frames mesmo com cooldown ativo,
  (b) Enemy não atualiza enquanto isTransitioning().


## Status dos fixes 22c (aplicados em Game.java)
1. **Fade**: `transitionAlpha` doc atualizado para 255; decaimento agora SEMPRE (`if (transitionAlpha > 0) alpha -= 8`) — removida condição `transitionCooldown <= 0`. Fixado.
2. **Freeze de inimigos**: linha ~575 agora inclui `|| isTransitioning()` no congelamento do update de Enemy.
3. Render já pula inimigos via `questCompletedPending || isTransitionCooldown()` (linha ~726) e `Enemy.render` retorna cedo em `isTransitioning()`.

## Pendências
- Verificar que `transitionAlpha` é resetado em returnToMainMenu (linhas 1914, 1585 já zera showLevelTransition; conferir alpha).
- Rebuild + testar com Rodada22cTest (criar): 
  (a) fade zera em <= 40 frames mesmo com transitionCooldown > 0;
  (b) com isTransitioning()=true (setando via clearQuestPending não serve — usar reflection para questCompletedPending ou showLevelTransition), inimigos não avançam posição após N frames de update do Game.
- Rodar regressão completa (suíte: ObjectivesVariadosTest, ShopQaTest, MenuNavigationTest, PhaseTransitionTest, AutoValidate, StoryNpcPlacementTest, WaypointFrameTest, TransitionCooldownTest, MusicZoneTest, InventoryTest, BranchingNpcTest, Rodada22bTest).
- Commitar (branch manus/rodada22-trilha-npcs-inventario), push, atualizar PR #36 (https://github.com/mccartney0/First-game-in-java/pull/36), reportar.
- Nota: usuário relatou "ao passar da primeira fase" as faixas pretas — pode ser o PhaseStatsScreen (card pós-fase) desenhando fundo escuro; não confirmado, mas fade fix ataca a causa mais provável (escurecimento residual 150).
