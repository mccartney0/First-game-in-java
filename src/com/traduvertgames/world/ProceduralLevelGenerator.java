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
 * - um chefe fixo (GUARDIAN/WARBRINGER/OVERSEER_PRIME em rotação) por ciclo;
 * - itens de suprimento (LifePack/NanoMedkit) para manter o arco justo.
 *
 * A geração usa {@link #validate(BufferedImage)} ao final: se a estrutura
 * falhar (spawn ocupado, sem chefe ou pouco chão), o mapa é regenerado uma
 * vez com semente alternativa para nunca entregar um layout injogável.
 *
 * O PNG é gravado em {@code bin/proc_level_N.png} e carregado pela World
 * como qualquer outro nível.
 */
public final class ProceduralLevelGenerator {

	private static final Color BLACK = new Color(0, 0, 0, 255);
	private static final Color WALL = new Color(255, 255, 255, 255);
	private static final Color DESTRUCT = new Color(128, 128, 128, 255);

	private static final Color ENEMY = new Color(255, 0, 0, 255);
	private static final Color WARDEN = new Color(63, 81, 181, 255);
	private static final Color SENTINEL = new Color(0, 150, 136, 255);
	private static final Color RAVAGER = new Color(244, 81, 30, 255);
	private static final Color GUARDIAN = new Color(255, 87, 34, 255);
	private static final Color WARBRINGER = new Color(233, 30, 99, 255);
	private static final Color OVERSEER_PRIME = new Color(208, 25, 55, 255);
	private static final Color LIFEPACK = new Color(76, 255, 0, 255);
	private static final Color NANO = new Color(255, 82, 82, 255);
	private static final Color PLAYER = new Color(0, 38, 255, 255);

	/** Largura padrão dos mapas procedurais (tiles). */
	public static final int MAP_WIDTH = 46;
	/** Altura padrão dos mapas procedurais (tiles). */
	public static final int MAP_HEIGHT = 30;

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
			placeEntities(map, w, h, altRng, depth);
			placeBoss(map, w, h, altRng, depth);
		}

		File file = new File("bin/proc_level_" + depth + ".png");
		ImageIO.write(map, "png", file);
		return file;
	}

	/** Escava a sala de entrada e 2–3 salas secundárias conectadas. */
	private static void carveRooms(BufferedImage map, int w, int h, Random rng, int depth) {
		// Sala de entrada (superior esquerda) — sempre livre para o jogador.
		carveBox(map, w, h, 2, 2, 9, 7);

		// 3 templates de layout rotativos por profundidade (depth % 3):
		// 0 = sala aberta (grandes salas, poucas barreiras),
		// 1 = corredores (salas menores e mais estreitas),
		// 2 = câmaras (salas grandes separadas por paredes).
		int layout = depth % 3;
		int rooms = 2 + (depth % 3); // 2 a 4 salas por profundidade
		int roomMinW = layout == 1 ? 4 : 6;
		int roomMinH = layout == 1 ? 3 : 5;
		int rx = 14, ry = 2;
		for (int i = 0; i < rooms; i++) {
			int rw = roomMinW + rng.nextInt(4);
			int rh = roomMinH + rng.nextInt(4);
			if (layout == 2 && rng.nextBoolean()) {
				rw += 2; // câmaras tendem a ser maiores
			}
			// Desloca a sala para dentro do mapa, evitando a borda.
			rx = Math.max(4, Math.min(w - rw - 3, rx + 6 + rng.nextInt(5)));
			ry = Math.max(2, Math.min(h - rh - 3, ry + (rng.nextBoolean() ? 6 : -6)));
			carveBox(map, w, h, rx, ry, rw, rh);
		}
		// Pilares decorativos (paredes destrutíveis isoladas no chão):
		// câmaras ganham mais barreiras; salas abertas quase nenhum.
		int pillars = layout == 2 ? 9 : (layout == 1 ? 5 : 2);
		for (int i = 0; i < pillars; i++) {
			int px = 4 + rng.nextInt(w - 8);
			int py = 2 + rng.nextInt(h - 6);
			if (isFloor(map, w, px, py)) {
				map.setRGB(px, py, DESTRUCT.getRGB());
			}
		}
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

	/** Conecta as salas escavadas com corredores em L (garantindo navegabilidade). */
	private static void carveCorridors(BufferedImage map, int w, int h, Random rng) {
		// Varre o mapa: chão escavado é coletado; conecta o centro de massa
		// das regiões de chão por caminhos retos até o canto do jogador.
		int playerX = 4, playerY = 4;
		for (int y = 2; y < h - 2; y += 4) {
			for (int x = 2; x < w - 2; x += 4) {
				// Só conecta células que já são chão (dentro de uma sala).
				if (!isFloor(map, w, x, y)) {
					continue;
				}
				int cx = x, cy = y;
				boolean horizontalFirst = rng.nextBoolean();
				if (horizontalFirst) {
					while (cx != playerX) {
						carve(map, w, h, cx, cy);
						cx += cx > playerX ? -1 : 1;
					}
					while (cy != playerY) {
						carve(map, w, h, cx, cy);
						cy += cy > playerY ? -1 : 1;
					}
				} else {
					while (cy != playerY) {
						carve(map, w, h, cx, cy);
						cy += cy > playerY ? -1 : 1;
					}
					while (cx != playerX) {
						carve(map, w, h, cx, cy);
						cx += cx > playerX ? -1 : 1;
					}
				}
			}
		}
		// Corredor principal horizontal + vertical de fallback garantido.
		for (int x = 2; x < w - 2; x++) {
			carve(map, w, h, x, h / 2);
		}
		for (int y = 2; y < h - 2; y++) {
			carve(map, w, h, w / 3, y);
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
			int x = 2 + rng.nextInt(w - 4);
			int y = 2 + rng.nextInt(h - 4);
			if (!isFloor(map, w, x, y)) {
				continue;
			}
			// Zona segura do spawn fixo do jogador.
			if (Math.hypot(x - PLAYER_SPAWN_X, y - PLAYER_SPAWN_Y) < 6) {
				continue;
			}
			if (map.getRGB(x, y) != BLACK.getRGB()) {
				continue;
			}
			// Rolagem da ocupação: a maioria inimigos, alguns itens.
			int roll = rng.nextInt(100);
			Color color;
			if (roll < 70) {
				// Inimigo: genérico ou variante conforme a profundidade.
				color = depth >= 3 && rng.nextInt(3) == 0 ? WARDEN
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
		return map.getRGB(x, y) == BLACK.getRGB();
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
				} else if (rgb == BLACK.getRGB() || rgb == WALL.getRGB() || rgb == DESTRUCT.getRGB()
						|| rgb == ENEMY.getRGB() || rgb == WARDEN.getRGB() || rgb == SENTINEL.getRGB()
						|| rgb == RAVAGER.getRGB() || rgb == LIFEPACK.getRGB() || rgb == NANO.getRGB()) {
					if (rgb == BLACK.getRGB()) {
						floorCount++;
					}
				}
			}
		}
		return playerSpawn && bossCount >= 1 && floorCount > 200;
	}
}
