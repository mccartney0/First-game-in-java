package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Companion;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.main.WeaponBuildManager;

/** Regressões de builds de armas e habilidades ativas dos companions. */
public class System4BuildTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        GameTestFixture.initHeadless();
        Game.gameState = "NORMAL";
        WeaponBuildManager.reset();
        Companion.clear();
    }

    @AfterEach
    void tearDown() {
        Companion.clear();
        WeaponBuildManager.reset();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void weaponBuildLevelsAndPathModifiersAreApplied() {
        WeaponBuildManager.addXp(WeaponType.BLASTER, 45);
        WeaponBuildManager.setPath(WeaponType.BLASTER, WeaponBuildManager.BuildPath.POWER);

        assertTrue(WeaponBuildManager.getLevel(WeaponType.BLASTER) >= 3);
        assertTrue(WeaponBuildManager.getDamage(WeaponType.BLASTER) > WeaponType.BLASTER.getDamage());

        WeaponBuildManager.setPath(WeaponType.BLASTER, WeaponBuildManager.BuildPath.MULTI);
        assertTrue(WeaponBuildManager.getProjectilesPerShot(WeaponType.BLASTER) >= 2);
    }

    @Test
    void companionAbilityUsesCooldownAndRestoresFairyResources() {
        Player.life = 10;
        Player.mana = 10;
        Companion.spawn(Companion.CompanionType.FAIRY, -1);
        Companion companion = Companion.getActive();
        assertTrue(companion.activateAbility());
        assertTrue(Player.life > 10);
        assertTrue(Player.mana > 10);
        assertFalse(companion.activateAbility());
        assertTrue(companion.getAbilityCooldownFrames() > 0);
    }

    @Test
    void buildAndCompanionAbilityLevelSurviveRegionalSave() throws Exception {
        WeaponBuildManager.addXp(WeaponType.BLASTER, 30);
        WeaponBuildManager.setPath(WeaponType.BLASTER, WeaponBuildManager.BuildPath.RAPID);
        Companion.spawn(Companion.CompanionType.SCOUT, -1);
        Companion.getActive().setAbilityLevel(3);
        SaveManager.activeSlot = 1;
        assertTrue(SaveManager.saveCurrentGame());

        WeaponBuildManager.reset();
        Companion.clear();
        assertTrue(SaveManager.loadSlot(1));

        assertEquals(WeaponBuildManager.BuildPath.RAPID, WeaponBuildManager.getPath(WeaponType.BLASTER));
        assertTrue(WeaponBuildManager.getLevel(WeaponType.BLASTER) > 0);
        assertEquals(3, Companion.getActive().getAbilityLevel());
    }
}
