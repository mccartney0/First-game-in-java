package com.traduvertgames.tools;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Locale;

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
        Diagnosis diagnosis = inspect(source);
        if (!diagnosis.readable) throw new IOException("Selecione um PNG legível para o Asset Coach.");
        if (!diagnosis.visiblePixels) throw new IOException("O PNG não contém pixels visíveis para exportar.");
        BufferedImage original = ImageIO.read(source);
        BufferedImage normalized = normalize(original);
        return ContentStudioProject.exportImportedRpgSprite(normalized, safeId(id), kind, properties, projectRoot);
    }

    /** Normaliza uma imagem para 32×32 com margem transparente e pixel art preservado. */
    static BufferedImage normalize(BufferedImage source) throws IOException {
        BufferedImage alphaReady = copyToArgb(source);
        removeUniformBorderBackground(alphaReady);
        int[] bounds = visibleBounds(alphaReady);
        if (bounds == null) throw new IOException("A normalização removeu toda a silhueta; use uma imagem com sujeito visível.");
        int left = Math.max(0, bounds[0] - 2);
        int top = Math.max(0, bounds[1] - 2);
        int right = Math.min(alphaReady.getWidth(), bounds[2] + 3);
        int bottom = Math.min(alphaReady.getHeight(), bounds[3] + 3);
        BufferedImage cropped = alphaReady.getSubimage(left, top, right - left, bottom - top);
        double scale = Math.min((double) SAFE_AREA / cropped.getWidth(), (double) SAFE_AREA / cropped.getHeight());
        int width = Math.max(1, (int) Math.round(cropped.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(cropped.getHeight() * scale));
        BufferedImage target = new BufferedImage(TARGET_SIZE, TARGET_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.drawImage(cropped, (TARGET_SIZE - width) / 2, (TARGET_SIZE - height) / 2, width, height, null);
        graphics.dispose();
        return target;
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
