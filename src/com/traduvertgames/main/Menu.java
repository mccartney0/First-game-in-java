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
		HOW_TO_PLAY,
		EXIT_CONFIRM
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
	private static final int OPTIONS_INDEX_MUSIC_VOLUME = 1;
	private static final int OPTIONS_INDEX_DIFFICULTY = 2;
	private static final int OPTIONS_INDEX_BACK = 3;

	private static final String[] OPTIONS_LABELS = {
			"musica",
			"volume da trilha",
			"dificuldade",
			"voltar"
	};

	private static final int LINE_HEIGHT = 40;
	private static final int OPTIONS_LINE_HEIGHT = 36;
	private static final int OPTION_Y_START_RATIO = 46;

	private Screen currentScreen = Screen.MAIN;
	private int currentOption = 0;
	private int exitConfirmSelection = 0; // 0 = Não, 1 = Sim

	public boolean up, down, enter, left, right, escape;

	/** Rodada 22: Shift + Enter nas opções alterna o sentido (ex.: volume da trilha). */
	public boolean shift;

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
		// Navegação horizontal por A/D e setas esquerda/direita: nas telas de
		// opções (pausa, opções, carregar, confirmação de saída) move a seleção
		// para os lados; no EXIT_CONFIRM escolhe Não/Sim.
		if (left) {
			left = false;
			moveSelection(-1);
		}
		if (right) {
			right = false;
			moveSelection(1);
		}
		// ESC fecha a tela atual voltando ao nível anterior (o jogador ficava
		// preso em "Deseja realmente sair?" e nas demais telas do menu).
		if (escape) {
			escape = false;
			escapeFromCurrentScreen();
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
				// A tela do tutorial informa "Enter para voltar"; voltar ao
				// menu principal em vez de apenas resetar a seleção (o jogador
				// ficava preso na tela do tutorial).
				currentScreen = Screen.MAIN;
				currentOption = 0;
				break;
			case EXIT_CONFIRM:
				handleExitConfirmSelection();
				break;
			default:
				break;
			}
		}
	}

	/** ESC fecha a tela atual voltando ao nível anterior do menu. */
	private void escapeFromCurrentScreen() {
		switch (currentScreen) {
		case MAIN:
			// ESC no menu principal com o jogo pausado fecha a pausa (volta ao jogo).
			if (pause) {
				closePauseScreen();
			}
			break;
		case PAUSE:
			closePauseScreen();
			break;
		case OPTIONS:
			currentScreen = pause ? Screen.PAUSE : Screen.MAIN;
			currentOption = 0;
			break;
		case LOAD:
			currentScreen = pause ? Screen.PAUSE : Screen.MAIN;
			currentOption = 0;
			break;
		case HOW_TO_PLAY:
			currentScreen = Screen.MAIN;
			currentOption = 0;
			break;
		case EXIT_CONFIRM:
			// ESC na confirmação de saída equivale a "Não".
			currentScreen = pause ? Screen.PAUSE : Screen.MAIN;
			currentOption = 0;
			break;
		default:
			break;
		}
	}

	private void moveSelection(int delta) {
		if (currentScreen == Screen.EXIT_CONFIRM) {
			exitConfirmSelection += delta;
			if (exitConfirmSelection < 0) exitConfirmSelection = 1;
			if (exitConfirmSelection > 1) exitConfirmSelection = 0;
			// Blip discreto ao mover a seleção Não/Sim (rodada 15).
			com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.MENU_SELECT);
			return;
		}
		int count = getCurrentOptionCount();
		if (count <= 0) {
			currentOption = 0;
			return;
		}
		currentOption = (currentOption + delta) % count;
		if (currentOption < 0) {
			currentOption += count;
		}
		// Blip discreto ao mover a seleção nas telas do menu (rodada 15).
		com.traduvertgames.main.SoundManager.play(com.traduvertgames.main.SoundManager.Event.MENU_SELECT);
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
		case EXIT_CONFIRM:
			return 2;
		default:
			return 0;
		}
	}

	/** Inicia a tela de pausa sem sair do jogo. */
	public static void openPauseScreen() {
		pause = true;
		Game.gameState = "MENU";
		Game game = Game.getInstance();
		if (game != null && game.menu != null) {
			game.menu.currentScreen = Screen.PAUSE;
			game.menu.currentOption = 0;
		}
		currentScreenStatic = Screen.PAUSE;
	}

	/** Volta da tela de pausa para o jogo.
	 *  Só altera o estado para NORMAL quando a tela aberta do menu era a tela
	 *  de pausa (currentScreenStatic==PAUSE); voltar de outra tela (game
	 *  over, loja, seleção de fase) para o menu não deve despausar o jogo. */
	public static void closePauseScreen() {
		pause = false;
		if (currentScreenStatic == Screen.PAUSE) {
			Game.gameState = "NORMAL";
			currentScreenStatic = Screen.MAIN;
		}
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
			currentScreen = Screen.EXIT_CONFIRM;
			exitConfirmSelection = 0;
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
			currentScreen = Screen.EXIT_CONFIRM;
			exitConfirmSelection = 0;
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
		case OPTIONS_INDEX_MUSIC_VOLUME:
			// Rodada 22: volume separado da trilha sonora adaptativa —
			// Enter/M aumenta; Shift+M (ou A/seta esquerda) diminui.
			if (shift) {
				OptionsConfig.adjustMusicVolume(-2);
			} else {
				OptionsConfig.adjustMusicVolume(2);
			}
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
			} else {
				if (Sound.music != null) {
					Sound.music.stop();
				}
				com.traduvertgames.main.MusicManager.pause();
			}
		}
		currentScreen = pause ? Screen.PAUSE : Screen.MAIN;
		currentOption = 0;
	}


	private void renderExitConfirm(Graphics g) {
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;
		Font headerFont = new Font("arial", Font.BOLD, 28);
		Font optionFont = new Font("arial", Font.BOLD, 22);
		g.setFont(headerFont);
		String header = "Deseja realmente sair?";
		int headerWidth = g.getFontMetrics().stringWidth(header);
		int textX = (screenWidth - headerWidth) / 2;
		int headerBaseline = screenHeight / 2 - 30;
		g.setColor(Color.white);
		g.setFont(headerFont);
		g.drawString(header, textX, headerBaseline);
		g.setFont(optionFont);
		String[] options = { "Não", "Sim" };
		for (int i = 0; i < options.length; i++) {
			int optionY = headerBaseline + 40 * (i + 1);
			if (exitConfirmSelection == i) {
				g.setColor(Color.yellow);
				g.drawString(">", textX - 20, optionY);
				g.setColor(Color.white);
			}
			g.drawString(options[i], textX, optionY);
		}
	}

	private void handleExitConfirmSelection() {
		if (exitConfirmSelection == 1) {
			// Sim: sair do jogo
			System.exit(0);
		} else {
			// Não: voltar ao menu anterior
			currentScreen = pause ? Screen.PAUSE : Screen.MAIN;
			currentOption = 0;
		}
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

	/** Reseta o menu para a tela principal (usado ao voltar ao menu do jogo). */
	public void resetToMain() {
		currentScreen = Screen.MAIN;
		currentOption = 0;
		pause = false;
	}

	public void render(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		g2.setColor(new Color(0, 0, 0, 150));
		g2.fillRect(0, 0, screenWidth, screenHeight);

		// Painéis do menu (pausa, como jogar, opções, carregar) cobrem o jogo
		// com fundo totalmente opaco para não sobreporem elementos do mundo.
		boolean screenOverlay = currentScreen != Screen.MAIN;
		if (screenOverlay) {
			g.setColor(new Color(0, 0, 0, 255));
			g.fillRect(0, 0, screenWidth, screenHeight);
		}

		// O título amarelo só aparece no menu principal; nos painéis de
		// overlay, cada painel desenha seu próprio cabeçalho limpo.
		if (!screenOverlay) {
			String title = ">Traduvert<";
			Font titleFont = new Font("arial", Font.BOLD, 40);
			g.setFont(titleFont);
			int titleX = (screenWidth - g.getFontMetrics().stringWidth(title)) / 2;
			int titleBaseline = (int) (screenHeight * 0.28);
			g.setColor(Color.yellow);
			g.drawString(title, titleX, titleBaseline);
		}

		// Consistência: se o jogo está em MENU sem pausa e não está em nenhuma
		// tela de overlay, forçar a tela principal. As telas de overlay (PAUSE,
		// LOAD, OPTIONS, HOW_TO_PLAY, EXIT_CONFIRM) são válidas no estado MENU.
		boolean isOverlay = (currentScreen == Screen.PAUSE || currentScreen == Screen.LOAD
				|| currentScreen == Screen.OPTIONS || currentScreen == Screen.HOW_TO_PLAY
				|| currentScreen == Screen.EXIT_CONFIRM);
		if (!pause && currentScreen != Screen.MAIN && !isOverlay) {
			currentScreen = Screen.MAIN;
			currentOption = 0;
		}
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
		case EXIT_CONFIRM:
			renderExitConfirm(g);
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
		// Cabeçalho próprio "Pausa" — a primeira opção da lista já é
		// "continuar", então usar pauseMenuLabel(0) como header duplicava
		// a opção no topo da tela.
		renderOptionList(g, PAUSE_OPTIONS_LIST, "Pausa");
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
					int slotSurvival = SaveManager.getSlotSurvivalRecord(slotId);
					detail = String.format("  (Fase %d — Pontuação %d", level, slotScore);
					if (slotSurvival > 0) {
						detail += String.format(" — Sobrevivência: %d ondas", slotSurvival);
					}
					// Recorde de profundidade (rodada 24b): maior ciclo do infinito.
					int slotDeep = SaveManager.getSlotDeepRecord(slotId);
					if (slotDeep > 0) {
						detail += String.format(" — Profundidades: %d", slotDeep);
					}
					detail += ")";
			} else {
				detail = "  (vazio)";
			}
			lines[i] = LOAD_SLOT_LABELS[i] + detail;
		}
			lines[LOAD_SLOT_LABELS.length] = "Voltar";

			// Rodapé: melhor partida acumulada do save (bestRun v3).
			// Atualiza o recorde acumulado a partir do disco antes de exibir.
			SaveManager.refreshBestRun();
			String bestRunLine = "";
			if (SaveManager.hasBestRun()) {
				bestRunLine = String.format("Melhor partida: %d kills — %s — combo x%d",
						SaveManager.getBestRunKills(),
						Game.formatLevelTime(SaveManager.getBestRunTimeMs()),
						SaveManager.getBestRunCombo());
			}

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

			// Rodapé dourado com a melhor partida do save, alinhado abaixo dos slots.
			if (!bestRunLine.isEmpty()) {
				Font bestFont = new Font("arial", Font.BOLD, 15);
				g.setFont(bestFont);
				g.setColor(new Color(255, 214, 0));
				int bestWidth = g.getFontMetrics().stringWidth(bestRunLine);
				g.drawString(bestRunLine, (screenWidth - bestWidth) / 2,
						headerBaseline + OPTIONS_LINE_HEIGHT * (lines.length + 1) + 6);
			}

		// Progresso de missão de cada slot abaixo do rótulo, com quebra de linha
		// para caber na largura do card e sem sobrepor a opção seguinte.
		Font objectiveFont = new Font("arial", Font.PLAIN, 13);
		g.setFont(objectiveFont);
		g.setColor(new Color(180, 200, 255));
		int maxObjectiveWidth = maxWidth;
		for (int i = 0; i < LOAD_SLOT_LABELS.length; i++) {
			if (SaveManager.hasSlotSave(i + 1)) {
				String objective = SaveManager.getSlotObjectiveText(i + 1);
				if (!objective.isEmpty()) {
					java.util.List<String> wrapped = wrapText(objective, g.getFontMetrics(), maxObjectiveWidth);
					int baselineY = headerBaseline + OPTIONS_LINE_HEIGHT * (i + 1) + 18;
					for (String segment : wrapped) {
						g.drawString(segment, textX, baselineY);
						baselineY += g.getFontMetrics().getHeight();
					}
				}
			}
		}
	}

	private static java.util.List<String> wrapText(String text, java.awt.FontMetrics metrics, int maxWidth) {
		java.util.List<String> lines = new java.util.ArrayList<String>();
		if (text == null || text.isEmpty() || metrics.stringWidth(text) <= maxWidth) {
			lines.add(text == null ? "" : text);
			return lines;
		}
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			current.append(text.charAt(i));
			if (metrics.stringWidth(current.toString()) > maxWidth) {
				// Volta ao último espaço para não quebrar palavra.
				int lastSpace = current.length() - 1;
				while (lastSpace > 0 && current.charAt(lastSpace) != ' ') {
					lastSpace--;
				}
				if (lastSpace <= 0) {
					lines.add(current.toString());
					current = new StringBuilder();
				} else {
					lines.add(current.substring(0, lastSpace).trim());
					current = new StringBuilder(text.substring(i - (current.length() - 1 - lastSpace)));
				}
			}
		}
		if (current.length() > 0) {
			lines.add(current.toString());
		}
		return lines;
	}

	private void renderHowToPlay(Graphics g) {
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		Font headerFont = new Font("arial", Font.BOLD, 28);
		g.setFont(headerFont);
		String header = "Como jogar";
		int headerWidth = g.getFontMetrics().stringWidth(header);

		Font optionFont = new Font("arial", Font.PLAIN, 14);
		g.setFont(optionFont);

		String[] lines = {
				"WASD/setas: mover — Space: pular — X: atirar — Q/E: armas",
				"Shift: dash — F: especial — TAB: painel — L: fases — F11: cheia",
				"Fase 1: recolha as 4 reliquias — Fase 2: derrote o chefe",
				"Fase 3: ative os obeliscos — Fase 4: evacue sobreviventes",
				"Fase 5: proteja a pesquisadora e recupere os nucleos",
				"Matar concede XP e melhoras; a loja abre ao concluir a fase",
				"PHANTOM (verde furtivo) drena escudo/mana: mantenha distancia!",
				"GUARDIAN (laranja) regenera escudo — prioridade alta no ataque",
				"Morrer salva automaticamente; o jogo volta ao menu sozinho",
				"Enter para voltar"
		};

		int maxWidth = 0;
		for (String line : lines) {
			maxWidth = Math.max(maxWidth, g.getFontMetrics().stringWidth(line));
		}
		// Painel escuro por trás do tutorial para garantir legibilidade
		// (evita que o título e o jogo apareçam por cima do texto).
		int panelHeight = 124 + linesBlockHeight(lines, 28) + 20;
		int panelWidth = Math.max(headerWidth, maxWidth) + 80;
		int panelX = (screenWidth - panelWidth) / 2;
		// Centraliza o painel na tela para nenhuma linha ficar cortada
		// em resoluções menores que o conteúdo completo.
		int panelY = Math.max(20, (screenHeight - panelHeight) / 2);
		g.setFont(headerFont);
		g.setColor(new Color(10, 12, 18, 235));
		g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);
		g.setColor(Color.yellow);
		g.drawString(header, (screenWidth - headerWidth) / 2, panelY + 40);

		int textX = (screenWidth - maxWidth) / 2;
		int startY = panelY + 68;
		for (int i = 0; i < lines.length; i++) {
			g.setColor(Color.white);
			g.drawString(lines[i], textX, startY + (28 * i));
		}
	}

	private static int linesBlockHeight(String[] lines, int lineHeight) {
		int count = 0;
		for (String line : lines) {
			if (line != null && !line.isEmpty()) {
				count++;
			}
		}
		return count * lineHeight;
	}

	private static int maxLineWidth(String[] lines, Graphics g) {
		int max = 0;
		for (String line : lines) {
			if (line != null) {
				max = Math.max(max, g.getFontMetrics().stringWidth(line));
			}
		}
		return max;
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
		// Rodada 22: volume da trilha sonora adaptativa é independente dos efeitos.
		int musicDb = (int) Math.round(OptionsConfig.getMusicVolume() / 2);
		String[] lines = {
				"Música: " + (OptionsConfig.isMusicEnabled() ? "Ligada" : "Desligada"),
				"Trilha sonora: " + (musicDb > 0 ? "+" : "") + musicDb + " dB",
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
