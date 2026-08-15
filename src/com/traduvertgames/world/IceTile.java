package com.traduvertgames.world;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/** Terreno de gelo: escorregadio — o movimento ganha inércia. */
public class IceTile extends FloorTile {
	public IceTile(int x, int y, BufferedImage sprite) {
		super(x, y, sprite);
	}
	@Override
	public void render(Graphics g) {
		super.render(g);
		g.setColor(new Color(176, 190, 197, 90));
		g.fillRect(getX() - Camera.x, getY() - Camera.y, World.TILE_SIZE, World.TILE_SIZE);
		g.setColor(new Color(255, 255, 255, 70));
		g.drawRect(getX() - Camera.x + 2, getY() - Camera.y + 2, World.TILE_SIZE - 5, World.TILE_SIZE - 5);
	}
}
