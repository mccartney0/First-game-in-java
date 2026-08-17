import com.traduvertgames.entities.TeleportPad;
import com.traduvertgames.main.Game;
import com.traduvertgames.graficos.Spritesheet;
import java.lang.reflect.*;
import java.awt.image.BufferedImage;

public class TeleportPadTest {
    static int passed = 0, failed = 0;

    static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("[PASS] " + name); }
        else { failed++; System.out.println("[FAIL] " + name); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== TeleportPad Unit Test ===");

        // Mock Game.spritesheet — usar o spritesheet real
        Field fSheet = Game.class.getDeclaredField("spritesheet");
        fSheet.setAccessible(true);
        Spritesheet mockSheet = new Spritesheet("/spritesheet.png");
        fSheet.set(null, mockSheet);

        // Reset pads
        TeleportPad.reset();

        // Criar 2 pads (simula o level2.png)
        TeleportPad p1 = new TeleportPad(16, 16);
        TeleportPad p2 = new TeleportPad(384, 192);
        TeleportPad.linkPairs();

        Field fPairIndex = TeleportPad.class.getDeclaredField("pairIndex");
        fPairIndex.setAccessible(true);
        check("Pad1 pairIndex = 0", fPairIndex.getInt(p1) == 0);
        check("Pad2 pairIndex = 1", fPairIndex.getInt(p2) == 1);

        // Teste com 3 pads (ímpar)
        TeleportPad.reset();
        TeleportPad p3 = new TeleportPad(16, 16);
        TeleportPad p4 = new TeleportPad(100, 100);
        TeleportPad p5 = new TeleportPad(200, 200);
        TeleportPad.linkPairs();
        check("Pad3 pairIndex = 0 (odd)", fPairIndex.getInt(p3) == 0);
        check("Pad4 pairIndex = 1 (odd)", fPairIndex.getInt(p4) == 1);
        check("Pad5 pairIndex = 2 (odd, no partner)", fPairIndex.getInt(p5) == 2);

        // Teste: reset não crasha
        TeleportPad.reset();
        check("TeleportPad.reset() no exception", true);

        System.out.println("\n=== TeleportPad Results: " + passed + " passed, " + failed + " failed ===");
        System.exit(failed > 0 ? 1 : 0);
    }
}
