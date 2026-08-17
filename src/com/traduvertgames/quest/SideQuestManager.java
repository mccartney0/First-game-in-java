package com.traduvertgames.quest;

import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.InventoryManager;
import com.traduvertgames.main.SoundManager;

/**
 * Missões secundárias (rodada 22). Missões curtas oferecidas por NPCs
 * secundários (BranchingNpc) que recompensam o jogador ao completar
 * objetivos adicionais dentro da fase atual:
 *
 * - KILL_N: eliminar N inimigos nesta fase;
 * - COLLECT_N: coletar N unidades de um tipo de item no inventário;
 * - DELIVER: entregar um item específico do inventário (consome).
 *
 * Cada missão tem um estado (ativa/concluída/abandonada) persistido na
 * session do save ("sideQuests") e as recompensas são entregues por
 * Reward (vida/escudo/mana/score/skin unlock).
 *
 * O progresso é atualizado pelos hooks do QuestManager/Player e o estado
 * fica acessível via isActive/getProgress/hasCompleted para renderização
 * na HUD da missão e no painel do NPC.
 */
public final class SideQuestManager {

	/** Tipos de missão secundária. */
	public enum Type {
		KILL_N, COLLECT_N, DELIVER
	}

	/** Recompensa entregue ao concluir a missão. */
	public static final class Reward {
		public final double life;
		public final double shield;
		public final double mana;
		public final int score;

		public Reward(double life, double shield, double mana, int score) {
			this.life = life;
			this.shield = shield;
			this.mana = mana;
			this.score = score;
		}

		/** Aplica a recompensa ao jogador atual. */
		public void grant() {
			if (Game.player == null) {
				return;
			}
			Game.player.heal(life);
			Game.player.addShield(shield);
			Game.player.addMana(mana);
			if (score > 0) {
				Game.addScore(score);
			}
			com.traduvertgames.entities.FloatingText.show("MISSAO SECUNDARIA CONCLUIDA",
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 40,
					new java.awt.Color(255, 235, 59), 90);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (life > 0) sb.append("+" + (int) life + " vida ");
			if (shield > 0) sb.append("+" + (int) shield + " escudo ");
			if (mana > 0) sb.append("+" + (int) mana + " mana ");
			if (score > 0) sb.append("+" + score + " pts");
			return sb.toString();
		}
	}

	/** Definição de uma missão secundária. */
	public static final class SideQuest {
		public final String id;
		public final Type type;
		public final InventoryManager.ItemType itemType; // COLLECT_N/DELIVER
		public final int target;
		public final Reward reward;

		public SideQuest(String id, Type type, InventoryManager.ItemType itemType, int target,
				Reward reward) {
			this.id = id;
			this.type = type;
			this.itemType = itemType;
			this.target = target;
			this.reward = reward;
		}
	}

	private static final Map<String, SideQuest> quests = new HashMap<String, SideQuest>();
	private static final Map<String, Integer> progress = new HashMap<String, Integer>();
	private static final Map<String, Boolean> completed = new HashMap<String, Boolean>();

	private SideQuestManager() {
	}

	/** Registra uma missão (chamado pelos NPCs/StoryManager). */
	public static void register(SideQuest quest) {
		quests.put(quest.id, quest);
	}

	public static SideQuest get(String id) {
		return quests.get(id);
	}

	/** Ativa a missão e zera o progresso (nova fase/novo save). */
	public static void activate(String id) {
		if (quests.containsKey(id)) {
			progress.put(id, 0);
			completed.remove(id);
		}
	}

	public static boolean isActive(String id) {
		return quests.containsKey(id) && progress.containsKey(id) && !isCompleted(id);
	}

	public static boolean isCompleted(String id) {
		Boolean done = completed.get(id);
		return done != null && done.booleanValue();
	}

	public static int getProgress(String id) {
		Integer p = progress.get(id);
		return p != null ? p.intValue() : 0;
	}

	/** Registra N eventos de progresso (ex.: kill +1). */
	public static void addProgress(String id, int delta) {
		if (!isActive(id)) {
			return;
		}
		int next = getProgress(id) + delta;
		progress.put(id, Math.max(0, next));
		if (next >= quests.get(id).target) {
			complete(id);
		}
	}

	/** Marca a missão como concluída e entrega a recompensa. */
	public static void complete(String id) {
		SideQuest quest = quests.get(id);
		if (quest == null || isCompleted(id)) {
			return;
		}
		completed.put(id, true);
		quest.reward.grant();
		SoundManager.play(SoundManager.Event.TUTORIAL_DONE);
	}

	/** Registra um kill (mission KILL_N) para as missões ativas. */
	public static void onEnemyKilled(com.traduvertgames.entities.Enemy enemy) {
		if (enemy == null) {
			return;
		}
		for (Map.Entry<String, SideQuest> entry : quests.entrySet()) {
			if (entry.getValue().type == Type.KILL_N && isActive(entry.getKey())) {
				addProgress(entry.getKey(), 1);
			}
		}
	}

	/** Atualiza missões de coleta/entrega a partir do inventário. */
	public static void refreshCollectibles() {
		for (Map.Entry<String, SideQuest> entry : quests.entrySet()) {
			SideQuest quest = entry.getValue();
			if (quest.type == Type.COLLECT_N && isActive(quest.id)) {
				int have = InventoryManager.count(quest.itemType);
				progress.put(quest.id, have);
				if (have >= quest.target) {
					complete(quest.id);
				}
			}
		}
	}

	/** Entrega o item da missão de DELIVER (consome do inventário). */
	public static boolean deliver(String id) {
		SideQuest quest = quests.get(id);
		if (quest == null || quest.type != Type.DELIVER || !isActive(id)) {
			return false;
		}
		if (!InventoryManager.consume(quest.itemType, 1)) {
			return false;
		}
		complete(id);
		return true;
	}

	/** Label legível do progresso (ex.: "5/10 inimigos"). */
	public static String getProgressLabel(String id) {
		SideQuest quest = quests.get(id);
		if (quest == null) {
			return "";
		}
		if (isCompleted(id)) {
			return "concluida";
		}
		if (!isActive(id)) {
			return "";
		}
		String item = quest.itemType != null ? quest.itemType.displayName : "inimigos";
		return getProgress(id) + "/" + quest.target + " " + item;
	}

	/** Persistência: snapshot {id -> progresso}. */
	public static Map<String, Boolean> getCompleted() {
		return new HashMap<String, Boolean>(completed);
	}

	public static Map<String, Integer> serialize() {
		Map<String, Integer> snapshot = new HashMap<String, Integer>();
		for (Map.Entry<String, Integer> entry : progress.entrySet()) {
			snapshot.put(entry.getKey(), entry.getValue());
		}
		return snapshot;
	}

	/** Persistência: restaura progresso e marcações do save. */
	public static void deserialize(Map<String, Integer> snapshot, Map<String, Boolean> done) {
		progress.clear();
		completed.clear();
		if (snapshot != null) {
			for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
				progress.put(entry.getKey(), Math.max(0, entry.getValue()));
			}
		}
		if (done != null) {
			for (Map.Entry<String, Boolean> entry : done.entrySet()) {
				completed.put(entry.getKey(), entry.getValue());
			}
		}
	}

	/** Novo jogo limpa o estado de missões secundárias. */
	public static void reset() {
		quests.clear();
		progress.clear();
		completed.clear();
	}
}
