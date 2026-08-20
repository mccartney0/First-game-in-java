
## Segunda inspeção

A nova prévia de `how_to_play.png` ficou contida no painel, com fonte de conteúdo reduzida, seções destacadas e todos os controles legíveis.

A prévia de `options.png` revelou um problema de apresentação: a tela exibe chaves internas (`menu.options`, `menu.music`, `menu.music_volume`, etc.) em vez dos rótulos localizados. Isso precisa ser corrigido antes da entrega para cumprir o objetivo de melhorar as opções.

## Validação final das opções

Com `build/resources/main` no classpath, a tela de opções mostrou os rótulos em português (`Opções`, `Música`, `Trilha sonora`, `Efeitos sonoros`, `Dificuldade`, `Idioma`, `Voltar`). Após aumentar a margem inferior do painel e afastar o hint, `Voltar` e `←/→ ajustar  ENTER alternar  ESC voltar` ficaram separados e legíveis.

## Tela principal final

A prévia final de `main.png` mostra `Jogar` como primeira opção visível, seguido por `Continuar`, `Carregar jogo`, `Como jogar`, `Opções`, `Melhorias do piloto`, `Nova campanha+` e `Sair`. O título `>TRADUVERT<` aparece uma única vez, acima do painel, sem a renderização antiga escurecida por trás.
