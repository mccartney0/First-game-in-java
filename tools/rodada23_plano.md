# Rodada 23 — Plano da próxima rodada (PR #37)

A rodada 22 consolidou a trilha sonora, NPCs de história com diálogos
ramificados, missões secundárias e o inventário visual. A rodada 22g–22h
fechou o ciclo de QA com validação completa: bug de sessão circular no save
corrigido, robustez a saves corrompidos e novas suítes E2E, elevando a
regressão para 20 suítes (~250 checks).

Este documento define o escopo para a rodada 23, partindo da `main` após o
merge do PR #36, incluindo os fixes de QA já entregues na fase 23a.

## Já entregue nesta branch (fase 23a)

| Item | Descrição | Validação |
|------|-----------|-----------|
| Música desde o menu | `MusicManager.update()` agora roda em qualquer gameState; antes só tocava no estado NORMAL (por isso a trilha só iniciava ao carregar um save) | Rodada23aMusicAndScreensTest: 8 checks |
| Tela de Game Over alinhada | Fontes e coordenadas proporcionais ao SCALE (fim do texto cortado e desalinhado na tela cheia) | Idem |
| Fase concluída sem texto duplicado | Faixa "Fase X concluída" suprimida enquanto o card de estatísticas está visível; inimigos ocultos e fade decaindo no GAMEOVER | Idem |

## Escopo proposto (fases 23b+)

| # | Item | Descrição | Critério de validação |
|---|------|-----------|----------------------|
| 1 | **Fases 7 e 8 com chefes** | `level7.png` e `level8.png` no padrão da engine (mapa 42×24, 16×16) e chefes com padrões de ataque distintos (projéteis + investida; escudo + rajada) | BossSpawnTest: chefe aparece só após o objetivo; chefes morrem no balanceamento definido; suíte de boss behavior |
| 2 | **Tela de estatísticas pós-fase** | Ao concluir uma fase, mostrar kills, tempo, combo máximo e score antes de avançar | PhaseStatsScreenTest: renderiza, Enter avança; valores condizentes |
| 3 | **Modo Infinito procedurais** | Após a fase 8, gerar fases infinitas com dificuldade escalável (mais inimigos, elites, chefe procedural a cada 3 fases) | InfiniteModeTest: fase 9+ gerada, dificuldade cresce, não trava |
| 4 | **Balanceamento fino** | Ajustar HP/dano/chances dos chefes 7/8 e a curva do modo infinito | Suítes de QA aprovam; playtest sem mortes involuntárias na fase 1 |
| 5 | **Skins/companions na loja** | Preview visual do companion na tela de compra antes de confirmar | ShopSkinLogicTest: preview muda com a seleção; compra aplica |

## Ordem de execução

1. Fase 23a (feita): fixes de música e telas.
2. Fases 23b–23c: assets das fases 7/8 e chefes + balanceamento inicial.
3. Fase 23d: tela de estatísticas (integra com o save via `bestRun`).
4. Fase 23e: modo infinito (depende da tela de estatísticas para o loop pós-fase).
5. Fase 23f: skins de companions na loja (independente, em paralelo).
6. QA final: suítes novas por item + regressão completa das 20 suítes atuais.

## Padrão de qualidade (institucionalizado)

- Todo item entrega pelo menos uma suíte de teste headless em `tools/` com
  prefixo de rodada (ex.: `Rodada23aTest`).
- Commits atômicos com prefixo `feat(23)`, `fix(23)` ou `test(23)`.
- Regressão completa roda verde antes de qualquer merge.
