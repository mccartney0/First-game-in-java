package com.traduvertgames.rpg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import com.traduvertgames.graficos.AssetCatalog;

/** Mapa mínimo construído por intenção para a Rodada 1; a renderização é substituível por tiles. */
public final class RpgMap {
    public static final int TILE_SIZE = 32;
    public static final int WIDTH_TILES = 36;
    public static final int HEIGHT_TILES = 24;
    public static final int VILLAGE_GUIDE_X = 10 * TILE_SIZE + TILE_SIZE / 2;
    public static final int VILLAGE_GUIDE_Y = 8 * TILE_SIZE + TILE_SIZE / 2;
    public static final int WELL_X = 7 * TILE_SIZE + TILE_SIZE / 2;
    public static final int WELL_Y = 11 * TILE_SIZE + TILE_SIZE / 2;
    public static final int WARDEN_X = 24 * TILE_SIZE + TILE_SIZE / 2;
    public static final int WARDEN_Y = 11 * TILE_SIZE + TILE_SIZE / 2;
    private static final int WIDTH = WIDTH_TILES * TILE_SIZE;
    private static final int HEIGHT = HEIGHT_TILES * TILE_SIZE;

    public boolean isWalkable(double x, double y, int entityWidth, int entityHeight) {
        int left = (int) Math.floor((x - entityWidth / 2.0) / TILE_SIZE);
        int right = (int) Math.floor((x + entityWidth / 2.0 - 1) / TILE_SIZE);
        int top = (int) Math.floor((y - entityHeight / 2.0) / TILE_SIZE);
        int bottom = (int) Math.floor((y + entityHeight / 2.0 - 1) / TILE_SIZE);
        return isWalkableTile(left, top) && isWalkableTile(right, top)
                && isWalkableTile(left, bottom) && isWalkableTile(right, bottom);
    }

    private boolean isWalkableTile(int x, int y) {
        if (x < 1 || y < 1 || x >= WIDTH_TILES - 1 || y >= HEIGHT_TILES - 1) return false;
        char tile = tileAt(x, y);
        return tile != '#' && tile != '~' && tile != 'H';
    }

    private char tileAt(int x, int y) {
        if (x <= 0 || y <= 0 || x >= WIDTH_TILES - 1 || y >= HEIGHT_TILES - 1) return '#';
        if (x >= 19 && x <= 22 && y >= 2 && y <= 5) return '~';
        if (x >= 26 && y >= 15) return 'r';
        if ((y == 11 || y == 12) || (x == 12 || x == 13) && y >= 8 && y <= 13) return '.';
        if (x >= 2 && x <= 11 && y >= 5 && y <= 10) return 'v';
        if (x >= 25 && y <= 12) return 'f';
        if (x >= 26 && y >= 13) return 'r';
        return 'g';
    }

    public int getPixelWidth() { return WIDTH; }
    public int getPixelHeight() { return HEIGHT; }
    public double getSpawnX() { return 7 * TILE_SIZE + TILE_SIZE / 2.0; }
    public double getSpawnY() { return 8 * TILE_SIZE + TILE_SIZE / 2.0; }
    public double getVillageGuideX() { return VILLAGE_GUIDE_X; }
    public double getVillageGuideY() { return VILLAGE_GUIDE_Y; }
    public double getWellX() { return WELL_X; }
    public double getWellY() { return WELL_Y; }
    public double getWardenX() { return WARDEN_X; }
    public double getWardenY() { return WARDEN_Y; }
    public String getDisplayName() { return "Vale de Brumafolha"; }

    public void render(Graphics g, double cameraX, double cameraY, int viewportWidth, int viewportHeight) {
        int firstX = Math.max(0, (int) Math.floor(cameraX / TILE_SIZE) - 1);
        int firstY = Math.max(0, (int) Math.floor(cameraY / TILE_SIZE) - 1);
        int lastX = Math.min(WIDTH_TILES, firstX + viewportWidth / TILE_SIZE + 3);
        int lastY = Math.min(HEIGHT_TILES, firstY + viewportHeight / TILE_SIZE + 3);
        for (int y = firstY; y < lastY; y++) {
            for (int x = firstX; x < lastX; x++) {
                char tile = tileAt(x, y);
                int drawX = x * TILE_SIZE - (int) cameraX;
                int drawY = y * TILE_SIZE - (int) cameraY;
                BufferedImage texture = terrainTexture(tile, x, y);
                if (texture != null) {
                    g.drawImage(texture, drawX, drawY, TILE_SIZE, TILE_SIZE, null);
                } else {
                    g.setColor(colorFor(tile));
                    g.fillRect(drawX, drawY, TILE_SIZE + 1, TILE_SIZE + 1);
                }
                if (texture == null) drawTileDetail(g, tile, drawX, drawY, x, y);
            }
        }
        drawLandmarkLabels(g, cameraX, cameraY);
    }

    private Color colorFor(char tile) {
        switch (tile) {
        case '#': return new Color(45, 48, 55);
        case '~': return new Color(47, 115, 152);
        case '.': return new Color(157, 125, 78);
        case 'v': return new Color(111, 151, 83);
        case 'f': return new Color(55, 112, 72);
        case 'r': return new Color(103, 91, 87);
        default: return new Color(91, 139, 76);
        }
    }

    /**
     * A variante é derivada apenas da posição. Assim não há cintilação entre
     * frames, save/load ou máquinas, e a grade física continua 32×32.
     */
    public static int terrainVariantFor(char tile, int tileX, int tileY, int variantCount) {
        if (variantCount <= 1) return 0;
        int hash = tileX * 73856093 ^ tileY * 19349663 ^ tile * 83492791;
        return Math.floorMod(hash, variantCount);
    }

    private BufferedImage terrainTexture(char tile, int tileX, int tileY) {
        String prefix;
        int variants;
        if (tile == 'g') {
            prefix = "brumafolha_grass";
            variants = 4;
        } else if (tile == '.') {
            prefix = "brumafolha_road";
            variants = 3;
        } else if (tile == 'r') {
            prefix = "brumafolha_ruins";
            variants = 3;
        } else {
            return null;
        }
        int variant = terrainVariantFor(tile, tileX, tileY, variants);
        BufferedImage texture = AssetCatalog.contentTile(prefix + "_" + variant);
        if (texture == null && tile == 'g') texture = AssetCatalog.contentTile("brumafolha_grass");
        return texture;
    }

    private void drawTileDetail(Graphics g, char tile, int x, int y, int tileX, int tileY) {
        if (tile == 'f' && (tileX + tileY) % 3 == 0) {
            g.setColor(new Color(28, 78, 50));
            g.fillOval(x + 7, y + 5, 18, 18);
            g.setColor(new Color(89, 61, 40));
            g.fillRect(x + 14, y + 19, 4, 10);
        } else if (tile == 'v' && tileX % 4 == 0 && tileY % 3 == 0) {
            g.setColor(new Color(132, 77, 51));
            g.fillRect(x + 4, y + 8, 24, 20);
            g.setColor(new Color(178, 61, 45));
            g.fillPolygon(new int[] {x + 2, x + 16, x + 30}, new int[] {y + 9, y - 2, y + 9}, 3);
        } else if (tile == 'r' && (tileX + tileY) % 2 == 0) {
            g.setColor(new Color(154, 142, 128));
            g.drawRect(x + 5, y + 5, 21, 21);
        } else if (tile == '~') {
            g.setColor(new Color(102, 187, 211, 150));
            g.drawLine(x + 4, y + 11, x + 27, y + 11);
            g.drawLine(x + 10, y + 21, x + 30, y + 21);
        }
    }

    private void drawLandmarkLabels(Graphics g, double cameraX, double cameraY) {
        g.setFont(new Font("Arial", Font.BOLD, 11));
        drawLabel(g, "Vila de Brumafolha", 3 * TILE_SIZE - cameraX, 4 * TILE_SIZE - cameraY);
        drawLabel(g, "Estrada Antiga", 13 * TILE_SIZE - cameraX, 10 * TILE_SIZE - cameraY);
        drawLabel(g, "Bosque dos Sussurros", 25 * TILE_SIZE - cameraX, 12 * TILE_SIZE - cameraY);
        drawLabel(g, "Ruínas do Sino", 27 * TILE_SIZE - cameraX, 14 * TILE_SIZE - cameraY);
    }

    private void drawLabel(Graphics g, String text, double x, double y) {
        int drawX = (int) x;
        int drawY = (int) y;
        if (drawX < -220 || drawX > 390 || drawY < -20 || drawY > 230) return;
        g.setColor(new Color(20, 25, 27, 180));
        g.fillRoundRect(drawX - 4, drawY - 13, g.getFontMetrics().stringWidth(text) + 8, 16, 5, 5);
        g.setColor(new Color(245, 232, 190));
        g.drawString(text, drawX, drawY);
    }
}
