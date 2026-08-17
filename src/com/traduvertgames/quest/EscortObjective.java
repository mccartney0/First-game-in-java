package com.traduvertgames.quest;

import java.util.ArrayList;
import java.util.List;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.EscortNpc;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;

/**
 * Objetivo do tipo "escoltar": o jogador deve proteger o escoltado no
 * caminho até o ponto de fuga do setor. O escoltado caminha sozinho e
 * congela quando inimigos se aproximam; cada acerto inimigo consome um
 * ponto de vida — se o escoltado cair, a escolta falha e a fase precisa
 * ser retomada (o jogador é avisado pelo banner); se o escoltado chegar
 * ao destino, o objetivo conclui e a fase avança normalmente.
 *
 * Inimigos vivos dentro do raio de ameaça atingem o escoltado a cada
 * intervalo, simulando ataques no caminho.
 *
 * Progresso persistente: guarda o estado do escoltado ("VIVO" ou
 * "FALHA"); ao recarregar, se o escoltado ainda estiver no mapa ele é
 * re-rastreado pelo {@link #registerEscort(EscortNpc)}; se não estiver
 * (morte), a fase recomeça do zero.
 */
public class EscortObjective extends BaseObjective {

	/** Raio em pixels no qual inimigos ameaçam o escoltado. */
	public static final double THREAT_RADIUS = 70;
	/** Intervalo em frames entre os ataques de inimigos na zona de ameaça. */
	private static final int ATTACK_INTERVAL_FRAMES = 60;
	/** Distância em pixels para registrar ataque já contabilizado. */
	private static final double HIT_DIST_SQ = 20 * 20;

	private final List<EscortNpc> tracked = new ArrayList<EscortNpc>();
	private int hitTimer = 0;
	private boolean spawned = false;
	private boolean failed = false;

	public EscortObjective() {
		this("Escoltar até o ponto de fuga",
				"O informante precisa chegar vivo ao ponto de fuga. Proteja-o dos ataques no caminho.");
	}

	public EscortObjective(String title, String description) {
		super(title, description);
	}

	@Override
	public void onLevelStart() {
		tracked.clear();
		hitTimer = 0;
		spawned = false;
		failed = false;
	}

	/** O escoltado foi criado no mapa (real ou programático): passa a ser rastreado. */
	public void onEscortSpawned(EscortNpc npc) {
		if (!tracked.contains(npc)) {
			tracked.add(npc);
		}
		spawned = true;
	}

	/** O escoltado morreu: a escolta falha, o banner avisa e a fase recomeça. */
	public void onEscortFailed(EscortNpc npc) {
		if (tracked.remove(npc)) {
			failed = true;
			com.traduvertgames.graficos.MissionBanner.show(
					"ESCOLTA COMPROMETIDA",
					"O informante não resistiu. A fase será reiniciada.",
					new java.awt.Color(244, 67, 54),
					java.awt.Color.WHITE,
					240);
			QuestManager.restartCurrentLevel();
		}
	}

	/** O escoltado chegou ao ponto de fuga: som de etapa concluída. */
	public void onEscortArrived(EscortNpc npc) {
		SoundManager.play(SoundManager.Event.LEVELUP);
	}

	@Override
	public void onQuestItemSpawned(com.traduvertgames.entities.QuestItem item) {
		// nada — a fase não usa coleta.
	}

	/**
	 * Escoltado programático da fase 8: o Núcleo Central não tem um NPC de
	 * escolta fixo no mapa, então o informante é criado em um tile livre
	 * próximo ao jogador e caminha até o ponto de fuga do setor.
	 */
	@Override
	public void onLevelLoaded() {
		if (spawned || QuestManager.getCurrentLevel() != 8) {
			return;
		}
		int tx = 8;
		int ty = 10;
		if (!com.traduvertgames.world.World.isValidTile(tx, ty)
				|| com.traduvertgames.world.World.isWallTile(tx, ty)) {
			return;
		}
		int px = tx * com.traduvertgames.world.World.TILE_SIZE;
		int py = ty * com.traduvertgames.world.World.TILE_SIZE;
		// Ponto de fuga: canto inferior direito do mapa da fase 8.
		int ex = 42 * com.traduvertgames.world.World.TILE_SIZE;
		int ey = 26 * com.traduvertgames.world.World.TILE_SIZE;
		if (!com.traduvertgames.world.World.isValidTile(ex / 16, ey / 16)
				|| com.traduvertgames.world.World.isWallTile(ex / 16, ey / 16)) {
			return;
		}
		EscortNpc npc = new EscortNpc(px, py, ex, ey);
		Game.entities.add(npc);
		com.traduvertgames.graficos.MissionBanner.show(
				"INFORMANTE LOCALIZADO",
				"O informante conhece o caminho do núcleo. Proteja-o até o ponto de fuga",
				new java.awt.Color(40, 120, 220),
				java.awt.Color.WHITE,
				300);
	}

	/** @return progresso da jornada (0 a 1). Usado pela HUD. */
	public double getJourneyProgress() {
		if (tracked.isEmpty()) {
			return 0;
		}
		EscortNpc npc = tracked.get(0);
		double traveled = npc.distanceTo(npc.escapeTargetX(), npc.escapeTargetY());
		double total = npc.distanceFromSpawn();
		if (total < 1) {
			return 0;
		}
		return Math.min(1.0, 1.0 - traveled / total);
	}

	@Override
	public void update() {
		if (tracked.isEmpty() || !spawned || failed) {
			return;
		}
		EscortNpc escort = tracked.get(0);
		if (escort.hasArrived()) {
			return;
		}
		hitTimer++;
		if (hitTimer >= ATTACK_INTERVAL_FRAMES) {
			hitTimer = 0;
			// Inimigos vivos dentro do raio de ameaça atacam o escoltado.
			for (int i = 0; i < Game.entities.size(); i++) {
				Entity e = Game.entities.get(i);
				if (!(e instanceof Enemy)) {
					continue;
				}
				double dx = e.getX() - escort.getX();
				double dy = e.getY() - escort.getY();
				if (dx * dx + dy * dy <= THREAT_RADIUS * THREAT_RADIUS) {
					escort.takeHit();
					if (failed) {
						break;
					}
				}
			}
		}
	}

	@Override
	public String getProgressText() {
		if (tracked.isEmpty() || !spawned) {
			return "Localize o informante";
		}
		EscortNpc escort = tracked.get(0);
		if (escort.hasArrived()) {
			return "Informante em segurança!";
		}
		if (failed) {
			return "Escolta comprometida";
		}
		int hp = escort.getHp();
		int percent = (int) Math.round(100.0 * getJourneyProgress());
		return "Jornada: " + percent + "% (vida " + hp + ")";
	}

	@Override
	public boolean isComplete() {
		return spawned && !tracked.isEmpty() && tracked.get(0).hasArrived();
	}

	@Override
	public String getTargetHint() {
		if (failed || (tracked.isEmpty() || tracked.get(0).hasArrived())) {
			return null;
		}
		return "Informante";
	}

	@Override
	public String serializeState() {
		if (failed) {
			return "FAILED";
		}
		if (isComplete()) {
			return "ARRIVED";
		}
		return spawned ? "ALIVE" : "UNKNOWN";
	}

	@Override
	public void deserializeState(String state) {
		if ("FAILED".equalsIgnoreCase(state)) {
			failed = true;
		}
	}
}
