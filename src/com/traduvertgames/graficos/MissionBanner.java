package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.main.Game;

/**
 * Banner central temporário para eventos narrativos importantes, como a
 * conclusão de uma missão ("MISSÃO CONCLUÍDA") ou marcos da campanha.
 * O banner aparece em coordenadas de buffer (384x216), centralizado, e
 * desaparece gradualmente.
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
	}

	/** Desmarca a flag de anúncio sem esconder o banner visível. */
	public static void allowReannounce() {
		announced = false;
	}

	public static boolean isShowing() {
		return life > 0;
	}

	/** Atualiza o tempo de vida do banner. */
	public static void update() {
		if (life > 0) {
			life--;
		}
	}

	/** Renderiza o banner centralizado sobre o buffer do jogo. */
	public static void render(Graphics g) {
		if (life <= 0 || title.isEmpty()) {
			return;
		}
		int screenWidth = Game.WIDTH;
		int screenHeight = Game.HEIGHT;
		int elapsed = DEFAULT_LIFE - life;
		int alpha = 255;
		if (elapsed < FADE_IN) {
			alpha = Math.max(0, (elapsed * 255) / FADE_IN);
		} else if (life < FADE_IN) {
			alpha = Math.max(0, (life * 255) / FADE_IN);
		}

		int fontSize = 14;
		g.setFont(new Font("arial", Font.BOLD, fontSize));
		int titleWidth = g.getFontMetrics().stringWidth(title);
		g.setFont(new Font("arial", Font.PLAIN, 8));
		int subtitleWidth = g.getFontMetrics().stringWidth(subtitle);
		int boxWidth = Math.max(titleWidth, subtitleWidth) + 24;
		int boxHeight = 34;
		int x = (screenWidth - boxWidth) / 2;
		int y = (screenHeight - boxHeight) / 2 - 12;

		g.setColor(new Color(8, 12, 20, (int) (alpha * 0.85)));
		g.fillRoundRect(x, y, boxWidth, boxHeight, 6, 6);
		g.setColor(new Color(255, 214, 0, alpha / 2));
		g.drawRoundRect(x, y, boxWidth, boxHeight, 6, 6);

		g.setFont(new Font("arial", Font.BOLD, fontSize));
		g.setColor(new Color(titleColor.getRed(), titleColor.getGreen(), titleColor.getBlue(), alpha));
		g.drawString(title, (screenWidth - titleWidth) / 2, y + 14);
		g.setFont(new Font("arial", Font.PLAIN, 8));
		g.setColor(new Color(subtitleColor.getRed(), subtitleColor.getGreen(), subtitleColor.getBlue(), alpha));
		g.drawString(subtitle, (screenWidth - subtitleWidth) / 2, y + 26);
	}
}
