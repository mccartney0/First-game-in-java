package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.entities.Player;

/**
 * Inventário visual (rodada 22). Itens consumíveis coletados no mapa ficam
 * guardados no inventário em vez de aplicar o efeito de imediato; o jogador
 * abre o painel com I, navega com as setas (ou A/D/W/S) e usa o item
 * selecionado com Enter/Espaço.
 *
 * Painel em espaço escalado da janela (overlayG), com cantos no padrão da
 * HUD e indicação de quantidade por célula. Itens de uso único reduzem a
 * quantidade; o progresso é persistido junto com a sessão do save.
 */
public final class InventoryManager {

	/** Slots do inventário: 8 consumíveis, 2x4. */
	public enum ItemType {
		/** Kit de reparo imediato: restaura vida e escudo. */
		MEDKIT("MediKit", new Color(70, 255, 120), "+40 vida"),
		/** Cápsula de nanobots: vida extra + escudo. */
		NANOMEDKIT("NanoMed", new Color(110, 190, 255), "+30 vida/escudo"),
		/** Célula de energia: restaura mana e energia da arma. */
		ENERGY_CELL("Célula", new Color(255, 235, 59), "+60 mana"),
		/** Órbita de escudo: reforço temporário. */
		SHIELD_ORB("Escudo", new Color(130, 220, 255), "+15 escudo"),
		/** Munição de munição extra (módulo overclock). */
		OVERCLOCK("Overclock", new Color(255, 130, 220), "bônus combo"),
		/** Pente de munição: recarga rápida de munição. */
		AMMO_PACK("Munição", new Color(255, 160, 60), "recarga"),
		/** Chave de acesso a áreas restritas (missões secundárias). */
		ACCESS_KEY("Chave", new Color(255, 210, 80), "acesso"),
		/** Dados criptografados: moeda de troca com NPCs e loja. */
		DATA_CORE("Dados", new Color(180, 160, 255), "troca");

		public final String displayName;
		public final Color color;
		public final String hint;

		ItemType(String displayName, Color color, String hint) {
			this.displayName = displayName;
			this.color = color;
			this.hint = hint;
		}
	}

	private static final ItemType[] SLOTS = ItemType.values();

	private static final Map<ItemType, Integer> counts = new HashMap<ItemType, Integer>();

	private static boolean open = false;
	private static int selected = 0;
	/** Cooldown do painel após usar um item (evita consumo acidental duplo). */
	private static int useCooldown = 0;

	private InventoryManager() {
	}

	public static boolean isOpen() {
		return open;
	}

	public static int count(ItemType item) {
		Integer c = counts.get(item);
		return c != null ? c.intValue() : 0;
	}

	/** Adiciona o item ao inventário (quantidade) — o item não desaparece do
	 *  mapa ainda; o coletor cuida de remover a entidade (ver hook abaixo). */
	public static void add(ItemType item, int quantity) {
		if (quantity <= 0) {
			return;
		}
		int current = count(item);
		counts.put(item, current + quantity);
	}

	/** Avisa que a entidade do item acabou de ser adicionada ao inventário
	 *  (conveniência para os pontos de coleta no Player). */
	public static void addPickup(ItemType item) {
		add(item, 1);
		// Feedback visual discreto no mundo (número flutuante no jogador).
		com.traduvertgames.entities.FloatingText.show(
				"INV +" + item.displayName.toUpperCase(),
				Game.WIDTH * Game.SCALE / 2, Game.SCALE * 60, item.color, 60);
	}

	/**
	 * Abre/fecha o inventário. Quando fechado, o jogo continua em NORMAL
	 * (o painel é um overlay leve); ao abrir com o jogo em combate a
	 * velocidade não pausa — só a movimentação é bloqueada enquanto o
	 * painel estiver aberto.
	 */
	public static void toggle() {
		if (!"NORMAL".equals(Game.gameState)
				|| com.traduvertgames.dialogue.DialogueManager.isActive()) {
			return;
		}
		open = !open;
		if (open) {
			// Seleciona o primeiro slot com itens, se houver.
			for (int i = 0; i < SLOTS.length; i++) {
				if (count(SLOTS[i]) > 0) {
					selected = i;
					break;
				}
			}
		}
	}

	public static void navigateUp() {
		if (!open) {
			return;
		}
		selected = (selected - 2 + SLOTS.length) % SLOTS.length;
	}

	public static void navigateDown() {
		if (!open) {
			return;
		}
		selected = (selected + 2) % SLOTS.length;
	}

	public static void navigateLeft() {
		if (!open) {
			return;
		}
		selected = (selected - 1 + SLOTS.length) % SLOTS.length;
	}

	public static void navigateRight() {
		if (!open) {
			return;
		}
		selected = (selected + 1) % SLOTS.length;
	}

	/** Usa o item do slot selecionado (Enter/Espaço). */
	public static void useSelected() {
		if (!open || useCooldown > 0) {
			return;
		}
		ItemType item = SLOTS[selected];
		if (count(item) <= 0) {
			return;
		}
		Player p = Game.player;
		if (p == null) {
			return;
		}
		SoundManager.play(SoundManager.Event.PICKUP);
		int previous = count(item);
		switch (item) {
		case MEDKIT:
			p.heal(40);
			p.addShield(10);
			com.traduvertgames.entities.FloatingText.show("+40 VIDA",
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 50, item.color, 70);
			break;
		case NANOMEDKIT:
			p.heal(30);
			p.addShield(30);
			com.traduvertgames.entities.FloatingText.show("+30 VIDA/ESCUDO",
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 50, item.color, 70);
			break;
		case ENERGY_CELL:
			p.addMana(60);
			p.addWeaponEnergy(2);
			com.traduvertgames.entities.FloatingText.show("+60 MANA",
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 50, item.color, 70);
			break;
		case SHIELD_ORB:
			p.addShield(15);
			com.traduvertgames.entities.FloatingText.show("+15 ESCUDO",
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 50, item.color, 70);
			break;
		case OVERCLOCK:
			Game.applyComboSurge(2, Game.getComboBaseDuration());
			com.traduvertgames.entities.FloatingText.show("COMBO x" + Math.min(5, Game.getComboMultiplier() + 2),
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 50, item.color, 70);
			break;
		case AMMO_PACK:
			p.refillCurrentWeapon();
			com.traduvertgames.entities.FloatingText.show("MUNIÇÃO RECARGADA",
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 50, item.color, 70);
			break;
		default:
			// Chave e Dados não têm efeito direto — são itens de troca/missão.
			com.traduvertgames.entities.FloatingText.show(item.displayName.toUpperCase() + " — item de troca",
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 50, item.color, 70);
			return;
		}
		counts.put(item, previous - 1);
		useCooldown = 15;
	}

	/** Consome um item por nome genérico (usado por missões secundárias e NPC). */
	public static boolean consume(ItemType item, int quantity) {
		if (count(item) < quantity) {
			return false;
		}
		counts.put(item, count(item) - quantity);
		return true;
	}

	/** Persistência: mapa {item -> quantidade} sem chaves zeradas. */
	public static Map<String, Integer> serialize() {
		Map<String, Integer> snapshot = new HashMap<String, Integer>();
		for (Map.Entry<ItemType, Integer> entry : counts.entrySet()) {
			if (entry.getValue() > 0) {
				snapshot.put(entry.getKey().name(), entry.getValue());
			}
		}
		return snapshot;
	}

	/** Persistência: restaura quantidades do save. */
	public static void deserialize(Map<String, Integer> snapshot) {
		counts.clear();
		if (snapshot == null) {
			return;
		}
		for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
			try {
				ItemType item = ItemType.valueOf(entry.getKey());
				int qty = Math.max(0, entry.getValue() == null ? 0 : entry.getValue().intValue());
				if (qty > 0) {
					counts.put(item, qty);
				}
			} catch (IllegalArgumentException error) {
				// Item antigo/desconhecido no save: ignorar sem quebrar o load.
			}
		}
	}

	/** Zera o inventário (novo jogo/troca de fase). */
	public static void reset() {
		counts.clear();
		open = false;
		selected = 0;
		useCooldown = 0;
	}

	/** Decai o cooldown de uso a cada frame. */
	public static void update() {
		if (useCooldown > 0) {
			useCooldown--;
		}
	}

	/**
	 * Renderiza o painel do inventário no espaço escalado da janela
	 * (overlayG) e a barra de contadores ao longo da base da HUD.
	 */
	public static void render(Graphics2D g) {
		int scale = Game.SCALE;
		int windowWidth = Game.WIDTH * scale;
		int windowHeight = Game.HEIGHT * scale;

		// 1) Barra de contadores na base da HUD (sempre visível no NORMAL,
		//    mostrando os itens com quantidade > 0).
		if ("NORMAL".equals(Game.gameState) && !open) {
			int x = 16;
			int y = windowHeight - 46;
			for (ItemType item : SLOTS) {
				int c = count(item);
				if (c > 0) {
					g.setFont(new Font("arial", Font.BOLD, 11));
					g.setColor(new Color(0, 0, 0, 200));
					g.fillRoundRect(x - 2, y - 10, 88, 18, 6, 6);
					g.setColor(item.color);
					g.fillRect(x, y - 7, 8, 8);
					g.setColor(Color.white);
					g.drawString(item.displayName + " x" + c, x + 12, y);
					x += g.getFontMetrics().stringWidth(item.displayName + " x" + c) + 22;
					if (x > windowWidth - 120) {
						break;
					}
				}
			}
			// Dica do atalho I.
			g.setFont(new Font("arial", Font.BOLD, 10));
			g.setColor(new Color(176, 190, 197));
			g.drawString("I = inventario", 16, windowHeight - 16);
			return;
		}

		// 2) Painel aberto: grade 2x4 centralizada inferior.
		if (!open) {
			return;
		}
		int cellSize = 54 * scale / 4 + 6;
		int cols = 4;
		int rows = 2;
		int panelW = cols * cellSize + 24;
		int panelH = rows * cellSize + 44;
		int panelX = (windowWidth - panelW) / 2;
		int panelY = windowHeight - panelH - 24;

		// Fundo escuro com borda amarela (identidade da HUD/onboarding)
		g.setColor(new Color(0, 0, 0, 235));
		g.fillRoundRect(panelX, panelY, panelW, panelH, 12, 12);
		g.setColor(new Color(255, 235, 59));
		g.drawRoundRect(panelX, panelY, panelW, panelH, 12, 12);

		// Título
		g.setFont(new Font("arial", Font.BOLD, 14 * scale / 4 + 2));
		g.setColor(new Color(255, 235, 59));
		g.drawString("INVENTARIO", panelX + 16, panelY + 22);
		g.setFont(new Font("arial", Font.PLAIN, 10));
		g.setColor(new Color(176, 190, 197));
		g.drawString("Setas para navegar, Enter para usar, I para fechar", panelX + panelW - 260, panelY + 22);

		// Grade
		int startX = panelX + 12;
		int startY = panelY + 36;
		Font cellFont = new Font("arial", Font.BOLD, 10 * scale / 4 + 2);
		for (int i = 0; i < SLOTS.length; i++) {
			int row = i / cols;
			int col = i % cols;
			int cx = startX + col * cellSize;
			int cy = startY + row * cellSize;
			ItemType item = SLOTS[i];
			int c = count(item);

			// Célula
			g.setColor(i == selected ? new Color(255, 235, 59, 70) : new Color(30, 40, 50, 160));
			g.fillRoundRect(cx, cy, cellSize - 4, cellSize - 4, 6, 6);
			g.setColor(i == selected ? new Color(255, 235, 59) : new Color(90, 110, 130));
			g.drawRoundRect(cx, cy, cellSize - 4, cellSize - 4, 6, 6);

			if (c > 0) {
				// Ícone do item (quadrado colorido da cor do tipo)
				int iconSize = cellSize / 3;
				g.setColor(item.color);
				g.fillRect(cx + (cellSize - iconSize) / 2, cy + cellSize / 5, iconSize, iconSize);
				// Quantidade
				g.setFont(cellFont);
				g.setColor(Color.white);
				String qty = "x" + c;
				g.drawString(qty, cx + (cellSize - g.getFontMetrics().stringWidth(qty)) / 2,
						cy + cellSize - 10);
			}
		}

		// Dica do item selecionado
		ItemType selectedType = SLOTS[selected];
		g.setFont(new Font("arial", Font.BOLD, 11 * scale / 4 + 1));
		g.setColor(selectedType.color);
		String info = selectedType.displayName + " (" + selectedType.hint + ")";
		g.drawString(info, panelX + 16, panelY + panelH - 10);
	}
}
