package com.traduvertgames.rpg;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lê os parâmetros exportados pelo Content Studio sem introduzir dependência de parser JSON no jogo. */
final class RpgContentEnemyProfile {
    private final String assetId;
    private final int baseLife;
    private final int baseDamage;
    private final double speed;
    private final String behaviorTag;

    private RpgContentEnemyProfile(String assetId, int baseLife, int baseDamage, double speed, String behaviorTag) {
        this.assetId = assetId;
        this.baseLife = Math.max(1, baseLife);
        this.baseDamage = Math.max(1, baseDamage);
        this.speed = Math.max(0.1, speed);
        this.behaviorTag = behaviorTag == null || behaviorTag.trim().isEmpty() ? "hunt" : behaviorTag.trim();
    }

    static RpgContentEnemyProfile load(String assetId, int fallbackLife, int fallbackDamage,
            double fallbackSpeed, String fallbackBehavior) {
        String safeId = assetId == null ? "enemy" : assetId.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "_");
        String resource = "/assets/generated/enemies/" + safeId + ".json";
        try (InputStream input = RpgContentEnemyProfile.class.getResourceAsStream(resource)) {
            if (input == null) return new RpgContentEnemyProfile(safeId, fallbackLife, fallbackDamage, fallbackSpeed, fallbackBehavior);
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return new RpgContentEnemyProfile(safeId,
                    intValue(json, "baseLife", fallbackLife),
                    intValue(json, "baseDamage", fallbackDamage),
                    doubleValue(json, "speed", fallbackSpeed),
                    stringValue(json, "behaviorTag", fallbackBehavior));
        } catch (Exception ignored) {
            return new RpgContentEnemyProfile(safeId, fallbackLife, fallbackDamage, fallbackSpeed, fallbackBehavior);
        }
    }

    private static int intValue(String json, String key, int fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)").matcher(json);
        return matcher.find() ? Math.max(1, Integer.parseInt(matcher.group(1))) : fallback;
    }

    private static double doubleValue(String json, String key, double fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        return matcher.find() ? Math.max(0.1, Double.parseDouble(matcher.group(1))) : fallback;
    }

    private static String stringValue(String json, String key, String fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1) : fallback;
    }

    String getAssetId() { return assetId; }
    int getBaseLife() { return baseLife; }
    int getBaseDamage() { return baseDamage; }
    double getSpeed() { return speed; }
    String getBehaviorTag() { return behaviorTag; }
}
