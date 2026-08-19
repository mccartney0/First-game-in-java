package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.graficos.SectorEntryOverlay;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.world.OpenWorldManager;
import com.traduvertgames.world.OpenWorldMarkerManager;

class OpenWorldExplorationUxTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Game.getInstance().loadOpenWorld(1);
        Game.gameState = "NORMAL";
        OpenWorldManager.updatePlayerPosition(Game.player.getX(), Game.player.getY());
    }

    @AfterEach
    void tearDown() {
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void markerTogglesPerSectorAndSurvivesSerialization() {
        assertEquals("A1", OpenWorldManager.getActiveSectorCode());
        assertEquals(OpenWorldMarkerManager.Change.ADDED,
                OpenWorldMarkerManager.toggleAtPixel(Game.player.getX(), Game.player.getY()));
        assertTrue(OpenWorldMarkerManager.hasMarkerInSector(0, 0));
        assertEquals(OpenWorldMarkerManager.Change.REMOVED,
                OpenWorldMarkerManager.toggleAtPixel(Game.player.getX(), Game.player.getY()));
        assertFalse(OpenWorldMarkerManager.hasMarkerInSector(0, 0));

        assertEquals(OpenWorldMarkerManager.Change.ADDED,
                OpenWorldMarkerManager.toggleAtPixel(70 * 16, 70 * 16));
        Map<String, Object> snapshot = OpenWorldMarkerManager.serialize();
        OpenWorldMarkerManager.reset();
        OpenWorldMarkerManager.deserialize(snapshot);

        assertEquals(1, OpenWorldMarkerManager.getMarkerCount());
        assertTrue(OpenWorldMarkerManager.hasMarkerInSector(1, 1));
    }

    @Test
    void markerRoundTripUsesTheOpenWorldSaveSession() throws Exception {
        OpenWorldMarkerManager.toggleAtPixel(Game.player.getX(), Game.player.getY());
        SaveManager.activeSlot = 1;
        assertTrue(SaveManager.saveCurrentGame());
        OpenWorldMarkerManager.reset();

        assertTrue(SaveManager.loadSlot(1));
        assertTrue(OpenWorldMarkerManager.hasMarkerInSector(0, 0));
    }

    @Test
    void sectorOverlayFadesAfterItsShortDisplayWindow() {
        SectorEntryOverlay.show("A1", "Refúgio da Colônia");
        assertTrue(SectorEntryOverlay.isShowing());
        for (int frame = 0; frame < 8; frame++) {
            SectorEntryOverlay.update();
        }
        BufferedImage image = new BufferedImage(Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            SectorEntryOverlay.render(graphics);
        } finally {
            graphics.dispose();
        }

        int coloredPixels = 0;
        for (int y = 0; y < image.getHeight() / 4; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 0) {
                    coloredPixels++;
                }
            }
        }
        assertTrue(coloredPixels > 0);
        for (int frame = 0; frame < 130; frame++) {
            SectorEntryOverlay.update();
        }
        assertFalse(SectorEntryOverlay.isShowing());
    }
}
