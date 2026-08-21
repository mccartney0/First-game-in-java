package com.traduvertgames.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final float WORLD_WIDTH = 1152f;
    private static final float WORLD_HEIGHT = 648f;
    private static final float PLAYER_RADIUS = 22f;
    private static final float MAX_DT = 0.04f;

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random(0xF1A5E);
    private final List<Shot> shots = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final Bitmap scoutBitmap;
    private final Bitmap blasterBitmap;

    private Thread gameThread;
    private volatile boolean running;
    private long previousNanos;
    private float scale = 1f;
    private float offsetX;
    private float offsetY;

    private float playerX = WORLD_WIDTH / 2f;
    private float playerY = WORLD_HEIGHT / 2f;
    private float aimX = WORLD_WIDTH - 120f;
    private float aimY = WORLD_HEIGHT / 2f;
    private float moveAxisX;
    private float moveAxisY;
    private boolean firing;
    private int movePointerId = -1;
    private int aimPointerId = -1;
    private float fireTimer;
    private float spawnTimer;
    private float elapsed;
    private float health = 100f;
    private int score;
    private boolean gameOver;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        scoutBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.scout_ref);
        blasterBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.blaster_clean);
    }

    public void resumeGame() {
        if (holder.getSurface().isValid() && gameThread == null) {
            startLoop();
        }
    }

    public void pauseGame() {
        stopLoop();
    }

    private synchronized void startLoop() {
        if (running) return;
        running = true;
        previousNanos = System.nanoTime();
        gameThread = new Thread(this, "first-game-android-loop");
        gameThread.start();
    }

    private synchronized void stopLoop() {
        running = false;
        Thread thread = gameThread;
        gameThread = null;
        if (thread != null) {
            try {
                thread.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        startLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        calculateViewport(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopLoop();
    }

    private void calculateViewport(int width, int height) {
        scale = Math.min(width / WORLD_WIDTH, height / WORLD_HEIGHT);
        offsetX = (width - WORLD_WIDTH * scale) * 0.5f;
        offsetY = (height - WORLD_HEIGHT * scale) * 0.5f;
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            float dt = Math.min(MAX_DT, (now - previousNanos) / 1_000_000_000f);
            previousNanos = now;
            if (dt <= 0f) dt = 0.016f;
            update(dt);
            drawFrame();
            long frameNanos = System.nanoTime() - now;
            long sleepMillis = Math.max(1L, 16L - frameNanos / 1_000_000L);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private void update(float dt) {
        if (gameOver) return;
        elapsed += dt;
        fireTimer -= dt;
        spawnTimer -= dt;

        float moveLength = (float) Math.sqrt(moveAxisX * moveAxisX + moveAxisY * moveAxisY);
        float inputX = moveLength > 1f ? moveAxisX / moveLength : moveAxisX;
        float inputY = moveLength > 1f ? moveAxisY / moveLength : moveAxisY;
        playerX = clamp(playerX + inputX * 260f * dt, PLAYER_RADIUS, WORLD_WIDTH - PLAYER_RADIUS);
        playerY = clamp(playerY + inputY * 260f * dt, PLAYER_RADIUS, WORLD_HEIGHT - PLAYER_RADIUS);

        if (firing && fireTimer <= 0f) {
            spawnShot();
            fireTimer = 0.16f;
        }
        if (spawnTimer <= 0f && enemies.size() < 14) {
            spawnEnemy();
            spawnTimer = Math.max(0.42f, 1.2f - elapsed * 0.01f);
        }

        for (Enemy enemy : enemies) {
            float dx = playerX - enemy.x;
            float dy = playerY - enemy.y;
            float distance = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
            enemy.x += dx / distance * enemy.speed * dt;
            enemy.y += dy / distance * enemy.speed * dt;
            if (distance < PLAYER_RADIUS + enemy.radius) {
                health -= 18f * dt;
                if (health <= 0f) {
                    health = 0f;
                    gameOver = true;
                }
            }
        }

        Iterator<Shot> shotIterator = shots.iterator();
        while (shotIterator.hasNext()) {
            Shot shot = shotIterator.next();
            shot.x += shot.dx * 620f * dt;
            shot.y += shot.dy * 620f * dt;
            shot.life -= dt;
            boolean removed = shot.life <= 0f || shot.x < -20f || shot.y < -20f
                    || shot.x > WORLD_WIDTH + 20f || shot.y > WORLD_HEIGHT + 20f;
            if (!removed) {
                Iterator<Enemy> enemyIterator = enemies.iterator();
                while (enemyIterator.hasNext()) {
                    Enemy enemy = enemyIterator.next();
                    float dx = shot.x - enemy.x;
                    float dy = shot.y - enemy.y;
                    float hitDistance = shot.radius + enemy.radius;
                    if (dx * dx + dy * dy <= hitDistance * hitDistance) {
                        enemyIterator.remove();
                        score += 10;
                        removed = true;
                        break;
                    }
                }
            }
            if (removed) shotIterator.remove();
        }
    }

    private void spawnShot() {
        float dx = aimX - playerX;
        float dy = aimY - playerY;
        float length = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
        shots.add(new Shot(playerX + dx / length * 26f, playerY + dy / length * 26f,
                dx / length, dy / length));
    }

    private void spawnEnemy() {
        int edge = random.nextInt(4);
        float x = edge == 0 ? -32f : edge == 1 ? WORLD_WIDTH + 32f : random.nextFloat() * WORLD_WIDTH;
        float y = edge == 2 ? -32f : edge == 3 ? WORLD_HEIGHT + 32f : random.nextFloat() * WORLD_HEIGHT;
        enemies.add(new Enemy(x, y, 38f + random.nextFloat() * 12f,
                42f + Math.min(30f, elapsed * 0.4f)));
    }

    private void drawFrame() {
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            if (canvas == null) return;
            calculateViewport(canvas.getWidth(), canvas.getHeight());
            canvas.drawColor(Color.rgb(5, 8, 18));
            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);
            drawWorld(canvas);
            canvas.restore();
        } finally {
            if (canvas != null) holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawWorld(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(8, 17, 32));
        canvas.drawRect(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT, paint);
        drawGrid(canvas);
        drawEnemies(canvas);
        drawShots(canvas);
        drawPlayer(canvas);
        drawHud(canvas);
        if (gameOver) drawGameOver(canvas);
    }

    private void drawGrid(Canvas canvas) {
        paint.setStrokeWidth(1f);
        paint.setColor(Color.rgb(18, 39, 59));
        for (int x = 0; x <= WORLD_WIDTH; x += 48) canvas.drawLine(x, 0, x, WORLD_HEIGHT, paint);
        for (int y = 0; y <= WORLD_HEIGHT; y += 48) canvas.drawLine(0, y, WORLD_WIDTH, y, paint);
        paint.setColor(Color.rgb(30, 70, 86));
        canvas.drawRect(22, 22, WORLD_WIDTH - 22, WORLD_HEIGHT - 22, paint);
    }

    private void drawPlayer(Canvas canvas) {
        paint.setColor(Color.rgb(53, 190, 240));
        canvas.drawCircle(playerX, playerY, PLAYER_RADIUS + 7f, paint);
        paint.setColor(Color.rgb(17, 33, 54));
        canvas.drawCircle(playerX, playerY, PLAYER_RADIUS, paint);
        paint.setColor(Color.rgb(117, 230, 255));
        PathUtil.drawShip(canvas, playerX, playerY, aimX, aimY, paint);
        if (blasterBitmap != null) {
            RectF icon = new RectF(playerX - 13f, playerY - 13f, playerX + 13f, playerY + 13f);
            paint.setAlpha(120);
            canvas.drawBitmap(blasterBitmap, null, icon, paint);
            paint.setAlpha(255);
        }
    }

    private void drawEnemies(Canvas canvas) {
        for (Enemy enemy : enemies) {
            if (scoutBitmap != null) {
                RectF target = new RectF(enemy.x - enemy.radius, enemy.y - enemy.radius,
                        enemy.x + enemy.radius, enemy.y + enemy.radius);
                canvas.drawBitmap(scoutBitmap, null, target, paint);
            } else {
                paint.setColor(Color.rgb(190, 61, 92));
                canvas.drawCircle(enemy.x, enemy.y, enemy.radius, paint);
                paint.setColor(Color.rgb(255, 130, 150));
                canvas.drawCircle(enemy.x, enemy.y, enemy.radius * 0.35f, paint);
            }
        }
    }

    private void drawShots(Canvas canvas) {
        for (Shot shot : shots) {
            paint.setColor(Color.rgb(255, 215, 92));
            canvas.drawCircle(shot.x, shot.y, shot.radius + 5f, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(shot.x, shot.y, shot.radius, paint);
        }
    }

    private void drawHud(Canvas canvas) {
        paint.setColor(Color.argb(210, 4, 10, 22));
        canvas.drawRoundRect(new RectF(18f, 14f, 384f, 76f), 12f, 12f, paint);
        textPaint.setTextSize(21f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("FIRST GAME", 34f, 40f, textPaint);
        textPaint.setTextSize(14f);
        textPaint.setColor(Color.rgb(145, 195, 218));
        canvas.drawText("BLASTER  •  SCORE " + score, 34f, 61f, textPaint);

        paint.setColor(Color.rgb(37, 45, 62));
        canvas.drawRoundRect(new RectF(WORLD_WIDTH - 238f, 20f, WORLD_WIDTH - 30f, 35f), 7f, 7f, paint);
        paint.setColor(Color.rgb(77, 220, 135));
        canvas.drawRoundRect(new RectF(WORLD_WIDTH - 238f, 20f,
                WORLD_WIDTH - 238f + 208f * health / 100f, 35f), 7f, 7f, paint);
        textPaint.setTextSize(13f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("HULL " + Math.round(health) + "%", WORLD_WIDTH - 230f, 57f, textPaint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(150, 111, 205, 239));
        canvas.drawCircle(90f, WORLD_HEIGHT - 90f, 56f, paint);
        canvas.drawCircle(WORLD_WIDTH - 90f, WORLD_HEIGHT - 90f, 56f, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(12f);
        textPaint.setColor(Color.rgb(147, 204, 223));
        canvas.drawText("MOVE", 71f, WORLD_HEIGHT - 86f, textPaint);
        canvas.drawText("AIM / FIRE", WORLD_WIDTH - 123f, WORLD_HEIGHT - 86f, textPaint);
    }

    private void drawGameOver(Canvas canvas) {
        paint.setColor(Color.argb(190, 0, 0, 0));
        canvas.drawRect(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(42f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("SHIP LOST", WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f - 18f, textPaint);
        textPaint.setTextSize(18f);
        textPaint.setColor(Color.rgb(155, 219, 240));
        canvas.drawText("Tap anywhere to restart", WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f + 24f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (gameOver && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)) {
            restartGame();
            return true;
        }
        int actionIndex = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            int pointerId = event.getPointerId(actionIndex);
            float worldX = toWorldX(event.getX(actionIndex));
            float worldY = toWorldY(event.getY(actionIndex));
            if (worldX < WORLD_WIDTH * 0.5f && movePointerId == -1) {
                movePointerId = pointerId;
                updateMove(worldX, worldY);
            } else if (aimPointerId == -1) {
                aimPointerId = pointerId;
                aimX = clamp(worldX, 0f, WORLD_WIDTH);
                aimY = clamp(worldY, 0f, WORLD_HEIGHT);
                firing = true;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (movePointerId != -1) {
                int index = event.findPointerIndex(movePointerId);
                if (index >= 0) updateMove(toWorldX(event.getX(index)), toWorldY(event.getY(index)));
            }
            if (aimPointerId != -1) {
                int index = event.findPointerIndex(aimPointerId);
                if (index >= 0) {
                    aimX = clamp(toWorldX(event.getX(index)), 0f, WORLD_WIDTH);
                    aimY = clamp(toWorldY(event.getY(index)), 0f, WORLD_HEIGHT);
                }
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
                || action == MotionEvent.ACTION_CANCEL) {
            int pointerId = event.getPointerId(actionIndex);
            if (pointerId == movePointerId) {
                movePointerId = -1;
                moveAxisX = 0f;
                moveAxisY = 0f;
            }
            if (pointerId == aimPointerId) {
                aimPointerId = -1;
                firing = false;
            }
            return true;
        }
        return true;
    }

    private void updateMove(float worldX, float worldY) {
        float centerX = 90f;
        float centerY = WORLD_HEIGHT - 90f;
        moveAxisX = (worldX - centerX) / 56f;
        moveAxisY = (worldY - centerY) / 56f;
        float length = (float) Math.sqrt(moveAxisX * moveAxisX + moveAxisY * moveAxisY);
        if (length > 1f) {
            moveAxisX /= length;
            moveAxisY /= length;
        }
    }

    private float toWorldX(float screenX) {
        return (screenX - offsetX) / scale;
    }

    private float toWorldY(float screenY) {
        return (screenY - offsetY) / scale;
    }

    private void restartGame() {
        playerX = WORLD_WIDTH / 2f;
        playerY = WORLD_HEIGHT / 2f;
        aimX = WORLD_WIDTH - 120f;
        aimY = WORLD_HEIGHT / 2f;
        health = 100f;
        score = 0;
        elapsed = 0f;
        fireTimer = 0f;
        spawnTimer = 0.2f;
        shots.clear();
        enemies.clear();
        gameOver = false;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Shot {
        float x;
        float y;
        final float dx;
        final float dy;
        final float radius = 7f;
        float life = 1.5f;

        Shot(float x, float y, float dx, float dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }
    }

    private static final class Enemy {
        float x;
        float y;
        final float radius;
        final float speed;

        Enemy(float x, float y, float radius, float speed) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speed = speed;
        }
    }

    private static final class PathUtil {
        static void drawShip(Canvas canvas, float x, float y, float targetX, float targetY, Paint paint) {
            float angle = (float) Math.atan2(targetY - y, targetX - x);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float tipX = x + cos * 28f;
            float tipY = y + sin * 28f;
            float leftX = x - cos * 17f + sin * 15f;
            float leftY = y - sin * 17f - cos * 15f;
            float rightX = x - cos * 17f - sin * 15f;
            float rightY = y - sin * 17f + cos * 15f;
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(tipX, tipY);
            path.lineTo(leftX, leftY);
            path.lineTo(x - cos * 7f, y - sin * 7f);
            path.lineTo(rightX, rightY);
            path.close();
            canvas.drawPath(path, paint);
        }
    }
}
