package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.graficos.MiniMap;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.OpenWorldManager;

/** Regressão do mini-mapa de setores: mapa grande sem varredura de tiles por frame. */
class OpenWorldMiniMapTest {

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
    void miniMapRendersDiscoveredSectorLayerForOpenWorld() {
        BufferedImage canvas = new BufferedImage(Game.WIDTH * Game.SCALE,
                Game.HEIGHT * Game.SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            MiniMap.render(graphics);
        } finally {
            graphics.dispose();
        }

        int cyanPixels = 0;
        for (int y = 0; y < canvas.getHeight(); y++) {
            for (int x = canvas.getWidth() * 2 / 3; x < canvas.getWidth(); x++) {
                int argb = canvas.getRGB(x, y);
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;
                if (green > 150 && blue > 150 && red < 130) {
                    cyanPixels++;
                }
            }
        }
        assertTrue(OpenWorldManager.getDiscoveredChunkCount() >= 1);
        assertTrue(cyanPixels > 0, "mini-mapa deve sinalizar setores descobertos e posição do piloto");
    }
}

