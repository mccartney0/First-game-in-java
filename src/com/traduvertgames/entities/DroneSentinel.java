package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.World;

/**
 * Drone sentinela: entidade autônoma que orbita o jogador a distância fixa,
 * atira automaticamente no inimigo mais próximo e se defende ao colidir.
 * Dura por tempo limitado (frames) ou até ser destruído pelo dano recebido.
 */
public class DroneSentinel extends Entity {

	private static final double ORBIT_RADIUS = 64.0;
	private static final double ORBIT_SPEED = 0.045;
	private static final int FIRE_INTERVAL_FRAMES = 14;
	private static final int LIFESPAN_FRAMES = 420;

	private double orbitAngle;
	private int fireCooldown;
	private int remainingFrames;
	private double life;

	public DroneSentinel(int x, int y) {
		super(x, y, 10, 10, null);
		this.orbitAngle = 0;
		this.fireCooldown = 0;
		this.remainingFrames = LIFESPAN_FRAMES;
		this.life = 30.0;
	}

	@Override
	public void update() {
		if (remainingFrames <= 0 || life <= 0) {
			// Explosão dourada ao se desintegrar (tempo esgotado ou destruído).
			com.traduvertgames.graficos.ParticleSystem.explode(
					(int) (this.getX() + width / 2.0),
					(int) (this.getY() + height / 2.0),
					new Color(255, 203, 5));
			Game.entities.remove(this);
			return;
		}
		remainingFrames--;

		// Orbita em torno do jogador.
		Player player = Game.player;
		if (player == null) {
			Game.entities.remove(this);
			return;
		}
		orbitAngle += ORBIT_SPEED;
		double targetX = player.getX() + 8 + Math.cos(orbitAngle) * ORBIT_RADIUS - width / 2.0;
		double targetY = player.getY() + 8 + Math.sin(orbitAngle) * ORBIT_RADIUS - height / 2.0;
		// Movimento suave (sem teleport).
		x += (targetX - x) * 0.2;
		y += (targetY - y) * 0.2;

		// Rastro dourado de propulsão durante a órbita.
		if (remainingFrames % 2 == 0) {
			com.traduvertgames.graficos.ParticleSystem.trail(
					(int) (this.getX() + width / 2.0),
					(int) (this.getY() + height / 2.0),
					new Color(255, 203, 5));
		}

		// Atira automaticamente no inimigo mais próximo.
		if (fireCooldown > 0) {
			fireCooldown--;
		}
		if (fireCooldown == 0) {
			Enemy nearest = findNearestEnemy();
			if (nearest != null) {
				fireAt(nearest);
			}
		}

		// Dano de contato: colide com inimigos próximos.
		for (int i = 0; i < Game.enemies.size(); i++) {
			Entity candidate = Game.enemies.get(i);
			if (candidate instanceof Enemy && Entity.isColliding(this, candidate)) {
				Enemy enemy = (Enemy) candidate;
				
				enemy.takeDamageDirect(3.0);
				life -= 6.0;
				break;
			}
		}
	}

	private Enemy findNearestEnemy() {
		Enemy best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < Game.enemies.size(); i++) {
			Entity candidate = Game.enemies.get(i);
			if (!(candidate instanceof Enemy)) {
				continue;
			}
			Enemy enemy = (Enemy) candidate;
			double distance = calculateDistance(getX() + width / 2, getY() + height / 2,
					enemy.getX() + enemy.mwidth / 2, enemy.getY() + enemy.mheight / 2);
			if (distance < bestDistance && distance <= 320) {
				best = enemy;
				bestDistance = distance;
			}
		}
		return best;
	}

	private void fireAt(Enemy target) {
		double originX = this.getX() + width / 2.0;
		double originY = this.getY() + height / 2.0;
		double angle = Math.atan2((target.getY() + target.mheight / 2.0) - originY,
				(target.getX() + target.mwidth / 2.0) - originX);
		double dx = Math.cos(angle);
		double dy = Math.sin(angle);
		int size = 6;
		BulletShoot bullet = new BulletShoot((int) originX, (int) originY, size, size, null, dx, dy,
				5.2, 5.5, false, new Color(255, 203, 5));
		bullet.setMask(0, 0, size, size);
		Game.bullets.add(bullet);
		fireCooldown = FIRE_INTERVAL_FRAMES;
	}

	@Override
	public void render(Graphics g) {
		int renderX = this.getX() - Camera.x;
		int renderY = this.getY() - Camera.y;
		// Aura de energia.
		g.setColor(new Color(255, 203, 5, 90));
		g.fillOval(renderX - 4, renderY - 4, width + 8, height + 8);
		// Corpo do drone (hexágono aproximado) com antena de radar.
		g.setColor(new Color(180, 140, 0));
		g.fillRect(renderX + 4, renderY - 3, 2, 4);
		g.setColor(new Color(255, 80, 80));
		g.fillRect(renderX + 4, renderY - 3, 2, 2);
		g.setColor(new Color(255, 203, 5));
		g.fillRect(renderX + 1, renderY + 3, width - 2, height - 6);
		g.fillRect(renderX + 3, renderY + 1, width - 6, height - 2);
		g.setColor(Color.WHITE);
		g.fillRect(renderX + 4, renderY + 4, 3, 3);
		// Propulsores traseiros piscando conforme a fase da órbita.
		if ((remainingFrames / 4) % 2 == 0) {
			g.setColor(new Color(255, 140, 0));
			g.fillRect(renderX + 2, renderY + height - 2, 2, 3);
			g.fillRect(renderX + width - 4, renderY + height - 2, 2, 3);
		}
		// Barra de vida curta sobre o drone.
		double percent = Math.max(0, Math.min(1, life / 30.0));
		g.setColor(Color.DARK_GRAY);
		g.fillRect(renderX, renderY - 5, width, 3);
		g.setColor(percent > 0.3 ? new Color(76, 175, 80) : new Color(244, 67, 54));
		g.fillRect(renderX, renderY - 5, (int) (width * percent), 3);
	}

	/** Dano de projéteis inimigos contra o drone. */
	public void applyDamage(double amount) {
		this.life -= amount;
	}
}
