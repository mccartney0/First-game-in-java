package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.world.RegionalProgressionManager;
import com.traduvertgames.world.RpgWorldManager;

class RegionalProgressionTest {

    @BeforeEach
    void reset() {
        RegionalProgressionManager.reset();
    }

    @Test
    void newRegionsStartWithNeutralReputationAndHighThreat() {
        assertEquals(0, RegionalProgressionManager.getReputation(RpgWorldManager.RegionType.REFUGE));
        assertEquals(50, RegionalProgressionManager.getThreat(RpgWorldManager.RegionType.REFUGE));
        assertEquals("Recém-chegado", RegionalProgressionManager.getReputationTier(RpgWorldManager.RegionType.REFUGE));
        assertEquals("Alta", RegionalProgressionManager.getThreatLabel(RpgWorldManager.RegionType.REFUGE));
    }

    @Test
    void successfulEventImprovesRegionAndReducesThreat() {
        RpgWorldManager.RegionType region = RpgWorldManager.RegionType.MARSH;
        RegionalProgressionManager.registerEventOutcome(region, true);
        assertEquals(10, RegionalProgressionManager.getReputation(region));
        assertEquals(42, RegionalProgressionManager.getThreat(region));
    }

    @Test
    void dungeonCompletionHasStrongerRegionalImpact() {
        RpgWorldManager.RegionType region = RpgWorldManager.RegionType.CORE;
        RegionalProgressionManager.registerDungeonComplete(region);
        assertEquals(18, RegionalProgressionManager.getReputation(region));
        assertEquals(38, RegionalProgressionManager.getThreat(region));
    }

    @Test
    void reputationRoundTripClampsInvalidValues() {
        Map<String, Object> snapshot = RegionalProgressionManager.serialize();
        @SuppressWarnings("unchecked")
        Map<String, Object> reputation = (Map<String, Object>) snapshot.get("reputation");
        @SuppressWarnings("unchecked")
        Map<String, Object> threat = (Map<String, Object>) snapshot.get("threat");
        reputation.put("TUNDRA", 140);
        threat.put("TUNDRA", -20);
        RegionalProgressionManager.deserialize(snapshot);
        assertEquals(100, RegionalProgressionManager.getReputation(RpgWorldManager.RegionType.TUNDRA));
        assertEquals(0, RegionalProgressionManager.getThreat(RpgWorldManager.RegionType.TUNDRA));
    }

    @Test
    void summaryExposesBothProgressionAxes() {
        String summary = RegionalProgressionManager.getSummary(RpgWorldManager.RegionType.SANCTUARY);
        assertTrue(summary.contains("Reputação 0/100"));
        assertTrue(summary.contains("Ameaça 50/100"));
    }
}
