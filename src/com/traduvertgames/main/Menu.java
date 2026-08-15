package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JOptionPane;

import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.World;

public class Menu {

	private enum Screen {
		MAIN,
		PAUSE,
		OPTIONS,
		LOAD,
		HOW_TO_PLAY
	}

	private static final int OPTION_NEW_GAME = 0;
	private static final int OPTION_CONTINUE = 1;
	private static final int OPTION_LOAD_GAME = 2;
	private static final int OPTION_HOW_TO_PLAY = 3;
	private static final int OPTION_SETTINGS = 4;
	private static final int OPTION_EXIT = 5;

	private static final String[] MAIN_OPTIONS = {
			"novo jogo",
			"continuar",
			"carregar jogo",
			"como jogar",
			"opções",
			"sair"
	};

	private static final int PAUSE_CONTINUE = 0;
	private static final int PAUSE_LOAD_GAME = 1;
	private static final int PAUSE_SAVE_GAME = 2;
	private static final int PAUSE_OPTIONS = 3;
	private static final int PAUSE_EXIT = 4;

	private static final String[] PAUSE_OPTIONS_LIST = {
			"continuar",
			"carregar jogo",
			"salvar jogo",
			"opções",
			"sair do jogo"
	};

	private static final String[] LOAD_SLOT_LABELS = { "Slot 1", "Slot 2", "Slot 3" };
	private static final int LOAD_BACK = SaveManager.SLOT_COUNT;

	private static final int OPTIONS_INDEX_MUSIC = 0;
	private static final int OPTIONS_INDEX_DIFFICULTY = 1;
	private static final int OPTIONS_INDEX_BACK = 2;

	private static final String[] OPTIONS_LABELS = {
			"musica",
			"dificuldade",
			"voltar"
	};

	private static final int LINE_HEIGHT = 40;
	private static final int OPTIONS_LINE_HEIGHT = 36;
	private static final int OPTION_Y_START_RATIO = 46;

	private Screen currentScreen = Screen.MAIN;
	private int currentOption = 0;

	public boolean up, down, enter;

	public static boolean pause = false;

	public static boolean saveExists = false;

	/** Renderiza a tela de pausa sobre o jogo (usado quando o jogo continua atrás). */
	public static void renderPauseScreen(Graphics g) {
		// A tela de pausa só aparece quando o jogador pausa de verdade (ESC/P) e
		// escolheu pausar; no estado SHOP o jogo fica em slow-motion sob a loja.
		if (!pause) {
			return;
		}
		Game game = Game.getInstance();
		if (game != null && game.menu != null) {
			game.menu.render(g);
		}
	}

	public void update() {
		saveExists = SaveManager.hasAnySave();

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
			switch (currentScreen) {
			case MAIN:
				handleMainMenuSelection();
				break;
			case PAUSE:
				handlePauseSelection();
				break;
			case OPTIONS:
				handleOptionsSelection();
				break;
			case LOAD:
				handleLoadSelection();
				break;
			case HOW_TO_PLAY:
				currentScreen = Screen.MAIN;
				currentOption = 0;
				break;
			default:
				break;
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
		switch (currentScreen) {
		case MAIN:
			return MAIN_OPTIONS.length;
		case PAUSE:
			return PAUSE_OPTIONS_LIST.length;
		case OPTIONS:
			return OPTIONS_LABELS.length;
		case LOAD:
			return LOAD_SLOT_LABELS.length + 1;
		default:
			return 0;
		}
	}

	/** Inicia a tela de pausa sem sair do jogo. */
	public static void openPauseScreen() {
		currentScreenStatic = Screen.PAUSE;
		pause = true;
		Game.gameState = "MENU";
	}

	/** Volta da tela de pausa para o jogo. */
	public static void closePauseScreen() {
		pause = false;
		Game.gameState = "NORMAL";
	}

	private static Screen currentScreenStatic = Screen.MAIN;

	private void handleMainMenuSelection() {
		switch (currentOption) {
		case OPTION_NEW_GAME:
			if (pause) {
				closePauseScreen();
			} else {
				Game game = Game.getInstance();
				if (game != null) {
					game.startNewGame();
				} else {
					closePauseScreen();
				}
			}
			break;
		case OPTION_CONTINUE:
			if (pause) {
				closePauseScreen();
			} else if (saveExists) {
				// Fora da pausa, "Continuar" retoma o último progresso salvo
				// (autosave) em vez de iniciar um jogo do zero.
				SaveManager.loadSlot(SaveManager.activeSlot);
				pause = false;
			}
			break;
		case OPTION_LOAD_GAME:
			if (saveExists) {
				currentScreen = Screen.LOAD;
				currentOption = 0;
			}
			break;
		case OPTION_HOW_TO_PLAY:
			currentScreen = Screen.HOW_TO_PLAY;
			currentOption = 0;
			break;
		case OPTION_SETTINGS:
			currentScreen = Screen.OPTIONS;
			currentOption = 0;
			break;
		case OPTION_EXIT:
			if (askExit()) {
				System.exit(0);
			}
			break;
		default:
			break;
		}
	}

	private void handlePauseSelection() {
		switch (currentOption) {
		case PAUSE_CONTINUE:
			closePauseScreen();
			break;
		case PAUSE_LOAD_GAME:
			if (saveExists) {
				currentScreen = Screen.LOAD;
				currentOption = 0;
			}
			break;
		case PAUSE_SAVE_GAME:
			if (SaveManager.saveCurrentGame()) {
				System.out.println("Jogo salvo no slot " + SaveManager.activeSlot + "!");
			}
			closePauseScreen();
			break;
		case PAUSE_OPTIONS:
			currentScreen = Screen.OPTIONS;
			currentOption = 0;
			break;
		case PAUSE_EXIT:
			closePauseScreen();
			currentScreen = Screen.MAIN;
			currentOption = 0;
			break;
		default:
			break;
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
			currentScreen = pause ? Screen.PAUSE : Screen.MAIN;
			currentOption = 0;
			break;
		default:
			break;
		}
	}

	private void handleLoadSelection() {
		if (currentOption >= LOAD_SLOT_LABELS.length) {
			currentScreen = pause ? Screen.PAUSE : Screen.MAIN;
			currentOption = 0;
			return;
		}
		int slotId = currentOption + 1;
		if (SaveManager.hasSlotSave(slotId)) {
			SaveManager.activeSlot = slotId;
			Game game = Game.getInstance();
			if (game != null) {
				if (SaveManager.loadSlot(slotId)) {
					SaveManager.saveAutoSave();
				}
			} else {
				SaveManager.loadSlot(slotId);
			}
			if (OptionsConfig.isMusicEnabled()) {
				OptionsConfig.applyMusicPreference();
			} else if (Sound.music != null) {
				Sound.music.stop();
			}
		}
		currentScreen = pause ? Screen.PAUSE : Screen.MAIN;
		currentOption = 0;
	}

	private static boolean askExit() {
		int result = JOptionPane.showConfirmDialog(
				null,
				"Deseja realmente sair?",
				"Fechar o jogo",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		return result == JOptionPane.YES_OPTION;
	}

	public void render(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		g2.setColor(new Color(0, 0, 0, 150));
		g2.fillRect(0, 0, screenWidth, screenHeight);

		String title = pause ? ">Pausa<" : ">Traduvert<";
		Font titleFont = new Font("arial", Font.BOLD, 40);
		g.setFont(titleFont);
		int titleX = (screenWidth - g.getFontMetrics().stringWidth(title)) / 2;
		int titleBaseline = (int) (screenHeight * 0.28);
		g.setColor(Color.yellow);
		g.drawString(title, titleX, titleBaseline);

		switch (currentScreen) {
		case PAUSE:
			renderPauseMenu(g);
			break;
		case OPTIONS:
			renderOptionsMenu(g);
			break;
		case LOAD:
			renderLoadMenu(g);
			break;
		case HOW_TO_PLAY:
			renderHowToPlay(g);
			break;
		default:
			renderMainMenu(g);
			break;
		}
	}

	private void renderMainMenu(Graphics g) {
		Font optionFont = new Font("arial", Font.BOLD, 25);
		g.setFont(optionFont);
		String[] labels = new String[MAIN_OPTIONS.length];
		int maxWidth = 0;
		for (int i = 0; i < MAIN_OPTIONS.length; i++) {
			labels[i] = getMainMenuLabel(i);
			maxWidth = Math.max(maxWidth, g.getFontMetrics().stringWidth(labels[i]));
		}

		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;
		int textX = (screenWidth - maxWidth) / 2;
		int arrowX = textX - g.getFontMetrics().charWidth('>') - 16;
		int totalHeight = MAIN_OPTIONS.length * LINE_HEIGHT;
		int startY = (screenHeight - totalHeight) / 2 + g.getFontMetrics().getAscent();

		for (int i = 0; i < MAIN_OPTIONS.length; i++) {
			int baselineY = startY + (LINE_HEIGHT * i);
			if (currentOption == i) {
				g.setColor(Color.white);
				g.drawString(">", arrowX, baselineY);
			}

			if (!isOptionAvailable(MAIN_OPTIONS[i])) {
				g.setColor(Color.LIGHT_GRAY);
			} else {
				g.setColor(Color.white);
			}

			g.drawString(labels[i], textX, baselineY);
		}
	}

	private boolean isOptionAvailable(String option) {
		if ("carregar jogo".equals(option)) {
			return saveExists;
		}
		if ("continuar".equals(option)) {
			return pause || saveExists;
		}
		return true;
	}

	private String getMainMenuLabel(int index) {
		switch (index) {
		case OPTION_NEW_GAME:
			return pause ? "Continuar" : "Novo jogo";
		case OPTION_CONTINUE:
			return "Continuar";
		case OPTION_LOAD_GAME:
			return saveExists ? "Carregar jogo" : "Carregar jogo (indisponível)";
		case OPTION_HOW_TO_PLAY:
			return "Como jogar";
		case OPTION_SETTINGS:
			return "Opções";
		case OPTION_EXIT:
			return "Sair";
		default:
			return "";
		}
	}

	private void renderPauseMenu(Graphics g) {
		renderOptionList(g, PAUSE_OPTIONS_LIST, pauseMenuLabel(0));
	}

	private String pauseMenuLabel(int index) {
		String label = PAUSE_OPTIONS_LIST[index];
		if ("carregar jogo".equals(label)) {
			return saveExists ? "carregar jogo" : "carregar jogo (indisponível)";
		}
		return label;
	}

	private void renderLoadMenu(Graphics g) {
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		Font headerFont = new Font("arial", Font.BOLD, 28);
		g.setFont(headerFont);
		String header = "Carregar jogo";
		int headerWidth = g.getFontMetrics().stringWidth(header);

		Font optionFont = new Font("arial", Font.BOLD, 22);
		g.setFont(optionFont);

		String[] lines = new String[LOAD_SLOT_LABELS.length + 1];
		for (int i = 0; i < LOAD_SLOT_LABELS.length; i++) {
			int slotId = i + 1;
			String detail = "";
			if (SaveManager.hasSlotSave(slotId)) {
				int level = SaveManager.getSlotLevel(slotId);
				int slotScore = SaveManager.getSlotScore(slotId);
				detail = String.format("  (Fase %d — Pontuação %d)", level, slotScore);
			} else {
				detail = "  (vazio)";
			}
			lines[i] = LOAD_SLOT_LABELS[i] + detail;
		}
		lines[LOAD_SLOT_LABELS.length] = "Voltar";

		int maxWidth = headerWidth;
		for (String line : lines) {
			maxWidth = Math.max(maxWidth, g.getFontMetrics().stringWidth(line));
		}

		int textX = (screenWidth - maxWidth) / 2;
		int totalHeight = g.getFontMetrics(headerFont).getHeight() + lines.length * OPTIONS_LINE_HEIGHT;
		int startY = (screenHeight - totalHeight) / 2;
		int headerBaseline = startY + g.getFontMetrics(headerFont).getAscent();

		g.setColor(Color.white);
		g.setFont(headerFont);
		g.drawString(header, (screenWidth - headerWidth) / 2, headerBaseline);

		g.setFont(optionFont);
		int arrowX = textX - g.getFontMetrics().charWidth('>') - 16;
		for (int i = 0; i < lines.length; i++) {
			int baselineY = headerBaseline + OPTIONS_LINE_HEIGHT * (i + 1);
			if (currentOption == i) {
				g.setColor(Color.yellow);
				g.drawString(">", arrowX, baselineY);
				g.setColor(Color.white);
			}
			if (i < LOAD_SLOT_LABELS.length && !SaveManager.hasSlotSave(i + 1)) {
				g.setColor(Color.LIGHT_GRAY);
			}
			g.drawString(lines[i], textX, baselineY);
		}
	}

	private void renderHowToPlay(Graphics g) {
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		Font headerFont = new Font("arial", Font.BOLD, 28);
		g.setFont(headerFont);
		String header = "Como jogar";
		g.setColor(Color.white);
		g.drawString(header, (screenWidth - g.getFontMetrics().stringWidth(header)) / 2, 110);

		Font optionFont = new Font("arial", Font.PLAIN, 18);
		g.setFont(optionFont);

		String[] lines = {
				"WASD ou setas: mover",
				"X ou clique do mouse: atirar",
				"Shift: dash / esquiva",
				"F: habilidade especial",
				"Q / E: alternar armas",
				"1 a 6: selecionar arma direta",
				"Space: pular",
				"T: salvar rapidamente",
				"TAB: painel tático",
				"ESC: pausar",
				"",
				"Pressione Enter para voltar"
		};

		int maxWidth = 0;
		for (String line : lines) {
			maxWidth = Math.max(maxWidth, g.getFontMetrics().stringWidth(line));
		}
		int textX = (screenWidth - maxWidth) / 2;
		int startY = 140;
		for (int i = 0; i < lines.length; i++) {
			g.setColor(Color.white);
			g.drawString(lines[i], textX, startY + (OPTIONS_LINE_HEIGHT * i));
		}
	}

	private void renderOptionsMenu(Graphics g) {
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		Font headerFont = new Font("arial", Font.BOLD, 28);
		g.setFont(headerFont);
		String header = "Opções";
		int headerWidth = g.getFontMetrics().stringWidth(header);

		Font optionFont = new Font("arial", Font.PLAIN, 22);
		g.setFont(optionFont);
		String[] lines = {
				"Música: " + (OptionsConfig.isMusicEnabled() ? "Ligada" : "Desligada"),
				"Dificuldade: " + OptionsConfig.getDifficulty().getDisplayName(),
				"Voltar"
		};

		int maxWidth = headerWidth;
		for (String line : lines) {
			maxWidth = Math.max(maxWidth, g.getFontMetrics().stringWidth(line));
		}

		int textX = (screenWidth - maxWidth) / 2;
		int totalHeight = g.getFontMetrics(headerFont).getHeight() + OPTIONS_LABELS.length * OPTIONS_LINE_HEIGHT;
		int startY = (screenHeight - totalHeight) / 2;
		int headerBaseline = startY + g.getFontMetrics(headerFont).getAscent();

		g.setColor(Color.white);
		g.setFont(headerFont);
		g.drawString(header, (screenWidth - headerWidth) / 2, headerBaseline);

		g.setFont(optionFont);
		int arrowX = textX - g.getFontMetrics().charWidth('>') - 16;
		for (int i = 0; i < lines.length; i++) {
			int baselineY = headerBaseline + OPTIONS_LINE_HEIGHT * (i + 1);
			if (currentOption == i) {
				g.setColor(Color.yellow);
				g.drawString(">", arrowX, baselineY);
				g.setColor(Color.white);
			}
			g.drawString(lines[i], textX, baselineY);
		}
	}

	private void renderOptionList(Graphics g, String[] labels, String headerLabel) {
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		Font headerFont = new Font("arial", Font.BOLD, 28);
		g.setFont(headerFont);
		int headerWidth = g.getFontMetrics().stringWidth(headerLabel);

		Font optionFont = new Font("arial", Font.BOLD, 22);
		g.setFont(optionFont);

		int maxWidth = headerWidth;
		for (String label : labels) {
			maxWidth = Math.max(maxWidth, g.getFontMetrics().stringWidth(label));
		}

		int textX = (screenWidth - maxWidth) / 2;
		int totalHeight = g.getFontMetrics(headerFont).getHeight() + labels.length * OPTIONS_LINE_HEIGHT;
		int startY = (screenHeight - totalHeight) / 2;
		int headerBaseline = startY + g.getFontMetrics(headerFont).getAscent();

		g.setColor(Color.white);
		g.setFont(headerFont);
		g.drawString(headerLabel, (screenWidth - headerWidth) / 2, headerBaseline);

		g.setFont(optionFont);
		int arrowX = textX - g.getFontMetrics().charWidth('>') - 16;
		for (int i = 0; i < labels.length; i++) {
			int baselineY = headerBaseline + OPTIONS_LINE_HEIGHT * (i + 1);
			if (currentOption == i) {
				g.setColor(Color.yellow);
				g.drawString(">", arrowX, baselineY);
				g.setColor(Color.white);
			}
			g.drawString(labels[i], textX, baselineY);
		}
	}

	/** @deprecated Use {@link SaveManager} para persistência. */
	@Deprecated
	public static String loadGame(int encode) {
		return "";
	}

	/** @deprecated Use {@link SaveManager} para persistência. */
	@Deprecated
	public static void saveGame(String[] val1, int[] val2, int encode) {
		SaveManager.saveCurrentGame();
	}

	/** @deprecated Use {@link SaveManager} para persistência. */
	@Deprecated
	public static void applySave(String str) {
		if (str == null || str.isEmpty()) {
			return;
		}
		Game game = Game.getInstance();
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
				Player.weapon = Integer.parseInt(value);
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
				com.traduvertgames.entities.Enemy.enemies = Integer.parseInt(value);
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
					}
				}
				break;
			}
		}
		if (game != null) {
			game.applyPostLoadAdjustments();
		}
	}
}
