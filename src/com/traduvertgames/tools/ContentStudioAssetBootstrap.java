package com.traduvertgames.tools;

import java.io.File;

/** Gera o pacote-base de sprites distintos sem depender de editor externo. */
public final class ContentStudioAssetBootstrap {
    private ContentStudioAssetBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args.length > 0 ? args[0] : System.getProperty("user.dir"));
        for (ContentStudioProject.EnemyRole role : ContentStudioProject.EnemyRole.values()) {
            ContentStudioProject.generateEnemySprite(role, null, null, root);
        }
        for (int variant = 0; variant < 4; variant++) {
            ContentStudioProject.generateTile(ContentStudioProject.TileStyle.GRAMA, "brumafolha_grass_" + variant,
                    variant, ContentStudioProject.TileProperties.defaults(ContentStudioProject.TileStyle.GRAMA), root);
        }
        for (int variant = 0; variant < 3; variant++) {
            ContentStudioProject.generateTile(ContentStudioProject.TileStyle.ESTRADA, "brumafolha_road_" + variant,
                    variant, ContentStudioProject.TileProperties.defaults(ContentStudioProject.TileStyle.ESTRADA), root);
            ContentStudioProject.generateTile(ContentStudioProject.TileStyle.RUINAS, "brumafolha_ruins_" + variant,
                    variant, ContentStudioProject.TileProperties.defaults(ContentStudioProject.TileStyle.RUINAS), root);
        }
        System.out.println("Pacote de terreno e inimigos exportado em "
                + new File(root, "res/assets/generated").getAbsolutePath());
    }
}
