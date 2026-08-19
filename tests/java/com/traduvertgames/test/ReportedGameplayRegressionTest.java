package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.EscortNpc;
import com.traduvertgames.entities.Player;
import com.traduvertgames.graficos.PilotUpgradesScreen;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.InventoryManager;
import com.traduvertgames.main.LevelSelectScreen;
import com.traduvertgames.main.Menu;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.state.PilotUpgrades;
import com.traduvertgames.state.PilotUpgrades.Upgrade;
import com.traduvertgames.world.World;

/** Regressões dos bugs reportados na escolta, overlays e slots de save. */
public class ReportedGameplayRegressionTest {

	private Game game;

	@BeforeEach
	void setUp() throws Exception {
		GameTestFixture.cleanSaveFiles();
		PilotUpgrades.resetCredits();
		PilotUpgradesScreen.close();
		InventoryManager.reset();
		LevelSelectScreen.close();
		game = GameTestFixture.initHeadless();
		Game.gameState = "NORMAL";
		Menu.pause = false;
	}

	@Test
	void escortAccumulatesSubpixelMovement() {
		Game.entities.removeIf(entity -> entity instanceof Enemy);
		QuestManager.prepareForLevel(8);
		EscortNpc escort = new EscortNpc(32, 32, 200, 160);
		Game.entities.add(escort);
		int startX = escort.getX();
		int startY = escort.getY();

		for (int i = 0; i < 10; i++) {
			escort.update();
		}

		assertNotEquals(startX, escort.getX(), "o avanço menor que 1 px deve acumular");
		assertNotEquals(startY, escort.getY(), "o avanço menor que 1 px deve acumular");
	}

	@Test
	void levelSelectAndInventoryAreMutuallyExclusive() {
		InventoryManager.toggle();
		assertTrue(InventoryManager.isOpen());

		LevelSelectScreen.open();
		assertFalse(InventoryManager.isOpen(), "abrir fases deve fechar o inventário");
		assertTrue(LevelSelectScreen.isOpen());

		press(KeyEvent.VK_I);
		assertFalse(InventoryManager.isOpen(), "I não pode abrir painel atrás da seleção de fases");
		assertTrue(LevelSelectScreen.isOpen());

		press(KeyEvent.VK_ESCAPE);
		assertFalse(LevelSelectScreen.isOpen());
		assertEquals("NORMAL", Game.gameState);
	}

	@Test
	void escapeRecoversInventoryEvenFromAnInvalidStackedState() {
		InventoryManager.toggle();
		assertTrue(InventoryManager.isOpen());
		Game.gameState = "MENU";
		Menu.pause = true;

		press(KeyEvent.VK_ESCAPE);

		assertFalse(InventoryManager.isOpen());
		assertTrue(Menu.pause, "o primeiro ESC deve fechar apenas o painel da frente");
	}

	@Test
	void savingToANewSlotKeepsBothSlotsAndChangesActiveSlot() {
		SaveManager.activeSlot = 1;
		Player.life = 81;
		assertTrue(SaveManager.saveCurrentGame());

		Player.life = 64;
		assertTrue(SaveManager.saveCurrentGameToSlot(2));

		assertTrue(SaveManager.hasSlotSave(1));
		assertTrue(SaveManager.hasSlotSave(2));
		assertEquals(2, SaveManager.activeSlot);
		assertTrue(SaveManager.loadSlot(1));
		assertEquals(81.0, Player.life, 0.01);
		assertTrue(SaveManager.loadSlot(2));
		assertEquals(64.0, Player.life, 0.01);
		SaveManager.activeSlot = 1;
		SaveManager.refreshActiveSlot();
		assertEquals(2, SaveManager.activeSlot, "uma nova sessão deve retomar o último slot usado");
	}

	@Test
	void clearingActiveSlotRemovesItsMissionAndFallsBackToRemainingSave() {
		SaveManager.activeSlot = 1;
		assertTrue(SaveManager.saveCurrentGame());
		assertTrue(SaveManager.saveCurrentGameToSlot(2));

		assertTrue(SaveManager.clearSlot(2));

		assertFalse(SaveManager.hasSlotSave(2));
		assertEquals("", SaveManager.getSlotObjectiveText(2));
		assertTrue(SaveManager.hasSlotSave(1));
		assertEquals(1, SaveManager.activeSlot);
		assertFalse(SaveManager.clearSlot(0));
		assertFalse(SaveManager.clearSlot(4));
	}

	@Test
	void pauseMenuExposesResetSaveAsAndMainMenuActions() throws Exception {
		Field optionsField = Menu.class.getDeclaredField("PAUSE_OPTIONS_LIST");
		optionsField.setAccessible(true);
		String[] options = (String[]) optionsField.get(null);

		assertTrue(java.util.Arrays.asList(options).contains("salvar em novo slot"));
		assertTrue(java.util.Arrays.asList(options).contains("reiniciar missão atual"));
		assertTrue(java.util.Arrays.asList(options).contains("voltar ao menu principal"));
	}

	@Test
	void spacePurchaseUpdatesLevelImmediatelyAndPersistsMetagame() {
		PilotUpgrades.addCredits(200);
		assertTrue(SaveManager.saveMetagame());
		Game.gameState = "MENU";
		PilotUpgradesScreen.open();
		PilotUpgradesScreen.down(); // CELLS -> REGEN

		press(KeyEvent.VK_SPACE);

		assertTrue(PilotUpgradesScreen.isOpen(), "a tela deve permanecer aberta após comprar");
		assertEquals(1, PilotUpgrades.getLevel(Upgrade.REGEN));
		assertEquals(0, PilotUpgrades.getCredits());

		PilotUpgrades.resetCredits();
		SaveManager.refreshMetagame();
		assertEquals(1, PilotUpgrades.getLevel(Upgrade.REGEN), "a compra deve sobreviver ao reload");
		assertEquals(0, PilotUpgrades.getCredits());

		press(KeyEvent.VK_ESCAPE);
		assertFalse(PilotUpgradesScreen.isOpen());
	}

	@Test
	void corruptedPrimarySaveRecoversFromBackup() throws Exception {
		SaveManager.activeSlot = 1;
		Player.life = 90;
		assertTrue(SaveManager.saveCurrentGame());
		Player.life = 80;
		assertTrue(SaveManager.saveCurrentGame());
		assertTrue(SaveManager.SAVE_BACKUP.exists());

		Files.write(SaveManager.SAVE_FILE.toPath(), "{save truncado".getBytes(StandardCharsets.UTF_8));

		assertTrue(SaveManager.hasSlotSave(1));
		String recovered = new String(Files.readAllBytes(SaveManager.SAVE_FILE.toPath()),
				StandardCharsets.UTF_8);
		assertTrue(recovered.contains("\"slots\""));
	}

	@Test
	void restartingMissionReturnsLevelEightToItsFirstStageAndFreshSaveState() {
		game.setCurrentLevel(8);
		World.restartGame("level8.png");
		QuestManager.deserializeObjectiveState("IDX=1|S0=BRIEFING=true;BOSS=true|S1=ALIVE");
		assertNotNull(findEscort());

		game.restartCurrentMission();

		assertEquals(null, findEscort());
		assertTrue(QuestManager.serializeObjectiveState().startsWith("IDX=0"));
		assertEquals("Fale com a Comandante Ava", QuestManager.getObjectiveProgress());
		assertEquals("NORMAL", Game.gameState);
		assertTrue(SaveManager.hasSlotSave(SaveManager.activeSlot));
	}

	private EscortNpc findEscort() {
		for (Entity entity : Game.entities) {
			if (entity instanceof EscortNpc) {
				return (EscortNpc) entity;
			}
		}
		return null;
	}

	private void press(int keyCode) {
		game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED,
				System.currentTimeMillis(), 0, keyCode, (char) keyCode));
	}
}
