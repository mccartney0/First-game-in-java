package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.World;

public class BulletShoot extends Entity {

    private final double dx;
    private final double dy;
    private final double speed;
    private final Color overrideColor;

    private int life = 30, curLife = 0;

    private final boolean fromEnemy;
    private final double damage;

    public BulletShoot(int x, int y, int width, int height, BufferedImage sprite, double dx, double dy,
            double speed, double damage, boolean fromEnemy) {
        this(x, y, width, height, sprite, dx, dy, speed, damage, fromEnemy, null);
    }

    public BulletShoot(int x, int y, int width, int height, BufferedImage sprite, double dx, double dy,
            double speed, double damage, boolean fromEnemy, Color overrideColor) {
        super(x, y, width, height, sprite);
        this.dx = dx;
        this.dy = dy;
        this.speed = speed;
        this.damage = damage;
        this.fromEnemy = fromEnemy;
        this.overrideColor = overrideColor;
    }

    public void update() {
        x += dx * speed;
        y += dy * speed;
        curLife++;
        // Remover caso atinja uma parede ou saia do mapa
        if (hitWall() || curLife >= life) {
            Game.bullets.remove(this);
            return;
        }

        if (fromEnemy) {
            if (Entity.isColliding(this, Game.player)) {
                double scaledDamage = damage * Game.getDamageTakenMultiplier();
                Game.player.applyDamage(scaledDamage);
                Game.player.damage = true;
                Game.registerPlayerDamage();
                Game.bullets.remove(this);
                return;
            }
        }
    }

    public void render(Graphics g) {
        Color color = overrideColor;
        if (color == null) {
            color = fromEnemy ? Color.ORANGE : Color.pink;
        }
        g.setColor(color);
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

    public double getDamage() {
        return damage;
    }
}
