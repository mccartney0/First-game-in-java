package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.DungeonManager;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.World;
import com.traduvertgames.world.WorldWeatherManager;

class WorldFileLoadingAndDungeonExpeditionTest {
    private File relativeMap;

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
    }

    @AfterEach
    void tearDown() {
        if (relativeMap != null) {
            relativeMap.delete();
        }
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void existingDiskMapLoadsEvenWhenItsPathIsNotClasspathStyle() throws Exception {
        File directory = new File("build/world-file-loading-test");
        directory.mkdirs();
        relativeMap = new File(directory, "relative-map.png");
        BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, 0xFF000000);
            }
        }
        ImageIO.write(image, "png", relativeMap);

        new World(relativeMap.getPath());

        assertEquals(3, World.WIDTH);
        assertEquals(2, World.HEIGHT);
    }

    @Test
    void dungeonExpeditionStartsARealRuinsInstanceWithoutRegionalChain() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();

        game.startDungeonExpedition();

        assertTrue(DungeonManager.isInDungeon());
        assertEquals(RpgWorldManager.RegionType.RUINS, DungeonManager.getDungeonRegion());
        assertTrue(RpgWorldManager.isDungeonMode());
    }

    @Test
    void regionalAdventureDoesNotReceiveOpenWorldClimateTint() throws Exception {
        Game game = GameTestFixture.newIsolatedGame();
        game.loadRegionalAdventure(1);

        assertFalse(WorldWeatherManager.isActive());
        BufferedImage frame = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = frame.createGraphics();
        try {
            graphics.setColor(new Color(40, 180, 80));
            graphics.fillRect(0, 0, 20, 20);
            WorldWeatherManager.renderAmbient(graphics);
        } finally {
            graphics.dispose();
        }
        assertEquals(new Color(40, 180, 80).getRGB(), frame.getRGB(10, 10));
    }
}
