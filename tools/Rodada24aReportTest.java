import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.main.OnboardingManager;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.graficos.PhaseStatsScreen;
import com.traduvertgames.world.World;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Rodada 24a — reproduz os dois bugs reportados pelo jogador:
 * (1) card "Fase Concluída" mostra kills/tempo/combo zerados;
 * (2) ao morrer e reiniciar, o jogo volta ao tutorial.
 */
public class Rodada24aReportTest {
	private static int passed = 0;
	private static int failed = 0;

	private static void check(boolean condition, String description) {
		if (condition) {
			passed++;
			System.out.println("PASS: " + description);
		} else {
			failed++;
			System.out.println("FAIL: " + description);
		}
	}

	public static void main(String[] args) throws Exception {
		// Limpa saves residuais para isolar o cenário.
		new java.io.File("saves.json").delete();

		Game g = new Game();
		Game.setCurrentLevel(1);
		Game.SCALE = 4;
		Game.player = new com.traduvertgames.entities.Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		World.restartGame("level1.png");

		System.out.println("=== BUG 1: STATS ZERADOS NO CARD DE FASE CONCLUÍDA ===");

		// Simula o progresso da fase 1: kills, combo e tempo decorrido.
		Game.registerEnemyKill();
		Game.registerEnemyKill();
		Game.setBestComboThisRun(4);

		check(Game.getKillsThisLevel() == 2, "Pré-transição: kills da fase acumulados (2)");
		check(Game.getBestComboThisRun() >= 4, "Pré-transição: combo máximo da fase >= 4");

		// Avança de fase como o jogo faz ao concluir o objetivo.
		Game.advanceToNextLevel();

		// O card de estatísticas captura os valores da fase que acabou de
		// terminar em seus campos internos — os estáticos do Game já foram
		// zerados pelo restart, mas o card deve ter gravado tudo antes disso.
		check(getCardKills() == 2,
				"Pós-transição: kills da fase gravados no card (era " + getCardKills() + ")");
		// Em ambiente headless (sem game loop rodando), o timer real decorrido
		// é de poucos milissegundos — o que importa é que o card captura o
		// tempo da fase que terminou (não mais 0, como acontecia antes).
		check(getCardTimeMs() > 0,
				"Pós-transição: tempo da fase gravado no card (era 0)");
		check(getCardCombo() >= 4,
				"Pós-transição: combo máximo gravado no card (era " + getCardCombo() + ")");

		// Melhora no total acumulado da campanha (score) deve seguir visível.
		check(Game.getScore() >= 200,
				"Pós-transição: score acumulado preservado (" + Game.getScore() + ")");

		System.out.println("=== BUG 2: RESTART PÓS-MORTE VOLTA AO TUTORIAL ===");

		// Recria o jogo na fase 1 para isolar o cenário reportado: morrer na
		// fase 1, apertar "Reiniciar partida" e verificar se o jogo retoma a
		// fase salva em vez de cair no tutorial/tela de escolha de arma.
		Game g2 = new Game();
		Game.setCurrentLevel(1);
		Game.SCALE = 4;
		Game.player = new com.traduvertgames.entities.Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		World.restartGame("level1.png");
		Game.clearInitialWeaponSelect();
		// O timer da fase precisa estar em andamento para o save ser válido.
		Thread.sleep(1200);

		SaveManager.saveAutoSave();
		check(SaveManager.hasAnySave(), "Autosave criado ao morrer");
		int deadLevel = Game.getCurrentLevel();
		check(deadLevel == 1, "Cenário: morte acontece na fase 1");

		// handleGameOverRestart é privado — o teste invoca via reflection,
		// simulando exatamente o que o update() chama ao marcar restartGame.
		Method hgr = Game.class.getDeclaredMethod("handleGameOverRestart");
		hgr.setAccessible(true);
		hgr.invoke(g2);

		check(!OnboardingManager.isActive(),
				"Restart pós-morte: tutorial NÃO reaparece");
		check(!isShowInitialWeaponSelect(),
				"Restart pós-morte: tela de escolha de arma inicial fechada");
		check(Game.getCurrentLevel() == deadLevel,
				"Restart pós-morte: retorna à fase " + deadLevel
						+ " em que o jogador morreu (não cai em tela inicial)");
		check("NORMAL".equals(Game.gameState),
				"Restart pós-morte: estado do jogo é NORMAL (jogando a fase)");

		// Limpa o save de teste.
		new java.io.File("saves.json").delete();

		System.out.println("Progresso: " + passed + " passaram, " + failed + " falharam");
		System.exit(failed == 0 ? 0 : 1);
	}

	private static boolean isShowInitialWeaponSelect() throws Exception {
		Field f = Game.class.getDeclaredField("showInitialWeaponSelect");
		f.setAccessible(true);
		return f.getBoolean(null);
	}

	// ---- Acesso aos valores capturados pelo PhaseStatsScreen (rodada 24) ----
	private static int getCardKills() throws Exception {
		return readIntCard("capturedKills");
	}

	private static long getCardTimeMs() throws Exception {
		return readLongCard("capturedTimeMs");
	}

	private static int getCardCombo() throws Exception {
		return readIntCard("capturedBestCombo");
	}

	private static int readIntCard(String field) throws Exception {
		Class<?> clazz = com.traduvertgames.graficos.PhaseStatsScreen.class;
		try {
			Field f = clazz.getDeclaredField(field);
			f.setAccessible(true);
			return (int) f.getLong(null);
		} catch (NoSuchFieldException e) {
			return -1;
		}
	}

	private static long readLongCard(String field) throws Exception {
		Class<?> clazz = com.traduvertgames.graficos.PhaseStatsScreen.class;
		try {
			Field f = clazz.getDeclaredField(field);
			f.setAccessible(true);
			return f.getLong(null);
		} catch (NoSuchFieldException e) {
			return -1;
		}
	}
}
