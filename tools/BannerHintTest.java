import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.traduvertgames.main.Game;
import com.traduvertgames.graficos.MissionBanner;

/**
 * Valida que ao carregar a fase 2 (DialogueObjective: BossHunt + Engenheira Nia)
 * o banner de dica aparece orientando o jogador a falar com a NPC.
 */
public class BannerHintTest {
    static int fails = 0;
    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (!ok) fails++;
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "false");
        Game g = new Game();
        Field fLife = MissionBanner.class.getDeclaredField("life");
        fLife.setAccessible(true);
        Field fTitle = MissionBanner.class.getDeclaredField("title");
        fTitle.setAccessible(true);
        Field fSub = MissionBanner.class.getDeclaredField("subtitle");
        fSub.setAccessible(true);

        // Fase 1 → fase 2 via restartGame (como LevelSelectScreen faria)
        g.setCurrentLevel(2);
        com.traduvertgames.world.World.restartGame("level2.png");

        check("fase = 2", com.traduvertgames.quest.QuestManager.getCurrentLevel() == 2);
        String title = (String) fTitle.get(null);
        String sub = (String) fSub.get(null);
        int life = (Integer) fLife.get(null);
        check("banner ativo após load da fase 2", life > 0);
        check("banner diz Fale com Engenheira Nia", sub != null && sub.contains("Engenheira Nia") && sub.contains("tecla R"));

        // Fase 3 (Ritual + Pesquisador Ivo) também deve mostrar dica
        com.traduvertgames.graficos.MissionBanner.reset();
        g.setCurrentLevel(3);
        com.traduvertgames.world.World.restartGame("level3.png");
        sub = (String) fSub.get(null);
        life = (Integer) fLife.get(null);
        check("fase 3 mostra dica (Pesquisador Ivo)", life > 0 && sub != null && sub.contains("Pesquisador Ivo"));

        if (fails == 0) System.out.println("== BannerHintTest: TODOS PASSARAM ==");
        else System.out.println("== BannerHintTest: " + fails + " falharam ==");
        System.exit(fails == 0 ? 0 : 1);
    }
}
