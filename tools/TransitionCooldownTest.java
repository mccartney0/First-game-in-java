import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.graficos.PhaseStatsScreen;
import com.traduvertgames.graficos.MissionBanner;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.main.WaveManager;

/**
 * Rodada 21 — testa o cooldown de respiro pós-transição:
 * 1. advanceToNextLevel() arma transitionCooldown = RESPIRO_FRAMES
 * 2. Durante o cooldown: aviso fixo (showLevelTransition não decai),
 *    fade fixo (transitionAlpha não decai), WaveManager não spawna,
 *    inimigos congelados
 * 3. Ao final do cooldown: tudo zera e o combate retoma
 * 4. PhaseStatsScreen na campanha não fecha sozinho (sem auto-dismiss)
 * 5. Banner de lore da nova fase é agendado (scheduleLore) e só dispara
 *    quando o cooldown termina
 */
public class TransitionCooldownTest {
	static int fails = 0;

	static void check(String name, boolean ok) {
		System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
		if (!ok) {
			fails++;
		}
	}

	static int getInt(Class<?> clazz, String field) throws Exception {
		Field f = clazz.getDeclaredField(field);
		f.setAccessible(true);
		return f.getInt(null);
	}

	static void setInt(Class<?> clazz, String field, int value) throws Exception {
		Field f = clazz.getDeclaredField(field);
		f.setAccessible(true);
		f.setInt(null, value);
	}

	public static void main(String[] args) throws Exception {
		// Desativar áudio: remover pools de clips para não travar sem dispositivo
		SoundManager.unload();
		Game g = new Game();
		g.setCurrentLevel(2);
		com.traduvertgames.world.World.restartGame("level2.png");
		com.traduvertgames.quest.QuestManager.onLevelLoaded();
		Game.gameState = "NORMAL";

		// --- 1. Cooldown é armado ao avançar de fase ---
		System.out.println("[Teste 1] Cooldown armado em advanceToNextLevel");
		setInt(Game.class, "transitionCooldown", 0);
		setInt(Game.class, "showLevelTransition", 0);
		setInt(Game.class, "transitionAlpha", 0);
		Game.advanceToNextLevel();
		// Após o avanço o estado é MENU (card de estatísticas na frente);
		// o cooldown decairá quando o jogador fechar o card (Enter/dismiss).

		int cooldown = getInt(Game.class, "transitionCooldown");
		check("estado é MENU com o card de estatísticas na frente",
				"MENU".equals(Game.gameState));
		check("aviso prolongado (>= RESPIRO)", getInt(Game.class, "showLevelTransition") >= 150);
		check("fade ativo (150)", getInt(Game.class, "transitionAlpha") == 150);
		System.out.println("    cooldown=" + cooldown);
		// Simula o fechamento do card pelo jogador (Enter): disparará o
		// cooldown de respiro.
		PhaseStatsScreen.dismiss();
		cooldown = getInt(Game.class, "transitionCooldown");
		check("cooldown > 0 após fechar o card", cooldown > 0);
		System.out.println("    cooldown após dismiss=" + cooldown);

		// --- 2. Aviso e fade fixos durante o cooldown ---
		System.out.println("[Teste 2] Aviso e fade fixos durante o cooldown");
		int trBefore = getInt(Game.class, "showLevelTransition");
		int alphaBefore = getInt(Game.class, "transitionAlpha");
		g.update();
		g.update();
		check("showLevelTransition não decai durante cooldown",
				getInt(Game.class, "showLevelTransition") == trBefore);
		check("transitionAlpha não decai durante cooldown",
				getInt(Game.class, "transitionAlpha") == alphaBefore);

		// --- 3. Cooldown decai no estado NORMAL ---
		System.out.println("[Teste 3] Cooldown decai e libera o combate");
		Game.gameState = "NORMAL";
		int cd = getInt(Game.class, "transitionCooldown");
		for (int i = 0; i <= cd + 5; i++) {
			g.update();
		}
		check("cooldown zera ao final", getInt(Game.class, "transitionCooldown") == 0);
		check("showLevelTransition zera ao final",
				getInt(Game.class, "showLevelTransition") == 0);
		int alphaFinal = getInt(Game.class, "transitionAlpha");
		check("fade esmaeceu ao final", alphaFinal < alphaBefore);
		// O fade decai junto com o cooldown: no último frame ele ainda
		// pode estar ligeiramente acima de 0 (o jogo revela a fase).

		// --- 4. Card de stats na campanha não fecha sozinho ---
		System.out.println("[Teste 4] Card de stats na campanha só fecha por Enter");
		WaveManager.stopArena();
		Game.advanceToNextLevel();
		PhaseStatsScreen.show();
		check("card visível", PhaseStatsScreen.isShowing());
		for (int i = 0; i < 300; i++) {
			PhaseStatsScreen.update(false, false);
		}
		check("card permanece visível após 300 frames (sem auto-dismiss na campanha)",
				PhaseStatsScreen.isShowing());
		// No modo infinito (arena ativa), o auto-dismiss continua funcionando:
		WaveManager.startArena();
		PhaseStatsScreen.show();
		for (int i = 0; i < 320; i++) {
			PhaseStatsScreen.update(false, false);
		}
		check("auto-dismiss mantém funcionando no modo infinito",
				!PhaseStatsScreen.isShowing());

		// --- 5. Inimigos congelados durante o cooldown ---
		System.out.println("[Teste 5] Inimigos congelados durante o cooldown");
		WaveManager.stopArena();
		Game.advanceToNextLevel();
		PhaseStatsScreen.dismiss(); // fecha o card → inicia o cooldown
		Game.gameState = "NORMAL";
		Enemy spy = new Enemy(10, 10, 16, 16, null);
		Game.enemies.add(spy);
		double xBefore = spy.getX();
		g.update();
		g.update();
		check("inimigo não se move durante cooldown", Math.abs(spy.getX() - xBefore) < 0.001);
		Game.enemies.remove(spy);

		// --- 6. Banner de lore agendado dispara ao fim do cooldown ---
		System.out.println("[Teste 6] Lore agendada dispara ao fim do cooldown");
		WaveManager.stopArena();
		Game.advanceToNextLevel();
		PhaseStatsScreen.dismiss(); // fecha o card → inicia o cooldown
		Game.gameState = "NORMAL";
		check("banner de lore agendado (não exibido de imediato)",
				getInt(MissionBanner.class, "scheduledDelay") > 0 && !MissionBanner.isShowing());
		int cd2 = getInt(Game.class, "transitionCooldown");
		for (int i = 0; i <= cd2 + 5; i++) {
			g.update();
		}
		check("lore aparece após o cooldown", MissionBanner.isShowing());

		System.out.println("\n=== Results: " + (15 - fails) + " passed, " + fails + " failed ===");
		System.exit(fails > 0 ? 1 : 0);
	}
}
