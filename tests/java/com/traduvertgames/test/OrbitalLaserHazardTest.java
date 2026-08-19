package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.main.OrbitalLaserHazard;
import com.traduvertgames.main.SurvivalStageDefinition;

class OrbitalLaserHazardTest {

    @Test
    void laserLanesAreDeterministicByTileAndWave() {
        assertTrue(OrbitalLaserHazard.isLaserAt(0, 0, 1));
        assertFalse(OrbitalLaserHazard.isLaserAt(32, 32, 1));
        assertTrue(OrbitalLaserHazard.isLaserAt(32, 32, 5));
    }

    @Test
    void orbitalLaserDamageScalesAfterTheFactoryStage() {
        assertEquals(2.4, OrbitalLaserHazard.damageForStage(7), 0.0001);
        assertEquals(2.85, OrbitalLaserHazard.damageForStage(8), 0.0001);
    }

    @Test
    void orbitalFactoryKeepsItsHeavyMachinePool() {
        List<SurvivalStageDefinition> stages = SurvivalStageDefinition.defaultStages();
        SurvivalStageDefinition factory = stages.get(6);
        assertTrue(factory.hasRule(SurvivalStageDefinition.SpecialRule.ORBITAL_LASERS));
        assertTrue(factory.getPool().contains(Enemy.Variant.ARTILLERY));
        assertTrue(factory.getPool().contains(Enemy.Variant.WARBRINGER));
        assertTrue(factory.isHazardAt(0, 0, 1));
        assertFalse(factory.isHazardAt(32, 32, 1));
    }
}
