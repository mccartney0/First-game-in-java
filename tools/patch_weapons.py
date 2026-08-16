import os

# 1. BulletShoot: adicionar isPersistent
p = 'src/com/traduvertgames/entities/BulletShoot.java'
s = open(p).read()
old = '''    private boolean hitWall() {'''
new = '''    /**
     * Projéteis persistentes (ex.: bumerangue, relâmpago em cadeia) não são
     * consumidos no primeiro impacto e continuam existindo para novas colisões.
     */
    public boolean isPersistent() {
        return false;
    }

    private boolean hitWall() {'''
assert old in s
s = s.replace(old, new, 1)
open(p, 'w').write(s)
print('BulletShoot patched')

# 2. Player.fireWeapon: branch das novas armas
p = 'src/com/traduvertgames/entities/Player.java'
s = open(p).read()
old = '''                int projectiles = Math.max(1, currentWeapon.getProjectilesPerShot());
                double spreadRadians = Math.toRadians(currentWeapon.getSpreadDegrees());
                double originX = this.getX() + 8;
                double originY = this.getY() + 8 - z;
                for (int i = 0; i < projectiles; i++) {'''
new = '''                if (currentWeapon == WeaponType.DRONE_SENTINEL) {
                        // O drone é uma entidade autônoma que orbita e atira sozinho.
                        Game.entities.add(new DroneSentinel((int) originX() - 5, (int) originY() - 5));
                        fireCooldown = Math.max(0, currentWeapon.getFireDelayFrames());
                        return;
                }

                int projectiles = Math.max(1, currentWeapon.getProjectilesPerShot());
                double spreadRadians = Math.toRadians(currentWeapon.getSpreadDegrees());
                double originX = originX();
                double originY = originY();
                for (int i = 0; i < projectiles; i++) {'''
assert old in s, 'old fireWeapon block not found'
s = s.replace(old, new, 1)

# fechar o loop com o branch das armas persistentes
old2 = '''                        BulletShoot bullet = new BulletShoot((int) originX, (int) originY, size, size, null, dx, dy,
                                        currentWeapon.getProjectileSpeed(), currentWeapon.getDamage(), false);
                        bullet.setMask(0, 0, size, size);
                        Game.bullets.add(bullet);
                }
                fireCooldown = Math.max(0, currentWeapon.getFireDelayFrames());
        }'''
new2 = '''                        if (currentWeapon == WeaponType.BOOMERANG_ARCANO) {
                                BoomerangProjectile boomerang = new BoomerangProjectile((int) originX, (int) originY, size, dx, dy,
                                                currentWeapon.getProjectileSpeed(), currentWeapon.getDamage());
                                boomerang.setMask(0, 0, size, size);
                                Game.bullets.add(boomerang);
                        } else if (currentWeapon == WeaponType.CHAIN_ARC) {
                                ChainArcProjectile arc = new ChainArcProjectile((int) originX, (int) originY, size, dx, dy,
                                                currentWeapon.getProjectileSpeed(), currentWeapon.getDamage());
                                arc.setMask(0, 0, size, size);
                                Game.bullets.add(arc);
                        } else {
                                BulletShoot bullet = new BulletShoot((int) originX, (int) originY, size, size, null, dx, dy,
                                                currentWeapon.getProjectileSpeed(), currentWeapon.getDamage(), false);
                                bullet.setMask(0, 0, size, size);
                                Game.bullets.add(bullet);
                        }
                }
                fireCooldown = Math.max(0, currentWeapon.getFireDelayFrames());
        }

        private double originX() {
                return this.getX() + 8;
        }

        private double originY() {
                return this.getY() + 8 - z;
        }'''
assert old2 in s, 'bullet spawn block not found'
s = s.replace(old2, new2, 1)
open(p, 'w').write(s)
print('Player.java patched')
