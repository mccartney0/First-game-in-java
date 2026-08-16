import com.traduvertgames.main.*;
import com.traduvertgames.quest.*;
import com.traduvertgames.entities.*;

public class AutoValidate {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("[PASS] " + name); }
        else { failed++; System.out.println("[FAIL] " + name); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== AutoValidate Fase 10 ===");

        // 1. OptionsConfig - som habilitado por padrão
        check("OptionsConfig.isSoundEnabled default true", OptionsConfig.isSoundEnabled());
        check("OptionsConfig.getSoundVolume default 0.0dB", Math.abs(OptionsConfig.getSoundVolume() - 0.0f) < 0.01f);

        // 2. QuestManager - títulos e modo sobrevivência
        check("QuestManager.getPhaseTitle(0) not null", QuestManager.getPhaseTitle(0) != null);
        check("QuestManager.getPhaseTitle(1) not null", QuestManager.getPhaseTitle(1) != null);
        check("QuestManager.getPhaseTitle(6) not null", QuestManager.getPhaseTitle(6) != null);
        check("QuestManager.getPhaseTitle(7) not null", QuestManager.getPhaseTitle(7) != null);
        check("QuestManager.isSurvivalMode() static no-arg",
                QuestManager.isSurvivalMode() == true || QuestManager.isSurvivalMode() == false);

        // 3. SoundManager - classe estática, sem getInstance; eventos reais
        check("SoundManager.Event.SHOT exists", SoundManager.Event.SHOT != null);
        check("SoundManager.Event.KILL exists", SoundManager.Event.KILL != null);
        check("SoundManager.Event.TELEPORT exists", SoundManager.Event.TELEPORT != null);
        check("SoundManager.Event.BOSS_DEFEAT exists", SoundManager.Event.BOSS_DEFEAT != null);
        check("SoundManager.Event.LEVELUP exists", SoundManager.Event.LEVELUP != null);
        check("SoundManager.Event.SHOP exists", SoundManager.Event.SHOP != null);
        check("SoundManager.Event.WAVE exists", SoundManager.Event.WAVE != null);
        check("SoundManager.Event.TUTORIAL_STEP exists", SoundManager.Event.TUTORIAL_STEP != null);
        check("SoundManager.Event.TUTORIAL_DONE exists", SoundManager.Event.TUTORIAL_DONE != null);
        check("SoundManager.Event.HIT exists", SoundManager.Event.HIT != null);
        check("SoundManager.Event.PICKUP exists", SoundManager.Event.PICKUP != null);
        check("SoundManager.Event.BOSS_ALERT exists", SoundManager.Event.BOSS_ALERT != null);
        check("SoundManager.Event.DAMAGE exists", SoundManager.Event.DAMAGE != null);
        check("SoundManager.Event.LASER exists", SoundManager.Event.LASER != null);

        // 4. SoundManager.play() não crasha (mesmo sem display de áudio em Xvfb)
        boolean soundPlayOk = true;
        try {
            for (SoundManager.Event e : SoundManager.Event.values()) {
                SoundManager.play(e);
            }
        } catch (Exception ex) {
            soundPlayOk = false;
            System.out.println("[WARN] SoundManager.play threw: " + ex);
        }
        check("SoundManager.play() all events no exception", soundPlayOk);

        // 5. WaveManager - métodos estáticos, sem getInstance
        check("WaveManager.getCurrentWaveNumber() static >= 0", WaveManager.getCurrentWaveNumber() >= 0);
        check("WaveManager.isArenaMode() static boolean",
                WaveManager.isArenaMode() == true || WaveManager.isArenaMode() == false);

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }
}
