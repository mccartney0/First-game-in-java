package com.traduvertgames.graficos;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.traduvertgames.entities.WeaponType;

/** Catálogo centralizado dos assets visuais gerados para a nova direção de arte. */
public final class AssetCatalog {

    private static final Map<WeaponType, String> WEAPON_PATHS = new EnumMap<WeaponType, String>(WeaponType.class);
    private static final Map<WeaponType, BufferedImage> WEAPON_ICONS = new EnumMap<WeaponType, BufferedImage>(WeaponType.class);
    private static boolean initialized;

    private AssetCatalog() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        WEAPON_PATHS.put(WeaponType.BLASTER, "/assets/generated/weapons/blaster_clean.png");
        WEAPON_PATHS.put(WeaponType.ION_RIFLE, "/assets/generated/weapons/ion_rifle_clean.png");
        WEAPON_PATHS.put(WeaponType.SCATTER_CANNON, "/assets/generated/weapons/scatter_cannon_clean.png");
        WEAPON_PATHS.put(WeaponType.FUSION_LANCE, "/assets/generated/weapons/fusion_lance_clean.png");
        WEAPON_PATHS.put(WeaponType.VOID_MORTAR, "/assets/generated/weapons/void_mortar_clean.png");
        for (Map.Entry<WeaponType, String> entry : WEAPON_PATHS.entrySet()) {
            BufferedImage icon = load(entry.getValue());
            if (icon != null) {
                WEAPON_ICONS.put(entry.getKey(), icon);
            }
        }
        initialized = true;
    }

    public static BufferedImage weaponIcon(WeaponType type) {
        initialize();
        return WEAPON_ICONS.get(type);
    }

    public static BufferedImage companionAtlas() {
        return load("/assets/generated/companions/companion_set.png");
    }

    public static BufferedImage enemyAtlas() {
        return load("/assets/generated/enemies/enemy_set.png");
    }

    public static BufferedImage dungeonPortal() {
        return load("/assets/generated/world/dungeon_portal.png");
    }

    public static BufferedImage regionalTileAtlas() {
        return load("/assets/generated/world/regional_tile_texture.png");
    }

    public static void drawWeaponIcon(Graphics2D graphics, WeaponType type, int x, int y, int width, int height) {
        BufferedImage icon = weaponIcon(type);
        if (icon == null) {
            return;
        }
        Object oldInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Object oldRendering = graphics.getRenderingHint(RenderingHints.KEY_RENDERING);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.drawImage(icon, x, y, width, height, null);
        if (oldInterpolation != null) {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        }
        if (oldRendering != null) {
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, oldRendering);
        }
    }

    private static BufferedImage load(String path) {
        try (InputStream stream = AssetCatalog.class.getResourceAsStream(path)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException ex) {
            return null;
        }
    }
}
