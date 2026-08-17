import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.world.World;

/**
 * Teste de diagnóstico do waypoint da fase 1: instancia o jogo, imprime as
 * coordenadas do jogador, do alvo da seta e a distância/ângulo calculados
 * pelo MissionHud.drawWaypoint.
 */
public class WaypointDebugTest {
	public static void main(String[] args) throws Exception {
		// O construtor do Game cria a janela e inicia o thread de render; o
		// restartGameCommon já executa prepareForLevel(1) e placeStoryNpcs.
		new Game();
		Game.SCALE = 4;
		Thread.sleep(1200);
		// Entra direto na fase 1 (simula "Novo Jogo") para que o jogo fique em
		// NORMAL e a câmera siga o jogador como na partida real.
		Game.gameState = "NORMAL";
		Thread.sleep(800);
		System.out.println("gameState=" + Game.gameState);
		System.out.println("player world=(" + Game.player.getX() + "," + Game.player.getY() + ")");
		String hint = QuestManager.getTargetHint();
		System.out.println("targetHint=" + hint);
		Entity target = null;
		for (Entity e : Game.entities) {
			if (e instanceof InteractiveNpc && hint != null
					&& hint.equals(((InteractiveNpc) e).getName())) {
				target = e;
				break;
			}
		}
		if (target != null) {
			System.out.println("target world=(" + target.getX() + "," + target.getY() + ")");
			double dx = target.getX() + 8 - Game.player.getX() - 8;
			double dy = target.getY() + 8 - Game.player.getY() - 8;
			double dist = Math.sqrt(dx * dx + dy * dy);
			System.out.println("dx=" + dx + " dy=" + dy + " dist=" + dist + " m=" + (int) (dist / 16)
					+ " angleDeg=" + Math.toDegrees(Math.atan2(dy, dx)));
			System.out.println("target class=" + target.getClass().getSimpleName());
		} else {
			System.out.println("NO TARGET FOUND");
		}
		System.out.println("entities count=" + Game.entities.size());
		for (Entity e : Game.entities) {
			if (e instanceof InteractiveNpc) {
				System.out.println("npc=" + ((InteractiveNpc) e).getName() + " @("
						+ e.getX() + "," + e.getY() + ") class="
						+ e.getClass().getSimpleName());
			}
		}
		System.exit(0);
	}
}
