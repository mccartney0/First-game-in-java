package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.graficos.ParticleSystem;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;

/**
 * Companion/pet comprável na loja: criatura que orbita o jogador e oferece um
 * suporte passivo conforme o tipo ({@link CompanionType}).
 *
 * Apenas um companion pode estar ativo por vez — comprar outro substitui o
 * anterior (com troca na posição atual do jogador). O companion é permanente:
 * permanece até ser destruído por dano recebido ou substituído pela compra de
 * outro; o tipo e o HP são persistidos no save.
 */
public class Companion extends Entity {

	/** Tipos de companion disponíveis na loja. */
	public enum CompanionType {
		/** Drone batedor: atira automaticamente no inimigo mais próximo. */
		SCOUT,
		/** Drone de escudo: regenera +2 de escudo por segundo. */
		SHIELD_BOT,
		/** Fada curadora: regenera +1 de vida por segundo. */
		FAIRY;
	}

	private static final double ORBIT_RADIUS = 24.0;
	private static final double ORBIT_SPEED = 0.06;
	private static final int SCOUT_FIRE_INTERVAL_FRAMES = 20;
	private static final double BASE_HP = 40.0;
	/** Frames entre regenerações de suporte (1s a 60 FPS). */
	private static final int SUPPORT_INTERVAL_FRAMES = 60;

	private CompanionType type;
	private double orbitAngle;
	private int fireCooldown;
	private int supportCooldown;
	private double hp;

	private static Companion active;

	private Companion(CompanionType type) {
		super(0, 0, 12, 12, null);
		this.type = type;
		this.orbitAngle = 0;
		this.fireCooldown = 0;
		this.supportCooldown = 0;
		this.hp = BASE_HP;
		active = this;
	}

	/** @return companion ativo em jogo ou null se não houver nenhum. */
	public static Companion getActive() {
		return active;
	}

	/** Remove o companion ativo (troca por outro ou fim de jogo). */
	public static void clear() {
		Companion current = active;
		active = null;
		if (current != null) {
			Game.entities.remove(current);
		}
	}

	/**
	 * Cria o companion do tipo informado na posição do jogador, substituindo
	 * qualquer outro ativo. O HP pode ser restaurado do save.
	 */
	public static void spawn(CompanionType type, double savedHp) {
		clear();
		if (Game.player == null) {
			return;
		}
		Companion companion = new Companion(type);
		companion.hp = savedHp > 0 ? Math.min(savedHp, BASE_HP) : BASE_HP;
		companion.x = Game.player.getX();
		companion.y = Game.player.getY();
		Game.entities.add(companion);
		com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.PICKUP);
	}

	/** @return HP atual do companion (persistido no save). */
	public double getHp() {
		return hp;
	}

	/** @return tipo deste companion. */
	public CompanionType getType() {
		return type;
	}

	@Override
	public void update() {
		Player player = Game.player;
		if (player == null || hp <= 0) {
			if (active == this) {
				Game.entities.remove(this);
				active = null;
				ParticleSystem.explode((int) (this.x + width / 2.0), (int) (this.y + height / 2.0),
						colorForType());
			}
			return;
		}
		if (active != this) {
			Game.entities.remove(this);
			return;
		}

		// Órbita suave em torno do jogador.
		orbitAngle += ORBIT_SPEED;
		double targetX = player.getX() + 8 + Math.cos(orbitAngle) * ORBIT_RADIUS - width / 2.0;
		double targetY = player.getY() + 8 + Math.sin(orbitAngle) * ORBIT_RADIUS - height / 2.0;
		x += (targetX - x) * 0.2;
		y += (targetY - y) * 0.2;

		if (remainingFrames() % 3 == 0) {
			ParticleSystem.trail((int) (x + width / 2.0), (int) (y + height / 2.0), colorForType());
		}

		switch (type) {
		case SCOUT:
			updateScout();
			break;
		case SHIELD_BOT:
			updateShieldBot();
			break;
		case FAIRY:
			updateFairy();
			break;
		default:
			break;
		}

		// Dano de contato com inimigos próximos (comum a todos os tipos).
		for (int i = 0; i < Game.enemies.size(); i++) {
			Entity candidate = Game.enemies.get(i);
			if (candidate instanceof Enemy && Entity.isColliding(this, candidate)) {
				Enemy enemy = (Enemy) candidate;
				enemy.takeDamageDirect(2.0);
				hp = Math.max(0.0, hp - 5.0);
				break;
			}
		}
	}

	private int remainingFrames() {
		return (int) (hp * 100);
	}

	private void updateScout() {
		if (fireCooldown > 0) {
			fireCooldown--;
			return;
		}
		Enemy nearest = null;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < Game.enemies.size(); i++) {
			Entity candidate = Game.enemies.get(i);
			if (!(candidate instanceof Enemy)) {
				continue;
			}
			Enemy enemy = (Enemy) candidate;
				double distance = Math.hypot((enemy.getX() + enemy.mwidth / 2.0) - (x + width / 2.0),
						(enemy.getY() + enemy.mheight / 2.0) - (y + height / 2.0));
			if (distance < bestDistance && distance <= 300) {
				bestDistance = distance;
				nearest = enemy;
			}
		}
		if (nearest == null) {
			return;
		}
		double originX = x + width / 2.0;
		double originY = y + height / 2.0;
		double angle = Math.atan2((nearest.getY() + nearest.mheight / 2.0) - originY,
				(nearest.getX() + nearest.mwidth / 2.0) - originX);
		BulletShoot bullet = new BulletShoot((int) originX, (int) originY, 4, 4, null,
				Math.cos(angle), Math.sin(angle), 5.0, 3.5, false, colorForType());
		bullet.setMask(0, 0, 4, 4);
		Game.bullets.add(bullet);
		fireCooldown = SCOUT_FIRE_INTERVAL_FRAMES;
	}

	private void updateShieldBot() {
		if (supportCooldown > 0) {
			supportCooldown--;
			return;
		}
		if (Player.shield < Player.maxShield) {
			Player.shield = Math.min(Player.shield + 2, Player.maxShield);
		}
		supportCooldown = SUPPORT_INTERVAL_FRAMES;
	}

	private void updateFairy() {
		if (supportCooldown > 0) {
			supportCooldown--;
			return;
		}
		if (Player.life < Player.maxLife) {
			Player.life = Math.min(Player.life + 1, Player.maxLife);
		}
		supportCooldown = SUPPORT_INTERVAL_FRAMES;
	}

	private Color colorForType() {
		switch (type) {
		case SHIELD_BOT:
			return new Color(90, 160, 255);
		case FAIRY:
			return new Color(255, 130, 220);
		case SCOUT:
		default:
			return new Color(255, 203, 5);
		}
	}

	@Override
	public void render(Graphics g) {
		int renderX = this.getX() - Camera.x;
		int renderY = this.getY() - Camera.y;
		Color color = colorForType();
		// Aura de suporte.
		g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
		g.fillOval(renderX - 5, renderY - 5, width + 10, height + 10);
		// Corpo (círculo colorido).
		g.setColor(color);
		g.fillOval(renderX + 2, renderY + 2, width - 4, height - 4);
		// Ícone interno conforme o tipo.
		switch (type) {
		case SCOUT:
			g.setColor(Color.WHITE);
			g.fillRect(renderX + 4, renderY + 5, 4, 2);
			break;
		case SHIELD_BOT:
			g.setColor(Color.WHITE);
			g.fillOval(renderX + 4, renderY + 3, 5, 5);
			g.setColor(new Color(20, 60, 140));
			g.fillRect(renderX + 5, renderY + 6, 3, 2);
			break;
		case FAIRY:
			g.setColor(new Color(255, 240, 120));
			g.fillOval(renderX + 4, renderY + 2, 3, 3);
			g.fillOval(renderX + 6, renderY + 3, 2, 2);
			break;
		default:
			break;
		}
		// Barra de HP do companion.
		double percent = Math.max(0, Math.min(1, hp / BASE_HP));
		g.setColor(Color.DARK_GRAY);
		g.fillRect(renderX, renderY - 6, width, 3);
		g.setColor(percent > 0.3 ? new Color(76, 175, 80) : new Color(244, 67, 54));
		g.fillRect(renderX, renderY - 6, (int) (width * percent), 3);
	}

	/** Aplica dano de projéteis inimigos ao companion. */
	public void applyDamage(double amount) {
		this.hp -= amount;
	}
}
