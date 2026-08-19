package com.traduvertgames.rpg;

import java.util.HashMap;
import java.util.Map;

/** Movimento e câmera do player clássico, sem armas, projéteis ou pickups sci-fi. */
public final class RpgPlayerController {
    private double x;
    private double y;
    private double cameraX;
    private double cameraY;
    private double facingX = 0;
    private double facingY = 1;
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private final RpgMap map;

    public RpgPlayerController(RpgMap map) {
        this.map = map;
        setPosition(map.getSpawnX(), map.getSpawnY());
    }

    public void update() {
        double dx = (right ? 1 : 0) - (left ? 1 : 0);
        double dy = (down ? 1 : 0) - (up ? 1 : 0);
        if (dx != 0 || dy != 0) {
            double length = Math.sqrt(dx * dx + dy * dy);
            dx /= length;
            dy /= length;
            facingX = dx;
            facingY = dy;
            double speed = 2.45;
            move(dx * speed, dy * speed);
        }
        updateCamera();
    }

    private void move(double dx, double dy) {
        if (map.isWalkable(x + dx, y, 18, 18)) x += dx;
        if (map.isWalkable(x, y + dy, 18, 18)) y += dy;
        x = clamp(x, 16, map.getPixelWidth() - 16);
        y = clamp(y, 16, map.getPixelHeight() - 16);
    }

    private void updateCamera() {
        double targetX = x - 192 + facingX * 24;
        double targetY = y - 108 + facingY * 16;
        cameraX += (targetX - cameraX) * 0.12;
        cameraY += (targetY - cameraY) * 0.12;
        cameraX = clamp(cameraX, 0, Math.max(0, map.getPixelWidth() - 384));
        cameraY = clamp(cameraY, 0, Math.max(0, map.getPixelHeight() - 216));
    }

    public void setPosition(double x, double y) {
        this.x = clamp(x, 16, map.getPixelWidth() - 16);
        this.y = clamp(y, 16, map.getPixelHeight() - 16);
        this.cameraX = clamp(this.x - 192, 0, Math.max(0, map.getPixelWidth() - 384));
        this.cameraY = clamp(this.y - 108, 0, Math.max(0, map.getPixelHeight() - 216));
    }

    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("x", x);
        data.put("y", y);
        data.put("facingX", facingX);
        data.put("facingY", facingY);
        return data;
    }

    public void deserialize(Map<String, Object> data) {
        if (data == null) {
            setPosition(map.getSpawnX(), map.getSpawnY());
            return;
        }
        setPosition(toDouble(data.get("x"), map.getSpawnX()),
                toDouble(data.get("y"), map.getSpawnY()));
        facingX = toDouble(data.get("facingX"), 0);
        facingY = toDouble(data.get("facingY"), 1);
    }

    private static double toDouble(Object raw, double fallback) {
        if (raw instanceof Number) return ((Number) raw).doubleValue();
        try { return Double.parseDouble(String.valueOf(raw)); }
        catch (Exception ignored) { return fallback; }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public void setUp(boolean value) { up = value; }
    public void setDown(boolean value) { down = value; }
    public void setLeft(boolean value) { left = value; }
    public void setRight(boolean value) { right = value; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getCameraX() { return cameraX; }
    public double getCameraY() { return cameraY; }
    public double getFacingX() { return facingX; }
    public double getFacingY() { return facingY; }
}
