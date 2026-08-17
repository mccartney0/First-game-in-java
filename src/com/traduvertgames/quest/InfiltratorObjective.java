package com.traduvertgames.quest;

import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.entities.Enemy;

/**
 * Objetivo da fase 8 (Núcleo Central — A Mente da Colônia): conversar com a
 * Comandante Ava para receber o código de desativação do núcleo da IA e então
 * destruir o OVERSEER PRIME, a mente que comanda todas as máquinas da colônia.
 *
 * Etapas:
 * 1. Falar com a Comandante Ava (briefing final com o código de desativação).
 * 2. Destruir o OVERSEER PRIME (chefe da fase, spawnado no mapa).
 */
public final class InfiltratorObjective extends BaseObjective {
	private boolean briefingDone = false;
	private boolean bossDefeated = false;

	public InfiltratorObjective() {
		super("Destruir o Núcleo da IA",
				"A mente da colônia está no núcleo central. Fale com a Comandante Ava para receber o código de desativação e então destrua o Supervisor-Prime.");
	}

	@Override
	public void onLevelStart() {
		briefingDone = false;
		bossDefeated = false;
	}

	@Override
	public void onDialogueFinished(InteractiveNpc npc) {
		if (!briefingDone && npc.getName().contains("Ava")) {
			briefingDone = true;
		}
	}

	@Override
	public void onEnemyKilled(Enemy enemy) {
		// O chefe da fase 8 é o Supervisor-Prime; a verificação também
		// aceita a variante OVERSEER por compatibilidade (rodada 23b).
		if (!bossDefeated && enemy.isBoss()
				&& (enemy.getVariant() == Enemy.Variant.OVERSEER
					|| enemy.getVariant() == Enemy.Variant.OVERSEER_PRIME)) {
			bossDefeated = true;
		}
	}

	@Override
	public String getProgressText() {
		if (!briefingDone) {
			return "Fale com a Comandante Ava";
		}
		if (!bossDefeated) {
			return "Destrua o Supervisor-Prime";
		}
		return "Núcleo desativado";
	}

	@Override
	public String getTargetHint() {
		if (!briefingDone) {
			return "Comandante Ava";
		}
		if (!bossDefeated) {
			return "o Supervisor-Prime";
		}
		return null;
	}

	@Override
	public boolean isComplete() {
		return briefingDone && bossDefeated;
	}

	@Override
	public String serializeState() {
		return "BRIEFING=" + briefingDone + ";BOSS=" + bossDefeated;
	}

	@Override
	public void deserializeState(String state) {
		if (state == null || state.isEmpty()) {
			return;
		}
		for (String part : state.split(";")) {
			if (part.startsWith("BRIEFING=")) {
				briefingDone = "true".equalsIgnoreCase(part.substring("BRIEFING=".length()));
			} else if (part.startsWith("BOSS=")) {
				bossDefeated = "true".equalsIgnoreCase(part.substring("BOSS=".length()));
			}
		}
	}
}
