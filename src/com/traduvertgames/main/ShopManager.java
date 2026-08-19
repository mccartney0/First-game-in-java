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
			ARMA("Desbloquear a próxima arma", 2500),
			DRONE_SCOUT("Drone batedor (atira sozinho)", 3200),
			SHIELD_BOT("Drone de escudo (+2 escudo/s)", 2600),
			FAIRY("Fada curadora (+1 vida/s)", 2800),
			COMPANION_CORE("Núcleo de habilidade do companion (+1 nível)", 3000),
			SKIN_DOURADO("Skin do companion: Dourado", 1200),
			SKIN_NEON("Skin do companion: Neon ciano", 1500),
			SKIN_CARMESIM("Skin do companion: Carmesim", 1800);

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
		closeOnNextEnter = false;
		purchaseSelection = -1;
		escCooldown = 0;
		Game.gameState = "SHOP";
		SoundManager.play(SoundManager.Event.SHOP);
	}

	public static void close() {
		if (!open) {
			return;
		}
		open = false;
		closeOnNextEnter = false;
		purchaseSelection = -1;
		Game.gameState = "NORMAL";
		// Evita que o key-repeat do ESC reabra o menu de pausa imediatamente.
		escCooldown = 15;
		SoundManager.play(SoundManager.Event.SHOP);
	}

	/** Navegação via setas/W-S: exposta para o handler de teclado do Game. */
	public static void navigateUp() {
		if (!open) {
			return;
		}
		closeOnNextEnter = false; // navegar cancela o "fecha" pós-compra
		purchaseSelection = -1;
		selection = (selection - 1 + ITEMS.length) % ITEMS.length;
	}

	/** Navegação via setas/W-S: exposta para o handler de teclado do Game. */
	public static void navigateDown() {
		if (!open) {
			return;
		}
		closeOnNextEnter = false; // navegar cancela o "fecha" pós-compra
		purchaseSelection = -1;
		selection = (selection + 1) % ITEMS.length;
	}

	/** Navegação por A/D: mesmo comportamento das setas (lista vertical). */
	public static void navigateA() {
		navigateUp();
	}

	/** Navegação por A/D: mesmo comportamento das setas (lista vertical). */
	public static void navigateD() {
		navigateDown();
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
		// Feedback sonoro de compra bem-sucedida (rodada 15): itens de skin e
		// companion já tocam seu próprio som; a compra genérica toca PURCHASE.
		SoundManager.play(SoundManager.Event.PURCHASE);
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
		case DRONE_SCOUT:
			Game.addScore(-item.price);
			com.traduvertgames.entities.Companion.spawn(
					com.traduvertgames.entities.Companion.CompanionType.SCOUT, -1);
			purchaseFeedback = "Drone batedor acoplado!";
			break;
		case SHIELD_BOT:
			Game.addScore(-item.price);
			com.traduvertgames.entities.Companion.spawn(
					com.traduvertgames.entities.Companion.CompanionType.SHIELD_BOT, -1);
			purchaseFeedback = "Drone de escudo acoplado!";
			break;
			case FAIRY:
				Game.addScore(-item.price);
				com.traduvertgames.entities.Companion.spawn(
						com.traduvertgames.entities.Companion.CompanionType.FAIRY, -1);
				purchaseFeedback = "Fada curadora acoplada!";
				break;
			case COMPANION_CORE:
				com.traduvertgames.entities.Companion active =
						com.traduvertgames.entities.Companion.getActive();
				if (active != null && active.getAbilityLevel() < 5) {
					Game.addScore(-item.price);
					active.upgradeAbility();
					purchaseFeedback = "Habilidade do companion evoluiu para Nv." + active.getAbilityLevel() + "!";
				} else {
					purchaseSucceeded = false;
					purchaseFeedback = active == null ? "Compre um companion antes do núcleo!"
							: "Habilidade já está no nível máximo!";
				}
				break;
			case SKIN_DOURADO:
				if (applyCompanionSkin(com.traduvertgames.entities.Companion.CompanionSkin.DOURADO)) {
					Game.addScore(-item.price);
					purchaseFeedback = "Skin Dourado aplicada ao companion!";
					SoundManager.play(SoundManager.Event.SKIN_APPLY);
				} else {
				purchaseSucceeded = false;
				purchaseFeedback = "Compre um companion antes da skin!";
			}
			break;
		case SKIN_NEON:
				if (applyCompanionSkin(com.traduvertgames.entities.Companion.CompanionSkin.NEON)) {
					Game.addScore(-item.price);
					purchaseFeedback = "Skin Neon aplicada ao companion!";
					SoundManager.play(SoundManager.Event.SKIN_APPLY);
				} else {
				purchaseSucceeded = false;
				purchaseFeedback = "Compre um companion antes da skin!";
			}
			break;
		case SKIN_CARMESIM:
				if (applyCompanionSkin(com.traduvertgames.entities.Companion.CompanionSkin.CARMESIM)) {
					Game.addScore(-item.price);
					purchaseFeedback = "Skin Carmesim aplicada ao companion!";
					SoundManager.play(SoundManager.Event.SKIN_APPLY);
				} else {
				purchaseSucceeded = false;
				purchaseFeedback = "Compre um companion antes da skin!";
			}
			break;
		default:
			purchaseSucceeded = false;
			break;
		}
		feedback = purchaseFeedback;
		feedbackTimer = 90;
		// Após uma compra efetuada, a loja PERMANECE aberta (rodada de QA):
		// assim o jogador consegue comprar vários itens em uma única visita
		// e vê o feedback da compra antes de sair. Fecha-se com ESC ou
		// pressionando Enter com o feedback ativo (confirma e sai).
		if (purchaseSucceeded) {
			closeOnNextEnter = true;
			purchaseSelection = selection;
		} else {
			purchaseSelection = -1;
		}
		return;
	}

	/** Após uma compra bem-sucedida, o próximo Enter fecha a loja. Navegar
	 *  (setas/A-D) antes do Enter cancela o "fecha" e compra o novo item
	 *  selecionado — assim o jogador consegue fazer várias compras seguidas. */
	private static boolean closeOnNextEnter = false;
	private static int purchaseSelection = -1;

	/** Enter com loja aberta: confirma a compra e, se acabou de comprar,
	 *  fecha a loja na confirmação. */
	public static void confirmOrPurchase() {
		if (!open) {
			return;
		}
		if (closeOnNextEnter) {
			closeOnNextEnter = false;
			close();
			return;
		}
		purchaseSelected();
	}

	private static boolean applyCompanionSkin(
			com.traduvertgames.entities.Companion.CompanionSkin skin) {
		com.traduvertgames.entities.Companion active =
				com.traduvertgames.entities.Companion.getActive();
		if (active == null) {
			return false;
		}
		active.setSkin(skin);
		return true;
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

		// Preview da skin de companion selecionada (rodada companions-ux):
		// um mini orbe mostra a cor exata da skin antes de comprar.
		renderSkinPreview(g, screenWidth, screenHeight);

		g.setFont(hintFont);
		g.setColor(new Color(200, 200, 200));
			String hint = "Setas/A-D/W-S para navegar — Enter para comprar (Enter de novo fecha) — ESC para fechar";
		g.drawString(hint, (screenWidth - g.getFontMetrics().stringWidth(hint)) / 2,
				panelY + panelHeight + 56);
	}

	/** Preview da skin/companion selecionado (rodada 19): o orbe reutiliza a cor
	 *  real do companion (com a skin, se já for dono dela; caso contrário,
	 *  simula a cor da skin comprada) e um cartão mostra o nome do tipo. */
	private static void renderSkinPreview(java.awt.Graphics g, int screenWidth,
			int screenHeight) {
		if (selection >= ITEMS.length) {
			return;
		}
		ShopItem item = ITEMS[selection];
		com.traduvertgames.entities.Companion.CompanionSkin skin = null;
		String skinName = null;
		switch (item) {
		case SKIN_DOURADO:
			skin = com.traduvertgames.entities.Companion.CompanionSkin.DOURADO;
			skinName = "Pré-visualização: Dourado";
			break;
		case SKIN_NEON:
			skin = com.traduvertgames.entities.Companion.CompanionSkin.NEON;
			skinName = "Pré-visualização: Neon ciano";
			break;
		case SKIN_CARMESIM:
			skin = com.traduvertgames.entities.Companion.CompanionSkin.CARMESIM;
			skinName = "Pré-visualização: Carmesim";
			break;
		default:
			// Companions e demais itens também mostram o pet atual: se já houver
			// um companion ativo, o orbe reflete a cor dele em tempo real.
			com.traduvertgames.entities.Companion active =
					com.traduvertgames.entities.Companion.getActive();
			if (active == null) {
				return;
			}
			skin = null;
			skinName = "Ativo: " + active.typeLabel();
			break;
		}
		Color preview;
		if (skin == null) {
			com.traduvertgames.entities.Companion active =
					com.traduvertgames.entities.Companion.getActive();
			preview = active != null ? active.colorForHud() : new Color(255, 203, 5);
		} else {
			// Simula a cor da skin antes de comprar (sem alterar o pet ativo).
			com.traduvertgames.entities.Companion active =
					com.traduvertgames.entities.Companion.getActive();
			com.traduvertgames.entities.Companion.CompanionSkin previous =
					active != null ? active.getSkin() : null;
			if (active != null) {
				active.setSkin(skin);
			}
			preview = active != null ? active.colorForHud() : new Color(255, 203, 5);
			if (active != null) {
				active.setSkin(previous != null ? previous
						: com.traduvertgames.entities.Companion.CompanionSkin.PADRAO);
			}
		}
		int orbX = screenWidth / 2 + 220;
		int orbY = 200;
		int orbSize = 36;
		// Aura e anel pulsante imitando o estilo do companion no jogo.
		g.setColor(new Color(preview.getRed(), preview.getGreen(), preview.getBlue(), 80));
		g.fillOval(orbX - 8, orbY - 8, orbSize + 16, orbSize + 16);
		g.setColor(new Color(preview.getRed(), preview.getGreen(), preview.getBlue(), 120));
		g.drawOval(orbX - 5, orbY - 5, orbSize + 10, orbSize + 10);
		g.setColor(preview);
		g.fillOval(orbX + 4, orbY + 4, orbSize - 8, orbSize - 8);
		g.setColor(Color.WHITE);
		g.fillOval(orbX + 8, orbY + 7, 6, 6);
		// Painel de apoio atrás da amostra.
		g.setFont(new Font("arial", Font.PLAIN, 13));
		int w = g.getFontMetrics().stringWidth(skinName);
		g.setColor(new Color(14, 18, 28, 220));
		g.fillRoundRect(orbX - w / 2 - 14, orbY + orbSize + 6, w + 28, 24, 10, 10);
		g.setColor(Color.yellow);
		g.drawString(skinName, orbX - w / 2, orbY + orbSize + 22);
	}
}
