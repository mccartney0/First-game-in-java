package com.traduvertgames.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** Painel visual de prontidão para as 24 poses direcionais de cada entidade do RPG. */
final class AnimationCoveragePanel extends JPanel {
    private static final String[] STATES = { "walk", "attack" };
    private static final String[] DIRECTIONS = { "right", "left", "up", "down" };
    private final File projectRoot;
    private final JPanel cards = new JPanel(new GridLayout(0, 2, 10, 10));
    private final JTextArea report = new JTextArea();

    AnimationCoveragePanel(File projectRoot) {
        super(new BorderLayout(10, 10));
        this.projectRoot = projectRoot;
        setBackground(new Color(31, 39, 51));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        JLabel title = new JLabel("COBERTURA DE ANIMAÇÕES — 4 DIREÇÕES × 3 FRAMES × 2 AÇÕES");
        title.setFont(new Font("Dialog", Font.BOLD, 15)); title.setForeground(new Color(245, 218, 146));
        JButton refresh = new JButton("Atualizar relatório");
        JPanel header = new JPanel(new BorderLayout(8, 4)); header.setOpaque(false);
        header.add(title, BorderLayout.WEST); header.add(refresh, BorderLayout.EAST); add(header, BorderLayout.NORTH);
        cards.setOpaque(false); add(new JScrollPane(cards), BorderLayout.CENTER);
        report.setEditable(false); report.setFont(new Font("Monospaced", Font.PLAIN, 12));
        report.setBackground(new Color(13, 17, 24)); report.setForeground(new Color(194, 219, 196));
        JScrollPane reportScroll = new JScrollPane(report); reportScroll.setPreferredSize(new Dimension(780, 118));
        reportScroll.setBorder(BorderFactory.createTitledBorder("Resumo de cobertura")); add(reportScroll, BorderLayout.SOUTH);
        refresh.addActionListener(event -> refresh()); refresh();
    }

    private void refresh() {
        AssetCoach.AnimationCoverageReport coverage = AssetCoach.inspectAnimationCoverage(projectRoot);
        cards.removeAll();
        for (AssetCoach.AnimationCoverage entity : coverage.entities) cards.add(card(entity));
        report.setText(coverage.toReport()); cards.revalidate(); cards.repaint();
    }

    private static JPanel card(AssetCoach.AnimationCoverage entity) {
        JPanel card = new JPanel(new BorderLayout(6, 6)); card.setBackground(new Color(20, 26, 36));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(108, 88, 42)),
                BorderFactory.createEmptyBorder(9, 9, 9, 9)));
        JLabel title = new JLabel(entity.id.toUpperCase() + "  •  " + entity.frameCount() + "/24");
        title.setForeground(new Color(245, 218, 146)); title.setFont(new Font("Dialog", Font.BOLD, 12)); card.add(title, BorderLayout.NORTH);
        JPanel cells = new JPanel(new GridLayout(2, 12, 2, 2)); cells.setOpaque(false);
        for (String state : STATES) for (String direction : DIRECTIONS) for (int frame = 0; frame < 3; frame++) {
            boolean present = entity.hasFrame(state, direction, frame);
            JLabel cell = new JLabel("", SwingConstants.CENTER); cell.setOpaque(true);
            cell.setBackground(present ? ("attack".equals(state) ? new Color(177, 82, 70) : new Color(80, 157, 111))
                    : new Color(69, 76, 88));
            cell.setToolTipText(state + " • " + direction + " • frame " + frame + (present ? " — presente" : " — ausente"));
            cells.add(cell);
        }
        card.add(cells, BorderLayout.CENTER);
        JLabel status = new JLabel(entity.isComplete() ? "✓ Pronto para runtime" : "• Gere frames ausentes", SwingConstants.LEFT);
        status.setForeground(entity.isComplete() ? new Color(132, 212, 151) : new Color(222, 181, 101)); card.add(status, BorderLayout.SOUTH);
        return card;
    }
}
