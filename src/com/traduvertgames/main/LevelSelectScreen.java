package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.quest.QuestManager;

/**
 * Tela de seleção de fases com atalho do menu: apresenta as 8 fases do jogo
 * e permite reiniciar em qualquer uma delas (perdendo os recursos da partida
 * atual, como um treino livre).
 */
import com.traduvertgames.main.Game;
import com.traduvertgames.entities.UltimateAbility;
import com.traduvertgames.entities.DashAbility;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.main.OnboardingManager;

public final class LevelSelectScreen {

	private static final int TOTAL_LEVELS = 9;
	private static final String[] LEVEL_NAMES = {
			"Setor Alpha — Coleta de artefatos",
			"Câmara do Warbringer — Caçada ao chefe",
			"Círculo do Ritual — Ritual sombrio",
			"Núcleo da Colônia — Resgate",
			"Datacenter Nexus — Recuperação de dados",
			"Torre do Supervisor — Queda do Supervisor",
			"Subsolo da Colônia — Sabotagem dos geradores",
			"Núcleo Central — Destruição da IA",
			"Modo Infinito — Fases Procedurais"
	};

	private static int selection = 0;
	private static boolean open = false;
	/** Frames restantes do aviso "fase travada" (som e feedback ao tentar entrar). */
	private static int lockFeedbackFrames = 0;

	private LevelSelectScreen() {
	}

	public static boolean isOpen() {
		return open;
	}

	/** @return fase mais alta alcançada pela campanha (mínimo 1); usada para
	 *  destravar a seleção de fases pelo progresso real do save. */
	public static int getUnlockedUpTo() {
		int reached = SaveManager.getHighestUnlockedLevel();
		if (reached < 1) {
			reached = 1;
		}
		return reached;
	}

	/** @return true se a fase indicada (1..9) está destravada pelo progresso. */
	public static boolean isUnlocked(int level) {
		if (level == TOTAL_LEVELS) {
			// O modo infinito abre junto da fase 8 (última da campanha).
			return getUnlockedUpTo() >= Game.MAX_LEVEL;
		}
		// A fase atual do jogador e todas as anteriores estão destravadas:
		// a progressão da campanha destrava a próxima automaticamente.
		return level <= getUnlockedUpTo();
	}

	public static void open() {
		// A seleção de fases é modal: nunca pode coexistir com o inventário.
		InventoryManager.close();
		open = true;
		selection = Math.max(0, Math.min(TOTAL_LEVELS - 1, QuestManager.getCurrentLevel() - 1));
		// Garante que a seleção inicial nunca cai em uma fase travada.
		while (!isUnlocked(selection + 1)) {
			selection = (selection + 1) % TOTAL_LEVELS;
		}
		Game.gameState = "LEVELSELECT";
	}

	/** Fecha a tela de seleção restaurando o jogo (não o menu principal).
	 * A tela pode ser aberta no meio de uma fase — fechar deve devolver o
	 * jogador ao combate em vez de mandá-lo de volta ao menu. */
	public static void close() {
		if (!open) {
			return;
		}
		open = false;
		Game.gameState = "NORMAL";
	}

	/** TAB também fecha a tela (mesmo atalho do painel tático, sem conflito). */
	public static void closeOnTab() {
		if (open) {
			close();
		}
	}

	public static void update() {
		if (lockFeedbackFrames > 0) {
			lockFeedbackFrames--;
		}
		if (!open) {
			return;
		}
		Game game = Game.getInstance();
		if (game == null) {
			return;
		}
		if (Game.getInstance().menu.up) {
			Game.getInstance().menu.up = false;
			navigateUp();
		} else if (Game.getInstance().menu.down) {
			Game.getInstance().menu.down = false;
			navigateDown();
		} else if (Game.getInstance().menu.enter) {
			Game.getInstance().menu.enter = false;
			confirmSelection();
		} else if (Game.escapePressed) {
			Game.escapePressed = false;
			close();
		}
	}

	/** Navegação exposta para o handler de teclado do Game. */
	public static void navigateUp() {
		if (!open) {
			return;
		}
		// Rodada 22b: a navegação circula apenas pelas fases destravadas —
		// as travadas ficam puladas pelas setas, para não confundir o jogador.
		for (int attempts = 0; attempts < TOTAL_LEVELS; attempts++) {
			selection = (selection - 1 + TOTAL_LEVELS) % TOTAL_LEVELS;
			if (isUnlocked(selection + 1)) {
				return;
			}
		}
	}

	/** Navegação exposta para o handler de teclado do Game. */
	public static void navigateDown() {
		if (!open) {
			return;
		}
		for (int attempts = 0; attempts < TOTAL_LEVELS; attempts++) {
			selection = (selection + 1) % TOTAL_LEVELS;
			if (isUnlocked(selection + 1)) {
				return;
			}
		}
	}

	/** Confirma a seleção (Enter). Se a fase escolhida for a atual, apenas
	 * fecha a tela: reiniciar a própria fase por acidente já custou partidas
	 * inteiras (o jogador perdeu o progresso dos recursos coletados). */
	public static void confirmSelection() {
		if (!open) {
			return;
		}
		int chosen = selection + 1;
		if (chosen == QuestManager.getCurrentLevel()) {
			close();
			return;
		}
		if (!isUnlocked(chosen)) {
			// Rodada 22b: fases não concluídas não podem ser selecionadas —
			// só o avanço natural da campanha destrava a próxima.
			lockFeedbackFrames = 45;
			SoundManager.play(SoundManager.Event.PICKUP);
			return;
		}
		playLevel(chosen);
	}

	private static void playLevel(int level) {
		Game game = Game.getInstance();
		if (game == null) {
			System.out.println("[LSS] playLevel(" + level + ") ignorado: game null");
			return;
		}
		System.out.println("[LSS] playLevel(" + level + ") selecionado; gameState=" + Game.gameState);
		// Cancela qualquer avanço de fase pendente da loja anterior.
		Game.clearQuestPending();
		Game.player.resetPersistentArsenal();
		com.traduvertgames.entities.Player.resetBaseStats();
		Game.resetTraitorTalked();
		Enemy.enemies = 0;
		Game.setScore(0);
		LevelUpManager.resetProgress();
		DashAbility.reset();
		UltimateAbility.reset();
		WaveManager.reset();
		// Opção 9: modo infinito — fases procedurais geradas por semente.
		if (level == TOTAL_LEVELS) {
			Game.enterInfiniteMode();
		} else {
			game.setCurrentLevel(level);
			com.traduvertgames.world.World.restartGame("level" + level + ".png");
		}
		// Trocar de fase abandona a arena de treino — o onboarding não deve
		// continuar ativo por cima da nova fase (bug do painel fantasma).
		OnboardingManager.stop();
		open = false;
		Menu.pause = false;
	}

	public static void render(Graphics g) {
		if (!open) {
			return;
		}
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		g.setColor(new Color(0, 0, 0, 185));
		g.fillRect(0, 0, screenWidth, screenHeight);

		g.setFont(new Font("arial", Font.BOLD, 26));
		g.setColor(Color.yellow);
		String title = "Seleção de fases";
		g.drawString(title, (screenWidth - g.getFontMetrics().stringWidth(title)) / 2, 100);

		g.setFont(new Font("arial", Font.BOLD, 18));
		int panelX = (screenWidth - 420) / 2;
		int panelY = 130;
		int lineHeight = 44;
		int panelHeight = TOTAL_LEVELS * lineHeight + 20;
		g.setColor(new Color(14, 18, 28, 220));
		g.fillRoundRect(panelX, panelY, 420, panelHeight, 16, 16);

		int unlockedUpTo = getUnlockedUpTo();
		for (int i = 0; i < TOTAL_LEVELS; i++) {
			int levelNumber = i + 1;
			boolean unlocked = isUnlocked(levelNumber);
			int rowY = panelY + 16 + lineHeight * i;
			if (selection == i) {
				if (unlocked) {
					g.setColor(new Color(60, 68, 88));
				} else {
					g.setColor(new Color(74, 44, 44));
				}
				g.fillRoundRect(panelX + 8, rowY - 22, 404, 30, 10, 10);
				g.setColor(Color.yellow);
				g.drawString(">", panelX + 22, rowY);
				g.setColor(unlocked ? Color.white : new Color(200, 200, 200));
			} else {
				g.setColor(unlocked ? Color.white : new Color(140, 140, 140));
			}
			String label = "Fase " + (i + 1) + ": " + LEVEL_NAMES[i];
			if (!unlocked) {
				label += "  [TRAVADA]";
			}
			g.drawString(label, panelX + 40, rowY);
		}

		g.setFont(new Font("arial", Font.PLAIN, 14));
		g.setColor(new Color(200, 200, 200));
		String progressHint = "Progresso destravado ate a fase " + unlockedUpTo
				+ (unlockedUpTo >= Game.MAX_LEVEL ? " (Modo Infinito liberado)" : "");
		g.drawString(progressHint, (screenWidth - g.getFontMetrics().stringWidth(progressHint)) / 2, panelY + panelHeight + 30);
		if (lockFeedbackFrames > 0) {
			g.setColor(new Color(255, 87, 34, Math.min(255, lockFeedbackFrames * 6)));
			String lockText = "Conclua a fase anterior para desbloquear";
			g.drawString(lockText, (screenWidth - g.getFontMetrics().stringWidth(lockText)) / 2, panelY - 18);
		}
		String hint = "Setas para escolher — Enter para trocar de fase — ESC ou TAB para voltar";
		g.drawString(hint, (screenWidth - g.getFontMetrics().stringWidth(hint)) / 2, panelY + panelHeight + 52);
	}
}
