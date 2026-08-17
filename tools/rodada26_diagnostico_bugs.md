# Rodada 26 — Bugs HUD e mapa fechado (diagnóstico)

## Bugs reportados (screenshot do usuário, fase 2)
1. Painel do NPC: nome "Engenheira Nia" sobreposto à mensagem "Comunicação estabelecida. Cuide-se, piloto."
2. Missão (topo esquerdo) sobreposta ao "[SHIFT] Dash 0%".
3. Inventário (MedKit x3, Célula x7, Overclock x1) sobreposto no canto inferior esquerdo, junto com "VIDA/ESCUDO/MANA/CADEIA".
4. Mapa "torto" — proporção distorcida (tiles esticados) na tela cheia do usuário.
5. Fase 3: sala toda fechada (sem passagem).

## Bug 1 — nome sobre mensagem (DialogueManager.render)
CAUSA: nome desenhado em panelY + 26, e o corpo começa em lineY = panelY + headerY (headerY = 26). O nome ("Engenheira Nia", ~140px) e a primeira linha do corpo têm a MESMA baseline → sobreposição quando o texto tem mais de uma linha não — na verdade o nome e a 1ª linha colidem SEMPRE (mesma linha Y), pois o primeiro drawString do texto (drawWrappedLines) começa em lineY = panelY + 26 = MESMA coordenada do nome. CORREÇÃO: nome fica na linha do cabeçalho, corpo deve começar em panelY + 26 + ascent+descent (headerY deve incluir a altura da fonte do nome).

## Contexto técnico
- Buffer 384×216, Game.SCALE=4 (janela 1536×864); usuário vê 1913×1079 → tela cheia estica/distorce (16:9 vs 1913/1079=1.77). Verificar Game.render/fullscreen scaling.
- HUD missão: procurar "Missão" drawString em Game.java; inventário: procurar "MedKit"/"Célula"/"Overclock".
- level3.png deve existir em res/ (gitignore deixa level1-8.png versionados).

## Estado
- PR #40 mergeado na main (rodada 25). Branch atual: main.
- Próximo: branch rodada26, fixes HUD + verificação level3.

## Diagnóstico de código (após leitura)

### Bug 1 — nome sobre mensagem no painel do NPC (DialogueManager.render)
O nome é desenhado em `panelY + 26` (linha 333) e o corpo do texto em `lineY = panelY + headerY` onde `headerY = 26` (linha 292). NOME E PRIMEIRA LINHA DO TEXTO têm a MESMA baseline Y → colisão visível. CORREÇÃO: corpo deve começar abaixo do nome → `lineY = panelY + 26 + ascent` do nameFont (ex.: panelY + 44), e o neededHeight precisa incluir a altura do nome (+lineStep em vez de headerY=26).

### Bug 2 — missão sobre "[SHIFT] Dash" (MissionHud vs UI.drawAbilityHud)
MissionHud.card: `cardHeight = 26*s/4+6` (s=4 → 32px), título em margin+12*s/4+2=14, texto em margin+cardHeight-6=38. UI.drawAbilityHud em baseY = margin + statusHeight + 16 = 20+200+16=236 no painel expandido. NO screenshot, o usuário vê "[SHIFT] Dash 0%" logo abaixo do card da missão → drawAbilityHud NÃO é desenhado em baseY=236 quando overlay expandido? No screenshot: "Missão" em cima, "Neutralizar comandante..." e "[SHIFT] Dash 0%" grudados abaixo. O card tem cardHeight 32, então o dash aparece logo abaixo do card (margin=10+32+algum). Sugestão: MissionHud.render usa coordenadas de janela — o card e o drawAbilityHud conflitam quando NÃO expandido (drawAbilityHud pode estar sendo chamado em outro local). Verificar onde drawAbilityHud é chamado sem overlay expandido (possivelmente MissionHud o desenha? Não). Investigar.

### Bug 3 — itens sobrepostos (InventoryManager.render, linha ~270-300)
Barra de itens: cada item desenha chip fillRoundRect(x-2, y-10, 88, 18) e texto em (x+12, y), avançando `x += width + 22`. Parece espaçado. MAS no screenshot os 3 itens "MedKit x3, Célula x7, Overclock x1" aparecem SOBREPOSTOS no canto inferior esquerdo. Provável: em tela cheia, Game.SCALE muda ou os itens usam fonte fixa 11 sem SCALE → coordenadas x baseadas em "stringWidth + 22" com fonte fixa. No screenshot, todos os 3 estão amontoados. Também a HUD VIDA/ESCUDO/MANA/CADEIA fica sobre eles. CORREÇÃO: garantir largura mínima por chip e quebra para próxima linha se estourar, ou afastar da HUD (rodada 22d já moveu para acima da HUD — mas colide na tela cheia do usuário).

### Bug 4 — mapa "torto" na tela cheia
Buffer 384x216 (16:9 → na verdade 384/216=1.778). Tela do usuário 1913x1079=1.773 — quase igual. Mas no screenshot o jogo parece esticado/distorto. Verificar render em tela cheia: Game renderiza buffer para janela com Graphics.drawImage(buffer, 0, 0, screenWidth, screenHeight) → estica para o tamanho da janela. Se a janela não é exatamente 16:9, distorce. Fix: manter aspect ratio (barras pretas) OU ajustar janela para múltiplo exato do buffer.

### Bug 5 — fase 3 sem passagem
Verificar level3.png: todos os tiles são parede? Dump do mapa e verificar se existe caminho do spawn até anywhere (flood fill de chão). Pode ser que o nível esteja literalmente fechado, ou que o tile de spawn seja parede.

## Próximos passos
1. Corrigir DialogueManager: linha inicial do corpo = nome + altura.
2. Investigar drawAbilityHud fora do overlay expandido (onde MissionHud card se choca).
3. Corrigir barra de itens (largura mínima/quebra).
4. Verificar aspect ratio na tela cheia.
5. Analisar level3.png (flood fill) e corrigir/gerar mapa válido.

## Bug 2 — causa raiz CONFIRMADA
`UI.drawOverlayHint` (linha ~289-290) chama `drawAbilityHud(g2, 18, 44)` com o painel minimizado — as habilidades ficam no TOPO ESQUERDO, exatamente onde o MissionHud desenha seu card (margin=10, cardHeight=32 → termina em y=42). "[SHIFT] Dash 0%" em y=44+16=60 também colide. FIX: mover drawAbilityHud no modo minimizado para o CANTO SUPERIOR DIREITO (abaixo do card de score, baseX = screenWidth - scoreWidth - margin, baseY = margin + statusHeight + 16 = mesmo padrão do modo expandido). No modo expandido já está correto.

## Bug 5 — fase 3 CONFIRMADO: mapa dividido em 2 ilhas
Flood fill do chão preto do level3.png (36x24): 5 componentes — ilha A de 208 tiles, ilha B de 394 tiles (isoladas entre si!) e 3 tiles órfãos. O jogador spawnado numa ilha não alcança a outra → "sala fechada, não tem como passar". FIX: abrir corredores entre as duas ilhas no level3.png (converter paredes entre elas em chão preto em alguns pontos estratégicos). Preciso visualizar o mapa ASCII para escolher onde abrir.

## Bug 5 RESOLVIDO — level3.png
Causa: parede VERTICAL contínua em x=12 (y=1..22) isolava as salas esquerdas das centrais. As aberturas existentes nas linhas 6/12/18 (x=12 nessas linhas) conectavam fatias, mas a coluna continuava intacta nos trechos intermediários.
Fix (tools/fix_level3.py): abertos 4 corredores na coluna x=12 nas posições (12,4), (12,9), (12,15), (12,20). Resultado: componente de chão único (612 tiles + 3 órfãos esperados). Mapa agora totalmente navegável.

## Bug 4 — "mapa torto" na tela cheia: diagnóstico
Buffer 384x216 (1.778). Tela do usuário 1913x1079 (1.773). SCALE = min(1913/384, 1079/216) = min(4.98, 4.995) = 4 → canvas 1536x864, com letterboxing: sobra vertical (1079-864)/2=107px preta em cima e embaixo. MAS: no screenshot do usuário o jogo PREENCHE toda a janela sem letterboxing e parece "torto" (estirado). Causa provável: (1) o usuário pode estar usando o BOTÃO de maximizar do Windows (o listener trata como fullscreen e recomputeScale → SCALE=4, mas o listener NÃO limita a janela ao tamanho alvo no caminho maximized — ele só recomputeScale e return; a janela maximizada fica 1913x1079 com canvas 1536x864... não, drawImage usa scaledWidth=scaledHeight do canvas). Hmm — se o canvas é 1536x864 e a janela 1913x1079, o drawImage com NEAREST_NEIGHBOR estica o buffer 384x216 para 1536x864 (exato), com letterboxing nas bordas. No screenshot NÃO há letterboxing visível → o SCALE pode ter sido 5? 5*384=1920>1913 → SCALE=4. Ou o usuário está vendo uma janela antiga (antes do fix do modo janela que força targetW/targetH). A linha do screenshot: o jogo ocupa TODA a área do frame, incl. a faixa preta superior onde fica a barra de título... Não: a barra de título está visível. O jogo vai até a borda inferior direita SEM letterboxing. Isso significa que em algum fluxo o canvas ficou 1913x1079 — provável recomputeScale quando fullscreen/maximized com window 1913x1079: SCALE=4, canvas 1536x864, mas a janela não é reduzida. O drawImage desenha o jogo 1536x864 no canto (0,0) — sobras pretas embaixo/direita. No screenshot não vejo sobras... A imagem enviada tem o jogo cobrindo tudo. Diferença: a captura pode ter sido cortada.
DECISÃO: corrigir o caso "maximized sem F11": limitar a janela ao tamanho alvo (targetW/targetH) quando maximizada mas o SCALE não preenche a tela — OU aceitar letterboxing correto (que já existe). O problema visual real: ao maximizar, o listener não força a janela para targetW/targetH, então o jogo fica pequeno no canto ou esticado. Fix: no caminho maximized, após recomputeScale, forçar frame.setSize(targetW, targetH)? Isso quebra a experiência "tela cheia" do botão maximizar. Melhor fix robusto: no caminho maximized, recomputeScale; se scaledWidth/scaledHeight < área do monitor, centralizar o canvas (letterboxing) — O JOGO JÁ FAZ ISSO (offsetX/Y centralizam). Então o "torto" no screenshot deve vir de OUTRA coisa: overlayScaleX/Y quando windowWidth != scaledWidth ESTICA os overlays (HUD, texto) de forma anisotrópica (scaleX=1913/1536=1.245, scaleY=1079/864=1.249) — quase igual, ~0.3% de distorção. Desprezível.
SUSPEITA REAL: o usuário está rodando uma VERSÃO ANTIGA (sem o fix do modo janela) OU o frame pack() após toggleFullscreen gera tamanho não exato. Mas o screenshot mostra HUD nítida... A distorção percebida pode ser o jogo renderizado em NEAREST_NEIGHBOR com SCALE 4 em tela de 1913px (pixels do jogo 4x4 viram ~5px na tela) — normal, não é bug.
CONCLUSÃO PRÁTICA: o "torto" mais provável é que na tela cheia com resolução não-múltipla, os overlays sofrem estiramento anisotrópico PEQUENO — aceitável. Por segurança: melhorar: quando fullscreen/maximized, garantir que a janela seja do tamanho EXATO do canvas escalado (sem letterboxing esticado dos overlays) OU manter letterboxing mas SEM esticar overlays (remover o scale anisotrópico no fullscreen, mantendo-o só para redimensionamento manual de janela). Fix escolhido: quando fullscreen, a janela recebe setSize(targetW, targetH) e o conteúdo preenche 100% (sem overlayScale anisotrópico); maximização nativa continua tratando como fullscreen com recomputeScale, mas o listener agora TAMBÉM força a janela para o tamanho alvo quando o SCALE é inteiro e o canvas cabe na tela — evitando janela "grande demais" com jogo pequeno no canto.

## NOVO BUG (pós-merge rodada 26): missão "Defender o ponto / Localize o beacon do setor" NÃO avança mesmo com tudo limpo e NPCs falados
Sintoma: card da missão mostra "Localize o beacon do setor" (texto de fallback de getProgressText quando trackedBeacons vazio ou !spawned).
Fluxo da fase 2: DialogueObjective(Nia) → Sequence(HoldObjective, BossHuntObjective).
Hipóteses para investigação:
1. `onLevelLoaded` do HoldObjective pode não estar sendo chamado no fluxo real do jogo (World.restartGameCommon deve chamá-lo — verificar quem chama).
2. `findNearestFloorTile` pode falhar e retornar null em algum cenário (mapa carregado tarde?).
3. POSSÍVEL CAUSA RAIZ: `getProgressText` mostra "Localize o beacon do setor" quando trackedBeacons vazio || !spawned. Se o beacon programático foi criado mas o jogador NÃO o encontra, ok funciona. MAS o usuário diz que MATOU TUDO e FALOU COM TODOS → o Sequence só avança de Dialogue para Hold quando o DialogueObjective completa (falar com Nia). O objetivo mostrado é o TALKED→Hold. Se o jogador ainda vê "Localize o beacon", o Hold NÃO foi ativado OU o beacon não foi criado. Verificar: SequenceObjective avança estágios quando o interno completa? getProgressText do Sequence usa o estágio atual.
4. VERIFICAR QUESTBEACON.update: o jogador precisa PERMANECER no raio do beacon para ativar (activationProgress 180). Talvez a mensagem fique "Localize" porque spawned=false e o problema é que onLevelLoaded NÃO é chamado (quem chama? procurar "onLevelLoaded" no World.java).
5. Outro ponto: o teste Rodada25BeaconTest (16/16) criou o beacon programaticamente — mas o jogo real: o jogador pode estar na fase 2 COM o kill tracker zerando algo? Não relacionado.
PRÓXIMO PASSO: grep "onLevelLoaded" em World.java para ver quando é chamado; simular com probe o fluxo do jogo real (com player andando e inimigos).

## Notas do código atual (HoldObjective.java)
- update(): canal avança +1/frame se spawned e invaders==0; CHANNEL_MAX=600 (10s a 60fps).
- getProgressText: fallback "Localize o beacon do setor" quando trackedBeacons vazio ou !spawned.
- onLevelLoaded: reconnectRestoredBeacons primeiro; depois se level==2 cria beacon programático com findNearestFloorTile.
- serializeState: SPAWNED, CHANNEL, INVADERS, BEACONS (só não-ativados).

## ACHADO CRÍTICO da probe _ProbeHold (novo bug do usuário)
Fluxo: fase 2, falar com Nia OK, matar 11 inimigos OK. Beacon criado em (128,32) = tile (8,2)!
- Tile (8,2) é a região dos NPCs (Nia está em (3,2)?? na probe a Nia ficou em (480,192)=tile(30,12) — hmm mas (8,2)px=128,32).
- BUG VISÍVEL: o beacon é criado no TOPO do mapa (y=2), enquanto o jogador começa em (0,0) spawn (canto superior esquerdo) — o jogador PODE não saber onde está. Mas o pior:
- QuestBeacon.update: ativação exige o player COLIDIR com o beacon (isColliding). isColliding do Entity usa masks (maskx, mwidth, mheight)! Player pode ter mask zerado? Não — o jogo real funciona... MAS o jogador NUNCA ENCONTRA o beacon: a seta/targetHint "Beacon do setor" deveria guiá-lo. A missão mostra "Localize o beacon do setor" APENAS se trackedBeacons vazio || !spawned — mas aqui spawned=true, tracked não vazio → deveria mostrar "Permaneça junto ao beacon para ativar" (mostrou!). Então o card do usuário mostra "Localize o beacon do setor" → NO JOGO REAL trackedBeacons está VAZIO ou spawned=false!
- Ou seja: NO JOGO REAL o beacon NÃO é criado. No teste sandbox é criado (tile 8,2). Por quê no jogo do usuário não?
  - Possível: no jogo real do usuário, onLevelLoaded NÃO é chamado (jogo antigo? não, mergeado).
  - OU: QuestManager.getCurrentLevel() != 2 no momento do onLevelLoaded. Quando o jogo troca de fase, prepareForLevel(level) define currentLevel; onLevelLoaded chamado logo depois → level==2 OK.
  - OU: reconnectRestoredBeacons() retorna true (spawned && !trackedBeacons.isEmpty()) e o onLevelLoaded retorna ANTES de criar o beacon... MAS reconnect re-registra beacons existentes — se não há beacon ainda, tracked vazio → retorna false → continua → cria.
  - OU: findNearestFloorTile retorna null?? (sem chão no mapa inteiro — impossível, mapa tem chão).
  - OU: isValidTile/isWallTile lançam exceção silenciosa? Não.
  - VERIFICAR: no jogo real, o level2 do usuário pode ser DIFERENTE (o res versionado do sandbox pode não ser o que ele tem?). O sandbox level2 é 34x22 com paredes. O usuário compilou da main mergeada — mesmo repo.
  - IMPORTANTE: a imagem do usuário mostra o HUD "Defender o ponto / Localize o beacon do setor" e mapa verde! O level2 do sandbox é cinza/preto/branco?? A imagem tem chão VERDE (grass tiles). O level2.png do sandbox parece ter paleta diferente da renderizada na imagem do usuário... MAS a missão "Defender o ponto" com "Engenheira Nia" e tile de corredor cinza é a fase 2. O verde na imagem pode ser o tile de grama/grama.
- CONCLUSÃO mais provável: o beacon É criado no jogo real MAS... o getProgressText mostra "Localize o beacon do setor" só com tracked vazio. Então tracked vazio: onLevelLoaded NÃO rodou (level != 2) OU levelNumber na troca de fase era diferente.
- ALTERNATIVA forte: o usuário pode estar vendo o texto do DialogueObjective: getProgressText retorna "Fale com ..." se !talkedToTarget, senão delegate. "Localize o beacon do setor" é texto do HOLD. Se o Hold.getProgressText mostra isso, trackedBeacons.isEmpty() → beacon nunca registrado.
- Testar: no sandbox, a probe criou o beacon (tracked=1). No jogo do usuário, o jogo REAL usa o mesmo código. Diferença: o jogo real pode ter o level diferente (usuário na "fase 2" mas o nível interno é outro? ex.: modo procedural level 10?). Se level >= PHASE_TITLES.length → createObjectiveForLevel default? Verificar o default do switch! Se level 9+ usa HoldObjective de campanha?? Não — switch default verifica.

## Segunda passada de análise
O texto "Localize o beacon do setor" só existe em HoldObjective.getProgressText (linha 177), mostrado quando trackedBeacons vazio OU !spawned. A HUD do usuário mostra "Defender o ponto / Localize o beacon do setor" → o objetivo é o Hold da fase 2, mas o beacon nunca foi registrado no trackedBeacons no jogo real.

Hipóteses restantes para o jogo REAL:
(a) onLevelLoaded do Hold não criou o beacon: QuestManager.getCurrentLevel() != 2 (nível interno do jogo pode ser o índice do painel tático diferente — verificar o loadSlot com savedLevel).
(b) O beacon foi criado MAS Game.entities.clear() em algum ponto (troca de fase, menuReturnTimer, restart) removeu o beacon ANTES do registro... o onLevelStart do Hold limpa trackedBeacons! Se prepareForLevel(level) for chamado DUAS vezes (ex.: restart do loadSlot chama World.restartGameCommon que chama prepareForLevel, e depois restoreObjectiveState cria o beacon programaticamente via recreateRestoredBeaconsNow? Não — deserializeState não cria, apenas guarda posições; reconnectRestoredBeacons só re-registra beacons existentes).
(c) A causa (b) é a REAL no restart pós-morte: prepareForLevel cria NOVO HoldObjective; onLevelLoaded roda → cria beacon (tile 8,2 ou válido) e onBeaconSpawned registra → spawned=true. Depois placeStoryNpcs e load continua. OK funciona na probe! Mas o usuário morreu/reiniciou? A imagem mostra o jogo rodando... Se a sessão foi restartada (morte → continuar), o autosave tinha SPAWNED/CHANNEL; no reload: prepareForLevel → onLevelLoaded → reconnectRestoredBeacons → re-registra beacon existente → spawned=true → OK.
(d) FORTE: o tile (8,2) — onde o beacon vai no sandbox — é PERTO do topo do mapa. E o jogador aparece... Na imagem do usuário, o jogador está no meio do mapa e os inimigos estão espalhados; o card mostra "Localize o beacon". No sandbox level2 o beacon vai para (8,2)px(128,32) — topo esquerdo da sala. Se o jogador nunca olha pra cima-esquerda... mas o texto ainda seria "Permaneça junto ao beacon para ativar" pois spawned=true. SÓ apareceria "Localize..." se tracked vazio.
(e) VERDADEIRA SUSPEITA FINAL: no jogo do usuário (build Windows com a main mergeada), o level2.png REAL pode ter um layout diferente (mapa verde com grama!) — talvez o level2.png do usuário seja outro arquivo OU o usuário esteja em outro nível. A imagem mostra chão VERDE (tiles de grama), não o cinza/preto do sandbox. O repo versionado no sandbox pode ter level2 diferente! VERIFICAR: o level2.png do sandbox é mesmo cinza? dump anterior: paleta preto/branco/cinza/cor. A imagem do usuário tem grama verde 0xFF2E8B2E? checar hex. Se o level2 real do usuário tem grama, o World.isWallTile(isWallTile usa pixel branco 0xFFFFFFFF) e o chão verde é válido. OK.
(f) ALTERNATIVA CRÍTICA: no jogo real, o jogador MATOU TUDO incluindo os inimigos do tile do beacon?? Não afeta.
(g) A mais plausível agora: o jogador está vendo o fallback porque NO JOGO REAL o beacon não é criado porque isValidTile retorna false para o tile (17,11) e findNearestFloorTile retorna null OU o level 2 real (com mapa verde) tem TODO o raio 12 com invasores (o mapa verde tem inimigos em todo lugar)? findNearestFloorTile retorna fallback (primeiro chão encontrado) mesmo com invasores! Nunca retorna null se houver chão. Então spawned sempre fica true com o beacon criado.
→ Conclusão necessária: REPRODUZIR com dados reais. Adicionar logging no jogo real não é possível no sandbox. MAS há outra explicação: o jogador FALOU com a Nia (talkedToTarget=true), o delegate Sequence avança para Hold (idx=0), Hold getProgressText: trackedBeacons vazio → "Localize...". Tracked vazio acontece se onBeaconSpawned NUNCA foi chamado. O Beacon ctor chama registerBeacon via onBeaconSpawned... EXCETO se o Game entities.clear() ou um segundo prepareForLevel aconteceu DEPOIS da criação do beacon mas ANTES do registro?? onBeaconSpawned é chamado no ctor do beacon → imediato → tracked ok.
(h) ÚLTIMA HIPÓTESE plausível: o jogador viu "Localize..." porque o beacon criado foi REMOVIDO pelo kill tracker? EnemyKillTracker não afeta beacons. OU o beacon foi recolhido como QuestItem?? Não.
(i) REAL: ver se existe OUTRO lugar que limpa trackedBeacons: onLevelStart sim! Se o jogador passou de fase (BossHunt) e voltou?? Não.
(j) VERIFICAR NO JOGO REAL a ordem: World.restartGameCommon → prepareForLevel → new World → (construtor do World carrega mapa E chama applyMapPixels... onLevelLoaded é chamado DEPOIS de prepareForLevel, linha 357) → OK beacon criado. MAS: placeStoryNpcs (linha 363) pode recriar NPCs e... NÃO mexe com beacons. OK.
→ PRÓXIMO: verificar se o beacon NÃO é criado porque o jogo do usuário tem levelNumber DIFERENTE de 2 na fase 2 (o painel tático pode mapear fase 2 → nível 11?? verificar setCurrentLevel no painel tático e o level2.png usado).

## ACHADO: spawn do player no level2
O level2.png NÃO tem pixel 0xFF0026FF (player) → o jogador spawn em (0,0) = canto superior esquerdo = tile (0,0) que é chão preto no topo (linha 0 toda preta). O beacon programático vai para (8,2) = px(128,32) — a 140px do spawn. OK reachable.
MAS a Nia na probe ficou em (480,192) = tile (30,12)?? placeStoryNpcs reposiciona os NPCs para pontos temáticos! A Nia (engenheira) é movida para outro tile pelo StoryManager. E a tile (4,2) (102,187,106) no mapa é a position original.
IMPORTANTE para o bug real: o usuário diz "matei e limpei tudo e falei com todos". Ele fala com TODOS os NPCs (Nia + Ava + Rex/Lila/Finn). O diálogo termina... A fase 2 exige: falar com a Nia (talkedToTarget) → Hold → BossHunt (matar Warbringer). Se ele matou TUDO (incluindo o boss Warbringer em 17,10) ANTES de falar com a Nia, o BossHunt estava com boss já morto... sequence ok.
O beacon: no jogo REAL do usuário, após restart pós-morte, o estado salvo pode ter SPAWNED=true e BEACONS vazio (beacon ativado). No reload: prepareForLevel → onLevelLoaded → reconnectRestoredBeacons (não há beacon físico pois o mapa não tem) → tracked vazio → retorna false → cria beacon programático novo em findNearestFloorTile... MAS isEnemyInsideDefenseRadius verifica Game.enemies que AINDA não foi restaurado no reload?? ordem do loadSlot: restoreInimigosMortosSet → World.restartGame (cria inimigos) → placeStoryNpcs → restoreObjectiveState (deserialize recria beacon com posição salva — só se BEACONS não vazio). Se o save tinha beacon ativado, BEACONS vazio → beacon recriado no onLevelLoaded. OK.
→ PROBLEMA NO JOGO REAL: o usuário vê "Localize o beacon" com tudo falado → trackedBeacons vazio → o beacon não foi criado E o isComplete=false... VERIFICAR se existe cenário onde onLevelLoaded do Hold NÃO roda: o DialogueObjective.delegate.onLevelLoaded() é chamado por Sequence.onLevelLoaded que itera TODOS os stages (Hold e BossHunt) → Hold.onLevelLoaded roda SEMPRE. Então o beacon é sempre criado se level==2 e chão existe. CONCLUÍDO: level!=2 no jogo real do usuário neste momento OU ele está vendo o texto de uma fase diferente. MAS a missão é "Defender o ponto" — título do Hold! Então objective é Hold → level 2 ou 7. A fase 7 tem HoldObjective("Isolar o núcleo do Guardião", ...) — título diferente! Então é a fase 2 (level 2). → level==2 confirmado pelo título.
→ ÚLTIMA EXPLICAÇÃO: no jogo real do usuário, o onLevelLoaded do Hold roda MAS QuestManager.getCurrentLevel() != 2!! Quando isso acontece? Se prepareForLevel(level) usa levelNumber do parseLevelNumber("/level2.png")=2 → currentLevel=2. MAS: se o jogo foi carregado de save com savedLevel diferente (loadSlot define currentLevel via setCurrentLevel(savedLevel); restartGame parseLevelNumber(level2)=2 mas o prepareForLevel(2) recria o objetivo — MAS espere: o World.restartGameCommon chama prepareForLevel(levelNumber) com levelNumber=parseLevelNumber("/level2.png"). Se loadSlot chama restartGame("level2.png") → 2 OK.
→ TESTAR HIPÓTESE do jogo do usuário ser diferente: verificar quando getCurrentLevel() pode ser != 2 com título "Defender o ponto" — só se o DialogueObjective.getProgressText retornou delegate do level2 mas currentLevel mudou. prepareForLevel é chamado em Game.advanceToNextLevel (troca de fase) → se a troca de fase do level1→2 aconteceu e prepareForLevel(2) criou o objetivo, onLevelLoaded criou o beacon.
→ DECISÃO: criar teste reproduzindo o fluxo COMPLETO do jogo real (incluindo a troca de fase 1→2 como o jogo faz) e verificar se o beacon aparece no fim. Se o jogo real avança level 1→2 via painel, o prepareForLevel(2) cria Hold, onLevelLoaded cria beacon → deve funcionar. O bug real pode estar no onLevelLoaded rodando ANTES de applyMapPixels ter terminado?? Não — onLevelLoaded é chamado depois do construtor do World (que chama applyMapPixels).
→ SUSPEITA FINAL NOVA: o World.restartGameCommon na TROCA DE FASE real (não restart): Game.advanceToNextLevel → restartGameCommon(level+1, "/level"+level+".png") → prepareForLevel(2) → new World("/level2.png") → onLevelLoaded → beacon criado. No jogo REAL isso funciona? Sim na probe. Então o bug do usuário é OUTRA COISA: ele vê "Localize o beacon do setor" ANTES de falar com a Nia?? Não — DialogueObjective.getProgressText retorna "Fale com Engenheira Nia" se !talked. Ele diz que FALOU COM TODOS.
→ ALVO: testar o fluxo completo real 1→2 no sandbox (avançar de fase como o jogo) e verificar beacon. Se OK no sandbox, o bug é ambiente (o build do usuário pode estar DESATUALIZADO). Mas antes, verificar UM cenário plausível: o jogador MORRE na fase 2, o autosave grava SPAWNED=true BEACONS=vazio (beacon ativado), CHANNEL=300; no reload o beacon recriado em onLevelLoaded (tile 8,2) funciona. MAS: o jogador pode ter MORRE ANTES de falar com a Nia → save: TALKED=false, SPAWNED=false, BEACONS=| (beacon não-ativado) → restore cria beacon na posição (8,2) px=128,32... OK funciona.
→ PRÓXIMO PASSO: probe do fluxo real com troca de fase 1→2 (startNewGame flow) e testar também com morte+reload (SaveManager.loadSlot real).

## Resultado probe2 (fluxo real 1→2)
Fluxo real 1→2 FUNCIONA no sandbox: beacon criado (128,32), salvo, e preservado no reload (128,32). Progresso "Permaneça junto ao beacon para ativar". O bug do usuário (card "Localize o beacon do setor") só aparece quando trackedBeacons está VAZIO ou spawned=false.

Verificar o getProgressText exato do HoldObjective (linha 177) — pode existir cenário com spawned=false pós-reload.

## Cenário do BUG REAL identificado (análise do código do SequenceObjective)
SequenceObjective.onBeaconSpawned delega ao stage ATIVO (getActive()=stages.get(activeIndex)). Se activeIndex==1 (BossHunt, por exemplo se o Hold foi concluído antes), o beacon recém-criado NUNCA chega ao Hold (tracked vazio) → "Localize o beacon do setor" TRAVA PARA SEMPRE. MAS o usuário relata o contrário (matou tudo,Hold não concluiu)...

Na verdade o cenário do usuário: o boss do tile (17,10) pode ter sido destruído no início, e o jogador ativou o beacon... não importa. A DEFESA robusta: no onLevelLoaded do Hold, se o nível é 2 e (tracked vazio ou !spawned) mesmo após reconnect, recriar o beacon sempre. Implementar guarda:
if (level == 2 && (trackedBeacons.isEmpty() || !spawned)) { criar beacon } 
Isso elimina qualquer cenário de mission lock.

## STATUS FINAL da rodada 26 (beacon travado) — para retomada
O bug do card "Defender o ponto / Localize o beacon do setor" foi corrigido no `HoldObjective.onLevelLoaded` (rodada 26): se o nível é 2 e (trackedBeacons vazio OU spawned=false) após o reconnect, o beacon programático é SEMPRE recriado — guardando contra perda do registro (ex.: beacon entregue ao estágio errado da sequência, recarga com estado antigo, mapa sem tile livre).

Suíte nova: `tools/Rodada26BeaconLockTest.java` — 11/11 PASS (3 cenários: carga normal, morte+reload com canal preservado, estado corrompido BEACONS vazio).

Regressão: 31/33 OK na 2a corrida; `Rodada22bTest` e `Rodada26BeaconLockTest` flagrados como PROBLEM, mas ao rodar isolados ambos passam (13/13 e 11/11). O grep da regressão pega qualquer linha com "FAIL" — o Rodada22bTest imprime "nao" na linha do PASS 3 ("PASS missao ainda completa com canal zerado? (nao)") — SEM "FAIL". Verificar: o grep -qE "FAIL|Exception" pegou algo. Rodada22b isolado: ALL 13 PASSED sem FAIL. O PROBLEM na regressão foi FALSO POSITIVO de race/timeout? O BeaconLockTest rodou 3x isolado verde. Rodar de novo só o BeaconLock na regressão — se falhar de novo com 11 PASS, o grep está pegando algo no banner (MissionBanner.show pode imprimir? não). Hipótese: na regressão, o saves.json sujo de testes anteriores corrompe o cenário corrompido (corruptBeaconsInSave usa regex sobre arquivo sujo). Solução: limpar saves.json ANTES de cada teste na regressão.

Próximos passos: (1) garantir que cada teste da regressão limpa saves.json (ou aceitar false positive se isolados verdes); (2) commitar branch rodada26fix (não criada ainda — rodada 26 anterior usou branch rodada26, já mergeada PR #41; usar rodada26b); (3) PR e merge; (4) informar usuário.

Arquivos modificados na rodada 26b: `src/com/traduvertgames/quest/HoldObjective.java` (guarda onLevelLoaded), `tools/Rodada26BeaconLockTest.java` (novo). Arquivos temporários deletados.
