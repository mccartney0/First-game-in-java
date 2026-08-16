package com.traduvertgames.quest;

import com.traduvertgames.dialogue.CommanderNpc;
import com.traduvertgames.dialogue.InteractiveNpc;

/**
 * Objetivo composto da fase 1: conversar com a Comandante Ava e coletar os
 * artefatos do setor. A fase só é concluída quando ambos estiverem prontos.
 *
 * Enquanto o jogador não conversa com Ava, o waypoint e o card de missão
 * direcionam o jogador até ela, criando um objetivo narrativo explícito.
 */
public class ContactObjective implements RPGObjective {

	private static final int REQUIRED_ARTIFACTS = 2;

	private final String title;
	private final String description;
	private final String bossHint;

	private int artifactsCollected = 0;
	private boolean talkedToCommander = false;

	public ContactObjective() {
		this("Contacto com o Comando",
				"Localize a Comandante Ava e colete os artefatos do setor para estabilizar a colônia.",
				"Comandante Ava");
	}

	public ContactObjective(String title, String description, String bossHint) {
		this.title = title;
		this.description = description;
		this.bossHint = bossHint;
	}

	@Override
	public void onQuestItemCollected(com.traduvertgames.entities.QuestItem item) {
		artifactsCollected++;
	}

	@Override
	public void onDialogueFinished(InteractiveNpc npc) {
		if (npc instanceof CommanderNpc && !talkedToCommander) {
			talkedToCommander = true;
		}
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getProgressText() {
		if (!talkedToCommander) {
			return "Fale com a " + bossHint;
		}
		return "Artefatos: " + Math.min(artifactsCollected, REQUIRED_ARTIFACTS) + "/" + REQUIRED_ARTIFACTS;
	}

	@Override
	public boolean isComplete() {
		return talkedToCommander && artifactsCollected >= REQUIRED_ARTIFACTS;
	}

	@Override
	public String getTargetHint() {
		if (!talkedToCommander) {
			return bossHint;
		}
		return null;
	}
}
