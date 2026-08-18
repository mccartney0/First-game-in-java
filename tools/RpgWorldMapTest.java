import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import javax.imageio.ImageIO;

import com.traduvertgames.graficos.MiniMap;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.ProceduralLevelGenerator;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.World;

/** Regressão estrutural do mundo RPG procedural ampliado. */
public final class RpgWorldMapTest {
    private static int passed;
    private static int failed;

    private RpgWorldMapTest() {
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
        long start = System.nanoTime();
        File file = ProceduralLevelGenerator.generate(7);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        BufferedImage map = ImageIO.read(file);

        check("mapa RPG tem 96x64 tiles", map.getWidth() == 96 && map.getHeight() == 64);
        check("mapa RPG passa na validação estrutural", ProceduralLevelGenerator.validate(map));
        check("geração determinística termina em até 2s", elapsedMs < 2_000L);
        check("seis regiões registradas", RpgWorldManager.getPointsOfInterest().stream()
                .map(RpgWorldManager.PointOfInterest::getRegion).distinct().count() == 6);
        check("pelo menos cinco bolsões de mobs", RpgWorldManager.getMobAreas().size() >= 5);
        check("pelo menos seis pontos de interesse", RpgWorldManager.getPointsOfInterest().size() >= 6);

        int terrainCount = 0;
        int enemyCount = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int rgb = map.getRGB(x, y);
                if (rgb == 0xFF7CB342 || rgb == 0xFF6D4C41 || rgb == 0xFFB0BEC5) {
                    terrainCount++;
                }
                if (rgb == 0xFFFF0000 || rgb == 0xFF3F51B5 || rgb == 0xFF009688
                        || rgb == 0xFFF4511E || rgb == 0xFFFFC800) {
                    enemyCount++;
                }
            }
        }
        check("mapa contém terrenos regionais", terrainCount > 500);
        check("mapa contém áreas com mobs", enemyCount >= 12);

        check("spawn conectado ao refúgio", reachable(map, 3, 3,
                RpgWorldManager.getBounds(RpgWorldManager.RegionType.REFUGE).centerX(),
                RpgWorldManager.getBounds(RpgWorldManager.RegionType.REFUGE).centerY()));
        check("spawn conectado às ruínas", reachable(map, 3, 3,
                RpgWorldManager.getBounds(RpgWorldManager.RegionType.RUINS).centerX(),
                RpgWorldManager.getBounds(RpgWorldManager.RegionType.RUINS).centerY()));
        check("spawn conectado ao pântano", reachable(map, 3, 3,
                RpgWorldManager.getBounds(RpgWorldManager.RegionType.MARSH).centerX(),
                RpgWorldManager.getBounds(RpgWorldManager.RegionType.MARSH).centerY()));
        check("spawn conectado ao núcleo", reachable(map, 3, 3,
                RpgWorldManager.getBounds(RpgWorldManager.RegionType.CORE).centerX(),
                RpgWorldManager.getBounds(RpgWorldManager.RegionType.CORE).centerY()));

        Game game = new Game();
        World.restartGameFromFile(file.getAbsolutePath());
        check("World carrega o mapa grande", World.WIDTH == 96 && World.HEIGHT == 64);
        check("World preserva o spawn do jogador", Game.player != null
                && Game.player.getX() == 3 * World.TILE_SIZE
                && Game.player.getY() == 3 * World.TILE_SIZE);
        check("regiões permanecem ativas após o parsing", RpgWorldManager.isActive());
        check("spawn pertence ao Refúgio", RpgWorldManager.updatePlayerPosition(
                (int) Game.player.getX(), (int) Game.player.getY())
                && RpgWorldManager.getCurrentRegion() == RpgWorldManager.RegionType.REFUGE);
        check("tile de chão regional é caminhável", World.isFree((int) Game.player.getX(), (int) Game.player.getY(), 0));
        check("World cria inimigos a partir dos bolsões", Game.enemies.size() >= 8);
        check("World cria POIs como entidades", Game.entities.size() > Game.enemies.size() + 1);
        Game.gameState = "NORMAL";
        BufferedImage hudBuffer = new BufferedImage(Game.WIDTH * Game.SCALE,
                Game.HEIGHT * Game.SCALE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D hudGraphics = hudBuffer.createGraphics();
        try {
            MiniMap.render(hudGraphics);
            check("minimapa renderiza o mundo grande", true);
        } catch (RuntimeException ex) {
            check("minimapa renderiza o mundo grande", false);
            ex.printStackTrace(System.out);
        } finally {
            hudGraphics.dispose();
        }

        System.out.println("RpgWorldMapTest: " + passed + " pass, " + failed + " fail (" + elapsedMs + " ms)");
        System.exit(failed == 0 ? 0 : 1);
    }

    private static boolean reachable(BufferedImage map, int startX, int startY, int goalX, int goalY) {
        Queue<int[]> queue = new ArrayDeque<int[]>();
        Set<Long> visited = new HashSet<Long>();
        queue.add(new int[] { startX, startY });
        visited.add(key(startX, startY));
        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        while (!queue.isEmpty()) {
            int[] current = queue.remove();
            if (current[0] == goalX && current[1] == goalY) {
                return true;
            }
            for (int[] direction : directions) {
                int nextX = current[0] + direction[0];
                int nextY = current[1] + direction[1];
                if (nextX < 0 || nextY < 0 || nextX >= map.getWidth() || nextY >= map.getHeight()) {
                    continue;
                }
                if (!walkable(map.getRGB(nextX, nextY))) {
                    continue;
                }
                long key = key(nextX, nextY);
                if (visited.add(key)) {
                    queue.add(new int[] { nextX, nextY });
                }
            }
        }
        return false;
    }

    private static boolean walkable(int rgb) {
        return rgb != 0xFFFFFFFF && rgb != 0xFF808080;
    }

    private static long key(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }
}
