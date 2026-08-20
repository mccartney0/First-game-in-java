package com.traduvertgames.tools;

import java.io.File;

/** Demonstra a mesma exportação do botão visual para validação automatizada. */
public final class MistSovereignBossBootstrap {
    private MistSovereignBossBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args.length > 0 ? args[0] : System.getProperty("user.dir"));
        File boss = ContentStudioProject.generateMistSovereignBoss(root);
        System.out.println("Chefe exportado em " + boss.getAbsolutePath());
    }
}
