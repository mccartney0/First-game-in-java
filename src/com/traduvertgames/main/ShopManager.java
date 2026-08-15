package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;

import com.traduvertgames.entities.Player;

/**
 * Loja entre fases: o jogador gasta a pontuação acumulada em upgrades antes
 * de avançar para a próxima fase.
 */
public final class ShopManager {

	public enum ShopItem {
		CURAR("Recuperar 60 de vida", 800),
		ESCUDO("Recarregar 60 de escudo", 600),
		MANA("Recarregar 150 de mana", 700),
		ENERGIA("Recarregar 80 de energia", 500),
		VIDA_MAXIMA("+20 vida máxima", 1500),
		ESCUDO_MAXIMO("+25 escudo máximo", 1200),
		ARMA("Desbloquear a próxima arma", 2500);

		public final String label;
		public final int price;

		ShopItem(String label, int price) {
			this.label = label;
			this.price = price;
		}
	}

	private static final ShopItem[] ITEMS = ShopItem.values();
	private static int selection = 0;
	private static boolean open = false;
	private static String feedback = "";
	private static int feedbackTimer = 0;
	/** Frames em que o ESC deve ser ignorado (evita reabrir menu ao segurar ESC). */
	private static int escCooldown = 0;

	/** O ESC deve ser ignorado nesta chamada do handler? */
	public static boolean isEscOnCooldown() {
		return escCooldown > 0;
	}

	private ShopManager() {
	}

	public static boolean isOpen() {
		return open;
	}

	public static void open() {
		open = true;
		selection = 0;
		feedback = "";
		feedbackTimer = 0;
		Game.gameState = "SHOP";
	}

	public static void close() {
		if (!open) {
			return;
		}
		open = false;
		Game.gameState = "NORMAL";
		// Evita que o key-repeat do ESC reabra o menu de pausa imediatamente.
		escCooldown = 15;
	}

	/** Navegação via setas/W-S: exposta para o handler de teclado do Game. */
	public static void navigateUp() {
		if (!open) {
			return;
		}
		selection = (selection - 1 + ITEMS.length) % ITEMS.length;
	}

	/** Navegação via setas/W-S: exposta para o handler de teclado do Game. */
	public static void navigateDown() {
		if (!open) {
			return;
		}
		selection = (selection + 1) % ITEMS.length;
	}

	/** Compra o item selecionado: exposta para o Enter do Game. */
	public static void purchaseSelected() {
		if (!open) {
			return;
		}
		purchase(ITEMS[selection]);
	}

	/** Atualização por frame (timer do feedback). */
	public static void update() {
		if (!open) {
			return;
		}
		if (feedbackTimer > 0) {
			feedbackTimer--;
			if (feedbackTimer == 0) {
				feedback = "";
			}
		}
		if (escCooldown > 0) {
			escCooldown--;
		}
	}

	private static void purchase(ShopItem item) {
		if (Game.getScore() < item.price) {
			feedback = "Pontuação insuficiente!";
			feedbackTimer = 90;
			return;
		}
		boolean purchaseSucceeded = true;
		String purchaseFeedback = null;
		switch (item) {
		case CURAR:
			purchaseFeedback = "Vida recuperada!";
			Game.addScore(-item.price);
			Player.life = Math.min(Player.life + 60, Player.maxLife);
			break;
		case ESCUDO:
			purchaseFeedback = "Escudo recarregado!";
			Game.addScore(-item.price);
			Player.shield = Math.min(Player.shield + 60, Player.maxShield);
			break;
		case MANA:
			purchaseFeedback = "Mana recarregada!";
			Game.addScore(-item.price);
			Player.mana = Math.min(Player.mana + 150, Player.maxMana);
			break;
		case ENERGIA:
			purchaseFeedback = "Energia recarregada!";
			Game.addScore(-item.price);
			if (Game.player != null) {
				Game.player.addWeaponEnergy(80);
			}
			break;
		case VIDA_MAXIMA:
			purchaseFeedback = "Vida máxima aumentada!";
			Game.addScore(-item.price);
			Player.maxLife += 20;
			Player.life += 20;
			break;
		case ESCUDO_MAXIMO:
			purchaseFeedback = "Escudo máximo aumentado!";
			Game.addScore(-item.price);
			Player.maxShield += 25;
			Player.shield += 25;
			break;
		case ARMA:
			if (unlockNextWeapon()) {
				Game.addScore(-item.price);
				purchaseFeedback = "Nova arma desbloqueada!";
			} else {
				purchaseSucceeded = false;
				purchaseFeedback = "Todas as armas já desbloqueadas!";
			}
			break;
		default:
			purchaseSucceeded = false;
			break;
		}
		feedback = purchaseFeedback;
		feedbackTimer = 90;
		// Após uma compra efetuada, a loja fecha sozinha para o jogo continuar.
		if (purchaseSucceeded) {
			close();
		}
		return;
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
	public static void render(java.awt.Graphics g) {
		if (!open) {
			return;
		}
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		g.setColor(new Color(0, 0, 0, 200));
		g.fillRect(0, 0, screenWidth, screenHeight);

		Font titleFont = new Font("arial", Font.BOLD, 26);
		Font labelFont = new Font("arial", Font.BOLD, 16);
		Font rowFont = new Font("arial", Font.PLAIN, 15);
		Font hintFont = new Font("arial", Font.PLAIN, 13);

		g.setFont(titleFont);
		g.setColor(Color.yellow);
		String title = "Loja entre fases";
		g.drawString(title, (screenWidth - g.getFontMetrics().stringWidth(title)) / 2, 90);

		g.setFont(labelFont);
		g.setColor(new Color(255, 193, 7));
		String scoreLabel = "Pontuação disponível: " + Game.getScore();
		g.drawString(scoreLabel, (screenWidth - g.getFontMetrics().stringWidth(scoreLabel)) / 2, 118);

		int panelWidth = 380;
		int panelX = (screenWidth - panelWidth) / 2;
		int panelY = 140;
		int lineHeight = 30;
		int panelHeight = ITEMS.length * lineHeight + 20;
		g.setColor(new Color(14, 18, 28, 235));
		g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);

		g.setFont(rowFont);
		for (int i = 0; i < ITEMS.length; i++) {
			ShopItem item = ITEMS[i];
			int rowY = panelY + 14 + lineHeight * i;
			boolean selected = selection == i;
			if (selected) {
				g.setColor(new Color(76, 86, 110));
				g.fillRoundRect(panelX + 6, rowY - 20, panelWidth - 12, 26, 10, 10);
			}
			boolean canAfford = Game.getScore() >= item.price;
			Color rowColor = selected ? Color.yellow : (canAfford ? Color.white : Color.lightGray);
			g.setColor(rowColor);

			String label = "> " + item.label;
			String price = String.format("%d pts", item.price);
			java.awt.FontMetrics fm = g.getFontMetrics();
			g.drawString(label, panelX + 20, rowY);
			int priceWidth = fm.stringWidth(price);
			int priceX = panelX + panelWidth - 20 - priceWidth;
			// Separador visual entre o rótulo e o preço, evitando sobreposição.
			g.setColor(new Color(60, 66, 80));
			g.fillRect(panelX + panelWidth - 120, rowY - 14, 1, 18);
			g.setColor(canAfford ? new Color(130, 200, 130) : Color.lightGray);
			g.drawString(price, priceX, rowY);
		}

		if (!feedback.isEmpty()) {
			g.setFont(labelFont);
			boolean positive = !feedback.contains("insuficiente") && !feedback.contains("Todas");
			g.setColor(positive ? new Color(130, 220, 130) : new Color(255, 120, 120));
			String fb = feedback;
			g.drawString(fb, (screenWidth - g.getFontMetrics().stringWidth(fb)) / 2,
					panelY + panelHeight + 26);
		}

		g.setFont(hintFont);
		g.setColor(new Color(200, 200, 200));
		String hint = "Setas/W-S para navegar — Enter para comprar — ESC para fechar";
		g.drawString(hint, (screenWidth - g.getFontMetrics().stringWidth(hint)) / 2,
				panelY + panelHeight + 56);
	}
}
