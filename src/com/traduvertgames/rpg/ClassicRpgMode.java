package com.traduvertgames.rpg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.graficos.AssetCatalog;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SaveManager;

/**
 * Vertical slice inicial do RPG Clássico. O modo possui estado, mapa, player,
 * HUD e persistência próprios; a engine de janela e o canvas continuam sendo
 * os da aplicação principal.
 */
public final class ClassicRpgMode {
    private enum QuestStage {
        FIND_GUIDE,
        DEFEAT_WARDEN,
        RETURN_TO_GUIDE,
        COMPLETE
    }

    private final RpgMap map = new RpgMap();
    private RpgPlayerController player;
    private RpgCharacterStats character;
    private boolean active;
    private boolean choosingArchetype;
    private int archetypeSelection;
    private boolean characterSheetOpen;
    private String objective = "Fale com Iara, a guia da vila, perto das casas de Brumafolha.";
    private String notice = "";
    private int noticeFrames;
    private long playedFrames;
    private int staminaRegenFrames;
    private QuestStage questStage = QuestStage.FIND_GUIDE;
    private int wardenLife = 3;
    private int attackFrames;

    public void startNew(Game game) {
        active = true;
        choosingArchetype = true;
        archetypeSelection = 0;
        characterSheetOpen = false;
        playedFrames = 0;
        staminaRegenFrames = 0;
        questStage = QuestStage.FIND_GUIDE;
        wardenLife = 3;
        attackFrames = 0;
        objective = objectiveFor(questStage);
        player = new RpgPlayerController(map);
        character = null;
        notice = "Escolha seu arquétipo para iniciar a jornada";
        noticeFrames = 240;
        Game.gameState = "NORMAL";
        com.traduvertgames.state.GameState.gameState = "NORMAL";
    }

    @SuppressWarnings("unchecked")
    public void load(Game game, Map<String, Object> data) {
        active = true;
        choosingArchetype = false;
        characterSheetOpen = false;
        playedFrames = nonNegativeLong(data == null ? null : data.get("playedFrames"));
        questStage = stageValue(data == null ? null : data.get("questStage"));
        wardenLife = Math.max(0, intValue(data == null ? null : data.get("wardenLife"), 3));
        attackFrames = 0;
        objective = objectiveFor(questStage);
        player = new RpgPlayerController(map);
        character = RpgCharacterStats.deserialize(asMap(data == null ? null : data.get("character")));
        player.deserialize(asMap(data == null ? null : data.get("player")));
        notice = "RPG Clássico restaurado";
        noticeFrames = 150;
        Game.gameState = "NORMAL";
        com.traduvertgames.state.GameState.gameState = "NORMAL";
    }

    public void update() {
        if (!active) return;
        if (noticeFrames > 0) noticeFrames--;
        if (choosingArchetype || characterSheetOpen || character == null) return;
        player.update();
        playedFrames++;
        if (attackFrames > 0) attackFrames--;
        staminaRegenFrames++;
        if (staminaRegenFrames >= 4) {
            staminaRegenFrames = 0;
            character.updateStamina(1);
        }
        if (questStage == QuestStage.DEFEAT_WARDEN && isNearWarden()) {
            showNotice("Guardião do Bosque à frente — Espaço para atacar.");
        }
    }

    /** Retorna true quando o evento foi consumido pelo modo clássico. */
    public boolean handleInput(KeyEvent event) {
        if (!active) return false;
        int code = event.getKeyCode();
        if (choosingArchetype) {
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
                archetypeSelection = (archetypeSelection + RpgArchetype.values().length - 1)
                        % RpgArchetype.values().length;
                return true;
            }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
                archetypeSelection = (archetypeSelection + 1) % RpgArchetype.values().length;
                return true;
            }
            if (code == KeyEvent.VK_ENTER) {
                chooseArchetype();
                return true;
            }
            if (code == KeyEvent.VK_ESCAPE) {
                Game game = Game.getInstance();
                if (game != null) game.returnToMainMenu();
                return true;
            }
            return code != KeyEvent.VK_F11;
        }
        if (characterSheetOpen) {
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_C) {
                characterSheetOpen = false;
                return true;
            }
            return true;
        }
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) { player.setUp(true); return true; }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) { player.setDown(true); return true; }
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) { player.setLeft(true); return true; }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) { player.setRight(true); return true; }
        if (code == KeyEvent.VK_C) {
            characterSheetOpen = true;
            return true;
        }
        if (code == KeyEvent.VK_I) {
            showNotice("Inventário RPG será expandido na Rodada 3.");
            return true;
        }
        if (code == KeyEvent.VK_R || code == KeyEvent.VK_E) {
            interact();
            return true;
        }
        if (code == KeyEvent.VK_SPACE) {
            strikeWarden();
            return true;
        }
        if (code == KeyEvent.VK_T) {
            if (SaveManager.saveCurrentGame()) showNotice("Jogo salvo no slot " + SaveManager.activeSlot + ".");
            else showNotice("Não foi possível salvar o RPG Clássico.");
            return true;
        }
        // ESC, P e F11 seguem o comportamento global da engine: pausa, tela cheia etc.
        return false;
    }

    public boolean handleKeyReleased(KeyEvent event) {
        if (!active || player == null) return false;
        int code = event.getKeyCode();
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) { player.setUp(false); return true; }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) { player.setDown(false); return true; }
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) { player.setLeft(false); return true; }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) { player.setRight(false); return true; }
        return false;
    }

    private void chooseArchetype() {
        RpgArchetype[] archetypes = RpgArchetype.values();
        character = RpgCharacterStats.create(archetypes[archetypeSelection]);
        choosingArchetype = false;
        questStage = QuestStage.FIND_GUIDE;
        wardenLife = 3;
        objective = objectiveFor(questStage);
        showNotice("Você é " + character.getArchetype().getDisplayName()
                + ". Iara, a guia, precisa falar com você.");
    }

    private void interact() {
        if (character == null || player == null) return;
        if (!isNearGuide()) {
            if (questStage == QuestStage.DEFEAT_WARDEN) {
                showNotice("Siga pela Estrada Antiga até o Bosque dos Sussurros.");
            } else if (questStage == QuestStage.RETURN_TO_GUIDE) {
                showNotice("Volte para Iara, perto das casas de Brumafolha.");
            } else if (questStage == QuestStage.COMPLETE) {
                showNotice("O vale está seguro. Explore e grave sua jornada com T.");
            } else {
                showNotice("Iara está perto das casas. Aproxime-se e pressione R ou E.");
            }
            return;
        }
        if (questStage == QuestStage.FIND_GUIDE) {
            questStage = QuestStage.DEFEAT_WARDEN;
            objective = objectiveFor(questStage);
            showNotice("Iara: algo corrompeu o bosque. Derrote o Guardião e volte aqui.");
        } else if (questStage == QuestStage.RETURN_TO_GUIDE) {
            questStage = QuestStage.COMPLETE;
            objective = objectiveFor(questStage);
            character.gainExperience(90);
            character.restoreResources();
            showNotice("Iara: o vale respira de novo. +90 XP e recursos restaurados.");
        } else if (questStage == QuestStage.COMPLETE) {
            showNotice("Iara: as Ruínas do Sino serão a próxima expedição.");
        } else {
            showNotice("Iara: siga a Estrada Antiga até o Bosque dos Sussurros.");
        }
    }

    private void strikeWarden() {
        if (character == null || player == null) return;
        if (questStage != QuestStage.DEFEAT_WARDEN) {
            showNotice("Não há ameaça próxima. Use R ou E para conversar com Iara.");
            return;
        }
        if (!isNearWarden()) {
            showNotice("O Guardião está no Bosque dos Sussurros, além da Estrada Antiga.");
            return;
        }
        if (!character.spendStamina(8)) {
            showNotice("Fôlego insuficiente. Espere um instante antes de atacar.");
            return;
        }
        attackFrames = 12;
        int damage = Math.max(1, 1 + character.getStrength() / 14);
        wardenLife = Math.max(0, wardenLife - damage);
        if (wardenLife == 0) {
            questStage = QuestStage.RETURN_TO_GUIDE;
            objective = objectiveFor(questStage);
            showNotice("O Guardião foi purificado. Volte para Iara em Brumafolha.");
        } else {
            showNotice("Golpe acertado. Integridade do Guardião: " + wardenLife + ".");
        }
    }

    private boolean isNearGuide() {
        return distanceTo(map.getVillageGuideX(), map.getVillageGuideY()) <= 48;
    }

    private boolean isNearWarden() {
        return questStage == QuestStage.DEFEAT_WARDEN && wardenLife > 0
                && distanceTo(map.getWardenX(), map.getWardenY()) <= 52;
    }

    private double distanceTo(double x, double y) {
        double dx = player.getX() - x;
        double dy = player.getY() - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static String objectiveFor(QuestStage stage) {
        if (stage == QuestStage.DEFEAT_WARDEN) {
            return "Siga a Estrada Antiga e derrote o Guardião do Bosque com Espaço.";
        }
        if (stage == QuestStage.RETURN_TO_GUIDE) {
            return "Retorne a Brumafolha e conte a Iara que o Guardião foi purificado.";
        }
        if (stage == QuestStage.COMPLETE) {
            return "Missão concluída. Explore o vale, confira seus atributos e salve a jornada.";
        }
        return "Fale com Iara, a guia da vila, perto das casas de Brumafolha.";
    }

    private void showNotice(String text) {
        notice = text;
        noticeFrames = 180;
    }

    public void render(Graphics g) {
        if (!active) return;
        map.render(g, player == null ? 0 : player.getCameraX(),
                player == null ? 0 : player.getCameraY(), Game.WIDTH, Game.HEIGHT);
        if (player != null) {
            drawGuide(g);
            if (questStage == QuestStage.DEFEAT_WARDEN && wardenLife > 0) drawWarden(g);
        }
        if (player != null) drawPlayer(g);
    }

    private void drawGuide(Graphics g) {
        int x = (int) (map.getVillageGuideX() - player.getCameraX());
        int y = (int) (map.getVillageGuideY() - player.getCameraY());
        g.setColor(new Color(21, 27, 35, 105));
        g.fillOval(x - 11, y + 8, 22, 8);
        g.setColor(new Color(160, 78, 110));
        g.fillOval(x - 8, y - 9, 16, 22);
        g.setColor(new Color(246, 209, 173));
        g.fillOval(x - 6, y - 13, 12, 12);
        g.setColor(new Color(247, 226, 157));
        g.drawString("Iara", x - 10, y - 18);
        if (isNearGuide() && questStage != QuestStage.DEFEAT_WARDEN) {
            g.setColor(new Color(255, 241, 174));
            g.drawString("R/E", x - 8, y - 29);
        }
    }

    private void drawWarden(Graphics g) {
        int x = (int) (map.getWardenX() - player.getCameraX());
        int y = (int) (map.getWardenY() - player.getCameraY());
        g.setColor(new Color(25, 24, 39, 110));
        g.fillOval(x - 14, y + 8, 28, 9);
        BufferedImage sprite = AssetCatalog.enemySprite(Enemy.Variant.GUARDIAN);
        if (sprite != null) {
            g.drawImage(sprite, x - 14, y - 14, 28, 28, null);
        } else {
            g.setColor(new Color(62, 48, 88));
            g.fillOval(x - 11, y - 11, 22, 25);
            g.setColor(new Color(147, 198, 112));
            g.fillOval(x - 7, y - 7, 14, 11);
            g.setColor(new Color(237, 113, 122));
            g.fillRect(x - 4, y - 3, 3, 3);
            g.fillRect(x + 2, y - 3, 3, 3);
        }
        g.setColor(new Color(240, 224, 182));
        g.drawString("Guardião " + wardenLife, x - 23, y - 17);
        if (isNearWarden()) {
            g.setColor(new Color(255, 240, 176));
            g.drawString("ESPAÇO", x - 20, y - 28);
        }
    }

    public void renderOverlay(Graphics g) {
        if (!active) return;
        int width = Game.WIDTH * Game.SCALE;
        int height = Game.HEIGHT * Game.SCALE;
        if (choosingArchetype) {
            renderArchetypeSelection(g, width, height);
            return;
        }
        if (character != null) drawHud(g, width, height);
        if (characterSheetOpen) renderCharacterSheet(g, width, height);
        if (noticeFrames > 0 && notice != null && !notice.isEmpty()) {
            g.setFont(new Font("Arial", Font.BOLD, 15));
            int textWidth = g.getFontMetrics().stringWidth(notice);
            int x = (width - textWidth) / 2;
            int y = height - 24;
            g.setColor(new Color(15, 20, 25, 210));
            g.fillRoundRect(x - 14, y - 22, textWidth + 28, 32, 8, 8);
            g.setColor(new Color(248, 228, 167));
            g.drawString(notice, x, y);
        }
    }

    private void drawPlayer(Graphics g) {
        int px = (int) (player.getX() - player.getCameraX());
        int py = (int) (player.getY() - player.getCameraY());
        g.setColor(new Color(24, 28, 32, 100));
        g.fillOval(px - 13, py + 8, 26, 9);
        g.setColor(new Color(54, 86, 126));
        g.fillOval(px - 10, py - 12, 20, 25);
        g.setColor(new Color(221, 183, 126));
        g.fillOval(px - 7, py - 14, 14, 14);
        g.setColor(new Color(242, 216, 161));
        g.drawLine(px, py - 5, px + (int) player.getFacingX() * 13,
                py - 5 + (int) player.getFacingY() * 13);
        if (attackFrames > 0) {
            g.setColor(new Color(255, 239, 168, 210));
            g.drawArc(px - 16, py - 16, 32, 32, -45, 125);
        }
        g.setColor(Color.WHITE);
        g.fillRect(px - 2, py - 7, 2, 2);
    }

    private void drawHud(Graphics g, int width, int height) {
        int x = 12;
        int y = 12;
        g.setColor(new Color(17, 22, 30, 220));
        g.fillRoundRect(x, y, 178, 86, 10, 10);
        g.setColor(new Color(246, 222, 159));
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.drawString(character.getArchetype().getDisplayName() + "  Nv. " + character.getLevel(), x + 10, y + 20);
        drawBar(g, x + 10, y + 30, 120, 9, character.getLife(), character.getMaxLife(), new Color(191, 64, 67));
        drawBar(g, x + 10, y + 45, 120, 9, character.getMana(), character.getMaxMana(), new Color(59, 113, 190));
        drawBar(g, x + 10, y + 60, 120, 9, character.getStamina(), character.getMaxStamina(), new Color(207, 158, 55));
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("HP " + character.getLife() + "/" + character.getMaxLife(), x + 136, y + 38);
        g.drawString("MP " + character.getMana() + "/" + character.getMaxMana(), x + 136, y + 53);
        g.drawString("ST " + character.getStamina() + "/" + character.getMaxStamina(), x + 136, y + 68);
        g.setColor(new Color(232, 222, 193));
        g.drawString("XP " + character.getExperience() + "/" + character.experienceToNextLevel(), x + 10, y + 81);

        g.setColor(new Color(17, 22, 30, 205));
        g.fillRoundRect(width - 205, height - 68, 193, 56, 8, 8);
        g.setColor(new Color(246, 222, 159));
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("OBJETIVO", width - 193, height - 50);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        drawWrapped(g, objective, width - 193, height - 34, 174);
        g.setColor(new Color(235, 225, 197));
        g.drawString("WASD mover  R/E falar  Espaço atacar  C atributos  T salvar", 12, height - 8);
    }

    private void drawBar(Graphics g, int x, int y, int width, int height, int value, int max, Color color) {
        g.setColor(new Color(50, 55, 62));
        g.fillRoundRect(x, y, width, height, 5, 5);
        int filled = max <= 0 ? 0 : Math.max(0, Math.min(width, width * value / max));
        g.setColor(color);
        g.fillRoundRect(x, y, filled, height, 5, 5);
    }

    private void renderArchetypeSelection(Graphics g, int width, int height) {
        g.setColor(new Color(10, 15, 20, 235));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(246, 222, 159));
        g.setFont(new Font("Arial", Font.BOLD, 28));
        String title = "RPG CLÁSSICO";
        g.drawString(title, (width - g.getFontMetrics().stringWidth(title)) / 2, 72);
        g.setColor(new Color(210, 217, 219));
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        String subtitle = "Escolha uma origem para sua jornada — nenhuma build é permanente";
        g.drawString(subtitle, (width - g.getFontMetrics().stringWidth(subtitle)) / 2, 98);
        RpgArchetype[] options = RpgArchetype.values();
        for (int i = 0; i < options.length; i++) {
            int y = 126 + i * 48;
            if (i == archetypeSelection) {
                g.setColor(new Color(64, 91, 116, 220));
                g.fillRoundRect(width / 2 - 180, y - 22, 360, 39, 8, 8);
            }
            g.setColor(i == archetypeSelection ? new Color(255, 236, 167) : Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 17));
            g.drawString((i == archetypeSelection ? "> " : "  ") + options[i].getDisplayName(), width / 2 - 150, y);
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.setColor(new Color(202, 210, 212));
            g.drawString(options[i].getDescription(), width / 2 - 10, y);
        }
        g.setColor(new Color(190, 198, 202));
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        String hint = "W/S ou setas — Enter confirmar — Esc voltar";
        g.drawString(hint, (width - g.getFontMetrics().stringWidth(hint)) / 2, height - 35);
    }

    private void renderCharacterSheet(Graphics g, int width, int height) {
        g.setColor(new Color(8, 12, 17, 235));
        g.fillRoundRect(62, 24, width - 124, height - 48, 12, 12);
        g.setColor(new Color(246, 222, 159));
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("PERSONAGEM", 88, 61);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.drawString(character.getArchetype().getDisplayName() + " — nível " + character.getLevel(), 88, 88);
        String[] lines = {
            "Força: " + character.getStrength(), "Destreza: " + character.getDexterity(),
            "Inteligência: " + character.getIntelligence(), "Vitalidade: " + character.getVitality(),
            "Defesa física: " + character.getPhysicalDefense(), "Defesa mágica: " + character.getMagicalDefense(),
            "Crítico: " + character.getCriticalChance() + "%", "Pontos de atributo: " + character.getAttributePoints()
        };
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 100 + (i / 4) * 230, 125 + (i % 4) * 24);
        }
        g.setColor(new Color(190, 198, 202));
        g.drawString("C ou Esc para fechar", 88, height - 58);
    }

    private void drawWrapped(Graphics g, String text, int x, int y, int maxWidth) {
        String[] words = text.split(" ");
        String line = "";
        int lineY = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) > maxWidth) {
                g.drawString(line, x, lineY);
                line = word;
                lineY += 13;
            } else line = candidate;
        }
        if (!line.isEmpty()) g.drawString(line, x, lineY);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("schema", 2);
        data.put("mapId", "vale_brumafolha");
        data.put("objective", objective);
        data.put("playedFrames", playedFrames);
        data.put("questStage", questStage.name());
        data.put("wardenLife", wardenLife);
        data.put("character", character == null ? RpgCharacterStats.create(RpgArchetype.GUARDIAO).serialize() : character.serialize());
        data.put("player", player == null ? new HashMap<String, Object>() : player.serialize());
        data.put("restPoint", "village_west_gate");
        return data;
    }

    private static Map<String, Object> asMap(Object raw) {
        if (raw instanceof Map<?, ?>) {
            Map<String, Object> result = new HashMap<String, Object>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
                if (entry.getKey() != null) result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return null;
    }

    private static String stringValue(Object raw, String fallback) {
        return raw == null ? fallback : String.valueOf(raw);
    }

    private static long nonNegativeLong(Object raw) {
        if (raw instanceof Number) return Math.max(0, ((Number) raw).longValue());
        try { return Math.max(0, Long.parseLong(String.valueOf(raw))); }
        catch (Exception ignored) { return 0; }
    }

    private static int intValue(Object raw, int fallback) {
        if (raw instanceof Number) return ((Number) raw).intValue();
        try { return Integer.parseInt(String.valueOf(raw)); }
        catch (Exception ignored) { return fallback; }
    }

    private static QuestStage stageValue(Object raw) {
        if (raw != null) {
            try { return QuestStage.valueOf(String.valueOf(raw)); }
            catch (IllegalArgumentException ignored) { }
        }
        return QuestStage.FIND_GUIDE;
    }

    public boolean isActive() { return active; }
    public boolean isChoosingArchetype() { return choosingArchetype; }
    public RpgMap getMap() { return map; }
    public RpgPlayerController getPlayer() { return player; }
    public RpgCharacterStats getCharacter() { return character; }
    public String getObjective() { return objective; }
    public String getMapId() { return "vale_brumafolha"; }
    public String getMapDisplayName() { return map.getDisplayName(); }
    public long getPlayedFrames() { return playedFrames; }
    public String getQuestStageForTest() { return questStage.name(); }
    public int getWardenLifeForTest() { return wardenLife; }

    public void reset() {
        active = false;
        choosingArchetype = false;
        characterSheetOpen = false;
        player = null;
        character = null;
        notice = "";
        noticeFrames = 0;
    }
}
