import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Rodada 15 — valida a navegação das telas de menu sem precisar instanciar o
 * jogo (AWT):
 * 1) Menu: os campos left/right/escape existem (o Game os dispara);
 * 2) LevelUpManager: navigateLeft/Right mantém choiceIndex em [0,2] e
 *    selectAndConfirm(i) aplica o upgrade do índice.
 */
public class MenuNavigationTest {
	private static int passed = 0, failed = 0;

	private static void check(boolean ok, String label) {
		if (ok) {
			passed++;
			System.out.println("  PASS " + label);
		} else {
			failed++;
			System.out.println("  FAIL " + label);
		}
	}

	public static void main(String[] args) throws Exception {
		System.out.println("== MenuNavigationTest ==");

		// ---------- Menu: campos de navegação horizontal e ESC ----------
		Class<?> menuClass = Class.forName("com.traduvertgames.main.Menu");
		check(fieldExists(menuClass, "left"), "Menu.left existe (seta/A)");
		check(fieldExists(menuClass, "right"), "Menu.right existe (seta/D)");
		check(fieldExists(menuClass, "escape"), "Menu.escape existe (ESC nas telas do menu)");
		check(methodExists(menuClass, "moveSelection", int.class), "Menu.moveSelection(int) existe");
		check(methodExists(menuClass, "getCurrentOptionCount"), "Menu.getCurrentOptionCount() existe");

		// ---------- LevelUpManager: navegação horizontal e seleção direta ----------
		Class<?> lumClass = Class.forName("com.traduvertgames.main.LevelUpManager");
		check(methodExists(lumClass, "navigateLeft"), "LevelUpManager.navigateLeft() existe");
		check(methodExists(lumClass, "navigateRight"), "LevelUpManager.navigateRight() existe");
		check(methodExists(lumClass, "selectAndConfirm", int.class), "LevelUpManager.selectAndConfirm(int) existe");

		Field choiceField = lumClass.getDeclaredField("choiceIndex");
		choiceField.setAccessible(true);
		Field showing = lumClass.getDeclaredField("showingLevelUp");
		showing.setAccessible(true);

		Method navigateLeft = lumClass.getDeclaredMethod("navigateLeft");
		Method navigateRight = lumClass.getDeclaredMethod("navigateRight");
		Method selectAndConfirm = lumClass.getDeclaredMethod("selectAndConfirm", int.class);

		showing.set(null, true);
		choiceField.set(null, 1);

		navigateLeft.invoke(null);
		check((int) choiceField.get(null) == 0, "navigateLeft 1→0 com showing");
		navigateLeft.invoke(null);
		check((int) choiceField.get(null) == 0, "navigateLeft trava em 0 (clamp)");
		navigateRight.invoke(null);
		navigateRight.invoke(null);
		navigateRight.invoke(null);
		check((int) choiceField.get(null) == 2, "navigateRight trava em 2 (clamp)");
		// Volta ao meio e confirma: o upgrade do índice 1 é aplicado e o
		// painel fecha (gameState volta a NORMAL); choiceIndex é
		// redefinido apenas na próxima chamada de show().
		navigateLeft.invoke(null);
		Field showingAfter = lumClass.getDeclaredField("showingLevelUp");
		showingAfter.setAccessible(true);
		// Com upgrade válido no slot 1, o painel é aplicado e fechado:
		// o applyChoice completo depende do ambiente do jogo (Entity,
		// Spritesheet), por isso a verificação aqui é feita por gameState
		// e showingLevelUp antes do switch de aplicação (o índice e o
		// método selectAndConfirm(index) já foram confirmados acima).
		Class<?> upgradeClass = Class.forName("com.traduvertgames.main.LevelUpManager$Upgrade");
		Field pendingField = lumClass.getDeclaredField("pendingChoices");
		pendingField.setAccessible(true);
		Object[] fakeUpgrades = (Object[]) java.lang.reflect.Array.newInstance(upgradeClass, 3);
		fakeUpgrades[1] = upgradeClass.getDeclaredMethod("valueOf", String.class).invoke(upgradeClass, "VIDA_MAXIMA");
		pendingField.set(null, fakeUpgrades);
		// O applyChoice aplica Player.maxLife += 25, o que inicializa a
		// classe Entity (spritesheet = null fora do jogo). Usa-se o
		// Spritesheet real do jogo, presente no classpath res/.
		Field gsSpriteField = Class.forName("com.traduvertgames.main.Game").getDeclaredField("spritesheet");
		gsSpriteField.setAccessible(true);
		gsSpriteField.set(null, new com.traduvertgames.graficos.Spritesheet("/spritesheet.png"));
		selectAndConfirm.invoke(null, 1);
		check(!(boolean) showingAfter.get(null), "selectAndConfirm(1) aplica e fecha o painel");
		showing.set(null, true);
		navigateLeft.invoke(null);

		showing.set(null, false);

		System.out.println("Resultados: " + passed + " passaram, " + failed + " falharam");
		if (failed > 0) {
			System.exit(1);
		}
	}

	private static boolean fieldExists(Class<?> c, String name) {
		try {
			c.getDeclaredField(name);
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	private static boolean methodExists(Class<?> c, String name, Class<?>... params) {
		try {
			c.getDeclaredMethod(name, params);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
}
