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
 * Estrutura (v3):
 * {
 *   "version": 3,
 *   "activeSlot": 1,
 *   "bestRun": { "bestKills": 57, "bestTimeMs": 243000, "bestCombo": 5, "bestScore": 98400 },
 *   "campaign": { "completedLevels": [1,2], "maxLevelReached": 3 },
 *   "slots": [
 *     {
 *       "id": 1,
 *       "session": { "vida": 100, "level": 3, "levelPlus": 1, ... },
 *       "progress": {
 *         "objectiveState": { "1": "COMPLETE", "2": "TALKED=..." },
 *         "npcDialogues": { "Ava_1": true, "Hélio_7": true }
 *       },
 *       "timestamp": "2026-08-16T14:30:00"
 *     },
 *     ...
 *   ]
 * }
 *
 * Saves v1 (sem version/session) e v2 (sem bestRun/npcDialogues) são migrados
 * automaticamente na leitura. A gravação é atômica (tmp + rename) para evitar
 * corrupção por travamento. O {@code bestRun} é global ao arquivo de save
 * (não por slot) e captura a melhor partida já registrada: um snapshot
 * completo é gravado sempre que qualquer métrica (kills, tempo, combo ou
 * pontuação) supera o recorde anterior.
 */
public final class SaveManager {

	/** Arquivo único que guarda todos os slots. */
	public static final File SAVE_FILE = new File("saves.json");

	/** Arquivo temporário usado na gravação atômica. */
	private static final File SAVE_TMP = new File("saves.tmp");

	/** Versão atual do esquema de save emitida na escrita. */
	public static final int SCHEMA_VERSION = 3;

	/** Chave do snapshot da melhor partida no root do save (global, não por slot). */
	private static final String BEST_RUN_KEY = "bestRun";
	/** Chave do recorde de kills da melhor partida. */
	private static final String BEST_KILLS_KEY = "bestKills";
	/** Chave do tempo (ms) da melhor partida. */
	private static final String BEST_TIME_KEY = "bestTimeMs";
	/** Chave do melhor combo da melhor partida. */
	private static final String BEST_COMBO_KEY = "bestCombo";
	/** Chave da pontuação da melhor partida. */
	private static final String BEST_SCORE_KEY = "bestScore";
	/** Chave do mapa de flags de diálogos por NPC/fase no progress do slot. */
	private static final String NPC_DIALOGUES_KEY = "npcDialogues";

	/** Snapshot da melhor partida carregado do arquivo (em memória). */
	private static int bestRunKills = 0;
	private static long bestRunTimeMs = 0;
	private static int bestRunCombo = 0;
	private static int bestRunScore = 0;

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
		slot.put("survivalRecord", com.traduvertgames.main.WaveManager.getSurvivalRecord());

		// Atualiza o recorde global da melhor partida: se qualquer métrica da
		// fase que acabou de terminar superar o snapshot anterior, o bestRun
		// é regravado como um snapshot completo desta partida.
		captureBestRun();

		for (WeaponType type : WeaponType.values()) {
			slot.put("energiaArma_" + type.name(), clampDouble(Player.getStoredEnergyForType(type)));
		}

		// Companion ativo persistido na sessão (v3): tipo e HP para restaurar
		// a criatura na posição do jogador ao carregar.
		try {
			com.traduvertgames.entities.Companion activeCompanion = com.traduvertgames.entities.Companion.getActive();
			if (activeCompanion != null) {
				slot.put("companionType", activeCompanion.getType().name());
				slot.put("companionHp", clampDouble(activeCompanion.getHp()));
				// Skin de customização do companion (v3).
				slot.put("companionSkin", activeCompanion.getSkin().name());
			} else {
				slot.put("companionType", "");
			}
		} catch (Throwable ignored) {
			slot.put("companionType", "");
		}

		Map<String, Object> session = new HashMap<String, Object>();
		for (Map.Entry<String, Object> entry : slot.entrySet()) {
			String key = entry.getKey();
			if (!"id".equals(key) && !"progress".equals(key) && !"timestamp".equals(key)) {
				session.put(key, entry.getValue());
			}
		}
		Map<String, Object> progress = buildProgressMap(game);
		// As flags de diálogos por NPC/fase são persistidas em memória e
		// refletidas no progress a cada gravação (a migração v2→v3 inicia
		// o mapa a partir do disco, então ele precisa ser reescrito aqui).
		if (!npcDialogues.isEmpty()) {
			progress.put(NPC_DIALOGUES_KEY, npcDialogues);
		}
		slot.put("session", session);
		slot.put("progress", progress);
		// O recorde de sobrevivência também fica em session para exibição rápida
		// no menu de carregar sem precisar desaninhar o mapa.
		slot.put("survivalRecord", com.traduvertgames.main.WaveManager.getSurvivalRecord());
		slot.put("timestamp", currentTimestamp());
		// Remove as chaves antigas agora duplicadas dentro de session.
		for (String key : session.keySet()) {
			slot.remove(key);
		}

		updateCampaign(root, game);

		root.put("activeSlot", activeSlot);
		writeBestRun(root);
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
		// Flags narrativas da campanha (ex.: desertor do subsolo da fase 7).
		progress.put("traitorTalked", Game.isTraitorTalked());
		return progress;
	}

	// ---------- BestRun (melhor partida acumulada por save) ----------

	/**
	 * Captura a melhor partida em memória a partir dos stats da fase atual:
	 * se qualquer métrica (kills, tempo, combo ou pontuação) superar o snapshot
	 * anterior, o bestRun é reescrito com os valores atuais da partida.
	 * Chamado automaticamente por {@link #saveCurrentGame()} e por
	 * {@link Game#resetLevelStats()} antes de zerar os contadores da fase.
	 */
	public static void captureBestRun() {
		int kills = Game.getKillsThisLevel();
		long timeMs = Game.getLevelTimeMs();
		int combo = Game.getBestComboThisRun();
		int score = Game.getScore();
		// Fase ainda em andamento ou sem stats válidos: nada a capturar.
		if (kills <= 0 || timeMs <= 0) {
			return;
		}
		// Regra do snapshot: qualquer métrica acima do recorde anterior
		// reescreve o bestRun com os valores atuais da partida.
		boolean faster = bestRunTimeMs > 0 && timeMs < bestRunTimeMs;
		if (kills > bestRunKills || faster || combo > bestRunCombo || score > bestRunScore) {
			bestRunKills = kills;
			bestRunTimeMs = timeMs;
			bestRunCombo = combo;
			bestRunScore = score;
		}
	}

	/** Grava a seção bestRun no root do save (vazio quando não há recorde). */
	private static void writeBestRun(Map<String, Object> root) {
		if (bestRunKills <= 0 && bestRunCombo <= 0) {
			root.remove(BEST_RUN_KEY);
			return;
		}
		Map<String, Object> bestRun = new HashMap<String, Object>();
		bestRun.put(BEST_KILLS_KEY, bestRunKills);
		bestRun.put(BEST_TIME_KEY, bestRunTimeMs);
		bestRun.put(BEST_COMBO_KEY, bestRunCombo);
		bestRun.put(BEST_SCORE_KEY, bestRunScore);
		root.put(BEST_RUN_KEY, bestRun);
	}

	/** @return kills da melhor partida (0 se ainda não há recorde). */
	public static int getBestRunKills() {
		return bestRunKills;
	}

	/** @return tempo (ms) da melhor partida (0 se ainda não há recorde). */
	public static long getBestRunTimeMs() {
		return bestRunTimeMs;
	}

	/** @return melhor combo da melhor partida (0 se ainda não há recorde). */
	public static int getBestRunCombo() {
		return bestRunCombo;
	}

	/** @return pontuação da melhor partida (0 se ainda não há recorde). */
	public static int getBestRunScore() {
		return bestRunScore;
	}

	/** @return true se existe ao menos um recorde registrado no save. */
	public static boolean hasBestRun() {
		return bestRunKills > 0 || bestRunCombo > 0;
	}

	/**
	 * Recarrega o snapshot da melhor partida a partir do disco, para que as
	 * telas de menu exibam o recorde acumulado mesmo antes de um loadSlot.
	 */
	public static void refreshBestRun() {
		restoreBestRun(loadRoot());
	}

	// ---------- Flags de diálogos por NPC/fase ----------

	/** Flags de diálogos concluídos, carregadas do disco e mantidas em memória. */
	private static final Map<String, Boolean> npcDialogues = new HashMap<String, Boolean>();

	/**
	 * Registra que o jogador concluiu uma conversa com o NPC informado na fase
	 * atual. A chave persistida é {@code nomeNpc_fase} (ex.: {@code Ava_1}).
	 */
	public static void markNpcDialogue(String npcName, int level) {
		if (npcName == null || npcName.isEmpty()) {
			return;
		}
		npcDialogues.put(npcName + "_" + level, true);
	}

	/** @return true se a conversa com o NPC já foi concluída na fase. */
	public static boolean hasNpcDialogue(String npcName, int level) {
		return npcDialogues.get(npcName + "_" + level) == Boolean.TRUE;
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

		// Migração v2→v3: snapshot da melhor partida (root) e flags de diálogos.
		restoreBestRun(root);
		restoreNpcDialogues(slot);

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
			// Recorde de sobrevivência restaurado do save (preservado entre sessões).
			WaveManager.setSurvivalRecord(toInt(session.get("survivalRecord")));
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
			restoreNarrativeFlags(slot);
			restoreCompanion(session);
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
		restoreNarrativeFlags(slot);
		restoreCompanion(session);
		return true;
	}

	/** Restaura o snapshot da melhor partida do root do save (migração v2→v3). */
	@SuppressWarnings("unchecked")
	private static void restoreBestRun(Map<String, Object> root) {
		Object raw = root.get(BEST_RUN_KEY);
		if (!(raw instanceof Map)) {
			return;
		}
		Map<String, Object> bestRun = (Map<String, Object>) raw;
		bestRunKills = toInt(bestRun.get(BEST_KILLS_KEY));
		bestRunTimeMs = bestRun.get(BEST_TIME_KEY) instanceof Number
				? ((Number) bestRun.get(BEST_TIME_KEY)).longValue() : 0;
		bestRunCombo = toInt(bestRun.get(BEST_COMBO_KEY));
		bestRunScore = toInt(bestRun.get(BEST_SCORE_KEY));
	}

	/** Restaura as flags de diálogos por NPC/fase do slot (migração v2→v3). */
	@SuppressWarnings("unchecked")
	private static void restoreNpcDialogues(Map<String, Object> slot) {
		Object raw = slot.get("progress");
		if (!(raw instanceof Map)) {
			return;
		}
		Map<String, Object> progress = (Map<String, Object>) raw;
		Object dialoguesRaw = progress.get(NPC_DIALOGUES_KEY);
		if (!(dialoguesRaw instanceof Map)) {
			return;
		}
		Map<String, Object> dialogues = (Map<String, Object>) dialoguesRaw;
		for (Map.Entry<String, Object> entry : dialogues.entrySet()) {
			if ("true".equalsIgnoreCase(String.valueOf(entry.getValue()))) {
				npcDialogues.put(entry.getKey(), true);
			}
		}
	}

	/** Restaura o companion ativo do slot na posição do jogador (v3). */
	private static void restoreCompanion(Map<String, Object> session) {
		try {
			com.traduvertgames.entities.Companion.clear();
			Object raw = session.get("companionType");
			String type = raw instanceof String ? (String) raw : "";
			if (type.isEmpty()) {
				return;
			}
			com.traduvertgames.entities.Companion.CompanionType companionType =
					com.traduvertgames.entities.Companion.CompanionType.valueOf(type);
			double savedHp = toDouble(session.get("companionHp"));
			com.traduvertgames.entities.Companion.spawn(companionType, savedHp);
			// Skin de customização (v3): padrão se o campo estiver ausente.
			Object skinRaw = session.get("companionSkin");
			String skin = skinRaw instanceof String ? (String) skinRaw : "";
			if (!skin.isEmpty() && com.traduvertgames.entities.Companion.getActive() != null) {
				com.traduvertgames.entities.Companion.CompanionSkin companionSkin =
						com.traduvertgames.entities.Companion.CompanionSkin.valueOf(skin);
				com.traduvertgames.entities.Companion.getActive().setSkin(companionSkin);
			}
		} catch (Throwable ignored) {
			// Save sem companion (v2 ou campo ausente): segue sem criatura.
		}
	}

	/** Restaura as flags narrativas salvas (ex.: TraitorNpc da fase 7). */
	@SuppressWarnings("unchecked")
	private static void restoreNarrativeFlags(Map<String, Object> slot) {
		Object raw = slot.get("progress");
		if (!(raw instanceof Map)) {
			return;
		}
		Map<String, Object> progress = (Map<String, Object>) raw;
		Object talked = progress.get("traitorTalked");
		Game.resetTraitorTalked();
		if ("true".equalsIgnoreCase(String.valueOf(talked))) {
			Game.setTraitorTalked(true);
		}
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

	/** Recorde de ondas do modo sobrevivência do slot, ou 0 se vazio. */
	public static int getSlotSurvivalRecord(int slotId) {
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot == null) {
			return 0;
		}
		return toInt(getSession(slot).get("survivalRecord"));
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
