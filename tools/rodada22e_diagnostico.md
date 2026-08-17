# Rodada 22e — Diagnóstico: backdrop escuro permanente após transição de fase

## Sintoma (print do usuário)
Após passar de fase, se o jogo "trava" (respiro/cooldown) e o usuário aperta teclas/Enter, ele "descongela" e fica com um escurecimento tipo backdrop de modal por cima da tela inteira, permanente, enquanto o jogo continua rodando (HUD, mobs visíveis e ativos).

## Observações do print
A imagem mostra a janela com conteúdo do jogo na metade inferior e uma área escurecida na parte superior — o escurecimento cobre o jogo inteiro, não é letterboxing. O fade 255 e a faixa de conclusão já foram tratados na 22c/22d (fade decai -8/frame e faixa exige loja fechada, alpha 120).

## Hipóteses
1. **`transitionAlpha` não decai em algum estado:** o decaimento `if (transitionAlpha > 0) transitionAlpha = Math.max(0, transitionAlpha - 8)` está no update() e é incondicional — deveria zerar. MAS se o usuário "aperta botões" e isso re-seta o alpha ou congela... verificar handlers de teclado que setam transitionAlpha.
2. **`damageOverlayFrames` ou outro overlay permanente:** só decai quando > 0 no render. Se algo seta frames = valor fixo contínuo (ex.: loop de dano durante o cooldown), vira backdrop. Procurar setções de damageOverlayFrames.
3. **Overlay de pausa/ESC com gameState=PAUSED ou similar:** ESC no cooldown do respiro pode estar entrando em estado de pausa com backdrop (Menu.renderPauseScreen tem backdrop escuro?). Verificar pausable com isTransitionCooldown.
4. **Double-buffering corrompido / BufferStrategy de 3 buffers:** se um buffer ficou com o frame do fade e o flip não atualizou (swap em tela cheia do Windows + alt-tab), o backdrop pode "colar". O usuário mencionou minimizado antes. Investigar se há `contentsLost` recovery.
5. **`showLevelTransition` faixa alpha 120:** durante ~4s — não permanente. Mas com a janela maior que scaled (resize) e offsetY, o fillRect do overlay cobre só scaledHeight; o fundo preto da janela cobre o resto — consistente com a imagem? A imagem mostra área escura no topo e jogo normal embaixo — isso É o letterboxing/fundo preto da janela quando o jogo desenha com offsetY>0... Mas a janela não foi redimensionada (não há bordas). A imagem é screenshot com barra de título e barra de tarefas — 834px de altura útil vs 864 do jogo... o jogo está CORTADO embaixo, e o topo escuro = fundo preto da janela preenchido. Ou seja: o jogo renderiza em 1536x864 mas a janela mostra só ~560px de altura? Não — a janela é a mesma; o que mudou foi o `recomputeScale` após o evento: talvez SCALE mudou para 3 (1152x648) e o offsetY fica grande → fundo preto nas bordas + jogo pequeno no centro. O print do usuário: jogo normal embaixo, escuro em cima → o jogo foi desenhado na parte INFERIOR com espaço preto em cima = offsetY grande.

## Hipótese mais provável
**Recompute de escala durante/respiração:** quando o usuário pressiona teclas (ex.: F11, resize, ou o evento do jogo muda fullscreen state), `recomputeScale()` é chamado e recria a buffer strategy; durante a transição, o render pode pular frames e o frame preto do fade fica preso num buffer, OU o offsetY muda e o jogo pula para o canto inferior.

## Próximos passos
- Procurar todos os pontos que setam transitionAlpha (para achar re-set permanente).
- Procurar recomputeScale/SCALE changes e fullscreen toggle.
- Verificar handlers de teclado que podem ativar um overlay permanente.

## Progresso do diagnóstico (candidatos de overlay escuro)
- `transitionAlpha` decai incondicionalmente (linha 670) e é zerado em vários pontos (126, 1466, 1596, 1925) → NÃO é o fade.
- `damageOverlayFrames` (180,30,30,alpha, linha 848) só decai no render; é setado em linha 392 (`Math.max(..., DAMAGE_OVERLAY_DURATION)`) e zerado em 1917 (returnToMainMenu). Se algum caminho seta frames continuamente → backdrop vermelho escuro, não preto. Menos provável.
- Candidatos restantes (backdrop preto):
  - **PhaseStatsScreen.java:118** — `g.setColor(new Color(0,0,0,(int)(alpha*0.75)))` fillRect sobre tela — alpha depende de isShowing(). Se show nunca fecha (p.ex., Enter não fecha no cooldown?), fica permanente. O usuário: "aperto os botões e enter, dai ele descongela" — PhaseStatsScreen.show() é chamado no advanceToNextLevel (linha 1678) SEM o card auto-dismiss na campanha; só fecha com Enter. MAS a condição no update(): `if (PhaseStatsScreen.isShowing()) { PhaseStatsScreen.update(enter,escape); enter=false; ... }` — se o usuário apertou Enter ANTES (durante o congelamento) ou durante, o update consome enter e o card fecha. Porém a descrição "descongela e fica assim" sugere que o ENTER foi consumido por outro handler (ex.: ShopManager/LevelUp) e o PhaseStatsScreen NÃO fechou. E o jogo prossegue (avança a fase) enquanto o card continua aberto?? Não — advanceToNextLevel mostra o card ANTES de avançar... na verdade a ordem é: PhaseStatsScreen.show() → transitionAlpha=255 → restart → lore. O card fica por cima até Enter. Se o primeiro Enter foi consumido pelo "shopPendingOpened"? O update: PhaseStatsScreen.isShowing() é checado PRIMEIRO e consome enter. Mas se o jogador apertou Enter durante o fade (antes do update rodar), o flag `enter` é setado; quando o update rodar, é consumido pelo PhaseStatsScreen → fecha. Se o jogador segurou Enter, enter=true a cada frame → fecha no primeiro update.
  - **VICTORYCUTSCENE** similar.
  - **LevelUpManager.java:237** backdrop 170 — se o level-up não fecha (ESC consumido?), backdrop persiste. O LevelUpManager.update roda com gameState=LEVELUP.
  - **Menu.renderPauseScreen:453** (0,0,0,150) — se ESC durante transição abre pausa e não fecha? O ESC handler agora fecha inventário e abre pausa; durante pause, teclas não avançam... 
- Observação do print: o fundo é escuro mas o jogo (HUD, mobs, inventário) renderiza NORMAL por baixo — ou seja, algum overlay escuro permanente está cobrindo. O print mostra VIDA/ESCUDO/MANA/PADRÃO no canto inferior e mobs ativos → gameState NORMAL. Overlays que desenham em NORMAL: GameOver (não), faixa conclusão (exige slt>0, agora max 240 frames), pause (exige pause true), dialogue (não), onboarding (se ativo).
- **HIPÓTESE FORTE: OnboardingManager.isActive()/render** — OnboardingManager desenha overlay escuro 235 e NÃO sai do estado se o jogador já tiver completado? Verificar OnboardingManager.render/update — se o onboarding fica "preso" após a transição, o backdrop persiste para sempre em NORMAL.
- Próximo: ler OnboardingManager (isActive, render 233, update) e PhaseStatsScreen (show/update/isShowing/alpha).

## Contexto
- Branch: manus/rodada22-trilha-npcs-inventario, PR #36: https://github.com/mccartney0/First-game-in-java/pull/36
- Build: javac -d bin -cp bin (find src -name *.java); testes: out=/tmp/test_X; javac -d $out -cp bin:res tools/X.java; DISPLAY=:120 timeout 60 java -cp $out:bin:res X

## Refinamento
O fluxo avanço de fase (linha ~1668-1690): `advanceToNextLevel` → `prepareForLevel` → `restartGame` → `PhaseStatsScreen.show()` → `transitionAlpha=255` → `showLevelTransition=90+150=240`. No update: o card intercepta Enter/ESC e consome os flags SEMPRE (mesmo antes de FADE_TOTAL). Então Enter fecha o card... MAS espere: o usuário descreve "buga toda hora q passo de fase, dai aperto os botões e enter, dai ele descongela e fica assim (backdrop)". "Descongela" = o jogo volta a responder (fim do respir
o/cooldown ou fechamento do card). O backdrop residual permanente em tela NORMAL:

Candidato forte agora: **MissionHud.java:258/264/274** (backdrops alpha 150/220/190) — a HUD de missão com condições persistentes? Verificar linhas 250-280 do MissionHud: pode desenhar painel escuro com condições permanentes (ex.: `questCompletedPending` sem reset, ou banner visível sempre).
Verificar também: linha 849 `overlayG.fillRect(0,0,scaledWidth,scaledHeight)` com fillColor? O fillRect 849 é o overlayG = g (já com translate/scale) — se o fillColor for escuro com alpha, cobre tudo. Ver linha 845-852 (contexto da linha 849: fillRect do overlay base?) — na linha 849 é `overlayG.fillRect(0,0,scaledWidth,scaledHeight)` com o color do paint anterior! Se alguém setou overlayG.color para escuro antes e não restaurou, TODO o render seguinte fica escuro! VERIFICAR ISSO — é a hipótese mais plausível para "backdrop permanente": o fillColor do Graphics overlay não é resetado por frame se o código dependia do fillRect inicial do buffer `g` (que é outro objeto). O overlayG é recriado a cada render? Ver 780-845.

## Decisão do fix 22e
Regra de escala: em FULLSCREEN, usar frame.getBounds() (tela inteira, sem descontar a taskbar) para computar SCALE — assim SCALE mantém 4 no maximizado com taskbar e não downscale para 3 (que encolhia o jogo para 1152x648 e criava o letterboxing/preto em volta que o usuário chamou de "backdrop de modal"). Em modo janela, manter a regra atual (encaixar na área útil).

Fix em recomputeScale():
```java
public static void recomputeScale() {
    int width, height;
    if (fullscreen) {
        java.awt.Rectangle b = frame.getBounds();
        width = Math.max(1, b.width);
        height = Math.max(1, b.height);
    } else {
        width = Math.max(1, frame.getContentPane().getWidth());
        height = Math.max(1, frame.getContentPane().getHeight());
    }
    SCALE = Math.max(1, Math.min(width / WIDTH, height / HEIGHT));
}
```
Também verificar o installResizeListener em modo janela: o setSize(target) com frame.setResizable(false) — no Windows o setSize pode não surtir efeito quando a janela está MAXIMIZED_BOTH? Não, setResizable(false) mantém o resize habilitado para maximizar. OK.

Depois: rebuild, teste que simula maximizado (frame.getBounds com taskbar), regressão, commit, push, reportar.
