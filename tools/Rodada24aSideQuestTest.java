import java.awt.image.BufferedImage;

import com.traduvertgames.dialogue.BranchingNpc;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.quest.SideQuestManager;
import com.traduvertgames.quest.StoryManager;
import com.traduvertgames.world.World;

/**
 * Rodada 24a — QA das missões secundárias no modo infinito.
 *
 * Valida que as missões secundárias (Veterano Rex, Pesquisadora Lila e
 * Mercador Finn) funcionam nas Profundezas: o NPC correto spawna em cada
 * profundidade, a missão é ativa ao aceitar, o progresso acompanha kills e
 * não é apagado entre camadas (questId global do ciclo infinito).
 */
public class Rodada24aSideQuestTest {

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

	private static Game newGame() throws java.io.IOException {
		Game g = new Game();
		Game.SCALE = 4;
		Game.player = new Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		return g;
	}

	public static void main(String[] args) throws Exception {
	// ---- Campanha continua intacta: NPCs das fases 2-8 ----
	Game g1 = newGame();
	SideQuestManager.reset();
	World.restartGame("level3.png");
		boolean hasRex = Game.entities.stream()
				.anyMatch(e -> e instanceof com.traduvertgames.dialogue.InteractiveNpc
						&& ((com.traduvertgames.dialogue.InteractiveNpc) e).getName().contains("Rex"));
		check(hasRex, "Fase 3 (campanha): Veterano Rex presente");

		// ---- Modo infinito: NPC rotativo por profundidade ----
		SideQuestManager.reset();

		// Profundidade 1 (nível 9) → Rex
		Game.setCurrentLevel(9);
		Game.entities.clear();
		Game.SCALE = 4;
		Game.player = new Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		World.restartGame("level8.png"); // fallback: mapa da fase 8 para testes
		Game.setCurrentLevel(9);
		QuestManager.prepareForLevel(9);
		Game.entities.clear();
		StoryManager.placeStoryNpcs();
		boolean rex9 = Game.entities.stream()
				.anyMatch(e -> e instanceof com.traduvertgames.dialogue.InteractiveNpc
						&& ((com.traduvertgames.dialogue.InteractiveNpc) e).getName().contains("Rex"));
		check(rex9, "Profundidade 1 (nível 9): Veterano Rex spawna");
		check(!Game.entities.stream()
						.anyMatch(e -> e instanceof com.traduvertgames.dialogue.InteractiveNpc
								&& ((com.traduvertgames.dialogue.InteractiveNpc) e).getName().contains("Lila")),
				"Profundidade 1: apenas um NPC secundário (rotação sem poluição)");

		// ---- Profundidade 2 (nível 10) → Lila ----
		SideQuestManager.reset();
		Game.entities.clear();
		Game.setCurrentLevel(10);
		QuestManager.prepareForLevel(10);
		StoryManager.placeStoryNpcs();
		boolean lila10 = Game.entities.stream()
				.anyMatch(e -> e instanceof com.traduvertgames.dialogue.InteractiveNpc
						&& ((com.traduvertgames.dialogue.InteractiveNpc) e).getName().contains("Lila"));
		check(lila10, "Profundidade 2 (nível 10): Pesquisadora Lila spawna");

		// ---- Profundidade 3 (nível 11) → Finn ----
		SideQuestManager.reset();
		Game.entities.clear();
		Game.setCurrentLevel(11);
		QuestManager.prepareForLevel(11);
		StoryManager.placeStoryNpcs();
		boolean finn11 = Game.entities.stream()
				.anyMatch(e -> e instanceof com.traduvertgames.dialogue.InteractiveNpc
						&& ((com.traduvertgames.dialogue.InteractiveNpc) e).getName().contains("Finn"));
		check(finn11, "Profundidade 3 (nível 11): Mercador Finn spawna");

		// ---- QuestId global no modo infinito: missão não zera entre camadas ----
		SideQuestManager.reset();
		World.restartGame("level8.png");
		Game.setCurrentLevel(9);
		QuestManager.prepareForLevel(9);
		Game.entities.clear();
		StoryManager.placeStoryNpcs();
		// Simula o diálogo de aceite do Rex no nível 9
		InteractiveNpc rex = (InteractiveNpc) Game.entities.stream()
				.filter(e -> e instanceof InteractiveNpc
						&& ((InteractiveNpc) e).getName().contains("Rex"))
				.findFirst().orElse(null);
		boolean rexAvailable = rex != null;
		check(rexAvailable, "NPC Rex recuperável para o diálogo de aceite");
		if (rexAvailable) {
			// Aceita a missão (nó 0 → opção "Aceitar a missão")
			BranchingNpc branchRex = (BranchingNpc) rex;
			branchRex.selectChoice(0);
			check(SideQuestManager.get("rex_kills_deep") != null,
					"Missão rex_kills_deep registrada no modo infinito");
			check(SideQuestManager.isActive("rex_kills_deep"),
					"Missão da Profundidade 1 fica ativa ao aceitar");

			// Progrida 4 kills e avance para a profundidade 2
			com.traduvertgames.entities.Enemy phantom = com.traduvertgames.entities.Enemy.spawnRandomVariant(300, 200);
			for (int i = 0; i < 4; i++) {
				SideQuestManager.onEnemyKilled(phantom);
			}
			check(SideQuestManager.getProgress("rex_kills_deep") == 4,
					"Progresso da bounty acompanha os kills (4/10)");

							Game.setCurrentLevel(10);
		QuestManager.prepareForLevel(10);
		Game.entities.clear();
		StoryManager.placeStoryNpcs();
		BranchingNpc branchRex2 = (BranchingNpc) Game.entities.stream()
					.filter(e -> e instanceof BranchingNpc
							&& ((BranchingNpc) e).getName().contains("Rex"))
					.findFirst().orElse(null);
			// Reaparece na profundidade 2 e "re-aceita": o progresso NÃO pode zerar
			if (branchRex2 != null) {
				branchRex2.selectChoice(0);
			}
			check(SideQuestManager.getProgress("rex_kills_deep") == 4,
					"Progresso preservado entre profundidades (não reativa o zero)");

			// Conclui a bounty
			for (int i = 0; i < 6; i++) {
				SideQuestManager.onEnemyKilled(phantom);
			}
			check(SideQuestManager.isCompleted("rex_kills_deep"),
					"Bounty da Profundidade concluída com 10 kills (recompensa entregue)");
		}

		// ---- Na campanha o id continua por fase (sem colisão) ----
		SideQuestManager.reset();
		Game.setCurrentLevel(3);
		QuestManager.prepareForLevel(3);
		check(SideQuestManager.get("rex_kills_deep") == null,
				"Campanha: id da Profundidade não é registrado antes do aceite");
		SideQuestManager.reset();

		System.out.println("Progresso: " + passed + " passaram, " + failed + " falharam");
		System.exit(failed == 0 ? 0 : 1);
	}
}
