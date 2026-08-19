package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.main.ShopManager;
import com.traduvertgames.graficos.VictoryCutscene;
import com.traduvertgames.quest.QuestManager;

/** Regressões para slots, limpeza de progresso e exclusividade de overlays. */
public class SaveMenuOverlayTest {

    @BeforeEach
    void resetState() throws Exception {
        VictoryCutscene.stop();
        if (ShopManager.isOpen()) {
            ShopManager.close();
        }
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
        GameTestFixture.initHeadless();
        SaveManager.activeSlot = 1;
    }

    @Test
    void saveToSlotChangesActiveSlotWithoutOverwritingAnotherSlot() {
        Game.setCurrentLevel(2);
        QuestManager.prepareForLevel(2);
        assertTrue(SaveManager.saveCurrentGameToSlot(2));
        assertEquals(2, SaveManager.activeSlot);
        assertEquals(2, SaveManager.getSlotLevel(2));
        assertEquals(-1, SaveManager.getSlotLevel(1));

        Game.setCurrentLevel(3);
        QuestManager.prepareForLevel(3);
        assertTrue(SaveManager.saveCurrentGameToSlot(3));
        assertEquals(3, SaveManager.activeSlot);
        assertEquals(2, SaveManager.getSlotLevel(2));
        assertEquals(3, SaveManager.getSlotLevel(3));
    }

    @Test
    void clearingSlotRemovesNarrativeProgressToo() {
        Game.setCurrentLevel(8);
        QuestManager.prepareForLevel(8);
        assertTrue(SaveManager.saveCurrentGameToSlot(2));
        assertNotEquals("", SaveManager.getSlotObjectiveText(2));

        assertTrue(SaveManager.clearSlot(2));
        assertFalse(SaveManager.hasSlotSave(2));
        assertEquals(-1, SaveManager.getSlotLevel(2));
        assertEquals("", SaveManager.getSlotObjectiveText(2));
    }

    @Test
    void victoryCutsceneClosesShopBeforeShowing() {
        ShopManager.open();
        assertTrue(ShopManager.isOpen());

        VictoryCutscene.start();

        assertTrue(VictoryCutscene.isShowing());
        assertFalse(ShopManager.isOpen());
        VictoryCutscene.returnToMainMenu();
        assertFalse(VictoryCutscene.isShowing());
        assertEquals("MENU", Game.gameState);
    }
}
