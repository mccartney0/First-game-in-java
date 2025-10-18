package com.traduvertgames.graficos;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Player;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.main.Game;

public class UI {

        private static final int BAR_WIDTH = 96;
        private static final int BAR_HEIGHT = 9;

        public void render(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int panelX = 6;
                int panelY = 6;
                int panelWidth = 116;
                int panelHeight = 54;
                g2.setColor(new Color(8, 12, 20, 190));
                g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 10, 10);

                g2.setFont(new Font("SansSerif", Font.BOLD, 8));
                g2.setColor(Color.WHITE);

                drawResourceBar(g2, "VIDA", Player.life, Player.maxLife, panelX + 8, panelY + 12, new Color(244, 67, 54));
                drawResourceBar(g2, "MANA", Player.mana, Player.maxMana, panelX + 8, panelY + 24,
                                new Color(33, 150, 243));

                WeaponType currentWeapon = Game.player != null && Game.player.getCurrentWeaponType() != null
                                ? Game.player.getCurrentWeaponType()
                                : WeaponType.BLASTER;
                drawResourceBar(g2, currentWeapon.getDisplayName().toUpperCase(), Player.weapon, Player.maxWeapon,
                                panelX + 8, panelY + 36, currentWeapon.getUiColor());
        }

        public void renderOverlay(Graphics2D g2) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int screenWidth = Game.WIDTH * Game.SCALE;
                int screenHeight = Game.HEIGHT * Game.SCALE;

                drawStatusCard(g2, 20, 20, 280, 156);
                drawScoreCard(g2, screenWidth - 260 - 20, 20, 260, 180);
                drawArsenalCard(g2, 20, screenHeight - 220, 340, 200);
        }

        private void drawStatusCard(Graphics2D g2, int x, int y, int width, int height) {
                g2.setColor(new Color(8, 12, 20, 210));
                g2.fillRoundRect(x, y, width, height, 24, 24);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, width, height, 24, 24);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 24));
                g2.drawString("Status do piloto", x + 22, y + 34);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
                int textY = y + 64;
                g2.drawString(String.format("Vida: %.0f / %.0f", Player.life, Player.maxLife), x + 24, textY);
                textY += 26;
                g2.drawString(String.format("Mana: %.0f / %.0f", Player.mana, Player.maxMana), x + 24, textY);
                textY += 26;

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

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                if (Game.getComboMultiplier() > 1) {
                        g2.drawString(String.format("Combo ativo: x%d", Game.getComboMultiplier()), x + 24, textY);
                        textY += 22;
                        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
                        g2.setColor(new Color(255, 193, 7));
                        g2.drawString(String.format("Tempo restante: %ds", Game.getComboSecondsRemaining()), x + 24,
                                        textY);
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
                g2.setFont(new Font("SansSerif", Font.BOLD, 24));
                g2.drawString("Painel de missão", x + 20, y + 34);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
                int textY = y + 64;
                g2.drawString(String.format("Pontuação: %d", Game.getScore()), x + 20, textY);
                textY += 26;
                g2.drawString(String.format("Recorde: %d", Game.getHighScore()), x + 20, textY);
                textY += 26;
                g2.drawString(String.format("Inimigos vivos: %d", Game.enemies.size()), x + 20, textY);
                textY += 26;
                g2.drawString(String.format("Eliminados: %d", Enemy.enemies), x + 20, textY);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
                g2.setColor(new Color(200, 200, 200));
                textY += 28;
                g2.drawString("Aperte T para salvar rapidamente", x + 20, textY);
        }

        private void drawArsenalCard(Graphics2D g2, int x, int y, int width, int height) {
                g2.setColor(new Color(8, 12, 20, 205));
                g2.fillRoundRect(x, y, width, height, 24, 24);
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x, y, width, height, 24, 24);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 22));
                g2.drawString("Arsenal tático", x + 22, y + 32);

                Player player = Game.player;
                WeaponType currentWeapon = player != null ? player.getCurrentWeaponType() : WeaponType.BLASTER;

                g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
                int lineY = y + 60;
                for (WeaponType type : WeaponType.values()) {
                        boolean unlocked = player != null && player.hasWeaponUnlocked(type);
                        double percent = player != null ? player.getWeaponEnergyPercentage(type) : 0;
                        int percentage = (int) Math.round(percent * 100);
                        Color accent = type.getUiColor();
                        if (type == currentWeapon) {
                                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));
                                g2.fillRoundRect(x + 16, lineY - 18, width - 32, 28, 14, 14);
                                g2.setColor(Color.WHITE);
                        } else {
                                g2.setColor(new Color(220, 220, 220));
                        }

                        String status = unlocked ? String.format("%d%% de energia", percentage) : "Bloqueada";
                        g2.drawString(String.format("%s — %s", type.getDisplayName(), status), x + 26, lineY);
                        lineY += 26;
                }

                g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
                g2.setColor(new Color(200, 200, 200));
                lineY += 12;
                g2.drawString("Q/E alternam armas • 1-4 selecionam diretamente", x + 22, lineY);
        }

        private void drawResourceBar(Graphics2D g2, String label, double current, double max, int x, int y,
                        Color fillColor) {
                double percent = max <= 0 ? 0 : current / max;
                percent = Math.max(0, Math.min(1, percent));
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(x, y, BAR_WIDTH, BAR_HEIGHT, 6, 6);
                g2.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 210));
                g2.fillRoundRect(x, y, (int) (BAR_WIDTH * percent), BAR_HEIGHT, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 7));
                String text = String.format("%s", label);
                g2.drawString(text, x + 2, y - 2);
        }
}
