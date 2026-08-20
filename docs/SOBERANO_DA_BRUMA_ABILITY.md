# Soberano da Bruma — Núcleo da Bruma

O **Núcleo da Bruma** é a habilidade especial configurável do chefe **Soberano da Bruma**. Quando o jogador entra no alcance de ativação, o chefe emite uma pulsação de energia rubra que ignora o ataque básico daquele ciclo, causa dano próprio e cria um halo visual breve ao redor do chefe.

| Campo do manifesto | Padrão | Função no runtime |
|---|---:|---|
| `damage` | 14 | Dano bruto da pulsação, mitigado parcialmente pela defesa física. |
| `cooldownTicks` | 180 | Intervalo mínimo entre duas pulsacões. |
| `range` | 168 | Distância máxima de ativação em pixels do mundo RPG. |
| `ownerRole` | `MIST_SOVEREIGN` | Vincula a habilidade ao Soberano da Bruma. |

O Content Studio exporta um PNG transparente de 32×32 e um manifesto `boss_ability`. O jogo recarrega o manifesto ao iniciar a área externa e aplica valores válidos; na ausência do arquivo, usa os padrões acima para manter compatibilidade com saves e instalações anteriores.
