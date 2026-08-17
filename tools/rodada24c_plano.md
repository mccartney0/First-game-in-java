# Plano da Rodada 24c — QA de stress do gerador e balanceamento do modo infinito

Base: `main` (commit `80968b7` — rodadas 24a e 24b mergeadas).
Branch de trabalho: `rodada24c` (nova, criada a partir da `main`).

## Motivação

A rodada 24b introduziu tropas de elite e curva de dificuldade nas Profundezas,
e a 24a expandiu a narrativa e as side quests do modo infinito. Antes de mais
conteúdo, é preciso garantir duas coisas: (1) o gerador procedural não produz
mapas injogáveis em nenhuma profundidade, e (2) a curva de dificuldade não
se torna injusta (nem tediosa) nas profundidades altas.

## Itens

### 24c.1 — QA de stress do gerador procedural (Rodada24cGeneratorTest)
Suíte dedicada que gera e valida **50 profundidades** (depth 1 a 50) em lote,
checando por nível:

| Check | Critério |
|---|---|
| Estrutura | Mapa tem chão acessível, player spawn livre e saída válida |
| Inimigos | Pelo menos 1 inimigo e dentro do limite de densidade |
| Chefes | Exatamente 1 chefe por mapa a partir da depth 1 |
| Elites | Ausente nas depths 1–2; dentro do cap (`1 + depth/3`) nas demais; nunca é chefe |
| Itens | Itens de cura/mana/armas distribuídos (mínimo por profundidade) |
| Determinismo | Regenerar a mesma depth produz o mesmo mapa (mesma semente) |
| Robustez | Nenhuma exceção em geração/leitura/carga do mapa |

### 24c.2 — Balanceamento fino dos elites e da curva do infinito
- Revisar `applyDifficultyScalingForCurrentLevel` + bônus de elite para
  **depths altas**: propor um teto ou suavização acima da depth ~6 para a
  vida/dano dos elites (hoje +30% fixo sobre um inimigo já escalado).
- Ajustes possíveis: escudo inicial maior nas depths altas, cap de dano de
  colisão, ou densidade de elites crescendo mais devagar — o que os números
  do stress test indicarem.
- Registrar os valores de balanceamento (coeficientes) em constantes nomeadas
  para facilitar ajuste futuro.

### 24c.3 — Testes e regressão
- Nova suíte `Rodada24cGeneratorTest` (stress dos 50 níveis).
- Nova suíte `Rodada24cBalanceTest` (curva de vida/dano/densidade por depth).
- Regressão completa: **28/28 suítes verdes** antes do commit (26 existentes + 2 novas).

## Entregáveis
- Commits na branch `rodada24c` e PR novo para a `main`.
- Relatório do stress test (tabela de checks por depth).

## Ordem de execução
1. Criar branch `rodada24c` a partir da `main`.
2. Implementar 24c.1 e rodar o stress nos 50 níveis (coletar dados).
3. Implementar 24c.2 com base nos dados coletados.
4. Rodar regressão completa e criar o PR.
