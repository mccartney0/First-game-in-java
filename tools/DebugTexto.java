import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.quest.QuestManager;

public class DebugTexto {
	public static void main(String[] args) throws Exception {
		new java.io.File("saves.json").delete();
		try {
			Game.spritesheet = new Spritesheet("/spritesheet.png");
		} catch (Exception ex) {
			try {
				Spritesheet dummy = new Spritesheet("/spritesheet.png");
				Field f = Spritesheet.class.getDeclaredField("spritesheet");
				f.setAccessible(true);
				f.set(dummy, new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB));
				Game.spritesheet = dummy;
			} catch (Exception ignored) {}
		}
		Field f2 = SaveManager.class.getDeclaredField("activeSlot");
		f2.setAccessible(true);
		f2.set(null, 1);
		QuestManager.prepareForLevel(2);
		SaveManager.saveCurrentGame();
		QuestManager.prepareForLevel(3);
		String t = SaveManager.getSlotObjectiveText(1);
		System.out.println("OBJECTIVE_TEXT=[" + t + "]");
		System.out.println("SLOT_LEVEL=" + SaveManager.getSlotLevel(1));
		new java.io.File("saves.json").delete();
	}
}
