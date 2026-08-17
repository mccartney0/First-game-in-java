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
