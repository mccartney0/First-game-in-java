package com.traduvertgames.engine;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.traduvertgames.entities.Enemy;

/**
 * Catálogo extensível de conteúdo do RPG. A engine de mapa consulta este
 * catálogo em vez de espalhar regras de região, bioma e densidade em cada
 * gerador.
 */
public final class RpgExpansionEngine {

    public static final class RegionSpec {
        private final String id;
        private final String displayName;
        private final Color floorColor;
        private final Color accentColor;
        private final int baseEnemyDensity;
        private final Enemy.Variant[] preferredVariants;

        private RegionSpec(String id, String displayName, Color floorColor, Color accentColor,
                int baseEnemyDensity, Enemy.Variant... preferredVariants) {
            this.id = id;
            this.displayName = displayName;
            this.floorColor = floorColor;
            this.accentColor = accentColor;
            this.baseEnemyDensity = baseEnemyDensity;
            this.preferredVariants = preferredVariants;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Color getFloorColor() {
            return floorColor;
        }

        public Color getAccentColor() {
            return accentColor;
        }

        public int getBaseEnemyDensity() {
            return baseEnemyDensity;
        }

        public List<Enemy.Variant> getPreferredVariants() {
            return Collections.unmodifiableList(Arrays.asList(preferredVariants));
        }

        public Enemy.Variant chooseVariant(Random random, int depth) {
            if (preferredVariants.length == 0) {
                return Enemy.Variant.SCOUT;
            }
            int index = Math.floorMod(random.nextInt(preferredVariants.length) + depth, preferredVariants.length);
            return preferredVariants[index];
        }
    }

    public static final class PoiSpec {
        private final String id;
        private final String type;
        private final String regionId;
        private final String title;
        private final int xRatio;
        private final int yRatio;

        private PoiSpec(String id, String type, String regionId, String title, int xRatio, int yRatio) {
            this.id = id;
            this.type = type;
            this.regionId = regionId;
            this.title = title;
            this.xRatio = xRatio;
            this.yRatio = yRatio;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getRegionId() {
            return regionId;
        }

        public String getTitle() {
            return title;
        }

        public int resolveX(int width) {
            return Math.max(3, Math.min(width - 4, width * xRatio / 1000));
        }

        public int resolveY(int height) {
            return Math.max(3, Math.min(height - 4, height * yRatio / 1000));
        }
    }

    private static final RegionSpec REFUGE = new RegionSpec("REFUGE", "Refúgio Aurora",
            new Color(0, 0, 0), new Color(38, 198, 218), 5,
            Enemy.Variant.SCOUT, Enemy.Variant.SHIELDER);
    private static final RegionSpec RUINS = new RegionSpec("RUINS", "Ruínas de Ferro",
            new Color(0, 0, 0), new Color(255, 160, 40), 8,
            Enemy.Variant.BOMBER, Enemy.Variant.RAVAGER, Enemy.Variant.SCOUT);
    private static final RegionSpec SANCTUARY = new RegionSpec("SANCTUARY", "Santuário Violeta",
            new Color(0, 0, 0), new Color(214, 75, 255), 7,
            Enemy.Variant.TELEPORTER, Enemy.Variant.PHANTOM, Enemy.Variant.SNIPER);
    private static final RegionSpec MARSH = new RegionSpec("MARSH", "Pântano Tóxico",
            new Color(109, 76, 65), new Color(146, 255, 55), 10,
            Enemy.Variant.SWARM, Enemy.Variant.BOMBER, Enemy.Variant.SHIELDER);
    private static final RegionSpec TUNDRA = new RegionSpec("TUNDRA", "Tundra Cristalina",
            new Color(176, 190, 197), new Color(120, 220, 255), 6,
            Enemy.Variant.SNIPER, Enemy.Variant.SENTINEL, Enemy.Variant.SCOUT);
    private static final RegionSpec CORE = new RegionSpec("CORE", "Núcleo do Vazio",
            new Color(0, 0, 0), new Color(255, 55, 110), 12,
            Enemy.Variant.PHANTOM, Enemy.Variant.RAVAGER, Enemy.Variant.WARBRINGER);

    private static final List<RegionSpec> REGIONS = Collections.unmodifiableList(Arrays.asList(
            REFUGE, RUINS, SANCTUARY, MARSH, TUNDRA, CORE));

    private static final List<PoiSpec> DEFAULT_POIS = Collections.unmodifiableList(Arrays.asList(
            new PoiSpec("refuge_gate", "REFUGE_GATE", "REFUGE", "Portão do Refúgio", 150, 170),
            new PoiSpec("supply_depot", "SUPPLY_DEPOT", "RUINS", "Depósito de Suprimentos", 500, 170),
            new PoiSpec("data_terminal", "DATA_TERMINAL", "SANCTUARY", "Terminal de Dados", 840, 170),
            new PoiSpec("marsh_cache", "MARSH_CACHE", "MARSH", "Cache do Pântano", 160, 780),
            new PoiSpec("containment_beacon", "CONTAINMENT_BEACON", "TUNDRA", "Farol de Contenção", 500, 780),
            new PoiSpec("supervisor_arena", "SUPERVISOR_ARENA", "CORE", "Arena do Supervisor", 840, 780),
            new PoiSpec("dungeon_ruins", "DUNGEON_ENTRANCE", "RUINS", "Entrada das Ruínas", 570, 330),
            new PoiSpec("dungeon_marsh", "DUNGEON_ENTRANCE", "MARSH", "Entrada do Pântano", 220, 650),
            new PoiSpec("dungeon_tundra", "DUNGEON_ENTRANCE", "TUNDRA", "Entrada da Tundra", 650, 650),
            new PoiSpec("dungeon_core", "DUNGEON_ENTRANCE", "CORE", "Entrada do Núcleo", 900, 650)));

    private RpgExpansionEngine() {
    }

    public static List<RegionSpec> regions() {
        return REGIONS;
    }

    public static List<PoiSpec> defaultPois() {
        return DEFAULT_POIS;
    }

    public static RegionSpec region(String id) {
        if (id != null) {
            for (RegionSpec spec : REGIONS) {
                if (spec.id.equalsIgnoreCase(id)) {
                    return spec;
                }
            }
        }
        return REFUGE;
    }

    /** Retorna a região em uma grade 3x2, permitindo mapas de qualquer dimensão. */
    public static RegionSpec regionForTile(int tileX, int tileY, int width, int height) {
        int column = Math.max(0, Math.min(2, tileX * 3 / Math.max(1, width)));
        int row = Math.max(0, Math.min(1, tileY * 2 / Math.max(1, height)));
        int index = row == 0 ? column : 3 + column;
        return REGIONS.get(index);
    }

    public static Color terrainColorFor(int tileX, int tileY, int width, int height) {
        return regionForTile(tileX, tileY, width, height).getFloorColor();
    }

    public static int enemyBudget(int width, int height, int depth) {
        int base = Math.max(12, width * height / 520);
        return Math.min(96, base + Math.max(0, depth - 1) * 8);
    }
}
