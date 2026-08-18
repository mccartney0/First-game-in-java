import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

import com.traduvertgames.graficos.PhaseStatsScreen;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.Menu;
import com.traduvertgames.main.OptionsConfig;

/** Regressões de navegação de opções e overlays pós-fase. */
public final class OptionsTransitionRegressionTest {
    private static int passed;
    private static int failed;

    private OptionsTransitionRegressionTest() {
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + label);
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) throws Exception {
        Game game = new Game();
        Menu menu = game.menu;
        Class<?> screenClass = Class.forName("com.traduvertgames.main.Menu$Screen");
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Object optionsScreen = Enum.valueOf((Class) screenClass, "OPTIONS");
        Field currentScreen = Menu.class.getDeclaredField("currentScreen");
        Field currentOption = Menu.class.getDeclaredField("currentOption");
        currentScreen.setAccessible(true);
        currentOption.setAccessible(true);
        currentScreen.set(menu, optionsScreen);

        OptionsConfig.adjustSoundVolume(-100);
        currentOption.setInt(menu, 3);
        menu.right = true;
        menu.update();
        check("direita aumenta volume dos efeitos", OptionsConfig.getSoundVolume() == -36.0f);
        check("ajuste de volume mantém a opção selecionada", currentOption.getInt(menu) == 3);
        menu.left = true;
        menu.update();
        check("esquerda diminui volume dos efeitos", OptionsConfig.getSoundVolume() == -40.0f);

        OptionsConfig.adjustMusicVolume(-100);
        currentOption.setInt(menu, 1);
        menu.right = true;
        menu.update();
        check("direita aumenta volume da trilha", OptionsConfig.getMusicVolume() == -16.0f);
        menu.left = true;
        menu.update();
        check("esquerda diminui volume da trilha", OptionsConfig.getMusicVolume() == -20.0f);

        menu.clearPendingInput();
        currentScreen.set(menu, Enum.valueOf((Class) screenClass, "MAIN"));
        Game.gameState = "NORMAL";
        PhaseStatsScreen.show();
        menu.enter = false;
        game.mousePressed(new MouseEvent(new Canvas(), MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1));
        check("clique não confirma menu durante estatísticas", !menu.enter);
        check("clique não dispara tiro durante estatísticas", !game.player.mouseShoot);
        PhaseStatsScreen.dismiss();

        Field initialSelect = Game.class.getDeclaredField("showInitialWeaponSelect");
        initialSelect.setAccessible(true);
        initialSelect.setBoolean(null, true);
        Game.gameState = "MENU";
        menu.enter = false;
        game.mousePressed(new MouseEvent(new Canvas(), MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1));
        check("clique não confirma menu durante escolha inicial", !menu.enter);
        game.keyPressed(new KeyEvent(new Canvas(), KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
        check("seta direita não navega menu durante escolha inicial", !menu.right);
        initialSelect.setBoolean(null, false);
        menu.clearPendingInput();
        Game.gameState = "NORMAL";

        System.out.println("OptionsTransitionRegressionTest: " + passed + " pass, " + failed + " fail");
        System.exit(failed == 0 ? 0 : 1);
    }
}
