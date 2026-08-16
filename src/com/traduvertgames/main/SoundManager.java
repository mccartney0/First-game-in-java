package com.traduvertgames.main;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;

import com.traduvertgames.main.OptionsConfig;

/**
 * Gerenciador de efeitos sonoros do jogo. Os clips são carregados sob demanda
 * de {@code /sounds/*.wav} e reproduzidos em instâncias independentes
 * (permite sobreposição de sons rápidos, como tiros).
 */
public final class SoundManager {

	public enum Event {
		SHOT,
		LASER,
		HIT,
		DAMAGE,
		KILL,
		TELEPORT,
		PICKUP,
		LEVELUP,
		BOSS_ALERT,
		BOSS_DEFEAT,
		SHOP,
		WAVE,
		TUTORIAL_STEP,
		TUTORIAL_DONE
	}

	private static final Map<Event, String> FILES = new HashMap<>();

	static {
		FILES.put(Event.SHOT, "/sounds/shot.wav");
		FILES.put(Event.LASER, "/sounds/laser.wav");
		FILES.put(Event.HIT, "/sounds/hit.wav");
		FILES.put(Event.DAMAGE, "/sounds/damage.wav");
		FILES.put(Event.KILL, "/sounds/kill.wav");
		FILES.put(Event.TELEPORT, "/sounds/teleport.wav");
		FILES.put(Event.PICKUP, "/sounds/pickup.wav");
		FILES.put(Event.LEVELUP, "/sounds/levelup.wav");
		FILES.put(Event.BOSS_ALERT, "/sounds/boss_alert.wav");
		FILES.put(Event.BOSS_DEFEAT, "/sounds/boss_defeat.wav");
		FILES.put(Event.SHOP, "/sounds/blip.wav");
		FILES.put(Event.WAVE, "/sounds/wave.wav");
		FILES.put(Event.TUTORIAL_STEP, "/sounds/tutorial_step.wav");
		FILES.put(Event.TUTORIAL_DONE, "/sounds/tutorial_done.wav");
	}

	/** Pool de clips por evento: cada chamada play() devolve o clip ao pool. */
	private static final Map<Event, Clip[]> pools = new HashMap<>();
	private static final Map<Event, Integer> poolIdx = new HashMap<>();

	private static final int POOL_SIZE = 4;

	private SoundManager() {
	}

	/**
	 * Reproduz o efeito do evento. Respeita a preferência de som do usuário
	 * ({@link OptionsConfig#isSoundEnabled()}); a música de fundo continua
	 * controlada pela opção de música já existente.
	 */
	public static void play(Event event) {
		if (event == null) {
			return;
		}
		if (!OptionsConfig.isSoundEnabled()) {
			return;
		}
		Clip clip = obtain(event);
		if (clip == null) {
			return;
		}
		clip.setFramePosition(0);
		if (clip.isRunning()) {
			clip.stop();
		}
		clip.start();
	}

	private static synchronized Clip obtain(Event event) {
		Clip[] pool = pools.computeIfAbsent(event, k -> {
			String path = FILES.get(k);
			if (path == null) {
				return new Clip[0];
			}
			Clip[] arr = new Clip[POOL_SIZE];
			for (int i = 0; i < POOL_SIZE; i++) {
				try (InputStream in = SoundManager.class.getResourceAsStream(path)) {
					if (in == null) {
						continue;
					}
					AudioInputStream ais = AudioSystem.getAudioInputStream(in);
					arr[i] = AudioSystem.getClip();
					arr[i].open(ais);
					final Clip clip = arr[i];
					clip.addLineListener(e -> {
						if (e.getType() == LineEvent.Type.STOP) {
							release(event, clip);
						}
					});
					applyVolume(arr[i]);
				} catch (Exception ex) {
					// Clipe indisponível: não bloqueia o jogo.
					break;
				}
			}
			return arr;
		});
		if (pool.length == 0) {
			return null;
		}
		int idx = poolIdx.computeIfAbsent(event, k -> 0) % pool.length;
		poolIdx.put(event, idx + 1);
		return pool[idx];
	}

	private static void release(Event event, Clip clip) {
		// Nada a fazer: o pool é circular e o clip sempre pode ser reutilizado.
	}

	private static void applyVolume(Clip clip) {
		if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
			FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			float gain = OptionsConfig.getSoundVolume(); // -20..+4 dB aproximadamente
			gain = Math.max(volume.getMinimum(), Math.min(volume.getMaximum(), gain));
			volume.setValue(gain);
		}
	}

	/** Descarrega todos os clips (usado ao fechar o jogo ou trocar de save). */
	public static void unload() {
		pools.clear();
		poolIdx.clear();
	}
}
