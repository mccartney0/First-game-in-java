package com.traduvertgames.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

/**
 * Estado espacial do mundo RPG procedural pós-campanha.
 *
 * <p>O mapa continua sendo carregado pela {@link World} como uma grade de
 * tiles, mas esta classe fornece semântica de regiões e pontos de interesse
 * sem acoplar quests às coordenadas frágeis do PNG.</p>
 */
public final class RpgWorldManager {

    public enum RegionType {
        REFUGE("Refúgio da Colônia", "área segura e ponto de partida"),
        RUINS("Ruínas Industriais", "corredores de sucata e patrulhas"),
        MARSH("Pântano de Lodo", "terreno lento e emboscadas"),
        TUNDRA("Tundra de Contenção", "câmaras geladas e elites"),
        SANCTUARY("Santuário da IA", "arquitetura hostil e terminais"),
        CORE("Núcleo do Supervisor", "arena de risco máximo");

        private final String displayName;
        private final String subtitle;

        RegionType(String displayName, String subtitle) {
            this.displayName = displayName;
            this.subtitle = subtitle;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSubtitle() {
            return subtitle;
        }
    }

    public enum PoiType {
        REFUGE_GATE("Portão do Refúgio"),
        MEDICAL_SHELTER("Abrigo médico"),
        DATA_TERMINAL("Terminal de dados"),
        SUPPLY_DEPOT("Depósito de suprimentos"),
        MARSH_CACHE("Cache de sobrevivência"),
        CONTAINMENT_BEACON("Beacon de contenção"),
        SUPERVISOR_ARENA("Arena do Supervisor"),
        DUNGEON_ENTRANCE("Entrada da masmorra");

        private final String displayName;

        PoiType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final class RegionBounds {
        public final int minX;
        public final int minY;
        public final int maxX;
        public final int maxY;

        private RegionBounds(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        public int centerX() {
            return (minX + maxX) / 2;
        }

        public int centerY() {
            return (minY + maxY) / 2;
        }

        public boolean contains(int tileX, int tileY) {
            return tileX >= minX && tileX <= maxX && tileY >= minY && tileY <= maxY;
        }
    }

    public static final class MobArea {
        private final RegionType region;
        private final int centerX;
        private final int centerY;
        private final int radius;
        private final int targetCount;

        private MobArea(RegionType region, int centerX, int centerY, int radius, int targetCount) {
            this.region = region;
            this.centerX = centerX;
            this.centerY = centerY;
            this.radius = radius;
            this.targetCount = targetCount;
        }

        public RegionType getRegion() {
            return region;
        }

        public int getCenterX() {
            return centerX;
        }

        public int getCenterY() {
            return centerY;
        }

        public int getRadius() {
            return radius;
        }

        public int getTargetCount() {
            return targetCount;
        }
    }

    public static final class PointOfInterest {
        private final PoiType type;
        private final RegionType region;
        private final int tileX;
        private final int tileY;

        private PointOfInterest(PoiType type, RegionType region, int tileX, int tileY) {
            this.type = type;
            this.region = region;
            this.tileX = tileX;
            this.tileY = tileY;
        }

        public PoiType getType() {
            return type;
        }

        public RegionType getRegion() {
            return region;
        }

        public int getTileX() {
            return tileX;
        }

        public int getTileY() {
            return tileY;
        }

        public String getDisplayName() {
            return type.getDisplayName();
        }
    }

    private static final EnumMap<RegionType, RegionBounds> BOUNDS =
            new EnumMap<RegionType, RegionBounds>(RegionType.class);
    private static final List<PointOfInterest> pointsOfInterest = new ArrayList<PointOfInterest>();
    private static final List<MobArea> mobAreas = new ArrayList<MobArea>();

    private static boolean active;
    private static int depth;
    private static int mapWidth;
    private static int mapHeight;
    private static RegionType currentRegion;
    private static RegionType dungeonRegion;
    private static boolean dungeonMode;
    private static int regionBannerFrames;

    private RpgWorldManager() {
    }

    /** Ativa o contrato de regiões para um mapa procedural gerado. */
    public static void configure(int worldDepth, int width, int height) {
        active = true;
        depth = Math.max(1, worldDepth);
        mapWidth = Math.max(1, width);
        mapHeight = Math.max(1, height);
        currentRegion = null;
        dungeonRegion = null;
        dungeonMode = false;
        regionBannerFrames = 0;
        pointsOfInterest.clear();
        mobAreas.clear();
        BOUNDS.clear();
        for (RegionType region : RegionType.values()) {
            BOUNDS.put(region, boundsFor(region, mapWidth, mapHeight));
        }
    }

    /** Desativa as informações do mundo RPG ao voltar para uma fase fixa. */
    public static void disable() {
        active = false;
        currentRegion = null;
        dungeonRegion = null;
        dungeonMode = false;
        regionBannerFrames = 0;
        pointsOfInterest.clear();
        mobAreas.clear();
        BOUNDS.clear();
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isDungeonMode() {
        return dungeonMode;
    }

    /** Ativa a semântica espacial da instância de dungeon carregada. */
    public static void configureDungeon(RpgWorldManager.RegionType region, int depth) {
        active = true;
        dungeonMode = true;
        dungeonRegion = region == null ? RegionType.CORE : region;
        currentRegion = dungeonRegion;
        RpgWorldManager.depth = Math.max(1, depth);
        regionBannerFrames = 150;
        pointsOfInterest.clear();
        mobAreas.clear();
        BOUNDS.clear();
        BOUNDS.put(dungeonRegion, boundsFor(dungeonRegion, ProceduralDungeonGenerator.MAP_WIDTH,
                ProceduralDungeonGenerator.MAP_HEIGHT));
        mapWidth = ProceduralDungeonGenerator.MAP_WIDTH;
        mapHeight = ProceduralDungeonGenerator.MAP_HEIGHT;
    }

    public static int getDepth() {
        return depth;
    }

    public static int getMapWidth() {
        return mapWidth;
    }

    public static int getMapHeight() {
        return mapHeight;
    }

    /** Calcula a região de uma célula pela divisão 3×2 do mundo. */
    public static RegionType regionForTile(int tileX, int tileY) {
        if (dungeonMode && dungeonRegion != null) {
            return dungeonRegion;
        }
        if (mapWidth <= 0 || mapHeight <= 0) {
            return RegionType.REFUGE;
        }
        int col = Math.max(0, Math.min(2, tileX * 3 / mapWidth));
        int row = Math.max(0, Math.min(1, tileY * 2 / mapHeight));
        if (row == 0) {
            return col == 0 ? RegionType.REFUGE : col == 1 ? RegionType.RUINS : RegionType.SANCTUARY;
        }
        return col == 0 ? RegionType.MARSH : col == 1 ? RegionType.TUNDRA : RegionType.CORE;
    }

    /** Retorna limites seguros da região para um mapa de dimensões arbitrárias. */
    public static RegionBounds boundsFor(RegionType region, int width, int height) {
        int column = region == RegionType.REFUGE || region == RegionType.MARSH ? 0
                : region == RegionType.RUINS || region == RegionType.TUNDRA ? 1 : 2;
        int row = region == RegionType.REFUGE || region == RegionType.RUINS || region == RegionType.SANCTUARY ? 0 : 1;
        int columnStart = column * width / 3;
        int columnEnd = ((column + 1) * width / 3) - 1;
        int rowStart = row * height / 2;
        int rowEnd = ((row + 1) * height / 2) - 1;
        return new RegionBounds(Math.max(2, columnStart + 2), Math.max(2, rowStart + 2),
                Math.max(2, columnEnd - 2), Math.max(2, rowEnd - 2));
    }

    public static RegionBounds getBounds(RegionType region) {
        RegionBounds bounds = BOUNDS.get(region);
        return bounds != null ? bounds : boundsFor(region, mapWidth, mapHeight);
    }

    /** Registra um bolsão de combate após o gerador reservar sua região. */
    public static void registerMobArea(RegionType region, int centerX, int centerY, int radius, int targetCount) {
        if (!active || region == null || radius <= 0 || targetCount <= 0) {
            return;
        }
        mobAreas.add(new MobArea(region, centerX, centerY, radius, targetCount));
    }

    public static List<MobArea> getMobAreas() {
        return Collections.unmodifiableList(new ArrayList<MobArea>(mobAreas));
    }

    /** Registra um POI lógico após o gerador reservar seu tile no PNG. */
    public static void registerPoi(PoiType type, RegionType region, int tileX, int tileY) {
        if (!active || type == null || region == null) {
            return;
        }
        pointsOfInterest.add(new PointOfInterest(type, region, tileX, tileY));
    }

    public static List<PointOfInterest> getPointsOfInterest() {
        return Collections.unmodifiableList(new ArrayList<PointOfInterest>(pointsOfInterest));
    }

    /** Atualiza a região atual e inicia o banner de entrada quando necessário. */
    public static boolean updatePlayerPosition(int pixelX, int pixelY) {
        if (!active) {
            return false;
        }
        RegionType next = regionForTile(pixelX / World.TILE_SIZE, pixelY / World.TILE_SIZE);
        if (next == currentRegion) {
            return false;
        }
        currentRegion = next;
        regionBannerFrames = 150;
        return true;
    }

    public static RegionType getCurrentRegion() {
        return currentRegion;
    }

    public static String getCurrentRegionName() {
        if (dungeonMode && currentRegion != null) {
            return "Masmorra: " + currentRegion.getDisplayName();
        }
        return currentRegion == null ? "Exploração" : currentRegion.getDisplayName();
    }

    public static String getCurrentRegionSubtitle() {
        if (dungeonMode) {
            return "instância procedural — derrote o chefe e encontre a saída";
        }
        return currentRegion == null ? "mapeando o setor" : currentRegion.getSubtitle();
    }

    public static int getRegionBannerFrames() {
        return regionBannerFrames;
    }

    public static void tick() {
        if (regionBannerFrames > 0) {
            regionBannerFrames--;
        }
    }

    /** Encontra o POI mais próximo dentro do raio informado, se houver. */
    public static PointOfInterest nearestPoi(int pixelX, int pixelY, int tileRadius) {
        if (!active) {
            return null;
        }
        int tileX = pixelX / World.TILE_SIZE;
        int tileY = pixelY / World.TILE_SIZE;
        PointOfInterest nearest = null;
        int bestDistance = Integer.MAX_VALUE;
        for (PointOfInterest poi : pointsOfInterest) {
            int dx = poi.tileX - tileX;
            int dy = poi.tileY - tileY;
            int distance = dx * dx + dy * dy;
            if (distance <= tileRadius * tileRadius && distance < bestDistance) {
                nearest = poi;
                bestDistance = distance;
            }
        }
        return nearest;
    }
}
