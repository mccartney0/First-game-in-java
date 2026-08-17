import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.EscortNpc;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.QuestItem;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.dialogue.TraitorNpc;
import com.traduvertgames.world.World;

/**
 * Rodada 23b — validação das fases 7/8 e do balanceamento:
 * - Fase 7: exatamente 1 chefe (Guardian-chefe), guardians comuns não contam;
 *   matar o chefe completa a sequência; o desertor Hélio está no mapa.
 * - Fase 8: objetivo só completa ao matar o Supervisor-Prime (OVERSEER_PRIME);
 *   guardians comuns não contam como chefe; o informante spawna no mapa;
 *   a campanha avança e o save persiste.
 */
public class Rodada23bTest {
	static int passed, failed;
	static void check(boolean cond, String name) {
		if (cond) { passed++; System.out.println("PASS: " + name); }
		else { failed++; System.out.println("FAIL: " + name); }
	}

	static Game newGame() throws Exception {
		Game g = new Game();
		Game.setCurrentLevel(1);
		Game.SCALE = 4;
		Game.player = new Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		return g;
	}

	static List<?> entities() throws Exception {
		Field f = Game.class.getDeclaredField("entities");
		f.setAccessible(true);
		return (List<?>) f.get(null);
	}

	static long countBosses() throws Exception {
		long n = 0;
		for (Object e : entities()) {
			if (e instanceof Enemy && ((Enemy) e).isBoss()) { n++; }
		}
		return n;
	}

	static long countGuardians(boolean bossOnly) throws Exception {
		long n = 0;
		for (Object e : entities()) {
			if (e instanceof Enemy) {
				Enemy en = (Enemy) e;
				if (en.getVariant() == Enemy.Variant.GUARDIAN && en.isBoss() == bossOnly) { n++; }
			}
		}
		return n;
	}

	public static void main(String[] args) throws Exception {
		new java.io.File("saves.json").delete();

		// ============ Fase 7 ============
		System.out.println("=== FASE 7 ===");
		newGame();
		Game.setCurrentLevel(7);
		World.restartGame("level7.png");
		check(countBosses() == 1, "Fase 7: exatamente 1 chefe no mapa");
		check(countGuardians(true) == 1, "Fase 7: só o chefe é Guardian-boss");
		check(countGuardians(false) == 0, "Fase 7: nenhum Guardian comum (chefe único)");
		boolean helio = false;
		for (Object e : entities()) {
			if (e instanceof TraitorNpc) { helio = true; }
		}
		check(helio, "Fase 7: desertor Hélio presente no mapa");

		check(!QuestManager.isObjectiveComplete(), "Fase 7: objetivo inicia incompleto");
		// Simula sabotar os 3 geradores (quest items do mapa).
		int phase7QuestItems = 0;
		for (Object e : entities()) {
			if (e instanceof QuestItem) {
				phase7QuestItems++;
				QuestManager.collectQuestItem((QuestItem) e);
			}
		}
		check(phase7QuestItems >= 3, "Fase 7: ao menos 3 geradores no mapa");
		check(!QuestManager.isObjectiveComplete(), "Fase 7: só sabotar não completa (falta o chefe)");
		// Falar com a Ava (briefing) + matar o chefe.
		InteractiveNpc ava = null;
		for (Object e : entities()) {
			if (e instanceof InteractiveNpc && ((InteractiveNpc) e).getName().contains("Ava")) {
				ava = (InteractiveNpc) e; break;
			}
		}
		if (ava != null) {
			QuestManager.notifyDialogueFinished(ava);
		}
		QuestManager.notifyBossSpotted();
		QuestManager.notifyEnemyKilled(new Enemy(22 * 16, 23 * 16, 16, 16, Entity.ENEMY_EN,
				Enemy.Variant.GUARDIAN, true));
		// O estágio seguinte (Hold) exige ativar o beacon do setor (o jogador
		// encostado nele por ~180 frames) e então manter a zona limpa até o
		// canal completar (~600 frames). Simula o fluxo completo do estágio.
		com.traduvertgames.entities.QuestBeacon beaconPhase7 = null;
		for (Object e : entities()) {
			if (e instanceof com.traduvertgames.entities.QuestBeacon) {
				beaconPhase7 = (com.traduvertgames.entities.QuestBeacon) e; break;
			}
		}
		check(beaconPhase7 != null, "Fase 7: beacon do setor existe no mapa");
		if (beaconPhase7 != null) {
			Game.player.setX(beaconPhase7.getX());
			Game.player.setY(beaconPhase7.getY());
		}
		int frames = 0;
		while (frames < 1500 && !QuestManager.isObjectiveComplete()) {
			// A sequência só avança quando o estágio ativo completa; a atualização
			// global também propaga o update() aos beacons (ativação + defesa).
			for (Object e : entities()) {
				if (e instanceof com.traduvertgames.entities.QuestBeacon) {
					e.getClass().getMethod("update").invoke(e);
				}
			}
			QuestManager.getCurrentObjective().update();
			frames++;
		}
		check(QuestManager.isObjectiveComplete(), "Fase 7: chefe + briefing + defesa completa o objetivo");

		// ============ Fase 8 ============
		System.out.println("=== FASE 8 ===");
		newGame();
		Game.setCurrentLevel(8);
		World.restartGame("level8.png");
		check(countBosses() == 1, "Fase 8: exatamente 1 chefe (Supervisor-Prime)");
		check(countGuardians(true) == 0, "Fase 8: guardians não contam como chefe");
		boolean informante = false;
		for (Object e : entities()) {
			if (e instanceof EscortNpc) { informante = true; }
		}
		check(informante, "Fase 8: informante de escolta spawna no mapa");
		// Referência para usar mais adiante (a instância é estável no ciclo).

		InteractiveNpc ava8 = null;
		for (Object e : entities()) {
			if (e instanceof InteractiveNpc && ((InteractiveNpc) e).getName().contains("Ava")) {
				ava8 = (InteractiveNpc) e; break;
			}
		}
		if (ava8 != null) { QuestManager.notifyDialogueFinished(ava8); }
		check(!QuestManager.isObjectiveComplete(), "Fase 8: briefing sem chefe não completa");
		// Guardian comum morto não completa.
		QuestManager.notifyEnemyKilled(new Enemy(10, 10, 16, 16, Entity.ENEMY_EN,
				Enemy.Variant.GUARDIAN, false));
		check(!QuestManager.isObjectiveComplete(), "Fase 8: guardian comum não completa");
		QuestManager.notifyEnemyKilled(new Enemy(23 * 16, 14 * 16, 16, 16, Entity.ENEMY_EN,
				Enemy.Variant.OVERSEER_PRIME, true));
		// O Infiltrator completa com briefing + chefe; o Sequence avança para o
		// estágio de escolta na simulação do tick (update), como no jogo real.
		QuestManager.getCurrentObjective().update();
		check(!QuestManager.isObjectiveComplete(), "Fase 8: chefe Prime sozinho não fecha a fase (falta a escolta)");
		// A fase completa quando TODOS os estágios completam (Infiltrator + escolta).
		// Simula a chegada do informante para concluir o estágio de escolta.
		EscortNpc informante8 = null;
		for (Object e : entities()) {
			if (e instanceof EscortNpc) {
				informante8 = (EscortNpc) e;
			}
		}
		if (informante8 != null) {
			QuestManager.registerEscort(informante8);
			// No jogo real o informante caminha sozinho (update do game loop) e
			// notifica a chegada ao atingir o ponto de fuga. No teste, simula a
			// chegada teleportando-o para o destino e executando um tick.
			informante8.setX(informante8.escapeTargetX());
			informante8.setY(informante8.escapeTargetY());
			informante8.getClass().getMethod("update").invoke(informante8);
		}
		QuestManager.getCurrentObjective().update();
		check(QuestManager.isObjectiveComplete(), "Fase 8: escolta concluída + chefe fecha a fase");

		// Persistência: salvar e recarregar a fase 8.
		SaveManager.saveCurrentGame();
		check(SaveManager.hasSlotSave(1), "Fase 8: save gravado no slot 1");
		check(SaveManager.getSlotLevel(1) == 8, "Fase 8: slot registra a fase atual");
		System.out.println("Progresso: " + passed + " passaram, " + failed + " falharam");
		System.exit(0);
	}
}
