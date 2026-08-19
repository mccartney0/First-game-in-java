package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.IceTile;
import com.traduvertgames.world.MudTile;
import com.traduvertgames.world.OpenWorldManager;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.Tile;
import com.traduvertgames.world.WallTile;
import com.traduvertgames.world.World;

/**
 * Minimapa no canto superior direito. Em Mundo Aberto, usa uma grade 8×5 de
 * setores em vez de iterar os 163.840 tiles do mapa inteiro a cada frame.
 */
public final class MiniMap {

    private static final int PANEL_WIDTH = 84;
    private static final int PANEL_HEIGHT = 56;
    private static final int OPEN_WORLD_PANEL_WIDTH = 112;
    private static final int OPEN_WORLD_PANEL_HEIGHT = 70;

    private static final Color COLOR_FLOOR = new Color(30, 34, 44, 220);
    private static final Color COLOR_WALL = new Color(120, 130, 150, 230);
    private static final Color COLOR_MUD = new Color(109, 76, 65, 230);
    private static final Color COLOR_ICE = new Color(176, 190, 197, 230);
    private static final Color COLOR_PLAYER = new Color(80, 255, 120);
    private static final Color COLOR_ENEMY = new Color(255, 90, 90);
    private static final Color COLOR_POI = new Color(255, 214, 10);
    private static final Color COLOR_DISCOVERED = new Color(66, 232, 222, 105);
    private static final Color COLOR_UNDISCOVERED = new Color(3, 10, 12, 150);
    private static final Color COLOR_EVENT = new Color(255, 82, 104, 240);

    private MiniMap() {
    }

    public static void render(Graphics g) {
        if (!"NORMAL".equals(Game.gameState)) {
            return;
        }
        if (Game.world == null || World.tiles == null || World.WIDTH <= 0 || World.HEIGHT <= 0) {
            return;
        }

        boolean openWorld = Game.isOpenWorldMode() && OpenWorldManager.isActive();
        int s = Game.SCALE;
        int panelWidth = openWorld ? OPEN_WORLD_PANEL_WIDTH : PANEL_WIDTH;
        int panelHeight = openWorld ? OPEN_WORLD_PANEL_HEIGHT : PANEL_HEIGHT;
        int panelX = (Game.WIDTH - panelWidth - 8) * s;
        int panelY = 8 * s;
        int panelW = panelWidth * s;
        int panelH = panelHeight * s;
        double cellW = panelW / (double) World.WIDTH;
        double cellH = panelH / (double) World.HEIGHT;

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(8, 12, 20, 205));
        g2.fillRoundRect(panelX - 4 * s, panelY - 4 * s, panelW + 8 * s, panelH + 8 * s, 8 * s, 8 * s);

        if (openWorld) {
            renderOpenWorld(g2, panelX, panelY, panelW, panelH, cellW, cellH, s);
            return;
        }

        for (int yy = 0; yy < World.HEIGHT; yy++) {
            for (int xx = 0; xx < World.WIDTH; xx++) {
                Tile tile = World.tiles[xx + yy * World.WIDTH];
                g2.setColor(colorForTile(tile));
                int x0 = panelX + (int) Math.floor(xx * cellW);
                int y0 = panelY + (int) Math.floor(yy * cellH);
                int x1 = panelX + (int) Math.ceil((xx + 1) * cellW);
                int y1 = panelY + (int) Math.ceil((yy + 1) * cellH);
                g2.fillRect(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
            }
        }

        if (RpgWorldManager.isActive()) {
            for (RpgWorldManager.PointOfInterest poi : RpgWorldManager.getPointsOfInterest()) {
                drawMapMarker(g2, panelX, panelY, cellW, cellH,
                        poi.getTileX(), poi.getTileY(), COLOR_POI, 2 * s / 4 + 2);
            }
        }

        Object[] enemySnapshot = Game.enemies.toArray();
        for (Object obj : enemySnapshot) {
            if (!(obj instanceof Enemy)) {
                continue;
            }
            Enemy enemy = (Enemy) obj;
            drawMapMarker(g2, panelX, panelY, cellW, cellH,
                    enemy.getX() / World.TILE_SIZE, enemy.getY() / World.TILE_SIZE,
                    COLOR_ENEMY, 2 * s / 4 + 1);
        }

        if (Player.life > 0 && Game.player != null) {
            drawMapMarker(g2, panelX, panelY, cellW, cellH,
                    Game.player.getX() / World.TILE_SIZE, Game.player.getY() / World.TILE_SIZE,
                    COLOR_PLAYER, 3 * s / 4 + 2);
        }

        com.traduvertgames.quest.RPGObjective active = MissionHud.findActiveObjective();
        String targetName = active != null ? active.getTargetHint() : null;
        if (targetName != null && !targetName.isEmpty() && !com.traduvertgames.quest.QuestManager.isSurvivalMode()) {
            Entity target = MissionHud.findTargetEntity(targetName);
            if (target != null) {
                double pulse = 0.5 + 0.5 * Math.sin(2.0 * Math.PI
                        * (System.currentTimeMillis() % 960) / 960.0);
                int alpha = 140 + (int) (80 * pulse);
                drawMapMarker(g2, panelX, panelY, cellW, cellH,
                        target.getX() / World.TILE_SIZE, target.getY() / World.TILE_SIZE,
                        new Color(255, 235, 59, alpha), 3 * s / 4 + 2);
            }
        }
    }

    private static void renderOpenWorld(Graphics2D g2, int panelX, int panelY,
            int panelW, int panelH, double cellW, double cellH, int scale) {
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            RpgWorldManager.RegionBounds bounds = RpgWorldManager.getBounds(region);
            int x0 = panelX + (int) Math.floor(bounds.minX * cellW);
            int y0 = panelY + (int) Math.floor(bounds.minY * cellH);
            int x1 = panelX + (int) Math.ceil((bounds.maxX + 1) * cellW);
            int y1 = panelY + (int) Math.ceil((bounds.maxY + 1) * cellH);
            g2.setColor(colorForRegion(region));
            g2.fillRect(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
        }

        int columns = OpenWorldManager.getChunkColumns();
        int rows = OpenWorldManager.getChunkRows();
        for (int chunkY = 0; chunkY < rows; chunkY++) {
            for (int chunkX = 0; chunkX < columns; chunkX++) {
                int minTileX = chunkX * OpenWorldManager.CHUNK_SIZE_TILES;
                int minTileY = chunkY * OpenWorldManager.CHUNK_SIZE_TILES;
                int maxTileX = Math.min(World.WIDTH, minTileX + OpenWorldManager.CHUNK_SIZE_TILES);
                int maxTileY = Math.min(World.HEIGHT, minTileY + OpenWorldManager.CHUNK_SIZE_TILES);
                int x0 = panelX + (int) Math.floor(minTileX * cellW);
                int y0 = panelY + (int) Math.floor(minTileY * cellH);
                int x1 = panelX + (int) Math.ceil(maxTileX * cellW);
                int y1 = panelY + (int) Math.ceil(maxTileY * cellH);
                boolean discovered = OpenWorldManager.isChunkDiscovered(chunkX, chunkY);
                g2.setColor(discovered ? COLOR_DISCOVERED : COLOR_UNDISCOVERED);
                g2.fillRect(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
                g2.setColor(discovered ? new Color(66, 232, 222, 170) : new Color(180, 210, 204, 55));
                g2.drawRect(x0, y0, Math.max(1, x1 - x0 - 1), Math.max(1, y1 - y0 - 1));
            }
        }

        if (OpenWorldManager.getActiveChunkX() >= 0 && OpenWorldManager.getActiveChunkY() >= 0) {
            int x0 = panelX + (int) Math.floor(OpenWorldManager.getActiveChunkX()
                    * OpenWorldManager.CHUNK_SIZE_TILES * cellW);
            int y0 = panelY + (int) Math.floor(OpenWorldManager.getActiveChunkY()
                    * OpenWorldManager.CHUNK_SIZE_TILES * cellH);
            int width = (int) Math.ceil(OpenWorldManager.CHUNK_SIZE_TILES * cellW);
            int height = (int) Math.ceil(OpenWorldManager.CHUNK_SIZE_TILES * cellH);
            g2.setColor(new Color(66, 232, 222, 235));
            g2.drawRect(x0, y0, Math.max(1, width - 1), Math.max(1, height - 1));
        }

        for (RpgWorldManager.PointOfInterest poi : RpgWorldManager.getPointsOfInterest()) {
            drawMapMarker(g2, panelX, panelY, cellW, cellH,
                    poi.getTileX(), poi.getTileY(), colorForPoi(poi.getType()), Math.max(3, scale));
        }

        if (DynamicEventManager.isActive() && DynamicEventManager.getActiveRegion() != null) {
            RpgWorldManager.RegionBounds eventBounds = RpgWorldManager.getBounds(DynamicEventManager.getActiveRegion());
            drawMapMarker(g2, panelX, panelY, cellW, cellH, eventBounds.centerX(), eventBounds.centerY(),
                    COLOR_EVENT, Math.max(4, scale + 1));
        }

        if (Player.life > 0 && Game.player != null) {
            drawMapMarker(g2, panelX, panelY, cellW, cellH,
                    Game.player.getX() / World.TILE_SIZE, Game.player.getY() / World.TILE_SIZE,
                    COLOR_PLAYER, Math.max(4, scale + 1));
        }

        g2.setColor(new Color(226, 255, 248, 210));
        g2.setFont(g2.getFont().deriveFont(Math.max(9f, 2.5f * scale)));
        g2.drawString(OpenWorldManager.getExplorationLabel(), panelX + 4 * scale, panelY + panelH - 3 * scale);
    }

    private static Color colorForTile(Tile tile) {
        if (tile instanceof WallTile) {
            return COLOR_WALL;
        }
        if (tile instanceof MudTile) {
            return COLOR_MUD;
        }
        if (tile instanceof IceTile) {
            return COLOR_ICE;
        }
        return COLOR_FLOOR;
    }

    private static Color colorForRegion(RpgWorldManager.RegionType region) {
        if (region == RpgWorldManager.RegionType.REFUGE) {
            return new Color(39, 74, 68, 230);
        }
        if (region == RpgWorldManager.RegionType.RUINS) {
            return new Color(110, 72, 43, 230);
        }
        if (region == RpgWorldManager.RegionType.SANCTUARY) {
            return new Color(77, 40, 94, 230);
        }
        if (region == RpgWorldManager.RegionType.MARSH) {
            return new Color(70, 83, 49, 230);
        }
        if (region == RpgWorldManager.RegionType.TUNDRA) {
            return new Color(61, 83, 96, 230);
        }
        return new Color(87, 39, 53, 230);
    }

    private static Color colorForPoi(RpgWorldManager.PoiType type) {
        if (type == RpgWorldManager.PoiType.DUNGEON_ENTRANCE) {
            return new Color(200, 183, 255);
        }
        if (type == RpgWorldManager.PoiType.SUPERVISOR_ARENA) {
            return COLOR_EVENT;
        }
        if (type == RpgWorldManager.PoiType.CONTAINMENT_BEACON) {
            return new Color(120, 220, 255);
        }
        return COLOR_POI;
    }

    private static void drawMapMarker(Graphics2D g2, int panelX, int panelY,
            double cellW, double cellH, int tileX, int tileY, Color color, int size) {
        if (tileX < 0 || tileY < 0 || tileX >= World.WIDTH || tileY >= World.HEIGHT) {
            return;
        }
        int x = panelX + (int) ((tileX + 0.5) * cellW) - size / 2;
        int y = panelY + (int) ((tileY + 0.5) * cellH) - size / 2;
        g2.setColor(color);
        g2.fillRect(x, y, Math.max(1, size), Math.max(1, size));
    }
}
