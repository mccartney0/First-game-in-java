package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.quest.QuestManager;

/**
 * Tela de seleção de fases com atalho do menu: apresenta as 5 fases do jogo
 * e permite reiniciar em qualquer uma delas (perdendo os recursos da partida
 * atual, como um treino livre).
 */
import com.traduvertgames.main.Game;
import com.traduvertgames.entities.UltimateAbility;
import com.traduvertgames.entities.DashAbility;
import com.traduvertgames.entities.Enemy;

public final class LevelSelectScreen {

	private static final int TOTAL_LEVELS = 5;
	private static final String[] LEVEL_NAMES = {
			"Setor Alpha — Coleta de artefatos",
			"Câmara do Warbringer — Caçada ao chefe",
			"Círculo do Ritual — Ritual sombrio",
			"Núcleo da Colônia — Resgate",
			"Datacenter Nexus — Recuperação de dados"
	};

	private static int selection = 0;
	private static boolean open = false;

	private LevelSelectScreen() {
	}

	public static boolean isOpen() {
		return open;
	}

	public static void open() {
		open = true;
		selection = Math.max(0, Math.min(TOTAL_LEVELS - 1, QuestManager.getCurrentLevel() - 1));
		Game.gameState = "LEVELSELECT";
	}

	public static void close() {
		open = false;
		Game.gameState = "MENU";
	}

	public static void update() {
		if (!open) {
			return;
		}
		Game game = Game.getInstance();
		if (game == null) {
			return;
		}
		if (Game.getInstance().menu.up) {
			Game.getInstance().menu.up = false;
			selection = (selection - 1 + TOTAL_LEVELS) % TOTAL_LEVELS;
		} else if (Game.getInstance().menu.down) {
			Game.getInstance().menu.down = false;
			selection = (selection + 1) % TOTAL_LEVELS;
		} else if (Game.getInstance().menu.enter) {
			Game.getInstance().menu.enter = false;
			playLevel(selection + 1);
		} else if (Game.escapePressed) {
			Game.escapePressed = false;
			close();
		}
	}

	private static void playLevel(int level) {
		Game game = Game.getInstance();
		if (game == null) {
			return;
		}
		game.setCurrentLevel(level);
		Game.player.resetPersistentArsenal();
		com.traduvertgames.entities.Player.resetBaseStats();
		Enemy.enemies = 0;
		Game.setScore(0);
		LevelUpManager.reset();
		DashAbility.reset();
		UltimateAbility.reset();
		WaveManager.reset();
		com.traduvertgames.world.World.restartGame("level" + level + ".png");
		Game.gameState = "NORMAL";
		open = false;
		Menu.pause = false;
	}

	public static void render(Graphics g) {
		if (!open) {
			return;
		}
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		g.setColor(new Color(0, 0, 0, 185));
		g.fillRect(0, 0, screenWidth, screenHeight);

		g.setFont(new Font("arial", Font.BOLD, 26));
		g.setColor(Color.yellow);
		String title = "Seleção de fases";
		g.drawString(title, (screenWidth - g.getFontMetrics().stringWidth(title)) / 2, 100);

		g.setFont(new Font("arial", Font.BOLD, 18));
		int panelX = (screenWidth - 420) / 2;
		int panelY = 130;
		int lineHeight = 44;
		int panelHeight = TOTAL_LEVELS * lineHeight + 20;
		g.setColor(new Color(14, 18, 28, 220));
		g.fillRoundRect(panelX, panelY, 420, panelHeight, 16, 16);

		for (int i = 0; i < TOTAL_LEVELS; i++) {
			int rowY = panelY + 16 + lineHeight * i;
			if (selection == i) {
				g.setColor(new Color(60, 68, 88));
				g.fillRoundRect(panelX + 8, rowY - 22, 404, 30, 10, 10);
				g.setColor(Color.yellow);
				g.drawString(">", panelX + 22, rowY);
				g.setColor(Color.white);
			} else {
				g.setColor(Color.white);
			}
			String label = "Fase " + (i + 1) + ": " + LEVEL_NAMES[i];
			g.drawString(label, panelX + 40, rowY);
		}

		g.setFont(new Font("arial", Font.PLAIN, 14));
		g.setColor(new Color(200, 200, 200));
		String hint = "Setas para escolher — Enter para jogar — ESC para voltar";
		g.drawString(hint, (screenWidth - g.getFontMetrics().stringWidth(hint)) / 2, panelY + panelHeight + 30);
	}
}
