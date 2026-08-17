package com.traduvertgames.quest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.QuestBeacon;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;

/**
 * Objetivo do tipo "defender o ponto": o jogador deve ativar o beacon do
 * setor e mantê-lo defendido enquanto o canal de estabilização carrega.
 * Inimigos vivos próximos do beacon corrompem o canal (ele regredia); quando
 * nenhum invasor estiver na zona de defesa, o canal avança a cada frame.
 *
 * O beacon é registrado pela {@link QuestManager#registerBeacon(QuestBeacon)}
 * ao ser instanciado pelo mapa; a conclusão da defesa conclui o objetivo
 * (a fase avança para a loja com o fluxo normal da campanha).
 *
 * Progresso persistente: guarda a posição do beacon e o progresso do canal
 * no momento do save; ao recarregar, o beacon ainda presente no mapa é
 * re-rastreado pelo {@link #onBeaconSpawned(QuestBeacon)}.
 */
public class HoldObjective extends BaseObjective {

	/** Raio em pixels da zona de defesa do beacon. */
	public static final double DEFENSE_RADIUS = 90;
	/** Canal completo: objetivo concluído. */
	private static final int CHANNEL_MAX = 600;
	/** Avanço do canal por frame quando a zona está limpa. */
	private static final int CHANNEL_ADVANCE = 1;
	/** Retrocesso por frame por invasor dentro da zona. */
	private static final int CHANNEL_REGRESS_PER_INVADER = 2;

	private final Set<QuestBeacon> trackedBeacons = new HashSet<QuestBeacon>();
	private int channel = 0;
	private int invaders = 0;
	private boolean spawned = false;
	private String lastProgress = "";
	private boolean completionSoundPlayed = false;

	public HoldObjective() {
		this("Defender o ponto", "O setor precisa ser estabilizado. Ative o beacon do ponto estratégico e afaste os invasores da zona de defesa até o canal estabilizar.");
	}

	public HoldObjective(String title, String description) {
		super(title, description);
	}

	/** Posições dos beacons não ativados recuperadas de um save (rodada 22b). */
	private final List<int[]> restoredBeaconPositions = new ArrayList<int[]>();

	@Override
	public void onLevelStart() {
		trackedBeacons.clear();
		channel = 0;
		invaders = 0;
		spawned = false;
		completionSoundPlayed = false;
	}

	@Override
	public void onBeaconSpawned(QuestBeacon beacon) {
		if (!trackedBeacons.contains(beacon)) {
			trackedBeacons.add(beacon);
		}
		spawned = true;
	}

	/**
	 * Beacon programático da fase 2: o mapa da Câmara do Warbringer não tem
	 * beacon fixo, então o ponto de defesa é criado em um tile livre próximo
	 * do centro do mapa quando a fase carrega.
	 */
	@Override
	public void onLevelLoaded() {
		// Após um recarregamento de save, o beacon pode ter sido recriado pelo
		// deserializeState (onBeaconSpawned no construtor) ou guardado como
		// posição pendente (onLevelStart limpa o registro). Reconecta os beacons
		// restaurados ao objetivo em vez de criar um segundo beacon em cima deles.
		if (reconnectRestoredBeacons()) {
			return;
		}
		if (QuestManager.getCurrentLevel() != 2) {
			return;
		}
		// Ponto designado do beacon da fase 2. O mapa nem sempre tem um tile
		// de chão livre exatamente nessa posição (o pixel branco é parede),
		// então o beacon procura o chão válido mais próximo em raio crescente
		// — sem isso, a missão travava em "Localize o beacon do setor".
		int tx = 17;
		int ty = 11;
		if (!com.traduvertgames.world.World.isValidTile(tx, ty)
				|| com.traduvertgames.world.World.isWallTile(tx, ty)) {
			int[] floor = findNearestFloorTile(tx, ty);
			if (floor == null) {
				return;
			}
			tx = floor[0];
			ty = floor[1];
		}
		int px = tx * com.traduvertgames.world.World.TILE_SIZE;
		int py = ty * com.traduvertgames.world.World.TILE_SIZE;
		QuestBeacon beacon = new QuestBeacon(px, py, new java.awt.Color(0x4CAF50));
		Game.entities.add(beacon);
		onBeaconSpawned(beacon);
		com.traduvertgames.graficos.MissionBanner.show(
				"DEFESA NECESSÁRIA",
				"Ative o beacon e mantenha os invasores fora da zona de defesa",
				new java.awt.Color(255, 235, 59),
				java.awt.Color.WHITE,
				300);
	}

	/** @return progresso atual do canal (0 a 1). Usado pela HUD. */
	public double getChannelProgress() {
		return Math.min(1.0, channel / (double) CHANNEL_MAX);
	}

	/** @return true se o beacon foi criado e o canal está em andamento. */
	public boolean isActive() {
		return spawned && channel < CHANNEL_MAX;
	}

	/** @return true se há invasores na zona de defesa neste frame. */
	public boolean isUnderAttack() {
		return invaders > 0;
	}

	/** @return progresso do canal em texto de porcentagem (ex.: "73%"). */
	public String getPercentText() {
		return (int) Math.round(100.0 * channel / CHANNEL_MAX) + "%";
	}

	@Override
	public void update() {
		if (trackedBeacons.isEmpty() || !spawned) {
			return;
		}
		// Contar invasores vivos dentro da zona de defesa.
		int nearby = 0;
		for (int i = 0; i < Game.entities.size(); i++) {
			com.traduvertgames.entities.Entity e = Game.entities.get(i);
			if (!(e instanceof Enemy)) {
				continue;
			}
			// Inimigos removidos já saem das listas (destroySelf), então
			// quem estiver na lista está vivo (e ainda não passou pela
			// remoção de entidades do frame).
			Enemy enemy = (Enemy) e;
			for (QuestBeacon beacon : trackedBeacons) {
				double dx = enemy.getX() - beacon.getX();
				double dy = enemy.getY() - beacon.getY();
				if (dx * dx + dy * dy <= DEFENSE_RADIUS * DEFENSE_RADIUS) {
					nearby++;
				}
			}
		}
		invaders = nearby;
		if (invaders == 0) {
			channel = Math.min(CHANNEL_MAX, channel + CHANNEL_ADVANCE);
		} else {
			channel = Math.max(0, channel - CHANNEL_REGRESS_PER_INVADER * invaders);
		}
		if (channel >= CHANNEL_MAX && !completionSoundPlayed) {
			completionSoundPlayed = true;
			SoundManager.play(SoundManager.Event.LEVELUP);
		}
	}

	@Override
	public String getProgressText() {
		if (trackedBeacons.isEmpty() || !spawned) {
			return lastProgress.isEmpty() ? "Localize o beacon do setor" : lastProgress;
		}
		boolean activated = true;
		for (QuestBeacon beacon : trackedBeacons) {
			if (!beacon.isActivated()) {
				activated = false;
				break;
			}
		}
		String text;
		if (!activated) {
			// Rodada 22b: enquanto o beacon não for ativado, a dica orienta o
			// jogador a permanecer encostado nele (ele é pequeno e silencioso).
			text = "Permaneça junto ao beacon para ativar";
		} else if (invaders > 0) {
			text = "Defenda! " + invaders + " invasor" + (invaders == 1 ? "" : "es") + " na zona";
		} else {
			int percent = (int) Math.round(100.0 * channel / CHANNEL_MAX);
			text = "Canal: " + percent + "%";
		}
		lastProgress = text;
		return text;
	}

	@Override
	public boolean isComplete() {
		return spawned && channel >= CHANNEL_MAX;
	}

	@Override
	public String getTargetHint() {
		return isComplete() ? null : "Beacon do setor";
	}

	/** Cor fixa do beacon programático da fase 2 (para restaurar no save). */
	private static final java.awt.Color BEACON_COLOR = new java.awt.Color(0x4CAF50);

	@Override
	public String serializeState() {
		StringBuilder positions = new StringBuilder();
		for (QuestBeacon beacon : trackedBeacons) {
			if (!beacon.isActivated()) {
				if (positions.length() > 0) {
					positions.append('|');
				}
				positions.append(beacon.getX()).append(',').append(beacon.getY());
			}
		}
		return "SPAWNED=" + spawned + ";CHANNEL=" + channel
				+ ";INVADERS=" + invaders + ";BEACONS=" + positions.toString();
	}

	@Override
	public void deserializeState(String state) {
		if (state == null || state.isEmpty()) {
			return;
		}
		List<int[]> beaconPositions = new ArrayList<int[]>();
		for (String part : state.split(";")) {
			if (part.startsWith("SPAWNED=")) {
				spawned = "true".equalsIgnoreCase(part.substring("SPAWNED=".length()));
			} else if (part.startsWith("CHANNEL=")) {
				try {
					channel = Integer.parseInt(part.substring("CHANNEL=".length()));
				} catch (NumberFormatException ex) {
					channel = 0;
				}
			} else if (part.startsWith("INVADERS=")) {
				try {
					invaders = Integer.parseInt(part.substring("INVADERS=".length()));
				} catch (NumberFormatException ex) {
					invaders = 0;
				}
			} else if (part.startsWith("BEACONS=")) {
				// Rodada 22b: salva as posições dos beacons ainda não ativados —
				// no recarregamento do save o mundo é recriado e o beacon
				// físico não existe mais; ele é recriado aqui para a missão
				// não travar em "Localize o beacon do setor".
				String value = part.substring("BEACONS=".length());
				if (!value.isEmpty()) {
					for (String position : value.split("\\|")) {
						int comma = position.indexOf(',');
						if (comma < 0) {
							continue;
						}
						int x = parseIntSafe(position.substring(0, comma), 0);
						int y = parseIntSafe(position.substring(comma + 1), 0);
						beaconPositions.add(new int[] { x, y });
					}
				}
			}
		}
		// Guarda as posições dos beacons não ativados: os físicos são recriados
		// abaixo (o mundo é refeito no carregamento e o beacon antigo não existe
		// mais; no fluxo real do save, {@code prepareForLevel → restartGame →
		// deserializeState} o mapa já está carregado quando o estado chega).
		restoredBeaconPositions.addAll(beaconPositions);
		recreateRestoredBeaconsNow();
	}

	/**
	 * Recria imediatamente os beacons físicos pendentes do save. No fluxo real
	 * do carregamento ({@code prepareForLevel → restartGame → deserializeState})
	 * o mundo já existe quando o estado é restaurado — adiar a recriação para
	 * {@link #onLevelLoaded()} deixaria a missão travada em
	 * "Localize o beacon do setor" (o beacon só existiria em uma segunda
	 * recarga do mapa). Retornar false indica que não havia beacons pendentes.
	 */
	private boolean recreateRestoredBeaconsNow() {
		if (restoredBeaconPositions.isEmpty()) {
			return false;
		}
		for (int[] position : restoredBeaconPositions) {
			QuestBeacon beacon =
					new QuestBeacon(position[0], position[1], BEACON_COLOR);
			Game.entities.add(beacon);
			onBeaconSpawned(beacon);
		}
		restoredBeaconPositions.clear();
		return true;
	}

	/**
	 * Busca o tile de chão válido mais próximo de (tx, ty) em raio crescente
	 * (chão = tile existente no mundo que não é parede). Retorna null se nenhum
	 * chão for encontrado no mapa inteiro.
	 */
	private int[] findNearestFloorTile(int tx, int ty) {
		int radius = 1;
		int[] fallback = null;
		// Limite do raio de busca: o beacon não deve fugir do ponto
		// designado (o jogador espera a missão no centro da fase). Se não
		// houver chão livre perto, usa o chão mais próximo encontrado.
		int maxRadius = 12;
		while (radius <= maxRadius) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dx = -radius; dx <= radius; dx++) {
					int x = tx + dx;
					int y = ty + dy;
					if (!com.traduvertgames.world.World.isValidTile(x, y)
							|| com.traduvertgames.world.World.isWallTile(x, y)) {
						continue;
					}
					// Chão válido encontrado. Se nenhum invasor vivo estiver
					// dentro do raio de defesa do beacon, usa este tile — caso
					// contrário, os invasores bloqueariam a zona de defesa o
					// tempo todo e o canal nunca avançaria. Guarda o primeiro
					// chão como fallback caso todo o mapa tenha ameaça perto.
					if (fallback == null) {
						fallback = new int[] { x, y };
					}
					if (!isEnemyInsideDefenseRadius(x, y)) {
						return new int[] { x, y };
					}
				}
			}
			radius++;
		}
		return fallback;
	}

	/** @return true se algum inimigo vivo está dentro do raio de defesa de (tx, ty). */
	private static boolean isEnemyInsideDefenseRadius(int tx, int ty) {
		int px = tx * com.traduvertgames.world.World.TILE_SIZE
				+ com.traduvertgames.world.World.TILE_SIZE / 2;
		int py = ty * com.traduvertgames.world.World.TILE_SIZE
				+ com.traduvertgames.world.World.TILE_SIZE / 2;
		for (int i = 0; i < Game.enemies.size(); i++) {
			com.traduvertgames.entities.Enemy enemy = Game.enemies.get(i);
			double dx = enemy.getX() + 8 - px;
			double dy = enemy.getY() + 8 - py;
			if (dx * dx + dy * dy <= DEFENSE_RADIUS * DEFENSE_RADIUS) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Reconecta os beacons restaurados do save: devolve os que ainda estão no
	 * mundo ao objetivo e recria os que já se perderam. O retorno indica se o
	 * objetivo já foi reconectado (para não criar um beacon duplicado).
	 */
	private boolean reconnectRestoredBeacons() {
		// Re-registra os beacons físicos que sobreviveram à recarga do mundo.
		for (int i = 0; i < Game.entities.size(); i++) {
			if (Game.entities.get(i) instanceof QuestBeacon) {
				QuestBeacon existing = (QuestBeacon) Game.entities.get(i);
				if (!trackedBeacons.contains(existing)) {
					onBeaconSpawned(existing);
				}
			}
		}
		if (!restoredBeaconPositions.isEmpty()) {
			// Recria os beacons ainda não ativados na posição salva.
			for (int[] position : restoredBeaconPositions) {
				QuestBeacon beacon =
						new QuestBeacon(position[0], position[1], BEACON_COLOR);
				Game.entities.add(beacon);
				onBeaconSpawned(beacon);
			}
			restoredBeaconPositions.clear();
		}
		return spawned && !trackedBeacons.isEmpty();
	}

	private static int parseIntSafe(String text, int defaultValue) {
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}
}
