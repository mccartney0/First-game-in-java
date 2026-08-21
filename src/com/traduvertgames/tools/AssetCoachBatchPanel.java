package com.traduvertgames.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;

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
    private final Map<AssetCoach.ApprovedFixRule, JCheckBox> ruleChecks =
            new EnumMap<AssetCoach.ApprovedFixRule, JCheckBox>(AssetCoach.ApprovedFixRule.class);
    private final JComboBox<ContentStudioProject.RpgSpriteKind> kind =
            new JComboBox<ContentStudioProject.RpgSpriteKind>(ContentStudioProject.RpgSpriteKind.values());
    private final JComboBox<AssetCoach.EditorPreset> preset =
            new JComboBox<AssetCoach.EditorPreset>(AssetCoach.EditorPreset.values());
    private final JComboBox<Integer> previewFps = new JComboBox<Integer>(new Integer[] { 4, 6, 8, 12 });
    private final JLabel presetSummary = new JLabel();
    private final Timer previewTimer;
    private int previewFrameIndex;

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
        JLabel note = new JLabel("Arraste PNGs para a fila. A fonte nunca é alterada; regras atuam somente em cópias de trabalho.");
        note.setForeground(new Color(194, 219, 196));
        JPanel header = new JPanel(new BorderLayout(4, 4));
        header.setOpaque(false); header.add(title, BorderLayout.NORTH); header.add(note, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JButton choose = new JButton("Selecionar PNGs…");
        JButton process = new JButton("Normalizar lote e exportar");
        JButton undo = new JButton("Desfazer última cópia");
        JButton playPreview = new JButton("Prévia animada ▶");
        JButton pausePreview = new JButton("Pausar ⏸");
        JPanel commands = new JPanel();
        commands.setOpaque(false); commands.add(new JLabel("Preset:")); commands.add(preset);
        commands.add(new JLabel("Tipo runtime:")); commands.add(kind); commands.add(choose);
        commands.add(new JLabel("FPS:")); commands.add(previewFps); commands.add(playPreview); commands.add(pausePreview);
        commands.add(process); commands.add(undo);

        JPanel rules = new JPanel(); rules.setOpaque(false);
        rules.add(new JLabel("Regras pré-aprovadas:"));
        for (AssetCoach.ApprovedFixRule rule : AssetCoach.ApprovedFixRule.values()) {
            JCheckBox check = new JCheckBox(rule.label, true);
            check.setOpaque(false); check.setForeground(new Color(211, 222, 215));
            check.addActionListener(event -> refreshComparison());
            ruleChecks.put(rule, check); rules.add(check);
        }
        presetSummary.setForeground(new Color(245, 218, 146));
        rules.add(presetSummary);
        previewFps.setSelectedItem(Integer.valueOf(6));
        previewTimer = new Timer(previewDelay(), event -> advanceAnimatedPreview());
        preset.addActionListener(event -> applyPreset());
        previewFps.addActionListener(event -> previewTimer.setDelay(previewDelay()));
        applyPreset();

        selectedFiles.setEditable(false); selectedFiles.setLineWrap(true); selectedFiles.setWrapStyleWord(true);
        selectedFiles.setBackground(new Color(13, 17, 24)); selectedFiles.setForeground(new Color(205, 222, 211));
        JScrollPane selectedScroll = new JScrollPane(selectedFiles);
        selectedScroll.setBorder(BorderFactory.createTitledBorder("Fila de arquivos — solte PNGs aqui"));
        selectedScroll.setPreferredSize(new Dimension(780, 74));

        JPanel comparisons = new JPanel(new GridLayout(1, 2, 10, 10));
        comparisons.setOpaque(false); comparisons.add(before); comparisons.add(after);
        JPanel center = new JPanel(new BorderLayout(10, 10));
        JPanel controls = new JPanel(new BorderLayout(4, 4)); controls.setOpaque(false);
        controls.add(commands, BorderLayout.NORTH); controls.add(rules, BorderLayout.SOUTH);
        center.setOpaque(false); center.add(controls, BorderLayout.NORTH); center.add(comparisons, BorderLayout.CENTER);
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
        playPreview.addActionListener(event -> startAnimatedPreview());
        pausePreview.addActionListener(event -> pauseAnimatedPreview());
        new DropTarget(selectedFiles, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override public void drop(DropTargetDropEvent event) { receiveDrop(event); }
        }, true);
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
        replaceSources(chooser.getSelectedFiles(), "Seletor de arquivos");
    }

    private void receiveDrop(DropTargetDropEvent event) {
        try {
            if (!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.rejectDrop();
                return;
            }
            event.acceptDrop(DnDConstants.ACTION_COPY);
            Object data = event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            if (!(data instanceof List<?>)) throw new IllegalArgumentException("A seleção arrastada não contém arquivos.");
            List<File> dropped = new ArrayList<File>();
            for (Object entry : (List<?>) data) if (entry instanceof File) dropped.add((File) entry);
            appendSources(dropped.toArray(new File[0]), "Arrastar e soltar");
            event.dropComplete(true);
        } catch (Exception failure) {
            event.dropComplete(false);
            report.setText("Não foi possível receber os arquivos soltos: " + failure.getMessage());
        }
    }

    private void replaceSources(File[] files, String origin) {
        sources.clear();
        appendSources(files, origin);
    }

    private void appendSources(File[] files, String origin) {
        int ignored = 0;
        if (files != null) for (File file : files) {
            if (file == null || !file.isFile() || !file.getName().toLowerCase().endsWith(".png")) {
                ignored++;
            } else if (!sources.contains(file.getAbsoluteFile())) {
                sources.add(file.getAbsoluteFile());
            }
        }
        StringBuilder names = new StringBuilder();
        for (File source : sources) names.append("• ").append(source.getAbsolutePath()).append('\n');
        selectedFiles.setText(names.length() == 0 ? "Nenhum PNG válido na fila." : names.toString());
        previewFrameIndex = 0;
        pauseAnimatedPreview();
        refreshComparison();
        report.setText(origin + ": fila com " + sources.size() + " PNG(s)" + (ignored > 0 ? "; " + ignored + " item(ns) ignorado(s)" : "")
                + ". Regras ativas: " + AssetCoach.describeApprovedFixRules(selectedRules()) + ".");
    }

    private EnumSet<AssetCoach.ApprovedFixRule> selectedRules() {
        EnumSet<AssetCoach.ApprovedFixRule> selected = EnumSet.noneOf(AssetCoach.ApprovedFixRule.class);
        for (Map.Entry<AssetCoach.ApprovedFixRule, JCheckBox> entry : ruleChecks.entrySet()) {
            if (entry.getValue().isSelected()) selected.add(entry.getKey());
        }
        return selected;
    }

    private void applyPreset() {
        AssetCoach.EditorPreset selected = (AssetCoach.EditorPreset) preset.getSelectedItem();
        if (selected == null) return;
        EnumSet<AssetCoach.ApprovedFixRule> recommended = selected.approvedRules();
        for (Map.Entry<AssetCoach.ApprovedFixRule, JCheckBox> entry : ruleChecks.entrySet()) {
            entry.getValue().setSelected(recommended.contains(entry.getKey()));
        }
        presetSummary.setText(selected.label + " • canvas " + selected.targetCanvas + "×" + selected.targetCanvas
                + " • " + selected.recommendedFramesPerAction + " frames/ação • " + selected.guidance);
        refreshComparison();
    }

    private int previewDelay() {
        Integer fps = (Integer) previewFps.getSelectedItem();
        return 1000 / Math.max(1, fps == null ? 6 : fps.intValue());
    }

    private void startAnimatedPreview() {
        if (sources.isEmpty()) {
            report.setText("Adicione PNGs à fila para reproduzir uma prévia animada antes de exportar.");
            return;
        }
        previewTimer.setDelay(previewDelay());
        advanceAnimatedPreview();
        previewTimer.start();
        report.setText("Prévia animada em execução — " + sources.size() + " frame(s), "
                + previewFps.getSelectedItem() + " FPS. Nenhuma cópia foi exportada.");
    }

    private void pauseAnimatedPreview() {
        if (previewTimer != null) previewTimer.stop();
    }

    private void advanceAnimatedPreview() {
        if (sources.isEmpty()) { pauseAnimatedPreview(); return; }
        File frame = sources.get(previewFrameIndex % sources.size());
        try {
            showImage(after, AssetCoach.normalizedPreview(frame, selectedRules()), "Prévia animada — " + frame.getName());
            after.setBorder(BorderFactory.createTitledBorder("Prévia animada — frame "
                    + ((previewFrameIndex % sources.size()) + 1) + "/" + sources.size()));
        } catch (Exception failure) {
            after.setIcon(null); after.setText("Frame inválido na prévia");
            report.setText("Prévia interrompida em " + frame.getName() + ": " + failure.getMessage());
            pauseAnimatedPreview();
        }
        previewFrameIndex = (previewFrameIndex + 1) % sources.size();
    }

    private void refreshComparison() {
        if (!sources.isEmpty()) showComparison(sources.get(0));
    }

    private void showComparison(File source) {
        try {
            showImage(before, ImageIO.read(source), "Antes — " + source.getName());
            showImage(after, AssetCoach.normalizedPreview(source, selectedRules()), "Depois — regras aprovadas, ainda não salvo");
        } catch (Exception failure) {
            after.setIcon(null); after.setText("Prévia indisponível");
            report.setText("Não foi possível comparar " + source.getName() + ": " + failure.getMessage());
        }
    }

    private void processBatch() {
        pauseAnimatedPreview();
        if (sources.isEmpty()) {
            report.setText("Selecione ao menos um PNG antes de iniciar o lote.");
            return;
        }
        ContentStudioProject.RpgSpriteKind selectedKind = (ContentStudioProject.RpgSpriteKind) kind.getSelectedItem();
        AssetCoach.BatchReport result = AssetCoach.normalizeBatch(sources.toArray(new File[0]), selectedKind,
                ContentStudioProject.RpgSpriteProperties.defaults(selectedKind), selectedRules(), projectRoot);
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
