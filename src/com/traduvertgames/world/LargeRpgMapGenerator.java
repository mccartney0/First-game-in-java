package com.traduvertgames.world;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import com.traduvertgames.engine.RpgExpansionEngine;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.engine.RpgExpansionEngine.PoiSpec;
import com.traduvertgames.engine.RpgExpansionEngine.RegionSpec;

/**
 * Gerador de mapas regionais grandes e determinísticos para expansão do RPG.
 * Produz PNG compatível com {@link World} e um manifesto JSON para ferramentas.
 */
public final class LargeRpgMapGenerator {

    private static final Color BLACK = new Color(0, 0, 0, 255);
    private static final Color WALL = new Color(255, 255, 255, 255);
    private static final Color DESTRUCT = new Color(128, 128, 128, 255);
    private static final Color PLAYER = new Color(0, 38, 255, 255);
    private static final Color ENEMY = new Color(255, 0, 0, 255);
    private static final Color TELEPORTER = new Color(156, 39, 176, 255);
    private static final Color ARTILLERY = new Color(0, 188, 212, 255);
    private static final Color WARDEN = new Color(63, 81, 181, 255);
    private static final Color SENTINEL = new Color(0, 150, 136, 255);
    private static final Color RAVAGER = new Color(244, 81, 30, 255);
    private static final Color PHANTOM = new Color(102, 204, 102, 255);
    private static final Color BOSS = new Color(204, 0, 51, 255);
    private static final Color WEAPON = new Color(255, 106, 0, 255);
    private static final Color LIFE_PACK = new Color(76, 255, 0, 255);
    private static final Color NANO = new Color(255, 82, 82, 255);
    private static final Color BEACON = new Color(76, 175, 80, 255);
    private static final Color DATA_CORE = new Color(0, 172, 193, 255);
    private static final Color PORTAL = new Color(170, 0, 255, 255);

    public static final int DEFAULT_WIDTH = 192;
    public static final int DEFAULT_HEIGHT = 128;
    public static final int OPEN_WORLD_WIDTH = 512;
    public static final int OPEN_WORLD_HEIGHT = 320;
    private static final int MIN_WIDTH = 96;
    private static final int MIN_HEIGHT = 64;
    private static final int PLAYER_X = 4;
    private static final int PLAYER_Y = 4;

    private LargeRpgMapGenerator() {
    }

    public static File generateDefault(int depth) throws IOException {
        return generate(DEFAULT_WIDTH, DEFAULT_HEIGHT, depth,
                0x5EEDL + Math.max(1, depth) * 997L, new File("bin/large_rpg_maps"));
    }

    /** Gera o mapa do modo Mundo Aberto, maior que a aventura RPG regional. */
    public static File generateOpenWorldDefault(int depth) throws IOException {
        int safeDepth = Math.max(1, depth);
        long seed = 0x0F3A0B1DL + safeDepth * 4099L;
        File map = generate(OPEN_WORLD_WIDTH, OPEN_WORLD_HEIGHT, safeDepth, seed,
                new File("bin/open_world_maps"));
        OpenWorldManager.configure(OPEN_WORLD_WIDTH, OPEN_WORLD_HEIGHT, seed);
        return map;
    }

    public static File generate(int width, int height, int depth, long seed, File outputDir) throws IOException {
        width = Math.max(MIN_WIDTH, width);
        height = Math.max(MIN_HEIGHT, height);
        depth = Math.max(1, depth);
        Random rng = new Random(seed);
        BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        fill(map, WALL);
        carveRooms(map, width, height, rng);
        carveCorridors(map, width, height, rng);
        paintTerrain(map, width, height);
        configureWorldMetadata(width, height, depth);
        placePoiMarkers(map, width, height);
        placeEntities(map, width, height, depth, rng);
        placeBoss(map, width, height, depth);

        if (!validate(map)) {
            throw new IOException("Mapa grande inválido: spawn, chefe ou área navegável ausente");
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Não foi possível criar " + outputDir.getAbsolutePath());
        }
        String baseName = "large_rpg_" + width + "x" + height + "_d" + depth + "_s" + Long.toUnsignedString(seed);
        File png = new File(outputDir, baseName + ".png");
        ImageIO.write(map, "png", png);
        writeManifest(new File(outputDir, baseName + ".json"), width, height, depth, seed, map);
        return png;
    }

    private static void carveRooms(BufferedImage map, int width, int height, Random rng) {
        carveBox(map, 2, 2, 12, 9);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                int centerX = (column * 2 + 1) * width / 6;
                int centerY = (row * 2 + 1) * height / 4;
                int roomWidth = Math.max(18, width / 10 + rng.nextInt(Math.max(2, width / 20)));
                int roomHeight = Math.max(12, height / 10 + rng.nextInt(Math.max(2, height / 24)));
                carveBox(map, centerX - roomWidth / 2, centerY - roomHeight / 2, roomWidth, roomHeight);
                for (int pillar = 0; pillar < 2 + rng.nextInt(4); pillar++) {
                    int x = centerX - roomWidth / 2 + 2 + rng.nextInt(Math.max(2, roomWidth - 4));
                    int y = centerY - roomHeight / 2 + 2 + rng.nextInt(Math.max(2, roomHeight - 4));
                    if (Math.abs(x - centerX) + Math.abs(y - centerY) > 4) {
                        map.setRGB(x, y, DESTRUCT.getRGB());
                    }
                }
            }
        }
    }

    private static void carveCorridors(BufferedImage map, int width, int height, Random rng) {
        List<int[]> centers = regionCenters(width, height);
        int previousX = PLAYER_X;
        int previousY = PLAYER_Y;
        for (int[] center : centers) {
            if (rng.nextBoolean()) {
                carveSegment(map, previousX, previousY, center[0], previousY);
                carveSegment(map, center[0], previousY, center[0], center[1]);
            } else {
                carveSegment(map, previousX, previousY, previousX, center[1]);
                carveSegment(map, previousX, center[1], center[0], center[1]);
            }
            previousX = center[0];
            previousY = center[1];
        }
        carveSegment(map, PLAYER_X, PLAYER_Y, width - 4, PLAYER_Y);
        carveSegment(map, PLAYER_X, PLAYER_Y, PLAYER_X, height - 4);
    }

    private static List<int[]> regionCenters(int width, int height) {
        List<int[]> centers = new ArrayList<int[]>();
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                centers.add(new int[] { (column * 2 + 1) * width / 6, (row * 2 + 1) * height / 4 });
            }
        }
        return centers;
    }

    private static void carveSegment(BufferedImage map, int x0, int y0, int x1, int y1) {
        int stepX = Integer.compare(x1, x0);
        int stepY = Integer.compare(y1, y0);
        int x = x0;
        int y = y0;
        while (true) {
            carve(map, x, y);
            carve(map, x + 1, y);
            carve(map, x, y + 1);
            if (x == x1 && y == y1) {
                return;
            }
            if (x != x1) {
                x += stepX;
            }
            if (y != y1) {
                y += stepY;
            }
        }
    }

    private static void paintTerrain(BufferedImage map, int width, int height) {
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (isWalkable(map.getRGB(x, y))) {
                    map.setRGB(x, y, RpgExpansionEngine.terrainColorFor(x, y, width, height).getRGB());
                }
            }
        }
    }

    private static void configureWorldMetadata(int width, int height, int depth) {
        RpgWorldManager.configure(depth, width, height);
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            RpgWorldManager.RegionBounds bounds = RpgWorldManager.getBounds(region);
            RpgWorldManager.registerMobArea(region, bounds.centerX(), bounds.centerY(),
                    Math.max(6, Math.min(14, width / 18)), 6 + depth);
        }
        for (PoiSpec poi : RpgExpansionEngine.defaultPois()) {
            try {
                RpgWorldManager.PoiType type = RpgWorldManager.PoiType.valueOf(poi.getType());
                RpgWorldManager.RegionType region = RpgWorldManager.RegionType.valueOf(poi.getRegionId());
                RpgWorldManager.registerPoi(type, region, poi.resolveX(width), poi.resolveY(height));
            } catch (IllegalArgumentException ignored) {
                // Um conteúdo experimental pode existir no catálogo sem bloquear o mapa.
            }
        }
    }

    private static void placePoiMarkers(BufferedImage map, int width, int height) {
        for (PoiSpec poi : RpgExpansionEngine.defaultPois()) {
            int x = poi.resolveX(width);
            int y = poi.resolveY(height);
            if (!isWalkable(map.getRGB(x, y))) {
                carve(map, x, y);
            }
            Color marker = poi.getType().contains("DUNGEON") ? PORTAL
                    : poi.getType().contains("DATA") ? DATA_CORE
                    : poi.getType().contains("SUPPLY") ? WEAPON : BEACON;
            map.setRGB(x, y, marker.getRGB());
        }
    }

    private static void placeEntities(BufferedImage map, int width, int height, int depth, Random rng) {
        map.setRGB(PLAYER_X, PLAYER_Y, PLAYER.getRGB());
        int budget = RpgExpansionEngine.enemyBudget(width, height, depth);
        int placed = 0;
        int attempts = 0;
        while (placed < budget && attempts++ < budget * 200) {
            int x = 3 + rng.nextInt(width - 6);
            int y = 3 + rng.nextInt(height - 6);
            if (!isWalkable(map.getRGB(x, y)) || isNearReservedPoint(x, y, width, height, 5)
                    || Math.hypot(x - PLAYER_X, y - PLAYER_Y) < 10) {
                continue;
            }
            RegionSpec region = RpgExpansionEngine.regionForTile(x, y, width, height);
            Enemy.Variant variant = region.chooseVariant(rng, depth);
            map.setRGB(x, y, colorForVariant(variant).getRGB());
            placed++;
            if (rng.nextInt(100) < 12) {
                int itemX = Math.min(width - 3, x + 1);
                int itemY = Math.min(height - 3, y + 1);
                if (isWalkable(map.getRGB(itemX, itemY))) {
                    map.setRGB(itemX, itemY, rng.nextBoolean() ? LIFE_PACK.getRGB() : NANO.getRGB());
                }
            }
        }
        int weaponX = Math.min(width - 5, width / 2);
        int weaponY = Math.min(height - 5, height / 2);
        if (isWalkable(map.getRGB(weaponX, weaponY))) {
            map.setRGB(weaponX, weaponY, WEAPON.getRGB());
        }
    }

    private static void placeBoss(BufferedImage map, int width, int height, int depth) {
        int x = width - 5;
        int y = height - 5;
        while (x > width / 2 && !isWalkable(map.getRGB(x, y))) {
            x--;
            y--;
        }
        if (isWalkable(map.getRGB(x, y))) {
            map.setRGB(x, y, BOSS.getRGB());
        }
    }

    private static Color colorForVariant(Enemy.Variant variant) {
        switch (variant) {
        case TELEPORTER:
            return TELEPORTER;
        case ARTILLERY:
            return ARTILLERY;
        case WARDEN:
            return WARDEN;
        case SENTINEL:
            return SENTINEL;
        case RAVAGER:
            return RAVAGER;
        case PHANTOM:
            return PHANTOM;
        default:
            return ENEMY;
        }
    }

    private static boolean isNearReservedPoint(int x, int y, int width, int height, int radius) {
        for (PoiSpec poi : RpgExpansionEngine.defaultPois()) {
            int dx = x - poi.resolveX(width);
            int dy = y - poi.resolveY(height);
            if (dx * dx + dy * dy <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private static void carveBox(BufferedImage map, int x, int y, int width, int height) {
        for (int yy = Math.max(1, y); yy < Math.min(map.getHeight() - 1, y + height); yy++) {
            for (int xx = Math.max(1, x); xx < Math.min(map.getWidth() - 1, x + width); xx++) {
                map.setRGB(xx, yy, BLACK.getRGB());
            }
        }
    }

    private static void carve(BufferedImage map, int x, int y) {
        if (x > 0 && y > 0 && x < map.getWidth() - 1 && y < map.getHeight() - 1) {
            map.setRGB(x, y, BLACK.getRGB());
        }
    }

    private static void fill(BufferedImage map, Color color) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.setRGB(x, y, color.getRGB());
            }
        }
    }

    private static boolean isWalkable(int rgb) {
        return rgb == BLACK.getRGB() || rgb == new Color(109, 76, 65).getRGB()
                || rgb == new Color(176, 190, 197).getRGB();
    }

    public static boolean validate(BufferedImage map) {
        boolean spawn = false;
        boolean boss = false;
        int floor = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int rgb = map.getRGB(x, y);
                spawn |= rgb == PLAYER.getRGB();
                boss |= rgb == BOSS.getRGB();
                if (isWalkable(rgb)) {
                    floor++;
                }
            }
        }
        return spawn && boss && floor > map.getWidth() * map.getHeight() / 24;
    }

    private static void writeManifest(File file, int width, int height, int depth, long seed,
            BufferedImage map) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("{");
            out.println("  \"format\": \"first-game-large-rpg-v1\",");
            out.println("  \"width\": " + width + ",");
            out.println("  \"height\": " + height + ",");
            out.println("  \"depth\": " + depth + ",");
            out.println("  \"seed\": " + seed + ",");
            out.println("  \"floorPixels\": " + countFloor(map) + ",");
            out.println("  \"regions\": [");
            for (int i = 0; i < RpgExpansionEngine.regions().size(); i++) {
                RegionSpec region = RpgExpansionEngine.regions().get(i);
                out.print("    {\"id\": \"" + region.getId() + "\", \"name\": \""
                        + region.getDisplayName() + "\", \"density\": " + region.getBaseEnemyDensity() + "}");
                out.println(i + 1 == RpgExpansionEngine.regions().size() ? "" : ",");
            }
            out.println("  ],");
            out.println("  \"pois\": [");
            for (int i = 0; i < RpgExpansionEngine.defaultPois().size(); i++) {
                PoiSpec poi = RpgExpansionEngine.defaultPois().get(i);
                out.print("    {\"id\": \"" + poi.getId() + "\", \"type\": \"" + poi.getType()
                        + "\", \"region\": \"" + poi.getRegionId() + "\", \"x\": "
                        + poi.resolveX(width) + ", \"y\": " + poi.resolveY(height) + "}");
                out.println(i + 1 == RpgExpansionEngine.defaultPois().size() ? "" : ",");
            }
            out.println("  ]");
            out.println("}");
        }
    }

    private static int countFloor(BufferedImage map) {
        int count = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (isWalkable(map.getRGB(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }
}
