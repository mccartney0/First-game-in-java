package com.traduvertgames.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import com.traduvertgames.world.LargeRpgMapGenerator;

/**
 * Operações determinísticas de exportação do Content Studio.
 *
 * A UI Swing delega a esta classe para que o mesmo contrato possa ser testado em
 * headless e futuramente reutilizado por automações de conteúdo.
 */
public final class ContentStudioProject {

    public enum MapKind { REGIONAL, OPEN_WORLD }
    public enum TileStyle { GRAMA, ESTRADA, RUINAS, PEDRA, AREIA, TECNOLOGIA }
    public enum EnemyRole {
        SCOUT, BOMBER, SHIELDER, ARTILLERY, SNIPER, SWARM, SAPPER, STALKER, GUARDIAN,
        MIRE_HOUND, BOG_ORACLE, MIRE_BRUTE, MIST_SOVEREIGN
    }
    public enum ConsumableEffect { CURA, MANA, FÔLEGO, TRIAGEM }
    public enum RpgWeaponStyle { ESPADA, MACHADO, CAJADO, ADAGA }
    public enum RpgSpriteKind {
        HERO("hero", "Protagonista"),
        NPC_COMMANDANT("npc", "Ava, Comandante"),
        NPC_HEALER("npc", "Orin, Curador"),
        NPC_CARTOGRAPHER("npc", "Neris, Cartógrafa"),
        WEAPON_STAFF("weapon", "Cajado de Bruma"),
        WEAPON_RUNE_SWORD("weapon", "Espada Rúnica"),
        WEAPON_ARC_RIFLE("weapon", "Arc Rifle de Latão"),
        PROJECTILE_FIREBOLT("projectile", "Dardo de Brasa"),
        PROJECTILE_ARCANE("projectile", "Dardo Arcano"),
        PROJECTILE_MIST("projectile", "Orbe de Bruma");

        private final String category;
        private final String displayName;

        RpgSpriteKind(String category, String displayName) {
            this.category = category;
            this.displayName = displayName;
        }

        public String getCategory() { return category; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }
    }

    public enum EnemyBehavior {
        HUNT("hunt", "Caçador — persegue em alcance médio"),
        BOMBARD("bombard", "Artilheiro — mantém distância e dispara"),
        DETONATE("detonate", "Demolidor — aproxima e explode"),
        SHIELD("shield", "Protetor — fortalece aliados próximos"),
        SWARM("swarm", "Enxame — pressiona em grupo"),
        AMBUSH("ambush", "Emboscador — salta para a retaguarda"),
        DRAIN("drain", "Drenador — persegue e drena recursos"),
        REGENERATE("regenerate", "Guardião — resiste e se regenera"),
        SNIPE("snipe", "Atirador — dispara de longo alcance"),
        POUNCE("pounce", "Predador — avança em investidas curtas"),
        HEX("hex", "Oráculo — mantém distância e lança maldições"),
        FORTIFY("fortify", "Bruto — avança lento, mas aguenta o confronto");

        private final String tag;
        private final String displayName;

        EnemyBehavior(String tag, String displayName) {
            this.tag = tag;
            this.displayName = displayName;
        }

        public String getTag() { return tag; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }

        public static EnemyBehavior fromTag(String raw) {
            if (raw != null) for (EnemyBehavior behavior : values()) {
                if (behavior.tag.equalsIgnoreCase(raw.trim())) return behavior;
            }
            return HUNT;
        }
    }

    public static final class TileProperties {
        public final boolean walkable;
        public final int movementCost;
        public final String terrainTag;

        public TileProperties(boolean walkable, int movementCost, String terrainTag) {
            this.walkable = walkable;
            this.movementCost = Math.max(1, movementCost);
            this.terrainTag = safeTag(terrainTag, "ground");
        }

        public static TileProperties defaults(TileStyle style) {
            if (style == TileStyle.RUINAS || style == TileStyle.PEDRA) {
                return new TileProperties(true, 2, "ruins");
            }
            if (style == TileStyle.ESTRADA) return new TileProperties(true, 1, "road");
            return new TileProperties(true, 1, "ground");
        }
    }

    public static final class EnemyProperties {
        public final int baseLife;
        public final int baseDamage;
        public final double speed;
        public final String behaviorTag;
        public final boolean boss;

        public EnemyProperties(int baseLife, int baseDamage, double speed, String behaviorTag) {
            this(baseLife, baseDamage, speed, behaviorTag, false);
        }

        public EnemyProperties(int baseLife, int baseDamage, double speed, String behaviorTag, boolean boss) {
            this.baseLife = Math.max(1, baseLife);
            this.baseDamage = Math.max(0, baseDamage);
            this.speed = Math.max(0.1, speed);
            this.behaviorTag = safeTag(behaviorTag, "chase");
            this.boss = boss;
        }

        public static EnemyProperties defaults(EnemyRole role) {
            EnemyRole safeRole = role == null ? EnemyRole.SCOUT : role;
            EnemyBehavior behavior = behaviorForRole(safeRole);
            if (safeRole == EnemyRole.MIST_SOVEREIGN) return new EnemyProperties(48, 12, 0.55, behavior.getTag(), true);
            if (safeRole == EnemyRole.MIRE_BRUTE) return new EnemyProperties(16, 8, 0.65, behavior.getTag());
            if (safeRole == EnemyRole.BOG_ORACLE) return new EnemyProperties(8, 6, 0.7, behavior.getTag());
            if (safeRole == EnemyRole.MIRE_HOUND) return new EnemyProperties(5, 4, 1.65, behavior.getTag());
            if (safeRole == EnemyRole.GUARDIAN) return new EnemyProperties(18, 5, 0.8, behavior.getTag());
            if (safeRole == EnemyRole.SNIPER) return new EnemyProperties(7, 7, 0.55, behavior.getTag());
            if (safeRole == EnemyRole.ARTILLERY) return new EnemyProperties(7, 4, 1.1, behavior.getTag());
            if (safeRole == EnemyRole.SHIELDER) return new EnemyProperties(10, 2, 0.9, behavior.getTag());
            if (safeRole == EnemyRole.BOMBER) return new EnemyProperties(6, 6, 1.2, behavior.getTag());
            if (safeRole == EnemyRole.SWARM) return new EnemyProperties(3, 1, 1.7, behavior.getTag());
            if (safeRole == EnemyRole.SAPPER) return new EnemyProperties(4, 5, 1.5, behavior.getTag());
            if (safeRole == EnemyRole.STALKER) return new EnemyProperties(6, 3, 1.4, behavior.getTag());
            return new EnemyProperties(5, 2, 1.4, behavior.getTag());
        }
    }

    /** Contrato exportável de uma habilidade especial de chefe usada pelo runtime RPG. */
    public static final class BossAbilityProperties {
        public final String ownerRole;
        public final int damage;
        public final int cooldownTicks;
        public final int range;

        public BossAbilityProperties(String ownerRole, int damage, int cooldownTicks, int range) {
            this.ownerRole = safeTag(ownerRole, EnemyRole.MIST_SOVEREIGN.name().toLowerCase());
            this.damage = Math.max(1, damage);
            this.cooldownTicks = Math.max(1, cooldownTicks);
            this.range = Math.max(24, range);
        }

        public static BossAbilityProperties mistSovereignDefaults() {
            return new BossAbilityProperties("mist_sovereign", 14, 180, 168);
        }
    }

    /** Propriedades de um consumível usado pelo loop de inventário do RPG. */
    public static final class ConsumableProperties {
        public final String displayName;
        public final ConsumableEffect effect;
        public final int lifeRestore;
        public final int manaRestore;
        public final int staminaRestore;

        public ConsumableProperties(String displayName, ConsumableEffect effect, int lifeRestore,
                int manaRestore, int staminaRestore) {
            this.displayName = safeDisplayName(displayName, "Consumível");
            this.effect = effect == null ? ConsumableEffect.CURA : effect;
            this.lifeRestore = Math.max(0, lifeRestore);
            this.manaRestore = Math.max(0, manaRestore);
            this.staminaRestore = Math.max(0, staminaRestore);
        }

        public static ConsumableProperties defaults(ConsumableEffect effect) {
            ConsumableEffect safeEffect = effect == null ? ConsumableEffect.CURA : effect;
            if (safeEffect == ConsumableEffect.MANA) return new ConsumableProperties("Tônico de Éter", safeEffect, 0, 34, 12);
            if (safeEffect == ConsumableEffect.FÔLEGO) return new ConsumableProperties("Infusão de Fôlego", safeEffect, 8, 0, 38);
            if (safeEffect == ConsumableEffect.TRIAGEM) return new ConsumableProperties("Elixir de Bruma", safeEffect, 24, 18, 20);
            return new ConsumableProperties("Erva de Bruma", safeEffect, 30, 0, 14);
        }
    }

    /** Propriedades de uma arma equipável pelo personagem do modo RPG. */
    public static final class RpgWeaponProperties {
        public final String displayName;
        public final int damageBonus;
        public final int staminaCost;
        public final String rarity;

        public RpgWeaponProperties(String displayName, int damageBonus, int staminaCost, String rarity) {
            this.displayName = safeDisplayName(displayName, "Arma RPG");
            this.damageBonus = Math.max(0, damageBonus);
            this.staminaCost = Math.max(0, staminaCost);
            this.rarity = safeTag(rarity, "common");
        }

        public static RpgWeaponProperties defaults(RpgWeaponStyle style) {
            RpgWeaponStyle safeStyle = style == null ? RpgWeaponStyle.ESPADA : style;
            if (safeStyle == RpgWeaponStyle.MACHADO) return new RpgWeaponProperties("Machado de Raiz", 3, 12, "rare");
            if (safeStyle == RpgWeaponStyle.CAJADO) return new RpgWeaponProperties("Cajado de Bruma", 2, 8, "uncommon");
            if (safeStyle == RpgWeaponStyle.ADAGA) return new RpgWeaponProperties("Adaga do Vento", 1, 5, "common");
            return new RpgWeaponProperties("Lâmina de Bruma", 2, 9, "uncommon");
        }
    }

    /** Metadados comuns aos sprites visuais usados pelo runtime RPG Android. */
    public static final class RpgSpriteProperties {
        public final String displayName;
        public final double gameplayScale;
        public final int damage;
        public final int cooldownTicks;
        public final double shotOriginX;
        public final double shotOriginY;

        public RpgSpriteProperties(String displayName, double gameplayScale, int damage, int cooldownTicks,
                double shotOriginX, double shotOriginY) {
            this.displayName = safeDisplayName(displayName, "Sprite RPG");
            this.gameplayScale = Math.max(0.25, Math.min(3.0, gameplayScale));
            this.damage = Math.max(0, damage);
            this.cooldownTicks = Math.max(0, cooldownTicks);
            this.shotOriginX = Math.max(0.0, Math.min(1.0, shotOriginX));
            this.shotOriginY = Math.max(0.0, Math.min(1.0, shotOriginY));
        }

        public static RpgSpriteProperties defaults(RpgSpriteKind kind) {
            RpgSpriteKind safeKind = kind == null ? RpgSpriteKind.HERO : kind;
            if (safeKind.getCategory().equals("projectile")) {
                return new RpgSpriteProperties(safeKind.getDisplayName(), 0.72, 6, 16, 0.55, 0.33);
            }
            if (safeKind.getCategory().equals("weapon")) {
                return new RpgSpriteProperties(safeKind.getDisplayName(), 1.0, 3, 20, 0.72, 0.34);
            }
            return new RpgSpriteProperties(safeKind.getDisplayName(), 1.0, 0, 0, 0.5, 0.5);
        }
    }

    public static EnemyBehavior behaviorForRole(EnemyRole role) {
        if (role == EnemyRole.MIST_SOVEREIGN) return EnemyBehavior.REGENERATE;
        if (role == EnemyRole.MIRE_BRUTE) return EnemyBehavior.FORTIFY;
        if (role == EnemyRole.BOG_ORACLE) return EnemyBehavior.HEX;
        if (role == EnemyRole.MIRE_HOUND) return EnemyBehavior.POUNCE;
        if (role == EnemyRole.GUARDIAN) return EnemyBehavior.REGENERATE;
        if (role == EnemyRole.SNIPER) return EnemyBehavior.SNIPE;
        if (role == EnemyRole.ARTILLERY) return EnemyBehavior.BOMBARD;
        if (role == EnemyRole.SHIELDER) return EnemyBehavior.SHIELD;
        if (role == EnemyRole.BOMBER) return EnemyBehavior.DETONATE;
        if (role == EnemyRole.SWARM) return EnemyBehavior.SWARM;
        if (role == EnemyRole.SAPPER) return EnemyBehavior.AMBUSH;
        if (role == EnemyRole.STALKER) return EnemyBehavior.DRAIN;
        return EnemyBehavior.HUNT;
    }

    private ContentStudioProject() {
    }

    public static File generateMap(MapKind kind, int width, int height, int depth, long seed, File projectRoot)
            throws IOException {
        MapKind safeKind = kind == null ? MapKind.REGIONAL : kind;
        int requestedWidth = safeKind == MapKind.OPEN_WORLD
                ? Math.max(LargeRpgMapGenerator.OPEN_WORLD_WIDTH, width)
                : Math.max(LargeRpgMapGenerator.DEFAULT_WIDTH, width);
        int requestedHeight = safeKind == MapKind.OPEN_WORLD
                ? Math.max(LargeRpgMapGenerator.OPEN_WORLD_HEIGHT, height)
                : Math.max(LargeRpgMapGenerator.DEFAULT_HEIGHT, height);
        File output = new File(projectRoot, safeKind == MapKind.OPEN_WORLD
                ? "bin/open_world_maps" : "bin/large_rpg_maps");
        return LargeRpgMapGenerator.generate(requestedWidth, requestedHeight, Math.max(1, depth), seed, output);
    }

    public static File generateTile(TileStyle style, String name, File projectRoot) throws IOException {
        return generateTile(style, name, 0, TileProperties.defaults(style == null ? TileStyle.GRAMA : style), projectRoot);
    }

    public static File generateTile(TileStyle style, String name, int variation, TileProperties properties, File projectRoot)
            throws IOException {
        TileStyle safeStyle = style == null ? TileStyle.GRAMA : style;
        TileProperties safeProperties = properties == null ? TileProperties.defaults(safeStyle) : properties;
        File output = ensureDirectory(projectRoot, "res/assets/generated/tiles");
        File png = new File(output, safeName(name, "tile") + ".png");
        BufferedImage tile = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = tile.createGraphics();
        Color base = tileBase(safeStyle);
        Color accent = tileAccent(safeStyle);
        graphics.setColor(base);
        graphics.fillRect(0, 0, 32, 32);
        int variant = Math.floorMod(variation, 8);
        if (safeStyle == TileStyle.GRAMA) {
            int[][] tufts = {
                {4, 5, 17, 3, 24, 17, 11, 23},
                {2, 13, 12, 5, 22, 8, 27, 23},
                {7, 3, 18, 13, 4, 24, 23, 25},
                {3, 8, 13, 20, 26, 4, 24, 19}
            };
            int[] positions = tufts[variant % tufts.length];
            graphics.setColor(accent);
            for (int i = 0; i < positions.length; i += 2) {
                int x = positions[i];
                int y = positions[i + 1];
                graphics.fillRect(x, y, 2, 5);
                graphics.fillRect(x + 2, y + 2, 2, 3);
            }
            graphics.setColor(new Color(48, 94, 57));
            graphics.fillRect((variant * 9 + 5) % 26, (variant * 11 + 4) % 26, 4, 2);
            graphics.setColor(new Color(144, 166, 91));
            graphics.fillRect((variant * 7 + 11) % 28, (variant * 5 + 18) % 27, 2, 2);
            if (variant == 2) {
                graphics.setColor(new Color(205, 184, 113));
                graphics.fillRect(15, 7, 2, 2);
            }
        } else if (safeStyle == TileStyle.ESTRADA) {
            graphics.setColor(accent);
            graphics.fillRect(0, 4 + variant % 3, 32, 2);
            graphics.fillRect(0, 23 - variant % 4, 32, 2);
            graphics.setColor(new Color(109, 82, 55));
            for (int x = 3; x < 32; x += 9) graphics.fillRect(x, 12 + (x + variant) % 6, 5, 2);
        } else if (safeStyle == TileStyle.RUINAS) {
            graphics.setColor(accent);
            graphics.fillRect(2, 3, 12, 7);
            graphics.fillRect(17, 6, 13, 8);
            graphics.fillRect(5, 18, 10, 10);
            graphics.fillRect(19, 19, 9, 9);
            graphics.setColor(new Color(42, 45, 54));
            graphics.fillRect(14 + variant % 4, 0, 2, 15);
            graphics.fillRect(0, 15 + variant % 3, 13, 2);
        } else if (safeStyle == TileStyle.PEDRA) {
            graphics.setColor(accent);
            graphics.drawLine(0, 8, 31, 8);
            graphics.drawLine(0, 23, 31, 23);
            graphics.drawLine(10, 0, 7, 8);
            graphics.drawLine(22, 8, 25, 23);
            graphics.drawLine(15, 23, 12, 31);
        } else if (safeStyle == TileStyle.AREIA) {
            graphics.setColor(accent);
            for (int x = 2; x < 32; x += 7) {
                for (int y = 3; y < 32; y += 8) graphics.fillRect(x, y, 2, 2);
            }
        } else {
            graphics.setColor(accent);
            graphics.drawRect(2, 2, 27, 27);
            graphics.drawLine(0, 16, 32, 16);
            graphics.drawLine(16, 0, 16, 32);
            graphics.fillRect(14, 14, 4, 4);
        }
        graphics.dispose();
        ImageIO.write(tile, "png", png);
        writeTileManifest(png, safeStyle, variant, safeProperties);
        return png;
    }

    /**
     * Exporta as dez variantes de runtime de Brumafolha para
     * res/assets/generated/tiles. Seus nomes são consumidos diretamente pelo
     * RpgMap e, por isso, não exigem cópia manual ou mudança de código do jogo.
     */
    public static File[] generateBrumafolhaTerrainPack(File projectRoot) throws IOException {
        File[] generated = new File[10];
        int index = 0;
        for (int variant = 0; variant < 4; variant++) {
            generated[index++] = generateTile(TileStyle.GRAMA, "brumafolha_grass_" + variant,
                    variant, TileProperties.defaults(TileStyle.GRAMA), projectRoot);
        }
        for (int variant = 0; variant < 3; variant++) {
            generated[index++] = generateTile(TileStyle.ESTRADA, "brumafolha_road_" + variant,
                    variant, TileProperties.defaults(TileStyle.ESTRADA), projectRoot);
        }
        for (int variant = 0; variant < 3; variant++) {
            generated[index++] = generateTile(TileStyle.RUINAS, "brumafolha_ruins_" + variant,
                    variant, TileProperties.defaults(TileStyle.RUINAS), projectRoot);
        }
        return generated;
    }

    public static File generateEnemySprite(EnemyRole role, Color body, Color accent, File projectRoot) throws IOException {
        EnemyRole safeRole = role == null ? EnemyRole.SCOUT : role;
        return generateEnemySprite(safeRole, body, accent, EnemyProperties.defaults(safeRole), projectRoot);
    }

    public static File generateConsumable(String id, ConsumableProperties properties, File projectRoot) throws IOException {
        ConsumableProperties safeProperties = properties == null
                ? ConsumableProperties.defaults(ConsumableEffect.CURA) : properties;
        File output = ensureDirectory(projectRoot, "res/assets/generated/items");
        File png = new File(output, safeName(id, "consumable") + ".png");
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        drawConsumableIcon(graphics, safeProperties.effect);
        graphics.dispose();
        ImageIO.write(icon, "png", png);
        writeConsumableManifest(png, safeProperties);
        refreshRpgContentCatalog(projectRoot);
        return png;
    }

    public static File generateRpgWeapon(String id, RpgWeaponStyle style, RpgWeaponProperties properties,
            File projectRoot) throws IOException {
        RpgWeaponStyle safeStyle = style == null ? RpgWeaponStyle.ESPADA : style;
        RpgWeaponProperties safeProperties = properties == null ? RpgWeaponProperties.defaults(safeStyle) : properties;
        File output = ensureDirectory(projectRoot, "res/assets/generated/rpg_weapons");
        File png = new File(output, safeName(id, "rpg_weapon") + ".png");
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        drawRpgWeaponIcon(graphics, safeStyle);
        graphics.dispose();
        ImageIO.write(icon, "png", png);
        writeRpgWeaponManifest(png, safeStyle, safeProperties);
        refreshRpgContentCatalog(projectRoot);
        return png;
    }

    /** Exporta um sprite visual RPG e os metadados que o APK consome em runtime. */
    public static File generateRpgSprite(String id, RpgSpriteKind kind, RpgSpriteProperties properties,
            File projectRoot) throws IOException {
        RpgSpriteKind safeKind = kind == null ? RpgSpriteKind.HERO : kind;
        RpgSpriteProperties safeProperties = properties == null ? RpgSpriteProperties.defaults(safeKind) : properties;
        File output = ensureDirectory(projectRoot, "res/assets/generated/rpg_sprites");
        File png = new File(output, safeName(id, "rpg_" + safeKind.name().toLowerCase()) + ".png");
        BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sprite.createGraphics();
        drawRpgSprite(graphics, safeKind);
        graphics.dispose();
        ImageIO.write(sprite, "png", png);
        writeRpgSpriteManifest(png, safeKind, safeProperties);
        refreshRpgContentCatalog(projectRoot);
        return png;
    }

    /** Gera o pacote visual mínimo para protagonista, NPCs, armas e tiros. */
    public static File[] generateDefaultRpgVisualPack(File projectRoot) throws IOException {
        RpgSpriteKind[] kinds = {
                RpgSpriteKind.HERO, RpgSpriteKind.NPC_COMMANDANT, RpgSpriteKind.NPC_HEALER,
                RpgSpriteKind.NPC_CARTOGRAPHER, RpgSpriteKind.WEAPON_STAFF, RpgSpriteKind.WEAPON_RUNE_SWORD,
                RpgSpriteKind.WEAPON_ARC_RIFLE, RpgSpriteKind.PROJECTILE_FIREBOLT,
                RpgSpriteKind.PROJECTILE_ARCANE, RpgSpriteKind.PROJECTILE_MIST };
        File[] generated = new File[kinds.length];
        for (int index = 0; index < kinds.length; index++) {
            RpgSpriteKind spriteKind = kinds[index];
            generated[index] = generateRpgSprite(spriteKind.name().toLowerCase(), spriteKind,
                    RpgSpriteProperties.defaults(spriteKind), projectRoot);
        }
        return generated;
    }

    /** Exporta um conjunto inicial que o modo RPG reconhece automaticamente. */
    public static File[] generateDefaultRpgContentPack(File projectRoot) throws IOException {
        File elixir = generateConsumable("elixir_de_bruma",
                ConsumableProperties.defaults(ConsumableEffect.TRIAGEM), projectRoot);
        File blade = generateRpgWeapon("lamina_de_bruma", RpgWeaponStyle.ESPADA,
                RpgWeaponProperties.defaults(RpgWeaponStyle.ESPADA), projectRoot);
        return new File[] { elixir, blade };
    }

    public static File generateEnemySprite(EnemyRole role, Color body, Color accent, EnemyProperties properties,
            File projectRoot) throws IOException {
        EnemyRole safeRole = role == null ? EnemyRole.SCOUT : role;
        EnemyProperties safeProperties = properties == null ? EnemyProperties.defaults(safeRole) : properties;
        File output = ensureDirectory(projectRoot, "res/assets/generated/enemies");
        String fileName = safeRole == EnemyRole.SCOUT ? "scout_ref.png"
                : "enemy_" + safeRole.name().toLowerCase() + ".png";
        File png = new File(output, fileName);
        BufferedImage sprite = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sprite.createGraphics();
        Color primary = body == null ? defaultBody(safeRole) : body;
        Color secondary = accent == null ? defaultAccent(safeRole) : accent;
        graphics.setColor(new Color(10, 14, 20, 105));
        graphics.fillOval(8, 25, 16, 4);
        drawEnemySilhouette(graphics, safeRole, primary, secondary);
        graphics.dispose();
        ImageIO.write(sprite, "png", png);
        writeEnemyManifest(png, safeRole, safeProperties);
        return png;
    }

    /** Gera os três arquétipos usados pelo encontro expandido da Charneca da Bruma. */
    public static File[] generateOutlandEnemyPack(File projectRoot) throws IOException {
        EnemyRole[] roles = { EnemyRole.MIRE_HOUND, EnemyRole.BOG_ORACLE, EnemyRole.MIRE_BRUTE };
        File[] generated = new File[roles.length];
        for (int index = 0; index < roles.length; index++) {
            EnemyRole role = roles[index];
            generated[index] = generateEnemySprite(role, null, null, EnemyProperties.defaults(role), projectRoot);
        }
        return generated;
    }

    /** Demonstração exportável de um chefe configurado no canvas de inimigos. */
    public static File generateMistSovereignBoss(File projectRoot) throws IOException {
        EnemyProperties profile = EnemyProperties.defaults(EnemyRole.MIST_SOVEREIGN);
        return generateEnemySprite(EnemyRole.MIST_SOVEREIGN, null, null, profile, projectRoot);
    }

    /** Exporta o ícone e o manifesto configurável da pulsação especial do Soberano. */
    public static File generateMistSovereignAbility(File projectRoot) throws IOException {
        return generateMistSovereignAbility(BossAbilityProperties.mistSovereignDefaults(), projectRoot);
    }

    public static File generateMistSovereignAbility(BossAbilityProperties properties, File projectRoot) throws IOException {
        BossAbilityProperties safeProperties = properties == null
                ? BossAbilityProperties.mistSovereignDefaults() : properties;
        File output = ensureDirectory(projectRoot, "res/assets/generated/abilities");
        File png = new File(output, "mist_sovereign_nucleo_da_bruma.png");
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        graphics.setColor(new Color(36, 28, 57, 120));
        graphics.fillOval(2, 2, 28, 28);
        graphics.setColor(new Color(113, 42, 82, 230));
        graphics.fillOval(6, 6, 20, 20);
        graphics.setColor(new Color(220, 61, 103, 245));
        graphics.fillOval(10, 10, 12, 12);
        graphics.setColor(new Color(255, 216, 170, 255));
        graphics.fillOval(13, 13, 6, 6);
        graphics.setColor(new Color(174, 95, 194, 220));
        graphics.drawOval(3, 3, 26, 26);
        graphics.drawOval(7, 7, 18, 18);
        graphics.dispose();
        ImageIO.write(icon, "png", png);
        writeBossAbilityManifest(png, safeProperties);
        return png;
    }

    public static String readManifestFor(File generatedFile) throws IOException {
        if (generatedFile == null) return "";
        String name = generatedFile.getName();
        int dot = name.lastIndexOf('.');
        File manifest = new File(generatedFile.getParentFile(),
                (dot > 0 ? name.substring(0, dot) : name) + ".json");
        return manifest.isFile() ? Files.readString(manifest.toPath(), StandardCharsets.UTF_8)
                : "Manifesto ainda não encontrado para " + generatedFile.getName();
    }

    private static void drawEnemySilhouette(Graphics2D graphics, EnemyRole role, Color body, Color accent) {
        graphics.setColor(body.darker());
        if (role == EnemyRole.BOMBER) {
            graphics.fillRoundRect(7, 9, 18, 16, 6, 6);
            graphics.setColor(body);
            graphics.fillOval(10, 7, 12, 15);
            graphics.setColor(accent);
            graphics.fillRect(13, 5, 6, 6);
            graphics.fillRect(9, 21, 4, 5);
            graphics.fillRect(19, 21, 4, 5);
        } else if (role == EnemyRole.SHIELDER) {
            graphics.fillRoundRect(9, 9, 14, 15, 5, 5);
            graphics.setColor(accent);
            graphics.fillRoundRect(4, 8, 7, 17, 3, 3);
            graphics.drawLine(5, 8, 5, 25);
        } else if (role == EnemyRole.ARTILLERY) {
            graphics.fillRoundRect(10, 12, 12, 13, 4, 4);
            graphics.setColor(body);
            graphics.fillRect(17, 5, 5, 12);
            graphics.setColor(accent);
            graphics.fillRect(18, 4, 3, 8);
            graphics.fillRect(8, 23, 4, 5);
            graphics.fillRect(20, 23, 4, 5);
        } else if (role == EnemyRole.SWARM) {
            Polygon shell = new Polygon(new int[] {16, 24, 22, 16, 10, 8}, new int[] {5, 11, 23, 28, 23, 11}, 6);
            graphics.fillPolygon(shell);
            graphics.setColor(accent);
            graphics.fillPolygon(new Polygon(new int[] {10, 4, 11}, new int[] {12, 7, 18}, 3));
            graphics.fillPolygon(new Polygon(new int[] {22, 28, 21}, new int[] {12, 7, 18}, 3));
        } else if (role == EnemyRole.SNIPER) {
            graphics.fillRoundRect(10, 11, 12, 14, 4, 4);
            graphics.setColor(accent);
            graphics.fillRect(19, 4, 3, 16);
            graphics.fillRect(16, 4, 8, 3);
        } else if (role == EnemyRole.SAPPER) {
            graphics.fillRoundRect(8, 10, 16, 15, 6, 6);
            graphics.setColor(accent);
            graphics.fillRect(13, 5, 6, 7);
            graphics.fillRect(6, 22, 5, 4);
            graphics.fillRect(21, 22, 5, 4);
        } else if (role == EnemyRole.STALKER) {
            graphics.fillRoundRect(9, 8, 14, 18, 7, 7);
            graphics.setColor(accent);
            graphics.fillRect(7, 12, 18, 3);
            graphics.fillRect(11, 24, 3, 4);
            graphics.fillRect(18, 24, 3, 4);
        } else if (role == EnemyRole.MIRE_HOUND) {
            Polygon hound = new Polygon(new int[] {5, 11, 18, 27, 23, 13, 7}, new int[] {19, 10, 11, 17, 23, 25, 23}, 7);
            graphics.fillPolygon(hound);
            graphics.setColor(accent);
            graphics.fillRect(17, 14, 7, 3);
            graphics.fillRect(8, 8, 3, 6);
            graphics.fillRect(20, 22, 3, 5);
        } else if (role == EnemyRole.BOG_ORACLE) {
            graphics.fillRoundRect(9, 9, 14, 17, 5, 5);
            graphics.setColor(accent);
            graphics.fillOval(11, 5, 10, 10);
            graphics.fillRect(5, 12, 4, 12);
            graphics.fillRect(23, 12, 4, 12);
            graphics.fillRect(14, 24, 4, 4);
        } else if (role == EnemyRole.MIRE_BRUTE) {
            graphics.fillRoundRect(5, 9, 22, 18, 8, 8);
            graphics.fillRect(2, 15, 6, 10);
            graphics.fillRect(24, 15, 6, 10);
            graphics.setColor(accent);
            graphics.fillRect(10, 12, 12, 4);
            graphics.fillRect(13, 5, 6, 6);
        } else if (role == EnemyRole.MIST_SOVEREIGN) {
            Polygon cloak = new Polygon(new int[] {16, 25, 28, 23, 9, 4, 7},
                    new int[] {4, 13, 27, 28, 28, 27, 13}, 7);
            graphics.fillPolygon(cloak);
            graphics.setColor(accent);
            graphics.fillRect(10, 10, 12, 3);
            graphics.fillOval(12, 5, 8, 8);
            graphics.fillRect(5, 4, 3, 10);
            graphics.fillRect(24, 4, 3, 10);
            graphics.fillRect(14, 22, 4, 6);
        } else if (role == EnemyRole.GUARDIAN) {
            graphics.fillRoundRect(5, 8, 22, 18, 7, 7);
            graphics.fillRect(2, 14, 6, 10);
            graphics.fillRect(24, 14, 6, 10);
            graphics.setColor(accent);
            graphics.fillOval(12, 12, 8, 8);
            graphics.fillRect(9, 6, 4, 5);
            graphics.fillRect(19, 6, 4, 5);
        } else {
            graphics.fillRoundRect(8, 8, 16, 17, 6, 6);
            graphics.fillRect(5, 14, 5, 8);
            graphics.fillRect(22, 14, 5, 8);
            graphics.setColor(accent);
            graphics.fillRect(12, 12, 8, 3);
            graphics.fillRect(13, 24, 2, 4);
            graphics.fillRect(18, 24, 2, 4);
        }
        graphics.setColor(Color.WHITE);
        graphics.fillRect(15, 14, 2, 2);
    }

    private static Color tileBase(TileStyle style) {
        if (style == TileStyle.ESTRADA) return new Color(165, 130, 82);
        if (style == TileStyle.RUINAS) return new Color(94, 95, 106);
        if (style == TileStyle.PEDRA) return new Color(100, 104, 110);
        if (style == TileStyle.AREIA) return new Color(192, 158, 96);
        if (style == TileStyle.TECNOLOGIA) return new Color(42, 56, 79);
        return new Color(74, 128, 73);
    }

    private static void drawConsumableIcon(Graphics2D graphics, ConsumableEffect effect) {
        Color liquid = effect == ConsumableEffect.MANA ? new Color(83, 137, 220)
                : effect == ConsumableEffect.FÔLEGO ? new Color(108, 194, 125)
                : effect == ConsumableEffect.TRIAGEM ? new Color(172, 112, 202) : new Color(201, 79, 80);
        graphics.setColor(new Color(9, 14, 22, 120));
        graphics.fillOval(8, 25, 16, 4);
        graphics.setColor(new Color(229, 224, 205));
        graphics.fillRect(13, 4, 6, 5);
        graphics.setColor(new Color(112, 118, 126));
        graphics.fillRoundRect(9, 8, 14, 18, 5, 5);
        graphics.setColor(liquid);
        graphics.fillRoundRect(11, 13, 10, 11, 4, 4);
        graphics.setColor(new Color(255, 246, 194));
        graphics.fillRect(14, 15, 4, 4);
    }

    private static void drawRpgWeaponIcon(Graphics2D graphics, RpgWeaponStyle style) {
        graphics.setColor(new Color(9, 14, 22, 120));
        graphics.fillOval(7, 25, 18, 4);
        if (style == RpgWeaponStyle.CAJADO) {
            graphics.setColor(new Color(110, 75, 47));
            graphics.fillRect(15, 5, 3, 22);
            graphics.setColor(new Color(93, 205, 194));
            graphics.fillOval(11, 3, 11, 10);
        } else if (style == RpgWeaponStyle.MACHADO) {
            graphics.setColor(new Color(116, 77, 48));
            graphics.fillRect(15, 7, 3, 21);
            graphics.setColor(new Color(191, 199, 204));
            graphics.fillRoundRect(7, 7, 12, 10, 4, 4);
        } else if (style == RpgWeaponStyle.ADAGA) {
            graphics.setColor(new Color(202, 211, 214));
            graphics.fillPolygon(new Polygon(new int[] {16, 21, 16}, new int[] {3, 22, 18}, 3));
            graphics.setColor(new Color(172, 121, 61));
            graphics.fillRect(13, 19, 7, 3);
        } else {
            graphics.setColor(new Color(205, 214, 215));
            graphics.fillPolygon(new Polygon(new int[] {16, 20, 17, 14, 12}, new int[] {3, 20, 24, 20, 6}, 5));
            graphics.setColor(new Color(180, 130, 63));
            graphics.fillRect(11, 20, 11, 3);
            graphics.fillRect(14, 22, 4, 6);
        }
    }

    private static void drawRpgSprite(Graphics2D graphics, RpgSpriteKind kind) {
        graphics.setColor(new Color(10, 14, 20, 90));
        graphics.fillOval(7, 25, 18, 4);
        if (kind.getCategory().equals("projectile")) {
            Color core = kind == RpgSpriteKind.PROJECTILE_FIREBOLT ? new Color(242, 125, 55)
                    : kind == RpgSpriteKind.PROJECTILE_MIST ? new Color(170, 102, 215) : new Color(75, 205, 235);
            graphics.setColor(core.darker()); graphics.fillOval(6, 10, 20, 12);
            graphics.setColor(core); graphics.fillOval(10, 8, 14, 14);
            graphics.setColor(new Color(255, 244, 190)); graphics.fillOval(14, 11, 6, 6);
            return;
        }
        if (kind.getCategory().equals("weapon")) {
            if (kind == RpgSpriteKind.WEAPON_ARC_RIFLE) {
                graphics.setColor(new Color(102, 68, 40)); graphics.fillRoundRect(8, 14, 17, 5, 3, 3);
                graphics.setColor(new Color(81, 211, 232)); graphics.fillRect(20, 11, 5, 4); graphics.fillRect(12, 19, 3, 6);
            } else if (kind == RpgSpriteKind.WEAPON_RUNE_SWORD) {
                graphics.setColor(new Color(203, 214, 221));
                graphics.fillPolygon(new Polygon(new int[] {16, 21, 18, 13, 11}, new int[] {3, 20, 25, 20, 7}, 5));
                graphics.setColor(new Color(87, 169, 222)); graphics.fillRect(12, 19, 9, 3);
            } else {
                graphics.setColor(new Color(110, 75, 47)); graphics.fillRect(15, 5, 3, 22);
                graphics.setColor(new Color(218, 177, 77)); graphics.fillOval(11, 3, 11, 10);
            }
            return;
        }
        Color cloak = kind == RpgSpriteKind.NPC_COMMANDANT ? new Color(58, 91, 144)
                : kind == RpgSpriteKind.NPC_HEALER ? new Color(74, 123, 89)
                : kind == RpgSpriteKind.NPC_CARTOGRAPHER ? new Color(118, 83, 150) : new Color(50, 97, 143);
        Color accent = kind == RpgSpriteKind.NPC_COMMANDANT ? new Color(218, 166, 74)
                : kind == RpgSpriteKind.NPC_HEALER ? new Color(102, 208, 183)
                : kind == RpgSpriteKind.NPC_CARTOGRAPHER ? new Color(232, 196, 107) : new Color(224, 188, 104);
        graphics.setColor(new Color(129, 84, 57)); graphics.fillOval(12, 5, 8, 8);
        graphics.setColor(cloak.darker()); graphics.fillRoundRect(9, 12, 14, 14, 5, 5);
        graphics.setColor(cloak); graphics.fillRoundRect(11, 13, 10, 11, 4, 4);
        graphics.setColor(accent); graphics.fillRect(10, 16, 12, 3); graphics.fillRect(12, 24, 3, 4); graphics.fillRect(18, 24, 3, 4);
        if (kind == RpgSpriteKind.HERO || kind == RpgSpriteKind.NPC_COMMANDANT) {
            graphics.setColor(new Color(104, 71, 45)); graphics.fillRect(22, 7, 2, 18);
            graphics.setColor(accent); graphics.fillOval(19, 4, 7, 7);
        }
    }

    private static void writeConsumableManifest(File png, ConsumableProperties properties) throws IOException {
        writeAssetManifest(png, "consumable", properties.effect.name(),
                "  \"displayName\": \"" + properties.displayName + "\",\n"
                + "  \"lifeRestore\": " + properties.lifeRestore + ",\n"
                + "  \"manaRestore\": " + properties.manaRestore + ",\n"
                + "  \"staminaRestore\": " + properties.staminaRestore + ",\n");
    }

    private static void writeRpgWeaponManifest(File png, RpgWeaponStyle style, RpgWeaponProperties properties)
            throws IOException {
        writeAssetManifest(png, "rpg_weapon", style.name(),
                "  \"displayName\": \"" + properties.displayName + "\",\n"
                + "  \"damageBonus\": " + properties.damageBonus + ",\n"
                + "  \"staminaCost\": " + properties.staminaCost + ",\n"
                + "  \"rarity\": \"" + properties.rarity + "\",\n");
    }

    private static void writeRpgSpriteManifest(File png, RpgSpriteKind kind, RpgSpriteProperties properties)
            throws IOException {
        writeAssetManifest(png, "rpg_" + kind.getCategory(), kind.name(),
                "  \"displayName\": \"" + properties.displayName + "\",\n"
                + "  \"gameplayScale\": " + properties.gameplayScale + ",\n"
                + "  \"damage\": " + properties.damage + ",\n"
                + "  \"cooldownTicks\": " + properties.cooldownTicks + ",\n"
                + "  \"shotOriginX\": " + properties.shotOriginX + ",\n"
                + "  \"shotOriginY\": " + properties.shotOriginY + ",\n"
                + "  \"runtimeLoaded\": true,\n");
    }

    private static void refreshRpgContentCatalog(File projectRoot) throws IOException {
        File output = ensureDirectory(projectRoot, "res/assets/generated");
        File catalog = new File(output, "rpg_content_catalog.json");
        try (FileWriter writer = new FileWriter(catalog, StandardCharsets.UTF_8)) {
            writer.write("{\n  \"schema\": 1,\n  \"itemsDirectory\": \"items\",\n"
                    + "  \"weaponsDirectory\": \"rpg_weapons\",\n  \"spritesDirectory\": \"rpg_sprites\",\n"
                    + "  \"autoDiscovery\": true\n}\n");
        }
    }

    private static Color tileAccent(TileStyle style) {
        if (style == TileStyle.ESTRADA) return new Color(211, 179, 119);
        if (style == TileStyle.RUINAS) return new Color(146, 145, 153);
        if (style == TileStyle.PEDRA) return new Color(65, 70, 76);
        if (style == TileStyle.AREIA) return new Color(230, 195, 125);
        if (style == TileStyle.TECNOLOGIA) return new Color(81, 189, 206);
        return new Color(119, 176, 91);
    }

    private static Color defaultBody(EnemyRole role) {
        if (role == EnemyRole.MIST_SOVEREIGN) return new Color(57, 84, 112);
        if (role == EnemyRole.MIRE_HOUND) return new Color(71, 128, 74);
        if (role == EnemyRole.BOG_ORACLE) return new Color(86, 72, 132);
        if (role == EnemyRole.MIRE_BRUTE) return new Color(101, 76, 53);
        if (role == EnemyRole.BOMBER) return new Color(143, 80, 42);
        if (role == EnemyRole.SHIELDER) return new Color(40, 119, 127);
        if (role == EnemyRole.ARTILLERY) return new Color(82, 67, 127);
        if (role == EnemyRole.SWARM) return new Color(66, 123, 68);
        if (role == EnemyRole.GUARDIAN) return new Color(52, 60, 86);
        return new Color(67, 75, 94);
    }

    private static Color defaultAccent(EnemyRole role) {
        if (role == EnemyRole.MIST_SOVEREIGN) return new Color(224, 189, 91);
        if (role == EnemyRole.MIRE_HOUND) return new Color(184, 222, 104);
        if (role == EnemyRole.BOG_ORACLE) return new Color(117, 215, 208);
        if (role == EnemyRole.MIRE_BRUTE) return new Color(233, 139, 79);
        if (role == EnemyRole.BOMBER) return new Color(244, 184, 65);
        if (role == EnemyRole.SHIELDER) return new Color(81, 218, 237);
        if (role == EnemyRole.ARTILLERY) return new Color(245, 135, 71);
        if (role == EnemyRole.SWARM) return new Color(206, 101, 172);
        if (role == EnemyRole.GUARDIAN) return new Color(219, 68, 100);
        return new Color(218, 83, 82);
    }

    private static File ensureDirectory(File projectRoot, String relative) throws IOException {
        File output = new File(projectRoot, relative);
        if (!output.exists() && !output.mkdirs()) {
            throw new IOException("Não foi possível criar " + output.getAbsolutePath());
        }
        return output;
    }

    private static String safeName(String value, String fallback) {
        String candidate = value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "_");
        return candidate.isEmpty() ? fallback : candidate;
    }

    private static String safeDisplayName(String value, String fallback) {
        String candidate = value == null ? "" : value.trim().replaceAll("[\\r\\n\\t]+", " ");
        if (candidate.isEmpty()) return fallback;
        return candidate.length() > 42 ? candidate.substring(0, 42) : candidate;
    }

    private static void writeTileManifest(File png, TileStyle style, int variation, TileProperties properties)
            throws IOException {
        writeAssetManifest(png, "tile", style.name(), "  \"variation\": " + variation + ",\n"
                + "  \"walkable\": " + properties.walkable + ",\n"
                + "  \"movementCost\": " + properties.movementCost + ",\n"
                + "  \"terrainTag\": \"" + properties.terrainTag + "\",\n");
    }

    private static void writeEnemyManifest(File png, EnemyRole role, EnemyProperties properties) throws IOException {
        writeAssetManifest(png, "enemy", role.name(), "  \"baseLife\": " + properties.baseLife + ",\n"
                + "  \"baseDamage\": " + properties.baseDamage + ",\n"
                + "  \"speed\": " + properties.speed + ",\n"
                + "  \"behaviorTag\": \"" + properties.behaviorTag + "\",\n"
                + "  \"boss\": " + properties.boss + ",\n");
    }

    private static void writeBossAbilityManifest(File png, BossAbilityProperties properties) throws IOException {
        writeAssetManifest(png, "boss_ability", properties.ownerRole,
                "  \"damage\": " + properties.damage + ",\n"
                + "  \"cooldownTicks\": " + properties.cooldownTicks + ",\n"
                + "  \"range\": " + properties.range + ",\n");
    }

    private static void writeAssetManifest(File png, String kind, String variant, String propertiesJson) throws IOException {
        File manifest = new File(png.getParentFile(), png.getName().replaceFirst("\\.png$", ".json"));
        try (FileWriter writer = new FileWriter(manifest, StandardCharsets.UTF_8)) {
            writer.write("{\n");
            writer.write("  \"schema\": 1,\n");
            writer.write("  \"kind\": \"" + kind + "\",\n");
            writer.write("  \"variant\": \"" + variant + "\",\n");
            writer.write("  \"file\": \"" + png.getName() + "\",\n");
            writer.write("  \"width\": 32,\n");
            writer.write("  \"height\": 32,\n");
            writer.write(propertiesJson);
            writer.write("  \"alphaRequired\": true\n");
            writer.write("}\n");
        }
    }

    private static String safeTag(String value, String fallback) {
        String candidate = value == null ? "" : value.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "_");
        return candidate.isEmpty() ? fallback : candidate;
    }
}
