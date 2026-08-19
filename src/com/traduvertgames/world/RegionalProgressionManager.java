package com.traduvertgames.world;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Progressão por região: reputação conquistada e ameaça inimiga restante. */
public final class RegionalProgressionManager {

    private static final int DEFAULT_THREAT = 50;
    private static final EnumMap<RpgWorldManager.RegionType, Integer> reputation =
            new EnumMap<RpgWorldManager.RegionType, Integer>(RpgWorldManager.RegionType.class);
    private static final EnumMap<RpgWorldManager.RegionType, Integer> threat =
            new EnumMap<RpgWorldManager.RegionType, Integer>(RpgWorldManager.RegionType.class);

    private RegionalProgressionManager() {
    }

    public static void reset() {
        reputation.clear();
        threat.clear();
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            reputation.put(region, 0);
            threat.put(region, DEFAULT_THREAT);
        }
    }

    private static void ensureInitialized() {
        if (reputation.size() != RpgWorldManager.RegionType.values().length) {
            reset();
        }
    }

    public static int getReputation(RpgWorldManager.RegionType region) {
        ensureInitialized();
        return region == null ? 0 : reputation.getOrDefault(region, 0);
    }

    public static int getThreat(RpgWorldManager.RegionType region) {
        ensureInitialized();
        return region == null ? DEFAULT_THREAT : threat.getOrDefault(region, DEFAULT_THREAT);
    }

    public static void addReputation(RpgWorldManager.RegionType region, int amount) {
        if (region == null || amount == 0) {
            return;
        }
        ensureInitialized();
        reputation.put(region, clamp(getReputation(region) + amount));
    }

    public static void addThreat(RpgWorldManager.RegionType region, int amount) {
        if (region == null || amount == 0) {
            return;
        }
        ensureInitialized();
        threat.put(region, clamp(getThreat(region) + amount));
    }

    /** Aplica a consequência de um evento regional concluído ou perdido. */
    public static void registerEventOutcome(RpgWorldManager.RegionType region, boolean success) {
        if (success) {
            addReputation(region, 10);
            addThreat(region, -8);
        } else {
            addReputation(region, -3);
            addThreat(region, 6);
        }
    }

    /** Dungeons controlam um pouco mais da ameaça da região que os eventos comuns. */
    public static void registerDungeonComplete(RpgWorldManager.RegionType region) {
        addReputation(region, 18);
        addThreat(region, -12);
    }

    public static String getReputationTier(RpgWorldManager.RegionType region) {
        int value = getReputation(region);
        if (value >= 90) {
            return "Lenda regional";
        }
        if (value >= 60) {
            return "Aliado confiável";
        }
        if (value >= 25) {
            return "Defensor reconhecido";
        }
        return "Recém-chegado";
    }

    public static String getThreatLabel(RpgWorldManager.RegionType region) {
        int value = getThreat(region);
        if (value >= 75) {
            return "Crítica";
        }
        if (value >= 50) {
            return "Alta";
        }
        if (value >= 25) {
            return "Instável";
        }
        return "Baixa";
    }

    public static String getSummary(RpgWorldManager.RegionType region) {
        return "Reputação " + getReputation(region) + "/100 — " + getReputationTier(region)
                + " | Ameaça " + getThreat(region) + "/100 — " + getThreatLabel(region);
    }

    public static Map<String, Object> serialize() {
        ensureInitialized();
        Map<String, Object> root = new HashMap<String, Object>();
        Map<String, Object> reputationData = new HashMap<String, Object>();
        Map<String, Object> threatData = new HashMap<String, Object>();
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            reputationData.put(region.name(), getReputation(region));
            threatData.put(region.name(), getThreat(region));
        }
        root.put("reputation", reputationData);
        root.put("threat", threatData);
        return root;
    }

    public static void deserialize(Object raw) {
        reset();
        if (!(raw instanceof Map<?, ?>)) {
            return;
        }
        Map<?, ?> root = (Map<?, ?>) raw;
        Map<?, ?> reputationData = asMap(root.get("reputation"));
        Map<?, ?> threatData = asMap(root.get("threat"));
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            if (reputationData != null && reputationData.containsKey(region.name())) {
                reputation.put(region, clamp(toInt(reputationData.get(region.name()))));
            }
            if (threatData != null && threatData.containsKey(region.name())) {
                threat.put(region, clamp(toInt(threatData.get(region.name()))));
            }
        }
    }

    private static Map<?, ?> asMap(Object raw) {
        return raw instanceof Map<?, ?> ? (Map<?, ?>) raw : null;
    }

    private static int toInt(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
