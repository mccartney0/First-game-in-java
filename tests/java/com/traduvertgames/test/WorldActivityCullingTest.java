package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.WorldActivityCulling;

class WorldActivityCullingTest {

    @AfterEach
    void reset() {
        Game.resetAllForTest();
        Camera.x = 0;
        Camera.y = 0;
    }

    @Test
    void extendedRpgKeepsNearbyEnemiesActiveAndSleepsDistantEnemies() throws Exception {
        GameTestFixture.newIsolatedGame();
        Game.setRegionalAdventureMode(true);
        Camera.x = 320;
        Camera.y = 160;
        Enemy nearby = new Enemy(420, 220, 16, 16, Entity.ENEMY_EN, Enemy.Variant.SCOUT);
        Enemy distant = new Enemy(4000, 2800, 16, 16, Entity.ENEMY_EN, Enemy.Variant.SCOUT);

        assertTrue(WorldActivityCulling.shouldUpdate(nearby));
        assertTrue(WorldActivityCulling.shouldRender(nearby));
        assertFalse(WorldActivityCulling.shouldUpdate(distant));
        assertFalse(WorldActivityCulling.shouldRender(distant));
    }

    @Test
    void campaignDoesNotApplyWorldDistanceLimit() throws Exception {
        GameTestFixture.newIsolatedGame();
        Game.setRegionalAdventureMode(false);
        Game.setOpenWorldMode(false);
        Enemy distant = new Enemy(4000, 2800, 16, 16, Entity.ENEMY_EN, Enemy.Variant.SCOUT);

        assertTrue(WorldActivityCulling.shouldUpdate(distant));
        assertTrue(WorldActivityCulling.shouldRender(distant));
    }
}
