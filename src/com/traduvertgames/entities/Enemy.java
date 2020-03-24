package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.World;

public class Enemy extends Entity {

	private double speed = 0.6;
	//Máscara do inimigo
	private int maskx=8,masky=8,maskw=5,maskh=5;

	public Enemy(int x, int y, int width, int height, BufferedImage sprite) {
		super(x, y, width, height, sprite);

	}

	public void update() {
//		if (Game.rand.nextInt(100) < 50) { // Fazendo os NPCs não se colidirem 1#
		// IA dos mobs seguindo o jogador
		if ((int) x < Game.player.getX() && World.isFree((int) (x + speed), this.getY()) 
				&& !isColiding((int) (x + speed), this.getY())) {
			x += speed;
		} else if ((int) x > Game.player.getX() && World.isFree((int) (x - speed), this.getY())
				&& !isColiding((int) (x - speed), this.getY())) {
			x -= speed;
		}
		if ((int) y < Game.player.getY() && World.isFree(this.getX(), (int) (y + speed))
				&& !isColiding(this.getX(), (int) (y + speed))) {
			y += speed;
		} else if ((int) y > Game.player.getY() && World.isFree(this.getX(), (int) (y - speed))
				&& !isColiding(this.getX(), (int) (y - speed))) {
			y -= speed;
//			}
		}
	}

	// Fazer nenhum NPC colidir com o outro
	public boolean isColiding(int xNext, int yNext) {
		Rectangle enemyCurrent = new Rectangle(xNext+maskx, yNext+maskw,maskw,maskh);
		for (int i = 0; i < Game.enemies.size(); i++) {
			Enemy e = Game.enemies.get(i);
			if (e == this)
				continue;
			Rectangle targetEnemy = new Rectangle(e.getX()+maskx,e.getY()+maskw,maskw,maskh);
			if(enemyCurrent.intersects(targetEnemy)) {
				return true;
			}
		}
		
		return false;
	}
	
	//Trocar a máscara quadrada para nao colidir
	public void render (Graphics g) {
		super.render(g);
		g.setColor(Color.blue);
		g.fillRect(this.getX()+maskx-Camera.x,this.getY()+ masky-Camera.y,maskw,maskh);
	}
}
