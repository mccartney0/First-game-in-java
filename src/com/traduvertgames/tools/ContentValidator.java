package com.traduvertgames.tools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

/** Valida contratos de conteúdo antes da exportação para o runtime. */
public final class ContentValidator {
    private static final Pattern CATALOG_PATH = Pattern.compile("/assets/generated/([A-Za-z0-9_./-]+\\.png)");
    private static final Pattern MANIFEST_PATH = Pattern.compile("res/assets/generated/([A-Za-z0-9_./-]+\\.(?:png|json))");

    private ContentValidator() { }

    public enum Check {
        FILE("Arquivo"), TRANSPARENCY("Transparência"), SCALE("Escala"), ATLAS("Atlas"),
        METADATA("Metadados"), REFERENCE("Referência"), RUNTIME("Runtime");
        private final String label;
        Check(String label) { this.label = label; }
        public String label() { return label; }
    }

    public static final class Issue {
        private final Check check;
        private final String target;
        private final String message;
        private final boolean warning;

        private Issue(Check check, String target, String message, boolean warning) {
            this.check = check;
            this.target = target;
            this.message = message;
            this.warning = warning;
        }

        public Check getCheck() { return check; }
        public String getTarget() { return target; }
        public String getMessage() { return message; }
        public boolean isWarning() { return warning; }

        @Override
        public String toString() {
            return (warning ? "WARN" : "ERRO") + " [" + check.label() + "] " + target + " — " + message;
        }
    }

    public static final class Report {
        private final List<Issue> issues = new ArrayList<Issue>();

        private void error(Check check, String target, String message) {
            issues.add(new Issue(check, target, message, false));
        }

        private void warning(Check check, String target, String message) {
            issues.add(new Issue(check, target, message, true));
        }

        public List<Issue> getIssues() {
            return Collections.unmodifiableList(issues);
        }

        public boolean isValid() {
            for (Issue issue : issues) if (!issue.warning) return false;
            return true;
        }

        public long errorCount() {
            return issues.stream().filter(issue -> !issue.warning).count();
        }

        public long warningCount() {
            return issues.stream().filter(Issue::isWarning).count();
        }

        public String toText() {
            StringBuilder result = new StringBuilder();
            result.append("Content Studio — validação de conteúdo\n");
            result.append("====================================\n");
            if (issues.isEmpty()) result.append("OK: nenhum problema encontrado.\n");
            for (Issue issue : issues) result.append(issue).append('\n');
            result.append("\nResumo: ").append(errorCount()).append(" erro(s), ")
                    .append(warningCount()).append(" aviso(s).\n");
            return result.toString();
        }

        public String toJson() {
            StringBuilder result = new StringBuilder("{\n  \"valid\": ");
            result.append(isValid()).append(",\n  \"errors\": ").append(errorCount())
                    .append(",\n  \"warnings\": ").append(warningCount()).append(",\n  \"issues\": [\n");
            for (int i = 0; i < issues.size(); i++) {
                Issue issue = issues.get(i);
                result.append("    {\"check\":\"").append(json(issue.check.label()))
                        .append("\",\"target\":\"").append(json(issue.target))
                        .append("\",\"severity\":\"").append(issue.warning ? "warning" : "error")
                        .append("\",\"message\":\"").append(json(issue.message)).append("\"}");
                if (i + 1 < issues.size()) result.append(',');
                result.append('\n');
            }
            return result.append("  ]\n}\n").toString();
        }
    }

    public static Report validate(File projectRoot) throws IOException {
        Report report = new Report();
        File generated = new File(projectRoot, "res/assets/generated");
        File catalog = new File(projectRoot, "src/com/traduvertgames/graficos/AssetCatalog.java");
        File userManifest = new File(generated, "user_asset_manifest.json");
        if (!catalog.isFile()) report.error(Check.FILE, catalog.getPath(), "AssetCatalog.java não foi encontrado.");
        if (!generated.isDirectory()) {
            report.error(Check.FILE, generated.getPath(), "Diretório de assets gerados não encontrado.");
            return report;
        }

        String catalogText = catalog.isFile() ? Files.readString(catalog.toPath(), StandardCharsets.UTF_8) : "";
        String manifestText = userManifest.isFile()
                ? Files.readString(userManifest.toPath(), StandardCharsets.UTF_8) : "";
        Set<String> catalogPaths = catalogPaths(catalogText);
        Set<String> manifestPaths = manifestPaths(manifestText);

        validateCatalogFiles(projectRoot, catalogPaths, report);
        Set<String> declaredPaths = new LinkedHashSet<String>();
        declaredPaths.addAll(catalogPaths);
        declaredPaths.addAll(manifestPaths);
        validateRuntimeFiles(projectRoot, declaredPaths, report);
        validateOrphans(projectRoot, catalogPaths, manifestPaths, report);
        validateTransparency(projectRoot, catalogPaths, manifestPaths, report);
        validateScale(projectRoot, catalogPaths, catalogText, report);
        validateAtlases(projectRoot, report);
        validateMetadata(projectRoot, report);
        validateReferences(manifestText, report);
        return report;
    }

    public static void main(String[] args) throws Exception {
        File root = args.length == 0 ? new File(System.getProperty("user.dir")) : new File(args[0]);
        Report report = validate(root);
        File output = new File(root, "res/assets/generated/content_validation_report.json");
        Files.writeString(output.toPath(), report.toJson(), StandardCharsets.UTF_8);
        System.out.print(report.toText());
        if (!report.isValid()) System.exit(1);
    }

    private static Set<String> catalogPaths(String text) {
        Set<String> result = new LinkedHashSet<String>();
        Matcher matcher = CATALOG_PATH.matcher(text);
        while (matcher.find()) result.add("res/assets/generated/" + matcher.group(1));
        return result;
    }

    private static Set<String> manifestPaths(String text) {
        Set<String> result = new LinkedHashSet<String>();
        Matcher matcher = MANIFEST_PATH.matcher(text);
        while (matcher.find()) result.add("res/assets/generated/" + matcher.group(1));
        return result;
    }

    private static void validateCatalogFiles(File root, Set<String> paths, Report report) {
        for (String relative : paths) {
            if (!new File(root, relative).isFile()) {
                report.error(Check.FILE, relative, "Arquivo usado pelo AssetCatalog não foi encontrado no pacote de conteúdo.");
            }
        }
    }

    private static void validateRuntimeFiles(File root, Set<String> paths, Report report) {
        for (String relative : paths) {
            File file = new File(root, relative);
            if (!file.isFile()) {
                report.error(Check.RUNTIME, relative, "Arquivo declarado não está presente no pacote final.");
                continue;
            }
            if (!relative.toLowerCase(Locale.ROOT).endsWith(".png")) continue;
            try {
                if (ImageIO.read(file) == null) report.error(Check.RUNTIME, relative,
                        "Arquivo existe, mas não é uma imagem PNG carregável.");
            } catch (IOException failure) {
                report.error(Check.RUNTIME, relative, "Falha ao carregar o arquivo: " + failure.getMessage());
            }
        }
    }

    private static void validateOrphans(File root, Set<String> catalog, Set<String> manifest, Report report)
            throws IOException {
        Path generated = new File(root, "res/assets/generated").toPath();
        try (java.util.stream.Stream<Path> files = Files.walk(generated)) {
            files.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".png")).forEach(path -> {
                String relative = normalize(root.toPath().relativize(path));
                if (isStaticCatalogCandidate(relative) && !catalog.contains(relative) && !manifest.contains(relative)) {
                    report.error(Check.FILE, relative, "Arquivo existe, mas não está no AssetCatalog nem no manifesto.");
                }
            });
        }
    }

    private static void validateTransparency(File root, Set<String> catalog, Set<String> manifest, Report report) {
        Set<String> paths = new LinkedHashSet<String>();
        paths.addAll(catalog);
        paths.addAll(manifest);
        for (String relative : paths) {
            if (!requiresAlpha(relative) || !relative.toLowerCase(Locale.ROOT).endsWith(".png")) continue;
            File file = new File(root, relative);
            if (!file.isFile()) continue;
            try {
                BufferedImage image = ImageIO.read(file);
                if (image == null) continue;
                if (!image.getColorModel().hasAlpha()) {
                    report.error(Check.TRANSPARENCY, relative, "Fundo sólido encontrado: PNG sem canal alfa.");
                    continue;
                }
                int transparent = 0;
                int visible = 0;
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                        if (alpha == 0) transparent++;
                        else if (alpha > 8) visible++;
                    }
                }
                if (visible == 0) report.error(Check.TRANSPARENCY, relative, "Sprite completamente transparente.");
                if (transparent == 0) report.error(Check.TRANSPARENCY, relative,
                        "Fundo sólido encontrado em um sprite que deveria ter alfa.");
            } catch (IOException failure) {
                report.error(Check.RUNTIME, relative, "Imagem não pôde ser lida: " + failure.getMessage());
            }
        }
    }

    private static void validateScale(File root, Set<String> catalog, String catalogText, Report report) {
        for (String relative : catalog) {
            if (isAtlas(relative) || isWorldOrTile(relative)) continue;
            File file = new File(root, relative);
            if (!file.isFile()) continue;
            try {
                BufferedImage image = ImageIO.read(file);
                if (image == null || Math.max(image.getWidth(), image.getHeight()) <= 1024) continue;
                boolean normalized = relative.contains("/weapons/") && catalogText.contains("drawWeaponIcon")
                        || relative.contains("/enemies/") && catalogText.contains("normalizeEnemySprite");
                if (!normalized) report.error(Check.SCALE, relative,
                        "Sprite de " + image.getWidth() + "×" + image.getHeight()
                                + " px usado sem contrato de escala de gameplay.");
            } catch (IOException failure) {
                report.error(Check.RUNTIME, relative, "Falha ao verificar escala: " + failure.getMessage());
            }
        }
    }

    private static void validateAtlases(File root, Report report) throws IOException {
        validateAtlas(root, "res/assets/generated/companions/companion_set_clean.png",
                "res/assets/generated/atlas_cells/companions", 3, report);
        validateAtlas(root, "res/assets/generated/enemies/enemy_set_clean.png",
                "res/assets/generated/atlas_cells/enemies", 6, report);
    }

    private static void validateAtlas(File root, String atlasRelative, String cellsRelative, int expected,
            Report report) throws IOException {
        File atlasFile = new File(root, atlasRelative);
        if (!atlasFile.isFile()) return;
        BufferedImage atlas = ImageIO.read(atlasFile);
        if (atlas == null) {
            report.error(Check.ATLAS, atlasRelative, "Atlas não pôde ser carregado.");
            return;
        }
        File directory = new File(root, cellsRelative);
        File[] cells = directory.listFiles((dir, name) -> name.endsWith(".png"));
        int count = cells == null ? 0 : cells.length;
        if (count != expected) {
            report.error(Check.ATLAS, atlasRelative,
                    "Grade inconsistente: esperadas " + expected + " células, encontradas " + count + ".");
            return;
        }
        for (File cell : cells) {
            BufferedImage image = ImageIO.read(cell);
            if (image == null || image.getWidth() == 0 || image.getHeight() == 0 || noVisiblePixel(image)) {
                report.error(Check.ATLAS, cell.getPath(), "Célula vazia ou ilegível.");
            }
        }
    }

    private static void validateMetadata(File root, Report report) throws IOException {
        File weaponType = new File(root, "src/com/traduvertgames/entities/WeaponType.java");
        if (!weaponType.isFile()) {
            report.error(Check.METADATA, weaponType.getPath(), "Fonte de metadados de armas não encontrada.");
        } else {
            String source = Files.readString(weaponType.toPath(), StandardCharsets.UTF_8);
            if (!source.contains("damage") || !source.contains("getDamage")) {
                report.error(Check.METADATA, "WeaponType.java", "Armas sem metadado de dano.");
            }
            if (!source.contains("fireDelayFrames") || !source.contains("getFireDelayFrames")) {
                report.error(Check.METADATA, "WeaponType.java", "Armas sem metadado de cadência.");
            }
            if (!containsAny(source, "muzzle", "barrel", "shotOrigin", "fireOrigin", "disparoX", "disparoY")) {
                report.error(Check.METADATA, "WeaponType.java",
                        "Armas sem ponto de disparo explícito (muzzle/barrel/shotOrigin).");
            }
        }
        Path generated = new File(root, "res/assets/generated").toPath();
        try (java.util.stream.Stream<Path> files = Files.walk(generated)) {
            files.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> validateJson(path.toFile(), report));
        }
    }

    private static void validateJson(File file, Report report) {
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            String path = file.getPath();
            if (path.contains("/enemies/") && (!hasNumber(text, "baseLife") || !hasNumber(text, "baseDamage"))) {
                report.error(Check.METADATA, path, "Inimigo sem baseLife ou baseDamage numérico.");
            }
            if (path.contains("/rpg_weapons/") && (!hasNumber(text, "damageBonus") || !text.contains("rarity"))) {
                report.error(Check.METADATA, path, "Arma RPG sem damageBonus ou raridade.");
            }
        } catch (IOException failure) {
            report.error(Check.RUNTIME, file.getPath(), "Manifesto JSON não pôde ser lido: " + failure.getMessage());
        }
    }

    private static void validateReferences(String manifest, Report report) {
        if (manifest.isEmpty()) {
            report.error(Check.REFERENCE, "user_asset_manifest.json", "Manifesto de assets do usuário não foi encontrado.");
            return;
        }
        String[] assets = manifest.split("\\{\\s*\\\"source\\\"");
        for (int i = 1; i < assets.length; i++) {
            String block = assets[i];
            String source = block.split("\\\"")[1];
            String category = value(block, "category");
            boolean runtimeLoaded = block.contains("\"runtime_loaded\": true");
            if (!runtimeLoaded && !category.contains("reference")) {
                report.error(Check.REFERENCE, source,
                        "Asset importado, mas nenhum inimigo, arma ou cena o utiliza no runtime.");
            }
            if (!block.contains("res/assets/generated/")
                    || !(block.contains(".png") || block.contains(".webp"))) {
                report.error(Check.RUNTIME, source, "Asset sem saída registrada no manifesto.");
            }
        }
    }

    private static boolean isStaticCatalogCandidate(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.startsWith("res/assets/generated/") && lower.endsWith(".png")
                && (lower.contains("/weapons/") || lower.contains("/companions/")
                    || lower.contains("/world/"));
    }

    private static boolean isWorldOrTile(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.contains("/world/") || lower.contains("/tiles/");
    }

    private static boolean requiresAlpha(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.contains("/weapons/") || lower.contains("/enemies/") || lower.contains("/companions/")
                || lower.contains("/effects/") || lower.contains("/items/") || lower.contains("/rpg_weapons/")
                || lower.contains("/abilities/") || lower.endsWith("/dungeon_portal.png");
    }

    private static boolean isAtlas(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.contains("_set") || lower.contains("/companions/companion_set")
                || lower.contains("/enemies/enemy_set");
    }

    private static boolean noVisiblePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 8) return false;
            }
        }
        return true;
    }

    private static boolean containsAny(String text, String... values) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String value : values) if (lower.contains(value.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private static boolean hasNumber(String text, String key) {
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        return text.matches("(?s).*\\\"" + Pattern.quote(normalizedKey)
                + "\\\"\\s*:\\s*-?[0-9]+(?:\\.[0-9]+)?.*");
    }

    private static String value(String block, String key) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(block);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : "";
    }

    private static String normalize(Path path) {
        return path.toString().replace(File.separatorChar, '/');
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
