package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.traduvertgames.world.Camera;

public class DataCore extends QuestItem {

        private double rotation = 0;

        public DataCore(int x, int y) {
                super(x, y, new Color(0, 172, 193));
        }

        @Override
        public void update() {
                super.update();
                if (!isCollected()) {
                        rotation += 0.04;
                        if (rotation > Math.PI * 2) {
                                rotation -= Math.PI * 2;
                        }
                }
        }

        @Override
        public void render(Graphics g) {
                if (isCollected()) {
                        return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                int screenX = this.getX() - Camera.x;
                int screenY = this.getY() - Camera.y;
                g2.translate(screenX + 8, screenY + 8);
                g2.rotate(rotation);

                g2.setColor(new Color(0, 188, 212, 220));
                g2.fillRect(-5, -5, 10, 10);
                g2.setColor(new Color(0, 121, 107));
                g2.drawRect(-5, -5, 10, 10);

                g2.setColor(new Color(224, 247, 250));
                g2.fillRect(-2, -2, 4, 4);
                g2.dispose();
        }
}
