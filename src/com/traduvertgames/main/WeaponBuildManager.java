package com.traduvertgames.main;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.graficos.MissionBanner;

/** Progressão de build por arma: XP de abates, nível e caminho de especialização. */
public final class WeaponBuildManager {

    public enum BuildPath {
        POWER("NÚCLEO DE POTÊNCIA", "+10% dano por nível"),
        RAPID("CADÊNCIA TÁTICA", "-1 frame de recarga por nível"),
        MULTI("FRAGMENTAÇÃO", "+1 projétil a partir do nível 3");

        private final String label;
        private final String description;

        BuildPath(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final int MAX_LEVEL = 5;
    private static final EnumMap<WeaponType, Integer> levels =
            new EnumMap<WeaponType, Integer>(WeaponType.class);
    private static final EnumMap<WeaponType, Integer> xp =
            new EnumMap<WeaponType, Integer>(WeaponType.class);
    private static final EnumMap<WeaponType, BuildPath> paths =
            new EnumMap<WeaponType, BuildPath>(WeaponType.class);

    private WeaponBuildManager() {
    }

    public static void reset() {
        levels.clear();
        xp.clear();
        paths.clear();
        for (WeaponType type : WeaponType.values()) {
            levels.put(type, 0);
            xp.put(type, 0);
            paths.put(type, BuildPath.POWER);
        }
    }

    public static void onEnemyDefeated() {
        if (Game.player == null || Game.player.getCurrentWeaponType() == null) {
            return;
        }
        addXp(Game.player.getCurrentWeaponType(), 1);
    }

    public static void addXp(WeaponType type, int amount) {
        if (type == null || amount <= 0) {
            return;
        }
        ensure(type);
        int level = getLevel(type);
        if (level >= MAX_LEVEL) {
            return;
        }
        int nextXp = getXp(type) + amount;
        int required = xpForNextLevel(type);
        while (nextXp >= required && level < MAX_LEVEL) {
            nextXp -= required;
            level++;
            required = xpForNextLevelAt(level);
            MissionBanner.show("BUILD EVOLUÍDA", type.getShortName() + " — nível " + level + "/" + MAX_LEVEL,
                    new java.awt.Color(255, 193, 7), java.awt.Color.WHITE, 120);
        }
        levels.put(type, level);
        xp.put(type, Math.max(0, nextXp));
    }

    public static int getLevel(WeaponType type) {
        ensure(type);
        return levels.get(type);
    }

    public static int getXp(WeaponType type) {
        ensure(type);
        return xp.get(type);
    }

    public static int xpForNextLevel(WeaponType type) {
        return xpForNextLevelAt(getLevel(type));
    }

    private static int xpForNextLevelAt(int level) {
        return 8 + level * 5;
    }

    public static BuildPath getPath(WeaponType type) {
        ensure(type);
        return paths.get(type);
    }

    public static void setPath(WeaponType type, BuildPath path) {
        if (type == null || path == null) {
            return;
        }
        ensure(type);
        paths.put(type, path);
    }

    public static BuildPath cyclePath(WeaponType type, boolean forward) {
        if (type == null) {
            return BuildPath.POWER;
        }
        BuildPath[] values = BuildPath.values();
        int current = getPath(type).ordinal();
        int next = Math.floorMod(current + (forward ? 1 : -1), values.length);
        setPath(type, values[next]);
        return values[next];
    }

    public static double getDamage(WeaponType type) {
        if (type == null) {
            return 0;
        }
        double multiplier = 1.0;
        if (getPath(type) == BuildPath.POWER) {
            multiplier += getLevel(type) * 0.10;
        }
        return type.getDamage() * multiplier;
    }

    public static int getFireDelayFrames(WeaponType type) {
        if (type == null) {
            return 0;
        }
        int reduction = getPath(type) == BuildPath.RAPID ? getLevel(type) : 0;
        return Math.max(1, type.getFireDelayFrames() - reduction);
    }

    public static double getManaCost(WeaponType type) {
        if (type == null) {
            return 0;
        }
        double multiplier = getPath(type) == BuildPath.RAPID ? Math.max(0.65, 1.0 - getLevel(type) * 0.06) : 1.0;
        return type.getManaCost() * multiplier;
    }

    public static double getDurabilityCost(WeaponType type) {
        if (type == null) {
            return 0;
        }
        double multiplier = getPath(type) == BuildPath.RAPID ? Math.max(0.65, 1.0 - getLevel(type) * 0.05) : 1.0;
        return type.getDurabilityCost() * multiplier;
    }

    public static int getProjectilesPerShot(WeaponType type) {
        if (type == null) {
            return 1;
        }
        int bonus = getPath(type) == BuildPath.MULTI && getLevel(type) >= 3 ? 1 : 0;
        return Math.max(1, type.getProjectilesPerShot() + bonus);
    }

    public static String getSummary(WeaponType type) {
        if (type == null) {
            return "Build indisponível";
        }
        return type.getShortName() + " Nv." + getLevel(type) + " — " + getPath(type).getLabel()
                + " (" + getXp(type) + "/" + xpForNextLevel(type) + " XP)";
    }

    public static Map<String, Object> serialize() {
        Map<String, Object> snapshot = new HashMap<String, Object>();
        for (WeaponType type : WeaponType.values()) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("level", getLevel(type));
            row.put("xp", getXp(type));
            row.put("path", getPath(type).name());
            snapshot.put(type.name(), row);
        }
        return snapshot;
    }

    public static void deserialize(Object raw) {
        reset();
        if (!(raw instanceof Map<?, ?>)) {
            return;
        }
        Map<?, ?> snapshot = (Map<?, ?>) raw;
        for (WeaponType type : WeaponType.values()) {
            Object rawRow = snapshot.get(type.name());
            if (!(rawRow instanceof Map<?, ?>)) {
                continue;
            }
            Map<?, ?> row = (Map<?, ?>) rawRow;
            int level = row.get("level") instanceof Number ? ((Number) row.get("level")).intValue() : 0;
            int value = row.get("xp") instanceof Number ? ((Number) row.get("xp")).intValue() : 0;
            levels.put(type, Math.max(0, Math.min(MAX_LEVEL, level)));
            xp.put(type, Math.max(0, value));
            Object rawPath = row.get("path");
            if (rawPath != null) {
                try {
                    paths.put(type, BuildPath.valueOf(String.valueOf(rawPath)));
                } catch (IllegalArgumentException ignored) {
                    paths.put(type, BuildPath.POWER);
                }
            }
        }
    }

    private static void ensure(WeaponType type) {
        if (type == null) {
            return;
        }
        if (!levels.containsKey(type)) {
            levels.put(type, 0);
        }
        if (!xp.containsKey(type)) {
            xp.put(type, 0);
        }
        if (!paths.containsKey(type)) {
            paths.put(type, BuildPath.POWER);
        }
    }

    static {
        reset();
    }
}
