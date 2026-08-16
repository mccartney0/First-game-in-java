# Teste visual headless (Xvfb :120) — 16/08/2026

- Jogo abre sem exceções no log (/tmp/game.log limpo após compilação).
- Menu principal renderiza corretamente com itens do menu.
- Fase 1 carrega com **MissionHud** no topo esquerdo: "Missão — Contato com o Comando — Fale com a Comandante Ava" + waypoint. OK.
- Aproximar player do NPC (Ava, teal) exibe prompt "R — Comandante Ava" no HUD. OK.
- Tecla R abre overlay de diálogo no rodapé com nome amarelo "Comandante Ava", fala branca "Piloto, a colônia precisa de você. Este é o plano da operação." e "Enter para continuar — 1/2". Inimigos parecem pausados durante o diálogo. OK.
- OnboardingManager: tutorial pular com Space; passos: movimento → atirar (X, 3 tiros) → dash (SHIFT).
- Pendente: testar 2ª fala (Enter), conclusão do contato, e o bug fix do HOW_TO_PLAY (Enter volta ao MAIN) + novas armas visuais (bumerangue) com saves teste.

## Sessão 2 (diálogo concluído)

Após as duas falas (Enter, Enter), o overlay fechou, o card Missão atualizou para "Artefatos: 0/2" (progresso dinâmico OK) e o player recebeu +80 de mana (80/500 — bônus da conversa com a Comandante OK). O waypoint de artefato segue visível no minimapa/HUD. Sistema de missão end-to-end validado: missão → diálogo → progresso → bônus.
