package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.main.Game;

/**
 * Banner central temporário para eventos narrativos importantes, como a
 * conclusão de uma missão ("MISSÃO CONCLUÍDA") ou marcos da campanha.
 * O banner é desenhado no espaço escalado da janela (overlayG), com fontes
 * proporcionais à escala, garantindo leitura nítida em qualquer resolução.
 */
public final class MissionBanner {

	private static final int DEFAULT_LIFE = 150;
	private static final int FADE_IN = 20;

	private static String title = "";
	private static String subtitle = "";
	private static Color titleColor = new Color(255, 214, 0);
	private static Color subtitleColor = Color.WHITE;
	private static int life = 0;
	private static boolean announced = false;

	/**
	 * Lore da próxima fase agendada (rodada 21): o banner de ambientação não
	 * compete mais com o aviso de conclusão nem com o card de estatísticas —
	 * aparece apenas após o jogador fechar o card e o fade revelar a nova fase.
	 */
	private static String scheduledTitle = "";
	private static String scheduledSubtitle = "";
	private static int scheduledDelay = 0;

	private MissionBanner() {
	}

	/** Exibe o banner de missão concluída (título + recompensa). */
	public static void showComplete(String objectiveTitle) {
		if (announced) {
			return;
		}
		announced = true;
		title = "MISSÃO CONCLUÍDA";
		subtitle = objectiveTitle;
		titleColor = new Color(255, 214, 0);
		subtitleColor = Color.WHITE;
		life = DEFAULT_LIFE;
	}

	/** Exibe um banner genérico (ex.: abertura de campanha, evento especial). */
	public static void show(String title, String subtitle, Color titleColor, Color subtitleColor, int lifeFrames) {
		MissionBanner.title = title;
		MissionBanner.subtitle = subtitle;
		MissionBanner.titleColor = titleColor;
		MissionBanner.subtitleColor = subtitleColor;
		life = lifeFrames;
		announced = false;
	}

	/** Reseta o banner e a flag de "já anunciado" (troca de fase/reinício). */
	public static void reset() {
		life = 0;
		announced = false;
		title = "";
		subtitle = "";
		scheduledTitle = "";
		scheduledSubtitle = "";
		scheduledDelay = 0;
	}

	/** Desmarca a flag de anúncio sem esconder o banner visível. */
	public static void allowReannounce() {
		announced = false;
	}

	public static boolean isShowing() {
		return life > 0;
	}

	/**
	 * Agenda o banner de lore da próxima fase para aparecer daqui a
	 * `delayFrames` — usado na transição de fase: a lore só entra depois
	 * que o cooldown de respiro (rodada 21) termina.
	 */
	public static void scheduleLore(String title, String subtitle, int delayFrames) {
		scheduledTitle = title;
		scheduledSubtitle = subtitle;
		scheduledDelay = delayFrames;
	}

	/** Atualiza o tempo de vida do banner e dispara a lore agendada. */
	public static void update() {
		if (life > 0) {
			life--;
		}
		// Rodada 21: quando o cooldown da transição termina, exibe a lore
		// da nova fase (se houver agendamento pendente).
		if (scheduledDelay > 0) {
			scheduledDelay--;
			if (scheduledDelay == 0 && !scheduledTitle.isEmpty()) {
				show(scheduledTitle, scheduledSubtitle,
					new Color(255, 235, 59), Color.WHITE, 300);
				scheduledTitle = "";
				scheduledSubtitle = "";
			}
		}
	}

	/** Renderiza o banner centralizado sobre o buffer do jogo. */
	public static void render(Graphics g) {
		if (life <= 0 || title.isEmpty()) {
			return;
		}
	// Espaço do jogo em coordenadas escaladas (buffer * escala base 4).
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;
		int s = Game.SCALE;
		int elapsed = DEFAULT_LIFE - life;
		int alpha = 255;
		if (elapsed < FADE_IN) {
			alpha = Math.max(0, (elapsed * 255) / FADE_IN);
		} else if (life < FADE_IN) {
			alpha = Math.max(0, (life * 255) / FADE_IN);
		}

		// Fontes proporcionais à escala: leitura nítida sem depender do zoom do buffer.
		int titleSize = 18 * s / 4 + 3;
		int subtitleSize = 12 * s / 4 + 2;
		g.setFont(new Font("arial", Font.BOLD, titleSize));
		int titleWidth = g.getFontMetrics().stringWidth(title);
		g.setFont(new Font("arial", Font.PLAIN, subtitleSize));
		int subtitleWidth = g.getFontMetrics().stringWidth(subtitle);
		int pad = 20 * s / 4 + 6;
		int boxWidth = Math.min(screenWidth - 32, Math.max(titleWidth, subtitleWidth) + pad * 2);
		int boxHeight = 26 * s / 4 + 16;
		int x = (screenWidth - boxWidth) / 2;
		int y = (screenHeight - boxHeight) / 2 - 18 * s / 4;

		g.setColor(new Color(8, 12, 20, (int) (alpha * 0.9)));
		g.fillRoundRect(x, y, boxWidth, boxHeight, 10, 10);
		g.setColor(new Color(255, 214, 0, alpha / 2));
		g.drawRoundRect(x, y, boxWidth, boxHeight, 10, 10);

		g.setFont(new Font("arial", Font.BOLD, titleSize));
		g.setColor(new Color(titleColor.getRed(), titleColor.getGreen(), titleColor.getBlue(), alpha));
		g.drawString(title, x + (boxWidth - titleWidth) / 2, y + boxHeight / 2 - subtitleSize / 2 - 2);
		g.setFont(new Font("arial", Font.PLAIN, subtitleSize));
		g.setColor(new Color(subtitleColor.getRed(), subtitleColor.getGreen(), subtitleColor.getBlue(), alpha));
		g.drawString(subtitle, x + (boxWidth - subtitleWidth) / 2, y + boxHeight / 2 + subtitleSize - 2);
	}
}
