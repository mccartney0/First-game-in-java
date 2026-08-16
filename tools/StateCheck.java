import java.io.File;
import java.lang.reflect.Field;

/** Inicia o jogo e imprime o gameState via reflexão repetidamente. */
public class StateCheck {
	public static void main(String[] args) throws Exception {
		new File("saves.json").delete();
		new File("save.txt").delete();
		Class<?> gameClass = Class.forName("com.traduvertgames.main.Game");
		new java.lang.Thread(() -> {
			try { Thread.sleep(2500); System.exit(0); } catch (InterruptedException ignored) {}
		}).start();
		for (int i = 0; i < 25; i++) {
			Thread.sleep(100);
			Field f = gameClass.getField("gameState");
			System.out.println("t=" + (i * 100) + "ms gameState=" + f.get(null));
		}
	}
}
