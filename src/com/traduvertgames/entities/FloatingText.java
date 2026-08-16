package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Textos flutuantes temporários sobre o mapa (ex.: "-ESCUDO", "-MANA" do
 * dreno do Phantom, dano causado). Cada texto sobe e desaparece gradualmente.
 */
public final class FloatingText {

	private static final List<Item> items = new ArrayList<Item>();

	/** Mostra um texto flutuante na posição informada com a cor dada. */
	public static void show(String text, int x, int y, Color color) {
		items.add(new Item(text, x, y, color));
		// Limita a quantidade para não sobrecarregar a renderização.
		if (items.size() > 64) {
			items.remove(0);
		}
	}

	/** Atualiza (move e expira) os textos flutuantes ativos. */
	public static void update() {
		Iterator<Item> it = items.iterator();
		while (it.hasNext()) {
			Item item = it.next();
			item.y -= 0.4;
			item.life--;
			if (item.life <= 0) {
				it.remove();
			}
		}
	}

	/** Renderiza os textos flutuantes ativos sobre o buffer do jogo. */
	public static void render(Graphics g, int scale) {
		g.setFont(new Font("arial", Font.BOLD, 8));
		for (Item item : items) {
			int alpha = Math.max(0, Math.min(255, item.life * 6));
			g.setColor(new Color(item.color.getRed(), item.color.getGreen(), item.color.getBlue(), alpha));
			g.drawString(item.text, item.x * scale, item.y * scale);
		}
	}

	/** Limpa todos os textos flutuantes (troca de fase/reinício). */
	public static void clear() {
		items.clear();
	}

	private static final class Item {
		final String text;
		int x;
		int y;
		final Color color;
		int life = 45;

		Item(String text, int x, int y, Color color) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.color = color;
		}
	}

	private FloatingText() {
	}
}
