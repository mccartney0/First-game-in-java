package com.traduvertgames.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
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
        tabs.addTab("Itens RPG", createRpgItemsPanel());
        tabs.addTab("Referências", createTerrainGalleryPanel());
        tabs.addTab("Manifesto", createManifestPanel());
        tabs.addTab("Validação", createValidationPanel());
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
        JComboBox<ContentStudioProject.EnemyBehavior> behavior =
                new JComboBox<ContentStudioProject.EnemyBehavior>(ContentStudioProject.EnemyBehavior.values());
        role.addActionListener(event -> {
            ContentStudioProject.EnemyRole selected = (ContentStudioProject.EnemyRole) role.getSelectedItem();
            ContentStudioProject.EnemyProperties defaults = ContentStudioProject.EnemyProperties.defaults(selected);
            behavior.setSelectedItem(ContentStudioProject.behaviorForRole(selected));
            baseLife.setValue(defaults.baseLife);
            baseDamage.setValue(defaults.baseDamage);
            speed.setValue(defaults.speed);
        });
        behavior.setSelectedItem(ContentStudioProject.behaviorForRole((ContentStudioProject.EnemyRole) role.getSelectedItem()));
        JButton generate = new JButton("Exportar sprite transparente 32×32");
        generate.addActionListener(event -> runExport("Sprite", () -> {
            Color[] colors = paletteFor((String) palette.getSelectedItem());
            ContentStudioProject.EnemyProperties properties = new ContentStudioProject.EnemyProperties(
                    (Integer) baseLife.getValue(), (Integer) baseDamage.getValue(),
                    ((Number) speed.getValue()).doubleValue(),
                    ((ContentStudioProject.EnemyBehavior) behavior.getSelectedItem()).getTag(),
                    role.getSelectedItem() == ContentStudioProject.EnemyRole.MIST_SOVEREIGN);
            return ContentStudioProject.generateEnemySprite((ContentStudioProject.EnemyRole) role.getSelectedItem(),
                    colors[0], colors[1], properties, projectRoot);
        }));
        addField(panel, 0, "Papel de combate", role); addField(panel, 1, "Paleta", palette);
        addField(panel, 2, "Vida base", baseLife); addField(panel, 3, "Dano base", baseDamage);
        JButton outlandPack = new JButton("Gerar pacote da Charneca");
        outlandPack.addActionListener(event -> runExport("Pacote Charneca", () -> {
            File[] exports = ContentStudioProject.generateOutlandEnemyPack(projectRoot);
            return exports[exports.length - 1];
        }));
        JButton bossDemo = new JButton("Gerar demo: Soberano da Bruma");
        bossDemo.addActionListener(event -> runExport("Chefe da Charneca", () ->
                ContentStudioProject.generateMistSovereignBoss(projectRoot)));
        JButton bossAbility = new JButton("Exportar habilidade: Núcleo da Bruma");
        bossAbility.addActionListener(event -> runExport("Habilidade do Soberano", () ->
                ContentStudioProject.generateMistSovereignAbility(projectRoot)));
        addField(panel, 4, "Velocidade", speed); addField(panel, 5, "Perfil de IA", behavior);
        addField(panel, 6, "", generate); addField(panel, 7, "", bossDemo);
        addField(panel, 8, "", bossAbility); addField(panel, 9, "", outlandPack); addPreview(panel, 10);
        return panel;
    }

    private JPanel createRpgItemsPanel() {
        JTabbedPane itemTabs = new JTabbedPane();
        itemTabs.addTab("Consumível", createConsumablePanel());
        itemTabs.addTab("Arma", createRpgWeaponPanel());
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        root.add(itemTabs, BorderLayout.CENTER);
        JButton defaults = new JButton("Gerar pacote RPG inicial");
        defaults.addActionListener(event -> runExport("Pacote RPG", () -> {
            File[] exports = ContentStudioProject.generateDefaultRpgContentPack(projectRoot);
            return exports[exports.length - 1];
        }));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.add(defaults);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JPanel createConsumablePanel() {
        JPanel panel = formPanel();
        JTextField id = new JTextField("elixir_de_bruma", 16);
        JTextField displayName = new JTextField("Elixir de Bruma", 16);
        JComboBox<ContentStudioProject.ConsumableEffect> effect =
                new JComboBox<ContentStudioProject.ConsumableEffect>(ContentStudioProject.ConsumableEffect.values());
        JSpinner life = new JSpinner(new SpinnerNumberModel(24, 0, 999, 1));
        JSpinner mana = new JSpinner(new SpinnerNumberModel(18, 0, 999, 1));
        JSpinner stamina = new JSpinner(new SpinnerNumberModel(20, 0, 999, 1));
        JButton generate = new JButton("Exportar consumível 32×32");
        generate.addActionListener(event -> runExport("Consumível", () -> {
            ContentStudioProject.ConsumableProperties properties = new ContentStudioProject.ConsumableProperties(
                    displayName.getText(), (ContentStudioProject.ConsumableEffect) effect.getSelectedItem(),
                    (Integer) life.getValue(), (Integer) mana.getValue(), (Integer) stamina.getValue());
            return ContentStudioProject.generateConsumable(id.getText(), properties, projectRoot);
        }));
        addField(panel, 0, "ID do arquivo", id); addField(panel, 1, "Nome visível", displayName);
        addField(panel, 2, "Efeito", effect); addField(panel, 3, "Restaura vida", life);
        addField(panel, 4, "Restaura mana", mana); addField(panel, 5, "Restaura fôlego", stamina);
        addField(panel, 6, "", generate);
        return panel;
    }

    private JPanel createRpgWeaponPanel() {
        JPanel panel = formPanel();
        JTextField id = new JTextField("lamina_de_bruma", 16);
        JTextField displayName = new JTextField("Lâmina de Bruma", 16);
        JComboBox<ContentStudioProject.RpgWeaponStyle> style =
                new JComboBox<ContentStudioProject.RpgWeaponStyle>(ContentStudioProject.RpgWeaponStyle.values());
        JSpinner damage = new JSpinner(new SpinnerNumberModel(2, 0, 99, 1));
        JSpinner stamina = new JSpinner(new SpinnerNumberModel(9, 0, 99, 1));
        JTextField rarity = new JTextField("uncommon", 12);
        JButton generate = new JButton("Exportar arma RPG 32×32");
        generate.addActionListener(event -> runExport("Arma RPG", () -> {
            ContentStudioProject.RpgWeaponProperties properties = new ContentStudioProject.RpgWeaponProperties(
                    displayName.getText(), (Integer) damage.getValue(), (Integer) stamina.getValue(), rarity.getText());
            return ContentStudioProject.generateRpgWeapon(id.getText(),
                    (ContentStudioProject.RpgWeaponStyle) style.getSelectedItem(), properties, projectRoot);
        }));
        addField(panel, 0, "ID do arquivo", id); addField(panel, 1, "Nome visível", displayName);
        addField(panel, 2, "Estilo", style); addField(panel, 3, "Bônus de dano", damage);
        addField(panel, 4, "Custo de fôlego", stamina); addField(panel, 5, "Raridade", rarity);
        addField(panel, 6, "", generate);
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

    private JPanel createValidationPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(new Color(31, 39, 51));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        header.setOpaque(false);
        JLabel explanation = new JLabel("Valida os sete contratos antes de enviar conteúdo para o runtime.");
        explanation.setForeground(new Color(225, 232, 237));
        JButton validate = new JButton("Validar conteúdo");
        header.add(explanation);
        header.add(validate);
        root.add(header, BorderLayout.NORTH);

        JTextArea report = new JTextArea();
        report.setEditable(false);
        report.setFont(new Font("Monospaced", Font.PLAIN, 12));
        report.setBackground(new Color(13, 17, 24));
        report.setForeground(new Color(194, 219, 196));
        report.setText("Clique em ‘Validar conteúdo’ para verificar arquivos, transparência, escala, atlas, metadados, referências e runtime.");
        report.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(new JScrollPane(report), BorderLayout.CENTER);

        JLabel note = new JLabel("O relatório JSON é salvo em res/assets/generated/content_validation_report.json.");
        note.setForeground(new Color(194, 219, 196));
        root.add(note, BorderLayout.SOUTH);
        validate.addActionListener(event -> validateContent(report));
        return root;
    }

    private void validateContent(JTextArea reportArea) {
        try {
            ContentValidator.Report report = ContentValidator.validate(projectRoot);
            reportArea.setText(report.toText());
            File output = new File(projectRoot, "res/assets/generated/content_validation_report.json");
            Files.writeString(output.toPath(), report.toJson(), StandardCharsets.UTF_8);
            activity.append("\\nValidação: " + report.errorCount() + " erro(s), "
                    + report.warningCount() + " aviso(s). Relatório salvo em " + output.getPath());
            if (report.isValid()) {
                JOptionPane.showMessageDialog(null, "Conteúdo válido. Nenhum erro encontrado.",
                        "Validação concluída", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, report.errorCount() + " erro(s) de conteúdo encontrados.",
                        "Validação encontrou problemas", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception failure) {
            reportArea.setText("Falha ao validar conteúdo: " + failure.getMessage());
            activity.append("\\nERRO na validação: " + failure.getMessage());
            JOptionPane.showMessageDialog(null, failure.getMessage(), "Falha na validação", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createTerrainGalleryPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(new Color(31, 39, 51));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        header.setOpaque(false);
        JLabel explanation = new JLabel("Referências visuais de Brumafolha — a exportação instala os tiles 32×32 no jogo.");
        explanation.setForeground(new Color(225, 232, 237));
        JButton exportPack = new JButton("Gerar pacote runtime 32×32");
        exportPack.addActionListener(event -> exportBrumafolhaTerrainPack());
        JButton importAssets = new JButton("Importar assets do projeto");
        importAssets.addActionListener(event -> importUserAssets());
        header.add(explanation);
        header.add(exportPack);
        header.add(importAssets);
        root.add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 12));
        cards.setBackground(new Color(31, 39, 51));
        cards.add(createTerrainReferenceCard("Grama — 4 variações", "brumafolha_grass_reference.png"));
        cards.add(createTerrainReferenceCard("Estrada — 3 variações", "brumafolha_road_reference.png"));
        cards.add(createTerrainReferenceCard("Ruínas — 3 variações", "brumafolha_ruins_reference.png"));
        root.add(cards, BorderLayout.CENTER);

        JLabel note = new JLabel("As referências orientam a arte; o Vale carrega PNGs em res/assets/generated/tiles/.");
        note.setForeground(new Color(194, 219, 196));
        root.add(note, BorderLayout.SOUTH);
        return root;
    }

    private void importUserAssets() {
        String[] pythonCommands = { "python3", "python" };
        Process process = null;
        StringBuilder commandErrors = new StringBuilder();
        try {
            for (String python : pythonCommands) {
                try {
                    process = new ProcessBuilder(python, "tools/import_user_assets.py")
                            .directory(projectRoot)
                            .redirectErrorStream(true)
                            .start();
                    break;
                } catch (IOException unavailable) {
                    commandErrors.append(python).append(": ").append(unavailable.getMessage()).append("\\n");
                }
            }
            if (process == null) {
                throw new IOException("Python não encontrado.\\n" + commandErrors);
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException(output.isEmpty() ? "O importador terminou com erro." : output);
            }
            activity.append("\\nAssets importados automaticamente: " + output.trim());
            JOptionPane.showMessageDialog(null,
                    "Assets importados. Veja o manifesto em res/assets/generated/user_asset_manifest.json.",
                    "Importação concluída", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception failure) {
            activity.append("\\nERRO na importação de assets: " + failure.getMessage());
            JOptionPane.showMessageDialog(null, failure.getMessage(), "Falha na importação", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createTerrainReferenceCard(String title, String fileName) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(new Color(17, 22, 30));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(82, 100, 105)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setForeground(new Color(245, 218, 146));
        label.setFont(new Font("Dialog", Font.BOLD, 14));
        card.add(label, BorderLayout.NORTH);
        JLabel image = new JLabel("Referência ainda não encontrada", SwingConstants.CENTER);
        image.setForeground(new Color(180, 194, 201));
        image.setOpaque(true);
        image.setBackground(new Color(10, 14, 20));
        File source = new File(projectRoot, "res/assets/generated/terrain_sources/" + fileName);
        try {
            BufferedImage loaded = ImageIO.read(source);
            if (loaded != null) {
                image.setIcon(new ImageIcon(loaded.getScaledInstance(248, 186, java.awt.Image.SCALE_SMOOTH)));
                image.setText("");
            }
        } catch (Exception ignored) {
            image.setText("Não foi possível carregar " + fileName);
        }
        card.add(image, BorderLayout.CENTER);
        return card;
    }

    private void exportBrumafolhaTerrainPack() {
        try {
            File[] outputs = ContentStudioProject.generateBrumafolhaTerrainPack(projectRoot);
            latestExport.set(outputs[0]);
            preview.setIcon(new ImageIcon(outputs[0].getAbsolutePath()));
            preview.setText("");
            activity.append("\nPacote Brumafolha exportado: " + outputs.length
                    + " tiles em res/assets/generated/tiles");
        } catch (Exception failure) {
            activity.append("\nERRO no pacote de terreno: " + failure.getMessage());
            JOptionPane.showMessageDialog(null, failure.getMessage(), "Falha na exportação", JOptionPane.ERROR_MESSAGE);
        }
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
