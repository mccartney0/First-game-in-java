package com.traduvertgames.rpg;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.graficos.AssetCatalog;

/** Inimigo leve, determinístico e independente do loop do shooter para o modo RPG. */
public final class RpgCombatEnemy {
    public enum Kind { STALKER, SNIPER, MARSH_WARDEN, MIRE_HOUND, BOG_ORACLE, MIRE_BRUTE, MIST_SOVEREIGN }

    private final Kind kind;
    private final String name;
    private final int maxLife;
    private final int attack;
    private final int experienceReward;
    private final int dropElixirs;
    private final boolean boss;
    private final double moveSpeed;
    private final String behaviorTag;
    private final String contentSpriteId;
    private final RpgBossAbilityProfile specialAbility;
    private double x;
    private double y;
    private int life;
    private int attackCooldown;
    private int specialCooldown;
    private int specialPulseFrames;
    private boolean specialUsedLastUpdate;

    public RpgCombatEnemy(Kind kind, String name, double x, double y, int maxLife, int attack,
            int experienceReward, int dropElixirs, boolean boss) {
        this(kind, name, x, y, maxLife, attack, experienceReward, dropElixirs, boss,
                defaultSpeed(kind), defaultBehavior(kind), null, null);
    }

    public RpgCombatEnemy(Kind kind, String name, double x, double y, int maxLife, int attack,
            int experienceReward, int dropElixirs, boolean boss, double moveSpeed, String behaviorTag,
            String contentSpriteId) {
        this(kind, name, x, y, maxLife, attack, experienceReward, dropElixirs, boss, moveSpeed, behaviorTag,
                contentSpriteId, null);
    }

    public RpgCombatEnemy(Kind kind, String name, double x, double y, int maxLife, int attack,
            int experienceReward, int dropElixirs, boolean boss, double moveSpeed, String behaviorTag,
            String contentSpriteId, RpgBossAbilityProfile specialAbility) {
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
        this.moveSpeed = Math.max(0.1, moveSpeed);
        this.behaviorTag = behaviorTag == null ? defaultBehavior(this.kind) : behaviorTag;
        this.contentSpriteId = contentSpriteId;
        this.specialAbility = specialAbility;
    }

    public int update(RpgPlayerController player, RpgMap map, int physicalDefense) {
        if (!isAlive() || player == null || map == null) return 0;
        if (attackCooldown > 0) attackCooldown--;
        if (specialCooldown > 0) specialCooldown--;
        if (specialPulseFrames > 0) specialPulseFrames--;
        specialUsedLastUpdate = false;
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double desiredRange = isRanged() ? 150 : isFortified() ? 42 : isPounce() ? 30
                : kind == Kind.SNIPER ? 180 : kind == Kind.MARSH_WARDEN ? 44 : 34;
        if (distance > desiredRange && distance < 300) {
            double nx = x + dx / Math.max(1, distance) * moveSpeed;
            double ny = y + dy / Math.max(1, distance) * moveSpeed;
            if (map.isWalkable(nx, ny, 18, 18)) {
                x = nx;
                y = ny;
            }
        }
        if (specialAbility != null && distance <= specialAbility.getRange() && specialCooldown <= 0) {
            specialCooldown = specialAbility.getCooldownTicks();
            specialPulseFrames = 24;
            specialUsedLastUpdate = true;
            return Math.max(1, specialAbility.getDamage() - Math.max(0, physicalDefense) / 2);
        }
        double attackRange = isRanged() ? 165 : isFortified() ? 58 : isPounce() ? 42
                : kind == Kind.SNIPER ? 190 : kind == Kind.MARSH_WARDEN ? 52 : 38;
        if (distance <= attackRange && attackCooldown <= 0) {
            attackCooldown = isRanged() ? 80 : isFortified() ? 75 : isPounce() ? 42
                    : kind == Kind.SNIPER ? 95 : kind == Kind.MARSH_WARDEN ? 72 : 58;
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
        BufferedImage sprite = contentSpriteId == null ? AssetCatalog.enemySprite(spriteVariant())
                : AssetCatalog.contentEnemySprite(contentSpriteId);
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
        if (specialPulseFrames > 0) {
            int radius = 16 + (24 - specialPulseFrames) / 2;
            g.setColor(new Color(222, 70, 111, 145));
            g.drawOval(drawX - radius, drawY - radius, radius * 2, radius * 2);
            g.setColor(new Color(255, 204, 164, 100));
            g.drawOval(drawX - radius - 3, drawY - radius - 3, radius * 2 + 6, radius * 2 + 6);
            BufferedImage abilityIcon = AssetCatalog.contentAbilityIcon("mist_sovereign_nucleo_da_bruma");
            if (abilityIcon != null) g.drawImage(abilityIcon, drawX - 8, drawY - 8, 16, 16, null);
        }
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
        if (kind == Kind.MIST_SOVEREIGN) return Enemy.Variant.GUARDIAN;
        return Enemy.Variant.PHANTOM;
    }

    private boolean isRanged() { return "snipe".equals(behaviorTag) || "hex".equals(behaviorTag) || "bombard".equals(behaviorTag); }
    private boolean isPounce() { return "pounce".equals(behaviorTag); }
    private boolean isFortified() { return "fortify".equals(behaviorTag); }

    private static double defaultSpeed(Kind kind) {
        if (kind == Kind.MIRE_HOUND) return 1.65;
        if (kind == Kind.BOG_ORACLE) return 0.7;
        if (kind == Kind.MIRE_BRUTE) return 0.65;
        if (kind == Kind.MIST_SOVEREIGN) return 0.55;
        if (kind == Kind.STALKER) return 1.2;
        if (kind == Kind.MARSH_WARDEN) return 0.7;
        return 0.45;
    }

    private static String defaultBehavior(Kind kind) {
        if (kind == Kind.MIRE_HOUND) return "pounce";
        if (kind == Kind.BOG_ORACLE) return "hex";
        if (kind == Kind.MIRE_BRUTE) return "fortify";
        if (kind == Kind.SNIPER) return "snipe";
        if (kind == Kind.MARSH_WARDEN) return "regenerate";
        if (kind == Kind.MIST_SOVEREIGN) return "regenerate";
        return "drain";
    }

    public boolean isBoss() { return boss; }
    public int getLife() { return life; }
    public int getMaxLife() { return maxLife; }
    public int getExperienceReward() { return experienceReward; }
    public int getDropElixirs() { return dropElixirs; }
    public String getName() { return name; }
    public Kind getKind() { return kind; }
    public boolean wasSpecialUsedLastUpdate() { return specialUsedLastUpdate; }
    public int getSpecialPulseFrames() { return specialPulseFrames; }
    public double getX() { return x; }
    public double getY() { return y; }
}
