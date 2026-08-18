package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.traduvertgames.quest.ContactObjective;
import com.traduvertgames.quest.SequenceObjective;
import com.traduvertgames.quest.HoldObjective;
import com.traduvertgames.quest.BossHuntObjective;
import com.traduvertgames.quest.DialogueObjective;
import com.traduvertgames.dialogue.CommanderNpc;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.dialogue.SupportNpcs;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.QuestItem;
import com.traduvertgames.quest.QuestManager;

/**
 * Rodada 27 — testes de lógica pura dos objetivos de missão (sem `Game`).
 *
 * Valida a composição `DialogueObjective` → `SequenceObjective` →
 * `HoldObjective` → `BossHuntObjective` usada pela campanha (fases 2, 6 e 7)
 * e a missão de contato da fase 1: cada etapa avança a próxima e o objetivo
 * composto só completa quando todas concluem.
 */
public class QuestObjectiveLogicTest {

	/**
	 * Classes como `CommanderNpc` e `QuestItem` herdam `Entity`, cujo
	 * inicializador carrega `Game.spritesheet` — sem essa instância, a
	 * primeira construção de entidade falha com `NullPointerException`.
	 */
	@BeforeAll
	static void initHeadlessEnvironment() throws Exception {
		GameTestFixture.initHeadless();
	}

	/** Fase 1: conversa com a Comandante + 2 artefatos completam o contato. */
	@Test
	void contactObjectiveRequiresTalkAndArtifacts() {
		ContactObjective contact = new ContactObjective();
		assertFalse(contact.isComplete(), "não deve completar sem fala nem coleta");

		InteractiveNpc ava = new CommanderNpc(0, 0);
		contact.onDialogueFinished(ava);
		assertFalse(contact.isComplete(), "fala sozinha não completa");

		contact.onQuestItemCollected(new QuestItem(0, 0, java.awt.Color.ORANGE));
		assertFalse(contact.isComplete(), "1 artefato + fala não completa");

		contact.onQuestItemCollected(new QuestItem(0, 0, java.awt.Color.ORANGE));
		assertTrue(contact.isComplete(), "2 artefatos + fala devem completar");
	}

	/** Fase 1: o progresso textual guia o jogador antes e depois da fala. */
	@Test
	void contactProgressTextGuidesPlayer() {
		ContactObjective contact = new ContactObjective();
		assertTrue(contact.getProgressText().contains("Fale"),
				"progresso inicial deve pedir a conversa");
		contact.onDialogueFinished(new CommanderNpc(0, 0));
		assertTrue(contact.getProgressText().contains("Artefatos"),
				"progresso pós-fala deve pedir os artefatos");
	}

/** Fase 2: hold (canal do beacon a 100%) + caça ao chefe em sequência. */
	@Test
	void holdThenBossSequence() {
		HoldObjective hold = new HoldObjective();
		BossHuntObjective boss = new BossHuntObjective();
		SequenceObjective sequence = new SequenceObjective(hold, boss);

		assertFalse(sequence.isComplete(), "sequência não inicia completa");
		assertFalse(boss.isComplete(), "boss não inicia completo");
		assertEquals(hold, sequence.getActive(), "a etapa ativa inicial é o hold");

		// O hold completa quando o canal do beacon atinge 100% — simulado
		// injetando o valor via reflexão (o avanço real depende de
		// `update()` varrendo `Game.entities`, fora do escopo deste teste
		// de lógica pura).
		hold.onBeaconSpawned(new com.traduvertgames.entities.QuestBeacon(0, 0,
				java.awt.Color.GREEN));
		setChannelToMax(hold);
		assertTrue(hold.isComplete(), "hold marcado como completo após o canal fechar");
		// A troca para a próxima etapa só ocorre dentro de `update()`, quando
		// `getActive().isComplete()` vira true — mesma semântica do jogo.
		sequence.update();
		assertFalse(sequence.isComplete(), "sequência aguarda o boss após o hold");
		assertEquals(boss, sequence.getActive(),
				"a etapa ativa avança para o boss após o update()");

		boss.onLevelStart();
		boss.registerBossPresence();
		boss.onEnemyKilled(new Enemy(0, 0, 20, 20, null,
				Enemy.Variant.WARBRINGER, true));
		assertTrue(boss.isComplete(), "boss derrotado completa");
		assertTrue(sequence.isComplete(), "sequência completa após ambas as etapas");
	}

/** Fase 2: o DialogueObjective só libera a sequência após a conversa. */
	@Test
	void dialogueObjectiveUnlocksSequence() {
		BossHuntObjective boss = new BossHuntObjective();
		HoldObjective hold = new HoldObjective();
		SequenceObjective inner = new SequenceObjective(hold, boss);
		DialogueObjective quest = new DialogueObjective(inner, "Engenheira Nia");

		assertFalse(quest.isComplete(), "sem conversa a missão não completa");
		assertFalse(quest.hasTalkedToTarget(), "nenhum diálogo ocorreu");

		// Diálogo com NPC errado não marca o alvo (Nia é fabricada pelo SupportNpcs).
		quest.onDialogueFinished(new CommanderNpc(0, 0));
		assertFalse(quest.hasTalkedToTarget(), "diálogo com NPC errado não marca o alvo");

		// Diálogo com a alvo correta completa a etapa de conversa.
		quest.onDialogueFinished(SupportNpcs.engineer(0, 0));
		assertTrue(quest.hasTalkedToTarget(), "diálogo com a Nia marca o alvo");
		assertFalse(quest.isComplete(), "diálogo completo, mas hold/boss pendentes");

		hold.onBeaconSpawned(new com.traduvertgames.entities.QuestBeacon(0, 0,
				java.awt.Color.GREEN));
		setChannelToMax(hold);
		assertFalse(quest.isComplete(), "falta o boss");

		// Mesma semântica do jogo: `update()` avança as etapas e
		// `onEnemyKilled` do boss (via QuestManager) marca a derrota.
		boss.onLevelStart();
		boss.registerBossPresence();
		boss.onEnemyKilled(new Enemy(0, 0, 20, 20, null,
				Enemy.Variant.OVERSEER, true));
		// A troca da etapa ativa acontece dentro de `update()` da sequência
		// (o wrapper não está no QuestManager real neste teste de lógica
		// pura, então o ciclo é fechado manualmente na instância local).
		inner.update();
		assertTrue(quest.isComplete(), "tudo completo: missão encerrada");
	}

	private static void setChannelToMax(HoldObjective hold) {
			try {
				java.lang.reflect.Field channelField =
						HoldObjective.class.getDeclaredField("channel");
				channelField.setAccessible(true);
				java.lang.reflect.Field maxField =
						HoldObjective.class.getDeclaredField("CHANNEL_MAX");
				maxField.setAccessible(true);
				channelField.set(hold, maxField.getInt(null));
			} catch (Exception ex) {
				throw new RuntimeException("não foi possível simular o canal",
						ex);
			}
		}

	/** Serialização preserva o estado do ContactObjective. */
	@Test
	void contactObjectiveSerializeRoundTrip() {
		ContactObjective contact = new ContactObjective();
		contact.onDialogueFinished(new CommanderNpc(0, 0));
		contact.onQuestItemCollected(new QuestItem(0, 0, java.awt.Color.ORANGE));

		String state = contact.serializeState();
		assertTrue(state.contains("TALKED=true"), "estado deve marcar a fala");
		assertTrue(state.contains("ARTIFACTS=1"), "estado deve marcar 1 artefato");
		assertFalse(state.equals("COMPLETE"), "estado parcial não pode ser COMPLETE");

		ContactObjective restored = new ContactObjective();
		restored.deserializeState(state);
		assertEquals(state, restored.serializeState(),
				"round-trip de serialização deve ser idempotente");
	}
}
