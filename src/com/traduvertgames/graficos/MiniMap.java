package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.IceTile;
import com.traduvertgames.world.MudTile;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.Tile;
import com.traduvertgames.world.WallTile;
import com.traduvertgames.world.World;

/**
 * Minimapa no canto superior direito da HUD, com escala adaptativa para mapas
 * de qualquer dimensão e marcadores das regiões/POIs do mundo RPG.
 */
public final class MiniMap {

    private static final int PANEL_WIDTH = 84;
    private static final int PANEL_HEIGHT = 56;

    private static final Color COLOR_FLOOR = new Color(30, 34, 44, 220);
    private static final Color COLOR_WALL = new Color(120, 130, 150, 230);
    private static final Color COLOR_MUD = new Color(109, 76, 65, 230);
    private static final Color COLOR_ICE = new Color(176, 190, 197, 230);
    private static final Color COLOR_PLAYER = new Color(80, 255, 120);
    private static final Color COLOR_ENEMY = new Color(255, 90, 90);
    private static final Color COLOR_POI = new Color(255, 214, 10);

    private MiniMap() {
    }

    public static void render(Graphics g) {
        if (!"NORMAL".equals(Game.gameState)) {
            return;
        }
        if (Game.world == null || World.tiles == null || World.WIDTH <= 0 || World.HEIGHT <= 0) {
            return;
        }

        int s = Game.SCALE;
        int panelX = (Game.WIDTH - PANEL_WIDTH - 8) * s;
        int panelY = 8 * s;
        int panelW = PANEL_WIDTH * s;
        int panelH = PANEL_HEIGHT * s;
        double cellW = panelW / (double) World.WIDTH;
        double cellH = panelH / (double) World.HEIGHT;

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(8, 12, 20, 205));
        g2.fillRoundRect(panelX - 4 * s, panelY - 4 * s, panelW + 8 * s, panelH + 8 * s, 8 * s, 8 * s);

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

        // Snapshot defensivo: durante a transição a lista de inimigos pode ser recriada.
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

        // Marcador do alvo da missão: o jogador sempre sabe onde ir.
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
