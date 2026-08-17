# Rodada 23b — Estado real dos mapas (verificado com scan de pixels)

## level7.png (42×28)
- Chefe Guardian (FF5722) em (22,23) — único. OK como chefe da fase 7.
- Hélio (A1887F) em (20,2) — JÁ ESTÁ NO MAPA (linha 2, col 20). OK.
- Composição: 4 enemies vermelhos (14,y), 3 ravager v, 3 sentinel teal, 3 warden navy, 1 teleporter purple, 1 artillery cyan, 1 phantom h(81C784), 3 quest items q, 2 lifepack L, 1 shield s, 1 nanomedkit n, 1 bullet u, 1 energy C, 2 overload o... (ver grid).
- NPCs: Ava (A) em (3,9), Ivo (I) em (20,20), Engenheira (E) em (29,20).

## level8.png (46×30)
- OVERSEER_PRIME (D01937) em (24,14). OK chefe final.
- SEM Guardian-chefe (FF5722) — mas o objetivo da fase 8 NÃO é BossHunt (é Infiltrator+Escort), então não precisa. OK.
- Composição: 4 guardians (74DE80) → com o fix viram tropas comuns; 4 ravager; 4 random; 2 teleporter; 2 nanomedkit; Ava (A) em (9,3); P em (24,14).

## Fixes aplicados (código)
1. InfiltratorObjective.java: aceita OVERSEER_PRIME como chefe da fase 8. DONE.
2. World.java: novo pixel 0xFFBF360C = Guardian comum (sem flag boss); FF5722 continua chefe na fase 7. DONE.

## Ajuste de mapa necessário
- level7: os 3 guardians adicionais (74DE84/74DE80 = phantom?? NA VERDADE 81C784 é phantom (4x) e NÃO há guardian comum no level7!). Não há nada a trocar no level7.
- level8: os 4 guardians 74DE80 viram tropas comuns automaticamente pelo novo pixel? NÃO — o mapa usa 74DE80, que não tem caso no World (não há pixel 74DE80!). Checar se 74DE80 aparece no World.java. Se não, guardians do level8 NUNCA spawnam no jogo real!
- level7: o "guardian" 74DE80 existe 1x (linha 25, col 1: 'h' em (1,25)?) — na verdade (25,1) no grid mostra h. Verificar World.java por 0xFF74DE80.
- level8 grid: não impresso ainda (só contagens: 4 ravager, 4 guardians...).

## Ação
1. Ver World.java: existe caso para 0xFF74DE80? Se não → adicionar como Guardian comum OU trocar os pixels dos mapas para 0xFFBF360C.
2. Decidir: usar o pixel 0xFFBF360C no World e converter 74DE80→BF360C nos mapas via PIL (consistente).
3. Suítes Rodada23bTest, commit, push PR #37.
