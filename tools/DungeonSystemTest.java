import java.io.File;

import com.traduvertgames.entities.DungeonExit;
import com.traduvertgames.entities.DungeonPortal;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.world.DungeonManager;
import com.traduvertgames.world.ProceduralDungeonGenerator;
import com.traduvertgames.world.ProceduralLevelGenerator;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.World;

/** Regressão da geração, entrada, boss e saída das dungeons regionais. */
public final class DungeonSystemTest {
    private static int passed;
    private static int failed;

    private DungeonSystemTest() {
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
        DungeonManager.reset();
        File surface = ProceduralLevelGenerator.generate(3);
        Game game = new Game();
        World.restartGameFromFile(surface.getAbsolutePath());
        Game.gameState = "NORMAL";

        int portals = count(DungeonPortal.class);
        check("superfície possui cinco portais de dungeon", portals >= 5);

        for (RpgWorldManager.RegionType region : RpgWorldManager.RegionType.values()) {
            File dungeon = ProceduralDungeonGenerator.generate(region, 3);
            check("dungeon válida para " + region, ProceduralDungeonGenerator.validate(
                    javax.imageio.ImageIO.read(dungeon)));
            check("boss regional distinto em " + region,
                    ProceduralDungeonGenerator.bossVariant(region) != null);
        }

        DungeonManager.requestEnter(RpgWorldManager.RegionType.RUINS);
        DungeonManager.processPendingTransition();
        check("entrada na dungeon muda o mapa", DungeonManager.isInDungeon()
                && World.WIDTH == ProceduralDungeonGenerator.MAP_WIDTH
                && World.HEIGHT == ProceduralDungeonGenerator.MAP_HEIGHT);
        check("dungeon carrega uma saída", count(DungeonExit.class) == 1);
        Enemy boss = findBoss();
        check("dungeon carrega um boss", boss != null
                && boss.getVariant() == ProceduralDungeonGenerator.bossVariant(RpgWorldManager.RegionType.RUINS));

        DungeonManager.requestExit();
        DungeonManager.processPendingTransition();
        check("saída fica bloqueada antes do boss", DungeonManager.isInDungeon());

        if (boss != null) {
            boss.destroySelf();
        }
        check("derrotar o boss libera a saída", DungeonManager.isBossDefeated());
        check("dungeon concluída fica registrada", DungeonManager.isRegionCompleted(RpgWorldManager.RegionType.RUINS));

        for (int i = 0; i < 50; i++) {
            DungeonManager.processPendingTransition();
        }
        DungeonManager.requestExit();
        DungeonManager.processPendingTransition();
        for (int i = 0; i < 50; i++) {
            DungeonManager.processPendingTransition();
        }
        check("retorno à superfície encerra a instância", !DungeonManager.isInDungeon()
                && World.WIDTH == ProceduralLevelGenerator.MAP_WIDTH);
        boolean saved = SaveManager.saveCurrentGame();
        check("save da conclusão regional retorna true", saved);
        DungeonManager.reset();
        Game reloaded = new Game();
        SaveManager.activeSlot = 1;
        boolean loaded = SaveManager.loadSlot(1);
        check("load da conclusão regional retorna true", loaded);
        check("conclusão do boss regional persiste", DungeonManager.isRegionCompleted(RpgWorldManager.RegionType.RUINS));

        System.out.println("DungeonSystemTest: " + passed + " pass, " + failed + " fail");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static int count(Class<?> type) {
        int total = 0;
        for (Entity entity : Game.entities) {
            if (type.isInstance(entity)) {
                total++;
            }
        }
        return total;
    }

    private static Enemy findBoss() {
        for (Enemy enemy : Game.enemies) {
            if (enemy.isBoss()) {
                return enemy;
            }
        }
        return null;
    }
}
