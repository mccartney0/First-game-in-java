package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.FloorTile;
import com.traduvertgames.world.Tile;
import com.traduvertgames.world.World;

public class TeleportPad extends Entity {
        private static final int TELEPORT_COOLDOWN_FRAMES = 45;
        private static final int DEFAULT_CENTRAL_RADIUS = 5;

        private int cooldown = 0;

        public TeleportPad(int x, int y) {
                super(x, y, 16, 16, null);
                setMask(0, 0, 16, 16);
        }

        @Override
        public void update() {
                if (cooldown > 0) {
                        cooldown--;
                        return;
                }

                if (Entity.isColliding(this, Game.player)) {
                        if (teleportPlayerToCentralTile()) {
                                cooldown = TELEPORT_COOLDOWN_FRAMES;
                        }
                }
        }

        private boolean teleportPlayerToCentralTile() {
                List<int[]> centralTiles = collectCentralFloorTiles();
                if (centralTiles.isEmpty()) {
                        return false;
                }

                Collections.shuffle(centralTiles, Game.rand);
                for (int[] tilePos : centralTiles) {
                        int px = tilePos[0] * World.TILE_SIZE;
                        int py = tilePos[1] * World.TILE_SIZE;
                        if (!World.isFree(px, py, Game.player.z)) {
                                continue;
                        }
                        if (collidesWithBlockingEntity(px, py)) {
                                continue;
                        }
                        Game.player.setX(px);
                        Game.player.setY(py);
                        Game.player.updateCamera();
                        return true;
                }
                return false;
        }

        private List<int[]> collectCentralFloorTiles() {
                List<int[]> positions = new ArrayList<>();
                if (World.tiles == null || World.tiles.length == 0) {
                        return positions;
                }

                int centerX = World.WIDTH / 2;
                int centerY = World.HEIGHT / 2;
                int radius = Math.min(DEFAULT_CENTRAL_RADIUS, Math.min(centerX, centerY));
                if (radius < 2) {
                        radius = Math.max(1, radius);
                }

                int startX = Math.max(0, centerX - radius);
                int endX = Math.min(World.WIDTH - 1, centerX + radius);
                int startY = Math.max(0, centerY - radius);
                int endY = Math.min(World.HEIGHT - 1, centerY + radius);

                for (int x = startX; x <= endX; x++) {
                        for (int y = startY; y <= endY; y++) {
                                Tile tile = World.tiles[x + (y * World.WIDTH)];
                                if (tile instanceof FloorTile) {
                                        positions.add(new int[] { x, y });
                                }
                        }
                }

                if (positions.isEmpty()) {
                        positions.add(new int[] { centerX, centerY });
                }

                return positions;
        }

        private boolean collidesWithBlockingEntity(int px, int py) {
                Rectangle playerMask = new Rectangle(px + Game.player.maskx, py + Game.player.masky, Game.player.mwidth,
                                Game.player.mheight);
                for (Entity entity : Game.entities) {
                        if (entity == Game.player || entity == this) {
                                continue;
                        }
                        if (entity.maskx == 0 && entity.masky == 0 && entity.mwidth == 0 && entity.mheight == 0) {
                                continue;
                        }
                        Rectangle entityMask = new Rectangle(entity.getX() + entity.maskx,
                                        entity.getY() + entity.masky, entity.mwidth, entity.mheight);
                        if (playerMask.intersects(entityMask)) {
                                return true;
                        }
                }
                return false;
        }

        @Override
        public void render(Graphics g) {
                int screenX = this.getX() - Camera.x;
                int screenY = this.getY() - Camera.y;

                g.setColor(new Color(88, 28, 135, 160));
                g.fillOval(screenX + 1, screenY + 1, 14, 14);

                g.setColor(new Color(171, 71, 188, 200));
                g.fillOval(screenX + 4, screenY + 4, 8, 8);

                g.setColor(new Color(255, 255, 255, 180));
                g.drawOval(screenX + 2, screenY + 2, 12, 12);
        }
}
