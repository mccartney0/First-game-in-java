package com.traduvertgames.quest;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;

/**
 * Objetivo do tipo "sobreviver": o jogador precisa resistir por um tempo
 * determinado contra as ondas do setor antes que o resgate/chegada ao
 * próximo estágio seja autorizada. Enquanto o timer não acaba, a fase
 * continua gerando inimigos normalmente; a conclusão do timer conclui
 * o objetivo e abre o fluxo normal de transição (loja e próxima fase).
 *
 * Progresso persistente: guarda os frames já sobrevividos; ao recarregar
 * o save na mesma fase, o timer continua de onde parou.
 */
public class SurviveObjective extends BaseObjective {

	/** Frames por segundo do loop do jogo. */
	private static final int FPS = 60;
	/** Duração padrão do desafio de sobrevivência (60 segundos). */
	private static final int DEFAULT_DURATION_SECONDS = 60;

	private final int durationFrames;
	private int survivedFrames;
	private boolean started = false;
	private boolean completionSoundPlayed = false;

	public SurviveObjective() {
		this("Resistir às ondas",
				"O reforço só chega em breve. Sobreviva às ondas do setor até a evacuação ser autorizada.",
				DEFAULT_DURATION_SECONDS);
	}

	public SurviveObjective(String title, String description, int durationSeconds) {
		super(title, description);
		this.durationFrames = Math.max(1, durationSeconds * FPS);
	}

	/** @return segundos restantes de sobrevivência. Usado pela HUD. */
	public int getRemainingSeconds() {
		int remaining = durationFrames - survivedFrames;
		return Math.max(0, remaining / FPS);
	}

	/** @return progresso do timer (0 a 1). Usado pela HUD. */
	public double getTimeProgress() {
		return Math.min(1.0, survivedFrames / (double) durationFrames);
	}

	@Override
	public void onLevelStart() {
		survivedFrames = 0;
		started = false;
		completionSoundPlayed = false;
	}

	@Override
	public void update() {
		// O timer só avança durante o jogo normal (estado NORMAL).
		if (!"NORMAL".equals(Game.gameState)) {
			return;
		}
		started = true;
		if (survivedFrames < durationFrames) {
			survivedFrames++;
		}
		if (survivedFrames >= durationFrames && !completionSoundPlayed) {
			completionSoundPlayed = true;
			SoundManager.play(SoundManager.Event.LEVELUP);
		}
	}

	@Override
	public String getProgressText() {
		if (!started) {
			return "Prepare-se para as ondas";
		}
		if (survivedFrames >= durationFrames) {
			return "Evacuação autorizada!";
		}
		return "Resistir: " + getRemainingSeconds() + "s";
	}

	@Override
	public boolean isComplete() {
		return survivedFrames >= durationFrames;
	}

	@Override
	public String serializeState() {
		return "FRAMES=" + survivedFrames + ";DURATION=" + durationFrames;
	}

	@Override
	public void deserializeState(String state) {
		if (state == null || state.isEmpty()) {
			return;
		}
		for (String part : state.split(";")) {
			if (part.startsWith("FRAMES=")) {
				try {
					survivedFrames = Integer.parseInt(part.substring("FRAMES=".length()));
				} catch (NumberFormatException ex) {
					survivedFrames = 0;
				}
			} else if (part.startsWith("DURATION=")) {
				try {
					int duration = Integer.parseInt(part.substring("DURATION=".length()));
					if (duration > 0) {
						// A duração salva deve coincidir com a configurada;
						// se o mapa mudou, usa-se a configuração atual.
						if (duration == durationFrames) {
							started = survivedFrames > 0;
						} else {
							survivedFrames = 0;
							started = false;
						}
					}
				} catch (NumberFormatException ex) {
					// formato desconhecido: recomeça do zero.
				}
			}
		}
	}
}
