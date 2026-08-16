import com.traduvertgames.main.*;
import com.traduvertgames.quest.*;
import com.traduvertgames.dialogue.*;
import com.traduvertgames.entities.*;

/** Verifica a lógica narrativa das fases 7-8 de forma unitária. */
public class NarrativeLogicTest {
	private static int fails = 0;
	private static void check(String name, boolean ok) {
		if (!ok) { fails++; System.out.println("FAIL: " + name); }
		else { System.out.println("PASS: " + name); }
	}
	public static void main(String[] a) throws Exception {
		// Isola o teste: injeta Spritesheet dummy (ambiente sem recursos).
		try {
			Game.spritesheet = new com.traduvertgames.graficos.Spritesheet("/spritesheet.png");
		} catch (Exception ex) {
			try {
				com.traduvertgames.graficos.Spritesheet dummy = new com.traduvertgames.graficos.Spritesheet("/spritesheet.png");
				java.lang.reflect.Field f = com.traduvertgames.graficos.Spritesheet.class.getDeclaredField("spritesheet");
				f.setAccessible(true);
				f.set(dummy, new java.awt.image.BufferedImage(256, 256, java.awt.image.BufferedImage.TYPE_INT_ARGB));
				Game.spritesheet = dummy;
			} catch (Exception ignored) {
				System.err.println("Spritesheet dummy indisponível");
			}
		}
		java.lang.reflect.Field fr = Game.class.getDeclaredField("rand");
		fr.setAccessible(true);
		fr.set(null, new java.util.Random(42));
		java.lang.reflect.Field fe = Game.class.getDeclaredField("entities");
		fe.setAccessible(true);
		fe.set(null, new java.util.ArrayList<>());
		java.lang.reflect.Field fen = Game.class.getDeclaredField("enemies");
		fen.setAccessible(true);
		fen.set(null, new java.util.ArrayList<>());
		java.lang.reflect.Field fp = Game.class.getDeclaredField("player");
		fp.setAccessible(true);
		fp.set(null, new Player(100, 100, 16, 16,
			new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)));
		com.traduvertgames.entities.Player.resetPersistentArsenal();
		// --- Fase 7: SabotageObjective ---
		SabotageObjective so = new SabotageObjective();
		check("f7 incompleto no inicio", !so.isComplete());
		QuestItem q1 = new QuestItem(0, 0, new java.awt.Color(0xFFFFC107));
		QuestItem q2 = new QuestItem(0, 0, new java.awt.Color(0xFFFFC107));
		QuestItem q3 = new QuestItem(0, 0, new java.awt.Color(0xFFFFC107));
		so.onQuestItemSpawned(q1);
		so.onQuestItemSpawned(q2);
		so.onQuestItemSpawned(q3);
		check("f7 total 3", so.getTotalCount() == 3);
		so.onQuestItemCollected(q1);
		so.onQuestItemCollected(q2);
		so.onQuestItemCollected(q3);
		check("f7 3/3 sabotados", so.getSabotagedCount() == 3);
		check("f7 incompleto sem chefe", !so.isComplete());
		Enemy guardian = new Enemy(0, 0, 16, 16, Entity.ENEMY_EN, Enemy.Variant.GUARDIAN, true);
		so.onEnemyKilled(guardian);
		check("f7 completo apos chefe", so.isComplete());
		check("f7 serialize contem SABOTAGED", so.serializeState().contains("SABOTAGED=3"));
		SabotageObjective so2 = new SabotageObjective();
		so2.deserializeState("SABOTAGED=2;BOSS=true");
		check("f7 desserializa sabotaged=2 boss", so2.getSabotagedCount() == 2 && so2.isComplete());
		check("f7 hint dinamico", so2.getTargetHint() != null && so2.getTargetHint().length() > 3);

		// --- Fase 8: InfiltratorObjective ---
		InfiltratorObjective io = new InfiltratorObjective();
		check("f8 incompleto no inicio", !io.isComplete());
		CommanderNpc ava = new CommanderNpc(0, 0);
		io.onDialogueFinished(ava);
		check("f8 briefing feito", "Destrua o Supervisor-Prime".equals(io.getProgressText()));
		Enemy prime = new Enemy(0, 0, 16, 16, Entity.ENEMY_EN, Enemy.Variant.OVERSEER, true);
		io.onEnemyKilled(prime);
		check("f8 completo apos fala + chefe", io.isComplete());
		InfiltratorObjective io2 = new InfiltratorObjective();
		io2.deserializeState("BRIEFING=true;BOSS=false");
		check("f8 desserializa briefing", !io2.isComplete());
		check("f8 hint dinamico", io2.getTargetHint() != null && io2.getTargetHint().length() > 3);

		// --- QuestManager fases 7-8 ---
		// Recompensa da fase 7 é concedida com CUR_LEVEL=7 (na conclusão da fase).
		QuestManager.prepareForLevel(7);
		check("f7 titulo", "Subsolo da Colônia".equals(QuestManager.getPhaseTitle(7)));
		String t7 = QuestManager.getObjectiveTitle();
		check("f7 objetivo sabotagem", t7.toLowerCase().contains("gerador") || t7.toLowerCase().contains("sabot"));
		check("f7 level 7", QuestManager.getCurrentLevel() == 7);
		QuestManager.prepareForLevel(8);
		Game.setCurrentLevel(8);
		check("f8 titulo", "Núcleo Central".equals(QuestManager.getPhaseTitle(8)));
		String t8 = QuestManager.getObjectiveTitle();
		check("f8 objetivo nucleo", t8.toLowerCase().contains("núcleo") || t8.toLowerCase().contains("ia") || t8.toLowerCase().contains("supervisor"));
		check("f8 level 8", QuestManager.getCurrentLevel() == 8);
		QuestManager.prepareForLevel(9);
		check("f9+ survival", QuestManager.isSurvivalMode());

		// --- TraitorNpc ---
		TraitorNpc helio = new TraitorNpc(100, 100);
		check("helio e Entity", helio instanceof Entity);
		check("helio interativo", helio instanceof InteractiveNpc);

		// --- Game traitorTalked + recompensas ---
		Game.setTraitorTalked(true);
		check("traitorTalked setado", Game.isTraitorTalked());
		Game.setCurrentLevel(7);
		{ java.lang.reflect.Method m = Game.class.getDeclaredMethod("grantCampaignReward"); m.setAccessible(true); m.invoke(null); }
		check("recompensa f7: VOID_MORTAR", Player.isWeaponUnlocked(WeaponType.VOID_MORTAR));
		QuestManager.prepareForLevel(8);
		Game.setCurrentLevel(8);
		{ java.lang.reflect.Method m = Game.class.getDeclaredMethod("grantCampaignReward"); m.setAccessible(true); m.invoke(null); }
		check("recompensa f8: DRONE_SENTINEL", Player.isWeaponUnlocked(WeaponType.DRONE_SENTINEL));
		Game.resetTraitorTalked();
		check("traitorTalked resetado", !Game.isTraitorTalked());

		// --- MAX_LEVEL ---
		check("MAX_LEVEL 8", Game.MAX_LEVEL == 8);

		if (fails == 0) { System.out.println("ALL PASSED"); System.exit(0); }
		else { System.out.println("FAILS: " + fails); System.exit(1); }
	}
}
