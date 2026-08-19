package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.WorldWeatherManager;

/** Aviso curto e suave ao entrar em um setor do Mundo Aberto. */
public final class SectorEntryOverlay {

    private static final int TOTAL_FRAMES = 130;
    private static final int FADE_FRAMES = 18;
    private static int frames;
    private static String sectorCode = "--";
    private static String regionName = "Mundo Aberto";
    private static String climateLabel = "DIA · CÉU LIMPO";

    private SectorEntryOverlay() {
    }

    public static void show(String code, String region) {
        sectorCode = code == null || code.isEmpty() ? "--" : code;
        regionName = region == null || region.isEmpty() ? "Mundo Aberto" : region;
        climateLabel = WorldWeatherManager.getCurrentClimateLabel();
        frames = TOTAL_FRAMES;
    }

    public static void reset() {
        frames = 0;
        sectorCode = "--";
        regionName = "Mundo Aberto";
        climateLabel = "DIA · CÉU LIMPO";
    }

    public static boolean isShowing() {
        return frames > 0 && Game.isOpenWorldMode();
    }

    public static void update() {
        if (frames > 0) {
            frames--;
        }
    }

    public static void render(Graphics2D g2) {
        if (!isShowing() || g2 == null) {
            return;
        }
        int elapsed = TOTAL_FRAMES - frames;
        float alpha = elapsed < FADE_FRAMES ? elapsed / (float) FADE_FRAMES
                : frames < FADE_FRAMES ? frames / (float) FADE_FRAMES : 1.0f;
        int scale = Game.SCALE;
        int width = 160 * scale;
        int height = 40 * scale;
        int targetY = 12 * scale;
        int y = targetY - (int) ((1.0f - alpha) * 10 * scale);
        int x = (Game.WIDTH * scale - width) / 2;

        g2.setColor(new Color(4, 20, 23, Math.max(0, Math.min(225, (int) (220 * alpha)))));
        g2.fillRoundRect(x, y, width, height, 6 * scale, 6 * scale);
        g2.setColor(new Color(66, 232, 222, Math.max(0, Math.min(255, (int) (235 * alpha)))));
        g2.drawRoundRect(x, y, width - 1, height - 1, 6 * scale, 6 * scale);

        g2.setFont(new Font("SansSerif", Font.BOLD, 16 * scale / 4 + 4));
        g2.drawString(sectorCode, x + 11 * scale, y + 22 * scale);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9 * scale / 4 + 3));
        g2.setColor(new Color(230, 255, 248, Math.max(0, Math.min(255, (int) (240 * alpha)))));
        g2.drawString(regionName.toUpperCase(), x + 48 * scale, y + 16 * scale);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 7 * scale / 4 + 2));
        g2.setColor(new Color(163, 205, 198, Math.max(0, Math.min(255, (int) (220 * alpha)))));
        g2.drawString("SETOR DESCOBERTO", x + 48 * scale, y + 27 * scale);
        g2.setColor(new Color(66, 232, 222, Math.max(0, Math.min(255, (int) (220 * alpha)))));
        g2.drawString(climateLabel, x + 48 * scale, y + 35 * scale);
    }
}
