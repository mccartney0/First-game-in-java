import com.traduvertgames.quest.StoryManager;

/**
 * Rodada 24a — QA da narrativa das Profundezas.
 *
 * Valida (a) que os ciclos-chave das Profundezas (profundidades 1, 4, 7, 10,
 * 13 — ou seja, níveis 9, 12, 15, 18, 21) têm títulos e linhas de lore
 * distintos da nomenclatura genérica; (b) que os níveis intermediários seguem
 * os dois estilos alternados; (c) que a trama progressiva se mantém coerente
 * (não repete um mesmo par título+lore em ciclos consecutivos); e (d) que as
 * fases de campanha (1-8) continuam intactas.
 */
public class Rodada24aDeepLoreTest {

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

	public static void main(String[] args) {
		// ---- (d) As fases de campanha continuam intactas ----
		check("Operação Socorro".equals(StoryManager.getPhaseLoreTitle(1)),
				"Fase 1: título de lore da campanha preservado");
		check("O Núcleo Central".equals(StoryManager.getPhaseLoreTitle(8)),
				"Fase 8: título de lore da campanha preservado");
		check(StoryManager.getPhaseLore(8).contains("batalha final"),
				"Fase 8: subtítulo de lore da campanha preservado");

		// ---- (a) Ciclos-chave têm identidade de lore própria ----
		String[] keyDepths = new String[] { "Camada Zero", "Camada Um",
				"Camada Dois", "Camada Três", "Camada Quatro" };
		for (int i = 0; i < keyDepths.length; i++) {
			int level = 9 + i * 3; // profundidades 1, 4, 7, 10, 13
			String title = StoryManager.getPhaseLoreTitle(level);
			check(title.startsWith(keyDepths[i]),
					"Profundidade " + (i + 1) + " (nível " + level
							+ "): título de ciclo-chave começa com '"
							+ keyDepths[i] + "' (obtido: '" + title + "')");
			String lore = StoryManager.getPhaseLore(level);
			check(lore != null && lore.length() > 40,
					"Profundidade " + (i + 1) + ": linha de lore substancial");
		}

		// ---- (b) Níveis intermediários seguem os estilos alternados ----
		// depth % 3 == 0 → estilo genérico "Setor" (ex.: profundidade 3, nível 11);
		// depth % 3 == 2 → "Câmara de Contenção" (ex.: profundidade 2, nível 10).
		check(StoryManager.getPhaseLoreTitle(11).startsWith("Profundidade 3")
					&& StoryManager.getPhaseLoreTitle(11).contains("Setor"),
			"Profundidade 3 (nível 11): estilo 'Setor'");
		check(StoryManager.getPhaseLoreTitle(10).contains("Câmara de Contenção"),
			"Profundidade 2 (nível 10): estilo 'Câmara de Contenção'");

		// ---- (c) A trama progressiva não se repete em ciclos consecutivos ----
		String previousTitle = StoryManager.getPhaseLoreTitle(9);
		String previousLore = StoryManager.getPhaseLore(9);
		boolean narrativeMoves = true;
		boolean loreUnique = true;
		for (int depth = 2; depth <= 15; depth++) {
			int level = 8 + depth;
			String title = StoryManager.getPhaseLoreTitle(level);
			String lore = StoryManager.getPhaseLore(level);
			if (title.equals(previousTitle)) {
				narrativeMoves = false;
			}
			if (depth > 1 && lore.equals(previousLore)) {
				loreUnique = false;
			}
			previousTitle = title;
			previousLore = lore;
		}
		check(narrativeMoves,
				"Trama: nenhum ciclo-chave repete o título do anterior");
		check(loreUnique,
				"Trama: linhas de lore não se repetem entre ciclos vizinhos");

		// ---- Rotatividade dos vetores: profundidades além do 13 reciclam ----
		String title16 = StoryManager.getPhaseLoreTitle(8 + 16);
		check(title16 != null && title16.length() > 0,
				"Profundidade 16 (nível 24): título gerado sem exceção ('"
						+ title16 + "')");

		System.out.println("Progresso: " + passed + " passaram, " + failed + " falharam");
		System.exit(failed == 0 ? 0 : 1);
	}
}
