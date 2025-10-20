package com.traduvertgames.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import com.traduvertgames.world.Camera;

public class ShieldOrb extends Entity {

        private final double shieldValue;
        private double pulse = 0;
        private boolean pulseGrowing = true;

        public ShieldOrb(int x, int y) {
                this(x, y, 16, 16, 75);
        }

        public ShieldOrb(int x, int y, int width, int height, double shieldValue) {
                super(x, y, width, height, null);
                this.shieldValue = Math.max(0, shieldValue);
                setMask(2, 2, Math.max(1, width - 4), Math.max(1, height - 4));
        }

        public double getShieldValue() {
                return shieldValue;
        }

        @Override
        public void update() {
                double delta = 0.05;
                if (pulseGrowing) {
                        pulse += delta;
                        if (pulse >= 1.0) {
                                pulse = 1.0;
                                pulseGrowing = false;
                        }
                } else {
                        pulse -= delta;
                        if (pulse <= 0.0) {
                                pulse = 0.0;
                                pulseGrowing = true;
                        }
                }
        }

        @Override
        public void render(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(this.getX() - Camera.x, this.getY() - Camera.y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                float alpha = (float) (0.45 + 0.35 * pulse);
                Color base = new Color(63, 81, 181);
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (alpha * 255)));
                g2.fillOval(2, 2, width - 4, height - 4);

                g2.setStroke(new BasicStroke(1.4f));
                g2.setColor(new Color(197, 202, 233));
                g2.drawOval(2, 2, width - 4, height - 4);

                Path2D.Double hex = new Path2D.Double();
                double centerX = width / 2.0;
                double centerY = height / 2.0;
                double radius = (width - 8) / 2.0;
                for (int i = 0; i < 6; i++) {
                        double angle = Math.toRadians(60 * i - 30);
                        double px = centerX + Math.cos(angle) * radius;
                        double py = centerY + Math.sin(angle) * radius;
                        if (i == 0) {
                                hex.moveTo(px, py);
                        } else {
                                hex.lineTo(px, py);
                        }
                }
                hex.closePath();

                g2.setColor(new Color(121, 134, 203));
                g2.setStroke(new BasicStroke(1.1f));
                g2.draw(hex);

                g2.dispose();
        }
}
