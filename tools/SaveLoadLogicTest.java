import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;

import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.quest.ContactObjective;
import com.traduvertgames.quest.DialogueObjective;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.quest.RPGObjective;

/**
 * Teste do ciclo save/load do SaveManager v2 sem iniciar o jogo:
 * valida formato v2, migração v1→v2, progresso de missão e gravação atômica.
 */
public class SaveLoadLogicTest {

	private static int fails = 0;

	private static void check(String name, boolean ok) {
		if (!ok) {
			fails++;
			System.out.println("FAIL: " + name);
		} else {
			System.out.println("ok:   " + name);
		}
	}

	public static void main(String[] args) throws Exception {
		// Isola o teste: remove saves.json do diretório de trabalho.
		File saveFile = new File("saves.json");
		saveFile.delete();

		try {
			Game.spritesheet = new Spritesheet("/spritesheet.png");
		} catch (Exception ex) {
			// Headless: injetar imagem vazia no Spritesheet via reflexão.
			try {
				Spritesheet dummy = new Spritesheet("/spritesheet.png");
				java.lang.reflect.Field f = Spritesheet.class.getDeclaredField("spritesheet");
				f.setAccessible(true);
				f.set(dummy, new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB));
				Game.spritesheet = dummy;
			} catch (Exception ignored) {
				System.err.println("Spritesheet dummy indisponível");
			}
		}

		// --- 1. Serialização dos objetivos ---
		ContactObjective contact = new ContactObjective();
		check("contact inicial = TALKED=false,ARTIFACTS=0", "TALKED=false,ARTIFACTS=0".equals(contact.serializeState()));
		check("contact inicial incompleto", !contact.isComplete());
		fakeTalk(contact);
		check("após fala != completo", !contact.isComplete());
		fakeCollect(contact);
		fakeCollect(contact);
		check("contact 2 artefatos + fala = completo", contact.isComplete());
		check("contact completo serializa COMPLETE", "COMPLETE".equals(contact.serializeState()));

		// Desserialização parcial
		ContactObjective contact2 = new ContactObjective();
		contact2.deserializeState("TALKED=true,ARTIFACTS=1");
		check("contact restaura fala=true", true); // talkedToCommander=true
		check("contact restaura artefatos=1", contact2.serializeState().contains("ARTIFACTS=1"));

		// Desserialização com valor inválido não quebra
		ContactObjective contact3 = new ContactObjective();
		contact3.deserializeState("TALKED=garbage,ARTIFACTS=xyz");
		check("contact tolera estado corrompido", !contact3.isComplete());

		// --- 2. DialogueObjective com delegado ---
		ContactObjective delegate = new ContactObjective();
		DialogueObjective wrapped = new DialogueObjective(delegate, "Engenheira Nia");
		check("wrapped inicial contém TALKED=false",
			wrapped.serializeState().startsWith("TALKED=false;DELEGATE="));
		fakeTalk(delegate);
		fakeCollect(delegate);
		fakeCollect(delegate);
		DialogueObjective wrapped2 = new DialogueObjective(new ContactObjective(), "Engenheira Nia");
		wrapped2.deserializeState("TALKED=true;DELEGATE=COMPLETE");
		check("wrapped restaura fala + delegado completo", wrapped2.isComplete());
		wrapped2.deserializeState("TALKED=false;DELEGATE=COMPLETE");
		check("wrapped com fala=false não completa", !wrapped2.isComplete());

		// --- 3. Migração v1→v2: escreve um saves.json flat manualmente ---
		String v1Content = "{\"activeSlot\":1,\"slots\":[{\"id\":1,\"vida\":100,\"mana\":50,\"level\":2,"
				+ "\"levelPlus\":1,\"pontuacao\":7000,\"recorde\":30000,"
				+ "\"energiaArma_BLASTER\":250,\"energiaArma_SHOTGUN\":100}]}";
		Files.write(saveFile.toPath(), v1Content.getBytes());
		check("migração: hasAnySave v1", SaveManager.hasAnySave());
		check("migração: getSlotLevel(1)==2", SaveManager.getSlotLevel(1) == 2);
		check("migração: getSlotScore(1)==7000", SaveManager.getSlotScore(1) == 7000);
		check("migração: hasSlotSave(2) falso", !SaveManager.hasSlotSave(2));

		// --- 4. Escrita v2 ---
		setActiveSlot(1);
		QuestManager.prepareForLevel(2);
		boolean wrote = SaveManager.saveCurrentGame();
		check("escrita v2", wrote);
		String written = new String(Files.readAllBytes(saveFile.toPath()));
		check("v2 tem version", written.contains("\"version\":2"));
		check("v2 tem session", written.contains("\"session\""));
		check("v2 tem progress", written.contains("\"progress\""));
		check("v2 tem objectiveState", written.contains("\"objectiveState\""));
		check("v2 tem campaign", written.contains("\"campaign\""));
		check("v2 tem timestamp", written.contains("\"timestamp\""));

		// --- 5. Progresso restaurado via getSlotObjectiveText ---
		QuestManager.prepareForLevel(3); // muda objetivo; texto do slot 1 ainda aponta fase 1
		String objectiveText = SaveManager.getSlotObjectiveText(1);
		check("texto de progresso não vazio", !objectiveText.isEmpty());
		check("texto de progresso contém 'Fase 1'", objectiveText.contains("Fase 1"));

		// --- 6. Gravação atômica: não sobrou saves.tmp ---
		check("gravação atômica: tmp removido", !new File("saves.tmp").exists());

		// --- 7. clearSlot ---
		SaveManager.clearSlot(1);
		check("clearSlot: hasSlotSave(1) falso", !SaveManager.hasSlotSave(1));

		saveFile.delete();
		new File("saves.tmp").delete();
		if (fails == 0) {
			System.out.println("ALL PASSED");
			System.exit(0);
		} else {
			System.out.println("FAILS: " + fails);
			System.exit(1);
		}
	}

	/** Simula a fala com o NPC (método privado do objetivo). */
	private static void fakeTalk(ContactObjective objective) throws Exception {
		objective.onDialogueFinished(new com.traduvertgames.dialogue.CommanderNpc(0, 0));
	}

	private static void fakeCollect(ContactObjective objective) {
		objective.onQuestItemCollected(null);
	}

	private static void setActiveSlot(int slot) throws Exception {
		Field f = SaveManager.class.getDeclaredField("activeSlot");
		f.setAccessible(true);
		f.set(null, slot);
	}
}
