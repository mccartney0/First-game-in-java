package com.traduvertgames.world;

import java.awt.Color;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.main.Game;

/**
 * Simulação ambiental determinística da superfície do Mundo Aberto. O período
 * é global, enquanto o clima é calculado por setor para que colossos em áreas
 * diferentes possam reagir a condições distintas sem salvar 40 estados soltos.
 */
public final class WorldWeatherManager {

    public enum TimeOfDay {
        DAY("DIA", 0.92, 0.95, 1.08, new Color(255, 238, 188, 12)),
        DUSK("CREPÚSCULO", 1.00, 1.00, 1.00, new Color(255, 118, 70, 34)),
        NIGHT("NOITE", 1.08, 1.06, 0.93, new Color(20, 42, 94, 92)),
        DAWN("AURORA", 0.97, 0.98, 1.03, new Color(92, 202, 238, 24));

        private final String label;
        private final double giantSpeed;
        private final double giantDamage;
        private final double giantCooldown;
        private final Color tint;

        TimeOfDay(String label, double giantSpeed, double giantDamage, double giantCooldown, Color tint) {
            this.label = label;
            this.giantSpeed = giantSpeed;
            this.giantDamage = giantDamage;
            this.giantCooldown = giantCooldown;
            this.tint = tint;
        }
    }

    public enum Weather {
        CLEAR("CÉU LIMPO", 1.00, 1.00, 1.00, 0.0, new Color(186, 240, 224)),
        RAIN("CHUVA IÔNICA", 0.92, 0.95, 1.08, 0.0, new Color(91, 180, 255)),
        FOG("NEBLINA", 1.06, 1.00, 0.98, 0.0, new Color(203, 228, 221)),
        ASH("CINZAS MAGNÉTICAS", 0.94, 1.04, 0.98, 0.006, new Color(255, 159, 90)),
        ION_STORM("TEMPESTADE IÔNICA", 1.11, 1.12, 0.85, 0.010, new Color(130, 126, 255)),
        ACID_DRIZZLE("GAROÁCIDA", 0.97, 1.09, 0.95, 0.016, new Color(155, 255, 70)),
        CRYSTAL_SQUALL("NEVASCA CRISTALINA", 0.90, 1.03, 1.04, 0.004, new Color(130, 228, 255)),
        VOID_ECLIPSE("ECLIPSE DO VAZIO", 1.15, 1.16, 0.82, 0.022, new Color(255, 70, 134));

        private final String label;
        private final double giantSpeed;
        private final double giantDamage;
        private final double giantCooldown;
        private final double giantRegen;
        private final Color signalColor;

        Weather(String label, double giantSpeed, double giantDamage, double giantCooldown,
                double giantRegen, Color signalColor) {
            this.label = label;
            this.giantSpeed = giantSpeed;
            this.giantDamage = giantDamage;
            this.giantCooldown = giantCooldown;
            this.giantRegen = giantRegen;
            this.signalColor = signalColor;
        }

        public Color getSignalColor() {
            return signalColor;
        }
    }

    public static final class GiantModifier {
        private final double speedMultiplier;
        private final double damageMultiplier;
        private final double cooldownMultiplier;
        private final double regenPerTick;
        private final Weather weather;
        private final TimeOfDay time;

        private GiantModifier(double speedMultiplier, double damageMultiplier, double cooldownMultiplier,
                double regenPerTick, Weather weather, TimeOfDay time) {
            this.speedMultiplier = speedMultiplier;
            this.damageMultiplier = damageMultiplier;
            this.cooldownMultiplier = cooldownMultiplier;
            this.regenPerTick = regenPerTick;
            this.weather = weather;
            this.time = time;
        }

        public double getSpeedMultiplier() { return speedMultiplier; }
        public double getDamageMultiplier() { return damageMultiplier; }
        public double getCooldownMultiplier() { return cooldownMultiplier; }
        public double getRegenPerTick() { return regenPerTick; }
        public Weather getWeather() { return weather; }
        public TimeOfDay getTime() { return time; }
    }

    private static final int DAY_FRAMES = 1500;
    private static final int DUSK_FRAMES = 420;
    private static final int NIGHT_FRAMES = 1260;
    private static final int DAWN_FRAMES = 420;
    private static final int CLIMATE_EPOCH_FRAMES = 1080;
    private static final int CYCLE_FRAMES = DAY_FRAMES + DUSK_FRAMES + NIGHT_FRAMES + DAWN_FRAMES;

    private static boolean active;
    private static long worldSeed;
    private static int timeTicks;
    private static final GiantModifier NEUTRAL_MODIFIER = new GiantModifier(1.0, 1.0, 1.0, 0.0,
            Weather.CLEAR, TimeOfDay.DAY);

    private WorldWeatherManager() {
    }

    public static void configure(long seed) {
        if (!active || worldSeed != seed) {
            timeTicks = 0;
        }
        active = true;
        worldSeed = seed;
    }

    public static void reset() {
        active = false;
        worldSeed = 0L;
        timeTicks = 0;
    }

    public static boolean isActive() {
        return active && Game.isOpenWorldMode() && OpenWorldManager.isActive();
    }

    public static void update() {
        if (isActive()) {
            timeTicks = (timeTicks + 1) % CYCLE_FRAMES;
        }
    }

    public static TimeOfDay getTimeOfDay() {
        int tick = Math.floorMod(timeTicks, CYCLE_FRAMES);
        if (tick < DAY_FRAMES) return TimeOfDay.DAY;
        if (tick < DAY_FRAMES + DUSK_FRAMES) return TimeOfDay.DUSK;
        if (tick < DAY_FRAMES + DUSK_FRAMES + NIGHT_FRAMES) return TimeOfDay.NIGHT;
        return TimeOfDay.DAWN;
    }

    public static Weather getWeatherAtTile(int tileX, int tileY) {
        if (!isActive()) {
            return Weather.CLEAR;
        }
        int sectorX = Math.max(0, Math.min(OpenWorldManager.getChunkColumns() - 1,
                tileX / OpenWorldManager.CHUNK_SIZE_TILES));
        int sectorY = Math.max(0, Math.min(OpenWorldManager.getChunkRows() - 1,
                tileY / OpenWorldManager.CHUNK_SIZE_TILES));
        RpgWorldManager.RegionType region = RpgWorldManager.regionForTile(tileX, tileY);
        Weather[] palette = paletteFor(region);
        long salt = worldSeed ^ ((long) sectorX * 0x9E3779B97F4A7C15L)
                ^ ((long) sectorY * 0xC2B2AE3D27D4EB4FL)
                ^ ((long) (timeTicks / CLIMATE_EPOCH_FRAMES) * 0x165667B19E3779F9L);
        int mixed = (int) (salt ^ (salt >>> 32));
        mixed ^= mixed >>> 16;
        mixed *= 0x7FEB352D;
        mixed ^= mixed >>> 15;
        return palette[Math.floorMod(mixed, palette.length)];
    }

    public static Weather getCurrentWeather() {
        if (Game.player == null) {
            return Weather.CLEAR;
        }
        return getWeatherAtTile(Game.player.getX() / World.TILE_SIZE, Game.player.getY() / World.TILE_SIZE);
    }

    public static GiantModifier getGiantModifier(int pixelX, int pixelY) {
        if (!isActive()) {
            return NEUTRAL_MODIFIER;
        }
        Weather weather = getWeatherAtTile(pixelX / World.TILE_SIZE, pixelY / World.TILE_SIZE);
        TimeOfDay time = getTimeOfDay();
        return new GiantModifier(clamp(weather.giantSpeed * time.giantSpeed, 0.82, 1.25),
                clamp(weather.giantDamage * time.giantDamage, 0.86, 1.24),
                clamp(weather.giantCooldown * time.giantCooldown, 0.76, 1.16),
                weather.giantRegen, weather, time);
    }

    public static GiantModifier neutralGiantModifier() {
        return NEUTRAL_MODIFIER;
    }

    public static String getCurrentClimateLabel() {
        return getTimeOfDay().label + " · " + getCurrentWeather().label;
    }

    public static Color getCurrentSignalColor() {
        return getCurrentWeather().getSignalColor();
    }

    public static void renderAmbient(Graphics graphics) {
        if (!isActive() || graphics == null) {
            return;
        }
        TimeOfDay time = getTimeOfDay();
        graphics.setColor(time.tint);
        graphics.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
        Weather weather = getCurrentWeather();
        if (weather == Weather.FOG) {
            graphics.setColor(new Color(210, 235, 225, 26));
            for (int band = 0; band < 4; band++) {
                int y = Math.floorMod(timeTicks / 4 + band * 47, Game.HEIGHT + 26) - 13;
                graphics.fillRect(0, y, Game.WIDTH, 13);
            }
        } else if (weather == Weather.RAIN || weather == Weather.ION_STORM || weather == Weather.ACID_DRIZZLE
                || weather == Weather.CRYSTAL_SQUALL) {
            Color particle = weather == Weather.ACID_DRIZZLE ? new Color(166, 255, 89, 110)
                    : weather == Weather.ION_STORM ? new Color(166, 132, 255, 130)
                    : weather == Weather.CRYSTAL_SQUALL ? new Color(158, 231, 255, 120)
                    : new Color(112, 190, 255, 100);
            graphics.setColor(particle);
            for (int drop = 0; drop < 17; drop++) {
                int x = Math.floorMod(timeTicks * 3 + drop * 37, Game.WIDTH + 16) - 8;
                int y = Math.floorMod(timeTicks * 5 + drop * 29, Game.HEIGHT + 20) - 10;
                graphics.drawLine(x, y, x - 2, y + 5);
            }
        } else if (weather == Weather.VOID_ECLIPSE) {
            graphics.setColor(new Color(82, 0, 45, 54));
            graphics.fillRect(0, 0, Game.WIDTH, Game.HEIGHT);
        }
    }

    public static Map<String, Object> serialize() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("active", active);
        result.put("seed", worldSeed);
        result.put("timeTicks", timeTicks);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static void deserialize(Object raw) {
        if (!(raw instanceof Map)) {
            reset();
            return;
        }
        Map<String, Object> data = (Map<String, Object>) raw;
        active = Boolean.TRUE.equals(data.get("active"));
        worldSeed = data.get("seed") instanceof Number ? ((Number) data.get("seed")).longValue() : 0L;
        timeTicks = data.get("timeTicks") instanceof Number
                ? Math.floorMod(((Number) data.get("timeTicks")).intValue(), CYCLE_FRAMES) : 0;
    }

    private static Weather[] paletteFor(RpgWorldManager.RegionType region) {
        if (region == RpgWorldManager.RegionType.RUINS) {
            return new Weather[] { Weather.CLEAR, Weather.ASH, Weather.ION_STORM, Weather.RAIN };
        }
        if (region == RpgWorldManager.RegionType.SANCTUARY) {
            return new Weather[] { Weather.FOG, Weather.ION_STORM, Weather.ION_STORM, Weather.CLEAR };
        }
        if (region == RpgWorldManager.RegionType.MARSH) {
            return new Weather[] { Weather.RAIN, Weather.ACID_DRIZZLE, Weather.FOG, Weather.RAIN };
        }
        if (region == RpgWorldManager.RegionType.TUNDRA) {
            return new Weather[] { Weather.CRYSTAL_SQUALL, Weather.CLEAR, Weather.FOG, Weather.ION_STORM };
        }
        if (region == RpgWorldManager.RegionType.CORE) {
            return new Weather[] { Weather.ION_STORM, Weather.VOID_ECLIPSE, Weather.ASH, Weather.ION_STORM };
        }
        return new Weather[] { Weather.CLEAR, Weather.CLEAR, Weather.FOG, Weather.RAIN };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
