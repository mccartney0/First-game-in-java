package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.Tile;
import com.traduvertgames.world.World;

/**
 * Minimapa no canto superior direito da HUD, desenhando paredes, o jogador
 * e os inimigos ao vivo.
 */
public final class MiniMap {

	private static final int MAP_WIDTH = 56;
	private static final int MAP_HEIGHT = 32;
	private static final int TILE_DRAW = 2;

	private static final Color COLOR_FLOOR = new Color(30, 34, 44, 200);
	private static final Color COLOR_WALL = new Color(120, 130, 150, 220);
	private static final Color COLOR_PLAYER = new Color(80, 255, 120);
	private static final Color COLOR_ENEMY = new Color(255, 90, 90);

	private MiniMap() {
	}

	public static void render(Graphics g) {
		if (Game.gameState == null || !Game.gameState.equals("NORMAL")) {
			return;
		}
		if (Game.world == null || World.tiles == null || World.WIDTH <= 0) {
			return;
		}

		int panelX = Game.WIDTH - MAP_WIDTH - 8;
		int panelY = 8;

		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(new Color(8, 12, 20, 200));
		g2.fillRoundRect(panelX - 4, panelY - 4, MAP_WIDTH + 8, MAP_HEIGHT + 8, 8, 8);

		for (int yy = 0; yy < World.HEIGHT; yy++) {
			for (int xx = 0; xx < World.WIDTH; xx++) {
				Tile tile = World.tiles[xx + (yy * World.WIDTH)];
				if (tile instanceof com.traduvertgames.world.WallTile) {
					g2.setColor(COLOR_WALL);
				} else {
					g2.setColor(COLOR_FLOOR);
				}
				g2.fillRect(panelX + xx * TILE_DRAW, panelY + yy * TILE_DRAW, TILE_DRAW, TILE_DRAW);
			}
		}

		for (Enemy enemy : Game.enemies) {
			int mapX = panelX + (enemy.getX() / 16) * TILE_DRAW;
			int mapY = panelY + (enemy.getY() / 16) * TILE_DRAW;
			g2.setColor(COLOR_ENEMY);
			g2.fillRect(mapX, mapY, TILE_DRAW, TILE_DRAW);
		}

		if (Player.life > 0) {
			int mapX = panelX + (Player.life > 0 ? Game.player.getX() / 16 : 0) * TILE_DRAW;
			int mapY = panelY + (Game.player.getY() / 16) * TILE_DRAW;
			g2.setColor(COLOR_PLAYER);
			g2.fillRect(mapX, mapY, TILE_DRAW, TILE_DRAW);
		}
	}
}
