import java.awt.image.BufferedImage;

import com.traduvertgames.dialogue.CommanderNpc;
import com.traduvertgames.dialogue.SupportNpcs;
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.main.Game;
import com.traduvertgames.quest.ContactObjective;
import com.traduvertgames.quest.DialogueObjective;
import com.traduvertgames.quest.BossHuntObjective;

/**
 * Teste lógico dos objetivos de missão (headless, sem JFrame).
 * Valida o fluxo: conversa com NPC marca o objetivo; coleta de artefatos
 * atualiza progresso; conclusão do objetivo só ocorre com ambos.
 */
public class QuestLogicTest {
    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean ok) {
        if (ok) {
            pass++;
            System.out.println("  PASS: " + name);
        } else {
            fail++;
            System.out.println("  FAIL: " + name);
        }
    }

    public static void main(String[] args) {
        // Inicializa o mínimo necessário para as entidades carregarem sprites.
        try {
            Game.spritesheet = new Spritesheet("/spritesheet.png");
        } catch (Exception ex) {
            // Headless: spritesheet não disponível — injetar imagem vazia via
            // reflexão no campo interno do Spritesheet (se possível) ou deixar
            // null apenas se as entidades não falharem em <clinit>.
            try {
                Spritesheet dummy = new Spritesheet("/spritesheet.png");
                java.lang.reflect.Field f = Spritesheet.class.getDeclaredField("spritesheet");
                f.setAccessible(true);
                f.set(dummy, new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB));
                Game.spritesheet = dummy;
            } catch (Exception ignored) {
                System.err.println("Spritesheet dummy indisponível");
            }
        }
        System.out.println("== Teste ContactObjective ==");
        ContactObjective co = new ContactObjective();
        co.onLevelStart();
        check("progresso inicial pede para falar", co.getProgressText().contains("Fale com"));
        check("nao completo sem fala", !co.isComplete());

        CommanderNpc ava = new CommanderNpc(48, 32);
        co.onDialogueFinished(ava);
        check("fala com Ava marca talkedToCommander",
                co.getProgressText().startsWith("Artefatos"));

        co.onQuestItemCollected(null);
        check("progresso 1/2 apos coleta", co.getProgressText().contains("1/2"));
        co.onQuestItemCollected(null);
        check("progresso 2/2 apos 2 coletas", co.getProgressText().contains("2/2"));
        check("completo com fala + 2 artefatos", co.isComplete());

        System.out.println("== Teste DialogueObjective ==");
        DialogueObjective lvl2 = new DialogueObjective(new BossHuntObjective(), "Engenheira Nia");
        lvl2.onLevelStart();
        check("nao completo sem fala (lvl2)", !lvl2.isComplete());
        lvl2.onDialogueFinished(SupportNpcs.engineer(64, 32));
        check("fala com Nia nao completa sozinha", !lvl2.isComplete());

        System.out.println("== Resumo: " + pass + " passaram, " + fail + " falharam ==");
        System.exit(fail == 0 ? 0 : 1);
    }
}
