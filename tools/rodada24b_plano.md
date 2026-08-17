# Rodada 24b — Elites procedurais + recorde de profundidade

## 1. Elites procedurais (a partir da profundidade 3)
Design (plano 24): inimigos de elite com vida/dano +30% e aura brilhante (visual).

Implementação:
- `Enemy.java`:
  - campo `private final boolean elite` + parâmetro `boolean elite` nos 3 construtores (default false)
  - construtor multiplica `maxLife` e `projectileDamage` por 1.3 quando elite; velocidade +5%
  - `render(Graphics g)`: aura pulsante dourada quando elite (frame pares/desimpares, alpha ~150-220)
- `ProceduralLevelGenerator.java`:
  - nova cor `ELITE = (255, 200, 0)` — pixel amarelo-dourado
  - `placeEntities`: rolagem dedicada — a partir de depth >= 3, parte dos slots de variante viram ELITE pixel; usar roll separado: `if (roll < 70) variante; else if (eliteEligible && eliteRoll) ELITE` — manter contagem de elites limitada: `eliteCount < 1 + depth/3`, total de entidades cap 20 (o pixel ELITE vira um inimigo randômico de variante não-boss com elite=true ao ser lido pela World)
  - `validate`: aceitar rgb ELITE como entidade válida
- `World.java` (applyMapPixels): novo caso `0xFFFFC800` → inimigo de variante aleatória não-boss com `elite=true` (escolher entre WARDEN/SENTINEL/RAVAGER/PHANTOM via rng do mapa — usar QuestManager/World RNG? Melhor: variante fixa determinada por posição para determinismo: (xx+yy)%4)
  - IMPORTANTE: elite NÃO pode contar como chefe (`boss=false`) e não pode ser PHANTOM? O plano diz vida/dano +30%; PHANTOM drena — usar variantes sólidas: WARDEN, SENTINEL, RAVAGER, SCOUT. Excluir GUARDIAN/WARBRINGER/OVERSEER/PHANTOM (elite = tropas de choque)

## 2. Recorde de profundidade (deepRecord)
- `SaveManager.java` (v4): novo campo `deepRecord` por slot (int) + raiz `bestDeepRecord`
  - Incrementar quando uma profundidade do modo infinito é COMPLETA (fases 1-8 completadas contam como campanha; recorde = maior profundidade alcançada no infinito)
  - Atualizar em `advanceProceduralPhase` (Game.java) quando depth supera o recorde do slot; gravar em `saveGame`/autosave
  - Exibir no menu principal abaixo das outras stats (linha "Recorde de Profundidade: N") — Menu.java handleMainMenuSelection/render do menu; usar a fonte/posição padrão do menu
- Migração v3→v4: init 0
- Testes: `Rodada24bEliteTest.java` (elite stats +30%, aura, não aparece antes de depth 3, não conta como chefe, valida mapa) e `Rodada24bDeepRecordTest.java` (record persiste entre sessões, aumenta com profundidade, só infinito, exibido no menu)

## Regressão
26 suítes anteriores + 2 novas = 28/28 verdes antes do commit.
