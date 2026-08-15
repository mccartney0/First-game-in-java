package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.entities.Player;

/**
 * Loja entre fases: o jogador gasta a pontuação acumulada em upgrades antes
 * de avançar para a próxima fase.
 */
import com.traduvertgames.main.Game;

public final class ShopManager {

	public enum ShopItem {
		CURAR("Recuperar 60 de vida", 800),
		ESCUDO("Recarregar 60 de escudo", 600),
		ENERGIA("Recarregar 80 de energia", 500),
		VIDA_MAXIMA("+20 vida máxima", 1500),
		ESCUDO_MAXIMO("+25 escudo máximo", 1200),
		ARMA("Desbloquear a próxima arma", 2500),
		MANA("Recarregar 150 de mana", 700);

		public final String label;
		public final int price;

		ShopItem(String label, int price) {
			this.label = label;
			this.price = price;
		}
	}

	private static ShopItem[] items = ShopItem.values();
	private static int selection = 0;
	private static boolean open = false;
	private static String feedback = "";
	private static int feedbackTimer = 0;

	private ShopManager() {
	}

	public static boolean isOpen() {
		return open;
	}

	public static void open() {
		open = true;
		selection = 0;
		feedback = "";
		Game.gameState = "SHOP";
	}

	public static void close() {
		open = false;
		Game.gameState = "NORMAL";
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
			selection = (selection - 1 + items.length) % items.length;
		} else if (Game.getInstance().menu.down) {
			Game.getInstance().menu.down = false;
			selection = (selection + 1) % items.length;
		} else if (Game.getInstance().menu.enter) {
			Game.getInstance().menu.enter = false;
			purchase(items[selection]);
		}
		if (feedbackTimer > 0) {
			feedbackTimer--;
			if (feedbackTimer == 0) {
				feedback = "";
			}
		}
	}

	private static void purchase(ShopItem item) {
		if (Game.getScore() < item.price) {
			feedback = "Pontuação insuficiente!";
			feedbackTimer = 60;
			return;
		}
		switch (item) {
		case CURAR:
			Game.addScore(-item.price);
			Player.life = Math.min(Player.life + 60, Player.maxLife);
			feedback = "Vida recuperada!";
			break;
		case ESCUDO:
			Game.addScore(-item.price);
			Player.shield = Math.min(Player.shield + 60, Player.maxShield);
			feedback = "Escudo recarregado!";
			break;
		case ENERGIA:
			Game.addScore(-item.price);
			if (Game.player != null) {
				Game.player.addWeaponEnergy(80);
			}
			feedback = "Energia recarregada!";
			break;
		case VIDA_MAXIMA:
			Game.addScore(-item.price);
			Player.maxLife += 20;
			Player.life += 20;
			feedback = "Vida máxima aumentada!";
			break;
		case ESCUDO_MAXIMO:
			Game.addScore(-item.price);
			Player.maxShield += 25;
			Player.shield += 25;
			feedback = "Escudo máximo aumentado!";
			break;
		case ARMA:
			if (unlockNextWeapon()) {
				Game.addScore(-item.price);
				feedback = "Nova arma desbloqueada!";
			} else {
				feedback = "Todas as armas já desbloqueadas!";
				feedbackTimer = 60;
				return;
			}
			break;
		case MANA:
			Game.addScore(-item.price);
			Player.mana = Math.min(Player.mana + 150, Player.maxMana);
			feedback = "Mana recarregada!";
			break;
		default:
			break;
		}
		feedbackTimer = 60;
	}

	private static boolean unlockNextWeapon() {
		com.traduvertgames.entities.WeaponType[] order = com.traduvertgames.entities.WeaponType.values();
		if (Game.player == null) {
			return false;
		}
		for (com.traduvertgames.entities.WeaponType type : order) {
			if (!Game.player.hasWeaponUnlocked(type)) {
				Game.player.unlockWeapon(type);
				return true;
			}
		}
		return false;
	}

	/** Renderiza a loja dentro do jogo (canvas). */
	public static void render(Graphics g) {
		if (!open) {
			return;
		}
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		g.setColor(new Color(0, 0, 0, 180));
		g.fillRect(0, 0, screenWidth, screenHeight);

		g.setFont(new Font("arial", Font.BOLD, 26));
		g.setColor(Color.yellow);
		String title = "Loja entre fases";
		g.drawString(title, (screenWidth - g.getFontMetrics().stringWidth(title)) / 2, 90);

		g.setFont(new Font("arial", Font.BOLD, 18));
		g.setColor(new Color(255, 193, 7));
		String scoreLabel = "Pontuação disponível: " + Game.getScore();
		g.drawString(scoreLabel, (screenWidth - g.getFontMetrics().stringWidth(scoreLabel)) / 2, 120);

		g.setFont(new Font("arial", Font.PLAIN, 18));
		int panelX = (screenWidth - 340) / 2;
		int panelY = 140;
		int lineHeight = 34;
		int panelHeight = items.length * lineHeight + 24;
		g.setColor(new Color(14, 18, 28, 220));
		g.fillRoundRect(panelX, panelY, 340, panelHeight, 16, 16);

		for (int i = 0; i < items.length; i++) {
			ShopItem item = items[i];
			int rowY = panelY + 16 + lineHeight * i;
			boolean selected = selection == i;
			if (selected) {
				g.setColor(new Color(60, 68, 88));
				g.fillRoundRect(panelX + 6, rowY - 20, 328, 28, 10, 10);
				g.setColor(Color.yellow);
			} else {
				g.setColor(Color.white);
			}
			boolean canAfford = Game.getScore() >= item.price;
			String label = item.label;
			String price = String.format("%d pts", item.price);
			g.drawString("> " + label, panelX + 20, rowY);
			g.setColor(canAfford ? new Color(130, 200, 130) : Color.LIGHT_GRAY);
			g.drawString(price, panelX + 320 - g.getFontMetrics().stringWidth(price) - 16, rowY);
		}

		if (!feedback.isEmpty()) {
			g.setFont(new Font("arial", Font.BOLD, 18));
			g.setColor(new Color(130, 220, 130));
			g.drawString(feedback, (screenWidth - g.getFontMetrics().stringWidth(feedback)) / 2,
					panelY + panelHeight + 30);
		}

		g.setFont(new Font("arial", Font.PLAIN, 14));
		g.setColor(new Color(200, 200, 200));
		String hint = "Setas para navegar — Enter para comprar — ESC para fechar";
		g.drawString(hint, (screenWidth - g.getFontMetrics().stringWidth(hint)) / 2, panelY + panelHeight + 60);
	}
}
