package com.traduvertgames.quest;

import com.traduvertgames.dialogue.CommanderNpc;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.World;

/**
 * Posiciona os NPCs da campanha em pontos temáticos do mapa (em vez de deixá-los
 * sempre no canto superior esquerdo) e fornece a introdução de lore de cada fase.
 */
public final class StoryManager {

	private StoryManager() {
	}

	/**
	 * Posições preferidas (tiles) de cada tipo de NPC por fase. O mapa é o ponto
	 * de encontro da narrativa: a Comandante Ava espera no centro de comando da
	 * fase 1, a Engenheira Nia mantém um esconderijo técnico, o Pesquisador Ivo
	 * trabalha num laboratório central, o Armeiro Mercúrio opera uma forja e o
	 * Técnico Hélio (o traidor) é encontrado no coração do subsolo.
	 */
	private static final int[][] AVA_TILES = {
			{ 16, 11 } };
	private static final int[][] NIA_TILES = {
			{ 28, 6 }, { 18, 22 }, { 30, 12 } };
	private static final int[][] IVO_TILES = {
			{ 18, 11 }, { 18, 12 }, { 22, 8 } };
	private static final int[][] MERCURIO_TILES = {
			{ 22, 14 }, { 14, 14 }, { 30, 14 }, { 38, 22 } };
	private static final int[][] HELIO_TILES = {
			{ 20, 14 } };

	/** Reenquadra os NPCs interativos existentes para posições temáticas. */
	public static void placeStoryNpcs() {
		int level = QuestManager.getCurrentLevel();
		relocate(CommanderNpc.class, AVA_TILES, level);
		relocateByTag("Engenheira Nia", NIA_TILES, level);
		relocateByTag("Pesquisador Ivo", IVO_TILES, level);
		relocateByTag("Armeiro Mercúrio", MERCURIO_TILES, level);
		relocateByTag("Técnico Hélio", HELIO_TILES, level);
		// Rodada 22: NPCs secundários com diálogos ramificados e missões.
		// Veterano Rex (missões de kills) aparece a partir da fase 2;
		// Pesquisadora Lila (coleta) a partir da fase 3; Mercador Finn
		// (troca de dados) a partir da fase 5.
		spawnSecondaryNpcs(level);
	}

	private static void spawnSecondaryNpcs(int level) {
		if (Game.entities == null) {
			return;
		}
		// Cada NPC aparece uma vez por fase (vetor de entidades).
		for (Entity e : Game.entities) {
			if (e instanceof com.traduvertgames.dialogue.InteractiveNpc && isSecondary(e)) {
				return;
			}
		}
		// Rodada 24: as missões secundárias acompanham o piloto também nas
		// Profundezas (modo infinito). Na campanha, os três NPCs aparecem entre
		// as fases 2 e 8; no modo infinito, cada profundidade mantém um NPC
		// rotativo (1 por ciclo), para as missões renderem recompensas sem
		// poluir o layout procedural.
		boolean inCampaign = level >= 2 && level <= 8;
		if (inCampaign) {
			// Campanha (fases 2-8): os três NPCs secundários presentes, cada
			// um com seu tipo de missão.
			Game.entities.add(com.traduvertgames.entities.SecondaryNpcs.createVeteranRex(0, 0));
			Game.entities.add(com.traduvertgames.entities.SecondaryNpcs.createResearcherLila(0, 0));
			Game.entities.add(com.traduvertgames.entities.SecondaryNpcs.createMerchantFinn(0, 0));
		} else if (level > 8) {
			// Modo infinito (Profundezas): um NPC rotativo por profundidade.
			int depth = level - 8;
			switch ((depth - 1) % 3) {
			case 0:
			default:
				Game.entities.add(com.traduvertgames.entities.SecondaryNpcs.createVeteranRex(0, 0));
				break;
			case 1:
				Game.entities.add(com.traduvertgames.entities.SecondaryNpcs.createResearcherLila(0, 0));
				break;
			case 2:
				Game.entities.add(com.traduvertgames.entities.SecondaryNpcs.createMerchantFinn(0, 0));
				break;
			}
		}
		// Reposiciona os NPCs secundários recém-criados para chão válido,
		// espalhados dos NPCs da campanha.
		relocateSecondary("Veterano Rex", REX_TILES, level);
		relocateSecondary("Pesquisadora Lila", LILA_TILES, level);
		relocateSecondary("Mercador Finn", FINN_TILES, level);
	}

	private static final int[][] REX_TILES = {
			{ 10, 8 }, { 34, 18 }, { 14, 20 } };
	private static final int[][] LILA_TILES = {
			{ 26, 16 }, { 12, 14 }, { 36, 8 } };
	private static final int[][] FINN_TILES = {
			{ 32, 20 }, { 20, 6 }, { 8, 16 } };

	private static boolean isSecondary(Entity e) {
		String name = ((com.traduvertgames.dialogue.InteractiveNpc) e).getName();
		return "Veterano Rex".equals(name) || "Pesquisadora Lila".equals(name)
				|| "Mercador Finn".equals(name);
	}

	private static void relocateSecondary(String name, int[][] tiles, int level) {
		if (Game.entities == null) {
			return;
		}
		for (Entity e : Game.entities) {
			if (e instanceof com.traduvertgames.dialogue.InteractiveNpc
					&& name.equals(((com.traduvertgames.dialogue.InteractiveNpc) e).getName())) {
				moveToNearestFreeTile(e, tiles, level);
				return;
			}
		}
	}

	private static void relocate(Class<?> npcClass, int[][] tiles, int level) {
		if (Game.entities == null) {
			return;
		}
		for (Entity e : Game.entities) {
			if (npcClass.isInstance(e)) {
				moveToNearestFreeTile(e, tiles, level);
				return;
			}
		}
	}

	private static void relocateByTag(String name, int[][] tiles, int level) {
		if (Game.entities == null) {
			return;
		}
		for (Entity e : Game.entities) {
			if (e instanceof com.traduvertgames.dialogue.InteractiveNpc
					&& name.equals(((com.traduvertgames.dialogue.InteractiveNpc) e).getName())) {
				moveToNearestFreeTile(e, tiles, level);
				return;
			}
		}
	}

	/** Move a entidade para o tile livre mais próximo da preferência, com
	 *  rotação por fase (tiles[level % tiles.length]) e fallback por anel. */
	private static void moveToNearestFreeTile(Entity e, int[][] tiles, int level) {
		int[] preferred = tiles[level % tiles.length];
		int startRadius = 0;
		int maxRadius = 8;
		for (int radius = startRadius; radius <= maxRadius; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					if (Math.abs(dx) != radius && Math.abs(dy) != radius) {
						continue;
					}
					int tx = preferred[0] + dx;
					int ty = preferred[1] + dy;
					if (World.isValidTile(tx, ty) && !World.isWallTile(tx, ty)) {
						e.setX(tx * 16);
						e.setY(ty * 16);
						return;
					}
				}
			}
		}
		// O tile preferido e o anel de raio 8 falharam (mapa cercado de paredes,
		// como o nível 1): varre o mapa inteiro por ordem de distância do tile
		// preferido e escolhe o primeiro chão válido — assim o NPC nunca fica
		// grudado no canto de spawn, que era a reclamação do jogador.
		int bestTx = -1;
		int bestTy = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int ty = 0; ty < World.HEIGHT; ty++) {
			for (int tx = 0; tx < World.WIDTH; tx++) {
				// Chão válido: o World aplica FloorTile em todos os pixels sem
				// caso específico, então tile null não persiste — basta rejeitar
				// paredes (inclui tiles fora dos limites do mapa).
				if (World.isWallTile(tx, ty)) {
					continue;
				}
				int dist = Math.abs(tx - preferred[0]) + Math.abs(ty - preferred[1]);
				if (dist < bestDist) {
					bestDist = dist;
					bestTx = tx;
					bestTy = ty;
				}
			}
		}
		if (bestTx >= 0) {
			e.setX(bestTx * 16);
			e.setY(bestTy * 16);
		}
		// Sem alternativa válida: mantém a posição original (seguro).
	}

	/** Título de lore exibido ao entrar na fase (banner de abertura). */
	public static String getPhaseLoreTitle(int level) {
		switch (level) {
		case 1:
			return "Operação Socorro";
		case 2:
			return "A Base Secreta de Nia";
		case 3:
			return "O Laboratório de Ivo";
		case 4:
			return "A Forja de Mercúrio";
		case 5:
			return "Sinais do Subsolo";
		case 6:
			return "A Torre do Supervisor";
		case 7:
			return "O Traidor do Subsolo";
		case 8:
			return "O Núcleo Central";
		case 9:
			return "O Vale dos Refugiados";
		default:
			// Rodada 24a — narrativa das Profundezas: ciclos-chave a cada 3
			// profundidades ganham título próprio (Camada Zero, Um, Dois,
			// Três, Quatro), enquanto os níveis intermediários seguem dois
			// estilos alternados ("Setor" e "Câmara de Contenção").
			int depth = level - 8;
			int keyIdx = (depth - 1) / 3;
			if ((depth - 1) % 3 == 0) {
				return DEEP_KEY_TITLES[keyIdx % DEEP_KEY_TITLES.length];
			}
			if (depth % 2 == 0) {
				return "Profundidade " + depth + " — Câmara de Contenção "
						+ DEEP_SECTION_LETTERS[(depth - 1) % DEEP_SECTION_LETTERS.length];
			}
			return "Profundidade " + depth + " — Setor "
					+ DEEP_SECTION_LETTERS[(depth - 1) % DEEP_SECTION_LETTERS.length];
		}
	}

	// Títulos rotativos dos ciclos-chave das Profundezas (trama progressiva).
	private static final String[] DEEP_KEY_TITLES = { "Camada Zero — A Queda",
			"Camada Um — O Abismo da IA", "Camada Dois — Ossuário de Ferro",
			"Camada Três — A Mente Fragmentada",
			"Camada Quatro — O Eco da Colônia" };
	// Letras dos setores das profundezas intermediárias.
	private static final String[] DEEP_SECTION_LETTERS = { "A", "B", "C" };

	/** Linha de lore exibida ao entrar na fase (subtítulo do banner). */
	public static String getPhaseLore(int level) {
		switch (level) {
		case 1:
			return "A colônia enviou um sinal de socorro. Encontre a Comandante Ava e receba as ordens da operação.";
		case 2:
			return "A Engenheira Nia mantém uma base técnica escondida nesta região. Encontre-a para armar a caçada.";
		case 3:
			return "Fragmentos de dados indicam que o Pesquisador Ivo estudava a IA da colônia. Encontre o laboratório dele.";
		case 4:
			return "O Armeiro Mercúrio forja armamento de resistência numa oficina secreta. Busque reforços para o assalto.";
		case 5:
			return "Sinais misteriosos emanam do subsolo. A verdade sobre a colônia está mais perto do que parece.";
		case 6:
			return "A torre de vigilância do Supervisor bloqueia o avanço. Neutralize a sentinela no topo.";
		case 7:
			return "Um desertor confessa a traição no subsolo. Confronte o Técnico Hélio e descubra quem puxa as cordas.";
		case 8:
			return "O Núcleo Central revela a mente por trás da colônia. Esta é a batalha final da campanha.";
		case 9:
			return "Sobreviventes se abrigaram num vale além da colônia. Resgate a líder dos refugiados e abra a passagem de evacuação.";
		default:
			// Rodada 24a — linhas de lore das Profundezas, coerentes com o
			// título da profundidade correspondente.
			int depth = level - 8;
			int keyIdx = (depth - 1) / 3;
			if ((depth - 1) % 3 == 0) {
				return DEEP_KEY_LORE[keyIdx % DEEP_KEY_LORE.length];
			}
			if (depth % 2 == 0) {
				return "Unidades de contenção patrulham esta câmara. O Supervisor"
						+ " mantém reféns do passado da colônia trancados aqui.";
			}
			return "Um setor abandonado da colônia. Os sensores capturam"
					+ " fragmentos de transmissão da mente artificial abaixo.";
		}
	}

	// Lore rotativa dos ciclos-chave das Profundezas (subtítulo do banner).
	private static final String[] DEEP_KEY_LORE = {
			"A primeira queda. O que era a superfície virou ruína — e algo ainda responde aos nossos sinais.",
			"Os restos da inteligência que governava a colônia se dissolvem neste abismo. Ela ainda ouve.",
			"Guardiões de ferro guardam o que restou das tripulações anteriores. Nenhum piloto voltou.",
			"A mente da colônia se despedaçou em ecos. Cada fragmento repete uma ordem que ninguém mais entende.",
			"No fundo, o eco da colônia sussurra o nome de quem construiu tudo isso. Continue descendo." };

	/** NPCs presentes na fase, para o HUD narrativo. */
	public static String getStoryNpcsLabel(int level) {
		switch (level) {
		case 1:
			return "Comandante Ava";
		case 2:
			return "Engenheira Nia";
		case 3:
			return "Pesquisador Ivo";
		case 4:
			return "Armeiro Mercúrio";
		case 5:
			return "Pesquisador Ivo";
		case 6:
			return "Armeiro Mercúrio";
		case 7:
			return "Técnico Hélio, Nia e Mercúrio";
		case 8:
			return "Armeiro Mercúrio";
		case 9:
			return "Curandeiro Léo, Nia e Ivo";
		default:
			return "Nenhum contato";
		}
	}
}
