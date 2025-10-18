package com.traduvertgames.main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.graficos.UI;
import com.traduvertgames.world.World;

public class Game extends Canvas implements Runnable, KeyListener, MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static JFrame frame;
	private Thread thread;
	private boolean isRunning = true;
	public static final int WIDTH = 240;
	public static final int HEIGHT = 160;
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
		world = new World("/level1.png");

		menu = new Menu();
	}

        public static Game getInstance() {
                return instance;
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
                        if(Game.saveGame) {
                                Game.saveGame = false;
                                String[] opt1 = {"vida","mana","arma","inimigosMortos","levelPlus","level"};
                                int[] opt2 = {(int) Player.life,(int) Player.mana,(int) Player.weapon, Enemy.enemies, levelPlus, this.CUR_LEVEL};
                                Menu.saveGame(opt1,opt2,20);
                                System.out.println("Jogo salvo!");
                        }
			
			this.restartGame = false; // Prevenção
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

			if (enemies.size() == 0) {
//Avançar para o próximo nível
				CUR_LEVEL++;
				if(CUR_LEVEL == MAX_LEVEL) {
					levelPlus+=1;
				}
				if (CUR_LEVEL > MAX_LEVEL) {
					CUR_LEVEL = 1;
					
				}
				
				if(CUR_LEVEL == MAX_LEVEL) {
					Player.maxMana = 1500;
					Player.maxLife = 1000;
					Player.life = 500;
					Player.maxWeapon = 1000;
				}else if(levelPlus >= 1 ){
					Player.maxMana = 500;
					Player.maxLife = 100;
					Player.maxWeapon = 250;
					Player.mana = 500;
					Player.life = 100;
					Player.weapon = 250;
				}
				
				String newWorld = "level" + CUR_LEVEL + ".png";
				World.restartGame(newWorld);
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
		g.drawImage(image, 0, 0, WIDTH * SCALE, HEIGHT * SCALE, null);
//Renderizar a String - Full HD
		g.setFont(new Font("arial", Font.BOLD, 20));
		g.setColor(Color.white);
		g.drawString("Vida: ", 30, 32);
		g.drawString((int) Player.life + "/" + (int) Player.maxLife, 158, 32);
		g.drawString("Inimigos: ", 30, 62);
		g.drawString(Game.enemies.size() +"", 118, 63);
		g.drawString("Inimigos mortos: ", 413, 62);
		g.drawString(Enemy.enemies +"", 590, 63);
		g.drawString("Mana: ", 413, 32);
		g.drawString((int) Player.mana + "/" + (int) Player.maxMana, 560, 32);
		if(Player.weapon >0) {
		g.drawString("Arma: ", 22, 465);
		g.drawString((int) Player.weapon + "/" + (int) Player.maxWeapon, 165, 467);
		}
//		
		if (gameState == "GAMEOVER") {
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(new Color(0, 0, 0, 100));
			g2.fillRect(0, 0, WIDTH * SCALE, HEIGHT * SCALE);
			g.setFont(new Font("arial", Font.BOLD, 36));
			g.setColor(Color.white);
			g.drawString("Game Over", 290, 234);
			g.setFont(new Font("arial", Font.BOLD, 32));

			if (showMessageGameOver) {
				g.drawString(">Pressione Enter para reiniciar<", 130, 284);
			}

		} else if (gameState == "MENU") {
			menu.render(g);
		}
		bs.show();
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
                World.restartGame("level1.png");
                gameState = "NORMAL";
        }

        private void resetPlayerToDefaults() {
                Player.maxLife = 100;
                Player.life = Player.maxLife;
                Player.maxMana = 500;
                Player.mana = 0;
                Player.maxWeapon = 250;
                Player.weapon = 0;
        }

        private void applyProgressBonuses() {
                if (this.CUR_LEVEL == MAX_LEVEL) {
                        Player.maxMana = 1500;
                        Player.maxLife = 1000;
                        Player.maxWeapon = 1000;
                } else {
                        Player.maxMana = 500;
                        Player.maxLife = 100;
                        Player.maxWeapon = 250;
                }

                if (this.levelPlus >= 1) {
                        Player.mana = Player.maxMana;
                        Player.life = Player.maxLife;
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
        }

        public void applyPostLoadAdjustments() {
                resetGameOverState();
                Menu.pause = false;
                applyProgressBonuses();
                clampPlayerResources();
                gameState = "NORMAL";
        }

        private void resetGameOverState() {
                this.framesGameOver = 0;
                this.showMessageGameOver = true;
        }
}
