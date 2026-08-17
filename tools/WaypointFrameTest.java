import java.awt.image.BufferedImage;

import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.world.Camera;

/**
 * Reproduz o cenário da screenshot: abre a fase 1, simula alguns frames de
 * jogo parado e imprime onde a seta amarela do waypoint aparece no buffer
 * (borda) — reproduzindo "6m para cima à esquerda".
 */
public class WaypointFrameTest {
	public static void main(String[] args) throws Exception {
		new Game();
		Game.SCALE = 4;
		Thread.sleep(1000);
		Game.gameState = "NORMAL";
		// Fluxo real de carregamento de fase: o restartGameCommon é o caminho
		// que também executa placeStoryNpcs (o init do construtor não o faz).
		com.traduvertgames.world.World.restartGame("level1.png");
		Thread.sleep(1500);

		System.out.println("player=(" + Game.player.getX() + "," + Game.player.getY()
				+ ") camera=(" + Camera.x + "," + Camera.y + ")");
		System.out.println("targetHint=" + QuestManager.getTargetHint());
		for (Entity e : Game.entities) {
			if (e instanceof InteractiveNpc) {
				System.out.println("npc=" + ((InteractiveNpc) e).getName() + " @("
						+ e.getX() + "," + e.getY() + ")");
			}
		}
		// Dump do buffer: procura pixels amarelos (seta é 0xFFEB3B) fora da HUD.
		BufferedImage img = Game.getBufferImage();
		if (img == null) {
			System.err.println("sem acesso ao buffer público");
			System.exit(1);
			return;
		}
		int w = img.getWidth(), h = img.getHeight();
		System.out.println("buffer=" + w + "x" + h);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = img.getRGB(x, y) & 0xFFFFFF;
				if (p == 0xFFEB3B || p == 0xFFD700) {
					System.out.println("yellowPixel @(" + x + "," + y + ")");
				}
			}
		}
		System.exit(0);
	}
}
