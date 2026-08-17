package com.traduvertgames.main;

import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

/**
 * Trilha sonora adaptativa (rodada 22): cada zona do jogo tem um tema musical
 * próprio, com transição suave (crossfade) entre zonas e volume independente
 * dos efeitos sonoros ({@link OptionsConfig#getMusicVolume()}).
 *
 * Zonas: FOREST (fases 1-2, calma), TENSION (fases 3-5, base inimiga),
 * BOSS (fases 6-8, chefe), ARENA (modo infinito/sobrevivência).
 */
public final class MusicManager {

	public enum Zone {
		FOREST("/sounds/music_forest.wav"),
		TENSION("/sounds/music_tension.wav"),
		BOSS("/sounds/music_boss.wav"),
		ARENA("/sounds/music_arena.wav");

		private final String path;

		Zone(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}

		/** Zona do tema para a fase informada (campanha). */
		public static Zone forLevel(int level) {
			if (level >= 6) {
				return BOSS;
			}
			if (level >= 3) {
				return TENSION;
			}
			return FOREST;
		}
	}

	private static final int CROSSFADE_FRAMES = 120; // ~2 s a 60 fps

	private static Clip current = null;
	private static Zone currentZone = null;

	/** Clip que está entrando durante o crossfade. */
	private static Clip fadingIn = null;
	private static int crossfadeRemaining = 0;

	private MusicManager() {
	}

	/**
	 * Troca para o tema da zona informada (com crossfade suave). Ignora chamadas
	 * redundantes (mesma zona) e respeita a preferência de música do usuário.
	 */
	public static void setZone(Zone zone) {
		if (zone == null || zone == currentZone) {
			return;
		}
		currentZone = zone;
		if (!OptionsConfig.isMusicEnabled()) {
			return;
		}
		Clip next = load(zone.getPath());
		if (next == null) {
			return;
		}
		// Crossfade: o clip antigo permanece tocando até o novo atingir volume pleno.
		fadingIn = next;
		crossfadeRemaining = CROSSFADE_FRAMES;
		startAt(fadingIn, -60.0f); // entra em silêncio
	}

	/**
	 * Chamado a cada frame pelo loop principal para conduzir o crossfade.
	 */
	public static void update() {
		if (fadingIn == null) {
			return;
		}
		// Volume atual do clip antigo decresce; o novo cresce, em dB (escala log).
		float gainDb = OptionsConfig.getMusicVolume();
		float floor = -60.0f;
		if (crossfadeRemaining > 0) {
			float progress = 1.0f - ((float) crossfadeRemaining / CROSSFADE_FRAMES);
			float oldGain = gainDb * (1.0f - progress);
			if (current != null && current.isRunning()) {
				applyGain(current, Math.max(floor, oldGain));
			}
			if (fadingIn != null) {
				applyGain(fadingIn, Math.max(floor, gainDb * progress));
				if (!fadingIn.isRunning()) {
					fadingIn.setFramePosition(0);
					fadingIn.loop(Clip.LOOP_CONTINUOUSLY);
				}
			}
			crossfadeRemaining--;
		} else {
			// Crossfade concluído: parar o clip antigo e manter o novo.
			if (current != null && current != fadingIn) {
				stopQuiet(current);
			}
			current = fadingIn;
			fadingIn = null;
			if (current != null && current.isRunning()) {
				applyGain(current, gainDb);
			}
		}
	}

	/** Reaplica o volume preferido do usuário (após mudança nas opções). */
	public static void applyMusicPreference() {
		if (current != null && current.isRunning()) {
			applyGain(current, OptionsConfig.getMusicVolume());
		}
		if (fadingIn != null) {
			applyGain(fadingIn, OptionsConfig.getMusicVolume());
		}
	}

	/** Pausa o tema (menu, pausa, game over) sem perder a zona atual. */
	public static void pause() {
		if (current != null && current.isRunning()) {
			current.stop();
		}
		if (fadingIn != null && fadingIn.isRunning()) {
			fadingIn.stop();
		}
		fadingIn = null;
	}

	/** Retoma o tema da zona atual (saída de pausa/menu). */
	public static void resume() {
		if (!OptionsConfig.isMusicEnabled() || currentZone == null) {
			return;
		}
		if (current != null) {
			current.setFramePosition(0);
			current.loop(Clip.LOOP_CONTINUOUSLY);
			applyGain(current, OptionsConfig.getMusicVolume());
		} else {
			setZone(currentZone);
		}
	}

	/** Descarta os clips (troca de save, fim de sessão). */
	public static void unload() {
		if (current != null) {
			stopQuiet(current);
			current = null;
		}
		if (fadingIn != null) {
			stopQuiet(fadingIn);
			fadingIn = null;
		}
		currentZone = null;
		crossfadeRemaining = 0;
	}

	private static Clip load(String path) {
		try (InputStream in = MusicManager.class.getResourceAsStream(path)) {
			if (in == null) {
				return null;
			}
			AudioInputStream ais = AudioSystem.getAudioInputStream(in);
			Clip clip = AudioSystem.getClip();
			clip.open(ais);
			clip.loop(Clip.LOOP_CONTINUOUSLY);
			// O loop do clip se ajusta automaticamente ao volume do crossfade.
			return clip;
		} catch (Exception ex) {
			return null;
		}
	}

	private static void startAt(Clip clip, float gainDb) {
		applyGain(clip, gainDb);
		if (!clip.isRunning()) {
			clip.start();
		}
	}

	private static void applyGain(Clip clip, float gainDb) {
		if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
			return;
		}
		FloatControl vol = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		float clamped = Math.max(vol.getMinimum(), Math.min(vol.getMaximum(), gainDb));
		vol.setValue(clamped);
	}

	private static void stopQuiet(Clip clip) {
		try {
			if (clip.isRunning()) {
				clip.stop();
			}
			clip.setFramePosition(0);
		} catch (Exception ignored) {
			// Clip já fechado: nada a fazer.
		}
	}
}
