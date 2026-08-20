package com.traduvertgames.tools;

import java.io.File;

/** Exporta a habilidade especial demonstrada no canvas de inimigos do Content Studio. */
public final class MistSovereignAbilityBootstrap {
    private MistSovereignAbilityBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args.length > 0 ? args[0] : System.getProperty("user.dir"));
        File ability = ContentStudioProject.generateMistSovereignAbility(root);
        System.out.println("Habilidade do Soberano exportada em " + ability.getAbsolutePath());
    }
}
