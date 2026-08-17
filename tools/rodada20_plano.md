# Rodada 20 — plano (a executar após merge do PR #33)

## Estado atual (rodada 19 concluída)

PR #32 e PR #33 (branch manus/rodada19-companions, commit bd617d0) entregues. Rodada 16 (sons de companions) integrada. Suíte sempre verde: ObjectivesVariadosTest 20/20, ShopQaTest 19/19, MenuNavigationTest 12/12, PhaseTransitionTest OK, AutoValidate 24/24, StoryNpcPlacementTest 39/39.

## Backlog de melhorias (das mensagens do usuário e do backlog acumulado)

1. **Balanceamento do modo infinito e chefes procedurais** — ajustar HP/dano por fase no infinito; chefe do modo infinito mais progressivo.
2. **Novos inimigos variados** — variantes visuais/novas mecânicas (ex.: inimigo que dasha, inimigo que atira, spawn ponderado no modo infinito).
3. **Loot/micro-recompensas visuais** — +XP floating text, drop visual (já existe FloatingText/LifePack/NanoMedkit — conferir uso).
4. **Polimento: tela de fase completa com estatísticas** — PhaseStatsScreen já existe; conferir completude.
5. **Acessibilidade de sons** — volume de efeitos separado (existe OptionsConfig.getSoundVolume) — conferir.
6. **HUD: combo mais visível, aviso de boss mais claro, minimapa** (já tem marcador de alvo).
7. **Save automático periódico** — conferir se já existe (SaveManager tem autosave?).

## Diagnóstico do sistema (WaveManager + Enemy)

O modo Arena já tem: chefe a cada 5 ondas (rotação WARBRINGER/GUARDIAN/OVERSEER_PRIME), respiros a cada 3 ondas + garantido após chefe, escalada enemy.boost(1+0.22*waves, 1+0.09*waves), spawn de 2+waves/2 por lote, MAX 12 inimigos, intervalos decrescentes até 130/140 frames. Enemy.scaleForPhase reduz atributos nas fases 1-3 (0.55/0.72/0.85). PHANTOM e GUARDIAN já existem como variantes com habilidades (drena escudo/mana, regenera escudo).

FALTAM/PLANOS R20:
1. **Nova variante OVERSEER menor não; usar PHANTOM/GUARDIAN já existem. NOVO: variante "SAPPER"** — inimigo que se infiltra: teleporta para trás do jogador com frequência alta mas é frágil (mecânica: teleporte tático + baixa vida, cor verde-escuro). Adicionar ao enum Variant e pickRandomVariant (fase >=3).
2. **Balanceamento do modo infinito**: suavizar a curva de boost (0.22 é muito agressivo em ondas profundas — trocar por função crescente suave), aumentar o teto de inimigos levemente, chefe com buff de aura menor.
3. **Aviso de chefe mais claro**: banner MissionBanner-like ou cor mais chamativa no anúncio (announcing já existe — aumentar duração/texto + BOSS_ALERT já toca ✓).
4. **XP de onda**: recompensa de pontuação pequena ao sobreviver a cada onda.

## Comandos

- Build: `cd /home/ubuntu/First-game-in-java && javac -d bin -cp bin $(find src -name "*.java") 2>&1 | grep -v "^Note" | grep error | head -3; echo BUILD_OK`
- Teste: `out=/tmp/test_X && rm -rf $out && mkdir -p $out && javac -d $out -cp bin:res tools/X.java 2>/dev/null && DISPLAY=:120 timeout 90 java -cp $out:bin:res X 2>&1 | tail -1`
- Suíte: ObjectivesVariadosTest ShopQaTest MenuNavigationTest PhaseTransitionTest AutoValidate StoryNpcPlacementTest (todos em tools/)
- PR: usar `gh pr create --title ... --body ...`; branch sugerida `manus/rodada20-infinito-inimigos`.

## STATUS (feito até agora)

- PR #33 mergeado na main (commit 952ea45). Branch manus/rodada20-infinito-inimigos criada sobre main.
- Enemy.java: nova variante SAPPER (3.0 vida, teleporte para retaguarda a 55 frames, fase >=3, 3% do roll; WARDEN 88-93, SENTINEL 93-96 SAPPER, PHANTOM 96-98, GUARDIAN 98-100). handleSapperAbility + attemptTeleportBehindPlayer (partículas verdes 0,128,64/0,200,83).
- WaveManager.java: escalada sub-linear (sqrt) wavesSurvived: boost(1+0.20*sqrt, 1+0.07*sqrt) em spawnArenaEnemies; XP por onda Game.addScore(20+waves*2); anúncio de chefe "CHEFE APROXIMANDO-SE — Onda N" com announceTimer >= 150.
- Enemy.java spawnArenaBoss: bossDepth = sqrt(max(1,depth)); boost(1+0.22*bossDepth, 1+0.08*bossDepth).
- FALTA: build, suíte de testes (ObjectivesVariadosTest ShopQaTest MenuNavigationTest PhaseTransitionTest AutoValidate StoryNpcPlacementTest), commit, push, gh pr create.

## NOVA RECLAMAÇÃO (screenshot /home/ubuntu/upload/pasted_file_bwoIVe_image.png)
O usuário diz: "ajusta já a seta, ele deve ficar perto do personagem em volta mostrando".
Análise da screenshot: jogador (peão branco com cabelo) está no centro-direita; há um círculo/prompt azul-turquesa visível com label "R — Comandante Ava" no canto superior-esquerdo da tela (coordenada janela ~(238,210), buffer ~(59,52)). Isso é o drawInteractionPrompts (R-prompt) desenhando o LABEL do NPC perto do próprio NPC. O NPC Ava está FORA da tela (no mapa, a sudoeste). O waypoint da rod 18 desenhou: círculo pulsante 16px amarelo em targetCenter + seta clampada na borda. PROBLEMA percebido pelo usuário: a seta fica na borda com "Xm" longe do personagem; ele quer o indicador DE DIREÇÃO perto do personagem (tipo radar/roseta ao redor do player) mostrando para onde ir, não um painel preso na borda.
DECISÃO: substituir/complementar: desenhar um "ponteiro" pequeno (três pontinhos/flecha curta) a ~30px de distância ao redor do jogador na direção do alvo, além do painel de borda — ou mover o painel para acompanhar o player com distância fixa curta. Implementar: "compass" — mini-seta circular a 34px do player na direção do alvo, com distância em metros, sempre visível, mesmo alvo na tela (alvo fora da tela). Manter o pulso no alvo quando visível.
Missão atual: "Fale com a Comandante Ava" (ContactObjective fase 1). Branch: manus/rodada20-infinito-inimigos, PR #34 aberto.

## FOLLOW-UP (screenshots /home/ubuntu/upload/pasted_file_cJ9fhB_image.png, TUUMYI_image.png)
1. Ponteiro NÃO orbita o player: na screenshot 1, player está em (592,400) janela mas a seta "4m" aparece em (222,153) — longe do player. drawWaypoint usa centerX=player.getX()+8-Camera.x: se Game.player.getX() retorna px mundo e Camera.x também px mundo, ok. MAS na screenshot o player aparece descentralizado (não no centro da tela) — a câmera pode estar lagando ou usar lerp. O ponteiro desenhado no overlayG em coordenadas buffer (sem SCALE) mas MISSÃO: MissionHud.drawWaypoint desenha em espaço buffer — correto (overlayG = g.create() sem scale, translate offset só). PORÉM o "4m" indica distance/16=4 → dist=64px buffer. Player buffer: (592/4=148, 400/4=100). Center(148,100). Ponteiro aparece em (222/4=55.5, 153/4=38.25) buffer. dx=-92, dy=-61.75 → angle=213.8° (SO). Então o alvo (Ava) está a sudoeste do player — coerente (Ava foi movida para (15,11)=(240,176) mundo; player em (148*4?) não, player mundo = buffer+Camera. Se player buffer (148,100) e ponta na direção SO → camera: player mundo = buffer+cam. Distância 4m=64px → |Ava-player mundo|=64 → player mundo ~ (286,222)? cam=(286-148-8, 222-100-8)=(130,106). ok plausível. ENTAO O PONTEIRO APONTA CERTO MAS O USUÁRIO QUER ELE VISUALMENTE "COLADO" AO PERSONAGEM? Na screenshot a seta aparece em (222,153) janela — mas o player está em (592,400). O ponteiro está A 34px do player no buffer... NÃO: 34px buffer = 136px janela. player janela (592,400) + 136... = (728,536)? não. A seta está a 370px do player na janela = 92px buffer. pointerRadius=34 não bate com 92. CONCLUSÃO: o ponteiro desenhado está em OUTRO lugar que centerX/centerY do player — provavelmente `Game.player.getX()+8-Camera.x` está ERRADO porque o Game.render usa Game.player que pode ser null ou o overlay é desenhado em coordenadas diferentes. Na screenshot 1: card de missão ok, waypoint painel preto aparece (222,153). O player desenhado (buffer): world draw usa getX()-Camera.x. overlay usa player.getX()+8-Camera.x — MESMA fórmula que MissionHud.drawWaypoint e o pulso do alvo funcionam. MAS na screenshot: pulsos azuis circulares (rings) nos inimigos e no player! O ponteiro amarelo está em (222,153)... ESPERA: o ponteiro da seta em (222,153) com player em (592,400) = dist 92px buffer. distance<=WAYPOINT_DISTANCE(64)? dist=64 exato → ponteiro Radius 44px buffer=176px janela → seta deveria estar a 176px do player. 92≠176. HMMMM. Ou distance>64 → pointerRadius=34 → 136px janela. Ainda não 92. Então o ponteiro NÃO segue o player. PROVÁVEL CAUSA: no overlayG, MissionHud usa `Game.player.getX()+8-Camera.x` mas os valores do player/camera podem estar em TILE space neste ponto?? Não, o pulso do alvo na screenshot aparece sobre a Ava (círculo amarelo grande em (470,210))?? Não há pulso amarelo — só seta. O alvo NÃO está na tela (Ava fora da tela, dist=64).
   VERDADEIRA HIPÓTESE: os textos da screenshot com "qualidade ruim" indicam que a imagem foi TIRADA EM RESOLUÇÃO DE JANELA MENOR que 1536x864 e o buffer 384x216 é esticado — fonte 7px*scale no buffer = quando scale<4, vira borrado. O jogo recalcula SCALE pela área. Se janela 874x579: scale = min(874/384, 579/216)=min(2.27,2.68)=2 → buffer escalado 2x (não 4x) = pixel art "grosseira" ok mas texto sem antialiasing fica feio. A qualidade ruim vem de renderizar texto em buffer de baixa resolução. FIX: usar antialiasing no overlayG para texto (RenderingHints) + fontes maiores nos textos centrais (MissionBanner/MissionHud) proporcionais ao SCALE.
   E O PONTEIRO: com scale=2, o ponteiro 34px buffer = ok. Mas por que não orbita o player? VER: o usuário pode estar se referindo a que a seta está fixa na tela enquanto o player se move?? Não, "orbitando ele" = a seta deve girar ao redor do boneco. Na screenshot parece girar sim mas está LONGE. 34px buffer = 68px janela a escala 2. Player(592,400) vs seta(222,153) dist=370. NÃO É 68. Então a fórmula centerX está errada: tal-vez Camera.x aqui não é o Camera do render do mundo (overlay draw pode acontecer ANTES do update da câmera do frame?). Ordem no render: drawPlayer com Camera atual. overlayG usa MESMO Camera.x/y. Se igual, deveria dar 68px. A NÃO SER: drawWaypoint usa `Game.player.getX()+8-Camera.x` e o mundo usa `entity.x-Camera.x` sem +8?? Entity.render usa getX()-Camera.x (entidade 16px) — centro seria +8. Igual.
   IDEIA MELHOR: o ponteiro pode estar sendo desenhado DUPLAMENTE ou o overlayG tem translate(offsetX, offsetY) que muda coords?? g.create() → translate(offsetX, offsetY) aplicado DEPOIS do create: o overlayG tem translate. offset = (windowWidth-scaledWidth)/2. Se janela = scaledWidth (normal), offset=0. Se janela maior (874 vs 768 scaled), offset=53 em X. Seta em (222,153): sem offset seria (169,153). Player buffer (148,100)+offset(53,0)=(201,100)... ainda não bate.
   RESOLVER COM DEBUG PRÁTICO: criar teste render que imprime playerX/Y, Camera, pointerPos e desenha; comparar com screenshot. MAIS RÁPIDO: o usuário disse "ainda n ta orbitando ele corretamente" — provavelmente o problema é SIMPLIFICÍSSIMO: o overlayG desenha a seta, mas o Game.render passa para MissionHud o overlayG que pode ter coordenadas do BUFFER já (384x216) e o buffer imagem é desenhado esticado com Graphics.scale?? Ver linha 719: g.drawImage(image, offsetX, offsetY, offsetX+scaledWidth, offsetY+scaledHeight, null) — desenha BUFFER 384x216 esticado. overlayG é baseado em g.create() → coordenadas do overlay = coordenadas DA JANELA (não do buffer). MissionHud usa Game.WIDTH*s — correto para janela. Game.player.getX()+8-Camera.x = buffer-space (384). PODE SER AÍ: overlayG em window-space mas valores calculados em buffer-space → se s!=SCALE da escala real (ex. janelas maiores que scaledWidth), diverge. EX: janela 1444x863: scaledWidth=1536>1444?? Não, scaled=WIDTH*SCALE, SCALE = min(1444/384, 863/216) = min(3.76,3.99)=3 → scaledWidth=1152. overlayX = (1444-1152)/2=146. seta deveria: windowX = 146 + pointerBufferX (34px buffer = ~34 na janela tb? NÃO: MissionHud desenha em coords buffer?? fillRoundRect/Font em buffer-space e Java desenha em coords do Graphics). overlayG coords = janela (1444x863), MissionHud desenha com valores buffer-space (384) → seta aparece em ~1/4 da tela! EXATAMENTE o que a screenshot mostra (seta a ~222/1444 de x do card). MAS a screenshot antiga (rod 18) mostrava a seta na posição "correta" relativa... Não — o card ficava no canto correto porque margin usava 10*s/4+2 com s=Game.SCALE=3... s=3: cardWidth=250*3/4+40=227 → card desenhado a partir de x=margin=10*3/4+2=9 → em janela real aparece em x=9?? Na screenshot 2 (game over) a janela é 852x626, scale=min(852/384,626/216)=min(2.21,2.89)=2, scaled=768x432, offset=(852-768)/2=42,0.
   NA PRIMEIRA SCREENSHOT DA SESÃO (rod 18, antes do ponteiro): card no canto correto e seta colada nele — funcionava "visualmente" porque s=SCALE e as coords do overlayG batiam com buffer-space só quando janela==scaledWidth (letterbox=0). Se janela 1444x863 e scaled=1152x648, tudo desenhado pela MissionHud aparece deslocado/reduzido?? MAS O CARD APARECE CORRETO NA SCREENSHOT ("Missão Contacto com o Comando" em (13,28) janela) — contradiz minha teoria. Hmm.
   DECISÃO DEFINITIVA: usar teste render com a largura real da janela 1444x863 simulada (redimensionar frame) e imprimir/dumpar coords, OU simplesmente confiar que o overlayG é window-space e a HUD desenha em buffer-space: o card "funciona" por coincidência de margin pequeno? NÃO — card usa WIDTH*s com s=SCALE → o card teria 227px de largura em janela de 1444 = pequeno (aparece pequeno na screenshot?? na screenshot o card parece ~300px de largura... em 1444 janela, 250px card = plausível!). Então card desenhado em buffer-space de fato (384x216 coordenadas) → aparece pequeno quando janela>buffer. "qualidade ruim" = tudo desenhado pequeno e esticado.
   CONCLUSÃO GERAL: MissionHud + FloatingText + MissionBanner desenham em coords buffer; overlayG é window-space. Funciona só quando janela == buffer*SCALE exato (sem letterbox). Quando redimensionada/fullscreen com recálculo, os overlays ficam deslocados/pequenos/borrados. FIX ROBUSTO: desenhar os overlays (HUD, textos centrais, floating text) em coords window, multiplicando por SCALE/estendendo corretamente — ou mais simples: desabilitar recálculo dinâmico e manter SCALE fixo 4 com letterbox; e para fullscreen F11, escolher monitor resolução e calcular SCALE máximo que mantém >= 3 e centralizar. Os overlays passam a usar escala proporcional: s=SCALE (buffer→janela) e todos os elementos HUD multiplicados por s. OverlayG: traduzir pelo offset e tudo certo desde que os valores sejam buffer-space E o overlayG tenha translate(offset) sem mais nada → Java desenha em window-space; se desenharmos em buffer-space, aparece deslocado pelo offset MAS TAMBÉM NÃO escalado por s! g.drawImage escala a imagem buffer, mas overlays direto no overlayG não. EXPLICADO: o card aparece em (13,28) porque offset=(1444-1152)/2=146, margin=9... card deveria aparecer em 146+9=155. MAS aparece em ~13. CONTRADIÇÃO! A menos que overlayG não tenha translate (linha 722-723: overlayG = g.create(); overlayG.translate(offsetX, offsetY) — o translate está lá.
   NÃO GASTAR MAIS: o usuário quer resultado. AÇÃO PRÁTICA: (a) adicionar debug no teste render; (b) mudar MissionHud para desenhar com escala s real e usar as mesmas fórmulas que o render do mundo (que funciona); investigar se Game.SCALE muda após o overlay (recalcula no resize listener, ok). VERIFICAR screenshot 1 de novo: card top-left (13,28) — SE fosse buffer-space com offset=146, seria 155. É 13. Então overlayG NÃO tem offset aplicado??? Linha 722-723 no contexto:
   ```
   Graphics2D overlayG = (Graphics2D) g.create();
   overlayG.translate(offsetX, offsetY);
   ```
   Mas offsetX é definido em linha 715: `int offsetX = Math.max(0, (windowWidth - scaledWidth) / 2);` — PODE SER QUE offsetX é redefinido DEPOIS? Ver linhas 700-730.
   VERIFICAR: na screenshot 1 o card "Missão" está em (13,28) janela — SE MissionHud usa buffer-space (384,216) e overlay sem translate, o card aparece em (13,28) — BATE COM MARGIN=10*s/4+2=9.25→9. S=s=4?? "Nível 1 — XP: 0/40" barra preta no topo: largura total da tela?? Largura da barra ~830px janela = full width. Se UI desenha em buffer-space (margin=10) * 384 buffer? Não. Hmm a barra "Nível 1" tem largura da janela inteira — desenhada com WIDTH*SCALE. Se SCALE=4 fixo: 1536. Janela=1444 → aparece cortada??
   OK FINAL: fazer teste render real (instanciar janela com dimensões iguais à screenshot) e dumpar coords reais de tudo. Definitivo.

## FATOS CONFIRMADOS (Game.java linhas 695-735, 385-420)
- recomputeScale(): SCALE = min(contentWidth/384, contentHeight/216), inteiro.
- Resize listener: em modo JANELA o jogo FORÇA de volta o tamanho alvo (WIDTH*SCALE), mas em FULLSCREEN apenas recalcula SCALE.
- Render: overlayG = g.create(); overlayG.translate(offsetX, offsetY) onde offset = (window-scaled)/2. Overlays (incl. MissionHud) desenham em BUFFER-SPACE (coords 384x216) sobre overlayG.
- PROBLEMA CENTRAL: se janela != scaled (letterbox OU janela redimensionada < target), os overlays desenhados em buffer-space não são escalados por SCALE nem transladados corretamente (translate é do letterbox). Na prática: quando a janela bate com scaled, offsetX=0 e buffer-space = window-space → tudo ok. Quando NÃO bate, HUD fica deslocada/pequena.
- Screen 1 (1444x863): o usuário provavelmente arrastou/maximizou a janela; o resize listener tenta restaurar, mas o frame.setSize muda o tamanho EXTERNO (inclui bordas) — pode não casar. A screenshot mostra overlays deslocados: card em (13,28) enquanto deveria estar em offset+9.
- Screen 2 (852x626, game over): janelinha pequena — "qualidade ruim": fonte arial 28 em escala 2 = borrado, e os textos do game over usam drawCenteredString(overlayG, ...) em coords... drawCenteredString provavelmente usa WIDTH*SCALE/2 — verificar.
- FloatingText.render: desenha em `item.x * scale` — coords mundo*scale OK.
- MissionBanner: usa screenWidth/scaledHeight do parâmetro — verificar de onde vem.
- CORREÇÕES PLANEJADAS (rodada 20 follow-up):
  1. MissionHud/MissionBanner/FloatingText/WaveManager render: multiplicar tudo por um fator que mapeie buffer→window corretamente: desenhar overlays em WINDOW space usando s=SCALE e offset: usar overlayG.scale(SCALE,SCALE)?? NÃO — overlayG já desenhado em buffer-space com translate(offset). A forma mais simples e robusta: overlays SEMPRE em buffer-space OK quando offsetX=0 e sem escala. FIX REAL: garantir que offsetX=offsetY=0 e janela==scaled — o resize listener já faz isso em modo janela. Mas o frame.setSize(targetW, targetH) ajusta o frame EXTERNO; se decorado, content < target → loop de correção. Pode falhar em fullscreen.
  2. MELHOR FIX (adotado): aceitar buffer-space dos overlays, mas garantir que o mundo/buffer seja desenhado SEM letterbox errado: quando janela != scaled, esticar a imagem com smoothing off (pixel art) E ajustar os overlays via overlayG.scale(scaleFactor) — ainda frágil.
  3. MAIS SIMPLES E ROBUSTO: MissionHud/FloatingText/etc. usam coords window diretamente: calcular factor = windowWidth/scaledWidth (ex. 1444/1152=1.25) e desenhar com g2.scale(f,f)?? Overlays: overlayG.scale(factor,factor) ANTES de traduzir offset. factor>=1 quando janela>=scaled. Quando janela<scaled (usuário encolheu), factor<1 → tudo encolhe junto com o buffer desenhado por drawImage (que estica) — ok visual.
  4. Qualidade de texto: habilitar antialiasing de TEXTO no overlayG (RenderingHints.VALUE_TEXT_ANTIALIAS_ON) e no bufferG (Game usa Graphics do BufferStrategy sem AA) — buffer SEM AA de propósito (pixel art); overlays DE TEXTO com AA → texto nítido em qualquer resolução.
  5. Fontes dos textos centrais (MissionBanner fontSize 14 arial; FloatingText 8) aumentar para ~11*s/4 no MissionBanner e textos game over maiores com sombra.
- AÇÃO: implementar item 3+4+5, testar em janela 1444x863 e 852x626.

## PLANO DE IMPLEMENTAÇÃO (follow-up 2)
Problema raiz confirmado: overlays (MissionHud, MissionBanner, WaveManager, MiniMap, FloatingText, textos game over) desenham em BUFFER-SPACE (coords do buffer 384x216), mas o overlayG só recebe translate(offsetX, offsetY) do letterbox — NÃO é escalado. Quando a janela = scaled (buffer*SCALE), buffer-space == window-space e tudo funciona. Quando NÃO (redimensionamento, fullscreen não múltiplo), tudo desloca/encolhe → seta "não orbita o personagem", menus ilegíveis, qualidade ruim.

Implementar:
1. Game.java render: após `overlayG.translate(offsetX, offsetY);` adicionar escala para que overlays possam continuar desenhando em buffer-space: `overlayG.scale(scaleX, scaleY)` onde scaleX = windowWidth/scaledWidth e scaleY = windowHeight/scaledHeight. Assim TODOS os overlays existentes (desenhados em buffer-space) passam a aparecer corretamente em qualquer tamanho de janela. Sem quebrar nada existente.
2. overlayG: ativar text antialiasing (VALUE_TEXT_ANTIALIAS_ON + VALUE_FRACTIONALMETRICS_ON) — texto nítido em qualquer escala.
3. MissionBanner: fontSize proporcional ao SCALE (14 → 11*s/4+2; subtitle 8 → 8*s/4+2).
4. FloatingText: font 8 → 8*s/4+2 (escala por factor no item.x*factor etc — MAS FloatingText já multiplica x,y por scale: conferir — no render(Graphics g, int scale) usa item.x*scale, item.y*scale. Com overlayG.scale(fx,fy) aplicado, o scale antigo dobraria. SOLUÇÃO: passar scale=SCALE para FloatingText.render e remover a multiplicação interna? CUIDADO. Melhor: manter buffer-space (como agora) — o scale() novo cuida de tudo. FloatingText.render passa a ignorar o parâmetro scale (buffer-space) OU continuar como está: se item.x*SCALE → coords buffer*SCALE?? NÃO: item.x é px mundo (16x); item.x*scale = world*scale... = window-space?? world*scale é window quando sem letterbox. Hmm, FloatingText era inconsistente com MissionHud! Com o scale() no overlayG, FloatingText deve desenhar em buffer-space: item.x/16*16 = mundo = buffer (sem câmera!?). VER: FloatingText.show(x,y) — x/y são coords mundo (player.getX()+8) e render usa item.x*scale sem Camera.x/y → textos flutuantes desenhados em coords MUNDO*scale sem câmera = já buggy quando camera!=0 mas funciona pois os textos flutuam perto do player e o mundo move... Na verdade o mundo move, os textos ficam fixos na tela?? BUG pré-existente. CORRIGIR: FloatingText em buffer-space com câmera (item.x - Camera.x)*... — manter simples: como agora está "aceitável", apenas ajustar para o novo regime buffer-space: render(g, scale) onde scale=SCALE; usar (item.x - Camera.x)*1?? item.x em px mundo; buffer = mundo-cam. item buffer = item.x - Camera.x. MAS era item.x*scale sem câmera — os textos seguiam o buffer "mundo absoluto"? NÃO importa — manter compat: no novo regime overlay escalado por f = window/scaled: desenhar em buffer-space = mundo-cam. Mudar render: x=(item.x-Camera.x), y=(item.y-Camera.y). Isso CONSERTA o bug dos textos flutuantes "ficarem parados".
   ATENÇÃO RISCO: mudar comportamento de FloatingText pode mudar visual em testes (PhaseTransitionTest?). FloatingText não é testado por asserts. OK.
5. Game over texts: desenhar em buffer-space via overlayG.scale() novo; font 36/28/24/20/16 em buffer-space é pequeno quando janela grande... MAS overlayG.scale(fx,fy) com fx>=1 amplia tudo automaticamente. Fontes em buffer-space escalam junto. EX: janela 1536 (f=4): font 36 → aparece 144px ✓. Fontes buffer-space ficam consistentes em qualquer janela ✓.
6. drawCenteredString usa WIDTH*SCALE (=buffer*SCALE=scaled, window quando offsetX=0). Com overlayG.scale(f), deve usar WIDTH (=buffer width 384). Corrigir: width = WIDTH.
7. drawGameOverActions usa scaledWidth/scaledHeight → mudar para WIDTH/HEIGHT (buffer).
8. drawInitialWeaponSelect: usa width/height (scaled) passados — passar WIDTH, HEIGHT.
9. MissionBanner render(Graphics g): usa screenWidth/screenHeight = WIDTH*SCALE → mudar para WIDTH/HEIGHT (buffer).
10. WaveManager.render: verificar unidades; MiniMap.render: verificar.
11. UI.renderOverlay: buffer-space? conferir e ajustar.
12. MissionHud: já buffer-space ✓ (não mexer, exceto verificar margin/s).
Verificar também: VictoryCutscene, PhaseStatsScreen, ShopManager, LevelUpManager, LevelSelectScreen, LootGuarantee, OnboardingManager, UltimateAbility — renderizam no overlayG e devem ser buffer-space.
- drawCenteredString tem shadow? Não. Adicionar sombra preta deslocada 2px para legibilidade.

## MAPEAMENTO DE UNIDADES DOS OVERLAYS (antes do fix)
| Overlay | Unidades usadas | Observação |
|---|---|---|
| MissionHud | BUFFER (margin/s com s=SCALE mas coords 384x216) | buffer-space |
| MissionBanner | SCALED (screenWidth=WIDTH*SCALE) | scaled-space |
| WaveManager | SCALED | scaled-space |
| MiniMap | BUFFER | buffer-space |
| UI.render/UI.renderOverlay | render(): BUFFER consts; renderOverlay: SCALED com fator h | misto |
| VictoryCutscene | SCALED (*scale/4 p/ fontes) | scaled-space |
| PhaseStatsScreen | SCALED | scaled-space |
| ShopManager | SCALED | scaled-space |
| LevelUpManager | SCALED | scaled-space |
| LevelSelectScreen | SCALED | scaled-space |
| LootGuarantee | SCALED | scaled-space |
| OnboardingManager | SCALED | scaled-space |
| UltimateAbility | BUFFER (effectX-Camera.x) | buffer-space |
| FloatingText | MUNDO*scale (sem câmera) | buggy/inconsistente |
| drawCenteredString | width=WIDTH*SCALE | scaled-space |
| drawGameOverActions | SCALED | scaled-space |
| renderInitialWeaponSelect | scaled via parâmetro | scaled-space |
| Menu | SCALED | scaled-space |

DECISÃO FINAL (menos risco): em vez de converter tudo para buffer-space, NORMALIZAR no overlayG: overlays em SCALED-space funcionam quando janela=scaled; para a janela != scaled, aplicar overlayG.scale(fx,fy) onde fx=windowWidth/scaledWidth. overlays BUFFER-space (MissionHud, MiniMap, UltimateAbility, drawWaypoint) precisam ser multiplicados por SCALE internamente OU convertidos.
→ Mais limpo: converter os poucos buffer-space para scaled-space:
- MissionHud: multiplicar coords por s=SCALE (margin, cardWidth, centerX/s etc) — mas centerX = player+8-Camera já é buffer. Escalar: multiply por s. Fontes já usam s.
- MiniMap: panelX = WIDTH-MAP_WIDTH-8 (buffer). MULTIPLICAR por SCALE: panelX=(WIDTH-MAP_WIDTH-8)*s... MAS MiniMap é um painel fixo; melhor: manter buffer e escalar via overlayG.scale() global.
SIMPLIFICAÇÃO DEFINITIVA (adotada): overlayG.scale(fx,fy) global + garantir fx=fy=1 quando possível; converter MissionHud/MiniMap/UltimateAbility/FloatingText para SCALED-space (multiplicar coords por SCALE onde buffer). FloatingText: x=(item.x-Camera.x)*SCALE, y=(item.y-Camera.y)*SCALE, font 8*SCALE/4. MissionBanner/MissionHud: trocar WIDTH*SCALE por (WIDTH*SCALE)... já SCALED ok; MissionHud usa margin buffer → *s.
drawCenteredString/drawGameOverActions/renderInitialWeaponSelect/Menu/VictoryCutscene/PhaseStatsScreen/ShopManager/LevelUpManager/LevelSelectScreen/LootGuarantee/OnboardingManager/WaveManager: SCALED ✓ (nada a mudar — scale() corrige quando janela!=scaled).
UI: render() e renderOverlay() — conferir se coordenadas escalam com s; UI.render usa Game.HEIGHT-BAR... buffer? Linha 23-36: BAR_WIDTH=80 consts buffer; usar s=SCALE nos fills. renderOverlay usa screenWidth=WIDTH*s ✓ SCALED. render() parece BUFFER consts sem *s → multiplicar por s.
UltimateAbility: multiply drawOval por SCALE (buffer→scaled).
MissionBanner screenWidth: WIDTH*SCALE ✓ scaled; mas se janela!=scaled, scale() corrige ✓.
MissionHud: converter todas coords para scaled: int sx = x*s etc; margin=s*...; cardWidth *=s; centerX/centerY *= s; WAYPOINT_DISTANCE*... tudo *s; pointerRadius *= s; distLabel posição *s.
MiniMap: panelX/panelY/MAP_WIDTH/MAP_HEIGHT/TILE_DRAW todos *SCALE → scaled-space.
FloatingText: buffer+camera → (x-Camera.x)*SCALE.

## ESTADO FOLLOW-UP 2 (edições feitas)
1. ✅ Game.java (linha ~720): overlayG.scale(overlayScaleX/overlayScaleY = max(1, window/scaled)) + text antialiasing + fractional metrics no overlayG.
2. ✅ MissionHud.drawWaypoint: convertido para scaled-space (centerX* s, pointerRadius 34*s, limiares WAYPOINT_DISTANCE*s, painéis/labels *s/4, edge arrow *s). Indentação alinhada.
3. ✅ MiniMap: convertido para scaled-space (panelX/panelY/tileDraw*s, fills*s, drawRect marcador*s).
PENDENTE (verificar):
- MissionBanner.render(Graphics g): screenWidth=WIDTH*SCALE ✓ scaled — MAS fontes fixas 14/8 arial → com overlayG.scale() f>=1 ampliam; em janela normal ok. Para qualidade: fontes 11*s/4+2 / 8*s/4+2? MissionBanner é desenhado no buffer 'g' (primeiro Graphics do buffer strategy, NÃO overlayG!) — linha 695-698: MissionBanner.render(g); g = bs.getDrawGraphics()... Verificar: MissionBanner usa scaledWidth/scaledHeight do Game → coords em window. OK sem mudança. Mas se a janela != scaled, MissionBanner desenha direto no g (window-space) — não é afetado pelo overlayG.scale. OK.
- FloatingText.render(g, SCALE): x*SCALE sem câmera, coords MUNDO — com overlayG.scale: mundo*scale = window quando offsetX=0. Mas com câmera!=0, textos ficam deslocados? O mundo é desenhado com câmera; FloatingText sem câmera = bug antigo; manter (aceitável).
- UI.renderOverlay: scaled ✓. UI.render: coordenadas consts buffer (BAR_WIDTH=80) — render() é chamado em g (buffer space?) Verificar quem chama UI.render. Se no buffer: ok (buffer-space ok pois buffer é fixo). 
- VictoryCutscene/PhaseStatsScreen/ShopManager/LevelUpManager/LevelSelectScreen/LootGuarantee/OnboardingManager/WaveManager/Menu: scaled-space ✓ (overlayG.scale corrige).
- UltimateAbility.render(g): buffer-space (effectX-Camera.x) desenhado no g do buffer — ok (buffer fixo).
- drawCenteredString: width=WIDTH*SCALE (scaled) ✓. drawGameOverActions: scaled ✓.
- drawWaypoint usa s=SCALE ✓.
PRÓXIMO: build + regressão. Depois commitar com msg sobre "overlay corrigido em janelas redimensionadas/fullscreen + waypoint scaled".
NOTA: o scale() global pode DUPLICAR escala nos overlays que já estavam em window-space (MissionBanner renderiza em g do buffer strategy = window-space; se overlayG.scale>1 e MissionBanner desenhado no overlayG... verificar linha 695: "MissionBanner.render(g);" — renderiza ANTES do overlayG, direto no g = window-space ✓ ok.
WaveManager.render é chamado dentro do overlayG (linha 745) e usa scaledWidth=WIDTH*SCALE — com scale() global fica 2x quando janela>scaled?? NÃO: scaledWidth=scaled; window=scaled*f; overlayG.scale(f): elemento desenhado em x=scaled/2 aparece em scaled/2*f = window/2 ✓ correto.
FloatingText.render(g, SCALE) chamado em linha ~694: "FloatingText.render(g, SCALE);" — em g (window-space): x*SCALE=world*SCALE=window ✓ (sem câmera bug antigo mantido).
UI.renderOverlay(overlayG) usa screenWidth=WIDTH*s=scaled; com scale(f) → ok.
Menu.render(g): menu chama overlayG? verificar se Menu é desenhado no overlayG ou em g.

## ANÁLISE GAME OVER / transição
- drawGameOverActions/drawCenteredString usam coords scaledWidth/scaledHeight em overlayG ✓ com overlayG.scale(f) fica correto na janela.
- Fontes do game over/transição: "arial" 36/28/16/24/26/13 SEM *SCALE — fonte 36 no overlayG scaled (f>=1) = 36*f na janela; com AA ligado fica legível, mas em fullscreen o texto fica menor relativo à tela? Não: scaledWidth=1536 e window=1444 → f≈0.94 (mas max(1,.) → 1.0). f nunca <1 (max(1,...)). Em tela cheia 1536x864: f=1. OK.
- A reclamação do usuário sobre "qualidade ruim" do texto central: causada pela ausência de antialiasing (agora ligado) E o Game Over desenhado SEM AA no g direto (g.setFont/g.drawString com g original? drawCenteredString usa overlayG ✓; o 'g.setFont/g.setColor(Color.white)' afeta o overlayG também? g e overlayG são Graphics diferentes — g.setFont no g NÃO afeta overlayG... drawCenteredString seta a fonte no overlayG antes do drawString? Verificar drawCenteredString: faz g.setFont(font) com o Graphics recebido (overlayG) ✓.
- MAS linha 791: "g.setFont(new Font("arial", Font.BOLD, 36));" — define no g (buffer window) e depois drawCenteredString(overlayG...) define a fonte do overlayG. OK.
- A "qualidade ruim" do painel central (Game Over) na screenshot 2: texto sem AA + fonte arial minúscula desenhada em g do buffer sem AA. Com overlayG AA + métricas fracionárias: MELHOR. E as fontes: 36pt em overlay de 1536x864 = ok. Manter.
- "R — Comandante Ava" label do prompt do NPC (renderPrompt) — desenhado no mundo em g do buffer (buffer-space) ✓ ok (fixo ao mundo).
- Menu.render(overlayG): scaled ✓.
- renderInitialWeaponSelect(overlayG, scaledWidth, scaledHeight): scaled ✓.
TUDO COERENTE. Build + testes.
