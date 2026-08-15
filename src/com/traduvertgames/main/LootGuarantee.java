package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.EnergyCell;
import com.traduvertgames.entities.NanoMedkit;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.ShieldOrb;

/**
 * Loot garantido: cada variante de inimigo sempre deixa cair um item ligado à
 * sua especialidade, além do loot aleatório já existente.
 */
public final class LootGuarantee {

	/** Notificação visual de loot (exibe quando a variante é elite/boss). */
	private static String lastNotice = "";
	private static int noticeTimer = 0;

	private LootGuarantee() {
	}

	/** Deve ser chamado em Enemy.destroySelf antes de remover o inimigo. */
	public static void dropForVariant(Enemy enemy) {
		if (enemy == null) {
			return;
		}
		int spawnX = enemy.getX();
		int spawnY = enemy.getY();
		switch (enemy.getVariant()) {
		case SENTINEL:
			// Sentinel reforça escudo do piloto.
			Game.entities.add(new ShieldOrb(spawnX, spawnY));
			notice("Sentinela: escudo recarregado!");
			break;
		case WARDEN:
			// Warden libera energia para a arma do piloto.
			Game.entities.add(new EnergyCell(spawnX, spawnY));
			Game.entities.add(new EnergyCell(spawnX + 8, spawnY));
			notice("Warden: energia recuperada!");
			break;
		case RAVAGER:
			// Ravager é agressivo: retribui com cura.
			Game.entities.add(new NanoMedkit(spawnX, spawnY));
			notice("Ravager: nanomedkit liberado!");
			break;
		case TELEPORTER:
			// Teleporter reabastece mana.
			Player.mana += 40;
			if (Player.mana > Player.maxMana) {
				Player.mana = Player.maxMana;
			}
			notice("Teleportador: mana recuperada!");
			break;
		case ARTILLERY:
			Game.entities.add(new EnergyCell(spawnX, spawnY));
			notice("Artilharia: munição recuperada!");
			break;
		case WARBRINGER:
		case OVERSEER:
			// Bosses garantem cura completa e bônus.
			Player.life = Player.maxLife;
			Player.shield = Player.maxShield;
			Player.mana = Player.maxMana;
			notice("Chefe eliminado: recursos restaurados!");
			break;
		default:
			break;
		}
	}

	private static void notice(String text) {
		lastNotice = text;
		noticeTimer = 150;
	}

	/** Reinicia o aviso para uma nova partida. */
	public static void reset() {
		lastNotice = "";
		noticeTimer = 0;
	}

	public static void update() {
		if (noticeTimer > 0) {
			noticeTimer--;
		}
	}

	public static void render(Graphics g) {
		if (noticeTimer <= 0 || lastNotice.isEmpty()) {
			return;
		}
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;
		int alpha = noticeTimer > 120 ? (150 - noticeTimer) * 12 : noticeTimer > 30 ? 200 : noticeTimer * 6;
		g.setColor(new Color(255, 225, 100, Math.max(0, Math.min(200, alpha))));
		g.setFont(new Font("arial", Font.BOLD, 16));
		int width = g.getFontMetrics().stringWidth(lastNotice);
		g.drawString(lastNotice, (screenWidth - width) / 2, screenHeight - 70);
	}
}
