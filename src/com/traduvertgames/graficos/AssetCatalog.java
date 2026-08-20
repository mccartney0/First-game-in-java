package com.traduvertgames.graficos;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.traduvertgames.entities.Companion.CompanionType;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.WeaponType;

/** Catálogo centralizado dos assets visuais gerados para a nova direção de arte. */
public final class AssetCatalog {

    private static final Map<WeaponType, String> WEAPON_PATHS = new EnumMap<WeaponType, String>(WeaponType.class);
    private static final Map<WeaponType, BufferedImage> WEAPON_ICONS = new EnumMap<WeaponType, BufferedImage>(WeaponType.class);
    private static final Map<CompanionType, BufferedImage> COMPANION_SPRITES = new EnumMap<CompanionType, BufferedImage>(CompanionType.class);
    private static final Map<Enemy.Variant, BufferedImage> ENEMY_SPRITES = new EnumMap<Enemy.Variant, BufferedImage>(Enemy.Variant.class);
    private static final Map<Enemy.Variant, String> ENEMY_PATHS = new EnumMap<Enemy.Variant, String>(Enemy.Variant.class);
    private static final Map<String, BufferedImage> CONTENT_TILES = new HashMap<String, BufferedImage>();
    private static BufferedImage companionAtlas;
    private static BufferedImage enemyAtlas;
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
        companionAtlas = load("/assets/generated/companions/companion_set_clean.png");
        enemyAtlas = load("/assets/generated/enemies/enemy_set_clean.png");
        configureEnemyPaths();
        initialized = true;
    }

    public static BufferedImage weaponIcon(WeaponType type) {
        initialize();
        return WEAPON_ICONS.get(type);
    }

    public static BufferedImage companionAtlas() {
        initialize();
        return companionAtlas;
    }

    public static BufferedImage companionSprite(CompanionType type) {
        initialize();
        if (type == null || companionAtlas == null) {
            return null;
        }
        BufferedImage cached = COMPANION_SPRITES.get(type);
        if (cached != null) {
            return cached;
        }
        int cell = Math.max(1, companionAtlas.getWidth() / 3);
        int index = type == CompanionType.SCOUT ? 0 : type == CompanionType.SHIELD_BOT ? 1 : 2;
        cached = crop(companionAtlas, index * cell, 0, cell, companionAtlas.getHeight());
        COMPANION_SPRITES.put(type, cached);
        return cached;
    }

    public static BufferedImage enemyAtlas() {
        initialize();
        return enemyAtlas;
    }

    public static BufferedImage enemySprite(Enemy.Variant variant) {
        initialize();
        if (variant == null) {
            return null;
        }
        BufferedImage cached = ENEMY_SPRITES.get(variant);
        if (cached != null) {
            return cached;
        }
        cached = loadNormalizedEnemySprite(ENEMY_PATHS.get(variant));
        if (cached != null) {
            ENEMY_SPRITES.put(variant, cached);
            return cached;
        }
        if (enemyAtlas == null) {
            return null;
        }
        int cellWidth = enemyAtlas.getWidth() / 3;
        int cellHeight = enemyAtlas.getHeight() / 2;
        int index = variant == Enemy.Variant.BOMBER ? 1
                : variant == Enemy.Variant.ARTILLERY || variant == Enemy.Variant.SNIPER ? 3
                : variant == Enemy.Variant.SWARM ? 4
                : variant == Enemy.Variant.SHIELDER ? 2
                : variant == Enemy.Variant.GUARDIAN || variant == Enemy.Variant.WARBRINGER
                        || variant == Enemy.Variant.OVERSEER || variant == Enemy.Variant.OVERSEER_PRIME ? 5 : 0;
        int column = index % 3;
        int row = index / 3;
        cached = normalizeEnemySprite(crop(enemyAtlas, column * cellWidth, row * cellHeight, cellWidth, cellHeight));
        ENEMY_SPRITES.put(variant, cached);
        return cached;
    }

    /**
     * Cada função de combate possui um arquivo de sprite próprio. A separação evita
     * recortes de uma arte promocional grande que, reduzida para 16 pixels, podia
     * virar um quadrado sem silhueta reconhecível.
     */
    private static void configureEnemyPaths() {
        ENEMY_PATHS.put(Enemy.Variant.SCOUT, "/assets/generated/enemies/scout_ref.png");
        ENEMY_PATHS.put(Enemy.Variant.TELEPORTER, "/assets/generated/enemies/enemy_artillery.png");
        ENEMY_PATHS.put(Enemy.Variant.ARTILLERY, "/assets/generated/enemies/enemy_artillery.png");
        ENEMY_PATHS.put(Enemy.Variant.WARDEN, "/assets/generated/enemies/enemy_shielder.png");
        ENEMY_PATHS.put(Enemy.Variant.SENTINEL, "/assets/generated/enemies/enemy_shielder.png");
        ENEMY_PATHS.put(Enemy.Variant.RAVAGER, "/assets/generated/enemies/scout_ref.png");
        ENEMY_PATHS.put(Enemy.Variant.WARBRINGER, "/assets/generated/enemies/enemy_guardian.png");
        ENEMY_PATHS.put(Enemy.Variant.OVERSEER, "/assets/generated/enemies/enemy_guardian.png");
        ENEMY_PATHS.put(Enemy.Variant.OVERSEER_PRIME, "/assets/generated/enemies/enemy_guardian.png");
        ENEMY_PATHS.put(Enemy.Variant.SAPPER, "/assets/generated/enemies/enemy_sapper.png");
        ENEMY_PATHS.put(Enemy.Variant.PHANTOM, "/assets/generated/enemies/enemy_stalker.png");
        ENEMY_PATHS.put(Enemy.Variant.GUARDIAN, "/assets/generated/enemies/enemy_guardian.png");
        ENEMY_PATHS.put(Enemy.Variant.BOMBER, "/assets/generated/enemies/enemy_bomber.png");
        ENEMY_PATHS.put(Enemy.Variant.SHIELDER, "/assets/generated/enemies/enemy_shielder.png");
        ENEMY_PATHS.put(Enemy.Variant.SNIPER, "/assets/generated/enemies/enemy_sniper.png");
        ENEMY_PATHS.put(Enemy.Variant.SWARM, "/assets/generated/enemies/enemy_swarm.png");
    }

    private static BufferedImage loadNormalizedEnemySprite(String path) {
        BufferedImage source = path == null ? null : load(path);
        // Enquanto um pacote específico é substituído, a silhueta base preserva
        // leitura no jogo. Nunca se volta ao recorte grande e pouco legível só
        // porque um único arquivo ainda não foi exportado.
        if (source == null && !"/assets/generated/enemies/scout_ref.png".equals(path)) {
            source = load("/assets/generated/enemies/scout_ref.png");
        }
        return source == null ? null : normalizeEnemySprite(source);
    }

    /**
     * Garante que PNGs importados tenham transparência real e um envelope pequeno
     * de pixels. O chroma verde é aceito apenas como proteção para editores que não
     * preservem alfa; assets com alfa continuam inalterados.
     */
    private static BufferedImage normalizeEnemySprite(BufferedImage source) {
        BufferedImage alphaSafe = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;
                boolean chromaGreen = green > 220 && red < 45 && blue < 45;
                alphaSafe.setRGB(x, y, chromaGreen ? 0x00000000 : argb);
            }
        }
        BufferedImage trimmed = trimTransparent(alphaSafe);
        BufferedImage normalized = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        int available = 28;
        double scale = Math.min((double) available / Math.max(1, trimmed.getWidth()),
                (double) available / Math.max(1, trimmed.getHeight()));
        int width = Math.max(1, (int) Math.round(trimmed.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(trimmed.getHeight() * scale));
        Graphics2D graphics = normalized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.drawImage(trimmed, (32 - width) / 2, (32 - height) / 2, width, height, null);
        graphics.dispose();
        return normalized;
    }

    public static BufferedImage dungeonPortal() {
        return load("/assets/generated/world/dungeon_portal.png");
    }

    public static BufferedImage regionalTileAtlas() {
        return load("/assets/generated/world/regional_tile_texture.png");
    }

    /**
     * Retorna um tile exportado pelo Content Studio e o mantém em cache. O jogo
     * continua operando com o preenchimento de cor caso o asset ainda não exista.
     */
    public static synchronized BufferedImage contentTile(String assetName) {
        if (assetName == null || assetName.trim().isEmpty()) {
            return null;
        }
        String normalized = assetName.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "_");
        if (CONTENT_TILES.containsKey(normalized)) {
            return CONTENT_TILES.get(normalized);
        }
        BufferedImage tile = load("/assets/generated/tiles/" + normalized + ".png");
        CONTENT_TILES.put(normalized, tile);
        return tile;
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

    private static BufferedImage crop(BufferedImage source, int x, int y, int width, int height) {
        int safeWidth = Math.min(width, source.getWidth() - x);
        int safeHeight = Math.min(height, source.getHeight() - y);
        if (safeWidth <= 0 || safeHeight <= 0) {
            return null;
        }
        return trimTransparent(source.getSubimage(x, y, safeWidth, safeHeight));
    }

    private static BufferedImage trimTransparent(BufferedImage source) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if ((source.getRGB(x, y) >>> 24) > 8) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return source;
        }
        BufferedImage trimmed = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = trimmed.createGraphics();
        graphics.drawImage(source, -minX, -minY, null);
        graphics.dispose();
        return trimmed;
    }

    private static BufferedImage load(String path) {
        try (InputStream stream = AssetCatalog.class.getResourceAsStream(path)) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException ex) {
            return null;
        }
    }
}
