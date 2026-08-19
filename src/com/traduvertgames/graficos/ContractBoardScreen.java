package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import com.traduvertgames.main.Game;
import com.traduvertgames.quest.ContractManager;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.RegionalProgressionManager;

/** Quadro de contratos do hub, com ofertas rotativas e bônus explícitos. */
public final class ContractBoardScreen {

    private static boolean open;
    private static int selection;
    private static String feedback = "";
    private static int feedbackFrames;

    private ContractBoardScreen() {
    }

    public static boolean isOpen() {
        return open;
    }

    public static void open(RpgWorldManager.RegionType region) {
        if (region == null) {
            return;
        }
        ContractManager.open(region);
        open = true;
        selection = 0;
        feedback = "";
        feedbackFrames = 0;
        Game.gameState = "REGIONAL_CONTRACTS";
    }

    public static void closeToHub() {
        open = false;
        feedback = "";
        feedbackFrames = 0;
        if ("REGIONAL_CONTRACTS".equals(Game.gameState)) {
            HubScreen.open();
        }
    }

    public static void closeToExploration() {
        open = false;
        feedback = "";
        feedbackFrames = 0;
        HubScreen.close();
    }

    public static void reset() {
        open = false;
        selection = 0;
        feedback = "";
        feedbackFrames = 0;
    }

    public static void navigateUp() {
        List<ContractManager.Contract> contracts = ContractManager.getOffered();
        if (open && !contracts.isEmpty()) {
            selection = (selection + contracts.size() - 1) % contracts.size();
        }
    }

    public static void navigateDown() {
        List<ContractManager.Contract> contracts = ContractManager.getOffered();
        if (open && !contracts.isEmpty()) {
            selection = (selection + 1) % contracts.size();
        }
    }

    public static void confirm() {
        List<ContractManager.Contract> contracts = ContractManager.getOffered();
        if (!open || contracts.isEmpty() || selection >= contracts.size()) {
            return;
        }
        ContractManager.Contract contract = contracts.get(selection);
        if (ContractManager.isCompleted(contract)) {
            feedback = "Este contrato já foi concluído nesta região.";
            feedbackFrames = 150;
            return;
        }
        if (!ContractManager.accept(selection)) {
            feedback = "Contrato indisponível agora; escolha outra oferta.";
            feedbackFrames = 150;
            return;
        }
        MissionBanner.show("CONTRATO ACEITO", contract.getTitle() + " — objetivo iniciado.",
                new Color(255, 193, 7), Color.WHITE, 150);
        closeToExploration();
    }

    public static void update() {
        if (feedbackFrames > 0) {
            feedbackFrames--;
        }
    }

    public static void render(Graphics g) {
        if (!open || g == null) {
            return;
        }
        int width = g.getClipBounds() != null ? g.getClipBounds().width : Game.WIDTH * Game.SCALE;
        int height = g.getClipBounds() != null ? g.getClipBounds().height : Game.HEIGHT * Game.SCALE;
        int unit = Math.max(1, Game.SCALE / 4);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(3, 8, 16, 225));
        g2.fillRect(0, 0, width, height);
        int panelWidth = Math.min(width - 24 * unit, 330 * unit);
        int panelHeight = Math.min(height - 20 * unit, 190 * unit);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        g2.setColor(new Color(20, 31, 45, 250));
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 12 * unit, 12 * unit);
        g2.setColor(new Color(255, 193, 7));
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 12 * unit, 12 * unit);

        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        g2.setColor(new Color(255, 214, 10));
        g2.setFont(new Font("Arial", Font.BOLD, 18 * unit));
        drawCentered(g2, "QUADRO DE CONTRATOS", panelX, panelWidth, panelY + 25 * unit);
        g2.setColor(new Color(190, 220, 190));
        g2.setFont(new Font("Arial", Font.PLAIN, 8 * unit));
        drawCentered(g2, RegionalProgressionManager.getSummary(region), panelX, panelWidth, panelY + 42 * unit);

        List<ContractManager.Contract> contracts = ContractManager.getOffered();
        int listY = panelY + 64 * unit;
        for (int i = 0; i < contracts.size(); i++) {
            ContractManager.Contract contract = contracts.get(i);
            int rowY = listY + i * 31 * unit;
            if (i == selection) {
                g2.setColor(new Color(72, 91, 116));
                g2.fillRoundRect(panelX + 12 * unit, rowY - 14 * unit, panelWidth - 24 * unit, 26 * unit,
                        6 * unit, 6 * unit);
            }
            g2.setColor(i == selection ? Color.WHITE : new Color(210, 220, 230));
            g2.setFont(new Font("Arial", i == selection ? Font.BOLD : Font.PLAIN, 10 * unit));
            String prefix = i == selection ? "> " : "  ";
            g2.drawString(prefix + contract.getTitle(), panelX + 20 * unit, rowY);
            g2.setColor(new Color(188, 245, 200));
            g2.setFont(new Font("Arial", Font.PLAIN, 8 * unit));
            String status = ContractManager.isCompleted(contract) ? "CONCLUÍDO" : "+" + contract.getRewardBonus() + " créditos";
            g2.drawString(status, panelX + panelWidth - 102 * unit, rowY);
            g2.setColor(new Color(175, 194, 205));
            drawWrapped(g2, contract.getDescription(), panelX + 32 * unit, rowY + 12 * unit,
                    panelWidth - 64 * unit, 9 * unit);
        }
        g2.setColor(new Color(129, 199, 132));
        g2.setFont(new Font("Arial", Font.PLAIN, 8 * unit));
        g2.drawString("Setas/W-S: navegar   ENTER: aceitar   ESC: voltar ao hub",
                panelX + 18 * unit, panelY + panelHeight - 9 * unit);
        if (feedbackFrames > 0 && !feedback.isEmpty()) {
            g2.setColor(new Color(255, 235, 59));
            g2.setFont(new Font("Arial", Font.BOLD, 8 * unit));
            drawCentered(g2, feedback, panelX, panelWidth, panelY + panelHeight + 14 * unit);
        }
        g2.dispose();
    }

    private static void drawCentered(Graphics2D g, String text, int x, int width, int baseline) {
        g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, baseline);
    }

    private static void drawWrapped(Graphics2D g, String text, int x, int baseline, int maxWidth, int lineHeight) {
        String line = "";
        int y = baseline;
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                g.drawString(line, x, y);
                y += lineHeight;
                line = word;
            } else {
                line = candidate;
            }
        }
        if (!line.isEmpty()) {
            g.drawString(line, x, y);
        }
    }
}
