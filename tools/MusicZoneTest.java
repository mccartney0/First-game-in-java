import java.lang.reflect.Field;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.MusicManager;
import com.traduvertgames.main.MusicManager.Zone;
import com.traduvertgames.main.OptionsConfig;
import com.traduvertgames.main.SoundManager;

/**
 * Testa a trilha sonora adaptativa (rodada 22) sem iniciar o loop do jogo
 * (padrão dos testes da pasta tools/): mapeamento de zonas por fase, troca de
 * zona com crossfade conduzido manualmente, volume independente e unload.
 */
public class MusicZoneTest {

	static int fails = 0;

	static void check(String name, boolean ok) {
		System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
		if (!ok) fails++;
	}

	public static void main(String[] args) throws Exception {
		// Desativar áudio: remover pools de clips para não travar sem dispositivo.
		SoundManager.unload();
		Game g = new Game();
		g.setCurrentLevel(1);

		// 1. Mapeamento de zona por fase da campanha.
		checkZone("fase 1", Zone.forLevel(1), Zone.FOREST);
		checkZone("fase 2", Zone.forLevel(2), Zone.FOREST);
		checkZone("fase 3", Zone.forLevel(3), Zone.TENSION);
		checkZone("fase 5", Zone.forLevel(5), Zone.TENSION);
		checkZone("fase 6", Zone.forLevel(6), Zone.BOSS);
		checkZone("fase 8", Zone.forLevel(8), Zone.BOSS);

		// 2. setZone carrega o tema sem crashar; crossfade conduz manualmente.
		System.out.println("TESTE 2: setZone TENSION + update() por frames de crossfade");
		MusicManager.setZone(Zone.TENSION);
		for (int i = 0; i < 140; i++) {
			MusicManager.update();
		}
		check("crossfade concluído sem erro", true);

		// 3. Redundância: setZone com a mesma zona não recarrega.
		System.out.println("TESTE 3: setZone redundante (mesma zona)");
		MusicManager.setZone(Zone.TENSION);
		MusicManager.update();
		check("setZone redundante não crasha", true);

		// 4. Volume da trilha independente dos efeitos.
		System.out.println("TESTE 4: OptionsConfig musicVolume");
		float before = OptionsConfig.getMusicVolume();
		// Passo de 2 dB por chamada (mesma semântica de adjustSoundVolume).
		OptionsConfig.adjustMusicVolume(2);
		check("volume da trilha +4 dB (2 passos)", Math.abs(OptionsConfig.getMusicVolume() - (before + 4.0f)) < 0.001f);
		OptionsConfig.adjustMusicVolume(-2);
		check("volume da trilha volta ao padrão", Math.abs(OptionsConfig.getMusicVolume() - before) < 0.001f);
		OptionsConfig.adjustMusicVolume(6);
		check("volume da trilha no máximo +10 dB", Math.abs(OptionsConfig.getMusicVolume() - 10.0f) < 0.001f);
		// Restaurar o volume padrão.
		OptionsConfig.adjustMusicVolume(-5);
		check("volume restaurado a 0 dB", OptionsConfig.getMusicVolume() == 0.0f);

		// 5. Troca para ARENA, pausa e retoma.
		System.out.println("TESTE 5: setZone ARENA e pause()/resume()");
		MusicManager.setZone(Zone.ARENA);
		MusicManager.pause();
		MusicManager.resume();
		MusicManager.pause();
		check("arena + pause/resume sem erro", true);

		// 6. Unload e update após unload.
		System.out.println("TESTE 6: unload");
		MusicManager.unload();
		MusicManager.update();
		check("update após unload não crasha", true);

		// 7. Crossfade interrompido por nova setZone no meio.
		System.out.println("TESTE 7: nova zona no meio do crossfade");
		MusicManager.setZone(Zone.FOREST);
		for (int i = 0; i < 30; i++) {
			MusicManager.update();
		}
		MusicManager.setZone(Zone.BOSS); // interrompe o crossfade anterior
		for (int i = 0; i < 200; i++) {
			MusicManager.update();
		}
		check("setZone no meio do crossfade sem erro", true);
		MusicManager.unload();

		if (fails == 0) {
			System.out.println("ALL PASSED");
			System.exit(0);
		} else {
			System.out.println("FAILURES: " + fails);
			System.exit(1);
		}
	}

	private static void checkZone(String label, Zone got, Zone expected) {
		boolean ok = got == expected;
		check(label + " -> " + got, ok);
	}
}
