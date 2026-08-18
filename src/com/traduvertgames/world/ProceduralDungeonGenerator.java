package com.traduvertgames.world;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import com.traduvertgames.entities.Enemy;

/** Gera instâncias de masmorra determinísticas para cada região do mundo. */
public final class ProceduralDungeonGenerator {

    public static final int MAP_WIDTH = 64;
    public static final int MAP_HEIGHT = 40;

    private static final Color FLOOR = new Color(0, 0, 0, 255);
    private static final Color WALL = new Color(255, 255, 255, 255);
    private static final Color PLAYER = new Color(0, 38, 255, 255);
    private static final Color EXIT = new Color(255, 0, 255, 255);
    private static final Color ENEMY = new Color(255, 0, 0, 255);
    private static final Color SAPPER = new Color(0, 128, 128, 255);
    private static final Color ARTILLERY = new Color(0, 188, 212, 255);
    private static final Color PHANTOM = new Color(129, 199, 132, 255);
    private static final Color GUARDIAN = new Color(255, 87, 34, 255);
    private static final Color OVERSEER = new Color(121, 134, 203, 255);
    private static final Color OVERSEER_PRIME = new Color(208, 25, 55, 255);
    private static final Color SAPPER_BOSS = new Color(170, 0, 170, 255);
    private static final Color ARTILLERY_BOSS = new Color(0, 166, 166, 255);
    private static final Color PHANTOM_BOSS = new Color(102, 204, 102, 255);
    private static final Color GUARDIAN_BOSS = new Color(255, 102, 0, 255);
    private static final Color OVERSEER_BOSS = new Color(102, 102, 204, 255);
    private static final Color OVERSEER_PRIME_BOSS = new Color(204, 0, 51, 255);

    private ProceduralDungeonGenerator() {
    }

    public static File generate(RpgWorldManager.RegionType region, int depth) throws IOException {
        if (region == null) {
            region = RpgWorldManager.RegionType.CORE;
        }
        BufferedImage map = new BufferedImage(MAP_WIDTH, MAP_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        fill(map, WALL);
        Random random = new Random(0xD00D0000L + region.ordinal() * 7919L + depth * 104729L);

        int[][] centers = {
                { 8, 7 }, { 22, 7 }, { 36, 7 }, { 50, 7 }, { 22, 29 }, { 50, 29 }
        };
        for (int[] center : centers) {
            int roomWidth = 9 + random.nextInt(4);
            int roomHeight = 6 + random.nextInt(3);
            carveRoom(map, center[0] - roomWidth / 2, center[1] - roomHeight / 2, roomWidth, roomHeight);
        }
        for (int i = 1; i < centers.length; i++) {
            connect(map, centers[i - 1][0], centers[i - 1][1], centers[i][0], centers[i][1], random.nextBoolean());
        }
        connect(map, 4, 4, centers[0][0], centers[0][1], true);
        connect(map, centers[centers.length - 1][0], centers[centers.length - 1][1], MAP_WIDTH - 5,
                MAP_HEIGHT - 5, false);
        border(map);

        map.setRGB(4, 4, PLAYER.getRGB());
        map.setRGB(MAP_WIDTH - 5, MAP_HEIGHT - 5, EXIT.getRGB());
        placeEnemies(map, centers, region, depth, random);
        int[] boss = centers[centers.length - 1];
        map.setRGB(boss[0], boss[1], bossColor(region).getRGB());

        if (!validate(map)) {
            throw new IOException("Masmorra procedural inválida para a região " + region);
        }
        File outputDir = new File("bin");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Não foi possível criar o diretório das masmorras: " + outputDir.getAbsolutePath());
        }
        File output = new File(outputDir, "dungeon_" + region.name().toLowerCase() + "_" + Math.max(1, depth) + ".png");
        ImageIO.write(map, "png", output);
        return output;
    }

    public static Enemy.Variant bossVariant(RpgWorldManager.RegionType region) {
        switch (region) {
        case REFUGE:
            return Enemy.Variant.SAPPER;
        case RUINS:
            return Enemy.Variant.ARTILLERY;
        case MARSH:
            return Enemy.Variant.PHANTOM;
        case TUNDRA:
            return Enemy.Variant.GUARDIAN;
        case SANCTUARY:
            return Enemy.Variant.OVERSEER;
        case CORE:
        default:
            return Enemy.Variant.OVERSEER_PRIME;
        }
    }

    private static Color mobColor(RpgWorldManager.RegionType region) {
        switch (bossVariant(region)) {
        case SAPPER:
            return SAPPER;
        case ARTILLERY:
            return ARTILLERY;
        case PHANTOM:
            return PHANTOM;
        case GUARDIAN:
            return GUARDIAN;
        case OVERSEER:
            return OVERSEER;
        case OVERSEER_PRIME:
        default:
            return OVERSEER_PRIME;
        }
    }

    private static Color bossColor(RpgWorldManager.RegionType region) {
        switch (bossVariant(region)) {
        case SAPPER:
            return SAPPER_BOSS;
        case ARTILLERY:
            return ARTILLERY_BOSS;
        case PHANTOM:
            return PHANTOM_BOSS;
        case GUARDIAN:
            return GUARDIAN_BOSS;
        case OVERSEER:
            return OVERSEER_BOSS;
        case OVERSEER_PRIME:
        default:
            return OVERSEER_PRIME_BOSS;
        }
    }

    private static void placeEnemies(BufferedImage map, int[][] centers,
            RpgWorldManager.RegionType region, int depth, Random random) {
        Color variantColor = mobColor(region);
        for (int i = 1; i < centers.length; i++) {
            int count = 2 + Math.min(2, depth / 3);
            for (int j = 0; j < count; j++) {
                int x = centers[i][0] - 3 + random.nextInt(7);
                int y = centers[i][1] - 2 + random.nextInt(5);
                if (isFloor(map, x, y)) {
                    map.setRGB(x, y, j == 0 ? variantColor.getRGB() : ENEMY.getRGB());
                }
            }
        }
    }

    private static void carveRoom(BufferedImage map, int x, int y, int width, int height) {
        for (int yy = y; yy < y + height; yy++) {
            for (int xx = x; xx < x + width; xx++) {
                carve(map, xx, yy);
            }
        }
    }

    private static void connect(BufferedImage map, int x0, int y0, int x1, int y1, boolean horizontalFirst) {
        int x = x0;
        int y = y0;
        while (horizontalFirst && x != x1) {
            carve(map, x, y);
            carve(map, x, y + 1);
            x += Integer.compare(x1, x);
        }
        while (y != y1) {
            carve(map, x, y);
            carve(map, x + 1, y);
            y += Integer.compare(y1, y);
        }
        while (!horizontalFirst && x != x1) {
            carve(map, x, y);
            carve(map, x, y + 1);
            x += Integer.compare(x1, x);
        }
        carve(map, x, y);
    }

    private static void carve(BufferedImage map, int x, int y) {
        if (x > 0 && y > 0 && x < MAP_WIDTH - 1 && y < MAP_HEIGHT - 1) {
            map.setRGB(x, y, FLOOR.getRGB());
        }
    }

    private static boolean isFloor(BufferedImage map, int x, int y) {
        return x > 0 && y > 0 && x < MAP_WIDTH - 1 && y < MAP_HEIGHT - 1
                && map.getRGB(x, y) == FLOOR.getRGB();
    }

    private static void border(BufferedImage map) {
        for (int x = 0; x < MAP_WIDTH; x++) {
            map.setRGB(x, 0, WALL.getRGB());
            map.setRGB(x, MAP_HEIGHT - 1, WALL.getRGB());
        }
        for (int y = 0; y < MAP_HEIGHT; y++) {
            map.setRGB(0, y, WALL.getRGB());
            map.setRGB(MAP_WIDTH - 1, y, WALL.getRGB());
        }
    }

    private static void fill(BufferedImage map, Color color) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.setRGB(x, y, color.getRGB());
            }
        }
    }

    public static boolean validate(BufferedImage map) {
        if (map == null || map.getWidth() != MAP_WIDTH || map.getHeight() != MAP_HEIGHT) {
            return false;
        }
        boolean player = false;
        boolean exit = false;
        boolean boss = false;
        int floorCount = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int rgb = map.getRGB(x, y);
                player |= rgb == PLAYER.getRGB();
                exit |= rgb == EXIT.getRGB();
                boss |= rgb == SAPPER_BOSS.getRGB() || rgb == ARTILLERY_BOSS.getRGB()
                        || rgb == PHANTOM_BOSS.getRGB() || rgb == GUARDIAN_BOSS.getRGB()
                        || rgb == OVERSEER_BOSS.getRGB() || rgb == OVERSEER_PRIME_BOSS.getRGB();
                if (rgb == FLOOR.getRGB() || rgb == PLAYER.getRGB() || rgb == EXIT.getRGB()
                        || rgb == ENEMY.getRGB() || boss) {
                    floorCount++;
                }
            }
        }
        return player && exit && boss && floorCount > 500;
    }
}
