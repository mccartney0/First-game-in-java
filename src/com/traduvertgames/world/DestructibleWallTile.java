package com.traduvertgames.world;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class DestructibleWallTile extends WallTile {

    private final double maxHealth;
    private double health;

    public DestructibleWallTile(int x, int y, BufferedImage sprite) {
        this(x, y, sprite, 12.0);
    }

    public DestructibleWallTile(int x, int y, BufferedImage sprite, double maxHealth) {
        super(x, y, sprite);
        this.maxHealth = Math.max(1.0, maxHealth);
        this.health = this.maxHealth;
    }

    public boolean applyDamage(double amount) {
        if (health <= 0) {
            return true;
        }
        if (Double.isNaN(amount) || amount <= 0) {
            return false;
        }
        health -= amount;
        return health <= 0;
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    public double getHealthRatio() {
        if (maxHealth <= 0) {
            return 0;
        }
        return Math.max(0, health) / maxHealth;
    }

    @Override
    public void render(Graphics g) {
        super.render(g);
        if (isDestroyed()) {
            return;
        }
        double ratio = getHealthRatio();
        if (ratio >= 0.99) {
            return;
        }
        int screenX = getX() - Camera.x;
        int screenY = getY() - Camera.y;
        int alpha = (int) Math.min(180, Math.max(60, (1.0 - ratio) * 200));
        g.setColor(new Color(255, 120, 64, alpha));
        g.fillRect(screenX, screenY, World.TILE_SIZE, World.TILE_SIZE);
        g.setColor(new Color(60, 20, 0, Math.min(200, alpha + 40)));
        g.drawRect(screenX, screenY, World.TILE_SIZE - 1, World.TILE_SIZE - 1);
    }
}
