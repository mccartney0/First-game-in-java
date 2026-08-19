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
			ESCUDO_INICIAL("+40 escudo agora", new Color(186, 104, 200)),
			MAGNETISMO("+35 raio de coleta de XP", new Color(255, 214, 10));

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
	/** Bônus acumulados escolhidos durante a campanha atual. */
	private static int maxLifeBonus = 0;
	private static int maxManaBonus = 0;
	private static int maxShieldBonus = 0;
	private static int magnetRadiusBonus = 0;

	private static int toInt(Object value) {
		return value instanceof Number ? ((Number) value).intValue() : 0;
	}

	private static Upgrade[] pendingChoices = new Upgrade[CHOICES];
	private static int choiceIndex = 0;
	private static boolean showingLevelUp = false;

	private LevelUpManager() {
	}

	public static void grantKillXp() {
		collectXp(XP_PER_KILL * Game.getComboMultiplier());
	}

	/** Coleta XP de abate ou de cristal; dispara no máximo um level up por coleta. */
	public static void collectXp(int amount) {
		if (showingLevelUp || playerLevel >= MAX_PLAYER_LEVEL || amount <= 0) {
			return;
		}
		xp += amount;
		double required = xpForNextLevel();
		if (xp >= required) {
			xp -= required;
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

	/** Bônus acumulado de vida máxima das escolhas de level up. */
	public static int getMaxLifeBonus() {
		return maxLifeBonus;
	}

	/** Bônus acumulado de mana máxima das escolhas de level up. */
	public static int getMaxManaBonus() {
		return maxManaBonus;
	}

	/** Bônus acumulado de escudo máximo das escolhas de level up. */
	public static int getMaxShieldBonus() {
		return maxShieldBonus;
	}

	public static int getMagnetRadius() {
		return 72 + magnetRadiusBonus;
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
		SoundManager.play(SoundManager.Event.LEVELUP);
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
			maxLifeBonus += 25;
			Player.maxLife += 25;
			Player.life += 25;
			break;
		case MANA_MAXIMA:
			maxManaBonus += 100;
			Player.maxMana += 100;
			Player.mana += 50;
			break;
		case ESCUDO_MAXIMO:
			maxShieldBonus += 30;
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
			case MAGNETISMO:
				magnetRadiusBonus += 35;
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

		/** Seleciona um upgrade pelo índice e aplica de imediato (teclas 1/2/3). */
		public static void selectAndConfirm(int index) {
			if (!showingLevelUp) {
				return;
			}
			applyChoice(index);
		}

		/** Navega horizontalmente entre os cards por A/D e setas esquerda/direita.
		 *  Clamp no limite (não wrap): o primeiro card não leva ao último. */
		public static void navigateLeft() {
			if (!showingLevelUp) {
				return;
			}
			setChoiceIndex(Math.max(0, choiceIndex - 1));
		}

		/** Navega horizontalmente entre os cards por A/D e setas esquerda/direita.
		 *  Clamp no limite (não wrap): o último card não leva ao primeiro. */
		public static void navigateRight() {
			if (!showingLevelUp) {
				return;
			}
			setChoiceIndex(Math.min(CHOICES - 1, choiceIndex + 1));
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
			// Guarda defensiva: slot vazio não deve travar o jogo.
			if (upgrade == null) {
				continue;
			}
			int cardX = startX + i * (cardWidth + gap);
			g.setColor(i == choiceIndex ? upgrade.accent : new Color(40, 44, 54, 230));
			g.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 14, 14);
			g.setColor(Color.white);
			String label = upgrade.label;
			g.drawString(label, cardX + 12, cardY + cardHeight / 2 + 6);
		}

		g.setFont(new Font("arial", Font.PLAIN, 14));
		g.setColor(new Color(200, 200, 200));
		String hint = "Setas/A/D para escolher — Enter para confirmar — 1, 2, 3 para escolher direto";
		g.drawString(hint, (screenWidth - g.getFontMetrics().stringWidth(hint)) / 2, cardY + cardHeight + 30);
	}

	public static void reset() {
		xp = 0;
		playerLevel = 1;
		// Se a tela de level-up estiver aberta, fecha-a antes de recriar as
		// opções — era aqui que o render acessava um slot nulo e crashava.
		dismiss();
		pendingChoices = new Upgrade[CHOICES];
		for (int i = 0; i < CHOICES; i++) {
			pendingChoices[i] = Upgrade.values()[i % Upgrade.values().length];
		}
		choiceIndex = 0;
	}

	/** Zera o progresso e os bônus temporários de uma nova campanha. */
	public static void resetProgress() {
		reset();
		maxLifeBonus = 0;
		maxManaBonus = 0;
		maxShieldBonus = 0;
		magnetRadiusBonus = 0;
	}

	/** Serializa os bônus de máximos escolhidos na campanha atual. */
	public static java.util.Map<String, Object> serializeBonuses() {
		java.util.Map<String, Object> map = new java.util.HashMap<String, Object>();
		map.put("maxLifeBonus", maxLifeBonus);
		map.put("maxManaBonus", maxManaBonus);
		map.put("maxShieldBonus", maxShieldBonus);
		map.put("magnetRadiusBonus", magnetRadiusBonus);
		map.put("xp", xp);
		map.put("playerLevel", playerLevel);
		return map;
	}

	/** Restaura os bônus de máximos de um save antigo ou atual. */
	public static void deserializeBonuses(Object raw) {
		if (!(raw instanceof java.util.Map<?, ?>)) {
			return;
		}
		java.util.Map<?, ?> map = (java.util.Map<?, ?>) raw;
		maxLifeBonus = Math.max(0, toInt(map.get("maxLifeBonus")));
		maxManaBonus = Math.max(0, toInt(map.get("maxManaBonus")));
		maxShieldBonus = Math.max(0, toInt(map.get("maxShieldBonus")));
		magnetRadiusBonus = Math.max(0, toInt(map.get("magnetRadiusBonus")));
		if (map.get("xp") instanceof Number) {
			xp = Math.max(0.0, ((Number) map.get("xp")).doubleValue());
		}
		if (map.get("playerLevel") instanceof Number) {
			playerLevel = Math.max(1, Math.min(MAX_PLAYER_LEVEL,
					((Number) map.get("playerLevel")).intValue()));
		}
	}
}
