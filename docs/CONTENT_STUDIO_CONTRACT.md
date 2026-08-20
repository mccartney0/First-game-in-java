# Contrato de Produção — Content Studio

O **Content Studio** é a ferramenta oficial de geração e exportação de conteúdo para *First Game in Java*. Seus arquivos são produzidos de forma determinística e utilizam os mesmos contratos do carregador `World`, evitando a necessidade de editar código para testar um mapa ou um asset novo.

| Tipo de conteúdo | Formato de saída | Diretório padrão | Contrato de compatibilidade |
| --- | --- | --- | --- |
| Mapa RPG regional | PNG + JSON | `bin/large_rpg_maps/` | PNG ARGB com um pixel por tile; largura mínima de 96 e altura mínima de 64 tiles. |
| Mundo Aberto | PNG + JSON | `bin/open_world_maps/` | PNG ARGB com 512×320 tiles por padrão; 8×5 setores para a grade de exploração. |
| Tile procedimental | PNG | `res/assets/generated/tiles/` | PNG ARGB 32×32, pixels totalmente opacos no desenho e alfa fora dele. |
| Sprite de inimigo | PNG | `res/assets/generated/enemies/` | PNG ARGB transparente, silhueta centralizada em área segura de 28×28 pixels, sem fundo, texto ou moldura. |
| Sprite de companion | PNG | `res/assets/generated/companions/` | PNG ARGB transparente, silhueta centralizada e legível na escala de 16 pixels do jogo. |
| Manifesto | JSON | Ao lado do mapa exportado | Deve registrar dimensões, seed, profundidade e pontos de interesse para inspeção em ferramentas. |

## Cores funcionais do mapa

O mapa é uma imagem de dados, não uma textura desenhada. Cada pixel determina um tile ou uma entidade. O Content Studio delega a geração ao `LargeRpgMapGenerator`, que já escreve as cores válidas e o manifesto correspondente.

| Marca | Uso no jogo |
| --- | --- |
| Preto `#000000` | Solo caminhável/base. |
| Branco `#FFFFFF` | Parede sólida. |
| Cinza `#808080` | Parede destrutível. |
| Azul `#0026FF` | Spawn do jogador. |
| Vermelho `#FF0000` | Inimigo padrão. |
| Ciano `#00BCD4` | Inimigo de artilharia. |
| Roxo `#9C27B0` | Teleporte. |
| Magenta `#AA00FF` | Portal de dungeon. |

## Regras de qualidade do sprite

> Um sprite de inimigo é aceito quando mantém uma **silhueta reconhecível**, alto contraste entre corpo e fundo do mapa, transparência real e uma leitura de papel de combate em 16 pixels lógicos.

O carregador remove margens transparentes, converte pixels de chroma verde `#00FF00` em transparência como proteção para PNGs importados e cria uma cópia normalizada em 32×32 antes da escala final do renderizador. Portanto, o produtor não deve colocar fundo branco, padrão xadrez, molduras ou indicadores de HUD dentro do PNG.

## Fluxo recomendado

1. Abrir `Content Studio` pelo comando Gradle descrito no README.
2. Gerar um mapa regional ou de mundo aberto com seed anotada para reprodução.
3. Testar o PNG no jogo pelo modo correspondente e revisar o JSON de manifesto.
4. Usar a aba de tiles para criar variantes visuais e a aba de assets para exportar sprites transparentes.
5. Para um sprite definitivo, salvar com o nome da variante (`enemy_bomber.png`, `enemy_artillery.png`, entre outros) e substituir o arquivo em `res/assets/generated/enemies/`.
