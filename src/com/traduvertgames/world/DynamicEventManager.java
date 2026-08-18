package com.traduvertgames.world;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.FloatingText;
import com.traduvertgames.graficos.MissionBanner;
import com.traduvertgames.main.Game;
import com.traduvertgames.state.PilotUpgrades;

/** Eventos temporários que dão propósito à exploração entre hubs e dungeons. */
public final class DynamicEventManager {

    public enum Type {
        AMBUSH("Emboscada regional", "Derrote o grupo que cercou a rota de exploração."),
        ELITE_HUNT("Caça à elite", "Encontre e elimine o alvo de elite antes que ele recue.");

        private final String title;
        private final String description;

        Type(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final int AUTO_TRIGGER_FRAMES = 900;
    private static final int EVENT_DURATION_FRAMES = 60 * 90;
    private static final Map<String, Boolean> completed = new HashMap<String, Boolean>();
    private static final Set<Enemy> eventEnemies = new HashSet<Enemy>();

    private static Type activeType;
    private static RpgWorldManager.RegionType activeRegion;
    private static int activeDepth;
    private static int activeTimer;
    private static int explorationFrames;
    private static RpgWorldManager.RegionType observedRegion;
    private static boolean needsSpawn;
    private static int eventSequence;

    private DynamicEventManager() {
    }

    public static void reset() {
        completed.clear();
        eventEnemies.clear();
        activeType = null;
        activeRegion = null;
        activeDepth = 0;
        activeTimer = 0;
        explorationFrames = 0;
        observedRegion = null;
        needsSpawn = false;
        eventSequence = 0;
    }

    /** Encerra apenas a atividade corrente ao trocar de mapa/camada. */
    public static void abortActiveEventForMapChange() {
        eventEnemies.clear();
        activeType = null;
        activeRegion = null;
        activeDepth = 0;
        activeTimer = 0;
        explorationFrames = 0;
        needsSpawn = false;
    }

    public static boolean isActive() {
        return activeType != null;
    }

    public static Type getActiveType() {
        return activeType;
    }

    public static RpgWorldManager.RegionType getActiveRegion() {
        return activeRegion;
    }

    public static int getActiveTimer() {
        return activeTimer;
    }

    public static String getActiveTitle() {
        return activeType == null ? "" : activeType.getTitle();
    }

    public static String getActiveDescription() {
        return activeType == null ? "" : activeType.getDescription();
    }

    public static String getProgressLabel() {
        if (activeType == null) {
            return "";
        }
        if (activeType == Type.ELITE_HUNT) {
            return eventEnemies.isEmpty() ? "alvo localizado" : "alvo ativo";
        }
        return eventEnemies.size() + " ameaças restantes";
    }

    /** Indica se pelo menos um dos eventos regionais ainda está disponível. */
    public static boolean hasAvailableEvent(RpgWorldManager.RegionType region) {
        if (region == null || isActive() || !RpgWorldManager.isActive() || RpgWorldManager.isDungeonMode()) {
            return false;
        }
        int depth = Math.max(1, RpgWorldManager.getDepth());
        for (Type type : Type.values()) {
            if (!isCompleted(region, depth, type)) {
                return true;
            }
        }
        return false;
    }

    /** Inicia manualmente um evento escolhido no hub. */
    public static boolean startEventForCurrentRegion(Type preferred) {
        if (!hasAvailableEvent(RpgWorldManager.getCurrentRegion())) {
            return false;
        }
        Type chosen = preferred;
        if (chosen == null || isCompleted(RpgWorldManager.getCurrentRegion(), Math.max(1, RpgWorldManager.getDepth()), chosen)) {
            chosen = chooseAvailableType(RpgWorldManager.getCurrentRegion(), Math.max(1, RpgWorldManager.getDepth()));
        }
        return startEvent(chosen, true);
    }

    private static Type chooseAvailableType(RpgWorldManager.RegionType region, int depth) {
        Type[] values = Type.values();
        for (int i = 0; i < values.length; i++) {
            Type candidate = values[(eventSequence + region.ordinal() + depth + i) % values.length];
            if (!isCompleted(region, depth, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean startEvent(Type type, boolean manual) {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        int depth = Math.max(1, RpgWorldManager.getDepth());
        if (type == null || region == null || isActive() || RpgWorldManager.isDungeonMode()
                || isCompleted(region, depth, type) || Game.player == null) {
            return false;
        }
        activeType = type;
        activeRegion = region;
        activeDepth = depth;
        activeTimer = EVENT_DURATION_FRAMES;
        eventEnemies.clear();
        needsSpawn = true;
        eventSequence++;
        if (manual) {
            MissionBanner.show("EVENTO REGIONAL", type.getTitle() + " — " + type.getDescription(),
                    new Color(255, 152, 0), Color.WHITE, 180);
        }
        return true;
    }

    /** Atualiza o relógio de exploração e gera um evento ocasional. */
    public static void update() {
        if (!RpgWorldManager.isActive() || RpgWorldManager.isDungeonMode() || Game.player == null) {
            return;
        }
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        if (region == null) {
            return;
        }
        if (observedRegion != region) {
            observedRegion = region;
            explorationFrames = 0;
        }
        if (activeType == null) {
            explorationFrames++;
            if (explorationFrames >= AUTO_TRIGGER_FRAMES && Game.enemies.size() <= 4) {
                Type chosen = chooseAvailableType(region, Math.max(1, RpgWorldManager.getDepth()));
                if (chosen != null) {
                    startEvent(chosen, false);
                    explorationFrames = 0;
                }
            }
            return;
        }
        if (needsSpawn) {
            spawnEventEnemies();
            needsSpawn = false;
        }
        if (activeTimer > 0) {
            activeTimer--;
        }
        if (activeTimer == 0) {
            expireEvent();
        }
    }

    private static void spawnEventEnemies() {
        if (activeType == Type.AMBUSH) {
            spawnVariant(Enemy.Variant.BOMBER, false);
            spawnVariant(Enemy.Variant.SWARM, false);
            spawnVariant(Enemy.Variant.SWARM, false);
            spawnVariant(Enemy.Variant.SHIELDER, false);
            spawnVariant(Enemy.Variant.SNIPER, false);
        } else if (activeType == Type.ELITE_HUNT) {
            Enemy target = spawnVariant(Enemy.Variant.SNIPER, true);
            if (target == null) {
                target = spawnVariant(Enemy.Variant.SHIELDER, true);
            }
            spawnVariant(Enemy.Variant.SWARM, false);
            spawnVariant(Enemy.Variant.BOMBER, false);
            if (target == null) {
                expireEvent();
            }
        }
    }

    private static Enemy spawnVariant(Enemy.Variant variant, boolean elite) {
        int[] spot = findSpawnSpot();
        if (spot == null || Game.enemies.size() >= 12) {
            return null;
        }
        Enemy enemy = new Enemy(spot[0], spot[1], 16, 16, Entity.ENEMY_EN, variant, false, elite);
        Game.entities.add(enemy);
        Game.enemies.add(enemy);
        eventEnemies.add(enemy);
        return enemy;
    }

    private static int[] findSpawnSpot() {
        for (int attempt = 0; attempt < 24; attempt++) {
            int tileX = 2 + Game.rand.nextInt(Math.max(1, World.WIDTH - 4));
            int tileY = 2 + Game.rand.nextInt(Math.max(1, World.HEIGHT - 4));
            int x = tileX * World.TILE_SIZE;
            int y = tileY * World.TILE_SIZE;
            if (!World.isFree(x, y, 0) || Game.player == null) {
                continue;
            }
            double distance = Math.hypot(x - Game.player.getX(), y - Game.player.getY());
            if (distance < 128 || distance > 320) {
                continue;
            }
            return new int[] { x, y };
        }
        return null;
    }

    /** Hook de morte do inimigo; chamado uma vez para cada alvo do evento. */
    public static void onEnemyDefeated(Enemy enemy) {
        if (activeType == null || enemy == null || !eventEnemies.remove(enemy)) {
            return;
        }
        if (activeType == Type.ELITE_HUNT && enemy.isElite()) {
            completeEvent();
        } else if (activeType == Type.AMBUSH && eventEnemies.isEmpty()) {
            completeEvent();
        }
    }

    private static void completeEvent() {
        if (activeType == null || activeRegion == null) {
            return;
        }
        String key = key(activeRegion, activeDepth, activeType);
        completed.put(key, true);
        int credits = activeType == Type.ELITE_HUNT ? 300 : 180;
        PilotUpgrades.addCredits(credits);
        Game.addScore(activeType == Type.ELITE_HUNT ? 450 : 250);
        FloatingText.show("EVENTO CONCLUÍDO +" + credits + " CRÉDITOS",
                Game.player.getX(), Game.player.getY() - 20, new Color(255, 235, 59), 120);
        MissionBanner.show("EVENTO CONCLUÍDO", activeType.getTitle() + " — recompensa permanente recebida.",
                new Color(129, 199, 132), Color.WHITE, 180);
        clearActive();
    }

    private static void expireEvent() {
        if (activeType != null) {
            MissionBanner.show("EVENTO ENCERRADO", "A oportunidade regional foi perdida; continue explorando.",
                    new Color(176, 190, 197), Color.WHITE, 120);
        }
        clearActive();
    }

    private static void clearActive() {
        eventEnemies.clear();
        activeType = null;
        activeRegion = null;
        activeDepth = 0;
        activeTimer = 0;
        needsSpawn = false;
        explorationFrames = 0;
    }

    private static String key(RpgWorldManager.RegionType region, int depth, Type type) {
        return region.name() + ":" + Math.max(1, depth) + ":" + type.name();
    }

    private static boolean isCompleted(RpgWorldManager.RegionType region, int depth, Type type) {
        return Boolean.TRUE.equals(completed.get(key(region, depth, type)));
    }

    public static Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("completed", new HashMap<String, Boolean>(completed));
        data.put("activeType", activeType == null ? "" : activeType.name());
        data.put("activeRegion", activeRegion == null ? "" : activeRegion.name());
        data.put("activeDepth", activeDepth);
        data.put("activeTimer", activeTimer);
        data.put("needsSpawn", needsSpawn);
        return data;
    }

    @SuppressWarnings("unchecked")
    public static void deserialize(Object raw) {
        reset();
        if (!(raw instanceof Map<?, ?>)) {
            return;
        }
        Map<?, ?> data = (Map<?, ?>) raw;
        Object rawCompleted = data.get("completed");
        if (rawCompleted instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawCompleted).entrySet()) {
                if (entry.getKey() != null && Boolean.TRUE.equals(entry.getValue())) {
                    completed.put(String.valueOf(entry.getKey()), true);
                }
            }
        }
        String typeName = String.valueOf(data.get("activeType"));
        String regionName = String.valueOf(data.get("activeRegion"));
        try {
            activeType = typeName.isEmpty() || "null".equals(typeName) ? null : Type.valueOf(typeName);
            activeRegion = regionName.isEmpty() || "null".equals(regionName)
                    ? null : RpgWorldManager.RegionType.valueOf(regionName);
        } catch (IllegalArgumentException invalidSave) {
            activeType = null;
            activeRegion = null;
        }
        activeDepth = number(data.get("activeDepth"));
        activeTimer = number(data.get("activeTimer"));
        needsSpawn = Boolean.TRUE.equals(data.get("needsSpawn"));
        if (activeType == null || activeRegion == null || activeTimer <= 0) {
            clearActive();
        }
    }

    private static int number(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public static void render(Graphics g) {
        if (!isActive() || g == null) {
            return;
        }
        int width = g.getClipBounds() != null ? g.getClipBounds().width : Game.WIDTH * Game.SCALE;
        int unit = Math.max(1, Game.SCALE / 4);
        int x = 9 * unit;
        int y = 38 * unit;
        int panelWidth = Math.min(width - 18 * unit, 190 * unit);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(22, 31, 45, 225));
        g2.fillRoundRect(x, y, panelWidth, 34 * unit, 6 * unit, 6 * unit);
        g2.setColor(activeType == Type.ELITE_HUNT ? new Color(255, 82, 82) : new Color(255, 152, 0));
        g2.drawRoundRect(x, y, panelWidth, 34 * unit, 6 * unit, 6 * unit);
        g2.setFont(new Font("Arial", Font.BOLD, 9 * unit));
        g2.drawString(activeType.getTitle(), x + 7 * unit, y + 12 * unit);
        g2.setFont(new Font("Arial", Font.PLAIN, 8 * unit));
        g2.setColor(Color.WHITE);
        g2.drawString(getProgressLabel(), x + 7 * unit, y + 23 * unit);
        g2.setColor(new Color(220, 230, 235));
        g2.drawString("tempo " + ((activeTimer + 59) / 60) + "s", x + panelWidth - 47 * unit, y + 12 * unit);
        g2.dispose();
    }
}
