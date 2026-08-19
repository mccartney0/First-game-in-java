package com.traduvertgames.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Marcadores pessoais de exploração, limitados a um por setor para leitura rápida. */
public final class OpenWorldMarkerManager {

    public enum Change {
        ADDED, REMOVED, LIMIT_REACHED, UNAVAILABLE
    }

    public static final int MAX_MARKERS = 12;

    public static final class Marker {
        private final int tileX;
        private final int tileY;

        private Marker(int tileX, int tileY) {
            this.tileX = tileX;
            this.tileY = tileY;
        }

        public int getTileX() {
            return tileX;
        }

        public int getTileY() {
            return tileY;
        }

        public String getSectorCode() {
            return OpenWorldManager.getSectorCode(tileX / OpenWorldManager.CHUNK_SIZE_TILES,
                    tileY / OpenWorldManager.CHUNK_SIZE_TILES);
        }
    }

    private static final Map<String, Marker> markersBySector = new HashMap<String, Marker>();

    private OpenWorldMarkerManager() {
    }

    public static void reset() {
        markersBySector.clear();
    }

    /** Alterna um marcador no setor que contém a posição do piloto. */
    public static Change toggleAtPixel(int pixelX, int pixelY) {
        if (!OpenWorldManager.isActive()) {
            return Change.UNAVAILABLE;
        }
        int tileX = Math.max(0, Math.min(OpenWorldManager.getWorldWidth() - 1,
                Math.floorDiv(pixelX, World.TILE_SIZE)));
        int tileY = Math.max(0, Math.min(OpenWorldManager.getWorldHeight() - 1,
                Math.floorDiv(pixelY, World.TILE_SIZE)));
        String sector = OpenWorldManager.getSectorCode(tileX / OpenWorldManager.CHUNK_SIZE_TILES,
                tileY / OpenWorldManager.CHUNK_SIZE_TILES);
        if (markersBySector.containsKey(sector)) {
            markersBySector.remove(sector);
            return Change.REMOVED;
        }
        if (markersBySector.size() >= MAX_MARKERS) {
            return Change.LIMIT_REACHED;
        }
        markersBySector.put(sector, new Marker(tileX, tileY));
        return Change.ADDED;
    }

    public static int getMarkerCount() {
        return markersBySector.size();
    }

    public static boolean hasMarkerInSector(int chunkX, int chunkY) {
        return markersBySector.containsKey(OpenWorldManager.getSectorCode(chunkX, chunkY));
    }

    public static List<Marker> getMarkers() {
        List<Marker> snapshot = new ArrayList<Marker>(markersBySector.values());
        Collections.sort(snapshot, (left, right) -> left.getSectorCode().compareTo(right.getSectorCode()));
        return Collections.unmodifiableList(snapshot);
    }

    public static Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<String, Object>();
        List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        for (Marker marker : getMarkers()) {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("x", marker.getTileX());
            data.put("y", marker.getTileY());
            values.add(data);
        }
        result.put("markers", values);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static void deserialize(Object raw) {
        reset();
        if (!(raw instanceof Map)) {
            return;
        }
        Object values = ((Map<String, Object>) raw).get("markers");
        if (!(values instanceof List)) {
            return;
        }
        for (Object value : (List<Object>) values) {
            if (!(value instanceof Map) || markersBySector.size() >= MAX_MARKERS) {
                continue;
            }
            Map<String, Object> data = (Map<String, Object>) value;
            int tileX = toInt(data.get("x"));
            int tileY = toInt(data.get("y"));
            if (tileX < 0 || tileY < 0 || tileX >= OpenWorldManager.getWorldWidth()
                    || tileY >= OpenWorldManager.getWorldHeight()) {
                continue;
            }
            String sector = OpenWorldManager.getSectorCode(tileX / OpenWorldManager.CHUNK_SIZE_TILES,
                    tileY / OpenWorldManager.CHUNK_SIZE_TILES);
            markersBySector.put(sector, new Marker(tileX, tileY));
        }
    }

    private static int toInt(Object raw) {
        return raw instanceof Number ? ((Number) raw).intValue() : -1;
    }
}
