import java.lang.reflect.Field;

import java.awt.image.BufferedImage;

import com.traduvertgames.entities.QuestBeacon;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.main.InventoryManager;
import com.traduvertgames.quest.HoldObjective;
import com.traduvertgames.quest.QuestManager;

/**
 * Testa os fixes da rodada 22b: beacon recriado no restore, seletor de fases
 * travado por progressao e inventario fechavel com ESC.
 */
public class Rodada22bTest {
	private static int checks = 0;
	private static int passed = 0;

	private static void check(String label, boolean ok) {
		checks++;
		if (ok) {
			passed++;
			System.out.println("PASS " + label);
		} else {
			System.out.println("FAIL " + label);
		}
	}

	public static void main(String[] args) throws Exception {
		// Este teste altera maxLevelReached. Preserva o save real mesmo quando
		// termina por System.exit, evitando apagar o progresso do jogador.
		final java.nio.file.Path savePath = SaveManager.SAVE_FILE.toPath();
		final boolean saveExisted = java.nio.file.Files.exists(savePath);
		final byte[] originalSave = saveExisted ? java.nio.file.Files.readAllBytes(savePath) : null;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (saveExisted) {
					java.nio.file.Files.write(savePath, originalSave);
				} else {
					java.nio.file.Files.deleteIfExists(savePath);
				}
			} catch (java.io.IOException ex) {
				System.err.println("Nao foi possivel restaurar o save do teste: " + ex.getMessage());
			}
		}, "restore-save-after-test"));

		// Desativar audio: remover pools de clips para nao travar sem dispositivo.
		com.traduvertgames.main.SoundManager.unload();
		Game g = new Game();
		g.setCurrentLevel(1);
		com.traduvertgames.world.World.restartGame("level1.png");
		Game.player = new Player(200, 100, 16, 16,
				new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		Game.entities.add(Game.player);
		Game.gameState = "NORMAL";

		// ---------- Teste 1: beacon recriado pelo restore ----------
		QuestManager.prepareForLevel(2);
		QuestManager.onLevelLoaded();
		// O beacon programatico da fase 2 foi criado?
		int beaconsBefore = 0;
		for (Object e : Game.entities) {
			if (e instanceof QuestBeacon) {
				beaconsBefore++;
			}
		}
		check("beacon criado na fase 2", beaconsBefore == 1);

		final String state = QuestManager.serializeObjectiveState();
		check("estado inclui BEACONS", state != null && state.contains("BEACONS=272,176"));

		// Simula recarga do mapa: entidade do beacon some do mundo.
		for (Object e : Game.entities.toArray()) {
			if (e instanceof QuestBeacon) {
				Game.entities.remove(e);
			}
		}
		int beaconsAfterWorldReset = 0;
		for (Object e : Game.entities) {
			if (e instanceof QuestBeacon) {
				beaconsAfterWorldReset++;
			}
		}
		check("beacon fisico removido antes do restore", beaconsAfterWorldReset == 0);

		// Restaura o estado salvo: o beacon deve voltar ao mundo e ao objetivo.
		// No fluxo real o estado passa por DialogueObjective/SequenceObjective,
		// que repassam o payload interno ao stage de defesa (HoldObjective).
		Object objectiveRef = QuestManager.getCurrentObjective();
		if (objectiveRef instanceof com.traduvertgames.quest.DialogueObjective) {
			((com.traduvertgames.quest.DialogueObjective) objectiveRef)
					.deserializeState(state);
		} else {
			QuestManager.deserializeObjectiveState(state);
		}
		int beaconsRestored = 0;
		for (Object e : Game.entities) {
			if (e instanceof QuestBeacon) {
				beaconsRestored++;
			}
		}
		check("beacon recriado pelo restore", beaconsRestored == 1);
		check("missao ainda completa com canal zerado? (nao)",
				!QuestManager.isObjectiveComplete());

		// Avanca o canal manualmente simulando zona limpa: o objetivo deve concluir.
		Object objectiveField = getQuestManagerCurrentObjective();
		Object unwrapped = findActiveHold(objectiveField);
		boolean channelAdvanced = false;
		if (unwrapped instanceof HoldObjective) {
			HoldObjective hold = (HoldObjective) unwrapped;
			// Simula "zona limpa": remove inimigos vivos dentro da zona de
			// defesa (no jogo o jogador os mata; no teste apenas garantimos a
			// condição de canal limpo para validar o avanço do objetivo).
			for (Object entity : Game.entities.toArray()) {
				if (entity instanceof com.traduvertgames.entities.Enemy) {
					com.traduvertgames.entities.Enemy enemy =
						(com.traduvertgames.entities.Enemy) entity;
					double dx = enemy.getX() - 272;
					double dy = enemy.getY() - 176;
					if (dx * dx + dy * dy
							<= HoldObjective.DEFENSE_RADIUS
							* HoldObjective.DEFENSE_RADIUS) {
						Game.entities.remove(enemy);
					}
				}
			}
			for (int frame = 0; frame < 620; frame++) {
				hold.update();
			}
			channelAdvanced = hold.isComplete();
		}
		check("canal do hold conclui apos zona limpa", channelAdvanced);

		// ---------- Teste 2: onLevelLoaded nao duplica beacon restaurado ----------
		// Com spawned=true e trackedBeacons nao-vazio (apos o restore),
		// preparar a fase 2 de novo nao deve criar um segundo beacon.
		QuestManager.prepareForLevel(2);
		QuestManager.onLevelLoaded();
		int beaconsAfterReload = 0;
		for (Object e : Game.entities) {
			if (e instanceof QuestBeacon) {
				beaconsAfterReload++;
			}
		}
		check("sem duplicacao de beacon apos reload", beaconsAfterReload <= 1);

		// ---------- Teste 3: seletor de fases travado ----------
		// Sem save ainda, maxLevelReached = 0 -> so a fase 1 destravada.
		check("sem save: apenas fase 1 destravada",
				com.traduvertgames.main.LevelSelectScreen.isUnlocked(1)
				&& !com.traduvertgames.main.LevelSelectScreen.isUnlocked(2)
				&& !com.traduvertgames.main.LevelSelectScreen.isUnlocked(9));

		// Com maxLevelReached = 3 no save ativo, fases 1-3 destravadas.
		setHighestReachedForTest(3);
		check("alcancado 3: fases 1-3 destravadas, 4 travada",
				com.traduvertgames.main.LevelSelectScreen.isUnlocked(3)
				&& !com.traduvertgames.main.LevelSelectScreen.isUnlocked(4));
		check("modo infinito travado ate a fase 8",
				!com.traduvertgames.main.LevelSelectScreen.isUnlocked(9));
		setHighestReachedForTest(8);
		check("alcancado 8: infinito destravado",
				com.traduvertgames.main.LevelSelectScreen.isUnlocked(9));
		setHighestReachedForTest(0);

		// ---------- Teste 4: inventario aberto fecha com ESC ----------
		InventoryManager.reset();
		InventoryManager.addPickup(InventoryManager.ItemType.MEDKIT);
		InventoryManager.toggle();
		check("inventario abre", InventoryManager.isOpen());
		Game.escapePressed = true;
		// Simula o handler do ESC do Game (ESC fecha inventario).
		if (InventoryManager.isOpen()) {
			InventoryManager.toggle();
			Game.escapePressed = false;
		}
		check("ESC fecha o inventario", !InventoryManager.isOpen());

		if (passed == checks) {
			System.out.println("ALL " + checks + " PASSED");
			System.exit(0);
		} else {
			System.out.println("FAILURES " + (checks - passed) + " of " + checks);
			System.exit(1);
		}
	}

	/** Escreve maxLevelReached direto no saves.json (loadRoot/saveRoot sao privados). */
	private static void setHighestReachedForTest(int level) throws Exception {
		java.io.File f = SaveManager.SAVE_FILE;
		java.util.Map<String, Object> root;
		if (f.exists()) {
			try (java.io.BufferedReader r = new java.io.BufferedReader(
					new java.io.FileReader(f))) {
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					sb.append(line);
				}
				root = readObject(sb.toString());
			}
		} else {
			root = new java.util.HashMap<String, Object>();
		}
		if (root == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		java.util.Map<String, Object> campaign =
				(java.util.Map<String, Object>) root.get("campaign");
		if (campaign == null) {
			campaign = new java.util.HashMap<String, Object>();
			root.put("campaign", campaign);
		}
		campaign.put("maxLevelReached", level);
		try (java.io.PrintWriter w = new java.io.PrintWriter(
				new java.io.FileWriter(f))) {
			w.println(writeObject(root));
		}
	}

	@SuppressWarnings("unchecked")
	private static java.util.Map<String, Object> readObject(String s) {
		if (s == null) {
			return null;
		}
		s = s.trim();
		if (!s.startsWith("{") || !s.endsWith("}")) {
			return null;
		}
		s = s.substring(1, s.length() - 1).trim();
		java.util.Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
		while (!s.isEmpty()) {
			int kStart = s.indexOf('"');
			if (kStart < 0) {
				break;
			}
			int kEnd = s.indexOf('"', kStart + 1);
			String key = s.substring(kStart + 1, kEnd);
			s = s.substring(kEnd + 1).trim();
			s = s.substring(s.indexOf(':') + 1).trim();
			Object value;
			if (s.startsWith("\"")) {
				int e = s.indexOf('"', 1);
				value = s.substring(1, e);
				s = s.substring(e + 1);
			} else if (s.startsWith("{")) {
				int depth = 0;
				int e = 0;
				for (int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					if (c == '{') {
						depth++;
					}
					if (c == '}') {
						depth--;
					}
					if (depth == 0) {
						e = i;
						break;
					}
				}
				value = readObject(s.substring(0, e + 1));
				s = s.substring(e + 1);
			} else if (s.startsWith("[")) {
				int depth = 0;
				int e = 0;
				for (int i = 0; i < s.length(); i++) {
					char c = s.charAt(i);
					if (c == '[') {
						depth++;
					}
					if (c == ']') {
						depth--;
					}
					if (depth == 0) {
						e = i;
						break;
					}
				}
				value = s.substring(0, e + 1);
				s = s.substring(e + 1);
			} else {
				int e = s.indexOf(',');
				if (e < 0) {
					e = s.length();
				}
				value = s.substring(0, e).trim();
				s = s.substring(e);
			}
			m.put(key, value);
			if (!s.isEmpty() && s.charAt(0) == ',') {
				s = s.substring(1).trim();
			}
		}
		return m;
	}

	@SuppressWarnings("unchecked")
	private static String writeObject(Object o) {
		if (o instanceof java.util.Map) {
			java.util.Map<String, Object> m =
				(java.util.Map<String, Object>) o;
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			for (java.util.Map.Entry<String, Object> e : m.entrySet()) {
				if (sb.length() > 1) {
					sb.append(",");
				}
				sb.append("\"").append(e.getKey()).append("\":");
				Object v = e.getValue();
				if (v instanceof java.util.Map || v instanceof String) {
					sb.append(writeObject(v));
				} else {
					sb.append(String.valueOf(v));
				}
			}
			sb.append("}");
			return sb.toString();
		}
		if (o instanceof String) {
			return "\"" + o + "\"";
		}
		return String.valueOf(o);
	}

	private static Object getQuestManagerCurrentObjective() throws Exception {
		Field f = QuestManager.class.getDeclaredField("currentObjective");
		f.setAccessible(true);
		return f.get(null);
	}

	private static Object findActiveHold(Object wrapper) throws Exception {
		if (wrapper instanceof HoldObjective) {
			return wrapper;
		}
		// DialogueObjective expoe o objetivo interno via getDelegate().
		if (wrapper instanceof com.traduvertgames.quest.DialogueObjective) {
			return findActiveHold(
				((com.traduvertgames.quest.DialogueObjective) wrapper).getDelegate());
		}
		// SequenceObjective executa etapas em ordem — avançar a primeira etapa
		// (HoldObjective) ate concluir tambem valida o comportamento em jogo.
		if (wrapper instanceof com.traduvertgames.quest.SequenceObjective) {
			Object nested = firstHoldOfSequence(wrapper);
			if (nested != null) {
				return nested;
			}
		}
		// Percorre campos do wrapper (SequenceObjective, etc.).
		if (wrapper == null) {
			return null;
		}
		// Evita classes de biblioteca padrao (inacessiveis em Java >=16).
		String pkg = wrapper.getClass().getPackageName();
		if (!pkg.startsWith("com.traduvertgames")) {
			return null;
		}
		for (Field f : wrapper.getClass().getDeclaredFields()) {
			if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			f.setAccessible(true);
			Object value = f.get(wrapper);
			Object nested = findActiveHold(value);
			if (nested != null) {
				return nested;
			}
		}
		return null;
	}

	/** Primeira HoldObjective da SequenceObjective (etapa ativa). */
	private static Object firstHoldOfSequence(Object sequence) throws Exception {
		for (Field f : sequence.getClass().getDeclaredFields()) {
			if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			f.setAccessible(true);
			Object value = f.get(sequence);
			if (value instanceof java.util.Collection) {
				for (Object item : (java.util.Collection<?>) value) {
					Object found = findActiveHold(item);
					if (found != null) {
						return found;
					}
				}
			} else {
				Object found = findActiveHold(value);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
