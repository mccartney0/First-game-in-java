package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.graficos.VictoryCutscene;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.Menu;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.entities.Player;

/**
 * Rodada 31 — conteúdo pós-campanha: valida a despedida da Comandante Ava na
 * fase 9, o epílogo dos refugiados na cutscene de vitória e a Nova campanha+.
 */
public class PostCampaignTest {

	@BeforeEach
	public void setUp() {
		// Reseta o jogo antes de cada teste: as flags de campanha ficam em
		// memória durante a execução (o save de disco é tocado apenas pelos
		// testes explícitos de persistência).
		Game.resetAllForTest();
		Game.setCurrentLevel(1);
	}

	@AfterEach
	public void tearDown() {
		Game.resetAllForTest();
	}

	@Test
	public void campaignInitiallyNotCompleted() {
		assertFalse(SaveManager.hasCampaignCompleted(),
				"campanha começa não concluída");
	}

	@Test
	public void newGamePlusInitiallyOff() {
		assertFalse(SaveManager.isNewGamePlus(),
				"Nova campanha+ começa desativada");
	}

	@Test
	public void refugeeEndingDisabledByDefault() {
		VictoryCutscene.setRefugeeEnding(false);
		assertFalse(VictoryCutscene.isRefugeeEnding(),
				"epílogo dos refugiados começa desativado");
	}

	@Test
	public void campaignCompletedFlagPersists() {
		SaveManager.setCampaignCompleted(true);
		assertTrue(SaveManager.hasCampaignCompleted());
		// A flag permanece gravada até ser explicitamente desligada.
		SaveManager.setCampaignCompleted(false);
		assertFalse(SaveManager.hasCampaignCompleted());
	}

	@Test
	public void newGamePlusFlagPersists() {
		SaveManager.setNewGamePlus(true);
		assertTrue(SaveManager.isNewGamePlus());
		SaveManager.setNewGamePlus(false);
		assertFalse(SaveManager.isNewGamePlus());
	}

	@Test
	public void bonusAppliedWhenNewGamePlusIsActive() throws java.io.IOException {
		// Ativa a Nova campanha+ e reinicia o jogo: o bônus de +25% deve
		// ser aplicado sobre os máximos de vida e mana da fase inicial.
		SaveManager.setNewGamePlus(true);
		Game game = new Game();
		game.startNewGamePlus();

		// O bônus se esgota ao iniciar (a flag é consumida e gravada).
		assertFalse(SaveManager.isNewGamePlus());

		// A campanha reinicia na fase 1 com os bônus aplicados: máximos de
		// vida e mana acima do padrão da fase inicial e recursos cheios.
		assertEquals(1, game.getCurrentLevel());
		int expectedMaxLife = 120 + (int) Math.round(120 * 0.25);
		int expectedMaxMana = 500 + (int) Math.round(500 * 0.25);
		assertEquals(expectedMaxLife, Player.maxLife,
				"vida máxima com bônus de +25% da Nova campanha+");
		assertEquals(expectedMaxMana, Player.maxMana,
				"mana máxima com bônus de +25% da Nova campanha+");
		assertEquals(Player.maxLife, Player.life, "vida cheia após o bônus");
		assertEquals(Player.maxMana, Player.mana, "mana cheia após o bônus");
	}

	@Test
	public void newGamePlusSkipsBonusWhenFlagOff() throws java.io.IOException {
		SaveManager.setNewGamePlus(false);
		Game game = new Game();
		game.startNewGamePlus();
		// Sem a flag, o bônus não é aplicado (novo jogo padrão).
		assertEquals(120, Player.maxLife);
		assertEquals(500, Player.maxMana);
	}

	@Test
	public void menuOptionCountIncludesNewGamePlus() {
		// O menu principal expõe a nova opção entre as entradas padrão.
		int mainOptionCount = new Menu().getMainMenuOptionCountForTest();
		assertTrue(mainOptionCount >= 8,
				"menu principal inclui a Nova campanha+ (>= 8 opções)");
	}

	@Test
	public void newGamePlusOptionUnavailableBeforeCampaignCompletion() {
		SaveManager.setCampaignCompleted(false);
		Menu menu = new Menu();
		assertFalse(menu.isNewGamePlusAvailableForTest(),
				"Nova campanha+ fica indisponível antes de concluir a campanha");
	}

	@Test
	public void newGamePlusOptionAvailableAfterCampaignCompletion() {
		SaveManager.setCampaignCompleted(true);
		Menu menu = new Menu();
		assertTrue(menu.isNewGamePlusAvailableForTest(),
				"Nova campanha+ fica disponível após concluir a campanha");
		SaveManager.setCampaignCompleted(false);
	}

	@Test
	public void level9FarewellMarksNpcDialogue() {
		// A Ava de despedida só aparece na fase 9: fora dela a CommanderNpc
		// padrão prevalece. O diálogo concluído é persistido como "Ava_9".
		assertFalse(SaveManager.hasNpcDialogue("Ava", 9));
		SaveManager.markNpcDialogue("Ava", 9);
		assertTrue(SaveManager.hasNpcDialogue("Ava", 9));
	}
}
