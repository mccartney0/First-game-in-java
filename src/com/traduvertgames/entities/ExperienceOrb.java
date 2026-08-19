package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.LevelUpManager;
import com.traduvertgames.world.Camera;

/** Cristal de XP do modo sobrevivência, atraído pelo piloto e coletado por contato. */
public final class ExperienceOrb extends Entity {

    private final int amount;
    private double vx;
    private double vy;
    private int lifeFrames = 60 * 30;

    public ExperienceOrb(int x, int y, int amount) {
        super(x, y, 7, 7, null);
        this.amount = Math.max(1, amount);
    }

    @Override
    public void update() {
        if (Game.player == null || lifeFrames-- <= 0) {
            Game.entities.remove(this);
            return;
        }
        double dx = Game.player.getX() - x;
        double dy = Game.player.getY() - y;
        double distance = Math.hypot(dx, dy);
        double magnetRadius = LevelUpManager.getMagnetRadius();
        if (distance <= magnetRadius && distance > 0.1) {
            double acceleration = distance < magnetRadius * 0.45 ? 0.12 : 0.045;
            vx += dx / distance * acceleration;
            vy += dy / distance * acceleration;
            double speed = Math.hypot(vx, vy);
            if (speed > 3.2) {
                vx = vx / speed * 3.2;
                vy = vy / speed * 3.2;
            }
        } else {
            vx *= 0.94;
            vy *= 0.94;
        }
        x += vx;
        y += vy;
        if (Entity.isColliding(this, Game.player)) {
            LevelUpManager.collectXp(amount);
			com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.EXPERIENCE_ORB);
            Game.entities.remove(this);
        }
    }

    @Override
    public void render(Graphics g) {
        int drawX = getX() - Camera.x;
        int drawY = getY() - Camera.y;
        g.setColor(new Color(255, 214, 10, 100));
        g.fillOval(drawX - 3, drawY - 3, width + 6, height + 6);
        g.setColor(new Color(255, 235, 59));
        g.fillOval(drawX, drawY, width, height);
        g.setColor(Color.WHITE);
        g.fillRect(drawX + 2, drawY + 1, 2, 2);
    }
}
