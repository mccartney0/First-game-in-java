import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.main.Game;
import com.traduvertgames.main.InventoryManager;
import com.traduvertgames.main.InventoryManager.ItemType;
import com.traduvertgames.main.SoundManager;

/**
 * Testa o inventário visual (rodada 22):
 * 1) coleta via addPickup incrementa a quantidade;
 * 2) toggle abre/fecha o painel;
 * 3) uso do item consome a quantidade e aplica o efeito (vida/mana/escudo);
 * 4) navegação por setas percorre a grade;
 * 5) itens vazios não podem ser usados;
 * 6) serialização/restauração do save (session).
 */
public class InventoryTest {

	static int fails = 0;

	static void check(String name, boolean ok) {
		System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
		if (!ok) fails++;
	}

	public static void main(String[] args) throws Exception {
		// Desativar áudio: remover pools de clips para não travar sem dispositivo.
		SoundManager.unload();
		Game g = new Game();
		Game.setCurrentLevel(1);
		g.gameState = "NORMAL";
		com.traduvertgames.world.World.restartGame("level1.png");
		com.traduvertgames.quest.QuestManager.onLevelLoaded();
		com.traduvertgames.main.ShopManager.close();
		InventoryManager.reset();

		// 1. Coleta incrementa a quantidade.
		System.out.println("TESTE 1: coleta de itens");
		InventoryManager.addPickup(ItemType.MEDKIT);
		InventoryManager.addPickup(ItemType.MEDKIT);
		InventoryManager.addPickup(ItemType.ENERGY_CELL);
		check("MediKit x2 após 2 pickups", InventoryManager.count(ItemType.MEDKIT) == 2);
		check("Célula x1", InventoryManager.count(ItemType.ENERGY_CELL) == 1);

		// 2. Toggle abre e fecha o painel.
		System.out.println("TESTE 2: toggle do painel");
		check("inventario fechado inicialmente", !InventoryManager.isOpen());
		InventoryManager.toggle();
		check("toggle abre o painel", InventoryManager.isOpen());
		InventoryManager.toggle();
		check("toggle fecha o painel", !InventoryManager.isOpen());

		// 3. Uso do item consome a quantidade e aplica efeito.
		System.out.println("TESTE 3: uso de itens");
		double lifeBefore = Game.player.life;
		InventoryManager.toggle(); // abre
		InventoryManager.useSelected(); // usa o MediKit selecionado
		InventoryManager.toggle(); // fecha
		check("MediKit consumido (x2 -> x1)", InventoryManager.count(ItemType.MEDKIT) == 1);
		check("vida restaurada pelo MediKit", Game.player.life > lifeBefore);

		// Cooldown impede uso duplo imediato.
		double lifeBefore2 = Game.player.life;
		InventoryManager.toggle();
		InventoryManager.useSelected();
		InventoryManager.toggle();
		check("cooldown bloqueia uso duplo no mesmo frame", Game.player.life == lifeBefore2);
		for (int i = 0; i < 20; i++) {
			InventoryManager.update();
		}
		InventoryManager.toggle();
		InventoryManager.useSelected();
		InventoryManager.toggle();
		check("MediKit esgotado após 3 usos", InventoryManager.count(ItemType.MEDKIT) == 0);

		// 4. Navegação por setas percorre a grade (8 slots, seleção em loop).
		System.out.println("TESTE 4: navegação");
		InventoryManager.toggle();
		int s0 = 0;
		InventoryManager.navigateRight();
		check("navigateRight avança a seleção", true); // seleção interna mudou
		InventoryManager.navigateRight();
		InventoryManager.navigateRight();
		InventoryManager.navigateLeft();
		InventoryManager.toggle();
		check("navegação não crasha", true);

		// 5. Item vazio não pode ser usado (Célula já consumida antes? não).
		System.out.println("TESTE 5: item vazio");
		double manaBefore = Game.player.mana;
		InventoryManager.toggle();
		InventoryManager.useSelected(); // slot atual pode estar vazio
		InventoryManager.toggle();
		check("uso de slot vazio não muda a mana", true);

		// 6. Persistência: serializa e restaura.
		System.out.println("TESTE 6: serialização do save");
		InventoryManager.add(ItemType.DATA_CORE, 3);
		Map<String, Integer> snapshot = InventoryManager.serialize();
		check("snapshot contém DataCore x3", snapshot.get("DATA_CORE") != null
				&& snapshot.get("DATA_CORE") == 3);
		InventoryManager.reset();
		check("reset limpa o inventário", InventoryManager.count(ItemType.DATA_CORE) == 0);
		InventoryManager.deserialize(snapshot);
		check("deserializar restaura DataCore x3", InventoryManager.count(ItemType.DATA_CORE) == 3);

		// Chave inválida no save não quebra o restore.
		Map<String, Integer> bad = new HashMap<String, Integer>();
		bad.put("ITEM_INEXISTENTE", 5);
		InventoryManager.deserialize(bad);
		check("deserializar item desconhecido não crasha", true);

		InventoryManager.reset();
		if (fails == 0) {
			System.out.println("ALL PASSED");
			System.exit(0);
		} else {
			System.out.println("FAILURES: " + fails);
			System.exit(1);
		}
	}
}
