package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.traduvertgames.world.Camera;

public class NanoMedkit extends Entity {

        private final double healAmount;
        private final double shieldAmount;
        private double pulse = 0;

        public NanoMedkit(int x, int y) {
                this(x, y, 16, 16, 60, 50);
        }

        public NanoMedkit(int x, int y, int width, int height, double healAmount, double shieldAmount) {
                super(x, y, width, height, null);
                this.healAmount = Math.max(0, healAmount);
                this.shieldAmount = Math.max(0, shieldAmount);
                setMask(2, 2, Math.max(1, width - 4), Math.max(1, height - 4));
        }

        public double getHealAmount() {
                return healAmount;
        }

        public double getShieldAmount() {
                return shieldAmount;
        }

        @Override
        public void update() {
                pulse += 0.05;
                if (pulse > Math.PI * 2) {
                        pulse -= Math.PI * 2;
                }
        }

        @Override
        public void render(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int screenX = this.getX() - Camera.x;
                int screenY = this.getY() - Camera.y;
                g2.translate(screenX + width / 2.0, screenY + height / 2.0);
                g2.rotate(Math.sin(pulse) * 0.15);

                int bodyWidth = Math.max(6, width - 4);
                int bodyHeight = Math.max(6, height - 4);
                g2.setColor(new Color(229, 57, 53, 210));
                g2.fillRoundRect(-bodyWidth / 2, -bodyHeight / 2, bodyWidth, bodyHeight, 6, 6);

                g2.setColor(new Color(255, 255, 255, 235));
                g2.fillRect(-2, -bodyHeight / 2 + 3, 4, bodyHeight - 6);
                g2.fillRect(-bodyWidth / 2 + 3, -2, bodyWidth - 6, 4);

                g2.setColor(new Color(183, 28, 28, 220));
                g2.drawRoundRect(-bodyWidth / 2, -bodyHeight / 2, bodyWidth, bodyHeight, 6, 6);
                g2.dispose();
        }
}
