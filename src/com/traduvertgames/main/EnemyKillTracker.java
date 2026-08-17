package com.traduvertgames.main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;

/**
 * Persistência dos inimigos mortos do mapa entre restarts (rodada 25): antes, o
 * "Reiniciar partida" recriava o mapa inteiro e os inimigos que o jogador já
 * tinha matado voltavam a aparecer — a reclamação de que "os mobs da sala
 * mudam". Este rastreador mantém o conjunto de inimigos mortos por nível
 * (identificados por tile e variante) e grava o conjunto no autosave/saves.
 *
 * Ao recarregar o mapa, o {@code applyMapPixels} consulta
 * {@link #isAlreadyDead(int, int, boolean)} e pula a criação dos inimigos
 * cujas posições estão no conjunto de mortos.
 */
public final class EnemyKillTracker {

	private static final Set<String> killSet = new HashSet<String>();
	private static int currentLevel = -1;

	private EnemyKillTracker() {
	}

	/** Chave de identificação do inimigo no tile (x,y) da fase atual. */
	private static String key(int tileX, int tileY, boolean boss) {
		return tileX + "," + tileY + (boss ? "B" : "N");
	}

	/**
	 * Troca a fase rastreada: os mortos de outra fase são preservados em
	 * memória até a troca definitiva (novo jogo zera tudo via {@link #reset()}).
	 */
	public static void setCurrentLevel(int level) {
		currentLevel = level;
	}

	/** Marca o inimigo na posição de tile como morto. */
	public static void markDead(int tileX, int tileY, boolean boss) {
		if (currentLevel < 1) {
			return;
		}
		killSet.add(key(tileX, tileY, boss));
	}

	/** Consulta se o inimigo do tile já foi abatido nesta fase. */
	public static boolean isAlreadyDead(int tileX, int tileY, boolean boss) {
		return killSet.contains(key(tileX, tileY, boss));
	}

	/**
	 * Informa quantos inimigos já caíram nesta fase (usado para restaurar o
	 * contador global Enemy.enemies no carregamento de save).
	 */
	public static int deadCount() {
		return killSet.size();
	}

	/** Serializa o conjunto de mortos para o save (formato: x,y,B|N;...). */
	public static String serialize() {
		StringBuilder sb = new StringBuilder();
		for (String key : killSet) {
			if (sb.length() > 0) {
				sb.append(';');
			}
			sb.append(key);
		}
		return sb.toString();
	}

	/** Restaura o conjunto de mortos do save (chamado pelo SaveManager). */
	@SuppressWarnings("unchecked")
	public static void deserialize(Object raw) {
		killSet.clear();
		if (!(raw instanceof String)) {
			return;
		}
		String state = (String) raw;
		if (state.isEmpty()) {
			return;
		}
		for (String part : state.split(";")) {
			int comma = part.indexOf(',');
			if (comma < 1) {
				continue;
			}
			int tileX;
			int tileY;
			try {
				tileX = Integer.parseInt(part.substring(0, comma));
				tileY = Integer.parseInt(part.substring(comma + 1, part.length() - 1));
			} catch (NumberFormatException ex) {
				continue;
			}
			boolean boss = part.endsWith("B");
			killSet.add(key(tileX, tileY, boss));
		}
	}

	/** Novo jogo ou troca de fase permanente: descarta os mortos rastreados. */
	public static void reset() {
		killSet.clear();
		currentLevel = -1;
	}
}
