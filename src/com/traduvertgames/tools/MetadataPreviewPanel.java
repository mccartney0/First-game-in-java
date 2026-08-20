package com.traduvertgames.tools;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;

import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.WeaponType;
import com.traduvertgames.graficos.AssetCatalog;

/**
 * Pré-visualização gráfica dos contratos de conteúdo usados pelo combate.
 * Mostra o sprite real, ponto de origem do tiro e os metadados que o jogo lê.
 */
public final class MetadataPreviewPanel extends JPanel {
    private static final Color BACKGROUND = new Color(31, 39, 51);
    private static final Color PANEL = new Color(17, 22, 30);
    private static final Color TEXT = new Color(225, 232, 237);
    private static final Color MUTED = new Color(180, 194, 201);
    private static final Color ACCENT = new Color(245, 218, 146);
    private static final Color SHOT_POINT = new Color(255, 103, 74);
    private static final Color CENTER_POINT = new Color(90, 210, 255);

    private final File projectRoot;
    private final JComboBox<String> category = new JComboBox<String>(new String[] { "Armas", "Inimigos" });
    private final JComboBox<String> item = new JComboBox<String>();
    private final JLabel status = new JLabel("", SwingConstants.LEFT);
    private final JTextArea details = new JTextArea();
    private final ArtCanvas canvas = new ArtCanvas();

    public MetadataPreviewPanel(File projectRoot) {
        this.projectRoot = projectRoot;
        setLayout(new BorderLayout(10, 10));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        JPanel controls = new JPanel(new GridLayout(1, 4, 8, 0));
        controls.setOpaque(false);
        controls.add(label("Categoria"));
        controls.add(category);
        controls.add(label("Conteúdo"));
        controls.add(item);
        add(controls, BorderLayout.NORTH);

        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(new Font("Monospaced", Font.PLAIN, 12));
        details.setBackground(PANEL);
        details.setForeground(TEXT);
        details.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setOpaque(false);
        right.add(new JScrollPane(details), BorderLayout.CENTER);
        status.setForeground(MUTED);
        right.add(status, BorderLayout.SOUTH);
        right.setPreferredSize(new Dimension(330, 420));

        canvas.setPreferredSize(new Dimension(600, 420));
        add(canvas, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        category.addActionListener(event -> reloadItems());
        item.addActionListener(event -> refresh());
        reloadItems();
    }

    private JLabel label(String value) {
        JLabel label = new JLabel(value);
        label.setForeground(TEXT);
        return label;
    }

    private void reloadItems() {
        item.removeAllItems();
        if (isWeaponCategory()) {
            for (WeaponType type : WeaponType.values()) item.addItem(type.name());
        } else {
            for (Enemy.Variant variant : Enemy.Variant.values()) item.addItem(variant.name());
        }
        if (item.getItemCount() > 0) item.setSelectedIndex(0);
        refresh();
    }

    private boolean isWeaponCategory() {
        return "Armas".equals(category.getSelectedItem());
    }

    private void refresh() {
        if (item.getSelectedItem() == null) return;
        BufferedImage image;
        double pointX;
        double pointY;
        boolean hasShotPoint;
        String text;
        String target = String.valueOf(item.getSelectedItem());

        if (isWeaponCategory()) {
            WeaponType type = WeaponType.valueOf(target);
            image = loadWeapon(type);
            pointX = type.getShotOriginX();
            pointY = type.getShotOriginY();
            hasShotPoint = true;
            text = weaponDetails(type, image);
        } else {
            Enemy.Variant variant = Enemy.Variant.valueOf(target);
            image = loadEnemy(variant);
            pointX = 0.50;
            pointY = 0.50;
            hasShotPoint = isShooter(variant);
            text = enemyDetails(variant, image);
        }

        canvas.setImage(image, pointX, pointY, hasShotPoint);
        details.setText(text);
        if (image == null) {
            status.setForeground(new Color(255, 145, 125));
            status.setText("Asset não encontrado — será apontado na validação.");
        } else if (hasShotPoint) {
            status.setForeground(new Color(150, 230, 172));
            status.setText("Ponto vermelho = disparo | cruz azul = centro do sprite");
        } else {
            status.setForeground(MUTED);
            status.setText("Esta variante não possui projétil de ataque configurado.");
        }
    }

    private String weaponDetails(WeaponType type, BufferedImage image) {
        StringBuilder text = new StringBuilder();
        text.append("ARMA\n");
        text.append("------------------------------\n");
        text.append("ID: ").append(type.name()).append('\n');
        text.append("Nome: ").append(type.getDisplayName()).append('\n');
        text.append("Dano: ").append(format(type.getDamage())).append('\n');
        text.append("Cadência: ").append(type.getFireDelayFrames()).append(" frames\n");
        text.append("Velocidade: ").append(format(type.getProjectileSpeed())).append('\n');
        text.append("Projéteis/disparo: ").append(type.getProjectilesPerShot()).append('\n');
        text.append("Dispersão: ").append(format(type.getSpreadDegrees())).append("°\n");
        text.append("Custo de mana: ").append(format(type.getManaCost())).append('\n');
        text.append("Sprite: ").append(image == null ? "AUSENTE" : image.getWidth() + "×" + image.getHeight()).append('\n');
        text.append("\nPONTO DE DISPARO\n");
        text.append("------------------------------\n");
        text.append("X: ").append(format(type.getShotOriginX())).append(" normalizado\n");
        text.append("Y: ").append(format(type.getShotOriginY())).append(" normalizado\n");
        text.append("Espaço: ").append(type.getShotOriginSpace()).append('\n');
        text.append("\nObservação: o combate atual ainda cria o tiro a partir do centro do jogador; este ponto prepara a integração por arma.");
        return text.toString();
    }

    private String enemyDetails(Enemy.Variant variant, BufferedImage image) {
        boolean shooter = isShooter(variant);
        StringBuilder text = new StringBuilder();
        text.append("INIMIGO\n");
        text.append("------------------------------\n");
        text.append("ID: ").append(variant.name()).append('\n');
        text.append("Vida: ").append(format(variant.getMaxLife())).append('\n');
        text.append("Velocidade: ").append(format(variant.getSpeedMultiplier())).append('\n');
        text.append("Dano do projétil: ").append(format(variant.getProjectileDamage())).append('\n');
        text.append("Velocidade do projétil: ").append(format(variant.getProjectileSpeed())).append('\n');
        text.append("Tamanho do projétil: ").append(variant.getProjectileSize()).append('\n');
        text.append("Cadência: ").append(variant.getAttackCooldown()).append(" frames\n");
        text.append("Alcance especial: ").append(format(variant.getSpecialRange())).append('\n');
        text.append("Sprite: ").append(image == null ? "AUSENTE" : image.getWidth() + "×" + image.getHeight()).append('\n');
        text.append("\nPONTO DE ATAQUE\n");
        text.append("------------------------------\n");
        text.append(shooter ? "X: 0.50 normalizado\nY: 0.50 normalizado\n" : "Sem ponto de disparo: variante corpo a corpo/suporte.\n");
        text.append("Espaço: centro do sprite; runtime usa a posição da entidade.");
        return text.toString();
    }

    private boolean isShooter(Enemy.Variant variant) {
        return variant.getProjectileSpeed() > 0 || variant.getProjectileDamage() > 0;
    }

    private BufferedImage loadWeapon(WeaponType type) {
        AssetCatalog.initialize();
        BufferedImage image = AssetCatalog.weaponIcon(type);
        if (image != null) return image;
        String slug = type.name().toLowerCase(Locale.ROOT);
        String[] candidates = {
                "res/assets/generated/weapons/" + slug + "_clean.png",
                "res/assets/generated/effects/" + slug + ".png"
        };
        return loadFirst(candidates);
    }

    private BufferedImage loadEnemy(Enemy.Variant variant) {
        AssetCatalog.initialize();
        BufferedImage image = AssetCatalog.enemySprite(variant);
        if (image != null) return image;
        return loadFirst(new String[] { "res/assets/generated/enemies/" + variant.name().toLowerCase(Locale.ROOT) + ".png" });
    }

    private BufferedImage loadFirst(String[] paths) {
        for (String path : paths) {
            try {
                BufferedImage image = ImageIO.read(new File(projectRoot, path));
                if (image != null) return image;
            } catch (Exception ignored) {
                // O status da interface informa a ausência sem interromper o Studio.
            }
        }
        return null;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class ArtCanvas extends JPanel {
        private BufferedImage image;
        private double pointX;
        private double pointY;
        private boolean hasShotPoint;

        private ArtCanvas() {
            setOpaque(true);
            setBackground(PANEL);
            setBorder(BorderFactory.createTitledBorder("Sprite e pontos de origem"));
        }

        private void setImage(BufferedImage image, double pointX, double pointY, boolean hasShotPoint) {
            this.image = image;
            this.pointX = pointX;
            this.pointY = pointY;
            this.hasShotPoint = hasShotPoint;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            drawCheckerboard(g);
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int drawWidth = 0;
            int drawHeight = 0;
            int drawX = centerX;
            int drawY = centerY;
            if (image != null) {
                double scale = Math.min((getWidth() * 0.76) / image.getWidth(), (getHeight() * 0.76) / image.getHeight());
                drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
                drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
                drawX = centerX - drawWidth / 2;
                drawY = centerY - drawHeight / 2;
                g.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
                drawCrosshair(g, centerX, centerY, CENTER_POINT, 7);
                if (hasShotPoint) {
                    int shotX = drawX + (int) Math.round(pointX * drawWidth);
                    int shotY = drawY + (int) Math.round(pointY * drawHeight);
                    drawCrosshair(g, shotX, shotY, SHOT_POINT, 10);
                    g.setColor(SHOT_POINT);
                    g.drawString("disparo", shotX + 12, shotY - 8);
                }
            } else {
                g.setColor(new Color(255, 145, 125));
                g.drawString("Sprite não encontrado", centerX - 62, centerY);
            }
            g.dispose();
        }

        private void drawCrosshair(Graphics2D g, int x, int y, Color color, int radius) {
            g.setColor(color);
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
            g.drawLine(x - radius - 5, y, x + radius + 5, y);
            g.drawLine(x, y - radius - 5, x, y + radius + 5);
        }

        private void drawCheckerboard(Graphics2D g) {
            int size = 16;
            Color light = new Color(42, 50, 61);
            Color dark = new Color(34, 41, 51);
            for (int y = 0; y < getHeight(); y += size) {
                for (int x = 0; x < getWidth(); x += size) {
                    g.setColor(((x / size + y / size) & 1) == 0 ? light : dark);
                    g.fillRect(x, y, size, size);
                }
            }
        }
    }
}

// Mantém a unidade de compilação independente de imports de eventos Swing em versões antigas.
@SuppressWarnings("unused")
final class MetadataPreviewPanelCompatibility {
    private MetadataPreviewPanelCompatibility() { }
}
