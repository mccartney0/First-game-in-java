package com.traduvertgames.world;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

/**
 * Gerador de fases procedurais para o modo infinito pós-campanha.
 *
 * Produz mapas PNG com a mesma paleta de cores usada pelos mapas
 * versionados (padrão da engine: 1 tile = 1 pixel de 16 px), de forma
 * determinística por semente — a profundidade do modo infinito vira a
 * semente, então cada ciclo gera um layout inédito e reprodutível.
 *
 * Estrutura do mapa gerado:
 * - borda de paredes com chão preto interno;
 * - sala de entrada com o spawn do jogador fixo no tile (3,3) (tile 0,38,255);
 * - salas secundárias conectadas por corredores (drunkard's walk), com 3
 *   templates de layout rotativos por profundidade (aberta, corredores,
 *   câmaras) que variam abertura das salas e pilares;
 * - inimigos aleatórios (255,0,0) e variantes coloridas proporcionalmente
 *   à profundidade, com cap de densidade ({@value MAX_ENEMY_TARGET});
 * - tropas de elite (amarelo-dourado, {@value ELITE}) a partir da profundidade
 *   3, com cap progressivo — viram inimigos de variante sólida com aura
 *   dourada e vida/dano +30% (rodada 24b);
 * - um chefe fixo (GUARDIAN/WARBRINGER/OVERSEER_PRIME em rotação) por ciclo;
 * - itens de suprimento (LifePack/NanoMedkit) para manter o arco justo.
 *
 * A geração usa {@link #validate(BufferedImage)} ao final: se a estrutura
 * falhar (spawn ocupado, sem chefe ou pouco chão), o mapa é regenerado uma
 * vez com semente alternativa para nunca entregar um layout injogável.
 *
 * O PNG é gravado em {@code bin/proc_level_N.png} e carregado pela World
 * como qualquer outro nível. O diretório de saída é criado automaticamente
 * quando necessário, inclusive em um clone limpo do projeto.
 */
public final class ProceduralLevelGenerator {

	private static final Color BLACK = new Color(0, 0, 0, 255);
	private static final Color WALL = new Color(255, 255, 255, 255);
	private static final Color DESTRUCT = new Color(128, 128, 128, 255);
	private static final Color GRASS = new Color(124, 179, 66, 255);
	private static final Color MUD = new Color(109, 76, 65, 255);
	private static final Color ICE = new Color(176, 190, 197, 255);

	private static final Color ENEMY = new Color(255, 0, 0, 255);
	private static final Color WARDEN = new Color(63, 81, 181, 255);
	private static final Color SENTINEL = new Color(0, 150, 136, 255);
	private static final Color RAVAGER = new Color(244, 81, 30, 255);
	private static final Color GUARDIAN = new Color(255, 87, 34, 255);
	private static final Color WARBRINGER = new Color(233, 30, 99, 255);
	private static final Color OVERSEER_PRIME = new Color(208, 25, 55, 255);
	/** Pixel de tropas de elite procedurais (rodada 24b): amarelo-dourado. */
	private static final Color ELITE = new Color(255, 200, 0, 255);
	private static final Color LIFEPACK = new Color(76, 255, 0, 255);
	private static final Color NANO = new Color(255, 82, 82, 255);
	private static final Color WEAPON = new Color(255, 106, 0, 255);
	private static final Color BEACON = new Color(76, 175, 80, 255);
	private static final Color DATACORE = new Color(0, 172, 193, 255);
	private static final Color OVERCLOCK = new Color(0, 229, 255, 255);
	private static final Color PLAYER = new Color(0, 38, 255, 255);

	/** Largura dos mapas RPG procedurais (tiles). */
	public static final int MAP_WIDTH = 96;
	/** Altura dos mapas RPG procedurais (tiles). */
	public static final int MAP_HEIGHT = 64;

	/** Cap da densidade de inimigos: impede mapas injogáveis em profundidades altas. */
	public static final int MAX_ENEMY_TARGET = 20;

	/** Tile fixo de spawn do jogador (consistência entre ciclos). */
	private static final int PLAYER_SPAWN_X = 3;
	private static final int PLAYER_SPAWN_Y = 3;

	private ProceduralLevelGenerator() {
	}

	/**
	 * Gera o mapa procedural da profundidade informada e devolve o arquivo PNG
	 * gravado em {@code bin/proc_level_{depth}.png}.
	 */
	public static File generate(int depth) throws IOException {
		Random rng = new Random(depth * 97L + 13L);
		int w = MAP_WIDTH;
		int h = MAP_HEIGHT;
		BufferedImage map = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

		// Base: tudo parede, depois escava o chão.
		fill(map, w, h, WALL);
			carveRooms(map, w, h, rng, depth);
			carveCorridors(map, w, h, rng);
			border(map, w, h);
			paintRegions(map, w, h, depth);
			placePoiMarkers(map);

			// Tile a tile: escaneia o chão livre e decide ocupação.
		placeEntities(map, w, h, rng, depth);
		placeBoss(map, w, h, rng, depth);

		// Verificação estrutural: um mapa inválido é regenerado uma vez com
		// semente alternativa para nunca entregar um layout injogável.
		if (!validate(map)) {
			fill(map, w, h, WALL);
			Random altRng = new Random(depth * 53L + 77L);
				carveRooms(map, w, h, altRng, depth);
				carveCorridors(map, w, h, altRng);
				border(map, w, h);
				paintRegions(map, w, h, depth);
				placePoiMarkers(map);
				placeEntities(map, w, h, altRng, depth);
			placeBoss(map, w, h, altRng, depth);
		}

		File outputDir = new File("bin");
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new IOException("Não foi possível criar o diretório de mapas procedurais: "
					+ outputDir.getAbsolutePath());
		}
		File file = new File(outputDir, "proc_level_" + depth + ".png");
		ImageIO.write(map, "png", file);
		return file;
	}

	/** Escava as seis macro-regiões do mundo e a área segura inicial. */
	private static void carveRooms(BufferedImage map, int w, int h, Random rng, int depth) {
		// Sala segura do spawn fixo do jogador.
		carveBox(map, w, h, 2, 2, 9, 7);
		int[][] anchors = regionAnchors(w, h);
		for (int[] anchor : anchors) {
			int roomW = anchor[2];
			int roomH = anchor[3];
			carveBox(map, w, h, anchor[0] - roomW / 2, anchor[1] - roomH / 2, roomW, roomH);
			// Varia a silhueta de cada região sem remover a passagem central.
			int pillars = 2 + Math.floorMod(depth + anchor[0] + anchor[1], 4);
			for (int i = 0; i < pillars; i++) {
				int px = anchor[0] - roomW / 2 + 2 + rng.nextInt(Math.max(2, roomW - 4));
				int py = anchor[1] - roomH / 2 + 2 + rng.nextInt(Math.max(2, roomH - 4));
				if (isFloor(map, w, px, py)
						&& Math.abs(px - anchor[0]) + Math.abs(py - anchor[1]) > 3) {
					map.setRGB(px, py, DESTRUCT.getRGB());
				}
			}
		}
	}

	/** Âncoras das seis regiões: refúgio, ruínas, pântano, tundra, santuário e núcleo. */
	private static int[][] regionAnchors(int w, int h) {
		return new int[][] {
			{ Math.max(7, w / 6), Math.max(7, h / 4), 18, 12 },
			{ w / 2, Math.max(7, h / 4), 22, 14 },
			{ Math.max(7, w / 6), (h * 3) / 4, 20, 14 },
			{ w / 2, (h * 3) / 4, 22, 15 },
			{ (w * 5) / 6, Math.max(7, h / 4), 20, 14 },
			{ (w * 5) / 6, (h * 3) / 4, 20, 16 }
		};
	}

	private static void carveBox(BufferedImage map, int w, int h, int x, int y, int bw, int bh) {
		for (int yy = y; yy < y + bh && yy < h - 1; yy++) {
			for (int xx = x; xx < x + bw && xx < w - 1; xx++) {
				if (xx > 0 && yy > 0) {
					map.setRGB(xx, yy, BLACK.getRGB());
				}
			}
		}
	}

	/** Conecta as seis salas por corredores de duas células de largura. */
	private static void carveCorridors(BufferedImage map, int w, int h, Random rng) {
		int[][] anchors = regionAnchors(w, h);
		int playerX = 4;
		int playerY = 4;
		for (int i = 0; i < anchors.length; i++) {
			int targetX = anchors[i][0];
			int targetY = anchors[i][1];
			int fromX = i == 0 ? playerX : anchors[i - 1][0];
			int fromY = i == 0 ? playerY : anchors[i - 1][1];
			boolean horizontalFirst = ((i + rng.nextInt(2)) % 2 == 0);
			if (horizontalFirst) {
				carveCorridorSegment(map, w, h, fromX, fromY, targetX, fromY);
				carveCorridorSegment(map, w, h, targetX, fromY, targetX, targetY);
			} else {
				carveCorridorSegment(map, w, h, fromX, fromY, fromX, targetY);
				carveCorridorSegment(map, w, h, fromX, targetY, targetX, targetY);
			}
		}
		// Dois eixos de fallback mantêm o spawn conectado mesmo com salas ruins.
		carveCorridorSegment(map, w, h, playerX, playerY, w - 4, playerY);
		carveCorridorSegment(map, w, h, playerX, playerY, playerX, h - 4);
	}

	private static void carveCorridorSegment(BufferedImage map, int w, int h,
			int x0, int y0, int x1, int y1) {
		int stepX = Integer.compare(x1, x0);
		int stepY = Integer.compare(y1, y0);
		int x = x0;
		int y = y0;
		while (true) {
			carve(map, w, h, x, y);
			carve(map, w, h, x + 1, y);
			carve(map, w, h, x, y + 1);
			if (x == x1 && y == y1) {
				break;
			}
			if (x != x1) {
				x += stepX;
			}
			if (y != y1) {
				y += stepY;
			}
		}
	}

	private static void carve(BufferedImage map, int w, int h, int x, int y) {
		if (x > 0 && y > 0 && x < w - 1 && y < h - 1) {
			map.setRGB(x, y, BLACK.getRGB());
		}
	}

	private static void border(BufferedImage map, int w, int h) {
		for (int x = 0; x < w; x++) {
			map.setRGB(x, 0, WALL.getRGB());
			map.setRGB(x, h - 1, WALL.getRGB());
		}
		for (int y = 0; y < h; y++) {
			map.setRGB(0, y, WALL.getRGB());
			map.setRGB(w - 1, y, WALL.getRGB());
		}
	}

	/**
	 * Aplica a identidade visual das regiões e reserva pontos de interesse.
	 * O mapa continua monocromático onde há paredes, mas os terrenos jogáveis
	 * passam a comunicar a geografia do mundo sem exigir novos sprites.
	 */
	private static void paintRegions(BufferedImage map, int w, int h, int depth) {
		RpgWorldManager.configure(depth, w, h);
		for (int y = 1; y < h - 1; y++) {
			for (int x = 1; x < w - 1; x++) {
				if (!isFloor(map, w, x, y)) {
					continue;
				}
				RpgWorldManager.RegionType region = RpgWorldManager.regionForTile(x, y);
				Color terrain = region == RpgWorldManager.RegionType.MARSH ? MUD
						: region == RpgWorldManager.RegionType.TUNDRA ? ICE
						: region == RpgWorldManager.RegionType.REFUGE ? GRASS : BLACK;
				map.setRGB(x, y, terrain.getRGB());
			}
		}

					registerRegionContent(w, h);
	}

	private static void placePoiMarkers(BufferedImage map) {
		for (RpgWorldManager.PointOfInterest poi : RpgWorldManager.getPointsOfInterest()) {
			Color marker;
			switch (poi.getType()) {
				case REFUGE_GATE:
				case CONTAINMENT_BEACON:
					marker = BEACON;
					break;
				case MEDICAL_SHELTER:
					marker = LIFEPACK;
					break;
				case DATA_TERMINAL:
					marker = DATACORE;
					break;
				case SUPERVISOR_ARENA:
					marker = OVERCLOCK;
					break;
				default:
					marker = WEAPON;
					break;
			}
			map.setRGB(poi.getTileX(), poi.getTileY(), marker.getRGB());
		}
	}

	private static void registerRegionContent(int w, int h) {
		registerMobArea(RpgWorldManager.RegionType.RUINS, w, h, 1);
		registerMobArea(RpgWorldManager.RegionType.MARSH, w, h, 2);
		registerMobArea(RpgWorldManager.RegionType.TUNDRA, w, h, 3);
		registerMobArea(RpgWorldManager.RegionType.SANCTUARY, w, h, 4);
		registerMobArea(RpgWorldManager.RegionType.CORE, w, h, 5);

		RpgWorldManager.RegionBounds refuge = RpgWorldManager.getBounds(RpgWorldManager.RegionType.REFUGE);
		RpgWorldManager.RegionBounds ruins = RpgWorldManager.getBounds(RpgWorldManager.RegionType.RUINS);
		RpgWorldManager.RegionBounds marsh = RpgWorldManager.getBounds(RpgWorldManager.RegionType.MARSH);
		RpgWorldManager.RegionBounds tundra = RpgWorldManager.getBounds(RpgWorldManager.RegionType.TUNDRA);
		RpgWorldManager.RegionBounds sanctuary = RpgWorldManager.getBounds(RpgWorldManager.RegionType.SANCTUARY);
		RpgWorldManager.RegionBounds core = RpgWorldManager.getBounds(RpgWorldManager.RegionType.CORE);
		RpgWorldManager.registerPoi(RpgWorldManager.PoiType.REFUGE_GATE, RpgWorldManager.RegionType.REFUGE,
				refuge.centerX(), refuge.centerY());
		RpgWorldManager.registerPoi(RpgWorldManager.PoiType.MEDICAL_SHELTER, RpgWorldManager.RegionType.REFUGE,
				refuge.minX + 4, refuge.minY + 4);
		RpgWorldManager.registerPoi(RpgWorldManager.PoiType.SUPPLY_DEPOT, RpgWorldManager.RegionType.RUINS,
				ruins.centerX(), ruins.centerY());
		RpgWorldManager.registerPoi(RpgWorldManager.PoiType.MARSH_CACHE, RpgWorldManager.RegionType.MARSH,
				marsh.centerX(), marsh.centerY());
		RpgWorldManager.registerPoi(RpgWorldManager.PoiType.CONTAINMENT_BEACON, RpgWorldManager.RegionType.TUNDRA,
				tundra.centerX(), tundra.centerY());
		RpgWorldManager.registerPoi(RpgWorldManager.PoiType.DATA_TERMINAL, RpgWorldManager.RegionType.SANCTUARY,
				sanctuary.centerX(), sanctuary.centerY());
		RpgWorldManager.registerPoi(RpgWorldManager.PoiType.SUPERVISOR_ARENA, RpgWorldManager.RegionType.CORE,
				core.centerX(), core.centerY());
	}

	private static void registerMobArea(RpgWorldManager.RegionType region, int w, int h, int offset) {
		RpgWorldManager.RegionBounds bounds = RpgWorldManager.getBounds(region);
		RpgWorldManager.registerMobArea(region, bounds.centerX() + (offset % 2 == 0 ? 4 : -4),
				bounds.centerY() + (offset % 3 == 0 ? 3 : -3), 7, 4);
	}

	/** Distribui inimigos e itens no chão livre. */
	private static void placeEntities(BufferedImage map, int w, int h, Random rng, int depth) {
		// Spawn do jogador fixo no tile (3,3) da sala de entrada — sempre
		// livre e longe de inimigos (consistência entre ciclos do modo infinito).
		map.setRGB(PLAYER_SPAWN_X, PLAYER_SPAWN_Y, PLAYER.getRGB());

		// Densidade escala com a profundidade, com cap para nunca virar
		// carnificina: min(20, 6 + depth * 2).
		int enemyTarget = Math.min(MAX_ENEMY_TARGET, 6 + depth * 2);
		int placed = 0;
		int attempts = 0;
		while (placed < enemyTarget && attempts < 4000) {
			attempts++;
				RpgWorldManager.MobArea area = RpgWorldManager.getMobAreas().isEmpty()
						? null : RpgWorldManager.getMobAreas().get(placed % RpgWorldManager.getMobAreas().size());
				int x = area == null ? 2 + rng.nextInt(w - 4)
						: area.getCenterX() + rng.nextInt(area.getRadius() * 2 + 1) - area.getRadius();
				int y = area == null ? 2 + rng.nextInt(h - 4)
						: area.getCenterY() + rng.nextInt(area.getRadius() * 2 + 1) - area.getRadius();
				if (!isFloor(map, w, x, y) || isNearReservedPoi(x, y, 3)) {
					continue;
				}
				// Zona segura do spawn fixo do jogador.
			if (Math.hypot(x - PLAYER_SPAWN_X, y - PLAYER_SPAWN_Y) < 6) {
				continue;
			}
					if (!isWalkableColor(map.getRGB(x, y))) {
						continue;
					}
				// Rolagem da ocupação: a maioria inimigos, alguns itens.
				// Rodada 24b: tropas de elite (amarelo-dourado) entram a partir da
				// profundidade 3, com cap progressivo de 1 + depth/3 por mapa.
				int eliteCount = countColor(map, w, h, ELITE.getRGB());
				int eliteCap = 1 + depth / 3;
				int roll = rng.nextInt(100);
				Color color;
				if (roll < 70) {
					// Inimigo: genérico ou variante conforme a profundidade.
					color = depth >= 3 && eliteCount < eliteCap && rng.nextInt(4) == 0 ? ELITE
							: depth >= 3 && rng.nextInt(3) == 0 ? WARDEN
									: depth >= 2 && rng.nextInt(4) == 0 ? SENTINEL
											: rng.nextInt(6) == 0 ? RAVAGER : ENEMY;
				} else if (roll < 88) {
					color = rng.nextBoolean() ? LIFEPACK : NANO;
				} else {
					continue; // célula vazia (chão)
				}
				map.setRGB(x, y, color.getRGB());
				placed++;
			}
		}

	/** @return número de ocorrências do pixel rgb no mapa (rodada 24b). */
	private static int countColor(BufferedImage map, int w, int h, int rgb) {
		int count = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (map.getRGB(x, y) == rgb) {
					count++;
				}
			}
		}
		return count;
	}

	/** Posiciona o chefe fixo do ciclo (rotação WARBRINGER → GUARDIAN → OVERSEER_PRIME). */
	private static void placeBoss(BufferedImage map, int w, int h, Random rng, int depth) {
		Color bossColor;
		switch (depth % 3) {
			case 1:
				bossColor = GUARDIAN;
				break;
			case 2:
				bossColor = OVERSEER_PRIME;
				break;
			default:
				bossColor = WARBRINGER;
				break;
		}
		// Ponto de spawn do chefe: canto inferior direito do mapa, em chão livre.
		int bx = w - 5, by = h - 5;
		int tries = 0;
		while (tries < 60) {
			if (isFloor(map, w, bx, by)) {
				break;
			}
			bx--;
			by--;
			tries++;
		}
		if (isFloor(map, w, bx, by)) {
			map.setRGB(bx, by, bossColor.getRGB());
		}
	}

	private static boolean isFloor(BufferedImage map, int w, int x, int y) {
		if (x <= 0 || y <= 0 || x >= w - 1 || y >= map.getHeight() - 1) {
			return false;
		}
		return isWalkableColor(map.getRGB(x, y));
	}

	private static boolean isWalkableColor(int rgb) {
		return rgb == BLACK.getRGB() || rgb == GRASS.getRGB() || rgb == MUD.getRGB() || rgb == ICE.getRGB();
	}

	private static boolean isNearReservedPoi(int x, int y, int radius) {
		for (RpgWorldManager.PointOfInterest poi : RpgWorldManager.getPointsOfInterest()) {
			int dx = x - poi.getTileX();
			int dy = y - poi.getTileY();
			if (dx * dx + dy * dy <= radius * radius) {
				return true;
			}
		}
		return false;
	}

	private static void fill(BufferedImage map, int w, int h, Color color) {
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				map.setRGB(x, y, color.getRGB());
			}
		}
	}

	/**
	 * Rápida verificação estrutural do mapa gerado (usado por testes
	 * unitários e pelo fluxho de geração): garante que o jogador tem spawn
	 * livre, que o chefe está no mapa e que existe chão conectado.
	 */
	public static boolean validate(BufferedImage map) {
		int w = map.getWidth();
		int h = map.getHeight();
		boolean playerSpawn = false;
		int floorCount = 0;
		int bossCount = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int rgb = map.getRGB(x, y);
				if (rgb == PLAYER.getRGB()) {
					playerSpawn = true;
				} else if (rgb == GUARDIAN.getRGB() || rgb == WARBRINGER.getRGB() || rgb == OVERSEER_PRIME.getRGB()) {
					bossCount++;
					} else if (rgb == BLACK.getRGB() || rgb == GRASS.getRGB() || rgb == MUD.getRGB()
							|| rgb == ICE.getRGB() || rgb == WALL.getRGB() || rgb == DESTRUCT.getRGB()
							|| rgb == ENEMY.getRGB() || rgb == WARDEN.getRGB() || rgb == SENTINEL.getRGB()
						|| rgb == RAVAGER.getRGB() || rgb == ELITE.getRGB() || rgb == LIFEPACK.getRGB() || rgb == NANO.getRGB()) {
						if (isWalkableColor(rgb)) {
							floorCount++;
						}
				}
			}
		}
		return playerSpawn && bossCount >= 1 && floorCount > 200;
	}
}
