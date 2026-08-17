# Rodada 22b — Diagnóstico do teste (atualizado)

## Status do Rodada22bTest (última execução)
- 11/13 PASS
- FAILs: "canal do hold conclui apos zona limpa" e "sem duplicacao de beacon apos reload"

## Diagnóstico raiz
- DEBUG: spawned=true, trackedBeacons=2 → o fix da rodada 22b NÃO evita duplicação no
  cenário em que `prepareForLevel(2)` é chamado antes de `onLevelLoaded()` (fluxo real do
  Game.load: `QuestManager.prepareForLevel(CUR_LEVEL); World.restartGame(...); ...deserializeObjectiveState(...)`).
- Causa: `HoldObjective.onLevelStart()` (chamado por prepareForLevel) faz
  `trackedBeacons.clear()` e `spawned=false`, DESTRUINDO o registro do beacon restaurado.
  Em seguida `onLevelLoaded()` vê `spawned=false` e cria um segundo beacon.
  O beacon restaurado fica órfão (não re-registrado, pois registro já ocorreu antes do clear).

## Fix necessário em HoldObjective
- `deserializeState`: após recriar beacons, marcar uma flag `restored=true` (ou recriar os
  beacons em `onLevelLoaded` quando o state restaurado indicar beacons pendentes).
- Melhor abordagem: manter lista `restoredBeaconPositions` no deserialize; em
  `onLevelLoaded`, antes da checagem, se `restoredBeaconPositions` não-vazio, recriar os
  beacons físicos (se não existirem no mundo) e re-registrá-los via `onBeaconSpawned`.
- A guarda `if (spawned && !trackedBeacons.isEmpty()) return;` deve considerar beacons
  restaurados.

## Falha do canal (teste 1)
- Consequência da duplicação: com 2 beacons na lista e inimigos da fase (BossHunt spawna
  mobs) próximos da zona, o canal regrediu a 0. A "zona limpa" exige remover inimigos ou
  reposicionar; ajustar o teste: após verificar "sem duplicacao", o teste deve remover os
  inimigos próximos do beacon (ou o próprio hold.count). Alternativa: mover Game.player
  para longe dos spawns? Mais robusto: limpar inimigos dentro de DEFENSE_RADIUS do beacon
  antes de rodar o loop de frames (simula "zona limpa").

## Fluxo real do jogo (Game.java)
- Transição de fase: `prepareForLevel(CUR_LEVEL)` → `World.restartGame(...)` → faseStats...
- Load do save: `SaveManager.load(...)` chama `QuestManager.prepareForLevel(savedLevel)` e
  `QuestManager.deserializeObjectiveState(state)` — ORDEM IMPORTANTE: prepareForLevel
  reseta spawned/trackedBeacons ANTES do restore; por isso o restore precisa se reconectar.

## Notas do teste
- Teste 3 (seletor) e 4 (inventario/ESC) PASS — fixes ok.
- Helper setHighestReachedForTest agora escreve JSON direto no saves.json (loadRoot/saveRoot
  são privados). OK.
- findActiveHold agora unwrap DialogueObjective→getDelegate() e SequenceObjective (lista de
  stages via reflection). OK.
- gameState="NORMAL" necessário para InventoryManager.toggle funcionar. OK.

## Próximos passos
1. Fixar HoldObjective (restoredBeaconPositions + reconexão em onLevelLoaded).
2. Rebuild + re-teste; verificar que channel chega a 600 (matar inimigos próximos no teste).
3. Rodar suíte de regressão completa.
4. Commit/push dos fixes 22b, atualizar PR #36, reportar usuário.
