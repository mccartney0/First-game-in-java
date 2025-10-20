package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.world.Camera;

public class QuestItem extends Entity {
    private final Color color;
    private boolean collected = false;

    public QuestItem(int x, int y, Color color) {
        super(x, y, 16, 16, null);
        this.color = color;
        setMask(2, 2, 12, 12);
        QuestManager.registerQuestItem(this);
    }

    public boolean isCollected() {
        return collected;
    }

    @Override
    public void update() {
        if (collected) {
            return;
        }
        if (Entity.isColliding(this, Game.player)) {
            collected = true;
            QuestManager.collectQuestItem(this);
        }
    }

    @Override
    public void render(Graphics g) {
        if (collected) {
            return;
        }
        int screenX = this.getX() - Camera.x;
        int screenY = this.getY() - Camera.y;
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
        int[] xs = { screenX + 8, screenX + 12, screenX + 8, screenX + 4 };
        int[] ys = { screenY + 2, screenY + 8, screenY + 14, screenY + 8 };
        g.fillPolygon(xs, ys, xs.length);
        g.setColor(color.darker());
        g.drawPolygon(xs, ys, xs.length);
    }
}
