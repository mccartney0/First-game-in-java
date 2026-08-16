import java.io.File;
import java.util.HashMap;
import java.util.Map;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.entities.Companion;

/**
 * Teste de gravação e restauração de companion (save v3).
 * Grava um slot sintético via manipulação direta do JSON (mesmo formato que
 * SaveManager produz) e verifica que loadSlot restaura tipo, HP e skin.
 */
public class CompanionSaveRestoreTest {

	static int pass = 0;
	static int fail = 0;

	static void check(String label, boolean ok) {
		if (ok) { pass++; System.out.println("  PASS: " + label); }
		else { fail++; System.out.println("  FAIL: " + label); }
	}

	public static void main(String[] args) throws Exception {
		new File("saves.json").delete();
		Game game;
		try { game = new Game(); } catch (Exception e) { e.printStackTrace(); return; }
		Thread.sleep(2500);

		// Spawna um companion de teste e aplica uma skin.
		Companion.spawn(Companion.CompanionType.SHIELD_BOT, 80.0);
		Companion active = Companion.getActive();
		check("spawn ativo", active != null);
		active.setSkin(Companion.CompanionSkin.NEON);
		check("skin aplicada", active.getSkin() == Companion.CompanionSkin.NEON);

		// Simula o comportamento de saveCurrentGame manipulando diretamente o JSON:
		// SaveManager já existe no classpath; usamos reflexão simples NÃO é
		// necessária — escrevemos um saves.json com o mesmo formato v3 que o
		// SaveManager produz e invocamos loadSlot.
		StringBuilder sb = new StringBuilder();
		sb.append("{\"activeSlot\":1,\"bestRun\":{\"bestKills\":7,\"bestTimeMs\":1234,\"bestCombo\":3,\"bestScore\":420},\"slots\":[")
			.append("{\"id\":1,\"timestamp\":\"2026-08-16T00:00:00\",")
			.append("\"session\":{\"vida\":60,\"mana\":40,\"arma\":100,\"escudo\":50,")
			.append("\"inimigosMortos\":7,\"levelPlus\":0,\"level\":3,\"pontuacao\":210,\"recorde\":210,")
			.append("\"melhorCombo\":3,\"melhorComboSessao\":3,\"armaAtual\":0,\"armasDesbloqueadas\":3,")
			.append("\"companionType\":\"SHIELD_BOT\",\"companionHp\":80,\"companionSkin\":\"NEON\",")
			.append("\"energiaArma_PLASMA\":10,\"energiaArma_FROST\":0},")
			.append("\"progress\":{\"objectiveState\":{\"3\":\"IN_PROGRESS\"},\"npcDialogues\":{\"NIASPEAKS\":true}},")
			.append("\"campaign\":{}}]}");
		java.io.FileWriter w = new java.io.FileWriter("saves.json");
		w.write(sb.toString()); w.close();

		boolean loaded = SaveManager.loadSlot(1);
		check("loadSlot retorna true", loaded);
		active = Companion.getActive();
		if (active == null) {
			check("companion restaurado", false);
		} else {
			check("companion restaurado (SHIELD_BOT)", active.getType() == Companion.CompanionType.SHIELD_BOT);
			// saveCurrentGame clampa o HP a BASE_HP (40) ao gravar e ao restaurar;
		// um save v2 antigo com hp>40 também seria clampado ao máximo da criatura.
		check("HP restaurado (clampado a BASE_HP=40)", Math.abs(active.getHp() - 40.0) < 0.01);
			check("skin restaurada (NEON)", active.getSkin() == Companion.CompanionSkin.NEON);
		}
		check("fase restaurada (3)", game.getCurrentLevel() == 3);

		// Segundo cenário: save sem companion (v2) não deve quebrar.
		sb = new StringBuilder();
		sb.append("{\"activeSlot\":1,\"slots\":[")
			.append("{\"id\":1,\"timestamp\":\"t\",\"session\":{\"vida\":100,\"mana\":100,\"arma\":100,\"escudo\":100,")
			.append("\"inimigosMortos\":0,\"levelPlus\":0,\"level\":1,\"pontuacao\":0,\"recorde\":0,")
			.append("\"melhorCombo\":1,\"melhorComboSessao\":1,\"armaAtual\":0,\"armasDesbloqueadas\":1},")
			.append("\"progress\":{},\"campaign\":{}}]}");
		w = new java.io.FileWriter("saves.json");
		w.write(sb.toString()); w.close();
		loaded = SaveManager.loadSlot(1);
		check("load v2 sem companion retorna true", loaded);
		check("companion ausente após load v2", Companion.getActive() == null);
		check("fase restaurada (1)", game.getCurrentLevel() == 1);

		new File("saves.json").delete();
		System.out.println(fail == 0 ? "ALL PASSED (" + pass + ")" : pass + " passed, " + fail + " failed");
		System.exit(fail == 0 ? 0 : 1);
	}
}
