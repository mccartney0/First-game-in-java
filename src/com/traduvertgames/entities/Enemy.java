package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.AStar;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.Node;
import com.traduvertgames.world.Vector2i;
import com.traduvertgames.world.World;

public class Enemy extends Entity {

        private int frames = 0, maxFrames = 20, index = 0, maxIndex = 1;
	private BufferedImage[] sprites;
	public static int enemies = 0;

	private int life = 2;

	private boolean isDamaged = false;
	private int damageFrames = 10, damageCurrent = 0;
//	private int maskx=8,masky=8,maskw=10,maskh=10;
	public static boolean addEnemy;

	public Enemy(int x, int y, int width, int height, BufferedImage sprite) {
		super(x, y, width, height, null);
		sprites = new BufferedImage[2];
		sprites[0] = Game.spritesheet.getSprite(112, 16, 16, 16);
		sprites[1] = Game.spritesheet.getSprite(112 + 16, 16, 16, 16);
	}

        public void update() {

                depth = 0;
                // Ajustando a mascara de colisao.
                maskx = 8;
                masky = 8;
                mwidth = 8;
                mheight = 8;

                double distanceToPlayer = this.calculateDistance(this.getX(), this.getY(), Game.player.getX(),
                                Game.player.getY());

                if (distanceToPlayer < 120) {
                        if (!isCollidingWithPlayer()) {
                                if (path == null || path.isEmpty() || Game.rand.nextInt(100) < 5) {
                                        Vector2i startNode = new Vector2i((int) (x / 16), (int) (y / 16));
                                        Vector2i endNode = new Vector2i((int) (Game.player.x / 16), (int) (Game.player.y / 16));
                                        path = AStar.findPath(Game.world, startNode, endNode);
                                }

                                if (path != null && !path.isEmpty()) {
                                        followPath(path);
                                }
                        } else {
                                if (Game.rand.nextInt(100) < 10) {
                                        Game.player.life -= Game.rand.nextInt(3);
                                        Game.player.damage = true;
                                }
                        }
                }

                frames++;
                if (frames == maxFrames) {
                        frames = 0;
                        index++;
                        if (index > maxIndex) {
                                index = 0;
                        }
                }

                collidingBullet();
                if (life <= 0) {
                        destroySelf();
                        enemies += 1;

                        if (Game.enemies.size() == 0) {
                                addEnemy = true;
                        }
                        return;
                }

                if (Enemy.addEnemy) {
                        Enemy en = new Enemy(1 * 16, 1 * 16, 16, 16, Entity.ENEMY_EN);
                        Game.entities.add(en);
                        Game.enemies.add(en);
                        Enemy.addEnemy = false;
                }

                if (isDamaged) {
                        this.damageCurrent++;
                        if (this.damageCurrent == this.damageFrames) {
                                this.damageCurrent = 0;
                                this.isDamaged = false;
                        }
                }

        }
	public void destroySelf() {
		Game.enemies.remove(this);
		Game.entities.remove(this);
	}

	// Tirando dano com tiro
	public void collidingBullet() {
		for (int i = 0; i < Game.bullets.size(); i++) {
			Entity e = Game.bullets.get(i);
			if (Entity.isColliding(this, e)) {
				isDamaged = true;
				life--;
				Game.bullets.remove(i);
				return;
			}
		}

	}

	// Fazer nenhum NPC colidir com o outro
//			public boolean isColliding(int xNext, int yNext) {
//				Rectangle enemyCurrent = new Rectangle(xNext+ maskx, yNext + masky,mwidth,mheight);
//				for (int i = 0; i < Game.enemies.size(); i++) {
//					Enemy e = Game.enemies.get(i);
//					if (e == this)
//						continue;
//					Rectangle targetEnemy = new Rectangle(e.getX()+ maskx, e.getY()+ masky, mwidth,mheight);
//					if (enemyCurrent.intersects(targetEnemy)) {
//						return true;
//					}
//				}
//
//				return false;
//			}

	public boolean isCollidingWithPlayer() {
		int enemyX = this.getX() + maskx;
		int enemyY = this.getY() + masky;
		int playerX = Game.player.getX();
		int playerY = Game.player.getY();

		return enemyX < playerX + 16 && enemyX + mwidth > playerX && enemyY < playerY + 16 && enemyY + mheight > playerY;
	}

	// Trocar a máscara quadrada para nao colidir
//	Testar mascara de colisão
//	public void render (Graphics g) {
//		super.render(g);
//		g.setColor(Color.blue);
//		g.fillRect(this.getX()+maskx-Camera.x,this.getY()+ masky-Camera.y,maskw,masky);
//	}

	public void render(Graphics g) {
		if (!isDamaged) {
			// * Sempre que renderizar, tem que por a posição da Camera
			g.drawImage(sprites[index], this.getX() + 4 - Camera.x, this.getY() + 4 - Camera.y, null);
		} else {
			g.drawImage(Entity.ENEMY_FEEDBACK, this.getX() + 4 - Camera.x, this.getY() + 4 - Camera.y, null);
		}
	}
}
