package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.DynamicEventManager;

/** Carga móvel protegida pelo jogador durante o evento SUPPLY_CONVOY. */
public final class SupplyConvoy extends Entity {

    private static final double SPEED = 0.55;
    private static final int MAX_HP = 6;
    private static final int HIT_COOLDOWN_FRAMES = 45;
    private static final double THREAT_RADIUS = 48.0;

    private final double startX;
    private final double startY;
    private final int destinationX;
    private final int destinationY;
    private int hp = MAX_HP;
    private int hitCooldown;
    private boolean arrived;
    private boolean failed;

    public SupplyConvoy(int x, int y, int destinationX, int destinationY) {
        super(x, y, 24, 18, null);
        this.startX = x;
        this.startY = y;
        this.destinationX = destinationX;
        this.destinationY = destinationY;
        setMask(1, 1, 22, 16);
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return MAX_HP;
    }

    public boolean hasArrived() {
        return arrived;
    }

    public boolean hasFailed() {
        return failed;
    }

    /** Percentual de rota concluída, usado pela HUD do evento. */
    public int getRoutePercent() {
        double total = Math.hypot(destinationX - startX, destinationY - startY);
        if (total <= 0.0) {
            return 100;
        }
        double remaining = Math.hypot(destinationX - x, destinationY - y);
        return Math.max(0, Math.min(100, (int) Math.round((1.0 - remaining / total) * 100.0)));
    }

    public void takeHit() {
        if (arrived || failed || hitCooldown > 0) {
            return;
        }
        hitCooldown = HIT_COOLDOWN_FRAMES;
        hp--;
        FloatingText.show("-1 carga", getX() + 8, getY(), new Color(255, 120, 80), 45);
        if (hp <= 0) {
            failed = true;
            DynamicEventManager.onConvoyFailed(this);
        }
    }

    @Override
    public void update() {
        if (arrived || failed) {
            return;
        }
        if (hitCooldown > 0) {
            hitCooldown--;
        }

        boolean threatened = false;
        for (Enemy enemy : new java.util.ArrayList<Enemy>(Game.enemies)) {
            if (enemy == null) {
                continue;
            }
            double distance = Math.hypot(enemy.getX() - getX(), enemy.getY() - getY());
            if (distance <= THREAT_RADIUS) {
                threatened = true;
                if (Entity.isColliding(this, enemy)) {
                    takeHit();
                }
            }
        }
        if (threatened) {
            return;
        }

        double dx = destinationX - x;
        double dy = destinationY - y;
        double distance = Math.hypot(dx, dy);
        if (distance < 2.0) {
            arrived = true;
            DynamicEventManager.onConvoyArrived(this);
            return;
        }
        x += (dx / distance) * SPEED;
        y += (dy / distance) * SPEED;
    }

    @Override
    public void render(Graphics g) {
        int screenX = getX() - Camera.x;
        int screenY = getY() - Camera.y;
        g.setColor(new Color(40, 40, 40));
        g.fillRect(screenX + 1, screenY + 5, 22, 11);
        g.setColor(new Color(121, 85, 55));
        g.fillRect(screenX + 4, screenY + 2, 8, 8);
        g.fillRect(screenX + 14, screenY + 1, 7, 9);
        g.setColor(new Color(255, 193, 7));
        g.drawRect(screenX + 1, screenY + 5, 22, 11);
        g.setColor(Color.DARK_GRAY);
        g.fillOval(screenX + 3, screenY + 14, 5, 5);
        g.fillOval(screenX + 16, screenY + 14, 5, 5);

        g.setColor(new Color(30, 30, 30, 210));
        g.fillRect(screenX, screenY - 7, 24, 4);
        g.setColor(new Color(76, 175, 80));
        g.fillRect(screenX, screenY - 7, Math.max(0, hp * 24 / MAX_HP), 4);
        g.setColor(Color.WHITE);
        g.drawString(getRoutePercent() + "%", screenX - 2, screenY - 10);
    }
}
