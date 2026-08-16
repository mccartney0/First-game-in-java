package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Player;
import com.traduvertgames.world.World;

/**
 * Gerencia ondas progressivas de inimigos: em vez de aparecer todos de uma
 * vez, os inimigos nascem em lotes ao longo do tempo, aumentando a tensão.
 * Também permite o modo Arena Infinita após zerar o jogo.
 */
public final class WaveManager {

	private static final int SPAWN_INTERVAL_FRAMES = 90;
	private static final int SPAWN_COUNT_PER_WAVE = 3;
	private static final int MAX_ENEMIES_ON_MAP = 12;

	private static final List<int[]> spawnQueue = new ArrayList<int[]>();
	private static int waveIndex = 0;
	private static int spawnTimer = 0;
	private static boolean announcing = false;
	private static int announceTimer = 0;
	private static String announceText = "";
	private static Color announceColor = Color.WHITE;
	private static int queuedTotal = 0;
	private static int spawnedCount = 0;

	private static int arenaWave = 0;
	private static int arenaTimer = 0;
	private static boolean arenaMode = false;

	private WaveManager() {
	}

	/** Inicia as ondas para os inimigos já registrados em fila. */
	public static void startWaves() {
		waveIndex = 0;
		spawnTimer = 0;
		spawnedCount = 0;
	}

	/** Enfileira uma posição de spawn. */
	public static void queueSpawn(int x, int y) {
		spawnQueue.add(new int[] { x, y });
		queuedTotal++;
	}

	/** @return total de inimigos ainda não nascidos (fila + anunciados). */
	public static int pendingCount() {
		return spawnQueue.size() - waveIndex;
	}

	public static boolean hasPending() {
		return waveIndex < spawnQueue.size();
	}

	public static boolean isArenaMode() {
		return arenaMode;
	}

	public static void startArena() {
		arenaMode = true;
		arenaWave = 1;
		arenaTimer = 180;
		announce("ARENA INFINITA", new Color(255, 87, 34));
	}

	public static void stopArena() {
		arenaMode = false;
		arenaWave = 0;
		arenaTimer = 0;
	}

	public static int getArenaWave() {
		return arenaWave;
	}

	/** Número atual da onda no modo sobrevivência (alias de getArenaWave). */
	public static int getCurrentWaveNumber() {
		return arenaWave;
	}

	public static void reset() {
		spawnQueue.clear();
		waveIndex = 0;
		spawnTimer = 0;
		announcing = false;
		announceTimer = 0;
		queuedTotal = 0;
		spawnedCount = 0;
		arenaMode = false;
		arenaWave = 0;
		arenaTimer = 0;
	}

	public static void update() {
		if (announcing) {
			announceTimer--;
			if (announceTimer <= 0) {
				announcing = false;
			}
		}

		if (arenaMode) {
			updateArena();
			return;
		}

		if (spawnTimer > 0) {
			spawnTimer--;
			return;
		}
		if (waveIndex >= spawnQueue.size()) {
			return;
		}

		int toSpawn = Math.min(SPAWN_COUNT_PER_WAVE, spawnQueue.size() - waveIndex);
		int spawnedThisWave = 0;
		for (int i = 0; i < toSpawn; i++) {
			if (Game.enemies.size() >= MAX_ENEMIES_ON_MAP) {
				spawnTimer = 60;
				break;
			}
			int[] position = spawnQueue.get(waveIndex);
			waveIndex++;
			spawnEnemyAt(position[0], position[1]);
			spawnedThisWave++;
		}
		if (spawnedThisWave > 0) {
			spawnedCount += spawnedThisWave;
			spawnTimer = SPAWN_INTERVAL_FRAMES;
		}
	}

	private static void updateArena() {
		if (arenaTimer > 0) {
			arenaTimer--;
			return;
		}
		if (Game.enemies.size() == 0) {
			arenaWave++;
			announce("Onda " + arenaWave, new Color(255, 193, 7));
			arenaTimer = 180;
		}
		if (arenaTimer <= 0 && Game.enemies.size() < MAX_ENEMIES_ON_MAP) {
			spawnArenaEnemies();
			arenaTimer = 240;
		}
	}

	private static void spawnArenaEnemies() {
		int count = 2 + arenaWave / 2;
		for (int i = 0; i < count; i++) {
			int[] spot = findSpawnSpot();
			if (spot == null) {
				return;
			}
			Enemy enemy = Enemy.spawnRandomVariant(spot[0], spot[1]);
			Game.entities.add(enemy);
			Game.enemies.add(enemy);
		}
	}

	public static void spawnEnemyAt(int x, int y) {
		Enemy enemy = Enemy.spawnRandomVariant(x, y);
		Game.entities.add(enemy);
		Game.enemies.add(enemy);
		// Spawn em lote avança imediatamente: os inimigos começam patrulhando.
	}

	private static int[] findSpawnSpot() {
		// Tenta encontrar um tile de chão livre perto de bordas do mapa.
		for (int attempt = 0; attempt < 20; attempt++) {
			int tileX = Game.rand.nextInt(World.WIDTH);
			int tileY = Game.rand.nextInt(World.HEIGHT);
			if (!World.isFree(tileX * 16 + 8, tileY * 16 + 8, 0)) {
				continue;
			}
			double distanceToPlayer = Math.hypot(tileX * 16 - Player.life, tileY * 16 - Player.life);
			double distance = Math.hypot(tileX * 16 - Game.player.getX(), tileY * 16 - Game.player.getY());
			if (distance > 120) {
				return new int[] { tileX * 16, tileY * 16 };
			}
		}
		return null;
	}

	private static void announce(String text, Color color) {
		announceText = text;
		announceColor = color;
		announcing = true;
		announceTimer = 90;
		if (text.startsWith("Onda ") || text.startsWith("ARENA")) {
			SoundManager.play(SoundManager.Event.WAVE);
		}
	}

	public static void render(Graphics g) {
		if (!announcing) {
			return;
		}
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;
		int alpha = announceTimer > 75 ? (90 - announceTimer) * 12 : (announceTimer > 15 ? 180 : announceTimer * 12);
		g.setColor(new Color(announceColor.getRed(), announceColor.getGreen(), announceColor.getBlue(),
				Math.max(0, Math.min(200, alpha))));
		Font font = new Font("arial", Font.BOLD, 30);
		g.setFont(font);
		int textWidth = g.getFontMetrics().stringWidth(announceText);
		g.drawString(announceText, (screenWidth - textWidth) / 2, screenHeight / 2 - 40);
	}
}
