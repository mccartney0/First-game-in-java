package com.traduvertgames.entities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.BulletShoot;
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.World;

public class Player extends Entity {

	public boolean right, up, left, down;
	public int right_dir = 0, left_dir = 1, up_dir = 2, down_dir = 3;
	public int dir = right_dir;
	public double speed = 1.5;
	public double life = 100, maxLife = 100;
	public static double mana = 0, maxMana = 500;
	public boolean damage = false;
//Animando o dano
	private int damageFrames = 0;

	private boolean hasGun = false;

	public boolean shoot = false, mouseShoot = false;
	public int mx, my;

	private int frames = 0, maxFrames = 7, index = 0, maxIndex = 3;
	private boolean moved = false;
	private BufferedImage[] rightPlayer;
	private BufferedImage[] leftPlayer;
	private BufferedImage[] upPlayer;
	private BufferedImage[] downPlayer;

	private BufferedImage playerDamage;
	private BufferedImage gunRight;
	private BufferedImage gunLeft;

	public Player(int x, int y, int width, int height, BufferedImage sprite) {
		super(x, y, width, height, sprite);

		rightPlayer = new BufferedImage[4];
		leftPlayer = new BufferedImage[4];
		upPlayer = new BufferedImage[4];
		downPlayer = new BufferedImage[4];
		playerDamage = Game.spritesheet.getSprite(0, 16, 16, 16);
		gunRight = Game.spritesheet.getSprite(16, 16, 16, 16);
		gunLeft = Game.spritesheet.getSprite(0, 32, 16, 16);

		for (int i = 0; i < 4; i++) {
			rightPlayer[i] = Game.spritesheet.getSprite(32 + (i * 16), 0, 16, 16);
		}
		for (int i = 0; i < 4; i++) {
			leftPlayer[i] = Game.spritesheet.getSprite(32 + (i * 16), 16, 16, 16);
		}
		upPlayer[0] = Game.spritesheet.getSprite(32, 32, 16, 16);
		upPlayer[1] = Game.spritesheet.getSprite(32 + 16, 32, 16, 16);
		upPlayer[2] = Game.spritesheet.getSprite(32, 32, 16, 16);
		upPlayer[3] = Game.spritesheet.getSprite(32 + 16, 32, 16, 16);

		downPlayer[0] = Game.spritesheet.getSprite(64, 32, 16, 16);
		downPlayer[1] = Game.spritesheet.getSprite(64 + 16, 32, 16, 16);
		downPlayer[2] = Game.spritesheet.getSprite(64, 32, 16, 16);
		downPlayer[3] = Game.spritesheet.getSprite(64 + 16, 32, 16, 16);
	}

	public void update() {
		moved = false;
		if (right && World.isFree((int) (x + speed), this.getY())) {
			moved = true;
			dir = right_dir;
			// Mover a câmera - Colocar a camera para se mover com o jogador EX:
			// Camera.x+=speed;
			x += speed;
		} else if (left && World.isFree((int) (x - speed), this.getY())) {
			moved = true;
			dir = left_dir;
			x -= speed;
		}
		if (up && World.isFree(this.getX(), (int) (y - speed))) {
			moved = true;
			dir = up_dir;
			y -= speed;
		} else if (down && World.isFree(this.getX(), (int) (y + speed))) {

			moved = true;
			dir = down_dir;
			y += speed;
		}
		if (moved) {
			frames++;
			if (frames == maxFrames) {
				frames = 0;
				index++;
				if (index > maxIndex) {
					index = 0;
				}
			}

			this.checkCollisionLifePack();
			this.checkCollisionAmmo();
			this.checkCollisionGun();

			if (damage) {
				this.damageFrames++;
				if (this.damageFrames == 8) {// 8 milsegundo
					this.damageFrames = 0;
					damage = false;
				}
			}

			if (shoot) {
				shoot = false;
				if (hasGun && mana > 0) {
					// Criar bala e atirar
					mana--;
					shoot = false;
					int dx = 0;
					int px = 0;
					int py = 8;
					if (dir == right_dir) {
						px = 1;
						dx = 3;

					} else {
						px = -1;
						dx = -3;
					}

					BulletShoot bullet = new BulletShoot(this.getY() + px, this.getX() + py, 3, 3, null, dx, 0);
					Game.bullets.add(bullet);
				}
			}

			if (mouseShoot) {

				mouseShoot = false;

				if (hasGun && mana > 0) {
					mana--;
					// Criar bala e atirar!

					int px = 0, py = 8;
					double angle = 0;
					if (dir == right_dir) {
						px = 8;
						angle = Math.atan2(my - (this.getY() + py - Camera.y), mx - (this.getX() + px - Camera.x));
					} else {
						px = 8;
						angle = Math.atan2(my - (this.getY() + py - Camera.y), mx - (this.getX() + px - Camera.x));
					}

					double dx = Math.cos(angle);
					double dy = Math.sin(angle);

					BulletShoot bullet = new BulletShoot(this.getX() + px, this.getY() + py, 3, 3, null, dx, dy);
					Game.bullets.add(bullet);
				}
			}

			if (life <= 0) {
				Game.entities.clear();
				Game.enemies.clear();
				Game.entities = new ArrayList<Entity>();
				Game.enemies = new ArrayList<Enemy>();
				Game.spritesheet = new Spritesheet("/spritesheet.png");
				// Passando tamanho dele e posições
				Game.player = new Player(0, 0, 16, 16, Game.spritesheet.getSprite(32, 0, 16, 16));
				// Adicionar o jogador na lista e ja aparece na tela
				Game.entities.add(Game.player);
				Game.world = new World("/map.png");
				return;
			}

			// Adicionando a câmera com o Jogador sempre no meio da Tela
			// Renderizando o mapa com método Clamp da Camera
			Camera.x = Camera.clamp(this.getX() - (Game.WIDTH / 2), 0, World.WIDTH * 16 - Game.WIDTH);
			Camera.y = Camera.clamp(this.getY() - (Game.HEIGHT / 2), 0, World.WIDTH * 16 - Game.HEIGHT);
		}
	}

	public void checkCollisionGun() {
		for (int i = 0; i < Game.entities.size(); i++) {
			Entity atual = Game.entities.get(i);
			if (atual instanceof Weapon) {
				if (Entity.isColliding(this, atual)) {
					hasGun = true;
					// Pegou a arma
					Game.entities.remove(atual);
				}
			}
		}
	}

	public void checkCollisionLifePack() {
		for (int i = 0; i < Game.entities.size(); i++) {
			Entity atual = Game.entities.get(i);
			if (atual instanceof LifePack) {
				if (Entity.isColliding(this, atual)) {
					life += 20;
					if (life >= 100)
						life = 100;
					Game.entities.remove(atual);
				}
			}
		}
	}

	public void checkCollisionAmmo() {
		for (int i = 0; i < Game.entities.size(); i++) {
			Entity atual = Game.entities.get(i);
			if (atual instanceof Bullet) {
				if (Entity.isColliding(this, atual)) {
					mana += 100;
					Game.entities.remove(atual);
				}
			}
		}
	}

	public void render(Graphics g) {
		if (!damage) {
			if (dir == right_dir) {
				g.drawImage(rightPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y, null);
				if (hasGun) {
					// Desenhar arma para direita
					g.drawImage(Entity.GUN_RIGHT, this.getX() + 6 - Camera.x, this.getY() - Camera.y, null);
				}
			} else if (dir == left_dir) {
				g.drawImage(leftPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y, null);
				if (hasGun) {
					// Desenhar arma para esquerda
					g.drawImage(Entity.GUN_LEFT, this.getX() - 6 - Camera.x, this.getY() - Camera.y, null);
				}
			}
			if (dir == up_dir) {
				g.drawImage(upPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y, null);
			}
			if (dir == down_dir) {
				g.drawImage(downPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y, null);
			}
		} else {
			g.drawImage(playerDamage, this.getX() - Camera.x, this.getY() - Camera.y, null);
			if (hasGun) {
				if (dir == left_dir) {
					g.drawImage(gunRight, this.getX() - 6 - Camera.x, this.getY() - Camera.y, null);
				} else {
					g.drawImage(gunLeft, this.getX() + 6 - Camera.x, this.getY() - Camera.y, null);
				}
			}
		}
	}
}
