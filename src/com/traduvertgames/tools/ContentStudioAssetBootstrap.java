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
        System.out.println("Pacote de inimigos exportado em "
                + new File(root, "res/assets/generated/enemies").getAbsolutePath());
    }
}
