package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;

public class BulletShoot extends Entity {

	private double dx;
	private double dy;
	private double spd = 4;

	private int life = 30, curLife = 0;

	public BulletShoot(int x, int y, int width, int height, BufferedImage sprite, double dx, double dy) {
		super(x, y, width, height, sprite);
		this.dx = dx;
		this.dy = dy;
	}

	public void update() {
		x += dx * spd;
		y += dy * spd;
		curLife++;
		//Remover a bala em 3 segundos dps de disparada
		if (curLife == life) {
			// Sempre damos um return quando removemos o próprio objeto
			Game.bullets.remove(this);
			return;
		}
	}

	public void render(Graphics g) {
		g.setColor(Color.YELLOW);
		g.fillOval(this.getX() - Camera.x, this.getY() - Camera.y, width, height);
	}

}
