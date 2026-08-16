package com.traduvertgames.quest;

import java.util.HashSet;
import java.util.Set;

import com.traduvertgames.entities.QuestItem;
import com.traduvertgames.main.Game;

/**
 * Objetivo da fase 7 (Subsolo da Colônia): sabotar os geradores que alimentam
 * a sala do chefe. Os geradores são representados por QuestItems no mapa.
 *
 * Diferente de {@link CollectArtifactsObjective}, aqui o objetivo exige também
 * eliminar o chefe do subsolo depois que todos os geradores forem sabotados —
 * a sabotagem desativa as defesas do chefe, permitindo a sua destruição.
 *
 * Progresso persistente: {@link #serializeState()} guarda a contagem de
 * sabotagens e a derrota do chefe; itens ainda presentes no mapa são
 * re-adicionados ao rastreamento pela {@link #onQuestItemSpawned(QuestItem)}
 * ao recarregar a fase, mantendo o total correto.
 */
public final class SabotageObjective extends BaseObjective {
	private final Set<QuestItem> trackedItems = new HashSet<QuestItem>();
	private int sabotaged = 0;
	private boolean bossDefeated = false;

	public SabotageObjective() {
		super("Sabotar os geradores",
				"Os geradores do subsolo alimentam o guardião da colônia. Sabote todos os geradores para desativar suas defesas e então destrua o Guardião do Subsolo.");
	}

	@Override
	public void onLevelStart() {
		trackedItems.clear();
		sabotaged = 0;
		bossDefeated = false;
	}

	@Override
	public void onQuestItemSpawned(QuestItem item) {
		trackedItems.add(item);
	}

	@Override
	public void onQuestItemCollected(QuestItem item) {
		if (trackedItems.remove(item)) {
			sabotaged++;
		}
	}

	@Override
	public void onEnemyKilled(com.traduvertgames.entities.Enemy enemy) {
		// O Guardião do Subsolo é o GUARDIAN que aparece como chefe da fase 7.
		if (!bossDefeated && enemy.isBoss()
				&& enemy.getVariant() == com.traduvertgames.entities.Enemy.Variant.GUARDIAN) {
			bossDefeated = true;
		}
	}

	/** Quantidade de geradores já sabotados. */
	public int getSabotagedCount() {
		return sabotaged;
	}

	/** Quantidade total de geradores da fase. */
	public int getTotalCount() {
		return trackedItems.size() + sabotaged;
	}

	@Override
	public String getProgressText() {
		int total = getTotalCount();
		if (total == 0) {
			return bossDefeated ? "Guardião derrotado" : "Nenhum gerador detectado";
		}
		if (!bossDefeated) {
			return String.format("%d de %d geradores sabotados", sabotaged, total);
		}
		return String.format("%d de %d geradores — Guardião derrotado", sabotaged, total);
	}

	@Override
	public String getTargetHint() {
		if (!Game.isTraitorTalked()) {
			return "Técnico Hélio";
		}
		if (!bossDefeated) {
			return "o Guardião do Subsolo";
		}
		return null;
	}

	@Override
	public boolean isComplete() {
		return trackedItems.isEmpty() && sabotaged > 0 && bossDefeated;
	}

	@Override
	public String serializeState() {
		return "SABOTAGED=" + sabotaged + ";BOSS=" + bossDefeated;
	}

	@Override
	public void deserializeState(String state) {
		if (state == null || state.isEmpty()) {
			return;
		}
		for (String part : state.split(";")) {
			if (part.startsWith("SABOTAGED=")) {
				try {
					sabotaged = Integer.parseInt(part.substring("SABOTAGED=".length()));
				} catch (NumberFormatException ignored) {
				}
			} else if (part.startsWith("BOSS=")) {
				bossDefeated = "true".equalsIgnoreCase(part.substring("BOSS=".length()));
			}
		}
		// Itens ainda presentes no mapa são re-adicionados ao rastreamento
		// pela onQuestItemSpawned ao recarregar a fase.
	}
}
