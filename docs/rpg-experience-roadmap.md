# Roadmap de experiência RPG

## Diagnóstico atual

O jogo já possui regiões, NPCs, missões secundárias, dungeons e chefes regionais, mas o ciclo principal ainda é percebido como linear: entrar na fase, falar com um NPC, eliminar inimigos e avançar. A HUD compacta também reduz títulos e progresso até ficarem difíceis de ler. Os objetivos de resistência contam tempo, porém não possuem um encontro próprio garantido; por isso o cronômetro pode avançar sem que o jogador perceba uma ameaça.

## Ajustes imediatos

A missão principal deve ocupar um card maior, com título em uma linha própria, descrição/progresso em uma segunda linha, barra de progresso quando houver canalização ou sobrevivência e um indicador de ameaça. O card secundário deve continuar visível, porém abaixo do principal e com leitura independente.

Objetivos de resistência devem gerar encontros periódicos a partir de uma posição segura distante do jogador. O primeiro encontro deve aparecer nos primeiros segundos, seguido por lotes moderados e um alerta visual. A conclusão deve continuar baseada no tempo, mas a presença de inimigos deve transformar o período em uma atividade jogável, não em espera passiva.

A defesa da fase 2 deve usar um canal curto, regressão moderada por invasor e uma densidade inicial baixa. O jogador precisa conseguir manter o controle do ponto sem perder toda a progressão por um único inimigo, e deve receber aviso claro quando a área está segura ou sob ataque.

A economia de mana deve preservar a fantasia de armas especiais sem impedir o uso básico. O canhão padrão continua econômico, armas automáticas têm custos por segundo controlados e pickups/pausas de recuperação devem aparecer antes de uma sequência longa de combate. A dificuldade dos mobs deve crescer por encontro, não por saturação imediata do mapa.

O ESC deve ser uma ação global confiável. O Canvas precisa recuperar foco após cliques, transições, abertura de loja, diálogos e retorno de overlays. Em qualquer estado modal, o ESC fecha apenas o modal atual; em jogo normal, abre a pausa.

## Loop RPG entre fases

Entre fases, o jogador deve ter uma escolha de atividade em vez de apenas uma confirmação de avanço. O fluxo recomendado é um hub seguro regional com NPCs, oficina, mapa e três destinos opcionais: seguir a missão principal, realizar uma missão secundária ou explorar um ponto de interesse/dungeon. A atividade escolhida concede recompensa diferente e altera a preparação da próxima fase.

Eventos regionais devem surgir de forma opcional: resgate de sobreviventes, comboio de suprimentos, emboscada, caça a elite e recuperação de dados. Cada evento deve ter uma condição clara, um risco controlado e uma recompensa visível. Dungeons devem funcionar como conteúdo de risco e recompensa, com chefe, recompensa regional e conclusão persistente.

A progressão deve combinar quatro camadas: créditos e compras imediatas, melhorias permanentes do piloto, reputação/registro regional e desbloqueios narrativos. Assim, o jogador pode explorar sem atrasar a campanha, mas ainda sente que cada desvio produz consequência.

## Critérios de aceitação

| Área | Critério |
|---|---|
| Legibilidade | O título e o progresso da missão principal são legíveis em resolução padrão, sem truncamento agressivo. |
| Resistência | Um encontro de mobs aparece nos primeiros segundos e novos lotes surgem durante o timer. |
| Defesa | O canal mostra claramente área segura, invasores e regressão sem punição desproporcional. |
| Mana | O jogador consegue usar a arma básica continuamente e armas avançadas por janelas coerentes, com recuperação acessível. |
| ESC | O ESC abre a pausa em jogo normal após qualquer sequência de mouse, diálogo, loja ou transição. |
| RPG | Pelo menos uma atividade opcional entre fases oferece recompensa e não força avanço linear imediato. |
| Persistência | Missões, dungeons concluídas, créditos e decisões regionais sobrevivem a save/load. |
