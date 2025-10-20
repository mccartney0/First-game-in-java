package com.traduvertgames.world;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.traduvertgames.entities.*;
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.main.Game;

public class World {

	public static Tile[] tiles;
	public static int WIDTH, HEIGHT;
	public static final int TILE_SIZE = 16;
	
	public World(String path) {
		try {
			BufferedImage map = ImageIO.read(getClass().getResource(path));
			int[] pixels = new int[map.getWidth() * map.getHeight()];
			WIDTH = map.getWidth();
			HEIGHT = map.getHeight();
			tiles = new Tile[map.getWidth() * map.getHeight()];
			map.getRGB(0, 0, map.getWidth(), map.getHeight(), pixels, 0, map.getWidth());
			for (int xx = 0; xx < map.getWidth(); xx++) {
				for (int yy = 0; yy < map.getHeight(); yy++) {
					int pixelAtual = pixels[xx + (yy * map.getWidth())];
					tiles[xx + (yy * WIDTH)] = new FloorTile(xx * 16, yy * 16, Tile.TILE_FLOOR);
					if (pixelAtual == 0xFF000000) {
						// Floor
						tiles[xx + (yy * WIDTH)] = new FloorTile(xx * 16, yy * 16, Tile.TILE_FLOOR);
					} else if (pixelAtual == 0xFFFFFFFF) {
						// Parede
						tiles[xx + (yy * map.getWidth())] = new WallTile(xx * 16, yy * 16, Tile.TILE_WALL);
					} else if (pixelAtual == 0xFF0026FF) {
						// Player
						Game.player.setX(xx * 16);
						Game.player.setY(yy * 16);
} else if (pixelAtual == 0xFFFF0000) {
// Enemy
Enemy en = Enemy.spawnRandomVariant(xx * 16, yy * 16);
Game.entities.add(en);
Game.enemies.add(en);
} else if (pixelAtual == 0xFF9C27B0) {
// Teleporter elite
Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN, Enemy.Variant.TELEPORTER);
Game.entities.add(en);
Game.enemies.add(en);
} else if (pixelAtual == 0xFF00BCD4) {
// Artillery elite
Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN, Enemy.Variant.ARTILLERY);
Game.entities.add(en);
Game.enemies.add(en);
					} else if (pixelAtual == 0xFFFF6A00) {
						// Weapon
						Game.entities.add(new Weapon(xx * 16, yy * 16, 16, 16, Entity.WEAPON_EN));
					} else if (pixelAtual == 0xFF4CFF00) {
						// Life Pack
						LifePack pack = new LifePack(xx * 16, yy * 16, 16, 16, Entity.LIFEPACK_EN);
						pack.setMask(4, 4,8, 8);
						Game.entities.add(pack);
//						Game.entities.add(new LifePack(xx * 16, yy * 16, 16, 16, Entity.LIFEPACK_EN));
                                        } else if (pixelAtual == 0xFFFFD800) {
                                                // Bullet
                                                Game.entities.add(new Bullet(xx * 16, yy * 16, 16, 16, Entity.BULLET_EN));
                                        } else if (pixelAtual == 0xFF8E24AA) {
                                                // Shield orb
                                                Game.entities.add(new ShieldOrb(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFF1DE9B6) {
                                                // Energy cell
                                                Game.entities.add(new EnergyCell(xx * 16, yy * 16));
                                        }
                                        // Floor
                                }
                        }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean isFree(int xNext,int yNext, int zplayer) {
		int x1 = xNext / TILE_SIZE;
		int y1 = yNext / TILE_SIZE;
		
		int x2 = (xNext+TILE_SIZE-1) / TILE_SIZE;
		int y2 = yNext / TILE_SIZE;
		
		int x3 = xNext / TILE_SIZE;
		int y3 = (yNext+TILE_SIZE-1) / TILE_SIZE;
		
		int x4 = (xNext+TILE_SIZE-1) / TILE_SIZE;
		int y4 = (yNext+TILE_SIZE-1) / TILE_SIZE;
		try {
			if( !((tiles[x1+(y1*World.WIDTH)] instanceof WallTile) ||
					(tiles[x2+(y2*World.WIDTH)] instanceof WallTile) || 
					(tiles[x3+(y3*World.WIDTH)] instanceof WallTile) || 
					(tiles[x4+(y4*World.WIDTH)] instanceof WallTile))) {
				return true;
			}
		}catch(ArrayIndexOutOfBoundsException ex) {
//			System.out.println("Saiu do mapa");
		}
		
		
//		Posibilita pular pelas paredes
//		if(zplayer>0) {
//			return true;
//		}
		return false;
	}
	
        public static void restartGame(String level) {
                Game.entities.clear();
                Game.enemies.clear();
                Game.entities = new ArrayList<Entity>();
                Game.enemies = new ArrayList<Enemy>();
                Game.bullet = new ArrayList<Bullet>();
                Game.bullets = new ArrayList<BulletShoot>();
                Game.spritesheet = new Spritesheet("/spritesheet.png");
                // Passando tamanho dele e posições
                Game.player = new Player(0, 0, 16, 16, Game.spritesheet.getSprite(32, 0, 16, 16));
                // Adicionar o jogador na lista e ja aparece na tela
                Game.entities.add(Game.player);
                Game.world = new World("/"+level);
                return;
        }

        public static boolean isValidTile(int tileX, int tileY) {
                return tileX >= 0 && tileY >= 0 && tileX < WIDTH && tileY < HEIGHT;
        }

        public static boolean isWallTile(int tileX, int tileY) {
                if (!isValidTile(tileX, tileY)) {
                        return true;
                }
                return tiles[tileX + (tileY * WIDTH)] instanceof WallTile;
        }

        public static boolean isWallByPixel(int pixelX, int pixelY) {
                int tileX = pixelX / TILE_SIZE;
                int tileY = pixelY / TILE_SIZE;
                return isWallTile(tileX, tileY);
        }
        public void render(Graphics g) {

                //Otimizando e renderizando o mapa apenas para onde a Câmera pega
                int xstart = Camera.x / 16;
                int ystart = Camera.y / 16;

		int xfinal = xstart + (Game.WIDTH / 16) ;
		int yfinal = ystart + (Game.HEIGHT / 16) ;
		for (int xx = xstart; xx <= xfinal; xx++) {
			for (int yy = ystart; yy <= yfinal; yy++) {
				if(xx<0 || yy < 0 || xx >= WIDTH || yy>= HEIGHT)
					continue;
				Tile tile = tiles[xx + (yy * WIDTH)];
				tile.render(g);
			}
		}
	}
}
