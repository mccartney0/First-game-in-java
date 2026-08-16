package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.main.Menu;
import com.traduvertgames.main.Game;

/**
 * Cutscene de vitória ao concluir a campanha (fase 8). Exibe a mensagem final
 * da Comandante Ava, as estatísticas da campanha e permite avançar ao modo
 * sobrevivência com Enter ou ao menu principal com ESC.
 */
public final class VictoryCutscene {

	private static boolean showing = false;
	private static int fadeIn = 0;
	private static final int FADE_TOTAL = 30;
	private static int framesElapsed = 0;

	private static final String[] MESSAGES = {
			"O Supervisor-Prime foi destruído.",
			"A mente que comandava as máquinas está desativada.",
			"A colônia finalmente pertence a nós novamente.",
			"Obrigada, piloto. Você salvou todos nós.",
			"— Comandante Ava"
	};

	private VictoryCutscene() {
	}

	/** Inicia a cutscene de vitória (chamada uma única vez ao concluir a campanha). */
	public static void start() {
		if (showing) {
			return;
		}
		showing = true;
		fadeIn = 0;
		framesElapsed = 0;
		Game.gameState = "MENU";
		Menu.pause = true;
		com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.LEVELUP);
	}

	public static boolean isShowing() {
		return showing;
	}

	/** Cancela a cutscene restaurando o estado de jogo normal. */
	public static void stop() {
		showing = false;
		Game.gameState = "NORMAL";
		Menu.pause = false;
	}

	/** Volta ao menu principal (ESC durante a cutscene). */
	public static void returnToMainMenu() {
		stop();
		Game.gameState = "MENU";
		Menu.pause = false;
		Menu.closePauseScreen();
		Game game = Game.getInstance();
		if (game != null && game.menu != null) {
			game.menu.resetToMain();
		}
	}

	/** Avança ao modo sobrevivência (Enter durante a cutscene). */
	public static void advanceToSurvival() {
		stop();
		Game.enterSurvivalMode();
	}

	/** Atualiza: Enter avança ao modo sobrevivência; ESC volta ao menu principal. */
	public static void update(boolean enter, boolean escape) {
		if (!showing) {
			return;
		}
		framesElapsed++;
		if (escape && framesElapsed > FADE_TOTAL) {
			returnToMainMenu();
		} else if (enter && framesElapsed > FADE_TOTAL) {
			advanceToSurvival();
		}
	}

	/** Renderiza o overlay de vitória sobre o buffer escalado do jogo. */
	public static void render(Graphics g, int scale) {
		if (!showing) {
			return;
		}
		fadeIn = Math.min(FADE_TOTAL, fadeIn + 1);
		int alpha = (fadeIn * 255) / FADE_TOTAL;

		int screenWidth = Game.WIDTH * scale;
		int screenHeight = Game.HEIGHT * scale;

		g.setColor(new Color(0, 0, 0, (int) (alpha * 0.85)));
		g.fillRect(0, 0, screenWidth, screenHeight);

		g.setFont(new Font("arial", Font.BOLD, 26 * scale / 4));
		g.setColor(new Color(255, 214, 0, alpha));
		String title = "VITÓRIA — CAMPANHA CONCLUÍDA";
		g.drawString(title, (screenWidth - g.getFontMetrics().stringWidth(title)) / 2, screenHeight / 4);

		g.setFont(new Font("arial", Font.PLAIN, 13 * scale / 4));
		g.setColor(new Color(230, 230, 230, alpha));
		int y = screenHeight / 4 + 40 * scale / 4;
		for (String line : MESSAGES) {
			g.drawString(line, (screenWidth - g.getFontMetrics().stringWidth(line)) / 2, y);
			y += 20 * scale / 4;
		}

		// Estatísticas da campanha.
		y += 26 * scale / 4;
		g.setFont(new Font("arial", Font.BOLD, 14 * scale / 4));
		g.setColor(new Color(130, 210, 255, alpha));
		String statsLabel = "Estatísticas da campanha";
		g.drawString(statsLabel, (screenWidth - g.getFontMetrics().stringWidth(statsLabel)) / 2, y);
		y += 24 * scale / 4;
		g.setFont(new Font("arial", Font.PLAIN, 12 * scale / 4));
		g.setColor(new Color(200, 200, 200, alpha));
		String scoreLine = String.format("Pontuação: %d — Melhor combo: %d — Inimigos derrotados: %d",
				Game.getScore(), Game.getBestComboRecord(), Enemy.enemies);
		g.drawString(scoreLine, (screenWidth - g.getFontMetrics().stringWidth(scoreLine)) / 2, y);

		if (framesElapsed > FADE_TOTAL) {
			y += 34 * scale / 4;
			g.setFont(new Font("arial", Font.BOLD, 13 * scale / 4));
			g.setColor(new Color(255, 255, 255, (int) (alpha * blink())));
			String hint = "ENTER: modo sobrevivência — ESC: menu principal";
			g.drawString(hint, (screenWidth - g.getFontMetrics().stringWidth(hint)) / 2, y);
		}
	}

	private static double blink() {
		return 0.5 + 0.5 * Math.sin(framesElapsed * 0.1);
	}
}
