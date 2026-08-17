import java.io.File;
import java.awt.image.BufferedImage;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.entities.Player;
import com.traduvertgames.world.World;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.entities.QuestBeacon;
import com.traduvertgames.entities.Enemy;

/**
 * Rodada 26 — missão do beacon da fase 2 nunca pode travar.
 *
 * Cenários validados:
 *  1. Carga normal da fase 2: o beacon programático é criado (mesmo com o
 *     tile designado sendo parede e as salas cheias de inimigos).
 *  2. Recarga após morte com estado "spawned=true, beacon ativado
 *     (BEACONS vazio)": o beacon é recriado e o canal continua do ponto
 *     salvo.
 *  3. Cenário extremo: tracked vazio com spawned=true (perda do registro,
 *     ex.: beacon entregue ao estágio errado da sequência) — o
 *     onLevelLoaded recupera o beacon na fase 2.
 */
public class Rodada26BeaconLockTest {
	private static int pass = 0;
	private static int fail = 0;

	private static void check(String name, boolean ok) {
		if (ok) {
			pass++;
		} else {
			fail++;
			System.out.println("FAIL: " + name);
		}
	}

	public static void main(String[] args) throws Exception {
		new File("saves.json").delete();
		new File("save.txt").delete();
		Game g = new Game();
		Game.SCALE = 4;
		Game.player = new Player(200, 200, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));

		// Cenário 1: carga normal da fase 2 (troca de fase 1 → 2 como o jogo).
		Game.advanceToNextLevel();
		check("nivel e 2 apos a troca de fase", QuestManager.getCurrentLevel() == 2);
		QuestBeacon beacon = findBeacon();
		check("beacon programatico criado na fase 2", beacon != null);
		// Antes da conversa com a Nia o card mostra "Fale com Engenheira Nia";
		// após a conversa o estágio de defesa revela o beacon.
		com.traduvertgames.dialogue.InteractiveNpc nia = findNia();
		if (nia != null) {
			QuestManager.notifyDialogueFinished(nia);
		}
		check("beacon rastreado pelo objetivo", QuestManager.isObjectiveComplete() == false
				&& (QuestManager.getObjectiveProgress().contains("beacon")
						|| QuestManager.getObjectiveProgress().startsWith("Permaneça")));
		if (beacon != null) {
			Game.player.setX(beacon.getX());
			Game.player.setY(beacon.getY());
		}

		// Cenário 2: canal parcial, morte + autosave + reload.
		int channelBefore = 0;
		if (findBeacon() != null) {
			for (int i = 0; i < 90; i++) {
				QuestManager.update();
			}
			channelBefore = channelOf();
		}
		check("canal avanca quando a zona esta limpa", channelBefore > 0);
		SaveManager.saveCurrentGame();
		SaveManager.loadSlot(SaveManager.activeSlot);
		check("nivel e 2 apos o reload", QuestManager.getCurrentLevel() == 2);
		QuestBeacon beaconAfter = findBeacon();
		check("beacon existe apos o reload", beaconAfter != null);
		check("canal preservado apos o reload", channelOf() == channelBefore);
		check("progresso nao travado apos o reload",
				QuestManager.getObjectiveProgress().startsWith("Permaneça")
						|| QuestManager.getObjectiveProgress().startsWith("Defenda")
						|| QuestManager.getObjectiveProgress().startsWith("Canal"));

		// Cenário 3: perda total do registro (tracked vazio, spawned=true) —
		// simula o beacon entregue ao estágio errado da sequência.
		SaveManager.saveCurrentGame();
		// Corromper o estado salvo: spawned=true mas sem beacons.
		corruptBeaconsInSave();
		SaveManager.loadSlot(SaveManager.activeSlot);
		check("nivel e 2 apos reload com estado corrompido",
				QuestManager.getCurrentLevel() == 2);
		QuestBeacon beaconRecovered = findBeacon();
		check("beacon recuperado apos estado corrompido", beaconRecovered != null);
		check("progresso recuperado (nao travado em Localize)",
				beaconRecovered != null && !QuestManager.getObjectiveProgress()
						.equals("Localize o beacon do setor"));

		System.out.println("Rodada26BeaconLockTest: " + pass + " PASS, " + fail + " FAIL");
		System.exit(fail == 0 ? 0 : 1);
	}

	private static com.traduvertgames.dialogue.InteractiveNpc findNia() {
		for (Object o : Game.entities) {
			if (o instanceof com.traduvertgames.dialogue.InteractiveNpc) {
				com.traduvertgames.dialogue.InteractiveNpc npc =
						(com.traduvertgames.dialogue.InteractiveNpc) o;
				if (npc.getName() != null && npc.getName().contains("Nia")) {
					return npc;
				}
			}
		}
		return null;
	}

	private static QuestBeacon findBeacon() {
		for (Object o : Game.entities) {
			if (o instanceof QuestBeacon) {
				return (QuestBeacon) o;
			}
		}
		return null;
	}

	private static int channelOf() {
		String state = QuestManager.serializeObjectiveState();
		int idx = state.indexOf("CHANNEL=");
		if (idx < 0) {
			return -1;
		}
		String rest = state.substring(idx + "CHANNEL=".length());
		int semi = rest.indexOf(';');
		String value = semi >= 0 ? rest.substring(0, semi) : rest;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			return -1;
		}
	}

	private static void corruptBeaconsInSave() {
		try {
			String text = new String(java.nio.file.Files.readAllBytes(SaveManager.SAVE_FILE.toPath()),
					java.nio.charset.StandardCharsets.UTF_8);
			text = text.replaceAll("BEACONS=128,32", "BEACONS=");
			text = text.replaceAll("BEACONS=\\|128,32", "BEACONS=");
			java.nio.file.Files.write(SaveManager.SAVE_FILE.toPath(),
					text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		} catch (Exception ex) {
			System.out.println("warn: nao foi possivel corromper o save: " + ex);
		}
	}
}
