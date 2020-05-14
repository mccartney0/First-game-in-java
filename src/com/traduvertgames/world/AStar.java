package com.traduvertgames.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AStar {

	public static double lastTime = System.currentTimeMillis();

	private static Comparator<Node> nodeSorter = new Comparator<Node>() {
		@Override
		public int compare(Node n0, Node n1) {
			if (n1.fCost < n0.fCost)
				return +1;
			if (n1.fCost > n0.fCost)
				return -1;
			return 0;
		}
	};

//	Limpar para otimizar o algoritmo
	public static boolean clear() {
		if (System.currentTimeMillis() - lastTime >= 1000) {
			return true;
		}
		return false;
	}

//	Método A* para encontrar o caminho
	public static List<Node> findPath(World world, Vector2i start, Vector2i end) {
		lastTime = System.currentTimeMillis();
		List<Node> openList = new ArrayList<Node>();
		List<Node> closedList = new ArrayList<Node>();

		// end = ponto final, onde quer q a entidade chege
		Node current = new Node(start, null, 0, getDistance(start, end));

		openList.add(current);

		while (openList.size() > 0) {
			Collections.sort(openList, nodeSorter);
			current = openList.get(0);
			if (current.tile.equals(end)) {
				// Chegamos no ponto final!
				// Basta retornar o valor
				List<Node> path = new ArrayList<Node>();
				while (current.parent != null) {
					path.add(current);
					current = current.parent;
				}
				openList.clear();
				closedList.clear();
				return path;
			}

			openList.remove(current);
			closedList.add(current);

			for (int i = 0; i < 9; i++) {
				if (i == 4)
					continue; // posição 4 = do inimigo, não precisa verificar
				int x = current.tile.x;
				int y = current.tile.y;
				// calculo para fazer num looping só e trabalhar com vetores
				int xi = (i % 3) - 1;
				int yi = (i / 3) - 1;
				// Verifica se o tile existe, se livre vai pra openList
				try {
					Tile tile = World.tiles[x + xi + ((y + yi) * World.WIDTH)];
					if (tile == null)
						continue;
					if (tile instanceof WallTile)
						continue;
					
					// Posibilita o inimigo[NPC] andar em todas direções
					if (i == 0) {

						Tile test = World.tiles[x + xi + 1 + ((y + yi) * World.WIDTH)];
						Tile test2 = World.tiles[x + xi + ((y + yi + 1) * World.WIDTH)];
						if (test instanceof WallTile || test2 instanceof WallTile) {
							continue;
						}
					} else if (i == 2) {
						Tile test = World.tiles[x + xi - 1 + ((y + yi) * World.WIDTH)];
						Tile test2 = World.tiles[x + xi + ((y + yi + 1) * World.WIDTH)];
						if (test instanceof WallTile || test2 instanceof WallTile) {
							continue;
						}
					} else if (i == 6) {
						Tile test = World.tiles[x + xi + ((y + yi - 1) * World.WIDTH)];
						Tile test2 = World.tiles[x + xi + 1 + ((y + yi) * World.WIDTH)];
						if (test instanceof WallTile || test2 instanceof WallTile) {
							continue;
						}
					} else if (i == 8) {
						Tile test = World.tiles[x + xi + ((y + yi - 1) * World.WIDTH)];
						Tile test2 = World.tiles[x + xi - 1 + ((y + yi) * World.WIDTH)];
						if (test instanceof WallTile || test2 instanceof WallTile) {
							continue;
						}
					}
				}catch(ArrayIndexOutOfBoundsException ex){
//					ex.getMessage();
//					ex.printStackTrace();
//					System.out.println("Inimigo fora do mapa");
				}
				
				

				Vector2i a = new Vector2i(x + xi, y + yi);

				// Inimigo[NPC] sempre pegara o caminho mais curto
				double gCost = current.gCost + getDistance(current.tile, a);
				double hCost = getDistance(a, end);

				Node node = new Node(a, current, gCost, hCost);

				// Verifico se esse node acima já tem na closedList
				// e se o gCost q calculei agora é >= current.gCost, se baterem, da o continuar
				// Tudo isso para deixar o caminho mais curto
				if (vecInList(closedList, a) && gCost >= current.gCost)
					continue;

				if (!vecInList(openList, a)) {
					openList.add(node);

				} else if (gCost < current.gCost) {
					openList.remove(current);
					openList.add(node);
				}
			}
		}
		closedList.clear();
		return null;
	}

	// Ver se Node(posição q estou verificando ja está na lista
	private static boolean vecInList(List<Node> list, Vector2i vector) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).tile.equals(vector)) {
				return true;
			}
		}
		return false;
	}

	// Pegando a distancia dos pontos
	private static double getDistance(Vector2i tile, Vector2i goal) {
		double dx = tile.x - goal.x;
		double dy = tile.y - goal.y;

		return Math.sqrt(dx * dx + dy * dy);
	}
}
