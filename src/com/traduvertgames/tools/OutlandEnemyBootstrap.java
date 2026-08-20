package com.traduvertgames.tools;

import java.io.File;

/** Exporta as variações de inimigos usadas na Charneca da Bruma. */
public final class OutlandEnemyBootstrap {
    private OutlandEnemyBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args.length > 0 ? args[0] : System.getProperty("user.dir"));
        File[] generated = ContentStudioProject.generateOutlandEnemyPack(root);
        System.out.println("Pacote da Charneca exportado com " + generated.length + " sprites em "
                + new File(root, "res/assets/generated/enemies").getAbsolutePath());
    }
}
