package com.traduvertgames.tools;

// Design: painel de estúdio escuro e funcional; a prévia amplia pixel art sem suavizar e destaca frames ausentes.

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Reproduz os três frames contratados para cada ação/direção antes ou depois da
 * exportação. A pasta pode ser o pacote gerado ou uma pasta de trabalho externa.
 */
final class AnimatedPreviewPanel extends JPanel {
    private static final Color PANEL = new Color(26, 33, 45);
    private static final Color TEXT = new Color(229, 234, 241);
    private static final FrameEntity[] ENTITIES = {
            new FrameEntity("hero", "Herói"),
            new FrameEntity("npc_commandant", "Ava — Comandante"),
            new FrameEntity("npc_healer", "Orin — Curador"),
            new FrameEntity("npc_cartographer", "Ilyra — Cartógrafa") };

    private final File generatedDirectory;
    private File previewDirectory;
    private final JTextField directory = new JTextField();
    private final JComboBox<FrameEntity> entity = new JComboBox<FrameEntity>(ENTITIES);
    private final JComboBox<String> action = new JComboBox<String>(new String[] { "walk", "attack" });
    private final JComboBox<String> direction = new JComboBox<String>(new String[] { "right", "left", "up", "down" });
    private final JSpinner fps = new JSpinner(new SpinnerNumberModel(8, 1, 24, 1));
    private final PreviewCanvas canvas = new PreviewCanvas();
    private final JLabel status = new JLabel("Carregando prévia…");
    private final List<BufferedImage> frames = new ArrayList<BufferedImage>();
    private final Timer timer;
    private int frameIndex;

    AnimatedPreviewPanel(File projectRoot) {
        generatedDirectory = new File(projectRoot, "res/assets/generated/rpg_sprites");
        previewDirectory = generatedDirectory;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(new Color(20, 25, 34));

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.setOpaque(false);
        JLabel title = new JLabel("PRÉVIA ANIMADA — CONTRATO DE 3 FRAMES");
        title.setForeground(new Color(245, 218, 146));
        title.setHorizontalAlignment(SwingConstants.LEFT);
        header.add(title, BorderLayout.NORTH);
        JLabel explanation = new JLabel("Use a pasta de trabalho para revisar antes de exportar; use o pacote gerado para conferir o resultado final.");
        explanation.setForeground(TEXT);
        header.add(explanation, BorderLayout.CENTER);

        JPanel source = new JPanel(new BorderLayout(8, 0));
        source.setOpaque(false);
        directory.setEditable(false);
        directory.setBackground(PANEL);
        directory.setForeground(TEXT);
        directory.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(73, 91, 112)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JButton chooseFolder = new JButton("Pasta de frames…");
        JButton generated = new JButton("Usar pacote gerado");
        source.add(directory, BorderLayout.CENTER);
        JPanel sourceButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        sourceButtons.setOpaque(false);
        sourceButtons.add(chooseFolder); sourceButtons.add(generated);
        source.add(sourceButtons, BorderLayout.EAST);
        header.add(source, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        controlPanel.setOpaque(false);
        JPanel selectors = titledPanel("Seleção do ciclo");
        selectors.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        selectors.add(label("Entidade")); selectors.add(entity);
        selectors.add(label("Ação")); selectors.add(action);
        selectors.add(label("Direção")); selectors.add(direction);
        selectors.add(label("FPS")); selectors.add(fps);
        controlPanel.add(selectors);

        JPanel playback = titledPanel("Reprodução sem exportar");
        playback.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton previous = new JButton("◀ Frame");
        JButton play = new JButton("Reproduzir ▶");
        JButton pause = new JButton("Pausar ⏸");
        JButton next = new JButton("Frame ▶");
        playback.add(previous); playback.add(play); playback.add(pause); playback.add(next);
        controlPanel.add(playback);
        add(controlPanel, BorderLayout.SOUTH);

        canvas.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(88, 106, 126)),
                "Frame selecionado — ampliado 8× sem suavização"));
        add(canvas, BorderLayout.CENTER);

        timer = new Timer(delayForFps(), event -> nextFrame());
        chooseFolder.addActionListener(event -> choosePreviewDirectory());
        generated.addActionListener(event -> setPreviewDirectory(generatedDirectory));
        entity.addActionListener(event -> reloadFrames());
        action.addActionListener(event -> reloadFrames());
        direction.addActionListener(event -> reloadFrames());
        fps.addChangeListener(event -> timer.setDelay(delayForFps()));
        previous.addActionListener(event -> previousFrame());
        play.addActionListener(event -> startPlayback());
        pause.addActionListener(event -> pausePlayback());
        next.addActionListener(event -> nextFrame());
        status.setForeground(new Color(194, 219, 196));
        status.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        add(status, BorderLayout.WEST);
        setPreviewDirectory(generatedDirectory);
    }

    private JPanel titledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setOpaque(true); panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(73, 91, 112)), title));
        return panel;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text + ":");
        label.setForeground(TEXT);
        return label;
    }

    private void choosePreviewDirectory() {
        JFileChooser chooser = new JFileChooser(previewDirectory);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Escolha a pasta com os frames PNG");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) setPreviewDirectory(chooser.getSelectedFile());
    }

    private void setPreviewDirectory(File folder) {
        previewDirectory = folder == null ? generatedDirectory : folder;
        try { directory.setText(previewDirectory.getCanonicalPath()); }
        catch (IOException ignored) { directory.setText(previewDirectory.getAbsolutePath()); }
        reloadFrames();
    }

    private void reloadFrames() {
        pausePlayback();
        frames.clear();
        frameIndex = 0;
        FrameEntity selectedEntity = (FrameEntity) entity.getSelectedItem();
        String selectedAction = (String) action.getSelectedItem();
        String selectedDirection = (String) direction.getSelectedItem();
        int found = 0;
        for (int index = 0; index < 3; index++) {
            File frame = new File(previewDirectory, selectedEntity.id + "_" + selectedAction + "_" + selectedDirection + "_" + index + ".png");
            BufferedImage image = null;
            try { image = ImageIO.read(frame); }
            catch (IOException ignored) { /* frame ausente continua visível como pendência */ }
            if (image != null) found++;
            frames.add(image);
        }
        status.setText(found + "/3 frames disponíveis — " + selectedEntity.id + "_" + selectedAction + "_" + selectedDirection
                + " | " + (previewDirectory.equals(generatedDirectory) ? "pacote gerado" : "pasta de trabalho"));
        renderCurrentFrame();
    }

    private int delayForFps() {
        return 1000 / Math.max(1, ((Number) fps.getValue()).intValue());
    }

    private void startPlayback() {
        boolean hasFrame = false;
        for (BufferedImage frame : frames) if (frame != null) { hasFrame = true; break; }
        if (!hasFrame) {
            status.setText("Nenhum frame correspondente foi encontrado. Revise nome, pasta, ação e direção antes de exportar.");
            return;
        }
        timer.setDelay(delayForFps());
        timer.start();
        status.setText(status.getText() + " • reprodução em " + fps.getValue() + " FPS");
    }

    private void pausePlayback() { timer.stop(); }

    private void previousFrame() {
        pausePlayback();
        frameIndex = (frameIndex + 2) % 3;
        renderCurrentFrame();
    }

    private void nextFrame() {
        frameIndex = (frameIndex + 1) % 3;
        renderCurrentFrame();
    }

    private void renderCurrentFrame() {
        BufferedImage frame = frames.isEmpty() ? null : frames.get(frameIndex);
        canvas.setFrame(frame, frameIndex, previewDirectory);
    }

    @Override public void removeNotify() {
        pausePlayback();
        super.removeNotify();
    }

    private static final class FrameEntity {
        private final String id;
        private final String label;
        FrameEntity(String id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }

    private static final class PreviewCanvas extends JPanel {
        private BufferedImage frame;
        private int index;
        private File source;

        PreviewCanvas() {
            setPreferredSize(new Dimension(500, 390));
            setBackground(new Color(16, 21, 29));
        }

        void setFrame(BufferedImage frame, int index, File source) {
            this.frame = frame; this.index = index; this.source = source;
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            int size = Math.min(getWidth() - 44, getHeight() - 68);
            int x = (getWidth() - size) / 2;
            int y = 24;
            for (int row = 0; row < 12; row++) for (int column = 0; column < 12; column++) {
                g.setColor(((row + column) & 1) == 0 ? new Color(66, 76, 91) : new Color(44, 53, 67));
                g.fillRect(x + column * size / 12, y + row * size / 12, size / 12 + 1, size / 12 + 1);
            }
            if (frame != null) {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(frame, x, y, size, size, null);
                g.setColor(new Color(245, 218, 146));
                g.drawString("Frame " + index + " — " + frame.getWidth() + "×" + frame.getHeight(), x, y + size + 22);
            } else {
                g.setColor(new Color(245, 218, 146));
                g.drawString("Frame " + index + " ausente", x + 12, y + size / 2);
                g.setColor(new Color(194, 219, 196));
                g.drawString("A prévia não inventa fallback: corrija ou exporte este PNG.", x + 12, y + size / 2 + 22);
            }
            if (source != null) {
                g.setColor(new Color(194, 219, 196));
                g.drawString(source.getName(), x, getHeight() - 12);
            }
            g.dispose();
        }
    }
}
