package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.graficos.HubScreen;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.RpgWorldManager;

/** Regressão do atalho H no primeiro acesso à Aventura RPG. */
public class HubHotkeyTest {

    private Game game;

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        game = GameTestFixture.initHeadless();
        Game.setRegionalAdventureMode(true);
        game.loadRegionalAdventure(1);
        RpgWorldManager.updatePlayerPosition(Game.player.getX(), Game.player.getY());
    }

    @AfterEach
    void tearDown() {
        HubScreen.reset();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void hOpensHubImmediatelyAfterRegionalAdventureLoads() {
        assertEquals("NORMAL", Game.gameState);
        assertTrue(RpgWorldManager.isActive());
        assertTrue(Game.isTransitionCooldown());

        press(KeyEvent.VK_H);

        assertTrue(HubScreen.isOpen());
        assertEquals("REGIONAL_HUB", Game.gameState);
        assertFalse(Game.isTransitionCooldown());
    }

    @Test
    void escapeClosesHubAfterOpeningWithH() {
        press(KeyEvent.VK_H);
        assertTrue(HubScreen.isOpen());

        press(KeyEvent.VK_ESCAPE);

        assertFalse(HubScreen.isOpen());
        assertEquals("NORMAL", Game.gameState);
    }

    private void press(int keyCode) {
        game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, keyCode, (char) keyCode));
    }
}
