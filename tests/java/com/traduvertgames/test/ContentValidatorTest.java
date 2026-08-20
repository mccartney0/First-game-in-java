package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.traduvertgames.tools.ContentValidator;
import com.traduvertgames.tools.ContentValidator.Check;

class ContentValidatorTest {
    @TempDir
    Path temp;

    @Test
    void detectsAllSevenRequestedCategories() throws Exception {
        Files.createDirectories(temp.resolve("src/com/traduvertgames/graficos"));
        Files.createDirectories(temp.resolve("src/com/traduvertgames/entities"));
        Files.createDirectories(temp.resolve("res/assets/generated/weapons"));
        Files.createDirectories(temp.resolve("res/assets/generated/companions"));
        Files.createDirectories(temp.resolve("res/assets/generated/atlas_cells/companions"));

        Files.writeString(temp.resolve("src/com/traduvertgames/graficos/AssetCatalog.java"),
                "class AssetCatalog { String a=\"/assets/generated/weapons/missing.png\";"
                + " String b=\"/assets/generated/weapons/solid.png\";"
                + " String c=\"/assets/generated/weapons/large.png\"; }", StandardCharsets.UTF_8);
        Files.writeString(temp.resolve("src/com/traduvertgames/entities/WeaponType.java"),
                "enum WeaponType { BLASTER; }", StandardCharsets.UTF_8);

        BufferedImage solid = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        BufferedImage large = new BufferedImage(2048, 8, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(solid, "png", temp.resolve("res/assets/generated/weapons/solid.png").toFile());
        ImageIO.write(large, "png", temp.resolve("res/assets/generated/weapons/large.png").toFile());

        Files.writeString(temp.resolve("res/assets/generated/companions/companion_set_clean.png"), "", StandardCharsets.UTF_8);
        Files.writeString(temp.resolve("res/assets/generated/user_asset_manifest.json"),
                "{\"assets\":["
                + "{\"source\":\"atlas.webp\",\"category\":\"companion_atlas\",\"runtime_loaded\":true,"
                + "\"outputs\":[\"res/assets/generated/companions/companion_set_clean.png\"]},"
                + "{\"source\":\"shot.png\",\"category\":\"weapon_shot_effect\",\"runtime_loaded\":false,"
                + "\"outputs\":[\"res/assets/generated/effects/missing.png\"]}]}"
                , StandardCharsets.UTF_8);

        ContentValidator.Report report = ContentValidator.validate(temp.toFile());
        Set<Check> checks = report.getIssues().stream()
                .map(ContentValidator.Issue::getCheck)
                .collect(Collectors.toSet());
        assertTrue(checks.containsAll(EnumSet.allOf(Check.class)),
                "Categorias encontradas: " + checks + "\n" + report.toText());
    }
}
