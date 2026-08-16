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
 * Estrutura:
 * {
 *   "activeSlot": 1,
 *   "slots": [
 *     {
 *       "id": 1,
 *       "vida": 100, "mana": 50, ...,
 *       "level": 3, "levelPlus": 1,
 *       "pontuacao": 12000, "recorde": 50000,
 *       "energiaArma_BLASTER": 250, ...
 *     },
 *     ...
 *   ]
 * }
 */
public final class SaveManager {

	/** Arquivo único que guarda todos os slots. */
	public static final File SAVE_FILE = new File("saves.json");

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

		root.put("activeSlot", activeSlot);
		root.put("slots", slots);

		return writeRoot(root);
	}

	/**
	 * Salva automaticamente no slot ativo (usado ao morrer ou trocar de fase).
	 */
	public static boolean saveAutoSave() {
		Game game = Game.getInstance();
		if (game == null) {
			return false;
		}
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findOrCreateSlot(slots, activeSlot);

		slot.put("vida", clampDouble(Player.life));
		slot.put("mana", clampDouble(Player.mana));
		slot.put("arma", clampDouble(Player.weapon));
		slot.put("escudo", clampDouble(Player.shield));
		slot.put("inimigosMortos", Enemy.enemies);
		slot.put("levelPlus", game.getLevelPlus());
		slot.put("level", game.getCurrentLevel());
		slot.put("pontuacao", Game.getScore());
		slot.put("recorde", Game.getHighScore());
		slot.put("melhorCombo", Game.getBestComboRecord());
		slot.put("melhorComboSessao", Game.getBestComboThisRun());
		slot.put("armaAtual", Player.getCurrentWeaponOrdinal());
		slot.put("armasDesbloqueadas", Player.getWeaponUnlockMask());

		for (WeaponType type : WeaponType.values()) {
			slot.put("energiaArma_" + type.name(), clampDouble(Player.getStoredEnergyForType(type)));
		}

		root.put("activeSlot", activeSlot);
		root.put("slots", slots);

		return writeRoot(root);
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

		// Valores salvos são aplicados DEPOIS do reload do mundo, pois o
		// restart redefine os máximos de vida/mana/escudo para a fase carregada.
		double savedLife = toDouble(slot.get("vida"));
		double savedMana = toDouble(slot.get("mana"));
		double savedWeapon = toDouble(slot.get("arma"));
		double savedShield = toDouble(slot.get("escudo"));
		int savedEnemies = toInt(slot.get("inimigosMortos"));
		int savedLevelPlus = toInt(slot.get("levelPlus"));
		int savedLevel = toInt(slot.get("level"));
		int savedScore = toInt(slot.get("pontuacao"));
		int savedHighScore = toInt(slot.get("recorde"));
		int savedBestComboRecord = toInt(slot.get("melhorCombo"));
		int savedBestComboSession = toInt(slot.get("melhorComboSessao"));
		int savedWeaponOrdinal = toInt(slot.get("armaAtual"));
		int savedWeaponMask = toInt(slot.get("armasDesbloqueadas"));

		Player.loadCurrentWeaponFromSave(savedWeaponOrdinal);
		Player.loadUnlockedWeaponsFromSave(savedWeaponMask);
		for (WeaponType type : WeaponType.values()) {
			Object raw = slot.get("energiaArma_" + type.name());
			if (raw != null) {
				Player.loadWeaponEnergyFromSave(type, toDouble(raw));
			}
		}

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
			Game.gameState = "NORMAL";
			Menu.pause = false;
			activeSlot = slotId;
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
		return true;
	}

	/** Verifica se existe ao menos um slot salvo. */
	public static boolean hasAnySave() {
		if (!SAVE_FILE.exists()) {
			return false;
		}
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		for (Map<String, Object> slot : slots) {
			if (slot.containsKey("vida") || slot.containsKey("level")) {
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
		return slot != null && (slot.containsKey("vida") || slot.containsKey("level"));
	}

	/** Retorna a fase salva em um slot, ou -1 se vazio. */
	public static int getSlotLevel(int slotId) {
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot == null) {
			return -1;
		}
		Object level = slot.get("level");
		return level instanceof Number ? ((Number) level).intValue() : -1;
	}

	/** Retorna a pontuação salva em um slot, ou -1 se vazio. */
	public static int getSlotScore(int slotId) {
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot == null) {
			return -1;
		}
		Object score = slot.get("pontuacao");
		return score instanceof Number ? ((Number) score).intValue() : -1;
	}

	/** Apaga o conteúdo de um slot específico. */
	public static boolean clearSlot(int slotId) {
		Map<String, Object> root = loadRoot();
		List<Map<String, Object>> slots = getSlots(root);
		Map<String, Object> slot = findSlot(slots, slotId);
		if (slot != null) {
			slot.clear();
			slot.put("id", slotId);
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
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(SAVE_FILE));
			writer.write(JsonWriter.write(root));
			writer.flush();
			writer.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
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
