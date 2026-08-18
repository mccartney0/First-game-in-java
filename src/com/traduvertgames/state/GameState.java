package com.traduvertgames.state;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.traduvertgames.entities.Bullet;
import com.traduvertgames.entities.BulletShoot;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.Player;
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.world.World;

/**
 * Rodada 28 — estado global do jogo extraído do {@code Game}.
 *
 * <p>O {@code Game.java} (~2.078 linhas) concentrava dezenas de campos
 * {@code static} com reseters dispersos ({@code resetLevelStats},
 * {@code resetTraitorTalked}, {@code toggleOverlayExpanded}...), o que
 * historicamente causou bugs de transição (fase travada, progresso perdido no
 * reload). Esta classe centraliza os dados e, principalmente, os
 * <strong>resets bem definidos</strong> — cada transição de estado do jogo
 * agora tem um único ponto de entrada para limpar o estado.</p>
 *
 * <h3>Escopo dos resets</h3>
 * <ul>
 *   <li>{@link #resetAll()} — novo jogo completo: substitui o
 *       {@code startNewGame} (limpa listas, zera score/nível/transições).</li>
 *   <li>{@link #resetToMainMenu()} — volta ao menu principal preservando o
 *       progresso salvo em disco: zera nível, esconde overlays e transições.</li>
 *   <li>{@link #resetLevel()} — troca de fase/ciclo do modo infinito:
 *       limpa entidades, balas e reinicia as estatísticas da fase
 *       ({@code killsThisLevel}, combo, timer).</li>
 * </ul>
 *
 * <p>Os campos continuam acessíveis por {@code Game.player}/
 * {@code Game.entities} (compatibilidade com as 104 classes) — o {@code Game}
 * agora apenas delega. Rodadas futuras podem migrar os usos para
 * {@code GameState.get().*} sem nova quebra.</p>
 */
public final class GameState {

	private GameState() {}

	// ---------- Constantes ----------

	/** Largura do buffer de jogo (pixels do mundo). */
	public static final int WIDTH = 384;

	/** Altura do buffer de jogo (pixels do mundo). */
	public static final int HEIGHT = 216;

	/** Máximo de fases da campanha. */
	public static final int MAX_LEVEL = 8;

	// ---------- Mundo e entidades ----------

	/** Buffer do mundo renderizado a cada frame. */
	public static BufferedImage bufferImage;

	/** Escala de renderização (janela / buffer). */
	public static int scale = 4;

	/** Spritesheet carregado do classpath. */
	public static Spritesheet spritesheet;

	/** Mundo/tiles da fase atual. */
	public static World world;

	/** Jogador. */
	public static Player player;

		/** Entidades do mapa (NPCs, itens, beacons, aliados...). */
	public static List<Entity> entities = new ArrayList<>();
	/** Inimigos vivos. */
	public static List<Enemy> enemies = new ArrayList<>();


	/**
	 * Balas do jogador.
	 * <p>Atenção: no {@code Game} original, o nome {@code bullet} (sem 's')
	 * guarda as balas do jogador e {@code bullets} guarda as balas inimigas —
	 * a inversão foi mantida na delegação para não quebrar os ~40 arquivos
		 * que referenciam os nomes antigos.</p>
	 */
	public static List<Bullet> bullet = new ArrayList<>();

	/** Balas inimigas (projéteis de {@code BulletShoot}). */
	public static List<BulletShoot> bullets = new ArrayList<>();
	/** RNG compartilhado do jogo. */
	public static final Random rand = new Random();

	// ---------- Fluxo e estado da máquina ----------

	/** Estado da máquina de estados ({@code MENU}, {@code PLAY}, ...). */
	public static String gameState = "MENU";

	/** Fase atual da campanha. */
	public static int currentLevel = 1;

	/** Flags de fluxo. */
	public static boolean restorePhase = false;
	public static boolean saveGame = false;
	public static boolean escapePressed = false;

	// ---------- Score e run atual ----------

	/** Score da partida atual. */
	public static int score = 0;

	/** Recorde local da máquina (por sessão). */
	public static int highScore = 0;

	/** Multiplicador de combo em andamento. */
	public static int comboMultiplier = 1;

	/** Frames restantes do combo ativo. */
	public static int comboTimer = 0;

	/** Melhor combo já registrado (sessão/saves). */
	public static int bestComboRecord = 1;

	/** Melhor combo da run atual. */
	public static int bestComboThisRun = 1;

	/** Kills na fase atual. */
	public static int killsThisLevel = 0;

	/** Instante de início da fase atual (ms). */
	public static long levelStartTime = System.currentTimeMillis();

	// ---------- Overlays e transições ----------

	/** Painel tático expandido (TAB). */
	public static boolean overlayExpanded = false;

	/** Loja pendente de abertura após completar missão. */
	public static boolean shopPendingOpened = false;

	/** Missão concluída aguardando transição. */
	public static boolean questCompletedPending = false;

	/** Diálogo com o traidor já ocorreu (campanha). */
	public static boolean traitorTalked = false;

	/** Fullscreen ativo. */
	public static boolean fullscreen = false;

	/** Offset de centralização do canvas (letterbox). */
	public static int drawOffsetX = 0;

	/** Offset de centralização do canvas (letterbox). */
	public static int drawOffsetY = 0;

	/** Frames restantes do banner de transição de fase. */
	public static int showLevelTransition = 0;

	/** Duração do respiratório entre as transições de fase. */
	public static final int RESPIRO_FRAMES = 150;

	/** Cooldown de transição (impede trocas de fase muito próximas). */
	public static int transitionCooldown = 0;

	/** Alpha do fade de transição. */
	public static int transitionAlpha = 0;

	/** Frames restantes do overlay de dano recebido. */
	public static int damageOverlayFrames = 0;

	/** Duração do overlay de dano recebido. */
	public static final int DAMAGE_OVERLAY_DURATION = 12;

	/** Score base por kill. */
	public static final int BASE_SCORE_PER_KILL = 100;

	/** Multiplicador máximo de combo. */
	public static final int MAX_COMBO_MULTIPLIER = 5;

	/** Duração base do combo em frames. */
	public static final int COMBO_DURATION_FRAMES = 240;

	// ---------- Resets ----------

	/**
	 * Novo jogo completo. Usado pelo {@code startNewGame} e pelo menu de
	 * seleção de fases com save existente. Limpa o estado de execução
	 * mantendo as configurações (janela, spritesheet, mundo base).
	 */
	public static void resetAll() {
		bullet.clear();
		bullets.clear();
		entities.clear();
		enemies.clear();

		currentLevel = 1;
		score = 0;
		comboMultiplier = 1;
		comboTimer = 0;
		bestComboThisRun = 1;
		bestComboRecord = Math.max(1, bestComboRecord);
		killsThisLevel = 0;
		levelStartTime = System.currentTimeMillis();

		restorePhase = false;
		shopPendingOpened = false;
		questCompletedPending = false;
		traitorTalked = false;
		escapePressed = false;
		overlayExpanded = false;
		showLevelTransition = 0;
		transitionCooldown = 0;
		transitionAlpha = 0;
		damageOverlayFrames = 0;
	}

	/**
	 * Troca de fase ou ciclo do modo infinito. Limpa as entidades efêmeras
	 * (mobs, balas, itens soltos) e reinicia as estatísticas da fase — a
	 * campanha e o score da run são preservados.
	 */
	public static void resetLevel() {
		bullet.clear();
		bullets.clear();
		entities.clear();
		enemies.clear();

		killsThisLevel = 0;
		bestComboThisRun = 1;
		comboMultiplier = 1;
		comboTimer = 0;
		questCompletedPending = false;
		shopPendingOpened = false;
		overlayExpanded = false;
		escapePressed = false;
		levelStartTime = System.currentTimeMillis();
	}

	/**
	 * Volta ao menu principal sem perder o progresso salvo em disco.
	 * Usado pelo {@code ESC} no menu e ao fechar/abortar a partida.
	 */
	public static void resetToMainMenu() {
		gameState = "MENU";
		currentLevel = 1;
		overlayExpanded = false;
		escapePressed = false;
		shopPendingOpened = false;
		questCompletedPending = false;
		showLevelTransition = 0;
		transitionCooldown = 0;
		transitionAlpha = 0;
		damageOverlayFrames = 0;
	}

	/**
	 * Cria listas vazias protegidas — evita NPE quando código externo
	 * reatribui as listas ({@code Game.entities = ...}) em vez de limpar.
	 */
	public static List<Entity> newEntities() {
		return new ArrayList<>();
	}

	/** Lista de inimigos vazia. */
	public static List<Enemy> newEnemies() {
		return new ArrayList<>();
	}

	/** Lista de balas do jogador vazia. */
	public static List<Bullet> newPlayerBullets() {
		return new ArrayList<>();
	}

	/** Lista de balas inimigas vazia. */
	public static List<BulletShoot> newEnemyBullets() {
		return new ArrayList<>();
	}

	/** Formata milissegundos como mm:ss (estatísticas de fase). */
	public static String formatLevelTime(long ms) {
		long totalSeconds = ms / 1000;
		long minutes = totalSeconds / 60;
		long seconds = totalSeconds % 60;
		return String.format("%d:%02d", minutes, seconds);
	}

	/** Inicia o timer da fase atual. */
	public static void startLevelTimer() {
		levelStartTime = System.currentTimeMillis();
	}

	/** Milissegundos desde o início da fase atual. */
	public static long getLevelTimeMs() {
		return System.currentTimeMillis() - levelStartTime;
	}
}
