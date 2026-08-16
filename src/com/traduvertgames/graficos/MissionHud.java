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

		// --- Waypoint apontando para o alvo da missão ---
		drawWaypoint(g2, s, screenWidth);

		// --- Prompt de interação (R) para NPC próximo ---
		drawInteractionPrompts(g2, s);
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
			// Alvo fora da tela: seta na borda na direção do alvo
			double angle = Math.atan2(dy, dx);
			double arrowX = centerX + Math.cos(angle) * WAYPOINT_DISTANCE * 0.85;
			double arrowY = centerY + Math.sin(angle) * WAYPOINT_DISTANCE * 0.85;
			// Clampa à área visível
			int visibleW = Game.WIDTH * s;
			int visibleH = Game.HEIGHT * s;
			arrowX = Math.max(18, Math.min(visibleW - 18, arrowX));
			arrowY = Math.max(18, Math.min(visibleH - 18, arrowY));
			int size = 7;
			double headAngle1 = angle + Math.toRadians(150);
			double headAngle2 = angle - Math.toRadians(150);
			g2.setColor(new Color(255, 235, 59, 230));
			g2.fillPolygon(
					new int[] { (int) arrowX, (int) (arrowX + size * Math.cos(headAngle1)),
							(int) (arrowX + size * Math.cos(headAngle2)) },
					new int[] { (int) arrowY, (int) (arrowY + size * Math.sin(headAngle1)),
							(int) (arrowY + size * Math.sin(headAngle2)) },
					3);
			// Distância até o alvo
			Font smallFont = new Font("SansSerif", Font.BOLD, 7 * s / 4 + 2);
			g2.setFont(smallFont);
			String distLabel = String.format("%dm", (int) (distance / 16));
			int labelW = g2.getFontMetrics().stringWidth(distLabel);
			g2.setColor(new Color(0, 0, 0, 180));
			g2.fillRoundRect((int) arrowX - labelW / 2 - 4, (int) arrowY + 9, labelW + 8, 10, 5, 5);
			g2.setColor(new Color(255, 235, 59, 245));
			g2.drawString(distLabel, (int) arrowX - labelW / 2, (int) arrowY + 17);
		}
	}

	/**
	 * Localiza a entidade (NPC interativo, beacon ou similar) cujo nome
	 * corresponde ao hint do objetivo. Usa o nome dos NPCs interativos e
	 * a cor como critério para beacons.
	 */
	private static Entity findTargetEntity(String targetName) {
		for (int i = 0; i < Game.entities.size(); i++) {
			Entity e = Game.entities.get(i);
			if (e instanceof InteractiveNpc && targetName.equals(((InteractiveNpc) e).getName())) {
				return e;
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
