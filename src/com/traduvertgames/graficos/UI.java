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

        private static final int BAR_WIDTH = 110;
        private static final int BAR_HEIGHT = 9;
        private static final int LINE_SPACING = 11;

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
                int arsenalWidth = Math.min(screenWidth - margin * 2, 480);
                int arsenalHeight = 158;

                drawStatusCard(g2, margin, margin, statusWidth, 188);
                drawScoreCard(g2, screenWidth - scoreWidth - margin, margin, scoreWidth, 188);
                int arsenalX = (screenWidth - arsenalWidth) / 2;
                drawArsenalCard(g2, arsenalX, screenHeight - arsenalHeight - margin, arsenalWidth, arsenalHeight);

                drawXpHud(g2, screenWidth);
                drawAbilityHud(g2, screenWidth);
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
        private void drawAbilityHud(Graphics2D g2, int screenWidth) {
                int y = 44;
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));

                double ultimateReady = UltimateAbility.getReadyPercentage();
                g2.setColor(ultimateReady >= 1 ? new Color(130, 230, 230) : new Color(120, 120, 120));
                String ultimateLabel = "[F] Ultimate " + (ultimateReady >= 1 ? "PRONTO" : String.format("%d%%", (int) (ultimateReady * 100)));
                g2.drawString(ultimateLabel, 18, y);

                double dashReady = DashAbility.getReadyPercentage();
                g2.setColor(dashReady >= 1 ? new Color(130, 230, 230) : new Color(120, 120, 120));
                String dashLabel = "[SHIFT] Dash " + (dashReady >= 1 ? "PRONTO" : String.format("%d%%", (int) (dashReady * 100)));
                g2.drawString(dashLabel, 18, y + 16);

                if (WaveManager.isArenaMode()) {
                        g2.setColor(new Color(255, 152, 0));
                        String arenaLabel = "ARENA — Onda " + WaveManager.getArenaWave();
                        int metricsWidth = g2.getFontMetrics().stringWidth(arenaLabel);
                        g2.drawString(arenaLabel, screenWidth - metricsWidth - 18, y);
                }
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

                textY += 30;
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                g2.drawString("Missão atual", x + 20, textY);
                textY += 22;
                g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                textY = drawParagraph(g2, QuestManager.getObjectiveTitle(), x + 20, textY, width - 40, 18,
                                Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
                textY = drawParagraph(g2, QuestManager.getObjectiveDescription(), x + 20, textY, width - 40, 18,
                                new Color(210, 210, 210));
                g2.setFont(new Font("SansSerif", Font.BOLD, 15));
                textY = drawParagraph(g2, QuestManager.getObjectiveProgress(), x + 20, textY, width - 40, 18,
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

        private void drawOverlayHint(Graphics2D g2) {
                int screenWidth = Game.WIDTH * Game.SCALE;
                int screenHeight = Game.HEIGHT * Game.SCALE;
                int padding = 10;

                String title = "TAB: painel tático";
                String hint = "";

                Font hintFont = new Font("SansSerif", Font.BOLD, 12);

                FontMetrics hintMetrics = g2.getFontMetrics(hintFont);

                int rectWidth = hintMetrics.stringWidth(title) + 20;
                int rectHeight = hintMetrics.getHeight() + 14;

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

                // Habilidades ficam visíveis mesmo com o painel minimizado.
                drawXpHud(g2, screenWidth);
                drawAbilityHud(g2, screenWidth);
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
                if (max > 0) {
                        String value = String.format("%.0f/%.0f", current, max);
                        FontMetrics metrics = g2.getFontMetrics();
                        int valueX = x + BAR_WIDTH - metrics.stringWidth(value) - 3;
                        g2.drawString(value, valueX, y + BAR_HEIGHT - 2);
                }
        }
}
