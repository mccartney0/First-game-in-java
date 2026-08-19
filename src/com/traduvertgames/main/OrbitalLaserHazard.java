package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Graphics;

/** Regra ambiental da Fábrica Orbital: faixas cruzadas de disparo por onda. */
public final class OrbitalLaserHazard {

    public static final int GRID_SIZE = 32;
    public static final int TICK_FRAMES = 34;

    private OrbitalLaserHazard() {
    }

    public static boolean isLaserAt(int worldX, int worldY, int wave) {
        int tileX = Math.floorDiv(worldX, GRID_SIZE);
        int tileY = Math.floorDiv(worldY, GRID_SIZE);
        return Math.floorMod(tileX + Math.max(0, wave - 1), 5) == 0
                || Math.floorMod(tileY * 2 + wave, 7) == 0;
    }

    public static double damageForStage(int stage) {
        return 2.4 + Math.max(0, stage - 7) * 0.45;
    }

    public static void render(Graphics g, int screenWidth, int screenHeight, int wave, int scale) {
        int verticalLane = Math.floorMod(wave * 43, Math.max(1, screenWidth));
        int horizontalLane = Math.floorMod(wave * 29, Math.max(1, screenHeight));
        g.setColor(new Color(0, 229, 255, 125));
        g.fillRect(verticalLane, 0, 3 * scale, screenHeight);
        g.setColor(new Color(255, 82, 82, 105));
        g.fillRect(0, horizontalLane, screenWidth, 2 * scale);
    }
}
