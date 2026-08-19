package com.traduvertgames.rpg;

/** Arquétipos iniciais; eles fornecem uma base, não classes rígidas. */
public enum RpgArchetype {
    GUARDIAO("Guardião", "Resistência e bloqueio", 34, 8, 6, 10, 8, 14, 4),
    ARCANISTA("Arcanista", "Mana e afinidade arcana", 18, 34, 5, 8, 14, 7, 7),
    ERRANTE("Errante", "Mobilidade e golpes críticos", 24, 16, 9, 14, 7, 9, 10);

    private final String displayName;
    private final String description;
    private final int bonusLife;
    private final int bonusMana;
    private final int strength;
    private final int dexterity;
    private final int intelligence;
    private final int vitality;
    private final int criticalChance;

    RpgArchetype(String displayName, String description, int bonusLife, int bonusMana,
                 int strength, int dexterity, int intelligence, int vitality,
                 int criticalChance) {
        this.displayName = displayName;
        this.description = description;
        this.bonusLife = bonusLife;
        this.bonusMana = bonusMana;
        this.strength = strength;
        this.dexterity = dexterity;
        this.intelligence = intelligence;
        this.vitality = vitality;
        this.criticalChance = criticalChance;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getBonusLife() { return bonusLife; }
    public int getBonusMana() { return bonusMana; }
    public int getStrength() { return strength; }
    public int getDexterity() { return dexterity; }
    public int getIntelligence() { return intelligence; }
    public int getVitality() { return vitality; }
    public int getCriticalChance() { return criticalChance; }
}
