package com.traduvertgames.world;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/** Terreno de grama: concede +20% de velocidade de movimento. */
public class GrassTile extends FloorTile {
	public GrassTile(int x, int y, BufferedImage sprite) {
		super(x, y, sprite);
	}
	@Override
	public void render(Graphics g) {
		super.render(g);
		// Sobreposição verde translúcida para diferenciar da grama escura do fundo.
		g.setColor(new Color(124, 179, 66, 60));
		g.fillRect(getX() - Camera.x, getY() - Camera.y, World.TILE_SIZE, World.TILE_SIZE);
	}
}
