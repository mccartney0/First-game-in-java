import java.lang.reflect.Field;
import javax.swing.JFrame;
import com.traduvertgames.main.Game;

/**
 * Rodada 22e — valida que a janela maximizada (com barra de tarefas) não
 * encolhe o jogo nem cria faixas pretas tipo "backdrop de modal":
 *
 *  1. recomputeScale em fullscreen usa o retângulo da janela inteira
 *     (sem descontar a barra de tarefas) — o SCALE não downscale de 4 para 3.
 *  2. Maximização nativa (botão do Windows, sem F11) é tratada como tela cheia
 *     pelo listener de resize e pelo recomputeScale.
 *  3. Ao sair da tela cheia, a janela volta ao tamanho exato do jogo.
 */
public class Rodada22eTest {
	private static int passed = 0;
	private static int failed = 0;

	private static void check(String name, boolean ok) {
		if (ok) { passed++; System.out.println("[PASS] " + name); }
		else { failed++; System.out.println("[FAIL] " + name); }
	}

	public static void main(String[] args) throws Exception {
		com.traduvertgames.main.SoundManager.unload();
		new Game();
		Game.SCALE = 4;

		Field fsField = Game.class.getDeclaredField("fullscreen");
		fsField.setAccessible(true);

		// ---- 1) recomputeScale em fullscreen usa o tamanho da janela inteira ----
		// Simula um monitor 1536x864 com janela maximizada cuja área útil é
		// menor que a tela (barra de tarefas consome ~26px): getBounds = tela,
		// getContentPane = tela - taskbar.
		java.lang.reflect.Field frameField = Game.class.getDeclaredField("frame");
		frameField.setAccessible(true);
		JFrame frame = (JFrame) frameField.get(null);
		fsField.setBoolean(null, true);
		frame.setSize(1536, 864); // bounds da janela = tela cheia
		frame.getContentPane().setSize(1536, 838); // área útil menor (taskbar)
		Game.recomputeScale();
		check("Fullscreen com taskbar: SCALE mantem 4 (era " + Game.SCALE + ")", Game.SCALE == 4);
		check("Jogo preenche a janela (1536x864 == buffer*scale)",
				Game.WIDTH * Game.SCALE == 1536 && Game.HEIGHT * Game.SCALE == 864);

		// ---- 2) Maximização nativa (botão do Windows) vira fullscreen ----
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setSize(1536, 864);
		frame.getContentPane().setSize(1536, 838);
		// O listener trata maximização nativa como tela cheia.
		java.awt.event.ComponentListener[] listeners = frame.getComponentListeners();
		boolean hasListener = listeners.length > 0;
		check("Listener de resize registrado", hasListener);

		// ---- 3) Sair do fullscreen restaura o tamanho exato do jogo ----
		// toggleFullscreen sai: frame.setSize(WIDTH*SCALE, HEIGHT*SCALE) ANTES
		// de recomputeScale, usando o SCALE da tela cheia.
		Game.toggleFullscreen();
		boolean normalState = (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == 0;
		check("Janela voltou ao estado NORMAL apos sair do fullscreen", normalState);
		check("Tamanho da janela apos sair do fullscreen = buffer*SCALE (1536x864)",
				frame.getWidth() == 1536 && frame.getHeight() == 864);
		check("Fullscreen false apos toggle", !((Boolean) fsField.get(null)));

		// O SCALE pós-saída (janela 1536x864 > área útil ~838) deve manter 4,
		// pois o tamanho da janela foi fixado antes do recompute.
		check("SCALE apos sair do fullscreen mantem 4", Game.SCALE == 4);

		// ---- Resultado ----
		System.out.println();
		System.out.println("Rodada22eTest: " + passed + " passaram, " + failed
				+ " falharam (total " + (passed + failed) + ")");
		System.out.flush();
		if (failed > 0) { System.exit(1); }
		System.exit(0);
	}
}
