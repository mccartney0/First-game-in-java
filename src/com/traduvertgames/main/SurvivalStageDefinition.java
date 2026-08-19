package com.traduvertgames.main;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.traduvertgames.entities.Enemy;

/** Configuração declarativa de uma fase do modo sobrevivência. */
public final class SurvivalStageDefinition {

    public enum SpecialRule {
        NONE,
        ACID_POOLS,
        ORBITAL_LASERS
    }

    private final int index;
    private final String name;
    private final String description;
    private final Color accentColor;
    private final int baseThreatBudget;
    private final int threatBudgetPerWave;
    private final int maxEnemies;
    private final int spawnIntervalFrames;
    private final int waveIntervalFrames;
    private final double lifeMultiplier;
    private final double damageMultiplier;
    private final SpecialRule specialRule;
    private final Enemy.Variant[] pool;

    public SurvivalStageDefinition(int index, String name, String description, Color accentColor,
            int baseThreatBudget, int threatBudgetPerWave, int maxEnemies, int spawnIntervalFrames,
            int waveIntervalFrames, double lifeMultiplier, double damageMultiplier, SpecialRule specialRule,
            Enemy.Variant... pool) {
        if (index < 1 || name == null || name.isEmpty() || pool == null || pool.length == 0) {
            throw new IllegalArgumentException("Fase de sobrevivência inválida");
        }
        this.index = index;
        this.name = name;
        this.description = description == null ? "" : description;
        this.accentColor = accentColor == null ? Color.WHITE : accentColor;
        this.baseThreatBudget = Math.max(1, baseThreatBudget);
        this.threatBudgetPerWave = Math.max(0, threatBudgetPerWave);
        this.maxEnemies = Math.max(1, maxEnemies);
        this.spawnIntervalFrames = Math.max(30, spawnIntervalFrames);
        this.waveIntervalFrames = Math.max(60, waveIntervalFrames);
        this.lifeMultiplier = Math.max(0.1, lifeMultiplier);
        this.damageMultiplier = Math.max(0.1, damageMultiplier);
        this.specialRule = specialRule == null ? SpecialRule.NONE : specialRule;
        this.pool = Arrays.copyOf(pool, pool.length);
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Color getAccentColor() {
        return accentColor;
    }

    public int getMaxEnemies() {
        return maxEnemies;
    }

    public int getSpawnIntervalFrames() {
        return spawnIntervalFrames;
    }

    public int getWaveIntervalFrames() {
        return waveIntervalFrames;
    }

    public double getLifeMultiplier() {
        return lifeMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public SpecialRule getSpecialRule() {
        return specialRule;
    }

    public List<Enemy.Variant> getPool() {
        return Collections.unmodifiableList(Arrays.asList(Arrays.copyOf(pool, pool.length)));
    }

    public int getThreatBudget(int wave) {
        return baseThreatBudget + Math.max(0, wave - 1) * threatBudgetPerWave;
    }

    public int getThreatCost(Enemy.Variant variant) {
        if (variant == null) {
            return Integer.MAX_VALUE;
        }
        switch (variant) {
        case SWARM:
            return 1;
        case SCOUT:
        case BOMBER:
        case SAPPER:
            return 2;
        case PHANTOM:
        case SHIELDER:
        case SNIPER:
        case TELEPORTER:
            return 4;
        case ARTILLERY:
        case SENTINEL:
        case RAVAGER:
            return 6;
        case WARDEN:
        case GUARDIAN:
        case WARBRINGER:
            return 8;
        case OVERSEER:
        case OVERSEER_PRIME:
            return 12;
        default:
            return 4;
        }
    }

    public boolean hasRule(SpecialRule rule) {
        return specialRule == rule;
    }

    /** Retorna true quando o tile lógico da fase contém o perigo especial. */
    public boolean isHazardAt(int x, int y, int wave) {
        if (specialRule == SpecialRule.ACID_POOLS) {
            return AcidPoolHazard.isPoolAt(x, y, wave);
        }
        if (specialRule == SpecialRule.ORBITAL_LASERS) {
            int tileX = Math.floorDiv(x, 32);
            int tileY = Math.floorDiv(y, 32);
            return Math.floorMod(tileX + Math.max(0, wave - 1), 5) == 0
                    || Math.floorMod(tileY * 2 + wave, 7) == 0;
        }
        return false;
    }

    public static List<SurvivalStageDefinition> defaultStages() {
        return Arrays.asList(
                new SurvivalStageDefinition(1, "PRIMEIRA ONDA",
                        "Aprenda a se mover e construa seu primeiro arsenal.", new Color(255, 87, 34),
                        18, 4, 12, 270, 210, 1.00, 1.00, SpecialRule.NONE,
                        Enemy.Variant.SCOUT, Enemy.Variant.SWARM, Enemy.Variant.BOMBER),
                new SurvivalStageDefinition(2, "FLORESTA INFECTADA",
                        "Bombardeiros e escudos começam a fechar o mapa.", new Color(76, 175, 80),
                        24, 5, 12, 250, 205, 1.05, 1.02, SpecialRule.NONE,
                        Enemy.Variant.SWARM, Enemy.Variant.BOMBER, Enemy.Variant.SHIELDER, Enemy.Variant.SNIPER),
                new SurvivalStageDefinition(3, "CIDADE DOS ENXAMES",
                        "Enxames velozes pressionam a coleta de XP.", new Color(255, 193, 7),
                        30, 6, 14, 230, 195, 1.10, 1.04, SpecialRule.NONE,
                        Enemy.Variant.SWARM, Enemy.Variant.BOMBER, Enemy.Variant.SHIELDER,
                        Enemy.Variant.SNIPER, Enemy.Variant.TELEPORTER),
                new SurvivalStageDefinition(4, "RUÍNAS DE GUERRA",
                        "Snipers, teleportadores e elites dominam as rotas.", new Color(121, 85, 72),
                        38, 7, 16, 210, 185, 1.16, 1.07, SpecialRule.NONE,
                        Enemy.Variant.SHIELDER, Enemy.Variant.SNIPER, Enemy.Variant.TELEPORTER,
                        Enemy.Variant.RAVAGER, Enemy.Variant.PHANTOM),
                new SurvivalStageDefinition(5, "NÚCLEO DO VAZIO",
                        "A arena final mistura todas as ameaças regionais.", new Color(103, 58, 183),
                        46, 8, 18, 190, 175, 1.23, 1.10, SpecialRule.NONE,
                        Enemy.Variant.SHIELDER, Enemy.Variant.SNIPER, Enemy.Variant.TELEPORTER,
                        Enemy.Variant.RAVAGER, Enemy.Variant.PHANTOM, Enemy.Variant.ARTILLERY,
                        Enemy.Variant.WARBRINGER),
                new SurvivalStageDefinition(6, "PÂNTANO ÁCIDO",
                        "Poças corrosivas forçam movimento constante e drenam recursos.", new Color(124, 179, 66),
                        54, 9, 18, 185, 170, 1.30, 1.13, SpecialRule.ACID_POOLS,
                        Enemy.Variant.SWARM, Enemy.Variant.SAPPER, Enemy.Variant.PHANTOM,
                        Enemy.Variant.SHIELDER, Enemy.Variant.BOMBER, Enemy.Variant.SNIPER),
                new SurvivalStageDefinition(7, "FÁBRICA ORBITAL",
                        "Linhas de laser e máquinas de guerra transformam a arena em um labirinto.", new Color(0, 188, 212),
                        62, 10, 20, 170, 160, 1.38, 1.17, SpecialRule.ORBITAL_LASERS,
                        Enemy.Variant.ARTILLERY, Enemy.Variant.SNIPER, Enemy.Variant.TELEPORTER,
                        Enemy.Variant.RAVAGER, Enemy.Variant.WARBRINGER, Enemy.Variant.SHIELDER));
    }
}
