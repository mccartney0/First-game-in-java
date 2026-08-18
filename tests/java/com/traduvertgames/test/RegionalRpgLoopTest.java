package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.RegionalNpcs;
import com.traduvertgames.quest.SideQuestManager;
import com.traduvertgames.world.DungeonManager;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.RpgWorldManager;

/** Regressões do loop RPG regional e das atividades opcionais. */
public class RegionalRpgLoopTest {

    @BeforeEach
    void resetRegionalState() throws Exception {
        GameTestFixture.cleanSaveFiles();
        GameTestFixture.initHeadless();
        SideQuestManager.reset();
        RegionalNpcs.registerDefinitions();
        DungeonManager.reset();
        DynamicEventManager.reset();
    }

    @Test
    void newEnemyVariantsExposeDistinctRegionalRoles() {
        assertNotNull(Enemy.Variant.valueOf("BOMBER"));
        assertNotNull(Enemy.Variant.valueOf("SHIELDER"));
        assertNotNull(Enemy.Variant.valueOf("SNIPER"));
        assertNotNull(Enemy.Variant.valueOf("SWARM"));
        assertNotEquals(Enemy.Variant.BOMBER, Enemy.Variant.SNIPER);
    }

    @Test
    void hubMissionUsesTheSamePersistentRegionalQuestDefinition() {
        RpgWorldManager.RegionType region = RpgWorldManager.RegionType.RUINS;
        String questId = RegionalNpcs.getQuestIdForRegion(region);
        RegionalNpcs.activateQuestForRegion(region);

        assertTrue(SideQuestManager.isRegistered(questId));
        assertTrue(SideQuestManager.isActive(questId));
        assertEquals("Limpeza das Ruínas", SideQuestManager.get(questId).title);
    }

    @Test
    void dynamicEventStateRoundTripsWithoutLosingCompletedActivities() {
        Map<String, Object> snapshot = new HashMap<String, Object>();
        Map<String, Boolean> completed = new HashMap<String, Boolean>();
        completed.put("RUINS:2:AMBUSH", true);
        snapshot.put("completed", completed);
        snapshot.put("activeType", "ELITE_HUNT");
        snapshot.put("activeRegion", "TUNDRA");
        snapshot.put("activeDepth", 2);
        snapshot.put("activeTimer", 120);
        snapshot.put("needsSpawn", true);

        DynamicEventManager.deserialize(snapshot);
        assertTrue(DynamicEventManager.isActive());
        assertEquals(DynamicEventManager.Type.ELITE_HUNT, DynamicEventManager.getActiveType());

        Map<String, Object> serialized = DynamicEventManager.serialize();
        assertEquals(Boolean.TRUE,
                ((Map<?, ?>) serialized.get("completed")).get("RUINS:2:AMBUSH"));
        assertEquals("ELITE_HUNT", serialized.get("activeType"));
        assertEquals("TUNDRA", serialized.get("activeRegion"));
    }

    @Test
    void dungeonCompletionSnapshotRemainsPersistent() {
        Map<String, Boolean> completed = new HashMap<String, Boolean>();
        completed.put(RpgWorldManager.RegionType.MARSH.name(), true);
        DungeonManager.deserializeCompletions(completed);

        assertTrue(DungeonManager.isRegionCompleted(RpgWorldManager.RegionType.MARSH));
        assertFalse(DungeonManager.isRegionCompleted(RpgWorldManager.RegionType.TUNDRA));
    }
}
