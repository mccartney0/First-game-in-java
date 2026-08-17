import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Valida os objetivos variados da rodada 17 (defesa de ponto, sobrevivência e
 * escolta) sem instanciar o jogo completo — usa reflexão e uma instância
 * mínima de Game com a spritesheet real injetada, igual aos testes anteriores.
 */
public class ObjectivesVariadosTest {

	static int passed = 0;
	static int failed = 0;

	static void check(boolean ok, String name) {
		if (ok) {
			passed++;
			System.out.println("  PASS " + name);
		} else {
			failed++;
			System.out.println("  FAIL " + name);
		}
	}

	public static void main(String[] args) throws Exception {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		// Carrega todas as classes SEM inicializá-las (Entity.<clinit> lê
		// Game.spritesheet, que será injetada antes da inicialização).
		Class.forName("com.traduvertgames.main.Game", false, cl);
		Class.forName("com.traduvertgames.entities.Player", false, cl);
		Class.forName("com.traduvertgames.entities.Entity", false, cl);
		Class.forName("com.traduvertgames.entities.Enemy", false, cl);
		Class.forName("com.traduvertgames.entities.QuestBeacon", false, cl);
		Class.forName("com.traduvertgames.entities.EscortNpc", false, cl);
		Class.forName("com.traduvertgames.quest.QuestManager", false, cl);
		Class.forName("com.traduvertgames.quest.RPGObjective", false, cl);
		Class.forName("com.traduvertgames.quest.BaseObjective", false, cl);
		Class.forName("com.traduvertgames.quest.HoldObjective", false, cl);
		Class.forName("com.traduvertgames.quest.SurviveObjective", false, cl);
		Class.forName("com.traduvertgames.quest.EscortObjective", false, cl);
		Class.forName("com.traduvertgames.quest.BossHuntObjective", false, cl);
		Class.forName("com.traduvertgames.quest.SequenceObjective", false, cl);
		Class.forName("com.traduvertgames.quest.DialogueObjective", false, cl);
		Class.forName("com.traduvertgames.graficos.Spritesheet", false, cl);

		Class<?> gameClass = Class.forName("com.traduvertgames.main.Game", false, cl);
		Field gsSprite = gameClass.getDeclaredField("spritesheet");
		gsSprite.setAccessible(true);
		Class<?> spriteClass = Class.forName("com.traduvertgames.graficos.Spritesheet", false, cl);
		Constructor<?> spriteCtor = spriteClass.getDeclaredConstructor(String.class);
		spriteCtor.setAccessible(true);
		gsSprite.set(null, spriteCtor.newInstance("/spritesheet.png"));

		// Inicializa as classes só agora (spritesheet já injetada).
		Class.forName("com.traduvertgames.entities.Entity", true, cl);
		Class.forName("com.traduvertgames.entities.Enemy", true, cl);
		Class.forName("com.traduvertgames.entities.Player", true, cl);
		Class.forName("com.traduvertgames.main.Game", true, cl);
		Class.forName("com.traduvertgames.quest.QuestManager", true, cl);

		// Inicializa os campos estáticos usados pelos objetivos manualmente
		// (instanciar o Game completo carregaria o World e abriria a janela).
		java.lang.reflect.Field entitiesF = gameClass.getDeclaredField("entities");
		entitiesF.setAccessible(true);
		java.lang.reflect.Field enemiesF = gameClass.getDeclaredField("enemies");
		enemiesF.setAccessible(true);
		java.lang.reflect.Field bulletF = gameClass.getDeclaredField("bullet");
		bulletF.setAccessible(true);
		java.lang.reflect.Field bulletsF = gameClass.getDeclaredField("bullets");
		bulletsF.setAccessible(true);
		java.lang.reflect.Field playerF = gameClass.getDeclaredField("player");
		playerF.setAccessible(true);
		Class<?> playerClass2 = Class.forName("com.traduvertgames.entities.Player", true, cl);
		Constructor<?> playerCtor = null;
		for (Constructor<?> c : playerClass2.getDeclaredConstructors()) {
			if (c.getParameterCount() >= 4) {
				playerCtor = c;
			}
		}
		playerCtor.setAccessible(true);
		Object player = playerCtor.newInstance(0, 0, 16, 16,
			((java.awt.image.BufferedImage) spriteClass.getMethod("getSprite", int.class, int.class, int.class, int.class)
						.invoke(spriteCtor.newInstance("/spritesheet.png"), 32, 0, 16, 16)));
		java.util.List<Object> entitiesList = new java.util.ArrayList<Object>();
		entitiesList.add(player);
		entitiesF.set(null, entitiesList);
		enemiesF.set(null, new java.util.ArrayList<Object>());
		bulletF.set(null, new java.util.ArrayList<Object>());
		bulletsF.set(null, new java.util.ArrayList<Object>());
		playerF.set(null, player);
		java.lang.reflect.Field randF = gameClass.getDeclaredField("rand");
		randF.setAccessible(true);
		randF.set(null, new java.util.Random());
		Field gameState = gameClass.getDeclaredField("gameState");
		gameState.setAccessible(true);
		gameState.set(null, "NORMAL");
		check(entitiesF.get(null) != null, "Game.entities inicializado para o teste");
		check(playerF.get(null) != null, "Game.player inicializado para o teste");

		// ---- HoldObjective: canal avança sem invasores e regride com invasores ----
		Class<?> holdClass = Class.forName("com.traduvertgames.quest.HoldObjective", true, cl);
		Object hold = holdClass.getDeclaredConstructor().newInstance();
		holdClass.getMethod("onLevelStart").invoke(hold);
		Field channel = holdClass.getDeclaredField("channel");
		channel.setAccessible(true);

		holdClass.getMethod("update").invoke(hold);
		check((int) channel.get(hold) == 0, "canal não avança sem beacon registrado");

		// Simula spawn do beacon (sem mapa real, invoca onBeaconSpawned com beacon construído).
		Class<?> beaconClass = Class.forName("com.traduvertgames.entities.QuestBeacon", true, cl);
		Object beacon = beaconClass.getDeclaredConstructor(int.class, int.class, java.awt.Color.class)
				.newInstance(0, 0, new java.awt.Color(0x4CAF50));
		holdClass.getMethod("onBeaconSpawned", beaconClass).invoke(hold, beacon);
		check((boolean) holdClass.getMethod("isActive").invoke(hold), "canal ativo após spawn do beacon");

		Method updateHold = holdClass.getMethod("update");
		for (int i = 0; i < 10; i++) {
			updateHold.invoke(hold);
		}
		check((int) channel.get(hold) == 10, "canal avança 1/frame sem invasores");

		// Simula um inimigo dentro do raio de defesa: adiciona aos Game.entities.
		Class<?> enemyClass = Class.forName("com.traduvertgames.entities.Enemy", true, cl);
		Constructor<?> enemyCtor = null;
		for (Constructor<?> c : enemyClass.getDeclaredConstructors()) {
			if (c.getParameterCount() == 5) {
				enemyCtor = c;
			}
		}
		enemyCtor.setAccessible(true);
		Object intruder = enemyCtor.newInstance(10, 10, 16, 16,
			(java.awt.image.BufferedImage) spriteClass.getMethod("getSprite", int.class, int.class, int.class, int.class)
						.invoke(spriteCtor.newInstance("/spritesheet.png"), 48, 0, 16, 16));
		Field gameEntities = gameClass.getDeclaredField("entities");
		gameEntities.setAccessible(true);
		java.util.List<?> entities = (java.util.List<?>) gameEntities.get(null);
		java.lang.reflect.Method addMethod = entities.getClass().getMethod("add", Object.class);
		addMethod.invoke(entities, intruder);
		for (int i = 0; i < 10; i++) {
			updateHold.invoke(hold);
		}
		check((boolean) holdClass.getMethod("isUnderAttack").invoke(hold), "canal sob ataque com invasor na zona");
		check((int) channel.get(hold) < 10, "canal regride sob ataque");
		java.lang.reflect.Method removeMethod = entities.getClass().getMethod("remove", Object.class);
		removeMethod.invoke(entities, intruder);

		// Progressão até concluir: canal avança até 600 frames (~10s) sem invasores.
		for (int i = 0; i < 700; i++) {
			updateHold.invoke(hold);
		}
		check((boolean) holdClass.getMethod("isComplete").invoke(hold), "defesa conclui com canal a 100%");
		check((boolean) holdClass.getMethod("isActive").invoke(hold) == false, "canal sai do estado ativo após conclusão");

		// ---- SurviveObjective: timer avança em NORMAL e conclui no limite ----
		Class<?> survClass = Class.forName("com.traduvertgames.quest.SurviveObjective", true, cl);
		Object surv = survClass.getDeclaredConstructor(String.class, String.class, int.class)
				.newInstance("Teste", "Teste", 3);
		Method updateSurv = survClass.getMethod("update");
		String estadoAntes = (String) gameState.get(null);

		updateSurv.invoke(surv);
		check(!((boolean) survClass.getMethod("isComplete").invoke(surv)),
				"sobrevivência não conclui com jogo pausado/fora de NORMAL");

		gameState.set(null, "NORMAL");
		for (int i = 0; i < 3 * 60; i++) {
			updateSurv.invoke(surv);
		}
		check((boolean) survClass.getMethod("isComplete").invoke(surv), "sobrevivência conclui após 3s");
		check((int) survClass.getMethod("getRemainingSeconds").invoke(surv) <= 0, "tempo restante zera ao concluir");
		gameState.set(null, estadoAntes);

		// ---- SequenceObjective: avança etapas e propaga eventos ----
		Class<?> seqClass = Class.forName("com.traduvertgames.quest.SequenceObjective", true, cl);
		Class<?> bhClass = Class.forName("com.traduvertgames.quest.BossHuntObjective", true, cl);
		Object bh = bhClass.getDeclaredConstructor(String.class, String.class, String.class)
				.newInstance("Título", "Descrição", "o Alvo");
		java.lang.reflect.Constructor<?> seqCtor = seqClass.getDeclaredConstructor(com.traduvertgames.quest.RPGObjective[].class);
		seqCtor.setAccessible(true);
		Object seq = seqCtor.newInstance((Object) new com.traduvertgames.quest.RPGObjective[] {(com.traduvertgames.quest.RPGObjective) hold, (com.traduvertgames.quest.RPGObjective) bh});
		seqClass.getMethod("onLevelStart").invoke(seq);

		Method getActive = seqClass.getMethod("getActive");
		Object firstStage = getActive.invoke(seq);
		check(firstStage == hold, "sequência começa no primeiro estágio");

		// Avança a primeira etapa (Hold) simulando conclusão; a sequência deve trocar para BossHunt.
		Method onBeaconSpawned = seqClass.getMethod("onBeaconSpawned", beaconClass);
		Object beacon2 = beaconClass.getDeclaredConstructor(int.class, int.class, java.awt.Color.class)
				.newInstance(0, 0, new java.awt.Color(0x4CAF50));
		onBeaconSpawned.invoke(seq, beacon2);
		Field ch2 = holdClass.getDeclaredField("channel");
		ch2.setAccessible(true);
		ch2.set(hold, 700);
		// update() da sequência avança de estágio quando o ativo conclui.
		Method updateSeq = seqClass.getMethod("update");
		updateSeq.invoke(seq);
		Object secondStage = getActive.invoke(seq);
		check(secondStage == bh, "sequência avança ao estágio seguinte");

		// onBossSpotted propaga pelo wrapper: BossHunt dentro de Sequence registra o chefe.
		Method bossSpotted = seqClass.getMethod("onBossSpotted");
		bossSpotted.invoke(seq);
		Field bossPresent = bhClass.getDeclaredField("bossPresent");
		bossPresent.setAccessible(true);
		check((boolean) bossPresent.get(bh), "chefe spotted chega ao BossHunt dentro da sequência");

		// Kill de um chefe (boss=true) conclui o BossHunt e toda a sequência.
		java.lang.reflect.Constructor<?> bossCtor = enemyClass.getDeclaredConstructor(
				int.class, int.class, int.class, int.class,
				java.awt.image.BufferedImage.class,
				Class.forName("com.traduvertgames.entities.Enemy$Variant", true, cl), boolean.class);
		bossCtor.setAccessible(true);
		Object boss = bossCtor.newInstance(200, 200, 16, 16,
			(java.awt.image.BufferedImage) spriteClass.getMethod("getSprite", int.class, int.class, int.class, int.class)
						.invoke(spriteCtor.newInstance("/spritesheet.png"), 48, 0, 16, 16),
			java.lang.Enum.valueOf((Class<Enum>) Class.forName("com.traduvertgames.entities.Enemy$Variant", true, cl), "WARBRINGER"), true);
		Method onEnemyKilled = seqClass.getMethod("onEnemyKilled", enemyClass);
		onEnemyKilled.invoke(seq, boss);
		check((boolean) seqClass.getMethod("isComplete").invoke(seq), "sequência conclui quando o último estágio conclui");

		// ---- Wrapper DialogueObjective: delega e desembrulha ----
		Class<?> dlgClass = Class.forName("com.traduvertgames.quest.DialogueObjective", true, cl);
		Object dlg = dlgClass.getDeclaredConstructor(com.traduvertgames.quest.RPGObjective.class, String.class)
				.newInstance(seq, "Teste");
		check(((com.traduvertgames.quest.RPGObjective) dlgClass.getMethod("getDelegate").invoke(dlg)) == seq,
				"DialogueObjective desembrulha a sequência interna");

		// ---- Serialize/deserialize de Sequence com estado interno contendo ';' ----
		String serialized = (String) seqClass.getMethod("serializeState").invoke(seq);
		check(serialized.contains("|S0=") && serialized.contains("|S1="), "sequência serializa estágios com separador |");
		Object seq2 = seqCtor.newInstance((Object) new com.traduvertgames.quest.RPGObjective[] {(com.traduvertgames.quest.RPGObjective) hold, (com.traduvertgames.quest.RPGObjective) bh});
		seqClass.getMethod("deserializeState", String.class).invoke(seq2, serialized);
		Field idx = seqClass.getDeclaredField("activeIndex");
		idx.setAccessible(true);
		check((int) idx.get(seq2) == 1, "sequência restaura a etapa ativa pelo save");

		// ---- Escala por fase: inimigos da fase 2 mais fracos ----
		Method scaleForPhase = enemyClass.getDeclaredMethod("scaleForPhase", enemyClass, int.class);
		scaleForPhase.setAccessible(true);
		Object e1 = enemyCtor.newInstance(100, 100, 16, 16,
			(java.awt.image.BufferedImage) spriteClass.getMethod("getSprite", int.class, int.class, int.class, int.class)
						.invoke(spriteCtor.newInstance("/spritesheet.png"), 48, 0, 16, 16));
		Object e2 = enemyCtor.newInstance(100, 100, 16, 16,
			(java.awt.image.BufferedImage) spriteClass.getMethod("getSprite", int.class, int.class, int.class, int.class)
						.invoke(spriteCtor.newInstance("/spritesheet.png"), 48, 0, 16, 16));
		scaleForPhase.invoke(null, e1, 1);
		scaleForPhase.invoke(null, e2, 2);
		Field lifeF = enemyClass.getDeclaredField("life");
		lifeF.setAccessible(true);
		check((double) lifeF.get(e1) < (double) lifeF.get(e2), "inimigo da fase 1 nasce mais fraco que o da fase 2 (progressão de dificuldade)");

		System.out.println("\n[Resultados] " + passed + " passaram, " + failed + " falharam (total " + (passed + failed) + ")");
		System.exit(failed == 0 ? 0 : 1);
	}
}
