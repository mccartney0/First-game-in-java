import java.util.ArrayList;
import java.util.List;

import com.traduvertgames.quest.StoryManager;

/**
 * Valida a narrativa da rodada 13 sem instanciar a janela do jogo (a
 * validação do posicionamento físico dos NPCs no mapa é feita pelo jogo
 * rodando; aqui garantimos que a camada de lore é completa e consistente).
 */
public class StoryNpcPlacementTest {

	static int passed = 0;
	static int failed = 0;

	static void check(String desc, boolean ok) {
		if (ok) {
			passed++;
			System.out.println("[PASS] " + desc);
		} else {
			failed++;
			System.out.println("[FAIL] " + desc);
		}
	}

	public static void main(String[] args) {
		System.out.println("=== StoryNpcPlacementTest (lore + NPCs por fase) ===");

		// 1. Toda fase da campanha (1..8) e o modo sobrevivência (>=9) têm lore.
		List<String> seenTitles = new ArrayList<String>();
		for (int level = 1; level <= 10; level++) {
			String title = StoryManager.getPhaseLoreTitle(level);
			String lore = StoryManager.getPhaseLore(level);
			String label = StoryManager.getStoryNpcsLabel(level);
			check("Fase " + level + ": título de lore definido ('" + title + "')",
					title != null && title.length() > 3);
			check("Fase " + level + ": lore com texto suficiente ("
					+ (lore == null ? "null" : lore.length()) + " chars)",
					lore != null && lore.length() > 30);
			check("Fase " + level + ": rótulo narrativo de NPCs definido ('" + label + "')",
					label != null && label.length() > 0);
			if (level <= 8) {
				boolean unique = !seenTitles.contains(title);
				check("Fase " + level + ": título de lore único", unique);
				if (unique) {
					seenTitles.add(title);
				}
			}
		}

		// 2. placeStoryNpcs não lança exceção quando não há NPCs registrados
		//    (situação de fase sem NPC temático, como as de sobrevivência).
		try {
			StoryManager.placeStoryNpcs();
			check("placeStoryNpcs sem NPCs registrados: não lança exceção", true);
		} catch (Throwable t) {
			check("placeStoryNpcs sem NPCs registrados: não lança exceção — " + t, false);
		}

		System.out.println("StoryNpcPlacementTest: " + passed + " passaram, "
				+ failed + " falharam");
	}
}
