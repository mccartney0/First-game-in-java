package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.World;

public class Menu {

        private enum Screen {
                MAIN,
                OPTIONS
        }

        private static final String[] MAIN_OPTIONS = { "novo jogo", "carregar jogo", "opções", "sair" };
        private static final int OPTION_NEW_GAME = 0;
        private static final int OPTION_LOAD_GAME = 1;
        private static final int OPTION_SETTINGS = 2;
        private static final int OPTION_EXIT = 3;

        private static final int OPTIONS_MENU_COUNT = 3;
        private static final int OPTIONS_INDEX_MUSIC = 0;
        private static final int OPTIONS_INDEX_DIFFICULTY = 1;
        private static final int OPTIONS_INDEX_BACK = 2;

        private static final int ARROW_X = 235;
        private static final int TEXT_X = 275;
        private static final int MAIN_MENU_Y = 234;
        private static final int LINE_HEIGHT = 40;
        private static final int OPTIONS_LINE_HEIGHT = 36;

        private Screen currentScreen = Screen.MAIN;
        private int currentOption = 0;

        public boolean up, down, enter;

	public static boolean pause = false;
	
	public static boolean saveExists = false;
        public static boolean saveGame = false;

        public void update() {
                File file = new File("save.txt");
                saveExists = file.exists();

                if (up) {
                        up = false;
                        moveSelection(-1);
                }
                if (down) {
                        down = false;
                        moveSelection(1);
                }
                if (enter) {
                        enter = false;
                        if (currentScreen == Screen.MAIN) {
                                handleMainMenuSelection(file);
                        } else {
                                handleOptionsSelection();
                        }
                }
        }

        private void moveSelection(int delta) {
                int count = getCurrentOptionCount();
                if (count <= 0) {
                        currentOption = 0;
                        return;
                }
                currentOption = (currentOption + delta) % count;
                if (currentOption < 0) {
                        currentOption += count;
                }
        }

        private int getCurrentOptionCount() {
                return currentScreen == Screen.MAIN ? MAIN_OPTIONS.length : OPTIONS_MENU_COUNT;
        }

        private void handleMainMenuSelection(File saveFile) {
                switch (currentOption) {
                        case OPTION_NEW_GAME:
                                handleNewGameSelection(saveFile);
                                break;
                        case OPTION_LOAD_GAME:
                                if (saveExists) {
                                        String saver = loadGame(20);
                                        try {
                                                applySave(saver);
                                        } catch (IOException e) {
                                                e.printStackTrace();
                                        }
                                }
                                break;
                        case OPTION_SETTINGS:
                                currentScreen = Screen.OPTIONS;
                                currentOption = 0;
                                break;
                        case OPTION_EXIT:
                                int result = JOptionPane.showConfirmDialog(
                                                null,
                                                "Deseja realmente sair?",
                                                "Fechar o jogo",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE);
                                if (result == JOptionPane.YES_OPTION) {
                                        System.exit(0);
                                }
                                break;
                        default:
                                break;
                }
        }

        private void handleNewGameSelection(File saveFile) {
                if (pause) {
                        Game.gameState = "NORMAL";
                        pause = false;
                } else {
                        Game game = Game.getInstance();
                        if (game != null) {
                                game.startNewGame();
                        } else {
                                Game.gameState = "NORMAL";
                                pause = false;
                        }

                        if (saveFile.exists()) {
                                saveFile.delete();
                        }
                }

                if (OptionsConfig.isMusicEnabled()) {
                        OptionsConfig.applyMusicPreference();
                } else if (Sound.music != null) {
                        Sound.music.stop();
                }
        }

        private void handleOptionsSelection() {
                switch (currentOption) {
                        case OPTIONS_INDEX_MUSIC:
                                OptionsConfig.toggleMusic();
                                break;
                        case OPTIONS_INDEX_DIFFICULTY:
                                OptionsConfig.cycleDifficulty();
                                break;
                        case OPTIONS_INDEX_BACK:
                                currentScreen = Screen.MAIN;
                                currentOption = 0;
                                break;
                        default:
                                break;
                }
        }

        public static void applySave(String str) throws IOException {
                if (str == null || str.isEmpty()) {
                        return;
                }

                Game game = Game.getInstance();
                Integer fallbackWeaponEnergy = null;
                boolean hasWeaponEnergyEntries = false;
                String[] spl = str.split("/");
                for (String entry : spl) {
                        if (entry == null || entry.isEmpty()) {
                                continue;
                        }

                        String[] pair = entry.split(":");
                        if (pair.length != 2) {
                                continue;
                        }

                        String key = pair[0];
                        String value = pair[1];

                        switch (key) {
                                case "vida":
                                        Player.life = Integer.parseInt(value);
                                        break;
                                case "mana":
                                        Player.mana = Integer.parseInt(value);
                                        break;
                                case "arma":
                                        fallbackWeaponEnergy = Integer.parseInt(value);
                                        Player.weapon = fallbackWeaponEnergy;
                                        break;
                                case "escudo":
                                        Player.shield = Integer.parseInt(value);
                                        break;
                                case "armaAtual":
                                        Player.loadCurrentWeaponFromSave(Integer.parseInt(value));
                                        break;
                                case "armasDesbloqueadas":
                                        Player.loadUnlockedWeaponsFromSave(Integer.parseInt(value));
                                        break;
                                case "inimigosMortos":
                                        Enemy.enemies = Integer.parseInt(value);
                                        break;
                                case "levelPlus":
                                        if (game != null) {
                                                game.setLevelPlus(Integer.parseInt(value));
                                        }
                                        break;
                                case "level":
                                        if (game != null) {
                                                game.setCurrentLevel(Integer.parseInt(value));
                                                World.restartGame("level" + game.getCurrentLevel() + ".png");
                                        } else {
                                                World.restartGame("level" + value + ".png");
                                                Game.gameState = "NORMAL";
                                                pause = false;
                                        }
                                        break;
                                case "pontuacao":
                                        Game.setScore(Integer.parseInt(value));
                                        break;
                                case "recorde":
                                        Game.setHighScore(Integer.parseInt(value));
                                        break;
                                case "melhorCombo":
                                        Game.setBestComboRecord(Integer.parseInt(value));
                                        break;
                                case "melhorComboSessao":
                                        Game.setBestComboThisRun(Integer.parseInt(value));
                                        break;
                                default:
                                        if (key.startsWith("energiaArma_")) {
                                                String typeKey = key.substring("energiaArma_".length());
                                                WeaponType type = WeaponType.fromSaveKey(typeKey);
                                                if (type != null) {
                                                        Player.loadWeaponEnergyFromSave(type, Integer.parseInt(value));
                                                        hasWeaponEnergyEntries = true;
                                                }
                                        }
                                        break;
                        }
                }

                if (!hasWeaponEnergyEntries && fallbackWeaponEnergy != null) {
                        WeaponType current = WeaponType.fromOrdinal(Player.getCurrentWeaponOrdinal());
                        Player.loadWeaponEnergyFromSave(current, fallbackWeaponEnergy);
                }

                if (game != null) {
                        game.applyPostLoadAdjustments();
                }
        }
	
	public static String loadGame(int encode)
	{
		String line = "";
		File file = new File("save.txt");

		if(file.exists())
		{
			try
			{
				String singleLine = null;
				BufferedReader reader = new BufferedReader(new FileReader("save.txt"));

				try
				{
					while((singleLine = reader.readLine()) != null)
					{
						String[] trans = singleLine.split(":");
						char[] val = trans[1].toCharArray();
						System.out.println(val);
						trans[1] = "";

						for(int i = 0; i < val.length; i++)
						{
							val[i] -= encode;
							trans[1] += val[i];
						}

						line += trans[0];
						line += ":";
						line += trans[1];
						line += "/";
					}
				}
				catch (IOException ignored) {}
			}
			catch (FileNotFoundException ignored) {}

		}

		return line;
	}

	public static void saveGame(String[] val1, int[] val2, int encode) {
		BufferedWriter write = null;
		try {
			write = new BufferedWriter(new FileWriter("save.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < val1.length; i++) {
			String current = val1[i];
			current += ":";
			char[] value = Integer.toString(val2[i]).toCharArray();

			for (int n = 0; n < value.length; n++) {
				value[n] += encode;
				current += value[n];
			}
			try {
				assert write != null;
				write.write(current);
				if (i < val1.length - 1)
					write.newLine();
			} catch (IOException e) {
			}
		}
		try {
			assert write != null;
			write.flush();
			write.close();
		} catch (IOException ignored) {
		}
	}

        public void render(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(0, 0, Game.WIDTH * Game.SCALE, Game.HEIGHT * Game.SCALE);
                g.setColor(Color.yellow);
                g.setFont(new Font("arial", Font.BOLD, 40));
                g.drawString(">Traduvert<", 245, 134);

                if (currentScreen == Screen.MAIN) {
                        renderMainMenu(g);
                } else {
                        renderOptionsMenu(g);
                }
        }

        private void renderMainMenu(Graphics g) {
                g.setFont(new Font("arial", Font.BOLD, 25));
                for (int i = 0; i < MAIN_OPTIONS.length; i++) {
                        int y = MAIN_MENU_Y + (LINE_HEIGHT * i);
                        if (currentOption == i) {
                                g.setColor(Color.white);
                                g.drawString(">", ARROW_X, y);
                        }

                        if (i == OPTION_LOAD_GAME && !saveExists) {
                                g.setColor(Color.LIGHT_GRAY);
                        } else {
                                g.setColor(Color.white);
                        }

                        g.drawString(getMainMenuLabel(i), TEXT_X, y);
                }
        }

        private String getMainMenuLabel(int index) {
                switch (index) {
                        case OPTION_NEW_GAME:
                                return pause ? "Continuar" : "Novo jogo";
                        case OPTION_LOAD_GAME:
                                return saveExists ? "Carregar jogo" : "Carregar jogo (indisponível)";
                        case OPTION_SETTINGS:
                                return "Opções";
                        case OPTION_EXIT:
                                return "Sair";
                        default:
                                return "";
                }
        }

        private void renderOptionsMenu(Graphics g) {
                g.setColor(Color.white);
                g.setFont(new Font("arial", Font.BOLD, 28));
                g.drawString("Opções", TEXT_X, MAIN_MENU_Y - LINE_HEIGHT);

                g.setFont(new Font("arial", Font.PLAIN, 22));
                drawOptionsLine(g, OPTIONS_INDEX_MUSIC,
                                "Música: " + (OptionsConfig.isMusicEnabled() ? "Ligada" : "Desligada"), MAIN_MENU_Y);
                drawOptionsLine(g, OPTIONS_INDEX_DIFFICULTY,
                                "Dificuldade: " + OptionsConfig.getDifficulty().getDisplayName(),
                                MAIN_MENU_Y + OPTIONS_LINE_HEIGHT);
                drawOptionsLine(g, OPTIONS_INDEX_BACK, "Voltar", MAIN_MENU_Y + (2 * OPTIONS_LINE_HEIGHT));
        }

        private void drawOptionsLine(Graphics g, int optionIndex, String text, int y) {
                if (currentOption == optionIndex) {
                        g.setColor(Color.white);
                        g.drawString(">", ARROW_X, y);
                }
                g.setColor(Color.white);
                g.drawString(text, TEXT_X, y);
        }
}
