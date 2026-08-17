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
		int lineStep = bodyFont.getSize() + 2;

		String text = getCurrentLine();
		String speaker = getSpeakerName();
		String progress = getProgressLabel();
		String hint = isLastLine() ? "Enter para concluir" : "Enter para continuar";

		g.setFont(bodyFont);
		int panelWidth = Math.min(screenWidth - 40, 560 * scale / 4 + 60);
		int panelHeight = 108 * scale / 4 + 20;
		int panelX = (screenWidth - panelWidth) / 2;
		int panelY = screenHeight - panelHeight - 16;

		int lineX = panelX + 16;
		int maxLineW = panelWidth - 32;
		int headerY = 26; // espaço até a primeira linha de texto (nome + margem)

		// 1) Primeiro passe: contar quantas linhas o texto ocupa (sem desenhar),
		// para dimensionar o painel à necessidade real do conteúdo — evita que
		// falas longas ou escolhas extensas escrevam por cima do rodapé ou
		// fiquem cortadas fora do painel (bug reportado na rodada 25).
		g.setFont(bodyFont);
		int bodyLines = countWrapLines(text, maxLineW, g);

		// Escolhas numeradas do diálogo ramificado (rodada 22) — linha extra cada.
		String[] choices = getBranchChoices();
		int choiceLines = 0;
		int[] choiceLineCounts = new int[choices.length];
		for (int i = 0; i < choices.length; i++) {
			if (choices[i] == null || choices[i].isEmpty()) {
				continue;
			}
			String choiceText = (i + 1) + ". " + choices[i];
			int n = countWrapLines(choiceText, maxLineW - 14, g);
			choiceLineCounts[i] = n;
			choiceLines += n;
		}

		int footerHeight = hintFont.getSize() + 8;
		// Altura: nome + margem, linhas de texto, espaço das escolhas, rodapé.
		int neededHeight = headerY + bodyLines * lineStep + choiceLines * lineStep
				+ footerHeight + 16;
		panelHeight = Math.max(108 * scale / 4 + 20, Math.min(neededHeight, screenHeight - 32));
		panelY = screenHeight - panelHeight - 16;
		int footerY = panelY + panelHeight - 12;

		// 2) Segundo passe: desenhar o painel e todo o conteúdo com as posições
		// já calculadas. O rodapé é desenhado por último, sempre dentro do painel.
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
		int lineY = panelY + headerY;
		drawWrappedLines(text, lineX, lineY, maxLineW, lineStep, g);
		int contentBottom = lineY + bodyLines * lineStep;

		// Escolhas numeradas dentro do painel (limitadas ao espaço disponível)
		int choicesLineY = contentBottom;
		for (int i = 0; i < choices.length; i++) {
			if (choices[i] == null || choices[i].isEmpty()) {
				continue;
			}
			// Espaço de escolhas: não invadir a área reservada ao rodapé.
			if (choicesLineY + lineStep > footerY - 10) {
				break;
			}
			String choiceText = (i + 1) + ". " + choices[i];
			int n = choiceLineCounts[i];
			if (g.getFontMetrics().stringWidth(choiceText) > maxLineW) {
				wrapText(choiceText, lineX + 14, choicesLineY, maxLineW - 14, lineStep,
						Math.min(n, (footerY - 10 - choicesLineY) / lineStep), g);
			} else {
				g.drawString(choiceText, lineX, choicesLineY);
				choicesLineY += lineStep;
			}
		}

		// Rodapé com progresso e dica de avanço (por último — nunca sobreposto)
		g.setFont(hintFont);
		g.setColor(new Color(176, 190, 197));
		String footerHint = choices.length > 0 ? "Digite 1-3 para escolher, Enter para a primeira" : hint;
		g.drawString(footerHint, panelX + 16, footerY);
		int progW = g.getFontMetrics().stringWidth(progress);
		g.drawString(progress, panelX + panelWidth - progW - 16, footerY);
	}

	/** Contagem de linhas que o texto ocupa quando quebrado na largura. */
	private static int countWrapLines(String text, int maxLineW, Graphics g) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		String[] words = text.split(" ");
		int lines = 1;
		StringBuilder line = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (g.getFontMetrics().stringWidth(candidate) > maxLineW && line.length() > 0) {
				lines++;
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		return lines;
	}

	/** Desenha o texto quebrado em linhas a partir da posição informada. */
	private static void drawWrappedLines(String text, int lineX, int lineY,
			int maxLineW, int lineStep, Graphics g) {
		String[] words = text.split(" ");
		StringBuilder line = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (g.getFontMetrics().stringWidth(candidate) > maxLineW && line.length() > 0) {
				g.drawString(line.toString(), lineX, lineY);
				lineY += lineStep;
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if (line.length() > 0) {
			g.drawString(line.toString(), lineX, lineY);
		}
	}

	/** Quebra o texto e desenha no máximo {@code maxLines} linhas (reticências se cortar). */
	private static void wrapText(String text, int lineX, int lineY, int maxLineW,
			int lineStep, int maxLines, Graphics g) {
		String[] words = text.split(" ");
		StringBuilder line = new StringBuilder();
		int drawn = 0;
		boolean cut = false;
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (g.getFontMetrics().stringWidth(candidate) > maxLineW && line.length() > 0) {
				if (drawn < maxLines) {
					g.drawString(line.toString(), lineX, lineY);
					lineY += lineStep;
					drawn++;
				} else {
					cut = true;
				}
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if (line.length() > 0 && drawn < maxLines) {
			String last = cut && g.getFontMetrics().stringWidth(line + "...") > maxLineW
					? line.substring(0, Math.max(1, maxLineW / g.getFontMetrics().stringWidth("a"))) : line.toString();
			g.drawString(last + (cut ? "..." : ""), lineX, lineY);
		}
	}

	/**
	 * Interface opcional para NPCs que definem falas próprias (missões com
	 * roteiro, escolhas ou objetivos narrativos).
	 */
	public interface DialogueProvider {
		String[] getDialogueLines();
	}
}
