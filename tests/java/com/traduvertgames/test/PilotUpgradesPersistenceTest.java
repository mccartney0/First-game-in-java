package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.LevelUpManager;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.state.PilotUpgrades;
import com.traduvertgames.state.PilotUpgrades.Upgrade;

/**
 * Regressões do cálculo de atributos do piloto entre compras, fases e saves.
 */
public class PilotUpgradesPersistenceTest {

    @org.junit.jupiter.api.BeforeEach
    @AfterEach
    void resetState() {
        PilotUpgrades.resetCredits();
        LevelUpManager.resetProgress();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void cellsPurchaseRaisesLifeImmediatelyAndSurvivesPhaseRecalculation() throws Exception {
        Game game = GameTestFixture.initHeadless();
        Player.resetBaseStats();
        Player.maxLife = 120;
        Player.life = 120;

        PilotUpgrades.addCredits(PilotUpgrades.getNextCost(Upgrade.CELLS));
        assertTrue(PilotUpgrades.buy(Upgrade.CELLS));
        assertEquals(145.0, Player.maxLife, 0.001,
                "a compra de células deve aumentar o máximo imediatamente");

        game.setCurrentLevel(2);
        game.applyDifficultyToPlayerStats();
        double expectedPhaseTwo = Math.round((128 + PilotUpgrades.cellsBonus())
                * com.traduvertgames.main.OptionsConfig.getLifeMultiplier());
        assertEquals(expectedPhaseTwo, Player.maxLife, 0.001,
                "o bônus de células não pode desaparecer na transição de fase");

        game.applyDifficultyToPlayerStats();
        assertEquals(expectedPhaseTwo, Player.maxLife, 0.001,
                "recalcular os atributos não pode acumular o mesmo bônus em loop");
    }

    @Test
    void levelUpMaximumBonusesAreRecomputedAndSerialized() throws Exception {
        Game game = GameTestFixture.initHeadless();
        Player.resetBaseStats();
        LevelUpManager.resetProgress();
        LevelUpManager.reset();

        // XP suficiente para abrir a primeira escolha, sem depender do conteúdo
        // aleatório dos demais cards: escolhe o card VIDA_MAXIMA se ele existir.
        for (int i = 0; i < 4 && !LevelUpManager.isShowingLevelUp(); i++) {
            LevelUpManager.grantKillXp();
        }
        int choice = -1;
        LevelUpManager.Upgrade[] choices = LevelUpManager.getPendingChoices();
        for (int i = 0; i < choices.length; i++) {
            if (choices[i] == LevelUpManager.Upgrade.VIDA_MAXIMA) {
                choice = i;
                break;
            }
        }
        if (choice < 0) {
            // O sorteio é uniforme e pode não incluir vida; o teste continua
            // cobrindo o caminho determinístico usando a API de escolha atual.
            choice = 0;
        }
        LevelUpManager.selectAndConfirm(choice);

        Map<String, Object> saved = LevelUpManager.serializeBonuses();
        int lifeBonus = LevelUpManager.getMaxLifeBonus();
        int manaBonus = LevelUpManager.getMaxManaBonus();
        int shieldBonus = LevelUpManager.getMaxShieldBonus();
        LevelUpManager.resetProgress();
        LevelUpManager.deserializeBonuses(saved);
        assertEquals(lifeBonus, LevelUpManager.getMaxLifeBonus());
        assertEquals(manaBonus, LevelUpManager.getMaxManaBonus());
        assertEquals(shieldBonus, LevelUpManager.getMaxShieldBonus());

        game.setCurrentLevel(1);
        game.applyDifficultyToPlayerStats();
        double expected = Math.round((120 + PilotUpgrades.cellsBonus()
                + lifeBonus) * com.traduvertgames.main.OptionsConfig.getLifeMultiplier());
        assertEquals(expected, Player.maxLife, 0.001);
    }

    @Test
    void levelUpBonusesSurviveSaveAndLoad() throws Exception {
        Game game = GameTestFixture.initHeadless();
        Player.resetBaseStats();
        LevelUpManager.resetProgress();
        Map<String, Object> snapshot = new HashMap<String, Object>();
        snapshot.put("maxLifeBonus", 25);
        snapshot.put("maxManaBonus", 100);
        snapshot.put("maxShieldBonus", 30);
        snapshot.put("xp", 12.5);
        snapshot.put("playerLevel", 2);
        LevelUpManager.deserializeBonuses(snapshot);
        game.setCurrentLevel(1);
        game.applyDifficultyToPlayerStats();
        SaveManager.activeSlot = 1;
        assertTrue(SaveManager.saveCurrentGame());

        LevelUpManager.resetProgress();
        assertTrue(SaveManager.loadSlot(1));
        assertEquals(25, LevelUpManager.getMaxLifeBonus());
        assertEquals(100, LevelUpManager.getMaxManaBonus());
        assertEquals(30, LevelUpManager.getMaxShieldBonus());
        assertEquals(145.0, Player.maxLife, 0.001);
    }

    @Test
    void regenerationIsApproximatelyOneLifePerSecondPerLevel() throws Exception {
        GameTestFixture.initHeadless();
        Player.resetBaseStats();
        PilotUpgrades.addCredits(PilotUpgrades.getNextCost(Upgrade.REGEN));
        assertTrue(PilotUpgrades.buy(Upgrade.REGEN));
        Player.maxLife = 200;
        Player.life = 50;

        for (int i = 0; i < 60; i++) {
            PilotUpgrades.regenTick();
        }

        assertEquals(51.0, Player.life, 0.0001,
                "nível 1 deve regenerar aproximadamente 1 vida por segundo");
    }
}
