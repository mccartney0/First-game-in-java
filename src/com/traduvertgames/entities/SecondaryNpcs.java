package com.traduvertgames.entities;

import java.awt.Color;

import com.traduvertgames.dialogue.BranchingNpc;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.InventoryManager;
import com.traduvertgames.quest.SideQuestManager;
import com.traduvertgames.quest.SideQuestManager.Reward;
import com.traduvertgames.quest.SideQuestManager.SideQuest;

/**
 * NPCs secundários com diálogos ramificados e missões secundárias (rodada 22).
 *
 * - Veterano Rex: oferece uma missão de eliminações (KILL_N) com recompensa
 *   de vida/escudo; o progresso acompanha os kills da fase via QuestManager.
 * - Pesquisadora Lila: oferece uma missão de coleta (COLLECT_N) ligada ao
 *   inventário; a entrega é automática quando o jogador junta os itens.
 * - Mercador Finn: oferece uma troca (DELIVER) — entrega Dados criptografados
 *   do inventário em troca de um bônus de mana e score.
 *
 * Instanciar por fase via StoryManager.placeStoryNpcs ou diretamente no
 * registro de entidades da fase.
 */
public final class SecondaryNpcs {

	private SecondaryNpcs() {
	}

	/** Veterano Rex: missão de eliminações. */
	public static InteractiveNpc createVeteranRex(int x, int y) {
		int __level = com.traduvertgames.quest.QuestManager.getCurrentLevel();
		final String questId = __level > com.traduvertgames.main.Game.MAX_LEVEL
				? "rex_kills_deep"
				: "rex_kills_" + __level;
		final int targetKills = 10;
		return new BranchingNpc(x, y, "Veterano Rex", new Color(120, 60, 40),
				new Color(255, 224, 178)) {
			@Override
			protected DialogueNode[] buildNodes() {
				return new DialogueNode[] {
						new DialogueNode(
								"Você tem cara de quem aguenta o tranco, piloto. Já viu gente como eu sobreviver sem lutar — nunca. Posso testar você... ou você pode seguir em frente e se lamentar depois.",
								new String[] { "Aceitar a missão", "Não tenho tempo para isso", "O que ganho com isso?" },
								// Aceitar vai direto ao progresso; não ativa um prompt intermediário.
									new int[] { 3, 4, 2 },
								new Runnable[] {
										() -> {
											SideQuest quest = new SideQuest(questId,
													SideQuestManager.Type.KILL_N, null, targetKills,
													new Reward(60, 20, 0, 150));
											SideQuestManager.register(quest);
											SideQuestManager.activateIfNeeded(questId);
										},
										null, null }),
						new DialogueNode(
								"Espere. Você acha que é fácil? Os comandantes nunca contam quantos caíram antes da fase começar. Mostre serviço e eu cuido da sua recuperação.",
								new String[] { "Aceito o desafio", "Esquece", null },
								new int[] { 3, 4, -1 },
								new Runnable[] {
										() -> {
											SideQuest quest = new SideQuest(questId,
													SideQuestManager.Type.KILL_N, null, targetKills,
													new Reward(60, 20, 0, 150));
											SideQuestManager.register(quest);
											SideQuestManager.activateIfNeeded(questId);
										},
										null, null }),
						new DialogueNode(
								"Honra, piloto. E recursos: kits de reparo, escudo de emergência e crédito extra com os comandantes. Vale cada gota de suor.",
								new String[] { "Então vamos lá", "Passo", null },
								new int[] { 3, 4, -1 },
								new Runnable[] {
										() -> {
											SideQuest quest = new SideQuest(questId,
													SideQuestManager.Type.KILL_N, null, targetKills,
													new Reward(60, 20, 0, 150));
											SideQuestManager.register(quest);
											SideQuestManager.activateIfNeeded(questId);
										},
										null, null }),
						// 3 — nó de progresso (usado após aceitar e conferir)
						new DialogueNode("missao_progresso", new String[] { "", null, null },
								new int[] { -1, -1, -1 },
								new Runnable[] { null, null, null }),
						// 4 — recusa/despedita
						new DialogueNode(
								"Cada um sabe de si, piloto. Quando precisar de um veterano de verdade, eu estarei por aqui.",
								new String[] { null, null, null }, new int[] { -1, -1, -1 },
								new Runnable[] { null, null, null }),
				};
			}

			@Override
			public String getNodeText() {
				DialogueNode node = getNode();
				if (node != null && "missao_progresso".equals(node.text)) {
					if (SideQuestManager.isCompleted(questId)) {
						return "Olha só... cumpriu o combinado. Toma sua recompensa — vida, escudo e crédito. Você tem futuro, piloto.";
					}
					int have = SideQuestManager.getProgress(questId);
					return "Já derrubou " + have + " de " + targetKills + ". Não pare por aqui — o campo ainda está cheio.";
				}
				return super.getNodeText();
			}
		};
	}

	/** Pesquisadora Lila: missão de coleta no inventário. */
	public static InteractiveNpc createResearcherLila(int x, int y) {
		int __level = com.traduvertgames.quest.QuestManager.getCurrentLevel();
		final String questId = __level > com.traduvertgames.main.Game.MAX_LEVEL
				? "lila_collect_deep"
				: "lila_collect_" + __level;
		final int targetItems = 3;
		return new BranchingNpc(x, y, "Pesquisadora Lila", new Color(40, 70, 140),
				new Color(255, 224, 178)) {
			@Override
			protected DialogueNode[] buildNodes() {
				return new DialogueNode[] {
						new DialogueNode(
								"Espere, piloto! Meus sensores indicam células de energia caídas por aqui. Preciso delas para calibrar os escudos da base — pode ajudar?",
								new String[] { "Claro, vou procurar", "Não posso agora", "Por que não vai você?", null },
								new int[] { 1, 3, 2, -1 },
								new Runnable[] {
										() -> {
											SideQuest quest = new SideQuest(questId,
													SideQuestManager.Type.COLLECT_N,
													InventoryManager.ItemType.ENERGY_CELL,
													targetItems, new Reward(20, 30, 80, 100));
											SideQuestManager.register(quest);
											SideQuestManager.activateIfNeeded(questId);
										},
										null, null, null }),
						new DialogueNode(
								"Obrigada! Pegue as células de energia pelo mapa e traga para mim. Eu mesmo detecto quando você tem o suficiente.",
								new String[] { null, null, null }, new int[] { -1, -1, -1 },
								new Runnable[] { null, null, null }),
						new DialogueNode(
								"Meus equipamentos não foram feitos para campo aberto. Você é a linha de frente — eu cuido da ciência. Justiça, não?",
								new String[] { "Tudo bem, aceito", "Sem tempo", null },
								new int[] { 1, 3, -1 },
								new Runnable[] {
										() -> {
											SideQuest quest = new SideQuest(questId,
													SideQuestManager.Type.COLLECT_N,
													InventoryManager.ItemType.ENERGY_CELL,
													targetItems, new Reward(20, 30, 80, 100));
											SideQuestManager.register(quest);
											SideQuestManager.activateIfNeeded(questId);
										},
										null, null }),
						// 3 — recusa
						new DialogueNode(
								"Sem problemas. Os escudos vão ficar instáveis, mas a culpa não será minha.",
								new String[] { null, null, null }, new int[] { -1, -1, -1 },
								new Runnable[] { null, null, null }),
				};
			}

			@Override
			public String getNodeText() {
				DialogueNode node = getNode();
				if (node != null && SideQuestManager.isActive(questId)) {
					int have = SideQuestManager.getProgress(questId);
					if (SideQuestManager.isCompleted(questId)) {
						return "Perfeito! Com essas células os escudos da base voltam a operar em cem por cento. Minha gratidão, piloto.";
					}
					if (have >= targetItems) {
						return "Você já tem o suficiente! As células no seu inventário serão transferidas automaticamente. Obrigada!";
					}
					return "Ainda faltam " + (targetItems - have) + " células de energia. Procure no mapa — elas brilham em amarelo.";
				}
				return super.getNodeText();
			}
		};
	}

	/** Mercador Finn: troca de Dados criptografados por bônus. */
	public static InteractiveNpc createMerchantFinn(int x, int y) {
		return new BranchingNpc(x, y, "Mercador Finn", new Color(90, 60, 120),
				new Color(255, 224, 178)) {
			@Override
			protected DialogueNode[] buildNodes() {
				return new DialogueNode[] {
						new DialogueNode(
								"Ei, piloto... tenho um negócio para você. Seus Dados criptografados valem muito para certos compradores. Quer trocar?",
								new String[] { "O que você oferece", "Não confio em você", null },
								new int[] { 1, 2, -1 },
								new Runnable[] { null, null, null }),
						new DialogueNode(
								"Por cada dado que você entregar: mana restaurada e crédito com os comandantes. Negócio limpo, sem pegadinha... quase.",
								new String[] { "Feito, toma um dado", "Deixa pra lá", null },
								new int[] { 3, 2, -1 },
								new Runnable[] {
										() -> {
											if (InventoryManager.consume(
													InventoryManager.ItemType.DATA_CORE, 1)) {
												Game.player.addMana(50);
												Game.addScore(75);
												com.traduvertgames.entities.FloatingText.show(
														"TROCA COM FINN: +50 MANA +75 PTS",
														Game.WIDTH * Game.SCALE / 2,
														Game.SCALE * 50,
														new Color(255, 235, 59), 90);
											}
										}, null }),
						// 2 — desconfiança/despedita
						new DialogueNode(
								"Desconfiança saudável num lugar como este. Quando mudar de ideia, estarei por aqui.",
								new String[] { null, null, null }, new int[] { -1, -1, -1 },
								new Runnable[] { null, null, null }),
						// 3 — sem dados
						new DialogueNode(
								"Sem dados na mochila? Sem negócio, amigo. Volte quando tiver algo nas mãos.",
								new String[] { null, null, null }, new int[] { -1, -1, -1 },
								new Runnable[] { null, null, null }),
				};
			}

			@Override
			public String getNodeText() {
				DialogueNode node = getNode();
				if (node != null && node.text.equals("Feito, toma um dado")) {
					if (InventoryManager.count(InventoryManager.ItemType.DATA_CORE) <= 0) {
						return "Sem dados na mochila? Sem negócio, amigo. Volte quando tiver algo nas mãos.";
					}
				}
				return super.getNodeText();
			}
		};
	}
}
