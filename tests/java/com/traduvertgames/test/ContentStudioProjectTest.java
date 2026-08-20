package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.graficos.AssetCatalog;
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

    @Test
    void studioSerializesCustomTileAndEnemyProperties() throws Exception {
        ContentStudioProject.TileProperties tileProperties = new ContentStudioProject.TileProperties(false, 3, "ancient_ruins");
        File tile = ContentStudioProject.generateTile(ContentStudioProject.TileStyle.RUINAS, "ruina_teste", 2,
                tileProperties, root);
        String tileManifest = ContentStudioProject.readManifestFor(tile);
        assertTrue(tileManifest.contains("\"variation\": 2"));
        assertTrue(tileManifest.contains("\"walkable\": false"));
        assertTrue(tileManifest.contains("\"movementCost\": 3"));
        assertTrue(tileManifest.contains("\"terrainTag\": \"ancient_ruins\""));

        ContentStudioProject.EnemyProperties enemyProperties = new ContentStudioProject.EnemyProperties(27, 8, 0.7, "boss_guardian");
        File enemy = ContentStudioProject.generateEnemySprite(ContentStudioProject.EnemyRole.GUARDIAN,
                null, null, enemyProperties, root);
        String enemyManifest = ContentStudioProject.readManifestFor(enemy);
        assertTrue(enemyManifest.contains("\"baseLife\": 27"));
        assertTrue(enemyManifest.contains("\"baseDamage\": 8"));
        assertTrue(enemyManifest.contains("\"speed\": 0.7"));
        assertTrue(enemyManifest.contains("\"behaviorTag\": \"boss_guardian\""));
    }

    @Test
    void valleyCanLoadDefaultContentStudioGrassAndGuardianAssets() {
        BufferedImage grass = AssetCatalog.contentTile("brumafolha_grass");
        assertNotNull(grass);
        assertEquals(32, grass.getWidth());
        assertEquals(32, grass.getHeight());
        assertNotNull(AssetCatalog.enemySprite(Enemy.Variant.GUARDIAN));
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
