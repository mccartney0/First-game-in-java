package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.Player;
import com.traduvertgames.world.Camera;
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
	private static final List<SurvivalStageDefinition> SURVIVAL_STAGES = SurvivalStageDefinition.defaultStages();
	private static int arenaThreatBudget = 0;
	private static int arenaThreatRemaining = 0;
	private static int specialRuleTimer = 0;

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

	/** Placar de ondas sobrevividas do modo sobrevivência (por partida).
	 *  Lido/gravado por {@link SaveManager} junto com o slot de save. */
	private static int survivalRecord = 0;
	/** Ondas concluídas na partida atual (usado para placar e drops). */
	private static int wavesSurvived = 0;

	/** True quando um chefe do modo infinito foi derrotado: o próximo ciclo
	 *  solta um respiro garantido (vida + escudo) para premiar a vitória. */
	private static boolean bossDefeated = false;

	private WaveManager() {
	}

	/** Inicia as ondas para os inimigos já registrados em fila. */
	public static void startWaves() {
		waveIndex = 0;
		spawnTimer = 0;
		spawnedCount = 0;
	}

	/**
	 * Gera um lote dedicado para objetivos de resistência fora da arena.
	 * Mapas sem fila de spawn própria ainda recebem combate periódico.
	 */
	public static int spawnResistanceWave(int requestedCount) {
		if (Game.player == null || World.WIDTH <= 0 || World.HEIGHT <= 0) {
			return 0;
		}
		int count = Math.max(0, requestedCount);
		int spawned = 0;
		for (int i = 0; i < count && Game.enemies.size() < MAX_ENEMIES_ON_MAP; i++) {
			int[] spot = findSpawnSpot();
			if (spot == null) {
				break;
			}
			Enemy enemy = Enemy.spawnRandomVariant(spot[0], spot[1]);
			Game.entities.add(enemy);
			Game.enemies.add(enemy);
			spawned++;
		}
		if (spawned > 0) {
			announce("INVASORES APROXIMANDO-SE", new Color(255, 152, 0));
		}
		return spawned;
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
		wavesSurvived = 0;
		arenaThreatBudget = 0;
		arenaThreatRemaining = 0;
		specialRuleTimer = 0;
		// A arena começa com um período de preparação; a primeira área vazia
		// não conta como uma onda concluída nem concede recompensa.
		waveClearedAnnounced = true;
		announce("FASE " + getSurvivalPhase() + " — " + getSurvivalPhaseName(), new Color(255, 87, 34));
	}

	/** Registra uma onda concluída: atualiza placar, drops e escalada. */
	public static void onWaveCleared() {
		wavesSurvived++;
		if (wavesSurvived > survivalRecord) {
			survivalRecord = wavesSurvived;
		}
		// Chefe a cada 5 ondas concluídas.
		if (wavesSurvived > 0 && wavesSurvived % 5 == 0) {
			int[] spot = findSpawnSpot();
			if (spot != null) {
				Enemy boss = Enemy.spawnArenaBoss(spot[0], spot[1], wavesSurvived);
				Game.entities.add(boss);
				Game.enemies.add(boss);
			}
			announce("CHEFE APROXIMANDO-SE — Onda " + wavesSurvived, new Color(233, 30, 99));
			SoundManager.play(SoundManager.Event.BOSS_ALERT);
			// Rodada 20: o alerta de chefe ganha mais duração para não passar
			// despercebido no meio do combate.
			announceTimer = Math.max(announceTimer, 150);
		}
		// Drop de respiro a cada 3 ondas concluídas. Quando um chefe do modo
		// infinito é derrotado, um respiro garantido é solto no próximo ciclo
		// como recompensa pela vitória.
		if (wavesSurvived > 0 && wavesSurvived % 3 == 0) {
			dropBreather();
		}
		if (bossDefeated) {
			bossDefeated = false;
			dropBreather();
			announce("CHEFE DERROTADO — SUPRIMENTOS!", new Color(255, 214, 0));
		}
		// Rodada 20: recompensa de pontuação por onda sobrevivida — incentiva
		// continuar jogando o modo infinito e converte em score/loja.
		if (wavesSurvived > 0) {
			Game.addScore(20 + wavesSurvived * 2);
		}
	}

	/** Soltura um LifePack e um NanoMedkit perto do jogador como recompensa. */
	private static void dropBreather() {
		int px = (int) Game.player.getX();
		int py = (int) Game.player.getY();
		Game.entities.add(new com.traduvertgames.entities.LifePack(px + 24, py + 12, 16, 16,
				com.traduvertgames.entities.Entity.LIFEPACK_EN));
		Game.entities.add(new com.traduvertgames.entities.NanoMedkit(px - 24, py + 12));
		announce("SUPRIMENTOS!", new Color(102, 187, 106));
		SoundManager.play(SoundManager.Event.PICKUP);
	}

	public static int getSurvivalRecord() {
		return survivalRecord;
	}

	public static void setSurvivalRecord(int value) {
		survivalRecord = Math.max(0, value);
	}

	/** Marca a derrota de um chefe do modo infinito (respiro garantido no
	 *  próximo ciclo). Chamado pelo jogo quando o boss da arena morre. */
	public static void onArenaBossDefeated() {
		if (arenaMode) {
			bossDefeated = true;
		}
	}

	public static int getWavesSurvived() {
		return wavesSurvived;
	}

	public static void stopArena() {
		arenaMode = false;
		arenaWave = 0;
		arenaTimer = 0;
		waveClearedAnnounced = false;
	}

	public static int getArenaWave() {
		return arenaWave;
	}

	/** Número atual da onda no modo sobrevivência (alias de getArenaWave). */
	public static int getCurrentWaveNumber() {
		return arenaWave;
	}

	public static int getSurvivalPhase() {
		Game game = Game.getInstance();
		return Math.max(1, game != null ? game.getLevelPlus() : 1);
	}

	public static SurvivalStageDefinition getSurvivalStageDefinition() {
		int index = Math.min(SURVIVAL_STAGES.size() - 1, getSurvivalPhase() - 1);
		return SURVIVAL_STAGES.get(index);
	}

	public static List<SurvivalStageDefinition> getSurvivalStages() {
		return java.util.Collections.unmodifiableList(SURVIVAL_STAGES);
	}

	public static String getSurvivalPhaseName() {
		return getSurvivalStageDefinition().getName();
	}

	public static String getSurvivalPhaseDescription() {
		return getSurvivalStageDefinition().getDescription();
	}

	public static int getThreatBudget() {
		return arenaThreatBudget;
	}

	public static int getThreatRemaining() {
		return arenaThreatRemaining;
	}

	public static double getThreatProgress() {
		if (arenaThreatBudget <= 0) {
			return 0.0;
		}
		return Math.max(0.0, Math.min(1.0,
				(arenaThreatBudget - arenaThreatRemaining) / (double) arenaThreatBudget));
	}

	public static String getSurvivalSummary() {
		return "Fase " + getSurvivalPhase() + " — " + getSurvivalPhaseName()
				+ " | Onda " + getArenaWave();
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
		wavesSurvived = 0;
		bossDefeated = false;
		arenaThreatBudget = 0;
		arenaThreatRemaining = 0;
		specialRuleTimer = 0;
		waveClearedAnnounced = false;
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
		updateSpecialRule();
		if (arenaTimer > 0) {
			arenaTimer--;
			return;
		}
		boolean waveCleared = Game.enemies.size() == 0;
		if (waveCleared && !waveClearedAnnounced) {
			waveClearedAnnounced = true;
			onWaveCleared();
			arenaWave++;
			announce("Onda " + arenaWave, new Color(255, 193, 7));
			arenaTimer = arenaWaveInterval();
		}
		if (arenaTimer <= 0 && Game.enemies.size() < getSurvivalStageDefinition().getMaxEnemies()) {
			int spawned = spawnArenaEnemies();
			arenaTimer = spawned > 0 ? arenaSpawnInterval() : 30;
		}
	}

	/** Intervalo de respiro entre ondas: as 5 primeiras são mais longas para o
	 *  jogador se adaptar; a partir da 6ª encurta gradualmente até o piso. */
	private static int arenaWaveInterval() {
		SurvivalStageDefinition stage = getSurvivalStageDefinition();
		return Math.max(120, stage.getWaveIntervalFrames() - Math.max(0, arenaWave - 1) * 3);
	}

	/** Intervalo entre lotes de spawn dentro da onda: começa lento e aperta
	 *  aos poucos, estabilizando a partir da onda 10. */
	private static int arenaSpawnInterval() {
		SurvivalStageDefinition stage = getSurvivalStageDefinition();
		return Math.max(90, stage.getSpawnIntervalFrames() - Math.max(0, arenaWave - 1) * 4);
	}

	private static boolean waveClearedAnnounced = false;

	private static int spawnArenaEnemies() {
		SurvivalStageDefinition stage = getSurvivalStageDefinition();
		int phase = getSurvivalPhase();
		if (arenaThreatRemaining <= 0) {
			arenaThreatBudget = stage.getThreatBudget(arenaWave);
			arenaThreatRemaining = arenaThreatBudget;
		}
		int availableSlots = Math.max(0, stage.getMaxEnemies() - Game.enemies.size());
		int count = Math.min(availableSlots, 2 + arenaWave / 2 + Math.max(0, phase - 1));
		int spawned = 0;
		for (int i = 0; i < count && arenaThreatRemaining > 0; i++) {
			Enemy.Variant variant = chooseAffordableVariant(stage, i);
			if (variant == null) {
				arenaThreatRemaining = 0;
				break;
			}
			int[] spot = findSpawnSpot();
			if (spot == null) {
				break;
			}
			Enemy enemy = spawnSurvivalEnemy(spot[0], spot[1], i, phase, stage, variant);
			Game.entities.add(enemy);
			Game.enemies.add(enemy);
			arenaThreatRemaining -= stage.getThreatCost(variant);
			spawned++;
		}
		if (spawned > 0) {
			// A próxima área vazia poderá registrar a conclusão desta onda.
			waveClearedAnnounced = false;
		}
		return spawned;
	}

	private static Enemy.Variant chooseAffordableVariant(SurvivalStageDefinition stage, int index) {
		List<Enemy.Variant> pool = stage.getPool();
		for (int offset = 0; offset < pool.size(); offset++) {
			Enemy.Variant candidate = pool.get(Math.floorMod(arenaWave + index + offset + getSurvivalPhase(), pool.size()));
			if (stage.getThreatCost(candidate) <= arenaThreatRemaining) {
				return candidate;
			}
		}
		return null;
	}

	private static Enemy spawnSurvivalEnemy(int x, int y, int index, int phase,
			SurvivalStageDefinition stage, Enemy.Variant variant) {
		Enemy enemy = new Enemy(x, y, 16, 16, Entity.ENEMY_EN, variant, false,
				arenaWave >= 4 && (index + arenaWave) % 7 == 0);
		double depth = Math.sqrt(Math.max(1, wavesSurvived));
		enemy.boost(stage.getLifeMultiplier() * (1.0 + depth * 0.20),
				stage.getDamageMultiplier() * (1.0 + depth * 0.07));
		return enemy;
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

	private static void updateSpecialRule() {
		if (!arenaMode || Game.player == null) {
			return;
		}
		if (specialRuleTimer > 0) {
			specialRuleTimer--;
			return;
		}
		SurvivalStageDefinition stage = getSurvivalStageDefinition();
		if (stage.getSpecialRule() == SurvivalStageDefinition.SpecialRule.NONE) {
			return;
		}
		specialRuleTimer = stage.hasRule(SurvivalStageDefinition.SpecialRule.ACID_POOLS) ? 42 : 34;
		int centerX = Game.player.getX() + 8;
		int centerY = Game.player.getY() + 8;
		if (!stage.isHazardAt(centerX, centerY, arenaWave)) {
			return;
		}
		if (stage.hasRule(SurvivalStageDefinition.SpecialRule.ACID_POOLS)) {
			Game.player.applyDamage(AcidPoolHazard.damageForStage(getSurvivalPhase()));
			com.traduvertgames.graficos.MissionBanner.show("PÂNTANO ÁCIDO", "Saia da poça corrosiva!",
					new Color(124, 179, 66), Color.WHITE, 45);
		} else if (stage.hasRule(SurvivalStageDefinition.SpecialRule.ORBITAL_LASERS)) {
			Game.player.applyDamage(2.4 + Math.max(0, getSurvivalPhase() - 7) * 0.45);
			com.traduvertgames.graficos.MissionBanner.show("LASER ORBITAL", "Atravesse a linha de disparo!",
					new Color(0, 188, 212), Color.WHITE, 45);
		}
	}

	public static void render(Graphics g) {
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;
		if (arenaMode) {
			renderSpecialRule(g, screenWidth, screenHeight);
		}
		if (!announcing) {
			return;
		}
		int alpha = announceTimer > 75 ? (90 - announceTimer) * 12 : (announceTimer > 15 ? 180 : announceTimer * 12);
		g.setColor(new Color(announceColor.getRed(), announceColor.getGreen(), announceColor.getBlue(),
				Math.max(0, Math.min(200, alpha))));
		Font font = new Font("arial", Font.BOLD, 30);
		g.setFont(font);
		int textWidth = g.getFontMetrics().stringWidth(announceText);
		g.drawString(announceText, (screenWidth - textWidth) / 2, screenHeight / 2 - 40);
	}

	private static void renderSpecialRule(Graphics g, int screenWidth, int screenHeight) {
		SurvivalStageDefinition stage = getSurvivalStageDefinition();
		if (stage.hasRule(SurvivalStageDefinition.SpecialRule.ACID_POOLS)) {
			AcidPoolHazard.render(g, Camera.x, Camera.y, screenWidth, screenHeight, arenaWave, Game.SCALE);
		} else if (stage.hasRule(SurvivalStageDefinition.SpecialRule.ORBITAL_LASERS)) {
			int lane = Math.floorMod(arenaWave * 43, Math.max(1, screenWidth));
			g.setColor(new Color(0, 229, 255, 125));
			g.fillRect(lane, 0, 3 * Game.SCALE, screenHeight);
			g.setColor(new Color(255, 82, 82, 105));
			g.fillRect(0, Math.floorMod(arenaWave * 29, Math.max(1, screenHeight)), screenWidth, 2 * Game.SCALE);
		}
	}
}
