package com.traduvertgames.world;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class World {

	private Tile[] tiles;
	public static int WIDHT, HEIGHT;
	public World(String path) {
		try {
			BufferedImage map = ImageIO.read(getClass().getResource(path));
			int[] pixels = new int[map.getWidth() * map.getHeight()];
			WIDHT = map.getWidth();
			HEIGHT = map.getHeight();
			tiles = new Tile[map.getWidth() * map.getHeight()];
			map.getRGB(0, 0, map.getWidth(), map.getHeight(), pixels, 0, map.getWidth());
			for(int xx=0; xx< map.getWidth();xx++) {
				for(int yy =0;yy<map.getHeight();yy++) {
					int pixelAtual = pixels[xx+(yy*map.getWidth())];
					if(pixelAtual == 0xFF000000) {
						//Floor
						tiles[xx+(yy*WIDHT)] = new FloorTile(xx*16,yy*16,Tile.TILE_FLOOR);
					}else if(pixelAtual == 0xFFFFFFFF) {
						//Wall
						tiles[xx+(yy*WIDHT)] = new FloorTile(xx*16,yy*16,Tile.TILE_WALL);
					}else if(pixelAtual == 0xFF0026FF) {
						//Player
						tiles[xx+(yy*WIDHT)] = new FloorTile(xx*16,yy*16,Tile.TILE_FLOOR);
					}else {
						tiles[xx+(yy*WIDHT)] = new FloorTile(xx*16,yy*16,Tile.TILE_FLOOR);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void render(Graphics g) {
		for(int xx =0;xx<WIDHT;xx++) {
			for(int yy=0;yy<HEIGHT;yy++) {
				Tile tile = tiles[xx+(yy*WIDHT)];
				tile.render(g);
			}
		}
	}
}
