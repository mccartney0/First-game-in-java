package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;

public class Weapon extends Entity {

        private final WeaponType type;

        public Weapon(int x, int y, int width, int height, BufferedImage sprite) {
                this(x, y, width, height, sprite, chooseTypeForDrop());
        }

        public Weapon(int x, int y, int width, int height, BufferedImage sprite, WeaponType type) {
                super(x, y, width, height, sprite);
                this.type = type;
        }

        public WeaponType getType() {
                return type;
        }

        @Override
        public void render(Graphics g) {
                super.render(g);
                if (type != null) {
                        Color color = type.getUiColor();
                        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 160));
                        g.fillRect(this.getX() - Camera.x + 4, this.getY() - Camera.y + 4, 8, 8);
                        g.setColor(color.darker());
                        g.drawRect(this.getX() - Camera.x + 3, this.getY() - Camera.y + 3, 10, 10);
                }
        }

        private static WeaponType chooseTypeForDrop() {
                Player player = Game.player;
                EnumSet<WeaponType> lockedTypes = EnumSet.noneOf(WeaponType.class);
                for (WeaponType candidate : WeaponType.values()) {
                        boolean unlocked = player != null && player.hasWeaponUnlocked(candidate);
                        if (!unlocked) {
                                lockedTypes.add(candidate);
                        }
                }

                WeaponType chosen = WeaponType.randomPreferLocked(Game.rand, lockedTypes);
                if (player == null || player.hasWeaponUnlocked(chosen)) {
                        // Provide variation by avoiding repeating the current weapon consecutively.
                        List<WeaponType> candidates = new ArrayList<WeaponType>();
                        for (WeaponType type : WeaponType.values()) {
                                if (player == null || type != player.getCurrentWeaponType()) {
                                        candidates.add(type);
                                }
                        }
                        if (!candidates.isEmpty()) {
                                chosen = candidates.get(Game.rand.nextInt(candidates.size()));
                        }
                }
                return chosen;
        }
}
