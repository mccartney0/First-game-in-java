# Diretrizes do Repositório

Bem-vindo ao projeto **First Game in Java**. Este repositório contém um jogo simples desenvolvido em Java que serve como base para experimentos com desenvolvimento de jogos 2D, incluindo renderização básica, controle de entidades e gerenciamento de recursos.

## Visão geral do projeto

- **Estrutura do código:** o código-fonte principal está em `src/main/java`. As classes mais importantes estão organizadas em pacotes que lidam com lógica de jogo, renderização e entrada do usuário.
- **Recursos:** arquivos de imagem, áudio e configurações ficam em `res/`. Garanta que novos recursos sejam nomeados de forma consistente e acompanhados de documentação no `README.md`.
- **Documentação:** o diretório `docs/` agrega material de apoio, fluxos de trabalho e anotações históricas do desenvolvimento.

## Diretrizes de contribuição

### Fluxo de trabalho recomendado

- Utilize sempre o Gradle Wrapper (`./gradlew`) para compilar, executar e testar o projeto (`./gradlew build`, `./gradlew run`).
- Antes de abrir um PR, execute `./gradlew check` e garanta que não há falhas ou *warnings* novos no console.
- Se adicionar dependências, documente o motivo na seção "Dependências" do `README.md` e confirme que o cache do Gradle foi atualizado quando necessário.

### Qualidade de código e estilo

- Arquivos-fonte Java devem usar indentação de 4 espaços e seguir as convenções padrão de nomenclatura em Java (classes em PascalCase, métodos e variáveis em camelCase).
- Prefira extrair constantes reutilizáveis para classes `Config` ou `Constants` dentro do pacote correspondente.
- Inclua comentários somente quando o comportamento não for evidente pelo nome do método ou variável. Comentários devem ser breves e atualizados.

### Documentação e comunicação

- Mantenha a documentação atualizada em conjunto com qualquer alteração de código ou recurso. O `README.md` é a referência inicial para usuários e contribuidores.
- Mensagens de *commit* devem ser descritivas e contextualizar a alteração (ex.: "Implementa movimentação do jogador" ou "Atualiza sprites do inimigo").
- Ao abrir um PR, descreva claramente o impacto na jogabilidade ou nas ferramentas e adicione capturas de tela ou GIFs quando houver mudanças visuais.

## Próximos passos sugeridos

1. **Melhorias de jogabilidade:** adicionar novos inimigos, power-ups ou níveis para ampliar a rejogabilidade.
2. **Sistema de pontuação:** implementar e exibir um placar que registre conquistas do jogador.
3. **Testes automatizados:** ampliar a cobertura com testes unitários e, se possível, testes de integração para as principais mecânicas. Considere usar JUnit 5 e Mockito para isolar comportamentos complexos.
4. **Pipeline de assets:** documentar e padronizar o processo de criação/otimização de sprites e sons no diretório `res/`. Utilize *sheets* otimizadas para reduzir o uso de memória.
5. **Localização:** avaliar a viabilidade de internacionalização das strings principais do jogo para PT-BR e EN.

Obrigado por ajudar a manter e evoluir o projeto!
