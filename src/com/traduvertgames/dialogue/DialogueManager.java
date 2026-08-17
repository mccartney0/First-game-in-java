package com.traduvertgames.dialogue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.quest.QuestManager;

/**
 * Gerencia o overlay de diálogo no jogo. Ao abrir uma conversa:
 * - inimigos e objetivos ficam pausados (sem dano ou conclusão acidental);
 * - as falas avançam com Enter/Space até a última linha;
 * - ao concluir, o NPC é marcado como finalizado e o DialogueEventListener
 *   da missão recebe o evento de conclusão;
 * - sons próprios: DIALOGUE_START ao abrir, TUTORIAL_STEP ao avançar a fala.
 *
 * O diálogo não afeta o tutorial/onboarding e vice-versa: durante o
 * onboarding o diálogo não pode ser aberto.
 */
public final class DialogueManager {

	private static InteractiveNpc target = null;
	private static String[] lines = new String[0];
	private static int currentLine = 0;
	private static boolean active = false;

	private DialogueManager() {
	}

	public static boolean isActive() {
		return active;
	}

	public static InteractiveNpc getTarget() {
		return target;
	}

	/**
	 * Encontra o NPC interativo mais próximo do jogador dentro do raio e abre
	 * a conversa. Retorna o NPC iniciado ou null se nada estiver ao alcance.
	 */
	public static InteractiveNpc startNearestDialogue() {
		if (active) {
			return null;
		}
		// O onboarding tem prioridade — sem diálogo durante o treino.
		if (com.traduvertgames.main.OnboardingManager.isActive()) {
			return null;
		}
		if (!"NORMAL".equals(Game.gameState) || Game.player == null) {
			return null;
		}
		InteractiveNpc nearest = null;
		double best = Double.MAX_VALUE;
		for (int i = 0; i < Game.entities.size(); i++) {
			if (Game.entities.get(i) instanceof InteractiveNpc) {
				InteractiveNpc npc = (InteractiveNpc) Game.entities.get(i);
				if (npc.hasFinished() || !npc.isWithinReach()) {
					continue;
				}
				double d = distanceToPlayer(npc);
				if (d < best) {
					best = d;
					nearest = npc;
				}
			}
		}
		if (nearest == null) {
			return null;
		}
		target = nearest;
		// Rodada 22: diálogos ramificados definem a própria árvore de nós
		// (BranchingNpc); falas lineares continuam via buildLines().
		if (nearest instanceof BranchingNpc) {
			lines = new String[] { ((BranchingNpc) nearest).getNodeText() };
		} else {
			lines = buildLines(target);
		}
		currentLine = 0;
		active = true;
		target.startInteraction();
		// Início de diálogo com NPC: som curto de confirmação no momento em
		// que o jogador aperta R (follow-up rodada 20), seguido do som de
		// diálogo da rodada 15.
		SoundManager.play(SoundManager.Event.NPC_INTERACT);
		SoundManager.play(SoundManager.Event.DIALOGUE_START);
		QuestManager.notifyDialogueStarted(target);
		return target;
	}

	/**
	 * Avança a fala (Enter/Space). Na última linha, fecha o diálogo e entrega
	 * a conclusão ao NPC e à missão ativa.
	 */
		public static void advance() {
		if (!active || target == null) {
			return;
		}
		// Rodada 22: diálogo ramificado — Enter avança no texto do nó ou
		// conclui quando o nó é terminal.
		if (target instanceof BranchingNpc) {
			BranchingNpc branch = (BranchingNpc) target;
			if (!branch.hasChoices()) {
				close();
				return;
			}
			// Sem escolha explícita digitada, escolhe a primeira opção.
			selectBranchChoice(0);
			return;
		}
		currentLine++;
		if (currentLine >= lines.length) {
			close();
		} else {
			SoundManager.play(SoundManager.Event.TUTORIAL_STEP);
		}
	}

	/**
	 * Aplica a escolha numerada (0..2) no nó atual de um BranchingNpc:
	 * executa a ação da escolha, troca o nó de texto e toca o som de passo.
	 */
	public static void selectBranchChoice(int choiceIndex) {
		if (!(target instanceof BranchingNpc)) {
			return;
		}
		BranchingNpc branch = (BranchingNpc) target;
		branch.selectChoice(choiceIndex);
		SoundManager.play(SoundManager.Event.TUTORIAL_STEP);
		if (branch.isTerminal()) {
			close();
		} else {
			lines = new String[] { branch.getNodeText() };
			currentLine = 0;
		}
	}

	/** Fecha o diálogo, reativa o mundo e notifica a missão da conclusão. */
	public static void close() {
		if (!active || target == null) {
			return;
		}
		InteractiveNpc npc = target;
		target = null;
		lines = new String[0];
		currentLine = 0;
		active = false;
		SoundManager.play(SoundManager.Event.TUTORIAL_DONE);
		QuestManager.notifyDialogueFinished(npc);
		npc.onDialogueClosed();
	}

	/** Fecha sem notificar a missão (ex.: troca de fase, volta ao menu). */
	public static void stop() {
		if (active && target != null) {
			InteractiveNpc npc = target;
			target = null;
			lines = new String[0];
			currentLine = 0;
			active = false;
			npc.onDialogueClosed();
		}
	}

	/** Inimigos ficam paralisados enquanto o diálogo está ativo. */
	public static boolean isEnemyPaused() {
		return active;
	}

	public static String getSpeakerName() {
		return target != null ? target.getName() : "";
	}

	public static String getCurrentLine() {
		if (lines == null || lines.length == 0 || currentLine >= lines.length) {
			return "";
		}
		return lines[currentLine];
	}

	public static boolean isLastLine() {
		// Rodada 22: em nós ramificados, o último texto indica que existem
		// escolhas numeradas (Enter conclui ou seleciona a primeira).
		if (target instanceof BranchingNpc) {
			return true;
		}
		return lines.length == 0 || currentLine >= lines.length - 1;
	}

	/** Escolhas do nó atual do diálogo ramificado (rodada 22). */
	public static String[] getBranchChoices() {
		if (target instanceof BranchingNpc) {
			BranchingNpc branch = (BranchingNpc) target;
			BranchingNpc.DialogueNode node = branch.getNode();
			if (node != null && node.choiceTexts != null) {
				return node.choiceTexts;
			}
		}
		return new String[0];
	}

	private static double distanceToPlayer(InteractiveNpc npc) {
		if (Game.player == null) {
			return Double.MAX_VALUE;
		}
		double dx = npc.getX() - Game.player.getX();
		double dy = npc.getY() - Game.player.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	public static String getProgressLabel() {
		return (currentLine + 1) + "/" + lines.length;
	}

	/**
	 * Monta as falas do NPC: fala padrão do nome do personagem + linhas
	 * definidas. Subclasses/missões podem sobrescrever via DialogueProvider.
	 */
	private static String[] buildLines(InteractiveNpc npc) {
		if (npc instanceof DialogueProvider) {
			String[] custom = ((DialogueProvider) npc).getDialogueLines();
			if (custom != null && custom.length > 0) {
				return custom;
			}
		}
		return new String[] { getGreeting(npc), getFarewell(npc) };
	}

	private static String getGreeting(InteractiveNpc npc) {
		String name = npc.getName();
		switch (name) {
		case "Comandante Ava":
			return "Piloto, a colônia precisa de você. Este é o plano da operação.";
		case "Engenheira Nia":
			return "Seus equipamentos precisam de manutenção. Posso recarregar sua energia.";
		case "Pesquisador Ivo":
			return "Eu sabia que você viria. Tenho informações sobre o que está acontecendo.";
		case "Armeiro Mercúrio":
			return "Cada arma tem um papel na batalha. Quer conhecer o meu arsenal?";
		default:
			return "Olá, piloto. Fico feliz em vê-lo.";
		}
	}

	private static String getFarewell(InteractiveNpc npc) {
		String name = npc.getName();
		switch (name) {
		case "Comandante Ava":
			return "A missão está clara. Boa sorte lá fora, piloto.";
		case "Engenheira Nia":
			return "Pronto — energia restabelecida. Cuide-se, piloto.";
		case "Pesquisador Ivo":
			return "Use o que aprendeu aqui com sabedoria. Até breve.";
		case "Armeiro Mercúrio":
			return "Volte sempre. O melhor armamento é o que você conhece.";
		default:
			return "Até mais, piloto.";
		}
	}

	/**
	 * Renderiza o painel de diálogo sobre o jogo (coordenadas da janela).
	 */
	public static void render(Graphics g) {
		if (!active || target == null) {
			return;
		}
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		int scale = Game.SCALE;
		Font nameFont = new Font("arial", Font.BOLD, 16 * scale / 4 + 2);
		Font bodyFont = new Font("arial", Font.PLAIN, 13 * scale / 4 + 2);
		Font hintFont = new Font("arial", Font.PLAIN, 10 * scale / 4 + 1);

		String text = getCurrentLine();
		String speaker = getSpeakerName();
		String progress = getProgressLabel();
		String hint = isLastLine() ? "Enter para concluir" : "Enter para continuar";

		g.setFont(bodyFont);
		int panelWidth = Math.min(screenWidth - 40, 560 * scale / 4 + 60);
		int panelHeight = 108 * scale / 4 + 20;
		int panelX = (screenWidth - panelWidth) / 2;
		int panelY = screenHeight - panelHeight - 16;

		// Fundo escuro com borda amarela (mesma identidade do onboarding)
		g.setColor(new Color(0, 0, 0, 235));
		g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);
		g.setColor(new Color(255, 235, 59));
		g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);

		// Nome do personagem no topo
		g.setFont(nameFont);
		g.setColor(new Color(255, 235, 59));
		g.drawString(speaker, panelX + 16, panelY + 26);

		// Texto quebrado em linhas dentro do painel
		g.setFont(bodyFont);
		g.setColor(Color.WHITE);
		String[] words = text.split(" ");
		int lineX = panelX + 16;
		int lineY = panelY + 48;
		int maxLineW = panelWidth - 32;
		StringBuilder line = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (g.getFontMetrics().stringWidth(candidate) > maxLineW && line.length() > 0) {
				g.drawString(line.toString(), lineX, lineY);
				lineY += g.getFontMetrics().getHeight() + 2;
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if (line.length() > 0) {
			g.drawString(line.toString(), lineX, lineY);
		}

		// Escolhas numeradas do diálogo ramificado (rodada 22)
		String[] choices = getBranchChoices();
		int choicesLineY = lineY;
		for (int i = 0; i < choices.length; i++) {
			if (choices[i] == null || choices[i].isEmpty()) {
				continue;
			}
			String choiceText = (i + 1) + ". " + choices[i];
			if (g.getFontMetrics().stringWidth(choiceText) > maxLineW) {
				// Quebra da escolha em duas linhas quando exceder o painel.
				g.drawString(choiceText.substring(0, Math.min(choiceText.length(), maxLineW / 7)),
						lineX, choicesLineY);
				choicesLineY += g.getFontMetrics().getHeight() + 2;
				String rest = choiceText.substring(Math.min(choiceText.length(), maxLineW / 7));
				g.drawString(rest, lineX + 14, choicesLineY);
				choicesLineY += g.getFontMetrics().getHeight() + 2;
			} else {
				g.drawString(choiceText, lineX, choicesLineY);
				choicesLineY += g.getFontMetrics().getHeight() + 2;
			}
		}

		// Rodapé com progresso e dica de avanço
		g.setFont(hintFont);
		g.setColor(new Color(176, 190, 197));
		String footerHint = choices.length > 0 ? "Digite 1-3 para escolher, Enter para a primeira" : hint;
		g.drawString(footerHint, panelX + 16, panelY + panelHeight - 12);
		int progW = g.getFontMetrics().stringWidth(progress);
		g.drawString(progress, panelX + panelWidth - progW - 16, panelY + panelHeight - 12);
	}

	/**
	 * Interface opcional para NPCs que definem falas próprias (missões com
	 * roteiro, escolhas ou objetivos narrativos).
	 */
	public interface DialogueProvider {
		String[] getDialogueLines();
	}
}
