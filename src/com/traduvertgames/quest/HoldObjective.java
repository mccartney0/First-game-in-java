package com.traduvertgames.quest;

import java.util.HashSet;
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
		if (spawned || QuestManager.getCurrentLevel() != 2) {
			return;
		}
		int tx = 17;
		int ty = 11;
		if (!com.traduvertgames.world.World.isValidTile(tx, ty)
				|| com.traduvertgames.world.World.isWallTile(tx, ty)) {
			return;
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
			// quem estiver na lista está vivo.
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
		String text;
		if (invaders > 0) {
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

	@Override
	public String serializeState() {
		return "SPAWNED=" + spawned + ";CHANNEL=" + channel;
	}

	@Override
	public void deserializeState(String state) {
		if (state == null || state.isEmpty()) {
			return;
		}
		for (String part : state.split(";")) {
			if (part.startsWith("SPAWNED=")) {
				spawned = "true".equalsIgnoreCase(part.substring("SPAWNED=".length()));
			} else if (part.startsWith("CHANNEL=")) {
				try {
					channel = Integer.parseInt(part.substring("CHANNEL=".length()));
				} catch (NumberFormatException ex) {
					channel = 0;
				}
			}
		}
	}
}
