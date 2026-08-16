package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;

/**
 * Relâmpago do Arco em Cadeia: projétil normal até o primeiro inimigo; a partir
 * daí salta entre inimigos próximos (máximo de saltos configurável), aplicando
 * dano decrescente a cada alvo, sem depender do projétil que o originou.
 */
public class ChainArcProjectile extends BulletShoot {

	private static final double CHAIN_RANGE = 120.0;
	private static final int MAX_CHAIN_JUMPS = 3;

	private final double speedFactor;
	private final List<Enemy> hitEnemies = new ArrayList<Enemy>();
	private int jumps = 0;
	private double chainFactor = 1.0;
	private Enemy currentTarget;
	// Flash de impacto exibido no alvo por poucos frames após cada salto.
	private int impactFlashFrames = 0;
	private double flashX;
	private double flashY;

	public ChainArcProjectile(int x, int y, int size, double dx, double dy, double speed, double damage) {
		super(x, y, size, size, null, dx, dy, speed, damage, false);
		this.speedFactor = speed;
	}

	@Override
	public void update() {
		if (hitWall()) {
			Game.bullets.remove(this);
			return;
		}

		// Fase de salto: dispara relâmpago instantâneo no alvo atual.
		if (currentTarget != null) {
			applyChainDamage();
			Game.bullets.remove(this);
			return;
		}

		x += dx * speedFactor;
		y += dy * speedFactor;

		// Rastro elétrico durante o voo: faíscas caindo atrás da bala.
		if (Game.rand.nextBoolean()) {
			int sparkX = (int) (this.getX() - dx * 3);
			int sparkY = (int) (this.getY() - dy * 3);
			com.traduvertgames.graficos.ParticleSystem.trail(sparkX, sparkY,
					new Color(144, 202, 249));
		}

		for (int i = 0; i < Game.enemies.size(); i++) {
			Entity candidate = Game.enemies.get(i);
			if (!(candidate instanceof Enemy)) {
				continue;
			}
			Enemy enemy = (Enemy) candidate;
			if (hitEnemies.contains(enemy)) {
				continue;
			}
			if (Entity.isColliding(this, enemy)) {
				hitEnemies.add(enemy);
				
					enemy.takeDamageDirect(getDamage());
					chainFactor = 0.7;
					currentTarget = findNextTarget(enemy);
					// Flash de impacto no ponto de colisão.
					impactFlashFrames = 6;
					flashX = enemy.getX() + enemy.mwidth / 2.0;
					flashY = enemy.getY() + enemy.mheight / 2.0;
					return;
			}
		}

		// Saiu do mapa ou percorreu o alcance sem atingir nada.
		if (x < 0 || y < 0 || x > com.traduvertgames.world.World.WIDTH * 16
				|| y > com.traduvertgames.world.World.HEIGHT * 16) {
			Game.bullets.remove(this);
		}
	}

	private Enemy findNextTarget(Enemy source) {
		if (jumps >= MAX_CHAIN_JUMPS) {
			return null;
		}
		Enemy best = null;
		double bestDistance = CHAIN_RANGE;
		double sourceX = source.getX() + source.mwidth / 2.0;
		double sourceY = source.getY() + source.mheight / 2.0;
		for (int i = 0; i < Game.enemies.size(); i++) {
			Entity candidate = Game.enemies.get(i);
			if (!(candidate instanceof Enemy)) {
				continue;
			}
			Enemy enemy = (Enemy) candidate;
			if (hitEnemies.contains(enemy)) {
				continue;
			}
			double distance = calculateDistance((int) sourceX, (int) sourceY,
					enemy.getX() + enemy.mwidth / 2, enemy.getY() + enemy.mheight / 2);
			if (distance < bestDistance) {
				best = enemy;
				bestDistance = distance;
			}
		}
		return best;
	}

	private void applyChainDamage() {
		while (currentTarget != null && jumps < MAX_CHAIN_JUMPS) {
			Enemy target = currentTarget;
			
			target.takeDamageDirect(getDamage() * chainFactor);
			hitEnemies.add(target);
			chainFactor *= 0.7;
			jumps++;
			currentTarget = findNextTarget(target);
		}
	}

	@Override
	public void render(Graphics g) {
		int renderX = this.getX() - Camera.x;
		int renderY = this.getY() - Camera.y;
		Color core = new Color(144, 202, 249);
		Color edge = new Color(63, 81, 181);
		// Núcleo branco-azulado com halo.
		g.setColor(edge);
		g.fillRect(renderX - 2, renderY - 2, width + 4, height + 4);
		g.setColor(core);
		g.fillRect(renderX, renderY, width, height);
		g.setColor(Color.WHITE);
		g.fillRect(renderX + 2, renderY + 2, Math.max(1, width - 4), Math.max(1, height - 4));
		// Traço do relâmpago da cadeia até o alvo atual (mais segmentos para
		// o efeito zigzag elétrico).
		if (currentTarget != null) {
			double fromX = x + width / 2.0;
			double fromY = y + height / 2.0;
			double toX = currentTarget.getX() + currentTarget.mwidth / 2.0;
			double toY = currentTarget.getY() + currentTarget.mheight / 2.0;
			// Halo azul externo (desenhado em linha grossa via múltiplos traços).
			g.setColor(edge);
			for (int k = 0; k < 3; k++) {
				drawLightningBolt(g, fromX, fromY, toX, toY, 5);
			}
		}
		// Flash de impacto nos alvos recém-atendidos da cadeia.
		if (impactFlashFrames > 0) {
			impactFlashFrames--;
			int radius = 14 - impactFlashFrames * 2;
			if (radius > 0) {
				g.setColor(new Color(144, 202, 249, 180));
				g.fillOval((int) (flashX - Camera.x) - radius, (int) (flashY - Camera.y) - radius,
						radius * 2, radius * 2);
				g.setColor(Color.WHITE);
				g.fillOval((int) (flashX - Camera.x) - radius / 3, (int) (flashY - Camera.y) - radius / 3,
						Math.max(2, radius * 2 / 3), Math.max(2, radius * 2 / 3));
			}
		}
	}

	private void drawLightningBolt(Graphics g, double fromX, double fromY, double toX, double toY, int segments) {
		double distance = calculateDistance((int) fromX, (int) fromY, (int) toX, (int) toY);
		if (distance <= 0) {
			return;
		}
		double step = distance / segments;
		double dirX = (toX - fromX) / distance;
		double dirY = (toY - fromY) / distance;
		double perpendicularX = -dirY;
		double perpendicularY = dirX;
		double cursorX = fromX;
		double cursorY = fromY;
		for (int i = 0; i < segments; i++) {
			double nextX = fromX + dirX * step * (i + 1);
			double nextY = fromY + dirY * step * (i + 1);
			if (i < segments - 1) {
				double jitter = (Game.rand.nextDouble() - 0.5) * 10.0;
				nextX += perpendicularX * jitter;
				nextY += perpendicularY * jitter;
			}
			g.drawLine((int) (cursorX - Camera.x), (int) (cursorY - Camera.y),
					(int) (nextX - Camera.x), (int) (nextY - Camera.y));
			cursorX = nextX;
			cursorY = nextY;
		}
	}

	/** Relâmpago consome a si mesmo no primeiro impacto e continua em saltos. */
	@Override
	public boolean isPersistent() {
		return true;
	}
}
