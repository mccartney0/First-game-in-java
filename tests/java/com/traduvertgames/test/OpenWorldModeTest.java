package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.OpenWorldManager;
import com.traduvertgames.world.RpgWorldManager;

class OpenWorldModeTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Game.getInstance().loadOpenWorld(1);
        OpenWorldManager.updatePlayerPosition(Game.player.getX(), Game.player.getY());
        RpgWorldManager.updatePlayerPosition(Game.player.getX(), Game.player.getY());
    }

    @AfterEach
    void tearDown() {
        DynamicEventManager.reset();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void openWorldUsesSeparateModeAndGiantSurface() {
        assertTrue(Game.isOpenWorldMode());
        assertFalse(Game.isRegionalAdventureMode());
        assertTrue(RpgWorldManager.isActive());
        assertTrue(OpenWorldManager.isActive());
        assertEquals(Game.MAX_LEVEL + 2, Game.getCurrentLevel());
        assertEquals(512, OpenWorldManager.getWorldWidth());
        assertEquals(320, OpenWorldManager.getWorldHeight());
        assertTrue(OpenWorldManager.getTotalChunkCount() >= 40);
    }

    @Test
    void openWorldKeepsRegionalActivitiesAvailable() {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        assertTrue(DynamicEventManager.hasAvailableEvent(region));
        assertTrue(DynamicEventManager.startEventForCurrentRegion(DynamicEventManager.Type.ELITE_HUNT));
        assertEquals(DynamicEventManager.Type.ELITE_HUNT, DynamicEventManager.getActiveType());
    }

    @Test
    void openWorldSaveRoundTripRestoresModeAndExploration() throws Exception {
        SaveManager.activeSlot = 1;
        assertTrue(SaveManager.saveCurrentGame());
        assertTrue(SaveManager.getSlotObjectiveText(1).startsWith("Mundo Aberto gigante"));
        assertTrue(SaveManager.loadSlot(1));
        assertTrue(Game.isOpenWorldMode());
        assertFalse(Game.isRegionalAdventureMode());
        assertTrue(RpgWorldManager.isActive());
        assertTrue(OpenWorldManager.isActive());
        assertTrue(OpenWorldManager.getDiscoveredChunkCount() >= 1);
    }
}
