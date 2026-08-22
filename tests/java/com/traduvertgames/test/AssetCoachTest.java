package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.tools.AssetCoach;
import com.traduvertgames.tools.ContentStudioProject;

class AssetCoachTest {
    private final File root = new File("build/test-asset-coach");

    @AfterEach
    void clean() { delete(root); }

    @Test
    void batchKeepsFailuresIsolatedAndExportsRuntimeSafeCopies() throws Exception {
        File input = new File(root, "input/guardiao.png");
        input.getParentFile().mkdirs();
        BufferedImage image = new BufferedImage(64, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 64, 48);
        graphics.setColor(new Color(198, 73, 61)); graphics.fillRoundRect(19, 8, 24, 31, 6, 6); graphics.dispose();
        ImageIO.write(image, "png", input);
        AssetCoach.BatchReport report = AssetCoach.normalizeBatch(new File[] { input, new File(root, "input/invalido.png") },
                ContentStudioProject.RpgSpriteKind.HERO, ContentStudioProject.RpgSpriteProperties.defaults(
                        ContentStudioProject.RpgSpriteKind.HERO), root);
        assertEquals(1, report.successCount());
        assertEquals(1, report.failureCount());
        BufferedImage output = ImageIO.read(report.items.get(0).output);
        assertEquals(32, output.getWidth()); assertEquals(32, output.getHeight());
        assertEquals(0, output.getRGB(0, 0) >>> 24);
        assertTrue(input.isFile());
    }

    @Test
    void coverageReportsCompleteAndMissingDirectionalFrames() throws Exception {
        ContentStudioProject.RpgSpriteProperties props = ContentStudioProject.RpgSpriteProperties.defaults(
                ContentStudioProject.RpgSpriteKind.HERO);
        ContentStudioProject.generateRpgSprite("hero", ContentStudioProject.RpgSpriteKind.HERO, props, root);
        ContentStudioProject.generateRpgAnimationFrames("hero", ContentStudioProject.RpgSpriteKind.HERO, props, root);
        AssetCoach.AnimationCoverageReport report = AssetCoach.inspectAnimationCoverage(root);
        assertEquals(4, report.entities.size());
        assertTrue(report.entities.get(0).isComplete());
        assertEquals(24, report.entities.get(0).frameCount());
        assertEquals(24, report.totalFrames());
    }

    @Test
    void exportsCoverageAndAuditsApprovedRules() throws Exception {
        ContentStudioProject.RpgSpriteProperties props = ContentStudioProject.RpgSpriteProperties.defaults(
                ContentStudioProject.RpgSpriteKind.HERO);
        ContentStudioProject.generateRpgSprite("hero", ContentStudioProject.RpgSpriteKind.HERO, props, root);
        ContentStudioProject.generateRpgAnimationFrames("hero", ContentStudioProject.RpgSpriteKind.HERO, props, root);
        AssetCoach.AnimationCoverageReport coverage = AssetCoach.inspectAnimationCoverage(root);
        File csv = new File(root, "reports/cobertura.csv");
        File pdf = new File(root, "reports/cobertura.pdf");
        coverage.exportCsv(csv); coverage.exportPdf(pdf);
        assertTrue(new String(Files.readAllBytes(csv.toPath()), StandardCharsets.UTF_8).contains("entity,base_sprite,state"));
        assertTrue(new String(Files.readAllBytes(pdf.toPath()), StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4"));

        File input = new File(root, "input/regras.png"); input.getParentFile().mkdirs();
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics(); graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 40, 40);
        graphics.setColor(Color.RED); graphics.fillRect(12, 8, 16, 24); graphics.dispose(); ImageIO.write(image, "png", input);
        EnumSet<AssetCoach.ApprovedFixRule> rules = EnumSet.of(AssetCoach.ApprovedFixRule.REMOVE_CONNECTED_BORDER_BACKGROUND,
                AssetCoach.ApprovedFixRule.CENTER_ON_32PX_CANVAS);
        AssetCoach.BatchReport batch = AssetCoach.normalizeBatch(new File[] { input }, ContentStudioProject.RpgSpriteKind.HERO,
                props, rules, root);
        assertEquals(1, batch.successCount()); assertEquals(2, batch.items.get(0).appliedRules.size()); assertTrue(input.isFile());
    }

    @Test
    void editorPresetsKeepTheRpgImportContractExplicit() {
        AssetCoach.EditorPreset aseprite = AssetCoach.EditorPreset.ASEPRITE;
        AssetCoach.EditorPreset krita = AssetCoach.EditorPreset.KRITA;
        AssetCoach.EditorPreset piskel = AssetCoach.EditorPreset.PISKEL;
        assertEquals(32, aseprite.targetCanvas);
        assertEquals(3, aseprite.recommendedFramesPerAction);
        assertTrue(aseprite.approvedRules().contains(AssetCoach.ApprovedFixRule.NEAREST_NEIGHBOR_SCALING));
        assertTrue(krita.guidance.contains("PNG RGBA"));
        assertFalse(krita.approvedRules().contains(AssetCoach.ApprovedFixRule.TRIM_VISIBLE_SILHOUETTE));
        assertTrue(piskel.approvedRules().contains(AssetCoach.ApprovedFixRule.CENTER_ON_32PX_CANVAS));
    }

    @Test
    void spritesheetIsSplitInDeterministicRowMajorOrderWithoutChangingSource() throws Exception {
        File source = new File(root, "input/Hero Walk Down Sheet.png");
        source.getParentFile().mkdirs();
        BufferedImage sheet = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(Color.RED); graphics.fillRect(0, 0, 32, 32);
        graphics.setColor(Color.GREEN); graphics.fillRect(32, 0, 32, 32);
        graphics.setColor(Color.BLUE); graphics.fillRect(0, 32, 32, 32);
        graphics.setColor(Color.YELLOW); graphics.fillRect(32, 32, 32, 32);
        graphics.dispose();
        ImageIO.write(sheet, "png", source);

        AssetCoach.SpriteSheetLayout layout = AssetCoach.inspectSpritesheet(source, 32, 32);
        assertEquals(2, layout.columns); assertEquals(2, layout.rows); assertEquals(4, layout.frameCount());
        AssetCoach.SpriteSheetImport imported = AssetCoach.splitSpritesheet(source, "hero_walk_down", 32, 32, root);

        assertEquals(4, imported.frames.size());
        assertTrue(source.isFile());
        assertEquals(Color.RED.getRGB(), ImageIO.read(imported.frames.get(0)).getRGB(4, 4));
        assertEquals(Color.GREEN.getRGB(), ImageIO.read(imported.frames.get(1)).getRGB(4, 4));
        assertEquals(Color.BLUE.getRGB(), ImageIO.read(imported.frames.get(2)).getRGB(4, 4));
        assertEquals(Color.YELLOW.getRGB(), ImageIO.read(imported.frames.get(3)).getRGB(4, 4));
        assertEquals("hero_walk_down_0.png", imported.frames.get(0).getName());
        assertTrue(imported.toReport().contains("2 coluna(s) × 2 linha(s)"));
    }

    @Test
    void spritesheetRejectsAFrameGridWithTrailingPixels() throws Exception {
        File source = new File(root, "input/grade_invalida.png");
        source.getParentFile().mkdirs();
        ImageIO.write(new BufferedImage(65, 32, BufferedImage.TYPE_INT_ARGB), "png", source);
        IOException failure = assertThrows(IOException.class, () -> AssetCoach.inspectSpritesheet(source, 32, 32));
        assertTrue(failure.getMessage().contains("não fecha uma grade"));
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
