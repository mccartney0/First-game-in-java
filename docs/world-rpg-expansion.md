# Expansão do mundo RPG

## Objetivo

A campanha continuará usando mapas PNG compatíveis com a engine atual, mas o modo infinito ganhará um mundo de exploração significativamente maior, dividido em regiões temáticas conectadas por corredores e áreas de combate. A primeira entrega usará um mapa de **96 × 64 tiles** — 1.536 × 1.024 pixels — em vez dos atuais 46 × 30 tiles procedurais.

## Regiões

| Região | Área aproximada | Identidade | Função de gameplay |
|---|---:|---|---|
| Refúgio da Colônia | noroeste | grama e corredores largos | spawn, orientação e recuperação |
| Ruínas Industriais | nordeste | paredes destrutíveis e corredores | mobs comuns e recursos |
| Pântano de Lodo | sudoeste | terreno de lama | inimigos de controle e emboscadas |
| Tundra de Contenção | centro-sul | gelo e câmaras | elites e caminhos alternativos |
| Santuário da IA | sudeste | câmaras densas e pilares | área de elite e objetivo secundário |
| Núcleo do Supervisor | extremo leste | arena fechada | chefe, recompensa e transição |

## Regras de geração

O gerador deve manter um spawn seguro no Refúgio, garantir conectividade entre todas as regiões, reservar corredores de travessia e criar pelo menos quatro bolsões de mobs. Cada bolsão terá um centro, raio, densidade e variante predominante. Os inimigos serão colocados em chão livre, fora do raio seguro do jogador, sem ocupar os pontos de interesse.

A geração permanecerá determinística por profundidade e semente. A validação deverá verificar spawn, chefe, quantidade mínima de chão, conectividade do spawn ao centro de cada região e presença dos bolsões de mobs. A câmera e o render principal já fazem culling por viewport; o minimapa será limitado a uma amostragem/escala compatível com mapas maiores.

## Pontos de interesse

Cada região terá pelo menos um POI lógico: abrigo de cura, terminal de dados, depósito de recursos, portal de transição, beacon de missão ou arena de chefe. Inicialmente os POIs serão representados pela paleta existente e registrados em metadados determinísticos para que objetivos e testes possam consultá-los sem depender de coordenadas frágeis.

## Progressão

O jogador poderá explorar as regiões em qualquer ordem depois do spawn, mas a progressão de profundidade continuará exigindo a conclusão do objetivo principal e do chefe. A região atual será identificada pela posição do jogador e exibida no HUD como feedback de exploração. O conteúdo de campanha existente não será alterado; a expansão será aplicada ao mundo procedural pós-campanha e poderá ser estendida posteriormente às fases 1–9.
