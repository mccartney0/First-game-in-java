package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.Menu;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.main.WaveManager;

/**
 * Card de estatísticas exibido após cada fase concluída (incluindo ciclos do
 * modo infinito). Mostra os kills da fase, o tempo gasto e o combo máximo da
 * fase, além do recorde de ondas quando o modo sobrevivência estiver ativo.
 *
 * Funciona como overlay: o jogo fica pausado enquanto o card está visível e
 * libera com Enter (ou fecha automaticamente após ~280 frames).
 */
public final class PhaseStatsScreen {

	private static final int AUTO_DISMISS_FRAMES = 280;

	private static boolean showing = false;
	private static int fadeIn = 0;
	private static int framesElapsed = 0;
	private static final int FADE_TOTAL = 16;

	/** Snapshot das estatísticas no momento em que a fase termina. */
	private static int capturedKills = 0;
	private static long capturedTimeMs = 0;
	private static int capturedBestCombo = 1;
	private static int capturedWaves = 0;

	private PhaseStatsScreen() {
	}

	/** Arma o card com as estatísticas da fase que acabou de terminar. */
	public static void show() {
		capturedKills = Game.getKillsThisLevel();
		capturedTimeMs = Game.getLevelTimeMs();
		capturedBestCombo = Game.getBestComboThisRun();
		capturedWaves = WaveManager.getWavesSurvived();
		showing = true;
		fadeIn = 0;
		framesElapsed = 0;
		Menu.pause = true;
		Game.gameState = "MENU";
	}

	public static boolean isShowing() {
		return showing;
	}

	/** Fecha o card liberando o jogo (Enter ou tempo esgotado). */
	public static void dismiss() {
		if (!showing) {
			return;
		}
		showing = false;
		Menu.pause = false;
		Game.gameState = "NORMAL";
	}

	/** Enter fecha o card; ESC cancela para o menu principal (comportamento de fallback). */
	public static void update(boolean enter, boolean escape) {
		if (!showing) {
			return;
		}
		framesElapsed++;
		if (escape && framesElapsed > FADE_TOTAL) {
			dismiss();
			Game.gameState = "MENU";
			Menu.pause = false;
			Menu.closePauseScreen();
			Game game = Game.getInstance();
			if (game != null && game.menu != null) {
				game.menu.resetToMain();
			}
		} else if (enter && framesElapsed > FADE_TOTAL) {
			dismiss();
		} else if (framesElapsed > AUTO_DISMISS_FRAMES) {
			dismiss();
		}
	}

	/** Renderiza o card centralizado sobre o buffer escalado do jogo. */
	public static void render(Graphics g, int scale) {
		if (!showing) {
			return;
		}
		fadeIn = Math.min(FADE_TOTAL, fadeIn + 1);
		int alpha = (fadeIn * 255) / FADE_TOTAL;

		int screenWidth = Game.WIDTH * scale;
		int screenHeight = Game.HEIGHT * scale;
		int unit = scale / 4;

		int panelW = 300 * unit / 4;
		int panelH = 150 * unit / 4;
		int panelX = (screenWidth - panelW) / 2;
		int panelY = (screenHeight - panelH) / 2;

		g.setColor(new Color(0, 0, 0, (int) (alpha * 0.75)));
		g.fillRect(0, 0, screenWidth, screenHeight);

		g.setColor(new Color(16, 26, 42, alpha));
		g.fillRect(panelX, panelY, panelW, panelH);
		g.setColor(new Color(0, 230, 200, alpha));
		g.drawRect(panelX, panelY, panelW - 1, panelH - 1);

		int centerX = screenWidth / 2;
		g.setFont(new Font("arial", Font.BOLD, 16 * unit / 4));
		String title = QuestTitle();
		g.setColor(new Color(255, 214, 0, alpha));
		g.drawString(title, centerX - g.getFontMetrics().stringWidth(title) / 2, panelY + 28 * unit / 4);

		g.setFont(new Font("arial", Font.PLAIN, 12 * unit / 4));
		g.setColor(new Color(235, 235, 235, alpha));
		String timeLine = "Tempo: " + Game.formatLevelTime(capturedTimeMs);
		g.drawString(timeLine, centerX - g.getFontMetrics().stringWidth(timeLine) / 2, panelY + 54 * unit / 4);
		String killsLine = "Inimigos derrotados: " + capturedKills;
		g.drawString(killsLine, centerX - g.getFontMetrics().stringWidth(killsLine) / 2, panelY + 72 * unit / 4);
		String comboLine = "Combo máximo da fase: x" + capturedBestCombo;
		g.drawString(comboLine, centerX - g.getFontMetrics().stringWidth(comboLine) / 2, panelY + 90 * unit / 4);

		if (WaveManager.isArenaMode()) {
			String waveLine = "Ondas sobrevividas: " + capturedWaves + " — Recorde: " + WaveManager.getSurvivalRecord();
			g.drawString(waveLine, centerX - g.getFontMetrics().stringWidth(waveLine) / 2, panelY + 114 * unit / 4);
		} else if (Enemy.enemies > 0) {
			String totalLine = "Total acumulado na campanha: " + Enemy.enemies;
			g.drawString(totalLine, centerX - g.getFontMetrics().stringWidth(totalLine) / 2, panelY + 114 * unit / 4);
		}

		// Melhor partida acumulada do save (bestRun v3): destaque dourado quando
		// a fase atual quebrou o recorde global.
		String bestLine = "Melhor partida: " + SaveManager.getBestRunKills() + " kills — "
				+ Game.formatLevelTime(SaveManager.getBestRunTimeMs()) + " — combo x"
				+ SaveManager.getBestRunCombo();
			g.drawString(bestLine, centerX - g.getFontMetrics().stringWidth(bestLine) / 2,
					panelY + 132 * unit / 4);
		if (isRecordBreaking()) {
			g.setFont(new Font("arial", Font.BOLD, 12 * unit / 4));
			g.setColor(new Color(255, 214, 0, alpha));
			String recordLine = "★ NOVO RECORDE GLOBAL ★";
			g.drawString(recordLine, centerX - g.getFontMetrics().stringWidth(recordLine) / 2,
					panelY + 148 * unit / 4);
		} else {
			g.setFont(new Font("arial", Font.PLAIN, 12 * unit / 4));
			g.setColor(new Color(235, 235, 235, alpha));
		}

		if (framesElapsed > FADE_TOTAL) {
			g.setFont(new Font("arial", Font.BOLD, 10 * unit / 4));
			g.setColor(new Color(255, 255, 255, (int) (alpha * blink())));
			String hint = "ENTER: continuar";
			g.drawString(hint, centerX - g.getFontMetrics().stringWidth(hint) / 2, panelY + panelH - 10);
		}
	}

	private static String QuestTitle() {
		if (com.traduvertgames.quest.QuestManager.isSurvivalMode()) {
			return "FASE PROCEDURAL " + survivalDepth() + " CONCLUÍDA";
		}
		int nextLevel = Math.min(Game.getCurrentLevel() + 1, Game.MAX_LEVEL);
		String title = com.traduvertgames.quest.QuestManager.getPhaseTitle(nextLevel);
		if (nextLevel > com.traduvertgames.quest.QuestManager.getCurrentLevel()) {
			return "FASE CONCLUÍDA";
		}
		return title.toUpperCase() + " — RESUMO";
	}

	/** Profundidade do ciclo atual do modo infinito (nível - 8). */
	private static int survivalDepth() {
		Game game = Game.getInstance();
		return game != null ? Math.max(1, game.getLevelPlus()) : 1;
	}

	/** True quando os stats da fase atual superam o bestRun salvo do save. */
	private static boolean isRecordBreaking() {
		if (SaveManager.getBestRunKills() <= 0) {
			return false;
		}
		boolean faster = capturedTimeMs > 0 && capturedTimeMs < SaveManager.getBestRunTimeMs();
		return capturedKills > SaveManager.getBestRunKills() || faster
				|| capturedBestCombo > SaveManager.getBestRunCombo()
				|| Game.getScore() > SaveManager.getBestRunScore();
	}

	private static double blink() {
		return 0.5 + 0.5 * Math.sin(framesElapsed * 0.12);
	}
}
