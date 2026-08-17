// -*- coding: utf-8 -*-
import java.lang.reflect.Field;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.main.InventoryManager;
import com.traduvertgames.dialogue.BranchingNpc;
import com.traduvertgames.dialogue.BranchingNpc.DialogueNode;
import com.traduvertgames.dialogue.DialogueManager;
import com.traduvertgames.quest.SideQuestManager;
import com.traduvertgames.quest.SideQuestManager.Type;
import com.traduvertgames.quest.SideQuestManager.Reward;
import com.traduvertgames.quest.SideQuestManager.SideQuest;

/**
 * Valida os diálogos ramificados (BranchingNpc) e as missões secundárias
 * (SideQuestManager) da rodada 22:
 * 1) árvore de nós e escolhas executam a ação correta;
 * 2) diálogo de BranchingNpc via DialogueManager mostra as escolhas;
 * 3) missão KILL_N avança com kills e conclui com recompensa;
 * 4) missão COLLECT_N acompanha o inventário;
 * 5) missão DELIVER consome o item e completa;
 * 6) serialização/persistência de missões.
 */
public class BranchingNpcTest {
	static int fails = 0;

	static void check(String name, boolean ok) {
		System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
		if (!ok) {
			fails++;
		}
	}

	/** NPC de teste com missão de kills ramificada. */
	static BranchingNpc sampleNpc() {
		return new BranchingNpc(100, 100, "Teste NPC", new java.awt.Color(120, 60, 40),
				new java.awt.Color(255, 224, 178)) {
			@Override
			protected DialogueNode[] buildNodes() {
				return new DialogueNode[] {
						new DialogueNode("Bem-vindo, piloto!",
								new String[] { "Missão", "Adeus", null },
								new int[] { 1, 2, -1 },
								new Runnable[] {
										() -> SideQuestManager.activate("t_kills"), null, null }),
						new DialogueNode("Derrube 5 inimigos e eu te recompensarei.",
								new String[] { null, null, null },
								new int[] { -1, -1, -1 },
								new Runnable[] { null, null, null }),
						new DialogueNode("Até a próxima, piloto.",
								new String[] { null, null, null },
								new int[] { -1, -1, -1 },
								new Runnable[] { null, null, null }),
				};
			}
		};
	}

	public static void main(String[] args) throws Exception {
		SoundManager.unload();
		Game g = new Game();
		Game.setCurrentLevel(2);
		g.gameState = "NORMAL";
		com.traduvertgames.world.World.restartGame("level2.png");
		com.traduvertgames.quest.QuestManager.onLevelLoaded();
		com.traduvertgames.main.ShopManager.close();
		InventoryManager.reset();
		SideQuestManager.reset();

		SideQuestManager.register(new SideQuest("t_kills", Type.KILL_N, null, 5,
				new Reward(20, 10, 0, 50)));
		SideQuestManager.register(new SideQuest("t_collect", Type.COLLECT_N,
				InventoryManager.ItemType.ENERGY_CELL, 2,
				new Reward(0, 15, 40, 60)));
		SideQuestManager.register(new SideQuest("t_deliver", Type.DELIVER,
				InventoryManager.ItemType.DATA_CORE, 1,
				new Reward(0, 0, 30, 40)));

		// 1) Árvore de nós
		System.out.println("TESTE 1: árvore de nós");
		BranchingNpc npc = sampleNpc();
		check("nó inicial é o primeiro", "Bem-vindo, piloto!".equals(npc.getNode().text));
		check("nó inicial tem escolhas", npc.hasChoices());
		npc.selectChoice(0);
		check("escolha 0 ativa a missão t_kills", SideQuestManager.isActive("t_kills"));
		check("nó após escolha 0 é o segundo", npc.getNode().text.startsWith("Derrube 5"));
		check("nó 1 não é terminal", !npc.isTerminal());
		npc.selectChoice(0);
		check("escolha em nó terminal não crasha", true);
		check("nó 2 é terminal", npc.isTerminal());
		// Diagnóstico: imprime estado do nó para depurar a falha acima
		if (npc.getNode() != null) {
			System.out.println("  debug nó atual: " + npc.getNode().text.substring(0,
					Math.min(npc.getNode().text.length(), 40)));
			for (int i = 0; i < npc.getNode().choiceTexts.length; i++) {
				System.out.println("  debug escolha " + i + ": '" + npc.getNode().choiceTexts[i]
						+ "' target=" + npc.getNode().choiceTargets[i]);
			}
		}
		System.out.println("  debug isTerminal=" + npc.isTerminal() + " hasChoices="
				+ npc.hasChoices());

		// 2) DialogueManager detecta BranchingNpc
		System.out.println("TESTE 2: diálogo ramificado no DialogueManager");
		com.traduvertgames.main.InventoryManager.reset();
		SideQuestManager.reset();
		SideQuestManager.register(new SideQuest("t_kills2", Type.KILL_N, null, 3,
				new Reward(10, 0, 0, 25)));
		SideQuestManager.register(new SideQuest("t_collect", Type.COLLECT_N,
				InventoryManager.ItemType.ENERGY_CELL, 2,
				new Reward(0, 15, 40, 60)));
		SideQuestManager.register(new SideQuest("t_deliver", Type.DELIVER,
				InventoryManager.ItemType.DATA_CORE, 1,
				new Reward(0, 0, 30, 40)));
		BranchingNpc npc2 = sampleNpc();
		Field fEntities = Game.class.getDeclaredField("entities");
		fEntities.setAccessible(true);
		java.util.List<com.traduvertgames.entities.Entity> entities =
			(java.util.List<com.traduvertgames.entities.Entity>) fEntities.get(null);
		entities.add(npc2);
		npc2.setX(Game.player.getX() + 10);
		npc2.setY(Game.player.getY() + 10);
		DialogueManager.startNearestDialogue();
		check("diálogo abre com o BranchingNpc", DialogueManager.isActive());
		check("getBranchChoices retorna 3 escolhas",
				DialogueManager.getBranchChoices().length == 3);
		DialogueManager.selectBranchChoice(0);
		check("após escolha o texto muda",
				DialogueManager.getCurrentLine().startsWith("Derrube"));
		DialogueManager.advance();
		check("diálogo fecha no nó terminal", !DialogueManager.isActive());

		// 3) KILL_N
		System.out.println("TESTE 3: missão de kills");
		SideQuestManager.activate("t_kills2");
		com.traduvertgames.entities.Enemy dummy = new com.traduvertgames.entities.Enemy(
				200, 200, 16, 16, com.traduvertgames.entities.Entity.ENEMY_EN,
				com.traduvertgames.entities.Enemy.Variant.SCOUT);
		for (int i = 0; i < 3; i++) {
			SideQuestManager.onEnemyKilled(dummy);
			System.out.println("  debug kill " + (i + 1) + ": ativo="
					+ SideQuestManager.isActive("t_kills2") + " prog="
					+ SideQuestManager.getProgress("t_kills2"));
		}
		check("progresso 3/3 após 3 kills", SideQuestManager.getProgress("t_kills2") == 3);
		SideQuestManager.onEnemyKilled(dummy);
		SideQuestManager.onEnemyKilled(dummy);
		check("missão concluída no alvo", SideQuestManager.isCompleted("t_kills2"));

		// 4) COLLECT_N
		System.out.println("TESTE 4: missão de coleta");
		SideQuestManager.activate("t_collect");
		SideQuestManager.refreshCollectibles();
		check("progresso 0 sem itens", SideQuestManager.getProgress("t_collect") == 0);
		InventoryManager.addPickup(InventoryManager.ItemType.ENERGY_CELL);
		InventoryManager.addPickup(InventoryManager.ItemType.ENERGY_CELL);
		SideQuestManager.refreshCollectibles();
		check("coleta conclui a missão", SideQuestManager.isCompleted("t_collect"));

		// 5) DELIVER
		System.out.println("TESTE 5: missão de entrega");
		SideQuestManager.activate("t_deliver");
		check("entrega sem item falha", !SideQuestManager.deliver("t_deliver"));
		InventoryManager.addPickup(InventoryManager.ItemType.DATA_CORE);
		check("entrega com item completa", SideQuestManager.deliver("t_deliver"));
		check("item consumido do inventário",
				InventoryManager.count(InventoryManager.ItemType.DATA_CORE) == 0);

		// 6) Persistência
		System.out.println("TESTE 6: persistência de missões");
		SideQuestManager.activate("t_kills2");
		SideQuestManager.addProgress("t_kills2", 1);
		java.util.Map<String, Integer> snapshot = SideQuestManager.serialize();
		java.util.Map<String, Boolean> done = new java.util.HashMap<String, Boolean>();
		done.put("t_deliver", true);
		SideQuestManager.reset();
		SideQuestManager.register(new SideQuest("t_kills2", Type.KILL_N, null, 3,
				new Reward(10, 0, 0, 25)));
		SideQuestManager.deserialize(snapshot, done);
		check("progresso restaurado", SideQuestManager.getProgress("t_kills2") == 1);
		check("concluídas restauradas", SideQuestManager.isCompleted("t_deliver"));

		SideQuestManager.reset();
		if (fails == 0) {
			System.out.println("ALL PASSED");
			System.exit(0);
		} else {
			System.out.println("FAILURES: " + fails);
			System.exit(1);
		}
	}
}
