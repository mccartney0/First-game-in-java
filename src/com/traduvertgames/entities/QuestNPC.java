package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.world.Camera;

public class QuestNPC extends Entity {
    private final Color robeColor;
    private boolean rescued = false;

    public QuestNPC(int x, int y, Color robeColor) {
        super(x, y, 16, 16, null);
        this.robeColor = robeColor;
        setMask(2, 2, 12, 12);
        QuestManager.registerNpc(this);
    }

    protected void onRescued() {
    }

    @Override
    public void update() {
        if (rescued) {
            return;
        }
        if (Entity.isColliding(this, Game.player)) {
            rescued = true;
            QuestManager.rescueNpc(this);
            onRescued();
        }
    }

    @Override
    public void render(Graphics g) {
        if (rescued) {
            return;
        }
        int screenX = this.getX() - Camera.x;
        int screenY = this.getY() - Camera.y;
        g.setColor(new Color(30, 30, 30));
        g.fillRect(screenX + 6, screenY + 2, 4, 4);
        g.setColor(new Color(255, 224, 178));
        g.fillOval(screenX + 5, screenY + 5, 6, 6);
        g.setColor(robeColor);
        g.fillRoundRect(screenX + 3, screenY + 10, 10, 6, 4, 4);
    }
}
