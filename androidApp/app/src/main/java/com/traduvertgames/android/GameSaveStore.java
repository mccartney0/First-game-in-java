package com.traduvertgames.android;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Armazena o progresso local do RPG sem depender de rede ou permissões extras. */
public final class GameSaveStore {
    private static final String PREFS = "first_game_rpg_save";
    private static final String KEY_SAVE = "progress";
    private static final int VERSION = 1;
    private final SharedPreferences preferences;

    public GameSaveStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean hasSave() {
        return preferences.contains(KEY_SAVE);
    }

    public void save(SaveData data) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", VERSION);
            root.put("playerX", data.playerX);
            root.put("playerY", data.playerY);
            root.put("health", data.health);
            root.put("level", data.level);
            root.put("xp", data.xp);
            root.put("gold", data.gold);
            root.put("defeated", data.defeated);
            root.put("chestOpened", data.chestOpened);
            root.put("questStage", data.questStage);
            root.put("hunterKills", data.hunterKills);
            root.put("relicCollected", data.relicCollected);
            root.put("necromancerDefeated", data.necromancerDefeated);
            root.put("titanDefeated", data.titanDefeated);
            root.put("onboardingStep", data.onboardingStep);
            root.put("weapon", data.weaponName);
            root.put("armor", data.armorName);
            root.put("accessory", data.accessoryName);
            JSONArray items = new JSONArray();
            for (ItemData item : data.items) {
                JSONObject entry = new JSONObject();
                entry.put("name", item.name);
                entry.put("slot", item.slot);
                entry.put("attack", item.attack);
                entry.put("armor", item.armor);
                entry.put("magic", item.magic);
                entry.put("consumable", item.consumable);
                entry.put("quantity", item.quantity);
                items.put(entry);
            }
            root.put("items", items);
            preferences.edit().putString(KEY_SAVE, root.toString()).apply();
        } catch (JSONException ignored) {
            // O progresso atual segue jogável; uma tentativa posterior pode salvar novamente.
        }
    }

    public SaveData load() {
        String raw = preferences.getString(KEY_SAVE, null);
        if (raw == null) return null;
        try {
            JSONObject root = new JSONObject(raw);
            if (root.optInt("version", 0) != VERSION) return null;
            SaveData data = new SaveData();
            data.playerX = (float) root.optDouble("playerX", 216d);
            data.playerY = (float) root.optDouble("playerY", 504d);
            data.health = (float) root.optDouble("health", 100d);
            data.level = Math.max(1, root.optInt("level", 1));
            data.xp = Math.max(0, root.optInt("xp", 0));
            data.gold = Math.max(0, root.optInt("gold", 25));
            data.defeated = Math.max(0, root.optInt("defeated", 0));
            data.chestOpened = root.optBoolean("chestOpened", false);
            data.questStage = Math.max(0, root.optInt("questStage", 0));
            data.hunterKills = Math.max(0, root.optInt("hunterKills", 0));
            data.relicCollected = root.optBoolean("relicCollected", false);
            data.necromancerDefeated = root.optBoolean("necromancerDefeated", false);
            data.titanDefeated = root.optBoolean("titanDefeated", false);
            // Saves anteriores não tinham tutorial; não interrompemos aventuras em andamento com ele.
            data.onboardingStep = root.has("onboardingStep")
                    ? Math.max(1, Math.min(6, root.optInt("onboardingStep", 1))) : 6;
            data.weaponName = root.optString("weapon", "");
            data.armorName = root.optString("armor", "");
            data.accessoryName = root.optString("accessory", "");
            JSONArray items = root.optJSONArray("items");
            if (items != null) {
                for (int index = 0; index < items.length(); index++) {
                    JSONObject entry = items.optJSONObject(index);
                    if (entry == null) continue;
                    ItemData item = new ItemData();
                    item.name = entry.optString("name", "Item perdido");
                    item.slot = entry.optString("slot", "ACESSÓRIO");
                    item.attack = entry.optInt("attack", 0);
                    item.armor = entry.optInt("armor", 0);
                    item.magic = entry.optInt("magic", 0);
                    item.consumable = entry.optBoolean("consumable", false);
                    item.quantity = Math.max(1, entry.optInt("quantity", 1));
                    data.items.add(item);
                }
            }
            return data;
        } catch (JSONException ignored) {
            return null;
        }
    }

    public static final class SaveData {
        float playerX;
        float playerY;
        float health;
        int level;
        int xp;
        int gold;
        int defeated;
        boolean chestOpened;
        int questStage;
        int hunterKills;
        boolean relicCollected;
        boolean necromancerDefeated;
        boolean titanDefeated;
        int onboardingStep = 1;
        String weaponName = "";
        String armorName = "";
        String accessoryName = "";
        final List<ItemData> items = new ArrayList<>();
    }

    public static final class ItemData {
        String name;
        String slot;
        int attack;
        int armor;
        int magic;
        boolean consumable;
        int quantity;
    }
}
