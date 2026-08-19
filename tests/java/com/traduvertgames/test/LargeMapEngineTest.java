package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.engine.RpgExpansionEngine;
import com.traduvertgames.graficos.AssetCatalog;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.world.LargeRpgMapGenerator;
import com.traduvertgames.world.RpgWorldManager;

class LargeMapEngineTest {

    @AfterEach
    void tearDown() {
        RpgWorldManager.disable();
    }

    @Test
    void largeMapIsDeterministicAndEmitsManifest() throws Exception {
        File firstDir = new File("build/test-large-map-a");
        File secondDir = new File("build/test-large-map-b");
        File first = LargeRpgMapGenerator.generate(192, 128, 3, 987654L, firstDir);
        File second = LargeRpgMapGenerator.generate(192, 128, 3, 987654L, secondDir);
        BufferedImage image = ImageIO.read(first);

        assertEquals(192, image.getWidth());
        assertEquals(128, image.getHeight());
        assertTrue(LargeRpgMapGenerator.validate(image));
        assertTrue(new File(first.getParentFile(), first.getName().replace(".png", ".json")).isFile());
        assertTrue(Arrays.equals(Files.readAllBytes(first.toPath()), Files.readAllBytes(second.toPath())));
    }

    @Test
    void expansionCatalogScalesRegionsAndPoisToLargeWorld() {
        assertEquals("REFUGE", RpgExpansionEngine.regionForTile(5, 5, 192, 128).getId());
        assertEquals("CORE", RpgExpansionEngine.regionForTile(180, 116, 192, 128).getId());
        assertEquals(10, RpgExpansionEngine.defaultPois().size());
        assertTrue(RpgExpansionEngine.enemyBudget(192, 128, 4) > RpgExpansionEngine.enemyBudget(96, 64, 4));
    }

    @Test
    void generatedWeaponIconsAreLoadable() {
        assertNotNull(AssetCatalog.weaponIcon(WeaponType.BLASTER));
        assertNotNull(AssetCatalog.weaponIcon(WeaponType.ION_RIFLE));
        assertNotNull(AssetCatalog.weaponIcon(WeaponType.SCATTER_CANNON));
        assertNotNull(AssetCatalog.weaponIcon(WeaponType.FUSION_LANCE));
        assertNotNull(AssetCatalog.weaponIcon(WeaponType.VOID_MORTAR));
    }
}
