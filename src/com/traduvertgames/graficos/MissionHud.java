package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.traduvertgames.dialogue.DialogueManager;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.world.Camera;

/**
 * Card compacto da missão atual, sempre visível durante a partida (exceto em
 * menus e no onboarding), posicionado no canto superior esquerdo logo abaixo
 * da barra de XP — sem cobrir vida, mana ou a área de jogo.
 *
 * Além do texto, desenha um waypoint (seta) apontando para o alvo atual da
 * missão (personagem a conversar ou ponto a ativar), com um sinal pulsante
 * sobre o minimapa.
 */
public final class MissionHud {

	private static final int PULSE_PERIOD = 60;
	private static final double WAYPOINT_DISTANCE = 64.0;

	private MissionHud() {
	}

	/**
	 * Renderiza o card da missão e o waypoint, se houver alvo ativo.
	 * Coordenadas em espaço de janela (buffer * SCALE).
	 */
	public static void render(Graphics2D g2) {
		if ("MENU".equals(Game.gameState) || "GAMEOVER".equals(Game.gameState)) {
			return;
		}
		if (com.traduvertgames.main.OnboardingManager.isActive()) {
			return;
		}
		int s = Game.SCALE;
		int screenWidth = Game.WIDTH * s;
		int margin = 10 * s / 4 + 2;

		// --- Card compacto da missão (canto superior esquerdo) ---
		String title = QuestManager.getObjectiveTitle();
		String progress = QuestManager.getObjectiveProgress();
		if (title == null || title.isEmpty()) {
			return;
		}

		Font titleFont = new Font("SansSerif", Font.BOLD, 9 * s / 4 + 2);
		Font smallFont = new Font("SansSerif", Font.PLAIN, 7 * s / 4 + 2);

		g2.setColor(new Color(6, 9, 16, 180));
		int cardHeight = 26 * s / 4 + 6;
		int cardWidth = Math.min(screenWidth - margin * 2, 250 * s / 4 + 40);
		g2.fillRoundRect(margin, margin, cardWidth, cardHeight, 8, 8);
		g2.setColor(new Color(255, 235, 59, 160));
		g2.setStroke(new java.awt.BasicStroke(1.5f));
		g2.drawRoundRect(margin, margin, cardWidth, cardHeight, 8, 8);

		g2.setColor(new Color(255, 235, 59));
		g2.setFont(titleFont);
		g2.drawString("Missão", margin + 8, margin + 12 * s / 4 + 2);

		// Largura disponível para o título (respeita o espaço do progresso)
		int maxTitleChars = Math.max(5, (cardWidth - 64) / g2.getFontMetrics(smallFont).stringWidth("m"));
		String shortTitle = title.length() > maxTitleChars ? title.substring(0, maxTitleChars - 3) + "..." : title;
		g2.setColor(Color.WHITE);
		g2.setFont(smallFont);
		g2.drawString(shortTitle, margin + 8, margin + cardHeight - 6);

		// Progresso no lado direito do card, medido pela largura real da fonte
		String shortProgress = progress;
		int progMaxWidth = cardWidth / 2 - 24;
		while (shortProgress.length() > 3 && g2.getFontMetrics().stringWidth(shortProgress) > progMaxWidth) {
			shortProgress = shortProgress.substring(0, shortProgress.length() - 1);
		}
		if (shortProgress.length() < progress.length()) {
			shortProgress = shortProgress.substring(0, Math.max(1, shortProgress.length() - 3)) + "\u2026";
		}
		int progW = g2.getFontMetrics().stringWidth(shortProgress);
		g2.setColor(new Color(129, 199, 132));
		g2.drawString(shortProgress, margin + cardWidth - progW - 8, margin + cardHeight - 6);

		// --- Barra de canal (objetivo de defesa) e timer (sobrevivência) ---
		drawObjectiveWidgets(g2, s, screenWidth);

		// --- Waypoint apontando para o alvo da missão ---
		drawWaypoint(g2, s, screenWidth);

		// --- Prompt de interação (R) para NPC próximo ---
		drawInteractionPrompts(g2, s);
	}

	/**
	 * Desembrulha o objetivo ativo de um possível DialogueObjective/SequenceObjective,
	 * retornando o estágio atualmente em andamento (ou o próprio objetivo).
	 * Visível no pacote para o marcador do alvo no minimapa.
	 */
	static com.traduvertgames.quest.RPGObjective findActiveObjective() {
		com.traduvertgames.quest.RPGObjective o = QuestManager.getCurrentObjective();
		int guard = 0;
		while (o != null && guard < 8) {
			if (o instanceof com.traduvertgames.quest.DialogueObjective) {
				o = ((com.traduvertgames.quest.DialogueObjective) o).getDelegate();
			} else if (o instanceof com.traduvertgames.quest.SequenceObjective) {
				o = ((com.traduvertgames.quest.SequenceObjective) o).getActive();
			} else {
				break;
			}
			guard++;
		}
		return o;
	}

	/**
	 * Alias público do desembrulhamento para uso do {@link MiniMap}.
	 */
	static com.traduvertgames.quest.RPGObjective unwrapObjective() {
		return findActiveObjective();
	}

	/**
	 * Widgets de acompanhamento de objetivos variados: barra de canalização
	 * (objetivos de defesa) e timer grande centralizado no topo (sobrevivência).
	 */
	private static void drawObjectiveWidgets(Graphics2D g2, int s, int screenWidth) {
		com.traduvertgames.quest.RPGObjective o = unwrapObjective();
		if (o == null) {
			return;
		}
		if (o instanceof com.traduvertgames.quest.HoldObjective) {
			com.traduvertgames.quest.HoldObjective hold = (com.traduvertgames.quest.HoldObjective) o;
			if (!hold.isActive()) {
				return;
			}
			int barY = 18 * s / 4 + 10;
			int barW = 120 * s / 4 + 20;
			int barH = 6 * s / 4 + 2;
			int barX = (screenWidth - barW) / 2;
			g2.setColor(new Color(6, 9, 16, 190));
			g2.fillRoundRect(barX - 4, barY - 4, barW + 8, barH + 8, 6, 6);
			g2.setColor(new Color(66, 66, 66));
			g2.fillRoundRect(barX, barY, barW, barH, 3, 3);
			double fill = hold.getChannelProgress();
			int filled = (int) (barW * Math.min(1.0, Math.max(0.0, fill)));
			Color barColor = hold.isUnderAttack() ? new Color(244, 67, 54) : new Color(76, 175, 80);
			if (filled > 0) {
				g2.setColor(barColor);
				g2.fillRoundRect(barX, barY, filled, barH, 3, 3);
			}
			g2.setColor(Color.WHITE);
			g2.setFont(new Font("SansSerif", Font.BOLD, 8 * s / 4 + 2));
			String label = hold.isUnderAttack() ? "DEFESA SOB ATAQUE!" : "Defendendo... " + hold.getPercentText();
			int labelW = g2.getFontMetrics().stringWidth(label);
			g2.drawString(label, (screenWidth - labelW) / 2, barY - 6);
		} else if (o instanceof com.traduvertgames.quest.SurviveObjective) {
			com.traduvertgames.quest.SurviveObjective surv = (com.traduvertgames.quest.SurviveObjective) o;
			if (surv.isComplete()) {
				return;
			}
			int seconds = surv.getRemainingSeconds();
			Font timerFont = new Font("SansSerif", Font.BOLD, 14 * s / 4 + 2);
			g2.setFont(timerFont);
			String timerText = seconds + "s";
			int timerW = g2.getFontMetrics().stringWidth(timerText);
			int timerX = (screenWidth - timerW) / 2;
			int timerY = 26 * s / 4 + 12;
			g2.setColor(new Color(6, 9, 16, 180));
			g2.fillRoundRect(timerX - 8, timerY - 12, timerW + 16, 18, 6, 6);
			g2.setColor(seconds <= 10 ? new Color(255, 82, 82) : new Color(129, 199, 132));
			g2.drawString(timerText, timerX, timerY);
		}
	}

	private static void drawWaypoint(Graphics2D g2, int s, int screenWidth) {
		String targetName = QuestManager.getTargetHint();
		if (targetName == null || targetName.isEmpty() || QuestManager.isSurvivalMode()) {
			return;
		}
		Entity target = findTargetEntity(targetName);
		if (target == null) {
			return;
		}
		if (Game.player == null) {
			return;
		}
		int centerX = Game.player.getX() + 8 - Camera.x;
		int centerY = Game.player.getY() + 8 - Camera.y;
		int targetCenterX = target.getX() + 8 - Camera.x;
		int targetCenterY = target.getY() + 8 - Camera.y;

		double dx = targetCenterX - centerX;
		double dy = targetCenterY - centerY;
		double distance = Math.sqrt(dx * dx + dy * dy);

		if (distance < 2) {
			return;
		}

		if (distance <= WAYPOINT_DISTANCE) {
			// Alvo na tela: sinal pulsante sobre ele
			double pulse = 0.5 + 0.5 * Math.sin(2.0 * Math.PI * (System.currentTimeMillis() % (PULSE_PERIOD * 16))
					/ (PULSE_PERIOD * 16));
			int radius = 8 + (int) (2 * pulse);
			g2.setColor(new Color(255, 235, 59, 120 + (int) (80 * pulse)));
			g2.fillOval(targetCenterX - radius, targetCenterY - radius, radius * 2, radius * 2);
			g2.setColor(new Color(255, 235, 59, 200));
			g2.setStroke(new java.awt.BasicStroke(1.5f));
			g2.drawOval(targetCenterX - radius, targetCenterY - radius, radius * 2, radius * 2);
		} else {
			// Alvo fora da tela: seta na borda na direção do alvo. A seta é
			// sempre desenhada um pouco para dentro da borda (offset) e sobre um
			// painel escuro contínuo com a distância — assim ela nunca fica
			// grudada na borda nem é lida como "apontando para uma parede".
			double angle = Math.atan2(dy, dx);
			double arrowDist = Math.min(WAYPOINT_DISTANCE * 0.85, distance);
			double arrowX = centerX + Math.cos(angle) * arrowDist;
			double arrowY = centerY + Math.sin(angle) * arrowDist;
			// Clampa à área visível, com margem de segurança da borda.
			int visibleW = Game.WIDTH * s;
			int visibleH = Game.HEIGHT * s;
			int margin = 16;
			arrowX = Math.max(margin, Math.min(visibleW - margin, arrowX));
			arrowY = Math.max(margin, Math.min(visibleH - margin, arrowY));
			// A seta não deve ficar sobre o card da missão (canto superior
			// esquerdo): quando o clamp a empurra para a área do card, ela é
			// deslocada para baixo do card, mantendo a direção do alvo.
			int cardWidth = Math.min(visibleW - margin * 2, 250 * s / 4 + 40);
			int cardHeight = 26 * s / 4 + 6;
			if (arrowX <= margin + cardWidth && arrowY <= margin + cardHeight + 20) {
				arrowX = Math.min(visibleW - margin, Math.max(margin, arrowX));
				arrowY = Math.min(visibleH - margin, arrowY);
				if (arrowY <= margin + cardHeight + 20) {
					arrowY = Math.min(visibleH - margin, margin + cardHeight + 30);
				}
			}
			Font smallFont = new Font("SansSerif", Font.BOLD, 7 * s / 4 + 2);
			g2.setFont(smallFont);
			String distLabel = String.format("%dm", (int) (distance / 16));
			int labelW = g2.getFontMetrics().stringWidth(distLabel);
			int labelH = 11;
			// Painel escuro único atrás do label e da flecha
			int padX = 5, padY = 3;
			int panelW = Math.max(labelW + padX * 2, 14);
			int panelH = labelH + padY * 2 + 10;
			g2.setColor(new Color(0, 0, 0, 200));
			g2.fillRoundRect((int) arrowX - panelW / 2, (int) arrowY - 2 - padY, panelW, panelH, 6, 6);
			g2.setColor(new Color(255, 235, 59, 255));
			int size = 7;
			double headAngle1 = angle + Math.toRadians(150);
			double headAngle2 = angle - Math.toRadians(150);
			g2.fillPolygon(
					new int[] { (int) arrowX, (int) (arrowX + size * Math.cos(headAngle1)),
							(int) (arrowX + size * Math.cos(headAngle2)) },
					new int[] { (int) arrowY, (int) (arrowY + size * Math.sin(headAngle1)),
							(int) (arrowY + size * Math.sin(headAngle2)) },
					3);
			g2.setColor(new Color(255, 235, 59, 245));
			g2.drawString(distLabel, (int) arrowX - labelW / 2, (int) arrowY + 9 + labelH);
		}
	}

	/**
	 * Localiza a entidade (NPC interativo, beacon ou similar) cujo nome
	 * corresponde ao hint do objetivo. Usa o nome dos NPCs interativos e
	 * a cor como critério para beacons. Visível no pacote para o
	 * marcador do alvo no minimapa.
	 */
	static Entity findTargetEntity(String targetName) {
		for (int i = 0; i < Game.entities.size(); i++) {
			Entity e = Game.entities.get(i);
			if (e instanceof InteractiveNpc && targetName.equals(((InteractiveNpc) e).getName())) {
				return e;
			}
		}
		// Alvo narrativo de chefe (ex.: "o Guardião do Subsolo", "o Supervisor-Prime"):
		// localiza o boss vivo da fase pelo nome do objetivo ou pelo prefixo "o ".
		boolean looksLikeBoss = targetName.startsWith("o ") || targetName.toLowerCase().contains("chefe")
				|| targetName.toLowerCase().contains("supervisor");
		if (looksLikeBoss) {
			for (int i = 0; i < Game.entities.size(); i++) {
				Entity e = Game.entities.get(i);
				if (e instanceof com.traduvertgames.entities.Enemy
						&& ((com.traduvertgames.entities.Enemy) e).isBoss()
						&& ((com.traduvertgames.entities.Enemy) e).getTotalLife() > 0) {
					return e;
				}
			}
		}
		return null;
	}

	private static void drawInteractionPrompts(Graphics2D g2, int s) {
		if (DialogueManager.isActive()) {
			return;
		}
		for (int i = 0; i < Game.entities.size(); i++) {
			Entity e = Game.entities.get(i);
			if (e instanceof InteractiveNpc) {
				InteractiveNpc.renderPrompt(g2, (InteractiveNpc) e, s);
			}
		}
	}
}
