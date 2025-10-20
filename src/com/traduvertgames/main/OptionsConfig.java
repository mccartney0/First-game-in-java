package com.traduvertgames.main;

public final class OptionsConfig {

    public enum Difficulty {
        EASY("Fácil", 1.25, 1.20, 0.8, 1.2),
        NORMAL("Normal", 1.0, 1.0, 1.0, 1.0),
        HARD("Difícil", 0.8, 0.85, 1.3, 0.9);

        private final String displayName;
        private final double lifeMultiplier;
        private final double manaMultiplier;
        private final double damageTakenMultiplier;
        private final double weaponCapacityMultiplier;

        Difficulty(String displayName, double lifeMultiplier, double manaMultiplier,
                double damageTakenMultiplier, double weaponCapacityMultiplier) {
            this.displayName = displayName;
            this.lifeMultiplier = lifeMultiplier;
            this.manaMultiplier = manaMultiplier;
            this.damageTakenMultiplier = damageTakenMultiplier;
            this.weaponCapacityMultiplier = weaponCapacityMultiplier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public double getLifeMultiplier() {
            return lifeMultiplier;
        }

        public double getManaMultiplier() {
            return manaMultiplier;
        }

        public double getDamageTakenMultiplier() {
            return damageTakenMultiplier;
        }

        public double getWeaponCapacityMultiplier() {
            return weaponCapacityMultiplier;
        }

        public Difficulty next() {
            Difficulty[] values = values();
            int nextIndex = (ordinal() + 1) % values.length;
            return values[nextIndex];
        }
    }

    private static boolean musicEnabled = true;
    private static Difficulty difficulty = Difficulty.NORMAL;

    private OptionsConfig() {
    }

    public static boolean isMusicEnabled() {
        return musicEnabled;
    }

    public static void toggleMusic() {
        musicEnabled = !musicEnabled;
        applyMusicPreference();
    }

    public static void applyMusicPreference() {
        if (Sound.music == null) {
            return;
        }
        if (musicEnabled) {
            Sound.music.loop();
        } else {
            Sound.music.stop();
        }
    }

    public static Difficulty getDifficulty() {
        return difficulty;
    }

    public static void cycleDifficulty() {
        difficulty = difficulty.next();
        Game game = Game.getInstance();
        if (game != null) {
            game.applyDifficultyToPlayerStats();
        }
    }

    public static double getDamageTakenMultiplier() {
        return difficulty.getDamageTakenMultiplier();
    }

    public static double getLifeMultiplier() {
        return difficulty.getLifeMultiplier();
    }

    public static double getManaMultiplier() {
        return difficulty.getManaMultiplier();
    }

    public static double getWeaponCapacityMultiplier() {
        return difficulty.getWeaponCapacityMultiplier();
    }
}
