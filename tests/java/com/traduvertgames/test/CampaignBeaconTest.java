package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.entities.QuestBeacon;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.dialogue.SupportNpcs;

/**
 * Rodada 27 — regressão das missões de campanha no pipeline JUnit 5.
 *
 * Reproduz os cenários que causaram os bugs das rodadas 25–26:
 *  1. A missão do beacon da fase 2 avança o canal mesmo em mapas que não têm
 *     o tile do beacon (o `onLevelLoaded` da rodada 26 recria o beacon
 *     programático).
 *  2. Conversar com a Engenheira Nia destrava a etapa de defesa do beacon.
 *  3. Autosave + reload preservam a fase e o canal do beacon.
 *  4. Estado salvo corrompido (sem coordenadas do beacon) é recuperado na
 *     carga — fix da rodada 26, que impedia a missão de travar em
 *     "Localize o beacon do setor".
 *
 * Dependências: `res/` no classpath e display X11 (`xvfb-run` em headless).
 * Observação: os mapas de teste (`tools/generate_maps.py`) não contêm o tile
 * do tile `0xFF66BB6A` (Engenheira Nia), então os testes criam a NPC
 * manualmente — o `DialogueObjective` compara pelo nome exato, como no jogo.
 */
public class CampaignBeaconTest {

	private Game game;

	@BeforeEach
	void setUp() throws Exception {
		GameTestFixture.cleanSaveFiles();
		game = GameTestFixture.newIsolatedGame();
		Game.SCALE = 4;
	}

	@AfterEach
	void tearDown() {
		GameTestFixture.cleanSaveFiles();
	}

	/**
	 * Os mapas de QA (`generate_maps.py`) vêm com inimigos genéricos no
	 * raio do beacon — invadem a zona e travam o canal. O cenário da rodada
	 * 26 exige exatamente "zona limpa", então os invasores são removidos.
	 */
	private void clearInvaders() {
		Game.entities.removeIf(o -> o instanceof com.traduvertgames.entities.Enemy);
		Game.enemies.clear();
	}

	private QuestBeacon findBeacon() {
		for (Object o : Game.entities) {
			if (o instanceof QuestBeacon) {
				return (QuestBeacon) o;
			}
		}
		return null;
	}

	/**
	 * Conversa com a Nia. Como os mapas de QA não trazem a NPC, cria-se uma
	 * instância com o nome exato usado pelo `DialogueObjective` da fase 2.
	 */
	private void talkToNia() {
		InteractiveNpc nia = SupportNpcs.engineer(8 * 16, 6 * 16);
		Game.entities.add(nia);
		QuestManager.notifyDialogueFinished(nia);
	}

	private int channelOf() {
		String state = QuestManager.serializeObjectiveState();
		int idx = state.indexOf("CHANNEL=");
		if (idx < 0) {
			return -1;
		}
		String rest = state.substring(idx + "CHANNEL=".length());
		int semi = rest.indexOf(';');
		String value = semi >= 0 ? rest.substring(0, semi) : rest;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			return -1;
		}
	}

	/** Cenário 1: a troca de fase 1 → 2 chega ao nível 2 com beacon criado. */
	@Test
	void phaseTwoLoadsObjective() {
		GameTestFixture.advanceToLevel(2);
		QuestManager.onLevelLoaded();
		assertEquals(2, QuestManager.getCurrentLevel(),
				"após advanceToNextLevel o nível corrente deve ser 2");
		assertNotNull(findBeacon(),
				"o beacon programático da fase 2 deve existir (mesmo com tile de parede)");
	}

	/** Cenário 2: o canal do beacon avança com a zona de defesa limpa. */
	@Test
	void beaconChannelAdvances() {
		GameTestFixture.advanceToLevel(2);
		QuestManager.onLevelLoaded();
		clearInvaders();
		assertNotNull(findBeacon(), "o beacon deve existir");
		for (int i = 0; i < 90; i++) {
			QuestManager.update();
		}
		assertTrue(channelOf() > 0,
				"o canal do beacon deve avançar com a zona limpa, estado: "
						+ QuestManager.serializeObjectiveState());
	}

	/** Cenário 3: conversar com a Nia destrava a etapa de defesa. */
	@Test
	void beaconStageUnlockedAfterDialogue() {
		GameTestFixture.advanceToLevel(2);
		QuestManager.onLevelLoaded();
		String before = QuestManager.getObjectiveProgress();
		assertTrue(before.contains("Fale"),
				"antes da conversa o progresso pede o diálogo: " + before);

		talkToNia();

		String after = QuestManager.getObjectiveProgress();
		assertFalse(after.contains("Fale"),
				"após a conversa o diálogo não pode mais ser o pedido atual: "
						+ after);
		String state = QuestManager.serializeObjectiveState();
		assertTrue(state.contains("TALKED=true"),
				"o estado serializado deve marcar TALKED=true");
	}

	/** Cenário 4: autosave + reload preservam a fase e o canal do beacon. */
	@Test
	void reloadPreservesBeaconProgress() {
		GameTestFixture.advanceToLevel(2);
		QuestManager.onLevelLoaded();
		clearInvaders();
		talkToNia();
		for (int i = 0; i < 90; i++) {
			QuestManager.update();
		}
		int channelBefore = channelOf();
		assertTrue(channelBefore > 0,
				"o canal do beacon deve avançar com a zona limpa");

		assertTrue(SaveManager.saveCurrentGame(), "autosave deve gravar");
		assertTrue(SaveManager.loadSlot(SaveManager.activeSlot),
				"reload do slot ativo deve restaurar a fase 2");
		assertEquals(2, QuestManager.getCurrentLevel());
		assertNotNull(findBeacon(), "o beacon deve existir após o reload");
		assertEquals(channelBefore, channelOf(),
				"o canal do beacon deve ser preservado pelo reload");
	}

	/** Cenário 5: estado salvo sem beacons (corrompido) é recuperado na carga. */
	@Test
	void corruptedSaveRecoversBeacon() throws Exception {
		GameTestFixture.advanceToLevel(2);
		QuestManager.onLevelLoaded();
		assertTrue(SaveManager.saveCurrentGame(), "save deve gravar a fase 2");
		GameTestFixture.corruptBeaconsInSave();

		assertTrue(SaveManager.loadSlot(SaveManager.activeSlot),
				"reload com beacons ausentes deve completar");
		assertEquals(2, QuestManager.getCurrentLevel());
		assertNotNull(findBeacon(),
				"o beacon deve ser recriado mesmo com o save corrompido");
		String progress = QuestManager.getObjectiveProgress();
		assertNotEquals("Localize o beacon do setor", progress,
				"o progresso não pode travar em 'Localize' após recuperação: "
						+ progress);
	}
}
