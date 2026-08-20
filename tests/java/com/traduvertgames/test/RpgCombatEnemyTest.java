package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.traduvertgames.rpg.RpgArchetype;
import com.traduvertgames.rpg.RpgCharacterStats;
import com.traduvertgames.rpg.RpgCombatEnemy;
import com.traduvertgames.rpg.RpgMap;
import com.traduvertgames.rpg.RpgPlayerController;

class RpgCombatEnemyTest {
    @Test
    void outlandExtendsTheValleyAndKeepsTheRoadWalkable() {
        RpgMap map = new RpgMap();
        assertEquals(52 * RpgMap.TILE_SIZE, map.getPixelWidth());
        assertTrue(map.isWalkable(map.getOutlandGateX(), map.getOutlandGateY(), 18, 18));
        assertTrue(map.isWalkable(map.getOutlandBossX(), map.getOutlandBossY(), 18, 18));
    }

    @Test
    void enemyAttacksAtRangeAndCanBeDefeated() {
        RpgMap map = new RpgMap();
        RpgPlayerController player = new RpgPlayerController(map);
        player.setPosition(map.getStalkerX() + 20, map.getStalkerY());
        RpgCharacterStats character = RpgCharacterStats.create(RpgArchetype.GUARDIAO);
        RpgCombatEnemy enemy = new RpgCombatEnemy(RpgCombatEnemy.Kind.STALKER, "Teste", map.getStalkerX(),
                map.getStalkerY(), 5, 6, 20, 1, false);
        int beforeLife = character.getLife();
        int damage = enemy.update(player, map, character.getPhysicalDefense());
        character.takeDamage(damage);
        assertTrue(damage > 0);
        assertTrue(character.getLife() < beforeLife);
        assertTrue(enemy.hit(99));
        assertTrue(!enemy.isAlive());
    }
}
