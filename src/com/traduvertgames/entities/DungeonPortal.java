package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.DungeonManager;
import com.traduvertgames.world.RpgWorldManager;

/** Portal da superfície que abre a masmorra da região atual. */
public final class DungeonPortal extends Entity {

    private final RpgWorldManager.RegionType region;
    private int pulse;

    public DungeonPortal(int x, int y, RpgWorldManager.RegionType region) {
        super(x, y, 16, 16, null);
        this.region = region == null ? RpgWorldManager.RegionType.CORE : region;
        setMask(0, 0, 16, 16);
    }

    public RpgWorldManager.RegionType getRegion() {
        return region;
    }

    @Override
    public void update() {
        pulse++;
        if (Game.player != null && Entity.isColliding(this, Game.player)) {
            DungeonManager.requestEnter(region);
        }
    }

    @Override
    public void render(Graphics g) {
        int screenX = getX() - Camera.x;
        int screenY = getY() - Camera.y;
        int alpha = 150 + (int) (70 * (0.5 + 0.5 * Math.sin(pulse * 0.12)));
        g.setColor(new Color(74, 20, 120, alpha));
        g.fillOval(screenX + 1, screenY + 1, 14, 14);
        g.setColor(new Color(220, 90, 255, 230));
        g.drawOval(screenX + 2, screenY + 2, 12, 12);
        if (Game.player != null && calculateDistance(getX(), getY(), Game.player.getX(), Game.player.getY()) <= 48) {
            g.setFont(new Font("arial", Font.BOLD, 7 * Game.SCALE / 4 + 2));
            g.setColor(Color.WHITE);
            g.drawString("E — MASMORRA", screenX - 12, screenY - 5);
        }
    }
}
