package com.traduvertgames.world;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.FloatingText;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.main.Game;
import com.traduvertgames.state.PilotUpgrades;

/** Controla instâncias de masmorra e o retorno ao mapa de superfície. */
public final class DungeonManager {

    private static final Map<String, Boolean> completed = new HashMap<String, Boolean>();

    private static boolean inDungeon;
    private static boolean bossDefeated;
    private static RpgWorldManager.RegionType dungeonRegion;
    private static RpgWorldManager.RegionType surfaceRegion;
    private static int surfaceDepth = 1;
    private static int surfaceX;
    private static int surfaceY;
    private static RpgWorldManager.RegionType pendingEntry;
    private static boolean pendingExit;
    private static int transitionCooldown;

    private DungeonManager() {
    }

    public static boolean isInDungeon() {
        return inDungeon;
    }

    public static RpgWorldManager.RegionType getDungeonRegion() {
        return dungeonRegion;
    }

    public static boolean isBossDefeated() {
        return bossDefeated;
    }

    public static boolean isRegionCompleted(RpgWorldManager.RegionType region) {
        return region != null && Boolean.TRUE.equals(completed.get(region.name()));
    }

    public static Map<String, Boolean> serializeCompletions() {
        return new HashMap<String, Boolean>(completed);
    }

    public static void deserializeCompletions(Map<String, Boolean> snapshot) {
        completed.clear();
        if (snapshot != null) {
            completed.putAll(snapshot);
        }
    }

    public static void reset() {
        completed.clear();
        inDungeon = false;
        bossDefeated = false;
        dungeonRegion = null;
        surfaceRegion = null;
        surfaceDepth = 1;
        surfaceX = 0;
        surfaceY = 0;
        pendingEntry = null;
        pendingExit = false;
        transitionCooldown = 0;
    }

    /** Solicita uma entrada; a troca real ocorre fora do loop de entidades. */
	public static boolean requestEnter(RpgWorldManager.RegionType region) {
		if (!RpgWorldManager.isActive() || RpgWorldManager.isDungeonMode() || region == null
				|| transitionCooldown > 0 || Game.player == null) {
			return false;
		}
		pendingEntry = region;
		return true;
	}

    public static void requestExit() {
        if (!inDungeon || !bossDefeated || transitionCooldown > 0) {
            return;
        }
        pendingExit = true;
    }

    /** Executa as transições agendadas pelo portal depois do update das entidades. */
    public static void processPendingTransition() {
        if (transitionCooldown > 0) {
            transitionCooldown--;
            return;
        }
        if (pendingEntry != null) {
            RpgWorldManager.RegionType region = pendingEntry;
            pendingEntry = null;
            enter(region);
        } else if (pendingExit) {
            pendingExit = false;
            exit();
        }
    }

    private static void enter(RpgWorldManager.RegionType region) {
        if (Game.player == null) {
            return;
        }
        DynamicEventManager.abortActiveEventForMapChange();
        surfaceRegion = RpgWorldManager.getCurrentRegion();
        surfaceDepth = Math.max(1, RpgWorldManager.getDepth());
        surfaceX = Game.player.getX();
        surfaceY = Game.player.getY();
        dungeonRegion = region;
        bossDefeated = false;
        inDungeon = true;
        try {
            File dungeon = ProceduralDungeonGenerator.generate(region, surfaceDepth);
            RpgWorldManager.configureDungeon(region, surfaceDepth);
            World.restartDungeonFromFile(dungeon.getAbsolutePath());
            transitionCooldown = 45;
            Game.gameState = "NORMAL";
            FloatingText.show("ENTRADA NA MASMORRA: " + region.getDisplayName(),
                    Game.player.getX(), Game.player.getY() - 16, new java.awt.Color(220, 80, 255), 120);
        } catch (Exception error) {
            error.printStackTrace();
            inDungeon = false;
            dungeonRegion = null;
            RpgWorldManager.configure(surfaceDepth, RpgWorldManager.getMapWidth(), RpgWorldManager.getMapHeight());
        }
    }

    private static void exit() {
        if (!inDungeon) {
            return;
        }
        try {
            File surface = ProceduralLevelGenerator.generate(surfaceDepth);
            RpgWorldManager.configure(surfaceDepth, ProceduralLevelGenerator.MAP_WIDTH,
                    ProceduralLevelGenerator.MAP_HEIGHT);
            World.restartGameFromFile(surface.getAbsolutePath());
            inDungeon = false;
            RpgWorldManager.RegionType returnRegion = surfaceRegion == null
                    ? RpgWorldManager.RegionType.REFUGE : surfaceRegion;
            placePlayerNearSurface(returnRegion);
            dungeonRegion = null;
            bossDefeated = false;
            transitionCooldown = 45;
            Game.gameState = "NORMAL";
            FloatingText.show("RETORNO À SUPERFÍCIE", Game.player.getX(), Game.player.getY() - 16,
                    new java.awt.Color(129, 199, 132), 100);
        } catch (Exception error) {
            error.printStackTrace();
        }
    }

    private static void placePlayerNearSurface(RpgWorldManager.RegionType region) {
        int candidateX = surfaceX;
        int candidateY = surfaceY;
        if (candidateX <= 0 || candidateY <= 0 || !World.isFree(candidateX, candidateY, 0)) {
            RpgWorldManager.RegionBounds bounds = RpgWorldManager.getBounds(region);
            candidateX = bounds.centerX() * World.TILE_SIZE;
            candidateY = bounds.centerY() * World.TILE_SIZE;
        }
        Game.player.setX(candidateX);
        Game.player.setY(candidateY);
        Game.player.updateCamera();
    }

    /** Chamado pelo hook de morte de Enemy para concluir a instância. */
    public static void onEnemyDefeated(Enemy enemy) {
        if (!inDungeon || enemy == null || !enemy.isBoss()) {
            return;
        }
        Enemy.Variant expected = ProceduralDungeonGenerator.bossVariant(dungeonRegion);
        if (enemy.getVariant() != expected || bossDefeated) {
            return;
        }
		bossDefeated = true;
		completed.put(dungeonRegion.name(), true);
		RegionalProgressionManager.registerDungeonComplete(dungeonRegion);
		com.traduvertgames.quest.ContractManager.onDungeonCompleted(dungeonRegion);
		PilotUpgrades.addCredits(350);
        Game.addScore(500);
        WeaponType reward = rewardForRegion(dungeonRegion);
        if (Game.player != null && reward != null && !Game.player.hasWeaponUnlocked(reward)) {
            Game.player.unlockWeapon(reward);
            FloatingText.show("ARMA DESBLOQUEADA: " + reward.getDisplayName().toUpperCase(),
                    enemy.getX(), enemy.getY() - 30, new java.awt.Color(255, 235, 59), 180);
        } else {
            FloatingText.show("CHEFE REGIONAL DERROTADO +350 CRÉDITOS",
                    enemy.getX(), enemy.getY() - 16, new java.awt.Color(255, 214, 10), 150);
        }
    }

    /** Cada região entrega uma arma permanente diferente; a conclusão é idempotente. */
    private static WeaponType rewardForRegion(RpgWorldManager.RegionType region) {
        if (region == null) {
            return null;
        }
        switch (region) {
        case REFUGE:
            return WeaponType.BOOMERANG_ARCANO;
        case RUINS:
            return WeaponType.ARC_DISRUPTOR;
        case MARSH:
            return WeaponType.SOLAR_CANNON;
        case TUNDRA:
            return WeaponType.CHAIN_ARC;
        case SANCTUARY:
            return WeaponType.PLASMA_CUTTER;
        case CORE:
        default:
            return WeaponType.FUSION_LANCE;
        }
    }
}
