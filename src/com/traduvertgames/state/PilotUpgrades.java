package com.traduvertgames.state;

import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;

/**
 * Rodada 29 — Metagame: melhorias permanentes do piloto.
 *
 * Créditos são ganhos ao longo da campanha e do modo infinito e persistem
 * entre sessões via {@code SaveManager}. Os upgrades comprados com eles
 * alteram os stats base do piloto (vida máxima, regeneração, escudo inicial
 * e mana inicial) e são aplicados automaticamente após cada reset de
 * estado, por meio de {@link #applyToPlayer()}.
 */
public final class PilotUpgrades {

	/** Identificadores dos upgrades permanentes do piloto. */
	public enum Upgrade {
		/** +25 vida máxima por nível (máx. 8 níveis). */
		CELLS,
		/** +1 vida por segundo de regeneração (máx. 5 níveis). */
		REGEN,
		/** +% de escudo inicial sobre o máximo (máx. 5 níveis). */
		SHIELD,
		/** +% de mana/munição inicial sobre o máximo (máx. 5 níveis). */
		AMMO
	}

	// ---------- Custos e limites ----------
	private static final Map<Upgrade, Integer> BASE_COSTS = new HashMap<Upgrade, Integer>();
	private static final Map<Upgrade, Integer> COST_INCREMENT = new HashMap<Upgrade, Integer>();
	private static final Map<Upgrade, Integer> MAX_LEVELS = new HashMap<Upgrade, Integer>();

	static {
		BASE_COSTS.put(Upgrade.CELLS, 100);
		BASE_COSTS.put(Upgrade.REGEN, 200);
		BASE_COSTS.put(Upgrade.SHIELD, 150);
		BASE_COSTS.put(Upgrade.AMMO, 120);

		COST_INCREMENT.put(Upgrade.CELLS, 50);
		COST_INCREMENT.put(Upgrade.REGEN, 100);
		COST_INCREMENT.put(Upgrade.SHIELD, 100);
		COST_INCREMENT.put(Upgrade.AMMO, 100);

		MAX_LEVELS.put(Upgrade.CELLS, 8);
		MAX_LEVELS.put(Upgrade.REGEN, 5);
		MAX_LEVELS.put(Upgrade.SHIELD, 5);
		MAX_LEVELS.put(Upgrade.AMMO, 5);
	}

	// ---------- Estado persistente de conta ----------
	private static int credits = 0;
	private static final Map<Upgrade, Integer> levels = new HashMap<Upgrade, Integer>();

	static {
		for (Upgrade u : Upgrade.values()) {
			levels.put(u, 0);
		}
	}

	private PilotUpgrades() {
	}

	// ---------- Créditos ----------
	/** @return saldo atual de créditos. */
	public static int getCredits() {
		return credits;
	}

	/** Adiciona créditos (não negativa: protege contra bugs de recompensa). */
	public static void addCredits(int amount) {
		if (amount <= 0) {
			return;
		}
		credits += amount;
	}

	/** Deduz créditos na compra de upgrade (garantido >= 0). */
	public static void spendCredits(int amount) {
		credits = Math.max(0, credits - amount);
	}

	/** Zera os créditos — usado apenas por testes headless. */
	public static void resetCredits() {
		credits = 0;
		for (Upgrade u : Upgrade.values()) {
			levels.put(u, 0);
		}
	}

	// ---------- Upgrades ----------
	/** @return nível atual do upgrade (0 = não comprado). */
	public static int getLevel(Upgrade upgrade) {
		return levels.get(upgrade);
	}

	/** @return custo do próximo nível do upgrade. */
	public static int getNextCost(Upgrade upgrade) {
		int level = getLevel(upgrade);
		if (level >= getMaxLevel(upgrade)) {
			return -1; // já no nível máximo
		}
		return BASE_COSTS.get(upgrade) + level * COST_INCREMENT.get(upgrade);
	}

	/** @return true se o upgrade pode ser comprado agora. */
	public static boolean canAfford(Upgrade upgrade) {
		int cost = getNextCost(upgrade);
		return cost > 0 && credits >= cost;
	}

	/** @return nível máximo do upgrade. */
	public static int getMaxLevel(Upgrade upgrade) {
		return MAX_LEVELS.get(upgrade);
	}

	/**
	 * Compra um nível do upgrade: deduz os créditos e aplica os stats
	 * imediatamente ao piloto (o applyToPlayer() mantém o estado coerente
	 * após resets de fase/sessão).
	 */
	public static boolean buy(Upgrade upgrade) {
		int cost = getNextCost(upgrade);
		if (cost < 0 || credits < cost) {
			return false;
		}
		credits -= cost;
		levels.put(upgrade, levels.get(upgrade) + 1);
		// A compra deve ser perceptível imediatamente quando existe um piloto
		// ativo. O recálculo completo e idempotente dos máximos acontece em
		// Game.applyDifficultyScalingForCurrentLevel() nas transições/reloads.
		if (upgrade == Upgrade.CELLS && Player.life > 0 && Player.maxLife > 0) {
			int bonus = cellsBonusPerLevel();
			double ratio = Player.life / Player.maxLife;
			Player.maxLife += bonus;
			Player.life = Math.max(1, Math.min(Player.maxLife,
					Math.round(Player.maxLife * Math.max(0.0, ratio))));
		}
		applyToPlayer();
		return true;
	}

	// ---------- Aplicação de stats ----------
	/** Efeito por nível do upgrade de vida máxima. */
	public static int cellsBonusPerLevel() {
		return 25;
	}

	/** Vida adicional máxima devida aos upgrades. */
	public static int cellsBonus() {
		return getLevel(Upgrade.CELLS) * cellsBonusPerLevel();
	}

	/** Escudo inicial adicional (fração do máximo) devida aos upgrades. */
	public static double shieldStartFraction() {
		return 0.20 + 0.10 * getLevel(Upgrade.SHIELD);
	}

	/** Mana/munição inicial adicional (fração do máximo) devida aos upgrades. */
	public static double ammoStartFraction() {
		return 0.15 + 0.10 * getLevel(Upgrade.AMMO);
	}

	/**
	 * Aplica os efeitos de recursos iniciais do piloto atual. Os bônus de
	 * máximos são calculados de forma idempotente em
	 * {@code Game.applyDifficultyScalingForCurrentLevel()}; manter a alteração
	 * de {@code maxLife} fora deste método evita que chamadas repetidas
	 * acumulem o mesmo upgrade.
	 */
	public static void applyToPlayer() {
		if (Player.life == 0 && Player.maxLife > 0) {
			// Piloto abatido: não ressuscitar apenas por upgrades.
			return;
		}
		if (getLevel(Upgrade.SHIELD) > 0 && Player.maxShield > 0) {
			Player.shield = Math.min(Player.maxShield, Player.shield
					+ Math.round(Player.maxShield * shieldStartFraction()));
		}
		if (getLevel(Upgrade.AMMO) > 0 && Player.maxMana > 0) {
			Player.mana = Math.min(Player.maxMana, Player.mana
					+ Math.round(Player.maxMana * ammoStartFraction()));
		}
	}

	/**
	 * Regeneração passiva devida aos upgrades, respeitando o máximo. O loop do
	 * jogo chama este método a aproximadamente 60 Hz; portanto, cada nível
	 * recupera cerca de 1 ponto por segundo, e não 30 pontos por segundo.
	 */
	public static void regenTick() {
		int level = getLevel(Upgrade.REGEN);
		if (level <= 0 || Player.life <= 0) {
			return;
		}
		Player.life = Math.min(Player.maxLife, Player.life + level / 60.0);
	}

	// ---------- Persistência ----------
	private static final String CREDITS_KEY = "credits";
	private static final String UPGRADES_KEY = "pilotUpgrades";

	public static Map<String, Object> serialize() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(CREDITS_KEY, credits);
		Map<String, Object> lvl = new HashMap<String, Object>();
		for (Upgrade u : Upgrade.values()) {
			lvl.put(u.name(), levels.get(u));
		}
		map.put(UPGRADES_KEY, lvl);
		return map;
	}

	@SuppressWarnings("unchecked")
	public static void deserialize(Object raw) {
		if (raw instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) raw;
			credits = toInt(map.get(CREDITS_KEY));
			Object lvlRaw = map.get(UPGRADES_KEY);
			if (lvlRaw instanceof Map) {
				Map<String, Object> lvl = (Map<String, Object>) lvlRaw;
				for (Upgrade u : Upgrade.values()) {
					levels.put(u, Math.max(0, Math.min(toInt(lvl.get(u.name())), getMaxLevel(u))));
				}
			}
		}
	}

	private static int toInt(Object value) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return 0;
	}

	/** @return representação legível dos upgrades (para banners/HUD). */
	public static String summary() {
		StringBuilder sb = new StringBuilder();
		for (Upgrade u : Upgrade.values()) {
			int level = getLevel(u);
			if (level > 0) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(u.name().toLowerCase()).append(" ").append(level);
			}
		}
		return sb.length() == 0 ? "nenhum upgrade" : sb.toString();
	}

	// ---------- Referências para UI ----------
	/** @return labels legíveis dos upgrades (mesma ordem do enum). */
	public static String[] labels() {
		return new String[] { "celulas vitais", "regeneracao", "escudo inicial", "municao inicial" };
	}
}
