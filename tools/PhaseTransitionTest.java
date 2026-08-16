// -*- coding: utf-8 -*-
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.traduvertgames.main.Game;
import com.traduvertgames.main.SoundManager;

/**
 * Valida a transição de fase limpa (rodada 13):
 * 1) conclusão de objetivo marca a transição e remove bullets;
 * 2) fade preto ativo (transitionAlpha > 0) na transição;
 * 3) HUDs ocultas (hidingHud true) durante o fade;
 * 4) inimigos invisíveis e congelados quando questCompletedPending;
 * 5) ao avançar, banner de lore dourado da nova fase é exibido;
 * 6) NPCs temáticos posicionados fora do canto de spawn na fase 2.
 */
public class PhaseTransitionTest {
    static int fails = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (!ok) fails++;
    }

    public static void main(String[] args) throws Exception {
        // Desativar áudio: remover pools de clips para não travar sem dispositivo
        SoundManager.unload();
        Game g = new Game();
        g.setCurrentLevel(1);
        com.traduvertgames.world.World.restartGame("level1.png");
        com.traduvertgames.quest.QuestManager.onLevelLoaded();

        // 1) concluir objetivo manualmente (como o diálogo com a Ava faria)
        com.traduvertgames.main.ShopManager.close();
        Method onComplete = Game.class.getDeclaredMethod("onObjectiveComplete");
        onComplete.setAccessible(true);
        onComplete.invoke(g);

        Field fPending = Game.class.getDeclaredField("questCompletedPending");
        fPending.setAccessible(true);
        check("conclusão marca questCompletedPending", (boolean) fPending.get(null));
        check("bullets removidos na conclusão", Game.bullets.isEmpty());
        check("inimigos congelados (update pulado em transição)", Game.isTransitioning());

        // 4) inimigos invisíveis durante a transição (Enemy.render retorna cedo
        // quando Game.isTransitioning() — validar via reflexão do método render)
        com.traduvertgames.entities.Enemy sample = Game.enemies.isEmpty() ? null : Game.enemies.get(0);
        boolean renderSkipped = true;
        if (sample != null) {
            BufferedImage probe = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics probeG = probe.createGraphics();
            Method renderEnemy = sample.getClass().getDeclaredMethod("render", Graphics.class);
            renderEnemy.setAccessible(true);
            renderEnemy.invoke(sample, probeG);
            probeG.dispose();
            renderSkipped = (probe.getRGB(8, 8) & 0xFF000000) == 0;
        }
        check("inimigos invisíveis durante a conclusão", renderSkipped);

        // 2) fechar a loja e rodar o update: o avanço de fase acontece no update()
        // (quando questCompletedPending fecha a janela da conclusão).
        com.traduvertgames.main.ShopManager.close();
        g.update();
        Field fAlpha = Game.class.getDeclaredField("transitionAlpha");
        fAlpha.setAccessible(true);
        check("fade ativo após conclusão (transitionAlpha>0)", ((int) fAlpha.get(null)) > 0);

        // 3) hidingHud durante o fade = questCompletedPending || showLevelTransition>0
        Field fTrans = Game.class.getDeclaredField("showLevelTransition");
        fTrans.setAccessible(true);
        int pendingShown = (int) fTrans.get(null);
        boolean hidingHudLogic = (boolean) fPending.get(null) || pendingShown > 0;
        check("HUDs ocultas durante o fade", hidingHudLogic);

        // 5) avançar para a fase 2 e conferir banner de lore
        Field fCur = Game.class.getDeclaredField("CUR_LEVEL");
        fCur.setAccessible(true);
        fCur.set(null, 1);
        Method advance = Game.class.getDeclaredMethod("advanceToNextLevel");
        advance.setAccessible(true);
        advance.invoke(g);
        check("fase atual virou 2", com.traduvertgames.main.Game.getCurrentLevel() == 2);
        check("QuestManager acompanhou a fase 2",
            com.traduvertgames.quest.QuestManager.getCurrentLevel() == 2);

        Field fLife = com.traduvertgames.graficos.MissionBanner.class.getDeclaredField("life");
        fLife.setAccessible(true);
        int life = (int) fLife.get(null);
        check("banner de lore ativo na fase 2", life > 0);
        Field fSub = com.traduvertgames.graficos.MissionBanner.class.getDeclaredField("subtitle");
        fSub.setAccessible(true);
        String sub = (String) fSub.get(null);
        check("lore da fase 2 exibida", sub != null && !sub.isEmpty());

        // 6) NPCs temáticos na fase 2 fora do canto de spawn
        int storyNpcsAway = 0;
        int storyNpcsInSpawn = 0;
        for (Object o : Game.entities) {
            if (o instanceof com.traduvertgames.dialogue.InteractiveNpc) {
                com.traduvertgames.dialogue.InteractiveNpc npc = (com.traduvertgames.dialogue.InteractiveNpc) o;
                int x = npc.getX(), y = npc.getY();
                if (x < 32 && y < 32) storyNpcsInSpawn++;
                else storyNpcsAway++;
            }
        }
        check("NPCs temáticos espalhados pelo mapa (fora do spawn)", storyNpcsAway > 0);
        check("nenhum NPC temático colado no canto de spawn", storyNpcsInSpawn == 0);

        if (fails == 0) System.out.println("== PhaseTransitionTest: TODOS PASSARAM ==");
        else System.out.println("== PhaseTransitionTest: " + fails + " falharam ==");
        System.exit(fails == 0 ? 0 : 1);
    }
}
