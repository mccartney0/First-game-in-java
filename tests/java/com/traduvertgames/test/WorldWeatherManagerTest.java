package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.WorldWeatherManager;

class WorldWeatherManagerTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Game.getInstance().loadOpenWorld(1);
        Game.gameState = "NORMAL";
    }

    @AfterEach
    void tearDown() {
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void weatherIsDeterministicPerSectorAndModifierStaysBounded() {
        WorldWeatherManager.Weather first = WorldWeatherManager.getWeatherAtTile(10, 10);
        WorldWeatherManager.Weather second = WorldWeatherManager.getWeatherAtTile(10, 10);
        WorldWeatherManager.GiantModifier modifier = WorldWeatherManager.getGiantModifier(10 * 16, 10 * 16);

        assertEquals(first, second);
        assertTrue(modifier.getSpeedMultiplier() >= 0.82 && modifier.getSpeedMultiplier() <= 1.25);
        assertTrue(modifier.getDamageMultiplier() >= 0.86 && modifier.getDamageMultiplier() <= 1.24);
        assertTrue(modifier.getCooldownMultiplier() >= 0.76 && modifier.getCooldownMultiplier() <= 1.16);
    }

    @Test
    void serializedClockRestoresNightAndClimateState() {
        Map<String, Object> snapshot = new HashMap<String, Object>();
        snapshot.put("active", true);
        snapshot.put("seed", 913L);
        snapshot.put("timeTicks", 1500 + 420 + 80);
        WorldWeatherManager.deserialize(snapshot);

        assertEquals(WorldWeatherManager.TimeOfDay.NIGHT, WorldWeatherManager.getTimeOfDay());
        assertEquals(snapshot.get("timeTicks"), WorldWeatherManager.serialize().get("timeTicks"));
    }

    @Test
    void onlyHeavyVariantsAreClimateGiants() {
        Enemy scout = new Enemy(32, 32, 16, 16, Entity.ENEMY_EN, Enemy.Variant.SCOUT);
        Enemy guardian = new Enemy(48, 32, 16, 16, Entity.ENEMY_EN, Enemy.Variant.GUARDIAN);
        Enemy boss = new Enemy(64, 32, 16, 16, Entity.ENEMY_EN, Enemy.Variant.WARBRINGER, true);

        assertFalse(scout.isClimateGiant());
        assertTrue(guardian.isClimateGiant());
        assertTrue(boss.isClimateGiant());
    }
}
