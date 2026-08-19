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
		TUTORIAL_DONE,
		COMPANION_SHOT,
		COMPANION_PURCHASE,
		COMPANION_SPAWN,
		FAIRY_HEAL,
		SHIELD_PULSE,
		SCOUT_SHOT,
		COMPANION_DEATH,
		SKIN_APPLY,
		LEVEL_COMPLETE,
		VICTORY,
		DIALOGUE_START,
		PURCHASE,
		MENU_SELECT,
		NPC_INTERACT,
		MAGIC_CAST,
		MAGIC_HIT,
		EXPERIENCE_ORB,
		SURVIVAL_PHASE,
		DUNGEON_OPEN,
		WEAPON_ION,
		WEAPON_SCATTER,
		WEAPON_FUSION,
		WEAPON_VOID
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
		FILES.put(Event.COMPANION_SHOT, "/sounds/laser.wav");
		FILES.put(Event.COMPANION_PURCHASE, "/sounds/levelup.wav");
			// Sons dos companions (rodada companions-ux): acoplagem, cura da
			// fada, pulso de escudo, disparo do scout, destruição e skin.
			FILES.put(Event.COMPANION_SPAWN, "/sounds/companion_spawn.wav");
			FILES.put(Event.FAIRY_HEAL, "/sounds/fairy_heal.wav");
			FILES.put(Event.SHIELD_PULSE, "/sounds/shield_pulse.wav");
			FILES.put(Event.SCOUT_SHOT, "/sounds/scout_shot.wav");
			FILES.put(Event.COMPANION_DEATH, "/sounds/companion_death.wav");
			FILES.put(Event.SKIN_APPLY, "/sounds/skin_apply.wav");
		// Sons novos (rodada 15): fase concluída, vitória da campanha, início
		// de diálogo, compra na loja e seleção de item de menu.
		FILES.put(Event.LEVEL_COMPLETE, "/sounds/level_complete.wav");
		FILES.put(Event.VICTORY, "/sounds/victory.wav");
		FILES.put(Event.DIALOGUE_START, "/sounds/dialogue_start.wav");
		FILES.put(Event.PURCHASE, "/sounds/purchase.wav");
		FILES.put(Event.MENU_SELECT, "/sounds/menu_select.wav");
		// Som de interação com NPC ao pressionar R (follow-up rodada 20):
		// tom curto de confirmação que avisa que a conversa abriu.
			FILES.put(Event.NPC_INTERACT, "/sounds/npc_interact.wav");
			FILES.put(Event.MAGIC_CAST, "/sounds/magic_cast.wav");
			FILES.put(Event.MAGIC_HIT, "/sounds/magic_hit.wav");
			FILES.put(Event.EXPERIENCE_ORB, "/sounds/experience_orb.wav");
			FILES.put(Event.SURVIVAL_PHASE, "/sounds/survival_phase.wav");
			FILES.put(Event.DUNGEON_OPEN, "/sounds/dungeon_open.wav");
			FILES.put(Event.WEAPON_ION, "/sounds/weapon_ion.wav");
			FILES.put(Event.WEAPON_SCATTER, "/sounds/weapon_scatter.wav");
			FILES.put(Event.WEAPON_FUSION, "/sounds/weapon_fusion.wav");
			FILES.put(Event.WEAPON_VOID, "/sounds/weapon_void.wav");
	}

	/** Pool de clips por evento: cada chamada play() devolve o clip ao pool. */
	private static final Map<Event, Clip[]> pools = new HashMap<>();
	private static final Map<Event, Integer> poolIdx = new HashMap<>();

	private static final int POOL_SIZE = 4;

	private SoundManager() {
	}

	/** Permite substituir um arquivo de áudio sem alterar os call sites do jogo. */
	public static synchronized void registerFile(Event event, String resourcePath) {
		if (event == null || resourcePath == null || resourcePath.isEmpty()) {
			return;
		}
		FILES.put(event, resourcePath);
		Clip[] previous = pools.remove(event);
		poolIdx.remove(event);
		if (previous != null) {
			for (Clip clip : previous) {
				if (clip != null) {
					clip.close();
				}
			}
		}
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

	/** Atualiza o ganho dos clips já carregados após uma alteração nas opções. */
	public static synchronized void refreshVolume() {
		for (Clip[] pool : pools.values()) {
			for (Clip clip : pool) {
				if (clip != null) {
					applyVolume(clip);
				}
			}
		}
	}

	/** Descarrega todos os clips (usado ao fechar o jogo ou trocar de save). */
	public static void unload() {
		pools.clear();
		poolIdx.clear();
	}
}
