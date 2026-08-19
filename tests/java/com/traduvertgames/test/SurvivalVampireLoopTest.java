package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.ExperienceOrb;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.LevelUpManager;
import com.traduvertgames.main.WaveManager;
import com.traduvertgames.quest.QuestManager;

/** Regressões do loop Vampire Survivors no modo sobrevivência. */
public class SurvivalVampireLoopTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Game.getInstance().enterInfiniteMode();
        Game.enemies.clear();
        Game.bullets.clear();
        Game.entities.removeIf(entity -> entity instanceof Enemy || entity instanceof ExperienceOrb);
    }

    @AfterEach
    void tearDown() {
        WaveManager.reset();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void survivalStartsAnArenaPhaseWithExplicitSummary() {
        assertTrue(QuestManager.isSurvivalMode());
        assertTrue(WaveManager.isArenaMode());
        assertEquals(1, WaveManager.getSurvivalPhase());
        assertTrue(WaveManager.getSurvivalSummary().contains("Fase 1"));
        assertTrue(WaveManager.getSurvivalSummary().contains("Onda 1"));
    }

    @Test
    void experienceOrbIsCollectedByPilot() {
        double before = LevelUpManager.getXp();
        ExperienceOrb orb = new ExperienceOrb(Game.player.getX(), Game.player.getY(), 10);
        Game.entities.add(orb);
        orb.update();

        assertTrue(LevelUpManager.getXp() > before);
        assertFalse(Game.entities.contains(orb));
    }

    @Test
    void survivalAutoFiresAtNearestEnemyWithoutShootKey() {
        Enemy enemy = new Enemy(Game.player.getX() + 80, Game.player.getY(), 16, 16, null,
                Enemy.Variant.SCOUT, false);
        Game.entities.add(enemy);
        Game.enemies.add(enemy);
        Game.player.shoot = false;

        Game.player.update();

        assertTrue(Game.bullets.size() > 0);
    }
}
