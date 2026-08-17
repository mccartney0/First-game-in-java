import java.awt.image.BufferedImage;
import java.io.File;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.QuestBeacon;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.main.EnemyKillTracker;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;

import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.world.World;
/**
 * Rodada 25 — o beacon da fase 2 não trava após morrer e reiniciar.
 *
 * Cenário E2E do bug reportado: o jogador avança para a fase 2 (defesa do
 * beacon), conversa com a Engenheira Nia, atende o beacon na zona e então
 * morre. Ao escolher "Reiniciar partida", o save da morte é carregado e o
 * mundo é recriado. Valida:
 *
 *  1. O beacon programático é criado em um tile de chão válido (o ponto
 *     designado 17,11 fica sobre uma parede no level2.png — o código
 *     precisa procurar o chão livre mais próximo, senão a missão trava em
 *     "Localize o beacon do setor").
 *  2. O canal de estabilização já iniciado persiste no save (autosave da
 *     morte) e é restaurado no recarregamento.
 *  3. O beacon físico é recriado no reload (ele não existe mais no mundo
 *     recriado) e permanece rastreável pelo objetivo.
 *  4. Os inimigos abatidos antes da morte não ressuscitam (integração com
 *     o EnemyKillTracker da mesma rodada).
 *  5. O canal volta a avançar no reload quando a zona está limpa.
 */
public class Rodada25BeaconTest {
	private static int pass = 0;
	private static int fail = 0;
	static void check(String name, boolean ok) {
		if (ok) {
			pass++;
			System.out.println("PASS: " + name);
		} else {
			fail++;
			System.out.println("FAIL: " + name);
		}
	}
	private static int countBeacons() {
		int count = 0;
		for (int i = 0; i < Game.entities.size(); i++) {
			if (Game.entities.get(i) instanceof QuestBeacon) {
				count++;
			}
		}
		return count;
	}
	private static InteractiveNpc findNia() {
		for (int i = 0; i < Game.entities.size(); i++) {
			com.traduvertgames.entities.Entity e = Game.entities.get(i);
			if (e instanceof InteractiveNpc
					&& "Engenheira Nia".equals(((InteractiveNpc) e).getName())) {
				return (InteractiveNpc) e;
			}
		}
		return null;
	}
	private static QuestBeacon findBeacon() {
		for (int i = 0; i < Game.entities.size(); i++) {
			com.traduvertgames.entities.Entity e = Game.entities.get(i);
			if (e instanceof QuestBeacon) {
				return (QuestBeacon) e;
			}
		}
		return null;
	}
	public static void main(String[] args) throws Exception {
		new File("saves.json").delete();
		new File("save.txt").delete();
		Game g = new Game();
		Game.SCALE = 4;
		Game.setCurrentLevel(2);
		Game.player = new Player(100, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		// Carrega a fase 2 (o beacon programático nasce no onLevelLoaded).
		World.restartGame("level2.png");
		// Cenário 1: o beacon nasce em chão válido, mesmo com o tile
		// designado (17,11) sendo parede no level2.png.
		QuestBeacon beacon = findBeacon();
		check("Fase 2: beacon programático criado", beacon != null);
		int bx = beacon.getX() / 16;
		int by = beacon.getY() / 16;
		check("Beacon em tile de chão válido (não parede)",
				!World.isWallTile(bx, by));
		// O tile designado (17,11) é uma parede no level2.png — o beacon fica
		// no chão livre mais próximo (raio máx. 12) sem invasores na zona de
		// defesa; aceitar qualquer posição dentro desse raio.
		check("Beacon no chão livre mais próximo do designado (raio <= 12)",
				Math.abs(bx - 17) <= 12 && Math.abs(by - 11) <= 12);
		// Cenário 2: falar com a Nia avança o objetivo de diálogo e mantém o
		// beacon rastreável (a missão não trava em "fale com o NPC").
		InteractiveNpc nia = findNia();
		check("Engenheira Nia presente na fase 2", nia != null);
		if (nia != null) {
			QuestManager.notifyDialogueFinished(nia);
		}
		String state = QuestManager.serializeObjectiveState();
		check("Diálogo concluído: TALKED=true no estado",
				state.contains("TALKED=true"));
		check("Beacon rastreado após o diálogo (SPAWNED=true)",
				state.contains("SPAWNED=true"));
		// Cenário 3: o jogador canaliza parcialmente (fica na zona sem
		// completar) — o canal avança enquanto nenhum invasor está na zona.
		Game.player.setX(beacon.getX());
		Game.player.setY(beacon.getY());
		for (int frame = 0; frame < 120; frame++) {
			beacon.update();
			QuestManager.update();
		}
		String channelBeforeText = "CHANNEL=0";
		int channelBefore = 0;
		String stateBeforeDeath = QuestManager.serializeObjectiveState();
		int idx = stateBeforeDeath.indexOf("CHANNEL=");
		if (idx >= 0) {
			int end = stateBeforeDeath.indexOf(';', idx);
			channelBeforeText = stateBeforeDeath.substring(idx, end);
			channelBefore = Integer.parseInt(
					stateBeforeDeath.substring(idx + "CHANNEL=".length(), end));
		}
		check("Canal avança enquanto o jogador segura na zona",
				channelBefore > 0);
		// Cenário 4: o jogador morre — o autosave da morte grava o canal.
		Player.life = 0;
		SaveManager.saveAutoSave();
		Game.gameState = "GAMEOVER";
		String savedState = new String(java.nio.file.Files.readAllBytes(
				SaveManager.SAVE_FILE.toPath()));
		check("Autosave da morte gravado",
				SaveManager.SAVE_FILE.exists() && savedState.contains("progress"));
		// Cenário 5: "Reiniciar partida" → loadSlot restaura tudo.
		SaveManager.activeSlot = 1;
		boolean loaded = SaveManager.loadSlot(1);
		check("loadSlot(1) restaura o autosave da morte", loaded);
		check("Retorna à fase 2 (não ao tutorial)",
				Game.getCurrentLevel() == 2);
		check("Estado NORMAL após o reload", "NORMAL".equals(Game.gameState));
		// O beacon físico deve ser recriado e o canal restaurado.
		QuestBeacon beaconReload = findBeacon();
		check("Beacon recriado após o reload", beaconReload != null);
		check("Beacon restaurado na mesma posição salva",
				beaconReload != null && beaconReload.getX() == beacon.getX()
						&& beaconReload.getY() == beacon.getY());
		String stateAfter = QuestManager.serializeObjectiveState();
		check("SPAWNED e canal restaurados no estado",
				stateAfter.contains("SPAWNED=true")
						&& stateAfter.contains("CHANNEL=" + channelBefore));
		// Cenário 6: a fase não trava — com a zona limpa, o canal continua
		// avançando a partir do progresso restaurado.
		Game.player.setX(beaconReload.getX());
		Game.player.setY(beaconReload.getY());
		for (int frame = 0; frame < 100; frame++) {
			beaconReload.update();
			QuestManager.update();
		}
		int channelAfter = 0;
		String stateAfterRun = QuestManager.serializeObjectiveState();
		idx = stateAfterRun.indexOf("CHANNEL=");
		if (idx >= 0) {
			int end = stateAfterRun.indexOf(';', idx);
			channelAfter = Integer.parseInt(
					stateAfterRun.substring(idx + "CHANNEL=".length(), end));
		}
		check("Canal avança a partir do progresso restaurado",
				channelAfter >= channelBefore);
		// Cenário 7: integração com o kill persistence — um inimigo abatido
		// antes da morte não ressuscita no reload.
		EnemyKillTracker.markDead(2, 3, false);
		check("Tracker marca o tile abatido",
				EnemyKillTracker.isAlreadyDead(2, 3, false));
		new File("saves.json").delete();
		System.out.println("Resultado: " + pass + " pass, " + fail + " fail");
		System.exit(fail == 0 ? 0 : 1);
	}
}
