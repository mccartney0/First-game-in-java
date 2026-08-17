import java.io.File;
import com.traduvertgames.main.SaveManager;
import com.traduvertgames.main.Game;

/**
 * Rodada 24b — recorde de profundidade do modo infinito.
 * Valida: setDeepRecord grava e persiste em saves.json; não regride;
 * restaurado no carregamento do slot; novo jogo reseta em memória;
 * exibido pela tela de carregar jogo (getSlotDeepRecord).
 */
public class Rodada24bDeepRecordTest {

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
        // Estado limpo: nenhum save gravado.
        Game g = new Game();
        Game.SCALE = 4;

        // 1. Recorde inicial vazio.
        SaveManager.activeSlot = 1;
        check("Recorde inicial é 0", SaveManager.getDeepRecord() == 0);

        // 2. setDeepRecord grava no slot ativo e persiste no arquivo.
        SaveManager.setDeepRecord(5);
        check("setDeepRecord(5) atualiza em memória", SaveManager.getDeepRecord() == 5);
        String json = new String(java.nio.file.Files.readAllBytes(SaveManager.SAVE_FILE.toPath()));
        boolean persisted = SaveManager.SAVE_FILE.exists() && json.contains("\"deepRecord\"");
        check("Recorde gravado em saves.json", persisted);

        // 3. Não regride com profundidade menor.
        SaveManager.setDeepRecord(3);
        check("Não regride (5 -> 3 mantido)", SaveManager.getDeepRecord() == 5);
        SaveManager.setDeepRecord(5);
        check("Mesmo valor não regrava (5 -> 5 mantido)", SaveManager.getDeepRecord() == 5);
        SaveManager.setDeepRecord(9);
        check("Avança com profundidade maior", SaveManager.getDeepRecord() == 9);

        // 4. getSlotDeepRecord lê do disco sem carregar o slot.
        int fromDisk = SaveManager.getSlotDeepRecord(1);
        check("getSlotDeepRecord(1) reflete o disco", fromDisk == 9);
        check("Slot inexistente retorna 0", SaveManager.getSlotDeepRecord(2) == 0);
        check("Slot fora da faixa retorna 0", SaveManager.getSlotDeepRecord(0) == 0);

        // 5. Novo jogo reseta o recorde em memória.
        g.startNewGame();
        check("startNewGame reseta em memória", SaveManager.getDeepRecord() == 0);
        // O valor gravado no save persiste até ser sobrescrito.
        check("Gravado no disco sobrevive ao novo jogo", SaveManager.getSlotDeepRecord(1) == 9);

        // 6. Carregar o slot restaura o recorde.
        Game.player = new com.traduvertgames.entities.Player(200, 100, 16, 16,
                new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB));
        boolean loaded = SaveManager.loadSlot(1);
        check("Slot carregado", loaded);
        check("loadSlot restaura recorde = 9", SaveManager.getDeepRecord() == 9);

        System.out.println("Progresso: " + pass + " passaram, " + fail + " falharam");
        System.exit(fail == 0 ? 0 : 1);
    }
}
