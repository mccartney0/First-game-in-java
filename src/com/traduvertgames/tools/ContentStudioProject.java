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
    public enum EnemyRole { SCOUT, BOMBER, SHIELDER, ARTILLERY, SWARM, GUARDIAN }

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

        public EnemyProperties(int baseLife, int baseDamage, double speed, String behaviorTag) {
            this.baseLife = Math.max(1, baseLife);
            this.baseDamage = Math.max(0, baseDamage);
            this.speed = Math.max(0.1, speed);
            this.behaviorTag = safeTag(behaviorTag, "chase");
        }

        public static EnemyProperties defaults(EnemyRole role) {
            if (role == EnemyRole.GUARDIAN) return new EnemyProperties(18, 5, 0.8, "guardian");
            if (role == EnemyRole.ARTILLERY) return new EnemyProperties(7, 4, 1.1, "ranged");
            if (role == EnemyRole.SHIELDER) return new EnemyProperties(10, 2, 0.9, "shield");
            if (role == EnemyRole.BOMBER) return new EnemyProperties(6, 6, 1.2, "explosive");
            if (role == EnemyRole.SWARM) return new EnemyProperties(3, 1, 1.7, "swarm");
            return new EnemyProperties(5, 2, 1.4, "chase");
        }
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

    public static File generateEnemySprite(EnemyRole role, Color body, Color accent, File projectRoot) throws IOException {
        EnemyRole safeRole = role == null ? EnemyRole.SCOUT : role;
        return generateEnemySprite(safeRole, body, accent, EnemyProperties.defaults(safeRole), projectRoot);
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

    private static Color tileAccent(TileStyle style) {
        if (style == TileStyle.ESTRADA) return new Color(211, 179, 119);
        if (style == TileStyle.RUINAS) return new Color(146, 145, 153);
        if (style == TileStyle.PEDRA) return new Color(65, 70, 76);
        if (style == TileStyle.AREIA) return new Color(230, 195, 125);
        if (style == TileStyle.TECNOLOGIA) return new Color(81, 189, 206);
        return new Color(119, 176, 91);
    }

    private static Color defaultBody(EnemyRole role) {
        if (role == EnemyRole.BOMBER) return new Color(143, 80, 42);
        if (role == EnemyRole.SHIELDER) return new Color(40, 119, 127);
        if (role == EnemyRole.ARTILLERY) return new Color(82, 67, 127);
        if (role == EnemyRole.SWARM) return new Color(66, 123, 68);
        if (role == EnemyRole.GUARDIAN) return new Color(52, 60, 86);
        return new Color(67, 75, 94);
    }

    private static Color defaultAccent(EnemyRole role) {
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
                + "  \"behaviorTag\": \"" + properties.behaviorTag + "\",\n");
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
