# Visualização de metadados no Content Studio

A aba **Metadados** do Content Studio permite selecionar uma arma ou uma variante de inimigo e visualizar o sprite real usado pelo runtime, seus metadados de combate e os pontos de origem do disparo.

## Como usar

Execute o Content Studio pela raiz do projeto:

```bash
./gradlew runContentStudio
```

Depois abra a aba **Metadados**. Escolha **Armas** ou **Inimigos** e selecione o item desejado.

O painel esquerdo mostra a imagem com um fundo quadriculado. A **cruz azul** representa o centro do sprite. O **marcador vermelho** representa o ponto de disparo normalizado. O painel direito mostra dano, cadência, velocidade, vida, alcance e demais campos realmente definidos nas classes do jogo.

## Contrato de coordenadas

Os pontos de disparo das armas são armazenados em `WeaponType` usando coordenadas normalizadas entre `0.0` e `1.0`:

```text
(0.0, 0.0) = canto superior esquerdo
(1.0, 1.0) = canto inferior direito
```

Isso permite trocar a resolução do sprite sem perder o posicionamento relativo do cano. As variantes de inimigos que possuem projéteis são exibidas com um ponto central provisório, porque o runtime ainda cria o projétil a partir da posição da entidade e não possui um ponto por variante.

## Estado atual da validação

Após a integração, o projeto compila e os testes passam. A tarefa `validateContent` encontra sete erros de conteúdo reais:

| Categoria | Arquivo ou causa | Próxima ação |
|---|---|---|
| Arquivo | `companions/companion_set.png` existe, mas não está registrado. | Registrar como atlas ou remover a cópia solta. |
| Transparência | `enemies/scout_ref.png` contém fundo sólido. | Reexportar com canal alfa ou remover o fundo. |
| Referência | Cinco imagens com rastros de disparo ainda não têm consumidor no runtime. | Conectar a `BulletShoot` ou marcá-las explicitamente como referência. |

Os testes de metadados não acusam mais ausência de dano, cadência ou ponto de disparo nas armas; esses campos agora são visíveis na aba e fazem parte do contrato compartilhado com o validador.
