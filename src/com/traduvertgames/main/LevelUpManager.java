package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.entities.Player;

/**
 * Sistema de XP e níveis do jogador (estilo Vampire Survivors).
 * Cada abate concede XP; ao completar o nível, o jogo pausa e oferece
 * 3 melhorias aleatórias para o jogador escolher.
 */
import com.traduvertgames.main.Game;

public final class LevelUpManager {

	/** XP necessário para o próximo nível: base * 1.4^(nivel-1). */
	private static final double XP_BASE = 40;
	private static final double XP_GROWTH = 1.4;
	private static final int MAX_PLAYER_LEVEL = 10;

	private static final int XP_PER_KILL = 10;
	private static final int CHOICES = 3;

	public enum Upgrade {
		VIDA_MAXIMA("+25 vida máxima", new Color(244, 67, 54)),
		MANA_MAXIMA("+100 mana máxima", new Color(33, 150, 243)),
		ESCUDO_MAXIMO("+30 escudo máximo", new Color(121, 134, 203)),
		VELOCIDADE("+12% velocidade", new Color(76, 175, 80)),
		ENERGIA("+80 energia da arma atual", new Color(255, 183, 77)),
		ESCUDO_INICIAL("+40 escudo agora", new Color(186, 104, 200));

		public final String label;
		public final Color accent;

		Upgrade(String label, Color accent) {
			this.label = label;
			this.accent = accent;
		}
	}

	private static final boolean ENABLED = true;
	private static double xp = 0;
	private static int playerLevel = 1;
	private static Upgrade[] pendingChoices = new Upgrade[CHOICES];
	private static int choiceIndex = 0;
	private static boolean showingLevelUp = false;

	private LevelUpManager() {
	}

	public static void grantKillXp() {
		if (showingLevelUp || playerLevel >= MAX_PLAYER_LEVEL) {
			return;
		}
		xp += XP_PER_KILL * Game.getComboMultiplier();
		double required = xpForNextLevel();
		if (xp >= required) {
			xp = 0;
			playerLevel++;
			offerChoices();
		}
	}

	public static double xpForNextLevel() {
		return XP_BASE * Math.pow(XP_GROWTH, playerLevel - 1);
	}

	/** Indica se o sistema de XP está ativo nesta sessão. */
	public static boolean isEnabled() {
		return ENABLED;
	}

	public static double getXp() {
		return xp;
	}

	public static int getPlayerLevel() {
		return playerLevel;
	}

	public static boolean isShowingLevelUp() {
		return showingLevelUp;
	}

	public static Upgrade[] getPendingChoices() {
		return pendingChoices;
	}

	public static int getChoiceIndex() {
		return choiceIndex;
	}

	public static void setChoiceIndex(int index) {
		choiceIndex = (index + CHOICES) % CHOICES;
	}

	private static void offerChoices() {
		Game.gameState = "LEVELUP";
		showingLevelUp = true;
		Upgrade[] all = Upgrade.values();
		pendingChoices = new Upgrade[CHOICES];
		for (int i = 0; i < CHOICES; i++) {
			pendingChoices[i] = all[Game.rand.nextInt(all.length)];
		}
		choiceIndex = 0;
	}

	/** Aplica a melhoria escolhida e retorna ao jogo. */
	public static void applyChoice(int index) {
		if (index < 0 || index >= CHOICES || pendingChoices[index] == null) {
			return;
		}
		switch (pendingChoices[index]) {
		case VIDA_MAXIMA:
			Player.maxLife += 25;
			Player.life += 25;
			break;
		case MANA_MAXIMA:
			Player.maxMana += 100;
			Player.mana += 50;
			break;
		case ESCUDO_MAXIMO:
			Player.maxShield += 30;
			Player.shield += 30;
			break;
		case VELOCIDADE:
			if (Game.player != null) {
				Game.player.speed *= 1.12;
			}
			break;
		case ENERGIA:
			if (Game.player != null) {
				Game.player.addWeaponEnergy(80);
			}
			break;
		case ESCUDO_INICIAL:
			Player.shield += 40;
			if (Player.shield > Player.maxShield) {
				Player.shield = Player.maxShield;
			}
			break;
		default:
			break;
		}
		Game.gameState = "NORMAL";
		showingLevelUp = false;
		pendingChoices = new Upgrade[CHOICES];
	}

		/** Fecha a tela de level up sem aplicar melhoria (ESC). */
		public static void dismiss() {
			showingLevelUp = false;
			Game.gameState = "NORMAL";
		}

		/** Navegação exposta para o handler de teclado do Game. */
		public static void navigateUp() {
			if (!showingLevelUp) {
				return;
			}
			setChoiceIndex(choiceIndex - 1);
		}

		/** Navegação exposta para o handler de teclado do Game. */
		public static void navigateDown() {
			if (!showingLevelUp) {
				return;
			}
			setChoiceIndex(choiceIndex + 1);
		}

		/** Confirma a escolha selecionada (Enter). */
		public static void confirmChoice() {
			if (!showingLevelUp) {
				return;
			}
			applyChoice(choiceIndex);
		}

		/** Navega entre as opções com setas. */
		public static void update() {
			if (!showingLevelUp) {
				return;
			}
		Game game = Game.getInstance();
		if (game == null) {
			return;
		}
		if (Game.getInstance().menu.up) {
			Game.getInstance().menu.up = false;
			navigateUp();
		} else if (Game.getInstance().menu.down) {
			Game.getInstance().menu.down = false;
			navigateDown();
		} else if (Game.getInstance().menu.enter) {
			Game.getInstance().menu.enter = false;
			confirmChoice();
		}
	}

	public static void render(Graphics g) {
		if (!showingLevelUp) {
			return;
		}
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		g.setColor(new Color(0, 0, 0, 170));
		g.fillRect(0, 0, screenWidth, screenHeight);

		g.setFont(new Font("arial", Font.BOLD, 26));
		g.setColor(Color.yellow);
		String title = "Nível " + playerLevel + "! Escolha uma melhoria:";
		g.drawString(title, (screenWidth - g.getFontMetrics().stringWidth(title)) / 2, screenHeight / 2 - 90);

		g.setFont(new Font("arial", Font.BOLD, 18));
		int cardWidth = 260;
		int cardHeight = 64;
		int gap = 24;
		int totalWidth = cardWidth * CHOICES + gap * (CHOICES - 1);
		int startX = (screenWidth - totalWidth) / 2;
		int cardY = screenHeight / 2 - 50;

		for (int i = 0; i < CHOICES; i++) {
			Upgrade upgrade = pendingChoices[i];
			int cardX = startX + i * (cardWidth + gap);
			g.setColor(i == choiceIndex ? upgrade.accent : new Color(40, 44, 54, 230));
			g.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 14, 14);
			g.setColor(Color.white);
			String label = upgrade.label;
			g.drawString(label, cardX + 12, cardY + cardHeight / 2 + 6);
		}

		g.setFont(new Font("arial", Font.PLAIN, 14));
		g.setColor(new Color(200, 200, 200));
		String hint = "Setas para escolher — Enter para confirmar";
		g.drawString(hint, (screenWidth - g.getFontMetrics().stringWidth(hint)) / 2, cardY + cardHeight + 30);
	}

	public static void reset() {
		xp = 0;
		playerLevel = 1;
		showingLevelUp = false;
		pendingChoices = new Upgrade[CHOICES];
		choiceIndex = 0;
	}
}
