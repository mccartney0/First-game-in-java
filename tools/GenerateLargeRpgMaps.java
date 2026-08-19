import java.io.File;

import com.traduvertgames.world.LargeRpgMapGenerator;

/** Gera um lote de mapas RPG grandes para prototipagem, QA ou expansão de conteúdo. */
public final class GenerateLargeRpgMaps {

    private GenerateLargeRpgMaps() {
    }

    public static void main(String[] args) throws Exception {
        int width = 192;
        int height = 128;
        int depth = 1;
        int count = 1;
        long seed = 0x5EEDL;
        File output = new File("bin/large_rpg_maps");

        for (String arg : args) {
            if (arg.startsWith("--width=")) {
                width = Integer.parseInt(arg.substring("--width=".length()));
            } else if (arg.startsWith("--height=")) {
                height = Integer.parseInt(arg.substring("--height=".length()));
            } else if (arg.startsWith("--depth=")) {
                depth = Integer.parseInt(arg.substring("--depth=".length()));
            } else if (arg.startsWith("--count=")) {
                count = Integer.parseInt(arg.substring("--count=".length()));
            } else if (arg.startsWith("--seed=")) {
                seed = Long.parseLong(arg.substring("--seed=".length()));
            } else if (arg.startsWith("--output=")) {
                output = new File(arg.substring("--output=".length()));
            } else if ("--help".equals(arg)) {
                printHelp();
                return;
            }
        }

        if (count < 1) {
            throw new IllegalArgumentException("--count deve ser >= 1");
        }
        for (int i = 0; i < count; i++) {
            int currentDepth = depth + i;
            long currentSeed = seed + i * 104729L;
            File map = LargeRpgMapGenerator.generate(width, height, currentDepth, currentSeed, output);
            System.out.println("GENERATED " + map.getAbsolutePath());
        }
    }

    private static void printHelp() {
        System.out.println("Uso: GenerateLargeRpgMaps [opções]");
        System.out.println("  --width=192     largura em tiles (mínimo 96)");
        System.out.println("  --height=128    altura em tiles (mínimo 64)");
        System.out.println("  --depth=1       profundidade inicial");
        System.out.println("  --count=1       quantidade de mapas");
        System.out.println("  --seed=24301    semente determinística");
        System.out.println("  --output=...    diretório de PNGs e manifestos JSON");
    }
}
