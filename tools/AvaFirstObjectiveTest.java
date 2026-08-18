import java.util.ArrayDeque;

import com.traduvertgames.dialogue.CommanderNpc;
import com.traduvertgames.dialogue.DialogueManager;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.world.World;

/**
 * Regressão do primeiro objetivo da campanha.
 *
 * O teste sobe a fase 1 real, localiza Ava depois do posicionamento narrativo,
 * confirma que o tile é caminhável e alcançável pelo spawn, e abre o diálogo
 * pelo mesmo fluxo usado pela tecla R.
 */
public final class AvaFirstObjectiveTest {
    private static int passed;
    private static int failed;

    private AvaFirstObjectiveTest() {
    }

    private static void check(String description, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + description);
        } else {
            failed++;
            System.out.println("[FAIL] " + description);
        }
    }

    private static CommanderNpc findAva() {
        for (Entity entity : Game.entities) {
            if (entity instanceof CommanderNpc) {
                return (CommanderNpc) entity;
            }
        }
        return null;
    }

    private static int reachableSteps(int startX, int startY, int targetX, int targetY) {
        if (!World.isValidTile(startX, startY) || !World.isValidTile(targetX, targetY)
                || !World.isFree(startX * World.TILE_SIZE, startY * World.TILE_SIZE, 0)
                || !World.isFree(targetX * World.TILE_SIZE, targetY * World.TILE_SIZE, 0)) {
            return -1;
        }
        int[][] distance = new int[World.WIDTH][World.HEIGHT];
        for (int x = 0; x < World.WIDTH; x++) {
            java.util.Arrays.fill(distance[x], -1);
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { startX, startY });
        distance[startX][startY] = 0;
        while (!queue.isEmpty()) {
            int[] current = queue.removeFirst();
            int x = current[0];
            int y = current[1];
            if (x == targetX && y == targetY) {
                return distance[x][y];
            }
            int[][] neighbors = { { x + 1, y }, { x - 1, y }, { x, y + 1 }, { x, y - 1 } };
            for (int[] next : neighbors) {
                int nx = next[0];
                int ny = next[1];
                if (World.isValidTile(nx, ny) && distance[nx][ny] < 0
                        && World.isFree(nx * World.TILE_SIZE, ny * World.TILE_SIZE, 0)) {
                    distance[nx][ny] = distance[x][y] + 1;
                    queue.addLast(new int[] { nx, ny });
                }
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        new Game();
        Thread.sleep(700);
        World.restartGame("level1.png");
        Game.gameState = "NORMAL";
        Thread.sleep(500);

        CommanderNpc ava = findAva();
        check("Ava é carregada na fase 1", ava != null);
        check("o alvo inicial é Comandante Ava", "Comandante Ava".equals(QuestManager.getTargetHint()));
        if (ava == null) {
            System.out.println("AvaFirstObjectiveTest: " + passed + " passaram, " + failed + " falharam");
            System.exit(1);
        }

        int playerTileX = Game.player.getX() / World.TILE_SIZE;
        int playerTileY = Game.player.getY() / World.TILE_SIZE;
        int avaTileX = ava.getX() / World.TILE_SIZE;
        int avaTileY = ava.getY() / World.TILE_SIZE;
        int steps = reachableSteps(playerTileX, playerTileY, avaTileX, avaTileY);
        System.out.println("spawnTile=(" + playerTileX + "," + playerTileY + ") avaTile=(" + avaTileX + ","
                + avaTileY + ") steps=" + steps);
        check("Ava está em piso caminhável", World.isFree(ava.getX(), ava.getY(), 0));
        check("Ava é alcançável pelo spawn", steps >= 0);

        Game.player.setX(ava.getX());
        Game.player.setY(ava.getY());
        check("Ava entra no raio de interação", ava.isWithinReach());
        check("o fluxo R seleciona Ava", DialogueManager.startNearestDialogue() == ava);
        while (DialogueManager.isActive()) {
            DialogueManager.advance();
        }
        check("o diálogo muda o objetivo para os artefatos", QuestManager.getObjectiveProgress().startsWith("Artefatos"));

        System.out.println("AvaFirstObjectiveTest: " + passed + " passaram, " + failed + " falharam");
        System.exit(failed == 0 ? 0 : 1);
    }
}
