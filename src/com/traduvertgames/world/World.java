package com.traduvertgames.world;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.traduvertgames.entities.*;
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;

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
                                        } else if (pixelAtual == 0xFF808080) {
                                                tiles[xx + (yy * map.getWidth())] = new DestructibleWallTile(xx * 16, yy * 16,
                                                                Tile.TILE_WALL);
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
                                        } else if (pixelAtual == 0xFFFF5252) {
                                                Game.entities.add(new NanoMedkit(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFF00E5FF) {
                                                Game.entities.add(new OverclockModule(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFFFFC107) {
                                                // Quest item
                                                Game.entities.add(new QuestItem(xx * 16, yy * 16, new Color(255, 193, 7)));
                                        } else if (pixelAtual == 0xFF00ACC1) {
                                                Game.entities.add(new DataCore(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFF4CAF50) {
                                                // Quest beacon
                                                Game.entities.add(new QuestBeacon(xx * 16, yy * 16, new Color(76, 175, 80)));
                                        } else if (pixelAtual == 0xFF795548) {
                                                // Quest NPC
                                                Game.entities.add(new QuestNPC(xx * 16, yy * 16, new Color(121, 85, 72)));
                                        } else if (pixelAtual == 0xFFFFB74D) {
                                                Game.entities.add(new EngineerNPC(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFF7E57C2) {
                                                Game.entities.add(new ResearcherNPC(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFF3F51B5) {
                                                Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN, Enemy.Variant.WARDEN);
                                                Game.entities.add(en);
                                                Game.enemies.add(en);
                                        } else if (pixelAtual == 0xFF009688) {
                                                Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN, Enemy.Variant.SENTINEL);
                                                Game.entities.add(en);
                                                Game.enemies.add(en);
                                        } else if (pixelAtual == 0xFFF4511E) {
                                                Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN, Enemy.Variant.RAVAGER);
                                                Game.entities.add(en);
                                                Game.enemies.add(en);
                                        } else if (pixelAtual == 0xFFE91E63) {
                                                Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
                                                                Enemy.Variant.WARBRINGER, true);
                                                Game.entities.add(en);
                                                Game.enemies.add(en);
                                        } else if (pixelAtual == 0xFF7986CB) {
                                                Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
                                                                Enemy.Variant.OVERSEER, true);
                                                Game.entities.add(en);
                                                Game.enemies.add(en);
                                        }
                                        // Floor
                                }
                        }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean isFree(int xNext,int yNext, int zplayer) {
		final int margin = 1;

		int adjustedX1 = xNext + margin;
		int adjustedY1 = yNext + margin;

		int adjustedX2 = xNext + TILE_SIZE - 1 - margin;
		int adjustedY2 = yNext + margin;

		int adjustedX3 = xNext + margin;
		int adjustedY3 = yNext + TILE_SIZE - 1 - margin;

		int adjustedX4 = xNext + TILE_SIZE - 1 - margin;
		int adjustedY4 = yNext + TILE_SIZE - 1 - margin;

		try {
			int x1 = adjustedX1 / TILE_SIZE;
			int y1 = adjustedY1 / TILE_SIZE;

			int x2 = adjustedX2 / TILE_SIZE;
			int y2 = adjustedY2 / TILE_SIZE;

			int x3 = adjustedX3 / TILE_SIZE;
			int y3 = adjustedY3 / TILE_SIZE;

			int x4 = adjustedX4 / TILE_SIZE;
			int y4 = adjustedY4 / TILE_SIZE;

			boolean hitsWall = (tiles[x1 + (y1 * World.WIDTH)] instanceof WallTile)
						|| (tiles[x2 + (y2 * World.WIDTH)] instanceof WallTile)
						|| (tiles[x3 + (y3 * World.WIDTH)] instanceof WallTile)
						|| (tiles[x4 + (y4 * World.WIDTH)] instanceof WallTile);

			return !hitsWall;
		} catch (ArrayIndexOutOfBoundsException ex) {
			return false;
		}
	}
	
        public static void restartGame(String level) {
                int levelNumber = parseLevelNumber(level);
                QuestManager.prepareForLevel(levelNumber);
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
                QuestManager.onLevelLoaded();
                return;
        }

        private static int parseLevelNumber(String level) {
                if (level == null) {
                        return QuestManager.getCurrentLevel();
                }
                int value = 0;
                boolean foundDigit = false;
                for (int i = 0; i < level.length(); i++) {
                        char c = level.charAt(i);
                        if (Character.isDigit(c)) {
                                foundDigit = true;
                                value = value * 10 + Character.digit(c, 10);
                        } else if (foundDigit) {
                                break;
                        }
                }
                if (foundDigit) {
                        return value;
                }
                return QuestManager.getCurrentLevel();
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

        public static boolean damageDestructibleWall(int tileX, int tileY, double damage) {
                if (!isValidTile(tileX, tileY)) {
                        return false;
                }
                Tile tile = tiles[tileX + (tileY * WIDTH)];
                if (tile instanceof DestructibleWallTile) {
                        DestructibleWallTile destructible = (DestructibleWallTile) tile;
                        boolean destroyed = destructible.applyDamage(damage);
                        if (destroyed) {
                                tiles[tileX + (tileY * WIDTH)] = new FloorTile(tileX * TILE_SIZE, tileY * TILE_SIZE,
                                                Tile.TILE_FLOOR);
                        }
                        return true;
                }
                return false;
        }

        public static boolean damageDestructibleWallByPixel(int pixelX, int pixelY, double damage) {
                int tileX = pixelX / TILE_SIZE;
                int tileY = pixelY / TILE_SIZE;
                return damageDestructibleWall(tileX, tileY, damage);
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
