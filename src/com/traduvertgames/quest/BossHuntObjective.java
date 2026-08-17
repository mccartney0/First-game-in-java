package com.traduvertgames.quest;

import com.traduvertgames.entities.Enemy;

public final class BossHuntObjective extends BaseObjective {
    private boolean bossPresent = false;
    private boolean bossDefeated = false;

    private final String bossName;

    public BossHuntObjective() {
        this("Neutralizar comandante", "Elimine o líder tecnomante que controla a fortaleza.", "o Comandante");
    }

    public BossHuntObjective(String title, String description, String bossName) {
        super(title, description);
        this.bossName = bossName;
    }

    @Override
    public String getProgressText() {
        if (!bossPresent) {
            return "Varra a fortaleza e encontre " + bossName;
        }
        return bossDefeated ? bossName.substring(2) + " neutralizado" : "Confronto em andamento";
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
    public void onBossSpotted() {
        registerBossPresence();
    }


    /**
     * Enquanto o chefe não é neutralizado, a seta do waypoint da HUD aponta
     * para ele ({@code findTargetEntity} localiza o boss vivo pelo prefixo).*/
    @Override
    public String getTargetHint() {
        if (bossDefeated) {
            return null;
        }
        return bossName;
    }

    @Override
    public boolean isComplete() {
        return bossPresent && bossDefeated;
    }
}
