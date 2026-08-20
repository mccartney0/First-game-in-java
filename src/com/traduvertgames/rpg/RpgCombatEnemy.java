package com.traduvertgames.rpg;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.graficos.AssetCatalog;

/** Inimigo leve, determinístico e independente do loop do shooter para o modo RPG. */
public final class RpgCombatEnemy {
    public enum Kind { STALKER, SNIPER, MARSH_WARDEN }

    private final Kind kind;
    private final String name;
    private final int maxLife;
    private final int attack;
    private final int experienceReward;
    private final int dropElixirs;
    private final boolean boss;
    private double x;
    private double y;
    private int life;
    private int attackCooldown;

    public RpgCombatEnemy(Kind kind, String name, double x, double y, int maxLife, int attack,
            int experienceReward, int dropElixirs, boolean boss) {
        this.kind = kind == null ? Kind.STALKER : kind;
        this.name = name == null ? "Ameaça da Charneca" : name;
        this.x = x;
        this.y = y;
        this.maxLife = Math.max(1, maxLife);
        this.life = this.maxLife;
        this.attack = Math.max(1, attack);
        this.experienceReward = Math.max(1, experienceReward);
        this.dropElixirs = Math.max(0, dropElixirs);
        this.boss = boss;
    }

    public int update(RpgPlayerController player, RpgMap map, int physicalDefense) {
        if (!isAlive() || player == null || map == null) return 0;
        if (attackCooldown > 0) attackCooldown--;
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double desiredRange = kind == Kind.SNIPER ? 180 : kind == Kind.MARSH_WARDEN ? 44 : 34;
        if (distance > desiredRange && distance < 300) {
            double speed = kind == Kind.STALKER ? 1.2 : kind == Kind.MARSH_WARDEN ? 0.7 : 0.45;
            double nx = x + dx / Math.max(1, distance) * speed;
            double ny = y + dy / Math.max(1, distance) * speed;
            if (map.isWalkable(nx, ny, 18, 18)) {
                x = nx;
                y = ny;
            }
        }
        double attackRange = kind == Kind.SNIPER ? 190 : kind == Kind.MARSH_WARDEN ? 52 : 38;
        if (distance <= attackRange && attackCooldown <= 0) {
            attackCooldown = kind == Kind.SNIPER ? 95 : kind == Kind.MARSH_WARDEN ? 72 : 58;
            return Math.max(1, attack - Math.max(0, physicalDefense) / 3);
        }
        return 0;
    }

    public boolean hit(int damage) {
        if (!isAlive()) return false;
        life = Math.max(0, life - Math.max(1, damage));
        return life == 0;
    }

    public void render(Graphics g, double cameraX, double cameraY) {
        if (!isAlive()) return;
        int drawX = (int) (x - cameraX);
        int drawY = (int) (y - cameraY);
        Color body = kind == Kind.SNIPER ? new Color(130, 86, 166)
                : kind == Kind.MARSH_WARDEN ? new Color(120, 70, 62) : new Color(62, 124, 80);
        Color accent = kind == Kind.SNIPER ? new Color(240, 180, 78)
                : kind == Kind.MARSH_WARDEN ? new Color(220, 91, 72) : new Color(155, 220, 107);
        g.setColor(new Color(17, 22, 25, 130));
        g.fillOval(drawX - 12, drawY + 9, 24, 7);
        BufferedImage sprite = AssetCatalog.enemySprite(spriteVariant());
        if (sprite != null) {
            g.drawImage(sprite, drawX - 14, drawY - 19, 28, 28, null);
        } else {
            g.setColor(body);
            g.fillRoundRect(drawX - 10, drawY - 16, 20, 25, boss ? 10 : 7, boss ? 10 : 7);
            g.setColor(accent);
            g.fillRect(drawX - 5, drawY - 8, 10, 4);
            if (kind == Kind.SNIPER) g.fillRect(drawX + 6, drawY - 18, 4, 15);
        }
        if (boss) g.drawOval(drawX - 14, drawY - 20, 28, 31);
        g.setColor(new Color(14, 19, 23, 215));
        g.fillRoundRect(drawX - 22, drawY - 28, 44, 7, 3, 3);
        g.setColor(new Color(201, 75, 72));
        g.fillRoundRect(drawX - 21, drawY - 27, Math.max(1, 42 * life / maxLife), 5, 2, 2);
        g.setColor(new Color(239, 229, 193));
        g.drawString(name, drawX - Math.max(18, name.length() * 3), drawY - 33);
    }

    public boolean isAlive() { return life > 0; }

    private Enemy.Variant spriteVariant() {
        if (kind == Kind.SNIPER) return Enemy.Variant.SNIPER;
        if (kind == Kind.MARSH_WARDEN) return Enemy.Variant.GUARDIAN;
        return Enemy.Variant.PHANTOM;
    }

    public boolean isBoss() { return boss; }
    public int getLife() { return life; }
    public int getMaxLife() { return maxLife; }
    public int getExperienceReward() { return experienceReward; }
    public int getDropElixirs() { return dropElixirs; }
    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
}
