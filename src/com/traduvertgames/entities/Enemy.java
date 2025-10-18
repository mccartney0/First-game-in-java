package com.traduvertgames.entities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;

import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.AStar;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.Node;
import com.traduvertgames.world.Vector2i;
import com.traduvertgames.world.World;

public class Enemy extends Entity {

        private int frames = 0, maxFrames = 20, index = 0, maxIndex = 1;
	private BufferedImage[] sprites;
	public static int enemies = 0;

        private double life = 2;

	private boolean isDamaged = false;
	private int damageFrames = 10, damageCurrent = 0;
//	private int maskx=8,masky=8,maskw=10,maskh=10;
        public static boolean addEnemy;

        private enum EnemyState {
                PATROLLING,
                CHASING
        }

        private EnemyState state = EnemyState.PATROLLING;
        private Vector2i spawnTile;
        private Vector2i patrolTarget;
        private int patrolTimer = 0;
        private int pathCooldown = 0;
        private int attackCooldown = 0;
        private int strafeDirection = 1;
        private int strafeTimer = 0;

        private static final double PATROL_SPEED = 0.6;
        private static final double CHASE_SPEED = 1.2;
        private static final double STRAFE_SPEED = 0.9;
        private static final double DETECTION_RADIUS = 160.0;
        private static final double LOSE_INTEREST_RADIUS = 220.0;
        private static final double ATTACK_RANGE = 120.0;
        private static final double STRAFE_RANGE = 42.0;
        private static final int PATROL_INTERVAL = 120;
        private static final int PATH_RECALC_INTERVAL = 18;
        private static final int MAX_ATTACK_COOLDOWN = 75;

        public Enemy(int x, int y, int width, int height, BufferedImage sprite) {
                super(x, y, width, height, null);
                sprites = new BufferedImage[2];
                sprites[0] = Game.spritesheet.getSprite(112, 16, 16, 16);
                sprites[1] = Game.spritesheet.getSprite(112 + 16, 16, 16, 16);
                spawnTile = new Vector2i(x / 16, y / 16);
        }

        public void update() {

                depth = 0;
                // Ajustando a mascara de colisao.
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
                        if (distanceToPlayer <= DETECTION_RADIUS
                                        && (canSeePlayer || distanceToPlayer <= DETECTION_RADIUS / 2)) {
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
                        Enemy en = new Enemy(1 * 16, 1 * 16, 16, 16, Entity.ENEMY_EN);
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

                if ((patrolTarget == null || reachedTile(patrolTarget) || path == null || path.isEmpty())
                                && patrolTimer <= 0) {
                        chooseNewPatrolTarget();
                }

                if (path != null && !path.isEmpty()) {
                        moveAlongPath(PATROL_SPEED);
                }
        }

        private void handleChase(double distanceToPlayer, boolean canSeePlayer) {
                if (distanceToPlayer > STRAFE_RANGE) {
                        if (canSeePlayer) {
                                moveDirectlyTowardsPlayer(CHASE_SPEED);
                        } else {
                                recalculatePathToPlayer();
                                if (path != null && !path.isEmpty()) {
                                        moveAlongPath(CHASE_SPEED);
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
                                Game.player.life -= 2;
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
                double stepX = Math.cos(perpendicular) * STRAFE_SPEED;
                double stepY = Math.sin(perpendicular) * STRAFE_SPEED;

                if (!tryMove(stepX, stepY)) {
                        strafeDirection *= -1;
                        tryMove(Math.cos(angle) * CHASE_SPEED * 0.5, Math.sin(angle) * CHASE_SPEED * 0.5);
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
                double dx = Math.cos(angle);
                double dy = Math.sin(angle);
                BulletShoot bullet = new BulletShoot(this.getX() + 8, this.getY() + 8, 4, 4, null, dx, dy, 4.0, 2.0,
                                true);
                bullet.setMask(0, 0, 4, 4);
                Game.bullets.add(bullet);
                attackCooldown = MAX_ATTACK_COOLDOWN - Game.rand.nextInt(20);
        }
        public void destroySelf() {
                Game.registerEnemyKill();
                Game.enemies.remove(this);
                Game.entities.remove(this);
        }

	// Tirando dano com tiro
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

	// Fazer nenhum NPC colidir com o outro
//			public boolean isColliding(int xNext, int yNext) {
//				Rectangle enemyCurrent = new Rectangle(xNext+ maskx, yNext + masky,mwidth,mheight);
//				for (int i = 0; i < Game.enemies.size(); i++) {
//					Enemy e = Game.enemies.get(i);
//					if (e == this)
//						continue;
//					Rectangle targetEnemy = new Rectangle(e.getX()+ maskx, e.getY()+ masky, mwidth,mheight);
//					if (enemyCurrent.intersects(targetEnemy)) {
//						return true;
//					}
//				}
//
//				return false;
//			}

	public boolean isCollidingWithPlayer() {
		int enemyX = this.getX() + maskx;
		int enemyY = this.getY() + masky;
		int playerX = Game.player.getX();
		int playerY = Game.player.getY();

		return enemyX < playerX + 16 && enemyX + mwidth > playerX && enemyY < playerY + 16 && enemyY + mheight > playerY;
	}

	// Trocar a máscara quadrada para nao colidir
//	Testar mascara de colisão
//	public void render (Graphics g) {
//		super.render(g);
//		g.setColor(Color.blue);
//		g.fillRect(this.getX()+maskx-Camera.x,this.getY()+ masky-Camera.y,maskw,masky);
//	}

	public void render(Graphics g) {
		if (!isDamaged) {
			// * Sempre que renderizar, tem que por a posição da Camera
			g.drawImage(sprites[index], this.getX() + 4 - Camera.x, this.getY() + 4 - Camera.y, null);
		} else {
			g.drawImage(Entity.ENEMY_FEEDBACK, this.getX() + 4 - Camera.x, this.getY() + 4 - Camera.y, null);
		}
	}
}
