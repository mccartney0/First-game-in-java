package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.world.Camera;

public class QuestBeacon extends Entity {
    private static final int ACTIVATION_TIME = 180;
    private final Color color;
    private int channelProgress = 0;
    private boolean activated = false;

    public QuestBeacon(int x, int y, Color color) {
        super(x, y, 16, 16, null);
        this.color = color;
        setMask(0, 0, 16, 16);
        QuestManager.registerBeacon(this);
    }

    public boolean isActivated() {
        return activated;
    }

    @Override
    public void update() {
        if (activated) {
            return;
        }
        boolean closeToPlayer = Entity.isColliding(this, Game.player);
        if (closeToPlayer) {
            channelProgress += 2;
            if (channelProgress >= ACTIVATION_TIME) {
                activated = true;
                QuestManager.activateBeacon(this);
            }
        } else {
            channelProgress -= 3;
            if (channelProgress < 0) {
                channelProgress = 0;
            }
        }
    }

    @Override
    public void render(Graphics g) {
        int screenX = this.getX() - Camera.x;
        int screenY = this.getY() - Camera.y;
        Color aura = new Color(color.getRed(), color.getGreen(), color.getBlue(), 160);
        g.setColor(aura);
        g.fillOval(screenX + 1, screenY + 1, 14, 14);
        g.setColor(color.darker());
        g.drawOval(screenX + 1, screenY + 1, 14, 14);
        if (!activated) {
            double percent = Math.min(1.0, channelProgress / (double) ACTIVATION_TIME);
            int height = (int) Math.round(12 * percent);
            g.setColor(new Color(255, 255, 255, 180));
            g.fillRect(screenX + 7, screenY + 13 - height, 2, height);
        } else {
            g.setColor(new Color(255, 255, 255, 220));
            g.fillOval(screenX + 5, screenY + 5, 6, 6);
        }
    }
}
