package com.traduvertgames.entities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;

import com.traduvertgames.world.Camera;

public class EnergyCell extends Entity {

        private final double manaRestore;
        private final double weaponRestore;
        private double rotation = 0;

        public EnergyCell(int x, int y) {
                this(x, y, 16, 16, 90, 60);
        }

        public EnergyCell(int x, int y, int width, int height, double manaRestore, double weaponRestore) {
                super(x, y, width, height, null);
                this.manaRestore = Math.max(0, manaRestore);
                this.weaponRestore = Math.max(0, weaponRestore);
                setMask(2, 2, Math.max(1, width - 4), Math.max(1, height - 4));
        }

        public double getManaRestore() {
                return manaRestore;
        }

        public double getWeaponRestore() {
                return weaponRestore;
        }

        @Override
        public void update() {
                rotation += 0.05;
                if (rotation > Math.PI * 2) {
                        rotation -= Math.PI * 2;
                }
        }

        @Override
        public void render(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(this.getX() - Camera.x + width / 2.0, this.getY() - Camera.y + height / 2.0);
                g2.rotate(rotation);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int coreSize = Math.min(width, height) - 6;
                int half = coreSize / 2;

                g2.setColor(new Color(0, 230, 118, 200));
                g2.fillOval(-half, -half, coreSize, coreSize);

                g2.setStroke(new BasicStroke(1.6f));
                g2.setColor(new Color(0, 77, 64));
                g2.drawOval(-half, -half, coreSize, coreSize);

                GeneralPath lightning = new GeneralPath();
                lightning.moveTo(-half / 2.0, -half / 2.0);
                lightning.lineTo(0, -half);
                lightning.lineTo(half / 2.0, -half / 3.0);
                lightning.lineTo(half / 6.0, half / 6.0);
                lightning.lineTo(0, half);
                lightning.lineTo(-half / 3.0, half / 4.0);
                lightning.closePath();

                g2.setColor(new Color(224, 242, 241));
                g2.fill(lightning);
                g2.setColor(new Color(38, 166, 154));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(lightning);

                g2.dispose();
        }
}
