package com.traduvertgames.entities;


import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.World;

public class Enemy extends Entity {

	private double speed = 0.5;
	private int frames = 0, maxFrames = 20, index = 0, maxIndex = 1;
	private BufferedImage[] sprites;
//	private int maskx=8,masky=8,maskw=10,maskh=10;

	public Enemy(int x, int y, int width, int height, BufferedImage sprite) {
		super(x, y, width, height, null);
		sprites = new BufferedImage[2];
		sprites[0] = Game.spritesheet.getSprite(112,16,16,16);
		sprites[1] = Game.spritesheet.getSprite(112+16,16,16,16);
	}

	public void update() {
//		if (Game.rand.nextInt(100) < 50) { // Fazendo os NPCs não se colidirem 1#
		// IA dos mobs seguindo o jogador
		if ((int) x < Game.player.getX() && World.isFree((int) (x + speed), this.getY()) 
				&& !isColliding((int) (x + speed), this.getY())) {
			x += speed;
		} else if ((int) x > Game.player.getX() && World.isFree((int) (x - speed), this.getY())
				&& !isColliding((int) (x - speed), this.getY())) {
			x -= speed;
		}
		if ((int) y < Game.player.getY() && World.isFree(this.getX(), (int) (y + speed))
				&& !isColliding(this.getX(), (int) (y + speed))) {
			y += speed;
		} else if ((int) y > Game.player.getY() && World.isFree(this.getX(), (int) (y - speed))
				&& !isColliding(this.getX(), (int) (y - speed))) {
			y -= speed;
//			}
		}
			frames++;
			if (frames == maxFrames) {
				frames = 0;
				index++;
				if (index > maxIndex) {
					index = 0;
				}
			}
	}

	// Fazer nenhum NPC colidir com o outro
	public boolean isColliding(int xNext, int yNext) {
		Rectangle enemyCurrent = new Rectangle(xNext, yNext,World.TILE_SIZE,World.TILE_SIZE);
		for (int i = 0; i < Game.enemies.size(); i++) {
			Enemy e = Game.enemies.get(i);
			if (e == this)
				continue;
			Rectangle targetEnemy = new Rectangle(e.getX(),e.getY(),World.TILE_SIZE,World.TILE_SIZE);
			if(enemyCurrent.intersects(targetEnemy)) {
				return true;
			}
		}
		
		return false;
	}
	
	//Trocar a máscara quadrada para nao colidir
//	public void render (Graphics g) {
//		super.render(g);
//		g.setColor(Color.blue);
//		g.fillRect(this.getX()+maskx-Camera.x,this.getY()+ masky-Camera.y,maskw,masky);
//	}
	
	public void render(Graphics g) {
		// * Sempre que renderizar, tem que por a posição da Camera
		g.drawImage(sprites[index], this.getX()-Camera.x, this.getY()-Camera.y,null);
	}
}
