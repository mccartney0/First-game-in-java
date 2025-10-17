package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;

import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.Node;
import com.traduvertgames.world.Vector2i;
import com.traduvertgames.world.World;

public class Entity {

	public static BufferedImage LIFEPACK_EN = Game.spritesheet.getSprite(6 * 16, 0, 16, 16);
	public static BufferedImage WEAPON_EN = Game.spritesheet.getSprite(7 * 16, 0, 16, 16);
	public static BufferedImage BULLET_EN = Game.spritesheet.getSprite(6 * 16, 16, 16, 16);
	public static BufferedImage ENEMY_EN = Game.spritesheet.getSprite(7 * 16, 16, 16, 16);
	public static BufferedImage ENEMY_FEEDBACK = Game.spritesheet.getSprite(144, 16, 16, 16);
	public static BufferedImage GUN_LEFT = Game.spritesheet.getSprite(128, 0, 16, 16);
	public static BufferedImage GUN_RIGHT = Game.spritesheet.getSprite(128 + 16, 0, 16, 16);

	protected double x;
	protected double y;
	protected int width;
	protected int height;
	protected int z;
	public int depth;

	protected List<Node> path;

	private BufferedImage sprite;

	public int maskx, masky, mwidth, mheight;

	public Entity(int x, int y, int width, int height, BufferedImage sprite) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.sprite = sprite;

		this.maskx = 0;
		this.masky = 0;
		this.mwidth = width;
		this.mheight = height;
	}

	public void setMask(int maskx, int masky, int mwidth, int mheight) {
		this.maskx = maskx;
		this.masky = masky;
		this.mwidth = mwidth;
		this.mheight = mheight;
	}

	public static Comparator<Entity> nodeSorter = new Comparator<Entity>(){

		@Override
		public int compare(Entity n0, Entity n1)
		{
			if(n1.depth < n0.depth)
				return + 1;

			if(n1.depth > n0.depth)
				return - 1;

			return 0;
		}
	};
	
	public void setX(int newX) {
		this.x = newX;
	}

	public void setY(int newY) {
		this.y = newY;
	}

	public int getX() {
		return (int) x;
	}

	public int getY() {
		return (int) y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void update() {
	}

	// Calculando a distancia entre dois pontos do ponto 1 para o ponto 2 e
	// perseguir 2D
	public double calculateDistance(int x1, int y1, int x2, int y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

//	 Seguindo inimigo precisão
	public void followPath(List<Node> path)
//	public void followPath(List<Node> path, int speed) // metÃ³do para utilizar speed do enemy
	{
		if(path != null)
		{
			if(path.size() > 0)
			{
				Vector2i target = path.get(path.size() - 1).tile;
//				xPrev = x;
//				yPrev = y;

				if(x < target.x * 16)
				{
					x++;

					// metÃ³do para utilizar speed do enemy
//					x += speed;
//					if(target.x * 16 < this.x)
//					{
//						this.x = target.x * 16;
//					}
				}
				else if(x > target.x * 16)
				{
					x--;

					// metÃ³do para utilizar speed do enemy
//					x -= speed;
//					if(target.x * 16 > this.x)
//					{
//						this.x = target.x * 16;
//					}
				}

				if(y < target.y * 16)
				{
					y++;

					// metÃ³do para utilizar speed do enemy
//					y += speed;
//					if(target.y * 16 < this.y)
//					{
//						this.y = target.y * 16;
//					}
				}
				else if(y > target.y * 16)
				{
					y--;

		int e1x = e1.getX() + e1.maskx;
		int e1y = e1.getY() + e1.masky;
		int e2x = e2.getX() + e2.maskx;
		int e2y = e2.getY() + e2.masky;

		return e1x < e2x + e2.mwidth && e1x + e1.mwidth > e2x && e1y < e2y + e2.mheight
				&& e1y + e1.mheight > e2y;
//					{
//						this.y = target.y * 16;
//					}
				}

				if((x == target.x * 16) && (y == target.y * 16))
				{
					path.remove(path.size() - 1);
				}
			}
		}
	}

	
			
	// Colidindo com os itens "Pegando os itens"
	public static boolean isColliding(Entity e1, Entity e2) {
		Rectangle e1Mask = new Rectangle(e1.getX() + e1.maskx, e1.getY() + e1.masky, e1.mwidth, e1.mheight);
		Rectangle e2Mask = new Rectangle(e2.getX() + e2.maskx, e2.getY() + e2.masky, e2.mwidth, e2.mheight);
		return e1Mask.intersects(e2Mask);
	}

	public void render(Graphics g) {
		g.drawImage(sprite, this.getX() - Camera.x, this.getY() - Camera.y, null);
		// Debugador com a mascara de item
//		g.setColor(Color.red);
//		g.fillRect(this.getX() + maskx - Camera.x,this.getY() + masky - Camera.y,mwidth,mheight);
	}

}
