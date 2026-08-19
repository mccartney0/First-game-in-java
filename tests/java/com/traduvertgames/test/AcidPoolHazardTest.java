package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.traduvertgames.main.AcidPoolHazard;

class AcidPoolHazardTest {

    @Test
    void poolsAreDeterministicByTileAndWave() {
        assertFalse(AcidPoolHazard.isPoolAt(0, 0, 1));
        assertTrue(AcidPoolHazard.isPoolAt(64, 0, 1));
        assertEquals(AcidPoolHazard.isPoolAt(64, 0, 1), AcidPoolHazard.isPoolAt(64, 0, 1));
        assertFalse(AcidPoolHazard.isPoolAt(64, 0, 2));
    }

    @Test
    void acidDamageScalesAfterTheFirstSwampStage() {
        assertEquals(1.8, AcidPoolHazard.damageForStage(6), 0.0001);
        assertEquals(2.15, AcidPoolHazard.damageForStage(7), 0.0001);
    }
}
