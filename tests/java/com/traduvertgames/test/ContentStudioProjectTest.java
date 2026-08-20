package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.tools.ContentStudioProject;

class ContentStudioProjectTest {
    private final File root = new File("build/test-content-studio");

    @AfterEach
    void clean() {
        delete(root);
    }

    @Test
    void studioExportsMapTileSpriteAndInspectableManifests() throws Exception {
        File map = ContentStudioProject.generateMap(ContentStudioProject.MapKind.REGIONAL,
                96, 64, 2, 1919L, root);
        assertTrue(map.isFile());
        assertTrue(ContentStudioProject.readManifestFor(map).contains("\"seed\": 1919"));

        File tile = ContentStudioProject.generateTile(ContentStudioProject.TileStyle.TECNOLOGIA, "piso_teste", root);
        BufferedImage tileImage = ImageIO.read(tile);
        assertEquals(32, tileImage.getWidth());
        assertEquals(32, tileImage.getHeight());
        assertTrue(ContentStudioProject.readManifestFor(tile).contains("\"kind\": \"tile\""));

        File enemy = ContentStudioProject.generateEnemySprite(ContentStudioProject.EnemyRole.BOMBER,
                new Color(90, 80, 60), new Color(240, 190, 50), root);
        BufferedImage enemyImage = ImageIO.read(enemy);
        assertEquals(32, enemyImage.getWidth());
        assertEquals(0, enemyImage.getRGB(0, 0) >>> 24);
        assertTrue(ContentStudioProject.readManifestFor(enemy).contains("\"kind\": \"enemy\""));
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
