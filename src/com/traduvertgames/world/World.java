package com.traduvertgames.world;

import java.awt.Color;
import com.traduvertgames.dialogue.TraitorNpc;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.traduvertgames.entities.*;
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.dialogue.CommanderNpc;
import com.traduvertgames.dialogue.SupportNpcs;

public class World {

	public static Tile[] tiles;
	public static int WIDTH, HEIGHT;
	public static final int TILE_SIZE = 16;
	
	public World(String path) {
		// Caminhos absolutos (mapas procedurais do modo infinito) são carregados
		// diretamente do disco em vez do classpath.
		if (path != null && path.startsWith("/")) {
			java.io.File f = new java.io.File(path);
			if (f.exists()) {
				loadFromFile(f);
				TeleportPad.linkPairs();
				return;
			}
		}
		try {
			BufferedImage map = ImageIO.read(getClass().getResource(path));
			if (map == null) {
				throw new IOException("Mapa não encontrado no classpath: " + path);
			}
			int[] pixels = new int[map.getWidth() * map.getHeight()];
			WIDTH = map.getWidth();
			HEIGHT = map.getHeight();
			tiles = new Tile[map.getWidth() * map.getHeight()];
			map.getRGB(0, 0, map.getWidth(), map.getHeight(), pixels, 0, map.getWidth());
			applyMapPixels(pixels, map.getWidth(), map.getHeight());
		} catch (IOException e) {
			e.printStackTrace();
		}
		TeleportPad.linkPairs();
	}

	/** Carrega um mapa a partir de um arquivo PNG absoluto (mapas procedurais). */
	private void loadFromFile(java.io.File mapFile) {
		try {
			BufferedImage map = ImageIO.read(mapFile);
			if (map == null) {
				throw new IOException("Mapa não encontrado: " + mapFile.getAbsolutePath());
			}
			int[] pixels = new int[map.getWidth() * map.getHeight()];
			WIDTH = map.getWidth();
			HEIGHT = map.getHeight();
			tiles = new Tile[map.getWidth() * map.getHeight()];
			map.getRGB(0, 0, map.getWidth(), map.getHeight(), pixels, 0, map.getWidth());
			applyMapPixels(pixels, map.getWidth(), map.getHeight());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
		 * Interpreta os pixels do mapa e popula tiles, entidades e inimigos.
		 * Separado do construtor para ser reutilizado por mapas carregados de
		 * arquivos absolutos (fases procedurais do modo infinito).
		 *
		 * Os parâmetros mapWidth/mapHeight são passados explicitamente (em vez
		 * de derivar a altura de pixels.length / mapWidth) para evitar leituras
		 * fora dos limites quando o array de pixels não é múltiplo exato da
		 * largura — caso de mapas PNG com dimensões incompatíveis após merges.
		 */
	private void applyMapPixels(int[] pixels, int mapWidth, int mapHeight) {
		for (int xx = 0; xx < mapWidth; xx++) {
			for (int yy = 0; yy < mapHeight; yy++) {
				int idx = xx + (yy * mapWidth);
				if (idx >= pixels.length) {
					continue;
				}
				int tileIdx = xx + (yy * WIDTH);
				if (tileIdx >= tiles.length || tileIdx < 0) {
					continue;
				}
				int pixelAtual = pixels[idx];
					if (pixelAtual == 0xFF000000) {
						// Floor
			} else if (pixelAtual == 0xFFFFFFFF) {
						// Parede
						tiles[tileIdx] = new WallTile(xx * 16, yy * 16, Tile.TILE_WALL);
					} else if (pixelAtual == 0xFF808080) {
						tiles[tileIdx] = new DestructibleWallTile(xx * 16, yy * 16,
								Tile.TILE_WALL);
					} else if (pixelAtual == 0xFF7CB342) {
						// Grama: terreno rápido (+20% velocidade)
						tiles[tileIdx] = new GrassTile(xx * 16, yy * 16, Tile.TILE_FLOOR);
					} else if (pixelAtual == 0xFF6D4C41) {
						// Lama: terreno lento (-30% velocidade)
						tiles[tileIdx] = new MudTile(xx * 16, yy * 16, Tile.TILE_FLOOR);
					} else if (pixelAtual == 0xFFB0BEC5) {
						// Gelo: terreno escorregadio (inércia)
						tiles[tileIdx] = new IceTile(xx * 16, yy * 16, Tile.TILE_FLOOR);
					} else if (pixelAtual == 0xFF0026FF) {
						// Player
						Game.player.setX(xx * 16);
						Game.player.setY(yy * 16);
					} else if (pixelAtual == 0xFFFF0000
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
// Enemy — pulado se já foi abatido (rodada 25)
Enemy en = Enemy.spawnRandomVariant(xx * 16, yy * 16);
Game.entities.add(en);
Game.enemies.add(en);
} else if (pixelAtual == 0xFF9C27B0
						&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
// Teleporter elite — pulado se já foi abatido (rodada 25)
Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN, Enemy.Variant.TELEPORTER);
Game.entities.add(en);
Game.enemies.add(en);
} else if (pixelAtual == 0xFF00BCD4
						&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
// Artillery elite — pulado se já foi abatido (rodada 25)
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
                                        } else if (pixelAtual == 0xFF00897C) {
                                                // Comandante Ava — NPC interativo (diálogo R)
                                                Game.entities.add(new CommanderNpc(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFF66BB6A) {
                                                // Engenheira Nia — NPC interativo (recarga + mana)
                                                Game.entities.add(SupportNpcs.engineer(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFF5E35B1) {
                                                // Pesquisador Ivo — NPC interativo (mana + dica)
                                                Game.entities.add(SupportNpcs.researcher(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFFFF9800) {
                                                // Armeiro Mercúrio — NPC interativo (arma + vida)
                                                Game.entities.add(SupportNpcs.armorer(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFFFFB74D) {
                                                Game.entities.add(new EngineerNPC(xx * 16, yy * 16));
                                        } else if (pixelAtual == 0xFF7E57C2) {
                                                Game.entities.add(new ResearcherNPC(xx * 16, yy * 16));
					} else if (pixelAtual == 0xFF3F51B5
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
						Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN, Enemy.Variant.WARDEN);
						Game.entities.add(en);
						Game.enemies.add(en);
					} else if (pixelAtual == 0xFF009688
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
						Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN, Enemy.Variant.SENTINEL);
						Game.entities.add(en);
						Game.enemies.add(en);
					} else if (pixelAtual == 0xFFF4511E
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
						Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN, Enemy.Variant.RAVAGER);
						Game.entities.add(en);
						Game.enemies.add(en);
					} else if (pixelAtual == 0xFFE91E63
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, true)) {
						Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
								Enemy.Variant.WARBRINGER, true);
						Game.entities.add(en);
						Game.enemies.add(en);
					} else if (pixelAtual == 0xFF7986CB
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, true)) {
						Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
								Enemy.Variant.OVERSEER, true);
						Game.entities.add(en);
						Game.enemies.add(en);
					} else if (pixelAtual == 0xFF81C784
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
						// Phantom: caçador furtivo que drena escudo e mana
						Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
								Enemy.Variant.PHANTOM);
						Game.entities.add(en);
						Game.enemies.add(en);
						} else if (pixelAtual == 0xFFFF5722
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy,
									QuestManager.getCurrentLevel() == 7)) {
							// Guardian-chefe: o chefe do subsolo da fase 7
							// (boss fixo do mapa — apenas um por fase).
							boolean bossGuardian = QuestManager.getCurrentLevel() == 7;
							Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
									Enemy.Variant.GUARDIAN, bossGuardian);
							Game.entities.add(en);
							Game.enemies.add(en);
						} else if (pixelAtual == 0xFFBF360C
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
							// Guardian comum: tanque robusto que regenera vida,
							// presente nas fases 7/8 como tropa de elite —
							// nunca conta como chefe (rodada 23b).
							Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
									Enemy.Variant.GUARDIAN, false);
							Game.entities.add(en);
							Game.enemies.add(en);
						} else if (pixelAtual == 0xFF74DE80
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
							// Guardian comum (mapas das fases 7/8): nunca conta
							// como chefe (rodada 23b).
							Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
									Enemy.Variant.GUARDIAN, false);
							Game.entities.add(en);
							Game.enemies.add(en);
						} else if (pixelAtual == 0xFFFFC800
							&& !com.traduvertgames.main.EnemyKillTracker.isAlreadyDead(xx, yy, false)) {
							// Tropas de elite procedurais (rodada 24b): variante sólida
							// determinada deterministicamente pela posição, com aura
							// dourada e vida/dano +30% — nunca conta como chefe.
							Enemy.Variant eliteVariant;
							switch ((xx + yy) % 4) {
								case 0:
									eliteVariant = Enemy.Variant.WARDEN;
									break;
								case 1:
									eliteVariant = Enemy.Variant.SENTINEL;
									break;
								case 2:
									eliteVariant = Enemy.Variant.RAVAGER;
									break;
								default:
									eliteVariant = Enemy.Variant.SCOUT;
									break;
							}
							Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
									eliteVariant, false, true);
							Game.entities.add(en);
							Game.enemies.add(en);
						} else if (pixelAtual == 0xFFD01937) {
						// Supervisor-Prime: a mente da colônia, chefe final da campanha (fase 8).
						Enemy en = new Enemy(xx * 16, yy * 16, 16, 16, Entity.ENEMY_EN,
								Enemy.Variant.OVERSEER_PRIME, true);
						Game.entities.add(en);
						Game.enemies.add(en);
					} else if (pixelAtual == 0xFFA1887F) {
						// Técnico Hélio — desertor do subsolo (fase 7)
						Game.entities.add(new TraitorNpc(xx * 16, yy * 16));
					} else if (pixelAtual == 0xFF673AB7) {
						Game.entities.add(new TeleportPad(xx * 16, yy * 16));
					}
					// Floor: pixels sem caso específico (spawns de entidades, bordas
					// decorativas) viram chão caminhável — evita tiles null e
					// NullPointerException no render ao avançar de fase
					if (tiles[tileIdx] == null) {
						tiles[tileIdx] = new FloorTile(xx * 16, yy * 16, Tile.TILE_FLOOR);
					}
				}
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
	
        /** Reinicia o jogo a partir de um mapa PNG em caminho absoluto (modo infinito). */
        public static void restartGameFromFile(String absolutePath) {
                restartGameCommon(9, absolutePath);
        }

        public static void restartGame(String level) {
                restartGameCommon(parseLevelNumber(level), "/" + level);
        }

        /** Núcleo comum de reinício: limpa entidades e carrega o mapa informado. */
        private static void restartGameCommon(int levelNumber, String mapSource) {
                TeleportPad.reset();
                Game.entities.clear();
                Game.enemies.clear();
                Game.entities = new ArrayList<Entity>();
                Game.enemies = new ArrayList<Enemy>();
                Game.bullet = new ArrayList<Bullet>();
                Game.bullets = new ArrayList<BulletShoot>();
                // Nova fase (inclui ciclos procedurais): zera kills, combo da fase e o timer.
                Game.resetLevelStats();
                Game.spritesheet = new Spritesheet("/spritesheet.png");
                // Passando tamanho dele e posições
                Game.player = new Player(0, 0, 16, 16, Game.spritesheet.getSprite(32, 0, 16, 16));
                // Adicionar o jogador na lista e ja aparece na tela
                Game.entities.add(Game.player);
			// Sem prepareForLevel o nível da campanha ficava desatualizado ao
			// trocar de fase pelo painel tático (bug reportado: "matei tudo e
			// não avança").
			QuestManager.prepareForLevel(levelNumber);
			Game.world = new World(mapSource);
			QuestManager.onLevelLoaded();
			// Garante o chefe da fase: níveis a partir do 2 têm a missão de
			// neutralizar o comandante; se o mapa não tiver um boss fixo,
			// um WARBRINGER é posicionado em um local válido distante do spawn.
			ensurePhaseBoss(levelNumber);
			// Narrativa: os NPCs da campanha ficam em pontos temáticos do mapa
			// (centro de comando, esconderijo técnico, laboratório, forja), em
			// vez de sempre no canto superior esquerdo da fase.
			com.traduvertgames.quest.StoryManager.placeStoryNpcs();
			return;
	}

        private static boolean mapHasBoss() {
                for (Entity e : Game.entities) {
                        if (e instanceof com.traduvertgames.entities.Enemy && ((com.traduvertgames.entities.Enemy) e).isBoss()) {
                                return true;
                        }
                }
                return false;
        }

        private static void ensurePhaseBoss(int levelNumber) {
                // Fases 7 (Subsolo) e 8 (Núcleo Central) já carregam o chefe
                // fixo diretamente do mapa (GUARDIAN e OVERSEER-PRIME).
                if (levelNumber == 7 || levelNumber == 8 || levelNumber < 2 || mapHasBoss()) {
                        return;
                }
                int playerX = (int) (Game.player != null ? Game.player.getX() : 0);
                int playerY = (int) (Game.player != null ? Game.player.getY() : 0);
                int tries = 0;
                while (tries < 400) {
                        int tx = Game.rand.nextInt(WIDTH);
                        int ty = Game.rand.nextInt(HEIGHT);
                        if (!isValidTile(tx, ty)) {
                                tries++;
                                continue;
                        }
                        int fx = tx * 16;
                        int fy = ty * 16;
                        double dx = fx - playerX;
                        double dy = fy - playerY;
                        if (dx * dx + dy * dy < 200 * 200) {
                                tries++;
                                continue;
                        }
                        Enemy boss = new Enemy(fx, fy, 16, 16, Entity.ENEMY_EN,
                                        levelNumber == 6 ? Enemy.Variant.OVERSEER : Enemy.Variant.WARBRINGER, true);
                        Game.entities.add(boss);
                        Game.enemies.add(boss);
                        return;
                }
        }

        private static int parseLevelNumber(String level) {
                if (level == null) {
                        return QuestManager.getCurrentLevel();
                }
                // A arena de treino do onboarding não é uma fase real: não ganha
                // chefe de fase nem o escalonamento de dificuldade de campanha.
                if (level.startsWith("training")) {
                        return 0;
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
				if (tile == null) {
					continue;
				}
				tile.render(g);
			}
		}
	}
}
