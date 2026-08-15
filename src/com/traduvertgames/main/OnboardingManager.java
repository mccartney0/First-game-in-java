package com.traduvertgames.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.entities.DashAbility;
import com.traduvertgames.entities.Player;

/**
 * Onboarding interativo: guia o jogador novato pelas ações básicas antes de
 * liberar o combate. Durante o onboarding, todos os inimigos ficam paralisados
 * (não se movem nem atiram), de modo que o jogador pode praticar sem risco.
 *
 * Etapas:
 * 1. Movimentação — segurar WASD/setas por ~1 segundo de movimento contínuo.
 * 2. Atirar — pressionar X (ou clicar) 3 vezes.
 * 3. Dash — usar Shift 2 vezes.
 * Encerra automaticamente (ou pula com Space) e libera o jogo.
 */
public final class OnboardingManager {

	private static boolean active = false;
	private static int step = 0; // 0=mover, 1=atirar, 2=dash, 3=concluído

	private static int moveFrames = 0;
	private static int shotsDone = 0;
	private static int dashesDone = 0;
	private static boolean wasMoving = false;
	private static boolean wasShootPressed = false;
	private static boolean wasDashing = false;
	private static int skipFrames = 0; // piscada da dica "Space para pular"

	private static final int MOVE_REQUIRED_FRAMES = 60; // ~1 segundo de movimento
	private static final int SHOTS_REQUIRED = 3;
	private static final int DASHES_REQUIRED = 2;

	private OnboardingManager() {
	}

	public static void start() {
		active = true;
		step = 0;
		moveFrames = 0;
		shotsDone = 0;
		dashesDone = 0;
		skipFrames = 0;
	}

	public static boolean isActive() {
		return active;
	}

	/** Avança/encerra o onboarding imediatamente (usado ao pular com Space). */
	public static void skip() {
		finish();
	}

	private static void finish() {
		active = false;
		step = 3;
	}

	private static Player getPlayer() {
		return Game.player != null ? Game.player : null;
	}

	private static boolean isMoving() {
		Player p = getPlayer();
		return p != null && (p.right || p.left || p.up || p.down);
	}

	private static boolean isShootPressed() {
		Player p = getPlayer();
		return p != null && (p.shoot || p.mouseShoot);
	}

	/** Chamado pelo Game a cada frame durante o onboarding para acompanhar o jogador. */
	public static void update() {
		if (!active) {
			return;
		}
		skipFrames++;

		if (step == 0) {
			boolean moving = isMoving();
			if (moving) {
				moveFrames++;
				wasMoving = true;
			} else if (wasMoving && moveFrames > 0) {
				// Parou: continua contando mas não reseta (ensino, não punição).
				moveFrames = Math.min(moveFrames + 1, MOVE_REQUIRED_FRAMES);
			}
			if (moveFrames >= MOVE_REQUIRED_FRAMES) {
				step = 1;
			}
		} else if (step == 1) {
			// O flag de tiro é consumido no mesmo frame pelo Player.update,
			// então a contagem vem de eventos de pressão (notifyShotFired).
		} else if (step == 2) {
			// Conta por dash iniciado; notifyDashFinished garante a conclusão.
			if (DashAbility.isDashing() && !wasDashing) {
				dashesDone++;
				if (dashesDone >= DASHES_REQUIRED) {
					finish();
				}
			}
			wasDashing = DashAbility.isDashing();
		}
	}

	/**
	 * Registra um tiro disparado (chamado pelo Game quando X/clique é pressionado).
	 * A contagem vem de eventos de pressão porque o flag de tiro é consumido no
	 * mesmo frame pelo Player.update(), antes do OnboardingManager enxergá-lo.
	 */
	public static void notifyShotFired() {
		if (active && step == 1) {
			shotsDone++;
			if (shotsDone >= SHOTS_REQUIRED) {
				step = 2;
			}
		}
	}

	/**
	 * Registra um dash concluído (chamado por DashAbility quando o dash termina).
	 * Como o update() já detecta o início de cada dash, o evento de conclusão só
	 * conta quando o update() porventura não enxergou o dash (dash muito rápido).
	 */
	public static void notifyDashFinished() {
		if (active && step == 2) {
			if (!wasDashing && dashesDone < DASHES_REQUIRED) {
				dashesDone++;
				if (dashesDone >= DASHES_REQUIRED) {
					finish();
				}
			}
			wasDashing = false;
		}
	}



	/**
	 * Inimigos em onboarding ficam paralisados: ainda existem no mundo (o
	 * jogador pode vê-los e se preparar), mas não atualizam IA nem atiram.
	 */
	public static boolean isEnemyPaused() {
		return active;
	}

	private static String currentInstruction() {
		switch (step) {
		case 0:
			return "Segure WASD ou as setas para se mover";
		case 1:
			return "Pressione X (ou clique) para atirar";
		case 2:
			return "Segure SHIFT para o dash";
		default:
			return "";
		}
	}

	private static String progressHint() {
		switch (step) {
		case 0:
			return "Movimento: quase lá...";
		case 1:
			return "Tiros: " + shotsDone + "/" + SHOTS_REQUIRED;
		case 2:
			return "Dash: " + dashesDone + "/" + DASHES_REQUIRED;
		default:
			return "";
		}
	}

	public static void render(Graphics g) {
		if (!active) {
			return;
		}
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;

		// Painel central escuro com a instrução
		String title = "Bem-vindo, piloto!";
		String instruction = currentInstruction();
		String hint = progressHint();
		String skipLabel = "Space para pular";

		Font titleFont = new Font("arial", Font.BOLD, 26 * Game.SCALE / 4);
		Font bodyFont = new Font("arial", Font.PLAIN, 18 * Game.SCALE / 4);
		Font smallFont = new Font("arial", Font.PLAIN, 14 * Game.SCALE / 4);

		g.setFont(titleFont);
		int panelWidth = Math.max(screenWidth * 2 / 3, 420);
		int panelHeight = 180 * Game.SCALE / 4;
		int panelX = (screenWidth - panelWidth) / 2;
		int panelY = 40 * Game.SCALE / 4;

		g.setColor(new Color(0, 0, 0, 235));
		g.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);
		g.setColor(new Color(255, 235, 59));
		g.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 16, 16);

		g.setColor(new Color(255, 235, 59));
		int titleW = g.getFontMetrics().stringWidth(title);
		g.drawString(title, panelX + (panelWidth - titleW) / 2, panelY + 40 * Game.SCALE / 4);

		g.setFont(bodyFont);
		g.setColor(Color.WHITE);
		int instW = g.getFontMetrics().stringWidth(instruction);
		g.drawString(instruction, panelX + (panelWidth - instW) / 2, panelY + 90 * Game.SCALE / 4);

		g.setFont(smallFont);
		g.setColor(new Color(176, 190, 197));
		int hintW = g.getFontMetrics().stringWidth(hint);
		g.drawString(hint, panelX + (panelWidth - hintW) / 2, panelY + 130 * Game.SCALE / 4);

		// Dica de pular piscando
		if ((skipFrames / 30) % 2 == 0) {
			int skipW = g.getFontMetrics().stringWidth(skipLabel);
			g.drawString(skipLabel, panelX + (panelWidth - skipW) / 2, panelY + panelHeight - 12);
		}
	}
}
