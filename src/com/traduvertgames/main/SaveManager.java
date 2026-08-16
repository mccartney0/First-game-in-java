package com.traduvertgames.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.traduvertgames.entities.DashAbility;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.UltimateAbility;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.world.World;

/**
 * Sistema de salvamento correto em JSON com múltiplos slots (save/autosave/load).
 * Substitui a codificação manual de caracteres usada em {@link Menu#saveGame}
 * e {@link Menu#loadGame}, evitando perda e corrupção de dados quando valores
 * escapam do intervalo codificável.
 *
 * Arquivo: saves.json
 * Estrutura (v2):
 * {
 *   "version": 2,
 *   "activeSlot": 1,
 *   "campaign": { "completedLevels": [1,2], "maxLevelReached": 3 },
 *   "slots": [
 *     {
 *       "id": 1,
 *       "session": { "vida": 100, "level": 3, "levelPlus": 1, ... },
 *       "progress": { "objectiveState": { "1": "COMPLETE", "2": "TALKED=..." } },
 *       "timestamp": "2026-08-16T14:30:00"
 *     },
 *     ...
 *   ]
 * }
 *
 * Saves v1 (sem version/session) são migrados automaticamente na leitura.
 * A gravação é atômica (tmp + rename) para evitar corrupção por travamento.
 */
public final class SaveManager {

	/** Arquivo único que guarda todos os slots. */
	public static final File SAVE_FILE = new File("saves.json");

	/** Arquivo temporário usado na gravação atômica. */
	private static final File SAVE_TMP = new File("saves.tmp");

	/** Versão atual do esquema de save emitida na escrita. */
	public static final int SCHEMA_VERSION = 2;

	/** Número de slots disponíveis. */
	public static final int SLOT_COUNT = 3;

	/** Slot ativo na UI (1, 2 ou 3). */
	public static int activeSlot = 1;

	private static final String INDENT = "  ";


	private SaveManager() {
	}

	/** ---------- Escrita ---------- */

	/**
	 * Salva o estado atual do jogo no slot {@link #activeSlot}.
	 */
	public static boolean saveCurrentGame() {
		Game game = Game.getInstance();
		Map<String, Object> root = loadRoot();

		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findOrCreateSlot(slots, activeSlot);

		slot.put("vida", clampDouble(Player.life));
		slot.put("mana", clampDouble(Player.mana));
		slot.put("arma", clampDouble(Player.weapon));
		slot.put("escudo", clampDouble(Player.shield));
		slot.put("inimigosMortos", Enemy.enemies);
		slot.put("levelPlus", game != null ? game.getLevelPlus() : 0);
		slot.put("level", game != null ? game.getCurrentLevel() : 1);
		slot.put("pontuacao", Game.getScore());
		slot.put("recorde", Game.getHighScore());
		slot.put("melhorCombo", Game.getBestComboRecord());
		slot.put("melhorComboSessao", Game.getBestComboThisRun());
		slot.put("armaAtual", Player.getCurrentWeaponOrdinal());
		slot.put("armasDesbloqueadas", Player.getWeaponUnlockMask());

		for (WeaponType type : WeaponType.values()) {
			slot.put("energiaArma_" + type.name(), clampDouble(Player.getStoredEnergyForType(type)));
		}

		Map<String, Object> session = new HashMap<String, Object>();
		for (Map.Entry<String, Object> entry : slot.entrySet()) {
			String key = entry.getKey();
			if (!"id".equals(key) && !"progress".equals(key) && !"timestamp".equals(key)) {
				session.put(key, entry.getValue());
			}
		}
		Map<String, Object> progress = buildProgressMap(game);
		slot.put("session", session);
		slot.put("progress", progress);
		slot.put("timestamp", currentTimestamp());
		// Remove as chaves antigas agora duplicadas dentro de session.
		for (String key : session.keySet()) {
			slot.remove(key);
		}

		updateCampaign(root, game);

		root.put("activeSlot", activeSlot);
		root.put("slots", slots);

		return writeRoot(root);
	}

	/**
	 * Salva automaticamente no slot ativo (usado ao morrer ou trocar de fase).
	 */
	public static boolean saveAutoSave() {
		return saveCurrentGame();
	}

	/** Progresso narrativo da fase atual (estado da missão) por nível. */
	private static Map<String, Object> buildProgressMap(Game game) {
		Map<String, Object> progress = new HashMap<String, Object>();
		Map<String, String> objectiveState = new HashMap<String, String>();
		String state = com.traduvertgames.quest.QuestManager.serializeObjectiveState();
		if (state != null && !state.isEmpty() && !"UNKNOWN".equals(state)) {
			int key = game != null ? game.getCurrentLevel()
					: Math.max(1, com.traduvertgames.quest.QuestManager.getCurrentLevel());
			objectiveState.put(String.valueOf(key), state);
		}
		progress.put("objectiveState", objectiveState);
		return progress;
	}

	/** Atualiza a seção de campanha global (fases concluídas e fase máxima). */
	private static void updateCampaign(Map<String, Object> root, Game game) {
		@SuppressWarnings("unchecked")
		Map<String, Object> campaign = (Map<String, Object>) root.get("campaign");
		if (campaign == null) {
			campaign = new HashMap<String, Object>();
			root.put("campaign", campaign);
		}
		Game current = game != null ? game : Game.getInstance();
		if (current != null) {
			int reached = current.getCurrentLevel();
			int previousMax = toInt(campaign.get("maxLevelReached"));
			campaign.put("maxLevelReached", Math.max(previousMax, reached));
			// Uma fase é considerada concluída quando o jogador avança além dela:
			// o save registra a fase ANTERIOR à atual como concluída ao avançar.
			int completedLevel = reached - 1;
			if (completedLevel >= 1 && completedLevel < Game.MAX_LEVEL) {
				@SuppressWarnings("unchecked")
				List<Object> completed = (List<Object>) campaign.get("completedLevels");
				if (completed == null) {
					completed = new ArrayList<Object>();
					campaign.put("completedLevels", completed);
				}
				if (!completed.contains(completedLevel)) {
					completed.add(completedLevel);
				}
			}
		}
	}

	/** ---------- Leitura ---------- */

	/**
	 * Carrega o slot informado (1..3) e aplica o estado ao jogo.
	 *
	 * @return true se o slot existia e foi carregado
	 */
	public static boolean loadSlot(int slotId) {
		if (slotId < 1 || slotId > SLOT_COUNT) {
			return false;
		}
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot == null) {
			return false;
		}

		Game game = Game.getInstance();

		// Migração v1→v2: se o slot é flat (v1), a sessão é o próprio slot.
		Map<String, Object> session = getSession(slot);

		// Valores salvos são aplicados DEPOIS do reload do mundo, pois o
		// restart redefine os máximos de vida/mana/escudo para a fase carregada.
		double savedLife = toDouble(session.get("vida"));
		double savedMana = toDouble(session.get("mana"));
		double savedWeapon = toDouble(session.get("arma"));
		double savedShield = toDouble(session.get("escudo"));
		int savedEnemies = toInt(session.get("inimigosMortos"));
		int savedLevelPlus = toInt(session.get("levelPlus"));
		int savedLevel = toInt(session.get("level"));
		int savedScore = toInt(session.get("pontuacao"));
		int savedHighScore = toInt(session.get("recorde"));
		int savedBestComboRecord = toInt(session.get("melhorCombo"));
		int savedBestComboSession = toInt(session.get("melhorComboSessao"));
		int savedWeaponOrdinal = toInt(session.get("armaAtual"));
		int savedWeaponMask = toInt(session.get("armasDesbloqueadas"));

		Enemy.enemies = savedEnemies;
		Game.setScore(savedScore);
		Game.setHighScore(Math.max(savedScore, savedHighScore));
		Game.setBestComboRecord(Math.max(1, savedBestComboRecord));
		Game.setBestComboThisRun(Math.max(1, savedBestComboSession));

		if (game != null) {
			// Troca de fase completa: recarrega o mapa, a quest e o chefe da fase
			// salva (sem reabrir o onboarding), garantindo que o jogo retorne
			// exatamente à fase em que foi salvo — e não à fase atual.
			game.setLevelPlus(savedLevelPlus);
			game.setCurrentLevel(savedLevel);
			World.restartGame("level" + Math.min(Math.max(1, savedLevel), Game.MAX_LEVEL) + ".png");
			OnboardingManager.stop();
			LevelUpManager.reset();
			WaveManager.reset();
			DashAbility.reset();
			UltimateAbility.reset();
			LootGuarantee.reset();
			com.traduvertgames.graficos.ParticleSystem.clear();
			com.traduvertgames.entities.FloatingText.clear();
			game.resetGameOverState();
			game.clearQuestPending();
			game.applyDifficultyToPlayerStats();
			Player.life = savedLife;
			Player.mana = savedMana;
			Player.weapon = savedWeapon;
			Player.shield = savedShield;
			game.clampPlayerResources();
			if (game.player != null) {
				game.player.syncFromPersistentState();
				game.player.updateCamera();
			}
			// Arsenal salvo é aplicado DEPOIS do restart: o World.restartGame
			// recria o Player (construtor sincroniza o arsenal persistente),
			// então a restauração aqui garante que a arma e as energias do
			// save prevaleçam sobre o estado recém-inicializado.
			Player.loadCurrentWeaponFromSave(savedWeaponOrdinal);
			Player.loadUnlockedWeaponsFromSave(savedWeaponMask);
			for (WeaponType type : WeaponType.values()) {
				Object raw = session.get("energiaArma_" + type.name());
				if (raw != null) {
					Player.loadWeaponEnergyFromSave(type, toDouble(raw));
				}
			}
			if (game.player != null) {
				game.player.syncFromPersistentState();
			}
			Game.gameState = "NORMAL";
			Menu.pause = false;
			activeSlot = slotId;
			restoreObjectiveState(slot, savedLevel);
			return true;
		}

		World.restartGame("level" + Math.min(Math.max(1, savedLevel), Game.MAX_LEVEL) + ".png");
		if (game != null) {
			game.setLevelPlus(savedLevelPlus);
			game.applyDifficultyToPlayerStats();
		}
		Player.life = savedLife;
		Player.mana = savedMana;
		Player.weapon = savedWeapon;
		Player.shield = savedShield;
		Game.gameState = "NORMAL";
		Menu.pause = false;
		activeSlot = slotId;
		restoreObjectiveState(slot, savedLevel);
		return true;
	}

	/** Sessão do slot: v2 usa a seção "session"; v1 é o próprio slot (flat). */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> getSession(Map<String, Object> slot) {
		Object raw = slot.get("session");
		if (raw instanceof Map) {
			return (Map<String, Object>) raw;
		}
		return slot;
	}

	/** Restaura o estado da missão da fase salva após o reload do mundo. */
	@SuppressWarnings("unchecked")
	private static void restoreObjectiveState(Map<String, Object> slot, int savedLevel) {
		Object raw = slot.get("progress");
		if (!(raw instanceof Map)) {
			return;
		}
		Map<String, Object> progress = (Map<String, Object>) raw;
		Object rawState = progress.get("objectiveState");
		if (!(rawState instanceof Map)) {
			return;
		}
		Map<String, Object> objectiveState = (Map<String, Object>) rawState;
		Object state = objectiveState.get(String.valueOf(savedLevel));
		if (state instanceof String) {
			com.traduvertgames.quest.QuestManager.deserializeObjectiveState((String) state);
		}
	}

	/** Verifica se existe ao menos um slot salvo. */
	public static boolean hasAnySave() {
		if (!SAVE_FILE.exists()) {
			return false;
		}
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		for (Map<String, Object> slot : slots) {
			Map<String, Object> session = getSession(slot);
			if (session.containsKey("vida") || session.containsKey("level")) {
				return true;
			}
		}
		return false;
	}

	/** Verifica se um slot específico possui dados salvos. */
	public static boolean hasSlotSave(int slotId) {
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		return slot != null && (getSession(slot).containsKey("vida") || getSession(slot).containsKey("level"));
	}

	/** Retorna a fase salva em um slot, ou -1 se vazio. */
	public static int getSlotLevel(int slotId) {
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot == null) {
			return -1;
		}
		Object level = getSession(slot).get("level");
		return level instanceof Number ? ((Number) level).intValue() : -1;
	}

	/**
	 * Resumo humano do progresso de missão salvo em um slot (ex.: "Fase 1:
	 * Fale com a Comandante Ava"), ou "" quando não há progresso de missão.
	 */
	public static String getSlotObjectiveText(int slotId) {
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot == null) {
			return "";
		}
		Object raw = slot.get("progress");
		if (!(raw instanceof Map)) {
			return "";
		}
		Map<String, Object> progress = (Map<String, Object>) raw;
		Object rawState = progress.get("objectiveState");
		if (!(rawState instanceof Map)) {
			return "";
		}
		Map<String, Object> objectiveState = (Map<String, Object>) rawState;
		// Procura o estado pela fase salva no slot e, se não houver, pela fase
		// registrada na sessão (o save pode ter sido feito com uma fase
		// ligeiramente diferente da exibida no rótulo do slot).
		int savedLevel = getSlotLevel(slotId);
		Object state = objectiveState.get(String.valueOf(savedLevel));
		if (!(state instanceof String)) {
			Object sessionLevel = getSession(slot).get("level");
			if (sessionLevel instanceof Number) {
				state = objectiveState.get(String.valueOf(((Number) sessionLevel).intValue()));
			}
		}
		if (!(state instanceof String) && !objectiveState.isEmpty()) {
			// Usa o estado da chave mais alta como estimativa.
			int bestLevel = 0;
			Object bestState = null;
			for (Map.Entry<String, Object> entry : objectiveState.entrySet()) {
				int candidate = toInt(entry.getValue() == null ? 0 : Integer.parseInt(entry.getKey()));
				if (candidate > bestLevel) {
					bestLevel = candidate;
					bestState = entry.getValue();
				}
			}
			state = bestState;
		}
		if (!(state instanceof String)) {
			return "";
		}
		String stateText = (String) state;
		if ("COMPLETE".equals(stateText) || stateText.startsWith("COMPLETE")) {
			return "";
		}
		String levelTitle = com.traduvertgames.quest.QuestManager.getPhaseTitle(savedLevel);
		if (stateText.startsWith("TALKED=false")) {
			// Estado salvo antes de falar com o NPC da fase: indica o alvo da missão.
			return "Fase " + savedLevel + ": " + levelTitle + " (falta falar com o NPC)";
		}
		return "Fase " + savedLevel + ": " + levelTitle + " (em andamento)";
	}

	/** Retorna a pontuação salva em um slot, ou -1 se vazio. */
	public static int getSlotScore(int slotId) {
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot == null) {
			return -1;
		}
		Object score = getSession(slot).get("pontuacao");
		return score instanceof Number ? ((Number) score).intValue() : -1;
	}

	/** Apaga o conteúdo de um slot específico. */
	public static boolean clearSlot(int slotId) {
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot != null) {
			Map<String, Object> session = getSession(slot);
			session.clear();
			Object progress = slot.get("progress");
			slot.clear();
			slot.put("id", slotId);
			if (progress instanceof Map) {
				slot.put("progress", progress);
			}
			slot.put("timestamp", "");
		}
		root.put("slots", slots);
		return writeRoot(root);
	}

	/** ---------- Serialização JSON manual (sem dependências externas) ---------- */

	@SuppressWarnings("unchecked")
	private static Map<String, Object> loadRoot() {
		if (!SAVE_FILE.exists()) {
			return emptyRoot();
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(SAVE_FILE));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append('\n');
			}
			reader.close();
			Object parsed = JsonParser.parse(builder.toString());
			if (parsed instanceof Map) {
				return (Map<String, Object>) parsed;
			}
		} catch (IOException ignored) {
		}
		return emptyRoot();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> getSlots(Map<String, Object> root) {
		Object raw = root.get("slots");
		List<Map<String, Object>> slots = new ArrayList<Map<String, Object>>();
		if (raw instanceof List) {
			for (Object entry : (List<?>) raw) {
				if (entry instanceof Map) {
					slots.add((Map<String, Object>) entry);
				} else {
					slots.add(new HashMap<String, Object>());
				}
			}
		}
		while (slots.size() < SLOT_COUNT) {
			slots.add(new HashMap<String, Object>());
		}
		return slots;
	}

	private static Map<String, Object> findSlot(List<Map<String, Object>> slots, int slotId) {
		for (Map<String, Object> slot : slots) {
			Object id = slot.get("id");
			if (id instanceof Number && ((Number) id).intValue() == slotId) {
				return slot;
			}
		}
		return null;
	}

	private static Map<String, Object> findOrCreateSlot(List<Map<String, Object>> slots, int slotId) {
		Map<String, Object> existing = findSlot(slots, slotId);
		if (existing != null) {
			return existing;
		}
		Map<String, Object> fresh = new HashMap<String, Object>();
		fresh.put("id", slotId);
		// Sobrescreve o slot de menor prioridade: o que estiver vazio ou o último.
		int targetIndex = -1;
		for (int i = 0; i < slots.size(); i++) {
			Map<String, Object> current = slots.get(i);
			if (current.isEmpty()) {
				targetIndex = i;
				break;
			}
		}
		if (targetIndex == -1) {
			targetIndex = 0;
		}
		slots.set(targetIndex, fresh);
		return fresh;
	}

	private static Map<String, Object> emptyRoot() {
		Map<String, Object> root = new HashMap<String, Object>();
		root.put("activeSlot", activeSlot);
		root.put("slots", new ArrayList<Map<String, Object>>());
		return root;
	}

	private static boolean writeRoot(Map<String, Object> root) {
		root.put("version", SCHEMA_VERSION);
		BufferedWriter writer = null;
		try {
			// Gravação atômica: escreve em tmp e renomeia, evitando corrupção
			// se o jogo travar no meio da escrita.
			if (SAVE_TMP.exists() && !SAVE_TMP.delete()) {
				return false;
			}
			writer = new BufferedWriter(new FileWriter(SAVE_TMP));
			writer.write(JsonWriter.write(root));
			writer.flush();
			writer.close();
			writer = null;
			if (!SAVE_TMP.renameTo(SAVE_FILE)) {
				SAVE_TMP.delete();
				return false;
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	/** Timestamp ISO simples para o campo "timestamp" do slot. */
	private static String currentTimestamp() {
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		StringBuilder builder = new StringBuilder();
		builder.append(calendar.get(java.util.Calendar.YEAR));
		append2(builder, calendar.get(java.util.Calendar.MONTH) + 1);
		append2(builder, calendar.get(java.util.Calendar.DAY_OF_MONTH));
		builder.append('T');
		append2(builder, calendar.get(java.util.Calendar.HOUR_OF_DAY));
		append2(builder, calendar.get(java.util.Calendar.MINUTE));
		append2(builder, calendar.get(java.util.Calendar.SECOND));
		return builder.toString();
	}

	private static void append2(StringBuilder builder, int value) {
		builder.append(value < 10 ? "0" : "").append(value);
	}

	private static int clampDouble(double value) {
		if (value < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		if (value > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) Math.round(value);
	}

	private static int toInt(Object value) {
		return value instanceof Number ? ((Number) value).intValue() : 0;
	}

	private static double toDouble(Object value) {
		return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
	}
}
