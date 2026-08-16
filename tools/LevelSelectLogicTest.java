import java.lang.reflect.Method;
import java.lang.reflect.Field;
import com.traduvertgames.main.LevelSelectScreen;
import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;

/**
 * Teste da lógica da LevelSelectScreen sem abrir a janela:
 * 1) confirmSelection na fase atual apenas fecha (não reinicia);
 * 2) closeOnTab fecha;
 * 3) close restaura gameState NORMAL.
 */
public class LevelSelectLogicTest {
    static int fails = 0;
    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (!ok) fails++;
    }
    public static void main(String[] args) throws Exception {
        // Inicializa o jogo SEM janela visível (headless) - frame pode ser null;
        // a lógica de LSS depende de Game.instance e QuestManager.
        // Usamos reflexão para preparar state sem construtor completo? O QuestManager
        // prepareForLevel não precisa de jogo. LSS.open() seta gameState e seleção.
        // LSS.close() seta gameState = "NORMAL" - testável via reflexão dos campos.

        // Preparar QuestManager na fase 3 sem jogo rodando:
        QuestManager.prepareForLevel(3);
        check("fase atual = 3", QuestManager.getCurrentLevel() == 3);

        // open() via reflexão:
        Method open = LevelSelectScreen.class.getMethod("open");
        Method close = LevelSelectScreen.class.getMethod("close");
        Method closeOnTab = LevelSelectScreen.class.getMethod("closeOnTab");
        Method confirm = LevelSelectScreen.class.getMethod("confirmSelection");
        Method isOpen = LevelSelectScreen.class.getMethod("isOpen");

        // Instanciar game sem frame? O construtor Game() cria frame -> falha headless?
        // Canvas/JFrame criam no headless? AWT funciona sem display? Sim, com toolkit headless.
        System.setProperty("java.awt.headless", "false");
        Game g = new Game(); // cria janela - pode falhar sem X
        Field fState = Game.class.getField("gameState");

        open.invoke(null);
        check("LSS aberta", isOpen.invoke(null).equals(true));
        check("gameState = LEVELSELECT", "LEVELSELECT".equals(fState.get(null)));

        // Enter na fase atual (selection = 2 -> fase 3 = atual): não deve reiniciar
        String beforeState = (String) fState.get(null);
        com.traduvertgames.world.World wBefore = Game.world;
        confirm.invoke(null);
        check("confirm na fase atual fecha sem reiniciar", !isOpen.invoke(null).equals(true));
        check("gameState voltou a NORMAL", "NORMAL".equals(fState.get(null)));
        check("World não foi recriado", Game.world == wBefore);

        // Reabrir e fechar com TAB
        open.invoke(null);
        closeOnTab.invoke(null);
        check("closeOnTab fecha", !isOpen.invoke(null).equals(true));
        check("gameState NORMAL após TAB", "NORMAL".equals(fState.get(null)));

        // Reabrir e fechar com ESC (escapePressed)
        open.invoke(null);
        Field fEsc = Game.class.getField("escapePressed");
        fEsc.set(null, true);
        LevelSelectScreen.update();
        check("ESC fecha via update", !isOpen.invoke(null).equals(true));
        check("gameState NORMAL após ESC", "NORMAL".equals(fState.get(null)));

        if (fails == 0) System.out.println("== LevelSelectLogicTest: TODOS PASSARAM ==");
        else System.out.println("== LevelSelectLogicTest: " + fails + " falharam ==");
        System.exit(fails == 0 ? 0 : 1);
    }
}
