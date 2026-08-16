package com.traduvertgames.graficos;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import com.traduvertgames.entities.DashAbility;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.UltimateAbility;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.LevelUpManager;
import com.traduvertgames.main.WaveManager;
import com.traduvertgames.quest.QuestManager;

public class UI {

	private static final int BAR_WIDTH = 80;
	private static final int BAR_HEIGHT = 9;
	private static final int LINE_SPACING = 9;

	public void render(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Painel pequeno no canto inferior esquerdo: não cobre a área de jogo.
		// Coordenadas no espaço do buffer de renderização (384x216), escalado depois pela janela.
		int margin = 3;
		int panelX = margin;
		int panelHeight = 4 * LINE_SPACING + 6;
		int panelY = Game.HEIGHT - panelHeight - margin;
		int panelWidth = BAR_WIDTH + 16;
		g2.setColor(new Color(6, 9, 16, 150));
		g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 4, 4);

		g2.setFont(new Font("SansSerif", Font.BOLD, 7));

		int barX = panelX + 7;
		int barY = panelY + 5;
		drawResourceBar(g2, "VIDA", Player.life, Player.maxLife, barX, barY, new Color(244, 67, 54));
		barY += LINE_SPACING;
		drawResourceBar(g2, "ESCUDO", Player.shield, Player.maxShield, barX, barY,
				new Color(121, 134, 203));
		barY += LINE_SPACING;
		drawResourceBar(g2, "MANA", Player.mana, Player.maxMana, barX, barY, new Color(33, 150, 243));
		barY += LINE_SPACING;

		WeaponType currentWeapon = Game.player != null && Game.player.getCurrentWeaponType() != null
				? Game.player.getCurrentWeaponType()
				: WeaponType.BLASTER;
		drawResourceBar(g2, currentWeapon.getShortName().toUpperCase(), Player.weapon, Player.maxWeapon, barX,
				barY, currentWeapon.getUiColor());
	}

	public void renderOverlay(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// No menu o jogador ainda não tem valores de jogo; desenhar a HUD aqui
		// mostraria "VIDA 0/100" por baixo dos painéis. A HUD só aparece durante
		// a partida (NORMAL, SHOP, LEVELUP, LEVELSELECT).
		if ("MENU".equals(Game.gameState) || "GAMEOVER".equals(Game.gameState)) {
			return;
		}

		// HUD compacta desenhada sobre tudo (inclusive o overlay escuro da loja),
		// em coordenadas de tela cheia (com SCALE), evitando HUD esmaecida no fundo.
		drawResourceHudScaled(g2);

		if (!Game.isOverlayExpanded()) {
			drawOverlayHint(g2);
			return;
		}

		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;
		int margin = 20;
		// Painel expandido menor: colunas estreitas que não cobrem o campo de batalha.
		int statusWidth = 264;
		int scoreWidth = 264;
		int statusHeight = 200;
		int arsenalWidth = Math.min(screenWidth - margin * 2, 480);
		int arsenalHeight = 158;

		// Status (Piloto) no topo esquerdo e Missão no topo direito, sem colisão.
		drawStatusCard(g2, margin, margin, statusWidth, statusHeight);
		drawScoreCard(g2, screenWidth - scoreWidth - margin, margin, scoreWidth, statusHeight);

		int arsenalX = (screenWidth - arsenalWidth) / 2;
		// Arsenal centralizado no rodapé — a HUD compacta agora ocupa bem menos
		// altura, então não há mais sobreposição entre os dois no canto esquerdo.
		drawArsenalCard(g2, arsenalX, screenHeight - arsenalHeight - margin, arsenalWidth, arsenalHeight);

		// XP no topo central, habilidades (ultimate/dash) logo abaixo do painel de
		// Missão (topo direito), arena mode no topo direito logo abaixo das habilidades.
		drawXpHud(g2, screenWidth);
		drawAbilityHud(g2, screenWidth - scoreWidth - margin, margin + statusHeight + 16);
	}

	/** Barra de XP e nível sempre visíveis no topo. */
	private void drawXpHud(Graphics2D g2, int screenWidth) {
		if (!LevelUpManager.isEnabled()) {
			return;
		}
		int barWidth = Math.min(360, screenWidth - 40);
		int barHeight = 10;
		int barX = (screenWidth - barWidth) / 2;
		int barY = 12;
		double xp = LevelUpManager.getXp();
		double needed = LevelUpManager.xpForNextLevel();
		double ratio = needed > 0 ? Math.min(1, xp / needed) : 0;
		int level = LevelUpManager.getPlayerLevel();

		g2.setColor(new Color(0, 0, 0, 150));
		g2.fillRoundRect(barX - 30, barY - 12, barWidth + 60, 26, 12, 12);
		g2.setColor(new Color(60, 64, 74));
		g2.fillRoundRect(barX, barY, barWidth, barHeight, 5, 5);
		g2.setColor(new Color(255, 214, 0));
		if (ratio > 0) {
			g2.fillRoundRect(barX, barY, (int) (barWidth * ratio), barHeight, 5, 5);
		}
		g2.setFont(new Font("SansSerif", Font.BOLD, 11));
		g2.setColor(Color.WHITE);
		String label = "Nível " + level + " — XP: " + (int) xp + "/" + (int) needed;
		g2.drawString(label, barX + 6, barY + barHeight + 3);
	}

	/** Indicadores das habilidades: ultimate (F) e dash (Shift). */
	private void drawAbilityHud(Graphics2D g2, int baseX, int baseY) {
		g2.setFont(new Font("SansSerif", Font.BOLD, 12));

		double ultimateReady = UltimateAbility.getReadyPercentage();
		g2.setColor(ultimateReady >= 1 ? new Color(130, 230, 230) : new Color(120, 120, 120));
		String ultimateLabel = "[F] Ultimate " + (ultimateReady >= 1 ? "PRONTO" : String.format("%d%%", (int) (ultimateReady * 100)));
		g2.drawString(ultimateLabel, baseX, baseY);

		double dashReady = DashAbility.getReadyPercentage();
		g2.setColor(dashReady >= 1 ? new Color(130, 230, 230) : new Color(120, 120, 120));
		String dashLabel = "[SHIFT] Dash " + (dashReady >= 1 ? "PRONTO" : String.format("%d%%", (int) (dashReady * 100)));
		g2.drawString(dashLabel, baseX, baseY + 16);

		if (WaveManager.isArenaMode()) {
			g2.setColor(new Color(255, 152, 0));
			String arenaLabel = "ARENA — Onda " + WaveManager.getArenaWave();
			int metricsWidth = g2.getFontMetrics().stringWidth(arenaLabel);
			g2.drawString(arenaLabel, baseX, baseY + 32);
		}
	}

	/** HUD compacta em coordenadas de tela cheia (buffer * SCALE), em tamanho reduzido. */
	private void drawResourceHudScaled(Graphics2D g2) {
		int s = Game.SCALE;
		// Escala fixa menor para a HUD: mesmo em janelas 4x/5x, a HUD permanece
		// compacta e não cobre a área de jogo (altura ~95px em vez de ~185px).
		int h = Math.min(s, 3);
		int screenWidth = Game.WIDTH * s;
		int screenHeight = Game.HEIGHT * s;
		int margin = 6;
		int panelHeight = (4 * LINE_SPACING + 6) * h;
		int panelY = screenHeight - panelHeight - margin;
		int panelX = margin;
		int panelWidth = (BAR_WIDTH + 16) * h;
		g2.setColor(new Color(6, 9, 16, 150));
		g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 8, 8);
		g2.setFont(new Font("SansSerif", Font.BOLD, 6 * h));
		int barX = panelX + 10 * h;
		int barY = panelY + 8 * h;
		int barSpacing = LINE_SPACING * h;
		int barWidth = panelWidth - 24 * h;
		int barHeight = 8 * h;
		drawScaledBar(g2, "VIDA", Player.life, Player.maxLife, barX, barY, barWidth, barHeight,
				new Color(244, 67, 54));
		barY += barSpacing;
		drawScaledBar(g2, "ESCUDO", Player.shield, Player.maxShield, barX, barY, barWidth, barHeight,
				new Color(121, 134, 203));
		barY += barSpacing;
		drawScaledBar(g2, "MANA", Player.mana, Player.maxMana, barX, barY, barWidth, barHeight,
				new Color(33, 150, 243));
		barY += barSpacing;
		WeaponType currentWeapon = Game.player != null && Game.player.getCurrentWeaponType() != null
				? Game.player.getCurrentWeaponType()
				: WeaponType.BLASTER;
		drawScaledBar(g2, currentWeapon.getShortName().toUpperCase(), Player.weapon, Player.maxWeapon, barX,
				barY, barWidth, barHeight, currentWeapon.getUiColor());
	}

	private void drawScaledBar(Graphics2D g2, String label, double value, double max, int x, int y,
			int barWidth, int barHeight, Color color) {
		double ratio = max > 0 ? Math.min(1, Math.max(0, value / max)) : 0;
		int fontSize = Math.max(12, 6 * Game.SCALE);
		g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
		FontMetrics fm = g2.getFontMetrics();
		String valueText = (int) value + "/" + (int) max;
		int valWidth = fm.stringWidth(valueText);
		// Fundo da barra
		g2.setColor(new Color(20, 24, 34));
		g2.fillRoundRect(x, y, barWidth, barHeight, 3 * Game.SCALE, 3 * Game.SCALE);
		// Preenchimento colorido
		g2.setColor(color);
		if (ratio > 0) {
			g2.fillRoundRect(x, y, (int) (barWidth * ratio), barHeight, 3 * Game.SCALE, 3 * Game.SCALE);
		}
		// Rótulo/valor por cima do preenchimento (texto sempre legível)
		g2.setColor(Color.WHITE);
		g2.drawString(label, x + 4 * Game.SCALE, y + barHeight - 2 * Game.SCALE);
		g2.drawString(valueText, x + barWidth - valWidth - 2 * Game.SCALE, y + barHeight - 2 * Game.SCALE);
	}

	private void drawOverlayHint(Graphics2D g2) {
		int screenWidth = Game.WIDTH * Game.SCALE;
		int screenHeight = Game.HEIGHT * Game.SCALE;
		int padding = 10;

		String title = "TAB: painel tático";
		String hint = "F11: tela cheia";

		Font hintFont = new Font("SansSerif", Font.BOLD, 12);

		FontMetrics hintMetrics = g2.getFontMetrics(hintFont);
		FontMetrics titleMetrics = g2.getFontMetrics(hintFont);

		int lineWidth = Math.max(titleMetrics.stringWidth(title), hintMetrics.stringWidth(hint));
		int rectWidth = lineWidth + 20;
		int rectHeight = (hintMetrics.getHeight() + 14) * 2;

		int x = screenWidth - rectWidth - padding;
		int y = screenHeight - rectHeight - padding;

		g2.setColor(new Color(8, 12, 20, 150));
		g2.fillRoundRect(x, y, rectWidth, rectHeight, 10, 10);
		g2.setColor(new Color(255, 255, 255, 60));
		g2.setStroke(new BasicStroke(2f));
		g2.drawRoundRect(x, y, rectWidth, rectHeight, 16, 16);

		int textX = x + 10;
		int textY = y + 10 + hintMetrics.getAscent();

		g2.setFont(hintFont);
		g2.setColor(new Color(230, 230, 230));
		g2.drawString(title, textX, textY);
		g2.setColor(new Color(200, 200, 200));
		g2.drawString(hint, textX, textY + hintMetrics.getHeight() + 4);

		// Habilidades ficam visíveis mesmo com o painel minimizado.
		drawXpHud(g2, screenWidth);
		drawAbilityHud(g2, 18, 44);
	}

	private int drawParagraph(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight, Color color) {
		if (text == null || text.isEmpty()) {
			return y;
		}
		g2.setColor(color);
		FontMetrics metrics = g2.getFontMetrics();
		String[] words = text.split(" ");
		StringBuilder line = new StringBuilder();
		for (String word : words) {
			if (word == null || word.isEmpty()) {
				continue;
			}
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (metrics.stringWidth(candidate) > maxWidth && line.length() > 0) {
				g2.drawString(line.toString(), x, y);
				y += lineHeight;
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if (line.length() > 0) {
			g2.drawString(line.toString(), x, y);
			y += lineHeight;
		}
		return y;
	}

	private void drawResourceBar(Graphics2D g2, String label, double current, double max, int x, int y,
			Color fillColor) {
		double percent = max <= 0 ? 0 : current / max;
		percent = Math.max(0, Math.min(1, percent));
		g2.setColor(new Color(0, 0, 0, 180));
		g2.fillRoundRect(x, y, BAR_WIDTH, BAR_HEIGHT, 4, 4);
		g2.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 220));
		g2.fillRoundRect(x, y, (int) (BAR_WIDTH * percent), BAR_HEIGHT, 4, 4);
		g2.setFont(new Font("SansSerif", Font.BOLD, 7));
		g2.setColor(Color.WHITE);
		g2.drawString(label, x + 3, y + BAR_HEIGHT - 2);
	}

	private void drawStatusCard(Graphics2D g2, int x, int y, int width, int height) {
		g2.setColor(new Color(8, 12, 20, 210));
		g2.fillRoundRect(x, y, width, height, 24, 24);
		g2.setColor(new Color(255, 255, 255, 60));
		g2.setStroke(new BasicStroke(2f));
		g2.drawRoundRect(x, y, width, height, 24, 24);

		g2.setColor(Color.WHITE);
		g2.setFont(new Font("SansSerif", Font.BOLD, 20));
		g2.drawString("Piloto", x + 22, y + 30);

		g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
		int textY = y + 56;
		g2.drawString(String.format("Vida: %.0f/%.0f", Player.life, Player.maxLife), x + 24, textY);
		textY += 24;
		g2.drawString(String.format("Mana: %.0f/%.0f", Player.mana, Player.maxMana), x + 24, textY);
		textY += 24;
		g2.drawString(String.format("Escudo: %.0f/%.0f", Player.shield, Player.maxShield), x + 24, textY);
		textY += 24;

		WeaponType currentWeapon = Game.player != null && Game.player.getCurrentWeaponType() != null
				? Game.player.getCurrentWeaponType()
				: WeaponType.BLASTER;
		String weaponLabel = String.format("Arma: %s", currentWeapon.getDisplayName());
		g2.drawString(weaponLabel, x + 24, textY);
		textY += 26;
		g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
		g2.setColor(new Color(200, 200, 200));
		String ammoInfo = String.format("Energia: %.0f / %.0f", Player.weapon, Player.maxWeapon);
		g2.drawString(ammoInfo, x + 24, textY);
		textY += 24;
		g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
		g2.setColor(new Color(190, 200, 210));
		textY = drawParagraph(g2, currentWeapon.getDescription(), x + 24, textY, width - 48, 18,
				new Color(190, 200, 210));

		g2.setColor(Color.WHITE);
		g2.setFont(new Font("SansSerif", Font.BOLD, 16));
		if (Game.getComboMultiplier() > 1) {
			g2.drawString(String.format("Combo: x%d", Game.getComboMultiplier()), x + 24, textY);
		} else {
			g2.drawString(String.format("Melhor combo: x%d", Game.getBestComboThisRun()), x + 24, textY);
		}
	}

	private void drawScoreCard(Graphics2D g2, int x, int y, int width, int height) {
		g2.setColor(new Color(8, 12, 20, 210));
		g2.fillRoundRect(x, y, width, height, 24, 24);
		g2.setColor(new Color(255, 255, 255, 60));
		g2.setStroke(new BasicStroke(2f));
		g2.drawRoundRect(x, y, width, height, 24, 24);

		g2.setColor(Color.WHITE);
		g2.setFont(new Font("SansSerif", Font.BOLD, 20));
		g2.drawString("Missão", x + 20, y + 30);

		g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
		int textY = y + 56;
		g2.drawString(String.format("Pontuação: %d", Game.getScore()), x + 20, textY);
		textY += 24;
		g2.drawString(String.format("Recorde: %d", Game.getHighScore()), x + 20, textY);
		textY += 24;
		g2.drawString(String.format("Eliminados: %d", Enemy.enemies), x + 20, textY);

		textY += 28;
		g2.setColor(Color.WHITE);
		g2.setFont(new Font("SansSerif", Font.BOLD, 16));
		g2.drawString("Missão atual", x + 20, textY);
		textY += 20;
		g2.setFont(new Font("SansSerif", Font.BOLD, 15));
		textY = drawParagraph(g2, QuestManager.getObjectiveTitle(), x + 20, textY, width - 40, 17,
			Color.WHITE);
		g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
		textY = drawParagraph(g2, QuestManager.getObjectiveDescription(), x + 20, textY, width - 40, 17,
			new Color(210, 210, 210));
		g2.setFont(new Font("SansSerif", Font.BOLD, 14));
		textY = drawParagraph(g2, QuestManager.getObjectiveProgress(), x + 20, textY, width - 40, 17,
			new Color(129, 199, 132));

		g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
		g2.setColor(new Color(200, 200, 200));
		textY += 28;
		g2.drawString("Aperte T para salvar rapidamente", x + 20, textY);
	}

	private void drawArsenalCard(Graphics2D g2, int x, int y, int width, int height) {
		g2.setColor(new Color(8, 12, 20, 200));
		g2.fillRoundRect(x, y, width, height, 20, 20);
		g2.setColor(new Color(255, 255, 255, 60));
		g2.setStroke(new BasicStroke(2f));
		g2.drawRoundRect(x, y, width, height, 20, 20);

		g2.setColor(Color.WHITE);
		g2.setFont(new Font("SansSerif", Font.BOLD, 18));
		g2.drawString("Arsenal", x + 20, y + 28);

		Player player = Game.player;
		WeaponType currentWeapon = player != null ? player.getCurrentWeaponType() : WeaponType.BLASTER;

		// Grade em duas colunas para caber todas as armas em menos altura.
		g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
		int cols = 2;
		int cellWidth = (width - 40) / cols;
		int lineY = y + 48;
		int col = 0;
		for (WeaponType type : WeaponType.values()) {
			boolean unlocked = player != null && player.hasWeaponUnlocked(type);
			double percent = player != null ? player.getWeaponEnergyPercentage(type) : 0;
			int percentage = (int) Math.round(percent * 100);
			Color accent = type.getUiColor();
			int cellX = x + 20 + col * cellWidth;
			if (type == currentWeapon) {
				g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140));
				g2.fillRoundRect(cellX, lineY - 16, cellWidth - 8, 24, 12, 12);
				g2.setColor(Color.WHITE);
			} else {
				g2.setColor(unlocked ? new Color(220, 220, 220) : new Color(160, 160, 160));
			}

			String status = unlocked ? String.format("%d%%", percentage) : "Bloq.";
			g2.drawString(String.format("%s %s", type.getDisplayName(), status), cellX + 6, lineY);
			col++;
			if (col >= cols) {
				col = 0;
				lineY += 24;
			}
		}

		g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
		g2.setColor(new Color(200, 200, 200));
		g2.drawString("Q/E alternam • 1-6 selecionam", x + 20, y + height - 14);
	}
}
