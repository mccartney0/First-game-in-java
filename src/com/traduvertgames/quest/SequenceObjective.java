package com.traduvertgames.quest;

import java.util.ArrayList;
import java.util.List;

import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.QuestBeacon;
import com.traduvertgames.entities.QuestItem;
import com.traduvertgames.entities.QuestNPC;
import com.traduvertgames.main.SoundManager;

/**
 * Objetivo composto que executa uma sequência de objetivos na ordem em que
 * foram declarados: enquanto o objetivo ativo não for concluído, todos os
 * eventos de missão são delegados apenas a ele; quando ele conclui, o jogo
 * avança para o próximo estágio da sequência (com som de etapa concluída) e
 * a fase só termina quando o último estágio for concluído.
 *
 * Usado para dar às fases um encadeamento de objetivos variados
 * (ex.: defender o beacon e depois caçar o chefe; sobreviver às ondas e
 * então enfrentar o supervisor; escoltar o informante e destruir a mente da
 * colônia), sem alterar o fluxo de transição da campanha.
 *
 * Progresso persistente: {@link #serializeState()} grava o índice da etapa
 * atual e o estado do estágio ativo; ao recarregar o save, os estágios
 * anteriores já concluídos são retomados com {@link #deserializeState(String)}.
 */
public class SequenceObjective implements RPGObjective {

	private final List<RPGObjective> stages = new ArrayList<RPGObjective>();
	private int activeIndex = 0;

	public SequenceObjective(RPGObjective... stages) {
		for (RPGObjective stage : stages) {
			this.stages.add(stage);
		}
	}

	/** Delega eventos de escolta ao estágio ativo quando ele é um {@link EscortObjective}. */
	public void onEscortEvent(java.util.function.Consumer<EscortStage> action) {
		RPGObjective active = getActive();
		if (active instanceof EscortObjective) {
			action.accept((EscortStage) (EscortObjective) active);
		}
	}

	/** Contrato de estágio de escolta: registrado, falha e chegada. */
	public interface EscortStage {
		void onEscortSpawned(com.traduvertgames.entities.EscortNpc npc);

		void onEscortFailed(com.traduvertgames.entities.EscortNpc npc);

		void onEscortArrived(com.traduvertgames.entities.EscortNpc npc);
	}

	/** @return o objetivo atualmente em andamento (ou o último). */
	public RPGObjective getActive() {
		if (stages.isEmpty()) {
			return NullObjectiveHolder.INSTANCE;
		}
		return stages.get(Math.min(activeIndex, stages.size() - 1));
	}

	@Override
	public void onLevelStart() {
		activeIndex = 0;
		for (RPGObjective stage : stages) {
			stage.onLevelStart();
		}
	}

	@Override
	public void onLevelLoaded() {
		// Recursos programáticos (como o NPC de escolta) só podem nascer na
		// etapa em que serão usados. Criar todas as etapas antecipadamente fazia
		// o NPC da fase 8 ser registrado enquanto Infiltrator ainda estava ativo.
		getActive().onLevelLoaded();
	}

	@Override
	public void update() {
		getActive().update();
		// Avança para o próximo estágio quando o atual conclui.
		while (activeIndex < stages.size() - 1 && getActive().isComplete()) {
			activeIndex++;
			getActive().onLevelLoaded();
			SoundManager.play(SoundManager.Event.LEVELUP);
		}
	}

	@Override
	public void onQuestItemSpawned(QuestItem item) {
		getActive().onQuestItemSpawned(item);
	}

	@Override
	public void onQuestItemCollected(QuestItem item) {
		getActive().onQuestItemCollected(item);
	}

	@Override
	public void onBeaconSpawned(QuestBeacon beacon) {
		// Beacons pertencem ao mapa inteiro e podem ser usados por uma etapa
		// posterior. Registrar apenas no estágio ativo fazia a fase 3 chegar ao
		// ritual sem conhecer os obeliscos carregados durante a sobrevivência.
		for (RPGObjective stage : stages) {
			stage.onBeaconSpawned(beacon);
		}
	}

	@Override
	public void onBeaconActivated(QuestBeacon beacon) {
		// A ativação também precisa alcançar a etapa futura: se o jogador
		// interagir antes da troca de objetivo, esse progresso continua válido.
		for (RPGObjective stage : stages) {
			stage.onBeaconActivated(beacon);
		}
	}

	@Override
	public void onNpcSpawned(QuestNPC npc) {
		getActive().onNpcSpawned(npc);
	}

	@Override
	public void onNpcRescued(QuestNPC npc) {
		getActive().onNpcRescued(npc);
	}

	@Override
	public void onEnemyKilled(Enemy enemy) {
		getActive().onEnemyKilled(enemy);
	}

	@Override
	public void onDialogueStarted(InteractiveNpc npc) {
		getActive().onDialogueStarted(npc);
	}

	@Override
	public void onDialogueFinished(InteractiveNpc npc) {
		getActive().onDialogueFinished(npc);
	}

	@Override
	public void onBossSpotted() {
		for (RPGObjective stage : stages) {
			stage.onBossSpotted();
		}
	}

	@Override
	public String getTitle() {
		return getActive().getTitle();
	}

	@Override
	public String getDescription() {
		return getActive().getDescription();
	}

	@Override
	public String getProgressText() {
		return getActive().getProgressText();
	}

	@Override
	public boolean isComplete() {
		return !stages.isEmpty() && getActive().isComplete() && activeIndex >= stages.size() - 1;
	}

	@Override
	public String getTargetHint() {
		return getActive().getTargetHint();
	}

	@Override
	public String serializeState() {
		StringBuilder sb = new StringBuilder();
		sb.append("IDX=").append(activeIndex);
		for (int i = 0; i < stages.size(); i++) {
			// O estado de cada estágio pode conter ";" internamente, então os
			// estágios são separados por "||" (que não é usado pelos estágios).
			sb.append("|S").append(i).append("=").append(stages.get(i).serializeState());
		}
		return sb.toString();
	}

	@Override
	public void deserializeState(String state) {
		if (state == null || state.isEmpty()) {
			return;
		}
		String[] parts = state.split("\\|");
		for (String part : parts) {
			if (part.startsWith("IDX=")) {
				try {
					activeIndex = Integer.parseInt(part.substring("IDX=".length()));
				} catch (NumberFormatException ex) {
					activeIndex = 0;
				}
			} else if (part.startsWith("S")) {
				int eq = part.indexOf('=');
				if (eq < 0) {
					continue;
				}
				String idxPart = part.substring(1, eq);
				String stageState = part.substring(eq + 1);
				try {
					int idx = Integer.parseInt(idxPart);
					if (idx >= 0 && idx < stages.size()) {
						stages.get(idx).deserializeState(stageState);
					}
				} catch (NumberFormatException ex) {
					// índice desconhecido: ignora a parte.
				}
			}
		}
		// Se o save foi feito com o objetivo já concluído, mantê-lo concluído.
		if (activeIndex >= stages.size()) {
			activeIndex = stages.size() - 1;
		}
		// O mundo já foi carregado quando o SaveManager restaura a missão.
		// Inicializa os recursos da etapa recuperada (ex.: escolta da fase 8).
		getActive().onLevelLoaded();
	}

	/** Placeholder usado quando a sequência está vazia (nunca deve acontecer). */
	private static final class NullObjectiveHolder {
		static final RPGObjective INSTANCE = new BaseObjective("", "") {
			@Override
			public String getProgressText() {
				return "";
			}

			@Override
			public boolean isComplete() {
				return false;
			}
		};
	}
}
