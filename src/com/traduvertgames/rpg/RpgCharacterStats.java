package com.traduvertgames.rpg;

import java.util.HashMap;
import java.util.Map;

/** Estado de personagem do RPG Clássico; não compartilha arsenal ou créditos sci-fi. */
public final class RpgCharacterStats {
    private RpgArchetype archetype;
    private int level;
    private int experience;
    private int attributePoints;
    private int life;
    private int maxLife;
    private int mana;
    private int maxMana;
    private int stamina;
    private int maxStamina;
    private int strength;
    private int dexterity;
    private int intelligence;
    private int vitality;
    private int physicalDefense;
    private int magicalDefense;
    private int criticalChance;
    private int criticalDamage;
    private int speed;
    private int permanentPhysicalDefense;

    private RpgCharacterStats(RpgArchetype archetype) {
        this.archetype = archetype;
        this.level = 1;
        this.experience = 0;
        this.attributePoints = 0;
        this.strength = archetype.getStrength();
        this.dexterity = archetype.getDexterity();
        this.intelligence = archetype.getIntelligence();
        this.vitality = archetype.getVitality();
        this.criticalChance = archetype.getCriticalChance();
        this.criticalDamage = 150;
        this.speed = 2 + (dexterity / 10);
        recalculateDerivedValues();
        this.life = maxLife;
        this.mana = maxMana;
        this.stamina = maxStamina;
    }

    public static RpgCharacterStats create(RpgArchetype archetype) {
        return new RpgCharacterStats(archetype == null ? RpgArchetype.GUARDIAO : archetype);
    }

    public void updateStamina(int amount) {
        stamina = clamp(stamina + amount, 0, maxStamina);
    }

    public boolean spendStamina(int amount) {
        if (amount < 0 || stamina < amount) return false;
        stamina -= amount;
        return true;
    }

    public void restoreResources() {
        life = maxLife;
        mana = maxMana;
        stamina = maxStamina;
    }

    /** Restauração usada por itens e interações do modo RPG, sem tocar no shooter. */
    public void restore(int lifeAmount, int manaAmount, int staminaAmount) {
        life = clamp(life + Math.max(0, lifeAmount), 0, maxLife);
        mana = clamp(mana + Math.max(0, manaAmount), 0, maxMana);
        stamina = clamp(stamina + Math.max(0, staminaAmount), 0, maxStamina);
    }

    /** Aplica dano de combate do modo RPG após a mitigação calculada pela IA. */
    public void takeDamage(int amount) {
        life = clamp(life - Math.max(0, amount), 0, maxLife);
    }

    /** Recompensa permanente de exploração, persistida sem tocar nos atributos base do arquétipo. */
    public void grantPermanentPhysicalDefense(int amount) {
        permanentPhysicalDefense = Math.max(0, permanentPhysicalDefense + Math.max(0, amount));
        recalculateDerivedValues();
    }

    public void gainExperience(int amount) {
        if (amount <= 0) return;
        experience += amount;
        while (experience >= experienceToNextLevel()) {
            experience -= experienceToNextLevel();
            level++;
            attributePoints += 3;
            recalculateDerivedValues();
            life = maxLife;
            mana = maxMana;
            stamina = maxStamina;
        }
    }

    public int experienceToNextLevel() {
        return 100 + (level - 1) * 60;
    }

    public boolean spendAttributePoint(String attribute) {
        if (attributePoints <= 0 || attribute == null) return false;
        if ("forca".equals(attribute)) strength++;
        else if ("destreza".equals(attribute)) dexterity++;
        else if ("inteligencia".equals(attribute)) intelligence++;
        else if ("vitalidade".equals(attribute)) vitality++;
        else return false;
        attributePoints--;
        recalculateDerivedValues();
        return true;
    }

    private void recalculateDerivedValues() {
        maxLife = 70 + vitality * 5 + archetype.getBonusLife();
        maxMana = 30 + intelligence * 4 + archetype.getBonusMana();
        maxStamina = 80 + dexterity * 2;
        physicalDefense = 2 + vitality / 3 + permanentPhysicalDefense;
        magicalDefense = 2 + intelligence / 4;
        criticalChance = Math.min(50, archetype.getCriticalChance() + dexterity / 4);
        speed = Math.max(2, 2 + dexterity / 12);
        life = clamp(life, 0, maxLife);
        mana = clamp(mana, 0, maxMana);
        stamina = clamp(stamina, 0, maxStamina);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("archetype", archetype.name());
        data.put("level", level);
        data.put("experience", experience);
        data.put("attributePoints", attributePoints);
        data.put("life", life);
        data.put("maxLife", maxLife);
        data.put("mana", mana);
        data.put("maxMana", maxMana);
        data.put("stamina", stamina);
        data.put("maxStamina", maxStamina);
        data.put("strength", strength);
        data.put("dexterity", dexterity);
        data.put("intelligence", intelligence);
        data.put("vitality", vitality);
        data.put("physicalDefense", physicalDefense);
        data.put("magicalDefense", magicalDefense);
        data.put("criticalChance", criticalChance);
        data.put("criticalDamage", criticalDamage);
        data.put("speed", speed);
        data.put("permanentPhysicalDefense", permanentPhysicalDefense);
        return data;
    }

    public static RpgCharacterStats deserialize(Map<String, Object> data) {
        RpgArchetype type = RpgArchetype.GUARDIAO;
        if (data != null && data.get("archetype") != null) {
            try { type = RpgArchetype.valueOf(String.valueOf(data.get("archetype"))); }
            catch (IllegalArgumentException ignored) { }
        }
        RpgCharacterStats stats = create(type);
        if (data == null) return stats;
        stats.level = positiveInt(data.get("level"), 1);
        stats.experience = nonNegativeInt(data.get("experience"), 0);
        stats.attributePoints = nonNegativeInt(data.get("attributePoints"), 0);
        stats.strength = positiveInt(data.get("strength"), type.getStrength());
        stats.dexterity = positiveInt(data.get("dexterity"), type.getDexterity());
        stats.intelligence = positiveInt(data.get("intelligence"), type.getIntelligence());
        stats.vitality = positiveInt(data.get("vitality"), type.getVitality());
        stats.criticalDamage = positiveInt(data.get("criticalDamage"), 150);
        stats.permanentPhysicalDefense = nonNegativeInt(data.get("permanentPhysicalDefense"), 0);
        stats.recalculateDerivedValues();
        stats.life = clamp(nonNegativeInt(data.get("life"), stats.maxLife), 0, stats.maxLife);
        stats.mana = clamp(nonNegativeInt(data.get("mana"), stats.maxMana), 0, stats.maxMana);
        stats.stamina = clamp(nonNegativeInt(data.get("stamina"), stats.maxStamina), 0, stats.maxStamina);
        return stats;
    }

    private static int positiveInt(Object raw, int fallback) {
        int value = nonNegativeInt(raw, fallback);
        return value <= 0 ? fallback : value;
    }

    private static int nonNegativeInt(Object raw, int fallback) {
        if (raw instanceof Number) return Math.max(0, ((Number) raw).intValue());
        try { return Math.max(0, Integer.parseInt(String.valueOf(raw))); }
        catch (Exception ignored) { return fallback; }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public RpgArchetype getArchetype() { return archetype; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getAttributePoints() { return attributePoints; }
    public int getLife() { return life; }
    public int getMaxLife() { return maxLife; }
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }
    public int getStamina() { return stamina; }
    public int getMaxStamina() { return maxStamina; }
    public int getStrength() { return strength; }
    public int getDexterity() { return dexterity; }
    public int getIntelligence() { return intelligence; }
    public int getVitality() { return vitality; }
    public int getPhysicalDefense() { return physicalDefense; }
    public int getMagicalDefense() { return magicalDefense; }
    public int getCriticalChance() { return criticalChance; }
    public int getCriticalDamage() { return criticalDamage; }
    public int getSpeed() { return speed; }
    public int getPermanentPhysicalDefense() { return permanentPhysicalDefense; }
}
