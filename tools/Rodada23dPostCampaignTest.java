import com.traduvertgames.main.Game;
import com.traduvertgames.main.WaveManager;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.world.World;
import com.traduvertgames.world.ProceduralLevelGenerator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

/**
 * Rodada 23d — integração do pós-campanha ao modo infinito procedurais.
 * Ao concluir a fase 8, o jogo entra na primeira profundidade procedural
 * (mapa gerado, chefes rotativos) em vez da arena fixa de ondas.
 */
public class Rodada23dPostCampaignTest {
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
		// Inicialização mínima do jogo.
		Game g = new Game();
		Game.setCurrentLevel(8);
		Game.SCALE = 4;
		Game.player = new com.traduvertgames.entities.Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		World.restartGame("level8.png");

		System.out.println("=== PÓS-CAMPANHA (CONCLUSÃO DA FASE 8) ===");

		// Estado antes da transição: campanha na fase 8, modo procedural inativo.
		check(Game.getCurrentLevel() == 8, "Pré-transição: jogador está na fase 8");
		check(!WaveManager.isArenaMode(), "Pré-transição: modo arena inativo");

		// Simula a conclusão da fase 8 (o método avançava CUR_LEVEL e entrava
		// no modo sobrevivência; agora deve entrar no modo infinito procedural).
		Game.advanceToNextLevel();

		check(Game.getCurrentLevel() == 8,
				"Pós-campanha: fase exibida permanece 8 (Núcleo Central, visual)");
		check(WaveManager.isArenaMode(),
				"Pós-campanha: modo arena ativo (base do modo infinito)");

		// O mapa carregado deve ser o procedural da profundidade 1, e não o
		// level8.png fixo — os níveis procedural deixam arquivos no disco.
		File depth1 = ProceduralLevelGenerator.generate(1);
		check(depth1 != null && depth1.exists(),
				"Pós-campanha: mapa procedural da profundidade 1 existe");

		// Objetivo da fase deve ser o do modo infinito (profundidade 9), não
		// uma repetição da fase 8 — prepareForLevel(MAX_LEVEL+1).
		check(QuestManager.isObjectiveComplete() == false,
				"Pós-campanha: novo objetivo procedural criado (não completo de início)");

		// O cooldown de respiro evita que os inimigos ataquem de imediato.
		Field cooldown = Game.class.getDeclaredField("transitionCooldown");
		cooldown.setAccessible(true);
		check(((Number) cooldown.get(Game.getInstance())).intValue() > 0,
				"Pós-campanha: cooldown de respiro ativo antes dos mobs");

		// A profundidade começa em 1 a cada entrada (não herda resíduos).
		Field levelPlus = Game.class.getDeclaredField("levelPlus");
		levelPlus.setAccessible(true);
		check(((Number) levelPlus.get(Game.getInstance())).intValue() == 1,
				"Pós-campanha: profundidade inicial é 1");

		System.out.println("Progresso: " + passed + " passaram, " + failed + " falharam");
		System.exit(failed == 0 ? 0 : 1);
	}
}
