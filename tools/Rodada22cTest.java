import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.world.World;

/**
 * Rodada 22c — validação dos fixes:
 * (1) o fade preto da transição decai independentemente do cooldown pós-
 *     transição, evitando escurecimento residual na tela;
 * (2) os inimigos ficam congelados enquanto a fase está concluída ou a
 *     transição está visível (isTransitioning), evitando o bug de inimigos
 *     invisíveis que continuam movendo/atirando após fechar a loja.
 */
public class Rodada22cTest {

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

	static void setBoolean(Class<?> clazz, String field, boolean value) throws Exception {
		Field f = clazz.getDeclaredField(field);
		f.setAccessible(true);
		f.setBoolean(null, value);
	}

	public static void main(String[] args) throws Exception {
		new File("saves.json").delete();
		SoundManager.unload();

		Game g = new Game();
		g.setCurrentLevel(1);
		World.restartGame("level1.png");
		Game.gameState = "NORMAL";

		// --- 1. Fade decai mesmo com cooldown ativo (sem residual escuro) ---
		System.out.println("[Teste 1] Fade decai independentemente do cooldown");
		setInt(Game.class, "transitionCooldown", 150);
		setInt(Game.class, "transitionAlpha", 255);
		int before = getInt(Game.class, "transitionAlpha");
		for (int i = 0; i < 40; i++) {
			g.update();
		}
		int after = getInt(Game.class, "transitionAlpha");
		check("fade decai durante o cooldown (antes=" + before + " depois="
				+ after + ")", after < before && after < 100);

		for (int i = 0; i < 200 && getInt(Game.class, "transitionAlpha") > 0; i++) {
			g.update();
		}
		check("fade zera apos ~32 frames",
				getInt(Game.class, "transitionAlpha") == 0);

		// --- 2. Inimigos congelados enquanto isTransitioning (fase concluída) ---
		System.out.println("[Teste 2] Inimigos congelados com questCompletedPending");
		// Cria um inimigo próximo do jogador e anota a posição inicial.
		// WaveManager pausado para a fase estar quieta (sem spawns).
		com.traduvertgames.main.WaveManager.stopArena();
		BufferedImage dummy =
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		// Posiciona o spy a ~90px do jogador: dentro do raio de detecção
		// (~100px) ele entra em perseguição e se move em linha reta
		// (moveDirectlyTowardsPlayer), sem depender do pathfinder do teste.
		Enemy spy = new Enemy(Game.player.getX() + 64, Game.player.getY() + 64,
				16, 16, dummy);
		Game.entities.add(spy);
		Game.enemies.add(spy);
		double xBefore = spy.getX();

		setBoolean(Game.class, "questCompletedPending", true);
		Game.gameState = "NORMAL";
		for (int i = 0; i < 120; i++) {
			g.update();
		}
		double dx = Math.abs(spy.getX() - xBefore);
		check("inimigo nao se move enquanto a fase esta concluida (dx="
				+ String.format("%.1f", dx) + ")", dx < 1.0);

		// --- 3. A condição de congelamento depende de isTransitioning() ---
		// (rodada 22c): sem a pendência e sem transição visível, o freeze não
		// se aplica; validar o valor retornado de isTransitioning() em ambos
		// os estados antes/depois de limpar a pendência.
		System.out.println("[Teste 3] isTransitioning reflete a conclusao pendente");
		boolean transitioningWithPending = Game.isTransitioning();
		setBoolean(Game.class, "questCompletedPending", false);
		// Limpa o aviso herdado do estado anterior do teste para isolar a
		// verificação de isTransitioning() na pendência de conclusão.
		setInt(Game.class, "showLevelTransition", 0);
		setInt(Game.class, "transitionCooldown", 0);
		boolean transitioningAfterClear = Game.isTransitioning();
		check("isTransitioning==true com a conclusao pendente", transitioningWithPending);
		check("isTransitioning==false apos limpar a conclusao pendente",
				!transitioningAfterClear);
		Game.enemies.remove(spy);

		System.out.println("Rodada22cTest: " + (5 - fails) + " passaram, " + fails
				+ " falharam (total 5)");
		System.exit(fails > 0 ? 1 : 0);
	}
}
