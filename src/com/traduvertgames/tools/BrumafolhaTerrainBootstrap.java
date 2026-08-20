package com.traduvertgames.tools;

import java.io.File;

/** Entrada Gradle para regenerar somente o pacote de terreno do Vale. */
public final class BrumafolhaTerrainBootstrap {
    private BrumafolhaTerrainBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args.length > 0 ? args[0] : System.getProperty("user.dir"));
        File[] generated = ContentStudioProject.generateBrumafolhaTerrainPack(root);
        System.out.println("Pacote Brumafolha exportado: " + generated.length + " tiles em "
                + new File(root, "res/assets/generated/tiles").getAbsolutePath());
    }
}
