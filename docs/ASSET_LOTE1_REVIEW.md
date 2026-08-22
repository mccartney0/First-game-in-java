# Parecer de aceitação — First Game RPG, Lote 1

**Origem:** `first_game_rpg_lote1.zip` enviado pelo titular do projeto em 22 de agosto de 2026.  
**Princípio de segurança:** o ZIP original permanece intacto; a integração usa cópias catalogadas e derivados gerados.

## Resultado da triagem técnica

Os **29 PNGs** foram abertos como imagens, sem executar conteúdo do ZIP. As dimensões, os modos de cor e o canal alfa foram medidos diretamente. As 24 sheets de animação possuem grade exata de três células, o tileset possui 8 × 8 células de 32 px, o retrato mede 96 × 96 px e as peças de ponte medem 32 × 32 px. Nenhuma grade possui pixels restantes.

| Grupo | Contrato verificado | Resultado |
|---|---|---|
| Herói | 8 sheets de 96 × 32 px, RGBA, três células de 32 × 32 px | **Aprovado com ressalva** |
| Lobo da Bruma | 8 sheets de 96 × 32 px, RGBA, três células de 32 × 32 px | **Aprovado** |
| Titã da Bruma | 8 sheets de 288 × 96 px, RGBA, três células de 96 × 96 px | **Bloqueado por direção de arte** |
| Clareira | Tileset RGBA de 256 × 256 px, grade 8 × 8 de 32 px | **Aprovado para uso parcial** |
| Ponte de madeira | Três PNGs RGBA de 32 × 32 px | **Aprovado** |
| Retrato de Ava | PNG RGBA de 96 × 96 px | **Aprovado** |

## Movimento, pivô e leitura

Todos os frames aprováveis preservam o pivô inferior entre as células: o personagem não “flutua” no ciclo. O lobo apresenta três poses distintas nas oito sheets e possui silhueta clara de quadrúpede em quatro direções. O herói apresenta poses distintas em 22 das 24 verificações; nas sheets `hero_walk_left_sheet.png` e `hero_walk_right_sheet.png`, o primeiro e o terceiro frame são idênticos. Ele pode entrar como melhoria provisória, mas a versão final deve trocar o terceiro frame pela passada oposta.

> Uma animação de três poses deve conter **apoio inicial, passagem e apoio oposto**. Repetir a primeira pose como terceira reduz a caminhada a duas poses e deixa o ciclo mecânico.

O Titã possui grade, alfa e pivô corretos, mas não pode ser aceito como o chefe do cenário. A silhueta atual lê como **robô metálico quadrado**, enquanto a especificação do jogo exige um guardião ancestral de pedra, raízes, musgo e bruma. Além disso, todas as suas sheets repetem a primeira pose como recuperação. Ele fica fora do APK até receber uma substituição aprovada ou uma autorização explícita para uso provisório.

## Uso no mapa

O tileset melhora a paleta agressiva e repetitiva vista na captura: grama, terra e água possuem tons menos elétricos e mais compatíveis entre si. A folha ainda não contém uma matriz completa de cantos, árvores e paredes; por isso, esta entrega somente substitui os terrenos base que ela cobre e preserva os elementos ainda ausentes do atlas de fallback. A ponte e o retrato entram como assets próprios, sem alterar a arte-fonte.

## Decisão de integração

| Asset | Decisão | Observação de runtime |
|---|---|---|
| `hero_*` | Integrar com ressalva | Usar os 24 PNGs individuais; substituir as duas sheets horizontais na próxima revisão. |
| `enemy_moss_wolf_*` | Integrar | Adicionar carregamento e seleção direcional para o tipo Lobo da Bruma. |
| `mist_clearing_tileset.png` | Integrar parcialmente | Usar células de grama, caminho e água; manter árvores e muralhas anteriores enquanto faltarem peças equivalentes. |
| `wood_bridge_*.png` | Catalogar e integrar onde a orientação for compatível | Evitar girar ou cortar a arte no pipeline. |
| `npc_commandant_ava_portrait.png` | Integrar | Exibir no diálogo compacto da Comandante. |
| `boss_mist_titan_*` | Não integrar | Aguardar sheets de três poses únicas que respeitem o conceito do Titã. |

## Reenvio necessário para o Titã e para a caminhada lateral do herói

Envie somente os arquivos revisados, preservando os nomes existentes. Para o Titã, cada arquivo deve continuar em **288 × 96 px**, com três células de **96 × 96 px**, mas precisa exibir antecipação, golpe/passada e recuperação distintos. Para o herói, reenvie `hero_walk_left_sheet.png` e `hero_walk_right_sheet.png` em **96 × 32 px** com a passada oposta na terceira célula.

## Integração executada em ambiente de runtime

Os PNGs enviados foram preservados em `res/assets/incoming/lote1/`, junto de um manifesto de hashes. O script versionado `scripts/import_lote1_assets.py` gera apenas cópias derivadas: 24 frames do herói, 24 frames do Lobo da Bruma e cinco assets de mundo em `res/assets/generated/`. Nenhum arquivo-fonte do lote é regravado e os oito sheets do Titã não são copiados para o pacote Android.

| Área do RPG | Alteração aplicada | Proteção mantida |
|---|---|---|
| Herói | Os 24 frames são carregados pelo ciclo direcional existente. | O fallback anterior continua ativo se um PNG não puder ser aberto. |
| Lobo da Bruma | O tipo `WOLF` usa os frames direcionais aprovados durante o movimento. | Vida, dano, colisão e IA permanecem inalterados. |
| Clareira | Grama, caminho e água usam células explícitas do novo tileset; a ponte recebe começo, meio e fim. | Árvores, muralhas e tiles que não existem na folha seguem no atlas anterior. |
| Diálogo de Ava | O retrato 96 × 96 entra em uma caixa compacta na parte inferior da tela. | Inventário, controles e mensagem de missão não são cobertos pela nova caixa. |
| Titã da Bruma | Nenhuma arte do chefe foi vinculada ao runtime. | O fallback permanece até a arte aprovada chegar. |

## Verificação de entrega

O importador concluiu a geração de **24 frames do herói**, **24 frames do Lobo da Bruma** e **5 assets de mundo**. Em seguida, `./gradlew test validateContent --no-daemon` terminou com zero erros e zero avisos, e `./gradlew -p androidApp assembleDebug --no-daemon` gerou o APK de depuração com sucesso. A melhoria de composição reduz a caixa de diálogo para a faixa inferior segura, usa fundo de viewport azul-petróleo em vez de preto absoluto e preserva pixels sem filtragem.
