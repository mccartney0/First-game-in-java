import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.entities.Companion;

/** Verifica se o companion (tipo, HP e skin) é restaurado pelo loadSlot. */
public class CompanionLoadTest {

	public static void main(String[] args) {
		try {
			new Game();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			Thread.sleep(3000);
		} catch (InterruptedException ignored) {
		}
		boolean loaded = SaveManager.loadSlot(1);
		Companion active = Companion.getActive();
		System.out.println("loadSlot: " + loaded);
		if (active != null) {
			System.out.println("tipo: " + active.getType());
			System.out.println("skin: " + active.getSkin());
			System.out.println("hp: " + active.getHp());
			System.out.println("x: " + active.getX() + " y: " + active.getY());
		} else {
			System.out.println("COMPANION INATIVO após load!");
		}
		System.exit(0);
	}
}
