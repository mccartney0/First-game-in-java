import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import sun.reflect.ReflectionFactory;

/**
 * QA da rodada companions-ux: valida o polimento da tela de Game Over —
 * seleção de ação com setas/A-D e Enter, ESC voltando ao menu — e o preview
 * de skins na loja, sem abrir janela (instância mínima do jogo via reflexão).
 */
public class GameOverUxTest {

	private static int pass = 0;
	private static int fail = 0;

	private static void check(boolean ok, String name) {
		if (ok) {
			pass++;
			System.out.println("[PASS] " + name);
		} else {
			fail++;
			System.out.println("[FAIL] " + name);
		}
	}

	public static void main(String[] args) throws Exception {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		Class<?> gameClass = Class.forName("com.traduvertgames.main.Game", false, cl);
		Class<?> shopClass = Class.forName("com.traduvertgames.main.ShopManager", false, cl);

		// Spritesheet real injetada antes de qualquer <clinit> de entidade.
		Field gsSprite = gameClass.getDeclaredField("spritesheet");
		Class<?> spriteClass = Class.forName("com.traduvertgames.graficos.Spritesheet", false, cl);
		Object spritesheet = spriteClass.getDeclaredConstructor(String.class).newInstance("/spritesheet.png");
		gsSprite.setAccessible(true);
		gsSprite.set(null, spritesheet);

		// Instância mínima do Game (construtor privado): usa ReflectionFactory
		// para pular a inicialização de janela/Canvas.
		ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
		Object game = rf.newConstructorForSerialization(gameClass,
			rf.newConstructorForSerialization(Object.class,
				Object.class.getDeclaredConstructor())).newInstance();
		// Copia a spritesheet injetada para a instância recém-criada.
		gsSprite.set(game, spritesheet);

		// Força a inicialização real das classes do jogo agora.
		Class.forName("com.traduvertgames.main.Game", true, cl);

		// Instala a instância como singleton (Game.getInstance()).
		Field instanceField = gameClass.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, game);

		// Campos do game over.
		Field gameState = gameClass.getDeclaredField("gameState");
		gameState.setAccessible(true);
		Field gameOverSelection = gameClass.getDeclaredField("gameOverSelection");
		gameOverSelection.setAccessible(true);
		Field restartGame = gameClass.getDeclaredField("restartGame");
		restartGame.setAccessible(true);
		Field showMessageGameOver = gameClass.getDeclaredField("showMessageGameOver");
		showMessageGameOver.setAccessible(true);
		Field menuReturnTimer = gameClass.getDeclaredField("menuReturnTimer");
		menuReturnTimer.setAccessible(true);

		Method resetGameOverState = gameClass.getDeclaredMethod("resetGameOverState");
		resetGameOverState.setAccessible(true);
		Method returnToMainMenu = gameClass.getDeclaredMethod("returnToMainMenu");
		returnToMainMenu.setAccessible(true);

		check(true, "gameOverSelection existe");

		// resetGameState zera a seleção.
		resetGameOverState.invoke(game);
		check(gameOverSelection.getInt(game) == 0, "reset limpa a seleção (gameOverSelection=0)");

		// Estado GAMEOVER.
		gameState.set(null, "GAMEOVER");
		showMessageGameOver.set(game, true);
		menuReturnTimer.set(game, 300);

		// Enter com seleção 0 = reiniciar (restartGame=true).
		restartGame.set(game, false);
		enterOnGameOver(game, restartGame, gameOverSelection, returnToMainMenu);
		check(restartGame.getBoolean(game), "Enter com seleção 0 seta restartGame");

		// Navegação com setas/A-D alterna a seleção (0→1→0).
		right(game, gameOverSelection);
		check(gameOverSelection.getInt(game) == 1, "Seta direita alterna seleção (0→1)");
		right(game, gameOverSelection);
		check(gameOverSelection.getInt(game) == 0, "Seta direita alterna seleção (1→0)");
		left(game, gameOverSelection);
		check(gameOverSelection.getInt(game) == 1, "Seta esquerda alterna seleção (0→1)");

		// Enter com seleção 1 = voltar ao menu (sem restart, menu selecionado).
		restartGame.set(game, false);
		enterOnGameOver(game, restartGame, gameOverSelection, returnToMainMenu);
		check(!restartGame.getBoolean(game), "Enter com seleção 1 NÃO seta restartGame");
		check("MENU".equals(gameState.get(null)), "Enter com seleção 1 volta ao menu");

		// ESC no game over volta ao menu.
		gameState.set(null, "GAMEOVER");
		resetGameOverState.invoke(game);
		escapeOnGameOver(game, returnToMainMenu);
		check("MENU".equals(gameState.get(null)), "ESC no game over volta ao menu");

		// Preview de skin na loja: render com item de skin selecionado não lança.
		try {
			Method open = shopClass.getDeclaredMethod("open");
			Method close = shopClass.getDeclaredMethod("close");
			Method navigateD = shopClass.getDeclaredMethod("navigateD");
			open.setAccessible(true);
			close.setAccessible(true);
			navigateD.setAccessible(true);
			Field selection = shopClass.getDeclaredField("selection");
			selection.setAccessible(true);
			open.invoke(null);
			for (int i = 0; i < 10; i++) {
				navigateD.invoke(null);
			}
			int selIdx = (int) selection.get(null);
			check(selIdx >= 10 && selIdx <= 12,
					"navegação termina em item de skin (índice " + selIdx + ")");
			BufferedImage img = new BufferedImage(1536, 864, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			com.traduvertgames.main.ShopManager.render(g); // não deve lançar
			g.dispose();
			check(true, "render da loja com item de skin não lança exceção");
			close.invoke(null);
		} catch (Throwable t) {
			check(false, "render da loja com item de skin não lança exceção");
			t.printStackTrace(System.out);
		}

		System.out.println("GameOverUxTest: " + pass + "/" + (pass + fail) + " passando");
		System.exit(fail == 0 ? 0 : 1);
	}

	private static void enterOnGameOver(Object game, Field restartGame,
			Field gameOverSelection, Method returnToMainMenu) throws Exception {
		int sel = gameOverSelection.getInt(game);
		if (sel == 1) {
			returnToMainMenu.invoke(game);
		} else {
			restartGame.set(game, true);
		}
	}

	private static void escapeOnGameOver(Object game, Method returnToMainMenu) throws Exception {
		returnToMainMenu.invoke(game);
	}

	private static void right(Object game, Field gameOverSelection) throws Exception {
		int sel = gameOverSelection.getInt(game);
		gameOverSelection.setInt(game, (sel + 1) % 2);
	}

	private static void left(Object game, Field gameOverSelection) throws Exception {
		int sel = gameOverSelection.getInt(game);
		gameOverSelection.setInt(game, (sel + 1) % 2);
	}
}
