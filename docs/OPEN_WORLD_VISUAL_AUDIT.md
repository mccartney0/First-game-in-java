# Auditoria visual e arquitetural — Mundo Aberto

## Achados visuais

A imagem de gameplay mostra os monstros com um quadrado rosa/branco de 16×16 atrás do sprite, além de auras circulares muito dominantes. O atlas `res/assets/generated/enemies/enemy_set_clean.png` está visualmente transparente e contém seis silhuetas legíveis; portanto, o problema principal não está no atlas limpo. O trecho `Enemy.render()` substitui o sprite inteiro por `Entity.ENEMY_FEEDBACK` durante `isDamaged`, e esse sprite antigo é o candidato direto ao quadrado rosado observado. A correção deve manter o sprite gerado durante o flash e aplicar apenas uma sobreposição translúcida/contorno de dano.

## Arquitetura atual

A campanha narrativa e a Aventura RPG são modos distintos. A Aventura RPG já usa um mapa procedural de 192×128 tiles, regiões, POIs, áreas de mobs, eventos dinâmicos e dungeons. O carregamento de saves reconhece explicitamente `gameMode=RPG_ADVENTURE`, de modo que um novo modo deve receber uma identificação própria e uma restauração correspondente no `SaveManager`.

O `World` carrega mapas PNG completos em memória e transforma cada pixel em um tile de 16×16. Isso permite um mapa maior sem reescrever colisões, mas ainda não possui streaming de chunks. Para o primeiro incremento seguro do mundo aberto, a implementação deve usar um PNG procedural grande e determinístico, com semântica de regiões reutilizando `RpgWorldManager`, e deixar streaming físico para uma etapa posterior se o consumo de memória exigir.

## Critérios de aceitação propostos

1. Nenhum inimigo comum deve exibir um quadrado opaco quando recebe dano; o sprite precisa continuar visível.
2. A aura deve funcionar como informação secundária e não cobrir o corpo do monstro.
3. A nova opção deve iniciar um modo separado, sem alterar a campanha, Aventura RPG ou sobrevivência.
4. O novo mundo deve ser significativamente maior que 192×128 tiles, ter regiões contíguas, áreas de combate e POIs.
5. O modo deve persistir sua identificação, posição/profundidade e progresso regional no save e retornar corretamente após carregar.
6. Testes JUnit e QA headless precisam continuar aprovados, com teste específico para o modo Mundo Aberto.
