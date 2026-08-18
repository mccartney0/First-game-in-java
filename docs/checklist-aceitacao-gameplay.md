# Checklist de aceitação e próximos passos de gameplay

**Projeto:** First Game in Java  
**Escopo:** campanha principal, salvamento, transições, modo infinito e experiência do jogador  
**Base da validação:** 46 testes auxiliares executados após build limpo; 46 passaram.

## 1. Correções concluídas nesta rodada

| ID | Correção | Resultado |
|---|---|---|
| FIX-01 | O gerador procedural agora cria automaticamente o diretório `bin/` antes de gravar `proc_level_N.png`. | O modo infinito funciona a partir de um clone limpo, sem depender de um diretório previamente criado. |
| FIX-02 | `SaveLoadLogicTest` foi atualizado para validar o schema de salvamento v4, utilizado pelo `SaveManager`. | O teste de salvamento passou integralmente. |
| FIX-03 | `TeleportPadTest.java` passou a declarar a classe pública `TeleportPadTest`, igual ao nome do arquivo. | O teste pode ser compilado e executado diretamente. |

A validação automatizada final terminou com **46/46 testes aprovados**, incluindo objetivos variados, transições, diálogos, inventário, armas, companions, mapas procedurais, modo infinito, save/load, HUD, loja e teletransportes.

## 2. Próximos passos de desenvolvimento de gameplay

### 2.1 Playthrough manual completo da campanha

O próximo ciclo deve validar a campanha como experiência de jogador, não apenas como conjunto de regras isoladas. Faça uma partida limpa da fase 1 até a fase 8, registrando o tempo aproximado, a instrução apresentada ao jogador, o alvo indicado pelo waypoint, o gatilho de conclusão e a tela exibida na transição.

A prioridade é detectar situações em que os testes lógicos passam, mas o jogador não entende **o que fazer**, **para onde ir** ou **por que a fase ainda não terminou**. Cada fase deve ser concluível sem recorrer ao código, aos nomes dos arquivos de mapa ou a procedimentos externos.

### 2.2 Onboarding e clareza do primeiro ciclo

A fase 1 deve ensinar uma mecânica por vez. O jogador precisa receber, em sequência curta, instruções sobre movimentação, ataque, interação, troca de arma, dash, coleta de itens e salvamento. A mensagem não deve cobrir a HUD nem desaparecer antes de poder ser lida.

O primeiro objetivo deve informar explicitamente o alvo e sua localização aproximada. Se houver NPC, beacon ou item obrigatório, o waypoint, o minimapa e o texto da missão precisam apontar para o mesmo destino.

### 2.3 Objetivos e feedback de progresso

Todos os objetivos devem apresentar três estados legíveis: condição inicial, progresso atual e condição de conclusão. Em objetivos compostos, a HUD deve indicar o estágio ativo, por exemplo, conversar com o NPC, defender o beacon e depois derrotar o chefe.

Quando o objetivo estiver bloqueado por uma condição específica, a mensagem deve explicar a causa. Exemplos: “fale com a Engenheira Nia”, “permaneça junto ao beacon por 10 segundos” ou “derrote o comandante depois de concluir a defesa”. O texto genérico “Localize o beacon” deve aparecer apenas quando o beacon realmente ainda não foi criado ou registrado.

### 2.4 Ritmo e balanceamento da campanha

Depois do playthrough, registre para cada fase o tempo até o primeiro objetivo, o tempo total, o número de mortes, o dano recebido, o uso de cura, a munição restante, o mana restante e o número de inimigos derrotados. Esses dados devem orientar o balanceamento, não apenas a percepção de dificuldade.

A revisão deve começar pela fase 1, pela defesa da fase 2 e pelo conjunto de fases 6–8. O início precisa ser acessível, enquanto o final deve exigir domínio das armas, dos companions e da leitura dos objetivos. Evite adicionar novas armas ou chefes antes de estabilizar essa curva.

### 2.5 Derrota, reinício e recuperação

O fluxo após a morte deve ser testado em todas as situações relevantes: antes de falar com o NPC, durante um objetivo composto, após ativar parcialmente um beacon, depois de obter itens e no modo infinito. Ao recarregar, o jogador deve retornar a um estado consistente, sem inimigos ressuscitados indevidamente, objetivos duplicados, companions invisíveis ou progresso perdido.

A tela de game over deve explicar claramente as opções disponíveis. Reiniciar, carregar o save e retornar ao menu não devem parecer a mesma ação nem apagar progresso sem confirmação.

### 2.6 Transições e recompensas

A transição entre fases deve preservar a sensação de conclusão. O jogador deve ver o resultado da fase, pontuação, combo, recompensas e, quando aplicável, a loja ou a tela de estatísticas. Inimigos e projéteis da fase anterior não podem continuar atacando durante a transição.

As recompensas devem reforçar o estilo de jogo: armas novas devem ter função clara, companions devem apresentar benefícios diferentes e as skins devem ser visuais, sem criar uma vantagem obrigatória ou quebrar o balanceamento.

### 2.7 Modo infinito

O modo infinito deve ser tratado como um loop separado da campanha. A cada profundidade, o jogador precisa perceber progressão por meio de layout, densidade, elites, chefe, recompensas e lore. O aumento de dificuldade deve ser gradual, evitando que a geração procedural produza uma sequência injusta de salas fechadas, inimigos sobrepostos ou falta de recursos.

Valide pelo menos as profundidades 1, 3, 5, 9 e 10. Em cada uma, confira navegabilidade, presença de spawn seguro, chefe, quantidade de inimigos, geração de recursos, retorno ao mapa após a transição e persistência do recorde.

### 2.8 Conteúdo novo somente após o release candidate

Depois que a checklist abaixo estiver aprovada, o próximo conteúdo recomendado é uma expansão vertical pequena: uma nova fase ou um novo objetivo com uma mecânica inédita. A expansão deve ser acompanhada de mapa, tutorial contextual, recompensa, teste lógico, teste de save/load e teste de transição.

A recomendação é não adicionar simultaneamente nova fase, novas armas, novos inimigos e novo sistema de progressão. Um único eixo novo por rodada torna o balanceamento e a depuração controláveis.

## 3. Checklist de aceitação

### 3.1 Build e execução

- [x] Um clone limpo possui um JDK completo e executa `./gradlew clean build` sem falhas.
- [x] O JAR é gerado em `build/libs/`.
- [x] O jogo inicia sem exceções no loop principal.
- [x] O modo infinito cria o diretório de mapas procedurais automaticamente.
- [ ] O README documenta explicitamente a exigência de um JDK, e não apenas de um JRE.
- [ ] O jogo é executado em uma máquina Windows limpa usando as instruções documentadas.

### 3.2 Campanha: fases 1–8

- [ ] Uma partida nova inicia na fase 1 sem save residual.
- [ ] O tutorial inicial explica movimentação, ataque e interação sem cobrir elementos essenciais da HUD.
- [ ] Cada fase exibe título, descrição, estágio ativo e condição de conclusão.
- [ ] O waypoint e o minimapa apontam para o alvo correto do estágio atual.
- [ ] NPCs obrigatórios ficam em áreas acessíveis e podem ser identificados visualmente.
- [ ] Beacons obrigatórios são criados, registrados e recriados corretamente após morte e load.
- [ ] Objetivos compostos avançam somente após o estágio anterior ser concluído.
- [ ] Chefes aparecem uma vez por fase e permanecem derrotados após o fluxo de conclusão.
- [ ] As fases 1–8 podem ser concluídas em uma sessão manual sem intervenção externa.
- [ ] Nenhuma transição deixa inimigos, projéteis ou efeitos da fase anterior ativos.

### 3.3 Salvamento e recuperação

- [x] O schema v4 do save passa na validação automatizada.
- [x] O salvamento restaura sessão, progresso, campanha, objetivo, score e timestamp.
- [x] O salvamento preserva recorde, diálogos de NPC e estado do companion quando aplicável.
- [ ] Salvar antes da morte e carregar depois da morte restaura um estado jogável.
- [ ] Carregar um save durante cada tipo de objetivo não duplica NPCs, beacons ou inimigos.
- [ ] Um save corrompido ou incompleto produz fallback compreensível, sem crash silencioso.
- [ ] O retorno ao menu não altera o estado do save ativo sem confirmação.

### 3.4 Combate, progressão e economia

- [ ] O jogador recebe recursos suficientes para compreender a primeira arma.
- [ ] Cada arma desbloqueada tem uma função perceptível e uma desvantagem compreensível.
- [ ] Vida, escudo, mana e munição são legíveis em todos os estados da HUD.
- [ ] O dash, o combo e as habilidades exibem cooldown ou disponibilidade corretos.
- [ ] Inimigos comuns, variantes, elites e chefes possuem papéis distinguíveis.
- [ ] A dificuldade aumenta sem exigir dano inevitável ou conhecimento prévio do mapa.
- [ ] A loja mostra preço, efeito e consequência da compra antes da confirmação.
- [ ] Companions e skins são restaurados corretamente e não desaparecem após transições.

### 3.5 Modo infinito

- [x] A transição da fase 8 para o modo infinito passa nos testes automatizados.
- [x] Mapas procedurais são gerados em profundidades diferentes e permanecem válidos.
- [x] A geração procedural é determinística para a mesma profundidade.
- [ ] Profundidades 1, 3, 5, 9 e 10 são validadas visualmente.
- [ ] Cada mapa possui spawn seguro, caminho navegável e chefe alcançável.
- [ ] A densidade de inimigos e elites cresce de forma gradual.
- [ ] O respiro entre ondas impede dano imediato durante a transição.
- [ ] Recorde de profundidade, score, kills e combo são persistidos sem sobrescrever o melhor resultado.

### 3.6 UX, áudio e apresentação

- [ ] Nenhum texto de NPC, missão, inventário ou habilidade se sobrepõe.
- [ ] A HUD permanece legível em janela padrão e tela cheia.
- [ ] O mapa preserva a proporção e o pixel art em resoluções 16:9 comuns.
- [ ] A pausa interrompe efetivamente o combate e o diálogo.
- [ ] Os sons de tiro, dano, coleta, diálogo, chefe, compra e transição têm volume equilibrado.
- [ ] Música, efeitos e volume geral podem ser ajustados nas opções.
- [ ] O game over, a vitória e a transição possuem feedback visual e sonoro distintos.

### 3.7 Critérios para liberar uma nova versão

- [x] Build limpo aprovado.
- [x] Regressão automatizada aprovada: 46/46 testes.
- [ ] Playthrough manual completo da campanha aprovado.
- [ ] Playthrough manual do modo infinito aprovado nas profundidades definidas.
- [ ] Nenhum bug bloqueador aberto.
- [ ] Nenhuma perda de save reproduzível.
- [ ] Instruções de execução atualizadas.
- [ ] Versão marcada no Git e pacote distribuível testado fora do ambiente de desenvolvimento.

## 4. Ordem recomendada para a próxima rodada

| Ordem | Entrega | Critério de conclusão |
|---:|---|---|
| 1 | Atualizar README e preparar execução em clone limpo | Outro desenvolvedor compila e inicia o jogo sem conhecimento do histórico do projeto. |
| 2 | Fazer playthrough manual das fases 1–4 | Objetivos, waypoints, NPCs, beacons e transições são compreensíveis e concluíveis. |
| 3 | Fazer playthrough manual das fases 5–8 | Chefes, objetivos compostos, recompensas e salvamento permanecem consistentes. |
| 4 | Validar morte, reload e game over | Nenhum estado de progresso fica bloqueado ou duplicado. |
| 5 | Validar modo infinito em profundidades selecionadas | Mapas, elites, recursos, respiro e recordes permanecem justos e estáveis. |
| 6 | Balancear a curva de dificuldade | A dificuldade cresce sem picos injustificados e sem escassez acidental de recursos. |
| 7 | Polir tutorial, HUD e feedback | O jogador entende o próximo objetivo sem consultar documentação externa. |
| 8 | Criar release candidate | Build, pacote, instruções e checklist são aprovados em uma máquina limpa. |
| 9 | Só então adicionar novo conteúdo | A nova mecânica possui tutorial, recompensa e cobertura de regressão. |

> **Definição de pronto:** o game só deve avançar para uma nova fase de conteúdo quando a regressão automatizada continuar verde e todas as marcações manuais das seções de campanha, recuperação e modo infinito estiverem aprovadas.

## Referências

[1]: https://github.com/mccartney0/First-game-in-java/blob/main/src/com/traduvertgames/world/ProceduralLevelGenerator.java "Gerador de mapas procedurais"

[2]: https://github.com/mccartney0/First-game-in-java/blob/main/src/com/traduvertgames/main/SaveManager.java "SaveManager e schema v4"

[3]: https://github.com/mccartney0/First-game-in-java/blob/main/tools/SaveLoadLogicTest.java "Teste de salvamento"

[4]: https://github.com/mccartney0/First-game-in-java/blob/main/tools/rodada26_diagnostico_bugs.md "Diagnóstico recente de bugs de gameplay"

[5]: https://github.com/mccartney0/First-game-in-java/blob/main/README.md "README e instruções do projeto"
