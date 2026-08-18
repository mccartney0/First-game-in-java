import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.traduvertgames.graficos.MissionHud;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.Menu;
import com.traduvertgames.main.WaveManager;
import com.traduvertgames.quest.HoldObjective;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.quest.SurviveObjective;

/** Aceitação do loop de combate das missões de resistência e defesa. */
public final class RpgExperienceBalanceTest {
    private static int passed;
    private static int failed;

    private RpgExperienceBalanceTest() {
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
        Game.gameState = "NORMAL";
        WaveManager.reset();
        Game.entities.clear();
        Game.enemies.clear();
        Game.entities.add(Game.player);

        SurviveObjective survive = new SurviveObjective("Resistir", "Teste", 5);
        survive.onLevelStart();
        for (int i = 0; i < 110; i++) {
            survive.update();
        }
        check("resistência gera mobs nos primeiros segundos", !Game.enemies.isEmpty());
        check("resistência informa ameaças no progresso", survive.getProgressText().contains("ameaças:"));

        QuestManager.prepareForLevel(2);
        HoldObjective hold = new HoldObjective();
        check("defesa da fase 2 mantém canal curto", hold.getChannelLimit() == 480);

        BufferedImage canvas = new BufferedImage(Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        MissionHud.render(graphics);
        graphics.dispose();
        check("HUD de missão renderiza em resolução padrão", true);

        Menu.pause = false;
        Game.gameState = "NORMAL";
        System.out.println("RpgExperienceBalanceTest: " + passed + " pass, " + failed + " fail");
        System.exit(failed == 0 ? 0 : 1);
    }
}
