package com.traduvertgames.tools;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.text.Normalizer;

import javax.imageio.ImageIO;

/**
 * Inspector e normalizador de PNGs para o pipeline visual do RPG.
 *
 * <p>O Coach trabalha sempre sobre uma cópia em
 * {@code res/assets/generated/rpg_sprites}; o arquivo escolhido pelo artista
 * nunca é sobrescrito. A saída contém canvas 32×32, margem transparente e um
 * manifesto compatível com o carregador Android.</p>
 */
public final class AssetCoach {
    public static final int TARGET_SIZE = 32;
    private static final int SAFE_AREA = 28;
    private static final int ALPHA_VISIBLE = 8;
    private static final int COLOR_TOLERANCE = 42;

    private AssetCoach() {}

    /** Correções previamente aprovadas para cópias de trabalho, nunca para o arquivo-fonte. */
    public enum ApprovedFixRule {
        REMOVE_CONNECTED_BORDER_BACKGROUND("Remover fundo uniforme conectado às bordas"),
        TRIM_VISIBLE_SILHOUETTE("Recortar a silhueta visível com margem segura"),
        CENTER_ON_32PX_CANVAS("Centralizar no canvas runtime 32×32"),
        NEAREST_NEIGHBOR_SCALING("Preservar pixel art com escala nearest-neighbor");

        public final String label;

        ApprovedFixRule(String label) { this.label = label; }
    }

    /** Perfis de importação para os editores externos mais usados pelo pipeline RPG. */
    public enum EditorPreset {
        ASEPRITE("Aseprite", "PNG RGBA; sprite sheet ou frames separados; grade 32×32; 3 frames por ação.", 32, 3,
                EnumSet.allOf(ApprovedFixRule.class)),
        KRITA("Krita", "PNG RGBA sem camada de fundo; canvas 32×32 ou arte maior que será centralizada.", 32, 3,
                EnumSet.of(ApprovedFixRule.REMOVE_CONNECTED_BORDER_BACKGROUND,
                        ApprovedFixRule.CENTER_ON_32PX_CANVAS,
                        ApprovedFixRule.NEAREST_NEIGHBOR_SCALING)),
        PISKEL("Piskel", "PNG com transparência; frames separados ou sprite sheet 32×32; exportação nearest-neighbor.", 32, 3,
                EnumSet.allOf(ApprovedFixRule.class));

        public final String label;
        public final String guidance;
        public final int targetCanvas;
        public final int recommendedFramesPerAction;
        private final EnumSet<ApprovedFixRule> rules;

        EditorPreset(String label, String guidance, int targetCanvas, int recommendedFramesPerAction,
                EnumSet<ApprovedFixRule> rules) {
            this.label = label;
            this.guidance = guidance;
            this.targetCanvas = targetCanvas;
            this.recommendedFramesPerAction = recommendedFramesPerAction;
            this.rules = EnumSet.copyOf(rules);
        }

        /** Retorna uma cópia para a interface poder marcar ou desmarcar regras sem alterar o preset. */
        public EnumSet<ApprovedFixRule> approvedRules() { return EnumSet.copyOf(rules); }

        @Override public String toString() { return label; }
    }

    /** Perfil padrão recomendado pelo Content Studio para sprites destinados ao runtime Android. */
    public static EnumSet<ApprovedFixRule> defaultApprovedFixRules() {
        return EnumSet.allOf(ApprovedFixRule.class);
    }

    /** Texto de auditoria exibido pela fila e incluído nos relatórios de lote. */
    public static String describeApprovedFixRules(Set<ApprovedFixRule> rules) {
        EnumSet<ApprovedFixRule> effective = effectiveRules(rules);
        if (effective.isEmpty()) return "sem correção aprovada";
        StringBuilder text = new StringBuilder();
        for (ApprovedFixRule rule : effective) {
            if (text.length() > 0) text.append("; ");
            text.append(rule.label);
        }
        return text.toString();
    }

    /** Resultado legível da inspeção, reaproveitado pela aba Asset Coach. */
    public static final class Diagnosis {
        public final File source;
        public final boolean readable;
        public final int width;
        public final int height;
        public final boolean alpha;
        public final boolean visiblePixels;
        public final boolean transparentBorder;
        public final boolean targetCanvas;

        private Diagnosis(File source, boolean readable, int width, int height, boolean alpha,
                boolean visiblePixels, boolean transparentBorder, boolean targetCanvas) {
            this.source = source;
            this.readable = readable;
            this.width = width;
            this.height = height;
            this.alpha = alpha;
            this.visiblePixels = visiblePixels;
            this.transparentBorder = transparentBorder;
            this.targetCanvas = targetCanvas;
        }

        public boolean isReady() {
            return readable && alpha && visiblePixels && transparentBorder && targetCanvas;
        }

        public String toReport() {
            if (!readable) {
                return "ASSET COACH — diagnóstico\n\nArquivo não pôde ser lido como PNG. "
                        + "Escolha uma imagem PNG válida antes de normalizar.";
            }
            StringBuilder out = new StringBuilder("ASSET COACH — diagnóstico\n\n");
            out.append("Arquivo: ").append(source.getName()).append('\n');
            out.append("Canvas: ").append(width).append('×').append(height)
                    .append(targetCanvas ? "  ✓ alvo 32×32" : "  • será normalizado para 32×32").append('\n');
            out.append("Alfa: ").append(alpha ? "✓ detectado" : "• ausente; o Coach tenta remover fundo uniforme conectado às bordas")
                    .append('\n');
            out.append("Silhueta: ").append(visiblePixels ? "✓ pixels visíveis" : "✕ sprite vazio").append('\n');
            out.append("Margem: ").append(transparentBorder ? "✓ borda transparente" : "• será criada margem de 2 px")
                    .append("\n\n");
            if (!visiblePixels) {
                out.append("Não exporte este arquivo: não há silhueta visível após a leitura.");
            } else if (isReady()) {
                out.append("Pronto para runtime. Você ainda pode exportar para gerar o manifesto e registrar o asset no catálogo RPG.");
            } else {
                out.append("Plano seguro: preservar o original, recortar a silhueta, centralizar em área útil de 28×28, "
                        + "manter pixel art com nearest-neighbor e escrever manifesto RPG.");
            }
            return out.toString();
        }
    }

    /** Item individual de uma normalização em lote; uma falha não interrompe os demais arquivos. */
    public static final class BatchItem {
        public final File source;
        public final File output;
        public final String error;
        public final List<ApprovedFixRule> appliedRules;

        private BatchItem(File source, File output, String error, Set<ApprovedFixRule> appliedRules) {
            this.source = source;
            this.output = output;
            this.error = error;
            this.appliedRules = Collections.unmodifiableList(new ArrayList<ApprovedFixRule>(effectiveRules(appliedRules)));
        }

        public boolean isSuccess() { return output != null && error == null; }
    }

    /** Resultado agregável e legível da fila de importação do Asset Coach. */
    public static final class BatchReport {
        public final List<BatchItem> items;

        private BatchReport(List<BatchItem> items) {
            this.items = Collections.unmodifiableList(new ArrayList<BatchItem>(items));
        }

        public int successCount() {
            int count = 0;
            for (BatchItem item : items) if (item.isSuccess()) count++;
            return count;
        }

        public int failureCount() { return items.size() - successCount(); }

        public String toReport() {
            StringBuilder out = new StringBuilder("ASSET COACH — importação em lote\n\n");
            for (BatchItem item : items) {
                out.append(item.isSuccess() ? "✓ " : "✕ ").append(item.source.getName());
                out.append(item.isSuccess() ? " → " + item.output.getName() : " — " + item.error);
                out.append("\n  Regras: ").append(describeApprovedFixRules(new java.util.HashSet<ApprovedFixRule>(item.appliedRules))).append('\n');
            }
            out.append("\nConcluídos: ").append(successCount()).append("  •  Falhas isoladas: ")
                    .append(failureCount());
            return out.toString();
        }
    }

    /** Cobertura de frames esperada pelo runtime para uma entidade animável. */
    public static final class AnimationCoverage {
        public final String id;
        public final boolean baseSprite;
        private final boolean[][][] frames = new boolean[2][4][3];

        private AnimationCoverage(String id, boolean baseSprite) {
            this.id = id;
            this.baseSprite = baseSprite;
        }

        private void mark(int state, int direction, int frame) { frames[state][direction][frame] = true; }

        public boolean hasFrame(String state, String direction, int frame) {
            int stateIndex = "attack".equals(state) ? 1 : 0;
            int directionIndex = directionIndex(direction);
            return directionIndex >= 0 && frame >= 0 && frame < 3 && frames[stateIndex][directionIndex][frame];
        }

        public int frameCount() {
            int count = 0;
            for (int state = 0; state < 2; state++) for (int direction = 0; direction < 4; direction++)
                for (int frame = 0; frame < 3; frame++) if (frames[state][direction][frame]) count++;
            return count;
        }

        public boolean isComplete() { return baseSprite && frameCount() == 24; }

        public List<String> missingFrameLabels() {
            String[] states = { "walk", "attack" };
            String[] directions = { "right", "left", "up", "down" };
            List<String> missing = new ArrayList<String>();
            for (String state : states) for (String direction : directions) for (int frame = 0; frame < 3; frame++) {
                if (!hasFrame(state, direction, frame)) missing.add(state + "_" + direction + "_" + frame);
            }
            return missing;
        }
    }

    /** Relatório estruturado da cobertura de caminhada e ataque, reutilizável pela UI e por testes. */
    public static final class AnimationCoverageReport {
        public final List<AnimationCoverage> entities;

        private AnimationCoverageReport(List<AnimationCoverage> entities) {
            this.entities = Collections.unmodifiableList(new ArrayList<AnimationCoverage>(entities));
        }

        public int totalFrames() {
            int count = 0;
            for (AnimationCoverage coverage : entities) count += coverage.frameCount();
            return count;
        }

        public int expectedFrames() { return entities.size() * 24; }

        public String toReport() {
            StringBuilder out = new StringBuilder("COBERTURA DE ANIMAÇÕES RPG\n\n");
            for (AnimationCoverage coverage : entities) {
                out.append(coverage.isComplete() ? "✓ " : "• ").append(coverage.id)
                        .append(" — sprite-base ").append(coverage.baseSprite ? "presente" : "ausente")
                        .append(", ").append(coverage.frameCount()).append("/24 frames\n");
            }
            out.append("\nCobertura total: ").append(totalFrames()).append('/').append(expectedFrames())
                    .append(" frames direcionais.");
            return out.toString();
        }

        /** Dados granulares para auditoria, planilhas e integração externa. */
        public String toCsv() {
            String[] states = { "walk", "attack" };
            String[] directions = { "right", "left", "up", "down" };
            StringBuilder csv = new StringBuilder("entity,base_sprite,state,direction,frame,present,status\n");
            for (AnimationCoverage coverage : entities) {
                for (String state : states) for (String direction : directions) for (int frame = 0; frame < 3; frame++) {
                    boolean present = coverage.hasFrame(state, direction, frame);
                    csv.append(coverage.id).append(',').append(coverage.baseSprite).append(',')
                            .append(state).append(',').append(direction).append(',').append(frame).append(',')
                            .append(present).append(',').append(present ? "ready" : "missing").append('\n');
                }
            }
            return csv.toString();
        }

        public void exportCsv(File destination) throws IOException {
            ensureDestination(destination);
            Files.write(destination.toPath(), toCsv().getBytes(StandardCharsets.UTF_8));
        }

        /** PDF leve e independente de bibliotecas externas, útil para anexar a revisões de arte. */
        public void exportPdf(File destination) throws IOException {
            ensureDestination(destination);
            List<String> lines = new ArrayList<String>();
            lines.add("COBERTURA DE ANIMACOES RPG");
            lines.add("Frames direcionais: " + totalFrames() + "/" + expectedFrames());
            lines.add("Legenda: walk/attack x right/left/up/down x frames 0-2");
            lines.add("");
            for (AnimationCoverage coverage : entities) {
                lines.add(coverage.id + " | base=" + (coverage.baseSprite ? "sim" : "nao")
                        + " | frames=" + coverage.frameCount() + "/24 | " + (coverage.isComplete() ? "PRONTO" : "PENDENTE"));
                List<String> missing = coverage.missingFrameLabels();
                lines.add(missing.isEmpty() ? "  Todos os frames foram encontrados."
                        : "  Ausentes: " + joinLines(missing, 74));
            }
            writePlainTextPdf(destination, lines);
        }
    }

    /** Inspeciona um PNG sem criar ou alterar arquivos no projeto. */
    public static Diagnosis inspect(File source) throws IOException {
        if (source == null || !source.isFile()) {
            return new Diagnosis(source == null ? new File("nenhum_arquivo.png") : source,
                    false, 0, 0, false, false, false, false);
        }
        BufferedImage image = ImageIO.read(source);
        if (image == null) return new Diagnosis(source, false, 0, 0, false, false, false, false);
        boolean alpha = image.getColorModel().hasAlpha();
        boolean visible = false;
        boolean transparentBorder = true;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixelAlpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (pixelAlpha > ALPHA_VISIBLE) visible = true;
                if (x == 0 || y == 0 || x == image.getWidth() - 1 || y == image.getHeight() - 1) {
                    if (pixelAlpha > ALPHA_VISIBLE) transparentBorder = false;
                }
            }
        }
        return new Diagnosis(source, true, image.getWidth(), image.getHeight(), alpha, visible,
                transparentBorder, image.getWidth() == TARGET_SIZE && image.getHeight() == TARGET_SIZE);
    }

    /**
     * Cria uma cópia de trabalho pronta para o APK e seu manifesto. O arquivo
     * original do artista permanece intacto.
     */
    public static File normalizeRpgSprite(File source, String id, ContentStudioProject.RpgSpriteKind kind,
            ContentStudioProject.RpgSpriteProperties properties, File projectRoot) throws IOException {
        return normalizeRpgSprite(source, id, kind, properties, defaultApprovedFixRules(), projectRoot);
    }

    /** Aplica somente regras aprovadas à cópia de trabalho e mantém o PNG do artista intacto. */
    public static File normalizeRpgSprite(File source, String id, ContentStudioProject.RpgSpriteKind kind,
            ContentStudioProject.RpgSpriteProperties properties, Set<ApprovedFixRule> rules, File projectRoot) throws IOException {
        Diagnosis diagnosis = inspect(source);
        if (!diagnosis.readable) throw new IOException("Selecione um PNG legível para o Asset Coach.");
        if (!diagnosis.visiblePixels) throw new IOException("O PNG não contém pixels visíveis para exportar.");
        BufferedImage original = ImageIO.read(source);
        BufferedImage normalized = normalize(original, rules);
        return ContentStudioProject.exportImportedRpgSprite(normalized, safeId(id), kind, properties, projectRoot);
    }

    /** Normaliza muitos PNGs sem impedir a fila quando um arquivo estiver corrompido ou vazio. */
    public static BatchReport normalizeBatch(File[] sources, ContentStudioProject.RpgSpriteKind kind,
            ContentStudioProject.RpgSpriteProperties properties, File projectRoot) {
        return normalizeBatch(sources, kind, properties, defaultApprovedFixRules(), projectRoot);
    }

    /** Normaliza a fila usando um conjunto explícito de regras aprovadas e auditáveis. */
    public static BatchReport normalizeBatch(File[] sources, ContentStudioProject.RpgSpriteKind kind,
            ContentStudioProject.RpgSpriteProperties properties, Set<ApprovedFixRule> rules, File projectRoot) {
        List<BatchItem> items = new ArrayList<BatchItem>();
        if (sources == null) return new BatchReport(items);
        for (File source : sources) {
            try {
                items.add(new BatchItem(source, normalizeRpgSprite(source, baseId(source), kind, properties, rules, projectRoot), null, rules));
            } catch (Exception failure) {
                items.add(new BatchItem(source == null ? new File("arquivo_indefinido.png") : source, null,
                        failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage(), rules));
            }
        }
        return new BatchReport(items);
    }

    /** Produz uma prévia não persistida para a comparação antes/depois na interface. */
    public static BufferedImage normalizedPreview(File source) throws IOException {
        return normalizedPreview(source, defaultApprovedFixRules());
    }

    /** Produz a prévia correspondente exatamente às regras aprovadas selecionadas na fila. */
    public static BufferedImage normalizedPreview(File source, Set<ApprovedFixRule> rules) throws IOException {
        Diagnosis diagnosis = inspect(source);
        if (!diagnosis.readable || !diagnosis.visiblePixels) {
            throw new IOException("Selecione um PNG legível com pixels visíveis para comparar.");
        }
        return normalize(ImageIO.read(source), rules);
    }

    /** Lê a cobertura de quatro protagonistas/NPCs com o padrão de nomes aceito pelo runtime. */
    public static AnimationCoverageReport inspectAnimationCoverage(File projectRoot) {
        File directory = new File(projectRoot, "res/assets/generated/rpg_sprites");
        String[] ids = { "hero", "npc_commandant", "npc_healer", "npc_cartographer" };
        String[] states = { "walk", "attack" };
        String[] directions = { "right", "left", "up", "down" };
        List<AnimationCoverage> entities = new ArrayList<AnimationCoverage>();
        for (String id : ids) {
            AnimationCoverage coverage = new AnimationCoverage(id, new File(directory, id + ".png").isFile());
            for (int state = 0; state < states.length; state++) {
                for (int direction = 0; direction < directions.length; direction++) {
                    for (int frame = 0; frame < 3; frame++) {
                        if (new File(directory, id + "_" + states[state] + "_" + directions[direction]
                                + "_" + frame + ".png").isFile()) coverage.mark(state, direction, frame);
                    }
                }
            }
            entities.add(coverage);
        }
        return new AnimationCoverageReport(entities);
    }

    /** Normaliza uma imagem para 32×32 com margem transparente e pixel art preservado. */
    static BufferedImage normalize(BufferedImage source) throws IOException {
        return normalize(source, defaultApprovedFixRules());
    }

    static BufferedImage normalize(BufferedImage source, Set<ApprovedFixRule> requestedRules) throws IOException {
        EnumSet<ApprovedFixRule> rules = effectiveRules(requestedRules);
        BufferedImage alphaReady = copyToArgb(source);
        if (rules.contains(ApprovedFixRule.REMOVE_CONNECTED_BORDER_BACKGROUND)) removeUniformBorderBackground(alphaReady);
        int[] bounds = rules.contains(ApprovedFixRule.TRIM_VISIBLE_SILHOUETTE) ? visibleBounds(alphaReady)
                : new int[] { 0, 0, alphaReady.getWidth() - 1, alphaReady.getHeight() - 1 };
        if (bounds == null) throw new IOException("A normalização removeu toda a silhueta; use uma imagem com sujeito visível.");
        int margin = rules.contains(ApprovedFixRule.TRIM_VISIBLE_SILHOUETTE) ? 2 : 0;
        int left = Math.max(0, bounds[0] - margin);
        int top = Math.max(0, bounds[1] - margin);
        int right = Math.min(alphaReady.getWidth(), bounds[2] + margin + 1);
        int bottom = Math.min(alphaReady.getHeight(), bounds[3] + margin + 1);
        BufferedImage cropped = alphaReady.getSubimage(left, top, right - left, bottom - top);
        double scale = Math.min((double) SAFE_AREA / cropped.getWidth(), (double) SAFE_AREA / cropped.getHeight());
        int width = Math.max(1, (int) Math.round(cropped.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(cropped.getHeight() * scale));
        BufferedImage target = new BufferedImage(TARGET_SIZE, TARGET_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, rules.contains(ApprovedFixRule.NEAREST_NEIGHBOR_SCALING)
                ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        int drawX = rules.contains(ApprovedFixRule.CENTER_ON_32PX_CANVAS) ? (TARGET_SIZE - width) / 2 : 0;
        int drawY = rules.contains(ApprovedFixRule.CENTER_ON_32PX_CANVAS) ? (TARGET_SIZE - height) / 2 : 0;
        graphics.drawImage(cropped, drawX, drawY, width, height, null);
        graphics.dispose();
        return target;
    }

    private static EnumSet<ApprovedFixRule> effectiveRules(Set<ApprovedFixRule> rules) {
        if (rules == null) return defaultApprovedFixRules();
        if (rules.isEmpty()) return EnumSet.noneOf(ApprovedFixRule.class);
        return EnumSet.copyOf(rules);
    }

    private static void ensureDestination(File destination) throws IOException {
        if (destination == null) throw new IOException("Escolha um arquivo de destino para o relatório.");
        File parent = destination.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta do relatório.");
        }
    }

    private static String joinLines(List<String> values, int limit) {
        StringBuilder text = new StringBuilder();
        for (String value : values) {
            int separator = text.length() == 0 ? 0 : 2;
            if (text.length() + separator + value.length() > limit && text.length() > 0) {
                text.append(", …");
                break;
            }
            if (separator > 0) text.append(", ");
            text.append(value);
        }
        return text.toString();
    }

    private static void writePlainTextPdf(File destination, List<String> lines) throws IOException {
        final int linesPerPage = 46;
        int pageCount = Math.max(1, (lines.size() + linesPerPage - 1) / linesPerPage);
        int fontObject = 3 + pageCount * 2;
        List<String> objects = new ArrayList<String>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int page = 0; page < pageCount; page++) kids.append(3 + page * 2).append(" 0 R ");
        objects.add("<< /Type /Pages /Kids [ " + kids + "] /Count " + pageCount + " >>");
        for (int page = 0; page < pageCount; page++) {
            int pageObject = 3 + page * 2;
            int contentObject = pageObject + 1;
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 "
                    + fontObject + " 0 R >> >> /Contents " + contentObject + " 0 R >>");
            StringBuilder content = new StringBuilder("BT\n/F1 10 Tf\n50 750 Td\n14 TL\n");
            int first = page * linesPerPage;
            int last = Math.min(lines.size(), first + linesPerPage);
            for (int index = first; index < last; index++) content.append('(').append(pdfEscape(lines.get(index))).append(") Tj\nT*\n");
            content.append("ET");
            String stream = content.toString();
            objects.add("<< /Length " + stream.getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n" + stream + "\nendstream");
        }
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<Integer>();
        offsets.add(0);
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(output.size());
            output.write((index + 1 + " 0 obj\n" + objects.get(index) + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = output.size();
        output.write(("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n").getBytes(StandardCharsets.ISO_8859_1));
        for (int index = 1; index < offsets.size(); index++) output.write(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(index)).getBytes(StandardCharsets.ISO_8859_1));
        output.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
        Files.write(destination.toPath(), output.toByteArray());
    }

    private static String pdfEscape(String text) {
        String ascii = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^\\x20-\\x7E]", "?");
        return ascii.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static BufferedImage copyToArgb(BufferedImage source) {
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return target;
    }

    /** Remove apenas uma cor quase uniforme conectada à borda, preservando detalhes internos. */
    private static void removeUniformBorderBackground(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width < 2 || height < 2) return;
        int[] corners = { image.getRGB(0, 0), image.getRGB(width - 1, 0), image.getRGB(0, height - 1),
                image.getRGB(width - 1, height - 1) };
        int reference = corners[0];
        for (int corner : corners) {
            if (!similar(reference, corner)) return;
        }
        boolean[] removed = new boolean[width * height];
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        for (int x = 0; x < width; x++) { enqueue(image, reference, x, 0, removed, queue); enqueue(image, reference, x, height - 1, removed, queue); }
        for (int y = 0; y < height; y++) { enqueue(image, reference, 0, y, removed, queue); enqueue(image, reference, width - 1, y, removed, queue); }
        while (!queue.isEmpty()) {
            int index = queue.removeFirst();
            int x = index % width;
            int y = index / width;
            image.setRGB(x, y, 0);
            if (x > 0) enqueue(image, reference, x - 1, y, removed, queue);
            if (x + 1 < width) enqueue(image, reference, x + 1, y, removed, queue);
            if (y > 0) enqueue(image, reference, x, y - 1, removed, queue);
            if (y + 1 < height) enqueue(image, reference, x, y + 1, removed, queue);
        }
    }

    private static void enqueue(BufferedImage image, int reference, int x, int y, boolean[] removed,
            ArrayDeque<Integer> queue) {
        int index = y * image.getWidth() + x;
        if (removed[index] || !similar(reference, image.getRGB(x, y))) return;
        removed[index] = true;
        queue.addLast(index);
    }

    private static boolean similar(int first, int second) {
        int alphaA = (first >>> 24) & 0xFF;
        int alphaB = (second >>> 24) & 0xFF;
        if (alphaB <= ALPHA_VISIBLE) return true;
        if (alphaA <= ALPHA_VISIBLE) return false;
        int red = Math.abs(((first >>> 16) & 0xFF) - ((second >>> 16) & 0xFF));
        int green = Math.abs(((first >>> 8) & 0xFF) - ((second >>> 8) & 0xFF));
        int blue = Math.abs((first & 0xFF) - (second & 0xFF));
        return red <= COLOR_TOLERANCE && green <= COLOR_TOLERANCE && blue <= COLOR_TOLERANCE;
    }

    private static String baseId(File source) {
        if (source == null) return "hero";
        String name = source.getName();
        int dot = name.lastIndexOf('.');
        return safeId(dot > 0 ? name.substring(0, dot) : name);
    }

    private static int directionIndex(String direction) {
        if ("right".equals(direction)) return 0;
        if ("left".equals(direction)) return 1;
        if ("up".equals(direction)) return 2;
        if ("down".equals(direction)) return 3;
        return -1;
    }

    private static int[] visibleBounds(BufferedImage image) {
        int left = image.getWidth(); int top = image.getHeight(); int right = -1; int bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) <= ALPHA_VISIBLE) continue;
                left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x); bottom = Math.max(bottom, y);
            }
        }
        return right < left ? null : new int[] { left, top, right, bottom };
    }

    private static String safeId(String value) {
        String cleaned = value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]+", "_");
        cleaned = cleaned.replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "hero" : cleaned;
    }
}
