package com.traduvertgames.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Superfície RPG Android: exploração, combate, monstros, chefes, inventário
 * e equipamentos. A lógica AWT/Swing da versão desktop permanece separada.
 */
public final class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final int COLS = 32;
    private static final int ROWS = 20;
    private static final float TILE = 48f;
    private static final float WORLD_WIDTH = COLS * TILE;
    private static final float WORLD_HEIGHT = ROWS * TILE;
    private static final float PLAYER_RADIUS = 15f;
    private static final float MAX_DT = 0.04f;
    private static final char GRASS = 'g';
    private static final char PATH = 'p';
    private static final char WATER = 'w';
    private static final char WALL = 'x';
    private static final char TREE = 't';
    private static final char BRIDGE = 'b';

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random(0xB7A4F0L);
    private final char[][] tiles = new char[ROWS][COLS];
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<MagicBolt> bolts = new ArrayList<>();
    private final List<Item> inventory = new ArrayList<>();
    private final List<ItemPickup> pickups = new ArrayList<>();
    private final List<Npc> npcs = new ArrayList<>();
    private final GameSaveStore saveStore;
    private final Bitmap terrainAtlas;
    private final Bitmap baseAtlas;

    private Thread gameThread;
    private volatile boolean running;
    private long previousNanos;
    private float scale = 1f;
    private float offsetX;
    private float offsetY;
    private float cameraX;
    private float cameraY;

    private float playerX;
    private float playerY;
    private float aimX;
    private float aimY;
    private float moveAxisX;
    private float moveAxisY;
    private boolean actionHeld;
    private int movePointerId = -1;
    private int actionPointerId = -1;
    private float attackTimer;
    private float spawnTimer;
    private float messageTimer;
    private float health = 100f;
    private int level = 1;
    private int xp;
    private int gold = 25;
    private int defeated;
    private boolean chestOpened;
    private boolean dialogueVisible;
    private boolean inventoryVisible;
    private boolean gameOver;
    private int questStage;
    private int hunterKills;
    private boolean relicCollected;
    private boolean necromancerDefeated;
    private boolean titanDefeated;
    private String dialogueTitle = "AVA, COMANDANTE";
    private String dialogueLineOne = "A Bruma cresce ao norte.";
    private String dialogueLineTwo = "A Clareira precisa de você.";
    private String dialogueHint = "Toque em AÇÃO para encerrar o diálogo";
    private String message = "Explore a Clareira da Bruma";

    private Item equippedWeapon;
    private Item equippedArmor;
    private Item equippedAccessory;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        terrainAtlas = BitmapFactory.decodeResource(getResources(), R.drawable.terrain_atlas);
        baseAtlas = BitmapFactory.decodeResource(getResources(), R.drawable.base_out_atlas);
        saveStore = new GameSaveStore(context);
        if (!loadProgress(false)) resetRpg();
    }

    private void resetRpg() {
        buildMap();
        playerX = TILE * 4.5f;
        playerY = TILE * 10.5f;
        aimX = playerX + TILE;
        aimY = playerY;
        health = 100f;
        level = 1;
        xp = 0;
        gold = 25;
        defeated = 0;
        chestOpened = false;
        questStage = 0;
        hunterKills = 0;
        relicCollected = false;
        necromancerDefeated = false;
        titanDefeated = false;
        dialogueVisible = false;
        inventoryVisible = false;
        gameOver = false;
        attackTimer = 0f;
        spawnTimer = 4.5f;
        messageTimer = 4f;
        message = "Explore a Clareira da Bruma";
        enemies.clear();
        bolts.clear();
        pickups.clear();
        inventory.clear();
        equippedWeapon = new Item("Cajado de Cinzas", "ARMA", 8, 0, 4, false);
        equippedArmor = new Item("Manto da Clareira", "ARMADURA", 0, 5, 0, false);
        equippedAccessory = new Item("Anel Azul", "ACESSÓRIO", 0, 0, 3, false);
        inventory.add(equippedWeapon);
        inventory.add(equippedArmor);
        inventory.add(equippedAccessory);
        inventory.add(new Item("Poção Rubra", "CONSUMÍVEL", 0, 0, 0, true));
        inventory.add(new Item("Lâmina de Ferro", "ARMA", 13, 0, 0, false));
        inventory.add(new Item("Botas do Vento", "ARMADURA", 0, 3, 2, false));
        createNpcs();
        spawnInitialEnemies();
    }

    private void buildMap() {
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) tiles[y][x] = GRASS;
        }
        for (int x = 0; x < COLS; x++) {
            tiles[0][x] = TREE;
            tiles[ROWS - 1][x] = TREE;
        }
        for (int y = 0; y < ROWS; y++) {
            tiles[y][0] = TREE;
            tiles[y][COLS - 1] = TREE;
        }
        for (int x = 2; x < COLS - 2; x++) {
            tiles[10][x] = PATH;
            if (x > 9 && x < 16) tiles[10][x] = BRIDGE;
        }
        for (int y = 3; y < 11; y++) tiles[y][16] = PATH;
        for (int y = 4; y < 9; y++) {
            tiles[y][23] = WATER;
            tiles[y][24] = WATER;
        }
        for (int x = 21; x < 28; x++) {
            tiles[3][x] = WATER;
            tiles[4][x] = WATER;
        }
        for (int y = 13; y < 18; y++) {
            for (int x = 5; x < 9; x++) tiles[y][x] = WATER;
        }
        for (int y = 14; y < 18; y++) tiles[y][7] = BRIDGE;
        for (int y = 5; y < 9; y++) {
            for (int x = 26; x < 31; x++) tiles[y][x] = WALL;
        }
        for (int x = 26; x < 31; x++) tiles[9][x] = PATH;
        placeTrees(3, 2, 5, 6);
        placeTrees(11, 12, 15, 17);
        placeTrees(18, 13, 21, 18);
        tiles[10][4] = PATH;
        tiles[10][5] = PATH;
        tiles[10][6] = PATH;
        tiles[10][16] = BRIDGE;
    }

    private void placeTrees(int left, int top, int right, int bottom) {
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                if ((x * 7 + y * 11) % 5 == 0 && tiles[y][x] == GRASS) tiles[y][x] = TREE;
            }
        }
    }

    private void createNpcs() {
        npcs.clear();
        npcs.add(new Npc("AVA", "COMANDANTE", TILE * 15.5f, TILE * 10.5f, Color.rgb(255, 210, 120)));
        npcs.add(new Npc("ORIN", "CURANDEIRO", TILE * 8.5f, TILE * 11.5f, Color.rgb(132, 220, 188)));
        npcs.add(new Npc("ILYRA", "CARTÓGRAFA", TILE * 22.5f, TILE * 9.5f, Color.rgb(173, 162, 255)));
    }

    private void spawnInitialEnemies() {
        enemies.add(new Enemy(TILE * 12.5f, TILE * 7.5f, EnemyType.WOLF));
        enemies.add(new Enemy(TILE * 19.5f, TILE * 13.5f, EnemyType.SENTINEL));
        enemies.add(new Enemy(TILE * 25.5f, TILE * 11.5f, EnemyType.CULTIST));
        enemies.add(new Enemy(TILE * 8.5f, TILE * 8.5f, EnemyType.SPIDER));
        enemies.add(new Enemy(TILE * 21.5f, TILE * 15.5f, EnemyType.TROLL));
        if (!titanDefeated) enemies.add(new Enemy(TILE * 28.5f, TILE * 14.5f, EnemyType.BRUMA_TITAN));
        if (!necromancerDefeated) enemies.add(new Enemy(TILE * 24.5f, TILE * 17.5f, EnemyType.NECROMANCER));
    }

    public void resumeGame() {
        if (holder.getSurface().isValid() && gameThread == null) startLoop();
    }

    public void pauseGame() {
        stopLoop();
    }

    private synchronized void startLoop() {
        if (running) return;
        running = true;
        previousNanos = System.nanoTime();
        gameThread = new Thread(this, "first-game-rpg-android-loop");
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
        scale = Math.min(width / 1152f, height / 648f);
        offsetX = (width - 1152f * scale) * 0.5f;
        offsetY = (height - 648f * scale) * 0.5f;
    }

    @Override
    public void run() {
        while (running) {
            long now = System.nanoTime();
            float dt = Math.min(MAX_DT, Math.max(0.001f, (now - previousNanos) / 1_000_000_000f));
            previousNanos = now;
            update(dt);
            drawFrame();
            long frameNanos = System.nanoTime() - now;
            try {
                Thread.sleep(Math.max(1L, 16L - frameNanos / 1_000_000L));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private void update(float dt) {
        if (gameOver) return;
        messageTimer -= dt;
        if (inventoryVisible) return;
        attackTimer -= dt;
        spawnTimer -= dt;
        if (actionHeld && attackTimer <= 0f) {
            attackTimer = Math.max(0.22f, 0.42f - magicPower() * 0.01f);
            fireBolt();
        }
        float length = (float) Math.sqrt(moveAxisX * moveAxisX + moveAxisY * moveAxisY);
        float vx = length > 1f ? moveAxisX / length : moveAxisX;
        float vy = length > 1f ? moveAxisY / length : moveAxisY;
        movePlayer(vx * (180f + armorPower() * 2f) * dt, vy * (180f + armorPower() * 2f) * dt);
        cameraX = clamp(playerX - 576f, 0f, WORLD_WIDTH - 1152f);
        cameraY = clamp(playerY - 324f, 0f, WORLD_HEIGHT - 648f);
        updateEnemies(dt);
        updateBolts(dt);
        collectNearbyItems();
        if (spawnTimer <= 0f && enemies.size() < 10) {
            spawnWanderingEnemy();
            spawnTimer = 5.5f;
        }
        if (messageTimer <= 0f && !dialogueVisible) message = "BOLSA abre inventário • AÇÃO conversa e interage";
    }

    private void movePlayer(float dx, float dy) {
        float nextX = clamp(playerX + dx, TILE * 1.5f, WORLD_WIDTH - TILE * 1.5f);
        float nextY = clamp(playerY + dy, TILE * 1.5f, WORLD_HEIGHT - TILE * 1.5f);
        if (walkable(nextX, playerY)) playerX = nextX;
        if (walkable(playerX, nextY)) playerY = nextY;
    }

    private boolean walkable(float x, float y) {
        int col = (int) (x / TILE);
        int row = (int) (y / TILE);
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return false;
        return tiles[row][col] != WATER && tiles[row][col] != TREE && tiles[row][col] != WALL;
    }

    private void updateEnemies(float dt) {
        for (Enemy enemy : enemies) {
            float dx = playerX - enemy.x;
            float dy = playerY - enemy.y;
            float distance = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
            if (distance < TILE * (enemy.type.boss ? 9f : 7f)) {
                enemy.x += dx / distance * enemy.type.speed * dt;
                enemy.y += dy / distance * enemy.type.speed * dt;
            }
            if (distance < PLAYER_RADIUS + enemy.type.radius) {
                health -= Math.max(1f, enemy.type.damage - armorPower() * 0.7f) * dt;
                if (health <= 0f) {
                    health = 0f;
                    gameOver = true;
                    message = "A Clareira venceu você";
                }
            }
        }
    }

    private void updateBolts(float dt) {
        Iterator<MagicBolt> boltIterator = bolts.iterator();
        while (boltIterator.hasNext()) {
            MagicBolt bolt = boltIterator.next();
            bolt.x += bolt.dx * 360f * dt;
            bolt.y += bolt.dy * 360f * dt;
            bolt.life -= dt;
            boolean remove = bolt.life <= 0f || !insideWorld(bolt.x, bolt.y);
            if (!remove) {
                Iterator<Enemy> enemyIterator = enemies.iterator();
                while (enemyIterator.hasNext()) {
                    Enemy enemy = enemyIterator.next();
                    float dx = bolt.x - enemy.x;
                    float dy = bolt.y - enemy.y;
                    float hit = bolt.radius + enemy.type.radius;
                    if (dx * dx + dy * dy <= hit * hit) {
                        enemy.health -= bolt.damage;
                        remove = true;
                        if (enemy.health <= 0f) {
                            enemyIterator.remove();
                            onEnemyDefeated(enemy);
                        } else {
                            message = enemy.type.title + "  " + Math.max(0, Math.round(enemy.health)) + " PV";
                            messageTimer = 1.2f;
                        }
                        break;
                    }
                }
            }
            if (remove) boltIterator.remove();
        }
    }

    private void onEnemyDefeated(Enemy enemy) {
        defeated++;
        xp += enemy.type.xp;
        gold += enemy.type.gold;
        if (enemy.type == EnemyType.WOLF || enemy.type == EnemyType.SPIDER || enemy.type == EnemyType.CULTIST) {
            hunterKills++;
        }
        if (enemy.type == EnemyType.NECROMANCER) necromancerDefeated = true;
        if (enemy.type == EnemyType.BRUMA_TITAN) titanDefeated = true;
        Item drop = enemy.type.drop;
        if (drop != null) pickups.add(new ItemPickup(enemy.x, enemy.y, drop.copy()));
        if (enemy.type.boss) {
            message = "CHEFE derrotado: " + enemy.type.title + "  +" + enemy.type.xp + " XP";
            messageTimer = 4f;
        } else {
            message = enemy.type.title + " derrotado  +" + enemy.type.xp + " XP";
            messageTimer = 2.5f;
        }
        while (xp >= level * 60) {
            xp -= level * 60;
            level++;
            health = maxHealth();
            message = "Nível " + level + " alcançado";
            messageTimer = 3f;
        }
        updateQuestAfterCombat();
        saveProgress(false);
    }

    private void collectNearbyItems() {
        Iterator<ItemPickup> iterator = pickups.iterator();
        while (iterator.hasNext()) {
            ItemPickup pickup = iterator.next();
            if (distance(playerX, playerY, pickup.x, pickup.y) < TILE * 0.75f) {
                if (inventory.size() < 12 || pickup.item.consumable) {
                    addItem(pickup.item.copy());
                    iterator.remove();
                    message = pickup.item.name + " adicionada à bolsa";
                    messageTimer = 3f;
                } else {
                    message = "Bolsa cheia (12 espaços)";
                    messageTimer = 2f;
                }
            }
        }
    }

    private void spawnWanderingEnemy() {
        float x = TILE * (10 + random.nextInt(17));
        float y = TILE * (4 + random.nextInt(13));
        if (walkable(x, y)) {
            EnemyType[] types = {EnemyType.WOLF, EnemyType.SPIDER, EnemyType.CULTIST, EnemyType.TROLL};
            enemies.add(new Enemy(x, y, types[random.nextInt(types.length)]));
        }
    }

    private void fireBolt() {
        float dx = aimX - playerX;
        float dy = aimY - playerY;
        float distance = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
        float damage = 8f + level * 2f + attackPower() + magicPower() * 1.6f;
        bolts.add(new MagicBolt(playerX + dx / distance * 20f, playerY + dy / distance * 20f,
                dx / distance, dy / distance, damage));
    }

    private void interact() {
        if (inventoryVisible) return;
        float chestX = TILE * 27.5f;
        float chestY = TILE * 8.5f;
        for (Npc npc : npcs) {
            if (distance(playerX, playerY, npc.x, npc.y) < TILE * 2f) {
                openNpcDialogue(npc);
                return;
            }
        }
        if (!relicCollected && distance(playerX, playerY, TILE * 20.5f, TILE * 6.5f) < TILE * 2f) {
            relicCollected = true;
            addItem(new Item("Fragmento da Aurora", "ACESSÓRIO", 2, 1, 6, false));
            message = "Relíquia recuperada: Fragmento da Aurora";
            messageTimer = 4f;
            updateQuestAfterCombat();
            saveProgress(false);
        } else if (!chestOpened && distance(playerX, playerY, chestX, chestY) < TILE * 2f) {
            chestOpened = true;
            gold += 50;
            addItem(new Item("Amuleto da Bruma", "ACESSÓRIO", 2, 2, 7, false));
            message = "Baú aberto: +50 ouro e Amuleto da Bruma";
            messageTimer = 4f;
            saveProgress(false);
        } else if (distance(playerX, playerY, TILE * 16f, TILE * 10f) < TILE * 2f) {
            message = "Ponte para a Fortaleza das Cinzas";
            messageTimer = 3f;
        }
    }

    private void openNpcDialogue(Npc npc) {
        dialogueVisible = true;
        dialogueTitle = npc.name + ", " + npc.role;
        dialogueHint = "Toque em AÇÃO para encerrar o diálogo";
        if ("AVA".equals(npc.name)) {
            advanceAvaQuest();
        } else if ("ORIN".equals(npc.name)) {
            dialogueLineOne = health < maxHealth() * 0.55f
                    ? "Sua aura está ferida. Use uma Poção Rubra quando" : "A Bruma não perdoa os imprudentes. Mantenha";
            dialogueLineTwo = health < maxHealth() * 0.55f
                    ? "precisar; sua bolsa guarda tudo o que coleta." : "poções na bolsa e armadura equipada.";
        } else {
            dialogueLineOne = relicCollected
                    ? "O Fragmento da Aurora reagiu ao seu toque. Agora" : "Siga o reflexo das águas ao norte. Uma relíquia";
            dialogueLineTwo = relicCollected
                    ? "leve-o a Ava e descubra onde a Bruma nasceu." : "antiga repousa perto da margem protegida.";
        }
    }

    private void advanceAvaQuest() {
        if (questStage == 0) {
            questStage = 1;
            dialogueLineOne = "Primeiro, prove que consegue sobreviver à Clareira.";
            dialogueLineTwo = "Derrote 3 criaturas da Bruma e volte a mim.";
        } else if (questStage == 1 && hunterKills >= 3) {
            questStage = 2;
            gold += 20;
            dialogueLineOne = "A clareira reconhece sua coragem. Receba 20 ouro.";
            dialogueLineTwo = "Agora encontre o Fragmento da Aurora junto à água.";
        } else if (questStage == 2 && relicCollected) {
            questStage = 3;
            dialogueLineOne = "A relíquia aponta para o Necromante do Véu.";
            dialogueLineTwo = "Derrote-o antes que ele fortaleça o Titã.";
        } else if (questStage == 3 && necromancerDefeated) {
            questStage = 4;
            dialogueLineOne = "O véu caiu. O Titã da Bruma está vulnerável.";
            dialogueLineTwo = "Atravesse a fortaleza e encerre a expedição.";
        } else if (questStage == 4 && titanDefeated) {
            questStage = 5;
            gold += 100;
            addItem(new Item("Selo da Clareira", "ACESSÓRIO", 5, 5, 12, false));
            dialogueLineOne = "A Bruma se dissipou. Você protegeu a Clareira.";
            dialogueLineTwo = "Receba 100 ouro e o Selo da Clareira.";
        } else {
            dialogueLineOne = currentQuest();
            dialogueLineTwo = questDetail();
        }
        message = currentQuest();
        messageTimer = 4f;
        saveProgress(false);
    }

    private void updateQuestAfterCombat() {
        if (questStage == 1 && hunterKills >= 3) {
            message = "Objetivo concluído: retorne a Ava";
            messageTimer = 4f;
        } else if (questStage == 3 && necromancerDefeated) {
            message = "O Necromante caiu: retorne a Ava";
            messageTimer = 4f;
        } else if (questStage == 4 && titanDefeated) {
            message = "O Titã caiu: a Clareira está salva";
            messageTimer = 4f;
        }
    }

    private String currentQuest() {
        switch (questStage) {
            case 0: return "Fale com Ava no acampamento";
            case 1: return "Caçada da Bruma: " + Math.min(3, hunterKills) + "/3 criaturas";
            case 2: return "Recupere o Fragmento da Aurora";
            case 3: return "Derrote o Necromante do Véu";
            case 4: return "Derrote o Titã da Bruma";
            default: return "Expedição concluída: proteja a Clareira";
        }
    }

    private String questDetail() {
        switch (questStage) {
            case 0: return "A comandante aguarda perto da ponte.";
            case 1: return "Lobos, aranhas e cultistas contam para a caçada.";
            case 2: return "Ilyra marcou o local próximo às águas do norte.";
            case 3: return "O Necromante espreita no sul da fortaleza.";
            case 4: return "O Titã protege a saída nordeste.";
            default: return "Converse com os NPCs ou explore livremente.";
        }
    }

    private void addItem(Item item) {
        for (Item existing : inventory) {
            if (existing.name.equals(item.name) && existing.consumable) {
                existing.quantity += item.quantity;
                return;
            }
        }
        if (inventory.size() < 12) inventory.add(item);
    }

    public void persistProgress() {
        saveProgress(false);
    }

    private void saveProgress(boolean feedback) {
        GameSaveStore.SaveData data = new GameSaveStore.SaveData();
        data.playerX = playerX;
        data.playerY = playerY;
        data.health = health;
        data.level = level;
        data.xp = xp;
        data.gold = gold;
        data.defeated = defeated;
        data.chestOpened = chestOpened;
        data.questStage = questStage;
        data.hunterKills = hunterKills;
        data.relicCollected = relicCollected;
        data.necromancerDefeated = necromancerDefeated;
        data.titanDefeated = titanDefeated;
        data.weaponName = equippedWeapon == null ? "" : equippedWeapon.name;
        data.armorName = equippedArmor == null ? "" : equippedArmor.name;
        data.accessoryName = equippedAccessory == null ? "" : equippedAccessory.name;
        for (Item item : inventory) {
            GameSaveStore.ItemData savedItem = new GameSaveStore.ItemData();
            savedItem.name = item.name;
            savedItem.slot = item.slot;
            savedItem.attack = item.attack;
            savedItem.armor = item.armor;
            savedItem.magic = item.magic;
            savedItem.consumable = item.consumable;
            savedItem.quantity = item.quantity;
            data.items.add(savedItem);
        }
        saveStore.save(data);
        if (feedback) {
            message = "Progresso salvo nesta aventura";
            messageTimer = 2.8f;
        }
    }

    private boolean loadProgress(boolean feedback) {
        GameSaveStore.SaveData data = saveStore.load();
        if (data == null) {
            if (feedback) {
                message = "Nenhum progresso salvo encontrado";
                messageTimer = 2.8f;
            }
            return false;
        }
        buildMap();
        enemies.clear();
        bolts.clear();
        pickups.clear();
        inventory.clear();
        createNpcs();
        for (GameSaveStore.ItemData savedItem : data.items) {
            Item item = new Item(savedItem.name, savedItem.slot, savedItem.attack, savedItem.armor,
                    savedItem.magic, savedItem.consumable);
            item.quantity = savedItem.quantity;
            inventory.add(item);
        }
        if (inventory.isEmpty()) return false;
        playerX = clamp(data.playerX, TILE * 1.5f, WORLD_WIDTH - TILE * 1.5f);
        playerY = clamp(data.playerY, TILE * 1.5f, WORLD_HEIGHT - TILE * 1.5f);
        aimX = playerX + TILE;
        aimY = playerY;
        level = data.level;
        xp = data.xp;
        gold = data.gold;
        defeated = data.defeated;
        chestOpened = data.chestOpened;
        questStage = data.questStage;
        hunterKills = data.hunterKills;
        relicCollected = data.relicCollected;
        necromancerDefeated = data.necromancerDefeated;
        titanDefeated = data.titanDefeated;
        equippedWeapon = findItem(data.weaponName);
        equippedArmor = findItem(data.armorName);
        equippedAccessory = findItem(data.accessoryName);
        health = Math.min(maxHealth(), Math.max(1f, data.health));
        dialogueVisible = false;
        inventoryVisible = false;
        gameOver = false;
        actionHeld = false;
        moveAxisX = 0f;
        moveAxisY = 0f;
        spawnInitialEnemies();
        message = feedback ? "Progresso carregado" : "Progresso retomado";
        messageTimer = 3f;
        return true;
    }

    private Item findItem(String name) {
        for (Item item : inventory) {
            if (item.name.equals(name)) return item;
        }
        return null;
    }

    private void equipItem(Item item) {
        if (item == null) return;
        if (item.consumable) {
            if (item.quantity > 0 && health < maxHealth()) {
                item.quantity--;
                health = Math.min(maxHealth(), health + 38f);
                message = "Poção usada: vida restaurada";
                messageTimer = 2.5f;
            } else if (health >= maxHealth()) {
                message = "A vida já está cheia";
                messageTimer = 2f;
            }
            return;
        }
        if ("ARMA".equals(item.slot)) equippedWeapon = item;
        else if ("ARMADURA".equals(item.slot)) equippedArmor = item;
        else if ("ACESSÓRIO".equals(item.slot)) equippedAccessory = item;
        message = item.name + " equipado";
        messageTimer = 2.5f;
    }

    private int attackPower() {
        return equippedWeapon == null ? 0 : equippedWeapon.attack;
    }

    private int armorPower() {
        return equippedArmor == null ? 0 : equippedArmor.armor;
    }

    private int magicPower() {
        return (equippedWeapon == null ? 0 : equippedWeapon.magic)
                + (equippedAccessory == null ? 0 : equippedAccessory.magic);
    }

    private float maxHealth() {
        return 100f + armorPower() * 3f + (equippedAccessory == null ? 0 : equippedAccessory.armor * 2f);
    }

    private void drawFrame() {
        if (!holder.getSurface().isValid()) return;
        Canvas canvas = null;
        try {
            canvas = holder.lockCanvas();
            if (canvas == null) return;
            calculateViewport(canvas.getWidth(), canvas.getHeight());
            canvas.drawColor(Color.rgb(8, 12, 22));
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
        canvas.save();
        canvas.clipRect(0f, 0f, 1152f, 648f);
        canvas.translate(-cameraX, -cameraY);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) drawTile(canvas, col, row, tiles[row][col]);
        }
        drawWorldObjects(canvas);
        drawPickups(canvas);
        drawEnemies(canvas);
        drawBolts(canvas);
        drawPlayer(canvas);
        canvas.restore();
        drawHud(canvas);
        if (dialogueVisible) drawDialogue(canvas);
        if (inventoryVisible) drawInventory(canvas);
        if (gameOver) drawGameOver(canvas);
    }

    private void drawTile(Canvas canvas, int col, int row, char type) {
        float left = col * TILE;
        float top = row * TILE;
        RectF target = new RectF(left, top, left + TILE, top + TILE);
        if (type == WATER) {
            drawAtlas(canvas, terrainAtlas, new Rect(320, 288, 384, 352), target);
        } else if (type == PATH) {
            drawAtlas(canvas, terrainAtlas, new Rect(256, 640, 320, 704), target);
        } else if (type == BRIDGE) {
            drawAtlas(canvas, baseAtlas, new Rect(416, 512, 480, 576), target);
        } else if (type == WALL) {
            drawAtlas(canvas, baseAtlas, new Rect(800, 704, 864, 768), target);
        } else if (type == TREE) {
            drawAtlas(canvas, terrainAtlas, new Rect(800, 384, 864, 448), target);
        } else {
            drawAtlas(canvas, terrainAtlas, new Rect(192, 640, 256, 704), target);
        }
    }

    private void drawWorldObjects(Canvas canvas) {
        drawAtlas(canvas, baseAtlas, new Rect(704, 704, 832, 832),
                new RectF(TILE * 26f, TILE * 5f, TILE * 30f, TILE * 9f));
        drawAtlas(canvas, baseAtlas, new Rect(416, 512, 544, 640),
                new RectF(TILE * 14f, TILE * 9f, TILE * 18f, TILE * 11f));
        for (Npc npc : npcs) drawNpc(canvas, npc);
        if (!relicCollected) {
            float relicX = TILE * 20.5f;
            float relicY = TILE * 6.5f;
            paint.setColor(Color.argb(100, 106, 222, 255));
            canvas.drawCircle(relicX, relicY, 23f, paint);
            paint.setColor(Color.rgb(198, 244, 255));
            canvas.drawCircle(relicX, relicY, 11f, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(relicX, relicY - 4f, 4f, paint);
        }
        if (!chestOpened) {
            drawAtlas(canvas, baseAtlas, new Rect(640, 576, 704, 640),
                    new RectF(TILE * 27f, TILE * 8f, TILE * 28f, TILE * 9f));
        }
    }

    private void drawNpc(Canvas canvas, Npc npc) {
        drawCharacter(canvas, npc.x, npc.y, npc.color);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(11f);
        textPaint.setColor(Color.rgb(255, 231, 154));
        canvas.drawText(npc.name, npc.x, npc.y - 31f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPickups(Canvas canvas) {
        for (ItemPickup pickup : pickups) {
            paint.setColor(pickup.item.consumable ? Color.rgb(239, 73, 87) : Color.rgb(245, 200, 86));
            canvas.drawCircle(pickup.x, pickup.y, 12f, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(pickup.x, pickup.y, 4f, paint);
        }
    }

    private void drawEnemies(Canvas canvas) {
        for (Enemy enemy : enemies) {
            drawEnemy(canvas, enemy);
            paint.setColor(Color.argb(220, 115, 33, 57));
            canvas.drawRoundRect(new RectF(enemy.x - enemy.type.radius, enemy.y - enemy.type.radius - 10f,
                    enemy.x + enemy.type.radius, enemy.y - enemy.type.radius - 4f), 3f, 3f, paint);
            paint.setColor(enemy.type.boss ? Color.rgb(255, 197, 67) : Color.rgb(247, 91, 106));
            float ratio = Math.max(0f, enemy.health / enemy.type.maxHealth);
            canvas.drawRoundRect(new RectF(enemy.x - enemy.type.radius, enemy.y - enemy.type.radius - 10f,
                    enemy.x - enemy.type.radius + enemy.type.radius * 2f * ratio,
                    enemy.y - enemy.type.radius - 4f), 3f, 3f, paint);
        }
    }

    private void drawEnemy(Canvas canvas, Enemy enemy) {
        float x = enemy.x;
        float y = enemy.y;
        float radius = enemy.type.radius;
        paint.setColor(Color.argb(100, 0, 0, 0));
        canvas.drawOval(new RectF(x - radius - 5f, y + radius - 3f, x + radius + 5f, y + radius + 8f), paint);
        paint.setColor(enemy.type.color);
        if (enemy.type.boss) {
            canvas.drawCircle(x, y, radius + 11f, paint);
            paint.setColor(Color.argb(105, 255, 60, 90));
            canvas.drawCircle(x, y, radius + 19f, paint);
            paint.setColor(enemy.type.color);
            canvas.drawCircle(x, y, radius, paint);
            paint.setColor(Color.rgb(255, 225, 105));
            canvas.drawCircle(x - radius * 0.35f, y - 4f, 5f, paint);
            canvas.drawCircle(x + radius * 0.35f, y - 4f, 5f, paint);
            paint.setColor(Color.rgb(70, 20, 35));
            canvas.drawRect(x - radius * 0.45f, y + radius * 0.2f, x + radius * 0.45f, y + radius * 0.45f, paint);
        } else if (enemy.type == EnemyType.SPIDER) {
            canvas.drawCircle(x, y, radius, paint);
            paint.setStrokeWidth(4f);
            for (int i = 0; i < 4; i++) {
                float legY = y - radius * 0.6f + i * radius * 0.4f;
                canvas.drawLine(x - radius, legY, x - radius * 1.6f, legY - 10f, paint);
                canvas.drawLine(x + radius, legY, x + radius * 1.6f, legY - 10f, paint);
            }
            paint.setColor(Color.WHITE);
            canvas.drawCircle(x - 6f, y - 5f, 3f, paint);
            canvas.drawCircle(x + 6f, y - 5f, 3f, paint);
        } else if (enemy.type == EnemyType.TROLL) {
            canvas.drawRoundRect(new RectF(x - radius, y - radius, x + radius, y + radius + 6f), 12f, 12f, paint);
            paint.setColor(Color.rgb(255, 222, 154));
            canvas.drawCircle(x - 7f, y - 6f, 4f, paint);
            canvas.drawCircle(x + 7f, y - 6f, 4f, paint);
        } else {
            canvas.drawCircle(x, y, radius, paint);
            paint.setColor(Color.rgb(255, 227, 162));
            canvas.drawCircle(x - radius * 0.3f, y - 4f, 4f, paint);
            canvas.drawCircle(x + radius * 0.3f, y - 4f, 4f, paint);
            paint.setColor(Color.rgb(35, 24, 46));
            canvas.drawCircle(x, y + 8f, radius * 0.3f, paint);
        }
    }

    private void drawBolts(Canvas canvas) {
        for (MagicBolt bolt : bolts) {
            paint.setColor(Color.rgb(116, 225, 255));
            canvas.drawCircle(bolt.x, bolt.y, bolt.radius + 5f, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(bolt.x, bolt.y, bolt.radius, paint);
        }
    }

    private void drawPlayer(Canvas canvas) {
        int playerColor = equippedArmor == null ? Color.rgb(105, 196, 255) : Color.rgb(154, 131, 255);
        drawCharacter(canvas, playerX, playerY, playerColor);
        float dx = aimX - playerX;
        float dy = aimY - playerY;
        float distance = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
        paint.setColor(Color.argb(170, 211, 246, 255));
        paint.setStrokeWidth(4f);
        canvas.drawLine(playerX + dx / distance * 12f, playerY + dy / distance * 12f,
                playerX + dx / distance * 25f, playerY + dy / distance * 25f, paint);
    }

    private void drawCharacter(Canvas canvas, float x, float y, int color) {
        paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawOval(new RectF(x - 21f, y + 13f, x + 21f, y + 24f), paint);
        paint.setColor(Color.rgb(30, 42, 57));
        canvas.drawCircle(x, y, 19f, paint);
        paint.setColor(color);
        canvas.drawCircle(x, y - 4f, 14f, paint);
        paint.setColor(Color.rgb(255, 224, 176));
        canvas.drawCircle(x, y - 10f, 8f, paint);
        paint.setColor(Color.rgb(33, 49, 77));
        canvas.drawRect(x - 13f, y - 20f, x + 13f, y - 14f, paint);
    }

    private void drawHud(Canvas canvas) {
        paint.setColor(Color.argb(224, 8, 15, 29));
        canvas.drawRoundRect(new RectF(18f, 14f, 535f, 105f), 12f, 12f, paint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(22f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("CLAREIRA DA BRUMA", 34f, 42f, textPaint);
        textPaint.setTextSize(14f);
        textPaint.setColor(Color.rgb(164, 211, 226));
        canvas.drawText("NÍVEL " + level + "   XP " + xp + "/" + (level * 60) + "   OURO " + gold, 34f, 65f, textPaint);
        textPaint.setTextSize(12f);
        textPaint.setColor(Color.rgb(201, 185, 239));
        canvas.drawText("ATQ " + attackPower() + "   DEF " + armorPower() + "   MAG " + magicPower() + "   DERROTADOS " + defeated, 34f, 86f, textPaint);
        paint.setColor(Color.rgb(42, 54, 69));
        canvas.drawRoundRect(new RectF(34f, 93f, 234f, 101f), 5f, 5f, paint);
        paint.setColor(Color.rgb(83, 219, 137));
        canvas.drawRoundRect(new RectF(34f, 93f, 34f + 200f * Math.min(1f, health / maxHealth()), 101f), 5f, 5f, paint);

        drawUtilityButton(canvas, 250f, "SALVAR", Color.rgb(62, 116, 104));
        drawUtilityButton(canvas, 362f, "CARREGAR", Color.rgb(64, 83, 136));

        paint.setColor(inventoryVisible ? Color.rgb(80, 105, 176) : Color.argb(220, 8, 15, 29));
        canvas.drawRoundRect(new RectF(570f, 18f, 705f, 86f), 12f, 12f, paint);
        textPaint.setTextSize(18f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("BOLSA", 600f, 48f, textPaint);
        textPaint.setTextSize(12f);
        textPaint.setColor(Color.rgb(178, 212, 226));
        canvas.drawText(inventory.size() + "/12 itens", 598f, 69f, textPaint);

        paint.setColor(Color.argb(220, 8, 15, 29));
        canvas.drawRoundRect(new RectF(730f, 18f, 1134f, 86f), 12f, 12f, paint);
        textPaint.setTextSize(15f);
        textPaint.setColor(Color.rgb(255, 226, 145));
        canvas.drawText("MISSÃO PRINCIPAL", 750f, 42f, textPaint);
        textPaint.setTextSize(14f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(currentQuest(), 750f, 65f, textPaint);

        drawBossBar(canvas);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(160, 133, 215, 232));
        canvas.drawCircle(92f, 570f, 58f, paint);
        canvas.drawCircle(1060f, 570f, 58f, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(12f);
        textPaint.setColor(Color.rgb(166, 216, 230));
        canvas.drawText("MOVER", 67f, 574f, textPaint);
        canvas.drawText("AÇÃO", 1037f, 574f, textPaint);
        if (messageTimer > 0f || dialogueVisible) {
            paint.setColor(Color.argb(210, 5, 12, 23));
            canvas.drawRoundRect(new RectF(250f, 574f, 902f, 626f), 12f, 12f, paint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(15f);
            textPaint.setColor(Color.WHITE);
            canvas.drawText(message, 576f, 606f, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
        }
    }

    private void drawUtilityButton(Canvas canvas, float left, String label, int color) {
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(left, 18f, left + 98f, 58f), 10f, 10f, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(11f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(label, left + 49f, 43f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawBossBar(Canvas canvas) {
        Enemy boss = null;
        for (Enemy enemy : enemies) {
            if (enemy.type.boss) {
                boss = enemy;
                break;
            }
        }
        if (boss == null) return;
        paint.setColor(Color.argb(220, 20, 8, 22));
        canvas.drawRoundRect(new RectF(300f, 105f, 852f, 135f), 8f, 8f, paint);
        paint.setColor(Color.rgb(103, 25, 50));
        canvas.drawRoundRect(new RectF(316f, 115f, 836f, 125f), 5f, 5f, paint);
        paint.setColor(Color.rgb(247, 79, 103));
        canvas.drawRoundRect(new RectF(316f, 115f, 316f + 520f * Math.max(0f, boss.health / boss.type.maxHealth), 125f), 5f, 5f, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(13f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(boss.type.title + "  —  CHEFE", 576f, 112f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawDialogue(Canvas canvas) {
        paint.setColor(Color.argb(235, 19, 24, 38));
        canvas.drawRoundRect(new RectF(130f, 190f, 1022f, 400f), 18f, 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.rgb(212, 176, 91));
        canvas.drawRoundRect(new RectF(130f, 190f, 1022f, 400f), 18f, 18f, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(24f);
        textPaint.setColor(Color.rgb(255, 220, 134));
        canvas.drawText(dialogueTitle, 170f, 235f, textPaint);
        textPaint.setTextSize(20f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(dialogueLineOne, 170f, 285f, textPaint);
        canvas.drawText(dialogueLineTwo, 170f, 318f, textPaint);
        textPaint.setTextSize(15f);
        textPaint.setColor(Color.rgb(170, 209, 222));
        canvas.drawText(dialogueHint, 170f, 365f, textPaint);
    }

    private void drawInventory(Canvas canvas) {
        paint.setColor(Color.argb(220, 0, 0, 0));
        canvas.drawRect(0f, 0f, 1152f, 648f, paint);
        paint.setColor(Color.rgb(18, 25, 43));
        canvas.drawRoundRect(new RectF(135f, 105f, 1017f, 550f), 18f, 18f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.rgb(106, 162, 203));
        canvas.drawRoundRect(new RectF(135f, 105f, 1017f, 550f), 18f, 18f, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(27f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("INVENTÁRIO E EQUIPAMENTOS", 175f, 148f, textPaint);
        textPaint.setTextSize(14f);
        textPaint.setColor(Color.rgb(171, 211, 229));
        canvas.drawText("Toque em um item para equipar ou usar", 175f, 173f, textPaint);
        drawEquipmentSlot(canvas, 175f, 195f, 390f, 267f, "ARMA", equippedWeapon);
        drawEquipmentSlot(canvas, 410f, 195f, 625f, 267f, "ARMADURA", equippedArmor);
        drawEquipmentSlot(canvas, 645f, 195f, 970f, 267f, "ACESSÓRIO", equippedAccessory);
        for (int index = 0; index < inventory.size(); index++) {
            int column = index % 3;
            int row = index / 3;
            float left = 175f + column * 270f;
            float top = 292f + row * 60f;
            drawItemSlot(canvas, left, top, left + 250f, top + 50f, inventory.get(index));
        }
        textPaint.setTextSize(13f);
        textPaint.setColor(Color.rgb(185, 198, 213));
        canvas.drawText("Atributos: ATQ " + attackPower() + "  DEF " + armorPower() + "  MAG " + magicPower()
                + "  Vida máxima " + Math.round(maxHealth()), 175f, 530f, textPaint);
    }

    private void drawEquipmentSlot(Canvas canvas, float left, float top, float right, float bottom, String slot, Item item) {
        paint.setColor(Color.rgb(35, 45, 70));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 10f, 10f, paint);
        textPaint.setTextSize(11f);
        textPaint.setColor(Color.rgb(139, 197, 221));
        canvas.drawText(slot, left + 12f, top + 18f, textPaint);
        textPaint.setTextSize(15f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(item == null ? "vazio" : item.name, left + 12f, top + 43f, textPaint);
    }

    private void drawItemSlot(Canvas canvas, float left, float top, float right, float bottom, Item item) {
        paint.setColor(item.consumable ? Color.rgb(73, 42, 55) : Color.rgb(43, 57, 79));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 8f, 8f, paint);
        textPaint.setTextSize(14f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(item.name, left + 12f, top + 21f, textPaint);
        textPaint.setTextSize(11f);
        textPaint.setColor(Color.rgb(172, 203, 218));
        String stats = item.consumable ? "USAR  quantidade " + item.quantity
                : item.slot + "  ATQ+" + item.attack + " DEF+" + item.armor + " MAG+" + item.magic;
        canvas.drawText(stats, left + 12f, top + 40f, textPaint);
    }

    private void drawGameOver(Canvas canvas) {
        paint.setColor(Color.argb(205, 0, 0, 0));
        canvas.drawRect(0f, 0f, 1152f, 648f, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(44f);
        textPaint.setColor(Color.WHITE);
        canvas.drawText("HERÓI DERROTADO", 576f, 292f, textPaint);
        textPaint.setTextSize(19f);
        textPaint.setColor(Color.rgb(183, 220, 232));
        canvas.drawText("Toque para voltar à Clareira", 576f, 340f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        float x = toWorldX(event.getX(index));
        float y = toWorldY(event.getY(index));
        if (gameOver && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN)) {
            if (!loadProgress(true)) resetRpg();
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            if (dialogueVisible) {
                dialogueVisible = false;
                return true;
            }
            if (inventoryVisible) {
                if (x > 550f && x < 730f && y < 110f) {
                    inventoryVisible = false;
                } else {
                    handleInventoryTap(x, y);
                }
                return true;
            }
            if (x > 250f && x < 348f && y < 90f) {
                saveProgress(true);
                return true;
            }
            if (x > 362f && x < 460f && y < 90f) {
                loadProgress(true);
                return true;
            }
            if (x > 550f && x < 730f && y < 110f) {
                inventoryVisible = true;
                actionHeld = false;
                return true;
            }
            int pointerId = event.getPointerId(index);
            if (x < 360f && movePointerId == -1) {
                movePointerId = pointerId;
                updateMove(x, y);
            } else if (actionPointerId == -1) {
                actionPointerId = pointerId;
                aimX = clamp(x + cameraX, 0f, WORLD_WIDTH);
                aimY = clamp(y + cameraY, 0f, WORLD_HEIGHT);
                actionHeld = true;
                if (x > 970f && y > 480f) interact();
            }
            return true;
        }
        if (inventoryVisible) return true;
        if (action == MotionEvent.ACTION_MOVE) {
            if (movePointerId != -1) {
                int moveIndex = event.findPointerIndex(movePointerId);
                if (moveIndex >= 0) updateMove(toWorldX(event.getX(moveIndex)), toWorldY(event.getY(moveIndex)));
            }
            if (actionPointerId != -1) {
                int actionIndex = event.findPointerIndex(actionPointerId);
                if (actionIndex >= 0) {
                    aimX = clamp(toWorldX(event.getX(actionIndex)) + cameraX, 0f, WORLD_WIDTH);
                    aimY = clamp(toWorldY(event.getY(actionIndex)) + cameraY, 0f, WORLD_HEIGHT);
                }
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
            int pointerId = event.getPointerId(index);
            if (pointerId == movePointerId) {
                movePointerId = -1;
                moveAxisX = 0f;
                moveAxisY = 0f;
            }
            if (pointerId == actionPointerId) {
                actionPointerId = -1;
                actionHeld = false;
            }
            return true;
        }
        return true;
    }

    private void handleInventoryTap(float x, float y) {
        if (x < 160f || x > 1000f || y < 280f || y > 530f) return;
        int column = (int) ((x - 175f) / 270f);
        int row = (int) ((y - 292f) / 60f);
        if (column < 0 || column > 2 || row < 0) return;
        int index = row * 3 + column;
        if (index >= 0 && index < inventory.size()) equipItem(inventory.get(index));
    }

    private void updateMove(float x, float y) {
        float dx = (x - 92f) / 58f;
        float dy = (y - 570f) / 58f;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        moveAxisX = length > 1f ? dx / length : dx;
        moveAxisY = length > 1f ? dy / length : dy;
    }

    private float toWorldX(float screenX) {
        return screenX / scale;
    }

    private float toWorldY(float screenY) {
        return screenY / scale;
    }

    private void drawAtlas(Canvas canvas, Bitmap atlas, Rect source, RectF target) {
        if (atlas != null) canvas.drawBitmap(atlas, source, target, paint);
        else {
            paint.setColor(Color.rgb(62, 107, 76));
            canvas.drawRect(target, paint);
        }
    }

    private static float distance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static boolean insideWorld(float x, float y) {
        return x >= 0f && y >= 0f && x <= WORLD_WIDTH && y <= WORLD_HEIGHT;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum EnemyType {
        WOLF("Lobo da Bruma", 34f, 22f, 42f, 20, 8, Color.rgb(177, 95, 118), false,
                new Item("Presa Sombria", "ACESSÓRIO", 1, 0, 1, false)),
        SPIDER("Aranha de Cristal", 28f, 25f, 34f, 24, 6, Color.rgb(117, 192, 170), false,
                new Item("Veneno Cristalino", "ACESSÓRIO", 3, 0, 2, false)),
        SENTINEL("Sentinela de Pedra", 42f, 18f, 58f, 32, 12, Color.rgb(104, 139, 177), false,
                new Item("Fragmento de Pedra", "ARMADURA", 0, 4, 0, false)),
        CULTIST("Cultista da Cinza", 30f, 25f, 46f, 28, 10, Color.rgb(161, 86, 181), false,
                new Item("Faixa Arcana", "ACESSÓRIO", 0, 1, 4, false)),
        TROLL("Troll do Pântano", 53f, 16f, 96f, 45, 18, Color.rgb(84, 158, 103), false,
                new Item("Pele de Troll", "ARMADURA", 0, 7, 0, false)),
        NECROMANCER("Necromante do Véu", 42f, 20f, 130f, 85, 25, Color.rgb(124, 76, 172), true,
                new Item("Coroa do Véu", "ACESSÓRIO", 4, 2, 10, false)),
        BRUMA_TITAN("Titã da Bruma", 68f, 13f, 320f, 150, 70, Color.rgb(176, 55, 83), true,
                new Item("Núcleo do Titã", "ARMA", 22, 0, 13, false));

        final String title;
        final float radius;
        final float speed;
        final float maxHealth;
        final int xp;
        final int gold;
        final int color;
        final boolean boss;
        final Item drop;
        final float damage;

        EnemyType(String title, float radius, float speed, float maxHealth, int xp, int gold,
                  int color, boolean boss, Item drop) {
            this.title = title;
            this.radius = radius;
            this.speed = speed;
            this.maxHealth = maxHealth;
            this.xp = xp;
            this.gold = gold;
            this.color = color;
            this.boss = boss;
            this.drop = drop;
            this.damage = boss ? 22f : 12f;
        }
    }

    private static final class Enemy {
        float x;
        float y;
        final EnemyType type;
        float health;

        Enemy(float x, float y, EnemyType type) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.health = type.maxHealth;
        }
    }

    private static final class MagicBolt {
        float x;
        float y;
        final float dx;
        final float dy;
        final float damage;
        final float radius = 7f;
        float life = 1.4f;

        MagicBolt(float x, float y, float dx, float dy, float damage) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.damage = damage;
        }
    }

    private static final class Item {
        final String name;
        final String slot;
        final int attack;
        final int armor;
        final int magic;
        final boolean consumable;
        int quantity = 1;

        Item(String name, String slot, int attack, int armor, int magic, boolean consumable) {
            this.name = name;
            this.slot = slot;
            this.attack = attack;
            this.armor = armor;
            this.magic = magic;
            this.consumable = consumable;
        }

        Item copy() {
            Item copy = new Item(name, slot, attack, armor, magic, consumable);
            copy.quantity = quantity;
            return copy;
        }
    }

    private static final class ItemPickup {
        final float x;
        final float y;
        final Item item;

        ItemPickup(float x, float y, Item item) {
            this.x = x;
            this.y = y;
            this.item = item;
        }
    }

    private static final class Npc {
        final String name;
        final String role;
        final float x;
        final float y;
        final int color;

        Npc(String name, String role, float x, float y, int color) {
            this.name = name;
            this.role = role;
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }
}
