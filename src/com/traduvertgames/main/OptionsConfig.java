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
    private static boolean soundEnabled = true;
    /** Ganho master dos efeitos em dB (0 = normal; cada passo = 2 dB). */
    private static int soundVolumeDb = 0;
    /** Passos de 2 dB do ganho da trilha sonora (-10..+5 passos; 0 = normal). */
    private static int musicVolumeDb = 0;
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
        // Trilha adaptativa (rodada 22): volume separado da música do menu.
        com.traduvertgames.main.MusicManager.applyMusicPreference();
    }

    /** Ganho da trilha sonora em dB (-20..+10, passos de 2). */
    public static float getMusicVolume() {
        return musicVolumeDb * 2.0f;
    }

    /** Aumenta/diminui o volume da trilha sonora (delta em passos de 2 dB). */
    public static void adjustMusicVolume(int deltaDb) {
        musicVolumeDb = Math.max(-10, Math.min(5, musicVolumeDb + deltaDb));
        com.traduvertgames.main.MusicManager.applyMusicPreference();
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void toggleSound() {
        soundEnabled = !soundEnabled;
    }

    /** Ganho em dB para os efeitos sonoros. */
    public static float getSoundVolume() {
        return soundVolumeDb * 2.0f;
    }

    /** Aumenta/diminui o volume dos efeitos (delta dB, passos de 2). */
    public static void adjustSoundVolume(int deltaDb) {
        soundVolumeDb = Math.max(-20, Math.min(10, soundVolumeDb + deltaDb));
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
