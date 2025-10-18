package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.World;

public class BulletShoot extends Entity {

        private double dx;
        private double dy;
        private double spd = 4;

        private int life = 30, curLife = 0;

        private final boolean fromEnemy;

        public BulletShoot(int x, int y, int width, int height, BufferedImage sprite, double dx, double dy,
                        boolean fromEnemy) {
                super(x, y, width, height, sprite);
                this.dx = dx;
                this.dy = dy;
                this.fromEnemy = fromEnemy;
        }

        public void update() {
                x += dx * spd;
                y += dy * spd;
                curLife++;
                // Remover caso atinja uma parede ou saia do mapa
                if (hitWall() || curLife >= life) {
                        Game.bullets.remove(this);
                        return;
                }

                if (fromEnemy) {
                        if (Entity.isColliding(this, Game.player)) {
                                Game.player.life -= 2;
                                Game.player.damage = true;
                                Game.registerPlayerDamage();
                                Game.bullets.remove(this);
                                return;
                        }
                }
        }

        public void render(Graphics g) {
                g.setColor(Color.pink);
                g.fillOval(this.getX() - Camera.x, this.getY() - Camera.y, width, height);
        }

        private boolean hitWall() {
                int centerX = this.getX() + width / 2;
                int centerY = this.getY() + height / 2;
                return World.isWallByPixel(centerX, centerY);
        }

        public boolean isFromEnemy() {
                return fromEnemy;
        }

}
