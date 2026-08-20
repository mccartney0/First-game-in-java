package com.traduvertgames.rpg;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Leitor tolerante do manifesto de habilidade exportado pelo Content Studio. */
final class RpgBossAbilityProfile {
    private final int damage;
    private final int cooldownTicks;
    private final int range;

    private RpgBossAbilityProfile(int damage, int cooldownTicks, int range) {
        this.damage = Math.max(1, damage);
        this.cooldownTicks = Math.max(1, cooldownTicks);
        this.range = Math.max(24, range);
    }

    static RpgBossAbilityProfile mistSovereignDefaults() {
        return new RpgBossAbilityProfile(14, 180, 168);
    }

    static RpgBossAbilityProfile loadMistSovereignAbility() {
        String resource = "/assets/generated/abilities/mist_sovereign_nucleo_da_bruma.json";
        try (InputStream input = RpgBossAbilityProfile.class.getResourceAsStream(resource)) {
            if (input == null) return mistSovereignDefaults();
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            RpgBossAbilityProfile defaults = mistSovereignDefaults();
            return new RpgBossAbilityProfile(intValue(json, "damage", defaults.damage),
                    intValue(json, "cooldownTicks", defaults.cooldownTicks),
                    intValue(json, "range", defaults.range));
        } catch (Exception ignored) {
            return mistSovereignDefaults();
        }
    }

    private static int intValue(String json, String key, int fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+)").matcher(json);
        return matcher.find() ? Math.max(1, Integer.parseInt(matcher.group(1))) : fallback;
    }

    int getDamage() { return damage; }
    int getCooldownTicks() { return cooldownTicks; }
    int getRange() { return range; }
}
