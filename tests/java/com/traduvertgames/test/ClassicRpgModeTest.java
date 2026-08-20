package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.Menu;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.rpg.ClassicRpgMode;
import com.traduvertgames.rpg.RpgArchetype;
import com.traduvertgames.rpg.RpgCharacterStats;
import com.traduvertgames.rpg.RpgMap;
import com.traduvertgames.rpg.RpgPlayerController;

class ClassicRpgModeTest {

    @AfterEach
    void tearDown() {
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void mainMenuExposesClassicRpgEntryWithoutRemovingExistingOptions() {
        Menu menu = new Menu();
		assertEquals(8, menu.getMainMenuOptionCountForTest());
		assertEquals("Jogar", menu.getMainMenuLabelForTest(0));
		assertEquals("Como jogar", menu.getMainMenuLabelForTest(3));
		assertEquals(6, menu.getGameModeOptionCountForTest());
		assertEquals("mundo aberto gigante", menu.getGameModeLabelForTest(0));
		assertEquals("aventura RPG", menu.getGameModeLabelForTest(1));
		assertEquals("expedição de dungeon (teste)", menu.getGameModeLabelForTest(2));
		assertEquals("rpg clássico", menu.getGameModeLabelForTest(3));
    }

    @Test
    void enteringClassicRpgUsesDedicatedSceneAndCharacterCreation() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();

        assertTrue(Game.isClassicRpgMode());
        ClassicRpgMode mode = Game.getClassicRpgMode();
        assertNotNull(mode);
        assertTrue(mode.isChoosingArchetype());
        assertEquals("vale_brumafolha", mode.getMapId());

        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_ENTER));

        assertFalse(mode.isChoosingArchetype());
        assertNotNull(mode.getCharacter());
        assertEquals(RpgArchetype.ARCANISTA, mode.getCharacter().getArchetype());
        assertFalse(Game.isRegionalAdventureMode());
        assertFalse(Game.isOpenWorldMode());
    }

    @Test
    void classicPlayerMovesAndStopsAtWaterAndBorderCollision() {
        RpgMap map = new RpgMap();
        RpgPlayerController player = new RpgPlayerController(map);
        player.setPosition(18 * RpgMap.TILE_SIZE + 16, 3 * RpgMap.TILE_SIZE + 16);
        double waterX = player.getX();
        for (int i = 0; i < 30; i++) {
            player.setRight(true);
            player.update();
        }
        player.setRight(false);
        assertTrue(player.getX() >= waterX);
        assertTrue(player.getX() < 19 * RpgMap.TILE_SIZE + 16,
                "o player não pode atravessar o lago do mapa mínimo");

        player.setPosition(16, 16);
        for (int i = 0; i < 120; i++) {
            player.setLeft(true);
            player.setUp(true);
            player.update();
        }
        player.setLeft(false);
        player.setUp(false);
        assertEquals(16.0, player.getX(), 0.01);
        assertEquals(16.0, player.getY(), 0.01);
    }

    @Test
    void characterProgressionIsIndependentFromShooterState() {
        RpgCharacterStats stats = RpgCharacterStats.create(RpgArchetype.ERRANTE);
        int initialLife = stats.getMaxLife();
        stats.gainExperience(1000);
        assertTrue(stats.getLevel() > 1);
        assertTrue(stats.getMaxLife() >= initialLife);
        assertTrue(stats.getAttributePoints() > 0);
        assertTrue(stats.spendAttributePoint("destreza"));
        assertTrue(stats.getDexterity() > RpgArchetype.ERRANTE.getDexterity());
        assertTrue(stats.serialize().containsKey("archetype"));
    }

    @Test
    void classicSceneRendersMapPlayerHudAndCharacterSheet() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        ClassicRpgMode mode = Game.getClassicRpgMode();
        BufferedImage image = new BufferedImage(Game.WIDTH * Game.SCALE,
                Game.HEIGHT * Game.SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        mode.render(graphics);
        mode.renderOverlay(graphics);
        game.keyPressed(key(game, KeyEvent.VK_C));
        mode.renderOverlay(graphics);
        graphics.dispose();
        assertNotEquals(0, image.getWidth());
    }

    @Test
    void classicSaveRoundTripRestoresMapCharacterAndPosition() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        ClassicRpgMode mode = Game.getClassicRpgMode();
        mode.getPlayer().setPosition(320, 300);
        for (int i = 0; i < 20; i++) mode.update();
        double savedX = mode.getPlayer().getX();
        double savedY = mode.getPlayer().getY();
        assertTrue(SaveManager.saveCurrentGame());

        game.returnToMainMenu();
        assertFalse(Game.isClassicRpgMode());
        assertTrue(SaveManager.loadSlot(1));
        assertTrue(Game.isClassicRpgMode());
        ClassicRpgMode restored = Game.getClassicRpgMode();
        assertEquals("vale_brumafolha", restored.getMapId());
        assertEquals(RpgArchetype.ARCANISTA, restored.getCharacter().getArchetype());
        assertEquals(savedX, restored.getPlayer().getX(), 0.01);
        assertEquals(savedY, restored.getPlayer().getY(), 0.01);
    }

    private static KeyEvent key(Game game, int keyCode) {
        return new KeyEvent(game, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                keyCode, KeyEvent.CHAR_UNDEFINED);
    }
}
