package com.traduvertgames.entities;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

import com.traduvertgames.main.Game;
import com.traduvertgames.world.Camera;
import com.traduvertgames.world.World;

public class Player extends Entity {

        public boolean right, up, left, down;
        public int right_dir = 0, left_dir = 1, up_dir = 2, down_dir = 3;
        public int dir = right_dir;
        public double speed = 1.5;
        public static double life = 100, maxLife = 100;
        public static double mana = 0, maxMana = 500;
        public static double shield = 0, maxShield = 150;
        public static double weapon = 0, maxWeapon = 250;
        public boolean damage = false;
        private int damageFrames = 0;
        int manaFrames = 0;
        boolean manaContinue = false;
        int manaSeconds = 0;

        public boolean shoot = false, mouseShoot = false;
        public int mx, my;

        public boolean jump = false;
        public boolean isJumping = false;

        public static int z = 0;

        public int jumpFrames = 50, jumpCur = 0;

        public boolean jumpUp = false, jumpDown = false;
        public int jumpSpd = 2;

        private int frames = 0, maxFrames = 7, index = 0, maxIndex = 3;
        private boolean moved = false;
        private BufferedImage[] rightPlayer;
        private BufferedImage[] leftPlayer;
        private BufferedImage[] upPlayer;
        private BufferedImage[] downPlayer;

        private BufferedImage playerDamage;
        private BufferedImage gunRight;
        private BufferedImage gunLeft;

        private EnumSet<WeaponType> unlockedWeapons;
        private EnumMap<WeaponType, Double> weaponEnergy;
        private WeaponType currentWeapon;
        private int fireCooldown = 0;

        private static double weaponCapacityMultiplier = 1.0;
        private static boolean persistentInitialized = false;
        private static EnumSet<WeaponType> persistentUnlockedWeapons = EnumSet.noneOf(WeaponType.class);
        private static EnumMap<WeaponType, Double> persistentWeaponEnergy = new EnumMap<WeaponType, Double>(
                        WeaponType.class);
        private static WeaponType persistentCurrentWeapon = WeaponType.BLASTER;

        static {
                ensurePersistentDefaults();
        }

        public Player(int x, int y, int width, int height, BufferedImage sprite) {
                super(x, y, width, height, sprite);

                rightPlayer = new BufferedImage[4];
                leftPlayer = new BufferedImage[4];
                upPlayer = new BufferedImage[4];
                downPlayer = new BufferedImage[4];
                playerDamage = Game.spritesheet.getSprite(0, 16, 16, 16);
                gunRight = Game.spritesheet.getSprite(16, 16, 16, 16);
                gunLeft = Game.spritesheet.getSprite(0, 32, 16, 16);

                for (int i = 0; i < 4; i++) {
                        rightPlayer[i] = Game.spritesheet.getSprite(32 + (i * 16), 0, 16, 16);
                }
                for (int i = 0; i < 4; i++) {
                        leftPlayer[i] = Game.spritesheet.getSprite(32 + (i * 16), 16, 16, 16);
                }
                upPlayer[0] = Game.spritesheet.getSprite(32, 32, 16, 16);
                upPlayer[1] = Game.spritesheet.getSprite(32 + 16, 32, 16, 16);
                upPlayer[2] = Game.spritesheet.getSprite(32, 32, 16, 16);
                upPlayer[3] = Game.spritesheet.getSprite(32 + 16, 32, 16, 16);

                downPlayer[0] = Game.spritesheet.getSprite(64, 32, 16, 16);
                downPlayer[1] = Game.spritesheet.getSprite(64 + 16, 32, 16, 16);
                downPlayer[2] = Game.spritesheet.getSprite(64, 32, 16, 16);
                downPlayer[3] = Game.spritesheet.getSprite(64 + 16, 32, 16, 16);

                initializeArsenalState();
        }

        public void update() {
                handleJump();
                moved = false;
                if (right && World.isFree((int) (x + speed), this.getY(), z)) {
                        moved = true;
                        dir = right_dir;
                        x += speed;
                } else if (left && World.isFree((int) (x - speed), this.getY(), z)) {
                        moved = true;
                        dir = left_dir;
                        x -= speed;
                }
                if (up && World.isFree(this.getX(), (int) (y - speed), z)) {
                        moved = true;
                        dir = up_dir;
                        y -= speed;
                } else if (down && World.isFree(this.getX(), (int) (y + speed), z)) {

                        moved = true;
                        dir = down_dir;
                        y += speed;
                }
                if (moved) {
                        frames++;
                        if (frames == maxFrames) {
                                frames = 0;
                                index++;
                                if (index > maxIndex) {
                                        index = 0;
                                }
                        }

                        if (damage) {
                                this.damageFrames++;
                                if (this.damageFrames == 8) {
                                        this.damageFrames = 0;
                                        damage = false;
                                }
                        }
                }
                this.checkCollisionLifePack();

                this.checkCollisionAmmo();

                this.checkCollisionGun();

                this.checkCollisionShieldOrb();

                this.checkCollisionEnergyCell();

                if (manaContinue) {
                        this.manaFrames++;

                        if (this.manaFrames == 80 && Player.mana < maxMana) {
                                mana += 8;
                                manaSeconds++;
                                this.manaFrames = 0;
                        }
                        if (manaSeconds == 5) {
                                manaContinue = false;
                                manaSeconds = 0;
                        }

                }

                if (life <= 0) {
                        if (shield > 0) {
                                life = Math.max(life, 1);
                        } else {
                                life = 0;
                                weapon = 0;
                                updateWeaponEnergyEntry(currentWeapon, weapon);
                                Game.gameState = "GAMEOVER";
                        }
                }

                if (fireCooldown > 0) {
                        fireCooldown--;
                }

                if (shoot) {
                        attemptDirectionalShot();
                }

                if (mouseShoot) {
                        attemptMouseShot();
                }

                updateCamera();
        }

        private void handleJump() {
                if (jump) {
                        if (!isJumping) {
                                jump = false;
                                isJumping = true;
                                jumpUp = true;
                        }
                }

                if (isJumping) {

                        if (jumpUp) {
                                jumpCur += jumpSpd;
                        } else if (jumpDown) {
                                jumpCur -= jumpSpd;
                                if (jumpCur <= 0) {
                                        isJumping = false;
                                        jumpDown = false;
                                        jumpUp = false;
                                }
                        }
                        z = jumpCur;
                        if (jumpCur >= jumpFrames) {
                                jumpUp = false;
                                jumpDown = true;

                        }
                }
        }

        private void attemptDirectionalShot() {
                shoot = false;
                if (!hasWeaponReady() || fireCooldown > 0) {
                        return;
                }
                double angle = 0;
                if (dir == right_dir) {
                        angle = 0;
                } else if (dir == left_dir) {
                        angle = Math.PI;
                } else if (dir == up_dir) {
                        angle = -Math.PI / 2.0;
                } else if (dir == down_dir) {
                        angle = Math.PI / 2.0;
                }
                fireWeapon(angle);
        }

        private void attemptMouseShot() {
                mouseShoot = false;
                if (!hasWeaponReady() || fireCooldown > 0) {
                        return;
                }

                double originX = this.getX() + 8;
                double originY = this.getY() + 8 - z;
                double angle = Math.atan2(my - (originY - Camera.y), mx - (originX - Camera.x));
                fireWeapon(angle);
        }

        private boolean hasWeaponReady() {
                if (currentWeapon == null) {
                        return false;
                }
                if (!unlockedWeapons.contains(currentWeapon)) {
                        return false;
                }
                if (weapon <= 0) {
                        return false;
                }
                return mana >= currentWeapon.getManaCost();
        }

        private void fireWeapon(double angle) {
                if (currentWeapon == null) {
                        return;
                }
                if (!consumeResourcesForShot(currentWeapon)) {
                        return;
                }

                int projectiles = Math.max(1, currentWeapon.getProjectilesPerShot());
                double spreadRadians = Math.toRadians(currentWeapon.getSpreadDegrees());
                double originX = this.getX() + 8;
                double originY = this.getY() + 8 - z;
                for (int i = 0; i < projectiles; i++) {
                        double offset = 0;
                        if (projectiles > 1) {
                                double spacing = projectiles > 1 ? spreadRadians / (projectiles - 1) : 0;
                                offset = -spreadRadians / 2.0 + spacing * i;
                        } else if (spreadRadians > 0) {
                                offset = (Game.rand.nextDouble() - 0.5) * spreadRadians;
                        }
                        double finalAngle = angle + offset;
                        double dx = Math.cos(finalAngle);
                        double dy = Math.sin(finalAngle);
                        int size = currentWeapon.getProjectileSize();
                        BulletShoot bullet = new BulletShoot((int) originX, (int) originY, size, size, null, dx, dy,
                                        currentWeapon.getProjectileSpeed(), currentWeapon.getDamage(), false);
                        bullet.setMask(0, 0, size, size);
                        Game.bullets.add(bullet);
                }
                fireCooldown = Math.max(0, currentWeapon.getFireDelayFrames());
        }

        private boolean consumeResourcesForShot(WeaponType weaponType) {
                if (weaponType == null) {
                        return false;
                }
                if (mana < weaponType.getManaCost()) {
                        return false;
                }
                if (weapon < weaponType.getDurabilityCost()) {
                        return false;
                }
                mana -= weaponType.getManaCost();
                weapon -= weaponType.getDurabilityCost();
                if (weapon < 0) {
                        weapon = 0;
                }
                updateWeaponEnergyEntry(weaponType, weapon);
                return true;
        }

        private void updateWeaponEnergyEntry(WeaponType type, double value) {
                ensureWeaponEntry(type);
                double maxForType = getMaxDurabilityFor(type);
                double clamped = Math.max(0, Math.min(value, maxForType));
                weaponEnergy.put(type, clamped);
                savePersistentArsenal();
        }

        public void updateCamera() {
                Camera.x = Camera.clamp(this.getX() - (Game.WIDTH / 2), 0, World.WIDTH * 16 - Game.WIDTH);
                Camera.y = Camera.clamp(this.getY() - (Game.HEIGHT / 2), 0, World.HEIGHT * 16 - Game.HEIGHT);
        }

        public boolean isColiddingEnemys(int xnext, int ynext) {
                Rectangle player = new Rectangle(xnext + this.maskx + 2, ynext + this.masky + 2, this.mwidth - 4,
                                this.mheight - 4);
                for (int i = 0; i < Game.enemies.size(); i++) {
                        Enemy e = Game.enemies.get(i);
                        Rectangle enemyCurrent = new Rectangle(e.getX() + e.maskx, e.getY() + e.masky, e.mwidth, e.mheight);
                        if (enemyCurrent.intersects(player)) {
                                return true;
                        }
                }
                return false;
        }

        public double applyDamage(double amount) {
                if (amount <= 0) {
                        return 0;
                }

                double remaining = amount;
                if (shield > 0) {
                        double absorbed = Math.min(shield, remaining);
                        shield -= absorbed;
                        remaining -= absorbed;
                }

                if (remaining > 0) {
                        life -= remaining;
                        if (life < 0) {
                                life = 0;
                        }
                }

                return remaining;
        }

        public void checkCollisionGun() {
                for (int i = 0; i < Game.entities.size(); i++) {
                        Entity atual = Game.entities.get(i);
                        if (atual instanceof Weapon) {
                                Weapon weaponEntity = (Weapon) atual;
                                if (Entity.isColliding(this, atual)) {
                                        collectWeapon(weaponEntity);
                                        Game.entities.remove(i);
                                        i--;
                                }
                        }
                }
        }

        private void collectWeapon(Weapon weaponEntity) {
                WeaponType pickupType = weaponEntity != null ? weaponEntity.getType() : WeaponType.BLASTER;
                ensureWeaponEntry(pickupType);
                unlockedWeapons.add(pickupType);
                double stored = weaponEnergy.get(pickupType);
                stored += pickupType.getPickupRecharge();
                double maxForType = getMaxDurabilityFor(pickupType);
                if (stored > maxForType) {
                        stored = maxForType;
                }
                weaponEnergy.put(pickupType, stored);
                if (currentWeapon == null || weapon <= 0 || currentWeapon == pickupType) {
                        setCurrentWeapon(pickupType);
                        weapon = Math.min(stored, maxWeapon);
                } else if (!hasWeaponUnlocked(currentWeapon)) {
                        setCurrentWeapon(pickupType);
                        weapon = Math.min(stored, maxWeapon);
                }
                savePersistentArsenal();
        }

        public void checkCollisionLifePack() {
                for (int i = 0; i < Game.entities.size(); i++) {
                        Entity atual = Game.entities.get(i);
                        if (atual instanceof LifePack) {
                                if (Entity.isColliding(this, atual)) {
                                        heal(40);
                                        Game.entities.remove(i);
                                        i--;
                                }
                        }
                }
        }

        public void checkCollisionAmmo() {
                for (int i = 0; i < Game.entities.size(); i++) {
                        Entity atual = Game.entities.get(i);
                        if (atual instanceof Bullet) {
                                if (Entity.isColliding(this, atual)) {
                                        manaContinue = true;
                                        addMana(50);
                                        Game.entities.remove(i);
                                        i--;
                                }
                        }
                }
        }

        public void checkCollisionShieldOrb() {
                for (int i = 0; i < Game.entities.size(); i++) {
                        Entity current = Game.entities.get(i);
                        if (current instanceof ShieldOrb) {
                                ShieldOrb orb = (ShieldOrb) current;
                                if (Entity.isColliding(this, orb)) {
                                        addShield(orb.getShieldValue());
                                        Game.entities.remove(i);
                                        i--;
                                }
                        }
                }
        }

        public void checkCollisionEnergyCell() {
                for (int i = 0; i < Game.entities.size(); i++) {
                        Entity current = Game.entities.get(i);
                        if (current instanceof EnergyCell) {
                                EnergyCell cell = (EnergyCell) current;
                                if (Entity.isColliding(this, cell)) {
                                        manaContinue = true;
                                        addMana(cell.getManaRestore());
                                        addWeaponEnergy(cell.getWeaponRestore());
                                        Game.entities.remove(i);
                                        i--;
                                }
                        }
                }
        }

        public void heal(double amount) {
                if (amount <= 0) {
                        return;
                }
                life += amount;
                if (life > maxLife) {
                        life = maxLife;
                }
        }

        public void addMana(double amount) {
                if (amount <= 0) {
                        return;
                }
                mana += amount;
                if (mana > maxMana) {
                        mana = maxMana;
                }
        }

        public void addShield(double amount) {
                if (amount <= 0) {
                        return;
                }
                shield += amount;
                if (shield > maxShield) {
                        shield = maxShield;
                }
        }

        public void addWeaponEnergy(double amount) {
                if (amount <= 0) {
                        return;
                }
                if (currentWeapon == null) {
                        currentWeapon = WeaponType.BLASTER;
                }
                ensureWeaponEntry(currentWeapon);
                double stored = weaponEnergy.get(currentWeapon);
                stored += amount;
                double maxForType = getMaxDurabilityFor(currentWeapon);
                if (stored > maxForType) {
                        stored = maxForType;
                }
                weaponEnergy.put(currentWeapon, stored);
                weapon += amount;
                if (weapon > stored) {
                        weapon = stored;
                }
                if (weapon > maxWeapon) {
                        weapon = maxWeapon;
                }
                savePersistentArsenal();
        }

        public double getShield() {
                return shield;
        }

        public double getShieldPercentage() {
                if (maxShield <= 0) {
                        return 0;
                }
                return Math.max(0, Math.min(1, shield / maxShield));
        }

        public void render(Graphics g) {
                if (!damage) {
                        if (dir == right_dir) {
                                g.drawImage(rightPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y - z, null);
                                if (weapon >= 1) {
                                        g.drawImage(Entity.GUN_RIGHT, this.getX() + 6 - Camera.x, this.getY() - Camera.y - z,
                                                        null);
                                }
                        } else if (dir == left_dir) {
                                g.drawImage(leftPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y - z, null);
                                if (weapon >= 1) {
                                        g.drawImage(Entity.GUN_LEFT, this.getX() - 6 - Camera.x, this.getY() - Camera.y - z,
                                                        null);
                                }
                        }
                        if (dir == up_dir) {
                                g.drawImage(upPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y - z, null);
                        }
                        if (dir == down_dir) {
                                g.drawImage(downPlayer[index], this.getX() - Camera.x, this.getY() - Camera.y - z, null);
                        }
                } else {
                        g.drawImage(playerDamage, this.getX() - Camera.x, this.getY() - Camera.y - z, null);
                        if (weapon >= 1) {
                                if (dir == left_dir) {
                                        g.drawImage(gunRight, this.getX() - 6 - Camera.x, this.getY() - Camera.y - z, null);
                                } else {
                                        g.drawImage(gunLeft, this.getX() + 6 - Camera.x, this.getY() - Camera.y - z, null);
                                }
                        }
                }
                if (isJumping) {
                        g.setColor(Color.black);
                        g.fillOval(this.getX() - Camera.x + 8, this.getY() - Camera.y + 16, 6, 6);
                }
        }

        public WeaponType getCurrentWeaponType() {
                return currentWeapon;
        }

        public boolean hasWeaponUnlocked(WeaponType type) {
                return type != null && unlockedWeapons.contains(type);
        }

        public Set<WeaponType> getUnlockedWeapons() {
                return Collections.unmodifiableSet(unlockedWeapons);
        }

        public double getWeaponEnergyFor(WeaponType type) {
                ensureWeaponEntry(type);
                return weaponEnergy.get(type);
        }

        public double getWeaponEnergyPercentage(WeaponType type) {
                if (type == null) {
                        return 0;
                }
                double stored = getWeaponEnergyFor(type);
                double maxForType = getMaxDurabilityFor(type);
                if (maxForType <= 0) {
                        return 0;
                }
                return stored / maxForType;
        }

        public double getMaxWeaponCapacity() {
                return maxWeapon;
        }

        public void setCurrentWeaponEnergy(double amount) {
                if (currentWeapon == null) {
                        currentWeapon = WeaponType.BLASTER;
                }
                ensureWeaponEntry(currentWeapon);
                weapon = Math.max(0, Math.min(amount, maxWeapon));
                updateWeaponEnergyEntry(currentWeapon, weapon);
        }

        public void refillCurrentWeapon() {
                setCurrentWeaponEnergy(maxWeapon);
        }

        public void selectWeaponSlot(int slotIndex) {
                WeaponType[] order = WeaponType.values();
                if (slotIndex < 1 || slotIndex > order.length) {
                        return;
                }
                WeaponType selected = order[slotIndex - 1];
                if (hasWeaponUnlocked(selected)) {
                        setCurrentWeapon(selected);
                        weapon = Math.min(getWeaponEnergyFor(selected), maxWeapon);
                }
        }

        public void cycleWeapon(boolean forward) {
                WeaponType[] order = WeaponType.values();
                if (order.length == 0) {
                        return;
                }
                int currentIndex = currentWeapon != null ? currentWeapon.ordinal() : 0;
                for (int offset = 1; offset <= order.length; offset++) {
                        int indexCandidate = forward ? currentIndex + offset : currentIndex - offset;
                        indexCandidate = (indexCandidate % order.length + order.length) % order.length;
                        WeaponType candidate = order[indexCandidate];
                        if (hasWeaponUnlocked(candidate)) {
                                setCurrentWeapon(candidate);
                                weapon = Math.min(getWeaponEnergyFor(candidate), maxWeapon);
                                return;
                        }
                }
        }

        public void syncFromPersistentState() {
                ensurePersistentDefaults();
                unlockedWeapons = EnumSet.copyOf(persistentUnlockedWeapons);
                weaponEnergy = new EnumMap<WeaponType, Double>(WeaponType.class);
                for (WeaponType type : WeaponType.values()) {
                        double stored = persistentWeaponEnergy.containsKey(type) ? persistentWeaponEnergy.get(type) : 0.0;
                        weaponEnergy.put(type, stored);
                }
                currentWeapon = persistentCurrentWeapon;
                if (currentWeapon == null || !unlockedWeapons.contains(currentWeapon)) {
                        currentWeapon = WeaponType.BLASTER;
                }
                refreshWeaponCapacity();
        }

        private void initializeArsenalState() {
                syncFromPersistentState();
                savePersistentArsenal();
        }

        private void refreshWeaponCapacity() {
                if (currentWeapon == null) {
                        currentWeapon = WeaponType.BLASTER;
                }
                ensureWeaponEntry(currentWeapon);
                maxWeapon = getMaxDurabilityFor(currentWeapon);
                double stored = weaponEnergy.get(currentWeapon);
                weapon = Math.min(stored, maxWeapon);
                weaponEnergy.put(currentWeapon, weapon);
        }

        private double getMaxDurabilityFor(WeaponType type) {
                if (type == null) {
                        return WeaponType.BLASTER.getMaxDurability() * weaponCapacityMultiplier;
                }
                return type.getMaxDurability() * weaponCapacityMultiplier;
        }

        private void ensureWeaponEntry(WeaponType type) {
                if (type == null) {
                        return;
                }
                if (weaponEnergy == null) {
                        weaponEnergy = new EnumMap<WeaponType, Double>(WeaponType.class);
                }
                if (!weaponEnergy.containsKey(type)) {
                        weaponEnergy.put(type, 0.0);
                }
        }

        private void setCurrentWeapon(WeaponType type) {
                if (type == null) {
                        type = WeaponType.BLASTER;
                }
                ensureWeaponEntry(type);
                currentWeapon = type;
                refreshWeaponCapacity();
                savePersistentArsenal();
        }

        private void savePersistentArsenal() {
                ensurePersistentDefaults();
                persistentUnlockedWeapons = EnumSet.copyOf(unlockedWeapons);
                persistentWeaponEnergy = new EnumMap<WeaponType, Double>(weaponEnergy);
                persistentCurrentWeapon = currentWeapon;
        }

        private static void ensurePersistentDefaults() {
                if (persistentInitialized) {
                        return;
                }
                persistentUnlockedWeapons = EnumSet.noneOf(WeaponType.class);
                for (WeaponType type : WeaponType.values()) {
                        if (type.isUnlockedByDefault()) {
                                persistentUnlockedWeapons.add(type);
                        }
                }
                persistentWeaponEnergy = new EnumMap<WeaponType, Double>(WeaponType.class);
                for (WeaponType type : WeaponType.values()) {
                        persistentWeaponEnergy.put(type, 0.0);
                }
                persistentCurrentWeapon = WeaponType.BLASTER;
                persistentInitialized = true;
        }

        private static void syncActivePlayer() {
                if (Game.player != null) {
                        Game.player.syncFromPersistentState();
                }
        }

        public static void resetPersistentArsenal() {
                persistentInitialized = false;
                ensurePersistentDefaults();
                syncActivePlayer();
        }

        public static void resetBaseStats() {
                maxLife = 100;
                life = maxLife;
                maxMana = 500;
                mana = 0;
                maxShield = 150;
                shield = 0;
                weaponCapacityMultiplier = 1.0;
                maxWeapon = WeaponType.BLASTER.getMaxDurability() * weaponCapacityMultiplier;
                weapon = 0;
        }

        public static void setWeaponCapacityMultiplier(double multiplier) {
                if (multiplier <= 0) {
                        multiplier = 1.0;
                }
                weaponCapacityMultiplier = multiplier;
                syncActivePlayer();
        }

        public static double getWeaponCapacityMultiplier() {
                return weaponCapacityMultiplier;
        }

        public static int getCurrentWeaponOrdinal() {
                ensurePersistentDefaults();
                return persistentCurrentWeapon.ordinal();
        }

        public static int getWeaponUnlockMask() {
                ensurePersistentDefaults();
                int mask = 0;
                for (WeaponType type : persistentUnlockedWeapons) {
                        mask |= (1 << type.ordinal());
                }
                return mask;
        }

        public static double getStoredEnergyForType(WeaponType type) {
                ensurePersistentDefaults();
                if (type == null) {
                        return 0;
                }
                Double value = persistentWeaponEnergy.get(type);
                if (value == null) {
                        return 0;
                }
                return value;
        }

        public static void loadUnlockedWeaponsFromSave(int mask) {
                ensurePersistentDefaults();
                EnumSet<WeaponType> decoded = EnumSet.noneOf(WeaponType.class);
                for (WeaponType type : WeaponType.values()) {
                        if ((mask & (1 << type.ordinal())) != 0) {
                                decoded.add(type);
                        }
                }
                if (decoded.isEmpty()) {
                        for (WeaponType type : WeaponType.values()) {
                                if (type.isUnlockedByDefault()) {
                                        decoded.add(type);
                                }
                        }
                }
                persistentUnlockedWeapons = decoded;
                syncActivePlayer();
        }

        public static void loadCurrentWeaponFromSave(int ordinal) {
                ensurePersistentDefaults();
                WeaponType decoded = WeaponType.fromOrdinal(ordinal);
                if (!persistentUnlockedWeapons.contains(decoded)) {
                        persistentUnlockedWeapons.add(decoded);
                }
                persistentCurrentWeapon = decoded;
                syncActivePlayer();
        }

        public static void loadWeaponEnergyFromSave(WeaponType type, double energy) {
                ensurePersistentDefaults();
                if (type == null) {
                        return;
                }
                double max = type.getMaxDurability() * weaponCapacityMultiplier;
                persistentWeaponEnergy.put(type, Math.max(0, Math.min(energy, max)));
                syncActivePlayer();
        }
}
