import java.lang.reflect.Method;

import com.traduvertgames.main.ShopManager;

/** Valida os itens de skin de companion no ShopManager (rodada 7). */
public class ShopSkinLogicTest {

	private static int passed = 0;
	private static int failed = 0;

	private static void check(boolean condition, String description) {
		if (condition) {
			passed++;
			System.out.println("PASS: " + description);
		} else {
			failed++;
			System.out.println("FAIL: " + description);
		}
	}

	public static void main(String[] args) throws Exception {
		ShopManager.ShopItem[] items = ShopManager.ShopItem.values();

		ShopManager.ShopItem skinDourado = null;
		ShopManager.ShopItem skinNeon = null;
		ShopManager.ShopItem skinCarmesim = null;
		for (ShopManager.ShopItem item : items) {
			if (item.name().startsWith("SKIN_")) {
				System.out.println("  item loja: " + item.name() + " -> " + item.label + " (" + item.price + ")");
			}
			if ("SKIN_DOURADO".equals(item.name())) {
				skinDourado = item;
			}
			if ("SKIN_NEON".equals(item.name())) {
				skinNeon = item;
			}
			if ("SKIN_CARMESIM".equals(item.name())) {
				skinCarmesim = item;
			}
		}

		check(skinDourado != null, "SKIN_DOURADO existe no ShopItem");
		check(skinNeon != null, "SKIN_NEON existe no ShopItem");
		check(skinCarmesim != null, "SKIN_CARMESIM existe no ShopItem");
		check(skinNeon != null && skinNeon.price > 0 && skinNeon.price < 2000,
				"SKIN_NEON tem preço razoável (" + (skinNeon != null ? skinNeon.price : -1) + ")");
		check(skinNeon != null && skinNeon.label != null && skinNeon.label.contains("Neon"),
				"SKIN_NEON tem label descritivo");

		// Método de aplicação de skin existe (applyCompanionSkin é privado;
		// validar pelo enum CompanionSkin acessível via classe Companion).
		Class<?> skinClass = Class.forName("com.traduvertgames.entities.Companion$CompanionSkin");
		Object[] skins = skinClass.getEnumConstants();
		check(skins.length == 4, "CompanionSkin tem 4 valores (PADRAO + 3 skins): " + skins.length);

		System.out.println("== Resultados: " + passed + " passaram, " + failed + " falharam ==");
		if (failed > 0) {
			System.exit(1);
		}
	}
}
