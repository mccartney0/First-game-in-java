package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;

import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.world.AStar;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.Node;
import com.traduvertgames.world.Vector2i;
import com.traduvertgames.world.World;

public class Enemy extends Entity {

    private int frames = 0, maxFrames = 20, index = 0, maxIndex = 1;
    private BufferedImage[] sprites;
    public static int enemies = 0;

    private double life;
    private final double maxLife;

    private boolean isDamaged = false;
    private int damageFrames = 10, damageCurrent = 0;
    public static boolean addEnemy;

    private enum EnemyState {
        PATROLLING,
        CHASING
    }

    public enum Variant {
        SCOUT(2.0, 1.0, 1.0, 4.0, 2.0, 4, 75, 0, 0, new Color(255, 152, 0)),
        TELEPORTER(3.5, 1.1, 1.25, 5.5, 1.6, 4, 60, 220, 96, new Color(156, 39, 176)),
        ARTILLERY(6.0, 0.85, 0.9, 3.6, 2.8, 5, 110, 260, 128, new Color(3, 169, 244)),
        WARDEN(9.5, 0.7, 0.85, 3.0, 4.2, 6, 140, 0, 0, new Color(63, 81, 181)),
        SENTINEL(14.0, 0.85, 1.3, 4.2, 3.0, 6, 85, 180, 96, new Color(0, 150, 136)),
        RAVAGER(16.5, 1.25, 1.05, 4.8, 4.1, 6, 70, 150, 160, new Color(244, 81, 30)),
        WARBRINGER(18.0, 0.95, 1.1, 4.2, 6.5, 7, 90, 160, 140, new Color(233, 30, 99)),
        OVERSEER(28.0, 1.1, 1.2, 4.6, 5.2, 7, 80, 240, 160, new Color(121, 134, 203));

        private final double maxLife;
        private final double speedMultiplier;
        private final double strafeMultiplier;
        private final double projectileSpeed;
        private final double projectileDamage;
        private final int projectileSize;
        private final int attackCooldown;
        private final int specialCooldown;
        private final double specialRange;
        private final Color color;

        Variant(double maxLife, double speedMultiplier, double strafeMultiplier, double projectileSpeed,
                double projectileDamage, int projectileSize, int attackCooldown, int specialCooldown, double specialRange,
                Color color) {
            this.maxLife = maxLife;
            this.speedMultiplier = speedMultiplier;
            this.strafeMultiplier = strafeMultiplier;
            this.projectileSpeed = projectileSpeed;
            this.projectileDamage = projectileDamage;
            this.projectileSize = projectileSize;
            this.attackCooldown = attackCooldown;
            this.specialCooldown = specialCooldown;
            this.specialRange = specialRange;
            this.color = color;
        }

        double getMaxLife() {
            return maxLife;
        }

        double getSpeedMultiplier() {
            return speedMultiplier;
        }

        double getStrafeMultiplier() {
            return strafeMultiplier;
        }

        double getProjectileSpeed() {
            return projectileSpeed;
        }

        double getProjectileDamage() {
            return projectileDamage;
        }

        int getProjectileSize() {
            return projectileSize;
        }

        int getAttackCooldown() {
            return attackCooldown;
        }

        int getSpecialCooldown() {
            return specialCooldown;
        }

        double getSpecialRange() {
            return specialRange;
        }

        Color getProjectileColor() {
            return color;
        }

        Color getAuraColor() {
            return color;
        }
    }

    private EnemyState state = EnemyState.PATROLLING;
    private Vector2i spawnTile;
    private Vector2i patrolTarget;
    private int patrolTimer = 0;
    private int pathCooldown = 0;
    private int attackCooldown = 0;
    private int strafeDirection = 1;
    private int strafeTimer = 0;
    private int specialCooldown = 0;

    private final Variant variant;
    private final double patrolSpeed;
    private final double chaseSpeed;
    private final double strafeSpeed;
    private final double projectileSpeed;
    private final double projectileDamage;
    private final double specialRange;
    private final int projectileSize;
    private final int attackCooldownBase;
    private final int specialCooldownBase;
    private final Color projectileColor;
    private final Color auraColor;
    private final boolean boss;

    private static final double BASE_PATROL_SPEED = 0.6;
    private static final double BASE_CHASE_SPEED = 1.2;
    private static final double BASE_STRAFE_SPEED = 0.9;
    private static final double DETECTION_RADIUS = 160.0;
    private static final double LOSE_INTEREST_RADIUS = 220.0;
    private static final double ATTACK_RANGE = 120.0;
    private static final double STRAFE_RANGE = 42.0;
    private static final int PATROL_INTERVAL = 120;
    private static final int PATH_RECALC_INTERVAL = 18;

    public Enemy(int x, int y, int width, int height, BufferedImage sprite) {
        this(x, y, width, height, sprite, Variant.SCOUT, false);
    }

    public Enemy(int x, int y, int width, int height, BufferedImage sprite, Variant variant) {
        this(x, y, width, height, sprite, variant, false);
    }

    public Enemy(int x, int y, int width, int height, BufferedImage sprite, Variant variant, boolean boss) {
        super(x, y, width, height, null);
        this.variant = variant;
        this.boss = boss;
        this.maxLife = variant.getMaxLife();
        this.life = this.maxLife;
        sprites = new BufferedImage[2];
        sprites[0] = Game.spritesheet.getSprite(112, 16, 16, 16);
        sprites[1] = Game.spritesheet.getSprite(112 + 16, 16, 16, 16);
        spawnTile = new Vector2i(x / 16, y / 16);
        this.patrolSpeed = BASE_PATROL_SPEED * variant.getSpeedMultiplier();
        this.chaseSpeed = BASE_CHASE_SPEED * variant.getSpeedMultiplier();
        this.strafeSpeed = BASE_STRAFE_SPEED * variant.getStrafeMultiplier();
        this.projectileSpeed = variant.getProjectileSpeed();
        this.projectileDamage = variant.getProjectileDamage();
        this.projectileSize = variant.getProjectileSize();
        this.attackCooldownBase = Math.max(variant.getAttackCooldown(), 30);
        this.specialCooldownBase = Math.max(0, variant.getSpecialCooldown());
        this.specialRange = variant.getSpecialRange();
        this.projectileColor = variant.getProjectileColor();
        this.auraColor = variant.getAuraColor();
        if (this.specialCooldownBase > 0) {
            specialCooldown = Game.rand.nextInt(this.specialCooldownBase);
        }
        if (boss) {
            QuestManager.notifyBossSpotted();
        }
    }

    public static Enemy spawnRandomVariant(int x, int y) {
        return new Enemy(x, y, 16, 16, Entity.ENEMY_EN, pickRandomVariant());
    }

    private static Variant pickRandomVariant() {
        int roll = Game.rand.nextInt(100);
        if (roll < 35) {
            return Variant.SCOUT;
        } else if (roll < 55) {
            return Variant.TELEPORTER;
        } else if (roll < 75) {
            return Variant.ARTILLERY;
        } else if (roll < 90) {
            return Variant.WARDEN;
        } else if (roll < 96) {
            return Variant.SENTINEL;
        } else {
            return Variant.RAVAGER;
        }
    }

    public void update() {

        depth = 0;
        maskx = 8;
        masky = 8;
        mwidth = 8;
        mheight = 8;

        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (pathCooldown > 0) {
            pathCooldown--;
        }
        if (strafeTimer > 0) {
            strafeTimer--;
        }

        double distanceToPlayer = this.calculateDistance(this.getX(), this.getY(), Game.player.getX(),
                Game.player.getY());
        boolean canSeePlayer = hasLineOfSightToPlayer();

        if (state == EnemyState.PATROLLING) {
            handlePatrol();
            if (distanceToPlayer <= DETECTION_RADIUS && (canSeePlayer || distanceToPlayer <= DETECTION_RADIUS / 2)) {
                state = EnemyState.CHASING;
                path = null;
            }
        } else {
            handleChase(distanceToPlayer, canSeePlayer);
            if (distanceToPlayer > LOSE_INTEREST_RADIUS && !canSeePlayer) {
                state = EnemyState.PATROLLING;
                patrolTarget = null;
                path = null;
                patrolTimer = 0;
            }
        }

        updateVariantAbilities(distanceToPlayer, canSeePlayer);

        animate();

        collidingBullet();
        if (life <= 0) {
            destroySelf();
            enemies += 1;

            if (Game.enemies.size() == 0) {
                addEnemy = true;
            }
            return;
        }

        if (Enemy.addEnemy) {
            Enemy en = Enemy.spawnRandomVariant(1 * 16, 1 * 16);
            Game.entities.add(en);
            Game.enemies.add(en);
            Enemy.addEnemy = false;
        }

        if (isDamaged) {
            this.damageCurrent++;
            if (this.damageCurrent == this.damageFrames) {
                this.damageCurrent = 0;
                this.isDamaged = false;
            }
        }

    }

    private void updateVariantAbilities(double distanceToPlayer, boolean canSeePlayer) {
        if (variant == Variant.SCOUT) {
            return;
        }

        if (specialCooldown > 0) {
            specialCooldown--;
        }

        switch (variant) {
        case TELEPORTER:
            handleTeleportAbility(distanceToPlayer, canSeePlayer);
            break;
        case ARTILLERY:
            handleArtilleryAbility(distanceToPlayer);
            break;
        case SENTINEL:
            handleSentinelAbility(distanceToPlayer);
            break;
        case RAVAGER:
            handleRavagerAbility(distanceToPlayer);
            break;
        case OVERSEER:
            handleOverseerAbility(distanceToPlayer, canSeePlayer);
            break;
        default:
            break;
        }
    }

    private void handleTeleportAbility(double distanceToPlayer, boolean canSeePlayer) {
        if (specialCooldown > 0) {
            return;
        }
        boolean wantsTeleport = distanceToPlayer > Math.max(64, specialRange);
        if (!canSeePlayer && distanceToPlayer > 48) {
            wantsTeleport = true;
        }
        if (wantsTeleport && attemptTeleportNearPlayer()) {
            specialCooldown = specialCooldownBase;
            path = null;
            state = EnemyState.CHASING;
        }
    }

    private boolean attemptTeleportNearPlayer() {
        int attempts = 10;
        int minRadius = 48;
        int maxRadius = 112;
        for (int i = 0; i < attempts; i++) {
            double angle = Game.rand.nextDouble() * Math.PI * 2;
            double distance = minRadius + Game.rand.nextDouble() * (maxRadius - minRadius);
            int targetX = Game.player.getX() + (int) Math.round(Math.cos(angle) * distance);
            int targetY = Game.player.getY() + (int) Math.round(Math.sin(angle) * distance);
            targetX = (targetX / 16) * 16;
            targetY = (targetY / 16) * 16;

            if (!World.isFree(targetX, targetY, 0)) {
                continue;
            }
            if (this.calculateDistance(targetX, targetY, Game.player.getX(), Game.player.getY()) < 32) {
                continue;
            }
            boolean collidingOther = false;
            for (Enemy other : Game.enemies) {
                if (other == this) {
                    continue;
                }
                if (other.calculateDistance(targetX, targetY, other.getX(), other.getY()) < 16) {
                    collidingOther = true;
                    break;
                }
            }
            if (collidingOther) {
                continue;
            }

            this.x = targetX;
            this.y = targetY;
            strafeTimer = 0;
            return true;
        }
        return false;
    }

    private void handleArtilleryAbility(double distanceToPlayer) {
        if (specialCooldown > 0) {
            return;
        }
        if (distanceToPlayer >= Math.max(96, specialRange - 16)) {
            fireArtilleryBarrage();
            specialCooldown = specialCooldownBase;
        }
    }

    private void fireArtilleryBarrage() {
        double angle = Math.atan2(Game.player.getY() - this.getY(), Game.player.getX() - this.getX());
        double[] offsets = { -0.35, -0.18, 0.0, 0.18, 0.35 };
        for (int i = 0; i < offsets.length; i++) {
            double finalAngle = angle + offsets[i];
            double speedModifier = i == 2 ? 1.1 : 0.85;
            double damageModifier = i == 2 ? 1.2 : 0.8;
            Color color = i == 2 ? projectileColor : projectileColor.brighter();
            spawnProjectile(finalAngle, projectileSpeed * speedModifier, projectileDamage * damageModifier,
                    projectileSize + (i == 2 ? 1 : 0), color);
        }
        attackCooldown = Math.max(attackCooldown, attackCooldownBase + 30);
    }

    private void handleSentinelAbility(double distanceToPlayer) {
        if (specialCooldown > 0) {
            return;
        }
        if (distanceToPlayer > specialRange) {
            return;
        }

        emitRadialBurst(8, projectileSpeed * 0.8, projectileDamage * 0.75, projectileColor.brighter());
        double healAmount = maxLife * 0.2;
        life = Math.min(maxLife, life + healAmount);
        specialCooldown = specialCooldownBase;
    }

    private void handleRavagerAbility(double distanceToPlayer) {
        if (specialCooldown > 0) {
            return;
        }
        if (distanceToPlayer < 48 || distanceToPlayer > specialRange) {
            return;
        }

        double angle = Math.atan2(Game.player.getY() - this.getY(), Game.player.getX() - this.getX());
        double dashDistance = chaseSpeed * 3.4;
        boolean moved = false;
        int dashSteps = 4;
        double stepX = Math.cos(angle) * (dashDistance / dashSteps);
        double stepY = Math.sin(angle) * (dashDistance / dashSteps);
        for (int i = 0; i < dashSteps; i++) {
            moved |= tryMove(stepX, stepY);
        }

        if (moved) {
            specialCooldown = specialCooldownBase;
            attackCooldown = Math.max(attackCooldown, attackCooldownBase / 2);
        }
    }

    private void handleOverseerAbility(double distanceToPlayer, boolean canSeePlayer) {
        if (specialCooldown > 0) {
            return;
        }
        if (!canSeePlayer && distanceToPlayer > specialRange) {
            return;
        }

        emitRadialBurst(12, projectileSpeed * 0.95, projectileDamage, projectileColor);
        spawnSupportDrones();
        specialCooldown = specialCooldownBase + 60;
    }

    private void emitRadialBurst(int projectiles, double speed, double damage, Color color) {
        if (projectiles <= 0) {
            return;
        }
        double angleStep = (Math.PI * 2) / projectiles;
        for (int i = 0; i < projectiles; i++) {
            double angle = angleStep * i;
            spawnProjectile(angle, speed, damage, Math.max(3, projectileSize - 1), color);
        }
    }

    private void spawnSupportDrones() {
        int maxNew = Math.max(0, 12 - Game.enemies.size());
        if (maxNew <= 0) {
            return;
        }
        int desired = Math.min(2 + Game.rand.nextInt(2), maxNew);
        for (int i = 0; i < desired; i++) {
            Vector2i spawn = findSupportSpawnPosition();
            if (spawn == null) {
                continue;
            }
            Enemy minion = new Enemy(spawn.x, spawn.y, 16, 16, Entity.ENEMY_EN, Variant.SENTINEL);
            Game.entities.add(minion);
            Game.enemies.add(minion);
        }
    }

    private Vector2i findSupportSpawnPosition() {
        int attempts = 12;
        int minRadius = 48;
        int maxRadius = 112;
        for (int i = 0; i < attempts; i++) {
            double angle = Game.rand.nextDouble() * Math.PI * 2;
            double distance = minRadius + Game.rand.nextDouble() * (maxRadius - minRadius);
            int targetX = this.getX() + (int) Math.round(Math.cos(angle) * distance);
            int targetY = this.getY() + (int) Math.round(Math.sin(angle) * distance);
            targetX = (targetX / 16) * 16;
            targetY = (targetY / 16) * 16;

            if (!World.isFree(targetX, targetY, 0)) {
                continue;
            }
            boolean occupied = false;
            for (Enemy enemy : Game.enemies) {
                if (enemy.calculateDistance(targetX, targetY, enemy.getX(), enemy.getY()) < 16) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) {
                continue;
            }

            if (this.calculateDistance(targetX, targetY, Game.player.getX(), Game.player.getY()) < 24) {
                continue;
            }

            return new Vector2i(targetX, targetY);
        }
        return null;
    }

    private void animate() {
        frames++;
        if (frames >= maxFrames) {
            frames = 0;
            index++;
            if (index > maxIndex) {
                index = 0;
            }
        }
    }

    private void handlePatrol() {
        if (patrolTimer > 0) {
            patrolTimer--;
        }

        if ((patrolTarget == null || reachedTile(patrolTarget) || path == null || path.isEmpty()) && patrolTimer <= 0) {
            chooseNewPatrolTarget();
        }

        if (path != null && !path.isEmpty()) {
            moveAlongPath(patrolSpeed);
        }
    }

    private void handleChase(double distanceToPlayer, boolean canSeePlayer) {
        if (distanceToPlayer > STRAFE_RANGE) {
            if (canSeePlayer) {
                moveDirectlyTowardsPlayer(chaseSpeed);
            } else {
                recalculatePathToPlayer();
                if (path != null && !path.isEmpty()) {
                    moveAlongPath(chaseSpeed);
                }
            }
        } else {
            strafeAroundPlayer();
        }

        if (canSeePlayer) {
            if (distanceToPlayer <= ATTACK_RANGE) {
                shootAtPlayer();
            }
        } else {
            recalculatePathToPlayer();
        }

        if (isCollidingWithPlayer()) {
            if (Game.rand.nextInt(100) < 20) {
                double scaledDamage = 2 * Game.getDamageTakenMultiplier();
                Game.player.applyDamage(scaledDamage);
                Game.player.damage = true;
                Game.registerPlayerDamage();
            }
        }
    }

    private void moveAlongPath(double speed) {
        if (path == null || path.isEmpty()) {
            return;
        }

        Vector2i target = path.get(path.size() - 1).tile;
        double targetX = target.x * 16;
        double targetY = target.y * 16;
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance < 1) {
            x = targetX;
            y = targetY;
            path.remove(path.size() - 1);
            return;
        }

        double stepX = (dx / distance) * speed;
        double stepY = (dy / distance) * speed;
        if (!tryMove(stepX, stepY)) {
            path.remove(path.size() - 1);
        }
    }

    private void moveDirectlyTowardsPlayer(double speed) {
        double angle = Math.atan2(Game.player.getY() - this.getY(), Game.player.getX() - this.getX());
        double stepX = Math.cos(angle) * speed;
        double stepY = Math.sin(angle) * speed;
        if (!tryMove(stepX, stepY)) {
            recalculatePathToPlayer();
        }
    }

    private void strafeAroundPlayer() {
        if (strafeTimer <= 0) {
            strafeDirection = Game.rand.nextBoolean() ? 1 : -1;
            strafeTimer = 30;
        }

        double angle = Math.atan2(Game.player.getY() - this.getY(), Game.player.getX() - this.getX());
        double perpendicular = angle + (Math.PI / 2) * strafeDirection;
        double stepX = Math.cos(perpendicular) * strafeSpeed;
        double stepY = Math.sin(perpendicular) * strafeSpeed;

        if (!tryMove(stepX, stepY)) {
            strafeDirection *= -1;
            tryMove(Math.cos(angle) * chaseSpeed * 0.5, Math.sin(angle) * chaseSpeed * 0.5);
            strafeTimer = 15;
        }
    }

    private boolean tryMove(double dx, double dy) {
        boolean moved = false;
        double newX = x + dx;
        double newY = y + dy;

        if (World.isFree((int) newX, (int) y, 0)) {
            x = newX;
            moved = true;
        }

        if (World.isFree((int) x, (int) newY, 0)) {
            y = newY;
            moved = true;
        }

        return moved;
    }

    private boolean hasLineOfSightToPlayer() {
        int startX = this.getX() + this.mwidth / 2;
        int startY = this.getY() + this.mheight / 2;
        int endX = Game.player.getX() + Game.player.getWidth() / 2;
        int endY = Game.player.getY() + Game.player.getHeight() / 2;

        int steps = Math.max(Math.abs(endX - startX), Math.abs(endY - startY));
        if (steps == 0) {
            return true;
        }

        double stepX = (endX - startX) / (double) steps;
        double stepY = (endY - startY) / (double) steps;
        double currentX = startX;
        double currentY = startY;

        for (int i = 0; i < steps; i++) {
            currentX += stepX;
            currentY += stepY;
            int tileX = (int) (currentX / World.TILE_SIZE);
            int tileY = (int) (currentY / World.TILE_SIZE);
            if (World.isWallTile(tileX, tileY)) {
                return false;
            }
        }

        return true;
    }

    private void recalculatePathToPlayer() {
        if (pathCooldown > 0) {
            return;
        }

        Vector2i startNode = new Vector2i((int) (x / 16), (int) (y / 16));
        Vector2i endNode = new Vector2i(Game.player.getX() / 16, Game.player.getY() / 16);
        List<Node> newPath = AStar.findPath(Game.world, startNode, endNode);
        if (newPath != null && !newPath.isEmpty()) {
            path = newPath;
        }

        pathCooldown = PATH_RECALC_INTERVAL;
    }

    private void chooseNewPatrolTarget() {
        int attempts = 0;
        while (attempts < 8) {
            attempts++;
            int radius = 4;
            int tileX = spawnTile.x + Game.rand.nextInt(radius * 2 + 1) - radius;
            int tileY = spawnTile.y + Game.rand.nextInt(radius * 2 + 1) - radius;

            if (!World.isValidTile(tileX, tileY) || World.isWallTile(tileX, tileY)) {
                continue;
            }

            Vector2i startNode = new Vector2i((int) (x / 16), (int) (y / 16));
            Vector2i candidate = new Vector2i(tileX, tileY);
            List<Node> newPath = AStar.findPath(Game.world, startNode, candidate);
            if (newPath != null && !newPath.isEmpty()) {
                path = newPath;
                patrolTarget = candidate;
                patrolTimer = PATROL_INTERVAL;
                return;
            }
        }

        patrolTimer = PATROL_INTERVAL / 2;
    }

    private boolean reachedTile(Vector2i tile) {
        if (tile == null) {
            return false;
        }
        return this.getX() / 16 == tile.x && this.getY() / 16 == tile.y;
    }

    private void shootAtPlayer() {
        if (attackCooldown > 0) {
            return;
        }

        double angle = Math.atan2(Game.player.getY() - this.getY(), Game.player.getX() - this.getX());
        spawnProjectile(angle, projectileSpeed, projectileDamage, projectileSize, projectileColor);

        attackCooldown = attackCooldownBase - Game.rand.nextInt(Math.max(1, attackCooldownBase / 3));
    }

    private void spawnProjectile(double angle, double speed, double damage, int size, Color color) {
        double dx = Math.cos(angle);
        double dy = Math.sin(angle);
        int originX = this.getX() + 8 - size / 2;
        int originY = this.getY() + 8 - size / 2;
        BulletShoot bullet = new BulletShoot(originX, originY, size, size, null, dx, dy, speed, damage, true, color);
        bullet.setMask(0, 0, size, size);
        Game.bullets.add(bullet);
    }

    public void destroySelf() {
        Game.registerEnemyKill();
        maybeDropPickup();
        Game.enemies.remove(this);
        Game.entities.remove(this);
        QuestManager.notifyEnemyKilled(this);
    }

    private void maybeDropPickup() {
        int spawnX = this.getX();
        int spawnY = this.getY();

        if (Game.rand.nextDouble() < 0.14) {
            ShieldOrb orb = new ShieldOrb(spawnX, spawnY);
            Game.entities.add(orb);
        }

        if (Game.rand.nextDouble() < 0.2) {
            EnergyCell cell = new EnergyCell(spawnX, spawnY);
            Game.entities.add(cell);
        }

        if (Game.rand.nextDouble() < 0.12) {
            NanoMedkit medkit = new NanoMedkit(spawnX, spawnY);
            Game.entities.add(medkit);
        }

        if (Game.rand.nextDouble() < 0.08) {
            OverclockModule module = new OverclockModule(spawnX, spawnY);
            Game.entities.add(module);
        }
    }

    public void collidingBullet() {
        for (int i = 0; i < Game.bullets.size(); i++) {
            Entity e = Game.bullets.get(i);
            if (e instanceof BulletShoot) {
                BulletShoot bullet = (BulletShoot) e;
                if (bullet.isFromEnemy()) {
                    continue;
                }
            }

            if (Entity.isColliding(this, e)) {
                isDamaged = true;
                double takenDamage = 1.0;
                if (e instanceof BulletShoot) {
                    BulletShoot projectile = (BulletShoot) e;
                    takenDamage = projectile.getDamage();
                }
                life -= takenDamage;
                Game.bullets.remove(i);
                i--;
                return;
            }
        }

    }

    public boolean isCollidingWithPlayer() {
        int enemyX = this.getX() + maskx;
        int enemyY = this.getY() + masky;
        int playerX = Game.player.getX();
        int playerY = Game.player.getY();

        return enemyX < playerX + 16 && enemyX + mwidth > playerX && enemyY < playerY + 16 && enemyY + mheight > playerY;
    }

    public boolean isBoss() {
        return boss;
    }

    public void render(Graphics g) {
        if (!isDamaged) {
            g.drawImage(sprites[index], this.getX() + 4 - Camera.x, this.getY() + 4 - Camera.y, null);
        } else {
            g.drawImage(Entity.ENEMY_FEEDBACK, this.getX() + 4 - Camera.x, this.getY() + 4 - Camera.y, null);
        }

        if (variant != Variant.SCOUT) {
            Color aura = new Color(auraColor.getRed(), auraColor.getGreen(), auraColor.getBlue(), 120);
            g.setColor(aura);
            g.drawOval(this.getX() + 2 - Camera.x, this.getY() + 2 - Camera.y, 12, 12);
        }
    }
}
