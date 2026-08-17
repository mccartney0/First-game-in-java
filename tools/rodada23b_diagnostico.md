# Rodada 23b — Diagnóstico das fases 7/8 (o que existe vs o que falta)

## Já implementado na main (rodada 22) — NÃO recriar

| Componente | Estado |
|---|---|
| Mapas `res/level7.png` (42×28) e `res/level8.png` (46×30) | Existentes |
| Objetivo fase 7: DialogueObjective(Ava) → SequenceObjective(SabotageObjective, HoldObjective "Isolar o núcleo do Guardião") | Feito |
| Objetivo fase 8: SequenceObjective(InfiltratorObjective, EscortObjective) | Feito |
| Chefes: Guardian (0xFFFF5722, boss7=true na fase 7) e Overseer-Prime (0xFFD01937) | Feitos |
| Warbringer (0xFFE91E63), Supervisor (0xFF7986CB), Ravager (0xFFF4511E), Phantom (0xFF81C784) | Feitos |
| NPCs fase 7: Comandante (0xFF00897C), Engenheira (0xFF66BB6A), Pesquisador (0xFF5E35B1), Técnico Hélio/desertor (0xFFA1887F = TraitorNpc) | Feitos no código |
| Quest items sabotagem (0xFFFFC107), beacons (0xFF4CAF50), teleporte (0xFF673AB7) | Feitos |
| Fluxo pós-fase: advanceToNextLevel() na linha 549; fim campanha (fase 8) → VictoryCutscene + modo sobrevivência | Feito |
| BossHuntObjective (seta aponta pro chefe; onBossSpotted; isBoss()) | Feito |
| Música setZone na troca de fase | Feito |

## Deficiências encontradas no level7.png (scan de pixels)

level7: 4 guardians (1 deve ser chefe — o código marca boss7=true SEMPRE na fase 7, mesmo não-chefe!), 4 enemy_random, 3 quest_item, 3 ravager, comandante/engenheira/pesquisador, 1 weapon, 1 nanomedkit, 1 teleporter, 1 energy. **Falta o pixel 0xFFA1887F (Técnico Hélio) — o desertor NÃO está no mapa!**

level8: 4 guardians, 4 ravagers, 4 enemy_random, 2 teleporter, 2 nanomedkit, 1 comandante, 1 overseer_prime_boss, 1 weapon, 1 energy. **Falta o informante/EscortNpc do InfiltratorObjective!**

## Itens de trabalho da rodada 23b (novos)

1. **Adicionar spawns faltantes aos mapas**: Hélio (0xFFA1887F) na fase 7 em local acessível; informante EscortNpc na fase 8 (ver como EscortNpc é instanciado no World — pode precisar pixel novo ou spawn em código no InfiltratorObjective.onLevelLoaded).
2. **Balanceamento dos chefes**: Guardian boss7=true para TODOS os guardians da fase 7 (falso positivo de boss — cada guardian morto dispara BOSS_ALERT e conta como chefe). Corrigir: só 1 Guardian deve ser boss (o da câmara final) — os outros spawnam como variantes comuns. Verificar Enemy spawn do pixel 0xFFFF5722 (linha 209-214): `boss7 = getCurrentLevel()==7` → bug: marca TODOS como chefe. FIX: fase 7 deve ter exatamente 1 pixel de chefe, os demais usam outra variante ou o mapa deve ter só 1 guardian-pixel e o resto por outra cor.
3. **Balanceamento fino**: HP/dano dos chefes 7/8 e curva do infinito — verificar números atuais em Enemy.java (Variant.GUARDIAN, OVERSEER_PRIME).
4. **Suítes novas**: BossSpawnTest (chefe só aparece/morre, seta aponta), InfiniteModeTest (fase 9+ cresce), e regressão final 20 suítes.
5. Commitar `feat(23b)` e manter o PR #37 aberto.

## Verificações antes de mexer nos mapas
- Confirmar como EscortNpc é instanciado (World.java? QuestManager? InfiltratorObjective.onLevelLoaded?) — pode existir pixel dedicado não listado no scan (verificar pixels restantes: 0x... )
- Guardian comum vs boss: decidir cor única para boss (ex: manter 0xFFFF5722 para o chefe, e os 3 guardians comuns trocam para... verificar se existe variante GUARDIAN comum; se não, adicionar ou usar o mapa com 1 só guardian).
- Ferramenta de mapa: os PNGs são desenhados à mão (16×16 blocos) — editar com PIL programaticamente.

## Estado da branch
- Branch: manus/rodada23 (local: rodada23), HEAD 18af4bc (fix 23a + plano). PR #37 aberto: https://github.com/mccartney0/First-game-in-java/pull/37
- main = 96af48e (PR #36 mergeado).
