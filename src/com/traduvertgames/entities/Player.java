package com.traduvertgames.entities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.World;

public class Player extends Entity {

	public boolean right, up, left, down;
	public int right_dir = 0, left_dir = 1, up_dir = 2, down_dir = 3;
	public int dir = right_dir;
	public double speed = 1.5;
	public static double life = 100,maxLife=100;

	private int frames = 0, maxFrames = 7, index = 0, maxIndex = 3;
	private boolean moved = false;
	private BufferedImage[] rightPlayer;
	private BufferedImage[] leftPlayer;
	private BufferedImage[] upPlayer;
	private BufferedImage[] downPlayer;

	public Player(int x, int y, int width, int height, BufferedImage sprite) {
		super(x, y, width, height, sprite);

		rightPlayer = new BufferedImage[4];
		leftPlayer = new BufferedImage[4];
		upPlayer = new BufferedImage[4];
		downPlayer = new BufferedImage[4];

		for (int i = 0; i < 4; i++) {
			rightPlayer[i] = Game.spritesheet.getSprite(32 + (i * 16), 0, 16, 16);
		}
		for (int i = 0; i < 4; i++) {
			leftPlayer[i] = Game.spritesheet.getSprite(32 + (i * 16), 16, 16, 16);
		}
//		for (int i = 1; i < 4; i++) {
//			upPlayer[i] = Game.spritesheet.getSprite(32 + (i * 16), 32, 16, 16);
//		}
//		for (int i = 1; i < 4; i++) {
//			downPlayer[i] = Game.spritesheet.getSprite(64 + (i * 16), 32, 16, 16);
//		}
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
			dir = down_dir;
			moved = true;
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
			// Adicionando a câmera com o Jogador sempre no meio da Tela
			// Renderizando o mapa com método Clamp da Camera
			Camera.x = Camera.clamp(this.getX() - (Game.WIDTH / 2), 0, World.WIDTH * 16 - Game.WIDTH);
			Camera.y = Camera.clamp(this.getY() - (Game.HEIGHT / 2), 0, World.WIDTH * 16 - Game.HEIGHT);
		}
	}
//public boolean isCollinding() {
//	
//}

	public void render(Graphics g) {
		if (dir == right_dir) {
			g.drawImage(rightPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y, null);
		} else if (dir == left_dir) {
			g.drawImage(leftPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y, null);
		}
		if (dir == up_dir) {
			g.drawImage(upPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y, null);
		}
		if (dir == down_dir) {
			g.drawImage(downPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y, null);

		}
	}
}
