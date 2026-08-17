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
