package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.KeyEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.dialogue.DialogueManager;
import com.traduvertgames.dialogue.SupportNpcs;
import com.traduvertgames.entities.Entity;
import com.traduvertgames.entities.Player;
import com.traduvertgames.main.Game;

/** Regressão do bloqueio visual da escolta após carregar um save. */
public class EscortDialoguePauseTest {

    @BeforeEach
    void resetDialogueState() throws Exception {
        DialogueManager.stop();
        com.traduvertgames.graficos.PhaseStatsScreen.dismiss();
        com.traduvertgames.graficos.VictoryCutscene.stop();
        GameTestFixture.cleanSaveFiles();
        GameTestFixture.initHeadless();
        Game.gameState = "NORMAL";
        if (Game.entities != null) {
            Game.entities.removeIf(entity -> entity instanceof com.traduvertgames.dialogue.InteractiveNpc);
        }
    }

    @Test
    void escapeClosesDialogueBeforeOpeningPause() {
        assertNotNull(Game.player);
        Player pilot = Game.player;
        Entity npc = SupportNpcs.engineer(pilot.getX(), pilot.getY());
        Game.entities.add(npc);

        assertNotNull(DialogueManager.startNearestDialogue());
        assertTrue(DialogueManager.isActive());

        Game game = Game.getInstance();
        game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                KeyEvent.VK_ESCAPE, (char) KeyEvent.VK_ESCAPE));

        assertFalse(DialogueManager.isActive(), "ESC deve fechar o diálogo ativo");
        assertFalse("MENU".equals(Game.gameState), "ESC não deve abrir pausa por baixo do diálogo");
    }
}
