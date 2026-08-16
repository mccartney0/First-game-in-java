package com.traduvertgames;

import com.traduvertgames.entities.BoomerangProjectile;
import com.traduvertgames.entities.BulletShoot;
import com.traduvertgames.entities.ChainArcProjectile;
import com.traduvertgames.entities.DroneSentinel;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.graficos.Spritesheet;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;

/**
 * Valida a lógica das três novas armas sem UI: enum completo,
 * projéteis persistentes e propriedades do drone.
 */
public class WeaponsLogicTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + name);
        } else {
            failed++;
            System.out.println("[FAIL] " + name);
        }
    }

    public static void main(String[] args) {
        try {
            Game.spritesheet = new Spritesheet("/spritesheet.png");
        } catch (Exception fallback) {
            try {
                Spritesheet dummy = new Spritesheet("/spritesheet.png");
                java.lang.reflect.Field f = Spritesheet.class.getDeclaredField("spritesheet");
                f.setAccessible(true);
                f.set(dummy, new java.awt.image.BufferedImage(256, 256, java.awt.image.BufferedImage.TYPE_INT_ARGB));
                Game.spritesheet = dummy;
            } catch (Exception ignored) {
            }
        }
        // Enum: novas armas registradas com parâmetros válidos
        check("BOOMERANG_ARCANO registrado", WeaponType.valueOf("BOOMERANG_ARCANO") != null);
        check("CHAIN_ARC registrado", WeaponType.valueOf("CHAIN_ARC") != null);
        check("DRONE_SENTINEL registrado", WeaponType.valueOf("DRONE_SENTINEL") != null);
        check("total de 11 armas", WeaponType.values().length == 11);

        WeaponType boomerang = WeaponType.BOOMERANG_ARCANO;
        check("bumerangue: dano alto (>= 6)", boomerang.getDamage() >= 6.0);
        check("bumerangue: mana razoável", boomerang.getManaCost() > 0 && boomerang.getManaCost() < 5);
        check("bumerangue: shortName BUMERANGUE", "BUMERANGUE".equals(boomerang.getShortName()));

        WeaponType chain = WeaponType.CHAIN_ARC;
        check("cadeia: dano base >= 6", chain.getDamage() >= 6.0);
        check("cadeia: shortName CADEIA", "CADEIA".equals(chain.getShortName()));

        WeaponType drone = WeaponType.DRONE_SENTINEL;
        check("drone: descricao presente", drone.getDescription().length() > 10);
        check("drone: shortName DRONE", "DRONE".equals(drone.getShortName()));

        // Persistência: projéteis normais consomem-se no primeiro hit
        check("bullet comum não é persistente", !new BulletShoot(0, 0, 6, 6, null, 1, 0, 5, 1, false).isPersistent());
        check("bumerangue é persistente", new BoomerangProjectile(0, 0, 8, 1, 0, 6, 8).isPersistent());
        check("cadeia é persistente", new ChainArcProjectile(0, 0, 4, 1, 0, 5, 7).isPersistent());

        // Save: fromSaveKey reconhece as novas armas
        check("fromSaveKey BOOMERANG_ARCANO", WeaponType.fromSaveKey("BOOMERANG_ARCANO") == WeaponType.BOOMERANG_ARCANO);
        check("fromSaveKey DRONE_SENTINEL", WeaponType.fromSaveKey("DRONE_SENTINEL") == WeaponType.DRONE_SENTINEL);
        check("fromOrdinal dentro do range", WeaponType.fromOrdinal(10) == WeaponType.values()[10]);

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
