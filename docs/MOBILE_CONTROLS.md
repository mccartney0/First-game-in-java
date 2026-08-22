# Controles móveis — movimento e mira

## Objetivo

O RPG usa dois controles independentes para que o personagem possa deslocar-se em uma direção e atacar em outra. O analógico esquerdo regula o movimento. O controle direito mantém a direção de mira, aciona ataques sustentados e conserva a última orientação válida quando a pessoa solta o dedo.

> A orientação da caminhada não é mais substituída por um toque de ataque. Durante a animação de ataque, o sprite usa a direção de mira; ao terminar, volta a exibir a direção de deslocamento.

## Mapa de controle

| Área | Ação | Calibração aplicada |
|---|---|---|
| Analógico esquerdo — **MOVER** | Arrastar para deslocar o herói. | Zona morta radial de 18%, raio visual de 58 px e velocidade-base de 204 unidades/s. A diagonal é normalizada, portanto não fica mais rápida que um eixo. |
| Controle direito — **MIRA** | Arrastar para definir a direção independente de ataque. | Mantém o último vetor além da zona morta e projeta a mira a 118 unidades do herói. |
| Toque curto no controle direito | Conversa, abre baú ou interage quando há alvo próximo; caso contrário, dispara uma vez. | Ação contextual sem exigir que o jogador pare de mover. |
| Toque mantido no controle direito | Lança projéteis continuamente após 120 ms. | Cadência mínima de 180 ms, reduzida apenas pelo atributo de magia. |

## Feedback de combate

Enquanto o controle direito está ativo, uma retícula dourada aparece à frente do personagem. Ela indica o vetor de mira e o ponto de disparo. O botão direito ganha preenchimento dourado durante o ataque, enquanto seu disco interno mostra a última direção escolhida. O analógico esquerdo mostra apenas o vetor de caminhada.

## Protocolo de aceitação em dispositivo

| Cenário | Procedimento | Resultado esperado |
|---|---|---|
| Caminhada fina | Mover o dedo até 20% do raio esquerdo. | O personagem permanece parado; não há tremor por ruído de toque. |
| Caminhada diagonal | Arrastar o analógico esquerdo para um canto. | A velocidade é igual à caminhada horizontal ou vertical. |
| Tiro em recuo | Mover à esquerda e manter a mira à direita. | O herói caminha à esquerda e os projéteis seguem à direita. |
| Troca de alvo | Girar o controle direito durante a caminhada. | Retícula, arma e próximo projétil acompanham a nova direção sem interromper o deslocamento. |
| Interação | Parar junto de Ava e tocar brevemente o controle direito perto do centro. | O diálogo abre sem disparar um projétil. |
| Ataque sustentado | Arrastar e manter o controle direito por mais de 120 ms. | O ataque inicia após o limite e respeita a cadência de combate. |

## Ajustes futuros

Os valores de zona morta, velocidade, alcance de mira e atraso de ataque ficam declarados como constantes em `GameView`. Mudanças de sensação de controle devem alterar essas constantes, repetir o protocolo acima e reconstruir o APK. Não devem ser feitas por escala de bitmap ou por alteração nos assets de personagem.
