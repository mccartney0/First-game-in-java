package com.traduvertgames.entities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.traduvertgames.main.Game;

public class Player extends Entity {

	public boolean right, up, left, down;
	public int right_dir = 0, left_dir = 1;
	public int dir = right_dir;
	public double speed = 1.5;

	private int frames = 0, maxFrames = 5, index = 0, maxIndex = 3;
	private boolean moved = false;
	private BufferedImage[] rightPlayer;
	private BufferedImage[] leftPlayer;

	public Player(int x, int y, int width, int height, BufferedImage sprite) {
		super(x, y, width, height, sprite);

		rightPlayer = new BufferedImage[4];
		leftPlayer = new BufferedImage[4];

		for (int i = 0; i < 4; i++) {
			rightPlayer[i] = Game.spritesheet.getSprite(32 + (i * 16), 0, 16, 16);
		}
		for (int i = 0; i < 4; i++) {
			leftPlayer[i] = Game.spritesheet.getSprite(32 + (i * 16), 16, 16, 16);
		}

	}

	public void update() {
		moved = false;
		if (right) {
			moved = true;
			dir = right_dir;
			x += speed;
		} else if (left) {
			moved = true;
			dir = left_dir;
			x -= speed;
		}
		if (up) {
			moved = true;
			y -= speed;
		} else if (down) {
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
		}
	}

	public void render(Graphics g) {
		if (dir == right_dir) {
			g.drawImage(rightPlayer[index], this.getX(), this.getY(), null);
		} else if (dir == left_dir) {
			g.drawImage(leftPlayer[index], this.getX(), this.getY(), null);
		}

	}
}
