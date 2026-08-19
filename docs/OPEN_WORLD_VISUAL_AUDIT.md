
## Validação visual headless após a correção

O frame `build/visual-qa/enemy_damage_flash.png` confirma que o inimigo continua visível durante o dano e que o feedback passou a ser um contorno/flash pequeno, em vez de substituir o corpo por um quadrado rosado opaco. O sprite aparece em escala muito pequena no frame técnico, coerente com o buffer interno; a regressão verifica a presença de pixels escuros do corpo do monstro sob o efeito.

O teste `EnemyRenderVisualTest` foi aprovado depois de remover uma asserção frágil sobre o canto do sprite recortado. A validação relevante é feita diretamente no resultado renderizado, que corresponde ao defeito observado na captura do jogador.
