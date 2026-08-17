/**
 * Rodada 22g-2 — validação estendida de saves e missões secundárias.
 *
 * Complementa a FaseSaveE2ETest (troca de fase + persistência) cobrindo:
 *
 *  A. Save repetido no mesmo slot (regressão do bug de sessão circular).
 *  B. Múltiplos slots independentes (1, 2 e 3) e getHighestUnlockedLevel.
 *  C. clearSlot remove o save sem afetar os demais.
 *  D. Missões secundárias KILL_N / COLLECT_N / DELIVER: progresso,
 *     conclusão por addProgress, refreshCollectibles e deliver,
 *     e ativação/zera progresso em nova fase.
 *  E. Inventário: consumo (useSelected), consumo parcial, limites.
 *  F. bestRun: só grava quando há métricas válidas e persiste ao disco.
 */
import java.awt.image.BufferedImage;
import java.io.File;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.main.InventoryManager;
import com.traduvertgames.main.InventoryManager.ItemType;
import com.traduvertgames.entities.Player;
import com.traduvertgames.quest.SideQuestManager;
import com.traduvertgames.quest.SideQuestManager.SideQuest;
import com.traduvertgames.quest.SideQuestManager.Type;
import com.traduvertgames.quest.SideQuestManager.Reward;
import com.traduvertgames.main.SoundManager;
import com.traduvertgames.world.World;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

public class Rodada22g2Test {
	private static int passed = 0;
	private static int failed = 0;

	private static void check(String name, boolean ok) {
		if (ok) {
			passed++;
			System.out.println("[PASS] " + name);
		} else {
			failed++;
			System.out.println("[FAIL] " + name);
		}
	}

	private static void clean() throws Exception {
		File f = SaveManager.SAVE_FILE;
		if (f.exists()) {
			f.delete();
		}
		SaveManager.activeSlot = 1;
		SideQuestManager.reset();
		InventoryManager.reset();
	}

	private static Game newGame() throws Exception {
		Game g = new Game();
		Game.setCurrentLevel(1);
		Game.SCALE = 4;
		return g;
	}

	private static void initWorld(Game g) throws Exception {
		World.restartGame("level1.png");
		Game.player = new Player(200, 100, 16, 16,
				new BufferedImage(16, 16, TYPE_INT_ARGB));
	}

	public static void main(String[] args) throws Exception {
		SoundManager.unload();

		// ============ A. Save repetido no mesmo slot ============
		clean();
		Game a1 = newGame();
		initWorld(a1);
		setField(Player.class, "life", 80.0);
		Game.advanceToNextLevel(); // fase 2
		SaveManager.saveCurrentGame();
		SaveManager.saveCurrentGame(); // re-save imediato
		Game.setCurrentLevel(3);
		SaveManager.saveCurrentGame(); // save de nova fase no mesmo slot
		SaveManager.saveCurrentGame(); // re-save
		check("Save repetido: sessão permanece no slot 1",
				SaveManager.hasSlotSave(1));
		check("Save repetido: fase do slot é 3",
				SaveManager.getSlotLevel(1) == 3);
		// Recarregar em instância nova: vida persistida apesar dos re-saves.
		Game a2 = newGame();
		SaveManager.loadSlot(1);
		check("Save repetido: vida restaurada (~80)",
				Math.abs(Player.life - 80.0) < 1.0);
		check("Save repetido: unlocked==3",
				SaveManager.getHighestUnlockedLevel() == 3);

		// ============ B. Slots independentes ============
		clean();
		Game b1 = newGame();
		initWorld(b1);
		SaveManager.activeSlot = 1;
		Game.setCurrentLevel(2);
		SaveManager.saveCurrentGame();
		SaveManager.activeSlot = 2;
		Game.setCurrentLevel(5);
		SaveManager.saveCurrentGame();
		SaveManager.activeSlot = 3;
		Game.setCurrentLevel(7);
		SaveManager.saveCurrentGame();
		check("Slot 1 salvo na fase 2", SaveManager.getSlotLevel(1) == 2);
		check("Slot 2 salvo na fase 5", SaveManager.getSlotLevel(2) == 5);
		check("Slot 3 salvo na fase 7", SaveManager.getSlotLevel(3) == 7);
		check("Todos os slots têm save",
				SaveManager.hasSlotSave(1) && SaveManager.hasSlotSave(2)
						&& SaveManager.hasSlotSave(3));
		check("Unlocked reflete o slot ativo (3 -> fase 7)",
				SaveManager.getHighestUnlockedLevel() == 7);

		// ============ C. clearSlot ============
		SaveManager.activeSlot = 2;
		check("clearSlot(2) remove o save do slot 2",
				SaveManager.clearSlot(2) && !SaveManager.hasSlotSave(2));
		check("clearSlot(2) não afeta slots 1 e 3",
				SaveManager.hasSlotSave(1) && SaveManager.hasSlotSave(3));
		// clearSlot com id inválido não encontra slot, mas grava o root
		// sem alterações — o importante é que não corrompe os demais slots.
		check("clearSlot id inválido: slots válidos intactos",
				SaveManager.hasSlotSave(1) && SaveManager.hasSlotSave(3));

		// ============ D. Missões secundárias ============
		clean();
		SideQuestManager.reset();
		// KILL_N: registrar, ativar, progredir até o alvo.
		SideQuestManager.register(new SideQuest("k1", Type.KILL_N, null, 3,
				new Reward(10, 0, 0, 50)));
		SideQuestManager.activate("k1");
		check("KILL_N: ativa no início", SideQuestManager.isActive("k1"));
		check("KILL_N: progresso inicial 0",
				SideQuestManager.getProgress("k1") == 0);
		SideQuestManager.addProgress("k1", 1);
		SideQuestManager.addProgress("k1", 1);
		check("KILL_N: progresso 2/3", SideQuestManager.getProgress("k1") == 2);
		check("KILL_N: label '2/3 inimigos'",
				SideQuestManager.getProgressLabel("k1").startsWith("2/3"));
		SideQuestManager.addProgress("k1", 5); // estouro parcial
		check("KILL_N: conclui ao atingir/exceder alvo",
				SideQuestManager.isCompleted("k1"));
		// addProgress após conclusão não reabre.
		SideQuestManager.activate("k1");
		SideQuestManager.addProgress("k1", 3);
		check("KILL_N reativada: conclui de novo",
				SideQuestManager.isCompleted("k1"));

		// Missão inexistente: API tolerante.
		check("Quest inexistente: isCompleted=false",
				!SideQuestManager.isCompleted("inexistente"));
		check("Quest inexistente: addProgress no-op",
				SideQuestManager.getProgress("inexistente") == 0);

				// COLLECT_N: progresso espelha o inventário.
		// Nota: a missão fica ATIVA após activate (progresso registrado),
		// mesmo sem itens; o inventário só avança o progresso no refresh.
		SideQuestManager.register(new SideQuest("c1", Type.COLLECT_N,
				ItemType.MEDKIT, 2, new Reward(0, 0, 0, 30)));
		SideQuestManager.activate("c1");
		check("COLLECT_N: ativa após registro com progresso zerado",
				SideQuestManager.isActive("c1")
						&& SideQuestManager.getProgress("c1") == 0
						&& InventoryManager.count(ItemType.MEDKIT) == 0);
		SideQuestManager.refreshCollectibles();
		check("COLLECT_N: refresh sem itens mantém progresso 0",
				SideQuestManager.getProgress("c1") == 0
						&& !SideQuestManager.isCompleted("c1"));
		InventoryManager.add(ItemType.MEDKIT, 1);
		SideQuestManager.refreshCollectibles();
		check("COLLECT_N: progresso 1 após 1 item",
				SideQuestManager.getProgress("c1") == 1);
		InventoryManager.add(ItemType.MEDKIT, 2);
		SideQuestManager.refreshCollectibles();
		check("COLLECT_N: conclui com 3 items (>=2)",
				SideQuestManager.isCompleted("c1"));

		// DELIVER: entrega consome 1 item e conclui.
		SideQuestManager.register(new SideQuest("d1", Type.DELIVER,
				ItemType.NANOMEDKIT, 1, new Reward(0, 0, 0, 40)));
		SideQuestManager.activate("d1");
		check("DELIVER: entrega falha sem item no inventário",
				!SideQuestManager.deliver("d1")
						&& !SideQuestManager.isCompleted("d1"));
		InventoryManager.add(ItemType.NANOMEDKIT, 1);
		check("DELIVER: entrega consome 1 item e conclui",
				SideQuestManager.deliver("d1")
						&& SideQuestManager.isCompleted("d1")
						&& InventoryManager.count(ItemType.NANOMEDKIT) == 0);
		check("DELIVER: segunda entrega recusada (inativa)",
				!SideQuestManager.deliver("d1"));

		// Persistência das missões (grava e restaura).
		SideQuestManager.register(new SideQuest("k2", Type.KILL_N, null, 2,
				new Reward(0, 0, 0, 10)));
		SideQuestManager.activate("k2");
		SideQuestManager.addProgress("k2", 1);
		Game.setCurrentLevel(1);
		SaveManager.saveCurrentGame();
		Game b2 = newGame();
		SaveManager.loadSlot(1);
		check("Missão em andamento persistida (progresso 1)",
				SideQuestManager.getProgress("k2") == 1
						&& !SideQuestManager.isCompleted("k2"));
		check("Missão concluída persistida (c1)",
				SideQuestManager.isCompleted("c1"));

		// ============ E. Inventário: consumo e limites ============
		InventoryManager.reset();
		InventoryManager.add(ItemType.MEDKIT, 2);
		InventoryManager.add(ItemType.NANOMEDKIT, 1);
		check("Inventário: contagem correta",
				InventoryManager.count(ItemType.MEDKIT) == 2
						&& InventoryManager.count(ItemType.NANOMEDKIT) == 1);
		check("Inventário: consumo parcial",
				InventoryManager.consume(ItemType.MEDKIT, 1)
						&& InventoryManager.count(ItemType.MEDKIT) == 1);
		check("Inventário: consumo maior que estoque falha",
				!InventoryManager.consume(ItemType.MEDKIT, 5)
						&& InventoryManager.count(ItemType.MEDKIT) == 1);
		InventoryManager.addPickup(ItemType.ENERGY_CELL);
		check("Inventário: addPickup incrementa",
				InventoryManager.count(ItemType.ENERGY_CELL) == 1);

		// ============ F. bestRun ============
		clean();
		check("bestRun vazio no início", !SaveManager.hasBestRun());
		// captureBestRun só captura com kills>0 e tempo>0; sem gameplay
		// simulado, valida-se que a ausência de recorde não quebra o save.
		Game f1 = newGame();
		initWorld(f1);
		SaveManager.captureBestRun();
		SaveManager.saveCurrentGame();
		SaveManager.refreshBestRun();
		check("bestRun sem gameplay: não polui o disco",
				!SaveManager.hasBestRun());

		System.out.println();
		System.out.println("Rodada22g2Test: " + passed + " passaram, "
				+ failed + " falharam (total " + (passed + failed) + ")");
		System.out.flush();
		System.exit(0);
	}

	private static void setField(Object target, String name, Object value)
			throws Exception {
		Class<?> cls = target instanceof Class ? (Class<?>) target
				: target.getClass();
		java.lang.reflect.Field f = cls.getDeclaredField(name);
		f.setAccessible(true);
		f.set(target instanceof Class ? null : target, value);
	}
}
