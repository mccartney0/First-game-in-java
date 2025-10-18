package com.traduvertgames.entities;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Defines the weapon archetypes available to the player. The enum stores the
 * gameplay parameters required to instantiate projectiles and render
 * contextual UI hints.
 */
public enum WeaponType {

        BLASTER("Canhão padrão", "Equilíbrio entre dano e economia.", new Color(76, 175, 80),
                        14, 1.0, 0.4, 4.2, 1, 0.0, 1.0, 3, 250, 60, true),
        ION_RIFLE("Rifle de íons", "Alta cadência com custo moderado de mana.", new Color(3, 169, 244),
                        8, 1.4, 0.6, 5.2, 1, 6.0, 1.1, 3, 230, 70, false),
        SCATTER_CANNON("Canhão dispersor", "Dispara leque de projéteis a curta distância.", new Color(255, 152, 0),
                        26, 3.8, 1.8, 4.0, 5, 32.0, 0.9, 4, 220, 90, false),
        FUSION_LANCE("Lança de fusão", "Projétil perfurante de alto dano.", new Color(233, 30, 99),
                        12, 2.4, 1.1, 6.4, 1, 0.0, 2.4, 4, 260, 80, false);

        private final String displayName;
        private final String description;
        private final Color uiColor;
        private final int fireDelayFrames;
        private final double manaCost;
        private final double durabilityCost;
        private final double projectileSpeed;
        private final int projectilesPerShot;
        private final double spreadDegrees;
        private final double damage;
        private final int projectileSize;
        private final double maxDurability;
        private final double pickupRecharge;
        private final boolean unlockedByDefault;

        WeaponType(String displayName, String description, Color uiColor, int fireDelayFrames, double manaCost,
                        double durabilityCost, double projectileSpeed, int projectilesPerShot, double spreadDegrees,
                        double damage, int projectileSize, double maxDurability, double pickupRecharge,
                        boolean unlockedByDefault) {
                this.displayName = displayName;
                this.description = description;
                this.uiColor = uiColor;
                this.fireDelayFrames = fireDelayFrames;
                this.manaCost = manaCost;
                this.durabilityCost = durabilityCost;
                this.projectileSpeed = projectileSpeed;
                this.projectilesPerShot = projectilesPerShot;
                this.spreadDegrees = spreadDegrees;
                this.damage = damage;
                this.projectileSize = projectileSize;
                this.maxDurability = maxDurability;
                this.pickupRecharge = pickupRecharge;
                this.unlockedByDefault = unlockedByDefault;
        }

        public String getDisplayName() {
                return displayName;
        }

        public String getDescription() {
                return description;
        }

        public Color getUiColor() {
                return uiColor;
        }

        public int getFireDelayFrames() {
                return fireDelayFrames;
        }

        public double getManaCost() {
                return manaCost;
        }

        public double getDurabilityCost() {
                return durabilityCost;
        }

        public double getProjectileSpeed() {
                return projectileSpeed;
        }

        public int getProjectilesPerShot() {
                return projectilesPerShot;
        }

        public double getSpreadDegrees() {
                return spreadDegrees;
        }

        public double getDamage() {
                return damage;
        }

        public int getProjectileSize() {
                return projectileSize;
        }

        public double getPickupRecharge() {
                return pickupRecharge;
        }

        public double getMaxDurability() {
                return maxDurability;
        }

        public boolean isUnlockedByDefault() {
                return unlockedByDefault;
        }

        public static WeaponType random(Random random) {
                WeaponType[] values = values();
                return values[random.nextInt(values.length)];
        }

        public static WeaponType randomPreferLocked(Random random, Iterable<WeaponType> locked) {
                if (locked != null) {
                        WeaponType[] lockedArray = iterableToArray(locked);
                        if (lockedArray.length > 0) {
                                return lockedArray[random.nextInt(lockedArray.length)];
                        }
                }
                return random(random);
        }

        private static WeaponType[] iterableToArray(Iterable<WeaponType> iterable) {
                if (iterable == null) {
                        return new WeaponType[0];
                }
                List<WeaponType> items = new ArrayList<WeaponType>();
                for (WeaponType type : iterable) {
                        items.add(type);
                }
                return items.toArray(new WeaponType[0]);
        }

        public static WeaponType fromOrdinal(int ordinal) {
                WeaponType[] values = values();
                if (ordinal < 0 || ordinal >= values.length) {
                        return BLASTER;
                }
                return values[ordinal];
        }

        public static WeaponType fromSaveKey(String key) {
                if (key == null) {
                        return null;
                }
                String normalized = key.trim().toUpperCase(Locale.ROOT);
                for (WeaponType type : values()) {
                        if (type.name().equals(normalized)) {
                                return type;
                        }
                }
                return null;
        }
}
