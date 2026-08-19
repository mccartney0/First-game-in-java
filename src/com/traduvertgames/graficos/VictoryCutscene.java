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

	/**
	 * Epílogo dos refugiados (rodada 31): linhas adicionais contadas quando a
	 * campanha termina pela fase 9 (Vale dos Refugiados).
	 */
	private static final String[] REFUGEE_EPILOGUE = {
			"Os refugiados deixaram o vale esta noite, sob a escolta da sua luz.",
			"No caminho para a colônia, as crianças contavam o que viram: um drone que os protegia.",
			"A reconstrução começa amanhã — e o seu nome está no primeiro capítulo."
	};

	/** Epílogo dos refugiados ativo nesta cutscene (fase 9 concluiu a campanha). */
	private static boolean refugeeEnding = false;

	/** Ativa o epílogo dos refugiados na próxima cutscene de vitória (fase 9). */
	public static void setRefugeeEnding(boolean value) {
		refugeeEnding = value;
	}

	/** @return true se o epílogo dos refugiados está ativo nesta cutscene. */
	public static boolean isRefugeeEnding() {
		return refugeeEnding;
	}

	private VictoryCutscene() {
	}

	/** Inicia a cutscene de vitória (chamada uma única vez ao concluir a campanha). */
	public static void start() {
		if (showing) {
			return;
		}
		Game game = Game.getInstance();
		if (game != null) {
			game.clearPendingOverlayInput();
		}
		// A vitória é um overlay exclusivo: nenhuma loja, estatística ou level-up
		// pode permanecer aberta por baixo dela.
		if (com.traduvertgames.main.ShopManager.isOpen()) {
			com.traduvertgames.main.ShopManager.close();
		}
		if (com.traduvertgames.main.LevelUpManager.isShowingLevelUp()) {
			com.traduvertgames.main.LevelUpManager.dismiss();
		}
		if (com.traduvertgames.graficos.PhaseStatsScreen.isShowing()) {
			com.traduvertgames.graficos.PhaseStatsScreen.dismiss();
		}
		showing = true;
		fadeIn = 0;
		framesElapsed = 0;
		Game.gameState = "MENU";
		Menu.pause = true;
		// Vitória da campanha: fanfarra completa (VICTORY), não level-up (rodada 15).
		com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.VICTORY);
	}

	public static boolean isShowing() {
		return showing;
	}

	/** Cancela a cutscene restaurando o estado de jogo normal. */
	public static void stop() {
		showing = false;
		Game game = Game.getInstance();
		if (game != null) {
			game.clearPendingOverlayInput();
		}
		Game.gameState = "NORMAL";
		Menu.pause = false;
		refugeeEnding = false;
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
		// Epílogo dos refugiados (rodada 31): a campanha concluída pela fase 9
		// ganha três linhas finais sobre a evacuação do Vale dos Refugiados.
		if (refugeeEnding) {
			y += 14 * scale / 4;
			for (String line : REFUGEE_EPILOGUE) {
				g.setColor(new Color(255, 236, 179, alpha));
				g.drawString(line, (screenWidth - g.getFontMetrics().stringWidth(line)) / 2, y);
				y += 20 * scale / 4;
			}
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
			y += 18 * scale / 4;
			String detailLine = String.format("Última fase: %d kills — Tempo: %s — Combo máximo: x%d",
					Game.getKillsThisLevel(), Game.formatLevelTime(Game.getLevelTimeMs()), Game.getBestComboThisRun());
			g.drawString(detailLine, (screenWidth - g.getFontMetrics().stringWidth(detailLine)) / 2, y);
			// Melhor partida acumulada do save (bestRun v3).
			if (com.traduvertgames.main.SaveManager.hasBestRun()) {
				String bestRunLine = String.format("Melhor partida: %d kills — %s — combo x%d",
						com.traduvertgames.main.SaveManager.getBestRunKills(),
						Game.formatLevelTime(com.traduvertgames.main.SaveManager.getBestRunTimeMs()),
						com.traduvertgames.main.SaveManager.getBestRunCombo());
				y += 18 * scale / 4;
				g.setColor(new Color(255, 214, 0, alpha));
				g.drawString(bestRunLine, (screenWidth - g.getFontMetrics().stringWidth(bestRunLine)) / 2, y);
			}

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
