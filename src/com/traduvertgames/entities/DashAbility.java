package com.traduvertgames.entities;

import java.awt.Color;

import com.traduvertgames.graficos.ParticleSystem;
import com.traduvertgames.main.Game;

/**
 * Dash / esquiva (tecla Shift): o piloto se desloca rapidamente por uma curta
 * distância na direção do último movimento, ficando invulnerável durante o
 * deslocamento. Recarga de 2,5 segundos.
 */
public final class DashAbility {

	public static final int DASH_DURATION = 8;
	public static final double DASH_SPEED = 5.0;
	public static final int COOLDOWN_FRAMES = 2 * 60 + 30;

	private static int activeFrames = 0;
	private static double dashDx = 0;
	private static double dashDy = 0;
	private static int cooldown = 0;

	private DashAbility() {
	}

	public static boolean isReady() {
		return cooldown <= 0 && activeFrames <= 0 && Game.player != null;
	}

	public static boolean isDashing() {
		return activeFrames > 0;
	}

	public static double getReadyPercentage() {
		if (cooldown <= 0) {
			return 1;
		}
		return 1 - ((double) cooldown / COOLDOWN_FRAMES);
	}

	public static boolean perform() {
		if (!isReady()) {
			return false;
		}
		Player player = Game.player;
		if (player == null) {
			return false;
		}

		// Direção definida pelo último movimento; se parado, usa a direção frontal.
		if (player.right) {
			dashDx = DASH_SPEED;
			dashDy = 0;
		} else if (player.left) {
			dashDx = -DASH_SPEED;
			dashDy = 0;
		} else if (player.up) {
			dashDx = 0;
			dashDy = -DASH_SPEED;
		} else if (player.down) {
			dashDx = 0;
			dashDy = DASH_SPEED;
		} else {
			switch (player.dir) {
			case 0:
				dashDx = DASH_SPEED;
				break;
			case 1:
				dashDx = -DASH_SPEED;
				break;
			case 2:
				dashDy = -DASH_SPEED;
				break;
			case 3:
				dashDy = DASH_SPEED;
				break;
			default:
				dashDx = DASH_SPEED;
				break;
			}
		}

		activeFrames = DASH_DURATION;
		cooldown = COOLDOWN_FRAMES;
		ParticleSystem.trail(player.getX() + 8, player.getY() + 8, new Color(100, 200, 255));
		return true;
	}

	public static void update() {
		if (cooldown > 0) {
			cooldown--;
		}
		if (activeFrames > 0) {
			Game playerHost = Game.getInstance();
			Player player = Game.player;
			if (playerHost != null && player != null && com.traduvertgames.world.World.isFree(
					(int) (player.getX() + dashDx), (int) (player.getY() + dashDy), Player.z)) {
				player.x += dashDx;
				player.y += dashDy;
			}
			activeFrames--;
			if (activeFrames == 0) {
				dashDx = 0;
				dashDy = 0;
			}
		}
	}

	public static void reset() {
		activeFrames = 0;
		cooldown = 0;
		dashDx = 0;
		dashDy = 0;
	}
}
