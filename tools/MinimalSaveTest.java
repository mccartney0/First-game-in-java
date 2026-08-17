import java.io.*;
import java.util.Map;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.main.InventoryManager;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.world.World;
import com.traduvertgames.quest.QuestManager;

import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class MinimalSaveTest {

	private static Game g;

	public static void main(String[] args) throws Exception {
		SoundManager.unload();
		g = new Game();
		Game.setCurrentLevel(1);
		World.restartGame("level1.png");
		Game.player = new Player(200, 100, 16, 16, new BufferedImage(16, 16, TYPE_INT_ARGB));

		dump("DEPOIS do newGame+restart");

		Game.setCurrentLevel(3);
		System.out.println("antes save: level=" + Game.getCurrentLevel());
		boolean ok = SaveManager.saveCurrentGame();
		System.out.println("saveCurrentGame -> " + ok);
		dump("DEPOIS do save fase 3");

		boolean has = SaveManager.hasSlotSave(1);
		System.out.println("hasSlotSave(1)=" + has + " getSlotLevel=" + SaveManager.getSlotLevel(1));
		System.out.println("minimal: " + (has ? "PASS" : "FAIL"));
		System.out.flush();
		System.exit(0);
	}

	private static void dump(String label) {
		System.out.println("=== " + label + " ===");
		try (BufferedReader r = new BufferedReader(new FileReader("saves.json"))) {
			String l;
			while ((l = r.readLine()) != null) {
				System.out.println(l);
			}
		} catch (Exception e) {
			System.out.println("(sem arquivo)");
		}
	}
}
