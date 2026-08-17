package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.entities.FloatingText;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.world.Camera;

/**
 * NPC de escolta usado pelo {@link com.traduvertgames.quest.EscortObjective}:
 * ele se desloca sozinho em direção ao ponto de fuga do setor. O jogador deve
 * proteger o escoltado de inimigos no caminho: se ele for atingido o
 * suficiente, a escolta falha (objetivo reiniciado da fase); se chegar ao
 * destino, a fase é concluída.
 *
 * Diferente do {@link QuestNPC} (parado até o jogador recolhê-lo), o
 * EscortNpc tem vida, animação de caminhada e movimento autônomo lento,
 * parando brevemente quando um inimigo passa muito próximo (medo).
 */
public class EscortNpc extends Entity {

	/** Velocidade de deslocamento em pixels por frame. */
	private static final double SPEED = 0.6;
	/** Pontos de vida do escoltado. */
	private static final int MAX_HP = 3;
	/** Raio em que inimigos próximos assustam o escoltado (pausa de caminhada). */
	private static final double FEAR_RADIUS = 120;
	/** Frames de pausa quando assustado. */
	private static final int FEAR_FRAMES = 90;
	/** Frames de invulnerabilidade após ser atingido. */
	private static final int HIT_INVULN_FRAMES = 45;

	private final int escapeX;
	private final int escapeY;
	private int hp = MAX_HP;
	private int fearTimer = 0;
	private int invulnTimer = 0;
	private boolean arrived = false;
	private boolean arrivedNotified = false;
	private int walkFrame = 0;
	private final double initialX;
	private final double initialY;

	public EscortNpc(int x, int y, int escapeX, int escapeY) {
		super(x, y, 16, 16, null);
		this.initialX = x;
		this.initialY = y;
		this.escapeX = escapeX;
		this.escapeY = escapeY;
		setMask(2, 2, 12, 12);
		QuestManager.registerEscort(this);
	}

	/** @return true quando o escoltado chegou ao ponto de fuga. */
	public boolean hasArrived() {
		return arrived;
	}

	/** @return pontos de vida restantes (1 a MAX_HP). */
	public int getHp() {
		return hp;
	}

	/** X do ponto de fuga (destino da escolta). */
	public int escapeTargetX() {
		return escapeX;
	}

	/** Y do ponto de fuga (destino da escolta). */
	public int escapeTargetY() {
		return escapeY;
	}

	/** @return distância em pixels do escoltado a um ponto qualquer. */
	public double distanceTo(double x, double y) {
		double dx = x - getX();
		double dy = y - getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	/** @return distância total da jornada (do ponto de origem ao destino). */
	public double distanceFromSpawn() {
		double dx = escapeX - initialX;
		double dy = escapeY - initialY;
		return Math.sqrt(dx * dx + dy * dy);
	}

	/** Inimigo ataca o escoltado: -1 HP; a 0 o escoltado é evacuado à força (escolta falha). */
	public void takeHit() {
		if (invulnTimer > 0 || arrived) {
			return;
		}
		invulnTimer = HIT_INVULN_FRAMES;
		hp--;
		if (hp <= 0) {
			QuestManager.escortFailed(this);
		} else {
			FloatingText.show("-1", (int) getX() + 8, (int) getY(),
					new Color(255, 90, 90), 50);
		}
	}

	@Override
	public void update() {
		if (arrived) {
			return;
		}
		if (invulnTimer > 0) {
			invulnTimer--;
		}
		// Medo: inimigos vivos muito próximos fazem o escoltado congelar.
		boolean threatened = false;
		for (int i = 0; i < Game.entities.size(); i++) {
			Entity e = Game.entities.get(i);
			if (!(e instanceof Enemy)) {
				continue;
			}
			double dx = e.getX() - getX();
			double dy = e.getY() - getY();
			if (dx * dx + dy * dy <= FEAR_RADIUS * FEAR_RADIUS) {
				threatened = true;
				break;
			}
		}
		if (threatened) {
			fearTimer = FEAR_FRAMES;
		} else if (fearTimer > 0) {
			fearTimer--;
		}

		if (fearTimer == 0) {
			walkFrame++;
			double dx = escapeX - getX();
			double dy = escapeY - getY();
			double distance = Math.sqrt(dx * dx + dy * dy);
			if (distance < 2) {
				arrived = true;
				if (!arrivedNotified) {
					arrivedNotified = true;
					QuestManager.escortArrived(this);
				}
				return;
			}
			setX((int) (getX() + (dx / distance) * SPEED));
			setY((int) (getY() + (dy / distance) * SPEED));
		}
	}

	@Override
	public void render(Graphics g) {
		int screenX = getX() - Camera.x;
		int screenY = getY() - Camera.y;
		// Piscada de invulnerabilidade.
		if (invulnTimer > 0 && invulnTimer % 6 < 3) {
			return;
		}
		// Sombra e cabeça (mesma silhueta do QuestNPC para consistência visual).
		g.setColor(new Color(30, 30, 30));
		g.fillRect(screenX + 6, screenY + 2, 4, 4);
		g.setColor(new Color(255, 224, 178));
		g.fillOval(screenX + 5, screenY + 5, 6, 6);
		// Corpo com cor de destaque (azul de evacuação) e indicador de vida.
		g.setColor(new Color(40, 120, 220));
		g.fillRoundRect(screenX + 3, screenY + 10, 10, 6, 4, 4);
		// Corações de vida restantes acima da cabeça.
		g.setColor(new Color(255, 90, 90));
		for (int i = 0; i < MAX_HP; i++) {
			int hx = screenX + 2 + i * 4;
			int hy = screenY - 3;
			g.fillRect(hx, hy, 2, 2);
			if (i + 1 < hp) {
				g.fillRect(hx - 2, hy - 2, 2, 2);
				g.fillRect(hx + 2, hy - 2, 2, 2);
			}
		}
	}
}
