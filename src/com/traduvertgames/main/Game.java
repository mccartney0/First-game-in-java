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
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.graficos.UI;
import com.traduvertgames.world.World;
import com.traduvertgames.quest.QuestManager;

public class Game extends Canvas implements Runnable, KeyListener, MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static JFrame frame;
	private Thread thread;
	private boolean isRunning = true;
        public static final int WIDTH = 320;
        public static final int HEIGHT = 192;
        public static final int SCALE = 3;

        private static Game instance;

        private int CUR_LEVEL = 1, MAX_LEVEL = 4;
	private BufferedImage image;

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

        public Game() throws IOException {
                instance = this;
                rand = new Random();
		addKeyListener(this);
		addMouseListener(this);
                setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
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

        public static int getBestComboRecord() {
                return bestComboRecord;
        }

        public static void setBestComboRecord(int value) {
                bestComboRecord = Math.max(1, value);
        }

        public static int getBestComboThisRun() {
                return bestComboThisRun;
        }

        public static void setBestComboThisRun(int value) {
                bestComboThisRun = Math.max(1, value);
        }

        public void setCurrentLevel(int level) {
                if (level < 1)
                        level = 1;
                if (level > MAX_LEVEL)
                        level = MAX_LEVEL;
                this.CUR_LEVEL = level;
        }

        public int getCurrentLevel() {
                return this.CUR_LEVEL;
        }

        public void setLevelPlus(int value) {
                if (value < 0)
                        value = 0;
                this.levelPlus = value;
        }

        public int getLevelPlus() {
                return this.levelPlus;
        }

        public static void registerEnemyKill() {
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
        }

        public static double getDamageTakenMultiplier() {
                return OptionsConfig.getDamageTakenMultiplier();
        }

        public void initFrame() {
                frame = new JFrame("Game 2 RPG");
                frame.add(this);
                frame.setResizable(false);
                frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
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
		
                if (gameState == "NORMAL") {
// Salvar o Level
                        if (Game.saveGame) {
                                Game.saveGame = false;
                                java.util.List<String> keys = new java.util.ArrayList<String>();
                                java.util.List<Integer> values = new java.util.ArrayList<Integer>();
                                keys.add("vida");
                                values.add((int) Player.life);
                                keys.add("mana");
                                values.add((int) Player.mana);
                                keys.add("arma");
                                values.add((int) Player.weapon);
                                keys.add("escudo");
                                values.add((int) Player.shield);
                                keys.add("inimigosMortos");
                                values.add(Enemy.enemies);
                                keys.add("levelPlus");
                                values.add(levelPlus);
                                keys.add("level");
                                values.add(this.CUR_LEVEL);
                                keys.add("pontuacao");
                                values.add(Game.getScore());
                                keys.add("recorde");
                                values.add(Game.getHighScore());
                                keys.add("melhorCombo");
                                values.add(Game.getBestComboRecord());
                                keys.add("melhorComboSessao");
                                values.add(Game.getBestComboThisRun());
                                keys.add("armaAtual");
                                values.add(Player.getCurrentWeaponOrdinal());
                                keys.add("armasDesbloqueadas");
                                values.add(Player.getWeaponUnlockMask());
                                for (WeaponType type : WeaponType.values()) {
                                        keys.add("energiaArma_" + type.name());
                                        values.add((int) Math.round(Player.getStoredEnergyForType(type)));
                                }
                                String[] opt1 = keys.toArray(new String[0]);
                                int[] opt2 = new int[values.size()];
                                for (int i = 0; i < values.size(); i++) {
                                        opt2[i] = values.get(i);
                                }
                                Menu.saveGame(opt1, opt2, 20);
                                System.out.println("Jogo salvo!");
                        }

                        this.restartGame = false; // Prevenção
                        updateComboTimer();
                        for (int i = 0; i < entities.size(); i++) {
                                Entity e = entities.get(i);
                                e.update();
                        }

                        for (int i = 0; i < bullets.size(); i++) {
                                bullets.get(i).update();
                        }
                        for (int i = 0; i < bullet.size(); i++) {
                                bullet.get(i).update();
                        }

                        QuestManager.update();

                        if (QuestManager.isObjectiveComplete()) {
                                advanceToNextLevel();
                        }
                } else if (gameState == "GAMEOVER") {
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
		}else if (gameState == "MENU") {
			//Menu
			//Iniciando a camera junto com o jogador
			player.updateCamera();
			menu.update();
		}
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
		for (int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			e.render(g);
		}
		for (int i = 0; i < bullets.size(); i++) {
			bullets.get(i).render(g);
		}
		for (int i = 0; i < bullet.size(); i++) {
			bullet.get(i).render(g);
		}
		ui.render(g);
		g.dispose();

                g = bs.getDrawGraphics();
                int scaledWidth = WIDTH * SCALE;
                int scaledHeight = HEIGHT * SCALE;
                g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
                ui.renderOverlay((Graphics2D) g);

                if (gameState == "GAMEOVER") {
                        Graphics2D g2 = (Graphics2D) g;
                        g2.setColor(new Color(0, 0, 0, 120));
                        g2.fillRect(0, 0, scaledWidth, scaledHeight);
                        g.setFont(new Font("arial", Font.BOLD, 36));
                        g.setColor(Color.white);
                        drawCenteredString(g, "Game Over", scaledHeight / 2 - 50);
                        g.setFont(new Font("arial", Font.BOLD, 28));

                        if (showMessageGameOver) {
                                drawCenteredString(g, ">Pressione Enter para reiniciar<", scaledHeight / 2 + 4);
                        }

                        g.setFont(new Font("arial", Font.BOLD, 24));
                        drawCenteredString(g, "Pontuação final: " + Game.getScore(), scaledHeight / 2 + 52);
                        drawCenteredString(g, "Recorde: " + Game.getHighScore(), scaledHeight / 2 + 82);
                        drawCenteredString(g, "Melhor combo da partida: x" + Game.getBestComboThisRun(), scaledHeight / 2 + 112);

                } else if (gameState == "MENU") {
                        menu.render(g);
                }
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
			player.jump = true;
		}
		if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
			// execute tal ação!
			player.right = true;
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
			player.left = true;
		}

		if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
			player.up = true;

			if (gameState == "MENU") {
				menu.up = true;
			}
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
			player.down = true;

			if (gameState == "MENU") {
				menu.down = true;
			}
		}

                if (e.getKeyCode() == KeyEvent.VK_X) {
                        player.shoot = true;
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
                }

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        this.restartGame = true;
                        if(gameState == "MENU") {
                                menu.enter = true;
			}
		}
		
		if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			gameState = "MENU";
			Menu.pause = true;
		}
		if (e.getKeyCode() == KeyEvent.VK_T) {
			if(gameState == "NORMAL") {
				Game.saveGame = true;
				levelPlus=0;
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
		player.mx = (e.getX() / SCALE);
		player.my = (e.getY() / SCALE);
		
		
			this.restartGame = true;
			if(gameState == "MENU") {
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

                String saver = Menu.loadGame(20);
                if (saver == null || saver.isEmpty()) {
                        return false;
                }

                try {
                        Menu.applySave(saver);
                        return true;
                } catch (IOException e) {
                        e.printStackTrace();
                }
                return false;
        }

        public void startNewGame() {
                resetGameOverState();
                this.levelPlus = 0;
                this.CUR_LEVEL = 1;
                Enemy.enemies = 0;
                Menu.pause = false;
                resetPlayerToDefaults();
                applyDifficultyToPlayerStats();
                resetScoreState();
                World.restartGame("level1.png");
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

        private void advanceToNextLevel() {
                CUR_LEVEL++;
                if (CUR_LEVEL == MAX_LEVEL) {
                        levelPlus += 1;
                }
                if (CUR_LEVEL > MAX_LEVEL) {
                        CUR_LEVEL = 1;
                }
                applyProgressBonuses();
                String newWorld = "level" + CUR_LEVEL + ".png";
                World.restartGame(newWorld);
        }

        private void applyProgressBonuses() {
                applyDifficultyScalingForCurrentLevel();

                if (this.levelPlus >= 1) {
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

        private void applyDifficultyScalingForCurrentLevel() {
                int baseMaxLife;
                int baseMaxMana;
                int baseMaxShield;
                double baseCapacityMultiplier;

                if (this.CUR_LEVEL == MAX_LEVEL) {
                        baseMaxLife = 1000;
                        baseMaxMana = 1500;
                        baseMaxShield = 600;
                        baseCapacityMultiplier = 4.0;
                } else {
                        baseMaxLife = 100;
                        baseMaxMana = 500;
                        baseMaxShield = 150;
                        baseCapacityMultiplier = 1.0;
                }

                applyDifficultyScaling(baseMaxLife, baseMaxMana, baseMaxShield, baseCapacityMultiplier);
        }

        private void applyDifficultyScaling(int baseMaxLife, int baseMaxMana, int baseMaxShield,
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

        private void clampPlayerResources() {
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

        private void resetGameOverState() {
                this.framesGameOver = 0;
                this.showMessageGameOver = true;
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
