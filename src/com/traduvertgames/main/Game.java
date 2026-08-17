package com.traduvertgames.main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import com.traduvertgames.entities.Bullet;
import com.traduvertgames.entities.BulletShoot;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.DashAbility;
import com.traduvertgames.entities.UltimateAbility;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.FloatingText;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.graficos.MiniMap;
import com.traduvertgames.graficos.VictoryCutscene;
import com.traduvertgames.graficos.MissionBanner;
import com.traduvertgames.graficos.ParticleSystem;
import com.traduvertgames.graficos.UI;
import com.traduvertgames.world.World;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.dialogue.DialogueManager;

public class Game extends Canvas implements Runnable, KeyListener, MouseListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public static JFrame frame;
	private Thread thread;
	private boolean isRunning = true;
        public static final int WIDTH = 384;
        public static final int HEIGHT = 216;
        /** Escala base; em tela cheia o SCALE é recalculado para caber na resolução. */
        public static int SCALE = 4;

        private static Game instance;

        private static int CUR_LEVEL = 1;
        public static int MAX_LEVEL = 8;
	private BufferedImage image;

	/** Exibe o buffer interno para diagnóstico de HUD em testes automatizados. */
	public static BufferedImage getBufferImage() {
		return instance != null ? instance.image : null;
	}

	public static List<Entity> entities;
	public static List<Enemy> enemies;
	public static List<Bullet> bullet;
	public static List<BulletShoot> bullets;
	public static Spritesheet spritesheet;

        public static World world;

        public static Player player;

        public static Random rand;

        public UI ui;

        public static String gameState = "MENU";
        private boolean showMessageGameOver = true;
        private int framesGameOver = 0;
        private boolean restartGame = false;
        /** Contagem regressiva para voltar ao menu principal após o game over. */
        private int menuReturnTimer = 300;

        public static boolean saveGame = false;
        public int levelPlus = 0;
        public Menu menu;

        private static final int BASE_SCORE_PER_KILL = 100;
        private static final int MAX_COMBO_MULTIPLIER = 5;
        private static final int COMBO_DURATION_FRAMES = 240;

        private static int score = 0;
        private static int highScore = 0;
        private static int comboMultiplier = 1;
        private static int comboTimer = 0;
        private static int bestComboRecord = 1;
        private static int bestComboThisRun = 1;

        /** Kills acumuladas apenas na fase atual (para o card de estatísticas pós-fase). */
        private static int killsThisLevel = 0;
        /** Momento (ms) em que a fase atual começou, para o timer do card de estatísticas. */
        private static long levelStartTime = System.currentTimeMillis();

	private static boolean overlayExpanded = false;

	/** True enquanto a loja está aberta por causa de um objetivo concluído. */
	private static boolean questCompletedPending = false;

	/** Loja aguardando o level-up ser resolvido antes de abrir. */
	private static boolean shopPendingOpened = false;

	/** Cancela um avanço de fase pendente (usado ao trocar de fase manualmente). */
		public static void clearQuestPending() {
		questCompletedPending = false;
		shopPendingOpened = false;
		showLevelTransition = 0;
		transitionAlpha = 0;
	}

	/** True quando o jogador já falou com o desertor do subsolo (fase 7). */
	private static boolean traitorTalked = false;

	public static boolean isTraitorTalked() {
		return traitorTalked;
	}

	/**
	 * True enquanto a tela está no estado de transição de fase: a fase atual
	 * já foi concluída (loja da conclusão aberta) ou o fade preto de troca
	 * de fase ainda está visível. Usado para congelar e ocultar inimigos.
	 */
	public static boolean isTransitioning() {
		return questCompletedPending || showLevelTransition > 0;
	}

	public static void resetTraitorTalked() {
		traitorTalked = false;
	}

	/** Define a flag do desertor do subsolo (usada pelo TraitorNpc e pelos saves). */
	public static void setTraitorTalked(boolean value) {
		traitorTalked = value;
	}
        private static boolean fullscreen = false;

        /** Offset de centralização do jogo na janela (letterboxing em
         * fullscreen/resolução não múltipla do buffer). Os overlays devem
         * somar esses valores às próprias coordenadas. */
        public static int drawOffsetX = 0;
        public static int drawOffsetY = 0;
        /** Frames restantes de exibição do aviso "Fase X concluída — próxima fase". */
        private static int showLevelTransition = 0;
        /** Opacidade do fade preto da transição de fase (150 = totalmente escuro). */
        private static int transitionAlpha = 0;

        public Game() throws IOException {
                instance = this;
                rand = new Random();
                addKeyListener(this);
                addMouseListener(this);
                setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
                setFocusable(true);
                setFocusTraversalKeysEnabled(false);
		initFrame();
// Inicializando objetos;
		ui = new UI();
		image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_BGR);
		entities = new ArrayList<Entity>();
		enemies = new ArrayList<Enemy>();
		bullet = new ArrayList<Bullet>();
		bullets = new ArrayList<BulletShoot>();

		spritesheet = new Spritesheet("/spritesheet.png");
// Passando tamanho dele e posições
                player = new Player(0, 0, 16, 16, spritesheet.getSprite(32, 0, 16, 16));
// Adicionar o jogador na lista e ja aparece na tela
                entities.add(player);
                QuestManager.prepareForLevel(CUR_LEVEL);
                world = new World("/level1.png");
                QuestManager.onLevelLoaded();

                menu = new Menu();
                applyDifficultyToPlayerStats();
	}

        public static Game getInstance() {
                return instance;
        }

        public static int getScore() {
                return score;
        }

        public static void setScore(int value) {
                score = Math.max(0, value);
        }

        public static void addScore(int delta) {
                int newValue = score + delta;
                score = Math.max(0, newValue);
                if (score > highScore) {
                        highScore = score;
                }
        }

	/** Marcador para tecla Escape: usado por telas que consomem o ESC (loja, seleção de fases). */
	public static boolean escapePressed = false;

	/** Enter/Escape consumidos pela cutscene de vitória no update(). */
	private boolean enter = false;
	private boolean escape = false;

        public static int getHighScore() {
                return highScore;
        }

        public static void setHighScore(int value) {
                highScore = Math.max(0, value);
        }

        public static int getComboMultiplier() {
                return comboMultiplier;
        }

        public static int getComboTimer() {
                return comboTimer;
        }

        public static int getComboSecondsRemaining() {
                if (comboTimer <= 0) {
                        return 0;
                }
                return (int) Math.ceil(comboTimer / 60.0);
        }

        public static int getComboBaseDuration() {
                return COMBO_DURATION_FRAMES;
        }

        public static int getMaxComboLimit() {
                return MAX_COMBO_MULTIPLIER;
        }

        public static void applyComboSurge(int bonusMultiplier, int bonusFrames) {
                if (bonusMultiplier > 0) {
                        comboMultiplier = Math.min(MAX_COMBO_MULTIPLIER, comboMultiplier + bonusMultiplier);
                        bestComboThisRun = Math.max(bestComboThisRun, comboMultiplier);
                        bestComboRecord = Math.max(bestComboRecord, bestComboThisRun);
                }
                if (bonusFrames > 0) {
                        int cap = COMBO_DURATION_FRAMES * 2;
                        comboTimer = Math.min(cap, comboTimer + bonusFrames);
                }
        }

        public static int getBestComboRecord() {
                return bestComboRecord;
        }

        public static void setBestComboRecord(int value) {
                bestComboRecord = Math.max(1, value);
        }

        public static int getBestComboThisRun() {
                return bestComboThisRun;
        }

        /** Kills realizadas desde o início da fase atual. */
        public static int getKillsThisLevel() {
                return killsThisLevel;
        }

        /** Duração da fase atual em milissegundos (0 se ainda não iniciou). */
        public static long getLevelTimeMs() {
                long elapsed = System.currentTimeMillis() - levelStartTime;
                return Math.max(0, elapsed);
        }

        /** Zera kills e timer ao iniciar uma nova fase (inclui ciclos do modo infinito). */
        public static void resetLevelStats() {
                // Antes de zerar os contadores da fase, captura a melhor partida
                // (bestRun global do save) com base na fase que acabou de terminar.
                com.traduvertgames.main.SaveManager.captureBestRun();
                killsThisLevel = 0;
                bestComboThisRun = 1;
                comboMultiplier = 1;
                comboTimer = 0;
                levelStartTime = System.currentTimeMillis();
        }

        /** Inicia o timer da fase (usado por World.restartGame ao trocar de mapa). */
        public static void startLevelTimer() {
                levelStartTime = System.currentTimeMillis();
        }

        /** Formata milissegundos como mm:ss para o card de estatísticas. */
        public static String formatLevelTime(long ms) {
                long totalSeconds = ms / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                return String.format("%d:%02d", minutes, seconds);
        }

        public static void setBestComboThisRun(int value) {
                bestComboThisRun = Math.max(1, value);
        }

        public static boolean isOverlayExpanded() {
                return overlayExpanded;
        }

        public static void toggleOverlayExpanded() {
                overlayExpanded = !overlayExpanded;
        }

                public static void setCurrentLevel(int level) {
                if (level < 1)
                        level = 1;
                if (level > MAX_LEVEL)
                        level = MAX_LEVEL;
                CUR_LEVEL = level;
        }
        public static int getCurrentLevel() {
                return CUR_LEVEL;
        }

        public void setLevelPlus(int value) {
                if (value < 0)
                        value = 0;
                this.levelPlus = value;
        }

        public int getLevelPlus() {
                return this.levelPlus;
        }

        /** Acesso estático seguro à profundidade do modo infinito (sem jogo ativo = 0). */
        public static int getStaticLevelPlus() {
                Game game = getInstance();
                return game != null ? game.levelPlus : 0;
        }

        public static void registerEnemyKill() {
                LevelUpManager.grantKillXp();
                killsThisLevel++;
                int points = BASE_SCORE_PER_KILL * comboMultiplier;
                score += points;
                if (score > highScore) {
                        highScore = score;
                }

                bestComboThisRun = Math.max(bestComboThisRun, comboMultiplier);
                bestComboRecord = Math.max(bestComboRecord, bestComboThisRun);

                comboTimer = COMBO_DURATION_FRAMES;
                if (comboMultiplier < MAX_COMBO_MULTIPLIER) {
                        comboMultiplier++;
                }
        }

	public static void registerPlayerDamage() {
		if (comboMultiplier > 1) {
			bestComboRecord = Math.max(bestComboRecord, comboMultiplier);
		}
		comboMultiplier = 1;
		comboTimer = 0;
		damageOverlayFrames = Math.max(damageOverlayFrames, DAMAGE_OVERLAY_DURATION);
	}

		/** Duração (frames) da vinheta vermelha exibida quando o jogador toma dano. */
	private static final int DAMAGE_OVERLAY_DURATION = 12;
	/** Frames restantes da vinheta de dano. */
	private static int damageOverlayFrames = 0;

	public static double getDamageTakenMultiplier() {
                return OptionsConfig.getDamageTakenMultiplier();
        }

	/** Recalcula o SCALE para que o buffer (384x216) caiba na área útil atual. */
		public static void recomputeScale() {
		int width = Math.max(1, frame.getContentPane().getWidth());
		int height = Math.max(1, frame.getContentPane().getHeight());
		SCALE = Math.max(1, Math.min(width / WIDTH, height / HEIGHT));
	}
	/** Registra um listener que recompõe o SCALE sempre que a janela muda de
	 * tamanho (incluindo a alternância de tela cheia com F11). */
	public static void installResizeListener() {
		frame.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				// Modo tela cheia (F11, maximização nativa): recalcula o SCALE
				// para preencher o monitor mantendo a nitidez da pixel art.
				if (fullscreen) {
					recomputeScale();
					return;
				}
				// Modo janela: o tamanho alvo do jogo é fixo (buffer * SCALE).
				// Se a janela mudou de tamanho por qualquer motivo (botão de
				// maximizar, arrastar para outro monitor, acidental), o jogo
				// volta ao tamanho exato e recentraliza — sem isso a escala
				// errada cortava o jogo, criava áreas pretas e deslocava a mira.
				int targetW = WIDTH * SCALE;
				int targetH = HEIGHT * SCALE;
				java.awt.Dimension c = frame.getContentPane().getSize();
				int curW = Math.max(1, c.width);
				int curH = Math.max(1, c.height);
				if (Math.abs(curW - targetW) > 4 || Math.abs(curH - targetH) > 4) {
					int dw = targetW - curW;
					int dh = targetH - curH;
					frame.setSize(frame.getWidth() + dw, frame.getHeight() + dh);
					frame.setLocationRelativeTo(null);
				}
			}
		});
	}

	/** Alterna tela cheia (F11): maximiza e ajusta o SCALE à resolução do monitor.
	 * Em monitores cuja resolução não é múltiplo exato do buffer, o jogo fica
	 * centralizado (letterboxing preto) para preservar a nitidez da pixel art
	 * e a escala correta da HUD. */
	public static void toggleFullscreen() {
		// O modo de tela cheia usa maximização nativa da janela (MAXIMIZED_BOTH),
		// e NAO o modo exclusivo do GraphicsDevice: o modo exclusivo exige
		// dispose/setUndecorated, que no Windows gera janelas "fantasmas", deixa o
		// estado de maximizado corrompido ao sair (janela gigante que não volta
		// ao tamanho normal) e é bloqueado por alguns drivers/composicao DWM.
		if (!fullscreen) {
			frame.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
			fullscreen = true;
		} else {
			frame.setExtendedState(javax.swing.JFrame.NORMAL);
			frame.setSize(WIDTH * SCALE, HEIGHT * SCALE);
			frame.setLocationRelativeTo(null);
			fullscreen = false;
		}
		recomputeScale();
	}

		public void initFrame() {
		frame = new JFrame("Game 2 RPG");
		frame.add(this);
		frame.setResizable(false);
		// O fundo fora da área do jogo é preto (antes ficava branco, que
		// ficava visível como faixa ao redimensionar ou em tela cheia).
		frame.setBackground(java.awt.Color.BLACK);
		frame.getContentPane().setBackground(java.awt.Color.BLACK);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		installResizeListener();
		frame.setVisible(true);
		recomputeScale();
	}

	public synchronized void start() {
		thread = new Thread(this);
		isRunning = true;
		thread.start();
	}

	public synchronized void stop() {
		isRunning = false;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		Game game = new Game();
		game.start();
	}

	// Toda a lógica fica no update ou tick
	//Primeiro atualiza, depois renderiza
	public void update() {
		// Avança de fase assim que a loja aberta por objetivo concluído fecha.
		// DEVE rodar antes da lógica do estado NORMAL: se o avanço rodasse depois,
		// o bloco NORMAL reabriria a loja (objetivo ainda parece completo) e o jogo
		// ficaria preso na loja para sempre — era o bug do ESC piscando.
		// Cutscene de vitória: encerra a campanha ao concluir a fase 6.
		if (com.traduvertgames.graficos.PhaseStatsScreen.isShowing()) {
			// Card de estatísticas pós-fase: intercepta Enter/ESC enquanto visível.
			com.traduvertgames.graficos.PhaseStatsScreen.update(enter, escape);
			enter = false;
			escape = false;
		} else if (VictoryCutscene.isShowing()) {
			com.traduvertgames.graficos.VictoryCutscene.update(enter, escape);
			enter = false;
			escape = false;
		} else if (questCompletedPending && CUR_LEVEL == 8) {
			// Fim da campanha: cutscene de vitória antes de entrar no modo sobrevivência.
			questCompletedPending = false;
			if (SaveManager.saveCurrentGame()) {
				System.out.println("Jogo salvo no slot " + SaveManager.activeSlot + " (campanha concluída)!");
			}
			// Recompensa final da campanha: arma de elite desbloqueada.
			grantCampaignReward();
			com.traduvertgames.graficos.VictoryCutscene.start();
		} else if (questCompletedPending) {
			// Conclusão da fase 7: recompensa de arma da campanha (Canhão de Vazio).
			if (CUR_LEVEL == 7) {
				grantCampaignReward();
			}
			questCompletedPending = false;
			advanceToNextLevel();
			if (QuestManager.isObjectiveComplete()) {
				questCompletedPending = false;
			}
		}

                if ("NORMAL".equals(gameState)) {
// Salvar o jogo (formato JSON correto com slots)
                        if (Game.saveGame) {
                                Game.saveGame = false;
                                levelPlus = 0;
                                if (SaveManager.saveCurrentGame()) {
                                        System.out.println("Jogo salvo no slot " + SaveManager.activeSlot + "!");
                                }
                        }

                        this.restartGame = false; // Prevenção
                        updateComboTimer();

                        DashAbility.update();
                        UltimateAbility.update();
                        WaveManager.update();
                        LevelUpManager.update();
                        LootGuarantee.update();

			for (int i = 0; i < entities.size(); i++) {
				Entity e = entities.get(i);
				// Durante o onboarding, inimigos ficam paralisados para o novato
				// praticar sem risco (Player continua atualizando normalmente).
if (e instanceof Enemy && (OnboardingManager.isEnemyPaused() || DialogueManager.isEnemyPaused())) {
						continue;
					}
				e.update();
			}
			OnboardingManager.update();

                        for (int i = 0; i < bullets.size(); i++) {
                                bullets.get(i).update();
                        }
                        for (int i = 0; i < bullet.size(); i++) {
                                bullet.get(i).update();
                        }

				QuestManager.update();
				ParticleSystem.update();
				FloatingText.update();
				MissionBanner.update();

                        if (QuestManager.isObjectiveComplete()) {
                                onObjectiveComplete();
                        }
                } else if ("SHOP".equals(gameState)) {
                        ShopManager.update();
                        ParticleSystem.update();
		} else if ("GAMEOVER".equals(gameState)) {
//Forma de Fazer animação - Game over
			this.framesGameOver++;
			if (this.framesGameOver == 30) {
				this.framesGameOver = 0;
				if (this.showMessageGameOver)
					this.showMessageGameOver = false;
				else
					this.showMessageGameOver = true;
			}

                        if (restartGame) {
                                handleGameOverRestart();
                        }
                        // Volta ao menu principal automaticamente após alguns
                        // segundos (Enter/click reiniciam a partida antes disso).
                        if (this.menuReturnTimer > 0) {
					this.menuReturnTimer--;
					if (this.menuReturnTimer == 0) {
						returnToMainMenu();
					}
				}
                        // Autosave ao morrer: preserva o progresso da partida.
                        SaveManager.saveAutoSave();
		}else if ("MENU".equals(gameState)) {
				//Menu
				//Iniciando a camera junto com o jogador
				if (!showInitialWeaponSelect) {
					player.updateCamera();
					menu.update();
				}
	} else if ("LEVELUP".equals(gameState)) {
			LevelUpManager.update();
			// Se o level-up fechou neste frame (Enter/ESC) e havia uma fase
			// concluída aguardando a loja, abre a loja agora — o level-up
			// sempre tem prioridade sobre a loja entre fases.
			if (!LevelUpManager.isShowingLevelUp() && shopPendingOpened) {
				shopPendingOpened = false;
				ShopManager.open();
			}
		} else if ("LEVELSELECT".equals(gameState)) {
			LevelSelectScreen.update();
	}

		if (showLevelTransition > 0) {
			showLevelTransition--;
		}
		// Fade preto da transição de fase: escurece a fase antiga e some
		// suavemente para revelar a nova (fade de ~50 frames).
		if (transitionAlpha > 0) {
			transitionAlpha = Math.max(0, transitionAlpha - 3);
		}
	}

	/** Quando o objetivo da fase é completado: abre a loja antes de avançar. */
	private void onObjectiveComplete() {
		if (WaveManager.isArenaMode()) {
			return;
		}
		// Já em transição ou loja aberta: não reabrir nem marcar novamente.
		if (questCompletedPending || ShopManager.isOpen()) {
			return;
		}
		// Marca que a fase foi concluída: ao fechar a loja (compra ou ESC),
		// o jogo avança automaticamente para a próxima fase.
		questCompletedPending = true;
		// A fase terminou: inimigos ficam paralisados na hora (não seguem
		// atacando enquanto a loja está aberta) e os projéteis inimigos em
		// voo são removidos para acalmar a tela.
		for (int i = bullets.size() - 1; i >= 0; i--) {
			bullets.remove(i);
		}
		// Feedback narrativo: banner central de missão concluída com o título da fase.
		com.traduvertgames.graficos.MissionBanner.reset();
		com.traduvertgames.graficos.MissionBanner.showComplete(QuestManager.getPhaseTitle(CUR_LEVEL));
		// Fanfarra de fase concluída (rodada 15): antes reutilizava o level-up.
		com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.LEVEL_COMPLETE);
		// O level-up tem prioridade sobre a loja: se o jogador subiu de nível
		// no mesmo instante em que concluiu a fase, a loja aguarda o level-up
		// ser resolvido antes de abrir (evita oscilação entre as telas).
		if (LevelUpManager.isShowingLevelUp()) {
			shopPendingOpened = true;
			return;
		}
		ShopManager.open();
	}

        public void render() { // Renderização funciona por ordem de código, primeira linhas, segunda, etc...

                BufferStrategy bs = this.getBufferStrategy();
                if (bs == null) {
                        this.createBufferStrategy(3);
			return;
		}
		Graphics g = image.getGraphics();
		g.setColor(new Color(0, 0, 0)); // Cor de fundo
		g.fillRect(0, 0, WIDTH, HEIGHT);

// Renderizar jogo //
		world.render(g);
		// Fase concluída: inimigos e seus projéteis ficam invisíveis — a
		// próxima fase carrega limpa, sem "ruído" herdado da anterior.
		for (int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			if (questCompletedPending && e instanceof Enemy) {
				continue;
			}
			e.render(g);
		}
		for (int i = 0; i < bullets.size(); i++) {
			bullets.get(i).render(g);
		}
		for (int i = 0; i < bullet.size(); i++) {
			bullet.get(i).render(g);
		}
		// Fade preto da transição de fase (desenha por cima do jogo).
		if (transitionAlpha > 0) {
			g.setColor(new Color(0, 0, 0, transitionAlpha));
			g.fillRect(0, 0, WIDTH, HEIGHT);
		}
		// A HUD compacta é desenhada exclusivamente pelo overlay (por cima de tudo),
		// evitando HUD duplicada/esmaecida em menus, loja e game over.
		// ui.render(g) (coordenadas do buffer, por baixo dos overlays) foi removido.
		ParticleSystem.render(g);
		FloatingText.render(g, SCALE);
		MissionBanner.render(g);
		UltimateAbility.render(g);
		g.dispose();

                g = bs.getDrawGraphics();
                int scaledWidth = WIDTH * SCALE;
                int scaledHeight = HEIGHT * SCALE;
                int windowWidth = getWidth();
                int windowHeight = getHeight();
                // Preenche a janela inteira de preto: sem isso, a área que
                // sobra quando a tela não é múltipla do buffer ficava branca
                // (faixa visível em tela cheia ou ao redimensionar).
                g.setColor(java.awt.Color.BLACK);
                g.fillRect(0, 0, windowWidth, windowHeight);
                // O jogo é desenhado a partir do canto (0,0) para que todos os
                // overlays (HUD, minimapa, loja, level-up) continuem corretos;
                // a sobra da janela já está coberta pelo preenchimento preto.
                // Centraliza o jogo na janela quando ela for maior que o
                // canvas escalado (fullscreen com resolution não múltipla do
                // buffer — letterboxing preto nas bordas).
                int offsetX = Math.max(0, (windowWidth - scaledWidth) / 2);
                int offsetY = Math.max(0, (windowHeight - scaledHeight) / 2);
                drawOffsetX = offsetX;
                drawOffsetY = offsetY;
                g.drawImage(image, offsetX, offsetY, offsetX + scaledWidth, offsetY + scaledHeight, null);
                // Translada os overlays para o espaço do jogo centralizado
                // (fullscreen com letterboxing), revertendo depois.
                Graphics2D overlayG = (Graphics2D) g.create();
                overlayG.translate(offsetX, offsetY);
		// Durante a transição de fase (banner de conclusão / lore) as HUDs
		// de combate ficam escondidas para deixar o momento mais limpo.
		boolean hidingHud = questCompletedPending || showLevelTransition > 0;
		if (!hidingHud) {
		MiniMap.render(overlayG);
		LevelUpManager.render(overlayG);
		LevelSelectScreen.render(overlayG);
		WaveManager.render(overlayG);
		LootGuarantee.render(overlayG);
		// A loja tem seu próprio painel completo com fundo escuro; renderizá-la
		// aqui (bloco NORMAL) fazia o painel desenhar DUAS vezes por frame
		// (uma aqui e outra no bloco SHOP) e misturava a HUD compacta por cima
		// — era a causa do "menu de vida aparecendo no fundo da loja".
		if (!"SHOP".equals(gameState)) {
			ShopManager.render(overlayG);
		}
		}
		// ui.renderOverlay é desenhado por último para que a HUD compacta
		// (e os cards do painel tático) fiquem sobre o overlay escuro da loja
		// e sobre os demais painéis, sem parecer esmaecida no fundo.
	ui.renderOverlay(overlayG);
	if (!hidingHud) {
	com.traduvertgames.graficos.MissionHud.render(overlayG);
	}
		com.traduvertgames.graficos.VictoryCutscene.render(overlayG, SCALE);
		com.traduvertgames.graficos.PhaseStatsScreen.render(overlayG, SCALE);
	// OBS: o dispose do overlayG foi MOVIDO para o final do render — antes ele
	// ficava aqui, logo após a HUD, e todos os overlays seguintes (dano, diálogos,
	// onboarding, seleção de arma, menu, cutscene de vitória, stats, level-up,
	// loja, level select) desenhavam em um Graphics já descartado: eles
	// silenciosamente NÃO apareciam. Isso fazia o menu principal sumir e o
	// jogo parecer "preso" na primeira fase sem qualquer interface.
	if (damageOverlayFrames > 0) {
		int alpha = Math.max(0, (int) (70.0 * damageOverlayFrames / DAMAGE_OVERLAY_DURATION));
		overlayG.setColor(new Color(180, 30, 30, alpha));
		overlayG.fillRect(0, 0, scaledWidth, scaledHeight);
		damageOverlayFrames--;
	}
	DialogueManager.render(overlayG);
	OnboardingManager.render(overlayG);
	// A tela de escolha de arma inicial é desenhada por cima de tudo (inclusive
	// do menu de pausa): durante a seleção o estado é MENU com pause=true, mas o
	// menu principal não deve aparecer por cima das opções de arma.
	renderInitialWeaponSelect(overlayG, scaledWidth, scaledHeight);

		if ("GAMEOVER".equals(gameState)) {
                        Graphics2D g2 = overlayG;
                        g2.setColor(new Color(0, 0, 0, 120));
                        g2.fillRect(0, 0, scaledWidth, scaledHeight);
                        g.setFont(new Font("arial", Font.BOLD, 36));
                        g.setColor(Color.white);
                        drawCenteredString(overlayG, "Game Over", scaledHeight / 2 - 50);
                        g.setFont(new Font("arial", Font.BOLD, 28));

                        if (showMessageGameOver) {
                                drawCenteredString(overlayG, ">Pressione Enter para reiniciar — ESC para o menu<", scaledHeight / 2 + 4);
                        }
                        g.setFont(new Font("arial", Font.PLAIN, 16));
                        g.setColor(new Color(200, 200, 200));
                        if (menuReturnTimer > 0) {
                                drawCenteredString(overlayG, "Voltando ao menu em " + ((menuReturnTimer + 29) / 30) + "s...",
                                                scaledHeight / 2 + 142);
                        }

                        g.setFont(new Font("arial", Font.BOLD, 24));
                        drawCenteredString(overlayG, "Pontuação final: " + Game.getScore(), scaledHeight / 2 + 52);
                        drawCenteredString(overlayG, "Recorde: " + Game.getHighScore(), scaledHeight / 2 + 82);
                        drawCenteredString(overlayG, "Melhor combo da partida: x" + Game.getBestComboThisRun(), scaledHeight / 2 + 112);

                } else if ("MENU".equals(gameState)) {
                        if (!showInitialWeaponSelect) {
			menu.render(overlayG);
                        }
                        // Durante a seleção de arma inicial o menu não deve
                        // desenhar nada: o overlay da própria tela de arma
                        // escurece o fundo e desenha a lista por cima.
		} else if ("SHOP".equals(gameState)) {
				// Painel único da loja (fundo escuro + lista + feedback + dica).
				// Sem Menu.renderPauseScreen atrás: o overlay do menu de pausa
				// era desenhado com a HUD "em melhor qualidade" no fundo, o que
				// deixava a loja com aparência duplicada.
				ShopManager.render(overlayG);
			} else if ("LEVELUP".equals(gameState)) {
                        // Tela de level up já renderiza por cima do jogo (LevelUpManager.render).
			} else if ("LEVELSELECT".equals(gameState)) {
				LevelSelectScreen.render(overlayG);
                }

                // Aviso de transição de fase: a fase atual foi concluída e o jogo
                // avança para a próxima assim que a loja/level up forem encerrados.
                if (showLevelTransition > 0) {
                        Graphics2D g2 = overlayG;
                        g2.setColor(new Color(0, 0, 0, 190));
                        g2.fillRect(0, scaledHeight / 2 - 60, scaledWidth, 120);
                        g2.setColor(new Color(255, 235, 59));
                        g.setFont(new Font("arial", Font.BOLD, 26));
                        drawCenteredString(overlayG, "Fase " + Game.getCurrentLevel() + " concluída!", scaledHeight / 2 - 34);
                        String nextTitle;
                        String nextObjective = QuestManager.getObjectiveTitle();
                        if (QuestManager.isSurvivalMode()) {
                                nextTitle = "Campanha concluída — Modo Sobrevivência";
                        } else {
                                int nextLevel = Math.min(Game.getCurrentLevel() + 1, MAX_LEVEL);
                                nextTitle = "Próxima fase: " + QuestManager.getPhaseTitle(nextLevel);
                        }
                        g2.setColor(Color.WHITE);
                        g.setFont(new Font("arial", Font.BOLD, 16));
                        drawCenteredString(overlayG, nextTitle, scaledHeight / 2 - 4);
                        g2.setColor(new Color(176, 190, 197));
                        g.setFont(new Font("arial", Font.PLAIN, 13));
                        drawCenteredString(overlayG, "Missão: " + nextObjective, scaledHeight / 2 + 20);
                }
                overlayG.dispose();
                bs.show();
        }

        private void drawCenteredString(Graphics g, String text, int baselineY) {
                int width = WIDTH * SCALE;
                FontMetrics metrics = g.getFontMetrics();
                int textX = (width - metrics.stringWidth(text)) / 2;
                g.drawString(text, textX, baselineY);
        }

        private void updateComboTimer() {
                if (comboTimer > 0) {
                        comboTimer--;
                        if (comboTimer <= 0) {
                                comboTimer = 0;
                                comboMultiplier = 1;
                        }
                }
        }

        @Override
        public void run() {

		long lastTime = System.nanoTime();
		double amountOfUpdates = 60.0;
		double ns = 1000000000 / amountOfUpdates;
		double delta = 0;
		@SuppressWarnings("unused")
		int frames = 0;
		double timer = System.currentTimeMillis();
		requestFocus();
		while (isRunning) {
			long now = System.nanoTime();
			delta += (now - lastTime) / ns;
			lastTime = now;

			if (delta >= 1) {
				update();
				render();
				frames++;
				delta--;
			}

			if (System.currentTimeMillis() - timer >= 1000) {
//				System.out.println("FPS: " + frames);
				frames = 0;
				timer += 1000;
			}

		}

		stop();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	// Aqui só trocamos as variáveis. A lógica fica no UPDATE || Tick
		public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_SPACE) {
			if (OnboardingManager.isActive()) {
				OnboardingManager.skip();
			}
			player.jump = true;
		}
		if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
			if ("MENU".equals(gameState)) {
				// Navegação horizontal do menu por A/D e seta direita (rodada 15):
				// evita mover o personagem pelos itens do menu.
				menu.right = true;
			} else {
				player.right = true;
			}
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
			if ("MENU".equals(gameState)) {
				menu.left = true;
			} else {
				player.left = true;
			}
		}

		if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
			if (showInitialWeaponSelect) {
				initialWeaponSelection = Math.max(0, initialWeaponSelection - 1);
				return;
			}
			player.up = true;

			if ("MENU".equals(gameState)) {
				menu.up = true;
			} else if ("SHOP".equals(gameState)) {
				ShopManager.navigateUp();
			} else if ("LEVELUP".equals(gameState)) {
				LevelUpManager.navigateUp();
			} else if ("LEVELSELECT".equals(gameState)) {
				LevelSelectScreen.navigateUp();
			}
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
			if (showInitialWeaponSelect) {
				initialWeaponSelection = Math.min(getUnlockedInitialWeapons().length - 1,
						initialWeaponSelection + 1);
				return;
			}
			player.down = true;

			if ("MENU".equals(gameState)) {
				menu.down = true;
			} else if ("SHOP".equals(gameState)) {
				ShopManager.navigateDown();
			} else if ("LEVELUP".equals(gameState)) {
				LevelUpManager.navigateDown();
			} else if ("LEVELSELECT".equals(gameState)) {
				LevelSelectScreen.navigateDown();
			}
		}

                if (e.getKeyCode() == KeyEvent.VK_X) {
                        player.shoot = true;
                        // Registro por evento de pressão: o flag é consumido no
                        // mesmo frame pelo Player.update(), então o onboarding
                        // conta tiros aqui (não via flag no update).
                        OnboardingManager.notifyShotFired();
                }

                if (e.getKeyCode() == KeyEvent.VK_Q) {
                        player.cycleWeapon(false);
                } else if (e.getKeyCode() == KeyEvent.VK_E) {
                        player.cycleWeapon(true);
                }

                if (e.getKeyCode() == KeyEvent.VK_1 || e.getKeyCode() == KeyEvent.VK_NUMPAD1) {
                        player.selectWeaponSlot(1);
                } else if (e.getKeyCode() == KeyEvent.VK_2 || e.getKeyCode() == KeyEvent.VK_NUMPAD2) {
                        player.selectWeaponSlot(2);
                } else if (e.getKeyCode() == KeyEvent.VK_3 || e.getKeyCode() == KeyEvent.VK_NUMPAD3) {
                        player.selectWeaponSlot(3);
                } else if (e.getKeyCode() == KeyEvent.VK_4 || e.getKeyCode() == KeyEvent.VK_NUMPAD4) {
                        player.selectWeaponSlot(4);
                } else if (e.getKeyCode() == KeyEvent.VK_5 || e.getKeyCode() == KeyEvent.VK_NUMPAD5) {
                        player.selectWeaponSlot(5);
                } else if (e.getKeyCode() == KeyEvent.VK_6 || e.getKeyCode() == KeyEvent.VK_NUMPAD6) {
                        player.selectWeaponSlot(6);
                }

		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			if (showInitialWeaponSelect) {
				getInstance().applyInitialWeaponSelection();
				return;
			}
			this.restartGame = true;
			if ("MENU".equals(gameState)) {
				menu.enter = true;
			} else if ("SHOP".equals(gameState)) {
				// Enter confirma/compra: após uma compra bem-sucedida, o Enter
				// seguinte fecha a loja (rodada de QA — compras múltiplas).
				ShopManager.confirmOrPurchase();
			} else if ("LEVELUP".equals(gameState)) {
				LevelUpManager.confirmChoice();
			} else if ("LEVELSELECT".equals(gameState)) {
				LevelSelectScreen.confirmSelection();
			}
		}

		// Navegação da loja por A/D (rodada de QA): A sobe, D desce na lista de itens.
		if ("SHOP".equals(gameState)) {
			if (e.getKeyCode() == KeyEvent.VK_A) {
				ShopManager.navigateA();
			} else if (e.getKeyCode() == KeyEvent.VK_D) {
				ShopManager.navigateD();
			}
		}

		// Seleção direta de upgrade por tecla 1/2/3 na tela de level up (rodada 15):
		// escolhe o card correspondente sem precisar navegar com as setas.
		if ("LEVELUP".equals(gameState)) {
			if (e.getKeyCode() == KeyEvent.VK_1 || e.getKeyCode() == KeyEvent.VK_NUMPAD1) {
				LevelUpManager.selectAndConfirm(0);
			} else if (e.getKeyCode() == KeyEvent.VK_2 || e.getKeyCode() == KeyEvent.VK_NUMPAD2) {
				LevelUpManager.selectAndConfirm(1);
			} else if (e.getKeyCode() == KeyEvent.VK_3 || e.getKeyCode() == KeyEvent.VK_NUMPAD3) {
				LevelUpManager.selectAndConfirm(2);
			}
		}

		if (e.getKeyCode() == KeyEvent.VK_ESCAPE && VictoryCutscene.isShowing()) {
			this.escape = true;
		}

		if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			// Key-repeat do ESC ignorado logo após fechar a loja (evita o "brilho"
			// do menu de pausa ao segurar a tecla).
			if (ShopManager.isEscOnCooldown()) {
				return;
			}
			if ("MENU".equals(gameState)) {
				// ESC nas telas do menu fecha a tela atual voltando ao nível
				// anterior (pausa, opções, carregar, confirmação de saída).
				menu.escape = true;
				return;
			}
			// ESC na tela de escolha de arma inicial: cancela e volta ao menu
			// principal. Não usar Menu.closePauseScreen() aqui: ele define
			// gameState="NORMAL" e despausaria a fase — o returnToMainMenu já
			// zera a pausa e coloca o jogo no MENU corretamente.
			if (showInitialWeaponSelect) {
				showInitialWeaponSelect = false;
				returnToMainMenu();
				return;
			}
			if ("NORMAL".equals(gameState)) {
				Menu.openPauseScreen();
			} else if ("MENU".equals(gameState) && Menu.pause) {
				// ESC na tela de pausa: voltar ao jogo (fecha a pausa)
				Menu.closePauseScreen();
				gameState = "NORMAL";
			} else if ("SHOP".equals(gameState)) {
				if (ShopManager.isOpen()) {
					ShopManager.close();
				}
			} else if ("LEVELUP".equals(gameState)) {
				LevelUpManager.dismiss();
			} else if ("LEVELSELECT".equals(gameState)) {
				LevelSelectScreen.close();
			} else if ("GAMEOVER".equals(gameState)) {
				returnToMainMenu();
			} else {
				returnToMainMenu();
			}
		}
		if (e.getKeyCode() == KeyEvent.VK_T) {
                        if ("NORMAL".equals(gameState)) {
                                Game.saveGame = true;
                                levelPlus=0;
                        }
                }

                if (e.getKeyCode() == KeyEvent.VK_P) {
                        if ("NORMAL".equals(gameState)) {
                                Menu.openPauseScreen();
                        }
                }

		if (e.getKeyCode() == KeyEvent.VK_L) {
			if ("NORMAL".equals(gameState)) {
				LevelSelectScreen.open();
			}
		}

		if (e.getKeyCode() == KeyEvent.VK_F11) {
			toggleFullscreen();
		}

		if (e.getKeyCode() == KeyEvent.VK_F) {
                        if ("NORMAL".equals(gameState)) {
                                UltimateAbility.cast();
                        }
                }

                if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                        if ("NORMAL".equals(gameState)) {
                                DashAbility.perform();
                        }
                }

                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                        if (LevelSelectScreen.isOpen()) {
                                // TAB fecha a seleção de fases (mesmo atalho do
                                // painel tático, sem abrir o painel por cima).
                                LevelSelectScreen.closeOnTab();
                        } else if ("NORMAL".equals(gameState)) {
                                Game.toggleOverlayExpanded();
                        }
                }

                if (e.getKeyCode() == KeyEvent.VK_R) {
                        if (DialogueManager.isActive()) {
                                DialogueManager.advance();
                        } else if ("NORMAL".equals(gameState)) {
                                DialogueManager.startNearestDialogue();
                        }
                }

		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				if (DialogueManager.isActive()) {
					DialogueManager.advance();
				} else if (VictoryCutscene.isShowing()) {
					this.enter = true;
				}
			}

                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        if (DialogueManager.isActive()) {
                                DialogueManager.advance();
                        }
                }
        }


	@Override
			public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
			// execute tal ação!
			player.right = false;
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
			player.left = false;
		}

		if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
			player.up = false;
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
			player.down = false;
		}

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		player.mouseShoot = true;
		// A posição do clique é convertida do espaço da janela para o espaço do
		// buffer do jogo, desconsiderando o deslocamento do letterboxing
		// (drawOffsetX/Y — jogo centralizado em fullscreen ou redimensionamento)
		// e o SCALE atual da renderização.
		player.mx = Math.max(0, Math.min(WIDTH - 1, (e.getX() - drawOffsetX) / SCALE));
		player.my = Math.max(0, Math.min(HEIGHT - 1, (e.getY() - drawOffsetY) / SCALE));

		if ("GAMEOVER".equals(gameState)) {
			this.restartGame = true;
		} else if ("MENU".equals(gameState)) {
			menu.enter = true;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
        public void mouseExited(MouseEvent e) {
                // TODO Auto-generated method stub

        }

        private void handleGameOverRestart() {
                resetGameOverState();
                this.restartGame = false;
                if (!loadGameFromSave()) {
                        startNewGame();
                }
        }

                private boolean loadGameFromSave() {
                File file = new File("save.txt");
                if (!file.exists()) {
                        return false;
                }
                // Primeiro tenta o autosave do SaveManager; depois a codificação
                // manual antiga (legado) como último recurso.
                if (SaveManager.hasAnySave()) {
                        return SaveManager.loadSlot(SaveManager.activeSlot);
                }
                String saver = Menu.loadGame(20);
                if (saver == null || saver.isEmpty()) {
                        return false;
                }
                try {
                        Menu.applySave(saver);
                        return true;
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return false;
        }

	public void startNewGame() {
		resetGameOverState();
		// Novo jogo: remove o companion ativo (persistência apenas por save).
		com.traduvertgames.entities.Companion.clear();
		this.levelPlus = 0;
		CUR_LEVEL = 1;
		questCompletedPending = false;
		shopPendingOpened = false;
		showLevelTransition = 0;
		transitionAlpha = 0;
		Enemy.enemies = 0;
			Menu.pause = false;
		resetPlayerToDefaults();
		applyDifficultyToPlayerStats();
		resetScoreState();
		resetTraitorTalked();
			World.restartGame("level1.png");
			LevelUpManager.reset();
			WaveManager.reset();
			DashAbility.reset();
			UltimateAbility.reset();
			LootGuarantee.reset();
		ParticleSystem.clear();
			FloatingText.clear();
		// Antes do onboarding, o jogador escolhe sua arma inicial entre as
		// desbloqueadas — a escolha fica registrada no arsenal persistente.
		startInitialWeaponSelect();
	}

	/**
	 * Recompensa de arma concedida ao concluir fases da campanha (7 e 8).
	 * Fase 7: Morteiro do Vazio. Fase 8 (fim da campanha): Drone Sentinela.
	 */
	private static void grantCampaignReward() {
		WeaponType reward = CUR_LEVEL == 7 ? WeaponType.VOID_MORTAR : WeaponType.DRONE_SENTINEL;
		if (Game.player != null && !Game.player.hasWeaponUnlocked(reward)) {
			Game.player.unlockWeapon(reward);
			FloatingText.show("NOVA ARMA: " + reward.getDisplayName().toUpperCase(),
					Game.WIDTH * Game.SCALE / 2, Game.SCALE * 40, new java.awt.Color(255, 214, 10), 240);
			com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.LEVELUP);
		}
	}

	/** Abre a tela de escolha de arma inicial (pausa o jogo no estado NORMAL). */
	private static void startInitialWeaponSelect() {
		showInitialWeaponSelect = true;
		initialWeaponSelection = 0;
		gameState = "MENU";
		Menu.pause = true;
	}

	private static final WeaponType[] INITIAL_WEAPON_CATALOG = new WeaponType[] {
		WeaponType.BLASTER, WeaponType.ION_RIFLE, WeaponType.SCATTER_CANNON,
		WeaponType.FUSION_LANCE, WeaponType.ARC_DISRUPTOR, WeaponType.SOLAR_CANNON,
		WeaponType.PLASMA_CUTTER, WeaponType.VOID_MORTAR, WeaponType.BOOMERANG_ARCANO,
		WeaponType.CHAIN_ARC, WeaponType.DRONE_SENTINEL
	};

	/** Armas desbloqueadas que aparecem na tela de escolha de arma inicial. */
	private static WeaponType[] getUnlockedInitialWeapons() {
		java.util.List<WeaponType> unlocked = new java.util.ArrayList<WeaponType>();
		for (WeaponType type : INITIAL_WEAPON_CATALOG) {
			if (com.traduvertgames.entities.Player.isWeaponUnlocked(type)) {
				unlocked.add(type);
			}
		}
		return unlocked.toArray(new WeaponType[0]);
	}

	/** Aplica a arma inicial selecionada e entra na arena de treino. */
	private void applyInitialWeaponSelection() {
		WeaponType[] catalog = getUnlockedInitialWeapons();
		if (catalog.length > 0 && initialWeaponSelection >= 0
				&& initialWeaponSelection < catalog.length) {
			Player.setPersistentCurrentWeapon(catalog[initialWeaponSelection]);
		}
		showInitialWeaponSelect = false;
		Menu.pause = false;
		// O onboarding roda em uma arena de treino separada (sem itens de
		// missão nem inimigos); ao concluir, loadFirstPhase() carrega a fase 1 real.
		World.restartGame("training.png");
		// O tutorial pede para atirar — sem munição o jogador ficava travado
		// (resetPlayerToDefaults zera weapon/mana). A arena de treino começa
		// com mana e munição cheias; isso não afeta o save nem a fase 1 real.
		Player.mana = Player.maxMana;
		Player.weapon = Player.maxWeapon;
		if (player != null) {
			player.setCurrentWeaponEnergy(Player.weapon);
		}
		OnboardingManager.start();
		gameState = "NORMAL";
	}

	private static boolean showInitialWeaponSelect = false;
	private static int initialWeaponSelection = 0;

	/** Desenha a tela de escolha de arma inicial sobre o buffer do jogo. */
	private static void renderInitialWeaponSelect(Graphics g, int width, int height) {
		if (!showInitialWeaponSelect) {
			return;
		}
		g.setColor(new Color(0, 0, 0, 200));
		g.fillRect(0, 0, width, height);
		WeaponType[] catalog = getUnlockedInitialWeapons();
		java.awt.Font titleFont = new java.awt.Font("arial", java.awt.Font.BOLD, 20);
		java.awt.Font optionFont = new java.awt.Font("arial", java.awt.Font.BOLD, 14);
		g.setFont(titleFont);
		g.setColor(new Color(255, 214, 0));
		String title = "Escolha sua arma inicial";
		g.drawString(title, (width - g.getFontMetrics().stringWidth(title)) / 2, height / 2 - 60);
		g.setFont(optionFont);
		int totalHeight = catalog.length * 22;
		int startY = (height - totalHeight) / 2;
		for (int i = 0; i < catalog.length; i++) {
			WeaponType type = catalog[i];
			g.setColor(i == initialWeaponSelection ? java.awt.Color.yellow : java.awt.Color.white);
			String label = (i == initialWeaponSelection ? "> " : "  ") + type.getDisplayName();
			g.drawString(label, (width - g.getFontMetrics().stringWidth(label)) / 2, startY + i * 22);
		}
		g.setColor(new Color(170, 170, 170));
		String hint = "Up/Down para navegar — Enter para confirmar — Esc para voltar ao menu";
		g.setFont(new java.awt.Font("arial", java.awt.Font.PLAIN, 11));
		g.drawString(hint, (width - g.getFontMetrics().stringWidth(hint)) / 2, startY + totalHeight + 20);
	}

	/**
	 * Carrega a fase 1 real após o onboarding na arena de treino: o mapa da
	 * fase é recarregado com todos os itens de missão, o chefe da fase e a
	 * quest intactos — o treino nunca consome progresso da campanha.
	 */
		public void loadFirstPhase() {
		CUR_LEVEL = 1;
		questCompletedPending = false;
		shopPendingOpened = false;
		showLevelTransition = 0;
		transitionAlpha = 0;
		Enemy.enemies = 0;
		resetScoreState();
		applyDifficultyToPlayerStats();
		clampPlayerResources();
		LevelUpManager.reset();
		WaveManager.reset();
		DashAbility.reset();
		UltimateAbility.reset();
		LootGuarantee.reset();
		ParticleSystem.clear();
		FloatingText.clear();
		if (player != null) {
			player.syncFromPersistentState();
			player.updateCamera();
		}
		// A QuestManager já recebe a quest da fase 1 dentro do restartGame.
		World.restartGame("level1.png");
		// Lore de abertura da campanha: mesmo banner dourado das demais fases.
		com.traduvertgames.graficos.MissionBanner.reset();
		com.traduvertgames.graficos.MissionBanner.show(
				com.traduvertgames.quest.StoryManager.getPhaseLoreTitle(1),
				com.traduvertgames.quest.StoryManager.getPhaseLore(1),
				new java.awt.Color(255, 235, 59), java.awt.Color.WHITE, 360);
		gameState = "NORMAL";
	}

	private void resetPlayerToDefaults() {
		Player.resetPersistentArsenal();
		Player.resetBaseStats();
	}

        private void resetScoreState() {
                score = 0;
                comboMultiplier = 1;
                comboTimer = 0;
                bestComboThisRun = 1;
        }

	/** Avança para a próxima fase (loja encerra e o mapa muda). */
	public static void advanceToNextLevel() {
		CUR_LEVEL++;
		if (CUR_LEVEL > MAX_LEVEL) {
			// Pós-campanha: mantém a fase 8 (Núcleo Central) e entra no
			// modo sobrevivência com ondas infinitas e dificuldade crescente.
			CUR_LEVEL = MAX_LEVEL;
			instance.levelPlus += 1;
			enterSurvivalMode();
			// O progresso de fase encerra a loja aberta (ou level up) para seguir.
			if (ShopManager.isOpen()) {
				ShopManager.close();
			}
			if (LevelUpManager.isShowingLevelUp()) {
				LevelUpManager.dismiss();
			}
			applyProgressBonuses();
			showLevelTransition = 180;
		transitionAlpha = 150;
			return;
		}
		// O progresso de fase encerra a loja aberta (ou level up) para seguir.
		if (ShopManager.isOpen()) {
			ShopManager.close();
		}
		if (LevelUpManager.isShowingLevelUp()) {
			LevelUpManager.dismiss();
		}
		applyProgressBonuses();
		// Remove os projéteis inimigos em voo antes de carregar a nova fase
		// (evita projéteis perdidos na transição).
		bullets.clear();
		questCompletedPending = false;
		shopPendingOpened = false;
		QuestManager.prepareForLevel(CUR_LEVEL);
		String newWorld = "level" + CUR_LEVEL + ".png";
		World.restartGame(newWorld);
		// Card de estatísticas da fase que acabou de terminar (kills, tempo, combo).
		com.traduvertgames.graficos.PhaseStatsScreen.show();
		// Transição de fase limpa: fade preto suave para "apagar" a fase
		// antiga antes de revelar a nova, com HUDs escondidas no meio tempo.
		transitionAlpha = 150;
		showLevelTransition = 150;
		// Lore da nova fase: título e texto de ambientação em destaque dourado.
		com.traduvertgames.graficos.MissionBanner.reset();
		com.traduvertgames.graficos.MissionBanner.show(
			com.traduvertgames.quest.StoryManager.getPhaseLoreTitle(CUR_LEVEL),
			com.traduvertgames.quest.StoryManager.getPhaseLore(CUR_LEVEL),
			new Color(255, 235, 59), Color.WHITE, 360);
	}

	/**
	 * Ativa o modo sobrevivência: ondas infinitas na Torre do Supervisor,
	 * com a profundidade do modo (levelPlus) escalando a dificuldade.
	 */
	public static void enterSurvivalMode() {
		QuestManager.prepareForLevel(MAX_LEVEL + 1);
		WaveManager.startArena();
		showLevelTransition = 180;
		transitionAlpha = 150;
		// Card de estatísticas do ciclo anterior do modo infinito (se não for a
		// primeira entrada, que já ganha a cutscene de vitória da campanha).
		if (instance != null && instance.levelPlus > 1) {
			com.traduvertgames.graficos.PhaseStatsScreen.show();
		}
	}

	/**
	 * Entra no modo infinito pela primeira vez (a partir do seletor de fases).
	 * Gera o primeiro mapa procedural, entra no modo arena e exibe o banner de
	 * transição do modo.
	 */
	public static void enterInfiniteMode() {
		if (instance != null) {
			instance.levelPlus = 1;
		}
		QuestManager.prepareForLevel(MAX_LEVEL + 1);
		startProceduralLevel(1);
		showLevelTransition = 180;
		transitionAlpha = 150;
	}

	/**
	 * Avança para a próxima fase procedural do modo infinito: novo mapa gerado
	 * pela profundidade (semente determinística), arena reiniciada (mantendo o
	 * recorde de ondas) e bônus de recursos do piloto aplicados.
	 * Chamado ao derrotar um chefe enquanto o modo arena estiver ativo.
	 */
	public static void advanceProceduralPhase() {
		if (instance == null) {
			return;
		}
		instance.levelPlus += 1;
		int depth = instance.levelPlus;
		QuestManager.prepareForLevel(MAX_LEVEL + 1);
		applyProgressBonuses();
		// Card de estatísticas do ciclo que acabou de terminar.
		com.traduvertgames.graficos.PhaseStatsScreen.show();
		startProceduralLevel(depth);
		showLevelTransition = 180;
		transitionAlpha = 150;
	}

	/** Carrega o mapa procedural da profundidade informada. */
	private static void startProceduralLevel(int depth) {
		try {
			java.io.File mapFile = com.traduvertgames.world.ProceduralLevelGenerator.generate(depth);
			String absPath = mapFile.getAbsolutePath();
			com.traduvertgames.world.World.restartGameFromFile(absPath);
		} catch (Exception error) {
			error.printStackTrace();
			// Fallback: mapa fixo do Núcleo Central em caso de falha de geração.
			World.restartGame("level8.png");
		}
	}

	private static void applyProgressBonuses() {
                applyDifficultyScalingForCurrentLevel();
                if (instance != null && instance.levelPlus >= 1) {
                        Player.mana = Player.maxMana;
                        Player.life = Player.maxLife;
                        Player.shield = Player.maxShield;
                        if (player != null) {
                                player.refillCurrentWeapon();
                        } else {
                                Player.weapon = Player.maxWeapon;
                        }
                }
        }

        public void applyDifficultyToPlayerStats() {
                applyProgressBonuses();
                clampPlayerResources();
        }

                private static void applyDifficultyScalingForCurrentLevel() {
                int baseMaxLife;
                int baseMaxMana;
                int baseMaxShield;
                double baseCapacityMultiplier;
		if (CUR_LEVEL > MAX_LEVEL) {
			// Fase final: a cada ciclo de sobrevivência (levelPlus),
			// os recursos máximos do piloto crescem para compensar
			// as ondas cada vez mais agressivas.
			int survivalDepth = Math.max(0, instance != null ? instance.levelPlus - 1 : 0);
			baseMaxLife = 1000 + 200 * survivalDepth;
			baseMaxMana = 1500 + 300 * survivalDepth;
			baseMaxShield = 600 + 100 * survivalDepth;
			baseCapacityMultiplier = 4.0;
				} else {
			// Rodada 15: a vida base do piloto cresce suavemente com a fase
			// (120 na fase 1 até 176 na fase 8) em vez de ficar travada em
			// 100 — o piloto precisa de folga para aprender as fases iniciais.
			baseMaxLife = 120 + (CUR_LEVEL - 1) * 8;
			baseMaxMana = 500;
			baseMaxShield = 150;
			baseCapacityMultiplier = 1.0;
		}
		// Agressividade e recursos do piloto crescem no arco final da campanha
		// (fases 7 e 8), refletindo o esforço da colônia para deter a IA.
		if (CUR_LEVEL >= 7 && CUR_LEVEL <= MAX_LEVEL) {
			int finalStretch = CUR_LEVEL - 6;
			baseMaxLife += 10 * finalStretch;          // piloto mais resiliente
			baseMaxMana += 50 * finalStretch;
			baseMaxShield += 15 * finalStretch;
			baseCapacityMultiplier += 0.25 * finalStretch;
		}
                applyDifficultyScaling(baseMaxLife, baseMaxMana, baseMaxShield, baseCapacityMultiplier);
        }

        private static void applyDifficultyScaling(int baseMaxLife, int baseMaxMana, int baseMaxShield,
                        double baseCapacityMultiplier) {
                int scaledMaxLife = (int) Math.round(baseMaxLife * OptionsConfig.getLifeMultiplier());
                int scaledMaxMana = (int) Math.round(baseMaxMana * OptionsConfig.getManaMultiplier());
                int scaledMaxShield = (int) Math.round(baseMaxShield * OptionsConfig.getLifeMultiplier());
                double scaledCapacity = baseCapacityMultiplier * OptionsConfig.getWeaponCapacityMultiplier();

                Player.maxLife = Math.max(1, scaledMaxLife);
                if (Player.life > Player.maxLife) {
                        Player.life = Player.maxLife;
                }

                Player.maxMana = Math.max(0, scaledMaxMana);
                if (Player.mana > Player.maxMana) {
                        Player.mana = Player.maxMana;
                }

                Player.maxShield = Math.max(0, scaledMaxShield);
                if (Player.shield > Player.maxShield) {
                        Player.shield = Player.maxShield;
                }

                Player.setWeaponCapacityMultiplier(Math.max(0.5, scaledCapacity));
                if (Player.weapon > Player.maxWeapon) {
                        Player.weapon = Player.maxWeapon;
                }
        }

        public void clampPlayerResources() {
                if (Player.life <= 0) {
                        Player.life = Player.maxLife;
                } else if (Player.life > Player.maxLife) {
                        Player.life = Player.maxLife;
                }

                if (Player.mana < 0) {
                        Player.mana = 0;
                } else if (Player.mana > Player.maxMana) {
                        Player.mana = Player.maxMana;
                }

                if (Player.weapon < 0) {
                        Player.weapon = 0;
                } else if (Player.weapon > Player.maxWeapon) {
                        Player.weapon = Player.maxWeapon;
                }
                if (Player.shield < 0) {
                        Player.shield = 0;
                } else if (Player.shield > Player.maxShield) {
                        Player.shield = Player.maxShield;
                }
                if (player != null) {
                        player.setCurrentWeaponEnergy(Player.weapon);
                }
        }

        public void applyPostLoadAdjustments() {
                resetGameOverState();
                Menu.pause = false;
                applyProgressBonuses();
                clampPlayerResources();
                normalizeScoreAfterLoad();
                if (player != null) {
                        player.syncFromPersistentState();
                }
                gameState = "NORMAL";
        }

	public void resetGameOverState() {
		this.framesGameOver = 0;
		this.showMessageGameOver = true;
		this.menuReturnTimer = 300;
	}

	/** Volta ao menu principal mantendo o autosave do progresso da partida. */
	public void returnToMainMenu() {
		gameState = "MENU";
		Menu.pause = false;
		Menu.closePauseScreen();
		DialogueManager.stop();
		MissionBanner.reset();
		VictoryCutscene.stop();
		damageOverlayFrames = 0;
		showInitialWeaponSelect = false;
		if (this.menu != null) {
			this.menu.resetToMain();
		}
		questCompletedPending = false;
		shopPendingOpened = false;
		showLevelTransition = 0;
		transitionAlpha = 0;
		resetGameOverState();
	}

        private void normalizeScoreAfterLoad() {
                if (score < 0) {
                        score = 0;
                }
                if (highScore < score) {
                        highScore = score;
                }
                if (bestComboThisRun < 1) {
                        bestComboThisRun = 1;
                }
                if (bestComboRecord < bestComboThisRun) {
                        bestComboRecord = bestComboThisRun;
                }
                comboMultiplier = 1;
                comboTimer = 0;
        }
}
