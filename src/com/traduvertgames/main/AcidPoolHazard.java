package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Graphics;

/** Regra ambiental do Pântano Ácido: poças fixas por onda e semente lógica. */
public final class AcidPoolHazard {

    public static final int GRID_SIZE = 32;
    public static final int TICK_FRAMES = 42;

    private AcidPoolHazard() {
    }

    public static boolean isPoolAt(int worldX, int worldY, int wave) {
        int tileX = Math.floorDiv(worldX, GRID_SIZE);
        int tileY = Math.floorDiv(worldY, GRID_SIZE);
        int signature = Math.floorMod(tileX * 31 + tileY * 17 + Math.max(0, wave - 1) * 7, 11);
        return signature == 2 || signature == 7;
    }

    public static double damageForStage(int stage) {
        return 1.8 + Math.max(0, stage - 6) * 0.35;
    }

    public static void render(Graphics g, int cameraX, int cameraY, int screenWidth, int screenHeight, int wave,
            int scale) {
        int startX = Math.max(0, cameraX / GRID_SIZE * GRID_SIZE - GRID_SIZE);
        int startY = Math.max(0, cameraY / GRID_SIZE * GRID_SIZE - GRID_SIZE);
        for (int worldX = startX; worldX < cameraX + screenWidth + GRID_SIZE; worldX += GRID_SIZE) {
            for (int worldY = startY; worldY < cameraY + screenHeight + GRID_SIZE; worldY += GRID_SIZE) {
                if (!isPoolAt(worldX, worldY, wave)) {
                    continue;
                }
                int drawX = (worldX - cameraX) * scale;
                int drawY = (worldY - cameraY) * scale;
                g.setColor(new Color(124, 179, 66, 70));
                g.fillOval(drawX, drawY, 24 * scale, 16 * scale);
                g.setColor(new Color(205, 220, 57, 160));
                g.drawOval(drawX, drawY, 24 * scale, 16 * scale);
            }
        }
    }
}
