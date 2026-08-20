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
import com.traduvertgames.graficos.MissionHud;
import com.traduvertgames.graficos.UI;
import com.traduvertgames.rpg.ClassicRpgMode;
import com.traduvertgames.rpg.RpgArchetype;
import com.traduvertgames.rpg.RpgCombatEnemy;
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
		assertEquals("rpg", menu.getGameModeLabelForTest(3));
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
    void classicModeSuppressesShooterHudAndUsesStableTerrainVariants() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        BufferedImage overlay = new BufferedImage(Game.WIDTH * Game.SCALE,
                Game.HEIGHT * Game.SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = overlay.createGraphics();
        new UI().renderOverlay(graphics);
        MissionHud.render(graphics);
        graphics.dispose();
        assertEquals(0, overlay.getRGB(0, 0) >>> 24);
        assertEquals(0, overlay.getRGB(40, 40) >>> 24);

        int first = RpgMap.terrainVariantFor('g', 7, 9, 4);
        assertEquals(first, RpgMap.terrainVariantFor('g', 7, 9, 4));
        assertTrue(first >= 0 && first < 4);
        assertTrue(RpgMap.terrainVariantFor('.', 13, 11, 3) < 3);
        assertTrue(RpgMap.terrainVariantFor('r', 28, 17, 3) < 3);
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

    @Test
    void rpgOwnsPauseAndInventoryWithoutOpeningTheShooterMenu() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        ClassicRpgMode mode = Game.getClassicRpgMode();

        game.keyPressed(key(game, KeyEvent.VK_I));
        assertEquals("INVENTORY", mode.getRpgPanelForTest());
        int herbs = mode.getHerbCountForTest();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertEquals(herbs - 1, mode.getHerbCountForTest());
        game.keyPressed(key(game, KeyEvent.VK_ESCAPE));
        assertEquals("NONE", mode.getRpgPanelForTest());

        game.keyPressed(key(game, KeyEvent.VK_ESCAPE));
        assertEquals("PAUSE", mode.getRpgPanelForTest());
        assertEquals("NORMAL", Game.gameState);
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertEquals("NONE", mode.getRpgPanelForTest());
        assertTrue(Game.isClassicRpgMode());

        game.keyPressed(key(game, KeyEvent.VK_ESCAPE));
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertFalse(Game.isClassicRpgMode());
        assertEquals("MENU", Game.gameState);
    }

    @Test
    void classicQuestHasGuideCombatRewardAndPersistsItsStage() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        ClassicRpgMode mode = Game.getClassicRpgMode();

        mode.getPlayer().setPosition(mode.getMap().getVillageGuideX(), mode.getMap().getVillageGuideY());
        game.keyPressed(key(game, KeyEvent.VK_R));
        assertEquals("FIND_GUIDE", mode.getQuestStageForTest());
        for (int i = 0; i < 5; i++) game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertEquals("DEFEAT_WARDEN", mode.getQuestStageForTest());

        mode.getPlayer().setPosition(mode.getMap().getWardenX(), mode.getMap().getWardenY());
        for (int i = 0; i < 3; i++) game.keyPressed(key(game, KeyEvent.VK_SPACE));
        assertEquals(0, mode.getWardenLifeForTest());
        assertEquals("RETURN_TO_GUIDE", mode.getQuestStageForTest());

        mode.getPlayer().setPosition(mode.getMap().getVillageGuideX(), mode.getMap().getVillageGuideY());
        game.keyPressed(key(game, KeyEvent.VK_E));
        for (int i = 0; i < 3; i++) game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertEquals("COMPLETE", mode.getQuestStageForTest());
        assertTrue(mode.getCharacter().getExperience() > 0);
        assertEquals("COMPLETE", mode.serialize().get("questStage"));

        assertTrue(SaveManager.saveCurrentGame());
        game.returnToMainMenu();
        assertTrue(SaveManager.loadSlot(1));
        assertEquals("COMPLETE", Game.getClassicRpgMode().getQuestStageForTest());
    }

    @Test
    void rpgCollectsEquipsAndPersistsTheBellRelic() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        ClassicRpgMode mode = Game.getClassicRpgMode();

        mode.getPlayer().setPosition(mode.getMap().getBellRelicX(), mode.getMap().getBellRelicY());
        game.keyPressed(key(game, KeyEvent.VK_R));
        assertEquals(1, mode.getBellRelicCountForTest());

        game.keyPressed(key(game, KeyEvent.VK_I));
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertTrue(mode.isBellCharmEquippedForTest());
        game.keyPressed(key(game, KeyEvent.VK_ESCAPE));

        assertTrue(SaveManager.saveCurrentGame());
        game.returnToMainMenu();
        assertTrue(SaveManager.loadSlot(1));
        assertEquals(1, Game.getClassicRpgMode().getBellRelicCountForTest());
        assertTrue(Game.getClassicRpgMode().isBellCharmEquippedForTest());
    }

    @Test
    void rpgUsesAndPersistsContentStudioConsumableAndWeapon() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        ClassicRpgMode mode = Game.getClassicRpgMode();

        game.keyPressed(key(game, KeyEvent.VK_I));
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        int elixirs = mode.getBrumaElixirCountForTest();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertEquals(elixirs - 1, mode.getBrumaElixirCountForTest());
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_DOWN));
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertTrue(mode.isBrumaBladeEquippedForTest());
        game.keyPressed(key(game, KeyEvent.VK_ESCAPE));

        assertTrue(SaveManager.saveCurrentGame());
        game.returnToMainMenu();
        assertTrue(SaveManager.loadSlot(1));
        assertEquals(0, Game.getClassicRpgMode().getBrumaElixirCountForTest());
        assertTrue(Game.getClassicRpgMode().isBrumaBladeEquippedForTest());
    }

    @Test
    void outlandEnemyAwardsLootExperienceAndPersistsDefeat() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        ClassicRpgMode mode = Game.getClassicRpgMode();
        mode.getPlayer().setPosition(mode.getMap().getStalkerX(), mode.getMap().getStalkerY());
        int elixirs = mode.getBrumaElixirCountForTest();
        for (int hit = 0; hit < 5; hit++) game.keyPressed(key(game, KeyEvent.VK_SPACE));
        assertTrue(mode.isStalkerDefeatedForTest());
        assertEquals(2, mode.getOutlandEnemyCountForTest());
        assertEquals(elixirs + 1, mode.getBrumaElixirCountForTest());
        assertTrue(Game.getClassicRpgMode().getCharacter().getExperience() > 0);

        assertTrue(SaveManager.saveCurrentGame());
        game.returnToMainMenu();
        assertTrue(SaveManager.loadSlot(1));
        assertTrue(Game.getClassicRpgMode().isStalkerDefeatedForTest());
        assertEquals(2, Game.getClassicRpgMode().getOutlandEnemyCountForTest());
    }

    @Test
    void outlandScoutQuestClearsThreatsRewardsAndPersists() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        ClassicRpgMode mode = Game.getClassicRpgMode();

        mode.getPlayer().setPosition(mode.getMap().getOutlandScoutX(), mode.getMap().getOutlandScoutY());
        game.keyPressed(key(game, KeyEvent.VK_R));
        for (int advance = 0; advance < 3; advance++) game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertEquals("CLEAR_THREATS", mode.getOutlandQuestStageForTest());

        mode.getPlayer().setPosition(mode.getMap().getStalkerX(), mode.getMap().getStalkerY());
        for (int hit = 0; hit < 5; hit++) game.keyPressed(key(game, KeyEvent.VK_SPACE));
        mode.getPlayer().setPosition(mode.getMap().getSniperX(), mode.getMap().getSniperY());
        for (int hit = 0; hit < 5; hit++) game.keyPressed(key(game, KeyEvent.VK_SPACE));
        assertEquals("RETURN_TO_SCOUT", mode.getOutlandQuestStageForTest());

        mode.getPlayer().setPosition(mode.getMap().getOutlandScoutX(), mode.getMap().getOutlandScoutY());
        game.keyPressed(key(game, KeyEvent.VK_E));
        for (int advance = 0; advance < 3; advance++) game.keyPressed(key(game, KeyEvent.VK_ENTER));
        assertEquals("COMPLETE", mode.getOutlandQuestStageForTest());
        assertEquals(4, mode.getBrumaElixirCountForTest());
        assertEquals(4, mode.getOutlandEnemyCountForTest());
        assertTrue(mode.hasOutlandEnemyKindForTest(RpgCombatEnemy.Kind.MIRE_HOUND));
        assertTrue(mode.hasOutlandEnemyKindForTest(RpgCombatEnemy.Kind.BOG_ORACLE));
        assertTrue(mode.hasOutlandEnemyKindForTest(RpgCombatEnemy.Kind.MIRE_BRUTE));

        int defenseBeforeChest = mode.getCharacter().getPhysicalDefense();
        mode.getPlayer().setPosition(mode.getMap().getOutlandChestX(), mode.getMap().getOutlandChestY());
        game.keyPressed(key(game, KeyEvent.VK_R));
        assertTrue(mode.isOutlandChestOpenedForTest());
        assertEquals(6, mode.getBrumaElixirCountForTest());
        assertEquals(defenseBeforeChest + 1, mode.getCharacter().getPhysicalDefense());

        assertTrue(SaveManager.saveCurrentGame());
        game.returnToMainMenu();
        assertTrue(SaveManager.loadSlot(1));
        assertEquals("COMPLETE", Game.getClassicRpgMode().getOutlandQuestStageForTest());
        assertTrue(Game.getClassicRpgMode().isOutlandChestOpenedForTest());
        assertEquals(defenseBeforeChest + 1, Game.getClassicRpgMode().getCharacter().getPhysicalDefense());
    }

    @Test
    void outlandGateMarksDiscoveryWhenThePlayerEntersTheRegion() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.startClassicRpg();
        game.keyPressed(key(game, KeyEvent.VK_ENTER));
        ClassicRpgMode mode = Game.getClassicRpgMode();
        mode.getPlayer().setPosition(mode.getMap().getOutlandGateX(), mode.getMap().getOutlandGateY());
        mode.update();
        assertTrue(mode.hasSeenOutlandEntranceForTest());
        assertTrue(Boolean.TRUE.equals(mode.serialize().get("outlandEntranceSeen")));
    }

    private static KeyEvent key(Game game, int keyCode) {
        return new KeyEvent(game, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                keyCode, KeyEvent.CHAR_UNDEFINED);
    }
}
