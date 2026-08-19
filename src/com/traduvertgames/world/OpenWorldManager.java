package com.traduvertgames.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Estado de exploração contínua do modo Mundo Aberto. */
public final class OpenWorldManager {

    public static final int CHUNK_SIZE_TILES = 64;

    private static boolean active;
    private static int worldWidth;
    private static int worldHeight;
    private static long seed;
    private static int activeChunkX = -1;
    private static int activeChunkY = -1;
    private static final Set<Long> discoveredChunks = new HashSet<Long>();

    private OpenWorldManager() {
    }

    public static void configure(int width, int height, long worldSeed) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        if (!active || worldWidth != safeWidth || worldHeight != safeHeight || seed != worldSeed) {
            discoveredChunks.clear();
            activeChunkX = -1;
            activeChunkY = -1;
        }
        active = true;
        worldWidth = safeWidth;
        worldHeight = safeHeight;
        seed = worldSeed;
    }

    public static void reset() {
        active = false;
        worldWidth = 0;
        worldHeight = 0;
        seed = 0L;
        activeChunkX = -1;
        activeChunkY = -1;
        discoveredChunks.clear();
    }

    public static boolean isActive() {
        return active;
    }

    public static int getWorldWidth() {
        return worldWidth;
    }

    public static int getWorldHeight() {
        return worldHeight;
    }

    public static long getSeed() {
        return seed;
    }

    public static int getActiveChunkX() {
        return activeChunkX;
    }

    public static int getActiveChunkY() {
        return activeChunkY;
    }

    /** Código de referência humano para a grade do atlas, como B1 ou H5. */
    public static String getSectorCode(int chunkX, int chunkY) {
        if (chunkX < 0 || chunkY < 0 || chunkX >= getChunkColumns() || chunkY >= getChunkRows()) {
            return "--";
        }
        return String.valueOf((char) ('A' + chunkX)) + (chunkY + 1);
    }

    public static String getActiveSectorCode() {
        return getSectorCode(activeChunkX, activeChunkY);
    }

    /** Atualiza a célula lógica atual e retorna true ao entrar em um chunk novo. */
    public static boolean updatePlayerPosition(int pixelX, int pixelY) {
        if (!active) {
            return false;
        }
        int tileX = Math.floorDiv(pixelX, World.TILE_SIZE);
        int tileY = Math.floorDiv(pixelY, World.TILE_SIZE);
        int chunkX = Math.max(0, Math.min(getChunkColumns() - 1, Math.floorDiv(tileX, CHUNK_SIZE_TILES)));
        int chunkY = Math.max(0, Math.min(getChunkRows() - 1, Math.floorDiv(tileY, CHUNK_SIZE_TILES)));
        boolean changed = chunkX != activeChunkX || chunkY != activeChunkY;
        activeChunkX = chunkX;
        activeChunkY = chunkY;
        discoveredChunks.add(key(chunkX, chunkY));
        return changed;
    }

    public static int getChunkColumns() {
        return Math.max(1, (worldWidth + CHUNK_SIZE_TILES - 1) / CHUNK_SIZE_TILES);
    }

    public static int getChunkRows() {
        return Math.max(1, (worldHeight + CHUNK_SIZE_TILES - 1) / CHUNK_SIZE_TILES);
    }

    public static int getTotalChunkCount() {
        return getChunkColumns() * getChunkRows();
    }

    public static int getDiscoveredChunkCount() {
        return discoveredChunks.size();
    }

    /** Consulta direta usada pelo HUD para mascarar setores ainda não explorados. */
    public static boolean isChunkDiscovered(int chunkX, int chunkY) {
        if (chunkX < 0 || chunkY < 0 || chunkX >= getChunkColumns() || chunkY >= getChunkRows()) {
            return false;
        }
        return discoveredChunks.contains(key(chunkX, chunkY));
    }

    public static double getExplorationPercent() {
        int total = getTotalChunkCount();
        return total <= 0 ? 0.0 : discoveredChunks.size() * 100.0 / total;
    }

    public static String getExplorationLabel() {
        return "Setores " + discoveredChunks.size() + "/" + getTotalChunkCount();
    }

    public static List<String> getDiscoveredChunkKeys() {
        List<String> result = new ArrayList<String>();
        for (Long value : discoveredChunks) {
            int x = (int) (value.longValue() >> 32);
            int y = (int) value.longValue();
            result.add(x + "," + y);
        }
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    public static Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("active", active);
        data.put("width", worldWidth);
        data.put("height", worldHeight);
        data.put("seed", seed);
        data.put("activeChunkX", activeChunkX);
        data.put("activeChunkY", activeChunkY);
        data.put("discoveredChunks", new ArrayList<String>(getDiscoveredChunkKeys()));
        return data;
    }

    @SuppressWarnings("unchecked")
    public static void deserialize(Object raw) {
        if (!(raw instanceof Map)) {
            return;
        }
        Map<String, Object> data = (Map<String, Object>) raw;
        active = Boolean.TRUE.equals(data.get("active"));
        worldWidth = toInt(data.get("width"));
        worldHeight = toInt(data.get("height"));
        seed = toLong(data.get("seed"));
        activeChunkX = toInt(data.get("activeChunkX"));
        activeChunkY = toInt(data.get("activeChunkY"));
        discoveredChunks.clear();
        Object rawChunks = data.get("discoveredChunks");
        if (rawChunks instanceof List) {
            for (Object rawChunk : (List<Object>) rawChunks) {
                String[] parts = String.valueOf(rawChunk).split(",", 2);
                if (parts.length != 2) {
                    continue;
                }
                try {
                    discoveredChunks.add(key(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
                } catch (NumberFormatException ignored) {
                    // Dados corrompidos de exploração não impedem o load do slot.
                }
            }
        }
    }

    private static long key(int x, int y) {
        return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
    }

    private static int toInt(Object raw) {
        return raw instanceof Number ? ((Number) raw).intValue() : -1;
    }

    private static long toLong(Object raw) {
        return raw instanceof Number ? ((Number) raw).longValue() : 0L;
    }
}
