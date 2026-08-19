package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.quest.SideQuestManager;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.RegionalChainManager;
import com.traduvertgames.world.RpgWorldManager;

/** Regressões do encadeamento RESCUE -> NPC -> SUPPLY_CONVOY -> DUNGEON. */
public class RegionalChainManagerTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Game.setRegionalAdventureMode(true);
        Game.getInstance().loadRegionalAdventure(1);
        RpgWorldManager.updatePlayerPosition(Game.player.getX(), Game.player.getY());
        RegionalChainManager.reset();
        DynamicEventManager.reset();
    }

    @AfterEach
    void tearDown() {
        RegionalChainManager.reset();
        DynamicEventManager.reset();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void chainStartsWithRescueAndAdvancesThroughAllFourSteps() {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        assertEquals(RegionalChainManager.Stage.RESCUE, RegionalChainManager.getStage(region));
        assertTrue(RegionalChainManager.startNextStep(region));
        assertEquals(DynamicEventManager.Type.RESCUE, DynamicEventManager.getActiveType());

        RegionalChainManager.onRescueCompleted(region);
        assertEquals(RegionalChainManager.Stage.NPC, RegionalChainManager.getStage(region));
        assertTrue(RegionalChainManager.startNextStep(region));
        String questId = com.traduvertgames.entities.RegionalNpcs.getQuestIdForRegion(region);
        assertTrue(SideQuestManager.isActive(questId));

        SideQuestManager.complete(questId);
        assertEquals(RegionalChainManager.Stage.SUPPLY_CONVOY, RegionalChainManager.getStage(region));
        DynamicEventManager.abortActiveEventForMapChange();
        assertTrue(RegionalChainManager.startNextStep(region));
        assertEquals(DynamicEventManager.Type.SUPPLY_CONVOY, DynamicEventManager.getActiveType());

        RegionalChainManager.onConvoyCompleted(region);
        DynamicEventManager.abortActiveEventForMapChange();
        assertEquals(RegionalChainManager.Stage.DUNGEON, RegionalChainManager.getStage(region));
        RegionalChainManager.onDungeonCompleted(region);
        assertEquals(RegionalChainManager.Stage.COMPLETE, RegionalChainManager.getStage(region));
        assertEquals("Cadeia regional: 4/4", RegionalChainManager.getProgressLabel(region));
    }

    @Test
    void chainStateRoundTripsAndKeepsNextAction() {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        RegionalChainManager.onRescueCompleted(region);
        RegionalChainManager.onNpcQuestCompleted(
                com.traduvertgames.entities.RegionalNpcs.getQuestIdForRegion(region));
        RegionalChainManager.onConvoyCompleted(region);
        assertEquals(RegionalChainManager.Stage.DUNGEON, RegionalChainManager.getStage(region));

        Map<String, Object> snapshot = RegionalChainManager.serialize();
        RegionalChainManager.reset();
        assertEquals(RegionalChainManager.Stage.RESCUE, RegionalChainManager.getStage(region));
        RegionalChainManager.deserialize(snapshot);

        assertEquals(RegionalChainManager.Stage.DUNGEON, RegionalChainManager.getStage(region));
        assertNotNull(RegionalChainManager.getNextActionTitle(region));
        assertTrue(RegionalChainManager.getSummary(region).contains("Próximo: Entrar na dungeon"));
    }

    @Test
    void chainStatePersistsInsideRegionalSaveSession() throws Exception {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        RegionalChainManager.onRescueCompleted(region);
        RegionalChainManager.onNpcQuestCompleted(
                com.traduvertgames.entities.RegionalNpcs.getQuestIdForRegion(region));
        SaveManager.activeSlot = 1;
        assertTrue(SaveManager.saveCurrentGame());

        RegionalChainManager.reset();
        assertTrue(SaveManager.loadSlot(1));
        assertEquals(RegionalChainManager.Stage.SUPPLY_CONVOY, RegionalChainManager.getStage(region));
    }
}
