package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.WeaponBuildManager;

/** Painel modal para escolher a especialização da arma equipada. */
public final class WeaponBuildScreen {

    private static boolean open;
    private static int selection;

    private WeaponBuildScreen() {
    }

    public static boolean isOpen() {
        return open;
    }

    public static void open() {
        if (Game.player == null || Game.player.getCurrentWeaponType() == null
                || !"NORMAL".equals(Game.gameState)) {
            return;
        }
        open = true;
        selection = WeaponBuildManager.getPath(Game.player.getCurrentWeaponType()).ordinal();
        Game.gameState = "WEAPON_BUILD";
    }

    public static void close() {
        open = false;
        if ("WEAPON_BUILD".equals(Game.gameState)) {
            Game.gameState = "NORMAL";
        }
    }

    public static void reset() {
        open = false;
        selection = 0;
    }

    public static void navigateUp() {
        if (open) {
            selection = Math.floorMod(selection - 1, WeaponBuildManager.BuildPath.values().length);
        }
    }

    public static void navigateDown() {
        if (open) {
            selection = Math.floorMod(selection + 1, WeaponBuildManager.BuildPath.values().length);
        }
    }

    public static void confirm() {
        if (!open || Game.player == null) {
            return;
        }
        WeaponType weapon = Game.player.getCurrentWeaponType();
        WeaponBuildManager.BuildPath path = WeaponBuildManager.BuildPath.values()[selection];
        WeaponBuildManager.setPath(weapon, path);
        MissionBanner.show("BUILD CONFIGURADA", weapon.getShortName() + " — " + path.getLabel(),
                new Color(255, 193, 7), Color.WHITE, 150);
        close();
    }

    public static void render(Graphics g) {
        if (!open || g == null) {
            return;
        }
        int width = g.getClipBounds() != null ? g.getClipBounds().width : Game.WIDTH * Game.SCALE;
        int height = g.getClipBounds() != null ? g.getClipBounds().height : Game.HEIGHT * Game.SCALE;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(2, 6, 12, 230));
        g2.fillRect(0, 0, width, height);
        int unit = Math.max(1, Game.SCALE / 4);
        int panelWidth = Math.min(width - 24 * unit, 310 * unit);
        int panelHeight = Math.min(height - 20 * unit, 160 * unit);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        g2.setColor(new Color(18, 28, 44, 250));
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 12 * unit, 12 * unit);
        g2.setColor(new Color(255, 193, 7));
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 12 * unit, 12 * unit);

        WeaponType weapon = Game.player != null ? Game.player.getCurrentWeaponType() : WeaponType.BLASTER;
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16 * unit));
        drawCentered(g2, "BUILD DE " + weapon.getShortName(), panelX, panelWidth, panelY + 23 * unit);
        g2.setColor(new Color(255, 214, 10));
        g2.setFont(new Font("Arial", Font.PLAIN, 9 * unit));
        drawCentered(g2, WeaponBuildManager.getSummary(weapon), panelX, panelWidth, panelY + 39 * unit);

        WeaponBuildManager.BuildPath[] paths = WeaponBuildManager.BuildPath.values();
        for (int i = 0; i < paths.length; i++) {
            int rowY = panelY + 61 * unit + i * 25 * unit;
            if (i == selection) {
                g2.setColor(new Color(72, 91, 116));
                g2.fillRoundRect(panelX + 14 * unit, rowY - 13 * unit, panelWidth - 28 * unit, 20 * unit,
                        5 * unit, 5 * unit);
            }
            g2.setColor(i == selection ? Color.WHITE : new Color(206, 216, 225));
            g2.setFont(new Font("Arial", i == selection ? Font.BOLD : Font.PLAIN, 10 * unit));
            g2.drawString((i == selection ? "> " : "  ") + paths[i].getLabel(), panelX + 24 * unit, rowY);
            g2.setColor(new Color(180, 200, 210));
            g2.setFont(new Font("Arial", Font.PLAIN, 8 * unit));
            g2.drawString(paths[i].getDescription(), panelX + 45 * unit, rowY + 10 * unit);
        }
        g2.setColor(new Color(129, 199, 132));
        g2.setFont(new Font("Arial", Font.PLAIN, 8 * unit));
        g2.drawString("Setas/W-S: escolher   ENTER: aplicar   ESC/B: fechar",
                panelX + 20 * unit, panelY + panelHeight - 10 * unit);
        g2.dispose();
    }

    private static void drawCentered(Graphics2D g2, String text, int x, int width, int baseline) {
        g2.drawString(text, x + (width - g2.getFontMetrics().stringWidth(text)) / 2, baseline);
    }
}
