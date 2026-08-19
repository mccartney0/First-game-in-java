package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.graficos.AssetCatalog;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;

class EnemyRenderVisualTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Camera.x = 0;
        Camera.y = 0;
    }

    @AfterEach
    void tearDown() {
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void damagedEnemyKeepsReadableTransparentSprite() throws Exception {
        BufferedImage sprite = AssetCatalog.enemySprite(Enemy.Variant.SCOUT);
        assertNotNull(sprite);
        Enemy enemy = new Enemy(96, 96, 16, 16, Entity.ENEMY_EN, Enemy.Variant.SCOUT);
        enemy.takeDamageDirect(0.5);
        BufferedImage canvas = new BufferedImage(192, 144, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        enemy.render(graphics);
        graphics.dispose();

        boolean darkSpritePixel = false;
        for (int y = 88; y < 112 && !darkSpritePixel; y++) {
            for (int x = 88; x < 112; x++) {
                int argb = canvas.getRGB(x, y);
                int alpha = argb >>> 24;
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;
                if (alpha > 32 && red < 120 && green < 120 && blue < 120) {
                    darkSpritePixel = true;
                    break;
                }
            }
        }
        assertTrue(darkSpritePixel, "o flash de dano não pode substituir o corpo do monstro");

        File output = new File("build/visual-qa/enemy_damage_flash.png");
        output.getParentFile().mkdirs();
        ImageIO.write(canvas, "png", output);
        assertTrue(output.isFile());
    }
}
