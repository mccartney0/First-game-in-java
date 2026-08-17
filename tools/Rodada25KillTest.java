import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.EnemyKillTracker;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.world.World;

/**
 * Rodada 25 — inimigos mortos não ressuscitam no reinício.
 * E2E: carrega a fase 2 (mapa fixo), mata um inimigo, salva, reinicia o
 * mundo e confere que o inimigo abatido não volta; o inimigo vivo volta;
 * o conjunto é gravado no saves.json e restaurado.
 */
public class Rodada25KillTest {
	private static int pass = 0;
	private static int fail = 0;

	static void check(String name, boolean ok) {
		if (ok) {
			pass++;
			System.out.println("PASS: " + name);
		} else {
			fail++;
			System.out.println("FAIL: " + name);
		}
	}

	private static int countEnemies() {
		return Game.enemies == null ? 0 : Game.enemies.size();
	}

	public static void main(String[] args) throws Exception {
		new File("saves.json").delete();
		new File("save.txt").delete();

		Game g = new Game();
		Game.SCALE = 4;
		Game.setCurrentLevel(2);
		Game.player = new Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));

		// Carrega o mapa da fase 2 (aplica o pixel map).
		World.restartGame("level2.png");
		int antes = countEnemies();
		check("Fase 2 carregada com inimigos", antes >= 5);

		// Inimigo-alvo: primeiro da lista.
		Enemy alvo = Game.enemies.get(0);
		int alvoTileX = alvo.getX() / 16;
		int alvoTileY = alvo.getY() / 16;

		// Simula a morte: marca no tracker e remove das listas (destroySelf
		// faz isso em produção; aqui replicamos o efeito sem o jogo rodar).
		EnemyKillTracker.markDead(alvoTileX, alvoTileY, alvo.isBoss());
		Game.enemies.remove(alvo);
		Game.entities.remove(alvo);
		int depois = countEnemies();
		check("Inimigo abatido: contagem diminuiu", depois == antes - 1);
		check("Posição registrada como morta",
				EnemyKillTracker.isAlreadyDead(alvoTileX, alvoTileY, alvo.isBoss()));

		// Salva (o tracker é gravado em inimigosMortosSet) — mesmo fluxo do
		// autosave do jogo ao morrer/trocar de fase.
		boolean saved = SaveManager.saveCurrentGame();
		check("Save gravado com sucesso", saved);
		check("inimigosMortosSet gravado no arquivo",
				SaveManager.SAVE_FILE.exists()
					&& new String(java.nio.file.Files.readAllBytes(
							SaveManager.SAVE_FILE.toPath()))
							.contains("inimigosMortosSet"));

		// Fluxo real do restart pós-morte: carregar o slot restaura o
		// tracker E recria o mundo — o inimigo abatido não deve voltar.
		Game g2 = new Game();
		Game.setCurrentLevel(2);
		Game.SCALE = 4;
		SaveManager.activeSlot = 1;
		boolean loaded = SaveManager.loadSlot(1);
		check("loadSlot(1) restaura o save", loaded);

		// O inimigo abatido NÃO voltou; os demais sim.
		check("Inimigo abatido não ressuscitou no reload",
				countEnemies() == antes - 1);
		boolean mortoApareceu = false;
		for (Enemy en : Game.enemies) {
			if (en.getX() / 16 == alvoTileX && en.getY() / 16 == alvoTileY) {
				mortoApareceu = true;
				break;
			}
		}
		check("Tile do abatido vazio no reload", !mortoApareceu);

		// O tracker foi restaurado do saves.json (deserialização).
		check("Tracker persiste após reload: posição ainda morta",
				EnemyKillTracker.isAlreadyDead(alvoTileX, alvoTileY, alvo.isBoss()));

		// Serialize/deserialize diretos do formato.
		EnemyKillTracker.markDead(5, 6, false);
		String state = EnemyKillTracker.serialize();
		check("Serialize não vazio", state.contains("5,6N"));
		EnemyKillTracker.deserialize(state);
		check("Deserialize recupera a posição",
				EnemyKillTracker.isAlreadyDead(5, 6, false));
		check("Deserialize mantém a anterior",
				EnemyKillTracker.isAlreadyDead(alvoTileX, alvoTileY, alvo.isBoss()));

		// Reset limpa tudo (troca de fase).
		EnemyKillTracker.reset();
		check("Reset limpa o conjunto", EnemyKillTracker.deadCount() == 0);
		check("Reset não marca o tile", !EnemyKillTracker.isAlreadyDead(5, 6, false));

		new File("saves.json").delete();
		System.out.println("Resultado: " + pass + " pass, " + fail + " fail");
		System.exit(fail == 0 ? 0 : 1);
	}
}
