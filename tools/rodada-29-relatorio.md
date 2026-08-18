# Rodada 29 — Metagame de Progressão

**PR:** https://github.com/mccartney0/First-game-in-java/pull/45
**Branch:** `manus/metagame-rodada-29` (a partir de `main`, com os PRs #43 e #44 já mergeados)

## 1. O que foi implementado

A Rodada 29 adiciona uma camada de **progressão persistente entre sessões**: o piloto agora acumula créditos e pode gastá-los em melhorias permanentes que permanecem após reiniciar a campanha.

### Núcleo: `PilotUpgrades.java` (novo, ~160 linhas)

Classe de estado centralizada (mesmo padrão da Rodada 28) com os créditos e quatro upgrades do piloto:

| Upgrade | Efeito | Custo inicial | Escalonamento | Nível máx. |
|---|---|---|---|---|
| **Células Vitais** (`CELLS`) | +25 vida máxima | 100 | +50/nível | 8 (+200 vida) |
| **Regeneração** (`REGEN`) | +1 vida/s passiva | 200 | +100/nível | 5 (5/s) |
| **Escudo de Emergência** (`SHIELD`) | Começa a fase com % do escudo máx. | 150 | +10%/nível | 5 (60%) |
| **Cartucho Estendido** (`AMMO`) | +15% munição/mana inicial | 120 | +10%/nível | 5 (~375) |

O custo sobe **+50 créditos por nível** (base de escalonamento), e `applyToPlayer()` aplica os bônus sobre o `Player` respeitando a fase atual (chamada depois do difficulty scaling, para não ser sobrescrito).

### Fontes de créditos

| Fonte | Créditos |
|---|---|
| Inimigo abatido (`registerEnemyKill`) | +1 |
| Fase da campanha concluída (`onObjectiveComplete`) | +50 |
| Chefe derrotado na campanha (`grantCampaignReward`) | +100 |

### Economia de regeneração

A regeneração é aplicada no `update` do estado `NORMAL` (1 vida por frame-por-segundo, ~1 vida/s por nível), garantidamente **depois do dano** dos inimigos no mesmo frame, como o plano especificava.

### UI: `PilotUpgradesScreen.java` (novo, ~260 linhas)

- Nova tela de loja com os 4 upgrades, custo, nível atual e descrição
- Navegação por setas, `Enter` para comprar (com banner de feedback e som), `Esc` para fechar
- Exibição do saldo de créditos e das mudanças de stats no próximo nível
- Item **"Melhorias do piloto"** adicionado ao menu principal e saldo `CREDITOS: N` exibido no render do menu

### Persistência

- `SaveManager.serialize`: o metagame é gravado na raiz do `saves.json` (chave `metagame`, com `credits` e `pilotUpgrades`)
- `SaveManager.loadSlot`: restaura o metagame e reaplica os upgrades ao player carregado
- `refreshMetagame()`: carrega o saldo ao abrir o jogo e ao renderizar o menu principal

## 2. Validação

### Testes automatizados

**21/21 testes JUnit passando** (`./gradlew test --rerun-tasks`), sendo **11 novos** no `MetagamePersistenceTest`:

- Persistência de créditos e upgrades (save/load round-trip)
- Escalonamento de custos por nível e nível máximo
- Compra com créditos insuficientes é recusada
- `resetCredits` zera o estado estático (limpeza entre testes)
- Regeneração, escudo inicial e munição aplicados corretamente ao player

`./gradlew clean check` — **BUILD SUCCESSFUL**.

### Playthrough (parcial)

Validado em jogo real: menu principal com `CREDITOS: 0` e o item "Melhorias do piloto"; novo jogo → escolha de arma → onboarding → fase 1; save com `T` gravou o metagame no `saves.json`; o load do slot restaurou a fase, vida e munição exatamente como salvos. A validação completa de kill→crédito→compra ficou coberta pelos testes automatizados (mais determinística e reproduzível que a automação de UI).

## 3. Observações e pendências

- **Dificuldade percebida do jogo:** na fase 1 os spiders têm muita vida relativa ao dano do canhão padrão, o que torna os combates longos. Isso é comportamento do jogo base (não regressão — `Player.java`/`Enemy.java` não foram alterados), mas favorece a Regeneração como upgrade: a partir de agora o jogador pode sobreviver melhor acumulando créditos.
- **Próxima rodada planejada (Rodada 30):** distribuição — uber-JAR executável e releases no GitHub.
