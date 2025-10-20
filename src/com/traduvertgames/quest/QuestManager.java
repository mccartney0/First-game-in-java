package com.traduvertgames.quest;

import java.util.ArrayList;
import java.util.List;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.QuestBeacon;
import com.traduvertgames.entities.QuestItem;
import com.traduvertgames.entities.QuestNPC;
import com.traduvertgames.main.Game;

public final class QuestManager {
    private static final RPGObjective NULL_OBJECTIVE = new NullObjective();
    private static RPGObjective currentObjective = NULL_OBJECTIVE;
    private static int currentLevel = 1;
    private static final List<Entity> pendingRemoval = new ArrayList<Entity>();

    private QuestManager() {
    }

    public static void prepareForLevel(int level) {
        currentLevel = level;
        currentObjective = createObjectiveForLevel(level);
        currentObjective.onLevelStart();
        pendingRemoval.clear();
    }

    public static void onLevelLoaded() {
        currentObjective.onLevelLoaded();
    }

    private static RPGObjective createObjectiveForLevel(int level) {
        switch (level) {
        case 1:
            return new CollectArtifactsObjective();
        case 2:
            return new BossHuntObjective();
        case 3:
            return new RitualObjective();
        case 4:
            return new RescueObjective();
        case 5:
            return new DataRecoveryObjective();
        default:
            return NULL_OBJECTIVE;
        }
    }

    public static void registerQuestItem(QuestItem item) {
        currentObjective.onQuestItemSpawned(item);
    }

    public static void collectQuestItem(QuestItem item) {
        currentObjective.onQuestItemCollected(item);
        scheduleRemoval(item);
    }

    public static void registerBeacon(QuestBeacon beacon) {
        currentObjective.onBeaconSpawned(beacon);
    }

    public static void activateBeacon(QuestBeacon beacon) {
        currentObjective.onBeaconActivated(beacon);
    }

    public static void registerNpc(QuestNPC npc) {
        currentObjective.onNpcSpawned(npc);
    }

    public static void rescueNpc(QuestNPC npc) {
        currentObjective.onNpcRescued(npc);
        scheduleRemoval(npc);
    }

    public static void notifyEnemyKilled(Enemy enemy) {
        currentObjective.onEnemyKilled(enemy);
    }

    public static void notifyBossSpotted() {
        if (currentObjective instanceof BossHuntObjective) {
            ((BossHuntObjective) currentObjective).registerBossPresence();
        }
    }

    public static String getObjectiveTitle() {
        return currentObjective.getTitle();
    }

    public static String getObjectiveDescription() {
        return currentObjective.getDescription();
    }

    public static String getObjectiveProgress() {
        return currentObjective.getProgressText();
    }

    public static boolean isObjectiveComplete() {
        return currentObjective.isComplete();
    }

    public static int getCurrentLevel() {
        return currentLevel;
    }

    public static void update() {
        currentObjective.update();
        processPendingRemovals();
    }

    private static void scheduleRemoval(Entity entity) {
        if (entity != null && !pendingRemoval.contains(entity)) {
            pendingRemoval.add(entity);
        }
    }

    private static void processPendingRemovals() {
        if (pendingRemoval.isEmpty()) {
            return;
        }
        Game.entities.removeAll(pendingRemoval);
        pendingRemoval.clear();
    }
}
