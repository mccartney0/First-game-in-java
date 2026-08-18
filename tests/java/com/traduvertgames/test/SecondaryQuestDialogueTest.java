package com.traduvertgames.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.traduvertgames.dialogue.DialogueManager;
import com.traduvertgames.dialogue.InteractiveNpc;
import com.traduvertgames.entities.SecondaryNpcs;
import com.traduvertgames.main.Game;
import com.traduvertgames.quest.QuestManager;
import com.traduvertgames.quest.SideQuestManager;

/** Regressões das escolhas de aceitar/recusar missões secundárias. */
public class SecondaryQuestDialogueTest {

    @BeforeEach
    void resetState() throws Exception {
        DialogueManager.stop();
        SideQuestManager.reset();
        GameTestFixture.cleanSaveFiles();
        GameTestFixture.initHeadless();
        Game.gameState = "NORMAL";
        Game.entities.removeIf(entity -> entity instanceof InteractiveNpc);
        QuestManager.prepareForLevel(8);
    }

    @Test
    void firstChoiceAcceptsRexWithoutIntermediatePrompt() {
        Game.entities.add(SecondaryNpcs.createVeteranRex(Game.player.getX(), Game.player.getY()));

        assertNotNull(DialogueManager.startNearestDialogue());
        DialogueManager.selectBranchChoice(0);
        assertTrue(Arrays.stream(DialogueManager.getBranchChoices())
                        .allMatch(choice -> choice == null || choice.isEmpty()),
                "aceitar deve ir direto ao nó de progresso sem novo prompt");
        DialogueManager.advance();

        assertTrue(SideQuestManager.isActive("rex_kills_8"),
                "a opção 1 deve ativar a missão do Rex");
    }

    @Test
    void secondChoiceRefusesRexWithoutActivatingQuest() {
        Game.entities.add(SecondaryNpcs.createVeteranRex(Game.player.getX(), Game.player.getY()));

        assertNotNull(DialogueManager.startNearestDialogue());
        DialogueManager.selectBranchChoice(1);
        DialogueManager.advance();

        assertFalse(SideQuestManager.isActive("rex_kills_8"),
                "a opção 2 deve recusar a missão do Rex");
    }
}
