package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.main.Game;
import com.traduvertgames.quest.ContractManager;
import com.traduvertgames.state.PilotUpgrades;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.RpgWorldManager;

/** Regressões do quadro de contratos e dos modificadores de atividade. */
public class ContractManagerTest {

    @BeforeEach
    void setUp() throws Exception {
        GameTestFixture.cleanSaveFiles();
        Game.resetAllForTest();
        new Game();
        Game.setRegionalAdventureMode(true);
        Game.getInstance().loadRegionalAdventure(1);
        RpgWorldManager.updatePlayerPosition(Game.player.getX(), Game.player.getY());
        ContractManager.reset();
    }

    @AfterEach
    void tearDown() {
        ContractManager.reset();
        DynamicEventManager.reset();
        Game.resetAllForTest();
        GameTestFixture.cleanSaveFiles();
    }

    @Test
    void regionalBoardOffersThreeContractsWithDistinctActivities() {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        ContractManager.open(region);
        List<ContractManager.Contract> contracts = ContractManager.getOffered();

        assertEquals(3, contracts.size());
        assertNotNull(region);
        assertTrue(contracts.stream().anyMatch(c -> c.getKind() == ContractManager.Kind.EVENT));
        assertTrue(contracts.stream().anyMatch(c -> c.getKind() == ContractManager.Kind.DUNGEON
                || c.getKind() == ContractManager.Kind.NPC));
        assertTrue(contracts.stream().allMatch(c -> c.getRewardBonus() > 0));
    }

    @Test
    void acceptingEventContractStartsTheSelectedRegionalActivity() {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        ContractManager.open(region);
        List<ContractManager.Contract> contracts = ContractManager.getOffered();
        int eventIndex = -1;
        for (int i = 0; i < contracts.size(); i++) {
            if (contracts.get(i).getKind() == ContractManager.Kind.EVENT) {
                eventIndex = i;
                break;
            }
        }

        assertTrue(eventIndex >= 0);
        assertTrue(ContractManager.accept(eventIndex));
        assertNotNull(ContractManager.getActiveContractId());
        assertTrue(DynamicEventManager.isActive());
    }

    @Test
    void completingActiveEventDeliversContractBonusAndMarksItDone() {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        ContractManager.open(region);
        List<ContractManager.Contract> contracts = ContractManager.getOffered();
        int eventIndex = 0;
        while (eventIndex < contracts.size() && contracts.get(eventIndex).getKind() != ContractManager.Kind.EVENT) {
            eventIndex++;
        }
        assertTrue(eventIndex < contracts.size());
        ContractManager.Contract accepted = contracts.get(eventIndex);
        int creditsBefore = PilotUpgrades.getCredits();

        assertTrue(ContractManager.accept(eventIndex));
        DynamicEventManager.Type activeType = DynamicEventManager.getActiveType();
        ContractManager.onEventCompleted(region, activeType);

        assertEquals(creditsBefore + accepted.getRewardBonus(), PilotUpgrades.getCredits());
        assertTrue(ContractManager.serializeCompleted().containsKey(accepted.getId()));
        assertTrue(ContractManager.serializeCompleted().get(accepted.getId()));
        assertEquals(null, ContractManager.getActiveContractId());
    }

    @Test
    void completedContractsRoundTripThroughSerialization() {
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        ContractManager.open(region);
        ContractManager.Contract contract = ContractManager.getOffered().get(0);
        Map<String, Boolean> snapshot = Map.of(contract.getId(), true);

        ContractManager.deserializeCompleted(snapshot);
        assertTrue(ContractManager.serializeCompleted().get(contract.getId()));
        ContractManager.reset();
        assertFalse(ContractManager.serializeCompleted().containsKey(contract.getId()));
        ContractManager.deserializeCompleted(snapshot);
        ContractManager.open(region);

        assertTrue(ContractManager.isCompleted(ContractManager.getOffered().stream()
                .filter(c -> c.getId().equals(contract.getId())).findFirst().orElse(contract)));
    }
}
