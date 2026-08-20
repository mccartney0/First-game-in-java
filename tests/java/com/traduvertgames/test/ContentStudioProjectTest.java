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
    void studioExportsExplicitAiProfilesThatMapToRuntimeVariants() throws Exception {
        GameTestFixture.newIsolatedGame();
        ContentStudioProject.EnemyProperties sniper = ContentStudioProject.EnemyProperties.defaults(
                ContentStudioProject.EnemyRole.SNIPER);
        File sprite = ContentStudioProject.generateEnemySprite(ContentStudioProject.EnemyRole.SNIPER,
                null, null, sniper, root);
        assertTrue(ContentStudioProject.readManifestFor(sprite).contains("\"behaviorTag\": \"snipe\""));
        assertEquals(Enemy.Variant.SNIPER, Enemy.variantForContentBehavior(sniper.behaviorTag));
        assertEquals(Enemy.Variant.SAPPER, Enemy.variantForContentBehavior("ambush"));
        assertEquals(Enemy.Variant.PHANTOM, Enemy.variantForContentBehavior("drain"));
        assertEquals(Enemy.Variant.GUARDIAN, Enemy.variantForContentBehavior("regenerate"));
    }

    @Test
    void studioExportsConfigurableOutlandEnemyPack() throws Exception {
        File[] pack = ContentStudioProject.generateOutlandEnemyPack(root);
        assertEquals(3, pack.length);
        assertEquals("enemy_mire_hound.png", pack[0].getName());
        assertEquals("enemy_bog_oracle.png", pack[1].getName());
        assertEquals("enemy_mire_brute.png", pack[2].getName());
        assertTrue(ContentStudioProject.readManifestFor(pack[0]).contains("\"behaviorTag\": \"pounce\""));
        assertTrue(ContentStudioProject.readManifestFor(pack[1]).contains("\"behaviorTag\": \"hex\""));
        assertTrue(ContentStudioProject.readManifestFor(pack[2]).contains("\"behaviorTag\": \"fortify\""));
        for (File enemy : pack) {
            assertEquals(32, ImageIO.read(enemy).getWidth());
            assertEquals(0, ImageIO.read(enemy).getRGB(0, 0) >>> 24);
        }
    }

    @Test
    void valleyCanLoadDefaultContentStudioGrassAndGuardianAssets() {
        BufferedImage grass = AssetCatalog.contentTile("brumafolha_grass");
        assertNotNull(grass);
        assertEquals(32, grass.getWidth());
        assertEquals(32, grass.getHeight());
        assertNotNull(AssetCatalog.enemySprite(Enemy.Variant.GUARDIAN));
        assertNotNull(AssetCatalog.contentEnemySprite("enemy_mire_hound"));
    }

    @Test
    void studioExportsTheCompleteBrumafolhaRuntimeTerrainPack() throws Exception {
        File[] pack = ContentStudioProject.generateBrumafolhaTerrainPack(root);
        assertEquals(10, pack.length);
        for (File tile : pack) {
            assertTrue(tile.isFile());
            BufferedImage image = ImageIO.read(tile);
            assertEquals(32, image.getWidth());
            assertEquals(32, image.getHeight());
            assertTrue(ContentStudioProject.readManifestFor(tile).contains("\"kind\": \"tile\""));
        }
        assertEquals("brumafolha_grass_0.png", pack[0].getName());
        assertEquals("brumafolha_road_0.png", pack[4].getName());
        assertEquals("brumafolha_ruins_2.png", pack[9].getName());
    }

    @Test
    void studioExportsConsumableWeaponAndRpgContentCatalog() throws Exception {
        ContentStudioProject.ConsumableProperties elixirProperties = new ContentStudioProject.ConsumableProperties(
                "Elixir Teste", ContentStudioProject.ConsumableEffect.TRIAGEM, 31, 17, 19);
        File elixir = ContentStudioProject.generateConsumable("elixir_teste", elixirProperties, root);
        assertEquals(32, ImageIO.read(elixir).getWidth());
        String elixirManifest = ContentStudioProject.readManifestFor(elixir);
        assertTrue(elixirManifest.contains("\"kind\": \"consumable\""));
        assertTrue(elixirManifest.contains("\"lifeRestore\": 31"));
        assertTrue(elixirManifest.contains("\"displayName\": \"Elixir Teste\""));

        ContentStudioProject.RpgWeaponProperties bladeProperties = new ContentStudioProject.RpgWeaponProperties(
                "Lâmina Teste", 4, 10, "rare");
        File blade = ContentStudioProject.generateRpgWeapon("lamina_teste", ContentStudioProject.RpgWeaponStyle.ESPADA,
                bladeProperties, root);
        String bladeManifest = ContentStudioProject.readManifestFor(blade);
        assertTrue(bladeManifest.contains("\"kind\": \"rpg_weapon\""));
        assertTrue(bladeManifest.contains("\"damageBonus\": 4"));
        assertTrue(new File(root, "res/assets/generated/rpg_content_catalog.json").isFile());
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
