import java.io.File;

import javax.imageio.ImageIO;

import com.traduvertgames.dialogue.BranchingNpc;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.entities.RegionalNpcs;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.quest.SideQuestManager;
import com.traduvertgames.world.ProceduralLevelGenerator;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.World;

/** Regressão dos NPCs e missões secundárias regionais. */
public final class RegionalQuestTest {
    private static int passed;
    private static int failed;

    private RegionalQuestTest() {
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
        SideQuestManager.reset();
        File map = ProceduralLevelGenerator.generate(4);
        Game game = new Game();
        World.restartGameFromFile(map.getAbsolutePath());

        int regionalCount = 0;
        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            String name = RegionalNpcs.getNameForRegion(region);
            boolean found = false;
            for (com.traduvertgames.entities.Entity entity : Game.entities) {
                if (entity instanceof InteractiveNpc && name.equals(((InteractiveNpc) entity).getName())) {
                    found = true;
                    regionalCount++;
                    break;
                }
            }
            check("NPC regional presente: " + name, found);
        }
        check("seis NPCs regionais carregados", regionalCount == 6);

        BranchingNpc refugeNpc = (BranchingNpc) RegionalNpcs.create(RpgWorldManager.RegionType.REFUGE, 64, 64);
        refugeNpc.startInteraction();
        refugeNpc.selectChoice(0);
        check("missão do Refúgio pode ser aceita", SideQuestManager.getActiveQuestTitle().equals("Suprimentos para o Refúgio"));
        check("descrição da missão regional existe", !SideQuestManager.getActiveQuestDescription().isEmpty());
        refugeNpc.finishInteraction();
        check("NPC regional permanece disponível após conversar", !refugeNpc.hasFinished());

        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            BranchingNpc npc = (BranchingNpc) RegionalNpcs.create(region, 64, 64);
            npc.startInteraction();
            npc.selectChoice(0);
        }
        String[] questIds = {
                "region_refuge_supply", "region_ruins_scrap", "region_marsh_medicine",
                "region_tundra_beacon", "region_sanctuary_data", "region_core_purge"
        };
        int registered = 0;
        for (String questId : questIds) {
            if (SideQuestManager.isRegistered(questId)) {
                registered++;
            }
        }
        check("seis missões regionais registradas", registered == 6);
        SideQuestManager.addProgress("region_ruins_scrap", 2);
        boolean saved = SaveManager.saveCurrentGame();
        check("save regional retorna true", saved);
        SideQuestManager.reset();
        Game reloaded = new Game();
        SaveManager.activeSlot = 1;
        boolean loaded = SaveManager.loadSlot(1);
        check("load regional retorna true", loaded);
        check("definição regional restaura após load", SideQuestManager.isRegistered("region_ruins_scrap"));
        check("progresso regional restaura após load", SideQuestManager.getProgress("region_ruins_scrap") == 2);

        System.out.println("RegionalQuestTest: " + passed + " pass, " + failed + " fail");
        System.exit(failed == 0 ? 0 : 1);
    }

}
