# Rodada 24 — Diretrizes da próxima rodada (PR #38)

A rodada 23 consolidou as fases finais da campanha (7 e 8, com os chefes
Guardian e Supervisor-Prime), integrou a tela de estatísticas pós-fase ao
save (`bestRun`), balanceou os chefes e conectou o pós-campanha às
Profundezas — o modo infinito procedural. A regressão chegou a 23 suítes
(~310 checks). Este documento define o escopo para a rodada 24, partindo da
`main` após o merge do PR #37.

## Contexto: como funcionam as Profundezas (modo infinito procedurais)

O gerador `ProceduralLevelGenerator` (rodada 6, refinado na 23) produz mapas
PNG de 46×30 tiles no mesmo padrão da engine — 1 tile = 1 pixel de 16 px —
usando a profundidade como semente determinística (`depth * 97 + 13`), o que
garante que cada ciclo gera um layout inédito e ainda reprodutível.

A geração segue o pipeline abaixo. Primeiro, o mapa é preenchido com paredes
e são escavadas a sala de entrada (tile do jogador fixo em 3,3, para o
piloto nunca nascer em cima de um inimigo) e de 2 a 4 salas secundárias,
selecionadas entre três templates rotativos por profundidade: sala aberta,
corredores e câmaras. Em seguida, corredores em L conectam o centro de massa
do chão até o canto do jogador, com um corredor horizontal e outro vertical
de fallback que asseguram a navegabilidade de qualquer layout. Os pilares
destrutíveis (paredes cinza) variam por template, dando mais barreiras às
câmaras. Depois vem a ocupação: a densidade de inimigos escala com a
profundidade (`min(20, 6 + depth * 2)`) com rolagem de 70% inimigo, 18% item
de cura e o restante chão vazio — Warden e Sentinel só aparecem a partir da
profundidade 2–3, mantendo a curva de aprendizado. O chefe do ciclo
(Warbringer → Guardian → Supervisor-Prime, rotação por `depth % 3`) nasce no
canto inferior direito, sempre em chão livre. Por fim, `validate()` verifica
spawn livre, chefe presente e mais de 200 tiles de chão; se falhar, o mapa é
regenerado uma vez com semente alternativa antes de ser gravado em
`bin/proc_level_{depth}.png` e carregado pela `World` como qualquer nível.

Com o mapa gerado, o ciclo de jogo segue: o `WaveManager` (arena) spawna
inimigos, anuncia chefes a cada 5 ondas sobrevividas e, ao derrotar o chefe,
chama `advanceProceduralPhase()` — estatísticas do ciclo, bônus de
recuperação e novo mapa na próxima profundidade. A escala é sub-linear (raiz
quadrada das ondas) para nunca virar impossível; cada 3 ondas concluídas há
um respiro de suprimentos. Ao concluir a fase 8 da campanha, `advanceToNextLevel()`
desvia para `enterInfiniteMode()`, que gera o mapa da profundidade 1 e
ativa o ciclo de arena (fix da rodada 23d).

## Escopo proposto (fases 24a+)

| # | Item | Descrição | Critério de validação |
|---|------|-----------|----------------------|
| 1 | **Narrativa nas Profundezas** | Lore e título das profundidades via `StoryManager` (profundidades 1, 4, 7…), com o `MissionBanner` exibindo o avanço da trama ao entrar em cada ciclo | Rodada24aDeepLoreTest: lore existe para ciclos-chave e banner é exibido na transição |
| 2 | **Missões secundárias nas Profundezas** | O `SideQuestManager` continua funcionando no modo infinito: caçadas de bounty, coleta de dados e resgates com recompensas (score/XP) | Rodada24aSideQuestTest: missão secundária spawna, progride e recompensa no modo arena |
| 3 | **Elites procedurais** | A partir da profundidade 3, parte dos inimigos nasce como elite (vida/dano +30%, aura brilhante eFloatingText) para marcar picos de desafio | Rodada24bEliteTest: elites aparecem >= depth 3, atributos escalados, aura renderizada |
| 4 | **Recorde das Profundezas** | `SaveManager` persiste a maior profundidade alcançada (profundidade recorde) e expõe no menu/seletor | Rodada24bDeepRecordTest: recorde persiste entre sessões e aparece na UI |
| 5 | **QA do gerador** | Suíte de stress do `ProceduralLevelGenerator`: 50 profundidades distintas validadas, sem colisão de layout e com chefes sempre em chão livre | Rodada24cGeneratorTest: 50/50 mapas válidos |

## Ordem de execução

1. Fase 24a: narrativa das Profundezas (StoryManager + MissionBanner).
2. Fase 24a: missões secundárias funcionais no modo infinito (SideQuestManager + WaveManager).
3. Fase 24b: elites procedurais e recorde de profundidade no save.
4. Fase 24c: QA do gerador (stress test) + balanceamento fino das elites.
5. Regressão completa das 23 suítes atuais antes do merge.

## Padrão de qualidade (institucionalizado)

- Todo item entrega pelo menos uma suíte de teste headless em `tools/` com
  prefixo de rodada (ex.: `Rodada24aDeepLoreTest`).
- Commits atômicos com prefixo `feat(24)`, `fix(24)` ou `test(24)`.
- Regressão completa roda verde antes de qualquer merge.
