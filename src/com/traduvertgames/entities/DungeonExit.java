package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.DungeonManager;

/** Saída gerada na última sala da masmorra. */
public final class DungeonExit extends Entity {

    private int pulse;

    public DungeonExit(int x, int y) {
        super(x, y, 16, 16, null);
        setMask(0, 0, 16, 16);
    }

    @Override
    public void update() {
        pulse++;
        if (Game.player != null && Entity.isColliding(this, Game.player)) {
            if (DungeonManager.isBossDefeated()) {
                DungeonManager.requestExit();
            }
        }
    }

    @Override
    public void render(Graphics g) {
        int screenX = getX() - Camera.x;
        int screenY = getY() - Camera.y;
        boolean unlocked = DungeonManager.isBossDefeated();
        int alpha = 140 + (int) (80 * (0.5 + 0.5 * Math.sin(pulse * 0.12)));
        g.setColor(unlocked ? new Color(80, 200, 120, alpha) : new Color(90, 90, 110, 180));
        g.fillRect(screenX + 2, screenY + 2, 12, 12);
        g.setColor(unlocked ? new Color(180, 255, 200, 240) : new Color(160, 160, 180, 180));
        g.drawRect(screenX + 2, screenY + 2, 12, 12);
        if (Game.player != null && calculateDistance(getX(), getY(), Game.player.getX(), Game.player.getY()) <= 48) {
            g.setFont(new Font("arial", Font.BOLD, 7 * Game.SCALE / 4 + 2));
            g.setColor(Color.WHITE);
            g.drawString(unlocked ? "SAÍDA" : "DERROTE O CHEFE", screenX - 8, screenY - 5);
        }
    }
}
