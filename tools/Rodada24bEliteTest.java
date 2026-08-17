import java.awt.image.BufferedImage;
import java.io.File;
import com.traduvertgames.entities.Enemy;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.main.Game;
import com.traduvertgames.world.ProceduralLevelGenerator;

/**
 * Rodada 24b — tropas de elite procedurais.
 * Valida: vida/dano +30%, aura dourada (isElite), aparecem só a partir da
 * profundidade 3 (com cap), nunca contam como chefe e o mapa com elites
 * passa na validação estrutural.
 */
public class Rodada24bEliteTest {

    private static int pass = 0;
    private static int fail = 0;

    static void check(String name, boolean ok) {
        if (ok) {
            pass++;
            System.out.println("PASS: " + name);
        } else {
            fail++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        // Inicializa a engine (sem janela visível — Xvfb em DISPLAY=:120).
        Game g = new Game();
        Game.SCALE = 4;
        Game.setCurrentLevel(1);
        Game.player = new com.traduvertgames.entities.Player(200, 100, 16, 16,
                new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));

        // 1. Inimigo comum não é elite; elite tem vida e dano +30% comparados
        //    ao irmão comum da mesma variante.
        Enemy normal = new Enemy(100, 100, 16, 16, Entity.ENEMY_EN,
                Enemy.Variant.WARDEN, false, false);
        check("Comum não é elite", !normal.isElite());

        Enemy elite = new Enemy(100, 100, 16, 16, Entity.ENEMY_EN,
                Enemy.Variant.WARDEN, false, true);
        check("Elite marcado como elite", elite.isElite());
        check("Elite: vida +30% sobre o comum",
                Math.abs(elite.getTotalLife() - normal.getTotalLife() * 1.3) < 0.001);
        check("Elite: dano de projétil +30% sobre o comum",
                Math.abs(elite.getEffectiveProjectileDamage() - normal.getEffectiveProjectileDamage() * 1.3)
                        < 0.001);
        check("Elite nunca é chefe", !elite.isBoss());

        // 2. Profundidade 1 e 2 não geram elites; profundidade 3+ sim.
        File f1 = ProceduralLevelGenerator.generate(1);
        BufferedImage m1 = javax.imageio.ImageIO.read(f1);
        check("Depth 1: sem pixel elite", countPixel(m1, 0xFFFFC800) == 0);
        check("Depth 1: mapa válido", ProceduralLevelGenerator.validate(m1));

        File f2 = ProceduralLevelGenerator.generate(2);
        BufferedImage m2 = javax.imageio.ImageIO.read(f2);
        check("Depth 2: sem pixel elite", countPixel(m2, 0xFFFFC800) == 0);

        File f3 = ProceduralLevelGenerator.generate(3);
        BufferedImage m3 = javax.imageio.ImageIO.read(f3);
        int elites3 = countPixel(m3, 0xFFFFC800);
        check("Depth 3: elite presente", elites3 >= 1);
        check("Depth 3: elite dentro do cap (1 + depth/3 = 2)", elites3 <= 2);
        check("Depth 3: mapa válido", ProceduralLevelGenerator.validate(m3));

        // 3. Cap cresce com a profundidade.
        File f9 = ProceduralLevelGenerator.generate(9);
        BufferedImage m9 = javax.imageio.ImageIO.read(f9);
        int elites9 = countPixel(m9, 0xFFFFC800);
        check("Depth 9: cap = 1 + 9/3 = 4", elites9 <= 4);
        check("Depth 9: pelo menos 1 elite", elites9 >= 1);
        check("Depth 9: mapa válido", ProceduralLevelGenerator.validate(m9));

        // 4. Determinismo por semente: mesma profundidade gera o mesmo mapa.
        File f3b = ProceduralLevelGenerator.generate(3);
        BufferedImage m3b = javax.imageio.ImageIO.read(f3b);
        check("Determinismo: mesma semente = mesmo mapa", m3.getRGB(20, 20) == m3b.getRGB(20, 20)
                && countPixel(m3, 0xFFFFC800) == countPixel(m3b, 0xFFFFC800));

        System.out.println("Progresso: " + pass + " passaram, " + fail + " falharam");
        System.exit(fail == 0 ? 0 : 1);
    }

    private static int countPixel(BufferedImage map, int rgb) {
        int count = 0;
        int w = map.getWidth();
        int h = map.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (map.getRGB(x, y) == rgb) {
                    count++;
                }
            }
        }
        return count;
    }
}
