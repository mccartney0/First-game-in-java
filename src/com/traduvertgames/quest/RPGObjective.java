package com.traduvertgames.quest;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.QuestBeacon;
import com.traduvertgames.entities.QuestItem;
import com.traduvertgames.entities.QuestNPC;

public interface RPGObjective {
    default void onLevelStart() {
    }

    default void onLevelLoaded() {
    }

    default void update() {
    }

    default void onQuestItemSpawned(QuestItem item) {
    }

    default void onQuestItemCollected(QuestItem item) {
    }

    default void onBeaconSpawned(QuestBeacon beacon) {
    }

    default void onBeaconActivated(QuestBeacon beacon) {
    }

    default void onNpcSpawned(QuestNPC npc) {
    }

    default void onNpcRescued(QuestNPC npc) {
    }

    default void onEnemyKilled(Enemy enemy) {
    }

    String getTitle();

    String getDescription();

    String getProgressText();

    boolean isComplete();
}
