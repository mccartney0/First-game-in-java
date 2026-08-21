package com.traduvertgames.tools;

import java.io.File;

/** Ponto de entrada Gradle para exportar o pacote visual RPG consumido pelo APK. */
public final class RpgVisualContentBootstrap {
    private RpgVisualContentBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        File root = new File(System.getProperty("user.dir", ".")).getCanonicalFile();
        File[] files = ContentStudioProject.generateDefaultRpgVisualPack(root);
        System.out.println("Pacote visual RPG exportado em " + files[0].getParentFile().getAbsolutePath());
        for (File file : files) {
            System.out.println(" - " + file.getAbsolutePath());
        }
    }
}
