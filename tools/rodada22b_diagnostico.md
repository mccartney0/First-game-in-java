# Rodada 22b — Bugs reportados pelo usuário (PR #36)

## Bugs reportados (mensagens do usuário)
1. **Jogo travou/escureceu**: após abrir o menu (loja?) e fechar, mobs desapareceram e a tela ficou mais escura. Captura mostra o jogo com tudo escurecido e HUDs normais.
2. **Missão "Localize o beacon do setor"** (Defender o porto — fase 3?): após matar tudo e falar com todos, não avança. Possível: objetivo HoldSequence/Survive nunca conclui, ou refreshCollectibles do SideQuestManager quebra algo? Ou a fase precisa de entrega ao NPC (beacon) que não foi feita.
3. **Seletor de fases aberto a qualquer fase**: o menu inicial (TAB) permite escolher qualquer fase, até modo infinito. Corrigir para só permitir fases já concluídas + primeira não concluída; fases bloqueadas devem aparecer travadas (ícone cadeado/cinza).

## Hipóteses técnicas
- Bug 1: provável causa = o fade da loja/inventário não é restaurado, ou o `overlayG` do inventário deixou a loja em estado de fade permanente. Ou `ShopManager.close()` sem limpar `Game.fadeAlpha`/`fadeIn`. Outra pista: "depois que abriu o menu pra comprar algo e fechei, os mobs desapareceram" — pode ser `entities` limpo ou WaveManager `arenaMode`/`paused` preso. Também possível: `MusicManager.pause()` sem resume.
- Captura mostra HUD com "Célula x1" no inventário — o jogo estava com inventário ABERTO e o painel ocupando a tela? Não: a captura é normal com tudo escuro. O inventário desenha um painel transparente escurecendo a tela (`alpha` de overlay) — se o painel travou aberto, tudo fica escuro e mobs não se atualizam! **Hipótese forte: inventário aberto (I) sem poder fechar — ESC não fecha.** Verificar tratamento de ESC quando inventário aberto no Game.java.
- Bug 2: objetivo da fase 3 é "Defender o ponto" — verificar QuestManager fase 3 (Survive/Hold) e as condições de conclusão. Pode ser que falte "falar com o NPC de entrega" após survive. Verificar também se minha edit no QuestManager.update() (refreshCollectibles) está quebrando algo quando SideQuestManager não tem missões (iterate over empty map — ok). MAS: QuestManager.onLevelLoaded + SideQuestManager.register — se refreshCollectibles roda TODO frame e a missão tem target=0? Não. Verificar "Defender o porto" (fase 4?) e o beacon: é o HoldObjective/SurviveObjective que exige tempo — mas após matar tudo não conclui? Verificar WaveManager: wave completa → conclusão?
- Bug 3: Menu de seleção de fases (PhaseSelector no Menu.java?) sem trava. Localizar e adicionar bloqueio por progress (fases concluídas no save/progress).

## Checklist de correções
- [ ] Verificar ESC no estado com inventário aberto
- [ ] Verificar ShopManager.close → restaura fadeIn/fadeAlpha/waveManager
- [ ] Verificar QuestManager fase 3-4 objetivos e conclusão do beacon
- [ ] Travar seletor de fases por progresso
- [ ] Testes completos


## Diagnóstico confirmado (fase 1)

### Bug 1 — "menu pra comprar" fechado → tela escura + mobs invisíveis
Hipótese principal confirmada estruturalmente:
- O inventário abre com `I` (Game.java ~1258, `InventoryManager.toggle()`), mas o **ESC NÃO fecha o inventário** — o handler do ESC (linhas 1171-1205) trata MENU/GAMEOVER/SHOP/LEVELUP/LEVELSELECT/inicialWeaponSelect, mas NÃO cobre `InventoryManager.isOpen()` em NORMAL. O jogador fica preso com o painel aberto.
- O render do painel (`InventoryManager.render`, linha ~290+) desenha fundo `Color(0,0,0,235)` quase preto → tela escura; os mobs continuam atualizando, mas o overlay escuro os esconde.
- "Mobs desapareceram" na captura: provavelmente o overlay escuro + HUD de combate ocultada (`hidingHud=true` quando InventoryManager.isOpen(), linha ~808).
- FIX 1: no handler do ESC (Game.java, bloco `NORMAL`), fechar o inventário: `if (InventoryManager.isOpen()) { InventoryManager.toggle(); return; }` antes de `Menu.openPauseScreen()`.

### Bug 2 — Missão "Localize o beacon do setor" (fase 4 = "Núcleo da Colônia — Resgate"? / fase 3 Círculo do Ritual?)
- Objetivo é `HoldObjective("Isolar o núcleo do Guardião", "O beacon de contenção precisa de energia estável...")` (QuestManager.java linha 124-128) — HOLD com timer.
- Usuário reporta que após matar tudo e falar com todos não avança. HOLD objetivo termina por tempo (WaveManager?), ou precisa ativar beacon (NPC/interação)? Verificar QuestManager linha 124-150 (montagem de objetivos por fase) e HoldObjective.update() — se depende de WaveManager.isArenaMode ou timer. Também possível: refreshCollectibles do SideQuestManager chamado no QuestManager.update() interage mal quando não há missões (mapa vazio — não). MAIS PROVÁVEL: HoldObjective exige interação com beacon (QuestBeacon) e o usuário não sabe, OU o timer nunca completa porque WaveManager não está em modo arena. VERIFICAR código.
- Fase 4 é "Núcleo da Colônia — Resgate"; a captura do usuário mostra "Defender o porto — Localize o beacon do setor" — isso é a fase 2? (Câmara do Warbringer = caçada ao chefe). "Defender o porto" não aparece nos títulos visíveis — pode ser objetivo da fase 2/3. VERIFICAR getObjectiveTitle das fases 1-8.

### Bug 3 — Seletor de fases sem trava
- Atalho `L` (linha 1223) abre `LevelSelectScreen.open()`; TAB também (toggleOverlayExpanded). Permitir qualquer fase incluindo 9 (infinito).
- FIX: LevelSelectScreen deve bloquear fases não concluídas (usar SaveManager.progress / QuestManager). Mostrar fases bloqueadas com cadeado/cinza e impedir Enter nelas.

### Fixes planejados (fase 2)
1. Game.java ESC: fechar inventário antes de abrir pausa.
2. Investigar/corrigir objetivo do beacon (HoldObjective).
3. LevelSelectScreen: bloquear fases por progresso (fase desbloqueada = concluída anterior, ou primeira não concluída; fase 9 (infinito) só após fase 8? / ou após completar alguma). Verificar como SaveManager guarda progresso (progress map "completedLevels"?).
4. Melhorias UX sugeridas: dica de tecla ESC no painel do inventário ("I para fechar" já existe), talvez.


## Causa raiz do bug 2 ("Localize o beacon do setor" não avança)

A fase 2 é `DialogueObjective(Sequence(HoldObjective, BossHuntObjective("Derrubar o Warbringer")), "Engenheira Nia")`.
O progresso da HUD usa o objetivo ATIVO da sequência — enquanto o jogador está no Hold, o texto é o do Hold; mas quando o canal estabiliza (Hold completo), o texto passa a "Varra a fortaleza e encontre o Warbringer" (BossHunt). O usuário viu "Localize o beacon do setor" PARADO.

Cenário real do travamento:
1. O beacon da fase 2 é criado programaticamente em `HoldObjective.onLevelLoaded()` (tile 17,11) — spawnado, trackedBeacons ok.
2. O usuário mata todos os mobs da fila do WaveManager (inimigos do mapa + boss). Mas a fila do WaveManager é vazia na campanha (queueSpawn nunca é chamado!) — os mobs vêm dos pixels do mapa. Quando morrem todos, não há invasores → o canal avança 1/frame e estabiliza em ~10s. Hold completa → BossHunt ativa.
3. BossHunt.isComplete() exige `bossPresent && bossDefeated`. O boss é o Warbringer garantido por ensurePhaseBoss. Ele DEVERIA ser derrotado... mas `notifyBossSpotted` só roda no CONSTRUTOR do Enemy (linha 213) — para o boss do mapHasBoss (pixel do mapa), a construção do Enemy ocorre em World.applyMapPixels e o `isBoss()` é setado no construtor via variante, com notifyBossSpotted já chamado no construtor. Então bossPresent=true. Quando o jogador mata o boss, onEnemyKilled marca bossDefeated=true. Completo.

Onde trava então? O SequenceObjective: quando o delegate atual (Hold) completa, ele troca para o próximo (BossHunt). MAS se o BossHunt objective é do tipo que exige FALAR com o NPC? Não.

Hipótese mais forte: **o usuário não ativou o beacon nem deixou a zona limpa, e os inimigos continuam nascendo da fila?** Não, fila vazia na campanha.

Hipótese alternativa: o mapa level2 da captura do usuário ("Defender o porto — Localize o beacon do setor" não aparece nos títulos oficiais — títulos: Fase 1 Setor Alpha, 2 Câmara do Warbringer/Caçada ao chefe, 3 Círculo do Ritual, 4 Núcleo da Colônia/Resgate...). "Defender o porto" não bate com nenhum título → pode ser título de fase custom do usuário OU o texto do beacon "Localize o beacon do setor" (que é o getProgressText do Hold com trackedBeacons vazio!). Se trackedBeacons está VAZIO: `Localize o beacon do setor` fica parado para sempre!

Quando trackedBeacons fica vazio? `onBeaconSpawned` adiciona ao conjunto. O beacon é criado em onLevelLoaded — acontece DEPOIS do prepareForLevel? No World.restartGameCommon: QuestManager.prepareForLevel(level) → cria objetivo (Hold.onLevelStart limpa trackedBeacons) → World novo → onLevelLoaded chamado DEPOIS. OK, beacon registrado.

MAS E SE O JOGADOR CARREGOU UM SAVE antigo (save v3 da rodada 21, sem sideQuests/inventário)? No restore, `QuestManager.deserializeObjectiveState` — ver se deserializa HoldObjective (SPAWNED=true, CHANNEL=0) mas o beacon físico do mundo NÃO está mais registrado! trackedBeacons começa vazio (onLevelStart não é chamado no deserialize?) — VERIFICAR. Se restore não chama onLevelStart nem re-registra beacon, trackedBeacons vazio → "Localize o beacon do setor" eterno! **Causa provável do bug: save antigo + Hold restore sem beacon.**
FIX: no deserialize do HoldObjective, se SPAWNED=true mas trackedBeacons vazio, recriar beacon (ou marcar spawned=true e canalizar sem dependência do beacon físico). Melhor fix: tornar o canal independente do beacon físico para o restore — usar um flag interno `beaconMissing = spawned && trackedBeacons.isEmpty()` e fazer o update progredir normalmente (zona = posição do spawn original) OU recriar o QuestBeacon na mesma posição (17,11 fase 2 / 24,13? guardar posição serializada).

### Plano de fixes finais
1. **ESC fecha inventário** (Game.java ESC handler) — bug da tela escura/mobs "invisíveis".
2. **HoldObjective restore sem beacon**: serializar posição (x,y) do beacon; no deserialize, recriar o QuestBeacon no mundo se SPAWNED e trackedBeacons vazio. Também serializar trackedBeacons posições.
3. **UX do beacon**: aumentar visibilidade (raio pulsante, texto maior no mapa) e dica "Permaneça perto do beacon (segure) para ativar" quando spawned e não ativado. Banner já mostra "Ative o beacon..." ao carregar.
4. **Seletor de fases travado**: LevelSelectScreen — bloquear fases > fase máxima concluída + 1; fases bloqueadas cinza com cadeado e Enter ignorado. Progresso: SaveManager.progress/QuestManager completedLevels (verificar API real).


## Decisões finais de implementação (rodada 22b)

**Progresso da campanha**: `SaveManager.updateCampaign` grava `campaign.maxLevelReached` a cada save. Não há leitor público — adicionar `public static int getHighestUnlockedLevel()` (maxLevelReached, default 1) no SaveManager, para o LevelSelectScreen usar.

**Fix 3 (seletor de fases)**:
- LevelSelectScreen: calcular `maxUnlocked = Math.max(1, SaveManager.getHighestUnlockedLevel())`; se save não existe (partida nova), usar 1.
- Fases bloqueadas: renderizadas em cinza com símbolo `[🔒]` (texto "TRAVADA" ou cadeado "X") e Enter ignora. Navegação pula fases bloqueadas (navigateUp/Down) — ou deixa navegar mas bloqueia confirm. Decisão: navegar normalmente (o jogador vê tudo) mas Enter bloqueado com som de erro e banner "Conclua a fase anterior para desbloquear".
- Modo infinito (fase 9): desbloqueado após maxLevelReached >= 8.
- Seleção inicial: se a seleção cair numa fase bloqueada, avançar até a primeira desbloqueada no open().

**Fix 1 (ESC fecha inventário)**: Game.java ESC handler — antes de `Menu.openPauseScreen()`: `if (InventoryManager.isOpen()) { InventoryManager.toggle(); return; }`.

**Fix 2 (beacon restore)**: HoldObjective — serializar posições dos beacons (`Bx1,y1;Bx2,y2`) além de SPAWNED/CHANNEL; no deserialize, para cada beacon serializado, recriar o QuestBeacon no mundo (Game.entities.add) e registrar em trackedBeacons; também onLevelStart não limpa trackedBeacons se SPAWNED restaurado. Guardar posição também no spawn original (save da posição do beacon criado em onLevelLoaded).

**Fix 2b (UX do beacon)**: enquanto spawned && !activated && channel < MAX, MissionBanner já mostra dica; reforçar com texto no MissionHud? O getProgressText já mostra "Defenda! N invasores" / "Canal X%". Melhorar: no hold, se `!isBeaconActivated` mostrar "Segure o jogador junto ao beacon para ativar" — adicionar flag activatedCount.
