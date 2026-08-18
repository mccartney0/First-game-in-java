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

	/** Última versão válida antes de uma nova gravação. */
	public static final File SAVE_BACKUP = new File("saves.backup.json");

	/** Versão atual do esquema de save emitida na escrita. */
	public static final int SCHEMA_VERSION = 4;

	/** Chave do recorde de profundidade do modo infinito por slot (rodada 24b). */
	private static final String DEEP_RECORD_KEY = "deepRecord";

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

	/** Chave da flag de campanha concluída no root do save (rodada 31). */
	private static final String CAMPAIGN_COMPLETED_KEY = "campaignCompleted";

	/** Chave da flag de Nova campanha+ no root do save (rodada 31). */
	private static final String NEW_GAME_PLUS_KEY = "newGamePlus";

	/** Recorde de profundidade carregado do slot ativo (rodada 24b). */
	private static int deepRecord = 0;
	/** Campanha concluída (rodada 31 — conteúdo pós-campanha). */
	private static boolean campaignCompleted = false;
	/** Nova campanha+ ativa (rodada 31): herda armas e créditos com bônus. */
	private static boolean newGamePlus = false;
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
		// Rodada 25: o conjunto de inimigos mortos por tile (formato x,y,B|N)
		// evita que o reinício da fase ressuscite os abatidos — o
		// applyMapPixels pula as posições registradas aqui.
		slot.put("inimigosMortosSet", com.traduvertgames.main.EnemyKillTracker.serialize());
		slot.put("levelPlus", game != null ? game.getLevelPlus() : 0);
		slot.put("level", game != null ? game.getCurrentLevel() : 1);
		slot.put("pontuacao", Game.getScore());
		slot.put("recorde", Game.getHighScore());
		slot.put("melhorCombo", Game.getBestComboRecord());
		slot.put("melhorComboSessao", Game.getBestComboThisRun());
		slot.put("armaAtual", Player.getCurrentWeaponOrdinal());
		slot.put("armasDesbloqueadas", Player.getWeaponUnlockMask());
		slot.put("survivalRecord", com.traduvertgames.main.WaveManager.getSurvivalRecord());
		// Recorde de profundidade (rodada 24b): a maior profundidade alcançada
		// no modo infinito deste slot — gravado no nível superior do slot e
		// herdado pela sessão (construída logo abaixo).
		slot.put(DEEP_RECORD_KEY, Math.max(toInt(slot.get(DEEP_RECORD_KEY)), deepRecord));

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
			if (!"id".equals(key) && !"session".equals(key)
					&& !"progress".equals(key) && !"timestamp".equals(key)) {
				session.put(key, entry.getValue());
			}
		}
		// Inventário (rodada 22): quantidades persistidas na sessão.
		session.put("inventario", new HashMap<String, Object>(InventoryManager.serialize()));
				// Missões secundárias (rodada 22): progresso e concluídas persistidas.
				session.put("sideQuests", new HashMap<String, Object>(
					com.traduvertgames.quest.SideQuestManager.serialize()));
				session.put("dungeonsCompleted", new HashMap<String, Boolean>(
					com.traduvertgames.world.DungeonManager.serializeCompletions()));
			session.put("sideQuestsDone", new HashMap<String, Boolean>(
					com.traduvertgames.quest.SideQuestManager.getCompleted()));
			// Bônus escolhidos no level up pertencem à campanha atual e precisam
			// sobreviver à recriação do mapa e ao carregamento do slot.
			session.put("levelUpBonuses", new HashMap<String, Object>(
					LevelUpManager.serializeBonuses()));
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
		// A própria chave "session" (de uma gravação anterior do mesmo slot)
		// também sai do nível superior, pois o conteúdo dela é recriado
		// na sessão nova — ignorar as chaves estruturais evita apagar a
		// sessão recém-instalada.
		for (String key : session.keySet()) {
			if (!"session".equals(key)) {
				slot.remove(key);
			}
		}

		updateCampaign(root, game);

		root.put("activeSlot", activeSlot);
		writeBestRun(root);
		// Rodada 29 — metagame: créditos e upgrades permanentes do piloto
		// persistem no nível superior do save (independentes de slot, pois
		// são recompensas da conta e não do progresso de uma fase).
		Map<String, Object> metagame = new HashMap<String, Object>(
				com.traduvertgames.state.PilotUpgrades.serialize());
		root.put("metagame", metagame);
		// Rodada 31 — conteúdo pós-campanha: flags globais da conta no root.
		root.put(CAMPAIGN_COMPLETED_KEY, campaignCompleted);
		root.put(NEW_GAME_PLUS_KEY, newGamePlus);
		root.put("slots", slots);

		return writeRoot(root);
	}

	/**
	 * Salva automaticamente no slot ativo em checkpoints explícitos.
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

	/**
	 * Rodada 29 — recarrega os créditos e upgrades permanentes a partir do
	 * disco para que o menu principal exiba o saldo mesmo antes de um
	 * carregamento de slot (mesmo padrão do {@link #refreshBestRun()}).
	 */
	public static void refreshMetagame() {
		restoreMetagame(loadRoot());
	}

	/**
	 * Rodada 31 — recarrega as flags pós-campanha (campanha concluída e Nova
	 * campanha+) a partir do disco para que o menu principal reflita o estado
	 * gravado mesmo antes de um carregamento de slot (mesmo padrão do
	 * {@link #refreshBestRun()}).
	 */
	public static void refreshPostCampaignFlags() {
		restorePostCampaignFlags(loadRoot());
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

	/** Marca a campanha como concluída e grava no save. */
	public static void setCampaignCompleted(boolean value) {
		campaignCompleted = value;
		saveCurrentGame();
	}

	/** @return true se o jogador já concluiu a campanha completa. */
	public static boolean hasCampaignCompleted() {
		return campaignCompleted;
	}

	/** Ativa/desativa a Nova campanha+ e grava no save. */
	public static void setNewGamePlus(boolean value) {
		newGamePlus = value;
		saveCurrentGame();
	}

	/** @return true se a próxima campanha será uma Nova campanha+ com bônus. */
	public static boolean isNewGamePlus() {
		return newGamePlus;
	}

	/** Atualiza a seção de campanha global (fases concluídas e fase máxima). */
	private static void updateCampaign(Map<String, Object> root, Game game) {
		Map<String, Object> campaign = asMap(root.get("campaign"));
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
					List<Object> completed = asList(campaign.get("completedLevels"));
				if (completed == null) {
					completed = new ArrayList<Object>();
					campaign.put("completedLevels", completed);
				}
				if (!completed.contains(completedLevel)) {
					completed.add(completedLevel);
				}
				}
			}
			// asMap() devolve uma cópia validada; recolocar o mapa garante que as
		// mutações de progresso sejam efetivamente gravadas no root.
		root.put("campaign", campaign);
	}

	/** ---------- Recorde de profundidade (rodada 24b) ---------- */

	/** @return maior profundidade do modo infinito já alcançada no slot ativo. */
	public static int getDeepRecord() {
		return deepRecord;
	}

	/** @return recorde de profundidade gravado no slot informado (sem carregar). */
	public static int getSlotDeepRecord(int slotId) {
		if (slotId < 1 || slotId > SLOT_COUNT) {
			return 0;
		}
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot == null) {
			return 0;
		}
		int fromSlot = toInt(slot.get(DEEP_RECORD_KEY));
		int fromSession = toInt(getSession(slot).get(DEEP_RECORD_KEY));
		return Math.max(fromSlot, fromSession);
	}

	/** Registra um novo recorde de profundidade do slot ativo (e grava). */
	public static void setDeepRecord(int depth) {
		if (depth <= deepRecord) {
			return;
		}
		deepRecord = depth;
		saveCurrentGame();
	}

	/** Zera o recorde de profundidade em memória (novo jogo). */
	public static void resetDeepRecord() {
		deepRecord = 0;
	}

	/** Rodada 29 — metagame: restaura créditos e upgrades permanentes do root do save. */
	private static void restoreMetagame(Map<String, Object> root) {
		com.traduvertgames.state.PilotUpgrades.deserialize(root.get("metagame"));
	}

	/**
	 * Restaura as flags globais pós-campanha (rodada 31) a partir do root do
	 * save: campanha concluída e Nova campanha+ ativa.
	 */
	private static void restorePostCampaignFlags(Map<String, Object> root) {
		campaignCompleted = "true".equalsIgnoreCase(String.valueOf(root.get(CAMPAIGN_COMPLETED_KEY)));
		newGamePlus = "true".equalsIgnoreCase(String.valueOf(root.get(NEW_GAME_PLUS_KEY)));
	}

	private static void restoreDeepRecord(Map<String, Object> slot) {
		if (slot == null) {
			return;
		}
		int fromSlot = toInt(slot.get(DEEP_RECORD_KEY));
		int fromSession = toInt(getSession(slot).get(DEEP_RECORD_KEY));
		deepRecord = Math.max(fromSlot, fromSession);
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

		// Recorde de profundidade (rodada 24b): maior profundidade do infinito neste slot.
		restoreDeepRecord(slot);

		// Rodada 29 — metagame: créditos e upgrades permanentes restaurados
		// do nível superior do save; a aplicação dos stats acontece junto com
		// applyDifficultyToPlayerStats() mais abaixo.
		restoreMetagame(root);
		// Rodada 31 — flags globais pós-campanha restauradas do root do save.
		restorePostCampaignFlags(root);

					// Migração v1→v2: se o slot é flat (v1), a sessão é o próprio slot.
			Map<String, Object> session = getSession(slot);
			// Saves anteriores ao campo levelUpBonuses não devem herdar escolhas
			// da partida que estava aberta antes do carregamento.
			LevelUpManager.resetProgress();
			LevelUpManager.deserializeBonuses(session.get("levelUpBonuses"));

			// Valores salvos são aplicados DEPOIS do reload do mundo, pois o
		// restart redefine os máximos de vida/mana/escudo para a fase carregada.
		double savedLife = toDouble(session.get("vida"));
		double savedMana = toDouble(session.get("mana"));
		double savedWeapon = toDouble(session.get("arma"));
		double savedShield = toDouble(session.get("escudo"));
		int savedEnemies = toInt(session.get("inimigosMortos"));
		int savedLevelPlus = toInt(session.get("levelPlus"));
		int savedLevel = toInt(session.get("level"));
		// Rodada 25: o conjunto de inimigos abatidos — salvo antes do reload
		// do mundo (restoreObjectiveState abaixo) para o applyMapPixels pular
		// as posições registradas e não ressuscitar os mobs já derrotados.
		com.traduvertgames.main.EnemyKillTracker.setCurrentLevel(savedLevel);
		com.traduvertgames.main.EnemyKillTracker.deserialize(session.get("inimigosMortosSet"));
		int savedScore = toInt(session.get("pontuacao"));
		int savedHighScore = toInt(session.get("recorde"));
		int savedBestComboRecord = toInt(session.get("melhorCombo"));
		int savedBestComboSession = toInt(session.get("melhorComboSessao"));
		int savedWeaponOrdinal = toInt(session.get("armaAtual"));
		int savedWeaponMask = toInt(session.get("armasDesbloqueadas"));

			// Inventário (rodada 22): restaura as quantidades salvas da sessão.
			Map<String, Object> savedInventory = asMap(session.get("inventario"));
		if (savedInventory != null) {
			Map<String, Integer> inventory = new HashMap<String, Integer>();
			for (Map.Entry<String, Object> entry : savedInventory.entrySet()) {
				if (entry.getValue() instanceof Number) {
					inventory.put(entry.getKey(), ((Number) entry.getValue()).intValue());
				}
			}
			InventoryManager.deserialize(inventory);
		} else {
			InventoryManager.reset();
		}
			// Missões secundárias (rodada 22): progresso e concluídas restaurados.
			// As definições regionais precisam existir mesmo quando o save é carregado
			// antes de a superfície procedural ser reconstruída.
			com.traduvertgames.entities.RegionalNpcs.registerDefinitions();
			Map<String, Object> savedQuests = asMap(session.get("sideQuests"));
			Map<String, Object> savedDoneRaw = asMap(session.get("sideQuestsDone"));
							Map<String, Boolean> savedDone = toBooleanMap(savedDoneRaw);
				Map<String, Integer> questsSnapshot = new HashMap<String, Integer>();
				if (savedQuests != null) {
					for (Map.Entry<String, Object> entry : savedQuests.entrySet()) {
						if (entry.getValue() instanceof Number) {
							questsSnapshot.put(entry.getKey(),
									((Number) entry.getValue()).intValue());
						}
					}
				}
				com.traduvertgames.quest.SideQuestManager.deserialize(
						questsSnapshot,
						savedDone != null ? new HashMap<String, Boolean>(savedDone)
								: new HashMap<String, Boolean>());

				Map<String, Object> savedDungeonsRaw = asMap(session.get("dungeonsCompleted"));
				com.traduvertgames.world.DungeonManager.reset();
				com.traduvertgames.world.DungeonManager.deserializeCompletions(
					savedDungeonsRaw == null ? null : toBooleanMap(savedDungeonsRaw));

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
						// Rodada 25: a restauração de save recarrega a mesma fase;
						// o tracker de mortos (restaurado acima) não pode ser
						// zerado pelo restart — senão os mobs abatidos voltam.
						com.traduvertgames.main.Game.restorePhase = true;
						try {
							World.restartGame("level" + Math.min(Math.max(1, savedLevel), Game.MAX_LEVEL) + ".png");
						} finally {
							com.traduvertgames.main.Game.restorePhase = false;
						}
						com.traduvertgames.main.EnemyKillTracker.setCurrentLevel(savedLevel);
						com.traduvertgames.main.EnemyKillTracker.deserialize(session.get("inimigosMortosSet"));
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
			// Rodada 24: retomada da fase salva sem arremessar o piloto à
			// tela de seleção de arma inicial (também neste caminho).
			com.traduvertgames.main.Game.clearInitialWeaponSelect();
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
		// Rodada 24: o carregamento retoma a fase salva em vez de arremessar
		// o piloto de volta à tela de seleção de arma inicial (que parecia um
		// "tutorial" e travava o retorno ao combate após morrer).
		com.traduvertgames.main.Game.clearInitialWeaponSelect();
		activeSlot = slotId;
		restoreObjectiveState(slot, savedLevel);
		restoreNarrativeFlags(slot);
		restoreCompanion(session);
		return true;
	}

	/** Restaura o snapshot da melhor partida do root do save (migração v2→v3). */
	private static void restoreBestRun(Map<String, Object> root) {
		Map<String, Object> bestRun = asMap(root.get(BEST_RUN_KEY));
		if (bestRun == null) {
			return;
		}
		bestRunKills = toInt(bestRun.get(BEST_KILLS_KEY));
		bestRunTimeMs = bestRun.get(BEST_TIME_KEY) instanceof Number
				? ((Number) bestRun.get(BEST_TIME_KEY)).longValue() : 0;
		bestRunCombo = toInt(bestRun.get(BEST_COMBO_KEY));
		bestRunScore = toInt(bestRun.get(BEST_SCORE_KEY));
	}

	/** Restaura as flags de diálogos por NPC/fase do slot (migração v2→v3). */
	private static void restoreNpcDialogues(Map<String, Object> slot) {
		Map<String, Object> progress = asMap(slot.get("progress"));
		if (progress == null) {
			return;
		}
		Map<String, Object> dialogues = asMap(progress.get(NPC_DIALOGUES_KEY));
		if (dialogues == null) {
			return;
		}
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
	private static void restoreNarrativeFlags(Map<String, Object> slot) {
		Map<String, Object> progress = asMap(slot.get("progress"));
		if (progress == null) {
			return;
		}
		Object talked = progress.get("traitorTalked");
		Game.resetTraitorTalked();
		if ("true".equalsIgnoreCase(String.valueOf(talked))) {
			Game.setTraitorTalked(true);
		}
	}

	/** Sessão do slot: v2 usa a seção "session"; v1 é o próprio slot (flat). */
	private static Map<String, Object> getSession(Map<String, Object> slot) {
		Map<String, Object> session = asMap(slot.get("session"));
		return session != null ? session : slot;
	}

	/** Restaura o estado da missão da fase salva após o reload do mundo. */
	private static void restoreObjectiveState(Map<String, Object> slot, int savedLevel) {
		Map<String, Object> progress = asMap(slot.get("progress"));
		if (progress == null) {
			return;
		}
		Map<String, Object> objectiveState = asMap(progress.get("objectiveState"));
		if (objectiveState == null) {
			return;
		}
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

	/** Fase mais alta alcançada pela campanha no slot ativo: usada pela tela de
	 *  seleção de fases para destravar apenas o progresso real do jogador
	 *  (rodada 22b). Sem save válido, retorna 0. */
	public static int getHighestUnlockedLevel() {
		Map<String, Object> root = loadRoot();
		if (root == null) {
			return 0;
		}
		Map<String, Object> campaign = asMap(root.get("campaign"));
		if (campaign == null) {
			return 0;
		}
		return toInt(campaign.get("maxLevelReached"));
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
		Map<String, Object> progress = asMap(slot.get("progress"));
		if (progress == null) {
			return "";
		}
		Map<String, Object> objectiveState = asMap(progress.get("objectiveState"));
		if (objectiveState == null) {
			return "";
		}
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

	/** Converte um objeto JSON em mapa string-object sem cast unchecked. */
	private static Map<String, Object> asMap(Object raw) {
		if (!(raw instanceof Map<?, ?>)) {
			return null;
		}
		Map<?, ?> source = (Map<?, ?>) raw;
		Map<String, Object> result = new HashMap<String, Object>();
		for (Map.Entry<?, ?> entry : source.entrySet()) {
			if (entry.getKey() instanceof String) {
				result.put((String) entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

	/** Converte uma lista JSON em lista de objetos preservando valores brutos. */
	private static List<Object> asList(Object raw) {
		if (!(raw instanceof List<?>)) {
			return null;
		}
		return new ArrayList<Object>((List<?>) raw);
	}

	/** Converte flags JSON em booleanos apenas para chaves string válidas. */
	private static Map<String, Boolean> toBooleanMap(Map<String, Object> raw) {
		if (raw == null) {
			return null;
		}
		Map<String, Boolean> result = new HashMap<String, Boolean>();
		for (Map.Entry<String, Object> entry : raw.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Boolean) {
				result.put(entry.getKey(), (Boolean) value);
			} else if (value != null) {
				result.put(entry.getKey(), Boolean.valueOf(String.valueOf(value)));
			}
		}
		return result;
	}

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
			Map<String, Object> parsedRoot = asMap(parsed);
			if (parsedRoot != null) {
				return parsedRoot;
			}
		} catch (Exception ignored) {
			// Arquivo malformado (ex.: corrupção por queda de energia):
			// tratar como ausência de save em vez de derrubar o jogo.
			if (SAVE_FILE.exists()) {
				SAVE_FILE.delete();
			}
		}
		return emptyRoot();
	}

	private static List<Map<String, Object>> getSlots(Map<String, Object> root) {
		Object raw = root.get("slots");
		List<Map<String, Object>> slots = new ArrayList<Map<String, Object>>();
		if (raw instanceof List<?>) {
			for (Object entry : (List<?>) raw) {
				Map<String, Object> slot = asMap(entry);
				slots.add(slot != null ? slot : new HashMap<String, Object>());
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
			java.nio.file.Path target = SAVE_FILE.toPath();
			java.nio.file.Path temporary = SAVE_TMP.toPath();
			if (SAVE_FILE.exists()) {
				java.nio.file.Files.copy(target, SAVE_BACKUP.toPath(),
						java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
			try {
				java.nio.file.Files.move(temporary, target,
						java.nio.file.StandardCopyOption.ATOMIC_MOVE,
						java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
				java.nio.file.Files.move(temporary, target,
						java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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
