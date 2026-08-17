package com.traduvertgames.quest;

import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.main.SoundManager;

/**
 * Objetivo que combina a lógica de uma missão base (delegada) com a
 * necessidade de conversar com um NPC específico antes de poder concluí-la.
 *
 * Usado para dar um componente narrativo (personagem/alvo de diálogo) a
 * fases que já tinham objetivos de combate, coleta ou canalização.
 */
public class DialogueObjective implements RPGObjective {

	private final RPGObjective delegate;
	private final String dialogueTarget;
	private boolean talkedToTarget = false;

	public DialogueObjective(RPGObjective delegate, String dialogueTarget) {
		this.delegate = delegate;
		this.dialogueTarget = dialogueTarget;
	}

	/** Objetivo interno da fase (depois do diálogo). Usado pela HUD para unwrap. */
	public RPGObjective getDelegate() {
		return delegate;
	}

	@Override
	public void onDialogueStarted(InteractiveNpc npc) {
		delegate.onDialogueStarted(npc);
	}

	@Override
	public void onDialogueFinished(InteractiveNpc npc) {
		delegate.onDialogueFinished(npc);
		if (npc != null && dialogueTarget.equals(npc.getName())) {
			talkedToTarget = true;
			// Concluir a etapa de diálogo que completa a missão: som de recompensa.
			if (isComplete()) {
				SoundManager.play(SoundManager.Event.LEVELUP);
			}
		}
	}

	@Override
	public void onLevelStart() {
		delegate.onLevelStart();
	}

	@Override
	public void onLevelLoaded() {
		delegate.onLevelLoaded();
		// Dica de abertura: o jogador frequentemente mata todos os inimigos da
		// fase e não sabe que precisa conversar com o NPC-alvo antes de a missão
		// progredir (bug reportado: "peguei tudo, matei tudo e não avança").
		if (!talkedToTarget) {
			com.traduvertgames.graficos.MissionBanner.show(
				"MISSÃO",
				"Fale com " + dialogueTarget + " para iniciar (tecla R próximo a ele)",
				new java.awt.Color(255, 235, 59),
				java.awt.Color.WHITE,
				300);
		}
	}

	@Override
	public void update() {
		delegate.update();
	}

	@Override
	public void onQuestItemSpawned(com.traduvertgames.entities.QuestItem item) {
		delegate.onQuestItemSpawned(item);
	}

	@Override
	public void onQuestItemCollected(com.traduvertgames.entities.QuestItem item) {
		boolean wasComplete = isComplete();
		delegate.onQuestItemCollected(item);
		if (!wasComplete && isComplete()) {
			// Coleta que completa a missão da fase: som de recompensa.
			SoundManager.play(SoundManager.Event.LEVELUP);
		}
	}

	@Override
	public void onBeaconSpawned(com.traduvertgames.entities.QuestBeacon beacon) {
		delegate.onBeaconSpawned(beacon);
	}

	@Override
	public void onBeaconActivated(com.traduvertgames.entities.QuestBeacon beacon) {
		delegate.onBeaconActivated(beacon);
	}

	@Override
	public void onNpcSpawned(com.traduvertgames.entities.QuestNPC npc) {
		delegate.onNpcSpawned(npc);
	}

	@Override
	public void onBossSpotted() {
		delegate.onBossSpotted();
	}

	@Override
	public void onNpcRescued(com.traduvertgames.entities.QuestNPC npc) {
		delegate.onNpcRescued(npc);
	}

	@Override
	public void onEnemyKilled(com.traduvertgames.entities.Enemy enemy) {
		delegate.onEnemyKilled(enemy);
	}

	@Override
	public String getTitle() {
		return delegate.getTitle();
	}

	@Override
	public String getDescription() {
		return delegate.getDescription();
	}

	@Override
	public String getProgressText() {
		if (!talkedToTarget) {
			return "Fale com " + dialogueTarget;
		}
		return delegate.getProgressText();
	}

	@Override
	public boolean isComplete() {
		return talkedToTarget && delegate.isComplete();
	}

	@Override
	public String getTargetHint() {
		if (!talkedToTarget) {
			return dialogueTarget;
		}
		return delegate.getTargetHint();
	}

	@Override
	public String serializeState() {
		return "TALKED=" + talkedToTarget + ";DELEGATE=" + delegate.serializeState();
	}

	@Override
	public void deserializeState(String state) {
		if (state == null || state.isEmpty()) {
			return;
		}
		int sep = state.indexOf(';');
		String talkedPart = sep >= 0 ? state.substring(0, sep) : state;
		String delegatePart = sep >= 0 ? state.substring(sep + 1) : "";
		if (talkedPart.startsWith("TALKED=")) {
			talkedToTarget = "true".equalsIgnoreCase(talkedPart.substring("TALKED=".length()));
		}
		if (delegatePart.startsWith("DELEGATE=")) {
			delegate.deserializeState(delegatePart.substring("DELEGATE=".length()));
		}
	}
}
