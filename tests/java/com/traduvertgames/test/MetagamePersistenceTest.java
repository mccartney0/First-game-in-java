package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.state.PilotUpgrades;
import com.traduvertgames.state.PilotUpgrades.Upgrade;

/**
 * Rodada 29 — testes de regressão do metagame.
 *
 * Valida o ciclo completo dos créditos persistentes e das melhorias
 * permanentes do piloto: acúmulo, compra com escalonamento de custo,
 * aplicação de stats, serialização/desserialização e limites de nível.
 */
public class MetagamePersistenceTest {

	@org.junit.jupiter.api.BeforeEach
	void resetMetagameState() {
		// Estado estático compartilhado entre testes da suíte: garantir que
		// nenhum resíduo de teste anterior interfira nos créditos e níveis.
		PilotUpgrades.resetCredits();
	}

	// ---------- Acúmulo de créditos ----------
	@Test
	void creditsAccumulateFromKills() {
		assertEquals(0, PilotUpgrades.getCredits());
		PilotUpgrades.addCredits(1);
		PilotUpgrades.addCredits(1);
		PilotUpgrades.addCredits(1);
		assertEquals(3, PilotUpgrades.getCredits());
	}

	@Test
	void negativeCreditGrantsAreIgnored() {
		PilotUpgrades.addCredits(10);
		PilotUpgrades.addCredits(-5);
		PilotUpgrades.addCredits(0);
		assertEquals(10, PilotUpgrades.getCredits());
	}

	// ---------- Custos e escalonamento ----------
	@Test
	void upgradeCostsEscalatePerLevel() throws Exception {
		// Os upgrades alteram stats do Player: o ambiente mínimo (spritesheet,
		// entidades) precisa estar inicializado antes das compras.
		GameTestFixture.initHeadless();
		assertEquals(100, PilotUpgrades.getNextCost(Upgrade.CELLS));
		PilotUpgrades.addCredits(100);
		assertTrue(PilotUpgrades.buy(Upgrade.CELLS));
		assertEquals(150, PilotUpgrades.getNextCost(Upgrade.CELLS));
		PilotUpgrades.addCredits(1000);
		assertTrue(PilotUpgrades.buy(Upgrade.CELLS));
		assertEquals(200, PilotUpgrades.getNextCost(Upgrade.CELLS));
		// O bônus de células deve crescer com o nível mesmo antes do player existir.
		assertEquals(2 * PilotUpgrades.cellsBonusPerLevel(), PilotUpgrades.cellsBonus());
	}

	@Test
	void maxLevelReturnsNegativeCost() {
		PilotUpgrades.addCredits(50_000);
		for (int i = 0; i < PilotUpgrades.getMaxLevel(Upgrade.CELLS); i++) {
			System.out.println("DEBUG credits=" + PilotUpgrades.getCredits() + " cost=" + PilotUpgrades.getNextCost(Upgrade.CELLS)); assertTrue(PilotUpgrades.buy(Upgrade.CELLS));
		}
		assertEquals(-1, PilotUpgrades.getNextCost(Upgrade.CELLS));
		assertEquals(PilotUpgrades.getMaxLevel(Upgrade.CELLS), PilotUpgrades.getLevel(Upgrade.CELLS));
		assertFalse(PilotUpgrades.canAfford(Upgrade.CELLS));
	}

	@Test
	void purchaseRequiresSufficientCredits() {
		PilotUpgrades.addCredits(50);
		assertFalse(PilotUpgrades.canAfford(Upgrade.CELLS));
		assertFalse(PilotUpgrades.buy(Upgrade.CELLS));
		assertEquals(0, PilotUpgrades.getLevel(Upgrade.CELLS));
		assertEquals(50, PilotUpgrades.getCredits());
	}

	// ---------- Persistência ----------
	@Test
	void serializeDeserializeRoundTrip() throws Exception {
		GameTestFixture.initHeadless();
		PilotUpgrades.addCredits(345);
		System.out.println("DEBUG credits=" + PilotUpgrades.getCredits() + " cost=" + PilotUpgrades.getNextCost(Upgrade.CELLS)); assertTrue(PilotUpgrades.buy(Upgrade.CELLS));
		PilotUpgrades.addCredits(200);
		assertTrue(PilotUpgrades.buy(Upgrade.REGEN));

		Map<String, Object> serialized = PilotUpgrades.serialize();
		PilotUpgrades.resetCredits();

		PilotUpgrades.deserialize(serialized);
		assertEquals(345 + 200 - 100 - 200, PilotUpgrades.getCredits());
		assertEquals(1, PilotUpgrades.getLevel(Upgrade.CELLS));
		assertEquals(1, PilotUpgrades.getLevel(Upgrade.REGEN));
		assertEquals(0, PilotUpgrades.getLevel(Upgrade.SHIELD));
		assertEquals(0, PilotUpgrades.getLevel(Upgrade.AMMO));
	}

	@Test
	void deserializeHandlesNullAndUnknownDataGracefully() {
		PilotUpgrades.addCredits(10);
		PilotUpgrades.deserialize(null);
		assertEquals(10, PilotUpgrades.getCredits());
		PilotUpgrades.deserialize("not-a-map");
		assertEquals(10, PilotUpgrades.getCredits());
	}

	// ---------- Aplicação de stats ----------
	@Test
	void cellsBonusGrowsWithLevel() throws Exception {
		GameTestFixture.initHeadless();
		PilotUpgrades.addCredits(100_000);
		for (int i = 0; i < PilotUpgrades.getMaxLevel(Upgrade.CELLS); i++) {
			System.out.println("DEBUG credits=" + PilotUpgrades.getCredits() + " cost=" + PilotUpgrades.getNextCost(Upgrade.CELLS)); assertTrue(PilotUpgrades.buy(Upgrade.CELLS));
		}
		int expectedBonus = 8 * PilotUpgrades.cellsBonusPerLevel();
		assertEquals(expectedBonus, PilotUpgrades.cellsBonus());
	}

	@Test
	void regenTickRespectsMaxLife() throws Exception {
		GameTestFixture.initHeadless();
		PilotUpgrades.addCredits(200);
		assertTrue(PilotUpgrades.buy(Upgrade.REGEN));
		com.traduvertgames.entities.Player.life = 1;
		com.traduvertgames.entities.Player.maxLife = 1;
		PilotUpgrades.regenTick();
		assertEquals(1, com.traduvertgames.entities.Player.life);
	}

	@Test
	void regenTickDoesNotResurrectDeadPilot() throws Exception {
		GameTestFixture.initHeadless();
		PilotUpgrades.addCredits(1000);
		assertTrue(PilotUpgrades.buy(Upgrade.REGEN));
		com.traduvertgames.entities.Player.maxLife = 120;
		com.traduvertgames.entities.Player.life = 0;
		PilotUpgrades.regenTick();
		assertEquals(0, com.traduvertgames.entities.Player.life);
	}

	@Test
	void summaryDescribesPurchasedUpgrades() throws Exception {
		GameTestFixture.initHeadless();
		PilotUpgrades.addCredits(100_000);
		System.out.println("DEBUG credits=" + PilotUpgrades.getCredits() + " cost=" + PilotUpgrades.getNextCost(Upgrade.CELLS)); assertTrue(PilotUpgrades.buy(Upgrade.CELLS));
		String summary = PilotUpgrades.summary();
		assertNotNull(summary);
		assertTrue(summary.contains("cells"), summary);
	}
}
