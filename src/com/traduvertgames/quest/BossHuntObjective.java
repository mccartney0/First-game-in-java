package com.traduvertgames.quest;

import com.traduvertgames.entities.Enemy;

public final class BossHuntObjective extends BaseObjective {
    private boolean bossPresent = false;
    private boolean bossDefeated = false;

    public BossHuntObjective() {
        super("Neutralizar comandante", "Elimine o líder tecnomante que controla a fortaleza.");
    }

    @Override
    public void onLevelStart() {
        bossPresent = false;
        bossDefeated = false;
    }

    @Override
    public void onEnemyKilled(Enemy enemy) {
        if (enemy.isBoss()) {
            bossDefeated = true;
        }
    }

    public void registerBossPresence() {
        bossPresent = true;
    }

    @Override
    public String getProgressText() {
        if (!bossPresent) {
            return "Varra a fortaleza e encontre o chefe";
        }
        return bossDefeated ? "Chefe neutralizado" : "Confronto em andamento";
    }

    @Override
    public boolean isComplete() {
        return bossPresent && bossDefeated;
    }
}
