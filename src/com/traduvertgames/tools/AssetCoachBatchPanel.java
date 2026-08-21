package com.traduvertgames.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** Operações em lote e comparação reversível que complementam a aba Asset Coach. */
final class AssetCoachBatchPanel extends JPanel {
    private final File projectRoot;
    private final AtomicReference<File> latestExport;
    private final JTextArea activity;
    private final List<File> sources = new ArrayList<File>();
    private final Set<File> workingCopies = new LinkedHashSet<File>();
    private final AtomicReference<File> lastWorkingCopy = new AtomicReference<File>();
    private final JTextArea report = new JTextArea();
    private final JTextArea selectedFiles = new JTextArea();
    private final JLabel before = previewLabel("Antes — fonte original");
    private final JLabel after = previewLabel("Depois — prévia normalizada");
    private final JComboBox<ContentStudioProject.RpgSpriteKind> kind =
            new JComboBox<ContentStudioProject.RpgSpriteKind>(ContentStudioProject.RpgSpriteKind.values());

    AssetCoachBatchPanel(File projectRoot, AtomicReference<File> latestExport, JTextArea activity) {
        super(new BorderLayout(10, 10));
        this.projectRoot = projectRoot;
        this.latestExport = latestExport;
        this.activity = activity;
        setBackground(new Color(31, 39, 51));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));

        JLabel title = new JLabel("ASSET COACH — LOTE, COMPARAÇÃO E DESFAZER");
        title.setFont(new Font("Dialog", Font.BOLD, 15));
        title.setForeground(new Color(245, 218, 146));
        JLabel note = new JLabel("A fonte nunca é alterada. Desfazer apaga somente a cópia de trabalho exportada nesta sessão.");
        note.setForeground(new Color(194, 219, 196));
        JPanel header = new JPanel(new BorderLayout(4, 4));
        header.setOpaque(false); header.add(title, BorderLayout.NORTH); header.add(note, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JButton choose = new JButton("Selecionar PNGs…");
        JButton process = new JButton("Normalizar lote e exportar");
        JButton undo = new JButton("Desfazer última cópia");
        JPanel commands = new JPanel();
        commands.setOpaque(false); commands.add(new JLabel("Tipo runtime:")); commands.add(kind);
        commands.add(choose); commands.add(process); commands.add(undo);

        selectedFiles.setEditable(false); selectedFiles.setLineWrap(true); selectedFiles.setWrapStyleWord(true);
        selectedFiles.setBackground(new Color(13, 17, 24)); selectedFiles.setForeground(new Color(205, 222, 211));
        JScrollPane selectedScroll = new JScrollPane(selectedFiles);
        selectedScroll.setBorder(BorderFactory.createTitledBorder("Fila de arquivos"));
        selectedScroll.setPreferredSize(new Dimension(780, 74));

        JPanel comparisons = new JPanel(new GridLayout(1, 2, 10, 10));
        comparisons.setOpaque(false); comparisons.add(before); comparisons.add(after);
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false); center.add(commands, BorderLayout.NORTH); center.add(comparisons, BorderLayout.CENTER);
        center.add(selectedScroll, BorderLayout.SOUTH); add(center, BorderLayout.CENTER);

        report.setEditable(false); report.setLineWrap(true); report.setWrapStyleWord(true);
        report.setBackground(new Color(13, 17, 24)); report.setForeground(new Color(194, 219, 196));
        report.setText("Selecione vários PNGs para montar uma fila. A prévia mostra a transformação antes de qualquer exportação.");
        JScrollPane reportScroll = new JScrollPane(report);
        reportScroll.setPreferredSize(new Dimension(780, 142));
        reportScroll.setBorder(BorderFactory.createTitledBorder("Resultado por arquivo")); add(reportScroll, BorderLayout.SOUTH);

        choose.addActionListener(event -> chooseFiles());
        process.addActionListener(event -> processBatch());
        undo.addActionListener(event -> undoLastCopy());
    }

    private static JLabel previewLabel(String title) {
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setOpaque(true); label.setBackground(new Color(13, 17, 24)); label.setForeground(new Color(180, 194, 201));
        label.setPreferredSize(new Dimension(280, 280)); label.setBorder(BorderFactory.createTitledBorder(title));
        return label;
    }

    private void chooseFiles() {
        JFileChooser chooser = new JFileChooser(projectRoot);
        chooser.setMultiSelectionEnabled(true);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        sources.clear();
        for (File file : chooser.getSelectedFiles()) sources.add(file);
        StringBuilder names = new StringBuilder();
        for (File source : sources) names.append("• ").append(source.getAbsolutePath()).append('\n');
        selectedFiles.setText(names.toString());
        if (!sources.isEmpty()) showComparison(sources.get(0));
        report.setText("Fila carregada: " + sources.size() + " PNG(s). Revise a comparação e exporte quando estiver satisfeito.");
    }

    private void showComparison(File source) {
        try {
            showImage(before, ImageIO.read(source), "Antes — " + source.getName());
            showImage(after, AssetCoach.normalizedPreview(source), "Depois — canvas 32×32 (ainda não salvo)");
        } catch (Exception failure) {
            after.setIcon(null); after.setText("Prévia indisponível");
            report.setText("Não foi possível comparar " + source.getName() + ": " + failure.getMessage());
        }
    }

    private void processBatch() {
        if (sources.isEmpty()) {
            report.setText("Selecione ao menos um PNG antes de iniciar o lote.");
            return;
        }
        ContentStudioProject.RpgSpriteKind selectedKind = (ContentStudioProject.RpgSpriteKind) kind.getSelectedItem();
        AssetCoach.BatchReport result = AssetCoach.normalizeBatch(sources.toArray(new File[0]), selectedKind,
                ContentStudioProject.RpgSpriteProperties.defaults(selectedKind), projectRoot);
        for (AssetCoach.BatchItem item : result.items) if (item.isSuccess()) {
            File output = item.output.getAbsoluteFile(); workingCopies.add(output); lastWorkingCopy.set(output); latestExport.set(output);
        }
        report.setText(result.toReport());
        if (lastWorkingCopy.get() != null) {
            try { showImage(after, ImageIO.read(lastWorkingCopy.get()), "Depois — cópia de trabalho exportada"); }
            catch (Exception ignored) { after.setText("Cópia exportada; prévia indisponível"); }
        }
        activity.append("\nAsset Coach processou lote: " + result.successCount() + " exportado(s), "
                + result.failureCount() + " falha(s) isolada(s).");
    }

    private void undoLastCopy() {
        File output = lastWorkingCopy.get();
        if (output == null || !workingCopies.contains(output)) {
            report.setText("Não há cópia de trabalho desta sessão para desfazer.");
            return;
        }
        try {
            File outputDir = new File(projectRoot, "res/assets/generated/rpg_sprites").getCanonicalFile();
            File safeOutput = output.getCanonicalFile();
            if (!outputDir.equals(safeOutput.getParentFile())) throw new IllegalStateException("Destino fora da área segura do Asset Coach.");
            String fileName = safeOutput.getName();
            File manifest = new File(outputDir, fileName.replaceFirst("\\.png$", ".json"));
            boolean pngDeleted = !safeOutput.exists() || Files.deleteIfExists(safeOutput.toPath());
            boolean manifestDeleted = !manifest.exists() || Files.deleteIfExists(manifest.toPath());
            if (!pngDeleted || !manifestDeleted) throw new IllegalStateException("Não foi possível remover a cópia de trabalho.");
            workingCopies.remove(output); lastWorkingCopy.set(null);
            after.setIcon(null); after.setText("Última cópia descartada; a fonte foi preservada");
            report.setText("Desfeito com segurança: " + fileName + " e seu manifesto foram removidos. O PNG original permanece intacto.");
            activity.append("\nAsset Coach desfez cópia de trabalho: " + fileName);
        } catch (Exception failure) {
            report.setText("Falha ao desfazer: " + failure.getMessage());
        }
    }

    private static void showImage(JLabel label, BufferedImage image, String description) {
        if (image == null) throw new IllegalArgumentException("PNG ilegível");
        label.setIcon(new ImageIcon(image.getScaledInstance(240, 240, java.awt.Image.SCALE_REPLICATE)));
        label.setText(""); label.setToolTipText(description);
    }
}
