package com.traduvertgames.graficos;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.entities.RegionalNpcs;
import com.traduvertgames.world.DungeonManager;
import com.traduvertgames.world.DynamicEventManager;
import com.traduvertgames.world.RpgWorldManager;
import com.traduvertgames.world.RegionalProgressionManager;
import com.traduvertgames.world.RegionalChainManager;

/**
 * Hub de atividades do mundo RPG. O painel congela a exploração sem criar uma
 * segunda instância de mapa: ao confirmar uma atividade, ele devolve o foco ao
 * jogo e deixa a atividade escolhida alterar apenas o estado apropriado.
 */
public final class HubScreen {

    private enum Activity {
		MAIN_MISSION("Próximo passo da cadeia", "Seguir a trilha regional: resgate, NPC, comboio e dungeon"),
		CONTRACTS("Quadro de contratos", "Escolher uma oferta com bônus e modificador regional"),
		SIDE_QUEST("Missão do NPC regional", "Aceitar a tarefa do representante da região"),
		DYNAMIC_EVENT("Evento regional", "Escolher emboscada, caça, resgate ou comboio"),
        DUNGEON("Masmorra opcional", "Entrar na instância e enfrentar o chefe regional"),
        FREE_ROAM("Exploração livre", "Sair do hub e procurar recursos, POIs e eventos"),
        CLOSE("Fechar hub", "Voltar à exploração sem iniciar atividade");

        private final String title;
        private final String description;

        Activity(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    private static boolean open;
    private static int selection;
    private static String feedback = "";
    private static int feedbackFrames;

    private HubScreen() {
    }

    public static boolean isOpen() {
        return open;
    }

    /** Abre o hub somente na superfície do mundo RPG. */
    public static boolean open() {
        if (!RpgWorldManager.isActive() || RpgWorldManager.isDungeonMode()
                || Game.player == null || RpgWorldManager.getCurrentRegion() == null) {
            return false;
        }
        open = true;
        selection = 0;
        feedback = "";
        feedbackFrames = 0;
        Game.clearRegionalHubTransition();
        Game.gameState = "REGIONAL_HUB";
        return true;
    }

    public static void close() {
        open = false;
        feedback = "";
        feedbackFrames = 0;
        if ("REGIONAL_HUB".equals(Game.gameState)) {
            Game.gameState = "NORMAL";
        }
    }

    public static void reset() {
        open = false;
        selection = 0;
        feedback = "";
        feedbackFrames = 0;
    }

    public static void navigateUp() {
        if (!open) {
            return;
        }
        selection = (selection + Activity.values().length - 1) % Activity.values().length;
    }

    public static void navigateDown() {
        if (!open) {
            return;
        }
        selection = (selection + 1) % Activity.values().length;
    }

    public static void cancel() {
        if (open) {
            close();
        }
    }

    public static void confirm() {
        if (!open) {
            return;
        }
        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        Activity activity = Activity.values()[selection];
        if (activity == Activity.CLOSE || activity == Activity.FREE_ROAM) {
            MissionBanner.show("EXPLORAÇÃO LIVRE", "Procure bolsões de mobs, POIs e eventos na região.",
                    new Color(129, 199, 132), Color.WHITE, 120);
            close();
            return;
        }
		if (activity == Activity.CONTRACTS) {
			close();
			ContractBoardScreen.open(region);
			return;
		}
		if (activity == Activity.MAIN_MISSION) {
			if (RegionalChainManager.startNextStep(region)) {
				close();
				return;
			}
			MissionBanner.show("MISSÃO PRINCIPAL", QuestManager.getObjectiveTitle(),
					new Color(255, 235, 59), Color.WHITE, 150);
			close();
			return;
		}
        if (activity == Activity.SIDE_QUEST) {
            RegionalNpcs.activateQuestForRegion(region);
            MissionBanner.show("MISSÃO REGIONAL", RegionalNpcs.getQuestTitleForRegion(region),
                    new Color(129, 199, 132), Color.WHITE, 150);
            close();
            return;
        }
        if (activity == Activity.DYNAMIC_EVENT) {
            if (!DynamicEventManager.startEventForCurrentRegion(null)) {
                setFeedback("Nenhum evento regional está disponível neste ciclo.");
                return;
            }
            close();
            return;
        }
        if (activity == Activity.DUNGEON) {
            if (DungeonManager.isRegionCompleted(region)) {
                setFeedback("Masmorra concluída. Você ainda pode explorar a região.");
                return;
            }
            DungeonManager.requestEnter(region);
            MissionBanner.show("MASMORRA REGIONAL", "Entrada confirmada: prepare-se para o chefe exclusivo.",
                    new Color(220, 80, 255), Color.WHITE, 150);
            close();
        }
    }

    public static void update() {
        if (!open) {
            return;
        }
        if (feedbackFrames > 0) {
            feedbackFrames--;
        }
    }

    private static void setFeedback(String message) {
        feedback = message == null ? "" : message;
        feedbackFrames = 180;
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
        g2.setColor(new Color(3, 8, 16, 220));
        g2.fillRect(0, 0, width, height);

        int panelWidth = Math.min(width - 24 * unit, 310 * unit);
		int panelHeight = Math.min(height - 20 * unit, 196 * unit);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        g2.setColor(new Color(15, 27, 43, 248));
        g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 12 * unit, 12 * unit);
        g2.setColor(new Color(91, 150, 190));
        g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 12 * unit, 12 * unit);

        RpgWorldManager.RegionType region = RpgWorldManager.getCurrentRegion();
        String regionName = region == null ? "Região desconhecida" : region.getDisplayName();
        g2.setColor(new Color(129, 199, 132));
        g2.setFont(new Font("Arial", Font.BOLD, 18 * unit));
        drawCentered(g2, "HUB REGIONAL", panelX, panelWidth, panelY + 25 * unit);
		g2.setColor(Color.WHITE);
		g2.setFont(new Font("Arial", Font.PLAIN, 11 * unit));
		drawCentered(g2, regionName, panelX, panelWidth, panelY + 41 * unit);
		g2.setColor(new Color(190, 220, 190));
		g2.setFont(new Font("Arial", Font.PLAIN, 8 * unit));
			String regionSummary = RegionalProgressionManager.getSummary(region) + " | "
					+ RegionalChainManager.getProgressLabel(region);
		while (regionSummary.length() > 58 && g2.getFontMetrics().stringWidth(regionSummary) > panelWidth - 24 * unit) {
			regionSummary = regionSummary.substring(0, regionSummary.length() - 4) + "...";
		}
		drawCentered(g2, regionSummary, panelX, panelWidth, panelY + 53 * unit);

		Activity[] activities = Activity.values();
		int listY = panelY + 68 * unit;
        for (int i = 0; i < activities.length; i++) {
            Activity activity = activities[i];
            int rowY = listY + i * 21 * unit;
            if (i == selection) {
                g2.setColor(new Color(54, 91, 120));
                g2.fillRoundRect(panelX + 12 * unit, rowY - 14 * unit, panelWidth - 24 * unit, 18 * unit,
                        6 * unit, 6 * unit);
            }
            g2.setColor(i == selection ? Color.WHITE : new Color(191, 205, 214));
            g2.setFont(new Font("Arial", i == selection ? Font.BOLD : Font.PLAIN, 11 * unit));
            g2.drawString((i == selection ? "> " : "  ") + activity.title, panelX + 20 * unit, rowY);
        }

        Activity selected = activities[selection];
        g2.setColor(new Color(175, 194, 205));
        g2.setFont(new Font("Arial", Font.PLAIN, 9 * unit));
        drawWrapped(g2, selected.description, panelX + 20 * unit, panelY + panelHeight - 27 * unit,
                panelWidth - 40 * unit, 12 * unit);
        g2.setColor(new Color(129, 199, 132));
        g2.setFont(new Font("Arial", Font.PLAIN, 8 * unit));
        g2.drawString("Setas/W-S: navegar   ENTER: confirmar   ESC: fechar", panelX + 20 * unit,
                panelY + panelHeight - 9 * unit);
        if (feedbackFrames > 0 && !feedback.isEmpty()) {
            g2.setColor(new Color(255, 235, 59));
            g2.setFont(new Font("Arial", Font.BOLD, 9 * unit));
            drawCentered(g2, feedback, panelX, panelWidth, panelY + panelHeight + 15 * unit);
        }
        g2.dispose();
    }

    private static void drawCentered(Graphics2D g, String text, int x, int width, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, x + (width - metrics.stringWidth(text)) / 2, baseline);
    }

    private static void drawWrapped(Graphics2D g, String text, int x, int baseline, int maxWidth, int lineHeight) {
        String[] words = text.split(" ");
        String line = "";
        int y = baseline;
        for (String word : words) {
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
