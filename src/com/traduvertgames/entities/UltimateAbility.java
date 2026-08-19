package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.graficos.ParticleSystem;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.world.Camera;

/**
 * Habilidade especial (tecla F): "Overload Radial" — o piloto descarrega uma
 * onda de energia que causa dano massivo a todos os inimigos próximos e
 * destrói projéteis inimigos na área. Gasta 100 de mana e tem 12s de recarga.
 */
public final class UltimateAbility {

	public static final double MANA_COST = 100;
	public static final int COOLDOWN_FRAMES = 12 * 60; // 12 segundos
	public static final int EFFECT_FRAMES = 20;
	public static final int RADIUS = 80;
	public static final double DAMAGE = 20;

	private static int cooldown = 0;
	private static int effectTimer = 0;
	private static int effectX = 0;
	private static int effectY = 0;

	private UltimateAbility() {
	}

	/** @return true se a habilidade está pronta para uso. */
	public static boolean isReady() {
		return cooldown <= 0 && Player.mana >= MANA_COST && Game.player != null;
	}

	public static int getCooldownFrames() {
		return cooldown;
	}

	/** Percentual de recarga pronto (0 = acabando de usar, 1 = pronta). */
	public static double getReadyPercentage() {
		if (cooldown <= 0) {
			return 1;
		}
		return 1 - ((double) cooldown / COOLDOWN_FRAMES);
	}

	/** Tenta ativar a habilidade. Retorna true se foi usada. */
	public static boolean cast() {
		if (!isReady()) {
			return false;
		}
		Game.player.addMana(-MANA_COST);

		int originX = Game.player.getX() + 8;
		int originY = Game.player.getY() + 8;
		effectX = originX;
		effectY = originY;
		effectTimer = EFFECT_FRAMES;
		cooldown = COOLDOWN_FRAMES;

		ParticleSystem.explode(originX, originY, new Color(255, 220, 60));
		SoundManager.play(SoundManager.Event.MAGIC_CAST);

		for (Enemy enemy : Game.enemies) {
			double dx = (enemy.getX() + 8) - originX;
			double dy = (enemy.getY() + 8) - originY;
			double distance = Math.sqrt(dx * dx + dy * dy);
			if (distance <= RADIUS) {
				enemy.takeDamageDirect(DAMAGE);
				ParticleSystem.spark(enemy.getX() + 8, enemy.getY() + 8, new Color(255, 255, 200));
			}
		}

		Game.bullets.removeIf(bullet -> bullet instanceof BulletShoot && ((BulletShoot) bullet).isFromEnemy()
				&& isInsideRadius(bullet, originX, originY));
		return true;
	}

	/** Reinicia a habilidade para uma nova partida. */
	public static void reset() {
		cooldown = 0;
		effectTimer = 0;
		effectX = 0;
		effectY = 0;
	}

	/** Atualiza recarga e efeito visual. */
	public static void update() {
		if (cooldown > 0) {
			cooldown--;
		}
		if (effectTimer > 0) {
			effectTimer--;
		}
	}

	public static void render(Graphics g) {
		if (effectTimer <= 0) {
			return;
		}
		double progress = 1 - ((double) effectTimer / EFFECT_FRAMES);
		int drawRadius = (int) (RADIUS * Math.min(1.2, progress * 1.4));
		int alpha = (int) (220 * (1 - progress));
		g.setColor(new Color(255, 220, 60, alpha));
		g.drawOval(effectX - Camera.x - drawRadius, effectY - Camera.y - drawRadius, drawRadius * 2, drawRadius * 2);
		g.setColor(new Color(255, 240, 150, Math.max(0, alpha - 80)));
		g.drawOval(effectX - Camera.x - drawRadius / 2, effectY - Camera.y - drawRadius / 2, drawRadius, drawRadius);
	}

	/** Dano direto sem depender das colisões de projétil. */
	private static boolean isInsideRadius(BulletShoot bullet, int originX, int originY) {
		int bulletCenterX = bullet.getX() + bullet.getWidth() / 2;
		int bulletCenterY = bullet.getY() + bullet.getHeight() / 2;
		double dx = bulletCenterX - originX;
		double dy = bulletCenterY - originY;
		return Math.sqrt(dx * dx + dy * dy) <= RADIUS;
	}
}
