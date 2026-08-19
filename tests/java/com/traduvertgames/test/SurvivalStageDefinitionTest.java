package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SurvivalStageDefinition;
import com.traduvertgames.main.WaveManager;

class SurvivalStageDefinitionTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
    }

    @AfterEach
    void tearDown() {
        WaveManager.reset();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void catalogContainsTheOriginalStagesAndNewSpecialStages() {
        List<SurvivalStageDefinition> stages = WaveManager.getSurvivalStages();
        assertEquals(7, stages.size());
        assertEquals("PÂNTANO ÁCIDO", stages.get(5).getName());
        assertEquals("FÁBRICA ORBITAL", stages.get(6).getName());
        assertTrue(stages.get(5).hasRule(SurvivalStageDefinition.SpecialRule.ACID_POOLS));
        assertTrue(stages.get(6).hasRule(SurvivalStageDefinition.SpecialRule.ORBITAL_LASERS));
    }

    @Test
    void threatBudgetGrowsByWaveAndCostsFavorSwarmOverElites() {
        SurvivalStageDefinition swamp = WaveManager.getSurvivalStages().get(5);
        assertTrue(swamp.getThreatBudget(5) > swamp.getThreatBudget(1));
        assertTrue(swamp.getThreatCost(Enemy.Variant.SWARM) < swamp.getThreatCost(Enemy.Variant.WARBRINGER));
        assertTrue(swamp.getPool().contains(Enemy.Variant.SAPPER));
        assertFalse(swamp.getPool().contains(Enemy.Variant.ARTILLERY));
    }

    @Test
    void waveManagerSelectsStageFromSurvivalDepth() {
        Game.getInstance().setLevelPlus(6);
        assertEquals("PÂNTANO ÁCIDO", WaveManager.getSurvivalStageDefinition().getName());
        Game.getInstance().setLevelPlus(7);
        assertEquals("FÁBRICA ORBITAL", WaveManager.getSurvivalStageDefinition().getName());
    }
}
