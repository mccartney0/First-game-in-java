package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.world.LargeRpgMapGenerator;
import com.traduvertgames.world.OpenWorldManager;
import com.traduvertgames.world.RpgWorldManager;

class OpenWorldManagerTest {

    @AfterEach
    void tearDown() {
        OpenWorldManager.reset();
        RpgWorldManager.disable();
    }

    @Test
    void giantMapHasMoreThanFourTimesTheRegionalSurface() throws Exception {
        File firstDir = new File("build/test-open-world-a");
        File secondDir = new File("build/test-open-world-b");
        File first = LargeRpgMapGenerator.generateOpenWorldDefault(1);
        File second = LargeRpgMapGenerator.generate(LargeRpgMapGenerator.OPEN_WORLD_WIDTH,
                LargeRpgMapGenerator.OPEN_WORLD_HEIGHT, 1, 0x0F3A0B1DL + 4099L, secondDir);
        BufferedImage image = ImageIO.read(first);

        assertEquals(LargeRpgMapGenerator.OPEN_WORLD_WIDTH, image.getWidth());
        assertEquals(LargeRpgMapGenerator.OPEN_WORLD_HEIGHT, image.getHeight());
        assertTrue(LargeRpgMapGenerator.validate(image));
        assertTrue(first.isFile());
        assertTrue(new File(first.getParentFile(), first.getName().replace(".png", ".json")).isFile());
        assertTrue(Arrays.equals(Files.readAllBytes(first.toPath()), Files.readAllBytes(second.toPath())));
        assertTrue(image.getWidth() * image.getHeight() > 4 * 192 * 128);
    }

    @Test
    void chunkDiscoveryAndRoundTripAreDeterministic() {
        OpenWorldManager.configure(512, 320, 1234L);
        assertEquals(8, OpenWorldManager.getChunkColumns());
        assertEquals(5, OpenWorldManager.getChunkRows());
        assertTrue(OpenWorldManager.updatePlayerPosition(8 * 16, 8 * 16));
        assertTrue(OpenWorldManager.updatePlayerPosition(64 * 16, 0));
        assertEquals(2, OpenWorldManager.getDiscoveredChunkCount());

        Map<String, Object> snapshot = OpenWorldManager.serialize();
        OpenWorldManager.reset();
        OpenWorldManager.deserialize(snapshot);

        assertTrue(OpenWorldManager.isActive());
        assertEquals(2, OpenWorldManager.getDiscoveredChunkCount());
        assertEquals(40, OpenWorldManager.getTotalChunkCount());
        assertEquals("Setores 2/40", OpenWorldManager.getExplorationLabel());
    }
}
