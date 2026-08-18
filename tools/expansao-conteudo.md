# Expansão de conteúdo (rodadas livres) — roteiro

## Esquema de pixels dos mapas (World.applyMapPixels, TILE_SIZE=16, PNGs em res/)
- `0xFF000000` (preto puro) = vazio/fora do mapa
- `0xFFFFFFFF` (branco) = WALL? NÃO — branco é... verificar; cinza `0xFF808080` = TILE_WALL
- `0xFF7CB342` = TILE_FLOOR (grama)
- `0xFF6D4C41` = TILE_FLOOR (lama/mud)
- `0xFFB0BEC5` = TILE_FLOOR (gelo)
- `0xFF0026FF` = água?
- `0xFFFF0000` = ? (ver linhas 118+)
- `0xFF9C27B0` = ? / `0xFF00BCD4` = ? / `0xFFFF6A00` = ? / `0xFF4CFF00` = ? (provável: spawn player, NPC, inimigo, item, etc.)

## Recursos atuais
- res/: level1-8.png, training.png (mapas PNG-painted), spritesheet.png (160x160), hurt.wav, music.wav, sounds/
- Mapas: training 28x18 tiles, level1 32x22, level8 46x30

## Plano de conteúdo novo
1. **Novo mapa level9.png** (bioma novo: floresta densa/ruínas) + integração na progressão (CUR_LEVEL=9)
2. **Novos sprites no spritesheet** (16x16 grid, 10x10) — itens coletáveis: poção de vida, item de XP, chave de missão
3. **Novo item coletável (Pickup)**: poção dropada por inimigos com chance
4. **Novo NPC e diálogo** (ex.: Curandeiro na floresta — cura por créditos)
5. **Nova side quest** simples usando o sistema QuestManager existente
6. **Testes JUnit** para o conteúdo novo
7. Commits frequentes por novidade

## Progresso
- [ ] Analisar applyMapPixels completo (todas as cores)
- [ ] Analisar spritesheet (onde adicionar sprites novos)
- [ ] Analisar QuestManager/SideQuests existentes
- [ ] ...

## Esquema completo de cores dos mapas (confirmado)
Entidades/pickups: FF1DE9B6=EnergyCell, FFFF5252=NanoMedkit, FF00E5FF=OverclockModule, FFFFC107=QuestItem, FF00ACC1=DataCore, FF4CAF50=QuestBeacon, FF795548=QuestNPC, FF00897C=ComandanteAva, FF66BB6A=EngenheiraNia, FF5E35B1=PesquisadorIvo, FFFF9800=ArmeiroMercúrio, FFFFB74D=EngineerNPC, FF7E57C2=ResearcherNPC
Inimigos: FF3F51B5=WARDEN, FF009688=SENTINEL, FFF4511E=RAVAGER, FFE91E63=WARBRINGER(boss), FF7986CB=OVERSEER(boss), FF81C784=PHANTOM, FFFF5722=GUARDIAN (boss fixo fase 7)
Tiles: FF808080=wall, FF7CB342=grass floor, FF6D4C41=mud floor, FFB0BEC5=ice floor, FF0026FF=água(?), FFFF0000=?, FF000000=vazio
Cores LIVRES para novo conteúdo (a definir no applyMapPixels): escolher cores não usadas, ex: FF8D6E63(?), FF26A69A etc. — verificar uso completo antes.

## Spritesheet (160x160, grid 10x10 de 16px) — células usadas
Linha 0: (0,0)=tile grama, (1,0)=tile parede, (2,0)..(5,0)=player walk (magenta), (6,0)=player espada? NÃO (2,0)-(5,0): player frames. Linha 1: (0,1)=poção/jeringa, (1,1)=espada cinza, (2,1)(3,1)=arma marrom frames, (4,1)=player, (5,1)=espada branca, (6,1)=(7,1)=player
Linha 2: (0,2)=npc magenta, (1,2)=player, (2,2)=bola azul (mana?), (3,2)=spider, (4,2)=?, (5,2)=player, (6,2)=player, (7,2)=(8,2)=player? (0,3)? (1,3)=esqueleto? (2,3)=branco?, (3,3)=espada, (4,3)=player, (5,3)=player... (0,4)=player, (1,4)=player
CÉLULAS LIVRES (muito espaço em branco): linhas 2+ colunas 4-9 parcialmente, linhas 3-9 maioria livres.
Área segura p/ novos sprites: índice (linha,col) com (2,7)-(2,9), (3,6)-(3,9), (4,2)-(4,9), (5,0)-(9,9) — ~45 células livres.
## Decisões de implementação
- Novos sprites em células livres, registradas em Spritesheet.java (ver como os sprites são indexados)
- Novo mapa level9.png: bioma selva/ruínas (usar mud FF6D4C41 + grass FF7CB342 + walls FF808080 + água FF0026FF)
- Novo inimigo? NÃO (muito acoplado) — melhor: novo item coletável via pixel novo?applyMapPixels já tem pickups; adicionar NOVO pixel + NOVA entidade Pickup subclass ou usar QuestItem existente com drop
- Novo NPC: usar pixel de cor livre no mapa + classe HealerNPC
- Side quest via QuestManager (ContactObjective com artefatos) — reutilizar QuestItem FF697C107 como artefato no novo mapa

## Indexação Spritesheet.java (confirmada)
- getSprite(x,y,w,h) em pixels: x=col*16, y=lin*16
- Usados (pixel coords): (96,0)=LIFEPACK?, (112,0)=WEAPON?, (96,16)=BULLET_EN, (112,16)=ENEMY_EN, (144,16)=FEEDBACK, (128,0)+(144,0)=GUN_LEFT/RIGHT, (112,16)-(128,16)=player sprites array
- Nota: grid 10 colunas = 160px; usadas até col 9 (144+16=160 OK)
- LIVRE: linhas >=2 colunas 2-9 (y>=32): ex. células (32,48) a (144,144) — ~60 células livres
- Novo sprite plano: colocar na linha 2+ do spritesheet via script Python
## Progresso real
- [x] Mapas: esquema de pixels documentado
- [x] Spritesheet: grid analisado, áreas livres identificadas
- [ ] Spritesheet.java não tem enums — adicionados via Entity/Player como constants (ex.: HEALER_EN)
- Próximo: (1) adicionar sprite curandeiro+árvore/poção nova; (2) novo mapa level9.png selva; (3) HealerNPC (cura por 50 créditos? ou grátis com cooldown); (4) novo item "Relíquia da Floresta" como quest artefato; (5) teste JUnit

## Estado atual (para retomada)
Análise concluída. Arquetipos: SupportNpcs.java (factory com InteractionListener; addWeaponEnergy, addMana, PlayerLifeAdd.apply), InteractiveNpc (constructor c/ falas + listener; finishInteraction marca diálogo; renderPrompt badge "R"). QuestManager: registerQuestItem/collectQuestItem/registerBeacon/activateBeacon/registerNpc/rescueNpc/notifyEnemyKilled/getCurrentObjective/notifyDialogueStarted/Finished. applyMapPixels mapeia cores para entidades; posso adicionar NOVAS cores:
- FFCDDC39 = HealerNPC (verde-lima) — NÃO usado
- FFA1887F = árvore/obstáculo decorativo? tiles já cobertos; melhor: usar pixel novo em Entity decorativa
- FFFF4081 = Relíquia (quest item especial com cor rosa)
Decisões finais (implementar nesta ordem, 1 commit por item):
1. HEALER (pixel novo + HealerNPC em SupportNpcs.healer): cura +50 vida por conversa (1x por fase, SaveManager already marks dialogue)
2. level9.png novo mapa (selva/ruínas ~40x26) com: healer, 2 NPCs apoio, quest item, spiders/wards, paredes, água
3. Integrar level9 no Game (switch CUR_LEVEL 9, parseLevelNumber já suporta?) — verificar prepareForLevel/getPhaseTitle
4. Sprite novo no spritesheet (healer) — célula livre (x=48,y=48)
5. Teste JUnit: HealerNPC cura / quest item coleta
6. Commits individuais + PR

## Decisão final (implementação)
Inserir FASE 9 "Vale dos Refugiados" entre Núcleo Central (8) e Modo Sobrevivência: MAX_LEVEL 8→9; PHASE_TITLES[8]="Núcleo Central", [9]="Vale dos Refugiados", [10]="Modo Sobrevivência"; grantCampaignReward continua em 8 (a fase 9 é epílogo de resgate com side quest). advanceToNextLevel: nível 9 → enterInfiniteMode (mesma lógica). level9.png: floresta/vale, NPC healer, 2 suporte NPCs, quest item + beacon (side quest de resgate), inimigos warden/sentinel, paredes de pedra, água.
Ordem de commits: (1) healer NPC + sprite; (2) level9.png; (3) integração fase 9 + side quest; (4) testes.

## Pontos exatos de edição no Game.java (confirmados)
- Linha 595: `questCompletedPending && CUR_LEVEL == 8` → mudar para `CUR_LEVEL == 9` (fim da campanha vira fase 9)
- grantCampaignReward (1651-1662): ajustar reward — fase 7 = VOID_MORTAR, fase 9 (fim) = DRONE_SENTINEL; manter fase 8 com +100 créditos sem arma? OU fase 8 reward = nenhuma arma (só créditos) e fase 9 = drone. Decisão: reward = (CUR_LEVEL == 7) ? VOID_MORTAR : (CUR_LEVEL == 9) ? DRONE_SENTINEL : null (fase 8 ganha só 100 créditos)
- QuestManager: MAX_LEVEL=8→9; PHASE_TITLES inserir [9]="Vale dos Refugiados", deslocar "Modo Sobrevivência" para [10]
- advanceToNextLevel (1809): > MAX_LEVEL → infinite; ok sem mudança se MAX_LEVEL=9
- onLevelLoaded/prepareForLevel: verificar quest setup por nível (QuestManager.prepareForLevel) — a fase 9 terá side quest de resgate (beacon + NPC resgate + quest item)
- parseLevelNumber/restartCurrentPhaseWithoutSave: usar CUR_LEVEL diretamente (linha 1534 usa Math.min(phase, MAX_LEVEL) — ok)

## Design da fase 9 "Vale dos Refugiados"
Side quest no estilo aventura: sequência simples
1. Falar com o Curandeiro Léo (healer) — NPC novo, dialogue + curar
2. Resgatar o NPC fugitivo (QuestNPC pixel FF795548) — RescueObjective existente!
3. Ativar beacon (QuestBeacon FF4CAF50) para abrir passagem → conclusão
Use RescueObjective para nível 9: prepareForLevel case 9 → new DialogueObjective(new RescueObjective(), "Curandeiro Léo")
Fim da campanha move para CUR_LEVEL==9 (linha 595), e advanceToNextLevel com CUR_LEVEL==9 → MAX_LEVEL=9, avanço p/ 10 → enterInfiniteMode (ok).
grantCampaignReward: level 7 = morteiro, level 9 (fim) = drone sentinela; level 8 = só 100 créditos.
PHASE_TITLES: [9]="Vale dos Refugiados", [10]="Modo Sobrevivência".

FloatingText.show(texto, x, y, cor, duração) confirmado — uso no healer está correto.

## PROGRESSO DA EXPANSÃO (atualizar antes de qualquer pausa)
1. DONE: SupportNpcs.healer() adicionado (linha 63-94 de SupportNpcs.java) — cura +60% vida, +20 escudo, FloatingText.show ok
2. DONE: World.applyMapPixels: pixel 0xFFCDDC39 → SupportNpcs.healer (linha 172-174 de World.java)
3. ERRO ATUAL: compileJava falhou — erro linha 89 SupportNpcs.java "cannot find symbol" (FloatingText.show?). Verificar: pacote de FloatingText = com.traduvertgames.entities.FloatingText (não graficos!). Corrigir: trocar com.traduvertgames.graficos.FloatingText por com.traduvertgames.entities.FloatingText
4. PRÓXIMO: corrigir → compile ok → commit 1 "healer NPC"
5. DEPOIS (commits seguintes): level9.png (script python gera PNG com pixels: player 0026FF spawn em área segura; healer CDDC39; apoio NPCs 66BB6A+5E35B1; QuestNPC 795548; QuestBeacon 4CAF50; QuestItem FFC107; spiders/inimigos 3F51B5/009688/3F51B5; walls 808080; grass 7CB342; mud 6D4C41; water 0026FF é player spawn — NÃO usar água; bordo preto 000000 fora do mapa), integração (Game.java linha 595 CUR_LEVEL==8→9; grantCampaignReward 1651 reward por nível; QuestManager MAX_LEVEL 8→9, PHASE_TITLES[9]="Vale dos Refugiados", case 9 = RescueObjective; título no banner)
6. Testes: adicionar MetagamePersistenceTest ou novo arquivo ContentExpansionTest (healer cura / pixel parsing via World? — World exige Game init; usar GameTestFixture.initHeadless)
7. Rodar ./gradlew check antes de qualquer PR; decisão: PR único "Rodada 30: expansão de conteúdo" no final
8. DONE: level9.png gerado (48x32 via tools/make_level9.py) e verificado visualmente — spawn NW, healer em alcova, vale de lama com NPCs/beacon/itens, Wardens norte, Sentinels vale, Ravagers sul
9. DONE: integração fase 9 — QuestManager (PHASE_TITLES[9], case 9 = DialogueObjective(RescueObjective, "Curandeiro Léo"), case 10 = NullObjective; objectiveForLevel() público novo), Game.java (MAX_LEVEL=9, fim campanha CUR_LEVEL==9, grantCampaignReward fase 9 = DRONE_SENTINEL, fase 8 = só créditos com guard null), StoryManager (título/lore/NPCs fase 9)
10. DONE: ContentExpansionTest.java — 7 testes (fase 9 title/objective, MAX_LEVEL, healer, lore, créditos); 28 testes totais passando (./gradlew test)
11. PRÓXIMO: playthrough da fase 9 com GameDriver (validar mapa carrega, healer R interage, QuestNPC resgate, beacon) → commit → push → PR #46 "Rodada 30"

## Playthrough fase 9 — detalhes técnicos (11)
- saves.json: root {activeSlot:1, bestRun:{}, upgrades:{}, slots:[{id:1, name:"Slot 1", session:{level:9, vida:120, mana:200, escudo:50, arma:0, armaAtual:0, armasDesbloqueadas:1, pontuacao:0, recorde:0, melhorCombo:0, melhorComboSessao:0, inimigosMortos:0, inimigosMortosSet:[], npcs:{}, beacons:[], sideQuests:{}, sideQuestsDone:{}, inventario:{}}}]}. Gerado por tools/make_save_level9.py
- Load do jogo: menu principal → navegar setas até "CONTINUAR" (carrega slot ativo 1) → Enter. O jogo abre direto no level9.png se saves.json existe com level=9.
- Janela: W=$(xdotool search --name "Game 2 RPG" | head -1); rodar DISPLAY=:0 java -cp bin:res com.traduvertgames.main.Game &> /tmp/game.log &; matar com pkill -f com.traduvertgames.main.Game antes de reabrir.
- GameDriver (/tmp/GameDriver.class, sintaxe: click X Y | tap TECLA | hold/TECLA | wait MS). Teclas: WASD, X atirar, Space pulo, Shift+direção dash, R interagir NPC, T save, P pausa, Enter confirmar, setas menu, ESC menu.
- Posições do level9.png (48x32 tiles, 16px): spawn NW (2,2)-(4,4) → player começa em ~ (48,48); healer em (6,6)=tile 96-112px; muros destrutíveis y=10 e y=21 com aberturas x=16,17,30,31; vale lama y=12-20; QuestNPC líder (24,15)=tile (384,240); beacon (26,16)=(416,256); itens (12,15),(34,14); LifePack (18,18),(14,26),(30,27); Wardens norte y=4 e y=7; Sentinels y=13,17; Ravagers y=26,28 sul.
- Câmera segue player; jogo 768x448 (Game.WIDTH/HEIGHT x SCALE). Tela do jogo: 768x448 px.
- Objetivo fase 9: DialogueObjective(RescueObjective, "Curandeiro Léo") — banner diz "Fale com Curandeiro Léo"; depois resgatar QuestNPC (colisão) e ativar beacon (E? — verificar QuestBeacon interação, provavelmente R ou colisão).
- Fase 9 completa → questCompletedPending → cutscene vitória → modo sobrevivência (infinite).

## Problema no playthrough (2): "Continuar" carregou fase 1 (training-like grama), não fase 9! HUD mostra "Missão Destruir o Núcleo da IA / Fale com a Comandante Ava" = fase 1. Vida/mana restaurados do save (120/196, 200/600, escudo 43) — o save foi lido, mas o level carregado foi 1. Investigar: talvez restoreObjectiveState force prepareForLevel(1) ou saveCurrentGame grava nível real ao carregar; ou "Carregar jogo" escolhe slot e Continuar usa melhor run (level 1). Solução: usar "Carregar jogo" no menu (escolher slot 1) ou verificar como Continuar escolhe nível.

## Diagnóstico playthrough (3): o save NÃO foi carregado
Janela voltou ao menu principal com level1.png no fundo e saves.json intacto (632 B). Load provavelmente falhou silenciosamente (findSlot retornou slot mas restauração falhou OU saveExists=false). Causa provável: o menu mostra jogo em fase 1/PAUSE? Não — Menu.pause=false no main menu e handleMainMenuSelection exige saveExists. hasAnySave checa "vida" ou "level" na session — save tem ambos. Mas o screenshot3 mostra menu com seleção em Novo jogo — o Continuar foi pressionado antes do save ser persistido? Não, save já existia. Hipótese mais forte: loadSlot retornou false porque activeSlot=1 mas o slot encontrado não passou em algum toDouble? Não lança. Melhor: adicionar System.out.println no load para debug OU usar abordagem alternativa: pressionar F5 (save/load rápido?) — verificar atalho F5. Alternativa: modificar Menu para debug via log. VERIFICAR: talvez o jogo tenha reescrito saves.json ao abrir (loadRoot...?). salvar arquivo de debug: rodar java com -ea e capturar log — ativar print em SaveManager.loadSlot: adicionar println temporário e recompilar (não commitar).

## Diagnóstico playthrough (4): mesmo com save flat, HUD continua "Nível 1 — XP: 0/40" e missão "Fale com a Comandante Ava". Vida 120/196 e mana 200/600 confirmam que loadSlot aplicou os valores (save lido!). Então loadSlot rodou, mas restartGame("level9.png") NÃO recarregou? Ou Game.getCurrentLevel() retorna 1. Investigar: em loadSlot, o caminho que executa é o "game != null" (linha 530: game.setCurrentLevel(savedLevel) + World.restartGame). Mas talvez o fluxo tenha ido pelo branch de linha 591 (sem game) e aí seta level via loadLevelPlus... Não, HUD mostra vida do save. SUSPEITO FORTE: restartGame cria NOVO Player e World — mas se o load roda fora da thread de loop ou dentro dela com problemas... Melhor abordagem de debug: verificar Game.getCurrentLevel() após load via reflexão não possível. Alternativa: o jogo pode ter save automático T que sobrescreve; ou o problema é que o jogo já estava rodando quando criei o saves.json e a primeira leitura já havia falhado? Não.
NOVA HIPÓTESE: saveCurrentGame/loadSlot usam Game.CUR_LEVEL (int static) vs getCurrentLevel() — verificar getCurrentLevel() implementação. Se getCurrentLevel() retorna outro campo, setCurrentLevel(savedLevel) pode não atualizar o valor exibido.

## Diagnóstico playthrough (5): decisão de debug
setCurrentLevel atualiza GameState.currentLevel e CUR_LEVEL — correto. A vida/mana do save foram aplicadas (HUD mostra 120/196, 200/600). Então loadSlot rodou. Por que nível 1? Possível: GameState.currentLevel inicializado em 1 e loadSlot setou MAS prepareForLevel(1) chamado depois? Ver linha 530-540: game.setCurrentLevel(savedLevel) dentro de um if... Verificar qual branch (com/sem game) o load tomou. Se game != null (instância existe), roda o branch completo; senão o branch alternativo na linha 591. Vou criar um teste JUnit rápido (TempLoadLevel9Test) que: cria Game, chama loadSlot(1), verifica Game.getCurrentLevel() == 9. Se falhar, o bug está no load; se passar, o problema foi timing/UI na sessão.

## Diagnóstico playthrough (6): loadSlot FUNCIONA
TempLoadLevel9Test PASSED — loadSlot(1) carrega a fase 9 corretamente. Logo o problema no playthrough foi o estado da sessão do jogo: o jogo rodava ANTES do saves.json existir com level 9, e ao pressionar Continuar com o mouse/driver talvez a seleção tenha caído em outra opção, OU o jogo tinha save automático T que regravou level 1. Solução: no driver, pressionar ESC (menu), navegar até Continuar, Enter — MAS confirmar visualmente a seta ">" antes. Também possível: a primeira abertura com saves.json ausente criou o jogo em level 1 e o save de level 9 foi sobrescrito pelo auto-save T? Não, o driver não pressionou T. Verificar: screenshot2/3 — o menu mostrava ">" em "Novo jogo"! O driver enviou DOWN ENTER mas talvez o foco do driver/RawInput tenha ido ao jogo (keydown no canvas). No screenshot3 o menu mostrava seleção em Novo jogo (seta "> Novo jogo"). Ou seja: o DOWN não moveu a seleção (o jogo capturou a tecla?). Solução robusta: usar click no texto "Continuar" do menu, OU esperar e reenviar DOWN com pausa maior.

## Diagnóstico playthrough (7): click 770,633 acionou "Novo jogo" (onboarding de arma)
O clique com xdotool/Robot usa coordenadas da JANELA, e o窗口 é 1536x864 mas a captura retornou 1279x864 (escala X11 diferente!). As coordenadas precisam ser mapeadas à escala real da janela (clickAt usa robot.mouseMove absoluto — o jogo usa mouseMoved? verificar GameDriver.clickAt). Para o menu de teclado, usar DOWN/ENTER com pausas longas e confirmar a seta ">". MELHOR PLANO: matar o jogo, recriar saves.json, reabrir, esperar 5s, screenshot, validar seta em "Novo jogo", enviar DOWN 1x, screenshot, validar seta em "Continuar", Enter, screenshot final.

## Playthrough fase 9 — estado e plano (resumo consolidado)

Testes da expansão OK (28 testes passando). Integração fase 9 commitada (d0eabb0, 5bbeb2a, 9a2f451, e2343ca). Playthrough em andamento:

1. saves.json da fase 9: tools/make_save_level9.py (formato FLAT: chaves direto no slot {id:1, level:9, vida:120, mana:200, escudo:50, arma:0, armaAtual:0, armasDesbloqueadas:1, pontuacao:0, recorde:0, melhorCombo:0, melhorComboSessao:0, inimigosMortos:0, inimigosMortosSet:[], survivalRecord:0}). loadSlot funciona (TempLoadLevel9Test PASSED — pode excluir depois).
2. Game aberto (PID ~20579, window 18874375, 1536x864; import retorna 1279x864). Menu mostra seleção "> Novo jogo" em y≈633 (1279-wide coords); "Continuar" em y≈663.
3. Problema anterior: DOWN via GameDriver não moveu seleção (jogo capturou tecla? ou timing) e click 770,633 acionou "Novo jogo" (abriu onboarding de arma — ESC volta ao menu; ou clickAt usa coordenadas ABSOLUTAS do screen: janela em x=2,y=111 → corrigir: x_abs = 2+x_rel, y_abs = 111+y_rel; para "Continuar": x_abs≈772, y_abs≈774).
4. Plano: screenshot /tmp/menu_a.png (já salvo); validar seta; mover para Continuar (DOWN 1x); confirmar seta em Continuar; Enter; screenshot; validar HUD: título "Vale dos Refugiados", banner "Fale com Curandeiro Léo", player no canto NW.
5. Depois: mover player até healer (tile 6,6 → mapa 48x32, tile16px; câmera segue player; deslocar D + S) e interagir R; depois resgatar líder (tile 24,15) via colisão; depois beacon (tile 26,16) ficar parado ~3s (channel 180 ticks); validar conclusão da fase → cutscene vitória → modo sobrevivência; salvar screenshots da validação (não precisa commitar imagens).
6. Depois: limpar temporários (TempLoadLevel9Test.java, src/tools/MakeLevel9SaveHelper.java, writeDebugSave no SaveManager) → rodar ./gradlew check → push → PR "Rodada 30: expansão de conteúdo — fase 9 Vale dos Refugiados" (--base main).
7. Commits pendentes pós-playthrough (opcional, se der tempo): balancear créditos fase 8/9, novo inimigo elite fase 9, diálogos extras.

## Playthrough (8): clicks acionaram "Novo jogo" duas vezes
O clickAt do GameDriver usa robot.mouseMove(x,y) em coordenadas ABSOLUTAS da tela. A janela do jogo fica em (2,111). O clique em (774,774) caiu dentro da região do menu? "Novo jogo" ocupa y=111+615..645 na tela (617-647). Meu y=774 deveria cair em "Continuar" (728-757)... mas acionou Novo jogo de novo. Ou o menu capturou o clique em qualquer lugar visível? Não — parece que o clique em tela acionou o canvas (mouse listener do jogo chama new game?) ou o import mostra screenshot anterior. DECISÃO: abandonar cliques. Voltar ao menu (ESC do onboarding) e usar TECLAS com timing maior: enviar DOWN (delay 300ms), screenshot, enviar DOWN se necessário, Enter. Se DOWN não funcionar (jogo captura com RawInput?), tentar setas DOWN do menu são processadas em menu.update. Alternativa nuclear: apagar saves.json não; melhor: usar T no jogo? Não. Se as teclas DOWN não moverem a seleção, o problema é que o foco da janela não está no jogo (robot envia para a janela ativa — jogo é ativo).

## Playthrough (9): teclas do driver NÃO chegam ao jogo
Pressionar ESC (via robot keyPress) não saiu da tela "Escolha sua arma inicial". O tap ENTER também não confirmou antes (o jogo ficou na mesma tela). Ou seja: as teclas AWT do GameDriver não estão sendo recebidas pelo jogo em execução. Possíveis causas: (a) a janela não está ativa/foi clicada após o keyPress; (b) robot.keyPress envia para o display mas o jogo usa algum listener AWT que exige foco; (c) o Xvfb/Xtest pode não estar roteando para a janela certa. CORREÇÃO CONHECIDA: antes de keyPress, chamar robot.mouseMove para dentro da janela (dá foco à janela) OU usar xdotool key que usa XTEST direto. Testar: xdotool key --window WID Escape.

## Playthrough (10): nem xdotool --window nem robot.keyPress funcionam
A tela "Escolha sua arma inicial" persiste. O canvas do jogo provavelmente usa java.awt.Toolkit.getDefaultToolkit().addAWTEventListener? Não — usa KeyAdapter no Canvas, que recebe eventos via focus do AWT. xdotool --window deve injetar via XTEST... mas parece não funcionar. Testar: xdotool key sem --window (evento para a janela ativa do XTEST após activate). Se ainda falhar: injetar input via java.awt.Robot de DENTRO do processo do jogo não é possível (processo separado). ÚLTIMA OPÇÃO: modificar temporariamente o Game para ler comandos do System.in em uma thread (debug mode), recompilar, rodar e enviar linhas. Isso funciona 100%. Como é temporário, remover depois.

## Playthrough (11): PLAN DE EMERGÊNCIA — InputBridge via System.in
xdotool key (com/sem --window) e robot.keyPress NÃO funcionam. O jogo ignora todos os eventos de teclado injetados. Causa provável: a janela do jogo usa Canvas que recebe KeyEvents apenas quando tem foco de teclado do AWT; XTEST events geram FocusIn/WindowFocus mas talvez a aplicação intercepte com keyPressed e o evento chega, porém o onboarding "Escolha sua arma inicial" processa setas/Enter/ESC no Game.update — verificar Game.java: grep "Escolha sua arma" para ver como o input é tratado (pode exigir "armaInicial" flag; talvez o onboarding exige Enter e ignore ESC!). Verificar CÓDIGO primeiro! Se o onboarding processa Enter e Enter não funciona... talvez o jogo esteja TRAVADO/loop não processa? Screenshot atualizado mostra HUD sem VIDA — está no onboarding. Verificar: grep -n "initialWeapon\|Escolha sua arma" src/com/traduvertgames/main/Game.java. Se o código processa via Game.keys[] polled (raw polling com KeyListener), XTEST DEVERIA funcionar... a menos que o display :0 esteja em outro screen ou o import capture de janela antiga. VERIFICAR WID: rodar xdotool search --name "Game 2 RPG" de novo.

## Playthrough (12): robot click+ESC também não funciona
Todos os métodos de input externo falharam (xdotool com/sem --window, robot press+release). O jogo continua renderizando (screenshot atualizado em tempo real). Causa mais provável: o Canvas perdeu o foco de teclado do AWT e os eventos injetados não chegam ao KeyListener. SOLUÇÃO DEFINITIVA: adicionar bridge de input via System.in no Game (modo debug temporário): thread daemon lendo linhas do stdin e simulando keyPressed com os mesmos keyCodes. Comandos: UP/DOWN/LEFT/RIGHT/W/A/S/D/X/SPACE/ENTER/ESC/T/R. Rodar o jogo com nohup ... < /dev/null e enviar comandos via echo para /proc/PID/fd/0? stdin é /dev/null. Alternativa: FIFO — rodar java com stdin = mkfifo pipe e enviar "ENTER" > pipe. FÁCIL: substituir nohup por: (sleep infinity; ) | java ... não. Usar coproc: mkfifo /tmp/gamein; java -cp bin:res com.traduvertgames.main.Game < /tmp/gamein & e echo "ENTER" > /tmp/gamein. O loop System.in.read pode bloquear mas em thread daemon é ok.

## Playthrough (13): stdin bridge adicionado; jogo reiniciado com FIFO /tmp/gamein
O ESC foi enviado, mas a tela ficou preta (g1/g2). Verificar /tmp/game.log e ps. O ESC durante a inicialização pode ter ativado onboarding skip → tela preta do onboarding? Verificar log.

## Playthrough (14): TCP bridge FUNCIONA! /tmp/gamedrv (PRESS/RELEASE/wait)
Jogo reiniciado com bridge TCP na porta 10445 (startStdinBridge no main). Driver: /tmp/gamedrv "PRESS X" "wait N". Menu aberto, seta em "Novo jogo". PRÓXIMOS PASSOS: (1) PRESS DOWN → seta em Continuar; (2) PRESS ENTER; (3) validar HUD fase 9 "Vale dos Refugiados" + banner "Fale com Curandeiro Léo"; (4) mover player: hold D + hold S ~2s cada (câmera segue; mapa 48x32 tiles; healer tile (6,6), líder tile (24,15), beacon (26,16)); (5) R perto do healer (interação) → diálogo de cura; (6) mover ao líder (D), colidir resgate; (7) beacon (D), esperar ~3s no canal; (8) cutscene vitória → modo sobrevivência. Screenshots em /tmp/v*.png para evidência.

## Playthrough (15): DOWN não moveu a seta do menu
Estado atual (g6.png): menu principal ainda com seta "> Novo jogo" após PRESS DOWN via bridge. O Menu.processInput pode exigir keyReleased (menu.down = true no PRESS é processado no update, mas precisa de flag reset — o update consome menu.down uma vez por frame). PROBLEMA PROVÁVEL: o down do Menu exige PRESS+RELEASE ou o update rodou mas a lista de opções tem 6 itens e o DOWN do Menu incrementa currentOption com debounce (waitTime). g5 não foi checado. Solução: enviar PRESS S seguido RELEASE S (movimento + release) com 1s entre; ou usar DOWN do teclado do Menu diretamente. Alternativa: navegar pelo menu com "PRESS A/D"? Menu usa W/S e setas. Ver Menu.update para ver como menu.down é consumido e qual a seleção inicial.

## Playthrough (16): Menu.update consome menu.down 1x por frame (flag->false)
O DOWN deveria ter funcionado: menu.down=true no PRESS, update seta down=false e moveSelection(1). Possível razão da falha: o menu.update só roda quando gameState=="MENU" (ok) mas o PRESS DOWN do Game.keyPressed também setou player.down=true, e o Menu pode estar em Screen LOAD? Não, MAIN. O saveExists pode estar false e "Continuar" desabilitado? (handleMainMenuSelection checa). VERIFICAR g5.png antes de concluir.

## Playthrough (17): DOWN não moveu seta — investigar chegada do comando
g5 mostra seta ainda em "Novo jogo". Hipóteses: (a) bridge TCP não recebeu (nc fecha rápido, server.accept em loop deveria pegar); (b) keyPressed no Menu exige menu.down no estado MENU mas há algo no middle — por exemplo ESC abriu pause? Não. (c) O onboarding está ativo (OnboardingManager.isActive()) e o input do jogo é consumido por onboarding antes do menu? VERIFICAR: Game.keyPressed para DOWN com showInitialWeaponSelect return — onboarding. Se OnboardingManager.isActive() durante MENU, o SPACE skip, mas DOWN no Game: onboarding? Ver OnboardingManager: se ativo durante menu, o DOWN navega onboarding? Checar OnboardingManager.keyDown.

## Playthrough (18): jogo atual roda bin SEM bridge TCP (PID 21142)
A porta 10445 não está ouvindo. O jogo atual é a versão antiga. Plano: pkill, ./gradlew compileJava, reiniciar java > /tmp/game.log, verificar porta 10445 ouvindo, usar /tmp/gamedrv com nc reinstalado OU substituir gamedrv para usar /dev/tcp.

## Playthrough (19): porta 10445 não abre após recompilação
PID 21377, jogo roda, mas a bridge TCP não abre a porta. O catch ignora IOException. Possível causa real: SecurityException? Improvável. Ou o código compilado não contém startStdinBridge? Verificar: javap -c bin/.../Game.class | grep -i bridge. Se presente, adicionar System.err.println no catch para debug.

## Playthrough (20): DESCOBERTA — o Gradle compila para build/classes/java/main, NÃO para bin/!
bin/Game.class tem timestamp antigo (02:01). O jogo rodando usa classes velhas! Por isso nada dos meus novos features estava em jogo. SOLUÇÃO: rodar java -cp build/classes/java/main:res com.traduvertgames.main.Game. O bin/ pode estar vazio ou desatualizado — verificar o que existe em bin/.

## Playthrough (21): FUNCIONANDO — porta 10445 ouvindo, classes 03:58
Jogo correto rodando (PID 21464, classpath build/classes/java/main:res). Agora refazer: PRESS DOWN → Continuar → ENTER → carregar fase 9. IMPORTANTE: atualizar /tmp/gamedrv se usar nc (não existe) — usar /dev/tcp.

## Playthrough (22): FASE 9 CARREGADA COM SUCESSO!
HUD: "Missão — Evacuar sobreviventes / Fale com Curandeiro Léo". Banner central: "MISSÃO — Fale com Curandeiro Léo para iniciar (tecla R próximo a ele)". NPC amarelo (CDDC39) com label "R — Curandeiro Léo" visível à esquerda (~400,415 na tela). Player em (650,560) — sprite marrom/capuz. Inimigos (spiders pretas) espalhados. Vida 120/214, Escudo 37/195. PRÓXIMOS PASSOS: (1) mover player até o Léo: segurar A+W ou A (o Léo está em x≈400,y≈415; player x≈650,y≈560 — mover A + W); (2) PRESS R; (3) ENTER para conversar; (4) depois ir ao líder resgatável e ao beacon; (5) finalizar fase → cutscene → sobrevivência.

## Playthrough (23): player moveu mas ficou a ~80px do Léo; tomou dano de spiders (vida 71/214, escudo 0)
Player em (658,370); Léo em (420,295). Falta mover A+W de novo (~150px ≈ 0.7s). Banner de missão sumiu (consumido). O R não interagiu (fora do range). Próximo: PRESS A PRESS W wait 900 RELEASE; depois PRESS R; depois ENTER (se abrir diálogo). CUIDADO com spiders — podem dar dano no caminho; se morrer → game over, usar save/restart.

## Playthrough (24): vida crítica 8/214 — spiders derrubaram o player no caminho
Mover sem atirar é suicídio. ESTRATÉGIA: atirar (PRESS X, ~1.5s) na direção das spiders para limpar o caminho até o Léo. O tiro vai para o lado que o player olha (último movimento). Atirar enquanto A+W mantém direção NW. Se morrer → game over screen → opção de voltar ao save (gameOverSelection=0). O save existe (fase 9), então basta ENTER no game over.

## Playthrough (25): menu principal reabriu (seta em "Sair")
O PRESS R abriu o menu (não interagiu com o NPC — player longe). Menu aberto com seta em "Sair" — CUIDADO com ENTER! Voltar ao jogo: PRESS ESC (fecha menu/pausa) ou navegar para cima e ESC. Melhor: ESC direto (Menu.escapeFromCurrentScreen fecha).

## Playthrough (26): menu PRINCIPAL preso (não é pausa!) — o R abriu o menu principal, não a pausa
Estado: menu principal aberto com seta em "Sair"; 2 ESCs não fecharam. Causa provável: o PRESS R chegou quando gameState="MENU" e ativou menu.escape? Não — o menu.showMainMenu foi chamado de algum lugar: talvez PRESS X abriu menu? Não. O menu principal reabriu provavelmente porque o PRESS R com "gameState MENU" foi tratado como escapeFromCurrentScreen→MAIN... A tela segue idêntica após ESC: o menu.escape=true é consumido e escapeFromCurrentScreen no MAIN talvez reabre o MAIN (loop!). VERIFICAR Menu.java escapeFromCurrentScreen para Screen.MAIN.

### Situação do jogo:
- Jogo roda (PID ~21464, bridge TCP 10445 ouvindo, /tmp/gamedrv funciona via /dev/tcp)
- Fase 9 carregada no save: missão "Fale com Curandeiro Léo" funcionando
- Player estava em (658,370), Léo em (420,295); vida 8/214 (spiders derrubam rápido)
- Salvar a vida: se morrer, game over → ENTER escolhe "Voltar ao último save" (gameOverSelection=0) → salva
- Estratégia restante: fechar menu (ESC até funcionar ou PRESS UP 6x até "Novo jogo" e ESC), matar spiders com X, aproximar do Léo, R, ENTER; depois líder + beacon; validar fim de campanha.

## Playthrough (27): diagnóstico do menu preso
ESC no MAIN só fecha se pause=true — o menu foi aberto SEM pausa (algum código chamou Menu.showMainMenu/abriu menu principal direto), então ESC não fecha. SOLUÇÃO: navegar (PRESS UP x6 ou DOWN x2) até "Continuar" e ENTER → loadSlot → jogo volta. O save da fase 9 será recarregado (vida 120/214 original do save).

## Playthrough (28): jogo recarregado na fase 9 — vida 120/214, missão ativa
Léo (amarelo) em (400,350) com label "R — Curandeiro Léo". Player em (658,560). Próximo: mover A+W ~800ms (diagonal NW), depois R. Atirar se spider se aproximar (X).

## Playthrough (29): ainda longe do Léo
Player (658,540) → Léo (415,400): delta (-243,-140). Movimento diagonal 800ms ≈ 240px → deveria chegar. Não chegou: talvez A+W juntos movem apenas 1 eixo por frame (diagonal reduz). Fazer movimentos alternados: A 500ms, W 400ms. Depois R. Vida 56 — se spider atacar de novo, considerar usar PRESS S para fugir.

## Playthrough (30): PRESS R abre o menu principal — bug no key mapping do R
Segundo PRESS R (player perto do Léo, sem banner) abriu o menu principal de novo. Investigar: R pode estar mapeado para Menu.openPauseScreen? No Game.keyPressed, buscar VK_R. Também confirmar se a interação com NPC exige estar olhando para o NPC (direção do player) — talvez precise ficar embaixo do NPC olhando para cima.

## Playthrough (31): R funciona (DialogueManager.startNearestDialogue) mas menu abriu
Hipótese principal: o "menu aberto" foi o load do save ainda em andamento — ou o PRESS R chegou quando gameState era MENU. Adicionar log à simulateKey: imprimir gameState no momento do comando. E verificar se startNearestDialogue falha quando o NPC está atrás de paredes (o Léo tem 2 paredes marrons ao lado).

## Playthrough (32): fase 9 carregada (m15), vida 120/214. Spider em cima do Léo (370,300)!
Player (658,560). Léo (400,415) com spider em (370,300) logo acima. Plano: (1) PRESS W 300ms (mover player para cima), (2) PRESS X 2x para matar spider acima (player olha para cima após W), (3) PRESS A 700ms, (4) PRESS R.

## Playthrough (33): player NÃO se moveu apesar dos comandos (W 300ms + A 700ms)
Tela m16: player ainda em (658,560); vida 73 (spiders atacam). O movimento não funcionou. Possível causa: o RELEASE chega tão rápido que o update não processou? Não, 300-700ms é muito. Verificar o log [bridge]: se o PRESS W chegou, gameState deve ser visto. Se gameState era diferente de NORMAL... o menu.show? O log dirá.

## Playthrough (34): bridge aceita conexão mas não processa comandos
Conexão entra (sem Connection refused) mas nenhum log [bridge] aparece e o player não move. O server.accept funciona (porta não fecha). Suspeita: o client envia "PRESS W\n" e o readLine deveria pegar. Ou o thread do bridge morreu com exceção (o server.accept em loop dentro de try-with-resources — se client.accept falhar após accept, catch ignora e LOOP CONTINUA ok). VERIFICAR: ps do java atual, e testar se a classe compilada no build/classes contém o log (recalcular timestamp).

## Playthrough (35): classe Game.class de 03:58 = versão SEM a bridge nova
O jogo atual (PID 21978) NÃO contém startStdinBridge TCP nem o log. O compileJava disse "1 actionable task" mas escreveu na versão antiga? NÃO — o timestamp 03:58 é ANTERIOR à minha edição (editei depois). Ou seja: o compileJava não recompilou o Game.java! Motivo provável: a edição do Game.java quebrou a sintaxe? Não, BUILD SUCCESSFUL... ou o Gradle usa incremental e detectou mal. SOLUÇÃO: ./gradlew clean compileJava e conferir timestamp novo.

## Playthrough (36): BRIDGE FUNCIONA — PRESS W chegou, gameState=MENU, player px32 py32 (spawn fase 9)
O jogo carregou a fase 9 direto (spawn no NW do mapa 48x32 → x32 y32). Mas gameState=MENU! O load do save deixa o jogo em MENU (pausa?) — verificar menu.pause. Se pause=true, ENTER fecha a pausa? Não: ENTER no MAIN chama handleMainMenuSelection (Novo jogo!). ESC com pause=true fecha a pausa → NORMAL. Plano: PRESS ESC.

## Playthrough (37): load do save vai para MENU sem pausa (seta em Sair)
O save é carregado (player no spawn da fase 9) mas gameState=MENU sem menu.pause → jogo "pausado" no menu principal visível. ESC não fecha (MAIN sem pause). SOLUÇÃO: navegar até "Continuar" e ENTER → handleMainMenuSelection → loadSlot de novo → desta vez o fluxo normal? Na rodada anterior isso funcionou e o jogo rodou NORMAL. Fazer isso.

## Playthrough (38): jogo NORMAL rodando na fase 9, vida cheia
Plano: (1) PRESS W 250ms (player olha N); (2) PRESS X 2x (matar spider em cima do Léo); (3) PRESS A 600ms; (4) PRESS W 350ms; (5) PRESS R; (6) ver diálogo; (7) ENTER para avançar diálogo. Delta player→Léo: (-258,-145). A 600ms ≈ 200px + W 350ms ≈ 115px → perto.

## Playthrough (39): player atravessou a borda NW (px-16 py-15)
A velocidade do player é maior que o estimado (~350px/s). O player voltou? A tela mostra player em (658,560) com vida 60 — o screenshot m19 mostra posição antiga? Não, o player visível em m19 está em (658,560). Mas o log diz px-16 py-15. CONFLITO: o player visível não é o do log? O "player" no log é game.player; mas a tela mostra o personagem marrom no centro... (658,560) é o ponto de spawn visual inicial. A câmera mostra a região do Léo (400,415). O px-16/py-15 do log: o player SUBIU além da borda (o A 600 + W 350 + W250 = ~1350ms de movimento ≈ 470px — sim, passou!). Mas a tela mostra o player no centro... A tela m19 foi tirada DEPOIS do R (1800ms), durante esse tempo o player pode ter se movido de volta? Não. MAIS PROVÁVEL: o print (int) game.player.getX() retorna posição WORLD relativa à câmera? Não, getX é mundo. INCONSISTENTE: tela mostra player (658,560) e log -16,-15. O personagem marrom em (658,560) pode ser um NPC (Líder resgatável)! E o player de verdade está fora da tela (fora do mapa NW). O R interagiu com algo? Ver banner. AÇÃO: recuperar player — mover PRESS S 800ms PRESS D 400ms para voltar ao mapa; tirar screenshot e conferir.

## Playthrough (40): ESTADO ATUAL COMPLETO
Tudo funcionando: bridge TCP (porta 10445, /tmp/gamedrv via /dev/tcp), jogo com classes de build/classes/java/main:res (SEMPRE usar esse classpath; bin/ está desatualizado!), save fase 9 carregado, missão "Fale com Curandeiro Léo" ativa e HUD/banners corretos. O log da bridge ([bridge] ...) em /tmp/game.log mostra gameState e posição do player — usar para calibrar movimento.

PROBLEMA: movimento é mais rápido que o estimado. Último: player saiu do mapa (px-16 py-15) após A 600+W 350+W 250. O personagem marrom visível em (658,560) é provavelmente o NPC líder resgatável, não o player! O player real está fora do mapa a NW. PRÓXIMA AÇÃO: recuperar — PRESS S 1000ms PRESS D 600ms, screenshot, conferir log. Depois aproximar do Léo (visível em 400,415) com passos curtos de 150-250ms e screenshots entre passos. Player velocidade ≈ 350-400px/s. Léo tem 2 paredes destrutíveis marrons ao lado (tile 20,13 e 19,13 aprox).

SEQUÊNCIA FINAL APÓS FALAR COM LÉO: (a) resgatar líder resgatável (NPC marrom em ~658,560) colidindo com ele; (b) ir ao beacon verde (tile ~26,16) e aguardar ~3s para ativar; (c) cutscene vitória (Enter) → modo sobrevivência (sobreviver waves) → fim campanha. Depois: remover bridge TCP + log do Game.java, escreverDebugSave do SaveManager, TempLoadLevel9Test.java, src/tools/MakeLevel9SaveHelper.java; ./gradlew check; commitar; PR Rodada 30.

## Playthrough (41): gameState=MENU durante S/D — player congela fora do mapa
O load do save coloca o player em (-16,-15) = posição salva no save? Ou spawn mal calculado do level9.png (spawn tile (1,1) = (48,48) com -32? tile x*32: tile(0,0)=(-16,-16)?). O personagem marrom visível em (658,560) NÃO é o player — é o NPC líder resgatável. O player real está em (-16,-15) FORA do mapa, invisível, e o menu abre. SOLUÇÃO: editar make_save_level9.py para salvar posição do player no spawn correto ((48,48)) ou (96,96), regenerar save, reload. Ou mais simples: investigar loadSlot — se o save contém posição do player, ela é restaurada; se o save é do script com posição errada, corrigir.

## Playthrough (42): loadSlot OK mas spawn do level9.png posiciona player fora
loadSlot seta gameState=NORMAL e Menu.pause=false corretamente, chama World.restartGame("level9.png"). O player nasceu em (-16,-15) → o pixel 0026FF está no tile (0,0) do PNG (tile 0 = coordenada -16 a 16; centro -16? = fora da área jogável). FIX: editar tools/make_level9.py para mover o spawn 0026FF para o tile (3,3) (coord ~80,80, área de grama segura longe de spiders/inimigos e do Léo), regenerar res/level9.png, reiniciar jogo e reload.

## Playthrough (43): hipótese — applyMapPixels roda ANTES da criação do novo Player
Player.x default = -16,-15. Se World.restartGame cria o novo Player DEPOIS do applyMapPixels, o setX/setY do spawn age sobre o player antigo (que é substituído depois). FIX candidato: em Game.restartGame/World.restartGame, garantir que o player.set após criar o novo player — ou mover o player no próprio restartGame para o spawn. Verificar order primeiro; se confirmado, fazer correção mínima (aplicar spawn novamente após criar o player) — isso é uma MELHORIA real para o jogo (bug de spawn), pode virar commit próprio.

## Playthrough (44): culpado provável = syncFromPersistentState() após restart no loadSlot
A ordem do restart está correta (player criado em (0,0), applyMapPixels seta posição do spawn). Mas loadSlot (SaveManager ~linha 563) chama game.player.syncFromPersistentState() DEPOIS do restart — se esse método resetar x/y para default (-16,-15), o spawn é perdido no load! VERIFICAR syncFromPersistentState no Player.java. Se confirmado: preservar posição no save ou não resetar x/y no syncFromPersistentState (adicionar savedX/savedY no session do save) — a solução mais simples: no loadSlot, aplicar Game.player.setX/setY depois de syncFromPersistentState usando a posição do spawn do mapa (recalcular via pixel do mapa ou guardar no save).

## Playthrough (45): loadSlot provavelmente FALHOU (gameState ficou MENU)
Verificar /tmp/game.log por stacktraces após o ENTER do Continuar. Se loadSlot falha, o menu "Carregar jogo" volta ao MAIN — o jogo fica parado em (-16,-15) que é o default do menu/tela de fundo.

## Playthrough (46): DIAGNÓSTICO FINAL do escape do player
O spawn funcionou (px64 py64 no primeiro load). O player se moveu normalmente mas os comandos de teste (W/A longos) o empurraram pela borda NW até (-16,-15), FORA da área do PNG — onde não há tiles nem paredes, e o jogo abre o menu (player fora do mapa = game over/menu). CAUSA RAIZ: não há clamp de borda no movimento do player. CORREÇÃO: adicionar clamp no Player.update (x entre 0 e World.WIDTH*16-16, y idem) — bug real que afeta qualquer mapa. Commit próprio, depois reload save e continuar playthrough com passos curtos (150-250ms).

## Playthrough (47): jogo NORMAL fase 9 rodando (m21), vida 120/214, missão ativa
O load funcionou desta vez. Próximo: aproximação em passos curtos (200ms) com screenshots entre passos para calibrar velocidade real. Direção: A (esquerda) ~250px, depois W (cima) ~140px até o Léo em (400,415). Passos: A 200ms + screenshot + A 200ms + screenshot + W 150ms + screenshot + R.

## Playthrough (48): velocidade real do player > 3000px/s entre PRESS/RELEASE
Cada comando do gamedrv é uma conexão TCP separada com latência ~0.5-1s — o A ficou pressionado por ~5s, player foi para a borda (clamp funcionou). PLAYER ATUAL: px0 py48 (canto esquerdo). FIX no gamedrv: enviar múltiplos comandos na MESMA conexão com "wait N" inline.

## Playthrough (49): ESTADO COMPLETO + PRÓXIMOS PASSOS
### Infra funcionando:
- Jogo: `DISPLAY=:0 java -cp build/classes/java/main:res com.traduvertgames.main.Game > /tmp/game.log 2>&1 &` (SEMPRE build/classes, nunca bin/)
- Bridge TCP na porta 10445 (startStdinBridge no Game.main, temporária — REMOVER antes do PR junto com o System.out.println [bridge] na simulateKey)
- Driver: /tmp/gamedrv (bash, /dev/tcp). PROBLEMA: cada comando = conexão nova com ~0.5-1s de latência → PRESS fica pressionado por demais. MODIFICAR gamedrv para aceitar múltiplos argumentos na MESMA conexão com suporte a "wait N" inline (sleep dentro do loop de leitura).
- Save da fase 9: python3 tools/make_save_level9.py → saves.json; menu → DOWN+ENTER ("Continuar" carrega direto)
- Janela: WID 18874375 (xdotool search --name "Game 2 RPG")

### Fase 9 validada até agora:
- Mapa carrega, HUD "Missão: Evacuar sobreviventes — Fale com Curandeiro Léo", banner OK
- Léo (amarelo CDDC39) em tile (6,6)=(96,96) com label "R — Curandeiro Léo" — visível na tela em (400,415)
- Spawn do player tile (3,3)=(48,48) — clamp corrigido; player atual: px0 py48 (borda, movido demais)
- Vida 120/214, NPCs: líder marrom (658,560), beacon verde (26,16), Nia (22,14), Ivo (22,16)

### PRÓXIMOS PASSOS:
1. Modificar /tmp/gamedrv: loop `for arg; do if [[ $arg == wait:* ]] then sleep ... else echo $arg; fi; done` > /dev/tcp, tudo na MESMA conexão
2. Voltar o player ao centro: PRESS D 300, screenshot; calibrar distância/timing com wait inline (velocidade ~1400px/s em terreno grama? medir)
3. Aproximar do Léo (96,96) com passos curtos calibrados (50-100ms wait inline), depois PRESS R, depois ENTER (diálogo)
4. Depois: salvar (tecla T), falar com líder NPC marrom (resgatar), ativar beacon (ficar perto ~3s), ver cutscene, modo sobrevivência, fim de campanha
5. REMOVER código temporário: bridge TCP + log [bridge] no Game.java (simulateKey), e depois ./gradlew check completo, commit, push, PR Rodada 30 (base main)
6. Commits já feitos na branch manus/metagame-rodada-29: d0eabb0 (healer), mapa level9, 9a2f451 (integração fase 9), e2343ca (testes — 28 testes), d733071 (clamp), f17aeab (spawn único)

## Playthrough (50): calibração concluída
Velocidade do player: ~107px/s na grama (150ms=16px, incl. overhead). Player (16,48), Léo (96,96): D ≈ 750ms, S ≈ 450ms. Vida caiu a 48/214 (spiders) — curar no Léo primeiro! O player visível na tela é o boneco branco no canto superior esquerdo.

## Playthrough (51): player morreu (spiders) — estratégia ajustada
Continuar carrega save (vida 120, escudo 45). AÇÃO: D+X alternado (X rajadas de 250ms durante o deslocamento) para matar spiders no caminho. Sequência: D 200, X 300, D 200, X 300, S 300, X 300, D 150, R (no Léo). O tiro mata spiders que chegam perto (spider em (48,32) perto do spawn!) — matar primeiro com X.

## Playthrough (52): reload OK — executar deslocamento com X intermitente até o Léo

## Playthrough (53): DIÁLOGO DO CURANDEIRO LÉO ABERTO — página 1/2 "Olá, piloto. Fico feliz em vê-lo."
Pressionar ENTER para avançar; esperar a cura (+60% vida, +20 escudo) e a missão de resgate. Depois: resgatar líder (NPC marrom em (658,560) — caminhar D e S?), ativar beacon verde (tile 26,16=(416,256)), cutscene, sobrevivência, fim de campanha.

## Playthrough (54): página 2/2 "Até mais, piloto." — concluir com ENTER; verificar cura aplicada

## Playthrough (55): CURA FUNCIONOU + missão de resgate ativa
Vida 214/214, escudo 34/195, missão "Evacuar sobreviventes — 0 de 1 evacuados". FALAR COM LÉO VALIDADO! Próximo: resgatar líder (tile 24,15=(384,240)): caminho D ~275px (2600ms?) — não: dx=384-109=275px → D 2500ms? Não: velocidade 107px/s → 275px ≈ 2600ms?? ERRADO: calibração 150ms=16px → 107px/s, então 275px ≈ 2.6s. E dy=240-65=175px → S ≈ 1.6s. Executar D 2400 + S 1600 com rajadas X (spiders no caminho!). MAIS RÁPIDO: ir ao líder, resgatar (colidir com ele?), depois beacon (416,256).

## Playthrough (56): player (354,145) no muro norte, 7 spiders em volta, vida 157/214
Plano: 4x rajadas X 500ms, depois descer pela abertura do muro x=30,31 tiles (480-512): D 1300ms + S 950ms (dy=240-145=95px... esperem y aumenta para S: 145→240 = 95px ≈ 900ms). Depois resgatar líder (colisão com NPC marrom em tile 24,15).

## Playthrough (57): morreu de novo no muro — ir pela abertura esquerda com LifePack
Reload (Continuar) e rota segura: A 800ms (→274,145) + S 1400ms (→274,285, colide com LifePack 288,288) + X rajadas + D 1050ms (→380,285) + W 500ms (→380,235 ≈ líder 384,240). Se spiders seguirem: X intermitente. Depois validar resgate e beacon.

## Playthrough (58): spiders da linha y=7 matam o player no caminho
Editar make_level9.py: remover spiders y=4 (x 18,24,30,36) e y=7 (x 12,18,24,30,36) — região NW é área de chegada segura; mover desafio para o vale central/sul. Adicionar LifePack (18,8) e ShieldOrb (5,8) perto da rota. Depois: regen mapa, commit, reload, validar playthrough completo.

## Playthrough (59): novo mapa balanceado — diálogo Léo 1/2 de novo (reload reseta missão)
Mapa novo OK: sem spiders na área de chegada, ShieldOrb (5,8)=(80,128) e NanoMedkit visíveis. Executar ENTER 2x para concluir o diálogo e cura.

## Playthrough (60): ESTADO ATUAL (m33)
- Diálogo do Léo concluído, cura aplicada: VIDA 214/214, ESCUDO 67/195
- Missão: "Evacuar sobreviventes — 0 de 1 evacuados"
- NPC "Veterano Rex" (líder resgatável) em tela (672,560) = mundo (~656,560), label "R — Veterano Rex"
- Player em (125,96); spider próxima (717,433) e (597,595) perto do Rex
- Plano: matar spiders próximas com X (4x 500ms), descer: D 5300ms? não — player(125,96)→Rex(656,560): dx=531px=D ~5000ms, dy=464px=S ~4400ms. MUITO LONGO — spiders vão matar. Melhor: pegar ShieldOrb/NanoMedkit perto (ShieldOrb (80,128)=px80 py128 está a 45px do player!), ir S 32ms... ShieldOrb em tela (343,540)= mundo (343,540) — player (125,96). Pegar ShieldOrb: D 2200ms + S 4200ms?? dy=540-96=444px=4200ms, dx=343-125=218=2000ms.
- DECISÃO: pegar ShieldOrb primeiro (S direto 4200ms), X rajadas pelo caminho, depois Rex (D 313px=2900ms), resgatar (colidir = automático?), depois beacon (26,16)=(416,256): W... 
- Depois do resgate: salvar (T), ir ao beacon 4CFF00 (26,16), ficar perto ~3s para ativar evacuação, cutscene, modo sobrevivência, créditos/fim campanha.
- Depois REMOVER bridge TCP do Game.java + log [bridge] + writeDebugSave do SaveManager + tests/java/com/traduvertgames/test/TempLoadLevel9Test.java + src/tools/MakeLevel9SaveHelper.java, rodar ./gradlew check, commit, push, abrir PR Rodada 30 (base main).

## Playthrough (61): player (96,127), ShieldOrb consumido. Rota ao Rex (656,560): D 1700 (→270,127), S 700 (abertura 256-288), D 3850, S 3650, com rajadas X.

## Playthrough (62): morreu cercado por spiders — decisão de design
A fase 9 é fase NARRATIVA de resgate; o desafio fica na sobrevivência pós-beacon. Editar make_level9.py: remover linha WARDEN y=12 e reduzir Sentinels (11→6). Regenerar, commitar, reload, validar rota.

## Playthrough (63): mapa balanceado funciona! Player (743,496), vida 214/214
Rex (656,560): A 900 + S 600. Depois beacon (416,256): A 2200 + W 2800 (com rajadas X por segurança).

## Playthrough (64): o Rex não está nesta área — verificar posição no make_level9.py

## Playthrough (65): Rex em tile (24,15)=(384,240) — voltar pela abertura sul x=30,31
Player (645,496) abaixo da muralha sul (y=336-368). Rota: A 1500 + X + W 1300 (abertura 480-512) + A 1100 + W 1300 (→384,236=Rex).

## Playthrough (66): morreu no retorno — decisão final de balance
Remover Ravagers da trilha sul (y=26,28) e Sentinels y=17; manter apenas 3 Sentinels em y=13. Fase 9 = narrativa de resgate, dificuldade vai para a sobrevivência (fase 10/11).
