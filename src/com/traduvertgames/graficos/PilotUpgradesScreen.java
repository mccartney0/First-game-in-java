package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Map;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.state.PilotUpgrades;
import com.traduvertgames.state.PilotUpgrades.Upgrade;

/**
 * Rodada 29 — Tela de melhorias permanentes do piloto (metagame).
 *
 * Exibida sobre o menu principal (ou a pausa), lista os quatro upgrades
 * permanentes com custo, nível atual e descrição. Enter/Space compram o
 * upgrade selecionado quando há créditos suficientes; ESC/back fecha.
 */
public final class PilotUpgradesScreen {

	private static boolean open = false;
	private static int selection = 0;
	private static String feedback = "";
	private static Color feedbackColor = Color.WHITE;

	private PilotUpgradesScreen() {
	}

	/** @return true se a tela está aberta. */
	public static boolean isOpen() {
		return open;
	}

	/** Abre a tela e reposiciona a seleção em um upgrade comprável, se houver. */
	public static void open() {
		open = true;
		selection = 0;
		feedback = "";
	}

	/** Fecha a tela. */
	public static void close() {
		open = false;
		selection = 0;
		feedback = "";
	}

	/** Alterna a tela (enter/ESC do menu principal). */
	public static void toggle() {
		if (open) {
			close();
		} else {
			open();
		}
	}

	/** Move a seleção para cima (circular). */
	public static void up() {
		selection = (selection - 1 + Upgrade.values().length) % Upgrade.values().length;
		com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.MENU_SELECT);
	}

	/** Move a seleção para baixo (circular). */
	public static void down() {
		selection = (selection + 1) % Upgrade.values().length;
		com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.MENU_SELECT);
	}

	/** Confirma a compra do upgrade selecionado. */
	public static boolean confirm() {
		Upgrade upgrade = Upgrade.values()[selection];
		if (PilotUpgrades.canAfford(upgrade)) {
			Map<String, Object> beforePurchase = PilotUpgrades.serialize();
			if (!PilotUpgrades.buy(upgrade)) {
				return false;
			}
			if (!SaveManager.saveMetagame()) {
				PilotUpgrades.deserialize(beforePurchase);
				feedback = "Falha ao salvar a compra. Nenhum crédito foi gasto.";
				feedbackColor = new Color(244, 67, 54);
				return false;
			}
			com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.LEVELUP);
			feedback = "Compra concluída: " + PilotUpgrades.labels()[selection]
					+ " nível " + PilotUpgrades.getLevel(upgrade);
			feedbackColor = new Color(100, 255, 120);
			return true;
		} else {
			int cost = PilotUpgrades.getNextCost(upgrade);
			feedback = cost < 0
					? "Nível máximo alcançado"
					: "Créditos insuficientes (" + cost + " necessários)";
			feedbackColor = new Color(244, 67, 54);
			return false;
		}
	}

	/**
	 * Renderiza a tela sobre o jogo. Fundo escuro translúcido, saldo de
	 * créditos no topo, uma linha por upgrade com custo e nível.
	 */
	public static void draw(Graphics g) {
		if (!open) {
			return;
		}
		int w = Game.WIDTH * Game.SCALE;
		int h = Game.HEIGHT * Game.SCALE;

		g.setColor(new Color(0, 0, 0, 190));
		g.fillRect(0, 0, w, h);

		Graphics2D g2 = (Graphics2D) g;
		Font titleFont = new Font("Dialog", Font.BOLD, 42);
		Font font = new Font("Dialog", Font.PLAIN, 24);
		Font small = new Font("Dialog", Font.PLAIN, 18);

		g2.setFont(titleFont);
		g2.setColor(new Color(255, 214, 10));
		g2.drawString("MELHORIAS DO PILOTO", w / 2 - g2.getFontMetrics().stringWidth("MELHORIAS DO PILOTO") / 2, 90);

		g2.setFont(font);
		g2.setColor(Color.WHITE);
		String creditsLine = "CREDITOS: " + PilotUpgrades.getCredits();
		g2.drawString(creditsLine, w / 2 - g2.getFontMetrics().stringWidth(creditsLine) / 2, 140);

		String[] labels = PilotUpgrades.labels();
		String[] descriptions = {
				"vida maxima +25 por nivel (max 8)",
				"regeneracao passiva (max 5)",
				"escudo inicial +20% por nivel (max 5)",
				"mana/municao inicial +15% por nivel (max 5)"
		};

		int y = 210;
		Upgrade[] values = Upgrade.values();
		for (int i = 0; i < values.length; i++) {
			Upgrade u = values[i];
			int level = PilotUpgrades.getLevel(u);
			int cost = PilotUpgrades.getNextCost(u);
			boolean affordable = PilotUpgrades.canAfford(u);
			if (selection == i) {
				g.setColor(new Color(255, 255, 0, 60));
				g.fillRect(w / 2 - 380, y - 28, 760, 46);
				g.setColor(new Color(255, 214, 10));
				g.drawString(">", w / 2 - 395, y);
			}
			g2.setFont(font);
			String label = labels[i].toUpperCase() + "  [nivel " + level + "/" + PilotUpgrades.getMaxLevel(u) + "]";
			g2.setColor(affordable ? Color.WHITE : new Color(150, 150, 150));
			g2.drawString(label, w / 2 - 360, y);
			g2.setFont(small);
			g2.setColor(new Color(200, 200, 200));
			g2.drawString(descriptions[i], w / 2 - 360, y + 22);
			String costLabel = cost < 0 ? "MAXIMO" : affordable ? ("COMPRAR " + cost) : ("custo " + cost);
			g2.setColor(cost < 0 ? new Color(100, 255, 100) : affordable ? new Color(255, 214, 10) : new Color(150, 150, 150));
			g2.setFont(small);
			g2.drawString(costLabel, w / 2 + 290, y);
			y += 76;
		}

		g2.setFont(small);
		g2.setColor(new Color(200, 200, 200));
		String footer = "ENTER/SPACE: comprar  |  ESC: voltar";
		g2.drawString(footer, w / 2 - g2.getFontMetrics().stringWidth(footer) / 2, y + 30);
		if (!feedback.isEmpty()) {
			g2.setFont(new Font("Dialog", Font.BOLD, 18));
			g2.setColor(feedbackColor);
			g2.drawString(feedback,
					w / 2 - g2.getFontMetrics().stringWidth(feedback) / 2,
					y + 62);
		}
	}
}
