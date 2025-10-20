package com.traduvertgames.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import com.traduvertgames.world.Camera;

public class OverclockModule extends Entity {

        private final double manaBoost;
        private final double weaponBoost;
        private double rotation = 0;

        public OverclockModule(int x, int y) {
                this(x, y, 16, 16, 120, 90);
        }

        public OverclockModule(int x, int y, int width, int height, double manaBoost, double weaponBoost) {
                super(x, y, width, height, null);
                this.manaBoost = Math.max(0, manaBoost);
                this.weaponBoost = Math.max(0, weaponBoost);
                setMask(2, 2, Math.max(1, width - 4), Math.max(1, height - 4));
        }

        public double getManaBoost() {
                return manaBoost;
        }

        public double getWeaponBoost() {
                return weaponBoost;
        }

        @Override
        public void update() {
                rotation += 0.06;
                if (rotation > Math.PI * 2) {
                        rotation -= Math.PI * 2;
                }
        }

        @Override
        public void render(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int screenX = this.getX() - Camera.x;
                int screenY = this.getY() - Camera.y;
                g2.translate(screenX + width / 2.0, screenY + height / 2.0);
                g2.rotate(rotation);

                int radius = Math.max(6, Math.min(width, height) - 2);
                g2.setColor(new Color(0, 229, 255, 210));
                g2.fillOval(-radius / 2, -radius / 2, radius, radius);

                g2.setStroke(new BasicStroke(1.4f));
                g2.setColor(new Color(0, 96, 100));
                g2.drawOval(-radius / 2, -radius / 2, radius, radius);

                g2.rotate(-rotation * 2);
                g2.setColor(new Color(0, 188, 212));
                g2.drawLine(-radius / 2, 0, radius / 2, 0);
                g2.drawLine(0, -radius / 2, 0, radius / 2);
                g2.dispose();
        }
}
