package com.traduvertgames.tools;

import java.io.File;

/** Ponto de entrada para gerar o pacote inicial de itens RPG pelo Gradle. */
public final class RpgContentBootstrap {
    private RpgContentBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        File root = new File(System.getProperty("user.dir", ".")).getCanonicalFile();
        File[] files = ContentStudioProject.generateDefaultRpgContentPack(root);
        System.out.println("Pacote RPG exportado em " + files[0].getParentFile().getParentFile().getAbsolutePath());
        for (File file : files) {
            System.out.println(" - " + file.getAbsolutePath());
        }
    }
}
