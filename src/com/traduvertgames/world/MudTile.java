package com.traduvertgames.world;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/** Terreno de lama: reduz 30% da velocidade de movimento. */
public class MudTile extends FloorTile {
	public MudTile(int x, int y, BufferedImage sprite) {
		super(x, y, sprite);
	}
	@Override
	public void render(Graphics g) {
		super.render(g);
		g.setColor(new Color(109, 76, 65, 80));
		g.fillRect(getX() - Camera.x, getY() - Camera.y, World.TILE_SIZE, World.TILE_SIZE);
	}
}
