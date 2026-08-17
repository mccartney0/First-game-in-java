import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.traduvertgames.graficos.PhaseStatsScreen;
import com.traduvertgames.main.Game;

/**
 * Rodada 22f — valida o card de estatísticas pós-fase:
 *
 *  1. O card renderiza com tamanho legível (>= 300px de largura na escala 4)
 *     e fontes proporcionais — antes o "/4" duplo o esmagava para 75x37px
 *     com texto de 4px.
 *  2. Enter fecha o card em gameState NORMAL — antes o flag this.enter nunca
 *     era levantado quando o PhaseStatsScreen estava ativo e o jogador ficava
 *     preso no card (só escapava pelo menu de pausa).
 */
public class Rodada22fTest {
	private static int passed = 0;
	private static int failed = 0;

	private static void check(String name, boolean ok) {
		if (ok) { passed++; System.out.println("[PASS] " + name); }
		else { failed++; System.out.println("[FAIL] " + name); }
	}

	public static void main(String[] args) throws Exception {
		com.traduvertgames.main.SoundManager.unload();

		// ---- 1) Dimensões do card na escala 4 (probe de render) ----
		Game g = new Game();
		g.setCurrentLevel(1);
		Game.player = new com.traduvertgames.entities.Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		Game.SCALE = 4;

		// Arma o card com stats vazios (show() lê stats do nível atual).
		PhaseStatsScreen.show();
		check("Card está visível após show()", PhaseStatsScreen.isShowing());

		// Puxa o fade a completo antes do probe: no primeiro frame o painel é
		// quase transparente (alpha=15) e o probe de pixels exatos não o veria.
		Field fadeInField = PhaseStatsScreen.class.getDeclaredField("fadeIn");
		fadeInField.setAccessible(true);
		fadeInField.setInt(null, 16);

		BufferedImage probe = new BufferedImage(Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D gp = probe.createGraphics();
		gp.setColor(java.awt.Color.BLACK);
		gp.fillRect(0, 0, probe.getWidth(), probe.getHeight());
		PhaseStatsScreen.render(gp, Game.SCALE);
		gp.dispose();

		// O card azul (RGB 16,26,42) deve ocupar pelo menos 300px de largura.
		int left = -1, right = -1, top = -1, bottom = -1;
		for (int y = 0; y < probe.getHeight(); y++) {
			for (int x = 0; x < probe.getWidth(); x++) {
				int px = probe.getRGB(x, y);
				int r = (px >> 16) & 0xFF, gr = (px >> 8) & 0xFF, b = px & 0xFF;
				if (r == 16 && gr == 26 && b == 42) {
					if (left < 0 || x < left) left = x;
					if (x > right) right = x;
					if (top < 0 || y < top) top = y;
					if (y > bottom) bottom = y;
				}
			}
		}
		int panelW = right - left + 1;
		int panelH = bottom - top + 1;
		System.out.println("Card detectado: " + panelW + "x" + panelH + " (em " + probe.getWidth() + "x" + probe.getHeight() + ")");
		// O probe mede o painel azul (as bordas ciano ficam fora): 300px de
		// largura → 298px medidos, 152px de altura → 150px medidos.
		check("Largura do card ~300px (era 74px com o bug)", panelW >= 298);
		check("Altura do card ~152px (era 36px com o bug)", panelH >= 150);
		check("Card centralizado horizontalmente", Math.abs((left + right) / 2 - probe.getWidth() / 2) <= 4);

		// ---- 2) Enter fecha o card com gameState NORMAL ----
		// Simula o flag this.enter = true (o que o keyPressed agora levanta)
		// e chama o update do card; o gameState deve voltar a NORMAL.
		Field enterField = Game.class.getDeclaredField("enter");
		enterField.setAccessible(true);
		Field stateField = Game.class.getDeclaredField("gameState");
		stateField.setAccessible(true);

		// A guarda do card exige framesElapsed > FADE_TOTAL (16) — o primeiro
		// Enter durante o fade é ignorado (o card ainda está aparecendo).
		Field framesElapsedField = PhaseStatsScreen.class.getDeclaredField("framesElapsed");
		framesElapsedField.setAccessible(true);
		framesElapsedField.setInt(null, 20);

		// O card foi aberto com gameState MENU (pausa); simular o estado que o
		// jogador encontra: NORMAL enquanto o card aguarda o Enter.
		stateField.set(null, "NORMAL");
		enterField.setBoolean(g, true);

		Method update = PhaseStatsScreen.class.getDeclaredMethod("update", boolean.class, boolean.class);
		update.setAccessible(true);
		update.invoke(null, true, false);

		check("Card fecha após Enter durante gameState NORMAL", !PhaseStatsScreen.isShowing());
		check("gameState volta a NORMAL após fechar o card", "NORMAL".equals(stateField.get(null)));

		// Rodar alguns updates garantindo que o card não reabre sozinho.
		for (int i = 0; i < 60; i++) {
			g.update();
		}
		check("Card não reabre sozinho após o fechamento", !PhaseStatsScreen.isShowing());

		// ---- Resultado ----
		System.out.println();
		System.out.println("Rodada22fTest: " + passed + " passaram, " + failed
				+ " falharam (total " + (passed + failed) + ")");
		System.out.flush();
		if (failed > 0) { System.exit(1); }
		System.exit(0);
	}
}
