package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;

/**
 * Lâmina arcana disparada pelo Bumerangue Arcano: percorre o alcance máximo,
 * inverte o sentido e retorna ao jogador, causando dano na ida e na volta.
 * Ao retornar ao player, a energia da arma é parcialmente devolvida.
 */
public class BoomerangProjectile extends BulletShoot {

	private static int globalFrame = 0;

	private final double speedFactor;
	private boolean returning;

	public BoomerangProjectile(int x, int y, int size, double dx, double dy, double speed, double damage) {
		super(x, y, size, size, null, dx, dy, speed, damage, false);
		this.speedFactor = speed;
		this.returning = false;
	}

	@Override
	public void update() {
		globalFrame++;
		if (hitWall()) {
			Game.bullets.remove(this);
			return;
		}
		if (returning) {
			Player player = Game.player;
			if (player == null) {
				Game.bullets.remove(this);
				return;
			}
			double targetX = player.getX() + 8;
			double targetY = player.getY() + 8;
			double distance = calculateDistance((int) x, (int) y, (int) targetX, (int) targetY);
			if (distance <= 6) {
				// Colheu: devolve parte da energia da arma e some.
				WeaponType current = player.getCurrentWeaponType();
				if (current == WeaponType.BOOMERANG_ARCANO) {
					double recharge = Math.min(current.getPickupRecharge(),
							current.getMaxDurability() - getWeaponEnergy(player));
					player.addWeaponEnergy(recharge);
				}
				Game.bullets.remove(this);
				return;
			}
			double velocity = Math.min(3.6, Math.max(1.6, distance / 40.0));
			x += (targetX - x) / distance * velocity;
			y += (targetY - y) / distance * velocity;
			return;
		}

		x += dx * speedFactor;
		y += dy * speedFactor;
		// Rastro arcano durante o voo: partículas ciano caindo atrás da lâmina.
		if (globalFrame % 2 == 0) {
			com.traduvertgames.graficos.ParticleSystem.trail(
					(int) (this.getX() - dx * 4), (int) (this.getY() - dy * 4),
					new Color(0, 200, 230));
		}
		// Alcance de ida: ~30 frames de velocidade plena.
		if (globalFrame % 30 == 0) {
			returning = true;
		}
	}

	@Override
	public void render(Graphics g) {
		int renderX = this.getX() - Camera.x;
		int renderY = this.getY() - Camera.y;
		// Lâmina em rotação simulada: cruz que gira com o frame, com núcleo
		// branco na ida e ciano brilhante na volta (feedback de fase).
		boolean spinning = (globalFrame / 3) % 2 == 0;
		Color blade = returning ? new Color(120, 240, 255) : new Color(0, 200, 230);
		if (spinning) {
			g.setColor(Color.WHITE);
			g.fillRect(renderX + 2, renderY, width - 4, height);
			g.fillRect(renderX, renderY + 2, width, height - 4);
		} else {
			g.setColor(blade);
			g.fillRect(renderX, renderY, width, height);
		}
		g.setColor(returning ? new Color(0, 160, 180) : new Color(0, 90, 110));
		g.fillRect(renderX + 4, renderY + 4, width - 8, height - 8);
	}

	private static double getWeaponEnergy(Player player) {
		WeaponType current = player.getCurrentWeaponType();
		if (current == null) {
			return 0;
		}
		return Math.min(player.getWeaponEnergyFor(current), current.getMaxDurability());
	}

	/** Colide sem ser consumida no primeiro impacto (ida e volta). */
	@Override
	public boolean isPersistent() {
		return true;
	}
}
