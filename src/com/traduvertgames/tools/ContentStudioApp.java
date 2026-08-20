package com.traduvertgames.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Aplicativo desktop local para geração de conteúdo sem editar código. */
public final class ContentStudioApp {
    private final File projectRoot;
    private final AtomicReference<File> latestExport = new AtomicReference<File>();
    private final JTextArea activity = new JTextArea();
    private final JLabel preview = new JLabel("Nenhuma exportação selecionada", SwingConstants.CENTER);

    private ContentStudioApp(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Content Studio requer uma sessão gráfica. Execute este comando no Windows ou Linux desktop.");
            return;
        }
        SwingUtilities.invokeLater(() -> new ContentStudioApp(new File(System.getProperty("user.dir"))).show());
    }

    private void show() {
        JFrame frame = new JFrame("First Game — Content Studio");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(960, 660));
        frame.setLayout(new BorderLayout(12, 12));
        frame.getContentPane().setBackground(new Color(20, 25, 34));

        JLabel title = new JLabel("CONTENT STUDIO  •  MAPAS, TILES E ASSETS");
        title.setFont(new Font("Dialog", Font.BOLD, 19));
        title.setForeground(new Color(245, 218, 146));
        title.setBorder(BorderFactory.createEmptyBorder(14, 18, 4, 18));
        frame.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Mapas", createMapPanel());
        tabs.addTab("Tiles", createTilePanel());
        tabs.addTab("Inimigos", createEnemyPanel());
        tabs.addTab("Manifesto", createManifestPanel());
        frame.add(tabs, BorderLayout.CENTER);

        activity.setEditable(false);
        activity.setFont(new Font("Monospaced", Font.PLAIN, 12));
        activity.setBackground(new Color(13, 17, 24));
        activity.setForeground(new Color(194, 219, 196));
        activity.setText("Pronto. Exporte conteúdo compatível com o jogo.\nProjeto: " + projectRoot.getAbsolutePath());
        JScrollPane activityScroll = new JScrollPane(activity);
        activityScroll.setPreferredSize(new Dimension(900, 116));
        activityScroll.setBorder(BorderFactory.createTitledBorder("Atividade"));
        frame.add(activityScroll, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createMapPanel() {
        JPanel panel = formPanel();
        JRadioButton regional = new JRadioButton("Aventura RPG regional (192×128)", true);
        JRadioButton openWorld = new JRadioButton("Mundo Aberto (mínimo 512×320)");
        ButtonGroup group = new ButtonGroup();
        group.add(regional); group.add(openWorld);
        regional.setOpaque(false); openWorld.setOpaque(false);
        JSpinner width = new JSpinner(new SpinnerNumberModel(192, 96, 1024, 32));
        JSpinner height = new JSpinner(new SpinnerNumberModel(128, 64, 640, 32));
        JSpinner depth = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        JTextField seed = new JTextField("159991", 16);
        JButton generate = new JButton("Gerar mapa e manifesto");
        regional.addActionListener(event -> { width.setValue(192); height.setValue(128); });
        openWorld.addActionListener(event -> { width.setValue(512); height.setValue(320); });
        generate.addActionListener(event -> runExport("Mapa", () -> {
            long parsedSeed = Long.parseLong(seed.getText().trim());
            ContentStudioProject.MapKind kind = openWorld.isSelected()
                    ? ContentStudioProject.MapKind.OPEN_WORLD : ContentStudioProject.MapKind.REGIONAL;
            return ContentStudioProject.generateMap(kind, (Integer) width.getValue(), (Integer) height.getValue(),
                    (Integer) depth.getValue(), parsedSeed, projectRoot);
        }));
        addField(panel, 0, "Tipo", regional); addField(panel, 1, "", openWorld);
        addField(panel, 2, "Largura (tiles)", width); addField(panel, 3, "Altura (tiles)", height);
        addField(panel, 4, "Profundidade", depth); addField(panel, 5, "Seed reproduzível", seed);
        addField(panel, 6, "", generate);
        addPreview(panel, 7);
        return panel;
    }

    private JPanel createTilePanel() {
        JPanel panel = formPanel();
        JTextField name = new JTextField("piso_brumafolha", 16);
        JComboBox<ContentStudioProject.TileStyle> style = new JComboBox<ContentStudioProject.TileStyle>(ContentStudioProject.TileStyle.values());
        JSpinner variation = new JSpinner(new SpinnerNumberModel(0, 0, 7, 1));
        JCheckBox walkable = new JCheckBox("Caminhável", true);
        walkable.setOpaque(false);
        JTextField movementCost = new JTextField("1", 4);
        JTextField terrainTag = new JTextField("ground", 12);
        JButton generate = new JButton("Exportar tile 32×32");
        generate.addActionListener(event -> runExport("Tile", () -> {
            int cost = Integer.parseInt(movementCost.getText().trim());
            ContentStudioProject.TileProperties properties = new ContentStudioProject.TileProperties(
                    walkable.isSelected(), cost, terrainTag.getText());
            return ContentStudioProject.generateTile((ContentStudioProject.TileStyle) style.getSelectedItem(), name.getText(),
                    (Integer) variation.getValue(), properties, projectRoot);
        }));
        addField(panel, 0, "Nome do arquivo", name); addField(panel, 1, "Estilo", style);
        addField(panel, 2, "Variação", variation); addField(panel, 3, "Colisão", walkable);
        addField(panel, 4, "Custo de movimento", movementCost); addField(panel, 5, "Tag de terreno", terrainTag);
        addField(panel, 6, "", generate); addPreview(panel, 7);
        return panel;
    }

    private JPanel createEnemyPanel() {
        JPanel panel = formPanel();
        JComboBox<ContentStudioProject.EnemyRole> role = new JComboBox<ContentStudioProject.EnemyRole>(ContentStudioProject.EnemyRole.values());
        JComboBox<String> palette = new JComboBox<String>(new String[] { "Padrão do papel", "Íon ciano", "Ameaça rubra", "Musgo ácido" });
        JSpinner baseLife = new JSpinner(new SpinnerNumberModel(5, 1, 999, 1));
        JSpinner baseDamage = new JSpinner(new SpinnerNumberModel(2, 0, 999, 1));
        JSpinner speed = new JSpinner(new SpinnerNumberModel(1.4, 0.1, 10.0, 0.1));
        JTextField behavior = new JTextField("chase", 12);
        JButton generate = new JButton("Exportar sprite transparente 32×32");
        generate.addActionListener(event -> runExport("Sprite", () -> {
            Color[] colors = paletteFor((String) palette.getSelectedItem());
            ContentStudioProject.EnemyProperties properties = new ContentStudioProject.EnemyProperties(
                    (Integer) baseLife.getValue(), (Integer) baseDamage.getValue(),
                    ((Number) speed.getValue()).doubleValue(), behavior.getText());
            return ContentStudioProject.generateEnemySprite((ContentStudioProject.EnemyRole) role.getSelectedItem(),
                    colors[0], colors[1], properties, projectRoot);
        }));
        addField(panel, 0, "Papel de combate", role); addField(panel, 1, "Paleta", palette);
        addField(panel, 2, "Vida base", baseLife); addField(panel, 3, "Dano base", baseDamage);
        addField(panel, 4, "Velocidade", speed); addField(panel, 5, "Comportamento", behavior);
        addField(panel, 6, "", generate); addPreview(panel, 7);
        return panel;
    }

    private JPanel createManifestPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JTextArea manifest = new JTextArea();
        manifest.setEditable(false);
        manifest.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JButton open = new JButton("Abrir manifesto da última exportação");
        open.addActionListener(event -> {
            try {
                manifest.setText(ContentStudioProject.readManifestFor(latestExport.get()));
            } catch (Exception failure) {
                manifest.setText("Erro: " + failure.getMessage());
            }
        });
        panel.add(open, BorderLayout.NORTH);
        panel.add(new JScrollPane(manifest), BorderLayout.CENTER);
        return panel;
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.setBackground(new Color(31, 39, 51));
        return panel;
    }

    private void addField(JPanel panel, int row, String label, java.awt.Component component) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = row; left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(5, 5, 5, 14);
        if (!label.isEmpty()) {
            JLabel fieldLabel = new JLabel(label);
            fieldLabel.setForeground(new Color(225, 232, 237));
            panel.add(fieldLabel, left);
        }
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = row; right.weightx = 1; right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(5, 5, 5, 5);
        panel.add(component, right);
    }

    private void addPreview(JPanel panel, int row) {
        preview.setOpaque(true);
        preview.setBackground(new Color(17, 22, 30));
        preview.setForeground(new Color(180, 194, 201));
        preview.setPreferredSize(new Dimension(320, 240));
        preview.setBorder(BorderFactory.createTitledBorder("Prévia da exportação"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 2; constraints.gridy = 0; constraints.gridheight = row + 1;
        constraints.weightx = 1; constraints.weighty = 1; constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(5, 28, 5, 5);
        panel.add(preview, constraints);
    }

    private void runExport(String kind, ExportAction action) {
        try {
            File output = action.export();
            latestExport.set(output);
            preview.setIcon(new ImageIcon(output.getAbsolutePath()));
            preview.setText("");
            activity.append("\n" + kind + " exportado: " + output.getAbsolutePath());
        } catch (Exception failure) {
            activity.append("\nERRO: " + failure.getMessage());
            JOptionPane.showMessageDialog(null, failure.getMessage(), "Falha na exportação", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Color[] paletteFor(String selection) {
        if ("Íon ciano".equals(selection)) return new Color[] { new Color(38, 94, 111), new Color(90, 226, 238) };
        if ("Ameaça rubra".equals(selection)) return new Color[] { new Color(104, 46, 54), new Color(245, 90, 86) };
        if ("Musgo ácido".equals(selection)) return new Color[] { new Color(64, 96, 63), new Color(183, 232, 93) };
        return new Color[] { null, null };
    }

    @FunctionalInterface
    private interface ExportAction { File export() throws Exception; }
}
