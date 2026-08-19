import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.traduvertgames.main.Menu;
import com.traduvertgames.main.OptionsConfig;
import com.traduvertgames.quest.HoldObjective;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.quest.SurviveObjective;

/**
 * Regressão da rodada 27: curva de dificuldade e polimento de UX.
 * Valida os parâmetros de defesa/sobrevivência, as opções de áudio e os textos
 * essenciais que orientam o jogador nos estados tutorial e game over.
 */
public class Rodada27BalanceUxTest {
    private static int passed;
    private static int failed;

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    private static String source(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        QuestManager.prepareForLevel(2);
        check("fase 2: canal de defesa reduzido para 8s", new HoldObjective().getChannelLimit() == 480);

        QuestManager.prepareForLevel(7);
        check("fase 7: canal de defesa final dura 9s", new HoldObjective().getChannelLimit() == 540);

        SurviveObjective survive = new SurviveObjective("teste", "teste", 35);
        Field duration = SurviveObjective.class.getDeclaredField("durationFrames");
        duration.setAccessible(true);
        check("fase 6: sobrevivência configurada para 35s", duration.getInt(survive) == 35 * 60);

        Field labels = Menu.class.getDeclaredField("OPTIONS_LABELS");
        labels.setAccessible(true);
        check("opções: música, efeitos, volumes, dificuldade e idioma", ((String[]) labels.get(null)).length == 7);

        float before = OptionsConfig.getSoundVolume();
        OptionsConfig.adjustSoundVolume(2);
        check("volume de efeitos ajustável", OptionsConfig.getSoundVolume() == before + 4.0f);
        OptionsConfig.adjustSoundVolume(-2);
        check("volume de efeitos retorna ao padrão", OptionsConfig.getSoundVolume() == before);

        String onboarding = source("src/com/traduvertgames/main/OnboardingManager.java");
        check("tutorial enumera os três passos", onboarding.contains("PASSO 1/3")
                && onboarding.contains("PASSO 2/3") && onboarding.contains("PASSO 3/3"));
        check("tutorial explica como pular", onboarding.contains("SPACE: pular tutorial"));

        String menu = source("src/com/traduvertgames/main/Menu.java");
        check("Como jogar reflete fase 2", menu.contains("ative o beacon, defenda a área"));
        check("Como jogar reflete fases finais", menu.contains("Fase 6–8: resista, sabote"));

        String game = source("src/com/traduvertgames/main/Game.java");
        check("game over explica confirmação e retorno", game.contains("ENTER: confirmar — ESC: voltar ao menu"));
        check("pausa pode ser alternada com P", game.contains("Menu.closePauseScreen()"));

        String sound = source("src/com/traduvertgames/main/SoundManager.java");
        check("volume atualiza clips carregados", sound.contains("refreshVolume()"));

        System.out.println("Rodada27BalanceUxTest: " + passed + " passaram, " + failed + " falharam");
        System.exit(failed == 0 ? 0 : 1);
    }
}
