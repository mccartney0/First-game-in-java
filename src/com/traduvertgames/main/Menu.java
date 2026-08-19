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
		SAVE,
		HOW_TO_PLAY,
		EXIT_CONFIRM
	}

		private static final int OPTION_RPG_ADVENTURE = 0;
		private static final int OPTION_CAMPAIGN = 1;
		private static final int OPTION_CONTINUE = 2;
		private static final int OPTION_LOAD_GAME = 3;
		private static final int OPTION_HOW_TO_PLAY = 4;
		private static final int OPTION_SETTINGS = 5;
		// Rodada 29 — metagame: opção do menu principal para as melhorias
		// permanentes do piloto, compráveis com os créditos persistentes.
		private static final int OPTION_UPGRADES = 6;
		private static final int OPTION_NEW_GAME_PLUS = 7;
		private static final int OPTION_EXIT = 8;

		private static final String[] MAIN_OPTIONS = {
				"aventura RPG",
				"campanha narrativa",
				"continuar",
				"carregar jogo",
				"como jogar",
				"opções",
				"melhorias do piloto",
				"nova campanha+",
				"sair"
		};

	private static final int PAUSE_CONTINUE = 0;
	private static final int PAUSE_LOAD_GAME = 1;
	private static final int PAUSE_SAVE_GAME = 2;
	private static final int PAUSE_SAVE_NEW_SLOT = 3;
	private static final int PAUSE_RESTART_MISSION = 4;
	private static final int PAUSE_OPTIONS = 5;
	private static final int PAUSE_MAIN_MENU = 6;
	private static final int PAUSE_EXIT = 7;

	private static final String[] PAUSE_OPTIONS_LIST = {
			"continuar",
			"carregar jogo",
			"salvar jogo",
			"salvar em novo slot",
			"reiniciar missão atual",
			"opções",
			"voltar ao menu principal",
			"sair do jogo"
	};

	private static final String[] LOAD_SLOT_LABELS = { "Slot 1", "Slot 2", "Slot 3" };
	private static final int LOAD_BACK = SaveManager.SLOT_COUNT;

	private static final int OPTIONS_INDEX_MUSIC = 0;
	private static final int OPTIONS_INDEX_MUSIC_VOLUME = 1;
	private static final int OPTIONS_INDEX_SOUND = 2;
	private static final int OPTIONS_INDEX_SOUND_VOLUME = 3;
	private static final int OPTIONS_INDEX_DIFFICULTY = 4;
	private static final int OPTIONS_INDEX_LANGUAGE = 5;
	private static final int OPTIONS_INDEX_BACK = 6;

	private static final String[] OPTIONS_LABELS = {
			"musica",
			"volume da trilha",
			"efeitos sonoros",
			"volume dos efeitos",
		"dificuldade",
		"idioma",
		"voltar"
	};

	private static final int LINE_HEIGHT = 40;
	private static final int OPTIONS_LINE_HEIGHT = 36;
	private static final int OPTION_Y_START_RATIO = 46;

	private Screen currentScreen = Screen.MAIN;
	private int currentOption = 0;
	private int exitConfirmSelection = 0; // 0 = Não, 1 = Sim
	private String saveFeedback = "";
	private int saveFeedbackFrames = 0;

	public boolean up, down, enter, left, right, escape;

	/** Rodada 22: Shift + Enter nas opções alterna o sentido (ex.: volume da trilha). */
	public boolean shift;

	public static boolean pause = false;

	public static boolean saveExists = false;

	public Menu() {
		// O estado persistente é carregado uma vez ao abrir a sessão. Recarregar
		// o disco em todo frame revertia compras feitas poucos milissegundos antes.
		SaveManager.refreshBestRun();
		SaveManager.refreshMetagame();
		SaveManager.refreshPostCampaignFlags();
	}

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
		if (saveFeedbackFrames > 0) {
			saveFeedbackFrames--;
			if (saveFeedbackFrames == 0) {
				saveFeedback = "";
			}
		}

		// Rodada 29 — metagame: enquanto a tela de melhorias do piloto estiver
		// aberta, a navegação vertical e a confirmação operam sobre ela — o
		// menu principal fica parado atrás para o jogador conferir o saldo.
		boolean upgradesOpen = com.traduvertgames.graficos.PilotUpgradesScreen.isOpen();
		if (up) {
			up = false;
			if (upgradesOpen) {
				com.traduvertgames.graficos.PilotUpgradesScreen.up();
			} else {
				moveSelection(-1);
			}
		}
		if (down) {
			down = false;
			if (upgradesOpen) {
				com.traduvertgames.graficos.PilotUpgradesScreen.down();
			} else {
				moveSelection(1);
			}
		}
		// Navegação horizontal por A/D e setas esquerda/direita: nas telas de
		// opções (pausa, opções, carregar, confirmação de saída) move a seleção
		// para os lados; no EXIT_CONFIRM escolhe Não/Sim.
			if (left) {
				left = false;
				if (!upgradesOpen) {
					if (!adjustSelectedVolume(-2)) {
						moveSelection(-1);
					}
				}
			}
			if (right) {
				right = false;
				if (!upgradesOpen) {
					if (!adjustSelectedVolume(2)) {
						moveSelection(1);
					}
				}
			}
		// ESC fecha a tela atual voltando ao nível anterior (o jogador ficava
		// preso em "Deseja realmente sair?" e nas demais telas do menu).
		// Rodada 29 — metagame: ESC fecha a tela de melhorias do piloto.
		if (escape) {
			escape = false;
			if (upgradesOpen) {
				com.traduvertgames.graficos.PilotUpgradesScreen.close();
			} else {
				escapeFromCurrentScreen();
			}
		}
		if (enter) {
			enter = false;
			// Rodada 29 — metagame: Enter compra o upgrade selecionado quando
				// a tela de melhorias do piloto estiver aberta.
			if (upgradesOpen) {
				// Mantém a tela aberta para exibir imediatamente o novo nível, o
				// saldo restante e permitir compras consecutivas.
				com.traduvertgames.graficos.PilotUpgradesScreen.confirm();
				return;
			}
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
			case SAVE:
				handleSaveSelection();
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
		case SAVE:
			currentScreen = Screen.PAUSE;
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
		case SAVE:
			return LOAD_SLOT_LABELS.length + 1;
		case EXIT_CONFIRM:
			return 2;
		default:
			return 0;
		}
	}

	/** Inicia a tela de pausa sem sair do jogo. */
	public static void openPauseScreen() {
		InventoryManager.close();
		LevelSelectScreen.close();
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
		case OPTION_RPG_ADVENTURE:
			if (!pause) {
				Game adventureGame = Game.getInstance();
				if (adventureGame != null) {
					adventureGame.startNewGame();
				}
			}
			break;
		case OPTION_CAMPAIGN:
			if (!pause) {
				Game campaignGame = Game.getInstance();
				if (campaignGame != null) {
					campaignGame.startNarrativeCampaign();
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
		// Rodada 29 — metagame: abre a tela de melhorias permanentes do piloto.
		case OPTION_UPGRADES:
			com.traduvertgames.graficos.PilotUpgradesScreen.open();
			break;
		// Rodada 31 — conteúdo pós-campanha: a Nova campanha+ ativa a flag do
		// bônus persistente e reinicia a campanha do zero (herdando o arsenal).
		case OPTION_NEW_GAME_PLUS:
			if (SaveManager.hasCampaignCompleted()) {
				SaveManager.setNewGamePlus(true);
				Game game = Game.getInstance();
				if (game != null) {
					game.startNewGamePlus();
				} else {
					closePauseScreen();
				}
			}
			break;
		case OPTION_EXIT:
			currentScreen = Screen.EXIT_CONFIRM;
			exitConfirmSelection = 0;
			break;
		default:
			break;
		}
	}

	/** Contagem das opções do menu principal (exposta para testes — rodada 31). */
	public int getMainMenuOptionCountForTest() {
		return MAIN_OPTIONS.length;
	}

	/** Disponibilidade da opção Nova campanha+ (exposta para testes — rodada 31). */
	public boolean isNewGamePlusAvailableForTest() {
		return isOptionAvailable("nova campanha+");
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
				com.traduvertgames.graficos.MissionBanner.show(
						"JOGO SALVO",
						"Progresso gravado no slot " + SaveManager.activeSlot,
						new Color(76, 175, 80), Color.WHITE, 180);
			} else {
				com.traduvertgames.graficos.MissionBanner.show(
						"ERRO AO SALVAR",
						"Não foi possível gravar o slot " + SaveManager.activeSlot,
						new Color(244, 67, 54), Color.WHITE, 240);
			}
			closePauseScreen();
			break;
		case PAUSE_SAVE_NEW_SLOT:
			currentScreen = Screen.SAVE;
			currentOption = 0;
			break;
		case PAUSE_RESTART_MISSION:
			Game game = Game.getInstance();
			if (game != null) {
				game.restartCurrentMission();
			}
			break;
		case PAUSE_OPTIONS:
			currentScreen = Screen.OPTIONS;
			currentOption = 0;
			break;
		case PAUSE_MAIN_MENU:
			Game currentGame = Game.getInstance();
			if (currentGame != null) {
				currentGame.returnToMainMenu();
			}
			break;
		case PAUSE_EXIT:
			currentScreen = Screen.EXIT_CONFIRM;
			exitConfirmSelection = 0;
			break;
		default:
			break;
		}
	}

	private boolean adjustSelectedVolume(int deltaDb) {
		if (currentScreen != Screen.OPTIONS) {
			return false;
		}
		if (currentOption == OPTIONS_INDEX_MUSIC_VOLUME) {
			OptionsConfig.adjustMusicVolume(deltaDb);
			return true;
		}
		if (currentOption == OPTIONS_INDEX_SOUND_VOLUME) {
			OptionsConfig.adjustSoundVolume(deltaDb);
			SoundManager.play(SoundManager.Event.MENU_SELECT);
			return true;
		}
		return false;
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
		case OPTIONS_INDEX_SOUND:
			OptionsConfig.toggleSound();
			SoundManager.play(SoundManager.Event.MENU_SELECT);
			break;
		case OPTIONS_INDEX_SOUND_VOLUME:
			if (shift) {
				OptionsConfig.adjustSoundVolume(-2);
			} else {
				OptionsConfig.adjustSoundVolume(2);
			}
			SoundManager.play(SoundManager.Event.MENU_SELECT);
			break;
		case OPTIONS_INDEX_DIFFICULTY:
			OptionsConfig.cycleDifficulty();
			break;
		case OPTIONS_INDEX_LANGUAGE:
			Localization.cycleLanguage();
			SoundManager.play(SoundManager.Event.MENU_SELECT);
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

	private void handleSaveSelection() {
		if (currentOption >= LOAD_SLOT_LABELS.length) {
			currentScreen = Screen.PAUSE;
			currentOption = 0;
			return;
		}
		int slotId = currentOption + 1;
		// "Novo slot" nunca sobrescreve progresso existente sem confirmação.
		if (SaveManager.hasSlotSave(slotId)) {
			saveFeedback = "Slot ocupado: escolha um slot vazio";
			saveFeedbackFrames = 180;
			SoundManager.play(SoundManager.Event.PICKUP);
			return;
		}
		if (SaveManager.saveCurrentGameToSlot(slotId)) {
			com.traduvertgames.graficos.MissionBanner.show(
					"JOGO SALVO",
					"Novo progresso gravado no slot " + slotId,
					new Color(76, 175, 80), Color.WHITE, 180);
			closePauseScreen();
		} else {
			saveFeedback = "Não foi possível gravar o slot " + slotId;
			saveFeedbackFrames = 240;
		}
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
		saveFeedback = "";
		saveFeedbackFrames = 0;
		pause = false;
		clearPendingInput();
	}

	/** Descarta eventos que chegaram enquanto outro overlay estava na frente. */
	public void clearPendingInput() {
		up = false;
		down = false;
		enter = false;
		left = false;
		right = false;
		escape = false;
		shift = false;
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
				|| currentScreen == Screen.SAVE
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
		case SAVE:
			renderSaveMenu(g);
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

		// Rodada 29 — metagame: a tela de melhorias do piloto é desenhada por
		// cima do menu principal (fundo translúcido, saldo e lista de upgrades).
		com.traduvertgames.graficos.PilotUpgradesScreen.draw(g);
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

		// Saldo em memória: compras e recompensas aparecem imediatamente. O
		// estado do disco é carregado no construtor e nas rotinas de load.
		int credits = com.traduvertgames.state.PilotUpgrades.getCredits();
		Font creditFont = new Font("arial", Font.BOLD, 16);
		g.setFont(creditFont);
		String creditLabel = "CREDITOS: " + credits;
		g.setColor(new java.awt.Color(255, 214, 10));
		g.drawString(creditLabel, (screenWidth - g.getFontMetrics().stringWidth(creditLabel)) / 2,
				(int) (screenHeight * 0.28) + g.getFontMetrics().getHeight() + 6);
	}

	private boolean isOptionAvailable(String option) {
		if ("carregar jogo".equals(option)) {
			return saveExists;
		}
		if ("continuar".equals(option)) {
			return pause || saveExists;
		}
		// Rodada 31: a Nova campanha+ só fica disponível após concluir a
		// campanha completa.
		if ("nova campanha+".equals(option)) {
			return SaveManager.hasCampaignCompleted();
		}
		return true;
	}

	private String getMainMenuLabel(int index) {
		switch (index) {
		case OPTION_RPG_ADVENTURE:
			return pause ? "Continuar" : "Aventura RPG";
		case OPTION_CAMPAIGN:
			return "Campanha narrativa";
		case OPTION_CONTINUE:
			return "Continuar";
		case OPTION_LOAD_GAME:
			return saveExists ? "Carregar jogo" : "Carregar jogo (indisponível)";
		case OPTION_HOW_TO_PLAY:
			return "Como jogar";
		case OPTION_SETTINGS:
			return "Opções";
		case OPTION_UPGRADES:
			return "Melhorias do piloto";
		// Rodada 31: a Nova campanha+ fica invisível enquanto a campanha não
		// for concluída (a opção é desabilitada em isOptionAvailable).
		case OPTION_NEW_GAME_PLUS:
			return "Nova campanha+";
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

	private void renderSaveMenu(Graphics g) {
		String[] lines = new String[LOAD_SLOT_LABELS.length + 1];
		for (int i = 0; i < LOAD_SLOT_LABELS.length; i++) {
			int slotId = i + 1;
			if (SaveManager.hasSlotSave(slotId)) {
				lines[i] = LOAD_SLOT_LABELS[i] + "  (ocupado — Fase "
						+ SaveManager.getSlotLevel(slotId) + ")";
			} else {
				lines[i] = LOAD_SLOT_LABELS[i] + "  (vazio)";
			}
		}
		lines[LOAD_SLOT_LABELS.length] = "Voltar";
		renderOptionList(g, lines, "Salvar em novo slot");
		if (!saveFeedback.isEmpty()) {
			g.setFont(new Font("arial", Font.BOLD, 15));
			g.setColor(new Color(255, 152, 0));
			int screenWidth = Game.WIDTH * Game.SCALE;
			g.drawString(saveFeedback,
					(screenWidth - g.getFontMetrics().stringWidth(saveFeedback)) / 2,
					Game.HEIGHT * Game.SCALE - 48);
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
					"WASD/setas: mover — SPACE: pular — X/clique: atirar",
					"Q/E ou 1–6: trocar arma — SHIFT: dash — F: especial",
					"TAB: painel tático — I: inventário — P/ESC: pausa",
					"Fase 1: fale com Ava e colete 2 artefatos do setor",
					"Fase 2: ative o beacon, defenda a área e derrote o Warbringer",
					"Fase 3–5: sobreviva, desative obeliscos, resgate e recupere dados",
					"Fase 6–8: resista, sabote, defenda e escolte até o núcleo",
					"Matar concede XP; a loja e as estatísticas aparecem ao concluir a fase",
					"PHANTOM drena escudo/mana — mantenha distância; elites brilham em dourado",
					"Morrer salva automaticamente; no game over use setas/A-D e ENTER",
					"Opções: música, volume da trilha, efeitos, volume e dificuldade",
					"ENTER ou ESC: voltar"
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
		String header = Localization.tr("menu.options");
		int headerWidth = g.getFontMetrics().stringWidth(header);

		Font optionFont = new Font("arial", Font.PLAIN, 22);
		g.setFont(optionFont);
		// Rodada 22: volume da trilha sonora adaptativa é independente dos efeitos.
		int musicDb = (int) Math.round(OptionsConfig.getMusicVolume() / 2);
			int soundDb = (int) Math.round(OptionsConfig.getSoundVolume() / 2);
			String[] lines = {
					Localization.tr("menu.music") + ": " + (OptionsConfig.isMusicEnabled() ? (Localization.getLanguage() == Localization.Language.PT_BR ? "Ligada" : "On") : (Localization.getLanguage() == Localization.Language.PT_BR ? "Desligada" : "Off")),
					Localization.tr("menu.music_volume", (musicDb > 0 ? "+" : "") + musicDb),
					Localization.tr("menu.sound") + ": " + (OptionsConfig.isSoundEnabled() ? (Localization.getLanguage() == Localization.Language.PT_BR ? "Ligados" : "On") : (Localization.getLanguage() == Localization.Language.PT_BR ? "Desligados" : "Off")),
					Localization.tr("menu.sound_volume", (soundDb > 0 ? "+" : "") + soundDb),
					Localization.tr("menu.difficulty") + ": " + OptionsConfig.getDifficulty().getDisplayName(),
					Localization.tr("menu.language") + ": " + Localization.getLanguageLabel(),
					Localization.tr("menu.back")
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
			Font hintFont = new Font("arial", Font.PLAIN, 13);
			g.setFont(hintFont);
			g.setColor(new Color(190, 190, 190));
			String hint = "Esquerda/direita: ajustar volume — Enter: alternar/confirmar";
			g.drawString(hint, (screenWidth - g.getFontMetrics().stringWidth(hint)) / 2,
					startY + OPTIONS_LABELS.length * OPTIONS_LINE_HEIGHT + 28);
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
