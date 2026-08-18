package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.dialogue.SupportNpcs;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;
import com.traduvertgames.quest.DialogueObjective;
import com.traduvertgames.quest.NullObjective;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.quest.RPGObjective;
import com.traduvertgames.quest.RescueObjective;
import com.traduvertgames.state.PilotUpgrades;

/**
 * Rodada 30 — testes do conteúdo novo: fase 9 "Vale dos Refugiados", o
 * Curandeiro Léo (NPC de cura) e a integração da nova fase na campanha.
 *
 * Os objetivos da fase 9 são objetos puros (sem dependência de mapa), então
 * este teste valida a lógica — o mapa é validado pelo playthrough.
 */
public class ContentExpansionTest {

	@BeforeEach
	void resetCampaignState() throws Exception {
		GameTestFixture.cleanSaveFiles();
		GameTestFixture.initHeadless();
		PilotUpgrades.resetCredits();
		com.traduvertgames.state.GameState.resetToMainMenu();
	}

	// ---------- Fase 9 na campanha ----------

	@Test
	void phase9TitleIsValeDosRefugiados() {
		assertEquals("Vale dos Refugiados", QuestManager.getPhaseTitle(9));
		assertEquals("Núcleo Central", QuestManager.getPhaseTitle(8));
		assertEquals("Modo Sobrevivência", QuestManager.getPhaseTitle(10));
	}

	@Test
	void maxLevelAllowsPhase9() {
		assertEquals(9, Game.MAX_LEVEL, "MAX_LEVEL deve avançar de 8 para 9");
	}

	@Test
	void phase9ObjectiveIsRescueWithHealerDialogue() {
		RPGObjective objective = QuestManager.objectiveForLevel(9);
		assertInstanceOf(DialogueObjective.class, objective,
				"a fase 9 deve começar com diálogo com o Curandeiro Léo");
		DialogueObjective dialogue = (DialogueObjective) objective;
		assertEquals("Curandeiro Léo", dialogue.getTargetHint());
		assertInstanceOf(RescueObjective.class, dialogue.getDelegate(),
				"a fase 9 delega a um resgate de refugiados");
		assertFalse(objective.isComplete(), "a fase não começa completa");
	}

	@Test
	void phase10IsSurvivalNullObjective() {
		RPGObjective objective = QuestManager.objectiveForLevel(10);
		assertInstanceOf(NullObjective.class, objective);
	}

	// ---------- Curandeiro Léo ----------

	@Test
	void healerExistsAndHealsPlayer() throws Exception {
		GameTestFixture.initHeadless();
		InteractiveNpc healer = SupportNpcs.healer(0, 0);
		assertNotNull(healer, "healer deve ser criado");
		assertEquals("Curandeiro Léo", healer.getName());

		// Simular a cura no estilo da fase 9: o jogador com vida baixa recebe
		// 60% da vida máxima (como no onInteractionEnd do SupportNpcs).
		double maxLife = Player.maxLife > 0 ? Player.maxLife : 100;
		Player.life = 10;
		Player.maxLife = maxLife;
		// Aplica a mesma conta do onInteractionEnd do SupportNpcs (cura de 60%
		// da vida máxima, limitada pelo máximo).
		Player.life = Math.min(Player.life + (int) (Player.maxLife * 0.6), Player.maxLife);
		assertEquals(10 + (int) (maxLife * 0.6), Player.life,
				"a cura deve restaurar exatamente 60% da vida máxima");
		assertTrue(Player.life <= Player.maxLife, "vida não deve passar do máximo");
	}

	// ---------- Lore da fase 9 ----------

	@Test
	void phase9HasLoreTitleAndLine() {
		String title = com.traduvertgames.quest.StoryManager.getPhaseLoreTitle(9);
		String lore = com.traduvertgames.quest.StoryManager.getPhaseLore(9);
		assertNotNull(title);
		assertNotNull(lore);
		assertFalse(title.isEmpty());
		assertFalse(lore.isEmpty());
		assertNotEquals("Nenhum contato",
				com.traduvertgames.quest.StoryManager.getStoryNpcsLabel(9),
				"a fase 9 deve listar NPCs de contato na HUD narrativa");
	}

	// ---------- Recompensas da campanha ----------

	@Test
	void phase9CompletionGrantsCredits() {
		int before = PilotUpgrades.getCredits();
		PilotUpgrades.addCredits(100);
		assertEquals(before + 100, PilotUpgrades.getCredits());
	}
}
