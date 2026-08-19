package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.RpgWorldManager;

/** Regressões do modo principal Aventura RPG e dos eventos de superfície. */
public class RegionalAdventureModeTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Game.setRegionalAdventureMode(true);
        Game.getInstance().loadRegionalAdventure(1);
        RpgWorldManager.updatePlayerPosition(Game.player.getX(), Game.player.getY());
    }

    @AfterEach
    void tearDown() {
        DynamicEventManager.reset();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void rpgAdventureUsesRegionalWorldAsMainSession() {
        assertTrue(Game.isRegionalAdventureMode());
        assertTrue(RpgWorldManager.isActive());
        assertFalse(RpgWorldManager.isDungeonMode());
        assertEquals(Game.MAX_LEVEL + 1, Game.getCurrentLevel());
    }

    @Test
    void rescueAndSupplyConvoyAreAvailableActivities() {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        assertNotNull(region);
        assertTrue(DynamicEventManager.hasAvailableEvent(region));
        assertTrue(DynamicEventManager.startEventForCurrentRegion(DynamicEventManager.Type.RESCUE));
        assertEquals(DynamicEventManager.Type.RESCUE, DynamicEventManager.getActiveType());
        DynamicEventManager.abortActiveEventForMapChange();
        assertTrue(DynamicEventManager.startEventForCurrentRegion(DynamicEventManager.Type.SUPPLY_CONVOY));
        assertEquals(DynamicEventManager.Type.SUPPLY_CONVOY, DynamicEventManager.getActiveType());
    }

    @Test
    void eventStateKeepsNewTypesThroughSerialization() {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        assertTrue(DynamicEventManager.startEventForCurrentRegion(DynamicEventManager.Type.RESCUE));
        Map<String, Object> snapshot = DynamicEventManager.serialize();
        DynamicEventManager.deserialize(snapshot);
        assertEquals(DynamicEventManager.Type.RESCUE, DynamicEventManager.getActiveType());
        assertEquals(region, DynamicEventManager.getActiveRegion());
    }

    	@Test
	void saveRoundTripIdentifiesRegionalAdventure() throws Exception {
		SaveManager.activeSlot = 1;
		assertTrue(SaveManager.saveCurrentGame());
		assertTrue(SaveManager.getSlotObjectiveText(1).startsWith("Aventura RPG"));
		Game.setRegionalAdventureMode(false);
		RpgWorldManager.disable();
		assertTrue(SaveManager.loadSlot(1));
		assertTrue(Game.isRegionalAdventureMode());
		assertTrue(RpgWorldManager.isActive());
	}

}
