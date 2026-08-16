package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;

/**
 * Sistema de partículas para feedback visual: morte de inimigos, destruição
 * de paredes, dash, coleta de itens e impacto de projéteis.
 */
public final class ParticleSystem {

	private static final List<Particle> particles = new ArrayList<Particle>();

	private ParticleSystem() {
	}

	public static void burst(int x, int y, Color color, int count, double speed) {
		for (int i = 0; i < count; i++) {
			double angle = Game.rand.nextDouble() * Math.PI * 2;
			double velocity = speed * (0.4 + Game.rand.nextDouble() * 0.6);
			double dx = Math.cos(angle) * velocity;
			double dy = Math.sin(angle) * velocity;
			int life = 15 + Game.rand.nextInt(20);
			particles.add(new Particle(x, y, dx, dy, color, life));
		}
	}

	public static void spark(int x, int y, Color color) {
		burst(x, y, color, 6, 1.4);
	}

	public static void explode(int x, int y, Color color) {
		burst(x, y, color, 14, 2.4);
	}

	/** Partículas em anel radial: usadas pelo Shield Bot ao regenerar escudo. */
	public static void pulse(int x, int y, Color color) {
		for (int i = 0; i < 8; i++) {
			double angle = Math.PI * 2 * i / 8.0;
			double dx = Math.cos(angle) * 1.2;
			double dy = Math.sin(angle) * 1.2;
			particles.add(new Particle(x, y, dx, dy, color, 12));
		}
	}

	public static void trail(int x, int y, Color color) {
		if (Game.rand.nextInt(3) != 0) {
			return;
		}
		double dx = (Game.rand.nextDouble() - 0.5) * 0.8;
		double dy = (Game.rand.nextDouble() - 0.5) * 0.8;
		particles.add(new Particle(x, y, dx, dy, color, 10));
	}

	public static void update() {
		Iterator<Particle> iterator = particles.iterator();
		while (iterator.hasNext()) {
			Particle particle = iterator.next();
			particle.life--;
			particle.x += particle.dx;
			particle.y += particle.dy;
			particle.dx *= 0.92;
			particle.dy *= 0.92;
			if (particle.life <= 0) {
				iterator.remove();
			}
		}
	}

	public static void render(Graphics g) {
		for (Particle particle : particles) {
			int fadeAlpha = Math.min(255, Math.max(0, particle.life * 10));
			Color faded = new Color(particle.color.getRed(), particle.color.getGreen(),
					particle.color.getBlue(), fadeAlpha);
			g.setColor(faded);
			int size = Math.max(1, particle.life / 6);
			g.fillRect((int) particle.x - Camera.x, (int) particle.y - Camera.y, size, size);
		}
	}

	public static void clear() {
		particles.clear();
	}

	private static final class Particle {
		double x;
		double y;
		double dx;
		double dy;
		Color color;
		int life;

		Particle(double x, double y, double dx, double dy, Color color, int life) {
			this.x = x;
			this.y = y;
			this.dx = dx;
			this.dy = dy;
			this.color = color;
			this.life = life;
		}
	}
}
